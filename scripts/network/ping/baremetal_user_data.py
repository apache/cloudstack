#!/usr/bin/env python3

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

import sys
import base64
from pathlib import Path

HTML_ROOT = "/var/www/html/"

def writeIfNotHere(fileName, texts):
    if not Path(filename).exists():
        entries = []
    else:
        f = Path(filename).open(mode='r')
        entries = f.readline()

    texts = [ "{}\n".format(t) for t in texts ]
    need = False
    for t in texts:
        if not t in entries:
            entries.append(t)
            need = True

    if need:
        f = Path(filename).open(mode='w')
        f.write(''.join(entries))

def createRedirectEntry(vmIp, folder, filename):
    entry = "RewriteRule ^{0}$  ../{1}/%%{REMOTE_ADDR}/{2} [L,NC,QSA]".format(filename,folder,filename)
    htaccessFolder = "/var/www/html/latest"
    htaccessFile = Path(htaccessFolder).joinpath(".htaccess")
    if not Path(htaccessFolder).exists():
        Path(htaccessFolder).mkdir()
    writeIfNotHere(htaccessFile, ["Options +FollowSymLinks", "RewriteEngine On", entry])

    htaccessFolder = Path(HTML_ROOT).joinpath(folder, vmIp)
    if not Path.exists(htaccessFolder):
        Path(htaccessFolder).mkdir()
    htaccessFile = Path(htaccessFolder).joinpath(".htaccess")
    entry="Options -Indexes\nOrder Deny,Allow\nDeny from all\nAllow from {}".format(vmIp)
    f = Path(htaccessFile).open(mode='w')
    f.write(entry)

    if folder in ['metadata', 'meta-data']:
        entry1 = "RewriteRule ^meta-data/(.+)$  ../{}/%%{REMOTE_ADDR}/$1 [L,NC,QSA]".format(folder)
        htaccessFolder = "/var/www/html/latest"
        htaccessFile = Path(htaccessFolder).joinpath(".htaccess")
        entry2 = "RewriteRule ^meta-data/$  ../{}/%%{REMOTE_ADDR}/meta-data [L,NC,QSA]".format(folder)
        writeIfNotHere(htaccessFile, [entry1, entry2])


def addUserData(vmIp, folder, fileName, contents):

    baseFolder = Path(HTML_ROOT).joinpath(folder, vmIp)
    if not Path(baseFolder).exists():
        Path(baseFolder).mkdir()

    createRedirectEntry(vmIp, folder, fileName)

    datafileName = Path(HTML_ROOT).joinpath(folder, vmIp, fileName)
    metaManifest = Path(HTML_ROOT).joinpath(folder, vmIp, "meta-data")
    if folder == "userdata":
        if contents != "none":
            contents = base64.urlsafe_b64decode(contents)
        else:
            contents = ""

    f = Path(datafileName).open(mode='w')
    f.write(contents)

    if folder == "metadata" or folder == "meta-data":
        writeIfNotHere(metaManifest, [fileName])

if __name__ == '__main__':
    string = sys.argv[1]
    allEntires = string.split(";")
    for entry in allEntires:
        (vmIp, folder, fileName, contents) = entry.split(',', 3)
        addUserData(vmIp, folder, fileName, contents)
    sys.exit(0)
