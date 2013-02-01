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

# This is for test purpose, to test xcp plugin

import sys
import XenAPI
import os.path
import traceback
import socket
def getHost():
    hostname = socket.gethostname()
    url = "http://localhost"
    session = XenAPI.Session(url)
    session.xenapi.login_with_password("root", "password")
    host = session.xenapi.host
    hosts = session.xenapi.host.get_by_name_label(hostname)
    if len(hosts) != 1:
        print "can't find host:" + hostname
        sys.exit(1)
    localhost = hosts[0]
    return [host, localhost]

def callPlugin(pluginName, func, params):
    hostPair = getHost()
    host = hostPair[0]
    localhost = hostPair[1]
    return host.call_plugin(localhost, pluginName, func, params)

def main():
    if len(sys.argv) < 3:
        print "args: pluginName funcName params"
        sys.exit(1)

    pluginName = sys.argv[1]
    funcName = sys.argv[2]

    paramList = sys.argv[3:]
    if (len(paramList) % 2) != 0:
        print "params must be name/value pair"
        sys.exit(2)
    params = {}
    pos = 0;
    for i in range(len(paramList) / 2):
        params[str(paramList[pos])] = str(paramList[pos+1])
        pos = pos + 2
    print "call: " + pluginName + " " + funcName + ", with params: " + str(params)
    print "return: " +  callPlugin(pluginName, funcName, params)

if __name__ == "__main__":
    main()
