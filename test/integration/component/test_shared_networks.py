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

""" P1 tests for shared networks
"""
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.cloudstackAPI import rebootRouter, stopRouter, startRouter
from marvin.lib.base import (Account,
                             Network,
                             NetworkOffering,
                             VirtualMachine,
                             Project,
                             PhysicalNetwork,
                             Domain,
                             StaticNATRule,
                             FireWallRule,
                             ServiceOffering,
                             PublicIPAddress,
                             Router,
                             NATRule)
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               get_free_vlan,
                               wait_for_cleanup,
                               verifyRouterState,
                               verifyGuestTrafficPortGroups)
from marvin.sshClient import SshClient
from marvin.codes import PASS
from ddt import ddt, data
import time
import random
import netaddr


@ddt
class TestSharedNetworks(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestSharedNetworks, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )

        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )

        cls.testdata["shared_network_offering"]["specifyVlan"] = "True"
        cls.testdata["shared_network_offering"]["specifyIpRanges"] = "True"

        cls.shared_network_offering = NetworkOffering.create(
            cls.api_client,
            cls.testdata["shared_network_offering"],
            conservemode=False
        )

        NetworkOffering.update(
            cls.shared_network_offering,
            cls.api_client,
            id=cls.shared_network_offering.id,
            state="enabled"
        )

        cls.testdata["shared_network_offering_all_services"]["specifyVlan"] = "True"
        cls.testdata["shared_network_offering_all_services"]["specifyIpRanges"] = "True"

        cls.shared_network_offering_all_services = NetworkOffering.create(
            cls.api_client,
            cls.testdata["shared_network_offering_all_services"],
            conservemode=False
        )

        NetworkOffering.update(
            cls.shared_network_offering_all_services,
            cls.api_client,
            id=cls.shared_network_offering_all_services.id,
            state="enabled"
        )

        cls._cleanup = [
            cls.service_offering
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.api_client = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

        # Set the subnet number of shared networks randomly prior to execution
        # of each test case to avoid overlapping of ip addresses
        shared_network_subnet_number = random.randrange(1, 254)

        self.testdata["shared_network"]["netmask"] = "255.255.255.0"
        self.testdata["shared_network"]["gateway"] = "172.16." + \
            str(shared_network_subnet_number) + ".1"
        self.testdata["shared_network"]["startip"] = "172.16." + \
            str(shared_network_subnet_number) + ".2"
        self.testdata["shared_network"]["endip"] = "172.16." + \
            str(shared_network_subnet_number) + ".20"

        self.cleanup = []
        self.cleanup_networks = []
        self.cleanup_accounts = []
        self.cleanup_domains = []
        self.cleanup_projects = []
        self.cleanup_vms = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created network offerings
            cleanup_resources(self.api_client, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        # below components is not a part of cleanup because to mandate the
        # order and to cleanup network
        try:
            for vm in self.cleanup_vms:
                vm.delete(self.api_client)
        except Exception as e:
            raise Exception(
                "Warning: Exception during virtual machines cleanup : %s" %
                e)

        try:
            for project in self.cleanup_projects:
                project.delete(self.api_client)
        except Exception as e:
            raise Exception(
                "Warning: Exception during project cleanup : %s" %
                e)

        try:
            for account in self.cleanup_accounts:
                account.delete(self.api_client)
        except Exception as e:
            raise Exception(
                "Warning: Exception during account cleanup : %s" %
                e)

        # Wait till all resources created are cleaned up completely and then
        # attempt to delete domains
        wait_for_cleanup(self.api_client, ["account.cleanup.interval"])

        try:
            for network in self.cleanup_networks:
                network.delete(self.api_client)
        except Exception:
            self.debug("Network %s failed to delete. Moving on" % network.id)
            pass  # because domain/account deletion will get rid of the network

        try:
            for domain in self.cleanup_domains:
                domain.delete(self.api_client)
        except Exception as e:
            raise Exception(
                "Warning: Exception during domain cleanup : %s" %
                e)

        return

    def verifyRouterResponse(self, router_response, ip):
        if (router_response) and (isinstance(router_response, list)) and \
           (router_response[0].state == "Running") and \
           (router_response[0].publicip == ip):
            return True
        return False

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_sharedNetworkOffering_01(self):
        """  Test shared network Offering 01 """

        # Steps,
        #  1. create an Admin Account - admin-XABU1
        #  2. listPhysicalNetworks in available zone
        #  3. createNetworkOffering:
        #   - name = "MySharedOffering"
        #   - guestiptype="shared"
        #   - services = {Dns, Dhcp, UserData}
        #   -  conservemode = false
        #   - specifyVlan = true
        #   - specifyIpRanges = true
        #  4. Enable network offering - updateNetworkOffering - state=Enabled
        #  5. delete the admin account
        # Validations,
        #  1. listAccounts name=admin-XABU1, state=enabled returns your account
        #  2. listPhysicalNetworks should return at least one active physical
        #     network
        #  3. listNetworkOfferings - name=mysharedoffering , should list
        #     offering in disabled state
        # 4.  listNetworkOfferings - name=mysharedoffering, should list
        #     enabled offering

        # Create an account
        self.account = Account.create(
            self.api_client,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )

        self.cleanup_accounts.append(self.account)

        # verify that the account got created with state enabled
        list_accounts_response = Account.list(
            self.api_client,
            id=self.account.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_accounts_response, list),
            True,
            "listAccounts returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_accounts_response),
            0,
            "listAccounts returned empty list."
        )
        self.assertEqual(
            list_accounts_response[0].state,
            "enabled",
            "The admin account created is not enabled."
        )

        self.debug("Admin Type account created: %s" % self.account.name)

        # Verify that there should be at least one physical network present in
        # zone.
        list_physical_networks_response = PhysicalNetwork.list(
            self.api_client,
            zoneid=self.zone.id
        )
        self.assertEqual(
            isinstance(list_physical_networks_response, list),
            True,
            "listPhysicalNetworks returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_physical_networks_response),
            0,
            "listPhysicalNetworks should return at least one physical network."
        )

        physical_network = list_physical_networks_response[0]

        self.debug("Physical network found: %s" % physical_network.id)

        self.testdata["shared_network_offering"]["specifyVlan"] = "True"
        self.testdata["shared_network_offering"]["specifyIpRanges"] = "True"

        # Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
            self.api_client,
            self.testdata["shared_network_offering"],
            conservemode=False
        )

        # Verify that the network offering got created
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Disabled",
            "The network offering created should be bydefault disabled."
        )

        # Update network offering state from disabled to enabled.
        NetworkOffering.update(
            self.shared_network_offering,
            self.api_client,
            id=self.shared_network_offering.id,
            state="enabled"
        )
        # Verify that the state of the network offering is updated
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Enabled",
            "The network offering state should get updated to Enabled."
        )
        self.debug(
            "NetworkOffering created and enabled: %s" %
            self.shared_network_offering.id)

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_sharedNetworkOffering_02(self):
        """ Test Shared Network Offering 02 """

        # Steps,
        #  1. create an Admin Account - admin-XABU1
        #  2. listPhysicalNetworks in available zone
        #  3. createNetworkOffering:
        #    - name = "MySharedOffering"
        #    - guestiptype="shared"
        #    - services = {Dns, Dhcp, UserData}
        #    -  conservemode = false
        #    - specifyVlan = false
        #    - specifyIpRanges = false
        #  4. delete the admin account
        # Validations,
        #  1. listAccounts name=admin-XABU1, state=enabled returns your account
        #  2. listPhysicalNetworks should return at least one active physical
        #     network
        # 3.  createNetworkOffering fails - vlan should be specified in
        #     advanced zone

        # Create an account
        self.account = Account.create(
            self.api_client,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )

        self.cleanup_accounts.append(self.account)

        # verify that the account got created with state enabled
        list_accounts_response = Account.list(
            self.api_client,
            id=self.account.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_accounts_response, list),
            True,
            "listAccounts returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_accounts_response),
            0,
            "listAccounts returned empty list."
        )
        self.assertEqual(
            list_accounts_response[0].state,
            "enabled",
            "The admin account created is not enabled."
        )

        self.debug("Admin type account created: %s" % self.account.name)

        # Verify that there should be at least one physical network present in
        # zone.
        list_physical_networks_response = PhysicalNetwork.list(
            self.api_client,
            zoneid=self.zone.id
        )
        self.assertEqual(
            isinstance(list_physical_networks_response, list),
            True,
            "listPhysicalNetworks returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_physical_networks_response),
            0,
            "listPhysicalNetworks should return at least one physical network."
        )

        physical_network = list_physical_networks_response[0]

        self.debug("Physical network found: %s" % physical_network.id)

        self.testdata["shared_network_offering"]["specifyVlan"] = "False"
        self.testdata["shared_network_offering"]["specifyIpRanges"] = "False"

        try:
            # Create Network Offering
            self.shared_network_offering = NetworkOffering.create(
                self.api_client,
                self.testdata["shared_network_offering"],
                conservemode=False
            )
            self.fail(
                "Network offering got created with vlan as False in advance\
                        mode and shared guest type, which is invalid case.")
        except Exception as e:
            self.debug(
                "Network Offering creation failed with vlan as False\
                 in advance mode and shared guest type. Exception: %s" %
                e)

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_sharedNetworkOffering_03(self):
        """ Test Shared Network Offering 03 """

        # Steps,
        #  1. create an Admin Account - admin-XABU1
        #  2. listPhysicalNetworks in available zone
        #  3. createNetworkOffering:
        #    - name = "MySharedOffering"
        #    - guestiptype="shared"
        #    - services = {Dns, Dhcp, UserData}
        #    -  conservemode = false
        #    - specifyVlan = true
        #    - specifyIpRanges = false
        #  4. delete the admin account
        # Validations,
        #  1. listAccounts name=admin-XABU1, state=enabled returns your account
        #  2. listPhysicalNetworks should return at least one active physical
        #     network
        # 3.  createNetworkOffering fails - ip ranges should be specified when
        #     creating shared network offering

        # Create an account
        self.account = Account.create(
            self.api_client,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )

        self.cleanup_accounts.append(self.account)

        # verify that the account got created with state enabled
        list_accounts_response = Account.list(
            self.api_client,
            id=self.account.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_accounts_response, list),
            True,
            "listAccounts returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_accounts_response),
            0,
            "listAccounts returned empty list."
        )
        self.assertEqual(
            list_accounts_response[0].state,
            "enabled",
            "The admin account created is not enabled."
        )

        self.debug("Admin Type account created: %s" % self.account.name)

        # Verify that there should be at least one physical network present in
        # zone.
        list_physical_networks_response = PhysicalNetwork.list(
            self.api_client,
            zoneid=self.zone.id
        )
        self.assertEqual(
            isinstance(list_physical_networks_response, list),
            True,
            "listPhysicalNetworks returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_physical_networks_response),
            0,
            "listPhysicalNetworks should return at least one physical network."
        )

        physical_network = list_physical_networks_response[0]

        self.debug("Physical Network found: %s" % physical_network.id)

        self.testdata["shared_network_offering"]["specifyVlan"] = "True"
        self.testdata["shared_network_offering"]["specifyIpRanges"] = "False"

        try:
            # Create Network Offering
            self.shared_network_offering = NetworkOffering.create(
                self.api_client,
                self.testdata["shared_network_offering"],
                conservemode=False
            )
            self.fail(
                "Network offering got created with vlan as True and ip ranges\
                as False in advance mode and with shared guest type,\
                which is invalid case.")
        except Exception as e:
            self.debug(
                "Network Offering creation failed with vlan as true and ip\
                 ranges as False in advance mode and with shared guest type.\
                 Exception : %s" % e)

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_createSharedNetwork_All(self):
        """ Test Shared Network ALL  """

        # Steps,
        #  1. create an Admin Account - admin-XABU1
        #  2. listPhysicalNetworks in available zone
        #  3. createNetworkOffering:
        #    - name = "MySharedOffering"
        #    - guestiptype="shared"
        #    - services = {Dns, Dhcp, UserData}
        #    -  conservemode = false
        #    - specifyVlan = true
        #    - specifyIpRanges = true
        #  4. Create network offering - updateNetworkOffering - state=Enabled
        #  5. createNetwork
        #    - name = mysharednetwork, displaytext = mysharednetwork
        #    - vlan = 123 (say)
        #    - networkofferingid = <mysharedoffering>
        #    - gw = 172.16.15.1, startip = 172.16.15.2 , endip = 172.16.15.200
        #    - netmask=255.255.255.0
        #    - scope = all
        #  6. create User account - user-ASJDK
        #  7. deployVirtualMachine in this account and in admin account &
        #     within networkid = <mysharednetwork>
        #  8. delete the admin account and the user account
        # Validations,
        #  1. listAccounts name=admin-XABU1, state=enabled returns account
        #  2. listPhysicalNetworks should return at least one active
        #     physical network
        #  3. listNetworkOfferings - name=mysharedoffering , should list
        #     offering in disabled state
        #  4. listNetworkOfferings - name=mysharedoffering, should list
        #     enabled offering
        #  5. listNetworks - name = mysharednetwork should list the
        #     successfully created network, verify the guestIp ranges and
        #     CIDR are as given in the createNetwork call
        #  6. No checks reqd
        #  7. a. listVirtualMachines should show both VMs in running state
        #     in the user account and the admin account
        #     b. VM's IPs shoud be in the range of the shared network ip ranges

        # Create admin account
        self.admin_account = Account.create(
            self.api_client,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )

        self.cleanup_accounts.append(self.admin_account)

        # verify that the account got created with state enabled
        list_accounts_response = Account.list(
            self.api_client,
            id=self.admin_account.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_accounts_response, list),
            True,
            "listAccounts returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_accounts_response),
            0,
            "listAccounts returned empty list."
        )
        self.assertEqual(
            list_accounts_response[0].state,
            "enabled",
            "The admin account created is not enabled."
        )

        self.debug("Admin type account created: %s" % self.admin_account.name)

        # Create an user account
        self.user_account = Account.create(
            self.api_client,
            self.testdata["account"],
            admin=False,
            domainid=self.domain.id
        )

        self.cleanup_accounts.append(self.user_account)

        # verify that the account got created with state enabled
        list_accounts_response = Account.list(
            self.api_client,
            id=self.user_account.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_accounts_response, list),
            True,
            "listAccounts returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_accounts_response),
            0,
            "listAccounts returned empty list."
        )
        self.assertEqual(
            list_accounts_response[0].state,
            "enabled",
            "The user account created is not enabled."
        )

        self.debug("User type account created: %s" % self.user_account.name)

        physical_network, shared_vlan = get_free_vlan(
            self.api_client, self.zone.id)
        if shared_vlan is None:
            self.fail("Failed to get free vlan id for shared network")

        self.debug("Physical network found: %s" % physical_network.id)

        self.testdata["shared_network_offering"]["specifyVlan"] = "True"
        self.testdata["shared_network_offering"]["specifyIpRanges"] = "True"

        # Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
            self.api_client,
            self.testdata["shared_network_offering"],
            conservemode=False
        )

        # Verify that the network offering got created
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Disabled",
            "The network offering created should be bydefault disabled."
        )

        self.debug(
            "Shared Network offering created: %s" %
            self.shared_network_offering.id)

        # Update network offering state from disabled to enabled.
        NetworkOffering.update(
            self.shared_network_offering,
            self.api_client,
            id=self.shared_network_offering.id,
            state="enabled"
        )

        # Verify that the state of the network offering is updated
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Enabled",
            "The network offering state should get updated to Enabled."
        )

        # create network using the shared network offering created
        self.testdata["shared_network"]["acltype"] = "Domain"
        self.testdata["shared_network"][
            "networkofferingid"] = self.shared_network_offering.id
        self.testdata["shared_network"][
            "physicalnetworkid"] = physical_network.id
        self.testdata["shared_network"]["vlan"] = shared_vlan

        self.network = Network.create(
            self.api_client,
            self.testdata["shared_network"],
            networkofferingid=self.shared_network_offering.id,
            zoneid=self.zone.id,
        )

        self.cleanup_networks.append(self.network)

        list_networks_response = Network.list(
            self.api_client,
            id=self.network.id
        )
        self.assertEqual(
            isinstance(list_networks_response, list),
            True,
            "listNetworks returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_networks_response),
            0,
            "listNetworks returned empty list."
        )
        self.assertEqual(
            list_networks_response[0].specifyipranges,
            True,
            "The network is created with ip range but the flag is\
                    set to False.")

        self.debug(
            "Shared Network created for scope domain: %s" %
            self.network.id)

        self.admin_account_virtual_machine = VirtualMachine.create(
            self.api_client,
            self.testdata["virtual_machine"],
            networkids=self.network.id,
            serviceofferingid=self.service_offering.id
        )

        self.cleanup_vms.append(self.admin_account_virtual_machine)

        vms = VirtualMachine.list(
            self.api_client,
            id=self.admin_account_virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "listVirtualMachines returned invalid object in response."
        )
        self.assertNotEqual(
            len(vms),
            0,
            "listVirtualMachines returned empty list."
        )

        self.debug(
            "Virtual Machine created: %s" %
            self.admin_account_virtual_machine.id)

        ip_range = list(
            netaddr.iter_iprange(
                str(
                    self.testdata["shared_network"]["startip"]), str(
                    self.testdata["shared_network"]["endip"])))
        if netaddr.IPAddress(str(vms[0].nic[0].ipaddress)) not in ip_range:
            self.fail(
                "Virtual machine ip should be from the ip range assigned to\
                        network created.")

        self.user_account_virtual_machine = VirtualMachine.create(
            self.api_client,
            self.testdata["virtual_machine"],
            accountid=self.user_account.name,
            domainid=self.user_account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=self.network.id
        )
        vms = VirtualMachine.list(
            self.api_client,
            id=self.user_account_virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "listVirtualMachines returned invalid object in response."
        )
        self.assertNotEqual(
            len(vms),
            0,
            "listVirtualMachines returned empty list."
        )

        self.debug(
            "Virtual Machine created: %s" %
            self.user_account_virtual_machine.id)

        ip_range = list(
            netaddr.iter_iprange(
                str(
                    self.testdata["shared_network"]["startip"]), str(
                    self.testdata["shared_network"]["endip"])))
        if netaddr.IPAddress(str(vms[0].nic[0].ipaddress)) not in ip_range:
            self.fail(
                "Virtual machine ip should be from the ip range assigned to\
                network created.")

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_createSharedNetwork_accountSpecific(self):
        """ Test Shared Network with scope account """

        # Steps,
        #  1. create an Admin Account - admin-XABU1
        #     create a user account = user-SOPJD
        #  2. listPhysicalNetworks in available zone
        #  3. createNetworkOffering:
        #    - name = "MySharedOffering"
        #    - guestiptype="shared"
        #    - services = {Dns, Dhcp, UserData}
        #    -  conservemode = false
        #    - specifyVlan = true
        #    - specifyIpRanges = true
        #  4. Enable network offering - updateNetworkOffering - state=Enabled
        #  5. createNetwork
        #    - name = mysharednetwork, displaytext = mysharednetwork
        #    - vlan = 123 (say)
        #    - networkofferingid = <mysharedoffering>
        #    - gw = 172.16.15.1, startip = 172.16.15.2 , endip = 172.16.15.200
        #    - netmask=255.255.255.0
        #    - scope = account, account = user-SOPJD, domain = ROOT
        #  6. deployVirtualMachine in this account and in admin account
        #     & within networkid = <mysharednetwork>
        #  7. delete the admin account and the user account
        # Validations,
        #  1. listAccounts name=admin-XABU1 and user-SOPJD, state=enabled
        #     returns your account
        #  2. listPhysicalNetworks should return at least one active
        #     physical network
        #  3. listNetworkOfferings - name=mysharedoffering , should list
        #     offering in disabled state
        #  4. listNetworkOfferings - name=mysharedoffering, should list
        #     enabled offering
        #  5. listNetworks - name = mysharednetwork should list the
        #     successfully created network, verify the guestIp ranges and CIDR
        #     are as given in the createNetwork call
        #  6. VM deployed in admin account should FAIL to deploy
        #     VM should be deployed in user account only
        #    verify VM's IP is within shared network range

        # Create admin account
        self.admin_account = Account.create(
            self.api_client,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )

        self.cleanup_accounts.append(self.admin_account)

        # verify that the account got created with state enabled
        list_accounts_response = Account.list(
            self.api_client,
            id=self.admin_account.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_accounts_response, list),
            True,
            "listAccounts returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_accounts_response),
            0,
            "listAccounts returned empty list."
        )
        self.assertEqual(
            list_accounts_response[0].state,
            "enabled",
            "The admin account created is not enabled."
        )

        self.debug("Admin type account created: %s" % self.admin_account.name)

        # Create an user account
        self.user_account = Account.create(
            self.api_client,
            self.testdata["account"],
            admin=False,
            domainid=self.domain.id
        )

        self.cleanup_accounts.append(self.user_account)

        # verify that the account got created with state enabled
        list_accounts_response = Account.list(
            self.api_client,
            id=self.user_account.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_accounts_response, list),
            True,
            "listAccounts returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_accounts_response),
            0,
            "listAccounts returned empty list."
        )
        self.assertEqual(
            list_accounts_response[0].state,
            "enabled",
            "The user account created is not enabled."
        )

        self.debug("User type account created: %s" % self.user_account.name)

        physical_network, shared_vlan = get_free_vlan(
            self.api_client, self.zone.id)
        if shared_vlan is None:
            self.fail("Failed to get free vlan id for shared network")

        self.debug("Physical Network found: %s" % physical_network.id)

        self.testdata["shared_network_offering"]["specifyVlan"] = "True"
        self.testdata["shared_network_offering"]["specifyIpRanges"] = "True"

        # Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
            self.api_client,
            self.testdata["shared_network_offering"],
            conservemode=False
        )

        # Verify that the network offering got created
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Disabled",
            "The network offering created should be by default disabled."
        )

        self.debug(
            "Shared Network Offering created: %s" %
            self.shared_network_offering.id)

        # Update network offering state from disabled to enabled.
        NetworkOffering.update(
            self.shared_network_offering,
            self.api_client,
            id=self.shared_network_offering.id,
            state="enabled"
        )
        # Verify that the state of the network offering is updated
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Enabled",
            "The network offering state should get updated to Enabled."
        )

        # create network using the shared network offering created
        self.testdata["shared_network"]["acltype"] = "Account"
        self.testdata["shared_network"][
            "networkofferingid"] = self.shared_network_offering.id
        self.testdata["shared_network"][
            "physicalnetworkid"] = physical_network.id
        self.testdata["shared_network"]["vlan"] = shared_vlan

        self.network = Network.create(
            self.api_client,
            self.testdata["shared_network"],
            accountid=self.user_account.name,
            domainid=self.user_account.domainid,
            networkofferingid=self.shared_network_offering.id,
            zoneid=self.zone.id
        )

        self.cleanup_networks.append(self.network)

        list_networks_response = Network.list(
            self.api_client,
            id=self.network.id
        )
        self.assertEqual(
            isinstance(list_networks_response, list),
            True,
            "listNetworks returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_networks_response),
            0,
            "listNetworks returned empty list."
        )
        self.assertEqual(
            list_networks_response[0].specifyipranges,
            True,
            "The network is created with ip range but the flag is\
                    set to False.")

        self.debug("Network created: %s" % self.network.id)

        try:
            self.admin_account_virtual_machine = VirtualMachine.create(
                self.api_client,
                self.testdata["virtual_machine"],
                accountid=self.admin_account.name,
                domainid=self.admin_account.domainid,
                networkids=self.network.id,
                serviceofferingid=self.service_offering.id
            )
            self.fail(
                "Virtual Machine got created in admin account with network\
                 created but the network used is of scope account and for\
                 user account.")
        except Exception as e:
            self.debug(
                "Virtual Machine creation failed as network used have scoped\
                only for user account. Exception: %s" % e)

        self.user_account_virtual_machine = VirtualMachine.create(
            self.api_client,
            self.testdata["virtual_machine"],
            accountid=self.user_account.name,
            domainid=self.user_account.domainid,
            networkids=self.network.id,
            serviceofferingid=self.service_offering.id
        )
        vms = VirtualMachine.list(
            self.api_client,
            id=self.user_account_virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "listVirtualMachines returned invalid object in response."
        )
        self.assertNotEqual(
            len(vms),
            0,
            "listVirtualMachines returned empty list."
        )

        ip_range = list(
            netaddr.iter_iprange(
                str(
                    self.testdata["shared_network"]["startip"]), str(
                    self.testdata["shared_network"]["endip"])))
        if netaddr.IPAddress(str(vms[0].nic[0].ipaddress)) not in ip_range:
            self.fail(
                "Virtual machine ip should be from the ip range assigned\
                 to network created.")

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_createSharedNetwork_domainSpecific(self):
        """ Test Shared Network with scope domain """

        # Steps,
        #  1. create an Admin Account - admin-XABU1
        #    create a domain - DOM
        #    create a domain admin account = domadmin-SOPJD
        #    create a user in domain - DOM
        #  2. listPhysicalNetworks in available zone
        #  3. createNetworkOffering:
        #    - name = "MySharedOffering"
        #    - guestiptype="shared"
        #    - services = {Dns, Dhcp, UserData}
        #    -  conservemode = false
        #    - specifyVlan = true
        #    - specifyIpRanges = true
        #  4. Enable network offering - updateNetworkOffering - state=Enabled
        #  5. createNetwork
        #    - name = mysharednetwork, displaytext = mysharednetwork
        #    - vlan = 123 (say)
        #    - networkofferingid = <mysharedoffering>
        #    - gw = 172.16.15.1, startip = 172.16.15.2 , endip = 172.16.15.200
        #    - netmask=255.255.255.0
        #    - scope = domain, domain = DOM
        #  6. deployVirtualMachine in this admin, domainadmin and user account
        #     & within networkid = <mysharednetwork>
        #  7. delete all the accounts
        # Validations,
        #  1. listAccounts state=enabled returns your accounts,
        #     listDomains - DOM should be created
        #  2. listPhysicalNetworks should return at least one
        #     active physical network
        #  3. listNetworkOfferings - name=mysharedoffering , should list
        #     offering in disabled state
        #  4. listNetworkOfferings - name=mysharedoffering, should list
        #     enabled offering
        #  5. listNetworks - name = mysharednetwork should list the
        #     successfully created network, verify the guestIp ranges and
        #     CIDR are as given in the createNetwork call
        #  6. VM should NOT be deployed in admin account
        #     VM should be deployed in user account and domain admin account
        #     verify VM's IP are within shared network range

        # Create admin account
        self.admin_account = Account.create(
            self.api_client,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )

        self.cleanup_accounts.append(self.admin_account)

        # verify that the account got created with state enabled
        list_accounts_response = Account.list(
            self.api_client,
            id=self.admin_account.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_accounts_response, list),
            True,
            "listAccounts returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_accounts_response),
            0,
            "listAccounts returned empty list."
        )
        self.assertEqual(
            list_accounts_response[0].state,
            "enabled",
            "The admin account created is not enabled."
        )

        self.debug("Admin type account created: %s" % self.admin_account.id)

        # create domain
        self.dom_domain = Domain.create(
            self.api_client,
            self.testdata["domain"],
        )

        self.cleanup_domains.append(self.dom_domain)

        # verify that the account got created with state enabled
        list_domains_response = Domain.list(
            self.api_client,
            id=self.dom_domain.id
        )
        self.assertEqual(
            isinstance(list_domains_response, list),
            True,
            "listDomains returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_domains_response),
            0,
            "listDomains returned empty list."
        )

        self.debug("Domain created: %s" % self.dom_domain.id)

        # Create admin account
        self.domain_admin_account = Account.create(
            self.api_client,
            self.testdata["account"],
            admin=True,
            domainid=self.dom_domain.id
        )

        self.cleanup_accounts.append(self.domain_admin_account)

        # verify that the account got created with state enabled
        list_accounts_response = Account.list(
            self.api_client,
            id=self.domain_admin_account.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_accounts_response, list),
            True,
            "listAccounts returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_accounts_response),
            0,
            "listAccounts returned empty list."
        )
        self.assertEqual(
            list_accounts_response[0].state,
            "enabled",
            "The domain admin account created is not enabled."
        )

        self.debug(
            "Domain admin account created: %s" %
            self.domain_admin_account.id)

        # Create an user account
        self.domain_user_account = Account.create(
            self.api_client,
            self.testdata["account"],
            admin=False,
            domainid=self.dom_domain.id
        )

        self.cleanup_accounts.append(self.domain_user_account)

        # verify that the account got created with state enabled
        list_accounts_response = Account.list(
            self.api_client,
            id=self.domain_user_account.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_accounts_response, list),
            True,
            "listAccounts returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_accounts_response),
            0,
            "listAccounts returned empty list."
        )
        self.assertEqual(
            list_accounts_response[0].state,
            "enabled",
            "The domain user account created is not enabled."
        )

        self.debug(
            "Domain user account created: %s" %
            self.domain_user_account.id)

        physical_network, shared_vlan = get_free_vlan(
            self.api_client, self.zone.id)
        if shared_vlan is None:
            self.fail("Failed to get free vlan id for shared network")

        self.debug("Physical Network found: %s" % physical_network.id)

        self.testdata["shared_network_offering"]["specifyVlan"] = "True"
        self.testdata["shared_network_offering"]["specifyIpRanges"] = "True"

        # Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
            self.api_client,
            self.testdata["shared_network_offering"],
            conservemode=False
        )

        # Verify that the network offering got created
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Disabled",
            "The network offering created should be by default disabled."
        )

        self.debug(
            "Shared Network Offering created: %s" %
            self.shared_network_offering.id)

        # Update network offering state from disabled to enabled.
        NetworkOffering.update(
            self.shared_network_offering,
            self.api_client,
            id=self.shared_network_offering.id,
            state="enabled"
        )

        # Verify that the state of the network offering is updated
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Enabled",
            "The network offering state should get updated to Enabled."
        )

        # create network using the shared network offering created
        self.testdata["shared_network"]["acltype"] = "domain"
        self.testdata["shared_network"][
            "networkofferingid"] = self.shared_network_offering.id
        self.testdata["shared_network"][
            "physicalnetworkid"] = physical_network.id
        self.testdata["shared_network"]["vlan"] = shared_vlan

        self.network = Network.create(
            self.api_client,
            self.testdata["shared_network"],
            accountid=self.domain_admin_account.name,
            domainid=self.dom_domain.id,
            networkofferingid=self.shared_network_offering.id,
            zoneid=self.zone.id
        )

        self.cleanup_networks.append(self.network)

        list_networks_response = Network.list(
            self.api_client,
            id=self.network.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_networks_response, list),
            True,
            "listNetworks returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_networks_response),
            0,
            "listNetworks returned empty list."
        )
        self.assertEqual(
            list_networks_response[0].specifyipranges,
            True,
            "The network is created with ip range but the flag is\
                    set to False.")

        self.debug("Shared Network created: %s" % self.network.id)

        try:
            self.admin_account_virtual_machine = VirtualMachine.create(
                self.api_client,
                self.testdata["virtual_machine"],
                accountid=self.admin_account.name,
                domainid=self.admin_account.domainid,
                networkids=self.network.id,
                serviceofferingid=self.service_offering.id
            )
            self.fail(
                "Virtual Machine got created in admin account with network\
                 specified but the network used is of scope domain and admin\
                 account is not part of this domain.")
        except Exception as e:
            self.debug(
                "Virtual Machine creation failed as network used have scoped\
                only for DOM domain. Exception: %s" % e)

        self.domain_user_account_virtual_machine = VirtualMachine.create(
            self.api_client,
            self.testdata["virtual_machine"],
            accountid=self.domain_user_account.name,
            domainid=self.domain_user_account.domainid,
            networkids=self.network.id,
            serviceofferingid=self.service_offering.id
        )
        self.cleanup_vms.append(self.domain_user_account_virtual_machine)
        vms = VirtualMachine.list(
            self.api_client,
            id=self.domain_user_account_virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "listVirtualMachines returned invalid object in response."
        )
        self.assertNotEqual(
            len(vms),
            0,
            "listVirtualMachines returned empty list."
        )

        ip_range = list(
            netaddr.iter_iprange(
                str(
                    self.testdata["shared_network"]["startip"]), str(
                    self.testdata["shared_network"]["endip"])))
        if netaddr.IPAddress(str(vms[0].nic[0].ipaddress)) not in ip_range:
            self.fail(
                "Virtual machine ip should be from the ip range\
                        assigned to network created.")

        self.domain_admin_account_virtual_machine = VirtualMachine.create(
            self.api_client,
            self.testdata["virtual_machine"],
            accountid=self.domain_admin_account.name,
            domainid=self.domain_admin_account.domainid,
            networkids=self.network.id,
            serviceofferingid=self.service_offering.id
        )
        self.cleanup_vms.append(self.domain_admin_account_virtual_machine)
        vms = VirtualMachine.list(
            self.api_client,
            id=self.domain_admin_account_virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "listVirtualMachines returned invalid object in response."
        )
        self.assertNotEqual(
            len(vms),
            0,
            "listVirtualMachines returned empty list."
        )

        ip_range = list(
            netaddr.iter_iprange(
                str(
                    self.testdata["shared_network"]["startip"]), str(
                    self.testdata["shared_network"]["endip"])))
        if netaddr.IPAddress(str(vms[0].nic[0].ipaddress)) not in ip_range:
            self.fail(
                "Virtual machine ip should be from the ip range assigne\
                 to network created.")

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_createSharedNetwork_projectSpecific(self):
        """ Test Shared Network with scope project  """

        # Steps,
        #  1. create an Admin Account - admin-XABU1
        #     create a project - proj-SADJKS
        #     create another project - proj-SLDJK
        #  2. listPhysicalNetworks in available zone
        #  3. createNetworkOffering:
        #    - name = "MySharedOffering"
        #    - guestiptype="shared"
        #    - services = {Dns, Dhcp, UserData}
        #    -  conservemode = false
        #    - specifyVlan = true
        #    - specifyIpRanges = true
        #  4. Enable network offering - updateNetworkOffering - state=Enabled
        #  5. createNetwork
        #    - name = mysharednetwork, displaytext = mysharednetwork
        #    - vlan = 123 (say)
        #    - networkofferingid = <mysharedoffering>
        #    - gw = 172.16.15.1, startip = 172.16.15.2 , endip = 172.16.15.200
        #    - netmask=255.255.255.0
        #    - scope = project, project =  proj-SLDJK
        #  6. deployVirtualMachine in admin, project and user account & within
        #     networkid = <mysharednetwork>
        #  7. delete all the accounts
        # Validations,
        #  1. listAccounts state=enabled returns your accounts, listDomains
        #     - DOM should be created
        #  2. listPhysicalNetworks should return at least one active physical
        #     network
        #  3. listNetworkOfferings - name=mysharedoffering , should list
        #     offering in disabled state
        #  4. listNetworkOfferings - name=mysharedoffering, should list
        #     enabled offering
        #  5. listNetworks - name = mysharednetwork should list the
        #     successfully created network, verify the guestIp ranges
        #     and CIDR are as given in the createNetwork call
        #  6. VM should NOT be deployed in admin account and user account
        #     VM should be deployed in project account only
        #     verify VM's IP are within shared network range

        # Create admin account
        self.admin_account = Account.create(
            self.api_client,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )

        self.cleanup_accounts.append(self.admin_account)

        # verify that the account got created with state enabled
        list_accounts_response = Account.list(
            self.api_client,
            id=self.admin_account.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_accounts_response, list),
            True,
            "listAccounts returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_accounts_response),
            0,
            "listAccounts returned empty list."
        )
        self.assertEqual(
            list_accounts_response[0].state,
            "enabled",
            "The admin account created is not enabled."
        )

        self.debug("Admin account created: %s" % self.admin_account.id)

        self.testdata["project"]["name"] = "proj-SADJKS"
        self.testdata["project"]["displaytext"] = "proj-SADJKS"

        self.project1 = Project.create(
            self.api_client,
            self.testdata["project"],
            account=self.admin_account.name,
            domainid=self.admin_account.domainid
        )

        self.cleanup_projects.append(self.project1)

        list_projects_response = Project.list(
            self.api_client,
            id=self.project1.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_projects_response, list),
            True,
            "listProjects returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_projects_response),
            0,
            "listProjects should return at least one."
        )

        self.debug("Project created: %s" % self.project1.id)

        self.testdata["project"]["name"] = "proj-SLDJK"
        self.testdata["project"]["displaytext"] = "proj-SLDJK"

        self.project2 = Project.create(
            self.api_client,
            self.testdata["project"],
            account=self.admin_account.name,
            domainid=self.admin_account.domainid
        )

        self.cleanup_projects.append(self.project2)

        list_projects_response = Project.list(
            self.api_client,
            id=self.project2.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_projects_response, list),
            True,
            "listProjects returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_projects_response),
            0,
            "listProjects should return at least one."
        )

        self.debug("Project2 created: %s" % self.project2.id)

        physical_network, shared_vlan = get_free_vlan(
            self.api_client, self.zone.id)
        if shared_vlan is None:
            self.fail("Failed to get free vlan id for shared network")

        self.debug("Physical Network found: %s" % physical_network.id)

        self.testdata["shared_network_offering"]["specifyVlan"] = "True"
        self.testdata["shared_network_offering"]["specifyIpRanges"] = "True"

        # Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
            self.api_client,
            self.testdata["shared_network_offering"],
            conservemode=False
        )

        # Verify that the network offering got created
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Disabled",
            "The network offering created should be by default disabled."
        )

        # Update network offering state from disabled to enabled.
        NetworkOffering.update(
            self.shared_network_offering,
            self.api_client,
            id=self.shared_network_offering.id,
            state="enabled"
        )

        # Verify that the state of the network offering is updated
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Enabled",
            "The network offering state should get updated to Enabled."
        )

        self.debug(
            "Shared Network found: %s" %
            self.shared_network_offering.id)

        # create network using the shared network offering created
        self.testdata["shared_network"]["acltype"] = "account"
        self.testdata["shared_network"][
            "networkofferingid"] = self.shared_network_offering.id
        self.testdata["shared_network"][
            "physicalnetworkid"] = physical_network.id
        self.testdata["shared_network"]["vlan"] = shared_vlan

        self.network = Network.create(
            self.api_client,
            self.testdata["shared_network"],
            projectid=self.project1.id,
            domainid=self.admin_account.domainid,
            networkofferingid=self.shared_network_offering.id,
            zoneid=self.zone.id
        )
        self.cleanup_networks.append(self.network)

        list_networks_response = Network.list(
            self.api_client,
            id=self.network.id,
            projectid=self.project1.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_networks_response, list),
            True,
            "listNetworks returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_networks_response),
            0,
            "listNetworks returned empty list."
        )
        self.assertEqual(
            list_networks_response[0].specifyipranges,
            True,
            "The network is created with ip range but the flag is\
             set to False")

        self.debug("Shared Network created: %s" % self.network.id)

        with self.assertRaises(Exception):
            self.project2_admin_account_virtual_machine =\
                VirtualMachine.create(
                    self.api_client,
                    self.testdata["virtual_machine"],
                    networkids=self.network.id,
                    projectid=self.project2.id,
                    serviceofferingid=self.service_offering.id)
        self.debug("Deploying a vm to project other than the one in which \
                   network is created raised an Exception as expected")
        self.project1_admin_account_virtual_machine = VirtualMachine.create(
            self.api_client,
            self.testdata["virtual_machine"],
            networkids=self.network.id,
            projectid=self.project1.id,
            serviceofferingid=self.service_offering.id
        )
        vms = VirtualMachine.list(
            self.api_client,
            id=self.project1_admin_account_virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "listVirtualMachines returned invalid object in response."
        )
        self.assertNotEqual(
            len(vms),
            0,
            "listVirtualMachines returned empty list."
        )

        ip_range = list(
            netaddr.iter_iprange(
                str(
                    self.testdata["shared_network"]["startip"]), str(
                    self.testdata["shared_network"]["endip"])))
        if netaddr.IPAddress(str(vms[0].nic[0].ipaddress)) not in ip_range:
            self.fail(
                "Virtual machine ip should be from the ip range assigned\
                 to network created.")

    @unittest.skip(
        "skipped - This is a redundant case and also this\
                is causing issue for rest fo the cases ")
    @attr(tags=["advanced", "advancedns", "NA"])
    def test_createSharedNetwork_usedVlan(self):
        """ Test Shared Network with used vlan 01 """

        # Steps,
        #  1. create an Admin account
        #  2. create a shared NetworkOffering
        #  3. enable the network offering
        #  4. listPhysicalNetworks
        #    - vlan = guest VLAN range = 10-90 (say)
        #  5. createNetwork
        #    - name = mysharednetwork, displaytext = mysharednetwork
        #    - vlan = any vlan between 10-90
        #    - networkofferingid = <mysharedoffering>
        #    - gw = 172.16.15.1, startip = 172.16.15.2 , endip = 172.16.15.200
        #    - netmask=255.255.255.0
        #    - scope = all
        #  6. delete admin account
        # Validations,
        #  1. listAccounts state=enabled returns your account
        #  2. listNetworkOfferings - name=mysharedoffering , should list
        #     offering in disabled state
        #  3. listNetworkOfferings - name=mysharedoffering, should list
        #     enabled offering
        #  4. listPhysicalNetworks should return at least one active
        #     physical network
        #  5. network creation should FAIL since VLAN is used for
        #     guest networks

        # Create admin account
        self.admin_account = Account.create(
            self.api_client,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )

        self.cleanup_accounts.append(self.admin_account)

        # verify that the account got created with state enabled
        list_accounts_response = Account.list(
            self.api_client,
            id=self.admin_account.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_accounts_response, list),
            True,
            "listAccounts returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_accounts_response),
            0,
            "listAccounts returned empty list."
        )
        self.assertEqual(
            list_accounts_response[0].state,
            "enabled",
            "The admin account created is not enabled."
        )

        self.debug("Domain admin account created: %s" % self.admin_account.id)

        physical_network, shared_vlan = get_free_vlan(
            self.api_client, self.zone.id)
        if shared_vlan is None:
            self.fail("Failed to get free vlan id for shared network")

        self.testdata["shared_network_offering"]["specifyVlan"] = "True"
        self.testdata["shared_network_offering"]["specifyIpRanges"] = "True"

        # Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
            self.api_client,
            self.testdata["shared_network_offering"],
            conservemode=False
        )

        # Verify that the network offering got created
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Disabled",
            "The network offering created should be bydefault disabled."
        )

        self.debug(
            "Shared Network Offering created: %s" %
            self.shared_network_offering.id)

        # Update network offering state from disabled to enabled.
        NetworkOffering.update(
            self.shared_network_offering,
            self.api_client,
            id=self.shared_network_offering.id,
            state="enabled"
        )

        # Verify that the state of the network offering is updated
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Enabled",
            "The network offering state should get updated to Enabled."
        )

        # create network using the shared network offering created
        self.testdata["shared_network"]["vlan"] = str.split(
            str(physical_network.vlan), "-")[0]
        self.testdata["shared_network"]["acltype"] = "domain"
        self.testdata["shared_network"][
            "networkofferingid"] = self.shared_network_offering.id
        self.testdata["shared_network"][
            "physicalnetworkid"] = physical_network.id
        self.testdata["shared_network"]["vlan"] = shared_vlan

        try:
            self.network = Network.create(
                self.api_client,
                self.testdata["shared_network"],
                networkofferingid=self.shared_network_offering.id,
                zoneid=self.zone.id,
            )
            self.fail(
                "Network created with used vlan %s id, which is invalid" %
                shared_vlan)
        except Exception as e:
            self.debug(
                "Network creation failed because the valn id being used by\
                 another network. Exception: %s" % e)

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_createSharedNetwork_usedVlan2(self):
        """ Test Shared Network with used vlan 02 """

        # Steps,
        #  1. create an Admin account
        #  2. create a shared NetworkOffering
        #  3. enable the network offering
        #  4. listPhysicalNetworks
        #    - vlan = guest VLAN range = 10-90 (say)
        #  5. createNetwork
        #    - name = mysharednetwork, displaytext = mysharednetwork
        #    - vlan = any vlan beyond 10-90 (123 for eg)
        #    - networkofferingid = <mysharedoffering>
        #    - gw = 172.16.15.1, startip = 172.16.15.2 , endip = 172.16.15.200
        #    - netmask=255.255.255.0
        #    - scope = all
        #  6. createNetwork again with same VLAN  but different IP ranges and
        #     gateway
        #  7. delete admin account
        # Validations,
        #  1. listAccounts state=enabled returns your account
        #  2. listNetworkOfferings - name=mysharedoffering , should list
        #     offering in disabled state
        #  3. listNetworkOfferings - name=mysharedoffering, should list
        #     enabled offering
        #  4. listPhysicalNetworks should return at least one active
        #     physical network
        #  5. network creation shoud PASS
        #  6. network creation should FAIL since VLAN is already used by
        #     previously created network

        # Create admin account
        self.admin_account = Account.create(
            self.api_client,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup_accounts.append(self.admin_account)

        # verify that the account got created with state enabled
        list_accounts_response = Account.list(
            self.api_client,
            id=self.admin_account.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_accounts_response, list),
            True,
            "listAccounts returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_accounts_response),
            0,
            "listAccounts returned empty list."
        )
        self.assertEqual(
            list_accounts_response[0].state,
            "enabled",
            "The admin account created is not enabled."
        )

        self.debug("Admin account created: %s" % self.admin_account.id)

        physical_network, shared_vlan = get_free_vlan(
            self.api_client, self.zone.id)
        if shared_vlan is None:
            self.fail("Failed to get free vlan id for shared network")

        self.debug("Physical Network found: %s" % physical_network.id)

        self.testdata["shared_network_offering"]["specifyVlan"] = "True"
        self.testdata["shared_network_offering"]["specifyIpRanges"] = "True"

        # Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
            self.api_client,
            self.testdata["shared_network_offering"],
            conservemode=False
        )

        # Verify that the network offering got created
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Disabled",
            "The network offering created should be bydefault disabled."
        )

        self.debug(
            "Shared Network Offering created: %s" %
            self.shared_network_offering.id)

        # Update network offering state from disabled to enabled.
        NetworkOffering.update(
            self.shared_network_offering,
            self.api_client,
            id=self.shared_network_offering.id,
            state="enabled"
        )

        # Verify that the state of the network offering is updated
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Enabled",
            "The network offering state should get updated to Enabled."
        )

        # create network using the shared network offering created
        self.testdata["shared_network"]["acltype"] = "Domain"
        self.testdata["shared_network"][
            "networkofferingid"] = self.shared_network_offering.id
        self.testdata["shared_network"][
            "physicalnetworkid"] = physical_network.id
        self.testdata["shared_network"]["vlan"] = shared_vlan
        self.debug(
            "Creating a shared network in non-cloudstack VLAN %s" %
            shared_vlan)
        self.network = Network.create(
            self.api_client,
            self.testdata["shared_network"],
            networkofferingid=self.shared_network_offering.id,
            zoneid=self.zone.id,
        )

        self.cleanup_networks.append(self.network)

        list_networks_response = Network.list(
            self.api_client,
            id=self.network.id
        )
        self.assertEqual(
            isinstance(list_networks_response, list),
            True,
            "listNetworks returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_networks_response),
            0,
            "listNetworks returned empty list."
        )
        self.assertEqual(
            list_networks_response[0].specifyipranges,
            True,
            "The network is created with ip range but the flag is\
             set to False.")

        self.debug("Network created: %s" % self.network.id)

        shared_network_subnet_number = random.randrange(1, 254)

        self.testdata["shared_network"]["gateway"] = "172.16." + \
            str(shared_network_subnet_number) + ".1"
        self.testdata["shared_network"]["startip"] = "172.16." + \
            str(shared_network_subnet_number) + ".2"
        self.testdata["shared_network"]["endip"] = "172.16." + \
            str(shared_network_subnet_number) + ".20"

        self.testdata["shared_network"]["acltype"] = "domain"
        self.testdata["shared_network"][
            "networkofferingid"] = self.shared_network_offering.id
        self.testdata["shared_network"][
            "physicalnetworkid"] = physical_network.id

        try:
            self.network1 = Network.create(
                self.api_client,
                self.testdata["shared_network"],
                networkofferingid=self.shared_network_offering.id,
                zoneid=self.zone.id,
            )
            self.cleanup_networks.append(self.network1)
            self.fail(
                "Network got created with used vlan id, which is invalid")
        except Exception as e:
            self.debug(
                "Network creation failed because the valn id being used by\
                 another network. Exception: %s" % e)

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_deployVM_multipleSharedNetwork(self):
        """ Test Vm deployment with multiple shared networks """

        # Steps,
        #  0. create a user account
        #  1. Create two shared Networks (scope=ALL, different IP ranges)
        #  2. deployVirtualMachine in both the above networkids within the
        #     user account
        #  3. delete the user account
        # Validations,
        #  1. shared networks should be created successfully
        #  2. a. VM should deploy successfully
        # b. VM should be deployed in both networks and have IP in both the
        # networks

        # Create admin account
        self.admin_account = Account.create(
            self.api_client,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )

        self.cleanup_accounts.append(self.admin_account)

        # verify that the account got created with state enabled
        list_accounts_response = Account.list(
            self.api_client,
            id=self.admin_account.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_accounts_response, list),
            True,
            "listAccounts returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_accounts_response),
            0,
            "listAccounts returned empty list."
        )
        self.assertEqual(
            list_accounts_response[0].state,
            "enabled",
            "The admin account created is not enabled."
        )

        self.debug("Admin account created: %s" % self.admin_account.id)

        physical_network, shared_vlan = get_free_vlan(
            self.api_client, self.zone.id)
        if shared_vlan is None:
            self.fail("Failed to get free vlan id for shared network")

        self.debug("Physical Network found: %s" % physical_network.id)

        self.testdata["shared_network_offering"]["specifyVlan"] = "True"
        self.testdata["shared_network_offering"]["specifyIpRanges"] = "True"

        # Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
            self.api_client,
            self.testdata["shared_network_offering"],
            conservemode=False
        )

        # Verify that the network offering got created
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Disabled",
            "The network offering created should be bydefault disabled."
        )

        self.debug(
            "Shared Network offering created: %s" %
            self.shared_network_offering.id)

        # Update network offering state from disabled to enabled.
        NetworkOffering.update(
            self.shared_network_offering,
            self.api_client,
            id=self.shared_network_offering.id,
            state="enabled"
        )

        # Verify that the state of the network offering is updated
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Enabled",
            "The network offering state should get updated to Enabled."
        )

        # create network using the shared network offering created
        self.testdata["shared_network"]["acltype"] = "domain"
        self.testdata["shared_network"][
            "networkofferingid"] = self.shared_network_offering.id
        self.testdata["shared_network"][
            "physicalnetworkid"] = physical_network.id
        self.testdata["shared_network"]["vlan"] = shared_vlan

        self.network = Network.create(
            self.api_client,
            self.testdata["shared_network"],
            networkofferingid=self.shared_network_offering.id,
            zoneid=self.zone.id,
        )

        self.cleanup_networks.append(self.network)

        list_networks_response = Network.list(
            self.api_client,
            id=self.network.id
        )
        self.assertEqual(
            isinstance(list_networks_response, list),
            True,
            "listNetworks returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_networks_response),
            0,
            "listNetworks returned empty list."
        )
        self.assertEqual(
            list_networks_response[0].specifyipranges,
            True,
            "The network is created with ip range but the flag is\
             set to False.")

        self.debug("Shared Network created: %s" % self.network.id)

        shared_network_subnet_number = random.randrange(1, 254)

        self.testdata["shared_network"]["gateway"] = "172.16." + \
            str(shared_network_subnet_number) + ".1"
        self.testdata["shared_network"]["startip"] = "172.16." + \
            str(shared_network_subnet_number) + ".2"
        self.testdata["shared_network"]["endip"] = "172.16." + \
            str(shared_network_subnet_number) + ".20"

        self.testdata["shared_network"]["acltype"] = "domain"
        self.testdata["shared_network"][
            "networkofferingid"] = self.shared_network_offering.id
        self.testdata["shared_network"][
            "physicalnetworkid"] = physical_network.id

        shared_vlan = get_free_vlan(self.api_client, self.zone.id)[1]
        if shared_vlan is None:
            self.fail("Failed to get free vlan id for shared network")

        self.testdata["shared_network"]["vlan"] = shared_vlan

        self.network1 = Network.create(
            self.api_client,
            self.testdata["shared_network"],
            networkofferingid=self.shared_network_offering.id,
            zoneid=self.zone.id,
        )

        self.cleanup_networks.append(self.network1)

        list_networks_response = Network.list(
            self.api_client,
            id=self.network1.id
        )
        self.assertEqual(
            isinstance(list_networks_response, list),
            True,
            "listNetworks returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_networks_response),
            0,
            "listNetworks returned empty list."
        )
        self.assertEqual(
            list_networks_response[0].specifyipranges,
            True,
            "The network is created with ip range but the flag is\
             set to False.")

        self.debug("Network created: %s" % self.network1.id)

        self.network_admin_account_virtual_machine = VirtualMachine.create(
            self.api_client,
            self.testdata["virtual_machine"],
            accountid=self.admin_account.name,
            domainid=self.admin_account.domainid,
            networkids=self.network.id,
            serviceofferingid=self.service_offering.id
        )
        vms = VirtualMachine.list(
            self.api_client,
            id=self.network_admin_account_virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "listVirtualMachines returned invalid object in response."
        )
        self.assertNotEqual(
            len(vms),
            0,
            "listVirtualMachines returned empty list."
        )

        self.debug(
            "Virtual Machine created: %s" %
            self.network_admin_account_virtual_machine.id)

        self.assertTrue(
            self.network_admin_account_virtual_machine.nic[0].ipaddress
            is not None,
            "ip should be assigned to running virtual machine")

        self.network1_admin_account_virtual_machine = VirtualMachine.create(
            self.api_client,
            self.testdata["virtual_machine"],
            accountid=self.admin_account.name,
            domainid=self.admin_account.domainid,
            networkids=self.network1.id,
            serviceofferingid=self.service_offering.id
        )
        vms = VirtualMachine.list(
            self.api_client,
            id=self.network1_admin_account_virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "listVirtualMachines returned invalid object in response."
        )
        self.assertNotEqual(
            len(vms),
            0,
            "listVirtualMachines returned empty list."
        )
        self.debug(
            "Virtual Machine created: %s" %
            self.network1_admin_account_virtual_machine.id)

        self.assertTrue(
            self.network1_admin_account_virtual_machine.nic[0].ipaddress
            is not None,
            "ip should be assigned to running virtual machine")

    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    def test_deployVM_isolatedAndShared(self):
        """ Test VM deployment in shared and isolated networks """

        # Steps,
        #  0. create a user account
        #  1. Create one shared Network (scope=ALL, different IP ranges)
        #  2. Create one Isolated Network
        #  3. deployVirtualMachine in both the above networkids within
        #     the user account
        #  4. apply FW rule and enable PF for port 22 for guest VM on
        #     isolated network
        #  5. delete the user account
        # Validations,
        #  1. shared network should be created successfully
        #  2. isolated network should be created successfully
        #  3.
        #    a. VM should deploy successfully
        #    b. VM should be deployed in both networks and have IP in both
        #       the networks
        # 4. FW and PF should apply successfully, ssh into the VM should work
        # over isolated network

        # Create admin account
        self.admin_account = Account.create(
            self.api_client,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )

        self.cleanup_accounts.append(self.admin_account)

        # verify that the account got created with state enabled
        list_accounts_response = Account.list(
            self.api_client,
            id=self.admin_account.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_accounts_response, list),
            True,
            "listAccounts returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_accounts_response),
            0,
            "listAccounts returned empty list."
        )
        self.assertEqual(
            list_accounts_response[0].state,
            "enabled",
            "The admin account created is not enabled."
        )

        self.debug("Admin type account created: %s" % self.admin_account.name)

        self.testdata["shared_network_offering"]["specifyVlan"] = "True"
        self.testdata["shared_network_offering"]["specifyIpRanges"] = "True"

        # Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
            self.api_client,
            self.testdata["shared_network_offering"],
            conservemode=False
        )

        # Verify that the network offering got created
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Disabled",
            "The network offering created should be bydefault disabled."
        )

        self.debug(
            "Shared Network offering created: %s" %
            self.shared_network_offering.id)

        # Update network offering state from disabled to enabled.
        NetworkOffering.update(
            self.shared_network_offering,
            self.api_client,
            id=self.shared_network_offering.id,
            state="enabled"
        )

        # Verify that the state of the network offering is updated
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Enabled",
            "The network offering state should get updated to Enabled."
        )

        self.isolated_network_offering = NetworkOffering.create(
            self.api_client,
            self.testdata["isolated_network_offering"],
            conservemode=False
        )

        # Update network offering state from disabled to enabled.
        NetworkOffering.update(
            self.isolated_network_offering,
            self.api_client,
            id=self.isolated_network_offering.id,
            state="enabled"
        )

        # Verify that the state of the network offering is updated
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.isolated_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Enabled",
            "The isolated network offering state should get\
             updated to Enabled.")

        self.debug(
            "Isolated Network Offering created: %s" %
            self.isolated_network_offering.id)

        physical_network, shared_vlan = get_free_vlan(
            self.api_client, self.zone.id)
        if shared_vlan is None:
            self.fail("Failed to get free vlan id for shared network")

        # create network using the shared network offering created
        self.testdata["shared_network"]["acltype"] = "domain"
        self.testdata["shared_network"][
            "networkofferingid"] = self.shared_network_offering.id
        self.testdata["shared_network"][
            "physicalnetworkid"] = physical_network.id
        self.testdata["shared_network"]["vlan"] = shared_vlan

        self.shared_network = Network.create(
            self.api_client,
            self.testdata["shared_network"],
            accountid=self.admin_account.name,
            domainid=self.admin_account.domainid,
            networkofferingid=self.shared_network_offering.id,
            zoneid=self.zone.id
        )

        self.cleanup_networks.append(self.shared_network)

        list_networks_response = Network.list(
            self.api_client,
            id=self.shared_network.id
        )
        self.assertEqual(
            isinstance(list_networks_response, list),
            True,
            "listNetworks returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_networks_response),
            0,
            "listNetworks returned empty list."
        )
        self.assertEqual(
            list_networks_response[0].specifyipranges,
            True,
            "The network is created with ip range but the flag is\
                    set to False.")

        self.debug("Shared Network created: %s" % self.shared_network.id)

        self.isolated_network = Network.create(
            self.api_client,
            self.testdata["isolated_network"],
            accountid=self.admin_account.name,
            domainid=self.admin_account.domainid,
            networkofferingid=self.isolated_network_offering.id,
            zoneid=self.zone.id
        )

        self.cleanup_networks.append(self.isolated_network)

        list_networks_response = Network.list(
            self.api_client,
            id=self.isolated_network.id
        )
        self.assertEqual(
            isinstance(list_networks_response, list),
            True,
            "listNetworks returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_networks_response),
            0,
            "listNetworks returned empty list."
        )

        self.debug("Isolated Network created: %s" % self.isolated_network.id)

        self.shared_network_admin_account_virtual_machine = \
            VirtualMachine.create(
                self.api_client,
                self.testdata["virtual_machine"],
                accountid=self.admin_account.name,
                domainid=self.admin_account.domainid,
                networkids=self.shared_network.id,
                serviceofferingid=self.service_offering.id
            )
        vms = VirtualMachine.list(
            self.api_client,
            id=self.shared_network_admin_account_virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "listVirtualMachines returned invalid object in response."
        )
        self.assertNotEqual(
            len(vms),
            0,
            "listVirtualMachines returned empty list."
        )
        self.debug(
            "Virtual Machine created: %s" %
            self.shared_network_admin_account_virtual_machine.id)

        self.assertTrue(
            self.shared_network_admin_account_virtual_machine.nic[0].ipaddress
            is not None,
            "ip should be assigned to running virtual machine")

        self.isolated_network_admin_account_virtual_machine = \
            VirtualMachine.create(
                self.api_client,
                self.testdata["virtual_machine"],
                accountid=self.admin_account.name,
                domainid=self.admin_account.domainid,
                networkids=self.isolated_network.id,
                serviceofferingid=self.service_offering.id
            )
        vms = VirtualMachine.list(
            self.api_client,
            id=self.isolated_network_admin_account_virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "listVirtualMachines returned invalid object in response."
        )
        self.assertNotEqual(
            len(vms),
            0,
            "listVirtualMachines returned empty list."
        )

        self.debug(
            "Virtual Machine created: %s" %
            self.isolated_network_admin_account_virtual_machine.id)

        self.assertTrue(
            self.isolated_network_admin_account_virtual_machine.nic[0].ipaddress
            is not None,
            "ip should be assigned to running virtual machine")

        self.debug(
            "Associating public IP for account: %s" %
            self.admin_account.name)
        self.public_ip = PublicIPAddress.create(
            self.api_client,
            accountid=self.admin_account.name,
            zoneid=self.zone.id,
            domainid=self.admin_account.domainid,
            networkid=self.isolated_network.id
        )

        self.debug(
            "Associated %s with network %s" %
            (self.public_ip.ipaddress.ipaddress, self.isolated_network.id))
        self.debug(
            "Creating PF rule for IP address: %s" %
            self.public_ip.ipaddress.ipaddress)

        public_ip = self.public_ip.ipaddress

        # Enable Static NAT for VM
        StaticNATRule.enable(
            self.api_client,
            public_ip.id,
            self.isolated_network_admin_account_virtual_machine.id
        )

        self.debug("Enabled static NAT for public IP ID: %s" % public_ip.id)
        # Create Firewall rule on source NAT
        fw_rule = FireWallRule.create(
            self.api_client,
            ipaddressid=self.public_ip.ipaddress.id,
            protocol='TCP',
            cidrlist=[self.testdata["fwrule"]["cidr"]],
            startport=self.testdata["fwrule"]["startport"],
            endport=self.testdata["fwrule"]["endport"]
        )
        self.debug("Created firewall rule: %s" % fw_rule.id)

        fw_rules = FireWallRule.list(
            self.api_client,
            id=fw_rule.id
        )
        self.assertEqual(
            isinstance(fw_rules, list),
            True,
            "List fw rules should return a valid firewall rules"
        )

        self.assertNotEqual(
            len(fw_rules),
            0,
            "Length of fw rules response should not be zero"
        )

        # Should be able to SSH VM
        try:
            self.debug(
                "SSH into VM: %s" %
                self.isolated_network_admin_account_virtual_machine.id)
            self.isolated_network_admin_account_virtual_machine.get_ssh_client(
                ipaddress=self.public_ip.ipaddress.ipaddress)
        except Exception as e:
            self.fail(
                "SSH Access failed for %s: %s" %
                (self.isolated_network_admin_account_virtual_machine.ipaddress,
                    e))

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_networkWithsubdomainaccessTrue(self):
        """ Test Shared Network with subdomainaccess=True """

        # Steps,
        #  1. create Network using shared network offering for scope=Account
        #     and subdomainaccess=true.
        # Validations,
        #  (Expected) API should fail saying that subdomainaccess cannot be
        #   given when scope is Account

        # Create admin account
        self.admin_account = Account.create(
            self.api_client,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )

        self.cleanup_accounts.append(self.admin_account)

        # verify that the account got created with state enabled
        list_accounts_response = Account.list(
            self.api_client,
            id=self.admin_account.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_accounts_response, list),
            True,
            "listAccounts returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_accounts_response),
            0,
            "listAccounts returned empty list."
        )
        self.assertEqual(
            list_accounts_response[0].state,
            "enabled",
            "The admin account created is not enabled."
        )

        self.debug("Admin type account created: %s" % self.admin_account.id)

        physical_network, shared_vlan = get_free_vlan(
            self.api_client, self.zone.id)
        if shared_vlan is None:
            self.fail("Failed to get free vlan id for shared network")

        self.debug("Physical Network found: %s" % physical_network.id)

        self.testdata["shared_network_offering"]["specifyVlan"] = "True"
        self.testdata["shared_network_offering"]["specifyIpRanges"] = "True"

        # Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
            self.api_client,
            self.testdata["shared_network_offering"],
            conservemode=False
        )

        # Verify that the network offering got created
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Disabled",
            "The network offering created should be bydefault disabled."
        )

        self.debug(
            "Shared Network Offering created: %s" %
            self.shared_network_offering.id)

        # Update network offering state from disabled to enabled.
        NetworkOffering.update(
            self.shared_network_offering,
            self.api_client,
            id=self.shared_network_offering.id,
            state="enabled"
        )
        # Verify that the state of the network offering is updated
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Enabled",
            "The network offering state should get updated to Enabled."
        )

        # create network using the shared network offering created
        self.testdata["shared_network"]["acltype"] = "Account"
        self.testdata["shared_network"][
            "networkofferingid"] = self.shared_network_offering.id
        self.testdata["shared_network"][
            "physicalnetworkid"] = physical_network.id
        self.testdata["shared_network"]["vlan"] = shared_vlan
        self.testdata["shared_network"]["subdomainaccess"] = "True"

        try:
            self.network = Network.create(
                self.api_client,
                self.testdata["shared_network"],
                accountid=self.admin_account.name,
                domainid=self.admin_account.domainid,
                networkofferingid=self.shared_network_offering.id,
                zoneid=self.zone.id
            )
            self.fail("Network creation should fail.")
        except:
            self.debug(
                "Network creation failed because subdomainaccess parameter was\
                 passed when scope was account.")

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_networkWithsubdomainaccessFalse(self):
        """ Test shared Network with subdomainaccess=False """

        # Steps,
        #  1. create Network using shared network offering for scope=Account
        #     and subdomainaccess=false
        #  Validations,
        #  (Expected) API should fail saying that subdomainaccess cannot be
        #  given when scope is Account

        # Create admin account
        self.admin_account = Account.create(
            self.api_client,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )

        self.cleanup_accounts.append(self.admin_account)

        # verify that the account got created with state enabled
        list_accounts_response = Account.list(
            self.api_client,
            id=self.admin_account.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_accounts_response, list),
            True,
            "listAccounts returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_accounts_response),
            0,
            "listAccounts returned empty list."
        )
        self.assertEqual(
            list_accounts_response[0].state,
            "enabled",
            "The admin account created is not enabled."
        )

        self.debug("Admin type account created: %s" % self.admin_account.id)

        physical_network, shared_vlan = get_free_vlan(
            self.api_client, self.zone.id)
        if shared_vlan is None:
            self.fail("Failed to get free vlan id for shared network")

        self.debug("Physical Network found: %s" % physical_network.id)

        self.testdata["shared_network_offering"]["specifyVlan"] = "True"
        self.testdata["shared_network_offering"]["specifyIpRanges"] = "True"

        # Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
            self.api_client,
            self.testdata["shared_network_offering"],
            conservemode=False
        )

        # Verify that the network offering got created
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Disabled",
            "The network offering created should be bydefault disabled."
        )

        self.debug(
            "Shared Network Offering created: %s" %
            self.shared_network_offering.id)

        # Update network offering state from disabled to enabled.
        NetworkOffering.update(
            self.shared_network_offering,
            self.api_client,
            id=self.shared_network_offering.id,
            state="enabled"
        )
        # Verify that the state of the network offering is updated
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_offerings_response, list),
            True,
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_network_offerings_response),
            0,
            "listNetworkOfferings returned empty list."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Enabled",
            "The network offering state should get updated to Enabled."
        )

        # create network using the shared network offering created
        self.testdata["shared_network"]["acltype"] = "Account"
        self.testdata["shared_network"][
            "networkofferingid"] = self.shared_network_offering.id
        self.testdata["shared_network"][
            "physicalnetworkid"] = physical_network.id
        self.testdata["shared_network"]["vlan"] = shared_vlan
        self.testdata["shared_network"]["subdomainaccess"] = "False"

        try:
            self.network = Network.create(
                self.api_client,
                self.testdata["shared_network"],
                accountid=self.admin_account.name,
                domainid=self.admin_account.domainid,
                networkofferingid=self.shared_network_offering.id,
                zoneid=self.zone.id
            )
            self.fail("Network creation should fail.")
        except:
            self.debug(
                "Network creation failed because subdomainaccess parameter\
                        was passed when scope was account.")

    @attr(tags=["advanced"], required_hardware="false")
    def test_escalation_ES1621(self):
        """
        @summary: ES1621:Allow creating shared networks with overlapping
                  ip ranges in different vlans
        @steps:
        Step1: Create an Admin account for the test
        Step2: Create shared network offering
        Step3: Update the network offering to Enabled state
        Step4: list network offering
        Step5: Create network with above offering
        Step6: List netwokrs and verify the network created in
               step5 in the response
        Step7: Create another network with offering,vlan and ip range
               same as in step6
        Step8: Verify that network creationin Step7 should fail
        Step9: Repeat step6 with diff vlan but same ip range and network
               offering
        Step10: List netwokrs and verify the network created in step9
                in the response
        Step11: Dislable network offering for the cleanup to delete at
                the end of the test
        """
        # Creating Admin account
        self.admin_account = Account.create(
            self.api_client,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup_accounts.append(self.admin_account)
        # verify that the account got created with state enabled
        list_accounts_response = Account.list(
            self.api_client,
            id=self.admin_account.id,
            listall=True
        )
        status = validateList(list_accounts_response)
        self.assertEqual(
            PASS,
            status[0],
            "listAccounts returned invalid object in response"
        )
        self.assertEqual(
            list_accounts_response[0].state,
            "enabled",
            "The admin account created is not enabled."
        )
        self.debug("Admin type account created: %s" % self.admin_account.name)
        physical_network, shared_vlan = get_free_vlan(
            self.api_client, self.zone.id)
        if shared_vlan is None:
            self.fail("Failed to get free vlan id for shared network")
        self.debug("Physical network found: %s" % physical_network.id)
        self.testdata["shared_network_offering"]["specifyVlan"] = "True"
        self.testdata["shared_network_offering"]["specifyIpRanges"] = "True"
        # Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
            self.api_client,
            self.testdata["shared_network_offering"],
            conservemode=False
        )
        # Verify that the network offering got created
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        status = validateList(list_network_offerings_response)
        self.assertEqual(
            PASS,
            status[0],
            "listNetworkOfferings returned invalid object in response."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Disabled",
            "The network offering created should be bydefault disabled."
        )
        self.debug(
            "Shared Network offering created: %s" %
            self.shared_network_offering.id)
        # Update network offering state from disabled to enabled.
        NetworkOffering.update(
            self.shared_network_offering,
            self.api_client,
            id=self.shared_network_offering.id,
            state="enabled"
        )
        # Verify that the state of the network offering is updated
        list_network_offerings_response = NetworkOffering.list(
            self.api_client,
            id=self.shared_network_offering.id
        )
        status = validateList(list_network_offerings_response)
        self.assertEqual(
            PASS,
            status[0],
            "listNetworkOfferings returned invalid object in\
                    response after enabling it."
        )
        self.assertEqual(
            list_network_offerings_response[0].state,
            "Enabled",
            "The network offering state should get updated to Enabled."
        )
        # create network using the shared network offering created
        self.testdata["shared_network"]["acltype"] = "Domain"
        self.testdata["shared_network"][
            "networkofferingid"] = self.shared_network_offering.id
        self.testdata["shared_network"][
            "physicalnetworkid"] = physical_network.id
        self.testdata["shared_network"]["vlan"] = shared_vlan
        self.network = Network.create(
            self.api_client,
            self.testdata["shared_network"],
            networkofferingid=self.shared_network_offering.id,
            zoneid=self.zone.id,
        )
        self.cleanup_networks.append(self.network)
        list_networks_response = Network.list(
            self.api_client,
            id=self.network.id
        )
        status = validateList(list_accounts_response)
        self.assertEqual(
            PASS,
            status[0],
            "listNetworks returned invalid object in response."
        )
        self.assertEqual(
            list_networks_response[0].specifyipranges,
            True,
            "The network is created with ip range but the flag is\
                    set to False.")
        self.debug(
            "Shared Network created for scope domain: %s" %
            self.network.id)
        # Create another network with same ip range and vlan. It should fail
        try:
            self.network1 = Network.create(
                self.api_client,
                self.testdata["shared_network"],
                networkofferingid=self.shared_network_offering.id,
                zoneid=self.zone.id,
            )
            self.cleanup_networks.append(self.network1)
            self.fail(
                "CS is allowing to create shared network with ip range and\
                        vlan same as used by another shared network")
        except Exception as e:
            self.debug("Network Creation Exception Raised: %s" % e)
        # Create another shared network with overlapped ip range but different
        # vlan
        physical_network, shared_vlan1 = get_free_vlan(
            self.api_client, self.zone.id)
        if shared_vlan1 is None:
            self.fail("Failed to get free vlan id for shared network")
        self.testdata["shared_network"]["vlan"] = shared_vlan1
        self.network2 = Network.create(
            self.api_client,
            self.testdata["shared_network"],
            networkofferingid=self.shared_network_offering.id,
            zoneid=self.zone.id,
        )
        self.cleanup_networks.append(self.network2)
        list_networks_response = Network.list(
            self.api_client,
            id=self.network2.id
        )
        status = validateList(list_networks_response)
        self.assertEqual(
            PASS,
            status[0],
            "listNetworks returned invalid object in response after\
                    creating with overlapped ip range in diff vlan."
        )
        self.assertEqual(
            list_networks_response[0].specifyipranges,
            True,
            "The network is created with ip range but the flag is set to\
                False after creating with overlapped ip range in diff vlan")
        self.debug(
            "Shared Network created for scope domain: %s" %
            self.network2.id)
        # Update network offering state from enabled to disabled.
        NetworkOffering.update(
            self.shared_network_offering,
            self.api_client,
            id=self.shared_network_offering.id,
            state="disabled"
        )
        self.cleanup_networks.append(self.shared_network_offering)
        return

    @data(True, False)
    @attr(tags=["advanced", "advancedns", "dvs"], required_hardware="false")
    def test_restart_network(self, cleanup):
        """ Test restart shared Network

        # Steps
        # 1. Create a shared network in an account
        # 2. Deploy a VM in the network
        # 3. Restart the network with cleanup true and false
        # 4. List the router for the network and verify that publicip of
             the router remain the same
        """

        # Create admin account
        account = Account.create(
            self.api_client,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup_accounts.append(account)

        physical_network, shared_vlan = get_free_vlan(
            self.api_client, self.zone.id)
        if shared_vlan is None:
            self.fail("Failed to get free vlan id for shared network")

        self.debug("Physical network found: %s" % physical_network.id)

        # create network using the shared network offering created
        self.testdata["shared_network"]["acltype"] = "Domain"
        self.testdata["shared_network"][
            "networkofferingid"] = self.shared_network_offering.id
        self.testdata["shared_network"][
            "physicalnetworkid"] = physical_network.id
        self.testdata["shared_network"]["vlan"] = shared_vlan

        shared_network = Network.create(
            self.api_client,
            self.testdata["shared_network"],
            networkofferingid=self.shared_network_offering.id,
            zoneid=self.zone.id,
        )

        self.cleanup_networks.append(shared_network)

        self.debug(
            "Shared Network created for scope domain: %s" %
            shared_network.id)

        VirtualMachine.create(
            self.api_client,
            self.testdata["virtual_machine"],
            networkids=shared_network.id,
            serviceofferingid=self.service_offering.id
        )

        list_router_response = Router.list(
            self.api_client,
            networkid=shared_network.id,
            listall=True
        )
        self.assertEqual(
            validateList(list_router_response)[0],
            PASS,
            "Router list validation failed"
        )
        router = list_router_response[0]
        # Store old values before restart
        old_publicip = router.publicip

        shared_network.restart(self.api_client, cleanup=cleanup)

        # Get router details after restart
        list_router_response = Router.list(
            self.api_client,
            networkid=shared_network.id,
            listall=True
        )
        self.assertEqual(
            validateList(list_router_response)[0],
            PASS,
            "Router list validation failed"
        )
        router = list_router_response[0]

        self.assertEqual(
            router.publicip,
            old_publicip,
            "Public IP of the router should remain same after network restart"
        )
        return

    @attr(tags=["advanced", "advancedns", "dvs"], required_hardware="false")
    def test_reboot_router(self):
        """Test reboot router

        # Steps
        # 1. Create a shared network in an account
        # 2. Deploy a VM in the network
        # 3. Restart the router related to shared network
        # 4. List the router for the network and verify that publicip of
             the router remain the same
        """

        # Create admin account
        account = Account.create(
            self.api_client,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup_accounts.append(account)

        physical_network, shared_vlan = get_free_vlan(
            self.api_client, self.zone.id)
        if shared_vlan is None:
            self.fail("Failed to get free vlan id for shared network")

        self.debug("Physical network found: %s" % physical_network.id)

        # create network using the shared network offering created
        self.testdata["shared_network"]["acltype"] = "Domain"
        self.testdata["shared_network"][
            "networkofferingid"] = self.shared_network_offering.id
        self.testdata["shared_network"][
            "physicalnetworkid"] = physical_network.id
        self.testdata["shared_network"]["vlan"] = shared_vlan

        shared_network = Network.create(
            self.api_client,
            self.testdata["shared_network"],
            networkofferingid=self.shared_network_offering.id,
            zoneid=self.zone.id,
        )

        self.cleanup_networks.append(shared_network)

        self.debug(
            "Shared Network created for scope domain: %s" %
            shared_network.id)

        vm = VirtualMachine.create(
            self.api_client,
            self.testdata["virtual_machine"],
            networkids=shared_network.id,
            serviceofferingid=self.service_offering.id
        )
        self.cleanup_vms.append(vm)

        list_router_response = Router.list(
            self.api_client,
            networkid=shared_network.id,
            listall=True
        )
        self.assertEqual(
            validateList(list_router_response)[0],
            PASS,
            "Router list validation failed"
        )
        router = list_router_response[0]
        # Store old values before restart
        public_ip = router.publicip

        self.debug("Rebooting the router with ID: %s" % router.id)
        # Stop the router
        cmd = rebootRouter.rebootRouterCmd()
        cmd.id = router.id
        self.api_client.rebootRouter(cmd)

        isRouterRunningSuccessfully = False

        # List routers to check state of router
        retries_cnt = 6
        while retries_cnt >= 0:
            router_response = Router.list(
                self.api_client,
                id=router.id
            )
            if self.verifyRouterResponse(router_response, public_ip):
                isRouterRunningSuccessfully = True
                break
            time.sleep(10)
            retries_cnt = retries_cnt - 1

        if not isRouterRunningSuccessfully:
            self.fail(
                "Router response after reboot is either is invalid\
                    or in stopped state")
        return

    @attr(tags=["advanced", "advancedns", "dvs"], required_hardware="false")
    def test_stop_start_router(self):
        """Test stop and start router

        # Steps
        # 1. Create a shared network in an account
        # 2. Deploy a VM in the network
        # 3. Stop the router related to shared network and start it again
        # 4. List the router for the network and verify that publicip of
             the router remain the same
        """

        # Create admin account
        account = Account.create(
            self.api_client,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup_accounts.append(account)

        physical_network, shared_vlan = get_free_vlan(
            self.api_client, self.zone.id)
        if shared_vlan is None:
            self.fail("Failed to get free vlan id for shared network")

        self.debug("Physical network found: %s" % physical_network.id)

        # create network using the shared network offering created
        self.testdata["shared_network"]["acltype"] = "Domain"
        self.testdata["shared_network"][
            "networkofferingid"] = self.shared_network_offering.id
        self.testdata["shared_network"][
            "physicalnetworkid"] = physical_network.id
        self.testdata["shared_network"]["vlan"] = shared_vlan

        shared_network = Network.create(
            self.api_client,
            self.testdata["shared_network"],
            networkofferingid=self.shared_network_offering.id,
            zoneid=self.zone.id,
        )

        self.cleanup_networks.append(shared_network)

        self.debug(
            "Shared Network created for scope domain: %s" %
            shared_network.id)

        vm = VirtualMachine.create(
            self.api_client,
            self.testdata["virtual_machine"],
            networkids=shared_network.id,
            serviceofferingid=self.service_offering.id
        )
        self.cleanup_vms.append(vm)

        list_router_response = Router.list(
            self.api_client,
            networkid=shared_network.id,
            listall=True
        )
        self.assertEqual(
            validateList(list_router_response)[0],
            PASS,
            "Router list validation failed"
        )
        router = list_router_response[0]

        self.debug("Stopping the router with ID: %s" % router.id)
        # Reboot the router
        cmd = stopRouter.stopRouterCmd()
        cmd.id = router.id
        self.api_client.stopRouter(cmd)

        response = verifyRouterState(self.api_client, router.id, "stopped")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)

        self.debug("Starting the router with ID: %s" % router.id)
        # Reboot the router
        cmd = startRouter.startRouterCmd()
        cmd.id = router.id
        self.api_client.startRouter(cmd)

        response = verifyRouterState(self.api_client, router.id, "running")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        return

    @attr(tags=["advanced", "advancedns", "dvs"], required_hardware="false")
    def test_acquire_ip(self):
        """Test acquire IP in shared network

        # Steps
        # 1. Create a shared network in an account
        # 2. Deploy a VM in the network
        # 3. Acquire a public IP in the network
        # 4. List the public IP by passing the id, it should be listed properly
        # 5. Create Firewall and NAT rules for the public IP and verify that
             ssh to vm works using the public IP
        # 6. Disassociate the public IP and try to list the public IP
        # 7. The list should be empty
        """

        # Create admin account
        account = Account.create(
            self.api_client,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup_accounts.append(account)

        physical_network, shared_vlan = get_free_vlan(
            self.api_client, self.zone.id)
        if shared_vlan is None:
            self.fail("Failed to get free vlan id for shared network")

        self.debug("Physical network found: %s" % physical_network.id)

        # create network using the shared network offering created
        self.testdata["shared_network"]["acltype"] = "Domain"
        self.testdata["shared_network"][
            "networkofferingid"] = self.shared_network_offering_all_services.id
        self.testdata["shared_network"][
            "physicalnetworkid"] = physical_network.id
        self.testdata["shared_network"]["vlan"] = shared_vlan

        shared_network = Network.create(
            self.api_client,
            self.testdata["shared_network"],
            networkofferingid=self.shared_network_offering_all_services.id,
            zoneid=self.zone.id,
        )

        self.cleanup_networks.append(shared_network)

        self.debug(
            "Shared Network created for scope domain: %s" %
            shared_network.id)

        vm = VirtualMachine.create(
            self.api_client,
            self.testdata["virtual_machine"],
            networkids=shared_network.id,
            serviceofferingid=self.service_offering.id
        )
        self.cleanup_vms.append(vm)

        public_ip = PublicIPAddress.create(
            self.api_client,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=shared_network.id)

        # listPublicIpAddresses should return newly created public IP
        list_pub_ip_addr_resp = PublicIPAddress.list(
            self.api_client,
            id=public_ip.ipaddress.id
        )
        self.assertEqual(
            validateList(list_pub_ip_addr_resp)[0],
            PASS,
            "IP address list validation failed"
        )
        self.assertEqual(
            list_pub_ip_addr_resp[0].id,
            public_ip.ipaddress.id,
            "Check Correct IP Address is returned in the List Call"
        )

        FireWallRule.create(
                self.api_client,
                ipaddressid=public_ip.ipaddress.id,
                protocol='TCP',
                cidrlist=[
                    self.testdata["fwrule"]["cidr"]],
                startport=self.testdata["fwrule"]["startport"],
                endport=self.testdata["fwrule"]["endport"])

        NATRule.create(
                    self.api_client,
                    vm,
                    self.testdata["natrule"],
                    ipaddressid=public_ip.ipaddress.id,
                    networkid=shared_network.id)

        SshClient(
                  public_ip.ipaddress.ipaddress,
                  vm.ssh_port,
                  vm.username,
                  vm.password
                  )

        public_ip.delete(self.api_client)

        list_pub_ip_addr_resp = PublicIPAddress.list(
            self.api_client,
            id=public_ip.ipaddress.id
        )

        self.assertEqual(
            list_pub_ip_addr_resp,
            None,
            "Check if disassociated IP Address is no longer available"
        )
        return

    @attr(tags=["dvs"], required_hardware="true")
    def test_guest_traffic_port_groups_shared_network(self):
        """ Verify vcenter port groups are created for shared network

        # Steps,
        #  1. Create a shared network
        #  2. Deploy a VM in shared network so that router is
        #     created
        #  3. Verify that corresponding port groups are created
              for guest traffic
        """

        if self.hypervisor.lower() != "vmware":
            self.skipTest("This test is intended for only vmware")

        physical_network, shared_vlan = get_free_vlan(
            self.api_client, self.zone.id)
        if shared_vlan is None:
            self.fail("Failed to get free vlan id for shared network")

        self.testdata["shared_network_offering"]["specifyVlan"] = "True"
        self.testdata["shared_network_offering"]["specifyIpRanges"] = "True"

        # Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
            self.api_client,
            self.testdata["shared_network_offering"],
            conservemode=False
        )

        # Update network offering state from disabled to enabled.
        NetworkOffering.update(
            self.shared_network_offering,
            self.api_client,
            id=self.shared_network_offering.id,
            state="enabled"
        )

        # create network using the shared network offering created
        self.testdata["shared_network"]["acltype"] = "Domain"
        self.testdata["shared_network"][
            "networkofferingid"] = self.shared_network_offering.id
        self.testdata["shared_network"][
            "physicalnetworkid"] = physical_network.id
        self.testdata["shared_network"]["vlan"] = shared_vlan

        self.network = Network.create(
            self.api_client,
            self.testdata["shared_network"],
            networkofferingid=self.shared_network_offering.id,
            zoneid=self.zone.id,
        )
        self.cleanup_networks.append(self.network)

        vm = VirtualMachine.create(
            self.api_client,
            self.testdata["virtual_machine"],
            networkids=self.network.id,
            serviceofferingid=self.service_offering.id
        )
        self.cleanup_vms.append(vm)

        routers = Router.list(self.api_client,
                    networkid=self.network.id,
                    listall=True)

        self.assertEqual(validateList(routers)[0], PASS,
                "No Router associated with the network found")

        response = verifyGuestTrafficPortGroups(self.api_client,
                                                self.config,
                                                self.zone)
        self.assertEqual(response[0], PASS, response[1])
        return
