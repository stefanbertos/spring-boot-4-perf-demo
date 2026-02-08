add owasp scanner for java code as part of the build, add report etc
check sonar reports, why the projects dont run sonar scan as part of the build?
add deployment with argocd instead of deploy script, but keep helm charts if possible
add component spring test with compose and spring docker support and run test e2e, the component test should use the gherkin language and steps implemented in java
add same components test for all major components like api gateway, consumers
add some docker image violation check as part of the pipeline
fix grafana dashboards in helm
add ui with react which would call rest service in bff

check the CLAUDE.md rules and make sure those rules are as well in checkstyle and pmd, then rebuild the code and fix violations if found