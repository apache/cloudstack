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

""" P1 tests for brocade
"""
#Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.cloudstackAPI import (listPhysicalNetworks,
                                  listNetworkServiceProviders,
                                  addNetworkServiceProvider,
                                  addBrocadeVcsDevice,
                                  updateNetworkServiceProvider,
                                  deleteBrocadeVcsDevice)
from marvin.lib.utils import (cleanup_resources)
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             NetworkOffering,
                             Configurations,
                             Network)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
import time

class Services:
    """Test brocade plugin
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
                                    "memory": 128,       # In MBs
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
                         "network_offering": {
                                    "name": 'Brocade',
                                    "displaytext": 'Brocade',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'SourceNat,Connectivity',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "serviceProviderList": {
                                            "SourceNat": 'VirtualRouter',
                                            "Connectivity" : 'BrocadeVcs'
                                    },
                         },
                         "network": {
                                  "name": "Brocade",
                                  "displaytext": "Brocade",
                         },
                         "ostype": 'CentOS 5.3 (64-bit)',
                         # Cent OS 5.3 (64 bit)
                         "sleep": 60,
                         "timeout": 10
                    }

class TestBrocadeVcs(cloudstackTestCase):


    @classmethod
    def setUpClass(cls):
        cls._cleanup = []
        cls.testClient = super(TestBrocadeVcs, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                           )
        try:
            cls.brocadeDeviceData = cls.config.__dict__["brocadeDeviceData"].__dict__
            assert cls.brocadeDeviceData["ipaddress"], "ipaddress of brocade device\
                    not present in config file"
            assert cls.brocadeDeviceData["username"], "username of brocade device\
                    not present in config file"
        except Exception as e:
            raise unittest.SkipTest("Exception occured while reading\
                    brocade device data from config file: %s" % e)
        try:

           """ Adds Brocade device and enables NS provider"""
           cmd = listPhysicalNetworks.listPhysicalNetworksCmd()
           cmd.zoneid = cls.zone.id
           physical_networks = cls.api_client.listPhysicalNetworks(cmd)
           if isinstance(physical_networks, list):
               physical_network = physical_networks[0]
           cmd = listNetworkServiceProviders.listNetworkServiceProvidersCmd()
           cmd.name = 'BrocadeVcs'
           cmd.physicalnetworkid = physical_network.id
           nw_service_providers = cls.api_client.listNetworkServiceProviders(cmd)

           if isinstance(nw_service_providers, list):
               brocade_provider = nw_service_providers[0]
           else:
               cmd1 = addNetworkServiceProvider.addNetworkServiceProviderCmd()
               cmd1.name = 'BrocadeVcs'
               cmd1.physicalnetworkid = physical_network.id
               brocade_provider = cls.api_client.addNetworkServiceProvider(cmd1)

           cmd2 = addBrocadeVcsDevice.addBrocadeVcsDeviceCmd()
           cmd2.physicalnetworkid = physical_network.id
           cmd2.username = cls.brocadeDeviceData["username"]
           cmd2.password = cls.brocadeDeviceData["password"]
           cmd2.hostname = cls.brocadeDeviceData["ipaddress"]
           cls.brocade = cls.api_client.addBrocadeVcsDevice(cmd2)

           if brocade_provider.state != 'Enabled':
               cmd = updateNetworkServiceProvider.updateNetworkServiceProviderCmd()
               cmd.id = brocade_provider.id
               cmd.state = 'Enabled'
               cls.api_client.updateNetworkServiceProvider(cmd)


           cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
           cls._cleanup.append(cls.network_offering)

           # Enable Network offering
           cls.network_offering.update(cls.api_client, state='Enabled')
           cls.services["virtual_machine"]["zoneid"] = cls.zone.id
           cls.services["virtual_machine"]["template"] = cls.template.id
           cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
           cls._cleanup.append(cls.service_offering)
        except Exception as e:
           cls.tearDownClass()
           raise Exception ("Warning: Exception in setUpClass: %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cmd = deleteBrocadeVcsDevice.deleteBrocadeVcsDeviceCmd()
            cmd.vcsdeviceid = cls.brocade.vcsdeviceid
            cls.brocade = cls.api_client.deleteBrocadeVcsDevice(cmd)
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

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
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            #Clean up, terminate the created network offerings
            #cleanup_resources(self.apiclient, self.cleanup)
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
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return


    @attr(tags = ["advancedns"])
    def test_network_vcs(self):
        """Test Brocade Network and VM Creation
        """


        # Validate the following
        # 1. Deploy the first VM using a network from the above created
        #    Network offering.
        # 2. Deploy another VM.
        # 3. Delete The VMs and the Network

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        self.network = Network.create(
                                    self.apiclient,
                                    self.services["network"],
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        virtual_machine_1 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network.id)
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

        self.debug("Deploying another VM in account: %s" %
                                            self.account.name)

        # Spawn an instance in that network
        virtual_machine_2 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network.id)
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

        # Deleting a single VM
        VirtualMachine.delete(virtual_machine_1, self.apiclient, expunge=True)


        # Deleting a single VM
        VirtualMachine.delete(virtual_machine_2, self.apiclient, expunge=True)


        # Delete Network
        Network.delete(self.network, self.apiclient)

        return

