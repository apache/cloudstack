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
 Test cases for deploying Virtual Machine using impersonation (passing account and domainId parameters) for shared network 
"""
# Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from marvin.cloudstackException import CloudstackAclException
from nose.plugins.attrib import attr
# Import System modules
import time

_multiprocess_shared_ = True


class TestSharedNetworkImpersonation(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        """
        Create the following domain tree and accounts that are reqiured for executing impersonation test cases for shared networks:
        Under ROOT - create 2 domaind D1 and D2
        Under D1 - Create 2 subdomain D11 and D12
        Under D11 - Create subdimain D111

        Under each of the domain create 1 admin user and couple of regular users.

        Create shared network with the following scope:
        1. Network with scope="all"
        2. Network with scope="domain" with no subdomain access
        3. Network with scope="domain" with subdomain access
        4. Network with scope="account"

        """

        cls.testclient = super(TestSharedNetworkImpersonation, cls).getClsTestClient()
        cls.apiclient = cls.testclient.getApiClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        cls.acldata = cls.testdata["acl"]

        cls.domain_1 = None
        cls.domain_2 = None
        cleanup = []

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

            user = cls.generateKeysForUser(cls.apiclient, cls.account_roota)
            cls.user_roota_apikey = user.apikey
            cls.user_roota_secretkey = user.secretkey

            cls.account_root = Account.create(
                cls.apiclient,
                cls.acldata["accountROOTA"],
                admin=True,
            )

            user = cls.generateKeysForUser(cls.apiclient, cls.account_root)
            cls.user_root_apikey = user.apikey
            cls.user_root_secretkey = user.secretkey

            # create service offering
            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.acldata["service_offering"]["small"]
            )
            cls.zone = get_zone(cls.apiclient, cls.testclient.getZoneForTests())
            cls.acldata['mode'] = cls.zone.networktype
            cls.template = get_template(cls.apiclient, cls.zone.id, cls.acldata["ostype"])

            ## As admin user , create shared network with scope "all","domain" with subdomain access , "domain" without subdomain access and "account"

            cls.apiclient.connection.apiKey = cls.default_apikey
            cls.apiclient.connection.securityKey = cls.default_secretkey

            list_shared_network_offerings_response = NetworkOffering.list(
                cls.apiclient,
                name="DefaultSharedNetworkOffering",
                displayText="Offering for Shared networks"
            )
            # Override name parameter so that there is no overlap with names being used in other shared network test suites
            cls.acldata["network_all"]["name"] = cls.acldata["network_all"]["name"] + "-impersonation"
            cls.acldata["network_domain_with_no_subdomain_access"]["name"] = cls.acldata["network_domain_with_no_subdomain_access"]["name"] + "-impersonation"
            cls.acldata["network_domain_with_subdomain_access"]["name"] = cls.acldata["network_domain_with_subdomain_access"]["name"] + "-impersonation"
            cls.acldata["network_account"]["name"] = cls.acldata["network_account"]["name"] + "-impersonation"

            cls.shared_network_offering_id = list_shared_network_offerings_response[0].id
            cls.shared_network_all = Network.create(
                cls.apiclient,
                cls.acldata["network_all"],
                networkofferingid=cls.shared_network_offering_id,
                zoneid=cls.zone.id
            )

            cls.shared_network_domain_d11 = Network.create(
                cls.apiclient,
                cls.acldata["network_domain_with_no_subdomain_access"],
                networkofferingid=cls.shared_network_offering_id,
                zoneid=cls.zone.id,
                domainid=cls.domain_11.id,
                subdomainaccess=False
            )

            cls.shared_network_domain_with_subdomain_d11 = Network.create(
                cls.apiclient,
                cls.acldata["network_domain_with_subdomain_access"],
                networkofferingid=cls.shared_network_offering_id,
                zoneid=cls.zone.id,
                domainid=cls.domain_11.id,
                subdomainaccess=True
            )

            cls.shared_network_account_d111a = Network.create(
                cls.apiclient,
                cls.acldata["network_account"],
                networkofferingid=cls.shared_network_offering_id,
                zoneid=cls.zone.id,
                domainid=cls.domain_111.id,
                accountid=cls.account_d111a.user[0].username
            )
            cls.vmdata = {"name": "test",
                          "displayname": "test"
                          }

            cls.cleanup = [
                cls.account_root,
                cls.account_roota,
                cls.shared_network_all,
                cls.service_offering,
            ]
        except Exception as e:
            cls.domain_1.delete(cls.apiclient, cleanup="true")
            cls.domain_2.delete(cls.apiclient, cleanup="true")
            cleanup_resources(cls.apiclient, cls.cleanup)
            raise Exception("Failed to create the setup required to execute the test cases: %s" % e)

    @classmethod
    def tearDownClass(cls):
        cls.apiclient = super(TestSharedNetworkImpersonation, cls).getClsTestClient().getApiClient()
        cls.apiclient.connection.apiKey = cls.default_apikey
        cls.apiclient.connection.securityKey = cls.default_secretkey
        cls.domain_1.delete(cls.apiclient, cleanup="true")
        cls.domain_2.delete(cls.apiclient, cleanup="true")
        cleanup_resources(cls.apiclient, cls.cleanup)
        return

    def setUp(cls):
        cls.apiclient = cls.testClient.getApiClient()
        cls.dbclient = cls.testClient.getDbConnection()

    def tearDown(cls):
        # restore back default apikey and secretkey
        cls.apiclient.connection.apiKey = cls.default_apikey
        cls.apiclient.connection.securityKey = cls.default_secretkey
        return

    ## Test cases relating to deploying Virtual Machine as ROOT admin for other users in shared network with scope=all

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_all_domainuser(self):
        """
        Valiate that ROOT admin is able to deploy a VM for other users in a shared network with scope=all
        """

        # Deploy VM for a user in a domain under ROOT as admin

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmD1A"]["name"] + "-shared-scope-all-root-admin"
        self.vmdata["displayname"] = self.acldata["vmD1A"]["displayname"] + "-shared-scope-all-root-admin"

        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_all.id,
            accountid=self.account_d1a.name,
            domainid=self.account_d1a.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d1a.name and vm.domainid == self.account_d1a.domainid,
                         True,
                         "ROOT admin is not able to deploy a VM for other users in a shared network with scope=all")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_all_domainadminuser(self):
        """
        Valiate that ROOT admin is able to deploy a VM for a domain admin users in a shared network with scope=all

        """
        # Deploy VM for an admin user in a domain under ROOT as admin

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmD1"]["name"] + "-shared-scope-all-root-admin"
        self.vmdata["displayname"] = self.acldata["vmD1"]["displayname"] + "-shared-scope-all-root-admin"

        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_all.id,
            accountid=self.account_d1.name,
            domainid=self.account_d1.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d1.name and vm.domainid == self.account_d1.domainid,
                         True,
                         "ROOT admin is not able to deploy a VM for a domain admin users in a shared network with scope=all")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_all_subdomainuser(self):
        """
        Valiate that ROOT admin is able to deploy a VM for any user in a subdomain in a shared network with scope=all
        """
        # Deploy VM as user in a subdomain under ROOT 

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmD11A"]["name"] + "-shared-scope-all-root-admin"
        self.vmdata["displayname"] = self.acldata["vmD11A"]["displayname"] + "-shared-scope-all-root-admin"
        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_all.id,
            accountid=self.account_d11a.name,
            domainid=self.account_d11a.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d11a.name and vm.domainid == self.account_d11a.domainid,
                         True,
                         "ROOT admin is not able to deploy a VM for any user in a subdomain in a shared network with scope=all")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_all_subdomainadminuser(self):
        """
        Valiate that ROOT admin is able to deploy a VM for admin user in a domain in a shared network with scope=all

        """
        # Deploy VM as an admin user in a subdomain under ROOT

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmD11"]["name"] + "-shared-scope-all-root-admin"
        self.vmdata["displayname"] = self.acldata["vmD11"]["displayname"] + "-shared-scope-all-root-admin"
        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_all.id,
            accountid=self.account_d11.name,
            domainid=self.account_d11.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d11.name and vm.domainid == self.account_d11.domainid,
                         True,
                         "ROOT admin is not able to deploy a VM for admin user in a domain in a shared network with scope=all")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_all_ROOTuser(self):
        """
        Valiate that ROOT admin is able to deploy a VM for user in ROOT domain in a shared network with scope=all

        """
        # Deploy VM as user in ROOT domain

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmROOTA"]["name"] + "-shared-scope-all-root-admin"
        self.vmdata["displayname"] = self.acldata["vmROOTA"]["displayname"] + "-shared-scope-all-root-admin"
        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_all.id,
            accountid=self.account_roota.name,
            domainid=self.account_roota.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_roota.name and vm.domainid == self.account_roota.domainid,
                         True,
                         "ROOT admin is not able to deploy a VM for user in ROOT domain in a shared network with scope=all")

    ## Test cases relating to deploying Virtual Machine as ROOT admin for other users in shared network with scope=Domain and no subdomain access

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_domain_nosubdomainaccess_domainuser(self):
        """
        Valiate that ROOT admin is able to deploy a VM for domain user in a shared network with scope=domain with no subdomain access

        """
        # Deploy VM as user in a domain that has shared network with no subdomain access

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmD11A"]["name"] + "-shared-scope-domain-nosubdomainaccess-root-admin"
        self.vmdata["displayname"] = self.acldata["vmD11A"]["displayname"] + "-shared-scope-domain-nosubdomainaccess-root-admin"

        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_domain_d11.id,
            accountid=self.account_d11a.name,
            domainid=self.account_d11a.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d11a.name and vm.domainid == self.account_d11a.domainid,
                         True,
                         "ROOT admin is not able to deploy a VM for domain user in a shared network with scope=domain with no subdomain access")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_domain_nosubdomainaccess_domainadminuser(self):
        """
        Valiate that ROOT admin is able to deploy a VM for domain admin user in a shared network with scope=domain with no subdomain access
        """

        # Deploy VM as an admin user in a domain that has shared network with no subdomain access

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmD11"]["name"] + "-shared-scope-domain-nosubdomainaccess-root-admin"
        self.vmdata["displayname"] = self.acldata["vmD11"]["displayname"] + "-shared-scope-domain-nosubdomainaccess-root-admin"

        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_domain_d11.id,
            accountid=self.account_d11.name,
            domainid=self.account_d11.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d11.name and vm.domainid == self.account_d11.domainid,
                         True,
                         "ROOT admin is not able to deploy a VM for domain admin user in a shared network with scope=domain with no subdomain access")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_domain_nosubdomainaccess_subdomainuser(self):
        """
         Valiate that ROOT admin is NOT able to deploy a VM for sub domain user in a shared network with scope=domain with no subdomain access

        """
        # Deploy VM as user in a subdomain under  a domain that has shared network with no subdomain access

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmD111A"]["name"] + "-shared-scope-domain-nosubdomainaccess-root-admin"
        self.vmdata["displayname"] = self.acldata["vmD111A"]["displayname"] + "-shared-scope-domain-nosubdomainaccess-root-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id,
                accountid=self.account_d111a.name,
                domainid=self.account_d111a.domainid
            )
            self.fail("ROOT admin is able to deploy a VM for sub domain user in a shared network with scope=domain with no subdomain access")

        except Exception as e:
            self.debug("When a user from a subdomain deploys a VM in a shared network with scope=domain with no subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when ROOT admin tries to deploy a VM for sub domain user in a shared network with scope=domain with no subdomain access ")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_domain_nosubdomainaccess_subdomainadminuser(self):
        """
        Valiate that ROOT admin is NOT able to deploy a VM for sub domain admin user in a shared network with scope=domain with no subdomain access
        """

        # Deploy VM as an admin user in a subdomain under  a domain that has shared network with no subdomain access

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmD111"]["name"] + "-shared-scope-domain-nosubdomainaccess-root-admin"
        self.vmdata["displayname"] = self.acldata["vmD111"]["displayname"] + "-shared-scope-domain-nosubdomainaccess-root-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id,
                accountid=self.account_d111.name,
                domainid=self.account_d111.domainid
            )
            self.fail("ROOT admin is able to deploy a VM for sub domain admin user in a shared network with scope=domain with no subdomain access")
        except Exception as e:
            self.debug("When a admin user from a subdomain deploys a VM in a shared network with scope=domain with no subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when ROOT admin tries to deploy a VM for sub domain admin user in a shared network with scope=domain with no subdomain access")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_domain_nosubdomainaccess_parentdomainuser(self):
        """
        Valiate that ROOT admin is NOT able to deploy a VM for parent domain user in a shared network with scope=domain with no subdomain access

        """
        # Deploy VM as user in parentdomain of a domain that has shared network with no subdomain access

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmD1A"]["name"] + "-shared-scope-domain-nosubdomainaccess-root-admin"
        self.vmdata["displayname"] = self.acldata["vmD1A"]["displayname"] + "-shared-scope-domain-nosubdomainaccess-root-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id,
                accountid=self.account_d1a.name,
                domainid=self.account_d1a.domainid
            )
            self.fail(" ROOT admin is able to deploy a VM for parent domain user in a shared network with scope=domain with no subdomain access")
        except Exception as e:
            self.debug("When a user from parent domain deploys a VM in a shared network with scope=domain with no subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when  ROOT admin tries to deploy a VM for parent domain user in a shared network with scope=domain with no subdomain access")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_domain_nosubdomainaccess_parentdomainadminuser(self):
        """
        Valiate that ROOT admin is NOT able to deploy a VM for parent domain admin user in a shared network with scope=domain with no subdomain access
        """

        # Deploy VM as an admin user in parentdomain of  a domain that has shared network with no subdomain access

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmD1"]["name"] + "-shared-scope-domain-nosubdomainaccess-root-admin"
        self.vmdata["displayname"] = self.acldata["vmD1"]["displayname"] + "-shared-scope-domain-nosubdomainaccess-root-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id,
                accountid=self.account_d1.name,
                domainid=self.account_d1.domainid
            )
            self.fail("ROOT admin is able to deploy a VM for parent domain admin user in a shared network with scope=domain with no subdomain access")
        except Exception as e:
            self.debug("When an admin user from parent domain deploys a VM in a shared network with scope=domain with no subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when ROOT admin tries to deploy a VM for parent domain admin user in a shared network with scope=domain with no subdomain access ")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_domain_nosubdomainaccess_ROOTuser(self):
        """
        Valiate that ROOT admin is NOT able to deploy a VM for parent domain admin user in a shared network with scope=domain with no subdomain access
        """

        # Deploy VM as user in ROOT domain

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmROOTA"]["name"] + "-shared-scope-domain-nosubdomainaccess-root-admin"
        self.vmdata["displayname"] = self.acldata["vmROOTA"]["displayname"] + "-shared-scope-domain-nosubdomainaccess-root-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id,
                accountid=self.account_roota.name,
                domainid=self.account_roota.domainid
            )
            self.fail("ROOT admin is able to deploy a VM for parent domain admin user in a shared network with scope=domain with no subdomain access")
        except Exception as e:
            self.debug("When a regular user from ROOT domain deploys a VM in a shared network with scope=domain with no subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when ROOT admin tries to deploy a VM for parent domain admin user in a shared network with scope=domain with no subdomain access")

    ## Test cases relating to deploying Virtual Machine as ROOT admin for other users in shared network with scope=Domain and with subdomain access

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_domain_withsubdomainaccess_domainuser(self):
        """
        Valiate that ROOT admin is able to deploy a VM for domain user in a shared network with scope=domain with subdomain access
        """

        # Deploy VM as user in a domain that has shared network with subdomain access

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmD11A"]["name"] + "-shared-scope-domain-withsubdomainaccess-root-admin"
        self.vmdata["displayname"] = self.acldata["vmD11A"]["displayname"] + "-shared-scope-domain-withsubdomainaccess-root-admin"

        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_domain_with_subdomain_d11.id,
            accountid=self.account_d11a.name,
            domainid=self.account_d11a.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d11a.name and vm.domainid == self.account_d11a.domainid,
                         True,
                         "ROOT admin is NOT able to deploy a VM for domain user in a shared network with scope=domain with subdomain access")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_domain_withsubdomainaccess_domainadminuser(self):
        """
        Valiate that ROOT admin is able to deploy a VM for domain admin user in a shared network with scope=domain with subdomain access
        """

        # Deploy VM as an admin user in a domain that has shared network with subdomain access

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmD11"]["name"] + "-shared-scope-domain-withsubdomainaccess-root-admin"
        self.vmdata["displayname"] = self.acldata["vmD11"]["displayname"] + "-shared-scope-domain-withsubdomainaccess-root-admin"

        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_domain_with_subdomain_d11.id,
            accountid=self.account_d11.name,
            domainid=self.account_d11.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d11.name and vm.domainid == self.account_d11.domainid,
                         True,
                         "ROOT admin is not able to deploy a VM for domain admin user in a shared network with scope=domain with subdomain access")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_domain_withsubdomainaccess_subdomainuser(self):
        """
        Valiate that ROOT admin is able to deploy a VM for subdomain user in a shared network with scope=domain with subdomain access
        """

        # Deploy VM as user in a subdomain under  a domain that has shared network with subdomain access

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmD111A"]["name"] + "-shared-scope-domain-withsubdomainaccess-root-admin"
        self.vmdata["displayname"] = self.acldata["vmD111A"]["displayname"] + "-shared-scope-domain-withsubdomainaccess-root-admin"
        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_domain_with_subdomain_d11.id,
            accountid=self.account_d111a.name,
            domainid=self.account_d111a.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d111a.name and vm.domainid == self.account_d111a.domainid,
                         True,
                         "ROOT admin is not able to deploy a VM for subdomain user in a shared network with scope=domain with subdomain access")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_domain_withsubdomainaccess_subdomainadminuser(self):
        """
        Valiate that ROOT admin is able to deploy a VM for subdomain admin user in a shared network with scope=domain with subdomain access
        """

        # Deploy VM as an admin user in a subdomain under  a domain that has shared network with subdomain access

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmD111"]["name"] + "-shared-scope-domain-withsubdomainaccess-root-admin"
        self.vmdata["displayname"] = self.acldata["vmD111"]["displayname"] + "-shared-scope-domain-withsubdomainaccess-root-admin"
        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_domain_with_subdomain_d11.id,
            accountid=self.account_d111.name,
            domainid=self.account_d111.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d111.name and vm.domainid == self.account_d111.domainid,
                         True,
                         "ROOT admin is not able to deploy a VM for subdomain admin user in a shared network with scope=domain with subdomain access")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_domain_withsubdomainaccess_parentdomainuser(self):
        """
        Valiate that ROOT admin is NOT able to deploy a VM for parent domain user in a shared network with scope=domain with subdomain access
        """

        # Deploy VM as user in parentdomain of a domain that has shared network with subdomain access

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmD1A"]["name"] + "-shared-scope-domain-withsubdomainaccess-root-admin"
        self.vmdata["displayname"] = self.acldata["vmD1A"]["displayname"] + "-shared-scope-domain-withsubdomainaccess-root-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_with_subdomain_d11.id,
                accountid=self.account_d1a.name,
                domainid=self.account_d1a.domainid
            )
            self.fail("ROOT admin is NOT able to deploy a VM for parent domain user in a shared network with scope=domain with subdomain access")
        except Exception as e:
            self.debug("When a user from parent domain deploys a VM in a shared network with scope=domain with subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when ROOT admin tries to deploy a VM for parent domain user in a shared network with scope=domain with subdomain access ")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_domain_withsubdomainaccess_parentdomainadminuser(self):
        """
        Valiate that ROOT admin is NOT able to deploy a VM for parent domain admin user in a shared network with scope=domain with subdomain access
        """

        # Deploy VM as an admin user in parentdomain of  a domain that has shared network with subdomain access

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmD1"]["name"] + "-shared-scope-domain-withsubdomainaccess-root-admin"
        self.vmdata["displayname"] = self.acldata["vmD1"]["displayname"] + "-shared-scope-domain-withsubdomainaccess-root-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_with_subdomain_d11.id,
                accountid=self.account_d1.name,
                domainid=self.account_d1.domainid
            )
            self.fail("ROOT admin is able to deploy a VM for parent domain admin user in a shared network with scope=domain with subdomain access ")
        except Exception as e:
            self.debug("When an admin user from parent domain deploys a VM in a shared network with scope=domain with  subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when ROOT admin tries to deploy a VM for parent domain admin user in a shared network with scope=domain with subdomain access ")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_domain_withsubdomainaccess_ROOTuser(self):
        """
        Valiate that ROOT admin is NOT able to deploy a VM for user in ROOT domain in a shared network with scope=domain with subdomain access
        """

        # Deploy VM as user in ROOT domain

        self.apiclient.connection.apiKey = self.user_roota_apikey
        self.apiclient.connection.securityKey = self.user_roota_secretkey
        self.vmdata["name"] = self.acldata["vmROOTA"]["name"] + "-shared-scope-domain-withsubdomainaccess-root-admin"
        self.vmdata["displayname"] = self.acldata["vmROOTA"]["displayname"] + "-shared-scope-domain-withsubdomainaccess-root-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_with_subdomain_d11.id,
                accountid=self.account_roota.name,
                domainid=self.account_roota.domainid
            )
            self.fail("ROOT admin is able to deploy a VM for user in ROOT domain in a shared network with scope=domain with subdomain access")
        except Exception as e:
            self.debug("When a user from ROOT domain deploys a VM in a shared network with scope=domain with subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when ROOT admin tries to deploy a VM for user in ROOT domain in a shared network with scope=domain with subdomain access")

    ## Test cases relating to deploying Virtual Machine as ROOT admin for other users in shared network with scope=account

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_account_domainuser(self):
        """
        Valiate that ROOT admin is NOT able to deploy a VM for user in the same domain but in a different account in a shared network with scope=account
        """

        # Deploy VM as user in a domain under the same domain but different account from the account that has a shared network with scope=account

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmD111B"]["name"] + "-shared-scope-domain-withsubdomainaccess-root-admin"
        self.vmdata["displayname"] = self.acldata["vmD111B"]["displayname"] + "-shared-scope-domain-withsubdomainaccess-root-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_account_d111a.id,
                accountid=self.account_d111b.name,
                domainid=self.account_d111b.domainid
            )
            self.fail("ROOT admin is able to deploy a VM for user in the same domain but in a different account in a shared network with scope=account")
        except Exception as e:
            self.debug("When a user from same domain but different account deploys a VM in a shared network with scope=account %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.UNABLE_TO_USE_NETWORK):
                self.fail(
                    "Error message validation failed when ROOT admin tries to deploy a VM for user in the same domain but in a different account in a shared network with scope=account")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_account_domainadminuser(self):
        """
        Valiate that ROOT admin is NOT able to deploy a VM for admin user in the same domain but in a different account in a shared network with scope=account

        """
        # Deploy VM as admin user for a domain that has an account with shared network with scope=account

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmD111"]["name"] + "-shared-scope-domain-withsubdomainaccess-root-admin"
        self.vmdata["displayname"] = self.acldata["vmD111"]["displayname"] + "-shared-scope-domain-withsubdomainaccess-root-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_account_d111a.id,
                accountid=self.account_d111.name,
                domainid=self.account_d111.domainid
            )
            self.fail("ROOT admin is able to deploy a VM for admin user in the same domain but in a different account in a shared network with scope=account")
        except Exception as e:
            self.debug("When a user from same domain but different account deploys a VM in a shared network with scope=account %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.UNABLE_TO_USE_NETWORK):
                self.fail(
                    "Error message validation failed when ROOT admin tries to deploy a VM for admin user in the same domain but in a different account in a shared network with scope=account")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_account_user(self):
        """
        Valiate that ROOT admin is able to deploy a VM for regular user in a shared network with scope=account
        """

        # Deploy VM as account with shared network with scope=account

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmD111A"]["name"] + "-shared-scope-domain-withsubdomainaccess-root-admin"
        self.vmdata["displayname"] = self.acldata["vmD111A"]["displayname"] + "-shared-scope-domain-withsubdomainaccess-root-admin"

        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_account_d111a.id,
            accountid=self.account_d111a.name,
            domainid=self.account_d111a.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d111a.name and vm.domainid == self.account_d111a.domainid,
                         True,
                         "ROOT admin is not able to deploy a VM for regular user in a shared network with scope=account")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_account_differentdomain(self):
        """
        Valiate that ROOT admin is NOT able to deploy a VM for a admin user in a shared network with scope=account which the admin user does not have access to
        """

        # Deploy VM as an admin user in a subdomain under ROOT 

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmD2A"]["name"] + "-shared-scope-account-root-admin"
        self.vmdata["displayname"] = self.acldata["vmD2A"]["displayname"] + "-shared-scope-account-root-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_account_d111a.id,
                accountid=self.account_d2a.name,
                domainid=self.account_d2a.domainid
            )
            self.fail("ROOT admin is able to deploy a VM for a admin user in a shared network with scope=account which the admin user does not have access to")
        except Exception as e:
            self.debug("account %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.UNABLE_TO_USE_NETWORK):
                self.fail(
                    "Error message validation failed when ROOT admin tries to deploy a VM for a admin user in a shared network with scope=account which the admin user does not have access to ")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_account_ROOTuser(self):
        """
        Valiate that ROOT admin is NOT able to deploy a VM for a user in ROOT domain in a shared network with scope=account which the user does not have access to
        """

        # Deploy VM as user in ROOT domain

        self.apiclient.connection.apiKey = self.default_apikey
        self.apiclient.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = self.acldata["vmROOTA"]["name"] + "-shared-scope-account-root-admin"
        self.vmdata["displayname"] = self.acldata["vmROOTA"]["displayname"] + "-shared-scope-account-root-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_account_d111a.id,
                accountid=self.account_roota.name,
                domainid=self.account_roota.domainid
            )
            self.fail("ROOT admin is able to deploy a VM for a user in ROOT domain in a shared network with scope=account which the user does not have access to")
        except Exception as e:
            self.debug("When a user from ROOT domain deploys a VM in a shared network with scope=account %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.UNABLE_TO_USE_NETWORK):
                self.fail(
                    "Error message validation failed when ROOT admin tries to deploy a VM for a user in ROOT domain in a shared network with scope=account which the user does not have access to ")

    ## Test cases relating to deploying Virtual Machine as Domain admin for other users in shared network with scope=all

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_all_domainuser(self):
        """
        Valiate that Domain admin is able to deploy a VM for a domain user in a shared network with scope=all
        """

        # Deploy VM for a user in a domain under ROOT as admin
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD1A"]["name"] + "-shared-scope-all-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD1A"]["displayname"] + "-shared-scope-all-domain-admin"

        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_all.id,
            accountid=self.account_d1a.name,
            domainid=self.account_d1a.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d1a.name and vm.domainid == self.account_d1a.domainid,
                         True,
                         "Domain admin is not able to deploy a VM for a domain user in a shared network with scope=all")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_all_domainadminuser(self):
        """
        Valiate that Domain admin is able to deploy a VM for a domain admin user in a shared network with scope=all
        """

        # Deploy VM for an admin user in a domain under ROOT as admin
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD1"]["name"] + "-shared-scope-all-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD1"]["displayname"] + "-shared-scope-all-domain-admin"

        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_all.id,
            accountid=self.account_d1.name,
            domainid=self.account_d1.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d1.name and vm.domainid == self.account_d1.domainid,
                         True,
                         "Domain admin is not able to deploy a VM for a domain admin user in a shared network with scope=all")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_all_subdomainuser(self):
        """
        Valiate that Domain admin is able to deploy a VM for a sub domain user in a shared network with scope=all
        """

        # Deploy VM as user in a subdomain under ROOT 
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD11A"]["name"] + "-shared-scope-all-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD11A"]["displayname"] + "-shared-scope-all-domain-admin"
        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_all.id,
            accountid=self.account_d11a.name,
            domainid=self.account_d11a.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d11a.name and vm.domainid == self.account_d11a.domainid,
                         True,
                         "Domain admin is not able to deploy a VM for a sub domain user in a shared network with scope=all")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_all_subdomainadminuser(self):
        """
        Valiate that Domain admin is able to deploy a VM for a sub domain admin user in a shared network with scope=all
        """

        # Deploy VM as an admin user in a subdomain under ROOT 
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD11"]["name"] + "-shared-scope-all-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD11"]["displayname"] + "-shared-scope-all-domain-admin"
        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_all.id,
            accountid=self.account_d11.name,
            domainid=self.account_d11.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d11.name and vm.domainid == self.account_d11.domainid,
                         True,
                         "Domain admin is not able to deploy a VM for a sub domain admin user in a shared network with scope=all")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_all_ROOTuser(self):
        """
        Valiate that Domain admin is NOT able to deploy a VM for user in ROOT domain in a shared network with scope=all
        """

        # Deploy VM as user in ROOT domain
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmROOTA"]["name"] + "-shared-scope-all"
        self.vmdata["displayname"] = self.acldata["vmROOTA"]["displayname"] + "-shared-scope-all"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_all.id,
                accountid=self.account_roota.name,
                domainid=self.account_roota.domainid
            )
            self.fail("Domain admin is NOT able to deploy a VM for user in ROOT domain in a shared network with scope=all")
        except Exception as e:
            self.debug("When a Domain admin user deploys a VM for ROOT user in a shared network with scope=all %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NO_PERMISSION_TO_OPERATE_DOMAIN):
                self.fail("Error message validation failed when Domain admin is NOT able to deploy a VM for user in ROOT domain in a shared network with scope=all")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_all_crossdomainuser(self):
        """
        Valiate that Domain admin is NOT able to deploy a VM for user in other domain in a shared network with scope=all
        """

        # Deploy VM as user in ROOT domain
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmROOTA"]["name"] + "-shared-scope-all"
        self.vmdata["displayname"] = self.acldata["vmROOTA"]["displayname"] + "-shared-scope-all"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_all.id,
                accountid=self.account_d2a.name,
                domainid=self.account_d2a.domainid
            )
            self.fail("Domain admin user is able to Deploy VM for a domain user he does not have access to in a shared network with scope=domain with no subdomain access ")
        except Exception as e:
            self.debug("When a Domain admin user  deploys a VM for a domain user he does not have access to in a shared network with scope=domain with no subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NO_PERMISSION_TO_OPERATE_DOMAIN):
                self.fail(
                    "Error mesage validation failed when Domain admin user tries to Deploy VM for a domain user he does not have access to in a shared network with scope=domain with no subdomain access ")

    ## Test cases relating to deploying Virtual Machine as Domain admin for other users in shared network with scope=Domain and no subdomain access

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_domain_nosubdomainaccess_domainuser(self):
        """
        Valiate that Domain admin is able to deploy a VM for domain user in a shared network with scope=Domain and no subdomain access
        """

        # Deploy VM as user in a domain that has shared network with no subdomain access
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD11A"]["name"] + "-shared-scope-domain-nosubdomainaccess-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD11A"]["displayname"] + "-shared-scope-domain-nosubdomainaccess-domain-admin"

        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_domain_d11.id,
            accountid=self.account_d11a.name,
            domainid=self.account_d11a.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d11a.name and vm.domainid == self.account_d11a.domainid,
                         True,
                         "Domain admin is not able to deploy a VM for domain user in a shared network with scope=Domain and no subdomain access")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_domain_nosubdomainaccess_domainadminuser(self):
        """
        Valiate that Domain admin is able to deploy a VM for domain admin user in a shared network with scope=Domain and no subdomain access

        """
        # Deploy VM as an admin user in a domain that has shared network with no subdomain access

        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD11"]["name"] + "-shared-scope-domain-nosubdomainaccess-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD11"]["displayname"] + "-shared-scope-domain-nosubdomainaccess-domain-admin"

        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_domain_d11.id,
            accountid=self.account_d11.name,
            domainid=self.account_d11.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d11.name and vm.domainid == self.account_d11.domainid,
                         True,
                         "Admin User in a  domain that has a shared network with no subdomain access failed to Deploy VM in a shared network with scope=domain with no subdomain access")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_domain_nosubdomainaccess_subdomainuser(self):
        """
        Valiate that Domain admin is NOT able to deploy a VM for sub domain user in a shared network with scope=Domain and no subdomain access
        """

        # Deploy VM as user in a subdomain under  a domain that has shared network with no subdomain access

        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD111A"]["name"] + "-shared-scope-domain-nosubdomainaccess-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD111A"]["displayname"] + "-shared-scope-domain-nosubdomainaccess-domain-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id,
                accountid=self.account_d111a.name,
                domainid=self.account_d111a.domainid
            )
            self.fail("Domain admin is able to deploy a VM for sub domain user in a shared network with scope=Domain and no subdomain access")
        except Exception as e:
            self.debug("When a user from a subdomain deploys a VM in a shared network with scope=domain with no subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when Domain admin tries to deploy a VM for sub domain user in a shared network with scope=Domain and no subdomain access")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_domain_nosubdomainaccess_subdomainadminuser(self):
        """
        Valiate that Domain admin is NOT able to deploy a VM for sub domain admin user in a shared network with scope=Domain and no subdomain access
        """

        # Deploy VM as an admin user in a subdomain under  a domain that has shared network with no subdomain access

        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD111"]["name"] + "-shared-scope-domain-nosubdomainaccess-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD111"]["displayname"] + "-shared-scope-domain-nosubdomainaccess-domain-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id,
                accountid=self.account_d111.name,
                domainid=self.account_d111.domainid
            )
            self.fail("Domain admin is able to deploy a VM for sub domain admin user in a shared network with scope=Domain and no subdomain access")
        except Exception as e:
            self.debug("When a admin user from a subdomain deploys a VM in a shared network with scope=domain with no subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when Domain admin tries to deploy a VM for sub domain admin user in a shared network with scope=Domain and no subdomain access ")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_domain_nosubdomainaccess_parentdomainuser(self):
        """
        Valiate that Domain admin is NOT able to deploy a VM for parent domain user in a shared network with scope=Domain and no subdomain access

        """
        # Deploy VM as user in parentdomain of a domain that has shared network with no subdomain access

        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD1A"]["name"] + "-shared-scope-domain-nosubdomainaccess-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD1A"]["displayname"] + "-shared-scope-domain-nosubdomainaccess-domain-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id,
                accountid=self.account_d1a.name,
                domainid=self.account_d1a.domainid
            )
            self.fail("Domain admin is able to deploy a VM for parent domain user in a shared network with scope=Domain and no subdomain access")
        except Exception as e:
            self.debug("When a user from parent domain deploys a VM in a shared network with scope=domain with no subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when Domain admin tries to deploy a VM for parent domain user in a shared network with scope=Domain and no subdomain access ")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_domain_nosubdomainaccess_parentdomainadminuser(self):
        """
        Valiate that Domain admin is NOT able to deploy a VM for parent domain admin user in a shared network with scope=Domain and no subdomain access
        """

        # Deploy VM as an admin user in parentdomain of  a domain that has shared network with no subdomain access

        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD1"]["name"] + "-shared-scope-domain-nosubdomainaccess-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD1"]["displayname"] + "-shared-scope-domain-nosubdomainaccess-domain-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id,
                accountid=self.account_d1.name,
                domainid=self.account_d1.domainid
            )
            self.fail("Domain admin is able to deploy a VM for parent domain admin user in a shared network with scope=Domain and no subdomain access")
        except Exception as e:
            self.debug("When an admin user from parent domain deploys a VM in a shared network with scope=domain with no subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when Domain admin tries to deploy a VM for parent domain admin user in a shared network with scope=Domain and no subdomain access ")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_domain_nosubdomainaccess_ROOTuser(self):
        """
        Valiate that Domain admin is NOT able to deploy a VM for user in ROOT domain in a shared network with scope=Domain and no subdomain access
        """

        # Deploy VM as user in ROOT domain
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmROOTA"]["name"] + "-shared-scope-domain-nosubdomainaccess-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmROOTA"]["displayname"] + "-shared-scope-domain-nosubdomainaccess-domain-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id,
                accountid=self.account_roota.name,
                domainid=self.account_roota.domainid
            )
            self.fail("Domain admin is able to deploy a VM for user in ROOT domain in a shared network with scope=Domain and no subdomain access")
        except Exception as e:
            self.debug("When a regular user from ROOT domain deploys a VM in a shared network with scope=domain with no subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NO_PERMISSION_TO_OPERATE_DOMAIN):
                self.fail(
                    "Error message validation failed when Domain admin tries to deploy a VM for user in ROOT domain in a shared network with scope=Domain and no subdomain access")

    ## Test cases relating to deploying Virtual Machine as Domain admin for other users in shared network with scope=Domain and with subdomain access

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_domain_withsubdomainaccess_domainuser(self):
        """
        Valiate that Domain admin is able to deploy a VM for regular user in domain in a shared network with scope=Domain and subdomain access
        """

        # Deploy VM as user in a domain that has shared network with subdomain access
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD11A"]["name"] + "-shared-scope-domain-withsubdomainaccess-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD11A"]["displayname"] + "-shared-scope-domain-withsubdomainaccess-domain-admin"

        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_domain_with_subdomain_d11.id,
            accountid=self.account_d11a.name,
            domainid=self.account_d11a.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d11a.name and vm.domainid == self.account_d11a.domainid,
                         True,
                         "Domain admin is not able to deploy a VM for regular user in domain in a shared network with scope=Domain and subdomain access")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_domain_withsubdomainaccess_domainadminuser(self):
        """
        Valiate that Domain admin is able to deploy a VM for admin user in domain in a shared network with scope=Domain and subdomain access
        """

        # Deploy VM as an admin user in a domain that has shared network with subdomain access
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD11"]["name"] + "-shared-scope-domain-withsubdomainaccess-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD11"]["displayname"] + "-shared-scope-domain-withsubdomainaccess-domain-admin"

        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_domain_with_subdomain_d11.id,
            accountid=self.account_d11.name,
            domainid=self.account_d11.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d11.name and vm.domainid == self.account_d11.domainid,
                         True,
                         "Domain admin is not able to deploy a VM for admin user in domain in a shared network with scope=Domain and subdomain access")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_domain_withsubdomainaccess_subdomainuser(self):
        """
        Valiate that Domain admin is able to deploy a VM for regular user in subdomain in a shared network with scope=Domain and subdomain access
        """

        # Deploy VM as user in a subdomain under  a domain that has shared network with subdomain access
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD111A"]["name"] + "-shared-scope-domain-withsubdomainaccess-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD111A"]["displayname"] + "-shared-scope-domain-withsubdomainaccess-domain-admin"
        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_domain_with_subdomain_d11.id,
            accountid=self.account_d111a.name,
            domainid=self.account_d111a.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d111a.name and vm.domainid == self.account_d111a.domainid,
                         True,
                         "Domain admin is not able to deploy a VM for regular user in subdomain in a shared network with scope=Domain and subdomain access")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_domain_withsubdomainaccess_subdomainadminuser(self):
        """
        Valiate that Domain admin is able to deploy a VM for admin user in subdomain in a shared network with scope=Domain and subdomain access
        """

        # Deploy VM as an admin user in a subdomain under  a domain that has shared network with subdomain access
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD111"]["name"] + "-shared-scope-domain-withsubdomainaccess-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD111"]["displayname"] + "-shared-scope-domain-withsubdomainaccess-domain-admin"
        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_domain_with_subdomain_d11.id,
            accountid=self.account_d111.name,
            domainid=self.account_d111.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d111.name and vm.domainid == self.account_d111.domainid,
                         True,
                         "Domain admin is not able to deploy a VM for admin user in subdomain in a shared network with scope=Domain and subdomain access")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_domain_withsubdomainaccess_parentdomainuser(self):
        """
        Valiate that Domain admin is NOT able to deploy a VM for regular user in parent domain in a shared network with scope=Domain and subdomain access
        """

        # Deploy VM as user in parentdomain of a domain that has shared network with subdomain access
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD1A"]["name"] + "-shared-scope-domain-withsubdomainaccess-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD1A"]["displayname"] + "-shared-scope-domain-withsubdomainaccess-domain-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_with_subdomain_d11.id,
                accountid=self.account_d1a.name,
                domainid=self.account_d1a.domainid
            )
            self.fail(" Domain admin is able to deploy a VM for regular user in parent domain in a shared network with scope=Domain and subdomain access")
        except Exception as e:
            self.debug("When a user from parent domain deploys a VM in a shared network with scope=domain with subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when Domain admin tries to deploy a VM for regular user in parent domain in a shared network with scope=Domain and subdomain access")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_domain_withsubdomainaccess_parentdomainadminuser(self):
        """
        Valiate that Domain admin is NOT able to deploy a VM for admin user in parent domain in a shared network with scope=Domain and subdomain access
        """

        # Deploy VM as an admin user in parentdomain of  a domain that has shared network with subdomain access

        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD1"]["name"] + "-shared-scope-domain-withsubdomainaccess-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD1"]["displayname"] + "-shared-scope-domain-withsubdomainaccess-domain-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_with_subdomain_d11.id,
                accountid=self.account_d1.name,
                domainid=self.account_d1.domainid
            )
            self.fail("Domain admin is able to deploy a VM for admin user in parent domain in a shared network with scope=Domain and subdomain access")
        except Exception as e:
            self.debug("When an admin user from parent domain deploys a VM in a shared network with scope=domain with  subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when Domain admin tries to deploy a VM for admin user in parent domain in a shared network with scope=Domain and subdomain access")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_domain_withsubdomainaccess_ROOTuser(self):
        """
        Valiate that Domain admin is NOT able to deploy a VM for user in ROOT domain in a shared network with scope=Domain and subdomain access
        """

        # Deploy VM as user in ROOT domain
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmROOTA"]["name"] + "-shared-scope-domain-withsubdomainaccess-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmROOTA"]["displayname"] + "-shared-scope-domain-withsubdomainaccess-domain-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_with_subdomain_d11.id,
                accountid=self.account_roota.name,
                domainid=self.account_roota.domainid
            )
            self.fail("Domain admin is able to deploy a VM for user in ROOT domain in a shared network with scope=Domain and subdomain access")
        except Exception as e:
            self.debug("When a user from ROOT domain deploys a VM in a shared network with scope=domain with subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NO_PERMISSION_TO_OPERATE_DOMAIN):
                self.fail(
                    "Error message validation failed when Domain admin tries to deploy a VM for user in ROOT domain in a shared network with scope=Domain and subdomain access")

    ## Test cases relating to deploying Virtual Machine as Domain admin for other users in shared network with scope=account

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_account_domainuser(self):
        """
        Valiate that Domain admin is NOT able to deploy a VM for user in the same domain but belonging to a different account in a shared network with scope=account
        """

        # Deploy VM as user in a domain under the same domain but different account from the acount that has a shared network with scope=account
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD111B"]["name"] + "-shared-scope-domain-withsubdomainaccess-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD111B"]["displayname"] + "-shared-scope-domain-withsubdomainaccess-domain-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_account_d111a.id,
                accountid=self.account_d111b.name,
                domainid=self.account_d111b.domainid
            )
            self.fail("Domain admin is able to deploy a VM for user in the same domain but belonging to a different account in a shared network with scope=account")
        except Exception as e:
            self.debug("When a user from same domain but different account deploys a VM in a shared network with scope=account %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.UNABLE_TO_USE_NETWORK):
                self.fail(
                    "Error message validation failed when Domain admin tries to deploy a VM for user in the same domain but belonging to a different account in a shared network with scope=account")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_account_domainadminuser(self):
        """
        Valiate that Domain admin is NOT able to deploy a VM for an admin user in the same domain but belonging to a different account in a shared network with scope=account
        """

        # Deploy VM as admin user for a domain that has an account with shared network with scope=account
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD111"]["name"] + "-shared-scope-domain-withsubdomainaccess-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD111"]["displayname"] + "-shared-scope-domain-withsubdomainaccess-domain-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_account_d111a.id,
                accountid=self.account_d111.name,
                domainid=self.account_d111.domainid
            )
            self.fail("Domain admin is able to deploy a VM for user in the same domain but belonging to a different account in a shared network with scope=account")
        except Exception as e:
            self.debug("When a user from same domain but different account deploys a VM in a shared network with scope=account %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.UNABLE_TO_USE_NETWORK):
                self.fail(
                    "Error message validation failed when Domain admin tries to deploy a VM for user in the same domain but belonging to a different account in a shared network with scope=account")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_account_user(self):
        """
        Valiate that Domain admin is able to deploy a VM for an regular user in a shared network with scope=account
        """

        # Deploy VM as account with shared network with scope=account
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD111A"]["name"] + "-shared-scope-domain-withsubdomainaccess-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD111A"]["displayname"] + "-shared-scope-domain-withsubdomainaccess-domain-admin"

        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_account_d111a.id,
            accountid=self.account_d111a.name,
            domainid=self.account_d111a.domainid
        )

        self.assertEqual(vm.state == "Running" and vm.account == self.account_d111a.name and vm.domainid == self.account_d111a.domainid,
                         True,
                         "Domain admin is not able to deploy a VM for an regular user in a shared network with scope=account")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_account_differentdomain(self):
        """
        Valiate that Domain admin is able NOT able to deploy a VM for an regular user from a differnt domain in a shared network with scope=account
        """

        # Deploy VM as an admin user in a subdomain under ROOT 
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD2A"]["name"] + "-shared-scope-account-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD2A"]["displayname"] + "-shared-scope-account-domain-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_account_d111a.id,
                accountid=self.account_d2a.name,
                domainid=self.account_d2a.domainid
            )
            self.fail("Domain admin is able able to deploy a VM for an regular user from a differnt domain in a shared network with scope=account")
        except Exception as e:
            self.debug("When a user from different domain deploys a VM in a shared network with scope=account %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NO_PERMISSION_TO_OPERATE_DOMAIN):
                self.fail(
                    "Error message validation failed when Domain admin tries to deploy a VM for an regular user from a differnt domain in a shared network with scope=account")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_account_ROOTuser(self):
        """
        Valiate that Domain admin is NOT able to deploy a VM for an regular user in ROOT domain in a shared network with scope=account
        """

        # Deploy VM as user in ROOT domain
        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmROOTA"]["name"] + "-shared-scope-account-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmROOTA"]["displayname"] + "-shared-scope-account-domain-admin"
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_account_d111a.id,
                accountid=self.account_roota.name,
                domainid=self.account_roota.domainid
            )
            self.fail("Domain admin is able to deploy a VM for an regular user in ROOT domain in a shared network with scope=account")
        except Exception as e:
            self.debug("When a user from ROOT domain deploys a VM in a shared network with scope=account %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NO_PERMISSION_TO_OPERATE_DOMAIN):
                self.fail("Error message validation failed when Domain admin tries to deploy a VM for an regular user in ROOT domain in a shared network with scope=account")

    ## Test cases relating to deploying Virtual Machine as Regular user for other users in shared network with scope=all

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_regularuser_scope_all_anotherusersamedomain(self):
        """
        Valiate that regular user is able NOT able to deploy a VM for another user in the same  domain in a shared network with scope=all
        """

        # Deploy VM for a user in a domain under ROOT as admin
        self.apiclient.connection.apiKey = self.user_d11a_apikey
        self.apiclient.connection.securityKey = self.user_d11a_secretkey
        self.vmdata["name"] = self.acldata["vmD11A"]["name"] + "-shared-scope-all-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD11A"]["displayname"] + "-shared-scope-all-domain-admin"
        try:
            vm_d1a = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_all.id,
                accountid=self.account_d12a.name,
                domainid=self.account_d12a.domainid
            )
            self.fail("Regular user is allowed to deploy a VM for another user in the same domain in a shared network with scope=all")
        except Exception as e:
            self.debug("When a regular user deploys a VM for another user in the same domain in a shared network with scope=all %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NO_PERMISSION_TO_OPERATE_ACCOUNT):
                self.fail("Error message validation failed when Regular user tries to deploy a VM for another user in the same domain in a shared network with scope=all")

    @attr("simulator_only", tags=["advanced"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_regularuser_scope_all_crossdomain(self):
        """
        Valiate that regular user is able NOT able to deploy a VM for another user in a different domain in a shared network with scope=all
        """

        # Deploy VM for a user in a domain under ROOT as admin
        self.apiclient.connection.apiKey = self.user_d11a_apikey
        self.apiclient.connection.securityKey = self.user_d11a_secretkey
        self.vmdata["name"] = self.acldata["vmD11A"]["name"] + "-shared-scope-all-domain-admin"
        self.vmdata["displayname"] = self.acldata["vmD11A"]["displayname"] + "-shared-scope-all-domain-admin"
        try:
            vm_d1a = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_all.id,
                accountid=self.account_d2a.name,
                domainid=self.account_d2a.domainid
            )
            self.fail("Regular user is allowed to deploy a VM for another user in the same domain in a shared network with scope=all")
        except Exception as e:
            self.debug("When a regular user deploys a VM for another user in the same domain in a shared network with scope=all %s" % e)
            if not CloudstackAclException.verifyMsginException(e, CloudstackAclException.NO_PERMISSION_TO_OPERATE_ACCOUNT):
                self.fail("Error message validation failed when Regular user tries to deploy a VM for another user in the same domain in a shared network with scope=all")

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
