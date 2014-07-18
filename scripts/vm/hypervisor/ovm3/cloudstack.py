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
import logging
import time
import re
import os.path
import paramiko
import subprocess
import socket

from xen.util.xmlrpcclient import ServerProxy
from xmlrpclib import Error
from xen.xend import XendClient
from agent.api.base import Agent
from agent.lib.settings import get_api_version
from xen.xend import sxp

class CloudStack(Agent):
    """
    Cloudstack plugin for OVM3.2.x.
    """

    # exposed services
    def get_services(self, version=None):
        return {
            'call': call,
            'get_vncport': getVncPort,
            'exec_domr': domrExec,
            'check_domr_port': domrCheckPort,
            'check_domr_ssh': domrCheckSsh,
            'ovs_control_interface': ovsControlInterface,
            'ovs_mkdir': ovsMkdir,
            'ovs_check_file': ovsCheckFile,
            'ovs_upload_ssh_key': ovsUploadSshKey,
            'ovs_upload_file': ovsUploadFile,
            'ovs_dom0_stats': ovsDom0Stats,
            'ovs_domU_stats': ovsDomUStats,
            'get_module_version': getModuleVersion,
            'ping': ping,
#            'patch': ovmCsPatch,
#            'ovs_agent_set_ssl': ovsAgentSetSsl,
#            'ovs_agent_set_port': ovsAgentSetPort,
#            'ovs_restart_agent': ovsRestartAgent,
        }

    def getName(self):
        return self.__class__.__name__

# which version are we intended for?
def getModuleVersion():
    return "0.1"

# call test
def call(msg):
    return msg

# execute something on domr
def domrExec(ip, cmd, timeout=10, username="root", port=3922, keyfile="~/.ssh/id_rsa.cloud"):
    ssh = paramiko.SSHClient()
    ssh.load_system_host_keys()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    privatekeyfile = os.path.expanduser(keyfile)
    key = paramiko.RSAKey.from_private_key_file(privatekeyfile)
    ssh.connect(ip, port, username, pkey=key, timeout=timeout)
    ssh_stdin, ssh_stdout, ssh_stderr = ssh.exec_command(cmd)
    exit_status = ssh_stdout.channel.recv_exit_status()
    ssh.close()
    return { "rc": exit_status,
        "out": ''.join(ssh_stdout.readlines()),
        "err": ''.join(ssh_stderr.readlines()) };

# check a port on domr
def domrPort(ip, port=3922, timeout=3):
    return domrCheckPort(ip, port, timeout=timeout)

# check a port on domr
def domrCheckPort(ip, port=3922, timeout=3):
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(timeout)
        s.connect((ip, port))
        s.close()
        return True
    except:
        return False

# check ssh
def domrCheckSsh(ip, port=3922, timeout=10):
    x = domrExec(ip, "", port=port, timeout=timeout)
    if (x.get("rc") == 0):
        return True
    return False

def grep(file, string):
    c=0
    for line in open(file):
        if string in line:
           c=c+1
    return c

def ovmVersion():
    path="/etc/ovs-release"
    return re.findall("[\d\.]+$", open(path).readline())[0]
  
# fix known bugs.... 
def ovmCsPatch(version="3.2.1"):
    path="/etc/xen/scripts"
    netcom="%s/xen-network-common.sh" % path
    netbr="%s/linuxbridge/ovs-vlan-bridge" % path
    func="setup_bridge_port"
    xendConst="/usr/lib64/python2.4/site-packages/xen/xend/XendConstants.py"
    xendRtime="MINIMUM_RESTART_TIME"
    if version == "":
        version=ovmVersion()

    if version == "3.2.1":
        if grep(netcom, "_%s" % func) == 3 and grep(netbr, "_%s" % func) < 1:
            _replaceInFile(netbr, func, "_%s" % func, True)
        
        if grep(xendConst, "%s = %s" % (xendRtime, 60)) == 1:
            _replaceInFile(xendConst, 
                "%s = %s" % (xendRtime, 60),
                "%s = %s" % (xendRtime, 10),
                True)
            ovsRestartXend()

    return True

def _replaceInFile(file, orig, set, full=False):
    replaced=False
    if os.path.isfile(file):
        import fileinput
        for line in fileinput.FileInput(file, inplace=1):
                line=line.rstrip('\n')
                if full == False:
                    if re.search("%s=" % orig, line):
                        line="%s=%s" % (orig, set)
                        replaced=True
                else:
                    if re.search(orig, line):
                        line=line.replace(orig, set)
                        replaced=True
                print line
    return replaced

def _ovsIni(setting, change):
    ini="/etc/ovs-agent/agent.ini"
    return _replaceInFile(ini, setting, change)

# enable/disable ssl for the agent
def ovsAgentSetSsl(state):
    ena="disable"
    if state and state != "disable" and state.lower() != "false":
        ena="enable"
    return _ovsIni("ssl", ena)

def ovsAgentSetPort(port):
    return _ovsIni("port", port)

def ovsRestartAgent():
    return restartService("ovs-agent")

def ovsRestartXend():
    return restartService("xend")

# replace with popen
def restartService(service):
    command = ['service', service, 'restart'];
    subprocess.call(command, shell=False)
    return True

# sets the control interface and removes the route net entry
def ovsControlInterface(dev, ip, mask):
    controlRoute = False
    command = ['route', '-n'];
    p = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE)
    while True:
        line= p.stdout.readline()
        if line == '' and p.poll() != None:
            break
        if line != '':
            if re.search("%s" % (ip), line) and not re.search("%s" % (dev), line):
                command = ['route','del','-net', ip, 'gw', '0.0.0.0', 'netmask', mask]
                subprocess.call(command, shell=False)
                print "removed: %s" % (line)
            elif re.search("%s" % (ip), line) and re.search("%s" % (dev), line):
                controlRoute = True
    
    if controlRoute == False:
        command = ['route', 'add', '-net', ip, 'netmask', mask, 'dev', dev];
        subprocess.call(command, shell=False)

    command = ['ifconfig', dev, 'arp']
    subprocess.call(command, shell=False)
    return True

# create a dir if we need it
def ovsMkdir(dir, mode=0700):
    if not os.path.exists(dir):
        return os.makedirs(dir, mode)
    return True

# if a file exists, easy
def ovsCheckFile(file):
    if os.path.isfile(file):
        return True
    return False

def ovsUploadFile():
    return True

# upload keys
def ovsUploadSshKey(keyfile, content):
    keydir=os.path.expanduser("~/.ssh")
    key="%s/%s" % (keydir, keyfile)
    try:
        ovsMkdir(os.path.expanduser(keydir))
    except Error, v:
        print "was already there %s" % keydir

    try:
        text_file = open("%s" % key, "w")
        text_file.write("%s" % content)
        text_file.close()
    except Error, v:
        print "something went wrong %s" % v
        return False
    return True


# older python,
def ovsDom0Stats(bridge):
    stats={}
    stats['cpu'] = "%s" % (100 - float(os.popen("top -b -n 1 | grep Cpu\(s\): | cut -d% -f4|cut -d, -f2").read()))
    stats['free'] = "%s" % (1048576 * int(os.popen("xm info | grep free_memory | awk '{ print $3 }'").read()))
    stats['total'] = "%s" % (1048576 * int(os.popen("xm info | grep total_memory | awk '{ print $3 }'").read()))
    stats['tx'] = os.popen("netstat -in | grep %s | head -1 | awk '{print $4 }'" % bridge).read()
    stats['rx'] = os.popen("netstat -in | grep %s | head -1 | awk '{print $8 }'" % bridge).read()
    return stats

def getVncPort(domain):
    port = "0"
    if re.search("\w-(\d+-)?\d+-VM", domain):
        server=ServerProxy(XendClient.uri)
        dom=server.xend.domain(domain, 1)
        devices = [child for child in sxp.children(dom)
            if len(child) > 0 and child[0] == "device"]
        vfbs_sxp = map(lambda x: x[1], [device for device in devices 
            if device[1][0] == "vfb"])[0]
        loc=[child for child in vfbs_sxp
            if child[0] == "location"][0][1]
        listner, port = loc.split(":")
    else:
        print "no valid domain: %s" % domain
    return port

def get_child_by_name(exp, childname, default = None):
    try:
        return [child for child in sxp.children(exp)
                if child[0] == childname][0][1]
    except:
        return default

def ovsDomUStats(domain):
    _rd_bytes=0
    _wr_bytes=0
    _rd_ops=0
    _wr_ops=0
    _tx_bytes=0
    _rx_bytes=0
    stats={}
    server=ServerProxy(XendClient.uri)
    dominfo=server.xend.domain(domain, 1)
    domid=get_child_by_name(dominfo, "domid")

    # vbds
    devs = server.xend.domain.getDeviceSxprs(domain, 'vbd')
    devids = [dev[0] for dev in devs]
    for dev in devids:
        sys_path="/sys/devices/%s-%s-%s/statistics" % ("vbd", domid, dev)
        _rd_bytes += long(open("%s/rd_sect" % sys_path).readline().strip())
        _wr_bytes += long(open("%s/wr_sect" % sys_path).readline().strip())
        _rd_ops += long(open("%s/rd_req" % sys_path).readline().strip())
        _wr_ops += long(open("%s/wr_req" % sys_path).readline().strip())

    # vifs
    devs = server.xend.domain.getDeviceSxprs(domain, 'vif')
    devids = [dev[0] for dev in devs]
    for dev in devids:
        vif="vif%s.%s" % (domid, dev)
        sys_path="/sys/devices/%s-%s-%s/net/%s/statistics" % ("vif", domid, dev, vif)
        _tx_bytes += long(open("%s/tx_bytes" % sys_path).readline().strip())
        _rx_bytes += long(open("%s/rx_bytes" % sys_path).readline().strip())

    epoch=time.time()
    stats['rd_bytes'] = "%s" % (_rd_bytes * 512)
    stats['wr_bytes'] = "%s" % (_wr_bytes * 512)
    stats['rd_ops'] = "%s" % (_rd_ops)
    stats['wr_ops'] = "%s" % (_wr_ops)
    stats['tx_bytes'] = "%s" % (_tx_bytes)
    stats['rx_bytes'] = "%s" % (_rx_bytes)
    stats['cputime']= "%s" % get_child_by_name(dominfo, "cpu_time")
    stats['uptime']= "%s" % (epoch - get_child_by_name(dominfo, "start_time"))
    stats['vcpus']= "%s" % get_child_by_name(dominfo, "online_vcpus")
    return stats

def ping(host, count=3):
    if os.system("ping -c %s %s " % (count, host)) == 0:
        return True
    return False

# add SystemVM stuff here....
#

#
# Self deploy and integration, not de-integration 
# should return False if fails
#
# install us if we are missing in: 
# /usr/lib64/python2.4/site-packages/agent/api
# and add our hooks in:
# /usr/lib64/python2.4/site-packages/agent/target/api.py
if __name__ == '__main__':
    from distutils.sysconfig import get_python_lib
    from agent.target.api import MODULES
    from shutil import copyfile
    import inspect, os, hashlib, getopt, sys

    # default vars
    exist=False
    agentpath="%s/agent" % (get_python_lib(1))
    api="%s/target/api.py" % (agentpath)
    modpath="%s/api" % (agentpath)
    ssl="disable"
    port=0
    exec_sub=""
    exec_opts=""

    # get options
    try:
        opts, args = getopt.getopt(sys.argv[1:], "eosp::", 
            [ 'port=','ssl=','exec=','opts='])
    except getopt.GetoptError:
        print "Available Options: --port=<number>, --ssl=<true|false>, --exec=<method>, --opts=<arg1,arg2..>"
        sys.exit()

    for o, a in opts:
        if o in ('-s', '--ssl'):
            ssl = a
        if o in ('-p', '--port'):
            port = int(a)
        if o in ('-e', '--exec'):
            exec_sub = a
        if o in ('-o', '--opts'):
            exec_opts = a
    
    if exec_sub != "":
        func = "%s(%s)" % (exec_sub, exec_opts)
        print "exec: %s" % (func)
        opts=exec_opts.split(',')
        print locals()[exec_sub](*opts)
        sys.exit()
    
    # check if we're in the modules already
    cs = CloudStack()
    for mod in MODULES:
        if re.search(cs.getName(), "%s" % (mod)):
            exist=True

    # if we're not:
    if not exist:
        if os.path.isfile(api):
            import fileinput
            for line in fileinput.FileInput(api, inplace=1):
                line=line.rstrip('\n')
                if re.search("import common", line):
                    line="%s, cloudstack" % (line)
                if re.search("MODULES", line):
                    n=cs.getName()
                    line="%s\n\t%s.%s," % (line, n.lower(), n)
                print line
            print "Api inserted, %s in %s" % (cs.getName(), api)
        else:
            print "Api missing, %s" % (api)
    else:
        print "Api present, %s in %s" % (cs.getName(), api)

    # either way check our version and install if checksum differs
    modfile="%s/%s.py" % (modpath, cs.getName().lower())
    me=os.path.abspath(__file__)
    if os.path.isfile(modfile):
        if hashlib.md5(open(me).read()).hexdigest() != hashlib.md5(open(modfile).read()).hexdigest():
            print "Module copy, %s" % (modfile)
            copyfile(me, modfile)
        else:
            print "Module correct, %s" % (modfile)
    else:
        print "Module copy, %s" % (modfile)
        copyfile(me, modfile)
    
    # setup ssl and port
    if ssl:
        ovsAgentSetSsl(ssl)
    if port > 1024:
        ovsAgentSetPort(port)

    # restart either way
    ovmCsPatch()
    ovsRestartAgent()
