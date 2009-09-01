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
if not exist %JAVA% ( for %%j in ("java.exe") do (set JAVA=%%~$PATH:j))
rem Could not find it, so exit with an error
if not exist %JAVA% (
       echo "JAVA_HOME was not specified correctly, reschedule a Faban run with the correct path!"
       goto :EOF
)

rem Check the PATHEXT and set it to the default if not exists
if not defined PATHEXT set PATHEXT=.COM;.EXE;.BAT;.CMD;.VBS;.VBE;.JS;.JSE;.WSF;.WSH;

set CLASSPATH=%FABAN_HOME%\lib\fabancommon.jar;%FABAN_HOME%\lib\fabanagents.jar;%FABAN_HOME%\lib\commons-httpclient-3.1.jar;%FABAN_HOME%\lib\commons-codec-1.2.jar;%FABAN_HOME%\lib\commons-logging.jar
set PATH=%PATH%;%BINDIR%
echo Starting CmdAgent >>cmdagent.log

REM Use the client JVM if possible. It is much lighter weight, less threads, less memory
REM and we do not need much performance for the agent.
%JAVA% -client -version >NUL 2>&1
if NOT ERRORLEVEL 1 set JAVA=%JAVA% -client

if "%2"=="" goto ONEARG
REM Here we see how we rely on the exact arg sequence
start /b %JAVA% -cp %CLASSPATH% -Djava.rmi.server.RMIClassLoaderSpi=com.sun.faban.harness.agent.RMIClassLoaderProvider -Dfaban.cli.command=%0 %5=%6 %7=%8 -Dfaban.pathext=%PATHEXT% com.sun.faban.harness.agent.AgentBootstrap %* >>agent.log 2>&1
goto :EOF

:ONEARG
REM This is either daemon mode or just querying for help
start /b %JAVA% -cp %CLASSPATH% -Djava.rmi.server.RMIClassLoaderSpi=com.sun.faban.harness.agent.RMIClassLoaderProvider -Dfaban.cli.command=%0 -Djava.security.policy=%FABAN_HOME%\\config\\faban.policy -Djava.util.logging.config.file=%FABAN_HOME%\\config\\logging.properties -Dfaban.pathext=%PATHEXT% com.sun.faban.harness.agent.AgentBootstrap %* >>agent.log 2>&1
FOR /F "tokens=2" %%i in ('%JAVA_HOME%\bin\jps') do IF /i %%i==AgentBootstrap echo Faban Agent started successfully in daemon mode.  Close this window to terminate the agent.
