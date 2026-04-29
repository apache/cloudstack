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
""" P1 tests for Dedicating Public IP addresses

    Test Plan: https://cwiki.apache.org/confluence/display/CLOUDSTACK/
    Dedicated+Resources+-+Public+IP+Addresses+and+VLANs+per+Tenant+Test+Plan

    Issue Link: https://issues.apache.org/jira/browse/CLOUDSTACK-2251

    Feature Specifications: https://cwiki.apache.org/confluence/display/
    CLOUDSTACK/FS-+Dedicate+Public+IP+Addresses+per+tenant
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.utils import (validateList,
                              cleanup_resources,
                              random_gen)
from marvin.lib.base import (Account,
                             PublicIpRange,
                             Domain,
                             Network,
                             NetworkOffering,
                             PublicIPAddress,
                             VPC,
                             VpcOffering,
                             LoadBalancerRule,
                             Resources)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_free_vlan)
from marvin.codes import PASS
from netaddr import IPAddress
import random


class TestDedicatePublicIPRange(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestDedicatePublicIPRange,
            cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        # Get Zone, Domain
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.testdata["isolated_network"]["zoneid"] = cls.zone.id
        cls.testdata["publiciprange"]["zoneid"] = cls.zone.id
        cls._cleanup = []
        try:
            cls.isolated_network_offering = NetworkOffering.create(
                cls.apiclient,
                cls.testdata["isolated_network_offering"])
            cls._cleanup.append(cls.isolated_network_offering)
            cls.isolated_network_offering.update(
                cls.apiclient,
                state='Enabled')

            cls.vpc_network_offering = NetworkOffering.create(
                cls.apiclient,
                cls.testdata["nw_offering_isolated_vpc"],
                conservemode=False)
            cls._cleanup.append(cls.vpc_network_offering)
            cls.vpc_network_offering.update(cls.apiclient, state='Enabled')

            cls.vpc_off = VpcOffering.create(
                cls.apiclient,
                cls.testdata["vpc_offering"]
            )
            cls.vpc_off.update(cls.apiclient, state='Enabled')
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest(e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        # Setting up random public ip range
        self.testdata["publiciprange"]["vlan"] = get_free_vlan(
            self.apiclient,
            self.zone.id)[1]
        random_subnet_number = random.randrange(1, 254)
        self.testdata["publiciprange"]["gateway"] = "172.16." + \
            str(random_subnet_number) + ".1"
        self.testdata["publiciprange"]["startip"] = "172.16." + \
            str(random_subnet_number) + ".2"
        self.testdata["publiciprange"]["endip"] = "172.16." + \
            str(random_subnet_number) + ".3"
        self.testdata["publiciprange"]["netmask"] = "255.255.255.0"
        return

    def tearDown(self):
        try:
            # Clean up
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_01_dedicate_public_ip_range_root_domain(self):
        """Dedicate public IP range to an account in root domain

        # Validate the following:
        # 1. Create a Public IP range
        # 2. Created IP range should be present, verify with listVlanIpRanges
        # 3. Dedicate the created IP range to user account
        # 4. Verify IP range is dedicated, verify with listVlanIpRanges
        """
        # Create Account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(account)

        public_ip_range = PublicIpRange.create(
            self.apiclient,
            self.testdata["publiciprange"]
        )
        self.cleanup.append(public_ip_range)
        public_ip_ranges = PublicIpRange.list(
            self.apiclient,
            id=public_ip_range.vlan.id
        )
        self.assertEqual(
            validateList(public_ip_ranges)[0],
            PASS,
            "public ip ranges list validation failed"
        )
        self.assertEqual(
            public_ip_ranges[0].id,
            public_ip_range.vlan.id,
            "Check public ip range response id is in listVlanIpRanges"
        )

        PublicIpRange.dedicate(
            self.apiclient,
            public_ip_range.vlan.id,
            account=account.name,
            domainid=account.domainid
        )
        public_ip_ranges = PublicIpRange.list(
            self.apiclient,
            id=public_ip_range.vlan.id
        )
        self.assertEqual(validateList(public_ip_ranges)[0], PASS,
                         "public ip ranges list validation failed")
        self.assertEqual(
            public_ip_ranges[0].account,
            account.name,
            "Check account name is in listVlanIpRanges\
               as the account public ip range is dedicated to")
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_02_dedicate_public_ip_range_user_domain(self):
        """Dedicate public IP range to an account in user domain

        # Validate the following:
        # 1. Create a Public IP range
        # 2. Created IP range should be present, verify with listVlanIpRanges
        # 3. Dedicate the created IP range to user account
        # 4. Verify IP range is dedicated, verify with listVlanIpRanges
        # 5. Release the dedicated Public IP range back to the system
        # 6. Verify IP range has been released, verify with listVlanIpRanges
        # 7. Delete the Public IP range"""

        user_domain = Domain.create(
            self.apiclient,
            services=self.testdata["domain"],
            parentdomainid=self.domain.id)
        self.cleanup.append(user_domain)
        # Create Account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=user_domain.id
        )
        self.cleanup.insert(-1, account)

        public_ip_range = PublicIpRange.create(
            self.apiclient,
            self.testdata["publiciprange"]
        )
        self.cleanup.append(public_ip_range)
        public_ip_ranges = PublicIpRange.list(
            self.apiclient,
            id=public_ip_range.vlan.id
        )
        self.assertEqual(
            validateList(public_ip_ranges)[0],
            PASS,
            "public ip ranges list validation failed"
        )
        self.assertEqual(
            public_ip_ranges[0].id,
            public_ip_range.vlan.id,
            "Check public ip range response id is in listVlanIpRanges"
        )

        PublicIpRange.dedicate(
            self.apiclient,
            public_ip_range.vlan.id,
            account=account.name,
            domainid=account.domainid
        )
        public_ip_ranges = PublicIpRange.list(
            self.apiclient,
            id=public_ip_range.vlan.id
        )
        self.assertEqual(validateList(public_ip_ranges)[0], PASS,
                         "public ip ranges list validation failed")
        self.assertEqual(
            public_ip_ranges[0].account,
            account.name,
            "Check account name is in listVlanIpRanges\
               as the account public ip range is dedicated to")
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_03_dedicate_during_creation_root_domain(self):
        """Dedicate public IP range to an account during its creation only

        # Validate the following:
        # 1. Create an account in root domain
        # 2. Create a public IP range and pass account name and domain id
             while creating
        # 3. The public IP range should be successfully added and it should
             be dedicated to the account
        """
        # Create Account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(account)

        public_ip_range = PublicIpRange.create(
            self.apiclient,
            self.testdata["publiciprange"],
            account=account.name,
            domainid=account.domainid
        )
        self.cleanup.append(public_ip_range)
        public_ip_ranges = PublicIpRange.list(
            self.apiclient,
            id=public_ip_range.vlan.id
        )
        self.assertEqual(
            validateList(public_ip_ranges)[0],
            PASS,
            "public ip ranges list validation failed"
        )
        self.assertEqual(
            public_ip_ranges[0].id,
            public_ip_range.vlan.id,
            "Check public ip range response id is in listVlanIpRanges"
        )

        self.assertEqual(
            public_ip_ranges[0].account,
            account.name,
            "Check account name is in listVlanIpRanges\
               as the account public ip range is dedicated to")
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_04_dedicate_during_creation_user_domain(self):
        """Dedicate public IP range to an account in user
           domain during its creation only

        # Validate the following:
        # 1. Create a user domain and an account in it
        # 2. Create a public IP range and pass account name
             and domain id while creating
        # 3. The public IP range should be successfully added
             and it should be dedicated to the account
        """

        user_domain = Domain.create(
            self.apiclient,
            services=self.testdata["domain"],
            parentdomainid=self.domain.id)
        self.cleanup.append(user_domain)
        # Create Account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=user_domain.id
        )
        self.cleanup.insert(-1, account)

        public_ip_range = PublicIpRange.create(
            self.apiclient,
            self.testdata["publiciprange"],
            account=account.name,
            domainid=account.domainid
        )
        self.cleanup.append(public_ip_range)
        public_ip_ranges = PublicIpRange.list(
            self.apiclient,
            id=public_ip_range.vlan.id
        )
        self.assertEqual(
            validateList(public_ip_ranges)[0],
            PASS,
            "public ip ranges list validation failed"
        )
        self.assertEqual(
            public_ip_ranges[0].id,
            public_ip_range.vlan.id,
            "Check public ip range response id is in listVlanIpRanges"
        )

        self.assertEqual(
            public_ip_ranges[0].account,
            account.name,
            "Check account name is in listVlanIpRanges\
               as the account public ip range is dedicated to")

        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_05_acquire_ip_single_guest_network(self):
        """Create guest network in account with dedicated public
           ip range and acquire IPs in network

        # Validate the following:
        # 1. Create a Public IP range with 2 IPs in it
        # 2. Dedicate the created IP range to user account
        # 3. Create a guest network in the account
        # 4. Acquire a public IP for this network
        # 5. Verify that the acquired IP is from the dedicated range
        # 6. Repeat steps 4 and 5 so that both the IPs in the range are
             now acquired
        # 7. Repeat step 4
        # 8. Now this public IP should not be from the dedicated range,
             it should be from global pool
        """
        # Create Account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(account)

        public_ip_range = PublicIpRange.create(
            self.apiclient,
            self.testdata["publiciprange"]
        )
        self.cleanup.append(public_ip_range)
        PublicIpRange.dedicate(
            self.apiclient,
            public_ip_range.vlan.id,
            account=account.name,
            domainid=account.domainid
        )

        isolated_network = Network.create(
            self.apiclient,
            self.testdata["isolated_network"],
            account.name,
            account.domainid,
            networkofferingid=self.isolated_network_offering.id)

        formatted_startip = IPAddress(
            self.testdata["publiciprange"]["startip"])
        formatted_endip = IPAddress(self.testdata["publiciprange"]["endip"])
        public_ip1 = PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_network.id)

        # Format the IP so that it can be used to compare with other IPs
        formatted_publicip = IPAddress(public_ip1.ipaddress.ipaddress)

        self.assertTrue(int(formatted_startip) <=
                        int(formatted_publicip) <= int(formatted_endip),
                        "publicip should be from the dedicated range")

        public_ip2 = PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_network.id)

        formatted_publicip = IPAddress(public_ip2.ipaddress.ipaddress)

        self.assertTrue(int(formatted_startip) <=
                        int(formatted_publicip) <= int(formatted_endip),
                        "publicip should be from the dedicated range")

        public_ip3 = PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_network.id)

        formatted_publicip = IPAddress(public_ip3.ipaddress.ipaddress)

        self.assertFalse(int(formatted_startip) <=
                         int(formatted_publicip) <= int(formatted_endip),
                         "publicip should not be from the dedicated range")
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_06_acquire_ip_multiple_guest_network(self):
        """Create multiple guest networks and acquire IPs from dedicated range

        # Validate the following:
        # 1. Create a Public IP range with 2 IPs in it
        # 2. Dedicate the created IP range to user account
        # 3. Create a guest network in the account
        # 4. Acquire a public IP for this network
        # 5. Verify that the acquired IP is from the dedicated range
        # 6. Repeat steps 3,4 and 5 so that both the IPs in the range are
             now acquired
        # 7. Repeat step 4 for network 1
        # 8. Now this public IP should not be from the dedicated range,
             it should be from global pool
        """
        # Create Account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(account)

        public_ip_range = PublicIpRange.create(
            self.apiclient,
            self.testdata["publiciprange"]
        )
        self.cleanup.append(public_ip_range)
        PublicIpRange.dedicate(
            self.apiclient,
            public_ip_range.vlan.id,
            account=account.name,
            domainid=account.domainid
        )

        formatted_startip = IPAddress(
            self.testdata["publiciprange"]["startip"])
        formatted_endip = IPAddress(self.testdata["publiciprange"]["endip"])

        isolated_network1 = Network.create(
            self.apiclient,
            self.testdata["isolated_network"],
            account.name,
            account.domainid,
            networkofferingid=self.isolated_network_offering.id)

        public_ip1 = PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_network1.id)

        formatted_publicip1 = IPAddress(public_ip1.ipaddress.ipaddress)

        self.assertTrue(int(formatted_startip) <=
                        int(formatted_publicip1) <= int(formatted_endip),
                        "publicip should be from the dedicated range")

        isolated_network2 = Network.create(
            self.apiclient,
            self.testdata["isolated_network"],
            account.name,
            account.domainid,
            networkofferingid=self.isolated_network_offering.id)

        public_ip2 = PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_network2.id)

        formatted_publicip2 = IPAddress(public_ip2.ipaddress.ipaddress)

        self.assertTrue(int(formatted_startip) <=
                        int(formatted_publicip2) <= int(formatted_endip),
                        "publicip should be from the dedicated range")

        public_ip3 = PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_network1.id)

        formatted_publicip3 = IPAddress(public_ip3.ipaddress.ipaddress)

        self.assertFalse(int(formatted_startip) <=
                         int(formatted_publicip3) <= int(formatted_endip),
                         "publicip should not be from the dedicated range")
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_08_acquire_ip_in_vpc(self):
        """Acquire public IP from dedicated range in network tier of VPC

        # Validate the following:
        # 1. Create a domain and an account in it
        # 2. Create a VPC in this account
        # 3. Create a netwok tier in this VPC
        # 4. Create a Public IP range and dedicate to account
        # 5. Dedicate the created IP range to user account
        # 6. Acquire public IP in the network tier of VPC
        # 7. Ip should be from the dedicated range
        # 8. Repeat steps 6 and 7
        # 9. Repeat step 6, this time the IP should not be from
             dedicated range, it should be from global pool
        """
        if self.hypervisor.lower() in ["hyperv"]:
            self.skipTest("Skipping test as VPC is not supported on HyperV")
        user_domain = Domain.create(
            self.apiclient,
            services=self.testdata["domain"],
            parentdomainid=self.domain.id)
        self.cleanup.append(user_domain)
        # Create Account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=user_domain.id
        )
        self.cleanup.insert(-1, account)
        self.testdata["vpc"]["cidr"] = '10.1.1.0/16'
        vpc = VPC.create(
            self.apiclient,
            self.testdata["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=account.name,
            domainid=account.domainid
        )
        network = Network.create(
            self.apiclient,
            self.testdata["network"],
            accountid=account.name,
            domainid=account.domainid,
            networkofferingid=self.vpc_network_offering.id,
            zoneid=self.zone.id,
            gateway='10.1.1.1',
            netmask='255.255.255.0',
            vpcid=vpc.id
        )

        public_ip_range = PublicIpRange.create(
            self.apiclient,
            self.testdata["publiciprange"]
        )
        self.cleanup.append(public_ip_range)
        PublicIpRange.dedicate(
            self.apiclient,
            public_ip_range.vlan.id,
            account=account.name,
            domainid=account.domainid
        )

        formatted_startip = IPAddress(
            self.testdata["publiciprange"]["startip"])
        formatted_endip = IPAddress(self.testdata["publiciprange"]["endip"])

        public_ip1 = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            networkid=network.id,
            accountid=account.name,
            domainid=account.domainid,
            vpcid=vpc.id)

        formatted_publicip1 = IPAddress(public_ip1.ipaddress.ipaddress)

        self.assertTrue(int(formatted_startip) <=
                        int(formatted_publicip1) <= int(formatted_endip),
                        "publicip should be from the dedicated range")

        public_ip2 = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            networkid=network.id,
            accountid=account.name,
            domainid=account.domainid,
            vpcid=vpc.id)

        formatted_publicip2 = IPAddress(public_ip2.ipaddress.ipaddress)

        self.assertTrue(int(formatted_startip) <=
                        int(formatted_publicip2) <= int(formatted_endip),
                        "publicip should be from the dedicated range")

        public_ip3 = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            networkid=network.id,
            accountid=account.name,
            domainid=account.domainid,
            vpcid=vpc.id)

        formatted_publicip3 = IPAddress(public_ip3.ipaddress.ipaddress)

        self.assertFalse(int(formatted_startip) <=
                         int(formatted_publicip3) <= int(formatted_endip),
                         "publicip should be from the dedicated range")

        return


class TestFailureScenarios(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestFailureScenarios, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.testdata["isolated_network"]["zoneid"] = cls.zone.id
        cls.testdata["publiciprange"]["zoneid"] = cls.zone.id
        cls._cleanup = []
        try:
            cls.isolated_network_offering = NetworkOffering.create(
                cls.apiclient,
                cls.testdata["isolated_network_offering"])
            cls._cleanup.append(cls.isolated_network_offering)
            cls.isolated_network_offering.update(
                cls.apiclient,
                state='Enabled')
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest(e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        # Setting up random public ip range
        self.testdata["publiciprange"]["vlan"] = get_free_vlan(
            self.apiclient,
            self.zone.id)[1]
        subnet_number = random.randrange(1, 254)
        self.testdata["publiciprange"][
            "gateway"] = "172.16." + str(subnet_number) + ".1"
        self.testdata["publiciprange"][
            "startip"] = "172.16." + str(subnet_number) + ".2"
        self.testdata["publiciprange"][
            "endip"] = "172.16." + str(subnet_number) + ".3"
        self.testdata["publiciprange"]["netmask"] = "255.255.255.0"
        return

    def tearDown(self):
        try:
            # Clean up
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_10_dedicate_invalid_ip_range(self):
        """Dedicate invalid public IP range to an account

        # Validate the following:
        # 1. Create an account in root domain
        # 2. Create a Public IP range by giving invalid ip addresses
             and specify the account name and domain id while creating
        # 3. The operations should fail
        """
        # Create Account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(account)

        # Specify invalid IPs
        self.testdata["publiciprange"]["startip"] = "172.16.255.256"
        self.testdata["publiciprange"]["startip"] = "172.16.255.257"
        self.testdata["publiciprange"]["gateway"] = "172.16.255.1"

        with self.assertRaises(Exception):
            public_ip_range = PublicIpRange.create(
                self.apiclient,
                self.testdata["publiciprange"],
                account=account.name,
                domainid=account.domainid
            )
            self.cleanup.append(public_ip_range)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_11_dedicate_to_wrong_account(self):
        """Dedicate public IP range to invalid account

        # Validate the following:
        # 1. Create an account in root domain
        # 2. Create a Public IP range with valid ip addresses
             and specify invalid account name and domain id while creating
        # 3. The operation should fail
        """
        # Create Account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(account)

        with self.assertRaises(Exception):
            public_ip_range = PublicIpRange.create(
                self.apiclient,
                self.testdata["publiciprange"],
                account=account.name + random_gen(),
                domainid=account.domainid
            )
            self.cleanup.append(public_ip_range)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_12_dedicate_already_dedicated_range(self):
        """Dedicate already dedicated public IP range

        # Validate the following:
        # 1. Create a user domain and two accounts in it
        # 2. Create a Public IP range
        # 3. Dedicate the created IP range to account1
        # 4. Try to dedicate the same public iprage to account 2
        # 5. The operation should fail"""

        user_domain = Domain.create(
            self.apiclient,
            services=self.testdata["domain"],
            parentdomainid=self.domain.id)
        self.cleanup.append(user_domain)

        account1 = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=user_domain.id
        )
        self.cleanup.insert(-1, account1)

        account2 = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=user_domain.id
        )
        self.cleanup.insert(-1, account2)

        public_ip_range = PublicIpRange.create(
            self.apiclient,
            self.testdata["publiciprange"]
        )
        self.cleanup.append(public_ip_range)
        PublicIpRange.dedicate(
            self.apiclient,
            public_ip_range.vlan.id,
            account=account1.name,
            domainid=account1.domainid
        )

        with self.assertRaises(Exception):
            PublicIpRange.dedicate(
                self.apiclient,
                public_ip_range.vlan.id,
                account=account2.name,
                domainid=account2.domainid
            )
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_13_public_ips_limit(self):
        """Dedicate public IP range to an account having lesser public
           ip limit than range

        # Validate the following:
        # 1. Create a user domain and account
        # 2. Set the public IP limit of account to 1
        # 3. Create a Public IP range of 2 IPs
        # 4. Try to dedicate the range to account
        # 5. Operation should fail as it exceeds the account limit
        """

        user_domain = Domain.create(
            self.apiclient,
            services=self.testdata["domain"],
            parentdomainid=self.domain.id)
        self.cleanup.append(user_domain)

        account1 = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=user_domain.id
        )
        self.cleanup.insert(-1, account1)

        # Updating maximum public ips limit of account to 1
        Resources.updateLimit(self.apiclient,
                              resourcetype=1,
                              max=1,
                              account=account1.name,
                              domainid=account1.domainid)

        public_ip_range = PublicIpRange.create(
            self.apiclient,
            self.testdata["publiciprange"]
        )
        self.cleanup.append(public_ip_range)
        # public ip range consists of 2 ips, hence dedication should fail
        with self.assertRaises(Exception):
            PublicIpRange.dedicate(
                self.apiclient,
                public_ip_range.vlan.id,
                account=account1.name,
                domainid=account1.domainid
            )
        return


class TestReleaseIPs(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestReleaseIPs, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.testdata["isolated_network"]["zoneid"] = cls.zone.id
        cls.testdata["publiciprange"]["zoneid"] = cls.zone.id
        cls._cleanup = []
        try:
            cls.isolated_network_offering = NetworkOffering.create(
                cls.apiclient,
                cls.testdata["isolated_network_offering"])
            cls._cleanup.append(cls.isolated_network_offering)
            cls.isolated_network_offering.update(
                cls.apiclient,
                state='Enabled')
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest(e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        # Setting up random public ip range
        self.testdata["publiciprange"]["vlan"] = get_free_vlan(
            self.apiclient,
            self.zone.id)[1]
        shared_network_subnet_number = random.randrange(1, 254)
        self.testdata["publiciprange"]["gateway"] = "172.16." + \
            str(shared_network_subnet_number) + ".1"
        self.testdata["publiciprange"]["startip"] = "172.16." + \
            str(shared_network_subnet_number) + ".2"
        self.testdata["publiciprange"]["endip"] = "172.16." + \
            str(shared_network_subnet_number) + ".3"
        self.testdata["publiciprange"]["netmask"] = "255.255.255.0"
        return

    def tearDown(self):
        try:
            # Clean up
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_14_release_ip_from_dedicated_range(self):
        """Disassociate a public IP from dedicated public IP range

        # Validate the following:
        # 1. Create an account in root domain
        # 2. Create a guest network in the account
        # 3. Acquire a public IP from global pool in this network
             for source nat purpose
        # 4. Create a public IP range
        # 5. Dedicate this range to the account
        # 6. Acquire public IP for the guest network
        # 7. Verify that the public IP is from the dedicate range
        # 8. Disassociate the public IP, the operation should be successful
        """
        # Create Account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(account)

        isolated_network = Network.create(
            self.apiclient,
            self.testdata["isolated_network"],
            account.name,
            account.domainid,
            networkofferingid=self.isolated_network_offering.id)

        # Acquiring IP for source nat purposes
        PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_network.id)

        public_ip_range = PublicIpRange.create(
            self.apiclient,
            self.testdata["publiciprange"]
        )
        self.cleanup.append(public_ip_range)
        PublicIpRange.dedicate(
            self.apiclient,
            public_ip_range.vlan.id,
            account=account.name,
            domainid=account.domainid
        )

        formatted_startip = IPAddress(
            self.testdata["publiciprange"]["startip"])
        formatted_endip = IPAddress(self.testdata["publiciprange"]["endip"])
        public_ip1 = PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_network.id)

        formatted_publicip = IPAddress(public_ip1.ipaddress.ipaddress)

        self.assertTrue(int(formatted_startip) <=
                        int(formatted_publicip) <= int(formatted_endip),
                        "publicip should be from the dedicated range")

        public_ip1.delete(self.apiclient)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_15_release_dedicated_range(self):
        """Release dedicated public ip range with no IPs acquired

        # Validate the following:
        # 1. Create a Public IP range in an account
        # 2. Dedicate the created IP range to user account
        # 3. Verify IP range is dedicated, verify with listVlanIpRanges
        # 4. Release the public IP range, verify that it is released
        """
        # Create Account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(account)

        public_ip_range = PublicIpRange.create(
            self.apiclient,
            self.testdata["publiciprange"]
        )
        self.cleanup.append(public_ip_range)
        PublicIpRange.dedicate(
            self.apiclient,
            public_ip_range.vlan.id,
            account=account.name,
            domainid=account.domainid
        )

        public_ip_ranges = PublicIpRange.list(
            self.apiclient,
            id=public_ip_range.vlan.id
        )
        self.assertEqual(validateList(public_ip_ranges)[0], PASS,
                         "public ip ranges list validation failed")
        self.assertEqual(
            public_ip_ranges[0].account,
            account.name,
            "Check account name is in listVlanIpRanges\
               as the account public ip range is dedicated to")

        public_ip_range.release(self.apiclient)

        public_ip_ranges = PublicIpRange.list(
            self.apiclient,
            id=public_ip_range.vlan.id
        )
        self.assertEqual(validateList(public_ip_ranges)[0], PASS,
                         "public ip ranges list validation failed")
        self.assertEqual(
            str(public_ip_ranges[0].account).lower(),
            "system",
            "Check account name is system")
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_21_release_dedicated_range_ip_address_in_use(self):
        """Dedicate a public IP range, acquire some IPs and relese the range

        # Validate the following:
        # 1. Create an account
        # 2. Create a guest network in the account
        # 3. Create a Public IP range in an account
        # 4. Dedicate the created IP range to user account
        # 5. Acquire IP in the network and verify that it is from dedicated
             range
        # 6. Release the public IP range, verify that it is released
        """
        # Create Account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(account)

        public_ip_range = PublicIpRange.create(
            self.apiclient,
            self.testdata["publiciprange"]
        )
        self.cleanup.append(public_ip_range)
        PublicIpRange.dedicate(
            self.apiclient,
            public_ip_range.vlan.id,
            account=account.name,
            domainid=account.domainid
        )

        public_ip_ranges = PublicIpRange.list(
            self.apiclient,
            id=public_ip_range.vlan.id
        )
        self.assertEqual(validateList(public_ip_ranges)[0], PASS,
                         "public ip ranges list validation failed")
        self.assertEqual(
            public_ip_ranges[0].account,
            account.name,
            "Check account name is in listVlanIpRanges\
               as the account public ip range is dedicated to")

        isolated_network = Network.create(
            self.apiclient,
            self.testdata["isolated_network"],
            account.name,
            account.domainid,
            networkofferingid=self.isolated_network_offering.id)

        formatted_startip = IPAddress(
            self.testdata["publiciprange"]["startip"])
        formatted_endip = IPAddress(self.testdata["publiciprange"]["endip"])
        public_ip1 = PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_network.id)

        # Format the IP so that it can be used to compare with other IPs
        formatted_publicip = IPAddress(public_ip1.ipaddress.ipaddress)

        self.assertTrue(int(formatted_startip) <=
                        int(formatted_publicip) <= int(formatted_endip),
                        "publicip should be from the dedicated range")

        public_ip_range.release(self.apiclient)

        public_ip_ranges = PublicIpRange.list(
            self.apiclient,
            id=public_ip_range.vlan.id
        )
        self.assertEqual(validateList(public_ip_ranges)[0], PASS,
                         "public ip ranges list validation failed")
        self.assertEqual(
            str(public_ip_ranges[0].account).lower(),
            "system",
            "Check account name is system")
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_16_delete_ip_range_no_network_rules(self):
        """Delete dedicated public IP range with no network rules on IP addresses

        # Validate the following:
        # 1. Create account in root domain
        # 2. Create guest network in it
        # 3. Create a Public IP range
        # 4. Dedicate the created IP range to user account
        # 5. Acquire public IP in guest network
        # 6. Verify that IP address in from dedicated range
        # 7. Delete the dedicated public IP range
        # 8. The operation should be successful
        """
        # Create Account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(account)

        isolated_network = Network.create(
            self.apiclient,
            self.testdata["isolated_network"],
            account.name,
            account.domainid,
            networkofferingid=self.isolated_network_offering.id)

        # Acquiring ip for source nat purposes
        PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_network.id)

        public_ip_range = PublicIpRange.create(
            self.apiclient,
            self.testdata["publiciprange"]
        )
        self.cleanup.append(public_ip_range)
        PublicIpRange.dedicate(
            self.apiclient,
            public_ip_range.vlan.id,
            account=account.name,
            domainid=account.domainid
        )

        formatted_startip = IPAddress(
            self.testdata["publiciprange"]["startip"])
        formatted_endip = IPAddress(self.testdata["publiciprange"]["endip"])
        public_ip1 = PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_network.id)

        formatted_publicip = IPAddress(public_ip1.ipaddress.ipaddress)

        self.assertTrue(int(formatted_startip) <=
                        int(formatted_publicip) <= int(formatted_endip),
                        "publicip should be from the dedicated range")

        public_ip_range.delete(self.apiclient)
        self.cleanup.remove(public_ip_range)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_17_delete_ip_range_network_rules(self):
        """Delete Dedicated public IP range with network rules on IP addresses

        # Validate the following:
        # 1. Create an account
        # 2. Create a guest network in the account
        # 3. Create a Public IP range
        # 4  Dedicate the created IP range to user account
        # 5. Acquire a public IP for the network
        # 6. Verify that it is from the dedicated range
        # 7. Create a LB rule for the public IP address
        # 8. Try to delete the public IP range, the operation should fail
        """
        # Create Account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(account)

        isolated_network = Network.create(
            self.apiclient,
            self.testdata["isolated_network"],
            account.name,
            account.domainid,
            networkofferingid=self.isolated_network_offering.id)

        # Acquiring IP for source nat purpose
        PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_network.id)

        public_ip_range = PublicIpRange.create(
            self.apiclient,
            self.testdata["publiciprange"]
        )
        self.cleanup.append(public_ip_range)
        PublicIpRange.dedicate(
            self.apiclient,
            public_ip_range.vlan.id,
            account=account.name,
            domainid=account.domainid
        )

        formatted_startip = IPAddress(
            self.testdata["publiciprange"]["startip"])
        formatted_endip = IPAddress(self.testdata["publiciprange"]["endip"])
        public_ip = PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_network.id)

        formatted_publicip = IPAddress(public_ip.ipaddress.ipaddress)

        self.assertTrue(int(formatted_startip) <=
                        int(formatted_publicip) <= int(formatted_endip),
                        "publicip should be from the dedicated range")

        LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=public_ip.ipaddress.id,
            accountid=account.name,
            networkid=isolated_network.id,
            domainid=account.domainid)

        with self.assertRaises(Exception):
            public_ip_range.delete(self.apiclient)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_18_delete_network(self):
        """Delete network with public IP from dedicated range acquired in it

        # Validate the following:
        # 1. Create an account
        # 2. Create a guest network in it
        # 1. Create a Public IP range
        # 3. Dedicate the created IP range to user account
        # 5. Acquire a public IP in the network
        # 6. Verify that the IP is from the dedicated range
        # 7. Delete the network
        # 8. Verify that the public IP range is still dedicated to the account
        """
        # Create Account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(account)

        isolated_network = Network.create(
            self.apiclient,
            self.testdata["isolated_network"],
            account.name,
            account.domainid,
            networkofferingid=self.isolated_network_offering.id)

        # Acuiring IP for source nat purpose
        PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_network.id)

        public_ip_range = PublicIpRange.create(
            self.apiclient,
            self.testdata["publiciprange"]
        )
        self.cleanup.append(public_ip_range)
        PublicIpRange.dedicate(
            self.apiclient,
            public_ip_range.vlan.id,
            account=account.name,
            domainid=account.domainid
        )

        formatted_startip = IPAddress(
            self.testdata["publiciprange"]["startip"])
        formatted_endip = IPAddress(self.testdata["publiciprange"]["endip"])
        public_ip1 = PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_network.id)

        formatted_publicip = IPAddress(public_ip1.ipaddress.ipaddress)

        self.assertTrue(int(formatted_startip) <=
                        int(formatted_publicip) <= int(formatted_endip),
                        "publicip should be from the dedicated range")

        isolated_network.delete(self.apiclient)

        public_ip_ranges = PublicIpRange.list(
            self.apiclient,
            id=public_ip_range.vlan.id
        )
        self.assertEqual(validateList(public_ip_ranges)[0], PASS,
                         "public ip ranges list validation failed")
        self.assertEqual(
            public_ip_ranges[0].account,
            account.name,
            "Check account name is in listVlanIpRanges\
               as the account public ip range is dedicated to")
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_19_delete_account(self):
        """Create guest network in account with dedicated public ip range

        # Validate the following:
        # 1. Create an account
        # 2. Create a Public IP range
        # 3. Dedicate the created IP range to user account
        # 4. Delete the account
        # 5. Verify that the public IP range is released back to the system
        """
        # Create Account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(account)

        isolated_network = Network.create(
            self.apiclient,
            self.testdata["isolated_network"],
            account.name,
            account.domainid,
            networkofferingid=self.isolated_network_offering.id)

        public_ip_range = PublicIpRange.create(
            self.apiclient,
            self.testdata["publiciprange"]
        )
        self.cleanup.append(public_ip_range)
        PublicIpRange.dedicate(
            self.apiclient,
            public_ip_range.vlan.id,
            account=account.name,
            domainid=account.domainid
        )

        formatted_startip = IPAddress(
            self.testdata["publiciprange"]["startip"])
        formatted_endip = IPAddress(self.testdata["publiciprange"]["endip"])
        public_ip1 = PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_network.id)

        formatted_publicip = IPAddress(public_ip1.ipaddress.ipaddress)

        self.assertTrue(int(formatted_startip) <=
                        int(formatted_publicip) <= int(formatted_endip),
                        "publicip should be from the dedicated range")

        account.delete(self.apiclient)
        self.cleanup.remove(account)

        public_ip_ranges = PublicIpRange.list(
            self.apiclient,
            id=public_ip_range.vlan.id
        )
        self.assertEqual(validateList(public_ip_ranges)[0], PASS,
                         "public ip ranges list validation failed")
        self.assertEqual(
            str(public_ip_ranges[0].account).lower(),
            "system",
            "Account name should be system")
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_20_release_range_not_dedicated(self):
        """Create guest network in account with dedicated public ip range

        # Validate the following:
        # 1. Create a Public IP range
        # 2. Do not dedicate it to any account, try to release it
        # 3. Operation should fail as the range is not dedicated
        """
        public_ip_range = PublicIpRange.create(
            self.apiclient,
            self.testdata["publiciprange"]
        )
        self.cleanup.append(public_ip_range)

        with self.assertRaises(Exception):
            public_ip_range.release(self.apiclient)
        return
