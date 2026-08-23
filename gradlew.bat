@echo off
setlocal
set "GRADLE_VERSION=9.5.1"
set "GRADLE_HOME=%USERPROFILE%\.gradle\wrapper\custom-dists\gradle-%GRADLE_VERSION%"
set "GRADLE_ZIP=%TEMP%\gradle-%GRADLE_VERSION%-bin.zip"
set "GRADLE_URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"

if exist "%GRADLE_HOME%\bin\gradle.bat" goto run

if not exist "%GRADLE_HOME%" mkdir "%GRADLE_HOME%" >nul 2>&1

echo Downloading Gradle %GRADLE_VERSION%...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri '%GRADLE_URL%' -OutFile '%GRADLE_ZIP%'"
if errorlevel 1 (
  echo Failed to download Gradle %GRADLE_VERSION%.
  exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%GRADLE_ZIP%' -DestinationPath '%GRADLE_HOME%' -Force"
if errorlevel 1 (
  echo Failed to extract Gradle %GRADLE_VERSION%.
  exit /b 1
)

if exist "%GRADLE_HOME%\gradle-%GRADLE_VERSION%\bin\gradle.bat" (
  move /Y "%GRADLE_HOME%\gradle-%GRADLE_VERSION%\*" "%GRADLE_HOME%\" >nul
  rmdir /S /Q "%GRADLE_HOME%\gradle-%GRADLE_VERSION%" >nul 2>&1
)

del /Q "%GRADLE_ZIP%" >nul 2>&1

:run
call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
