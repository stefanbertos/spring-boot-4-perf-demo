ollama launch claude --config

fix ingress in api gateway http://localhost/kafdrop kafdrop returns Whitelabel Error Page, http://localhost/loki loki 404 page not found possible config in api gateway
fix grafana dashboards
run test 50000 messages
add spring config server for all resources
add deployment with argocd
add some image violation check
add E2E automated tests with gherkin etc, add smoke tests to check that all services are working

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