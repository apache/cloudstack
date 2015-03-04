#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.


# Copies keys that enable SSH communication with system vms
# $1 = new public key
# $2 = new private key
'''
All imports go here...
'''
from subprocess import call
import socket
import sys
import os
import subprocess
import traceback

def generateSSLKey(outputPath):
  logf = open("ssl-keys.log", "w")
  hostName = socket.gethostbyname(socket.gethostname())
  keyFile = outputPath + os.sep + "cloudmanagementserver.keystore"
  logf.write("HostName = %s\n" % hostName)
  logf.write("OutputPath = %s\n" % keyFile)
  dname='cn="Cloudstack User",ou="' + hostName + '",o="' + hostName + '",c="Unknown"';
  logf.write("dname = %s\n" % dname)
  logf.flush()
  try :
    return_code = subprocess.Popen(["keytool", "-genkey", "-keystore", keyFile, "-storepass", "vmops.com", "-keypass", "vmops.com", "-keyalg", "RSA", "-validity", "3650", "-dname", dname],shell=True,stdout=logf, stderr=logf)
    return_code.wait()
  except OSError as e:
    logf.flush()
    traceback.print_exc(file=logf)
  logf.flush()
  logf.write("SSL key generated is : %s" % return_code)
  logf.flush()

argsSize=len(sys.argv)
if argsSize != 2:
	print("Usage: ssl-keys.py <SSL File Key Path>")
	sys.exit(None)
sslKeyPath=sys.argv[1]

generateSSLKey(sslKeyPath)