""" General helper functions 
for use in the configuation process

"""
import subprocess
import logging
import os.path
import re
import shutil

def updatefile(filename, val, mode):
    """ add val to file """
    for line in open(filename):
        if line.strip().lstrip("0") == val:
            return
    # set the value
    handle = open(filename, mode)
    handle.write(val)
    handle.close()

def definedinfile(filename, val):
    """ Check if val is defined in the file """
    for line in open(filename):
        if re.search(val, line):
            return True
    return False

def addifmissing(filename, val):
    """ Add something to a file
    if it is not already there """
    if not definedinfile(filename, val):
         updatefile(filename, val + "\n", "a")
         logging.debug("Added %s to file %s" % (val, filename))

def execute(command):
    """ Execute command """
    p = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
    result = p.communicate()[0]
    return result.splitlines()

def service(name, op):
    execute("service %s %s" % (name, op))
    logging.info("Service %s %s" % (name, op))

def copy_if_needed(src, dest):
    """ Copy a file if the destination does not already exist
    """
    if os.path.isfile(dest):
        return
    try:
        shutil.copy2(src, dest)
    except IOError:
        logging.Error("Could not copy %s to %s" % (src, dest))
    else:
        logging.info("Copied %s to %s" % (src, dest))

