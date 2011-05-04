#!/usr/bin/python
from subprocess import PIPE, Popen
from signal import alarm, signal, SIGALRM, SIGKILL
import tempfile
import shutil
import os
import logging
import sys
import re
import traceback
import socket
import cloudException
import utilities
import configFileOps

from optparse import OptionParser
           
class myCloudConfig(serviceCfgBase):
    def __init__(self, syscfg):
        super(myCloudConfig, self).__init__(syscfg)
        self.serviceName = "myCloud"
    
    def config(self):
        try:
            cfo = configFileOps("/etc/cloud/agent/agent.properties", self)  
            cfo.addEntry("host", self.syscfg.env.mgtSvr)
            cfo.addEntry("zone", self.syscfg.env.zoneToken)
            cfo.addEntry("port", "443")
            cfo.addEntry("private.network.device", self.syscfg.env.nics[0])
            cfo.addEntry("public.network.device", self.syscfg.env.nics[1])
            cfo.addEntry("guest.network.device", self.syscfg.env.nics[2])
            cfo.addEntry("guid", str(self.syscfg.env.uuid))
            cfo.addEntry("mount.path", "/mnt")
            cfo.addEntry("resource", "com.cloud.storage.resource.LocalSecondaryStorageResource|com.cloud.agent.resource.computing.CloudZonesComputingResource")
            cfo.save()
            
            self.syscfg.stopService("cloud-agent")
            self.syscfg.startService("cloud-agent")
            return True
        except:
            raise CloudRuntimeException("Failed to configure myCloud, please see the /var/log/cloud/setupAgent.log for detail", formatExceptionInfo()) 
    
    def restore(self):
        return True

#it covers RHEL6/Fedora13/Fedora14
class sysConfigRedhat6(sysConfigRedhatBase):
    def __init__(self, glbEnv):
        super(sysConfigRedhat6, self).__init__(glbEnv)
        self.services = [cgroupConfig(self),
                         securityPolicyConfigRedhat(self),
                         networkConfigRedhat(self),
                         libvirtConfigRedhat(self),
                         firewallConfigRedhat(self),
                         myCloudConfig(self)]

#It covers RHEL5/CentOS5, the mainly difference is that there is no cgroup
class sysConfigRedhat5(sysConfigRedhatBase):
    def __init__(self, glbEnv):
        super(sysConfigRedhat5, self).__init__(glbEnv)
        self.services = [
                         securityPolicyConfigRedhat(self),
                         networkConfigRedhat(self),
                         libvirtConfigRedhat(self),
                         firewallConfigRedhat(self),
                         myCloudConfig(self)]
    
def getUserInputs():
    print "Welcome to myCloud Setup:"

    cfo = configFileOps("/etc/cloud/agent/agent.properties")
    oldMgt = cfo.getEntry("host")

    mgtSvr = raw_input("Please input the Management Server Name/IP:[%s]"%oldMgt)
    if mgtSvr == "":
        mgtSvr = oldMgt
    try:
        socket.getaddrinfo(mgtSvr, 443)
    except:
        print "Failed to resolve %s. Please input correct server name or IP."%mgtSvr
        exit(1)

    oldToken = cfo.getEntry("zone")
    zoneToken = raw_input("Please input the Zone Token:[%s]"%oldToken)
    
    if zoneToken == "":
        zoneToken = oldToken

    try:
        defaultNic = networkConfig.getDefaultNetwork()
    except:
        print "Failed to get default route. Please configure your network to have a default route"
        exit(1)
        
    defNic = defaultNic.name
    network = raw_input("Please choose which network used to create VM:[%s]"%defNic)
    if network == "":
        if defNic == "":
            print "You need to specifiy one of Nic or bridge on your system"
            exit(1)
        elif network == "":
            network = defNic

    return [mgtSvr, zoneToken, network]

class globalEnv:
    pass

if __name__ == '__main__':
    parser = OptionParser()
    parser.add_option("-d", action="store_true", dest="debug")
    (options, args) = parser.parse_args()

    initLoging("/var/log/cloud/setupAgent.log")
    userInputs = getUserInputs()
    glbEnv = globalEnv()
    
    if options.debug:
        glbEnv.debug = True
    else:
        glbEnv.debug = False
    glbEnv.mgtSvr = userInputs[0]
    glbEnv.zoneToken = userInputs[1]
    glbEnv.defaultNic = userInputs[2]
    glbEnv.nics = []
    #generate UUID
    glbEnv.uuid = configFileOps("/etc/cloud/agent/agent.properties").getEntry("guid")
    if glbEnv.uuid == "":
            glbEnv.uuid = bash("uuidgen").getStdout()
        
    print "Starting to configure your system:"
    syscfg = sysConfig.getSysConfigFactory(glbEnv)
    try:
        syscfg.config()
        print "myCloud setup is Done!"
    except CloudRuntimeException, e:
        print e
        print "Try to restore your system:"
        try:
            syscfg.restore()
        except:
            pass
