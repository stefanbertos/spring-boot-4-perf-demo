@echo off
echo === Building Gradle projects ===
call gradlew.bat build

if %ERRORLEVEL% NEQ 0 (
    echo Gradle build failed!
    exit /b 1
)

echo.
echo === Stopping Docker Compose ===
docker compose down

echo.
echo === Rebuilding Docker images ===
docker compose build config-server perf-tester ibm-mq-consumer kafka-consumer api-gateway

echo.
echo === Starting all services ===
docker compose up -d