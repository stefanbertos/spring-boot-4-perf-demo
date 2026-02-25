@echo off
setlocal enabledelayedexpansion

:: ============================================
:: Detect JAVA_HOME from %USERPROFILE%\.jdks
:: ============================================
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" goto :java_ok
)
set "JDKS_DIR=%USERPROFILE%\.jdks"
if not exist "%JDKS_DIR%" (
    echo ERROR: JAVA_HOME is not set and %JDKS_DIR% not found.
    echo Please set JAVA_HOME or install a JDK via IntelliJ IDEA ^(File ^> Project Structure ^> SDKs^).
    exit /b 1
)
set "FOUND_JAVA="
for /d %%d in ("%JDKS_DIR%\*25*") do (
    if exist "%%d\bin\java.exe" if not defined FOUND_JAVA set "FOUND_JAVA=%%d"
)
if not defined FOUND_JAVA (
    for /d %%d in ("%JDKS_DIR%\*") do (
        if exist "%%d\bin\java.exe" if not defined FOUND_JAVA set "FOUND_JAVA=%%d"
    )
)
if not defined FOUND_JAVA (
    echo ERROR: No JDK found in %JDKS_DIR%.
    echo Please install a JDK via IntelliJ IDEA ^(File ^> Project Structure ^> SDKs^).
    exit /b 1
)
set "JAVA_HOME=%FOUND_JAVA%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
:java_ok
echo Using JAVA_HOME: %JAVA_HOME%
echo.

echo === Building Gradle projects ===
pushd "%~dp0.."
call gradlew.bat build
if %ERRORLEVEL% NEQ 0 (
    echo Gradle build failed!
    popd
    exit /b 1
)
popd

echo.
echo === Rebuilding Docker images ===
docker compose -f "%~dp0docker/compose.yaml" build config-server perf-tester ibm-mq-consumer kafka-consumer api-gateway

if %ERRORLEVEL% NEQ 0 (
    echo Image build failed!
    exit /b 1
)

echo.
echo === Stopping Docker Compose ===
docker compose -f "%~dp0docker/compose.yaml" down

echo.
echo === Starting all services ===
docker compose -f "%~dp0docker/compose.yaml" up --build -d

echo ============================================
echo  Ingress URLs
echo ============================================
echo.
echo Access the services:
echo   - Perf UI:         http://localhost:8090
echo   - Grafana:         http://localhost:3000 (admin/admin)
echo   - Prometheus:      http://localhost:9090
echo   - Kafdrop:         http://localhost:9000/kafdrop
echo   - Redis Commander: http://localhost:8083/redis-commander
echo   - Loki:            http://localhost:3100
echo   - Tempo:           http://localhost:3200
echo   - MQ Console:      https://localhost:9443/ibmmq/console (admin/passw0rd)
echo   - SonarQube:       http://localhost:9001 (admin/admin)
echo   - Config Server:   http://localhost:8888

endlocal
