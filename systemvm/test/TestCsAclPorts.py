# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import unittest
from configure import CsAcl


class FakeConfig:

    def __init__(self):
        self.fw = []
        self.ipv6_acl = []
        self.nft_ipv4_acl = []

    def get_fw(self):
        return self.fw

    def get_ipv6_acl(self):
        return self.ipv6_acl

    def get_nft_ipv4_acl(self):
        return self.nft_ipv4_acl

    def is_vpc(self):
        return True

    def is_routed(self):
        return False

    def get_ingress_chain(self, device, ip):
        return "ACL_INBOUND_%s" % device

    def get_egress_chain(self, device, ip):
        return "ACL_OUTBOUND_%s" % device

    def get_egress_table(self):
        return "mangle"


class TestCsAclPorts(unittest.TestCase):

    def acl(self, ingress, egress=None):
        config = FakeConfig()
        obj = {"device": "eth3", "nic_ip": "10.1.1.1", "nic_netmask": "24", "nic_ip6_cidr": "fd00:1::/64",
               "ingress_rules": ingress, "egress_rules": egress or []}
        CsAcl.AclDevice(obj, config).create()
        return config

    def iptables_rules(self, ingress, egress=None):
        return [fw[2] for fw in self.acl(ingress, egress).fw]

    def test_tcp_and_udp_without_ports_match_all_ports(self):
        # a tcp or udp rule without ports reaches the VR as the port range 0:0
        rules = self.iptables_rules(
            [{"type": "tcp", "cidr": "10.0.0.0/8", "first_port": 0, "last_port": 0, "allowed": True}],
            [{"type": "udp", "cidr": "10.0.0.0/8", "first_port": 0, "last_port": 0, "allowed": False}])
        self.assertEqual(rules, ["-A ACL_INBOUND_eth3 -p tcp -s 10.0.0.0/8 -j ACCEPT",
                                 "-A ACL_OUTBOUND_eth3 -p udp -d 10.0.0.0/8 -j DROP"])

    def test_tcp_without_ports_matches_all_ports_on_ipv6_too(self):
        config = self.acl([{"type": "tcp", "cidr": "10.0.0.0/8,fd00:2::/64", "first_port": 0, "last_port": 0, "allowed": True}])
        self.assertEqual([fw[2] for fw in config.fw], ["-A ACL_INBOUND_eth3 -p tcp -s 10.0.0.0/8 -j ACCEPT"])
        ipv6 = [r['rule'] for r in config.ipv6_acl if r.get('chain') == "eth3_ingress_policy" and 'rule' in r]
        self.assertIn("ip6 saddr fd00:2::/64 tcp dport { 0-65535 } accept", ipv6)

    def test_rules_without_port_fields_are_unaffected(self):
        rules = self.iptables_rules([
            {"type": "icmp", "cidr": "10.0.0.0/8", "icmp_type": 8, "icmp_code": 0, "allowed": True},
            {"type": "protocol", "protocol": 47, "cidr": "10.0.0.0/8", "allowed": True},
            {"type": "all", "cidr": "10.0.0.0/8", "allowed": False}])
        self.assertEqual(rules, ["-A ACL_INBOUND_eth3 -p icmp -s 10.0.0.0/8 -m icmp --icmp-type 8/0 -j ACCEPT",
                                 "-A ACL_INBOUND_eth3 -p 47 -s 10.0.0.0/8 -j ACCEPT",
                                 "-A ACL_INBOUND_eth3 -p all -s 10.0.0.0/8 -j DROP"])

    def test_ports_and_port_ranges_are_kept(self):
        rules = self.iptables_rules([
            {"type": "tcp", "cidr": "10.0.0.0/8", "first_port": 22, "last_port": 22, "allowed": True},
            {"type": "udp", "cidr": "10.0.0.0/8", "first_port": 1000, "last_port": 2000, "allowed": True},
            {"type": "tcp", "cidr": "10.0.0.0/8", "first_port": 0, "last_port": 1024, "allowed": False}])
        self.assertEqual(rules, ["-A ACL_INBOUND_eth3 -p tcp -s 10.0.0.0/8 -m tcp --dport 22 -j ACCEPT",
                                 "-A ACL_INBOUND_eth3 -p udp -s 10.0.0.0/8 -m udp --dport 1000:2000 -j ACCEPT",
                                 "-A ACL_INBOUND_eth3 -p tcp -s 10.0.0.0/8 -m tcp --dport 0:1024 -j DROP"])


if __name__ == '__main__':
    unittest.main()
