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
""" P1 tests for routers
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (stopVirtualMachine,
                                  stopRouter,
                                  startRouter)
from marvin.lib.utils import (cleanup_resources,
                              get_process_status)
from marvin.lib.base import (ServiceOffering,
                             VirtualMachine,
                             Account,
                             LoadBalancerRule,
                             FireWallRule,
                             NATRule)
from marvin.lib.common import (get_zone,
                               get_template,
                               get_domain,
                               list_virtual_machines,
                               list_networks,
                               list_configurations,
                               list_routers,
                               list_nat_rules,
                               list_publicIP,
                               list_lb_rules,
                               list_firewall_rules,
                               list_hosts)

# Import System modules
import time

class TestRouterServices(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestRouterServices, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        # Create an account, network, VM and IP addresses
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls.vm_1 = VirtualMachine.create(
            cls.api_client,
            cls.services["virtual_machine"],
            templateid=cls.template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls.vm_2 = VirtualMachine.create(
            cls.api_client,
            cls.services["virtual_machine"],
            templateid=cls.template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls._cleanup = [
            cls.account,
            cls.service_offering
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(
                TestRouterServices,
                cls).getClsTestClient().getApiClient()
            # Clean up, terminate the created templates
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_01_AdvancedZoneRouterServices(self):
        """Test advanced zone router services
        """
        # Validate the following:
        # 1. Verify that list of services provided by this network are running
        #    a. DNS
        #    b. DHCP
        #    c. Gateway
        #    d. Firewall
        #    e. LB
        #    f. VPN
        #    g. userdata
        # 2. wait for router to start and guest network to be created
        #    a. listRouters account=user, domainid=1 (router state=Running)
        #    b. listNetworks account=user domainid=1
        #       (network state=Implemented)
        #    c. listVirtualMachines account=user domainid=1 (VM state=Running)
        # 3. listNetwork

        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
        )

        self.assertEqual(
            isinstance(routers, list),
            True,
            "Check for list routers response return valid data"
        )
        self.assertNotEqual(
            len(routers),
            0,
            "Check list router response"
        )
        for router in routers:
            self.debug("Router ID: %s & Router state: %s" % (
                router.id,
                router.state
            ))
            self.assertEqual(
                router.state,
                'Running',
                "Router state is not running but is  %s" % router.state
            )

        # Network state associated with account should be 'Implemented'
        networks = list_networks(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            type='Isolated'
        )
        self.assertEqual(
            isinstance(networks, list),
            True,
            "Check for list networks response return valid data"
        )

        self.assertNotEqual(
            len(networks),
            0,
            "Check list networks response"
        )
        for network in networks:
            self.debug("Network ID: %s & Network state: %s" % (
                network.id,
                network.state
            ))
            self.assertIn(
                network.state,
                ['Implemented', 'Allocated'],
                "Check list network response for network state"
            )

        # VM state associated with account should be 'Running'
        virtual_machines = list_virtual_machines(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.assertEqual(
            isinstance(virtual_machines, list),
            True,
            "Check for list virtual machines response return valid data"
        )
        self.assertNotEqual(
            len(virtual_machines),
            0,
            "Check list virtual machines response"
        )
        for virtual_machine in virtual_machines:
            self.assertEqual(
                virtual_machine.state,
                'Running',
                "Check list VM response for Running state"
            )
            self.debug("VM ID: %s & VM state: %s" % (
                virtual_machine.id,
                virtual_machine.state
            ))

        # Check status of DNS, DHCP, FIrewall, LB VPN processes
        networks = list_networks(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            type='Isolated'
        )
        self.assertEqual(
            isinstance(networks, list),
            True,
            "Check for list networks response return valid data"
        )
        self.assertNotEqual(
            len(networks),
            0,
            "Check list networks response"
        )
        # Load Balancer, Userdata, VPN, Firewall, Gateway, DNS processes should
        # be running
        for network in networks:
            self.assertEqual(
                'Lb' in str(network.service),
                True,
                "Check Load balancing process in list networks"
            )
            self.assertEqual(
                'UserData' in str(network.service),
                True,
                "Check UserData service in list networks"
            )
            self.assertEqual(
                'Vpn' in str(network.service),
                True,
                "Check Vpn service in list networks"
            )
            self.assertEqual(
                'Firewall' in str(network.service),
                True,
                "Check Firewall service in list networks"
            )
            self.assertEqual(
                'Dns' in str(network.service),
                True,
                "Check Dns service in list networks"
            )
        return

    @attr(configuration="network.gc")
    @attr(tags=["advanced"], required_hardware="false")
    def test_02_NetworkGarbageCollection(self):
        """Test network garbage collection
        """
        # Validate the following
        # 1. wait for router to start and guest network to be created
        #    a.listRouters account=user, domainid=1 (router state=Running)
        #    b.listNetworks account=user domainid=1 (network state=Implemented)
        #    c.listVirtualMachines account=user domainid=1 (VM states=Running)
        # 4. stopVirtualMachines (stop all VMs in this account)
        # 5. wait for VMs to stop-listVirtualMachines account=user, domainid=1
        #    (Both VM states = Stopped)
        # 6. wait for network.gc.interval*2 seconds (600s)
        # 7. listRouters account=user, domainid=1

        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
        )

        self.assertEqual(
            isinstance(routers, list),
            True,
            "Check for list routers response return valid data"
        )
        self.assertNotEqual(
            len(routers),
            0,
            "Check list router response"
        )
        # Router associated with account should be in running state
        timeout = 180
        router = routers[0]
        self.debug("Router ID: %s & Router state: %s" % (
            router.id,
            router.state
        ))
        self.debug(
            "Wait for %s secs max for router to reach Running state" %
            timeout)
        while timeout:
            time.sleep(60)
            routers = list_routers(
                self.apiclient,
                account=self.account.name,
                domainid=self.account.domainid,
                id=router.id)
            router = routers[0]
            if router.state == 'Running':
                break

            timeout = timeout - 60
            if timeout == 0:
                self.assertEqual(
                    router.state,
                    'Running',
                    "Router not in Running state")

        # Network state associated with account should be 'Implemented'
        networks = list_networks(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            type='Isolated'
        )
        self.assertEqual(
            isinstance(networks, list),
            True,
            "Check for list networks response return valid data"
        )
        self.assertNotEqual(
            len(networks),
            0,
            "Check list networks response"
        )
        # Check if network in 'Implemented' state
        for network in networks:
            self.debug("Network ID: %s & Network state: %s" % (
                network.id,
                network.state
            ))
            self.assertIn(
                network.state,
                ['Implemented', 'Allocated'],
                "Check list network response for network state"
            )

        # VM state associated with account should be 'Running'
        virtual_machines = list_virtual_machines(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
        )

        self.assertEqual(
            isinstance(virtual_machines, list),
            True,
            "Check for list virtual machines response return valid data"
        )
        self.assertNotEqual(
            len(virtual_machines),
            0,
            "Check list virtual machines response"
        )
        for virtual_machine in virtual_machines:
            self.debug("VM ID: %s & VM state: %s" % (
                virtual_machine.id,
                virtual_machine.state
            ))
            self.assertEqual(
                virtual_machine.state,
                'Running',
                "Check list VM response for Running state"
            )

            # Stop virtual machine
            cmd = stopVirtualMachine.stopVirtualMachineCmd()
            cmd.id = virtual_machine.id
            self.apiclient.stopVirtualMachine(cmd)

        gcinterval = list_configurations(
            self.apiclient,
            name='network.gc.interval'
        )
        self.assertEqual(
            isinstance(gcinterval, list),
            True,
            "Check for list intervals response return valid data"
        )
        self.debug("network.gc.interval: %s" % gcinterval[0].value)

        gcwait = list_configurations(
            self.apiclient,
            name='network.gc.wait'
        )
        self.assertEqual(
            isinstance(gcwait, list),
            True,
            "Check for list intervals response return valid data"
        )
        self.debug("network.gc.wait: %s" % gcwait[0].value)

        total_wait = int(gcinterval[0].value) + int(gcwait[0].value)
        # Router is stopped after (network.gc.interval *2) time. Wait for
        # (network.gc.interval+network.gc.wait) * 2 for moving
        # router to 'Stopped'
        time.sleep(total_wait * 2)

        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
        )

        self.assertEqual(
            isinstance(routers, list),
            True,
            "Check for list routers response return valid data"
        )
        self.assertNotEqual(
            len(routers),
            0,
            "Check list router response"
        )
        for router in routers:
            self.debug("Router ID: %s & Router state: %s" % (
                router.id,
                router.state
            ))
            self.assertEqual(
                router.state,
                'Stopped',
                "Check list router response for router state"
            )

        # Cleanup Vm_2 - Not required for further tests
        self.cleanup.append(self.vm_2)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_03_RouterStartOnVmDeploy(self):
        """Test router start on VM deploy
        """
        # Validate the following
        # 1. deployVirtualMachine in the account
        # 2. listVirtualMachines account=user, domainid=1
        # 3. when listVirtualMachines reports the userVM to be in state=Running
        # 4. listRouters should report router to have come back to "Running"
        #    state
        # 5. All other VMs in the account should remain in "Stopped" state

        # stop all pre-existing virtual machines if they are in 'Running' state
        virtual_machines = list_virtual_machines(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
        )

        self.assertEqual(
            isinstance(virtual_machines, list),
            True,
            "Check for list virtual machines response return valid data"
        )
        self.assertNotEqual(
            len(virtual_machines),
            0,
            "Check list virtual machines response"
        )
        for virtual_machine in virtual_machines:
            self.debug("VM ID: %s & VM state: %s" % (
                virtual_machine.id,
                virtual_machine.state
            ))
            if virtual_machine.state == 'Running':
                # Stop virtual machine
                cmd = stopVirtualMachine.stopVirtualMachineCmd()
                cmd.id = virtual_machine.id
                self.apiclient.stopVirtualMachine(cmd)

        vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id
        )
        self.debug("Deployed a VM with ID: %s" % vm.id)

        virtual_machines = list_virtual_machines(
            self.apiclient,
            id=vm.id,
            account=self.account.name,
            domainid=self.account.domainid,
        )

        self.assertEqual(
            isinstance(virtual_machines, list),
            True,
            "Check for list virtual machines response return valid data"
        )

        self.assertNotEqual(
            len(virtual_machines),
            0,
            "Check list virtual machines response"
        )
        # VM state should be 'Running'
        for virtual_machine in virtual_machines:
            self.assertEqual(
                virtual_machine.state,
                'Running',
                "Check list VM response for Running state"
            )

        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
        )

        self.assertEqual(
            isinstance(routers, list),
            True,
            "Check for list routers response return valid data"
        )

        self.assertNotEqual(
            len(routers),
            0,
            "Check list router response"
        )
        # Routers associated with account should be 'Running' after deployment
        # of VM
        for router in routers:
            self.debug("Router ID: %s & Router state: %s" % (
                router.id,
                router.state
            ))
            self.assertEqual(
                router.state,
                'Running',
                "Check list router response for router state"
            )

        # All other VMs (VM_1) should be in 'Stopped'
        virtual_machines = list_virtual_machines(
            self.apiclient,
            id=self.vm_1.id,
            account=self.account.name,
            domainid=self.account.domainid,
        )

        self.assertEqual(
            isinstance(virtual_machines, list),
            True,
            "Check for list VMs response return valid data"
        )

        self.assertNotEqual(
            len(virtual_machines),
            0,
            "Check list virtual machines response"
        )
        for virtual_machine in virtual_machines:
            self.debug("VM ID: %s & VM state: %s" % (
                virtual_machine.id,
                virtual_machine.state
            ))
            self.assertEqual(
                virtual_machine.state,
                'Stopped',
                "Check list VM response for Stopped state"
            )

        return


class TestRouterStopCreatePF(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestRouterStopCreatePF, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        # Create an account, network, VM and IP addresses
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls.vm_1 = VirtualMachine.create(
            cls.api_client,
            cls.services["virtual_machine"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls._cleanup = [
            cls.account,
            cls.service_offering
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(
                TestRouterStopCreatePF,
                cls).getClsTestClient().getApiClient()
            # Clean up, terminate the created resources
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created resources
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    def test_01_RouterStopCreatePF(self):
        """Test router stop create port forwarding
        """
        # validate the following
        # 1. wait for router to start, guest network to be implemented and
        #    VM to report Running
        # 2. stopRouter for this account
        # 3. wait for listRouters to report Router as 'Stopped'
        # 4. listPublicIpAddresses account=user, domainid=1 - pick ipaddressid
        # 5. createPortForwardingRule (ipaddressid from step 5.)
        #    a. for port 22 (ssh) for user VM deployed in step 1.
        #    b. public port 22 , private port 22
        # 6. startRouter stopped for this account
        # 7. wait for listRouters to show router as Running

        # Get router details associated for that account
        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
        )

        self.assertEqual(
            isinstance(routers, list),
            True,
            "Check for list routers response return valid data"
        )
        self.assertNotEqual(
            len(routers),
            0,
            "Check list router response"
        )
        router = routers[0]

        self.debug("Stopping router ID: %s" % router.id)

        # Stop the router
        cmd = stopRouter.stopRouterCmd()
        cmd.id = router.id
        self.apiclient.stopRouter(cmd)

        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "Check for list routers response return valid data"
        )
        router = routers[0]

        self.assertEqual(
            router.state,
            'Stopped',
            "Check list router response for router state"
        )

        public_ips = list_publicIP(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id
        )
        self.assertEqual(
            isinstance(public_ips, list),
            True,
            "Check for list public IPs response return valid data"
        )

        public_ip = public_ips[0]

        # Open up firewall port for SSH
        FireWallRule.create(
            self.apiclient,
            ipaddressid=public_ip.id,
            protocol=self.services["natrule"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["natrule"]["publicport"],
            endport=self.services["natrule"]["publicport"]
        )

        self.debug("Creating NAT rule for VM ID: %s" % self.vm_1.id)
        # Create NAT rule
        nat_rule = NATRule.create(
            self.apiclient,
            self.vm_1,
            self.services["natrule"],
            public_ip.id
        )

        self.debug("Starting router ID: %s" % router.id)
        # Start the router
        cmd = startRouter.startRouterCmd()
        cmd.id = router.id
        self.apiclient.startRouter(cmd)

        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "Check for list routers response return valid data"
        )
        router = routers[0]

        self.assertEqual(
            router.state,
            'Running',
            "Check list router response for router state"
        )
        # NAT Rule should be in Active state after router start
        nat_rules = list_nat_rules(
            self.apiclient,
            id=nat_rule.id
        )
        self.assertEqual(
            isinstance(nat_rules, list),
            True,
            "Check for list NAT rules response return valid data"
        )
        self.assertEqual(
            nat_rules[0].state,
            'Active',
            "Check list port forwarding rules"
        )
        try:

            self.debug("SSH into VM with ID: %s" % nat_rule.ipaddress)

            self.vm_1.get_ssh_client(
                ipaddress=nat_rule.ipaddress,
                port=self.services["natrule"]["publicport"])
        except Exception as e:
            self.fail(
                "SSH Access failed for %s: %s" %
                (nat_rule.ipaddress, e)
            )
        return


class TestRouterStopCreateLB(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestRouterStopCreateLB, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        # Create an account, network, VM and IP addresses
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls.vm_1 = VirtualMachine.create(
            cls.api_client,
            cls.services["virtual_machine"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls._cleanup = [
            cls.account,
            cls.service_offering
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(
                TestRouterStopCreateLB,
                cls).getClsTestClient().getApiClient()
            # Clean up, terminate the created resources
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    def test_01_RouterStopCreateLB(self):
        """Test router stop create Load balancing
        """
        # validate the following
        # 1. listLoadBalancerRules (publicipid=ipaddressid of source NAT)
        # 2. rule should be for port 22 as applied and
        #    should be in state=Active
        # 3. ssh access should be allowed to the userVMs over the source NAT IP
        #    and port 22

        # Get router details associated for that account
        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
        )

        self.assertEqual(
            isinstance(routers, list),
            True,
            "Check for list routers response return valid data"
        )

        self.assertNotEqual(
            len(routers),
            0,
            "Check list router response"
        )

        router = routers[0]

        self.debug("Stopping router with ID: %s" % router.id)
        # Stop the router
        cmd = stopRouter.stopRouterCmd()
        cmd.id = router.id
        self.apiclient.stopRouter(cmd)

        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "Check for list routers response return valid data"
        )
        router = routers[0]

        self.assertEqual(
            router.state,
            'Stopped',
            "Check list router response for router state"
        )

        public_ips = list_publicIP(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(public_ips, list),
            True,
            "Check for list public IPs response return valid data"
        )
        public_ip = public_ips[0]

        # Open up firewall port for SSH
        FireWallRule.create(
            self.apiclient,
            ipaddressid=public_ip.id,
            protocol=self.services["lbrule"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["lbrule"]["publicport"],
            endport=self.services["lbrule"]["publicport"]
        )
        self.debug("Creating LB rule for public IP: %s" % public_ip.id)
        # Create Load Balancer rule and assign VMs to rule
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.services["lbrule"],
            public_ip.id,
            accountid=self.account.name
        )
        self.debug("Assigning VM %s to LB rule: %s" % (
            self.vm_1.id,
            lb_rule.id
        ))
        lb_rule.assign(self.apiclient, [self.vm_1])

        # Start the router
        cmd = startRouter.startRouterCmd()
        cmd.id = router.id
        self.apiclient.startRouter(cmd)

        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "Check for list routers response return valid data"
        )
        router = routers[0]

        self.assertEqual(
            router.state,
            'Running',
            "Check list router response for router state"
        )
        # After router start, LB RUle should be in Active state
        lb_rules = list_lb_rules(
            self.apiclient,
            id=lb_rule.id
        )
        self.assertEqual(
            isinstance(lb_rules, list),
            True,
            "Check for list LB rules response return valid data"
        )
        self.assertEqual(
            lb_rules[0].state,
            'Active',
            "Check list load balancing rules"
        )
        self.assertEqual(
            lb_rules[0].publicport,
            str(self.services["lbrule"]["publicport"]),
            "Check list load balancing rules"
        )

        try:
            self.debug("SSH into VM with IP: %s" % public_ip.ipaddress)
            self.vm_1.ssh_port = self.services["lbrule"]["publicport"]
            self.vm_1.get_ssh_client(public_ip.ipaddress)
        except Exception as e:
            self.fail(
                "SSH Access failed for %s: %s" %
                (self.vm_1.ipaddress, e)
            )
        return


class TestRouterStopCreateFW(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestRouterStopCreateFW, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        # Create an account, network, VM and IP addresses
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls.vm_1 = VirtualMachine.create(
            cls.api_client,
            cls.services["virtual_machine"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls._cleanup = [
            cls.account,
            cls.service_offering
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(
                TestRouterStopCreateFW,
                cls).getClsTestClient().getApiClient()
            # Clean up, terminate the created templates
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.cleanup = []
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    def test_01_RouterStopCreateFW(self):
        """Test router stop create Firewall rule
        """
        # validate the following
        # 1. 1. listFirewallRules (filter by ipaddressid of sourcenat)
        # 2. rule should be for ports 1-600 and in state=Active
        #    (optional backend)
        # 3. verify on router using iptables -t nat -nvx if rules are applied

        # Get the router details associated with account
        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
        )

        self.assertEqual(
            isinstance(routers, list),
            True,
            "Check for list routers response return valid data"
        )

        self.assertNotEqual(
            len(routers),
            0,
            "Check list router response"
        )

        router = routers[0]

        self.debug("Stopping the router: %s" % router.id)
        # Stop the router
        cmd = stopRouter.stopRouterCmd()
        cmd.id = router.id
        self.apiclient.stopRouter(cmd)

        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "Check for list routers response return valid data"
        )
        router = routers[0]

        self.assertEqual(
            router.state,
            'Stopped',
            "Check list router response for router state"
        )

        public_ips = list_publicIP(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(public_ips, list),
            True,
            "Check for list public IP response return valid data"
        )
        public_ip = public_ips[0]

        # Create Firewall rule with configurations from settings file
        fw_rule = FireWallRule.create(
            self.apiclient,
            ipaddressid=public_ip.id,
            protocol='TCP',
            cidrlist=[self.services["fwrule"]["cidr"]],
            startport=self.services["fwrule"]["startport"],
            endport=self.services["fwrule"]["endport"]
        )
        self.debug("Created firewall rule: %s" % fw_rule.id)

        self.debug("Starting the router: %s" % router.id)
        # Start the router
        cmd = startRouter.startRouterCmd()
        cmd.id = router.id
        self.apiclient.startRouter(cmd)

        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "Check for list routers response return valid data"
        )

        router = routers[0]

        self.assertEqual(
            router.state,
            'Running',
            "Check list router response for router state"
        )
        # After Router start, FW rule should be in Active state
        fw_rules = list_firewall_rules(
            self.apiclient,
            id=fw_rule.id,
        )
        self.assertEqual(
            isinstance(fw_rules, list),
            True,
            "Check for list FW rules response return valid data"
        )

        self.assertEqual(
            fw_rules[0].state,
            'Active',
            "Check list load balancing rules"
        )
        self.assertEqual(
            fw_rules[0].startport,
            str(self.services["fwrule"]["startport"]),
            "Check start port of firewall rule"
        )

        self.assertEqual(
            fw_rules[0].endport,
            str(self.services["fwrule"]["endport"]),
            "Check end port of firewall rule"
        )
        # For DNS and DHCP check 'dnsmasq' process status
        if (self.hypervisor.lower() == 'vmware'
                or self.hypervisor.lower() == 'hyperv'):
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                router.linklocalip,
                'iptables -t nat -L',
                hypervisor=self.hypervisor
            )
        else:
            hosts = list_hosts(
                self.apiclient,
                id=router.hostid,
            )
            self.assertEqual(
                isinstance(hosts, list),
                True,
                "Check for list hosts response return valid data"
            )
            host = hosts[0]
            host.user = self.services["configurableData"]["host"]["username"]
            host.passwd = self.services["configurableData"]["host"]["password"]
            try:
                result = get_process_status(
                    host.ipaddress,
                    22,
                    host.user,
                    host.passwd,
                    router.linklocalip,
                    'iptables -t nat -L'
                )
            except KeyError:
                self.skipTest(
                    "Provide a marvin config file with host\
                            credentials to run %s" %
                    self._testMethodName)

        self.debug("iptables -t nat -L: %s" % result)
        self.debug("Public IP: %s" % public_ip.ipaddress)
        res = str(result)
        self.assertEqual(
            res.count(str(public_ip.ipaddress)),
            1,
            "Check public IP address"
        )

        return
