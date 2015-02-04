#!/usr/bin/python
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
import sys
import os
from merge import dataBag
from cs_databag import CsDataBag, CsCmdLine
from pprint import pprint
import subprocess
import logging
import re
import time
import shutil
import os.path
import CsHelper
from CsNetfilter import CsNetfilters
from fcntl import flock, LOCK_EX, LOCK_UN
from CsDhcp import CsDhcp
from CsRedundant import *
from CsFile import CsFile

fw = []

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
            CsHelper.execute(cmd)
            logging.info("Added fwmark rule for %s" % (self.table))

    def findMark(self):
        srch = "from all fwmark 0x%s lookup %s" % (self.tableNo, self.table)
        for i in CsHelper.execute("ip rule show"):
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
        CsHelper.addifmissing(filename, str)

    def flush(self):
        CsHelper.execute("ip route flush table %s" % (self.table))
        CsHelper.execute("ip route flush cache")

    def add(self, address, method = "add"):
        # ip route show dev eth1 table Table_eth1 10.0.2.0/24
        if(method == "add"):
            cmd = "dev %s table %s %s" % (self.dev, self.table, address['network'])
            self.set_route(cmd, method)

    def set_route(self, cmd, method = "add"):
        """ Add a route is it is not already defined """
        found = False
        for i in CsHelper.execute("ip route show " + cmd):
            found = True
        if not found and method == "add":
            logging.info("Add " + cmd)
            cmd = "ip route add " + cmd
        elif found and method == "delete":
            logging.info("Delete " + cmd)
            cmd = "ip route delete " + cmd
        else:
            return
        CsHelper.execute(cmd)


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
        CsHelper.updatefile(filename, val, "w+")
        CsHelper.updatefile("/proc/sys/net/core/rps_sock_flow_entries", "256", "w+")
        filename = "/sys/class/net/%s/queues/rx-0/rps_flow_cnt" % (self.dev)
        CsHelper.updatefile(filename, "256", "w+")
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
        for i in CsHelper.execute("ps aux"):
            items = len(self.search)
            proc = re.split("\s+", i)[items*-1:]
            matches = len([m for m in proc if m in self.search])
            if matches == items:
                self.pid.append(re.split("\s+", i)[1])
        return len(self.pid) > 0


class CsApp:
    def __init__(self, ip):
        self.dev     = ip.getDevice()
        self.ip      = ip.get_ip_address()
        self.type    = ip.get_type()
        global fw

class CsPasswdSvc(CsApp):
    """
      nohup bash /opt/cloud/bin/vpc_passwd_server $ip >/dev/null 2>&1 &
    """

    def setup(self):
        fw.append(["", "front",
            "-A INPUT -i %s -d %s/32 -p tcp -m tcp -m state --state NEW --dport 8080 -j ACCEPT" % (self.dev, self.ip)
        ])

        proc = CsProcess(['/opt/cloud/bin/vpc_passwd_server', self.ip])
        if not proc.find():
            proc.start("/usr/bin/nohup", "2>&1 &")

class CsApache(CsApp):
    """ Set up Apache """

    def remove(self):
        file = "/etc/apache2/conf.d/vhost%s.conf" % self.dev
        if os.path.isfile(file):
            os.remove(file)
            CsHelper.service("apache2", "restart")


    def setup(self):
        CsHelper.copy_if_needed("/etc/apache2/vhostexample.conf",
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
            CsHelper.service("apache2", "restart")

        fw.append(["", "front",
            "-A INPUT -i %s -d %s/32 -p tcp -m tcp -m state --state NEW --dport 80 -j ACCEPT" % (self.dev, self.ip)
        ])

class CsDnsmasq(CsApp):
    """ Set up dnsmasq """

    def add_firewall_rules(self):
        """ Add the necessary firewall rules 
        """
        fw.append(["", "front",
            "-A INPUT -i %s -p udp -m udp --dport 67 -j ACCEPT" % self.dev
        ])

        fw.append(["", "front",
            "-A INPUT -i %s -d %s/32 -p udp -m udp --dport 53 -j ACCEPT" % (self.dev, self.ip)
        ])

        fw.append(["", "front",
            "-A INPUT -i %s -d %s/32 -p tcp -m tcp --dport 53 -j ACCEPT" % ( self.dev, self.ip )
        ])

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
        global fw

    def configure_rp(self):
        """
        Configure Reverse Path Filtering
        """
        filename = "/proc/sys/net/ipv4/conf/%s/rp_filter" % self.dev
        CsHelper.updatefile(filename, "1\n", "w")

    def buildlist(self):
        """
        List all available network devices on the system
        """
        self.devlist = []
        for line in open('/proc/net/dev'):
            vals = line.lstrip().split(':')
            if (not vals[0].startswith("eth")):
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
        for i in CsHelper.execute(cmd):
            if " DOWN " in i:
                cmd2 = "ip link set %s up" % self.dev
                CsHelper.execute(cmd2)
        cmd = "-A PREROUTING -i %s -m state --state NEW -j CONNMARK --set-xmark 0x%s/0xffffffff" % \
        (self.dev, self.dev[3])
        fw.append(["mangle", "", cmd])


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

    def get_ip_address(self):
        """ 
        Return ip address if known
        """
        if "public_ip" in self.address:
            return self.address['public_ip']
        return "unknown"


    def post_config_change(self, method):
        route = CsRoute(self.dev)
        route.routeTable()
        route.add(self.address, method)
        # On deletion nw_type will no longer be known
        if self.get_type() in [ "guest" ]:
            devChain = "ACL_INBOUND_%s" % (self.dev)
            CsDevice(self.dev).configure_rp()

            fw.append(["nat", "front", 
            "-A POSTROUTING -s %s -o %s -j SNAT --to-source %s" % \
            (self.address['network'], self.dev, self.address['public_ip'])
            ])
            fw.append(["mangle", "front", "-A %s -j ACCEPT" % devChain])

            fw.append(["", "front", 
            "-A FORWARD -o %s -d %s -j %s" % (self.dev, self.address['network'], devChain)
            ])
            fw.append(["", "", "-A %s -j DROP" % devChain])
            fw.append(["mangle", "", 
               "-A PREROUTING -m state --state NEW -i %s -s %s ! -d %s/32 -j %s" % \
               (self.dev, self.address['network'], self.address['public_ip'], devChain)
            ])
            dns = CsDnsmasq(self)
            dns.add_firewall_rules()
            app = CsApache(self)
            app.setup()
            pwdsvc = CsPasswdSvc(self).setup()
        elif self.get_type() == "public":
            if self.address["source_nat"] == True:
                cmdline = CsDataBag("cmdline")
                dbag = cmdline.get_bag()
                type = dbag["config"]["type"]
                if type == "vpcrouter":
                    vpccidr = dbag["config"]["vpccidr"]
                    fw.append(["filter", "", "-A FORWARD -s %s ! -d %s -j ACCEPT" % (vpccidr, vpccidr)])
                    fw.append(["nat","","-A POSTROUTING -j SNAT -o %s --to-source %s" % (self.dev, self.address['public_ip'])])
                elif type == "router":
                    logging.error("Not able to setup sourcenat for a regular router yet")
                else:
                    logging.error("Unable to process source nat configuration for router of type %s" % type)
        route.flush()

    def list(self):
        self.iplist = {}
        cmd = ("ip addr show dev " + self.dev)
        for i in CsHelper.execute(cmd):
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
        CsHelper.execute(cmd)

    # Delete any ips that are configured but not in the bag
    def compare(self, bag):
        if len(self.iplist) > 0 and (not self.dev in bag.keys() or len(bag[self.dev]) == 0):
            # Remove all IPs on this device
            logging.info("Will remove all configured addresses on device %s", self.dev)
            self.delete("all")
            app = CsApache(self)
            app.remove()

        # This condition should not really happen but did :)
        # It means an apache file got orphaned after a guest network address was deleted
        if len(self.iplist) == 0 and (not self.dev in bag.keys() or len(bag[self.dev]) == 0):
            app = CsApache(self)
            app.remove()

        for ip in self.iplist:
            found = False
            if self.dev in bag.keys():
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


class CsPassword(CsDataBag):
    """
      Update the password cache

      A stupid step really as we should just rewrite the password server to
      use the databag
    """
    cache = "/var/cache/cloud/passwords"

    def process(self):
        file = CsFile(self.cache)
        for item in self.dbag:
            if item == "id":
                continue
            self.__update(file, item, self.dbag[item])
        file.commit()

    def __update(self, file, ip, password):
        file.search("%s=" % ip, "%s=%s" % (ip, password))

class CsAcl(CsDataBag):
    """
        Deal with Network acls
    """

    class AclDevice():
        """ A little class for each list of acls per device """

        def __init__(self, obj):
            self.ingess = []
            self.egress = []
            self.device = obj['device']
            self.ip     = obj['nic_ip']
            self.netmask= obj['nic_netmask']
            self.cidr   = "%s/%s" % (self.ip, self.netmask)
            if "ingress_rules" in obj.keys():
                self.ingress = obj['ingress_rules']
            if "egress_rules" in obj.keys():
                self.egress = obj['egress_rules']

        def create(self):
            self.process("ingress", self.ingress)
            self.process("egress", self.egress)

        def process(self, direction, rule_list):
            for i in rule_list:
                r = self.AclRule(direction, self.device, i)
                r.create()

        class AclRule():

            def __init__(self, direction, device, rule):
                self.table = ""
                self.device = device
                self.chain = "ACL_INBOUND_%s" % self.device
                self.dest  = "-s %s" % rule['cidr']
                if direction == "egress":
                    self.table = "mangle"
                    self.chain = "ACL_OUTBOUND_%s" % self.device
                    self.dest  = "-d %s" % rule['cidr']
                self.type = ""
                self.type = rule['type']
                self.icmp_type = "any"
                self.protocol = self.type
                if "icmp_type" in rule.keys() and rule['icmp_type'] != -1:
                    self.icmp_type = rule['icmp_type']
                if "icmp_code" in rule.keys() and rule['icmp_code'] != -1:
                    self.icmp_type = "%s/%s" % (self.icmp_type, rule['icmp_code'])
                if self.type == "protocol":
                    if rule['protocol'] == 41:
                        rule['protocol'] = "ipv6"
                    self.protocol = rule['protocol']
                self.action = "DROP"
                self.dport = ""
                if 'allowed' in rule.keys() and rule['allowed'] and rule['allowed']:
                    self.action = "ACCEPT"
                global fw
                if 'first_port' in rule.keys():
                    self.dport = "-m %s --dport %s" % (self.protocol, rule['first_port'])
                if 'last_port' in rule.keys() and self.dport and \
                   rule['last_port'] != rule['first_port']:
                    self.dport = "%s:%s" % (self.dport, rule['last_port'])


            def create(self):
                rstr = ""
                rstr = "%s -A %s -p %s %s" % (rstr, self.chain, self.protocol, self.dest)
                if self.type == "icmp":
                    rstr = "%s -m icmp --icmp-type %s" % (rstr, self.icmp_type)
                rstr = "%s %s -j %s" % (rstr, self.dport, self.action)
                rstr = rstr.replace("  ", " ").lstrip()
                fw.append([self.table, "front", rstr])

                    
    def process(self):
        for item in self.dbag:
            if item == "id":
                continue
            dev_obj = self.AclDevice(self.dbag[item]).create()


class CsVmMetadata(CsDataBag):

    def process(self):
        for ip in self.dbag:
            if ("id" == ip):
                continue
            logging.info("Processing metadata for %s" % ip)
            for item in self.dbag[ip]:
                folder = item[0]
                file   = item[1]
                data   = item[2]

                # process only valid data
                if folder != "userdata" and folder != "metadata":
                    continue

                if file == "":
                    continue

                self.__htaccess(ip, folder, file)

                if data == "":
                    self.__deletefile(ip, folder, file)
                else:
                    self.__createfile(ip, folder, file, data)

    def __deletefile(self, ip, folder, file):
        datafile = "/var/www/html/" + folder + "/" + ip + "/" + file

        if os.path.exists(datafile):
            os.remove(datafile)

    def __createfile(self, ip, folder, file, data):
        dest = "/var/www/html/" + folder + "/" + ip + "/" + file
        metamanifestdir = "/var/www/html/" + folder + "/" + ip
        metamanifest =  metamanifestdir + "/meta-data"

        # base64 decode userdata
        if folder == "userdata" or folder == "user-data":
            if data is not None:
                data = base64.b64decode(data)

        fh = open(dest, "w")
        self.__exflock(fh)
        if data is not None:
            fh.write(data)
        else:
            fh.write("")
        self.__unflock(fh)
        fh.close()
        os.chmod(dest, 0644)

        if folder == "metadata" or folder == "meta-data":
            try:
                os.makedirs(metamanifestdir, 0755)
            except OSError as e:
                # error 17 is already exists, we do it this way for concurrency
                if e.errno != 17:
                    print "failed to make directories " + metamanifestdir + " due to :" +e.strerror
                    sys.exit(1)
            if os.path.exists(metamanifest):
                fh = open(metamanifest, "r+a")
                self.__exflock(fh)
                if not file in fh.read():
                    fh.write(file + '\n')
                self.__unflock(fh)
                fh.close()
            else:
                fh = open(metamanifest, "w")
                self.__exflock(fh)
                fh.write(file + '\n')
                self.__unflock(fh)
                fh.close()

        if os.path.exists(metamanifest):
            os.chmod(metamanifest, 0644)

    def __htaccess(self, ip, folder, file):
        entry = "RewriteRule ^" + file + "$  ../" + folder + "/%{REMOTE_ADDR}/" + file + " [L,NC,QSA]"
        htaccessFolder = "/var/www/html/latest"
        htaccessFile = htaccessFolder + "/.htaccess"

        CsHelper.mkdir(htaccessFolder, 0755, True)

        if os.path.exists(htaccessFile):
            fh = open(htaccessFile, "r+a")
            self.__exflock(fh)
            if not entry in fh.read():
                fh.write(entry + '\n')
            self.__unflock(fh)
            fh.close()
        else:
            fh = open(htaccessFile, "w")
            self.__exflock(fh)
            fh.write("Options +FollowSymLinks\nRewriteEngine On\n\n")
            fh.write(entry + '\n')
            self.__unflock(fh)
            fh.close()

        entry="Options -Indexes\nOrder Deny,Allow\nDeny from all\nAllow from " + ip
        htaccessFolder = "/var/www/html/" + folder + "/" + ip
        htaccessFile = htaccessFolder+"/.htaccess"

        try:
            os.makedirs(htaccessFolder,0755)
        except OSError as e:
            # error 17 is already exists, we do it this way for sake of concurrency
            if e.errno != 17:
                print "failed to make directories " + htaccessFolder + " due to :" +e.strerror
                sys.exit(1)

        fh = open(htaccessFile, "w")
        self.__exflock(fh)
        fh.write(entry + '\n')
        self.__unflock(fh)
        fh.close()

        if folder == "metadata" or folder == "meta-data":
            entry = "RewriteRule ^meta-data/(.+)$  ../" + folder + "/%{REMOTE_ADDR}/$1 [L,NC,QSA]"
            htaccessFolder = "/var/www/html/latest"
            htaccessFile = htaccessFolder + "/.htaccess"

            fh = open(htaccessFile, "r+a")
            self.__exflock(fh)
            if not entry in fh.read():
                fh.write(entry + '\n')

            entry = "RewriteRule ^meta-data/$  ../" + folder + "/%{REMOTE_ADDR}/meta-data [L,NC,QSA]"

            fh.seek(0)
            if not entry in fh.read():
                fh.write(entry + '\n')
            self.__unflock(fh)
            fh.close()

    def __exflock(self, file):
        try:
            flock(file, LOCK_EX)
        except IOError as e:
            print "failed to lock file" + file.name + " due to : " + e.strerror
            sys.exit(1) #FIXME
        return True

    def __unflock(self, file):
        try:
            flock(file, LOCK_UN)
        except IOError:
            print "failed to unlock file" + file.name + " due to : " + e.strerror
            sys.exit(1) #FIXME
        return True

class CsAddress(CsDataBag):

    def compare(self):
        for dev in CsDevice('').list():
            ip = CsIP(dev)
            ip.compare(self.dbag)

    def get_ips(self):
        ret = []
        for dev in self.dbag:
            if dev == "id":
                continue
            for ip in self.dbag[dev]:
                ret.append(CsInterface(ip))
        return ret

    def needs_vrrp(self,o):
        """
        Returns if the ip needs to be managed by keepalived or not
        """
        if "nw_type" in o and o['nw_type'] in [ 'guest' ]:
            return True
        return False

    def get_control_if(self):
        """
        Return the address object that has the control interface
        """
        for ip in self.get_ips():
            if ip.is_control():
                return ip
        return None

    def process(self):
        for dev in self.dbag:
            if dev == "id":
                continue
            ip = CsIP(dev)
            addcnt = 0
            for address in self.dbag[dev]:
                if not address["nw_type"] == "control":
                    CsRoute(dev).add(address)
                ip.setAddress(address)
                if ip.configured():
                    logging.info("Address %s on device %s already configured", ip.ip(), dev)
                    ip.post_configure()
                else:
                    logging.info("Address %s on device %s not configured", ip.ip(), dev)
                    if CsDevice(dev).waitfordevice():
                        ip.configure()
                # This could go one level up but the ip type is stored in the 
                # ip address object and not in the device object
                # Call only once
                if addcnt == 0:
                    self.add_netstats(address)
                addcnt += 1

    def add_netstats(self, address):
        # add in the network stats iptables rules
        dev = "eth%s" % address['nic_dev_id']
        if address["nw_type"] == "public_ip":
            fw.append(["", "front", "-A FORWARD -j NETWORK_STATS"])
            fw.append(["", "front", "-A INPUT -j NETWORK_STATS"])
            fw.append(["", "front", "-A OUTPUT -j NETWORK_STATS"])
            # it is not possible to calculate these devices
            # When the vrouter and the vpc router are combined this silliness can go
            fw.append(["", "", "-A NETWORK_STATS -i %s -o eth0 -p tcp" % dev])
            fw.append(["", "", "-A NETWORK_STATS -o %s -i eth0 -p tcp" % dev])
            fw.append(["", "", "-A NETWORK_STATS -o %s ! -i eth0 -p tcp" % dev])
            fw.append(["", "", "-A NETWORK_STATS -i %s ! -o eth0 -p tcp" % dev])

        if address["nw_type"] == "guest":
            fw.append(["", "front", "-A FORWARD -j NETWORK_STATS_%s" % dev])
            fw.append(["", "front", "-A NETWORK_STATS_%s -o %s -s %s" % (dev, dev, address['network'])])
            fw.append(["", "front", "-A NETWORK_STATS_%s -o %s -d %s" % (dev, dev, address['network'])])
            # Only relevant if there is a VPN configured so will have to move
            # at some stage
            fw.append(["mangle", "", "-A FORWARD -j VPN_STATS_%s" % dev])
            fw.append(["mangle", "", "-A VPN_STATS_%s -o %s -m mark --set-xmark 0x525/0xffffffff" % (dev, dev)])
            fw.append(["mangle", "", "-A VPN_STATS_%s -i %s -m mark --set-xmark 0x524/0xffffffff" % (dev, dev)])

class CsInterface:
    """ Hold one single ip """
    def __init__(self, o):
        self.address = o

    def get_ip(self):
        return self.get_attr("public_ip")

    def get_device(self):
        return self.get_attr("device")

    def get_cidr(self):
        return self.get_attr("cidr")

    def get_broadcast(self):
        return self.get_attr("broadcast")

    def get_attr(self, attr):
        if attr in self.address:
            return self.address[attr]
        else:
            return "ERROR"

    def needs_vrrp(self):
        """
        Returns if the ip needs to be managed by keepalived or not
        """
        if "nw_type" in self.address and self.address['nw_type'] in [ 'guest' ]:
            return True
        return False

    def is_control(self):
        if "nw_type" in self.address and self.address['nw_type'] in [ 'control' ]:
            return True
        return False

    def to_str(self):
        pprint(self.address)

class CsSite2SiteVpn(CsDataBag):
    """
    Setup any configured vpns (using swan)
    left is the local machine
    right is where the clients connect from
    """

    VPNCONFDIR = "/etc/ipsec.d"

    def process(self):
        self.confips = []
        # collect a list of configured vpns
        for file in os.listdir(self.VPNCONFDIR):
            m = re.search("^ipsec.vpn-(.*).conf", file)
            if m:
                self.confips.append(m.group(1))

        for public_ip in self.dbag:
            if public_ip == "id":
                continue
            dev = CsHelper.get_device(public_ip)
            if dev == "":
                logging.error("Request for ipsec to %s not possible because ip is not configured", public_ip)
                continue
            CsHelper.start_if_stopped("ipsec")
            self.configure_iptables(dev, self.dbag[public_ip])
            self.configure_ipsec(self.dbag[public_ip])

        # Delete vpns that are no longer in the configuration
        for ip in self.confips:
            self.deletevpn(ip)

    def deletevpn(self, ip):
        logging.info("Removinf VPN configuration for %s", ip)
        CsHelper.execute("ipsec auto --down vpn-%s" % ip)
        CsHelper.execute("ipsec auto --delete vpn-%s" % ip)
        vpnconffile    = "%s/ipsec.vpn-%s.conf" % (self.VPNCONFDIR, ip)
        vpnsecretsfile = "%s/ipsec.vpn-%s.secrets" % (self.VPNCONFDIR, ip)
        os.remove(vpnconffile)
        os.remove(vpnsecretsfile)
        CsHelper.execute("ipsec auto --rereadall")

    def configure_iptables(self, dev, obj):
        fw.append([ "", "front", "-A INPUT -i %s -p udp -m udp --dport 500 -j ACCEPT" % dev ])
        fw.append([ "", "front", "-A INPUT -i %s -p udp -m udp --dport 4500 -j ACCEPT" % dev ])
        fw.append([ "", "front", "-A INPUT -i %s -p esp -j ACCEPT" % dev ])
        fw.append([ "nat", "front", "-A POSTROUTING -t nat -o %s-m mark --set-xmark 0x525/0xffffffff -j ACCEPT" % dev ])
        for net in obj['peer_guest_cidr_list'].lstrip().rstrip().split(','):
            fw.append([ "mangle", "front", "-A FORWARD -s %s -d %s -j MARK --set-xmark 0x525/0xffffffff" % (obj['local_guest_cidr'], net)])
            fw.append([ "mangle", "", "-A OUTPUT -s %s -d %s -j MARK --set-xmark 0x525/0xffffffff" % (obj['local_guest_cidr'], net)])
            fw.append([ "mangle", "front", "-A FORWARD -s %s -d %s -j MARK --set-xmark 0x524/0xffffffff" % (net, obj['local_guest_cidr'])])
            fw.append([ "mangle", "", "-A INPUT -s %s -d %s -j MARK --set-xmark 0x524/0xffffffff"  % (net, obj['local_guest_cidr']) ])

    def configure_ipsec(self, obj):
        leftpeer  = obj['local_public_ip']
        rightpeer = obj['peer_gateway_ip']
        peerlist  = obj['peer_guest_cidr_list'].lstrip().rstrip().replace(',', ' ')
        vpnconffile    = "%s/ipsec.vpn-%s.conf" % (self.VPNCONFDIR, rightpeer)
        vpnsecretsfile = "%s/ipsec.vpn-%s.secrets" % (self.VPNCONFDIR, rightpeer)
        if rightpeer in self.confips:
            self.confips.remove(rightpeer)
        file = CsFile(vpnconffile)
        file.search("conn ", "conn vpn-%s" % rightpeer)
        file.addeq(" left=%s" % leftpeer)
        file.addeq(" leftsubnet=%s" % obj['local_guest_cidr'])
        file.addeq(" leftnexthop=%s" % obj['local_public_gateway'])
        file.addeq(" right=%s" % rightpeer)
        file.addeq(" rightsubnets=%s" % peerlist)
        file.addeq(" type=tunnel")
        file.addeq(" authby=secret")
        file.addeq(" keyexchange=ike")
        file.addeq(" ike=%s" % obj['ike_policy'])
        file.addeq(" ikelifetime=%s" % self.convert_sec_to_h(obj['ike_lifetime']))
        file.addeq(" esp=%s" % self.convert_sec_to_h(obj['esp_lifetime']))
        file.addeq(" salifetime=%s" % self.convert_sec_to_h(obj['esp_lifetime']))
        file.addeq(" pfs=%s" % CsHelper.bool_to_yn(obj['dpd']))
        file.addeq(" keyingtries=2")
        file.addeq(" auto=add")
        if obj['dpd']:
            file.addeq("  dpddelay=30")
            file.addeq("  dpdtimeout=120")
            file.addeq("  dpdaction=restart")
        file.commit()
        secret = CsFile(vpnsecretsfile)
        secret.search("%s " % leftpeer, "%s %s: PSK \"%s\"" % (leftpeer, rightpeer, obj['ipsec_psk']))
        secret.commit()
        if secret.is_changed() or file.is_changed():
            logging.info("Configured vpn %s %s", leftpeer, rightpeer)
            CsHelper.execute("ipsec auto --rereadall")
            CsHelper.execute("ipsec --add vpn-%s" % rightpeer)
            if not obj['passive']:
                CsHelper.execute("ipsec --up vpn-%s" % rightpeer)
        os.chmod(vpnsecretsfile, 0o400)

    def convert_sec_to_h(self, val):
        hrs = int(val) / 3600
        return "%sh" % hrs

                
class CsForwardingRules(CsDataBag):
    def __init__(self, key):
        super(CsForwardingRules, self).__init__(key)
        global fw
    
    def process(self):
        for public_ip in self.dbag:
            if public_ip == "id":
                continue
            for rule in self.dbag[public_ip]:
                if rule["type"] == "forward":
                    self.processForwardRule(rule)
                elif rule["type"] == "staticnat":
                    self.processStaticNatRule(rule)

    def getDeviceByIp(self, ip):
        ips = CsDataBag("ips")
        dbag = ips.get_bag()
        for device in dbag:
            if device == "id":
                continue
            for addy in dbag[device]:
                if addy["public_ip"] == ip:
                    return device
        return None
                    
    def portsToString(self, ports, delimiter):
        ports_parts = ports.split(":", 2)
        if ports_parts[0] == ports_parts[1]:
            return str(ports_parts[0])
        else:
            return "%s%s%s" % (port_parts, delimiter, port_parts[1])


    def processForwardRule(self, rule):
        # FIXME this seems to be different for regular VRs?
        fwrule = "-A PREROUTING -d %s/32" % rule["public_ip"]
        if not rule["protocol"] == "any":
            fwrule += " -m %s -p %s" % (rule["protocol"], rule["protocol"])
        if not rule["public_ports"] == "any":
            fwrule += " --dport %s" % self.portsToString(rule["public_ports"], ":")
        fwrule += " -j DNAT --to-destination %s" % rule["internal_ip"]
        if not rule["internal_ports"] == "any":
            fwrule += ":" + self.portsToString(rule["internal_ports"], "-")
        fw.append(["nat","",fwrule])
        

    def processStaticNatRule(self, rule):
        # FIXME this needs ordering with the VPN no nat rule
        device = self.getDeviceByIp(rule["public_ip"])
        if device == None:
            raise Exception("Ip address %s has no device in the ips databag" % rule["public_ip"])
        fw.append(["nat","front","-A PREROUTING -d %s/32 -j DNAT --to-destination %s" % ( rule["public_ip"], rule["internal_ip"]) ])
        fw.append(["nat","front","-A POSTROUTING -o %s -s %s/32 -j SNAT --to-source %s" % ( device, rule["internal_ip"], rule["public_ip"]) ])

def main(argv):

    logging.basicConfig(filename='/var/log/cloud.log',
                        level=logging.DEBUG,
                        format='%(asctime)s %(message)s')
 
    cl = CsCmdLine("cmdline")

    address = CsAddress("ips")
    address.compare()
    address.process()

    password = CsPassword("vmpassword")
    password.process()

    metadata = CsVmMetadata('vmdata')
    metadata.process()

    acls = CsAcl('networkacl')
    acls.process()

    fwd = CsForwardingRules("forwardingrules")
    fwd.process()

    vpns = CsSite2SiteVpn("site2sitevpn")
    vpns.process()

    red = CsRedundant()
# Move to init and make a single call?
    red.set(cl, address)

    nf = CsNetfilters()
    nf.compare(fw)

    dh = CsDataBag("dhcpentry")
    dhcp = CsDhcp(dh.get_bag(), cl)



if __name__ == "__main__":
    main(sys.argv)
