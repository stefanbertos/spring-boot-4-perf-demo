@echo off
setlocal enabledelayedexpansion

echo ============================================
echo  ArgoCD Deployment Script
echo ============================================
echo.

set ARGOCD_NAMESPACE=argocd
set APP_NAMESPACE=perf-demo
set ARGOCD_VERSION=stable

:: Check if kubectl is installed
where kubectl >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: kubectl is not installed or not in PATH
    exit /b 1
)

:: ============================================
:: Step 1: Create ArgoCD namespace
:: ============================================
echo [1/7] Creating namespace %ARGOCD_NAMESPACE%...
kubectl create namespace %ARGOCD_NAMESPACE% --dry-run=client -o yaml | kubectl apply -f -
if %errorlevel% neq 0 (
    echo ERROR: Failed to create namespace %ARGOCD_NAMESPACE%
    exit /b 1
)
echo      Namespace %ARGOCD_NAMESPACE% ready.
echo.

:: ============================================
:: Step 2: Install ArgoCD into its namespace
:: ============================================
echo [2/7] Installing ArgoCD (%ARGOCD_VERSION%) into namespace %ARGOCD_NAMESPACE%...
kubectl apply --server-side -n %ARGOCD_NAMESPACE% -f https://raw.githubusercontent.com/argoproj/argo-cd/%ARGOCD_VERSION%/manifests/install.yaml
if %errorlevel% neq 0 (
    echo ERROR: Failed to install ArgoCD
    exit /b 1
)
echo      ArgoCD manifests applied.
echo.

:: ============================================
:: Step 3: Wait for ArgoCD to be ready
:: ============================================
echo [3/7] Waiting for ArgoCD server to be ready...
kubectl wait --for=condition=available deployment/argocd-server -n %ARGOCD_NAMESPACE% --timeout=300s
if %errorlevel% neq 0 (
    echo ERROR: ArgoCD server did not become ready within 5 minutes
    exit /b 1
)
echo      ArgoCD server is ready.
echo.

:: ============================================
:: Step 4: Apply AppProject
:: ============================================
echo [4/7] Applying ArgoCD AppProject...
kubectl apply -f "%~dp0argocd\project.yaml"
if %errorlevel% neq 0 (
    echo ERROR: Failed to apply AppProject
    exit /b 1
)
echo      AppProject 'perf-demo' created.
echo.

:: ============================================
:: Step 5: Apply Root Application (App of Apps)
:: ============================================
echo [5/7] Applying root Application (App of Apps)...
kubectl apply -f "%~dp0argocd\root-app.yaml"
if %errorlevel% neq 0 (
    echo ERROR: Failed to apply root Application
    exit /b 1
)
echo      Root Application 'perf-demo' created.
echo.

:: ============================================
:: Step 6: Retrieve initial admin password
:: ============================================
echo [6/7] Retrieving ArgoCD admin password...
for /f "tokens=*" %%p in ('kubectl -n %ARGOCD_NAMESPACE% get secret argocd-initial-admin-secret -o jsonpath^="{.data.password}"') do set ENCODED_PW=%%p
for /f "tokens=*" %%d in ('powershell -Command "[System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String('%ENCODED_PW%'))"') do set ARGOCD_PW=%%d

:: ============================================
:: Step 7: Start ArgoCD port-forward
:: ============================================
echo [7/7] Starting ArgoCD port-forward...
start "ArgoCD Port-Forward" cmd /k "kubectl port-forward svc/argocd-server -n %ARGOCD_NAMESPACE% 8443:443"

:: Detect ingress URL
set INGRESS_HOST=localhost
for /f "tokens=*" %%h in ('kubectl get ingress perf-demo-ingress -n %APP_NAMESPACE% -o jsonpath^="{.status.loadBalancer.ingress[0].ip}" 2^>nul') do (
    if not "%%h"=="" set INGRESS_HOST=%%h
)

echo ============================================
echo  ArgoCD Deployment Complete!
echo ============================================
echo.
echo ArgoCD is running in namespace: %ARGOCD_NAMESPACE%
echo.
echo ArgoCD UI:      https://localhost:8443
echo Admin user:     admin
echo Admin password:  %ARGOCD_PW%
echo.
echo ============================================
echo  Service URLs (via Ingress)
echo ============================================
echo.
echo   ArgoCD           https://localhost:8443           (port-forward)
echo   Perf UI          http://%INGRESS_HOST%/
echo   Grafana          http://%INGRESS_HOST%/grafana          (admin / admin)
echo   Prometheus       http://%INGRESS_HOST%/prometheus
echo   Kafdrop          http://%INGRESS_HOST%/kafdrop
echo   IBM MQ Console   http://%INGRESS_HOST%/ibmmq            (admin / passw0rd)
echo   Redis Commander  http://%INGRESS_HOST%/redis-commander
echo   SonarQube        http://%INGRESS_HOST%/sonar            (admin / admin)
echo   Perf Tester API  http://%INGRESS_HOST%/api
echo   Config Server    http://%INGRESS_HOST%/config
echo   Loki (Grafana)   http://%INGRESS_HOST%/loki
echo.
echo All services are routed through the NGINX Ingress -^> API Gateway.
echo ArgoCD port-forward is running in a separate window.
echo ArgoCD will now automatically sync all child applications
echo in wave order (0 through 6). Monitor progress in the UI.
echo.

endlocal
