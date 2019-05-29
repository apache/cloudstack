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

DHCP_HOSTS = "/etc/dhcphosts.txt"
DNS_HOSTS = "/etc/hosts"


def main(argv):
    macaddr = ''
    ip = ''
    type = ''

    try:
        opts, args = getopt.getopt(argv, "m:i:t:")
    except getopt.GetoptError:
        print 'params: -m <MAC> -i <IP> -t <TYPE>'
        sys.exit(2)

    for opt, arg in opts:
        if opt == '-m':
            macaddr = arg
        elif opt == '-i':
            ip = arg
        elif opt == '-t':
            type = arg

    if type == "dhcphosts":
        delete_hosts(DHCP_HOSTS, macaddr)
    elif type == "dnshosts":
        delete_hosts(DNS_HOSTS, ip)


def delete_hosts(hostsfile, param):
    try:
        persist = []
        delimiter = ','
        if hostsfile == DNS_HOSTS:
            delimiter = '\t'

        read = open(hostsfile, 'r')
        for line in read:
            opts = line.split(delimiter)
            if param != opts[0]:
                persist.append(line)
        read.close()

        write = open(hostsfile, 'w')
        for line in persist:
            write.write(line)
        write.close()
        return True
    except Exception as e:
        print "Failed to cleanup entry on file " + hostsfile + " due to : " + e.strerror
        sys.exit(1)


if __name__ == "__main__":
    main(sys.argv[1:])
