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

import factory
import marvin
from marvin import cloudstackTestCase
from marvin.integration.lib.base import *
from marvin.integration.lib import utils

class AccountFactory(factory.Factory):
    FACTORY_FOR = createAccount.createAccountCmd

    firstname = 'firstname-'+random_gen()
    lastname = 'lastname-'+random_gen()
    email = factory.lazy_attribute(lambda e: '{0}.{1}@cloudstack.org'.format(e.firstname, e.lastname).lower())

class AdminAccountFactory(AccountFactory):
    accounttype = 1

class UserAccountFactory(AccountFactory):
    accounttype = 0

class TestCreateAccount(cloudstackTestCase):
    def setUp(self):
        self.apiClient = self.testClient.getApiClient()
        self.userApiClient = self.testClient.getUserApiClient(account='test'+utils.random_gen(), 'ROOT')

    def test_createAccountAsAdmin(self):
        """
        creates an account for a user as admin
        """
        Account.create(self.apiClient, services=None)
        from marvin.cloudstackAPI.createAccount import createAccountCmd
        self.assertEqual(True, False)

    def test_createAccountAsUser(self):
        """
        negative: create account as a user
        """
        self.assertEqual(True, False)

    def tearDown(self):
        self.apiClient.close()
        self.userApiClient.close()

if __name__ == '__main__':
    unittest.main()
