package com.example.rabbitmq.service;

import com.example.rabbitmq.model.IotMessage;
import com.example.rabbitmq.repository.IotMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IotService {

    private final IotMessageRepository messageRepository;
    private final MessageChannel mqttOutboundChannel;

    @Value("${mqtt.topic}")
    private String topic;
    
    private volatile boolean receiverActive = true;
    private volatile boolean brokerConnected = false;

    // 시작 시 미처리 메시지 체크
    @PostConstruct
    public void init() {
        processUnprocessedMessages();
    }

    // 브로커 상태 업데이트 메소드
    public void updateBrokerStatus(boolean connected) {
        this.brokerConnected = connected;
        log.info("MQTT/RabbitMQ 브로커 연결 상태 업데이트: {}", connected ? "연결됨" : "연결 안됨");
    }

    // MQTT 메시지 수신 - MQTT 플러그인에서 RabbitMQ로 전달
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMqttMessage(Message<String> message) {
        String payload = message.getPayload();
        String receivedTopic = message.getHeaders().get("mqtt_receivedTopic", String.class);
        
        // 브로커 연결 상태 업데이트
        updateBrokerStatus(true);
        
        // 메시지 저장
        IotMessage iotMessage = saveMessage(receivedTopic, payload);
        
        log.info("MQTT 메시지 수신 [{}]: {}", receivedTopic, payload);
        
        // 수신기가 활성화된 경우에만 즉시 처리
        if (receiverActive) {
            processMessage(iotMessage);
        } else {
            log.info("수신기가 비활성화 상태입니다. 메시지가 큐에 저장되었습니다.");
        }
    }

    // MQTT 연결 실패 처리
    @ServiceActivator(inputChannel = "mqttFailureChannel")
    // MQTT 연결 실패 처리는 애플리케이션 로직으로 대체
    private void handleMqttFailure(String payload) {
        brokerConnected = false;
        log.error("MQTT 브로커 연결 실패: {}", payload);
    }

    // 메시지 저장 메소드
    @Transactional
    public IotMessage saveMessage(String topic, String payload) {
        IotMessage iotMessage = new IotMessage(topic, payload);
        return messageRepository.save(iotMessage);
    }

    // 메시지 처리 로직
    @Transactional
    public void processMessage(IotMessage message) {
        try {
            // 실제 메시지 처리 로직 (여기서는 로그만 출력)
            log.info("메시지 처리: {}", message.getPayload());
            
            // 처리 완료 표시
            message.setProcessed(true);
            messageRepository.save(message);
        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생", e);
        }
    }

    // 미처리 메시지 처리
    @Transactional
    public void processUnprocessedMessages() {
        List<IotMessage> unprocessedMessages = messageRepository.findByProcessedFalseOrderByReceivedTimeAsc();
        log.info("미처리 메시지 {}개 발견", unprocessedMessages.size());
        
        for (IotMessage message : unprocessedMessages) {
            if (receiverActive) {
                processMessage(message);
            } else {
                log.info("수신기가 비활성화 상태입니다. 처리가 연기됩니다.");
                break;
            }
        }
    }

    // 메시지 재전송
    @Transactional
    public void resendMessage(Long messageId) {
        messageRepository.findById(messageId).ifPresent(message -> {
            if (brokerConnected) {
                Message<String> mqttMessage = MessageBuilder
                        .withPayload(message.getPayload())
                        .build();
                mqttOutboundChannel.send(mqttMessage);
                log.info("메시지 재전송 [{}]: {}", message.getTopic(), message.getPayload());
            } else {
                log.warn("MQTT 브로커에 연결할 수 없습니다. 메시지 재전송이 취소되었습니다.");
            }
        });
    }

    // 특정 기간의 메시지 조회
    @Transactional(readOnly = true)
    public List<IotMessage> getMessagesByTimeRange(LocalDateTime start, LocalDateTime end) {
        return messageRepository.findByReceivedTimeBetweenOrderByReceivedTimeAsc(start, end);
    }

    // 수신기 활성화/비활성화
    public void setReceiverActive(boolean active) {
        this.receiverActive = active;
        log.info("수신기 상태 변경: {}", active ? "활성화" : "비활성화");
        
        // 활성화 시 미처리 메시지 처리
        if (active) {
            processUnprocessedMessages();
        }
    }
    
    // 브로커 연결 상태 확인
    public boolean isBrokerConnected() {
        return brokerConnected;
    }
    
    // 수신기 활성화 상태 확인
    public boolean isReceiverActive() {
        return receiverActive;
    }

    // 미처리 메시지 수 조회
    public long countUnprocessedMessages() {
        return messageRepository.countByProcessedFalse();
    }
    
    // 메시지 삭제
    @Transactional
    public void deleteMessage(Long messageId) {
        messageRepository.deleteById(messageId);
    }

    // 주기적으로 미처리 메시지 체크 (5분마다)
    @Scheduled(fixedRate = 300000)
    public void scheduledProcessing() {
        if (receiverActive) {
            processUnprocessedMessages();
        }
    }
    
    // 주기적으로 브로커에 재연결 시도 (30초마다)
    @Scheduled(fixedRate = 30000)
    public void checkAndReconnect() {
        if (!brokerConnected) {
            log.info("MQTT 브로커에 재연결 시도 중...");
            // 재연결 로직은 MqttPahoClientFactory에서 자동 처리됨
        }
    }
    
    
}