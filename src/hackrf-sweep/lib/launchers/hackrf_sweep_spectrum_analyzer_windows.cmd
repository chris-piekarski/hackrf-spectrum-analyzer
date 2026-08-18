@echo off
setlocal EnableDelayedExpansion
set MIN_JAVA=21
if defined JAVA_HOME (
  set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA_BIN=java"
)

"%JAVA_BIN%" -version >nul 2>&1
if errorlevel 1 (
  echo Java %MIN_JAVA% or newer is required but java was not found.
  echo Install a JDK 21+ from https://adoptium.net/
  exit /b 1
)

set "JVER="
for /f tokens^=2^ delims^=^" %%v in ('"%JAVA_BIN%" -version 2^>^&1') do (
  set "JVER=%%v"
  goto :gotver
)
:gotver
if not defined JVER (
  echo Could not parse Java version.
  "%JAVA_BIN%" -version
  exit /b 1
)
for /f "tokens=1,2 delims=." %%a in ("!JVER!") do (
  if "%%a"=="1" (set "JMAJ=%%b") else (set "JMAJ=%%a")
)
if not defined JMAJ (
  echo Could not parse Java version from !JVER!.
  exit /b 1
)
if !JMAJ! LSS %MIN_JAVA% (
  echo Java %MIN_JAVA% or newer is required (found !JVER!).
  echo Install a JDK 21+ from https://adoptium.net/
  exit /b 1
)

cd /d "%~dp0"
"%JAVA_BIN%" -Djna.platform.library.path=lib\win32-x86-64 -jar "%~dp0lib\hackrf_sweep_spectrum_analyzer.jar" %*
endlocal
