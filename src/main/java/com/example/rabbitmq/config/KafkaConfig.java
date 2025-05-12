package com.example.rabbitmq.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 프로듀서, 컨슈머, 토픽 설정을 담당하는 구성 클래스입니다.
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    /**
     * Kafka 클러스터 주소를 application.yml에서 주입
     */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * 사용할 토픽 이름을 application.yml에서 주입
     */
    @Value("${spring.kafka.topic}")
    private String topic;

    /**
     * 컨슈머 그룹 아이디를 application.yml에서 주입
     */
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /**
     * 프로듀서 성능 및 안정성 설정을 위한 기본 프로퍼티 맵을 생성합니다.
     * - 부트스트랩 서버
     * - 직렬화 클래스
     * - 아이디임포턴스(Exactly-once) 설정
     * - ACK 설정
     * - 무제한 재시도
     */
    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        return props;
    }

    /**
     * ProducerFactory를 생성하여 KafkaTemplate이 이를 사용하도록 합니다.
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    /**
     * KafkaTemplate 빈을 생성하여 서비스에서 메시지 전송에 사용할 수 있게 합니다.
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * 컨슈머 설정용 프로퍼티 맵을 생성합니다.
     * - 부트스트랩 서버
     * - 그룹 아이디
     * - 역직렬화 클래스
     * - 수동 커밋
     * - 트랜잭션 격리 레벨(read_committed)
     */
    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        return configProps;
    }

    /**
     * ConsumerFactory를 생성하여 KafkaListenerContainerFactory가 사용하게 합니다.
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    /**
     * ConcurrentKafkaListenerContainerFactory 설정:
     * - 컨슈머 팩토리 주입
     * - 동시성(Consumer 쓰레드 수) 설정
     * - 폴링 타임아웃
     * - 수동 ACK 모드
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(8);
        factory.getContainerProperties().setPollTimeout(3000);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    /**
     * 토픽이 없을 경우 자동 생성하도록 설정:
     * - 파티션 수
     * - 레플리카 수
     * - 데이터 보존 기간(retention.ms)
     */
    @Bean
    public NewTopic topic() {
        long retentionMs = Duration.ofDays(1).toMillis();
        return TopicBuilder.name(topic)
                .partitions(8)
                .replicas((short) 1)
                .config("retention.ms", String.valueOf(retentionMs))
                .build();
    }
}