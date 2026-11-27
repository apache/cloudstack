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

import tempfile
import unittest
import mock
from cs import CsHelper
import merge


class TestCsHelper(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."

    @mock.patch('cs.CsHelper.subprocess')
    def test_execute(self, mock_subprocess):
        result = CsHelper.execute("/bin/false")
        self.assertTrue(result is not None)

    @mock.patch('cs.CsHelper.execute')
    def test_save_iptables_writes_the_command_output(self, mock_execute):
        mock_execute.return_value = ["*filter", "COMMIT"]

        with tempfile.NamedTemporaryFile(mode="r") as saved:
            CsHelper.save_iptables("iptables-save", saved.name)
            self.assertEqual("*filter\nCOMMIT\n", saved.read())
        mock_execute.assert_called_once_with("iptables-save")

    @mock.patch('cs.CsHelper.execute')
    def test_save_nftables_leaves_out_iptables_tables(self, mock_execute):
        tables = ["table ip filter", "table ip nat", "table ip mangle", "table ip6 filter", "table ip6 raw",
                  "table bridge broute", "table ip6 ip6_firewall", "table ip ip4_firewall", "table inet cloudstack"]
        mock_execute.side_effect = lambda command: tables if command == "nft list tables" else \
            ["%s {" % command.replace("nft list ", ""), "}"]

        with tempfile.NamedTemporaryFile(mode="r") as saved:
            CsHelper.save_nftables(saved.name)
            content = saved.read()

        self.assertEqual("table ip6 ip6_firewall {\n}\ntable ip ip4_firewall {\n}\ntable inet cloudstack {\n}\n", content)

    @mock.patch('cs.CsHelper.execute')
    def test_save_nftables_writes_an_empty_file_without_nftables_tables(self, mock_execute):
        mock_execute.side_effect = lambda command: ["table ip filter", "table ip nat"] if command == "nft list tables" else ["x"]

        with tempfile.NamedTemporaryFile(mode="r") as saved:
            CsHelper.save_nftables(saved.name)
            self.assertEqual("", saved.read())


if __name__ == '__main__':
    unittest.main()
