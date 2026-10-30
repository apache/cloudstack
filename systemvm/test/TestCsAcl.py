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

    def __init__(self, routed=False):
        self.routed = routed
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
        return self.routed

    def get_ingress_chain(self, device, ip):
        return "ACL_INBOUND_%s" % device

    def get_egress_chain(self, device, ip):
        return "ACL_OUTBOUND_%s" % device

    def get_egress_table(self):
        return "mangle"


class TestCsAcl(unittest.TestCase):

    def acl_device(self, config, ingress, egress=None):
        obj = {"device": "eth3", "nic_ip": "10.1.1.1", "nic_netmask": "24", "nic_ip6_cidr": "fd00:1::/64",
               "ingress_rules": ingress, "egress_rules": egress or []}
        return CsAcl.AclDevice(obj, config)

    def test_multiple_cidrs_emit_one_iptables_rule_each(self):
        config = FakeConfig()
        acl = self.acl_device(config, [
            {"type": "tcp", "cidr": "1.2.3.4/32,2.3.4.5/32", "first_port": 22, "last_port": 22, "allowed": True},
            {"type": "all", "cidr": "0.0.0.0/0", "allowed": False}])
        acl.process("ingress", acl.ingress, acl.FIXED_RULES_INGRESS, False)

        # iptables expands "-s a,b" into several rules, which CsNetfilters would count as one
        # and so insert the following rules between them, i.e. behind a later deny
        rules = [fw[2] for fw in config.fw]
        self.assertEqual(rules, [
            "-A ACL_INBOUND_eth3 -p tcp -s 1.2.3.4/32 -m tcp --dport 22 -j ACCEPT",
            "-A ACL_INBOUND_eth3 -p tcp -s 2.3.4.5/32 -m tcp --dport 22 -j ACCEPT",
            "-A ACL_INBOUND_eth3 -p all -s 0.0.0.0/0 -j DROP"])
        self.assertEqual([fw[1] for fw in config.fw], [3, 4, 5])

    def test_multiple_egress_cidrs_emit_one_iptables_rule_each(self):
        config = FakeConfig()
        acl = self.acl_device(config, [], [
            {"type": "udp", "cidr": "8.8.8.8/32,8.8.4.4/32", "first_port": 53, "last_port": 53, "allowed": True},
            {"type": "all", "cidr": "0.0.0.0/0", "allowed": False}])
        acl.process("egress", acl.egress, acl.FIXED_RULES_EGRESS, False)

        self.assertEqual(config.fw, [
            ["mangle", 3, "-A ACL_OUTBOUND_eth3 -p udp -d 8.8.8.8/32 -m udp --dport 53 -j ACCEPT"],
            ["mangle", 4, "-A ACL_OUTBOUND_eth3 -p udp -d 8.8.4.4/32 -m udp --dport 53 -j ACCEPT"],
            ["mangle", 5, "-A ACL_OUTBOUND_eth3 -p all -d 0.0.0.0/0 -j DROP"]])

    def test_empty_cidr_elements_are_skipped(self):
        config = FakeConfig()
        acl = self.acl_device(config, [
            {"type": "tcp", "cidr": "1.2.3.4/32,,2.3.4.5/32,", "first_port": 22, "last_port": 22, "allowed": True}])
        acl.process("ingress", acl.ingress, acl.FIXED_RULES_INGRESS, False)

        self.assertEqual([fw[2] for fw in config.fw], [
            "-A ACL_INBOUND_eth3 -p tcp -s 1.2.3.4/32 -m tcp --dport 22 -j ACCEPT",
            "-A ACL_INBOUND_eth3 -p tcp -s 2.3.4.5/32 -m tcp --dport 22 -j ACCEPT"])
        rules = [r['rule'] for r in config.ipv6_acl if r.get('chain') == "eth3_ingress_policy" and 'rule' in r]
        self.assertNotIn("{", " ".join(rules))

    def test_multiple_ipv6_cidrs_use_nft_set(self):
        config = FakeConfig()
        acl = self.acl_device(config, [
            {"type": "tcp", "cidr": "2001:db8:1::/64,2001:db8:2::/64,1.2.3.4/32", "first_port": 22, "last_port": 22,
             "allowed": True}])
        acl.process("ingress", acl.ingress, acl.FIXED_RULES_INGRESS, False)

        # the IPv4 CIDR goes to iptables on its own, the IPv6 ones to nft as a set
        self.assertEqual([fw[2] for fw in config.fw], ["-A ACL_INBOUND_eth3 -p tcp -s 1.2.3.4/32 -m tcp --dport 22 -j ACCEPT"])
        rules = [r['rule'] for r in config.ipv6_acl if r.get('chain') == "eth3_ingress_policy" and 'rule' in r]
        self.assertIn("ip6 saddr { 2001:db8:1::/64, 2001:db8:2::/64 } tcp dport 22 accept", rules)

    def test_multiple_cidrs_use_nft_set_when_routed(self):
        config = FakeConfig(routed=True)
        acl = self.acl_device(config, [
            {"type": "tcp", "cidr": "1.2.3.4/32,2.3.4.5/32", "first_port": 22, "last_port": 22, "allowed": True},
            {"type": "tcp", "cidr": "3.4.5.6/32", "first_port": 443, "last_port": 443, "allowed": True}])
        acl.process("ingress", acl.ingress, acl.FIXED_RULES_INGRESS, True)

        rules = [r['rule'] for r in config.nft_ipv4_acl if r.get('chain') == "eth3_ingress_policy" and 'rule' in r]
        self.assertIn("ip saddr { 1.2.3.4/32, 2.3.4.5/32 } tcp dport 22 accept", rules)
        self.assertIn("ip saddr 3.4.5.6/32 tcp dport 443 accept", rules)


if __name__ == '__main__':
    unittest.main()
