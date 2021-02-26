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
""" 
Test cases relating to access checks for createNetwork(), deploying VM in an isolated network and restartNetwork() for Admin, domain admin and regular users 
"""

# Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from marvin.cloudstackException import CloudstackAclException
from marvin.lib.common import *
from marvin.codes import (RUNNING,
                          ALLOCATED)
from nose.plugins.attrib import attr
# Import System modules
import time

_multiprocess_shared_ = True


class TestIsolatedNetwork(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        """
        Create the following domain tree and accounts that are reqiured for executing Test cases relating to access checks for createNetwork(), deploying VM in an isolated network and restartNetwork():
        Under ROOT - create 2 domaind D1 and D2
        Under D1 - Create 2 subdomain D11 and D12
        Under D11 - Create subdimain D111

        Under each of the domain create 1 admin user and couple of regular users.

        As each of these users , create an isolated network.

        """

        cls.testclient = super(TestIsolatedNetwork, cls).getClsTestClient()
        cls.apiclient = cls.testclient.getApiClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        cls.acldata = cls.testdata["acl"]
        cls.acldata["network"]["vlan"] = None
        cls.domain_1 = None
        cls.domain_2 = None
        cls._cleanup = []

        try:
            # backup default apikey and secretkey
            cls.default_apikey = cls.apiclient.connection.apiKey
            cls.default_secretkey = cls.apiclient.connection.securityKey

            # Create domains
            cls.domain_1 = Domain.create(
                cls.apiclient,
                cls.acldata["domain1"]
            )

            cls.domain_11 = Domain.create(
                cls.apiclient,
                cls.acldata["domain11"],
                parentdomainid=cls.domain_1.id
            )
            cls.domain_111 = Domain.create(
                cls.apiclient,
                cls.acldata["domain111"],
                parentdomainid=cls.domain_11.id,
            )
            cls.domain_12 = Domain.create(
                cls.apiclient,
                cls.acldata["domain12"],
                parentdomainid=cls.domain_1.id
            )
            cls.domain_2 = Domain.create(
                cls.apiclient,
                cls.acldata["domain2"]
            )
            # Create  1 admin account and 2 user accounts for doamin_1
            cls.account_d1 = Account.create(
                cls.apiclient,
                cls.acldata["accountD1"],
                admin=True,
                domainid=cls.domain_1.id
            )

            user = cls.generateKeysForUser(cls.apiclient, cls.account_d1)
            cls.user_d1_apikey = user.apikey
            cls.user_d1_secretkey = user.secretkey

            cls.account_d1a = Account.create(
                cls.apiclient,
                cls.acldata["accountD1A"],
                admin=False,
                domainid=cls.domain_1.id
            )
            user = cls.generateKeysForUser(cls.apiclient, cls.account_d1a)
            cls.user_d1a_apikey = user.apikey
            cls.user_d1a_secretkey = user.secretkey

            cls.account_d1b = Account.create(
                cls.apiclient,
                cls.acldata["accountD1B"],
                admin=False,
                domainid=cls.domain_1.id
            )

            user = cls.generateKeysForUser(cls.apiclient, cls.account_d1b)
            cls.user_d1b_apikey = user.apikey
            cls.user_d1b_secretkey = user.secretkey

            # Create  1 admin and 2 user accounts for doamin_11
            cls.account_d11 = Account.create(
                cls.apiclient,
                cls.acldata["accountD11"],
                admin=True,
                domainid=cls.domain_11.id
            )
            user = cls.generateKeysForUser(cls.apiclient, cls.account_d11)
            cls.user_d11_apikey = user.apikey
            cls.user_d11_secretkey = user.secretkey

            cls.account_d11a = Account.create(
                cls.apiclient,
                cls.acldata["accountD11A"],
                admin=False,
                domainid=cls.domain_11.id
            )
            user = cls.generateKeysForUser(cls.apiclient, cls.account_d11a)
            cls.user_d11a_apikey = user.apikey
            cls.user_d11a_secretkey = user.secretkey

            cls.account_d11b = Account.create(
                cls.apiclient,
                cls.acldata["accountD11B"],
                admin=False,
                domainid=cls.domain_11.id
            )
            user = cls.generateKeysForUser(cls.apiclient, cls.account_d11b)
            cls.user_d11b_apikey = user.apikey
            cls.user_d11b_secretkey = user.secretkey

            # Create  2 user accounts and 1 admin account for doamin_111

            cls.account_d111 = Account.create(
                cls.apiclient,
                cls.acldata["accountD111"],
                admin=True,
                domainid=cls.domain_111.id
            )
            user = cls.generateKeysForUser(cls.apiclient, cls.account_d111)
            cls.user_d111_apikey = user.apikey
            cls.user_d111_secretkey = user.secretkey

            cls.account_d111a = Account.create(
                cls.apiclient,
                cls.acldata["accountD111A"],
                admin=False,
                domainid=cls.domain_111.id
            )
            user = cls.generateKeysForUser(cls.apiclient, cls.account_d111a)
            cls.user_d111a_apikey = user.apikey
            cls.user_d111a_secretkey = user.secretkey

            cls.account_d111b = Account.create(
                cls.apiclient,
                cls.acldata["accountD111B"],
                admin=False,
                domainid=cls.domain_111.id
            )
            user = cls.generateKeysForUser(cls.apiclient, cls.account_d111b)
            cls.user_d111b_apikey = user.apikey
            cls.user_d111b_secretkey = user.secretkey

            # Create  2 user accounts for doamin_12
            cls.account_d12a = Account.create(
                cls.apiclient,
                cls.acldata["accountD12A"],
                admin=False,
                domainid=cls.domain_12.id
            )
            user = cls.generateKeysForUser(cls.apiclient, cls.account_d12a)
            cls.user_d12a_apikey = user.apikey
            cls.user_d12a_secretkey = user.secretkey

            cls.account_d12b = Account.create(
                cls.apiclient,
                cls.acldata["accountD12B"],
                admin=False,
                domainid=cls.domain_12.id
            )

            user = cls.generateKeysForUser(cls.apiclient, cls.account_d12b)
            cls.user_d12b_apikey = user.apikey
            cls.user_d12b_secretkey = user.secretkey

            # Create 1 user account for domain_2

            cls.account_d2a = Account.create(
                cls.apiclient,
                cls.acldata["accountD2"],
                admin=False,
                domainid=cls.domain_2.id
            )

            user = cls.generateKeysForUser(cls.apiclient, cls.account_d2a)
            cls.user_d2a_apikey = user.apikey
            cls.user_d2a_secretkey = user.secretkey

            # Create 1 user account and admin account in "ROOT" domain

            cls.account_roota = Account.create(
                cls.apiclient,
                cls.acldata["accountROOTA"],
                admin=False,
            )
            cls._cleanup.append(cls.account_roota)

            user = cls.generateKeysForUser(cls.apiclient, cls.account_roota)
            cls.user_roota_apikey = user.apikey
            cls.user_roota_secretkey = user.secretkey

            cls.account_root = Account.create(
                cls.apiclient,
                cls.acldata["accountROOT"],
                admin=True,
            )
            cls._cleanup.append(cls.account_root)

            user = cls.generateKeysForUser(cls.apiclient, cls.account_root)
            cls.user_root_apikey = user.apikey
            cls.user_root_secretkey = user.secretkey

            # create service offering
            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.acldata["service_offering"]["small"]
            )
            cls._cleanup.append(cls.service_offering)

            cls.zone = get_zone(cls.apiclient, cls.testclient.getZoneForTests())
            cls.acldata['mode'] = cls.zone.networktype
            cls.template = get_template(cls.apiclient, cls.zone.id, cls.acldata["ostype"])

            cls.apiclient.connection.apiKey = cls.default_apikey
            cls.apiclient.connection.securityKey = cls.default_secretkey

            list_isolated_network_offerings_response = NetworkOffering.list(
                cls.apiclient,
                name="DefaultIsolatedNetworkOfferingWithSourceNatService"
            )
            cls.isolated_network_offering_id = list_isolated_network_offerings_response[0].id

            ## Create Network objects for deployVirtualMachine and restartNetwork API related test cases

            cls.apiclient.connection.apiKey = cls.user_root_apikey
            cls.apiclient.connection.securityKey = cls.user_root_secretkey
            cls.network_root = cls.createNetwork(cls.apiclient, cls.account_root, cls.isolated_network_offering_id, cls.zone)
            cls.createVM(cls.apiclient, cls.zone.id, cls.service_offering.id, cls.template.id, cls.network_root.id)

            cls.apiclient.connection.apiKey = cls.user_d1_apikey
            cls.apiclient.connection.securityKey = cls.user_d1_secretkey
            cls.network_d1 = cls.createNetwork(cls.apiclient, cls.account_d1, cls.isolated_network_offering_id, cls.zone)
            cls.createVM(cls.apiclient, cls.zone.id, cls.service_offering.id, cls.template.id, cls.network_d1.id)

            cls.apiclient.connection.apiKey = cls.user_d1a_apikey
            cls.apiclient.connection.securityKey = cls.user_d1a_secretkey
            cls.network_d1a = cls.createNetwork(cls.apiclient, cls.account_d1a, cls.isolated_network_offering_id, cls.zone)
            cls.createVM(cls.apiclient, cls.zone.id, cls.service_offering.id, cls.template.id, cls.network_d1a.id)

            cls.apiclient.connection.apiKey = cls.user_d1b_apikey
            cls.apiclient.connection.securityKey = cls.user_d1b_secretkey
            cls.network_d1b = cls.createNetwork(cls.apiclient, cls.account_d1b, cls.isolated_network_offering_id, cls.zone)
            cls.createVM(cls.apiclient, cls.zone.id, cls.service_offering.id, cls.template.id, cls.network_d1b.id)

            cls.apiclient.connection.apiKey = cls.user_d11a_apikey
            cls.apiclient.connection.securityKey = cls.user_d11a_secretkey
            cls.network_d11a = cls.createNetwork(cls.apiclient, cls.account_d11a, cls.isolated_network_offering_id, cls.zone)
            cls.createVM(cls.apiclient, cls.zone.id, cls.service_offering.id, cls.template.id, cls.network_d11a.id)

            cls.apiclient.connection.apiKey = cls.user_d11b_apikey
            cls.apiclient.connection.securityKey = cls.user_d11b_secretkey
            cls.network_d11b = cls.createNetwork(cls.apiclient, cls.account_d11b, cls.isolated_network_offering_id, cls.zone)
            cls.createVM(cls.apiclient, cls.zone.id, cls.service_offering.id, cls.template.id, cls.network_d11b.id)

            cls.apiclient.connection.apiKey = cls.user_d12a_apikey
            cls.apiclient.connection.securityKey = cls.user_d12a_secretkey
            cls.network_d12a = cls.createNetwork(cls.apiclient, cls.account_d12a, cls.isolated_network_offering_id, cls.zone)
            cls.createVM(cls.apiclient, cls.zone.id, cls.service_offering.id, cls.template.id, cls.network_d12a.id)

            cls.apiclient.connection.apiKey = cls.user_roota_apikey
            cls.apiclient.connection.securityKey = cls.user_roota_secretkey
            cls.network_roota = cls.createNetwork(cls.apiclient, cls.account_roota, cls.isolated_network_offering_id, cls.zone)
            cls.createVM(cls.apiclient, cls.zone.id, cls.service_offering.id, cls.template.id, cls.network_roota.id)

            cls.apiclient.connection.apiKey = cls.user_d111a_apikey
            cls.apiclient.connection.securityKey = cls.user_d111a_secretkey
            cls.network_d111a = cls.createNetwork(cls.apiclient, cls.account_d111a, cls.isolated_network_offering_id, cls.zone)
            cls.createVM(cls.apiclient, cls.zone.id, cls.service_offering.id, cls.template.id, cls.network_d111a.id)

            cls.apiclient.connection.apiKey = cls.user_d111b_apikey
            cls.apiclient.connection.securityKey = cls.user_d111b_secretkey
            cls.network_d111b = cls.createNetwork(cls.apiclient, cls.account_d111b, cls.isolated_network_offering_id, cls.zone)
            cls.createVM(cls.apiclient, cls.zone.id, cls.service_offering.id, cls.template.id, cls.network_d111b.id)

            cls.apiclient.connection.apiKey = cls.user_d2a_apikey
            cls.apiclient.connection.securityKey = cls.user_d2a_secretkey
            cls.network_d2a = cls.createNetwork(cls.apiclient, cls.account_d2a, cls.isolated_network_offering_id, cls.zone)
            cls.createVM(cls.apiclient, cls.zone.id, cls.service_offering.id, cls.template.id, cls.network_d2a.id)

        except Exception as e:
            cls.domain_1.delete(cls.apiclient, cleanup="true")
            cls.domain_2.delete(cls.apiclient, cleanup="true")
            super(TestIsolatedNetwork,cls).tearDownClass()
            raise Exception("Failed to create the setup required to execute the test cases: %s" % e)

    @classmethod
    def tearDownClass(cls):
        # TODO more cleanup could be done in this class and in a more elegant way
        cls.apiclient = super(TestIsolatedNetwork, cls).getClsTestClient().getApiClient()
        cls.apiclient.connection.apiKey = cls.default_apikey
        cls.apiclient.connection.securityKey = cls.default_secretkey
        cls.domain_1.delete(cls.apiclient, cleanup="true")
        cls.domain_2.delete(cls.apiclient, cleanup="true")
        super(TestIsolatedNetwork,cls).tearDownClass()
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

    def tearDown(self):
        # restore back default apikey and secretkey
        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        return

    ## Test cases relating to createNetwork as admin user

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_01_createNetwork_admin(self):

        """
        # Validate that Admin should be able to create network for himslef
        """
        self.apiclient.connection.apiKey = self.user_root_apikey
        self.apiclient.connection.securityKey = self.user_root_secretkey
        self.acldata["network"]["name"] = "root"
        self.acldata["network"]["displayname"] = "root"

        network = Network.create(
            self.apiclient,
            self.acldata["network"],
            networkofferingid=self.isolated_network_offering_id,
            zoneid=self.zone.id
        )

        self.assertEqual(network.state.lower() == ALLOCATED.lower(),
                         True,
                         "Admin User is not able to create a network for himself")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_02_createNetwork_admin_foruserinsamedomain(self):
        """
        # Validate that Admin should be able to create network for users in his domain
        """
        self.apiclient.connection.apiKey = self.user_root_apikey
        self.apiclient.connection.securityKey = self.user_root_secretkey
        self.acldata["network"]["name"] = "root_roota"
        self.acldata["network"]["displayname"] = "root_roota"

        network = Network.create(
            self.apiclient,
            self.acldata["network"],
            networkofferingid=self.isolated_network_offering_id,
            zoneid=self.zone.id,
            accountid=self.account_roota.name,
            domainid=self.account_roota.domainid
        )

        self.assertEqual(network.state.lower() == ALLOCATED.lower(),
                         True,
                         "Admin User is not able to create a network for other users in his domain")


    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_03_createNetwork_admin_foruserinotherdomain(self):
        """
        # Validate that Admin should be able to create network for users in his sub domain
        """
        self.apiclient.connection.apiKey = self.user_root_apikey
        self.apiclient.connection.securityKey = self.user_root_secretkey
        self.acldata["network"]["name"] = "root_d11a"
        self.acldata["network"]["displayname"] = "root_d11a"

        network = Network.create(
            self.apiclient,
            self.acldata["network"],
            networkofferingid=self.isolated_network_offering_id,
            zoneid=self.zone.id,
            accountid=self.account_d11a.name,
            domainid=self.account_d11a.domainid
        )

        self.assertEqual(network.state.lower() == ALLOCATED.lower(),
                         True,
                         "Admin User is not able to create a network for for other users in other domain")


    ## Test cases relating to createNetwork as domain admin user

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_04_createNetwork_domaindmin(self):
        """
        # Validate that Domain admin should be able to create network for himslef
        """
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.acldata["network"]["name"] = "d1"
        self.acldata["network"]["displayname"] = "d1"

        network = Network.create(
            self.apiclient,
            self.acldata["network"],
            networkofferingid=self.isolated_network_offering_id,
            zoneid=self.zone.id
        )

        self.assertEqual(network.state.lower() == ALLOCATED.lower(),
                         True,
                         "Domain admin User is not able to create a network for himself")


    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_05_createNetwork_domaindmin_foruserinsamedomain(self):
        """
        # Validate that Domain admin should be able to create network for users in his domain
        """
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.acldata["network"]["name"] = "d1-d1a"
        self.acldata["network"]["displayname"] = "d1-d1a"

        network = Network.create(
            self.apiclient,
            self.acldata["network"],
            networkofferingid=self.isolated_network_offering_id,
            zoneid=self.zone.id,
            accountid=self.account_d1a.name,
            domainid=self.account_d1a.domainid
        )

        self.assertEqual(network.state.lower() == ALLOCATED.lower(),
                         True,
                         "Domain admin User is not able to create a network for other users in his domain")


    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_06_createNetwork_domaindmin_foruserinsubdomain(self):
        """
        # Validate that Domain admin should be able to create network for users in his sub domain
        """
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.acldata["network"]["name"] = "d1_d11a"
        self.acldata["network"]["displayname"] = "d1_d11a"

        network = Network.create(
            self.apiclient,
            self.acldata["network"],
            networkofferingid=self.isolated_network_offering_id,
            zoneid=self.zone.id,
            accountid=self.account_d11a.name,
            domainid=self.account_d11a.domainid
        )

        self.assertEqual(network.state.lower() == ALLOCATED.lower(),
                         True,
                         "Domain admin User is not able to create a network for other users in his sub domain")


    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_07_createNetwork_domaindmin_forcrossdomainuser(self):
        """
        # Validate that Domain admin should not be able to create network for users in his sub domain
        """
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.acldata["network"]["name"] = "d1_d2a"
        self.acldata["network"]["displayname"] = "d1_d2a"
        try:
            network = Network.create(
                self.apiclient,
                self.acldata["network"],
                networkofferingid=self.isolated_network_offering_id,
                zoneid=self.zone.id,
                accountid=self.account_d2a.name,
                domainid=self.account_d2a.domainid
            )
            self.fail("Domain admin is allowed to create network for users not in his domain ")
        except Exception as e:
            self.debug("When Domain admin tries to create network for users in his sub domain %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NO_PERMISSION_TO_OPERATE_DOMAIN):
                self.fail("Error message validation failed when Domain admin tries to create network for users not in his domain ")

    ## Test cases relating to createNetwork as regular user

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_08_createNetwork_user(self):
        """
        # Validate that Regular should be able to create network for himslef
        """
        self.apiclient.connection.apiKey = self.user_d1a_apikey
        self.apiclient.connection.securityKey = self.user_d1a_secretkey
        self.acldata["network"]["name"] = "d1a"
        self.acldata["network"]["displayname"] = "d1a"

        network = Network.create(
            self.apiclient,
            self.acldata["network"],
            networkofferingid=self.isolated_network_offering_id,
            zoneid=self.zone.id
        )

        self.assertEqual(network.state.lower() == ALLOCATED.lower(),
                         True,
                         "User is not able to create a network for himself")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_09_createNetwork_user_foruserinsamedomain(self):
        """
        # Validate that  Regular user should NOT be able to create network for users in his domain
        """
        self.apiclient.connection.apiKey = self.user_d1a_apikey
        self.apiclient.connection.securityKey = self.user_d1a_secretkey
        self.acldata["network"]["name"] = "d1a_d1b"
        self.acldata["network"]["displayname"] = "d1a_d1b"

        try:
            network = Network.create(
                self.apiclient,
                self.acldata["network"],
                networkofferingid=self.isolated_network_offering_id,
                zoneid=self.zone.id,
                accountid=self.account_d1b.name,
                domainid=self.account_d1b.domainid
            )
            self.fail("User is allowed to create network for other users in his domain ")
        except Exception as e:
            self.debug("When user tries to create network for users in his domain %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.UNABLE_TO_LIST_NETWORK_ACCOUNT):
                self.fail("Error message validation failed when when User tries to create network for other users in his domain ")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_10_createNetwork_user_foruserinotherdomain(self):
        """
        # Validate that Domain admin should be NOT be able to create network for users in other domains
        """
        self.apiclient.connection.apiKey = self.user_d1a_apikey
        self.apiclient.connection.securityKey = self.user_d1a_secretkey
        self.acldata["network"]["name"] = "d1a_d11a"
        self.acldata["network"]["displayname"] = "d1a_d11a"

        try:
            network = Network.create(
                self.apiclient,
                self.acldata["network"],
                networkofferingid=self.isolated_network_offering_id,
                zoneid=self.zone.id,
                accountid=self.account_d11a.name,
                domainid=self.account_d11a.domainid
            )
            self.fail("User is allowed to create network for users not in his domain ")
        except Exception as e:
            self.debug("When user tries to create network for users in other domain %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.UNABLE_TO_LIST_NETWORK_ACCOUNT):
                self.fail("Error message validation failed when User tries to create network for users not in his domain ")

    ## Test cases relating to Deploying VM in a network as admin user

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_11_deployvm_admin(self):
        """
        # Validate that Admin should be able to deploy VM in the networks he owns
        """
        self.apiclient.connection.apiKey = self.user_root_apikey
        self.apiclient.connection.securityKey = self.user_root_secretkey
        vmData = {"name": "root-root", "dispayname": "root-root"}

        vm = VirtualMachine.create(
            self.apiclient,
            vmData,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.network_root.id
        )

        self.assertEqual(vm.state.lower() == RUNNING.lower(),
                         True,
                         "Admin User is not able to deploy VM in his own network")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_12_deployvm_admin_foruserinsamedomain(self):
        """
        # Validate that Admin should be able to deploy Vm  for users in his domain
        """
        self.apiclient.connection.apiKey = self.user_root_apikey
        self.apiclient.connection.securityKey = self.user_root_secretkey
        vmData = {"name": "roota-root", "displayname": "roota-root"}

        vm = VirtualMachine.create(
            self.apiclient,
            vmData,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.network_roota.id,
            accountid=self.account_roota.name,
            domainid=self.account_roota.domainid
        )

        self.assertEqual(vm.state.lower() == RUNNING.lower() and vm.account == self.account_roota.name and vm.domainid == self.account_roota.domainid,
                         True,
                         "Admin User is not able to deploy VM for users in his domain")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_13_deployvm_admin_foruserinotherdomain(self):
        """
        # Validate that Admin should be able to deploy VM for users in his sub domain
        """
        self.apiclient.connection.apiKey = self.user_root_apikey
        self.apiclient.connection.securityKey = self.user_root_secretkey
        vmData = {"name": "d2a-root", "displayname": "d2a-root"}

        vm = VirtualMachine.create(
            self.apiclient,
            vmData,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.network_d2a.id,
            accountid=self.account_d2a.name,
            domainid=self.account_d2a.domainid
        )

        self.assertEqual(vm.state.lower() == RUNNING.lower() and vm.account == self.account_d2a.name and vm.domainid == self.account_d2a.domainid,
                         True,
                         "Admin User is not able to deploy VM for users users in other domain")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_13_1_deployvm_admin_foruserinotherdomain_crossnetwork(self):
        """
        # Validate that Admin should not be able deploy VM for a user in a network that does not belong to the user
        """
        self.apiclient.connection.apiKey = self.user_root_apikey
        self.apiclient.connection.securityKey = self.user_root_secretkey
        vmData = {"name": "d11a-root-invalidnetwork", "displayname": "d11a-root-invalidnetwork"}
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                vmData,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.network_d11b.id,
                accountid=self.account_d11a.name,
                domainid=self.account_d11a.domainid
            )
            self.fail("Admin is allowed to deploy VM for a user in a network that does not belong to the user ")
        except Exception as e:
            self.debug("When admin tries to deploy vm for users in network that does not belong to the user %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.UNABLE_TO_USE_NETWORK):
                self.fail("Admin tries to deploy VM for a user in a network that does not belong to the user ")

    ## Test cases relating to deploying VM as domain admin user

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_14_deployvm_domaindmin(self):
        """
        # Validate that Domain admin should be able to deploy vm for himslef
        """
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        vmData = {"name": "d1-d1", "displayname": "d1-d1"}

        vm = VirtualMachine.create(
            self.apiclient,
            vmData,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.network_d1.id,
        )

        self.assertEqual(vm.state.lower() == RUNNING.lower(),
                         True,
                         "Domain admin User is not able to deploy VM for himself")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_15_deployvm_domaindmin_foruserinsamedomain(self):
        """
        # Validate that Domain admin should be able to deploy vm for users in his domain
        """
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        vmData = {"name": "d1a-d1", "displayname": "d1a-d1"}

        vm = VirtualMachine.create(
            self.apiclient,
            vmData,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.network_d1a.id,
            accountid=self.account_d1a.name,
            domainid=self.account_d1a.domainid
        )
        self.assertEqual(vm.state.lower() == RUNNING.lower() and vm.account == self.account_d1a.name and vm.domainid == self.account_d1a.domainid,
                         True,
                         "Domain admin User is not able to deploy VM for other users in his domain")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_16_deployvm_domaindmin_foruserinsubdomain(self):
        """
        # Validate that Domain admin should be able to deploy vm for users in his sub domain
        """
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        vmData = {"name": "d11a-d1", "displayname": "d111a-d1"}

        vm = VirtualMachine.create(
            self.apiclient,
            vmData,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.network_d11a.id,
            accountid=self.account_d11a.name,
            domainid=self.account_d11a.domainid
        )
        self.assertEqual(vm.state.lower() == RUNNING.lower() and vm.account == self.account_d11a.name and vm.domainid == self.account_d11a.domainid,
                         True,
                         "Domain admin User is not able to deploy vm for himself")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_17_deployvm_domaindmin_forcrossdomainuser(self):
        """
        # Validate that Domain admin should not be able allowed to deploy vm for users not in his sub domain
        """
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        vmData = {"name": "d2a-d1", "displayname": "d2a-d1"}

        try:
            vm = VirtualMachine.create(
                self.apiclient,
                vmData,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.network_d2a.id,
                accountid=self.account_d2a.name,
                domainid=self.account_d2a.domainid
            )
            self.fail("Domain admin is allowed to deploy vm for users not in hos domain ")
        except Exception as e:
            self.debug("When Domain admin tries to deploy vm for users in his sub domain %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NO_PERMISSION_TO_OPERATE_DOMAIN):
                self.fail("Error message validation failed when Domain admin tries to deploy vm for users not in hos domain ")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_17_1_deployvm_domainadmin_foruserinotherdomain_crossnetwork(self):
        """
        # Validate that Domain admin should not be able deploy VM for a user in a network that does not belong to the user
        """
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        vmData = {"name": "d1-d11a-invalidnetwork", "displayname": "d1-d11a-invalidnetwork"}
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                vmData,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.network_d11b.id,
                accountid=self.account_d11a.name,
                domainid=self.account_d11a.domainid
            )
            self.fail("Domain admin is allowed to deploy vm for users in a network that does not belong to him ")
        except Exception as e:
            self.debug("When domain admin tries to deploy vm for users in network that does not belong to the user %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.UNABLE_TO_USE_NETWORK):
                self.fail("Error message validation failed when Domain admin tries to deploy vm for users in a network that does not belong to him ")

    ## Test cases relating to deploying VM as regular user

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_18_deployvm_user(self):
        """
        # Validate that Regular should be able to deploy vm for himslef
        """
        self.apiclient.connection.apiKey = self.user_d1a_apikey
        self.apiclient.connection.securityKey = self.user_d1a_secretkey
        vmData = {"name": "d1a-d1a", "displayname": "d1a-d1a"}

        vm = VirtualMachine.create(
            self.apiclient,
            vmData,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.network_d1a.id,
        )
        self.assertEqual(vm.state.lower() == RUNNING.lower(),
                         True,
                         "User is not able to deploy vm for himself")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_19_deployvm_user_foruserinsamedomain(self):
        """
        # Validate that Regular user should NOT be able to deploy vm for users in his domain
        """
        self.apiclient.connection.apiKey = self.user_d1a_apikey
        self.apiclient.connection.securityKey = self.user_d1a_secretkey
        vmData = {"name": "d1b-d1a", "displayname": "d1b-d1a"}

        try:
            vm = VirtualMachine.create(
                self.apiclient,
                vmData,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.network_d1b.id,
                accountid=self.account_d1b.name,
                domainid=self.account_d1b.domainid
            )
            self.fail("Regular user is allowed to deploy vm for other users in his domain ")
        except Exception as e:
            self.debug("When user tries to deploy vm for users in his domain %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NO_PERMISSION_TO_OPERATE_ACCOUNT):
                self.fail("Error message validation failed when Regular user tries to deploy vm for other users in his domain ")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_20_deployvm_user_foruserincrossdomain(self):
        """
        # Validate that Regular user should NOT be able to deploy vm for users in his domain
        """
        self.apiclient.connection.apiKey = self.user_d1a_apikey
        self.apiclient.connection.securityKey = self.user_d1a_secretkey
        vmData = {"name": "d2a-d1a", "displayname": "d2a-d1a"}

        try:
            vm = VirtualMachine.create(
                self.apiclient,
                vmData,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.network_d2a.id,
                accountid=self.account_d2a.name,
                domainid=self.account_d2a.domainid
            )
            self.fail("Regular user is allowed to deploy vm for users not in his domain ")
        except Exception as e:
            self.debug("When user tries to deploy vm for users n different domain %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NO_PERMISSION_TO_OPERATE_ACCOUNT):
                self.fail("Error message validation failed when Regular user tries to deploy vm for users not in his domain ")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_20_1_deployvm_user_incrossnetwork(self):
        """
        #Validate that User should not be able deploy VM in a network that does not belong to him
        """
        self.apiclient.connection.apiKey = self.user_d11a_apikey
        self.apiclient.connection.securityKey = self.user_d11a_secretkey
        vmData = {"name": "d11a-invalidnetwork", "displayname": "d11a-invalidnetwork"}
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                vmData,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.network_d11b.id,
            )
            self.fail("User is allowed to deploy VM  in a network that does not belong to him ")
        except Exception as e:
            self.debug("When user tries to deploy vm in a  network that does not belong to him %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.UNABLE_TO_USE_NETWORK):
                self.fail("Error message validation failed when User is allowed to deploy VM  in a network that does not belong to him ")

    ## Test cases relating to restart Network as admin user

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_21_restartNetwork_admin(self):
        """
        #Validate that Admin should be able to restart network for networks he owns
        """
        self.apiclient.connection.apiKey = self.user_root_apikey
        self.apiclient.connection.securityKey = self.user_root_secretkey

        restartResponse = self.network_root.restart(self.apiclient)

        self.assertEqual(restartResponse.success,
                         True,
                         "Admin User is not able to restart network he owns")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_22_restartNetwork_admin_foruserinsamedomain(self):
        """
        # Validate that Admin should be able to restart network for users in his domain
        """
        self.apiclient.connection.apiKey = self.user_root_apikey
        self.apiclient.connection.securityKey = self.user_root_secretkey

        restartResponse = self.network_roota.restart(self.apiclient)

        self.assertEqual(restartResponse.success,
                         True,
                         "Admin User is not able to restart network owned by users his domain")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_23_restartNetwork_admin_foruserinotherdomain(self):
        """
        # Validate that Admin should be able to restart network for users in his sub domain
        """
        self.apiclient.connection.apiKey = self.user_root_apikey
        self.apiclient.connection.securityKey = self.user_root_secretkey

        restartResponse = self.network_d11a.restart(self.apiclient)

        self.assertEqual(restartResponse.success,
                         True,
                         "Admin User is not able to restart network owned other users in other domain")


    ## Test cases relating to restart Network as domain admin user

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_24_restartNetwork_domaindmin(self):
        """
        # Validate that Domain admin should be able to restart network for himslef
        """
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey

        restartResponse = self.network_d1.restart(self.apiclient)

        self.assertEqual(restartResponse.success,
                         True,
                         "Domain admin User is not able to restart network for himself")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_25_restartNetwork_domaindmin_foruserinsamedomain(self):
        """
        # Validate that Domain admin should be able to restart network for users in his domain
        """
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey

        restartResponse = self.network_d1a.restart(self.apiclient)
        self.assertEqual(restartResponse.success,
                         True,
                         "Domain admin User is not able to restart network for other users in his domain")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_26_restartNetwork_domaindmin_foruserinsubdomain(self):
        """
        # Validate that Domain admin should be able to restart network for users in his sub domain
        """
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey

        restartResponse = self.network_d11a.restart(self.apiclient)
        self.assertEqual(restartResponse.success,
                         True,
                         "Domain admin User is not able to restart network he owns")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_27_restartNetwork_domaindmin_forcrossdomainuser(self):
        """
        # Validate that Domain admin should be able to restart network for users in his sub domain
        """
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey

        try:
            restartResponse = self.network_d2a.restart(self.apiclient)
            self.fail("Domain admin is allowed to restart network for users not in his domain ")
        except Exception as e:
            self.debug("When Domain admin tries to restart network for users in his sub domain %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NO_PERMISSION_TO_OPERATE_DOMAIN):
                self.fail("Error message validation failed when Domain admin tries to restart network for users not in his domain ")

    ## Test cases relating restart network as regular user

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_28_restartNetwork_user(self):
        """
        #Validate that  Regular should be able to restart network for himslef
        """
        self.apiclient.connection.apiKey = self.user_d1a_apikey
        self.apiclient.connection.securityKey = self.user_d1a_secretkey

        restartResponse = self.network_d1a.restart(self.apiclient)
        self.assertEqual(restartResponse.success,
                         True,
                         "User is not able to restart network he owns")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_29_restartNetwork_user_foruserinsamedomain(self):
        """
        #Validate that Regular user should NOT be able to restart network for users in his domain
        """
        self.apiclient.connection.apiKey = self.user_d1a_apikey
        self.apiclient.connection.securityKey = self.user_d1a_secretkey

        try:
            restartResponse = self.network_d1b.restart(self.apiclient)
            self.fail("Regular user is allowed to restart network for users in his domain ")
        except Exception as e:
            self.debug("When user tries to restart network for users in his domain %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NO_PERMISSION_TO_OPERATE_ACCOUNT):
                self.fail("Error message validation failed when Regular user tries to restart network for users in his domain ")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_30_restartNetwork_user_foruserinotherdomain(self):
        """
        #Validate that Domain admin should be NOT be able to restart network for users in other domains
        """
        self.apiclient.connection.apiKey = self.user_d1a_apikey
        self.apiclient.connection.securityKey = self.user_d1a_secretkey

        try:
            restartResponse = self.network_d11a.restart(self.apiclient)
            self.fail("Regular user is allowed to restart network for users not in his domain ")
        except Exception as e:
            self.debug("When user tries to restart network for users in other domain %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NO_PERMISSION_TO_OPERATE_ACCOUNT):
                self.fail("Error message validation failed when Regular user is allowed to restart network for users not in his domain ")

    @staticmethod
    def generateKeysForUser(apiclient, account):
        user = User.list(
            apiclient,
            account=account.name,
            domainid=account.domainid
        )[0]

        return (User.registerUserKeys(
            apiclient,
            user.id
        ))

    @staticmethod
    def createNetwork(apiclient, account, isolated_network_offering_id, zone):
        network = {
            "name": "Network-",
            "displaytext": "Network-",
            "gateway": "10.223.1.1",
            "netmask": "255.255.255.0",
            "startip": "10.223.1.2",
            "endip": "10.223.1.100",
        }

        network["name"] = account.name + " -forupdate"
        network["displayname"] = account.name + "-forupdate"

        network = Network.create(
            apiclient,
            network,
            networkofferingid=isolated_network_offering_id,
            zoneid=zone.id
        )
        return network

    @staticmethod
    def createVM(apiclient, zoneId, serviceOfferingId, templateId, networkId):
        vmData = {"name": "prereq", "dispayname": "prereq"}

        vm = VirtualMachine.create(
            apiclient,
            vmData,
            zoneid=zoneId,
            serviceofferingid=serviceOfferingId,
            templateid=templateId,
            networkids=networkId
        )

        return vm
