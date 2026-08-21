# Compose: AAAX + Kafka + notify consumer

> **Prefer the v0.6 golden path:** [../compose-mesh/](../compose-mesh/)  
> (Postgres + Redis OTP + Kafka + **HMAC webhook** + sample-notify)

This folder remains a **lighter** Kafka-only demo.

```text
login / OTP  →  AAAX Event Bus  →  Kafka topic aaax.identity.events
                                      ↓
                              sample-notify (prints JSON)
```

## Run

```bash
mvn -DskipTests package
cd examples/compose-kafka-notify
docker compose up --build
```

See [compose-mesh](../compose-mesh/) for Redis + signed webhook.
