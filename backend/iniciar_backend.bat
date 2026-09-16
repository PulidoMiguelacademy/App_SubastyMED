@echo off
title SubastyMED Backend
cd /d "%~dp0"
echo ====================================================
echo           Iniciando Servidor SubastyMED API
echo ====================================================
echo.
echo Documentacion interactiva disponible en:
echo   http://localhost:8000/docs
echo.
.venv\Scripts\python.exe run.py
pause
