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

"""Tests for how CsBgpPeers writes and applies frr.conf on a router with BGP peers.

Once bgpd runs, changes are applied with frr-reload.py and frr is not
restarted, so sessions to unchanged peers stay up. Because frr-reload.py
deletes any running line the file leaves out, the file states the BGP defaults
it relies on and the kernel's forwarding setting for both address families.
"""

import os
import re
import unittest
try:
    import mock
except ImportError:
    from unittest import mock

from vr_test_helpers import VrSandbox, FakeShell

from cs.CsBgpPeers import CsBgpPeers
from cs import CsBgpPeers as bgppeers_module

OPERATOR_PEER = {'peer_id': 1, 'network_id': 100, 'network_as_number': 64600, 'peer_as_number': 65100,
                 'ip4_address': '198.51.100.1', 'guest_ip4_cidr': '172.16.10.0/24', 'details': {}}


class OperatorFrrTestCase(unittest.TestCase):
    """Runs CsBgpPeers.process() in a sandbox and records the shell commands."""

    def run_operator(self, peer=None, bgpd_running=True, frr_reload=True, reload_returncode=0,
                     ip_forwarding=True, ipv6_forwarding=False):
        bag = {"id": "bgppeers", "1-100": dict(peer or OPERATOR_PEER)}
        sandbox = VrSandbox(mode="routed", extra={"bgppeers.json": bag})
        self.addCleanup(sandbox.close)
        self.shell = FakeShell(returncode=reload_returncode)
        self.conf = sandbox.path("frr.conf")
        daemons = sandbox.path("daemons")
        with open(daemons, "w") as f:
            f.write("bgpd=%s\nbfdd=no\n" % ("yes" if bgpd_running else "no"))
        reload_script = sandbox.path("frr-reload.py")
        if frr_reload:
            open(reload_script, "w").close()
        patches = [mock.patch('cs.CsHelper.execute', side_effect=self.shell.execute),
                   mock.patch('cs.CsHelper.execute2', side_effect=self.shell.execute2),
                   mock.patch('cs.CsHelper.mkdir'),
                   mock.patch('cs.CsHelper.get_hostname', return_value="r-1001-VM"),
                   mock.patch.object(bgppeers_module, "FRR_DIR", sandbox.dir + os.sep),
                   mock.patch.object(bgppeers_module, "FRR_CONFIG", self.conf),
                   mock.patch.object(bgppeers_module, "FRR_DAEMONS", daemons),
                   mock.patch.object(bgppeers_module, "FRR_RELOAD", reload_script),
                   mock.patch.object(CsBgpPeers, "_ip_forwarding", return_value=ip_forwarding),
                   mock.patch.object(CsBgpPeers, "_ipv6_forwarding", return_value=ipv6_forwarding)]
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


class TestCsBgpPeersFrrReload(OperatorFrrTestCase):
    """Changes go through frr-reload.py, and the file states every global setting."""

    def test_operator_applies_with_frr_reload(self):
        # bgpd already runs, so a change reloads and other sessions stay up.
        self.run_operator()
        self.assertTrue([c for c in self.shell.commands if "frr-reload.py" in c], self.shell.commands)
        self.assertFalse([c for c in self.shell.commands if re.search(r'systemctl restart frr|service frr restart', c)],
                         self.shell.commands)

    def test_operator_restarts_frr_when_turning_bgpd_on(self):
        # bgpd starts only with frr, so the first configuration restarts it.
        self.run_operator(bgpd_running=False)
        self.assertTrue([c for c in self.shell.commands if c == "systemctl restart frr"], self.shell.commands)
        self.assertFalse([c for c in self.shell.commands if "frr-reload.py" in c], self.shell.commands)

    def test_operator_restarts_frr_without_frr_reload(self):
        # A template without frr-reload.py restarts frr as before.
        self.run_operator(frr_reload=False)
        self.assertTrue([c for c in self.shell.commands if c == "systemctl restart frr"], self.shell.commands)
        self.assertFalse([c for c in self.shell.commands if "frr-reload.py" in c], self.shell.commands)

    def test_operator_frr_reload_logs_to_stdout(self):
        # Without --stdout, frr-reload.py writes its own log file with peer passwords in it.
        self.run_operator()
        reloads = [c for c in self.shell.commands if "frr-reload.py" in c]
        self.assertEqual(len(reloads), 1, self.shell.commands)
        args = reloads[0].split()
        self.assertEqual(args[0], bgppeers_module.FRR_RELOAD)
        self.assertIn("--reload", args)
        self.assertIn("--stdout", args)
        self.assertEqual(args[-1], self.conf)

    def test_operator_reads_frr_reload_output_while_waiting(self):
        # A wait with unread output pipes blocks once frr-reload.py fills a pipe.
        self.run_operator()
        self.assertFalse([c for c in self.shell.waited if "frr-reload.py" in c], self.shell.waited)
        self.assertTrue([c for c in self.shell.communicated if "frr-reload.py" in c], self.shell.communicated)

    def test_operator_restarts_frr_when_frr_reload_fails(self):
        # A failed reload restarts frr, so the new file is still applied.
        self.run_operator(reload_returncode=1)
        reload_at = [i for i, c in enumerate(self.shell.commands) if "frr-reload.py" in c]
        restart_at = [i for i, c in enumerate(self.shell.commands) if c == "systemctl restart frr"]
        self.assertTrue(reload_at, self.shell.commands)
        self.assertTrue(restart_at, self.shell.commands)
        self.assertLess(reload_at[0], restart_at[0])

    def test_operator_defaults_lines(self):
        # No version line, so FRR uses the installed version's defaults. The two the
        # router relies on are stated, so a reload never depends on those defaults.
        lines = self.run_operator()
        self.assertEqual(lines[0], "frr defaults traditional")
        self.assertFalse([x for x in lines if x.startswith("frr version")], lines)
        block = self.router_block(lines)
        self.assertIn(" bgp ebgp-requires-policy", block)
        self.assertIn(" bgp network import-check", block)
        self.assertEqual(lines[-1], "line vty")

    def test_operator_states_forwarding_off_from_kernel(self):
        # zebra shows "no ipv6 forwarding" while the kernel has it off, and frr-reload.py
        # deletes running lines the file leaves out. Stating it keeps a reload from
        # switching forwarding on.
        lines = self.run_operator(ip_forwarding=False, ipv6_forwarding=False)
        self.assertIn("no ip forwarding", lines)
        self.assertIn("no ipv6 forwarding", lines)
        self.assertNotIn("ip forwarding", lines)
        self.assertNotIn("ipv6 forwarding", lines)

    def test_operator_states_forwarding_on_from_kernel(self):
        lines = self.run_operator(ip_forwarding=True, ipv6_forwarding=True)
        self.assertIn("ip forwarding", lines)
        self.assertIn("ipv6 forwarding", lines)
        self.assertNotIn("no ip forwarding", lines)
        self.assertNotIn("no ipv6 forwarding", lines)

    def test_operator_render_is_deterministic(self):
        self.assertEqual(self.run_operator(), self.run_operator())


if __name__ == '__main__':
    unittest.main()
