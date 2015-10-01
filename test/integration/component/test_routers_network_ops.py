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
import logging

class TestCreatePFOnStoppedRouter(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestCreatePFOnStoppedRouter, cls).getClsTestClient()
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
        
        cls.logger = logging.getLogger('TestCreatePFOnStoppedRouter')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(
                TestCreatePFOnStoppedRouter,
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
    def test_01_CreatePFOnStoppedRouter(self):
        """Stop existing router, add a PF rule and check we can access the VM """

        # Get router details associated for that account
        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
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

        self.logger.debug("Stopping router ID: %s" % router.id)

        # Stop the router
        cmd = stopRouter.stopRouterCmd()
        cmd.id = router.id
        self.apiclient.stopRouter(cmd)

        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
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

        self.logger.debug("Creating NAT rule for VM ID: %s" % self.vm_1.id)
        # Create NAT rule
        nat_rule = NATRule.create(
            self.apiclient,
            self.vm_1,
            self.services["natrule"],
            public_ip.id
        )

        self.logger.debug("Starting router ID: %s" % router.id)
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

            self.logger.debug("SSH into VM with ID: %s" % nat_rule.ipaddress)

            self.vm_1.get_ssh_client(
                ipaddress=nat_rule.ipaddress,
                port=self.services["natrule"]["publicport"])
        except Exception as e:
            self.fail(
                "SSH Access failed for %s: %s" %
                (nat_rule.ipaddress, e)
            )
        return


class TestCreateLBOnStoppedRouter(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestCreateLBOnStoppedRouter, cls).getClsTestClient()
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

        cls.logger = logging.getLogger('TestCreateLBOnStoppedRouter')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(
                TestCreateLBOnStoppedRouter,
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
    def test_01_CreateLBOnStoppedRouter(self):
        """Stop existing Router, add LB rule and check we can reach the VM"""

        # Get router details associated for that account
        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
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

        self.logger.debug("Stopping router with ID: %s" % router.id)
        # Stop the router
        cmd = stopRouter.stopRouterCmd()
        cmd.id = router.id
        self.apiclient.stopRouter(cmd)

        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
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
        self.logger.debug("Creating LB rule for public IP: %s" % public_ip.id)
        # Create Load Balancer rule and assign VMs to rule
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.services["lbrule"],
            public_ip.id,
            accountid=self.account.name
        )
        self.logger.debug("Assigning VM %s to LB rule: %s" % (
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
            domainid=self.account.domainid
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
            self.logger.debug("SSH into VM with IP: %s" % public_ip.ipaddress)
            self.vm_1.ssh_port = self.services["lbrule"]["publicport"]
            self.vm_1.get_ssh_client(public_ip.ipaddress)
        except Exception as e:
            self.fail(
                "SSH Access failed for %s: %s" %
                (self.vm_1.ipaddress, e)
            )
        return


class TestCreateFWOnStoppedRouter(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestCreateFWOnStoppedRouter, cls).getClsTestClient()
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

        cls.logger = logging.getLogger('TestCreateFWOnStoppedRouter')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(
                TestCreateFWOnStoppedRouter,
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
    def test_01_CreateFWOnStoppedRouter(self):
        """Stop existing Router, create Firewall rules and check that the rules are applied to the router"""

        # Get the router details associated with account
        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
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

        self.logger.debug("Stopping the router: %s" % router.id)
        # Stop the router
        cmd = stopRouter.stopRouterCmd()
        cmd.id = router.id
        self.apiclient.stopRouter(cmd)

        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
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
        self.logger.debug("Created firewall rule: %s" % fw_rule.id)

        self.logger.debug("Starting the router: %s" % router.id)
        # Start the router
        cmd = startRouter.startRouterCmd()
        cmd.id = router.id
        self.apiclient.startRouter(cmd)

        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
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

        self.logger.debug("iptables -t nat -L: %s" % result)
        self.logger.debug("Public IP: %s" % public_ip.ipaddress)
        res = str(result)
        self.assertEqual(
            res.count(str(public_ip.ipaddress)),
            1,
            "Check public IP address"
        )

        return
