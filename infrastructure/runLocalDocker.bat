@echo off
echo === Building Gradle projects ===
cd ..
call gradlew.bat build
cd infrastructure

if %ERRORLEVEL% NEQ 0 (
    echo Gradle build failed!
    exit /b 1
)

echo.
echo === Rebuilding Docker images ===
docker compose -f docker/compose.yaml build config-server perf-tester ibm-mq-consumer kafka-consumer api-gateway


if %ERRORLEVEL% NEQ 0 (
    echo Image build failed!
    exit /b 1
)

echo.
echo === Stopping Docker Compose ===
docker compose -f docker/compose.yaml down

echo.
echo === Starting all services ===
docker compose -f docker/compose.yaml up --build -d

echo ============================================
echo  Ingress URLs
echo ============================================
echo.
echo Access the services:
echo   - Perf UI:         http://localhost:5173
echo   - Grafana:         http://localhost:3000 (admin/admin)
echo   - Prometheus:      http://localhost:9090
echo   - Kafdrop:         http://localhost:9000/kafdrop
echo   - Redis Commander: http://localhost:8083/redis-commander
echo   - Loki:            http://localhost:3100
echo   - Tempo:           http://localhost:3200
echo   - MQ Console:      https://localhost:9443/ibmmq/console (admin/passw0rd)
echo   - SonarQube:       http://localhost:9001 (admin/admin)
echo   - Config Server:   http://localhost:8888
echo   - Oracle DB:       localhost:1521/XEPDB1 (perfuser/perfpass)