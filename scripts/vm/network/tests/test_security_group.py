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

The classic golden files pin the exact command stream the classic (bridged) path produces. They
were regenerated once, deliberately, when --physdev-is-bridged was removed from the --physdev-in
rules: the flag restricted them to bridged traffic, which is all that can reach these chains on
a classic bridge anyway (the BF- framework hook gates on it), while bridged-then-routed traffic
on Direct Routed networks must match them too. --physdev-out rules keep the flag: the kernel
cannot know the bridged egress port for routed packets, so removing it there changes nothing.
Regenerate only when a change to the classic rules is intended:

    python3 tests/test_security_group.py --regenerate

The Direct Routed (L3) tests go further than golden files: a small model of iptables applies
the emitted commands and walks packets through the resulting chains, so the two-pass filtering
(source egress, then destination ingress) is checked for every traffic case on a host with two
L3 bridges, created in either order.
"""

import importlib.util
import inspect
import ipaddress
import os
import shlex
import subprocess
import sys
import tempfile
import unittest
from unittest import mock

HERE = os.path.dirname(os.path.abspath(__file__))
SCRIPT = os.path.join(HERE, os.pardir, 'security_group.py')
GOLDEN = os.path.join(HERE, 'golden_default_network_rules.txt')
GOLDEN_FRAMEWORK = os.path.join(HERE, 'golden_add_fw_framework.txt')
GOLDEN_L3 = os.path.join(HERE, 'golden_default_network_rules_l3.txt')
GOLDEN_L3_FRAMEWORK = os.path.join(HERE, 'golden_add_l3_fw_framework.txt')

VM_ARGS = dict(vm_name="i-2-7-VM", vm_id="7", vm_ip="10.1.1.55", vm_ip6="fd00::55",
               vm_mac="02:00:4c:5f:00:01", vif="vnet3", sec_ips="0:")

MARK = "0x40000000/0x40000000"
APPROVE = "-j MARK --set-xmark " + MARK


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


def capture_default_network_rules(sg, brname, direct_routed, vm_ip=VM_ARGS['vm_ip']):
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
    sg.delete_rules_for_vm_in_bridge_firewall_chain = lambda name: set()
    sg.destroy_ebtables_rules = lambda name, vif: None

    ok = sg.default_network_rules(VM_ARGS['vm_name'], VM_ARGS['vm_id'], vm_ip, VM_ARGS['vm_ip6'],
                                  VM_ARGS['vm_mac'], VM_ARGS['vif'], brname, VM_ARGS['sec_ips'],
                                  is_first_nic=True, direct_routed=direct_routed)
    return ok, captured


class IptablesModel:
    """ Applies the iptables/ip6tables/ipset commands the script emits to an in-memory table
    and evaluates packets against it. Only the matches and targets the script uses are
    understood. iptables-save pipelines are run through the shell against the modelled state, so
    the script's awk/grep/sed teardown logic is exercised as written. """

    ACCEPT, DROP = 'ACCEPT', 'DROP'

    def __init__(self):
        self.tables = {ipt: {'FORWARD': [], 'INPUT': [], 'OUTPUT': []} for ipt in ('iptables', 'ip6tables')}
        self.ipsets = {}
        self.commands = []

    # --- command application -------------------------------------------------------------

    def execute(self, cmd):
        self.commands.append(cmd)
        first = cmd.split()[0]
        if first in ('iptables-save', 'ip6tables-save'):
            return self._save_pipeline(first[:-5], cmd)
        if first in ('iptables', 'ip6tables'):
            if ' -t raw ' in cmd or ' -t nat ' in cmd:
                if 'grep -q' in cmd:
                    raise Exception("absent")
                return ""
            return self._iptables(first, shlex.split(cmd)[1:])
        if first == 'ipset':
            return self._ipset(shlex.split(cmd)[1:])
        return ""

    def _iptables(self, ipt, args):
        chains = self.tables[ipt]
        op, chain = args[0], args[1]
        rest = args[2:]
        if op == '-N':
            if chain in chains:
                raise Exception("exists")
            chains[chain] = []
        elif op == '-L':
            if chain not in chains:
                raise Exception("absent")
        elif op == '-S':
            return "\n".join(["-N %s" % chain] + ["-A %s %s" % (chain, r) for r in chains[chain]]) + "\n"
        elif op == '-F':
            chains[chain] = []
        elif op == '-X':
            for rules in chains.values():
                for r in rules:
                    if r.split()[-1] == chain:
                        raise Exception("referenced")
            del chains[chain]
        elif op == '-A':
            chains[chain].append(" ".join(rest))
        elif op == '-I':
            pos = 0
            if rest and rest[0].isdigit():
                pos = int(rest[0]) - 1
                rest = rest[1:]
            chains[chain].insert(pos, " ".join(rest))
        elif op == '-C':
            if " ".join(rest) not in chains.get(chain, []):
                raise Exception("absent")
        elif op == '-D':
            if len(rest) == 1 and rest[0].isdigit():
                del chains[chain][int(rest[0]) - 1]
            else:
                chains[chain].remove(" ".join(rest))
        else:
            raise Exception("unsupported: %s" % " ".join(args))
        return ""

    def _ipset(self, args):
        args = [a for a in args if a != '-!']
        op = args[0]
        if op in ('-N', 'create'):
            self.ipsets.setdefault(args[1], set())
        elif op in ('-A', 'add'):
            self.ipsets.setdefault(args[1], set()).add(args[2])
        elif op in ('-D', 'del'):
            self.ipsets.get(args[1], set()).discard(args[2])
        elif op == '-F':
            if args[1] not in self.ipsets:
                raise Exception("absent")
            self.ipsets[args[1]] = set()
        elif op == '-X':
            if args[1] not in self.ipsets:
                raise Exception("absent")
            del self.ipsets[args[1]]
        return ""

    def save(self, ipt):
        out = ["*filter"]
        for chain in self.tables[ipt]:
            out.append(":%s - [0:0]" % chain)
        for chain, rules in self.tables[ipt].items():
            for r in rules:
                out.append("-A %s %s" % (chain, r))
        out.append("COMMIT")
        return "\n".join(out) + "\n"

    def _save_pipeline(self, ipt, cmd):
        with tempfile.NamedTemporaryFile('w', suffix='.save', delete=False) as f:
            f.write(self.save(ipt))
            path = f.name
        try:
            shell = cmd.replace(ipt + "-save", "cat " + shlex.quote(path), 1)
            res = subprocess.run(shell, shell=True, capture_output=True, text=True)
            if res.returncode != 0:
                raise Exception("pipeline failed")
            return res.stdout
        finally:
            os.unlink(path)

    # --- packet evaluation ---------------------------------------------------------------

    def _match(self, rule, pkt):
        tokens = shlex.split(rule)
        conds = []
        neg = False
        target = None
        i = 0
        while i < len(tokens):
            t = tokens[i]
            if t == '!':
                neg = True
                i += 1
                continue
            if t == '-m':
                i += 2
                continue
            if t in ('-j', '-g'):
                target = " ".join(tokens[i + 1:])
                break
            if t in ('--physdev-is-bridged', '--physdev-is-in', '--physdev-is-out'):
                conds.append((pkt.get('bridged', False), neg))
                neg = False
                i += 1
                continue
            val = tokens[i + 1]
            if t == '-i':
                res = pkt.get('iif') == val
            elif t == '-o':
                res = pkt.get('oif') == val
            elif t == '--physdev-in':
                res = pkt.get('physdev_in') == val
            elif t == '--physdev-out':
                res = pkt.get('physdev_out') == val
            elif t == '--match-set':
                direction = tokens[i + 2]
                addr = pkt.get('src' if direction == 'src' else 'dst')
                res = addr in self.ipsets.get(val, set())
                i += 1
            elif t == '--mark':
                value, mask = (int(x, 16) for x in val.split('/'))
                res = (pkt.get('mark', 0) & mask) == value
            elif t == '--state':
                res = pkt.get('established', False)
            elif t == '-p':
                res = pkt.get('proto') == val
            elif t in ('--dport', '--sport'):
                lo, _, hi = val.partition(':')
                port = pkt.get(t[2:])
                res = port is not None and int(lo) <= port <= int(hi or lo)
            elif t in ('--icmp-type', '--icmpv6-type'):
                res = pkt.get('icmp_type') == val
            elif t == '--hl-eq':
                res = pkt.get('hl') == int(val)
            elif t in ('-s', '--src', '-d', '--dst'):
                addr = pkt.get('src' if t in ('-s', '--src') else 'dst')
                res = addr is not None and ipaddress.ip_address(addr) in ipaddress.ip_network(val, strict=False)
            else:
                raise Exception("unsupported match %s in %s" % (t, rule))
            conds.append((res, neg))
            neg = False
            i += 2
        return all(res != n for res, n in conds), target

    def _walk(self, ipt, chain, pkt, depth=0):
        if depth > 16:
            raise Exception("loop")
        for rule in list(self.tables[ipt][chain]):
            matched, target = self._match(rule, pkt)
            if not matched or target is None:
                continue
            if target in (self.ACCEPT, self.DROP):
                return target
            if target == 'RETURN':
                return None
            if target.startswith('MARK --set-xmark '):
                value, mask = (int(x, 16) for x in target.split()[-1].split('/'))
                pkt['mark'] = (pkt.get('mark', 0) & ~mask) | value
                continue
            verdict = self._walk(ipt, target, pkt, depth + 1)
            if verdict is not None:
                return verdict
        return None

    def forward(self, pkt, ipt='iptables'):
        """ Verdict FORWARD gives the packet, or None when it falls through to the policy. """
        return self._walk(ipt, 'FORWARD', dict(pkt))


A = dict(name="i-2-7-VM", vif="vnet7", ip="10.1.1.7", ip6="fd00::7", mac="02:00:4c:5f:00:07", br="brdr-43")
B = dict(name="i-2-8-VM", vif="vnet8", ip="10.1.1.8", ip6="fd00::8", mac="02:00:4c:5f:00:08", br="brdr-42")


def program_l3_host(order):
    """ Two Direct Routed Instances on two bridges, programmed in the given order.
    A may send tcp/80 anywhere over IPv4 and to fd00::/64 over IPv6, plus tcp/443 over IPv4;
    B may send anything. A accepts tcp/22 from 10.1.1.0/24; B accepts tcp/22 and tcp/80 from
    10.1.1.0/24. Neither accepts anything over IPv6. """
    sg = load_script()
    model = IptablesModel()
    sg.execute = model.execute
    sg.get_br_fw = lambda b: "BF-" + b
    sg.get_vm_id = lambda name: "1"
    sg.write_rule_log_for_vm = lambda *a, **k: True
    sg.write_secip_log_for_vm = lambda *a, **k: True
    sg.check_rule_log_for_vm = lambda *a, **k: [True] * 6
    sg.remove_rule_log_for_vm = lambda *a, **k: True
    sg.remove_secip_log_for_vm = lambda *a, **k: True

    for vm in order:
        ok = sg.default_network_rules(vm['name'], "1", vm['ip'], vm['ip6'], vm['mac'], vm['vif'], vm['br'], "0:",
                                      is_first_nic=True, direct_routed=True)
        assert ok
    rules = {A['name']: "E:tcp;80;80;0.0.0.0/0,fd00::/64,NEXT;E:tcp;443;443;0.0.0.0/0,NEXT;I:tcp;22;22;10.1.1.0/24,NEXT;",
             B['name']: "I:tcp;22;22;10.1.1.0/24,NEXT;I:tcp;80;80;10.1.1.0/24,NEXT;"}
    for vm in order:
        ok = sg.add_network_rules(vm['name'], "1", vm['ip'], vm['ip6'], "sig", "1", vm['mac'], rules[vm['name']],
                                  vm['vif'], vm['br'], "0:", direct_routed=True)
        assert ok
    return sg, model


def pkt(src_vm, dst_vm=None, dst=None, proto='tcp', dport=80, src=None, oif='eth0', iif='eth0', physdev_in=None):
    """ An IPv4 packet in FORWARD; the fabric is eth0 on whichever side has no Instance. """
    p = dict(proto=proto, dport=dport)
    if src_vm:
        p.update(iif=src_vm['br'], physdev_in=physdev_in or src_vm['vif'], src=src or src_vm['ip'])
    else:
        p.update(iif=iif, src=src)
    if dst_vm:
        p.update(oif=dst_vm['br'], dst=dst or dst_vm['ip'])
    else:
        p.update(oif=oif, dst=dst)
    return p


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

    def test_classic_rules_carry_no_marks(self):
        sg = load_script()
        _, captured = capture_default_network_rules(sg, "cloudbr0", direct_routed=False)
        for cmd in captured:
            self.assertNotIn('mark', cmd.lower(), cmd)


class TestDirectRoutedRules(unittest.TestCase):

    def setUp(self):
        self.sg = load_script()
        ok, self.captured = capture_default_network_rules(self.sg, "brdr-42", direct_routed=True)
        self.assertTrue(ok)
        self.iptables = [c for c in self.captured if c.startswith('iptables ') or c.startswith('ip6tables ')]

    def test_matches_golden(self):
        with open(GOLDEN_L3) as f:
            golden = f.read().splitlines()
        self.assertEqual(golden, self.captured)

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

    def test_from_instance_allows_only_mark(self):
        """ A from-Instance allow must not end evaluation: the destination Instance's ingress
        rules still have to run. Approval is a packet mark, never ACCEPT. """
        from_vm = [c for c in self.iptables if '--physdev-in' in c]
        self.assertTrue(from_vm)
        for cmd in from_vm:
            self.assertNotIn('-j ACCEPT', cmd, cmd)
        self.assertIn("ip6tables -A i-2-7-def -m physdev --physdev-in vnet3 -p icmpv6 --icmpv6-type neighbor-solicitation -m hl --hl-eq 255 " + APPROVE, self.captured)
        self.assertIn("iptables -A i-2-7-def -m physdev --physdev-in vnet3 -m state --state ESTABLISHED,RELATED " + APPROVE, self.captured)

    def test_approved_packets_return_before_every_from_instance_drop_that_follows_an_allow(self):
        for ipt in ['iptables', 'ip6tables']:
            chain = [c for c in self.captured if c.startswith(ipt + ' -A i-2-7-def ')]
            check = ipt + " -A i-2-7-def -m physdev --physdev-in vnet3 -m mark --mark " + MARK + " -j RETURN"
            approved = False
            for cmd in chain:
                if APPROVE in cmd:
                    approved = True
                if cmd == check:
                    approved = False
                if '--physdev-in' in cmd and cmd.endswith('-j DROP'):
                    self.assertFalse(approved, "approved packet could be dropped by: " + cmd)

    def test_towards_instance_verdicts_are_terminal(self):
        self.assertIn("iptables -A i-2-7-def -m set --match-set i-2-7-VM dst -m state --state ESTABLISHED,RELATED -j ACCEPT", self.captured)
        self.assertIn("iptables -A i-2-7-def -m set --match-set i-2-7-VM dst -j DROP", self.captured)
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


class TestDirectRoutedEgressRules(unittest.TestCase):
    """ add_network_rules: egress rules on the L3 path mark instead of accepting; ingress rules
    accept as always. """

    def capture(self, direct_routed, rules):
        sg = load_script()
        captured = []
        sg.execute = lambda cmd: captured.append(cmd) or ""
        sg.get_vm_id = lambda name: "7"
        sg.check_rule_log_for_vm = lambda *a, **k: [True] * 6
        sg.write_rule_log_for_vm = lambda *a, **k: True
        ok = sg.add_network_rules("i-2-7-VM", "7", "10.1.1.55", "fd00::55", "sig", "1", VM_ARGS['vm_mac'], rules,
                                  "vnet3", "brdr-42", "0:", direct_routed=direct_routed)
        self.assertTrue(ok)
        return captured

    def test_l3_egress_rules_mark(self):
        captured = self.capture(True, "E:tcp;80;80;10.1.1.0/24,fd00::/64,NEXT;I:tcp;22;22;0.0.0.0/0,NEXT;")
        self.assertIn("iptables -I i-2-7-VM-eg -p tcp -m tcp --dport 80:80 -d 10.1.1.0/24 " + APPROVE, captured)
        self.assertIn("ip6tables -I i-2-7-VM-eg -p tcp -m tcp --dport 80:80 -d fd00::/64 " + APPROVE, captured)
        self.assertIn("iptables -I i-2-7-VM -p tcp -m tcp --dport 22:22 -s 0.0.0.0/0 -j ACCEPT", captured)
        self.assertIn("iptables -A i-2-7-VM-eg -j RETURN", captured)
        for cmd in captured:
            if 'i-2-7-VM-eg' in cmd:
                self.assertNotIn('ACCEPT', cmd, cmd)

    def test_l3_without_egress_rules_marks_everything(self):
        captured = self.capture(True, "I:tcp;22;22;0.0.0.0/0,NEXT;")
        self.assertIn("iptables -A i-2-7-VM-eg " + APPROVE, captured)
        self.assertIn("ip6tables -A i-2-7-VM-eg " + APPROVE, captured)

    def test_classic_egress_rules_accept(self):
        captured = self.capture(False, "E:tcp;80;80;10.1.1.0/24,NEXT;")
        self.assertIn("iptables -I i-2-7-VM-eg -p tcp -m tcp --dport 80:80 -d 10.1.1.0/24 -j ACCEPT", captured)
        self.assertIn("ip6tables -A i-2-7-VM-eg -j ACCEPT", captured)
        for cmd in captured:
            self.assertNotIn('MARK', cmd, cmd)


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

    def test_add_l3_fw_framework_matches_golden(self):
        sg = load_script()
        captured = capture_framework(sg, sg.add_l3_fw_framework, "brdr-42")
        with open(GOLDEN_L3_FRAMEWORK) as f:
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
        # the L3 framework hooks FORWARD once, into a shared chain; the classic one gates on
        # physdev-is-bridged per bridge
        self.assertIn("iptables -I FORWARD -j BF-L3", l3)
        for cmd in l3:
            if 'FORWARD' in cmd:
                self.assertNotIn('physdev', cmd, cmd)
                self.assertNotIn('brdr', cmd, cmd)
        self.assertTrue(any('physdev-is-bridged' in c and 'FORWARD' in c for c in classic))


class TestDirectRoutedFramework(unittest.TestCase):
    """ Structure of the shared L3 chains after adding two bridges, in both orders. """

    def build(self, order):
        sg = load_script()
        model = IptablesModel()
        sg.execute = model.execute
        sg.get_br_fw = lambda b: "BF-" + b
        for br in order:
            sg.add_l3_fw_framework(br)
        return sg, model

    def check_structure(self, model, bridges):
        for ipt in ['iptables', 'ip6tables']:
            t = model.tables[ipt]
            self.assertEqual(t['FORWARD'][0], "-j BF-L3")
            self.assertEqual(t['FORWARD'].count("-j BF-L3"), 1)
            self.assertEqual(t['BF-L3'], ["-j MARK --set-xmark 0x0/0x40000000",
                                          "-j BF-L3-IN",
                                          "-j BF-L3-OUT",
                                          "-m mark --mark " + MARK + " -j ACCEPT"])
            for br in bridges:
                jump_in = "-i %s -j BF-%s-IN" % (br, br)
                drop_in = "-i %s -m mark ! --mark %s -j DROP" % (br, MARK)
                self.assertLess(t['BF-L3-IN'].index(jump_in), t['BF-L3-IN'].index(drop_in))
                jump_out = "-o %s -j BF-%s-OUT" % (br, br)
                drop_out = "-o %s -j DROP" % br
                self.assertLess(t['BF-L3-OUT'].index(jump_out), t['BF-L3-OUT'].index(drop_out))
            self.assertEqual(len(t['BF-L3-IN']), 2 * len(bridges))
            self.assertEqual(len(t['BF-L3-OUT']), 2 * len(bridges))
            for rule in t['BF-L3-IN'] + t['BF-L3-OUT']:
                self.assertNotIn('ACCEPT', rule)

    def test_structure_independent_of_creation_order(self):
        for order in (["brdr-42", "brdr-43"], ["brdr-43", "brdr-42"]):
            _, model = self.build(order)
            self.check_structure(model, order)

    def test_idempotent(self):
        sg, model = self.build(["brdr-42", "brdr-43"])
        before = {ipt: {c: list(r) for c, r in t.items()} for ipt, t in model.tables.items()}
        sg.add_l3_fw_framework("brdr-42")
        sg.add_l3_fw_framework("brdr-43")
        self.assertEqual(before, model.tables)

    def test_hook_is_moved_back_to_the_top(self):
        sg, model = self.build(["brdr-42"])
        for ipt in ['iptables', 'ip6tables']:
            model.tables[ipt]['FORWARD'].insert(0, "-o cloudbr0 -j DROP")
            model.tables[ipt]['FORWARD'].insert(0, "-i cloudbr0 -j DROP")
        sg.add_l3_fw_framework("brdr-43")
        self.check_structure(model, ["brdr-42", "brdr-43"])
        self.assertEqual(model.tables['iptables']['FORWARD'][1:], ["-i cloudbr0 -j DROP", "-o cloudbr0 -j DROP"])

    def test_missing_body_rule_rebuilds_the_body(self):
        sg, model = self.build(["brdr-42"])
        model.tables['iptables']['BF-L3'].remove("-j BF-L3-OUT")
        sg.add_l3_fw_framework("brdr-42")
        self.check_structure(model, ["brdr-42"])

    def test_missing_backstop_is_appended_below_the_jump(self):
        sg, model = self.build(["brdr-42", "brdr-43"])
        model.tables['iptables']['BF-L3-IN'].remove("-i brdr-42 -m mark ! --mark %s -j DROP" % MARK)
        model.tables['iptables']['BF-L3-IN'].remove("-i brdr-42 -j BF-brdr-42-IN")
        model.tables['iptables']['BF-L3-IN'].append("-i brdr-42 -m mark ! --mark %s -j DROP" % MARK)
        sg.add_l3_fw_framework("brdr-42")
        self.check_structure(model, ["brdr-42", "brdr-43"])


class TestDirectRoutedTraversal(unittest.TestCase):
    """ Walk packets through the modelled tables of a host with A on brdr-43 and B on brdr-42.
    Every case must hold whichever bridge was set up first. """

    def for_both_orders(self, check):
        for order in ((A, B), (B, A)):
            sg, model = program_l3_host(order)
            check(model, "order %s" % [vm['name'] for vm in order])

    def test_instance_to_fabric(self):
        def check(m, ctx):
            self.assertEqual(m.forward(pkt(A, dst="8.8.8.8", dport=80)), 'ACCEPT', ctx)
            self.assertEqual(m.forward(pkt(A, dst="8.8.8.8", dport=22)), 'DROP', ctx)
            self.assertEqual(m.forward(pkt(B, dst="8.8.8.8", dport=22)), 'ACCEPT', ctx)
            # spoofed source
            self.assertEqual(m.forward(pkt(A, dst="8.8.8.8", src="10.1.1.99")), 'DROP', ctx)
            # port with no registered Instance
            self.assertEqual(m.forward(pkt(A, dst="8.8.8.8", physdev_in="vnet99", src="10.1.1.99")), 'DROP', ctx)
            self.assertEqual(m.forward(pkt(A, dst="8.8.8.8", physdev_in="vnet99", src="10.1.1.7")), 'DROP', ctx)
        self.for_both_orders(check)

    def test_fabric_to_instance(self):
        def check(m, ctx):
            self.assertEqual(m.forward(pkt(None, A, src="10.1.1.50", dport=22)), 'ACCEPT', ctx)
            self.assertEqual(m.forward(pkt(None, A, src="10.1.1.50", dport=80)), 'DROP', ctx)
            self.assertEqual(m.forward(pkt(None, A, src="203.0.113.5", dport=22)), 'DROP', ctx)
            self.assertEqual(m.forward(pkt(None, B, src="10.1.1.50", dport=80)), 'ACCEPT', ctx)
            self.assertEqual(m.forward(pkt(None, B, src="203.0.113.5", dport=80)), 'DROP', ctx)
            # unknown destination on an L3 bridge
            self.assertEqual(m.forward(pkt(None, B, src="203.0.113.5", dst="10.1.1.200")), 'DROP', ctx)
            # replies to connections the Instance opened
            p = pkt(None, A, src="8.8.8.8", dport=51000)
            p['established'] = True
            self.assertEqual(m.forward(p), 'ACCEPT', ctx)
        self.for_both_orders(check)

    def test_instance_to_instance_evaluates_both_sides(self):
        def check(m, ctx):
            # both sides allow tcp/80
            self.assertEqual(m.forward(pkt(A, B, dport=80)), 'ACCEPT', ctx)
            # B's ingress allows tcp/22 from A, A's egress does not: the source side decides
            self.assertEqual(m.forward(pkt(A, B, dport=22)), 'DROP', ctx)
            # A's egress allows tcp/443, B's ingress does not: the destination side decides
            self.assertEqual(m.forward(pkt(A, B, dport=443)), 'DROP', ctx)
            # B's egress allows everything, so in the reverse direction A's ingress decides
            self.assertEqual(m.forward(pkt(B, A, dport=22)), 'ACCEPT', ctx)
            self.assertEqual(m.forward(pkt(B, A, dport=80)), 'DROP', ctx)
            # spoofing between Instances
            self.assertEqual(m.forward(pkt(A, B, dport=80, src="10.1.1.99")), 'DROP', ctx)
            # an address on B's bridge that no Instance claims
            self.assertEqual(m.forward(pkt(A, dst="10.1.1.200", oif="brdr-42", dport=80)), 'DROP', ctx)
            # return traffic of an accepted connection
            p = pkt(B, A, dport=51000)
            p['established'] = True
            self.assertEqual(m.forward(p), 'ACCEPT', ctx)
        self.for_both_orders(check)

    def test_same_bridge_instances(self):
        """ Two Instances of one network: same bridge on both sides. """
        def check(m, ctx):
            b_on_43 = dict(B, br="brdr-43")
            t = m.tables['iptables']
            for suffix in ['-IN', '-OUT']:
                for rule in list(t['BF-brdr-42' + suffix]):
                    if 'i-2-8-def' in rule:
                        t['BF-brdr-42' + suffix].remove(rule)
                        t['BF-brdr-43' + suffix].append(rule)
            self.assertEqual(m.forward(pkt(A, b_on_43, dport=80)), 'ACCEPT', ctx)
            self.assertEqual(m.forward(pkt(A, b_on_43, dport=22)), 'DROP', ctx)
            self.assertEqual(m.forward(pkt(b_on_43, A, dport=22)), 'ACCEPT', ctx)
            self.assertEqual(m.forward(pkt(b_on_43, A, dport=80)), 'DROP', ctx)
        self.for_both_orders(check)

    def test_ipv6(self):
        def check(m, ctx):
            p = dict(iif=A['br'], physdev_in=A['vif'], src=A['ip6'], oif=B['br'], dst=B['ip6'], proto='tcp', dport=80)
            # A may send tcp/80 to fd00::/64 but B has no IPv6 ingress rule
            self.assertEqual(m.forward(p, 'ip6tables'), 'DROP', ctx)
            p = dict(iif=A['br'], physdev_in=A['vif'], src=A['ip6'], oif='eth0', dst='2001:db8::1', proto='tcp', dport=80)
            self.assertEqual(m.forward(p, 'ip6tables'), 'DROP', ctx)  # not in fd00::/64
            p['dst'] = 'fd00::1:1'
            self.assertEqual(m.forward(p, 'ip6tables'), 'ACCEPT', ctx)
            # duplicate address detection from the unspecified address is not caught by the
            # anti-spoof rule, which does catch everything else
            p = dict(iif=A['br'], physdev_in=A['vif'], src='::', oif='eth0', dst='ff02::1:ff00:7',
                     proto='icmpv6', icmp_type='neighbor-solicitation', hl=255)
            self.assertEqual(m.forward(p, 'ip6tables'), 'ACCEPT', ctx)
            p = dict(iif=A['br'], physdev_in=A['vif'], src='fd00::99', oif='eth0', dst='fd00::1:1', proto='tcp', dport=80)
            self.assertEqual(m.forward(p, 'ip6tables'), 'DROP', ctx)
        self.for_both_orders(check)

    def test_non_l3_traffic_falls_through(self):
        def check(m, ctx):
            self.assertIsNone(m.forward(dict(iif='eth0', oif='cloudbr0', src='10.0.0.1', dst='10.0.0.2', proto='tcp', dport=80)), ctx)
            self.assertIsNone(m.forward(dict(iif='cloudbr0', oif='eth0', src='10.0.0.1', dst='10.0.0.2', proto='tcp', dport=80)), ctx)
            # a foreign mark bit is cleared first and cannot approve anything
            p = pkt(None, B, src="203.0.113.5", dst="10.1.1.200")
            p['mark'] = 0x40000000
            self.assertEqual(m.forward(p), 'DROP', ctx)
            p = dict(iif='eth0', oif='cloudbr0', src='10.0.0.1', dst='10.0.0.2', proto='tcp', dport=80, mark=0x40000000)
            self.assertIsNone(m.forward(p), ctx)
        self.for_both_orders(check)


class TestDirectRoutedTeardown(unittest.TestCase):

    def test_destroying_the_last_instance_removes_the_bridge_rules(self):
        sg, model = program_l3_host((A, B))
        sg.destroy_network_rules_for_vm(A['name'], A['vif'])
        for ipt in ['iptables', 'ip6tables']:
            t = model.tables[ipt]
            for chain in ['BF-brdr-43', 'BF-brdr-43-IN', 'BF-brdr-43-OUT', 'i-2-7-def', 'i-2-7-VM', 'i-2-7-VM-eg']:
                self.assertNotIn(chain, t)
            self.assertFalse(any('brdr-43' in r for r in t['BF-L3-IN'] + t['BF-L3-OUT']))
            self.assertFalse(any('i-2-7' in r for rules in t.values() for r in rules))
            # B is untouched, as are the shared chains and the hook
            self.assertIn("-i brdr-42 -j BF-brdr-42-IN", t['BF-L3-IN'])
            self.assertIn("-o brdr-42 -j DROP", t['BF-L3-OUT'])
            self.assertTrue(any('i-2-8-def' in r for r in t['BF-brdr-42-IN']))
            self.assertEqual(t['FORWARD'], ["-j BF-L3"])
            self.assertEqual(len(t['BF-L3']), 4)
        self.assertEqual(model.forward(pkt(None, B, src="10.1.1.50", dport=80)), 'ACCEPT')
        self.assertEqual(model.forward(pkt(None, B, src="203.0.113.5", dport=80)), 'DROP')

        sg.destroy_network_rules_for_vm(B['name'], B['vif'])
        for ipt in ['iptables', 'ip6tables']:
            t = model.tables[ipt]
            self.assertEqual(t['BF-L3-IN'], [])
            self.assertEqual(t['BF-L3-OUT'], [])
            self.assertEqual(t['FORWARD'], ["-j BF-L3"])
            self.assertEqual(sorted(t), sorted(['FORWARD', 'INPUT', 'OUTPUT', 'BF-L3', 'BF-L3-IN', 'BF-L3-OUT']))

    def test_bridge_rules_stay_while_an_instance_remains(self):
        sg, model = program_l3_host((A, B))
        c = dict(name="i-2-9-VM", vif="vnet9", ip="10.1.1.9", ip6="fd00::9", mac="02:00:4c:5f:00:09", br="brdr-43")
        sg.default_network_rules(c['name'], "1", c['ip'], c['ip6'], c['mac'], c['vif'], c['br'], "0:",
                                 is_first_nic=True, direct_routed=True)
        sg.destroy_network_rules_for_vm(A['name'], A['vif'])
        t = model.tables['iptables']
        self.assertIn("-i brdr-43 -j BF-brdr-43-IN", t['BF-L3-IN'])
        self.assertTrue(any('i-2-9-def' in r for r in t['BF-brdr-43-IN']))
        self.assertFalse(any('i-2-7-def' in r for r in t['BF-brdr-43-IN']))

    def test_dispatch_deletion_is_anchored_on_the_chain_name(self):
        sg = load_script()
        model = IptablesModel()
        sg.execute = model.execute
        for ipt in ['iptables', 'ip6tables']:
            t = model.tables[ipt]
            t['BF-brdr-42-IN'] = ["-m physdev --physdev-in vnet3 -j i-2-7-def",
                                  "-m physdev --physdev-in vnet4 -j i-2-70-def"]
            t['BF-brdr-42-OUT'] = ["-m set --match-set i-2-7-VM dst -j i-2-7-def",
                                   "-m set --match-set i-2-70-VM dst -j i-2-70-def"]
            t['i-2-7-def'] = ["-m physdev --physdev-in vnet3 -j DROP"]
        chains = sg.delete_rules_for_vm_in_bridge_firewall_chain("i-2-7-VM")
        self.assertEqual(chains, {'BF-brdr-42-IN', 'BF-brdr-42-OUT'})
        for ipt in ['iptables', 'ip6tables']:
            t = model.tables[ipt]
            self.assertEqual(t['BF-brdr-42-IN'], ["-m physdev --physdev-in vnet4 -j i-2-70-def"])
            self.assertEqual(t['BF-brdr-42-OUT'], ["-m set --match-set i-2-70-VM dst -j i-2-70-def"])
            self.assertEqual(t['i-2-7-def'], ["-m physdev --physdev-in vnet3 -j DROP"])


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

    def test_rebooted_vm_path_has_no_l3_detection(self):
        sg = load_script()
        source = inspect.getsource(sg.network_rules_for_rebooted_vm)
        self.assertNotIn('match-set', source)
        self.assertNotIn('direct', source)


class TestVerifyRules(unittest.TestCase):
    """ verify_network_rules describes the rules in iptables-save form; keep it in step with
    what default_network_rules programs. """

    def expected(self, direct_routed):
        sg = load_script()
        seen = []
        sg.execute = lambda cmd: ""
        sg.get_br_fw = lambda b: "BF-" + b
        sg.verify_expected_rules_exist = lambda exp, rules: seen.extend(exp) or True
        sg.verify_expected_rules_in_order = lambda exp, rules: seen.extend(exp) or True
        sg.verify_default_iptables_rules_for_vm("i-2-7-VM", "7", ["10.1.1.55"], "fd00::55", VM_ARGS['vm_mac'],
                                                "vnet3", "brdr-42", direct_routed)
        return seen

    def test_classic_expectations_match_the_current_stream(self):
        seen = self.expected(False)
        self.assertIn("-A BF-brdr-42-IN -m physdev --physdev-in vnet3 -j i-2-7-def", seen)
        self.assertIn("-A BF-brdr-42-OUT -m physdev --physdev-out vnet3 --physdev-is-bridged -j i-2-7-def", seen)
        for rule in seen:
            if '--physdev-in' in rule:
                self.assertNotIn('--physdev-is-bridged', rule, rule)
            self.assertNotIn('mark', rule.lower(), rule)

    def test_l3_expectations(self):
        seen = self.expected(True)
        self.assertIn("-A BF-brdr-42-OUT -m set --match-set i-2-7-VM dst -j i-2-7-def", seen)
        self.assertIn("-A i-2-7-def -m physdev --physdev-in vnet3 -m mark --mark " + MARK + " -j RETURN", seen)
        self.assertIn("-A i-2-7-def -m physdev --physdev-in vnet3 -m state --state RELATED,ESTABLISHED " + APPROVE, seen)
        for rule in seen:
            self.assertNotIn('--physdev-is-bridged', rule, rule)
            self.assertNotIn('dport 67', rule, rule)


class TestIpv6OnlyDirectRouted(unittest.TestCase):
    """ An Instance on an IPv6-only Direct Routed network has no IPv4 address at all; the
    default rules must program cleanly without one. """

    def setUp(self):
        self.sg = load_script()
        ok, self.captured = capture_default_network_rules(self.sg, "brdr-42", direct_routed=True, vm_ip=None)
        self.assertTrue(ok, "default rules must succeed for an IPv6-only Instance")

    def test_no_command_carries_a_none_address(self):
        offenders = [c for c in self.captured if 'None' in c]
        self.assertEqual([], offenders)

    def test_ipv4_ipset_stays_empty(self):
        adds = [c for c in self.captured if c.startswith('ipset -! -A ') and not c.split()[3].endswith('-6')]
        self.assertEqual([], adds)

    def test_ipv6_ipset_still_populated(self):
        adds = [c for c in self.captured if c.startswith('ipset -! -A ') and c.split()[3].endswith('-6')]
        self.assertTrue(any(VM_ARGS['vm_ip6'] in c for c in adds), "the IPv6 address must still reach its ipset")


def regenerate_golden():
    for path, fn in [(GOLDEN, lambda sg: capture_default_network_rules(sg, "cloudbr0", direct_routed=False)[1]),
                     (GOLDEN_L3, lambda sg: capture_default_network_rules(sg, "brdr-42", direct_routed=True)[1]),
                     (GOLDEN_FRAMEWORK, lambda sg: capture_framework(sg, sg.add_fw_framework, "cloudbr0")),
                     (GOLDEN_L3_FRAMEWORK, lambda sg: capture_framework(sg, sg.add_l3_fw_framework, "brdr-42"))]:
        captured = fn(load_script())
        with open(path, 'w') as f:
            f.write("\n".join(captured) + "\n")
        print("wrote %d commands to %s" % (len(captured), path))


if __name__ == '__main__':
    if '--regenerate' in sys.argv:
        regenerate_golden()
    else:
        unittest.main()
