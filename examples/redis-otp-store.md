# Optional Redis store for OTP + magic-link tokens (multi-node)

Use when you run **more than one AAAX instance** and need shared OTP/magic tokens.

## Enable

```bash
export AAAX_OTP_STORE=redis
# or aaax.otp.store=redis

export SPRING_DATA_REDIS_HOST=localhost
export SPRING_DATA_REDIS_PORT=6379
# optional: SPRING_DATA_REDIS_PASSWORD, SPRING_DATA_REDIS_DATABASE
```

Keys:

| Purpose | Redis key |
|---------|-----------|
| OTP | `aaax:otp:{username\|reset:user}` |
| Magic link | `aaax:magic:{token}` |

TTL matches `aaax.otp.ttl-seconds` / `aaax.magic.ttl-seconds`.

## Default

`aaax.otp.store=memory` — in-process `ConcurrentHashMap`. Fine for demo / single node.  
**No Redis connection is opened** unless store=redis (Boot Redis auto-config is excluded).

## Compose sketch

```yaml
services:
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
  aaax:
    environment:
      AAAX_OTP_STORE: redis
      SPRING_DATA_REDIS_HOST: redis
```

See also [compose-kafka-notify](./compose-kafka-notify/) for Event Bus path (orthogonal to token store).
