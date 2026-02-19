@echo off
REM Start perf-ui dev server against local Docker Compose (api-gateway on port 8090)
set VITE_API_TARGET=http://localhost:8090
echo Starting perf-ui dev server...
echo API proxy target: %VITE_API_TARGET% (Docker Compose)
echo.
npm run dev
