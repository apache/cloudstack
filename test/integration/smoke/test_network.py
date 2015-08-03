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
""" BVT tests for Network Life Cycle
"""
# Import Local Modules
from marvin.codes import (FAILED, STATIC_NAT_RULE, LB_RULE,
                          NAT_RULE, PASS)
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
                             Router)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_hosts,
                               list_publicIP,
                               list_nat_rules,
                               list_routers,
                               list_virtual_machines,
                               list_lb_rules,
                               list_configurations,
                               verifyGuestTrafficPortGroups)
from nose.plugins.attrib import attr
from ddt import ddt, data
# Import System modules
import time

_multiprocess_shared_ = True


class TestPublicIP(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()

    @classmethod
    def setUpClass(cls):
        testClient = super(TestPublicIP, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        # Create Accounts & networks
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )

        cls.user = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls.services["network"]["zoneid"] = cls.zone.id

        cls.network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.services["network_offering"],
        )
        # Enable Network offering
        cls.network_offering.update(cls.apiclient, state='Enabled')

        cls.services["network"]["networkoffering"] = cls.network_offering.id
        cls.account_network = Network.create(
            cls.apiclient,
            cls.services["network"],
            cls.account.name,
            cls.account.domainid
        )
        cls.user_network = Network.create(
            cls.apiclient,
            cls.services["network"],
            cls.user.name,
            cls.user.domainid
        )

        # Create Source NAT IP addresses
        PublicIPAddress.create(
            cls.apiclient,
            cls.account.name,
            cls.zone.id,
            cls.account.domainid
        )
        PublicIPAddress.create(
            cls.apiclient,
            cls.user.name,
            cls.zone.id,
            cls.user.domainid
        )
        cls._cleanup = [
            cls.account_network,
            cls.user_network,
            cls.account,
            cls.user,
            cls.network_offering
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "smoke", "dvs"], required_hardware="false")
    def test_public_ip_admin_account(self):
        """Test for Associate/Disassociate public IP address for admin account"""

        # Validate the following:
        # 1. listPubliIpAddresses API returns the list of acquired addresses
        # 2. the returned list should contain our acquired IP address

        ip_address = PublicIPAddress.create(
            self.apiclient,
            self.account.name,
            self.zone.id,
            self.account.domainid
        )
        list_pub_ip_addr_resp = list_publicIP(
            self.apiclient,
            id=ip_address.ipaddress.id
        )
        self.assertEqual(
            isinstance(list_pub_ip_addr_resp, list),
            True,
            "Check list response returns a valid list"
        )
        # listPublicIpAddresses should return newly created public IP
        self.assertNotEqual(
            len(list_pub_ip_addr_resp),
            0,
            "Check if new IP Address is associated"
        )
        self.assertEqual(
            list_pub_ip_addr_resp[0].id,
            ip_address.ipaddress.id,
            "Check Correct IP Address is returned in the List Cacls"
        )

        ip_address.delete(self.apiclient)
        time.sleep(30)

        # Validate the following:
        # 1.listPublicIpAddresses should no more return the released address
        list_pub_ip_addr_resp = list_publicIP(
            self.apiclient,
            id=ip_address.ipaddress.id
        )
        if list_pub_ip_addr_resp is None:
            return
        if (list_pub_ip_addr_resp) and (
            isinstance(
                list_pub_ip_addr_resp,
                list)) and (
                len(list_pub_ip_addr_resp) > 0):
            self.fail("list public ip response is not empty")
        return

    @attr(tags=["advanced", "advancedns", "smoke", "dvs"], required_hardware="false")
    def test_public_ip_user_account(self):
        """Test for Associate/Disassociate public IP address for user account"""

        # Validate the following:
        # 1. listPubliIpAddresses API returns the list of acquired addresses
        # 2. the returned list should contain our acquired IP address

        ip_address = PublicIPAddress.create(
            self.apiclient,
            self.user.name,
            self.zone.id,
            self.user.domainid
        )

        # listPublicIpAddresses should return newly created public IP
        list_pub_ip_addr_resp = list_publicIP(
            self.apiclient,
            id=ip_address.ipaddress.id
        )
        self.assertEqual(
            isinstance(list_pub_ip_addr_resp, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(list_pub_ip_addr_resp),
            0,
            "Check if new IP Address is associated"
        )
        self.assertEqual(
            list_pub_ip_addr_resp[0].id,
            ip_address.ipaddress.id,
            "Check Correct IP Address is returned in the List Call"
        )

        ip_address.delete(self.apiclient)

        list_pub_ip_addr_resp = list_publicIP(
            self.apiclient,
            id=ip_address.ipaddress.id
        )

        self.assertEqual(
            list_pub_ip_addr_resp,
            None,
            "Check if disassociated IP Address is no longer available"
        )
        return


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
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
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

    @attr(tags=["advanced", "advancedns", "smoke", "dvs"], required_hardware="true")
    def test_01_port_fwd_on_src_nat(self):
        """Test for port forwarding on source NAT"""

        # Validate the following:
        # 1. listPortForwarding rules API should return the added PF rule
        # 2. attempt to do an ssh into the  user VM through the sourceNAT

        src_nat_ip_addrs = list_publicIP(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.assertEqual(
            isinstance(src_nat_ip_addrs, list),
            True,
            "Check list response returns a valid list"
        )
        src_nat_ip_addr = src_nat_ip_addrs[0]

        # Check if VM is in Running state before creating NAT rule
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
            ipaddressid=src_nat_ip_addr.id,
            protocol=self.services["natrule"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["natrule"]["publicport"],
            endport=self.services["natrule"]["publicport"]
        )

        # Create NAT rule
        nat_rule = NATRule.create(
            self.apiclient,
            self.virtual_machine,
            self.services["natrule"],
            src_nat_ip_addr.id
        )

        list_nat_rule_response = list_nat_rules(
            self.apiclient,
            id=nat_rule.id
        )
        self.assertEqual(
            isinstance(list_nat_rule_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_nat_rule_response),
            0,
            "Check Port Forwarding Rule is created"
        )
        self.assertEqual(
            list_nat_rule_response[0].id,
            nat_rule.id,
            "Check Correct Port forwarding Rule is returned"
        )
        # SSH virtual machine to test port forwarding
        try:
            self.debug("SSHing into VM with IP address %s with NAT IP %s" %
                       (
                           self.virtual_machine.ipaddress,
                           src_nat_ip_addr.ipaddress
                       ))

            self.virtual_machine.get_ssh_client(src_nat_ip_addr.ipaddress)
            vm_response = VirtualMachine.list(
                self.apiclient,
                id=self.virtual_machine.id
            )
            if vm_response[0].state != 'Running':
                self.fail(
                    "State of VM : %s is not found to be Running" % str(
                        self.virtual_machine.ipaddress))

        except Exception as e:
            self.fail(
                "SSH Access failed for %s: %s" %
                (self.virtual_machine.ipaddress, e)
            )

        try:
            nat_rule.delete(self.apiclient)
        except Exception as e:
            self.fail("NAT Rule Deletion Failed: %s" % e)

        # NAT rule listing should fail as the nat rule does not exist
        with self.assertRaises(Exception):
            list_nat_rules(self.apiclient,
                           id=nat_rule.id)

        # Check if the Public SSH port is inaccessible
        with self.assertRaises(Exception):
            self.debug(
                "SSHing into VM with IP address %s after NAT rule deletion" %
                self.virtual_machine.ipaddress)

            SshClient(
                src_nat_ip_addr.ipaddress,
                self.virtual_machine.ssh_port,
                self.virtual_machine.username,
                self.virtual_machine.password
            )
        return

    @attr(tags=["advanced", "advancedns", "smoke", "dvs"], required_hardware="true")
    def test_02_port_fwd_on_non_src_nat(self):
        """Test for port forwarding on non source NAT"""

        # Validate the following:
        # 1. listPortForwardingRules should not return the deleted rule anymore
        # 2. attempt to do ssh should now fail

        ip_address = PublicIPAddress.create(
            self.apiclient,
            self.account.name,
            self.zone.id,
            self.account.domainid,
            self.services["virtual_machine"]
        )
        self.cleanup.append(ip_address)

        # Check if VM is in Running state before creating NAT rule
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
        # Create NAT rule
        nat_rule = NATRule.create(
            self.apiclient,
            self.virtual_machine,
            self.services["natrule"],
            ip_address.ipaddress.id
        )
        # Validate the following:
        # 1. listPortForwardingRules should not return the deleted rule anymore
        # 2. attempt to do ssh should now fail

        list_nat_rule_response = list_nat_rules(
            self.apiclient,
            id=nat_rule.id
        )
        self.assertEqual(
            isinstance(list_nat_rule_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(list_nat_rule_response),
            0,
            "Check Port Forwarding Rule is created"
        )
        self.assertEqual(
            list_nat_rule_response[0].id,
            nat_rule.id,
            "Check Correct Port forwarding Rule is returned"
        )

        try:
            self.debug("SSHing into VM with IP address %s with NAT IP %s" %
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

        try:
            list_nat_rule_response = list_nat_rules(
                self.apiclient,
                id=nat_rule.id
            )
        except CloudstackAPIException:
            self.debug("Nat Rule is deleted")

        # Check if the Public SSH port is inaccessible
        with self.assertRaises(Exception):
            self.debug(
                "SSHing into VM with IP address %s after NAT rule deletion" %
                self.virtual_machine.ipaddress)

            SshClient(
                ip_address.ipaddress.ipaddress,
                self.virtual_machine.ssh_port,
                self.virtual_machine.username,
                self.virtual_machine.password,
                retries=2,
                delay=0
            )
        return

    @attr(tags=["dvs"], required_hardware="true")
    def test_guest_traffic_port_groups_isolated_network(self):
        """ Verify port groups are created for guest traffic
        used by isolated network """

        if self.hypervisor.lower() == "vmware":
            response = verifyGuestTrafficPortGroups(self.apiclient,
                                                    self.config,
                                                    self.zone)
            assert response[0] == PASS, response[1]


class TestRebootRouter(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.services = self.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        template = get_template(
            self.apiclient,
            self.zone.id,
            self.services["ostype"]
        )
        if template == FAILED:
            self.fail(
                "get_template() failed to return template with description %s" %
                self.services["ostype"])
        self.services["virtual_machine"]["zoneid"] = self.zone.id

        # Create an account, network, VM and IP addresses
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["tiny"]
        )
        self.vm_1 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id
        )

        # Wait for VM to come up
        time.sleep(120)

        src_nat_ip_addrs = list_publicIP(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        try:
            src_nat_ip_addr = src_nat_ip_addrs[0]
        except Exception as e:
            raise Exception(
                "Warning: Exception during fetching source NAT: %s" %
                e)

        self.public_ip = PublicIPAddress.create(
            self.apiclient,
            self.vm_1.account,
            self.vm_1.zoneid,
            self.vm_1.domainid,
            self.services["virtual_machine"]
        )
        # Open up firewall port for SSH
        FireWallRule.create(
            self.apiclient,
            ipaddressid=self.public_ip.ipaddress.id,
            protocol=self.services["lbrule"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["lbrule"]["publicport"],
            endport=self.services["lbrule"]["publicport"]
        )

        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.services["lbrule"],
            src_nat_ip_addr.id,
            self.account.name
        )
        lb_rule.assign(self.apiclient, [self.vm_1])
        self.nat_rule = NATRule.create(
            self.apiclient,
            self.vm_1,
            self.services["natrule"],
            ipaddressid=self.public_ip.ipaddress.id
        )
        self.cleanup = [self.nat_rule,
                        lb_rule,
                        self.vm_1,
                        self.service_offering,
                        self.account,
                        ]
        return

    @attr(tags=["advanced", "advancedns", "smoke", "dvs"], required_hardware="true")
    def test_reboot_router(self):
        """Test for reboot router"""

        # Validate the Following
        # 1. Post restart PF and LB rules should still function
        # 2. verify if the ssh into the virtual machine
        #   still works through the sourceNAT Ip

        # Retrieve router for the user account

        self.debug("Public IP: %s" % self.vm_1.ssh_ip)
        self.debug("Public IP: %s" % self.public_ip.ipaddress.ipaddress)
        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "Check list routers returns a valid list"
        )

        router = routers[0]

        self.debug("Rebooting the router (ID: %s)" % router.id)

        cmd = rebootRouter.rebootRouterCmd()
        cmd.id = router.id
        self.apiclient.rebootRouter(cmd)

        # Poll listVM to ensure VM is stopped properly
        timeout = self.services["timeout"]

        while True:
            time.sleep(self.services["sleep"])

            # Ensure that VM is in stopped state
            list_vm_response = list_virtual_machines(
                self.apiclient,
                id=self.vm_1.id
            )

            if isinstance(list_vm_response, list):

                vm = list_vm_response[0]
                if vm.state == 'Running':
                    self.debug("VM state: %s" % vm.state)
                    break

            if timeout == 0:
                raise Exception(
                    "Failed to start VM (ID: %s) in change service offering" %
                    vm.id)

            timeout = timeout - 1

        # we should be able to SSH after successful reboot
        try:
            self.debug("SSH into VM (ID : %s ) after reboot" % self.vm_1.id)

            SshClient(
                self.public_ip.ipaddress.ipaddress,
                self.services["natrule"]["publicport"],
                self.vm_1.username,
                self.vm_1.password
            )
        except Exception as e:
            self.fail(
                "SSH Access failed for %s: %s" %
                (self.public_ip.ipaddress.ipaddress, e))
        return

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return


class TestReleaseIP(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.services = self.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        template = get_template(
            self.apiclient,
            self.zone.id,
            self.services["ostype"]
        )
        self.services["virtual_machine"]["zoneid"] = self.zone.id

        # Create an account, network, VM, Port forwarding rule, LB rules
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
            domainid=self.domain.id
        )

        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["tiny"]
        )

        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id
        )

        self.ip_address = PublicIPAddress.create(
            self.apiclient,
            self.account.name,
            self.zone.id,
            self.account.domainid
        )

        ip_addrs = list_publicIP(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            issourcenat=False
        )
        try:
            self.ip_addr = ip_addrs[0]
        except Exception as e:
            raise Exception(
                "Failed: During acquiring source NAT for account: %s, :%s" %
                (self.account.name, e))

        self.nat_rule = NATRule.create(
            self.apiclient,
            self.virtual_machine,
            self.services["natrule"],
            self.ip_addr.id
        )
        self.lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.services["lbrule"],
            self.ip_addr.id,
            accountid=self.account.name
        )
        self.cleanup = [
            self.virtual_machine,
            self.account
        ]
        return

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)

    @attr(tags=["advanced", "advancedns", "smoke", "dvs"], required_hardware="false")
    def test_releaseIP(self):
        """Test for release public IP address"""

        self.debug("Deleting Public IP : %s" % self.ip_addr.id)

        self.ip_address.delete(self.apiclient)

        retriesCount = 10
        isIpAddressDisassociated = False
        while retriesCount > 0:
            listResponse = list_publicIP(
                self.apiclient,
                id=self.ip_addr.id
            )
            if listResponse is None:
                isIpAddressDisassociated = True
                break
            retriesCount -= 1
            time.sleep(60)
        # End while

        self.assertTrue(
            isIpAddressDisassociated,
            "Failed to disassociate IP address")

        # ListPortForwardingRules should not list
        # associated rules with Public IP address
        try:
            list_nat_rule = list_nat_rules(
                self.apiclient,
                id=self.nat_rule.id
            )
            self.debug("List NAT Rule response" + str(list_nat_rule))
        except CloudstackAPIException:
            self.debug("Port Forwarding Rule is deleted")

        # listLoadBalancerRules should not list
        # associated rules with Public IP address
        try:
            list_lb_rule = list_lb_rules(
                self.apiclient,
                id=self.lb_rule.id
            )
            self.debug("List LB Rule response" + str(list_lb_rule))
        except CloudstackAPIException:
            self.debug("Port Forwarding Rule is deleted")

        # SSH Attempt though public IP should fail
        with self.assertRaises(Exception):
            SshClient(
                self.ip_addr.ipaddress,
                self.services["natrule"]["publicport"],
                self.virtual_machine.username,
                self.virtual_machine.password,
                retries=2,
                delay=0
            )
        return


class TestDeleteAccount(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.services = self.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        template = get_template(
            self.apiclient,
            self.zone.id,
            self.services["ostype"]
        )
        self.services["virtual_machine"]["zoneid"] = self.zone.id

        # Create an account, network, VM and IP addresses
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["tiny"]
        )
        self.vm_1 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id
        )

        src_nat_ip_addrs = list_publicIP(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )

        try:
            src_nat_ip_addr = src_nat_ip_addrs[0]
        except Exception as e:
            self.fail("SSH failed for VM with IP: %s %s" %
                      (src_nat_ip_addr.ipaddress, e))

        self.lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.services["lbrule"],
            src_nat_ip_addr.id,
            self.account.name
        )
        self.lb_rule.assign(self.apiclient, [self.vm_1])

        self.nat_rule = NATRule.create(
            self.apiclient,
            self.vm_1,
            self.services["natrule"],
            src_nat_ip_addr.id
        )
        self.cleanup = []
        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="false")
    def test_delete_account(self):
        """Test for delete account"""

        # Validate the Following
        # 1. after account.cleanup.interval (global setting)
        #    time all the PF/LB rules should be deleted
        # 2. verify that list(LoadBalancer/PortForwarding)Rules
        #    API does not return any rules for the account
        # 3. The domR should have been expunged for this account

        self.account.delete(self.apiclient)
        interval = list_configurations(
            self.apiclient,
            name='account.cleanup.interval'
        )
        self.assertEqual(
            isinstance(interval, list),
            True,
            "Check if account.cleanup.interval config present"
        )
        # Sleep to ensure that all resources are deleted
        time.sleep(int(interval[0].value))

        # ListLoadBalancerRules should not list
        # associated rules with deleted account
        # Unable to find account testuser1 in domain 1 : Exception
        try:
            list_lb_rules(
                self.apiclient,
                account=self.account.name,
                domainid=self.account.domainid
            )
        except CloudstackAPIException:
            self.debug("Port Forwarding Rule is deleted")

        # ListPortForwardingRules should not
        # list associated rules with deleted account
        try:
            list_nat_rules(
                self.apiclient,
                account=self.account.name,
                domainid=self.account.domainid
            )
        except CloudstackAPIException:
            self.debug("NATRule is deleted")

        # Retrieve router for the user account
        try:
            routers = list_routers(
                self.apiclient,
                account=self.account.name,
                domainid=self.account.domainid
            )
            self.assertEqual(
                routers,
                None,
                "Check routers are properly deleted."
            )
        except CloudstackAPIException:
            self.debug("Router is deleted")

        except Exception as e:
            raise Exception(
                "Encountered %s raised while fetching routers for account: %s" %
                (e, self.account.name))
        return

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return


@ddt
class TestRouterRules(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        testClient = super(TestRouterRules, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.hypervisor = testClient.getHypervisorInfo()
        template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"]
        )
        if template == FAILED:
            assert False, "get_template() failed to return template\
                    with description %s" % cls.services["ostype"]

        # Create an account, network, VM and IP addresses
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
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
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return

    def getCommandResultFromRouter(self, router, command):
        """Run given command on router and return the result"""

        if (self.hypervisor.lower() == 'vmware'
                or self.hypervisor.lower() == 'hyperv'):
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                router.linklocalip,
                command,
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

            result = get_process_status(
                host.ipaddress,
                22,
                host.user,
                host.passwd,
                router.linklocalip,
                command
            )
        return result

    def createNetworkRules(self, rule, ipaddressobj, networkid):
        """ Create specified rule on acquired public IP and
        default network of virtual machine
        """
        # Open up firewall port for SSH
        self.fw_rule = FireWallRule.create(
            self.apiclient,
            ipaddressid=ipaddressobj.ipaddress.id,
            protocol=self.services["fwrule"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["fwrule"]["startport"],
            endport=self.services["fwrule"]["endport"]
        )

        if rule == STATIC_NAT_RULE:
            StaticNATRule.enable(
                self.apiclient,
                ipaddressobj.ipaddress.id,
                self.virtual_machine.id,
                networkid
            )

        elif rule == LB_RULE:
            self.lb_rule = LoadBalancerRule.create(
                self.apiclient,
                self.services["lbrule"],
                ipaddressid=ipaddressobj.ipaddress.id,
                accountid=self.account.name,
                networkid=self.virtual_machine.nic[0].networkid,
                domainid=self.account.domainid)

            vmidipmap = [{"vmid": str(self.virtual_machine.id),
                          "vmip": str(self.virtual_machine.nic[0].ipaddress)}]

            self.lb_rule.assign(
                self.apiclient,
                vmidipmap=vmidipmap
            )
        else:
            self.nat_rule = NATRule.create(
                self.apiclient,
                self.virtual_machine,
                self.services["natrule"],
                ipaddressobj.ipaddress.id
            )
        return

    def removeNetworkRules(self, rule, ipaddressobj):
        """ Remove specified rule on acquired public IP and
        default network of virtual machine
        """
        self.fw_rule.delete(self.apiclient)

        if rule == STATIC_NAT_RULE:
            StaticNATRule.disable(
                self.apiclient,
                ipaddressobj.ipaddress.id)

        elif rule == LB_RULE:
            self.lb_rule.delete(self.apiclient)
        else:
            self.nat_rule.delete(self.apiclient)
        return

    @data(STATIC_NAT_RULE, NAT_RULE, LB_RULE)
    @attr(tags=["advanced", "advancedns", "smoke", "dvs"], required_hardware="true")
    def test_network_rules_acquired_public_ip(self, value):
        """Test for Router rules for network rules on acquired public IP"""

        # Validate the following:
        # 1. listPortForwardingRules should not return the deleted rule anymore
        # 2. attempt to do ssh should now fail

        self.ipaddress = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=self.defaultNetworkId
        )
        self.cleanup.append(self.ipaddress)

        self.createNetworkRules(rule=value,
                                ipaddressobj=self.ipaddress,
                                networkid=self.defaultNetworkId)

        router = Router.list(self.apiclient,
                             networkid=self.virtual_machine.nic[0].networkid,
                             listall=True)[0]

        response = self.getCommandResultFromRouter(router, "ip addr")
        self.debug(response)
        stringToMatch = "inet %s" % self.ipaddress.ipaddress.ipaddress
        self.assertTrue(stringToMatch in str(response), "IP address is\
                not removed from VR even after disabling statin NAT")

        try:
            self.debug("SSHing into VM with IP address %s with NAT IP %s" %
                       (
                           self.virtual_machine.ipaddress,
                           self.ipaddress.ipaddress.ipaddress
                       ))
            self.virtual_machine.get_ssh_client(
                self.ipaddress.ipaddress.ipaddress)
        except Exception as e:
            self.fail(
                "SSH Access failed for %s: %s" %
                (self.virtual_machine.ipaddress, e)
            )

        # Validate the following:
        # 1. listIpForwardingRules should not return the deleted rule anymore
        # 2. attempt to do ssh should now fail

        self.removeNetworkRules(rule=value, ipaddressobj=self.ipaddress)

        response = self.getCommandResultFromRouter(router, "ip addr")
        self.debug(response)
        stringToMatch = "inet %s" % self.ipaddress.ipaddress.ipaddress
        self.assertFalse(stringToMatch in str(response), "IP address is\
                not removed from VR even after disabling statin NAT")

        # Check if the Public SSH port is inaccessible
        with self.assertRaises(Exception):
            self.debug(
                "SSHing into VM with IP address %s after NAT rule deletion" %
                self.virtual_machine.ipaddress)

            SshClient(
                self.ipaddress.ipaddress.ipaddress,
                self.virtual_machine.ssh_port,
                self.virtual_machine.username,
                self.virtual_machine.password,
                retries=2,
                delay=0
            )
        return
