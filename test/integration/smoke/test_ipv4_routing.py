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
from marvin.lib.base import ZoneIpv4Subnet, Domain, Account, ServiceOffering, NetworkOffering, VpcOffering
from marvin.lib.common import get_domain, get_zone, get_template

from nose.plugins.attrib import attr

SUBNET_PREFIX = "172.20."
SUBNET_1_PREFIX = SUBNET_PREFIX + str(random.randrange(100, 150))
SUBNET_2_PREFIX = SUBNET_PREFIX + str(random.randrange(151, 199))

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
            subnet=SUBNET_1_PREFIX + ".1/24"
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

        cls.vpc_offering = VpcOffering.create(
            cls.apiclient,
            VPC_OFFERING
        )
        cls._cleanup.append(cls.vpc_offering)

        cls.vpc_network_offering = NetworkOffering.create(
            cls.apiclient,
            VPC_NETWORK_OFFERING
        )
        cls._cleanup.append(cls.vpc_network_offering)

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
            subnet=SUBNET_2_PREFIX + ".1/24"
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
            subnets[0].subnet == SUBNET_2_PREFIX + ".1/24",
            True,
            "The subnet of subnet for zone (%s) should be equal to %s" % (subnets[0].subnet, SUBNET_2_PREFIX + ".1/24")
        )
        # 3. Update subnet
        self.subnet_2.update(
            self.apiclient,
            subnet=SUBNET_2_PREFIX + ".1/25"
        )
        subnets = ZoneIpv4Subnet.list(
            self.apiclient,
            id=self.subnet_2.id
        )
        self.assertEqual(
            isinstance(subnets, list) and len(subnets) == 1 and subnets[0].subnet == SUBNET_2_PREFIX + ".1/25",
            True,
            "The subnet of subnet for zone should be equal to %s" % (SUBNET_2_PREFIX + ".1/25")
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

    @attr(tags=['advanced', 'basic', 'sg'], required_hardware=False)
    def test_03_create_network_routed_mode_without_cidr(self):
        """ Test for guest network without cidr"""
        """
            # 1. Create subnet with cidr
            # 6. List subnets for network
            # 8. Create Isolated network. The cidr should be same
            # 9. Delete the network
            # 10. delete subnet for network

            # 10. Create subnet with cidrsize (todo)
            # 11. List subnet for network
            # 12. delete subnet for network
        """
        self.message("Running test_03_create_network_routed_mode_without_cidr")

    @attr(tags=['advanced', 'basic', 'sg'], required_hardware=False)
    def test_04_create_subnet_and_network_routed_mode_with_cidrsize(self):
        """ Test for subnet and guest network with cidrsize"""
        """
            # 1. Create subnet with cidrsize (todo)
            # 2. List subnet for network
            # 3. delete subnet for network
            
            # 4. Create Isolated network with cidrsize (todo)
            # 5. Delete the network
        """
        self.message("Running test_04_create_subnet_and_network_routed_mode_with_cidrsize")

    @attr(tags=['advanced', 'basic', 'sg'], required_hardware=False)
    def test_05_isolated_network_with_routed_mode(self):
        """ Test for Isolated Network with Routed mode"""
        """
            # 1. Create network offering with routing mode (egress policy = Allow)
            # 2. Create Isolated network
            # 3. Create VM in the network
        """
        self.message("Running test_05_isolated_network_with_routed_mode")

    @attr(tags=['advanced', 'basic', 'sg'], required_hardware=False)
    def test_06_vpc_and_tier_with_routed_mode(self):
        """ Test for VPC/tier with Routed mode"""
        """
            # 1. Create VPC offering with routing mode
            # 2. Create VPC tier offering with routing mode
            # 3. Create VPC
            # 4. Create Network ACL (egress = Deny, ingress = Deny)
            # 5. Create VPC tier with Network ACL in the VPC
            # 6. Create VM in the VPC tier
        """
        self.message("Running test_06_vpc_and_tier_with_routed_mode")

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
