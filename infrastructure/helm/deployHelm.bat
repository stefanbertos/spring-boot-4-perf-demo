@echo off
setlocal enabledelayedexpansion

echo ============================================
echo  Perf-Demo Helm Deployment Script
echo ============================================
echo.

set NAMESPACE=perf-demo
set RELEASE_PREFIX=perf
set IMAGE_TAG=latest
set PROJECT_ROOT=%~dp0..\..

:: GCP Configuration (set these for GCP deployment)
:: GCP_PROJECT - Google Cloud project ID (e.g., my-project-123)
:: GCP_REGION - Region for Artifact Registry (e.g., us-central1)
:: GCP_REGISTRY - Full registry path (auto-generated if not set)

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

:: Check if docker is installed
where docker >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: Docker is not installed or not in PATH
    exit /b 1
)

:: ============================================
:: Build Application JARs
:: ============================================
echo [1/10] Building application JARs with Gradle...
pushd %PROJECT_ROOT%
call gradlew.bat clean build -x test
if %errorlevel% neq 0 (
    echo ERROR: Gradle build failed
    popd
    exit /b 1
)
popd
echo      JARs built successfully.
echo.

:: ============================================
:: Build Docker Images
:: ============================================
echo [2/10] Building Docker image for perf-tester...
docker build -t perf-tester:%IMAGE_TAG% %PROJECT_ROOT%\perf-tester
if %errorlevel% neq 0 (
    echo ERROR: Failed to build perf-tester image
    exit /b 1
)
echo      perf-tester image built.
echo.

echo [3/10] Building Docker image for consumer...
docker build -t consumer:%IMAGE_TAG% %PROJECT_ROOT%\consumer
if %errorlevel% neq 0 (
    echo ERROR: Failed to build consumer image
    exit /b 1
)
echo      consumer image built.
echo.

:: ============================================
:: Load images to Kubernetes (for local clusters)
:: or push to registry (for GCP/remote clusters)
:: ============================================
echo [4/10] Loading images to Kubernetes cluster...

:: Detect cluster type and load images accordingly
kubectl config current-context > temp_context.txt
set /p CURRENT_CONTEXT=<temp_context.txt
del temp_context.txt

echo      Current context: %CURRENT_CONTEXT%

:: Initialize image repository variables (will be overridden for GCP)
set PERF_TESTER_IMAGE=perf-tester:%IMAGE_TAG%
set CONSUMER_IMAGE=consumer:%IMAGE_TAG%
set IMAGE_PULL_POLICY=IfNotPresent

:: Check for GKE (Google Kubernetes Engine)
echo %CURRENT_CONTEXT% | findstr /i "gke_" >nul
if %errorlevel% equ 0 (
    echo      Detected GKE cluster - pushing images to Google Artifact Registry...

    :: Validate GCP_PROJECT is set
    if "%GCP_PROJECT%"=="" (
        echo ERROR: GCP_PROJECT environment variable is not set
        echo Please set it: set GCP_PROJECT=your-project-id
        exit /b 1
    )

    :: Set default region if not specified
    if "%GCP_REGION%"=="" (
        set GCP_REGION=us-central1
        echo      Using default region: us-central1
    )

    :: Set registry path (Artifact Registry format)
    if "%GCP_REGISTRY%"=="" (
        set GCP_REGISTRY=%GCP_REGION%-docker.pkg.dev/%GCP_PROJECT%/perf-demo
    )

    echo      Registry: !GCP_REGISTRY!

    :: Check if gcloud is installed
    where gcloud >nul 2>nul
    if %errorlevel% neq 0 (
        echo ERROR: gcloud CLI is not installed or not in PATH
        exit /b 1
    )

    :: Configure docker for GCP Artifact Registry
    echo      Configuring Docker authentication for Artifact Registry...
    call gcloud auth configure-docker %GCP_REGION%-docker.pkg.dev --quiet
    if %errorlevel% neq 0 (
        echo ERROR: Failed to configure Docker for Artifact Registry
        exit /b 1
    )

    :: Create Artifact Registry repository if it doesn't exist
    echo      Ensuring Artifact Registry repository exists...
    call gcloud artifacts repositories describe perf-demo --location=%GCP_REGION% >nul 2>nul
    if %errorlevel% neq 0 (
        echo      Creating Artifact Registry repository 'perf-demo'...
        call gcloud artifacts repositories create perf-demo ^
            --repository-format=docker ^
            --location=%GCP_REGION% ^
            --description="Perf-Demo Docker images"
        if %errorlevel% neq 0 (
            echo ERROR: Failed to create Artifact Registry repository
            exit /b 1
        )
    )

    :: Tag and push perf-tester image
    set PERF_TESTER_IMAGE=!GCP_REGISTRY!/perf-tester:%IMAGE_TAG%
    echo      Tagging and pushing perf-tester to !PERF_TESTER_IMAGE!...
    docker tag perf-tester:%IMAGE_TAG% !PERF_TESTER_IMAGE!
    docker push !PERF_TESTER_IMAGE!
    if %errorlevel% neq 0 (
        echo ERROR: Failed to push perf-tester image
        exit /b 1
    )

    :: Tag and push consumer image
    set CONSUMER_IMAGE=!GCP_REGISTRY!/consumer:%IMAGE_TAG%
    echo      Tagging and pushing consumer to !CONSUMER_IMAGE!...
    docker tag consumer:%IMAGE_TAG% !CONSUMER_IMAGE!
    docker push !CONSUMER_IMAGE!
    if %errorlevel% neq 0 (
        echo ERROR: Failed to push consumer image
        exit /b 1
    )

    :: For GCP, we need to pull from registry
    set IMAGE_PULL_POLICY=Always

    goto :images_loaded
)

:: Check for minikube
echo %CURRENT_CONTEXT% | findstr /i "minikube" >nul
if %errorlevel% equ 0 (
    echo      Detected Minikube - loading images...
    minikube image load perf-tester:%IMAGE_TAG%
    minikube image load consumer:%IMAGE_TAG%
    goto :images_loaded
)

:: Check for kind
echo %CURRENT_CONTEXT% | findstr /i "kind" >nul
if %errorlevel% equ 0 (
    echo      Detected Kind - loading images...
    kind load docker-image perf-tester:%IMAGE_TAG%
    kind load docker-image consumer:%IMAGE_TAG%
    goto :images_loaded
)

:: Check for docker-desktop
echo %CURRENT_CONTEXT% | findstr /i "docker-desktop" >nul
if %errorlevel% equ 0 (
    echo      Detected Docker Desktop - images available directly.
    goto :images_loaded
)

:: Check for rancher-desktop
echo %CURRENT_CONTEXT% | findstr /i "rancher" >nul
if %errorlevel% equ 0 (
    echo      Detected Rancher Desktop - images available directly.
    goto :images_loaded
)

echo      Unknown cluster type. Images may need to be pushed to a registry.
echo      For GCP, set GCP_PROJECT environment variable before running.

:images_loaded
echo      Images ready.
echo.

:: ============================================
:: Create Namespace
:: ============================================
echo [5/10] Creating namespace %NAMESPACE%...
kubectl create namespace %NAMESPACE% --dry-run=client -o yaml | kubectl apply -f -
if %errorlevel% neq 0 (
    echo ERROR: Failed to create namespace
    exit /b 1
)
echo      Namespace ready.
echo.

:: ============================================
:: Deploy Infrastructure
:: ============================================
echo [6/10] Deploying IBM MQ...
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

echo [7/10] Deploying Prometheus...
helm upgrade --install %RELEASE_PREFIX%-prometheus ./prometheus ^
    --namespace %NAMESPACE% ^
    --wait --timeout 3m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Prometheus
    exit /b 1
)
echo      Prometheus deployed.
echo.

echo [8/10] Deploying Grafana...
helm upgrade --install %RELEASE_PREFIX%-grafana ./grafana ^
    --namespace %NAMESPACE% ^
    --wait --timeout 3m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Grafana
    exit /b 1
)
echo      Grafana deployed.
echo.

:: ============================================
:: Deploy Applications
:: ============================================
echo [9/10] Deploying Applications...

echo      Deploying Consumer...
:: Extract repository from full image path (remove tag)
for /f "tokens=1 delims=:" %%a in ("%CONSUMER_IMAGE%") do set CONSUMER_REPO=%%a
helm upgrade --install %RELEASE_PREFIX%-consumer ./consumer ^
    --namespace %NAMESPACE% ^
    --set image.repository=%CONSUMER_REPO% ^
    --set image.tag=%IMAGE_TAG% ^
    --set image.pullPolicy=%IMAGE_PULL_POLICY% ^
    --wait --timeout 3m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Consumer
    exit /b 1
)
echo      Consumer deployed.

echo      Deploying Perf-Tester...
:: Extract repository from full image path (remove tag)
for /f "tokens=1 delims=:" %%a in ("%PERF_TESTER_IMAGE%") do set PERF_TESTER_REPO=%%a
helm upgrade --install %RELEASE_PREFIX%-perf-tester ./perf-tester ^
    --namespace %NAMESPACE% ^
    --set image.repository=%PERF_TESTER_REPO% ^
    --set image.tag=%IMAGE_TAG% ^
    --set image.pullPolicy=%IMAGE_PULL_POLICY% ^
    --wait --timeout 3m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Perf-Tester
    exit /b 1
)
echo      Perf-Tester deployed.
echo.

:: ============================================
:: Deploy Ingress
:: ============================================
echo [10/10] Deploying Ingress...
helm upgrade --install %RELEASE_PREFIX%-ingress ./ingress ^
    --namespace %NAMESPACE% ^
    --wait --timeout 2m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Ingress
    exit /b 1
)
echo      Ingress deployed.
echo.

echo ============================================
echo  Deployment Complete!
echo ============================================
echo.
echo Cluster: %CURRENT_CONTEXT%
echo Namespace: %NAMESPACE%
echo.
echo ============================================
echo  Ingress URLs
echo ============================================
echo.
echo All services available at: http://localhost
echo.
echo   Grafana:     http://localhost/grafana
echo   Prometheus:  http://localhost/prometheus
echo   Perf-Tester: http://localhost/api
echo   IBM MQ Web:  http://localhost/mq
echo.
echo Access the services:
echo   - Grafana:     http://localhost/grafana (admin/admin)
echo   - Swagger UI:  http://localhost/api/swagger-ui.html
echo   - Prometheus:  http://localhost/prometheus
echo   - MQ Console:  http://localhost/mq (admin/passw0rd)
echo.
echo ============================================
echo  Usage Notes
echo ============================================
echo.
echo For GCP/GKE deployment:
echo   1. Ensure gcloud CLI is installed and authenticated
echo   2. Set environment variables before running:
echo      set GCP_PROJECT=your-project-id
echo      set GCP_REGION=us-central1  (optional, defaults to us-central1)
echo   3. Run this script while connected to GKE context
echo.
echo For local clusters (minikube, kind, docker-desktop, rancher):
echo   - Images are loaded directly, no registry needed
echo.

endlocal
