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

"""Tests for VPC Conserve Mode (since 4.23.0)

Conserve mode allows public IP services (LB, Port Forwarding, Static NAT) to be
shared across multiple VPC tiers using the same public IP address.

When conserve mode is ON:
  - A single public IP can have rules targeting VMs in different VPC tiers
  - FirewallManagerImpl skips the cross-network conflict check for that VPC

When conserve mode is OFF (default before 4.23.0):
  - Rules on a given public IP must all belong to the same VPC tier (network)
  - Attempting to create a rule on a different tier than an existing rule raises
    a NetworkRuleConflictException
"""

from marvin.cloudstackException import CloudstackAPIException
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.codes import FAILED
from marvin.lib.base import (
    Account,
    LoadBalancerRule,
    NATRule,
    Network,
    NetworkOffering,
    PublicIPAddress,
    ServiceOffering,
    VirtualMachine,
    VPC,
    VpcOffering,
)
from marvin.lib.common import (
    get_domain,
    get_test_template,
    get_zone,
    list_publicIP
)
from marvin.lib.utils import cleanup_resources
from nose.plugins.attrib import attr
import logging

class TestVPCConserveModeRules(cloudstackTestCase):
    """Tests that conserve mode for VPC controls whether rules on the same public IP are allowed in multiple VPC tiers.
    """

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVPCConserveModeRules, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.domain = get_domain(cls.apiclient)
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.logger = logging.getLogger("TestVPCConserveModeRules")

        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id)
        cls._cleanup.append(cls.account)

        cls.template = get_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor)
        if cls.template == FAILED:
            assert False, "get_test_template() failed to return template"

        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls._cleanup.append(cls.service_offering)

        cls.vpc_offering_conserve_mode = VpcOffering.create(
            cls.apiclient,
            cls.services["vpc_offering"],
            conservemode=True,
        )
        cls._cleanup.append(cls.vpc_offering_conserve_mode)

        cls.vpc_offering_conserve_mode.update(cls.apiclient, state="Enabled")

        cls.network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.services["network_offering"],
            conservemode=True
        )
        cls.network_offering.update(cls.apiclient, state="Enabled")

        cls.services["vpc"]["cidr"] = "10.10.20.0/24"

        cls.vpc = VPC.create(
            cls.apiclient,
            cls.services["vpc"],
            vpcofferingid=cls.vpc_offering_conserve_mode.id,
            zoneid=cls.zone.id,
            account=cls.account.name,
            domainid=cls.account.domainid,
        )

        gateway_tier1 = "10.10.20.1"
        netmask_tiers = "255.255.255.240"

        cls.services["network_offering"]["name"] = "tier1-" + cls.vpc.id
        cls.services["network_offering"]["displayname"] = "tier1-" + cls.vpc.id
        cls.tier1 = Network.create(
            cls.self.apiclient,
            services=cls.services["network_offering"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            networkofferingid=cls.network_offering.id,
            zoneid=cls.zone.id,
            vpcid=cls.vpc.id,
            gateway=gateway_tier1,
            netmask=netmask_tiers,
        )

        gateway_tier2 = "10.10.20.17"
        cls.services["network_offering"]["name"] = "tier2-" + cls.vpc.id
        cls.services["network_offering"]["displayname"] = "tier2-" + cls.vpc.id
        cls.tier2 = Network.create(
            cls.apiclient,
            services=cls.services["network_offering"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            networkofferingid=cls.network_offering.id,
            zoneid=cls.zone.id,
            vpcid=cls.vpc.id,
            gateway=gateway_tier2,
            netmask=netmask_tiers,
        )

        cls.services["virtual_machine"]["displayname"] = "vm1" + cls.vpc.id
        cls.vm1 = VirtualMachine.create(
            cls.apiclient,
            services=cls.services["virtual_machine"],
            templateid=cls.template.id,
            zoneid=cls.zone.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            networkids=[cls.tier1.id],
        )
        cls.services["virtual_machine"]["displayname"] = "vm2" + cls.vpc.id
        cls.vm2 = VirtualMachine.create(
            cls.apiclient,
            services=cls.services["virtual_machine"],
            templateid=cls.template.id,
            zoneid=cls.zone.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            networkids=[cls.tier2.id],
        )

    @classmethod
    def tearDownClass(cls):
        super(TestVPCConserveModeRules, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        super(TestVPCConserveModeRules, self).tearDown()

    @attr(tags=["advanced"], required_hardware="true")
    def test_01_vpc_conserve_mode_cross_tier_rules_allowed(self):
        """With conserveMode=True, LB rule on VPC Tier 1 and Port Forwarding rule on VPC Tier 2 can
        share the same public IP without a NetworkRuleConflictException.
        """

        public_ip = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            vpcid=self.vpc.id,
        )

        self.logger.debug(
            "Creating LB rule on tier-1 (networkid=%s) using public IP %s",
            self.tier1.id,
            public_ip.ipaddress.ipaddress,
        )
        lb_rule_tier1 = LoadBalancerRule.create(
            self.apiclient,
            self.services["lbrule"],
            ipaddressid=public_ip.ipaddress.id,
            accountid=self.account.name,
            vpcid=self.vpc.id,
            networkid=self.tier1.id,
            domainid=self.account.domainid,
        )
        self.assertIsNotNone(lb_rule_tier1, "LB rule creation on tier-1 failed")
        lb_rule_tier1.assign(self.apiclient, [self.vm1])

        self.logger.debug(
            "Creating Port Forwarding rule on tier-2 (networkid=%s) "
            "using the same public IP %s – should succeed with conserve mode",
            self.tier2.id,
            public_ip.ipaddress.ipaddress,
        )
        try:
            nat_rule = NATRule.create(
                self.apiclient,
                self.vm2,
                self.services["natrule"],
                ipaddressid=public_ip.ipaddress.id,
                vpcid=self.vpc.id,
                networkid=self.tier2.id,
            )
            self.assertIsNotNone(
                nat_rule,
                "Port Forwarding rule creation on tier-2 failed unexpectedly",
            )
        except CloudstackAPIException as e:
            self.fail(
                "Expected cross-tier Port Forwarding rule to succeed with "
                "conserveMode=True, but got exception: %s" % e
            )

    @attr(tags=["advanced"], required_hardware="true")
    def test_02_vpc_conserve_mode_reuse_source_nat_ip_address(self):
        """With VPC conserve mode enabled, a NAT rule can be created on a VPC tier (conserve mode enabled)
        with a source NAT IP address
        """
        source_nat_ip_resp = list_publicIP(
            self.apiclient,
            vpcid=self.vpc.id,
            listall=True,
            issourcenat=True
        )

        source_nat_ip = source_nat_ip_resp[0]

        self.logger.debug(
            "Creating Port Forwarding rule on tier-1 (networkid=%s) "
            "using the source NAT public IP %s – should succeed with conserve mode",
            self.tier1.id,
            source_nat_ip.ipaddress.ipaddress,
        )
        try:
            nat_rule = NATRule.create(
                self.apiclient,
                self.vm2,
                self.services["natrule"],
                ipaddressid=source_nat_ip.ipaddress.id,
                vpcid=self.vpc.id,
                networkid=self.tier2.id,
            )
            self.assertIsNotNone(
                nat_rule,
                "Port Forwarding rule creation on tier-2 failed unexpectedly",
            )
        except CloudstackAPIException as e:
            self.fail(
                "Expected cross-tier Port Forwarding rule to succeed with "
                "conserveMode=True, but got exception: %s" % e
            )
