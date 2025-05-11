package com.example.rabbitmq.controller;

import com.example.rabbitmq.service.TopicAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/topics/admin")
@RequiredArgsConstructor
public class TopicAdminController {

    private final TopicAdminService topicAdminService;

    @PostMapping("/clear")
    public ResponseEntity<String> clearTopic() {
        topicAdminService.clearTopicRecords();
        return ResponseEntity.ok("Topic records cleared");
    }

    @PostMapping("/recreate")
    public ResponseEntity<String> recreateTopic() {
        topicAdminService.recreateTopic();
        return ResponseEntity.ok("Topic deleted and recreated");
    }
}
