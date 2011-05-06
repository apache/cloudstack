from cloudException import CloudRuntimeException, formatExceptionInfo
import logging
from subprocess import PIPE, Popen
from signal import alarm, signal, SIGALRM, SIGKILL
import sys
import os
class bash:
    def __init__(self, args, timeout=600):
        self.args = args
        logging.debug("execute:%s"%args)
        self.timeout = timeout
        self.process = None
        self.success = False
        self.run()

    def run(self):
        class Alarm(Exception):
            pass
        def alarm_handler(signum, frame):
            raise Alarm

        try:
            self.process = Popen(self.args, shell=True, stdout=PIPE, stderr=PIPE)
            if self.timeout != -1:
                signal(SIGALRM, alarm_handler)
                alarm(self.timeout)

            try:
                self.stdout, self.stderr = self.process.communicate()
                if self.timeout != -1:
                    alarm(0)
            except Alarm:
                os.kill(self.process.pid, SIGKILL)
                raise  CloudRuntimeException("Timeout during command execution")

            self.success = self.process.returncode == 0
        except:
            raise  CloudRuntimeException(formatExceptionInfo())

        if not self.success: 
            logging.debug("Failed to execute:" + self.getErrMsg())

    def isSuccess(self):
        return self.success
    
    def getStdout(self):
        return self.stdout.strip("\n")
    
    def getLines(self):
        return self.stdout.split("\n")

    def getStderr(self):
        return self.stderr.strip("\n")
    
    def getErrMsg(self):
        if self.isSuccess():
            return ""
        
        if self.getStderr() is None or self.getStderr() == "":
            return self.getStdout()
        else:
            return self.getStderr()

def initLoging(logFile=None):
    try:
        if logFile is None:
            logging.basicConfig(level=logging.DEBUG) 
        else: 
            logging.basicConfig(filename=logFile, level=logging.DEBUG) 
    except:
        logging.basicConfig(level=logging.DEBUG) 

def writeProgressBar(msg, result):
    if msg is not None:
        output = "%-30s"%msg
    elif result is True:
        output = "[%-2s]\n"%"OK"
    elif result is False:
        output = "[%-6s]\n"%"Failed"
    sys.stdout.write(output)
    sys.stdout.flush()
 
class DistributionDetector:
    def __init__(self):
        self.distro = "Unknown"

        if os.path.exists("/etc/fedora-release"):
            self.distro = "Fedora"
        elif os.path.exists("/etc/centos-release"):
            self.distro = "CentOS"
        elif os.path.exists("/etc/redhat-release"):
            version = file("/etc/redhat-release").readline()
            if version.find("Red Hat Enterprise Linux Server release 6") != -1:
                self.distro = "RHEL6"
            elif version.find("CentOS release") != -1:
                self.distro = "CentOS"
            else:
                self.distro = "RHEL5"
        elif os.path.exists("/etc/legal") and "Ubuntu" in file("/etc/legal").read(-1):
            self.distro = "Ubuntu"
        else: 
            self.distro = "Unknown" 

    def getVersion(self):
        return self.distro 
 
class serviceOps:
    pass
class serviceOpsRedhat(serviceOps):
    def isServiceRunning(self, servicename):
        try:
            o = bash("service " + servicename + " status")
            if "start/running" in o.getStdout():
                return True
            else:
                return False
        except:
            return False

    def stopService(self, servicename,force=False):
        if self.isServiceRunning(servicename) or force:
            return bash("service " + servicename +" stop").isSuccess()
        
        return True
    def disableService(self, servicename):
        result = self.stopService(servicename)
        bash("chkconfig --del " + servicename)
        return result

    def startService(self, servicename,force=False):
        if not self.isServiceRunning(servicename) or force:
            return bash("service " + servicename + " start").isSuccess()
        return True

    def enableService(self, servicename,forcestart=False):
        bash("chkconfig --level 2345 " + servicename + " on")
        return self.startService(servicename,force=forcestart)
        
    def isKVMEnabled(self):
        if os.path.exists("/dev/kvm"):
            return True
        else:
            return False
        
class serviceOpsUbuntu(serviceOps):
    def isServiceRunning(self, servicename):
        try:
            o = bash("service " + servicename + " status")
            if "start/running" in o.getStdout():
                return True
            else:
                return False
        except:
            return False

    def stopService(self, servicename,force=False):
        if self.isServiceRunning(servicename) or force:
            return bash("service " + servicename +" stop").isSuccess()

    def disableService(self, servicename):
        result = self.stopService(servicename)
        bash("update-rc.d -f " + servicename + " remove")
        return result
    
    def startService(self, servicename,force=False):
        if not self.isServiceRunning(servicename) or force:
            return bash("service " + servicename + " start").isSuccess()

    def enableService(self, servicename,forcestart=False):
        bash("update-rc.d -f " + servicename + " remove")
        bash("update-rc.d -f " + servicename + " start 2 3 4 5 .")
        return self.startService(servicename,force=forcestart)

    def isKVMEnabled(self):
        return bash("kvm-ok").isSuccess() 