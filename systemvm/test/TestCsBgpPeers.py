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
try:
    import mock
except ImportError:
    from unittest import mock
from cs.CsBgpPeers import CsBgpPeers
from cs.CsFile import CsFile
import merge


class TestCsBgpPeers(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."
        self.csbgppeers = CsBgpPeers("bgppeers", {})
        self.csbgppeers.peers = {}
        self.csbgppeers.public_ip = "100.64.0.10"
        self.csbgppeers.frr_conf = CsFile("frr.conf.test")
        self.csbgppeers.frr_conf.repopulate()

    def _peer(self, **kwargs):
        peer = {
            'peer_id': 1,
            'network_id': 100,
            'network_as_number': 64512,
            'peer_as_number': 64496
        }
        peer.update(kwargs)
        return peer

    def _frr_conf(self):
        return [line.rstrip('\n') for line in self.csbgppeers.frr_conf.new_config]

    def test_init(self):
        self.assertTrue(self.csbgppeers is not None)

    def test_process_dbag_item(self):
        self.csbgppeers._process_dbag_item(self._peer(ip4_address='100.64.0.1',
                                                      guest_ip4_cidr='10.1.1.0/24'))
        self.csbgppeers._process_dbag_item(self._peer(ip6_address='2001:db8::1',
                                                      guest_ip6_cidr='2001:db8:100::/64'))
        self.csbgppeers._process_dbag_item(self._peer(network_as_number=64513,
                                                      ip4_address='100.64.1.1',
                                                      guest_ip4_cidr='10.2.1.0/24'))

        self.assertEqual(sorted(self.csbgppeers.peers.keys()), [64512, 64513])
        self.assertEqual(len(self.csbgppeers.peers[64512]['ip4_peers']), 1)
        self.assertEqual(len(self.csbgppeers.peers[64512]['ip6_peers']), 1)
        self.assertEqual(len(self.csbgppeers.peers[64513]['ip4_peers']), 1)
        self.assertEqual(len(self.csbgppeers.peers[64513]['ip6_peers']), 0)

    def test_process_dbag_item_dual_stack(self):
        self.csbgppeers._process_dbag_item(self._peer(ip4_address='100.64.0.1',
                                                      guest_ip4_cidr='10.1.1.0/24',
                                                      ip6_address='2001:db8::1',
                                                      guest_ip6_cidr='2001:db8:100::/64'))

        self.assertEqual(len(self.csbgppeers.peers[64512]['ip4_peers']), 1)
        self.assertEqual(len(self.csbgppeers.peers[64512]['ip6_peers']), 1)

    def test_access_list_set(self):
        self.csbgppeers._process_dbag_item(self._peer(ip4_address='100.64.0.1',
                                                      guest_ip4_cidr='10.1.1.0/24',
                                                      ip6_address='2001:db8::1',
                                                      guest_ip6_cidr='2001:db8:100::/64'))
        self.csbgppeers._access_list_set()

        config = self._frr_conf()
        self.assertIn("ip prefix-list all-v4 seq 1 permit any", config)
        self.assertIn("ip prefix-list default-v4 seq 1 permit 0.0.0.0/0", config)
        self.assertIn("ipv6 prefix-list all-v6 seq 1 permit any", config)
        self.assertIn("ipv6 prefix-list default-v6 seq 1 permit ::/0", config)
        self.assertIn("ip prefix-list local-v4 seq 1 permit 10.1.1.0/24", config)
        self.assertIn("ipv6 prefix-list local-v6 seq 1 permit 2001:db8:100::/64", config)

    def test_access_list_set_deduplicates_cidrs(self):
        # Two peers announcing the same guest CIDR should only result in a single prefix-list entry
        self.csbgppeers._process_dbag_item(self._peer(ip4_address='100.64.0.1',
                                                      guest_ip4_cidr='10.1.1.0/24'))
        self.csbgppeers._process_dbag_item(self._peer(peer_id=2,
                                                      ip4_address='100.64.0.2',
                                                      guest_ip4_cidr='10.1.1.0/24'))
        self.csbgppeers._access_list_set()

        config = self._frr_conf()
        local_v4 = [line for line in config if line.startswith("ip prefix-list local-v4")]
        self.assertEqual(local_v4, ["ip prefix-list local-v4 seq 1 permit 10.1.1.0/24"])

    def test_process_peers_ip4(self):
        self.csbgppeers._process_dbag_item(self._peer(ip4_address='100.64.0.1',
                                                      guest_ip4_cidr='10.1.1.0/24'))
        self.csbgppeers._process_peers()

        config = self._frr_conf()
        self.assertIn("router bgp 64512", config)
        self.assertIn(" bgp router-id 100.64.0.10", config)
        self.assertIn(" neighbor 100.64.0.1 remote-as 64496", config)
        self.assertIn(" neighbor 100.64.0.1 route-map upstream-v4-in in", config)
        self.assertIn(" neighbor 100.64.0.1 route-map upstream-v4-out out", config)
        self.assertIn("  network 10.1.1.0/24", config)
        self.assertNotIn(" bgp default ipv6-unicast", config)

    def test_process_peers_ip6(self):
        self.csbgppeers._process_dbag_item(self._peer(ip6_address='2001:db8::1',
                                                      guest_ip6_cidr='2001:db8:100::/64'))
        self.csbgppeers._process_peers()

        config = self._frr_conf()
        self.assertIn(" bgp default ipv6-unicast", config)
        self.assertIn(" neighbor 2001:db8::1 remote-as 64496", config)
        self.assertIn(" neighbor 2001:db8::1 route-map upstream-v6-in in", config)
        self.assertIn(" neighbor 2001:db8::1 route-map upstream-v6-out out", config)
        self.assertIn("  network 2001:db8:100::/64", config)

    def test_process_peers_password_and_multihop(self):
        self.csbgppeers._process_dbag_item(self._peer(ip4_address='100.64.0.1',
                                                      guest_ip4_cidr='10.1.1.0/24',
                                                      peer_password='S3cr3t!',
                                                      details={'EBGP_MultiHop': 2}))
        self.csbgppeers._process_peers()

        config = self._frr_conf()
        self.assertIn(" neighbor 100.64.0.1 password S3cr3t!", config)
        self.assertIn(" neighbor 100.64.0.1 ebgp-multihop 2", config)

    def test_route_map_set(self):
        self.csbgppeers._route_map_set()

        expected = [
            "route-map upstream-v4-in permit 10",
            "  match ip address prefix-list default-v4",
            "route-map upstream-v4-in deny 1000",
            "  match ip address prefix-list all-v4",
            "route-map upstream-v4-out permit 10",
            "  match ip address prefix-list local-v4",
            "route-map upstream-v4-out deny 1000",
            "  match ip address prefix-list all-v4",
            "route-map upstream-v6-in permit 10",
            "  match ipv6 address prefix-list default-v6",
            "route-map upstream-v6-in deny 1000",
            "  match ipv6 address prefix-list all-v6",
            "route-map upstream-v6-out permit 10",
            "  match ipv6 address prefix-list local-v6",
            "route-map upstream-v6-out deny 1000",
            "  match ipv6 address prefix-list all-v6"
        ]
        self.assertEqual(self._frr_conf(), expected)

    @mock.patch('cs.CsBgpPeers.CsHelper.get_hostname')
    def test_full_frr_conf(self, mock_hostname):
        mock_hostname.return_value = "r-1001-VM"
        self.csbgppeers._process_dbag_item(self._peer(ip4_address='100.64.0.1',
                                                      guest_ip4_cidr='10.1.1.0/24',
                                                      ip6_address='2001:db8::1',
                                                      guest_ip6_cidr='2001:db8:100::/64'))
        self.csbgppeers._pre_set()
        self.csbgppeers._access_list_set()
        self.csbgppeers._process_peers()
        self.csbgppeers._route_map_set()
        self.csbgppeers._post_set()

        expected = [
            "frr defaults traditional",
            "hostname r-1001-VM",
            "service integrated-vtysh-config",
            "ip nht resolve-via-default",
            "ip prefix-list all-v4 seq 1 permit any",
            "ip prefix-list default-v4 seq 1 permit 0.0.0.0/0",
            "ipv6 prefix-list all-v6 seq 1 permit any",
            "ipv6 prefix-list default-v6 seq 1 permit ::/0",
            "ip prefix-list local-v4 seq 1 permit 10.1.1.0/24",
            "ipv6 prefix-list local-v6 seq 1 permit 2001:db8:100::/64",
            "router bgp 64512",
            " bgp router-id 100.64.0.10",
            " bgp default ipv6-unicast",
            " neighbor 100.64.0.1 remote-as 64496",
            " neighbor 100.64.0.1 route-map upstream-v4-in in",
            " neighbor 100.64.0.1 route-map upstream-v4-out out",
            " neighbor 2001:db8::1 remote-as 64496",
            " neighbor 2001:db8::1 route-map upstream-v6-in in",
            " neighbor 2001:db8::1 route-map upstream-v6-out out",
            " address-family ipv4 unicast",
            "  network 10.1.1.0/24",
            " exit-address-family",
            " address-family ipv6 unicast",
            "  network 2001:db8:100::/64",
            " exit-address-family",
            "route-map upstream-v4-in permit 10",
            "  match ip address prefix-list default-v4",
            "route-map upstream-v4-in deny 1000",
            "  match ip address prefix-list all-v4",
            "route-map upstream-v4-out permit 10",
            "  match ip address prefix-list local-v4",
            "route-map upstream-v4-out deny 1000",
            "  match ip address prefix-list all-v4",
            "route-map upstream-v6-in permit 10",
            "  match ipv6 address prefix-list default-v6",
            "route-map upstream-v6-in deny 1000",
            "  match ipv6 address prefix-list all-v6",
            "route-map upstream-v6-out permit 10",
            "  match ipv6 address prefix-list local-v6",
            "route-map upstream-v6-out deny 1000",
            "  match ipv6 address prefix-list all-v6",
            "line vty"
        ]
        self.assertEqual(self._frr_conf(), expected)


if __name__ == '__main__':
    unittest.main()
