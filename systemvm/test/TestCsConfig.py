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
from unittest import mock
from cs.CsConfig import CsConfig
import merge


class TestCsConfig(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."

    def test_ini(self):
        csconfig = CsConfig()
        self.assertTrue(csconfig is not None)

    def usage_exempt_cidrs(self, cmdline):
        cl = mock.Mock()
        cl.idata.return_value = cmdline
        with mock.patch.object(CsConfig, "cmdline", return_value=cl):
            return CsConfig().get_usage_exempt_cidrs()

    def test_usage_exempt_cidrs_not_set(self):
        self.assertEqual(self.usage_exempt_cidrs({}), [])

    def test_usage_exempt_cidrs_are_written_as_iptables_prints_them(self):
        cidrs = self.usage_exempt_cidrs({"usage_exempt_cidrs": "192.0.2.5/32, 198.51.100.70/24,192.0.2.5"})
        self.assertEqual(cidrs, ["192.0.2.5/32", "198.51.100.0/24"])

    def test_usage_exempt_cidrs_skip_invalid_and_match_all(self):
        cidrs = self.usage_exempt_cidrs({"usage_exempt_cidrs": "not-an-ip,0.0.0.0/0,192.168.1.300,172.16.0.0/12"})
        self.assertEqual(cidrs, ["172.16.0.0/12"])

    def test_usage_exempt_cidrs_skip_ipv6(self):
        # the router only counts IPv4 traffic, so IPv6 entries have nothing to exempt
        cidrs = self.usage_exempt_cidrs({"usage_exempt_cidrs": "2001:db8::5/128,192.0.2.5/32,2001:db8:0:1::/64"})
        self.assertEqual(cidrs, ["192.0.2.5/32"])


if __name__ == '__main__':
    unittest.main()
