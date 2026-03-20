@ECHO OFF
SETLOCAL

set SCRIPT_DIR=%~dp0
set WRAPPER_DIR=%SCRIPT_DIR%.mvn\wrapper
set DIST_NAME=apache-maven-3.9.9
set DIST_ARCHIVE=%WRAPPER_DIR%\%DIST_NAME%-bin.zip
set DIST_HOME=%WRAPPER_DIR%\dists\%DIST_NAME%
set MAVEN_HOME=%DIST_HOME%\%DIST_NAME%
set DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/%DIST_NAME%-bin.zip

if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
if not exist "%WRAPPER_DIR%\dists" mkdir "%WRAPPER_DIR%\dists"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  echo Downloading Maven %DIST_NAME%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%DIST_ARCHIVE%'"
  if errorlevel 1 exit /b 1

  if exist "%DIST_HOME%" rmdir /s /q "%DIST_HOME%"
  mkdir "%DIST_HOME%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%DIST_ARCHIVE%' -DestinationPath '%DIST_HOME%' -Force"
  if errorlevel 1 exit /b 1
)

call "%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%