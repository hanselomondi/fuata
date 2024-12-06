@echo off
setlocal enabledelayedexpansion

echo Installing Fuata CLI...

rem Define variables for source and target paths
set "JAR_SOURCE=build\libs\fuata-1.0-SNAPSHOT.jar"
set "INSTALL_DIR=C:\Program Files\Fuata"
set "JAR_TARGET=%INSTALL_DIR%\fuata-1.0.jar"
set "WRAPPER_SCRIPT=%INSTALL_DIR%\fuata.bat"

rem Check if the JAR file exists
if not exist "%JAR_SOURCE%" (
    echo Error: JAR file '%JAR_SOURCE%' not found. Aborting.
    exit /b 1
)

rem Ensure the installation directory exists
if not exist "%INSTALL_DIR%" (
    mkdir "%INSTALL_DIR%"
    if errorlevel 1 (
        echo Error: Failed to create installation directory '%INSTALL_DIR%'. Aborting.
        exit /b 1
    )
)

rem Copy the JAR file
copy "%JAR_SOURCE%" "%JAR_TARGET%" >nul
if errorlevel 1 (
    echo Error: Failed to copy JAR file to '%JAR_TARGET%'. Aborting.
    exit /b 1
)

rem Create the wrapper script
(
    echo @echo off
    echo java -jar "%JAR_TARGET%" %%*
) > "%WRAPPER_SCRIPT%"
if errorlevel 1 (
    echo Error: Failed to create wrapper script '%WRAPPER_SCRIPT%'. Aborting.
    exit /b 1
)

rem Display success message only if all operations succeed
echo Installation complete. Add "%INSTALL_DIR%" to your PATH to use the 'fuata' command.
exit /b 0
