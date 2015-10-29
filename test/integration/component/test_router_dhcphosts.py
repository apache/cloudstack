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
                               get_template,
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

        cls.logger.debug("Creating Admin Account for domain %s on zone %s" % (cls.domain.id, cls.zone.id))
        # Create an account, network, VM and IP addresses
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )

        cls.logger.debug("Creating Service Offering on zone %s" % (cls.zone.id))
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )

        cls.services["isolated_network_offering"]["egress_policy"] = "true"

        cls.logger.debug("Creating Network Offering on zone %s" % (cls.zone.id))
        cls.network_offering = NetworkOffering.create(cls.api_client,
                                                       cls.services["isolated_network_offering"],
                                                       conservemode=True)

        cls.network_offering.update(cls.api_client, state='Enabled')

        cls.logger.debug("Creating Network for Account %s using offering %s" % (cls.account.name, cls.network_offering.id))
        cls.network = Network.create(cls.api_client,
                                      cls.services["network"],
                                      accountid=cls.account.name,
                                      domainid=cls.account.domainid,
                                      networkofferingid=cls.network_offering.id,
                                      zoneid=cls.zone.id)

        cls.logger.debug("Creating VM1 for Account %s using offering %s with IP 10.1.1.50" % (cls.account.name, cls.service_offering.id))
        cls.vm_1 = VirtualMachine.create(cls.api_client,
                                         cls.services["virtual_machine"],
                                         templateid=cls.template.id,
                                         accountid=cls.account.name,
                                         domainid=cls.domain.id,
                                         serviceofferingid=cls.service_offering.id,
                                         networkids=[str(cls.network.id)],
                                         ipaddress="10.1.1.50")

        cls.logger.debug("Creating VM2 for Account %s using offering %s with IP 10.1.1.51" % (cls.account.name, cls.service_offering.id))
        cls.vm_2 = VirtualMachine.create(cls.api_client,
                                         cls.services["virtual_machine"],
                                         templateid=cls.template.id,
                                         accountid=cls.account.name,
                                         domainid=cls.domain.id,
                                         serviceofferingid=cls.service_offering.id,
                                         networkids=[str(cls.network.id)],
                                         ipaddress="10.1.1.51")

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
                "password": "password",
                "username": "root",
                "port": 22
            },
            "input": "INPUT",
            "forward": "FORWARD"
        }

        cls._cleanup = [
            cls.vm_2,
            cls.network,
            cls.network_offering,
            cls.service_offering,
            cls.account
        ]

        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def test_ssh_command(self, vm, nat_rule, rule_label):
        result = 'failed'
        try:
            ssh_command = "ping -c 3 8.8.8.8"
            self.logger.debug("SSH into VM with IP: %s" % nat_rule.ipaddress)

            ssh = vm.get_ssh_client(ipaddress=nat_rule.ipaddress, port=self.services[rule_label]["publicport"], retries=5)
            result = str(ssh.execute(ssh_command))

            self.logger.debug("SSH result: %s; COUNT is ==> %s" % (result, result.count("3 packets received")))
        except:
            self.fail("Failed to SSH into VM - %s" % (nat_rule.ipaddress))

        self.assertEqual(
                         result.count("3 packets received"),
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
        host.user = self.services["configurableData"]["host"]["username"]
        host.passwd = self.services["configurableData"]["host"]["password"]
        host.port = self.services["configurableData"]["host"]["port"]
        #mac1,10.7.32.101,infinite
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
        FireWallRule.create(
            self.apiclient,
            ipaddressid=public_ip.id,
            protocol=self.services["natrule1"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["natrule1"]["publicport"],
            endport=self.services["natrule1"]["publicport"]
        )

        self.logger.debug("Creating NAT rule for VM ID: %s" % self.vm_1.id)
        # Create NAT rule
        nat_rule1 = NATRule.create(
            self.apiclient,
            self.vm_1,
            self.services["natrule1"],
            public_ip.id
        )

        self.logger.debug("Creating Firewall rule for VM ID: %s" % self.vm_2.id)
        FireWallRule.create(
            self.apiclient,
            ipaddressid=public_ip.id,
            protocol=self.services["natrule2"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["natrule2"]["publicport"],
            endport=self.services["natrule2"]["publicport"]
        )

        self.logger.debug("Creating NAT rule for VM ID: %s" % self.vm_2.id)
        # Create NAT rule
        nat_rule2 = NATRule.create(
            self.apiclient,
            self.vm_2,
            self.services["natrule2"],
            public_ip.id
        )

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

        self.logger.debug("Deleting and Expunging VM %s with ip %s" % (self.vm_1.id, self.vm_1.nic[0].ipaddress))
        self.vm_1.delete(self.apiclient)
        
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
