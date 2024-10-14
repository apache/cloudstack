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
""" BVT tests for Account User Access
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import *
from marvin.lib.base import (Account,
                             User,
                             Domain)
from marvin.lib.common import (get_domain)
from marvin.cloudstackAPI import (getUserKeys)
from marvin.cloudstackException import CloudstackAPIException
from nose.plugins.attrib import attr

_multiprocess_shared_ = True

class TestAccountAccess(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestAccountAccess, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        cls._cleanup = []

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)

        cls.domains = []
        cls.domain_admins = {}
        cls.domain_users = {}
        cls.account_users = {}

        domain_data = {
            "name": "domain_1"
        }
        cls.domain_1 = Domain.create(
            cls.apiclient,
            domain_data,
        )
        cls._cleanup.append(cls.domain_1)
        cls.domains.append(cls.domain_1)
        domain_data["name"] = "domain_11"
        cls.domain_11 = Domain.create(
            cls.apiclient,
            domain_data,
            parentdomainid=cls.domain_1.id
        )
        cls._cleanup.append(cls.domain_11)
        cls.domains.append(cls.domain_11)
        domain_data["name"] = "domain_12"
        cls.domain_12 = Domain.create(
            cls.apiclient,
            domain_data,
            parentdomainid=cls.domain_1.id
        )
        cls._cleanup.append(cls.domain_12)
        cls.domains.append(cls.domain_12)
        domain_data["name"] = "domain_2"
        cls.domain_2 = Domain.create(
            cls.apiclient,
            domain_data,
        )
        cls._cleanup.append(cls.domain_2)
        cls.domains.append(cls.domain_2)


        for d in cls.domains:
            cls.create_domainadmin_and_user(d)

    @classmethod
    def tearDownClass(cls):
        super(TestAccountAccess, cls).tearDownClass()

    @classmethod
    def create_account(cls, domain, is_admin):
        cls.debug(f"Creating account for domain {domain.name}, admin: {is_admin}")
        data = {
            "email": "admin-" + domain.name + "@test.com",
            "firstname": "Admin",
            "lastname": domain.name,
            "username": "admin-" + domain.name,
            "password": "password"
        }
        if is_admin == False:
            data["email"] = "user-" + domain.name + "@test.com"
            data["firstname"] = "User"
            data["username"] = "user-" + domain.name
        account = Account.create(
            cls.apiclient,
            data,
            admin=is_admin,
            domainid=domain.id
        )
        cls._cleanup.append(account)
        if is_admin == True:
            cls.domain_admins[domain.id] = account
        else:
            cls.domain_users[domain.id] = account

        user = User.create(
            cls.apiclient,
            data,
            account=account.name,
            domainid=account.domainid)
        cls._cleanup.append(user)
        cls.account_users[account.id] = user

    @classmethod
    def create_domainadmin_and_user(cls, domain):
        cls.debug(f"Creating accounts for domain #{domain.id} {domain.name}")
        cls.create_account(domain, True)
        cls.create_account(domain, False)

    def get_user_keys(self, api_client, user_id):
        getUserKeysCmd = getUserKeys.getUserKeysCmd()
        getUserKeysCmd.id = user_id
        return api_client.getUserKeys(getUserKeysCmd)

    def is_child_domain(self, parent_domain, child_domain):
        if not parent_domain or not child_domain:
            return False
        parent_domain_prefix = parent_domain.split('-')[0]
        child_domain_prefix = child_domain.split('-')[0]
        if not parent_domain_prefix or not child_domain_prefix:
            return False
        return child_domain_prefix.startswith(parent_domain_prefix)


    @attr(tags=["advanced", "advancedns", "smoke", "sg"], required_hardware="false")
    def test_01_user_access(self):
        """
        Test user account is not accessing any other account
        """

        domain_user_accounts = [value for value in self.domain_users.values()]
        all_account_users = [value for value in self.account_users.values()]
        for user_account in domain_user_accounts:
            current_account_user = self.account_users[user_account.id]
            self.debug(f"Check for account {user_account.name} with user {current_account_user.username}")
            user_api_client = self.testClient.getUserApiClient(
                UserName=user_account.name,
                DomainName=user_account.domain
            )
            for user in all_account_users:
                self.debug(f"Checking access for user {user.username} associated with account {user.account}")
                try:
                    self.get_user_keys(user_api_client, user.id)
                    self.debug(f"API successful")
                    if user.id != current_account_user.id:
                        self.fail(f"User account #{user_account.id} was able to access another account #{user.id}")
                except CloudstackAPIException as e:
                    self.debug(f"Exception occurred: {e}")
                    if user.id == current_account_user.id:
                        self.fail(f"User account #{user_account.id} not able to access own account")

    @attr(tags=["advanced", "advancedns", "smoke", "sg"], required_hardware="false")
    def test_02_domain_admin_access(self):
        """
        Test domain admin account is not accessing any other account from unauthorized domain
        """

        domain_admin_accounts = [value for value in self.domain_admins.values()]
        all_account_users = [value for value in self.account_users.values()]
        for admin_account in domain_admin_accounts:
            current_account_user = self.account_users[admin_account.id]
            self.debug(f"Check for domain admin {admin_account.name} with user {current_account_user.username}, {current_account_user.domain}")
            admin_api_client = self.testClient.getUserApiClient(
                UserName=admin_account.name,
                DomainName=admin_account.domain
            )
            for user in all_account_users:
                self.debug(f"Checking access for user {user.username}, {user.domain} associated with account {user.account}")
                try:
                    self.get_user_keys(admin_api_client, user.id)
                    self.debug(f"API successful")
                    if self.is_child_domain(current_account_user.domain, user.domain) == False:
                        self.fail(f"User account #{admin_account.id} was able to access another account #{user.id}")
                except CloudstackAPIException as e:
                    self.debug(f"Exception occurred: {e}")
                    if self.is_child_domain(current_account_user.domain, user.domain) == True:
                        self.fail(f"User account #{admin_account.id} not able to access own account")
