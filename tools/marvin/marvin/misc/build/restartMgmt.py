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

from configparser import ConfigParser
from optparse import OptionParser
import marvin
from marvin import configGenerator
from marvin import sshClient
from time import sleep as delay
import telnetlib
import socket

if __name__ == '__main__':
    parser = OptionParser()
    parser.add_option("-c", "--config", action="store", default="xen.cfg",
                      dest="config", help="the path where the server configurations is stored")
    (options, args) = parser.parse_args()
    
    if options.config is None:
        raise

    cscfg = configGenerator.getSetupConfig(options.config)
    mgmt_server = cscfg.mgtSvr[0].mgtSvrIp
    ssh = sshClient.SshClient(mgmt_server, 22, "root", "password")
    ssh.execute("service cloudstack-management restart")

    #Telnet wait until api port is open
    tn = None
    timeout = 120
    while timeout > 0:
        try:
            tn = telnetlib.Telnet(mgmt_server, 8096, timeout=120)
            break
        except Exception:
            delay(1)
            timeout = timeout - 1
    if tn is None:
        raise socket.error("Unable to reach API port")

