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

"""Component tests for Shared Network functionality with Nuage VSP SDN plugin:
Deploying Virtual Machines using impersonation
(passing account and domainId parameters)
"""
# Import Local Modules
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Account,
                             Domain,
                             User,
                             VirtualMachine,
                             Network,
                             NetworkOffering)
from marvin.cloudstackException import CloudstackAclException
from nuageTestCase import nuageTestCase
# Import System modules
from nose.plugins.attrib import attr
import random
import string


class TestNuageSharedNetworkDeployVm(nuageTestCase):
    @classmethod
    def setUpClass(cls):
        """
        Create the following domain tree and accounts that are required for
        executing Nuage VSP SDN plugin test cases for shared networks:
            Under ROOT - create domain D1
            Under domain D1 - Create two subdomains D11 and D12
            Under each of the domains - create one admin user and couple of
            regular users.
        Create shared network with the following scope:
            1. Network with scope="all"
            2. Network with scope="domain" with no subdomain access
            3. Network with scope="domain" with subdomain access
            4. Network with scope="account"
        """

        super(TestNuageSharedNetworkDeployVm, cls).setUpClass()
        cls.sharednetworkdata = cls.test_data["acl"]
        cls.nuagenetworkdata = cls.test_data["nuagevsp"]

        cls.domain_1 = None
        cls.domain_2 = None

        try:
            # backup default apikey and secretkey
            cls.default_apikey = cls.api_client.connection.apiKey
            cls.default_secretkey = cls.api_client.connection.securityKey

            # Create domains
            cls.domain_1 = Domain.create(
                cls.api_client,
                cls.sharednetworkdata["domain1"]
            )
            cls.domain_11 = Domain.create(
                cls.api_client,
                cls.sharednetworkdata["domain11"],
                parentdomainid=cls.domain_1.id
            )
            cls.domain_111 = Domain.create(
                cls.api_client,
                cls.sharednetworkdata["domain111"],
                parentdomainid=cls.domain_11.id,
            )
            cls.domain_12 = Domain.create(
                cls.api_client,
                cls.sharednetworkdata["domain12"],
                parentdomainid=cls.domain_1.id
            )
            cls.domain_2 = Domain.create(
                cls.api_client,
                cls.sharednetworkdata["domain2"]
            )
            # Create  1 admin account and 2 user accounts for doamin_1
            cls.account_d1 = Account.create(
                cls.api_client,
                cls.sharednetworkdata["accountD1"],
                admin=True,
                domainid=cls.domain_1.id
            )

            user = cls.generateKeysForUser(cls.api_client, cls.account_d1)
            cls.user_d1_apikey = user.apikey
            cls.user_d1_secretkey = user.secretkey

            cls.account_d1a = Account.create(
                cls.api_client,
                cls.sharednetworkdata["accountD1A"],
                admin=False,
                domainid=cls.domain_1.id
            )
            user = cls.generateKeysForUser(cls.api_client, cls.account_d1a)
            cls.user_d1a_apikey = user.apikey
            cls.user_d1a_secretkey = user.secretkey

            cls.account_d1b = Account.create(
                cls.api_client,
                cls.sharednetworkdata["accountD1B"],
                admin=False,
                domainid=cls.domain_1.id
            )

            user = cls.generateKeysForUser(cls.api_client, cls.account_d1b)
            cls.user_d1b_apikey = user.apikey
            cls.user_d1b_secretkey = user.secretkey

            # Create  1 admin and 2 user accounts for doamin_11
            cls.account_d11 = Account.create(
                cls.api_client,
                cls.sharednetworkdata["accountD11"],
                admin=True,
                domainid=cls.domain_11.id
            )
            user = cls.generateKeysForUser(cls.api_client, cls.account_d11)
            cls.user_d11_apikey = user.apikey
            cls.user_d11_secretkey = user.secretkey

            cls.account_d11a = Account.create(
                cls.api_client,
                cls.sharednetworkdata["accountD11A"],
                admin=False,
                domainid=cls.domain_11.id
            )
            user = cls.generateKeysForUser(cls.api_client, cls.account_d11a)
            cls.user_d11a_apikey = user.apikey
            cls.user_d11a_secretkey = user.secretkey

            cls.account_d11b = Account.create(
                cls.api_client,
                cls.sharednetworkdata["accountD11B"],
                admin=False,
                domainid=cls.domain_11.id
            )
            user = cls.generateKeysForUser(cls.api_client, cls.account_d11b)
            cls.user_d11b_apikey = user.apikey
            cls.user_d11b_secretkey = user.secretkey

            # Create  2 user accounts and 1 admin account for doamin_111

            cls.account_d111 = Account.create(
                cls.api_client,
                cls.sharednetworkdata["accountD111"],
                admin=True,
                domainid=cls.domain_111.id
            )
            user = cls.generateKeysForUser(cls.api_client, cls.account_d111)
            cls.user_d111_apikey = user.apikey
            cls.user_d111_secretkey = user.secretkey

            cls.account_d111a = Account.create(
                cls.api_client,
                cls.sharednetworkdata["accountD111A"],
                admin=False,
                domainid=cls.domain_111.id
            )
            user = cls.generateKeysForUser(cls.api_client, cls.account_d111a)
            cls.user_d111a_apikey = user.apikey
            cls.user_d111a_secretkey = user.secretkey

            cls.account_d111b = Account.create(
                cls.api_client,
                cls.sharednetworkdata["accountD111B"],
                admin=False,
                domainid=cls.domain_111.id
            )
            user = cls.generateKeysForUser(cls.api_client, cls.account_d111b)
            cls.user_d111b_apikey = user.apikey
            cls.user_d111b_secretkey = user.secretkey

            # Create  2 user accounts for doamin_12
            cls.account_d12a = Account.create(
                cls.api_client,
                cls.sharednetworkdata["accountD12A"],
                admin=False,
                domainid=cls.domain_12.id
            )
            user = cls.generateKeysForUser(cls.api_client, cls.account_d12a)
            cls.user_d12a_apikey = user.apikey
            cls.user_d12a_secretkey = user.secretkey

            cls.account_d12b = Account.create(
                cls.api_client,
                cls.sharednetworkdata["accountD12B"],
                admin=False,
                domainid=cls.domain_12.id
            )

            user = cls.generateKeysForUser(cls.api_client, cls.account_d12b)
            cls.user_d12b_apikey = user.apikey
            cls.user_d12b_secretkey = user.secretkey

            # Create 1 user account for domain_2

            cls.account_d2a = Account.create(
                cls.api_client,
                cls.sharednetworkdata["accountD2"],
                admin=False,
                domainid=cls.domain_2.id
            )

            user = cls.generateKeysForUser(cls.api_client, cls.account_d2a)
            cls.user_d2a_apikey = user.apikey
            cls.user_d2a_secretkey = user.secretkey

            # Create 1 user account and admin account in "ROOT" domain

            cls.account_roota = Account.create(
                cls.api_client,
                cls.sharednetworkdata["accountROOTA"],
                admin=False,
            )

            user = cls.generateKeysForUser(cls.api_client, cls.account_roota)
            cls.user_roota_apikey = user.apikey
            cls.user_roota_secretkey = user.secretkey

            cls.account_root = Account.create(
                cls.api_client,
                cls.sharednetworkdata["accountROOTA"],
                admin=True,
            )

            user = cls.generateKeysForUser(cls.api_client, cls.account_root)
            cls.user_root_apikey = user.apikey
            cls.user_root_secretkey = user.secretkey

            # service offering is already created in Nuagetestcase
            cls.sharednetworkdata['mode'] = cls.zone.networktype

            # As admin user , create shared network with scope "all", "domain"
            # with subdomain access , "domain" without subdomain access and
            # "account"
            cls.api_client.connection.apiKey = cls.default_apikey
            cls.api_client.connection.securityKey = cls.default_secretkey

            cls.test_data["nuagevsp"][
                "shared_nuage_network_offering"]["serviceProviderList"]\
                .update({"UserData": 'VirtualRouter'})
            cls.test_data["nuagevsp"]["shared_nuage_network_offering"][
                "supportedservices"] = 'Dhcp,Connectivity,UserData'
            for key, value in cls.test_data["nuagevsp"][
                "shared_nuage_network_offering"]["serviceProviderList"]\
                    .iteritems():
                cls.debug("elements are  %s and value is %s" % (key, value))

            cls.shared_network_offering = NetworkOffering.create(
                cls.api_client,
                cls.test_data["nuagevsp"]["shared_nuage_network_offering"],
                conservemode=False
                )
            # Enable Network offering
            cls.shared_network_offering.update(cls.api_client, state='Enabled')
            cls.shared_network_offering_id = cls.shared_network_offering.id

            cls.shared_network_all = Network.create(
                cls.api_client,
                cls.test_data["nuagevsp"]["network_all"],
                networkofferingid=cls.shared_network_offering_id,
                zoneid=cls.zone.id
            )

            cls.shared_network_domain_d11 = Network.create(
                cls.api_client,
                cls.test_data["nuagevsp"][
                    "network_domain_with_no_subdomain_access"],
                networkofferingid=cls.shared_network_offering_id,
                zoneid=cls.zone.id,
                domainid=cls.domain_11.id,
                subdomainaccess=False
            )

            cls.shared_network_domain_with_subdomain_d11 = Network.create(
                cls.api_client,
                cls.test_data["nuagevsp"][
                    "network_domain_with_subdomain_access"],
                networkofferingid=cls.shared_network_offering_id,
                zoneid=cls.zone.id,
                domainid=cls.domain_11.id,
                subdomainaccess=True
            )

            cls.shared_network_account_d111a = Network.create(
                cls.api_client,
                cls.test_data["nuagevsp"]["network_account"],
                networkofferingid=cls.shared_network_offering_id,
                zoneid=cls.zone.id,
                domainid=cls.domain_111.id,
                accountid=cls.account_d111a.user[0].username
            )
            cls.vmdata = {"name": "test",
                          "displayname": "test"
                          }

            cls._cleanup = [
                cls.account_root,
                cls.account_roota,
                cls.shared_network_all,
                cls.shared_network_offering,
                cls.service_offering,
            ]
            user_data = ''.join(random.choice(
                string.ascii_uppercase + string.digits) for x in range(2500))
            cls.vmdata["userdata"] = user_data
        except Exception as e:
            cls.domain_1.delete(cls.api_client, cleanup="true")
            cls.domain_2.delete(cls.api_client, cleanup="true")
            cleanup_resources(cls.api_client, cls._cleanup)
            raise Exception(
                "Failed to create the setup required to execute the test "
                "cases: %s" % e)

        return

    @classmethod
    def tearDownClass(cls):

        cls.api_client.connection.apiKey = cls.default_apikey
        cls.api_client.connection.securityKey = cls.default_secretkey
        cls.domain_1.delete(cls.api_client, cleanup="true")
        cls.domain_2.delete(cls.api_client, cleanup="true")
        cleanup_resources(cls.api_client, cls._cleanup)
        return

    def setUp(self):
        self.api_client = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

    def tearDown(self):
        # restore back default apikey and secretkey
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        return

    # Test cases relating to deploying Virtual Machine as ROOT admin for other
    # users in shared network with scope=all
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_all_domainuser(self):
        """Validate that ROOT admin is able to deploy a VM for other users in a
        shared network with scope=all
        """

        # Deploy VM for a user in a domain under ROOT as admin
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD1A"]["name"] + \
            "-shared-scope-all-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD1A"]["displayname"] + \
            "-shared-scope-all-root-admin"

        vm = self.create_VM(self.shared_network_all, testdata=self.vmdata,
                            account=self.account_d1a, cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d1a.name and
            vm.domainid == self.account_d1a.domainid,
            True,
            "ROOT admin is not able to deploy a VM for other users in a "
            "shared network with scope=all")
        subnet_id = self.get_subnet_id(self.shared_network_all.id,
                                       self.nuagenetworkdata["network_all"][
                                           "gateway"])
        self.verify_vsd_enterprise_vm(self.account_d1a.domainid,
                                      self.shared_network_all, vm,
                                      sharedsubnetid=subnet_id)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_all_domainadminuser(
            self):
        """Validate that ROOT admin is able to deploy a VM for a domain admin
        users in a shared network with scope=all
        """

        # Deploy VM for an admin user in a domain under ROOT as admin
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD1"]["name"] + \
            "-shared-scope-all-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD1"]["displayname"] + \
            "-shared-scope-all-root-admin"

        vm = self.create_VM(self.shared_network_all, testdata=self.vmdata,
                            account=self.account_d1, cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d1.name and
            vm.domainid == self.account_d1.domainid,
            True,
            "ROOT admin is not able to deploy a VM "
            "for a domain admin users in a shared network with scope=all")

        self.verify_vsd_shared_network(
            self.account_d1.domainid,
            self.shared_network_all,
            gateway=self.nuagenetworkdata["network_all"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_all.id,
            self.nuagenetworkdata["network_all"]["gateway"])
        self.verify_vsd_enterprise_vm(self.account_d1.domainid,
                                      self.shared_network_all, vm,
                                      sharedsubnetid=subnet_id)

        # Deleting the VM
        vm.delete(self.api_client, expunge=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_all_subdomainuser(self):
        """Validate that ROOT admin is able to deploy a VM for any user in a
        subdomain in a shared network with scope=all
        """

        # Deploy VM as user in a subdomain under ROOT
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD11A"]["name"] + \
            "-shared-scope-all-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD11A"]["displayname"] + \
            "-shared-scope-all-root-admin"

        vm = self.create_VM(self.shared_network_all, testdata=self.vmdata,
                            account=self.account_d11a, cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d11a.name and
            vm.domainid == self.account_d11a.domainid,
            True,
            "ROOT admin is not able to deploy a VM"
            " for any user in a subdomain in a shared network with scope=all")

        self.verify_vsd_shared_network(
            self.account_d11a.domainid,
            self.shared_network_all,
            gateway=self.nuagenetworkdata["network_all"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_all.id,
            self.nuagenetworkdata["network_all"]["gateway"])
        self.verify_vsd_enterprise_vm(self.account_d11a.domainid,
                                      self.shared_network_all, vm,
                                      sharedsubnetid=subnet_id)

        # Deleting the VM
        vm.delete(self.api_client, expunge=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_all_subdomainadminuser(
            self):
        """Validate that ROOT admin is able to deploy a VM for admin user in a
        domain in a shared network with scope=all
        """

        # Deploy VM as an admin user in a subdomain under ROOT
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD11"]["name"] + \
            "-shared-scope-all-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD11"]["displayname"] + \
            "-shared-scope-all-root-admin"

        vm = self.create_VM(self.shared_network_all, testdata=self.vmdata,
                            account=self.account_d11, cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d11.name and
            vm.domainid == self.account_d11.domainid,
            True,
            "ROOT admin is not able to deploy a VM for admin user in a domain "
            "in a shared network with scope=all")
        self.verify_vsd_shared_network(
            self.account_d11.domainid,
            self.shared_network_all,
            gateway=self.nuagenetworkdata["network_all"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_all.id,
            self.nuagenetworkdata["network_all"]["gateway"])
        self.verify_vsd_enterprise_vm(self.account_d11.domainid,
                                      self.shared_network_all, vm,
                                      sharedsubnetid=subnet_id)

        # Deleting the VM
        vm.delete(self.api_client, expunge=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_all_ROOTuser(self):
        """Validate that ROOT admin is able to deploy a VM for user in ROOT
        domain in a shared network with scope=all
        """

        # Deploy VM as user in ROOT domain
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmROOTA"]["name"] + \
            "-shared-scope-all-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmROOTA"]["displayname"] + \
            "-shared-scope-all-root-admin"

        vm = self.create_VM(self.shared_network_all, testdata=self.vmdata,
                            account=self.account_roota, cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_roota.name and
            vm.domainid == self.account_roota.domainid,
            True,
            "ROOT admin is not able to deploy a VM for user in ROOT domain in "
            "a shared network with scope=all")
        self.verify_vsd_shared_network(
            self.account_roota.domainid,
            self.shared_network_all,
            gateway=self.nuagenetworkdata["network_all"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_all.id,
            self.nuagenetworkdata["network_all"]["gateway"])
        self.verify_vsd_enterprise_vm(self.account_roota.domainid,
                                      self.shared_network_all, vm,
                                      sharedsubnetid=subnet_id)

        # Deleting the VM
        vm.delete(self.api_client, expunge=True)

    # Test cases relating to deploying Virtual Machine as ROOT admin for other
    # users in shared network with scope=Domain and no subdomain access
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_nosubdomccess_domainuser(self):
        """Validate that ROOT admin is able to deploy a VM for domain user in
        a shared network with scope=domain with no subdomain access
        """

        # Deploy VM as user in a domain that has shared network with no
        # subdomain access
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD11A"]["name"] + \
            "-shared-scope-domain-nosubdomainaccess-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD11A"]["displayname"] + \
            "-shared-scope-domain-nosubdomainaccess-root-admin"

        vm = self.create_VM(self.shared_network_domain_d11,
                            testdata=self.vmdata, account=self.account_d11a,
                            cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d11a.name and
            vm.domainid == self.account_d11a.domainid,
            True,
            "ROOT admin is not able to deploy a VM for domain user in a "
            "shared network with scope=domain with no subdomain access")
        self.verify_vsd_shared_network(
            self.account_d11a.domainid,
            self.shared_network_domain_d11,
            gateway=self.nuagenetworkdata[
                "network_domain_with_no_subdomain_access"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_domain_d11.id,
            self.nuagenetworkdata[
                "network_domain_with_no_subdomain_access"]["gateway"])
        self.verify_vsd_enterprise_vm(self.account_d11a.domainid,
                                      self.shared_network_domain_d11, vm,
                                      sharedsubnetid=subnet_id)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_nosubdomccess_domainadminuser(
            self):
        """Validate that ROOT admin is able to deploy a VM for domain admin
        user in a shared network with scope=domain with no subdomain access
        """

        # Deploy VM as an admin user in a domain that has shared network with
        # no subdomain access
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD11"]["name"] + \
            "-shared-scope-domain-nosubdomainaccess-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD11"]["displayname"] + \
            "-shared-scope-domain-nosubdomainaccess-root-admin"

        vm = self.create_VM(self.shared_network_domain_d11,
                            testdata=self.vmdata, account=self.account_d11,
                            cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d11.name and
            vm.domainid == self.account_d11.domainid,
            True,
            "ROOT admin is not able to deploy VM for domain admin user in "
            "shared network with scope=domain with no subdomain access")
        self.verify_vsd_shared_network(
            self.account_d11.domainid,
            self.shared_network_domain_d11,
            gateway=self.nuagenetworkdata[
                "network_domain_with_no_subdomain_access"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_domain_d11.id,
            self.nuagenetworkdata[
                "network_domain_with_no_subdomain_access"]["gateway"])
        self.verify_vsd_enterprise_vm(self.account_d11.domainid,
                                      self.shared_network_domain_d11, vm,
                                      sharedsubnetid=subnet_id)

        # Deleting the VM
        vm.delete(self.api_client, expunge=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_nosubdomccess_subdomainuser(
            self):
        """Validate that ROOT admin is NOT able to deploy a VM for sub domain
        user in a shared network with scope=domain with no subdomain access
        """

        # Deploy VM as user in a subdomain under  a domain that has shared
        # network with no subdomain access
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD111A"]["name"] + \
            "-shared-scope-domain-nosubdomainaccess-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD111A"]["displayname"] + \
            "-shared-scope-domain-nosubdomainaccess-root-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id,
                accountid=self.account_d111a.name,
                domainid=self.account_d111a.domainid
            )
            self.fail(
                "ROOT admin is able to deploy a VM for sub domain user in a "
                "shared network with scope=domain with no subdomain access")

        except Exception as e:
            self.debug(
                "When a user from a subdomain deploys a VM in a shared "
                "network with scope=domain with no subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when ROOT admin tries to "
                    "deploy a VM for sub domain user in a shared network with "
                    "scope=domain with no subdomain access ")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_nosubdomccess_subdomainadmin(
            self):
        """Validate that ROOT admin is NOT able to deploy a VM for sub domain
        admin user in a shared network with scope=domain with no subdomain
        access
        """

        # Deploy VM as an admin user in a subdomain under  a domain that has
        # shared network with no subdomain access
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD111"]["name"] + \
            "-shared-scope-domain-nosubdomainaccess-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD111"]["displayname"] + \
            "-shared-scope-domain-nosubdomainaccess-root-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id,
                accountid=self.account_d111.name,
                domainid=self.account_d111.domainid
            )
            self.fail(
                "ROOT admin is able to deploy VM for sub domain admin user in "
                "a shared network with scope=domain with no subdomain access")
        except Exception as e:
            self.debug(
                "When a admin user from a subdomain deploys a VM in a shared "
                "network with scope=domain with no subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when ROOT admin tries to "
                    "deploy a VM for sub domain admin user in a shared "
                    "network with scope=domain with no subdomain access")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_nosubdomccess_parentdomainuser(
            self):
        """Validate that ROOT admin is NOT able to deploy a VM for parent
        domain user in a shared network with scope=domain with no subdomain
        access
        """

        # Deploy VM as user in parentdomain of a domain that has shared network
        # with no subdomain access
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD1A"]["name"] + \
            "-shared-scope-domain-nosubdomainaccess-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD1A"]["displayname"] + \
            "-shared-scope-domain-nosubdomainaccess-root-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id,
                accountid=self.account_d1a.name,
                domainid=self.account_d1a.domainid
            )
            self.fail(
                " ROOT admin is able to deploy a VM for parent domain user in "
                "a shared network with scope=domain with no subdomain access")
        except Exception as e:
            self.debug(
                "When a user from parent domain deploys a VM in a shared "
                "network with scope=domain with no subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when  ROOT admin tries "
                    "to deploy a VM for parent domain user in a shared "
                    "network with scope=domain with no subdomain access")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_nosubdomccess_parentdomadmin(
            self):
        """Validate that ROOT admin is NOT able to deploy a VM for parent
        domain admin user in a shared network with scope=domain with no
        subdomain access
        """

        # Deploy VM as an admin user in parentdomain of  a domain that has
        # shared network with no subdomain access
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD1"]["name"] + \
            "-shared-scope-domain-nosubdomainaccess-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD1"]["displayname"] + \
            "-shared-scope-domain-nosubdomainaccess-root-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id,
                accountid=self.account_d1.name,
                domainid=self.account_d1.domainid
            )
            self.fail(
                "ROOT admin is able to deploy a VM for parent domain admin "
                "user in a shared network with scope=domain with no subdomain "
                "access")
        except Exception as e:
            self.debug(
                "When an admin user from parent domain deploys a VM in a "
                "shared network with scope=domain with no subdomain access %s"
                % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when ROOT admin tries to "
                    "deploy a VM for parent domain "
                    "admin user in a shared network with scope=domain with no "
                    "subdomain access ")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_nosubdomccess_ROOTuser(
            self):
        """Validate that ROOT admin is NOT able to deploy a VM for parent
        domain admin user in a shared network with scope=domain with no
        subdomain access
        """

        # Deploy VM as user in ROOT domain
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmROOTA"]["name"] + \
            "-shared-scope-domain-nosubdomainaccess-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmROOTA"]["displayname"] + \
            "-shared-scope-domain-nosubdomainaccess-root-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id,
                accountid=self.account_roota.name,
                domainid=self.account_roota.domainid
            )
            self.fail(
                "ROOT admin is able to deploy a VM for parent domain admin "
                "user in a shared network with scope=domain with no subdomain "
                "access")
        except Exception as e:
            self.debug(
                "When a regular user from ROOT domain deploys a VM in a "
                "shared network with scope=domain with no subdomain access %s"
                % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when ROOT admin tries to "
                    "deploy a VM for parent domain admin user in a shared "
                    "network with scope=domain with no subdomain access")

    # Test cases relating to deploying Virtual Machine as ROOT admin for other
    # users in shared network with scope=Domain and with subdomain access
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_subdomaccess_domainuser(self):
        """Validate that ROOT admin is able to deploy a VM for domain user in a
        shared network with scope=domain with subdomain access
        """

        # Deploy VM as user in a domain that has shared network with subdomain
        # access
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD11A"]["name"] + \
            "-shared-scope-domain-withsubdomainaccess-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD11A"]["displayname"] + \
            "-shared-scope-domain-withsubdomainaccess-root-admin"

        vm = self.create_VM(self.shared_network_domain_with_subdomain_d11,
                            testdata=self.vmdata, account=self.account_d11a,
                            cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d11a.name and
            vm.domainid == self.account_d11a.domainid,
            True,
            "ROOT admin is NOT able to deploy a VM for domain user in a "
            "shared network with scope=domain with subdomain access")

        self.verify_vsd_shared_network(
            self.account_d11a.domainid,
            self.shared_network_domain_with_subdomain_d11,
            gateway=self.nuagenetworkdata[
                "network_domain_with_subdomain_access"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_domain_with_subdomain_d11.id,
            self.nuagenetworkdata[
                "network_domain_with_subdomain_access"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11a.domainid,
            self.shared_network_domain_with_subdomain_d11,
            vm, sharedsubnetid=subnet_id)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_subdomaccess_domainadminuser(
            self):
        """Validate that ROOT admin is able to deploy a VM for domain admin
        user in a shared network with scope=domain with subdomain access
        """

        # Deploy VM as an admin user in a domain that has shared network with
        # subdomain access
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD11"]["name"] + \
            "-shared-scope-domain-withsubdomainaccess-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD11"]["displayname"] + \
            "-shared-scope-domain-withsubdomainaccess-root-admin"

        vm = self.create_VM(self.shared_network_domain_with_subdomain_d11,
                            testdata=self.vmdata, account=self.account_d11,
                            cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d11.name and
            vm.domainid == self.account_d11.domainid,
            True,
            "ROOT admin is not able to deploy a VM for domain admin user in a "
            "shared network with scope=domain with subdomain access")

        self.verify_vsd_shared_network(
            self.account_d11.domainid,
            self.shared_network_domain_with_subdomain_d11,
            gateway=self.nuagenetworkdata[
                "network_domain_with_subdomain_access"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_domain_with_subdomain_d11.id,
            self.nuagenetworkdata[
                "network_domain_with_subdomain_access"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11.domainid,
            self.shared_network_domain_with_subdomain_d11,
            vm, sharedsubnetid=subnet_id)

        # Deleting the VM
        vm.delete(self.api_client, expunge=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_subdomaccess_subdomainuser(
            self):
        """Validate that ROOT admin is able to deploy a VM for subdomain user
        in a shared network with scope=domain with subdomain access
        """

        # Deploy VM as user in a subdomain under  a domain that has shared
        # network with subdomain access
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD111A"]["name"] + \
            "-shared-scope-domain-withsubdomainaccess-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD111A"]["displayname"] + \
            "-shared-scope-domain-withsubdomainaccess-root-admin"

        vm = self.create_VM(self.shared_network_domain_with_subdomain_d11,
                            testdata=self.vmdata, account=self.account_d111a,
                            cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d111a.name and
            vm.domainid == self.account_d111a.domainid,
            True,
            "ROOT admin is not able to deploy a VM for subdomain user in a "
            "shared network with scope=domain with subdomain access")

        self.verify_vsd_shared_network(
            self.account_d111a.domainid,
            self.shared_network_domain_with_subdomain_d11,
            gateway=self.nuagenetworkdata[
                "network_domain_with_subdomain_access"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_domain_with_subdomain_d11.id,
            self.nuagenetworkdata[
                "network_domain_with_subdomain_access"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d111a.domainid,
            self.shared_network_domain_with_subdomain_d11,
            vm, sharedsubnetid=subnet_id)

        # Deleting the VM
        vm.delete(self.api_client, expunge=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_subdomaccess_subdomainadmin(
            self):
        """Validate that ROOT admin is able to deploy a VM for subdomain admin
        user in a shared network with scope=domain with subdomain access
        """

        # Deploy VM as an admin user in a subdomain under  a domain that has
        # shared network with subdomain access
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD111"]["name"] + \
            "-shared-scope-domain-withsubdomainaccess-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD111"]["displayname"] + \
            "-shared-scope-domain-withsubdomainaccess-root-admin"

        vm = self.create_VM(self.shared_network_domain_with_subdomain_d11,
                            testdata=self.vmdata, account=self.account_d111,
                            cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d111.name and
            vm.domainid == self.account_d111.domainid,
            True,
            "ROOT admin is not able to deploy VM for subdomain admin user in "
            "a shared network with scope=domain subdomain access")

        self.verify_vsd_shared_network(
            self.account_d111.domainid,
            self.shared_network_domain_with_subdomain_d11,
            gateway=self.nuagenetworkdata[
                "network_domain_with_subdomain_access"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_domain_with_subdomain_d11.id,
            self.nuagenetworkdata[
                "network_domain_with_subdomain_access"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d111.domainid,
            self.shared_network_domain_with_subdomain_d11,
            vm, sharedsubnetid=subnet_id)

        # Deleting the VM
        vm.delete(self.api_client, expunge=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_subdomaccess_parentdomainuser(
            self):
        """Validate that ROOT admin is NOT able to deploy a VM for parent
        domain user in a shared network with scope=domain with subdomain access
        """

        # Deploy VM as user in parentdomain of a domain that has shared network
        # with subdomain access
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD1A"]["name"] + \
            "-shared-scope-domain-withsubdomainaccess-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD1A"]["displayname"] + \
            "-shared-scope-domain-withsubdomainaccess-root-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_with_subdomain_d11.id,
                accountid=self.account_d1a.name,
                domainid=self.account_d1a.domainid
            )
            self.fail(
                "ROOT admin is NOT able to deploy a VM for parent domain user "
                "in a shared network with scope=domain with subdomain access")
        except Exception as e:
            self.debug(
                "When a user from parent domain deploys a VM in a shared "
                "network with scope=domain with subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when ROOT admin tries to "
                    "deploy a VM for parent domain user in a shared network "
                    "with scope=domain with subdomain access ")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_subdomaccess_parentdomainadmin(
            self):
        """Validate that ROOT admin is NOT able to deploy a VM for parent
        domain admin user in a shared network with scope=domain with subdomain
        access
        """

        # Deploy VM as an admin user in parentdomain of  a domain that has
        # shared network with subdomain access
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD1"]["name"] + \
            "-shared-scope-domain-withsubdomainaccess-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD1"]["displayname"] + \
            "-shared-scope-domain-withsubdomainaccess-root-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_with_subdomain_d11.id,
                accountid=self.account_d1.name,
                domainid=self.account_d1.domainid
            )
            self.fail(
                "ROOT admin is able to deploy VM for parent domain admin user "
                "in a shared network with scope=domain subdomain access ")
        except Exception as e:
            self.debug(
                "When an admin user from parent domain deploys a VM in a "
                "shared network with scope=domain with  subdomain access %s"
                % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when ROOT admin tries to "
                    "deploy a VM for parent domain admin user in a shared "
                    "network with scope=domain with subdomain access ")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_subdomaccess_ROOTuser(self):
        """Validate that ROOT admin is NOT able to deploy a VM for user in ROOT
        domain in a shared network with scope=domain with subdomain access
        """

        # Deploy VM as user in ROOT domain
        self.api_client.connection.apiKey = self.user_roota_apikey
        self.api_client.connection.securityKey = self.user_roota_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmROOTA"]["name"] + \
            "-shared-scope-domain-withsubdomainaccess-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmROOTA"]["displayname"] + \
            "-shared-scope-domain-withsubdomainaccess-root-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_with_subdomain_d11.id,
                accountid=self.account_roota.name,
                domainid=self.account_roota.domainid
            )
            self.fail(
                "ROOT admin is able to deploy a VM for user in ROOT domain in "
                "a shared network with scope=domain with subdomain access")
        except Exception as e:
            self.debug(
                "When a user from ROOT domain deploys a VM in a shared "
                "network with scope=domain with subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when ROOT admin tries to "
                    "deploy a VM for user in ROOT domain in a shared network "
                    "with scope=domain with subdomain access")

    # Test cases relating to deploying Virtual Machine as ROOT admin for other
    # users in shared network with scope=account
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_account_domainuser(self):
        """
        Valiate that ROOT admin is NOT able to deploy a VM for user in the same
        domain but in a different account in a shared network with
        scope=account
        """

        # Deploy VM as user in a domain under the same domain but different
        # account from the account that has a shared network with scope=account
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD111B"]["name"] + \
            "-shared-scope-domain-withsubdomainaccess-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD111B"]["displayname"] + \
            "-shared-scope-domain-withsubdomainaccess-root-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_account_d111a.id,
                accountid=self.account_d111b.name,
                domainid=self.account_d111b.domainid
            )
            self.fail(
                "ROOT admin is able to deploy VM for user in the same domain "
                "but in different account in shared network scope=account")
        except Exception as e:
            self.debug(
                "When a user from same domain but different account deploys "
                "a VM in a shared network with scope=account %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.UNABLE_TO_USE_NETWORK):
                self.fail(
                    "Error message validation failed when ROOT admin tries to "
                    "deploy a VM for user in the same domain but in a "
                    "different account in a shared network with scope=account")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_account_domainadminuser(
            self):
        """Validate that ROOT admin is NOT able to deploy a VM for admin user
        in the same domain but in a different account in a shared network with
        scope=account
        """

        # Deploy VM as admin user for a domain that has an account with shared
        # network with scope=account
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD111"]["name"] + \
            "-shared-scope-domain-withsubdomainaccess-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD111"]["displayname"] + \
            "-shared-scope-domain-withsubdomainaccess-root-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_account_d111a.id,
                accountid=self.account_d111.name,
                domainid=self.account_d111.domainid
            )
            self.fail(
                "ROOT admin is able to deploy VM for admin user in same "
                "domain but in different account in shared network with "
                "scope=account")
        except Exception as e:
            self.debug(
                "When a user from same domain but different account deploys a "
                "VM in a shared network with scope=account %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.UNABLE_TO_USE_NETWORK):
                self.fail(
                    "Error message validation failed when ROOT admin tries to "
                    "deploy a VM for admin user in the same domain but in a "
                    "different account in a shared network with scope=account")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_account_user(self):
        """Validate that ROOT admin is able to deploy a VM for regular user in
        a shared network with scope=account
        """

        # Deploy VM as account with shared network with scope=account
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD111A"]["name"] + \
            "-shared-scope-domain-withsubdomainaccess-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD111A"]["displayname"] + \
            "-shared-scope-domain-withsubdomainaccess-root-admin"

        vm = self.create_VM(self.shared_network_account_d111a,
                            testdata=self.vmdata, account=self.account_d111a,
                            cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d111a.name and
            vm.domainid == self.account_d111a.domainid,
            True,
            "ROOT admin is not able to deploy a VM for regular user in a "
            "shared network with scope=account")

        self.verify_vsd_shared_network(self.account_d111a.domainid,
                                       self.shared_network_account_d111a,
                                       gateway=self.nuagenetworkdata[
                                           "network_account"]["gateway"])
        subnet_id = self.get_subnet_id(self.shared_network_account_d111a.id,
                                       self.nuagenetworkdata[
                                           "network_account"]["gateway"])
        self.verify_vsd_enterprise_vm(self.account_d111a.domainid,
                                      self.shared_network_account_d111a, vm,
                                      sharedsubnetid=subnet_id)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_account_differentdomain(
            self):
        """Validate that ROOT admin is NOT able to deploy a VM for a admin user
        in a shared network with scope=account which the admin user does not
        have access to
        """

        # Deploy VM as an admin user in a subdomain under ROOT
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD2A"]["name"] + \
            "-shared-scope-account-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD2A"]["displayname"] + \
            "-shared-scope-account-root-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_account_d111a.id,
                accountid=self.account_d2a.name,
                domainid=self.account_d2a.domainid
            )
            self.fail(
                "ROOT admin is able to deploy VM for admin user in shared "
                "network scope=account which admin user does not have access")
        except Exception as e:
            self.debug("account %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.UNABLE_TO_USE_NETWORK):
                self.fail(
                    "Error message validation failed when ROOT admin tries to "
                    "deploy a VM for a admin user in a shared network with "
                    "scope=account which the admin user does not have access "
                    "to ")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_admin_scope_account_ROOTuser(self):
        """Validate that ROOT admin is NOT able to deploy a VM for a user in
        ROOT domain in a shared network with scope=account which the user does
        not have access to
        """

        # Deploy VM as user in ROOT domain
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmROOTA"]["name"] + \
            "-shared-scope-account-root-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmROOTA"]["displayname"] + \
            "-shared-scope-account-root-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_account_d111a.id,
                accountid=self.account_roota.name,
                domainid=self.account_roota.domainid
            )
            self.fail(
                "ROOT admin is able to deploy VM for a user in ROOT domain in "
                "shared network scope=account which user does not have access")
        except Exception as e:
            self.debug(
                "When a user from ROOT domain deploys a VM in a shared "
                "network with scope=account %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.UNABLE_TO_USE_NETWORK):
                self.fail(
                    "Error message validation failed when ROOT admin tries to "
                    "deploy a VM for a user in ROOT domain in a shared "
                    "network with scope=account which the user does not have "
                    "access to ")

    # Test cases relating to deploying Virtual Machine as Domain admin for
    # other users in shared network with scope=all
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_all_domainuser(
            self):
        """Validate that Domain admin is able to deploy a VM for a domain user
        in a shared network with scope=all
        """

        # Deploy VM for a user in a domain under ROOT as admin
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD1A"]["name"] + \
            "-shared-scope-all-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD1A"]["displayname"] + \
            "-shared-scope-all-domain-admin"

        vm = self.create_VM(self.shared_network_all, testdata=self.vmdata,
                            account=self.account_d1a, cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d1a.name and
            vm.domainid == self.account_d1a.domainid,
            True,
            "Domain admin is not able to deploy a VM for a domain user in a "
            "shared network with scope=all")

        self.verify_vsd_shared_network(
            self.account_d1a.domainid,
            self.shared_network_all,
            gateway=self.nuagenetworkdata["network_all"]["gateway"])
        subnet_id = self.get_subnet_id(self.shared_network_all.id,
                                       self.nuagenetworkdata["network_all"][
                                           "gateway"])
        self.verify_vsd_enterprise_vm(self.account_d1a.domainid,
                                      self.shared_network_all, vm,
                                      sharedsubnetid=subnet_id)

        # Deleting the VM
        vm.delete(self.api_client, expunge=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_all_domainadmin(
            self):
        """Validate that Domain admin is able to deploy a VM for a domain admin
        user in a shared network with scope=all
        """

        # Deploy VM for an admin user in a domain under ROOT as admin
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD1"]["name"] + \
            "-shared-scope-all-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD1"]["displayname"] + \
            "-shared-scope-all-domain-admin"

        vm = self.create_VM(self.shared_network_all, testdata=self.vmdata,
                            account=self.account_d1, cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d1.name and
            vm.domainid == self.account_d1.domainid,
            True,
            "Domain admin is not able to deploy a VM for a domain admin user "
            "in a shared network with scope=all")

        self.verify_vsd_shared_network(
            self.account_d1.domainid,
            self.shared_network_all,
            gateway=self.nuagenetworkdata["network_all"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_all.id,
            self.nuagenetworkdata["network_all"]["gateway"])
        self.verify_vsd_enterprise_vm(self.account_d1.domainid,
                                      self.shared_network_all, vm,
                                      sharedsubnetid=subnet_id)

        # Deleting the VM
        vm.delete(self.api_client, expunge=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_all_subdomainuser(
            self):
        """Validate that Domain admin is able to deploy a VM for a sub domain
        user in a shared network with scope=all
        """

        # Deploy VM as user in a subdomain under ROOT
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD11A"]["name"] + \
            "-shared-scope-all-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD11A"]["displayname"] + \
            "-shared-scope-all-domain-admin"
        vm = self.create_VM(self.shared_network_all, testdata=self.vmdata,
                            account=self.account_d11a, cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d11a.name and
            vm.domainid == self.account_d11a.domainid,
            True,
            "Domain admin is not able to deploy a VM for a sub domain user in "
            "a shared network with scope=all")

        self.verify_vsd_shared_network(
            self.account_d11a.domainid,
            self.shared_network_all,
            gateway=self.nuagenetworkdata["network_all"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_all.id,
            self.nuagenetworkdata["network_all"]["gateway"])
        self.verify_vsd_enterprise_vm(self.account_d11a.domainid,
                                      self.shared_network_all, vm,
                                      sharedsubnetid=subnet_id)

        # Deleting the VM
        vm.delete(self.api_client, expunge=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_all_subdomainadmin(
            self):
        """Validate that Domain admin is able to deploy a VM for a sub domain
        admin user in a shared network with scope=all
        """

        # Deploy VM as an admin user in a subdomain under ROOT
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD11"]["name"] + \
            "-shared-scope-all-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD11"]["displayname"] + \
            "-shared-scope-all-domain-admin"
        vm = self.create_VM(self.shared_network_all, testdata=self.vmdata,
                            account=self.account_d11, cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d11.name and
            vm.domainid == self.account_d11.domainid,
            True,
            "Domain admin is not able to deploy a VM for a sub domain admin "
            "user in a shared network with scope=all")

        self.verify_vsd_shared_network(
            self.account_d11.domainid,
            self.shared_network_all,
            gateway=self.nuagenetworkdata["network_all"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_all.id,
            self.nuagenetworkdata["network_all"]["gateway"])
        self.verify_vsd_enterprise_vm(self.account_d11.domainid,
                                      self.shared_network_all, vm,
                                      sharedsubnetid=subnet_id)

        # Deleting the VM
        vm.delete(self.api_client, expunge=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_all_ROOTuser(self):
        """Validate that Domain admin is NOT able to deploy a VM for user in
        ROOT domain in a shared network with scope=all
        """

        # Deploy VM as user in ROOT domain
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmROOTA"]["name"] + \
            "-shared-scope-all"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmROOTA"]["displayname"] + \
            "-shared-scope-all"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_all.id,
                accountid=self.account_roota.name,
                domainid=self.account_roota.domainid
            )
            self.fail(
                "Domain admin is NOT able to deploy a VM for user in ROOT "
                "domain in a shared network with scope=all")
        except Exception as e:
            self.debug(
                "When a Domain admin user deploys a VM for ROOT user in a "
                "shared network with scope=all %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NO_PERMISSION_TO_OPERATE_DOMAIN):
                self.fail(
                    "Error message validation failed when Domain admin is NOT "
                    "able to deploy a VM for user in ROOT domain in a shared "
                    "network with scope=all")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_all_crossdomuser(
            self):
        """Validate that Domain admin is NOT able to deploy a VM for user in
        other domain in a shared network with scope=all
        """

        # Deploy VM as user in ROOT domain
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmROOTA"]["name"] + \
            "-shared-scope-all"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmROOTA"]["displayname"] + \
            "-shared-scope-all"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_all.id,
                accountid=self.account_d2a.name,
                domainid=self.account_d2a.domainid
            )
            self.fail(
                "Domain admin user is able to Deploy VM for a domain user he "
                "does not have access to in a shared network with "
                "scope=domain with no subdomain access ")
        except Exception as e:
            self.debug(
                "When a Domain admin user  deploys a VM for a domain user he "
                "does not have access to in a shared network with "
                "scope=domain with no subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NO_PERMISSION_TO_OPERATE_DOMAIN):
                self.fail(
                    "Error mesage validation failed when Domain admin user "
                    "tries to Deploy VM for a domain user he does not have "
                    "access to in a shared network with scope=domain with no "
                    "subdomain access ")

    # Test cases relating to deploying Virtual Machine as Domain admin for
    # other users in shared network with scope=Domain and no subdomain access
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_nosubdomaccess_domuser(
            self):
        """Validate that Domain admin is able to deploy a VM for domain user in
        a shared network with scope=Domain and no subdomain access
        """

        # Deploy VM as user in a domain that has shared network with no
        # subdomain access
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD11A"]["name"] + \
            "-shared-scope-domain-nosubdomainaccess-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD11A"]["displayname"] + \
            "-shared-scope-domain-nosubdomainaccess-domain-admin"

        vm = self.create_VM(self.shared_network_domain_d11,
                            testdata=self.vmdata, account=self.account_d11a,
                            cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d11a.name and
            vm.domainid == self.account_d11a.domainid,
            True,
            "Domain admin is not able to deploy a VM for domain user in a "
            "shared network with scope=Domain and no subdomain access")

        self.verify_vsd_shared_network(
            self.account_d11a.domainid,
            self.shared_network_domain_d11,
            gateway=self.nuagenetworkdata[
                "network_domain_with_no_subdomain_access"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_domain_d11.id,
            self.nuagenetworkdata[
                "network_domain_with_no_subdomain_access"]["gateway"])
        self.verify_vsd_enterprise_vm(self.account_d11a.domainid,
                                      self.shared_network_domain_d11, vm,
                                      sharedsubnetid=subnet_id)

        # Deleting the VM
        vm.delete(self.api_client, expunge=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_nosubdomaccess_domadmin(
            self):
        """Validate that Domain admin is able to deploy a VM for domain admin
        user in a shared network with scope=Domain and no subdomain access
        """

        # Deploy VM as an admin user in a domain that has shared network with
        # no subdomain access
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD11"]["name"] + \
            "-shared-scope-domain-nosubdomainaccess-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD11"]["displayname"] + \
            "-shared-scope-domain-nosubdomainaccess-domain-admin"
        vm = self.create_VM(self.shared_network_domain_d11,
                            testdata=self.vmdata, account=self.account_d11,
                            cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d11.name and
            vm.domainid == self.account_d11.domainid,
            True,
            "Admin User in a  domain that has a shared network with no "
            "subdomain access failed to Deploy VM in a shared network with "
            "scope=domain with no subdomain access")
        self.verify_vsd_shared_network(
            self.account_d11.domainid,
            self.shared_network_domain_d11,
            gateway=self.nuagenetworkdata[
                "network_domain_with_no_subdomain_access"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_domain_d11.id,
            self.nuagenetworkdata[
                "network_domain_with_no_subdomain_access"]["gateway"])
        self.verify_vsd_enterprise_vm(self.account_d11.domainid,
                                      self.shared_network_domain_d11, vm,
                                      sharedsubnetid=subnet_id)

        # Deleting the VM
        vm.delete(self.api_client, expunge=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_nosubdomaccess_subdomain(
            self):
        """Validate that Domain admin is NOT able to deploy a VM for sub domain
        user in a shared network with scope=Domain and no subdomain access
        """

        # Deploy VM as user in a subdomain under  a domain that has shared
        # network with no subdomain access
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD111A"]["name"] + \
            "-shared-scope-domain-nosubdomainaccess-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD111A"]["displayname"] + \
            "-shared-scope-domain-nosubdomainaccess-domain-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id,
                accountid=self.account_d111a.name,
                domainid=self.account_d111a.domainid
            )
            self.fail(
                "Domain admin is able to deploy VM for sub domain user in a "
                "shared network with scope=Domain and no subdomain access")
        except Exception as e:
            self.debug(
                "When a user from a subdomain deploys a VM in a shared "
                "network with scope=domain with no subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when Domain admin tries "
                    "to deploy a VM for sub domain user in a shared network "
                    "with scope=Domain and no subdomain access")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_nosubdomaccess_subdomadm(
            self):
        """Validate that Domain admin is NOT able to deploy a VM for sub domain
        admin user in a shared network with scope=Domain and no subdomain
        access
        """

        # Deploy VM as an admin user in a subdomain under  a domain that has
        # shared network with no subdomain access
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD111"]["name"] + \
            "-shared-scope-domain-nosubdomainaccess-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD111"]["displayname"] + \
            "-shared-scope-domain-nosubdomainaccess-domain-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id,
                accountid=self.account_d111.name,
                domainid=self.account_d111.domainid
            )
            self.fail(
                "Domain admin is able to deploy a VM for sub domain admin "
                "user in a shared network with scope=Domain no subdomain "
                "access")
        except Exception as e:
            self.debug(
                "When a admin user from a subdomain deploys a VM in a shared "
                "network with scope=domain with no subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when Domain admin tries "
                    "to deploy a VM for sub domain admin user in a shared "
                    "network with scope=Domain and no subdomain access ")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_nosubdomaccess_parentdom(
            self):
        """Validate that Domain admin is NOT able to deploy a VM for parent
        domain user in a shared network with scope=Domain and no subdomain
        access
        """

        # Deploy VM as user in parentdomain of a domain that has shared network
        # with no subdomain access
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD1A"]["name"] + \
            "-shared-scope-domain-nosubdomainaccess-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD1A"]["displayname"] + \
            "-shared-scope-domain-nosubdomainaccess-domain-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id,
                accountid=self.account_d1a.name,
                domainid=self.account_d1a.domainid
            )
            self.fail(
                "Domain admin is able to deploy a VM for parent domain user "
                "in a shared network with scope=Domain and no subdomain "
                "access")
        except Exception as e:
            self.debug(
                "When a user from parent domain deploys a VM in a shared "
                "network with scope=domain with no subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when Domain admin tries "
                    "to deploy a VM for parent domain user in a shared "
                    "network with scope=Domain and no subdomain access ")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_nosubdomaccess_pardomadm(
            self):
        """Validate that Domain admin is NOT able to deploy VM for parent
        domain admin user in shared network with scope=Domain and no subdomain
        access
        """

        # Deploy VM as an admin user in parentdomain of  a domain that has
        # shared network with no subdomain access
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD1"]["name"] + \
            "-shared-scope-domain-nosubdomainaccess-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD1"]["displayname"] + \
            "-shared-scope-domain-nosubdomainaccess-domain-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id,
                accountid=self.account_d1.name,
                domainid=self.account_d1.domainid
            )
            self.fail(
                "Domain admin is able to deploy VM for parent domain admin "
                "user in a shared network with scope=Domain no subdomain "
                "access")
        except Exception as e:
            self.debug(
                "When an admin user from parent domain deploys a VM in a "
                "shared network with scope=domain with no subdomain access %s"
                % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when Domain admin tries "
                    "to deploy a VM for parent domain admin user in a shared "
                    "network with scope=Domain and no subdomain access ")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_nosubdomaccess_ROOTuser(
            self):
        """Validate that Domain admin is NOT able to deploy a VM for user in
        ROOT domain in a shared network with scope=Domain and no subdomain
        access
        """

        # Deploy VM as user in ROOT domain
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmROOTA"]["name"] + \
            "-shared-scope-domain-nosubdomainaccess-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmROOTA"]["displayname"] + \
            "-shared-scope-domain-nosubdomainaccess-domain-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_d11.id,
                accountid=self.account_roota.name,
                domainid=self.account_roota.domainid
            )
            self.fail(
                "Domain admin is able to deploy a VM for user in ROOT domain "
                "in a shared network with scope=Domain and no subdomain "
                "access")
        except Exception as e:
            self.debug(
                "When a regular user from ROOT domain deploys a VM in a "
                "shared network with scope=domain with no subdomain access %s"
                % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NO_PERMISSION_TO_OPERATE_DOMAIN):
                self.fail(
                    "Error message validation failed when Domain admin tries "
                    "to deploy a VM for user in ROOT domain in a shared "
                    "network with scope=Domain and no subdomain access")

    # Test cases relating to deploying Virtual Machine as Domain admin for
    # other users in shared network with scope=Domain and with subdomain access
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_subdomaccess_domainuser(
            self):
        """Validate that Domain admin is able to deploy a VM for regular user
        in domain in a shared network with scope=Domain and subdomain access
        """

        # Deploy VM as user in a domain that has shared network with subdomain
        # access
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD11A"]["name"] + \
            "-shared-scope-domain-withsubdomainaccess-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD11A"]["displayname"] + \
            "-shared-scope-domain-withsubdomainaccess-domain-admin"

        vm = self.create_VM(self.shared_network_domain_with_subdomain_d11,
                            testdata=self.vmdata, account=self.account_d11a,
                            cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d11a.name and
            vm.domainid == self.account_d11a.domainid,
            True,
            "Domain admin is not able to deploy VM for regular user in domain "
            "in a shared network with scope=Domain subdomain access")
        self.verify_vsd_shared_network(
            self.account_d11a.domainid,
            self.shared_network_domain_with_subdomain_d11,
            gateway=self.nuagenetworkdata[
                "network_domain_with_subdomain_access"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_domain_with_subdomain_d11.id,
            self.nuagenetworkdata[
                "network_domain_with_subdomain_access"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11a.domainid,
            self.shared_network_domain_with_subdomain_d11,
            vm, sharedsubnetid=subnet_id)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_subdomaccess_domainadmin(
            self):
        """Validate that Domain admin is able to deploy a VM for admin user in
        domain in a shared network with scope=Domain and subdomain access
        """

        # Deploy VM as an admin user in a domain that has shared network with
        # subdomain access
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD11"]["name"] + \
            "-shared-scope-domain-withsubdomainaccess-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD11"]["displayname"] + \
            "-shared-scope-domain-withsubdomainaccess-domain-admin"

        vm = self.create_VM(self.shared_network_domain_with_subdomain_d11,
                            testdata=self.vmdata, account=self.account_d11,
                            cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d11.name and
            vm.domainid == self.account_d11.domainid,
            True,
            "Domain admin is not able to deploy a VM for admin user in "
            "domain in a shared network with scope=Domain subdomain access")
        self.verify_vsd_shared_network(
            self.account_d11.domainid,
            self.shared_network_domain_with_subdomain_d11,
            gateway=self.nuagenetworkdata[
                "network_domain_with_subdomain_access"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_domain_with_subdomain_d11.id,
            self.nuagenetworkdata[
                "network_domain_with_subdomain_access"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11.domainid,
            self.shared_network_domain_with_subdomain_d11,
            vm, sharedsubnetid=subnet_id)

        # Deleting the VM
        vm.delete(self.api_client, expunge=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_subdomaccess_subdomain(
            self):
        """Validate that Domain admin is able to deploy a VM for regular user
        in subdomain in a shared network with scope=Domain and subdomain access
        """

        # Deploy VM as user in a subdomain under  a domain that has shared
        # network with subdomain access
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD111A"]["name"] + \
            "-shared-scope-domain-withsubdomainaccess-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD111A"]["displayname"] + \
            "-shared-scope-domain-withsubdomainaccess-domain-admin"

        vm = self.create_VM(self.shared_network_domain_with_subdomain_d11,
                            testdata=self.vmdata, account=self.account_d111a,
                            cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d111a.name and
            vm.domainid == self.account_d111a.domainid,
            True,
            "Domain admin not able to deploy VM for regular user in subdomain "
            "in shared network with scope=Domain subdomain access")
        self.verify_vsd_shared_network(
            self.account_d111a.domainid,
            self.shared_network_domain_with_subdomain_d11,
            gateway=self.nuagenetworkdata[
                "network_domain_with_subdomain_access"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_domain_with_subdomain_d11.id,
            self.nuagenetworkdata[
                "network_domain_with_subdomain_access"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d111a.domainid,
            self.shared_network_domain_with_subdomain_d11,
            vm, sharedsubnetid=subnet_id)

        # Deleting the VM
        vm.delete(self.api_client, expunge=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_subdomaccess_subdomadm(
            self):
        """Validate that Domain admin is able to deploy a VM for admin user in
        subdomain in a shared network with scope=Domain and subdomain access
        """

        # Deploy VM as an admin user in a subdomain under  a domain that has
        # shared network with subdomain access
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD111"]["name"] + \
            "-shared-scope-domain-withsubdomainaccess-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD111"]["displayname"] + \
            "-shared-scope-domain-withsubdomainaccess-domain-admin"

        vm = self.create_VM(self.shared_network_domain_with_subdomain_d11,
                            testdata=self.vmdata, account=self.account_d111,
                            cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d111.name and
            vm.domainid == self.account_d111.domainid,
            True,
            "Domain admin is not able to deploy VM for admin user in "
            "subdomain in a shared network with scope=Domain subdomain access")
        self.verify_vsd_shared_network(
            self.account_d111.domainid,
            self.shared_network_domain_with_subdomain_d11,
            gateway=self.nuagenetworkdata[
                "network_domain_with_subdomain_access"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_domain_with_subdomain_d11.id,
            self.nuagenetworkdata[
                "network_domain_with_subdomain_access"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d111.domainid,
            self.shared_network_domain_with_subdomain_d11,
            vm, sharedsubnetid=subnet_id)

        # Deleting the VM
        vm.delete(self.api_client, expunge=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_subdomaccess_parentdom(
            self):
        """Validate that Domain admin NOT able to deploy VM for regular user in
        parent domain in shared network with scope=Domain subdomain access
        """

        # Deploy VM as user in parentdomain of a domain that has shared network
        # with subdomain access
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD1A"]["name"] + \
            "-shared-scope-domain-withsubdomainaccess-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD1A"]["displayname"] + \
            "-shared-scope-domain-withsubdomainaccess-domain-admin"

        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_with_subdomain_d11.id,
                accountid=self.account_d1a.name,
                domainid=self.account_d1a.domainid
            )
            self.fail(
                " Domain admin is able to deploy VM for regular user in "
                "parent domain in a shared network with scope=Domain "
                "subdomain access")
        except Exception as e:
            self.debug(
                "When a user from parent domain deploys a VM in a shared "
                "network with scope=domain with subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when Domain admin tries "
                    "to deploy a VM for regular user in parent domain in a "
                    "shared network with scope=Domain and subdomain access")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_subdomaccess_pardomadmin(
            self):
        """Validate that Domain admin is NOT able to deploy VM for admin user
        in parent domain in shared network with scope=Domain subdomain access
        """

        # Deploy VM as an admin user in parentdomain of  a domain that has
        # shared network with subdomain access
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD1"]["name"] + \
            "-shared-scope-domain-withsubdomainaccess-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD1"]["displayname"] + \
            "-shared-scope-domain-withsubdomainaccess-domain-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_with_subdomain_d11.id,
                accountid=self.account_d1.name,
                domainid=self.account_d1.domainid
            )
            self.fail(
                "Domain admin is able to deploy a VM for admin user in parent "
                "domain in a shared network with scope=Domain subdomain "
                "access")
        except Exception as e:
            self.debug(
                "When an admin user from parent domain deploys a VM in a "
                "shared network with scope=domain with  subdomain access %s"
                % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NOT_AVAILABLE_IN_DOMAIN):
                self.fail(
                    "Error message validation failed when Domain admin tries "
                    "to deploy a VM for admin user in parent domain in a "
                    "shared network with scope=Domain and subdomain access")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_subdomaccess_ROOTuser(
            self):
        """Validate that Domain admin is NOT able to deploy a VM for user in
        ROOT domain in a shared network with scope=Domain and subdomain access
        """

        # Deploy VM as user in ROOT domain
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmROOTA"]["name"] + \
            "-shared-scope-domain-withsubdomainaccess-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmROOTA"]["displayname"] + \
            "-shared-scope-domain-withsubdomainaccess-domain-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_domain_with_subdomain_d11.id,
                accountid=self.account_roota.name,
                domainid=self.account_roota.domainid
            )
            self.fail(
                "Domain admin is able to deploy a VM for user in ROOT domain "
                "in a shared network with scope=Domain and subdomain access")
        except Exception as e:
            self.debug(
                "When a user from ROOT domain deploys a VM in a shared "
                "network with scope=domain with subdomain access %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NO_PERMISSION_TO_OPERATE_DOMAIN):
                self.fail(
                    "Error message validation failed when Domain admin tries "
                    "to deploy a VM for user in ROOT domain in a shared "
                    "network with scope=Domain and subdomain access")

    # Test cases relating to deploying Virtual Machine as Domain admin for
    # other users in shared network with scope=account
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_account_domainuser(
            self):
        """Validate that Domain admin is NOT able to deploy a VM for user in
        the same domain but belonging to a different account in a shared
        network with scope=account
        """

        # Deploy VM as user in a domain under the same domain but different
        # account from the acount that has a shared network with scope=account
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD111B"]["name"] + \
            "-shared-scope-domain-withsubdomainaccess-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD111B"]["displayname"] + \
            "-shared-scope-domain-withsubdomainaccess-domain-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_account_d111a.id,
                accountid=self.account_d111b.name,
                domainid=self.account_d111b.domainid
            )
            self.fail(
                "Domain admin is able to deploy a VM for user in the same "
                "domain but belonging to a different account in a shared "
                "network with scope=account")
        except Exception as e:
            self.debug(
                "When a user from same domain but different account deploys "
                "a VM in a shared network with scope=account %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.UNABLE_TO_USE_NETWORK):
                self.fail(
                    "Error message validation failed when Domain admin tries "
                    "to deploy a VM for user in the same domain but belonging "
                    "to a different account in a shared network with "
                    "scope=account")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_account_domainadm(
            self):
        """Validate that Domain admin is NOT able to deploy a VM for an admin
        user in the same domain but belonging to a different account in a
        shared network with scope=account
        """

        # Deploy VM as admin user for a domain that has an account with shared
        # network with scope=account
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD111"]["name"] + \
            "-shared-scope-domain-withsubdomainaccess-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD111"]["displayname"] + \
            "-shared-scope-domain-withsubdomainaccess-domain-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_account_d111a.id,
                accountid=self.account_d111.name,
                domainid=self.account_d111.domainid
            )
            self.fail(
                "Domain admin is able to deploy a VM for user in the same "
                "domain but belonging to a different account in a shared "
                "network with scope=account")
        except Exception as e:
            self.debug(
                "When a user from same domain but different account deploys a "
                "VM in a shared network with scope=account %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.UNABLE_TO_USE_NETWORK):
                self.fail(
                    "Error message validation failed when Domain admin tries "
                    "to deploy a VM for user in the same domain but belonging "
                    "to a different account in a shared network with "
                    "scope=account")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_account_user(self):
        """Validate that Domain admin is able to deploy a VM for an regular
        user in a shared network with scope=account
        """

        # Deploy VM as account with shared network with scope=account
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD111A"]["name"] + \
            "-shared-scope-domain-withsubdomainaccess-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD111A"]["displayname"] + \
            "-shared-scope-domain-withsubdomainaccess-domain-admin"

        vm = self.create_VM(self.shared_network_account_d111a,
                            testdata=self.vmdata, account=self.account_d111a,
                            cleanup=False)

        self.assertEqual(
            vm.state == "Running" and vm.account == self.account_d111a.name and
            vm.domainid == self.account_d111a.domainid,
            True,
            "Domain admin is not able to deploy a VM for an regular user in "
            "a shared network with scope=account")
        self.verify_vsd_shared_network(self.account_d111a.domainid,
                                       self.shared_network_account_d111a,
                                       gateway=self.nuagenetworkdata[
                                           "network_account"]["gateway"])
        subnet_id = self.get_subnet_id(self.shared_network_account_d111a.id,
                                       self.nuagenetworkdata[
                                           "network_account"]["gateway"])
        self.verify_vsd_enterprise_vm(self.account_d111a.domainid,
                                      self.shared_network_account_d111a, vm,
                                      sharedsubnetid=subnet_id)

        # Deleting the VM
        vm.delete(self.api_client, expunge=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_account_diffdom(
            self):
        """Validate that Domain admin is NOT able to deploy a VM for an
        regular user from a differnt domain in a shared network with
        scope=account
        """

        # Deploy VM as an admin user in a subdomain under ROOT
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD2A"]["name"] + \
            "-shared-scope-account-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD2A"]["displayname"] + \
            "-shared-scope-account-domain-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_account_d111a.id,
                accountid=self.account_d2a.name,
                domainid=self.account_d2a.domainid
            )
            self.fail(
                "Domain admin is able able to deploy a VM for an regular "
                "user from a differnt domain in a shared network with "
                "scope=account")
        except Exception as e:
            self.debug(
                "When a user from different domain deploys a VM in a shared "
                "network with scope=account %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NO_PERMISSION_TO_OPERATE_DOMAIN):
                self.fail(
                    "Error message validation failed when Domain admin tries "
                    "to deploy a VM for an regular user from a differnt "
                    "domain in a shared network with scope=account")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_domainadmin_scope_account_ROOTuser(
            self):
        """Validate that Domain admin is NOT able to deploy a VM for an regular
        user in ROOT domain in a shared network with scope=account
        """

        # Deploy VM as user in ROOT domain
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmROOTA"]["name"] + \
            "-shared-scope-account-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmROOTA"]["displayname"] + \
            "-shared-scope-account-domain-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_account_d111a.id,
                accountid=self.account_roota.name,
                domainid=self.account_roota.domainid
            )
            self.fail(
                "Domain admin is able to deploy a VM for an regular user in "
                "ROOT domain in a shared network with scope=account")
        except Exception as e:
            self.debug(
                "When a user from ROOT domain deploys a VM in a shared "
                "network with scope=account %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NO_PERMISSION_TO_OPERATE_DOMAIN):
                self.fail(
                    "Error message validation failed when Domain admin tries "
                    "to deploy a VM for an regular user in ROOT domain in a "
                    "shared network with scope=account")

    # Test cases relating to deploying Virtual Machine as Regular user for
    # other users in shared network with scope=all
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_regularuser_scope_all_anotheruser(
            self):
        """Validate that regular user is NOT able to deploy a VM for
        another user in the same  domain in a shared network with scope=all
        """

        # Deploy VM for a user in a domain under ROOT as admin
        self.api_client.connection.apiKey = self.user_d11a_apikey
        self.api_client.connection.securityKey = self.user_d11a_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD11A"]["name"] + \
            "-shared-scope-all-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD11A"]["displayname"] + \
            "-shared-scope-all-domain-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_all.id,
                accountid=self.account_d12a.name,
                domainid=self.account_d12a.domainid
            )
            self.fail(
                "Regular user is allowed to deploy a VM for another user in "
                "the same domain in a shared network with scope=all")
        except Exception as e:
            self.debug(
                "When a regular user deploys a VM for another user in the "
                "same domain in a shared network with scope=all %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NO_PERMISSION_TO_OPERATE_ACCOUNT):
                self.fail(
                    "Error message validation failed when Regular user tries "
                    "to deploy a VM for another user in the same domain in a "
                    "shared network with scope=all")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_deployVM_in_sharedNetwork_as_regularuser_scope_all_crossdomain(
            self):
        """Validate that regular user is NOT able to deploy a VM for
        another user in a different domain in a shared network with scope=all
        """

        # Deploy VM for a user in a domain under ROOT as admin
        self.api_client.connection.apiKey = self.user_d11a_apikey
        self.api_client.connection.securityKey = self.user_d11a_secretkey
        self.vmdata["name"] = \
            self.sharednetworkdata["vmD11A"]["name"] + \
            "-shared-scope-all-domain-admin"
        self.vmdata["displayname"] = \
            self.sharednetworkdata["vmD11A"]["displayname"] + \
            "-shared-scope-all-domain-admin"
        try:
            VirtualMachine.create(
                self.api_client,
                self.vmdata,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=self.shared_network_all.id,
                accountid=self.account_d2a.name,
                domainid=self.account_d2a.domainid
            )
            self.fail(
                "Regular user is allowed to deploy a VM for another user in "
                "the same domain in a shared network with scope=all")
        except Exception as e:
            self.debug(
                "When a regular user deploys a VM for another user in the "
                "same domain in a shared network with scope=all %s" % e)
            if not CloudstackAclException.verifyMsginException(
                    e,
                    CloudstackAclException.NO_PERMISSION_TO_OPERATE_ACCOUNT):
                self.fail(
                    "Error message validation failed when Regular user tries "
                    "to deploy a VM for another user in the same domain in a "
                    "shared network with scope=all")

    @staticmethod
    def generateKeysForUser(api_client, account):
        user = User.list(
            api_client,
            account=account.name,
            domainid=account.domainid
        )[0]

        return (User.registerUserKeys(
            api_client,
            user.id
        ))
