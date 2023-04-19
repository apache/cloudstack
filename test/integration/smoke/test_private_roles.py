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
from marvin.lib.base import Account, Role, User
from marvin.lib.utils import cleanup_resources
from nose.plugins.attrib import attr

import random


class TestData(object):
    """Test data object that is required to create resources
    """
    def __init__(self):
        self.testdata = {
            "accountadmin": {
                "email": "mtu@test.cloud",
                "firstname": "Marvin",
                "lastname": "TestAdminAccount",
                "username": "TestAdminAccount",
                "password": "password"
            },
            "accountdomainadmin": {
                "email": "mtu@test.cloud",
                "firstname": "Marvin",
                "lastname": "TestDomainAdminAccount",
                "username": "TestDomainAdminAccount",
                "password": "password"
            },
            "accountroleuser": {
                "email": "mtu@test.cloud",
                "firstname": "Marvin",
                "lastname": "TestUserAccount",
                "username": "TestUserAccount",
                "password": "password"
            },
            "roleadmin": {
                "name": "MarvinFake Admin Role ",
                "type": "Admin",
                "description": "Fake Admin Role created by Marvin test"
            },
            "roleuser": {
                "name": "MarvinFake User Role ",
                "type": "User",
                "description": "Fake User Role created by Marvin test",
                "ispublic": False
            },
            "publicrole": {
                "name": "MarvinFake Public Role ",
                "type": "User",
                "description": "Fake Public Role created by Marvin test"
            },
            "importrole": {
                "name": "MarvinFake Import Role ",
                "type": "User",
                "description": "Fake Import User Role created by Marvin test",
                "ispublic": True,
                "rules": [{"rule":"list*", "permission":"allow","description":"Listing apis"},
                           {"rule":"get*", "permission":"allow","description":"Get apis"},
                           {"rule":"update*", "permission":"deny","description":"Update apis"}]
            },
            "roledomainadmin": {
                "name": "MarvinFake DomainAdmin Role ",
                "type": "DomainAdmin",
                "description": "Fake Domain-Admin Role created by Marvin test",
                "ispublic": False
            },
            "apiConfig": {
                "listApis": "allow",
                "listAccounts": "allow",
                "listClusters": "deny",
                "*VM*": "allow",
                "*Host*": "deny"
            }
        }


class TestPrivateRoles(cloudstackTestCase):
    """Tests Visibility of private and public roles
    """

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.testdata = TestData().testdata

        self.testdata["roleadmin"]["name"] += self.getRandomString()
        self.testdata["roledomainadmin"]["name"] += self.getRandomString()
        self.testdata["roleuser"]["name"] += self.getRandomString()
        self.cleanup = []

        self.role_admin = Role.create(
            self.apiclient,
            self.testdata["roleadmin"]
        )
        self.cleanup.append(self.role_admin)

        self.role_domain_admin = Role.create(
            self.apiclient,
            self.testdata["roledomainadmin"]
        )
        self.cleanup.append(self.role_domain_admin)

        self.private_role = Role.create(
            self.apiclient,
            self.testdata["roleuser"]
        )
        self.cleanup.append(self.private_role)

        self.account_admin = Account.create(
            self.apiclient,
            self.testdata["accountadmin"],
            roleid=self.role_admin.id
        )
        self.cleanup.append(self.account_admin)

        self.account_domain_admin = Account.create(
            self.apiclient,
            self.testdata["accountdomainadmin"],
            roleid=self.role_domain_admin.id
        )
        self.cleanup.append(self.account_domain_admin)

        self.admin_apiclient = self.testClient.getUserApiClient(
            UserName=self.account_admin.name,
            DomainName='ROOT',
            type=1
        )

        self.domain_admin_apiclient = self.testClient.getUserApiClient(
            UserName=self.account_domain_admin.name,
            DomainName='ROOT',
            type=2
        )

    def tearDown(self):
        super(TestPrivateRoles, self).tearDown()

    def getRandomString(self):
        return "".join(random.choice("abcdefghijklmnopqrstuvwxyz0123456789") for _ in range(10))

    def asserts_visibility_of_private_role(self, role_id):
        list_roles_domain_admin = Role.list(self.domain_admin_apiclient, id=role_id)
        self.assertEqual(
            list_roles_domain_admin,
            None,
            "Domain Admins should not be able to list private roles"
        )

        list_roles_admin = Role.list(self.admin_apiclient, id=role_id)
        self.assertNotEqual(
            list_roles_admin,
            None,
            "Admins should be able to list private roles"
        )

    def asserts_visibility_of_public_role(self, role_id):
        list_roles_domain_admin = Role.list(self.domain_admin_apiclient, id=role_id)
        self.assertNotEqual(
            list_roles_domain_admin,
            None,
            "Domain Admins should be able to list public roles"
        )

        list_roles_admin = Role.list(self.admin_apiclient, id=role_id)
        self.assertNotEqual(
            list_roles_admin,
            None,
            "Admins should be able to list public roles"
        )

    @attr(tags=['simulator', 'basic'], required_hardware=False)
    def test_create_role(self):
        """
            1. Create a private role
            2. Create a public role
            3. Verify whether their visibility is as expected
        """
        self.testdata["roleuser"]["name"] += self.getRandomString()
        self.testdata["publicrole"]["name"] += self.getRandomString()
        private_role = Role.create(
            self.apiclient,
            self.testdata["roleuser"]
        )
        self.cleanup.append(self.private_role)
        public_role = Role.create(
            self.apiclient,
            self.testdata["publicrole"]
        )
        self.cleanup.append(self.public_role)
        self.asserts_visibility_of_private_role(private_role.id)
        self.asserts_visibility_of_public_role(public_role.id)

    @attr(tags=['simulator', 'basic'], required_hardware=False)
    def test_update_role(self):
        """
            1. Create a public role
            2. Check if its visibility is public
            3. Update it to make it private
            4. Verify if its visibility is private
        """
        self.testdata["publicrole"]["name"] += self.getRandomString()
        role = Role.create(
            self.apiclient,
            self.testdata["publicrole"]
        )
        self.cleanup.append(role)
        self.asserts_visibility_of_public_role(role.id)
        role.update(self.apiclient, id=role.id, ispublic=False)
        self.asserts_visibility_of_private_role(role.id)

    @attr(tags=['simulator', 'basic'], required_hardware=False)
    def test_import_role(self):
        """
            1. Import a public role
            2. Import a private role
            3. Verify their visibility
        """
        self.testdata["importrole"]["name"] += self.getRandomString()
        imported_public_role = Role.importRole(
            self.apiclient,
            self.testdata["importrole"]
        )
        self.cleanup.append(imported_public_role)
        self.testdata["importrole"]["name"] += self.getRandomString()
        self.testdata["importrole"]["ispublic"] = False
        imported_private_role = Role.importRole(
            self.apiclient,
            self.testdata["importrole"]
        )
        self.cleanup.append(imported_private_role)

        self.asserts_visibility_of_public_role(imported_public_role.id)
        self.asserts_visibility_of_private_role(imported_private_role.id)

    @attr(tags=['simulator', 'basic'], required_hardware=False)
    def test_login_private_role(self):
        """
            1. Crate a User account with a private role
            2. Login with the created account
            3. Verify that the login was successful
        """
        account_private_role = Account.create(
            self.apiclient,
            self.testdata["accountroleuser"],
            roleid=self.private_role.id
        )
        self.cleanup.append(account_private_role)

        response = User.login(
            self.apiclient,
            username=account_private_role.name,
            password=self.testdata["accountroleuser"]["password"]
        )

        self.assertNotEqual(
            response.sessionkey,
            None,
            "Accounts using private roles should be able to login."
        )
