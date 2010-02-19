@echo off
REM set BINDIR and FABAN_HOME
set BINDIR=%~dp0
REM %~d0
cd %BINDIR%
cd ..
set FABAN_HOME=%CD%
cd master\webapps\fenxi
set FENXI_HOME=%CD%
cd scripts
set FENXI_SCRIPTS=%CD%

set FENXI_HOST=localhost

REM TODO: Deal with log files

cd %FABAN_HOME%\logs

rem set JAVA=%4%\bin\java.exe
rem If no Java where it was specified to be, search PATH
rem if not exist %JAVA% for %%j in ( %PATH% ) do if exist %%j\java.exe set JAVA=%%j\java.exe
rem Couldn't find it, so exit with an error
rem if not exist %JAVA% (
rem       echo "JAVA_HOME was not specified correctly, reschedule a Faban run with the correct path!"
rem       goto :EOF
rem )


REM TODO: Check file and rename, need to find way.

set PROG=%FENXI_SCRIPTS%\fenxi

rmdir /s /q txt

REM Get the command line arguments
set CMD_LINE_ARGS=
:setArgs
 set ARG_COUNT=%ARG_COUNT%+1
 if ""%1""=="""" goto doneSetArgs
 if ""%3""=="""" (
        if NOT ""%2""=="""" set LAST_ARG=%2
        if NOT ""%2""=="""" set PRE_LAST_ARG=%1
        )
 set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
 shift
 goto setArgs
:doneSetArgs

%PROG% %CMD_LINE_ARGS%
