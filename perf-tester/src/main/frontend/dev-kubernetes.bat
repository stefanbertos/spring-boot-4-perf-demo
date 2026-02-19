@echo off
REM Start the frontend dev server proxying /api to the Kubernetes ingress (kubectl port-forward or localhost)
set VITE_API_TARGET=http://localhost

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
echo API proxy target: %VITE_API_TARGET% ^(Kubernetes ingress^)
echo Open: http://localhost:5173
echo.
npm run dev --workspace=app
