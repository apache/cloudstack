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
from .utility import getHealthChecksData


def getFirstLine(file=None):
    if file is not None and path.isfile(file):
        ret = None
        with open(file, 'r') as oFile:
            lines = oFile.readlines()
            if len(lines) > 0:
                ret = lines[0].strip()
            oFile.close()

        return ret
    else:
        return None


def main():
    entries = getHealthChecksData("routerVersion")
    data = {}
    if entries is not None and len(entries) == 1:
        data = entries[0]

    if len(data) == 0:
        print("Missing routerVersion in health_checks_data, skipping")

        exit(0)

    templateVersionMatches = True
    scriptVersionMatches = True

    if "templateVersion" in data:
        expected = data["templateVersion"].strip()
        releaseFile = "/etc/cloudstack-release"
        found = getFirstLine(releaseFile)
        if found is None:
            print("Release version not yet setup at " + releaseFile +\
                  ", skipping.")
        elif expected != found:
            print("Template Version mismatch. Expected: " + \
                  expected + ", found: " + found)
            templateVersionMatches = False

    if "scriptsVersion" in data:
        expected = data["scriptsVersion"].strip()
        sigFile = "/var/cache/cloud/cloud-scripts-signature"
        found = getFirstLine(sigFile)
        if found is None:
            print("Scripts signature is not yet setup at " + sigFile +\
                  ", skipping")
        if expected != found:
            print("Scripts Version mismatch. Expected: " + \
                  expected + ", found: " + found)
            scriptVersionMatches = False

    if templateVersionMatches and scriptVersionMatches:
        print("Template and scripts version match successful")
        exit(0)
    else:
        exit(1)


if __name__ == "__main__":
    if len(sys.argv) == 2 and sys.argv[1] == "basic":
        main()
