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
import mock
from cs.CsAddress import CsAddress, CsIP
import merge


class TestCsAddress(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."
        self.csaddress = CsAddress("ips", {})

    def test_needs_vrrp(self):
        self.assertTrue(self.csaddress.needs_vrrp({"nw_type": "guest"}))

    def test_get_guest_if(self):
        self.assertTrue(self.csaddress.get_guest_if() is None)

    def test_get_guest_ip(self):
        self.assertTrue(self.csaddress.get_guest_ip() is None)

    def test_get_guest_netmask(self):
        self.assertTrue(self.csaddress.get_guest_netmask() == "255.255.255.0")


class TestCsIPRouted(unittest.TestCase):

    def routed_guest_ip(self, dev, is_vpc):
        config = mock.MagicMock()
        config.is_vpc.return_value = is_vpc
        config.is_routed.return_value = True
        config.get_nft_ipv4_fw.return_value = []
        config.get_nft_ipv4_acl.return_value = []
        with mock.patch.object(CsIP, 'list'):
            ip = CsIP(dev, config)
        ip.setAddress({"nw_type": "guest", "network": "10.1.1.0/24"})
        return ip

    def input_rules(self, rules):
        return [rule['rule'] for rule in rules if rule['chain'] == 'INPUT']

    def test_fw_router_routing_accepts_vrrp_and_conntrackd(self):
        ip = self.routed_guest_ip("eth0", False)
        ip.fw_router_routing()
        rules = self.input_rules(ip.nft_ipv4_fw)
        self.assertIn("iifname eth0 ip daddr 224.0.0.18 counter accept", rules)
        self.assertIn("iifname eth0 ip daddr 225.0.0.50 counter accept", rules)

    def test_fw_vpcrouter_routing_accepts_vrrp_and_conntrackd(self):
        ip = self.routed_guest_ip("eth2", True)
        ip.fw_vpcrouter_routing()
        rules = self.input_rules(ip.nft_ipv4_acl)
        self.assertIn("iifname eth2 ip daddr 224.0.0.18 counter accept", rules)
        self.assertIn("iifname eth2 ip daddr 225.0.0.50 counter accept", rules)


if __name__ == '__main__':
    unittest.main()
