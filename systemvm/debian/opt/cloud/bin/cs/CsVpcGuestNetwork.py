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
import os.path
from cs.CsDatabag import CsDataBag
from CsFile import CsFile
import CsHelper

VPC_PUBLIC_INTERFACE = "eth1"

RADVD_CONF = "/etc/radvd.conf"
RADVD_CONF_NEW = "/etc/radvd.conf.new"

class CsVpcGuestNetwork(CsDataBag):
    """ Manage Vpc Guest Networks """

    def process(self):
        logging.debug("Processing CsVpcGuestNetwork")
        self.conf = CsFile(RADVD_CONF_NEW)
        self.conf.empty()
        for item in self.dbag:
            if item == "id":
                continue
            for address in self.dbag[item]:
                if address['add']:
                    self.add_address_route(address)
                    self.add_radvd_conf(address)
                else:
                    self.remove_address_route(address)
        self.conf.commit()
        file = CsFile(RADVD_CONF)
        if not file.compare(self.conf):
            CsHelper.copy(RADVD_CONF_NEW, RADVD_CONF)
            logging.debug("CsVpcGuestNetwork:: will restart radvd !")
            CsHelper.service("radvd", "restart")

    def __disable_dad(self, device):
        CsHelper.execute("sysctl net.ipv6.conf." + device + ".accept_dad=0")
        CsHelper.execute("sysctl net.ipv6.conf." + device + ".use_tempaddr=0")

    def add_address_route(self, entry):
        if 'router_guest_ip6' in entry.keys() and entry['router_guest_ip6']:
            self.enable_ipv6(entry['device'])
            cidr_size = entry['router_guest_ip6_cidr'].split("/")[-1]
            full_addr = entry['router_guest_ip6_gateway'] + "/" + cidr_size
            if not CsHelper.execute("ip -6 addr show dev %s | grep -w %s" % (entry['device'], full_addr)):
                CsHelper.execute("ip -6 addr add %s dev %s" % (full_addr, entry['device']))
            if 'router_ip6' in entry.keys() and entry['router_ip6']:
                self.__disable_dad(VPC_PUBLIC_INTERFACE)
                full_public_addr = entry['router_ip6'] + "/" + cidr_size
                if not CsHelper.execute("ip -6 addr show dev %s | grep -w %s" % (VPC_PUBLIC_INTERFACE, full_public_addr)):
                    CsHelper.execute("ip -6 addr add %s dev %s" % (full_public_addr, VPC_PUBLIC_INTERFACE))
                if not CsHelper.execute("ip -6 route list default via %s" % entry['router_ip6_gateway']):
                    CsHelper.execute("ip -6 route add default via %s" % entry['router_ip6_gateway'])
        else:
            return

    def remove_address_route(self, entry):
        if 'router_guest_ip6' in entry.keys() and entry['router_guest_ip6']:
            cidr_size = entry['router_guest_ip6_cidr'].split("/")[-1]
            full_addr = entry['router_guest_ip6_gateway'] + "/" + cidr_size
            CsHelper.execute("ip -6 addr del %s dev %s" % (full_addr, entry['device']))
            if 'router_ip6' in entry.keys() and entry['router_ip6']:
                full_public_addr = entry['router_ip6'] + "/" + cidr_size
                CsHelper.execute("ip -6 addr del %s dev %s" % (full_public_addr, VPC_PUBLIC_INTERFACE))
        else:
            return

    def enable_ipv6(self, device):
        logging.debug("Enabling IPv6 in this router")
        CsHelper.execute("sysctl net.ipv6.conf.all.disable_ipv6=0")
        CsHelper.execute("sysctl net.ipv6.conf.all.forwarding=1")
        CsHelper.execute("sysctl net.ipv6.conf.all.accept_ra=1")

        # to solve the 'tentative dadfailed' when perform rolling upgrade
        CsHelper.execute("sysctl net.ipv6.conf.all.accept_dad=0")
        CsHelper.execute("sysctl net.ipv6.conf.default.accept_dad=0")
        CsHelper.execute("sysctl net.ipv6.conf.all.use_tempaddr=0")
        CsHelper.execute("sysctl net.ipv6.conf.default.use_tempaddr=0")
        self.__disable_dad(device)

    def add_radvd_conf(self, entry):
        if 'router_guest_ip6' in entry.keys() and entry['router_guest_ip6']:
            cidr_size = entry['router_guest_ip6_cidr'].split("/")[-1]
            full_addr = entry['router_guest_ip6_gateway'] + "/" + cidr_size
            self.conf.append("interface %s" % entry['device'])
            self.conf.append("{")
            self.conf.append("    AdvSendAdvert on;")
            self.conf.append("    MinRtrAdvInterval 5;")
            self.conf.append("    MaxRtrAdvInterval 15;")
            self.conf.append("    prefix %s" % full_addr)
            self.conf.append("    {")
            self.conf.append("        AdvOnLink on;")
            self.conf.append("        AdvAutonomous on;")
            self.conf.append("    };")
            if 'dns6' in entry.keys() and entry['dns6']:
                for dns in entry['dns6'].split(","):
                    self.conf.append("    RDNSS %s" % dns)
                    self.conf.append("    {")
                    self.conf.append("        AdvRDNSSLifetime 30;")
                    self.conf.append("    };")
            self.conf.append("};")
