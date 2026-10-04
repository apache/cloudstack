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
        self.managed_lease = os.path.join(self.tmpdir, "dnsmasq_managed_lease")
        with open(self.managed_lease, "w") as f:
            f.write("1\n")
        for name, path in (("LEASES", self.leases), ("CLOUD_CONF", self.cloud_conf),
                           ("DNSMASQ_MANAGED_LEASE", self.managed_lease)):
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
        service.assert_not_called()

    def router(self, redundant=False, primary=True):
        csdhcp = CsDhcp("dhcpentry", {})
        csdhcp.cl = mock.Mock()
        csdhcp.cl.is_redundant.return_value = redundant
        csdhcp.cl.is_primary.return_value = primary
        return csdhcp

    @mock.patch("cs.CsDhcp.time.sleep")
    @mock.patch("cs.CsDhcp.CsHelper.service")
    def test_leases_left_by_dnsmasq_are_removed_with_one_wait_and_one_restart(self, service, sleep):
        self.router().ensure_leases_removed(["10.1.1.71", "10.1.1.72"])
        self.assertEqual(20, sleep.call_count)
        service.assert_called_once_with("dnsmasq", "try-restart")
        with open(self.leases) as f:
            self.assertEqual("", f.read())

    @mock.patch("cs.CsDhcp.time.sleep")
    @mock.patch("cs.CsDhcp.CsHelper.service")
    def test_released_leases_need_no_wait_or_restart(self, service, sleep):
        with open(self.leases, "w") as f:
            f.write("")
        self.router().ensure_leases_removed(["10.1.1.71", "10.1.1.72"])
        sleep.assert_not_called()
        service.assert_not_called()

    @mock.patch("cs.CsDhcp.time.sleep")
    @mock.patch("cs.CsDhcp.CsHelper.service")
    def test_read_only_leases_file_is_cleaned_without_wait_or_restart(self, service, sleep):
        with open(self.managed_lease, "w") as f:
            f.write("0\n")
        self.router().ensure_leases_removed(["10.1.1.71"])
        sleep.assert_not_called()
        service.assert_not_called()
        with open(self.leases) as f:
            self.assertEqual("0 02:00:00:00:00:02 10.1.1.72 vm-b *\n", f.read())

    @mock.patch("cs.CsDhcp.time.sleep")
    @mock.patch("cs.CsDhcp.CsHelper.service")
    @mock.patch("cs.CsDhcp.CsHelper.send_dhcp_release")
    def test_backup_router_only_cleans_the_file(self, send, service, sleep):
        csdhcp = self.router(redundant=True, primary=False)
        csdhcp.del_host = mock.Mock()
        with mock.patch("cs.CsDhcp.DHCP_HOSTS", os.path.join(self.tmpdir, "dhcphosts.txt")):
            open(os.path.join(self.tmpdir, "dhcphosts.txt"), "w").close()
            csdhcp.delete_leases()
        send.assert_not_called()
        sleep.assert_not_called()
        service.assert_not_called()
        with open(self.leases) as f:
            self.assertEqual("", f.read())

    @mock.patch("cs.CsDhcp.time.sleep")
    @mock.patch("cs.CsDhcp.CsHelper.service")
    def test_all_stale_leases_are_handled_while_the_file_is_rewritten(self, service, sleep):
        # more than one read buffer of leases, each removed in place while the loop runs
        with open(self.leases, "w") as f:
            for i in range(1000):
                f.write("0 02:00:00:00:%02x:%02x 10.1.%d.%d vm-%d *\n" % (i // 256, i % 256, 2 + i // 250, 1 + i % 250, i))
        csdhcp = self.router()
        csdhcp.del_host = mock.Mock()
        csdhcp.release_lease = mock.Mock()
        with mock.patch("cs.CsDhcp.DHCP_HOSTS", os.path.join(self.tmpdir, "dhcphosts.txt")):
            open(os.path.join(self.tmpdir, "dhcphosts.txt"), "w").close()
            csdhcp.delete_leases()
        self.assertEqual(1000, csdhcp.release_lease.call_count)
        # none of them was released (release_lease is a mock): still one wait and one restart
        self.assertEqual(20, sleep.call_count)
        service.assert_called_once_with("dnsmasq", "try-restart")
        with open(self.leases) as f:
            self.assertEqual("", f.read())


if __name__ == '__main__':
    unittest.main()
