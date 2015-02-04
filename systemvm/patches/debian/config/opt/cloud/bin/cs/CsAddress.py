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
from CsDatabag import CsDataBag, CsCmdLine
from CsApp import CsApache, CsDnsmasq, CsPasswdSvc
import CsHelper
import logging
import CsHelper
import subprocess
from CsRoute import CsRoute
from CsRule import CsRule

VRRP_TYPES = ['guest', 'public']


class CsAddress(CsDataBag):

    def compare(self):
        for dev in CsDevice('', self.config).list():
            ip = CsIP(dev, self.config)
            ip.compare(self.dbag)

    def get_ips(self):
        ret = []
        for dev in self.dbag:
            if dev == "id":
                continue
            for ip in self.dbag[dev]:
                ret.append(CsInterface(ip))
        return ret

    def needs_vrrp(self, o):
        """
        Returns if the ip needs to be managed by keepalived or not
        """
        if "nw_type" in o and o['nw_type'] in VRRP_TYPES:
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
            ip = CsIP(dev, self.config)
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
                    if CsDevice(dev, self.config).waitfordevice():
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
        if address["nw_type"] == "public":
            self.fw.append(["", "front", "-A FORWARD -j NETWORK_STATS"])
            self.fw.append(["", "front", "-A INPUT -j NETWORK_STATS"])
            self.fw.append(["", "front", "-A OUTPUT -j NETWORK_STATS"])
            # it is not possible to calculate these devices
            # When the vrouter and the vpc router are combined this silliness can go
            self.fw.append(["", "", "-A NETWORK_STATS -i %s -o eth0 -p tcp" % dev])
            self.fw.append(["", "", "-A NETWORK_STATS -o %s -i eth0 -p tcp" % dev])
            self.fw.append(["", "", "-A NETWORK_STATS -o %s ! -i eth0 -p tcp" % dev])
            self.fw.append(["", "", "-A NETWORK_STATS -i %s ! -o eth0 -p tcp" % dev])

        # Netstats per interface only used on VPC
        if address["nw_type"] == "guest" and self.config.is_vpc():
            self.fw.append(["", "front", "-A FORWARD -j NETWORK_STATS_%s" % dev])
            self.fw.append(["", "front", "-A NETWORK_STATS_%s -o %s -s %s" % (dev, dev, address['network'])])
            self.fw.append(["", "front", "-A NETWORK_STATS_%s -o %s -d %s" % (dev, dev, address['network'])])
            # Only relevant if there is a VPN configured so will have to move
            # at some stage
            self.fw.append(["mangle", "", "-A FORWARD -j VPN_STATS_%s" % dev])
            self.fw.append(["mangle", "", "-A VPN_STATS_%s -o %s -m mark --set-xmark 0x525/0xffffffff" % (dev, dev)])
            self.fw.append(["mangle", "", "-A VPN_STATS_%s -i %s -m mark --set-xmark 0x524/0xffffffff" % (dev, dev)])


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
        if "nw_type" in self.address and self.address['nw_type'] in VRRP_TYPES:
            return True
        return False

    def is_control(self):
        if "nw_type" in self.address and self.address['nw_type'] in ['control']:
            return True
        return False

    def to_str(self):
        pprint(self.address)


class CsDevice:
    """ Configure Network Devices """
    def __init__(self, dev, config):
        self.devlist = []
        self.dev = dev
        self.buildlist()
        self.table = ''
        self.tableNo = ''
        if dev != '':
            self.tableNo = dev[3]
            self.table = "Table_%s" % dev
        self.fw = config.get_fw()
        self.cl = config.get_cmdline()

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
            self.buildlist()
        logging.error("Device %s cannot be configured - device was not found", self.dev)
        return False

    def list(self):
        return self.devlist


class CsIP:

    def __init__(self, dev, config):
        self.dev = dev
        self.iplist = {}
        self.address = {}
        self.list()
        self.fw = config.get_fw()
        self.cl = config.get_cmdline()
        self.config = config

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
        if not self.get_type() in ["control"]:
            route = CsRoute(self.dev)
            route.routeTable()
            CsRule(self.dev).addMark()
            self.check_is_up()
            self.set_mark()
            self.arpPing()
            CsRpsrfs(self.dev).enable()
            self.post_config_change("add")

    def check_is_up(self):
        """ Ensure device is up """
        cmd = "ip link show %s | grep 'state DOWN'" % self.getDevice()
        for i in CsHelper.execute(cmd):
            if " DOWN " in i:
                cmd2 = "ip link set %s up" % self.getDevice()
                # Do not change the state of ips on a redundant router that are managed by vrrp or CsRedundant
                if self.cl.is_redundant() and self.needs_vrrp():
                    pass
                else:
                    CsHelper.execute(cmd2)

    def set_mark(self):
        cmd = "-A PREROUTING -i %s -m state --state NEW -j CONNMARK --set-xmark 0x%s/0xffffffff" % \
              (self.getDevice(), self.getDevice()[3])
        self.fw.append(["mangle", "", cmd])

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

    def fw_router(self):
        if self.config.is_vpc():
            return
        self.fw.append(["mangle", "front", "-A PREROUTING " +
                        "-m state --state RELATED,ESTABLISHED " +
                        "-j CONNMARK --restore-mark --nfmask 0xffffffff --ctmask 0xffffffff"])
        if self.get_type() in ["public"]:
            self.fw.append(["mangle", "front",
                            "-A PREROUTING " +
                            "-d %s -j VPN_%s" % (self.address['cidr'], self.address['public_ip'])])
            self.fw.append(["mangle", "front",
                            "-A PREROUTING " +
                            "-d %s -j FIREWALL_%s" % (self.address['cidr'], self.address['public_ip'])])
            self.fw.append(["mangle", "front",
                            "-A FIREWALL_%s " % self.address['public_ip'] +
                            "-m state --state RELATED,ESTABLISHED -j ACCEPT"])
            self.fw.append(["mangle", "",
                            "-A FIREWALL_%s DROP" % self.address['public_ip']])
            self.fw.append(["mangle", "",
                            "-A VPN_%s -m state --state RELATED,ESTABLISHED -j ACCEPT" % self.address['public_ip']])
            self.fw.append(["mangle", "",
                            "-A VPN_%s -j RETURN" % self.address['public_ip']])
            self.fw.append(["mangle", "front",
                            "-A POSTROUTING " +
                            "-p udp -m udp --dport 68 -j CHECKSUM --checksum-fill"])
            self.fw.append(["nat", "",
                            "-A POSTROUTING -o eth2 -j SNAT --to-source %s" % self.address['public_ip']])

        self.fw.append(["filter", "", "-A INPUT -d 224.0.0.18/32 -j ACCEPT"])
        self.fw.append(["filter", "", "-A INPUT -d 225.0.0.50/32 -j ACCEPT"])
        self.fw.append(["filter", "", "-A INPUT -i %s -m state --state RELATED,ESTABLISHED -j ACCEPT" %
                       self.dev])
        self.fw.append(["filter", "", "-A INPUT -p icmp -j ACCEPT"])
        self.fw.append(["filter", "", "-A INPUT -i lo -j ACCEPT"])

        if self.get_type() in ["guest"]:
            self.fw.append(["filter", "", "-A INPUT -i %s -p udp -m udp --dport 67 -j ACCEPT" % self.dev])
            self.fw.append(["filter", "", "-A INPUT -i %s -p udp -m udp --dport 53 -j ACCEPT" % self.dev])
            self.fw.append(["filter", "", "-A INPUT -i %s -p tcp -m tcp --dport 53 -j ACCEPT" % self.dev])
            self.fw.append(["filter", "",
                            "-A INPUT -s %s -i %s -p tcp -m state --state NEW -m tcp --dport 8080 -j ACCEPT" % (self.address['network'], self.dev)])
            self.fw.append(["filter", "", "-A INPUT -i %s -p tcp -m tcp --dport 80 -m state --state NEW -j ACCEPT" % self.dev])
            self.fw.append(["filter", "", "-A FORWARD -i %s -o eth1 -m state --state RELATED,ESTABLISHED -j ACCEPT" % self.dev])
            self.fw.append(["filter", "", "-A FORWARD -i %s -o %s -m state --state NEW -j ACCEPT" % (self.dev, self.dev)])
            self.fw.append(["filter", "", "-A FORWARD -i eth2 -o eth0 -m state --state RELATED,ESTABLISHED -j ACCEPT"])
            self.fw.append(["filter", "", "-A FORWARD -i eth0 -o eth0 -m state --state RELATED,ESTABLISHED -j ACCEPT"])
            self.fw.append(["filter", "", "-A FORWARD -i eth0 -o eth2 -j FW_OUTBOUND"])

        if self.get_type() in ["control"]:
            self.fw.append(["filter", "", "-A FW_OUTBOUND -m state --state RELATED,ESTABLISHED -j ACCEPT"])
            self.fw.append(["filter", "", "-A INPUT -i %s -p tcp -m tcp --dport 3922 -m state --state NEW -j ACCEPT" % self.dev])

    def fw_vpcrouter(self):
        if not self.config.is_vpc():
            return
        # TODO seperate out vpc rules

    def post_config_change(self, method):
        route = CsRoute(self.dev)
        route.routeTable()
        route.add(self.address, method)
        self.fw_router()
        self.fw_vpcrouter()
        # On deletion nw_type will no longer be known
        if self.get_type() in ["guest"] and self.config.is_vpc():
            devChain = self.config.get_ingress_chain(self.dev, self.address['public_ip'])
            CsDevice(self.dev, self.config).configure_rp()

            self.fw.append(["nat", "front",
                            "-A POSTROUTING -s %s -o %s -j SNAT --to-source %s" %
                           (self.address['network'], self.dev,
                            self.address['public_ip'])
                            ])
            self.fw.append(["mangle", "front", "-A %s -j ACCEPT" % devChain])

            self.fw.append(["", "front",
                            "-A FORWARD -o %s -d %s -j %s" % (self.dev, self.address['network'], devChain)
                            ])
            self.fw.append(["", "", "-A %s -j DROP" % devChain])
            self.fw.append(["mangle", "",
                            "-A PREROUTING -m state --state NEW -i %s -s %s ! -d %s/32 -j %s" %
                            (self.dev, self.address['network'], self.address['public_ip'], devChain)
                            ])
            logging.error("Not able to setup sourcenat for a regular router yet")
            dns = CsDnsmasq(self)
            dns.add_firewall_rules()
            app = CsApache(self)
            app.setup()
            pwdsvc = CsPasswdSvc(self).setup()

        if self.get_type() == "public" and self.config.is_vpc():
            if self.address["source_nat"]:
                vpccidr = self.cl.get_vpccidr()
                self.fw.append(["filter", "", "-A FORWARD -s %s ! -d %s -j ACCEPT" % (vpccidr, vpccidr)])
                self.fw.append(["nat", "", "-A POSTROUTING -j SNAT -o %s --to-source %s" % (self.dev, self.address['public_ip'])])
        # route.flush()

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

    def needs_vrrp(self):
        """
        Returns if the ip needs to be managed by keepalived or not
        """
        if "nw_type" in self.address and self.address['nw_type'] in VRRP_TYPES:
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
        if len(self.iplist) > 0 and (self.dev not in bag.keys() or len(bag[self.dev]) == 0):
            # Remove all IPs on this device
            logging.info("Will remove all configured addresses on device %s", self.dev)
            self.delete("all")
            app = CsApache(self)
            app.remove()

        # This condition should not really happen but did :)
        # It means an apache file got orphaned after a guest network address was deleted
        if len(self.iplist) == 0 and (self.dev not in bag.keys() or len(bag[self.dev]) == 0):
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


class CsRpsrfs:
    """ Configure rpsrfs if there is more than one cpu """

    def __init__(self, dev):
        self.dev = dev

    def enable(self):
        if not self.inKernel():
            return
        cpus = self.cpus()
        if cpus < 2:
            return
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
            if "processor" not in line:
                continue
            count += 1
        if count < 2:
            logging.debug("Single CPU machine")
        return count
