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

"""
Tests of user-private gateway
"""

import logging
import time

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources, random_gen

from marvin.lib.base import (Account,
                             Domain,
                             Project,
                             Configurations,
                             ServiceOffering,
                             Zone,
                             Network,
                             NetworkOffering,
                             VPC,
                             VpcOffering,
                             PrivateGateway)

from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)

NETWORK_STATE_ALLOCATED = "Allocated"
NETWORK_STATE_IMPLEMENTED = "Implemented"
NETWORK_STATE_SETUP = "Setup"
NETWORK_STATE_REMOVED = "Removed"

class TestUserPrivateGateways(cloudstackTestCase):
    """
    Test user-shared networks
    """
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestUserPrivateGateways,
            cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.zone = Zone(zone.__dict__)
        cls.template = get_template(cls.apiclient, cls.zone.id)
        cls._cleanup = []

        cls.logger = logging.getLogger("TestUserPrivateGateways")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        cls.domain = get_domain(cls.apiclient)

        # Create small service offering
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )
        cls._cleanup.append(cls.service_offering)

        # Create network offering for isolated networks
        cls.network_offering_isolated = NetworkOffering.create(
            cls.apiclient,
            cls.services["network_offering"]
        )
        cls.network_offering_isolated.update(cls.apiclient, state='Enabled')
        cls._cleanup.append(cls.network_offering_isolated)

        # Create vpc offering
        cls.vpc_offering = VpcOffering.create(
            cls.apiclient,
            cls.services["vpc_offering_multi_lb"])
        cls.vpc_offering.update(cls.apiclient, state='Enabled')
        cls._cleanup.append(cls.vpc_offering)

        # Create network offering for vpc tiers
        cls.network_offering_vpc = NetworkOffering.create(
            cls.apiclient,
            cls.services["nw_offering_isolated_vpc"],
            conservemode=False
        )
        cls.network_offering_vpc.update(cls.apiclient, state='Enabled')
        cls._cleanup.append(cls.network_offering_vpc)

        # Create sub-domain
        cls.sub_domain = Domain.create(
            cls.apiclient,
            cls.services["acl"]["domain1"]
        )
        cls._cleanup.append(cls.sub_domain)

        # Create domain admin and normal user
        cls.domain_admin = Account.create(
            cls.apiclient,
            cls.services["acl"]["accountD1A"],
            admin=True,
            domainid=cls.sub_domain.id
        )
        cls._cleanup.append(cls.domain_admin)

        cls.normal_user = Account.create(
            cls.apiclient,
            cls.services["acl"]["accountD1B"],
            domainid=cls.sub_domain.id
        )
        cls._cleanup.append(cls.normal_user)

        # Create project
        cls.project = Project.create(
          cls.apiclient,
          cls.services["project"],
          account=cls.domain_admin.name,
          domainid=cls.domain_admin.domainid
        )
        cls._cleanup.append(cls.project)

        # Create api clients for domain admin and normal user
        cls.domainadmin_user = cls.domain_admin.user[0]
        cls.domainapiclient = cls.testClient.getUserApiClient(
            cls.domainadmin_user.username, cls.sub_domain.name
        )
        cls.normaluser_user = cls.normal_user.user[0]
        cls.normaluser_apiclient = cls.testClient.getUserApiClient(
            cls.normaluser_user.username, cls.sub_domain.name
        )

    @classmethod
    def tearDownClass(cls):
        super(TestUserPrivateGateways, cls).tearDownClass()

    def setUp(self):
        self.cleanup = []

    def tearDown(self):
        super(TestUserPrivateGateways, self).tearDown()

    def delete_network(self, network, apiclient, expected=True):
        result = True
        try:
            Network.delete(
                network,
                apiclient,
            )
        except Exception as ex:
            result = False
            if expected:
                self.fail(f"Failed to remove Shared network, but expected to succeed : {ex}")
        if result and not expected:
            self.fail("network is removed successfully, but expected to fail")

    def create_isolated_network_for_account(self, apiclient, domain, account, project, expected=True):
        self.services["network"]["name"] = "Test Network Isolated - " + random_gen()
        domain_id = None
        account_name = None
        project_id = None
        if domain:
            domain_id = domain.id
        if account:
            account_name = account.name
        if project:
            project_id = project.id
        try:
            network = Network.create(
                apiclient,
                self.services["network"],
                domainid=domain_id,
                accountid=account_name,
                projectid=project_id,
                networkofferingid=self.network_offering_isolated.id,
                zoneid=self.zone.id
            )
        except Exception as ex:
            network = None
            if expected:
                self.fail(f"Failed to create Isolated network, but expected to succeed : {ex}")
        if network and not expected:
            self.fail("Isolated network is created successfully, but expected to fail")
        return network

    def check_network_state(self, apiclient, network, project, expected_state):
        project_id = None
        if project:
            project_id = project.id
        networks = Network.list(
            apiclient,
            listall=True,
            projectid=project_id,
            id=network.id
        )
        if isinstance(networks, list) and len(networks) > 0:
            if expected_state == NETWORK_STATE_REMOVED:
                self.fail("Found the network, but expected to fail")
            if networks[0].state != expected_state:
                self.fail(f"Expect network state is {expected_state}, but actual state is {networks[0].state}")
        elif expected_state != NETWORK_STATE_REMOVED:
            self.fail("Failed to find the network, but expected to succeed")

    def create_vpc_for_account(self, apiclient, domain, account, project):
        self.services["vpc"]["name"] = "Test VPC - " + random_gen()
        self.services["vpc"]["cidr"] = "10.1.0.0/20"
        domain_id = None
        account_name = None
        if domain:
            domain_id = domain.id
        if account:
            account_name = account.name
        project_id = None
        if project:
            project_id = project.id
        vpc = VPC.create(
            apiclient,
            self.services["vpc"],
            domainid=domain_id,
            accountid=account_name,
            projectid=project_id,
            vpcofferingid=self.vpc_offering.id,
            zoneid=self.zone.id,
            start=False
        )
        return vpc

    def create_vpc_tier_for_account(self, apiclient, vpc, project=None, gateway = '10.1.1.1'):
        self.services["network"]["name"] = "Test VPC tier - " + random_gen()
        project_id = None
        if project:
            project_id = project.id
        vpc_tier = Network.create(
            apiclient,
            self.services["network"],
            networkofferingid=self.network_offering_vpc.id,
            zoneid=self.zone.id,
            projectid=project_id,
            gateway=gateway,
            netmask="255.255.255.0",
            vpcid=vpc.id
        )
        return vpc_tier

    def create_vpc_private_gateway(self, apiclient, vpc, vlan_id, associated_network=None, expected=True):
        self.services["private_gateway"]["name"] = "Test Network Isolated - " + random_gen()
        associated_network_id = None
        if associated_network:
            associated_network_id = associated_network.id
        private_gateway = None
        try:
            private_gateway = PrivateGateway.create(
                apiclient,
                vpcid=vpc.id,
                gateway = self.services["private_gateway"]["gateway"],
                ipaddress = self.services["private_gateway"]["ipaddress"],
                netmask = self.services["private_gateway"]["netmask"],
                vlan = vlan_id,
                associatednetworkid = associated_network_id
            )
        except Exception as ex:
            private_gateway = None
            if expected:
                self.fail(f"Failed to create private gateway, but expected to succeed : {ex}")
        if private_gateway and not expected:
            self.fail("private gateway is created successfully, but expected to fail")
        return private_gateway

    def delete_vpc_private_gateway(self, apiclient, private_gateway, expected=True):
        result = True
        try:
            private_gateway.delete(apiclient)
        except Exception as ex:
            result = False
            if expected:
                self.fail(f"Failed to remove private gateway, but expected to succeed : {ex}")
        if result and not expected:
            self.fail("private gateway is removed successfully, but expected to fail")

    def delete_vpc(self, apiclient, vpc, expected=True):
        result = True
        try:
            vpc.delete(apiclient)
        except Exception as ex:
            result = False
            if expected:
                self.fail(f"Failed to remove VPC, but expected to succeed : {ex}")
        if result and not expected:
            self.fail("VPC is removed successfully, but expected to fail")

    @attr(tags=["advanced"], required_hardware="false")
    def test_01_create_private_gateway_with_vlan(self):
        """ Create private gateway with vlan """

        # Create VPC
        vpc1 = self.create_vpc_for_account(self.apiclient, None, None, None)
        vpc2 = self.create_vpc_for_account(self.domainapiclient, None, None, None)
        vpc3 = self.create_vpc_for_account(self.normaluser_apiclient, None, None, None)
        vpc4 = self.create_vpc_for_account(self.domainapiclient, None, None, self.project)

        # Create VPC private gateway with vlan (can only be done by ROOT admin)
        private_gateway1 = self.create_vpc_private_gateway(self.apiclient, vpc1, 10, None, True)
        self.create_vpc_private_gateway(self.domainapiclient, vpc2, 11, None, False)
        self.create_vpc_private_gateway(self.normaluser_apiclient, vpc3, 12, None, False)
        self.create_vpc_private_gateway(self.domainapiclient, vpc4, 13, None, False)
        private_gateway2 = self.create_vpc_private_gateway(self.apiclient, vpc2, 11, None, True)
        private_gateway3 = self.create_vpc_private_gateway(self.apiclient, vpc3, 12, None, True)
        private_gateway4 = self.create_vpc_private_gateway(self.apiclient, vpc4, 13, None, True)

        # Delete VPC private gateway (should succeed by ROOT admin)
        self.delete_vpc_private_gateway(self.apiclient, private_gateway1, True)
        self.delete_vpc_private_gateway(self.domainapiclient, private_gateway2, False)
        self.delete_vpc_private_gateway(self.normaluser_apiclient, private_gateway2, False)
        self.delete_vpc_private_gateway(self.domainapiclient, private_gateway4, False)
        self.delete_vpc_private_gateway(self.apiclient, private_gateway2, True)
        self.delete_vpc_private_gateway(self.apiclient, private_gateway3, True)
        self.delete_vpc_private_gateway(self.apiclient, private_gateway4, True)

        # Delete VPC
        self.delete_vpc(self.apiclient, vpc1)
        self.delete_vpc(self.domainapiclient, vpc2)
        self.delete_vpc(self.normaluser_apiclient, vpc3)
        self.delete_vpc(self.domainapiclient, vpc4)

    @attr(tags=["advanced"], required_hardware="false")
    def test_02_create_private_gateway_with_associated_network(self):
        """ Create private gateway with associated network """

        self.services["network"]["networkoffering"] = self.network_offering_isolated.id
        self.services["network"]["vlan"] = None

        # Create isolated networks
        isolated_network1 = self.create_isolated_network_for_account(self.apiclient, None, None, None)
        isolated_network2 = self.create_isolated_network_for_account(self.apiclient, self.sub_domain, self.domain_admin, None)
        isolated_network3 = self.create_isolated_network_for_account(self.apiclient, self.sub_domain, self.normal_user, None)
        isolated_network4 = self.create_isolated_network_for_account(self.apiclient, self.sub_domain, None, self.project)

        # Check state of isolated networks (should be Allocated)
        self.check_network_state(self.apiclient, isolated_network1, None, NETWORK_STATE_ALLOCATED)
        self.check_network_state(self.domainapiclient, isolated_network2, None, NETWORK_STATE_ALLOCATED)
        self.check_network_state(self.normaluser_apiclient, isolated_network3, None, NETWORK_STATE_ALLOCATED)
        self.check_network_state(self.domainapiclient, isolated_network4, self.project, NETWORK_STATE_ALLOCATED)

        # Create VPC
        vpc1 = self.create_vpc_for_account(self.apiclient, None, None, None)
        vpc2 = self.create_vpc_for_account(self.domainapiclient, None, None, None)
        vpc3 = self.create_vpc_for_account(self.normaluser_apiclient, None, None, None)
        vpc4 = self.create_vpc_for_account(self.domainapiclient, None, None, self.project)

        # Create VPC tier
        vpc1_tier1 = self.create_vpc_tier_for_account(self.apiclient, vpc1)
        vpc2_tier1 = self.create_vpc_tier_for_account(self.domainapiclient, vpc2)
        vpc3_tier1 = self.create_vpc_tier_for_account(self.normaluser_apiclient, vpc3)
        vpc4_tier1 = self.create_vpc_tier_for_account(self.domainapiclient, vpc4, self.project)

        # Create VPC private gateway with associated network (can be done by ROOT admin / domain admin / normal user)
        private_gateway1 = self.create_vpc_private_gateway(self.apiclient, vpc1, None, isolated_network1, True)
        private_gateway2 = self.create_vpc_private_gateway(self.domainapiclient, vpc2, None, isolated_network2, True)
        private_gateway3 = self.create_vpc_private_gateway(self.normaluser_apiclient, vpc3, None, isolated_network3, True)
        private_gateway4 = self.create_vpc_private_gateway(self.domainapiclient, vpc4, None, isolated_network4, True)

        # Check state of isolated networks (should be Implemented)
        self.check_network_state(self.apiclient, isolated_network1, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.domainapiclient, isolated_network2, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.normaluser_apiclient, isolated_network3, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.domainapiclient, isolated_network4, self.project, NETWORK_STATE_IMPLEMENTED)

        # Delete VPC private gateway of domain admin (should succeed)
        self.delete_vpc_private_gateway(self.domainapiclient, private_gateway2, True)

        # Wait for network GC to shut down the isolated networks
        gc_wait = Configurations.list(self.apiclient, name="network.gc.wait")
        gc_interval = Configurations.list(self.apiclient, name="network.gc.interval")
        total_sleep = 360
        if gc_wait and gc_interval:
            self.logger.debug("network.gc.wait is ==> %s", gc_wait[0].value)
            self.logger.debug("network.gc.interval is ==> %s", gc_interval[0].value)
            total_sleep = max(int(gc_wait[0].value), int(gc_interval[0].value)) * 2 + 60
        else:
            self.logger.debug("Could not retrieve the keys 'network.gc.interval' and 'network.gc.wait'. Sleeping for 6 minutes.")
        time.sleep(total_sleep)

        # Check state of isolated networks (1 should be Allocated, 2 should still be Implemented)
        self.check_network_state(self.apiclient, isolated_network1, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.domainapiclient, isolated_network2, None, NETWORK_STATE_ALLOCATED)
        self.check_network_state(self.normaluser_apiclient, isolated_network3, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.domainapiclient, isolated_network4, self.project, NETWORK_STATE_IMPLEMENTED)

        # Delete 2 VPC private gateway (should succeed)
        self.delete_vpc_private_gateway(self.apiclient, private_gateway1, True)
        self.delete_vpc_private_gateway(self.apiclient, private_gateway3, True)
        self.delete_vpc_private_gateway(self.apiclient, private_gateway4, True)

        # Delete VPC tiers (should succeed)
        self.delete_network(vpc1_tier1, self.apiclient, True)
        self.delete_network(vpc2_tier1, self.domainapiclient, True)
        self.delete_network(vpc3_tier1, self.normaluser_apiclient, True)
        self.delete_network(vpc4_tier1, self.domainapiclient, True)

        # Delete VPC
        self.delete_vpc(self.apiclient, vpc1)
        self.delete_vpc(self.domainapiclient, vpc2)
        self.delete_vpc(self.normaluser_apiclient, vpc3)
        self.delete_vpc(self.domainapiclient, vpc4)

        # Delete isolated networks (should succeed)
        self.delete_network(isolated_network1, self.apiclient, True)
        self.delete_network(isolated_network2, self.domainapiclient, True)
        self.delete_network(isolated_network3, self.normaluser_apiclient, True)
        self.delete_network(isolated_network4, self.domainapiclient, True)
