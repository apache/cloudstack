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
""" Tests for IP reservation feature
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
    """Test IP Reservation
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
                        "isolated_network_offering": {
                                    "name": 'Network offering for Isolated Network',
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
                         "isolated_persistent_network_offering": {
                                    "name": 'Network offering for Isolated Network',
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
                         "isolated_network": {
                                  "name": "Isolated Network",
                                  "displaytext": "Isolated Network",
                                  "netmask": "255.255.255.0",
                                  "gateway": "10.1.1.1"
                         },
                         # update CIDR according to netmask and gateway in isolated_network
                         "isolated_network_cidr": "10.1.1.0/24",
                         "virtual_machine": {
                                    "displayname": "Test VM",
                                },
                         "ostype": 'CentOS 5.3 (64-bit)',
                         # Cent OS 5.3 (64 bit)
                         "sleep": 90,
                         "timeout": 10,
                         "mode": 'advanced'
                    }


class TestIpReservation(cloudstackTestCase):
    """Test IP Range Reservation with a Network
    """
    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestIpReservation, cls).getClsTestClient().getApiClient()
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
        cls.isolated_network_offering = cls.create_isolated_network_offering("isolated_network_offering")
        cls.isolated_persistent_network_offering = cls.create_isolated_network_offering("isolated_persistent_network_offering")
        cls.isolated_network = cls.create_isolated_network(cls.isolated_network_offering.id)
        cls.isolated_persistent_network = cls.create_isolated_network(cls.isolated_persistent_network_offering.id)
        # network will be deleted as part of account cleanup
        cls._cleanup = [
                        cls.account, cls.service_offering, cls.isolated_network_offering, cls.isolated_persistent_network_offering,
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
    def create_isolated_network_offering(cls, network_offering):
        isolated_network_offering = NetworkOffering.create(
                                                 cls.api_client,
                                                 cls.services[network_offering],
                                                 conservemode=False
                                                 )
        # Update network offering state from disabled to enabled.
        network_offering_update_response = NetworkOffering.update(
                                                           isolated_network_offering,
                                                           cls.api_client,
                                                           id=isolated_network_offering.id,
                                                           state="enabled"
                                                           )
        return isolated_network_offering

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
        cls.debug("isolated network is created: " + isolated_network.id)
        return isolated_network



    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = [ ]
        update_response = Network.update(self.isolated_persistent_network, self.apiclient, id=self.isolated_persistent_network.id, guestvmcidr=self.services["isolated_network_cidr"])
        if self.services["isolated_network_cidr"] <> update_response.cidr:
            raise Exception("problem in updating cidr for test setup")
        return

    def tearDown(self):
        try:
            # Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def create_virtual_machine(self, network_id=None, ip_address=None):
        virtual_machine = VirtualMachine.create(self.apiclient,
                                                          self.services["virtual_machine"],
                                                          networkids=network_id,
                                                          serviceofferingid=self.service_offering.id,
                                                          accountid=self.account.name,
                                                          domainid=self.domain.id,
                                                          ipaddress=ip_address
                                                          )
        self.debug("Virtual Machine is created: " + virtual_machine.id)
        self.cleanup.append(virtual_machine)
        return virtual_machine

    @attr(tags=["advanced"])
    def test_network_not_implemented(self):
        # steps
        # 1. update guestvmcidr of isolated network (non persistent)
        #
        # validation
        # should throw exception as network is not in implemented state as no vm is created
        try:
            update_response = Network.update(self.isolated_network, self.apiclient, id=isolated_network.id, guestvmcidr="10.1.1.0/26")
            self.fail("Network Update of guest VM CIDR is successful withot any VM deployed in network")
        except Exception as e:
            self.debug("Network Update of guest VM CIDR should fail as there is no VM deployed in network")

    @attr(tags=["advanced"])
    def test_vm_create_after_reservation(self):
        # steps
        # 1. create vm in persistent isolated network with ip in guestvmcidr
        # 2. update guestvmcidr
        # 3. create another VM
        #
        # validation
        # 1. guest vm cidr should be successfully updated with correct value
        # 2. existing guest vm ip should not be changed after reservation
        # 3. newly created VM should get ip in guestvmcidr
        guest_vm_cidr = u"10.1.1.0/29"
        virtual_machine_1 = None
        try:
            virtual_machine_1 = self.create_virtual_machine(network_id=self.isolated_persistent_network.id, ip_address=u"10.1.1.3")
        except Exception as e:
            self.skipTest("VM creation fails in network ")

        update_response = Network.update(self.isolated_persistent_network, self.apiclient, id=self.isolated_persistent_network.id, guestvmcidr=guest_vm_cidr)
        self.assertEqual(guest_vm_cidr, update_response.cidr, "cidr in response is not as expected")
        vm_list = VirtualMachine.list(self.apiclient,
                                       id=virtual_machine_1.id)
        self.assertEqual(isinstance(vm_list, list),
                         True,
                         "VM list response in not a valid list")
        self.assertEqual(vm_list[0].nic[0].ipaddress,
                          virtual_machine_1.ipaddress,
                           "VM IP should not change after reservation")
        try:
            virtual_machine_2 = self.create_virtual_machine(network_id=self.isolated_persistent_network.id)
            if netaddr.IPAddress(virtual_machine_2.ipaddress) not in netaddr.IPNetwork(guest_vm_cidr):
                self.fail("Newly created VM doesn't get IP from reserverd CIDR")
        except Exception as e:
            self.skipTest("VM creation fails, cannot validate the condition")

    @attr(tags=["advanced"])
    def test_reservation_after_router_restart(self):
        # steps
        # 1. update guestvmcidr of persistent isolated network
        # 2. reboot router
        #
        # validation
        # 1. guest vm cidr should be successfully updated with correct value
        # 2. network cidr should remain same after router restart
        guest_vm_cidr = u"10.1.1.0/29"

        update_response = Network.update(self.isolated_persistent_network, self.apiclient, id=self.isolated_persistent_network.id, guestvmcidr=guest_vm_cidr)
        self.assertEqual(guest_vm_cidr, update_response.cidr, "cidr in response is not as expected")

        routers = Router.list(self.apiclient,
                             networkid=self.isolated_persistent_network.id,
                             listall=True)
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return valid response"
                    )
        if not routers:
            self.skipTest("Router list should not be empty, skipping test")

        Router.reboot(self.apiclient, routers[0].id)
        networks = Network.list(self.apiclient, id=self.isolated_persistent_network.id)
        self.assertEqual(
                    isinstance(networks, list),
                    True,
                    "list Networks should return valid response"
                    )
        self.assertEqual(networks[0].cidr, guest_vm_cidr, "guestvmcidr should match after router reboot")

    @attr(tags=["advanced"])
    def test_vm_create_outside_cidr_after_reservation(self):
        # steps
        # 1. update guestvmcidr of persistent isolated network
        # 2. create another VM with ip outside guestvmcidr
        #
        # validation
        # 1. guest vm cidr should be successfully updated with correct value
        # 2  newly created VM should not be created and result in exception
        guest_vm_cidr = u"10.1.1.0/29"
        update_response = Network.update(self.isolated_persistent_network, self.apiclient, id=self.isolated_persistent_network.id, guestvmcidr=guest_vm_cidr)
        self.assertEqual(guest_vm_cidr, update_response.cidr, "cidr in response is not as expected")
        try:
            self.create_virtual_machine(network_id=self.isolated_persistent_network.id, ip_address=u"10.1.1.9")
            self.fail("vm should not be created ")
        except Exception as e:
            self.debug("exception as IP is outside of guestvmcidr")
