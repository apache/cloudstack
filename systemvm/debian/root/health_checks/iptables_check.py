#!/usr/bin/python3
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
from .utility import getHealthChecksData, formatPort


def main():
    portForwards = getHealthChecksData("portForwarding")
    if portForwards is None or len(portForwards) == 0:
        print("No port forwarding rules provided to check, skipping")
        exit(0)

    failedCheck = False
    failureMessage = "Missing port forwarding rules in Iptables-\n "
    for portForward in portForwards:
        entriesExpected = []
        destIp = portForward["destIp"]
        srcIpText = "-d " + portForward["sourceIp"]
        srcPortText = "--dport " + formatPort(portForward["sourcePortStart"], portForward["sourcePortEnd"], ":")
        dstText = destIp + ":" + formatPort(portForward["destPortStart"], portForward["destPortEnd"], "-")
        for algo in [["PREROUTING", "--to-destination"],
                     ["OUTPUT", "--to-destination"]]:
            entriesExpected.append([algo[0], srcIpText, srcPortText, algo[1] + " " + dstText])

        fetchIpTableEntriesCmd = "iptables-save | grep " + destIp
        pout = Popen(fetchIpTableEntriesCmd, shell=True, stdout=PIPE)
        if pout.wait() != 0:
            failedCheck = True
            failureMessage = failureMessage + "Unable to execute iptables-save command " \
                                              "for fetching rules by " + fetchIpTableEntriesCmd + "\n"
            continue

        ipTablesMatchingEntries = pout.communicate()[0].strip().split('\n')
        for pfEntryListExpected in entriesExpected:
            foundPfEntryList = False
            for ipTableEntry in ipTablesMatchingEntries:
                # Check if all expected parts of pfEntryList
                # is present in this ipTableEntry
                foundAll = True
                for expectedEntry in pfEntryListExpected:
                    if ipTableEntry.find(expectedEntry) == -1:
                        foundAll = False
                        break

                if foundAll:
                    foundPfEntryList = True
                    break

            if not foundPfEntryList:
                failedCheck = True
                failureMessage = failureMessage + str(pfEntryListExpected) + "\n"

    if failedCheck:
        print(failureMessage)
        exit(1)
    else:
        print("Found all entries (count " + str(len(portForwards)) + ") in iptables")
        exit(0)


if __name__ == "__main__":
    if len(sys.argv) == 2 and sys.argv[1] == "advanced":
        main()
