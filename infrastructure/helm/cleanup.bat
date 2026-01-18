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

:: Confirm deletion
echo This will uninstall all Helm releases in namespace: %NAMESPACE%
echo.
set /p CONFIRM="Are you sure you want to continue? (y/N): "
if /i not "%CONFIRM%"=="y" (
    echo Cleanup cancelled.
    exit /b 0
)
echo.

:: Uninstall Perf-Tester
echo [1/6] Uninstalling Perf-Tester...
helm uninstall %RELEASE_PREFIX%-perf-tester --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Perf-Tester uninstalled.
) else (
    echo      Perf-Tester not found or already removed.
)
echo.

:: Uninstall Consumer
echo [2/6] Uninstalling Consumer...
helm uninstall %RELEASE_PREFIX%-consumer --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Consumer uninstalled.
) else (
    echo      Consumer not found or already removed.
)
echo.

:: Uninstall Grafana
echo [3/6] Uninstalling Grafana...
helm uninstall %RELEASE_PREFIX%-grafana --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Grafana uninstalled.
) else (
    echo      Grafana not found or already removed.
)
echo.

:: Uninstall Prometheus
echo [4/6] Uninstalling Prometheus...
helm uninstall %RELEASE_PREFIX%-prometheus --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      Prometheus uninstalled.
) else (
    echo      Prometheus not found or already removed.
)
echo.

:: Uninstall IBM MQ
echo [5/6] Uninstalling IBM MQ...
helm uninstall %RELEASE_PREFIX%-ibm-mq --namespace %NAMESPACE% 2>nul
if %errorlevel% equ 0 (
    echo      IBM MQ uninstalled.
) else (
    echo      IBM MQ not found or already removed.
)
echo.

:: Ask about PVC deletion
echo [6/6] Persistent Volume Claims...
set /p DELETE_PVC="Delete Persistent Volume Claims? This will delete all data! (y/N): "
if /i "%DELETE_PVC%"=="y" (
    echo      Deleting PVCs...
    kubectl delete pvc --all -n %NAMESPACE% 2>nul
    echo      PVCs deleted.
) else (
    echo      PVCs retained.
)
echo.

:: Ask about namespace deletion
set /p DELETE_NS="Delete namespace %NAMESPACE%? (y/N): "
if /i "%DELETE_NS%"=="y" (
    echo      Deleting namespace...
    kubectl delete namespace %NAMESPACE% 2>nul
    echo      Namespace deleted.
) else (
    echo      Namespace retained.
)
echo.

echo ============================================
echo  Cleanup Complete!
echo ============================================

endlocal
