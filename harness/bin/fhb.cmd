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

REM Use the client JVM if possible. It is much lighter weight, less threads, less memory
REM and we do not need much performance for the agent.
%JAVA% -client -version >NUL 2>&1
if NOT ERRORLEVEL 1 set JAVA=%JAVA% -client

%JAVA% -Xmx2m -cp %FABAN_HOME%\lib\fabancommon.jar -Dfaban.cli.command=%0 -Dfabanshell.needJDK=true -Dfabanshell.exec=com.sun.faban.driver.util.FabanHTTPBench com.sun.faban.common.FabanShell %*
