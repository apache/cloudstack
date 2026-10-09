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
""" Tests for a custom DHCP range on an isolated guest network
"""
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             Network,
                             NetworkOffering,
                             ServiceOffering,
                             VirtualMachine)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
from marvin.lib.utils import cleanup_resources
from nose.plugins.attrib import attr


class TestIsolatedNetworkDhcpRange(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestIsolatedNetworkDhcpRange, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.template = get_template(cls.apiclient, cls.zone.id, cls.services["ostype"])
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls._cleanup = []
        cls.account = Account.create(cls.apiclient, cls.services["account"], admin=True, domainid=cls.domain.id)
        cls._cleanup.append(cls.account)
        cls.service_offering = ServiceOffering.create(cls.apiclient, cls.services["service_offerings"]["tiny"])
        cls._cleanup.append(cls.service_offering)
        cls.network_offering = NetworkOffering.create(cls.apiclient, cls.services["isolated_network_offering"])
        cls.network_offering.update(cls.apiclient, state="Enabled")
        cls._cleanup.append(cls.network_offering)

    @classmethod
    def tearDownClass(cls):
        cleanup_resources(cls.apiclient, cls._cleanup)

    @attr(tags=["advanced"], required_hardware="false")
    def test_isolated_network_custom_dhcp_range(self):
        """A VM on an isolated network with a custom DHCP range gets an IP within it"""
        gateway = "10.99.99.1"
        start_ip = "10.99.99.10"
        end_ip = "10.99.99.20"
        network_services = dict(self.services["network"])
        network_services["gateway"] = gateway
        network_services["netmask"] = "255.255.255.0"
        network_services["startip"] = start_ip
        network_services["endip"] = end_ip

        network = Network.create(
            self.apiclient,
            network_services,
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id,
            gateway=gateway,
            netmask="255.255.255.0")
        self.cleanup.append(network)

        listed = Network.list(self.apiclient, id=network.id)[0]
        self.assertEqual(listed.startip, start_ip, "listNetworks should return the DHCP start IP")
        self.assertEqual(listed.endip, end_ip, "listNetworks should return the DHCP end IP")

        vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[network.id])
        self.cleanup.append(vm)

        octet = int(vm.nic[0].ipaddress.split(".")[-1])
        self.assertTrue(
            10 <= octet <= 20 and vm.nic[0].ipaddress.startswith("10.99.99."),
            "VM IP %s is not within the custom DHCP range %s-%s" % (vm.nic[0].ipaddress, start_ip, end_ip))

    def setUp(self):
        self.cleanup = []

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
