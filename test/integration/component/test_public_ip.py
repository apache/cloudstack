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
""" Tests for acquiring/listing public ip address
"""
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Account,
                             Configurations,
                             Domain,
                             PublicIpRange,
                             PublicIPAddress,
                             Network,
                             NetworkOffering,
                             ServiceOffering,
                             VPC,
                             VpcOffering,
                             Zone)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               get_free_vlan
                              )
import logging
import random

class TestPublicIp(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestPublicIp, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.logger = logging.getLogger("TestPublicIp")
        cls.domain = get_domain(cls.apiclient)
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"]
        )

        cls.use_system_ips_config_name = "use.system.public.ips"
        cls.use_system_ips_config = Configurations.list(
            cls.apiclient,
            name=cls.use_system_ips_config_name
        )
        cls.use_system_ips_config_value = cls.use_system_ips_config[0].value
        Configurations.update(
            cls.apiclient,
            name=cls.use_system_ips_config_name,
            value="false"
        )

        cls._cleanup = []
        cls.unsupportedHypervisor = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['lxc']:
            cls.unsupportedHypervisor = True
            return

        # Create new domain1
        cls.domain1 = Domain.create(
            cls.apiclient,
            services=cls.services["acl"]["domain1"],
            parentdomainid=cls.domain.id)

        # Create account1
        cls.account1 = Account.create(
            cls.apiclient,
            cls.services["account"],
            # cls.services["acl"]["accountD1"],
            admin=True,
            domainid=cls.domain1.id
        )

        # Create new sub-domain
        cls.sub_domain = Domain.create(
            cls.apiclient,
            services=cls.services["acl"]["domain11"],
            parentdomainid=cls.domain1.id)

        # Create account for sub-domain
        cls.sub_account = Account.create(
            cls.apiclient,
            cls.services["acl"]["accountD11"],
            domainid=cls.sub_domain.id
        )

        # Create new domain2
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
        cls.services["publiciprange"]["gateway"] = "10.100." + \
                                                   str(random_subnet_number) + ".254"
        cls.services["publiciprange"]["startip"] = "10.100." + \
                                                   str(random_subnet_number) + ".1"
        cls.services["publiciprange"]["endip"] = "10.100." + \
                                                 str(random_subnet_number) + ".10"
        cls.services["publiciprange"]["netmask"] = "255.255.255.0"
        cls.public_ip_range1 = PublicIpRange.create(
            cls.apiclient,
            cls.services["publiciprange"],
            account=cls.account1.name,
            domainid=cls.account1.domainid
        )

        # dedicate ip range to sub domain
        cls.services["publiciprange"]["vlan"] = get_free_vlan(
            cls.apiclient,
            cls.zone.id)[1]
        random_subnet_number = random.randrange(10,20)
        cls.services["publiciprange"]["gateway"] = "10.110." + \
                                                    str(random_subnet_number) + ".254"
        cls.services["publiciprange"]["startip"] = "10.110." + \
                                                    str(random_subnet_number) + ".1"
        cls.services["publiciprange"]["endip"] = "10.110." + \
                                                  str(random_subnet_number) + ".10"
        cls.services["publiciprange"]["netmask"] = "255.255.255.0"
        cls.public_ip_range2 = PublicIpRange.create(
            cls.apiclient,
            cls.services["publiciprange"],
            account=cls.sub_account.name,
            domainid=cls.sub_account.domainid
        )

        # dedicate ip range to second domain
        cls.services["publiciprange"]["vlan"] = get_free_vlan(
            cls.apiclient,
            cls.zone.id)[1]
        random_subnet_number = random.randrange(10,20)
        cls.services["publiciprange"]["gateway"] = "10.120." + \
                                                   str(random_subnet_number) + ".254"
        cls.services["publiciprange"]["startip"] = "10.120." + \
                                                   str(random_subnet_number) + ".1"
        cls.services["publiciprange"]["endip"] = "10.120." + \
                                                 str(random_subnet_number) + ".10"
        cls.services["publiciprange"]["netmask"] = "255.255.255.0"
        cls.public_ip_range3 = PublicIpRange.create(
            cls.apiclient,
            cls.services["publiciprange"],
            account=cls.account2.name,
            domainid=cls.account2.domainid
        )

        # create vpc offering and VPC
        cls.vpc_off = VpcOffering.create(
            cls.apiclient,
            cls.services["vpc_offering"]
        )
        cls.vpc_off.update(cls.apiclient, state='Enabled')

        # create network offering
        cls.isolated_network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.services["isolated_network_offering"],
            conservemode=False
        )

        NetworkOffering.update(
            cls.isolated_network_offering,
            cls.apiclient,
            id=cls.isolated_network_offering.id,
            state="enabled"
        )

        physical_network, shared_vlan = get_free_vlan(
            cls.apiclient, cls.zone.id)
        if shared_vlan is None:
            cls.fail("Failed to get free vlan id for shared network")

        cls.services["shared_network_offering"]["specifyVlan"] = "True"
        cls.services["shared_network_offering"]["specifyIpRanges"] = "True"

        cls.shared_network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.services["shared_network_offering"],
            conservemode=False
        )

        NetworkOffering.update(
            cls.shared_network_offering,
            cls.apiclient,
            id=cls.shared_network_offering.id,
            state="enabled"
        )

        # create network using the shared network offering created
        cls.services["shared_network"]["acltype"] = "Domain"
        cls.services["shared_network"][
            "networkofferingid"] = cls.shared_network_offering.id
        cls.services["shared_network"][
            "physicalnetworkid"] = physical_network.id
        cls.services["shared_network"]["vlan"] = shared_vlan
        shared_network_subnet_number = random.randrange(1, 254)

        cls.services["shared_network"]["netmask"] = "255.255.255.0"
        cls.services["shared_network"]["gateway"] = "172.16." + \
                                                     str(shared_network_subnet_number) + ".254"
        cls.services["shared_network"]["startip"] = "172.16." + \
                                                     str(shared_network_subnet_number) + ".1"
        cls.services["shared_network"]["endip"] = "172.16." + \
                                                   str(shared_network_subnet_number) + ".10"

        cls.guest_network = Network.create(
            cls.apiclient,
            cls.services["shared_network"],
            networkofferingid=cls.shared_network_offering.id,
            zoneid=cls.zone.id
        )

        cls._cleanup.append(cls.guest_network)
        cls._cleanup.append(cls.shared_network_offering)
        cls._cleanup.append(cls.account1)
        cls._cleanup.append(cls.account2)
        cls._cleanup.append(cls.sub_account)
        cls._cleanup.append(cls.sub_domain)
        cls._cleanup.append(cls.domain1)
        cls._cleanup.append(cls.domain2)
        cls._cleanup.append(cls.public_ip_range1)
        cls._cleanup.append(cls.public_ip_range2)
        cls._cleanup.append(cls.public_ip_range3)

    @classmethod
    def tearDownClass(cls):
        try:
            cls.apiclient = super(
                TestPublicIp,
                cls).getClsTestClient().getApiClient()

            Configurations.update(
                cls.apiclient,
                name=cls.use_system_ips_config_name,
                value=cls.use_system_ips_config_value
            )
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_01_list_publicip_root_domain(self):
        """
        ROOT domain should be able to list public IP address of all
        sub domains

        Step 1: List all public ip address
        Step 2: Ensure that the count is greater than or equal to 30
        Step 3: Display only allocated ip address
        Step 4: Ensure that the count is greater than or equal to 0
        Step 5: Display ip address from shared networks
        Step 6: Ensure that the count is greater than or equal to 30
        :return:
        """
        # Step 1
        ipAddresses = PublicIPAddress.list(
            self.apiclient,
            allocatedonly=False,
            isrecursive=True,
            listall=True,
            forvirtualnetwork=True)

        # Step 2
        self.assertGreaterEqual(
            len(ipAddresses),
            30,
            "Unable to display all public ip for ROOT domain"
        )

        # Step 4
        ipAddresses = PublicIPAddress.list(
            self.apiclient,
            allocatedonly=True,
            isrecursive=True,
            listall=True,
            forvirtualnetwork=True)

        self.assertGreaterEqual(
            len(ipAddresses),
            0,
            "Unable to display all public ip for ROOT domain"
        )

        # Step 5
        ipAddresses = PublicIPAddress.list(
            self.apiclient,
            allocatedonly=False,
            isrecursive=True,
            listall=True,
            forvirtualnetwork=False)

        # Step 6
        self.assertGreaterEqual(
            len(ipAddresses),
            10,
            "Unable to display all public ip for ROOT domain"
        )

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_02_list_publicip_domain_admin(self):
        """
        A domain admin should be able to list public IP address of
        his/her domain and all sub domains

        Step 1: Create an isolated network in the user domain and sub domain
        Step 2: Associate IP in the range, dedicated to domain1
        Step 3: Display all public ip address in domain1
        Step 4: Ensure that the count is greater than or equal to 10
        Step 5: Display only the allocated Ip address in domain1
        Step 6: Ensure that the count is 1
        Step 7: Try to display public ip address from shared networks
        Step 8: It should not return any result
        Step 9: Try to display ip address of domain1 from domain2
        Step 10: Ensure that it doesnt not return any result
        :return:
        """
        # Step 1. Create isolated network
        self.isolated_network1 = Network.create(
            self.apiclient,
            self.services["isolated_network"],
            accountid=self.account1.name,
            domainid=self.account1.domainid,
            networkofferingid=self.isolated_network_offering.id,
            zoneid=self.zone.id
        )

        # Step 2. Associate IP in range dedicated to domain1
        ip_address_1 = self.get_free_ipaddress(self.public_ip_range1.vlan.id)
        ipaddress = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            networkid=self.isolated_network1.id,
            ipaddress=ip_address_1
        )
        self.assertIsNotNone(
            ipaddress,
            "Failed to Associate IP Address"
        )

        # Step 3: Display all public ip address in domain1
        user = self.account1.user[0]
        user_api_client = self.testClient.getUserApiClient(user.username, self.domain1.name)

        ipAddresses = PublicIPAddress.list(
            user_api_client,
            allocatedonly=False,
            isrecursive=True,
            listall=True,
            forvirtualnetwork=True)

        # Step 4: Ensure that the count is equal to 10
        self.assertEqual(
            len(ipAddresses),
            10,
            "Failed to display all public ip address is domain %s" % self.domain1.name
        )

        # Display public ip address using network id
        ipAddresses = PublicIPAddress.list(
            self.apiclient,
            associatednetworkid=self.isolated_network1.id,
            account=self.account1.name,
            domainid=self.account1.domainid,
            allocatedonly=False,
            listall=True,
            forvirtualnetwork=True)

        # Step 4: Ensure that the count is greater than or equal to 10
        self.assertEqual(
            len(ipAddresses),
            10,
            "Failed to display all public ip address using network id"
        )

        # Step 5: Display all allocated public ip address in domain1
        ipAddresses = PublicIPAddress.list(
            user_api_client,
            allocatedonly=True,
            isrecursive=True,
            listall=True,
            forvirtualnetwork=True)

        # Step 6: Ensure that the count is greater than or equal to 1
        self.assertEqual(
            len(ipAddresses),
            1,
            "Allocated IP address is greater than 1"
        )

        # Step 7: Display public ip address from shared networks
        ipAddresses = PublicIPAddress.list(
            user_api_client,
            allocatedonly=True,
            isrecursive=True,
            listall=True,
            forvirtualnetwork=False)

        # Step 8: Ensure that the result is empty
        self.assertIsNone(
            ipAddresses,
            "Users should not display ip from shared networks"
        )

        try:
            # Step 9
            user = self.account2.user[0]
            user_api_client = self.testClient.getUserApiClient(user.username, self.domain2.name)

            ipAddresses = PublicIPAddress.list(
                user_api_client,
                allocatedonly=False,
                listall=True,
                associatednetworkid=self.isolated_network1.id,
                forvirtualnetwork=True)

            # Step 10
            self.fail("Domain should not access public ip of sibling domain")
        except Exception as e:
            self.info("Got exception as expected since domain2 cant access network of domain1")

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_03_list_publicip_user_domain(self):
        """
        A regular user should be able to display public ip address
        only in their domain

        Step 1: Create an isolated network in the user domain
        Step 2: Display all public ip address in that domain
        Step 3: Ensure that the count is 10
        Step 4: Associate IP in the range, dedicated to domain2
        Step 5: Display only the allocated Ip address in domain2
        Step 6: Ensure that the count is 1
        Step 7: Try to display public ip address from shared networks
        Step 8: It should not return any result
        Step 9: Try to display allocated public ip address of child domain using networkid
        Step 10: It should display all allocated ip address from child domain
        Step 11: Try to display all public ip address from child domain
        Step 12: It should display all public ip of child domain
        Step 13: Try to display ip of domain2 from a different domain
        Step 14: Ensure that we get exception when trying to do so
        :return:
        """
        user = self.sub_account.user[0]
        sub_user_api_client = self.testClient.getUserApiClient(user.username, self.sub_domain.name)

        # Step 1: create network in child domain
        self.isolated_network2 = Network.create(
            self.apiclient,
            self.services["isolated_network"],
            accountid=self.sub_account.name,
            domainid=self.sub_account.domainid,
            networkofferingid=self.isolated_network_offering.id,
            zoneid=self.zone.id
        )

        # Step 2: Display all public ip address in sub domain
        ipAddresses = PublicIPAddress.list(
            sub_user_api_client,
            allocatedonly=False,
            listall=True,
            forvirtualnetwork=True)

        # Step 3: Ensure that sub domain can list only the ip address in their domain
        self.assertEqual(
            len(ipAddresses),
            10,
            "Allocated IP address is greater than 1"
        )

        # Step 4: Associate IP in range dedicated to sub domain
        ip_address_1 = self.get_free_ipaddress(self.public_ip_range2.vlan.id)
        ipaddress = PublicIPAddress.create(
            sub_user_api_client,
            zoneid=self.zone.id,
            networkid=self.isolated_network2.id,
            ipaddress=ip_address_1
        )

        # Step 5: Display all allocated ip address in sub domain
        ipAddresses = PublicIPAddress.list(
            sub_user_api_client,
            allocatedonly=True,
            listall=True,
            forvirtualnetwork=True)

        # Step 6: Ensure that the count is 1
        self.assertEqual(
            len(ipAddresses),
            1,
            "Allocated IP address is greater than 1"
        )

        # Step 7: Display ip address from shared networks
        ipAddresses = PublicIPAddress.list(
            sub_user_api_client,
            allocatedonly=True,
            listall=True,
            forvirtualnetwork=False)

        # Step 8: It should not return any result
        self.assertIsNone(
            ipAddresses,
            "Users should not display ip from shared networks"
        )

        user = self.account1.user[0]
        user_api_client = self.testClient.getUserApiClient(user.username, self.domain1.name)

        # Step 9: display ip of child domain using network id
        ipAddresses = PublicIPAddress.list(
            user_api_client,
            allocatedonly=True,
            listall=True,
            isrecursive=True,
            associatednetworkid=self.isolated_network2.id,
            forvirtualnetwork=True)

        # Step 10: Ensure that the count is 1 as only 1 ip is acquired in test 3
        self.assertEqual(
            len(ipAddresses),
            1,
            "Unable to display IP address of child domain using network id"
        )

        # Step 11: display ip of child domain using network id
        ipAddresses = PublicIPAddress.list(
            user_api_client,
            allocatedonly=False,
            listall=True,
            isrecursive=True,
            associatednetworkid=self.isolated_network2.id,
            forvirtualnetwork=True)

        # Step 12: Ensure that the count is 1 as only 1 ip is acquired in test 3
        self.assertEqual(
            len(ipAddresses),
            10,
            "Unable to display IP address of child domain using network id"
        )

        try:
            # Step 13
            user = self.account2.user[0]
            user_api_client = self.testClient.getUserApiClient(user.username, self.domain2.name)

            PublicIPAddress.list(
                user_api_client,
                allocatedonly=False,
                listall=True,
                associatednetworkid=self.isolated_network2.id,
                forvirtualnetwork=True)

            # Step 14
            self.fail("Domain should not access public ip of sibling domain")
        except Exception as e:
            self.info("Got exception as expected since domain2 cant access network of domain1")

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_04_list_publicip_all_subdomains(self):
        """
        A domain admin should be able to display public ip address
        in their domain and also all child domains

        Step 1: Display all public ip address in that domain and sub domain
        Step 2: Ensure that the count is 11 (all ip from parent domain and allocated from sub domain)
        Step 3: Display only the allocated Ip address
        Step 4: Ensure that the count is 2
        Step 5: Display public ip of child domain using domain/account
        Step 6: Ensure that the count is 10
        Step 7: Display public ip of child domain using network id
        Step 8: Ensure that the count is 1 as only one IP is allocated
        :return:
        """
        user = self.account1.user[0]
        user_api_client = self.testClient.getUserApiClient(user.username, self.domain1.name)

        # Step 1: Display all public ip
        ipAddresses = PublicIPAddress.list(
            user_api_client,
            allocatedonly=False,
            listall=True,
            isrecursive=True,
            forvirtualnetwork=True)

        # Step 2: Ensure that it can display all ip from current domain and
        # only allocated ip from child domains
        self.assertEqual(
            len(ipAddresses),
            11,
            "Unable to display all IP address is domain %s" % self.domain1.name
        )

        # Step 3: display only allocated ip for parent and sub domain
        ipAddresses = PublicIPAddress.list(
            user_api_client,
            allocatedonly=True,
            listall=True,
            isrecursive=True,
            forvirtualnetwork=True)

        # Step 4: Ensure that the count is 2
        self.assertEqual(
            len(ipAddresses),
            2,
            "Unable to display all allocated IP address is domain %s" % self.domain1.name
        )

        # Step 5: display ip of child domain using domainid/account
        ipAddresses = PublicIPAddress.list(
            user_api_client,
            allocatedonly=False,
            listall=True,
            isrecursive=True,
            domainid=self.sub_domain.id,
            account=self.sub_account.name,
            forvirtualnetwork=True)

        # Step 6: Ensure that the count is 10
        self.assertEqual(
            len(ipAddresses),
            10,
            "Unable to display IP address of child domain"
        )

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_05_list_publicip_user_domain(self):
        """
        A domain admin should be able to display public ip address
        in their domain and also all child domains

        Step 1: Display all public ip address in that domain and sub domain
        Step 2: Ensure that the count is 20
        Step 3: Display only the allocated Ip address
        Step 4: Ensure that the count is 2
        Step 5: Try to display public ip of vpc from different domain
        Step 6: Ensure that we get exception when trying to do so
        :return:
        """
        user = self.account2.user[0]
        user_api_client = self.testClient.getUserApiClient(user.username, self.domain2.name)

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        vpc_1 = VPC.create(
            user_api_client,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.account2.name,
            domainid=self.account2.domainid
        )
        self.validate_vpc_network(vpc_1)

        ipAddresses = PublicIPAddress.list(
            user_api_client,
            allocatedonly=False,
            listall=True,
            forvirtualnetwork=True)

        self.assertGreaterEqual(
            len(ipAddresses),
            10,
            "Unable to display all public ip in VPC"
        )

        # List ip address using vpcid
        ipAddresses = PublicIPAddress.list(
            self.apiclient,
            vpcid=vpc_1.id,
            allocatedonly=False,
            account=self.account2.name,
            domainid=self.domain2.id,
            forvirtualnetwork=True)

        self.assertGreaterEqual(
            len(ipAddresses),
            10,
            "Unable to display all public ip in VPC"
        )

        # Acquire public ip address from VPC
        ip_address_1 = self.get_free_ipaddress(self.public_ip_range3.vlan.id)
        PublicIPAddress.create(
            user_api_client,
            zoneid=self.zone.id,
            vpcid=vpc_1.id,
            ipaddress=ip_address_1
        )

        ipAddresses = PublicIPAddress.list(
            user_api_client,
            allocatedonly=True,
            listall=True,
            forvirtualnetwork=True)

        self.assertGreaterEqual(
            len(ipAddresses),
            2,
            "Unable to display allocated public ip in VPC"
        )

        try:
            # Step 5
            user = self.account1.user[0]
            user_api_client = self.testClient.getUserApiClient(user.username, self.domain1.name)

            PublicIPAddress.list(
                user_api_client,
                allocatedonly=False,
                listall=True,
                vpcid=vpc_1.id,
                forvirtualnetwork=True)

            # Step 6
            self.fail("Domain should not access public ip of sibling domain")
        except Exception as e:
            self.info("Got exception as expected since domain2 cant access network of domain1")

        self.vpc_off.update(self.apiclient, state='Disabled')
        self.cleanup.append(vpc_1)
        self.cleanup.append(self.vpc_off)

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_06_list_publicip_user_domain(self):
        """
        Display public ip address from shared networks
        1. List public ip address of shared network as root admin
        2. Ensure that it can display all public ip address
        3. List public ip address of shared networks as domain admin
        4. It should not return any result
        5. Try to display public ip by passing network id
        6. Ensure that we get exception when trying to do so
        :return:
        """
        # Step 1
        ipAddresses = PublicIPAddress.list(
            self.apiclient,
            allocatedonly=False,
            listall=True,
            networkid=self.guest_network.id,
            forvirtualnetwork=False)

        # Step 2
        self.assertGreaterEqual(
            len(ipAddresses),
            10,
            "Unable to display allocated public ip in VPC"
        )

        user = self.account1.user[0]
        user_api_client = self.testClient.getUserApiClient(user.username, self.domain1.name)

        # Step 3
        ipAddresses = PublicIPAddress.list(
            user_api_client,
            allocatedonly=False,
            listall=True,
            networkid=self.guest_network.id,
            forvirtualnetwork=False)

        # Step 4
        self.assertIsNone(
            ipAddresses,
            "Unable to display allocated public ip in VPC"
        )

        try:
            # Step 5
            PublicIPAddress.list(
                user_api_client,
                allocatedonly=False,
                listall=True,
                associatednetworkid=self.guest_network.id,
                forvirtualnetwork=True)

            # Step 6
            self.fail("Domain should not access public ip of sibling domain")
        except Exception as e:
            self.info("Got exception as expected since domain2 cant access network of domain1")

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

    def validate_vpc_network(self, network, state=None):
        """Validates the VPC network"""

        self.debug("Check if the VPC network is created successfully?")
        vpc_networks = VPC.list(
            self.apiclient,
            id=network.id
        )
        self.assertEqual(
            isinstance(vpc_networks, list),
            True,
            "List VPC network should return a valid list"
        )
        self.assertEqual(
            network.name,
            vpc_networks[0].name,
            "Name of the VPC network should match with listVPC data"
        )
        if state:
            self.assertEqual(
                vpc_networks[0].state,
                state,
                "VPC state should be '%s'" % state
            )
        self.debug("VPC network validated - %s" % network.name)
        return
