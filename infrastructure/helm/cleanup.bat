@echo off
setlocal enabledelayedexpansion

echo ============================================
echo  Perf-Demo Helm Cleanup Script
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

echo Uninstalling all Helm releases in namespace: %NAMESPACE%
echo.

:: Uninstall Ingress
echo [1/23] Uninstalling Ingress...
helm uninstall %RELEASE_PREFIX%-ingress --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Ingress uninstalled.
) else (
    echo      Ingress not found or already removed.
)
echo.

:: Uninstall API Gateway
echo [2/23] Uninstalling API Gateway...
helm uninstall %RELEASE_PREFIX%-api-gateway --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      API Gateway uninstalled.
) else (
    echo      API Gateway not found or already removed.
)
echo.

:: Uninstall Config Server
echo [3/23] Uninstalling Config Server...
helm uninstall %RELEASE_PREFIX%-config-server --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Config Server uninstalled.
) else (
    echo      Config Server not found or already removed.
)
echo.

:: Uninstall Kafka Exporter
echo [4/23] Uninstalling Kafka Exporter...
helm uninstall %RELEASE_PREFIX%-kafka-exporter --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Kafka Exporter uninstalled.
) else (
    echo      Kafka Exporter not found or already removed.
)
echo.

:: Uninstall Kafdrop
echo [5/23] Uninstalling Kafdrop...
helm uninstall %RELEASE_PREFIX%-kafdrop --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Kafdrop uninstalled.
) else (
    echo      Kafdrop not found or already removed.
)
echo.

:: Uninstall Perf-Tester
echo [6/23] Uninstalling Perf-Tester...
helm uninstall %RELEASE_PREFIX%-perf-tester --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Perf-Tester uninstalled.
) else (
    echo      Perf-Tester not found or already removed.
)
echo.

:: Uninstall IBM MQ Consumer
echo [7/23] Uninstalling IBM MQ Consumer...
helm uninstall %RELEASE_PREFIX%-ibm-mq-consumer --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      IBM MQ Consumer uninstalled.
) else (
    echo      IBM MQ Consumer not found or already removed.
)
echo.

:: Uninstall Kafka Consumer
echo [8/23] Uninstalling Kafka Consumer...
helm uninstall %RELEASE_PREFIX%-kafka-consumer --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Kafka Consumer uninstalled.
) else (
    echo      Kafka Consumer not found or already removed.
)
echo.

:: Uninstall Kafka
echo [9/23] Uninstalling Kafka...
helm uninstall %RELEASE_PREFIX%-kafka --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Kafka uninstalled.
) else (
    echo      Kafka not found or already removed.
)
echo.

:: Uninstall SonarQube
echo [10/23] Uninstalling SonarQube...
helm uninstall %RELEASE_PREFIX%-sonarqube --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      SonarQube uninstalled.
) else (
    echo      SonarQube not found or already removed.
)
echo.

:: Uninstall Grafana
echo [11/23] Uninstalling Grafana...
helm uninstall %RELEASE_PREFIX%-grafana --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Grafana uninstalled.
) else (
    echo      Grafana not found or already removed.
)
echo.

:: Uninstall Promtail
echo [12/23] Uninstalling Promtail...
helm uninstall %RELEASE_PREFIX%-promtail --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Promtail uninstalled.
) else (
    echo      Promtail not found or already removed.
)
echo.

:: Uninstall Loki
echo [13/23] Uninstalling Loki...
helm uninstall %RELEASE_PREFIX%-loki --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Loki uninstalled.
) else (
    echo      Loki not found or already removed.
)
echo.

:: Uninstall Tempo
echo [14/23] Uninstalling Tempo...
helm uninstall %RELEASE_PREFIX%-tempo --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Tempo uninstalled.
) else (
    echo      Tempo not found or already removed.
)
echo.

:: Uninstall Prometheus
echo [15/23] Uninstalling Prometheus...
helm uninstall %RELEASE_PREFIX%-prometheus --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Prometheus uninstalled.
) else (
    echo      Prometheus not found or already removed.
)
echo.

:: Uninstall Oracle Exporter
echo [16/23] Uninstalling Oracle Exporter...
helm uninstall %RELEASE_PREFIX%-oracle-exporter --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Oracle Exporter uninstalled.
) else (
    echo      Oracle Exporter not found or already removed.
)
echo.

:: Uninstall Oracle Database
echo [17/23] Uninstalling Oracle Database...
helm uninstall %RELEASE_PREFIX%-oracle --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Oracle Database uninstalled.
) else (
    echo      Oracle Database not found or already removed.
)
echo.

:: Uninstall Redis Commander
echo [18/23] Uninstalling Redis Commander...
helm uninstall %RELEASE_PREFIX%-redis-commander --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Redis Commander uninstalled.
) else (
    echo      Redis Commander not found or already removed.
)
echo.

:: Uninstall Redis
echo [19/23] Uninstalling Redis...
helm uninstall %RELEASE_PREFIX%-redis --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Redis uninstalled.
) else (
    echo      Redis not found or already removed.
)
echo.

:: Uninstall IBM MQ
echo [20/23] Uninstalling IBM MQ...
helm uninstall %RELEASE_PREFIX%-ibm-mq --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      IBM MQ uninstalled.
) else (
    echo      IBM MQ not found or already removed.
)
echo.

:: Delete PVCs
echo [21/23] Deleting Persistent Volume Claims...
kubectl delete pvc --all -n %NAMESPACE% 2>nul
echo      PVCs deleted.
echo.

:: Delete ConfigMaps and Secrets (optional cleanup)
echo [22/23] Deleting remaining ConfigMaps and Secrets...
kubectl delete configmap --all -n %NAMESPACE% 2>nul
kubectl delete secret --all -n %NAMESPACE% 2>nul
echo      ConfigMaps and Secrets deleted.
echo.

:: Delete namespace
echo [23/23] Deleting namespace %NAMESPACE%...
kubectl delete namespace %NAMESPACE% 2>nul
echo      Namespace deleted.
echo.

echo ============================================
echo  Cleanup Complete!
echo ============================================

endlocal
