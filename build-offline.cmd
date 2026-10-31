@echo off
rem Offline build helper: uses the local .m2repo folder so no network is required.
setlocal
set MVN="C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"
call %MVN% -o -Dmaven.repo.local=%~dp0.m2repo %*
endlocal
