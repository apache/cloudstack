#!/usr/bin/python

import sys
from merge import loadQueueFile
import logging
import subprocess
from subprocess import PIPE

logging.basicConfig(filename='/var/log/cloud.log',level=logging.DEBUG, format='%(asctime)s %(message)s')

# first commandline argument should be the file to process
if ( len(sys.argv) != 2 ):
    print "Invalid usage"
    sys.exit(1)

qf = loadQueueFile()
qf.setFile(sys.argv[1])
qf.load(None)

# Converge
run = subprocess.Popen(["/opt/cloud/bin/configure.py"],
                           stdout=PIPE, stderr=PIPE)
result = run.wait()

if (result != 0):
    print run.stderr
else:
    print "Convergence is achieved - you have been assimilated!"

sys.exit(result)
