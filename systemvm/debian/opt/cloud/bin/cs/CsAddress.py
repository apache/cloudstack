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
import logging
from ipaddress import *
import subprocess
import time
from . import CsHelper
from .CsDatabag import CsDataBag
from .CsApp import CsApache, CsDnsmasq, CsPasswdSvc
from .CsRoute import CsRoute
from .CsRule import CsRule

VRRP_TYPES = ['guest']


class CsAddress(CsDataBag):

    def compare(self):
        for dev in CsDevice('', self.config).list():
            ip = CsIP(dev, self.config)
            ip.compare(self.dbag)

    def get_interfaces(self):
        interfaces = []
        for dev in self.dbag:
            if dev == "id":
                continue
            for ip in self.dbag[dev]:
                interfaces.append(CsInterface(ip, self.config))
        return interfaces

    def get_guest_if(self):
        """
        Return CsInterface object for the lowest in use guest interface
        """
        guest_interface = None
        lowest_device = 1000
        for interface in self.get_interfaces():
            if interface.is_guest() and interface.is_added():
                device = interface.get_device()
                device_suffix = int(''.join([digit for digit in device if digit.isdigit()]))
                if device_suffix < lowest_device:
                    lowest_device = device_suffix
                    guest_interface = interface
                    logging.debug("Guest interface will be set on device '%s' and IP '%s'" % (guest_interface.get_device(), guest_interface.get_ip()))
        return guest_interface

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
        for interface in self.get_interfaces():
            if interface.is_control():
                return interface
        return None

    def process(self):
        for dev in self.dbag:
            if dev == "id":
                continue
            ip = CsIP(dev, self.config)

            for address in self.dbag[dev]:
                ip.setAddress(address)
                logging.info("Address found in DataBag ==> %s" % address)

                if not address['add'] and not ip.configured():
                    logging.info("Skipping %s as the add flag is set to %s " % (address['public_ip'], address['add']))
                    continue

                if ip.configured():
                    logging.info(
                        "Address %s on device %s already configured", ip.ip(), dev)

                    ip.post_configure(address)
                else:
                    logging.info(
                        "Address %s on device %s not configured", ip.ip(), dev)

                    if CsDevice(dev, self.config).waitfordevice():
                        ip.configure(address)


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
        if self.config.is_vpc() or not self.is_guest():
            return self.get_attr("gateway")
        else:
            return self.config.cmdline().get_guest_gw()

    def ip_in_subnet(self, ip):
        ipo = ip_address(ip)
        net = ip_network("%s/%s" % (self.get_ip(), self.get_size()))
        return ipo in net

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

    def is_private_gateway(self):
        if "is_private_gateway" in self.address:
            return self.address['is_private_gateway']
        return False

    def is_added(self):
        return self.get_attr("add")

    def to_str(self):
        return self.address


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

    def waitfordevice(self, timeout=2):
        count = 0
        while count < timeout:
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
        self.dnum = hex(100 + int(dev[3:]))
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

    def configure(self, address):
        # When "add" is false, it means that the IP has to be removed.
        if address["add"]:
            try:
                logging.info("Configuring address %s on device %s", self.ip(), self.dev)
                cmd = "ip addr add dev %s %s brd +" % (self.dev, self.ip())
                CsHelper.execute(cmd)
            except Exception as e:
                logging.info("Exception occurred ==> %s" % e)

            self.post_configure(address)
        else:
            # delete method performs post_configure, so no need to call post_configure here
            self.delete(self.ip())

    def post_configure(self, address):
        """ The steps that must be done after a device is configured """
        route = CsRoute()
        if not self.get_type() in ["control"]:
            route.add_table(self.dev)

            CsRule(self.dev).addMark()

            interfaces = [CsInterface(address, self.config)]
            CsHelper.reconfigure_interfaces(self.cl, interfaces)
            if self.get_type() in ['public']:
                self.set_mark()

            if 'gateway' in self.address:
                self.arpPing()

            CsRpsrfs(self.dev).enable()
            self.post_config_change("add")

        '''For isolated/redundant and dhcpsrvr routers, call this method after the post_config is complete '''
        if not self.config.is_vpc():
            self.setup_router_control()

        if self.config.is_vpc() or self.cl.is_redundant():
            # The code looks redundant here, but we actually have to cater for routers and
            # VPC routers in a different manner. Please do not remove this block otherwise
            # The VPC default route will be broken.
            if self.get_type() in ["public"] and address["device"] == CsHelper.PUBLIC_INTERFACES[self.cl.get_type()]:
                gateway = str(address["gateway"])
                route.add_defaultroute(gateway)
        else:
            # once we start processing public ip's we need to verify there
            # is a default route and add if needed
            if(self.cl.get_gateway()):
                route.add_defaultroute(self.cl.get_gateway())

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

    def setup_router_control(self):
        if self.config.is_vpc():
            return

        self.fw.append(
            ["filter", "", "-A FW_OUTBOUND -m state --state RELATED,ESTABLISHED -j ACCEPT"])
        self.fw.append(
            ["filter", "", "-A INPUT -i eth1 -p tcp -m tcp --dport 3922 -m state --state NEW,ESTABLISHED -j ACCEPT"])

        self.fw.append(["filter", "", "-P INPUT DROP"])
        self.fw.append(["filter", "", "-P FORWARD DROP"])

    def fw_router(self):
        if self.config.is_vpc():
            return

        self.fw.append(["mangle", "front", "-A PREROUTING " +
                        "-m state --state RELATED,ESTABLISHED " +
                        "-j CONNMARK --restore-mark --nfmask 0xffffffff --ctmask 0xffffffff"])

        self.fw.append(["mangle", "front",
                        "-A POSTROUTING " +
                        "-p udp -m udp --dport 68 -j CHECKSUM --checksum-fill"])

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
                            "-A FIREWALL_%s -j DROP" % self.address['public_ip']])
            self.fw.append(["mangle", "",
                            "-I VPN_%s -m state --state RELATED,ESTABLISHED -j ACCEPT" % self.address['public_ip']])
            self.fw.append(["mangle", "",
                            "-A VPN_%s -j RETURN" % self.address['public_ip']])
            self.fw.append(["nat", "",
                            "-A POSTROUTING -o %s -j SNAT --to-source %s" % (self.dev, self.cl.get_eth2_ip())])
            self.fw.append(["mangle", "",
                            "-A PREROUTING -i %s -m state --state NEW " % self.dev +
                            "-j CONNMARK --set-xmark %s/0xffffffff" % self.dnum])
            self.fw.append(["filter", "",
                            "-A FORWARD -i %s -o eth0 -m state --state RELATED,ESTABLISHED -j ACCEPT" % self.dev])
            self.fw.append(["filter", "",
                            "-A FORWARD -i eth0 -o %s -j FW_OUTBOUND" % self.dev])

        self.fw.append(["filter", "", "-A INPUT -d 224.0.0.18/32 -j ACCEPT"])
        self.fw.append(["filter", "", "-A INPUT -d 225.0.0.50/32 -j ACCEPT"])
        self.fw.append(["filter", "", "-A INPUT -i %s -m state --state RELATED,ESTABLISHED -j ACCEPT" %
                        self.dev])
        self.fw.append(["filter", "", "-A INPUT -p icmp -j ACCEPT"])
        self.fw.append(["filter", "", "-A INPUT -i lo -j ACCEPT"])

        if self.get_type() in ["guest"]:
            guestNetworkCidr = self.address['network']
            self.fw.append(
                ["filter", "", "-A INPUT -i %s -p udp -m udp --dport 67 -j ACCEPT" % self.dev])
            self.fw.append(
                ["filter", "", "-A INPUT -i %s -p udp -m udp --dport 53 -s %s -j ACCEPT" % (self.dev, guestNetworkCidr)])
            self.fw.append(
                ["filter", "", "-A INPUT -i %s -p tcp -m tcp --dport 53 -s %s -j ACCEPT" % (self.dev, guestNetworkCidr)])
            self.fw.append(
                ["filter", "", "-A INPUT -i %s -p tcp -m tcp --dport 80 -s %s -m state --state NEW -j ACCEPT" % (self.dev, guestNetworkCidr)])
            self.fw.append(
                ["filter", "", "-A INPUT -i %s -p tcp -m tcp --dport 8080 -s %s -m state --state NEW -j ACCEPT" % (self.dev, guestNetworkCidr)])
            self.fw.append(
                ["filter", "", "-A FORWARD -i %s -o eth1 -m state --state RELATED,ESTABLISHED -j ACCEPT" % self.dev])
            self.fw.append(
                ["filter", "", "-A FORWARD -i %s -o %s -m state --state NEW -j ACCEPT" % (self.dev, self.dev)])
            self.fw.append(
                ["filter", "", "-A FORWARD -i eth0 -o eth0 -m state --state RELATED,ESTABLISHED -j ACCEPT"])

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

        self.fw.append(["filter", "", "-A FORWARD -m state --state RELATED,ESTABLISHED -j ACCEPT"])

        if self.get_type() in ["guest"]:
            self.fw.append(["mangle", "front", "-A PREROUTING " +
                            " -i %s -m state --state RELATED,ESTABLISHED " % self.dev +
                            "-j CONNMARK --restore-mark --nfmask 0xffffffff --ctmask 0xffffffff"])
            guestNetworkCidr = self.address['network']
            self.fw.append(["filter", "", "-A FORWARD -d %s -o %s -j ACL_INBOUND_%s" %
                            (guestNetworkCidr, self.dev, self.dev)])
            self.fw.append(
                ["filter", "front", "-A ACL_INBOUND_%s -d 224.0.0.18/32 -j ACCEPT" % self.dev])
            self.fw.append(
                ["filter", "front", "-A ACL_INBOUND_%s -d 225.0.0.50/32 -j ACCEPT" % self.dev])
            self.fw.append(
                ["filter", "", "-A ACL_INBOUND_%s -j DROP" % self.dev])

            self.fw.append(
                ["mangle", "front", "-A ACL_OUTBOUND_%s -d 225.0.0.50/32 -j ACCEPT" % self.dev])
            self.fw.append(
                ["mangle", "front", "-A ACL_OUTBOUND_%s -d 224.0.0.18/32 -j ACCEPT" % self.dev])
            self.fw.append(
                ["filter", "", "-A INPUT -i %s -p udp -m udp --dport 67 -j ACCEPT" % self.dev])
            self.fw.append(
                ["mangle", "front", "-A POSTROUTING " + "-p udp -m udp --dport 68 -j CHECKSUM --checksum-fill"])
            self.fw.append(
                ["filter", "", "-A INPUT -i %s -p udp -m udp --dport 53 -s %s -j ACCEPT" % (self.dev, guestNetworkCidr)])
            self.fw.append(
                ["filter", "", "-A INPUT -i %s -p tcp -m tcp --dport 53 -s %s -j ACCEPT" % (self.dev, guestNetworkCidr)])

            self.fw.append(
                ["filter", "", "-A INPUT -i %s -p tcp -m tcp --dport 80 -s %s -m state --state NEW -j ACCEPT" % (self.dev, guestNetworkCidr)])
            self.fw.append(
                ["filter", "", "-A INPUT -i %s -p tcp -m tcp --dport 8080 -s %s -m state --state NEW -j ACCEPT" % (self.dev, guestNetworkCidr)])
            self.fw.append(["mangle", "",
                            "-A PREROUTING -m state --state NEW -i %s -s %s ! -d %s/32 -j ACL_OUTBOUND_%s" %
                            (self.dev, guestNetworkCidr, self.address['gateway'], self.dev)])

            self.fw.append(["", "front", "-A NETWORK_STATS_%s -i %s -d %s" %
                            ("eth1", "eth1", guestNetworkCidr)])
            self.fw.append(["", "front", "-A NETWORK_STATS_%s -o %s -s %s" %
                            ("eth1", "eth1", guestNetworkCidr)])

        if self.is_private_gateway():
            self.fw.append(["filter", "", "-A FORWARD -d %s -o %s -j ACL_INBOUND_%s" %
                            (self.address['network'], self.dev, self.dev)])
            self.fw.append(["filter", "", "-A ACL_INBOUND_%s -j DROP" % self.dev])
            self.fw.append(["mangle", "",
                            "-A PREROUTING -m state --state NEW -i %s -s %s ! -d %s/32 -j ACL_OUTBOUND_%s" %
                            (self.dev, self.address['network'], self.address['gateway'], self.dev)])
            self.fw.append(["mangle", "front",
                            "-A PREROUTING -s %s -d %s -m state --state NEW -j MARK --set-xmark %s/0xffffffff" %
                            (self.cl.get_vpccidr(), self.address['network'], hex(100 + int(self.dev[3:])))])
            if self.address["source_nat"]:
                self.fw.append(["nat", "front",
                                "-A POSTROUTING -o %s -j SNAT --to-source %s" %
                                (self.dev, self.address['public_ip'])])
            if self.get_gateway() == self.get_ip_address():
                for inf, addresses in list(self.config.address().dbag.items()):
                    if not inf.startswith("eth"):
                        continue
                    for address in addresses:
                        if "nw_type" in address and address["nw_type"] == "guest":
                            self.fw.append(["filter", "front", "-A FORWARD -s %s -d %s -j ACL_INBOUND_%s" %
                                            (address["network"], self.address["network"], self.dev)])
                            self.fw.append(["filter", "front", "-A FORWARD -s %s -d %s -j ACL_INBOUND_%s" %
                                            (self.address["network"], address["network"], address["device"])])
                # Accept packet from private gateway if VPC VR is used as gateway
                self.fw.append(["filter", "", "-A FORWARD -s %s ! -d %s -j ACCEPT" %
                                (self.address['network'], self.address['network'])])

        if self.get_type() in ["public"]:
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

        self.fw.append(["filter", "", "-A INPUT -d 224.0.0.18/32 -j ACCEPT"])
        self.fw.append(["filter", "", "-A INPUT -d 225.0.0.50/32 -j ACCEPT"])

        self.fw.append(["filter", "", "-A INPUT -p icmp -j ACCEPT"])
        self.fw.append(["filter", "", "-A INPUT -i lo -j ACCEPT"])

        self.fw.append(["filter", "", "-A INPUT -i eth0 -p tcp -m tcp --dport 3922 -m state --state NEW,ESTABLISHED -j ACCEPT"])
        self.fw.append(["filter", "", "-A INPUT -m state --state RELATED,ESTABLISHED -j ACCEPT"])

        self.fw.append(["filter", "", "-P INPUT DROP"])
        self.fw.append(["filter", "", "-P FORWARD DROP"])

    def post_config_change(self, method):
        route = CsRoute()
        tableName = "Table_" + self.dev

        if method == "add":
            if not self.config.is_vpc():
                if self.get_type() in ["public"]:
                    route.set_route("table %s throw %s proto static" % (tableName, self.config.address().dbag['eth0'][0]['network']))
                    route.set_route("table %s throw %s proto static" % (tableName, self.config.address().dbag['eth1'][0]['network']))

                # add 'default via gateway' rule in the device specific routing table
                if "gateway" in self.address and self.address["gateway"] and self.address["gateway"] != "None":
                    route.add_route(self.dev, self.address["gateway"])
                if "network" in self.address and self.address["network"]:
                    route.add_network_route(self.dev, str(self.address["network"]))

                if self.get_type() in ["public"]:
                    CsRule(self.dev).addRule("from " + str(self.address["network"]))

            if self.config.is_vpc():
                if self.get_type() in ["public"] and "gateway" in self.address and self.address["gateway"] and self.address["gateway"] != "None":
                    route.add_route(self.dev, self.address["gateway"])
                    for inf, addresses in list(self.config.address().dbag.items()):
                        if not inf.startswith("eth"):
                            continue
                        for address in addresses:
                            if "nw_type" in address and address["nw_type"] == "guest":
                                route.add_network_route(self.dev, str(address["network"]))

                route.add_network_route(self.dev, str(self.address["network"]))

            CsHelper.execute("sudo ip route flush cache")

        elif method == "delete":
            # treat the last IP to be dis-associated with interface as special case to clean up the routing rules
            if self.get_type() in ["public"] and (not self.config.is_vpc()) and (len(self.iplist) == 0):
                CsHelper.execute("sudo ip rule delete table " + tableName)
                CsHelper.execute("sudo ip route flush table " + tableName)
                CsHelper.execute("sudo ip route flush cache")
                CsRule(self.dev).delMark()

        self.fw_router()
        self.fw_vpcrouter()

        cmdline = self.config.cmdline()

        # On deletion nw_type will no longer be known
        if self.get_type() in ('guest'):
            if self.config.is_vpc() or self.config.is_router():
                CsDevice(self.dev, self.config).configure_rp()

                logging.error(
                    "Not able to setup source-nat for a regular router yet")

            if (self.config.has_dns() or self.config.is_dhcp()) and self.config.expose_dns():
                logging.info("Making dns publicly available")
                dns = CsDnsmasq(self)
                dns.add_firewall_rules()
            else:
                logging.info("Not making dns publicly available")

            if self.config.has_metadata():
                app = CsApache(self)
                app.setup()

                # If redundant then this is dealt with
                # by the master backup functions
                if not cmdline.is_redundant():
                    if method == "add":
                        CsPasswdSvc(self.address['public_ip']).start()
                    elif method == "delete":
                        CsPasswdSvc(self.address['public_ip']).stop()
                elif cmdline.is_master():
                    if method == "add":
                        CsPasswdSvc(self.get_gateway() + "," + self.address['public_ip']).start()
                    elif method == "delete":
                        CsPasswdSvc(self.get_gateway() + "," + self.address['public_ip']).stop()

        if self.get_type() == "public" and self.config.is_vpc() and method == "add":
            if self.address["source_nat"]:
                vpccidr = cmdline.get_vpccidr()
                self.fw.append(
                    ["filter", 3, "-A FORWARD -s %s ! -d %s -j ACCEPT" % (vpccidr, vpccidr)])
                self.fw.append(
                    ["nat", "", "-A POSTROUTING -j SNAT -o %s --to-source %s" % (self.dev, self.address['public_ip'])])

    def list(self):
        self.iplist = {}
        cmd = ("ip addr show dev " + self.dev)
        for i in CsHelper.execute(cmd):
            vals = i.lstrip().split()
            if (vals[0] == 'inet'):
                cidr = vals[1]
                self.iplist[cidr] = self.dev

    def configured(self):
        if self.address['cidr'] in list(self.iplist.keys()):
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

    def is_private_gateway(self):
        if "is_private_gateway" in self.address:
            return self.address['is_private_gateway']
        return False

    def ip(self):
        return str(self.address['cidr'])

    def getDevice(self):
        return self.dev

    def hasIP(self, ip):
        return ip in list(self.address.values())

    def arpPing(self):
        cmd = "arping -c 1 -I %s -A -U -s %s %s" % (
            self.dev, self.address['public_ip'], self.address['gateway'])
        if not self.cl.is_redundant() and (not self.address['gateway'] or self.address['gateway'] == "None"):
            cmd = "arping -c 1 -I %s -A -U %s" % (self.dev, self.address['public_ip'])
        CsHelper.execute2(cmd, False)

    # Delete any ips that are configured but not in the bag
    def compare(self, bag):
        if len(self.iplist) > 0 and (self.dev not in list(bag.keys()) or len(bag[self.dev]) == 0):
            # Remove all IPs on this device
            logging.info(
                "Will remove all configured addresses on device %s", self.dev)
            self.delete("all")
            app = CsApache(self)
            app.remove()

        # This condition should not really happen but did :)
        # It means an apache file got orphaned after a guest network address
        # was deleted
        if len(self.iplist) == 0 and (self.dev not in list(bag.keys()) or len(bag[self.dev]) == 0):
            app = CsApache(self)
            app.remove()

        for ip in self.iplist:
            found = False
            if self.dev in list(bag.keys()):
                for address in bag[self.dev]:
                    self.setAddress(address)
                    if (self.hasIP(ip) or self.is_guest_gateway(address, ip)) and address["add"]:
                        logging.debug("The IP address in '%s' will be configured" % address)
                        found = True
            if not found:
                self.delete(ip)

    def get_gateway(self):
        interface = CsInterface(self.address, self.config)
        return interface.get_gateway()

    def is_guest_gateway(self, bag, ip):
        """ Exclude the vrrp maintained addresses on a redundant router """
        interface = CsInterface(bag, self.config)
        if not self.config.cl.is_redundant():
            return False

        rip = ip.split('/')[0]
        logging.info("Checking if cidr is a gateway for rVPC. IP ==> %s / device ==> %s", ip, self.dev)

        gw = interface.get_gateway()
        logging.info("Interface has the following gateway ==> %s", gw)

        if bag['nw_type'] == "guest" and rip == gw:
            return True
        return False

    def delete(self, ip):
        remove = []
        if ip == "all":
            logging.info("Removing addresses from device %s", self.dev)
            remove = list(self.iplist.keys())
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
