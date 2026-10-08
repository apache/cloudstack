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
from unittest import mock
from cs.CsAddress import CsAddress, CsIP
import merge


class FakeConfig:

    def __init__(self):
        self.fw = []
        self.nft_ipv4_fw = []
        self.nft_ipv4_acl = []

    def get_fw(self):
        return self.fw

    def get_nft_ipv4_fw(self):
        return self.nft_ipv4_fw

    def get_nft_ipv4_acl(self):
        return self.nft_ipv4_acl

    def cmdline(self):
        cl = mock.Mock()
        cl.get_vpccidr.return_value = "10.0.0.0/16"
        return cl

    def is_vpc(self):
        return True

    def is_routed(self):
        return False

    def has_public_network(self):
        return True


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

    def acl_outbound_rules(self, address):
        config = FakeConfig()
        with mock.patch.object(CsIP, "list"):
            ip = CsIP("eth3", config)
        ip.setAddress(address)
        ip.fw_vpcrouter()
        return [fw for fw in config.fw if "-A ACL_OUTBOUND_eth3 " in fw[2]]

    def test_acl_outbound_ends_with_return_on_guest_tier(self):
        # ACL rules are inserted ahead of the last rule of the chain, so that rule must be a
        # terminal one, or the rule it is would end up behind the ACL rules
        rules = self.acl_outbound_rules({"nw_type": "guest", "network": "10.0.1.0/24", "gateway": "10.0.1.1"})
        self.assertEqual(rules[-1], ["mangle", "", "-A ACL_OUTBOUND_eth3 -j RETURN"])
        self.assertTrue(all(fw[1] == "front" for fw in rules[:-1]))

    def test_acl_outbound_ends_with_return_on_private_gateway(self):
        rules = self.acl_outbound_rules({"nw_type": "public", "is_private_gateway": True, "network": "172.16.0.0/24",
                                         "gateway": "172.16.0.1", "public_ip": "172.16.0.10", "source_nat": False})
        self.assertEqual(rules, [["mangle", "", "-A ACL_OUTBOUND_eth3 -j RETURN"]])


if __name__ == '__main__':
    unittest.main()
