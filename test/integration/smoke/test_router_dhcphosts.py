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
                             ServiceOffering,
                             NATRule,
                             NetworkACL,
                             FireWallRule,
                             PublicIPAddress,
                             NetworkOffering,
                             Network,
                             Router)
from marvin.lib.common import (get_zone,
                               get_test_template,
                               get_domain,
                               list_virtual_machines,
                               list_networks,
                               list_configurations,
                               list_routers,
                               list_nat_rules,
                               list_publicIP,
                               list_firewall_rules,
                               list_hosts)

# Import System modules
import time
import logging


class TestRouterDHCPHosts(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.logger = logging.getLogger('TestRouterDHCPHosts')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        cls.testClient = super(TestRouterDHCPHosts, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls._cleanup = []

        cls.services = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype
        cls.template = get_test_template(
            cls.api_client,
            cls.zone.id,
            cls.hypervisor
        )
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.logger.debug("Creating Admin Account for domain %s on zone %s" % (cls.domain.id, cls.zone.id))
        # Create an account, network, VM and IP addresses
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.logger.debug("Creating Service Offering on zone %s" % (cls.zone.id))
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)

        cls.services["isolated_network_offering"]["egress_policy"] = "true"

        cls.logger.debug("Creating Network Offering on zone %s" % (cls.zone.id))
        cls.network_offering = NetworkOffering.create(cls.api_client,
                                                       cls.services["isolated_network_offering"],
                                                       conservemode=True)
        cls._cleanup.append(cls.network_offering)

        cls.network_offering.update(cls.api_client, state='Enabled')

        cls.logger.debug("Creating Network for Account %s using offering %s" % (cls.account.name, cls.network_offering.id))
        cls.network = Network.create(cls.api_client,
                                      cls.services["network"],
                                      accountid=cls.account.name,
                                      domainid=cls.account.domainid,
                                      networkofferingid=cls.network_offering.id,
                                      zoneid=cls.zone.id)
        cls._cleanup.append(cls.network)

        cls.logger.debug("Creating VM1 for Account %s using offering %s with IP 10.1.1.50" % (cls.account.name, cls.service_offering.id))
        cls.vm_1 = VirtualMachine.create(cls.api_client,
                                         cls.services["virtual_machine"],
                                         templateid=cls.template.id,
                                         accountid=cls.account.name,
                                         domainid=cls.domain.id,
                                         serviceofferingid=cls.service_offering.id,
                                         networkids=[str(cls.network.id)],
                                         ipaddress="10.1.1.50")
        cls._cleanup.append(cls.vm_1)

        cls.logger.debug("Creating VM2 for Account %s using offering %s with IP 10.1.1.51" % (cls.account.name, cls.service_offering.id))
        cls.vm_2 = VirtualMachine.create(cls.api_client,
                                         cls.services["virtual_machine"],
                                         templateid=cls.template.id,
                                         accountid=cls.account.name,
                                         domainid=cls.domain.id,
                                         serviceofferingid=cls.service_offering.id,
                                         networkids=[str(cls.network.id)],
                                         ipaddress="10.1.1.51")
        cls._cleanup.append(cls.vm_2)

        cls.services["natrule1"] = {
            "privateport": 22,
            "publicport": 222,
            "protocol": "TCP"
        }

        cls.services["natrule2"] = {
            "privateport": 22,
            "publicport": 223,
            "protocol": "TCP"
        }

        cls.services["configurableData"] = {
            "host": {
                "port": 22
            },
            "input": "INPUT",
            "forward": "FORWARD"
        }

        return

    @classmethod
    def tearDownClass(cls):
        super(TestRouterDHCPHosts, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestRouterDHCPHosts, self).tearDown()

    def test_ssh_command(self, vm, nat_rule, rule_label):
        result = 'failed'
        try:
            ssh_command = "ping -c 3 8.8.8.8"
            self.logger.debug("SSH into VM with IP: %s" % nat_rule.ipaddress)

            ssh = vm.get_ssh_client(ipaddress=nat_rule.ipaddress, port=self.services[rule_label]["publicport"], retries=5)
            result = str(ssh.execute(ssh_command))

            self.logger.debug("SSH result: %s; COUNT is ==> %s" % (result, result.count(" 0% packet loss")))
        except:
            self.fail("Failed to SSH into VM - %s" % (nat_rule.ipaddress))

        self.assertEqual(
                         result.count(" 0% packet loss"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )

    def test_dhcphosts(self, vm, router):
        hosts = list_hosts(
            self.apiclient,
            id=router.hostid)

        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check for list hosts response return valid data")

        host = hosts[0]
        host.user = self.hostConfig['username']
        host.passwd = self.hostConfig['password']
        host.port = self.services["configurableData"]["host"]["port"]

        if self.hypervisor.lower() in ('vmware', 'hyperv'):
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                router.linklocalip,
                "cat /etc/dhcphosts.txt | grep %s | sed 's/\,/ /g' | awk '{print $2}'" % (vm.nic[0].ipaddress),
                hypervisor=self.hypervisor)
        else:
            try:
                result = get_process_status(
                    host.ipaddress,
                    host.port,
                    host.user,
                    host.passwd,
                    router.linklocalip,
                    "cat /etc/dhcphosts.txt | grep %s | sed 's/\,/ /g' | awk '{print $2}'" % (vm.nic[0].ipaddress))
            except KeyError:
                self.skipTest(
                    "Provide a marvin config file with host\
                            credentials to run %s" %
                    self._testMethodName)

        self.logger.debug("cat /etc/dhcphosts.txt | grep %s | sed 's/\,/ /g' | awk '{print $2}' RESULT IS ==> %s" % (vm.nic[0].ipaddress, result))
        res = str(result)

        self.assertEqual(
            res.count(vm.nic[0].ipaddress),
            1,
            "DHCP hosts file contains duplicate IPs ==> %s!" % res)

    @attr(tags=["advanced", "advancedns", "ssh"], required_hardware="true")
    def test_router_dhcphosts(self):
        """Check that the /etc/dhcphosts.txt doesn't contain duplicate IPs"""

        self.logger.debug("Starting test_router_dhcphosts...")
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

        self.assertEqual(
            router.state,
            'Running',
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

        self.logger.debug("Creating Firewall rule for VM ID: %s" % self.vm_1.id)
        fwr1 = FireWallRule.create(
            self.apiclient,
            ipaddressid=public_ip.id,
            protocol=self.services["natrule1"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["natrule1"]["publicport"],
            endport=self.services["natrule1"]["publicport"]
        )
        self.cleanup.append(fwr1)

        self.logger.debug("Creating NAT rule for VM ID: %s" % self.vm_1.id)
        # Create NAT rule
        nat_rule1 = NATRule.create(
            self.apiclient,
            self.vm_1,
            self.services["natrule1"],
            public_ip.id
        )
        self.cleanup.append(nat_rule1)

        self.logger.debug("Creating Firewall rule for VM ID: %s" % self.vm_2.id)
        fwr2 = FireWallRule.create(
            self.apiclient,
            ipaddressid=public_ip.id,
            protocol=self.services["natrule2"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["natrule2"]["publicport"],
            endport=self.services["natrule2"]["publicport"]
        )
        self.cleanup.append(fwr2)

        self.logger.debug("Creating NAT rule for VM ID: %s" % self.vm_2.id)
        # Create NAT rule
        nat_rule2 = NATRule.create(
            self.apiclient,
            self.vm_2,
            self.services["natrule2"],
            public_ip.id
        )
        self.cleanup.append(nat_rule2)

        nat_rules = list_nat_rules(
            self.apiclient,
            id=nat_rule1.id
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

        nat_rules = list_nat_rules(
            self.apiclient,
            id=nat_rule2.id
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

        self.logger.debug("Testing SSH to VMs %s and %s" % (self.vm_1.id, self.vm_2.id))
        self.test_ssh_command(self.vm_1, nat_rule1, "natrule1")
        self.test_ssh_command(self.vm_2, nat_rule2, "natrule2")

        self.logger.debug("Testing DHCP hosts for VMs %s and %s" % (self.vm_1.id, self.vm_2.id))
        self.test_dhcphosts(self.vm_1, router)
        self.test_dhcphosts(self.vm_2, router)

        #TODO vm1 should not be appended/created at class level
        self.logger.debug("Deleting and Expunging VM %s with ip %s" % (self.vm_1.id, self.vm_1.nic[0].ipaddress))
        self.vm_1.delete(self.apiclient)
        self._cleanup.remove(self.vm_1)
        self.cleanup.remove(nat_rule1)

        self.logger.debug("Creating new VM using the same IP as the one which was deleted => IP 10.1.1.50")
        self.vm_1 = VirtualMachine.create(self.apiclient,
                                         self.services["virtual_machine"],
                                         templateid=self.template.id,
                                         accountid=self.account.name,
                                         domainid=self.domain.id,
                                         serviceofferingid=self.service_offering.id,
                                         networkids=[str(self.network.id)],
                                         ipaddress="10.1.1.50")
        self.cleanup.append(self.vm_1)

        self.logger.debug("Testing DHCP hosts for VMs %s and %s" % (self.vm_1.id, self.vm_2.id))
        self.test_dhcphosts(self.vm_1, router)
        self.test_dhcphosts(self.vm_2, router)

        return


class TestRouterDHCPOpts(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.logger = logging.getLogger('TestRouterDHCPOpts')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        cls.testClient = super(TestRouterDHCPOpts, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls._cleanup = []

        cls.services = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype
        cls.template = get_test_template(
            cls.api_client,
            cls.zone.id,
            cls.hypervisor
        )
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.logger.debug("Creating Admin Account for domain %s on zone %s" % (cls.domain.id, cls.zone.id))
        # Create an account, network, VM and IP addresses
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.logger.debug("Creating Service Offering on zone %s" % (cls.zone.id))
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)

        cls.services["isolated_network_offering"]["egress_policy"] = "true"

        cls.logger.debug("Creating Network Offering on zone %s" % (cls.zone.id))
        cls.network_offering = NetworkOffering.create(cls.api_client,
                                                       cls.services["isolated_network_offering"],
                                                       conservemode=True)
        cls._cleanup.append(cls.network_offering)

        cls.network_offering.update(cls.api_client, state='Enabled')

        cls.logger.debug("Creating Network for Account %s using offering %s" % (cls.account.name, cls.network_offering.id))
        cls.services["network"]["name"] = "Test Network 1"
        cls.services["network"]["gateway"] = "10.1.1.1"
        cls.services["network"]["netmask"] = "255.255.255.0"
        cls.network1 = Network.create(cls.api_client,
                                      cls.services["network"],
                                      accountid=cls.account.name,
                                      domainid=cls.account.domainid,
                                      networkofferingid=cls.network_offering.id,
                                      zoneid=cls.zone.id)
        cls._cleanup.append(cls.network1)
        cls.services["network"]["name"] = "Test Network 2"
        cls.services["network"]["gateway"] = "10.1.2.1"
        cls.services["network"]["netmask"] = "255.255.255.0"
        cls.network2 = Network.create(cls.api_client,
                                      cls.services["network"],
                                      accountid=cls.account.name,
                                      domainid=cls.account.domainid,
                                      networkofferingid=cls.network_offering.id,
                                      zoneid=cls.zone.id)
        cls._cleanup.append(cls.network2)
        cls.logger.debug("Creating VM1 for Account %s using offering %s with IP 10.1.1.50" % (cls.account.name, cls.service_offering.id))
        cls.vm_1 = VirtualMachine.create(cls.api_client,
                                         cls.services["virtual_machine"],
                                         templateid=cls.template.id,
                                         accountid=cls.account.name,
                                         domainid=cls.domain.id,
                                         serviceofferingid=cls.service_offering.id,
                                         networkids=[str(cls.network1.id),str(cls.network2.id)])
        cls._cleanup.append(cls.vm_1)

        cls.logger.debug("Creating VM2 for Account %s using offering %s with IP 10.1.1.51" % (cls.account.name, cls.service_offering.id))
        cls.vm_2 = VirtualMachine.create(cls.api_client,
                                         cls.services["virtual_machine"],
                                         templateid=cls.template.id,
                                         accountid=cls.account.name,
                                         domainid=cls.domain.id,
                                         serviceofferingid=cls.service_offering.id,
                                         networkids=[str(cls.network2.id),str(cls.network1.id)])
        cls._cleanup.append(cls.vm_2)

        cls.services["natrule1"] = {
            "privateport": 22,
            "publicport": 222,
            "protocol": "TCP"
        }

        cls.services["natrule2"] = {
            "privateport": 22,
            "publicport": 223,
            "protocol": "TCP"
        }

        cls.services["configurableData"] = {
            "host": {
                "port": 22
            },
            "input": "INPUT",
            "forward": "FORWARD"
        }

        return

    @classmethod
    def tearDownClass(cls):
        super(TestRouterDHCPOpts, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestRouterDHCPOpts, self).tearDown()

    def test_dhcphopts(self, ipaddress, router):
        hosts = list_hosts(
            self.apiclient,
            id=router.hostid)

        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check for list hosts response return valid data")

        host = hosts[0]
        host.user = self.hostConfig['username']
        host.passwd = self.hostConfig['password']
        host.port = self.services["configurableData"]["host"]["port"]

        host_tag = ipaddress.replace(".","_")
        if self.hypervisor.lower() in ('vmware', 'hyperv'):
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                router.linklocalip,
                "cat /etc/dhcpopts.txt | grep %s" % (host_tag),
                hypervisor=self.hypervisor)
        else:
            try:
                result = get_process_status(
                    host.ipaddress,
                    host.port,
                    host.user,
                    host.passwd,
                    router.linklocalip,
                    "cat /etc/dhcpopts.txt | grep %s" % (host_tag))
            except KeyError:
                self.skipTest(
                    "Provide a marvin config file with host\
                            credentials to run %s" %
                    self._testMethodName)

        self.logger.debug("cat /etc/dhcpopts.txt | grep %s RESULT IS ==> %s" % (host_tag, result))
        res = str(result)

        self.assertNotEqual(
            res.count(host_tag),
            0,
            "DHCP opts file does not contain exception for IP ==> %s!" % res)

    @attr(tags=["advanced", "advancedns", "ssh"], required_hardware="true")
    def test_router_dhcp_opts(self):
        """Check that the /etc/dhcpopts.txt has entries for the"""

        self.logger.debug("Starting test_router_dhcphosts...")
        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            networkid=self.network1.id
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
        network1_router = routers[0]

        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            networkid=self.network2.id
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
        network2_router = routers[0]

        self.assertEqual(
            network1_router.state,
            'Running',
            "Check list router response for router state"
        )
        self.assertEqual(
            network2_router.state,
            'Running',
            "Check list router response for router state"
        )
        public_ips = list_publicIP(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            associatednetworkid=self.network1.id
        )

        self.assertEqual(
            isinstance(public_ips, list),
            True,
            "Check for list public IPs response return valid data"
        )

        network1_public_ip = public_ips[0]

        public_ips = list_publicIP(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            associatednetworkid=self.network2.id
        )

        self.assertEqual(
            isinstance(public_ips, list),
            True,
            "Check for list public IPs response return valid data"
        )
        network2_public_ip = public_ips[0]

        self.logger.debug("Creating Firewall rule for VM ID: %s" % self.vm_1.id)
        fwr1 = FireWallRule.create(
            self.apiclient,
            ipaddressid=network1_public_ip.id,
            protocol=self.services["natrule1"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["natrule1"]["publicport"],
            endport=self.services["natrule1"]["publicport"]
        )
        self.cleanup.append(fwr1)

        self.logger.debug("Creating NAT rule for VM ID: %s" % self.vm_1.id)
        # Create NAT rule
        nat_rule1 = NATRule.create(
            self.apiclient,
            self.vm_1,
            self.services["natrule1"],
            network1_public_ip.id
        )
        self.cleanup.append(nat_rule1)

        self.logger.debug("Creating Firewall rule for VM ID: %s" % self.vm_2.id)
        fwr2 = FireWallRule.create(
            self.apiclient,
            ipaddressid=network2_public_ip.id,
            protocol=self.services["natrule2"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["natrule2"]["publicport"],
            endport=self.services["natrule2"]["publicport"]
        )
        self.cleanup.append(fwr2)

        self.logger.debug("Creating NAT rule for VM ID: %s" % self.vm_2.id)
        # Create NAT rule
        nat_rule2 = NATRule.create(
            self.apiclient,
            self.vm_2,
            self.services["natrule2"],
            network2_public_ip.id
        )
        self.cleanup.append(nat_rule2)

        nat_rules = list_nat_rules(
            self.apiclient,
            id=nat_rule1.id
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

        nat_rules = list_nat_rules(
            self.apiclient,
            id=nat_rule2.id
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

        vm1_non_default_nic_ip = self.get_vm_non_default_nic_ip(self.vm_1)
        vm2_non_default_nic_ip = self.get_vm_non_default_nic_ip(self.vm_2)

        self.logger.debug("Testing DHCP options for VM's %s IP address %s in virtual router %s" % (self.vm_2.id, vm2_non_default_nic_ip, str(network1_router) ))
        self.test_dhcphopts(vm2_non_default_nic_ip, network1_router)

        self.logger.debug("Testing DHCP options for VM's %s IP address %s in virtual router %s" % (self.vm_1.id, vm1_non_default_nic_ip, str(network2_router) ))
        self.test_dhcphopts(vm1_non_default_nic_ip, network2_router)

        return

    def get_vm_non_default_nic_ip(self, vm):
        if vm.nic[0].isdefault:
            return vm.nic[1].ipaddress
        else:
            return vm.nic[0].ipaddress
