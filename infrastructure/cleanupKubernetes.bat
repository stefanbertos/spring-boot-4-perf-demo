@echo off
setlocal enabledelayedexpansion

echo ============================================
echo  Kubernetes Cleanup Script
echo ============================================
echo.

set ARGOCD_NAMESPACE=argocd
set APP_NAMESPACE=perf-demo

:: Check if kubectl is installed
where kubectl >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: kubectl is not installed or not in PATH
    exit /b 1
)

:: ============================================
:: Step 1: Delete ArgoCD root application (cascading delete)
:: ============================================
echo [1/5] Deleting ArgoCD root application (cascading delete of all child apps)...
kubectl delete application perf-demo -n %ARGOCD_NAMESPACE% --ignore-not-found=true
if %errorlevel% neq 0 (
    echo WARNING: Failed to delete root application, continuing...
)
echo      Root application deleted.
echo.

:: ============================================
:: Step 2: Wait for child applications to be cleaned up
:: ============================================
echo [2/5] Waiting for child applications to be removed...
timeout /t 15 /nobreak >nul
echo      Wait complete.
echo.

:: ============================================
:: Step 3: Delete any remaining ArgoCD applications
:: ============================================
echo [3/5] Cleaning up any remaining ArgoCD applications...
for /f "tokens=1" %%a in ('kubectl get applications -n %ARGOCD_NAMESPACE% -o name 2^>nul') do (
    echo      Deleting %%a...
    kubectl delete %%a -n %ARGOCD_NAMESPACE% --ignore-not-found=true
)
echo      ArgoCD applications cleaned up.
echo.

:: ============================================
:: Step 4: Delete AppProject
:: ============================================
echo [4/5] Deleting ArgoCD AppProject...
kubectl delete appproject perf-demo -n %ARGOCD_NAMESPACE% --ignore-not-found=true
echo      AppProject deleted.
echo.

:: ============================================
:: Step 5: Delete application namespace
:: ============================================
echo [5/5] Deleting namespace %APP_NAMESPACE%...
kubectl delete namespace %APP_NAMESPACE% --ignore-not-found=true
if %errorlevel% neq 0 (
    echo WARNING: Namespace deletion may take a moment to finalize...
)
echo      Namespace %APP_NAMESPACE% deleted.
echo.

echo ============================================
echo  Kubernetes Cleanup Complete!
echo ============================================
echo.
echo Removed:
echo   - ArgoCD root application and all child applications
echo   - AppProject 'perf-demo'
echo   - Namespace '%APP_NAMESPACE%' and all its resources
echo.
echo Note: ArgoCD itself is still installed in the '%ARGOCD_NAMESPACE%' namespace.
echo To also remove ArgoCD:
echo   kubectl delete namespace %ARGOCD_NAMESPACE%
echo.
echo To redeploy, run: infrastructure\deployArgo.bat

endlocal
