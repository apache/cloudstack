#!/usr/bin/python

import sys
from merge import loadQueueFile
import logging

logging.basicConfig(filename='/var/log/cloud.log',level=logging.DEBUG, format='%(asctime)s %(message)s')

# first commandline argument should be the file to process
if ( len(sys.argv) != 2 ):
    print "Invalid usage"
    sys.exit(1)

# ip files 
if(sys.argv[1].startswith('ip')):
    qf = loadQueueFile()
    qf.setType("ips")
    qf.setFile(sys.argv[1])
    qf.load()
