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
""" BVT tests for IPv6 infra operations"""

#Import Local Modules
from marvin.codes import FAILED
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (createGuestNetworkIpv6Prefix,
                                  listGuestNetworkIpv6Prefixes,
                                  deleteGuestNetworkIpv6Prefix)
from marvin.lib.utils import (cleanup_resources)
from marvin.lib.base import (Configurations,
                             NetworkOffering,
                             VpcOffering,
                             PublicIpRange)
from marvin.lib.common import (get_zone)
from marvin.cloudstackException import CloudstackAPIException

from nose.plugins.attrib import attr
import logging

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
