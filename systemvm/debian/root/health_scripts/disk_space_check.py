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

from os import sys, path, statvfs

sys.path.append('/opt/cloud/bin')
from healthchecksutility import getHealthChecksData

def main():
    entries = getHealthChecksData("systemThresholds")
    data = {}
    if len(entries) == 1:
        data = entries[0]

    if "minSpaceNeeded" in data:
        minSpaceNeeded = int(data["minSpaceNeeded"]) * 1024
        s = statvfs('/')
        freeSpace = (s.f_bavail * s.f_frsize) / 1024
        if (freeSpace < minSpaceNeeded):
            print "Insufficient free space is " + str(freeSpace/1024) + " MB"
            exit(1)
        else:
            print "Sufficient free space is " + str(freeSpace/1024) + " MB"
            exit(0)
    else:
        print "Missing config in health_checks_data systemThresholds > minSpaceNeeded"
        exit(1)

if __name__ == "__main__":
    if len(sys.argv) == 2 and sys.argv[1] == "basic":
        main()