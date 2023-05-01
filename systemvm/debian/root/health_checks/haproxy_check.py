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

def checkMaxconn(haproxyData, haCfgSections):
    if "maxconn" in haproxyData and "maxconn" in haCfgSections["global"]:
        if haproxyData["maxconn"] != haCfgSections["global"]["maxconn"][0].strip():
            print "global maxconn mismatch occurred"
            return False

    return True

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


def checkFrontendLbValues(lbSection, cfgSection):
    correct = True
    if "lb.timeout.client" in lbSection:
        if "timeout" not in cfgSection:
            print("timeout is enabled but not configured in haproxy rule %s" % cfgSection)
            correct = False
        else:
            timeout = "client     %s" % lbSection["lb.timeout.client"]
            if timeout not in cfgSection["timeout"]:
                print("timeout client section is not configured in rule %s" % cfgSection)
                correct = False

    return correct


def checkBackendLbValues(lbSection, cfgSection):
    correct = True
    if "lb.maxconn" in lbSection:
        if "maxconn" not in cfgSection:
            print("maxconn value is missing in %s" % cfgSection)
            correct = False
        else:
            if cfgSection["maxconn"][0] != lbSection["lb.maxconn"]:
                print("maxconn value in %s doesnt match with %s" % (lbSection, cfgSection))
                correct = False

    if "lb.fullconn" in lbSection:
        if "fullconn" not in cfgSection:
            print("fullconn value missing in %s" % cfgSection)
            correct = False
        else:
            if cfgSection["fullconn"][0] != lbSection["lb.fullconn"]:
                print("fullconn value in %s doesnt match with %s" % (lbSection, cfgSection))

    if "lb.timeout.connect" in lbSection:
        if "timeout" not in cfgSection:
            print("timeout is enabled but not configured in haproxy rule %s" % cfgSection)
            correct = False
        else:
            timeout = "connect    %s" % lbSection["lb.timeout.connect"]
            if timeout not in cfgSection["timeout"]:
                print("timeout connect section is not configured in rule %s" % cfgSection)
                correct = False

    if "lb.timeout.server" in lbSection:
        if "timeout" not in cfgSection:
            print("timeout is enabled but not configured in haproxy rule %s" % cfgSection)
            correct = False
        else:
            timeout = "server     %s" % lbSection["lb.timeout.server"]
            if timeout not in cfgSection["timeout"]:
                print("timeout server section is not configured in rule %s" % cfgSection)
                correct = False

    if "transparent" not in lbSection:
        correct = checkFrontendLbValues(lbSection, cfgSection)

    return correct


def checkServerValues(haproxyData, serverSections):
    correct = True
    serverArray = serverSections[0].split(" ")

    if "server.maxconn" in haproxyData:
        if "maxconn" not in serverArray:
            print("maxconn value is missing in line %s" % serverSections)
            correct = False
        else:
            maxconnServer = serverArray[serverArray.index("maxconn") + 1]
            if maxconnServer != haproxyData["server.maxconn"]:
                correct = False

    if "server.minconn" in haproxyData:
        if "minconn" not in serverArray:
            print("minconn value is missing in line %s" % serverSections)
            correct = False
        else:
            minconnServer = serverArray[serverArray.index("minconn") + 1]
            if minconnServer != haproxyData["server.minconn"]:
                correct = False

    if "server.maxqueue" in haproxyData:
        if "maxqueue" not in serverArray:
            print("maxqueue value is missing in line %s" % serverSections)
            correct = False
        else:
            maxqueueServer = serverArray[serverArray.index("maxqueue") + 1]
            if maxqueueServer != haproxyData["server.maxqueue"]:
                correct = False

    if "lb.backend.https" in haproxyData:
        if "ssl verify none" not in serverSections[0]:
            print("backend https is enabled but not configured in %s" % serverSections[0])
            correct = False

    return correct

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
                if cfgSection is not None:
                    correct = checkFrontendLbValues(lbSec, cfgSection)
                cfgSection.update(haCfgSections[secBackend])
        elif secName not in haCfgSections:
            print "Missing section for load balancing " + secName + "\n"
            correct = False
        else:
            cfgSection = haCfgSections[secName]
            if cfgSection:
                if "server" in cfgSection:
                    correct = correct and checkBackendLbValues(lbSec, cfgSection)
                    correct = correct and checkServerValues(lbSec, cfgSection["server"])
                    if lbSec["algorithm"] != cfgSection["balance"][0]:
                        print "Incorrect balance method for " + secName + \
                              "Expected : " + lbSec["algorithm"] + \
                              " but found " + cfgSection["balance"][0] + "\n"
                        correct = False

                    bindStr = lbSec["sourceIp"] + ":" + formatPort(lbSec["sourcePortStart"], lbSec["sourcePortEnd"])
                    if "sslcert" in lbSec:
                        bindStr += " ssl crt " + SSL_CERTS_DIR + lbSec["sslcert"]
                    if "http2" in lbSec and lbSec["http2"].lower() == 'true':
                        bindStr += " alpn h2,http/1.1"
                    if not cfgSection["bind"][0].startswith(bindStr):
                        print "Incorrect bind string found. Expected " + bindStr + " but found " + cfgSection["bind"][0] + "."
                        correct = False

                    if ("http" in lbSec and lbSec["http"] == 'true') \
                            or "sslcert" in lbSec \
                            or lbSec["stickiness"].find("AppCookie") != -1 \
                            or lbSec["stickiness"].find("LbCookie") != -1:
                        if not ("mode" in cfgSection and cfgSection["mode"][0] == "http"):
                            print "Expected HTTP mode but not found"
                            correct = False
                        if lbSec["keepAliveEnabled"] == "false" \
                                and not ("option" in cfgSection and cfgSection["option"][0] == "httpclose"):
                            print "Expected 'option httpclose' but not found"
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
