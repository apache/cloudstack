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

""" P1 tests for NuageVsp network Plugin
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (listPhysicalNetworks,
                                  listNetworkServiceProviders,
                                  addNetworkServiceProvider,
                                  addNuageVspDevice)
from marvin.lib.utils import (cleanup_resources)
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             NetworkOffering,
                             Network)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)


class Services:

    """Test NuageVsp plugin
    """

    def __init__(self):
        self.services = {
            "account": {
                "email": "cloudstack@cloudmonkey.com",
                "firstname": "cloudstack",
                "lastname": "bob",
                "username": "bobbuilder",
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
            "nuage_vsp_device": {
                "hostname": '192.168.0.7',
                "username": 'testusername',
                "password": 'testpassword',
                "port": '8443',
                "apiversion": 'v1_0',
                "retrycount": '4',
                "retryinterval": '60'
            },
            # services supported by Nuage for isolated networks.
            "network_offering": {
                "name": 'nuage_marvin',
                "displaytext": 'nuage_marvin',
                "guestiptype": 'Isolated',
                "supportedservices":
                'Dhcp,SourceNat,Connectivity,StaticNat,UserData,Firewall',
                "traffictype": 'GUEST',
                "availability": 'Optional',
                "serviceProviderList": {
                    "UserData": 'VirtualRouter',
                    "Dhcp": 'NuageVsp',
                    "Connectivity": 'NuageVsp',
                    "StaticNat": 'NuageVsp',
                    "SourceNat": 'NuageVsp',
                    "Firewall": 'NuageVsp'
                },
            },
            "network": {
                "name": "nuage",
                "displaytext": "nuage",
            },
            "ostype": 'CentOS 5.3 (64-bit)',
            "sleep": 60,
            "timeout": 10
        }


class TestNuageVsp(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls._cleanup = []
        cls.testClient = super(TestNuageVsp, cls).getClsTestClient()
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
        # nuage vsp device brings the Nuage virtual service platform into play
        cls.nuage_services = cls.services["nuage_vsp_device"]
        try:

            resp = listPhysicalNetworks.listPhysicalNetworksCmd()
            resp.zoneid = cls.zone.id
            physical_networks = cls.api_client.listPhysicalNetworks(resp)
            if isinstance(physical_networks, list):
                physical_network = physical_networks[0]
            resp = listNetworkServiceProviders.listNetworkServiceProvidersCmd()
            resp.name = 'NuageVsp'
            resp.physicalnetworkid = physical_network.id
            nw_service_providers = cls.api_client.listNetworkServiceProviders(
                resp)
            if not isinstance(nw_service_providers, list):
                # create network service provider and add nuage vsp device
                resp_add_nsp =\
                    addNetworkServiceProvider.addNetworkServiceProviderCmd()
                resp_add_nsp.name = 'NuageVsp'
                resp_add_nsp.physicalnetworkid = physical_network.id
                cls.api_client.addNetworkServiceProvider(resp_add_nsp)
                resp_add_device = addNuageVspDevice.addNuageVspDeviceCmd()
                resp_add_device.physicalnetworkid = physical_network.id
                resp_add_device.username = cls.nuage_services["username"]
                resp_add_device.password = cls.nuage_services["password"]
                resp_add_device.hostname = cls.nuage_services["hostname"]
                resp_add_device.apiversion = cls.nuage_services[
                   "apiversion"]
                resp_add_device.retrycount = cls.nuage_services[
                    "retrycount"]
                resp_add_device.retryinterval = cls.nuage_services[
                    "retryinterval"]
                cls.nuage = cls.api_client.addNuageVspDevice(
                    resp_add_device)

            cls.network_offering = NetworkOffering.create(
                cls.api_client,
                cls.services["network_offering"],
                conservemode=True
            )
            cls._cleanup.append(cls.network_offering)

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
            raise Exception("Warning: Exception in setUpClass: %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
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
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["invalid"])
    def test_network_vsp(self):
        """Test nuage Network and VM Creation
        """

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
            "Verify listVirtualMachines response for virtual machine: %s"
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
            "Verify listVirtualMachines response for virtual machine: %s"
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

        VirtualMachine.delete(virtual_machine_1, self.apiclient, expunge=True)

        # Deleting a single VM
        VirtualMachine.delete(virtual_machine_2, self.apiclient, expunge=True)

        # Delete Network
        Network.delete(self.network, self.apiclient)

        return
