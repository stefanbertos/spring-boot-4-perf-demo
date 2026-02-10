@echo off
echo === Building Gradle projects ===
call gradlew.bat build

if %ERRORLEVEL% NEQ 0 (
    echo Gradle build failed!
    exit /b 1
)

echo.
echo === Building OCI images with Cloud Native Buildpacks ===
call gradlew.bat bootBuildImage

if %ERRORLEVEL% NEQ 0 (
    echo Image build failed!
    exit /b 1
)

echo.
echo === Stopping Docker Compose ===
docker compose down

echo.
echo === Starting all services ===
docker compose up -d