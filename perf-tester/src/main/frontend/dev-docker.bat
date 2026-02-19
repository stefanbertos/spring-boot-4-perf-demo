@echo off
REM Start the frontend dev server proxying /api to the local Docker Compose api-gateway
set VITE_API_TARGET=http://localhost:8090

if not exist node_modules (
    echo Installing npm workspace dependencies...
    call npm install
    if errorlevel 1 (
        echo npm install failed
        exit /b 1
    )
)

echo.
echo Starting frontend dev server...
echo API proxy target: %VITE_API_TARGET% ^(Docker Compose api-gateway^)
echo Open: http://localhost:5173
echo.
npm run dev --workspace=app
