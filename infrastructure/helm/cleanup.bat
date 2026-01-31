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
echo [1/17] Uninstalling Ingress...
helm uninstall %RELEASE_PREFIX%-ingress --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Ingress uninstalled.
) else (
    echo      Ingress not found or already removed.
)
echo.

:: Uninstall Kafka Exporter
echo [2/17] Uninstalling Kafka Exporter...
helm uninstall %RELEASE_PREFIX%-kafka-exporter --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Kafka Exporter uninstalled.
) else (
    echo      Kafka Exporter not found or already removed.
)
echo.

:: Uninstall Kafdrop
echo [3/17] Uninstalling Kafdrop...
helm uninstall %RELEASE_PREFIX%-kafdrop --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Kafdrop uninstalled.
) else (
    echo      Kafdrop not found or already removed.
)
echo.

:: Uninstall Kafka
echo [4/17] Uninstalling Kafka...
helm uninstall %RELEASE_PREFIX%-kafka --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Kafka uninstalled.
) else (
    echo      Kafka not found or already removed.
)
echo.

:: Uninstall Perf-Tester
echo [5/17] Uninstalling Perf-Tester...
helm uninstall %RELEASE_PREFIX%-perf-tester --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Perf-Tester uninstalled.
) else (
    echo      Perf-Tester not found or already removed.
)
echo.

:: Uninstall IBM MQ Consumer
echo [6/17] Uninstalling IBM MQ Consumer...
helm uninstall %RELEASE_PREFIX%-ibm-mq-consumer --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      IBM MQ Consumer uninstalled.
) else (
    echo      IBM MQ Consumer not found or already removed.
)
echo.

:: Uninstall Kafka Consumer
echo [7/17] Uninstalling Kafka Consumer...
helm uninstall %RELEASE_PREFIX%-kafka-consumer --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Kafka Consumer uninstalled.
) else (
    echo      Kafka Consumer not found or already removed.
)
echo.

:: Uninstall Grafana
echo [8/17] Uninstalling Grafana...
helm uninstall %RELEASE_PREFIX%-grafana --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Grafana uninstalled.
) else (
    echo      Grafana not found or already removed.
)
echo.

:: Uninstall Loki
echo [9/17] Uninstalling Loki...
helm uninstall %RELEASE_PREFIX%-loki --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Loki uninstalled.
) else (
    echo      Loki not found or already removed.
)
echo.

:: Uninstall Tempo
echo [10/17] Uninstalling Tempo...
helm uninstall %RELEASE_PREFIX%-tempo --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Tempo uninstalled.
) else (
    echo      Tempo not found or already removed.
)
echo.

:: Uninstall Prometheus
echo [11/17] Uninstalling Prometheus...
helm uninstall %RELEASE_PREFIX%-prometheus --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Prometheus uninstalled.
) else (
    echo      Prometheus not found or already removed.
)
echo.

:: Uninstall IBM MQ
echo [12/17] Uninstalling IBM MQ...
helm uninstall %RELEASE_PREFIX%-ibm-mq --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      IBM MQ uninstalled.
) else (
    echo      IBM MQ not found or already removed.
)
echo.

:: Uninstall Oracle Exporter
echo [13/17] Uninstalling Oracle Exporter...
helm uninstall %RELEASE_PREFIX%-oracle-exporter --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Oracle Exporter uninstalled.
) else (
    echo      Oracle Exporter not found or already removed.
)
echo.

:: Uninstall Oracle Database
echo [14/17] Uninstalling Oracle Database...
helm uninstall %RELEASE_PREFIX%-oracle --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Oracle Database uninstalled.
) else (
    echo      Oracle Database not found or already removed.
)
echo.

:: Delete PVCs
echo [15/17] Deleting Persistent Volume Claims...
kubectl delete pvc --all -n %NAMESPACE% 2>nul
echo      PVCs deleted.
echo.

:: Delete namespace
echo [16/17] Deleting namespace %NAMESPACE%...
kubectl delete namespace %NAMESPACE% 2>nul
echo      Namespace deleted.
echo.

echo ============================================
echo  Cleanup Complete!
echo ============================================

endlocal
