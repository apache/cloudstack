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

""" Test for IPv4 Routing mode"""
import datetime
import logging
import random

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import ZoneIpv4Subnet, Domain, Account, ServiceOffering, NetworkOffering, VpcOffering, Network, \
    Ipv4SubnetForGuestNetwork, VirtualMachine, VPC, NetworkACLList
from marvin.lib.common import get_domain, get_zone, get_template

from nose.plugins.attrib import attr

SUBNET_PREFIX = "172.30."
SUBNET_1_PREFIX = SUBNET_PREFIX + str(random.randrange(100, 150))
SUBNET_2_PREFIX = SUBNET_PREFIX + str(random.randrange(151, 199))

VPC_CIDR_PREFIX = "172.31"  # .0 to .16
NETWORK_CIDR_PREFIX = VPC_CIDR_PREFIX + ".100"

NETWORK_OFFERING = {
    "name": "Test Network offering - Routing mode",
    "displaytext": "Test Network offering - Routing mode",
    "routingmode": "ROUTED",
    "guestiptype": "Isolated",
    "supportedservices":
        "Dhcp,Dns,UserData,Firewall",
    "traffictype": "GUEST",
    "availability": "Optional",
    "egress_policy": "true",
    "serviceProviderList": {
        "Dhcp": "VirtualRouter",
        "Dns": "VirtualRouter",
        "UserData": "VirtualRouter",
        "Firewall": "VirtualRouter"
    }
}

VPC_OFFERING = {
    "name": "Test VPC offering - Routing mode",
    "displaytext": "Test VPC offering - Routing mode",
    "routingmode": "ROUTED",
    "supportedservices":
        "Dhcp,Dns,UserData,NetworkACL"
}

VPC_NETWORK_OFFERING = {
    "name": "Test VPC Network offering - Routing mode",
    "displaytext": "Test VPC Network offering - Routing mode",
    "routingmode": "ROUTED",
    "guestiptype": "Isolated",
    "supportedservices":
        "Dhcp,Dns,UserData,NetworkACL",
    "traffictype": "GUEST",
    "availability": "Optional",
    "serviceProviderList": {
        "Dhcp": "VpcVirtualRouter",
        "Dns": "VpcVirtualRouter",
        "UserData": "VpcVirtualRouter",
        "NetworkACL": "VpcVirtualRouter"
    }
}


class TestIpv4Routing(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testdata = super(TestIpv4Routing, cls).getClsTestClient()
        cls.services = testdata.getParsedTestDataConfig()
        cls.apiclient = testdata.getApiClient()
        cls.dbclient = testdata.getDbConnection()
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient)
        cls.template = get_template(cls.apiclient, cls.zone.id)
        cls._cleanup = []

        cls.logger = logging.getLogger("TestIpv4Routing")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        # 1. create subnet for zone
        cls.subnet_1 = ZoneIpv4Subnet.create(
            cls.apiclient,
            zoneid=cls.zone.id,
            subnet=SUBNET_1_PREFIX + ".0/24"
        )
        cls._cleanup.append(cls.subnet_1)

        # 2. Create small service offering
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )
        cls._cleanup.append(cls.service_offering)

        # 3. Create network and vpc offering with routing mode
        cls.network_offering_isolated = NetworkOffering.create(
            cls.apiclient,
            NETWORK_OFFERING
        )
        cls._cleanup.append(cls.network_offering_isolated)
        cls.network_offering_isolated.update(cls.apiclient, state='Enabled')

        cls.vpc_offering = VpcOffering.create(
            cls.apiclient,
            VPC_OFFERING
        )
        cls._cleanup.append(cls.vpc_offering)
        cls.vpc_offering.update(cls.apiclient, state='Enabled')

        cls.vpc_network_offering = NetworkOffering.create(
            cls.apiclient,
            VPC_NETWORK_OFFERING
        )
        cls._cleanup.append(cls.vpc_network_offering)
        cls.vpc_network_offering.update(cls.apiclient, state='Enabled')

        # 4. Create sub-domain
        cls.sub_domain = Domain.create(
            cls.apiclient,
            cls.services["acl"]["domain1"]
        )
        cls._cleanup.append(cls.sub_domain)

        # 5. Create regular user
        cls.regular_user = Account.create(
            cls.apiclient,
            cls.services["acl"]["accountD11A"],
            domainid=cls.sub_domain.id
        )
        cls._cleanup.append(cls.regular_user)

        # 6. Create api clients for regular user
        cls.regular_user_user = cls.regular_user.user[0]
        cls.regular_user_apiclient = cls.testClient.getUserApiClient(
            cls.regular_user_user.username, cls.sub_domain.name
        )

        cls.test_network = None
        cls.test_network_vm = None
        cls.test_vpc = None
        cls.test_vpc_tier = None
        cls.test_vpc_vm = None

    @classmethod
    def tearDownClass(cls):
        super(TestIpv4Routing, cls).tearDownClass()

    @classmethod
    def message(cls, msg):
        cls.logger.debug("=== " + str(datetime.datetime.now()) + " " + msg)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        super(TestIpv4Routing, self).tearDown()

    @attr(tags=['advanced', 'basic', 'sg'], required_hardware=False)
    def test_01_zone_subnet(self):
        """ Test for subnet for zone"""
        """
            # 1. Create subnet
            # 2. List subnet
            # 3. Update subnet
            # 4. dedicate subnet to domain
            # 5. released dedicated subnet
            # 6. dedicate subnet to sub-domain/account
            # 7. released dedicated subnet
            # 8. delete subnet
        """
        self.message("Running test_01_zone_subnet")
        # 1. Create subnet
        self.subnet_2 = ZoneIpv4Subnet.create(
            self.apiclient,
            zoneid=self.zone.id,
            subnet=SUBNET_2_PREFIX + ".0/24"
        )
        self.cleanup.append(self.subnet_2)
        # 2. List subnet
        subnets = ZoneIpv4Subnet.list(
            self.apiclient,
            id=self.subnet_2.id
        )
        self.assertEqual(
            isinstance(subnets, list),
            True,
            "List subnets for zone should return a valid list"
        )
        self.assertEqual(
            len(subnets) == 1,
            True,
            "The number of subnets for zone (%s) should be equal to 1" % (len(subnets))
        )
        self.assertEqual(
            subnets[0].subnet == SUBNET_2_PREFIX + ".0/24",
            True,
            "The subnet of subnet for zone (%s) should be equal to %s" % (subnets[0].subnet, SUBNET_2_PREFIX + ".0/24")
        )
        # 3. Update subnet
        self.subnet_2.update(
            self.apiclient,
            subnet=SUBNET_2_PREFIX + ".0/25"
        )
        subnets = ZoneIpv4Subnet.list(
            self.apiclient,
            id=self.subnet_2.id
        )
        self.assertEqual(
            isinstance(subnets, list) and len(subnets) == 1 and subnets[0].subnet == SUBNET_2_PREFIX + ".0/25",
            True,
            "The subnet of subnet for zone should be equal to %s" % (SUBNET_2_PREFIX + ".0/25")
        )
        # 4. dedicate subnet to domain
        ZoneIpv4Subnet.dedicate(
            self.apiclient,
            id=self.subnet_2.id,
            domainid=self.domain.id
        )
        subnets = ZoneIpv4Subnet.list(
            self.apiclient,
            id=self.subnet_2.id
        )
        self.assertEqual(
            isinstance(subnets, list) and len(subnets) == 1 and subnets[0].domainid == self.domain.id,
            True,
            "The subnet should be dedicated to domain %s" % self.domain.id
        )
        # 5. released dedicated subnet
        self.subnet_2.release(
            self.apiclient
        )
        subnets = ZoneIpv4Subnet.list(
            self.apiclient,
            id=self.subnet_2.id
        )
        self.assertEqual(
            isinstance(subnets, list) and len(subnets) == 1 and not subnets[0].domainid,
            True,
            "The subnet should not be dedicated to domain %s" % self.domain.id
        )
        # 6. dedicate subnet to sub-domain/account
        ZoneIpv4Subnet.dedicate(
            self.apiclient,
            id=self.subnet_2.id,
            domainid=self.sub_domain.id,
            account=self.regular_user.name
        )
        subnets = ZoneIpv4Subnet.list(
            self.apiclient,
            id=self.subnet_2.id
        )
        self.assertEqual(
            isinstance(subnets, list) and len(subnets) == 1
            and subnets[0].domainid == self.sub_domain.id and subnets[0].account == self.regular_user.name,
            True,
            "The subnet should be dedicated to account %s" % self.regular_user.name
        )
        # 7. released dedicated subnet
        self.subnet_2.release(
            self.apiclient
        )
        subnets = ZoneIpv4Subnet.list(
            self.apiclient,
            id=self.subnet_2.id
        )
        self.assertEqual(
            isinstance(subnets, list) and len(subnets) == 1 and not subnets[0].domainid,
            True,
            "The subnet should not be dedicated to account %s" % self.regular_user.name
        )
        # 8. delete subnet
        self.subnet_2.delete(
            self.apiclient
        )
        self.cleanup.remove(self.subnet_2)

    @attr(tags=['advanced', 'basic', 'sg'], required_hardware=False)
    def test_02_create_network_routed_mode_with_specified_cidr(self):
        """ Test for guest network with specified cidr"""
        """
            # 1. Create Isolated network
            # 2. List subnet for network by subnet
            # 3. Delete the network
            # 4. List subnet for network by subnet. the subnet should be gone as well
        """
        self.message("Running test_02_create_network_routed_mode_with_specified_cidr")

        # 1. Create Isolated network
        isolated_network = Network.create(
            self.apiclient,
            self.services["network"],
            gateway=NETWORK_CIDR_PREFIX + ".1",
            netmask="255.255.255.0",
            networkofferingid=self.network_offering_isolated.id,
            zoneid=self.zone.id
        )
        self.cleanup.append(isolated_network)

        # 2. List subnet for network by subnet
        subnets = Ipv4SubnetForGuestNetwork.list(
            self.apiclient,
            subnet=NETWORK_CIDR_PREFIX + ".0/24"
        )
        self.assertEqual(
            isinstance(subnets, list) and len(subnets) == 1
            and subnets[0].subnet == NETWORK_CIDR_PREFIX + ".0/24" and subnets[0].state == "Allocated",
            True,
            "The subnet should be added for network %s" % isolated_network.name
        )

        # 3. Delete the network
        isolated_network.delete(self.apiclient)
        self.cleanup.remove(isolated_network)

        # 4. List subnet for network by subnet. the subnet should be gone as well
        network_cidr = subnets[0].subnet
        subnets = Ipv4SubnetForGuestNetwork.list(
            self.apiclient,
            subnet=network_cidr
        )
        self.assertEqual(
            not isinstance(subnets, list) or len(subnets) == 0,
            True,
            "The subnet %s should be removed for network %s" % (network_cidr, isolated_network.name)
        )

    @attr(tags=['advanced', 'basic', 'sg'], required_hardware=False)
    def test_03_create_subnets_for_guest_network(self):
        """ Test for subnets for guest network with cidr/cidrsize"""
        """
            # 1. Create subnet with cidr for guest network
            # 2. List subnets for network
            # 3. delete subnet for network

            # 4. Create subnet with cidrsize
            # 5. List subnet for network
            # 6. delete subnet for network
        """
        self.message("Running test_03_create_subnets_for_guest_network")

        # 1. Create subnet with cidr for guest network
        subnet_network_1 = Ipv4SubnetForGuestNetwork.create(
            self.apiclient,
            parentid=self.subnet_1.id,
            subnet=SUBNET_1_PREFIX + ".0/26"
        )
        self.cleanup.append(subnet_network_1)

        # 2. List subnets for network
        subnets = Ipv4SubnetForGuestNetwork.list(
            self.apiclient,
            subnet=subnet_network_1.subnet
        )
        self.assertEqual(
            isinstance(subnets, list) and len(subnets) == 1,
            True,
            "The subnet should be created for subnet_network_1 %s" % subnet_network_1.subnet
        )

        # 3. delete subnet for network
        subnet_network_1.delete(self.apiclient)
        self.cleanup.remove(subnet_network_1)

        # 4. Create subnet with cidrsize
        subnet_network_2 = Ipv4SubnetForGuestNetwork.create(
            self.apiclient,
            parentid=self.subnet_1.id,
            cidrsize=26
        )
        self.cleanup.append(subnet_network_2)
        # 5. List subnet for network
        subnets = Ipv4SubnetForGuestNetwork.list(
            self.apiclient,
            subnet=subnet_network_2.subnet
        )
        self.assertEqual(
            isinstance(subnets, list) and len(subnets) == 1,
            True,
            "The subnet should be created for subnet_network_2 %s" % subnet_network_2.subnet
        )

        # 6. delete subnet for network
        subnet_network_2.delete(self.apiclient)
        self.cleanup.remove(subnet_network_2)

    @attr(tags=['advanced', 'basic', 'sg'], required_hardware=False)
    def test_04_create_isolated_network_routed_mode_with_cidrsize(self):
        """ Test for subnet and guest network with cidrsize"""
        """
            # 1. Create Isolated network with cidrsize
            # 2. List subnet for network by networkid
            # 3. Delete the network
            # 4. List subnet for network by networkid, it should be removed
        """
        self.message("Running test_04_create_isolated_network_routed_mode_with_cidrsize")

        # 1. Create Isolated network with cidrsize
        isolated_network = Network.create(
            self.apiclient,
            self.services["network"],
            networkofferingid=self.network_offering_isolated.id,
            zoneid=self.zone.id,
            cidrsize=26
        )
        self.cleanup.append(isolated_network)

        # 2. List subnet for network by networkid
        subnets = Ipv4SubnetForGuestNetwork.list(
            self.apiclient,
            networkid=isolated_network.id
        )
        self.assertEqual(
            isinstance(subnets, list) and len(subnets) == 1
            and subnets[0].networkid == isolated_network.id and subnets[0].state == "Allocated",
            True,
            "The subnet should be created for isolated_network %s" % isolated_network.name
        )

        # 3. Delete the network
        isolated_network.delete(self.apiclient)
        self.cleanup.remove(isolated_network)

        # 4. List subnet for network by networkid, it should be removed
        network_cidr = subnets[0].subnet
        subnets = Ipv4SubnetForGuestNetwork.list(
            self.apiclient,
            subnet=network_cidr
        )
        self.assertEqual(
            isinstance(subnets, list) and len(subnets) == 1
            and not subnets[0].networkid and subnets[0].state == "Free",
            True,
            "The subnet should be created for isolated_network %s" % isolated_network.name
        )

    @attr(tags=['advanced', 'basic', 'sg'], required_hardware=False)
    def test_05_isolated_network_with_routed_mode(self):
        """ Test for Isolated Network with Routed mode"""
        """
            # 1. Create Isolated network
            # 2. Create VM in the network
        """
        self.message("Running test_05_isolated_network_with_routed_mode")

        # 1. Create Isolated network
        self.test_network = Network.create(
            self.regular_user_apiclient,
            self.services["network"],
            networkofferingid=self.network_offering_isolated.id,
            zoneid=self.zone.id,
            domainid=self.sub_domain.id,
            accountid=self.regular_user.name,
            cidrsize=26
        )
        self._cleanup.append(self.test_network)

        # 2. Create VM in the network
        self.test_network_vm = VirtualMachine.create(
            self.regular_user_apiclient,
            self.services["virtual_machine"],
            zoneid=self.zone.id,
            domainid=self.sub_domain.id,
            accountid=self.regular_user.name,
            networkids=self.test_network.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id)
        self._cleanup.append(self.test_network_vm)

    @attr(tags=['advanced', 'basic', 'sg'], required_hardware=False)
    def test_06_vpc_and_tier_with_routed_mode(self):
        """ Test for VPC/tier with Routed mode"""
        """
            # 1. Create VPC
            # 2. Create Network ACL (egress = Deny, ingress = Deny)
            # 3. Create VPC tier with Network ACL in the VPC
            # 4. Create VM in the VPC tier
        """
        self.message("Running test_06_vpc_and_tier_with_routed_mode")

        # 1. Create VPC
        self.services["vpc"]["cidr"] = VPC_CIDR_PREFIX + ".0.0/20"
        self.test_vpc = VPC.create(self.apiclient,
                                   self.services["vpc"],
                                   vpcofferingid=self.vpc_offering.id,
                                   zoneid=self.zone.id,
                                   domainid=self.sub_domain.id,
                                   account=self.regular_user.name,
                                   start=False
                                   )
        self._cleanup.append(self.test_vpc)

        # 2. Create Network ACL (egress = Deny, ingress = Deny)
        network_acl = NetworkACLList.create(self.apiclient,
                                            services={},
                                            name="test-network-acl",
                                            description="test-network-acl",
                                            vpcid=self.test_vpc.id
                                            )

        # 3. Create VPC tier with Network ACL in the VPC
        self.test_vpc_tier = Network.create(self.regular_user_apiclient,
                                            self.services["network"],
                                            networkofferingid=self.vpc_network_offering.id,
                                            zoneid=self.zone.id,
                                            domainid=self.sub_domain.id,
                                            accountid=self.regular_user.name,
                                            vpcid=self.test_vpc.id,
                                            gateway=VPC_CIDR_PREFIX + ".1.1",
                                            netmask="255.255.255.0",
                                            aclid=network_acl.id
                                            )
        self._cleanup.append(self.test_vpc_tier)

        # 4. Create VM in the VPC tier
        self.test_vpc_vm = VirtualMachine.create(
            self.regular_user_apiclient,
            self.services["virtual_machine"],
            zoneid=self.zone.id,
            domainid=self.sub_domain.id,
            accountid=self.regular_user.name,
            networkids=self.test_vpc_tier.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id)
        self._cleanup.append(self.test_vpc_vm)

    @attr(tags=['advanced', 'basic', 'sg'], required_hardware=False)
    def test_07_connectivity_between_network_and_vpc_tier(self):
        """ Test for connectivity between VMs in the Isolated Network and VPC/tier"""
        """
            # 1. Add static routes in VRs manually
            # 2. Test VM2 in VR1 (ping/ssh should fail)
            # 3. Test VM1 in VR2 (ping/ssh should fail)
            # 4. Create IPv4 firewalls for Isolated network
            # 5. Create Ingress rules in Network ACL for VPC
            # 6. Create Egress rules in Network ACL for VPC
            # 7. Test VM2 in VR1 (how-to in VM1 ?)
            # 8. Test VM1 in VR2 (how-to in VM2 ?)
            # 9. Delete Network ACL rules for VPC
            # 10. Delete IPv4 firewall rules for Network
        """
        self.message("Running test_07_connectivity_between_network_and_vpc_tier")
