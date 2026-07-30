#!/usr/bin/python3
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

""" Unit tests for security_group.py rule generation.

The golden test pins the exact command stream the classic (bridged) path produces. It was
regenerated once, deliberately, when --physdev-is-bridged was removed from the --physdev-in
rules: the flag restricted them to bridged traffic, which is all that can reach these chains on
a classic bridge anyway (the BF- framework hook gates on it), while bridged-then-routed traffic
on Direct Routed networks must match them too. --physdev-out rules keep the flag: the kernel
cannot know the bridged egress port for routed packets, so removing it there changes nothing.
Regenerate only when a change to the classic rules is intended:

    python3 tests/test_security_group.py --regenerate
"""

import importlib.util
import os
import sys
import unittest
from unittest import mock

HERE = os.path.dirname(os.path.abspath(__file__))
SCRIPT = os.path.join(HERE, os.pardir, 'security_group.py')
GOLDEN = os.path.join(HERE, 'golden_default_network_rules.txt')
GOLDEN_FRAMEWORK = os.path.join(HERE, 'golden_add_fw_framework.txt')

VM_ARGS = dict(vm_name="i-2-7-VM", vm_id="7", vm_ip="10.1.1.55", vm_ip6="fd00::55",
               vm_mac="02:00:4c:5f:00:01", vif="vnet3", sec_ips="0:")


def load_script():
    sys.modules.setdefault('libvirt', mock.MagicMock())
    spec = importlib.util.spec_from_file_location("security_group", SCRIPT)
    sg = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(sg)
    return sg


def capture_framework(sg, fn, brname):
    """ Run a framework function with every probe failing (so create/insert branches are taken)
    and chain references reported as zero (so hooks are installed). """
    captured = []

    def fake_execute(cmd):
        captured.append(cmd)
        if ' -L ' in cmd and 'awk' not in cmd:
            raise Exception("absent")
        if 'grep -q' in cmd:
            raise Exception("absent")
        if ' -C ' in cmd:
            raise Exception("absent")
        if 'awk' in cmd and 'references' in cmd:
            return "0"
        return ""

    sg.execute = fake_execute
    sg.get_br_fw = lambda b: "BF-" + b
    sg.get_bridge_physdev = lambda b: "eth0"
    fn(brname)
    return captured


def capture_default_network_rules(sg, brname, direct_routed):
    """ Run default_network_rules with all side effects stubbed, returning the commands the
    script would have executed. """
    captured = []

    def fake_execute(cmd):
        captured.append(cmd)
        return ""

    sg.execute = fake_execute
    sg.add_fw_framework = lambda brname: True
    sg.add_l3_fw_framework = lambda brname: True
    sg.get_vm_id = lambda name: "7"
    sg.write_rule_log_for_vm = lambda *a, **k: True
    sg.write_secip_log_for_vm = lambda *a, **k: True
    sg.delete_rules_for_vm_in_bridge_firewall_chain = lambda name: None
    sg.destroy_ebtables_rules = lambda name, vif: None

    ok = sg.default_network_rules(VM_ARGS['vm_name'], VM_ARGS['vm_id'], VM_ARGS['vm_ip'], VM_ARGS['vm_ip6'],
                                  VM_ARGS['vm_mac'], VM_ARGS['vif'], brname, VM_ARGS['sec_ips'],
                                  is_first_nic=True, direct_routed=direct_routed)
    return ok, captured


class TestClassicRulesUnchanged(unittest.TestCase):
    """ The classic path must stay byte-identical: it serves Basic zones and every Shared
    network in every existing deployment. """

    def test_default_network_rules_matches_golden(self):
        sg = load_script()
        ok, captured = capture_default_network_rules(sg, "cloudbr0", direct_routed=False)
        self.assertTrue(ok)
        with open(GOLDEN) as f:
            golden = f.read().splitlines()
        self.assertEqual(golden, captured)

    def test_classic_physdev_direction_flags(self):
        """ from-Instance rules identify the bridge port without --physdev-is-bridged (so the
        same rules work for bridged-then-routed traffic); towards-Instance rules keep it, since
        the kernel only knows the bridged egress port for bridged packets. """
        sg = load_script()
        ok, captured = capture_default_network_rules(sg, "cloudbr0", direct_routed=False)
        self.assertTrue(ok)
        for cmd in captured:
            if '--physdev-in' in cmd:
                self.assertNotIn('--physdev-is-bridged', cmd, cmd)
            if '--physdev-out' in cmd:
                self.assertIn('--physdev-is-bridged', cmd, cmd)


class TestDirectRoutedRules(unittest.TestCase):

    def setUp(self):
        self.sg = load_script()
        ok, self.captured = capture_default_network_rules(self.sg, "brdr-42", direct_routed=True)
        self.assertTrue(ok)
        self.iptables = [c for c in self.captured if c.startswith('iptables ') or c.startswith('ip6tables ')]

    def test_no_physdev_is_bridged_anywhere(self):
        # The defining property of the routed path: physdev-is-bridged matches only bridged
        # traffic, which routed traffic is not.
        for cmd in self.captured:
            self.assertNotIn('--physdev-is-bridged', cmd, cmd)

    def test_no_dhcp_rules(self):
        for cmd in self.iptables:
            for marker in ['dport 67', 'sport 67', 'dport 68', 'sport 546', 'sport 547', 'dport 546']:
                self.assertNotIn(marker, cmd, cmd)

    def test_from_instance_dispatch_uses_bridge_port(self):
        self.assertIn("iptables -A BF-brdr-42-IN -m physdev --physdev-in vnet3 -j i-2-7-def", self.captured)
        self.assertIn("ip6tables -A BF-brdr-42-IN -m physdev --physdev-in vnet3 -j i-2-7-def", self.captured)

    def test_towards_instance_dispatch_uses_ipset_destination(self):
        self.assertIn("iptables -A BF-brdr-42-OUT -m set --match-set i-2-7-VM dst -j i-2-7-def", self.captured)
        self.assertIn("ip6tables -A BF-brdr-42-OUT -m set --match-set i-2-7-VM-6 dst -j i-2-7-def", self.captured)

    def test_source_spoofing_dropped_both_families(self):
        self.assertIn("iptables -A i-2-7-def -m physdev --physdev-in vnet3 -m set ! --match-set i-2-7-VM src -j DROP", self.captured)
        self.assertIn("ip6tables -A i-2-7-def -m physdev --physdev-in vnet3 -m set ! --match-set i-2-7-VM-6 src -j DROP", self.captured)

    def test_router_advertisements_from_instance_dropped(self):
        self.assertIn("ip6tables -A i-2-7-def -m physdev --physdev-in vnet3 -p icmpv6 --icmpv6-type router-advertisement -j DROP", self.captured)

    def test_neighbor_discovery_between_instances_allowed(self):
        self.assertIn("ip6tables -A i-2-7-def -m physdev --physdev-in vnet3 -p icmpv6 --icmpv6-type neighbor-solicitation -m hl --hl-eq 255 -j ACCEPT", self.captured)
        self.assertIn("ip6tables -A i-2-7-def -m physdev --physdev-in vnet3 -p icmpv6 --icmpv6-type neighbor-advertisement -m set --match-set i-2-7-VM-6 src -m hl --hl-eq 255 -j ACCEPT", self.captured)

    def test_default_deny_in_both_directions_and_families(self):
        self.assertIn("iptables -A i-2-7-def -m physdev --physdev-in vnet3 -j DROP", self.captured)
        self.assertIn("iptables -A i-2-7-def -m set --match-set i-2-7-VM dst -j DROP", self.captured)
        self.assertIn("ip6tables -A i-2-7-def -m physdev --physdev-in vnet3 -j DROP", self.captured)
        self.assertIn("ip6tables -A i-2-7-def -m set --match-set i-2-7-VM-6 dst -j DROP", self.captured)

    def test_user_rule_chains_are_wired(self):
        self.assertIn("iptables -A i-2-7-def -m physdev --physdev-in vnet3 -m set --match-set i-2-7-VM src -j i-2-7-VM-eg", self.captured)
        self.assertIn("iptables -A i-2-7-def -m set --match-set i-2-7-VM dst -j i-2-7-VM", self.captured)

    def test_gateway_arp_protection_unchanged(self):
        # The ebtables rules are shared with the classic path: they pin the ARP *source* to the
        # Instance's own address and never filter the ARP target, so resolving the gateway works.
        ebtables = [c for c in self.captured if c.startswith('ebtables')]
        self.assertTrue(any('--arp-ip-src 10.1.1.55' in c for c in ebtables))
        self.assertFalse(any('arp-ip-dst 169.254' in c for c in ebtables))


class TestSharedFrameworkHelpers(unittest.TestCase):
    """ enable_bridge_netfilter, create_bridge_fw_chains and add_notrack_ipset_rules are shared
    by the classic and Direct Routed frameworks. The golden proves extracting them left the
    classic framework byte-identical. """

    def test_add_fw_framework_matches_golden(self):
        sg = load_script()
        captured = capture_framework(sg, sg.add_fw_framework, "cloudbr0")
        with open(GOLDEN_FRAMEWORK) as f:
            golden = f.read().splitlines()
        self.assertEqual(golden, captured)

    def test_both_frameworks_share_the_helpers(self):
        sg = load_script()
        classic = capture_framework(sg, sg.add_fw_framework, "cloudbr0")
        l3 = capture_framework(sg, sg.add_l3_fw_framework, "brdr-42")
        for stream in (classic, l3):
            self.assertIn("modprobe br_netfilter", stream)
            self.assertTrue(any('ipset -! create' in c for c in stream))
            self.assertTrue(any('-j NOTRACK' in c for c in stream))
        # the L3 hooks jump unconditionally; the classic ones gate on physdev-is-bridged
        self.assertTrue(any('-I FORWARD -i brdr-42 -j BF-brdr-42-IN' in c for c in l3))
        for cmd in l3:
            if 'FORWARD' in cmd:
                self.assertNotIn('physdev', cmd, cmd)
        self.assertTrue(any('physdev-is-bridged' in c and 'FORWARD' in c for c in classic))


class TestDirectRoutedFlagPlumbing(unittest.TestCase):
    """ The Agent decides the network type and passes --directrouted; the script never infers
    it from the bridge name, the gateway, or anything else. """

    def test_flag_reaches_default_network_rules(self):
        sg = load_script()
        self.assertFalse(hasattr(sg, 'is_direct_routed_bridge'),
                         "the script must not classify bridges itself")
        _, classic = capture_default_network_rules(sg, "brdr-42", direct_routed=False)
        # Same bridge name, flag off: the classic rules must be produced
        self.assertTrue(any('--physdev-is-bridged --physdev-out' in c for c in classic))

    def test_cli_exposes_directrouted(self):
        with open(SCRIPT) as f:
            source = f.read()
        self.assertIn('"--directrouted"', source)
        self.assertIn('dest="directRouted"', source)


def regenerate_golden():
    sg = load_script()
    ok, captured = capture_default_network_rules(sg, "cloudbr0", direct_routed=False)
    assert ok
    with open(GOLDEN, 'w') as f:
        f.write("\n".join(captured) + "\n")
    print("wrote %d commands to %s" % (len(captured), GOLDEN))


if __name__ == '__main__':
    if '--regenerate' in sys.argv:
        regenerate_golden()
    else:
        unittest.main()
