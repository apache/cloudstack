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
import os.path
import re
import shutil
from netaddr import *
from pprint import pprint


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
            print "failed to make directories " + name + " due to :" + e.strerror
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
    for i in execute("ip addr show"):
        vals = i.strip().lstrip().rstrip().split()
        if vals[0] == "inet":
            to = {}
            to['ip'] = vals[1]
            to['dev'] = vals[-1]
            to['network'] = IPNetwork(to['ip'])
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
    logging.debug("Executing %s" % command)
    p = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
    result = p.communicate()[0]
    return result.splitlines()


def save_iptables(command, iptables_file):
    """ Execute command """
    logging.debug("Saving iptables for %s" % command)

    result = execute(command)
    fIptables = open(iptables_file, "w+")

    for line in result:
        fIptables.write(line)
        fIptables.write("\n")
    fIptables.close()


def execute2(command):
    """ Execute command """
    logging.debug("Executing %s" % command)
    p = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
    p.wait()
    return p


def service(name, op):
    execute("service %s %s" % (name, op))
    logging.info("Service %s %s" % (name, op))


def start_if_stopped(name):
    ret = execute2("service %s status" % name)
    if ret.returncode:
        execute2("service %s start" % name)


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
    try:
        shutil.copy2(src, dest)
    except IOError:
        logging.Error("Could not copy %s to %s" % (src, dest))
    else:
        logging.info("Copied %s to %s" % (src, dest))
