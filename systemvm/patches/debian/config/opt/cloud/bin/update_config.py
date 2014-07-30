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
qf.load()

# Converge
chefrun = subprocess.Popen(["/usr/bin/chef-solo",
                            "-j", "/etc/chef/node.json",
                            "-l","fatal"],
                           stdout=PIPE, stderr=PIPE)
result = chefrun.wait()

if (result != 0):
    print result.stderr
else:
    print "chef update completed"

sys.exit(result)
