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

from nose.plugins.attrib import attr
from marvin.lib.base import (Account,
                             Router,
                             NetworkOffering,
                             Network,
                             VirtualMachine,
                             ServiceOffering,
                             Host)
from marvin.lib.utils import cleanup_resources
from marvin.lib.common import (get_domain,
                               get_template,
                               get_zone,
                               get_process_status)
import time
import multiprocessing

# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase


class TestCreateRvRNetworkOffering(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestCreateRvRNetworkOffering,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls._cleanup = []
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
        self.cleanup = []
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_createRvRNetworkOffering(self):
        """Test create RvR supported network offering
        """

        # Steps to validate
        # 1. create a network offering
        # - all services by VirtualRouter
        # - enable RedundantRouter servicecapability
        # 2. enable the network offering
        # Validate the following
        # 1. Redundant Router offering should be created successfully and
        #    listed in listNetworkOfferings response
        # assert if RvR capability is enabled

        self.debug("Creating network offering with redundant VR capability")
        try:
            network_offering = NetworkOffering.create(
                self.apiclient,
                self.testdata["nw_off_isolated_RVR"],
                conservemode=True
            )
        except Exception as e:
            self.fail("Create network offering failed! - %s" % e)

        self.debug("Enabling network offering - %s" % network_offering.name)
        # Enable Network offering
        network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(network_offering)

        self.debug("Checking if the network offering created successfully?")
        network_offs = NetworkOffering.list(
            self.apiclient,
            id=network_offering.id,
            listall=True
        )
        self.assertEqual(
            isinstance(network_offs, list),
            True,
            "List network offering should not return empty response"
        )
        self.assertEqual(
            len(network_offs),
            1,
            "List network off should have newly created network off"
        )
        for service in network_offs[0].service:
            if service.name == 'SourceNat':
                self.debug("Verifying SourceNat capabilites")
                for capability in service.capability:
                    if capability.name == 'RedundantRouter':
                        self.assertTrue(capability.value == 'true')
                        self.debug("RedundantRouter is enabled")

        return


class TestCreateRvRNetwork(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestCreateRvRNetwork, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )

        cls.testdata["small"]["zoneid"] = cls.zone.id
        cls.testdata["small"]["template"] = cls.template.id

        cls._cleanup = []
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        cls.network_offering = NetworkOffering.create(
            cls.api_client,
            cls.testdata["nw_off_isolated_RVR"],
            conservemode=True
        )
        cls._cleanup.append(cls.network_offering)

        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')

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
            self.testdata["account"],
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
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_createRvRNetwork(self):
        """Test create network with redundant routers
        """

        # Validate the following:
        # 1. listNetworkOfferings shows created offering
        # 2. listNetworks should show created network in Allocated state
        # 3. returns no Running routers in the network
        # 4. listVirtualmachines shows VM in Running state
        # 5. returns 2 routers
        # - same public IP
        # - same MAC address of public NIC
        # - different guestip address
        # - redundant state (PRIMARY or BACKUP)
        # - same gateway for the public traffic
        # 6. all routers, networks and user VMs are cleaned up

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        network = Network.create(
            self.apiclient,
            self.testdata["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id
        )
        self.debug("Created network with ID: %s" % network.id)

        networks = Network.list(
            self.apiclient,
            id=network.id,
            listall=True
        )
        self.assertEqual(
            isinstance(networks, list),
            True,
            "List networks should return a valid response for created network"
        )
        nw_response = networks[0]

        self.debug("Network state: %s" % nw_response.state)
        self.assertEqual(
            nw_response.state,
            "Allocated",
            "The network should be in allocated state after creation"
        )

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
            self.apiclient,
            networkid=network.id,
            listall=True
        )
        self.assertEqual(
            routers,
            None,
            "Routers should not be spawned when network is in allocated state"
        )

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(network.id)]
        )
        self.debug("Deployed VM in network: %s" % network.id)

        # wait for VR to update state
        time.sleep(self.testdata["sleep"])

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
            self.apiclient,
            networkid=network.id,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "list router should return Primary and backup routers"
        )
        self.assertEqual(
            len(routers),
            2,
            "Length of the list router should be 2 (Backup & primary)"
        )

        if routers[0].redundantstate == 'PRIMARY':
            primary_router = routers[0]
            backup_router = routers[1]
        else:
            primary_router = routers[1]
            backup_router = routers[0]

        self.debug("Redundant states: %s, %s" % (
            primary_router.redundantstate,
            backup_router.redundantstate
        ))
        self.assertEqual(
            primary_router.publicip,
            backup_router.publicip,
            "Public Ip should be same for both(PRIMARY & BACKUP)"
        )
        self.assertEqual(
            primary_router.redundantstate,
            "PRIMARY",
            "Redundant state of router should be PRIMARY"
        )
        self.assertEqual(
            backup_router.redundantstate,
            "BACKUP",
            "Redundant state of router should be BACKUP"
        )
        self.assertNotEqual(
            primary_router.guestipaddress,
            backup_router.guestipaddress,
            "Both (PRIMARY & BACKUP) routers should not have same guest IP"
        )

        self.assertNotEqual(
            primary_router.guestmacaddress,
            backup_router.guestmacaddress,
            "Both (PRIMARY & BACKUP) routers should not have same guestMAC"
        )
        return


class TestCreateRvRNetworkNonDefaultGuestCidr(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestCreateRvRNetworkNonDefaultGuestCidr,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )
        cls.testdata["small"]["zoneid"] = cls.zone.id
        cls.testdata["small"]["template"] = cls.template.id

        cls._cleanup = []
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        cls.network_offering = NetworkOffering.create(
            cls.api_client,
            cls.testdata["nw_off_isolated_RVR"],
            conservemode=True
        )
        cls._cleanup.append(cls.network_offering)
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')
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
            self.testdata["account"],
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
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns"])
    def test_createRvRNetwork(self):
        """Test create network with non-default guest cidr with redundant routers
        """

        # Validate the following:
        # 1. listNetworkOfferings shows created offering
        # 2. listNetworks should show created network in Allocated state
        # - gw = 192.168.2.1 and cidr = 192.168.2.0/23
        # 3. returns no Running routers in the network
        # 4. listVirtualmachines shows VM in Running state
        # 5. returns 2 routers
        # - same public IP
        # - same MAC address of public NIC
        # - different guestip address
        # - redundant state (PRIMARY or BACKUP)
        # - same gateway for the public traffic
        # 6. all routers, networks and user VMs are cleaned up

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        network = Network.create(
            self.apiclient,
            self.testdata["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id,
            netmask='255.255.254.0',
            gateway='192.168.2.1'
        )
        self.debug("Created network with ID: %s" % network.id)

        networks = Network.list(
            self.apiclient,
            id=network.id,
            listall=True
        )
        self.assertEqual(
            isinstance(networks, list),
            True,
            "List networks should return a valid response for created network"
        )
        nw_response = networks[0]

        self.debug("Network state: %s" % nw_response.state)
        self.assertEqual(
            nw_response.state,
            "Allocated",
            "The network should be in allocated state after creation"
        )
        self.assertEqual(
            nw_response.gateway,
            '192.168.2.1',
            "The gateway should be 192.168.2.1"
        )
        self.assertEqual(
            nw_response.cidr,
            '192.168.2.0/23',
            "Guest cidr should be 192.168.2.0/23 but is %s" % nw_response.cidr
        )

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
            self.apiclient,
            networkid=network.id,
            listall=True
        )
        self.assertEqual(
            routers,
            None,
            "Routers should not be spawned when network is in allocated state"
        )

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(network.id)]
        )
        self.debug("Deployed VM in network: %s" % network.id)

        # wait for VR to update state
        time.sleep(self.testdata["sleep"])

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
            self.apiclient,
            networkid=network.id,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "list router should return Primary and backup routers"
        )
        self.assertEqual(
            len(routers),
            2,
            "Length of the list router should be 2 (Backup & Primary)"
        )

        if routers[0].redundantstate == 'PRIMARY':
            primary_router = routers[0]
            backup_router = routers[1]
        else:
            primary_router = routers[1]
            backup_router = routers[0]

        self.assertEqual(
            primary_router.publicip,
            backup_router.publicip,
            "Public Ip should be same for both(PRIMARY & BACKUP)"
        )
        self.assertEqual(
            primary_router.redundantstate,
            "PRIMARY",
            "Redundant state of router should be PRIMARY"
        )
        self.assertEqual(
            backup_router.redundantstate,
            "BACKUP",
            "Redundant state of router should be BACKUP"
        )
        self.assertNotEqual(
            primary_router.guestipaddress,
            backup_router.guestipaddress,
            "Both (PRIMARY & BACKUP) routers should not have same guest IP"
        )

        self.assertNotEqual(
            primary_router.guestmacaddress,
            backup_router.guestmacaddress,
            "Both (PRIMARY & BACKUP) routers should not have same guestMAC"
        )
        return


class TestRVRInternals(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestRVRInternals, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )
        cls.testdata["small"]["zoneid"] = cls.zone.id
        cls.testdata["small"]["template"] = cls.template.id

        cls._cleanup = []
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        cls.network_offering = NetworkOffering.create(
            cls.api_client,
            cls.testdata["nw_off_isolated_RVR"],
            conservemode=True
        )
        cls._cleanup.append(cls.network_offering)
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')
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
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.dbclient = self.testClient.getDbConnection()
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
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
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_redundantVR_internals(self):
        """Test redundant router internals
        """

        # Steps to validate
        # 1. createNetwork using network offering for redundant virtual router
        # 2. listRouters in above network
        # 3. deployVM in above user account in the created network
        # 4. login to both Redundant Routers
        # 5. login to user VM
        # 6. delete user account
        # Validate the following:
        # 1. listNetworks lists network in Allocated state
        # 2. listRouters lists no routers created yet
        # 3. listRouters returns Primary and Backup routers
        # 4. ssh in to both routers and verify:
        #  - PRIMARY router has eth2 with public Ip address
        #  - BACKUP router has only guest eth0 and link local eth1
        #  - Broadcast on PRIMARY eth2 is non-zero (0.0.0.0)
        #  - execute checkrouter.sh in router home and check if it is status
        #    "PRIMARY|BACKUP" as returned by the listRouters API
        # 5. DNS of the user VM is set to RedundantRouter Gateway
        #     (/etc/resolv.conf)
        #    Check that the default gateway for the guest is the rvr gateway
        #    and not the guestIp of either of the RvRs

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        network = Network.create(
            self.apiclient,
            self.testdata["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id
        )
        self.debug("Created network with ID: %s" % network.id)

        networks = Network.list(
            self.apiclient,
            id=network.id,
            listall=True
        )
        self.assertEqual(
            isinstance(networks, list),
            True,
            "List networks should return a valid response for created network"
        )
        nw_response = networks[0]

        self.debug("Network state: %s" % nw_response.state)
        self.assertEqual(
            nw_response.state,
            "Allocated",
            "The network should be in allocated state after creation"
        )

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
            self.apiclient,
            networkid=network.id,
            listall=True
        )
        self.assertEqual(
            routers,
            None,
            "Routers should not be spawned when network is in allocated state"
        )

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(network.id)]
        )
        self.debug("Deployed VM in network: %s" % network.id)

        # wait for VR to update state
        time.sleep(self.testdata["sleep"])

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
            self.apiclient,
            networkid=network.id,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "list router should return Primary and backup routers"
        )
        self.assertEqual(
            len(routers),
            2,
            "Length of the list router should be 2 (Backup & Primary)"
        )

        if routers[0].redundantstate == 'PRIMARY':
            primary_router = routers[0]
            backup_router = routers[1]
        else:
            primary_router = routers[1]
            backup_router = routers[0]

        self.debug("Fetching the host details for double hop into router")

        hosts = Host.list(
            self.apiclient,
            id=primary_router.hostid
        )
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "List hosts should return a valid list"
        )
        primary_host = hosts[0]
        self.debug("Host for primary router: %s" % primary_host.name)
        self.debug("Host for primary router: %s" % primary_host.ipaddress)

        hosts = Host.list(
            self.apiclient,
            id=backup_router.hostid
        )
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "List hosts should return a valid list"
        )
        backup_host = hosts[0]
        self.debug("Host for backup router: %s" % backup_host.name)
        self.debug("Host for backup router: %s" % backup_host.ipaddress)
        self.debug(primary_router.linklocalip)

        # Check eth2 port for primary router
        if self.hypervisor.lower() in ('vmware', 'hyperv'):
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                primary_router.linklocalip,
                'ip addr show eth2',
                hypervisor=self.hypervisor
            )
        else:
            result = get_process_status(
                primary_host.ipaddress,
                22,
                self.testdata['configurableData']['host']["username"],
                self.testdata['configurableData']['host']["password"],
                primary_router.linklocalip,
                "ip addr show eth2"
            )

        res = str(result)

        self.debug("Command 'ip addr show eth2': %s" % result)
        self.debug("Router's public Ip: %s" % primary_router.publicip)
        self.assertEqual(
            res.count("state UP"),
            1,
            "PRIMARY router's public interface should be UP"
        )
        self.assertEqual(
            result.count('brd 0.0.0.0'),
            0,
            "Broadcast address of eth2 should not be 0.0.0.0"
        )

        # Check eth2 port for backup router
        if self.hypervisor.lower() in ('vmware', 'hyperv'):
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                backup_router.linklocalip,
                'ip addr show eth2',
                hypervisor=self.hypervisor
            )
        else:
            result = get_process_status(
                backup_host.ipaddress,
                22,
                self.testdata['configurableData']['host']["username"],
                self.testdata['configurableData']['host']["password"],
                backup_router.linklocalip,
                "ip addr show eth2"
            )

        res = str(result)

        self.debug("Command 'ip addr show eth2': %s" % result)
        self.assertEqual(
            res.count("state DOWN"),
            1,
            "BACKUP router's public interface should be DOWN"
        )
        self.assertEqual(
            result.count('brd 0.0.0.0'),
            0,
            "Broadcast address of eth2 should not be 0.0.0.0"
        )
        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List VMs should not return empty response"
        )
        vm = vms[0]

        self.assertNotEqual(
            vm.nic[0].gateway,
            primary_router.publicip,
            "The gateway of user VM should be same as primary router"
        )

        self.assertNotEqual(
            vm.nic[0].gateway,
            backup_router.publicip,
            "The gateway of user VM should be same as backup router"
        )

        return


class TestRvRRedundancy(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestRvRRedundancy, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )
        cls.testdata["small"]["zoneid"] = cls.zone.id
        cls.testdata["small"]["template"] = cls.template.id

        cls._cleanup = []
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        cls.network_offering = NetworkOffering.create(
            cls.api_client,
            cls.testdata["nw_off_isolated_RVR"],
            conservemode=True
        )
        cls.network_offering_for_update=NetworkOffering.create(
            cls.api_client,
            cls.testdata["nw_off_isolated_RVR"],
            conservemode=True
        )
        cls._cleanup.append(cls.network_offering_for_update)
        cls._cleanup.append(cls.network_offering)
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')
        cls.network_offering_for_update.update(cls.api_client, state='Enabled')
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
        self.cleanup = []
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup.insert(0, self.account)
        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        self.network = Network.create(
            self.apiclient,
            self.testdata["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id
        )
        self.debug("Created network with ID: %s" % self.network.id)
        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(self.network.id)]
        )
        self.debug("Deployed VM in network: %s" % self.network.id)

        # wait for VR to update state
        time.sleep(self.testdata["sleep"])
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_01_stopPrimaryRvR(self):
        """Test stop primary RVR
        """

        # Steps to validate
        # 1. createNetwork using network offering for redundant virtual router
        #  listNetworks returns the allocated network
        # 2. listRouters in above network. Lists no routers in the created
        #    network
        # 3. deployVM in above user account in the created network. VM is
        #    successfully Running
        # 4. listRouters that has redundantstate=PRIMARY. only one router is
        #    returned with redundantstate = PRIMARY for this network
        # 5. stopRouter that is Primary. Router goes to stopped state
        #    successfully
        # 6. listRouters in the account and in the network. Lists old PRIMARY
        #    router in redundantstate=UNKNOWN, and the old BACKUP router as
        #    new PRIMARY
        # 7. start the stopped router. Stopped rvr starts up successfully and
        #    is in Running state
        # 8. listRouters in the account and in the network. Router shows up as
        #    BACKUP and NOT PRIMARY, should have only one BACKUP and one PRIMARY
        #    at the end, public IP of the SourceNAT should remain same after
        #    reboot
        # 9. delete the account

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
            self.apiclient,
            networkid=self.network.id,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "list router should return Primary and backup routers"
        )
        self.assertEqual(
            len(routers),
            2,
            "Length of the list router should be 2 (Backup & Primary)"
        )

        if routers[0].redundantstate == 'PRIMARY':
            primary_router = routers[0]
            backup_router = routers[1]
        else:
            primary_router = routers[1]
            backup_router = routers[0]

        self.debug("Stopping the PRIMARY router")
        try:
            Router.stop(self.apiclient, id=primary_router.id)
        except Exception as e:
            self.fail("Failed to stop primary router: %s" % e)

        # wait for VR to update state
        time.sleep(self.testdata["sleep"])

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
            self.apiclient,
            id=primary_router.id,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "list router should return Primary and backup routers"
        )
        self.assertIn(
            routers[0].redundantstate, [
                'UNKNOWN', 'FAULT'], "Redundant state of the primary router\
                        should be UNKNOWN/FAULT but is %s" %
            routers[0].redundantstate)

        self.debug(
            "Checking state of the backup router in %s" %
            self.network.name)
        routers = Router.list(
            self.apiclient,
            id=backup_router.id,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "list router should return backup router"
        )
        self.assertEqual(
            routers[0].redundantstate,
            'PRIMARY',
            "Redundant state of the router should be PRIMARY but is %s" %
            routers[0].redundantstate)

        self.debug("Starting the old PRIMARY router")
        try:
            Router.start(self.apiclient, id=primary_router.id)
            self.debug("old PRIMARY router started")
        except Exception as e:
            self.fail("Failed to start primary router: %s" % e)

        # wait for VR to update state
        time.sleep(self.testdata["sleep"])

        self.debug(
            "Checking state of the primary router in %s" %
            self.network.name)
        routers = Router.list(
            self.apiclient,
            id=primary_router.id,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "list router should return backup router"
        )
        self.assertEqual(
            routers[0].redundantstate,
            'BACKUP',
            "Redundant state of the router should be BACKUP but is %s" %
            routers[0].redundantstate)
        self.assertEqual(
            primary_router.publicip,
            routers[0].publicip,
            "Public IP should be same after reboot"
        )
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_02_stopBackupRvR(self):
        """Test stop backup RVR
        """

        # Steps to validate
        # 1. createNetwork using network offering for redundant virtual router
        #  listNetworks returns the allocated network
        # 2. listRouters in above network. Lists no routers in the created
        #    network
        # 3. deployVM in above user account in the created network. VM is
        #    successfully Running
        # 4. listRouters that has redundantstate=PRIMARY. only one router is
        #    returned with redundantstate = PRIMARY for this network
        # 5. stopRouter that is BACKUP. Router goes to stopped state
        #    successfully
        # 6. listRouters in the account and in the network. Lists old PRIMARY
        #    router in redundantstate=UNKNOWN
        # 7. start the stopped router. Stopped rvr starts up successfully and
        #    is in Running state
        # 8. listRouters in the account and in the network. Router shows up as
        #    BACKUP and NOT PRIMARY, should have only one BACKUP and one PRIMARY
        #    at the end, public IP of the SourceNAT should remain same after
        #    reboot
        # 9. delete the account

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
            self.apiclient,
            networkid=self.network.id,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "list router should return Primary and backup routers"
        )
        self.assertEqual(
            len(routers),
            2,
            "Length of the list router should be 2 (Backup & Primary)"
        )

        if routers[0].redundantstate == 'PRIMARY':
            primary_router = routers[0]
            backup_router = routers[1]
        else:
            primary_router = routers[1]
            backup_router = routers[0]

        self.debug("Stopping the BACKUP router")
        try:
            Router.stop(self.apiclient, id=backup_router.id)
        except Exception as e:
            self.fail("Failed to stop backup router: %s" % e)

        # wait for VR update state
        time.sleep(self.testdata["sleep"])

        self.debug(
            "Checking state of the backup router in %s" %
            self.network.name)
        routers = Router.list(
            self.apiclient,
            id=backup_router.id,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "list router should return Primary and backup routers"
        )
        self.assertIn(
            routers[0].redundantstate, [
                'UNKNOWN', 'FAULT'], "Redundant state of the backup router\
                        should be UNKNOWN/FAULT but is %s" %
            routers[0].redundantstate)

        self.debug(
            "Checking state of the primary router in %s" %
            self.network.name)
        routers = Router.list(
            self.apiclient,
            id=primary_router.id,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "list router should return Primary and backup routers"
        )
        self.assertEqual(
            routers[0].redundantstate,
            'PRIMARY',
            "Redundant state of the router should be PRIMARY but is %s" %
            routers[0].redundantstate)

        self.debug("Starting the old BACKUP router")
        try:
            Router.start(self.apiclient, id=backup_router.id)
            self.debug("old BACKUP router started")
        except Exception as e:
            self.fail("Failed to stop primary router: %s" % e)

        # wait for VR to start and update state
        time.sleep(self.testdata["sleep"])

        self.debug(
            "Checking state of the backup router in %s" %
            self.network.name)
        routers = Router.list(
            self.apiclient,
            id=backup_router.id,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "list router should return backup router"
        )
        self.assertEqual(
            routers[0].redundantstate,
            'BACKUP',
            "Redundant state of the router should be BACKUP but is %s" %
            routers[0].redundantstate)
        self.assertEqual(
            backup_router.publicip,
            routers[0].publicip,
            "Public IP should be same after reboot"
        )
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_03_rebootPrimaryRvR(self):
        """Test reboot primary RVR
        """

        # Steps to validate
        # 1. createNetwork using network offering for redundant virtual router
        #  listNetworks returns the allocated network
        # 2. listRouters in above network. Lists no routers in the created
        #    network
        # 3. deployVM in above user account in the created network. VM is
        #    successfully Running
        # 4. listRouters that has redundantstate=PRIMARY. only one router is
        #    returned with redundantstate = PRIMARY for this network
        # 5. reboot router that is PRIMARY. Router reboots state
        #    successfully
        # 6. lists old PRIMARY router in redundantstate=BACKUP and the old
        #    BACKUP router as new PRIMARY + public IP of the SourceNAT should
        #    remain same after the reboot

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
            self.apiclient,
            networkid=self.network.id,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "list router should return Primary and backup routers"
        )
        self.assertEqual(
            len(routers),
            2,
            "Length of the list router should be 2 (Backup & Primary)"
        )

        if routers[0].redundantstate == 'PRIMARY':
            primary_router = routers[0]
            backup_router = routers[1]
        else:
            primary_router = routers[1]
            backup_router = routers[0]

        self.debug("Rebooting the primary router")
        try:
            Router.reboot(self.apiclient, id=primary_router.id)
        except Exception as e:
            self.fail("Failed to reboot PRIMARY router: %s" % e)

        # wait for VR to update state
        time.sleep(self.testdata["sleep"])

        self.debug(
            "Checking state of the primary router in %s" %
            self.network.name)
        routers = Router.list(
            self.apiclient,
            id=primary_router.id,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "list router should return Primary and backup routers"
        )
        self.assertEqual(
            routers[0].redundantstate,
            'BACKUP',
            "Redundant state of the router should be BACKUP but is %s" %
            routers[0].redundantstate)

        self.debug(
            "Checking state of the backup router in %s" %
            self.network.name)
        routers = Router.list(
            self.apiclient,
            id=backup_router.id,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "list router should return Primary and backup routers"
        )
        self.assertEqual(
            routers[0].redundantstate,
            'PRIMARY',
            "Redundant state of the router should be PRIMARY but is %s" %
            routers[0].redundantstate)
        self.assertEqual(
            primary_router.publicip,
            routers[0].publicip,
            "Public IP should be same after reboot"
        )
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_04_rebootBackupRvR(self):
        """Test reboot backup RVR
        """

        # Steps to validate
        # 1. createNetwork using network offering for redundant virtual router
        #  listNetworks returns the allocated network
        # 2. listRouters in above network. Lists no routers in the created
        #    network
        # 3. deployVM in above user account in the created network. VM is
        #    successfully Running
        # 4. listRouters that has redundantstate=PRIMARY. only one router is
        #    returned with redundantstate = PRIMARY for this network
        # 5. reboot router that is BACKUP. Router reboots state
        #    successfully
        # 6. lists old BACKUP router in redundantstate=BACKUP, and the old
        #    PRIMARY router is still PRIMARY+ public IP of the SourceNAT should
        #    remain same after the reboot

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
            self.apiclient,
            networkid=self.network.id,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "list router should return Primary and backup routers"
        )
        self.assertEqual(
            len(routers),
            2,
            "Length of the list router should be 2 (Backup & Primary)"
        )

        if routers[0].redundantstate == 'PRIMARY':
            primary_router = routers[0]
            backup_router = routers[1]
        else:
            primary_router = routers[1]
            backup_router = routers[0]

        self.debug("Rebooting the backup router")
        try:
            Router.reboot(self.apiclient, id=backup_router.id)
        except Exception as e:
            self.fail("Failed to reboot BACKUP router: %s" % e)

        # wait for VR to update state
        time.sleep(self.testdata["sleep"])

        self.debug(
            "Checking state of the backup router in %s" %
            self.network.name)
        routers = Router.list(
            self.apiclient,
            id=backup_router.id,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "list router should return Primary and backup routers"
        )
        self.assertEqual(
            routers[0].redundantstate,
            'BACKUP',
            "Redundant state of the router should be BACKUP but is %s" %
            routers[0].redundantstate)

        self.debug(
            "Checking state of the Primary router in %s" %
            self.network.name)
        routers = Router.list(
            self.apiclient,
            id=primary_router.id,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "list router should return Primary and backup routers"
        )
        self.assertEqual(
            routers[0].redundantstate,
            'PRIMARY',
            "Redundant state of the router should be PRIMARY but is %s" %
            routers[0].redundantstate)
        self.assertEqual(
            primary_router.publicip,
            routers[0].publicip,
            "Public IP should be same after reboot"
        )
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_05_stopBackupRvR_startInstance(self):
        """Test stop backup RVR and start instance
        """

        # Steps to validate
        # 1. createNetwork using network offering for redundant virtual router
        #  listNetworks returns the allocated network
        # 2. listRouters in above network. Lists no routers in the created
        #    network
        # 3. deployVM in above user account in the created network. VM is
        #    successfully Running
        # 4. listRouters that has redundantstate=PRIMARY. only one router is
        #    returned with redundantstate = PRIMARY for this network
        # 5. stop router that is BACKUP.
        # 6. listRouters in the account and in the network
        # 7. deployVM in the user account in the created network
        # 8. listRouters in the account and in the network
        # 9. delete the account

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
            self.apiclient,
            networkid=self.network.id,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "list router should return Primary and backup routers"
        )
        self.assertEqual(
            len(routers),
            2,
            "Length of the list router should be 2 (Backup & Primary)"
        )

        if routers[0].redundantstate == 'PRIMARY':
            backup_router = routers[1]
        else:
            backup_router = routers[0]

        self.debug("Stopping the backup router")
        try:
            Router.stop(self.apiclient, id=backup_router.id)
        except Exception as e:
            self.fail("Failed to stop BACKUP router: %s" % e)

        # wait for VR to update state
        time.sleep(self.testdata["sleep"])

        self.debug(
            "Checking state of the backup router in %s" %
            self.network.name)
        routers = Router.list(
            self.apiclient,
            id=backup_router.id,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "list router should return Primary and backup routers"
        )
        self.assertIn(
            routers[0].redundantstate,
            'UNKNOWN',
            "Redundant state of the backup router\
                    should be UNKNOWN but is %s" %
            routers[0].redundantstate)

        # Spawn an instance in that network
        vm_2 = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(self.network.id)]
        )
        self.debug("Deployed VM in network: %s" % self.network.id)

        vms = VirtualMachine.list(
            self.apiclient,
            id=vm_2.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List Vms should return a valid list"
        )
        vm = vms[0]
        self.assertEqual(
            vm.state,
            "Running",
            "Vm should be in running state after deployment"
        )

        # wait for VR to update state
        time.sleep(self.testdata["sleep"])

        self.debug(
            "Checking state of the backup router in %s" %
            self.network.name)
        routers = Router.list(
            self.apiclient,
            id=backup_router.id,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "list router should return Primary and backup routers"
        )
        self.assertEqual(
            routers[0].redundantstate,
            'BACKUP',
            "Redundant state of the router should be BACKUP but is %s" %
            routers[0].redundantstate)
        return

    def updateNetwork(self, conn):
        try:
            self.network.update(
                self.api_client,
                networkofferingid=self.network_offering_for_update.id,
                updateinsequence=True,
                forced=True,
                changecidr=False
            )
        except Exception as e:
               conn.send("Failed to update network: %s due to %s"%(self.network.name, e))
        conn.send("update Network Complete")
        return



    def get_primary_and_backupRouter(self):
        retry = 4
        primary_router = backup_router=None
        while retry > 0:
            routers = Router.list(
                self.apiclient,
                networkid=self.network.id,
                listall=True
            )
            retry = retry-1
            if len(routers) < 2:
               continue
            if not (routers[0].redundantstate == 'PRIMARY' or routers[1].redundantstate == 'PRIMARY'):
                continue;
            if routers[0].redundantstate == 'PRIMARY':
               primary_router = routers[0]
               backup_router = routers[1]
               break
            else:
               primary_router = routers[1]
               backup_router = routers[0]
               break
            self.info("primary_router: %s, backup_router: %s" % (primary_router, backup_router))
        return primary_router, backup_router


    def chek_for_new_backupRouter(self,old_backup_router):
        primary_router, backup_router = self.get_primary_and_backupRouter()
        retry = 4
        self.info("Checking if new router is getting created.")
        self.info("old_backup_router:"+old_backup_router.name+" new_backup_router:"+backup_router.name)
        while old_backup_router.name == backup_router.name:
            self.debug("waiting for new router old router:"+backup_router.name)
            retry = retry-1
            if retry == 0:
                break;
            time.sleep(self.testdata["sleep"])
            primary_router, backup_router = self.get_primary_and_backupRouter()
        if retry == 0:
            self.fail("New router creation taking too long, timed out")

    def wait_untill_router_stabilises(self):
        retry=4
        while retry > 0:
            routers = Router.list(
                self.apiclient,
                networkid=self.network.id,
                listall=True
            )
            retry = retry-1
            self.info("waiting untill state of the routers is stable")
            if routers[0].redundantstate != 'UNKNOWN' and routers[1].redundantstate != 'UNKNOWN':
                return
            elif retry==0:
                self.fail("timedout while waiting for routers to stabilise")
                return
            time.sleep(self.testdata["sleep"])

    @attr(tags=["bharat"])
    def test_06_updateVRs_in_sequence(self):
        """Test update network and check if VRs are updated in sequence
        """

        # Steps to validate
        # update network to a new offering
        # check if the primary router is running while backup is starting.
        # check if the backup is running while primary is starting.
        # check if both the routers are running after the update is complete.

        #clean up the network to make sure it is in proper state.
        self.network.restart(self.apiclient,cleanup=True)
        time.sleep(self.testdata["sleep"])
        self.wait_untill_router_stabilises()
        old_primary_router, old_backup_router = self.get_primary_and_backupRouter()
        self.info("old_primary_router:"+old_primary_router.name+" old_backup_router"+old_backup_router.name)
        #chek if the network is in correct state
        self.assertEqual(old_primary_router.state, "Running", "The primary router is not running, network is not in a correct state to start the test")
        self.assertEqual(old_backup_router.state, "Running", "The backup router is not running, network is not in a correct state to start the test")

        worker, monitor = multiprocessing.Pipe()
        worker_process = multiprocessing.Process(target=self.updateNetwork, args=(worker,))
        worker_process.start()
        if not worker_process.is_alive():
            message = monitor.recv()
            if "Complete" not in message:
                self.fail(message)

        self.info("Network update Started, the old backup router will get destroyed and a new router will be created")

        self.chek_for_new_backupRouter(old_backup_router)
        primary_router, new_backup_router=self.get_primary_and_backupRouter()
        #the state of the primary router should be running. while backup is being updated
        self.assertEqual(primary_router.state, "Running", "State of the primary router is not running")
        self.assertEqual(primary_router.redundantstate, 'PRIMARY', "Redundant state of the primary router should be PRIMARY, but it is %s"%primary_router.redundantstate)
        self.info("Old backup router:"+old_backup_router.name+" is destroyed and new router:"+new_backup_router.name+" got created")

        #wait for the new backup to become primary.
        retry = 4
        while new_backup_router.name != primary_router.name:
            retry = retry-1
            if retry == 0:
                break
            time.sleep(self.testdata["sleep"])
            self.info("wating for backup router to become primary router name:"+new_backup_router.name)
            primary_router, backup_router = self.get_primary_and_backupRouter()
        if retry == 0:
            self.fail("timed out while waiting for new backup router to change state to PRIMARY.")

        #new backup router has become primary.
        self.info("newly created router:"+new_backup_router.name+" has changed state to Primary")
        self.info("old primary router:"+old_primary_router.name+"is destroyed")
        #old primary will get destroyed and a new backup will be created.
        #wait until new backup changes state from unknown to backup
        primary_router, backup_router = self.get_primary_and_backupRouter()
        retry = 4
        while backup_router.redundantstate != 'BACKUP':
            retry = retry-1
            self.info("waiting for router:"+backup_router.name+" to change state to Backup")
            if retry == 0:
                break
            time.sleep(self.testdata["sleep"])
            primary_router, backup_router = self.get_primary_and_backupRouter()
            self.assertEqual(primary_router.state, "Running", "State of the primary router is not running")
            self.assertEqual(primary_router.redundantstate, 'PRIMARY', "Redundant state of the primary router should be PRIMARY, but it is %s"%primary_router.redundantstate)
        if retry == 0:
            self.fail("timed out while waiting for new backup rotuer to change state to PRIMARY.")

        #the network update is complete.finally both the router should be running.
        new_primary_router, new_backup_router=self.get_primary_and_backupRouter()
        self.assertEqual(new_primary_router.state, "Running", "State of the primary router:"+new_primary_router.name+" is not running")
        self.assertEqual(new_backup_router.state, "Running", "State of the backup router:"+new_backup_router.name+" is not running")
        worker_process.join()
