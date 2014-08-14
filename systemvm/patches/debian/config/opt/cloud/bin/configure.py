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
from cs_ip import merge
import CsHelper
import CsNetfilter

fw = []

class CsFile:
    """ File editors """

    def __init__(self, filename):
        self.filename = filename
        self.changed  = False
        self.load()

    def load(self):
        self.new_config = []
        try:
            for line in open(self.filename):
                self.new_config.append(line)
        except IOError:
            logging.debug("File %s does not exist" % self.filename)
            return
        else:
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

class CsPassword(object):
    """
      Update the password cache

      A stupid step really as we should just rewrite the password server to
      use the databag
    """
    cache = "/var/cache/cloud/passwords"

    def __init__(self):
        db = dataBag()
        db.setKey("vmpassword")
        db.load()
        dbag = db.getDataBag()
        file = CsFile(self.cache)
        for item in dbag:
            if item == "id":
                continue
            self.update(file, item, dbag[item])
        file.commit()

    def update(self, file, ip, password):
        file.search("%s=" % ip, "%s=%s" % (ip, password))

class CsApp:
    def __init__(self, ip):
        self.dev     = ip.getDevice()
        self.ip      = ip.get_ip_address()
        self.domain  = "domain.local"
        self.type    = ip.get_type()
        if self.type == "guest":
            gn = CsGuestNetwork(self.dev)
            self.domain = gn.get_domain()
        global fw

class CsPasswdSvc(CsApp):
    """
      nohup bash /opt/cloud/bin/vpc_passwd_server $ip >/dev/null 2>&1 &
    """

    def setup(self):
        fw.append(["", "front",
            "-A INPUT -i %s -d %s -p tcp -m tcp --state NEW --dport 8080 -j ACCEPT" % (self.dev, self.ip)
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
            "-A INPUT -i %s -d %s -p tcp -m state --state NEW --dport 80 -j ACCEPT" % (self.dev, self.ip)
        ])

class CsDnsmasq(CsApp):
    """ Set up dnsmasq """

    def add_firewall_rules(self):
        """ Add the necessary firewall rules 
        """
        fw.append(["", "front"
            "-A INPUT -i %s -p udp -m udp --dport 67 -j ACCEPT" % self.dev
        ])

        fw.append(["", "front"
            "-A INPUT -i %s -d %s -p udp -m udp --dport 53 -j ACCEPT" % (self.dev, self.ip)
        ])

        fw.append(["", "front"
            "-A INPUT -i %s -d %s -p tcp -m tcp --dport 53 -j ACCEPT" % ( self.dev, self.ip )
        ])

    def configure_server(self, method = "add"):
        file = CsFile("/etc/dnsmasq.d/cloud.conf")
        file.search("dhcp-range=interface:%s" % self.dev, \
                    "dhcp-range=interface:%s,set:interface-%s,%s,static" % (self.dev, self.dev, self.ip))
        file.search("dhcp-option=tag:interface-%s," % self.dev, \
                    "dhcp-option=tag:interface-%s,15,%s" % (self.dev, self.domain))
        file.commit()

        if file.is_changed():
            CsHelper.service("dnsmasq", "restart")


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
            if len(dbag[dev]) == 0:
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
        cmd = "-A PREROUTING -i %s -m state --state NEW -j CONNMARK --set-mark 0x%s" % \
        (self.dev, "Table_%s" % self.tableNo)
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

            fw.append(["nat", "", 
            "-A POSTROUTING -s %s -o %s -j SNAT --to-source %s" % \
            (self.address['network'], self.dev, self.address['public_ip'])
            ])
            fw.append(["", "", "-N %s" % devChain ])
            fw.append(["mangle", "", "-N %s" % devChain ])
            fw.append(["mangle", "", "-A %s -j ACCEPT" % devChain])

            fw.append(["", "", 
            "-A FORWARD -o %s -d %s -j %s" % (self.dev, self.address['network'], devChain)
            ])
            fw.append(["", "", "-A DROP -j %s" % devChain])
            fw.append(["mangle", "", 
               "-A PREROUTING -m state --state NEW -i %s -s %s ! -d %s -j %s" % \
               (self.dev, self.address['network'], self.address['public_ip'], devChain)
            ])
            dns = CsDnsmasq(self)
            dns.add_firewall_rules()
            dns.configure_server()
            app = CsApache(self)
            app.setup()
            pwdsvc = CsPasswdSvc(self).setup()

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


class CsVmMetadata():
    def __init__(self):
        self.data = {}
        db = dataBag()
        db.setKey("vmdata")
        db.load()
        self.dbag = db.getDataBag()

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

        try:
            os.mkdir(htaccessFolder,0755)
        except OSError as e:
            # error 17 is already exists, we do it this way for concurrency
            if e.errno != 17:
                print "failed to make directories " + htaccessFolder + " due to :" +e.strerror
                sys.exit(1)

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
    CsPassword()
    pprint(fw)

    metadata = CsVmMetadata()
    metadata.process()

if __name__ == "__main__":
    main(sys.argv)
