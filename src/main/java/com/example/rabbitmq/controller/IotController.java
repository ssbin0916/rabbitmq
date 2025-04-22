package com.example.rabbitmq.controller;

import com.example.rabbitmq.dto.SensorData;
import com.example.rabbitmq.model.IotMessage;
import com.example.rabbitmq.service.IotService;
import com.example.rabbitmq.service.RabbitMqService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/iot")
@RequiredArgsConstructor
@Slf4j
public class IotController {

    private final IotService iotService;
    private final MessageChannel mqttOutboundChannel;
    private final RabbitMqService rabbitMqService;
    private final ObjectMapper objectMapper;

    @PostMapping("/sensor")
    public ResponseEntity<Map<String, Object>> sendSensorData(@RequestBody SensorData sensorData) {
        Map<String, Object> response = new HashMap<>();

        try {
            // SensorData를 JSON 문자열로 변환
            String payload = objectMapper.writeValueAsString(sensorData);

            // 메시지 저장
            IotMessage savedMessage = iotService.saveMessage("iot/sensor", payload);
            response.put("messageId", savedMessage.getId());
            response.put("savedToDatabase", true);

            // MQTT로 전송 시도
            boolean mqttSent = false;
            try {
                Message<String> message = MessageBuilder.withPayload(payload).build();
                mqttOutboundChannel.send(message);
                mqttSent = true;
            } catch (Exception e) {
                log.error("MQTT 메시지 전송 중 오류: {}", e.getMessage());
                iotService.updateBrokerStatus(false);
            }

            // RabbitMQ로 전송 시도
            boolean rabbitSent = false;
            try {
                rabbitMqService.sendMessage(payload);
                rabbitSent = true;
            } catch (Exception e) {
                log.error("RabbitMQ 메시지 전송 중 오류: {}", e.getMessage());
            }

            // 응답 구성
            if (mqttSent && rabbitSent) {
                response.put("status", "success");
                response.put("message", "센서 데이터가 MQTT와 RabbitMQ 모두에 성공적으로 전송되었습니다");
            } else if (mqttSent || rabbitSent) {
                response.put("status", "partial");
                response.put("message", "센서 데이터가 " + (mqttSent ? "MQTT" : "RabbitMQ") + "에만 전송되었습니다");
            } else {
                response.put("status", "failure");
                response.put("message", "센서 데이터가 데이터베이스에 저장되었으나 메시지 브로커로 전송되지 않았습니다");
            }

            response.put("sentToMqtt", mqttSent);
            response.put("sentToRabbitMq", rabbitSent);

        } catch (Exception e) {
            log.error("센서 데이터 처리 중 오류: {}", e.getMessage());
            response.put("status", "error");
            response.put("message", "센서 데이터 처리 중 오류: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    // 메시지 발행 API
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody String payload) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 먼저 메시지를 데이터베이스에 저장
//            IotMessage savedMessage = iotService.saveMessage("iot/data", payload);
//            response.put("messageId", savedMessage.getId());
//            response.put("savedToDatabase", true);

            // 그 다음 MQTT로 전송 시도
            try {
                Message<String> message = MessageBuilder.withPayload(payload).build();
                mqttOutboundChannel.send(message);

                // 메시지가 전송된 것으로 가정 (실제로는 확인할 수 없음)
                response.put("status", "success");
                response.put("message", "메시지가 MQTT 브로커로 전송 시도되었습니다");
                response.put("sentToMqtt", true);

            } catch (Exception e) {
                // MQTT 전송 오류
                log.error("MQTT 메시지 전송 중 오류: {}", e.getMessage());
                response.put("status", "partial");
                response.put("message", "메시지가 데이터베이스에 저장되었으나 MQTT 브로커로 전송되지 않았습니다");
                response.put("sentToMqtt", false);
                iotService.updateBrokerStatus(false);
            }
        } catch (Exception e) {
            log.error("메시지 처리 중 오류: {}", e.getMessage());
            response.put("status", "error");
            response.put("message", "메시지 처리 중 오류: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    // MQTT 브로커 연결 상태 확인 API
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("brokerConnected", iotService.isBrokerConnected());
        status.put("receiverActive", iotService.isReceiverActive());

        // 미처리 메시지 수 가져오기
        long unprocessedCount = iotService.countUnprocessedMessages();
        status.put("unprocessedMessages", unprocessedCount);

        return ResponseEntity.ok(status);
    }

    // 나머지 메소드는 기존과 동일...
    // 수신기 상태 제어 API
    @PostMapping("/receiver/status")
    public ResponseEntity<String> setReceiverStatus(@RequestBody Map<String, Boolean> status) {
        boolean active = status.getOrDefault("active", true);
        iotService.setReceiverActive(active);
        return ResponseEntity.ok("수신기 상태가 " + (active ? "활성화" : "비활성화") + "되었습니다");
    }

    // 미처리 메시지 수동 처리 API
    @PostMapping("/process-unprocessed")
    public ResponseEntity<String> processUnprocessedMessages() {
        iotService.processUnprocessedMessages();
        return ResponseEntity.ok("미처리 메시지 처리가 시작되었습니다");
    }

    // 특정 메시지 재전송 API
    @PostMapping("/resend/{messageId}")
    public ResponseEntity<String> resendMessage(@PathVariable Long messageId) {
        iotService.resendMessage(messageId);
        return ResponseEntity.ok("메시지가 재전송되었습니다");
    }

    // 특정 기간 메시지 조회 API
    @GetMapping("/messages")
    public ResponseEntity<List<IotMessage>> getMessages(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<IotMessage> messages = iotService.getMessagesByTimeRange(start, end);
        return ResponseEntity.ok(messages);
    }

    // 메시지 삭제 API
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<String> deleteMessage(@PathVariable Long messageId) {
        iotService.deleteMessage(messageId);
        return ResponseEntity.ok("메시지가 삭제되었습니다");
    }
}