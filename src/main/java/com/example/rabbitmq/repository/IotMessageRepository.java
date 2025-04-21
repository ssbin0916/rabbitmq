package com.example.rabbitmq.repository;

import com.example.rabbitmq.model.IotMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IotMessageRepository extends JpaRepository<IotMessage, Long> {
    
    List<IotMessage> findByProcessedFalseOrderByReceivedTimeAsc();
    
    List<IotMessage> findByReceivedTimeBetweenOrderByReceivedTimeAsc(
            LocalDateTime start, LocalDateTime end);
            
    long countByProcessedFalse();
}