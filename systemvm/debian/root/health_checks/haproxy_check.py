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
from utility import getHealthChecksData, formatPort

SSL_CERTS_DIR = "/etc/ssl/cloudstack/"

def checkGlobal(haproxyData, haCfgSections):
    if "global.maxconn" in haproxyData and "maxconn" in haCfgSections["global"]:
        if haproxyData["global.maxconn"] != haCfgSections["global"]["maxconn"][0].strip():
            print "global maxconn mismatch occured"
            return False
    if "global.maxpipes" in haproxyData and "maxpipes" in haCfgSections["global"]:
        if haproxyData["global.maxpipes"] != haCfgSections["global"]["maxpipes"][0].strip():
            print "global maxpipes mismatch occured"
            return False

    return True

def checkDefaults(haproxyData, haCfgSections):
    if "timeout" in haCfgSections["defaults"]:
        timeouts = haCfgSections["defaults"]["timeout"]
        if "default.timeout.connect" in haproxyData:
            timeout = "connect    %s" % haproxyData["default.timeout.connect"]
            if timeout not in timeouts:
                 print "default timeout connect mismatch occured"
                 return False
        if "default.timeout.server" in haproxyData:
            timeout = "server     %s" % haproxyData["default.timeout.server"]
            if timeout not in timeouts:
                 print "default timeout server mismatch occured"
                 return False
        if "default.timeout.client" in haproxyData:
            timeout = "client     %s" % haproxyData["default.timeout.client"]
            if timeout not in timeouts:
                 print "default timeout client mismatch occured"
                 return False

    return True

def checkServerValues(haproxyData, serverSections):
    serverArray = serverSections[0].split(" ")

    if "maxconn" in serverArray and "server.maxconn" in haproxyData:
        maxconnServer = serverArray[serverArray.index("maxconn") + 1]
        if maxconnServer != haproxyData["server.maxconn"]:
            return False

    if "minconn" in serverArray and "server.minconn" in haproxyData:
        minconnServer = serverArray[serverArray.index("minconn") + 1]
        if minconnServer != haproxyData["server.minconn"]:
            return False

    if "maxqueue" in serverArray and "server.maxqueue" in haproxyData:
        maxqueueServer = serverArray[serverArray.index("maxqueue") + 1]
        if maxqueueServer != haproxyData["server.maxqueue"]:
            return False

    return True

def checkLoadBalance(haproxyData, haCfgSections):
    correct = True
    for lbSec in haproxyData:
        if "global.maxconn" in lbSec:   # Ignore first part (global and default settings)
            continue
        srcServer = lbSec["sourceIp"].replace('.', '_') + "-" + \
                    formatPort(lbSec["sourcePortStart"],
                               lbSec["sourcePortEnd"])
        secName = "listen " + srcServer
        secFrontend = "frontend " + srcServer
        secBackend = "backend " + srcServer + "-backend"
        cfgSection = None

        if "transparent" in lbSec and lbSec["transparent"].lower() == 'true':
            if secFrontend not in haCfgSections:
                print "Missing section for load balancing " + secFrontend + "\n"
                correct = False
            elif secBackend not in haCfgSections:
                print "Missing section for load balancing " + secBackend + "\n"
                correct = False
            else:
                cfgSection = haCfgSections[secFrontend]
                cfgSection.update(haCfgSections[secBackend])
        elif secName not in haCfgSections:
            print "Missing section for load balancing " + secName + "\n"
            correct = False
        else:
            cfgSection = haCfgSections[secName]

        if cfgSection:
            if "server" in cfgSection:
                correct = checkServerValues(haproxyData, cfgSection["server"])
                if lbSec["algorithm"] != cfgSection["balance"][0]:
                    print "Incorrect balance method for " + secName + \
                          "Expected : " + lbSec["algorithm"] + \
                          " but found " + cfgSection["balance"][0] + "\n"
                    correct = False

                bindStr = lbSec["sourceIp"] + ":" + formatPort(lbSec["sourcePortStart"], lbSec["sourcePortEnd"])
                if lbSec.has_key("sslcert"):
                    bindStr += " ssl crt " + SSL_CERTS_DIR + lbSec["sslcert"]
                if cfgSection["bind"][0] != bindStr:
                    print "Incorrect bind string found. Expected " + bindStr + " but found " + cfgSection["bind"][0] + "."
                    correct = False

                if (lbSec["sourcePortStart"] == "80" and lbSec["sourcePortEnd"] == "80" and lbSec["keepAliveEnabled"] == "false") \
                        or (lbSec["stickiness"].find("AppCookie") != -1 or lbSec["stickiness"].find("LbCookie") != -1):
                    if not ("mode" in cfgSection and cfgSection["mode"][0] == "http"):
                        print "Expected HTTP mode but not found"
                        correct = False

                expectedServerIps = lbSec["vmIps"].split(" ")
                for expectedServerIp in expectedServerIps:
                    pattern = expectedServerIp + ":" + \
                              formatPort(lbSec["destPortStart"],
                                         lbSec["destPortEnd"])
                    foundPattern = False
                    for server in cfgSection["server"]:
                        s = server.split()
                        if s[0].strip().find(srcServer + "_") == 0 and s[1].strip() == pattern:
                            foundPattern = True
                            break

                    if not foundPattern:
                        correct = False
                        print "Missing load balancing for " + pattern + ". "

    return correct


def main():
    '''
    Checks for max con and each load balancing rule - source ip, ports and destination
    ips and ports. Also checks for http mode. Does not check for stickiness policies.
    '''
    haproxyData = getHealthChecksData("haproxyData")
    if haproxyData is None or len(haproxyData) == 0:
        print "No data provided to check, skipping"
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

    checkGlobalResult = checkGlobal(haproxyData[0], haCfgSections)
    checkDefaultsResult = checkDefaults(haproxyData[0], haCfgSections)
    checkLbRules = checkLoadBalance(haproxyData, haCfgSections)

    if checkGlobalResult and checkDefaultsResult and checkLbRules:
        print "All checks pass"
        exit(0)
    else:
        exit(1)


if __name__ == "__main__":
    if len(sys.argv) == 2 and sys.argv[1] == "advanced":
        main()
