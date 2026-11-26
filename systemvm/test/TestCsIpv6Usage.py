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
from cs.CsIpv6Usage import CsIpv6Usage


class FakeInterface(object):
    def __init__(self, dev, nw_type, added=True):
        self.dev, self.nw_type, self.added = dev, nw_type, added

    def get_device(self):
        return self.dev

    def is_public(self):
        return self.nw_type == "public"

    def is_guest(self):
        return self.nw_type == "guest"

    def is_added(self):
        return self.added


class FakeConfig(object):
    """ Just enough of CsConfig for CsIpv6Usage. """

    def __init__(self, kind, interfaces, exempt=None, cidr6=None, routed=False):
        self.kind, self.interfaces, self.exempt, self.cidr6, self.routed = kind, interfaces, exempt or [], cidr6 or {}, routed

    def is_vpc(self):
        return self.kind == "vpcrouter"

    def is_router(self):
        return self.kind == "router"

    def is_dhcp(self):
        return self.kind == "dhcpsrvr"

    def is_routed(self):
        return self.routed

    def get_usage_exempt_cidrs(self, version=4):
        return self.exempt if version == 6 else []

    def address(self):
        return self

    def get_interfaces(self):
        return self.interfaces

    def guestnetwork(self):
        return self

    def get_dev_ip6cidr(self, dev):
        return self.cidr6.get(dev, False)


ISOLATED = [FakeInterface("eth0", "guest"), FakeInterface("eth1", "control"), FakeInterface("eth2", "public"),
            FakeInterface("eth2", "public"), FakeInterface("eth3", "public")]


class TestCsIpv6Usage(unittest.TestCase):

    def build(self, *args, **kwargs):
        return CsIpv6Usage(FakeConfig(*args, **kwargs)).build().splitlines()

    def test_isolated_router_mirrors_the_ipv4_rules(self):
        rules = self.build("router", ISOLATED)
        self.assertIn('add rule ip6 cloudstack_usage forward iifname "eth0" oifname "eth2" counter name stats_out', rules)
        self.assertIn('add rule ip6 cloudstack_usage forward iifname "eth2" oifname "eth0" counter name stats_in', rules)
        self.assertIn('add rule ip6 cloudstack_usage forward oifname "eth2" iifname != "eth0" meta l4proto tcp counter name stats_tcp_out', rules)
        self.assertIn('add rule ip6 cloudstack_usage output oifname "eth2" meta l4proto tcp counter name stats_tcp_out', rules)
        self.assertIn('add rule ip6 cloudstack_usage input iifname "eth2" meta l4proto tcp counter name stats_tcp_in', rules)
        # each public interface also gets its own counters, like NETWORK_STATS_<dev>
        self.assertIn('add rule ip6 cloudstack_usage forward iifname "eth0" oifname "eth3" counter name eth3_out', rules)
        self.assertIn("add counter ip6 cloudstack_usage eth2_tcp_in", rules)
        self.assertEqual(1, rules.count("add counter ip6 cloudstack_usage eth2_out"))

    def test_counters_are_created_before_the_rules_that_use_them(self):
        rules = self.build("router", ISOLATED)
        last_counter = max(i for i, r in enumerate(rules) if r.startswith("add counter"))
        first_rule = min(i for i, r in enumerate(rules) if r.startswith("add rule"))
        self.assertLess(last_counter, first_rule)

    def test_rules_are_rebuilt_without_touching_the_counters(self):
        rules = self.build("router", ISOLATED)
        for hook in ["forward", "input", "output"]:
            self.assertIn("flush chain ip6 cloudstack_usage %s" % hook, rules)
        self.assertFalse([r for r in rules if r.startswith("delete") or r.startswith("reset")])

    def test_vpc_router_counts_the_tiers_prefixes(self):
        interfaces = [FakeInterface("eth1", "control"), FakeInterface("eth2", "public"),
                      FakeInterface("eth3", "guest"), FakeInterface("eth4", "guest")]
        rules = self.build("vpcrouter", interfaces, cidr6={"eth3": "2001:db8:0:3::/64", "eth4": "2001:db8:0:4::/64"})
        self.assertIn("add element ip6 cloudstack_usage vpc { 2001:db8:0:3::/64, 2001:db8:0:4::/64 }", rules)
        self.assertIn('add rule ip6 cloudstack_usage forward ip6 saddr @vpc oifname "eth2" counter name eth2_out', rules)
        self.assertIn('add rule ip6 cloudstack_usage forward ip6 daddr @vpc iifname "eth2" counter name eth2_in', rules)
        self.assertIn('add rule ip6 cloudstack_usage input ip6 daddr != @vpc iifname "eth2" meta l4proto tcp counter name eth2_tcp_in', rules)
        self.assertFalse([r for r in rules if "stats_" in r])

    def test_vpc_router_counts_its_own_traffic_on_tier_addresses(self):
        interfaces = [FakeInterface("eth2", "public"), FakeInterface("eth3", "guest")]
        rules = self.build("vpcrouter", interfaces, cidr6={"eth3": "2001:db8:0:3::/64"})
        self.assertIn('add rule ip6 cloudstack_usage input ip6 daddr @vpc iifname "eth2" counter name eth2_in', rules)
        self.assertIn('add rule ip6 cloudstack_usage output ip6 saddr @vpc oifname "eth2" counter name eth2_out', rules)

    def test_vpc_router_leaves_out_removed_tiers(self):
        interfaces = [FakeInterface("eth2", "public"), FakeInterface("eth3", "guest"), FakeInterface("eth4", "guest", added=False)]
        rules = self.build("vpcrouter", interfaces, cidr6={"eth3": "2001:db8:0:3::/64", "eth4": "2001:db8:0:4::/64"})
        self.assertIn("add element ip6 cloudstack_usage vpc { 2001:db8:0:3::/64 }", rules)

    def test_exempt_addresses_skip_forwarded_traffic_only(self):
        rules = self.build("router", ISOLATED, exempt=["2001:db8::5/128", "2001:db8:1::/48"])
        self.assertIn("add element ip6 cloudstack_usage exempt { 2001:db8::5/128, 2001:db8:1::/48 }", rules)
        exempt_rules = [r for r in rules if "@exempt" in r]
        self.assertEqual(["add rule ip6 cloudstack_usage forward ip6 daddr @exempt return",
                          "add rule ip6 cloudstack_usage forward ip6 saddr @exempt return"], exempt_rules)
        first_counter_rule = min(i for i, r in enumerate(rules) if "counter name" in r)
        self.assertLess(rules.index(exempt_rules[1]), first_counter_rule)

    def test_clearing_the_exempt_list_empties_the_set(self):
        rules = self.build("router", ISOLATED)
        self.assertIn("flush set ip6 cloudstack_usage exempt", rules)
        self.assertFalse([r for r in rules if r.startswith("add element ip6 cloudstack_usage exempt")])

    def test_routers_without_usage_counters_are_left_alone(self):
        self.assertIsNone(CsIpv6Usage(FakeConfig("dhcpsrvr", ISOLATED)).build())
        self.assertIsNone(CsIpv6Usage(FakeConfig("router", ISOLATED, routed=True)).build())


if __name__ == '__main__':
    unittest.main()
