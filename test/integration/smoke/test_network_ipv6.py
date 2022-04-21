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
""" BVT tests for Network offerings"""

#Import Local Modules
from marvin.codes import FAILED
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (createGuestNetworkIpv6Prefix,
                                  listGuestNetworkIpv6Prefixes,
                                  deleteGuestNetworkIpv6Prefix,
                                  listIpv6FirewallRules,
                                  createIpv6FirewallRule,
                                  deleteIpv6FirewallRule)
from marvin.lib.utils import (isAlmostEqual,
                              cleanup_resources,
                              random_gen,
                              get_process_status,
                              get_host_credentials)
from marvin.lib.base import (Configurations,
                             Domain,
                             NetworkOffering,
                             VpcOffering,
                             Account,
                             PublicIpRange,
                             Network,
                             Router,
                             ServiceOffering,
                             VirtualMachine,
                             NIC,
                             Host)
from marvin.lib.common import (get_domain,
                               get_zone,
                               list_hosts,
                               get_test_template,
                               get_template)
from marvin.sshClient import SshClient
from marvin.cloudstackException import CloudstackAPIException
from marvin.lib.decoratorGenerators import skipTestIf

from nose.plugins.attrib import attr
from ipaddress import IPv6Network
from random import getrandbits, choice, randint
import time
import logging

ipv6_offering_config_name = "ipv6.offering.enabled"
ULA_BASE = IPv6Network("fd00::/8")
PREFIX_OPTIONS = [i for i in range(48, 65, 4)]
FIREWALL_TABLE = "ip6_firewall"
FIREWALL_CHAINS = {
    "Ingress": "fw_chain_ingress",
    "Egress": "fw_chain_egress"
}
CIDR_IPV6_ANY = "::/0"
ICMPV6_TYPE = {
    1: "destination-unreachable",
    2: "packet-too-big",
    3: "time-exceeded",
    4: "parameter-problem",
    128: "echo-request",
    129: "echo-reply",
    130: "mld-listener-query",
    131: "mld-listener-report",
    132: "mld-listener-done",
    132: "mld-listener-reduction",
    133: "nd-router-solicit",
    134: "nd-router-advert",
    135: "nd-neighbor-solicit",
    136: "nd-neighbor-advert",
    137: "nd-redirect",
    138: "router-renumbering",
    141: "ind-neighbor-solicit",
    142: "ind-neighbor-advert",
    143: "mld2-listener-report"
}
ICMPV6_CODE_TYPE = {
    0: "no-route",
    1: "admin-prohibited",
    3: "addr-unreachable",
    4: "port-unreachable",
    5: "policy-fail",
    6: "reject-route"
}
ICMPV6_TYPE_ANY = "{ destination-unreachable, packet-too-big, time-exceeded, parameter-problem, echo-request, echo-reply, mld-listener-query, mld-listener-report, mld-listener-done, nd-router-solicit, nd-router-advert, nd-neighbor-solicit, nd-neighbor-advert, nd-redirect, router-renumbering }"
TCP_UDP_PORT_ANY = "{ 0-65535 }"

class TestCreateIpv6NetworkVpcOffering(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestCreateIpv6NetworkVpcOffering, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.initial_ipv6_offering_enabled = Configurations.list(
            cls.apiclient,
            name=ipv6_offering_config_name)[0].value
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        if cls.initial_ipv6_offering_enabled != None:
            Configurations.update(cls.apiclient,
                ipv6_offering_config_name,
                cls.initial_ipv6_offering_enabled)
        super(TestCreateIpv6NetworkVpcOffering, cls).tearDownClass()

    def setUp(self):
        self.services = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns",
            "smoke"],
        required_hardware="false")
    def test_01_create_ipv6_network_offering(self):
        """Test to create network offering

        # Validate the following:
        # 1. createNetworkOffering should return valid info for new offering
        # 2. The Cloud Database contains the valid information
        """
        Configurations.update(self.apiclient,
            ipv6_offering_config_name,
            "true")
        ipv6_service = self.services["network_offering"]
        ipv6_service["internetprotocol"] = "dualstack"
        network_offering = NetworkOffering.create(
            self.apiclient,
            ipv6_service
        )
        self.cleanup.append(network_offering)

        self.debug("Created Network offering with ID: %s" % network_offering.id)

        list_network_off_response = NetworkOffering.list(self.apiclient,
            id=network_offering.id)
        self.assertEqual(
            isinstance(list_network_off_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(list_network_off_response),
            0,
            "Check Network offering is created"
        )
        network_off_response = list_network_off_response[0]

        self.assertEqual(
            network_off_response.id,
            network_offering.id,
            "Check server id in listNetworkOfferings"
        )
        self.assertEqual(
            network_off_response.internetprotocol.lower(),
            ipv6_service["internetprotocol"].lower(),
            "Check internetprotocol in listNetworkOfferings"
        )
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns",
            "smoke"],
        required_hardware="false")
    def test_02_create_ipv6_network_offering_fail(self):
        """Test to create network offering

        # Validate the following:
        # 1. createNetworkOffering should fail
        """
        Configurations.update(self.apiclient,
            ipv6_offering_config_name,
            "false")
        ipv6_service = self.services["network_offering"]
        ipv6_service["internetprotocol"] = "dualstack"
        try:
            network_offering = NetworkOffering.create(
                self.apiclient,
                ipv6_service
            )
            self.cleanup.append(network_offering)
            self.fail("Network offering created despite global setting - %s set to false" % ipv6_offering_config_name)
        except CloudstackAPIException as e:
            self.debug("Network offering creation failed as expected %s " % e)
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns",
            "smoke"],
        required_hardware="false")
    def test_03_create_ipv6_vpc_offering(self):
        """Test to create network offering

        # Validate the following:
        # 1. createVpcOffering should return valid info for new offering
        # 2. The Cloud Database contains the valid information
        """
        Configurations.update(self.apiclient,
            ipv6_offering_config_name,
            "true")
        ipv6_service = self.services["vpc_offering"]
        ipv6_service["internetprotocol"] = "dualstack"
        vpc_offering = VpcOffering.create(
            self.apiclient,
            ipv6_service
        )
        self.cleanup.append(vpc_offering)

        self.debug("Created VPC offering with ID: %s" % vpc_offering.id)

        list_vpc_off_response = VpcOffering.list(self.apiclient,
            id=vpc_offering.id)
        self.assertEqual(
            isinstance(list_vpc_off_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(list_vpc_off_response),
            0,
            "Check VPC offering is created"
        )
        vpc_off_response = list_vpc_off_response[0]
        self.assertEqual(
            vpc_off_response.id,
            vpc_offering.id,
            "Check server id in listVpcOfferings"
        )
        self.assertEqual(
            vpc_off_response.internetprotocol.lower(),
            ipv6_service["internetprotocol"].lower(),
            "Check internetprotocol in listVpcOfferings"
        )
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns",
            "smoke"],
        required_hardware="false")
    def test_04_create_ipv6_vpc_offering_fail(self):
        """Test to create VPC offering failure

        # Validate the following:
        # 1. createVpcOffering should fail
        """
        Configurations.update(self.apiclient,
            ipv6_offering_config_name,
            "false")
        ipv6_service = self.services["vpc_offering"]
        ipv6_service["internetprotocol"] = "dualstack"
        try:
            vpc_offering = VpcOffering.create(
                self.apiclient,
                ipv6_service
            )
            self.cleanup.append(vpc_offering)
            self.fail("VPC offering created despite global setting - %s set to false" % ipv6_offering_config_name)
        except CloudstackAPIException as e:
            self.debug("VPC offering creation failed as expected %s " % e)
        return

class TestIpv6PublicIpRange(cloudstackTestCase):

    def setUp(self):
        self.services = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        testClient = super(TestIpv6PublicIpRange, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        super(TestIpv6PublicIpRange, cls).tearDownClass()

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns",
            "smoke"],
        required_hardware="false")
    def test_01_create_ipv6_public_ip_range(self):
        """Test to add IPv6 public IP range

        # Validate the following:
        # 1. createVlanIpRange should return valid info for new public range
        # 2. The Cloud Database contains the valid information
        """
        ipv6_publiciprange_service = self.services["publicip6range"]
        ipv6_publiciprange_service["zoneid"] = self.zone.id
        ipv6_publiciprange = PublicIpRange.create(
            self.apiclient,
            ipv6_publiciprange_service
        )
        self.cleanup.append(ipv6_publiciprange)

        self.debug("Created IPv6 public IP range with ID: %s" % ipv6_publiciprange.vlan.id)
        ipv6_publiciprange = ipv6_publiciprange.vlan

        public_ip_ranges = PublicIpRange.list(
                    self.apiclient,
                    id=ipv6_publiciprange.id
                )
        self.assertEqual(
            isinstance(public_ip_ranges, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(public_ip_ranges),
            0,
            "Check public IP range is created"
        )
        public_ip_range = public_ip_ranges[0]

        self.assertEqual(
            public_ip_range.id,
            ipv6_publiciprange.id,
            "Check server id"
        )
        self.assertEqual(
            public_ip_range.ip6cidr,
            ipv6_publiciprange_service["ip6cidr"],
            "Check ip6cidr for IPv6 public IP range"
        )
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns",
            "smoke"],
        required_hardware="false")
    def test_02_create_ipv6_public_ip_range_fail(self):
        """Test to add IPv6 public IP range failure

        # Validate the following:
        # 1. createVlanIpRange should return valid info for new public range
        # 2. The Cloud Database contains the valid information
        """
        ipv6_publiciprange_service = self.services["publicip6range"]
        cidr = ipv6_publiciprange_service["ip6cidr"]
        x = cidr.split("/")
        x[1] = "72"
        cidr = "/".join(x)
        ipv6_publiciprange_service["ip6cidr"] = cidr
        ipv6_publiciprange_service["zoneid"] = self.zone.id
        try:
            ipv6_publiciprange = PublicIpRange.create(
                self.apiclient,
                ipv6_publiciprange_service
            )
        except Exception as e:
            self.debug("IPv6 public range creation failed as expected %s " % e)
            ipv6_publiciprange = None
        if ipv6_publiciprange != None:
            self.debug("Created IPv6 public range with ID: %s. Deleting it before failure" % ipv6_publiciprange.id)
            self.cleanup.append(ipv6_publiciprange)
            self.fail("IPv6 guest prefix created despite CIDR size greater than 64")
        return

class TestIpv6GuestPrefix(cloudstackTestCase):

    def setUp(self):
        self.services = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        testClient = super(TestIpv6GuestPrefix, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        super(TestIpv6GuestPrefix, cls).tearDownClass()

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns",
            "smoke"],
        required_hardware="false")
    def test_01_create_ipv6_guest_prefix(self):
        """Test to add IPv6 guest prefix

        # Validate the following:
        # 1. createGuestNetworkIpv6Prefix should return valid info for new IPv6 prefix
        # 2. The Cloud Database contains the valid information
        """
        ipv6_guestprefix_service = self.services["guestip6prefix"]
        cmd = createGuestNetworkIpv6Prefix.createGuestNetworkIpv6PrefixCmd()
        cmd.zoneid = self.zone.id
        cmd.prefix = ipv6_guestprefix_service["prefix"]
        ipv6_guestprefix = self.apiclient.createGuestNetworkIpv6Prefix(cmd)

        self.debug("Created IPv6 guest prefix with ID: %s" % ipv6_guestprefix.id)

        cmd = listGuestNetworkIpv6Prefixes.listGuestNetworkIpv6PrefixesCmd()
        cmd.id = ipv6_guestprefix.id
        ipv6_guestprefixes = self.apiclient.listGuestNetworkIpv6Prefixes(cmd)
        self.assertEqual(
            isinstance(ipv6_guestprefixes, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(ipv6_guestprefixes),
            0,
            "Check guest IPv6 prefix is created"
        )
        ipv6_guestprefix_response = ipv6_guestprefixes[0]

        self.assertEqual(
            ipv6_guestprefix.id,
            ipv6_guestprefix_response.id,
            "Check server id"
        )
        self.assertEqual(
            ipv6_guestprefix_response.prefix,
            ipv6_guestprefix_service["prefix"],
            "Check prefix for IPv6"
        )

        cmd = deleteGuestNetworkIpv6Prefix.deleteGuestNetworkIpv6PrefixCmd()
        cmd.id = ipv6_guestprefix.id
        self.apiclient.deleteGuestNetworkIpv6Prefix(cmd)
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns",
            "smoke"],
        required_hardware="false")
    def test_02_create_ipv6_guest_prefix_fail(self):
        """Test to add IPv6 guest prefix failure

        # Validate the following:
        # 1. createGuestNetworkIpv6Prefix should fail
        """
        ipv6_guestprefix_service = self.services["guestip6prefix"]
        cmd = createGuestNetworkIpv6Prefix.createGuestNetworkIpv6PrefixCmd()
        cmd.zoneid = self.zone.id
        prefix = ipv6_guestprefix_service["prefix"]
        x = prefix.split("/")
        x[1] = "72"
        prefix = "/".join(x)
        cmd.prefix = prefix
        try:
            ipv6_guestprefix = self.apiclient.createGuestNetworkIpv6Prefix(cmd)
        except Exception as e:
            self.debug("IPv6 guest prefix creation failed as expected %s " % e)
            ipv6_guestprefix = None
        if ipv6_guestprefix != None:
            self.debug("Created IPv6 guest prefix with ID: %s. Deleting it before failure" % ipv6_guestprefix.id)
            cmd = deleteGuestNetworkIpv6Prefix.deleteGuestNetworkIpv6PrefixCmd()
            cmd.id = ipv6_guestprefix.id
            self.apiclient.deleteGuestNetworkIpv6Prefix(cmd)
            self.fail("IPv6 guest prefix created despite CIDR size greater than 64")
        return

class TestIpv6Network(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestIpv6Network, cls).getClsTestClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.apiclient = testClient.getApiClient()
        cls.dbclient = testClient.getDbConnection()
        cls.test_ipv6_guestprefix = None
        cls.initial_ipv6_offering_enabled = None
        cls._cleanup = []
        cls.routerDetailsMap = {}

        cls.logger = logging.getLogger('TestIpv6Network')

        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.ipv6NotSupported = False

        ipv6_guestprefix = cls.getGuestIpv6Prefix()
        if ipv6_guestprefix == None:
            cls.ipv6NotSupported = True
        if cls.ipv6NotSupported == False:
            ipv6_publiciprange = cls.getPublicIpv6Range()
            if ipv6_publiciprange == None:
                cls.ipv6NotSupported = True

        if cls.ipv6NotSupported == False:
            cls.initial_ipv6_offering_enabled = Configurations.list(
                cls.apiclient,
                name=ipv6_offering_config_name)[0].value
            Configurations.update(cls.apiclient,
                ipv6_offering_config_name,
                "true")
            cls.domain = get_domain(cls.apiclient)
            cls.account = Account.create(
                cls.apiclient,
                cls.services["account"],
                admin=True,
                domainid=cls.domain.id
            )
            cls._cleanup.append(cls.account)
            cls.hypervisor = testClient.getHypervisorInfo()
            cls.template = get_template(
                cls.apiclient,
                cls.zone.id,
                cls.services["ostype"]
            )
        else:
            cls.debug("IPv6 is not supported, skipping tests!")
        return

    @classmethod
    def tearDownClass(cls):
        if cls.initial_ipv6_offering_enabled != None:
            Configurations.update(cls.apiclient,
                ipv6_offering_config_name,
                cls.initial_ipv6_offering_enabled)
        super(TestIpv6Network, cls).tearDownClass()
        if cls.test_ipv6_guestprefix != None:
            cmd = deleteGuestNetworkIpv6Prefix.deleteGuestNetworkIpv6PrefixCmd()
            cmd.id = cls.test_ipv6_guestprefix.id
            cls.apiclient.deleteGuestNetworkIpv6Prefix(cmd)

    @classmethod
    def getGuestIpv6Prefix(cls):
        cmd = listGuestNetworkIpv6Prefixes.listGuestNetworkIpv6PrefixesCmd()
        cmd.zoneid = cls.zone.id
        ipv6_prefixes_response = cls.apiclient.listGuestNetworkIpv6Prefixes(cmd)
        if isinstance(ipv6_prefixes_response, list) == True and len(ipv6_prefixes_response) > 0:
            return ipv6_prefixes_response[0]
        ipv6_guestprefix_service = cls.services["guestip6prefix"]
        cmd = createGuestNetworkIpv6Prefix.createGuestNetworkIpv6PrefixCmd()
        cmd.zoneid = cls.zone.id
        cmd.prefix = ipv6_guestprefix_service["prefix"]
        ipv6_guestprefix = cls.apiclient.createGuestNetworkIpv6Prefix(cmd)
        cls.test_ipv6_guestprefix = ipv6_guestprefix
        return ipv6_guestprefix

    @classmethod
    def getPublicIpv6Range(cls):
        list_public_ip_range_response = PublicIpRange.list(
            cls.apiclient,
            zoneid=cls.zone.id
        )
        ipv4_range_vlan = None
        if isinstance(list_public_ip_range_response, list) == True and len(list_public_ip_range_response) > 0:
            for ip_range in list_public_ip_range_response:
                if ip_range.ip6cidr != None and ip_range.ip6gateway != None:
                    return ip_range
                if ip_range.netmask != None and ip_range.gateway != None:
                    vlan = ip_range.vlan
                    if ipv4_range_vlan == None and vlan.startswith("vlan://"):
                        vlan = vlan.replace("vlan://", "")
                        ipv4_range_vlan = int(vlan)
        ipv6_publiciprange_service = cls.services["publicip6range"]
        ipv6_publiciprange_service["zoneid"] = cls.zone.id
        ipv6_publiciprange_service["vlan"] = ipv4_range_vlan
        ipv6_publiciprange = PublicIpRange.create(
            cls.apiclient,
            ipv6_publiciprange_service
        )
        cls._cleanup.append(ipv6_publiciprange)
        return ipv6_publiciprange

    def setUp(self):
        self.services = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, reversed(self.cleanup))

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def getRandomIpv6Cidr(self):
        prefix_length = choice(PREFIX_OPTIONS)
        random_suffix = getrandbits(40) << (128-prefix_length)
        base_address = ULA_BASE.network_address + random_suffix
        return str(IPv6Network((base_address, prefix_length)))

    def createTinyServiceOffering(self):
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["big"],
        )
        self.cleanup.append(self.service_offering)

    def createIpv6NetworkOfferingInternal(self, is_redundant, egressdefaultpolicy=True):
        ipv6_service = self.services["network_offering"]
        if is_redundant:
            ipv6_service = self.services["nw_off_isolated_RVR"]
        ipv6_service["internetprotocol"] = "dualstack"
        if egressdefaultpolicy:
            ipv6_service["egress_policy"] = egressdefaultpolicy
        network_offering = NetworkOffering.create(
            self.apiclient,
            ipv6_service
        )
        network_offering.update(self.apiclient, state='Enabled')
        return network_offering

    def createIpv6NetworkOffering(self, is_redundant):
        self.network_offering = self.createIpv6NetworkOfferingInternal(is_redundant, False)
        self.cleanup.append(self.network_offering)

    def createIpv6NetworkOfferingForUpdate(self, is_redundant):
        self.network_offering_update = self.createIpv6NetworkOfferingInternal(is_redundant)
        self.cleanup.append(self.network_offering_update)


    def deployIpv6Network(self):
        self.services["network"]["networkoffering"] = self.network_offering.id
        self.network = Network.create(
            self.apiclient,
            self.services["network"],
            self.account.name,
            self.account.domainid,
            zoneid=self.zone.id
        )
        self.cleanup.append(self.network)

    def deployIpv6NetworkVm(self):
        if self.template == FAILED:
            assert False, "get_test_template() failed to return template"
        self.services["virtual_machine"]["zoneid"] = self.zone.id
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=self.network.id,
            serviceofferingid=self.service_offering.id
        )
        self.cleanup.append(self.virtual_machine)

    def checkIpv6NetworkBasic(self):
        self.debug("Listing network: %s" % (self.network.name))
        ipv6_network = Network.list(self.apiclient,listall="true",id=self.network.id)
        self.assertTrue(
            isinstance(ipv6_network, list),
            "Check listNetworks response returns a valid list"
        )
        self.assertEqual(
            len(ipv6_network),
            1,
            "Network not found"
        )
        ipv6_network = ipv6_network[0]
        self.assertNotEqual(ipv6_network,
                    None,
                    "User is not able to retrieve network details %s" % self.network.id)
        self.assertNotEqual(ipv6_network.ip6cidr,
                    None,
                    "IPv6 CIDR for network is empty")
        self.assertNotEqual(ipv6_network.ip6gateway,
                    None,
                    "IPv6 gateway for network is empty")
        self.assertNotEqual(ipv6_network.ip6routes,
                    None,
                    "IPv6 routes for network is empty")
        self.network_ipv6_routes = ipv6_network.ip6routes

    def checkIpv6NetworkRoutersBasic(self):
        self.debug("Listing routers for network: %s" % self.network.name)
        self.routers = Router.list(
            self.apiclient,
            networkid=self.network.id,
            listall=True
        )
        self.assertTrue(
            isinstance(self.routers, list),
            "Check listRouters response returns a valid list"
        )
        self.assertTrue(
            len(self.routers) > 0,
            "Router for the network isn't found"
        )
        for router in self.routers:
            self.assertFalse(
                router.isredundantrouter == True and router.redundantstate == "FAULT",
                "Router for the network is in FAULT state"
            )
            nics = router.nic
            for nic in nics:
                if (nic.traffictype == 'Guest' and router.isredundantrouter == False) or nic.traffictype == 'Public':
                    self.assertNotEqual(nic.ip6address,
                                None,
                                "IPv6 address for router %s NIC is empty" % nic.traffictype)
                    self.assertNotEqual(nic.ip6cidr,
                                None,
                                "IPv6 CIDR for router %s NIC is empty" % nic.traffictype)
                    self.assertNotEqual(nic.ip6gateway,
                                None,
                                "IPv6 gateway for router %s NIC is empty" % nic.traffictype)


    def getRouterProcessStatus(self, router, cmd):
        if router.id not in self.routerDetailsMap or self.routerDetailsMap[router.id] is None:
            connect_ip = self.apiclient.connection.mgtSvr
            connect_user = self.apiclient.connection.user
            connect_passwd = self.apiclient.connection.passwd
            hypervisor = self.hypervisor
            if self.hypervisor.lower() not in ('vmware', 'hyperv'):
                hosts = Host.list(
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
                connect_ip = host.ipaddress
                hypervisor = None
                try:
                    connect_user, connect_passwd= get_host_credentials(
                        self.config, host.ipaddress)
                except KeyError:
                    self.skipTest(
                        "Marvin configuration has no host credentials to\
                                check router services")
            details = {}
            details['connect_ip'] = connect_ip
            details['connect_user'] = connect_user
            details['connect_passwd'] = connect_passwd
            details['hypervisor'] = hypervisor
            self.routerDetailsMap[router.id] = details
        result = get_process_status(
            self.routerDetailsMap[router.id]['connect_ip'],
            22,
            self.routerDetailsMap[router.id]['connect_user'],
            self.routerDetailsMap[router.id]['connect_passwd'],
            router.linklocalip,
            cmd,
            hypervisor=self.routerDetailsMap[router.id]['hypervisor']
        )
        self.assertTrue(type(result) == list and len(result) > 0,
            "%s on router %s returned invalid result" % (cmd, router.id))
        result = '\n'.join(result)
        return result

    def getNetworkRouter(self, network, red_state="PRIMARY"):
        routers = Router.list(
            self.apiclient,
            networkid=network.id,
            listall=True
        )
        self.assertTrue(
            isinstance(routers, list) and len(routers) > 0,
            "No routers found for network %s" % network.id
        )
        if len(routers) == 1:
            return routers[0]
        for router in routers:
            if router.redundantstate == red_state:
                return router

    def getNetworkRoutes(self, network):
        ipv6_network = Network.list(self.apiclient,listall="true",id=network.id)
        self.assertTrue(
            isinstance(ipv6_network, list),
            "Check listNetworks response returns a valid list"
        )
        self.assertEqual(
            len(ipv6_network),
            1,
            "Network not found"
        )
        ipv6_network = ipv6_network[0]
        self.assertNotEqual(ipv6_network.ip6routes,
                    None,
                    "IPv6 routes for network is empty")
        return ipv6_network.ip6routes

    def isNetworkEgressDefaultPolicyAllow(self, network):
        ipv6_network = Network.list(self.apiclient,listall="true",id=network.id)
        if len(ipv6_network) == 1:
            ipv6_network = ipv6_network[0]
            return ipv6_network.egressdefaultpolicy
        return False

    def checkRouterNicState(self, router, dev, state):
        st = "state %s" % state
        cmd = "ip link show %s | grep '%s'" % (dev, st)
        res = self.getRouterProcessStatus(router, cmd)
        self.assertTrue(type(res) == str and len(res) > 0 and st in res,
            "%s failed on router %s" % (cmd, router.id))

    def checkIpv6NetworkPrimaryRouter(self, router):
        self.checkRouterNicState(router, "eth0", "UP")
        guest_gateway_check_cmd = "ip -6 address show %s | grep 'inet6 %s'" % ("eth0", self.network.ip6gateway)
        res = self.getRouterProcessStatus(router, guest_gateway_check_cmd)
        self.assertTrue(type(res) == str and len(res) > 0 and self.network.ip6gateway in res,
            "%s failed on router %s" % (guest_gateway_check_cmd, router.id))
        self.assertFalse("dadfailed" in res,
            "dadfailed for IPv6 guest gateway on router %s" % router.id)
        self.checkRouterNicState(router, "eth2", "UP")
        public_ipv6 = None
        public_ipv6_gateway = None
        nics = router.nic
        for nic in nics:
            if nic.traffictype == 'Public':
                public_ipv6 = nic.ip6address
                public_ipv6_gateway = nic.ip6gateway
                break
        self.assertNotEqual(public_ipv6,
            None,
            "IPv6 address for router Public NIC is empty")
        public_ip_check_cmd = "ip -6 address show %s | grep 'inet6 %s'" % ("eth2", public_ipv6)
        res = self.getRouterProcessStatus(router, public_ip_check_cmd)
        self.assertTrue(type(res) == str and len(res) > 0 and public_ipv6 in res,
            "%s failed on router %s" % (public_ip_check_cmd, router.id))
        self.assertFalse("dadfailed" in res,
            "dadfailed for public IPv6 on router %s" % router.id)
        self.assertNotEqual(public_ipv6_gateway,
            None,
            "IPv6 gateway for router Public NIC is empty")
        default_route_check_cmd = "ip -6 route | grep 'default via %s'" % (public_ipv6_gateway)
        res = self.getRouterProcessStatus(router, default_route_check_cmd)
        self.assertTrue(type(res) == str and len(res) > 0 and public_ipv6_gateway in res,
            "%s failed on router %s" % (default_route_check_cmd, router.id))

    def checkIpv6NetworkBackupRouter(self, router):
        self.checkRouterNicState(router, "eth0", "UP")
        guest_gateway_check_cmd = "ip -6 address show %s | grep 'inet6 %s'" % ("eth0", self.network.ip6gateway)
        res = self.getRouterProcessStatus(router, guest_gateway_check_cmd)
        self.assertFalse(type(res) == str and len(res) > 0 and self.network.ip6gateway in res,
            "%s failed on router %s" % (guest_gateway_check_cmd, router.id))
        self.checkRouterNicState(router, "eth2", "DOWN")

    def checkIpv6NetworkRoutersInternal(self):
        for router in self.routers:
            if router.state != "Running":
                continue
            if router.isredundantrouter == True and router.redundantstate == 'BACKUP':
                self.checkIpv6NetworkBackupRouter(router)
                continue
            self.checkIpv6NetworkPrimaryRouter(router)


    def checkIpv6NetworkVm(self):
        self.debug("Listing NICS for VM %s in network: %s" % (self.virtual_machine.name, self.network.name))
        nics = NIC.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            networkid=self.network.id
        )
        self.assertEqual(
            len(nics),
            1,
            "Router for the network isn't found"
        )
        nic = nics[0]
        self.assertNotEqual(nic.ip6address,
                    None,
                    "IPv6 address for VM %s NIC is empty" % nic.traffictype)
        self.virtual_machine_ipv6_address = nic.ip6address
        self.assertNotEqual(nic.ip6cidr,
                    None,
                    "IPv6 CIDR for VM %s NIC is empty" % nic.traffictype)
        self.assertNotEqual(nic.ip6gateway,
                    None,
                    "IPv6 gateway for VM %s NIC is empty" % nic.traffictype)

    def restartNetworkWithCleanup(self):
        self.network.restart(self.apiclient, cleanup=True)

    def updateNetworkWithOffering(self):
        self.network.update(self.apiclient, networkofferingid=self.network_offering_update.id)

    def createIpv6FirewallRuleInNetwork(self, network_id, traffic_type, source_cidr, dest_cidr, protocol,
        start_port, end_port, icmp_type, icmp_code):
        cmd = createIpv6FirewallRule.createIpv6FirewallRuleCmd()
        cmd.networkid = network_id
        cmd.traffictype = traffic_type
        if source_cidr:
            cmd.cidrlist = source_cidr
        if dest_cidr:
            cmd.destcidrlist = dest_cidr
        if protocol:
            cmd.protocol = protocol
        if start_port:
            cmd.startport = start_port
        if end_port:
            cmd.endport = end_port
        if icmp_type is not None:
            cmd.icmptype = icmp_type
        if icmp_code is not None:
            cmd.icmpcode = icmp_code
        fw_rule = self.apiclient.createIpv6FirewallRule(cmd)
        return fw_rule

    def checkNetworkRouting(self):
        self.routing_test_network_offering = self.createIpv6NetworkOfferingInternal(False, True)
        self.cleanup.append(self.routing_test_network_offering)
        self.services["network"]["networkoffering"] = self.routing_test_network_offering.id
        self.routing_test_network = Network.create(
            self.apiclient,
            self.services["network"],
            self.account.name,
            self.account.domainid,
            zoneid=self.zone.id
        )
        self.cleanup.append(self.routing_test_network)
        self.services["virtual_machine"]["zoneid"] = self.zone.id
        self.routing_test_vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=[self.routing_test_network.id],
            serviceofferingid=self.service_offering.id,
            mode="advanced"
        )
        self.cleanup.append(self.routing_test_vm)

        fw1 = self.createIpv6FirewallRuleInNetwork(self.routing_test_network.id, "Ingress", None, None, "icmp",
            None, None, None, None)
        fw2 = self.createIpv6FirewallRuleInNetwork(self.network.id, "Ingress", None, None, "icmp",
            None, None, None, None)

        router = self.getNetworkRouter(self.routing_test_network)
        self.logger.debug("Adding network routes in routing_test_network %s" % self.network_ipv6_routes)
        for route in self.network_ipv6_routes:
            add_route_cmd = "ip -6 route add %s via %s" % (route.subnet, route.gateway)
            self.getRouterProcessStatus(router, add_route_cmd)

        router = self.getNetworkRouter(self.network)
        routes = self.getNetworkRoutes(self.routing_test_network)
        self.logger.debug("Adding routing_test_network routes in network %s" % routes)
        for route in routes:
            add_route_cmd = "ip -6 route add %s via %s" % (route.subnet, route.gateway)
            self.getRouterProcessStatus(router, add_route_cmd)

        time.sleep(self.services["sleep"])

        ping_cmd = "ping6 -c 4 %s" % self.virtual_machine_ipv6_address
        res = self.getRouterProcessStatus(router, ping_cmd)
        self.assertTrue(" 0% packet loss" in res,
            "Ping from router %s of network %s to VM %s of network %s is unsuccessful" % (router.id, self.routing_test_network.id, self.virtual_machine.id, self.network.id))

        ssh = self.routing_test_vm.get_ssh_client(retries=5)
        res = ssh.execute(ping_cmd)
        self.assertTrue(type(res) == list and len(res) > 0,
            "%s on VM %s returned invalid result" % (ping_cmd, self.routing_test_vm.id))
        self.logger.debug(res)
        res = '\n'.join(res)
        self.assertTrue(" 0% packet loss" in res,
            "Ping from VM %s of network %s to VM %s of network %s is unsuccessful" % (self.routing_test_vm.id, self.routing_test_network.id, self.virtual_machine.id, self.network.id))

        cmd = deleteIpv6FirewallRule.deleteIpv6FirewallRuleCmd()
        cmd.id = fw2.id
        self.apiclient.deleteIpv6FirewallRule(cmd)

    def createAndVerifyIpv6FirewallRule(self, traffic_type, source_cidr, dest_cidr, protocol,
        start_port, end_port, icmp_type, icmp_code, parsed_rule, delete=False):
        self.logger.debug("createAndVerifyIpv6FirewallRule - %s" % parsed_rule)
        fw_rule = self.createIpv6FirewallRuleInNetwork(self.network.id, traffic_type, source_cidr, dest_cidr, protocol,
        start_port, end_port, icmp_type, icmp_code)
        cmd = listIpv6FirewallRules.listIpv6FirewallRulesCmd()
        cmd.id = fw_rule.id
        rules = self.apiclient.listIpv6FirewallRules(cmd)
        self.assertTrue(
            isinstance(rules, list),
            "Check listIpv6FirewallRules response returns a valid list"
        )
        rule = rules[0]
        self.assertEqual(rule.networkid, self.network.id,
            "IPv6 firewall rule network ID mismatch %s, %s" % (rule.networkid, self.network.id))
        self.assertEqual(rule.traffictype, traffic_type,
            "IPv6 firewall rule traffic type mismatch %s, %s" % (rule.traffictype, traffic_type))
        if source_cidr:
            self.assertEqual(rule.cidrlist, source_cidr,
                "IPv6 firewall rule source CIDR mismatch %s, %s" % (rule.cidrlist, source_cidr))
        if dest_cidr:
            self.assertEqual(rule.destcidrlist, dest_cidr,
                "IPv6 firewall rule destination CIDR mismatch %s, %s" % (rule.destcidrlist, dest_cidr))
        if protocol:
            self.assertEqual(rule.protocol, protocol,
                "IPv6 firewall rule protocol mismatch %s, %s" % (rule.protocol, protocol))
        if start_port:
            self.assertEqual(rule.startport, start_port,
                "IPv6 firewall rule start port mismatch %d, %d" % (rule.startport, start_port))
        if end_port:
            self.assertEqual(rule.endport, end_port,
                "IPv6 firewall rule end port mismatch %d, %d" % (rule.endport, end_port))
        if icmp_type is not None:
            self.assertEqual(rule.icmptype, icmp_type,
                "IPv6 firewall rule ICMP type mismatch %d, %d" % (rule.icmptype, icmp_type))
        if icmp_code is not None:
            self.assertEqual(rule.icmpcode, icmp_code,
                "IPv6 firewall rule ICMP code mismatch %d, %d" % (rule.icmpcode, icmp_code))
        routerCmd = "nft list chain ip6 %s %s" % (FIREWALL_TABLE, FIREWALL_CHAINS[traffic_type])
        res = self.getRouterProcessStatus(self.getNetworkRouter(self.network), routerCmd)
        self.assertTrue(parsed_rule in res,
            "Listing firewall rule with nft list chain failure for rule: %s" % parsed_rule)
        if delete == True:
            cmd = deleteIpv6FirewallRule.deleteIpv6FirewallRuleCmd()
            cmd.id = fw_rule.id
            self.apiclient.deleteIpv6FirewallRule(cmd)
            res = self.getRouterProcessStatus(self.getNetworkRouter(self.network), routerCmd)
            self.assertFalse(parsed_rule in res,
                "Firewall rule present in nft list chain failure despite delete for rule: %s" % parsed_rule)

    def checkIpv6FirewallRule(self):
        traffic_type = "Ingress"

        # Ingress - ip6 saddr SOURCE_CIDR ip6 daddr DEST_CIDR tcp dport { START_PORT-END_PORT } accept
        source_cidr = self.getRandomIpv6Cidr()
        dest_cidr = self.getRandomIpv6Cidr()
        protocol = "tcp"
        start_port = randint(3000, 5000)
        end_port = start_port + randint(1, 8)
        rule = "ip6 saddr %s ip6 daddr %s %s dport { %d-%d } accept" % (source_cidr, dest_cidr, protocol, start_port, end_port)
        self.createAndVerifyIpv6FirewallRule(traffic_type, source_cidr, dest_cidr, protocol,
            start_port, end_port, None, None, rule, True)

        # Ingress - ip6 daddr DEST_CIDR icmpv6 type TYPE code CODE accept
        source_cidr = self.getRandomIpv6Cidr()
        protocol = "icmp"
        icmp_type = choice(list(ICMPV6_TYPE.keys()))
        icmp_code = choice(list(ICMPV6_CODE_TYPE.keys()))
        rule = "ip6 saddr %s ip6 daddr %s %sv6 type %s %sv6 code %s accept" % (source_cidr, CIDR_IPV6_ANY, protocol, ICMPV6_TYPE[icmp_type], protocol, ICMPV6_CODE_TYPE[icmp_code])
        self.createAndVerifyIpv6FirewallRule(traffic_type, source_cidr, None, protocol,
            None, None, icmp_type, icmp_code, rule)

        action = "accept"
        if self.isNetworkEgressDefaultPolicyAllow(self.network):
            action = "drop"
        traffic_type = "Egress"

        # Egress - ip6 saddr ::/0 ip6 daddr ::/0 udp dport { 0-65355 } ACTION
        protocol = "udp"
        rule = "ip6 saddr %s ip6 daddr %s %s dport %s %s" % (CIDR_IPV6_ANY, CIDR_IPV6_ANY, protocol, TCP_UDP_PORT_ANY, action)
        self.createAndVerifyIpv6FirewallRule(traffic_type, None, None, protocol,
            None, None, None, None, rule)

        # Egress - ip6 saddr ::/0 ip6 daddr ::/0 icmpv6 type ANY_TYPE ACTION
        protocol = "icmp"
        rule = "ip6 saddr %s ip6 daddr %s %sv6 type %s %s" % (CIDR_IPV6_ANY, CIDR_IPV6_ANY, protocol, ICMPV6_TYPE_ANY, action)
        self.createAndVerifyIpv6FirewallRule(traffic_type, None, None, protocol,
            None, None, None, None, rule)

        # Egress - ip6 saddr ::/0 ip6 daddr DEST_CIDR ACTION
        protocol = "all"
        dest_cidr = self.getRandomIpv6Cidr()
        rule = "ip6 saddr %s ip6 daddr %s %s" % (CIDR_IPV6_ANY, CIDR_IPV6_ANY, action)
        self.createAndVerifyIpv6FirewallRule(traffic_type, None, None, protocol,
            None, None, None, None, rule)

    def checkNetworkVRRedundancy(self):
        primary_router = self.getNetworkRouter(self.network)
        Router.stop(
            self.apiclient,
            id=primary_router.id
        )
        time.sleep(self.services["sleep"]/2)
        new_primary_router = self.getNetworkRouter(self.network)
        self.assertNotEqual(new_primary_router.id, primary_router.id,
            "Original primary router ID: %s of network is still the primary router after stopping" % (primary_router.id))
        print(new_primary_router)
        self.checkIpv6NetworkPrimaryRouter(new_primary_router)


    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns",
            "smoke"],
        required_hardware="false")
    @skipTestIf("ipv6NotSupported")
    def test_01_verify_ipv6_network(self):
        """Test to verify IPv6 network

        # Validate the following:
        # 1. Create IPv6 network, deploy VM
        # 2. Verify network has required IPv6 details
        # 3. List router for the network and verify it has required IPv6 details for Guest and Public NIC of the VR
        # 4. SSH into VR(s) and verify correct details are present for its NICs
        # 5. Verify VM in network has required IPv6 details
        # 6. Restart network with cleanup
        # 7. Update network with a new offering
        # 8. Again verify network and VR details
        # 9. Deploy another IPv6 network and check routing between two networks and their VM
        # 10. Create IPv6 firewall rules and verify in VR if they get implemented
        """

        self.createIpv6NetworkOffering(False)
        self.createIpv6NetworkOfferingForUpdate(False)
        self.createTinyServiceOffering()
        self.deployIpv6Network()
        self.deployIpv6NetworkVm()
        self.checkIpv6NetworkBasic()
        self.checkIpv6NetworkRoutersBasic()
        self.checkIpv6NetworkRoutersInternal()
        self.checkIpv6NetworkVm()
        self.restartNetworkWithCleanup()
        self.updateNetworkWithOffering()
        self.checkIpv6NetworkBasic()
        self.checkIpv6NetworkRoutersBasic()
        self.checkNetworkRouting()
        self.checkIpv6FirewallRule()

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns",
            "smoke"],
        required_hardware="false")
    @skipTestIf("ipv6NotSupported")
    def test_02_verify_ipv6_network_redundant(self):
        """Test to verify redundant IPv6 network

        # Validate the following:
        # 1. Create IPv6 network, deploy VM
        # 2. Verify network has required IPv6 details
        # 3. List VRs for the network and verify it has required IPv6 details for Guest and Public NIC of the VR
        # 4. SSH into VR(s) and verify correct details are present for its NICs
        # 5. Verify VM in network has required IPv6 details
        # 6. Restart network with cleanup
        # 7. Update network with a new offering
        # 8. Again verify network and VR details
        # 9. Deploy another IPv6 network and check routing between two networks and their VM
        # 10. Create IPv6 firewall rules and verify in VR if they get implemented
        # 11. Stop primary router and verify internals in backup VR
        """

        self.createIpv6NetworkOffering(True)
        self.createIpv6NetworkOfferingForUpdate(True)
        self.createTinyServiceOffering()
        self.deployIpv6Network()
        self.deployIpv6NetworkVm()
        self.checkIpv6NetworkBasic()
        self.checkIpv6NetworkRoutersBasic()
        self.checkIpv6NetworkRoutersInternal()
        self.checkIpv6NetworkVm()
        self.restartNetworkWithCleanup()
        self.updateNetworkWithOffering()
        self.checkIpv6NetworkBasic()
        self.checkIpv6NetworkRoutersBasic()
        self.checkNetworkRouting()
        self.checkIpv6FirewallRule()
        self.checkNetworkVRRedundancy()
