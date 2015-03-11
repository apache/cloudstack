#!/usr/bin/python
#
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
#
import os, sys, subprocess, socket,fcntl, struct
from socket import gethostname
import errno
from socket import error as socket_error
from xml.dom.minidom import parseString

from xmlrpclib import ServerProxy, Error

def spCon(proto, auth, host, port):
    print "trying %s on %s@%s:%s" % (proto, auth, host, port)
    try:
        x=ServerProxy("%s://%s@%s:%s" % (proto, auth, host, port))
        x.echo(proto)
        return x
    except Error, v:
        return
    except socket_error, serr:
        return

def getCon(auth, host, port):
    try:
        server = spCon("http", auth, host, port)
        if server:
            return server
        else:
            server = spCon("https", auth, host, port)
    except Error, v:
        print "ERROR", v
    return server

def get_ip_address(ifname):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    return socket.inet_ntoa(fcntl.ioctl(
        s.fileno(),
        0x8915,  # SIOCGIFADDR
        struct.pack('256s', ifname[:15])
    )[20:24])

def is_it_up(host, port):
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(1)
        s.connect((host, port))
        s.close()
    except:
        print "host: %s:%s DOWN" % (host, port)
        return False

    print "host: %s:%s UP" % (host, port)
    return True

# hmm master actions don't apply to a slave
master="192.168.1.161"
port=8899
user = "oracle"
password = "test123"
auth = "%s:%s" % (user, password)
server = getCon(auth, 'localhost', port)
mserver = getCon(auth, master, port)
poolNode=True
interface = "c0a80100"
role='xen,utility'
hostname=gethostname()
ip = get_ip_address(interface)
poolMembers = []
xserver = server
poolCount = 0

try:
    print server.discover_pool_filesystem()
    print
    print server.discover_server_pool()
    poolDom = parseString(server.discover_server_pool())
    for node in poolDom.getElementsByTagName('Server_Pool'):
        id = node.getElementsByTagName('Unique_Id')[0].firstChild.nodeValue
        alias = node.getElementsByTagName('Pool_Alias')[0].firstChild.nodeValue
        mvip = node.getElementsByTagName('Master_Virtual_Ip')[0].firstChild.nodeValue
        print "pool: %s, %s, %s" % (id, mvip, alias)
        members = node.getElementsByTagName('Member')
        for member in members:
            poolCount = poolCount + 1
            mip = member.getElementsByTagName('Registered_IP')[0].firstChild.nodeValue
            print "member: %s" % (mip)
            if mip == ip:
               pooled = True
            else:
               poolMembers.append(mip)

    # print server.discover_server()

except Error, v:
    print "ERROR", v
