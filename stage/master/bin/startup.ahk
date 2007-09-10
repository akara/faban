; -----------------------------------------------------------------------------
; Autohotkey source for CATALINA startup script
; This starts up the engine under Windows to host the Faban master web interface.
;
; startup.ahk,v 1.0 2007/04/27 djmorse
; -----------------------------------------------------------------------------
#NoEnv
#NoTrayIcon

PRGDIR := A_ScriptDir
JDK_VENDOR = JavaSoft ; could be JRockit or any other JDK

; Determine current Java version from registry
RegRead, JavaCurrentVersion, HKEY_LOCAL_MACHINE, SOFTWARE\%JDK_VENDOR%\Java Development Kit, CurrentVersion
If JavaCurrentVersion not in 1.5,1.6,1.7
{
	MsgBox, Java version is %JavaCurrentVersion%.  Faban needs 1.5 or later.`nPlease install the appropriate JDK.
	Exit, 1
}
;MsgBox, Success, current Java version = %JavaCurrentVersion%

RegRead, JavaHome, HKEY_LOCAL_MACHINE, SOFTWARE\%JDK_VENDOR%\Java Development Kit\%JavaCurrentVersion%, JavaHome
If ErrorLevel
{
	MsgBox, Java version is %JavaCurrentVersion%, but could not obtain JavaHome registry entry.  Please reinstall JDK.
	Exit, 1
}
EnvSet, JAVA_HOME, %JavaHome%

EnvGet, PathExt, PATHEXT
if (PathExt = "")
	PathExt := ".COM;.EXE;.BAT;.CMD;.VBS;.VBE;.JS;.JSE;.WSF;.WSH;"

EnvSet, JAVA_OPTS, -Xms64m -Xmx1024m -Djava.awt.headless=true -Dfaban.pathext=%PathExt%

; Unpack a fresh copy of Faban
; webapps directory is on the same level as the current script (in "webapps" instead of "bin")
StringReplace, WebAppsDir, PRGDIR, bin, webapps
SetWorkingDir, %WebAppsDir%
FileRemoveDir, faban, 1
FileRemoveDir, xanadu
FileCreateDir, faban
SetWorkingDir, faban
Run, %JavaHome%\bin\jar xf ../faban.war, , Hide

SetWorkingDir, %PRGDIR%
Run, %comspec% /c start "Tomcat" catalina.bat run %1% %2% %3% %4% %5%

; Display a hyperlink to launch the Faban web interface
Sleep, 5000
Gui, +AlwaysOnTop
Gui, Font, underline
Gui, Add, Text, cBlue gLaunchFaban, Click here to launch the Web interface.
Gui, Font, norm
Gui, Show, Center, Faban
return

LaunchFaban:
Run, http://%A_ComputerName%:9980/
GuiEscape:
GuiClose:
ExitApp
