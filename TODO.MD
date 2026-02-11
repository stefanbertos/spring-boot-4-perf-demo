modify the build.yml for github actions to include the modules build/reports/checstyle/main.html then jacoco test html report folder,
pmd main.html, and tests folders for all 4 modules so it can be uploaded as artifacts.

create a plan and write to file first how to do analysis for current tps e2e is 111 ops/s in rate(jms_message_process_seconds_count{job="perf-tester"}[1m]) check the code why i cannot go higher is it s problem of cpu, memory, connection in mq, kafka or other blocking operation, is it a problem if kafka partitions? or disk is not fast enough? how can i find the bottleneck if metrics are not sufficient? should I use distributed tracing?

add cadadvisor metrics and enhance grafana dashboards for perf-tester.json,ibm-mq-consumer.json,kafka-consumer.json also when the app is using kafka add kafka lag panel
check my grafana dashboards if those are using best practives like filters, templates, interval variables, proper legends, naming, colors


add deployment with argocd instead of deploy script, but keep helm charts if possible
is it possible to run locally github actions? and then argocd?

add component spring test with compose and spring docker support and run test e2e, the component test should use the gherkin language and steps implemented in java
add same components test for all major components like api gateway, consumers
add some docker image violation check as part of the pipeline
add ui with react which would call rest service in bff
