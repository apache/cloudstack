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
from pprint import pprint
import logging

NO_PRELOAD = False
LEASES     = "/var/lib/misc/dnsmasq.leases"
DHCP_HOSTS = "/etc/dhcphosts.txt"

"""
    "172.16.1.102": {
                "default_entry": true,
                "default_gateway": "172.16.1.1",
                "host_name": "VM-58976c22-0832-451e-9ab2-039e9f27e415",
                "ipv4_adress": "172.16.1.102",
                "ipv6_duid": "00:03:00:01:02:00:26:c3:00:02",
                "mac_address": "02:00:26:c3:00:02",
                "type": "dhcpentry"
      },
"""
class CsDhcp(object):
    """ Manage dhcp entries """

    def __init__(self, dbag, cl):
        dnsmasq = CsDnsMasq()
        for item in dbag:
            if item == "id":
                continue
            dnsmasq.add(dbag[item])
        dnsmasq.collect_leases()
        dnsmasq.to_str()
        dnsmasqb4 = CsDnsMasq(NO_PRELOAD)
        dnsmasqb4.parse_hosts()
        if not dnsmasq.compare_hosts(dnsmasqb4):
            dnsmasq.write_hosts()
        else:
            logging.debug("Hosts file is up to date")


class CsDnsMasq(object):

    def __init__(self, preload = True):
        self.list  = []
        self.hosts = []
        self.leases = []
        if preload:
            self.add_host("127.0.0.1", "localhost")
            self.add_host("::1",     "localhost ip6-localhost ip6-loopback")
            self.add_host("ff02::1", "ip6-allnodes")
            self.add_host("ff02::2", "ip6-allrouters")
            self.add_host("127.0.0.1", CsHelper.get_hostname())

    def collect_leases(self):
        # Format is
        # 0 02:00:56:aa:00:03 10.1.1.18 pup *
        try:
            for line in open(LEASES):
                bits = line.strip().split(' ')
                to = { "device" : bits[0],
                        "mac" : bits[1],
                        "ip" : bits[2],
                        "host" : bits[3]
                        }
                self.leases.append(to)
        except IOError:
            pass

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

    def write_hosts(self):
        logging.debug("Updating hosts file with new entry")
        handle = open("/etc/hosts", 'w+')
        for line in self.hosts:
            handle.write("%s\n" % line)
        handle.close()

    def add(self,entry):
        self.add_host(entry['ipv4_adress'], entry['host_name'])
        self.add_dnsmasq(entry['ipv4_adress'], entry['host_name'], entry['mac_address'])

    def add_dnsmasq(self, ip, host, mac):
        self.list.append("%s,%s,%s,infinite" % (mac, ip, host))

    def add_host(self, ip, host):
        self.hosts.append("%s\t%s" % (ip, host))

    def to_str(self):
        pprint(self.hosts)
        pprint(self.list)
