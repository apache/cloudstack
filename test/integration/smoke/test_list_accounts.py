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
""" Tests for API listing of accounts with different filters
"""

# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             Domain)
from marvin.lib.common import (get_domain, list_accounts)
# Import System modules
from nose.plugins.attrib import attr

_multiprocess_shared_ = True


class TestListAccounts(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestListAccounts, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.domain = get_domain(cls.apiclient)
        cls.account = list_accounts(cls.apiclient, name="admin")[0]
        cls._cleanup = []
        cls.accounts = list_accounts(cls.apiclient, listall=True)

        cls.child_domain_1 = Domain.create(
            cls.apiclient,
            cls.services["domain"],
            parentdomainid=cls.domain.id
        )
        cls._cleanup.append(cls.child_domain_1)

        cls.services["account"]["username"] = "child_account_admin"
        cls.child_account_admin = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.child_domain_1.id
        )
        cls._cleanup.append(cls.child_account_admin)

        cls.services["username"] = "child_account_user"
        cls.child_account_user = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=0,
            domainid=cls.child_domain_1.id
        )
        cls.child_account_user.disable(cls.apiclient)
        cls._cleanup.append(cls.child_account_user)

        cls.child_domain_2 = Domain.create(
            cls.apiclient,
            cls.services["domain"],
            parentdomainid=cls.domain.id
        )
        cls._cleanup.append(cls.child_domain_2)

    @classmethod
    def tearDownClass(cls):
        super(TestListAccounts, cls).tearDownClass()

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_01_list_accounts_accounttype_filter(self):
        """Test listing accounts with accounttype filter
        """
        list_account_response = Account.list(
            self.apiclient,
            accounttype=0,
            domainid=self.child_domain_1.id
        )
        self.assertTrue(
            isinstance(list_account_response, list),
            "List Account response is not a valid list"
        )
        self.assertEqual(
            len(list_account_response),
            1,
            "List Account response has incorrect length"
        )
        self.assertEqual(
            list_account_response[0].name,
            self.child_account_user.name,
            "Check for list response return valid data"
        )
        self.assertEqual(
            list_account_response[0].accounttype,
            0,
            "Check for list response return valid data"
        )

        list_account_response = Account.list(
            self.apiclient,
            accounttype=2,
            domainid=self.child_domain_1.id
        )
        self.assertTrue(
            isinstance(list_account_response, list),
            "List Account response is not a valid list"
        )
        self.assertEqual(
            len(list_account_response),
            1,
            "List Account response has incorrect length"
        )
        self.assertEqual(
            list_account_response[0].name,
            self.child_account_admin.name,
            "Check for list response return valid data"
        )
        self.assertEqual(
            list_account_response[0].accounttype,
            2,
            "Check for list response return valid data"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_02_list_accounts_domainid_filter(self):
        """Test listing accounts with domainid filter
        """
        list_account_response = Account.list(
            self.apiclient,
            domainid=self.child_domain_1.id
        )
        self.assertTrue(
            isinstance(list_account_response, list),
            "List Account response is not a valid list"
        )
        self.assertEqual(
            len(list_account_response),
            2,
            "List Account response has incorrect length"
        )
        self.assertEqual(
            self.child_domain_1.id,
            list_account_response[0].domainid,
            "Check for list response return valid data"
        )
        self.assertEqual(
            self.child_domain_1.id,
            list_account_response[1].domainid,
            "Check for list response return valid data"
        )

        list_account_response = Account.list(
            self.apiclient,
            domainid=self.child_domain_2.id
        )
        self.assertIsNone(list_account_response, "Check for list response return valid data")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_03_list_accounts_id_filter(self):
        """Test listing accounts with id filter
        """
        list_account_response = Account.list(
            self.apiclient,
            id=self.child_account_user.id
        )
        self.assertTrue(
            isinstance(list_account_response, list),
            "List Account response is not a valid list"
        )
        self.assertEqual(
            len(list_account_response),
            1,
            "List Account response has incorrect length"
        )
        self.assertEqual(
            list_account_response[0].name,
            self.child_account_user.name,
            "Expected account name and actual account name should be same"
        )

        list_account_response = Account.list(
            self.apiclient,
            id=self.child_account_admin.id
        )
        self.assertTrue(
            isinstance(list_account_response, list),
            "List Account response is not a valid list"
        )
        self.assertEqual(
            len(list_account_response),
            1,
            "List Account response has incorrect length"
        )
        self.assertEqual(
            list_account_response[0].name,
            self.child_account_admin.name,
            "Expected account name and actual account name should be same"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_04_list_accounts_name_filter(self):
        """Test listing accounts with name filter
        """
        list_account_response = Account.list(
            self.apiclient,
            name=self.child_account_user.name,
            domainid=self.child_domain_1.id
        )
        self.assertTrue(
            isinstance(list_account_response, list),
            "List Account response is not a valid list"
        )
        self.assertEqual(
            len(list_account_response),
            1,
            "List Account response has incorrect length"
        )
        self.assertEqual(
            list_account_response[0].name,
            self.child_account_user.name,
            "Expected account name and actual account name should be same"
        )

        list_account_response = Account.list(
            self.apiclient,
            name=self.child_account_admin.name,
            domainid=self.child_domain_1.id
        )
        self.assertTrue(
            isinstance(list_account_response, list),
            "List Account response is not a valid list"
        )
        self.assertEqual(
            len(list_account_response),
            1,
            "List Account response has incorrect length"
        )
        self.assertEqual(
            list_account_response[0].name,
            self.child_account_admin.name,
            "Expected account name and actual account name should be same"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_05_list_accounts_state_filter(self):
        """Test listing accounts with state filter
        """
        list_account_response = Account.list(
            self.apiclient,
            state="enabled",
            domainid=self.child_domain_1.id
        )
        self.assertTrue(
            isinstance(list_account_response, list),
            "List Account response is not a valid list"
        )
        self.assertEqual(
            len(list_account_response),
            1,
            "List Account response has incorrect length"
        )
        self.assertEqual(
            list_account_response[0].name,
            self.child_account_admin.name,
            "Expected account name and actual account name should be same"
        )

        list_account_response = Account.list(
            self.apiclient,
            state="disabled",
            domainid=self.child_domain_1.id
        )
        self.assertTrue(
            isinstance(list_account_response, list),
            "List Account response is not a valid list"
        )
        self.assertEqual(
            len(list_account_response),
            1,
            "List Account response has incorrect length"
        )
        self.assertEqual(
            list_account_response[0].name,
            self.child_account_user.name,
            "Expected account name and actual account name should be same"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_06_list_accounts_keyword_filter(self):
        """Test listing accounts with keyword filter
        """
        list_account_response = Account.list(
            self.apiclient,
            keyword=self.child_account_user.name,
            domainid=self.child_domain_1.id
        )
        self.assertTrue(
            isinstance(list_account_response, list),
            "List Account response is not a valid list"
        )
        self.assertEqual(
            self.child_account_user.name,
            list_account_response[0].name,
            "Expected account name and actual account name should be same"
        )

        list_account_response = Account.list(
            self.apiclient,
            keyword=self.child_account_admin.name,
            domainid=self.child_domain_1.id
        )
        self.assertTrue(
            isinstance(list_account_response, list),
            "List Account response is not a valid list"
        )
        self.assertEqual(
            self.child_account_admin.name,
            list_account_response[0].name,
            "Expected account name and actual account name should be same"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_07_list_accounts_with_listall_filters(self):
        """Test listing accounts with listall filters
        """
        list_account_response = Account.list(
            self.apiclient,
            listall=False
        )
        self.assertTrue(
            isinstance(list_account_response, list),
            "List Account response is not a valid list"
        )
        self.assertEqual(
            1,
            len(list_account_response),
            "List Account response has incorrect length"
        )

        list_account_response = Account.list(
            self.apiclient,
            listall=True
        )
        self.assertTrue(
            isinstance(list_account_response, list),
            "List Account response is not a valid list"
        )
        self.assertEqual(
            2,
            len(list_account_response) - len(self.accounts),
            "List Account response has incorrect length"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_08_list_accounts_with_no_filters(self):
        """Test listing accounts with no filters
        """
        list_account_response = Account.list(
            self.apiclient
        )
        self.assertTrue(
            isinstance(list_account_response, list),
            "List Account response is not a valid list"
        )
        self.assertEqual(
            1,
            len(list_account_response),
            "List Account response has incorrect length"
        )
