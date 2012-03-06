# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#

""" BVT tests for Network Life Cycle
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
import remoteSSHClient
from testcase.libs.utils import *
from testcase.libs.base import *
from testcase.libs.common import *
#Import System modules
import time


class Services:
    """Test Network Services
    """

    def __init__(self):
        self.services = {
                            "ostypeid": '0c2c5d19-525b-41be-a8c3-c6607412f82b',
                            # Cent OS 5.3 (64 bit)
                            "zoneid": '4a6c0290-e64d-40fc-afbb-4a05cab6fa4b',
                            # Optional, if specified the mentioned zone will be
                            # used for tests
                            "mode": 'advanced',
                            # Networking mode: Basic or advanced
                            "lb_switch_wait": 10,
                            # Time interval after which LB switches the requests
                            "sleep": 60,
                            "timeout":10,
                            "network": {
                                  "name": "Test Network",
                                  "displaytext": "Test Network",
                                  "networkoffering": '4c6dea7e-7aa8-4b17-bf1c-26c312586e7c',
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
                                    "domainid": '9ee36d2e-8b8f-432e-a927-a678ebec1d6b',
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
                                }
                        }


class TestPublicIP(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.services = Services().services

    @classmethod
    def setUpClass(cls):
        cls.api_client = fetch_api_client()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.api_client, cls.services)

        # Create Accounts & networks
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True
                            )

        cls.user = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            )
        cls.services["network"]["zoneid"] = cls.zone.id
        cls.account_network = Network.create(
                                             cls.api_client,
                                             cls.services["network"],
                                             cls.account.account.name,
                                             cls.account.account.domainid
                                             )
        cls.user_network = Network.create(
                                             cls.api_client,
                                             cls.services["network"],
                                             cls.user.account.name,
                                             cls.user.account.domainid
                                             )

        # Create Source NAT IP addresses
        account_src_nat_ip = PublicIPAddress.create(
                                            cls.api_client,
                                            cls.account.account.name,
                                            cls.zone.id,
                                            cls.account.account.domainid
                                            )
        user_src_nat_ip = PublicIPAddress.create(
                                            cls.api_client,
                                            cls.user.account.name,
                                            cls.zone.id,
                                            cls.user.account.domainid
                                            )
        cls._cleanup = [
                        cls.account_network,
                        cls.user_network,
                        cls.account,
                        cls.user,
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

    def test_public_ip_admin_account(self):
        """Test for Associate/Disassociate
        public IP address for admin account"""

        # Validate the following:
        # 1. listPubliIpAddresses API returns the list of acquired addresses
        # 2. the returned list should contain our acquired IP address

        ip_address = PublicIPAddress.create(
                                            self.apiclient,
                                            self.account.account.name,
                                            self.zone.id,
                                            self.account.account.domainid
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

    def test_public_ip_user_account(self):
        """Test for Associate/Disassociate
            public IP address for user account"""

        # Validate the following:
        # 1. listPubliIpAddresses API returns the list of acquired addresses
        # 2. the returned list should contain our acquired IP address

        ip_address = PublicIPAddress.create(
                                            self.apiclient,
                                            self.user.account.name,
                                            self.zone.id,
                                            self.user.account.domainid
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

        cls.api_client = fetch_api_client()
        cls.services = Services().services

        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.api_client, cls.services)
        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostypeid"]
                            )
        #Create an account, network, VM and IP addresses
        cls.account = Account.create(
                                cls.api_client,
                                cls.services["account"],
                                admin=True
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
                                    accountid=cls.account.account.name,
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
            cls.api_client = fetch_api_client()
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return

    def test_01_port_fwd_on_src_nat(self):
        """Test for port forwarding on source NAT"""

        #Validate the following:
        #1. listPortForwarding rules API should return the added PF rule
        #2. attempt to do an ssh into the  user VM through the sourceNAT

        src_nat_ip_addrs = list_publicIP(
                                    self.apiclient,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
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

        list_nat_rule_response = list_nat_rules(
                                                self.apiclient,
                                                id=nat_rule.id
                                                )

        self.assertEqual(
                            list_nat_rule_response,
                            None,
                            "Check Port Forwarding Rule is deleted"
                            )
        # Check if the Public SSH port is inaccessible
        with self.assertRaises(Exception):
            self.debug(
                "SSHing into VM with IP address %s after NAT rule deletion" %
                                                 self.virtual_machine.ipaddress)
            
            remoteSSHClient.remoteSSHClient(
                                            src_nat_ip_addr.ipaddress,
                                            self.virtual_machine.ssh_port,
                                            self.virtual_machine.username,
                                            self.virtual_machine.password
                                            )
        return

    def test_02_port_fwd_on_non_src_nat(self):
        """Test for port forwarding on non source NAT"""

        #Validate the following:
        #1. listPortForwardingRules should not return the deleted rule anymore
        #2. attempt to do ssh should now fail

        ip_address = PublicIPAddress.create(
                                            self.apiclient,
                                            self.account.account.name,
                                            self.zone.id,
                                            self.account.account.domainid,
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

        list_nat_rule_response = list_nat_rules(
                                                self.apiclient,
                                                id=nat_rule.id
                                                )
        self.assertEqual(
                            list_nat_rule_response,
                            None,
                            "Check Port Forwarding Rule is deleted"
                            )
        # Check if the Public SSH port is inaccessible
        with self.assertRaises(Exception):
            self.debug(
                "SSHing into VM with IP address %s after NAT rule deletion" %
                                                 self.virtual_machine.ipaddress)
            
            remoteSSHClient.remoteSSHClient(
                                            ip_address.ipaddress.ipaddress,
                                            self.virtual_machine.ssh_port,
                                            self.virtual_machine.username,
                                            self.virtual_machine.password
                                            )
        return


class TestLoadBalancingRule(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.api_client = fetch_api_client()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.api_client, cls.services)
        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostypeid"]
                            )
        cls.services["server"]["zoneid"] = cls.zone.id

        #Create an account, network, VM and IP addresses
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True
                            )
        cls.service_offering = ServiceOffering.create(
                                        cls.api_client,
                                        cls.services["service_offering"]
                                        )
        cls.vm_1 = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["server"],
                                    templateid=template.id,
                                    accountid=cls.account.account.name,
                                    serviceofferingid=cls.service_offering.id
                                    )
        cls.vm_2 = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["server"],
                                    templateid=template.id,
                                    accountid=cls.account.account.name,
                                    serviceofferingid=cls.service_offering.id
                                    )
        cls.non_src_nat_ip = PublicIPAddress.create(
                                            cls.api_client,
                                            cls.account.account.name,
                                            cls.zone.id,
                                            cls.account.account.domainid,
                                            cls.services["server"]
                                            )
        cls._cleanup = [
                        cls.account,
                        cls.service_offering
                        ]

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        cleanup_resources(cls.api_client, cls._cleanup)
        return

    def test_01_create_lb_rule_src_nat(self):
        """Test to create Load balancing rule with source NAT"""

        # Validate the Following:
        #1. listLoadBalancerRules should return the added rule
        #2. attempt to ssh twice on the load balanced IP
        #3. verify using the hostname of the VM
        #   that round robin is indeed happening as expected

        src_nat_ip_addrs = list_publicIP(
                                    self.apiclient,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                  )
        self.assertEqual(
                            isinstance(src_nat_ip_addrs, list),
                            True,
                            "Check list response returns a valid list"
                        )
        src_nat_ip_addr = src_nat_ip_addrs[0]
        
        # Check if VM is in Running state before creating LB rule
        vm_response = VirtualMachine.list(
                                          self.apiclient,
                                          account=self.account.account.name,
                                          domainid=self.account.account.domainid
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
        for vm in vm_response:
            self.assertEqual(
                            vm.state,
                            'Running',
                            "VM state should be Running before creating a NAT rule."
                        )
        
        #Create Load Balancer rule and assign VMs to rule
        lb_rule = LoadBalancerRule.create(
                                          self.apiclient,
                                          self.services["lbrule"],
                                          src_nat_ip_addr.id,
                                          accountid=self.account.account.name
                                          )
        self.cleanup.append(lb_rule)

        lb_rule.assign(self.apiclient, [self.vm_1, self.vm_2])

        lb_rules = list_lb_rules(
                                 self.apiclient,
                                 id=lb_rule.id
                                 )
        self.assertEqual(
                            isinstance(lb_rules, list),
                            True,
                            "Check list response returns a valid list"
                        )
        #verify listLoadBalancerRules lists the added load balancing rule
        self.assertNotEqual(
                            len(lb_rules),
                            0,
                            "Check Load Balancer Rule in its List"
                        )
        self.assertEqual(
                            lb_rules[0].id,
                            lb_rule.id,
                            "Check List Load Balancer Rules returns valid Rule"
                        )

        # listLoadBalancerRuleInstances should list all
        # instances associated with that LB rule
        lb_instance_rules = list_lb_instances(
                                              self.apiclient,
                                              id=lb_rule.id
                                              )
        self.assertEqual(
                            isinstance(lb_instance_rules, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            len(lb_instance_rules),
                            0,
                            "Check Load Balancer instances Rule in its List"
                        )

        self.assertIn(
                lb_instance_rules[0].id,
                [self.vm_1.id, self.vm_2.id],
                "Check List Load Balancer instances Rules returns valid VM ID"
            )

        self.assertIn(
            lb_instance_rules[1].id,
            [self.vm_1.id, self.vm_2.id],
            "Check List Load Balancer instances Rules returns valid VM ID"
            )
        try:
            self.debug(
                "SSH into VM (IPaddress: %s) & NAT Rule (Public IP: %s)"%
                (self.vm_1.ipaddress, src_nat_ip_addr.ipaddress)
                )
            
            ssh_1 = remoteSSHClient.remoteSSHClient(
                                        src_nat_ip_addr.ipaddress,
                                        self.services['lbrule']["publicport"],
                                        self.vm_1.username,
                                        self.vm_1.password
                                        )
    
            # If Round Robin Algorithm is chosen,
            # each ssh command should alternate between VMs
            hostnames = [ssh_1.execute("hostname")[0]]
            
        except Exception as e:
            self.fail("%s: SSH failed for VM with IP Address: %s" % 
                                        (e, src_nat_ip_addr.ipaddress))

        time.sleep(self.services["lb_switch_wait"])
        
        try:
            self.debug("SSHing into IP address: %s after adding VMs (ID: %s , %s)" % 
                                            (
                                             src_nat_ip_addr.ipaddress,
                                             self.vm_1.id,
                                             self.vm_2.id
                                             ))
            
            ssh_2 = remoteSSHClient.remoteSSHClient(
                                        src_nat_ip_addr.ipaddress,
                                        self.services['lbrule']["publicport"],
                                        self.vm_1.username,
                                        self.vm_1.password
                                        )
            hostnames.append(ssh_2.execute("hostname")[0])

        except Exception as e:
            self.fail("%s: SSH failed for VM with IP Address: %s" % 
                                        (e, src_nat_ip_addr.ipaddress))

        self.assertIn(
                      self.vm_1.name,
                      hostnames,
                      "Check if ssh succeeded for server1"
                    )
        self.assertIn(
                      self.vm_2.name,
                      hostnames,
                      "Check if ssh succeeded for server2"
                      )

        #SSH should pass till there is a last VM associated with LB rule
        lb_rule.remove(self.apiclient, [self.vm_2])
        try:
            self.debug("SSHing into IP address: %s after removing VM (ID: %s)" % 
                                            (
                                             src_nat_ip_addr.ipaddress,
                                             self.vm_2.id
                                             ))
            
            ssh_1 = remoteSSHClient.remoteSSHClient(
                                        src_nat_ip_addr.ipaddress,
                                        self.services['lbrule']["publicport"],
                                        self.vm_1.username,
                                        self.vm_1.password
                                        )
    
            hostnames.append(ssh_1.execute("hostname")[0])
        
        except Exception as e:
            self.fail("%s: SSH failed for VM with IP Address: %s" % 
                                        (e, src_nat_ip_addr.ipaddress))

        self.assertIn(
                      self.vm_1.name,
                      hostnames,
                      "Check if ssh succeeded for server1"
                      )

        lb_rule.remove(self.apiclient, [self.vm_1])
        
        with self.assertRaises(Exception):
            self.debug("Removed all VMs, trying to SSH")
            ssh_1 = remoteSSHClient.remoteSSHClient(
                                        src_nat_ip_addr.ipaddress,
                                        self.services['lbrule']["publicport"],
                                        self.vm_1.username,
                                        self.vm_1.password
                                        )
            ssh_1.execute("hostname")[0]
        return
    
    def test_02_create_lb_rule_non_nat(self):
        """Test to create Load balancing rule with source NAT"""

        # Validate the Following:
        #1. listLoadBalancerRules should return the added rule
        #2. attempt to ssh twice on the load balanced IP
        #3. verify using the hostname of the VM that
        #   round robin is indeed happening as expected
        
        # Check if VM is in Running state before creating LB rule
        vm_response = VirtualMachine.list(
                                          self.apiclient,
                                          account=self.account.account.name,
                                          domainid=self.account.account.domainid
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
        for vm in vm_response:
            self.assertEqual(
                            vm.state,
                            'Running',
                            "VM state should be Running before creating a NAT rule."
                        )
        
        #Create Load Balancer rule and assign VMs to rule
        lb_rule = LoadBalancerRule.create(
                                          self.apiclient,
                                          self.services["lbrule"],
                                          self.non_src_nat_ip.ipaddress.id,
                                          accountid=self.account.account.name
                                          )
        self.cleanup.append(lb_rule)

        lb_rule.assign(self.apiclient, [self.vm_1, self.vm_2])

        lb_rules = list_lb_rules(
                                 self.apiclient,
                                 id=lb_rule.id
                                 )
        
        self.assertEqual(
                            isinstance(lb_rules, list),
                            True,
                            "Check list response returns a valid list"
                        )
        #verify listLoadBalancerRules lists the added load balancing rule
        self.assertNotEqual(
                            len(lb_rules),
                            0,
                            "Check Load Balancer Rule in its List"
                        )
        self.assertEqual(
                            lb_rules[0].id,
                            lb_rule.id,
                            "Check List Load Balancer Rules returns valid Rule"
                        )
        # listLoadBalancerRuleInstances should list
        # all instances associated with that LB rule
        lb_instance_rules = list_lb_instances(
                                              self.apiclient,
                                              id=lb_rule.id
                                              )
        self.assertEqual(
                            isinstance(lb_instance_rules, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            len(lb_instance_rules),
                            0,
                            "Check Load Balancer instances Rule in its List"
                        )

        self.assertIn(
                lb_instance_rules[0].id,
                [self.vm_1.id, self.vm_2.id],
                "Check List Load Balancer instances Rules returns valid VM ID"
                            )

        self.assertIn(
                lb_instance_rules[1].id,
                [self.vm_1.id, self.vm_2.id],
                "Check List Load Balancer instances Rules returns valid VM ID"
                )
        try:
            self.debug("SSHing into IP address: %s after adding VMs (ID: %s , %s)" % 
                                            (
                                             self.non_src_nat_ip.ipaddress.ipaddress,
                                             self.vm_1.id,
                                             self.vm_2.id
                                             ))
            ssh_1 = remoteSSHClient.remoteSSHClient(
                                    self.non_src_nat_ip.ipaddress.ipaddress,
                                    self.services['lbrule']["publicport"],
                                    self.vm_1.username,
                                    self.vm_1.password
                                    )

            # If Round Robin Algorithm is chosen,
            # each ssh command should alternate between VMs
            hostnames = [ssh_1.execute("hostname")[0]]
            
            time.sleep(self.services["lb_switch_wait"])
        
            self.debug("SSHing again into IP address: %s with VMs (ID: %s , %s) added to LB rule" % 
                                            (
                                             self.non_src_nat_ip.ipaddress.ipaddress,
                                             self.vm_1.id,
                                             self.vm_2.id
                                             ))
            ssh_2 = remoteSSHClient.remoteSSHClient(
                                    self.non_src_nat_ip.ipaddress.ipaddress,
                                    self.services['lbrule']["publicport"],
                                    self.vm_1.username,
                                    self.vm_1.password
                                    )
            
            hostnames.append(ssh_2.execute("hostname")[0])
            
            self.assertIn(
                      self.vm_1.name,
                      hostnames,
                      "Check if ssh succeeded for server1"
                      )
            self.assertIn(
                      self.vm_2.name,
                      hostnames,
                      "Check if ssh succeeded for server2"
                      )

            #SSH should pass till there is a last VM associated with LB rule
            lb_rule.remove(self.apiclient, [self.vm_2])
        
            self.debug("SSHing into IP address: %s after removing VM (ID: %s) from LB rule" % 
                                            (
                                             self.non_src_nat_ip.ipaddress.ipaddress,
                                             self.vm_2.id
                                             ))
            ssh_1 = remoteSSHClient.remoteSSHClient(
                                        self.non_src_nat_ip.ipaddress.ipaddress,
                                        self.services['lbrule']["publicport"],
                                        self.vm_1.username,
                                        self.vm_1.password
                                        )
    
            hostnames.append(ssh_1.execute("hostname")[0])
            
        except Exception as e:
            self.fail("%s: SSH failed for VM with IP Address: %s" % 
                                        (e, self.non_src_nat_ip.ipaddress.ipaddress))

        self.assertIn(
                      self.vm_1.name,
                      hostnames,
                      "Check if ssh succeeded for server1"
                      )

        lb_rule.remove(self.apiclient, [self.vm_1])
        with self.assertRaises(Exception):
            self.fail("SSHing into IP address: %s after removing VM (ID: %s) from LB rule" % 
                                            (
                                             self.non_src_nat_ip.ipaddress.ipaddress,
                                             self.vm_1.id
                                             ))
            ssh_1 = remoteSSHClient.remoteSSHClient(
                                        self.non_src_nat_ip.ipaddress.ipaddress,
                                        self.services['lbrule']["publicport"],
                                        self.vm_1.username,
                                        self.vm_1.password
                                        )
            ssh_1.execute("hostname")[0]
        return


class TestRebootRouter(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.services = Services().services

        # Get Zone, Domain and templates
        self.zone = get_zone(self.apiclient, self.services)
        template = get_template(
                            self.apiclient,
                            self.zone.id,
                            self.services["ostypeid"]
                            )
        self.services["server"]["zoneid"] = self.zone.id

        #Create an account, network, VM and IP addresses
        self.account = Account.create(
                                      self.apiclient,
                                      self.services["account"],
                                      admin=True
                                      )
        self.service_offering = ServiceOffering.create(
                                            self.apiclient,
                                            self.services["service_offering"]
                                            )
        self.vm_1 = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["server"],
                                    templateid=template.id,
                                    accountid=self.account.account.name,
                                    serviceofferingid=self.service_offering.id
                                    )
        
        src_nat_ip_addrs = list_publicIP(
                                    self.apiclient,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
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

        lb_rule = LoadBalancerRule.create(
                                            self.apiclient,
                                            self.services["lbrule"],
                                            src_nat_ip_addr.id,
                                            self.account.account.name
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
                        self.service_offering,
                        self.nat_rule,
                        self.account,
                        ]
        return

    def test_reboot_router(self):
        """Test for reboot router"""

        #Validate the Following
        #1. Post restart PF and LB rules should still function
        #2. verify if the ssh into the virtual machine
        #   still works through the sourceNAT Ip

        #Retrieve router for the user account
        routers = list_routers(
                                self.apiclient,
                                account=self.account.account.name,
                                domainid=self.account.account.domainid
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
            
            remoteSSHClient.remoteSSHClient(
                                    self.nat_rule.ipaddress,
                                    self.services["natrule"]["publicport"],
                                    self.vm_1.username,
                                    self.vm_1.password
                                    )
        except Exception as e:
            self.fail(
                      "SSH Access failed for %s: %s" % \
                      (self.vm_1.ipaddress, e)
                      )
        return

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return


class TestAssignRemoveLB(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.services = Services().services
        # Get Zone, Domain and templates
        self.zone = get_zone(self.apiclient, self.services)
        template = get_template(
                            self.apiclient,
                            self.zone.id,
                            self.services["ostypeid"]
                            )
        self.services["server"]["zoneid"] = self.zone.id

        #Create VMs, accounts
        self.account = Account.create(
                                      self.apiclient,
                                      self.services["account"],
                                      admin=True
                                      )
        self.service_offering = ServiceOffering.create(
                                            self.apiclient,
                                            self.services["service_offering"]
                                        )

        self.vm_1 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["server"],
                                  templateid=template.id,
                                  accountid=self.account.account.name,
                                  serviceofferingid=self.service_offering.id
                                  )

        self.vm_2 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=template.id,
                                accountid=self.account.account.name,
                                serviceofferingid=self.service_offering.id
                              )

        self.vm_3 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=template.id,
                                accountid=self.account.account.name,
                                serviceofferingid=self.service_offering.id
                              )

        self.cleanup = [
                        self.account,
                        self.service_offering
                        ]
        return

    def test_assign_and_removal_elb(self):
        """Test for assign & removing load balancing rule"""

        # Validate:
        #1. Verify list API - listLoadBalancerRules lists
        #   all the rules with the relevant ports
        #2. listLoadBalancerInstances will list
        #   the instances associated with the corresponding rule.
        #3. verify ssh attempts should pass as long as there
        #   is at least one instance associated with the rule

        src_nat_ip_addrs = list_publicIP(
                                    self.apiclient,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                  )
        self.assertEqual(
                            isinstance(src_nat_ip_addrs, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.non_src_nat_ip = src_nat_ip_addrs[0]
        
        # Check if VM is in Running state before creating LB rule
        vm_response = VirtualMachine.list(
                                          self.apiclient,
                                          account=self.account.account.name,
                                          domainid=self.account.account.domainid
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
        for vm in vm_response:
            self.assertEqual(
                            vm.state,
                            'Running',
                            "VM state should be Running before creating a NAT rule."
                        )
        
        lb_rule = LoadBalancerRule.create(
                                self.apiclient,
                                self.services["lbrule"],
                                self.non_src_nat_ip.id,
                                self.account.account.name
                              )
        lb_rule.assign(self.apiclient, [self.vm_1, self.vm_2])
        
        try:
            self.debug("SSHing into IP address: %s with VMs (ID: %s , %s) added to LB rule" % 
                                            (
                                             self.non_src_nat_ip.ipaddress,
                                             self.vm_1.id,
                                             self.vm_2.id
                                             ))
            #Create SSH client for each VM
            ssh_1 = remoteSSHClient.remoteSSHClient(
                                        self.non_src_nat_ip.ipaddress,
                                        self.services["lbrule"]["publicport"],
                                        self.vm_1.username,
                                        self.vm_1.password
                                        )
        except Exception as e:
            self.fail("SSH failed for VM with IP: %s" % 
                                    self.non_src_nat_ip.ipaddress)
        
        try:
            self.debug("SSHing again into IP address: %s with VMs (ID: %s , %s) added to LB rule" % 
                                            (
                                             self.non_src_nat_ip.ipaddress,
                                             self.vm_1.id,
                                             self.vm_2.id
                                             ))
            ssh_2 = remoteSSHClient.remoteSSHClient(
                                        self.non_src_nat_ip.ipaddress,
                                        self.services["lbrule"]["publicport"],
                                        self.vm_2.username,
                                        self.vm_2.password
                                        )
            
            # If Round Robin Algorithm is chosen,
            # each ssh command should alternate between VMs
            res_1 = ssh_1.execute("hostname")[0]
            self.debug(res_1)
        
            time.sleep(self.services["lb_switch_wait"])
        
            res_2 = ssh_2.execute("hostname")[0]
            self.debug(res_2)

        except Exception as e:
            self.fail("SSH failed for VM with IP: %s" % 
                                    self.non_src_nat_ip.ipaddress)

        self.assertIn(
                      self.vm_1.name,
                      res_1,
                      "Check if ssh succeeded for server1"
                      )
        self.assertIn(
                      self.vm_2.name,
                      res_2,
                      "Check if ssh succeeded for server2"
                      )

        #Removing VM and assigning another VM to LB rule
        lb_rule.remove(self.apiclient, [self.vm_2])
        
        try:
            self.debug("SSHing again into IP address: %s with VM (ID: %s) added to LB rule" % 
                                            (
                                             self.non_src_nat_ip.ipaddress,
                                             self.vm_1.id,
                                             ))
            # Again make a SSH connection, as previous is not used after LB remove
            ssh_1 = remoteSSHClient.remoteSSHClient(
                                        self.non_src_nat_ip.ipaddress,
                                        self.services["lbrule"]["publicport"],
                                        self.vm_1.username,
                                        self.vm_1.password
                                        )    
            res_1 = ssh_1.execute("hostname")[0]
            self.debug(res_1)

        except Exception as e:
            self.fail("SSH failed for VM with IP: %s" % 
                                    self.non_src_nat_ip.ipaddress)
            
        self.assertIn(
                      self.vm_1.name,
                      res_1,
                      "Check if ssh succeeded for server1"
                      )

        lb_rule.assign(self.apiclient, [self.vm_3])
        
        try:
            ssh_1 = remoteSSHClient.remoteSSHClient(
                                        self.non_src_nat_ip.ipaddress,
                                        self.services["lbrule"]["publicport"],
                                        self.vm_1.username,
                                        self.vm_1.password
                                        )
            ssh_3 = remoteSSHClient.remoteSSHClient(
                                        self.non_src_nat_ip.ipaddress,
                                        self.services["lbrule"]["publicport"],
                                        self.vm_3.username,
                                        self.vm_3.password
                                        )
            
            res_1 = ssh_1.execute("hostname")[0]
            self.debug(res_1)
        
            time.sleep(self.services["lb_switch_wait"])
        
            res_3 = ssh_3.execute("hostname")[0]
            self.debug(res_3)

        except Exception as e:
            self.fail("SSH failed for VM with IP: %s" % 
                                    self.non_src_nat_ip.ipaddress)
            
        self.assertIn(
                      self.vm_1.name,
                      res_1,
                      "Check if ssh succeeded for server1"
                      )
        self.assertIn(
                      self.vm_3.name,
                      res_3,
                      "Check if ssh succeeded for server3"
                      )
        return

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return


class TestReleaseIP(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.services = Services().services

        # Get Zone, Domain and templates
        self.zone = get_zone(self.apiclient, self.services)
        template = get_template(
                            self.apiclient,
                            self.zone.id,
                            self.services["ostypeid"]
                            )
        self.services["server"]["zoneid"] = self.zone.id

        #Create an account, network, VM, Port forwarding rule, LB rules
        self.account = Account.create(
                                      self.apiclient,
                                      self.services["account"],
                                      admin=True
                                      )

        self.service_offering = ServiceOffering.create(
                                           self.apiclient,
                                           self.services["service_offering"]
                                         )

        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["server"],
                                    templateid=template.id,
                                    accountid=self.account.account.name,
                                    serviceofferingid=self.service_offering.id
                                    )

        self.ip_address = PublicIPAddress.create(
                                            self.apiclient,
                                            self.account.account.name,
                                            self.zone.id,
                                            self.account.account.domainid
                                            )

        ip_addrs = list_publicIP(
                                    self.apiclient,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                  )
        try:
            self.ip_addr = ip_addrs[0]
        except Exception as e:
            raise Exception("Failed: During acquiring source NAT for account: %s" %
                                self.account.account.name)
            
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
                                        accountid=self.account.account.name
                                        )
        self.cleanup = [
                        self.virtual_machine,
                        self.account
                        ]
        return

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)

    def test_releaseIP(self):
        """Test for Associate/Disassociate public IP address"""
        
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
        list_nat_rule = list_nat_rules(
                                        self.apiclient,
                                        id=self.nat_rule.id
                                        )
        self.debug("List NAT Rule response" + str(list_nat_rule))
        self.assertEqual(
                list_nat_rule,
                None,
                "Check if PF rules are no longer available for IP address"
            )

        # listLoadBalancerRules should not list
        # associated rules with Public IP address
        list_lb_rule = list_lb_rules(
                                     self.apiclient,
                                     id=self.lb_rule.id
                                     )
        self.debug("List LB Rule response" + str(list_lb_rule))
        
        self.assertEqual(
                list_lb_rule,
                None,
                "Check if LB rules for IP Address are no longer available"
            )

        # SSH Attempt though public IP should fail
        with self.assertRaises(Exception):
            ssh_2 = remoteSSHClient.remoteSSHClient(
                                    self.ip_addr.ipaddress,
                                    self.services["natrule"]["publicport"],
                                    self.virtual_machine.username,
                                    self.virtual_machine.password
                                    )
        return


class TestDeleteAccount(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.services = Services().services

        # Get Zone, Domain and templates
        self.zone = get_zone(self.apiclient, self.services)
        template = get_template(
                            self.apiclient,
                            self.zone.id,
                            self.services["ostypeid"]
                            )
        self.services["server"]["zoneid"] = self.zone.id

        #Create an account, network, VM and IP addresses
        self.account = Account.create(
                                self.apiclient,
                                self.services["account"],
                                admin=True
                                )
        self.service_offering = ServiceOffering.create(
                                    self.apiclient,
                                    self.services["service_offering"]
                                    )
        self.vm_1 = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["server"],
                                    templateid=template.id,
                                    accountid=self.account.account.name,
                                    serviceofferingid=self.service_offering.id
                                    )

        src_nat_ip_addrs = list_publicIP(
                                    self.apiclient,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
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
                                            self.account.account.name
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
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                    )
            self.assertEqual(
                     list_lb_reponse,
                     None,
                    "Check load balancing rule is properly deleted."
                   )
        except Exception as e:
            
            raise Exception(
                "Exception raised while fetching LB rules for account: %s" %
                                                    self.account.account.name)
        # ListPortForwardingRules should not
        # list associated rules with deleted account
        try:
            list_nat_reponse= list_nat_rules(
                                    self.apiclient,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                        )
            self.assertEqual(
                             list_nat_reponse,
                             None,
                             "Check load balancing rule is properly deleted."
                   )
        except Exception as e:
            
            raise Exception(
                "Exception raised while fetching NAT rules for account: %s" %
                                                    self.account.account.name)
        #Retrieve router for the user account
        try:
            routers = list_routers(
                          self.apiclient,
                          account=self.account.account.name,
                          domainid=self.account.account.domainid
                        )
            self.assertEqual(
                             routers,
                             None,
                             "Check routers are properly deleted."
                   )
        except Exception as e:
            
            raise Exception(
                "Exception raised while fetching routers for account: %s" %
                                                    self.account.account.name)
        return

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return