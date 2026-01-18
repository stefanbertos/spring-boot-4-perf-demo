@echo off
echo === Building Gradle projects ===
call gradlew.bat :perf-tester:bootJar :consumer:bootJar

if %ERRORLEVEL% NEQ 0 (
    echo Gradle build failed!
    exit /b 1
)

echo.
echo === Stopping Docker Compose ===
docker compose down

echo.
echo === Rebuilding Docker images ===
docker compose build perf-tester consumer

echo.
echo === Starting all services ===
docker compose up -d