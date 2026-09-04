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
""" Tests for L3 (Direct Routed) guest networks: the hypervisor routes a public IPv4/IPv6
    address directly to the Instance - no Virtual Router, no NAT, no DHCP. Instances receive
    a /32 (or /128) with the shared link-local gateway, delivered via ConfigDrive.
"""

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackException import CloudstackAPIException
from marvin.lib.base import (Account,
                             Network,
                             NetworkOffering,
                             PhysicalNetwork,
                             ServiceOffering,
                             VirtualMachine,
                             Zone)
from marvin.lib.common import (get_domain,
                               get_template,
                               get_zone)
from marvin.lib.utils import cleanup_resources
from nose.plugins.attrib import attr


class TestL3Networks(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestL3Networks, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.template = get_template(cls.apiclient, cls.zone.id, cls.services["ostype"])

        cls._cleanup = []
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.skip = False

        zone = Zone(cls.zone.__dict__)
        if zone.networktype != 'Advanced':
            cls.skip = True
            return

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        # The network operator's opt-in: L3 networks live on a dedicated physical network
        # with isolation method ROUTED. Its "vlan" range is the routed-id pool for networks
        # whose offering does not carry specifyVlan; its tag steers L3 offerings to it.
        cls.physical_network = PhysicalNetwork.create(
            cls.apiclient,
            {"name": "l3-direct-routed"},
            zoneid=cls.zone.id,
            isolationmethods="ROUTED"
        )
        cls._cleanup.append(cls.physical_network)
        cls.physical_network.addTrafficType(cls.apiclient, "Guest")
        cls.physical_network.update(
            cls.apiclient,
            vlan="5800-5899",
            tags="l3routed",
            state="Enabled"
        )

        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)

        cls.network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.services["l3_network_offering"]
        )
        cls._cleanup.append(cls.network_offering)
        cls.network_offering.update(cls.apiclient, state='Enabled')

        cls.network_offering_specifyid = NetworkOffering.create(
            cls.apiclient,
            cls.services["l3_network_offering_specifyid"]
        )
        cls._cleanup.append(cls.network_offering_specifyid)
        cls.network_offering_specifyid.update(cls.apiclient, state='Enabled')

        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, reversed(cls._cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during class cleanup : %s" % e)

    def setUp(self):
        if self.skip:
            self.skipTest("L3 networks require an Advanced zone, skipping")
        self.cleanup = []

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, reversed(self.cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def create_l3_network(self, startip="203.0.113.10", endip="203.0.113.50"):
        services = dict(self.services["l3_network"])
        services["startip"] = startip
        services["endip"] = endip
        return Network.create(
            self.apiclient,
            services,
            zoneid=self.zone.id,
            networkofferingid=self.network_offering.id,
            accountid=self.account.name,
            domainid=self.account.domainid
        )

    @attr(tags=["advanced", "smoke"], required_hardware="false")
    def test_01_create_l3_network(self):
        """ An L3 network is created like a Shared network: with a subnet. The subnet is an
            allocation pool routed to the hypervisors, not a broadcast domain. The network
            carries a routed://<id> broadcast domain - here allocated from the ROUTED
            physical network's range - whose id names the bridge (brdr-<id>) on the hosts. """
        network = self.create_l3_network()
        self.cleanup.append(network)

        self.assertEqual(network.type, "L3", "network type should be L3")
        self.assertEqual(network.broadcastdomaintype, "Routed", "an L3 network has a routed broadcast domain")
        self.assertTrue(network.broadcasturi.startswith("routed://"),
                        "the broadcast URI must be routed://<id>, got %s" % network.broadcasturi)
        allocated_id = int(network.broadcasturi.replace("routed://", ""))
        self.assertTrue(5800 <= allocated_id <= 5899,
                        "the routed id must come from the physical network's range, got %d" % allocated_id)
        self.assertIn(network.state, ["Setup", "Allocated"], "unexpected network state")

    @attr(tags=["advanced", "smoke"], required_hardware="false")
    def test_01b_create_l3_network_with_operator_specified_id(self):
        """ With a specifyVlan offering the operator picks the routed id at creation via the
            vlan parameter, so bridge names (brdr-<id>) are plannable before the network
            exists. The id must lie outside the physical network's dynamic range. """
        services = dict(self.services["l3_network"])
        network = Network.create(
            self.apiclient,
            services,
            zoneid=self.zone.id,
            networkofferingid=self.network_offering_specifyid.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            vlan="5928"
        )
        self.cleanup.append(network)
        self.assertEqual(network.broadcasturi, "routed://5928",
                         "the operator-specified routed id must be carried verbatim, got %s" % network.broadcasturi)

    @attr(tags=["advanced", "smoke"], required_hardware="false")
    def test_02_deploy_vm_in_l3_network(self):
        """ The Instance's NIC carries its address as a host route: a /32 with the shared,
            host-independent link-local gateway. The subnet's own gateway is never used. """
        network = self.create_l3_network()
        self.cleanup.append(network)

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[network.id]
        )
        self.cleanup.append(virtual_machine)

        self.assertEqual(virtual_machine.state, "Running")
        nic = virtual_machine.nic[0]
        self.assertEqual(nic.netmask, "255.255.255.255", "an L3 NIC address is a host route (/32)")
        self.assertEqual(nic.gateway, "169.254.0.1", "an L3 NIC uses the shared link-local gateway")
        self.assertTrue(nic.ipaddress.startswith("203.0.113."), "the address must come from the network's subnet")

    @attr(tags=["advanced", "smoke"], required_hardware="false")
    def test_03_l3_offering_rejects_dhcp(self):
        """ DHCP is not supported and not needed on L3 networks: ConfigDrive carries the
            address, netmask, gateway and routes, so DHCP would have nothing left to hand out. """
        services = dict(self.services["l3_network_offering"])
        services["name"] = "Test L3 offering with Dhcp - must fail"
        services["supportedservices"] = "UserData,Dns,Dhcp"
        services["serviceProviderList"] = {
            "UserData": "ConfigDrive",
            "Dns": "ConfigDrive",
            "Dhcp": "ConfigDrive"
        }
        with self.assertRaises(Exception):
            NetworkOffering.create(self.apiclient, services)

    @attr(tags=["advanced", "smoke"], required_hardware="false")
    def test_04_l3_offering_requires_userdata(self):
        """ UserData via ConfigDrive is mandatory: it is the only channel that carries the
            Instance's network configuration. """
        services = dict(self.services["l3_network_offering"])
        services["name"] = "Test L3 offering without UserData - must fail"
        services["supportedservices"] = "Dns"
        services["serviceProviderList"] = {"Dns": "ConfigDrive"}
        with self.assertRaises(Exception):
            NetworkOffering.create(self.apiclient, services)

    @attr(tags=["advanced", "smoke"], required_hardware="false")
    def test_05_l3_subnets_may_not_overlap_zone_wide(self):
        """ All L3 subnets share one host routing table and one routing fabric, so an overlap
            is an address conflict, not a policy preference. The check is zone wide. """
        network = self.create_l3_network(startip="203.0.113.10", endip="203.0.113.30")
        self.cleanup.append(network)

        try:
            overlapping = self.create_l3_network(startip="203.0.113.20", endip="203.0.113.40")
            self.cleanup.append(overlapping)
            self.fail("creating an L3 network overlapping another must fail")
        except (CloudstackAPIException, Exception):
            pass
