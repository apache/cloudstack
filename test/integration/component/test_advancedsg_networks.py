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

""" P1 tests for networks in advanced zone with security groups
"""
#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
import netaddr
from nose.plugins.attrib import attr

class Services:
    """ Test networks in advanced zone with security groups"""

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
                                    "password": "fr3sca",
                                    },
                         "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 100, # in MHz
                                    "memory": 128, # In MBs
                                    },
                         "shared_network_offering_sg": {
                                    "name": 'MySharedOffering-sg',
                                    "displaytext": 'MySharedOffering-sg',
                                    "guestiptype": 'Shared',
                                    "supportedservices": 'Dhcp,Dns,UserData,SecurityGroup',
                                    "specifyVlan" : "False",
                                    "specifyIpRanges" : "False",
                                    "traffictype": 'GUEST',
                                    "serviceProviderList" : {
                                            "Dhcp": 'VirtualRouter',
                                            "Dns": 'VirtualRouter',
                                            "UserData": 'VirtualRouter',
                                            "SecurityGroup": 'SecurityGroupProvider'
                                        },
                                },
                         "shared_network_offering": {
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
                         "shared_network_sg": {
                                  "name": "MyIsolatedNetwork - Test",
                                  "displaytext": "MyIsolatedNetwork",
                                  "networkofferingid":"1",
                                  "vlan" :1200,
                                  "gateway" :"172.16.15.1",
                                  "netmask" :"255.255.255.0",
                                  "startip" :"172.16.15.2",
                                  "endip" :"172.16.15.20",
                                  "acltype" : "Domain",
                                  "scope":"all",
                                },
                         "shared_network": {
                                  "name": "MySharedNetwork - Test",
                                  "displaytext": "MySharedNetwork",
                                  "vlan" :1201,
                                  "gateway" :"172.16.15.1",
                                  "netmask" :"255.255.255.0",
                                  "startip" :"172.16.15.21",
                                  "endip" :"172.16.15.41",
                                  "acltype" : "Domain",
                                  "scope":"all",
                                },
                         "isolated_network_offering": {
                                    "name": 'Network offering-DA services',
                                    "displaytext": 'Network offering-DA services',
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
                         "sleep": 90,
                         "timeout": 10,
                         "mode": 'advanced',
                         "securitygroupenabled": 'true'
                    }
        
class TestNetworksInAdvancedSG(cloudstackTestCase):
    
    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestNetworksInAdvancedSG,
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
        
        try:
            for domain in self.cleanup_domains:
                domain.delete(self.api_client)
        except Exception as e:
            raise Exception("Warning: Exception during domain cleanup : %s" % e)
            
        #Wait till all resources created are cleaned up completely and then attempt to delete Network
        time.sleep(self.services["sleep"])
        
        try:
            for network in self.cleanup_networks:
                network.delete(self.api_client)
        except Exception as e:
            raise Exception("Warning: Exception during network cleanup : %s" % e)
        return

    @attr(tags = ["advancedsg"])
    def test_createIsolatedNetwork(self):
        """ Test Isolated Network """
        
        # Steps,
        #  1. create an Admin Account - admin-XABU1
        #  2. listPhysicalNetworks in available zone
        #  3. createNetworkOffering: 
        #  4. Enable network offering - updateNetworkOffering - state=Enabled
        #  5. createNetwork
        # Validations,
        #  1. listAccounts name=admin-XABU1, state=enabled returns your account
        #  2. listPhysicalNetworks should return at least one active physical network
        #  4. listNetworkOfferings - name=myisolatedoffering, should list enabled offering
        #  5. network creation should FAIL since isolated network is not supported in advanced zone with security groups.
        
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
        
        #Create Network Offering
        self.isolated_network_offering = NetworkOffering.create(
                                                 self.api_client,
                                                 self.services["isolated_network_offering"],
                                                 conservemode=False
                                                 )
        
        self.cleanup.append(self.isolated_network_offering)
        
        #Verify that the network offering got created 
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
            "Disabled",
            "The network offering created should be bydefault disabled."
            )
        
        self.debug("Isolated Network offering created: %s" % self.isolated_network_offering.id)
        
        #Update network offering state from disabled to enabled.
        network_offering_update_response = NetworkOffering.update(
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
            "The network offering state should get updated to Enabled."
            )
        
        #create network using the isolated network offering created
        try:
            self.isolated_network = Network.create(
                         self.api_client,
                         self.services["isolated_network"],
                         networkofferingid=self.isolated_network_offering.id,
                         zoneid=self.zone.id,
                         )
            self.cleanup_networks.append(self.isolated_network) 
            self.fail("Create isolated network is invalid in advanced zone with security groups.")
        except Exception as e:
            self.debug("Network creation failed because create isolated network is invalid in advanced zone with security groups.")

    @attr(tags = ["advancedsg"])
    def test_createSharedNetwork_withoutSG(self):
        """ Test Shared Network with without SecurityProvider """
        
        # Steps,
        #  1. create an Admin account
        #  2. create a shared NetworkOffering
        #  3. enable the network offering
        #  4. listPhysicalNetworks 
        #  5. createNetwork
        # Validations,
        #  1. listAccounts state=enabled returns your account
        #  2. listNetworkOfferings - name=mysharedoffering , should list offering in disabled state
        #  3. listNetworkOfferings - name=mysharedoffering, should list enabled offering
        #  4. listPhysicalNetworks should return at least one active physical network
        #  5. network creation should FAIL since there is no SecurityProvide in the network offering
        
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
        
        self.services["shared_network_offering"]["specifyVlan"] = "True"
        self.services["shared_network_offering"]["specifyIpRanges"] = "True"
        
        #Create Network Offering
        self.shared_network_offering = NetworkOffering.create(
                                                 self.api_client,
                                                 self.services["shared_network_offering"],
                                                 conservemode=False
                                                 )
        
        self.cleanup.append(self.shared_network_offering)
        
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
        network_offering_update_response = NetworkOffering.update(
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
        self.services["shared_network"]["acltype"] = "domain"
        self.services["shared_network"]["networkofferingid"] = self.shared_network_offering.id
        self.services["shared_network"]["physicalnetworkid"] = physical_network.id
        
        try:
            self.shared_network = Network.create(
                         self.api_client,
                         self.services["shared_network"],
                         networkofferingid=self.shared_network_offering.id,
                         zoneid=self.zone.id
                         )
            self.cleanup_networks.append(self.shared_network)
            self.fail("Network created without SecurityProvider , which is invalid")
        except Exception as e:
            self.debug("Network creation failed because there is no SecurityProvider in the network offering.")
    
    @attr(tags = ["advancedsg"])
    def test_deployVM_SharedwithSG(self):
        """ Test VM deployment in shared networks with SecurityProvider """
        
        # Steps,
        #  0. create a user account
        #  1. Create one shared Network (scope=ALL, different IP ranges)
        #  2. deployVirtualMachine in the above networkid within the user account
        #  3. delete the user account
        # Validations,
        #  1. shared network should be created successfully
        #  2. VM should deploy successfully
        
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
                                        liistall=True
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
        
        self.services["shared_network_offering_sg"]["specifyVlan"] = "True"
        self.services["shared_network_offering_sg"]["specifyIpRanges"] = "True"
        
        #Create Network Offering
        self.shared_network_offering_sg = NetworkOffering.create(
                                                 self.api_client,
                                                 self.services["shared_network_offering_sg"],
                                                 conservemode=False
                                                 )
        
        self.cleanup.append(self.shared_network_offering_sg)
        
        #Verify that the network offering got created 
        list_network_offerings_response = NetworkOffering.list(
                                                         self.api_client,
                                                         id=self.shared_network_offering_sg.id
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
        
        self.debug("Shared Network offering created: %s" % self.shared_network_offering_sg.id)
        
        #Update network offering state from disabled to enabled.
        network_offering_update_response = NetworkOffering.update(
                                                           self.shared_network_offering_sg,
                                                           self.api_client,
                                                           id=self.shared_network_offering_sg.id,
                                                           state="enabled"
                                                           )
        
        #Verify that the state of the network offering is updated
        list_network_offerings_response = NetworkOffering.list(
                                                         self.api_client,
                                                         id=self.shared_network_offering_sg.id
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
        
        physical_network = PhysicalNetwork.list(self.api_client)[0]

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["acltype"] = "domain"
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id
        self.shared_network_sg = Network.create(
                         self.api_client,
                         self.services["shared_network_sg"],
                         domainid=self.admin_account.domainid,
                         networkofferingid=self.shared_network_offering_sg.id,
                         zoneid=self.zone.id
                         )
        
        self.cleanup_networks.append(self.shared_network_sg)
        
        list_networks_response = Network.list(
                                        self.api_client,
                                        id=self.shared_network_sg.id
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

        self.debug("Shared Network created: %s" % self.shared_network_sg.id)
        
        self.shared_network_admin_account_virtual_machine = VirtualMachine.create(
                                                                     self.api_client,
                                                                     self.services["virtual_machine"],
                                                                     accountid=self.admin_account.name,
                                                                     domainid=self.admin_account.domainid,
                                                                     networkids=self.shared_network_sg.id,
								     serviceofferingid=self.service_offering.id
                                                                     )
        self.cleanup_vms.append(self.shared_network_admin_account_virtual_machine)
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
        
        ip_range = list(netaddr.iter_iprange(unicode(self.services["shared_network_sg"]["startip"]), unicode(self.services["shared_network_sg"]["endip"])))
        if netaddr.IPAddress(unicode(vms[0].nic[0].ipaddress)) not in ip_range:
            self.fail("Virtual machine ip should be from the ip range assigned to network created.")

