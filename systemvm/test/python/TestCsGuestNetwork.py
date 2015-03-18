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
from cs.CsGuestNetwork import CsGuestNetwork
import merge


class TestCsGuestNetwork(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."

    def test_init(self):
        csguestnetwork = CsGuestNetwork({}, {})
        self.assertTrue(csguestnetwork is not None)

    def test_get_dns(self):
        csguestnetwork = CsGuestNetwork({}, {})
        csguestnetwork.guest = True
        csguestnetwork.set_dns("1.1.1.1,2.2.2.2")
        csguestnetwork.set_router("3.3.3.3")
        dns = csguestnetwork.get_dns()
        self.assertTrue(len(dns) == 3)
        csguestnetwork.set_dns("1.1.1.1")
        dns = csguestnetwork.get_dns()
        self.assertTrue(len(dns) == 2)

if __name__ == '__main__':
    unittest.main()
