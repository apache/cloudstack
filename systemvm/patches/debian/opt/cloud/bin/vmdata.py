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

import sys
import getopt
import json
import os
import base64
from fcntl import flock, LOCK_EX, LOCK_UN


def main(argv):
    fpath = ''
    b64data = ''

    try:
        opts, args = getopt.getopt(argv, "f:d:")
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
            file = item[1]
            data = item[2]

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
    metamanifest = metamanifestdir + "/meta-data"

    # base64 decode userdata
    if folder == "userdata" or folder == "user-data":
        if data is not None:
            data = base64.b64decode(data)

    fh = open(dest, "w")
    exflock(fh)
    if data is not None:
        fh.write(data)
    else:
        fh.write("")
    unflock(fh)
    fh.close()
    os.chmod(dest, 0644)

    if folder == "metadata" or folder == "meta-data":
        try:
            os.makedirs(metamanifestdir, 0755)
        except OSError as e:
            # error 17 is already exists, we do it this way for concurrency
            if e.errno != 17:
                print "failed to make directories " + metamanifestdir + " due to :" + e.strerror
                sys.exit(1)
        if os.path.exists(metamanifest):
            fh = open(metamanifest, "r+a")
            exflock(fh)
            if file not in fh.read():
                fh.write(file + '\n')
            unflock(fh)
            fh.close()
        else:
            fh = open(metamanifest, "w")
            exflock(fh)
            fh.write(file + '\n')
            unflock(fh)
            fh.close()

    if os.path.exists(metamanifest):
        os.chmod(metamanifest, 0644)


def htaccess(ip, folder, file):
    entry = "Options -Indexes\nOrder Deny,Allow\nDeny from all\nAllow from " + ip
    htaccessFolder = "/var/www/html/" + folder + "/" + ip
    htaccessFile = htaccessFolder+"/.htaccess"

    try:
        os.makedirs(htaccessFolder, 0755)
    except OSError as e:
        # error 17 is already exists, we do it this way for sake of concurrency
        if e.errno != 17:
            print "failed to make directories " + htaccessFolder + " due to :" + e.strerror
            sys.exit(1)

    fh = open(htaccessFile, "w")
    exflock(fh)
    fh.write(entry + '\n')
    unflock(fh)
    fh.close()


def exflock(file):
    try:
        flock(file, LOCK_EX)
    except IOError as e:
        print "failed to lock file" + file.name + " due to : " + e.strerror
        sys.exit(1)
    return True


def unflock(file):
    try:
        flock(file, LOCK_UN)
    except IOError:
        print "failed to unlock file" + file.name + " due to : " + e.strerror
        sys.exit(1)
    return True

if __name__ == "__main__":
    main(sys.argv[1:])
