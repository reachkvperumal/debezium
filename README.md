# 1. <ins>How Debezium captures changes from Azure SQL.</ins>
> Debezium uses the SQL Server Connector to capture changes from Azure SQL. It works by:
> Reading the transaction log (also called the change data capture (CDC) log) of the database.
> Emitting change events for every insert, update, or delete operation on the monitored tables.
> Sending these events to a configured destination (e.g., Kafka or directly to your application).
>Each change event contains:
>Key: The primary key of the changed row.
>Value: The before and after state of the row, along with metadata (e.g., operation type, timestamp).
``` json
{
  "key": "primary",
  "value": {
    "before": null,
    "after": {
      "id": 1001,
      "name": "KV Perumal",
      "email": "kv.perumal@google.com"
    },
    "op": "c", // 'c' for create/insert
    "ts_ms": 1739141098833
  }
}
```
# 2. <ins>Configuring Debezium to emit changes directly to your application.</ins>
> Instead of sending change events to Kafka, Debezium can be embedded directly in your Spring Boot application using the Debezium Embedded Engine. 
> This allows you to process change events in real-time without relying on Kafka.

<ins>To configure Debezium for Azure SQL:</ins>

- Add the Debezium SQL Server Connector dependency.

- Configure Debezium to connect to Azure SQL and monitor specific tables.

- Use the Debezium Embedded Engine to emit change events directly to your application.

# 3. <ins> Setting up Spring Integration to consume and process Debezium events.</ins>
Spring Integration can be used to process Debezium events in real-time. 

<ins>The key steps are:</ins>

- Configure Debezium to capture changes from Azure SQL and emit them to your application.
- Define an IntegrationFlow to process the change events.
- Handle the processed events (e.g., log, store, or send to another system).

# Properties definition in [DebeziumConfig.java](https://github.com/reachkvperumal/debezium/blob/main/src/main/java/com/kv/debezium/example/cdc_example/configuration/DebeziumConfig.java)

name: This property uniquely identifies the connector instance. 
      This is especially important if you have multiple connectors running in the same application or environment

connector.class: Specifies the Debezium connector to use (e.g., SqlServerConnector for Azure SQL).

database.hostname: The hostname of the Azure SQL database.

database.port: The port of the Azure SQL database (default is 1433).

database.user: The username for connecting to the database.

database.password: The password for connecting to the database.

database.dbname: The name of the database to monitor.

database.server.name: A logical name for the database server .

table.include.list: Specifies the tables to monitor (e.g., dbo.your_table_name).


|Property  |	MySQL Connector  | 	SQL Server Connector  |
|--------- |-------------------- |------------------------|
|database.server.id |	Required | 	Not Required          |

> [!CAUTION]
> For database history, file-based schema history is not durable.
> For large or frequently changing schemas, file-based storage may become a bottleneck. 
> Kafka-based storage is more scalable and performant.
> For local testing file based is preferred.

> [!IMPORTANT]
> When you move to a production environment, you can switch to Kafka-based schema history by updating the configuration
> database.history = io.debezium.relational.history.KafkaDatabaseHistory
> database.history.kafka.bootstrap.servers=${bootStrapServers}
> database.history.kafka.topic=${kafka.topicName}

database.history: [schema-history.txt](src/main/resources/db/schema-history.txt) 

- Debezium uses the FileDatabaseHistory class to store the schema history in the specified file.

- The schema history file contains a log of all schema changes (e.g., table creation, column additions, etc.).

- When the connector starts, it reads the schema history from the file to reconstruct the schema as it was at the time of each change.

## Configuration details - [DebeziumConfig](https://github.com/reachkvperumal/debezium/blob/main/src/main/java/com/kv/debezium/example/cdc_example/configuration/DebeziumConfig.java)
> This class is the backbone of the integration between Debezium and Spring Integration, 
> enabling real-time change data capture (CDC) from Azure SQL and forwarding the events to Kafka.

**Configures Debezium:**
- Sets up the Debezium engine to capture changes from Azure SQL.
- Specifies the database connection details, tables to monitor, and Kafka settings.

**Processes Change Events:**
- Captures change events using the handleEvent method.
- Sends these events to a Spring Integration channel for further processing.

**Integrates with Spring Integration:**
- Uses a DirectChannel to connect Debezium with the Spring Integration flow.
- Enables seamless processing of change events in the Spring ecosystem.

**Runs Debezium Asynchronously:**
- Uses an Executor to start the Debezium engine in a separate thread.

## Integration flow - [IntegrationConfig](https://github.com/reachkvperumal/debezium/blob/main/src/main/java/com/kv/debezium/example/cdc_example/configuration/IntegrationConfig.java)
- `IntegrationFlow` consumes events from the debeziumEventsChannel.
- `.transform()` method processes each event. In this example, it transforms to json.
- `.handle()` method processes payload. In this example, it posts with Kafka topic.

### Summary
- Use the Debezium Embedded Engine to capture changes from Azure SQL and emit them directly to your application.
- Use Spring Integration to process the change events in real-time and post with target Kafka topic.
- This approach avoids the need for interim Kafka and provides a lightweight, embedded solution for CDC.
