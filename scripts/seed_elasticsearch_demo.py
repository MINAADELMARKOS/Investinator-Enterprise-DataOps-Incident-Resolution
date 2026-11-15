"""Seed synthetic KRaft cross-service failure logs into a REAL local Elasticsearch index.

The log backend is real; the failure data are deliberately SIMULATED.
Never describe these documents as observed production incidents.
"""
import os
from datetime import datetime, timedelta, timezone

import httpx


def main() -> None:
    endpoint = os.getenv("INVESTINATOR_ELASTICSEARCH_URL", "http://127.0.0.1:9200").rstrip("/")
    index = os.getenv("INVESTINATOR_ELASTICSEARCH_INDEX", "logs-investinator-demo")
    if index != "logs-investinator-demo":
        raise SystemExit("Refusing to seed a non-demo index")
    now = datetime.now(timezone.utc)
    events = [
        ("DEMO-NETWORK-01", "network-monitor", 360,
         "Kafka KRaft controller-01 NIC reports packet loss and intermittent connection resets"),
        ("DEMO-QUORUM-01", "kraft-controller", 300,
         "Kafka KRaft metadata quorum heartbeat timed out; controller leader temporarily unavailable"),
        ("DEMO-BROKER-01", "kafka-broker", 240,
         "Kafka broker metadata update delayed due to KRaft controller connectivity failures"),
        ("DEMO-CONSUMER-01", "kafka-consumer", 180,
         "Kafka consumers experience fetch retries and increasing consumer lag on orders.events"),
        ("DEMO-PIPELINE-01", "spark-streaming", 120,
         "Spark streaming input delay rising after Kafka consumer fetch retries"),
        ("DEMO-DATA-01", "data-quality", 60,
         "Dataset fact_orders freshness SLA breached due to upstream Kafka streaming delay"),
    ]
    with httpx.Client(timeout=8.0) as client:
        ping = client.get(endpoint)
        ping.raise_for_status()
        mapping = {
            "mappings": {"properties": {
                "@timestamp": {"type": "date"},
                "tenant_id": {"type": "keyword"},
                "source_mode": {"type": "keyword"},
                "service": {"properties": {"name": {"type": "keyword"}}},
                "message": {"type": "text"},
                "environment": {"type": "keyword"},
            }},
        }
        created = client.put(f"{endpoint}/{index}", json=mapping)
        if created.status_code not in (200, 400):
            created.raise_for_status()
        for doc_id, service, age, message in events:
            payload = {
                "@timestamp": (now - timedelta(seconds=age)).isoformat(),
                "tenant_id": "demo", "source_mode": "simulated",
                "environment": "ReplayLab / simulated KRaft failure",
                "service": {"name": service},
                "message": message,
            }
            response = client.put(f"{endpoint}/{index}/_doc/{doc_id}", json=payload)
            response.raise_for_status()
        refreshed = client.post(f"{endpoint}/{index}/_refresh")
        refreshed.raise_for_status()
    print(f"Seeded {len(events)} SIMULATED KRaft failure events into {index}.")


if __name__ == "__main__":
    main()
