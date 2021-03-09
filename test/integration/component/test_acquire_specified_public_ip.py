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
Tests of acquiring a specified public IP for isolated network or vpc
"""

# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.cloudstackAPI import createVlanIpRange
from marvin.lib.utils import (validateList,
                              cleanup_resources)
from marvin.lib.base import (Account,
                             Domain,
                             Configurations,
                             VirtualMachine,
                             ServiceOffering,
                             VpcOffering,
                             Zone,
                             Network,
                             VPC,
                             PublicIPAddress,
                             PublicIpRange)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_free_vlan,
                               get_template)
import logging
import random

class TestAcquireSpecifiedPublicIp(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestAcquireSpecifiedPublicIp,
            cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.zone = Zone(zone.__dict__)
        cls.template = get_template(cls.apiclient, cls.zone.id)
        cls._cleanup = []

        if str(cls.zone.securitygroupsenabled) == "True":
            sys.exit(1)

        cls.logger = logging.getLogger("TestAcquireSpecifiedPublicIp")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)

        # Create new domain1
        cls.domain1 = Domain.create(
            cls.apiclient,
            services=cls.services["acl"]["domain1"],
            parentdomainid=cls.domain.id)

        # Create account1
        cls.account1 = Account.create(
            cls.apiclient,
            cls.services["acl"]["accountD1"],
            domainid=cls.domain1.id
        )

        # Create domain2
        cls.domain2 = Domain.create(
            cls.apiclient,
            services=cls.services["acl"]["domain2"],
            parentdomainid=cls.domain.id)

        # Create account2
        cls.account2 = Account.create(
            cls.apiclient,
            cls.services["acl"]["accountD2"],
            domainid=cls.domain2.id
        )

        cls.services["publiciprange"]["zoneid"] = cls.zone.id
        cls.services["publiciprange"]["forvirtualnetwork"] = "true"

        # Create public ip range 1
        cls.services["publiciprange"]["vlan"] = get_free_vlan(
            cls.apiclient,
            cls.zone.id)[1]
        random_subnet_number = random.randrange(10,20)
        cls.services["publiciprange"]["gateway"] = "172.16." + \
            str(random_subnet_number) + ".1"
        cls.services["publiciprange"]["startip"] = "172.16." + \
            str(random_subnet_number) + ".2"
        cls.services["publiciprange"]["endip"] = "172.16." + \
            str(random_subnet_number) + ".10"
        cls.services["publiciprange"]["netmask"] = "255.255.255.0"
        cls.public_ip_range1 = PublicIpRange.create(
            cls.apiclient,
            cls.services["publiciprange"]
        )
        PublicIpRange.dedicate(
            cls.apiclient,
            cls.public_ip_range1.vlan.id,
            domainid=cls.account1.domainid
        )

        # Create public ip range 2
        cls.services["publiciprange"]["vlan"] = get_free_vlan(
            cls.apiclient,
            cls.zone.id)[1]
        cls.services["publiciprange"]["gateway"] = "172.16." + \
            str(random_subnet_number + 1) + ".1"
        cls.services["publiciprange"]["startip"] = "172.16." + \
            str(random_subnet_number + 1) + ".2"
        cls.services["publiciprange"]["endip"] = "172.16." + \
            str(random_subnet_number + 1) + ".10"
        cls.services["publiciprange"]["netmask"] = "255.255.255.0"
        cls.public_ip_range2 = PublicIpRange.create(
            cls.apiclient,
            cls.services["publiciprange"]
        )
        PublicIpRange.dedicate(
            cls.apiclient,
            cls.public_ip_range2.vlan.id,
            account=cls.account1.name,
            domainid=cls.account1.domainid
        )

        # Create public ip range 3
        cls.services["publiciprange"]["vlan"] = get_free_vlan(
            cls.apiclient,
            cls.zone.id)[1]
        cls.services["publiciprange"]["gateway"] = "172.16." + \
            str(random_subnet_number + 2) + ".1"
        cls.services["publiciprange"]["startip"] = "172.16." + \
            str(random_subnet_number + 2) + ".2"
        cls.services["publiciprange"]["endip"] = "172.16." + \
            str(random_subnet_number + 2) + ".10"
        cls.services["publiciprange"]["netmask"] = "255.255.255.0"
        cls.public_ip_range3 = PublicIpRange.create(
            cls.apiclient,
            cls.services["publiciprange"]
        )
        PublicIpRange.dedicate(
            cls.apiclient,
            cls.public_ip_range3.vlan.id,
            domainid=cls.account2.domainid
        )

        # Create public ip range 4
        cls.services["publiciprange"]["vlan"] = get_free_vlan(
            cls.apiclient,
            cls.zone.id)[1]
        cls.services["publiciprange"]["gateway"] = "172.16." + \
            str(random_subnet_number + 3) + ".1"
        cls.services["publiciprange"]["startip"] = "172.16." + \
            str(random_subnet_number + 3) + ".2"
        cls.services["publiciprange"]["endip"] = "172.16." + \
            str(random_subnet_number + 3) + ".10"
        cls.services["publiciprange"]["netmask"] = "255.255.255.0"
        cls.public_ip_range4 = PublicIpRange.create(
            cls.apiclient,
            cls.services["publiciprange"]
        )
        PublicIpRange.dedicate(
            cls.apiclient,
            cls.public_ip_range4.vlan.id,
            account=cls.account2.name,
            domainid=cls.account2.domainid
        )

        # Create public ip range 5
        cls.services["publiciprange"]["vlan"] = get_free_vlan(
            cls.apiclient,
            cls.zone.id)[1]
        cls.services["publiciprange"]["gateway"] = "172.16." + \
            str(random_subnet_number + 4) + ".1"
        cls.services["publiciprange"]["startip"] = "172.16." + \
            str(random_subnet_number + 4) + ".2"
        cls.services["publiciprange"]["endip"] = "172.16." + \
            str(random_subnet_number + 4) + ".10"
        cls.services["publiciprange"]["netmask"] = "255.255.255.0"
        cls.public_ip_range5 = PublicIpRange.create(
            cls.apiclient,
            cls.services["publiciprange"]
        )

        cls._cleanup.append(cls.account1)
        cls._cleanup.append(cls.domain1)
        cls._cleanup.append(cls.account2)
        cls._cleanup.append(cls.domain2)
        cls._cleanup.append(cls.public_ip_range1)
        cls._cleanup.append(cls.public_ip_range2)
        cls._cleanup.append(cls.public_ip_range3)
        cls._cleanup.append(cls.public_ip_range4)
        cls._cleanup.append(cls.public_ip_range5)

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(cls):
        cls.apiclient = cls.testClient.getApiClient()
        cls.cleanup = []
        return

    def tearDown(cls):
        try:
            cleanup_resources(cls.apiclient, cls.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_01_acquire_public_ip_in_isolated_network(self):
        # Validate the following
        # 1. create a vm . it will create a network as well.
        # 2. assign a specified IP from subnet which is dedicated to domain1, it should succeed
        # 3. assign a specified IP from subnet which is dedicated to domain1/account1, it should succeed
        # 4. assign a specified IP from subnet which is dedicated to domain2, it should fail
        # 5. assign a specified IP from subnet which is dedicated to domain2/account2, it should fail
        # 6. update account setting use.system.public.ips to false, assign a specified IP from subnet which is public, it should fail
        # 7. update account setting use.system.public.ips to true, assign a specified IP from subnet which is public, it should succeed

        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"]
        )
        self.cleanup.append(self.service_offering)

        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            templateid=self.template.id,
            accountid=self.account1.name,
            domainid=self.account1.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id
        )

        networks = Network.list(
            self.apiclient,
            account=self.account1.name,
            domainid=self.account1.domainid,
            listall=True
        )
        self.assertEqual(
            isinstance(networks, list),
            True,
            "List networks should return a valid response for created network"
             )
        network = networks[0]

        # Associate IP in range dedicated to domain1
        ip_address_1 = self.get_free_ipaddress(self.public_ip_range1.vlan.id)
        ipaddress = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            networkid=network.id,
            ipaddress=ip_address_1
        )
        self.assertIsNotNone(
            ipaddress,
            "Failed to Associate IP Address"
        )
        self.cleanup.append(ipaddress)
        self.assertEqual(ipaddress.ipaddress.ipaddress, ip_address_1, "Associated IP is not same as specified")

        # Associate IP in range dedicated to domain1/account1
        ip_address_2 = self.get_free_ipaddress(self.public_ip_range2.vlan.id)
        ipaddress = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            networkid=network.id,
            ipaddress=ip_address_2
        )
        self.assertIsNotNone(
            ipaddress,
            "Failed to Associate IP Address"
        )
        self.cleanup.append(ipaddress)
        self.assertEqual(ipaddress.ipaddress.ipaddress, ip_address_2, "Associated IP is not same as specified")

        # Associate IP in range dedicated to domain2
        ip_address_3 = self.get_free_ipaddress(self.public_ip_range3.vlan.id)
        with self.assertRaises(Exception):
            ipaddress = PublicIPAddress.create(
                self.apiclient,
                zoneid=self.zone.id,
                networkid=network.id,
                ipaddress=ip_address_3
            )

        # Associate IP in range dedicated to domain2/account2
        ip_address_4 = self.get_free_ipaddress(self.public_ip_range4.vlan.id)
        with self.assertRaises(Exception):
            ipaddress = PublicIPAddress.create(
                self.apiclient,
                zoneid=self.zone.id,
                networkid=network.id,
                ipaddress=ip_address_4
            )

        # Associate IP in public IP pool
        Configurations.update(
            self.apiclient,
            name="use.system.public.ips",
            value="false",
            accountid=self.account1.id
        )

        ip_address_5 = self.get_free_ipaddress(self.public_ip_range5.vlan.id)
        with self.assertRaises(Exception):
            ipaddress = PublicIPAddress.create(
                self.apiclient,
                zoneid=self.zone.id,
                networkid=network.id,
                ipaddress=ip_address_5
            )

        Configurations.update(
            self.apiclient,
            name="use.system.public.ips",
            value="true",
            accountid=self.account1.id
        )
        ipaddress = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            networkid=network.id,
            ipaddress=ip_address_5
        )
        self.assertIsNotNone(
            ipaddress,
            "Failed to Associate IP Address"
        )
        self.cleanup.append(ipaddress)
        self.assertEqual(ipaddress.ipaddress.ipaddress, ip_address_5, "Associated IP is not same as specified")

        self.cleanup.append(self.virtual_machine)
        self.cleanup.append(self.service_offering)

        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_02_acquire_public_ip_in_vpc(self):
        # Validate the following
        # 1. create a VPC with default offering.
        # 2. assign a specified IP from subnet which is dedicated to domain1, it should succeed
        # 3. assign a specified IP from subnet which is dedicated to domain1/account1, it should succeed
        # 4. assign a specified IP from subnet which is dedicated to domain2, it should fail
        # 5. assign a specified IP from subnet which is dedicated to domain2/account2, it should fail
        # 6. update account setting use.system.public.ips to false, assign a specified IP from subnet which is public, it should fail
        # 7. update account setting use.system.public.ips to true, assign a specified IP from subnet which is public, it should succeed

        vpcOffering = VpcOffering.list(self.apiclient, name="Default VPC offering")
        vpc = VPC.create(
            apiclient=self.apiclient,
            services=self.services["vpc"],
            vpcofferingid=vpcOffering[0].id,
            zoneid=self.zone.id,
            account=self.account1.name,
            domainid=self.account1.domainid
        )

        # Associate IP in range dedicated to domain1
        ip_address_1 = self.get_free_ipaddress(self.public_ip_range1.vlan.id)
        ipaddress = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            vpcid=vpc.id,
            ipaddress=ip_address_1
        )
        self.assertIsNotNone(
            ipaddress,
            "Failed to Associate IP Address"
        )
        self.cleanup.append(ipaddress)
        self.assertEqual(ipaddress.ipaddress.ipaddress, ip_address_1, "Associated IP is not same as specified")

        # Associate IP in range dedicated to domain1/account1
        ip_address_2 = self.get_free_ipaddress(self.public_ip_range2.vlan.id)
        ipaddress = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            vpcid=vpc.id,
            ipaddress=ip_address_2
        )
        self.assertIsNotNone(
            ipaddress,
            "Failed to Associate IP Address"
        )
        self.cleanup.append(ipaddress)
        self.assertEqual(ipaddress.ipaddress.ipaddress, ip_address_2, "Associated IP is not same as specified")

        # Associate IP in range dedicated to domain2
        ip_address_3 = self.get_free_ipaddress(self.public_ip_range3.vlan.id)
        with self.assertRaises(Exception):
            ipaddress = PublicIPAddress.create(
                self.apiclient,
                zoneid=self.zone.id,
                vpcid=vpc.id,
                ipaddress=ip_address_3
            )

        # Associate IP in range dedicated to domain2/account2
        ip_address_4 = self.get_free_ipaddress(self.public_ip_range4.vlan.id)
        with self.assertRaises(Exception):
            ipaddress = PublicIPAddress.create(
                self.apiclient,
                zoneid=self.zone.id,
                vpcid=vpc.id,
                ipaddress=ip_address_4
            )

        # Associate IP in public IP pool
        Configurations.update(
            self.apiclient,
            name="use.system.public.ips",
            value="false",
            accountid=self.account1.id
        )

        ip_address_5 = self.get_free_ipaddress(self.public_ip_range5.vlan.id)
        with self.assertRaises(Exception):
            ipaddress = PublicIPAddress.create(
                self.apiclient,
                zoneid=self.zone.id,
                vpcid=vpc.id,
                ipaddress=ip_address_5
            )

        Configurations.update(
            self.apiclient,
            name="use.system.public.ips",
            value="true",
            accountid=self.account1.id
        )
        ipaddress = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            vpcid=vpc.id,
            ipaddress=ip_address_5
        )
        self.assertIsNotNone(
            ipaddress,
            "Failed to Associate IP Address"
        )
        self.cleanup.append(ipaddress)
        self.assertEqual(ipaddress.ipaddress.ipaddress, ip_address_5, "Associated IP is not same as specified")

        self.cleanup.append(vpc)
        return

    def get_free_ipaddress(self, vlanId):
        ipaddresses = PublicIPAddress.list(
            self.apiclient,
            vlanid=vlanId,
            state='Free'
        )
        self.assertEqual(
            isinstance(ipaddresses, list),
            True,
            "List ipaddresses should return a valid response for Free ipaddresses"
             )
        random.shuffle(ipaddresses)
        return ipaddresses[0].ipaddress
