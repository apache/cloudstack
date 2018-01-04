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
Public Shared Network IP Range
"""
# Import Local Modules
from nuageTestCase import nuageTestCase
from marvin.lib.utils import cleanup_resources
from marvin.lib.common import list_templates
from marvin.lib.base import (Account,
                             Domain,
                             User,
                             VirtualMachine,
                             Network,
                             NetworkOffering)
from marvin.cloudstackAPI import (createVlanIpRange,
                                  listVlanIpRanges,
                                  deleteVlanIpRange,
                                  updateTemplate)
# Import System Modules
from nose.plugins.attrib import attr
import random
import string


class TestNuageSharedNetworkUserdata(nuageTestCase):
    """Test Shared Network functionality with Nuage VSP SDN plugin:
    Public Shared Network IP Range
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

        super(TestNuageSharedNetworkUserdata, cls).setUpClass()
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
            # with subdomain access ,"domain" without subdomain access and
            # "account"

            cls.api_client.connection.apiKey = cls.default_apikey
            cls.api_client.connection.securityKey = cls.default_secretkey
            cls.nuagenetworkdata["shared_nuage_public_network_offering"][
                "serviceProviderList"].update({"UserData": 'VirtualRouter'})
            cls.nuagenetworkdata["shared_nuage_public_network_offering"][
                "supportedservices"] = 'Dhcp,Connectivity,UserData'
            for key, value in cls.test_data["nuagevsp"][
                "shared_nuage_public_network_offering"]["serviceProviderList"]\
                    .iteritems():
                cls.debug("elements are  %s and value is %s" % (key, value))

            cls.shared_network_offering = NetworkOffering.create(
                cls.api_client,
                cls.nuagenetworkdata["shared_nuage_public_network_offering"],
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
            user_data = ''.join(random.choice(
                string.ascii_uppercase + string.digits) for x in range(2500))
            cls.test_data["virtual_machine"]["userdata"] = user_data
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
        self.debug("Cleaning up the resources")
        for obj in reversed(self.cleanup):
            try:
                if isinstance(obj, VirtualMachine):
                    obj.delete(self.api_client, expunge=True)
                else:
                    obj.delete(self.api_client)
            except Exception as e:
                self.error("Failed to cleanup %s, got %s" % (obj, e))
        # cleanup_resources(self.api_client, self.cleanup)
        self.cleanup = []
        self.debug("Cleanup complete!")
        self.updateTemplate(False)
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
            self.debug("iprange is successfully deleted from shared "
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

    # updateTemplate - Updates value of the guest VM template's password
    # enabled setting
    def updateTemplate(self, value):
        self.debug("Updating value of guest VM template's password enabled "
                   "setting")
        cmd = updateTemplate.updateTemplateCmd()
        cmd.id = self.template.id
        cmd.passwordenabled = value
        self.api_client.updateTemplate(cmd)
        list_template_response = list_templates(self.api_client,
                                                templatefilter="all",
                                                id=self.template.id)
        self.template = list_template_response[0]
        self.debug("Updated guest VM template")

    # Test cases relating to VR IP check on Shared Network
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_01_verify_deployvm_fail_startip_sharednetwork_scope_all(self):
        """Validate that deploy vm fails if user specify the first ip of subnet
        because that is reserved for VR shared network with scope=all
        """
        # Add vm as start ip of subnet
        self.debug("Adding VM as start IP of Subnet")
        self.test_data["virtual_machine"]["ipaddress"] = \
            self.nuagenetworkdata["network_all"]["startip"]
        try:
            self.create_VM(self.shared_network_all, account=self.account_d11a)
            self.fail("VM with subnet start IP is deployed successfully")
        except Exception as e:
            self.debug("Deploy vm fails as expected with exception %s" % e)
            self.debug("Going to verify the exception message")
            exceptionmsg = "it is reserved for the VR in network"
            if exceptionmsg in str(e):
                self.debug("correct exception is raised")
            else:
                self.fail("correct exception is not raised")

    # Test cases relating to add/delete Shared Network IP ranges
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_02_add_delete_Subnet_restart_public_sharednetwork_scope_all(self):
        """Validate that subnet of same gateway can be added to shared network
        with scope=all and restart network with clean up works
        """
        self.debug("Deploy VM to shared Network scope as all")
        self.test_data["virtual_machine"]["ipaddress"] = \
            self.nuagenetworkdata["network_all"]["endip"]
        vm_1 = self.create_VM(
            self.shared_network_all, account=self.account_d11a)

        # Verify shared Network and VM in VSD
        self.verify_vsd_shared_network(
            self.account_d11a.domainid, self.shared_network_all,
            gateway=self.nuagenetworkdata["network_all"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_all.id,
            self.nuagenetworkdata["network_all"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11a.domainid, self.shared_network_all, vm_1,
            sharedsubnetid=subnet_id)
        # verify VR
        vr = self.get_Router(self.shared_network_all)
        self.check_Router_state(vr, state="Running")
        self.verify_vsd_router(vr)

        # Add subnet with same cidr
        self.debug("Adding subnet of same cidr to shared Network scope as all")
        subnet1 = self.add_subnet_verify(
            self.shared_network_all, self.nuagenetworkdata["publiciprange3"])
        self.test_data["virtual_machine"]["ipaddress"] = \
            self.nuagenetworkdata["publiciprange3"]["startip"]
        vm_2 = self.create_VM(
            self.shared_network_all, account=self.account_d11a)
        # verify on VSD
        self.verify_vsd_shared_network(
            self.account_d11a.domainid, self.shared_network_all,
            gateway=self.nuagenetworkdata["publiciprange3"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_all.id,
            self.nuagenetworkdata["publiciprange3"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11a.domainid, self.shared_network_all, vm_2,
            sharedsubnetid=subnet_id)
        # Restart network with cleanup
        self.debug("Restarting shared Network with cleanup")
        self.shared_network_all.restart(self.api_client, cleanup=True)

        self.debug("validating SharedNetwork on VSD")
        self.verify_vsd_shared_network(
            self.account_d11a.domainid, self.shared_network_all,
            gateway=self.nuagenetworkdata["network_all"]["gateway"])
        # verify VR
        vr = self.get_Router(self.shared_network_all)
        self.check_Router_state(vr, state="Running")
        self.verify_vsd_router(vr)
        # put ping here
        self.delete_VM(vm_1)
        self.delete_VM(vm_2)
        self.delete_subnet_verify(self.shared_network_all, subnet1)

    # Test cases relating to add/delete Shared Network IP ranges
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_03_add_delete_Subnet_restart_sharednetwork_scope_domain(self):
        """Validate that subnet of same gateway can be added to shared network
        with scope=all and restart network with clean up works
        """
        self.debug("Deploy VM to shared Network scope domain as all")
        self.test_data["virtual_machine"]["ipaddress"] = \
            self.nuagenetworkdata["network_all"]["endip"]
        vm_1 = self.create_VM(
            self.shared_network_domain_with_subdomain_d11,
            account=self.account_d11a)

        # Verify shared Network and VM in VSD
        self.verify_vsd_shared_network(
            self.account_d11a.domainid,
            self.shared_network_domain_with_subdomain_d11,
            gateway=self.nuagenetworkdata["network_all"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_domain_with_subdomain_d11.id,
            self.nuagenetworkdata["network_all"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11a.domainid,
            self.shared_network_domain_with_subdomain_d11, vm_1,
            sharedsubnetid=subnet_id)
        # verify VR
        vr = self.get_Router(self.shared_network_domain_with_subdomain_d11)
        self.check_Router_state(vr, state="Running")
        self.verify_vsd_router(vr)

        # Add subnet with same cidr
        self.debug("Adding subnet of same cidr to shared Network scope as all")
        subnet1 = self.add_subnet_verify(
            self.shared_network_domain_with_subdomain_d11,
            self.nuagenetworkdata["publiciprange3"])
        self.test_data["virtual_machine"]["ipaddress"] = \
            self.nuagenetworkdata["publiciprange3"]["startip"]
        vm_2 = self.create_VM(
            self.shared_network_domain_with_subdomain_d11,
            account=self.account_d11a)
        # VSD check points
        self.verify_vsd_shared_network(
            self.account_d11a.domainid,
            self.shared_network_domain_with_subdomain_d11,
            gateway=self.nuagenetworkdata["publiciprange3"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_domain_with_subdomain_d11.id,
            self.nuagenetworkdata["publiciprange3"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11a.domainid,
            self.shared_network_domain_with_subdomain_d11, vm_2,
            sharedsubnetid=subnet_id)
        # Restart network with cleanup
        self.debug("Restarting shared Network with cleanup")
        self.shared_network_domain_with_subdomain_d11.restart(self.api_client,
                                                              cleanup=True)

        self.debug("validating SharedNetwork on VSD")
        self.verify_vsd_shared_network(
            self.account_d11a.domainid,
            self.shared_network_domain_with_subdomain_d11,
            gateway=self.nuagenetworkdata["network_all"]["gateway"])
        # verify VR
        vr = self.get_Router(self.shared_network_domain_with_subdomain_d11)
        self.check_Router_state(vr, state="Running")
        self.verify_vsd_router(vr)
        # put ping here
        self.delete_VM(vm_1)
        self.delete_VM(vm_2)
        self.delete_subnet_verify(
            self.shared_network_domain_with_subdomain_d11, subnet1)

    # Test cases relating to add/delete Shared Network IP ranges
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_04_add_delete_Subnet_restart_scope_domain_nosubdomain(self):
        """Validate that subnet of same gateway can be added to shared network
        with scope domain nosubdomain and restart network with clean up works
        """

        self.debug("Deploy VM to shared Network scope domain no subdomain")
        self.test_data["virtual_machine"]["ipaddress"] = \
            self.nuagenetworkdata["network_all"]["endip"]
        vm_1 = self.create_VM(
            self.shared_network_domain_d11, account=self.account_d11a)

        # Verify shared Network and VM in VSD
        self.verify_vsd_shared_network(
            self.account_d11a.domainid, self.shared_network_domain_d11,
            gateway=self.nuagenetworkdata["network_all"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_domain_d11.id,
            self.nuagenetworkdata["network_all"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11a.domainid, self.shared_network_domain_d11, vm_1,
            sharedsubnetid=subnet_id)
        # verify VR
        vr = self.get_Router(self.shared_network_domain_d11)
        self.check_Router_state(vr, state="Running")
        self.verify_vsd_router(vr)

        # Add subnet with same cidr
        self.debug("Adding subnet of same cidr to shared Network scope as all")
        subnet1 = self.add_subnet_verify(
            self.shared_network_domain_d11,
            self.nuagenetworkdata["publiciprange3"])
        self.test_data["virtual_machine"]["ipaddress"] = \
            self.nuagenetworkdata["publiciprange3"]["startip"]
        vm_2 = self.create_VM(
            self.shared_network_domain_d11, account=self.account_d11a)

        self.verify_vsd_shared_network(
            self.account_d11a.domainid, self.shared_network_domain_d11,
            gateway=self.nuagenetworkdata["publiciprange3"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_domain_d11.id,
            self.nuagenetworkdata["publiciprange3"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11a.domainid, self.shared_network_domain_d11, vm_2,
            sharedsubnetid=subnet_id)
        # Restart network with cleanup
        self.debug("Restarting shared Network with cleanup")
        self.shared_network_domain_d11.restart(self.api_client, cleanup=True)

        self.debug("validating SharedNetwork on VSD")
        self.verify_vsd_shared_network(
            self.account_d11a.domainid, self.shared_network_domain_d11,
            gateway=self.nuagenetworkdata["network_all"]["gateway"])
        # verify VR
        vr = self.get_Router(self.shared_network_domain_d11)
        self.check_Router_state(vr, state="Running")
        self.verify_vsd_router(vr)
        # put ping here
        self.delete_VM(vm_1)
        self.delete_VM(vm_2)
        self.delete_subnet_verify(self.shared_network_domain_d11, subnet1)

    # Test cases relating to add/delete Shared Network IP ranges
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_05_add_delete_Subnet_restart_scope_account(self):
        """Validate that subnet of same gateway can be added to shared network
        with scope as account and restart network with clean up works
        """

        self.debug("Deploy VM to shared Network scope as account")

        self.test_data["virtual_machine"]["ipaddress"] = \
            self.nuagenetworkdata["network_all"]["endip"]
        vm_1 = self.create_VM(
            self.shared_network_account_d111a, account=self.account_d11a)

        # Verify shared Network and VM in VSD
        self.verify_vsd_shared_network(
            self.account_d11a.domainid, self.shared_network_account_d111a,
            gateway=self.nuagenetworkdata["network_all"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_account_d111a.id,
            self.nuagenetworkdata["network_all"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11a.domainid, self.shared_network_account_d111a,
            vm_1, sharedsubnetid=subnet_id)
        # verify VR
        vr = self.get_Router(self.shared_network_account_d111a)
        self.check_Router_state(vr, state="Running")
        self.verify_vsd_router(vr)

        # Add subnet with same cidr
        self.debug("Add subnet of same cidr shared Network scope as account")
        subnet1 = self.add_subnet_verify(
            self.shared_network_account_d111a,
            self.nuagenetworkdata["publiciprange3"])

        self.test_data["virtual_machine"]["ipaddress"] = \
            self.nuagenetworkdata["publiciprange3"]["startip"]
        vm_2 = self.create_VM(
            self.shared_network_account_d111a, account=self.account_d11a)

        self.verify_vsd_shared_network(
            self.account_d11a.domainid, self.shared_network_account_d111a,
            gateway=self.nuagenetworkdata["publiciprange3"]["gateway"])
        subnet_id = self.get_subnet_id(
            self.shared_network_account_d111a.id,
            self.nuagenetworkdata["publiciprange3"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.account_d11a.domainid, self.shared_network_account_d111a,
            vm_2, sharedsubnetid=subnet_id)
        # Restart network with cleanup
        self.debug("Restarting shared Network with cleanup")
        self.shared_network_account_d111a.restart(self.api_client,
                                                  cleanup=True)

        self.debug("validating SharedNetwork on VSD")
        self.verify_vsd_shared_network(
            self.account_d11a.domainid, self.shared_network_account_d111a,
            gateway=self.nuagenetworkdata["network_all"]["gateway"])
        # verify VR
        vr = self.get_Router(self.shared_network_account_d111a)
        self.check_Router_state(vr, state="Running")
        self.verify_vsd_router(vr)
        # put ping here
        self.delete_VM(vm_1)
        self.delete_VM(vm_2)
        self.delete_subnet_verify(self.shared_network_account_d111a, subnet1)

    # Test cases relating to VR IP check on Shared Network
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_06_verify_different_gateway_subnet_fails_sharednetwork_all(self):
        """Validate that Different gateway subnet fail as it is not supported
        for userdata service shared network with scope=all
        """
        # Add subnet of different gateway
        self.debug("Adding subnet of different gateway")

        try:
            subnet2 = self.add_subnet_verify(
                self.shared_network_all,
                self.nuagenetworkdata["publiciprange2"])
            self.test_data["virtual_machine"]["ipaddress"] = \
                self.nuagenetworkdata["network_all"]["endip"]
            vm_1 = self.create_VM(
                self.shared_network_all, account=self.account_d11a)
            self.delete_VM(vm_1)
            self.delete_subnet_verify(self.shared_network_all, subnet2)
            self.fail("VM is successfully added which is not expected")
        except Exception as e:
            self.debug("different gateway subnet "
                       "fails as expected with exception %s" % e)
            self.debug("Going to verify the exception message")
            self.delete_subnet_verify(self.shared_network_all, subnet2)
            exceptionmsg = "Unable to start VM instance"
            if exceptionmsg in str(e):
                self.debug("correct exception is raised")
            else:
                self.fail("correct exception is not raised")

    # Test cases relating to different gateway subnet check on Shared Network
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_07_different_gateway_subnet_fails_sharednetwork_domain(self):
        """Validate that Different gateway subnet fail as it is not supported
        for userdata service shared network with scope domain
        """
        # Add subnet of different gateway
        self.debug("Adding subnet of different gateway")

        try:
            subnet2 = self.add_subnet_verify(
                self.shared_network_domain_with_subdomain_d11,
                self.nuagenetworkdata["publiciprange2"])
            self.test_data["virtual_machine"]["ipaddress"] = \
                self.nuagenetworkdata["network_all"]["endip"]
            vm_1 = self.create_VM(
                self.shared_network_domain_with_subdomain_d11,
                account=self.account_d11a)
            self.delete_VM(vm_1)
            self.delete_subnet_verify(
                self.shared_network_domain_with_subdomain_d11, subnet2)
            self.fail("VM is successfully added which is not expected")
        except Exception as e:
            self.debug("different gateway subnet "
                       "fails as expected with exception %s" % e)
            self.debug("Going to verify the exception message")
            self.delete_subnet_verify(
                self.shared_network_domain_with_subdomain_d11, subnet2)
            exceptionmsg = "Unable to start VM instance"
            if exceptionmsg in str(e):
                self.debug("correct exception is raised")
            else:
                self.fail("correct exception is not raised")

    # Test cases relating to different gateway subnet check on Shared Network
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_08_different_gateway_subnet_fails_sharednetwork_nosubdomain(self):
        """Validate that Different gateway subnet fail as it is not supported
        for userdata service shared network with scope nosubdomain
        """
        # Add subnet of different gateway
        self.debug("Adding subnet of different gateway")

        try:
            subnet2 = self.add_subnet_verify(
                self.shared_network_domain_d11,
                self.nuagenetworkdata["publiciprange2"])
            self.test_data["virtual_machine"]["ipaddress"] = \
                self.nuagenetworkdata["network_all"]["endip"]
            vm_1 = self.create_VM(
                self.shared_network_domain_d11, account=self.account_d11a)
            self.delete_VM(vm_1)
            self.delete_subnet_verify(
                self.shared_network_domain_d11, subnet2)
            self.fail("VM is successfully added which is not expected")
        except Exception as e:
            self.debug("different gateway subnet"
                       " fails as expected with exception %s" % e)
            self.debug("Going to verify the exception message")
            self.delete_subnet_verify(
                self.shared_network_domain_d11, subnet2)
            exceptionmsg = "Unable to start VM instance"
            if exceptionmsg in str(e):
                self.debug("correct exception is raised")
            else:
                self.fail("correct exception is not raised")

    # Test cases relating to different gateway subnet check on Shared Network
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_09_different_gateway_subnet_fails_sharednetwork_account(self):
        """Validate that Different gateway subnet fail as it is not supported
        for userdata service shared network with scope account
        """
        # Add subnet of different gateway
        self.debug("Adding subnet of different gateway")

        try:
            subnet2 = self.add_subnet_verify(
                self.shared_network_account_d111a,
                self.nuagenetworkdata["publiciprange2"])
            self.test_data["virtual_machine"]["ipaddress"] = \
                self.nuagenetworkdata["network_all"]["endip"]
            vm_1 = self.create_VM(
                self.shared_network_account_d111a, account=self.account_d11a)
            self.delete_VM(vm_1)
            self.delete_subnet_verify(
                self.shared_network_account_d111a, subnet2)
            self.fail("VM is successfully added which is not expected")
        except Exception as e:
            self.debug("different gateway subnet"
                       " fails as expected with exception %s" % e)
            self.debug("Going to verify the exception message")
            self.delete_subnet_verify(
                self.shared_network_account_d111a, subnet2)
            exceptionmsg = "Unable to start VM instance"
            if exceptionmsg in str(e):
                self.debug("correct exception is raised")
            else:
                self.fail("correct exception is not raised")

    # Test cases relating to reset password in Shared Network
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_10_password_reset_public_sharednetwork_scope_all(self):
        """Validate that reset password works fine in shared network
        with scope=all
        """
        self.updateTemplate(True)
        self.debug("Deploy VM to shared Network scope as all")
        self.test_data["virtual_machine"]["ipaddress"] = \
            self.nuagenetworkdata["network_all"]["endip"]
        vm_1 = self.create_VM(
            self.shared_network_all, account=self.account_d11a)

        # verify VR
        vr = self.get_Router(self.shared_network_all)
        self.check_Router_state(vr, state="Running")
        self.verify_vsd_router(vr)

        self.debug("Stopping VM: %s" % vm_1.name)
        vm_1.stop(self.api_client)
        self.debug("Resetting VM password for VM: %s" % vm_1.name)
        password = vm_1.resetPassword(self.api_client)
        self.debug("Password reset to: %s" % password)
        vm_1.start(self.api_client)

        # put login to vm here
        self.delete_VM(vm_1)

    # Test cases relating to reset password in Shared Network
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_11_password_reset_public_sharednetwork_scope_domain(self):
        """Validate that reset password works fine in shared network
        with scope as domain with subdomain access
        """
        self.updateTemplate(True)
        self.debug("Deploy VM to shared Network scope as all")
        self.test_data["virtual_machine"]["ipaddress"] = \
            self.nuagenetworkdata["network_all"]["endip"]
        vm_1 = self.create_VM(
            self.shared_network_domain_with_subdomain_d11,
            account=self.account_d11a)

        # verify VR
        vr = self.get_Router(self.shared_network_domain_with_subdomain_d11)
        self.check_Router_state(vr, state="Running")
        self.verify_vsd_router(vr)

        self.debug("Stopping VM: %s" % vm_1.name)
        vm_1.stop(self.api_client)
        self.debug("Resetting VM password for VM: %s" % vm_1.name)
        password = vm_1.resetPassword(self.api_client)
        self.debug("Password reset to: %s" % password)
        vm_1.start(self.api_client)

        # put login to vm here
        self.delete_VM(vm_1)

    # Test cases relating to reset password in Shared Network
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_12_password_reset_public_sharednetwork_scope_account(self):
        """Validate that reset password works fine in shared network
        with scope as Account
        """
        self.updateTemplate(True)
        self.debug("Deploy VM to shared Network scope as all")
        self.test_data["virtual_machine"]["ipaddress"] = \
            self.nuagenetworkdata["network_all"]["endip"]
        vm_1 = self.create_VM(
            self.shared_network_account_d111a, account=self.account_d11a)

        # verify VR
        vr = self.get_Router(self.shared_network_account_d111a)
        self.check_Router_state(vr, state="Running")
        self.verify_vsd_router(vr)

        self.debug("Stopping VM: %s" % vm_1.name)
        vm_1.stop(self.api_client)
        self.debug("Resetting VM password for VM: %s" % vm_1.name)
        password = vm_1.resetPassword(self.api_client)
        self.debug("Password reset to: %s" % password)
        vm_1.start(self.api_client)

        # put login to vm here
        self.delete_VM(vm_1)

    def test_13_public_sharednetwork_domain_cleanup(self):
        """Validate that  sharedNetwork Parent domain is cleaned up properly
        """

        try:
            self.test_data["virtual_machine"]["ipaddress"] = \
                self.nuagenetworkdata["network_all"]["endip"]
            vm_1 = self.create_VM(
                self.shared_network_domain_with_subdomain_d11,
                account=self.account_d11a)

            self.verify_vsd_shared_network(
                self.account_d11a.domainid,
                self.shared_network_domain_with_subdomain_d11,
                gateway=self.nuagenetworkdata["network_all"]["gateway"])
            subnet_id_subdomain = self.get_subnet_id(
                self.shared_network_domain_with_subdomain_d11.id,
                self.nuagenetworkdata["network_all"]["gateway"])
            self.verify_vsd_enterprise_vm(
                self.account_d11a.domainid,
                self.shared_network_domain_with_subdomain_d11,
                vm_1, sharedsubnetid=subnet_id_subdomain)

            subnet_id_subdomain1 = self.get_subnet_id(
                self.shared_network_domain_with_subdomain_d11.id,
                self.nuagenetworkdata["publiciprange2"]["gateway"])
            self.domain_1.delete(self.api_client, cleanup="true")
        except Exception as e:
            self.debug("test case Fail")
            self.debug("exception msg is %s" % e)
            self.domain_1.delete(self.api_client, cleanup="true")
            self.fail("Fail to delete the Parent domain")

        self.shared_subnet_not_present(
            self.shared_network_domain_with_subdomain_d11,
            subnet_id_subdomain)
        self.shared_subnet_not_present(
            self.shared_network_domain_with_subdomain_d11,
            subnet_id_subdomain1)

    @staticmethod
    def generateKeysForUser(api_client, account):
        user = User.list(
            api_client,
            account=account.name,
            domainid=account.domainid)[0]

        return (User.registerUserKeys(
            api_client,
            user.id))
