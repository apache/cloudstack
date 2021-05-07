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

""" Component tests for VPC network functionality - with and without Netscaler (Netscaler tests will be skipped if Netscaler configuration fails)
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.cloudstackAPI import startVirtualMachine, stopVirtualMachine
from marvin.lib.utils import cleanup_resources, validateList
from marvin.lib.base import (VirtualMachine,
                             ServiceOffering,
                             Account,
                             NATRule,
                             NetworkOffering,
                             Network,
                             VPC,
                             VpcOffering,
                             LoadBalancerRule,
                             Router,
                             StaticNATRule,
                             NetworkACL,
                             PublicIPAddress)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               wait_for_cleanup,
                               add_netscaler,
                               list_networks,
                               verifyRouterState)
# For more info on ddt refer to
# http://ddt.readthedocs.org/en/latest/api.html#module-ddt
from ddt import ddt, data
import time
from marvin.codes import PASS


class Services:

    """Test VPC network services
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
                "serviceCapabilityList": {
                    "SourceNat": {"SupportedSourceNatTypes": "peraccount"},
                },
            },
            # Offering that uses Netscaler as provider for LB inside VPC,
            # dedicated = false
            "network_off_netscaler": {
                "name": 'Network offering-netscaler',
                "displaytext": 'Network offering-netscaler',
                "guestiptype": 'Isolated',
                "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Lb,UserData,StaticNat',
                "traffictype": 'GUEST',
                "availability": 'Optional',
                "useVpc": 'on',
                "serviceProviderList": {
                    "Dhcp": 'VpcVirtualRouter',
                    "Dns": 'VpcVirtualRouter',
                    "SourceNat": 'VpcVirtualRouter',
                    "PortForwarding": 'VpcVirtualRouter',
                    "Vpn": 'VpcVirtualRouter',
                    "Lb": 'Netscaler',
                    "UserData": 'VpcVirtualRouter',
                    "StaticNat": 'VpcVirtualRouter',
                },
                "serviceCapabilityList": {
                    "SourceNat": {"SupportedSourceNatTypes": "peraccount"},
                },
            },
            # Offering that uses Netscaler as provider for LB in VPC, dedicated = True
            # This offering is required for the tests that use Netscaler as external LB provider in VPC
            "network_offering_vpcns": {
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
                    "Lb": 'Netscaler',
                    "UserData": 'VpcVirtualRouter',
                    "StaticNat": 'VpcVirtualRouter',
                    "NetworkACL": 'VpcVirtualRouter'
                },
                "serviceCapabilityList": {
                    "SourceNat": {
                        "SupportedSourceNatTypes": "peraccount"
                    },
                    "lb": {
                        "SupportedLbIsolation": "dedicated"
                    },
                },
            },

            "network_off_shared": {
                "name": 'Shared Network offering',
                "displaytext": 'Shared Network offering',
                "guestiptype": 'Shared',
                "traffictype": 'GUEST',
                "availability": 'Optional',
                "useVpc": 'on',
                "specifyIpRanges": True,
                "specifyVlan": True
            },
            "vpc_offering": {
                "name": 'VPC off',
                "displaytext": 'VPC off',
                "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Lb,UserData,StaticNat',
            },
            "vpc": {
                "name": "TestVPC",
                "displaytext": "TestVPC",
                "cidr": '10.0.0.1/24'
            },
            # Netscaler should be added as a dedicated device for it to work as external LB provider in VPC
            "netscaler": {
                "ipaddress": '10.102.192.50',
                "username": 'nsroot',
                "password": 'nsroot',
                "networkdevicetype": 'NetscalerVPXLoadBalancer',
                "publicinterface": '1/3',
                "privateinterface": '1/4',
                "numretries": 2,
                "lbdevicededicated": True,
                "lbdevicecapacity": 50,
                "port": 22,
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
            "ostype": 'CentOS 5.3 (64-bit)',
            # Cent OS 5.3 (64 bit)
            "sleep": 60,
            "timeout": 10,
        }


@ddt
class TestVPCNetwork(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVPCNetwork, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services

        # Added an attribute to track if Netscaler addition was successful.
        # Value is checked in tests and if not configured, Netscaler tests will
        # be skipped
        cls.ns_configured = False
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

        cls._cleanup = []
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        # Configure Netscaler device
        # If configuration succeeds, set ns_configured to True so that
        # Netscaler tests are executed
        try:
            cls.netscaler = add_netscaler(
                cls.api_client,
                cls.zone.id,
                cls.services["netscaler"])
            cls._cleanup.append(cls.netscaler)
            cls.debug("Netscaler configured")
            cls.ns_configured = True
        except Exception as e:
            cls.debug("Warning: Couldn't configure Netscaler: %s" % e)
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
        self.services = Services().services
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup = [self.account, ]
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

    @data("network_offering", "network_offering_vpcns")
    @attr(tags=["advanced", "intervlan"])
    def test_01_create_network(self, value):
        """ Test create network in VPC
        """

        # Validate the following
        # 1. Create a VPC using Default Offering
        # 2. Create a network offering with guest type=Isolated" that has
        #    all of supported Services(Vpn,dhcpdns,UserData, SourceNat,Static
        #    NAT,LB and PF,LB,NetworkAcl ) provided by VPCVR and conserve
        #    mode is ON
        # 3. Create a network tier using the network offering created in
        #    step 2 as part of this VPC.
        # 4. Validate Network is created
        # 5. Repeat test for offering which has Netscaler as external LB
        # provider

        if (value == "network_offering_vpcns" and not self.ns_configured):
            self.skipTest('Netscaler not configured: skipping test')

        if (value == "network_offering"):
            vpc_off_list = VpcOffering.list(
                self.apiclient,
                name='Default VPC offering',
                listall=True
            )
        else:
            vpc_off_list = VpcOffering.list(
                self.apiclient,
                name='Default VPC  offering with Netscaler',
                listall=True
            )
        vpc_off = vpc_off_list[0]
        self.debug("Creating a VPC with offering: %s" % vpc_off.id)

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
            self.services[value],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

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
            gateway='10.1.1.1',
            vpcid=vpc.id
        )
        self.debug("Created network with ID: %s" % network.id)
        self.debug(
            "Verifying list network response to check if network created?")
        networks = Network.list(
            self.apiclient,
            id=network.id,
            listall=True
        )
        self.assertEqual(
            isinstance(networks, list),
            True,
            "List networks should return a valid response"
        )
        nw = networks[0]

        self.assertEqual(
            nw.networkofferingid,
            self.network_offering.id,
            "Network should be created from network offering - %s" %
            self.network_offering.id
        )
        self.assertEqual(
            nw.vpcid,
            vpc.id,
            "Network should be created in VPC: %s" % vpc.name
        )
        return

    @data("network_offering", "network_offering_vpcns")
    @attr(tags=["advanced", "intervlan"])
    def test_02_create_network_fail(self, value):
        """ Test create network in VPC mismatched services (Should fail)
        """

        # Validate the following
        # 1. Create a VPC using Default VPC Offering
        # 2. Create a network offering with guest type=Isolated" that has
        #    one of supported Services(Vpn,dhcpdns,UserData, Static
        #    NAT,LB and PF,LB,NetworkAcl ) provided by VPCVR, SourceNat by VR
        #    and conserve mode is ON
        # 3. Create a network using the network offering created in step2 as
        #    part of this VPC.
        # 4. Network creation should fail since SourceNat offered by VR
        #    instead of VPCVR
        # 5. Repeat test for offering which has Netscaler as external LB
        #    provider

        if (value == "network_offering_vpcns" and not self.ns_configured):
            self.skipTest('Netscaler not configured: skipping test')

        if (value == "network_offering"):
            vpc_off_list = VpcOffering.list(
                self.apiclient,
                name='Default VPC offering',
                listall=True
            )
        else:
            vpc_off_list = VpcOffering.list(
                self.apiclient,
                name='Default VPC  offering with Netscaler',
                listall=True
            )
        vpc_off = vpc_off_list[0]
        self.debug("Creating a VPC with offering: %s" % vpc_off.id)

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

        self.services[value]["serviceProviderList"] = {
            "SourceNat": 'VirtualRouter', }

        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services[value],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        with self.assertRaises(Exception):
            Network.create(
                self.apiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=self.network_offering.id,
                zoneid=self.zone.id,
                gateway='10.1.1.1',
                vpcid=vpc.id
            )
        return

    @data("network_offering", "network_offering_vpcns")
    @attr(tags=["advanced", "intervlan"])
    def test_04_create_multiple_networks_with_lb(self, value):
        """ Test create multiple networks with LB service (Should fail)
        """
        self.skipTest('Skipping test due to CLOUDSTACK-8437')
        # Validate the following
        # 1. Create a VPC using Default Offering
        # 2. Create a network offering with guest type=Isolated that has LB
        #    services Enabled and conserve mode is "ON".
        # 3. Create a network using the network offering created in step2 as
        #    part of this VPC.
        # 4. Create another network using the network offering created in
        #    step3 as part of this VPC
        # 5. Create Network should fail
        # 6. Repeat test for offering which has Netscaler as external LB
        # provider
        if (value == "network_offering_vpcns" and not self.ns_configured):
            self.skipTest('Netscaler not configured: skipping test')

        if (value == "network_offering"):
            vpc_off_list = VpcOffering.list(
                self.apiclient,
                name='Default VPC offering',
                listall=True
            )
        else:
            vpc_off_list = VpcOffering.list(
                self.apiclient,
                name='Default VPC  offering with Netscaler',
                listall=True
            )
        vpc_off = vpc_off_list[0]
        self.debug("Creating a VPC with offering: %s" % vpc_off.id)

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
            self.services[value],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

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
            gateway='10.1.1.1',
            vpcid=vpc.id
        )
        self.debug("Created network with ID: %s" % network.id)
        self.debug(
            "Verifying list network response to check if network created?")
        networks = Network.list(
            self.apiclient,
            id=network.id,
            listall=True
        )
        self.assertEqual(
            isinstance(networks, list),
            True,
            "List networks should return a valid response"
        )
        nw = networks[0]

        self.assertEqual(
            nw.networkofferingid,
            self.network_offering.id,
            "Network should be created from network offering - %s" %
            self.network_offering.id
        )
        self.assertEqual(
            nw.vpcid,
            vpc.id,
            "Network should be created in VPC: %s" % vpc.name
        )
        self.debug("Creating another network in VPC: %s" % vpc.name)
        with self.assertRaises(Exception):
            Network.create(
                self.apiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=self.network_offering.id,
                zoneid=self.zone.id,
                gateway='10.1.2.1',
                vpcid=vpc.id
            )
        self.debug(
            "Network creation failed as network with LB service\
                    already exists")
        return

    @attr(tags=["intervlan"])
    def test_05_create_network_ext_LB(self):
        """ Test create network with external LB devices
        """

        # Validate the following
        # 1.Create a VPC using Default Offering (Without Netscaler)
        # 2. Create a network offering with guest type=Isolated that has LB
        #    service provided by netscaler and conserve mode is "ON".
        # 3. Create a network using this network offering as part of this VPC.
        # 4. Create Network should fail since it doesn't match the VPC offering

        vpc_off_list = VpcOffering.list(
            self.apiclient,
            name='Default VPC offering',
            listall=True
        )
        vpc_off = vpc_off_list[0]
        self.debug("Creating a VPC with offering: %s" % vpc_off.id)

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
            self.services["network_offering_vpcns"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        with self.assertRaises(Exception):
            Network.create(
                self.apiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=self.network_offering.id,
                zoneid=self.zone.id,
                gateway='10.1.1.1',
                vpcid=vpc.id
            )
        self.debug("Network creation failed")
        return

    @unittest.skip("skipped - RvR didn't support VPC currently ")
    @attr(tags=["advanced", "intervlan", "NA"])
    def test_06_create_network_with_rvr(self):
        """ Test create network with redundant router capability
        """

        # Validate the following
        # 1. Create VPC Offering by specifying all supported Services
        #    (Vpn,dhcpdns,UserData, SourceNat,Static NAT and PF,LB,NetworkAcl)
        # 2. Create a VPC using the above VPC offering
        # 3. Create a network offering with guest type=Isolated that has all
        #    services provided by VPC VR,conserver mode ""OFF"" and Redundant
        #    Router capability enabled.
        # 4. Create a VPC using the above VPC offering.
        # 5. Create a network using the network offering created in step2 as
        #    part of this VPC

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

        # Enable redundant router capability for the network offering
        self.services["network"]["serviceCapabilityList"] = {
            "SourceNat": {
                "RedundantRouter": "true",
            },
        }

        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        with self.assertRaises(Exception):
            Network.create(
                self.apiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=self.network_offering.id,
                zoneid=self.zone.id,
                gateway='10.1.2.1',
                vpcid=vpc.id
            )
        self.debug("Network creation failed")
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_07_create_network_unsupported_services(self):
        """ Test create network services not supported by VPC (Should fail)
        """

        # Validate the following
        # 1. Create VPC Offering without LB service
        # 2. Create a VPC using the above VPC offering
        # 3. Create a network offering with guest type=Isolated that has all
        #    supported Services(Vpn,dhcpdns,UserData, SourceNat,Static NAT,LB
        #    and PF,LB,NetworkAcl ) provided by VPCVR and conserve mode is OFF
        # 4. Create Network with the above offering
        # 5. Create network fails since VPC offering doesn't support LB

        self.debug("Creating a VPC offering without LB service")
        self.services["vpc_offering"][
            "supportedservices"] = 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,UserData,StaticNat'

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

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        with self.assertRaises(Exception):
            Network.create(
                self.apiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=self.network_offering.id,
                zoneid=self.zone.id,
                gateway='10.1.2.1',
                vpcid=vpc.id
            )
        self.debug("Network creation failed as VPC doesn't have LB service")
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_08_create_network_without_sourceNAT(self):
        """ Test create network without sourceNAT service in VPC (should fail)
        """

        # Validate the following
        # 1. Create VPC Offering by specifying supported Services-
        #    Vpn,dhcpdns,UserData, SourceNat,Static NAT and PF,LB,NetworkAcl)
        #    with out including LB services.
        # 2. Create a VPC using the above VPC offering
        # 3. Create a network offering with guest type=Isolated that does not
        #    have SourceNAT services enabled
        # 4. Create a VPC using the above VPC offering
        # 5. Create a network using the network offering created in step2 as
        #    part of this VPC

        self.debug("Creating a VPC offering without LB service")
        self.services["vpc_offering"][
            "supportedservices"] = 'Dhcp,Dns,SourceNat,PortForwarding,UserData,StaticNat'

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

        self.debug("Creating network offering without SourceNAT service")
        self.services["network_offering"][
            "supportedservices"] = 'Dhcp,Dns,PortForwarding,Lb,UserData,StaticNat,NetworkACL'
        self.services["network_offering"]["serviceProviderList"] = {
            "Dhcp": 'VpcVirtualRouter',
            "Dns": 'VpcVirtualRouter',
            "PortForwarding": 'VpcVirtualRouter',
            "Lb": 'VpcVirtualRouter',
            "UserData": 'VpcVirtualRouter',
            "StaticNat": 'VpcVirtualRouter',
            "NetworkACL": 'VpcVirtualRouter'
        }

        self.debug("Creating network offering without SourceNAT")
        with self.assertRaises(Exception):
            NetworkOffering.create(
                self.apiclient,
                self.services["network_offering"],
                conservemode=False
            )
        self.debug("Network creation failed as VPC doesn't have LB service")
        return

    @data("network_off_shared", "network_offering_vpcns")
    @attr(tags=["advanced", "intervlan"])
    def test_09_create_network_shared_nwoff(self, value):
        """ Test create network with shared network offering
        """

        # Validate the following
        # 1. Create VPC Offering using Default Offering
        # 2. Create a VPC using the above VPC offering
        # 3. Create a network offering with guest type=shared
        # 4. Create a network using the network offering created
        #    in step3 as part of this VPC
        # 5. Create network fails since it using shared offering
        # 6. Repeat test for offering which has Netscaler as external LB
        # provider

        if (value == "network_offering_vpcns" and not self.ns_configured):
            self.skipTest('Netscaler not configured: skipping test')

        if (value == "network_off_shared"):
            vpc_off_list = VpcOffering.list(
                self.apiclient,
                name='Default VPC offering',
                listall=True
            )
        else:
            vpc_off_list = VpcOffering.list(
                self.apiclient,
                name='Default VPC  offering with Netscaler',
                listall=True
            )
        vpc_off = vpc_off_list[0]
        self.debug("Creating a VPC with offering: %s" % vpc_off.id)

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

        self.debug("Creating network offering with guesttype=shared")

        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_off_shared"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

        # Creating network using the network offering created
        self.debug(
            "Creating network with network offering without SourceNAT: %s" %
            self.network_offering.id)
        with self.assertRaises(Exception):
            Network.create(
                self.apiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=self.network_offering.id,
                zoneid=self.zone.id,
                gateway='10.1.1.1',
                vpcid=vpc.id
            )
        self.debug("Network creation failed")
        return

    @data("network_offering", "network_offering_vpcns")
    @attr(tags=["advanced", "intervlan"])
    def test_10_create_network_with_conserve_mode(self, value):
        """ Test create network with conserve mode ON
        """

        # Validate the following
        # 1. Create a network offering with guest type=Isolated that has all
        #    supported Services(Vpn,dhcpdns,UserData, SourceNat,Static NAT,LB
        #    and PF,LB,NetworkAcl ) provided by VPCVR and conserve mode is ON
        # 2. Create offering fails since Conserve mode ON isn't allowed within
        #    VPC
        # 3. Repeat test for offering which has Netscaler as external LB
        #    provider

        self.debug("Creating network offering with conserve mode = ON")

        with self.assertRaises(Exception):
            NetworkOffering.create(
                self.apiclient,
                self.services[value],
                conservemode=True
            )
        self.debug(
            "Network creation failed as VPC support nw with conserve mode OFF")
        return


@ddt
class TestVPCNetworkRanges(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVPCNetworkRanges, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services

        # Added an attribute to track if Netscaler addition was successful.
        # Value is checked in tests and if not configured, Netscaler tests will
        # be skipped
        cls.ns_configured = False
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

        cls._cleanup = []
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        # Configure Netscaler device
        # If configuration succeeds, set ns_configured to True so that
        # Netscaler tests are executed
        try:
            cls.netscaler = add_netscaler(
                cls.api_client,
                cls.zone.id,
                cls.services["netscaler"])
            cls._cleanup.append(cls.netscaler)
            cls.ns_configured = True
        except Exception as e:
            cls.debug("Warning: Couldn't configure Netscaler: %s" % e)
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
        self.cleanup = [self.account, ]
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

    @data("network_offering", "network_offering_vpcns")
    @attr(tags=["advanced", "intervlan"])
    def test_01_create_network_outside_range(self, value):
        """ Test create network outside cidr range of VPC
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1 with cidr - 10.2.1.1/24  to this VPC
        # 3. Network creation should fail.
        # 4. Repeat test for offering which has Netscaler as external LB
        # provider

        if (value == "network_offering_vpcns" and not self.ns_configured):
            self.skipTest('Netscaler not configured: skipping test')

        if (value == "network_offering"):
            vpc_off_list = VpcOffering.list(
                self.apiclient,
                name='Default VPC offering',
                listall=True
            )
        else:
            vpc_off_list = VpcOffering.list(
                self.apiclient,
                name='Default VPC  offering with Netscaler',
                listall=True
            )
        vpc_off = vpc_off_list[0]
        self.debug("Creating a VPC with offering: %s" % vpc_off.id)

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

        self.debug("Creating network offering")

        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services[value],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

        # Creating network using the network offering created
        self.debug("Creating network outside of the VPC's network")
        with self.assertRaises(Exception):
            Network.create(
                self.apiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=self.network_offering.id,
                zoneid=self.zone.id,
                gateway='10.2.1.1',
                vpcid=vpc.id
            )
        self.debug(
            "Network creation failed as network cidr range is outside of vpc")
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_02_create_network_outside_range(self):
        """ Test create network outside cidr range of VPC
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1 with cidr - 10.2.1.1/24  to this VPC
        # 3. Network creation should fail.
        # 4. Repeat test for offering which has Netscaler as external LB
        # provider

        self.debug("Creating a VPC offering")
        vpc_off = VpcOffering.create(
            self.apiclient,
            self.services["vpc_offering"]
        )

        self.cleanup.append(vpc_off)
        self.validate_vpc_offering(vpc_off)

        self.debug("Enabling the VPC offering created")
        vpc_off.update(self.apiclient, state='Enabled')

        self.debug("creating a VPC network with cidr: 10.1.1.1/16")
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

        self.debug("Creating network offering")

        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

        # Creating network using the network offering created
        self.debug("Creating network outside of the VPC's network")
        with self.assertRaises(Exception):
            Network.create(
                self.apiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=self.network_offering.id,
                zoneid=self.zone.id,
                gateway='10.2.1.1',
                vpcid=vpc.id
            )
        self.debug(
            "Network creation failed as network cidr range is outside of vpc")
        return

    @data("network_offering", "network_offering_vpcns")
    @attr(tags=["advanced", "intervlan"])
    def test_03_create_network_inside_range(self, value):
        """ Test create network inside cidr range of VPC
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1 with cidr - 10.1.1.1/8  to this VPC
        # 3. Network creation should fail.
        # 4. Repeat test for offering which has Netscaler as external LB
        # provider

        if (value == "network_offering_vpcns" and not self.ns_configured):
            self.skipTest('Netscaler not configured: skipping test')

        if (value == "network_offering"):
            vpc_off_list = VpcOffering.list(
                self.apiclient,
                name='Default VPC offering',
                listall=True
            )
        else:
            vpc_off_list = VpcOffering.list(
                self.apiclient,
                name='Default VPC  offering with Netscaler',
                listall=True
            )
        vpc_off = vpc_off_list[0]
        self.debug("Creating a VPC with offering: %s" % vpc_off.id)

        self.debug("creating a VPC network with cidr: 10.1.1.1/16")
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

        self.debug("Creating network offering")

        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services[value],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

        # Creating network using the network offering created
        self.debug("Creating network inside of the VPC's network")
        with self.assertRaises(Exception):

            # cidr = 10.1.1.1/8 -> netmask = 255.0.0.0, gateway = 10.1.1.1
            self.services["network"]["netmask"] = '255.0.0.0'
            Network.create(
                self.apiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=self.network_offering.id,
                zoneid=self.zone.id,
                gateway='10.1.1.1',
                vpcid=vpc.id
            )
        self.debug(
            "Network creation failed as network cidr range is inside of vpc")
        return

    @data("network_offering", "network_offering_vpcns")
    @attr(tags=["advanced", "intervlan"])
    def test_04_create_network_overlapping_range(self, value):
        """ Test create network overlapping cidr range of VPC
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1 with cidr - 10.1.1.1/24  to this VPC
        # 3. Add network2 with cidr - 10.1.1.1/24  to this VPC
        # 4. Add network3 with cidr - 10.1.1.1/26  to this VPC
        # 5. Network creation in step 3 & 4 should fail.
        # 6. Repeat test for offering which has Netscaler as external LB
        # provider

        self.services = Services().services
        if (value == "network_offering_vpcns" and not self.ns_configured):
            self.skipTest('Netscaler not configured: skipping test')

        if (value == "network_offering"):
            vpc_off_list = VpcOffering.list(
                self.apiclient,
                name='Default VPC offering',
                listall=True
            )
        else:
            vpc_off_list = VpcOffering.list(
                self.apiclient,
                name='Default VPC  offering with Netscaler',
                listall=True
            )
        vpc_off = vpc_off_list[0]
        self.debug("Creating a VPC with offering: %s" % vpc_off.id)
        self.debug("creating a VPC network with cidr: 10.1.1.1/16")
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

        self.debug("Creating network offering")

        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services[value],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

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
            gateway='10.1.1.1',
            vpcid=vpc.id
        )
        self.debug("Created network with ID: %s" % network.id)
        self.debug(
            "Verifying list network response to check if network created?")
        networks = Network.list(
            self.apiclient,
            id=network.id,
            listall=True
        )
        self.assertEqual(
            isinstance(networks, list),
            True,
            "List networks should return a valid response"
        )
        nw = networks[0]

        self.assertEqual(
            nw.networkofferingid,
            self.network_offering.id,
            "Network should be created from network offering - %s" %
            self.network_offering.id
        )
        self.assertEqual(
            nw.vpcid,
            vpc.id,
            "Network should be created in VPC: %s" % vpc.name
        )

        # Creating network using the network offering created
        self.debug(
            "Creating network with same network range as of previous network")
        with self.assertRaises(Exception):
            Network.create(
                self.apiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=self.network_offering.id,
                zoneid=self.zone.id,
                gateway='10.1.1.1',
                vpcid=vpc.id
            )
        self.debug("Network creation as network range 10.1.1.1/24 is same" +
                   "as that of existing network")

        self.debug("Creating network having overlapping network ranges")
        with self.assertRaises(Exception):
            # cidr = 10.1.1.1/8 -> netmask=255.255.255.192, gateway=10.1.1.1
            self.services["network"]["netmask"] = '255.255.255.192'

            Network.create(
                self.apiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=self.network_offering.id,
                zoneid=self.zone.id,
                gateway='10.1.1.1',
                vpcid=vpc.id
            )
        self.debug(
            "Network creation failed as network range overlaps each other")
        return

    @data("network_offering", "network_offering_vpcns")
    @attr(tags=["advanced", "intervlan"])
    def test_05_create_network_diff_account(self, value):
        """ Test create network from different account in VPC
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1 with cidr - 10.1.1.1/24  to this VPC
        # 3. Create another account
        # 4. Create network using this account - Network creation should fail
        # 5. Repeat test for offering which has Netscaler as external LB
        # provider

        if (value == "network_offering_vpcns" and not self.ns_configured):
            self.skipTest('Netscaler not configured: skipping test')

        if (value == "network_offering"):
            vpc_off_list = VpcOffering.list(
                self.apiclient,
                name='Default VPC offering',
                listall=True
            )
        else:
            vpc_off_list = VpcOffering.list(
                self.apiclient,
                name='Default VPC  offering with Netscaler',
                listall=True
            )
        vpc_off = vpc_off_list[0]
        self.debug("Creating a VPC with offering: %s" % vpc_off.id)

        self.debug("creating a VPC network with cidr: 10.1.1.1/16")
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

        self.debug("Creating network offering")

        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services[value],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

        self.debug(
            "Creating the new account to create new network in VPC: %s" %
            vpc.name)
        account = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup.append(account)

        # Creating network using the network offering created
        self.debug("Creating network from diff account than VPC")
        with self.assertRaises(Exception):

            # cidr = 10.1.1.1/8 -> netmask = 255.0.0.0, gateway = 10.1.1.1
            self.services["network"]["netmask"] = '255.0.0.0'
            Network.create(
                self.apiclient,
                self.services["network"],
                accountid=account.name,
                domainid=account.domainid,
                networkofferingid=self.network_offering.id,
                zoneid=self.zone.id,
                gateway='10.1.1.1',
                vpcid=vpc.id
            )
        self.debug(
            "Network creation failed as VPC belongs to different account")
        return


class TestVPCNetworkUpgrade(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVPCNetworkUpgrade, cls).getClsTestClient()
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

        cls._cleanup = []
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)

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
        self.cleanup = [self.account, ]
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

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_01_network_services_upgrade(self):
        """ Test update Network that is part of a VPC to a network
            offering that has more services
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        #    except PF services provided by VRVPC provider, conserve mode=OFF
        # 3.Create a Network offering - NO2 with all supported services
        #    including Pf services provided by VRVPC provider,conserve mode=OFF
        # 4. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 5. Deploy vm1 and vm2 in network1.
        # 6. Create a Static Nat and LB rules for vms in network1.
        # 7. Make sure you are not allowed to create a PF rule for any Vm in
        #    network1 and the Static Nat and LB rules for vms work as expected
        # 8. Update network1 to NO2.

        self.debug("Creating a VPC offering..")

        vpc_off_list = VpcOffering.list(
            self.apiclient,
            name='Default VPC offering',
            listall=True
        )
        vpc_off = vpc_off_list[0]
        self.debug("Creating a VPC with offering: %s" % vpc_off.id)

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
        self.cleanup.append(nw_off)

        self.services["network_offering"][
            "supportedservices"] = 'Vpn,Dhcp,Dns,SourceNat,UserData,Lb,StaticNat,NetworkACL'
        self.services["network_offering"]["serviceProviderList"] = {
            "Vpn": 'VpcVirtualRouter',
            "Dhcp": 'VpcVirtualRouter',
            "Dns": 'VpcVirtualRouter',
            "SourceNat": 'VpcVirtualRouter',
            "Lb": 'VpcVirtualRouter',
            "UserData": 'VpcVirtualRouter',
            "StaticNat": 'VpcVirtualRouter',
            "NetworkACL": 'VpcVirtualRouter'
        }

        nw_off_no_pf = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        nw_off_no_pf.update(self.apiclient, state='Enabled')
        self.cleanup.append(nw_off_no_pf)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   nw_off_no_pf.id)
        network_1 = Network.create(
            self.apiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=nw_off_no_pf.id,
            zoneid=self.zone.id,
            gateway='10.1.1.1',
            vpcid=vpc.id
        )
        self.debug("Created network with ID: %s" % network_1.id)

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
        self.debug("Deployed another VM in network: %s" % network_1.id)

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

        self.debug("Creating LB rule for IP address: %s" %
                   public_ip_1.ipaddress.ipaddress)

        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.services["lbrule"],
            ipaddressid=public_ip_1.ipaddress.id,
            accountid=self.account.name,
            networkid=network_1.id,
            vpcid=vpc.id,
            domainid=self.account.domainid
        )

        self.debug("Adding virtual machines %s to LB rule" % vm_1.name)
        lb_rule.assign(self.apiclient, [vm_1])

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

        self.debug("Adding NetwrokACl rules to make PF and LB accessible")
        NetworkACL.create(
            self.apiclient,
            networkid=network_1.id,
            services=self.services["lbrule"],
            traffictype='Ingress'
        )

        self.debug(
            "Adding Egress rules to network %s to access internet" %
            (network_1.name))
        NetworkACL.create(
            self.apiclient,
            networkid=network_1.id,
            services=self.services["icmp_rule"],
            traffictype='Egress'
        )

        self.debug("Checking if we can SSH into VM_1? - IP: %s" %
                   public_ip_1.ipaddress.ipaddress)
        try:
            ssh_1 = vm_1.get_ssh_client(
                ipaddress=public_ip_1.ipaddress.ipaddress,
                reconnect=True,
                port=self.services["lbrule"]["publicport"]
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

        self.debug("Associating public IP for network: %s" % vpc.name)
        public_ip_3 = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network_1.id,
            vpcid=vpc.id
        )
        self.debug("Associated %s with network %s" % (
            public_ip_3.ipaddress.ipaddress,
            network_1.id
        ))

        self.debug("Creatinng NAT rule in network shall through exception?")
        with self.assertRaises(Exception):
            NATRule.create(
                self.apiclient,
                vm_1,
                self.services["natrule"],
                ipaddressid=public_ip_3.ipaddress.id,
                openfirewall=False,
                networkid=network_1.id,
                vpcid=vpc.id
            )
        self.debug("Create NAT rule failed!")

        self.debug(
            "Stopping all the virtual machines in network before upgrade")
        try:
            vm_1.stop(self.apiclient)
            vm_2.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop VMs, %s" % e)

        # When all Vms ain network are stopped, network state changes
        # from Implemented --> Shutdown --> Allocated
        # We can't update the network when it is in Shutodown state, hence
        # we should wait for the state to change to allocated and
        # then update the network
        retriesCount = 20
        while True:
            networks = list_networks(self.apiclient, id=network_1.id)
            self.assertEqual(
                validateList(networks)[0],
                PASS,
                "networks list validation failed, list id %s" %
                networks)
            self.debug("network state is %s" % networks[0].state)
            if networks[0].state == "Allocated":
                break
            if retriesCount == 0:
                self.fail(
                    "Network state should change to Allocated, it is %s" %
                    networks[0].state)
            retriesCount -= 1
            time.sleep(60)

        self.debug("Upgrading network offering to support PF services")
        try:
            network_1.update(
                self.apiclient,
                networkofferingid=nw_off.id
            )
        except Exception as e:
            self.fail("failed to upgrade the network offering- %s" % e)

        self.debug(
            "Starting all the virtual machines in network after upgrade")
        try:
            vm_1.start(self.apiclient)
            vm_2.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start VMs, %s" % e)

        NATRule.create(
            self.apiclient,
            vm_1,
            self.services["natrule"],
            ipaddressid=public_ip_3.ipaddress.id,
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
        self.debug("Checking if we can SSH into VM using NAT rule?")
        try:
            ssh_3 = vm_1.get_ssh_client(
                ipaddress=public_ip_3.ipaddress.ipaddress,
                reconnect=True,
                port=self.services["natrule"]["publicport"]
            )
            self.debug("SSH into VM is successfully")

            self.debug("Verifying if we can ping to outside world from VM?")
            res = ssh_3.execute("ping -c 1 www.google.com")
        except Exception as e:
            self.fail("Failed to SSH into VM - %s, %s" %
                      (public_ip_3.ipaddress.ipaddress, e))

        result = str(res)
        self.assertEqual(
            result.count("1 received"),
            1,
            "Ping to outside world from VM should be successful"
        )
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_02_network_vpcvr2vr_upgrade(self):
        """ Test update Network that is NOT part of a VPC to a nw offering
            that has services that are provided by VPCVR and vice versa
        """

        # Validate the following
        # 1. Create a Network offering - NO1 with all supported services
        #    except PF services provided by VRVPC provider, conserve mode=OFF
        # 2.Create a Network offering - NO2 with all supported services
        #    including Pf services provided by VR provider, conserve mode=OFF
        # 3. Deploy a Vm using a network, network1 created from NO2
        # 4. Update network1 to NO1.

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

        nw_off = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        nw_off.update(self.apiclient, state='Enabled')
        self.cleanup.append(nw_off)

        self.services["network_offering"][
            "supportedservices"] = 'Vpn,Dhcp,Dns,SourceNat,PortForwarding,UserData,Lb,StaticNat'
        self.services["network_offering"]["serviceProviderList"] = {
            "Vpn": 'VirtualRouter',
            "Dhcp": 'VirtualRouter',
            "Dns": 'VirtualRouter',
            "SourceNat": 'VirtualRouter',
            "PortForwarding": 'VirtualRouter',
            "Lb": 'VirtualRouter',
            "UserData": 'VirtualRouter',
            "StaticNat": 'VirtualRouter',
        }

        nw_off_vr = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        nw_off_vr.update(self.apiclient, state='Enabled')
        self.cleanup.append(nw_off_vr)

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

        self.debug(
            "Stopping all the virtual machines in network before upgrade")
        try:
            vm_1.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop VMs, %s" % e)

        self.debug("Upgrading network offering to support PF services")
        with self.assertRaises(Exception):
            network_1.update(
                self.apiclient,
                networkofferingid=nw_off_vr.id,
                changecidr=True
            )
        return


class TestVPCNetworkGc(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVPCNetworkGc, cls).getClsTestClient()
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
        cls._cleanup = []

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls.vpc_off = VpcOffering.create(
            cls.api_client,
            cls.services["vpc_offering"]
        )
        cls.vpc_off.update(cls.api_client, state='Enabled')

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.services["vpc"]["cidr"] = '10.1.1.1/16'
        cls.vpc = VPC.create(
            cls.api_client,
            cls.services["vpc"],
            vpcofferingid=cls.vpc_off.id,
            zoneid=cls.zone.id,
            account=cls.account.name,
            domainid=cls.account.domainid
        )

        cls.nw_off = NetworkOffering.create(
            cls.api_client,
            cls.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        cls.nw_off.update(cls.api_client, state='Enabled')

        cls.network_1 = Network.create(
            cls.api_client,
            cls.services["network"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            networkofferingid=cls.nw_off.id,
            zoneid=cls.zone.id,
            gateway='10.1.1.1',
            vpcid=cls.vpc.id
        )
        # Spawn an instance in that network
        cls.vm_1 = VirtualMachine.create(
            cls.api_client,
            cls.services["virtual_machine"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            networkids=[str(cls.network_1.id)]
        )
        cls.vm_2 = VirtualMachine.create(
            cls.api_client,
            cls.services["virtual_machine"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            networkids=[str(cls.network_1.id)]
        )
        cls.public_ip_1 = PublicIPAddress.create(
            cls.api_client,
            accountid=cls.account.name,
            zoneid=cls.zone.id,
            domainid=cls.account.domainid,
            networkid=cls.network_1.id,
            vpcid=cls.vpc.id
        )
        cls.lb_rule = LoadBalancerRule.create(
            cls.api_client,
            cls.services["lbrule"],
            ipaddressid=cls.public_ip_1.ipaddress.id,
            accountid=cls.account.name,
            networkid=cls.network_1.id,
            vpcid=cls.vpc.id,
            domainid=cls.account.domainid
        )
        cls.lb_rule.assign(cls.api_client, [cls.vm_1, cls.vm_2])

        cls.public_ip_2 = PublicIPAddress.create(
            cls.api_client,
            accountid=cls.account.name,
            zoneid=cls.zone.id,
            domainid=cls.account.domainid,
            networkid=cls.network_1.id,
            vpcid=cls.vpc.id
        )
        StaticNATRule.enable(
            cls.api_client,
            ipaddressid=cls.public_ip_2.ipaddress.id,
            virtualmachineid=cls.vm_1.id,
            networkid=cls.network_1.id
        )
        cls.nwacl_lb = NetworkACL.create(
            cls.api_client,
            networkid=cls.network_1.id,
            services=cls.services["lbrule"],
            traffictype='Ingress'
        )
        cls.nwacl_internet_1 = NetworkACL.create(
            cls.api_client,
            networkid=cls.network_1.id,
            services=cls.services["icmp_rule"],
            traffictype='Egress'
        )
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
        # Stop all the VMs as part of test
        vms = VirtualMachine.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )
        for vm in vms:
            if vm.state == "Running":
                cmd = stopVirtualMachine.stopVirtualMachineCmd()
                cmd.id = vm.id
                self.apiclient.stopVirtualMachine(cmd)
        return

    def tearDown(self):
        # Start all the VMs after test execution
        vms = VirtualMachine.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )
        for vm in vms:
            if vm.state == "Stopped":
                cmd = startVirtualMachine.startVirtualMachineCmd()
                cmd.id = vm.id
                self.apiclient.startVirtualMachine(cmd)
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

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_01_wait_network_gc(self):
        """ Test network gc after shutdown of vms in the network
        """

        # Validate the following
        # 1. Stop vm1 and vm2
        # 2. Wait for network GC
        # 3. When the network GC thread is run, NIC relating to this guest
        #    network will get hot unplugged.
        # 4. All the PF/Static NAT/LB rules for this network should be cleaned
        #    from VPCVR.
        # 5. All network Acl should be cleaned from VPCVR.
        # 6. All the network rules pertaining to the network in "Implemented"
        #    state should continue to work.

        self.debug("Waiting for network garbage collection thread to run")
        # Wait for the network garbage collection thread to run
        wait_for_cleanup(self.apiclient,
                         ["network.gc.interval", "network.gc.wait"])

        lbrules = LoadBalancerRule.list(
            self.apiclient,
            networkid=self.network_1.id)
        self.debug("List of LB Rules %s" % lbrules)
        self.assertEqual(
            lbrules,
            None,
            "LBrules were not cleared after network GC thread is run")
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_02_start_vm_network_gc(self):
        """ Test network rules after starting a VpcVr that
            was shutdown after network.gc
        """

        # Validate the following
        # 1. Stop vm1 and vm2
        # 2. Wait for network GC. Start 1st VM
        # 3. All the network rules created shall continue to work.

        self.debug("Waiting for network garbage collection thread to run")
        # Wait for the network garbage collection thread to run
        wait_for_cleanup(self.apiclient,
                         ["network.gc.interval", "network.gc.wait"])

        self.debug("Starting one of the virtual machine")
        try:
            self.vm_1.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start virtual machine: %s, %s" %
                      (self.vm_1.name, e))

        try:
            ssh_1 = self.vm_1.get_ssh_client(
                ipaddress=self.public_ip_1.ipaddress.ipaddress,
                reconnect=True,
                port=self.services["lbrule"]["publicport"]
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
                      (self.public_ip_1.ipaddress.ipaddress, e))

        result = str(res)
        self.debug("Result: %s" % result)
        self.assertEqual(
            result.count("1 received"),
            1,
            "Ping to outside world from VM should be successful"
        )

        self.debug("Checking if we can SSH into VM_2?")
        try:
            ssh_2 = self.vm_1.get_ssh_client(
                ipaddress=self.public_ip_2.ipaddress.ipaddress,
                reconnect=True,
                port=self.services["natrule"]["publicport"]
            )
            self.debug("SSH into VM is successfully")

            self.debug("Verifying if we can ping to outside world from VM?")
            res = ssh_2.execute("ping -c 1 www.google.com")
        except Exception as e:
            self.fail("Failed to SSH into VM - %s, %s" %
                      (self.public_ip_2.ipaddress.ipaddress, e))

        result = str(res)
        self.debug("Result: %s" % result)
        self.assertEqual(
            result.count("1 received"),
            1,
            "Ping to outside world from VM should be successful"
        )
        return

    @attr(tags=["advanced", "intervlan", "dvs"], required_hardware="true")
    def test_03_restart_vpcvr(self):
        """ Test Stop all the Vms that are part of the a Network
            (Wait for network GC).Restart VPCVR.
        """

        # Validate the following
        # 1. Stop vm3 and vm4
        # 2. Wait for network GC. Restart VPC VR
        # 3. All the network rules created shall continue to work.

        self.debug("Starting instances 1 and 2")
        try:
            self.vm_1.start(self.apiclient)
            self.vm_2.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start Virtual machines")

        self.debug("Waiting for network garbage collection thread to run")
        # Wait for the network garbage collection thread to run
        wait_for_cleanup(self.apiclient,
                         ["network.gc.interval", "network.gc.wait"])

        self.debug("Finding the VPC virtual router for account: %s" %
                   self.account.name)
        routers = Router.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "List routers shall return a valid list"
        )
        vpcvr = routers[0]
        self.debug("restarting the VPC virtual router")
        try:
            Router.reboot(
                self.apiclient,
                id=vpcvr.id
            )
        except Exception as e:
            self.fail("Failed to reboot the virtual router: %s, %s" %
                      (vpcvr.id, e))

        try:
            ssh_1 = self.vm_1.get_ssh_client(
                ipaddress=self.public_ip_1.ipaddress.ipaddress,
                reconnect=True,
                port=self.services["lbrule"]["publicport"]
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
                      (self.public_ip_1.ipaddress.ipaddress, e))

        result = str(res)
        self.debug("Result: %s" % result)
        self.assertEqual(
            result.count("1 received"),
            1,
            "Ping to outside world from VM should be successful"
        )

        self.debug("Checking if we can SSH into VM_2?")
        try:
            ssh_2 = self.vm_1.get_ssh_client(
                ipaddress=self.public_ip_2.ipaddress.ipaddress,
                reconnect=True,
                port=self.services["natrule"]["publicport"]
            )
            self.debug("SSH into VM is successfully")

            self.debug("Verifying if we can ping to outside world from VM?")
            res = ssh_2.execute("ping -c 1 www.google.com")
        except Exception as e:
            self.fail("Failed to SSH into VM - %s, %s" %
                      (self.public_ip_2.ipaddress.ipaddress, e))

        result = str(res)
        self.debug("Result: %s" % result)
        self.assertEqual(
            result.count("1 received"),
            1,
            "Ping to outside world from VM should be successful"
        )
        return


class TestRouterOperations(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestRouterOperations, cls).getClsTestClient()
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
        cls._cleanup = []

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls.vpc_off = VpcOffering.create(
            cls.api_client,
            cls.services["vpc_offering"]
        )
        cls.vpc_off.update(cls.api_client, state='Enabled')

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.services["vpc"]["cidr"] = '10.1.1.1/16'
        cls.vpc = VPC.create(
            cls.api_client,
            cls.services["vpc"],
            vpcofferingid=cls.vpc_off.id,
            zoneid=cls.zone.id,
            account=cls.account.name,
            domainid=cls.account.domainid
        )

        cls.nw_off = NetworkOffering.create(
            cls.api_client,
            cls.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        cls.nw_off.update(cls.api_client, state='Enabled')
        cls._cleanup.append(cls.nw_off)
        cls._cleanup.append(cls.vpc_off)

        cls.network_1 = Network.create(
            cls.api_client,
            cls.services["network"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            networkofferingid=cls.nw_off.id,
            zoneid=cls.zone.id,
            gateway='10.1.1.1',
            vpcid=cls.vpc.id
        )
        # Spawn an instance in that network
        cls.vm_1 = VirtualMachine.create(
            cls.api_client,
            cls.services["virtual_machine"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            networkids=[str(cls.network_1.id)]
        )
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
        return

    def tearDown(self):
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_stop_start_vpc_router(self):
        """ Test stop and start VPC router

        # 1. List the router in VPC network
        # 2. Stop the router and verify that router is in stopped state
        # 3. Start the router and verify that router is in running state
        """

        # Validate the following
        # 1. Stop vm3 and vm4
        # 2. Wait for network GC. Restart VPC VR
        # 3. All the network rules created shall continue to work.

        self.debug("Finding the VPC virtual router for account: %s" %
                   self.account.name)
        routers = Router.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "List routers shall return a valid list"
        )
        vpcvr = routers[0]
        self.debug("restarting the VPC virtual router")

        try:
            Router.stop(
                self.apiclient,
                id=vpcvr.id
            )
        except Exception as e:
            self.fail("Failed to stop the virtual router: %s, %s" %
                      (vpcvr.id, e))

        response = verifyRouterState(self.apiclient, vpcvr.id, "stopped")
        exceptionOccured = response[0]
        isRouterInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isRouterInDesiredState)):
            self.fail(exceptionMessage)

        self.debug("Starting the router with ID: %s" % vpcvr.id)
        try:
            Router.start(
                self.apiclient,
                id=vpcvr.id
            )
        except Exception as e:
            self.fail("Failed to start the virtual router: %s, %s" %
                      (vpcvr.id, e))

        response = verifyRouterState(self.apiclient, vpcvr.id, "running")
        exceptionOccured = response[0]
        isRouterInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isRouterInDesiredState)):
            self.fail(exceptionMessage)
        return
