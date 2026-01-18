@echo off
setlocal

set NAMESPACE=perf-demo
set RELEASE_PREFIX=perf

echo ============================================
echo  Port Forwarding for Perf-Demo
echo ============================================
echo.
echo Starting port forwards in background...
echo.

:: Start port forwards
start "IBM MQ" cmd /c "kubectl port-forward svc/%RELEASE_PREFIX%-ibm-mq 1414:1414 9443:9443 9157:9157 -n %NAMESPACE%"
start "Prometheus" cmd /c "kubectl port-forward svc/%RELEASE_PREFIX%-prometheus 9090:9090 -n %NAMESPACE%"
start "Grafana" cmd /c "kubectl port-forward svc/%RELEASE_PREFIX%-grafana 3000:3000 -n %NAMESPACE%"
start "Perf-Tester" cmd /c "kubectl port-forward svc/%RELEASE_PREFIX%-perf-tester 8080:8080 -n %NAMESPACE%"

echo Port forwards started:
echo   - IBM MQ:      localhost:1414 (MQ), localhost:9443 (Web), localhost:9157 (Metrics)
echo   - Prometheus:  localhost:9090
echo   - Grafana:     localhost:3000
echo   - Perf-Tester: localhost:8080
echo.
echo Close the opened command windows to stop port forwarding.
echo.

endlocal
