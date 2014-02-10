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
from marvin.integration.lib.utils import verifyElementInList
from marvin.codes import PASS


def user(Name, DomainName, AcctType):
    def wrapper(cls):
        orig_init = cls.__init__

        def __init__(self, *args, **kws):
            cls.UserName = Name
            cls.DomainName = DomainName
            cls.AcctType = AcctType
            orig_init(self, *args, **kws)
        cls.__init__ = __init__
        return cls
    return wrapper


class cloudstackTestCase(unittest.case.TestCase):
    clstestclient = None

    def assertElementInList(inp, toverify, responsevar=None,  pos=0,
                            assertmsg="TC Failed for reason"):
        '''
        @Name: assertElementInList
        @desc:Uses the utility function verifyElementInList and
        asserts based upon PASS\FAIL value of the output.
        Takes one additional argument of what message to assert with
        when failed
        '''
        out = verifyElementInList(inp, toverify, responsevar,  pos)
        unittest.TestCase.assertEquals(out[0], PASS, "msg:%s" % out[1])

    @classmethod
    def getClsTestClient(cls):
        return cls.clstestclient
