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
from utility import getHealthChecksData


def main():
    vMs = getHealthChecksData("virtualMachines")
    if vMs is not None and len(vMs) > 0:
        with open('/etc/dhcphosts.txt', 'r') as hostsFile:
            allHosts = hostsFile.readlines()
            hostsFile.close()
        for vM in vMs:
            entry = vM["macAddress"] + "," + vM["ip"] + "," + vM["vmName"]
            foundEntry = False
            for host in allHosts:
                host = host.strip()
                if host.find(vM["macAddress"]) != -1 and host.find(vM["ip"]) != -1 and host.find(vM["vmName"]) != -1:
                    foundEntry = True
                    break

            if not foundEntry:
                print "Missing elements in dhcphosts.txt - " + entry
                exit(1)

        print "All " + str(len(vMs)) + " VMs are present in dhcphosts.txt"
    else:
        print "No VMs running data available, skipping"
    exit(0)


if __name__ == "__main__":
    if len(sys.argv) == 2 and sys.argv[1] == "advance":
        main()
