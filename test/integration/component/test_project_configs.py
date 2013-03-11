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
#Import Local Modules
import marvin
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from marvin.remoteSSHClient import remoteSSHClient
import datetime


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
                         "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 100,    # in MHz
                                    "memory": 64,       # In MBs
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
                         "template": {
                                "displaytext": "Public Template",
                                "name": "Public template",
                                "ostype": 'CentOS 5.3 (64-bit)',
                                "url": "http://download.cloud.com/releases/2.0.0/UbuntuServer-10-04-64bit.vhd.bz2",
                                "hypervisor": 'XenServer',
                                "format": 'VHD',
                                "isfeatured": True,
                                "ispublic": True,
                                "isextractable": True,
                        },
                        "configs": {
                                "project.invite.timeout": 300,
                        },
                        "ostype": 'CentOS 5.3 (64-bit)',
                        # Cent OS 5.3 (64 bit)
                        "sleep": 60,
                        "timeout": 10,
                        "mode": 'advanced'
                    }


class TestUserProjectCreation(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestUserProjectCreation,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone
        cls.zone = get_zone(cls.api_client, cls.services)

        configs = Configurations.list(
                                      cls.api_client,
                                      name='allow.user.create.projects'
                                      )

        if not isinstance(configs, list):
            raise unittest.SkipTest("List configurations has no config: allow.user.create.projects")
        elif (configs[0].value).lower() != 'true':
            raise unittest.SkipTest("'allow.user.create.projects' should be true")

        # Create domains, account etc.
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

        cls.user = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain.id
                            )

        cls._cleanup = [cls.account, cls.user, cls.domain]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(configuration = "allow.user.create.projects")
    @attr(tags = ["advanced", "basic", "sg", "eip", "advancedns", "simulator"])
    def test_admin_project_creation(self):
        """Test create project as a domain admin and domain user
        """
        # Validate the following
        # 1. Check if 'allow.user.create.projects' configuration is true
        # 2. Create a Project as domain admin
        # 3. Create a Project as domain user
        # 4. In both 2 and 3 project creation should be successful

        configs = Configurations.list(
                                      self.apiclient,
                                      name='allow.user.create.projects'
                                      )
        self.assertEqual(
                            isinstance(configs, list),
                            True,
                            "Check for a valid list configurations response"
                            )
        config = configs[0]
        self.assertEqual(
                            (config.value).lower(),
                            'true',
                            "'allow.user.create.projects' should be true"
                            )

        # Create project as a domain admin

        project = Project.create(
                                 self.apiclient,
                                 self.services["project"],
                                 account=self.account.account.name,
                                 domainid=self.account.account.domainid
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
        # Create project as a domain admin
        project = Project.create(
                                 self.apiclient,
                                 self.services["project"],
                                 account=self.user.account.name,
                                 domainid=self.user.account.domainid
                                 )
        # Cleanup created project at end of test
        self.cleanup.append(project)
        self.debug("Created project with domain user with ID: %s" %
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
        return


class TestProjectCreationNegative(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestProjectCreationNegative,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone
        cls.zone = get_zone(cls.api_client, cls.services)

        # Checking for prereqisits - global configs
        configs = Configurations.list(
                                      cls.api_client,
                                      name='allow.user.create.projects'
                                      )

        if not isinstance(configs, list):
            raise unittest.SkipTest("List configurations has no config: allow.user.create.projects")
        elif (configs[0].value).lower() != 'false':
            raise unittest.SkipTest("'allow.user.create.projects' should be false")

        # Create domains, account etc.
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

        cls.user = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain.id
                            )

        cls._cleanup = [cls.account, cls.user, cls.domain]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(configuration = "allow.user.create.projects")
    @attr(tags = ["advanced", "basic", "sg", "eip", "advancedns", "simulator"])
    @unittest.skip("Known bug-able to create project as a domain user")
    def test_user_project_creation(self):
        """Test create project as a domain admin and domain user
        """
        # Validate the following
        # 1. Check if 'allow.user.create.projects' configuration is false
        # 2. Create a Project as domain admin. Project creation should be
        #    successful.
        # 3. Create a Project as domain user. Project creation should fail

        configs = Configurations.list(
                                      self.apiclient,
                                      name='allow.user.create.projects'
                                      )
        self.assertEqual(
                            isinstance(configs, list),
                            True,
                            "Check for a valid list configurations response"
                            )
        config = configs[0]
        self.assertEqual(
                            (config.value).lower(),
                            'false',
                            "'allow.user.create.projects' should be true"
                            )

        # Create project as a domain admin
        project = Project.create(
                                 self.apiclient,
                                 self.services["project"],
                                 account=self.account.account.name,
                                 domainid=self.account.account.domainid
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
        with self.assertRaises(Exception):
            project = Project.create(
                                 self.apiclient,
                                 self.services["project"],
                                 account=self.user.account.name,
                                 domainid=self.user.account.domainid
                                 )
            self.debug("Project creation with domain user: %s failed" %
                                                    self.user.account.name)
        return


class TestProjectInviteRequired(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                                TestProjectInviteRequired,
                                cls
                            ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone
        cls.zone = get_zone(cls.api_client, cls.services)

        # Create domains, account etc.
        cls.domain = get_domain(cls.api_client, cls.services)

        # Verify 'project.invite.required' is set to false
        configs = Configurations.list(
                                      cls.api_client,
                                      name='project.invite.required'
                                      )

        if not isinstance(configs, list):
            raise unittest.SkipTest("The 'project.invite.required' is not found in global configs")
        elif (configs[0].value).lower() != 'false':
            raise unittest.SkipTest("'project.invite.required' should be false")

        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain.id
                            )

        cls.user = Account.create(
                            cls.api_client,
                            cls.services["user"],
                            admin=True,
                            domainid=cls.domain.id
                            )

        cls._cleanup = [cls.account, cls.user]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advanced", "basic", "sg", "eip", "advancedns", "simulator"])
    def test_add_user_to_project(self):
        """Add user to project when 'project.invite.required' is false"""


        # Validate the following:
        # 1. Create a Project
        # 2. Add users to the project. Verify user is added to project
        #    as regular user

        # Create project as a domain admin
        project = Project.create(
                                 self.apiclient,
                                 self.services["project"],
                                 account=self.account.account.name,
                                 domainid=self.account.account.domainid
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
                                                      self.user.account.name,
                                                      project.name
                                                      ))
         # Add user to the project
        project.addAccount(
                           self.apiclient,
                           self.user.account.name,
                           self.user.account.email
                           )

        # listProjectAccount to verify the user is added to project or not
        accounts_reponse = Project.listAccounts(
                                            self.apiclient,
                                            projectid=project.id,
                                            account=self.user.account.name,
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

class TestProjectInviteRequiredTrue(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                                TestProjectInviteRequiredTrue,
                                cls
                            ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone
        cls.zone = get_zone(cls.api_client, cls.services)

        # Create domains, account etc.
        cls.domain = get_domain(cls.api_client, cls.services)

        # Verify 'project.invite.required' is set to true
        configs = Configurations.list(
                                      cls.api_client,
                                      name='project.invite.required'
                                      )

        if not isinstance(configs, list):
            raise unittest.SkipTest("The 'project.invite.required' is not found in global configs")
        elif (configs[0].value).lower() != 'true':
            raise unittest.SkipTest("'project.invite.required' should be true")

        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain.id
                            )

        cls.user = Account.create(
                            cls.api_client,
                            cls.services["user"],
                            admin=True,
                            domainid=cls.domain.id
                            )

        cls._cleanup = [cls.account, cls.user]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(configuration = "project.invite.required")
    @attr(tags = ["advanced", "basic", "sg", "eip", "advancedns"])
    def test_add_user_to_project(self):
        """Add user to project when 'project.invite.required' is true"""


        # Validate the following:
        # 1. Create a Project
        # 2. Add users to the project. verify user is shown in pending state

        # Create project as a domain admin
        project = Project.create(
                                 self.apiclient,
                                 self.services["project"],
                                 account=self.account.account.name,
                                 domainid=self.account.account.domainid
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
                                                      self.user.account.name,
                                                      project.name
                                                      ))
         # Add user to the project
        project.addAccount(
                           self.apiclient,
                           self.user.account.name,
                           self.user.account.email
                           )

        # listProjectAccount to verify the user is added to project or not
        accounts_reponse = ProjectInvitation.list(
                                        self.apiclient,
                                        state='Pending',
                                        account=self.user.account.name,
                                        domainid=self.user.account.domainid
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
                    account.state,
                    'Pending',
                    "Newly added user is not added as a regular user"
                    )
        return


class TestProjectInviteTimeout(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                                TestProjectInviteTimeout,
                                cls
                            ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone
        cls.zone = get_zone(cls.api_client, cls.services)

        # Create domains, account etc.
        cls.domain = get_domain(cls.api_client, cls.services)

        # Verify 'project.invite.required' is set to true
        configs = Configurations.list(
                                      cls.api_client,
                                      name='project.invite.required'
                                      )

        if not isinstance(configs, list):
            raise unittest.SkipTest("The 'project.invite.required' is not found in global configs")
        elif (configs[0].value).lower() != 'true':
            raise unittest.SkipTest("'project.invite.required' should be true")

        # Verify 'project.invite.timeout' is set to 300
        configs = Configurations.list(
                                      cls.api_client,
                                      name='project.invite.timeout'
                                      )

        if not isinstance(configs, list):
            raise unittest.SkipTest("The 'project.invite.timeout' is not found in global configs")
        elif int(configs[0].value) != cls.services["configs"]["project.invite.timeout"]:
            raise unittest.SkipTest("'project.invite.timeout' should be: %s " %
                            cls.services["configs"]["project.invite.timeout"])

        cls.config = configs[0]
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain.id
                            )

        cls.user = Account.create(
                            cls.api_client,
                            cls.services["user"],
                            admin=True,
                            domainid=cls.domain.id
                            )

        cls._cleanup = [cls.account, cls.user]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(configuration = "project.invite.timeout")
    @attr(tags = ["advanced", "basic", "sg", "eip", "advancedns", "simulator"])
    def test_01_invitation_timeout(self):
        """Test global config project invitation timeout"""


        # Validate the following:
        # 1. Set configuration to 5 mins
        # 2. Create a Project
        # 3. Add users to the project
        # 4. As a user accept invitation within 5 mins. Verify invitation is
        #    accepted and user become regular user of project

        # Create project as a domain admin
        project = Project.create(
                                 self.apiclient,
                                 self.services["project"],
                                 account=self.account.account.name,
                                 domainid=self.account.account.domainid
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
                                                      self.user.account.name,
                                                      project.name
                                                      ))
         # Add user to the project
        project.addAccount(
                           self.apiclient,
                           self.user.account.name,
                           self.user.account.email
                           )

        # listProjectAccount to verify the user is added to project or not
        accounts_reponse = ProjectInvitation.list(
                                        self.apiclient,
                                        state='Pending',
                                        account=self.user.account.name,
                                        domainid=self.user.account.domainid
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
                    account.state,
                    'Pending',
                    "Newly added user is not added as a regular user"
                    )

        # Accept the invite
        ProjectInvitation.update(
                                 self.apiclient,
                                 projectid=project.id,
                                 accept=True,
                                 account=self.user.account.name
                                 )
        self.debug(
            "Accepting project invitation for project: %s user: %s" % (
                                                      project.name,
                                                      self.user.account.name
                                                      ))
        # listProjectAccount to verify the user is added to project or not
        accounts_reponse = Project.listAccounts(
                                            self.apiclient,
                                            projectid=project.id,
                                            account=self.user.account.name,
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

    @attr(configuration = "project.invite.timeout")
    @attr(tags = ["advanced", "basic", "sg", "eip", "advancedns", "simulator"])
    def test_02_invitation_timeout_after_expiry(self):
        """Test global config project invitation timeout"""


        # Validate the following:
        # 1. Set configuration to 5 mins
        # 2. Create a Project
        # 3. Add users to the project
        # 4. As a user accept invitation after 5 mins. Verify invitation is
        #    not accepted and is shown as expired

        # Create project as a domain admin
        project = Project.create(
                                 self.apiclient,
                                 self.services["project"],
                                 account=self.account.account.name,
                                 domainid=self.account.account.domainid
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
                                                      self.user.account.name,
                                                      project.name
                                                      ))
         # Add user to the project
        project.addAccount(
                           self.apiclient,
                           self.user.account.name,
                           self.user.account.email
                           )

        # listProjectAccount to verify the user is added to project or not
        accounts_reponse = ProjectInvitation.list(
                                        self.apiclient,
                                        state='Pending',
                                        account=self.user.account.name,
                                        domainid=self.user.account.domainid
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
                    account.state,
                    'Pending',
                    "Newly added user is not added as a regular user"
                    )

        # sleep for 'project.invite.timeout' * 2 interval to wait for invite
        # to expire
        time.sleep(int(self.config.value) * 2)

        with self.assertRaises(Exception):
            # Accept the invite
            ProjectInvitation.update(
                                 self.apiclient,
                                 projectid=project.id,
                                 accept=True,
                                 account=self.user.account.name
                                 )
            self.debug(
                "Accepting invitation after expiry project: %s user: %s" % (
                                                      project.name,
                                                      self.user.account.name
                                                      ))
        # listProjectAccount to verify the user is added to project or not
        accounts_reponse = ProjectInvitation.list(
                                        self.apiclient,
                                        account=self.user.account.name,
                                        domainid=self.user.account.domainid
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
                    account.state,
                    'Expired',
                    "Newly added user is not added as a regular user"
                    )
        return

    @attr(configuration = "project.invite.timeout")
    @attr(tags = ["advanced", "basic", "sg", "eip", "advancedns", "simulator"])
    def test_03_invite_after_expiry(self):
        """Test global config project invitation timeout"""

        # Validate the following:
        # 1. Set configuration to 5 mins
        # 2. Create a Project
        # 3. Add users to the project
        # 4. As a user accept invitation after 5 mins.
        # 5. Resend the invitation
        # 6. Verify invitation is sent again

        # Create project as a domain admin
        project = Project.create(
                                 self.apiclient,
                                 self.services["project"],
                                 account=self.account.account.name,
                                 domainid=self.account.account.domainid
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
                                                      self.user.account.name,
                                                      project.name
                                                      ))
        # Add user to the project
        project.addAccount(
                           self.apiclient,
                           self.user.account.name,
                           self.user.account.email
                           )

        # listProjectAccount to verify the user is added to project or not
        accounts_reponse = ProjectInvitation.list(
                                        self.apiclient,
                                        state='Pending',
                                        account=self.user.account.name,
                                        domainid=self.user.account.domainid
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
                    account.state,
                    'Pending',
                    "Newly added user is not added as a regular user"
                    )

        # sleep for 'project.invite.timeout' * 2 interval to wait for invite
        # to expire
        time.sleep(int(self.config.value) * 2)

        self.debug("Adding %s user again to project: %s" % (
                                                      self.user.account.name,
                                                      project.name
                                                      ))
        # Add user to the project
        project.addAccount(
                           self.apiclient,
                           self.user.account.name,
                           self.user.account.email
                           )

        # listProjectAccount to verify the user is added to project or not
        accounts_reponse = ProjectInvitation.list(
                                        self.apiclient,
                                        state='Pending',
                                        account=self.user.account.name,
                                        domainid=self.user.account.domainid
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
                    account.state,
                    'Pending',
                    "Newly added user is not added as a regular user"
                    )
        return

    @attr(configuration = "project.invite.timeout")
    @attr(tags = ["advanced", "basic", "sg", "eip", "advancedns", "simulator"])
    def test_04_decline_invitation(self):
        """Test decline invitation"""

        # Validate the following:
        # 1. Set configuration to 5 mins
        # 2. Create a Project
        # 3. Add users to the project
        # 4. As a user decline invitation within 5 mins.
        # 5. Verify invitation is rejected and user doesn't become regular
        #    user.

        # Create project as a domain admin
        project = Project.create(
                                 self.apiclient,
                                 self.services["project"],
                                 account=self.account.account.name,
                                 domainid=self.account.account.domainid
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
                                                      self.user.account.name,
                                                      project.name
                                                      ))
        # Add user to the project
        project.addAccount(
                           self.apiclient,
                           self.user.account.name,
                           self.user.account.email
                           )

        # listProjectAccount to verify the user is added to project or not
        accounts_reponse = ProjectInvitation.list(
                                        self.apiclient,
                                        state='Pending',
                                        account=self.user.account.name,
                                        domainid=self.user.account.domainid
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
                    account.state,
                    'Pending',
                    "Newly added user is not added as a regular user"
                    )
        # Accept the invite
        ProjectInvitation.update(
                                 self.apiclient,
                                 projectid=project.id,
                                 accept=False,
                                 account=self.user.account.name
                                 )
        self.debug(
                "Declining invitation for project: %s user: %s" % (
                                                      project.name,
                                                      self.user.account.name
                                                      ))
        # listProjectAccount to verify the user is added to project or not
        accounts_reponse = Project.listAccounts(
                                            self.apiclient,
                                            projectid=project.id,
                                            account=self.user.account.name,
                                            )
        self.assertEqual(
                            accounts_reponse,
                            None,
                            "Check for a valid list accounts response"
                            )
        return

    @unittest.skip("Requires SMPT configs")
    def test_09_invite_to_project_by_email(self):
        """Test invite user to project by email"""
        
        # Validate the following:
        # 1. Set configuration to 5 mins
        # 2. Create a Project
        # 3. Add users to the project
        # 4. As a user decline invitation within 5 mins.
        # 5. Verify invitation is rejected and user doesn't become regular
        #    user.

        # Verify 'project.invite.required' is set to false
        configs = Configurations.list(
                                      self.apiclient,
                                      name='project.invite.timeout'
                                      )
        self.assertEqual(
                            isinstance(configs, list),
                            True,
                            "Check for a valid list configurations response"
                            )
        config = configs[0]
        self.assertEqual(
                    int(config.value),
                    self.services["configs"]["project.invite.timeout"],
                    "'project.invite.timeout' should be %s" %
                            self.services["configs"]["project.invite.timeout"]
                    )

        # Create project as a domain admin
        project = Project.create(
                                 self.apiclient,
                                 self.services["project"],
                                 account=self.account.account.name,
                                 domainid=self.account.account.domainid
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
        self.debug("Adding user with email: %s to project: %s" % (
                                                      self.user.account.email,
                                                      project.name
                                                      ))

        # Add user to the project
        project.addAccount(
                           self.apiclient,
                           email=self.user.account.user[0].email
                           )

        # Fetch the latest mail sent to user
        mail_content = fetch_latest_mail(
                                         self.services["mail_account"],
                                         from_mail=self.user.account.user[0].email
                                         )
        return
