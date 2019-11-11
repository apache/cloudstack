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

from os import sys, path

sys.path.append('/opt/cloud/bin')
from healthchecksutility import getHealthChecksData

def main():
    vMs = getHealthChecksData("virtualMachines")
    if vMs != None and len(vMs) > 0:
        with open('/etc/hosts', 'r') as hostsFile:
            allHosts = hostsFile.readlines()
            hostsFile.close()
        for vM in vMs:
            foundEntry = False
            for host in allHosts:
                if host.find(vM["ip"]) != -1 and host.find(vM["vmName"]) != -1:
                    foundEntry = True
                    break

            if foundEntry == False:
                print "Missing entry in /etc/hosts - " + vM["ip"] + " " + vM["vmName"]
                exit(1)

        print "All " + str(len(vMs)) + " VMs are present in /etc/hosts"
    else:
        print "No VMs running data available"
    exit(0)

if __name__ == "__main__":
    if len(sys.argv) == 2 and sys.argv[1] == "advance":
        main()