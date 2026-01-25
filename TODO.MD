add some message id, correlation id, and grafana tempo for tracing seems with kafka it got slower by design

add kafka startup script to create topic

kafka-topics.sh --create \
--topic mq-requests \
--partitions 10 \
--replication-factor 3

kafka-topics.sh --create \
--topic mq-responses \
--partitions 10 \
--replication-factor 3


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