#!/usr/bin/python
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

import sys, getopt, json, os, base64

def main(argv):
    fpath =  ''
    b64data = ''

    try:
        opts, args = getopt.getopt(argv,"f:d:")
    except getopt.GetoptError:
        print 'params: -f <filename> -d <b64jsondata>'
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-f':
            fpath = arg
        elif opt == '-d':
            b64data = arg

    json_data = ''
    if fpath != '':
        fh = open(fpath, 'r')
        json_data = json.loads(fh.read())
    elif b64data != '':
        json_data = json.loads(base64.b64decode(b64data))
    else:
        print '-f <filename> or -d <b64jsondata> required'
        sys.exit(2)

    for ip in json_data:
        for item in json_data[ip]:
            folder = item[0]
            file   = item[1]
            data   = item[2]

            # process only valid data
            if folder != "userdata" and folder != "metadata":
                continue

            if file == "":
                continue

            htaccess(ip, folder, file)

            if data == "":
                deletefile(ip, folder, file)
            else:
                createfile(ip, folder, file, data)
    
    if fpath != '':
        fh.close()
        os.remove(fpath)

def deletefile(ip, folder, file):
    datafile = "/var/www/html/" + folder + "/" + ip + "/" + file

    if os.path.exists(datafile):
        os.remove(datafile)

def createfile(ip, folder, file, data):
    dest = "/var/www/html/" + folder + "/" + ip + "/" + file
    metamanifestdir = "/var/www/html/" + folder + "/" + ip 
    metamanifest =  metamanifestdir + "/meta-data"

    # base64 decode userdata
    if folder == "userdata" or folder == "user-data":
        if data is not None:
            data = base64.b64decode(data)

    if data is not None:
        open(dest, "w").write(data)
    else:
        open(dest, "w").write("")
    os.chmod(dest, 0644)

    if folder == "metadata" or folder == "meta-data":
        if not os.path.exists(metamanifestdir):
            os.makedirs(metamanifestdir, 0755)
        if os.path.exists(metamanifest):
            if not file in open(metamanifest).read():
                open(metamanifest, "a").write(file + '\n')
        else:
            open(metamanifest, "w").write(file + '\n')

    if os.path.exists(metamanifest):
        os.chmod(metamanifest, 0644)

def htaccess(ip, folder, file):
    entry = "RewriteRule ^" + file + "$  ../" + folder + "/%{REMOTE_ADDR}/" + file + " [L,NC,QSA]"
    htaccessFolder = "/var/www/html/latest"
    htaccessFile = htaccessFolder + "/.htaccess"

    if not os.path.exists(htaccessFolder):
        os.mkdir(htaccessFolder,0755)

    if os.path.exists(htaccessFile):
        if not entry in open(htaccessFile).read():
            open(htaccessFile, "a").write(entry + '\n')

    entry="Options -Indexes\nOrder Deny,Allow\nDeny from all\nAllow from " + ip
    htaccessFolder = "/var/www/html/" + folder + "/" + ip
    htaccessFile = htaccessFolder+"/.htaccess"

    if not os.path.exists(htaccessFolder):
        os.makedirs(htaccessFolder,0755)

    open(htaccessFile, "w").write(entry + '\n')

    if folder == "metadata" or folder == "meta-data":
        entry="RewriteRule ^meta-data/(.+)$  ../" + folder + "/%{REMOTE_ADDR}/$1 [L,NC,QSA]"
        htaccessFolder = "/var/www/html/latest"
        htaccessFile = htaccessFolder + "/.htaccess"

        if not entry in open(htaccessFile).read():
            open(htaccessFile, "a").write(entry + '\n')

        entry="RewriteRule ^meta-data/$  ../" + folder + "/%{REMOTE_ADDR}/meta-data [L,NC,QSA]"

        if not entry in open(htaccessFile).read():
            open(htaccessFile, "a").write(entry + '\n')


if __name__ == "__main__":
    main(sys.argv[1:])
