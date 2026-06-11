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

"""Smoke tests for firewall rules on VPC public IPs."""

from nose.plugins.attrib import attr

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import Account, FireWallRule, Network, NetworkOffering, PublicIPAddress, VPC, VpcOffering
from marvin.lib.common import get_domain, get_zone, list_publicIP
from marvin.lib.utils import cleanup_resources, wait_until


class TestVpcFirewallRules(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVpcFirewallRules, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.domain = get_domain(cls.apiclient)
        cls._cleanup = []

        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.services["vpc_offering"]["supportedservices"] = "Vpn,Dhcp,Dns,SourceNat,Lb,UserData,StaticNat,NetworkACL,PortForwarding,Firewall"
        cls.services["vpc_offering"]["serviceProviderList"] = {
            "Vpn": "VpcVirtualRouter",
            "Dhcp": "VpcVirtualRouter",
            "Dns": "VpcVirtualRouter",
            "SourceNat": "VpcVirtualRouter",
            "Lb": "VpcVirtualRouter",
            "UserData": "VpcVirtualRouter",
            "StaticNat": "VpcVirtualRouter",
            "NetworkACL": "VpcVirtualRouter",
            "PortForwarding": "VpcVirtualRouter",
            "Firewall": "VpcVirtualRouter"
        }

        cls.vpc_offering = VpcOffering.create(
            cls.apiclient,
            cls.services["vpc_offering"]
        )
        cls.vpc_offering.update(cls.apiclient, state="Enabled")
        cls._cleanup.append(cls.vpc_offering)

        network_offering = NetworkOffering.list(
            cls.apiclient,
            name="DefaultIsolatedNetworkOfferingForVpcNetworks"
        )
        cls.assertTrue(network_offering is not None and len(network_offering) > 0,
                       "No VPC tier network offering found")
        cls.network_offering = network_offering[0]

        cls.services["vpc"]["cidr"] = "10.20.30.0/24"
        cls.vpc = VPC.create(
            cls.apiclient,
            cls.services["vpc"],
            vpcofferingid=cls.vpc_offering.id,
            zoneid=cls.zone.id,
            account=cls.account.name,
            domainid=cls.account.domainid
        )
        cls._cleanup.append(cls.vpc)

        cls.tier = Network.create(
            cls.apiclient,
            services={"name": "vpc-fw-tier", "displaytext": "vpc-fw-tier"},
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            networkofferingid=cls.network_offering.id,
            zoneid=cls.zone.id,
            vpcid=cls.vpc.id,
            gateway="10.20.30.1",
            netmask="255.255.255.0"
        )
        cls._cleanup.append(cls.tier)

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup: %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)

    def _wait_for_firewall_rule(self, rule_id):
        rules = FireWallRule.list(self.apiclient, id=rule_id, listall=True)
        if rules and len(rules) == 1:
            return True, rules[0]
        return False, None

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="false")
    def test_01_create_firewall_rule_on_vpc_public_ip(self):
        """Verify firewall rule can be created and listed on a dedicated VPC public IP."""
        public_ip = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            vpcid=self.vpc.id
        )
        self.cleanup.append(public_ip)

        firewall_rule = FireWallRule.create(
            self.apiclient,
            ipaddressid=public_ip.ipaddress.id,
            protocol="tcp",
            cidrlist=["0.0.0.0/0"],
            startport=19090,
            endport=19090,
            vpcid=self.vpc.id
        )
        self.cleanup.insert(0, firewall_rule)

        result, listed_rule = wait_until(2, 10, self._wait_for_firewall_rule, firewall_rule.id)
        self.assertTrue(result, "Firewall rule was not listed for the VPC public IP")
        self.assertEqual(listed_rule.id, firewall_rule.id)
        self.assertEqual(listed_rule.ipaddressid, public_ip.ipaddress.id)
        self.assertEqual(listed_rule.vpcid, self.vpc.id)
        self.assertEqual(listed_rule.protocol.lower(), "tcp")
        self.assertEqual(int(listed_rule.startport), 19090)
        self.assertEqual(int(listed_rule.endport), 19090)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="false")
    def test_02_create_firewall_rule_on_vpc_source_nat_ip(self):
        """Verify firewall rule can be created and listed on the VPC source NAT IP."""
        source_nat_ips = list_publicIP(
            self.apiclient,
            vpcid=self.vpc.id,
            listall=True,
            issourcenat=True
        )
        self.assertTrue(source_nat_ips is not None and len(source_nat_ips) > 0,
                        "No source NAT IP found for the VPC")
        source_nat_ip = source_nat_ips[0]

        firewall_rule = FireWallRule.create(
            self.apiclient,
            ipaddressid=source_nat_ip.id,
            protocol="tcp",
            cidrlist=["0.0.0.0/0"],
            startport=19443,
            endport=19443,
            vpcid=self.vpc.id
        )
        self.cleanup.append(firewall_rule)

        result, listed_rule = wait_until(2, 10, self._wait_for_firewall_rule, firewall_rule.id)
        self.assertTrue(result, "Firewall rule was not listed for the VPC source NAT IP")
        self.assertEqual(listed_rule.id, firewall_rule.id)
        self.assertEqual(listed_rule.ipaddressid, source_nat_ip.id)
        self.assertEqual(listed_rule.vpcid, self.vpc.id)
        self.assertEqual(listed_rule.protocol.lower(), "tcp")
        self.assertEqual(int(listed_rule.startport), 19443)
        self.assertEqual(int(listed_rule.endport), 19443)
