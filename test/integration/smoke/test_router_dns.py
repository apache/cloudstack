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

import logging
import dns.resolver

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (ServiceOffering,
                             VirtualMachine,
                             Account,
                             NATRule,
                             FireWallRule,
                             NetworkOffering,
                             Network)
from marvin.lib.common import (get_zone,
                               get_test_template,
                               get_domain,
                               list_routers,
                               list_nat_rules,
                               list_publicIP)


class TestRouterDns(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.logger = logging.getLogger('TestRouterDns')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        cls.testClient = super(TestRouterDns, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        cls.services['mode'] = cls.zone.networktype
        cls.template = get_test_template(
            cls.api_client,
            cls.zone.id,
            cls.hypervisor
        )

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.logger.debug("Creating Admin Account for domain %s on zone %s" % (cls.domain.id, cls.zone.id))
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

        cls.logger.debug("Creating Network Offering on zone %s" % (cls.zone.id))
        cls.services["isolated_network_offering"]["egress_policy"] = "true"
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

        cls.logger.debug("Creating guest VM for Account %s using offering %s" % (cls.account.name, cls.service_offering.id))
        cls.vm = VirtualMachine.create(cls.api_client,
                                         cls.services["virtual_machine"],
                                         templateid=cls.template.id,
                                         accountid=cls.account.name,
                                         domainid=cls.domain.id,
                                         serviceofferingid=cls.service_offering.id,
                                         networkids=[str(cls.network.id)])
        cls.vm.password = "password"

        cls.services["natrule1"] = {
            "privateport": 22,
            "publicport": 22,
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
            cls.vm,
            cls.network,
            cls.network_offering,
            cls.service_offering,
            cls.account
        ]


    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)


    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []


    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)


    def test_router_common(self):
        """Performs common router tests and returns router public_ips"""

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

        self.assertTrue(
            len(routers) >= 1,
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

        self.assertTrue(
            len(public_ips) >= 1,
            "Check public IP list has at least one IP"
        )

        return public_ips


    @attr(tags=["advanced", "advancedns", "ssh"], required_hardware="true")
    def test_router_dns_externalipquery(self):
        """Checks that non-guest network IPs cannot access VR DNS"""

        self.logger.debug("Starting test_router_dns_externalips...")

        public_ip = self.test_router_common()[0]

        self.logger.debug("Querying VR DNS IP: " + public_ip.ipaddress)
        resolver = dns.resolver.Resolver()
        resolver.namerservers = [public_ip.ipaddress]
        try:
            resolver.query('google.com', 'A')
            self.fail("Non-guest network IPs are able to access VR DNS, failing.")
        except:
            self.logger.debug("VR DNS query failed from non-guest network IP as expected")


    @attr(tags=["advanced", "advancedns", "ssh"], required_hardware="true")
    def test_router_dns_guestipquery(self):
        """Checks that guest VM can query VR DNS"""

        self.logger.debug("Starting test_router_dns_guestipquery...")
        public_ip = self.test_router_common()[0]

        self.logger.debug("Creating Firewall rule for VM ID: %s" % self.vm.id)
        FireWallRule.create(
            self.apiclient,
            ipaddressid=public_ip.id,
            protocol=self.services["natrule1"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["natrule1"]["publicport"],
            endport=self.services["natrule1"]["publicport"]
        )

        self.logger.debug("Creating NAT rule for VM ID: %s" % self.vm.id)
        nat_rule1 = NATRule.create(
            self.apiclient,
            self.vm,
            self.services["natrule1"],
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
        self.assertTrue(
            len(nat_rules) >= 1,
            "Check for list NAT rules to have at least one rule"
        )
        self.assertEqual(
            nat_rules[0].state,
            'Active',
            "Check list port forwarding rules"
        )

        result = None
        try:
            self.logger.debug("SSH into guest VM with IP: %s" % nat_rule1.ipaddress)
            ssh = self.vm.get_ssh_client(ipaddress=nat_rule1.ipaddress, port=self.services['natrule1']["publicport"], retries=15)
            result = str(ssh.execute("nslookup google.com"))
        except Exception as e:
            self.fail("Failed to SSH into VM - %s due to exception: %s" % (nat_rule1.ipaddress, e))

        if not result:
            self.fail("Did not to receive any response from the guest VM, failing.")

        self.assertTrue("google.com" in result and "10.1.1.1" in result,
                        "VR DNS should serve requests from guest network, unable to get valid nslookup result from guest VM.")
