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
from cs.CsAddress import CsInterface
from cs.CsConfig import CsConfig
from cs.CsDatabag import CsCmdLine
import merge


class TestCsInterface(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."
        csconfig = CsConfig()
        self.cmdline = CsCmdLine("cmdline", csconfig)
        csconfig.cl = self.cmdline
        self.csinterface = CsInterface({}, csconfig)

    def test_is_public(self):
        self.assertTrue(self.csinterface.is_public() is False)

if __name__ == '__main__':
    unittest.main()
