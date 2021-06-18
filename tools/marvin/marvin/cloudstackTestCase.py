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
from marvin.lib.utils import verifyElementInList, cleanup_resources
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

    def assertElementInList(self, inp, toverify, responsevar=None, pos=0,
                            assertmsg="TC Failed for reason"):
        '''
        @Name: assertElementInList
        @desc:Uses the utility function verifyElementInList and
        asserts based upon PASS\FAIL value of the output.
        Takes one additional argument of what message to assert with
        when failed
        '''
        out = verifyElementInList(inp, toverify, responsevar, pos)
        unittest.TestCase.assertEqual(out[0], PASS, "msg:%s" % out[1])

    @classmethod
    def getClsTestClient(cls):
        return cls.clstestclient

    @classmethod
    def getClsConfig(cls):
        return cls.config

    @classmethod
    def tearDownClass(cls):
        try:
            if hasattr(cls,'_cleanup'):
                if hasattr(cls,'apiclient'):
                    cleanup_resources(cls.apiclient, reversed(cls._cleanup))
                elif hasattr(cls,'api_client'):
                    cleanup_resources(cls.api_client, reversed(cls._cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def tearDown(self):
        try:
            if hasattr(self,'apiclient') and hasattr(self,'cleanup'):
                cleanup_resources(self.apiclient, reversed(self.cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def assertEqual(self, first, second, msg=None):
        """Fail if the two objects are unequal as determined by the '=='
           operator.
        """
        if isinstance(msg, str):
            msg = msg.encode()
        super(cloudstackTestCase,self).assertEqual(first,second,msg)
