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
    }

    // MQTT 메시지 수신 - MQTT 플러그인에서 RabbitMQ로 전달
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMqttMessage(Message<String> message) {
        String payload = message.getPayload();
        String receivedTopic = message.getHeaders().get("mqtt_receivedTopic", String.class);
        
        // 브로커 연결 상태 업데이트
        updateBrokerStatus(true);
    }

    // MQTT 연결 실패 처리
    @ServiceActivator(inputChannel = "mqttFailureChannel")
    // MQTT 연결 실패 처리는 애플리케이션 로직으로 대체
    private void handleMqttFailure(String payload) {
        brokerConnected = false;
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
            // 처리 완료 표시
            message.setProcessed(true);
            messageRepository.save(message);
        } catch (Exception e) {
        }
    }

    // 미처리 메시지 처리
    @Transactional
    public void processUnprocessedMessages() {
        List<IotMessage> unprocessedMessages = messageRepository.findByProcessedFalseOrderByReceivedTimeAsc();

        for (IotMessage message : unprocessedMessages) {
            if (receiverActive) {
                processMessage(message);
            } else {
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
            } else {
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
            // 재연결 로직은 MqttPahoClientFactory에서 자동 처리됨
        }
    }
    
    
}