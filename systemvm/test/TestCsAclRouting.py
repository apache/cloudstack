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


class TestCsAclRouting(unittest.TestCase):

    def test_protocol_number_rule(self):
        config = FakeConfig()
        obj = {"device": "eth3", "nic_ip": "10.1.1.1", "nic_netmask": "24", "nic_ip6_cidr": "fd00:1::/64",
               "ingress_rules": [{"type": "protocol", "protocol": 47, "cidr": "1.2.3.4/32,2001:db8::/64", "allowed": True}],
               "egress_rules": [{"type": "protocol", "protocol": 47, "cidr": "5.6.7.8/32", "allowed": False}]}
        acl = CsAcl.AclDevice(obj, config)
        acl.create()

        # nexthdr is an IPv6 header field, nft rejects it in an ip rule
        ip4 = [r['rule'] for r in config.nft_ipv4_acl if r.get('chain') == "eth3_ingress_policy" and 'rule' in r]
        self.assertIn("ip saddr 1.2.3.4/32 ip protocol 47 accept", ip4)
        ip6 = [r['rule'] for r in config.ipv6_acl if r.get('chain') == "eth3_ingress_policy" and 'rule' in r]
        # the IPv4 match must not leak into the IPv6 rule
        self.assertTrue(any(r.startswith("ip6 saddr 2001:db8::/64 ") and r.endswith(" 47 accept") for r in ip6))
        self.assertFalse(any("ip protocol" in r for r in ip6))
        ip4 = [r['rule'] for r in config.nft_ipv4_acl if r.get('chain') == "eth3_egress_policy" and 'rule' in r]
        self.assertIn("ip daddr 5.6.7.8/32 ip protocol 47 drop", ip4)


if __name__ == '__main__':
    unittest.main()
