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
""" BVT tests for network services on public IP's from different public IP
  range than that of associated source NAT IP of the network. Each IP associated
  with network from a different public IP range results in a new public
  interface on VR (eth3, eth4 etc) and iptable
"""
# Import Local Modules
from marvin.codes import (FAILED)
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackException import CloudstackAPIException
from marvin.cloudstackAPI import rebootRouter
from marvin.sshClient import SshClient
from marvin.lib.utils import cleanup_resources, get_process_status
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             NATRule,
                             PublicIPAddress,
                             StaticNATRule,
                             FireWallRule,
                             Network,
                             NetworkOffering,
                             LoadBalancerRule,
                             PublicIpRange,
                             Router,
                             VpcOffering,
                             VPC,
                             NetworkACLList,
                             NetworkACL)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_hosts,
                               list_routers)
from nose.plugins.attrib import attr
from ddt import ddt, data
# Import System modules
import socket
import time
import logging

_multiprocess_shared_ = True

logger = logging.getLogger('TestNetworkOps')
stream_handler = logging.StreamHandler()
logger.setLevel(logging.DEBUG)
logger.addHandler(stream_handler)

class TestPortForwarding(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        testClient = super(TestPortForwarding, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["zoneid"] = cls.zone.id
        template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"]
        )
        if template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services[
                "ostype"]

        # Create an account, network, VM and IP addresses
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls.services["publiciprange"]["zoneid"] = cls.zone.id
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["virtual_machine"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls._cleanup = [
            cls.virtual_machine,
            cls.account,
            cls.service_offering
        ]

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.apiclient = super(
                TestPortForwarding,
                cls).getClsTestClient().getApiClient()
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_port_forwarding_on_ip_from_non_src_nat_ip_range(self):
        """Test for port forwarding on a IP which is in pubic IP range different
           from public IP range that has source NAT IP associated with network
        """

        # Validate the following:
        # 1. Create a new public IP range and dedicate to a account
        # 2. Acquire a IP from new public range
        # 3. create a port forwarding on acquired IP from new range
        # 4. Create a firewall rule to open up the port
        # 5. Test SSH works to the VM

        self.public_ip_range = PublicIpRange.create(
                                    self.apiclient,
                                    self.services["publiciprange"]
                               )

        logger.debug("Dedicating Public IP range to the account");
        dedicate_public_ip_range_response = PublicIpRange.dedicate(
                                                self.apiclient,
                                                self.public_ip_range.vlan.id,
                                                account=self.account.name,
                                                domainid=self.account.domainid
                                            )
        ip_address = PublicIPAddress.create(
            self.apiclient,
            self.account.name,
            self.zone.id,
            self.account.domainid,
            self.services["virtual_machine"]
        )
        self.cleanup.append(ip_address)
        self.cleanup.append(self.public_ip_range)
        # Check if VM is in Running state before creating NAT and firewall rules
        vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id
        )

        self.assertEqual(
            isinstance(vm_response, list),
            True,
            "Check list VM returns a valid list"
        )

        self.assertNotEqual(
            len(vm_response),
            0,
            "Check Port Forwarding Rule is created"
        )
        self.assertEqual(
            vm_response[0].state,
            'Running',
            "VM state should be Running before creating a NAT rule."
        )

        # Open up firewall port for SSH
        FireWallRule.create(
            self.apiclient,
            ipaddressid=ip_address.ipaddress.id,
            protocol=self.services["natrule"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["natrule"]["publicport"],
            endport=self.services["natrule"]["publicport"]
        )

        # Create PF rule
        nat_rule = NATRule.create(
            self.apiclient,
            self.virtual_machine,
            self.services["natrule"],
            ip_address.ipaddress.id
        )

        try:
            logger.debug("SSHing into VM with IP address %s with NAT IP %s" %
                       (
                           self.virtual_machine.ipaddress,
                           ip_address.ipaddress.ipaddress
                       ))
            self.virtual_machine.get_ssh_client(ip_address.ipaddress.ipaddress)
        except Exception as e:
            self.fail(
                "SSH Access failed for %s: %s" %
                (self.virtual_machine.ipaddress, e)
            )

        nat_rule.delete(self.apiclient)

class TestStaticNat(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        testClient = super(TestStaticNat, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["zoneid"] = cls.zone.id
        template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"]
        )
        if template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services[
                "ostype"]

        # Create an account, network, VM and IP addresses
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls.services["publiciprange"]["zoneid"] = cls.zone.id
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["virtual_machine"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls.defaultNetworkId = cls.virtual_machine.nic[0].networkid
        cls._cleanup = [
            cls.virtual_machine,
            cls.account,
            cls.service_offering
        ]

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.apiclient = super(
                TestStaticNat,
                cls).getClsTestClient().getApiClient()
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_static_nat_on_ip_from_non_src_nat_ip_range(self):
        """Test for static nat on a IP which is in pubic IP range different
           from public IP range that has source NAT IP associated with network
        """

        # Validate the following:
        # 1. Create a new public IP range and dedicate to a account
        # 2. Acquire a IP from new public range
        # 3. Enable static NAT on acquired IP from new range
        # 4. Create a firewall rule to open up the port
        # 5. Test SSH works to the VM

        self.public_ip_range = PublicIpRange.create(
                                    self.apiclient,
                                    self.services["publiciprange"]
                               )
        logger.debug("Dedicating Public IP range to the account");
        dedicate_public_ip_range_response = PublicIpRange.dedicate(
                                                self.apiclient,
                                                self.public_ip_range.vlan.id,
                                                account=self.account.name,
                                                domainid=self.account.domainid
                                            )
        ip_address = PublicIPAddress.create(
            self.apiclient,
            self.account.name,
            self.zone.id,
            self.account.domainid,
            self.services["virtual_machine"]
        )
        self.cleanup.append(ip_address)
        self.cleanup.append(self.public_ip_range)
        # Check if VM is in Running state before creating NAT and firewall rules
        vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id
        )

        self.assertEqual(
            isinstance(vm_response, list),
            True,
            "Check list VM returns a valid list"
        )

        self.assertNotEqual(
            len(vm_response),
            0,
            "Check Port Forwarding Rule is created"
        )
        self.assertEqual(
            vm_response[0].state,
            'Running',
            "VM state should be Running before creating a NAT rule."
        )

        # Open up firewall port for SSH
        FireWallRule.create(
            self.apiclient,
            ipaddressid=ip_address.ipaddress.id,
            protocol=self.services["natrule"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["natrule"]["publicport"],
            endport=self.services["natrule"]["publicport"]
        )

        # Create Static NAT rule
        StaticNATRule.enable(
            self.apiclient,
            ip_address.ipaddress.id,
            self.virtual_machine.id,
            self.defaultNetworkId
        )

        try:
            logger.debug("SSHing into VM with IP address %s with NAT IP %s" %
                       (
                           self.virtual_machine.ipaddress,
                           ip_address.ipaddress.ipaddress
                       ))
            self.virtual_machine.get_ssh_client(ip_address.ipaddress.ipaddress)
        except Exception as e:
            self.fail(
                "SSH Access failed for %s: %s" %
                (self.virtual_machine.ipaddress, e)
            )

        StaticNATRule.disable(
            self.apiclient,
            ip_address.ipaddress.id,
            self.virtual_machine.id
        )

class TestRouting(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        testClient = super(TestRouting, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["zoneid"] = cls.zone.id
        template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"]
        )
        if template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services[
                "ostype"]

        # Create an account, network, VM and IP addresses
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls.services["publiciprange"]["zoneid"] = cls.zone.id
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__
        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["virtual_machine"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls._cleanup = [
            cls.virtual_machine,
            cls.account,
            cls.service_offering
        ]

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.apiclient = super(
                TestRouting,
                cls).getClsTestClient().getApiClient()
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_routing_tables(self):
        """Test routing table in case we have IP associated with a network which is in
            different pubic IP range from that of public IP range that has source NAT IP.
            When IP is associated we should see a new route table created.
            When IP is associated we should see a that route table is deleted.
        """

        # Validate the following:
        # 1. Create a new public IP range and dedicate to a account
        # 2. Acquire a IP from new public range
        # 3. Create a firewall rule to open up the port, so that IP is associated with network
        # 5. Login to VR and verify routing tables, there should be Table_eth3
        # 6. Delete firewall rule, since its last IP, routing table Table_eth3 should be deleted

        self.public_ip_range = PublicIpRange.create(
                                    self.apiclient,
                                    self.services["publiciprange"]
                               )
        self._cleanup.append(self.public_ip_range)

        logger.debug("Dedicating Public IP range to the account");
        dedicate_public_ip_range_response = PublicIpRange.dedicate(
                                                self.apiclient,
                                                self.public_ip_range.vlan.id,
                                                account=self.account.name,
                                                domainid=self.account.domainid
                                            )
        ip_address = PublicIPAddress.create(
            self.apiclient,
            self.account.name,
            self.zone.id,
            self.account.domainid,
            self.services["virtual_machine"]
        )
        self.cleanup.append(ip_address)
        self.cleanup.append(self.public_ip_range)
        # Check if VM is in Running state before creating NAT and firewall rules
        vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id
        )

        self.assertEqual(
            isinstance(vm_response, list),
            True,
            "Check list VM returns a valid list"
        )

        self.assertNotEqual(
            len(vm_response),
            0,
            "Check Port Forwarding Rule is created"
        )
        self.assertEqual(
            vm_response[0].state,
            'Running',
            "VM state should be Running before creating Firewall rule."
        )

        # Open up firewall port for SSH, this will associate IP with VR
        firewall_rule = FireWallRule.create(
            self.apiclient,
            ipaddressid=ip_address.ipaddress.id,
            protocol=self.services["natrule"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["natrule"]["publicport"],
            endport=self.services["natrule"]["publicport"]
        )

        # Get the router details associated with account
        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
        )
        router = routers[0]

        if (self.hypervisor.lower() == 'vmware'
                or self.hypervisor.lower() == 'hyperv'):
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                router.linklocalip,
                'ip route list table Table_eth3',
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
            host.user = self.hostConfig['username']
            host.passwd = self.hostConfig['password']
            try:
                result = get_process_status(
                    host.ipaddress,
                    22,
                    host.user,
                    host.passwd,
                    router.linklocalip,
                    'ip route list table Table_eth3'
                )
            except KeyError:
                self.skipTest(
                    "Provide a marvin config file with host\
                            credentials to run %s" %
                    self._testMethodName)

        logger.debug("ip route list table Table_eth3: %s" % result)
        public_range_gateway = self.services["publiciprange"]["gateway"]
        default_route_rule = "default via " + public_range_gateway + " dev eth3  proto static"
        logger.debug("default route result: %s" % str(result[0]))
        self.assertEqual(
            default_route_rule,
            str(result[0]),
            "Check default route table entry for public ip range"
        )

        res = str(result)
        self.assertEqual(
            res.count("throw") == 2,
            True,
            "Check routing rules to throw rest of the traffic. Count shoule be Atleast 2 for the control and guest traffic "
        )

        firewall_rule.delete(self.apiclient)

        if (self.hypervisor.lower() == 'vmware'
                or self.hypervisor.lower() == 'hyperv'):
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                router.linklocalip,
                'ip route list table Table_eth3',
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
            host.user = self.hostConfig['username']
            host.passwd = self.hostConfig['password']
            try:
                result = get_process_status(
                    host.ipaddress,
                    22,
                    host.user,
                    host.passwd,
                    router.linklocalip,
                    'ip route list table Table_eth3'
                )
            except KeyError:
                self.skipTest(
                    "Provide a marvin config file with host\
                            credentials to run %s" %
                    self._testMethodName)

        logger.debug("ip route list table Table_eth3: %s" % result)
        res = str(result)
        self.assertEqual(
            res.count("default via"),
            0,
            "Check to ensure there should not be any default rule"
        )

        self.assertEqual(
            res.count("throw"),
            0,
            "Check to ensure there should not be any throw rule"
        )

class TestIptables(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        testClient = super(TestIptables, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["zoneid"] = cls.zone.id
        template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"]
        )
        if template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services[
                "ostype"]

        # Create an account, network, VM and IP addresses
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls.services["publiciprange"]["zoneid"] = cls.zone.id
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__
        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["virtual_machine"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls._cleanup = [
            cls.virtual_machine,
            cls.account,
            cls.service_offering
        ]

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.apiclient = super(
                TestIptables,
                cls).getClsTestClient().getApiClient()
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_iptable_rules(self):
        """Test iptable rules in case we have IP associated with a network which is in
            different pubic IP range from that of public IP range that has source NAT IP.
            When IP is associated we should see a rule '-i eth3 -o eth0 -m state --state RELATED,ESTABLISHED -j ACCEPT' in FORWARD table.
            When IP is dis-associated we should see a rule in the FORWARD table is deleted.
        """

        # Validate the following:
        # 1. Create a new public IP range and dedicate to a account
        # 2. Acquire a IP from new public range
        # 3. Create a firewall rule to open up the port, so that IP is associated with network
        # 5. Login to VR and verify routing tables, there should be Table_eth3
        # 6. Delete firewall rule, since its last IP, routing table Table_eth3 should be deleted

        self.public_ip_range = PublicIpRange.create(
                                    self.apiclient,
                                    self.services["publiciprange"]
                               )
        self._cleanup.append(self.public_ip_range)

        logger.debug("Dedicating Public IP range to the account");
        dedicate_public_ip_range_response = PublicIpRange.dedicate(
                                                self.apiclient,
                                                self.public_ip_range.vlan.id,
                                                account=self.account.name,
                                                domainid=self.account.domainid
                                            )
        ip_address = PublicIPAddress.create(
            self.apiclient,
            self.account.name,
            self.zone.id,
            self.account.domainid,
            self.services["virtual_machine"]
        )
        self.cleanup.append(ip_address)
        # Check if VM is in Running state before creating NAT and firewall rules
        vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id
        )

        self.assertEqual(
            isinstance(vm_response, list),
            True,
            "Check list VM returns a valid list"
        )

        self.assertNotEqual(
            len(vm_response),
            0,
            "Check Port Forwarding Rule is created"
        )
        self.assertEqual(
            vm_response[0].state,
            'Running',
            "VM state should be Running before creating a NAT rule."
        )

        # Open up firewall port for SSH
        firewall_rule = FireWallRule.create(
            self.apiclient,
            ipaddressid=ip_address.ipaddress.id,
            protocol=self.services["natrule"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["natrule"]["publicport"],
            endport=self.services["natrule"]["publicport"]
        )
        # Get the router details associated with account
        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
        )
        router = routers[0]

        if (self.hypervisor.lower() == 'vmware'
                or self.hypervisor.lower() == 'hyperv'):
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                router.linklocalip,
                'iptables -t filter -L FORWARD  -v',
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
            host.user = self.hostConfig['username']
            host.passwd = self.hostConfig['password']
            try:
                result = get_process_status(
                    host.ipaddress,
                    22,
                    host.user,
                    host.passwd,
                    router.linklocalip,
                    'iptables -t filter -L FORWARD  -v'
                )
            except KeyError:
                self.skipTest(
                    "Provide a marvin config file with host\
                            credentials to run %s" %
                    self._testMethodName)

        logger.debug("iptables -t filter -L FORWARD  -v: %s" % result)
        res = str(result)
        self.assertEqual(
            res.count("eth3   eth0    anywhere             anywhere             state RELATED,ESTABLISHED"),
            1,
            "Check to ensure there is a iptable rule to accept the RELATED,ESTABLISHED traffic"
        )
        firewall_rule.delete(self.apiclient)

class TestVPCPortForwarding(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        socket.setdefaulttimeout(60)

        testClient = super(TestVPCPortForwarding, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
                                    cls.api_client,
                                    cls.zone.id,
                                    cls.services["ostype"]
                                    )
        cls.services["vpc_offering"] = { "name": 'VPC off',
                                     "displaytext": 'VPC off',
                                     "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Lb,UserData,StaticNat',
                                    }
        cls.services["network_offering"] = {
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
            }
        cls.services["network"] = {
                "name": "Test Network",
                "displaytext": "Test Network",
                "netmask": '255.255.255.0'
            }
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.services["publiciprange"]["zoneid"] = cls.zone.id

        cls.service_offering = ServiceOffering.create(
                                                        cls.api_client,
                                                        cls.services["service_offering"]
                                                        )
        cls._cleanup = [cls.service_offering]
        return


    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
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
        self.cleanup = [self.account]
        logger.debug("Creating a VPC offering..")
        self.vpc_off = VpcOffering.create(
                                                self.apiclient,
                                                self.services["vpc_offering"]
                                                )
        self._cleanup.append(self.vpc_off)
        logger.debug("Enabling the VPC offering created")
        self.vpc_off.update(self.apiclient, state='Enabled')

        logger.debug("Creating a VPC network in the account: %s" % self.account.name)
        self.services["vpc"]["cidr"] = '10.1.0.0/16'
        self.vpc = VPC.create(
                                self.apiclient,
                                self.services["vpc"],
                                vpcofferingid=self.vpc_off.id,
                                zoneid=self.zone.id,
                                account=self.account.name,
                                domainid=self.account.domainid
                                )
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            logger.debug("Warning: Exception during cleanup : %s" % e)
        return

    def check_ssh_into_vm(self, vm, public_ip, testnegative=False):
        logger.debug("Checking if we can SSH into VM=%s on public_ip=%s" % (vm.name, public_ip.ipaddress.ipaddress))
        try:
                vm.get_ssh_client(ipaddress=public_ip.ipaddress.ipaddress)
                if not testnegative:
                    logger.debug("SSH into VM=%s on public_ip=%s is successfully" % (vm.name, public_ip.ipaddress.ipaddress))
                else:
                    self.fail("SSH into VM=%s on public_ip=%s is successfully" % (vm.name, public_ip.ipaddress.ipaddress))
        except:
                if not testnegative:
                    self.fail("Failed to SSH into VM - %s" % (public_ip.ipaddress.ipaddress))
                else:
                    logger.debug("Failed to SSH into VM - %s" % (public_ip.ipaddress.ipaddress))

    def create_natrule(self, vm, public_ip, network, services=None):
        logger.debug("Creating NAT rule in network for vm with public IP")
        if not services:
            services = self.services["natrule"]
        nat_rule = NATRule.create(self.apiclient,
                                            vm,
                                            services,
                                            ipaddressid=public_ip.ipaddress.id,
                                            openfirewall=False,
                                            networkid=network.id,
                                            vpcid=self.vpc.id
                                            )
        return nat_rule

    def acquire_publicip(self, network):
        logger.debug("Associating public IP for network: %s" % network.name)
        public_ip = PublicIPAddress.create(self.apiclient,
                                        accountid=self.account.name,
                                        zoneid=self.zone.id,
                                        domainid=self.account.domainid,
                                        networkid=network.id,
                                        vpcid=self.vpc.id
                                        )
        logger.debug("Associated %s with network %s" % (public_ip.ipaddress.ipaddress,
                                                    network.id
                                                    ))
        return public_ip

    def create_network(self, net_offerring, gateway='10.1.1.1',vpc=None):
        try:
                logger.debug('Create NetworkOffering')
                net_offerring["name"] = "NET_OFF-" + str(gateway)
                nw_off = NetworkOffering.create(self.apiclient,
                                                        net_offerring,
                                                        conservemode=False
                                                        )
                # Enable Network offering
                nw_off.update(self.apiclient, state='Enabled')
                self._cleanup.append(nw_off)
                logger.debug('Created and Enabled NetworkOffering')

                self.services["network"]["name"] = "NETWORK-" + str(gateway)
                logger.debug('Adding Network=%s' % self.services["network"])
                default_acl = NetworkACLList.list(self.apiclient, name="default_allow")[0]
                obj_network = Network.create(self.apiclient,
                                                self.services["network"],
                                                accountid=self.account.name,
                                                domainid=self.account.domainid,
                                                networkofferingid=nw_off.id,
                                                zoneid=self.zone.id,
                                                gateway=gateway,
                                                aclid=default_acl.id,
                                                vpcid=vpc.id if vpc else self.vpc.id
                                                )
                logger.debug("Created network with ID: %s" % obj_network.id)
                return obj_network
        except Exception as e:
                self.fail('Unable to create a Network with offering=%s because of %s ' % (net_offerring, e))

    def deployvm_in_network(self, network, host_id=None):
        try:
                logger.debug('Creating VM in network=%s' % network.name)
                vm = VirtualMachine.create(
                                                self.apiclient,
                                                self.services["virtual_machine"],
                                                accountid=self.account.name,
                                                domainid=self.account.domainid,
                                                serviceofferingid=self.service_offering.id,
                                                networkids=[str(network.id)],
                                                hostid=host_id
                                                )
                logger.debug('Created VM=%s in network=%s' % (vm.id, network.name))

                return vm
        except:
                self.fail('Unable to create VM in a Network=%s' % network.name)

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_network_services_VPC_CreatePF(self):
        """ Test Create VPC PF rules on acquired public ip when VpcVirtualRouter is Running
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 4. Deploy vm1 in network1.
        # 5. Use the Create PF rule for vm in network1.
        # 6. Successfully ssh into the Guest VM using the PF rule

        network_1 = self.create_network(self.services["network_offering"])
        vm_1 = self.deployvm_in_network(network_1)
        self.public_ip_range = PublicIpRange.create(
                                    self.apiclient,
                                    self.services["publiciprange"]
                               )
        self._cleanup.append(self.public_ip_range)
        logger.debug("Dedicating Public IP range to the account");
        dedicate_public_ip_range_response = PublicIpRange.dedicate(
                                                self.apiclient,
                                                self.public_ip_range.vlan.id,
                                                account=self.account.name,
                                                domainid=self.account.domainid
                                            )
        public_ip_1 = self.acquire_publicip(network_1)
        self.create_natrule( vm_1, public_ip_1, network_1)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        self.public_ip_range.release(self.apiclient)
        return

class TestVPCStaticNat(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        socket.setdefaulttimeout(60)

        testClient = super(TestVPCStaticNat, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
                                    cls.api_client,
                                    cls.zone.id,
                                    cls.services["ostype"]
                                    )
        cls.services["vpc_offering"] = { "name": 'VPC off',
                                     "displaytext": 'VPC off',
                                     "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Lb,UserData,StaticNat',
                                    }
        cls.services["network_offering"] = {
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
            }
        cls.services["network"] = {
                "name": "Test Network",
                "displaytext": "Test Network",
                "netmask": '255.255.255.0'
            }
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.services["publiciprange"]["zoneid"] = cls.zone.id

        cls.service_offering = ServiceOffering.create(
                                                        cls.api_client,
                                                        cls.services["service_offering"]
                                                        )
        cls._cleanup = [cls.service_offering]
        return


    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
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
        self.cleanup = [self.account]
        logger.debug("Creating a VPC offering..")
        self.vpc_off = VpcOffering.create(
                                                self.apiclient,
                                                self.services["vpc_offering"]
                                                )
        self._cleanup.append(self.vpc_off)
        logger.debug("Enabling the VPC offering created")
        self.vpc_off.update(self.apiclient, state='Enabled')

        logger.debug("Creating a VPC network in the account: %s" % self.account.name)
        self.services["vpc"]["cidr"] = '10.1.0.0/16'
        self.vpc = VPC.create(
                                self.apiclient,
                                self.services["vpc"],
                                vpcofferingid=self.vpc_off.id,
                                zoneid=self.zone.id,
                                account=self.account.name,
                                domainid=self.account.domainid
                                )
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            logger.debug("Warning: Exception during cleanup : %s" % e)
        return

    def check_ssh_into_vm(self, vm, public_ip, testnegative=False):
        logger.debug("Checking if we can SSH into VM=%s on public_ip=%s" % (vm.name, public_ip.ipaddress.ipaddress))
        try:
                vm.get_ssh_client(ipaddress=public_ip.ipaddress.ipaddress)
                if not testnegative:
                    logger.debug("SSH into VM=%s on public_ip=%s is successfully" % (vm.name, public_ip.ipaddress.ipaddress))
                else:
                    self.fail("SSH into VM=%s on public_ip=%s is successfully" % (vm.name, public_ip.ipaddress.ipaddress))
        except:
                if not testnegative:
                    self.fail("Failed to SSH into VM - %s" % (public_ip.ipaddress.ipaddress))
                else:
                    logger.debug("Failed to SSH into VM - %s" % (public_ip.ipaddress.ipaddress))


    def acquire_publicip(self, network):
        logger.debug("Associating public IP for network: %s" % network.name)
        public_ip = PublicIPAddress.create(self.apiclient,
                                        accountid=self.account.name,
                                        zoneid=self.zone.id,
                                        domainid=self.account.domainid,
                                        networkid=network.id,
                                        vpcid=self.vpc.id
                                        )
        logger.debug("Associated %s with network %s" % (public_ip.ipaddress.ipaddress,
                                                    network.id
                                                    ))
        return public_ip

    def create_network(self, net_offerring, gateway='10.1.1.1',vpc=None):
        try:
                logger.debug('Create NetworkOffering')
                net_offerring["name"] = "NET_OFF-" + str(gateway)
                nw_off = NetworkOffering.create(self.apiclient,
                                                        net_offerring,
                                                        conservemode=False
                                                        )
                # Enable Network offering
                nw_off.update(self.apiclient, state='Enabled')
                self._cleanup.append(nw_off)
                logger.debug('Created and Enabled NetworkOffering')

                self.services["network"]["name"] = "NETWORK-" + str(gateway)
                logger.debug('Adding Network=%s' % self.services["network"])
                default_acl = NetworkACLList.list(self.apiclient, name="default_allow")[0]
                obj_network = Network.create(self.apiclient,
                                                self.services["network"],
                                                accountid=self.account.name,
                                                domainid=self.account.domainid,
                                                networkofferingid=nw_off.id,
                                                zoneid=self.zone.id,
                                                gateway=gateway,
                                                aclid=default_acl.id,
                                                vpcid=vpc.id if vpc else self.vpc.id
                                                )
                logger.debug("Created network with ID: %s" % obj_network.id)
                return obj_network
        except Exception as e:
                self.fail('Unable to create a Network with offering=%s because of %s ' % (net_offerring, e))

    def deployvm_in_network(self, network, host_id=None):
        try:
                logger.debug('Creating VM in network=%s' % network.name)
                vm = VirtualMachine.create(
                                                self.apiclient,
                                                self.services["virtual_machine"],
                                                accountid=self.account.name,
                                                domainid=self.account.domainid,
                                                serviceofferingid=self.service_offering.id,
                                                networkids=[str(network.id)],
                                                hostid=host_id
                                                )
                logger.debug('Created VM=%s in network=%s' % (vm.id, network.name))

                return vm
        except:
                self.fail('Unable to create VM in a Network=%s' % network.name)

    def create_StaticNatRule_For_VM(self, vm, public_ip, network, services=None):
        logger.debug("Enabling static NAT for IP: %s" %public_ip.ipaddress.ipaddress)
        if not services:
            services = self.services["natrule"]
        try:
                StaticNATRule.enable(
                                        self.apiclient,
                                        ipaddressid=public_ip.ipaddress.id,
                                        virtualmachineid=vm.id,
                                        networkid=network.id
                                        )
                logger.debug("Static NAT enabled for IP: %s" %
                                                        public_ip.ipaddress.ipaddress)
                logger.debug("Adding NetworkACL rules to make NAT rule accessible")
        except Exception as e:
                self.fail("Failed to enable static NAT on IP: %s - %s" % (
                                                    public_ip.ipaddress.ipaddress, e))

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_network_services_VPC_CreatePF(self):
        """ Test Create VPC PF rules on acquired public ip when VpcVirtualRouter is Running
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 4. Deploy vm1 in network1.
        # 5. Use the Create PF rule for vm in network1.
        # 6. Successfully ssh into the Guest VM using the PF rule

        network_1 = self.create_network(self.services["network_offering"])
        vm_1 = self.deployvm_in_network(network_1)
        self.public_ip_range = PublicIpRange.create(
                                    self.apiclient,
                                    self.services["publiciprange"]
                               )
        self._cleanup.append(self.public_ip_range)
        logger.debug("Dedicating Public IP range to the account");
        dedicate_public_ip_range_response = PublicIpRange.dedicate(
                                                self.apiclient,
                                                self.public_ip_range.vlan.id,
                                                account=self.account.name,
                                                domainid=self.account.domainid
                                            )
        public_ip_1 = self.acquire_publicip(network_1)
        self.create_StaticNatRule_For_VM( vm_1, public_ip_1, network_1)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        self.public_ip_range.release(self.apiclient)
        return
