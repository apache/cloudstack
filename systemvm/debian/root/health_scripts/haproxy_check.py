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

def checkMaxconn(haproxyData, haCfgSections):
    if "maxconn" in haproxyData and "maxconn" in haCfgSections["global"]:
        if haproxyData["maxconn"] != haCfgSections["global"]["maxconn"]:
            print "global maxconn mismatch occured"
            return False

    return True

def formatPort(portStart, portEnd, delim = "-"):
    return portStart if portStart == portEnd else portStart + delim + portEnd

def checkLoadBalance(haproxyData, haCfgSections):
    correct = True
    for lbSec in haproxyData:
        srcServer = lbSec["sourceIp"].replace('.', '_') + "-" + formatPort(lbSec["sourcePortStart"], lbSec["sourcePortEnd"])
        secName = "listen " + srcServer

        if secName not in haCfgSections:
            print "Missing section for load balancing " + secName + "\n"
            correct = False
        else:
            cfgSection = haCfgSections[secName]
            if "server" in cfgSection:
                if lbSec["algorithm"] != cfgSection["balance"][0]:
                    print "Incorrect balance method for source " + secName + "Expected : " + lbSec["algorithm"] + " but found " + cfgSection["balance"][0] + "\n"
                    correct = False
                expectedServerIps = lbSec["vmIps"].split(" ")
                for expectedServerIp in expectedServerIps:
                    pattern = expectedServerIp + ":" + formatPort(lbSec["destPortStart"], lbSec["destPortEnd"])
                    foundPattern = False
                    for server in cfgSection["server"]:
                        if server.find(srcServer) != -1 and server.find(pattern) != -1:
                            foundPattern = True
                            break

                    if not foundPattern:
                        correct = False
                        print "Missing load balancing for " + pattern + ". "


    return correct


def main():
    haproxyData = getHealthChecksData("haproxyData")
    if haproxyData == None or len(haproxyData) == 0:
        print "No data provided to check"
        exit(0)

    with open("/etc/haproxy/haproxy.cfg", 'r') as haCfgFile:
        haCfgLines = haCfgFile.readlines()
        haCfgFile.close()

    if len(haCfgLines) == 0:
        print "Unable to read config file /etc/haproxy/haproxy.cfg"
        exit(1)

    haCfgSections = {}
    currSection = None
    currSectionDict = {}
    for line in haCfgLines:
        line = line.strip()
        if len(line) == 0:
            if currSection is not None and len(currSectionDict) > 0:
                haCfgSections[currSection] = currSectionDict

            currSection = None
            currSectionDict = {}
            continue

        if currSection is None:
            currSection = line
        else:
            lineSec = line.split(' ', 1)
            if lineSec[0] not in currSectionDict:
                currSectionDict[lineSec[0]] = []

            currSectionDict[lineSec[0]].append(lineSec[1] if len(lineSec) > 1 else '')

    if checkMaxconn(haproxyData, haCfgSections) and checkLoadBalance(haproxyData, haCfgSections):
        print "All checks pass"
        exit(0)
    else:
        exit(1)


if __name__ == "__main__":
    if len(sys.argv) == 2 and sys.argv[1] == "advance":
        main()
