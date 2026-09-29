# 9. Kafka with Spring for Apache Kafka

## 9.1 Concepts in 60 seconds

- **Topic** = named, append-only log, split into **partitions** (parallelism unit).
- **Key** → partition (hash). Same key ⇒ same partition ⇒ **ordered** for that key. We key by `userId` / `applicationId`.
- **Consumer group** = the instances of one service; each partition is consumed by exactly one instance in the group. Different groups each get *all* messages (fan-out).
- **Offset** = position in a partition; committed after successful processing. Delivery is **at-least-once** → consumers must be **idempotent**.
- Retention is time/size based — Kafka is not a queue that deletes on consume.

Topic naming here: `homefin.<domain>.<event>.v<version>` (`common-lib/.../Topics.java`). Events are immutable, past-tense facts (`UserRegistered`, not `CreateCustomer`) carrying an `eventId` and `occurredAt`.

## 9.2 Producing

```java
private final KafkaTemplate<String, Object> kafkaTemplate;

kafkaTemplate.send(Topics.USER_REGISTERED, userId.toString(), event)        // topic, key, value
    .whenComplete((result, ex) -> { if (ex != null) log.error(...); });      // async result
```

Config (`application.yml`): `StringSerializer` for keys, Spring's `JsonSerializer` for values, `acks: all` (+ idempotent producer, default in modern clients) so an acknowledged write isn't lost. Topics are declared as `NewTopic` beans (`KafkaTopicsConfig`) and created at startup by `KafkaAdmin`; in production topics are usually managed by the platform team (Terraform, Strimzi...).

**Publish after commit** (`@TransactionalEventListener(AFTER_COMMIT)` in `UserEventsPublisher`): never announce data that might roll back.

## 9.3 Consuming

```java
@KafkaListener(topics = Topics.USER_REGISTERED)       // group id from spring.kafka.consumer.group-id
public void onUserRegistered(@Payload UserRegisteredEvent event,
                             @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                             @Header(KafkaHeaders.OFFSET) long offset) {
    customerService.createFromRegistration(event);
}
```

Deserialization config (customer-service):

```yaml
value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
properties:
  spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer
  spring.json.trusted.packages: com.homefin.common.events
```

`ErrorHandlingDeserializer` wraps the JSON deserializer so a malformed message becomes an error you can dead-letter, instead of an infinite crash loop. `trusted.packages` prevents deserializing arbitrary classes named in headers. The producer adds a `__TypeId__` header with the class name; to decouple class names between services, configure `spring.json.type.mapping` (`userRegistered:com.x.UserRegisteredEvent`) on both sides, or use **Avro/Protobuf + Schema Registry** (common in banks) for enforced schema evolution.

Scaling: `spring.kafka.listener.concurrency: 3` (threads per instance) — useful up to the partition count.

## 9.4 Idempotent consumers

Because of at-least-once delivery and rebalances, the same event *will* arrive twice sometimes:

```java
if (customers.existsByUserId(event.userId())) { return; }   // business-key check
customers.saveAndFlush(...);                                   // + unique index on user_id as the real guard
```

Alternatives: a `processed_events(event_id)` table written in the same transaction as the business change.
Test it: `UserRegisteredListenerIT` sends the same event twice and asserts exactly one row.

## 9.5 Errors, retries & dead-letter topics

`customer-service/config/KafkaConfig.java`:

```java
var recoverer = new DeadLetterPublishingRecoverer(templates,
        (record, ex) -> new TopicPartition(record.topic() + ".DLT", -1));
var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3L));   // 3 retries, 1s apart
handler.addNotRetryableExceptions(IllegalArgumentException.class, ValidationException.class);
```

- Transient failures (DB blip) → retried in place.
- Still failing / not retryable → published to `<topic>.DLT` with exception headers; the partition keeps flowing.
- Monitor DLTs (Kafka UI → <http://localhost:8090>) and have a replay procedure.
- Blocking retries hold up the partition; for long back-offs use **non-blocking retries** (`@RetryableTopic`, which creates `-retry-N` topics).

## 9.6 Ordering & exactly-once, realistically

Ordering is only per partition → choose keys carefully. "Exactly-once" in Kafka applies to Kafka-to-Kafka flows with transactions; when a DB is involved, design for **at-least-once + idempotency**.

## 9.7 The dual-write problem & Transactional Outbox

`save to DB` then `send to Kafka` are two systems — if the process dies in between, the event is lost (our publish-after-commit accepts that risk for simplicity). The robust solution is the **Transactional Outbox**:

1. In the *same* DB transaction, insert the business row **and** an `outbox` row with the event.
2. A relay publishes outbox rows to Kafka (polling publisher, or CDC with **Debezium** reading the DB log) and marks them sent.

Libraries: Spring Modulith's event publication registry, Debezium outbox event router, or a small scheduled poller.

## 9.8 Testing

- Integration test with a real broker: Testcontainers `KafkaContainer` + `@ServiceConnection` (`UserRegisteredListenerIT`), assert with **Awaitility** (consumption is async).
- `spring-kafka-test` also offers `@EmbeddedKafka` (in-JVM broker) if Docker isn't available.
- Unit-test the listener's logic by calling the service method directly.

## 9.9 Hands-on

```bash
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
   --topic homefin.auth.user-registered.v1 --from-beginning --property print.key=true --property print.headers=true
```

## Sources
- Spring for Apache Kafka reference: <https://docs.spring.io/spring-kafka/reference/>
- Error handling & DLT: <https://docs.spring.io/spring-kafka/reference/kafka/annotation-error-handling.html>
- Non-blocking retries: <https://docs.spring.io/spring-kafka/reference/retrytopic.html>
- Spring Boot Kafka properties: <https://docs.spring.io/spring-boot/reference/messaging/kafka.html>
- Apache Kafka documentation: <https://kafka.apache.org/documentation/>
- Transactional outbox pattern: <https://microservices.io/patterns/data/transactional-outbox.html>
- Debezium outbox: <https://debezium.io/documentation/reference/stable/transformations/outbox-event-router.html>
- *Kafka: The Definitive Guide*, 2nd ed. (O'Reilly)
