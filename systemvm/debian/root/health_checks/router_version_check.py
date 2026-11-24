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

import re
import subprocess
from os import sys, path, statvfs
from utility import getHealthChecksData

FRR_RELOAD_PATH = "/usr/lib/frr/frr-reload.py"
BFDD_PATH = "/usr/lib/frr/bfdd"


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


def frr_version():
    """Returns the installed FRR package version, or None when it cannot tell."""
    try:
        out = subprocess.check_output(["dpkg-query", "-W", "-f=${Version}", "frr"], stderr=subprocess.DEVNULL)
    except (OSError, subprocess.CalledProcessError):
        return None
    version = out.decode().strip()
    return version or None


def capability_report():
    """One line naming the BGP tools present: frrreload=yes|no bfdd=yes|no frrmajor=<n>|none."""
    version = frr_version()
    match = re.match(r'^(\d+)\.', version) if version else None
    frr_reload = "yes" if path.isfile(FRR_RELOAD_PATH) else "no"
    bfdd = "yes" if path.isfile(BFDD_PATH) else "no"
    major = match.group(1) if match else "none"
    return "frrreload=%s bfdd=%s frrmajor=%s" % (frr_reload, bfdd, major)


def main():
    # The report is information only and never changes the exit code.
    print(capability_report())

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
            print("Release version not yet setup at " + releaseFile +
                  ", skipping.")
        elif expected != found:
            print("Template Version mismatch. Expected: " +
                  expected + ", found: " + found)
            templateVersionMatches = False

    if "scriptsVersion" in data:
        expected = data["scriptsVersion"].strip()
        sigFile = "/var/cache/cloud/cloud-scripts-signature"
        found = getFirstLine(sigFile)
        if found is None:
            print("Scripts signature is not yet setup at " + sigFile +
                  ", skipping")
        if expected != found:
            print("Scripts Version mismatch. Expected: " +
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
