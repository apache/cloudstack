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

NO_PRELOAD = False
LEASES     = "/var/lib/misc/dnsmasq.leases"
DHCP_HOSTS = "/etc/dhcphosts.txt"
DHCP_OPTS  = "/etc/dhcpopts.txt"
DNSMASQ_CONF = "/etc/dnsmasq.conf"
CLOUD_CONF = "/etc/dnsmasq.d/cloud.conf"

class CsDhcp(object):
    """ Manage dhcp entries """

    def __init__(self, dbag, cl):
        dnsmasq = CsDnsMasq()
        for item in dbag:
            if item == "id":
                continue
            dnsmasq.add(dbag[item])
        dnsmasqb4 = CsDnsMasq(NO_PRELOAD)
        dnsmasqb4.parse_hosts()
        dnsmasqb4.parse_dnsmasq()
        if not dnsmasq.compare_hosts(dnsmasqb4):
            logging.info("Updating hosts file")
            dnsmasq.write_hosts()
        else:
            logging.debug("Hosts file is up to date")
        diff = dnsmasq.compare_dnsmasq(dnsmasqb4)
        if len(diff) > 0:
            dnsmasq.updated = True
            dnsmasq.delete_leases(diff)
            dnsmasq.write_dnsmasq()
        dnsmasq.first_host = dnsmasqb4.first_host
        dnsmasq.configure_server()

class CsDnsMasq(object):

    def __init__(self, preload = True):
        self.list  = []
        self.hosts = []
        self.leases = []
        self.updated = False
        self.devinfo = CsHelper.get_device_info()
        self.devs = []
        self.first_host = False
        if preload:
            self.add_host("127.0.0.1", "localhost")
            self.add_host("::1",     "localhost ip6-localhost ip6-loopback")
            self.add_host("ff02::1", "ip6-allnodes")
            self.add_host("ff02::2", "ip6-allrouters")
            self.add_host("127.0.0.1", CsHelper.get_hostname())

    def delete_leases(self, clist):
        try:
            for line in open(LEASES):
                bits = line.strip().split(' ')
                to = { "device" : bits[0],
                        "mac" : bits[1],
                        "ip" : bits[2],
                        "host" : bits[3],
                        "del" : False
                        }
                for l in clist:
                    lbits = l.split(',')
                    if lbits[0] == to['mac'] or \
                       lbits[1] == to['ip']:
                        to['del'] == True
                        break
                self.leases.append(to)
            for o in self.leases:
                if o['del']:
                    cmd = "dhcp_release %s %s %s" % (o.device, o.ip, o.mac)
                    logging.info(cmd)
                    CsHelper.execute(cmd)
            # Finally add the new lease
        except IOError:
            return

    def configure_server(self):
        self.updated = self.updated | CsHelper.addifmissing(CLOUD_CONF, "dhcp-hostsfile=/etc/dhcphosts.txt")
        self.updated = self.updated | CsHelper.addifmissing(DNSMASQ_CONF, "dhcp-optsfile=%s:" % DHCP_OPTS)
        for i in self.devinfo:
            if not i['dnsmasq']:
                continue
            device = i['dev']
            ip = i['ip'].split('/')[0]
            line = "dhcp-range=interface:%s,set:interface-%s,%s,static" \
                    % (device, device, ip)
            self.updated = self.updated | CsHelper.addifmissing(CLOUD_CONF, line)
            # Next add the domain
            # if this is a guest network get it there otherwise use the value in resolv.conf
            gn = CsGuestNetwork(device)
            line = "dhcp-option=tag:interface-%s,15,%s" % (device,gn.get_domain())
            self.updated = self.updated | CsHelper.addifmissing(CLOUD_CONF, line)
        if self.updated:
            if self.first_host:
                CsHelper.service("dnsmasq", "restart")
            else:
                CsHelper.hup_dnsmasq("dnsmasq", "dnsmasq")
            
    def parse_dnsmasq(self):
        self.first_host = False
        try:
            for line in open(DHCP_HOSTS):
                self.list.append(line.strip())
            if len(self.list) == 0:
                self.first_host = True
        except IOError:
            self.first_host = True

    def parse_hosts(self):
        for line in open("/etc/hosts"):
            line = line.rstrip().lstrip()
            if line == '':
                continue
            if line.startswith("#"):
                continue
            bits = ' '.join(line.split()).split(' ', 1)
            self.add_host(bits[0], bits[1])

    def compare_hosts(self, obj):
        return set(self.hosts) == set(obj.hosts)

    def compare_dnsmasq(self, obj):
        return list(set(self.list).symmetric_difference(set(obj.list)))

    def write_hosts(self):
        logging.debug("Updating hosts file")
        handle = open("/etc/hosts", 'w+')
        for line in self.hosts:
            handle.write("%s\n" % line)
        handle.close()

    def write_dnsmasq(self):
        logging.debug("Updating %s", DHCP_HOSTS)
        handle = open(DHCP_HOSTS, 'w+')
        for line in self.list:
            handle.write("%s\n" % line)
            b = line.split(',')
        handle.close()

    def add(self,entry):
        self.add_host(entry['ipv4_adress'], entry['host_name'])
        self.add_dnsmasq(entry['ipv4_adress'], entry['host_name'], entry['mac_address'])
        i = IPAddress(entry['ipv4_adress'])
        # Calculate the device
        for v in self.devinfo:
            if i > v['network'].network and i < v['network'].broadcast:
                v['dnsmasq'] = True
                
    def add_dnsmasq(self, ip, host, mac):
        self.list.append("%s,%s,%s,infinite" % (mac, ip, host))

    def add_host(self, ip, host):
        self.hosts.append("%s\t%s" % (ip, host))
