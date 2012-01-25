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
from utils import *
from base import *
#Import System modules
import time


class Services:
    """Test Network Services
    """

    def __init__(self):
        self.services = {
                            "ostypeid": 12,
                            # Cent OS 5.3 (64 bit)
                            "network": {
                                  "name": "Test Network",
                                  "displaytext": "Test Network",
                                  "networkoffering": 6,
                                },
                            "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 100,
                                    # in MHz
                                    "memory": 64,
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
                                        "domainid": 1,
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
                                    "privateport": 80,
                                    "publicport": 80,
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
        cls.zone = get_zone(cls.api_client)

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
        cmd = listPublicIpAddresses.listPublicIpAddressesCmd()
        cmd.id = ip_address.ipaddress.id

        list_pub_ip_addr_resp = self.apiclient.listPublicIpAddresses(cmd)

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

        cmd = listPublicIpAddresses.listPublicIpAddressesCmd()
        cmd.id = ip_address.ipaddress.id
        list_pub_ip_addr_resp = self.apiclient.listPublicIpAddresses(cmd)

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
        cmd = listPublicIpAddresses.listPublicIpAddressesCmd()
        cmd.id = ip_address.ipaddress.id

        #listPublicIpAddresses should return newly created public IP
        list_pub_ip_addr_resp = self.apiclient.listPublicIpAddresses(cmd)

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

        cmd = listPublicIpAddresses.listPublicIpAddressesCmd()
        cmd.id = ip_address.ipaddress.id
        list_pub_ip_addr_resp = self.apiclient.listPublicIpAddresses(cmd)

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
        cls.zone = get_zone(cls.api_client)
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

        cmd = listPublicIpAddresses.listPublicIpAddressesCmd()
        cmd.account = self.account.account.name
        cmd.domainid = self.account.account.domainid
        src_nat_ip_addr = self.apiclient.listPublicIpAddresses(cmd)[0]

        #Create NAT rule
        nat_rule = NATRule.create(
                        self.apiclient,
                        self.virtual_machine,
                        self.services["natrule"],
                        src_nat_ip_addr.id
                        )

        cmd = listPortForwardingRules.listPortForwardingRulesCmd()
        cmd.id = nat_rule.id
        list_nat_rule_response = self.apiclient.listPortForwardingRules(cmd)

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
            self.virtual_machine.get_ssh_client(src_nat_ip_addr.ipaddress)
        except Exception as e:
            self.fail(
                      "SSH Access failed for %s: %s" % \
                      (self.virtual_machine.ipaddress, e)
                      )

        nat_rule.delete(self.apiclient)

        cmd = listPortForwardingRules.listPortForwardingRulesCmd()
        cmd.id = nat_rule.id
        list_nat_rule_response = self.apiclient.listPortForwardingRules(cmd)

        self.assertEqual(
                            list_nat_rule_response,
                            None,
                            "Check Port Forwarding Rule is deleted"
                            )
        # Check if the Public SSH port is inaccessible
        with self.assertRaises(Exception):
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

        cmd = listPortForwardingRules.listPortForwardingRulesCmd()
        cmd.id = nat_rule.id
        list_nat_rule_response = self.apiclient.listPortForwardingRules(cmd)

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
            self.virtual_machine.get_ssh_client(ip_address.ipaddress.ipaddress)
        except Exception as e:
            self.fail(
                      "SSH Access failed for %s: %s" % \
                      (self.virtual_machine.ipaddress.ipaddress, e)
                      )

        nat_rule.delete(self.apiclient)

        cmd = listPortForwardingRules.listPortForwardingRulesCmd()
        cmd.id = nat_rule.id
        list_nat_rule_response = self.apiclient.listPortForwardingRules(cmd)

        self.assertEqual(
                            len(list_nat_rule_response),
                            None,
                            "Check Port Forwarding Rule is deleted"
                            )
        # Check if the Public SSH port is inaccessible
        with self.assertRaises(Exception):
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
        cls.zone = get_zone(cls.api_client)
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
                        cls.vm_1,
                        cls.vm_2,
                        cls.non_src_nat_ip,
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

        cmd = listPublicIpAddresses.listPublicIpAddressesCmd()
        cmd.account = self.account.account.name
        cmd.domainid = self.account.account.domainid
        src_nat_ip_addr = self.apiclient.listPublicIpAddresses(cmd)[0]

        #Create Load Balancer rule and assign VMs to rule
        lb_rule = LoadBalancerRule.create(
                                          self.apiclient,
                                          self.services["lbrule"],
                                          src_nat_ip_addr.id,
                                          accountid=self.account.account.name
                                          )
        self.cleanup.append(lb_rule)

        lb_rule.assign(self.apiclient, [self.vm_1, self.vm_2])
        cmd = listLoadBalancerRules.listLoadBalancerRulesCmd()
        cmd.id = lb_rule.id
        lb_rules = self.apiclient.listLoadBalancerRules(cmd)

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
        cmd = listLoadBalancerRuleInstances.listLoadBalancerRuleInstancesCmd()
        cmd.id = lb_rule.id
        lb_instance_rules = self.apiclient.listLoadBalancerRuleInstances(cmd)

        self.assertNotEqual(
                            len(lb_instance_rules),
                            0,
                            "Check Load Balancer instances Rule in its List"
                        )

        self.assertEqual(
                lb_instance_rules[0].id,
                self.vm_2.id,
                "Check List Load Balancer instances Rules returns valid VM ID"
            )

        self.assertEqual(
            lb_instance_rules[1].id,
            self.vm_1.id,
            "Check List Load Balancer instances Rules returns valid VM ID"
            )

        ssh_1 = remoteSSHClient.remoteSSHClient(
                                        src_nat_ip_addr.ipaddress,
                                        self.services['natrule']["publicport"],
                                        self.vm_1.username,
                                        self.vm_1.password
                                        )

        # If Round Robin Algorithm is chosen,
        # each ssh command should alternate between VMs
        hostnames = [ssh_1.execute("hostname")[0]]
        time.sleep(20)
        ssh_2 = remoteSSHClient.remoteSSHClient(
                                        src_nat_ip_addr.ipaddress,
                                        self.services['natrule']["publicport"],
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
        hostnames.append(ssh_1.execute("hostname")[0])
        self.assertIn(
                      self.vm_1.name,
                      hostnames,
                      "Check if ssh succeeded for server1"
                      )

        lb_rule.remove(self.apiclient, [self.vm_1])
        with self.assertRaises(Exception):
            ssh_1.execute("hostname")[0]
        return

    def test_02_create_lb_rule_non_nat(self):
        """Test to create Load balancing rule with source NAT"""

        # Validate the Following:
        #1. listLoadBalancerRules should return the added rule
        #2. attempt to ssh twice on the load balanced IP
        #3. verify using the hostname of the VM that
        #   round robin is indeed happening as expected

        #Create Load Balancer rule and assign VMs to rule
        lb_rule = LoadBalancerRule.create(
                                          self.apiclient,
                                          self.services["lbrule"],
                                          self.non_src_nat_ip.ipaddress.id,
                                          accountid=self.account.account.name
                                          )
        self.cleanup.append(lb_rule)

        lb_rule.assign(self.apiclient, [self.vm_1, self.vm_2])
        cmd = listLoadBalancerRules.listLoadBalancerRulesCmd()
        cmd.id = lb_rule.id
        lb_rules = self.apiclient.listLoadBalancerRules(cmd)

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
        cmd = listLoadBalancerRuleInstances.listLoadBalancerRuleInstancesCmd()
        cmd.id = lb_rule.id
        lb_instance_rules = self.apiclient.listLoadBalancerRuleInstances(cmd)

        self.assertNotEqual(
                            len(lb_instance_rules),
                            0,
                            "Check Load Balancer instances Rule in its List"
                        )

        self.assertEqual(
                lb_instance_rules[0].id,
                self.vm_2.id,
                "Check List Load Balancer instances Rules returns valid VM ID"
                            )

        self.assertEqual(
                lb_instance_rules[1].id,
                self.vm_1.id,
                "Check List Load Balancer instances Rules returns valid VM ID"
                )

        ssh_1 = remoteSSHClient.remoteSSHClient(
                                    self.non_src_nat_ip.ipaddress.ipaddress,
                                    self.services['natrule']["publicport"],
                                    self.vm_1.username,
                                    self.vm_1.password
                                    )

        # If Round Robin Algorithm is chosen,
        # each ssh command should alternate between VMs
        hostnames = [ssh_1.execute("hostname")[0]]
        time.sleep(20)
        ssh_2 = remoteSSHClient.remoteSSHClient(
                                    self.non_src_nat_ip.ipaddress.ipaddress,
                                    self.services['natrule']["publicport"],
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
        hostnames.append(ssh_1.execute("hostname")[0])
        self.assertIn(
                      self.vm_1.name,
                      hostnames,
                      "Check if ssh succeeded for server1"
                      )

        lb_rule.remove(self.apiclient, [self.vm_1])
        with self.assertRaises(Exception):
            ssh_1.execute("hostname")[0]
        return


class TestRebootRouter(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.services = Services().services

        # Get Zone, Domain and templates
        self.zone = get_zone(self.apiclient)
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

        cmd = listPublicIpAddresses.listPublicIpAddressesCmd()
        cmd.account = self.account.account.name
        cmd.domainid = self.account.account.domainid
        src_nat_ip_addr = self.apiclient.listPublicIpAddresses(cmd)[0]

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
        cmd = listRouters.listRoutersCmd()
        cmd.account = self.account.account.name
        cmd.domainid = self.account.account.domainid
        routers = self.apiclient.listRouters(cmd)
        router = routers[0]

        cmd = rebootRouter.rebootRouterCmd()
        cmd.id = router.id
        self.apiclient.rebootRouter(cmd)
        #Sleep to ensure router is rebooted properly
        time.sleep(60)

        #we should be able to SSH after successful reboot
        try:
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
        self.zone = get_zone(self.apiclient)
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
                        self.vm_1,
                        self.vm_2,
                        self.vm_3,
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

        cmd = listPublicIpAddresses.listPublicIpAddressesCmd()
        cmd.account = self.account.account.name
        cmd.domainid = self.account.account.domainid
        self.non_src_nat_ip = self.apiclient.listPublicIpAddresses(cmd)[0]

        lb_rule = LoadBalancerRule.create(
                                self.apiclient,
                                self.services["lbrule"],
                                self.non_src_nat_ip.id,
                                self.account.account.name
                              )
        self.cleanup.append(lb_rule)
        lb_rule.assign(self.apiclient, [self.vm_1, self.vm_2])
        #Create SSH client for each VM
        ssh_1 = remoteSSHClient.remoteSSHClient(
                                        self.non_src_nat_ip.ipaddress,
                                        self.services["natrule"]["publicport"],
                                        self.vm_1.username,
                                        self.vm_1.password
                                        )

        ssh_2 = remoteSSHClient.remoteSSHClient(
                                        self.non_src_nat_ip.ipaddress,
                                        self.services["natrule"]["publicport"],
                                        self.vm_2.username,
                                        self.vm_2.password
                                        )
        ssh_3 = remoteSSHClient.remoteSSHClient(
                                        self.non_src_nat_ip.ipaddress,
                                        self.services["natrule"]["publicport"],
                                        self.vm_3.username,
                                        self.vm_3.password
                                        )
        # If Round Robin Algorithm is chosen,
        # each ssh command should alternate between VMs
        res_1 = ssh_1.execute("hostname")[0]
        time.sleep(20)
        res_2 = ssh_2.execute("hostname")[0]

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

        res_1 = ssh_1.execute("hostname")[0]

        self.assertIn(
                      self.vm_1.name,
                      res_1,
                      "Check if ssh succeeded for server1"
                      )

        lb_rule.assign(self.apiclient, [self.vm_3])

        res_1 = ssh_1.execute("hostname")[0]
        time.sleep(20)
        res_3 = ssh_3.execute("hostname")[0]

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

    def teardown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return


class TestReleaseIP(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.services = Services().services

        # Get Zone, Domain and templates
        self.zone = get_zone(self.apiclient)
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

        cmd = listPublicIpAddresses.listPublicIpAddressesCmd()
        cmd.account = self.account.account.name
        cmd.domainid = self.account.account.domainid
        self.ip_addr = self.apiclient.listPublicIpAddresses(cmd)[0]

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

    def teardown(self):
        cleanup_resources(self.apiclient, self.cleanup)

    def test_releaseIP(self):
        """Test for Associate/Disassociate public IP address"""

        self.ip_address.delete(self.apiclient)

        # ListPublicIpAddresses should not list deleted Public IP address
        cmd = listPublicIpAddresses.listPublicIpAddressesCmd()
        cmd.id = self.ip_addr.id
        list_pub_ip_addr_resp = self.apiclient.listPublicIpAddresses(cmd)

        self.assertEqual(
                     list_pub_ip_addr_resp,
                     None,
                    "Check if disassociated IP Address is no longer available"
                   )

        # ListPortForwardingRules should not list
        # associated rules with Public IP address
        cmd = listPortForwardingRules.listPortForwardingRulesCmd()
        cmd.id = self.nat_rule.id
        list_nat_rules = self.apiclient.listPortForwardingRules(cmd)

        self.assertEqual(
                list_nat_rules,
                None,
                "Check if PF rules are no longer available for IP address"
            )

        # listLoadBalancerRules should not list
        # associated rules with Public IP address
        cmd = listLoadBalancerRules.listLoadBalancerRulesCmd()
        cmd.id = self.lb_rule.id
        list_lb_rules = self.apiclient.listLoadBalancerRules(cmd)

        self.assertEqual(
            list_lb_rules,
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
        self.zone = get_zone(self.apiclient)
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

        cmd = listPublicIpAddresses.listPublicIpAddressesCmd()
        cmd.account = self.account.account.name
        cmd.domainid = self.account.account.domainid
        src_nat_ip_addr = self.apiclient.listPublicIpAddresses(cmd)[0]

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
        # Sleep to ensure that all resources are deleted
        time.sleep(120)

        # ListLoadBalancerRules should not list
        # associated rules with deleted account
        cmd = listLoadBalancerRules.listLoadBalancerRulesCmd()
        cmd.account = self.account.account.name
        cmd.domainid = self.account.account.domainid
        # Unable to find account testuser1 in domain 1 : Exception
        with self.assertRaises(Exception):
            self.apiclient.listLoadBalancerRules(cmd)

        # ListPortForwardingRules should not
        # list associated rules with deleted account
        cmd = listPortForwardingRules.listPortForwardingRulesCmd()
        cmd.account = self.account.account.name
        cmd.domainid = self.account.account.domainid

        with self.assertRaises(Exception):
            self.apiclient.listPortForwardingRules(cmd)

        #Retrieve router for the user account
        cmd = listRouters.listRoutersCmd()
        cmd.account = self.account.account.name
        cmd.domainid = self.account.account.domainid
        with self.assertRaises(Exception):
            routers = self.apiclient.listRouters(cmd)
        return

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return
