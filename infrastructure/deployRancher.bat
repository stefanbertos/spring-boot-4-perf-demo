@echo off
setlocal enabledelayedexpansion

echo ============================================
echo  Helm Deployment Script (Rancher Desktop)
echo ============================================
echo.

set NS=perf-demo
set HELM=%~dp0helm

:: Check prerequisites
where kubectl >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: kubectl is not installed or not in PATH
    exit /b 1
)
where helm >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: helm is not installed or not in PATH
    exit /b 1
)

:: ============================================
:: Step 1: Build application and Docker images
:: ============================================
echo [1/9] Building application and Docker images...
pushd "%~dp0.."
call gradlew.bat build -x componentTest -x generateDocs
if %errorlevel% neq 0 (
    echo ERROR: Gradle build failed
    popd
    exit /b 1
)
call gradlew.bat dockerBuildAll
if %errorlevel% neq 0 (
    echo ERROR: Docker image build failed
    popd
    exit /b 1
)
popd
echo      Done.
echo.

:: ============================================
:: Step 2: Create namespace
:: ============================================
echo [2/9] Creating namespace %NS%...
kubectl create namespace %NS% --dry-run=client -o yaml | kubectl apply -f -
echo      Done.
echo.

:: ============================================
:: Step 3: Wave 0 - Infrastructure
:: ============================================
echo [3/9] Wave 0: Installing infrastructure (IBM MQ, Kafka, Redis, Postgres)...
helm upgrade --install perf-ibm-mq "%HELM%\ibm-mq" -n %NS%
helm upgrade --install perf-kafka "%HELM%\kafka" -n %NS%
helm upgrade --install perf-redis "%HELM%\redis" -n %NS%
helm upgrade --install perf-postgres "%HELM%\postgres" -n %NS%
echo      Waiting 30s for infrastructure to start...
timeout /t 30 /nobreak >nul
echo      Done.
echo.

:: ============================================
:: Step 4: Wave 1 - Monitoring
:: ============================================
echo [4/9] Wave 1: Installing monitoring (Prometheus, Grafana, Loki, Promtail, cAdvisor, SonarQube, Trivy)...
helm upgrade --install perf-prometheus "%HELM%\prometheus" -n %NS%
helm upgrade --install perf-grafana "%HELM%\grafana" -n %NS%
helm upgrade --install perf-loki "%HELM%\loki" -n %NS%
helm upgrade --install perf-promtail "%HELM%\promtail" -n %NS%
helm upgrade --install perf-cadvisor "%HELM%\cadvisor" -n %NS%
helm upgrade --install perf-sonarqube "%HELM%\sonarqube" -n %NS%
helm repo add aquasecurity https://aquasecurity.github.io/helm-charts 2>nul
helm repo update aquasecurity >nul
helm upgrade --install perf-trivy-operator aquasecurity/trivy-operator --version 0.27.0 -n %NS% ^
  --set trivy.ignoreUnfixed=true ^
  --set targetNamespaces=%NS% ^
  --set operator.scanJobsConcurrentLimit=3
echo      Done.
echo.

:: ============================================
:: Step 5: Wave 2 - Config Server
:: ============================================
echo [5/9] Wave 2: Installing config server...
helm upgrade --install perf-config-server "%HELM%\config-server" -n %NS% ^
  --set image.repository=config-server ^
  --set image.tag=latest
echo      Waiting 20s for config server to start...
timeout /t 20 /nobreak >nul
echo      Done.
echo.

:: ============================================
:: Step 6: Wave 3 - Applications
:: ============================================
echo [6/9] Wave 3: Installing applications (ibm-mq-consumer, kafka-consumer, perf-tester)...
helm upgrade --install perf-ibm-mq-consumer "%HELM%\ibm-mq-consumer" -n %NS% ^
  --set image.repository=ibm-mq-consumer ^
  --set image.tag=latest
helm upgrade --install perf-kafka-consumer "%HELM%\kafka-consumer" -n %NS% ^
  --set image.repository=kafka-consumer ^
  --set image.tag=latest
helm upgrade --install perf-perf-tester "%HELM%\perf-tester" -n %NS% ^
  --set image.repository=perf-tester ^
  --set image.tag=latest
echo      Done.
echo.

:: ============================================
:: Step 7: Wave 4 - Tools
:: ============================================
echo [7/9] Wave 4: Installing tools (Kafdrop, Redis Commander, Kafka Exporter)...
helm upgrade --install perf-kafdrop "%HELM%\kafdrop" -n %NS%
helm upgrade --install perf-redis-commander "%HELM%\redis-commander" -n %NS%
helm upgrade --install perf-kafka-exporter "%HELM%\kafka-exporter" -n %NS%
echo      Done.
echo.

:: ============================================
:: Step 8: Wave 5 - API Gateway
:: ============================================
echo [8/9] Wave 5: Installing API Gateway...
helm upgrade --install perf-api-gateway "%HELM%\api-gateway" -n %NS% ^
  --set image.repository=api-gateway ^
  --set image.tag=latest
echo      Done.
echo.

:: ============================================
:: Step 9: Wave 6 - Ingress
:: ============================================
echo [9/9] Wave 6: Installing Ingress...
helm upgrade --install perf-ingress "%HELM%\ingress" -n %NS%
echo      Done.
echo.

:: Detect ingress IP
set INGRESS_HOST=localhost
for /f "tokens=*" %%h in ('kubectl get ingress perf-demo-ingress -n %NS% -o jsonpath="{.status.loadBalancer.ingress[0].ip}" 2^>nul') do (
    if not "%%h"=="" set INGRESS_HOST=%%h
)

echo ============================================
echo  Deployment Complete!
echo ============================================
echo.
echo Service URLs (via Ingress):
echo.
echo   Perf UI          http://%INGRESS_HOST%/
echo   Perf Tester API  http://%INGRESS_HOST%/api
echo   Grafana          http://%INGRESS_HOST%/grafana          (admin / admin)
echo   Prometheus       http://%INGRESS_HOST%/prometheus
echo   Kafdrop          http://%INGRESS_HOST%/kafdrop
echo   IBM MQ Console   http://%INGRESS_HOST%/ibmmq            (admin / passw0rd)
echo   Redis Commander  http://%INGRESS_HOST%/redis-commander
echo   SonarQube        http://%INGRESS_HOST%/sonar            (admin / admin)
echo.
echo To clean up, run: cleanupRancher.bat
echo.

endlocal
