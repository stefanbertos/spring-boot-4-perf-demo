add owasp scanner for java code as part of the build, add report etc
fix redis commander http://localhost/redis-commander Whitelabel Error Page
refactor GrafanaExportService to export all dashboards not having DASHBOARDS constant
fix places in the code where we dont use var keywork and we can
fix the places in the code where we dont use @@RequiredArgsConstructor and add it to CLAUDE.md as new rule
check the CLAUDE.md if all files are matching the rules and fix it
refactor KubernetesService so it doesnt use KubernetesNodeInfo but exports everything into file which is available from KubernetesClient and adds it into the zip package
check sonar reports, why the projects dont run sonar scan as part of the build?
add deployment with argocd instead of deploy script, but keep helm charts if possible
add component spring test with compose and spring docker support and run test e2e, the component test should use the gherkin language and steps implemented in java
add same components test for all major components like api gateway, consumers
add some docker image violation check as part of the pipeline
fix grafana dashboards in helm
add ui with react which would call rest service in bff