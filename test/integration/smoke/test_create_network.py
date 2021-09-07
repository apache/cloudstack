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

# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.sshClient import SshClient
from marvin.lib.utils import (cleanup_resources,
                              random_gen)
from marvin.lib.base import (Account,
                             Configurations,
                             Domain,
                             Network,
                             NetworkOffering,
                             PhysicalNetwork,
                             ServiceOffering,
                             Zone)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_free_vlan)
import logging
import random

class TestNetworkManagement(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestNetworkManagement,
            cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.dbclient = cls.testClient.getDbConnection()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        cls.services = cls.testClient.getParsedTestDataConfig()
        zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.zone = Zone(zone.__dict__)
        cls._cleanup = []

        cls.logger = logging.getLogger("TestNetworkManagement")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        testClient = super(TestNetworkManagement, cls).getClsTestClient()
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        # Create new domain, account, network and VM
        cls.user_domain = Domain.create(
            cls.apiclient,
            services=cls.testdata["acl"]["domain2"],
            parentdomainid=cls.domain.id)

        # Create account
        cls.account = Account.create(
            cls.apiclient,
            cls.testdata["acl"]["accountD2"],
            admin=True,
            domainid=cls.user_domain.id
        )

        # Create small service offering
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.testdata["service_offerings"]["small"]
        )

        cls._cleanup.append(cls.service_offering)
        cls._cleanup.append(cls.account)
        cls._cleanup.append(cls.user_domain)

    @classmethod
    def tearDownClass(self):
        try:
            cleanup_resources(self.apiclient, self._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["adeancedsg"], required_hardware="false")
    def test_01_create_network_without_start_end_ip(self):
        """Create Shared network without start and end ip

            Steps:
            # 1. Update the global setting allow.empty.start.end.ipaddress to true
            # 2. Create a shared network without specifying start or end ip
            # 3. This should create the network
            # 4. Now Update the global setting allow.empty.start.end.ipaddress to false
            # 5. Create a shared network without specifying start or end ip
            # 6. Exception should be thrown since start and end ip are not specified
        :return:
        """
        # Create network offering
        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.testdata["network_offering_shared"]
        )

        NetworkOffering.update(
            self.network_offering,
            self.apiclient,
            id=self.network_offering.id,
            state="enabled"
        )

        physical_network, vlan = get_free_vlan(self.apiclient, self.zone.id)
        self.testdata["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        random_subnet_number = random.randrange(100, 199)
        self.testdata["shared_network_sg"]["specifyVlan"] = 'True'
        self.testdata["shared_network_sg"]["specifyIpRanges"] = 'True'
        self.testdata["shared_network_sg"]["name"] = "Shared-Network-SG-Test-vlan" + str(random_subnet_number)
        self.testdata["shared_network_sg"]["displaytext"] = "Shared-Network-SG-Test-vlan" + str(random_subnet_number)
        self.testdata["shared_network_sg"]["vlan"] = "vlan://" + str(random_subnet_number)
        self.testdata["shared_network_sg"]["startip"] = None
        self.testdata["shared_network_sg"]["endip"] = None
        self.testdata["shared_network_sg"]["gateway"] = "192.168." + str(random_subnet_number) + ".254"
        self.testdata["shared_network_sg"]["netmask"] = "255.255.255.0"
        self.testdata["shared_network_sg"]["acltype"] = "account"

        # Update the global setting to true
        Configurations.update(self.apiclient,
            name="allow.empty.start.end.ipaddress",
            value="true"
        )

        # Create network
        network = Network.create(
                self.apiclient,
                self.testdata["shared_network_sg"],
                networkofferingid=self.network_offering.id,
                zoneid=self.zone.id,
                accountid=self.account.name,
                domainid=self.account.domainid
        )

        self.logger.info("network id is %s" % network.id)
        self.cleanup.append(network)

        # Update the global setting to false
        Configurations.update(self.apiclient,
            name="allow.empty.start.end.ipaddress",
            value="false"
        )

        # Exception should be thrown
        with self.assertRaises(Exception):
            self.testdata["shared_network_sg"]["vlan"] = "vlan://" + str(random_subnet_number)
            network2 = Network.create(
                self.apiclient,
                self.testdata["shared_network_sg"],
                networkofferingid=self.network_offering.id,
                zoneid=self.zone.id,
                accountid=self.account.name,
                domainid=self.account.domainid
            )

        # Restore the setting to default value which is true
        Configurations.update(self.apiclient,
            name="allow.empty.start.end.ipaddress",
            value="true"
        )

        self.cleanup.append(self.network_offering)

    @attr(tags=["adeancedsg"], required_hardware="false")
    def test_02_create_network_with_same_name(self):
        """Create Shared network with same name in same account

            Steps:
            # 1. Update the global setting allow.duplicate.networkname to true
            # 2. Create a shared network in an account
            # 3. Try to create another shared network with same name in the same account
            # 4. No exception should be thrown as multiple networks with same name can be created
            # 5. Now update the global setting allow.duplicate.networkname to false
            # 6. Try to create another shared network with same name in the same account
            # 7. Exception should be thrown as network with same name cant be created in the same account
        :return:
        """
        # Update the global setting to true
        Configurations.update(self.apiclient,
            name="allow.duplicate.networkname",
            value="true"
        )

        # Create network offering
        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.testdata["network_offering_shared"]
        )

        NetworkOffering.update(
            self.network_offering,
            self.apiclient,
            id=self.network_offering.id,
            state="enabled"
        )

        physical_network, vlan = get_free_vlan(self.apiclient, self.zone.id)
        self.testdata["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        random_subnet_number = random.randrange(100, 199)
        self.testdata["shared_network_sg"]["specifyVlan"] = 'True'
        self.testdata["shared_network_sg"]["specifyIpRanges"] = 'True'
        self.testdata["shared_network_sg"]["name"] = "Shared-Network-SG-Test-vlan-1"
        self.testdata["shared_network_sg"]["displaytext"] = "Shared-Network-SG-Test-vlan-1"
        self.testdata["shared_network_sg"]["vlan"] = "vlan://" + str(random_subnet_number)
        self.testdata["shared_network_sg"]["startip"] = "192.168." + str(random_subnet_number) + ".1"
        self.testdata["shared_network_sg"]["endip"] = "192.168." + str(random_subnet_number) + ".10"
        self.testdata["shared_network_sg"]["gateway"] = "192.168." + str(random_subnet_number) + ".254"
        self.testdata["shared_network_sg"]["netmask"] = "255.255.255.0"
        self.testdata["shared_network_sg"]["acltype"] = "account"

        # Create the first network
        network3 = Network.create(
            self.apiclient,
            self.testdata["shared_network_sg"],
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(network3)

        # Create the second network with same name. No exception should be thrown
        random_subnet_number = random.randrange(100, 199)
        self.testdata["shared_network_sg"]["vlan"] = "vlan://" + str(random_subnet_number)
        network4 = Network.create(
            self.apiclient,
            self.testdata["shared_network_sg"],
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid
        )

        self.cleanup.append(network4)

        # Update the global setting to true
        Configurations.update(self.apiclient,
            name="allow.duplicate.networkname",
            value="false"
        )

        # Exception should be thrown while creating another network with same name
        with self.assertRaises(Exception):
            random_subnet_number = random.randrange(100, 199)
            self.testdata["shared_network_sg"]["vlan"] = "vlan://" + str(random_subnet_number)
            network5 = Network.create(
                self.apiclient,
                self.testdata["shared_network_sg"],
                networkofferingid=self.network_offering.id,
                zoneid=self.zone.id,
                accountid=self.account.name,
                domainid=self.account.domainid
            )

        # Update the global setting to original value
        Configurations.update(self.apiclient,
            name="allow.duplicate.networkname",
            value="true"
        )

        self.cleanup.append(self.network_offering)

