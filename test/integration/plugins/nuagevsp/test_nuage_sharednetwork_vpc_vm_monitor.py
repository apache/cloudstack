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
VPC Virtual Machine Monitoring
"""
# Import Local Modules
from nuageTestCase import nuageTestCase
from marvin.lib.utils import cleanup_resources, validateList
from marvin.lib.base import (VPC,
                             Account,
                             Domain,
                             User,
                             VirtualMachine,
                             Network,
                             NetworkOffering,
                             VpcOffering)
from marvin.lib.common import list_virtual_machines
from marvin.codes import PASS
# Import System modules
from nose.plugins.attrib import attr


class TestNuageSharedNetworkVpcVmMonitor(nuageTestCase):
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

        super(TestNuageSharedNetworkVpcVmMonitor, cls).setUpClass()
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
            cls.domain_12 = Domain.create(
                cls.api_client,
                cls.sharednetworkdata["domain12"],
                parentdomainid=cls.domain_1.id
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
                domainid=cls.domain_11.id,
                accountid=cls.account_d11a.user[0].username
            )

            cls.debug("Creating Nuage VSP VPC offering...")
            cls.vpc_offering = VpcOffering.create(cls.api_client,
                                                  cls.test_data["nuagevsp"][
                                                      "vpc_offering"]
                                                  )

            cls.vpc_offering.update(cls.api_client, state="Enabled")

            # Creating a VPC
            cls.debug("Creating a VPC with Nuage VSP VPC offering...")
            cls.test_data["vpc"]["cidr"] = '10.1.0.0/16'
            cls.vpcd11 = VPC.create(cls.api_client,
                                    cls.test_data["vpc"],
                                    vpcofferingid=cls.vpc_offering.id,
                                    zoneid=cls.zone.id,
                                    account=cls.account_d11a.name,
                                    domainid=cls.account_d11a.domainid
                                    )

            # Creating a network offering
            cls.debug("Creating Nuage Vsp VPC Network offering...")
            cls.network_offering = NetworkOffering.create(
                cls.api_client,
                cls.test_data["nuagevsp"]["vpc_network_offering"],
                conservemode=False
            )
            cls.network_offering.update(cls.api_client, state="Enabled")

            # Creating two VPC network in the VPC
            cls.debug(
                "Creating a VPC network with Nuage Vsp VPC Network "
                "offering...")
            cls.test_data["network"]["netmask"] = "255.255.255.0"
            cls.vpc_network1 = Network.create(
                cls.api_client,
                cls.test_data["network"],
                accountid=cls.account_d11a.name,
                domainid=cls.account_d11a.domainid,
                networkofferingid=cls.network_offering.id,
                zoneid=cls.zone.id,
                gateway="10.1.1.1",
                vpcid=cls.vpcd11.id
            )

            cls.debug(
                "Creating a VPC Tier2 network with Nuage Vsp VPC Network "
                "offering...")
            cls.test_data["network"]["name"] = "Tier2"
            cls.vpc_network2 = Network.create(
                cls.api_client,
                cls.test_data["network"],
                accountid=cls.account_d11a.name,
                domainid=cls.account_d11a.domainid,
                networkofferingid=cls.network_offering.id,
                zoneid=cls.zone.id,
                gateway="10.1.2.1",
                vpcid=cls.vpcd11.id
            )

            cls.vmdata = {"name": "vmvpc1",
                          "displayname": "vmvpc1"
                          }

            # Deploying a VM in the VPC network
            cls.vmvpc1 = VirtualMachine.create(
                cls.api_client,
                cls.vmdata,
                zoneid=cls.zone.id,
                serviceofferingid=cls.service_offering.id,
                templateid=cls.template.id,
                networkids=cls.vpc_network1.id,
                accountid=cls.account_d11a.name,
                domainid=cls.account_d11a.domainid
            )

            cls.vmdata = {"name": "vmvpc2",
                          "displayname": "vmvpc2"
                          }

            cls.vmvpc2 = VirtualMachine.create(
                cls.api_client,
                cls.vmdata,
                zoneid=cls.zone.id,
                serviceofferingid=cls.service_offering.id,
                templateid=cls.template.id,
                networkids=cls.vpc_network2.id,
                accountid=cls.account_d11a.name,
                domainid=cls.account_d11a.domainid
            )

            cls._cleanup = [
                cls.account_root,
                cls.account_roota,
                cls.shared_network_all,
                cls.shared_network_offering,
                cls.network_offering,
                cls.vpc_offering,
                cls.service_offering,
            ]
        except Exception as e:
            cls.domain_1.delete(cls.api_client, cleanup="true")
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

    def verify_nic(self, network, vm):
        """verify required nic is present in the VM"""
        self.debug(
            "Going to verify if %s Network nic is present in virtual machine "
            "%s" % (network.name, vm.id))
        vm_list = list_virtual_machines(self.api_client, id=vm.id)
        vm_list_validation_result = validateList(vm_list)
        self.assertEqual(vm_list_validation_result[0], PASS,
                         "vm list validation failed due to %s" %
                         vm_list_validation_result[2])
        self.debug("virtual machine nics: %s" % vm_list[0].nic)
        # filter nic of virtual machine based on Network
        nics = [x for x in vm_list[0].nic if x.networkid == network.id]
        self.debug("Filtered nics list: %s:" % nics)
        if len(nics) == 1:
            return True
        else:
            return False

    # Test cases relating to MonitorVM through SharedNetwork
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_01_addNic_in_sharedNetwork_scope_all_as_domainuser(self):
        """Validate that Normal user in the same domain able to add NIC in a
        shared network with scope=all
        """

        # Deploy VM for a user in a domain under ROOT as admin

        self.api_client.connection.apiKey = self.user_d11a_apikey
        self.api_client.connection.securityKey = self.user_d11a_secretkey
        self.debug("Adding NIC of shared Network as user d11a")

        self.vmvpc1.add_nic(self.api_client, self.shared_network_all.id)

        if self.verify_nic(self.shared_network_all, self.vmvpc1):
            self.debug(
                "virtual machine has NIC is SharedNetwork: %s" %
                self.shared_network_all.name)
        else:
            self.fail(
                "Expected network %s NIC is not present in the virtual "
                "Machine %s" %
                (self.shared_network_all.name, self.vmvpc1.id))

        self.debug("validating Network and VM on VSD")
        self.verify_vsd_shared_network(
            self.account_d11a.domainid,
            self.shared_network_all,
            gateway=self.nuagenetworkdata["network_all"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_all.id,
            self.nuagenetworkdata["network_all"]["gateway"])
        self.verify_vsd_enterprise_vm(self.account_d11a.domainid,
                                      self.shared_network_all, self.vmvpc1,
                                      sharedsubnetid=subnet_id)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_02_addNic_in_sharedNetwork_scope_all_as_domain_parentAdmin(self):
        """Validate that Parent domain admin is able to add a NIC  in a shared
        network with scope=all
        """

        # Add NIC as D1 user
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.debug("Adding NIC of shared Network as user d1")

        self.vmvpc2.add_nic(self.api_client, self.shared_network_all.id)

        if self.verify_nic(self.shared_network_all, self.vmvpc2):
            self.debug(
                "virtual machine has NIC is SharedNetwork: %s" %
                self.shared_network_all.name)
        else:
            self.fail(
                "Expected network %s NIC is not present in the virtual "
                "Machine %s" %
                (self.shared_network_all.name, self.vmvpc2.id))

        self.debug("validating Network and VM on VSD")
        self.verify_vsd_shared_network(
            self.account_d1.domainid,
            self.shared_network_all,
            gateway=self.nuagenetworkdata["network_all"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_all.id,
            self.nuagenetworkdata["network_all"]["gateway"])
        self.verify_vsd_enterprise_vm(self.account_d11a.domainid,
                                      self.shared_network_all, self.vmvpc2,
                                      sharedsubnetid=subnet_id)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_03_staticNat_in_VPC_secondNic_sharedNetwork_scope_all(self):
        """Validate that User can enable staticNat on VPC NIC where second nic
        is in a shared network with scope=all
        """

        self.api_client.connection.apiKey = self.user_d11a_apikey
        self.api_client.connection.securityKey = self.user_d11a_secretkey
        self.debug("Enabling StaticNat as user d11a")
        public_ip_1 = self.acquire_PublicIPAddress(self.vpc_network1,
                                                   self.vpcd11,
                                                   account=self.account_d11a)
        self.create_StaticNatRule_For_VM(self.vmvpc1, public_ip_1,
                                         self.vpc_network1)
        self.validate_PublicIPAddress(public_ip_1, self.vpc_network1,
                                      static_nat=True, vm=self.vmvpc1)
        subnet_id = self.get_subnet_id(
            self.shared_network_all.id,
            self.nuagenetworkdata["network_all"]["gateway"])
        self.verify_vsd_enterprise_vm(self.account_d11a.domainid,
                                      self.shared_network_all, self.vmvpc2,
                                      sharedsubnetid=subnet_id)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_04_rebootVM_after_sharedNetwork_nic(self):
        """Validate that reboot VM is done successfully without any Error
        """

        # Deploy VM for a user in a domain under ROOT as admin
        self.api_client.connection.apiKey = self.user_d11a_apikey
        self.api_client.connection.securityKey = self.user_d11a_secretkey

        self.debug("Rebooting VMs as user d1")

        try:
            self.vmvpc1.reboot(self.api_client)
            self.vmvpc2.reboot(self.api_client)
        except Exception as e:
            self.fail("Failed to reboot the virtual instances, %s" % e)

        self.debug("validating VM on VSD")
        subnet_id = self.get_subnet_id(
            self.shared_network_all.id,
            self.nuagenetworkdata["network_all"]["gateway"])
        self.verify_vsd_enterprise_vm(self.account_d11a.domainid,
                                      self.shared_network_all, self.vmvpc1,
                                      sharedsubnetid=subnet_id)
        self.verify_vsd_enterprise_vm(self.account_d11a.domainid,
                                      self.shared_network_all, self.vmvpc2,
                                      sharedsubnetid=subnet_id)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_05_restart_Tier_VPC_Network_sharedNetwork_nic(self):
        """Validate that restart Tier Network is done successfully with cleanup
        """

        # Deploy VM for a user in a domain under ROOT as admin
        self.api_client.connection.apiKey = self.user_d11a_apikey
        self.api_client.connection.securityKey = self.user_d11a_secretkey

        self.debug("Restarting Tier Networks and VPC")

        self.vpc_network1.restart(self.api_client, cleanup=True)
        self.vpc_network2.restart(self.api_client, cleanup=True)
        self.restart_Vpc(self.vpcd11, cleanup=False)

        self.debug("validating VM on VSD")
        subnet_id = self.get_subnet_id(
            self.shared_network_all.id,
            self.nuagenetworkdata["network_all"]["gateway"])
        self.verify_vsd_enterprise_vm(self.account_d11a.domainid,
                                      self.shared_network_all, self.vmvpc1,
                                      sharedsubnetid=subnet_id)
        self.verify_vsd_enterprise_vm(self.account_d11a.domainid,
                                      self.shared_network_all, self.vmvpc2,
                                      sharedsubnetid=subnet_id)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_06_restart_sharedNetwork_scope_all(self):
        """Validate that restart Shared Network is done successfully without
        any Error
        """

        self.debug("Restarting shared Network with cleanup")
        self.shared_network_all.restart(self.api_client, cleanup=True)

        self.debug("validating SharedNetwork on VSD")
        self.verify_vsd_shared_network(
            self.account_d11a.domainid,
            self.shared_network_all,
            gateway=self.nuagenetworkdata["network_all"]["gateway"])

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_07_removeNic_in_sharedNetwork_scope_all_as_domainuser(self):
        """Validate that Normal user in the same domain able to remove NIC in a
        shared network which is added by Parent Domain Admin
        """

        self.api_client.connection.apiKey = self.user_d11a_apikey
        self.api_client.connection.securityKey = self.user_d11a_secretkey

        self.debug("Removing NIC of shared Network as user d11a")

        vm_list = list_virtual_machines(self.api_client, id=self.vmvpc2.id)
        vm_list_validation_result = validateList(vm_list)
        self.assertEqual(vm_list_validation_result[0], PASS,
                         "vm list validation failed due to %s" %
                         vm_list_validation_result[2])
        self.debug("virtual machine nics: %s" % vm_list[0].nic)
        for nic in vm_list[0].nic:
            if nic.networkid == self.shared_network_all.id:
                reqNic = nic

        self.vmvpc2.remove_nic(self.api_client, reqNic.id)
        if not self.verify_nic(self.shared_network_all, self.vmvpc2):
            self.debug(
                "virtual machine has NIC is SharedNetwork: %s" %
                self.shared_network_all.name)
        else:
            self.fail("network %s NIC is present in the virtual Machine %s" %
                      (self.shared_network_all.name, self.vmvpc2.id))

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_08_removeNic_in_sharedNetwork_scope_all_as_domain_parentAdmin(
            self):
        """Validate that Parent domain admin is able to remove a NIC  which is
        added by child domain user
        """

        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey
        self.debug("Removing NIC od shared Network as user d1")

        vm_list = list_virtual_machines(self.api_client, id=self.vmvpc1.id)
        vm_list_validation_result = validateList(vm_list)
        self.assertEqual(vm_list_validation_result[0], PASS,
                         "vm list validation failed due to %s" %
                         vm_list_validation_result[2])
        self.debug("virtual machine nics: %s" % vm_list[0].nic)
        for nic in vm_list[0].nic:
            if nic.networkid == self.shared_network_all.id:
                reqNic = nic

        self.vmvpc1.remove_nic(self.api_client, reqNic.id)

        if not self.verify_nic(self.shared_network_all, self.vmvpc1):
            self.debug(
                "virtual machine has mot NIC is SharedNetwork: %s" %
                self.shared_network_all.name)
        else:
            self.fail("network %s NIC is present in the virtual Machine %s" %
                      (self.shared_network_all.name, self.vmvpc1.id))

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_09_addNic_in_sharedNetwork_scope_domain_as_domainuser(self):
        """Validate that Normal user in the same domain able to add NIC in a
        shared network with scope=domain without subdomain Access
        """

        # Deploy VM for a user in a domain under ROOT as admin
        self.api_client.connection.apiKey = self.user_d11a_apikey
        self.api_client.connection.securityKey = self.user_d11a_secretkey
        self.debug("Adding NIC of shared Network as user d11a")

        self.vmvpc1.add_nic(self.api_client, self.shared_network_domain_d11.id)

        if self.verify_nic(self.shared_network_domain_d11, self.vmvpc1):
            self.debug(
                "virtual machine has NIC is SharedNetwork: %s" %
                self.shared_network_domain_d11.name)
        else:
            self.fail(
                "Expected network %s NIC is not present in the virtual "
                "Machine %s" %
                (self.shared_network_domain_d11.name, self.vmvpc1.id))

        self.debug("validating Network and VM on VSD")
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
                                      self.shared_network_domain_d11,
                                      self.vmvpc1, sharedsubnetid=subnet_id)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_10_addNic_in_sharedNetwork_scope_domain_subdomain_as_domainuser(
            self):
        """Validate that Normal user in the same domain able to add NIC in a
        shared network with scope=domain with subdomain Access
        """

        # Deploy VM for a user in a domain under ROOT as admin
        self.api_client.connection.apiKey = self.user_d11a_apikey
        self.api_client.connection.securityKey = self.user_d11a_secretkey
        self.debug("Adding NIC of shared Network as user d11a")

        self.vmvpc2.add_nic(self.api_client,
                            self.shared_network_domain_with_subdomain_d11.id)

        if self.verify_nic(self.shared_network_domain_with_subdomain_d11,
                           self.vmvpc2):
            self.debug(
                "virtual machine has NIC is SharedNetwork: %s" %
                self.shared_network_domain_with_subdomain_d11.name)
        else:
            self.fail(
                "Expected network %s NIC is not present in the virtual "
                "Machine %s" %
                (self.shared_network_domain_with_subdomain_d11.name,
                 self.vmvpc2.id))

        self.debug("validating Network and VM on VSD")
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
            self.vmvpc2, sharedsubnetid=subnet_id)

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
