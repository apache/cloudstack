#!/usr/bin/python

import sys
from merge import loadQueueFile
import logging
import subprocess
from subprocess import PIPE, STDOUT

logging.basicConfig(filename='/var/log/cloud.log',level=logging.DEBUG, format='%(asctime)s %(message)s')

# first commandline argument should be the file to process
if ( len(sys.argv) != 2 ):
    print "Invalid usage"
    sys.exit(1)

qf = loadQueueFile()
qf.setFile(sys.argv[1])
qf.load(None)

qf = loadQueueFile()
qf.setFile("cmd_line.json")
qf.load(None)

# Converge
run = subprocess.Popen(["/opt/cloud/bin/configure.py"],
                           stdout=PIPE, stderr=STDOUT)
stdout, stderr = run.communicate()

if run.returncode:
    print stdout
else:
    print "Convergence is achieved - you have been assimilated!"

sys.exit(run.returncode)
