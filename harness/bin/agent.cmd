@echo off
REM Exact arg sequence required for windows - example
REM C:\faban\bin\faban.cmd nip12 nip12 129.146.239.77 C:\Java\jdk1.6.0_01 
REM -Dfaban.home=C:\\faban\\ -Djava.security.policy=C:\\faban\\config\\faban.policy
REM -Djava.util.logging.config.file=C:\\faban\\config\\logging.properties
REM -Dfaban.registry.port=9998 -Dfaban.logging.port=9999
REM -Xmx256m -Xms64m -XX:+DisableExplicitGC faban.benchmarkName=web101

REM set BINDIR and FABAN_HOME
set BINDIR=%~dp0
REM %~d0
cd %BINDIR%
cd ..
set FABAN_HOME=%CD%
cd logs

set JAVA=%4%\bin\java.exe
rem If no Java where it was specified to be, search PATH
if not exist %JAVA% for %%j in ( %PATH% ) do if exist %%j\java.exe set JAVA=%%j\java.exe
rem Couldn't find it, so exit with an error
if not exist %JAVA% (
       echo "JAVA_HOME was not specified correctly, reschedule a Faban run with the correct path!"
       goto :EOF
)

rem Check the PATHEXT and set it to the default if not exists
if not defined PATHEXT set PATHEXT=.COM;.EXE;.BAT;.CMD;.VBS;.VBE;.JS;.JSE;.WSF;.WSH;

set CLASSPATH=%FABAN_HOME%\lib\fabancommon.jar;%FABAN_HOME%\lib\fabanagents.jar
set PATH=%PATH%;%BINDIR%
echo Starting CmdAgent >>cmdagent.log

REM Use the client JVM. It is much lighter weight, less threads, less memory
REM and we do not need much performance for the agent.

if "%2"=="" goto ONEARG
REM Here we see how we rely on the exact arg sequence
start /b %JAVA% -client -cp %CLASSPATH% -Dfaban.cli.command=%0 %5=%6 %7=%8 -Dfaban.pathext=%PATHEXT% com.sun.faban.harness.agent.AgentBootstrap %* >>agent.log 2>&1
goto :EOF

:ONEARG
REM This is either daemon mode or just querying for help
start /b %JAVA% -client -cp %CLASSPATH% -Dfaban.cli.command=%0 -Dfaban.pathext=%PATHEXT% com.sun.faban.harness.agent.AgentBootstrap %* >>agent.log 2>&1