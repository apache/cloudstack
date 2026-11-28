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

import os
import secrets
import shutil
import tempfile
import unittest
from unittest import mock

import configure
from configure import CsRemoteAccessVpn

# random per run, never a real key
PSK1 = secrets.token_hex(8)
PSK2 = secrets.token_hex(8)


class TestCsRemoteAccessVpn(unittest.TestCase):

    def setUp(self):
        self.dir = tempfile.mkdtemp()
        self.vpn = CsRemoteAccessVpn.__new__(CsRemoteAccessVpn)
        self.vpn.VPNCONFDIR = self.dir
        self.obj = {"local_ip": "10.1.2.1", "local_cidr": "198.51.100.0/24", "public_interface": "eth1",
                    "ip_range": "10.1.2.2-10.1.2.8", "preshared_key": PSK1, "vpn_server_ip": "203.0.113.10"}
        real = configure.CsFile
        # keep every file configure_l2tpIpsec() writes inside the test directory
        patcher = mock.patch("configure.CsFile", side_effect=lambda name: real(os.path.join(self.dir, os.path.basename(name))))
        patcher.start()
        self.addCleanup(patcher.stop)
        self.addCleanup(shutil.rmtree, self.dir)

    def configure(self):
        return self.vpn.configure_l2tpIpsec("203.0.113.10", self.obj)

    def secret(self):
        with open(os.path.join(self.dir, "ipsec.any.secrets")) as f:
            return f.read()

    def test_unchanged_config_is_not_reported_as_changed(self):
        # an unchanged pass must not restart xl2tpd, which drops every connected client
        self.assertTrue(self.configure())
        self.assertFalse(self.configure())
        self.assertEqual(self.secret(), ': PSK "%s"\n' % PSK1)

    def test_changed_psk_is_reported_as_changed(self):
        self.configure()
        self.obj["preshared_key"] = PSK2
        self.assertTrue(self.configure())
        self.assertEqual(self.secret(), ': PSK "%s"\n' % PSK2)
        self.assertFalse(self.configure())


if __name__ == '__main__':
    unittest.main()
