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

""" Component tests for Shared Network functionality with Nuage VSP SDN plugin:
Non Public Shared Network IP Range
"""
# Import Local Modules
from nuageTestCase import nuageTestCase
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import Account, Domain, User, Network, NetworkOffering
from marvin.lib.common import list_virtual_machines
from marvin.cloudstackAPI import (createVlanIpRange,
                                  listVlanIpRanges,
                                  deleteVlanIpRange,
                                  updateVmNicIp)
# Import System Modules
from nose.plugins.attrib import attr


class TestNuageSharedNetworkNonPublicIPRange(nuageTestCase):
    """Test Shared Network functionality with Nuage VSP SDN plugin:
    Non Public Shared Network IP Range
    """

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

        super(TestNuageSharedNetworkNonPublicIPRange, cls).setUpClass()
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
            # Create  1 admin account and 2 user accounts for domain_1
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

            # As admin user, create shared network with scope "all", "domain"
            # with subdomain access, "domain" without subdomain access and
            # "account"

            cls.api_client.connection.apiKey = cls.default_apikey
            cls.api_client.connection.securityKey = cls.default_secretkey

            cls.shared_network_offering = NetworkOffering.create(
                cls.api_client,
                cls.nuagenetworkdata["shared_nuage_network_offering"],
                conservemode=False
            )
            # Enable Network offering
            cls.shared_network_offering.update(cls.api_client, state='Enabled')
            cls.shared_network_offering_id = cls.shared_network_offering.id

            cls.shared_network_all = Network.create(
                cls.api_client,
                cls.nuagenetworkdata["network_all"],
                networkofferingid=cls.shared_network_offering_id,
                zoneid=cls.zone.id
            )

            cls.shared_network_domain_d11 = Network.create(
                cls.api_client,
                cls.nuagenetworkdata["network_all"],
                networkofferingid=cls.shared_network_offering_id,
                zoneid=cls.zone.id,
                domainid=cls.domain_11.id,
                subdomainaccess=False
            )

            cls.shared_network_domain_with_subdomain_d11 = Network.create(
                cls.api_client,
                cls.nuagenetworkdata["network_all"],
                networkofferingid=cls.shared_network_offering_id,
                zoneid=cls.zone.id,
                domainid=cls.domain_11.id,
                subdomainaccess=True
            )

            cls.shared_network_account_d111a = Network.create(
                cls.api_client,
                cls.nuagenetworkdata["network_all"],
                networkofferingid=cls.shared_network_offering_id,
                zoneid=cls.zone.id,
                domainid=cls.domain_11.id,
                accountid=cls.account_d11a.user[0].username
            )

            cls._cleanup = [
                cls.account_root,
                cls.account_roota,
                cls.shared_network_all,
                cls.shared_network_offering,
                cls.service_offering,
            ]
        except Exception as e:
            cls.domain_1.delete(cls.api_client, cleanup="true")
            cleanup_resources(cls.api_client, cls._cleanup)
            raise Exception("Failed to create the setup required to execute "
                            "the test cases: %s" % e)

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
        self.cleanup = []

    def tearDown(self):
        # restore back default apikey and secretkey
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        return

    def add_subnet_verify(self, network, services):
        """verify required nic is present in the VM"""

        self.debug("Going to add new ip range in shared network %s" %
                   network.name)
        cmd = createVlanIpRange.createVlanIpRangeCmd()
        cmd.networkid = network.id
        cmd.gateway = services["gateway"]
        cmd.netmask = services["netmask"]
        cmd.startip = services["startip"]
        cmd.endip = services["endip"]
        cmd.forVirtualNetwork = services["forvirtualnetwork"]
        addedsubnet = self.api_client.createVlanIpRange(cmd)

        self.debug("verify above iprange is successfully added in shared "
                   "network %s or not" % network.name)

        cmd1 = listVlanIpRanges.listVlanIpRangesCmd()
        cmd1.networkid = network.id
        cmd1.id = addedsubnet.vlan.id

        allsubnets = self.api_client.listVlanIpRanges(cmd1)
        self.assertEqual(
            allsubnets[0].id,
            addedsubnet.vlan.id,
            "Check New subnet is successfully added to the shared Network"
        )
        return addedsubnet

    def delete_subnet_verify(self, network, subnet):
        """verify required nic is present in the VM"""

        self.debug("Going to delete ip range in shared network %s" %
                   network.name)
        cmd = deleteVlanIpRange.deleteVlanIpRangeCmd()
        cmd.id = subnet.vlan.id
        self.api_client.deleteVlanIpRange(cmd)

        self.debug("verify above iprange is successfully deleted from shared "
                   "network %s or not" % network.name)

        cmd1 = listVlanIpRanges.listVlanIpRangesCmd()
        cmd1.networkid = network.id
        cmd1.id = subnet.vlan.id

        try:
            allsubnets = self.api_client.listVlanIpRanges(cmd1)
            self.assertEqual(
                allsubnets[0].id,
                subnet.vlan.id,
                "Check Subnet is not present to the shared Network"
            )
            self.fail("iprange is not successfully deleted from shared "
                      "network %s" % network.name)
        except Exception as e:
            self.debug("iprange is not successfully deleted from shared "
                       "network %s" % network.name)
            self.debug("exception msg is %s" % e)

    def shared_subnet_not_present(self, network, subnetid):
        shared_resources = self.vsd.get_shared_network_resource(
            filter=self.get_externalID_filter(subnetid))
        try:
            self.assertEqual(shared_resources.description, network.name,
                             "VSD shared resources description should match "
                             "network name in CloudStack"
                             )
            self.fail("still shared resource are present on VSD")
        except Exception as e:
            self.debug("sharedNetwork resources is successfully deleted from "
                       "VSD")
            self.debug("exception msg is %s" % e)

    # Test cases relating to add/delete Shared Network IP ranges
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_01_add_delete_Subnet_in_sharednetwork_scope_all(self):
        """Validate that subnet of same and different public gateway can be
        added to shared network with scope=all
        """

        # Add subnet with same cidr
        self.debug("Adding subnet of same cidr to shared Network scope as all")
        subnet1 = self.add_subnet_verify(
            self.shared_network_all, self.nuagenetworkdata["publiciprange1"])

        self.test_data["virtual_machine"]["ipaddress"] = \
            self.nuagenetworkdata["publiciprange1"]["startip"]
        vm_1 = self.create_VM(
            self.shared_network_all, account=self.account_d11a)

        # Verify shared Network and VM in VSD
        self.verify_vsd_shared_network(
            self.account_d11a.domainid, self.shared_network_all,
            gateway=self.nuagenetworkdata["publiciprange1"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_all.id,
            self.nuagenetworkdata["publiciprange1"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11a.domainid, self.shared_network_all, vm_1,
            sharedsubnetid=subnet_id)

        # Add subnet with different cidr
        self.debug("Adding subnet of different cidr to shared Network scope "
                   "as all")
        subnet2 = self.add_subnet_verify(
            self.shared_network_all, self.nuagenetworkdata["publiciprange2"])

        self.test_data["virtual_machine"]["ipaddress"] = \
            self.nuagenetworkdata["publiciprange2"]["startip"]
        vm_2 = self.create_VM(
            self.shared_network_all, account=self.account_d11a)

        self.verify_vsd_shared_network(
            self.account_d11a.domainid, self.shared_network_all,
            gateway=self.nuagenetworkdata["publiciprange2"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_all.id,
            self.nuagenetworkdata["publiciprange2"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11a.domainid, self.shared_network_all, vm_2,
            sharedsubnetid=subnet_id)
        # put ping here
        self.delete_VM(vm_1)
        self.delete_VM(vm_2)
        self.delete_subnet_verify(self.shared_network_all, subnet1)
        self.delete_subnet_verify(self.shared_network_all, subnet2)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_02_add_delete_Subnet_in_sharednetwork_scope_domain(self):
        """Validate subnet of same and different gateway can be added to public
        shared network scope=domain with subdomain access
        """

        # Add subnet with same cidr
        self.debug("Adding subnet of same cidr to shared Network scope domain "
                   "with subdomain access")
        subnet1 = self.add_subnet_verify(
            self.shared_network_domain_with_subdomain_d11,
            self.nuagenetworkdata["publiciprange1"])

        self.test_data["virtual_machine"]["ipaddress"] = \
            self.nuagenetworkdata["publiciprange1"]["startip"]
        vm_1 = self.create_VM(
            self.shared_network_domain_with_subdomain_d11,
            account=self.account_d11a)

        # Verify shared Network and VM in VSD
        self.verify_vsd_shared_network(
            self.account_d11a.domainid,
            self.shared_network_domain_with_subdomain_d11,
            gateway=self.nuagenetworkdata["publiciprange1"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_domain_with_subdomain_d11.id,
            self.nuagenetworkdata["publiciprange1"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11a.domainid,
            self.shared_network_domain_with_subdomain_d11, vm_1,
            sharedsubnetid=subnet_id)

        # Add subnet with different cidr
        self.debug("Adding subnet of different cidr to shared Network")
        subnet2 = self.add_subnet_verify(
            self.shared_network_domain_with_subdomain_d11,
            self.nuagenetworkdata["publiciprange2"])

        self.test_data["virtual_machine"]["ipaddress"] = \
            self.nuagenetworkdata["publiciprange2"]["startip"]
        vm_2 = self.create_VM(
            self.shared_network_domain_with_subdomain_d11,
            account=self.account_d11a)

        self.verify_vsd_shared_network(
            self.account_d11a.domainid,
            self.shared_network_domain_with_subdomain_d11,
            gateway=self.nuagenetworkdata["publiciprange2"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_domain_with_subdomain_d11.id,
            self.nuagenetworkdata["publiciprange2"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11a.domainid,
            self.shared_network_domain_with_subdomain_d11, vm_2,
            sharedsubnetid=subnet_id)
        # put ping here
        self.delete_VM(vm_1)
        self.delete_VM(vm_2)
        self.delete_subnet_verify(
            self.shared_network_domain_with_subdomain_d11, subnet1)
        self.delete_subnet_verify(
            self.shared_network_domain_with_subdomain_d11, subnet2)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_03_add_delete_Subnet_in_sharednetwork_scope_nosubdomain(self):
        """Validate subnet of same and different gateway can added to public
        shared network scope=domain without subdomain access
        """

        # Add subnet with same cidr
        self.debug("Adding subnet of same cidr to shared Network scope domain "
                   "without subdomain access")
        subnet1 = self.add_subnet_verify(
            self.shared_network_domain_d11,
            self.nuagenetworkdata["publiciprange1"])

        self.test_data["virtual_machine"]["ipaddress"] = \
            self.nuagenetworkdata["publiciprange1"]["startip"]
        vm_1 = self.create_VM(
            self.shared_network_domain_d11, account=self.account_d11a)

        # Verify shared Network and VM in VSD
        self.verify_vsd_shared_network(
            self.account_d11a.domainid, self.shared_network_domain_d11,
            gateway=self.nuagenetworkdata["publiciprange1"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_domain_d11.id,
            self.nuagenetworkdata["publiciprange1"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11a.domainid, self.shared_network_domain_d11, vm_1,
            sharedsubnetid=subnet_id)

        # Add subnet with different cidr
        self.debug("Adding subnet of different cidr to shared Network")
        subnet2 = self.add_subnet_verify(
            self.shared_network_domain_d11,
            self.nuagenetworkdata["publiciprange2"])

        self.test_data["virtual_machine"]["ipaddress"] = \
            self.nuagenetworkdata["publiciprange2"]["startip"]
        vm_2 = self.create_VM(
            self.shared_network_domain_d11, account=self.account_d11a)

        self.verify_vsd_shared_network(
            self.account_d11a.domainid, self.shared_network_domain_d11,
            gateway=self.nuagenetworkdata["publiciprange2"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_domain_d11.id,
            self.nuagenetworkdata["publiciprange2"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11a.domainid, self.shared_network_domain_d11, vm_2,
            sharedsubnetid=subnet_id)
        # put ping here
        self.delete_VM(vm_1)
        self.delete_VM(vm_2)
        self.delete_subnet_verify(self.shared_network_domain_d11, subnet1)
        self.delete_subnet_verify(self.shared_network_domain_d11, subnet2)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_04_add_delete_Subnet_in_sharednetwork_scope_account(self):
        """Validate subnet of same and different gateway can be added to shared
        network scope=Account
        """

        # Add subnet with same cidr
        self.debug("Adding subnet of same cidr to shared Network scope as "
                   "Account")
        subnet1 = self.add_subnet_verify(
            self.shared_network_account_d111a,
            self.nuagenetworkdata["publiciprange1"])

        self.test_data["virtual_machine"]["ipaddress"] = \
            self.nuagenetworkdata["publiciprange1"]["startip"]
        vm_1 = self.create_VM(
            self.shared_network_account_d111a, account=self.account_d11a)

        # Verify shared Network and VM in VSD
        self.verify_vsd_shared_network(
            self.account_d11a.domainid, self.shared_network_account_d111a,
            gateway=self.nuagenetworkdata["publiciprange1"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_account_d111a.id,
            self.nuagenetworkdata["publiciprange1"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11a.domainid, self.shared_network_account_d111a,
            vm_1, sharedsubnetid=subnet_id)

        # Add subnet with different cidr
        self.debug("Adding subnet of different cidr to shared Network")
        subnet2 = self.add_subnet_verify(
            self.shared_network_account_d111a,
            self.nuagenetworkdata["publiciprange2"])

        self.test_data["virtual_machine"]["ipaddress"] = \
            self.nuagenetworkdata["publiciprange2"]["startip"]
        vm_2 = self.create_VM(
            self.shared_network_account_d111a, account=self.account_d11a)

        self.verify_vsd_shared_network(
            self.account_d11a.domainid, self.shared_network_account_d111a,
            gateway=self.nuagenetworkdata["publiciprange2"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_account_d111a.id,
            self.nuagenetworkdata["publiciprange2"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11a.domainid, self.shared_network_account_d111a,
            vm_2, sharedsubnetid=subnet_id)
        # put ping here
        self.delete_VM(vm_1)
        self.delete_VM(vm_2)
        self.delete_subnet_verify(self.shared_network_account_d111a, subnet1)
        self.delete_subnet_verify(self.shared_network_account_d111a, subnet2)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_05_change_ip_from_different_Subnet_in_sharednetwork(self):
        """Validate that ip of a vm can be changed to a different subnet ip for
        shared_network_scope_all
        """

        # Add subnet with same cidr
        self.debug("Adding subnet of same cidr to shared Network scope as all")
        subnet1 = self.add_subnet_verify(
            self.shared_network_all, self.nuagenetworkdata["publiciprange1"])

        self.test_data["virtual_machine"]["ipaddress"] = \
            self.nuagenetworkdata["publiciprange1"]["startip"]
        vm_1 = self.create_VM(
            self.shared_network_all, account=self.account_d11a)

        # Verify shared Network and VM in VSD
        self.verify_vsd_shared_network(
            self.account_d11a.domainid, self.shared_network_all,
            gateway=self.nuagenetworkdata["publiciprange1"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_all.id,
            self.nuagenetworkdata["publiciprange1"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11a.domainid, self.shared_network_all, vm_1,
            sharedsubnetid=subnet_id)

        # Add subnet with different cidr
        self.debug("Adding subnet of different cidr to shared Network scope "
                   "as all")
        subnet2 = self.add_subnet_verify(
            self.shared_network_all, self.nuagenetworkdata["publiciprange2"])

        # stop VM to update the ipaddress
        try:
            vm_1.stop(self.api_client)
        except Exception as e:
            self.fail("Failed to stop the virtual instances, %s" % e)
        vm_list = list_virtual_machines(self.api_client, id=vm_1.id)
        nics = [x for x in vm_list[0].nic
                if x.networkid == self.shared_network_all.id]
        self.debug("Filtered nics list: %s:" % nics)

        cmd = updateVmNicIp.updateVmNicIpCmd()
        for x in vm_list[0].nic:
            cmd.nicid = x.id
        cmd.ipaddress = self.nuagenetworkdata["publiciprange2"]["startip"]
        self.api_client.updateVmNicIp(cmd)

        try:
            vm_1.start(self.api_client)
        except Exception as e:
            self.fail("Failed to start the virtual instances, %s" % e)

        # Verify shared Network and VM in VSD
        self.verify_vsd_shared_network(
            self.account_d11a.domainid, self.shared_network_all,
            gateway=self.nuagenetworkdata["publiciprange2"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_all.id,
            self.nuagenetworkdata["publiciprange2"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11a.domainid, self.shared_network_all, vm_1,
            sharedsubnetid=subnet_id)

        self.delete_VM(vm_1)
        self.delete_subnet_verify(self.shared_network_all, subnet1)
        self.delete_subnet_verify(self.shared_network_all, subnet2)

    @staticmethod
    def generateKeysForUser(api_client, account):
        user = User.list(
            api_client,
            account=account.name,
            domainid=account.domainid)[0]
        return (User.registerUserKeys(
            api_client,
            user.id))
