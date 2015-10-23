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

from marvin.codes import FAILED
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.sshClient import SshClient
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr
#Import System modules
import time

_multiprocess_shared_ = True

class TestLoadBalance(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        testClient = super(TestLoadBalance, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient() 
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        template = get_template(
                            cls.apiclient,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        if template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]
        
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        #Create an account, network, VM and IP addresses
        cls.account = Account.create(
                            cls.apiclient,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain.id
                            )
        cls.service_offering = ServiceOffering.create(
                                        cls.apiclient,
                                        cls.services["service_offerings"]["tiny"]
                                        )
        cls.vm_1 = VirtualMachine.create(
                                    cls.apiclient,
                                    cls.services["virtual_machine"],
                                    templateid=template.id,
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    serviceofferingid=cls.service_offering.id
                                    )
        cls.vm_2 = VirtualMachine.create(
                                    cls.apiclient,
                                    cls.services["virtual_machine"],
                                    templateid=template.id,
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    serviceofferingid=cls.service_offering.id
                                    )
        cls.vm_3 = VirtualMachine.create(
                                    cls.apiclient,
                                    cls.services["virtual_machine"],
                                    templateid=template.id,
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    serviceofferingid=cls.service_offering.id
                                    )
        cls.non_src_nat_ip = PublicIPAddress.create(
                                            cls.apiclient,
                                            cls.account.name,
                                            cls.zone.id,
                                            cls.account.domainid,
                                            cls.services["virtual_machine"]
                                            )
        # Open up firewall port for SSH
        cls.fw_rule = FireWallRule.create(
                            cls.apiclient,
                            ipaddressid=cls.non_src_nat_ip.ipaddress.id,
                            protocol=cls.services["lbrule"]["protocol"],
                            cidrlist=['0.0.0.0/0'],
                            startport=cls.services["lbrule"]["publicport"],
                            endport=cls.services["lbrule"]["publicport"]
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
        cleanup_resources(cls.apiclient, cls._cleanup)
        return

    def try_ssh(self, ip_addr, hostnames):
        try:
            self.debug(
                "SSH into VM (IPaddress: %s) & NAT Rule (Public IP: %s)" %
                (self.vm_1.ipaddress, ip_addr)
            )
           # If Round Robin Algorithm is chosen,
            # each ssh command should alternate between VMs

            ssh_1  = SshClient(
                ip_addr,
                self.services['lbrule']["publicport"],
                self.vm_1.username,
                self.vm_1.password
            )
            hostnames.append(ssh_1.execute("hostname")[0])
            self.debug(hostnames)
        except Exception as e:
            self.fail("%s: SSH failed for VM with IP Address: %s" %
                                    (e, ip_addr))
        time.sleep(10)
        return

    @attr(tags = ["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_01_create_lb_rule_src_nat(self):
        """Test to create Load balancing rule with source NAT"""

        # Validate the Following:
        #1. listLoadBalancerRules should return the added rule
        #2. attempt to ssh twice on the load balanced IP
        #3. verify using the hostname of the VM
        #   that round robin is indeed happening as expected
        src_nat_ip_addrs = PublicIPAddress.list(
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

        # Check if VM is in Running state before creating LB rule
        vm_response = VirtualMachine.list(
                                          self.apiclient,
                                          account=self.account.name,
                                          domainid=self.account.domainid
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
                                          accountid=self.account.name
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
        self.debug("lb_instance_rules Ids: %s, %s" % (
                                                    lb_instance_rules[0].id,
                                                    lb_instance_rules[1].id
                                                    ))
        self.debug("VM ids: %s, %s" % (self.vm_1.id, self.vm_2.id))

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


        hostnames = []
        self.try_ssh(src_nat_ip_addr.ipaddress, hostnames)
        self.try_ssh(src_nat_ip_addr.ipaddress, hostnames)
        self.try_ssh(src_nat_ip_addr.ipaddress, hostnames)
        self.try_ssh(src_nat_ip_addr.ipaddress, hostnames)
        self.try_ssh(src_nat_ip_addr.ipaddress, hostnames)

        self.debug("Hostnames: %s" % str(hostnames))
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

        # making hostnames list empty
        hostnames[:] = []

        try:
            self.debug("SSHing into IP address: %s after removing VM (ID: %s)" %
                                            (
                                             src_nat_ip_addr.ipaddress,
                                             self.vm_2.id
                                             ))

            self.try_ssh(src_nat_ip_addr.ipaddress, hostnames)
            self.assertIn(
                          self.vm_1.name,
                          hostnames,
                          "Check if ssh succeeded for server1"
                          )
        except Exception as e:
            self.fail("%s: SSH failed for VM with IP Address: %s" %
                                        (e, src_nat_ip_addr.ipaddress))

        lb_rule.remove(self.apiclient, [self.vm_1])

        with self.assertRaises(Exception):
            self.debug("Removed all VMs, trying to SSH")
            self.try_ssh(src_nat_ip_addr.ipaddress, hostnames)
        return

    @attr(tags = ["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_02_create_lb_rule_non_nat(self):
        """Test to create Load balancing rule with non source NAT"""

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
            accountid=self.account.name
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
            hostnames = []
            self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, hostnames)
            self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, hostnames)
            self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, hostnames)
            self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, hostnames)
            self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, hostnames)

            self.debug("Hostnames: %s" % str(hostnames))
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
            # Making host list empty
            hostnames[:] = []

            self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, hostnames)
            self.assertIn(
                self.vm_1.name,
                hostnames,
                "Check if ssh succeeded for server1"
            )
            self.debug("Hostnames after removing VM2: %s" % str(hostnames))
        except Exception as e:
            self.fail("%s: SSH failed for VM with IP Address: %s" %
                      (e, self.non_src_nat_ip.ipaddress.ipaddress))

        lb_rule.remove(self.apiclient, [self.vm_1])
        with self.assertRaises(Exception):
            self.debug("SSHing into IP address: %s after removing VM (ID: %s) from LB rule" %
                       (
                           self.non_src_nat_ip.ipaddress.ipaddress,
                           self.vm_1.id
                           ))
            self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, hostnames)
        return

    @attr(tags = ["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_assign_and_removal_lb(self):
        """Test for assign & removing load balancing rule"""

        # Validate:
        #1. Verify list API - listLoadBalancerRules lists
        #   all the rules with the relevant ports
        #2. listLoadBalancerInstances will list
        #   the instances associated with the corresponding rule.
        #3. verify ssh attempts should pass as long as there
        #   is at least one instance associated with the rule

        # Check if VM is in Running state before creating LB rule
        vm_response = VirtualMachine.list(
                                          self.apiclient,
                                          account=self.account.name,
                                          domainid=self.account.domainid
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
                                self.non_src_nat_ip.ipaddress.id,
                                self.account.name
                              )
        lb_rule.assign(self.apiclient, [self.vm_1, self.vm_2])

        hostnames = []
        self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, hostnames)
        self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, hostnames)
        self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, hostnames)
        self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, hostnames)
        self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, hostnames)

        self.debug("Hostnames: %s" % str(hostnames))
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
        #Removing VM and assigning another VM to LB rule
        lb_rule.remove(self.apiclient, [self.vm_2])

        # making hostnames list empty
        hostnames[:] = []

        try:
            self.debug("SSHing again into IP address: %s with VM (ID: %s) added to LB rule" %
                                            (
                                             self.non_src_nat_ip.ipaddress.ipaddress,
                                             self.vm_1.id,
                                             ))
            self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, hostnames)

            self.assertIn(
                          self.vm_1.name,
                          hostnames,
                          "Check if ssh succeeded for server1"
                          )
        except Exception as e:
            self.fail("SSH failed for VM with IP: %s" %
                                    self.non_src_nat_ip.ipaddress.ipaddress)

        lb_rule.assign(self.apiclient, [self.vm_3])

#        # Making hostnames list empty
        hostnames[:] = []
        self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, hostnames)
        self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, hostnames)
        self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, hostnames)
        self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, hostnames)
        self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, hostnames)
        self.debug("Hostnames: %s" % str(hostnames))
        self.assertIn(
                  self.vm_1.name,
                  hostnames,
                  "Check if ssh succeeded for server1"
                )
        self.assertIn(
                  self.vm_3.name,
                  hostnames,
                  "Check if ssh succeeded for server3"
                  )
        return
