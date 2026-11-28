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

import ipaddress
import re
import unittest
from unittest import mock

from configure import CsRemoteAccessVpn

PROTO = CsRemoteAccessVpn.CLIENT_ROUTE_PROTO
# CloudStack's default client range: local_ip 10.1.2.1, pool 10.1.2.2-10.1.2.8
CLIENT_NETS = ["10.1.2.1/32", "10.1.2.2/31", "10.1.2.4/30", "10.1.2.8/32"]
POOL_NETS = ["10.1.2.2/31", "10.1.2.4/30", "10.1.2.8/32"]


class FakeKernel:
    """Policy rules and routes (type, dst, table, proto), with output shaped like iproute2's."""

    def __init__(self):
        self.rules = ["0:\tfrom all lookup local", "220:\tfrom all lookup 220",
                      "32759:\tfrom 198.51.100.128/25 lookup Table_eth2", "32760:\tfrom all fwmark 0x67 lookup Table_eth3",
                      "32761:\tfrom 203.0.113.0/28 lookup Table_eth1", "32762:\tfrom all fwmark 0x66 lookup Table_eth2",
                      "32763:\tfrom all fwmark 0x65 lookup Table_eth1", "32766:\tfrom all lookup main"]
        self.routes = {("throw", "198.51.100.128/25", "Table_eth2", "static"),
                       ("throw", "192.0.2.0/24", "Table_eth2", "static"),
                       ("throw", "192.0.2.0/24", "Table_eth1", "static"),
                       # not ours, must never be touched
                       ("throw", "203.0.113.128/25", "Table_eth9", "static"),
                       ("10.1.2.2 dev ppp0", None, "main", "kernel")}
        self.calls = []

    def execute(self, cmd):
        self.calls.append(cmd)
        if cmd == "ip rule show":
            return list(self.rules)
        if cmd == "ip -4 route show table all proto %s" % PROTO:
            out = []
            for typ, dst, table, proto in sorted(self.routes, key=str):
                if proto == PROTO:
                    dst = dst[:-3] if dst.endswith("/32") else dst
                    out.append("%s %s%s" % (typ, dst, "" if table == "main" else " table " + table))
            return out
        m = re.fullmatch(r"ip route (add|delete) throw (\S+) table (\S+) proto (\d+)", cmd)
        if not m:
            raise AssertionError("unexpected command: " + cmd)
        route = ("throw", str(ipaddress.ip_network(m.group(2))), m.group(3), m.group(4))
        if m.group(1) == "add":
            if any(r[:3] == route[:3] for r in self.routes):
                raise AssertionError("route exists: " + cmd)
            self.routes.add(route)
        else:
            self.routes.remove(route)
        return []

    def owned(self):
        return {(dst, table) for typ, dst, table, proto in self.routes if proto == PROTO}

    def foreign(self):
        return {r for r in self.routes if r[3] != PROTO}

    def changes(self):
        return [c for c in self.calls if c.startswith("ip route ")]


class FakeConfig:

    def __init__(self, networks):
        self.networks = networks

    def is_vpc(self):
        return True

    def address(self):
        interfaces = [mock.Mock(**{"get_network.return_value": n}) for n in self.networks]
        return mock.Mock(**{"get_interfaces.return_value": interfaces})


class TestCsRemoteAccessVpnClientRoutes(unittest.TestCase):

    def setUp(self):
        self.kernel = FakeKernel()
        self.static_routes = {"id": "staticroutes"}
        self.peers = {"id": "site2sitevpn"}
        self.vpn_entry = {"create": True, "local_ip": "10.1.2.1", "ip_range": "10.1.2.2-10.1.2.8",
                          "local_cidr": "192.0.2.0/24", "public_interface": "eth1"}
        self.dbag = {"id": "remoteaccessvpn", "203.0.113.10": self.vpn_entry}
        self.config = FakeConfig(["203.0.113.0/28", "198.51.100.128/25", "192.0.2.0/24"])
        for name, value in (("CsHelper.execute", self.kernel.execute),
                            ("CsStaticRoutes", lambda *a: mock.Mock(**{"get_bag.return_value": self.static_routes})),
                            ("CsSite2SiteVpn", lambda *a: mock.Mock(**{"get_bag.return_value": self.peers}))):
            patcher = mock.patch("configure." + name, value)
            patcher.start()
            self.addCleanup(patcher.stop)
        self.foreign = self.kernel.foreign()

    def vpn(self):
        vpn = CsRemoteAccessVpn.__new__(CsRemoteAccessVpn)
        vpn.dbag = self.dbag
        vpn.config = self.config
        vpn.fw = []
        return vpn

    def run_pass(self):
        self.kernel.calls = []
        self.vpn().client_routes()
        self.assertEqual(self.kernel.foreign(), self.foreign)
        return self.kernel.changes()

    def expected(self, nets=CLIENT_NETS, tables=("Table_eth1", "Table_eth2", "Table_eth3")):
        return {(n, t) for n in nets for t in tables}

    def test_throws_in_every_nic_table(self):
        self.assertEqual(len(self.run_pass()), 12)
        self.assertEqual(self.kernel.owned(), self.expected())
        self.assertEqual(self.run_pass(), [])

    def test_rebuilt_table_gets_its_throws_back(self):
        self.run_pass()
        self.kernel.routes = {r for r in self.kernel.routes if r[2] != "Table_eth2"}
        self.foreign = self.kernel.foreign()
        self.assertEqual(len(self.run_pass()), 4)
        self.assertEqual(self.kernel.owned(), self.expected())

    def test_new_nic_table(self):
        self.run_pass()
        self.kernel.rules.insert(2, "219:\tfrom 198.51.100.0/26 lookup Table_eth4")
        self.assertEqual(len(self.run_pass()), 4)
        self.assertEqual(self.kernel.owned(), self.expected(tables=("Table_eth1", "Table_eth2", "Table_eth3", "Table_eth4")))

    def test_vpn_removed(self):
        self.run_pass()
        del self.dbag["203.0.113.10"]
        self.assertEqual(len(self.run_pass()), 12)
        self.assertEqual(self.kernel.owned(), set())

    def test_vpn_disabled_entry_left_in_bag(self):
        self.run_pass()
        self.vpn_entry["create"] = False
        self.run_pass()
        self.assertEqual(self.kernel.owned(), set())

    def test_range_changed(self):
        self.run_pass()
        self.vpn_entry["ip_range"] = "10.1.2.2-10.1.2.3"
        self.run_pass()
        self.assertEqual(self.kernel.owned(), self.expected(nets=["10.1.2.1/32", "10.1.2.2/31"]))

    def test_no_rules_read_changes_nothing(self):
        self.run_pass()
        self.kernel.rules = []
        self.assertEqual(self.run_pass(), [])
        self.assertEqual(self.kernel.owned(), self.expected())

    def test_unreadable_entry_adds_nothing(self):
        self.vpn_entry["ip_range"] = "not-a-range"
        self.run_pass()
        self.assertEqual(self.kernel.owned(), set())

    def test_overlapping_networks_are_skipped(self):
        # a subnet on the router, a site-to-site peer network and a static route inside the pool
        self.config.networks.append("10.1.2.8/32")
        self.peers["198.51.100.1"] = {"create": True, "peer_guest_cidr_list": "192.0.2.128/25, 10.1.2.2/32"}
        self.static_routes["10.1.2.4/31"] = {"network": "10.1.2.4/31", "revoke": False}
        self.run_pass()
        self.assertEqual(self.kernel.owned(), self.expected(nets=["10.1.2.1/32"]))
        # a removed peer and a revoked route don't count
        self.config.networks.pop()
        self.peers["198.51.100.1"]["create"] = False
        self.static_routes["10.1.2.4/31"]["revoke"] = True
        self.run_pass()
        self.assertEqual(self.kernel.owned(), self.expected())

    def test_static_route_containing_the_pool_does_not_count(self):
        self.vpn_entry["local_ip"] = "203.0.113.65"
        self.vpn_entry["ip_range"] = "203.0.113.66-203.0.113.72"
        self.static_routes["203.0.113.64/26"] = {"network": "203.0.113.64/26", "revoke": False}
        self.run_pass()
        nets = ["203.0.113.66/31", "203.0.113.68/30", "203.0.113.72/32", "203.0.113.65/32"]
        self.assertEqual(self.kernel.owned(), self.expected(nets=nets))

    def test_spoofed_client_sources_are_dropped(self):
        vpn = self.vpn()
        vpn.remoteaccessvpn_iptables("eth1", "203.0.113.10", self.vpn_entry)
        drops = [fw for fw in vpn.fw if fw[0] == "mangle" and fw[2].endswith("! -i ppp+ -j DROP")]
        self.assertEqual(drops, [["mangle", "front", "-A PREROUTING -s %s ! -i ppp+ -j DROP" % n] for n in POOL_NETS])


if __name__ == '__main__':
    unittest.main()
