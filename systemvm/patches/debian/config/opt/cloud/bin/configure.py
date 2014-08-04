#!/usr/bin/python
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
import sys
from merge import dataBag
from pprint import pprint
import subprocess
import logging
import re
import time

class CsHelper:
    """ General helper functions 
    for use in the configuation process

    TODO - Convert it to a module
    """
    def updatefile(self, filename, val, mode):
        """ add val to file """
        for line in open(filename):
            if line.strip().lstrip("0") == val:
                return
        # set the value
        handle = open(filename, mode)
        handle.write(val)
        handle.close()

    def definedinfile(self, filename, val):
        """ Check if val is defined in the file """
        for line in open(filename):
            if re.search(val, line):
                return True
        return False

    def addifmissing(self, filename, val):
        """ Add something to a file
        if it is not already there """
        if not CsHelper().definedinfile(filename, val):
             CsHelper().updatefile(filename, val + "\n", "a")
             logging.debug("Added %s to file %s" % (val, filename))

    def execute(self, command):
        """ Execute command """
        p = subprocess.Popen(command, stdout=subprocess.PIPE, shell=True)
        result = p.communicate()[0]
        return result.splitlines()


class CsRule:
    """ Manage iprules
    Supported Types:
    fwmark
    """

    def __init__(self, dev):
        self.dev = dev
        self.tableNo = dev[3]
        self.table = "Table_%s" % (dev)

    def addMark(self):
        if not self.findMark():
            cmd = "ip rule add fwmark %s table %s" % (self.tableNo, self.table)
            CsHelper().execute(cmd)
            logging.info("Added fwmark rule for %s" % (self.table))

    def findMark(self):
        srch = "from all fwmark 0x%s lookup %s" % (self.tableNo, self.table)
        for i in CsHelper().execute("ip rule show"):
            if srch in i.strip():
                return True
        return False


class csRoute:
    """ Manage routes """

    def __init__(self, dev):
        self.dev = dev
        self.tableNo = dev[3]
        self.table = "Table_%s" % (dev)

    def routeTable(self):
        str = "%s %s" % (self.tableNo, self.table)
        filename = "/etc/iproute2/rt_tables"
        CsHelper().addifmissing(filename, str)

    def flush(self):
        CsHelper().execute("ip route flush table %s" % (self.table))
        CsHelper().execute("ip route flush cache")

    def add(self, address):
        # ip route show dev eth1 table Table_eth1 10.0.2.0/24
        # sudo ip route add default via $defaultGwIP table $tableName proto static
        cmd = "dev %s table %s %s" % (self.dev, self.table, address['network'])
        self.addifmissing(cmd)

    def addifmissing(self, cmd):
        """ Add a route is it is not already defined """
        found = False
        for i in CsHelper().execute("ip route show " + cmd):
            found = True
        if not found:
            logging.info("Add " + cmd)
            cmd = "ip route add " + cmd
            CsHelper().execute(cmd)


class csRpsrfs:
    """ Configure rpsrfs if there is more than one cpu """

    def __init__(self, dev):
        self.dev = dev

    def enable(self):
        if not self.inKernel(): return
        cpus = self.cpus()
        if cpus < 2: return
        val = format((1 << cpus) - 1, "x")
        filename = "/sys/class/net/%s/queues/rx-0/rps_cpus" % (self.dev)
        CsHelper().updatefile(filename, val, "w+")
        CsHelper().updatefile("/proc/sys/net/core/rps_sock_flow_entries", "256", "w+")
        filename = "/sys/class/net/%s/queues/rx-0/rps_flow_cnt" % (self.dev)
        CsHelper().updatefile(filename, "256", "w+")
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


class CsDevice:
    """ Configure Network Devices """
    def __init__(self, dev):
        self.devlist = []
        self.dev = dev
        self.buildlist()
        self.table = ''
        self.tableNo = ''
        if dev != '':
            self.tableNo = dev[3]
            self.table = "Table_%s" % (dev)

    def buildlist(self):
        """
        List all available network devices on the system
        """
        self.devlist = []
        for line in open('/proc/net/dev'):
            vals = line.lstrip().split(':')
            if (not vals[0].startswith("eth")):
                 continue
            # Ignore control interface for now
            if (vals[0] == 'eth0'):
                continue
            self.devlist.append(vals[0])

    def set_connmark(self):
        """ Set connmark for device """
        if not self.has_connmark():
            cmd="-A PREROUTING -i %s -m state --state NEW -j CONNMARK --set-mark %s" \
                % (self.dev, self.tableNo)
            CsHelper().execute("iptables -t mangle %s" % (cmd))
            logging.error("Set connmark for device %s (Table %s)", self.dev, self.tableNo)

    def has_connmark(self):
        cmd = "iptables-save -t mangle"
        for line in CsHelper().execute(cmd):
            if not "PREROUTING" in line:
                continue
            if not "state" in line:
                continue
            if not "CONNMARK" in line:
                continue
            if not "set-xmark" in line:
                continue
            if not self.dev in line:
                continue
            return True
        return False


    def waitForDevice(self):
        """ Wait up to 15 seconds for a device to become available """
        count = 0
        while count < 15:
            if self.dev in self.devlist:
                return True
            time.sleep(1)
            count += 1
            self.buildlist();
        logging.error("Address %s on device %s cannot be configured - device was not found", ip.ip(), self.dev)
        return False

    def list(self):
        return self.devlist

    def setUp(self):
        """ Ensure device is up """
        cmd = "ip link show %s | grep 'state DOWN'" % (self.dev)
        for i in CsHelper().execute(cmd):
            if " DOWN " in i:
                cmd2 = "ip link set %s up" % (self.dev)
                CsHelper().execute(cmd2)
        self.set_connmark()


class CsIP:

    def __init__(self, dev):
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
        self.post_configure()

    def post_configure(self):
        """ The steps that must be done after a device is configured """
        route = csRoute(self.dev)
        route.routeTable()
        CsRule(self.dev).addMark()
        CsDevice(self.dev).setUp()
        self.arpPing()
        route.add(self.address)
        csRpsrfs(self.dev).enable()
        route.flush()

    def list(self):
        self.iplist = {}
        cmd = ("ip addr show dev " + self.dev)
        for i in CsHelper().execute(cmd):
            vals = i.lstrip().split()
            if (vals[0] == 'inet'):
                self.iplist[vals[1]] = self.dev

    def configured(self):
        dev = self.address['device']
        if (self.address['cidr'] in self.iplist.keys()):
            return True
        return False

    def ip(self):
        return str(self.address['cidr'])

    def hasIP(self, ip):
        return ip in self.address.values()

    def arpPing(self):
        cmd = "arping -c 1 -I %s -A -U -s %s %s" % (self.dev, self.address['public_ip'], self.address['public_ip'])
        CsHelper().execute(cmd)

    # Delete any ips that are configured but not in the bag
    def compare(self, bag):
        if (len(self.iplist) > 0 and not self.dev in bag.keys()):
            # Remove all IPs on this device
            logging.info("Will remove all configured addresses on device %s", self.dev)
            self.delete("all")
            return False
        for ip in self.iplist:
            found = False
            for address in bag[self.dev]:
                self.setAddress(address)
                if (self.hasIP(ip)):
                    found = True
            if (not found):
                self.delete(ip)

    def delete(self, ip):
        remove = []
        if (ip == "all"):
            logging.info("Removing addresses from device %s", self.dev)
            remove = self.iplist.keys()
        else:
            remove.append(ip)
        for ip in remove:
            cmd = "ip addr del dev %s %s" % (self.dev, ip)
            subprocess.call(cmd, shell=True)
            logging.info("Removed address %s from device %s", ip, self.dev)


def main(argv):
    logging.basicConfig(filename='/var/log/cloud.log', 
            level=logging.DEBUG, format='%(asctime)s %(message)s')

    db = dataBag()
    db.setKey("ips")
    db.load()
    dbag = db.getDataBag()
    for dev in CsDevice('').list():
        ip = CsIP(dev)
        ip.compare(dbag)

    for dev in dbag:
        if dev == "id":
            continue
        if dev == "eth0":
            continue
        ip = CsIP(dev)
        for address in dbag[dev]:
            csRoute(dev).add(address)
            ip.setAddress(address)
            if ip.configured():
                logging.info("Address %s on device %s already configured", ip.ip(), dev)
                ip.post_configure()
            else:
                logging.info("Address %s on device %s not configured", ip.ip(), dev)
                if CsDevice(dev).waitForDevice():
                    ip.configure()


if __name__ == "__main__":
    main(sys.argv)
