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
                                  deleteGuestNetworkIpv6Prefix)
from marvin.lib.utils import (isAlmostEqual,
                              cleanup_resources,
                              random_gen)
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
                             NIC)
from marvin.lib.common import (get_domain,
                               get_zone,
                               list_hosts,
                               get_test_template)
from nose.plugins.attrib import attr

import time
from marvin.sshClient import SshClient
from marvin.cloudstackException import CloudstackAPIException
from marvin.lib.decoratorGenerators import skipTestIf


_multiprocess_shared_ = True

ipv6_offering_config_name = "ipv6.offering.enabled"

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
            self.fail("Network offering created despite gloabal setting - %s set to false" % ipv6_offering_config_name)
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
            ipv6_service = cls.services["network_offering"]
            ipv6_service["internetprotocol"] = "dualstack"
            cls.network_offering = NetworkOffering.create(
                cls.apiclient,
                ipv6_service
            )
            cls._cleanup.append(cls.network_offering)
            cls.network_offering.update(cls.apiclient, state='Enabled')
            cls.services["network"]["networkoffering"] = cls.network_offering.id
            cls.network = Network.create(
                cls.apiclient,
                cls.services["network"],
                cls.account.name,
                cls.account.domainid,
                zoneid=cls.zone.id
            )
            cls._cleanup.append(cls.network)
            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.services["service_offerings"]["tiny"],
            )
            cls._cleanup.append(cls.service_offering)
            cls.hypervisor = testClient.getHypervisorInfo()
            cls.template = get_test_template(
                cls.apiclient,
                cls.zone.id,
                cls.hypervisor
            )
            if cls.template == FAILED:
                assert False, "get_test_template() failed to return template"
            cls.services["virtual_machine"]["zoneid"] = cls.zone.id
            cls.virtual_machine = VirtualMachine.create(
                cls.apiclient,
                cls.services["virtual_machine"],
                templateid=cls.template.id,
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                networkids=cls.network.id,
                serviceofferingid=cls.service_offering.id
            )
            cls._cleanup.append(cls.virtual_machine)
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
    @skipTestIf("ipv6NotSupported")
    def test_01_verify_ipv6_network(self):
        """Test to verify IPv6 network

        # Validate the following:
        # 1. List network and verify it has required IPv6 details
        # 2. List router for the network and verify it has required IPv6 details for Guest and Public NIC of the VR
        """
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
        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
            self.apiclient,
            networkid=self.network.id,
            listall=True
        )
        self.assertTrue(
            isinstance(routers, list),
            "Check listRouters response returns a valid list"
        )
        self.assertEqual(
            len(routers),
            1,
            "Router for the network isn't found"
        )
        router = routers[0]
        nics = router.nic
        for nic in nics:
            if nic.traffictype == 'Public' or nic.traffictype == 'Guest':
                self.assertNotEqual(nic.ip6address,
                            None,
                            "IPv6 address for router %s NIC is empty" % nic.traffictype)
                self.assertNotEqual(nic.ip6cidr,
                            None,
                            "IPv6 CIDR for router %s NIC is empty" % nic.traffictype)
                self.assertNotEqual(nic.ip6gateway,
                            None,
                            "IPv6 gateway for router %s NIC is empty" % nic.traffictype)
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
    @skipTestIf("ipv6NotSupported")
    def test_02_verify_ipv6_network_vm(self):
        """Test to verify IPv6 network

        # Validate the following:
        # 1. List NIC for the VM in the network and verify IPv6 details
        # 2. List router for the network and verify it has required IPv6 details for Guest and Public NIC of the VR
        """

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
                    "IPv6 address for router %s NIC is empty" % nic.traffictype)
        self.assertNotEqual(nic.ip6cidr,
                    None,
                    "IPv6 CIDR for router %s NIC is empty" % nic.traffictype)
        self.assertNotEqual(nic.ip6gateway,
                    None,
                    "IPv6 gateway for router %s NIC is empty" % nic.traffictype)
        return
