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

import sys
from merge import loadQueueFile
import logging
import subprocess
from subprocess import PIPE, STDOUT
import os
import os.path
import configure

logging.basicConfig(filename='/var/log/cloud.log', level=logging.DEBUG, format='%(asctime)s %(message)s')

# first commandline argument should be the file to process
if (len(sys.argv) != 2):
    print "Invalid usage"
    sys.exit(1)

# FIXME we should get this location from a configuration class
filePath = "/var/cache/cloud/%s" % sys.argv[1]
if not (os.path.isfile(filePath) and os.access(filePath, os.R_OK)):
    print "You are telling me to process %s, but i can't access it" % filePath
    sys.exit(1)

# If the command line json file is unprocessed process it
# This is important or, the control interfaces will get deleted!
if os.path.isfile("filePath/%s" % "cmd_line.json"):
    qf = loadQueueFile()
    qf.setFile("cmd_line.json")
    qf.load(None)

qf = loadQueueFile()
qf.setFile(sys.argv[1])
qf.load(None)

# Converge
returncode = configure.main([])

sys.exit(returncode)
