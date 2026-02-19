@echo off
REM Start perf-ui dev server against Kubernetes (ingress on port 80)
set VITE_API_TARGET=http://localhost
echo Starting perf-ui dev server...
echo API proxy target: %VITE_API_TARGET% (Kubernetes)
echo.
npm run dev
