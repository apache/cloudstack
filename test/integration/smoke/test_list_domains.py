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
""" Tests for API listing of domains with different filters
"""

# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             Domain)
from marvin.lib.common import (get_domain, list_accounts)
# Import System modules
from nose.plugins.attrib import attr

_multiprocess_shared_ = True


class TestListDomains(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestListDomains, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.domain = get_domain(cls.apiclient)
        cls.account = list_accounts(cls.apiclient, name="admin")[0]
        cls._cleanup = []

        cls.child_domain_1 = Domain.create(
            cls.apiclient,
            cls.services["domain"],
            parentdomainid=cls.domain.id
        )
        cls._cleanup.append(cls.child_domain_1)

        cls.child_account_1 = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.child_domain_1.id
        )
        cls._cleanup.append(cls.child_account_1)

        cls.child_account_apiclient = testClient.getUserApiClient(cls.child_account_1.user[0]['username'], cls.child_domain_1.name, type=2)

        cls.child_domain_2 = Domain.create(
            cls.apiclient,
            cls.services["domain"],
            parentdomainid=cls.child_domain_1.id
        )
        cls._cleanup.append(cls.child_domain_2)

    @classmethod
    def tearDownClass(cls):
        super(TestListDomains, cls).tearDownClass()

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_01_list_domains_id_filter(self):
        """ Test list domains with id filter
        """
        # List all domains
        domains = Domain.list(self.apiclient, id=self.domain.id)
        self.assertEqual(
            isinstance(domains, list),
            True,
            "List Domain response is not a valid list"
        )
        self.assertEqual(
            len(domains),
            1,
            "List Domain response has incorrect length"
        )
        self.assertEqual(
            domains[0].id,
            self.domain.id,
            "Check if list domains returns valid domain"
        )

        # List all domains with a non-existent id
        with self.assertRaises(Exception):
            Domain.list(self.apiclient, id=-1)

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_02_list_domains_name_filter(self):
        """ Test list domains with name filter
        """
        # List all domains
        domains = Domain.list(self.apiclient, name=self.domain.name)
        self.assertEqual(
            isinstance(domains, list),
            True,
            "List Domain response is not a valid list"
        )
        self.assertEqual(
            len(domains),
            1,
            "List Domain response has incorrect length"
        )
        self.assertEqual(
            domains[0].name,
            self.domain.name,
            "Check if list domains returns valid domain"
        )

        domains = Domain.list(self.apiclient, name="non-existent-domain")
        self.assertIsNone(domains, "List Domain response is not None")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_03_list_domains_listall_filter(self):
        """ Test list domains with listall parameter
        """
        # List all domains
        domains = Domain.list(self.child_account_apiclient, listall=True)
        self.assertEqual(
            isinstance(domains, list),
            True,
            "List Domain response is not a valid list"
        )
        self.assertEqual(
            len(domains),
            2,
            "List Domain response has incorrect length"
        )

        domains = Domain.list(self.child_account_apiclient, listall=False)
        self.assertEqual(
            isinstance(domains, list),
            True,
            "List Domain response is not a valid list"
        )
        self.assertEqual(
            len(domains),
            1,
            "List Domain response has incorrect length"
        )
        self.assertEqual(
            domains[0].id,
            self.child_domain_1.id,
            "Check if list domains returns valid domain"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_04_list_domains_level_filter(self):
        """ Test list domains with level filter
        """
        # List all domains
        domains = Domain.list(self.apiclient, level=0)
        self.assertEqual(
            isinstance(domains, list),
            True,
            "List Domain response is not a valid list"
        )
        self.assertEqual(
            len(domains),
            1,
            "List Domain response has incorrect length"
        )
        self.assertEqual(
            domains[0].id,
            self.domain.id,
            "Check if list domains returns valid domain"
        )

        domains = Domain.list(self.apiclient, level=1)
        self.assertEqual(
            isinstance(domains, list),
            True,
            "List Domain response is not a valid list"
        )
        self.assertEqual(
            len(domains),
            1,
            "List Domain response has incorrect length"
        )

        domains = Domain.list(self.apiclient, level=2)
        self.assertEqual(
            isinstance(domains, list),
            True,
            "List Domain response is not a valid list"
        )
        self.assertEqual(
            len(domains),
            1,
            "List Domain response has incorrect length"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_05_list_domains_no_filter(self):
        """ Test list domains with no filter
        """
        # List all domains
        domains = Domain.list(self.apiclient)
        self.assertEqual(
            isinstance(domains, list),
            True,
            "List Domain response is not a valid list"
        )
        self.assertEqual(
            len(domains),
            3,
            "List Domain response has incorrect length"
        )
