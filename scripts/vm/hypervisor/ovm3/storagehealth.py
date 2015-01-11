#!/usr/bin/python
#
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
#
# TODO:
# add multipath -ll
# add iscsiadm output
#
import time
import socket
import getopt
import sys
import subprocess, threading
import logging
import logging.handlers
import re
import os

""" a class to do checks with as a thread so we can have nice timeouts """
class Check(object):
    def __init__(self, cmd="", failcmd="", primary="",
            file="", timeout="120", interval=1, logger=""):
        self.file=file
        self.cmd=cmd
        self.failcmd=failcmd
        self.primary=primary
        self.timeout=timeout
        self.interval=interval
        self.process=None
        self.logger=logger
        self.run(self.timeout)

    def writehb(self,file=""):
        if file:
            epoch=time.time()
            self.logger.debug('Work on file: %s' % (file))
            # print "file: %s %s" % (file,epoch) 
            text_file = open("%s" % file, "w")
            text_file.write("%s" % epoch)
            text_file.close()
            self.logger.info('Worked on file %s for %s' %
                 (file, (time.time() - epoch)))

    """ We only want mounted nfs filesystems """
    def nfsoutput(self):
        command="mount -v -t nfs"
        p=subprocess.Popen(command, shell=True, stdout=subprocess.PIPE)
        lines=map(lambda line: line.split()[2], p.stdout.readlines())
        test=re.compile("^%s" % (primary))
        lines=filter(test.search, lines)
        return lines

    """ 
        The main run for all checks we do,
        everything is in here on purpose.

        the other FSs to heartbeat should be added to filesystems...!
    """
    def run(self, timeout):
        def target():
            filesystems=[]
            filesystems.extend(self.nfsoutput())
            for fs in filesystems:
                if self.file:
                    self.writehb("%s/%s" % (fs,file))

            epoch=time.time()
            if self.cmd:
                self.logger.debug('Executeing: %s' % (cmd))
                self.process = subprocess.Popen(self.cmd, shell=True)
                self.process.communicate()
                self.logger.info('Executed: %s in %s' %
                    (cmd, (time.time() - epoch)))

        thread = threading.Thread(target=target)
        thread.start()
        thread.join(self.timeout)
        if thread.isAlive():
            self.logger.critical('Critical: thread timeout; %s' % (timeout))
            if self.failcmd:
                self.logger.critical('Critical: executing; %s' % (failcmd))
                p=subprocess.Popen(failcmd, shell=True, stdout=subprocess.PIPE)

""" here we figure out what we're running on more or less """
def figureOutPrimary():
    redhat="/etc/redhat-release"
    if os.path.isfile(redhat):
        for line in open(redhat):
            if "XenServer" in line:
                return "/var/run/sr-mount"
            if "Oracle VM server" in line:
                return "/OVS/Repositories/"
    print "Unknown hypervisor, consider adding it, exiting"
    sys.exit(42)
    
""" The logger is here """
def Logger(level=logging.WARNING):
    logger = logging.getLogger('cs-heartbeat')
    logger.setLevel(level)
    handler = logging.handlers.SysLogHandler(address = '/dev/log')
    logger.addHandler(handler)
    return logger

""" main for preso-dent """
if __name__ == '__main__':
    me=os.path.basename(__file__)
    timeout=120
    interval=1
    hostname=socket.gethostname()
    file=".hb-%s" % (hostname)
    cmd=""
    level=logging.DEBUG
    primary=""
    failcmd=("echo 1 > /proc/sys/kernel/sysrq "
        "&& "
        "echo c > /proc/sysrq-trigger")
    
    # Decide based on our identity
    if me == "heartbeat": 
        #  String result = callHostPluginPremium(conn, "heartbeat",
        #  "host", _host.uuid,
        #  "timeout", Integer.toString(_heartbeatTimeout),
        #  "interval", Integer.toString(_heartbeatInterval));
        # if (result == null || !result.contains("> DONE <")) {
        #
        print heartbeat
    else:
        # get options
        try:
            opts, args = getopt.getopt(sys.argv[1:], "p:f:c:t:i:",
                [ 'primary=','failcmd=','cmd=','timeout=','interval'])
        except getopt.GetoptError:
            print """Usage: 
                   --primary|-p: match for primary storage to monitor.
                   --failcmd|-f: executed on timeout.
                   --cmd|-c: command to execute next to hb file(s) on primary.
                   --timeout|-t: excute failcmd after timeout(s) is hit.
                   --interval|-i: run the checks every %ss>""" 
            sys.exit()
    
        for o, a in opts:
            if o in ('-p', '--primary'):
                primary=a
            if o in ('-f', '--failcmd'):
                failcmd=a
            if o in ('-c', '--cmd'):
                cmd=a
            if o in ('-t', '--timeout'):
                timeout=int(a)
            if o in ('-i', '--interval'):
                interval=int(a)

    if primary == "":
        primary=figureOutPrimary()

    logger=Logger(level=level)
    os.chdir("/") 
    # os.setsid() 
    os.umask(0) 
    try:    
        pid = os.fork() 
        if pid > 0:
            # exit first parent
            if me == "heartbeat":
                print "> DONE <" 
            sys.exit(0) 
    except OSError, e: 
        print >>sys.stderr, "fork #1 failed: %d (%s)" % (e.errno, e.strerror) 
        sys.exit(1)

    checker = Check(cmd=cmd,
        failcmd=failcmd,
        file=file,
        timeout=timeout,
        interval=interval,
        logger=Logger(level=level));

    while True:
        start=time.time()
        checker.run(timeout)
        runtime=time.time() - start 
        logger.debug("cmd time: %s" % (runtime))
        if runtime > interval:
            logger.warning('Warning: runtime %s bigger than interval %s' %
                (runtime, interval))
        else:   
            time.sleep(interval)