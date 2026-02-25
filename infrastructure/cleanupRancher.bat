@echo off
setlocal enabledelayedexpansion

echo ============================================
echo  Helm Cleanup Script (Rancher Desktop)
echo ============================================
echo.

set NS=perf-demo

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
:: Step 1: Uninstall all Helm releases (reverse wave order)
:: ============================================
echo [1/2] Uninstalling all Helm releases...

echo      Wave 6: Ingress...
helm uninstall perf-ingress -n %NS% 2>nul

echo      Wave 5: API Gateway...
helm uninstall perf-api-gateway -n %NS% 2>nul

echo      Wave 4: Tools...
helm uninstall perf-kafdrop -n %NS% 2>nul
helm uninstall perf-redis-commander -n %NS% 2>nul
helm uninstall perf-kafka-exporter -n %NS% 2>nul

echo      Wave 3: Applications...
helm uninstall perf-perf-tester -n %NS% 2>nul
helm uninstall perf-kafka-consumer -n %NS% 2>nul
helm uninstall perf-ibm-mq-consumer -n %NS% 2>nul

echo      Wave 2: Config Server...
helm uninstall perf-config-server -n %NS% 2>nul

echo      Wave 1: Monitoring...
helm uninstall perf-trivy-operator -n %NS% 2>nul
helm uninstall perf-sonarqube -n %NS% 2>nul
helm uninstall perf-cadvisor -n %NS% 2>nul
helm uninstall perf-promtail -n %NS% 2>nul
helm uninstall perf-loki -n %NS% 2>nul
helm uninstall perf-grafana -n %NS% 2>nul
helm uninstall perf-prometheus -n %NS% 2>nul

echo      Wave 0: Infrastructure...
helm uninstall perf-postgres -n %NS% 2>nul
helm uninstall perf-redis -n %NS% 2>nul
helm uninstall perf-kafka -n %NS% 2>nul
helm uninstall perf-ibm-mq -n %NS% 2>nul

echo      All releases uninstalled.
echo.

:: ============================================
:: Step 2: Delete namespace
:: ============================================
echo [2/2] Deleting namespace %NS%...
kubectl delete namespace %NS% --ignore-not-found=true
echo      Done.
echo.

echo ============================================
echo  Cleanup Complete!
echo ============================================
echo.
echo To redeploy, run: deployRancher.bat
echo.

endlocal
