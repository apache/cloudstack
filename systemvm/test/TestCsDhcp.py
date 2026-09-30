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
import shutil
import tempfile
import unittest
import mock
from netaddr import IPNetwork
from cs import CsDhcp as CsDhcpModule
from cs.CsDhcp import CsDhcp
from cs import CsHelper
import merge


class TestCsDhcp(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."
        self.tmpdir = tempfile.mkdtemp()
        self.addCleanup(shutil.rmtree, self.tmpdir)
        self.leases = os.path.join(self.tmpdir, "dnsmasq.leases")
        self.cloud_conf = os.path.join(self.tmpdir, "cloud.conf")
        with open(self.leases, "w") as f:
            f.write("0 02:00:00:00:00:01 10.1.1.71 vm-a *\n0 02:00:00:00:00:02 10.1.1.72 vm-b *\n")
        with open(self.cloud_conf, "w") as f:
            f.write("listen-address=127.0.0.1,10.1.1.1\n")
        for name, path in (("LEASES", self.leases), ("CLOUD_CONF", self.cloud_conf)):
            patcher = mock.patch.object(CsDhcpModule, name, path)
            patcher.start()
            self.addCleanup(patcher.stop)

    # @mock.patch('cs.CsDhcp.CsHelper')
    # @mock.patch('cs.CsDhcp.CsDnsMasq')
    def test_init(self):
        csdhcp = CsDhcp("dhcpentry", {})
        self.assertTrue(csdhcp is not None)

    @mock.patch("cs.CsHelper.socket.socket")
    def test_send_dhcp_release(self, sock):
        CsHelper.send_dhcp_release("eth0", "10.1.1.1", "10.1.1.71", "02:00:00:00:00:01")
        packet, address = sock.return_value.sendto.call_args[0]
        self.assertEqual(("10.1.1.1", 67), address)
        self.assertEqual(548, len(packet))
        self.assertEqual(bytes([10, 1, 1, 71]), packet[12:16])
        self.assertEqual(bytes([2, 0, 0, 0, 0, 1]), packet[28:34])
        self.assertEqual(bytes([99, 130, 83, 99, 53, 1, 7, 54, 4, 10, 1, 1, 1, 255]), packet[236:250])

    @mock.patch("cs.CsDhcp.CsHelper.send_dhcp_release")
    def test_release_lease_uses_listen_address_as_server_id(self, send):
        # redundant router: router address 10.1.1.192, dnsmasq listens on the gateway 10.1.1.1
        csdhcp = CsDhcp("dhcpentry", {})
        csdhcp.devinfo = [{'dev': 'eth2', 'network': IPNetwork("192.168.1.10/24")},
                          {'dev': 'eth0', 'network': IPNetwork("10.1.1.192/24")}]
        csdhcp.release_lease("10.1.1.71", "02:00:00:00:00:01")
        send.assert_called_once_with("eth0", "10.1.1.1", "10.1.1.71", "02:00:00:00:00:01")

    @mock.patch("cs.CsDhcp.CsHelper.service")
    def test_remove_lease_in_place(self, service):
        csdhcp = CsDhcp("dhcpentry", {})
        inode = os.stat(self.leases).st_ino
        self.assertTrue(csdhcp.remove_lease("10.1.1.71"))
        self.assertEqual(inode, os.stat(self.leases).st_ino)
        with open(self.leases) as f:
            self.assertEqual("0 02:00:00:00:00:02 10.1.1.72 vm-b *\n", f.read())
        service.assert_called_once_with("dnsmasq", "try-restart")


if __name__ == '__main__':
    unittest.main()
