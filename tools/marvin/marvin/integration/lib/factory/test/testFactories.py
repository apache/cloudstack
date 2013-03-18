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
from marvin.integration.lib.factory import AccountFactory
from marvin.integration.lib.base import Account
from marvin.cloudstackTestClient import cloudstackTestClient

class AccountFactoryTest(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost')

    def test_userAccountFactory(self):
        af = AccountFactory.AccountFactory()
        accnt = Account.Account.create(apiclient=self.apiClient, AccountFactory=af)
        self.assertTrue(accnt is not None, msg="no account created by factory")

    def test_adminAccountFactory(self):
        af = AccountFactory.AccountFactory()
        accnt = Account.Account.create(apiclient=self.apiClient, AccountFactory=af)
        self.assertTrue(accnt is not None, msg="no account created by factory")

    def test_userAccountFactoryCustomArgs(self):
        af = AccountFactory.AccountFactory(firstname='test', lastname='test')
        accnt = Account.Account.create(apiclient=self.apiClient, AccountFactory=af)
        self.assertTrue(accnt is not None, msg="no account created by factory")

    def tearDown(self):
        self.apiClient.close()