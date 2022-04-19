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
Tests of user-shared networks
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
                             VpcOffering)

from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)

NETWORK_STATE_ALLOCATED = "Allocated"
NETWORK_STATE_IMPLEMENTED = "Implemented"
NETWORK_STATE_SETUP = "Setup"
NETWORK_STATE_REMOVED = "Removed"

class TestUserSharedNetworks(cloudstackTestCase):
    """
    Test user-shared networks
    """
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestUserSharedNetworks,
            cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.zone = Zone(zone.__dict__)
        cls.template = get_template(cls.apiclient, cls.zone.id)
        cls._cleanup = []

        cls.logger = logging.getLogger("TestUserSharedNetworks")
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

        # Create network offering for user-shared networks (specifyVlan=true)
        cls.network_offering_withvlan = NetworkOffering.create(
            cls.apiclient,
            cls.services["network_offering_shared"]
        )
        cls.network_offering_withvlan.update(cls.apiclient, state='Enabled')
        cls._cleanup.append(cls.network_offering_withvlan)

        # Create network offering for user-shared networks (specifyVlan=false)
        cls.services["network_offering_shared"]["specifyVlan"] = "False"
        cls.network_offering_novlan = NetworkOffering.create(
            cls.apiclient,
            cls.services["network_offering_shared"]
        )
        cls.network_offering_novlan.update(cls.apiclient, state='Enabled')
        cls._cleanup.append(cls.network_offering_novlan)

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
        super(TestUserSharedNetworks, cls).tearDownClass()

    def setUp(self):
        self.cleanup = []

    def tearDown(self):
        super(TestUserSharedNetworks, self).tearDown()

    def create_shared_network_for_account(self, apiclient, domain, account, expected=True):
        return self.create_shared_network_with_associated_network(apiclient, domain, account, None, None, expected)

    def create_shared_network_with_associated_network_for_domain(self, apiclient, domain, associated_network, expected=True):
        return self.create_shared_network_with_associated_network(apiclient, domain, None, None, associated_network, expected)

    def create_shared_network_with_associated_network_for_caller(self, apiclient, project, associated_network, expected=True):
        return self.create_shared_network_with_associated_network(apiclient, None, None, project, associated_network, expected)

    def create_shared_network_with_associated_network(self, apiclient, domain, account, project, associated_network, expected=True):
        self.services["network2"]["acltype"] = "Account"
        self.services["network2"]["name"] = "Test Network Shared - " + random_gen()
        domain_id = None
        account_name = None
        project_id = None
        if domain:
            self.services["network2"]["acltype"] = "Domain"
            domain_id = domain.id
        if account:
            self.services["network2"]["acltype"] = "Account"
            account_name = account.name
        if project:
            self.services["network2"]["acltype"] = "Account"
            project_id = project.id
        associated_network_id = None
        if associated_network:
            associated_network_id = associated_network.id
        try:
            network = Network.create(
                apiclient,
                self.services["network2"],
                domainid=domain_id,
                accountid=account_name,
                projectid=project_id,
                associatednetworkid=associated_network_id,
                zoneid=self.zone.id
            )
        except Exception as ex:
            network = None
            if expected:
                self.fail(f"Failed to create Shared network, but expected to succeed : {ex}")
        if network and not expected:
            self.fail("Shared network is created successfully, but expected to fail")
        return network

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
        self.services["vpc"]["displaytext"] = self.services["vpc"]["name"]
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
    def test_01_create_user_shared_network_without_vlan(self):
        """ Create user-shared networks without vlan """
        self.services["network2"]["networkoffering"] = self.network_offering_novlan.id
        self.services["network2"]["vlan"] = None

        network1 = self.create_shared_network_for_account(self.apiclient, None, None)
        network2 = self.create_shared_network_for_account(self.domainapiclient, None, None)
        network3 = self.create_shared_network_for_account(self.normaluser_apiclient, None, None)

        self.delete_network(network1, self.apiclient)
        self.delete_network(network2, self.domainapiclient)
        self.delete_network(network3, self.normaluser_apiclient)

        network4 = self.create_shared_network_for_account(self.domainapiclient, self.sub_domain, self.normal_user, True)
        self.delete_network(network4, self.normaluser_apiclient)

        network5 = self.create_shared_network_for_account(self.apiclient, self.sub_domain, None, True)
        self.delete_network(network5, self.apiclient)
        network6 = self.create_shared_network_for_account(self.domainapiclient, self.sub_domain, None, True)
        self.delete_network(network6, self.apiclient)
        self.create_shared_network_for_account(self.normaluser_apiclient, self.sub_domain, None, False)

    @attr(tags=["advanced"], required_hardware="false")
    def test_02_create_shared_network_with_vlan(self):
        """ Create shared networks with vlan
            Only be created/deleted by root admin, Cannot create networks with same vlan
        """
        self.services["network2"]["networkoffering"] = self.network_offering_withvlan.id
        self.services["network2"]["vlan"] = 4000

        self.create_shared_network_for_account(self.domainapiclient, self.sub_domain, None, False)
        self.create_shared_network_for_account(self.normaluser_apiclient, self.sub_domain, None, False)
        network1 = self.create_shared_network_for_account(self.apiclient, self.sub_domain, None, True)
        self.create_shared_network_for_account(self.apiclient, self.sub_domain, self.normal_user, False)
        self.delete_network(network1, self.domainapiclient, False)
        self.delete_network(network1, self.apiclient, True)

        self.create_shared_network_for_account(self.domainapiclient, self.sub_domain, self.normal_user, False)
        self.create_shared_network_for_account(self.normaluser_apiclient, self.sub_domain, self.normal_user, False)
        network2 = self.create_shared_network_for_account(self.apiclient, self.sub_domain, self.normal_user, True)
        self.delete_network(network2, self.domainapiclient, False)
        self.delete_network(network2, self.normaluser_apiclient, False)
        self.delete_network(network2, self.apiclient, True)

    @attr(tags=["advanced"], required_hardware="false")
    def test_03_create_domain_shared_network_with_associated_network(self):
        """ Create domain-level shared networks with associated network """

        self.services["network2"]["networkoffering"] = self.network_offering_novlan.id
        self.services["network2"]["vlan"] = None

        # Create isolated networks
        isolated_network1 = self.create_isolated_network_for_account(self.apiclient, None, None, None, True)
        isolated_network2 = self.create_isolated_network_for_account(self.apiclient, self.sub_domain, self.domain_admin, None, True)
        isolated_network3 = self.create_isolated_network_for_account(self.apiclient, self.sub_domain, self.normal_user, None, True)
        isolated_network4 = self.create_isolated_network_for_account(self.apiclient, self.sub_domain, None, self.project, True)

        # Create domain-level shared network with associated_network (caller must be root admin/domain admin, must be in same domain)
        self.create_shared_network_with_associated_network_for_domain(self.apiclient, self.sub_domain, isolated_network1, False)
        self.create_shared_network_with_associated_network_for_domain(self.domainapiclient, self.sub_domain, isolated_network1, False)
        self.create_shared_network_with_associated_network_for_domain(self.normaluser_apiclient, self.sub_domain, isolated_network1, False)
        self.create_shared_network_with_associated_network_for_domain(self.normaluser_apiclient, self.sub_domain, isolated_network2, False)
        self.create_shared_network_with_associated_network_for_domain(self.normaluser_apiclient, self.sub_domain, isolated_network3, False)
        self.create_shared_network_with_associated_network_for_domain(self.normaluser_apiclient, self.sub_domain, isolated_network4, False)
        shared_network1 = self.create_shared_network_with_associated_network_for_domain(self.apiclient, self.domain, isolated_network1, True)
        shared_network2 = self.create_shared_network_with_associated_network_for_domain(self.domainapiclient, self.sub_domain, isolated_network2, True)
        shared_network3 = self.create_shared_network_with_associated_network_for_domain(self.domainapiclient, self.sub_domain, isolated_network3, True)
        shared_network4 = self.create_shared_network_with_associated_network_for_domain(self.domainapiclient, self.sub_domain, isolated_network4, True)

        # Check state of isolated networks (should be Implemented)
        self.check_network_state(self.apiclient, isolated_network1, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.domainapiclient, isolated_network2, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.normaluser_apiclient, isolated_network3, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.domainapiclient, isolated_network4, self.project, NETWORK_STATE_IMPLEMENTED)

        # Delete isolated networks (should fail)
        self.delete_network(isolated_network1, self.apiclient, False)
        self.delete_network(isolated_network2, self.apiclient, False)
        self.delete_network(isolated_network3, self.apiclient, False)
        self.delete_network(isolated_network4, self.apiclient, False)

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

        # Create domain-level shared network with associated vpc tier (caller must be root admin/domain admin, must be in same domain)
        self.create_shared_network_with_associated_network_for_domain(self.apiclient, self.sub_domain, vpc1_tier1, False)
        self.create_shared_network_with_associated_network_for_domain(self.domainapiclient, self.sub_domain, vpc1_tier1, False)
        self.create_shared_network_with_associated_network_for_domain(self.normaluser_apiclient, self.sub_domain, vpc1_tier1, False)
        self.create_shared_network_with_associated_network_for_domain(self.normaluser_apiclient, self.sub_domain, vpc2_tier1, False)
        self.create_shared_network_with_associated_network_for_domain(self.normaluser_apiclient, self.sub_domain, vpc3_tier1, False)
        self.create_shared_network_with_associated_network_for_domain(self.normaluser_apiclient, self.sub_domain, vpc4_tier1, False)
        shared_network_vpctier1 = self.create_shared_network_with_associated_network_for_domain(self.apiclient, self.domain, vpc1_tier1, True)
        shared_network_vpctier2 = self.create_shared_network_with_associated_network_for_domain(self.domainapiclient, self.sub_domain, vpc2_tier1, True)
        shared_network_vpctier3 = self.create_shared_network_with_associated_network_for_domain(self.domainapiclient, self.sub_domain, vpc3_tier1, True)
        shared_network_vpctier4 = self.create_shared_network_with_associated_network_for_domain(self.domainapiclient, self.sub_domain, vpc4_tier1, True)

        # Check state of vpc tiers (should be Implemented)
        self.check_network_state(self.apiclient, vpc1_tier1, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.domainapiclient, vpc2_tier1, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.normaluser_apiclient, vpc3_tier1, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.domainapiclient, vpc4_tier1, self.project, NETWORK_STATE_IMPLEMENTED)

        # Delete vpc tiers(should fail)
        self.delete_network(vpc1_tier1, self.apiclient, False)
        self.delete_network(vpc2_tier1, self.apiclient, False)
        self.delete_network(vpc3_tier1, self.apiclient, False)
        self.delete_network(vpc4_tier1, self.apiclient, False)

        # Delete shared networks associated to domain admin's isolated network or vpc tier(should succeed)
        self.delete_network(shared_network2, self.domainapiclient, True)
        self.delete_network(shared_network_vpctier2, self.domainapiclient, True)

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

        # Check state of isolated networks (1 should be Allocated, 3 should still be Implemented)
        self.check_network_state(self.apiclient, isolated_network1, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.domainapiclient, isolated_network2, None, NETWORK_STATE_ALLOCATED)
        self.check_network_state(self.normaluser_apiclient, isolated_network3, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.domainapiclient, isolated_network4, self.project, NETWORK_STATE_IMPLEMENTED)

        self.check_network_state(self.apiclient, vpc1_tier1, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.domainapiclient, vpc2_tier1, None, NETWORK_STATE_ALLOCATED)
        self.check_network_state(self.normaluser_apiclient, vpc3_tier1, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.domainapiclient, vpc4_tier1, self.project, NETWORK_STATE_IMPLEMENTED)

        # Check state of shared network (should be Setup)
        self.check_network_state(self.apiclient, shared_network1, None, NETWORK_STATE_SETUP)
        self.check_network_state(self.normaluser_apiclient, shared_network3, None, NETWORK_STATE_SETUP)
        self.check_network_state(self.domainapiclient, shared_network4, None, NETWORK_STATE_SETUP)

        self.check_network_state(self.apiclient, shared_network_vpctier1, None, NETWORK_STATE_SETUP)
        self.check_network_state(self.normaluser_apiclient, shared_network_vpctier3, None, NETWORK_STATE_SETUP)
        self.check_network_state(self.domainapiclient, shared_network_vpctier4, None, NETWORK_STATE_SETUP)

        # Delete admin's shared networks (should succeed)
        self.delete_network(shared_network1, self.apiclient, True)
        self.delete_network(shared_network3, self.domainapiclient, True)
        self.delete_network(shared_network4, self.domainapiclient, True)

        self.delete_network(shared_network_vpctier1, self.apiclient, True)
        self.delete_network(shared_network_vpctier3, self.domainapiclient, True)
        self.delete_network(shared_network_vpctier4, self.domainapiclient, True)

        # Delete admin's and domain admin's isolated network, but keep the normal user's network
        # normal user's shared network and isolated network should be removed in tearDown successfully
        self.delete_network(isolated_network1, self.apiclient, True)
        self.delete_network(isolated_network2, self.domainapiclient, True)

        self.delete_network(vpc1_tier1, self.apiclient, True)
        self.delete_vpc(self.apiclient, vpc1)

    @attr(tags=["advanced"], required_hardware="false")
    def test_04_create_account_shared_network_with_associated_network(self):
        """ Create account-level shared networks with associated network """

        self.services["network2"]["networkoffering"] = self.network_offering_novlan.id
        self.services["network2"]["vlan"] = None

        # Create isolated networks
        isolated_network1 = self.create_isolated_network_for_account(self.apiclient, None, None, None, True)
        isolated_network2 = self.create_isolated_network_for_account(self.apiclient, self.sub_domain, self.domain_admin, None, True)
        isolated_network3 = self.create_isolated_network_for_account(self.apiclient, self.sub_domain, self.normal_user, None, True)
        isolated_network4 = self.create_isolated_network_for_account(self.apiclient, self.sub_domain, None, self.project, True)

        # Check state of isolated networks (should be Allocated)
        self.check_network_state(self.apiclient, isolated_network1, None, NETWORK_STATE_ALLOCATED)
        self.check_network_state(self.domainapiclient, isolated_network2, None, NETWORK_STATE_ALLOCATED)
        self.check_network_state(self.normaluser_apiclient, isolated_network3, None, NETWORK_STATE_ALLOCATED)
        self.check_network_state(self.domainapiclient, isolated_network4, self.project, NETWORK_STATE_ALLOCATED)

        # Create shared networks with associated network (must be same owner)
        self.create_shared_network_with_associated_network_for_caller(self.domainapiclient, None, isolated_network1, False)
        self.create_shared_network_with_associated_network_for_caller(self.domainapiclient, None, isolated_network3, False)
        self.create_shared_network_with_associated_network_for_caller(self.domainapiclient, None, isolated_network4, False)
        self.create_shared_network_with_associated_network_for_caller(self.normaluser_apiclient, None, isolated_network1, False)
        self.create_shared_network_with_associated_network_for_caller(self.normaluser_apiclient, None, isolated_network2, False)
        self.create_shared_network_with_associated_network_for_caller(self.normaluser_apiclient, None, isolated_network4, False)
        shared_network1 = self.create_shared_network_with_associated_network_for_caller(self.apiclient, None, isolated_network1, True)
        shared_network2 = self.create_shared_network_with_associated_network_for_caller(self.domainapiclient, None, isolated_network2, True)
        shared_network3 = self.create_shared_network_with_associated_network_for_caller(self.normaluser_apiclient, None, isolated_network3, True)
        shared_network4 = self.create_shared_network_with_associated_network_for_caller(self.domainapiclient, self.project, isolated_network4, True)

        # Check state of isolated networks (should be Implemented)
        self.check_network_state(self.apiclient, isolated_network1, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.domainapiclient, isolated_network2, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.normaluser_apiclient, isolated_network3, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.domainapiclient, isolated_network4, self.project, NETWORK_STATE_IMPLEMENTED)

        # Delete isolated networks (should fail)
        self.delete_network(isolated_network1, self.apiclient, False)
        self.delete_network(isolated_network2, self.apiclient, False)
        self.delete_network(isolated_network3, self.apiclient, False)
        self.delete_network(isolated_network4, self.apiclient, False)

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

        # Create account-level shared network with associated vpc tier (caller must be root admin/domain admin, must be in same domain)
        self.create_shared_network_with_associated_network_for_caller(self.domainapiclient, None, vpc1_tier1, False)
        self.create_shared_network_with_associated_network_for_caller(self.domainapiclient, None, vpc3_tier1, False)
        self.create_shared_network_with_associated_network_for_caller(self.domainapiclient, None, vpc4_tier1, False)
        self.create_shared_network_with_associated_network_for_caller(self.normaluser_apiclient, None, vpc1_tier1, False)
        self.create_shared_network_with_associated_network_for_caller(self.normaluser_apiclient, None, vpc2_tier1, False)
        self.create_shared_network_with_associated_network_for_caller(self.normaluser_apiclient, None, vpc4_tier1, False)
        shared_network_vpctier1 = self.create_shared_network_with_associated_network_for_caller(self.apiclient, None, vpc1_tier1, True)
        shared_network_vpctier2 = self.create_shared_network_with_associated_network_for_caller(self.domainapiclient, None, vpc2_tier1, True)
        shared_network_vpctier3 = self.create_shared_network_with_associated_network_for_caller(self.normaluser_apiclient, None, vpc3_tier1, True)
        shared_network_vpctier4 = self.create_shared_network_with_associated_network_for_caller(self.domainapiclient, self.project, vpc4_tier1, True)

        # Check state of vpc tiers (should be Implemented)
        self.check_network_state(self.apiclient, vpc1_tier1, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.domainapiclient, vpc2_tier1, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.normaluser_apiclient, vpc3_tier1, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.domainapiclient, vpc4_tier1, self.project, NETWORK_STATE_IMPLEMENTED)

        # Delete vpc tiers(should fail)
        self.delete_network(vpc1_tier1, self.apiclient, False)
        self.delete_network(vpc2_tier1, self.apiclient, False)
        self.delete_network(vpc3_tier1, self.apiclient, False)
        self.delete_network(vpc4_tier1, self.apiclient, False)

        # Delete shared networks associated to domain admin's isolated network or vpc tier(should succeed)
        self.delete_network(shared_network2, self.domainapiclient, True)
        self.delete_network(shared_network_vpctier2, self.domainapiclient, True)

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

        # Check state of isolated networks (1 should be Allocated, 3 should still be Implemented)
        self.check_network_state(self.apiclient, isolated_network1, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.domainapiclient, isolated_network2, None, NETWORK_STATE_ALLOCATED)
        self.check_network_state(self.normaluser_apiclient, isolated_network3, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.domainapiclient, isolated_network4, self.project, NETWORK_STATE_IMPLEMENTED)

        self.check_network_state(self.apiclient, vpc1_tier1, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.domainapiclient, vpc2_tier1, None, NETWORK_STATE_ALLOCATED)
        self.check_network_state(self.normaluser_apiclient, vpc3_tier1, None, NETWORK_STATE_IMPLEMENTED)
        self.check_network_state(self.domainapiclient, vpc4_tier1, self.project, NETWORK_STATE_IMPLEMENTED)

        # Check state of shared network (should be Setup)
        self.check_network_state(self.apiclient, shared_network1, None, NETWORK_STATE_SETUP)
        self.check_network_state(self.normaluser_apiclient, shared_network3, None, NETWORK_STATE_SETUP)
        self.check_network_state(self.domainapiclient, shared_network4, self.project, NETWORK_STATE_SETUP)

        self.check_network_state(self.apiclient, shared_network_vpctier1, None, NETWORK_STATE_SETUP)
        self.check_network_state(self.normaluser_apiclient, shared_network_vpctier3, None, NETWORK_STATE_SETUP)
        self.check_network_state(self.domainapiclient, shared_network_vpctier4, self.project, NETWORK_STATE_SETUP)

        # Delete admin's shared networks (should succeed)
        self.delete_network(shared_network1, self.apiclient, True)
        self.delete_network(shared_network_vpctier1, self.apiclient, True)

        # Delete admin's and domain admin's isolated network, but keep the normal user's network
        # normal user's shared network and isolated network should be removed in tearDown successfully
        self.delete_network(isolated_network1, self.apiclient, True)
        self.delete_network(isolated_network2, self.domainapiclient, True)

        self.delete_network(vpc1_tier1, self.apiclient, True)
        self.delete_vpc(self.apiclient, vpc1)
        self.delete_network(vpc2_tier1, self.domainapiclient, True)
        self.delete_vpc(self.domainapiclient, vpc2)
