@echo off
rem  Licensed to the Apache Software Foundation (ASF) under one
rem  or more contributor license agreements.  See the NOTICE file
rem  distributed with this work for additional information
rem  regarding copyright ownership.  The ASF licenses this file
rem  to you under the Apache License, Version 2.0 (the
rem  "License"); you may not use this file except in compliance
rem  with the License.  You may obtain a copy of the License at
rem  
rem    http://www.apache.org/licenses/LICENSE-2.0
rem  
rem  Unless required by applicable law or agreed to in writing,
rem  software distributed under the License is distributed on an
rem  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
rem  KIND, either express or implied.  See the License for the
rem  specific language governing permissions and limitations
rem  under the License.

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
