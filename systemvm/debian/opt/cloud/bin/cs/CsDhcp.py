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
from . import CsHelper
import logging
import os
from ipaddress import ip_address
from random import randint
from .CsGuestNetwork import CsGuestNetwork
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
        self.dhcp_leases = CsFile(LEASES)

        self.cloud.repopulate()
        self.dhcp_opts.repopulate()

        for item in self.dbag:
            if item == "id":
                continue
            if not self.dbag[item]['remove']:
                self.add(self.dbag[item])

        self.configure_server()

        restart_dnsmasq = False

        if self.conf.commit():
            restart_dnsmasq = True

        if self.cloud.commit():
            restart_dnsmasq = True

        if self.dhcp_leases.commit():
            restart_dnsmasq = True

        self.dhcp_opts.commit()

        if restart_dnsmasq:
            self.delete_leases()

        self.write_hosts()

        if not self.cl.is_redundant() or self.cl.is_master():
            if restart_dnsmasq:
                CsHelper.service("dnsmasq", "restart")
            else:
                CsHelper.start_if_stopped("dnsmasq")
                CsHelper.service("dnsmasq", "reload")

    def configure_server(self):
        # self.conf.addeq("dhcp-hostsfile=%s" % DHCP_HOSTS)
        idx = 0
        listen_address = ["127.0.0.1"]
        for i in self.devinfo:
            if not i['dnsmasq']:
                continue
            device = i['dev']
            ip = i['ip'].split('/')[0]
            gn = CsGuestNetwork(device, self.config)
            # Gateway
            gateway = ''
            if self.config.is_vpc():
                gateway = gn.get_gateway()
            else:
                gateway = i['gateway']
            sline = "dhcp-range=set:interface-%s-%s" % (device, idx)
            if self.cl.is_redundant():
                line = "dhcp-range=set:interface-%s-%s,%s,static" % (device, idx, gateway)
            else:
                line = "dhcp-range=set:interface-%s-%s,%s,static" % (device, idx, ip)
            self.conf.search(sline, line)
            sline = "dhcp-option=tag:interface-%s-%s,15" % (device, idx)
            line = "dhcp-option=tag:interface-%s-%s,15,%s" % (device, idx, gn.get_domain())
            self.conf.search(sline, line)
            # DNS search order
            if gn.get_dns() and device:
                sline = "dhcp-option=tag:interface-%s-%s,6" % (device, idx)
                dns_list = [x for x in gn.get_dns() if x]
                line = "dhcp-option=tag:interface-%s-%s,6,%s" % (device, idx, ','.join(dns_list))
                self.conf.search(sline, line)
            if gateway != '0.0.0.0':
                sline = "dhcp-option=tag:interface-%s-%s,3," % (device, idx)
                line = "dhcp-option=tag:interface-%s-%s,3,%s" % (device, idx, gateway)
                self.conf.search(sline, line)
            # Netmask
            netmask = ''
            if self.config.is_vpc():
                netmask = gn.get_netmask()
            else:
                netmask = str(i['network'].netmask)
            sline = "dhcp-option=tag:interface-%s-%s,1," % (device, idx)
            line = "dhcp-option=tag:interface-%s-%s,1,%s" % (device, idx, netmask)
            self.conf.search(sline, line)
            # Listen Address
            if self.cl.is_redundant():
                listen_address.append(gateway)
            else:
                listen_address.append(ip)
            idx += 1

        # Listen Address
        sline = "listen-address="
        line = "listen-address=%s" % (','.join(listen_address))
        self.conf.search(sline, line)

    def delete_leases(self):
        macs_dhcphosts = []
        try:
            logging.info("Attempting to delete entries from dnsmasq.leases file for VMs which are not on dhcphosts file")
            for host in open(DHCP_HOSTS):
                macs_dhcphosts.append(host.split(',')[0])

            removed = 0
            for leaseline in open(LEASES):
                lease = leaseline.split(' ')
                mac = lease[1]
                ip = lease[2]
                if mac not in macs_dhcphosts:
                    cmd = "dhcp_release $(ip route get %s | grep eth | head -1 | awk '{print $3}') %s %s" % (ip, ip, mac)
                    logging.info(cmd)
                    CsHelper.execute(cmd)
                    removed = removed + 1
                    self.del_host(ip)
            logging.info("Deleted %s entries from dnsmasq.leases file" % str(removed))
        except Exception as e:
            logging.error("Caught error while trying to delete entries from dnsmasq.leases file: %s" % e)

    def preseed(self):
        self.add_host("127.0.0.1", "localhost")
        self.add_host("127.0.1.1", "%s" % CsHelper.get_hostname())
        self.add_host("::1", "localhost ip6-localhost ip6-loopback")
        self.add_host("ff02::1", "ip6-allnodes")
        self.add_host("ff02::2", "ip6-allrouters")
        if self.config.is_router() or self.config.is_dhcp():
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
        # Lease time set to "infinite" since we properly control all DHCP/DNS config via CloudStack.
        # Infinite time helps avoid some edge cases which could cause DHCPNAK being sent to VMs since
        # (RHEL) system lose routes when they receive DHCPNAK.
        # When VM is expunged, its active lease and DHCP/DNS config is properly removed from related files in VR,
        # so the infinite duration of lease does not cause any issues or garbage.
        lease = 'infinite'

        if entry['default_entry']:
            self.cloud.add("%s,%s,%s,%s" % (entry['mac_address'],
                                            entry['ipv4_address'],
                                            entry['host_name'],
                                            lease))
            self.dhcp_leases.search(entry['mac_address'], "0 %s %s %s *" % (entry['mac_address'],
                                                                            entry['ipv4_address'],
                                                                            entry['host_name']))
        else:
            tag = entry['ipv4_address'].replace(".", "_")
            self.cloud.add("%s,set:%s,%s,%s,%s" % (entry['mac_address'],
                                                   tag,
                                                   entry['ipv4_address'],
                                                   entry['host_name'],
                                                   lease))
            self.dhcp_opts.add("%s,%s" % (tag, 3))
            self.dhcp_opts.add("%s,%s" % (tag, 6))
            self.dhcp_opts.add("%s,%s" % (tag, 15))
            self.dhcp_leases.search(entry['mac_address'], "0 %s %s %s *" % (entry['mac_address'],
                                                                            entry['ipv4_address'],
                                                                            entry['host_name']))

        i = ip_address(entry['ipv4_address'])
        # Calculate the device
        for v in self.devinfo:
            if i > v['network'].network and i < v['network'].broadcast:
                v['dnsmasq'] = True
                # Virtual Router
                v['gateway'] = entry['default_gateway']

    def add_host(self, ip, hosts):
        self.hosts[ip] = hosts

    def del_host(self, ip):
        if ip in self.hosts:
            self.hosts.pop(ip)
