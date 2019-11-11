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
from subprocess import *

sys.path.append('/opt/cloud/bin')
from healthchecksutility import getHealthChecksData

def main():
    portForwards = getHealthChecksData("portForwarding")
    if portForwards != None and len(portForwards) > 0:
        entriesExpected = []
        for portForward in portForwards:
            srcIp = portForward["sourceIp"]
            srcPortStart = portForward["sourcePortStart"]
            srcPortEnd = portForward["sourcePortEnd"]
            destIp = portForward["destIp"]
            destPortStart = portForward["destPortStart"]
            destPortEnd = portForward["destPortEnd"]
            entriesExpected.append(["-d " + srcIp, "--dport " + (srcPortStart if (srcPortStart == srcPortEnd) else (srcPortStart + ":" + srcPortEnd)), "--to-destination " + destIp + ":" + (destPortStart if (destPortStart == destPortEnd) else (destPortStart + "-" + destPortEnd))])

        pout = Popen("iptables-save | grep " + destIp, shell=True, stdout=PIPE)
        if pout.wait() != 0:
            print "Unable to execute iptables-save command for fetching rules"
            exit(1)

        ipTablesMatchingEntries = pout.communicate()[0].strip().split('\n')
        for pfEntryListExpected in entriesExpected:
            foundPfEntryList = False
            for ipTableEntry in ipTablesMatchingEntries:
                # Check if all expected parts of pfEntryList is present in this ipTableEntry
                foundAll = True
                for expectedEntry in pfEntryListExpected:
                    if ipTableEntry.find(expectedEntry) == -1:
                        foundAll = False
                        break

                if foundAll:
                    foundPfEntryList = True
                    break

            if foundPfEntryList == False:
                print "Missing entry for port forwarding rules in Iptables - "
                print pfEntryListExpected
                exit(1)

        print "Found all entries (count " + str(len(portForwards)) + ") in iptables"
    else:
        print "No portforwarding rules provided to check"

    exit(0)

if __name__ == "__main__":
    if len(sys.argv) == 2 and sys.argv[1] == "advance":
        main()
