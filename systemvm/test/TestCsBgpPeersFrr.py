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

"""Tests for how CsBgpPeers renders bgp minimum-holdtime on a router with BGP peers.

CsBgpPeers renders bgp minimum-holdtime from the min_holdtime key of the
bgppeers databag, which cs_bgppeers.merge keeps beside the peer entries.
"""

import os
import unittest
try:
    import mock
except ImportError:
    from unittest import mock

from vr_test_helpers import VrSandbox, FakeShell

from cs.CsBgpPeers import CsBgpPeers
from cs import CsBgpPeers as bgppeers_module
import cs_bgppeers

OPERATOR_PEER = {'peer_id': 1, 'network_id': 100, 'network_as_number': 64600, 'peer_as_number': 65100,
                 'ip4_address': '198.51.100.1', 'guest_ip4_cidr': '172.16.10.0/24', 'details': {}}


class OperatorFrrTestCase(unittest.TestCase):
    """Runs CsBgpPeers.process() in a sandbox and records the shell commands."""

    def run_operator(self, min_holdtime=None, peer=None, bgpd_running=True):
        # None leaves the min_holdtime key out of the databag.
        bag = {"id": "bgppeers", "1-100": dict(peer or OPERATOR_PEER)}
        if min_holdtime is not None:
            bag["min_holdtime"] = min_holdtime
        sandbox = VrSandbox(mode="routed", extra={"bgppeers.json": bag})
        self.addCleanup(sandbox.close)
        self.shell = FakeShell()
        self.conf = sandbox.path("frr.conf")
        daemons = sandbox.path("daemons")
        with open(daemons, "w") as f:
            f.write("bgpd=%s\nbfdd=no\n" % ("yes" if bgpd_running else "no"))
        patches = [mock.patch('cs.CsHelper.execute', side_effect=self.shell.execute),
                   mock.patch('cs.CsHelper.mkdir'),
                   mock.patch('cs.CsHelper.get_hostname', return_value="r-1001-VM"),
                   mock.patch.object(bgppeers_module, "FRR_DIR", sandbox.dir + os.sep),
                   mock.patch.object(bgppeers_module, "FRR_CONFIG", self.conf),
                   mock.patch.object(bgppeers_module, "FRR_DAEMONS", daemons)]
        for p in patches:
            p.start()
            self.addCleanup(p.stop)
        try:
            CsBgpPeers("bgppeers", sandbox.config).process()
        except (TypeError, KeyError, AttributeError, ValueError) as e:
            self.fail("CsBgpPeers failed on databag %r: %r" % (bag, e))
        with open(self.conf) as f:
            return [line.rstrip('\n') for line in f]

    def router_block(self, lines):
        start = [i for i, x in enumerate(lines) if x.startswith("router bgp ")]
        self.assertEqual(len(start), 1, lines)
        end = next((i for i in range(start[0] + 1, len(lines)) if not lines[i].startswith(" ")), len(lines))
        return lines[start[0]:end]


class TestCsBgpPeersMinimumHoldtime(OperatorFrrTestCase):
    """A router with BGP peers renders bgp minimum-holdtime from the databag."""

    def test_databag_merge_keeps_min_holdtime(self):
        dbag = cs_bgppeers.merge({"id": "bgppeers"}, {"peers": [dict(OPERATOR_PEER)], "min_holdtime": 3})
        self.assertEqual(dbag.get("min_holdtime"), 3)
        self.assertEqual(dbag.get("1-100", {}).get("ip4_address"), "198.51.100.1")

    def test_operator_only_vr_renders_minimum_holdtime(self):
        block = self.router_block(self.run_operator(3))
        self.assertEqual([x for x in block if "minimum-holdtime" in x], [" bgp minimum-holdtime 3"])
        first_neighbour = min(i for i, x in enumerate(block) if x.startswith(" neighbor "))
        self.assertLess(block.index(" bgp minimum-holdtime 3"), first_neighbour)

    def test_operator_only_vr_renders_raised_minimum_holdtime(self):
        block = self.router_block(self.run_operator(9))
        self.assertEqual([x for x in block if "minimum-holdtime" in x], [" bgp minimum-holdtime 9"])

    def test_operator_only_vr_renders_no_minimum_holdtime_for_zero(self):
        # A minimum of 0 lets peers turn keepalives off with hold time 0.
        block = self.router_block(self.run_operator(0))
        self.assertEqual([x for x in block if "minimum-holdtime" in x], [])

    def test_operator_only_vr_skips_minimum_holdtime_outside_frr_range(self):
        # FRR rejects these values, so the router renders no line for them.
        for value in (-1, 65536):
            block = self.router_block(self.run_operator(value))
            self.assertEqual([x for x in block if "minimum-holdtime" in x], [], value)


if __name__ == '__main__':
    unittest.main()
