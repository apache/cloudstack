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

""" P1 tests for netscaler configurations
"""
#Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              random_gen)
from marvin.lib.base import (VirtualMachine,
                             NetworkServiceProvider,
                             PublicIPAddress,
                             Account,
                             Network,
                             NetScaler,
                             LoadBalancerRule,
                             NetworkOffering,
                             ServiceOffering,
                             PhysicalNetwork,
                             Configurations)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               add_netscaler)
from marvin.sshClient import SshClient
import time

class _NetScalerBase(cloudstackTestCase):
    """Base class for NetScaler tests in this file"""
    @classmethod
    def tearDownClass(cls):
        """Generic class tear-down method"""
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        """Generic test-case setup method"""
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        """Generic test-case tear-down method"""
        try:
            self.debug("Cleaning up the resources")
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    @classmethod
    def _addNetScaler(cls, netscaler_config):
        """Helper method for common Netscaler add procedure"""
        physical_networks = PhysicalNetwork.list(
                                                 cls.api_client,
                                                 zoneid=cls.zone.id
                                                 )
        if isinstance(physical_networks, list):
            cls.physical_network = physical_networks[0]

        # Check if a NetScaler network service provider exists - if not add one
        nw_service_providers = NetworkServiceProvider.list(
                                        cls.api_client,
                                        name='Netscaler',
                                        physicalnetworkid=cls.physical_network.id
                                        )
        if not isinstance(nw_service_providers, list) or len(nw_service_providers) < 1:
            NetworkServiceProvider.add(cls.api_client,
                                       name='Netscaler',
                                       physicalnetworkid=cls.physical_network.id,
                                       servicelist=["Lb"]
                                       )

        cls.netscaler = NetScaler.add(
                                  cls.api_client,
                                  netscaler_config,
                                  physicalnetworkid=cls.physical_network.id
                                  )

        nw_service_providers = NetworkServiceProvider.list(
                                        cls.api_client,
                                        name='Netscaler',
                                        physicalnetworkid=cls.physical_network.id
                                        )
        if isinstance(nw_service_providers, list):
            cls.netscaler_provider = nw_service_providers[0]

        if cls.netscaler_provider.state != 'Enabled':
            NetworkServiceProvider.update(
                                          cls.api_client,
                                          id=cls.netscaler_provider.id,
                                          state='Enabled'
                                          )

class _NetScalerAddBase(_NetScalerBase):
    """Base class for tests that add a NetScaler (valid & invalid)
       Provides standard Setup / TearDown methods"""
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(_NetScalerAddBase, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        physical_networks = PhysicalNetwork.list(
                                                 cls.api_client,
                                                 zoneid=cls.zone.id
                                                 )
        assert isinstance(physical_networks, list), "There should be atleast one physical network for advanced zone"
        cls.physical_network = physical_networks[0]

        # Check if a NetScaler network service provider exists - if not add one
        nw_service_providers = NetworkServiceProvider.list(
                                        cls.api_client,
                                        name='Netscaler',
                                        physicalnetworkid=cls.physical_network.id
                                        )
        if not isinstance(nw_service_providers, list) or len(nw_service_providers) < 1:
            NetworkServiceProvider.add(cls.api_client,
                                       name='Netscaler',
                                       physicalnetworkid=cls.physical_network.id,
                                       servicelist=["Lb"]
                                       )

        cls._cleanup = []

class TestAddNetScaler(_NetScalerAddBase):

    @attr(tags=["advancedns"], required_hardware="true")
    def test_add_netscaler_device(self):
        """Test add netscaler device
        """
        # Validate the following
        # 1. Add Netscaler device into a Zone by providing valid log in
        #    credentials , public , private interface and enabling Load
        #    Balancing feature.
        # 2. Netscaler should be configured successfully.
        self.debug("Adding netscaler device: %s" %
                                    self.services["configurableData"]["netscaler"]["ipaddress"])
        netscaler_config = dict(self.services["configurableData"]["netscaler"])
        netscaler_config.update({'lbdevicededicated': "False"})
        netscaler = NetScaler.add(
                                  self.apiclient,
                                  netscaler_config,
                                  physicalnetworkid=self.physical_network.id
                                  )
        self.cleanup.append(netscaler)
        self.debug("Checking if Netscaler network service provider is enabled?")

        nw_service_providers = NetworkServiceProvider.list(
                                        self.apiclient,
                                        name='Netscaler',
                                        physicalnetworkid=self.physical_network.id
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
                                 lbdeviceid=netscaler.lbdeviceid
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

class TestInvalidParametersNetscaler(_NetScalerAddBase):

    @attr(tags=["advancedns"], required_hardware="true")
    def test_invalid_cred(self):
        """Test add netscaler device with invalid credential
        """

        # Validate the following
        # 1. Add Netscaler device into a Zone by providing invalid log in
        #    credentials , but valid public, private interface
        # 2. Netscaler API should throw error

        self.debug("Passing invalid credential for NetScaler")
        netscaler_config = dict(self.services["configurableData"]["netscaler"])
        netscaler_config.update({'username': random_gen(), 'password': random_gen()})
        self.debug("Adding netscaler device: %s" %
                                    netscaler_config["ipaddress"])

        self.debug("Username: %s, password: %s" % (
                                    netscaler_config["username"],
                                    netscaler_config["password"]
                                    ))

        with self.assertRaises(Exception):
            NetScaler.add(
                          self.apiclient,
                          netscaler_config,
                          physicalnetworkid=self.physical_network.id
                         )

    @attr(tags=["advancedns"], required_hardware="true")
    def test_invalid_public_interface(self):
        """Test add netscaler device with invalid public interface
        """

        # Validate the following
        # 1. Add Netscaler device into a Zone by providing valid log in
        #    credentials , private interface and invalid public interface
        # 2. Netscaler API should throw error

        self.debug("Passing invalid public interface for NetScaler")
        netscaler_config = dict(self.services["configurableData"]["netscaler"])
        netscaler_config.update({'publicinterface': random_gen()})
        self.debug("Adding netscaler device: %s" %
                                    netscaler_config["ipaddress"])

        self.debug("Public interface: %s" %
                                netscaler_config["publicinterface"])

        with self.assertRaises(Exception):
            NetScaler.add(
                          self.apiclient,
                          netscaler_config,
                          physicalnetworkid=self.physical_network.id
                         )

    @attr(tags=["advancedns"], required_hardware="true")
    def test_invalid_private_interface(self):
        """Test add netscaler device with invalid private interface
        """

        # Validate the following
        # 1. Add Netscaler device into a Zone by providing valid log in
        #    credentials , public interface and invalid private interface
        # 2. Netscaler API should throw error
        netscaler_config = dict(self.services["configurableData"]["netscaler"])
        netscaler_config.update({'privateinterface': random_gen()})
        self.debug("Adding netscaler device: %s" %
                                    netscaler_config["ipaddress"])

        self.debug("Private interface: %s" %
                                netscaler_config["privateinterface"])

        with self.assertRaises(Exception):
            NetScaler.add(
                          self.apiclient,
                          netscaler_config,
                          physicalnetworkid=self.physical_network.id
                         )

class _NetScalerDeployVMBase(_NetScalerBase):
    """Base class for testing VM deployment using NetScaler networking
       Provides standard test-case setup / tear-down methods"""

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
        self.cleanup = [self.account_1, self.account_2]

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

class TestNetScalerDedicated(_NetScalerDeployVMBase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestNetScalerDedicated, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )

        netscaler_config = dict(cls.services["configurableData"]["netscaler"])
        netscaler_config.update({'lbdevicededicated': "True"})
        cls._addNetScaler(netscaler_config)

        nw_offering_config = dict(cls.services["nw_off_isolated_netscaler"])
        nw_offering_config.update({"serviceCapabilityList": {
                                       "SourceNat": {
                                           "SupportedSourceNatTypes": "peraccount"
                                       },
                                       "lb": {
                                           "SupportedLbIsolation": "dedicated"
                                       }
                                       }
                                  })

        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            nw_offering_config,
                                            conservemode=False
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls._cleanup = [
                        cls.service_offering,
                        cls.network_offering,
                        cls.netscaler,
                        ]

    @attr(tags = ["advancedns"])
    def test_netscaler_dedicated_mode(self):
        """Test netscaler device in dedicated mode
        """

        # Validate the following
        # 1. Add Netscaler device in dedicated mode.
        # 2. Netscaler should be configured successfully.It should be able to
        #    service only 1 account.

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
                         True,
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
                                    self.services["netscaler_network"],
                                    accountid=self.account_1.name,
                                    domainid=self.account_1.domainid,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account_1.name)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_1.name,
                                  domainid=self.account_1.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network.id)],
                                  templateid=self.template.id,
                                  zoneid=self.zone.id
                                  )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=virtual_machine.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % virtual_machine.id
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

        Network.create(
                            self.apiclient,
                            self.services["netscaler_network"],
                            accountid=self.account_2.name,
                            domainid=self.account_2.domainid,
                            networkofferingid=self.network_offering.id,
                            zoneid=self.zone.id
                        )
        self.debug("Deploying an instance in account: %s" % self.account_2.name)
        with self.assertRaises(Exception):
            VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_2.name,
                                  domainid=self.account_2.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network.id)],
                                  templateid=self.template.id,
                                  zoneid=self.zone.id
                                  )
            self.debug("Deply instance in dedicated Network offering mode failed")

class TestNetScalerShared(_NetScalerDeployVMBase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestNetScalerShared, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )

        netscaler_config = dict(cls.services["configurableData"]["netscaler"])
        netscaler_config.update({'lbdevicededicated': "False"})
        cls._addNetScaler(netscaler_config)

        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["nw_off_isolated_netscaler"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls._cleanup = [
                        cls.service_offering,
                        cls.network_offering,
                        cls.netscaler,
                        ]

    @attr(tags = ["advancedns"])
    def test_netscaler_shared_mode(self):
        """Test netscaler device in shared mode
        """

        # Validate the following
        # 1. Add Netscaler device in shared mode.
        # 2. Netscaler should be configured successfully.It should be able to
        #    service only 1 account.

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
                                    self.services["netscaler_network"],
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
                                  networkids=[str(self.network_1.id)],
                                  templateid=self.template.id,
                                  zoneid=self.zone.id
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
                                self.services["netscaler_network"],
                                accountid=self.account_2.name,
                                domainid=self.account_2.domainid,
                                networkofferingid=self.network_offering.id,
                                zoneid=self.zone.id
                                )
        self.debug("Created network with ID: %s" % self.network_1.id)

        self.debug("Deploying VM in account: %s" % self.account_2.name)

        # Spawn an instance in that network
        virtual_machine_2 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_2.name,
                                  domainid=self.account_2.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_2.id)],
                                  templateid=self.template.id,
                                  zoneid=self.zone.id
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

class _NetScalerCapacity(_NetScalerDeployVMBase):
    """Base class for NetScaler capacity tests
       Provides standard test-case setup / tear-down method"""
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

class TestNetScalerCustomCapacity(_NetScalerCapacity):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestNetScalerCustomCapacity, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )

        netscaler_config = dict(cls.services["configurableData"]["netscaler"])
        netscaler_config.update({'lbdevicecapacity': 3, 'lbdevicededicated': "False"})
        cls._addNetScaler(netscaler_config)

        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["nw_off_isolated_netscaler"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls._cleanup = [
                        cls.service_offering,
                        cls.network_offering,
                        cls.netscaler,
                        ]

    @attr(tags = ["advancedns","test"])
    def test_netscaler_custom_capacity(self):
        """Test netscaler device with custom capacity
        """

        # Validate the following
        # 1. Add Netscaler device in shared mode with capacity 3
        # 2. Netscaler should be configured successfully.It should be able to
        #    service only 3 accounts.

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
                                    self.services["netscaler_network"],
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
                                  networkids=[str(self.network_1.id)],
                                  templateid=self.template.id,
                                  zoneid=self.zone.id
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
                                self.services["netscaler_network"],
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
                                  networkids=[str(self.network_2.id)],
                                  templateid=self.template.id,
                                  zoneid=self.zone.id
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
                            self.services["netscaler_network"],
                            accountid=self.account_3.name,
                            domainid=self.account_3.domainid,
                            networkofferingid=self.network_offering.id,
                            zoneid=self.zone.id
                        )
        self.debug("Created network with ID: %s" % self.network_3.id)

        self.debug("Deploying VM in account: %s" % self.account_3.name)
        # Spawn an instance in that network
        virtual_machine_3 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_3.name,
                                  domainid=self.account_3.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_3.id)],
                                  templateid=self.template.id,
                                  zoneid=self.zone.id
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

class TestNetScalerNoCapacity(_NetScalerCapacity):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestNetScalerNoCapacity, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )

        netscaler_config = dict(cls.services["configurableData"]["netscaler"])
        netscaler_config.update({'lbdevicecapacity': 2, 'lbdevicededicated': "False"})
        cls._addNetScaler(netscaler_config)

        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["nw_off_isolated_netscaler"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls._cleanup = [
                        cls.service_offering,
                        cls.network_offering,
                        cls.netscaler,
                        ]

    @attr(tags = ["advancedns","test"])
    def test_netscaler_no_capacity(self):
        """Test netscaler device with no capacity remaining
        """

        # Validate the following
        # 1. Add Netscaler device in shared mode with capacity 2
        # 2. Netscaler should be configured successfully.It should be able to
        #    service only 2 accounts.
        # 3. Deploy instance for account 3 should fail

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
                                    self.services["netscaler_network"],
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
                                  networkids=[str(self.network_1.id)],
                                  templateid=self.template.id,
                                  zoneid=self.zone.id
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
                                self.services["netscaler_network"],
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
                                  networkids=[str(self.network_2.id)],
                                  templateid=self.template.id,
                                  zoneid=self.zone.id
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
                            self.services["netscaler_network"],
                            accountid=self.account_3.name,
                            domainid=self.account_3.domainid,
                            networkofferingid=self.network_offering.id,
                            zoneid=self.zone.id
                        )
        self.debug("Created network with ID: %s" % self.network_3.id)

        self.debug("Deploying VM in account: %s" % self.account_3.name)
        with self.assertRaises(Exception):
            # Spawn an instance in that network
            VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_3.name,
                                  domainid=self.account_3.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_3.id)],
                                  templateid=self.template.id,
                                  zoneid=self.zone.id
                                  )

class TestGuestNetworkWithNetScaler(_NetScalerDeployVMBase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestGuestNetworkWithNetScaler, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )

        netscaler_config = dict(cls.services["configurableData"]["netscaler"])
        netscaler_config.update({'lbdevicededicated': "False"})
        cls._addNetScaler(netscaler_config)

        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["nw_off_isolated_netscaler"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls._cleanup = [
                        cls.service_offering,
                        cls.network_offering,
                        cls.netscaler,
                        ]


    @attr(tags = ["advancedns","test"])
    def test_01_guest_network(self):
        """Implementing Guest Network when first VM gets deployed using the network having Netscaler as LB
        """

        # Validate the following
        # 1. Configure Netscaler for load balancing.
        # 2. Create a Network offering with LB services provided by Netscaler
        #    and all other services by VR.
        # 3.Create a new account/user.
        # 4. Deploy the first VM using a network from the above created
        #    Network offering.
        # In Netscaler:
        # 1. Private interface of Netscaler device will be configured to make
        #    it part of the virtual guest network by binding the interface to
        #    the VLAN and subnet allocated for the virtual guest network
        # 2. Private interface should be associated with a self-ip (second IP
        #    in the subnet) from the guest subnet.

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
                                    self.services["netscaler_network"],
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
                                  networkids=[str(self.network_1.id)],
                                  templateid=self.template.id,
                                  zoneid=self.zone.id
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
        # Find Network vlan used
        network_list = Network.list(
                                    self.apiclient,
                                    id=self.network_1.id,
                                    listall=True
                                    )
        nw = network_list[0]
        self.debug("SSH into netscaler: %s" %
                                    self.services["configurableData"]["netscaler"]["ipaddress"])
        try:
            ssh_client = SshClient(
                                    self.services["configurableData"]["netscaler"]["ipaddress"],
                                    self.services["configurableData"]["netscaler"]["port"],
                                    self.services["configurableData"]["netscaler"]["username"],
                                    self.services["configurableData"]["netscaler"]["password"],
                                    )
            cmd = "show vlan %s" % (nw.vlan)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)
            # Output: ERROR: No such resource [id, 123]

            self.assertNotEqual(
                    result.count("ERROR: No such resource "),
                    1,
                    "Netscaler should have vlan configured for the network"
                    )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["configurableData"]["netscaler"]["ipaddress"], e))
        return

    @attr(tags = ["advancedns","test"])
    def test_02_guest_network_multiple(self):
        """Implementing Guest Network when multiple VMs gets deployed using the network having Netscaler as LB
        """

        # Validate the following
        # 1. Configure Netscaler for load balancing.
        # 2. Create a Network offering with LB services provided by Netscaler
        #    and all other services by VR.
        # 3.Create a new account/user.
        # 4. Deploy the first VM using a network from the above created
        #    Network offering.
        # In Netscaler:
        # 1. Private interface of Netscaler device will be configured to make
        #    it part of the virtual guest network by binding the interface to
        #    the VLAN and subnet allocated for the virtual guest network
        # 2. Private interface should be associated with a self-ip (second IP
        #    in the subnet) from the guest subnet.

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        self.network_1 = Network.create(
                                    self.apiclient,
                                    self.services["netscaler_network"],
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
                                  networkids=[str(self.network_1.id)],
                                  templateid=self.template.id,
                                  zoneid=self.zone.id
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
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        self.network_2 = Network.create(
                                    self.apiclient,
                                    self.services["netscaler_network"],
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
                                  networkids=[str(self.network_2.id)],
                                  templateid=self.template.id,
                                  zoneid=self.zone.id
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
        self.debug("SSH into netscaler: %s" %
                                    self.services["configurableData"]["netscaler"]["ipaddress"])
        try:
            # Find Network vlan used
            network_list = Network.list(
                                    self.apiclient,
                                    id=self.network_1.id,
                                    listall=True
                                    )
            nw = network_list[0]
            ssh_client = SshClient(
                                    self.services["configurableData"]["netscaler"]["ipaddress"],
                                    self.services["configurableData"]["netscaler"]["port"],
                                    self.services["configurableData"]["netscaler"]["username"],
                                    self.services["configurableData"]["netscaler"]["password"],
                                    )
            cmd = "show vlan %s" % (nw.vlan)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)
            # Output: ERROR: No such resource [id, 123]

            self.assertNotEqual(
                    result.count("ERROR: No such resource "),
                    1,
                    "Netscaler should have vlan configured for the network"
                    )

            # Find Network vlan used
            network_list = Network.list(
                                    self.apiclient,
                                    id=self.network_2.id,
                                    listall=True
                                    )
            nw = network_list[0]
            cmd = "show vlan %s" % (nw.vlan)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)
            # Output: ERROR: No such resource [id, 123]

            self.assertNotEqual(
                    result.count("ERROR: No such resource"),
                    1,
                    "Netscaler should have vlan configured for the network"
                    )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["configurableData"]["netscaler"]["ipaddress"], e))
        return

    @attr(tags = ["advancedns","test"])
    def test_03_delete_account(self):
        """Delete an account that has LB rules
        """

        # Validate the following
        # 1. Acquire an ipaddress. Create multiple Lb rules on this ip address
        # 2. Delete this account that has LB rules
        # In Netscaler:
        # 1. Private interface on the netscaler LB device will be unbound to
        #    vlan and subnet
        # 2. All the service instance and the servers that are part of this
        #    vlan, that were created on the Netscaler device as part of
        #    applying LB rules will be destroyed.
        # 3. Any lb virtual server that is created using this public IP
        #    allocated for the account will be destroyed

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        self.network = Network.create(
                                    self.apiclient,
                                    self.services["netscaler_network"],
                                    accountid=self.account_1.name,
                                    domainid=self.account_1.domainid,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account_1.name)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account_1.name,
                                  domainid=self.account_1.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network.id)],
                                  templateid=self.template.id,
                                  zoneid=self.zone.id
                                  )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=virtual_machine.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % virtual_machine.id
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
        self.debug("Assigning public IP for the account: %s" %
                                                self.account_1.name)
        public_ip = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account_1.name,
                                zoneid=self.zone.id,
                                domainid=self.account_1.domainid,
                                networkid=self.network.id
                                )
        self.debug(
            "Creating LB rule for IP address: %s with round robin algo" %
                                            public_ip.ipaddress.ipaddress)

        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=public_ip.ipaddress.id,
                                    accountid=self.account_1.name,
                                    networkid=self.network.id
                                )
        self.debug("Created the load balancing rule for public IP: %s" %
                                                public_ip.ipaddress.ipaddress)
        self.debug("Assigning VMs to LB rule")
        lb_rule.assign(self.apiclient, [virtual_machine])

        # Find Network vlan used
        network_list = Network.list(
                                    self.apiclient,
                                    id=self.network.id,
                                    listall=True
                                    )
        nw = network_list[0]

        self.debug("Deleting account: %s" % self.account_1.name)
        # This is a hack. Delete first account from cleanup list
        self.cleanup.pop(0).delete(self.apiclient)
        self.debug("Account: %s is deleted!" % self.account_1.name)

        self.debug("Waiting for network.gc.interval & network.gc.wait..")
        interval = Configurations.list(
                                    self.apiclient,
                                    name='network.gc.interval'
                                    )
        wait = Configurations.list(
                                    self.apiclient,
                                    name='network.gc.wait'
                                    )
        # Sleep to ensure that all resources are deleted
        time.sleep(int(interval[0].value) + int(wait[0].value))

        self.debug("SSH into netscaler: %s" %
                                    self.services["configurableData"]["netscaler"]["ipaddress"])
        try:
            ssh_client = SshClient(
                                    self.services["configurableData"]["netscaler"]["ipaddress"],
                                    self.services["configurableData"]["netscaler"]["port"],
                                    self.services["configurableData"]["netscaler"]["username"],
                                    self.services["configurableData"]["netscaler"]["password"],
                                    )
            cmd = "show vlan %s" % (nw.vlan)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)
            # Output: ERROR: No such resource [id, 123]

            self.assertEqual(
                    result.count("ERROR: No such resource"),
                    1,
                    "Netscaler should have vlan configured for the network"
                    )

            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                                        public_ip.ipaddress.ipaddress,
                                        lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)
            # Output: ERROR: No such resource [id, 123]

            self.assertEqual(
                    result.count("ERROR: No such resource"),
                    1,
                    "Netscaler should have vlan configured for the network"
                    )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["configurableData"]["netscaler"]["ipaddress"], e))
        return



class TestGuestNetworkShutDown(_NetScalerBase):

    @classmethod
    def setUpClass(cls):
        cls._cleanup = []
        cls.testClient = super(TestGuestNetworkShutDown, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        try:
           netscaler_config = dict(cls.services["configurableData"]["netscaler"])
           netscaler_config.update({'lbdevicededicated': "False"})
           cls.netscaler = add_netscaler(cls.api_client, cls.zone.id, netscaler_config)
           cls._cleanup.append(cls.netscaler)
           cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["nw_off_isolated_netscaler"],
                                            conservemode=True
                                            )
           # Enable Network offering
           cls.network_offering.update(cls.api_client, state='Enabled')

           cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
           cls.account = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     admin=True,
                                     domainid=cls.domain.id
                                     )
           cls._cleanup.insert(0,cls.account)
           # Creating network using the network offering created
           cls.network = Network.create(
                                    cls.api_client,
                                    cls.services["netscaler_network"],
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    networkofferingid=cls.network_offering.id,
                                    zoneid=cls.zone.id
                                    )

           # Spawn few instances in that network
           cls.vm_1 = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering.id,
                                  networkids=[str(cls.network.id)],
                                  templateid=cls.template.id,
                                  zoneid=cls.zone.id
                                  )
           cls.vm_2 = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering.id,
                                  networkids=[str(cls.network.id)],
                                  templateid=cls.template.id,
                                  zoneid=cls.zone.id
                                  )
           cls.public_ip = PublicIPAddress.create(
                                cls.api_client,
                                accountid=cls.account.name,
                                zoneid=cls.zone.id,
                                domainid=cls.account.domainid,
                                networkid=cls.network.id
                                )
           cls.lb_rule = LoadBalancerRule.create(
                                    cls.api_client,
                                    cls.services["lbrule"],
                                    ipaddressid=cls.public_ip.ipaddress.id,
                                    accountid=cls.account.name,
                                    networkid=cls.network.id
                                )
           cls.lb_rule.assign(cls.api_client, [cls.vm_1, cls.vm_2])
        except Exception as e:
           cls.tearDownClass()
           raise Exception ("Warning: Exception in setUpClass: %s" % e)

    @attr(tags = ["advancedns","test"])
    def test_01_stop_all_vms(self):
        """Test Stopping all the Vms for any account that has LB rules.
        """

        # Validate the following
        # 1. Acquire IP address and create a load balancer rule
        # 2. Stop all VMs in the account that has LB rules
        # 3. This will result in the network being shutdown. As part of this,
        #    this account and the all the Lb rules for this account should get
        #    removed from the Netscaler

        # Find Network vlan used
        network_list = Network.list(
                                    self.apiclient,
                                    id=self.network.id,
                                    listall=True
                                    )
        nw = network_list[0]
        self.debug("Network vlan used is: %s" % nw.vlan)

        self.debug(
            "Stopping all the VM instances for the account: %s" %
                                                    self.account.name)

        try:
            self.vm_1.stop(self.apiclient)
            self.vm_2.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop instance: %s" % e)

        self.debug("Sleep for network.gc.interval + network.gc.wait")
        interval = Configurations.list(
                                    self.apiclient,
                                    name='network.gc.interval'
                                    )
        wait = Configurations.list(
                                    self.apiclient,
                                    name='network.gc.wait'
                                    )
        # Sleep to ensure that all resources are deleted
        time.sleep((int(interval[0].value) + int(wait[0].value)) * 2)

        self.debug("SSH into netscaler: %s" %
                                    self.services["configurableData"]["netscaler"]["ipaddress"])
        try:
            ssh_client = SshClient(
                                    self.services["configurableData"]["netscaler"]["ipaddress"],
                                    self.services["configurableData"]["netscaler"]["port"],
                                    self.services["configurableData"]["netscaler"]["username"],
                                    self.services["configurableData"]["netscaler"]["password"],
                                    )
            cmd = "show vlan %s" % (nw.vlan)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)
            # Output: ERROR: No such resource [id, 123]

            self.assertEqual(
                    result.count("ERROR: No such resource"),
                    1,
                    "Netscaler should not have vlan configured for the network"
                    )

            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                                        self.public_ip.ipaddress.ipaddress,
                                        self.lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)
            # Output: ERROR: No such resource [id, 123]

            self.assertEqual(
                    result.count("ERROR: No such resource"),
                    1,
                    "Netscaler should not have vserver configured for the network"
                    )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["configurableData"]["netscaler"]["ipaddress"], e))
        return

    @attr(tags = ["advancedns","test"])
    def test_02_start_one_vm(self):
        """Test LB rules on Netscaler after starting one Vm in account
        """

        # Validate the following
        # 1. Acquire IP address and create a load balancer rule
        # 2. Stop all VMs in the account that has LB rules
        # 3. This will result in the network being shutdown. As part of this,
        #    this account and the all the Lb rules for this account should get
        #    removed from the Netscaler
        # 3. Start one of the VMs. LB rules should get reconfigured on
        #    Netscaler

        self.debug(
            "starting one VM instances for the account: %s" %
                                                    self.account.name)
        self.vm_1.start(self.apiclient)

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=self.vm_1.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return a valid list"
                        )

        for vm in vms:
            self.assertEqual(
                        vm.state,
                        "Running",
                        "VM instance should be Up and running after start"
                        )

        self.debug("SSH into netscaler: %s" %
                                    self.services["configurableData"]["netscaler"]["ipaddress"])
        try:
            # Find Network vlan used
            network_list = Network.list(
                                    self.apiclient,
                                    id=self.network.id,
                                    listall=True
                                    )
            nw = network_list[0]
            ssh_client = SshClient(
                                    self.services["configurableData"]["netscaler"]["ipaddress"],
                                    self.services["configurableData"]["netscaler"]["port"],
                                    self.services["configurableData"]["netscaler"]["username"],
                                    self.services["configurableData"]["netscaler"]["password"],
                                    )
            cmd = "show vlan %s" % (nw.vlan)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)
            # Output: ERROR: No such resource [id, 123]

            self.assertNotEqual(
                    result.count("ERROR: No such resource "),
                    1,
                    "Netscaler should have vlan configured for the network"
                    )

            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                                        self.public_ip.ipaddress.ipaddress,
                                        self.lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)
            # Output: ERROR: No such resource [id, 123]

            self.assertNotEqual(
                    result.count("ERROR: No such resource"),
                    1,
                    "Netscaler should have vlan configured for the network"
                    )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["configurableData"]["netscaler"]["ipaddress"], e))
        return

    @attr(tags = ["advancedns","test"])
    def test_03_network_restart_without_cleanup(self):
        """Test LB rules on Netscaler after network restart without cleanup
        """

        # Validate the following
        # 1. Acquire IP address and create a load balancer rule
        # 2. Restart network->without cleanup option enabled
        # 3. All existing Lb rules get added again to the netscaler. All the
        #    existing LB rules should continue to work.

        self.debug("Restarting the network: %s" % self.network.id)
        self.network.restart(self.apiclient, cleanup=False)

        self.debug("SSH into netscaler: %s" %
                                    self.services["configurableData"]["netscaler"]["ipaddress"])
        try:
            # Find Network vlan used
            network_list = Network.list(
                                    self.apiclient,
                                    id=self.network.id,
                                    listall=True
                                    )
            nw = network_list[0]
            ssh_client = SshClient(
                                    self.services["configurableData"]["netscaler"]["ipaddress"],
                                    self.services["configurableData"]["netscaler"]["port"],
                                    self.services["configurableData"]["netscaler"]["username"],
                                    self.services["configurableData"]["netscaler"]["password"],
                                    )
            cmd = "show vlan %s" % (nw.vlan)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)
            # Output: ERROR: No such resource [id, 123]

            self.assertNotEqual(
                    result.count("ERROR: No such resource "),
                    1,
                    "Netscaler should have vlan configured for the network"
                    )

            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                                        self.public_ip.ipaddress.ipaddress,
                                        self.lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)
            # Output: ERROR: No such resource [id, 123]

            self.assertNotEqual(
                    result.count("ERROR: No such resource"),
                    1,
                    "Netscaler should have vlan configured for the network"
                    )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["configurableData"]["netscaler"]["ipaddress"], e))
        return

    @attr(tags = ["advancedns","test"])
    def test_04_network_restart_with_cleanup(self):
        """Test LB rules on Netscaler after network restart with cleanup
        """

        # Validate the following
        # 1. Acquire IP address and create a load balancer rule
        # 2. Restart network->with cleanup option enabled
        # 3. All existing Lb rules get deleted and reconfigured again to the
        #    netscaler. All the existing LB rules should continue to work.

        self.debug("Restarting the network: %s" % self.network.id)
        self.network.restart(self.apiclient, cleanup=True)

        self.debug("SSH into netscaler: %s" %
                                    self.services["configurableData"]["netscaler"]["ipaddress"])
        try:
            # Find Network vlan used
            network_list = Network.list(
                                    self.apiclient,
                                    id=self.network.id,
                                    listall=True
                                    )
            nw = network_list[0]
            ssh_client = SshClient(
                                    self.services["configurableData"]["netscaler"]["ipaddress"],
                                    self.services["configurableData"]["netscaler"]["port"],
                                    self.services["configurableData"]["netscaler"]["username"],
                                    self.services["configurableData"]["netscaler"]["password"],
                                    )
            cmd = "show vlan %s" % (nw.vlan)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)
            # Output: ERROR: No such resource [id, 123]

            self.assertNotEqual(
                    result.count("ERROR: No such resource "),
                    1,
                    "Netscaler should have vlan configured for the network"
                    )

            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                                        self.public_ip.ipaddress.ipaddress,
                                        self.lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)
            # Output: ERROR: No such resource [id, 123]

            self.assertNotEqual(
                    result.count("ERROR: No such resource"),
                    1,
                    "Netscaler should have vlan configured for the network"
                    )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["configurableData"]["netscaler"]["ipaddress"], e))
        return



class TestServiceProvider(_NetScalerDeployVMBase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestServiceProvider, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )

        netscaler_config = dict(cls.services["configurableData"]["netscaler"])
        netscaler_config.update({'lbdevicededicated': "False"})
        cls._addNetScaler(netscaler_config)

        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["nw_off_isolated_netscaler"],
                                            conservemode=True
                                            )

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls._cleanup = [
                        cls.service_offering,
                        cls.network_offering,
                        cls.netscaler,
                        ]

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.account = Account.create(
                                     self.apiclient,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )
        self.cleanup = [self.account]

    @attr(tags = ["advancedns","test"])
    def test_01_create_nw_off_disabled(self):
        """Test create network with network offering disabled
        """

        # Validate the following
        # 1. Configure Netscaler for load balancing.
        # 2. Create a Network offering. Do not enable the network offering
        # 3. Try to create a network with this network offering.
        # 4. Network creation should fail

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
        with self.assertRaises(Exception):
            Network.create(
                            self.apiclient,
                            self.services["netscaler_network"],
                            accountid=self.account.name,
                            domainid=self.account.domainid,
                            networkofferingid=self.network_offering.id,
                            zoneid=self.zone.id
                            )
        return

    @attr(tags = ["advancedns","test"])
    def test_02_create_nw_sp_disabled(self):
        """Test create network when service provider is disabled
        """

        # Validate the following
        # 1. Configure Netscaler for load balancing.
        # 2. Disable the service provider. Create a Network offering.
        # 3. Try to create a network. Network creation should fail

        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')

        # Disable the service provider
        NetworkServiceProvider.update(
                                      self.apiclient,
                                      id=self.netscaler_provider.id,
                                      state='Disabled'
                                    )
        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        with self.assertRaises(Exception):
            Network.create(
                            self.apiclient,
                            self.services["netscaler_network"],
                            accountid=self.account.name,
                            domainid=self.account.domainid,
                            networkofferingid=self.network_offering.id,
                            zoneid=self.zone.id
                            )
        return

    @attr(tags = ["advancedns","test"])
    def test_03_create_lb_sp_disabled(self):
        """Test create LB rules when service provider is disabled
        """

        # Validate the following
        # 1. Configure Netscaler for load balancing.
        # 2. Create a Network offering. Create instances and acquire public Ip
        # 3. Disabled service provider and again try to create LB rules
        # 4.Deploy VM should fail

        # Enable the service provider
        NetworkServiceProvider.update(
                                      self.apiclient,
                                      id=self.netscaler_provider.id,
                                      state='Enabled'
                                    )
        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        self.network = Network.create(
                            self.apiclient,
                            self.services["netscaler_network"],
                            accountid=self.account.name,
                            domainid=self.account.domainid,
                            networkofferingid=self.network_offering.id,
                            zoneid=self.zone.id
                            )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network.id)],
                                  templateid=self.template.id,
                                  zoneid=self.zone.id
                                  )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=virtual_machine.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % virtual_machine.id
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
        self.debug("Acquiring a public IP for Network: %s" % self.network.name)
        public_ip = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=self.network.id
                                )
        self.debug(
            "Creating LB rule for IP address: %s with round robin algo" %
                                            public_ip.ipaddress.ipaddress)

        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=public_ip.ipaddress.id,
                                    accountid=self.account.name,
                                    networkid=self.network.id
                                )
        self.debug("Created the load balancing rule for public IP: %s" %
                                                public_ip.ipaddress.ipaddress)

        self.debug("Assigning VM instance: %s to LB rule: %s" % (
                                                    virtual_machine.name,
                                                    lb_rule.name
                                                    ))
        lb_rule.assign(self.apiclient, [virtual_machine])
        self.debug("Assigned VM instance: %s to lb rule: %s" % (
                                                    virtual_machine.name,
                                                    lb_rule.name
                                                    ))
        self.debug("Disabling Netscaler service provider")

        # Disable the service provider
        NetworkServiceProvider.update(
                                      self.apiclient,
                                      id=self.netscaler_provider.id,
                                      state='Disabled'
                                    )
        with self.assertRaises(Exception):
            self.debug("Deploying VM in the network: %s" % self.network.id)
            LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=public_ip.ipaddress.id,
                                    accountid=self.account.name,
                                    networkid=self.network.id
                                )
            VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network.id)],
                                  templateid=self.template.id,
                                  zoneid=self.zone.id
                                  )

class TestDeleteNetscaler(_NetScalerDeployVMBase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestDeleteNetscaler, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )

        netscaler_config = dict(cls.services["configurableData"]["netscaler"])
        netscaler_config.update({'lbdevicededicated': "False"})
        cls._addNetScaler(netscaler_config)

        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["nw_off_isolated_netscaler"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls._cleanup = [
                        cls.service_offering,
                        cls.network_offering,
                        cls.netscaler,
                        ]

    @attr(tags = ["advancedns","test"])
    def test_delete_netscaler_with_lb(self):
        """Test delete Netscaler when active LB rules are present
        """

        # Validate the following
        # 1. Configure Netscaler for load balancing.
        # 2. Create a Network offering with LB services provided by Netscaler
        #    and all other services by VR.
        # 3.Create a new account/user.
        # 4. Deploy the first VM using a network from the above created
        #    Network offering.
        # 5. Attempt to delete Netscaler load balancer from zone.
        #    Deletion should NOT be allowed.

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
                                    self.services["netscaler_network"],
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
                                  networkids=[str(self.network_1.id)],
                                  templateid=self.template.id,
                                  zoneid=self.zone.id
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
        self.debug("Attempt to delete netscaler load balancer device")
        with self.assertRaises(Exception):
            self.netscaler.delete(self.apiclient)
        self.debug("Attempt to delete Netscaler device failed!")
