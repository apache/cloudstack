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

""" Test VPC max networks based on the hypervisor limits """

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             NetworkOffering,
                             Network,
                             VPC,
                             VpcOffering,
                             Router,
                             Host,
                             Cluster,
                             Configurations,
                             Resources)
from marvin.lib.common import (get_zone,
                               get_domain)


class Services:
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
            "network": {
                "name": "Test Network",
                "displaytext": "Test Network",
                "netmask": '255.255.255.0',
                # Max networks allowed per hypervisor: Xenserver -> 7, VMWare -> 10 & KVM -> 27
                "limit": 5,
            }
        }


class TestVpcHypervisorMaxNetworks(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVpcHypervisorMaxNetworks, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services

        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        cls._cleanup = []

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

    @classmethod
    def tearDownClass(cls):
        super(TestVpcHypervisorMaxNetworks, cls).tearDownClass()

    def tearDown(self):
        super(TestVpcHypervisorMaxNetworks, self).tearDown()

    def validate_vpc_network(self, network, state=None):
        """Validates the VPC network"""

        self.debug("Checking if the VPC network was created successfully.")
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

    def validate_vpc_offering(self, vpc_offering):
        """Validates the VPC offering"""

        self.debug("Checking if the VPC offering was created successfully.")
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
        self.debug("VPC offering was created successfully - %s" % vpc_offering.name)

        return

    def create_network_max_limit_hypervisor_test(self, hypervisor):
        Configurations.update(
            self.apiclient,
            accountid=self.account.id,
            name="vpc.max.networks",
            value=30
        )

        self.debug("Creating a VPC offering.")
        vpc_off = VpcOffering.create(
            self.apiclient,
            self.services["vpc_offering"]
        )

        self._cleanup.append(vpc_off)
        self.validate_vpc_offering(vpc_off)

        self.debug("Enabling the VPC offering.")
        vpc_off.update(self.apiclient, state='Enabled')

        self.debug("Creating a VPC network in the account: %s" % self.account.name)
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

        vpc_router = Router.list(
            self.apiclient,
            vpcId=vpc.id,
            listAll=True
        )[0]

        host = Host.list(
            self.apiclient,
            id=vpc_router.hostId
        )[0]

        cluster = Cluster.list(
            self.apiclient,
            id=host.clusterId
        )[0]

        cluster_old_hypervisor_type = cluster.hypervisortype

        self.debug("Updating cluster %s hypervisor from %s to %s." %
                   (cluster.name, cluster_old_hypervisor_type, hypervisor))
        Cluster.update(
            self.apiclient,
            id=cluster.id,
            hypervisor=hypervisor
        )

        network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        self._cleanup.append(network_offering)
        network_offering.update(self.apiclient, state='Enabled')

        # Empty list to store all networks
        networks = []

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s." % network_offering.id)
        network_1 = Network.create(
            self.apiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=network_offering.id,
            zoneid=self.zone.id,
            gateway='10.1.0.1',
            vpcid=vpc.id
        )

        self.debug("Created network with ID: %s." % network_1.id)

        config_name = "virtual.machine.max.nics.%s" % hypervisor.lower()

        configs = Configurations.list(
            self.apiclient,
            name=config_name,
            listall=True
        )
        if not isinstance(configs, list):
            raise Exception("Failed to find %s virtual machine max NICs." % hypervisor)

        self.services["network"]["limit"] = int(configs[0].value)

        self.debug("Updating network resource limit for account %s with value %s." % (
            self.account.name, self.services["network"]["limit"]))
        Resources.updateLimit(
            self.apiclient,
            resourcetype=6,
            account=self.account.name,
            domainid=self.domain.id,
            max=self.services["network"]["limit"]
        )

        # Create networks till max limit of hypervisor
        for i in range(self.services["network"]["limit"] - 3):
            # Creating network using the network offering created
            self.debug("Creating network with network offering: %s." %
                       network_offering.id)
            gateway = '10.1.' + str(i + 1) + '.1'
            self.debug("Gateway for new network: %s." % gateway)

            network = Network.create(
                self.apiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=network_offering.id,
                zoneid=self.zone.id,
                gateway=gateway,
                vpcid=vpc.id
            )
            self.debug("Created network with ID: %s." % network.id)
            networks.append(network)

        self.debug(
            "Trying to create one more network than %s hypervisor limit in VPC: %s." % (hypervisor, vpc.name))
        gateway = '10.1.' + str(self.services["network"]["limit"]) + '.1'

        with self.assertRaises(Exception):
            Network.create(
                self.apiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=network_offering.id,
                zoneid=self.zone.id,
                gateway=gateway,
                vpcid=vpc.id
            )

        self.debug("Deleting one of the existing networks.")
        try:
            network_1.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete network: %s - %s." %
                      (network_1.name, e))

        self.debug("Creating a new network in VPC: %s." % vpc.name)
        network = Network.create(
            self.apiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=network_offering.id,
            zoneid=self.zone.id,
            gateway=gateway,
            vpcid=vpc.id
        )
        self.debug("Created a new network: %s." % network.name)
        networks.append(network)

        self.debug(
            "Updating cluster %s hypervisor to its old hypervisor %s." % (cluster.name, cluster_old_hypervisor_type))
        Cluster.update(
            self.apiclient,
            id=cluster.id,
            hypervisor=cluster_old_hypervisor_type
        )

        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_01_create_network_max_limit_xenserver(self):
        """ Test create networks in VPC up to maximum limit for XenServer hypervisor.
        """

        # Validate the following
        # 1. Create a VPC and add maximum # of supported networks to the VPC;
        # 2. After reaching the maximum networks, adding another network to the VPC must raise an exception;
        # 3. Delete a network from VPC;
        # 4. Add a new network to the VPC.

        self.create_network_max_limit_hypervisor_test("XenServer")
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_02_create_network_max_limit_vmware(self):
        """ Test create networks in VPC up to maximum limit for VMware hypervisor.
        """

        # Validate the following
        # 1. Create a VPC and add maximum # of supported networks to the VPC;
        # 2. After reaching the maximum networks, adding another network to the VPC must raise an exception;
        # 3. Delete a network from VPC;
        # 4. Add a new network to the VPC.

        self.create_network_max_limit_hypervisor_test("VMware")
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_03_create_network_max_limit_kvm(self):
        """ Test create networks in VPC up to maximum limit for KVM hypervisor.
        """

        # Validate the following
        # 1. Create a VPC and add maximum # of supported networks to the VPC;
        # 2. After reaching the maximum networks, adding another network to the VPC must raise an exception;
        # 3. Delete a network from VPC;
        # 4. Add a new network to the VPC.

        self.create_network_max_limit_hypervisor_test("KVM")
        return
