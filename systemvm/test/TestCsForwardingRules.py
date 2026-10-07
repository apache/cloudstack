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
from configure import CsForwardingRules


class TestCsForwardingRules(unittest.TestCase):

    def ftp_helper_rules(self, protocol, public_ports, internal_ports):
        forwarding = CsForwardingRules.__new__(CsForwardingRules)
        forwarding.fw = []
        forwarding.forward_ftp_helper({"public_ip": "10.0.0.2", "internal_ip": "10.1.1.10", "protocol": protocol,
                                       "public_ports": public_ports, "internal_ports": internal_ports})
        return forwarding.fw

    def test_ftp_helper_on_other_public_port(self):
        self.assertEqual(self.ftp_helper_rules("tcp", "2121:2121", "21:21"),
                         [["raw", "", "-A PREROUTING -d 10.0.0.2/32 -p tcp -m tcp --dport 2121 -j CT --helper ftp"]])

    def test_ftp_helper_on_port_range(self):
        self.assertEqual(self.ftp_helper_rules("tcp", "1020:1030", "20:30"),
                         [["raw", "", "-A PREROUTING -d 10.0.0.2/32 -p tcp -m tcp --dport 1021 -j CT --helper ftp"]])

    def test_no_ftp_helper(self):
        self.assertEqual(self.ftp_helper_rules("tcp", "21:21", "21:21"), [])
        self.assertEqual(self.ftp_helper_rules("tcp", "2121:2121", "2121:2121"), [])
        self.assertEqual(self.ftp_helper_rules("udp", "2121:2121", "21:21"), [])
        self.assertEqual(self.ftp_helper_rules("tcp", "any", "any"), [])


if __name__ == '__main__':
    unittest.main()
