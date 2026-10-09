@echo off
REM Requiere JDK 21 y Gradle 8.8 en el PATH
gradle build
echo.
echo El .jar esta en build\libs\gelmod-1.0.0.jar
pause
