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
import mock
import configure
from configure import CsSite2SiteVpn


class TestCsSite2SiteVpn(unittest.TestCase):

    def setUp(self):
        self.vpn = CsSite2SiteVpn.__new__(CsSite2SiteVpn)

    @mock.patch('configure.CsHelper.execute')
    def test_start_connections_brings_up_a_connection_that_is_not_established(self, mock_execute):
        mock_execute.return_value = []
        self.vpn.start_connections('10.0.0.2', 1)
        mock_execute.assert_any_call('timeout 20 ipsec up vpn-10.0.0.2')

    @mock.patch('configure.CsHelper.execute')
    def test_start_connections_skips_a_connection_that_is_established(self, mock_execute):
        mock_execute.return_value = ['vpn-10.0.0.2[1]: ESTABLISHED 5 seconds ago']
        self.vpn.start_connections('10.0.0.2', 1)
        for call in mock_execute.call_args_list:
            self.assertNotIn('ipsec up', call[0][0])

    @mock.patch('configure.CsHelper.execute')
    def test_start_connections_covers_every_split_connection(self, mock_execute):
        mock_execute.return_value = []
        self.vpn.start_connections('10.0.0.2', 3)
        ups = [call[0][0] for call in mock_execute.call_args_list if 'ipsec up' in call[0][0]]
        self.assertEqual(ups, ['timeout 20 ipsec up vpn-10.0.0.2',
                               'timeout 20 ipsec up vpn-10.0.0.2-2',
                               'timeout 20 ipsec up vpn-10.0.0.2-3'])


if __name__ == '__main__':
    unittest.main()
