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
import time

from nose.plugins.attrib import attr

from marvin.cloudstackAPI import updateConfiguration
from marvin.cloudstackException import CloudstackAPIException
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import Network, NetworkOffering, VpcOffering, VPC, PublicIPAddress
from marvin.lib.common import get_domain, get_zone


class Services:
    """ Test Quarantine for public IPs
    """

    def __init__(self):
        self.services = {
            "root_domain": {
                "name": "ROOT",
            },
            "domain_admin": {
                "username": "Domain admin",
                "roletype": 2,
            },
            "root_admin": {
                "username": "Root admin",
                "roletype": 1,
            },
            "domain_vpc": {
                "name": "domain-vpc",
                "displaytext": "domain-vpc",
                "cidr": "10.1.1.0/24",
            },
            "domain_network": {
                "name": "domain-network",
                "displaytext": "domain-network",
            },
            "root_vpc": {
                "name": "root-vpc",
                "displaytext": "root-vpc",
                "cidr": "10.2.1.0/24",
            },
            "root_network": {
                "name": "root-network",
                "displaytext": "root-network",
            }
        }


class TestQuarantineIPs(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestQuarantineIPs, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        return

    def setUp(self):
        self.domain_admin_apiclient = self.testClient.getUserApiClient(self.services["domain_admin"]["username"],
                                                                       self.services["root_domain"]["name"],
                                                                       self.services["domain_admin"]["roletype"])

        self.admin_apiclient = self.testClient.getUserApiClient(self.services["root_admin"]["username"],
                                                                self.services["root_domain"]["name"],
                                                                self.services["root_admin"]["roletype"])

        """
        Set public.ip.address.quarantine.duration to 60 minutes
        """
        update_configuration_cmd = updateConfiguration.updateConfigurationCmd()
        update_configuration_cmd.name = "public.ip.address.quarantine.duration"
        update_configuration_cmd.value = "1"
        self.apiclient.updateConfiguration(update_configuration_cmd)

        self.cleanup = []
        return

    def tearDown(self):
        """
        Reset public.ip.address.quarantine.duration to 0 minutes
        """
        update_configuration_cmd = updateConfiguration.updateConfigurationCmd()
        update_configuration_cmd.name = "public.ip.address.quarantine.duration"
        update_configuration_cmd.value = "0"
        self.apiclient.updateConfiguration(update_configuration_cmd)

        super(TestQuarantineIPs, self).tearDown()

    def create_vpc(self, api_client, services):
        # Get network offering
        network_offering = NetworkOffering.list(api_client, name="DefaultIsolatedNetworkOfferingForVpcNetworks")
        self.assertTrue(network_offering is not None and len(network_offering) > 0, "No VPC network offering")

        # Getting VPC offering
        vpc_offering = VpcOffering.list(api_client, name="Default VPC offering")
        self.assertTrue(vpc_offering is not None and len(vpc_offering) > 0, "No VPC offerings found")

        # Creating VPC
        vpc = VPC.create(
            apiclient=api_client,
            services=services,
            networkDomain="vpc.networkacl",
            vpcofferingid=vpc_offering[0].id,
            zoneid=self.zone.id,
            domainid=self.domain.id,
            start=False
        )

        self.cleanup.append(vpc)
        self.assertTrue(vpc is not None, "VPC creation failed")
        return vpc

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_only_owner_can_allocate_ip_in_quarantine_vpc(self):
        """ Test allocate IP in quarantine to VPC.
        """
        # Creating Domain Admin VPC
        domain_vpc = self.create_vpc(self.domain_admin_apiclient, self.services["domain_vpc"])

        # Allocating source nat first
        PublicIPAddress.create(self.domain_admin_apiclient,
                               zoneid=self.zone.id,
                               vpcid=domain_vpc.id)

        # Getting available public IP address
        ip_address = PublicIPAddress.list(self.domain_admin_apiclient, state="Free", listall=True)[0].ipaddress

        self.debug(
            f"creating public address with zone {self.zone.id} and vpc id {domain_vpc.id} and ip address {ip_address}.")
        # Associating public IP address to Domain Admin account
        public_ip = PublicIPAddress.create(self.domain_admin_apiclient,
                                           zoneid=self.zone.id,
                                           vpcid=domain_vpc.id,
                                           ipaddress=ip_address)
        self.assertIsNotNone(public_ip, "Failed to Associate IP Address")
        self.assertEqual(public_ip.ipaddress.ipaddress, ip_address, "Associated IP is not same as specified")

        self.debug(f"Disassociating public IP {public_ip.ipaddress.ipaddress}.")
        public_ip.delete(self.domain_admin_apiclient)

        # Creating Root Admin VPC
        root_vpc = self.create_vpc(self.admin_apiclient, self.services["root_vpc"])

        self.debug(f"Trying to allocate the same IP address {ip_address} that is still in quarantine.")

        with self.assertRaises(CloudstackAPIException) as exception:
            PublicIPAddress.create(self.admin_apiclient,
                                   zoneid=self.zone.id,
                                   vpcid=root_vpc.id,
                                   ipaddress=ip_address)
        self.assertIn(f"Failed to allocate public IP address [{ip_address}] as it is in quarantine.",
                      exception.exception.errorMsg)

        # Owner should be able to allocate its IP in quarantine
        public_ip = PublicIPAddress.create(self.domain_admin_apiclient,
                                           zoneid=self.zone.id,
                                           vpcid=domain_vpc.id,
                                           ipaddress=ip_address)
        self.assertIsNotNone(public_ip, "Failed to Associate IP Address")
        self.assertEqual(public_ip.ipaddress.ipaddress, ip_address, "Associated IP is not same as specified")

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_another_user_can_allocate_ip_after_quarantined_has_ended_vpc(self):
        """ Test allocate IP to VPC after quarantine has ended.
        """
        # Creating Domain Admin VPC
        domain_vpc = self.create_vpc(self.domain_admin_apiclient, self.services["domain_vpc"])

        # Allocating source nat first
        PublicIPAddress.create(self.domain_admin_apiclient,
                               zoneid=self.zone.id,
                               vpcid=domain_vpc.id)

        # Getting available public IP address
        ip_address = PublicIPAddress.list(self.domain_admin_apiclient, state="Free", listall=True)[0].ipaddress

        self.debug(
            f"creating public address with zone {self.zone.id} and vpc id {domain_vpc.id} and ip address {ip_address}.")
        # Associating public IP address to Domain Admin account
        public_ip = PublicIPAddress.create(self.domain_admin_apiclient,
                                           zoneid=self.zone.id,
                                           vpcid=domain_vpc.id,
                                           ipaddress=ip_address)
        self.assertIsNotNone(public_ip, "Failed to Associate IP Address")
        self.assertEqual(public_ip.ipaddress.ipaddress, ip_address, "Associated IP is not same as specified")

        self.debug(f"Disassociating public IP {public_ip.ipaddress.ipaddress}.")
        public_ip.delete(self.domain_admin_apiclient)

        # Creating Root Admin VPC
        root_vpc = self.create_vpc(self.admin_apiclient, self.services["root_vpc"])

        self.debug(f"Trying to allocate the same IP address {ip_address} after the quarantine duration.")

        time.sleep(60)

        public_ip_2 = PublicIPAddress.create(self.admin_apiclient,
                                             zoneid=self.zone.id,
                                             vpcid=root_vpc.id,
                                             ipaddress=ip_address)
        self.assertIsNotNone(public_ip_2, "Failed to Associate IP Address")
        self.assertEqual(public_ip_2.ipaddress.ipaddress, ip_address, "Associated IP is not same as specified")

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_only_owner_can_allocate_ip_in_quarantine_network(self):
        """ Test allocate IP in quarantine to network.
        """
        network_offering = NetworkOffering.list(self.domain_admin_apiclient,
                                                name="DefaultIsolatedNetworkOfferingWithSourceNatService")
        domain_network = Network.create(self.domain_admin_apiclient,
                                        zoneid=self.zone.id,
                                        services=self.services["domain_network"],
                                        networkofferingid=network_offering[0].id)
        self.cleanup.append(domain_network)

        # Allocating source nat first
        PublicIPAddress.create(self.domain_admin_apiclient,
                               zoneid=self.zone.id,
                               networkid=domain_network.id)

        # Getting available public IP address
        ip_address = PublicIPAddress.list(self.domain_admin_apiclient, state="Free", listall=True)[0].ipaddress

        self.debug(
            f"creating public address with zone {self.zone.id} and network id {domain_network.id} and ip address {ip_address}.")
        # Associating public IP address to Domain Admin account
        public_ip = PublicIPAddress.create(self.domain_admin_apiclient,
                                           zoneid=self.zone.id,
                                           networkid=domain_network.id,
                                           ipaddress=ip_address)
        self.assertIsNotNone(public_ip, "Failed to Associate IP Address")
        self.assertEqual(public_ip.ipaddress.ipaddress, ip_address, "Associated IP is not same as specified")

        self.debug(f"Disassociating public IP {public_ip.ipaddress.ipaddress}.")
        public_ip.delete(self.domain_admin_apiclient)

        # Creating Root Admin network
        root_network = Network.create(self.admin_apiclient,
                                      zoneid=self.zone.id,
                                      services=self.services["root_network"],
                                      networkofferingid=network_offering[0].id)
        self.cleanup.append(root_network)
        self.debug(f"Trying to allocate the same IP address {ip_address} that is still in quarantine.")

        with self.assertRaises(CloudstackAPIException) as exception:
            PublicIPAddress.create(self.admin_apiclient,
                                   zoneid=self.zone.id,
                                   networkid=root_network.id,
                                   ipaddress=ip_address)
        self.assertIn(f"Failed to allocate public IP address [{ip_address}] as it is in quarantine.",
                      exception.exception.errorMsg)

        # Owner should be able to allocate its IP in quarantine
        public_ip = PublicIPAddress.create(self.domain_admin_apiclient,
                                           zoneid=self.zone.id,
                                           networkid=domain_network.id,
                                           ipaddress=ip_address)
        self.assertIsNotNone(public_ip, "Failed to Associate IP Address")
        self.assertEqual(public_ip.ipaddress.ipaddress, ip_address, "Associated IP is not same as specified")

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_another_user_can_allocate_ip_after_quarantined_has_ended_network(self):
        """ Test allocate IP to network after quarantine has ended.
        """
        network_offering = NetworkOffering.list(self.domain_admin_apiclient,
                                                name="DefaultIsolatedNetworkOfferingWithSourceNatService")
        domain_network = Network.create(self.domain_admin_apiclient,
                                        zoneid=self.zone.id,
                                        services=self.services["domain_network"],
                                        networkofferingid=network_offering[0].id)
        self.cleanup.append(domain_network)
        # Allocating source nat first
        PublicIPAddress.create(self.domain_admin_apiclient,
                               zoneid=self.zone.id,
                               networkid=domain_network.id)

        # Getting available public IP address
        ip_address = PublicIPAddress.list(self.domain_admin_apiclient, state="Free", listall=True)[0].ipaddress

        self.debug(
            f"creating public address with zone {self.zone.id} and network id {domain_network.id} and ip address {ip_address}.")
        # Associating public IP address to Domain Admin account
        public_ip = PublicIPAddress.create(self.domain_admin_apiclient,
                                           zoneid=self.zone.id,
                                           networkid=domain_network.id,
                                           ipaddress=ip_address)
        self.assertIsNotNone(public_ip, "Failed to Associate IP Address")
        self.assertEqual(public_ip.ipaddress.ipaddress, ip_address, "Associated IP is not same as specified")

        self.debug(f"Disassociating public IP {public_ip.ipaddress.ipaddress}.")
        public_ip.delete(self.domain_admin_apiclient)

        # Creating Root Admin VPC
        root_network = Network.create(self.admin_apiclient,
                                      zoneid=self.zone.id,
                                      services=self.services["root_network"],
                                      networkofferingid=network_offering[0].id)
        self.cleanup.append(root_network)

        self.debug(f"Trying to allocate the same IP address {ip_address} after the quarantine duration.")

        time.sleep(60)

        public_ip_2 = PublicIPAddress.create(self.admin_apiclient,
                                             zoneid=self.zone.id,
                                             networkid=domain_network.id,
                                             ipaddress=ip_address)
        self.assertIsNotNone(public_ip_2, "Failed to Associate IP Address")
        self.assertEqual(public_ip_2.ipaddress.ipaddress, ip_address, "Associated IP is not same as specified")
