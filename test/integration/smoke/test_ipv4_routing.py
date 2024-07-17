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

""" Test for IPv4 Routed mode"""
import datetime
import logging
import random
import time

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import ZoneIpv4Subnet, Domain, Account, ServiceOffering, NetworkOffering, VpcOffering, Network, \
    Ipv4SubnetForGuestNetwork, VirtualMachine, VPC, NetworkACLList, NetworkACL, RoutingFirewallRule, Template
from marvin.lib.common import get_domain, get_zone, list_routers, list_hosts
from marvin.lib.utils import get_host_credentials, get_process_status

from nose.plugins.attrib import attr

ICMPv4_ALL_TYPES = ("{ echo-reply, destination-unreachable, source-quench, redirect, echo-request, "
                    "router-advertisement, router-solicitation, time-exceeded, parameter-problem, timestamp-request, "
                    "timestamp-reply, info-request, info-reply, address-mask-request, address-mask-reply }")
SUBNET_PREFIX = "172.30."
SUBNET_1_PREFIX = SUBNET_PREFIX + str(random.randrange(100, 150))
SUBNET_2_PREFIX = SUBNET_PREFIX + str(random.randrange(151, 199))

VPC_CIDR_PREFIX = "172.31"  # .0 to .16
NETWORK_CIDR_PREFIX = VPC_CIDR_PREFIX + ".100"

MAX_RETRIES = 30
WAIT_INTERVAL = 5

test_network = None
test_network_vm = None
test_vpc = None
test_vpc_tier = None
test_vpc_vm = None
test_network_acl = None

NETWORK_OFFERING = {
    "name": "Test Network offering - Routed mode",
    "displaytext": "Test Network offering - Routed mode",
    "networkmode": "ROUTED",
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
    "name": "Test VPC offering - Routed mode",
    "displaytext": "Test VPC offering - Routed mode",
    "networkmode": "ROUTED",
    "supportedservices":
        "Dhcp,Dns,UserData,NetworkACL"
}

VPC_NETWORK_OFFERING = {
    "name": "Test VPC Network offering - Routed mode",
    "displaytext": "Test VPC Network offering - Routed mode",
    "networkmode": "ROUTED",
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
        cls.hypervisor = testdata.getHypervisorInfo()
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient)

        cls._cleanup = []

        cls.logger = logging.getLogger("TestIpv4Routing")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        # 0. register template
        cls.template = Template.register(cls.apiclient, cls.services["test_templates"][cls.hypervisor.lower()],
                                         zoneid=cls.zone.id, hypervisor=cls.hypervisor.lower())
        cls.template.download(cls.apiclient)
        cls._cleanup.append(cls.template)

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

        # 3. Create network and vpc offering with routed mode
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

    @classmethod
    def tearDownClass(cls):
        super(TestIpv4Routing, cls).tearDownClass()

    @classmethod
    def message(cls, msg):
        cls.logger.debug("====== " + str(datetime.datetime.now()) + " " + msg + " ======")

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        super(TestIpv4Routing, self).tearDown()

    def get_router(self, networkid=None, vpcid=None):
        # list router
        if vpcid:
            list_router_response = list_routers(
                self.apiclient,
                vpcid=vpcid,
                listall="true"
            )
        else:
            list_router_response = list_routers(
                self.apiclient,
                networkid=networkid,
                listall="true"
            )
        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "list routers response should return a valid list"
        )
        router = list_router_response[0]
        return router

    def run_command_in_router(self, router, command):
        # get host of router
        hosts = list_hosts(
            self.apiclient,
            zoneid=router.zoneid,
            type='Routing',
            state='Up',
            id=router.hostid
        )
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check list host returns a valid list"
        )
        host = hosts[0]

        # run command
        result = ''
        if router.hypervisor.lower() in ('vmware', 'hyperv'):
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                router.linklocalip,
                command,
                hypervisor=router.hypervisor
            )
        else:
            try:
                host.user, host.passwd = get_host_credentials(self.config, host.ipaddress)
                result = get_process_status(
                    host.ipaddress,
                    22,
                    host.user,
                    host.passwd,
                    router.linklocalip,
                    command
                )
            except KeyError:
                self.skipTest("Marvin configuration has no host credentials to check router services")
        res = str(result)
        self.message("VR command (%s) result: (%s)" % (command, res))
        return res

    def createNetworkAclRule(self, rule):
        return NetworkACL.create(self.apiclient,
                                 services=rule,
                                 aclid=test_network_acl.id)

    def createIpv4RoutingFirewallRule(self, rule):
        return RoutingFirewallRule.create(self.apiclient,
                                          services=rule,
                                          networkid=test_network.id)

    def verifyNftablesRulesInRouter(self, router, rules):
        if router.vpcid:
            table = "ip4_acl"
        else:
            table = "ip4_firewall"
        for rule in rules:
            cmd = "nft list chain ip %s %s" % (table, rule["chain"])
            res = self.run_command_in_router(router, cmd)
            if "exists" not in rule or rule["exists"]:
                exists = True
            else:
                exists = False
            if exists and not rule["rule"] in res:
                self.fail("The nftables rule (%s) should exist but is not found in the VR !!!" % rule["rule"])
            if not exists and rule["rule"] in res:
                self.fail("The nftables rule (%s) should not exist but is found in the VR !!!" % rule["rule"])
            self.message("The nftables rules look good so far.")

    def verifyPingFromRouter(self, router, vm, expected=True, retries=2):
        while retries > 0:
            cmd_ping_vm = "ping -c1 -W1 %s" % vm.ipaddress
            try:
                result = self.run_command_in_router(router, cmd_ping_vm)
                if "0 packets received" in result:
                    retries = retries - 1
                    self.message("No packets received, remaining retries %s" % retries)
                    if retries > 0:
                        time.sleep(WAIT_INTERVAL)
                else:
                    self.message("packets are received, looks good")
                    return
            except Exception as ex:
                self.fail("Failed to ping vm %s from router %s: %s" % (vm.ipaddress, router.name, ex))
        if retries == 0 and expected:
            self.fail("Failed to ping vm %s from router %s, which is expected to work !!!" % (vm.ipaddress, router.name))
        if retries > 0 and not expected:
            self.fail("ping vm %s from router %s works, however it is unexpected !!!" % (vm.ipaddress, router.name))

    @attr(tags=['advanced'], required_hardware=False)
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

    @attr(tags=['advanced'], required_hardware=False)
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

    @attr(tags=['advanced'], required_hardware=False)
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

    @attr(tags=['advanced'], required_hardware=False)
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

        # 4. List subnet for network by network cidr, it should be removed
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

    @attr(tags=['advanced'], required_hardware=False)
    def test_05_create_vpc_routed_mode_with_cidrsize(self):
        """ Test for Routed VPC with cidrsize"""
        """
            # 1. Create VPC with cidrsize
            # 2. List subnet for network by vpcid
            # 3. Delete the VPC
            # 4. List subnet for network by vpcid, it should be removed
        """
        self.message("Running test_05_create_vpc_routed_mode_with_cidrsize")

        # 1. Create VPC with cidrsize
        del self.services["vpc"]["cidr"]
        vpc = VPC.create(self.apiclient,
                         self.services["vpc"],
                         vpcofferingid=self.vpc_offering.id,
                         zoneid=self.zone.id,
                         cidrsize=26,
                         start=False
                         )
        self.cleanup.append(vpc)

        # 2. List subnet for network by networkid
        subnets = Ipv4SubnetForGuestNetwork.list(
            self.apiclient,
            vpcid=vpc.id
        )
        self.assertEqual(
            isinstance(subnets, list) and len(subnets) == 1
            and subnets[0].vpcid == vpc.id and subnets[0].state == "Allocated",
            True,
            "The subnet should be created for vpc %s" % vpc.name
        )

        # 3. Delete the VPC
        vpc.delete(self.apiclient)
        self.cleanup.remove(vpc)

        # 4. List subnet for network by vpc cidr, it should be removed
        vpc_cidr = subnets[0].subnet
        subnets = Ipv4SubnetForGuestNetwork.list(
            self.apiclient,
            subnet=vpc_cidr
        )
        self.assertEqual(
            isinstance(subnets, list) and len(subnets) == 1
            and not subnets[0].vpcid and subnets[0].state == "Free",
            True,
            "The subnet should be created for vpc %s" % vpc.name
        )

    @attr(tags=['advanced'], required_hardware=False)
    def test_06_isolated_network_with_routed_mode(self):
        """ Test for Isolated Network with Routed mode"""
        """
            # 1. Create Isolated network
            # 2. Create VM in the network
        """
        self.message("Running test_06_isolated_network_with_routed_mode")

        # 1. Create Isolated network
        global test_network
        test_network = Network.create(
            self.apiclient,
            self.services["network"],
            networkofferingid=self.network_offering_isolated.id,
            zoneid=self.zone.id,
            domainid=self.sub_domain.id,
            accountid=self.regular_user.name,
            gateway=NETWORK_CIDR_PREFIX + ".1",
            netmask="255.255.255.0"
        )
        self._cleanup.append(test_network)

        # 2. Create VM in the network
        global test_network_vm
        test_network_vm = VirtualMachine.create(
            self.regular_user_apiclient,
            self.services["virtual_machine"],
            zoneid=self.zone.id,
            domainid=self.sub_domain.id,
            accountid=self.regular_user.name,
            networkids=test_network.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id)
        self._cleanup.append(test_network_vm)

    @attr(tags=['advanced'], required_hardware=False)
    def test_07_vpc_and_tier_with_routed_mode(self):
        """ Test for VPC/tier with Routed mode"""
        """
            # 1. Create VPC
            # 2. Create Network ACL (egress = Deny, ingress = Deny)
            # 3. Create VPC tier with Network ACL in the VPC
            # 4. Create VM in the VPC tier
        """
        self.message("Running test_07_vpc_and_tier_with_routed_mode")

        # 1. Create VPC
        self.services["vpc"]["cidr"] = VPC_CIDR_PREFIX + ".0.0/22"
        global test_vpc
        test_vpc = VPC.create(self.apiclient,
                              self.services["vpc"],
                              vpcofferingid=self.vpc_offering.id,
                              zoneid=self.zone.id,
                              domainid=self.sub_domain.id,
                              account=self.regular_user.name,
                              start=False
                              )
        self._cleanup.append(test_vpc)

        # 2. Create Network ACL (egress = Deny, ingress = Deny)
        global test_network_acl
        test_network_acl = NetworkACLList.create(self.apiclient,
                                                 services={},
                                                 name="test-network-acl",
                                                 description="test-network-acl",
                                                 vpcid=test_vpc.id
                                                 )

        # 3. Create VPC tier with Network ACL in the VPC
        global test_vpc_tier
        test_vpc_tier = Network.create(self.regular_user_apiclient,
                                       self.services["network"],
                                       networkofferingid=self.vpc_network_offering.id,
                                       zoneid=self.zone.id,
                                       domainid=self.sub_domain.id,
                                       accountid=self.regular_user.name,
                                       vpcid=test_vpc.id,
                                       gateway=VPC_CIDR_PREFIX + ".1.1",
                                       netmask="255.255.255.0",
                                       aclid=test_network_acl.id
                                       )
        self._cleanup.append(test_vpc_tier)

        # 4. Create VM in the VPC tier
        global test_vpc_vm
        test_vpc_vm = VirtualMachine.create(
            self.regular_user_apiclient,
            self.services["virtual_machine"],
            zoneid=self.zone.id,
            domainid=self.sub_domain.id,
            accountid=self.regular_user.name,
            networkids=test_vpc_tier.id,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id)
        self._cleanup.append(test_vpc_vm)

    @attr(tags=['advanced'], required_hardware=False)
    def test_08_vpc_and_tier_failed_cases(self):
        """ Test for VPC/tier with Routed mode (some failed cases)"""
        """
            # 1. create VPC with Routed mode
            # 2. create network offering with NATTED mode, create vpc tier, it should fail
            # 3. create vpc tier not in the vpc cidr, it should fail
        """

        self.message("Running test_08_vpc_and_tier_failed_cases")

        # 1. Create VPC
        self.services["vpc"]["cidr"] = VPC_CIDR_PREFIX + ".8.0/22"
        test_vpc_2 = VPC.create(self.apiclient,
                                self.services["vpc"],
                                vpcofferingid=self.vpc_offering.id,
                                zoneid=self.zone.id,
                                domainid=self.sub_domain.id,
                                account=self.regular_user.name,
                                start=False
                                )
        self.cleanup.append(test_vpc_2)

        # 2. create network offering with NATTED mode, create vpc tier, it should fail
        nw_offering_isolated_vpc = NetworkOffering.create(
            self.apiclient,
            self.services["nw_offering_isolated_vpc"]
        )
        self.cleanup.append(nw_offering_isolated_vpc)
        nw_offering_isolated_vpc.update(self.apiclient, state='Enabled')
        try:
            test_vpc_tier_2 = Network.create(self.regular_user_apiclient,
                                             self.services["network"],
                                             networkofferingid=nw_offering_isolated_vpc.id,
                                             zoneid=self.zone.id,
                                             domainid=self.sub_domain.id,
                                             accountid=self.regular_user.name,
                                             vpcid=test_vpc_2.id,
                                             gateway=VPC_CIDR_PREFIX + ".1.1",
                                             netmask="255.255.255.0"
                                             )
            self.cleanup.append(test_vpc_tier_2)
            self.fail("Created vpc network successfully, but expected to fail")
        except Exception as ex:
            self.message("Failed to create vpc network due to %s, which is expected behaviour" % ex)

        # 3. create vpc tier not in the vpc cidr, it should fail
        try:
            test_vpc_tier_3 = Network.create(self.regular_user_apiclient,
                                             self.services["network"],
                                             networkofferingid=self.vpc_network_offering.id,
                                             zoneid=self.zone.id,
                                             domainid=self.sub_domain.id,
                                             accountid=self.regular_user.name,
                                             vpcid=test_vpc_2.id,
                                             gateway=VPC_CIDR_PREFIX + ".31.1",
                                             netmask="255.255.255.0"
                                             )
            self.cleanup.append(test_vpc_tier_3)
            self.fail("Created vpc network successfully, but expected to fail")
        except Exception as ex:
            self.message("Failed to create vpc network due to %s, which is expected behaviour" % ex)

    @attr(tags=['advanced'], required_hardware=False)
    def test_09_connectivity_between_network_and_vpc_tier(self):
        """ Test for connectivity between VMs in the Isolated Network and VPC/tier"""
        """
            # 0. Get static routes of Network/VPC
            # 1. Add static routes in VRs manually

            # 2. Test VM2 in VR1-Network (ping/ssh should fail)
            # 3. Test VM1 in VR2-VPC (ping/ssh should fail)

            # 4. Create Ingress rules in Network ACL for VPC
            # 5. Create Egress rules in Network ACL for VPC
            # 6. Test VM2 in VR1-Network (ping/ssh should succeed)
            # 7. Test VM1 in VR2-VPC (ping/ssh should fail)

            # 8. Create IPv4 firewalls for Isolated network
            # 9. Test VM2 in VR1-Network (ping/ssh should succeed)
            # 10. Test VM1 in VR2-VPC (ping/ssh should succeed)

            # 11. Delete Network ACL rules for VPC
            # 12. Delete IPv4 firewall rules for Network
            # 13. Test VM2 in VR1-Network (ping/ssh should fail)
            # 14. Test VM1 in VR2-VPC (ping/ssh should fail)

        """
        self.message("Running test_09_connectivity_between_network_and_vpc_tier")

        # 0. Get static routes of Network/VPC
        network_ip4routes = []
        if test_network:
            network_ip4routes = Network.list(
                self.apiclient,
                id=test_network.id,
                listall=True
            )[0].ip4routes
        else:
            self.skipTest("test_network is not created")

        vpc_ip4routes = []
        if test_vpc:
            vpc_ip4routes = VPC.list(
                self.apiclient,
                id=test_vpc.id,
                listall=True
            )[0].ip4routes
        else:
            self.skipTest("test_vpc is not created")

        network_router = self.get_router(networkid=test_network.id)
        vpc_router = self.get_router(vpcid=test_vpc.id)

        # Test VM1 in VR1-Network (wait until ping works)
        self.verifyPingFromRouter(network_router, test_network_vm, retries=MAX_RETRIES)
        # Test VM2 in VR2-VPC (wait until ping works)
        self.verifyPingFromRouter(vpc_router, test_vpc_vm, retries=MAX_RETRIES)

        # 1. Add static routes in VRs manually
        if not network_router or not vpc_router:
            self.skipTest("network_router (%s) or vpc_router (%s) does not exist" % (network_router, vpc_router))
        for ip4route in network_ip4routes:
            self.run_command_in_router(vpc_router, "ip route add %s via %s" % (ip4route.subnet, ip4route.gateway))
        for ip4route in vpc_ip4routes:
            self.run_command_in_router(network_router, "ip route add %s via %s" % (ip4route.subnet, ip4route.gateway))

        # 2. Test VM2 in VR1-Network (ping/ssh should fail)
        self.verifyPingFromRouter(network_router, test_vpc_vm, expected=False)
        # 3. Test VM1 in VR2-VPC (ping/ssh should fail)
        self.verifyPingFromRouter(vpc_router, test_network_vm, expected=False)

        vpc_router_rules = [{"chain": "FORWARD",
                             "rule": "ip daddr %s jump eth2_ingress_policy" % test_vpc_tier.cidr},
                            {"chain": "FORWARD",
                             "rule": "ip saddr %s jump eth2_egress_policy" % test_vpc_tier.cidr}]
        vpc_acl_rules = []
        # 4. Create Ingress rules in Network ACL for VPC
        rule = {}
        rule["traffictype"] = "Ingress"
        rule["cidrlist"] = test_network.cidr
        rule["protocol"] = "icmp"
        rule["icmptype"] = -1
        rule["icmpcode"] = -1
        vpc_acl_rules.append(self.createNetworkAclRule(rule))
        vpc_router_rules.append({"chain": "eth2_ingress_policy",
                                 "rule": "ip saddr %s icmp type %s accept" % (test_network.cidr, ICMPv4_ALL_TYPES)})
        self.verifyNftablesRulesInRouter(vpc_router, vpc_router_rules)

        rule = {}
        rule["traffictype"] = "Ingress"
        rule["cidrlist"] = test_network.cidr
        rule["protocol"] = "tcp"
        rule["startport"] = 22
        rule["endport"] = 22
        vpc_acl_rules.append(self.createNetworkAclRule(rule))
        vpc_router_rules.append({"chain": "eth2_ingress_policy",
                                 "rule": "ip saddr %s tcp dport 22 accept" % test_network.cidr})
        self.verifyNftablesRulesInRouter(vpc_router, vpc_router_rules)

        rule = {}
        rule["traffictype"] = "Ingress"
        rule["cidrlist"] = network_router.publicip + "/32"
        rule["protocol"] = "icmp"
        rule["icmptype"] = -1
        rule["icmpcode"] = -1
        vpc_acl_rules.append(self.createNetworkAclRule(rule))
        vpc_router_rules.append({"chain": "eth2_ingress_policy",
                                 "rule": "ip saddr %s icmp type %s accept" % (network_router.publicip, ICMPv4_ALL_TYPES)})
        self.verifyNftablesRulesInRouter(vpc_router, vpc_router_rules)

        # 5. Create Egress rules in Network ACL for VPC
        rule = {}
        rule["traffictype"] = "Egress"
        rule["protocol"] = "icmp"
        rule["icmptype"] = -1
        rule["icmpcode"] = -1
        vpc_acl_rules.append(self.createNetworkAclRule(rule))
        vpc_router_rules.append({"chain": "eth2_egress_policy",
                                 "rule": "ip daddr 0.0.0.0/0 icmp type %s accept" % ICMPv4_ALL_TYPES})
        self.verifyNftablesRulesInRouter(vpc_router, vpc_router_rules)

        # 6. Test VM2 in VR1-Network (ping/ssh should succeed)
        self.verifyPingFromRouter(network_router, test_vpc_vm, expected=True)
        # 7. Test VM1 in VR2-VPC (ping/ssh should fail)
        self.verifyPingFromRouter(vpc_router, test_network_vm, expected=False)

        network_router_rules = [{"chain": "FORWARD",
                                 "rule": "ip daddr %s jump fw_chain_ingress" % test_network.cidr},
                                {"chain": "FORWARD",
                                 "rule": "ip saddr %s jump fw_chain_egress" % test_network.cidr}]
        network_routing_firewall_rules = []
        # 8. Create IPv4 firewalls for Isolated network
        rule = {}
        rule["traffictype"] = "Ingress"
        rule["cidrlist"] = test_vpc.cidr
        rule["protocol"] = "icmp"
        rule["icmptype"] = -1
        rule["icmpcode"] = -1
        network_routing_firewall_rules.append(self.createIpv4RoutingFirewallRule(rule))
        network_router_rules.append({"chain": "fw_chain_ingress",
                                     "rule": "ip saddr %s ip daddr 0.0.0.0/0 icmp type %s accept" % (test_vpc.cidr, ICMPv4_ALL_TYPES)})
        self.verifyNftablesRulesInRouter(network_router, network_router_rules)

        rule = {}
        rule["traffictype"] = "Ingress"
        rule["cidrlist"] = test_vpc.cidr
        rule["protocol"] = "tcp"
        rule["startport"] = 22
        rule["endport"] = 22
        network_routing_firewall_rules.append(self.createIpv4RoutingFirewallRule(rule))
        network_router_rules.append({"chain": "fw_chain_ingress",
                                     "rule": "ip saddr %s ip daddr 0.0.0.0/0 tcp dport 22 accept" % test_vpc.cidr})
        self.verifyNftablesRulesInRouter(network_router, network_router_rules)

        rule = {}
        rule["traffictype"] = "Ingress"
        rule["cidrlist"] = vpc_router.publicip + "/32"
        rule["protocol"] = "icmp"
        rule["icmptype"] = -1
        rule["icmpcode"] = -1
        network_routing_firewall_rules.append(self.createIpv4RoutingFirewallRule(rule))
        network_router_rules.append({"chain": "fw_chain_ingress",
                                     "rule": "ip saddr %s ip daddr 0.0.0.0/0 icmp type %s accept" % (vpc_router.publicip, ICMPv4_ALL_TYPES)})
        self.verifyNftablesRulesInRouter(network_router, network_router_rules)

        # 9. Test VM2 in VR1-Network (ping/ssh should succeed)
        self.verifyPingFromRouter(network_router, test_vpc_vm, expected=True)
        # 10. Test VM1 in VR2-VPC (ping/ssh should succeed)
        self.verifyPingFromRouter(vpc_router, test_network_vm, expected=True)

        # 11. Delete Network ACL rules for VPC
        for rule in vpc_acl_rules:
            rule.delete(self.apiclient)
        vpc_router_rules[2] = {"chain": "eth2_ingress_policy",
                               "rule": "ip saddr %s icmp type %s accept" % (test_network.cidr, ICMPv4_ALL_TYPES),
                               "exists": False}
        vpc_router_rules[3] = {"chain": "eth2_ingress_policy",
                               "rule": "ip saddr %s tcp dport 22 accept" % test_network.cidr,
                               "exists": False}
        vpc_router_rules[4] = {"chain": "eth2_egress_policy",
                               "rule": "ip daddr 0.0.0.0/0 icmp type %s accept" % ICMPv4_ALL_TYPES,
                               "exists": False}
        vpc_router_rules[5] = {"chain": "eth2_ingress_policy",
                               "rule": "ip saddr %s icmp type %s accept" % (network_router.publicip, ICMPv4_ALL_TYPES),
                               "exists": False}
        self.verifyNftablesRulesInRouter(vpc_router, vpc_router_rules)

        # 12. Delete IPv4 firewall rules for Network
        for rule in network_routing_firewall_rules:
            rule.delete(self.apiclient)
        network_router_rules[2] = {"chain": "fw_chain_ingress",
                                   "rule": "ip saddr %s ip daddr 0.0.0.0/0 icmp type %s accept" % (test_vpc.cidr, ICMPv4_ALL_TYPES),
                                   "exists": False}
        network_router_rules[3] = {"chain": "fw_chain_ingress",
                                   "rule": "ip saddr %s ip daddr 0.0.0.0/0 tcp dport 22 accept" % test_vpc.cidr,
                                   "exists": False}
        network_router_rules[4] = {"chain": "fw_chain_ingress",
                                   "rule": "ip saddr %s ip daddr 0.0.0.0/0 icmp type %s accept" % (vpc_router.publicip, ICMPv4_ALL_TYPES),
                                   "exists": False}
        self.verifyNftablesRulesInRouter(network_router, network_router_rules)

        # 13. Test VM2 in VR1-Network (ping/ssh should fail)
        self.verifyPingFromRouter(network_router, test_vpc_vm, expected=False)
        # 14. Test VM1 in VR2-VPC (ping/ssh should fail)
        self.verifyPingFromRouter(vpc_router, test_network_vm, expected=False)
