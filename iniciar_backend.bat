@echo off
title SubastyMED Backend y Conexión Móvil
cd /d "%~dp0"

echo ====================================================
echo        Iniciando Servidor SubastyMED y Enlace ADB
echo ====================================================
echo.

:: Configurar ADB reverse para que el celular por cable siempre conecte
if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
    echo Configurando tunel ADB hacia el celular...
    "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" reverse tcp:8000 tcp:8000
    echo Tunel ADB activado en el puerto 8000.
)

echo.
echo Documentacion API: http://localhost:8000/docs
echo Servidor escuchando en: http://0.0.0.0:8000
echo.
echo Presiona Ctrl+C para detener el servidor.
echo ====================================================
echo.

cd backend
.venv\Scripts\python.exe run.py
pause
