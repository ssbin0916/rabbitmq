package com.example.rabbitmq.repository;

import com.example.rabbitmq.entity.KafkaMessageEntity;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.UUID;

public interface KafkaMessageRepository extends CassandraRepository<KafkaMessageEntity, UUID> {
}
