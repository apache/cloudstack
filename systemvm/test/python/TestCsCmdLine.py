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
from cs.CsDatabag import CsCmdLine
import merge


class TestCsCmdLine(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."
        self.cscmdline = CsCmdLine('cmdline', {})

    def test_ini(self):
        self.assertTrue(self.cscmdline is not None)

    def test_idata(self):
        self.assertTrue(self.cscmdline.idata() == {})

    def test_is_redundant(self):
        self.assertTrue(self.cscmdline.is_redundant() is False)
        self.cscmdline.set_redundant()
        self.assertTrue(self.cscmdline.is_redundant() is True)

    def test_get_guest_gw(self):
        tval = "192.168.1.4"
        self.cscmdline.set_guest_gw(tval)
        self.assertTrue(self.cscmdline.get_guest_gw() == tval)

if __name__ == '__main__':
    unittest.main()
