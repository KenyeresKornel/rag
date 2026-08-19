@echo off
setlocal enabledelayedexpansion
set "BASE_DIR=%~dp0"
set "PROPS=%BASE_DIR%.mvn\wrapper\maven-wrapper.properties"
for /f "tokens=1,* delims==" %%A in (%PROPS%) do (
  if "%%A"=="distributionUrl" set "DIST_URL=%%B"
)
for %%F in (!DIST_URL!) do set "DIST_FILE=%%~nxF"
set "DIST_NAME=!DIST_FILE:-bin.zip=!"
if not defined MAVEN_USER_HOME set "MAVEN_USER_HOME=%USERPROFILE%\.m2"
set "MAVEN_HOME=!MAVEN_USER_HOME!\wrapper\dists\!DIST_NAME!"

if not exist "!MAVEN_HOME!\bin\mvn.cmd" (
  set "TMP_DIR=%TEMP%\mvnw-%RANDOM%-%RANDOM%"
  mkdir "!TMP_DIR!" >nul 2>&1
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri '!DIST_URL!' -OutFile '!TMP_DIR!\!DIST_FILE!'; Expand-Archive -Path '!TMP_DIR!\!DIST_FILE!' -DestinationPath '!TMP_DIR!' -Force"
  if errorlevel 1 exit /b 1
  mkdir "!MAVEN_HOME!" >nul 2>&1
  xcopy /E /I /Q /Y "!TMP_DIR!\!DIST_NAME!\*" "!MAVEN_HOME!\" >nul
  rmdir /S /Q "!TMP_DIR!"
)

call "!MAVEN_HOME!\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%
