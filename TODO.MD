helm grafana dashboards pod filter doesnt populate with pod values so all panels are empty, check and fix it

make sure TestResultPersistenceService is used to store result to db, currently its unused, make sure the rest controller, has endpoint to query it

add ui with react which would call rest service to display the data from database about the tests

create a plan and write to file first how to do analysis for current tps e2e
is 111 ops/s in rate(jms_message_process_seconds_count{job="perf-tester"}[1m]) 
check the code why i cannot go higher is it s problem of cpu, memory, connection in mq, 
kafka or other blocking operation, is it a problem if kafka partitions? or disk is not fast enough? 
how can i find the bottleneck if metrics are not sufficient? should I use distributed tracing?

add deployment with argocd instead of deploy script, but keep helm charts if possible
is it possible to run locally github actions? and then argocd?

add component spring test with compose and spring docker support and run test e2e, the component test should use the gherkin language and steps implemented in java
add same components test for all major components like api gateway, consumers
add some docker image violation check as part of the pipeline

