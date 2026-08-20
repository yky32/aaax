# Compose: AAAX + Kafka + notify consumer

**v0.4 production-shaped path** for the Identity Event Bus wedge.

```text
login / OTP  →  AAAX Event Bus  →  Kafka topic aaax.identity.events
                                      ↓
                              sample-notify (prints JSON;
                              replace with your notification-service → SMS)
```

## Run

```bash
# from repo root — build jar first
mvn -DskipTests package

cd examples/compose-kafka-notify
docker compose up --build
```

| Service | Port |
|---------|------|
| AAAX | http://localhost:8081 |
| Kafka | localhost:9092 |
| Postgres | localhost:5433 |
| sample-notify | logs only |

## Smoke

```bash
# health
curl -sS http://localhost:8081/actuator/health

# password login → emits com.aaax.auth.login on Kafka
curl -sS -c /tmp/aaax.cj -X POST http://localhost:8081/v1/auth/login \
  -H 'content-type: application/json' \
  -d '{"username":"admin","password":"admin12345"}'

# watch sample-notify container logs for CloudEvents-ish JSON
docker compose logs -f sample-notify
```

## Env (already in compose)

| | |
|--|--|
| `AAAX_EVENTS_KAFKA_ENABLED` | `true` |
| `AAAX_EVENTS_KAFKA_TOPIC` | `aaax.identity.events` |
| `AAAX_EVENTS_KAFKA_BOOTSTRAP` | `kafka:9092` |
| `AAAX_OTP_CHANNEL` | `kafka` (OTP also on bus) |

## Replace sample-notify

Point your real notification-service at the same topic (or set `AAAX_EVENTS_WEBHOOK_URL` instead of Kafka).

See [docs/IDENTITY_EVENTS.md](../../docs/IDENTITY_EVENTS.md).
