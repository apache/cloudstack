cabarc -s 6144 N vmopsvnc.cab vmopsvnc.dll cab.inf
signtool sign /f vmopscert.pfx /p vmops /t http://tsa.starfieldtech.com vmopsvnc.cab
copy vmopsvnc.cab ..\dist
