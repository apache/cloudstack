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

""" Component tests for VPC functionality
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackException import CloudstackAPIException
from marvin.cloudstackAPI import updateZone
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Account,
                             VPC,
                             VpcOffering,
                             VirtualMachine,
                             ServiceOffering,
                             Network,
                             NetworkOffering,
                             PublicIPAddress,
                             LoadBalancerRule,
                             Router,
                             NetworkACL,
                             NATRule,
                             Zone,
                             StaticNATRule)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_configurations)
import time


class Services:

    """Test VPC services
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
            "domain_admin": {
                "email": "domain@admin.com",
                "firstname": "Domain",
                "lastname": "Admin",
                "username": "DoA",
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
                "supportedservices": 'Vpn,Dhcp,Dns,SourceNat,PortForwarding,Lb,UserData,StaticNat,NetworkACL',
                "traffictype": 'GUEST',
                "availability": 'Optional',
                "useVpc": 'on',
                "serviceProviderList": {
                    "Vpn": 'VpcVirtualRouter',
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
                "supportedservices": 'Vpn,Dhcp,Dns,SourceNat,PortForwarding,UserData,StaticNat,NetworkACL',
                "traffictype": 'GUEST',
                "availability": 'Optional',
                "useVpc": 'on',
                "serviceProviderList": {
                    "Vpn": 'VpcVirtualRouter',
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
                "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Lb,UserData,StaticNat,NetworkACL',
            },
            "vpc": {
                "name": "TestVPC",
                "displaytext": "TestVPC",
                "cidr": '10.0.0.1/24'
            },
            "vpc_no_name": {
                "displaytext": "TestVPC",
                "cidr": '10.0.0.1/24'
            },
            "network": {
                "name": "Test Network",
                "displaytext": "Test Network",
                "netmask": '255.255.255.0'
            },
            "lbrule": {
                "name": "SSH",
                "alg": "leastconn",
                # Algorithm used for load balancing
                "privateport": 22,
                "publicport": 2222,
                "openfirewall": False,
                "startport": 22,
                "endport": 2222,
                "protocol": "TCP",
                "cidrlist": '0.0.0.0/0',
            },
            "natrule": {
                "privateport": 22,
                "publicport": 22,
                "startport": 22,
                "endport": 22,
                "protocol": "TCP",
                "cidrlist": '0.0.0.0/0',
            },
            "fw_rule": {
                "startport": 1,
                "endport": 6000,
                "cidr": '0.0.0.0/0',
                # Any network (For creating FW rule)
                "protocol": "TCP"
            },
            "icmp_rule": {
                "icmptype": -1,
                "icmpcode": -1,
                "cidrlist": '0.0.0.0/0',
                "protocol": "ICMP"
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
            "domain": {
                "name": "TestDomain"
            },
            "ostype": 'CentOS 5.3 (64-bit)',
            # Cent OS 5.3 (64 bit)
            "sleep": 60,
            "timeout": 10,
            "mode": 'advanced'
        }


class TestVPC(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVPC, cls).getClsTestClient()
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
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
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
        self.cleanup = []
        self.cleanup.insert(0, self.account)
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning: Exception during cleanup : %s" % e)
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

    # list_vpc_apis should be the first case otherwise the vpc counts would be
    # wrong
    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_01_list_vpc_apis(self):
        """ Test list VPC APIs
        """

        # Validate the following
        # 1. Create multiple VPCs
        # 2. listVPCs() by name. VPC with the provided name should be listed.
        # 3. listVPCs() by displayText. VPC with the provided displayText
        #    should be listed.
        # 4. listVPCs() by cidr. All the VPCs with the provided cidr should
        #    be listed.
        # 5. listVPCs() by vpcofferingId.All the VPCs with the vpcofferingId
        #    should be listed.
        # 6. listVPCs() by supported Services(). All the VPCs that provide the
        #    list of services should be listed.
        # 7. listVPCs() by restartRequired (set to true). All the VPCs that
        #    require restart should be listed.

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("creating a VPC network in the account: %s" %
                   self.account.name)
        vpc_1 = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.validate_vpc_network(vpc_1)

        self.services["vpc"]["cidr"] = "10.1.46.1/16"
        vpc_2 = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.validate_vpc_network(vpc_2)

        self.debug("Check list VPC API by Name?")
        vpcs = VPC.list(
            self.apiclient,
            name=vpc_1.name,
            listall=True
        )
        self.assertEqual(
            isinstance(vpcs, list),
            True,
            "List VPC shall return a valid resposne"
        )
        vpc = vpcs[0]
        self.assertEqual(
            vpc.name,
            vpc_1.name,
            "VPC name should match with the existing one"
        )

        self.debug("Check list VPC API by displayText?")
        vpcs = VPC.list(
            self.apiclient,
            displaytext=vpc_1.displaytext,
            listall=True
        )
        self.assertEqual(
            isinstance(vpcs, list),
            True,
            "List VPC shall return a valid resposne"
        )
        vpc = vpcs[0]
        self.assertEqual(
            vpc.displaytext,
            vpc_1.displaytext,
            "VPC displaytext should match with the existing one"
        )

        self.debug("Check list VPC API by cidr?")
        vpcs = VPC.list(
            self.apiclient,
            cidr=vpc_2.cidr,
            listall=True
        )
        self.assertEqual(
            isinstance(vpcs, list),
            True,
            "List VPC shall return a valid resposne"
        )
        vpc = vpcs[0]
        self.assertEqual(
            vpc.cidr,
            vpc_2.cidr,
            "VPC cidr should match with the existing one"
        )
        self.debug("Validating list VPC by Id")
        self.validate_vpc_network(vpc_1)

        self.debug("Validating list VPC by vpcofferingId")
        vpcs = VPC.list(
            self.apiclient,
            vpcofferingid=self.vpc_off.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vpcs, list),
            True,
            "List VPC by vpcofferingId should return a valid response"
        )
        self.debug("Length of list VPC response: %s" % len(vpcs))
        self.assertEqual(
            len(vpcs),
            2,
            "List VPC should return 2 enabled VPCs"
        )
        for vpc in vpcs:
            self.assertEqual(
                vpc.vpcofferingid,
                self.vpc_off.id,
                "VPC offering ID should match with that of resposne"
            )

        self.debug("Validating list VPC by supportedservices")
        vpcs = VPC.list(
            self.apiclient,
            supportedservices='Vpn,Dhcp,Dns,SourceNat,PortForwarding,Lb,UserData,StaticNat,NetworkACL',
            listall=True,
            account=self.account.name,
            domainid=self.account.domainid)
        self.assertEqual(
            isinstance(vpcs, list),
            True,
            "List VPC by vpcofferingId should return a valid response"
        )
        for vpc in vpcs:
            self.assertIn(
                vpc.id,
                [vpc_1.id, vpc_2.id],
                "VPC offering ID should match with that of resposne"
            )
        self.debug("Validating list VPC by restart required")
        vpcs = VPC.list(
            self.apiclient,
            restartrequired=True,
            listall=True,
            account=self.account.name,
            domainid=self.account.domainid
        )
        if vpcs is not None:
            for vpc in vpcs:
                self.assertEqual(
                    vpc.restartrequired,
                    True,
                    "RestartRequired should be set as True"
                )
        self.debug("Validating list VPC by restart required")
        vpcs = VPC.list(
            self.apiclient,
            restartrequired=False,
            listall=True,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(vpcs, list),
            True,
            "List VPC by vpcofferingId should return a valid response"
        )
        if vpcs is not None:
            for vpc in vpcs:
                self.assertEqual(
                    vpc.restartrequired,
                    False,
                    "RestartRequired should be set as False"
                )
        return

    @attr(tags=["advanced", "intervlan", "dvs"], required_hardware="false")
    def test_02_restart_vpc_no_networks(self):
        """ Test restart VPC having no networks
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Restart VPC. Restart VPC should be successful

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("creating a VPC network in the account: %s" %
                   self.account.name)
        vpc = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.validate_vpc_network(vpc)

        self.debug("Restarting the VPC with no network")
        try:
            vpc.restart(self.apiclient)
        except Exception as e:
            self.fail("Failed to restart VPC network - %s" % e)

        self.validate_vpc_network(vpc, state='Enabled')
        return

    @attr(tags=["advanced", "intervlan", "dvs"], required_hardware="false")
    def test_03_restart_vpc_with_networks(self):
        """ Test restart VPC having networks
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add couple of networks to VPC.
        # 3. Restart VPC. Restart network should be successful

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("creating a VPC network in the account: %s" %
                   self.account.name)
        vpc = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.validate_vpc_network(vpc)

        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

        gateway = vpc.cidr.split('/')[0]
        # Split the cidr to retrieve gateway
        # for eg. cidr = 10.0.0.1/24
        # Gateway = 10.0.0.1

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        network_1 = Network.create(
            self.apiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id,
            gateway=gateway,
            vpcid=vpc.id
        )
        self.debug("Created network with ID: %s" % network_1.id)

        self.network_offering_no_lb = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering_no_lb"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering_no_lb.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering_no_lb)

        gateway = '10.1.2.1'    # New network -> different gateway
        self.debug("Creating network with network offering: %s" %
                   self.network_offering_no_lb.id)
        network_2 = Network.create(
            self.apiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering_no_lb.id,
            zoneid=self.zone.id,
            gateway=gateway,
            vpcid=vpc.id
        )
        self.debug("Created network with ID: %s" % network_2.id)

        self.debug("Restarting the VPC with no network")
        try:
            vpc.restart(self.apiclient)
        except Exception as e:
            self.fail("Failed to restart VPC network - %s" % e)

        self.validate_vpc_network(vpc, state='Enabled')
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_04_delete_vpc_no_networks(self):
        """ Test delete VPC having no networks
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Delete VPC. Delete VPC should be successful

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("creating a VPC network in the account: %s" %
                   self.account.name)
        vpc = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.validate_vpc_network(vpc)

        self.debug("Restarting the VPC with no network")
        try:
            vpc.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete VPC network - %s" % e)

        self.debug("Check if the VPC offering is deleted successfully?")
        vpcs = VPC.list(
            self.apiclient,
            id=vpc.id
        )
        self.assertEqual(
            vpcs,
            None,
            "List VPC offerings should not return anything"
        )
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_05_delete_vpc_with_networks(self):
        """ Test delete VPC having networks
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add couple of networks to VPC.
        # 3. Delete VPC. Delete network should be successful
        # 4. Virtual Router should be deleted
        # 5. Source NAT should be released back to pool

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("creating a VPC network in the account: %s" %
                   self.account.name)
        vpc = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.validate_vpc_network(vpc)

        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

        gateway = vpc.cidr.split('/')[0]
        # Split the cidr to retrieve gateway
        # for eg. cidr = 10.0.0.1/24
        # Gateway = 10.0.0.1

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        network_1 = Network.create(
            self.apiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id,
            gateway=gateway,
            vpcid=vpc.id
        )
        self.debug("Created network with ID: %s" % network_1.id)

        self.network_offering_no_lb = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering_no_lb"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering_no_lb.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering_no_lb)

        gateway = '10.1.2.1'    # New network -> different gateway
        self.debug("Creating network with network offering: %s" %
                   self.network_offering_no_lb.id)
        network_2 = Network.create(
            self.apiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering_no_lb.id,
            zoneid=self.zone.id,
            gateway=gateway,
            vpcid=vpc.id
        )
        self.debug("Created network with ID: %s" % network_2.id)

        self.debug("Deleting the VPC with no network")
        with self.assertRaises(Exception):
            vpc.delete(self.apiclient)
        self.debug("Delete VPC failed as there are still networks in VPC")
        self.debug("Deleting the networks in the VPC")

        try:
            network_1.delete(self.apiclient)
            network_2.delete(self.apiclient)
        except Exception as e:
            self.fail("failed to delete the VPC networks: %s" % e)

        self.debug("Now trying to delete VPC")
        try:
            vpc.delete(self.apiclient)
        except Exception as e:
            self.fail("Delete to restart VPC network - %s" % e)

        self.debug("Check if the VPC offering is deleted successfully?")
        vpcs = VPC.list(
            self.apiclient,
            id=vpc.id
        )
        self.assertEqual(
            vpcs,
            None,
            "List VPC offerings should not return anything"
        )
        self.debug(
            "Waiting for network.gc.interval to cleanup network resources")
        interval = list_configurations(
            self.apiclient,
            name='network.gc.interval'
        )
        wait = list_configurations(
            self.apiclient,
            name='network.gc.wait'
        )
        # Sleep to ensure that all resources are deleted
        time.sleep(int(interval[0].value) + int(wait[0].value))
        self.debug("Check if VR is deleted or not?")
        routers = Router.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )
        self.assertEqual(
            routers,
            None,
            "List Routers for the account should not return any response"
        )
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_06_list_vpc_apis_admin(self):
        """ Test list VPC APIs for different user roles
        """

        # Validate the following
        # 1. list VPCS as admin User to view all the Vpcs owned by admin user
        # 2. list VPCS as regular User to view all the Vpcs owned by user
        # 3. list VPCS as domain admin User to view all the Vpcs owned by admin

        self.user = Account.create(
            self.apiclient,
            self.services["account"],
        )
        self.cleanup.append(self.user)

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("creating a VPC network in the account: %s" %
                   self.account.name)
        vpc_1 = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.validate_vpc_network(vpc_1)

        self.services["vpc"]["cidr"] = "10.1.46.1/16"
        vpc_2 = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.user.name,
            domainid=self.user.domainid
        )
        self.validate_vpc_network(vpc_2)

        self.debug("Validating list VPCs call by passing account and domain")
        vpcs = VPC.list(
            self.apiclient,
            account=self.user.name,
            domainid=self.user.domainid,
            listall=True
        )
        self.assertEqual(
            isinstance(vpcs, list),
            True,
            "List VPC should return a valid response"
        )
        vpc = vpcs[0]
        self.assertEqual(
            vpc.id,
            vpc_2.id,
            "List VPC should return VPC belonging to that account"
        )
        return

    @attr(tags=["advanced", "intervlan", "multiple"], required_hardware="true")
    def test_07_restart_network_vm_running(self):
        """ Test Restart VPC when there are multiple networks associated
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1(10.1.1.1/24) and network2(10.1.2.1/24) to this VPC
        # 3. Deploy vm1 and vm2 in network1 and vm3 and vm4 in network2
        # 4. Create a PF rule using TCP protocol on port 22 for vm1
        # 5. Create a Static Nat rule  for vm2
        # 6. Create an LB rule for vm3 and vm4
        # 7. Create ingress network ACL for allowing all the above rules from
        #    public ip range on network1 and network2.
        # 8. Create egress network ACL for network1 and network2 to access
        #    google.com
        # 9. Create a private gateway for this VPC and add a static route to
        #    this gateway
        # 10. Create a VPN gateway for this VPC and add static route to gateway
        # 11. Make sure that all the PF, LB and Static NAT rules work
        # 12. Make sure that we are able to access google.com from all VM
        # 13. Make sure that the newly added private gateway's and VPN
        #    gateway's static routes work as expected.

        self.debug("Creating a VPC offering..")
        vpc_off = VpcOffering.create(
            self.apiclient,
            self.services["vpc_offering"]
        )

        self.cleanup.append(vpc_off)
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

        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

        self.network_offering_no_lb = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering_no_lb"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering_no_lb.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering_no_lb)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering_no_lb.id)
        network_1 = Network.create(
            self.apiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering_no_lb.id,
            zoneid=self.zone.id,
            gateway='10.1.1.1',
            vpcid=vpc.id
        )
        self.debug("Created network with ID: %s" % network_1.id)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        network_2 = Network.create(
            self.apiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id,
            gateway='10.1.2.1',
            vpcid=vpc.id
        )
        self.debug("Created network with ID: %s" % network_2.id)

        self.debug("deploying VMs in network: %s" % network_1.name)
        # Spawn an instance in that network
        vm_1 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(network_1.id)]
        )
        self.debug("Deployed VM in network: %s" % network_1.id)

        vm_2 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(network_1.id)]
        )
        self.debug("Deployed VM in network: %s" % network_1.id)

        self.debug("deploying VMs in network: %s" % network_2.name)
        # Spawn an instance in that network
        vm_3 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(network_2.id)]
        )
        self.debug("Deployed VM in network: %s" % network_2.id)

        vm_4 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(network_2.id)]
        )
        self.debug("Deployed VM in network: %s" % network_2.id)

        self.debug("Associating public IP for network: %s" % network_1.name)
        public_ip_1 = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network_1.id,
            vpcid=vpc.id
        )
        self.debug("Associated %s with network %s" % (
            public_ip_1.ipaddress.ipaddress,
            network_1.id
        ))

        NATRule.create(
            self.apiclient,
            vm_1,
            self.services["natrule"],
            ipaddressid=public_ip_1.ipaddress.id,
            openfirewall=False,
            networkid=network_1.id,
            vpcid=vpc.id
        )

        self.debug("Adding NetwrokACl rules to make NAT rule accessible")
        NetworkACL.create(
            self.apiclient,
            networkid=network_1.id,
            services=self.services["natrule"],
            traffictype='Ingress'
        )

        self.debug("Associating public IP for network: %s" % network_1.name)
        public_ip_2 = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network_1.id,
            vpcid=vpc.id
        )
        self.debug("Associated %s with network %s" % (
            public_ip_2.ipaddress.ipaddress,
            network_1.id
        ))
        self.debug("Enabling static NAT for IP: %s" %
                   public_ip_2.ipaddress.ipaddress)
        try:
            StaticNATRule.enable(
                self.apiclient,
                ipaddressid=public_ip_2.ipaddress.id,
                virtualmachineid=vm_2.id,
                networkid=network_1.id
            )
            self.debug("Static NAT enabled for IP: %s" %
                       public_ip_2.ipaddress.ipaddress)
        except Exception as e:
            self.fail("Failed to enable static NAT on IP: %s - %s" % (
                public_ip_2.ipaddress.ipaddress, e))

        public_ips = PublicIPAddress.list(
            self.apiclient,
            networkid=network_1.id,
            listall=True,
            isstaticnat=True,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(public_ips, list),
            True,
            "List public Ip for network should list the Ip addr"
        )
        self.assertEqual(
            public_ips[0].ipaddress,
            public_ip_2.ipaddress.ipaddress,
            "List public Ip for network should list the Ip addr"
        )

        self.debug("Associating public IP for network: %s" % vpc.name)
        public_ip_3 = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network_2.id,
            vpcid=vpc.id
        )
        self.debug("Associated %s with network %s" % (
            public_ip_3.ipaddress.ipaddress,
            network_2.id
        ))

        self.debug("Creating LB rule for IP address: %s" %
                   public_ip_3.ipaddress.ipaddress)

        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.services["lbrule"],
            ipaddressid=public_ip_3.ipaddress.id,
            accountid=self.account.name,
            networkid=network_2.id,
            vpcid=vpc.id,
            domainid=self.account.domainid
        )

        self.debug("Adding virtual machines %s and %s to LB rule" % (
            vm_3.name, vm_4.name))
        lb_rule.assign(self.apiclient, [vm_3, vm_4])

        self.debug("Adding NetwrokACl rules to make PF and LB accessible")
        NetworkACL.create(
            self.apiclient,
            networkid=network_2.id,
            services=self.services["lbrule"],
            traffictype='Ingress'
        )

        self.debug("Adding Egress rules to network %s and %s to allow\
                    access to internet")
        NetworkACL.create(
            self.apiclient,
            networkid=network_1.id,
            services=self.services["icmp_rule"],
            traffictype='Egress'
        )
        NetworkACL.create(
            self.apiclient,
            networkid=network_2.id,
            services=self.services["icmp_rule"],
            traffictype='Egress'
        )

        self.debug("Checking if we can SSH into VM_1?")
        try:
            ssh_1 = vm_1.get_ssh_client(
                ipaddress=public_ip_1.ipaddress.ipaddress,
                reconnect=True,
                port=self.services["natrule"]["publicport"]
            )
            self.debug("SSH into VM is successfully")

            self.debug("Verifying if we can ping to outside world from VM?")
            # Ping to outsite world
            res = ssh_1.execute("ping -c 1 www.google.com")
            # res = 64 bytes from maa03s17-in-f20.1e100.net (74.125.236.212):
            # icmp_req=1 ttl=57 time=25.9 ms
            # --- www.l.google.com ping statistics ---
            # 1 packets transmitted, 1 received, 0% packet loss, time 0ms
            # rtt min/avg/max/mdev = 25.970/25.970/25.970/0.000 ms
        except Exception as e:
            self.fail("Failed to SSH into VM - %s, %s" %
                      (public_ip_1.ipaddress.ipaddress, e))

        result = str(res)
        self.debug("Result: %s" % result)
        self.assertEqual(
            result.count("1 received"),
            1,
            "Ping to outside world from VM should be successful"
        )

        self.debug("Checking if we can SSH into VM_2?")
        try:
            ssh_2 = vm_2.get_ssh_client(
                ipaddress=public_ip_2.ipaddress.ipaddress,
                reconnect=True,
                port=self.services["natrule"]["publicport"]
            )
            self.debug("SSH into VM is successfully")

            self.debug("Verifying if we can ping to outside world from VM?")
            res = ssh_2.execute("ping -c 1 www.google.com")
        except Exception as e:
            self.fail("Failed to SSH into VM - %s, %s" %
                      (public_ip_2.ipaddress.ipaddress, e))

        result = str(res)
        self.debug("Result: %s" % result)
        self.assertEqual(
            result.count("1 received"),
            1,
            "Ping to outside world from VM should be successful"
        )

        self.debug("Checking if we can SSH into VM using LB rule?")
        try:
            ssh_3 = vm_3.get_ssh_client(
                ipaddress=public_ip_3.ipaddress.ipaddress,
                reconnect=True,
                port=self.services["lbrule"]["publicport"]
            )
            self.debug("SSH into VM is successfully")

            self.debug("Verifying if we can ping to outside world from VM?")
            res = ssh_3.execute("ping -c 1 www.google.com")
        except Exception as e:
            self.fail("Failed to SSH into VM - %s, %s" %
                      (public_ip_3.ipaddress.ipaddress, e))

        result = str(res)
        self.debug("Result: %s" % result)
        self.assertEqual(
            result.count("1 received"),
            1,
            "Ping to outside world from VM should be successful"
        )
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_08_delete_vpc(self):
        """ Test vpc deletion after account deletion
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1(10.1.1.1/24) and network2(10.1.2.1/24) to this VPC
        # 3. Deploy vm1 and vm2 in network1 and vm3 and vm4 in network2
        # 4. Create a PF rule using TCP protocol on port 22 for vm1
        # 5. Create a Static Nat rule  for vm2
        # 6. Create an LB rule for vm3 and vm4
        # 7. Create ingress network ACL for allowing all the above rules from
        #    public ip range on network1 and network2.
        # 8. Create egress network ACL for network1 and network2 to access
        #    google.com
        # 9. Delete account

        self.debug("Removing account from cleanup list")
        self.cleanup = []
        self.debug("Creating a VPC offering..")
        vpc_off = VpcOffering.create(
            self.apiclient,
            self.services["vpc_offering"]
        )

        self.cleanup.append(vpc_off)
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

        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

        self.network_offering_no_lb = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering_no_lb"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering_no_lb.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering_no_lb)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        network_1 = Network.create(
            self.apiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering_no_lb.id,
            zoneid=self.zone.id,
            gateway='10.1.1.1',
            vpcid=vpc.id
        )
        self.debug("Created network with ID: %s" % network_1.id)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering_no_lb.id)
        network_2 = Network.create(
            self.apiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id,
            gateway='10.1.2.1',
            vpcid=vpc.id
        )
        self.debug("Created network with ID: %s" % network_2.id)

        self.debug("deploying VMs in network: %s" % network_1.name)
        # Spawn an instance in that network
        vm_1 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(network_1.id)]
        )
        self.debug("Deployed VM in network: %s" % network_1.id)

        vm_2 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(network_1.id)]
        )
        self.debug("Deployed VM in network: %s" % network_1.id)

        self.debug("deploying VMs in network: %s" % network_2.name)
        # Spawn an instance in that network
        vm_3 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(network_2.id)]
        )
        self.debug("Deployed VM in network: %s" % network_2.id)

        vm_4 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(network_2.id)]
        )
        self.debug("Deployed VM in network: %s" % network_2.id)

        self.debug("Associating public IP for network: %s" % network_1.name)
        public_ip_1 = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network_1.id,
            vpcid=vpc.id
        )
        self.debug("Associated %s with network %s" % (
            public_ip_1.ipaddress.ipaddress,
            network_1.id
        ))

        NATRule.create(
            self.apiclient,
            vm_1,
            self.services["natrule"],
            ipaddressid=public_ip_1.ipaddress.id,
            openfirewall=False,
            networkid=network_1.id,
            vpcid=vpc.id
        )

        self.debug("Adding NetwrokACl rules to make NAT rule accessible")
        NetworkACL.create(
            self.apiclient,
            networkid=network_1.id,
            services=self.services["natrule"],
            traffictype='Ingress'
        )

        self.debug("Associating public IP for network: %s" % network_1.name)
        public_ip_2 = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network_1.id,
            vpcid=vpc.id
        )
        self.debug("Associated %s with network %s" % (
            public_ip_2.ipaddress.ipaddress,
            network_1.id
        ))
        self.debug("Enabling static NAT for IP: %s" %
                   public_ip_2.ipaddress.ipaddress)
        try:
            StaticNATRule.enable(
                self.apiclient,
                ipaddressid=public_ip_2.ipaddress.id,
                virtualmachineid=vm_2.id,
                networkid=network_1.id
            )
            self.debug("Static NAT enabled for IP: %s" %
                       public_ip_2.ipaddress.ipaddress)
        except Exception as e:
            self.fail("Failed to enable static NAT on IP: %s - %s" % (
                public_ip_2.ipaddress.ipaddress, e))

        public_ips = PublicIPAddress.list(
            self.apiclient,
            networkid=network_1.id,
            listall=True,
            isstaticnat=True,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(public_ips, list),
            True,
            "List public Ip for network should list the Ip addr"
        )
        self.assertEqual(
            public_ips[0].ipaddress,
            public_ip_2.ipaddress.ipaddress,
            "List public Ip for network should list the Ip addr"
        )

        self.debug("Associating public IP for network: %s" % vpc.name)
        public_ip_3 = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network_2.id,
            vpcid=vpc.id
        )
        self.debug("Associated %s with network %s" % (
            public_ip_3.ipaddress.ipaddress,
            network_2.id
        ))

        self.debug("Creating LB rule for IP address: %s" %
                   public_ip_3.ipaddress.ipaddress)

        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.services["lbrule"],
            ipaddressid=public_ip_3.ipaddress.id,
            accountid=self.account.name,
            networkid=network_2.id,
            vpcid=vpc.id,
            domainid=self.account.domainid
        )

        self.debug("Adding virtual machines %s and %s to LB rule" % (
            vm_3.name, vm_4.name))
        lb_rule.assign(self.apiclient, [vm_3, vm_4])

        self.debug("Adding NetwrokACl rules to make PF and LB accessible")
        NetworkACL.create(
            self.apiclient,
            networkid=network_2.id,
            services=self.services["lbrule"],
            traffictype='Ingress'
        )

        self.debug(
            "Adding Egress rules to network %s and %s to allow\
            access to internet")
        NetworkACL.create(
            self.apiclient,
            networkid=network_1.id,
            services=self.services["icmp_rule"],
            traffictype='Egress'
        )
        NetworkACL.create(
            self.apiclient,
            networkid=network_2.id,
            services=self.services["icmp_rule"],
            traffictype='Egress'
        )

        self.debug("Checking if we can SSH into VM_1?")
        try:
            ssh_1 = vm_1.get_ssh_client(
                ipaddress=public_ip_1.ipaddress.ipaddress,
                reconnect=True,
                port=self.services["natrule"]["publicport"])
            self.debug("SSH into VM is successfully")

            self.debug("Verifying if we can ping to outside world from VM?")
            # Ping to outsite world
            res = ssh_1.execute("ping -c 1 www.google.com")
            # res = 64 bytes from maa03s17-in-f20.1e100.net (74.125.236.212):
            # icmp_req=1 ttl=57 time=25.9 ms
            # --- www.l.google.com ping statistics ---
            # 1 packets transmitted, 1 received, 0% packet loss, time 0ms
            # rtt min/avg/max/mdev = 25.970/25.970/25.970/0.000 ms
        except Exception as e:
            self.fail("Failed to SSH into VM - %s, %s" %
                      (public_ip_1.ipaddress.ipaddress, e))

        result = str(res)
        self.debug("result: %s" % result)
        self.assertEqual(
            result.count("1 received"),
            1,
            "Ping to outside world from VM should be successful"
        )

        self.debug("Checking if we can SSH into VM_2?")
        try:
            ssh_2 = vm_2.get_ssh_client(
                ipaddress=public_ip_2.ipaddress.ipaddress,
                reconnect=True,
                port=self.services["natrule"]["publicport"])
            self.debug("SSH into VM is successfully")

            self.debug("Verifying if we can ping to outside world from VM?")
            res = ssh_2.execute("ping -c 1 www.google.com")
        except Exception as e:
            self.fail("Failed to SSH into VM - %s, %s" %
                      (public_ip_2.ipaddress.ipaddress, e))

        result = str(res)
        self.debug("Result: %s" % result)
        self.assertEqual(
            result.count("1 received"),
            1,
            "Ping to outside world from VM should be successful"
        )

        self.debug("Checking if we can SSH into VM using LB rule?")
        try:
            ssh_3 = vm_3.get_ssh_client(
                ipaddress=public_ip_3.ipaddress.ipaddress,
                reconnect=True,
                port=self.services["lbrule"]["publicport"]
            )
            self.debug("SSH into VM is successfully")

            self.debug("Verifying if we can ping to outside world from VM?")
            res = ssh_3.execute("ping -c 1 www.google.com")
        except Exception as e:
            self.fail("Failed to SSH into VM - %s, %s" %
                      (public_ip_3.ipaddress.ipaddress, e))

        result = str(res)
        self.debug("Result: %s" % result)
        self.assertEqual(
            result.count("1 received"),
            1,
            "Ping to outside world from VM should be successful"
        )
        self.debug("Deleting the account")
        self.account.delete(self.apiclient)

        self.debug("Waiting for account to cleanup")

        interval = list_configurations(
            self.apiclient,
            name='account.cleanup.interval'
        )
        # Sleep to ensure that all resources are deleted
        time.sleep(int(interval[0].value))

        self.debug("Checking if VPC is deleted after account deletion")
        vpcs = VPC.list(
            self.apiclient,
            id=vpc.id,
            listall=True
        )
        self.assertEqual(
            vpcs,
            None,
            "List VPC should not return any response"
        )
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_09_vpc_create(self):
        """ Test to create vpc and verify VPC state, VR and SourceNatIP
        """

        # Validate the following:
        # 1. VPC should get created with "Enabled" state.
        # 2. The VR should start when VPC is created.
        # 3. SourceNatIP address should be allocated to the VR

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("creating a VPC network in the account: %s" %
                   self.account.name)
        vpc = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.validate_vpc_network(vpc)

        self.debug("Verify if the VPC was created with enabled state")
        self.assertEqual(
            vpc.state,
            'Enabled',
            "VPC after creation should be in enabled state but the "
            "state is %s" % vpc.state
        )

        self.debug("Verify if the Router has started")
        routers = Router.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "List Routers should return a valid list"
        )
        self.assertEqual(routers[0].state,
                         'Running',
                         "Router should be in running state"
                         )

        src_nat_list = PublicIPAddress.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True,
            issourcenat=True,
            vpcid=vpc.id
        )
        self.assertEqual(src_nat_list[0].ipaddress,
                         routers[0].publicip,
                         "Source Nat IP address was not allocated to VR"
                         )

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_10_nonoverlaping_cidrs(self):
        """ Test creation of multiple VPCs with non-overlapping CIDRs
        """

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("Creating a VPC network in the account: %s" %
                   self.account.name)
        vpc_1 = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.validate_vpc_network(vpc_1)

        self.services["vpc"]["cidr"] = "10.2.1.1/16"
        self.debug(
            "Creating a non-overlapping VPC network in the account: %s" %
            self.account.name)
        vpc_2 = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.validate_vpc_network(vpc_2)

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("Creating a overlapping VPC network in the account: %s" %
                   self.account.name)
        try:
            vpc_3 = VPC.create(
                self.apiclient,
                self.services["vpc"],
                vpcofferingid=self.vpc_off.id,
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid
            )
            self.debug("%s" % vpc_3)
        except Exception as e:
            self.debug("%s" % e)
            pass
        else:
            assert("VPC created with overlapping CIDR")
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_11_deploy_vm_wo_network_netdomain(self):
        """ Test deployment of vm in a VPC without network domain
        """

        # 1. Create VPC without providing networkDomain.
        # 2. Add network without networkDomain to this VPC.
        # 3. Deploy VM in this network.

        if self.zone.domain is None:
            cmd = updateZone.updateZoneCmd()
            cmd.id = self.zone.id
            cmd.domain = "test.domain.org"
            self.apiclient.updateZone(cmd)
            self.zone = Zone.list(self.apiclient, id=self.zone.id)[0]

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("creating a VPC network in the account: %s" %
                   self.account.name)

        vpc = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.validate_vpc_network(vpc)
        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

        gateway = vpc.cidr.split('/')[0]
        # Split the cidr to retrieve gateway
        # for eg. cidr = 10.0.0.1/24
        # Gateway = 10.0.0.1

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        network = Network.create(
            self.apiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id,
            gateway=gateway,
            vpcid=vpc.id,
        )
        self.debug("Created network with ID: %s" % network.id)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(network.id)]
        )
        self.debug("Deployed VM in network: %s" % network.id)

        self.validate_vm_netdomain(
            virtual_machine,
            vpc,
            network,
            self.zone.domain)

    def validate_vm_netdomain(self, vm, vpc, network, expected_netdomain):

        self.debug("Associating public IP for network: %s" % network.name)
        src_nat_ip_addr = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkid=network.id,
            vpcid=vpc.id
        )

        self.debug("Associated %s with network %s" % (
            src_nat_ip_addr.ipaddress.ipaddress,
            network.id
        ))

        self.debug("Public IP %s" % src_nat_ip_addr.__dict__)

        # Create NAT rule
        nat_rule = NATRule.create(
            self.apiclient,
            vm,
            self.services["natrule"],
            src_nat_ip_addr.ipaddress.id,
            openfirewall=False,
            networkid=network.id,
            vpcid=vpc.id
        )

        list_nat_rule_response = NATRule.list(
            self.apiclient,
            id=nat_rule.id
        )
        self.assertEqual(
            isinstance(list_nat_rule_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_nat_rule_response),
            0,
            "Check Port Forwarding Rule is created"
        )
        self.assertEqual(
            list_nat_rule_response[0].id,
            nat_rule.id,
            "Check Correct Port forwarding Rule is returned"
        )

        self.debug("Adding NetworkACl rules to make NAT rule accessible")
        NetworkACL.create(
            self.apiclient,
            networkid=network.id,
            services=self.services["natrule"],
            traffictype='Ingress'
        )

        self.debug("SSHing into VM with IP address %s with NAT IP %s" %
                   (
                       vm.ipaddress,
                       src_nat_ip_addr.ipaddress.ipaddress))
        try:
            ssh_1 = vm.get_ssh_client(
                ipaddress=src_nat_ip_addr.ipaddress.ipaddress)
            self.debug("SSH into VM is successfully")

            # Ping to outsite world
            res = ssh_1.execute("cat /etc/resolv.conf")

        except Exception as e:
            self.fail("Failed to SSH into VM - %s, %s" %
                      (vm.ssh_ip, e))
        vm_domain = res[1].split(" ")[1]
        self.assertEqual(
            vm_domain,
            expected_netdomain,
            "The network domain assigned to virtual machine "
            "is %s expected domain was %s" %
            (vm_domain, expected_netdomain)
        )

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_12_deploy_vm_with_netdomain(self):
        """ Test deployment of vm in a VPC with network domain
        """

        # 1. Create VPC without providing networkDomain.
        # 2. Add network with networkDomain to this VPC.
        # 3. It should fail.

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("creating a VPC network in the account: %s" %
                   self.account.name)
        vpc = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.validate_vpc_network(vpc)

        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

        gateway = vpc.cidr.split('/')[0]
        # Split the cidr to retrieve gateway
        # for eg. cidr = 10.0.0.1/24
        # Gateway = 10.0.0.1

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)

        # Creation of network with different network domain than the one
        # specified in VPC should fail.
        with self.assertRaises(Exception):
            Network.create(
                self.apiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=self.network_offering.id,
                zoneid=self.zone.id,
                gateway=gateway,
                vpcid=vpc.id,
                networkdomain='test.netdomain'
            )

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_13_deploy_vm_with_vpc_netdomain(self):
        """ Test deployment of vm in a VPC with network domain
        """

        # 1. Create VPC with providing networkDomain.
        # 2. Add network without networkDomain to this VPC.
        # 3. Deploy VM in this network, it should get VPC netdomain

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("creating a VPC network in the account: %s" %
                   self.account.name)
        netdomain = "cl2.internal"
        vpc = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            networkDomain=netdomain
        )
        self.validate_vpc_network(vpc)
        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

        gateway = vpc.cidr.split('/')[0]
        # Split the cidr to retrieve gateway
        # for eg. cidr = 10.0.0.1/24
        # Gateway = 10.0.0.1

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        network = Network.create(
            self.apiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id,
            gateway=gateway,
            vpcid=vpc.id,
        )
        self.debug("Created network with ID: %s" % network.id)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(network.id)]
        )
        self.debug("Deployed VM in network: %s" % network.id)

        self.validate_vm_netdomain(virtual_machine, vpc, network, netdomain)

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_14_deploy_vm_1(self):
        """ Test vm deploy in network by a user where VPC was created
            without account/domain ID
        """

        # 1. Create VPC without providing account/domain ID.
        # 2. Add network with using user account to this VPC.
        # 3. Deploy VM in this network

        user = Account.create(
            self.apiclient,
            self.services["account"]
        )
        self.debug("Created account: %s" % user.name)
        self.cleanup.append(user)

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("creating a VPC network in the account: %s" %
                   user.name)

        userapiclient = self.testClient.getUserApiClient(
            UserName=user.name,
            DomainName=user.domain,
            type=0)

        vpc = VPC.create(
            userapiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
        )
        self.validate_vpc_network(vpc)

        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

        gateway = vpc.cidr.split('/')[0]
        # Split the cidr to retrieve gateway
        # for eg. cidr = 10.0.0.1/24
        # Gateway = 10.0.0.1

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        network = Network.create(
            userapiclient,
            self.services["network"],
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id,
            gateway=gateway,
            vpcid=vpc.id
        )
        self.debug("Created network with ID: %s" % network.id)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
            userapiclient,
            self.services["virtual_machine"],
            serviceofferingid=self.service_offering.id,
            networkids=[str(network.id)]
        )
        self.debug("Deployed VM in network: %s" % network.id)

        self.assertNotEqual(virtual_machine,
                            None,
                            "VM creation in the network failed")

        return

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_15_deploy_vm_2(self):
        """ Test deployment of vm in a network in a domain admin
        account where VPC is created without account/domain ID
        """

        # 1. Create VPC without providing account/domain ID.
        # 2. Add network with using domain admin account to this VPC.
        # 3. Deploy VM in this network

        user = Account.create(
            self.apiclient,
            self.services["account"]
        )
        self.debug("Created account: %s" % user.name)
        self.cleanup.append(user)

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("creating a VPC network in the account: %s" %
                   user.name)

        # 0 - User, 1 - Root Admin, 2 - Domain Admin
        userapiclient = self.testClient.getUserApiClient(
            UserName=user.name,
            DomainName=self.services["domain"]["name"],
            type=2)

        vpc = VPC.create(
            userapiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
        )
        self.validate_vpc_network(vpc)

        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

        gateway = vpc.cidr.split('/')[0]
        # Split the cidr to retrieve gateway
        # for eg. cidr = 10.0.0.1/24
        # Gateway = 10.0.0.1

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        network = Network.create(
            userapiclient,
            self.services["network"],
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id,
            gateway=gateway,
            vpcid=vpc.id
        )
        self.debug("Created network with ID: %s" % network.id)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
            userapiclient,
            self.services["virtual_machine"],
            serviceofferingid=self.service_offering.id,
            networkids=[str(network.id)]
        )
        self.debug("Deployed VM in network: %s" % network.id)

        self.assertNotEqual(virtual_machine,
                            None,
                            "VM creation in the network failed")

        return

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_16_deploy_vm_for_user_by_admin(self):
        """ Test deployment of vm in a network by root admin for user.
        """

        # 1. As root admin account ,
        #    Create VPC(name,zoneId,cidr,vpcOfferingId,networkDomain by
        #    passing user Account/domain ID.
        # 2. As the user account used in step1 , create a network as part
        #    of this VPC.
        # 3. Deploy Vms as part of this network.
        user = Account.create(
            self.apiclient,
            self.services["account"]
        )
        self.debug("Created account: %s" % user.name)
        self.cleanup.append(user)

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("creating a VPC network in the account: %s" %
                   user.name)

        userapiclient = self.testClient.getUserApiClient(
            UserName=user.name,
            DomainName=user.domain,
            type=0)

        vpc = VPC.create(
            self.apiclient,
            self.services["vpc"],
            account=user.name,
            domainid=user.domainid,
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
        )
        self.validate_vpc_network(vpc)

        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

        gateway = vpc.cidr.split('/')[0]
        # Split the cidr to retrieve gateway
        # for eg. cidr = 10.0.0.1/24
        # Gateway = 10.0.0.1

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        network = Network.create(
            userapiclient,
            self.services["network"],
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id,
            gateway=gateway,
            vpcid=vpc.id
        )
        self.debug("Created network with ID: %s" % network.id)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
            userapiclient,
            self.services["virtual_machine"],
            serviceofferingid=self.service_offering.id,
            networkids=[str(network.id)]
        )
        self.debug("Deployed VM in network: %s" % network.id)

        self.assertNotEqual(virtual_machine,
                            None,
                            "VM creation in the network failed")

        return

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_17_deploy_vm_for_user_by_domain_admin(self):
        """ Test deployment of vm in a network by domain admin for user.
        """

        # 1. As domain admin account , Create
        #   VPC(name,zoneId,cidr,vpcOfferingId,networkDomain
        #   by passing user Account/domain ID.
        # 2. As the user account used in step1, create network as part of
        #    this VPC
        # 3. Deploy Vms as part of this network.

        domain_admin = Account.create(
            self.apiclient,
            self.services["domain_admin"]
        )
        self.debug("Created account: %s" % domain_admin.name)
        self.cleanup.append(domain_admin)
        da_apiclient = self.testClient.getUserApiClient(
            UserName=domain_admin.name,
            DomainName=domain_admin.domain,
            type=2)

        user = Account.create(
            self.apiclient,
            self.services["account"]
        )
        self.debug("Created account: %s" % user.name)
        self.cleanup.append(user)

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("creating a VPC network in the account: %s" %
                   user.name)

        # 0 - User, 1 - Root Admin, 2 - Domain Admin
        self.testClient.getUserApiClient(
            UserName=user.name,
            DomainName=user.domain,
            type=0)

        with self.assertRaises(CloudstackAPIException):
            VPC.create(
                da_apiclient,
                self.services["vpc"],
                account=user.name,
                domainid=user.domainid,
                vpcofferingid=self.vpc_off.id,
                zoneid=self.zone.id,
            )

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_18_create_net_for_user_diff_domain_by_doadmin(self):
        """ Test creation of network by domain admin for user from different domain
        """

        # 1. As domain admin account , Create VPC(name,zoneId,cidr,
        #    vpcOfferingId,networkDomain) without passing Account/domain ID.
        # 2. As any User account that is not under this domain , create a
        #    network as part of this VPC.

        domain_admin = Account.create(
            self.apiclient,
            self.services["domain_admin"]
        )
        self.debug("Created account: %s" % domain_admin.name)
        self.cleanup.append(domain_admin)
        da_apiclient = self.testClient.getUserApiClient(
            UserName=domain_admin.name,
            DomainName=self.services["domain"]["name"],
            type=2)

        user = Account.create(
            self.apiclient,
            self.services["account"]
        )
        self.debug("Created account: %s" % user.name)
        self.cleanup.append(user)

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("creating a VPC network in the account: %s" %
                   user.name)

        # 0 - User, 1 - Root Admin, 2 - Domain Admin
        userapiclient = self.testClient.getUserApiClient(
            UserName=user.name,
            DomainName=user.domain,
            type=0)

        vpc = VPC.create(
            da_apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
        )
        self.validate_vpc_network(vpc)

        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

        gateway = vpc.cidr.split('/')[0]
        # Split the cidr to retrieve gateway
        # for eg. cidr = 10.0.0.1/24
        # Gateway = 10.0.0.1

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)

        with self.assertRaises(Exception):
            Network.create(
                userapiclient,
                self.services["network"],
                networkofferingid=self.network_offering.id,
                zoneid=self.zone.id,
                gateway=gateway,
                vpcid=vpc.id
            )

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_19_create_vpc_wo_params(self):
        """ Test creation of VPC without mandatory parameters
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Delete VPC. Delete VPC should be successful

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("creating a VPC network in the account: %s" %
                   self.account.name)

        # Create VPC without vpcOffering param
        with self.assertRaises(Exception):
            VPC.create(
                self.apiclient,
                self.services["vpc"],
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid
            )

        self.services["vpc_no_name"]["cidr"] = "10.1.1.1/16"
        # Create VPC without name param
        with self.assertRaises(Exception):
            VPC.create(
                self.apiclient,
                self.services["vpc_no_name"],
                vpcofferingid=self.vpc_off.id,
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid
            )

        # Create VPC without zoneid param
        with self.assertRaises(Exception):
            VPC.create(
                self.apiclient,
                self.services["vpc"],
                vpcofferingid=self.vpc_off.id,
                account=self.account.name,
                domainid=self.account.domainid
            )

        vpc_wo_cidr = {"name": "TestVPC_WO_CIDR",
                       "displaytext": "TestVPC_WO_CIDR"
                       }

        # Create VPC without CIDR
        with self.assertRaises(Exception):
            VPC.create(
                self.apiclient,
                vpc_wo_cidr,
                vpcofferingid=self.vpc_off.id,
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid
            )

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_20_update_vpc_name_display_text(self):
        """ Test to verify updation of vpc name and display text
        """

        # Validate the following:
        # 1. VPC should get created with "Enabled" state.
        # 2. The VR should start when VPC is created.
        # 3. SourceNatIP address should be allocated to the VR

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("creating a VPC network in the account: %s" %
                   self.account.name)
        vpc = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.validate_vpc_network(vpc)

        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

        gateway = vpc.cidr.split('/')[0]
        # Split the cidr to retrieve gateway
        # for eg. cidr = 10.0.0.1/24
        # Gateway = 10.0.0.1

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        network = Network.create(
            self.apiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id,
            gateway=gateway,
            vpcid=vpc.id
        )
        self.debug("Created network with ID: %s" % network.id)

        new_name = "New VPC"
        new_display_text = "New display text"
        vpc.update(
            self.apiclient,
            name=new_name,
            displaytext=new_display_text
        )

        vpc_networks = VPC.list(
            self.apiclient,
            id=vpc.id
        )

        self.assertEqual(
            isinstance(vpc_networks, list),
            True,
            "List VPC network should return a valid list"
        )
        self.assertEqual(vpc_networks[0].name,
                         new_name,
                         "Updation of VPC name failed.")

        self.assertEqual(vpc_networks[0].displaytext,
                         new_display_text,
                         "Updation of VPC display text failed.")

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_21_deploy_vm_with_gateway_ip(self):
        self.services["vpc"]["cidr"] = "192.168.1.0/24"
        self.debug("creating a VPC network in the account: %s" %
                   self.account.name)
        vpc = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.validate_vpc_network(vpc)
        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)
        #Instead of first ip, assigning last ip in the CIDR as the gateway ip
        gateway = "192.168.1.2"
        self.services["network"]["netmask"] = "255.255.255.252"
        # Split the cidr to retrieve gateway
        # for eg. cidr = 10.0.0.1/24
        # Gateway = 10.0.0.1

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        network = Network.create(
            self.apiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id,
            gateway=gateway,
            vpcid=vpc.id
        )
        self.debug("Created network with ID: %s" % network.id)
        vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(network.id)]
        )
        self.debug("Deployed VM in network: %s" % network.id)
        self.assertIsNotNone(
            vm,
            "Failed to create VM with first ip address in the CIDR as the vm ip"
        )
        return
