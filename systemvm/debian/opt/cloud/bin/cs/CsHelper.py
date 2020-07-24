# -- coding: utf-8 --
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
""" General helper functions
for use in the configuration process

"""
import subprocess
import logging
import sys
import os.path
import re
import shutil
from ipaddress import *

PUBLIC_INTERFACES = {"router": "eth2", "vpcrouter": "eth1"}

STATE_COMMANDS = {"router": "ip addr show dev eth0 | grep inet | wc -l | xargs bash -c  'if [ $0 == 2 ]; then echo \"MASTER\"; else echo \"BACKUP\"; fi'",
                  "vpcrouter": "ip addr show dev eth1 | grep state | awk '{print $9;}' | xargs bash -c 'if [ $0 == \"UP\" ]; then echo \"MASTER\"; else echo \"BACKUP\"; fi'"}


def reconfigure_interfaces(router_config, interfaces):
    for interface in interfaces:
        cmd = "ip link show %s | grep ' state '" % interface.get_device()
        for device in execute(cmd):
            if " DOWN " in device:
                cmd = "ip link set %s up" % interface.get_device()
                # If redundant only bring up public interfaces that are not eth1.
                # Reason: private gateways are public interfaces.
                # master.py and keepalived will deal with eth1 public interface.

                if router_config.is_redundant() and interface.is_public():
                    state_cmd = STATE_COMMANDS[router_config.get_type()]
                    logging.info("Check state command => %s" % state_cmd)
                    state = execute(state_cmd)[0]
                    logging.info("Route state => %s" % state)
                    if interface.get_device() != PUBLIC_INTERFACES[router_config.get_type()] and state == "MASTER":
                        execute(cmd)
                else:
                    execute(cmd)


def is_mounted(name):
    for i in execute("mount"):
        vals = i.lstrip().split()
        if vals[0] == "tmpfs" and vals[2] == name:
            return True
    return False


def mount_tmpfs(name):
    if not is_mounted(name):
        execute("mount tmpfs %s -t tmpfs" % name)


def umount_tmpfs(name):
    if is_mounted(name):
        execute("umount %s" % name)


def rm(name):
    os.remove(name) if os.path.isfile(name) else None


def rmdir(name):
    if name:
        shutil.rmtree(name, True)


def mkdir(name, mode, fatal):
    try:
        os.makedirs(name, mode)
    except OSError as e:
        if e.errno != 17:
            print("failed to make directories " + name + " due to :" + e.strerror)
            if(fatal):
                sys.exit(1)


def updatefile(filename, val, mode):
    """ add val to file """
    handle = open(filename, 'r')
    for line in handle.read():
        if line.strip().lstrip() == val:
            return
    # set the value
    handle.close()
    handle = open(filename, mode)
    handle.write(val)
    handle.close()


def bool_to_yn(val):
    if val:
        return "yes"
    return "no"


def get_device_info():
    """ Returns all devices on system with their ipv4 ip netmask """
    list = []
    for i in execute("ip addr show |grep -v secondary"):
        vals = i.strip().lstrip().rstrip().split()
        if vals[0] == "inet":
            to = {}
            to['ip'] = vals[1]
            to['dev'] = vals[-1]
            to['network'] = ip_network(to['ip'])
            to['dnsmasq'] = False
            list.append(to)
    return list


def get_domain():
    for line in open("/etc/resolv.conf"):
        vals = line.lstrip().split()
        if vals[0] == "domain":
            return vals[1]
    return "cloudnine.internal"


def get_device(ip):
    """ Returns the device which has a specific ip
    If the ip is not found returns an empty string
    """
    for i in execute("ip addr show"):
        vals = i.strip().lstrip().rstrip().split()
        if vals[0] == "inet":
            if vals[1].split('/')[0] == ip:
                return vals[-1]
    return ""


def get_ip(device):
    """ Return first ip on an interface """
    cmd = "ip addr show dev %s" % device
    for i in execute(cmd):
        vals = i.lstrip().split()
        if (vals[0] == 'inet'):
            return vals[1]
    return ""


def definedinfile(filename, val):
    """ Check if val is defined in the file """
    for line in open(filename):
        if re.search(val, line):
            return True
    return False


def addifmissing(filename, val):
    """ Add something to a file
    if it is not already there """
    if not os.path.isfile(filename):
        logging.debug("File %s doesn't exist, so create" % filename)
        open(filename, "w").close()
    if not definedinfile(filename, val):
        updatefile(filename, val + "\n", "a")
        logging.debug("Added %s to file %s" % (val, filename))
        return True
    return False


def get_hostname():
    for line in open("/etc/hostname"):
        return line.strip()


def execute(command):
    """ Execute command """
    returncode = -1
    try:
        logging.info("Executing: %s" % command)
        result = subprocess.check_output(command, shell=True)
        returncode = 0

        logging.debug("Command [%s] has the result [%s]" % (command, result))
        return result.splitlines()
    except subprocess.CalledProcessError as e:
        logging.error(e)
        returncode = e.returncode
    finally:
        logging.debug("Executed: %s - exitstatus=%s " % (command, returncode))

    return list()


def save_iptables(command, iptables_file):
    """ Execute command """
    logging.debug("Saving iptables for %s" % command)

    result = execute(command)
    fIptables = open(iptables_file, "w+")

    for line in result:
        fIptables.write(line)
        fIptables.write("\n")
    fIptables.close()


def execute2(command, wait=True):
    """ Execute command """
    logging.info("Executing: %s" % command)
    p = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
    if wait:
        p.wait()
    return p


def service(name, op):
    execute("systemctl %s %s" % (op, name))
    logging.info("Service %s %s" % (name, op))


def start_if_stopped(name):
    ret = execute2("systemctl is-active %s" % name)
    if ret.returncode:
        execute2("systemctl start %s" % name)


def hup_dnsmasq(name, user):
    pid = ""
    for i in execute("ps -ef | grep %s" % name):
        vals = i.lstrip().split()
        if (vals[0] == user):
            pid = vals[1]
    if pid:
        logging.info("Sent hup to %s", name)
        execute("kill -HUP %s" % pid)
    else:
        service("dnsmasq", "start")


def copy_if_needed(src, dest):
    """ Copy a file if the destination does not already exist
    """
    if os.path.isfile(dest):
        return
    copy(src, dest)


def copy(src, dest):
    """
    copy source to destination.
    """
    try:
        shutil.copy2(src, dest)
    except IOError:
        logging.error("Could not copy %s to %s" % (src, dest))
    else:
        logging.info("Copied %s to %s" % (src, dest))
