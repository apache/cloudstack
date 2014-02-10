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
#Import Local Modules
import marvin
from marvin.cloudstackException import cloudstackAPIException
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.sshClient import SshClient
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from nose.plugins.attrib import attr
#Import System modules
import time

_multiprocess_shared_ = True

class Services:
    """Test Network Services
    """

    def __init__(self):
        self.services = {
                            "ostype": "CentOS 5.3 (64-bit)",
                            # Cent OS 5.3 (64 bit)
                            "lb_switch_wait": 10,
                            # Time interval after which LB switches the requests
                            "sleep": 60,
                            "timeout":10,
                            "network_offering": {
                                    "name": 'Test Network offering',
                                    "displaytext": 'Test Network offering',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "serviceProviderList" : {
                                            "Dhcp": 'VirtualRouter',
                                            "Dns": 'VirtualRouter',
                                            "SourceNat": 'VirtualRouter',
                                            "PortForwarding": 'VirtualRouter',
                                        },
                                },
                            "network": {
                                  "name": "Test Network",
                                  "displaytext": "Test Network",
                                },
                            "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 100,
                                    # in MHz
                                    "memory": 256,
                                    # In MBs
                                    },
                            "account": {
                                    "email": "test@test.com",
                                    "firstname": "Test",
                                    "lastname": "User",
                                    "username": "test",
                                    "password": "password",
                                    },
                            "server":
                                    {
                                    "displayname": "Small Instance",
                                    "username": "root",
                                    "password": "password",
                                    "hypervisor": 'XenServer',
                                    "privateport": 22,
                                    "publicport": 22,
                                    "ssh_port": 22,
                                    "protocol": 'TCP',
                                },
                        "natrule":
                                {
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": "TCP"
                                },
                        "lbrule":
                                {
                                    "name": "SSH",
                                    "alg": "roundrobin",
                                    # Algorithm used for load balancing
                                    "privateport": 22,
                                    "publicport": 2222,
                                    "protocol": 'TCP'
                                }
                        }


class TestPublicIP(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.services = Services().services

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestPublicIP, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.services['mode'] = cls.zone.networktype
        # Create Accounts & networks
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain.id
                            )

        cls.user = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.services["network"]["zoneid"] = cls.zone.id

        cls.network_offering = NetworkOffering.create(
                                    cls.api_client,
                                    cls.services["network_offering"],
                                    )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')

        cls.services["network"]["networkoffering"] = cls.network_offering.id
        cls.account_network = Network.create(
                                             cls.api_client,
                                             cls.services["network"],
                                             cls.account.name,
                                             cls.account.domainid
                                             )
        cls.user_network = Network.create(
                                             cls.api_client,
                                             cls.services["network"],
                                             cls.user.name,
                                             cls.user.domainid
                                             )

        # Create Source NAT IP addresses
        account_src_nat_ip = PublicIPAddress.create(
                                            cls.api_client,
                                            cls.account.name,
                                            cls.zone.id,
                                            cls.account.domainid
                                            )
        user_src_nat_ip = PublicIPAddress.create(
                                            cls.api_client,
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
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advanced", "advancedns", "smoke"])
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
        #listPublicIpAddresses should return newly created public IP
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

        # Validate the following:
        # 1.listPublicIpAddresses should no more return the released address
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

    @attr(tags = ["advanced", "advancedns", "smoke"])
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

        #listPublicIpAddresses should return newly created public IP
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

        cls.api_client = super(TestPortForwarding, cls).getClsTestClient().getApiClient()
        cls.services = Services().services

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        #Create an account, network, VM and IP addresses
        cls.account = Account.create(
                                cls.api_client,
                                cls.services["account"],
                                admin=True,
                                domainid=cls.domain.id
                                )
        cls.services["server"]["zoneid"] = cls.zone.id
        cls.service_offering = ServiceOffering.create(
                                cls.api_client,
                                cls.services["service_offering"]
                                )
        cls.virtual_machine = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["server"],
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
            cls.api_client = super(TestPortForwarding, cls).getClsTestClient().getApiClient()
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @attr(tags = ["advanced", "advancedns", "smoke"])
    def test_01_port_fwd_on_src_nat(self):
        """Test for port forwarding on source NAT"""

        #Validate the following:
        #1. listPortForwarding rules API should return the added PF rule
        #2. attempt to do an ssh into the  user VM through the sourceNAT

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
        fw_rule = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=src_nat_ip_addr.id,
                            protocol=self.services["natrule"]["protocol"],
                            cidrlist=['0.0.0.0/0'],
                            startport=self.services["natrule"]["publicport"],
                            endport=self.services["natrule"]["publicport"]
                            )

        #Create NAT rule
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
        #SSH virtual machine to test port forwarding
        try:
            self.debug("SSHing into VM with IP address %s with NAT IP %s" %
                                    (
                                     self.virtual_machine.ipaddress,
                                     src_nat_ip_addr.ipaddress
                                    ))

            self.virtual_machine.get_ssh_client(src_nat_ip_addr.ipaddress)

        except Exception as e:
            self.fail(
                      "SSH Access failed for %s: %s" % \
                      (self.virtual_machine.ipaddress, e)
                      )

        nat_rule.delete(self.apiclient)

        try:
            list_nat_rule_response = list_nat_rules(
                                                self.apiclient,
                                                id=nat_rule.id
                                                )
        except cloudstackAPIException:
            self.debug("Nat Rule is deleted")

        # Check if the Public SSH port is inaccessible
        with self.assertRaises(Exception):
            self.debug(
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

    @attr(tags = ["advanced", "advancedns", "smoke"])
    def test_02_port_fwd_on_non_src_nat(self):
        """Test for port forwarding on non source NAT"""

        #Validate the following:
        #1. listPortForwardingRules should not return the deleted rule anymore
        #2. attempt to do ssh should now fail

        ip_address = PublicIPAddress.create(
                                            self.apiclient,
                                            self.account.name,
                                            self.zone.id,
                                            self.account.domainid,
                                            self.services["server"]
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
        fw_rule = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=ip_address.ipaddress.id,
                            protocol=self.services["natrule"]["protocol"],
                            cidrlist=['0.0.0.0/0'],
                            startport=self.services["natrule"]["publicport"],
                            endport=self.services["natrule"]["publicport"]
                            )
        #Create NAT rule
        nat_rule = NATRule.create(
                                  self.apiclient,
                                  self.virtual_machine,
                                  self.services["natrule"],
                                  ip_address.ipaddress.id
                                  )
        #Validate the following:
        #1. listPortForwardingRules should not return the deleted rule anymore
        #2. attempt to do ssh should now fail

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
                      "SSH Access failed for %s: %s" % \
                      (self.virtual_machine.ipaddress, e)
                      )

        nat_rule.delete(self.apiclient)

        try:
            list_nat_rule_response = list_nat_rules(
                                                self.apiclient,
                                                id=nat_rule.id
                                                )
        except cloudstackAPIException:
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




class TestRebootRouter(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.services = Services().services

        # Get Zone, Domain and templates
        self.domain = get_domain(self.apiclient, self.services)
        self.zone = get_zone(self.apiclient, self.services)
        template = get_template(
                            self.apiclient,
                            self.zone.id,
                            self.services["ostype"]
                            )
        self.services["server"]["zoneid"] = self.zone.id

        #Create an account, network, VM and IP addresses
        self.account = Account.create(
                                      self.apiclient,
                                      self.services["account"],
                                      admin=True,
                                      domainid=self.domain.id
                                      )
        self.service_offering = ServiceOffering.create(
                                            self.apiclient,
                                            self.services["service_offering"]
                                            )
        self.vm_1 = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["server"],
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
            raise Exception("Warning: Exception during fetching source NAT: %s" % e)

        self.public_ip = PublicIPAddress.create(
                                            self.apiclient,
                                            self.vm_1.account,
                                            self.vm_1.zoneid,
                                            self.vm_1.domainid,
                                            self.services["server"]
                                            )
        #Open up firewall port for SSH
        fw_rule = FireWallRule.create(
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
        self.cleanup = [
                        self.vm_1,
                        lb_rule,
                        self.nat_rule,
                        self.service_offering,
                        self.account,
                        ]
        return

    @attr(tags = ["advanced", "advancedns", "smoke"])
    def test_reboot_router(self):
        """Test for reboot router"""

        #Validate the Following
        #1. Post restart PF and LB rules should still function
        #2. verify if the ssh into the virtual machine
        #   still works through the sourceNAT Ip

        #Retrieve router for the user account

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
                        "Failed to start VM (ID: %s) in change service offering" % vm.id)

            timeout = timeout - 1

        #we should be able to SSH after successful reboot
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
                      "SSH Access failed for %s: %s" % \
                      (self.public_ip.ipaddress.ipaddress, e))
        return

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return


class TestReleaseIP(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.services = Services().services

        # Get Zone, Domain and templates
        self.domain = get_domain(self.apiclient, self.services)
        self.zone = get_zone(self.apiclient, self.services)
        template = get_template(
                            self.apiclient,
                            self.zone.id,
                            self.services["ostype"]
                            )
        self.services["server"]["zoneid"] = self.zone.id

        #Create an account, network, VM, Port forwarding rule, LB rules
        self.account = Account.create(
                                      self.apiclient,
                                      self.services["account"],
                                      admin=True,
                                      domainid=self.domain.id
                                      )

        self.service_offering = ServiceOffering.create(
                                           self.apiclient,
                                           self.services["service_offering"]
                                         )

        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["server"],
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
                                    domainid=self.account.domainid
                                  )
        try:
            self.ip_addr = ip_addrs[0]
        except Exception as e:
            raise Exception("Failed: During acquiring source NAT for account: %s" %
                                self.account.name)

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

    @attr(tags = ["advanced", "advancedns", "smoke"])
    def test_releaseIP(self):
        """Test for release public IP address"""

        self.debug("Deleting Public IP : %s" % self.ip_addr.id)

        self.ip_address.delete(self.apiclient)

        # Sleep to ensure that deleted state is reflected in other calls 
        time.sleep(self.services["sleep"])

        # ListPublicIpAddresses should not list deleted Public IP address
        list_pub_ip_addr_resp = list_publicIP(
                                    self.apiclient,
                                    id=self.ip_addr.id
                                  )
        self.debug("List Public IP response" + str(list_pub_ip_addr_resp))

        self.assertEqual(
                     list_pub_ip_addr_resp,
                     None,
                    "Check if disassociated IP Address is no longer available"
                   )

        # ListPortForwardingRules should not list
        # associated rules with Public IP address
        try:
            list_nat_rule = list_nat_rules(
                                        self.apiclient,
                                        id=self.nat_rule.id
                                        )
            self.debug("List NAT Rule response" + str(list_nat_rule))
        except cloudstackAPIException:
            self.debug("Port Forwarding Rule is deleted")

        # listLoadBalancerRules should not list
        # associated rules with Public IP address
        try:
            list_lb_rule = list_lb_rules(
                                     self.apiclient,
                                     id=self.lb_rule.id
                                     )
            self.debug("List LB Rule response" + str(list_lb_rule))
        except cloudstackAPIException:
            self.debug("Port Forwarding Rule is deleted")

        # SSH Attempt though public IP should fail
        with self.assertRaises(Exception):
            ssh_2 = SshClient(
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
        self.services = Services().services

        # Get Zone, Domain and templates
        self.domain = get_domain(self.apiclient, self.services)
        self.zone = get_zone(self.apiclient, self.services)
        template = get_template(
                            self.apiclient,
                            self.zone.id,
                            self.services["ostype"]
                            )
        self.services["server"]["zoneid"] = self.zone.id

        #Create an account, network, VM and IP addresses
        self.account = Account.create(
                                self.apiclient,
                                self.services["account"],
                                admin=True,
                                domainid=self.domain.id
                                )
        self.service_offering = ServiceOffering.create(
                                    self.apiclient,
                                    self.services["service_offering"]
                                    )
        self.vm_1 = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["server"],
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
            self.fail("SSH failed for VM with IP: %s" %
                                    src_nat_ip_addr.ipaddress)

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

    @attr(tags = ["advanced", "advancedns", "smoke"])
    def test_delete_account(self):
        """Test for delete account"""

        #Validate the Following
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
            list_lb_reponse = list_lb_rules(
                                    self.apiclient,
                                    account=self.account.name,
                                    domainid=self.account.domainid
                                    )
        except cloudstackAPIException:
            self.debug("Port Forwarding Rule is deleted")

        # ListPortForwardingRules should not
        # list associated rules with deleted account
        try:
            list_nat_reponse = list_nat_rules(
                                    self.apiclient,
                                    account=self.account.name,
                                    domainid=self.account.domainid
                        )
        except cloudstackAPIException:
            self.debug("NATRule is deleted")

        #Retrieve router for the user account
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
        except cloudstackAPIException:
            self.debug("Router is deleted")

        except Exception as e:
            raise Exception(
                "Encountered %s raised while fetching routers for account: %s" % (e,
                                                    self.account.name))
        return

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return
