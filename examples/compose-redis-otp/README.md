# Multi-node OTP / magic tokens with Redis

Minimal add-on for `aaax.otp.store=redis`.

```bash
# from repo root after: mvn -DskipTests package
docker compose -f examples/compose-redis-otp/docker-compose.yml up --build
```

```bash
export AAAX_OTP_STORE=redis
export SPRING_DATA_REDIS_HOST=localhost
# AAAX on host talking to published redis:6379
```

Docs: [../redis-otp-store.md](../redis-otp-store.md)
