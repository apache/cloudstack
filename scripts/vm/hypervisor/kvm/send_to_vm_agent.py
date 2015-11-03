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

import libvirt_qemu
from libvirt_qemu import libvirt
from optparse import OptionParser
import os
import sys
import logging
import json
import ast
import socket

def _connect(uri, readonly):  
    def _connect_auth_cb(creds, opaque):
        if len(creds) == 0:
            return 0
        return -1
    auth = [[libvirt.VIR_CRED_AUTHNAME,
             libvirt.VIR_CRED_ECHOPROMPT,
             libvirt.VIR_CRED_REALM,
             libvirt.VIR_CRED_PASSPHRASE,
             libvirt.VIR_CRED_NOECHOPROMPT,
             libvirt.VIR_CRED_EXTERNAL],
            _connect_auth_cb,
            None]
    if readonly:
        return libvirt.openReadOnly(uri)
    else:
        return libvirt.openAuth(uri, auth, 0)

def exec_qemuga_command(domain, cmd, timeout=10, flags=0):
    try:
        return libvirt_qemu.qemuAgentCommand(domain, cmd, timeout, flags)
    except libvirt.libvirtError as error:
        logging.warning("Execute qemu-ga command failed, command: %s, domain uuid: %s, exception: %s" % (cmd, domain.UUIDString(), error))
        return None

if __name__ == '__main__':
    parser = OptionParser()
    parser.add_option('-c', '--command', action='store_true', dest='command', default=False, help='true if command, false if data')
    parser.add_option('-n', '--name', dest='name', help='the name of VM on host')
    parser.add_option('-p', '--path', dest='path', help='the path of VM agent file on host')
    parser.add_option('-d', '--data', dest='data', help='the content of command or data')
    parser.add_option('-r', '--readonly', action='store_true', dest='readonly', default=False, help='true if the command is read only, false if not')
    parser.add_option('-s', '--systemvm', action='store_true', dest='systemvm', default=False, help='true if the data is send to systemvm, false if not')
    parser.add_option('-t', '--timeout', dest='timeout', type=int, help='the timeout config of command')
    (options, args) = parser.parse_args()
    if options.command & (options.name != None) & (options.data != None):
        conn = _connect('qemu:///system', options.readonly)
        domain = conn.lookupByName(options.name)
        if (domain != None) & (options.timeout == None):
            response = exec_qemuga_command(domain, options.data)
        elif (domain != None):
            response = exec_qemuga_command(domain, options.data, options.timeout)
        if (response == None):
            sys.exit(1)
        print ast.literal_eval(response)
        sys.exit()
    elif (options.command == False) & (options.path != None) & (options.data != None):
       s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
       s.connect(options.path)
       if (options.systemvm):
           pubkeyfile = "/root/.ssh/id_rsa.pub.cloud"
           file = open("%s" % pubkeyfile, "r")
           key = file.readline()
           file.close()
           message = "pubkey:%s\ncmdline:%s" % (key,options.data.replace("%"," "))
           #logging.warning("Sending: \n%s" % message)
           s.send(message)
       else:
           s.send(options.data)
       s.send("\n")
       s.close()
       sys.exit(0)
    else:
       sys.exit(0)
