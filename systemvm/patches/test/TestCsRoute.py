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
from cs.CsRoute import CsRoute
import merge


class TestCsRoute(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."

    def test_init(self):
        csroute = CsRoute()
        self.assertIsInstance(csroute, CsRoute)

    def test_defaultroute_exists(self):
        csroute = CsRoute()
        self.assertFalse(csroute.defaultroute_exists())

    def test_add_defaultroute(self):
        csroute = CsRoute()
        self.assertTrue(csroute.add_defaultroute("192.168.1.1"))

    def test_get_tablename(self):
        csroute = CsRoute()
        name = "eth1"
        self.assertEqual("Table_eth1", csroute.get_tablename(name))

if __name__ == '__main__':
    unittest.main()
