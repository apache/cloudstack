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
from random import randint
from CsGuestNetwork import CsGuestNetwork
from cs.CsDatabag import CsDataBag
from cs.CsFile import CsFile

LEASES = "/var/lib/misc/dnsmasq.leases"
DHCP_HOSTS = "/etc/dhcphosts.txt"
DHCP_OPTS = "/etc/dhcpopts.txt"
CLOUD_CONF = "/etc/dnsmasq.d/cloud.conf"


class CsDhcp(CsDataBag):
    """ Manage dhcp entries """

    def process(self):
        self.hosts = {}
        self.changed = []
        self.devinfo = CsHelper.get_device_info()
        self.preseed()
        self.cloud = CsFile(DHCP_HOSTS)
        self.dhcp_opts = CsFile(DHCP_OPTS)
        self.conf = CsFile(CLOUD_CONF)

        self.cloud.repopulate()
        self.dhcp_opts.repopulate()

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
        self.dhcp_opts.commit()

        # We restart DNSMASQ every time the configure.py is called in order to avoid lease problems.
        if not self.cl.is_redundant() or self.cl.is_master():
            CsHelper.service("dnsmasq", "restart")

    def configure_server(self):
        # self.conf.addeq("dhcp-hostsfile=%s" % DHCP_HOSTS)
        idx = 0
        for i in self.devinfo:
            if not i['dnsmasq']:
                continue
            device = i['dev']
            ip = i['ip'].split('/')[0]
            sline = "dhcp-range=set:interface-%s-%s" % (device, idx)
            line = "dhcp-range=set:interface-%s-%s,%s,static" % (device, idx, ip)
            self.conf.search(sline, line)
            gn = CsGuestNetwork(device, self.config)
            sline = "dhcp-option=tag:interface-%s-%s,15" % (device, idx)
            line = "dhcp-option=tag:interface-%s-%s,15,%s" % (device, idx, gn.get_domain())
            self.conf.search(sline, line)
            # DNS search order
            if gn.get_dns() and device:
                sline = "dhcp-option=tag:interface-%s-%s,6" % (device, idx)
                dns_list = [x for x in gn.get_dns() if x is not None]
                line = "dhcp-option=tag:interface-%s-%s,6,%s" % (device, idx, ','.join(dns_list))
                self.conf.search(sline, line)
            # Gateway
            gateway = ''
            if self.config.is_vpc():
                gateway = gn.get_gateway()
            else:
                gateway = i['gateway']
            if gateway != '0.0.0.0':
                sline = "dhcp-option=tag:interface-%s-%s,3," % (device, idx)
                line = "dhcp-option=tag:interface-%s-%s,3,%s" % (device, idx, gateway)
                self.conf.search(sline, line)
            # Netmask
            netmask = ''
            if self.config.is_vpc():
                netmask = gn.get_netmask()
            else:
                netmask = self.config.address().get_guest_netmask()
            sline = "dhcp-option=tag:interface-%s-%s,1," % (device, idx)
            line = "dhcp-option=tag:interface-%s-%s,1,%s" % (device, idx, netmask)
            self.conf.search(sline, line)
            idx += 1

    def delete_leases(self):
        try:
            open(LEASES, 'w').close()
        except IOError:
            return

    def preseed(self):
        self.add_host("127.0.0.1", "localhost %s" % CsHelper.get_hostname())
        self.add_host("::1", "localhost ip6-localhost ip6-loopback")
        self.add_host("ff02::1", "ip6-allnodes")
        self.add_host("ff02::2", "ip6-allrouters")
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
        self.add_host(entry['ipv4_address'], entry['host_name'])
        # lease time boils down to once a month
        # with a splay of 60 hours to prevent storms
        lease = randint(700, 760)

        if entry['default_entry'] == True:
            self.cloud.add("%s,%s,%s,%sh" % (entry['mac_address'],
                                             entry['ipv4_address'],
                                             entry['host_name'],
                                             lease))
        else:
            tag = entry['ipv4_address'].replace(".","_")
            self.cloud.add("%s,set:%s,%s,%s,%sh" % (entry['mac_address'],
                                                    tag,
                                                    entry['ipv4_address'],
                                                    entry['host_name'],
                                                    lease))
            self.dhcp_opts.add("%s,%s" % (tag, 3))
            self.dhcp_opts.add("%s,%s" % (tag, 6))
            self.dhcp_opts.add("%s,%s" % (tag, 15))

        i = IPAddress(entry['ipv4_address'])
        # Calculate the device
        for v in self.devinfo:
            if i > v['network'].network and i < v['network'].broadcast:
                v['dnsmasq'] = True
                # Virtual Router
                v['gateway'] = entry['default_gateway']

    def add_host(self, ip, hosts):
        self.hosts[ip] = hosts
        
