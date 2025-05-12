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

import static java.util.stream.Collectors.toMap;

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

            Map<TopicPartition, RecordsToDelete> deleteMap = new HashMap<>();
            for (TopicPartitionInfo pInfo : desc.partitions()) {
                TopicPartition tp = new TopicPartition(topicName, pInfo.partition());
                // -1L 을 주면 EARLIEST offset 이전(=전체)를 삭제
                deleteMap.put(tp, RecordsToDelete.beforeOffset(-1L));
            }
            adminClient.deleteRecords(deleteMap).all().get();
            log.info("All records cleared for topic '{}'.", topicName);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to clear records for topic '{}'.", topicName, e);
            Thread.currentThread().interrupt();
        }
    }

    public void recreateTopic() {
        try {
            adminClient.deleteTopics(Collections.singletonList(topicName)).all().get();
            log.info("Topic '{}' deleted.", topicName);

            NewTopic newTopic = TopicBuilder.name(topicName)
                    .partitions(8)
                    .replicas((short) 1)
                    .config("retention.ms", String.valueOf(Duration.ofDays(1).toMillis()))
                    .build();
            adminClient.createTopics(Collections.singletonList(newTopic)).all().get();
            log.info("Topic '{}' recreated.", topicName);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to recreate topic '{}'.", topicName, e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 현재 토픽에 쌓여 있는 전체 메시지 수를 파티션별
     * (latestOffset – earliestOffset)의 합으로 계산해서 반환합니다.
     */
    public long getTopicMessageCount() throws ExecutionException, InterruptedException {
        // 1) 토픽 파티션 정보 조회
        Map<String, TopicDescription> desc = adminClient
                .describeTopics(List.of(topicName))
                .all().get();
        List<TopicPartition> tps = desc.get(topicName).partitions().stream()
                .map(p -> new TopicPartition(topicName, p.partition()))
                .toList();

        // 2) earliest & latest offsets 조회 요청 준비
        Map<TopicPartition, OffsetSpec> earliestReq = tps.stream()
                .collect(toMap(tp -> tp, tp -> OffsetSpec.earliest()));
        Map<TopicPartition, OffsetSpec> latestReq = tps.stream()
                .collect(toMap(tp -> tp, tp -> OffsetSpec.latest()));

        // 3) 실제 요청
        ListOffsetsResult earliestRes = adminClient.listOffsets(earliestReq);
        ListOffsetsResult latestRes   = adminClient.listOffsets(latestReq);

        // 4) 파티션별 (latest – earliest) 합산
        long total = 0;
        for (TopicPartition tp : tps) {
            long begin = earliestRes.partitionResult(tp).get().offset();
            long end   = latestRes.partitionResult(tp).get().offset();
            total += (end - begin);
        }
        return total;
    }

}