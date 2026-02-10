current tps e2e is 111 ops/s in rate(jms_message_process_seconds_count{job="perf-tester"}[1m]) check the code why i cannot go higher


add cadadvisor metrics and enhance grafana dashboards for perf-tester.json,ibm-mq-consumer.json,kafka-consumer.json also when the app is using kafka add kafka lag panel

add deployment with argocd instead of deploy script, but keep helm charts if possible
is it possible to run locally github actions? and then argocd?

add component spring test with compose and spring docker support and run test e2e, the component test should use the gherkin language and steps implemented in java
add same components test for all major components like api gateway, consumers
add some docker image violation check as part of the pipeline
add ui with react which would call rest service in bff
