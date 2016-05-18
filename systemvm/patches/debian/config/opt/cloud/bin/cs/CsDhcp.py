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
import CsHelper
import logging
from netaddr import *
from CsGuestNetwork import CsGuestNetwork
from cs.CsDatabag import CsDataBag
from cs.CsFile import CsFile

LEASES = "/var/lib/misc/dnsmasq.leases"
DHCP_HOSTS = "/etc/dhcphosts.txt"
CLOUD_CONF = "/etc/dnsmasq.d/cloud.conf"


class CsDhcp(CsDataBag):
    """ Manage dhcp entries """

    def process(self):
        self.hosts = {}
        self.changed = []
        self.devinfo = CsHelper.get_device_info()
        self.preseed()
        self.cloud = CsFile(DHCP_HOSTS)
        self.conf = CsFile(CLOUD_CONF)

        self.cloud.repopulate()

        for item in self.dbag:
            if item == "id":
                continue
            self.add(self.dbag[item])
        self.write_hosts()
        
        if self.cloud.is_changed():
            self.delete_leases()

        self.configure_server()

        self.conf.commit()
        self.cloud.commit()

        # We restart DNSMASQ every time the configure.py is called in order to avoid lease problems.
        CsHelper.execute2("service dnsmasq restart")

    def configure_server(self):
        # self.conf.addeq("dhcp-hostsfile=%s" % DHCP_HOSTS)
        for i in self.devinfo:
            if not i['dnsmasq']:
                continue
            device = i['dev']
            ip = i['ip'].split('/')[0]
            sline = "dhcp-range=interface:%s,set:interface" % (device)
            line = "dhcp-range=interface:%s,set:interface-%s,%s,static" % (device, device, ip)
            self.conf.search(sline, line)
            gn = CsGuestNetwork(device, self.config)
            sline = "dhcp-option=tag:interface-%s,15" % device
            line = "dhcp-option=tag:interface-%s,15,%s" % (device, gn.get_domain())
            self.conf.search(sline, line)
            # DNS search order
            if gn.get_dns() and device:
                sline = "dhcp-option=tag:interface-%s,6" % device
                dns_list = [x for x in gn.get_dns() if x is not None]
                line = "dhcp-option=tag:interface-%s,6,%s" % (device, ','.join(dns_list))
                self.conf.search(sline, line)
            # Gateway
            gateway = ''
            if self.config.is_vpc():
                gateway = gn.get_gateway()
            else:
                gateway = i['gateway']
            sline = "dhcp-option=tag:interface-%s,3," % device
            line = "dhcp-option=tag:interface-%s,3,%s" % (device, gateway)
            self.conf.search(sline, line)
            # Netmask
            netmask = ''
            if self.config.is_vpc():
                netmask = gn.get_netmask()
            else:
                netmask = self.config.address().get_guest_netmask()
            sline = "dhcp-option=tag:interface-%s,1," % device
            line = "dhcp-option=tag:interface-%s,1,%s" % (device, netmask)
            self.conf.search(sline, line)

    def delete_leases(self):
        try:
            open(LEASES, 'w').close()
        except IOError:
            return

    def preseed(self):
        self.add_host("127.0.0.1", "localhost")
        self.add_host("::1",     "localhost ip6-localhost ip6-loopback")
        self.add_host("ff02::1", "ip6-allnodes")
        self.add_host("ff02::2", "ip6-allrouters")
        if self.config.is_vpc():
            self.add_host("127.0.0.1", CsHelper.get_hostname())
        if self.config.is_router():
            self.add_host(self.config.address().get_guest_ip(), "%s data-server" % CsHelper.get_hostname())

    def write_hosts(self):
        file = CsFile("/etc/hosts")
        file.repopulate()
        for ip in self.hosts:
            file.add("%s\t%s" % (ip, self.hosts[ip]))
        if file.is_changed():
            file.commit()
            logging.info("Updated hosts file")
        else:
            logging.debug("Hosts file unchanged")

    def add(self, entry):
        self.add_host(entry['ipv4_adress'], entry['host_name'])
        self.cloud.add("%s,%s,%s,infinite" % (entry['mac_address'],
                                              entry['ipv4_adress'],
                                              entry['host_name']))
        i = IPAddress(entry['ipv4_adress'])
        # Calculate the device
        for v in self.devinfo:
            if i > v['network'].network and i < v['network'].broadcast:
                v['dnsmasq'] = True
                # Virtual Router
                v['gateway'] = entry['default_gateway']

    def add_host(self, ip, hosts):
        self.hosts[ip] = hosts
