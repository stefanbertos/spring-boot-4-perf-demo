add grafana loki for logs and support stuff


use https://grafana.com/grafana/dashboards/15661-k8s-dashboard-en-20250125/
https://grafana.com/grafana/dashboards/1860-node-exporter-full/


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