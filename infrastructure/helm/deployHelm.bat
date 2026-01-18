@echo off
setlocal enabledelayedexpansion

echo ============================================
echo  Perf-Demo Helm Deployment Script
echo ============================================
echo.

set NAMESPACE=perf-demo
set RELEASE_PREFIX=perf

:: Check if helm is installed
where helm >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: Helm is not installed or not in PATH
    exit /b 1
)

:: Check if kubectl is installed
where kubectl >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: kubectl is not installed or not in PATH
    exit /b 1
)

:: Create namespace if it doesn't exist
echo [1/6] Creating namespace %NAMESPACE%...
kubectl create namespace %NAMESPACE% --dry-run=client -o yaml | kubectl apply -f -
if %errorlevel% neq 0 (
    echo ERROR: Failed to create namespace
    exit /b 1
)
echo      Namespace ready.
echo.

:: Deploy IBM MQ
echo [2/6] Deploying IBM MQ...
helm upgrade --install %RELEASE_PREFIX%-ibm-mq ./ibm-mq ^
    --namespace %NAMESPACE% ^
    --wait --timeout 5m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy IBM MQ
    exit /b 1
)
echo      IBM MQ deployed.
echo.

:: Wait for MQ to be ready
echo      Waiting for IBM MQ to be ready...
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=ibm-mq -n %NAMESPACE% --timeout=300s
echo.

:: Deploy Prometheus
echo [3/6] Deploying Prometheus...
helm upgrade --install %RELEASE_PREFIX%-prometheus ./prometheus ^
    --namespace %NAMESPACE% ^
    --wait --timeout 3m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Prometheus
    exit /b 1
)
echo      Prometheus deployed.
echo.

:: Deploy Grafana
echo [4/6] Deploying Grafana...
helm upgrade --install %RELEASE_PREFIX%-grafana ./grafana ^
    --namespace %NAMESPACE% ^
    --wait --timeout 3m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Grafana
    exit /b 1
)
echo      Grafana deployed.
echo.

:: Deploy Consumer
echo [5/6] Deploying Consumer...
helm upgrade --install %RELEASE_PREFIX%-consumer ./consumer ^
    --namespace %NAMESPACE% ^
    --wait --timeout 3m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Consumer
    exit /b 1
)
echo      Consumer deployed.
echo.

:: Deploy Perf-Tester
echo [6/6] Deploying Perf-Tester...
helm upgrade --install %RELEASE_PREFIX%-perf-tester ./perf-tester ^
    --namespace %NAMESPACE% ^
    --wait --timeout 3m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Perf-Tester
    exit /b 1
)
echo      Perf-Tester deployed.
echo.

echo ============================================
echo  Deployment Complete!
echo ============================================
echo.
echo Services:
echo   - IBM MQ:      kubectl port-forward svc/%RELEASE_PREFIX%-ibm-mq 1414:1414 -n %NAMESPACE%
echo   - Prometheus:  kubectl port-forward svc/%RELEASE_PREFIX%-prometheus 9090:9090 -n %NAMESPACE%
echo   - Grafana:     kubectl port-forward svc/%RELEASE_PREFIX%-grafana 3000:3000 -n %NAMESPACE%
echo   - Perf-Tester: kubectl port-forward svc/%RELEASE_PREFIX%-perf-tester 8080:8080 -n %NAMESPACE%
echo.
echo To run a test:
echo   curl -X POST "http://localhost:8080/api/perf/send?count=1000" -H "Content-Type: text/plain" -d "test" -o results.zip
echo.

endlocal
