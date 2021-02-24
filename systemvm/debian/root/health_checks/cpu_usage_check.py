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

from os import sys, path, statvfs
from subprocess import *
from .utility import getHealthChecksData


def main():
    entries = getHealthChecksData("systemThresholds")
    data = {}
    if entries is not None and len(entries) == 1:
        data = entries[0]

    if "maxCpuUsage" not in data:
        print("Missing maxCpuUsage in health_checks_data systemThresholds, skipping")
        exit(0)

    maxCpuUsage = float(data["maxCpuUsage"])
    cmd = "top -b -n2 -p 1 | fgrep \"Cpu(s)\" | tail -1 | " \
          "awk -F 'id,' " \
          "'{ split($1, vs, \",\");  idle=vs[length(vs)]; " \
          "sub(\"%\", \"\", idle); printf \"%.2f\", 100 - idle }'"
    pout = Popen(cmd, shell=True, stdout=PIPE)
    if pout.wait() == 0:
        currentUsage = float(pout.communicate()[0].strip())
        if currentUsage > maxCpuUsage:
            print("CPU Usage " + str(currentUsage) +
                  "% has crossed threshold of " + str(maxCpuUsage) + "%")
            exit(1)
        print("CPU Usage within limits with current at "
              + str(currentUsage) + "%")
        exit(0)
    else:
        print("Failed to retrieve cpu usage using " + cmd)
        exit(1)


if __name__ == "__main__":
    if len(sys.argv) == 2 and sys.argv[1] == "basic":
        main()
