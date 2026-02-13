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
echo [1/28] Building application JARs with Gradle...
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
echo [2/28] Building Docker image for perf-tester...
docker build -t perf-tester:%IMAGE_TAG% %PROJECT_ROOT%\perf-tester
if %errorlevel% neq 0 (
    echo ERROR: Failed to build perf-tester image
    exit /b 1
)
echo      perf-tester image built.
echo.

echo [3/28] Building Docker image for ibm-mq-consumer...
docker build -t ibm-mq-consumer:%IMAGE_TAG% %PROJECT_ROOT%\ibm-mq-consumer
if %errorlevel% neq 0 (
    echo ERROR: Failed to build ibm-mq-consumer image
    exit /b 1
)
echo      ibm-mq-consumer image built.
echo.

echo [4/28] Building Docker image for kafka-consumer...
docker build -t kafka-consumer:%IMAGE_TAG% %PROJECT_ROOT%\kafka-consumer
if %errorlevel% neq 0 (
    echo ERROR: Failed to build kafka-consumer image
    exit /b 1
)
echo      kafka-consumer image built.
echo.

echo [5/28] Building Docker image for api-gateway...
docker build -t api-gateway:%IMAGE_TAG% %PROJECT_ROOT%\api-gateway
if %errorlevel% neq 0 (
    echo ERROR: Failed to build api-gateway image
    exit /b 1
)
echo      api-gateway image built.
echo.

echo [6/28] Building Docker image for config-server...
docker build -t config-server:%IMAGE_TAG% %PROJECT_ROOT%\config-server
if %errorlevel% neq 0 (
    echo ERROR: Failed to build config-server image
    exit /b 1
)
echo      config-server image built.
echo.

echo [7/28] Building Docker image for perf-ui...
docker build -t perf-ui:%IMAGE_TAG% %PROJECT_ROOT%\perf-ui
if %errorlevel% neq 0 (
    echo ERROR: Failed to build perf-ui image
    exit /b 1
)
echo      perf-ui image built.
echo.

:: ============================================
:: Load images to Kubernetes (for local clusters)
:: or push to registry (for GCP/remote clusters)
:: ============================================
echo [8/28] Loading images to Kubernetes cluster...

:: Detect cluster type and load images accordingly
kubectl config current-context > temp_context.txt
set /p CURRENT_CONTEXT=<temp_context.txt
del temp_context.txt

echo      Current context: %CURRENT_CONTEXT%

:: Initialize image repository variables (will be overridden for GCP)
set PERF_TESTER_IMAGE=perf-tester:%IMAGE_TAG%
set IBM_MQ_CONSUMER_IMAGE=ibm-mq-consumer:%IMAGE_TAG%
set KAFKA_CONSUMER_IMAGE=kafka-consumer:%IMAGE_TAG%
set API_GATEWAY_IMAGE=api-gateway:%IMAGE_TAG%
set CONFIG_SERVER_IMAGE=config-server:%IMAGE_TAG%
set PERF_UI_IMAGE=perf-ui:%IMAGE_TAG%
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

    :: Tag and push ibm-mq-consumer image
    set IBM_MQ_CONSUMER_IMAGE=!GCP_REGISTRY!/ibm-mq-consumer:%IMAGE_TAG%
    echo      Tagging and pushing ibm-mq-consumer to !IBM_MQ_CONSUMER_IMAGE!...
    docker tag ibm-mq-consumer:%IMAGE_TAG% !IBM_MQ_CONSUMER_IMAGE!
    docker push !IBM_MQ_CONSUMER_IMAGE!
    if %errorlevel% neq 0 (
        echo ERROR: Failed to push ibm-mq-consumer image
        exit /b 1
    )

    :: Tag and push kafka-consumer image
    set KAFKA_CONSUMER_IMAGE=!GCP_REGISTRY!/kafka-consumer:%IMAGE_TAG%
    echo      Tagging and pushing kafka-consumer to !KAFKA_CONSUMER_IMAGE!...
    docker tag kafka-consumer:%IMAGE_TAG% !KAFKA_CONSUMER_IMAGE!
    docker push !KAFKA_CONSUMER_IMAGE!
    if %errorlevel% neq 0 (
        echo ERROR: Failed to push kafka-consumer image
        exit /b 1
    )

    :: Tag and push api-gateway image
    set API_GATEWAY_IMAGE=!GCP_REGISTRY!/api-gateway:%IMAGE_TAG%
    echo      Tagging and pushing api-gateway to !API_GATEWAY_IMAGE!...
    docker tag api-gateway:%IMAGE_TAG% !API_GATEWAY_IMAGE!
    docker push !API_GATEWAY_IMAGE!
    if %errorlevel% neq 0 (
        echo ERROR: Failed to push api-gateway image
        exit /b 1
    )

    :: Tag and push config-server image
    set CONFIG_SERVER_IMAGE=!GCP_REGISTRY!/config-server:%IMAGE_TAG%
    echo      Tagging and pushing config-server to !CONFIG_SERVER_IMAGE!...
    docker tag config-server:%IMAGE_TAG% !CONFIG_SERVER_IMAGE!
    docker push !CONFIG_SERVER_IMAGE!
    if %errorlevel% neq 0 (
        echo ERROR: Failed to push config-server image
        exit /b 1
    )

    :: Tag and push perf-ui image
    set PERF_UI_IMAGE=!GCP_REGISTRY!/perf-ui:%IMAGE_TAG%
    echo      Tagging and pushing perf-ui to !PERF_UI_IMAGE!...
    docker tag perf-ui:%IMAGE_TAG% !PERF_UI_IMAGE!
    docker push !PERF_UI_IMAGE!
    if %errorlevel% neq 0 (
        echo ERROR: Failed to push perf-ui image
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
    minikube image load ibm-mq-consumer:%IMAGE_TAG%
    minikube image load kafka-consumer:%IMAGE_TAG%
    minikube image load api-gateway:%IMAGE_TAG%
    minikube image load config-server:%IMAGE_TAG%
    minikube image load perf-ui:%IMAGE_TAG%
    goto :images_loaded
)

:: Check for kind
echo %CURRENT_CONTEXT% | findstr /i "kind" >nul
if %errorlevel% equ 0 (
    echo      Detected Kind - loading images...
    kind load docker-image perf-tester:%IMAGE_TAG%
    kind load docker-image ibm-mq-consumer:%IMAGE_TAG%
    kind load docker-image kafka-consumer:%IMAGE_TAG%
    kind load docker-image api-gateway:%IMAGE_TAG%
    kind load docker-image config-server:%IMAGE_TAG%
    kind load docker-image perf-ui:%IMAGE_TAG%
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
echo [9/28] Creating namespace %NAMESPACE%...
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
echo [10/28] Deploying IBM MQ...
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

echo [11/28] Deploying Oracle Database...
helm upgrade --install %RELEASE_PREFIX%-oracle ./oracle ^
    --namespace %NAMESPACE% ^
    --wait --timeout 10m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Oracle Database
    exit /b 1
)
echo      Oracle Database deployed.
echo.

:: Wait for Oracle to be ready
echo      Waiting for Oracle Database to be ready...
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=oracle -n %NAMESPACE% --timeout=600s
echo.

echo [12/28] Deploying Oracle Exporter...
helm upgrade --install %RELEASE_PREFIX%-oracle-exporter ./oracle-exporter ^
    --namespace %NAMESPACE% ^
    --wait --timeout 3m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Oracle Exporter
    exit /b 1
)
echo      Oracle Exporter deployed.
echo.

echo [13/28] Deploying Prometheus...
helm upgrade --install %RELEASE_PREFIX%-prometheus ./prometheus ^
    --namespace %NAMESPACE% ^
    --wait --timeout 3m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Prometheus
    exit /b 1
)
echo      Prometheus deployed.
echo.

echo [14/28] Deploying Loki...
helm upgrade --install %RELEASE_PREFIX%-loki ./loki ^
    --namespace %NAMESPACE% ^
    --wait --timeout 3m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Loki
    exit /b 1
)
echo      Loki deployed.
echo.

echo [15/28] Deploying Promtail...
helm upgrade --install %RELEASE_PREFIX%-promtail ./promtail ^
    --namespace %NAMESPACE% ^
    --wait --timeout 3m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Promtail
    exit /b 1
)
echo      Promtail deployed.
echo.

echo [16/28] Deploying Grafana...
helm upgrade --install %RELEASE_PREFIX%-grafana ./grafana ^
    --namespace %NAMESPACE% ^
    --wait --timeout 3m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Grafana
    exit /b 1
)
echo      Grafana deployed.
echo.

echo [17/28] Deploying SonarQube...
helm upgrade --install %RELEASE_PREFIX%-sonarqube ./sonarqube ^
    --namespace %NAMESPACE% ^
    --wait --timeout 10m
if %errorlevel% neq 0 (
    echo WARNING: Failed to deploy SonarQube (optional component)
)
echo      SonarQube deployed.
echo.

echo [18/28] Deploying Redis...
helm upgrade --install %RELEASE_PREFIX%-redis ./redis ^
    --namespace %NAMESPACE% ^
    --wait --timeout 3m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Redis
    exit /b 1
)
echo      Redis deployed.
echo.

:: ============================================
:: Deploy Kafka
:: ============================================
echo [19/28] Deploying Kafka...
helm upgrade --install %RELEASE_PREFIX%-kafka ./kafka ^
    --namespace %NAMESPACE% ^
    --wait --timeout 5m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Kafka
    exit /b 1
)
echo      Kafka deployed.
echo.

:: Wait for Kafka to be ready
echo      Waiting for Kafka to be ready...
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=kafka -n %NAMESPACE% --timeout=300s
echo.

:: ============================================
:: Deploy Config Server
:: ============================================
echo [20/28] Deploying Config Server...
:: Extract repository from full image path (remove tag)
for /f "tokens=1 delims=:" %%a in ("%CONFIG_SERVER_IMAGE%") do set CONFIG_SERVER_REPO=%%a
helm upgrade --install %RELEASE_PREFIX%-config-server ./config-server ^
    --namespace %NAMESPACE% ^
    --set image.repository=%CONFIG_SERVER_REPO% ^
    --set image.tag=%IMAGE_TAG% ^
    --set image.pullPolicy=%IMAGE_PULL_POLICY% ^
    --wait --timeout 3m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Config Server
    exit /b 1
)
echo      Config Server deployed.
echo.

:: Wait for Config Server to be ready
echo      Waiting for Config Server to be ready...
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=config-server -n %NAMESPACE% --timeout=120s
echo.

:: ============================================
:: Deploy Applications
:: ============================================
echo [21/28] Deploying Applications...

echo      Deploying IBM MQ Consumer...
:: Extract repository from full image path (remove tag)
for /f "tokens=1 delims=:" %%a in ("%IBM_MQ_CONSUMER_IMAGE%") do set IBM_MQ_CONSUMER_REPO=%%a
helm upgrade --install %RELEASE_PREFIX%-ibm-mq-consumer ./ibm-mq-consumer ^
    --namespace %NAMESPACE% ^
    --set image.repository=%IBM_MQ_CONSUMER_REPO% ^
    --set image.tag=%IMAGE_TAG% ^
    --set image.pullPolicy=%IMAGE_PULL_POLICY% ^
    --wait --timeout 3m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy IBM MQ Consumer
    exit /b 1
)
echo      IBM MQ Consumer deployed.

echo      Deploying Kafka Consumer...
:: Extract repository from full image path (remove tag)
for /f "tokens=1 delims=:" %%a in ("%KAFKA_CONSUMER_IMAGE%") do set KAFKA_CONSUMER_REPO=%%a
helm upgrade --install %RELEASE_PREFIX%-kafka-consumer ./kafka-consumer ^
    --namespace %NAMESPACE% ^
    --set image.repository=%KAFKA_CONSUMER_REPO% ^
    --set image.tag=%IMAGE_TAG% ^
    --set image.pullPolicy=%IMAGE_PULL_POLICY% ^
    --wait --timeout 3m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Kafka Consumer
    exit /b 1
)
echo      Kafka Consumer deployed.

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
:: Deploy Perf-UI
:: ============================================
echo [22/28] Deploying Perf-UI...
:: Extract repository from full image path (remove tag)
for /f "tokens=1 delims=:" %%a in ("%PERF_UI_IMAGE%") do set PERF_UI_REPO=%%a
helm upgrade --install %RELEASE_PREFIX%-perf-ui ./perf-ui ^
    --namespace %NAMESPACE% ^
    --set image.repository=%PERF_UI_REPO% ^
    --set image.tag=%IMAGE_TAG% ^
    --set image.pullPolicy=%IMAGE_PULL_POLICY% ^
    --wait --timeout 3m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Perf-UI
    exit /b 1
)
echo      Perf-UI deployed.
echo.

:: ============================================
:: Deploy Kafdrop
:: ============================================
echo [23/28] Deploying Kafdrop...
helm upgrade --install %RELEASE_PREFIX%-kafdrop ./kafdrop ^
    --namespace %NAMESPACE% ^
    --wait --timeout 3m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Kafdrop
    exit /b 1
)
echo      Kafdrop deployed.
echo.

:: ============================================
:: Deploy Redis Commander
:: ============================================
echo [24/28] Deploying Redis Commander...
helm upgrade --install %RELEASE_PREFIX%-redis-commander ./redis-commander ^
    --namespace %NAMESPACE% ^
    --wait --timeout 3m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Redis Commander
    exit /b 1
)
echo      Redis Commander deployed.
echo.

:: ============================================
:: Deploy Kafka Exporter
:: ============================================
echo [25/28] Deploying Kafka Exporter...
helm upgrade --install %RELEASE_PREFIX%-kafka-exporter ./kafka-exporter ^
    --namespace %NAMESPACE% ^
    --wait --timeout 2m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Kafka Exporter
    exit /b 1
)
echo      Kafka Exporter deployed.
echo.

:: ============================================
:: Deploy API Gateway
:: ============================================
echo [26/28] Deploying API Gateway...
:: Extract repository from full image path (remove tag)
for /f "tokens=1 delims=:" %%a in ("%API_GATEWAY_IMAGE%") do set API_GATEWAY_REPO=%%a
helm upgrade --install %RELEASE_PREFIX%-api-gateway ./api-gateway ^
    --namespace %NAMESPACE% ^
    --set image.repository=%API_GATEWAY_REPO% ^
    --set image.tag=%IMAGE_TAG% ^
    --set image.pullPolicy=%IMAGE_PULL_POLICY% ^
    --wait --timeout 3m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy API Gateway
    exit /b 1
)
echo      API Gateway deployed.
echo.

:: ============================================
:: Deploy Ingress
:: ============================================
echo [27/28] Deploying Ingress...
helm upgrade --install %RELEASE_PREFIX%-ingress ./ingress ^
    --namespace %NAMESPACE% ^
    --wait --timeout 2m
if %errorlevel% neq 0 (
    echo ERROR: Failed to deploy Ingress
    exit /b 1
)
echo      Ingress deployed.
echo.

echo [28/28] Printing deployment summary...
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
echo Access the services:
echo   - Perf-UI:     http://localhost/ (default landing page)
echo   - Grafana:     http://localhost/grafana (admin/admin)
echo   - Swagger UI:  http://localhost/api/swagger-ui/index.html
echo   - Prometheus:  http://localhost/prometheus
echo   - Kafdrop:     http://localhost/kafdrop
echo   - Redis Commander: http://localhost/redis-commander
echo   - Loki:        http://localhost/loki
echo   - MQ Console:  http://localhost/ibmmq (admin/passw0rd)
echo   - SonarQube:   http://localhost/sonar (admin/admin)
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
