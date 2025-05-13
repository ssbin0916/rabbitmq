package com.example.rabbitmq.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("kafka_messages")
@Data
@Builder
public class KafkaMessageEntity {
    @PrimaryKey
    private UUID id;
    private String topic;
    private String payload;

    @Column("received_at")
    private Instant receivedAt;
}
