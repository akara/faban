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

set JAVA=%4%\bin\java.exe
rem If no Java where it was specified to be, search PATH
if not exist %JAVA% for %%j in ( %PATH% ) do if exist %%j\java.exe set JAVA=%%j\java.exe
rem Couldn't find it, so exit with an error
if not exist %JAVA% (
       echo "JAVA_HOME was not specified correctly, reschedule a Faban run with the correct path!"
       goto :EOF
)


REM TODO: Check file and rename, need to find way.

set PROG=%FENXI_SCRIPTS/fenxi

rem Check the PATHEXT and set it to the default if not exists
if not defined PATHEXT set PATHEXT=.COM;.EXE;.BAT;.CMD;.VBS;.VBE;.JS;.JSE;.WSF;.WSH;

set CLASSPATH=%FENXI_HOME%\WEB-INF\lib\spark.jar;%FENXI_HOME%\WEB-INF\classes;%FENXI_HOME%\WEB-INF\lib\derby.jar;%FENXI_HOME%\WEB-INF\lib\jfreechart.jar;%FENXI_HOME%\WEB-INF\lib\jcommon.jar;%FENXI_HOME%\WEB-INF\lib\jruby.jar
set PERL5LIB=%FENXI_HOME%\txt2db

set JAVA_ARGS=-mx756 -Dderby.storage.pageReservedSpace=0 -Dderby.language.logQueryPlan=true -Dderby.storage.rowLocking=false -Dfenxi.basedir=%FENXI_HOME% -Djava.awt.headless=true -Dsun.java2d.pmoffscreen=false -Dfenxi.profile=default_profile

rmdir /s /q txt
mkdir txt

set PROCESS=%1
SHIFT

REM Shift does not change the value in %*.
if "%PROCESS%"=="process" (
	%JAVA% -cp %CLASSPATH% %JAVA_ARGS% -Dfenxi.profile=%PROFILE% org.fenxi.cmd.process.ProcessRun %1 %2 %3
)

if "%PROCESS%"=="compare" (
	%JAVA% -cp %CLASSPATH% %JAVA_ARGS% -Dfenxi.profile=%PROFILE% org.fenxi.cmd.compare.CompareDBinMemory %1 %2 %3
)


editXan() {
    sed 's%FenXi.jpg%/fenxi/html/FenXi.jpg%g' ${1} > ${1}.1
    sed 's%/xanadu/html%/fenxi/html%g' ${1}.1 > ${1}
    rm ${1}.1
}

cd $2
for i in *.xan*.html
do
    editXan $i
done
for i in *.log.*.html
do
    editXan $i
done
cd ..
rm -rf txt

# end hack removal
