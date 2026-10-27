@echo off
setlocal
set "APP_HOME=%~dp0"
if not exist "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%APP_HOME%scripts\bootstrap-wrapper.ps1"
    if errorlevel 1 exit /b 1
)
if defined JAVA_HOME (
    "%JAVA_HOME%\bin\java.exe" -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
) else (
    java -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
)
exit /b %ERRORLEVEL%
