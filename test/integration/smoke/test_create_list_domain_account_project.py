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
""" tests for create/list domain,account,project
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Account,
                             Project,
                             Domain)
from marvin.lib.common import get_domain

class Services:

    """Test Project Services
    """

    def __init__(self):
        self.services = {
            "domain": {
                "name": "Test Domain",
            },
            "project": {
                "name": "Test Project",
                "displaytext": "Test project",
            },
            "account": {
                "email": "administrator@cloudstack.apache.org",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                "password": "password",
            },
        }


class TestDomainAccountProject(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestDomainAccountProject,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.domain = get_domain(cls.api_client)
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_01_create_list_domain_account_project(self):
        """ Verify list domain, account and project return expected response
        """
        # Validate the following
        # 1. Create domain
        # 2. list domain, 'cpulimit' should be included in response
        # 3. list domain with details=min, 'cpulimit' should not be included in response.

        # 4. create account in the domain
        # 5. list account, 'cpulimit' should be included in response
        # 6. list account with details=min, 'cpulimit' should not be included in response.

        # 7. create project in the domain
        # 8. list project, 'cpulimit' should be included in response
        # 9. list project with details=min, 'cpulimit' should not be included in response.

        # Create new domain
        self.user_domain = Domain.create(
            self.apiclient,
            self.services["domain"],
            parentdomainid=self.domain.id)

        list_domain_response = Domain.list(
            self.apiclient,
            id = self.user_domain.id)

        self.assertEqual(
            isinstance(list_domain_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertIsNotNone(list_domain_response[0].cpulimit, "'cpulimit' should be included in response")

        list_domain_response = Domain.list(
            self.apiclient,
            details="min",
            id = self.user_domain.id)

        self.assertEqual(
            isinstance(list_domain_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertIsNone(list_domain_response[0].cpulimit, "'cpulimit' should not be included in response")

        # Create account
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
            domainid=self.user_domain.id
        )

        list_account_response = Account.list(
            self.apiclient,
            id = self.account.id)

        self.assertEqual(
            isinstance(list_account_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertIsNotNone(list_account_response[0].cpulimit, "'cpulimit' should be included in response")

        list_account_response = Account.list(
            self.apiclient,
            details="min",
            id = self.account.id)

        self.assertEqual(
            isinstance(list_account_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertIsNone(list_account_response[0].cpulimit, "'cpulimit' should not be included in response")

        # Create project
        self.project = Project.create(
            self.apiclient,
            self.services["project"],
            account=self.account.name,
            domainid=self.account.domainid
        )

        list_project_response = Project.list(
            self.apiclient,
            listall="true",
            id = self.project.id)

        self.assertEqual(
            isinstance(list_project_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertIsNotNone(list_project_response[0].cpulimit, "'cpulimit' should be included in response")

        list_project_response = Project.list(
            self.apiclient,
            details="min",
            listall="true",
            id = self.project.id)

        self.assertEqual(
            isinstance(list_project_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertIsNone(list_project_response[0].cpulimit, "'cpulimit' should not be included in response")

        self.cleanup.append(self.project)
        self.cleanup.append(self.account)
        self.cleanup.append(self.user_domain)
