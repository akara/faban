@echo off
REM set BINDIR and FABAN_HOME
set BINDIR=%~dp0
REM %~d0
cd %BINDIR%
cd ..
set FABAN_HOME=%CD%

set JAVA=%JAVA_HOME%\bin\java.exe
rem If no Java where it was specified to be, search PATH
if not exist %JAVA% for %%j in ( %PATH% ) do if exist %%j\java.exe (
       set JAVA=%%j\java.exe
       cd %%j
       cd ..
       set JAVA_HOME=%CD%
)

rem Couldn't find it, so exit with an error
if not exist %JAVA% (
       echo "JAVA_HOME was not specified correctly!"
       goto :EOF
)

set CLASSPATH=%FABAN_HOME%\lib\fabancommon.jar;%FABAN_HOME%\lib\fabanagents.jar;%FABAN_HOME%\lib\commons-httpclient-3.1.jar;%FABAN_HOME%\lib\commons-codec-1.2.jar;%FABAN_HOME%\lib\commons-logging.jar

REM Use the client JVM if possible. It is much lighter weight, less threads, less memory
REM and we do not need much performance for the agent.
%JAVA% -client -version >NUL 2>&1
if NOT ERRORLEVEL 1 set JAVA=%JAVA% -client

%JAVA% -Xmx4m -cp %CLASSPATH% -Dfaban.cli.command=%0 com.sun.faban.harness.util.CLI %*