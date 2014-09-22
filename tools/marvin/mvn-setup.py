#!/usr/bin/env python
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

# wrapper around setup.py which injects the version number provided as a
# command line argument called from maven (see pom.xml)

import sys
import re
import subprocess

import os.path

basedir = os.path.dirname(__file__)
setupScript = os.path.join(basedir, 'setup.py')


def replaceVersion(fname, version):
    """replace VERSION in setup.py"""
    with open(fname, 'r') as f:
        content = f.read()
    needle = '\nVERSION\s*=\s*[\'"][^\'"]*[\'"]'
    replacement = '\nVERSION = "%s"' % version
    content = re.sub(needle, replacement, content, 1)
    with open(fname, 'w') as f:
        f.write(content)


def runSetupScript(args):
    """Invoke setup.py with the provided arguments"""
    cmd = ['python', setupScript] + args
    exitCode = subprocess.call(cmd)
    return exitCode


if __name__ == "__main__":
    version = sys.argv[1]
    remainingArgs = sys.argv[2:]
    replaceVersion(setupScript, version)
    runSetupScript(remainingArgs)
