package com.kv.debezium.example.cdc_example.configuration;

import io.debezium.engine.ChangeEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.kafka.dsl.Kafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.messaging.MessageChannel;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class IntegrationConfig {

    @Value("${kafka.bootstrap.names}")
    private String bootstrapServers;

    @Value("${kafka.topic}")
    private String kafkaTopic;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public IntegrationFlow debeziumToKafkaFlow(MessageChannel debeziumEventsChannel) {
        return IntegrationFlow
                .from(debeziumEventsChannel) // Channel to receive Debezium events
                .transform(event -> {
                    // Format the event (e.g., JSON or custom format)
                    return formatEvent((ChangeEvent<String, String>) event);
                })
                .handle(Kafka.outboundChannelAdapter(kafkaTemplate())
                        .topic(kafkaTopic) // Kafka topic to publish to
                )
                .get();
    }

    private final String resp = """
            {
                "key": "%s",
                "value": "%s"
            }
            """;

    private String formatEvent(ChangeEvent<String, String> event) {
        // Customize the event format as needed
        return String.format(resp, event.key(), event.value());
    }
}
