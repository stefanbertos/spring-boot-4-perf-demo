add owasp scanner for java code as part of the build, add report etc
check sonar reports, why the projects dont run sonar scan as part of the build?
add deployment with argocd instead of deploy script, but keep helm charts if possible
add component spring test with compose and spring docker support and run test e2e, the component test should use the gherkin language and steps implemented in java
add same components test for all major components like api gateway, consumers
add some docker image violation check as part of the pipeline
fix grafana dashboards in helm
add ui with react which would call rest service in bff


[2026-02-08  18:19:52]	2026-02-08T17:19:52.577Z  WARN 1 --- [perf-tester] [  scheduling-56] c.e.p.health.HealthCheckScheduler        : Kafka health check failed: Connection refused
[2026-02-08  18:19:52]	2026-02-08T17:19:52.577Z  WARN 1 --- [perf-tester] [  scheduling-56] c.e.p.health.HealthCheckScheduler        : IBM MQ health check failed: Connection refused
[2026-02-08  18:19:52]	2026-02-08T17:19:52.577Z  WARN 1 --- [perf-tester] [  scheduling-56] c.e.p.health.HealthCheckScheduler        : Oracle health check failed: Connection refused


check the CLAUDE.md rules and make sure those rules are as well in checkstyle and pmd, then rebuild the code and fix violations if found