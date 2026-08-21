# Identity events example

AAAX emits CloudEvents-ish JSON on the **Identity Event Bus**.

## Watch live buffer (no Kafka)

```bash
# after admin session cookie / or use curl with login:
./examples/curl/login-admin-and-events.sh
```

## Kafka consumer (Python)

```bash
export KAFKA_BOOTSTRAP=localhost:9092
export AAAX_EVENTS_TOPIC=aaax.identity.events
python3 examples/identity-events/consumer.py
```

## Enable Kafka on AAAX

```bash
export AAAX_EVENTS_KAFKA_ENABLED=true
export AAAX_EVENTS_KAFKA_TOPIC=aaax.identity.events
export AAAX_EVENTS_KAFKA_BOOTSTRAP=localhost:9092
mvn spring-boot:run
```

## Webhook

```bash
export AAAX_EVENTS_WEBHOOK_URL=http://127.0.0.1:9999/hook
# run any HTTP dump server, then login on AAAX
```

Spec: [docs/booklet.md#15-identity-event-bus](../../docs/booklet.md#15-identity-event-bus)
