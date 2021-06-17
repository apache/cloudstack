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

from marvin.cloudstackAPI import *
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackException import CloudstackAPIException
from marvin.lib.base import Account, User, Project, ProjectRole, ProjectRolePermission, PublicIPAddress
from marvin.lib.utils import cleanup_resources
from nose.plugins.attrib import attr
from random import shuffle

import copy
import random
import re


class TestData(object):
    """Test data object that is required to create resources
    """

    def __init__(self):
        self.testdata = {
            "account": {
                "email": "user@test.com",
                "firstname": "User",
                "lastname": "User",
                "username": "user",
                "password": "password",
            },
            "useracc": {
                "email": "user1@test.com",
                "firstname": "User",
                "lastname": "User",
                "username": "user1",
                "password": "fr3sca",
            },
            "project": {
                "name": "Test Project",
                "displaytext": "Test project",
            },
            "projectrole": {
                "name": "MarvinFake project Role ",
                "description": "Fake project Role created by Marvin test"
            },
            "projectrolepermission": {
                "rule": "listPublicIpAddresses",
                "permission": "deny",
                "description": "Fake role permission created by Marvin test"
            }
        }


class TestRoleBasedUsersInProjects(cloudstackTestCase):
    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.testdata = TestData().testdata

        feature_enabled = self.apiclient.listCapabilities(listCapabilities.listCapabilitiesCmd()).dynamicrolesenabled
        if not feature_enabled:
            self.skipTest("Dynamic Role-Based API checker not enabled, skipping test")

        self.testdata["projectrole"]["name"] += self.getRandomString()
        self.project = Project.create(self.apiclient, self.testdata["project"])
        self.projectrole = ProjectRole.create(
            self.apiclient,
            self.testdata["projectrole"],
            self.project.id
        )

        self.testdata["projectrolepermission"]["projectroleid"] = self.projectrole.id
        self.projectrolepermission = ProjectRolePermission.create(
            self.apiclient,
            self.testdata["projectrolepermission"],
            self.project.id
        )

        self.cleanup = [self.project]

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)

    def getUserApiClient(self, username, domain='ROOT', role_type='User'):
        self.user_apiclient = self.testClient.getUserApiClient(UserName=username, DomainName=domain,
                                                               type=self.translateRoleToAccountType(role_type))
        return self.user_apiclient

    def getRandomString(self):
        return "".join(random.choice("abcdefghijklmnopqrstuvwxyz0123456789") for _ in range(10))

    def translateRoleToAccountType(self, role_type):
        if role_type == "User":
            return 0
        elif role_type == "Admin":
            return 1
        elif role_type == "DomainAdmin":
            return 2
        elif role_type == "ResourceAdmin":
            return 3
        return -1

    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_add_account_to_project_with_project_role(self):
        """
            1. Create a User Account
            2. Add user account with 'Regular' project account role associate it with a Project role;
                The role defines what APIs are allowed/disallowed for the user: here, 'listPublicIpAddresses'
                is denied for the user account
            3. Execute the 'listPublicIpAddresses' API and verify/confirm that the API isn't allowed to be executed
                by the account
        """
        self.useraccount = Account.create(
            self.apiclient,
            self.testdata["account"],
            roleid=4
        )
        self.cleanup.append(self.useraccount)
        # Add account to the project
        self.project.addAccount(
            self.apiclient,
            account=self.useraccount.name,
            projectroleid=self.projectrole.id
        )

        self.userapiclient = self.testClient.getUserApiClient(
            UserName=self.useraccount.name,
            DomainName=self.useraccount.domain,
            type=0)
        try:
            PublicIPAddress.list(
                self.userapiclient,
                projectid=self.project.id
            )
            self.fail("API call succeeded which is denied for the project role")
        except CloudstackAPIException:
            pass

    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_add_user_to_project_with_project_role(self):
        """
            1. Create a User Account
            2. Add user of an account with 'Regular' project account role associate it with a Project role;
                The role defines what APIs are allowed/disallowed for the user: here, 'listPublicIpAddresses'
                is denied for the user account
            3. Execute the 'listPublicIpAddresses' API and verify/confirm that the API isn't allowed to be executed
                by the user
        """
        self.useraccount = Account.create(
            self.apiclient,
            self.testdata["account"],
            roleid=4
        )
        self.cleanup.append(self.useraccount)
        # Add account to the project
        self.project.addUser(
            self.apiclient,
            username=self.useraccount.user[0].username,
            projectroleid=self.projectrole.id
        )
        Project.listAccounts(self.apiclient, projectid=self.project.id)
        self.userapiclient = self.testClient.getUserApiClient(
            UserName=self.useraccount.name,
            DomainName=self.useraccount.domain,
            type=0)
        try:
            PublicIPAddress.list(
                self.userapiclient,
                projectid=self.project.id
            )
            self.fail("API call succeeded which is denied for the project role")
        except CloudstackAPIException:
            pass


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_add_multiple_admins_in_project(self):
        """
            1. Create a User Account
            2. Add user account with 'Admin' project account role  and associate it with a Project role;
                The role defines what APIs are allowed/disallowed for the user: here, 'listPublicIpAddresses'
                is denied for the user account
            3. Execute the 'listPublicIpAddresses' API and verify/confirm that the user/account can execute the
            API as it is a project admin
        """
        self.useraccount = Account.create(
            self.apiclient,
            self.testdata["account"],
            roleid=4
        )
        self.cleanup.append(self.useraccount)

        self.useraccount1 = Account.create(
            self.apiclient,
            self.testdata["useracc"],
            roleid=4
        )

        self.cleanup.append(self.useraccount1)

        self.project.addAccount(
            self.apiclient,
            account=self.useraccount.name,
            projectroleid=self.projectrole.id,
            roletype='Admin'
        )

        self.project.addAccount(
            self.apiclient,
            account=self.useraccount1.name,
            projectroleid=self.projectrole.id
        )

        project_accounts = Project.listAccounts(self.apiclient, projectid=self.project.id, role='Admin')
        self.assertEqual(len(project_accounts), 2, "account not added with admin Role")

        self.userapiclientAdminRole = self.testClient.getUserApiClient(
            UserName=self.useraccount.name,
            DomainName=self.useraccount.domain,
            type=0)

        self.userapiclientRegularRole = self.testClient.getUserApiClient(
            UserName=self.useraccount1.name,
            DomainName=self.useraccount1.domain,
            type=0)

        try:
            PublicIPAddress.list(
                self.userapiclientAdminRole,
                projectid=self.project.id
            )
            self.debug("User added to the project could execute the listPublicIpAddresses API despite the project "
                       "role as it is the Admin")
            pass
        except CloudstackAPIException:
            self.fail("User is an Admin, should be able to execute the command despite Project role")

        try:
            self.project.suspend(
                self.userapiclientAdminRole,
            )
            self.debug("The user can perform Project administrative operations as it is added as "
                       "an Admin to the project")
            pass
        except CloudstackAPIException:
            self.fail("User should be allowed to execute project administrative operations"
                      "as it is the Project Admin")

        try:
            self.project.suspend(
                self.userapiclientRegularRole,
            )
        except Exception as e:
            pass

