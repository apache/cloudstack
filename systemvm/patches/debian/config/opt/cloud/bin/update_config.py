#!/usr/bin/python

import sys
from merge import loadQueueFile
import logging
import subprocess
from subprocess import PIPE, STDOUT
import os
import os.path

logging.basicConfig(filename='/var/log/cloud.log',level=logging.DEBUG, format='%(asctime)s %(message)s')

# first commandline argument should be the file to process
if ( len(sys.argv) != 2 ):
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
run = subprocess.Popen(["/opt/cloud/bin/configure.py"],
                       stdin=PIPE, stdout=PIPE, stderr=PIPE)
run.wait()

if run.returncode:
    print "stdout >>"
    print run.stdout.read()
    print "stderr >>"
    print run.stderr.read()
else:
    print "Convergence is achieved - you have been assimilated!"

sys.exit(run.returncode)
