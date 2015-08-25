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
from netaddr import IPAddress, IPNetwork
import CsHelper

import subprocess
import time
from CsRoute import CsRoute
from CsRule import CsRule

VRRP_TYPES = ['guest']


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
                ret.append(CsInterface(ip, self.config))
        return ret

    def get_guest_if(self):
        """
        Return CsInterface object for the lowest guest interface
        """
        ipr = []
        for ip in self.get_ips():
            if ip.is_guest():
                ipr.append(ip)
        if len(ipr) > 0:
            return sorted(ipr)[-1]
        return None

    def get_guest_ip(self):
        """
        Return the ip of the first guest interface
        For use with routers not vpcrouters
        """
        ip = self.get_guest_if()
        if ip:
            return ip.get_ip()
        return None

    def get_guest_netmask(self):
        """
        Return the netmask of the first guest interface
        For use with routers not vpcrouters
        """
        ip = self.get_guest_if()
        if ip:
            return ip.get_netmask()
        return "255.255.255.0"

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
        route = CsRoute()
        found_defaultroute = False

        for dev in self.dbag:
            if dev == "id":
                continue
            ip = CsIP(dev, self.config)

            for address in self.dbag[dev]:

                gateway = str(address["gateway"])
                network = str(address["network"])

                ip.setAddress(address)

                if ip.configured():
                    logging.info(
                        "Address %s on device %s already configured", ip.ip(), dev)

                    ip.post_configure()

                else:
                    logging.info(
                        "Address %s on device %s not configured", ip.ip(), dev)
                    if CsDevice(dev, self.config).waitfordevice():
                        ip.configure()

                if address["nw_type"] != "control":
                    route.add_route(dev, network)

                # once we start processing public ip's we need to verify there
                # is a default route and add if needed
                if address["nw_type"] == "public" and not found_defaultroute:
                    if not route.defaultroute_exists():
                        if route.add_defaultroute(gateway):
                            found_defaultroute = True


class CsInterface:

    """ Hold one single ip """

    def __init__(self, o, config):
        self.address = o
        self.config = config

    def get_ip(self):
        return self.get_attr("public_ip")

    def get_network(self):
        return self.get_attr("network")

    def get_netmask(self):
        return self.get_attr("netmask")

    def get_gateway(self):
        if self.config.is_vpc():
            return self.get_attr("gateway")
        else:
            return self.config.cmdline().get_guest_gw()

    def ip_in_subnet(self, ip):
        ipo = IPAddress(ip)
        net = IPNetwork("%s/%s" % (self.get_ip(), self.get_size()))
        return ipo in list(net)

    def get_gateway_cidr(self):
        return "%s/%s" % (self.get_gateway(), self.get_size())

    def get_size(self):
        """ Return the network size in bits (24, 16, 8 etc) """
        return self.get_attr("size")

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

    def is_guest(self):
        if "nw_type" in self.address and self.address['nw_type'] in ['guest']:
            return True
        return False

    def is_public(self):
        if "nw_type" in self.address and self.address['nw_type'] in ['public']:
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
            self.tableNo = dev[3:]
            self.table = "Table_%s" % dev
        self.fw = config.get_fw()
        self.cl = config.cmdline()

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
        logging.error(
            "Device %s cannot be configured - device was not found", self.dev)
        return False

    def list(self):
        return self.devlist


class CsIP:

    def __init__(self, dev, config):
        self.dev = dev
        self.dnum = hex(int(dev[3:]))
        self.iplist = {}
        self.address = {}
        self.list()
        self.fw = config.get_fw()
        self.cl = config.cmdline()
        self.config = config

    def setAddress(self, address):
        self.address = address

    def getAddress(self):
        return self.address

    def configure(self):
        logging.info(
            "Configuring address %s on device %s", self.ip(), self.dev)
        cmd = "ip addr add dev %s %s brd +" % (self.dev, self.ip())
        subprocess.call(cmd, shell=True)
        self.post_configure()

    def post_configure(self):
        """ The steps that must be done after a device is configured """
        if not self.get_type() in ["control"]:
            route = CsRoute()
            route.add_table(self.dev)
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
                # If redundant do not bring up public interfaces
                # master.py and keepalived deal with tham
                if self.config.cmdline().is_redundant() and not self.is_public():
                    CsHelper.execute(cmd2)
                # if not redundant bring everything up
                if not self.config.cmdline().is_redundant():
                    CsHelper.execute(cmd2)

    def set_mark(self):
        cmd = "-A PREROUTING -i %s -m state --state NEW -j CONNMARK --set-xmark %s/0xffffffff" % \
            (self.getDevice(), self.dnum)
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
                            "-d %s/32 -j VPN_%s" % (self.address['public_ip'], self.address['public_ip'])])
            self.fw.append(["mangle", "front",
                            "-A PREROUTING " +
                            "-d %s/32 -j FIREWALL_%s" % (self.address['public_ip'], self.address['public_ip'])])
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
            self.fw.append(["mangle", "",
                            "-A PREROUTING -i %s -m state --state NEW " % self.dev +
                            "-j CONNMARK --set-xmark %s/0xffffffff" % self.dnum])
            self.fw.append(
                ["mangle", "", "-A FIREWALL_%s -j DROP" % self.address['public_ip']])

        self.fw.append(["filter", "", "-A INPUT -d 224.0.0.18/32 -j ACCEPT"])
        self.fw.append(["filter", "", "-A INPUT -d 225.0.0.50/32 -j ACCEPT"])
        self.fw.append(["filter", "", "-A INPUT -i %s -m state --state RELATED,ESTABLISHED -j ACCEPT" %
                        self.dev])
        self.fw.append(["filter", "", "-A INPUT -p icmp -j ACCEPT"])
        self.fw.append(["filter", "", "-A INPUT -i lo -j ACCEPT"])

        if self.get_type() in ["guest"]:
            self.fw.append(
                ["filter", "", "-A INPUT -i %s -p udp -m udp --dport 67 -j ACCEPT" % self.dev])
            self.fw.append(
                ["filter", "", "-A INPUT -i %s -p udp -m udp --dport 53 -j ACCEPT" % self.dev])
            self.fw.append(
                ["filter", "", "-A INPUT -i %s -p tcp -m tcp --dport 53 -j ACCEPT" % self.dev])
            self.fw.append(
                ["filter", "", "-A INPUT -i %s -p tcp -m tcp --dport 80 -m state --state NEW -j ACCEPT" % self.dev])
            self.fw.append(
                ["filter", "", "-A INPUT -i %s -p tcp -m tcp --dport 8080 -m state --state NEW -j ACCEPT" % self.dev])
            self.fw.append(
                ["filter", "", "-A FORWARD -i %s -o eth1 -m state --state RELATED,ESTABLISHED -j ACCEPT" % self.dev])
            self.fw.append(
                ["filter", "", "-A FORWARD -i %s -o %s -m state --state NEW -j ACCEPT" % (self.dev, self.dev)])
            self.fw.append(
                ["filter", "", "-A FORWARD -i eth2 -o eth0 -m state --state RELATED,ESTABLISHED -j ACCEPT"])
            self.fw.append(
                ["filter", "", "-A FORWARD -i eth0 -o eth0 -m state --state RELATED,ESTABLISHED -j ACCEPT"])
            self.fw.append(
                ["filter", "", "-A FORWARD -i eth0 -o eth2 -j FW_OUTBOUND"])
            self.fw.append(["mangle", "",
                            "-A PREROUTING -i %s -m state --state NEW " % self.dev +
                            "-j CONNMARK --set-xmark %s/0xffffffff" % self.dnum])

        if self.get_type() in ["control"]:
            self.fw.append(
                ["filter", "", "-A FW_OUTBOUND -m state --state RELATED,ESTABLISHED -j ACCEPT"])
            self.fw.append(
                ["filter", "", "-A INPUT -i %s -p tcp -m tcp --dport 3922 -m state --state NEW -j ACCEPT" % self.dev])
        self.fw.append(['', 'front', '-A FORWARD -j NETWORK_STATS'])
        self.fw.append(['', 'front', '-A INPUT -j NETWORK_STATS'])
        self.fw.append(['', 'front', '-A OUTPUT -j NETWORK_STATS'])
        self.fw.append(['', '', '-A NETWORK_STATS -i eth0 -o eth2'])
        self.fw.append(['', '', '-A NETWORK_STATS -i eth2 -o eth0'])
        self.fw.append(['', '', '-A NETWORK_STATS -o eth2 ! -i eth0 -p tcp'])
        self.fw.append(['', '', '-A NETWORK_STATS -i eth2 ! -o eth0 -p tcp'])

    def fw_vpcrouter(self):
        if not self.config.is_vpc():
            return
        self.fw.append(["mangle", "front", "-A PREROUTING " +
                        "-m state --state RELATED,ESTABLISHED " +
                        "-j CONNMARK --restore-mark --nfmask 0xffffffff --ctmask 0xffffffff"])
        if self.get_type() in ["guest"]:
            self.fw.append(["filter", "", "-A FORWARD -d %s -o %s -j ACL_INBOUND_%s" %
                            (self.address['network'], self.dev, self.dev)])
            self.fw.append(
                ["filter", "front", "-A ACL_INBOUND_%s -d 224.0.0.18/32 -j ACCEPT" % self.dev])
            self.fw.append(
                ["filter", "front", "-A ACL_INBOUND_%s -d 225.0.0.50/32 -j ACCEPT" % self.dev])
            self.fw.append(
                ["mangle", "front", "-A ACL_OUTBOUND_%s -d 225.0.0.50/32 -j ACCEPT" % self.dev])
            self.fw.append(
                ["mangle", "front", "-A ACL_OUTBOUND_%s -d 224.0.0.18/32 -j ACCEPT" % self.dev])
            self.fw.append(
                ["filter", "", "-A INPUT -i %s -p udp -m udp --dport 67 -j ACCEPT" % self.dev])
            self.fw.append(
                ["filter", "", "-A INPUT -i %s -p udp -m udp --dport 53 -j ACCEPT" % self.dev])
            self.fw.append(
                ["filter", "", "-A INPUT -i %s -p tcp -m tcp --dport 53 -j ACCEPT" % self.dev])

            self.fw.append(
                ["filter", "", "-A INPUT -i %s -p tcp -m tcp --dport 80 -m state --state NEW -j ACCEPT" % self.dev])
            self.fw.append(
                ["filter", "", "-A INPUT -i %s -p tcp -m tcp --dport 8080 -m state --state NEW -j ACCEPT" % self.dev])
            self.fw.append(["mangle", "",
                            "-A PREROUTING -m state --state NEW -i %s -s %s ! -d %s/32 -j ACL_OUTBOUND_%s" %
                            (self.dev, self.address[
                             'network'], self.address['gateway'], self.dev)
                            ])
            self.fw.append(["", "front", "-A NETWORK_STATS_%s -o %s -s %s" %
                            ("eth1", "eth1", self.address['network'])])
            self.fw.append(["", "front", "-A NETWORK_STATS_%s -o %s -d %s" %
                            ("eth1", "eth1", self.address['network'])])
            self.fw.append(["nat", "front",
                            "-A POSTROUTING -s %s -o %s -j SNAT --to-source %s" %
                            (self.address['network'], self.dev,
                             self.address['public_ip'])
                            ])

        if self.get_type() in ["public"]:
            self.fw.append(["", "front",
                            "-A FORWARD -o %s -d %s -j ACL_INBOUND_%s" % (
                                self.dev, self.address['network'], self.dev)
                            ])
            self.fw.append(
                ["mangle", "", "-A FORWARD -j VPN_STATS_%s" % self.dev])
            self.fw.append(
                ["mangle", "", "-A VPN_STATS_%s -o %s -m mark --mark 0x525/0xffffffff" % (self.dev, self.dev)])
            self.fw.append(
                ["mangle", "", "-A VPN_STATS_%s -i %s -m mark --mark 0x524/0xffffffff" % (self.dev, self.dev)])
            self.fw.append(
                ["", "front", "-A FORWARD -j NETWORK_STATS_%s" % self.dev])

        self.fw.append(["", "front", "-A FORWARD -j NETWORK_STATS"])
        self.fw.append(["", "front", "-A INPUT -j NETWORK_STATS"])
        self.fw.append(["", "front", "-A OUTPUT -j NETWORK_STATS"])

        self.fw.append(["", "", "-A NETWORK_STATS -i eth0 -o eth2 -p tcp"])
        self.fw.append(["", "", "-A NETWORK_STATS -i eth2 -o eth0 -p tcp"])
        self.fw.append(["", "", "-A NETWORK_STATS ! -i eth0 -o eth2 -p tcp"])
        self.fw.append(["", "", "-A NETWORK_STATS -i eth2 ! -o eth0 -p tcp"])

    def post_config_change(self, method):
        route = CsRoute()
        if method == "add":
            route.add_table(self.dev)
            route.add_route(self.dev, str(self.address["network"]))
        elif method == "delete":
            logging.warn("delete route not implemented")

        self.fw_router()
        self.fw_vpcrouter()
        # On deletion nw_type will no longer be known
        if self.get_type() in ["guest"] and self.config.is_vpc():

            CsDevice(self.dev, self.config).configure_rp()

            logging.error(
                "Not able to setup sourcenat for a regular router yet")
            dns = CsDnsmasq(self)
            dns.add_firewall_rules()
            app = CsApache(self)
            app.setup()

        # If redundant then this is dealt with by the master backup functions
        if self.get_type() in ["guest"] and not self.config.cl.is_redundant():
            pwdsvc = CsPasswdSvc(self.address['public_ip']).start()

        if self.get_type() == "public" and self.config.is_vpc():
            if self.address["source_nat"]:
                vpccidr = self.config.cmdline().get_vpccidr()
                self.fw.append(
                    ["filter", "", "-A FORWARD -s %s ! -d %s -j ACCEPT" % (vpccidr, vpccidr)])
                self.fw.append(
                    ["nat", "", "-A POSTROUTING -j SNAT -o %s --to-source %s" % (self.dev, self.address['public_ip'])])

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

    def is_public(self):
        if "nw_type" in self.address and self.address['nw_type'] in ['public']:
            return True
        return False

    def ip(self):
        return str(self.address['cidr'])

    def getDevice(self):
        return self.dev

    def hasIP(self, ip):
        return ip in self.address.values()

    def arpPing(self):
        cmd = "arping -c 1 -I %s -A -U -s %s %s" % (
            self.dev, self.address['public_ip'], self.address['public_ip'])
        CsHelper.execute(cmd)

    # Delete any ips that are configured but not in the bag
    def compare(self, bag):
        if len(self.iplist) > 0 and (self.dev not in bag.keys() or len(bag[self.dev]) == 0):
            # Remove all IPs on this device
            logging.info(
                "Will remove all configured addresses on device %s", self.dev)
            self.delete("all")
            app = CsApache(self)
            app.remove()

        # This condition should not really happen but did :)
        # It means an apache file got orphaned after a guest network address
        # was deleted
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
                    if self.is_guest_gateway(address, ip):
                        found = True
            if not found:
                self.delete(ip)

    def is_guest_gateway(self, bag, ip):
        """ Exclude the vrrp maintained addresses on a redundant router """
        interface = CsInterface(bag, self.config)
        if not self.config.cl.is_redundant():
            return False
        rip = ip.split('/')[0]
        gw = interface.get_gateway()
        if bag['nw_type'] == "guest" and rip == gw:
            return True
        return False

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
        CsHelper.updatefile(
            "/proc/sys/net/core/rps_sock_flow_entries", "256", "w+")
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
