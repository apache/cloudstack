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
import os
from merge import dataBag
from pprint import pprint
import subprocess
import logging
import re
import time
import shutil
import os.path

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

    def service(self, name, op):
        self.execute("service %s %s" % (name, op))
        logging.info("Service %s %s" % (name, op))

    def copy_if_needed(self, src, dest):
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

class CsFile:
    """ File editors """

    def __init__(self, filename):
        self.filename = filename
        self.changed  = False
        self.load()

    def load(self):
        self.new_config = []
        for line in open(self.filename):
            self.new_config.append(line)
        logging.debug("Reading file %s" % self.filename)

    def is_changed(self):
        return self.changed

    def commit(self):
        if not self.changed:
            return
        handle = open(self.filename, "w+")
        for line in self.new_config:
            handle.write(line)
        handle.close()
        logging.info("Wrote edited file %s" % self.filename)

    def search(self, search, replace):
        found   = False
        logging.debug("Searching for %s and replacing with %s" % (search, replace))
        for index, line in enumerate(self.new_config):
            if line.lstrip().startswith("#"):
                continue
            if re.search(search, line):
                found = True
                if not replace in line:
                    self.changed = True
                    self.new_config[index] = replace + "\n"
        if not found:
           self.new_config.append(replace + "\n")
           self.changed = True

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


class CsRoute:
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

    def add(self, address, method = "add"):
        # ip route show dev eth1 table Table_eth1 10.0.2.0/24
        if(method == "add"):
            cmd = "dev %s table %s %s" % (self.dev, self.table, address['network'])
            self.set_route(cmd, method)

    def set_route(self, cmd, method = "add"):
        """ Add a route is it is not already defined """
        found = False
        for i in CsHelper().execute("ip route show " + cmd):
            found = True
        if not found and method == "add":
            logging.info("Add " + cmd)
            cmd = "ip route add " + cmd
        elif found and method == "delete":
            logging.info("Delete " + cmd)
            cmd = "ip route delete " + cmd
        else:
            return
        CsHelper().execute(cmd)


class CsRpsrfs:
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


class CsProcess(object):
    """ Manipulate processes """

    def __init__(self, search):
        self.search = search

    def start(self, thru, background = ''):
        #if(background):
            #cmd = cmd + " &"
        logging.info("Started %s", " ".join(self.search))
        os.system("%s %s %s" % (thru, " ".join(self.search), background))

    def find(self):
        self.pid = []
        for i in CsHelper().execute("ps aux"):
            items = len(self.search)
            proc = re.split("\s+", i)[items*-1:]
            matches = len([m for m in proc if m in self.search])
            if matches == items:
                self.pid.append(re.split("\s+", i)[1])
        return len(self.pid) > 0

class CsApp:
    def __init__(self, ip):
        self.dev     = ip.getDevice()
        self.ip      = ip.getAddress()['public_ip']
        self.domain  = "domain.local"
        self.type    = ip.get_type()
        if self.type == "guest":
            gn = CsGuestNetwork(self.dev)
            self.domain = gn.get_domain()

class CsPasswdSvc(CsApp):
    """
      nohup bash /opt/cloud/bin/vpc_passwd_server $ip >/dev/null 2>&1 &
    """

    def setup(self):
        cmds = "-A INPUT -i %s -d %s -p %s -m %s --state %s --dport %s -j %s"
        slist = [ self.dev, self.ip, "tcp", "state", "NEW", "8080", "ACCEPT" ]

        firewall = CsIpTables(self.dev)
        firewall.change_rule("", slist, cmds)

        proc = CsProcess(['/opt/cloud/bin/vpc_passwd_server', self.ip])
        if not proc.find():
            proc.start("/usr/bin/nohup", "2>&1 &")

class CsApache(CsApp):
    """ Set up Apache """

    def setup(self):
        CsHelper().copy_if_needed("/etc/apache2/vhostexample.conf",
                                  "/etc/apache2/conf.d/vhost%s.conf" % self.dev)

        file = CsFile("/etc/apache2/conf.d/vhost%s.conf" % (self.dev))
        file.search("<VirtualHost.*:80>", "\t<VirtualHost %s:80>" % (self.ip))
        file.search("<VirtualHost.*:80>", "\t<VirtualHost %s:80>" % (self.ip))
        file.search("<VirtualHost.*:443>", "\t<VirtualHost %s:443>" % (self.ip))
        file.search("Listen .*:80", "Listen %s:80" % (self.ip))
        file.search("Listen .*:443", "Listen %s:443" % (self.ip))
        file.search("ServerName.*", "\tServerName vhost%s.cloudinternal.com" % (self.dev))
        file.commit()
        if file.is_changed():
            CsHelper().service("apache2", "restart")

        cmds = "-A INPUT -i %s -d %s -p %s -m %s --state %s --dport %s -j %s"
        slist = [ self.dev, self.ip, "tcp", "state", "NEW", "80", "ACCEPT" ]

        firewall = CsIpTables(self.dev)
        firewall.change_rule("", slist, cmds)


class CsDnsmasq(CsApp):
    """ Set up dnsmasq """

    def add_firewall_rules(self, method):
        """ Add the necessary firewall rules 
        This is problamatic because the current logic cannot delete them
        (In a convergence model)

        We will need to store some state about what "used" to be there
        """
        firewall = CsIpTables(self.dev)

        cmds = "-A INPUT -i %s -p %s -m %s --dport %s -j %s"
        slist = [ self.dev, "udp", "udp", "67", "ACCEPT" ]
        firewall.change_rule("", slist, cmds)

        cmds = "-A INPUT -i %s -d %s -p %s -m %s --dport %s -j %s"
        slist = [ self.dev, self.ip, "udp", "udp", "53", "ACCEPT" ]
        firewall.change_rule("", slist, cmds)

        cmds = "-A INPUT -i %s -d %s -p %s -m %s --dport %s -j %s"
        slist = [ self.dev, self.ip, "tcp", "tcp", "53", "ACCEPT" ]
        firewall.change_rule("", slist, cmds)

    def configure_server(self, method = "add"):
        file = CsFile("/etc/dnsmasq.d/cloud.conf")
        file.search("dhcp-range=interface:%s" % self.dev, \
                    "dhcp-range=interface:%s,set:interface-%s,%s,static" % (self.dev, self.dev, self.ip))
        file.search("dhcp-option=tag:interface-%s," % self.dev, \
                    "dhcp-option=tag:interface-%s,15,%s" % (self.dev, self.domain))
        file.commit()

        if file.is_changed():
            CsHelper().service("dnsmasq", "restart")


class CsGuestNetwork:
    def __init__(self, device):
        self.data = {}
        db = dataBag()
        db.setKey("guestnetwork")
        db.load()
        dbag = db.getDataBag()
        for dev in dbag:
            if dev == "id":
                continue
            if dev == device:
                self.data = dbag[dev][0]

    def get_domain(self):
        if 'domain_name' in self.data:
            return self.data['domain_name']
        else:
            return "cloudnine.internal"

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
            self.table = "Table_%s" % dev

    def configure_rp(self):
        """
        Configure Reverse Path Filtering
        """
        filename = "/proc/sys/net/ipv4/conf/%s/rp_filter" % self.dev
        CsHelper().updatefile(filename, "1\n", "w")

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
            if vals[0] == 'eth0':
                continue
            self.devlist.append(vals[0])


    def waitfordevice(self):
        """ Wait up to 15 seconds for a device to become available """
        count = 0
        while count < 15:
            if self.dev in self.devlist:
                return True
            time.sleep(1)
            count += 1
            self.buildlist();
        logging.error("Device %s cannot be configured - device was not found", self.dev)
        return False

    def list(self):
        return self.devlist

    def setUp(self):
        """ Ensure device is up """
        cmd = "ip link show %s | grep 'state DOWN'" % self.dev
        for i in CsHelper().execute(cmd):
            if " DOWN " in i:
                cmd2 = "ip link set %s up" % self.dev
                CsHelper().execute(cmd2)
        CsIpTables(self.dev).set_connmark()


class CsIpTables:

    """ Utility class
    All the bits and pieces needed for iptables operations
    """
    def __init__(self, dev):
        self.dev = dev
        self.tableNo = dev[3]
        self.table = "Table_%s" % (dev)
        self.devChain = "ACL_INBOUND_%s" % (dev)

    def set_connmark(self, method = "add"):
        """ Set connmark for device """
        slist = ["PREROUTING", self.dev, "state", "NEW", "CONNMARK", self.tableNo]
        if method == "add":
            cmds="-A %s -i %s -m %s --state %s -j %s --set-mark 0x%s"
        else:
            cmds="-D %s -i %s -m %s --state %s -j %s --set-mark 0x%s"
        self.change_rule("mangle", slist, cmds)

    def set_accept(self, table, method = "add"):
        """ Add an accept rule
        First version - very simple - once I find out what the patterns
        are this will be refactored
        """
        slist = [ self.devChain, "ACCEPT" ]
        if method == "add":
            cmds  = "-A %s -j %s"
        else:    
            cmds  = "-D %s -j %s"
        self.change_rule( table, slist, cmds)

    def set_forward(self, ip, method = "add"):
        """ set a forward to the device chain
        takes a CsIP object """
        slist = [ "FORWARD", self.dev, ip['network'], self.devChain ]
        if method == "add":
            cmds = "-A %s -o %s -d %s -j %s"
        else:    
            cmds = "-D %s -o %s -d %s -j %s"
        self.change_rule('', slist, cmds)

    def set_drop(self, method = "add"):
        """ Ensure the last rule is drop """
        slist = [ self.devChain, "DROP" ]
        if method == "add":
            cmds="-A %s -j %s"
        else:
            cmds="-D %s -j %s"
        self.change_rule('', slist, cmds)

    def set_static_nat(self, ip, method = "add"):
        """ Add static nat to a device/ip combination
        Takes a CsIp object as its parameter
        """
        slist = ["POSTROUTING", ip['network'], self.dev, "SNAT", ip['public_ip']]
        if method == "add":
            cmds   ="-A %s -s %s -o %s -j %s --to-source %s"
        else:
            cmds   ="-D %s -s %s -o %s -j %s --to-source %s"
        self.change_rule('nat', slist, cmds)

    def set_preroute(self, ip, method):
        slist = [ "PREROUTING", "NEW", self.dev, ip['network'], ip['public_ip'], self.devChain ]
        cmds = "-A %s -m state --state %s -i %s -s %s ! -d %s -j %s"
        if method == "add":
            self.change_rule('mangle', slist, cmds)

    def change_rule(self, table, slist, cmds):
        cmd = ''
        if not self.has_rule(table, slist):
            if table != '':
                cmd = "-t %s " % table
            cmd += cmds % tuple(slist)
            CsHelper().execute("iptables %s" % (cmd))
            logging.info("iptables %s", cmd)

    def set_chain(self, table, method):
        """ Create a chain if it does not already exist """
        slist = [ self.devChain ]
        cmd = ''
        if not self.has_rule(table, slist):
            if table != '':
                cmd = "-t %s " % table
            cmd += "-N %s" % tuple(slist)
            CsHelper().execute("iptables %s" % (cmd))
            logging.info("iptables %s", cmd)

    def has_rule(self, table, list):
        """ Check if a particular rule exists """
        cmd = "iptables-save "
        if table != "":
           cmd += "-t %s" % table
        for line in CsHelper().execute(cmd):
            matches = len([i for i in list if i in line])
            if matches == len(list):
                return True
        return False

class CsIP:

    def __init__(self, dev):
        self.dev = dev
        self.iplist = {}
        self.address = {}
        self.list()

    def setAddress(self, address):
        self.address = address

    def getAddress(self):
        return self.address

    def configure(self):
        logging.info("Configuring address %s on device %s", self.ip(), self.dev)
        cmd = "ip addr add dev %s %s brd +" % (self.dev, self.ip())
        subprocess.call(cmd, shell=True)
        self.post_configure()

    def post_configure(self):
        """ The steps that must be done after a device is configured """
        if not self.get_type() in [ "control" ]:
            route = CsRoute(self.dev)
            route.routeTable()
            CsRule(self.dev).addMark()
            CsDevice(self.dev).setUp()
            self.arpPing()
            CsRpsrfs(self.dev).enable()
            self.post_config_change("add")

    def get_type(self):
        """ Return the type of the IP
        guest
        control
        public
        """
        if "nw_type" in self.address:
            return self.address['nw_type']
        return "unknown"

    def post_config_change(self, method):
        route = CsRoute(self.dev)
        route.routeTable()
        route.add(self.address, method)
        # On deletion nw_type will no longer be known
        if self.get_type() in [ "guest" ]:
            CsDevice(self.dev).configure_rp()
            CsIpTables(self.dev).set_static_nat(self.address, method)
            CsIpTables(self.dev).set_chain('', method)
            CsIpTables(self.dev).set_chain('mangle', method)
            CsIpTables(self.dev).set_accept('mangle', method)
            CsIpTables(self.dev).set_forward(self.address, method)
            CsIpTables(self.dev).set_drop(method)
            CsIpTables(self.dev).set_preroute(self.address, method)
            dns = CsDnsmasq(self)
            dns.add_firewall_rules("add")
            dns.configure_server()
            app = CsApache(self)
            app.setup()
            pwdsvc = CsPasswdSvc(self).setup()

        route.flush()

    def list(self):
        self.iplist = {}
        cmd = ("ip addr show dev " + self.dev)
        for i in CsHelper().execute(cmd):
            vals = i.lstrip().split()
            if (vals[0] == 'inet'):
                self.iplist[vals[1]] = self.dev

    def configured(self):
        if self.address['cidr'] in self.iplist.keys():
            return True
        return False

    def ip(self):
        return str(self.address['cidr'])

    def getDevice(self):
        return self.dev

    def hasIP(self, ip):
        return ip in self.address.values()

    def arpPing(self):
        cmd = "arping -c 1 -I %s -A -U -s %s %s" % (self.dev, self.address['public_ip'], self.address['public_ip'])
        CsHelper().execute(cmd)

    # Delete any ips that are configured but not in the bag
    def compare(self, bag):
        if len(self.iplist) > 0 and not self.dev in bag.keys():
            # Remove all IPs on this device
            logging.info("Will remove all configured addresses on device %s", self.dev)
            self.delete("all")
        for ip in self.iplist:
            found = False
            for address in bag[self.dev]:
                self.setAddress(address)
                if self.hasIP(ip):
                    found = True
            if not found:
                self.delete(ip)

    def delete(self, ip):
        remove = []
        if ip == "all":
            logging.info("Removing addresses from device %s", self.dev)
            remove = self.iplist.keys()
        else:
            remove.append(ip)
        for ip in remove:
            cmd = "ip addr del dev %s %s" % (self.dev, ip)
            subprocess.call(cmd, shell=True)
            logging.info("Removed address %s from device %s", ip, self.dev)
            self.post_config_change("delete")

def main(argv):

    logging.basicConfig(filename='/var/log/cloud.log',
                        level=logging.DEBUG,
                        format='%(asctime)s %(message)s')

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
        ip = CsIP(dev)
        for address in dbag[dev]:
            CsRoute(dev).add(address)
            ip.setAddress(address)
            if ip.configured():
                logging.info("Address %s on device %s already configured", ip.ip(), dev)
                ip.post_configure()
            else:
                logging.info("Address %s on device %s not configured", ip.ip(), dev)
                if CsDevice(dev).waitfordevice():
                    ip.configure()

if __name__ == "__main__":
    main(sys.argv)
