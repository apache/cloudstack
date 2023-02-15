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
 Test cases for access control for Vm deployment in Shared Network with scope all,account and Domain

"""
#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from marvin.cloudstackException import CloudstackAclException
from nose.plugins.attrib import attr
#Import System modules
import time

_multiprocess_shared_ = True

class TestSharedNetwork(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        """
        Create the following domain tree and accounts that are reqiured for executing  access control test cases relating to deploying VM in shared network with different scopes:

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
        cls.testclient = super(TestSharedNetwork, cls).getClsTestClient()
        cls.apiclient =  cls.testclient.getApiClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        cls.acldata = cls.testdata["acl"]
        cls.domain_1 = None
        cls.domain_2 = None
        cls.cleanup = []


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

            user = cls.generateKeysForUser(cls.apiclient,cls.account_d1)
            cls.user_d1_apikey = user.apikey
            cls.user_d1_secretkey = user.secretkey

            cls.account_d1a = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD1A"],
                                admin=False,
                                domainid=cls.domain_1.id
                                )
            user = cls.generateKeysForUser(cls.apiclient,cls.account_d1a)
            cls.user_d1a_apikey = user.apikey
            cls.user_d1a_secretkey = user.secretkey


            cls.account_d1b = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD1B"],
                                admin=False,
                                domainid=cls.domain_1.id
                                )

            user = cls.generateKeysForUser(cls.apiclient,cls.account_d1b)
            cls.user_d1b_apikey = user.apikey
            cls.user_d1b_secretkey = user.secretkey

            # Create  1 admin and 2 user accounts for doamin_11
            cls.account_d11 = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD11"],
                                admin=True,
                                domainid=cls.domain_11.id
                                )
            user = cls.generateKeysForUser(cls.apiclient,cls.account_d11)
            cls.user_d11_apikey = user.apikey
            cls.user_d11_secretkey = user.secretkey

            cls.account_d11a = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD11A"],
                                admin=False,
                                domainid=cls.domain_11.id
                                )
            user = cls.generateKeysForUser(cls.apiclient,cls.account_d11a)
            cls.user_d11a_apikey = user.apikey
            cls.user_d11a_secretkey = user.secretkey

            cls.account_d11b = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD11B"],
                                admin=False,
                                domainid=cls.domain_11.id
                                )
            user = cls.generateKeysForUser(cls.apiclient,cls.account_d11b)
            cls.user_d11b_apikey = user.apikey
            cls.user_d11b_secretkey = user.secretkey

            # Create  2 user accounts and 1 admin account for doamin_111

            cls.account_d111 = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD111"],
                                admin=True,
                                domainid=cls.domain_111.id
                                )
            user = cls.generateKeysForUser(cls.apiclient,cls.account_d111)
            cls.user_d111_apikey = user.apikey
            cls.user_d111_secretkey = user.secretkey

            cls.account_d111a = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD111A"],
                                admin=False,
                                domainid=cls.domain_111.id
                                )
            user = cls.generateKeysForUser(cls.apiclient,cls.account_d111a)
            cls.user_d111a_apikey = user.apikey
            cls.user_d111a_secretkey = user.secretkey

            cls.account_d111b = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD111B"],
                                admin=False,
                                domainid=cls.domain_111.id
                                )
            user = cls.generateKeysForUser(cls.apiclient,cls.account_d111b)
            cls.user_d111b_apikey = user.apikey
            cls.user_d111b_secretkey = user.secretkey

            # Create  2 user accounts for doamin_12
            cls.account_d12a = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD12A"],
                                admin=False,
                                domainid=cls.domain_12.id
                                )
            user = cls.generateKeysForUser(cls.apiclient,cls.account_d12a)
            cls.user_d12a_apikey = user.apikey
            cls.user_d12a_secretkey = user.secretkey

            cls.account_d12b = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD12B"],
                                admin=False,
                                domainid=cls.domain_12.id
                                )

            user = cls.generateKeysForUser(cls.apiclient,cls.account_d12b)
            cls.user_d12b_apikey = user.apikey
            cls.user_d12b_secretkey = user.secretkey

            # Create 1 user account for domain_2

            cls.account_d2a = Account.create(
                                cls.apiclient,
                                cls.acldata["accountD2"],
                                admin=False,
                                domainid=cls.domain_2.id
                                )

            user = cls.generateKeysForUser(cls.apiclient,cls.account_d2a)
            cls.user_d2a_apikey = user.apikey
            cls.user_d2a_secretkey = user.secretkey


            # Create 1 user account and admin account in "ROOT" domain

            cls.account_roota = Account.create(
                                cls.apiclient,
                                cls.acldata["accountROOTA"],
                                admin=False,
                                )

            user = cls.generateKeysForUser(cls.apiclient,cls.account_roota)
            cls.user_roota_apikey = user.apikey
            cls.user_roota_secretkey = user.secretkey

            cls.account_root = Account.create(
                                cls.apiclient,
                                cls.acldata["accountROOTA"],
                                admin=True,
                                )

            user = cls.generateKeysForUser(cls.apiclient,cls.account_root)
            cls.user_root_apikey = user.apikey
            cls.user_root_secretkey = user.secretkey

            # create service offering
            cls.service_offering = ServiceOffering.create(
                                    cls.apiclient,
                                    cls.acldata["service_offering"]["small"]
                                    )

            cls.zone = get_zone(cls.apiclient,cls.testclient.getZoneForTests())
            cls.acldata['mode'] = cls.zone.networktype
            cls.template = get_template(cls.apiclient, cls.zone.id, cls.acldata["ostype"])

            cls.apiclient.connection.apiKey = cls.default_apikey
            cls.apiclient.connection.securityKey = cls.default_secretkey

            list_shared_network_offerings_response = NetworkOffering.list(
                                                             cls.apiclient,
                                                             name="DefaultSharedNetworkOffering",
                                                             displayText="Offering for Shared networks"
                                                             )

            cls.shared_network_offering_id = list_shared_network_offerings_response[0].id

            #Override vlan parameter so that there is no overlap with vlans being used in other shared network impersonation test suite
            cls.acldata["network_all"]["vlan"]="3001"
            cls.acldata["network_domain_with_no_subdomain_access"]["vlan"]="3002"
            cls.acldata["network_domain_with_subdomain_access"]["vlan"]="3003"
            cls.acldata["network_account"]["vlan"]="3004"

            cls.shared_network_all = Network.create(
                             cls.apiclient,
                             cls.acldata["network_all"],
                             networkofferingid=cls.shared_network_offering_id,
                             zoneid=cls.zone.id
                             )

            cls.shared_network_domain_d11 =  Network.create(
                             cls.apiclient,
                             cls.acldata["network_domain_with_no_subdomain_access"],
                             networkofferingid=cls.shared_network_offering_id,
                             zoneid=cls.zone.id,
                             domainid=cls.domain_11.id,
                             subdomainaccess=False
                             )

            cls.shared_network_domain_with_subdomain_d11 =  Network.create(
                             cls.apiclient,
                             cls.acldata["network_domain_with_subdomain_access"],
                             networkofferingid=cls.shared_network_offering_id,
                             zoneid=cls.zone.id,
                             domainid=cls.domain_11.id,
                            subdomainaccess=True
                             )

            cls.shared_network_account_d111a =  Network.create(
                             cls.apiclient,
                             cls.acldata["network_account"],
                             networkofferingid=cls.shared_network_offering_id,
                             zoneid=cls.zone.id,
                             domainid=cls.domain_111.id,
                             accountid=cls.account_d111a.user[0].username
                        )
            cls.vmdata = {"name": "test",
                          "displayname" : "test"
                          }
            cls.cleanup = [
                            cls.account_root,
                            cls.account_roota,
                            cls.shared_network_all,
                            cls.service_offering,
                            ]
        except Exception as e:
                cls.domain_1.delete(cls.apiclient,cleanup="true")
                cls.domain_2.delete(cls.apiclient,cleanup="true")
                cleanup_resources(cls.apiclient, cls.cleanup)
                raise Exception("Failed to create the setup required to execute the test cases: %s" % e)

    @classmethod
    def tearDownClass(cls):
        cls.apiclient = super(TestSharedNetwork, cls).getClsTestClient().getApiClient()
        cls.apiclient.connection.apiKey = cls.default_apikey
        cls.apiclient.connection.securityKey = cls.default_secretkey
        cls.domain_1.delete(cls.apiclient,cleanup="true")
        cls.domain_2.delete(cls.apiclient,cleanup="true")
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

## Test cases relating to deploying Virtual Machine in shared network with scope=all

    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_all_domainuser(self):
        """
        Validate that regular user in a domain is allowed to deploy VM in a shared network created with scope="all"
        """
        # deploy VM as user in a domain under ROOT

        self.apiclient.connection.apiKey = self.user_d1a_apikey
        self.apiclient.connection.securityKey = self.user_d1a_secretkey
        self.vmdata["name"] = self.acldata["vmD1A"]["name"] +"-shared-scope-all"
        self.vmdata["displayname"] = self.acldata["vmD1A"]["displayname"] +"-shared-scope-all"

        vm_d1a = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_all.id
        )

        self.assertEqual(vm_d1a.state == "Running",
                    True,
                    "User in a domain under ROOT failed to deploy VM in a shared network with scope=all")


    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_all_domainadminuser(self):
        """
        Validate that regular user in "ROOT" domain is allowed to deploy VM in a shared network created with scope="all"

        """
        # deploy VM as  an admin user in a domain under ROOT

        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD1"]["name"] +"-shared-scope-all"
        self.vmdata["displayname"] = self.acldata["vmD1"]["displayname"] +"-shared-scope-all"

        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_all.id
        )

        self.assertEqual(vm.state == "Running",
                    True,
                    "Admin User in a domain under ROOT failed to deploy VM in a shared network with scope=all")


    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_all_subdomainuser(self):
        """
        Validate that regular user in any subdomain is allowed to deploy VM in a shared network created with scope="all"
        """

        # deploy VM as user in a subdomain under ROOT

        self.apiclient.connection.apiKey = self.user_d11a_apikey
        self.apiclient.connection.securityKey = self.user_d11a_secretkey
        self.vmdata["name"] = self.acldata["vmD11A"]["name"] +"-shared-scope-all"
        self.vmdata["displayname"] = self.acldata["vmD11A"]["displayname"] +"-shared-scope-all"
        vm_d11a = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_all.id
        )

        self.assertEqual(vm_d11a.state == "Running",
                    True,
                    "User in a domain under ROOT failed to deploy VM in a shared network with scope=all")

    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_all_subdomainadminuser(self):
        """
        Validate that regular user in a subdomain under ROOT is allowed to deploy VM in a shared network created with scope="all"

        """
        # deploy VM as an admin user in a subdomain under ROOT

        self.apiclient.connection.apiKey = self.user_d11_apikey
        self.apiclient.connection.securityKey = self.user_d11_secretkey
        self.vmdata["name"] = self.acldata["vmD11"]["name"] +"-shared-scope-all"
        self.vmdata["displayname"] = self.acldata["vmD11"]["displayname"] +"-shared-scope-all"
        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_all.id
        )

        self.assertEqual(vm.state == "Running",
                    True,
                    "Admin User in a domain under ROOT failed to deploy VM in a shared network with scope=all")


    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_all_ROOTuser(self):
        """
        Validate that regular user in ROOT domain is allowed to deploy VM in a shared network created with scope="all"

        """
        # deploy VM as user in ROOT domain

        self.apiclient.connection.apiKey = self.user_roota_apikey
        self.apiclient.connection.securityKey = self.user_roota_secretkey
        self.vmdata["name"] = self.acldata["vmROOTA"]["name"] + "-shared-scope-all"
        self.vmdata["displayname"] = self.acldata["vmROOTA"]["displayname"] + "-shared-scope-all"
        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_all.id
        )

        self.assertEqual(vm.state == "Running",
                    True,
                    "User in ROOT domain failed to deploy VM in a shared network with scope=all")

    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_all_ROOTadmin(self):
        """
        Validate that admin user in ROOT domain is allowed to deploy VM in a shared network created with scope="all"
        """
        # deploy VM as admin user in ROOT domain

        self.apiclient.connection.apiKey = self.user_root_apikey
        self.apiclient.connection.securityKey = self.user_root_secretkey
        self.vmdata["name"] = self.acldata["vmROOT"]["name"] + "-shared-scope-all"
        self.vmdata["displayname"] = self.acldata["vmROOT"]["displayname"] + "-shared-scope-all"
        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_all.id
        )

        self.assertEqual(vm.state == "Running",
                    True,
                    "Admin in ROOT domain failed to deploy VM in a shared network with scope=all")

## Test cases relating to deploying Virtual Machine in shared network with scope=Domain and no subdomain access

    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_domain_nosubdomainaccess_domainuser(self):
        """
        Validate that regular user in a domain is allowed to deploy VM in a shared network created with scope="domain" and no subdomain access
        """

        # deploy VM as user in a domain that has shared network with no subdomain access

        self.apiclient.connection.apiKey = self.user_d11a_apikey
        self.apiclient.connection.securityKey = self.user_d11a_secretkey
        self.vmdata["name"] = self.acldata["vmD11A"]["name"] +"-shared-scope-domain-nosubdomainaccess"
        self.vmdata["displayname"] = self.acldata["vmD11A"]["displayname"] +"-shared-scope-domain-nosubdomainaccess"

        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_domain_d11.id
        )

        self.assertEqual(vm.state == "Running",
                    True,
                    "User in a domain that has a shared network with no subdomain access failed to deploy VM in a shared network with scope=domain with no subdomain access")


    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_domain_nosubdomainaccess_domainadminuser(self):
        """
        Validate that admin user in a domain is allowed to deploy VM in a shared network created with scope="domain" and no subdomain access

        """
        #deploy VM as an admin user in a domain that has shared network with no subdomain access

        self.apiclient.connection.apiKey = self.user_d11_apikey
        self.apiclient.connection.securityKey = self.user_d11_secretkey
        self.vmdata["name"] = self.acldata["vmD11"]["name"] +"-shared-scope-domain-nosubdomainaccess"
        self.vmdata["displayname"] = self.acldata["vmD11"]["displayname"] +"-shared-scope-domain-nosubdomainaccess"

        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_domain_d11.id
        )

        self.assertEqual(vm.state == "Running",
                    True,
                    "Admin User in a  domain that has a shared network with no subdomain access failed to deploy VM in a shared network with scope=domain with no subdomain access")

    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_domain_nosubdomainaccess_subdomainuser(self):
        """
        Validate that regular user in a subdomain is NOT allowed to deploy VM in a shared network created with scope="domain" and no subdomain access

        """
        # deploy VM as user in a subdomain under  a domain that has shared network with no subdomain access

        self.apiclient.connection.apiKey = self.user_d111a_apikey
        self.apiclient.connection.securityKey = self.user_d111a_secretkey
        self.vmdata["name"] = self.acldata["vmD111A"]["name"] +"-shared-scope-domain-nosubdomainaccess"
        self.vmdata["displayname"] = self.acldata["vmD111A"]["displayname"] +"-shared-scope-domain-nosubdomainaccess"
        try:
                vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id
                )
                self.fail("Subdomain user is able to deploy VM in a shared network with scope=domain with no subdomain access ")
        except Exception as e:
                self.debug ("When a user from a subdomain deploys a VM in a shared network with scope=domain with no subdomain access %s" %e)
                if not CloudstackAclException.verifyMsginException(e,CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                    self.fail("Error message validation failed when Subdomain user tries to deploy VM in a shared network with scope=domain with no subdomain access")

    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_domain_nosubdomainaccess_subdomainadminuser(self):
        """
        Validate that admin user in a subdomain is NOT allowed to deploy VM in a shared network created with scope="domain" and no subdomain access

        """
        # deploy VM as an admin user in a subdomain under  a domain that has shared network with no subdomain access

        self.apiclient.connection.apiKey = self.user_d111_apikey
        self.apiclient.connection.securityKey = self.user_d111_secretkey
        self.vmdata["name"] = self.acldata["vmD111"]["name"] +"-shared-scope-domain-nosubdomainaccess"
        self.vmdata["displayname"] = self.acldata["vmD111"]["displayname"] +"-shared-scope-domain-nosubdomainaccess"
        try:
                vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id
                )
                self.fail("Subdomain admin user is able to deploy VM in a shared network with scope=domain with no subdomain access ")
        except Exception as e:
                self.debug ("When a admin user from a subdomain deploys a VM in a shared network with scope=domain with no subdomain access %s" %e)
                if not CloudstackAclException.verifyMsginException(e,CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                    self.fail("Error message validation failed when Subdomain admin user tries to deploy VM in a shared network with scope=domain with no subdomain access")



    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_domain_nosubdomainaccess_parentdomainuser(self):
        """
        Validate that user in the parent domain is NOT allowed to deploy VM in a shared network created with scope="domain" and no subdomain access

        """
        # deploy VM as user in parentdomain of a domain that has shared network with no subdomain access

        self.apiclient.connection.apiKey = self.user_d1a_apikey
        self.apiclient.connection.securityKey = self.user_d1a_secretkey
        self.vmdata["name"] = self.acldata["vmD1A"]["name"] +"-shared-scope-domain-nosubdomainaccess"
        self.vmdata["displayname"] = self.acldata["vmD1A"]["displayname"] +"-shared-scope-domain-nosubdomainaccess"
        try:
                vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id
                )
                self.fail("Parent domain user is able to deploy VM in a shared network with scope=domain with no subdomain access ")
        except Exception as e:
                self.debug ("When a user from parent domain deploys a VM in a shared network with scope=domain with no subdomain access %s" %e)
                if not CloudstackAclException.verifyMsginException(e,CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                    self.fail("Error message validation failed when Parent domain user tries to deploy VM in a shared network with scope=domain with no subdomain access")


    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_domain_nosubdomainaccess_parentdomainadminuser(self):
        """
        Validate that admin user in the parent domain is NOT allowed to deploy VM in a shared network created with scope="domain" and no subdomain access

        """
        # deploy VM as an admin user in parentdomain of  a domain that has shared network with no subdomain access

        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD1"]["name"] +"-shared-scope-domain-nosubdomainaccess"
        self.vmdata["displayname"] = self.acldata["vmD1"]["displayname"] +"-shared-scope-domain-nosubdomainaccess"
        try:
                vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id
                )
                self.fail("Parent domain's admin user is able to deploy VM in a shared network with scope=domain with no subdomain access ")
        except Exception as e:
                self.debug ("When an admin user from parent domain deploys a VM in a shared network with scope=domain with no subdomain access %s" %e)
                if not CloudstackAclException.verifyMsginException(e,CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                    self.fail("Error message validation failed when Parent domain's admin user tries to deploy VM in a shared network with scope=domain with no subdomain access")



    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_domain_nosubdomainaccess_ROOTuser(self):
        """
        Validate that user in ROOT domain is NOT allowed to deploy VM in a shared network created with scope="domain" and no subdomain access
        """

        # deploy VM as user in ROOT domain

        self.apiclient.connection.apiKey = self.user_roota_apikey
        self.apiclient.connection.securityKey = self.user_roota_secretkey
        self.vmdata["name"] = self.acldata["vmROOTA"]["name"] + "-shared-scope-domain-nosubdomainaccess"
        self.vmdata["displayname"] = self.acldata["vmROOTA"]["displayname"] + "-shared-scope-domain-nosubdomainaccess"
        try:
                vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id
                )
                self.fail("ROOT domain's user is able to deploy VM in a shared network with scope=domain with no subdomain access ")
        except Exception as e:
                self.debug ("When a regular user from ROOT domain deploys a VM in a shared network with scope=domain with no subdomain access %s" %e)
                if not CloudstackAclException.verifyMsginException(e,CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                    self.fail("Error message validation failed when ROOT domain's user tries to deploy VM in a shared network with scope=domain with no subdomain access")



    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_domain_nosubdomainaccess_ROOTadmin(self):
        """
        Validate that admin in ROOT domain is NOT allowed to deploy VM in a shared network created with scope="domain" and no subdomain access

        """
        # deploy VM as admin user in ROOT domain

        self.apiclient.connection.apiKey = self.user_root_apikey
        self.apiclient.connection.securityKey = self.user_root_secretkey
        self.vmdata["name"] = self.acldata["vmROOT"]["name"] + "-shared-scope-domain-nosubdomainaccess"
        self.vmdata["displayname"] = self.acldata["vmROOT"]["displayname"] + "-shared-scope-domain-nosubdomainaccess"
        try:
                vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id
        )
                self.fail("ROOT domain's admin user is able to deploy VM in a shared network with scope=domain with no subdomain access ")
        except Exception as e:
                self.debug ("When a admin user from ROOT domain deploys a VM in a shared network with scope=domain with no subdomain access %s" %e)
                if not CloudstackAclException.verifyMsginException(e,CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                    self.fail("Error message validation failed when ROOT domain's admin user tries to deploy VM in a shared network with scope=domain with no subdomain access")



## Test cases relating to deploying Virtual Machine in shared network with scope=Domain and with subdomain access

    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_domain_withsubdomainaccess_domainuser(self):
        """
        Validate that regular user in a domain is allowed to deploy VM in a shared network created with scope="domain" and  with subdomain access for the domain

        """
        # deploy VM as user in a domain that has shared network with subdomain access

        self.apiclient.connection.apiKey = self.user_d11a_apikey
        self.apiclient.connection.securityKey = self.user_d11a_secretkey
        self.vmdata["name"] = self.acldata["vmD11A"]["name"] +"-shared-scope-domain-withsubdomainaccess"
        self.vmdata["displayname"] = self.acldata["vmD11A"]["displayname"] +"-shared-scope-domain-withsubdomainaccess"

        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_domain_with_subdomain_d11.id
        )

        self.assertEqual(vm.state == "Running",
                    True,
                    "User in a domain that has a shared network with subdomain access failed to deploy VM in a shared network with scope=domain with no subdomain access")


    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_domain_withsubdomainaccess_domainadminuser(self):
        """
        Validate that admin user in a domain is allowed to deploy VM in a shared network created with scope="domain" and  with subdomain access for the domain
        """

        # deploy VM as an admin user in a domain that has shared network with subdomain access

        self.apiclient.connection.apiKey = self.user_d11_apikey
        self.apiclient.connection.securityKey = self.user_d11_secretkey
        self.vmdata["name"] = self.acldata["vmD11"]["name"] +"-shared-scope-domain-withsubdomainaccess"
        self.vmdata["displayname"] = self.acldata["vmD11"]["displayname"] +"-shared-scope-domain-withsubdomainaccess"

        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_domain_with_subdomain_d11.id
        )

        self.assertEqual(vm.state == "Running",
                    True,
                    "Admin User in a  domain that has a shared network with subdomain access failed to deploy VM in a shared network with scope=domain with no subdomain access")

    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_domain_withsubdomainaccess_subdomainuser(self):
        """
        Validate that regular user in a subdomain is allowed to deploy VM in a shared network created with scope="domain" and  with subdomain access  for the parent domain
        """

        # deploy VM as user in a subdomain under  a domain that has shared network with subdomain access

        self.apiclient.connection.apiKey = self.user_d111a_apikey
        self.apiclient.connection.securityKey = self.user_d111a_secretkey
        self.vmdata["name"] = self.acldata["vmD111A"]["name"] +"-shared-scope-domain-withsubdomainaccess"
        self.vmdata["displayname"] = self.acldata["vmD111A"]["displayname"] +"-shared-scope-domain-withsubdomainaccess"
        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_domain_with_subdomain_d11.id
        )

        self.assertEqual(vm.state == "Running",
                    True,
                    "User in a subdomain that has a shared network with subdomain access failed to deploy VM in a shared network with scope=domain with no subdomain access")

    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_domain_withsubdomainaccess_subdomainadminuser(self):
        """
        Validate that an admin user in a subdomain is allowed to deploy VM in a shared network created with scope="domain" and  with subdomain access for the parent domain
        """

        # deploy VM as an admin user in a subdomain under  a domain that has shared network with subdomain access

        self.apiclient.connection.apiKey = self.user_d111_apikey
        self.apiclient.connection.securityKey = self.user_d111_secretkey
        self.vmdata["name"] = self.acldata["vmD111"]["name"] +"-shared-scope-domain-withsubdomainaccess"
        self.vmdata["displayname"] = self.acldata["vmD111"]["displayname"] +"-shared-scope-domain-withsubdomainaccess"
        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_domain_with_subdomain_d11.id
        )

        self.assertEqual(vm.state == "Running",
                    True,
                    "Admin User in a subdomain that has a shared network with subdomain access failed to deploy VM in a shared network with scope=domain with no subdomain access")

    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_domain_withsubdomainaccess_parentdomainuser(self):
        """
        Validate that regular user in a parent domain is NOT allowed to deploy VM in a shared network created with scope="domain" and  with subdomain access for the domain
        """

        # deploy VM as user in parentdomain of a domain that has shared network with subdomain access

        self.apiclient.connection.apiKey = self.user_d1a_apikey
        self.apiclient.connection.securityKey = self.user_d1a_secretkey
        self.vmdata["name"] = self.acldata["vmD1A"]["name"] +"-shared-scope-domain-withsubdomainaccess"
        self.vmdata["displayname"] = self.acldata["vmD1A"]["displayname"] +"-shared-scope-domain-withsubdomainaccess"
        try:
                vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_with_subdomain_d11.id
                )
                self.fail("Parent domain's user is able to deploy VM in a shared network with scope=domain with subdomain access ")
        except Exception as e:
                self.debug ("When a user from parent domain deploys a VM in a shared network with scope=domain with subdomain access %s" %e)
                if not CloudstackAclException.verifyMsginException(e,CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                    self.fail("Error message validation failed when Parent domain's user tries to deploy VM in a shared network with scope=domain with subdomain access ")


    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_domain_withsubdomainaccess_parentdomainadminuser(self):
        """
        Validate that admin user in a parent domain is NOT allowed to deploy VM in a shared network created with scope="domain" and  with subdomain access for any domain
        """

        # deploy VM as an admin user in parentdomain of  a domain that has shared network with subdomain access

        self.apiclient.connection.apiKey = self.user_d1_apikey
        self.apiclient.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = self.acldata["vmD1"]["name"] +"-shared-scope-domain-withsubdomainaccess"
        self.vmdata["displayname"] = self.acldata["vmD1"]["displayname"] +"-shared-scope-domain-withsubdomainaccess"
        try:
                vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_with_subdomain_d11.id
                )
                self.fail("Parent domain's admin user is able to deploy VM in a shared network with scope=domain with  subdomain access ")
        except Exception as e:
                self.debug ("When an admin user from parent domain deploys a VM in a shared network with scope=domain with  subdomain access %s" %e)
                if not CloudstackAclException.verifyMsginException(e,CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                    self.fail("Error message validation failed when Parent domain's admin user tries to deploy VM in a shared network with scope=domain with  subdomain access")



    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_domain_withsubdomainaccess_ROOTuser(self):
        """
        Validate that regular user in ROOT domain is NOT allowed to deploy VM in a shared network created with scope="domain" and  with subdomain access for any domain
        """

        # deploy VM as user in ROOT domain

        self.apiclient.connection.apiKey = self.user_roota_apikey
        self.apiclient.connection.securityKey = self.user_roota_secretkey
        self.vmdata["name"] = self.acldata["vmROOTA"]["name"] + "-shared-scope-domain-withsubdomainaccess"
        self.vmdata["displayname"] = self.acldata["vmROOTA"]["displayname"] + "-shared-scope-domain-withsubdomainaccess"
        try:
                vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_with_subdomain_d11.id
                )
                self.fail("ROOT domain's user is able to deploy VM in a shared network with scope=domain with subdomain access ")
        except Exception as e:
                self.debug ("When a user from ROOT domain deploys a VM in a shared network with scope=domain with subdomain access %s" %e)
                if not CloudstackAclException.verifyMsginException(e,CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                    self.fail("Error message validation failed when ROOT domain's user tries to deploy VM in a shared network with scope=domain with subdomain access")


    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_domain_withsubdomainaccess_ROOTadmin(self):
        """
        Validate that admin user in ROOT domain is NOT allowed to deploy VM in a shared network created with scope="domain" and  with subdomain access for any domain
        """

        # deploy VM as admin user in ROOT domain

        self.apiclient.connection.apiKey = self.user_root_apikey
        self.apiclient.connection.securityKey = self.user_root_secretkey
        self.vmdata["name"] = self.acldata["vmROOT"]["name"] + "-shared-scope-domain-withsubdomainaccess"
        self.vmdata["displayname"] = self.acldata["vmROOT"]["displayname"] + "-shared-scope-domain-withsubdomainaccess"
        try:
                vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_with_subdomain_d11.id
                )
                self.fail("ROOT domain's admin user is able to deploy VM in a shared network with scope=domain with subdomain access ")
        except Exception as e:
                self.debug ("When an admin user from ROOT domain deploys a VM in a shared network with scope=domain with subdomain access %s" %e)
                if not CloudstackAclException.verifyMsginException(e,CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                    self.fail("Error message validation failed when ROOT domain's admin user tries to deploy VM in a shared network with scope=domain with subdomain access")



## Test cases relating to deploying Virtual Machine in shared network with scope=account

    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_account_domainuser(self):
        """
        Validate that any other user in same domain is NOT allowed to deploy VM in a shared network created with scope="account" for an account
        """

        # deploy VM as user under the same domain but belonging to a different account from the acount that has a shared network with scope=account

        self.apiclient.connection.apiKey = self.user_d111b_apikey
        self.apiclient.connection.securityKey = self.user_d111b_secretkey
        self.vmdata["name"] = self.acldata["vmD111B"]["name"] +"-shared-scope-domain-withsubdomainaccess"
        self.vmdata["displayname"] = self.acldata["vmD111B"]["displayname"] +"-shared-scope-domain-withsubdomainaccess"
        try:
                vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_account_d111a.id
                )
                self.fail("User from same domain but different account is able to deploy VM in a shared network with scope=account")
        except Exception as e:
                self.debug ("When a user from same domain but different account deploys a VM in a shared network with scope=account %s" %e)
                if not CloudstackAclException.verifyMsginException(e,CloudstackAclException.UNABLE_TO_USE_NETWORK):
                    self.fail("Error message validation failed when User from same domain but different account tries to deploy VM in a shared network with scope=account")



    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_account_domainadminuser(self):
        """
        Validate that an admin user under the same domain but belonging to a different account is allowed to deploy VM in a shared network created with scope="account" for an account

        """
        # deploy VM as admin user for a domain that has an account with shared network with scope=account

        self.apiclient.connection.apiKey = self.user_d111_apikey
        self.apiclient.connection.securityKey = self.user_d111_secretkey
        self.vmdata["name"] = self.acldata["vmD111"]["name"] +"-shared-scope-domain-withsubdomainaccess"
        self.vmdata["displayname"] = self.acldata["vmD111"]["displayname"] +"-shared-scope-domain-withsubdomainaccess"
        try:
                vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_account_d111a.id
                )
                self.fail("User from same domain but different account is able to deploy VM in a shared network with scope=account")
        except Exception as e:
                self.debug ("When a user from same domain but different account deploys a VM in a shared network with scope=account %s" %e)
                if not CloudstackAclException.verifyMsginException(e,CloudstackAclException.UNABLE_TO_USE_NETWORK):
                    self.fail("Error message validation failed when User from same domain but different account tries to deploy VM in a shared network with scope=account")


    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_account_user(self):
        """
        Validate that regular user in the account is allowed to deploy VM in a shared network created with scope="account" for an account
        """

        # deploy VM as account with shared network with scope=account

        self.apiclient.connection.apiKey = self.user_d111a_apikey
        self.apiclient.connection.securityKey = self.user_d111a_secretkey
        self.vmdata["name"] = self.acldata["vmD111A"]["name"] +"-shared-scope-domain-withsubdomainaccess"
        self.vmdata["displayname"] = self.acldata["vmD111A"]["displayname"] +"-shared-scope-domain-withsubdomainaccess"

        vm = VirtualMachine.create(
            self.apiclient,
            self.vmdata,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=self.shared_network_account_d111a.id
        )

        self.assertEqual(vm.state == "Running",
                    True,
                    "User in the account that has a shared network with scope=account failed to deploy a VM in this shared network")

    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_account_differentdomain(self):
        """
        Validate that regular user from a domain different from that of the account is NOT allowed to deploy VM in a shared network created with scope="account" for an account
        """

        # deploy VM as a user in a subdomain under ROOT

        self.apiclient.connection.apiKey = self.user_d2a_apikey
        self.apiclient.connection.securityKey = self.user_d2a_secretkey
        self.vmdata["name"] = self.acldata["vmD2A"]["name"] +"-shared-scope-account"
        self.vmdata["displayname"] = self.acldata["vmD2A"]["displayname"] +"-shared-scope-account"
        try:
                vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_account_d111a.id
                )
                self.fail("User from different domain is able to deploy VM in a shared network with scope=account ")
        except Exception as e:
                self.debug ("When a user from different domain deploys a VM in a shared network with scope=account %s" %e)
                if not CloudstackAclException.verifyMsginException(e,CloudstackAclException.UNABLE_TO_USE_NETWORK):
                    self.fail("Error message validation failed when User from different domain tries to deploy VM in a shared network with scope=account")



    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_account_ROOTuser(self):
        """
        Validate that user in ROOT domain is NOT allowed to deploy VM in a shared network created with scope="account" for an account

        """
        # deploy VM as user in ROOT domain

        self.apiclient.connection.apiKey = self.user_roota_apikey
        self.apiclient.connection.securityKey = self.user_roota_secretkey
        self.vmdata["name"] = self.acldata["vmROOTA"]["name"] + "-shared-scope-account"
        self.vmdata["displayname"] = self.acldata["vmROOTA"]["displayname"] + "-shared-scope-account"
        try:
                vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_account_d111a.id
                )
                self.fail("ROOT domain's  user is able to deploy VM in a shared network with scope=account ")
        except Exception as e:
                self.debug ("When a user from ROOT domain deploys a VM in a shared network with scope=account %s" %e)
                if not CloudstackAclException.verifyMsginException(e,CloudstackAclException.UNABLE_TO_USE_NETWORK):
                    self.fail("Error message validation failed when ROOT domain's  user tries to deploy VM in a shared network with scope=account ")


    @attr("simulator_only",tags=["advanced"],required_hardware="false")
    def test_deployVM_in_sharedNetwork_scope_account_ROOTadmin(self):
        """
        Validate that admin user in ROOT domain is NOT allowed to deploy VM in a shared network created with scope="account" for an account
        """

        # deploy VM as admin user in ROOT domain

        self.apiclient.connection.apiKey = self.user_root_apikey
        self.apiclient.connection.securityKey = self.user_root_secretkey
        self.vmdata["name"] = self.acldata["vmROOT"]["name"] + "-shared-scope-account"
        self.vmdata["displayname"] = self.acldata["vmROOT"]["displayname"] + "-shared-scope-account"
        try:
                vm = VirtualMachine.create(
                self.apiclient,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_account_d111a.id
                )
                self.fail("ROOT domain's admin user is able to deploy VM in a shared network with scope=account ")
        except Exception as e:
                self.debug ("When an admin user from ROOT domain deploys a VM in a shared network with scope=account %s" %e)
                if not CloudstackAclException.verifyMsginException(e,CloudstackAclException.UNABLE_TO_USE_NETWORK):
                    self.fail("Error message validation failed when ROOT domain's admin user tries to deploy VM in a shared network with scope=account")



    @staticmethod
    def generateKeysForUser(apiclient,account):
        user = User.list(
                          apiclient,
                          account=account.name,
                          domainid=account.domainid
                          )[0]

        return (User.registerUserKeys(
                        apiclient,
                        user.id
                      ))
