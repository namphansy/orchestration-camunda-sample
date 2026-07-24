@echo off
setlocal

set "BASEDIR=%~dp0"
set "MVN_DIR=%BASEDIR%.mvn\wrapper"
set "MAVEN_VERSION=3.9.10"
set "MAVEN_HOME=%MVN_DIR%\apache-maven-%MAVEN_VERSION%"
set "MAVEN_ZIP=%MVN_DIR%\apache-maven-%MAVEN_VERSION%-bin.zip"
set "MAVEN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  if not exist "%MVN_DIR%" mkdir "%MVN_DIR%"
  if not exist "%MAVEN_ZIP%" (
    curl.exe -fsSL "%MAVEN_URL%" -o "%MAVEN_ZIP%"
    if errorlevel 1 exit /b 1
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%MAVEN_ZIP%' -DestinationPath '%MVN_DIR%' -Force"
  if errorlevel 1 exit /b 1
)

call "%MAVEN_HOME%\bin\mvn.cmd" %*

