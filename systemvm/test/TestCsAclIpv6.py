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
        return True


class TestCsAclIpv6(unittest.TestCase):

    def test_protocol_number_rule_uses_meta_l4proto(self):
        config = FakeConfig()
        obj = {"device": "eth3", "nic_ip": "10.1.1.1", "nic_netmask": "24", "nic_ip6_cidr": "fd00:1::/64",
               "ingress_rules": [{"type": "protocol", "protocol": 47, "cidr": "2001:db8::/64", "allowed": False}],
               "egress_rules": [{"type": "protocol", "protocol": 50, "cidr": "2001:db8:1::/64", "allowed": True}]}
        acl = CsAcl.AclDevice(obj, config)
        acl.create()

        # matched the way nft matches tcp, udp and icmpv6 rules
        ingress = [r['rule'] for r in config.ipv6_acl if r.get('chain') == "eth3_ingress_policy" and 'rule' in r]
        self.assertIn("ip6 saddr 2001:db8::/64 meta l4proto 47 drop", ingress)
        egress = [r['rule'] for r in config.ipv6_acl if r.get('chain') == "eth3_egress_policy" and 'rule' in r]
        self.assertIn("ip6 daddr 2001:db8:1::/64 meta l4proto 50 accept", egress)

    def test_extension_header_number_rule(self):
        config = FakeConfig()
        rules = [{"type": "protocol", "protocol": p, "cidr": "2001:db8::/64", "allowed": False} for p in (0, 43, 44, 60, 135)]
        obj = {"device": "eth3", "nic_ip": "10.1.1.1", "nic_netmask": "24", "nic_ip6_cidr": "fd00:1::/64",
               "ingress_rules": rules, "egress_rules": []}
        acl = CsAcl.AclDevice(obj, config)
        acl.create()

        ingress = [r['rule'] for r in config.ipv6_acl if r.get('chain') == "eth3_ingress_policy" and 'rule' in r]
        for name in ("hbh", "rt", "frag", "dst", "mh"):
            self.assertIn("ip6 saddr 2001:db8::/64 exthdr %s exists drop" % name, ingress)


if __name__ == '__main__':
    unittest.main()
