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
from .utility import getHealthChecksData


def main():
    gws = getHealthChecksData("gateways")
    if gws is None and len(gws) == 0:
        print("No gateways data available, skipping")
        exit(0)

    unreachableGateWays = []
    gwsList = gws[0]["gatewaysIps"].strip().split(' ')
    for gw in gwsList:
        if len(gw) == 0:
            continue
        reachableGw = False
        for i in range(5):
            pingCmd = "ping " + gw + " -c 5 -w 10"
            pout = Popen(pingCmd, shell=True, stdout=PIPE)
            if pout.wait() == 0:
                reachableGw = True
                break

        if not reachableGw:
            unreachableGateWays.append(gw)

    if len(unreachableGateWays) == 0:
        print("All " + str(len(gws)) + " gateways are reachable via ping")
        exit(0)
    else:
        print("Unreachable gateways found-")
        print(unreachableGateWays)
        exit(1)


if __name__ == "__main__":
    if len(sys.argv) == 2 and sys.argv[1] == "basic":
        main()
