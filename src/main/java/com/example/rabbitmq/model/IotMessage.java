package com.example.rabbitmq.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "iot_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IotMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String topic;
    
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String payload;
    
    private LocalDateTime receivedTime;
    
    private boolean processed;
    
    public IotMessage(String topic, String payload) {
        this.topic = topic;
        this.payload = payload;
        this.receivedTime = LocalDateTime.now();
        this.processed = false;
    }
}