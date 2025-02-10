package com.kv.debezium.example.cdc_example.configuration;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class DebeziumConfig {


    @Value("${debezium.database.hostname}")
    private String hostname;

    @Value("${debezium.database.port}")
    private String port;

    @Value("${debezium.database.user}")
    private String user;

    @Value("${debezium.database.secret}")
    private String secret;

    @Value("${debezium.database.dbname}")
    private String dbname;

    @Value("${debezium.database.table_name}")
    private String tableName;

    @Value("${debezium.database.server.name}")
    private String serverName;

    @Value("${debezium.connector.class}")
    private String connectorClass; //io.debezium.connector.sqlserver.SqlServerConnector

    @Value("${debezium.connector-name}")
    private String connectorName;

    @Value("${debezium.database.history}")
    private String history;

    @Value("${debezium.database.history_file}")
    private String historySrc;

    @Bean
    public MessageChannel debeziumEventsChannel() {
        return new DirectChannel(); // Define the channel
    }

    @Bean
    public DebeziumEngine<ChangeEvent<String, String>> debeziumEngine(MessageChannel debeziumEventsChannel) {
        // Configure Debezium properties
        Properties props = new Properties();
        props.setProperty("name", connectorName); // have unique name
        props.setProperty("connector.class", connectorClass);
        props.setProperty("database.hostname", hostname);
        props.setProperty("database.port", port);
        props.setProperty("database.user", user);
        props.setProperty("database.password", secret);
        props.setProperty("database.dbname", dbname);
        props.setProperty("database.server.name", serverName);
        props.setProperty("table.include.list", tableName); // Include specific table(s)

        //this is POC code change it to Kafka, read the help.md file.
        // File-based schema history configuration
        props.setProperty("database.history", history);
        props.setProperty("database.history.file.filename", historySrc);

        // Create and return DebeziumEngine
        return DebeziumEngine.create(Json.class)
                .using(props)
                .notifying(event -> {
                    // Send the event to the Spring Integration channel
                    Message<ChangeEvent<String, String>> message = MessageBuilder.withPayload(event).build();
                    debeziumEventsChannel.send(message);
                })
                .build();
    }

    @Bean
    public Executor executor() {
        // Use a single-threaded executor to run the Debezium engine
        return Executors.newSingleThreadExecutor();
    }

    @Bean
    public void startDebeziumEngine(DebeziumEngine<ChangeEvent<String, String>> debeziumEngine, Executor executor) {
        // Start the Debezium engine
        executor.execute(debeziumEngine);
    }
}
