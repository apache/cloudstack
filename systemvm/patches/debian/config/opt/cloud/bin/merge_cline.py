#!/usr/bin/python

import sys
from merge import loadQueueFile
import logging

logging.basicConfig(filename='/var/log/cloud.log',level=logging.DEBUG, format='%(asctime)s %(message)s')

# first commandline argument should be the file to process
if ( len(sys.argv) != 2 ):
    print "Invalid usage"
    sys.exit(1)

qf = loadQueueFile()
qf.setType("cl")
qf.setFile("cmdline.json")
qf.setPath("/var/chef/data_bags/vr")
qf.load()
