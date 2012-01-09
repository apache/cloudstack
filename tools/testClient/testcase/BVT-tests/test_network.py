# -*- encoding: utf-8 -*-
#
# Copyright (c) 2011 Citrix.  All rights reserved.
#

""" BVT tests for Virtual Machine Life Cycle
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from settings import *
import remoteSSHClient
from utils import *
from base import *
#Import System modules
import time


services = TEST_NETWORK_SERVICES


class TestPublicIP(cloudstackTestCase):
    """Test Associate/Disassociate Public IP Addresses
    """
    def setUp(self):
        self.apiclient = self.testClient.getApiClient()

    def test_public_ip_admin_account(self):
        """Test Associate/Disassociate IP address for Admin Account
        """
        # Validate the following:
        # 1. listPubliIpAddresses API returns the list of acquired addresses
        # 2. the returned list should contain our acquired IP address


        ip_address = PublicIPAddress.create(
                                            self.apiclient,
                                            services["admin_account"],
                                            services["zoneid"],
                                            services["domainid"]
                                            )
        cmd = listPublicIpAddresses.listPublicIpAddressesCmd()
        cmd.id = ip_address.ipaddress.id
        list_pub_ip_addr_resp = self.apiclient.listPublicIpAddresses(cmd)

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
        #1. listPublicIpAddresses API should no more return the released address
        cmd = listPublicIpAddresses.listPublicIpAddressesCmd()
        cmd.id = ip_address.ipaddress.id
        list_pub_ip_addr_resp = self.apiclient.listPublicIpAddresses(cmd)

        self.assertEqual(
                            list_pub_ip_addr_resp,
                            None,
                            "Check if disassociated IP Address is no longer available"
                            )



    def test_public_ip_user_account(self):
        """Test Associate/Disassociate IP address for Non-Admin User Account
        """

        # Validate the following:
        # 1. listPubliIpAddresses API returns the list of acquired addresses
        # 2. the returned list should contain our acquired IP address
        ip_address = PublicIPAddress.create(
                                            self.apiclient,
                                            services["user_account"],
                                            services["zoneid"],
                                            services["domainid"]
                                            )
        cmd = listPublicIpAddresses.listPublicIpAddressesCmd()
        cmd.id = ip_address.ipaddress.id
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


        # Validate the following:
        #1. listPublicIpAddresses API should no more return the released address
        cmd = listPublicIpAddresses.listPublicIpAddressesCmd()
        cmd.id = ip_address.ipaddress.id
        list_pub_ip_addr_resp = self.apiclient.listPublicIpAddresses(cmd)

        self.assertEqual(
                            list_pub_ip_addr_resp,
                            None,
                            "Check if disassociated IP Address is no longer available"
                            )


class TestPortForwarding(cloudstackTestCase):
    """Test Port Forwarding Rules for Source and Non-Source IP Addresses
    """
    @classmethod
    def setUpClass(cls):

        cls.api_client = fetch_api_client()
        #Create an account, network, VM and IP addresses
        cls.account = Account.create(cls.api_client, services["account"], admin=True)
        cls.virtual_machine = VirtualMachine.create(
                                                    cls.api_client,
                                                    services["server"],
                                                    accountid=cls.account.account.name,
                                                    networkids=[str(cls.account.network.id)]
                                                    )

        cls._cleanup = [cls.virtual_machine, cls.account]

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()

    @classmethod
    def tearDownClass(self):
        cleanup_resources(cls.api_client, cls._cleanup)

    def tearDown(self):
        cleanup_resources(self.cleanup)

    def test_01_port_fwd_on_src_nat(self):
        """Port Forwarding Tests for Source NAT IP Addresses
        """
        src_nat_ip_addr = self.account.public_ip.ipaddress


        #Validate the following:
        #1. listPortForwarding rules API should return the added PF rule
        #2. attempt to do an ssh into the  user VM through the sourceNAT

        nat_rule = NATRule.create(self.apiclient, self.virtual_machine, services["natrule"], src_nat_ip_addr.id)
        time.sleep(60)

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
            self.virtual_machine.get_ssh_client(src_nat_ip_addr.ipaddress)
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %(self.virtual_machine.ipaddress, e))

        nat_rule.delete(self.apiclient)
        time.sleep(60)

        #Validate the following:
        #1. listPortForwardingRules should not return the deleted rule anymore
        #2. attempt to do ssh should now fail

        cmd = listPortForwardingRules.listPortForwardingRulesCmd()
        cmd.id = nat_rule.id
        list_nat_rule_response = self.apiclient.listPortForwardingRules(cmd)

        self.assertEqual(
                            list_nat_rule_response,
                            None,
                            "Check Port Forwarding Rule is deleted"
                            )
        self.debug("Check if the Public SSH port is inaccessible")
        with self.assertRaises(Exception):
            remoteSSHClient.remoteSSHClient(
                                            ip.ipaddress,
                                            self.virtual_machine.ssh_port,
                                            self.virtual_machine.username,
                                            self.virtual_machine.password
                                            )

    def test_02_port_fwd_on_non_src_nat(self):
        """Port Forwarding Tests for Non-Source NAT IP Addresses
        """

        ip_address = PublicIPAddress.create(self.apiclient, self.account)
        self.clean_up.append(ip_address)
        nat_rule = NATRule.create(self.apiclient, self.virtual_machine, services)
        time.sleep(60)

        #Validate the following:
        #1. listPortForwarding rules API should return the added PF rule
        #2. attempt to do an ssh into the  user VM through the sourceNAT

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
            self.virtual_machine.get_ssh_client(public_ip = ip_address.ipaddress)
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %(self.virtual_machine.ipaddress, e))

        nat_rule.delete(apiclient)
        time.sleep(60)

        #Validate the following:
        #1. listPortForwardingRules should not return the deleted rule anymore
        #2. attempt to do ssh should now fail

        cmd = listPortForwardingRules.listPortForwardingRulesCmd()
        cmd.id = nat_rule.id
        list_nat_rule_response = self.apiclient.listPortForwardingRules(cmd)


        self.assertEqual(
                            len(list_nat_rule_response),
                            None,
                            "Check Port Forwarding Rule is deleted"
                            )
        self.debug("Check if the Public SSH port is inaccessible")
        with self.assertRaises(Exception):
            remoteSSHClient.remoteSSHClient(
                                            ip_address.ipaddress,
                                            self.virtual_machine.ssh_port,
                                            self.virtual_machine.username,
                                            self.virtual_machine.password
                                            )



class TestLoadBalancingRule(cloudstackTestCase):
    """Test Load Balancing Rules for Source and Non-Source IP Addresses
    """
    @classmethod
    def setUpClass(cls):

        cls.api_client = fetch_api_client()
        #Create an account, network, VM and IP addresses
        cls.account = Account.create(cls.api_client, services["account"], admin=True)
        cls.vm_1 = VirtualMachine.create(
                                            cls.api_client,
                                            services["server"],
                                            accountid=cls.account.account.name,
                                            networkids=[str(cls.account.network.id)]
                                        )
        cls.vm_2 = VirtualMachine.create(
                                            cls.api_client,
                                            services["server"],
                                            accountid=cls.account.account.name,
                                            networkids=[str(cls.account.network.id)]
                                        )
        cls.non_src_nat_ip = PublicIPAddress.create(
                                                    cls.api_client,
                                                    cls.account.account.name,
                                                    services["zoneid"],
                                                    services["domainid"]
                                                    )
        cls._cleanup = [cls.vm_1, cls.vm_2, cls.non_src_nat_ip, cls.account]

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)

    @classmethod
    def tearDownClass(cls):
        cleanup_resources(cls.api_client, cls._cleanup)


    def test_01_create_lb_rule_src_nat(self):
        """Test Port Forwarding Rules for Source NAT IP Addresses
        """
        src_nat_ip_addr = self.account.public_ip.ipaddress

        lb_rule = LoadBalancerRule.create(
                                            self.apiclient,
                                            services["lbrule"],
                                            src_nat_ip_addr.id,
                                            accountid = self.account.account.name
                                        )
        self.cleanup.append(lb_rule)

        lb_rule.assign(self.apiclient, [self.vm_1, self.vm_2])
        # Validate the Following:
        #1. listLoadBalancerRules should return the added rule
        #2. attempt to ssh twice on the load balanced IP
        #3. verify using the hostname of the VM that round robin is indeed happening as expected
        cmd = listLoadBalancerRules.listLoadBalancerRulesCmd()
        cmd.id = lb_rule.id
        lb_rules = self.apiclient.listLoadBalancerRules(cmd)

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


        ssh_1 = remoteSSHClient.remoteSSHClient(
                                                src_nat_ip_addr.ipaddress,
                                                services['lbrule']["publicport"],
                                                self.vm_1.username,
                                                self.vm_1.password
                                            )

        #If Round Robin Algorithm is chosen, each ssh command should alternate between VMs
        hostnames = [ssh_1.execute("hostname")[0]]
        time.sleep(20)
        ssh_2 = remoteSSHClient.remoteSSHClient(
                                                src_nat_ip_addr.ipaddress,
                                                services['lbrule']["publicport"],
                                                self.vm_1.username,
                                                self.vm_1.password
                                            )


        hostnames.append(ssh_2.execute("hostname")[0])
        self.assertIn(self.vm_1.name, hostnames, "Check if ssh succeeded for server1")
        self.assertIn(self.vm_2.name, hostnames, "Check if ssh succeeded for server2")

    def test_02_create_lb_rule_non_nat(self):
        """Test Load Balancing Rules for Non-Source IP Addresses
        """
        lb_rule = LoadBalancerRule.create(
                                            self.apiclient,
                                            services["lbrule"],
                                            cls.non_src_nat_ip.ipaddress.id,
                                            accountid = self.account.account.name
                                        )
        self.cleanup.append(lb_rule)

        lb_rule.assign(self.apiclient, [self.vm_1, self.vm_2])

        # Validate the Following:
        #1. listLoadBalancerRules should return the added rule
        #2. attempt to ssh twice on the load balanced IP
        #3. verify using the hostname of the VM that round robin is indeed happening as expected

        cmd = listLoadBalancerRules.listLoadBalancerRulesCmd()
        cmd.id = lb_rule.id
        lb_rules = self.apiclient.listLoadBalancerRules(cmd)

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


        ssh_1 = remoteSSHClient.remoteSSHClient(
                                                cls.non_src_nat_ip.ipaddress.ipaddress,
                                                services['lbrule']["publicport"],
                                                self.vm_1.username,
                                                self.vm_1.password
                                            )

        #If Round Robin Algorithm is chosen, each ssh command should alternate between VMs
        hostnames = [ssh_1.execute("hostname")[0]]
        time.sleep(20)
        ssh_2 = remoteSSHClient.remoteSSHClient(
                                                cls.non_src_nat_ip.ipaddress.ipaddress,
                                                services['lbrule']["publicport"],
                                                self.vm_1.username,
                                                self.vm_1.password
                                            )


        hostnames.append(ssh_2.execute("hostname")[0])
        self.assertIn(self.vm_1.name, hostnames, "Check if ssh succeeded for server1")
        self.assertIn(self.vm_2.name, hostnames, "Check if ssh succeeded for server2")



class TestRebootRouter(cloudstackTestCase):
    """Test Load Balancing Rules work post Router Reboot
    """
    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        #Create an account, network, VM and IP addresses
        self.account = Account.create(self.apiclient, services["account"], admin=True)
        self.vm_1 = VirtualMachine.create(
                                            self.apiclient,
                                            services["server"],
                                            accountid=self.account.account.name,
                                            networkids=[str(self.account.network.id)]
                                        )
        src_nat_ip_addr = self.account.public_ip.ipaddress
        lb_rule = LoadBalancerRule.create(
                                            self.apiclient,
                                            services["lbrule"],
                                            src_nat_ip_addr.id,
                                            self.account.account.name
                                        )
        lb_rule.assign(self.apiclient, [self.vm_1])
        #nat_rule = NATRule.create(self.apiclient, self.vm_1, services["natrule"], src_nat_ip_addr.id)
        self.cleanup = [self.vm_1, lb_rule, self.account]


    def test_reboot_router(self):

        #Validate the Following
        #1. Post restart PF and LB rules should still function
        #2. verify if the ssh into the virtual machine still works through the sourceNAT Ip
        cmd = listRouters.listRoutersCmd()
        cmd.account = self.account.account.name
        cmd.domainid = self.account.account.domainid
        routers = self.apiclient.listRouters(cmd)
        router = routers[0]

        cmd = rebootRouter.rebootRouterCmd()
        cmd.id = router.id
        self.apiclient.rebootRouter(cmd)
        time.sleep(60)
        src_nat_ip_addr = self.account.public_ip.ipaddress

        try:
            remoteSSHClient.remoteSSHClient(
                                            src_nat_ip_addr.ipaddress,
                                            services["natrule"]["publicport"],
                                            self.vm_1.username,
                                            self.vm_1.password
                                            )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %(self.vm_1.ipaddress, e))

    def tearDown(self):
        cleanup_resources(self.cleanup)

class TestAssignRemoveLB(cloudstackTestCase):
    """Assign Load Balancer Rule to two Virtual Machines, Remove One VM
    and associate another VM.
    """

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.account = Account.create(self.apiclient, services["account"], admin=True)

        self.vm_1 = VirtualMachine.create(
                                            self.apiclient,
                                            services["server"],
                                            accountid=self.account.account.name,
                                            networkids=[str(self.account.network.id)]
                                        )
        self.vm_2 = VirtualMachine.create(
                                            self.apiclient,
                                            services["server"],
                                            accountid=self.account.account.name,
                                            networkids=[str(self.account.network.id)]
                                        )

        self.vm_3 = VirtualMachine.create(
                                            self.apiclient,
                                            services["server"],
                                            accountid=self.account.account.name,
                                            networkids=[str(self.account.network.id)]
                                        )
        self.non_src_nat_ip = self.account.public_ip.ipaddress
        self.cleanup = [self.vm_1, self.vm_2, self.vm_3]

    def test_assign_and_removal_elb(self):

        #Validate:
        #1. Verify list API - listLoadBalancerRules lists all the rules with the relevant ports
        #2. listLoadBalancerInstances will list the instances associated with the corresponding rule.
        #3. verify ssh attempts should pass as long as there is at least one instance associated with the rule

        lb_rule = LoadBalancerRule.create(
                                            self.apiclient,
                                            services["lbrule"],
                                            self.non_src_nat_ip.id,
                                            self.account.account.name
                                        )
        self.cleanup.append(lb_rule)
        lb_rule.assign(self.apiclient, [self.vm_1, self.vm_2])

        ssh_1 = remoteSSHClient.remoteSSHClient(
                                                self.non_src_nat_ip.ipaddress,
                                                services["natrule"]["publicport"],
                                                self.vm_1.username,
                                                self.vm_1.password
                                            )

        ssh_2 = remoteSSHClient.remoteSSHClient(
                                                self.non_src_nat_ip.ipaddress,
                                                services["natrule"]["publicport"],
                                                self.vm_2.username,
                                                self.vm_2.password
                                            )
        ssh_3 = remoteSSHClient.remoteSSHClient(
                                                self.non_src_nat_ip.ipaddress,
                                                services["natrule"]["publicport"],
                                                self.vm_3.username,
                                                self.vm_3.password
                                            )
        #If Round Robin Algorithm is chosen, each ssh command should alternate between VMs
        res_1 = ssh_1.execute("hostname")[0]
        time.sleep(20)
        res_2 = ssh_2.execute("hostname")[0]

        self.assertIn(self.vm_1.name, res_1, "Check if ssh succeeded for server1")
        self.assertIn(self.vm_2.name, res_2, "Check if ssh succeeded for server2")

        lb_rule.remove(self.apiclient, [self.vm_2])

        res_1 = ssh_1.execute("hostname")[0]
        self.assertIn(self.vm_1.name, res_1, "Check if ssh succeeded for server1")

        lb_rule.assign(self.apiclient, [self.vm_3])

        res_1 = ssh_1.execute("hostname")[0]
        time.sleep(20)
        res_3 = ssh_3.execute("hostname")[0]

        self.assertIn(self.vm_1.name, res_1, "Check if ssh succeeded for server1")
        self.assertIn(self.vm_3.name, res_3, "Check if ssh succeeded for server3")

    def teardown(self):
        cleanup_resources(self.cleanup)
