#!/usr/bin/env python3
"""Minimal Kafka consumer for AAAX identity events (requires kafka-python)."""
import json
import os
import sys

try:
    from kafka import KafkaConsumer
except ImportError:
    print("pip install kafka-python", file=sys.stderr)
    sys.exit(1)

bootstrap = os.environ.get("KAFKA_BOOTSTRAP", "localhost:9092")
topic = os.environ.get("AAAX_EVENTS_TOPIC", "aaax.identity.events")

print(f"listening topic={topic} bootstrap={bootstrap}", flush=True)
consumer = KafkaConsumer(
    topic,
    bootstrap_servers=bootstrap.split(","),
    auto_offset_reset="latest",
    enable_auto_commit=True,
    value_deserializer=lambda b: (b or b"").decode("utf-8"),
)

for msg in consumer:
    try:
        evt = json.loads(msg.value)
        print(
            f"{evt.get('time')}  {evt.get('type')}  subject={evt.get('subject')}  id={evt.get('id')}",
            flush=True,
        )
        if evt.get("data"):
            print(f"  data={json.dumps(evt['data'], ensure_ascii=False)}", flush=True)
    except Exception as ex:
        print(f"raw={msg.value!r} err={ex}", flush=True)
