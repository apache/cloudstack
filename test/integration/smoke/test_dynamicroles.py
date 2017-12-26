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
from marvin.lib.base import Account, Role, RolePermission
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
                "email": "mtu@test.cloud",
                "firstname": "Marvin",
                "lastname": "TestUser",
                "username": "roletest",
                "password": "password",
            },
            "role": {
                "name": "MarvinFake Role ",
                "type": "User",
                "description": "Fake Role created by Marvin test"
            },
            "roleadmin": {
                "name": "MarvinFake Admin Role ",
                "type": "Admin",
                "description": "Fake Admin Role created by Marvin test"
            },
            "roledomainadmin": {
                "name": "MarvinFake DomainAdmin Role ",
                "type": "DomainAdmin",
                "description": "Fake Domain-Admin Role created by Marvin test"
            },
            "rolepermission": {
                "roleid": 1,
                "rule": "listVirtualMachines",
                "permission": "allow",
                "description": "Fake role permission created by Marvin test"
            },
            "apiConfig": {
                "listApis": "allow",
                "listAccounts": "allow",
                "listClusters": "deny",
                "*VM*": "allow",
                "*Host*": "deny"
            }
        }


class TestDynamicRoles(cloudstackTestCase):
    """Tests dynamic role and role permission management in CloudStack
    """

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.testdata = TestData().testdata

        feature_enabled = self.apiclient.listCapabilities(listCapabilities.listCapabilitiesCmd()).dynamicrolesenabled
        if not feature_enabled:
            self.skipTest("Dynamic Role-Based API checker not enabled, skipping test")

        self.testdata["role"]["name"] += self.getRandomString()
        self.role = Role.create(
            self.apiclient,
            self.testdata["role"]
        )

        self.testdata["rolepermission"]["roleid"] = self.role.id
        self.rolepermission = RolePermission.create(
            self.apiclient,
            self.testdata["rolepermission"]
        )

        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            roleid=self.role.id
        )
        self.cleanup = [
            self.account,
            self.rolepermission,
            self.role
        ]


    def tearDown(self):
        try:
           cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)


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


    def getUserApiClient(self, username, domain='ROOT', role_type='User'):
        self.user_apiclient = self.testClient.getUserApiClient(UserName=username, DomainName='ROOT', type=self.translateRoleToAccountType(role_type))
        return self.user_apiclient


    def getRandomString(self):
        return "".join(random.choice("abcdefghijklmnopqrstuvwxyz0123456789") for _ in range(10))


    def getRandomRoleName(self):
        return "MarvinFakeRoleNewName-" + self.getRandomString()


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_role_lifecycle_list(self):
        """
            Tests that default four roles exist
        """
        roleTypes = {1: "Admin", 2: "ResourceAdmin", 3: "DomainAdmin", 4: "User"}
        for idx in range(1,5):
            list_roles = Role.list(self.apiclient, id=idx)
            self.assertEqual(
                isinstance(list_roles, list),
                True,
                "List Roles response was not a valid list"
            )
            self.assertEqual(
                len(list_roles),
                1,
                "List Roles response size was not 1"
            )
            self.assertEqual(
                list_roles[0].type,
                roleTypes[idx],
                msg="Default role type differs from expectation"
            )


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_role_lifecycle_create(self):
        """
            Tests normal lifecycle operations for roles
        """
        # Reuse self.role created in setUp()
        try:
            role = Role.create(
                self.apiclient,
                self.testdata["role"]
            )
            self.fail("An exception was expected when creating duplicate roles")
        except CloudstackAPIException: pass

        list_roles = Role.list(self.apiclient, id=self.role.id)
        self.assertEqual(
            isinstance(list_roles, list),
            True,
            "List Roles response was not a valid list"
        )
        self.assertEqual(
            len(list_roles),
            1,
            "List Roles response size was not 1"
        )
        self.assertEqual(
            list_roles[0].name,
            self.testdata["role"]["name"],
            msg="Role name does not match the test data"
        )
        self.assertEqual(
            list_roles[0].type,
            self.testdata["role"]["type"],
            msg="Role type does not match the test data"
        )


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_role_lifecycle_update(self):
        """
            Tests role update
        """
        self.account.delete(self.apiclient)
        new_role_name = self.getRandomRoleName()
        self.role.update(self.apiclient, name=new_role_name, type='Admin')
        update_role = Role.list(self.apiclient, id=self.role.id)[0]
        self.assertEqual(
            update_role.name,
            new_role_name,
            msg="Role name does not match updated role name"
        )
        self.assertEqual(
            update_role.type,
            'Admin',
            msg="Role type does not match updated role type"
        )


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_role_lifecycle_update_role_inuse(self):
        """
            Tests role update when role is in use by an account
        """
        new_role_name = self.getRandomRoleName()
        try:
            self.role.update(self.apiclient, name=new_role_name, type='Admin')
            self.fail("Updation of role type is not allowed when role is in use")
        except CloudstackAPIException: pass

        self.role.update(self.apiclient, name=new_role_name)
        update_role = Role.list(self.apiclient, id=self.role.id)[0]
        self.assertEqual(
            update_role.name,
            new_role_name,
            msg="Role name does not match updated role name"
        )


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_role_lifecycle_delete(self):
        """
            Tests role update
        """
        self.account.delete(self.apiclient)
        self.role.delete(self.apiclient)
        list_roles = Role.list(self.apiclient, id=self.role.id)
        self.assertEqual(
            list_roles,
            None,
            "List Roles response should be empty"
        )


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_role_inuse_deletion(self):
        """
            Test to ensure role in use cannot be deleted
        """
        try:
            self.role.delete(self.apiclient)
            self.fail("Role with any account should not be allowed to be deleted")
        except CloudstackAPIException: pass


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_default_role_deletion(self):
        """
            Test to ensure 4 default roles cannot be deleted
        """
        for idx in range(1,5):
            cmd = deleteRole.deleteRoleCmd()
            cmd.id = idx
            try:
                self.apiclient.deleteRole(cmd)
                self.fail("Default role got deleted with id: " + idx)
            except CloudstackAPIException: pass


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_rolepermission_lifecycle_list(self):
        """
            Tests listing of default role's permission
        """
        for idx in range(1,5):
            list_rolepermissions = RolePermission.list(self.apiclient, roleid=idx)
            self.assertEqual(
                isinstance(list_rolepermissions, list),
                True,
                "List rolepermissions response was not a valid list"
            )
            self.assertTrue(
                len(list_rolepermissions) > 0,
                "List rolepermissions response was empty"
            )


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_rolepermission_lifecycle_create(self):
        """
            Tests creation of role permission
        """
        # Reuse self.rolepermission created in setUp()
        try:
            rolepermission = RolePermission.create(
                self.apiclient,
                self.testdata["rolepermission"]
            )
            self.fail("An exception was expected when creating duplicate role permissions")
        except CloudstackAPIException: pass

        list_rolepermissions = RolePermission.list(self.apiclient, roleid=self.role.id)
        self.assertEqual(
            isinstance(list_rolepermissions, list),
            True,
            "List rolepermissions response was not a valid list"
        )
        self.assertNotEqual(
            len(list_rolepermissions),
            0,
            "List rolepermissions response was empty"
        )
        self.assertEqual(
            list_rolepermissions[0].rule,
            self.testdata["rolepermission"]["rule"],
            msg="Role permission rule does not match the test data"
        )
        self.assertEqual(
            list_rolepermissions[0].permission,
            self.testdata["rolepermission"]["permission"],
            msg="Role permission permission-type does not match the test data"
        )


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_rolepermission_lifecycle_update(self):
        """
            Tests order updation of role permission
        """
        permissions = [self.rolepermission]
        rules = ['list*', '*Vol*', 'listCapabilities']
        for rule in rules:
            data = copy.deepcopy(self.testdata["rolepermission"])
            data['rule'] = rule
            permission = RolePermission.create(
                self.apiclient,
                data
            )
            self.cleanup.append(permission)
            permissions.append(permission)


        def validate_permissions_list(permissions):
            list_rolepermissions = RolePermission.list(self.apiclient, roleid=self.role.id)
            self.assertEqual(
                len(list_rolepermissions),
                len(permissions),
                msg="List of role permissions do not match created list of permissions"
            )

            for idx, rolepermission in enumerate(list_rolepermissions):
                self.assertEqual(
                    rolepermission.rule,
                    permissions[idx].rule,
                    msg="Rule permission don't match with expected item at the index"
                )
                self.assertEqual(
                    rolepermission.permission,
                    permissions[idx].permission,
                    msg="Rule permission don't match with expected item at the index"
                )

        # Move last item to the top
        rule = permissions.pop(len(permissions)-1)
        permissions = [rule] + permissions
        rule.update(self.apiclient, ruleorder=",".join(map(lambda x: x.id, permissions)))
        validate_permissions_list(permissions)

        # Move to the bottom
        rule = permissions.pop(0)
        permissions = permissions + [rule]
        rule.update(self.apiclient, ruleorder=",".join(map(lambda x: x.id, permissions)))
        validate_permissions_list(permissions)

        # Random shuffles
        for _ in range(3):
            shuffle(permissions)
            rule.update(self.apiclient, ruleorder=",".join(map(lambda x: x.id, permissions)))
            validate_permissions_list(permissions)

    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_rolepermission_lifecycle_update_permission(self):
        """
            Tests update of Allow to Deny permission of a rule
        """
        permissions = [self.rolepermission]

        rule = permissions.pop(0)
        rule.update(self.apiclient, ruleid=rule.id, permission='deny')

        list_rolepermissions = RolePermission.list(self.apiclient, roleid=self.role.id)
        self.assertEqual(
            list_rolepermissions[0].permission,
            'deny',
            msg="List of role permissions do not match created list of permissions"
        )

        rule.update(self.apiclient, ruleid=rule.id, permission='allow')

        list_rolepermissions = RolePermission.list(self.apiclient, roleid=self.role.id)
        self.assertEqual(
            list_rolepermissions[0].permission,
            'allow',
            msg="List of role permissions do not match created list of permissions"
        )

    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_rolepermission_lifecycle_update_permission_negative(self):
        """
            Tests negative test for setting incorrect value as permission
        """
        permissions = [self.rolepermission]

        rule = permissions.pop(0)
        try:
            rule.update(self.apiclient, ruleid=rule.id, permission='some_other_value')
        except Exception:
            pass
        else:
            self.fail("Negative test: Setting permission to 'some_other_value' should not be successful, failing")

    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_rolepermission_lifecycle_concurrent_updates(self):
        """
            Tests concurrent order updation of role permission
        """
        permissions = [self.rolepermission]
        rules = ['list*', '*Vol*', 'listCapabilities']
        for rule in rules:
            data = copy.deepcopy(self.testdata["rolepermission"])
            data['rule'] = rule
            permission = RolePermission.create(
                self.apiclient,
                data
            )
            self.cleanup.append(permission)
            permissions.append(permission)


        # The following rule is considered to be created by another mgmt server
        data = copy.deepcopy(self.testdata["rolepermission"])
        data['rule'] = "someRule*"
        permission = RolePermission.create(
            self.apiclient,
            data
        )
        self.cleanup.append(permission)

        shuffle(permissions)
        try:
            permission.update(self.apiclient, ruleorder=",".join(map(lambda x: x.id, permissions)))
            self.fail("Reordering should fail in case of concurrent updates by other user")
        except CloudstackAPIException: pass


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_rolepermission_lifecycle_delete(self):
        """
            Tests deletion of role permission
        """
        permission = self.cleanup.pop(1)
        permission.delete(self.apiclient)
        list_rolepermissions = RolePermission.list(self.apiclient, roleid=self.role.id)
        self.assertEqual(
            list_rolepermissions,
            None,
            "List rolepermissions response should be empty"
        )


    def checkApiAvailability(self, apiConfig, userApiClient):
        """
            Checks available APIs based on api map
        """
        response = userApiClient.listApis(listApis.listApisCmd())
        allowedApis = map(lambda x: x.name, response)
        for api in allowedApis:
            for rule, perm in apiConfig.items():
                if re.match(rule.replace('*', '.*'), api):
                    if perm.lower() == 'allow':
                        break
                    else:
                        self.fail('Denied API found to be allowed: ' + api)


    def checkApiCall(self, apiConfig, userApiClient):
        """
            Performs actual API calls to verify API ACLs
        """
        list_accounts = userApiClient.listAccounts(listAccounts.listAccountsCmd())
        self.assertEqual(
            isinstance(list_accounts, list),
            True,
            "List accounts response was not a valid list"
        )
        self.assertNotEqual(
            len(list_accounts),
            0,
            "List accounts response was empty"
        )

        # Perform actual API call for deny API
        try:
            userApiClient.listHosts(listHosts.listHostsCmd())
            self.fail("API call succeeded which is denied for the role")
        except CloudstackAPIException: pass

        # Perform actual API call for API with no allow/deny rule
        try:
            userApiClient.listZones(listZones.listZonesCmd())
            self.fail("API call succeeded which has no allow/deny rule for the role")
        except CloudstackAPIException: pass



    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_role_account_acls(self):
        """
            Test to check role, role permissions and account life cycles
        """
        apiConfig = self.testdata['apiConfig']
        for api, perm in apiConfig.items():
            testdata = self.testdata['rolepermission']
            testdata['roleid'] = self.role.id
            testdata['rule'] = api
            testdata['permission'] = perm.lower()

            RolePermission.create(
                self.apiclient,
                testdata
            )

        userApiClient = self.getUserApiClient(self.account.name, domain=self.account.domain, role_type=self.account.roletype)

        # Perform listApis check
        self.checkApiAvailability(apiConfig, userApiClient)

        # Perform actual API call for allow API
        self.checkApiCall(apiConfig, userApiClient)


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_role_account_acls_multiple_mgmt_servers(self):
        """
            Test for role-rule enforcement in case of multiple mgmt servers
            Inserts rule directly in DB and checks expected behaviour
        """
        apiConfig = self.testdata["apiConfig"]
        roleId = self.dbclient.execute("select id from roles where uuid='%s'" % self.role.id)[0][0]
        sortOrder = 1
        for rule, perm in apiConfig.items():
            self.dbclient.execute("insert into role_permissions (uuid, role_id, rule, permission, sort_order) values (UUID(), %d, '%s', '%s', %d)" % (roleId, rule, perm.upper(), sortOrder))
            sortOrder += 1

        userApiClient = self.getUserApiClient(self.account.name, domain=self.account.domain, role_type=self.account.roletype)

        # Perform listApis check
        self.checkApiAvailability(apiConfig, userApiClient)

        # Perform actual API call for allow API
        self.checkApiCall(apiConfig, userApiClient)
