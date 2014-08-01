#!/usr/bin/python
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
import sys
from merge import dataBag
from pprint import pprint
import subprocess
import logging
import re
import time

class csHelper:

    def upFile(self, fn, val, mode):
        for line in open(fn):
            if line.strip().lstrip("0") == val:
               return
        # set the value
        f = open(fn, mode)
        f.write(val)
        f.close

    def definedInFile(self, fn, val):
        for line in open(fn):
            if re.search(val, line):
               return True
        return False

    def addIfMissing(self, fn, val):
        if not csHelper().definedInFile(fn, val):
       	    csHelper().upFile(fn, val + "\n", "a")
            logging.debug("Added %s to file %s" % (val, fn))

    def execute(self, command):
        p = subprocess.Popen(command, stdout=subprocess.PIPE, shell=True)
        result = p.communicate()[0]
        return result.splitlines()

# ----------------------------------------------------------- #
# Manage ip rules (such as fwmark)
# ----------------------------------------------------------- #
class csRule:
#sudo ip rule add fwmark $tableNo table $tableName
    def __init__(self, dev):
        self.dev = dev
        self.tableNo = dev[3]
        self.table   = "Table_%s" % (dev)

    def addMark(self):
        if not self.findMark():
           cmd = "ip rule add fwmark %s table %s" % (self.tableNo, self.table)
           csHelper().execute(cmd)
           logging.info("Added fwmark rule for %s" % (self.table))
           
    def findMark(self):
        srch = "from all fwmark 0x%s lookup %s" % (self.tableNo, self.table)
        for i in csHelper().execute("ip rule show"):
            if srch in i.strip():
               return True
        return False

class csRoute:

    def __init__(self, dev):
        self.dev = dev
        self.tableNo = dev[3]
        self.table   = "Table_%s" % (dev)

    def routeTable(self):
        str = "%s %s" % (self.tableNo, self.table)
        fn = "/etc/iproute2/rt_tables"
        csHelper().addIfMissing(fn, str)

    def flush(self):
        csHelper().execute("ip route flush table %s" % (self.table) )
        csHelper().execute("ip route flush cache")

    def add(self, address):
  # ip route show dev eth1 table Table_eth1 10.0.2.0/24
  # sudo ip route add default via $defaultGwIP table $tableName proto static
        cmd = "dev %s table %s %s" % (self.dev, self.table, address['network'])
        self.addIfMissing(cmd)

    def addIfMissing(self, cmd):
        found = False
        for i in csHelper().execute("ip route show " + cmd):
            found = True
        if not found:
           logging.info("Add " + cmd)
           cmd = "ip route add " + cmd
           csHelper().execute(cmd)
     

class csRpsrfs:

    def __init__(self, dev):
        self.dev = dev

    def enable(self):
        if not self.inKernel(): return
        cpus = self.cpus()
        if cpus < 2: return
        val = format((1 << cpus) - 1, "x")
        fn = "/sys/class/net/%s/queues/rx-0/rps_cpus" % (self.dev)
        csHelper().upFile(fn, val, "w+")
        csHelper().upFile("/proc/sys/net/core/rps_sock_flow_entries", "256", "w+")
        fn = "/sys/class/net/%s/queues/rx-0/rps_flow_cnt" % (self.dev)
        csHelper().upFile(fn, "256", "w+")
        logging.debug("rpsfr is configured for %s cpus" % (cpus))

    def inKernel(self):
        try:
            open('/etc/rpsrfsenable')
        except IOError:
            logging.debug("rpsfr is not present in the kernel")
            return False
        else:
            logging.debug("rpsfr is present in the kernel")
            return True

    def cpus(self):
        count = 0
        for line in open('/proc/cpuinfo'):
            if "processor" not in line: continue
            count += 1
        if count < 2: logging.debug("Single CPU machine")
        return count

class csDevice:

    def __init__(self, dev):
        self.devlist = []
        self.dev = dev
        self.buildlist()

    # ------------------------------------------------------- #
    # List all available network devices on the system
    # ------------------------------------------------------- #
    def buildlist(self):
        self.devlist = []
        for line in open('/proc/net/dev'):
            vals = line.lstrip().split(':')
            if(not vals[0].startswith("eth")):
               continue
            # Ignore control interface for now
            if(vals[0] == 'eth0'):
               continue
            self.devlist.append(vals[0])

    # ------------------------------------------------------- #
    # Wait up to 15 seconds for a device to become available
    # ------------------------------------------------------- #
    def waitForDevice(self):
        count = 0
        while count < 15:
           if self.dev in self.devlist:
              return True
           time.sleep(1)
           count += 1
           self.buildlist();
        logging.error("Address %s on device %s cannot be configured - device was not found", ip.ip(), dev)
        return False

    def list(self):
        return self.devlist

    # ------------------------------------------------------- #
    # Ensure device is up
    # ------------------------------------------------------- #
    def setUp(self):
        cmd = "ip link show %s | grep 'state DOWN'" % (self.dev)
        for i in csHelper().execute(cmd):
            if " DOWN " in i:
               cmd2 = "ip link set %s up" % (self.dev)
               csHelper().execute(cmd2)

class csIp:

    def __init__(self,dev):
        self.dev = dev
        self.iplist = {}
        self.address = {}
        self.list()

    def setAddress(self, address):
        self.address = address

    def configure(self):
        logging.info("Configuring address %s on device %s", self.ip(), self.dev)
        cmd = "ip addr add dev %s %s brd +" % (self.dev, self.ip())
        subprocess.call(cmd, shell=True)
        route = csRoute(self.dev)
        route.routeTable()
        csRule(self.dev).addMark()
        csDevice(self.dev).setUp()
        self.arpPing()
        route.add(self.address)
        csRpsrfs(self.dev).enable()
        route.flush()

    def list(self):
        self.iplist = {}
        cmd = ("ip addr show dev " + self.dev)
        for i in csHelper().execute(cmd):
            vals = i.lstrip().split()
            if(vals[0] == 'inet'):
               self.iplist[vals[1]] = self.dev

    def configured(self):
        dev = self.address['device']
        if(self.address['cidr'] in self.iplist.keys()):
           return True
        return False
  
    def ip(self):
        return str(self.address['cidr'])
 
    def hasIP(self, ip):
        return ip in self.address.values()

    def arpPing(self):
        cmd = "arping -c 1 -I %s -A -U -s %s %s" % (self.dev, self.address['public_ip'], self.address['public_ip'])
        csHelper().execute(cmd)

    # Delete any ips that are configured but not in the bag
    def compare(self, bag):
        if(len(self.iplist) > 0 and not self.dev in bag.keys()):
           # Remove all IPs on this device
           logging.info("Will remove all configured addresses on device %s", self.dev)
           self.delete("all")
           return False
        for ip in self.iplist:
            found = False
            for address in bag[self.dev]:
                self.setAddress(address)
                if(self.hasIP(ip)):
                    found = True
            if(not found):
                self.delete(ip)

    def delete(self, ip):
        remove = []
        if(ip == "all"):
            logging.info("Removing addresses from device %s", self.dev)
            remove = self.iplist.keys()
        else:
            remove.append(ip)
        for ip in remove:
            cmd = "ip addr del dev %s %s" % (self.dev, ip)
            subprocess.call(cmd, shell=True)
            logging.info("Removed address %s from device %s", ip, self.dev)
            
            

# Main
logging.basicConfig(filename='/var/log/cloud.log',level=logging.DEBUG, format='%(asctime)s %(message)s')

db = dataBag()
db.setKey("ips")
db.load()
dbag = db.getDataBag()
for dev in csDevice('').list():
    ip = csIp(dev)
    ip.compare(dbag)

for dev in dbag:
    if dev == "id":
        continue
    ip = csIp(dev)
    for address in dbag[dev]:
        csRoute(dev).add(address)
        ip.setAddress(address)
        if ip.configured():
           logging.info("Address %s on device %s already configured", ip.ip(), dev)
        else:
           logging.info("Address %s on device %s not configured", ip.ip(), dev)
           if csDevice(dev).waitForDevice():
              ip.configure()
