it seems the the latency has increased by adding the kafka stuff, what can be done to improve it?
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