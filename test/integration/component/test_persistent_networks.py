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
""" Tests for Persistent Networks without running VMs feature
"""
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.cloudstackException import cloudstackAPIException
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
import netaddr

from nose.plugins.attrib import attr

class Services(object):
    """Test Persistent Networks without running VMs
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
                                    "name": "Tiny Instance ",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 200,  # in MHz
                                    "memory": 256,  # In MBs
                        },
                         "shared_persistent_network_offering": {
                                    "name": 'Network offering for Shared Persistent Network',
                                    "displaytext": 'Network offering-DA services',
                                    "guestiptype": 'Shared',
                                    "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Firewall,Lb,UserData,StaticNat',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "ispersistent": 'True',
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
                        "isolated_persistent_network_offering": {
                                    "name": 'Network offering for Isolated Persistent Network',
                                    "displaytext": 'Network offering-DA services',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Firewall,Lb,UserData,StaticNat',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "ispersistent": 'True',
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
                         "isolated_network_offering": {
                                    "name": 'Network offering for Isolated Persistent Network',
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
                                },
                         "ostype": 'CentOS 5.3 (64-bit)',
                         # Cent OS 5.3 (64 bit)
                         "sleep": 90,
                         "timeout": 10,
                         "mode": 'advanced'
                    }



class TestPersistentNetworks(cloudstackTestCase):
    """Test Persistent Networks without running VMs
    """

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestPersistentNetworks, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["zoneid"] = cls.zone.id
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.services["account"] = cls.account.name
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.isolated_persistent_network_offering = cls.create_network_offering("isolated_persistent_network_offering")
        cls.isolated_network = cls.create_isolated_network(cls.isolated_persistent_network_offering.id)
        cls.isolated_network_offering = cls.create_network_offering("isolated_network_offering")


        # network will be deleted as part of account cleanup
        cls._cleanup = [
                        cls.account, cls.service_offering, cls.isolated_persistent_network_offering, cls.isolated_network_offering
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

    @classmethod
    def create_network_offering(cls, network_offering_type):
        network_offering = NetworkOffering.create(
                                                 cls.api_client,
                                                 cls.services[network_offering_type],
                                                 conservemode=False
                                                 )
        # Update network offering state from disabled to enabled.
        network_offering_update_response = NetworkOffering.update(
                                                           network_offering,
                                                           cls.api_client,
                                                           id=network_offering.id,
                                                           state="enabled"
                                                           )
        return network_offering

    @classmethod
    def create_isolated_network(cls, network_offering_id):
        isolated_network = Network.create(
                         cls.api_client,
                         cls.services["isolated_network"],
                         networkofferingid=network_offering_id,
                         accountid=cls.account.name,
                         domainid=cls.domain.id,
                         zoneid=cls.zone.id
                         )
        cls.debug("persistent isolated network is created: " + isolated_network.id)
        return isolated_network

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = [ ]
        return

    def tearDown(self):
        try:
            # Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def create_virtual_machine(self, network_id=None):
        virtual_machine = VirtualMachine.create(self.apiclient,
                                                          self.services["virtual_machine"],
                                                          networkids=network_id,
                                                          serviceofferingid=self.service_offering.id,
                                                          accountid=self.account.name,
                                                          domainid=self.domain.id
                                                          )
        self.debug("Virtual Machine is created: " + virtual_machine.id)
        return virtual_machine

    @attr(tags=["advanced"])
    def test_network_state_after_destroying_vms(self):
        # steps
        # 1. create virtual machine in network
        # 2. destroy created virtual machine
        #
        # validation
        # 1. Persistent network state should be implemented before VM creation and have some vlan assigned
        # 2. virtual machine should be created successfully
        # 3. Network state should be implemented even after destroying all vms in network
        self.assertEquals(self.isolated_network.state, u"Implemented", "network state of persistent is not implemented")
        self.assertIsNotNone(self.isolated_network.vlan, "vlan must not be null for persistent network")

        try:
            virtual_machine = self.create_virtual_machine(network_id=self.isolated_network.id)
            virtual_machine.delete(self.apiclient)
        except Exception as e:
            self.skipTest("vm creation/deletion fails")

        # wait for time such that, network is cleaned up
        # assuming that it will change its state to allocated after this much period
        wait_for_cleanup(self.api_client, ["network.gc.interval", "network.gc.wait"])


        networks = Network.list(self.apiclient, id=self.isolated_network.id)
        self.assertEqual(
                    isinstance(networks, list),
                    True,
                    "list Networks should return valid response"
                    )


        self.assertEquals(networks[0].state, u"Implemented", "network state of persistent network after all vms are destroyed is not implemented")

    @attr(tags=["advanced"])
    def test_shared_network_offering_with_persistent(self):
        # steps
        # 1. create shared network offering with persistent field enabled
        #
        # validation
        # 1. network offering should throw an exception
        try:
            shared_persistent_network_offering = self.create_network_offering("shared_persistent_network_offering")
            shared_persistent_network_offering.delete(self.apiclient)
            self.fail("For shared network ispersistent must be False")
        except Exception as e:
            pass

    @attr(tags=["advanced"])
    def test_upgrade_network_offering_to_persistent(self):
        # steps
        # 1. create isolated network with network offering which has ispersistent field disabled
        # 2. upgrade isolated network offering to network offering which has ispersistent field enabled
        #
        # validation
        # 1. update of network should happen successfully
        # 2. network state should be implemented and have some vlan assigned
        isolated_network = self.create_isolated_network(self.isolated_network_offering.id)
        isolated_network_response = isolated_network.update(self.apiclient, networkofferingid=self.isolated_persistent_network_offering.id)
        self.assertEquals(self.isolated_network.state, u"Implemented", "network state of isolated network upgraded to persistent is not implemented")
        self.assertIsNotNone(self.isolated_network.vlan, "vlan must not be null isolated network upgraded to for persistent network")
