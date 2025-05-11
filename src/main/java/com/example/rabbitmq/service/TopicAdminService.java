package com.example.rabbitmq.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class TopicAdminService {

    private final AdminClient adminClient;
    private final String topicName;

    public TopicAdminService(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
                             @Value("${spring.kafka.topic}") String topicName) {
        this.topicName = topicName;
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        this.adminClient = AdminClient.create(props);
    }

    public void clearTopicRecords() {
        try {
            Map<String, TopicDescription> descMap = adminClient
                    .describeTopics(Collections.singletonList(topicName))
                    .all().get();
            TopicDescription desc = descMap.get(topicName);
            List<TopicPartitionInfo> partitions = desc.partitions();

            Map<TopicPartition, RecordsToDelete> deleteMap = new HashMap<>();
            for (TopicPartitionInfo pInfo : partitions) {
                TopicPartition tp = new TopicPartition(topicName, pInfo.partition());
                deleteMap.put(tp, RecordsToDelete.beforeOffset(-1L));
            }
            DeleteRecordsResult result = adminClient.deleteRecords(deleteMap);
            result.all().get();
            log.info("All records cleared for topic '{}'.", topicName);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to clear records for topic '{}'.", topicName, e);
            Thread.currentThread().interrupt();
        }
    }

    public void recreateTopic() {
        try {
            DeleteTopicsResult deleteTopics = adminClient.deleteTopics(
                    Collections.singletonList(topicName)
            );
            deleteTopics.all().get();
            log.info("Topic '{}' deleted.", topicName);

            NewTopic newTopic = TopicBuilder.name(topicName)
                    .partitions(8)
                    .replicas((short) 1)
                    .config("retention.ms", String.valueOf(Duration.ofDays(1).toMillis()))
                    .build();

            CreateTopicsResult createTopics = adminClient.createTopics(
                    Collections.singletonList(newTopic)
            );
            createTopics.all().get();
            log.info("Topic '{}' recreated.", topicName);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to recreate topic '{}'.", topicName, e);
            Thread.currentThread().interrupt();
        }
    }
}
