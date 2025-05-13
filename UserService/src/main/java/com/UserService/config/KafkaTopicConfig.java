package com.UserService.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic teamCreatedTopic() {
        return new NewTopic("team-created", 1, (short) 1);
    }

    @Bean
    public NewTopic teamUpdatedTopic() {
        return new NewTopic("team-updated", 1, (short) 1);
    }

    @Bean
    public NewTopic teamDeletedTopic() {
        return new NewTopic("team-deleted", 1, (short) 1);
    }

    @Bean
    public NewTopic teamMemberAddedTopic() {
        return new NewTopic("team-member-added", 1, (short) 1);
    }

    @Bean
    public NewTopic teamMemberRemovedTopic() {
        return new NewTopic("team-member-removed", 1, (short) 1);
    }
} 