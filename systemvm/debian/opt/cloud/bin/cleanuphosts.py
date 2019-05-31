#!/usr/bin/python
# -- coding: utf-8 --
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

import base64
import fcntl
import getopt
import json
import os
import sys

DNS_HOSTS = "/etc/hosts"
DHCP_HOSTS = "/etc/dhcphosts.txt"


def main(argv):
    macaddr = ''
    ip = ''

    try:
        opts, args = getopt.getopt(argv, "i:m:")
    except getopt.GetoptError:
        print "usage: cleanuphosts.py -i <IP> -m <MAC>"
        sys.exit(2)

    for opt, arg in opts:
        if opt == '-m':
            macaddr = arg
        elif opt == '-i':
            ip = arg

    if ip:
        delete_hosts(DNS_HOSTS, ip)

    if macaddr:
        delete_hosts(DHCP_HOSTS, macaddr)


def delete_hosts(hostsfile, param):
    try:
        delimiter = ','
        if hostsfile == DNS_HOSTS:
            delimiter = '\t'

        with open(hostsfile, 'rw+') as f:
            fcntl.flock(f, fcntl.LOCK_EX)
            content = filter(lambda x: param != x.split(delimiter)[0], f.readlines())
            f.seek(0)
            f.truncate()
            f.writelines(content)
            f.flush()
            fcntl.flock(f, fcntl.LOCK_UN)

    except Exception as e:
        print "Failed to cleanup entry on file " + hostsfile + " due to : " + str(e)
        sys.exit(1)


if __name__ == "__main__":
    main(sys.argv[1:])
