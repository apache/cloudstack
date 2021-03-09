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
from marvin.codes import PASS, FAILED
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (stopVirtualMachine,
                                  stopRouter,
                                  startRouter)
from marvin.lib.utils import (cleanup_resources,
                              get_process_status,
                              get_host_credentials)
from marvin.lib.base import (ServiceOffering,
                             VirtualMachine,
                             Account,
                             Template,
                             ServiceOffering,
                             NATRule,
                             NetworkACL,
                             FireWallRule,
                             PublicIPAddress,
                             NetworkOffering,
                             Network,
                             Router,
                             EgressFireWallRule)
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


def check_router_command(virtual_machine, public_ip, ssh_command, check_string, test_case, retries=5):
    result = 'failed'
    try:
        ssh = virtual_machine.get_ssh_client(ipaddress=public_ip, retries=retries)
        result = str(ssh.execute(ssh_command))
    except Exception as e:
        test_case.fail("Failed to SSH into the Virtual Machine: %s" % e)

    logging.debug("Result from SSH into the Virtual Machine: %s" % result)
    return result.count(check_string)


class TestRedundantIsolateNetworks(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.logger = logging.getLogger('TestRedundantIsolateNetworks')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        cls.testClient = super(TestRedundantIsolateNetworks, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        cls.hypervisor = cls.testClient.getHypervisorInfo()

        cls.template = get_test_template(cls.api_client, cls.zone.id, cls.hypervisor)
        if cls.template == FAILED:
            assert False, "get_test_template() failed to return template"

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

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

        cls.services["nw_off_persistent_RVR_egress_true"] = cls.services["nw_off_persistent_RVR"].copy()
        cls.services["nw_off_persistent_RVR_egress_true"]["egress_policy"] = "true"

        cls.services["nw_off_persistent_RVR_egress_false"] = cls.services["nw_off_persistent_RVR"].copy()
        cls.services["nw_off_persistent_RVR_egress_false"]["egress_policy"] = "false"

        cls.services["egress_80"] = {
                                    "startport": 80,
                                    "endport": 80,
                                    "protocol": "TCP",
                                    "cidrlist": ["0.0.0.0/0"]
                                    }

        cls.services["egress_53"] = {
                                    "startport": 53,
                                    "endport": 53,
                                    "protocol": "UDP",
                                    "cidrlist": ["0.0.0.0/0"]
                                    }

        cls._cleanup = [
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
            cleanup_resources(self.api_client, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "ssh"], required_hardware="true")
    def test_01_RVR_Network_FW_PF_SSH_default_routes_egress_true(self):
        """ Test redundant router internals """
        self.logger.debug("Starting test_01_RVR_Network_FW_PF_SSH_default_routes_egress_true...")

        self.logger.debug("Creating Network Offering with default egress TRUE")
        network_offering_egress_true = NetworkOffering.create(
                                            self.apiclient,
                                            self.services["nw_off_persistent_RVR_egress_true"],
                                            conservemode=True
                                            )
        network_offering_egress_true.update(self.api_client, state='Enabled')

        self.logger.debug("Creating network with network offering: %s" % network_offering_egress_true.id)
        network = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=network_offering_egress_true.id,
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

        self.cleanup.insert(0, network_offering_egress_true)
        self.cleanup.insert(0, network)
        self.cleanup.insert(0, virtual_machine)

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
                    "list router should return Primary and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & Primary)"
                    )

        public_ips = list_publicIP(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id
        )

        public_ip = public_ips[0]

        self.assertEqual(
            isinstance(public_ips, list),
            True,
            "Check for list public IPs response return valid data"
        )

        self.logger.debug("Creating Firewall rule for VM ID: %s" % virtual_machine.id)
        FireWallRule.create(
            self.apiclient,
            ipaddressid=public_ip.id,
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
            public_ip.id
        )

        # Test SSH after closing port 22
        expected = 1
        ssh_command = "ping -c 3 8.8.8.8"
        check_string = " 0% packet loss"
        result = check_router_command(virtual_machine, nat_rule.ipaddress, ssh_command, check_string, self)

        self.assertEqual(
                         result,
                         expected,
                         "Ping to outside world from VM should be successful!"
                         )

        expected = 1
        ssh_command = "wget -t 1 -T 5 www.google.com"
        check_string = "HTTP request sent, awaiting response... 200 OK"
        result = check_router_command(virtual_machine, nat_rule.ipaddress, ssh_command, check_string, self)

        self.assertEqual(
                         result,
                         expected,
                         "Attempt to retrieve google.com index page should be successful!"
                         )

        EgressFireWallRule.create(
                                 self.apiclient,
                                 networkid=network.id,
                                 protocol=self.services["egress_80"]["protocol"],
                                 startport=self.services["egress_80"]["startport"],
                                 endport=self.services["egress_80"]["endport"],
                                 cidrlist=self.services["egress_80"]["cidrlist"]
                                 )

        expected = 0
        ssh_command = "wget -t 1 -T 1 www.google.com"
        check_string = "HTTP request sent, awaiting response... 200 OK"
        result = check_router_command(virtual_machine, nat_rule.ipaddress, ssh_command, check_string, self)

        self.assertEqual(
                         result,
                         expected,
                         "Attempt to retrieve google.com index page should NOT be successful once rule is added!"
                         )

        return

    @attr(tags=["advanced", "advancedns", "ssh"], required_hardware="true")
    def test_02_RVR_Network_FW_PF_SSH_default_routes_egress_false(self):
        """ Test redundant router internals """
        self.logger.debug("Starting test_02_RVR_Network_FW_PF_SSH_default_routes_egress_false...")

        self.logger.debug("Creating Network Offering with default egress FALSE")
        network_offering_egress_false = NetworkOffering.create(
                                            self.apiclient,
                                            self.services["nw_off_persistent_RVR_egress_false"],
                                            conservemode=True
                                            )
        network_offering_egress_false.update(self.api_client, state='Enabled')

        self.logger.debug("Creating network with network offering: %s" % network_offering_egress_false.id)
        network = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=network_offering_egress_false.id,
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

        self.cleanup.insert(0, network_offering_egress_false)
        self.cleanup.insert(0, network)
        self.cleanup.insert(0, virtual_machine)

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
                    "list router should return Primary and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & Primary)"
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

        self.logger.debug("Creating Firewall rule for VM ID: %s" % virtual_machine.id)
        FireWallRule.create(
            self.apiclient,
            ipaddressid=public_ip.id,
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
            public_ip.id
        )

        expected = 0
        ssh_command = "ping -c 3 8.8.8.8"
        check_string = " 0% packet loss"
        result = check_router_command(virtual_machine, nat_rule.ipaddress, ssh_command, check_string, self)

        self.assertEqual(
                         result,
                         expected,
                         "Ping to outside world from VM should NOT be successful"
                         )

        expected = 0
        ssh_command = "wget -t 1 -T 1 www.google.com"
        check_string = "HTTP request sent, awaiting response... 200 OK"
        result = check_router_command(virtual_machine, nat_rule.ipaddress, ssh_command, check_string, self)

        self.assertEqual(
                         result,
                         expected,
                         "Attempt to retrieve google.com index page should NOT be successful"
                         )

        EgressFireWallRule.create(
                                 self.apiclient,
                                 networkid=network.id,
                                 protocol=self.services["egress_80"]["protocol"],
                                 startport=self.services["egress_80"]["startport"],
                                 endport=self.services["egress_80"]["endport"],
                                 cidrlist=self.services["egress_80"]["cidrlist"]
                                 )

        EgressFireWallRule.create(
                                 self.apiclient,
                                 networkid=network.id,
                                 protocol=self.services["egress_53"]["protocol"],
                                 startport=self.services["egress_53"]["startport"],
                                 endport=self.services["egress_53"]["endport"],
                                 cidrlist=self.services["egress_53"]["cidrlist"]
                                 )

        expected = 1
        ssh_command = "wget -t 1 -T 5 www.google.com"
        check_string = "HTTP request sent, awaiting response... 200 OK"
        result = check_router_command(virtual_machine, nat_rule.ipaddress, ssh_command, check_string, self)

        self.assertEqual(
                         result,
                         expected,
                         "Attempt to retrieve google.com index page should be successful once rule is added!"
                         )

        return

    @attr(tags=["advanced", "advancedns", "ssh"], required_hardware="true")
    def test_03_RVR_Network_check_router_state(self):
        """ Test redundant router internals """
        self.logger.debug("Starting test_03_RVR_Network_check_router_state...")

        hypervisor = self.testClient.getHypervisorInfo()

        self.logger.debug("Creating Network Offering with default egress FALSE")
        network_offering_egress_false = NetworkOffering.create(
                                            self.apiclient,
                                            self.services["nw_off_persistent_RVR_egress_false"],
                                            conservemode=True
                                            )
        network_offering_egress_false.update(self.apiclient, state='Enabled')

        self.logger.debug("Creating network with network offering: %s" % network_offering_egress_false.id)
        network = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=network_offering_egress_false.id,
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

        self.cleanup.insert(0, network_offering_egress_false)
        self.cleanup.insert(0, network)
        self.cleanup.insert(0, virtual_machine)

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
                    "list router should return Primary and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & Primary)"
                    )

        vals = ["PRIMARY", "BACKUP", "UNKNOWN"]
        cnts = [0, 0, 0]

        result = "UNKNOWN"
        for router in routers:
            if router.state == "Running":
                hosts = list_hosts(
                    self.apiclient,
                    zoneid=router.zoneid,
                    type='Routing',
                    state='Up',
                    id=router.hostid
                )
                self.assertEqual(
                    isinstance(hosts, list),
                    True,
                    "Check list host returns a valid list"
                )
                host = hosts[0]

                if hypervisor.lower() in ('vmware', 'hyperv'):
                        result = str(get_process_status(
                            self.apiclient.connection.mgtSvr,
                            22,
                            self.apiclient.connection.user,
                            self.apiclient.connection.passwd,
                            router.linklocalip,
                            "sh /opt/cloud/bin/checkrouter.sh ",
                            hypervisor=hypervisor
                        ))
                else:
                    try:
                        host.user, host.passwd = get_host_credentials(
                            self.config, host.ipaddress)
                        result = str(get_process_status(
                            host.ipaddress,
                            22,
                            host.user,
                            host.passwd,
                            router.linklocalip,
                            "sh /opt/cloud/bin/checkrouter.sh "
                        ))

                    except KeyError:
                        self.skipTest(
                            "Marvin configuration has no host credentials to\
                                    check router services")
            
                if result.count(vals[0]) == 1:
                    cnts[vals.index(vals[0])] += 1

        if cnts[vals.index('PRIMARY')] != 1:
            self.fail("No Primary or too many primary routers found %s" % cnts[vals.index('PRIMARY')])

        return


class TestIsolatedNetworks(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.logger = logging.getLogger('TestIsolatedNetworks')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)
        
        cls.testClient = super(TestIsolatedNetworks, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

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

        cls.services["network_offering_egress_true"] = cls.services["network_offering"].copy()
        cls.services["network_offering_egress_true"]["egress_policy"] = "true"

        cls.services["network_offering_egress_false"] = cls.services["network_offering"].copy()
        cls.services["network_offering_egress_false"]["egress_policy"] = "false"

        cls.services["egress_80"] = {
                                    "startport": 80,
                                    "endport": 80,
                                    "protocol": "TCP",
                                    "cidrlist": ["0.0.0.0/0"]
                                    }

        cls._cleanup = [
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

    @attr(tags=["advanced", "advancedns", "ssh"], required_hardware="true")
    def test_01_isolate_network_FW_PF_default_routes_egress_true(self):
        """ Test redundant router internals """
        self.logger.debug("Starting test_01_isolate_network_FW_PF_default_routes_egress_true...")

        self.logger.debug("Creating Network Offering with default egress TRUE")
        network_offering_egress_true = NetworkOffering.create(self.apiclient,
                                                       self.services["network_offering_egress_true"],
                                                       conservemode=True)

        network_offering_egress_true.update(self.apiclient, state='Enabled')

        self.logger.debug("Creating Network with Network Offering ID %s" % network_offering_egress_true.id)
        network = Network.create(self.apiclient,
                                      self.services["network"],
                                      accountid=self.account.name,
                                      domainid=self.account.domainid,
                                      networkofferingid=network_offering_egress_true.id,
                                      zoneid=self.zone.id)

        self.logger.debug("Deploying Virtual Machine on Network %s" % network.id)
        virtual_machine = VirtualMachine.create(self.apiclient,
                                         self.services["virtual_machine"],
                                         templateid=self.template.id,
                                         accountid=self.account.name,
                                         domainid=self.domain.id,
                                         serviceofferingid=self.service_offering.id,
                                         networkids=[str(network.id)])

        self.logger.debug("Deployed VM in network: %s" % network.id)

        self.cleanup.insert(0, network_offering_egress_true)
        self.cleanup.insert(0, network)
        self.cleanup.insert(0, virtual_machine)

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

        self.logger.debug("Creating Firewall rule for VM ID: %s" % virtual_machine.id)
        FireWallRule.create(
            self.apiclient,
            ipaddressid=public_ip.id,
            protocol=self.services["natrule"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["natrule"]["publicport"],
            endport=self.services["natrule"]["publicport"]
        )

        self.logger.debug("Creating NAT rule for VM ID: %s" % virtual_machine.id)
        # Create NAT rule
        nat_rule = NATRule.create(
            self.apiclient,
            virtual_machine,
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

        # Test SSH after closing port 22
        expected = 1
        ssh_command = "ping -c 3 8.8.8.8"
        check_string = " 0% packet loss"
        result = check_router_command(virtual_machine, nat_rule.ipaddress, ssh_command, check_string, self)

        self.assertEqual(
                         result,
                         expected,
                         "Ping to outside world from VM should be successful!"
                         )

        expected = 1
        ssh_command = "wget -t 1 -T 5 www.google.com"
        check_string = "HTTP request sent, awaiting response... 200 OK"
        result = check_router_command(virtual_machine, nat_rule.ipaddress, ssh_command, check_string, self)

        self.assertEqual(
                         result,
                         expected,
                         "Attempt to retrieve google.com index page should be successful!"
                         )

        EgressFireWallRule.create(
                                 self.apiclient,
                                 networkid=network.id,
                                 protocol=self.services["egress_80"]["protocol"],
                                 startport=self.services["egress_80"]["startport"],
                                 endport=self.services["egress_80"]["endport"],
                                 cidrlist=self.services["egress_80"]["cidrlist"]
                                 )

        expected = 0
        ssh_command = "wget -t 1 -T 1 www.google.com"
        check_string = "HTTP request sent, awaiting response... 200 OK"
        result = check_router_command(virtual_machine, nat_rule.ipaddress, ssh_command, check_string, self)

        self.assertEqual(
                         result,
                         expected,
                         "Attempt to retrieve google.com index page should NOT be successful once rule is added!"
                         )

        return

    @attr(tags=["advanced", "advancedns", "ssh"], required_hardware="true")
    def test_02_isolate_network_FW_PF_default_routes_egress_false(self):
        """ Test redundant router internals """
        self.logger.debug("Starting test_02_isolate_network_FW_PF_default_routes_egress_false...")

        self.logger.debug("Creating Network Offering with default egress FALSE")
        network_offering_egress_false = NetworkOffering.create(self.apiclient,
                                                       self.services["network_offering_egress_false"],
                                                       conservemode=True)

        network_offering_egress_false.update(self.apiclient, state='Enabled')

        self.logger.debug("Creating Network with Network Offering ID %s" % network_offering_egress_false.id)
        network = Network.create(self.apiclient,
                                      self.services["network"],
                                      accountid=self.account.name,
                                      domainid=self.account.domainid,
                                      networkofferingid=network_offering_egress_false.id,
                                      zoneid=self.zone.id)

        self.logger.debug("Deploying Virtual Machine on Network %s" % network.id)
        virtual_machine = VirtualMachine.create(self.apiclient,
                                         self.services["virtual_machine"],
                                         templateid=self.template.id,
                                         accountid=self.account.name,
                                         domainid=self.domain.id,
                                         serviceofferingid=self.service_offering.id,
                                         networkids=[str(network.id)])

        self.logger.debug("Deployed VM in network: %s" % network.id)

        self.cleanup.insert(0, network_offering_egress_false)
        self.cleanup.insert(0, network)
        self.cleanup.insert(0, virtual_machine)

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

        self.logger.debug("Creating Firewall rule for VM ID: %s" % virtual_machine.id)
        FireWallRule.create(
            self.apiclient,
            ipaddressid=public_ip.id,
            protocol=self.services["natrule"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["natrule"]["publicport"],
            endport=self.services["natrule"]["publicport"]
        )

        self.logger.debug("Creating NAT rule for VM ID: %s" % virtual_machine.id)
        # Create NAT rule
        nat_rule = NATRule.create(
            self.apiclient,
            virtual_machine,
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

        expected = 0
        ssh_command = "ping -c 3 8.8.8.8"
        check_string = " 0% packet loss"
        result = check_router_command(virtual_machine, nat_rule.ipaddress, ssh_command, check_string, self)

        self.assertEqual(
                         result,
                         expected,
                         "Ping to outside world from VM should NOT be successful"
                         )

        expected = 0
        ssh_command = "wget -t 1 -T 1 www.google.com"
        check_string = "HTTP request sent, awaiting response... 200 OK"
        result = check_router_command(virtual_machine, nat_rule.ipaddress, ssh_command, check_string, self)

        self.assertEqual(
                         result,
                         expected,
                         "Attempt to retrieve google.com index page should NOT be successful"
                         )

        EgressFireWallRule.create(
                                 self.apiclient,
                                 networkid=network.id,
                                 protocol=self.services["egress_80"]["protocol"],
                                 startport=self.services["egress_80"]["startport"],
                                 endport=self.services["egress_80"]["endport"],
                                 cidrlist=self.services["egress_80"]["cidrlist"]
                                 )

        expected = 1
        ssh_command = "wget -t 1 -T 5 www.google.com"
        check_string = "HTTP request sent, awaiting response... 200 OK"
        result = check_router_command(virtual_machine, nat_rule.ipaddress, ssh_command, check_string, self)

        self.assertEqual(
                         result,
                         expected,
                         "Attempt to retrieve google.com index page should be successful once rule is added!"
                         )

        return
