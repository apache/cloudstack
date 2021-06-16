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

""" Component tests VM deployment in VPC network functionality
"""
#Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.base import (VirtualMachine,
                             NetworkOffering,
                             VpcOffering,
                             VPC,
                             NetworkACL,
                             PrivateGateway,
                             StaticRoute,
                             Router,
                             Network,
                             Account,
                             ServiceOffering,
                             PublicIPAddress,
                             NATRule,
                             StaticNATRule,
                             Configurations)

from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               wait_for_cleanup,
                               get_free_vlan)

from marvin.lib.utils import (cleanup_resources, validateList)
from marvin.codes import *
from marvin.cloudstackAPI import rebootRouter
from marvin.cloudstackAPI import updateResourceCount

class Services:
    """Test IP count inn VPC network
    """

    def __init__(self):
        self.services = {
            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                # Random characters are appended for unique
                # username
                "password": "password",
            },
            "service_offering": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
            },
            "network_offering": {
                "name": 'VPC Network offering',
                "displaytext": 'VPC Network off',
                "guestiptype": 'Isolated',
                "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Lb,UserData,StaticNat,NetworkACL',
                "traffictype": 'GUEST',
                "availability": 'Optional',
                "useVpc": 'on',
                "serviceProviderList": {
                    "Dhcp": 'VpcVirtualRouter',
                    "Dns": 'VpcVirtualRouter',
                    "SourceNat": 'VpcVirtualRouter',
                    "PortForwarding": 'VpcVirtualRouter',
                    "Lb": 'VpcVirtualRouter',
                    "UserData": 'VpcVirtualRouter',
                    "StaticNat": 'VpcVirtualRouter',
                    "NetworkACL": 'VpcVirtualRouter'
                },
            },
            "network_offering_no_lb": {
                "name": 'VPC Network offering',
                "displaytext": 'VPC Network off',
                "guestiptype": 'Isolated',
                "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,UserData,StaticNat,NetworkACL',
                "traffictype": 'GUEST',
                "availability": 'Optional',
                "useVpc": 'on',
                "serviceProviderList": {
                    "Dhcp": 'VpcVirtualRouter',
                    "Dns": 'VpcVirtualRouter',
                    "SourceNat": 'VpcVirtualRouter',
                    "PortForwarding": 'VpcVirtualRouter',
                    "UserData": 'VpcVirtualRouter',
                    "StaticNat": 'VpcVirtualRouter',
                    "NetworkACL": 'VpcVirtualRouter'
                },
            },
            "vpc_offering": {
                "name": 'VPC off',
                "displaytext": 'VPC off',
                "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Lb,UserData,StaticNat',
            },
            "vpc": {
                "name": "TestVPC",
                "displaytext": "TestVPC",
                "cidr": '10.0.0.1/24'
            },
            "network": {
                "name": "Test Network",
                "displaytext": "Test Network",
                "netmask": '255.255.255.0',
                "limit": 5,
                # Max networks allowed as per hypervisor
                # Xenserver -> 5, VMWare -> 9
            },
 
            "virtual_machine": {
                "displayname": "Test VM",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'XenServer',
                # Hypervisor type should be same as
                # hypervisor type of cluster
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },
            "ostype": 'CentOS 5.3 (64-bit)',
            # Cent OS 5.3 (64 bit)
            "timeout": 10,
            "mode": 'advanced'
        }


class TestIPResourceCountVPC(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestIPResourceCountVPC, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls.vpc_off = VpcOffering.create(
            cls.api_client,
            cls.services["vpc_offering"]
        )
        cls.vpc_off.update(cls.api_client, state='Enabled')
        cls._cleanup = [
            cls.service_offering,
            cls.vpc_off
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup = [self.account]
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def validate_vpc_offering(self, vpc_offering):
        """Validates the VPC offering"""

        self.debug("Check if the VPC offering is created successfully?")
        vpc_offs = VpcOffering.list(
            self.apiclient,
            id=vpc_offering.id
        )
        self.assertEqual(
            isinstance(vpc_offs, list),
            True,
            "List VPC offerings should return a valid list"
        )
        self.assertEqual(
            vpc_offering.name,
            vpc_offs[0].name,
            "Name of the VPC offering should match with listVPCOff data"
        )
        self.debug(
            "VPC offering is created successfully - %s" %
            vpc_offering.name)
        return

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


    def updateIPCount(self):
        cmd=updateResourceCount.updateResourceCountCmd()
        cmd.account=self.account.name
        cmd.domainid=self.domain.id

        responce=self.apiclient.updateResourceCount(cmd)

    def acquire_publicip(self, network, vpc):
        self.debug("Associating public IP for network: %s" % network.name)
        public_ip = PublicIPAddress.create(self.apiclient,
                                           accountid=self.account.name,
                                           zoneid=self.zone.id,
                                           domainid=self.account.domainid,
                                           networkid=network.id,
                                           vpcid=vpc.id
                                           )
        self.debug("Associated {} with network {}".format(public_ip.ipaddress.ipaddress, network.id))
        return public_ip

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_01_ip_resouce_count_vpc_network(self):
        """ Test IP count in VPC networks
        """
        self.debug("Creating a VPC offering..")
        vpc_off = VpcOffering.create(
            self.apiclient,
            self.services["vpc_offering"]
        )

        self.validate_vpc_offering(vpc_off)

        self.debug("Enabling the VPC offering created")
        vpc_off.update(self.apiclient, state='Enabled')

        self.debug("creating a VPC network in the account: %s" %
                   self.account.name)
        self.services["vpc"]["cidr"] = '10.1.1.1/16'
        vpc = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.validate_vpc_network(vpc)


        nw_off = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        nw_off.update(self.apiclient, state='Enabled')
        self._cleanup.append(nw_off)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" % nw_off.id)
        network_1 = Network.create(
            self.apiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=nw_off.id,
            zoneid=self.zone.id,
            gateway='10.1.1.1',
            vpcid=vpc.id
        )
        self.debug("Created network with ID: %s" % network_1.id)

        account_list = Account.list(self.apiclient, id=self.account.id)
        totalip_1 = account_list[0].iptotal
        self.debug("Total IP: %s" % totalip_1)

        public_ip_1 = self.acquire_publicip(network_1, vpc)
        public_ip_2 = self.acquire_publicip(network_1, vpc)
        public_ip_3 = self.acquire_publicip(network_1, vpc)

        account_list = Account.list(self.apiclient, id=self.account.id)
        totalip = account_list[0].iptotal

        self.debug("Total IP: %s" % totalip)

        self.assertTrue(totalip - totalip_1 == 3,"publicip count is 3")
        self.updateIPCount()

        account_list = Account.list(self.apiclient, id=self.account.id)
        totalip = account_list[0].iptotal
        self.assertTrue(totalip - totalip_1 == 3, "publicip count is 3")


