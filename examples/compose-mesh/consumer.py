#!/usr/bin/env python3
"""Kafka consumer stub — your notification-service stand-in (catalog v1.0)."""
import json
import os
import sys
import time


def main():
    bootstrap = os.environ.get("KAFKA_BOOTSTRAP", "kafka:9092")
    topic = os.environ.get("AAAX_EVENTS_TOPIC", "aaax.identity.events")
    print(f"[sample-notify] waiting for kafka {bootstrap} topic={topic}", flush=True)

    for attempt in range(60):
        try:
            from kafka import KafkaConsumer

            consumer = KafkaConsumer(
                topic,
                bootstrap_servers=bootstrap.split(","),
                auto_offset_reset="earliest",
                enable_auto_commit=True,
                group_id="aaax-sample-notify",
                value_deserializer=lambda b: (b or b"").decode("utf-8"),
            )
            print(f"[sample-notify] listening on {topic}", flush=True)
            break
        except Exception as ex:
            print(f"[sample-notify] connect retry {attempt+1}: {ex}", flush=True)
            time.sleep(2)
    else:
        print("[sample-notify] failed to connect", file=sys.stderr)
        sys.exit(1)

    for msg in consumer:
        raw = msg.value
        try:
            evt = json.loads(raw)
            t = evt.get("type", "?")
            sub = evt.get("subject", "")
            eid = evt.get("id")
            cat = (evt.get("data") or {}).get("catalogVersion", "?")
            print(f"[notify] {t} subject={sub} id={eid} catalog={cat}", flush=True)
            if t == "com.aaax.otp.dispatch":
                data = evt.get("data") or {}
                print(
                    f"  → YOUR SMS/email provider  dest={data.get('destination')} "
                    f"code={data.get('code')} purpose={data.get('purpose')}",
                    flush=True,
                )
            elif data := evt.get("data"):
                print(f"  data={json.dumps(data, ensure_ascii=False)[:240]}", flush=True)
        except Exception as ex:
            print(f"[notify] raw={raw!r} err={ex}", flush=True)


if __name__ == "__main__":
    main()
