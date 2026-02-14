@echo off
setlocal enabledelayedexpansion

echo ============================================
echo  Docker Compose Cleanup Script
echo ============================================
echo.

:: ============================================
:: Step 1: Stop and remove containers, networks, volumes
:: ============================================
echo [1/2] Stopping containers, removing networks and volumes...
docker compose -f infrastructure/docker/compose.yaml down -v
if %ERRORLEVEL% NEQ 0 (
    echo WARNING: docker compose down had issues, continuing cleanup...
)
echo      Containers, networks, and volumes removed.
echo.

:: ============================================
:: Step 2: Remove built images
:: ============================================
echo [2/2] Removing built images...
for %%i in (config-server perf-tester ibm-mq-consumer kafka-consumer api-gateway perf-ui) do (
    docker rmi infrastructure-%%i 2>nul
    if !ERRORLEVEL! EQU 0 (
        echo      Removed image: infrastructure-%%i
    )
)
echo      Image cleanup complete.
echo.

echo ============================================
echo  Docker Compose Cleanup Complete!
echo ============================================
echo.
echo All containers, networks, volumes, and images have been removed.
echo Run 'infrastructure\runLocalDocker.bat' to rebuild everything.

endlocal
