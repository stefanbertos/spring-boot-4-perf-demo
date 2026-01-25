add some message id, correlation id, and grafana tempo for tracing seems with kafka it got slower by design

add default kafka metrics from client to grafana existing dashboard for ibm-mq-consumer and kafka-consumer
kafka_producer_record_send_total
kafka_producer_request_latency_avg
kafka_consumer_fetch_latency_avg
kafka_consumer_records_consumed_total
kafka_consumer_lag


new shared consumer / share group feature in Kafka 4.1
Important: share groups are preview/early access and expected to stabilize around Kafka 4.2+.
So for now:
Great for experimenting and flexible scaling
Not recommended for production workloads that depend on ordering
Traditional parti tioning + consumer groups remains the standard



Kubernetes gotcha 🚨 (important for you)
If you’re on Kubernetes:
network-attached storage (EBS, Ceph, NFS)
→ often the #1 Kafka killer
unpredictable latency
fsync stalls
👉 Kafka really wants local SSDs
(Strimzi, MSK, Confluent all strongly recommend this.)

If Kafka is mission-critical:
👉 local SSD (preferably NVMe) + lots of RAM

pd-balanced (what you likely have now)
Network-attached Persistent Disk
Shared IOPS / throughput
Latency spikes under load
fsync is unpredictable
👉 Works for dev / light load
👉 Risky for production Kafka

pd-ssd (strongly recommended minimum)
Much better IOPS
Lower & more stable latency
Predictable replication & recovery
👉 Baseline for production Kafka on GKE


kind: StorageClass
apiVersion: storage.k8s.io/v1
metadata:
name: kafka-ssd
provisioner: kubernetes.io/gce-pd
parameters:
type: pd-ssd
reclaimPolicy: Delete
volumeBindingMode: WaitForFirstConsumer

bigger disk = more IOPS

if you’re running Kafka and other workloads on the same node pool:
disk contention will hurt Kafka first
page cache eviction kills performance
JVM pauses get worse

👉 Dedicated node pool for Kafka brokers
(non-negotiable in prod)

Local SD (best, but with trade-offs)

Physically attached to the node
Insanely fast
Very stable latency

dev
20–30 GB
prod 100–200 GB
disk size

Red flags to watch in Prometheus 📉
These metrics will scream first:
kafka_log_log_size
kafka_server_replica_manager_under_replicated_partitions
kafka_server_broker_topic_metrics_bytes_in_total
disk usage %
If disk hits ~85%:
👉 Kafka starts protecting itself (throttling)


setup - partition 3
Test1
======
Completed Messages:   5000
Pending Messages:     0
Test Duration:        71.19 seconds
Throughput (TPS):     70.23 msg/sec
───────────────────────────────────────────────────────────────
LATENCY
───────────────────────────────────────────────────────────────
Average Latency:      41577.51 ms
Min Latency:          6274.61 ms
Max Latency:          50832.78 ms

Test2
=====
Completed Messages:   5000
Pending Messages:     0
Test Duration:        45.26 seconds
Throughput (TPS):     110.47 msg/sec
───────────────────────────────────────────────────────────────
LATENCY
───────────────────────────────────────────────────────────────
Average Latency:      19678.42 ms
Min Latency:          61.67 ms
Max Latency:          24202.09 ms

setup partitions 20, 20GB disk
Test3
======
Completed Messages:   5000
Pending Messages:     0
Test Duration:        75.25 seconds
Throughput (TPS):     66.45 msg/sec
───────────────────────────────────────────────────────────────
LATENCY
───────────────────────────────────────────────────────────────
Average Latency:      46569.86 ms
Min Latency:          3293.34 ms
Max Latency:          64851.20 ms

Test4
=====
Completed Messages:   5000
Pending Messages:     0
Test Duration:        48.48 seconds
Throughput (TPS):     103.14 msg/sec
───────────────────────────────────────────────────────────────
LATENCY
───────────────────────────────────────────────────────────────
Average Latency:      13607.99 ms
Min Latency:          39.79 ms
Max Latency:          29072.11 ms

added more cpu and memory to ibm consumers
Test5
=====
Completed Messages:   5000
Pending Messages:     0
Test Duration:        47.35 seconds
Throughput (TPS):     105.60 msg/sec
───────────────────────────────────────────────────────────────
LATENCY
───────────────────────────────────────────────────────────────
Average Latency:      741.98 ms
Min Latency:          31.53 ms
Max Latency:          6265.42 ms


removing unused percentiles
Test6
=======
Completed Messages:   5000
Pending Messages:     0
Test Duration:        56.83 seconds
Throughput (TPS):     87.98 msg/sec

───────────────────────────────────────────────────────────────
LATENCY
───────────────────────────────────────────────────────────────
Average Latency:      946.89 ms
Min Latency:          26.75 ms
Max Latency:          5746.95 ms

Test7
======
Completed Messages:   5000
Pending Messages:     0
Test Duration:        32.06 seconds
Throughput (TPS):     155.93 msg/sec

───────────────────────────────────────────────────────────────
LATENCY
───────────────────────────────────────────────────────────────
Average Latency:      203.14 ms
Min Latency:          19.02 ms
Max Latency:          1129.98 ms