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
""" P1 tests for Account
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (random_gen,
                              cleanup_resources,
                              validateList)
from marvin.cloudstackAPI import *
from marvin.lib.base import (Domain,
                             Account,
                             ServiceOffering,
                             VirtualMachine,
                             Network,
                             User,
                             Template,
                             Role)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_test_template,
                               list_accounts,
                               list_virtual_machines,
                               list_service_offering,
                               list_templates,
                               list_users,
                               wait_for_cleanup)
from nose.plugins.attrib import attr
from marvin.cloudstackException import CloudstackAPIException
from marvin.codes import PASS
import time

from pyVmomi.VmomiSupport import GetVersionFromVersionUri

class Services:

    """Test Account Services
    """

    def __init__(self):
        self.services = {
            "domain": {
                "name": "Domain",
            },
            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                # Random characters are appended for unique
                # username
                "password": "fr3sca",
            },
            "role": {
                "name": "User Role",
                "type": "User",
                "description": "User Role created by Marvin test"
            },
            "user": {
                "email": "user@test.com",
                "firstname": "User",
                "lastname": "User",
                "username": "User",
                # Random characters are appended for unique
                # username
                "password": "fr3sca",
            },
            "service_offering": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100,
                # in MHz
                "memory": 128,
                # In MBs
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
                "ostype": 'CentOS 5.6 (64-bit)',
                "url": "",
                "hypervisor": '',
                "format": '',
                "isfeatured": True,
                "ispublic": True,
                "isextractable": True,
                "templatefilter": "self"
            },
            "ostype": 'CentOS 5.6 (64-bit)',
            "sleep": 60,
            "timeout": 10,
        }

class TestUserLogin(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestUserLogin, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        super(TestUserLogin,cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

        self.debug("Enabling 2FA in global setting")
        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "enable.user.2fa"
        updateConfigurationCmd.value = "true"
        updateConfigurationResponse = self.apiclient.updateConfiguration(
            updateConfigurationCmd)

        self.cleanup = []
        return

    def tearDown(self):
        self.debug("Disable 2FA in global setting")
        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "enable.user.2fa"
        updateConfigurationCmd.value = "false"
        updateConfigurationResponse = self.apiclient.updateConfiguration(
            updateConfigurationCmd)

        super(TestUserLogin,self).tearDown()

    @attr(tags=["login", "accounts", "simulator", "advanced",
                "advancedns", "basic", "eip", "sg"])
    def test_2FA_enabled(self):
        """Test if Login API does not return UUID's
        """

        # Steps for test scenario
        # 1. create a user account
        # 2. login to the user account with given credentials (loginCmd)
        # 3. verify login response

        # Setup Global settings

        self.debug("Mandate 2FA in global setting")
        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "mandate.user.2fa"
        updateConfigurationCmd.value = "true"
        updateConfigurationResponse = self.apiclient.updateConfiguration(
            updateConfigurationCmd)

        self.debug("Creating an user account..")
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(self.account)

        self.debug("Logging into the cloudstack with login API")
        response = User.login(
            self.apiclient,
            username=self.account.name,
            password=self.services["account"]["password"]
        )

        self.debug("Login API response: %s" % response)

        self.assertEqual(
            response.is2faenabled,
            "true",
            "2FA enabled for user"
        )

        self.debug("Remove mandating 2FA in global setting")
        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "mandate.user.2fa"
        updateConfigurationCmd.value = "false"
        updateConfigurationResponse = self.apiclient.updateConfiguration(
            updateConfigurationCmd)

        return

    @attr(tags=["login", "accounts", "simulator", "advanced",
                "advancedns", "basic", "eip", "sg"])
    def test_2FA_setup(self):
        """Test if Login API does not return UUID's
        """

        # Steps for test scenario
        # 1. create a user account
        # 2. login to the user account with given credentials (loginCmd)
        # 3. verify login response for 2fa
        # 4. setup 2fa for the user
        # 5. verify the code in the setup 2fa response
        # 6. test disable 2FA

        # Setup Global settings

        self.debug("Mandate 2FA in global setting")
        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "mandate.user.2fa"
        updateConfigurationCmd.value = "true"
        updateConfigurationResponse = self.apiclient.updateConfiguration(
            updateConfigurationCmd)

        self.debug("Creating an user account..")
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(self.account)

        self.debug("Logging into the cloudstack with login API")
        response = User.login(
            self.apiclient,
            username=self.account.name,
            password=self.services["account"]["password"]
        )

        self.debug("Login API response: %s" % response)

        self.assertEqual(
            response.is2faenabled,
            "true",
            "2FA enabled for user"
        )

        self.user = self.account.user[0]
        self.user_apiclient = self.testClient.getUserApiClient(
            self.user.username, self.domain.id
        )

        setup2faCmd = setupUserTwoFactorAuthentication.setupUserTwoFactorAuthenticationCmd()
        setup2faCmd.provider = "staticpin"
        setup2faResponse = self.user_apiclient.setupUserTwoFactorAuthentication(
            setup2faCmd)

        self.assertNotEqual(
            setup2faResponse.secretcode,
            None,
            "2FA enabled for user"
        )

        disable2faCmd = setupUserTwoFactorAuthentication.setupUserTwoFactorAuthenticationCmd()
        disable2faCmd.enable = "false"
        disable2faResponse = self.user_apiclient.setupUserTwoFactorAuthentication(
            disable2faCmd)

        self.assertEqual(
            disable2faResponse.secretcode,
            None,
            "2FA disabled for user"
        )

        self.debug("Remove mandating 2FA in global setting")
        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "mandate.user.2fa"
        updateConfigurationCmd.value = "false"
        updateConfigurationResponse = self.apiclient.updateConfiguration(
            updateConfigurationCmd)

        return
