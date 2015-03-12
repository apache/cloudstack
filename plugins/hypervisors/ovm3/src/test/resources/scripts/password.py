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
import os, sys, subprocess, socket, fcntl, struct
from socket import gethostname
from xml.dom.minidom import parseString

from xmlrpclib import ServerProxy, Error

def spCon(proto, host, port):
    print "trying %s on %s:%s" % (proto, host, port)
    try:
        x = ServerProxy("%s://%s:%s" % (proto, host, port))
        x.echo(proto)
        return x
    except Error, v:
        print "ERROR", v
        return

def getCon(host, port):
    try:
        server = spCon("http", host, port)
    except Error, v:
        print "ERROR", v
        server = spCon("https", host, port)

    return server

# hmm master actions don't apply to a slave
port = 8899
user = "oracle"
password = "test123"
auth = "%s:%s" % (user, password)
host = "localhost"

print "setting up password"
try:
    con = getCon(host, port)
    print con.update_agent_password(user, password)
except Error, v:
    print "ERROR", v
