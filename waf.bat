@echo off

if exist jython.jar goto runjython
if exist C:\Python26\Python.exe goto runpython
goto helprun

:helprun
echo To run waf with Python
echo 1. Download Python 2.6 for Windows from http://www.python.org/
echo 2. Install it in C:\Python2.6 (the default path)
echo 3. Use this batch file to run waf
echo ""
echo To run waf without installing Python
echo 1. Download the Jython installer from http://wiki.python.org/jython/
echo 2. Install it to this directory in standalone mode
echo 3. Ensure the java command is on your PATH variable
echo 3. Use this batch file to run waf
goto end

:runjython
java -jar jython.jar waf %*
goto end

:runpython
C:\Python26\Python.exe waf %*
goto end

:end