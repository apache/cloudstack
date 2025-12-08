# Licensed to the Apache Software Foundation (ASF) under one
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
import json

# Import Local Modules
from marvin.codes import (FAILED, STATIC_NAT_RULE, LB_RULE,
                          NAT_RULE, PASS)
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackException import CloudstackAPIException
from marvin.cloudstackAPI import rebootRouter
from marvin.sshClient import SshClient
from marvin.lib.utils import cleanup_resources, get_process_status, get_host_credentials, random_gen
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
                             Router,
                             NIC,
                             Template,
                             Cluster,
                             SSHKeyPair)
from marvin.lib.common import (get_domain,
                               get_free_vlan,
                               get_zone,
                               get_template,
                               get_test_template,
                               list_hosts,
                               list_publicIP,
                               list_nat_rules,
                               list_routers,
                               list_virtual_machines,
                               list_lb_rules,
                               list_configurations,
                               verifyGuestTrafficPortGroups,
                               verifyNetworkState)

from nose.plugins.attrib import attr
from marvin.lib.decoratorGenerators import skipTestIf
from ddt import ddt, data
import unittest
# Import System modules
import os
import time
import logging
import random
import tempfile

_multiprocess_shared_ = True

logger = logging.getLogger('TestNetworkOps')
stream_handler = logging.StreamHandler()
logger.setLevel(logging.DEBUG)
logger.addHandler(stream_handler)


class TestPublicIP(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestPublicIP, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls._cleanup = []
        # Create Accounts & networks
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.user = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.user)
        cls.services["network"]["zoneid"] = cls.zone.id

        cls.network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.services["network_offering"],
        )
        cls._cleanup.append(cls.network_offering)
        # Enable Network offering
        cls.network_offering.update(cls.apiclient, state='Enabled')

        cls.services["network"]["networkoffering"] = cls.network_offering.id
        cls.account_network = Network.create(
            cls.apiclient,
            cls.services["network"],
            cls.account.name,
            cls.account.domainid
        )
        cls._cleanup.append(cls.account_network)
        cls.user_network = Network.create(
            cls.apiclient,
            cls.services["network"],
            cls.user.name,
            cls.user.domainid
        )
        cls._cleanup.append(cls.user_network)

        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"],
        )
        cls._cleanup.append(cls.service_offering)

        cls.hypervisor = testClient.getHypervisorInfo()
        cls.template = get_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor
        )
        if cls.template == FAILED:
            assert False, "get_test_template() failed to return template"

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.account_vm = VirtualMachine.create(
            cls.apiclient,
            cls.services["virtual_machine"],
            templateid=cls.template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            networkids=cls.account_network.id,
            serviceofferingid=cls.service_offering.id
        )
        cls._cleanup.append(cls.account_vm)

        cls.user_vm = VirtualMachine.create(
            cls.apiclient,
            cls.services["virtual_machine"],
            templateid=cls.template.id,
            accountid=cls.user.name,
            domainid=cls.user.domainid,
            networkids=cls.user_network.id,
            serviceofferingid=cls.service_offering.id
        )
        cls._cleanup.append(cls.user_vm)

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
        return

    @classmethod
    def tearDownClass(cls):
        super(TestPublicIP, cls).tearDownClass()

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
            id=ip_address.ipaddress.id,
            allocatedonly=True
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
            id=ip_address.ipaddress.id,
            allocatedonly=True
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
        template = get_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_test_template() failed to return template"

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
            logger.debug("SSHing into VM with IP address %s with NAT IP %s" %
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
            logger.debug(
                "SSHing into VM with IP address %s after NAT rule deletion" %
                self.virtual_machine.ipaddress)

            SshClient(
                src_nat_ip_addr.ipaddress,
                self.virtual_machine.ssh_port,
                self.virtual_machine.username,
                self.virtual_machine.password,
                retries=2,
                delay=0
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

        try:
            list_nat_rule_response = list_nat_rules(
                self.apiclient,
                id=nat_rule.id
            )
        except CloudstackAPIException:
            logger.debug("Nat Rule is deleted")

        # Check if the Public SSH port is inaccessible
        with self.assertRaises(Exception):
            logger.debug(
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
        self.hypervisor = self.testClient.getHypervisorInfo()
        template = get_test_template(
            self.apiclient,
            self.zone.id,
            self.hypervisor
        )
        if template == FAILED:
            self.fail("get_test_template() failed to return template")

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

        logger.debug("Public IP: %s" % self.vm_1.ssh_ip)
        logger.debug("Public IP: %s" % self.public_ip.ipaddress.ipaddress)
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

        logger.debug("Rebooting the router (ID: %s)" % router.id)

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
                    logger.debug("VM state: %s" % vm.state)
                    break

            if timeout == 0:
                raise Exception(
                    "Failed to start VM (ID: %s) in change service offering" %
                    vm.id)

            timeout = timeout - 1

        # we should be able to SSH after successful reboot
        try:
            logger.debug("SSH into VM (ID : %s ) after reboot" % self.vm_1.id)

            SshClient(
                self.public_ip.ipaddress.ipaddress,
                self.services["natrule"]["publicport"],
                self.vm_1.username,
                self.vm_1.password,
                retries=5
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
        self.hypervisor = self.testClient.getHypervisorInfo()
        template = get_test_template(
            self.apiclient,
            self.zone.id,
            self.hypervisor
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
        """Test for release public IP address using the ID"""

        logger.debug("Deleting Public IP : %s" % self.ip_addr.id)

        self.ip_address.delete(self.apiclient)

        retriesCount = 10
        isIpAddressDisassociated = False
        while retriesCount > 0:
            listResponse = list_publicIP(
                self.apiclient,
                id=self.ip_addr.id,
                state="Allocated"
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
            logger.debug("List NAT Rule response" + str(list_nat_rule))
        except CloudstackAPIException:
            logger.debug("Port Forwarding Rule is deleted")

        # listLoadBalancerRules should not list
        # associated rules with Public IP address
        try:
            list_lb_rule = list_lb_rules(
                self.apiclient,
                id=self.lb_rule.id
            )
            logger.debug("List LB Rule response" + str(list_lb_rule))
        except CloudstackAPIException:
            logger.debug("Port Forwarding Rule is deleted")

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

    @attr(tags=["advanced", "advancedns", "smoke", "dvs"], required_hardware="false")
    def test_releaseIP_using_IP(self):
        """Test for release public IP address using the address"""

        logger.debug("Deleting Public IP : %s" % self.ip_addr.ipaddress)
        self.ip_address.delete_by_ip(self.apiclient)

        retriesCount = 10
        isIpAddressDisassociated = False
        while retriesCount > 0:
            listResponse = list_publicIP(
                self.apiclient,
                id=self.ip_addr.id,
                state="Allocated"
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
            logger.debug("List NAT Rule response" + str(list_nat_rule))
        except CloudstackAPIException:
            logger.debug("Port Forwarding Rule is deleted")

        # listLoadBalancerRules should not list
        # associated rules with Public IP address
        try:
            list_lb_rule = list_lb_rules(
                self.apiclient,
                id=self.lb_rule.id
            )
            logger.debug("List LB Rule response" + str(list_lb_rule))
        except CloudstackAPIException:
            logger.debug("Port Forwarding Rule is deleted")

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
        self.hypervisor = self.testClient.getHypervisorInfo()
        template = get_test_template(
            self.apiclient,
            self.zone.id,
            self.hypervisor
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
            logger.debug("Port Forwarding Rule is deleted")

        # ListPortForwardingRules should not
        # list associated rules with deleted account
        try:
            list_nat_rules(
                self.apiclient,
                account=self.account.name,
                domainid=self.account.domainid
            )
        except CloudstackAPIException:
            logger.debug("NATRule is deleted")

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
            logger.debug("Router is deleted")

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
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][
            0].__dict__
        template = get_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_test_template() failed to return template"

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
            host.user = self.hostConfig['username']
            host.passwd = self.hostConfig['password']

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

    def removeNetworkRules(self, rule):
        """ Remove specified rule on acquired public IP and
        default network of virtual machine
        """
        self.fw_rule.delete(self.apiclient)

        if rule == STATIC_NAT_RULE:
            StaticNATRule.disable(
                self.apiclient,
                self.ipaddress.ipaddress.id)

        elif rule == LB_RULE:
            self.lb_rule.delete(self.apiclient)
        else:
            self.nat_rule.delete(self.apiclient)

        logger.debug("Releasing IP %s from account %s" % (self.ipaddress.ipaddress.ipaddress, self.account.name))
        self.ipaddress.delete(self.apiclient)

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

        self.createNetworkRules(rule=value,
                                ipaddressobj=self.ipaddress,
                                networkid=self.defaultNetworkId)

        router = Router.list(self.apiclient,
                             networkid=self.virtual_machine.nic[0].networkid,
                             listall=True)[0]

        response = self.getCommandResultFromRouter(router, "ip addr")
        logger.debug(response)
        stringToMatch = "inet %s" % self.ipaddress.ipaddress.ipaddress
        self.assertTrue(stringToMatch in str(response), "IP address is\
                not added to the VR!")

        try:
            logger.debug("SSHing into VM with IP address %s with NAT IP %s" %
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

        self.removeNetworkRules(rule=value)

        response = self.getCommandResultFromRouter(router, "ip addr")
        logger.debug(response)
        stringToMatch = "inet %s" % self.ipaddress.ipaddress.ipaddress
        self.assertFalse(stringToMatch in str(response), "IP address is\
                not removed from VR even after disabling stat in NAT")

        # Check if the Public SSH port is inaccessible
        with self.assertRaises(Exception):
            logger.debug(
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


class TestL2Networks(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.services["network"]["networkoffering"] = self.network_offering.id

        self.l2_network = Network.create(
            self.apiclient,
            self.services["l2-network"],
            zoneid=self.zone.id,
            networkofferingid=self.network_offering.id
        )
        self.cleanup = [
            self.l2_network]

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @classmethod
    def setUpClass(cls):
        testClient = super(TestL2Networks, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype
        # Create Accounts & networks
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls.template = get_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor
        )
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls.services["network"]["zoneid"] = cls.zone.id

        cls.network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.services["l2-network_offering"],
        )
        # Enable Network offering
        cls.network_offering.update(cls.apiclient, state='Enabled')

        cls._cleanup = [
            cls.account,
            cls.network_offering,
            cls.service_offering
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

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="false")
    def test_deploy_vm_l2network(self):
        """Creates an l2 network and verifies user is able to deploy a VM in it"""

        # Validate the following:
        # 1. Deploys a VM
        # 2. There are no network services available since this is L2 Network

        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.service_offering.id,
            networkids=self.l2_network.id,
            zoneid=self.zone.id
        )

        self.cleanup.insert(0, self.virtual_machine)

        list_vm = list_virtual_machines(
            self.apiclient,
            id=self.virtual_machine.id
        )
        self.assertEqual(
            isinstance(list_vm, list),
            True,
            "Check if virtual machine is present"
        )

        self.assertEqual(
            list_vm[0].nic[0].type,
            'L2',
            "Check Correct Network type is available"
        )

        self.assertFalse(
            'gateway' in str(list_vm[0].nic[0])
        )

        self.assertFalse(
            'ipaddress' in str(list_vm[0].nic[0])
        )

        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="false")
    def test_delete_network_while_vm_on_it(self):
        """It verifies the user is not able to delete network which has running vms"""

        # Validate the following:
        # 1. Deploys a VM
        # 2. Tries to delete network and expects exception to appear

        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.service_offering.id,
            networkids=self.l2_network.id,
            zoneid=self.zone.id
        )

        self.cleanup.insert(0, self.virtual_machine)

        list_vm = list_virtual_machines(
            self.apiclient,
            id=self.virtual_machine.id
        )
        self.assertEqual(
            isinstance(list_vm, list),
            True,
            "Check if virtual machine is present"
        )

        try:
            self.l2_network.delete(self.apiclient)
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")

        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="false")
    def test_l2network_restart(self):
        """This test covers a few scenarios around restarting a network"""

        # Validate the following:
        # 1. Creates a l2 network
        # 2. Tries to restart a network with no VMs, which trows error 'not in the right state'
        # 3. Deploys a VM
        # 4. Restarts the network without cleanup
        # 5. Restarts the network with cleanup

        try:
            self.l2_network.restart(self.apiclient, cleanup=True)
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")

        li_net = self.l2_network.list(self.apiclient)[0]

        self.assertTrue(
            li_net.state,
            'Allocated'
            "Not the correct state"
        )

        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.service_offering.id,
            networkids=self.l2_network.id,
            zoneid=self.zone.id
        )

        self.cleanup.insert(0, self.virtual_machine)

        list_vm = list_virtual_machines(
            self.apiclient,
            id=self.virtual_machine.id
        )
        self.assertEqual(
            isinstance(list_vm, list),
            True,
            "Check if virtual machine is present"
        )

        self.l2_network.restart(self.apiclient, cleanup=False)

        li_net = self.l2_network.list(self.apiclient)[0]

        self.assertTrue(
            li_net.state,
            'Implemented'
            "Not the correct state"
        )

        self.l2_network.restart(self.apiclient, cleanup=True)

        li_net = self.l2_network.list(self.apiclient)[0]

        self.assertTrue(
            li_net.state,
            'Implemented'
            "Not the correct state"
        )

        return


class TestPrivateVlansL2Networks(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()

        self.cleanup = [
        ]

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @classmethod
    def setUpClass(cls):
        testClient = super(TestPrivateVlansL2Networks, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype

        # Supported hypervisor = Vmware using dvSwitches for guest traffic
        isVmware = False
        isDvSwitch = False
        if cls.hypervisor.lower() in ["vmware"]:
            isVmware = True
            clusters = Cluster.list(cls.apiclient, zoneid=cls.zone.id, hypervisor=cls.hypervisor)
            for cluster in clusters:
                if cluster.resourcedetails.guestvswitchtype == "vmwaredvs":
                    # Test only if cluster uses dvSwitch
                    isDvSwitch = True
                    break

        # Supported hypervisor = KVM using OVS
        isKVM = cls.hypervisor.lower() in ["kvm"]
        isOVSEnabled = False
        hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][
            0].__dict__
        if isKVM:
            # Test only if all the hosts use OVS
            grepCmd = 'grep "network.bridge.type=openvswitch" /etc/cloudstack/agent/agent.properties'
            hosts = list_hosts(cls.apiclient, type='Routing', hypervisor='kvm')
            if len(hosts) > 0:
                isOVSEnabled = True
            for host in hosts:
                isOVSEnabled = isOVSEnabled and len(SshClient(host.ipaddress, port=22, user=hostConfig["username"],
                                                              passwd=hostConfig["password"]).execute(grepCmd)) != 0

        supported = isVmware and isDvSwitch or isKVM and isOVSEnabled
        cls.unsupportedHardware = not supported

        cls._cleanup = []

        if supported:
            cls.account = Account.create(
                cls.apiclient,
                cls.services["account"],
                admin=True,
                domainid=cls.domain.id
            )
            cls.template = get_test_template(
                cls.apiclient,
                cls.zone.id,
                cls.hypervisor
            )
            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.services["service_offerings"]["tiny"]
            )
            cls.services["network"]["zoneid"] = cls.zone.id
            cls.services['mode'] = cls.zone.networktype
            cls.services["small"]["zoneid"] = cls.zone.id
            cls.services["small"]["template"] = cls.template.id
            cls.services["l2-network-pvlan-community-1"] = {
                "name": "Test Network L2 PVLAN Community 1",
                "displaytext": "Test Network L2 PVLAN Community 1",
                "vlan": 900,
                "isolatedpvlan": "901",
                "isolatedpvlantype": "community"
            }
            cls.services["l2-network-pvlan-community-2"] = {
                "name": "Test Network L2 PVLAN Community 2",
                "displaytext": "Test Network L2 PVLAN Community 2",
                "vlan": 900,
                "isolatedpvlan": "902",
                "isolatedpvlantype": "community"
            }
            cls.services["l2-network-pvlan-promiscuous"] = {
                "name": "Test Network L2 PVLAN Promiscuous",
                "displaytext": "Test Network L2 PVLAN Promiscuous",
                "vlan": 900,
                "isolatedpvlan": "900",
                "isolatedpvlantype": "promiscuous"
            }
            cls.services["l2-network-pvlan-isolated"] = {
                "name": "Test Network L2 PVLAN Isolated",
                "displaytext": "Test Network L2 PVLAN Isolated",
                "vlan": 900,
                "isolatedpvlan": "903",
                "isolatedpvlantype": "isolated"
            }

            cls.l2_network_offering = NetworkOffering.create(
                cls.apiclient,
                cls.services["l2-network_offering"],
                specifyvlan=True
            )
            cls.isolated_network_offering = NetworkOffering.create(
                cls.apiclient,
                cls.services["network_offering"]
            )
            cls.l2_network_offering.update(cls.apiclient, state='Enabled')
            cls.isolated_network_offering.update(cls.apiclient, state='Enabled')

            cls.l2_pvlan_community1 = Network.create(
                cls.apiclient,
                cls.services["l2-network-pvlan-community-1"],
                zoneid=cls.zone.id,
                networkofferingid=cls.l2_network_offering.id
            )
            cls.l2_pvlan_community2 = Network.create(
                cls.apiclient,
                cls.services["l2-network-pvlan-community-2"],
                zoneid=cls.zone.id,
                networkofferingid=cls.l2_network_offering.id
            )
            cls.l2_pvlan_isolated = Network.create(
                cls.apiclient,
                cls.services["l2-network-pvlan-isolated"],
                zoneid=cls.zone.id,
                networkofferingid=cls.l2_network_offering.id
            )
            cls.l2_pvlan_promiscuous = Network.create(
                cls.apiclient,
                cls.services["l2-network-pvlan-promiscuous"],
                zoneid=cls.zone.id,
                networkofferingid=cls.l2_network_offering.id
            )
            cls.isolated_network = Network.create(
                cls.apiclient,
                cls.services["isolated_network"],
                zoneid=cls.zone.id,
                networkofferingid=cls.isolated_network_offering.id,
                accountid=cls.account.name,
                domainid=cls.account.domainid
            )

            cls._cleanup = [
                cls.l2_pvlan_promiscuous,
                cls.l2_pvlan_isolated,
                cls.l2_pvlan_community1,
                cls.l2_pvlan_community2,
                cls.isolated_network,
                cls.l2_network_offering,
                cls.isolated_network_offering,
                cls.service_offering,
                cls.account,
            ]

        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def deploy_vm_multiple_nics(self, name, l2net):
        """
        Deploy VM on L2 network and isolated network so VM can get an IP, to use with arping command for isolation test
        """
        self.services["small"]["name"] = name

        vm = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[self.isolated_network.id, l2net.id],
            mode=self.services["mode"]
        )

        return vm

    def is_vm_l2_isolated_from_dest(self, vm, eth_device, dest_ip):
        """
        True if VM is isolated from dest IP - using arping through the NIC on L2 network:
        If arping can reach destination, then response is greater than 3 (no reply)
        """
        ssh_client = vm.get_ssh_client()
        response = ssh_client.execute("/usr/sbin/arping -c 5 -I %s %s" % (eth_device, str(dest_ip)))
        return len(response) == 3

    def enable_l2_nic(self, vm):
        vm_ip = list([x for x in vm.nic if x['networkid'] == self.isolated_network.id])[0]['ipaddress']
        ssh_client = vm.get_ssh_client()
        eth_device = "eth0"
        if len(ssh_client.execute("/sbin/ifconfig %s | grep %s" % (eth_device, vm_ip))) > 0:
            eth_device = "eth1"
        ssh_client.execute("/sbin/ifconfig %s up" % eth_device)
        return vm_ip, eth_device

    @attr(tags=["advanced", "advancedns", "smoke", "pvlan"], required_hardware="true")
    @skipTestIf("unsupportedHardware")
    def test_l2_network_pvlan_connectivity(self):
        try:
            vm_community1_one = self.deploy_vm_multiple_nics("vmcommunity1one", self.l2_pvlan_community1)
            vm_community1_two = self.deploy_vm_multiple_nics("vmcommunity1two", self.l2_pvlan_community1)
            vm_community2 = self.deploy_vm_multiple_nics("vmcommunity2", self.l2_pvlan_community2)

            vm_isolated1 = self.deploy_vm_multiple_nics("vmisolated1", self.l2_pvlan_isolated)
            vm_isolated2 = self.deploy_vm_multiple_nics("vmisolated2", self.l2_pvlan_isolated)

            vm_promiscuous1 = self.deploy_vm_multiple_nics("vmpromiscuous1", self.l2_pvlan_promiscuous)
            vm_promiscuous2 = self.deploy_vm_multiple_nics("vmpromiscuous2", self.l2_pvlan_promiscuous)

            self.cleanup.append(vm_community1_one)
            self.cleanup.append(vm_community1_two)
            self.cleanup.append(vm_community2)
            self.cleanup.append(vm_isolated1)
            self.cleanup.append(vm_isolated2)
            self.cleanup.append(vm_promiscuous1)
            self.cleanup.append(vm_promiscuous2)

            vm_community1_one_ip, vm_community1_one_eth = self.enable_l2_nic(vm_community1_one)
            vm_community1_two_ip, vm_community1_two_eth = self.enable_l2_nic(vm_community1_two)
            vm_community2_ip, vm_community2_eth = self.enable_l2_nic(vm_community2)
            vm_isolated1_ip, vm_isolated1_eth = self.enable_l2_nic(vm_isolated1)
            vm_isolated2_ip, vm_isolated2_eth = self.enable_l2_nic(vm_isolated2)
            vm_promiscuous1_ip, vm_promiscuous1_eth = self.enable_l2_nic(vm_promiscuous1)
            vm_promiscuous2_ip, vm_promiscuous2_eth = self.enable_l2_nic(vm_promiscuous2)

            # Community PVLAN checks
            different_community_isolated = self.is_vm_l2_isolated_from_dest(vm_community1_one, vm_community1_one_eth,
                                                                            vm_community2_ip)
            same_community_isolated = self.is_vm_l2_isolated_from_dest(vm_community1_one, vm_community1_one_eth,
                                                                       vm_community1_two_ip)
            community_to_promiscuous_isolated = self.is_vm_l2_isolated_from_dest(vm_community1_one,
                                                                                 vm_community1_one_eth,
                                                                                 vm_promiscuous1_ip)
            community_to_isolated = self.is_vm_l2_isolated_from_dest(vm_community1_one, vm_community1_one_eth,
                                                                     vm_isolated1_ip)

            self.assertTrue(
                different_community_isolated,
                "VMs on different community PVLANs must be isolated on layer 2"
            )

            self.assertFalse(
                same_community_isolated,
                "VMs on the same community PVLAN must not be isolated on layer 2"
            )

            self.assertFalse(
                community_to_promiscuous_isolated,
                "VMs on community PVLANs must not be isolated on layer 2 to VMs on promiscuous PVLAN"
            )

            self.assertTrue(
                community_to_isolated,
                "VMs on community PVLANs must be isolated on layer 2 to Vms on isolated PVLAN"
            )

            # Isolated PVLAN checks
            same_isolated = self.is_vm_l2_isolated_from_dest(vm_isolated1, vm_isolated1_eth, vm_isolated2_ip)
            isolated_to_community_isolated = self.is_vm_l2_isolated_from_dest(vm_isolated1, vm_isolated1_eth,
                                                                              vm_community1_one_ip)
            isolated_to_promiscuous_isolated = self.is_vm_l2_isolated_from_dest(vm_isolated1, vm_isolated1_eth,
                                                                                vm_promiscuous1_ip)

            self.assertTrue(
                same_isolated,
                "VMs on isolated PVLANs must be isolated on layer 2"
            )
            self.assertTrue(
                isolated_to_community_isolated,
                "VMs on isolated PVLANs must be isolated on layer 2 to Vms on community PVLAN"
            )
            self.assertFalse(
                isolated_to_promiscuous_isolated,
                "VMs on isolated PVLANs must not be isolated on layer 2 to Vms on promiscuous PVLAN",
            )

            # Promiscuous PVLAN checks
            same_promiscuous = self.is_vm_l2_isolated_from_dest(vm_promiscuous1, vm_promiscuous1_eth,
                                                                vm_promiscuous2_ip)
            prom_to_community_isolated = self.is_vm_l2_isolated_from_dest(vm_promiscuous1, vm_promiscuous1_eth,
                                                                          vm_community1_one_ip)
            prom_to_isolated = self.is_vm_l2_isolated_from_dest(vm_promiscuous1, vm_promiscuous1_eth, vm_isolated1_ip)

            self.assertFalse(
                same_promiscuous,
                "VMs on promiscuous PVLANs must not be isolated on layer 2"
            )
            self.assertFalse(
                prom_to_community_isolated,
                "VMs on promiscuous PVLANs must not be isolated on layer 2 to Vms on isolated PVLAN"
            )
            self.assertFalse(
                prom_to_isolated,
                "VMs on promiscuous PVLANs must not be isolated on layer 2 to Vms on community PVLAN"
            )
        except Exception as e:
            self.fail("Failing test. Error: %s" % e)

        return


class TestSharedNetwork(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestSharedNetwork, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()

        cls.services = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.template = get_template(cls.apiclient, cls.zone.id,
                                    cls.services["ostype"])

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        # Create Network Offering
        cls.services["shared_network_offering"]["specifyVlan"] = "True"
        cls.services["shared_network_offering"]["specifyIpRanges"] = "True"
        cls.hv = cls.testClient.getHypervisorInfo()
        cls.shared_network_offering = NetworkOffering.create(cls.apiclient, cls.services["shared_network_offering"],
                                                             conservemode=False)

        # Update network offering state from disabled to enabled.
        NetworkOffering.update(cls.shared_network_offering, cls.apiclient, state="enabled")

        cls.service_offering = ServiceOffering.create(cls.apiclient, cls.services["service_offering"])
        physical_network, vlan = get_free_vlan(cls.apiclient, cls.zone.id)
        # create network using the shared network offering created

        cls.services["shared_network"]["acltype"] = "domain"
        cls.services["shared_network"]["vlan"] = vlan
        cls.services["shared_network"]["networkofferingid"] = cls.shared_network_offering.id
        cls.services["shared_network"]["physicalnetworkid"] = physical_network.id

        cls.setSharedNetworkParams("shared_network")
        cls.shared_network = Network.create(cls.apiclient,
                                            cls.services["shared_network"],
                                            networkofferingid=cls.shared_network_offering.id,
                                            zoneid=cls.zone.id)
        cls._cleanup = [
            cls.service_offering,
            cls.shared_network,
            cls.shared_network_offering
        ]
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @classmethod
    def setSharedNetworkParams(cls, network, range=20):

        # @range: range decides the endip. Pass the range as "x" if you want the difference between the startip
        # and endip as "x"
        # Set the subnet number of shared networks randomly prior to execution
        # of each test case to avoid overlapping of ip addresses
        shared_network_subnet_number = random.randrange(1, 254)
        cls.services[network]["routerip"] = "172.16." + str(shared_network_subnet_number) + "." + str(15)
        cls.services[network]["gateway"] = "172.16." + str(shared_network_subnet_number) + ".1"
        cls.services[network]["startip"] = "172.16." + str(shared_network_subnet_number) + ".2"
        cls.services[network]["endip"] = "172.16." + str(shared_network_subnet_number) + "." + str(range + 1)
        cls.services[network]["netmask"] = "255.255.255.0"
        logger.debug("Executing command '%s'" % cls.services[network])

    def get_router_host(self, router):
        self.assertEqual(
            router.state,
            'Running',
            "Check list router response for router state"
        )
        hosts = list_hosts(
            self.apiclient,
            id=router.hostid)
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check for list hosts response return valid data")
        host = hosts[0]
        if host.hypervisor.lower() in ("vmware", "hyperv"):
            host.ipaddress = self.apiclient.connection.mgtSvr
            host.user = self.apiclient.connection.user
            host.password = self.apiclient.connection.passwd
            host.port = 22
        else:
            host.user, host.password = get_host_credentials(self.config, host.ipaddress)
            host.port = 22
        return host

    def verify_ip_address_in_router(self, router, host, ipaddress, device, isExist=True):
        command = 'ip addr show %s |grep "inet "|cut -d " " -f6 |cut -d "/" -f1 |grep -w %s' % (device, ipaddress)
        logger.debug("Executing command '%s'" % command)
        result = get_process_status(
            host.ipaddress,
            host.port,
            host.user,
            host.password,
            router.linklocalip,
            command,
            host.hypervisor.lower())
        self.assertEqual(len(result) > 0 and result[0] == ipaddress, isExist, "ip %s verification failed" % ipaddress)

    @attr(tags=["advanced", "shared"])
    def test_01_deployVMInSharedNetwork(self):
        if self.hv.lower() == 'simulator':
            self.skipTest("Hypervisor is simulator - skipping Test..")
        try:
            self.virtual_machine = VirtualMachine.create(self.apiclient, self.services["virtual_machine"],
                                                         networkids=[self.shared_network.id],
                                                         serviceofferingid=self.service_offering.id
                                                         )
        except Exception as e:
            self.fail("Exception while deploying virtual machine: %s" % e)

        routerIp = self.services["shared_network"]["routerip"]
        nic_ip_address = self.dbclient.execute(
            "select ip4_address from nics where strategy='Placeholder' and ip4_address = '%s';" % routerIp);

        self.assertNotEqual(
            len(nic_ip_address),
            0,
            "Placeholder ip for the VR in shared network isn't the same as what was passed"
        )

        routers = Router.list(
            self.apiclient,
            networkid=self.shared_network.id,
            listall=True
        )

        for router in routers:
            host = self.get_router_host(router)
            self.verify_ip_address_in_router(router, host, routerIp, "eth0", True)

        # expunge VM
        VirtualMachine.delete(self.virtual_machine, self.apiclient, expunge=True)

    @attr(tags=["advanced", "shared"])
    def test_02_verifyRouterIpAfterNetworkRestart(self):
        if self.hv.lower() == 'simulator':
            self.skipTest("Hypervisor is simulator - skipping Test..")
        routerIp = self.services["shared_network"]["routerip"]
        self.debug("restarting network with cleanup")
        try:
            self.shared_network.restart(self.apiclient, cleanup=True)
        except Exception as e:
            self.fail("Failed to cleanup network - %s" % e)

        self.debug("Listing routers for network: %s" % self.shared_network.name)
        routers = Router.list(
            self.apiclient,
            networkid=self.shared_network.id,
            listall=True
        )
        self.assertEqual(
            len(routers),
            1,
            "Router for the shared network wasn't found)"
        )

        for router in routers:
            host = self.get_router_host(router)
            self.verify_ip_address_in_router(router, host, routerIp, "eth0", True)

    @attr(tags=["advanced", "shared"])
    def test_03_destroySharedNetwork(self):
        if self.hv.lower() == 'simulator':
            self.skipTest("Hypervisor is simulator - skipping Test..")
        routerIp = self.services["shared_network"]["routerip"]
        try:
            self.shared_network.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to destroy the shared network")
        self._cleanup.remove(self.shared_network)
        self.debug("Fetch the placeholder record for the router")
        nic_ip_address = self.dbclient.execute(
            "select ip4_address from nics where strategy='Placeholder' and ip4_address = '%s' and removed is NOT NULL;" % routerIp);

        self.assertNotEqual(
            len(nic_ip_address),
            0,
            "Failed to find the placeholder IP"
        )


class TestSharedNetworkWithConfigDrive(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestSharedNetworkWithConfigDrive, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()

        cls.services = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.hv = cls.testClient.getHypervisorInfo()

        if cls.hv.lower() == 'simulator':
            cls.skip = True
            return
        else:
            cls.skip = False

        cls._cleanup = []

        template = Template.register(
            cls.apiclient,
            cls.services["test_templates_cloud_init"][cls.hv.lower()],
            zoneid=cls.zone.id,
            hypervisor=cls.hv,
        )
        template.download(cls.apiclient)
        cls._cleanup.append(template)

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id
        cls.services["virtual_machine"]["username"] = "ubuntu"
        # Create Network Offering
        cls.services["shared_network_offering_configdrive"]["specifyVlan"] = "True"
        cls.services["shared_network_offering_configdrive"]["specifyIpRanges"] = "True"
        cls.shared_network_offering = NetworkOffering.create(cls.apiclient,
                                                             cls.services["shared_network_offering_configdrive"],
                                                             conservemode=True)

        cls.isolated_network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.services["isolated_network_offering"],
            conservemode=True
        )

        # Update network offering state from disabled to enabled.
        NetworkOffering.update(
            cls.isolated_network_offering,
            cls.apiclient,
            id=cls.isolated_network_offering.id,
            state="enabled"
        )

        # Update network offering state from disabled to enabled.
        NetworkOffering.update(cls.shared_network_offering, cls.apiclient, state="enabled")

        cls.service_offering = ServiceOffering.create(cls.apiclient, cls.services["service_offering"])
        physical_network, vlan = get_free_vlan(cls.apiclient, cls.zone.id)
        # create network using the shared network offering created

        cls.services["shared_network"]["acltype"] = "domain"
        cls.services["shared_network"]["vlan"] = vlan
        cls.services["shared_network"]["networkofferingid"] = cls.shared_network_offering.id
        cls.services["shared_network"]["physicalnetworkid"] = physical_network.id

        cls.setSharedNetworkParams("shared_network")
        cls.shared_network = Network.create(cls.apiclient,
                                            cls.services["shared_network"],
                                            networkofferingid=cls.shared_network_offering.id,
                                            zoneid=cls.zone.id)

        cls.isolated_network = Network.create(
            cls.apiclient,
            cls.services["isolated_network"],
            networkofferingid=cls.isolated_network_offering.id,
            zoneid=cls.zone.id
        )

        cls._cleanup.extend([
            cls.service_offering,
            cls.shared_network,
            cls.shared_network_offering,
            cls.isolated_network,
            cls.isolated_network_offering,
        ])
        cls.tmp_files = []
        cls.keypair = cls.generate_ssh_keys()
        return

    @classmethod
    def generate_ssh_keys(cls):
        """Generates ssh key pair

        Writes the private key into a temp file and returns the file name

        :returns: generated keypair
        :rtype: MySSHKeyPair
        """
        cls.keypair = SSHKeyPair.create(
            cls.apiclient,
            name=random_gen() + ".pem")

        cls._cleanup.append(SSHKeyPair(cls.keypair.__dict__, None))
        cls.debug("Created keypair with name: %s" % cls.keypair.name)
        cls.debug("Writing the private key to local file")
        pkfile = tempfile.gettempdir() + os.sep + cls.keypair.name
        cls.keypair.private_key_file = pkfile
        cls.tmp_files.append(pkfile)
        cls.debug("File path: %s" % pkfile)
        with open(pkfile, "w+") as f:
            f.write(cls.keypair.privatekey)
        os.chmod(pkfile, 0o400)

        return cls.keypair

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        if self.skip:
            self.skipTest("Hypervisor is simulator - skipping Test..")
        self.cleanup = []

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)
            for tmp_file in cls.tmp_files:
                os.remove(tmp_file)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @classmethod
    def setSharedNetworkParams(cls, network, range=20):

        # @range: range decides the endip. Pass the range as "x" if you want the difference between the startip
        # and endip as "x"
        # Set the subnet number of shared networks randomly prior to execution
        # of each test case to avoid overlapping of ip addresses
        shared_network_subnet_number = random.randrange(1, 254)
        cls.services[network]["gateway"] = "172.16." + str(shared_network_subnet_number) + ".1"
        cls.services[network]["startip"] = "172.16." + str(shared_network_subnet_number) + ".2"
        cls.services[network]["endip"] = "172.16." + str(shared_network_subnet_number) + "." + str(range + 1)
        cls.services[network]["netmask"] = "255.255.255.0"
        logger.debug("Executing command '%s'" % cls.services[network])

    def _mount_config_drive(self, ssh):
        """
        This method is to verify whether configdrive iso
        is attached to vm or not
        Returns mount path if config drive is attached else None
        """
        mountdir = "/root/iso"
        cmd = "sudo blkid -t LABEL='config-2' " \
              "/dev/sr? /dev/hd? /dev/sd? /dev/xvd? -o device"
        tmp_cmd = [
            'sudo bash -c "if [ ! -d {0} ]; then mkdir {0}; fi"'.format(mountdir),
            "sudo umount %s" % mountdir]
        self.debug("Unmounting drive from %s" % mountdir)
        for tcmd in tmp_cmd:
            ssh.execute(tcmd)

        self.debug("Trying to find ConfigDrive device")
        configDrive = ssh.execute(cmd)
        if not configDrive:
            self.warn("ConfigDrive is not attached")
            return None

        res = ssh.execute("sudo mount {} {}".format(str(configDrive[0]), mountdir))
        if str(res).lower().find("read-only") > -1:
            self.debug("ConfigDrive iso is mounted at location %s" % mountdir)
            return mountdir
        else:
            return None

    def _umount_config_drive(self, ssh, mount_path):
        """unmount config drive inside guest vm

        :param ssh: SSH connection to the VM
        :type ssh: marvin.sshClient.SshClient
        :type mount_path: str
        """
        ssh.execute("sudo umount -d %s" % mount_path)
        # Give the VM time to unlock the iso device
        time.sleep(0.5)
        # Verify umount
        result = ssh.execute("sudo ls %s" % mount_path)
        self.assertTrue(len(result) == 0,
                        "After umount directory should be empty "
                        "but contains: %s" % result)

    def _get_config_drive_data(self, ssh, file, name, fail_on_missing=True):
        """Fetches the content of a file file on the config drive

        :param ssh: SSH connection to the VM
        :param file: path to the file to fetch
        :param name: description of the file
        :param fail_on_missing:
                 whether the test should fail if the file is missing
        :type ssh: marvin.sshClient.SshClient
        :type file: str
        :type name: str
        :type fail_on_missing: bool
        :returns: the content of the file
        :rtype: str
        """
        cmd = "sudo cat %s" % file
        res = ssh.execute(cmd)
        content = '\n'.join(res)

        if fail_on_missing and "No such file or directory" in content:
            self.debug("{} is not found".format(name))
            self.fail("{} is not found".format(name))

        return content

    def _get_ip_address_output(self, ssh):
        cmd = "ip address"
        res = ssh.execute(cmd)
        return '\n'.join(res)

    @attr(tags=["advanced", "shared"], required_hardware="true")
    def test_01_deployVMInSharedNetwork(self):
        try:
            self.virtual_machine = VirtualMachine.create(self.apiclient, self.services["virtual_machine"],
                                                         networkids=[self.shared_network.id, self.isolated_network.id],
                                                         serviceofferingid=self.service_offering.id,
                                                         keypair=self.keypair.name
                                                         )
            self.cleanup.append(self.virtual_machine)
        except Exception as e:
            self.fail("Exception while deploying virtual machine: %s" % e)

        public_ips = list_publicIP(
            self.apiclient,
            associatednetworkid=self.isolated_network.id
        )
        public_ip = public_ips[0]
        FireWallRule.create(
            self.apiclient,
            ipaddressid=public_ip.id,
            protocol=self.services["natrule"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["natrule"]["publicport"],
            endport=self.services["natrule"]["publicport"]
        )

        nat_rule = NATRule.create(
            self.apiclient,
            self.virtual_machine,
            self.services["natrule"],
            public_ip.id
        )

        private_key_file_location = self.keypair.private_key_file if self.keypair else None
        ssh = self.virtual_machine.get_ssh_client(ipaddress=nat_rule.ipaddress,
                                                  keyPairFileLocation=private_key_file_location, retries=5)

        mount_path = self._mount_config_drive(ssh)

        network_data_content = self._get_config_drive_data(ssh, mount_path + "/openstack/latest/network_data.json",
                                                           "network_data")

        network_data = json.loads(network_data_content)

        self._umount_config_drive(ssh, mount_path)

        ip_address_output = self._get_ip_address_output(ssh)

        self.assertTrue('links' in network_data, "network_data.json doesn't contain links")
        self.assertTrue('networks' in network_data, "network_data.json doesn't contain networks")
        self.assertTrue('services' in network_data, "network_data.json doesn't contain services")

        for x in ['links', 'networks', 'services']:
            self.assertTrue(x in network_data, "network_data.json doesn't contain " + x)
            self.assertEqual(len(network_data[x]), 2, "network_data.json doesn't contain 2 " + x)

        self.assertIn(network_data['links'][0]['ethernet_mac_address'],
                      [self.virtual_machine.nic[0].macaddress, self.virtual_machine.nic[1].macaddress],
                      "macaddress doesn't match")
        self.assertIn(network_data['links'][1]['ethernet_mac_address'],
                      [self.virtual_machine.nic[0].macaddress, self.virtual_machine.nic[1].macaddress],
                      "macaddress doesn't match")

        self.assertIn(network_data['networks'][0]['ip_address'],
                      [self.virtual_machine.nic[0].ipaddress, self.virtual_machine.nic[1].ipaddress],
                      "ip address doesn't match")
        self.assertIn(network_data['networks'][1]['ip_address'],
                      [self.virtual_machine.nic[0].ipaddress, self.virtual_machine.nic[1].ipaddress],
                      "ip address doesn't match")
        self.assertIn(network_data['networks'][0]['netmask'],
                      [self.virtual_machine.nic[0].netmask, self.virtual_machine.nic[1].netmask],
                      "netmask doesn't match")
        self.assertIn(network_data['networks'][1]['netmask'],
                      [self.virtual_machine.nic[0].netmask, self.virtual_machine.nic[1].netmask],
                      "netmask doesn't match")

        self.assertEqual(network_data['services'][0]['type'], 'dns', "network_data.json doesn't contain dns service")
        self.assertEqual(network_data['services'][1]['type'], 'dns', "network_data.json doesn't contain dns service")

        self.assertTrue(self.virtual_machine.nic[0].ipaddress in ip_address_output, "ip address doesn't match")
        self.assertTrue(self.virtual_machine.nic[1].ipaddress in ip_address_output, "ip address doesn't match")
