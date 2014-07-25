#!/usr/bin/python

import syslog
import sys

# first commandline argument should be the file to process
if ( len(sys.argv) != 2 ):
    print "Invalid usage"
    sys.exit(1)

json_file = sys.argv[1]

syslog.syslog(sys.argv[0] + " called for file " + json_file)
