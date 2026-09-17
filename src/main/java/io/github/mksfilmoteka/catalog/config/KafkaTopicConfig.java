package io.github.mksfilmoteka.catalog.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.kafka.admin.auto-create", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    @Bean
    public NewTopic filmDeletedTopic(@Value("${app.kafka.topics.film-deleted.name}") String name) {
        return TopicBuilder.name(name).build();
    }
}
