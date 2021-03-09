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
""" P1 tests for Project
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Account,
                             ServiceOffering,
                             Project,
                             VirtualMachine,
                             Volume,
                             DiskOffering,
                             Configurations,
                             Domain)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_test_template,
                               list_configurations)
import time


class Services:

    """Test Project Services
    """

    def __init__(self):
        self.services = {
            "domain": {
                "name": "Domain",
            },
            "project": {
                "name": "Project",
                "displaytext": "Test project",
            },
            "mgmt_server": {
                "ipaddress": '192.168.100.21',
                "username": 'root',
                "password": 'password',
                "port": 22,
            },
            "account": {
                "email": "administrator@clogeny.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                # Random characters are appended for unique
                # username
                "password": "password",
            },
            "user": {
                "email": "administrator@clogeny.com",
                "firstname": "User",
                "lastname": "User",
                "username": "User",
                # Random characters are appended for unique
                # username
                "password": "password",
            },
            "disk_offering": {
                "displaytext": "Tiny Disk Offering",
                "name": "Tiny Disk Offering",
                "disksize": 1
            },
            "volume": {
                "diskname": "Test Volume",
            },
            "service_offering": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100,    # in MHz
                                    "memory": 128,       # In MBs
            },
            "virtual_machine": {
                "displayname": "Test VM",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'XenServer',
                # Hypervisor type should be same as
                # hypervisor type of cluster
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },
            "ostype": 'CentOS 5.3 (64-bit)',
            # Cent OS 5.3 (64 bit)
            "sleep": 60,
            "timeout": 10,
        }


class TestMultipleProjectCreation(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestMultipleProjectCreation,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls._cleanup = []
        cls.isGlobalSettingInvalid = False
        configs = Configurations.list(
            cls.api_client,
            name='project.invite.required'
        )

        if (configs[0].value).lower() != 'false':
            cls.isGlobalSettingInvalid = True
            return

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.user = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.user)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, reversed(cls._cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.isGlobalSettingInvalid:
            self.skipTest("'project.invite.required' should be set to false")
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, reversed(self.cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "sg",
            "eip",
            "advancedns",
            "simulator"],
        required_hardware="false")
    def test_01_create_multiple_projects_by_account(self):
        """ Verify an account can own multiple projects and can belong to multiple projects
        """
        # Validate the following
        # 1. Create multiple project. Verify at step 1 An account is allowed
        #    to create multiple projects
        # 2. add one account to multiple project. Verify at step 2 an account
        #    is allowed to added to multiple project

        # Create project as a domain admin
        project_1 = Project.create(
            self.apiclient,
            self.services["project"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        # Cleanup created project at end of test
        self.cleanup.append(project_1)
        self.debug("Created project with domain admin with ID: %s" %
                   project_1.id)

        list_projects_reponse = Project.list(
            self.apiclient,
            id=project_1.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_projects_reponse, list),
            True,
            "Check for a valid list projects response"
        )
        list_project = list_projects_reponse[0]

        self.assertNotEqual(
            len(list_projects_reponse),
            0,
            "Check list project response returns a valid project"
        )

        self.assertEqual(
            project_1.name,
            list_project.name,
            "Check project name from list response"
        )
        # Create another project as a domain admin
        project_2 = Project.create(
            self.apiclient,
            self.services["project"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        # Cleanup created project at end of test
        self.cleanup.append(project_2)
        self.debug("Created project with domain user with ID: %s" %
                   project_2.id)

        list_projects_reponse = Project.list(
            self.apiclient,
            id=project_2.id,
            listall=True
        )

        self.assertEqual(
            isinstance(list_projects_reponse, list),
            True,
            "Check for a valid list projects response"
        )
        list_project = list_projects_reponse[0]

        self.assertNotEqual(
            len(list_projects_reponse),
            0,
            "Check list project response returns a valid project"
        )

        self.assertTrue(isinstance(self.user.user, list))
        self.assertTrue(len(self.user.user) > 0,
                     msg="Account %s has no users" % self.user.name)
        self.debug(
            "Adding account %s to project with email %s" %
            (self.user.name, self.user.user[0].email))
        email = self.user.user[0].email

        # Add user to the project
        project_1.addAccount(
            self.apiclient,
            self.user.name,
            email
        )

        # listProjectAccount to verify the user is added to project or not
        accounts_response = Project.listAccounts(
            self.apiclient,
            projectid=project_1.id,
            account=self.user.name,
        )
        self.debug(accounts_response)
        self.assertEqual(
            isinstance(accounts_response, list),
            True,
            "Check for a valid list accounts response"
        )

        self.assertNotEqual(
            len(list_projects_reponse),
            0,
            "Check list project response returns a valid project"
        )
        account = accounts_response[0]

        self.assertEqual(
            account.role,
            'Regular',
            "Newly added user is not added as a regular user"
        )
        # Add user to the project
        project_2.addAccount(
            self.apiclient,
            self.user.name,
            email
        )

        # listProjectAccount to verify the user is added to project or not
        accounts_response = Project.listAccounts(
            self.apiclient,
            projectid=project_2.id,
            account=self.user.name,
        )
        self.debug(accounts_response)
        self.assertEqual(
            isinstance(accounts_response, list),
            True,
            "Check for a valid list accounts response"
        )

        self.assertNotEqual(
            len(list_projects_reponse),
            0,
            "Check list project response returns a valid project"
        )
        account = accounts_response[0]

        self.assertEqual(
            account.role,
            'Regular',
            "Newly added user is not added as a regular user"
        )
        return


class TestCrossDomainAccountAdd(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestCrossDomainAccountAdd,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        cls._cleanup = []
        cls.isGlobalSettingInvalid = False
        configs = Configurations.list(
            cls.api_client,
            name='project.invite.required'
        )

        if (configs[0].value).lower() != 'false':
            cls.isGlobalSettingInvalid = True
            return

        # Create domains, account etc.
        cls.new_domain = Domain.create(
            cls.api_client,
            cls.services["domain"]
        )
        cls._cleanup.append(cls.new_domain)

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.user = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.new_domain.id
        )
        cls._cleanup.append(cls.user)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, reversed(cls._cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.isGlobalSettingInvalid:
            self.skipTest("'project.invite.required' should be set to false")
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, reversed(self.cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "sg",
            "eip",
            "advancedns",
            "simulator"],
        required_hardware="false")
    def test_02_cross_domain_account_add(self):
        """ Verify No cross domain projects
        """
        # Validate the following
        # 1. Create a project in a domain.
        # 2. Add different domain account to the project. Add account should
        #    fail

        # Create project as a domain admin
        project = Project.create(
            self.apiclient,
            self.services["project"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        # Cleanup created project at end of test
        self.cleanup.append(project)
        self.debug("Created project with domain admin with ID: %s" %
                   project.id)

        list_projects_reponse = Project.list(
            self.apiclient,
            id=project.id,
            listall=True
        )

        self.assertEqual(
            isinstance(list_projects_reponse, list),
            True,
            "Check for a valid list projects response"
        )
        list_project = list_projects_reponse[0]

        self.assertNotEqual(
            len(list_projects_reponse),
            0,
            "Check list project response returns a valid project"
        )

        self.assertEqual(
            project.name,
            list_project.name,
            "Check project name from list response"
        )

        self.debug("Adding user: %s from domain: %s to project: %s" % (
            self.user.name,
            self.user.domainid,
            project.id
        ))
        with self.assertRaises(Exception):
            # Add user to the project from different domain
            project.addAccount(
                self.apiclient,
                self.user.name
            )
            self.debug("User add to project failed!")
        return


class TestDeleteAccountWithProject(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestDeleteAccountWithProject,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        cls._cleanup = []
        cls.isGlobalSettingInvalid = False
        configs = Configurations.list(
            cls.api_client,
            name='project.invite.required'
        )

        if (configs[0].value).lower() != 'false':
            cls.isGlobalSettingInvalid = True
            return

        # Create account
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, reversed(cls._cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.isGlobalSettingInvalid:
            self.skipTest("'project.invite.required' should be set to false")
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, reversed(self.cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "sg",
            "eip",
            "advancedns",
            "simulator"],
        required_hardware="false")
    def test_03_delete_account_with_project(self):
        """ Test As long as the project exists, its owner can't be removed
        """
        # Validate the following
        # 1. Create a project.
        # 2. Delete account who is owner of the project. Delete account should
        #    fail

        # Create project as a domain admin
        project = Project.create(
            self.apiclient,
            self.services["project"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        # Cleanup created project at end of test
        self.cleanup.append(project)
        self.debug("Created project with domain admin with ID: %s" %
                   project.id)

        list_projects_reponse = Project.list(
            self.apiclient,
            id=project.id,
            listall=True
        )

        self.assertEqual(
            isinstance(list_projects_reponse, list),
            True,
            "Check for a valid list projects response"
        )
        list_project = list_projects_reponse[0]

        self.assertNotEqual(
            len(list_projects_reponse),
            0,
            "Check list project response returns a valid project"
        )

        self.assertEqual(
            project.name,
            list_project.name,
            "Check project name from list response"
        )
        # Deleting account who is owner of the project
        with self.assertRaises(Exception):
            self.account.delete(self.apiclient)
            self.debug("Deleting account %s failed!" %
                       self.account.name)
        return


class TestDeleteDomainWithProject(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestDeleteDomainWithProject,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        cls._cleanup = []
        cls.isGlobalSettingInvalid = False
        configs = Configurations.list(
            cls.api_client,
            name='project.invite.required'
        )

        if (configs[0].value).lower() != 'false':
            cls.isGlobalSettingInvalid = True
            return

        # Create account
        cls.domain = Domain.create(
            cls.api_client,
            cls.services["domain"]
        )

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
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
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.isGlobalSettingInvalid:
            self.skipTest("'project.invite.required' should be set to false")
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "sg",
            "eip",
            "advancedns",
            "simulator"],
        required_hardware="false")
    def test_04_delete_domain_with_project(self):
        """ Test Verify delete domain with cleanup=true should delete projects
            belonging to the domain
        """
        # Validate the following
        # 1. Create a project in a domain
        # 2. Delete domain forcefully. Verify that project is also deleted as
        #    as part of domain cleanup

        # Create project as a domain admin
        project = Project.create(
            self.apiclient,
            self.services["project"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        # Cleanup created project at end of test
        self.debug("Created project with domain admin with ID: %s" %
                   project.id)

        list_projects_reponse = Project.list(
            self.apiclient,
            id=project.id,
            listall=True
        )

        self.assertEqual(
            isinstance(list_projects_reponse, list),
            True,
            "Check for a valid list projects response"
        )
        list_project = list_projects_reponse[0]

        self.assertNotEqual(
            len(list_projects_reponse),
            0,
            "Check list project response returns a valid project"
        )

        self.assertEqual(
            project.name,
            list_project.name,
            "Check project name from list response"
        )

        self.debug("Deleting domain: %s forcefully" % self.domain.name)
        # Delete domain with cleanup=True
        self.domain.delete(self.apiclient, cleanup=True)
        self.debug("Removed domain: %s" % self.domain.name)

        interval = list_configurations(
            self.apiclient,
            name='account.cleanup.interval'
        )
        self.assertEqual(
            isinstance(interval, list),
            True,
            "Check if account.cleanup.interval config present"
        )
        self.debug(
            "Sleep for account cleanup interval: %s" %
            interval[0].value)
        # Sleep to ensure that all resources are deleted
        time.sleep(int(interval[0].value))

        # Project should be deleted as part of domain cleanup
        list_projects_reponse = Project.list(
            self.apiclient,
            id=project.id,
            listall=True
        )
        self.assertEqual(
            list_projects_reponse,
            None,
            "Project should be deleted as part of domain cleanup"
        )
        return


class TestProjectOwners(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestProjectOwners, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.domain = get_domain(cls.api_client)
        cls.services['mode'] = cls.zone.networktype

        cls._cleanup = []
        cls.isGlobalSettingInvalid = False
        configs = Configurations.list(
            cls.api_client,
            name='project.invite.required'
        )

        if (configs[0].value).lower() != 'false':
            cls.isGlobalSettingInvalid = True
            return

        # Create accounts
        cls.admin = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.admin)
        cls.new_admin = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.new_admin)
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
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.isGlobalSettingInvalid:
            self.skipTest("'project.invite.required' should be set to false")
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "sg",
            "eip",
            "advancedns",
            "simulator"],
        required_hardware="false")
    def test_05_user_project_owner_promotion(self):
        """ Test Verify a project user can be later promoted to become a
            owner
        """
        # Validate the following
        # 1. Create a project.
        # 2. Add account to the project. Edit account to make it a project
        #    owner. verify new user is project owner and old account is
        #    regular user of the project.

        # Create project as a domain admin
        project = Project.create(
            self.apiclient,
            self.services["project"],
            account=self.admin.name,
            domainid=self.admin.domainid
        )
        self.cleanup.append(project)
        # Cleanup created project at end of test
        self.debug("Created project with domain admin with ID: %s" %
                   project.id)

        list_projects_reponse = Project.list(
            self.apiclient,
            id=project.id,
            listall=True
        )

        self.assertEqual(
            isinstance(list_projects_reponse, list),
            True,
            "Check for a valid list projects response"
        )
        list_project = list_projects_reponse[0]

        self.assertNotEqual(
            len(list_projects_reponse),
            0,
            "Check list project response returns a valid project"
        )

        self.assertEqual(
            project.name,
            list_project.name,
            "Check project name from list response"
        )
        self.debug("Adding %s user to project: %s" % (
            self.new_admin.name,
            project.name
        ))
        # Add user to the project
        project.addAccount(
            self.apiclient,
            self.new_admin.name,
        )

        # listProjectAccount to verify the user is added to project or not
        accounts_reponse = Project.listAccounts(
            self.apiclient,
            projectid=project.id,
            account=self.new_admin.name,
        )
        self.debug(accounts_reponse)
        self.assertEqual(
            isinstance(accounts_reponse, list),
            True,
            "Check for a valid list accounts response"
        )

        self.assertNotEqual(
            len(list_projects_reponse),
            0,
            "Check list project response returns a valid project"
        )
        account = accounts_reponse[0]

        self.assertEqual(
            account.role,
            'Regular',
            "Newly added user is not added as a regular user"
        )

        # Update the project with new admin
        project.update(
            self.apiclient,
            account=self.new_admin.name
        )

        # listProjectAccount to verify the user is new admin of the project
        accounts_reponse = Project.listAccounts(
            self.apiclient,
            projectid=project.id,
            account=self.new_admin.name,
        )
        self.debug(accounts_reponse)
        self.assertEqual(
            isinstance(accounts_reponse, list),
            True,
            "Check for a valid list accounts response"
        )

        self.assertNotEqual(
            len(list_projects_reponse),
            0,
            "Check list project response returns a valid project"
        )
        account = accounts_reponse[0]

        self.assertEqual(
            account.role,
            'Admin',
            "Newly added user is not added as a regular user"
        )

        # listProjectAccount to verify old user becomes a regular user
        accounts_reponse = Project.listAccounts(
            self.apiclient,
            projectid=project.id,
            account=self.admin.name,
        )
        self.debug(accounts_reponse)
        self.assertEqual(
            isinstance(accounts_reponse, list),
            True,
            "Check for a valid list accounts response"
        )

        self.assertNotEqual(
            len(list_projects_reponse),
            0,
            "Check list project response returns a valid project"
        )
        account = accounts_reponse[0]

        self.assertEqual(
            account.role,
            'Regular',
            "Newly added user is not added as a regular user"
        )
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "sg",
            "eip",
            "advancedns",
            "simulator"],
        required_hardware="false")
    def test_06_max_one_project_owner(self):
        """ Test Verify there can only be one owner of a project at a time
        """
        # Validate the following
        # 1. Create a project.
        # 2. Add account to the project. Edit account to make it a project
        #    owner.
        # 3. Update project to add another account as an owner

        # Create project as a domain admin
        project = Project.create(
            self.apiclient,
            self.services["project"],
            account=self.admin.name,
            domainid=self.admin.domainid
        )
        # Cleanup created project at end of test
        self.cleanup.append(project)
        self.debug("Created project with domain admin with ID: %s" %
                   project.id)
        self.user = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup.append(self.user)
        self.debug("Created account with ID: %s" %
                   self.user.name)

        list_projects_reponse = Project.list(
            self.apiclient,
            id=project.id,
            listall=True
        )

        self.assertEqual(
            isinstance(list_projects_reponse, list),
            True,
            "Check for a valid list projects response"
        )
        list_project = list_projects_reponse[0]

        self.assertNotEqual(
            len(list_projects_reponse),
            0,
            "Check list project response returns a valid project"
        )

        self.assertEqual(
            project.name,
            list_project.name,
            "Check project name from list response"
        )
        self.debug("Adding %s user to project: %s" % (
            self.new_admin.name,
            project.name
        ))
        # Add user to the project
        project.addAccount(
            self.apiclient,
            self.new_admin.name,
        )

        # listProjectAccount to verify the user is added to project or not
        accounts_reponse = Project.listAccounts(
            self.apiclient,
            projectid=project.id,
            account=self.new_admin.name,
        )
        self.debug(accounts_reponse)
        self.assertEqual(
            isinstance(accounts_reponse, list),
            True,
            "Check for a valid list accounts response"
        )

        self.assertNotEqual(
            len(list_projects_reponse),
            0,
            "Check list project response returns a valid project"
        )
        account = accounts_reponse[0]

        self.assertEqual(
            account.role,
            'Regular',
            "Newly added user is not added as a regular user"
        )
        self.debug("Updating project with new Admin: %s" %
                   self.new_admin.name)
        # Update the project with new admin
        project.update(
            self.apiclient,
            account=self.new_admin.name
        )

        # listProjectAccount to verify the user is new admin of the project
        accounts_reponse = Project.listAccounts(
            self.apiclient,
            projectid=project.id,
            account=self.new_admin.name,
        )
        self.assertEqual(
            isinstance(accounts_reponse, list),
            True,
            "Check for a valid list accounts response"
        )

        self.assertNotEqual(
            len(list_projects_reponse),
            0,
            "Check list project response returns a valid project"
        )
        account = accounts_reponse[0]

        self.assertEqual(
            account.role,
            'Admin',
            "Newly added user is not added as a regular user"
        )

        self.debug("Adding %s user to project: %s" % (
            self.user.name,
            project.name
        ))
        # Add user to the project
        project.addAccount(
            self.apiclient,
            self.user.name,
        )

        # listProjectAccount to verify the user is added to project or not
        accounts_reponse = Project.listAccounts(
            self.apiclient,
            projectid=project.id,
            account=self.user.name,
        )
        self.assertEqual(
            isinstance(accounts_reponse, list),
            True,
            "Check for a valid list accounts response"
        )

        self.assertNotEqual(
            len(list_projects_reponse),
            0,
            "Check list project response returns a valid project"
        )
        account = accounts_reponse[0]

        self.assertEqual(
            account.role,
            'Regular',
            "Newly added user is not added as a regular user"
        )

        self.debug("Updating project with new Admin: %s" %
                   self.user.name)

        # Update the project with new admin
        project.update(
            self.apiclient,
            account=self.user.name
        )

        # listProjectAccount to verify the user is new admin of the project
        accounts_reponse = Project.listAccounts(
            self.apiclient,
            projectid=project.id,
            account=self.user.name,
        )
        self.debug(accounts_reponse)
        self.assertEqual(
            isinstance(accounts_reponse, list),
            True,
            "Check for a valid list accounts response"
        )

        self.assertNotEqual(
            len(list_projects_reponse),
            0,
            "Check list project response returns a valid project"
        )
        account = accounts_reponse[0]

        self.assertEqual(
            account.role,
            'Admin',
            "Newly added user is not added as a regular user"
        )

       # listProjectAccount to verify old user becomes a regular user
        accounts_reponse = Project.listAccounts(
            self.apiclient,
            projectid=project.id,
            account=self.new_admin.name,
        )
        self.assertEqual(
            isinstance(accounts_reponse, list),
            True,
            "Check for a valid list accounts response"
        )

        self.assertNotEqual(
            len(list_projects_reponse),
            0,
            "Check list project response returns a valid project"
        )
        account = accounts_reponse[0]

        self.assertEqual(
            account.role,
            'Regular',
            "Newly added user is not added as a regular user"
        )
        return


class TestProjectResources(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestProjectResources, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.domain = get_domain(cls.api_client)
        cls.services['mode'] = cls.zone.networktype

        cls._cleanup = []
        cls.isGlobalSettingInvalid = False
        configs = Configurations.list(
            cls.api_client,
            name='project.invite.required'
        )

        if (configs[0].value).lower() != 'false':
            cls.isGlobalSettingInvalid = True
            return

        # Create account, disk offering etc.
        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.services["disk_offering"]
        )
        cls._cleanup.append(cls.disk_offering)

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)
        cls.user = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, reversed(cls._cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.isGlobalSettingInvalid:
            self.skipTest("'project.invite.required' should be set to false")
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, reversed(self.cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "sg",
            "eip",
            "advancedns",
            "simulator"],
        required_hardware="false")
    def test_07_project_resources_account_delete(self):
        """ Test Verify after an account is removed from the project, all its resources stay with the project.
        """
        # Validate the following
        # 1. Create a project.
        # 2. Add some accounts to project. Add resources to the project
        # 3. Delete the account. Verify resources are still there after
        #    account deletion.

        # Create project as a domain admin
        project = Project.create(
            self.apiclient,
            self.services["project"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        # Cleanup created project at end of test
        self.cleanup.append(project)
        self.debug("Created project with domain admin with ID: %s" %
                   project.id)

        list_projects_reponse = Project.list(
            self.apiclient,
            id=project.id,
            listall=True
        )

        self.assertEqual(
            isinstance(list_projects_reponse, list),
            True,
            "Check for a valid list projects response"
        )
        list_project = list_projects_reponse[0]

        self.assertNotEqual(
            len(list_projects_reponse),
            0,
            "Check list project response returns a valid project"
        )

        self.assertEqual(
            project.name,
            list_project.name,
            "Check project name from list response"
        )
        self.debug("Adding %s user to project: %s" % (
            self.user.name,
            project.name
        ))
        # Add user to the project
        project.addAccount(
            self.apiclient,
            self.user.name,
        )

        # listProjectAccount to verify the user is added to project or not
        accounts_response = Project.listAccounts(
            self.apiclient,
            projectid=project.id,
            account=self.user.name,
        )
        self.assertEqual(
            isinstance(accounts_response, list),
            True,
            "Check for a valid list accounts response"
        )

        self.assertNotEqual(
            len(list_projects_reponse),
            0,
            "Check list project response returns a valid project"
        )
        account = accounts_response[0]

        self.assertEqual(
            account.role,
            'Regular',
            "Newly added user is not added as a regular user"
        )
        # Create some resources(volumes) for the projects
        volume = Volume.create(
            self.apiclient,
            self.services["volume"],
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering.id,
            projectid=project.id
        )

        # Delete the project user ie the account
        self.user.delete(self.apiclient)

        volumes = Volume.list(self.apiclient, id=volume.id)

        self.assertEqual(
            isinstance(volumes, list),
            True,
            "Check for a valid list volumes response"
        )

        self.assertNotEqual(
            len(volumes),
            0,
            "Check list volumes API response returns a valid list"
        )
        volume_response = volumes[0]

        self.assertEqual(
            volume_response.name,
            volume.name,
            "Volume should exist after project user deletion."
        )
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "sg",
            "eip",
            "advancedns",
            "simulator"],
        required_hardware="false")
    def test_08_cleanup_after_project_delete(self):
        """ Test accounts are unassigned from project after project deletion
        """
        # Validate the following
        # 1. Create a project.
        # 2. Add some accounts to project. Add resources to the project
        # 3. Delete the project. Verify resources are freed after
        #    account deletion.
        # 4. Verify all accounts are unassigned from project.

        # Create project as a domain admin
        project = Project.create(
            self.apiclient,
            self.services["project"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        # Cleanup created project at end of test
        self.debug("Created project with domain admin with ID: %s" %
                   project.id)

        list_projects_reponse = Project.list(
            self.apiclient,
            id=project.id,
            listall=True
        )

        self.assertEqual(
            isinstance(list_projects_reponse, list),
            True,
            "Check for a valid list projects response"
        )
        list_project = list_projects_reponse[0]

        self.assertNotEqual(
            len(list_projects_reponse),
            0,
            "Check list project response returns a valid project"
        )

        self.assertEqual(
            project.name,
            list_project.name,
            "Check project name from list response"
        )
        self.user = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup.append(self.user)
        self.debug("Adding %s user to project: %s" % (
            self.user.name,
            project.name
        ))
        # Add user to the project
        project.addAccount(
            self.apiclient,
            self.user.name
        )

        # listProjectAccount to verify the user is added to project or not
        accounts_reponse = Project.listAccounts(
            self.apiclient,
            projectid=project.id,
            account=self.user.name,
        )
        self.assertEqual(
            isinstance(accounts_reponse, list),
            True,
            "Check for a valid list accounts response"
        )

        self.assertNotEqual(
            len(list_projects_reponse),
            0,
            "Check list project response returns a valid project"
        )
        account = accounts_reponse[0]

        self.assertEqual(
            account.role,
            'Regular',
            "Newly added user is not added as a regular user"
        )
        # Create some resources(volumes) for the projects
        volume = Volume.create(
            self.apiclient,
            self.services["volume"],
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering.id,
            projectid=project.id
        )
        self.debug("Created a volume: %s for project: %s" % (
            volume.id,
            project.name
        ))
        # Delete the project user
        self.debug("Deleting project: %s" % project.name)
        project.delete(self.apiclient)
        self.debug("Successfully deleted project: %s" % project.name)

        volumes = Volume.list(self.apiclient, id=volume.id)

        self.assertEqual(
            volumes,
            None,
            "Resources (volume) should be deleted as part of cleanup"
        )

        # Accounts should be un-assigned from project,
        # so this call will raise an exception: Unable to find the project id=
        with self.assertRaises(Exception):
            Project.listAccounts(self.apiclient, projectid=project.id)
        return


class TestProjectSuspendActivate(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestProjectSuspendActivate,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.domain = get_domain(cls.api_client)
        cls.services['mode'] = cls.zone.networktype
        cls.template = get_test_template(
            cls.api_client,
            cls.zone.id,
            cls.hypervisor
        )
        cls._cleanup = []
        cls.isGlobalSettingInvalid = False
        configs = Configurations.list(
            cls.api_client,
            name='project.invite.required'
        )

        if (configs[0].value).lower() != 'false':
            cls.isGlobalSettingInvalid = True
            return

        # Create account, service offering, disk offering etc.
        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.services["disk_offering"]
        )
        cls._cleanup.append(cls.disk_offering)
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.service_offering)
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)
        cls.user = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.user)

        # Create project as a domain admin
        cls.project = Project.create(
            cls.api_client,
            cls.services["project"],
            account=cls.account.name,
            domainid=cls.account.domainid
        )
        cls._cleanup.append(cls.project)
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, reversed(cls._cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.isGlobalSettingInvalid:
            self.skipTest("'project.invite.required' should be set to false")
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, reversed(self.cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "sg",
            "eip",
            "advancedns",
            "simulator"],
        required_hardware="false")
    def test_09_project_suspend(self):
        """ Test Verify after an account is removed from the project, all his
            resources stay with the project.
        """
        # Validate the following
        # 1. Create a project.
        # 2. Add some accounts to project. Add resources to the project
        # 3. Delete the account. Verify resources are still there after
        #    account deletion.

        self.debug("Adding %s user to project: %s" % (
            self.user.name,
            self.project.name
        ))
        # Add user to the project
        self.project.addAccount(
            self.apiclient,
            self.user.name,
        )

        # listProjectAccount to verify the user is added to project or not
        accounts_reponse = Project.listAccounts(
            self.apiclient,
            projectid=self.project.id,
            account=self.user.name,
        )
        self.assertEqual(
            isinstance(accounts_reponse, list),
            True,
            "Check for a valid list accounts response"
        )

        self.assertNotEqual(
            len(accounts_reponse),
            0,
            "Check list project response returns a valid project"
        )
        account = accounts_reponse[0]

        self.assertEqual(
            account.role,
            'Regular',
            "Newly added user is not added as a regular user"
        )

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.service_offering.id,
            projectid=self.project.id
        )
        self.debug("Created a VM: %s for project: %s" % (
            virtual_machine.id,
            self.project.id
        ))
        self.debug("Suspending a project: %s" % self.project.name)
        self.project.suspend(self.apiclient)

        # Check status of all VMs associated with project
        vms = VirtualMachine.list(
            self.apiclient,
            projectid=self.project.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "Check for a valid list accounts response"
        )

        self.assertNotEqual(
            len(vms),
            0,
            "Check list project response returns a valid project"
        )

        for vm in vms:
            self.debug("VM ID: %s state: %s" % (vm.id, vm.state))
            self.assertEqual(
                vm.state,
                'Stopped',
                "VM should be in stopped state after project suspension"
            )

        self.debug("Attempting to create volume in suspended project")
        with self.assertRaises(Exception):
            # Create some resources(volumes) for the projects
            Volume.create(
                self.apiclient,
                self.services["volume"],
                zoneid=self.zone.id,
                diskofferingid=self.disk_offering.id,
                projectid=self.project.id
            )

        self.debug("Volume creation failed")

        # Start the stopped VM
        self.debug("Attempting to start VM: %s in suspended project" %
                   virtual_machine.id)
        with self.assertRaises(Exception):
            virtual_machine.start(self.apiclient)
        self.debug("VM start failed!")

        # Destroy Stopped VM
        virtual_machine.delete(self.apiclient, expunge=False)
        self.debug("Destroying VM: %s" % virtual_machine.id)

        # Check status of all VMs associated with project
        vms = VirtualMachine.list(
            self.apiclient,
            projectid=self.project.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "Check for a valid list accounts response"
        )

        self.assertNotEqual(
            len(vms),
            0,
            "Check list project response returns a valid project"
        )

        for vm in vms:
            self.debug("VM ID: %s state: %s" % (vm.id, vm.state))
            self.assertEqual(
                vm.state,
                'Destroyed',
                "VM should be in stopped state after project suspension"
            )
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "sg",
            "eip",
            "advancedns",
            "simulator"],
        required_hardware="false")
    def test_10_project_activation(self):
        """ Test project activation after suspension
        """
        # Validate the following
        # 1. Activate the project
        # 2. Verify project is activated and we are able to add resources

        # Activating the project
        self.debug("Activating project: %s" % self.project.name)
        self.project.activate(self.apiclient)

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.service_offering.id,
            projectid=self.project.id
        )

        self.cleanup.append(virtual_machine)
        self.debug("Created a VM: %s for project: %s" % (
            virtual_machine.id,
            self.project.id
        ))
        # Check status of all VMs associated with project
        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "Check for a valid list accounts response"
        )

        self.assertNotEqual(
            len(vms),
            0,
            "Check list project response returns a valid project"
        )

        for vm in vms:
            self.debug("VM ID: %s state: %s" % (vm.id, vm.state))
            self.assertEqual(
                vm.state,
                'Running',
                "VM should be in Running state after project activation"
            )
        return
