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

    if vMs is None or len(vMs) == 0:
        print("No VMs running data available, skipping")
        exit(0)

    try:
        with open('/etc/dhcphosts.txt', 'r') as hostsFile:
            allHosts = hostsFile.readlines()
            hostsFile.close()
    except IOError:
        allHosts = []

    failedCheck = False
    failureMessage = "Missing elements in dhcphosts.txt - \n"
    COUNT = 0
    for vM in vMs:
        if vM["dhcp"] == "false":
            continue
        COUNT = COUNT + 1
        entry = vM["macAddress"] + " " + vM["ip"] + " " + vM["vmName"]
        foundEntry = False
        for host in allHosts:
            host = host.strip().split(',')
            if len(host) < 4:
                continue

            if host[0].strip() == vM["macAddress"] and host[1].strip() == vM["ip"]\
                    and host[2].strip() == vM["vmName"]:
                foundEntry = True
                break

            nonDefaultSet = "set:" + vM["ip"].replace(".", "_")
            if host[0].strip() == vM["macAddress"] and host[1].strip() == nonDefaultSet \
                    and host[2].strip() == vM["ip"] and host[3].strip() == vM["vmName"]:
                foundEntry = True
                break

        if not foundEntry:
            failedCheck = True
            failureMessage = failureMessage + entry + ", "

    if failedCheck:
        print(failureMessage[:-2])
        exit(1)
    else:
        print("All " + str(COUNT) + " VMs are present in dhcphosts.txt")
        exit(0)


if __name__ == "__main__":
    if len(sys.argv) == 2 and sys.argv[1] == "advanced":
        main()
