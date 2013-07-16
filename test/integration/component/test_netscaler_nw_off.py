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

""" P1 tests for multiple netscaler instances
"""
#Import Local Modules
import marvin
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from marvin.remoteSSHClient import remoteSSHClient
import datetime


class Services:
    """Test netscaler Services
    """

    def __init__(self):
        self.services = {
                         "account": {
                                    "email": "test@test.com",
                                    "firstname": "Test",
                                    "lastname": "User",
                                    "username": "test",
                                    # Random characters are appended for unique
                                    # username
                                    "password": "password",
                         },
                         "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 100,    # in MHz
                                    "memory": 128,      # In MBs
                         },
                         "virtual_machine": {
                                    "displayname": "TestVM",
                                    "username": "root",
                                    "password": "password",
                                    "ssh_port": 22,
                                    "hypervisor": 'XenServer',
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": 'TCP',
                                },
                         "netscaler_1": {
                                "ipaddress": '10.147.60.27',
                                "username": 'nsroot',
                                "password": 'nsroot',
                                "networkdevicetype": 'NetscalerVPXLoadBalancer',
                                "publicinterface": '1/1',
                                "privateinterface": '1/1',
                                "numretries": 2,
                                "lbdevicededicated": False,
                                "lbdevicecapacity": 50,
                                "port": 22,
                         },
                         "netscaler_2": {
                                "ipaddress": '192.168.100.100',
                                "username": 'nsroot',
                                "password": 'nsroot',
                                "networkdevicetype": 'NetscalerVPXLoadBalancer',
                                "publicinterface": '1/1',
                                "privateinterface": '1/1',
                                "numretries": 2,
                                "lbdevicededicated": False,
                                "lbdevicecapacity": 50,
                                "port": 22,
                         },
                         "netscaler_3": {
                                "ipaddress": '192.168.100.101',
                                "username": 'nsroot',
                                "password": 'nsroot',
                                "networkdevicetype": 'NetscalerVPXLoadBalancer',
                                "publicinterface": '1/1',
                                "privateinterface": '1/1',
                                "numretries": 2,
                                "lbdevicededicated": False,
                                "lbdevicecapacity": 50,
                                "port": 22,
                         },
                         "network_offering_dedicated": {
                                    "name": 'Netscaler',
                                    "displaytext": 'Netscaler',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Firewall,Lb,UserData,StaticNat',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "specifyVlan": False,
                                    "specifyIpRanges": False,
                                    "serviceProviderList": {
                                            "Dhcp": 'VirtualRouter',
                                            "Dns": 'VirtualRouter',
                                            "SourceNat": 'VirtualRouter',
                                            "PortForwarding": 'VirtualRouter',
                                            "Vpn": 'VirtualRouter',
                                            "Firewall": 'VirtualRouter',
                                            "Lb": 'Netscaler',
                                            "UserData": 'VirtualRouter',
                                            "StaticNat": 'VirtualRouter',
                                    },
                                    "serviceCapabilityList": {
                                        "SourceNat": {
                                            "SupportedSourceNatTypes": "peraccount"
                                        },
                                        "lb": {
                                               "SupportedLbIsolation": "dedicated"
                                        },
                                    },
                         },
                         "network_offering": {
                                    "name": 'Netscaler',
                                    "displaytext": 'Netscaler',
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
                                            "Lb": 'Netscaler',
                                            "UserData": 'VirtualRouter',
                                            "StaticNat": 'VirtualRouter',
                                    },
                         },
                         "network": {
                                  "name": "Netscaler",
                                  "displaytext": "Netscaler",
                         },
                         "lbrule": {
                                    "name": "SSH",
                                    "alg": "roundrobin",
                                    # Algorithm used for load balancing
                                    "privateport": 22,
                                    "publicport": 22,
                                    "openfirewall": False,
                         },
                         "ostype": 'Cent OS 5.3 (64 bit)',
                         # Cent OS 5.3 (64 bit)
                         "sleep": 60,
                         "timeout": 10,
                    }


class TestAddMultipleNetScaler(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestAddMultipleNetScaler,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls._cleanup = []
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
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advancedns"])
    def test_add_netscaler_device(self):
        """Test add netscaler device
        """


        # Validate the following
        # 1. Add multiple instances of netscaler
        # 2. Netscaler should be configured successfully.

        physical_networks = PhysicalNetwork.list(
                                                 self.apiclient,
                                                 zoneid=self.zone.id
                                                 )
        self.assertEqual(
            isinstance(physical_networks, list),
            True,
            "There should be atleast one physical network for advanced zone"
            )
        physical_network = physical_networks[0]
        self.debug("Adding netscaler device: %s" %
                                    self.services["netscaler_1"]["ipaddress"])
        netscaler_1 = NetScaler.add(
                                  self.apiclient,
                                  self.services["netscaler_1"],
                                  physicalnetworkid=physical_network.id
                                  )
        self.cleanup.append(netscaler_1)
        self.debug("Checking if Netscaler network service provider is enabled?")

        nw_service_providers = NetworkServiceProvider.list(
                                        self.apiclient,
                                        name='Netscaler',
                                        physicalnetworkid=physical_network.id
                                        )
        self.assertEqual(
                         isinstance(nw_service_providers, list),
                         True,
                         "Network service providers list should not be empty"
                         )
        netscaler_provider = nw_service_providers[0]
        if netscaler_provider.state != 'Enabled':
            self.debug("Netscaler provider is not enabled. Enabling it..")
            response = NetworkServiceProvider.update(
                                          self.apiclient,
                                          id=netscaler_provider.id,
                                          state='Enabled'
                                          )
            self.assertEqual(
                        response.state,
                        "Enabled",
                        "Network service provider should be in enabled state"
                         )
        else:
            self.debug("Netscaler service provider is already enabled.")

        ns_list = NetScaler.list(
                                 self.apiclient,
                                 lbdeviceid=netscaler_1.lbdeviceid
                                 )
        self.assertEqual(
                         isinstance(ns_list, list),
                         True,
                         "NetScaler list should not be empty"
                         )
        ns = ns_list[0]

        self.assertEqual(
                         ns.lbdevicededicated,
                         False,
                         "NetScaler device is configured in shared mode"
                         )
        self.assertEqual(
                         ns.lbdevicestate,
                         "Enabled",
                         "NetScaler device state should be enabled"
                         )
        self.assertEqual(
            ns.physicalnetworkid,
            physical_network.id,
            "Physical network id should match with the network in which device is configured"
            )

        self.debug("Adding netscaler device: %s" %
                                    self.services["netscaler_2"]["ipaddress"])
        netscaler_2 = NetScaler.add(
                                  self.apiclient,
                                  self.services["netscaler_2"],
                                  physicalnetworkid=physical_network.id
                                  )
        self.cleanup.append(netscaler_2)
        ns_list = NetScaler.list(
                                 self.apiclient,
                                 lbdeviceid=netscaler_1.lbdeviceid
                                 )
        self.assertEqual(
                         isinstance(ns_list, list),
                         True,
                         "NetScaler list should not be empty"
                         )
        ns = ns_list[0]

        self.assertEqual(
                         ns.lbdevicededicated,
                         False,
                         "NetScaler device is configured in shared mode"
                         )
        self.assertEqual(
                         ns.lbdevicestate,
                         "Enabled",
                         "NetScaler device state should be enabled"
                         )
        self.assertEqual(
            ns.physicalnetworkid,
            physical_network.id,
            "Physical network id should match with the network in which device is configured"
            )
        self.debug("Another Netscaler device is added!")
        return


class TestAddMultipleNSDiffZone(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestAddMultipleNSDiffZone,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        cls._cleanup = []
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
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advancedns", "multizone"])
    def test_add_mul_netscaler_diff_zone(self):
        """Test add netscaler devices in different zones
        """


        # Validate the following
        # 1. Add multiple instances of Netscaler in different zones
        # 2. Netscaler should be configured successfully.

        # Check if there are multiple zones present in the given setup
        zones = Zone.list(self.apiclient, listall=True)
        self.assertEqual(
                         isinstance(zones, list),
                         True,
                         "List Zones API should return a valid list"
                         )

        # Find the number of zones configured in advanced mode
        zone_list = []
        for zone in zones:
            if zone.networktype == 'Advanced':
                zone_list.append(zone)

        self.assertGreater(
                           len(zone_list),
                           1,
                           "Atleast 2 advanced mode zones should be present for this test"
                           )

        physical_networks = PhysicalNetwork.list(
                                                 self.apiclient,
                                                 zoneid=zone_list[0].id
                                                 )
        self.assertEqual(
                isinstance(physical_networks, list),
                True,
                "There should be atleast one physical network for advanced zone"
                )
        physical_network = physical_networks[0]
        self.debug("Adding netscaler device: %s" %
                                    self.services["netscaler_1"]["ipaddress"])
        netscaler_1 = NetScaler.add(
                                  self.apiclient,
                                  self.services["netscaler_1"],
                                  physicalnetworkid=physical_network.id
                                  )
        self.cleanup.append(netscaler_1)
        self.debug("Checking if Netscaler network service provider is enabled?")

        nw_service_providers = NetworkServiceProvider.list(
                                        self.apiclient,
                                        name='Netscaler',
                                        physicalnetworkid=physical_network.id
                                        )
        self.assertEqual(
                         isinstance(nw_service_providers, list),
                         True,
                         "Network service providers list should not be empty"
                         )
        netscaler_provider = nw_service_providers[0]
        if netscaler_provider.state != 'Enabled':
            self.debug("Netscaler provider is not enabled. Enabling it..")
            response = NetworkServiceProvider.update(
                                          self.apiclient,
                                          id=netscaler_provider.id,
                                          state='Enabled'
                                          )
            self.assertEqual(
                        response.state,
                        "Enabled",
                        "Network service provider should be in enabled state"
                         )
        else:
            self.debug("Netscaler service provider is already enabled.")

        ns_list = NetScaler.list(
                                 self.apiclient,
                                 lbdeviceid=netscaler_1.lbdeviceid
                                 )
        self.assertEqual(
                         isinstance(ns_list, list),
                         True,
                         "NetScaler list should not be empty"
                         )
        ns = ns_list[0]

        self.assertEqual(
                         ns.lbdevicededicated,
                         False,
                         "NetScaler device is configured in shared mode"
                         )
        self.assertEqual(
                         ns.lbdevicestate,
                         "Enabled",
                         "NetScaler device state should be enabled"
                         )
        self.assertEqual(
            ns.physicalnetworkid,
            physical_network.id,
            "Physical network id should match with the network in which device is configured"
            )

        physical_networks = PhysicalNetwork.list(
                                                 self.apiclient,
                                                 zoneid=zone_list[1].id
                                                 )
        self.assertEqual(
                isinstance(physical_networks, list),
                True,
                "There should be atleast one physical network for advanced zone"
                )
        physical_network = physical_networks[0]

        self.debug("Adding netscaler device: %s" %
                                    self.services["netscaler_2"]["ipaddress"])
        netscaler_2 = NetScaler.add(
                                  self.apiclient,
                                  self.services["netscaler_2"],
                                  physicalnetworkid=physical_network.id
                                  )
        self.cleanup.append(netscaler_2)
        ns_list = NetScaler.list(
                                 self.apiclient,
                                 lbdeviceid=netscaler_2.lbdeviceid
                                 )
        self.assertEqual(
                         isinstance(ns_list, list),
                         True,
                         "NetScaler list should not be empty"
                         )
        ns = ns_list[0]

        self.assertEqual(
                         ns.lbdevicededicated,
                         False,
                         "NetScaler device is configured in shared mode"
                         )
        self.assertEqual(
                         ns.lbdevicestate,
                         "Enabled",
                         "NetScaler device state should be enabled"
                         )
        self.assertEqual(
            ns.physicalnetworkid,
            physical_network.id,
            "Physical network id should match with the network in which device is configured"
            )
        self.debug("Another Netscaler device is added!")
        return


class TestNetScalerSharedMode(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestNetScalerSharedMode,
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

        physical_networks = PhysicalNetwork.list(
                                                 cls.api_client,
                                                 zoneid=cls.zone.id
                                                 )
        if isinstance(physical_networks, list):
            cls.physical_network = physical_networks[0]
        cls.services["netscaler_1"]["lbdevicecapacity"] = 2
        cls.netscaler_1 = NetScaler.add(
                                  cls.api_client,
                                  cls.services["netscaler_1"],
                                  physicalnetworkid=cls.physical_network.id
                                  )

        nw_service_providers = NetworkServiceProvider.list(
                                        cls.api_client,
                                        name='Netscaler',
                                        physicalnetworkid=cls.physical_network.id
                                        )
        if isinstance(nw_service_providers, list):
            netscaler_provider = nw_service_providers[0]

        if netscaler_provider.state != 'Enabled':
            response = NetworkServiceProvider.update(
                                          cls.api_client,
                                          id=netscaler_provider.id,
                                          state='Enabled'
                                          )
        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.account_1 = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     admin=True,
                                     domainid=cls.domain.id
                                     )
        cls.account_2 = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     admin=True,
                                     domainid=cls.domain.id
                                     )
        cls.account_3 = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     admin=True,
                                     domainid=cls.domain.id
                                     )
        cls.account_4 = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     admin=True,
                                     domainid=cls.domain.id
                                     )
        cls.account_5 = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     admin=True,
                                     domainid=cls.domain.id
                                     )
        cls._cleanup = [
                        cls.service_offering,
                        cls.network_offering,
                        cls.account_1,
                        cls.account_2,
                        cls.account_3,
                        cls.account_5
                        ]
        cls.cleanup_devices = [cls.netscaler_1]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
            interval = list_configurations(
                                    cls.api_client,
                                    name='network.gc.interval'
                                    )
            wait = list_configurations(
                                    cls.api_client,
                                    name='network.gc.wait'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) + int(wait[0].value))
            cleanup_resources(cls.api_client, cls.cleanup_devices)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advancedns"])
    def test_01_netscaler_shared_mode(self):
        """Test netscaler device in shared mode
        """


        # Validate the following
        # 1. Add Netscaler device in shared mode with capacity 3
        # 2. Netscaler should be configured successfully.It should be able to
        #    service only 3 account.

        ns_list = NetScaler.list(
                                 self.apiclient,
                                 lbdeviceid=self.netscaler_1.lbdeviceid
                                 )
        self.assertEqual(
                         isinstance(ns_list, list),
                         True,
                         "NetScaler list should not be empty"
                         )
        ns = ns_list[0]

        self.assertEqual(
                         ns.lbdevicededicated,
                         False,
                         "NetScaler device is configured in shared mode"
                         )
        self.assertEqual(
                         ns.lbdevicestate,
                         "Enabled",
                         "NetScaler device state should be enabled"
                         )
        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        self.network_1 = Network.create(
                                    self.apiclient,
                                    self.services["network"],
                                    accountid=self.account_1.name,
                                    domainid=self.account_1.domainid,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )
        self.debug("Created network with ID: %s" % self.network_1.id)

        self.debug("Deploying VM in account: %s" % self.account_1.name)

        # Spawn an instance in that network
        virtual_machine_1 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_1.name,
                                  domainid=self.account_1.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_1.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network_1.id)
        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=virtual_machine_1.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % virtual_machine_1.id
            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Running",
                            "VM state should be running after deployment"
                        )

        # Creating network using the network offering created
        self.debug("Trying to create network with network offering: %s" %
                                                    self.network_offering.id)
        self.network_2 = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account_2.name,
                                domainid=self.account_2.domainid,
                                networkofferingid=self.network_offering.id,
                                zoneid=self.zone.id
                                )
        self.debug("Created network with ID: %s" % self.network_2.id)

        self.debug("Deploying VM in account: %s" % self.account_2.name)

        # Spawn an instance in that network
        virtual_machine_2 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_2.name,
                                  domainid=self.account_2.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_2.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network_2.id)
        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=virtual_machine_2.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % virtual_machine_2.id
            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Running",
                            "VM state should be running after deployment"
                        )
        # Creating network using the network offering created
        self.debug("Trying to create network with network offering: %s" %
                                                    self.network_offering.id)
        self.network_3 = Network.create(
                            self.apiclient,
                            self.services["network"],
                            accountid=self.account_3.account.name,
                            domainid=self.account_3.account.domainid,
                            networkofferingid=self.network_offering.id,
                            zoneid=self.zone.id
                        )
        self.debug("Created network with ID: %s" % self.network_3.id)
        self.debug("Deploying VM in account: %s" % self.account_3.account.name)

        with self.assertRaises(Exception):
            # Spawn an instance in that network
            virtual_machine_3 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_3.account.name,
                                  domainid=self.account_3.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_3.id)]
                                  )

        self.debug("Deploy VM failed as Netscaler device capacity is full!")
        return

    @attr(tags = ["advancedns"])
    def test_02_multiple_netscaler_capacilty(self):
        """Test multiple netscaler devices with limited capacity
        """


        # Validate the following
        # 1. Add another netscaler device and spawn a new VM again
        # 2. VM deployement should be successful

        self.debug("Adding another netscaler device: %s" %
                                    self.services["netscaler_2"]["ipaddress"])
        self.services["netscaler_2"]["lbdevicecapacity"] = 2
        netscaler_2 = NetScaler.add(
                                  self.apiclient,
                                  self.services["netscaler_2"],
                                  physicalnetworkid=self.physical_network.id
                                  )
        self.cleanup_devices.append(netscaler_2)
        ns_list = NetScaler.list(
                                 self.apiclient,
                                 lbdeviceid=netscaler_2.lbdeviceid
                                 )
        self.assertEqual(
                         isinstance(ns_list, list),
                         True,
                         "NetScaler list should not be empty"
                         )
        ns = ns_list[0]

        self.assertEqual(
                         ns.lbdevicededicated,
                         False,
                         "NetScaler device is configured in shared mode"
                         )
        self.assertEqual(
                         ns.lbdevicestate,
                         "Enabled",
                         "NetScaler device state should be enabled"
                         )
        self.assertEqual(
            ns.physicalnetworkid,
            self.physical_network.id,
            "Physical network id should match with the network in which device is configured"
            )
        self.debug("Another Netscaler device is added!")

        # Creating network using the network offering created
        self.debug("Trying to create network with network offering: %s" %
                                                    self.network_offering.id)
        networks = Network.list(
                            self.apiclient,
                            account=self.account_3.account.name,
                            domainid=self.account_3.account.domainid,
                            zoneid=self.zone.id,
                            listall=True
                        )
        self.assertEqual(
                         isinstance(networks, list),
                         True,
                         "Network should be present for the account: %s" %
                                                self.account_3.account.name
                         )
        self.network_3 = networks[0]
        self.debug("Created network with ID: %s" % self.network_3.id)

        self.debug("Deploying VM in account: %s" % self.account_3.account.name)

        # Spawn an instance in that network
        virtual_machine_3 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_3.account.name,
                                  domainid=self.account_3.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_3.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network_3.id)
        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=virtual_machine_3.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % virtual_machine_3.id
            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Running",
                            "VM state should be running after deployment"
                        )
        return

    @attr(tags = ["advancedns"])
    def test_03_multiple_netscaler_full_capacilty(self):
        """Test netscaler device with full capacity
        """


        # Validate the following
        # 1. Spawn multiple instances for utilizing full capacity of Netscaler
        # 2. Deploy VM should fail after capacity full in netscaler device

        # Creating network using the network offering created
        self.debug("Trying to create network with network offering: %s" %
                                                    self.network_offering.id)
        self.network_4 = Network.create(
                            self.apiclient,
                            self.services["network"],
                            accountid=self.account_4.account.name,
                            domainid=self.account_4.account.domainid,
                            networkofferingid=self.network_offering.id,
                            zoneid=self.zone.id
                        )
        self.debug("Created network with ID: %s" % self.network_4.id)

        self.debug("Deploying VM in account: %s" % self.account_4.account.name)

        # Spawn an instance in that network
        virtual_machine_4 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_4.account.name,
                                  domainid=self.account_4.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_4.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network_4.id)
        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=virtual_machine_4.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % virtual_machine_4.id
            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Running",
                            "VM state should be running after deployment"
                        )
        # Creating network using the network offering created
        self.debug("Trying to create network with network offering: %s" %
                                                    self.network_offering.id)
        self.network_5 = Network.create(
                            self.apiclient,
                            self.services["network"],
                            accountid=self.account_5.account.name,
                            domainid=self.account_5.account.domainid,
                            networkofferingid=self.network_offering.id,
                            zoneid=self.zone.id
                        )
        self.debug("Created network with ID: %s" % self.network_5.id)

        self.debug("Deploying VM in account: %s" % self.account_5.account.name)

        with self.assertRaises(Exception):
            # Spawn an instance in that network
            virtual_machine_5 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_5.account.name,
                                  domainid=self.account_5.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_5.id)]
                                  )
        self.debug("Deploy VM failed as Netscaler device capacity is full!")
        return

    @attr(configuration = "network.gc")
    @attr(tags = ["advancedns"])
    def test_04_delete_account_after_capacity_full(self):
        """Test delete and add resouces after netscaler device capacity is full
        """


        # Validate the following
        # 1. Delete one of the account. Wait till Network.gc.wait &
        #    network.gc.interval time
        # 2. Create an instance from another account
        # 3. Deploy instance should succeed

        self.debug("Delete account: %s" % self.account_4.account.name)
        self.account_4.delete(self.apiclient)
        self.debug("Account: %s is deleted" % self.account_4.account.name)

        interval = list_configurations(
                                    self.apiclient,
                                    name='network.gc.interval'
                                    )
        wait = list_configurations(
                                    self.apiclient,
                                    name='network.gc.wait'
                                    )
        self.debug("Sleeping for: network.gc.interval + network.gc.wait")
        # Sleep to ensure that all resources are deleted
        time.sleep(int(interval[0].value) + int(wait[0].value))

        # Creating network using the network offering created
        self.debug("Trying to create network with network offering: %s" %
                                                    self.network_offering.id)
        self.network_5 = Network.create(
                            self.apiclient,
                            self.services["network"],
                            accountid=self.account_5.account.name,
                            domainid=self.account_5.account.domainid,
                            networkofferingid=self.network_offering.id,
                            zoneid=self.zone.id
                        )
        self.debug("Created network with ID: %s" % self.network_5.id)

        self.debug("Deploying VM in account: %s" % self.account_5.account.name)

        # Spawn an instance in that network
        virtual_machine_5 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_5.account.name,
                                  domainid=self.account_5.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_5.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network_5.id)
        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=virtual_machine_5.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % virtual_machine_5.id
            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Running",
                            "VM state should be running after deployment"
                        )
        return


class TestNwOffDedicatedNetscaler(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestNwOffDedicatedNetscaler,
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

        physical_networks = PhysicalNetwork.list(
                                                 cls.api_client,
                                                 zoneid=cls.zone.id
                                                 )
        if isinstance(physical_networks, list):
            physical_network = physical_networks[0]
        cls.services["netscaler_1"]["lbdevicecapacity"] = 3
        cls.netscaler = NetScaler.add(
                                  cls.api_client,
                                  cls.services["netscaler_1"],
                                  physicalnetworkid=physical_network.id
                                  )

        nw_service_providers = NetworkServiceProvider.list(
                                        cls.api_client,
                                        name='Netscaler',
                                        physicalnetworkid=physical_network.id
                                        )
        if isinstance(nw_service_providers, list):
            netscaler_provider = nw_service_providers[0]

        if netscaler_provider.state != 'Enabled':
            response = NetworkServiceProvider.update(
                                          cls.api_client,
                                          id=netscaler_provider.id,
                                          state='Enabled'
                                          )
        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering_dedicated"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls._cleanup = [
                        cls.service_offering,
                        cls.network_offering,
                        cls.netscaler,
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
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.account_1 = Account.create(
                                     self.apiclient,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )
        self.cleanup = [self.account_1]
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            interval = list_configurations(
                                    self.apiclient,
                                    name='network.gc.interval'
                                    )
            wait = list_configurations(
                                    self.apiclient,
                                    name='network.gc.wait'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) + int(wait[0].value))
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advancedns"])
    def test_nw_off_dedicated_mode(self):
        """Test network offering in dedicated mode device
        """


        # Validate the following
        # 1. Add Netscaler device in shared mode
        # 2. Create a network offering in dedicated mode.
        # 3. Try to implemenent network with that network offering. Network
        #    craetion should fail.

        ns_list = NetScaler.list(
                                 self.apiclient,
                                 lbdeviceid=self.netscaler.lbdeviceid
                                 )
        self.assertEqual(
                         isinstance(ns_list, list),
                         True,
                         "NetScaler list should not be empty"
                         )
        ns = ns_list[0]

        self.assertEqual(
                         ns.lbdevicededicated,
                         False,
                         "NetScaler device is configured in shared mode"
                         )
        self.assertEqual(
                         ns.lbdevicestate,
                         "Enabled",
                         "NetScaler device state should be enabled"
                         )
        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)

        self.network_1 = Network.create(
                                    self.apiclient,
                                    self.services["network"],
                                    accountid=self.account_1.name,
                                    domainid=self.account_1.domainid,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )
        self.debug("Deploy Instance with network: %s" % self.network_1.name)
        with self.assertRaises(Exception):
            # Spawn an instance in that network
            VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_1.name,
                                  domainid=self.account_1.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_1.id)]
                                  )
        self.debug("Created instance failed!")
        return


class TestNwOffNetscaler(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestNwOffNetscaler,
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

        physical_networks = PhysicalNetwork.list(
                                                 cls.api_client,
                                                 zoneid=cls.zone.id
                                                 )
        if isinstance(physical_networks, list):
            physical_network = physical_networks[0]
        cls.services["netscaler_1"]["lbdevicecapacity"] = 3
        cls.netscaler_1 = NetScaler.add(
                                  cls.api_client,
                                  cls.services["netscaler_1"],
                                  physicalnetworkid=physical_network.id
                                  )

        cls.services["netscaler_2"].pop("lbdevicecapacity")
        cls.services["netscaler_2"]["lbdevicededicated"] = True
        cls.netscaler_2 = NetScaler.add(
                                  cls.api_client,
                                  cls.services["netscaler_2"],
                                  physicalnetworkid=physical_network.id
                                  )
        nw_service_providers = NetworkServiceProvider.list(
                                        cls.api_client,
                                        name='Netscaler',
                                        physicalnetworkid=physical_network.id
                                        )
        if isinstance(nw_service_providers, list):
            netscaler_provider = nw_service_providers[0]

        if netscaler_provider.state != 'Enabled':
            response = NetworkServiceProvider.update(
                                          cls.api_client,
                                          id=netscaler_provider.id,
                                          state='Enabled'
                                          )
        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering_dedicated"],
                                            conservemode=True
                                            )
        cls.network_offering_shared = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')
        cls.network_offering_shared.update(cls.api_client, state='Enabled')
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls._cleanup = [
                        cls.service_offering,
                        cls.network_offering,
                        cls.network_offering_shared,
                        cls.netscaler_1,
                        cls.netscaler_2,
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
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.account_1 = Account.create(
                                     self.apiclient,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )
        self.account_2 = Account.create(
                                     self.apiclient,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )
        self.account_3 = Account.create(
                                     self.apiclient,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )
        self.cleanup = [self.account_2, self.account_3]
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            interval = list_configurations(
                                    self.apiclient,
                                    name='network.gc.interval'
                                    )
            wait = list_configurations(
                                    self.apiclient,
                                    name='network.gc.wait'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) + int(wait[0].value))
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advancedns"])
    def test_ns_shared_nw_dedicated(self):
        """Test netscaler device in shared mode with network offering in dedicated mode
        """


        # Validate the following
        # 1. Add Netscaler device in shared mode
        # 2. Create a network offering in dedicated mode.
        # 3. Try to implemenent network with that network offering. Network
        #    craetion should fail.

        ns_list = NetScaler.list(
                                 self.apiclient,
                                 lbdeviceid=self.netscaler_1.lbdeviceid
                                 )
        self.assertEqual(
                         isinstance(ns_list, list),
                         True,
                         "NetScaler list should not be empty"
                         )
        ns = ns_list[0]

        self.assertEqual(
                         ns.lbdevicededicated,
                         False,
                         "NetScaler device is configured in shared mode"
                         )
        self.assertEqual(
                         ns.lbdevicestate,
                         "Enabled",
                         "NetScaler device state should be enabled"
                         )
        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        self.network = Network.create(
                                    self.apiclient,
                                    self.services["network"],
                                    accountid=self.account_1.name,
                                    domainid=self.account_1.domainid,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )
        self.debug("Deploying VM in account: %s" % self.account_1.name)
        virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_1.name,
                                  domainid=self.account_1.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network.id)]
                                  )
        # Creating network using the network offering created
        self.debug("Creating different network with network offering: %s" %
                                                    self.network_offering.id)

        self.network_2 = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account_2.name,
                                domainid=self.account_2.domainid,
                                networkofferingid=self.network_offering.id,
                                zoneid=self.zone.id
                               )
        self.debug("Created network with ID: %s" % self.network_2.id)
        with self.assertRaises(Exception):
            VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_2.name,
                                  domainid=self.account_2.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_2.id)]
                                  )
        self.debug(
            "Attempt to create second network with dedicated network offering failed!")
        self.debug("Deleting account: %s" % self.account_1.name)
        self.account_1.delete(self.apiclient)
        self.debug("Account: %s deleted!" % self.account_1.name)
        interval = list_configurations(
                                    self.apiclient,
                                    name='network.gc.interval'
                                    )
        wait = list_configurations(
                                    self.apiclient,
                                    name='network.gc.wait'
                                    )
        self.debug("Sleeping for: network.gc.interval + network.gc.wait")
        # Sleep to ensure that all resources are deleted
        time.sleep(int(interval[0].value) + int(wait[0].value))

        self.debug("Deploying VM in account: %s" % self.account_2.name)

        # Spawn an instance in that network
        virtual_machine_2 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_2.name,
                                  domainid=self.account_2.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_2.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network_2.id)
        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=virtual_machine_2.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % virtual_machine_2.id
            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Running",
                            "VM state should be running after deployment"
                        )
        # Creating network using the network offering created
        self.debug("Trying to create network with network offering: %s" %
                                                    self.network_offering_shared.id)
        self.network_3 = Network.create(
                            self.apiclient,
                            self.services["network"],
                            accountid=self.account_3.account.name,
                            domainid=self.account_3.account.domainid,
                            networkofferingid=self.network_offering_shared.id,
                            zoneid=self.zone.id
                        )
        self.debug("Created network with ID: %s" % self.network_3.id)

        self.debug("Deploying VM in account: %s" % self.account_3.account.name)

        # Spawn an instance in that network
        virtual_machine_3 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_3.account.name,
                                  domainid=self.account_3.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_3.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network_3.id)
        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=virtual_machine_3.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % virtual_machine_3.id
            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Running",
                            "VM state should be running after deployment"
                        )
        return


class TestNwOffSToDUpgrade(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestNwOffSToDUpgrade,
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

        physical_networks = PhysicalNetwork.list(
                                                 cls.api_client,
                                                 zoneid=cls.zone.id
                                                 )
        if isinstance(physical_networks, list):
            cls.physical_network = physical_networks[0]
        cls.services["netscaler_1"]["lbdevicecapacity"] = 3
        cls.netscaler_1 = NetScaler.add(
                                  cls.api_client,
                                  cls.services["netscaler_1"],
                                  physicalnetworkid=cls.physical_network.id
                                  )

        cls.services["netscaler_2"].pop("lbdevicecapacity")
        cls.services["netscaler_2"]["lbdevicededicated"] = True
        cls.netscaler_2 = NetScaler.add(
                                  cls.api_client,
                                  cls.services["netscaler_2"],
                                  physicalnetworkid=cls.physical_network.id
                                  )

        nw_service_providers = NetworkServiceProvider.list(
                                        cls.api_client,
                                        name='Netscaler',
                                        physicalnetworkid=cls.physical_network.id
                                        )
        if isinstance(nw_service_providers, list):
            netscaler_provider = nw_service_providers[0]

        if netscaler_provider.state != 'Enabled':
            response = NetworkServiceProvider.update(
                                          cls.api_client,
                                          id=netscaler_provider.id,
                                          state='Enabled'
                                          )
        cls.network_offering_dedicated = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering_dedicated"],
                                            conservemode=True
                                            )
        cls.network_offering_shared = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering_dedicated.update(cls.api_client, state='Enabled')
        cls.network_offering_shared.update(cls.api_client, state='Enabled')
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls._cleanup = [
                        cls.service_offering,
                        cls.network_offering_dedicated,
                        cls.network_offering_shared,
                        cls.netscaler_1,
                        cls.netscaler_2,
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
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.account_1 = Account.create(
                                     self.apiclient,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )
        self.account_2 = Account.create(
                                     self.apiclient,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )
        self.account_3 = Account.create(
                                     self.apiclient,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )
        self.cleanup = [self.account_1, self.account_2, self.account_3]
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            interval = list_configurations(
                                    self.apiclient,
                                    name='network.gc.interval'
                                    )
            wait = list_configurations(
                                    self.apiclient,
                                    name='network.gc.wait'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) + int(wait[0].value))
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advancedns"])
    def test_shared_to_dedicated_upgrade(self):
        """Test upgrade from shared LB isolation to dedicated LB isolation"""


        # Validate the following
        # 1. Create a dedicated and shared network offering
        # 2. Configure 2 instances of Netscaler one with dedicated and other
        #    shared mode
        # 3. Deploy instance with shared network offering in account 1. create
        #    LB rules
        # 4. Deploy instance with shared network offering in account 2. create
        #   LB rules
        # 5. Deploy instance with dedicated network offering in account 3.
        #   Create Lb rules.
        # 6. Configure another instace of netscaler in dedicated mode
        # 7. upgrade networkj for user 1 to dedicated network offering.
        #    Create LB rules. LB rule creation should be successful

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering_shared.id)
        self.network_1 = Network.create(
                            self.apiclient,
                            self.services["network"],
                            accountid=self.account_1.name,
                            domainid=self.account_1.domainid,
                            networkofferingid=self.network_offering_shared.id,
                            zoneid=self.zone.id
                            )
        self.debug("Created network with ID: %s" % self.network_1.id)

        self.debug("Deploying VM in account: %s" % self.account_1.name)

        # Spawn an instance in that network
        virtual_machine_1 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_1.name,
                                  domainid=self.account_1.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_1.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network_1.id)
        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=virtual_machine_1.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % virtual_machine_1.id
            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Running",
                            "VM state should be running after deployment"
                        )

        # Creating network using the network offering created
        self.debug("Trying to create network with network offering: %s" %
                                                    self.network_offering_shared.id)
        self.network_2 = Network.create(
                            self.apiclient,
                            self.services["network"],
                            accountid=self.account_2.name,
                            domainid=self.account_2.domainid,
                            networkofferingid=self.network_offering_shared.id,
                            zoneid=self.zone.id
                            )
        self.debug("Created network with ID: %s" % self.network_2.id)

        self.debug("Deploying VM in account: %s" % self.account_2.name)

        # Spawn an instance in that network
        virtual_machine_2 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_2.name,
                                  domainid=self.account_2.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_2.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network_2.id)
        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=virtual_machine_2.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % virtual_machine_2.id
            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Running",
                            "VM state should be running after deployment"
                        )
        # Creating network using the network offering created
        self.debug("Trying to create network with network offering: %s" %
                                                    self.network_offering_dedicated.id)
        self.network_3 = Network.create(
                            self.apiclient,
                            self.services["network"],
                            accountid=self.account_3.account.name,
                            domainid=self.account_3.account.domainid,
                            networkofferingid=self.network_offering_dedicated.id,
                            zoneid=self.zone.id
                        )
        self.debug("Created network with ID: %s" % self.network_3.id)
        self.debug("Deploying VM in account: %s" % self.account_3.account.name)

        # Spawn an instance in that network
        virtual_machine_3 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_3.account.name,
                                  domainid=self.account_3.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_3.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network_3.id)
        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=virtual_machine_3.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % virtual_machine_3.id
            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Running",
                            "VM state should be running after deployment"
                        )
        self.debug("Configuring another Netscaler device in dedicated mode")

        self.services["netscaler_3"].pop("lbdevicecapacity")
        self.services["netscaler_3"]["lbdevicededicated"] = True
        self.netscaler_3 = NetScaler.add(
                                  self.apiclient,
                                  self.services["netscaler_3"],
                                  physicalnetworkid=self.physical_network.id
                                  )

        self.debug("Stopping All VMs before upgrading network for account: %s" %
                            self.account_1.name)
        virtual_machine_1.stop(self.apiclient)

        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=virtual_machine_1.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % virtual_machine_1.id
            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Stopped",
                            "VM state should be running after deployment"
                        )
        self.debug("All Vms are in stopped state")
        self.debug("Upgrading the network: %s" % self.network_1.id)
        self.network_1.update(
                            self.apiclient,
                            networkofferingid=self.network_offering_dedicated.id,
                            changecidr=True
                            )
        networks = Network.list(
                                self.apiclient,
                                id=self.network_1.id,
                                listall=True
                                )
        self.assertEqual(
                isinstance(networks, list),
                True,
                "List Networks should return a valid list for given network ID"
                )
        self.assertNotEqual(
                            len(networks),
                            0,
                            "Length of list networks should not be 0"
                            )
        network = networks[0]
        self.assertEqual(
                        network.networkofferingid,
                        self.network_offering_dedicated.id,
                        "Network offering ID should match with new offering ID"
                        )

        self.debug("Starting All VMs after upgrading network for account: %s" %
                            self.account_1.name)
        virtual_machine_1.start(self.apiclient)

        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=virtual_machine_1.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % virtual_machine_1.id
            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Running",
                            "VM state should be running after deployment"
                        )
        self.debug("All Vms are in running state")
        try:
            self.debug(
                "Associating public Ip to the network: %s" %
                                                        self.network_1.name)

            public_ip = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account_1.name,
                                zoneid=self.zone.id,
                                domainid=self.account_1.domainid,
                                networkid=self.network_1.id
                                )
            self.debug(
            "Creating LB rule for IP address: %s with round robin algo" %
                                                public_ip.ipaddress.ipaddress)

            lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=public_ip.ipaddress.id,
                                    accountid=self.account_1.name,
                                    networkid=self.network_1.id
                                )
            self.debug("Created the load balancing rule for public IP: %s" %
                                                public_ip.ipaddress.ipaddress)
        except Exception as e:
            self.fail("Failed to create load balancing rule - %s" % e)
        return


class TestNwOffDToSUpgrade(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestNwOffDToSUpgrade,
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

        physical_networks = PhysicalNetwork.list(
                                                 cls.api_client,
                                                 zoneid=cls.zone.id
                                                 )
        if isinstance(physical_networks, list):
            cls.physical_network = physical_networks[0]
        cls.services["netscaler_1"]["lbdevicecapacity"] = 3
        cls.netscaler_1 = NetScaler.add(
                                  cls.api_client,
                                  cls.services["netscaler_1"],
                                  physicalnetworkid=cls.physical_network.id
                                  )

        cls.services["netscaler_2"].pop("lbdevicecapacity")
        cls.services["netscaler_2"]["lbdevicededicated"] = True
        cls.netscaler_2 = NetScaler.add(
                                  cls.api_client,
                                  cls.services["netscaler_2"],
                                  physicalnetworkid=cls.physical_network.id
                                  )

        nw_service_providers = NetworkServiceProvider.list(
                                        cls.api_client,
                                        name='Netscaler',
                                        physicalnetworkid=cls.physical_network.id
                                        )
        if isinstance(nw_service_providers, list):
            netscaler_provider = nw_service_providers[0]

        if netscaler_provider.state != 'Enabled':
            response = NetworkServiceProvider.update(
                                          cls.api_client,
                                          id=netscaler_provider.id,
                                          state='Enabled'
                                          )
        cls.network_offering_dedicated = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering_dedicated"],
                                            conservemode=True
                                            )
        cls.network_offering_shared = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering_dedicated.update(cls.api_client, state='Enabled')
        cls.network_offering_shared.update(cls.api_client, state='Enabled')
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls._cleanup = [
                        cls.service_offering,
                        cls.network_offering_dedicated,
                        cls.network_offering_shared,
                        cls.netscaler_1,
                        cls.netscaler_2,
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
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.account_1 = Account.create(
                                     self.apiclient,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )
        self.account_2 = Account.create(
                                     self.apiclient,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )
        self.account_3 = Account.create(
                                     self.apiclient,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )
        self.cleanup = [self.account_1, self.account_2, self.account_3]
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            interval = list_configurations(
                                    self.apiclient,
                                    name='network.gc.interval'
                                    )
            wait = list_configurations(
                                    self.apiclient,
                                    name='network.gc.wait'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) + int(wait[0].value))
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advancedns"])
    def test_shared_to_dedicated_upgrade(self):
        """Test upgrade from shared LB isolation to dedicated LB isolation"""


        # Validate the following
        # 1. Create a dedicated and shared network offering
        # 2. Configure 2 instances of Netscaler one with dedicated and other
        #    shared mode
        # 3. Deploy instance with shared network offering in account 1. create
        #    LB rules
        # 4. Deploy instance with shared network offering in account 2. create
        #   LB rules
        # 5. Deploy instance with dedicated network offering in account 3.
        #   Create Lb rules.
        # 6. Configure another instace of netscaler in dedicated mode
        # 7. upgrade networkj for user 1 to dedicated network offering.
        #    Create LB rules. LB rule creation should be successful

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering_shared.id)
        self.network_1 = Network.create(
                            self.apiclient,
                            self.services["network"],
                            accountid=self.account_1.name,
                            domainid=self.account_1.domainid,
                            networkofferingid=self.network_offering_shared.id,
                            zoneid=self.zone.id
                            )
        self.debug("Created network with ID: %s" % self.network_1.id)

        self.debug("Deploying VM in account: %s" % self.account_1.name)

        # Spawn an instance in that network
        virtual_machine_1 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_1.name,
                                  domainid=self.account_1.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_1.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network_1.id)
        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=virtual_machine_1.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % virtual_machine_1.id
            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Running",
                            "VM state should be running after deployment"
                        )

        # Creating network using the network offering created
        self.debug("Trying to create network with network offering: %s" %
                                                    self.network_offering_shared.id)
        self.network_2 = Network.create(
                            self.apiclient,
                            self.services["network"],
                            accountid=self.account_2.name,
                            domainid=self.account_2.domainid,
                            networkofferingid=self.network_offering_shared.id,
                            zoneid=self.zone.id
                            )
        self.debug("Created network with ID: %s" % self.network_2.id)

        self.debug("Deploying VM in account: %s" % self.account_2.name)

        # Spawn an instance in that network
        virtual_machine_2 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_2.name,
                                  domainid=self.account_2.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_2.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network_2.id)
        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=virtual_machine_2.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % virtual_machine_2.id
            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Running",
                            "VM state should be running after deployment"
                        )
        # Creating network using the network offering created
        self.debug("Trying to create network with network offering: %s" %
                                                    self.network_offering_dedicated.id)
        self.network_3 = Network.create(
                            self.apiclient,
                            self.services["network"],
                            accountid=self.account_3.account.name,
                            domainid=self.account_3.account.domainid,
                            networkofferingid=self.network_offering_dedicated.id,
                            zoneid=self.zone.id
                        )
        self.debug("Created network with ID: %s" % self.network_3.id)
        self.debug("Deploying VM in account: %s" % self.account_3.account.name)

        # Spawn an instance in that network
        virtual_machine_3 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_3.account.name,
                                  domainid=self.account_3.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_3.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network_3.id)
        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=virtual_machine_3.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % virtual_machine_3.id
            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Running",
                            "VM state should be running after deployment"
                        )

        self.debug("Stopping all VMs in account: %s" % self.account_3.account.name)
        virtual_machine_3.stop(self.apiclient)

        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=virtual_machine_3.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % virtual_machine_3.id
            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Stopped",
                            "VM state should be stopped"
                        )
        self.debug("All user VMs stopped")
        self.debug("Upgrading the network: %s" % self.network_3.id)
        self.network_3.update(
                            self.apiclient,
                            networkofferingid=self.network_offering_shared.id,
                            changecidr=True
                            )
        networks = Network.list(
                                self.apiclient,
                                id=self.network_3.id,
                                listall=True
                                )
        self.assertEqual(
                isinstance(networks, list),
                True,
                "List Networks should return a valid list for given network ID"
                )
        self.assertNotEqual(
                            len(networks),
                            0,
                            "Length of list networks should not be 0"
                            )
        network = networks[0]
        self.assertEqual(
                        network.networkofferingid,
                        self.network_offering_shared.id,
                        "Network offering ID should match with new offering ID"
                        )
        self.debug("Starting instances in account: %s" % self.account_3.account.name)
        virtual_machine_3.start(self.apiclient)

        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=virtual_machine_3.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % virtual_machine_3.id
            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Running",
                            "VM state should be running after deployment"
                        )
        try:
            self.debug(
                "Associating public Ip to the network: %s" %
                                                        self.network_3.name)

            public_ip = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account_3.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account_3.account.domainid,
                                networkid=self.network_3.id
                                )
            self.debug(
            "Creating LB rule for IP address: %s with round robin algo" %
                                                public_ip.ipaddress.ipaddress)

            lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=public_ip.ipaddress.id,
                                    accountid=self.account_3.account.name,
                                    networkid=self.network_3.id
                                )
            self.debug("Created the load balancing rule for public IP: %s" %
                                                public_ip.ipaddress.ipaddress)
        except Exception as e:
            self.fail("Failed to create load balancing rule - %s" % e)
        return
