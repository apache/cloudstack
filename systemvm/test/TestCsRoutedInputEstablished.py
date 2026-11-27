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

"""ROUTED VR input hooks accept established and related traffic.

Without that accept the ip4_acl and ip4_firewall INPUT chains accept ICMP
echo only. Replies to the VR's own DNS and TCP traffic over a private
gateway are dropped by the chain's drop policy. The forward hook already
accepts established and related traffic (#12986).
"""

import re
import unittest
try:
    import mock
except ImportError:
    from unittest import mock

from cs.CsNetfilter import CsNetfilters
import merge


def established(cmd):
    return re.search(r'ct state (established,related|related,established) (counter )?accept', cmd) is not None


class TestCsRoutedInputEstablished(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."
        self.commands = []
        patcher = mock.patch('cs.CsNetfilter.CsHelper.execute', side_effect=self.record)
        patcher.start()
        self.addCleanup(patcher.stop)

    def record(self, cmd):
        self.commands.append(cmd)
        return []

    def chain_rules(self, table, chain):
        prefix = "nft add rule ip %s %s " % (table, chain)
        return [c for c in self.commands if c.startswith(prefix)]

    def test_acl_input_hook_accepts_established(self):
        CsNetfilters(load=False).add_ip4_chain('ip', 'ip4_acl', 'INPUT', 'input', 'drop')
        rules = self.chain_rules('ip4_acl', 'INPUT')
        self.assertTrue([c for c in rules if established(c)],
                        "ip4_acl INPUT needs 'ct state established,related accept': %s" % rules)

    def test_firewall_input_hook_accepts_established(self):
        CsNetfilters(load=False).add_ip4_chain('ip', 'ip4_firewall', 'INPUT', 'input', 'drop')
        rules = self.chain_rules('ip4_firewall', 'INPUT')
        self.assertTrue([c for c in rules if established(c)], rules)

    def test_input_keeps_icmp_echo_accept(self):
        CsNetfilters(load=False).add_ip4_chain('ip', 'ip4_acl', 'INPUT', 'input', 'drop')
        rules = self.chain_rules('ip4_acl', 'INPUT')
        self.assertTrue([c for c in rules if "icmp type" in c and "echo-reply" in c and c.endswith("accept")])

    def test_each_hook_gets_established_accept_once(self):
        # Input and forward get exactly one accept. Output gets none.
        for chain, hook, expected in [('INPUT', 'input', 1), ('FORWARD', 'forward', 1), ('OUTPUT', 'output', 0)]:
            CsNetfilters(load=False).add_ip4_chain('ip', 'ip4_acl', chain, hook, 'drop')
            rules = self.chain_rules('ip4_acl', chain)
            self.assertEqual(expected, len([c for c in rules if established(c)]), "%s: %s" % (chain, rules))

    def test_forward_hook_keeps_established(self):
        # #12986 behaviour, which already holds today.
        CsNetfilters(load=False).add_ip4_chain('ip', 'ip4_acl', 'FORWARD', 'forward', 'accept')
        self.assertTrue([c for c in self.chain_rules('ip4_acl', 'FORWARD') if established(c)])

    def test_rebuilt_acl_table_accepts_established_before_rules(self):
        # The accept sits on the chain before any rule from the ACL list is added.
        rules = [{'type': "", 'chain': 'INPUT', 'rule': 'iifname "eth2" udp dport 67 counter accept'}]
        with mock.patch('cs.CsNetfilter.CsHelper.execute', side_effect=self.record):
            CsNetfilters(load=False).apply_nft_ipv4_rules(rules, "acl")
        input_rules = self.chain_rules('ip4_acl', 'INPUT')
        found = [i for i, c in enumerate(input_rules) if established(c)]
        self.assertTrue(found, input_rules)
        listed = [i for i, c in enumerate(input_rules) if 'dport 67' in c]
        self.assertLess(found[0], listed[0])


if __name__ == '__main__':
    unittest.main()
