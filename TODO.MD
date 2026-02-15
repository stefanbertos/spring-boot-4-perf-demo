grafana dashboards are wrong for

change of debug level doesnt work 
[2026-02-15  11:24:55]	2026-02-15T10:24:55.718Z  INFO 1 --- [perf-tester] [at-handler-1167] c.e.p.admin.LoggingAdminService          : Set log level for 'com.example' to DEBUG
[2026-02-15  11:24:55]	2026-02-15T10:24:55.718Z  INFO 1 --- [perf-tester] [at-handler-1167] c.e.perftester.rest.PerfController       : Debug mode enabled for 'com.example' (previous level: INFO)

remove rest swagger ui

put grafana, prometheus dashboards as iframes into perf tester

add possibility to upload test templates and maintanance

add integration into jira to create a automatic ticket

add external api or enhance the current one so as well some other automation tool can call the perf tester like robo framework etc and perform the test, get results etc

use select for update, limit, skip and proper annotations