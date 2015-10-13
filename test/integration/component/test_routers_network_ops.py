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

class TestRedundantIsolateNetworks(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestRedundantIsolateNetworks, cls).getClsTestClient()
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
        
        cls.services["nw_off_persistent_RVR"]["egress_policy"] = "true"
        
        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["nw_off_persistent_RVR"],
                                            conservemode=True
                                            )
        cls.network_offering.update(cls.api_client, state='Enabled')

        cls._cleanup = [
                        cls.service_offering,
                        cls.network_offering,
                        ]
        
        cls.logger = logging.getLogger('TestRedundantIsolateNetworks')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

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
            cleanup_resources(self.api_client, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "ssh"], required_hardware="true")
    def test_RVR_Network_FW_PF_SSH_default_routes(self):
        """ Test redundant router internals """
        self.logger.debug("Starting test_RVR_Network_FW_PF_SSH_default_routes...")

        self.logger.debug("Creating network with network offering: %s" % self.network_offering.id)
        network = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=self.network_offering.id,
                                zoneid=self.zone.id
                                )
        self.logger.debug("Created network with ID: %s" % network.id)

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

        self.logger.debug("Deploying VM in account: %s" % self.account.name)
        virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  templateid=self.template.id,
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network.id)]
                                  )
        
        self.logger.debug("Deployed VM in network: %s" % network.id)

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=virtual_machine.id,
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
                         "VM should be in running state after deployment"
                         )

        self.logger.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )

        self.logger.debug("Associating public IP for network: %s" % network.name)
        public_ip = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.logger.debug("Associated %s with network %s" % (
                                        public_ip.ipaddress.ipaddress,
                                        network.id
                                        ))

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

        public_ip_1 = public_ips[0]

        self.logger.debug("Creating Firewall rule for VM ID: %s" % virtual_machine.id)
        FireWallRule.create(
            self.apiclient,
            ipaddressid=public_ip_1.id,
            protocol=self.services["natrule"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["natrule"]["publicport"],
            endport=self.services["natrule"]["publicport"]
        )

        self.logger.debug("Creating NAT rule for VM ID: %s" % virtual_machine.id)
        nat_rule = NATRule.create(
            self.apiclient,
            virtual_machine,
            self.services["natrule"],
            public_ip_1.id
        )

        self.cleanup.insert(0, network)
        self.cleanup.insert(0, virtual_machine)

        result = 'failed'
        try:
            ssh_command = "ping -c 3 8.8.8.8"
            ssh = virtual_machine.get_ssh_client(ipaddress=public_ip.ipaddress.ipaddress, retries=5)
            self.logger.debug("Ping to google.com from VM")

            result = str(ssh.execute(ssh_command))
            self.logger.debug("SSH result: %s; COUNT is ==> %s" % (result, result.count("3 packets received")))
        except:
            self.fail("Failed to SSH into VM - %s" % (public_ip.ipaddress.ipaddress))

        self.assertEqual(
                         result.count("3 packets received"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )

        return


class TestIsolatedNetworks(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestIsolatedNetworks, cls).getClsTestClient()
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
        
        cls.services["network_offering"]["egress_policy"] = "true"

        cls.network_offering = NetworkOffering.create(cls.api_client,
                                                       cls.services["network_offering"],
                                                       conservemode=True)

        cls.network_offering.update(cls.api_client, state='Enabled')
        
        cls.network = Network.create(cls.api_client,
                                      cls.services["network"],
                                      accountid=cls.account.name,
                                      domainid=cls.account.domainid,
                                      networkofferingid=cls.network_offering.id,
                                      zoneid=cls.zone.id)

        cls.vm_1 = VirtualMachine.create(cls.api_client,
                                         cls.services["virtual_machine"],
                                         templateid=template.id,
                                         accountid=cls.account.name,
                                         domainid=cls.domain.id,
                                         serviceofferingid=cls.service_offering.id,
                                         networkids=[str(cls.network.id)])

        cls._cleanup = [
            cls.vm_1,
            cls.network,
            cls.network_offering,
            cls.service_offering,
            cls.account
        ]
        
        cls.logger = logging.getLogger('TestIsolatedNetworks')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

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
        return

    @attr(tags=["advanced", "advancedns", "ssh"], required_hardware="true")
    def test_isolate_network_FW_PF_default_routes(self):
        """Stop existing router, add a PF rule and check we can access the VM """

        self.logger.debug("Starting test_isolate_network_FW_PF_default_routes...")
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
        
        result = 'failed'
        try:
            ssh_command = "ping -c 3 8.8.8.8"
            self.logger.debug("SSH into VM with ID: %s" % nat_rule.ipaddress)

            ssh = self.vm_1.get_ssh_client(ipaddress=nat_rule.ipaddress, port=self.services["natrule"]["publicport"], retries=5)
            result = str(ssh.execute(ssh_command))
            self.logger.debug("SSH result: %s; COUNT is ==> %s" % (result, result.count("3 packets received")))
        except:
            self.fail("Failed to SSH into VM - %s" % (public_ip.ipaddress.ipaddress))

        self.assertEqual(
                         result.count("3 packets received"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )
        return
