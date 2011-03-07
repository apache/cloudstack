REM signtool sign /f vmopscert.pfx /p vmops /t http://tsa.starfieldtech.com %1\CloudInstanceManager.msi
REM signtool sign /f vmopscert.pfx /p vmops /t http://tsa.starfieldtech.com %1\CloudInstanceManager.msi
signtool sign /f code_signing_cert.pfx /p vmops.com /t http://tsa.starfieldtech.com %1\CloudInstanceManager.msi
signtool sign /f code_signing_cert.pfx /p vmops.com /t http://tsa.starfieldtech.com %1\CloudInstanceManager.msi



