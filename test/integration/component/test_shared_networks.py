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
#Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.integration.lib.base import (Account,
                                         Network,
                                         NetworkOffering,
                                         VirtualMachine,
                                         Project,
                                         PhysicalNetwork,
                                         Domain,
                                         StaticNATRule,
                                         FireWallRule,
                                         ServiceOffering,
                                         PublicIPAddress)
from marvin.integration.lib.utils import (cleanup_resources,
                                          xsplit)
from marvin.integration.lib.common import (get_domain,
                                           get_zone,
                                           get_template,
                                           wait_for_cleanup)
import random

import netaddr

class Services:
    """ Test shared networks """

    def __init__(self):
        self.services = {
                          "domain": {
                                   "name": "DOM",
                                   },
                         "project": {
                                    "name": "Project",
                                    "displaytext": "Test project",
                                    },
                         "account": {
                                    "email": "admin-XABU1@test.com",
                                    "firstname": "admin-XABU1",
                                    "lastname": "admin-XABU1",
                                    "username": "admin-XABU1",
                                    # Random characters are appended for unique
                                    # username
                                    "password": "password",
                                    },
                         "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 100, # in MHz
                                    "memory": 128, # In MBs
                                    },
                         "network_offering": {
                                    "name": 'MySharedOffering',
                                    "displaytext": 'MySharedOffering',
                                    "guestiptype": 'Shared',
                                    "supportedservices": 'Dhcp,Dns,UserData',
                                    "specifyVlan" : "False",
                                    "specifyIpRanges" : "False",
                                    "traffictype": 'GUEST',
                                    "serviceProviderList" : {
                                            "Dhcp": 'VirtualRouter',
                                            "Dns": 'VirtualRouter',
                                            "UserData": 'VirtualRouter'
                                        },
                                },
                         "network": {
                                  "name": "MySharedNetwork - Test",
                                  "displaytext": "MySharedNetwork",
                                  "gateway" :"",
                                  "netmask" :"255.255.255.0",
                                  "startip" :"",
                                  "endip" :"",
                                  "acltype" : "Domain",
                                  "scope":"all",
                                },
                         "network1": {
                                  "name": "MySharedNetwork - Test1",
                                  "displaytext": "MySharedNetwork1",
                                  "gateway" :"",
                                  "netmask" :"255.255.255.0",
                                  "startip" :"",
                                  "endip" :"",
                                  "acltype" : "Domain",
                                  "scope":"all",
                                },
						 "isolated_network_offering": {
                                    "name": 'Network offering-VR services',
                                    "displaytext": 'Network offering-VR services',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Firewall,Lb,UserData,StaticNat',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "serviceProviderList": {
                                            "Dhcp": 'VirtualRouter',
                                            "Dns": 'VirtualRouter',
                                            "SourceNat": 'VirtualRouter',
                                            "PortForwarding": 'VirtualRouter',
                                            "Vpn": 'VirtualRouter',
                                            "Firewall": 'VirtualRouter',
                                            "Lb": 'VirtualRouter',
                                            "UserData": 'VirtualRouter',
                                            "StaticNat": 'VirtualRouter',
                                        },
                         },
                         "isolated_network": {
                                  "name": "Isolated Network",
                                  "displaytext": "Isolated Network",
                         },
                         "fw_rule": {
                                    "startport": 22,
                                    "endport": 22,
                                    "cidr": '0.0.0.0/0',
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
                         "ostype": 'CentOS 5.3 (64-bit)',
                         # Cent OS 5.3 (64 bit)
                         "timeout": 10,
                         "mode": 'advanced'
                    }

class TestSharedNetworks(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestSharedNetworks,
                               cls
                               ).getClsTestClient().getApiClient()

        cls.services = Services().services

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )

        cls._cleanup = [
                        cls.service_offering,
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.api_client = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

        # Set the subnet number of shared networks randomly prior to execution
        # of each test case to avoid overlapping of ip addresses
        shared_network_subnet_number = random.randrange(1,254)

        self.services["network"]["gateway"] = "172.16."+str(shared_network_subnet_number)+".1"
        self.services["network"]["startip"] = "172.16."+str(shared_network_subnet_number)+".2"
        self.services["network"]["endip"] = "172.16."+str(shared_network_subnet_number)+".20"

        self.services["network1"]["gateway"] = "172.16."+str(shared_network_subnet_number + 1)+".1"
        self.services["network1"]["startip"] = "172.16."+str(shared_network_subnet_number + 1)+".2"
        self.services["network1"]["endip"] = "172.16."+str(shared_network_subnet_number + 1)+".20"

        self.cleanup = []
        self.cleanup_networks = []
        self.cleanup_accounts = []
        self.cleanup_domains = []
        self.cleanup_projects = []
        self.cleanup_vms = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created network offerings
            cleanup_resources(self.api_client, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        #below components is not a part of cleanup because to mandate the order and to cleanup network
        try:
            for vm in self.cleanup_vms:
               vm.delete(self.api_client)
        except Exception as e:
            raise Exception("Warning: Exception during virtual machines cleanup : %s" % e)

        try:
            for project in self.cleanup_projects:
                 project.delete(self.api_client)
        except Exception as e:
             raise Exception("Warning: Exception during project cleanup : %s" % e)

        try:
            for account in self.cleanup_accounts:
                account.delete(self.api_client)
        except Exception as e:
            raise Exception("Warning: Exception during account cleanup : %s" % e)

        #Wait till all resources created are cleaned up completely and then attempt to delete domains
        wait_for_cleanup(self.api_client, ["account.cleanup.interval"])

        try:
            for network in self.cleanup_networks:
                network.delete(self.api_client)
        except Exception:
            self.debug("Network %s failed to delete. Moving on" % network.id)
            pass #because domain/account deletion will get rid of the network

        try:
            for domain in self.cleanup_domains:
                domain.delete(self.api_client)
        except Exception as e:
            raise Exception("Warning: Exception during domain cleanup : %s" % e)

        return

    def getFreeVlan(self, apiclient, zoneid):
        """
        Find an unallocated VLAN outside the range allocated to the physical network.

        @note: This does not guarantee that the VLAN is available for use in
        the deployment's network gear
        @return: physical_network, shared_vlan_tag
        """
        list_physical_networks_response = PhysicalNetwork.list(
            apiclient,
            zoneid=zoneid
        )
        assert isinstance(list_physical_networks_response, list)
        assert len(list_physical_networks_response) > 0, "No physical networks found in zone %s" % zoneid

        physical_network = list_physical_networks_response[0]
        vlans = xsplit(physical_network.vlan, ['-', ','])

        assert len(vlans) > 0
        assert int(vlans[0]) < int(vlans[-1]), "VLAN range  %s was improperly split" % physical_network.vlan
        shared_ntwk_vlan = int(vlans[-1]) + random.randrange(1, 20)
        if shared_ntwk_vlan > 4095:
            shared_ntwk_vlan = int(vlans[0]) - random.randrange(1, 20)
            assert shared_ntwk_vlan > 0, "VLAN chosen %s is invalid < 0" % shared_ntwk_vlan
        self.debug("Attempting free VLAN %s for shared network creation" % shared_ntwk_vlan)
        return physical_network, shared_ntwk_vlan

    @attr(tags=["advanced", "advancedns"])
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
        #  2. listPhysicalNetworks should return at least one active physical network
        #  3. listNetworkOfferings - name=mysharedoffering , should list offering in disabled state
        #  4. listNetworkOfferings - name=mysharedoffering, should list enabled offering

        #Create an account
        self.account = Account.create(
                         self.api_client,
                         self.services["account"],
                         admin=True,
                         domainid=self.domain.id
                         )

        self.cleanup_accounts.append(self.account)

        #verify that the account got created with state enabled
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

        #Verify that there should be at least one physical network present in zone.
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

        self.services["network_offering"]["specifyVlan"] = "True"
        self.services["network_offering"]["specifyIpRanges"] = "True"

        #Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
                                                 self.api_client,
                                                 self.services["network_offering"],
                                                 conservemode=False
                                                 )

        #Verify that the network offering got created
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

        #Update network offering state from disabled to enabled.
        NetworkOffering.update(
                                self.shared_network_offering,
                                self.api_client,
                                id=self.shared_network_offering.id,
                                state="enabled"
                                )
        #Verify that the state of the network offering is updated
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
        self.debug("NetworkOffering created and enabled: %s" % self.shared_network_offering.id)

    @attr(tags=["advanced", "advancedns"])
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
        #  2. listPhysicalNetworks should return at least one active physical network
        #  3. createNetworkOffering fails - vlan should be specified in advanced zone

        #Create an account
        self.account = Account.create(
                         self.api_client,
                         self.services["account"],
                         admin=True,
                         domainid=self.domain.id
                         )

        self.cleanup_accounts.append(self.account)

        #verify that the account got created with state enabled
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

        #Verify that there should be at least one physical network present in zone.
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

        self.services["network_offering"]["specifyVlan"] = "False"
        self.services["network_offering"]["specifyIpRanges"] = "False"

        try:
            #Create Network Offering
            self.shared_network_offering = NetworkOffering.create(
                                                     self.api_client,
                                                     self.services["network_offering"],
                                                     conservemode=False
                                                     )
            self.fail("Network offering got created with vlan as False in advance mode and shared guest type, which is invalid case.")
        except Exception as e:
            self.debug("Network Offering creation failed with vlan as False in advance mode and shared guest type. Exception: %s" % 
                        e)

    @attr(tags=["advanced", "advancedns"])
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
        #  2. listPhysicalNetworks should return at least one active physical network
        #  3. createNetworkOffering fails - ip ranges should be specified when creating shared network offering


        #Create an account
        self.account = Account.create(
                         self.api_client,
                         self.services["account"],
                         admin=True,
                         domainid=self.domain.id
                         )

        self.cleanup_accounts.append(self.account)

        #verify that the account got created with state enabled
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

        #Verify that there should be at least one physical network present in zone.
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

        self.services["network_offering"]["specifyVlan"] = "True"
        self.services["network_offering"]["specifyIpRanges"] = "False"

        try:
            #Create Network Offering
            self.shared_network_offering = NetworkOffering.create(
                                                     self.api_client,
                                                     self.services["network_offering"],
                                                     conservemode=False
                                                     )
            self.fail("Network offering got created with vlan as True and ip ranges as False in advance mode and with shared guest type, which is invalid case.")
        except Exception as e:
            self.debug("Network Offering creation failed with vlan as true and ip ranges as False in advance mode and with shared guest type.\
                        Exception : %s" % e)

    @attr(tags=["advanced", "advancedns"])
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
        #    - gw = 172.16.15.1, startip = 172.16.15.2 , endip = 172.16.15.200, netmask=255.255.255.0
        #    - scope = all
        #  6. create User account - user-ASJDK
        #  7. deployVirtualMachine in this account and in admin account & within networkid = <mysharednetwork>
        #  8. delete the admin account and the user account
        # Validations,
        #  1. listAccounts name=admin-XABU1, state=enabled returns your account
        #  2. listPhysicalNetworks should return at least one active physical network
        #  3. listNetworkOfferings - name=mysharedoffering , should list offering in disabled state
        #  4. listNetworkOfferings - name=mysharedoffering, should list enabled offering
        #  5. listNetworks - name = mysharednetwork should list the successfully created network, verify the guestIp ranges and CIDR are as given in the createNetwork call
        #  6. No checks reqd
        #  7. a. listVirtualMachines should show both VMs in running state in the user account and the admin account
        #     b. VM's IPs shoud be in the range of the shared network ip ranges

        #Create admin account
        self.admin_account = Account.create(
                                    self.api_client,
                                    self.services["account"],
                                    admin=True,
                                    domainid=self.domain.id
                                    )

        self.cleanup_accounts.append(self.admin_account)

        #verify that the account got created with state enabled
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

        #Create an user account
        self.user_account = Account.create(
                                   self.api_client,
                                   self.services["account"],
                                   admin=False,
                                   domainid=self.domain.id
                                   )

        self.cleanup_accounts.append(self.user_account)

        #verify that the account got created with state enabled
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

        physical_network, shared_vlan = self.getFreeVlan(self.api_client, self.zone.id)

        self.debug("Physical network found: %s" % physical_network.id)

        self.services["network_offering"]["specifyVlan"] = "True"
        self.services["network_offering"]["specifyIpRanges"] = "True"


        #Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
                                                 self.api_client,
                                                 self.services["network_offering"],
                                                 conservemode=False
                                                 )



        #Verify that the network offering got created
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

        self.debug("Shared Network offering created: %s" % self.shared_network_offering.id)

        #Update network offering state from disabled to enabled.
        NetworkOffering.update(
                                self.shared_network_offering,
                                self.api_client,
                                id=self.shared_network_offering.id,
                                state="enabled"
                                )

        #Verify that the state of the network offering is updated
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

        #create network using the shared network offering created
        self.services["network"]["acltype"] = "Domain"
        self.services["network"]["networkofferingid"] = self.shared_network_offering.id
        self.services["network"]["physicalnetworkid"] = physical_network.id
        self.services["network"]["vlan"] = shared_vlan

        self.network = Network.create(
                         self.api_client,
                         self.services["network"],
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
            "The network is created with ip range but the flag is set to False."
            )

        self.debug("Shared Network created for scope domain: %s" % self.network.id)

        self.admin_account_virtual_machine = VirtualMachine.create(
                                                       self.api_client,
                                                       self.services["virtual_machine"],
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

        self.debug("Virtual Machine created: %s" % self.admin_account_virtual_machine.id)

        ip_range = list(netaddr.iter_iprange(unicode(self.services["network"]["startip"]), unicode(self.services["network"]["endip"])))
        if netaddr.IPAddress(unicode(vms[0].nic[0].ipaddress)) not in ip_range:
            self.fail("Virtual machine ip should be from the ip range assigned to network created.")

        self.user_account_virtual_machine = VirtualMachine.create(
                                                       self.api_client,
                                                       self.services["virtual_machine"],
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

        self.debug("Virtual Machine created: %s" % self.user_account_virtual_machine.id)

        ip_range = list(netaddr.iter_iprange(unicode(self.services["network"]["startip"]), unicode(self.services["network"]["endip"])))
        if netaddr.IPAddress(unicode(vms[0].nic[0].ipaddress)) not in ip_range:
            self.fail("Virtual machine ip should be from the ip range assigned to network created.")

    @attr(tags=["advanced", "advancedns"])
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
        #    - gw = 172.16.15.1, startip = 172.16.15.2 , endip = 172.16.15.200, netmask=255.255.255.0
        #    - scope = account, account = user-SOPJD, domain = ROOT
        #  6. deployVirtualMachine in this account and in admin account & within networkid = <mysharednetwork>
        #  7. delete the admin account and the user account
        # Validations,
        #  1. listAccounts name=admin-XABU1 and user-SOPJD, state=enabled returns your account
        #  2. listPhysicalNetworks should return at least one active physical network
        #  3. listNetworkOfferings - name=mysharedoffering , should list offering in disabled state
        #  4. listNetworkOfferings - name=mysharedoffering, should list enabled offering
        #  5. listNetworks - name = mysharednetwork should list the successfully created network, verify the guestIp ranges and CIDR are as given in the createNetwork call
        #  6. VM deployed in admin account should FAIL to deploy
        #     VM should be deployed in user account only
        #    verify VM's IP is within shared network range

        #Create admin account
        self.admin_account = Account.create(
                                     self.api_client,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )

        self.cleanup_accounts.append(self.admin_account)

        #verify that the account got created with state enabled
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

        #Create an user account
        self.user_account = Account.create(
                         self.api_client,
                         self.services["account"],
                         admin=False,
                         domainid=self.domain.id
                         )

        self.cleanup_accounts.append(self.user_account)

        #verify that the account got created with state enabled
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

        physical_network, shared_vlan = self.getFreeVlan(self.api_client, self.zone.id)

        self.debug("Physical Network found: %s" % physical_network.id)

        self.services["network_offering"]["specifyVlan"] = "True"
        self.services["network_offering"]["specifyIpRanges"] = "True"

        #Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
                                                 self.api_client,
                                                 self.services["network_offering"],
                                                 conservemode=False
                                                 )

        #Verify that the network offering got created
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

        self.debug("Shared Network Offering created: %s" % self.shared_network_offering.id)

        #Update network offering state from disabled to enabled.
        NetworkOffering.update(
                                self.shared_network_offering,
                                self.api_client,
                                id=self.shared_network_offering.id,
                                state="enabled"
                                )
        #Verify that the state of the network offering is updated
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

        #create network using the shared network offering created
        self.services["network"]["acltype"] = "Account"
        self.services["network"]["networkofferingid"] = self.shared_network_offering.id
        self.services["network"]["physicalnetworkid"] = physical_network.id
        self.services["network"]["vlan"] = shared_vlan

        self.network = Network.create(
                         self.api_client,
                         self.services["network"],
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
            "The network is created with ip range but the flag is set to False."
            )

        self.debug("Network created: %s" % self.network.id)

        try:
            self.admin_account_virtual_machine = VirtualMachine.create(
                                                           self.api_client,
                                                           self.services["virtual_machine"],
                                                           accountid=self.admin_account.name,
                                                           domainid=self.admin_account.domainid,
                                                           networkids=self.network.id,
                                                           serviceofferingid=self.service_offering.id
                                                           )
            self.fail("Virtual Machine got created in admin account with network created but the network used is of scope account and for user account.")
        except Exception as e:
            self.debug("Virtual Machine creation failed as network used have scoped only for user account. Exception: %s" % e)

        self.user_account_virtual_machine = VirtualMachine.create(
                                                       self.api_client,
                                                       self.services["virtual_machine"],
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

        ip_range = list(netaddr.iter_iprange(unicode(self.services["network"]["startip"]), unicode(self.services["network"]["endip"])))
        if netaddr.IPAddress(unicode(vms[0].nic[0].ipaddress)) not in ip_range:
            self.fail("Virtual machine ip should be from the ip range assigned to network created.")

    @attr(tags=["advanced", "advancedns"])
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
        #    - gw = 172.16.15.1, startip = 172.16.15.2 , endip = 172.16.15.200, netmask=255.255.255.0
        #    - scope = domain, domain = DOM
        #  6. deployVirtualMachine in this admin, domainadmin and user account & within networkid = <mysharednetwork>
        #  7. delete all the accounts
        # Validations,
        #  1. listAccounts state=enabled returns your accounts, listDomains - DOM should be created
        #  2. listPhysicalNetworks should return at least one active physical network
        #  3. listNetworkOfferings - name=mysharedoffering , should list offering in disabled state
        #  4. listNetworkOfferings - name=mysharedoffering, should list enabled offering
        #  5. listNetworks - name = mysharednetwork should list the successfully created network, verify the guestIp ranges and CIDR are as given in the createNetwork call
        #  6. VM should NOT be deployed in admin account
        #     VM should be deployed in user account and domain admin account
        #     verify VM's IP are within shared network range

        #Create admin account
        self.admin_account = Account.create(
                                     self.api_client,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )

        self.cleanup_accounts.append(self.admin_account)

        #verify that the account got created with state enabled
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

        #create domain
        self.dom_domain = Domain.create(
                                self.api_client,
                                self.services["domain"],
                                )

        self.cleanup_domains.append(self.dom_domain)

        #verify that the account got created with state enabled
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

        #Create admin account
        self.domain_admin_account = Account.create(
                                     self.api_client,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.dom_domain.id
                                     )

        self.cleanup_accounts.append(self.domain_admin_account)

        #verify that the account got created with state enabled
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

        self.debug("Domain admin account created: %s" % self.domain_admin_account.id)

        #Create an user account
        self.domain_user_account = Account.create(
                         self.api_client,
                         self.services["account"],
                         admin=False,
                         domainid=self.dom_domain.id
                         )

        self.cleanup_accounts.append(self.domain_user_account)

        #verify that the account got created with state enabled
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

        self.debug("Domain user account created: %s" % self.domain_user_account.id)

        physical_network, shared_vlan = self.getFreeVlan(self.api_client, self.zone.id)

        self.debug("Physical Network found: %s" % physical_network.id)

        self.services["network_offering"]["specifyVlan"] = "True"
        self.services["network_offering"]["specifyIpRanges"] = "True"

        #Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
                                                 self.api_client,
                                                 self.services["network_offering"],
                                                 conservemode=False
                                                 )


        #Verify that the network offering got created
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

        self.debug("Shared Network Offering created: %s" % self.shared_network_offering.id)

        #Update network offering state from disabled to enabled.
        NetworkOffering.update(
                                self.shared_network_offering,
                                self.api_client,
                                id=self.shared_network_offering.id,
                                state="enabled"
                                )

        #Verify that the state of the network offering is updated
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

        #create network using the shared network offering created
        self.services["network"]["acltype"] = "domain"
        self.services["network"]["networkofferingid"] = self.shared_network_offering.id
        self.services["network"]["physicalnetworkid"] = physical_network.id
        self.services["network"]["vlan"] = shared_vlan

        self.network = Network.create(
                         self.api_client,
                         self.services["network"],
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
            "The network is created with ip range but the flag is set to False."
            )

        self.debug("Shared Network created: %s" % self.network.id)

        try:
            self.admin_account_virtual_machine = VirtualMachine.create(
                                                           self.api_client,
                                                           self.services["virtual_machine"],
                                                           accountid=self.admin_account.name,
                                                           domainid=self.admin_account.domainid,
                                                           networkids=self.network.id,
                                                           serviceofferingid=self.service_offering.id
                                                           )
            self.fail("Virtual Machine got created in admin account with network specified but the network used is of scope domain and admin account is not part of this domain.")
        except Exception as e:
            self.debug("Virtual Machine creation failed as network used have scoped only for DOM domain. Exception: %s" % e)

        self.domain_user_account_virtual_machine = VirtualMachine.create(
                                                       self.api_client,
                                                       self.services["virtual_machine"],
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

        ip_range = list(netaddr.iter_iprange(unicode(self.services["network"]["startip"]), unicode(self.services["network"]["endip"])))
        if netaddr.IPAddress(unicode(vms[0].nic[0].ipaddress)) not in ip_range:
            self.fail("Virtual machine ip should be from the ip range assigned to network created.")

        self.domain_admin_account_virtual_machine = VirtualMachine.create(
                                                       self.api_client,
                                                       self.services["virtual_machine"],
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

        ip_range = list(netaddr.iter_iprange(unicode(self.services["network"]["startip"]), unicode(self.services["network"]["endip"])))
        if netaddr.IPAddress(unicode(vms[0].nic[0].ipaddress)) not in ip_range:
            self.fail("Virtual machine ip should be from the ip range assigned to network created.")

    @attr(tags=["advanced", "advancedns"])
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
        #    - gw = 172.16.15.1, startip = 172.16.15.2 , endip = 172.16.15.200, netmask=255.255.255.0
        #    - scope = project, project =  proj-SLDJK
        #  6. deployVirtualMachine in admin, project and user account & within networkid = <mysharednetwork>
        #  7. delete all the accounts
        # Validations,
        #  1. listAccounts state=enabled returns your accounts, listDomains - DOM should be created
        #  2. listPhysicalNetworks should return at least one active physical network
        #  3. listNetworkOfferings - name=mysharedoffering , should list offering in disabled state
        #  4. listNetworkOfferings - name=mysharedoffering, should list enabled offering
        #  5. listNetworks - name = mysharednetwork should list the successfully created network, verify the guestIp ranges and CIDR are as given in the createNetwork call
        #  6. VM should NOT be deployed in admin account and user account
        #     VM should be deployed in project account only
        #     verify VM's IP are within shared network range

        #Create admin account
        self.admin_account = Account.create(
                                     self.api_client,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )

        self.cleanup_accounts.append(self.admin_account)

        #verify that the account got created with state enabled
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

        self.services["project"]["name"] = "proj-SADJKS"
        self.services["project"]["displaytext"] = "proj-SADJKS"

        self.project1 = Project.create(
                                 self.api_client,
                                 self.services["project"],
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

        self.services["project"]["name"] = "proj-SLDJK"
        self.services["project"]["displaytext"] = "proj-SLDJK"

        self.project2 = Project.create(
                                 self.api_client,
                                 self.services["project"],
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

        physical_network, shared_vlan = self.getFreeVlan(self.api_client, self.zone.id)

        self.debug("Physical Network found: %s" % physical_network.id)

        self.services["network_offering"]["specifyVlan"] = "True"
        self.services["network_offering"]["specifyIpRanges"] = "True"


        #Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
                                                 self.api_client,
                                                 self.services["network_offering"],
                                                 conservemode=False
                                                 )


        #Verify that the network offering got created
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

        #Update network offering state from disabled to enabled.
        NetworkOffering.update(
                                self.shared_network_offering,
                                self.api_client,
                                id=self.shared_network_offering.id,
                                state="enabled"
                                )

        #Verify that the state of the network offering is updated
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

        self.debug("Shared Network found: %s" % self.shared_network_offering.id)

        #create network using the shared network offering created
        self.services["network"]["acltype"] = "account"
        self.services["network"]["networkofferingid"] = self.shared_network_offering.id
        self.services["network"]["physicalnetworkid"] = physical_network.id
        self.services["network"]["vlan"] = shared_vlan

        self.network = Network.create(
                         self.api_client,
                         self.services["network"],
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
            "The network is created with ip range but the flag is set to False."
            )

        self.debug("Shared Network created: %s" % self.network.id)

        try:
            self.project2_admin_account_virtual_machine = VirtualMachine.create(
                                                           self.api_client,
                                                           self.services["virtual_machine"],
                                                           accountid=self.admin_account.name,
                                                           domainid=self.admin_account.domainid,
                                                           networkids=self.network.id,
                                                           projectid=self.project2.id,
                                                           serviceofferingid=self.service_offering.id
                                                           )
            self.fail("Virtual Machine got created in admin account with network specified but the network used is of scope project and the project2 is not assigned for the network.")
        except Exception as e:
            self.debug("Virtual Machine creation failed as network used have scoped only for project project1. Exception: %s" % e)

        self.project1_admin_account_virtual_machine = VirtualMachine.create(
                                                       self.api_client,
                                                       self.services["virtual_machine"],
                                                       accountid=self.admin_account.name,
                                                       domainid=self.admin_account.domainid,
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

        ip_range = list(netaddr.iter_iprange(unicode(self.services["network"]["startip"]), unicode(self.services["network"]["endip"])))
        if netaddr.IPAddress(unicode(vms[0].nic[0].ipaddress)) not in ip_range:
            self.fail("Virtual machine ip should be from the ip range assigned to network created.")

    @unittest.skip("skipped - This is a redundant case and also this is causing issue for rest fo the cases ")
    @attr(tags=["advanced", "advancedns"])
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
        #    - gw = 172.16.15.1, startip = 172.16.15.2 , endip = 172.16.15.200, netmask=255.255.255.0
        #    - scope = all
        #  6. delete admin account
        # Validations,
        #  1. listAccounts state=enabled returns your account
        #  2. listNetworkOfferings - name=mysharedoffering , should list offering in disabled state
        #  3. listNetworkOfferings - name=mysharedoffering, should list enabled offering
        #  4. listPhysicalNetworks should return at least one active physical network
        #  5. network creation should FAIL since VLAN is used for guest networks

        #Create admin account
        self.admin_account = Account.create(
                                     self.api_client,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )

        self.cleanup_accounts.append(self.admin_account)

        #verify that the account got created with state enabled
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

        physical_network, shared_vlan = self.getFreeVlan(self.api_client, self.zone.id)

        self.services["network_offering"]["specifyVlan"] = "True"
        self.services["network_offering"]["specifyIpRanges"] = "True"

        #Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
                                                 self.api_client,
                                                 self.services["network_offering"],
                                                 conservemode=False
                                                 )


        #Verify that the network offering got created
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

        self.debug("Shared Network Offering created: %s" % self.shared_network_offering.id)

        #Update network offering state from disabled to enabled.
        NetworkOffering.update(
                                self.shared_network_offering,
                                self.api_client,
                                id=self.shared_network_offering.id,
                                state="enabled"
                                )

        #Verify that the state of the network offering is updated
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

        #create network using the shared network offering created
        self.services["network"]["vlan"] = str.split(str(physical_network.vlan), "-")[0]
        self.services["network"]["acltype"] = "domain"
        self.services["network"]["networkofferingid"] = self.shared_network_offering.id
        self.services["network"]["physicalnetworkid"] = physical_network.id
        self.services["network"]["vlan"] = shared_vlan

        try:
            self.network = Network.create(
                         self.api_client,
                         self.services["network"],
                         networkofferingid=self.shared_network_offering.id,
                         zoneid=self.zone.id,
                         )
            self.fail("Network created with used vlan %s id, which is invalid" % shared_vlan)
        except Exception as e:
            self.debug("Network creation failed because the valn id being used by another network. Exception: %s" % e)


    @attr(tags=["advanced", "advancedns"])
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
        #    - gw = 172.16.15.1, startip = 172.16.15.2 , endip = 172.16.15.200, netmask=255.255.255.0
        #    - scope = all
        #  6. createNetwork again with same VLAN  but different IP ranges and gw
        #  7. delete admin account
        # Validations,
        #  1. listAccounts state=enabled returns your account
        #  2. listNetworkOfferings - name=mysharedoffering , should list offering in disabled state
        #  3. listNetworkOfferings - name=mysharedoffering, should list enabled offering
        #  4. listPhysicalNetworks should return at least one active physical network
        #  5. network creation shoud PASS
        #  6. network creation should FAIL since VLAN is already used by previously created network

        #Create admin account
        self.admin_account = Account.create(
                                     self.api_client,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )
        self.cleanup_accounts.append(self.admin_account)

        #verify that the account got created with state enabled
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

        physical_network, shared_ntwk_vlan = self.getFreeVlan(self.api_client, self.zone.id)

        self.debug("Physical Network found: %s" % physical_network.id)

        self.services["network_offering"]["specifyVlan"] = "True"
        self.services["network_offering"]["specifyIpRanges"] = "True"

        #Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
                                                 self.api_client,
                                                 self.services["network_offering"],
                                                 conservemode=False
                                                 )


        #Verify that the network offering got created
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

        self.debug("Shared Network Offering created: %s" % self.shared_network_offering.id)

        #Update network offering state from disabled to enabled.
        NetworkOffering.update(
                                self.shared_network_offering,
                                self.api_client,
                                id=self.shared_network_offering.id,
                                state="enabled"
                                )

        #Verify that the state of the network offering is updated
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

        #create network using the shared network offering created
        self.services["network"]["acltype"] = "Domain"
        self.services["network"]["networkofferingid"] = self.shared_network_offering.id
        self.services["network"]["physicalnetworkid"] = physical_network.id
        self.services["network"]["vlan"] = shared_ntwk_vlan
        self.debug("Creating a shared network in non-cloudstack VLAN %s" % shared_ntwk_vlan)
        self.network = Network.create(
                         self.api_client,
                         self.services["network"],
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
            "The network is created with ip range but the flag is set to False."
            )

        self.debug("Network created: %s" % self.network.id)

        self.services["network1"]["vlan"] = self.services["network"]["vlan"]
        self.services["network1"]["acltype"] = "domain"
        self.services["network1"]["networkofferingid"] = self.shared_network_offering.id
        self.services["network1"]["physicalnetworkid"] = physical_network.id

        try:
            self.network1 = Network.create(
                         self.api_client,
                         self.services["network"],
                         networkofferingid=self.shared_network_offering.id,
                         zoneid=self.zone.id,
                         )
            self.cleanup_networks.append(self.network1)
            self.fail("Network got created with used vlan id, which is invalid")
        except Exception as e:
            self.debug("Network creation failed because the valn id being used by another network. Exception: %s" % e)

    @attr(tags=["advanced", "advancedns"])
    def test_deployVM_multipleSharedNetwork(self):
        """ Test Vm deployment with multiple shared networks """

        # Steps,
        #  0. create a user account
        #  1. Create two shared Networks (scope=ALL, different IP ranges)
        #  2. deployVirtualMachine in both the above networkids within the user account
        #  3. delete the user account
        # Validations,
        #  1. shared networks should be created successfully
        #  2. a. VM should deploy successfully
        #     b. VM should be deployed in both networks and have IP in both the networks

        #Create admin account
        self.admin_account = Account.create(
	                     self.api_client,
					     self.services["account"],
					     admin=True,
					     domainid=self.domain.id
					   )

        self.cleanup_accounts.append(self.admin_account)

        #verify that the account got created with state enabled
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

        physical_network, shared_vlan = self.getFreeVlan(self.api_client, self.zone.id)

        self.debug("Physical Network found: %s" % physical_network.id)

        self.services["network_offering"]["specifyVlan"] = "True"
        self.services["network_offering"]["specifyIpRanges"] = "True"

        #Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
                                                 self.api_client,
                                                 self.services["network_offering"],
                                                 conservemode=False
                                                 )


        #Verify that the network offering got created
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

        self.debug("Shared Network offering created: %s" % self.shared_network_offering.id)

        #Update network offering state from disabled to enabled.
        NetworkOffering.update(
                                self.shared_network_offering,
                                self.api_client,
                                id=self.shared_network_offering.id,
                                state="enabled"
                                )

        #Verify that the state of the network offering is updated
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

        #create network using the shared network offering created
        self.services["network"]["acltype"] = "domain"
        self.services["network"]["networkofferingid"] = self.shared_network_offering.id
        self.services["network"]["physicalnetworkid"] = physical_network.id
        self.services["network"]["vlan"] = shared_vlan

        self.network = Network.create(
                         self.api_client,
                         self.services["network"],
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
            "The network is created with ip range but the flag is set to False."
            )

        self.debug("Shared Network created: %s" % self.network.id)

        self.services["network1"]["acltype"] = "domain"
        self.services["network1"]["networkofferingid"] = self.shared_network_offering.id
        self.services["network1"]["physicalnetworkid"] = physical_network.id
        self.services["network1"]["vlan"] = self.getFreeVlan(self.api_client, self.zone.id)[1] #vlan id is second return value of function

        self.network1 = Network.create(
                         self.api_client,
                         self.services["network1"],
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
            "The network is created with ip range but the flag is set to False."
            )

        self.debug("Network created: %s" % self.network1.id)

        self.network_admin_account_virtual_machine = VirtualMachine.create(
                                                       self.api_client,
                                                       self.services["virtual_machine"],
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

        self.debug("Virtual Machine created: %s" % self.network_admin_account_virtual_machine.id)

        self.assertTrue(self.network_admin_account_virtual_machine.nic[0].ipaddress is not None, "ip should be assigned to running virtual machine")

        self.network1_admin_account_virtual_machine = VirtualMachine.create(
                                                       self.api_client,
                                                       self.services["virtual_machine"],
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
        self.debug("Virtual Machine created: %s" % self.network1_admin_account_virtual_machine.id)

        self.assertTrue(self.network1_admin_account_virtual_machine.nic[0].ipaddress is not None, "ip should be assigned to running virtual machine")

    @attr(tags=["advanced", "advancedns"])
    def test_deployVM_isolatedAndShared(self):
        """ Test VM deployment in shared and isolated networks """

        # Steps,
        #  0. create a user account
        #  1. Create one shared Network (scope=ALL, different IP ranges)
        #  2. Create one Isolated Network
        #  3. deployVirtualMachine in both the above networkids within the user account
        #  4. apply FW rule and enable PF for port 22 for guest VM on isolated network
        #  5. delete the user account
        # Validations,
        #  1. shared network should be created successfully
        #  2. isolated network should be created successfully
        #  3.
        #    a. VM should deploy successfully
        #    b. VM should be deployed in both networks and have IP in both the networks
        #  4. FW and PF should apply successfully, ssh into the VM should work over isolated network

        #Create admin account
        self.admin_account = Account.create(
                                     self.api_client,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )

        self.cleanup_accounts.append(self.admin_account)

        #verify that the account got created with state enabled
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

        self.services["network_offering"]["specifyVlan"] = "True"
        self.services["network_offering"]["specifyIpRanges"] = "True"

        #Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
                                                 self.api_client,
                                                 self.services["network_offering"],
                                                 conservemode=False
                                                 )


        #Verify that the network offering got created
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

        self.debug("Shared Network offering created: %s" % self.shared_network_offering.id)

        #Update network offering state from disabled to enabled.
        NetworkOffering.update(
                                self.shared_network_offering,
                                self.api_client,
                                id=self.shared_network_offering.id,
                                state="enabled"
                                )

        #Verify that the state of the network offering is updated
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
                                          self.services["isolated_network_offering"],
                                          conservemode=False
                                          )


        #Update network offering state from disabled to enabled.
        NetworkOffering.update(
                                self.isolated_network_offering,
                                self.api_client,
                                id=self.isolated_network_offering.id,
                                state="enabled"
                                )

        #Verify that the state of the network offering is updated
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
            "The isolated network offering state should get updated to Enabled."
            )

        self.debug("Isolated Network Offering created: %s" % self.isolated_network_offering.id)
        physical_network, shared_vlan = self.getFreeVlan(self.api_client, self.zone.id)

        #create network using the shared network offering created
        self.services["network"]["acltype"] = "domain"
        self.services["network"]["networkofferingid"] = self.shared_network_offering.id
        self.services["network"]["physicalnetworkid"] = physical_network.id
        self.services["network"]["vlan"] = shared_vlan

        self.shared_network = Network.create(
                         self.api_client,
                         self.services["network"],
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
            "The network is created with ip range but the flag is set to False."
            )

        self.debug("Shared Network created: %s" % self.shared_network.id)

        self.isolated_network = Network.create(
                         self.api_client,
                         self.services["isolated_network"],
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

        self.shared_network_admin_account_virtual_machine =\
                                    VirtualMachine.create(
                                        self.api_client,
                                        self.services["virtual_machine"],
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
        self.debug("Virtual Machine created: %s" % self.shared_network_admin_account_virtual_machine.id)

        self.assertTrue(self.shared_network_admin_account_virtual_machine.nic[0].ipaddress is not None,
            "ip should be assigned to running virtual machine")

        self.isolated_network_admin_account_virtual_machine = \
                                    VirtualMachine.create(
                                           self.api_client,
                                           self.services["virtual_machine"],
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

        self.debug("Virtual Machine created: %s" % self.isolated_network_admin_account_virtual_machine.id)

        self.assertTrue(self.isolated_network_admin_account_virtual_machine.nic[0].ipaddress is not None, "ip should be assigned to running virtual machine")

        self.debug("Associating public IP for account: %s" % self.admin_account.name)
        self.public_ip = PublicIPAddress.create(
                                    self.api_client,
                                    accountid=self.admin_account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.admin_account.domainid,
                                    networkid=self.isolated_network.id
                                    )

        self.debug("Associated %s with network %s" % (self.public_ip.ipaddress.ipaddress, self.isolated_network.id))
        self.debug("Creating PF rule for IP address: %s" % self.public_ip.ipaddress.ipaddress)

        public_ip = self.public_ip.ipaddress

        # Enable Static NAT for VM
        StaticNATRule.enable(
                             self.api_client,
                             public_ip.id,
                             self.isolated_network_admin_account_virtual_machine.id
                            )

        self.debug("Enabled static NAT for public IP ID: %s" % public_ip.id)
        #Create Firewall rule on source NAT
        fw_rule = FireWallRule.create(
                            self.api_client,
                            ipaddressid=self.public_ip.ipaddress.id,
                            protocol='TCP',
                            cidrlist=[self.services["fw_rule"]["cidr"]],
                            startport=self.services["fw_rule"]["startport"],
                            endport=self.services["fw_rule"]["endport"]
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
            self.debug("SSH into VM: %s" % self.isolated_network_admin_account_virtual_machine.id)
            ssh = self.isolated_network_admin_account_virtual_machine.get_ssh_client(ipaddress=self.public_ip.ipaddress.ipaddress)
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % (self.isolated_network_admin_account_virtual_machine.ipaddress, e))

    @attr(tags=["advanced", "advancedns"])
    def test_networkWithsubdomainaccessTrue(self):
        """ Test Shared Network with subdomainaccess=True """

        # Steps,
        #  1. create Network using shared network offering for scope=Account and subdomainaccess=true.
        # Validations,
        #  (Expected) API should fail saying that subdomainaccess cannot be given when scope is Account

        #Create admin account
        self.admin_account = Account.create(
                                     self.api_client,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )

        self.cleanup_accounts.append(self.admin_account)

        #verify that the account got created with state enabled
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

        physical_network, shared_vlan = self.getFreeVlan(self.api_client, self.zone.id)

        self.debug("Physical Network found: %s" % physical_network.id)

        self.services["network_offering"]["specifyVlan"] = "True"
        self.services["network_offering"]["specifyIpRanges"] = "True"

        #Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
                                                 self.api_client,
                                                 self.services["network_offering"],
                                                 conservemode=False
                                                 )


        #Verify that the network offering got created
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

        self.debug("Shared Network Offering created: %s" % self.shared_network_offering.id)

        #Update network offering state from disabled to enabled.
        NetworkOffering.update(
                                self.shared_network_offering,
                                self.api_client,
                                id=self.shared_network_offering.id,
                                state="enabled"
                                )
        #Verify that the state of the network offering is updated
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

        #create network using the shared network offering created
        self.services["network"]["acltype"] = "Account"
        self.services["network"]["networkofferingid"] = self.shared_network_offering.id
        self.services["network"]["physicalnetworkid"] = physical_network.id
        self.services["network"]["vlan"] = shared_vlan
        self.services["network"]["subdomainaccess"] = "True"

        try:
            self.network = Network.create(
                             self.api_client,
                             self.services["network"],
                             accountid=self.admin_account.name,
                             domainid=self.admin_account.domainid,
                             networkofferingid=self.shared_network_offering.id,
                             zoneid=self.zone.id
                             )
            self.fail("Network creation should fail.")
        except:
            self.debug("Network creation failed because subdomainaccess parameter was passed when scope was account.")

    @attr(tags=["advanced", "advancedns"])
    def test_networkWithsubdomainaccessFalse(self):
        """ Test shared Network with subdomainaccess=False """

        # Steps,
        #  1. create Network using shared network offering for scope=Account and subdomainaccess=false
        # Validations,
        #  (Expected) API should fail saying that subdomainaccess cannot be given when scope is Account

        #Create admin account
        self.admin_account = Account.create(
                                     self.api_client,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )

        self.cleanup_accounts.append(self.admin_account)

        #verify that the account got created with state enabled
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

        physical_network, shared_vlan = self.getFreeVlan(self.api_client, self.zone.id)

        self.debug("Physical Network found: %s" % physical_network.id)

        self.services["network_offering"]["specifyVlan"] = "True"
        self.services["network_offering"]["specifyIpRanges"] = "True"

        #Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
                                                 self.api_client,
                                                 self.services["network_offering"],
                                                 conservemode=False
                                                 )

        #Verify that the network offering got created
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

        self.debug("Shared Network Offering created: %s" % self.shared_network_offering.id)

        #Update network offering state from disabled to enabled.
        NetworkOffering.update(
                                self.shared_network_offering,
                                self.api_client,
                                id=self.shared_network_offering.id,
                                state="enabled"
                                )
        #Verify that the state of the network offering is updated
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

        #create network using the shared network offering created
        self.services["network"]["acltype"] = "Account"
        self.services["network"]["networkofferingid"] = self.shared_network_offering.id
        self.services["network"]["physicalnetworkid"] = physical_network.id
        self.services["network"]["vlan"] = shared_vlan
        self.services["network"]["subdomainaccess"] = "False"

        try:
            self.network = Network.create(
                             self.api_client,
                             self.services["network"],
                             accountid=self.admin_account.name,
                             domainid=self.admin_account.domainid,
                             networkofferingid=self.shared_network_offering.id,
                             zoneid=self.zone.id
                             )
            self.fail("Network creation should fail.")
        except:
            self.debug("Network creation failed because subdomainaccess parameter was passed when scope was account.")
