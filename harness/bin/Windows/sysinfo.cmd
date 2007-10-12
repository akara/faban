@echo off
REM #########################################################################
REM # The sysinfo script reads many files and calls many platform-dependent
REM # tools to determine the system configuration. The output is a html
REM # snippet recording the files and tools output.
REM #########################################################################

echo         ^<h3^>System information for server %COMPUTERNAME%^</h3^>
echo         ^<pre^>
systeminfo
echo         ^</pre^>
