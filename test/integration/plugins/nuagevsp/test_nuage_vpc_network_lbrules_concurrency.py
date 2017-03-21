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

""" Component tests for VPC Network Load Balancing Rules Concurrency
"""
# Import Local Modules
from nuageTestCase import nuageTestCase
from marvin.lib.base import Account, LoadBalancerRule
# Import System Modules
from nose.plugins.attrib import attr
from concurrent.futures import ThreadPoolExecutor, wait, Future


class TestNuageVPCNetworkLBRulesConcurrency(nuageTestCase):
    """Test VPC Network Load Balancing Rules Concurrency with Nuage VSP
    SDN plugin
    """

    @classmethod
    def setUpClass(cls):
        super(TestNuageVPCNetworkLBRulesConcurrency, cls).setUpClass()
        return

    def setUp(self):
        # Create an account
        self.account = Account.create(self.api_client,
                                      self.test_data["account"],
                                      admin=True,
                                      domainid=self.domain.id
                                      )
        self.cleanup = [self.account]

        # Validating network service providers
        providers = ["NuageVsp", "VpcVirtualRouter", "VpcInlineLbVm"]
        for provider in providers:
            self.validate_NetworkServiceProvider(provider, state="Enabled")

        # Creating VPC offering
        self.lb_vpc_off = self.create_VpcOffering(
            self.test_data["nuagevsp"]["vpc_offering_lb"])
        self.validate_VpcOffering(self.lb_vpc_off, state="Enabled")

        # Creating network offering
        self.lb_net_off = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering_vpcinlinelbvm"])
        self.validate_NetworkOffering(self.lb_net_off, state="Enabled")
        return

    # create_LB_Rule - Creates a lb rule for the given public ip address
    def create_LB_Rule(self, public_ip, network, vpc, vm_array=None,
                       services=None):
        self.debug("Creating LB rule for IP address: %s" %
                   public_ip.ipaddress.ipaddress)
        if not services:
            services = self.test_data["vpclbrule"]
        lb_rule = LoadBalancerRule.create(self.api_client,
                                          services,
                                          ipaddressid=public_ip.ipaddress.id,
                                          accountid=self.account.name,
                                          networkid=network.id,
                                          vpcid=vpc.id
                                          )
        # Assigning VMs to the created Public Load Balancer rule
        if vm_array:
            self.debug("Adding virtual machines %s to LB rule" % vm_array)
            lb_rule.assign(self.api_client, vm_array)
        return lb_rule

    # validate_LBRule - Validates the load balancer rule creation and state
    def validate_LBRule(self, lb_rule, state=None):
        """Validates the LB Rule"""
        self.debug("Validating the creation and state of LB Rule - %s" %
                   lb_rule.name)
        lb_rules = LoadBalancerRule.list(self.api_client,
                                         id=lb_rule.id
                                         )
        self.assertEqual(isinstance(lb_rules, list), True,
                         "List LB Rule should return a valid list"
                         )
        self.assertEqual(lb_rule.name, lb_rules[0].name,
                         "Name of the LB Rule should match with the returned "
                         "list data"
                         )
        if state:
            self.assertEqual(lb_rules[0].state, state,
                             "LB Rule state should be '%s'" % state
                             )
        self.debug("Successfully validated the creation and state of LB Rule "
                   "- %s" % lb_rule.name)

    # check_result - Checks concurrent operation's result
    def check_result(self, future):
        result = future.result()
        success = result.success
        if not success:
            self.error("Delete failed: %s" % result.displaytext)
        return success

    @attr(tags=["advanced", "nuagevsp", "concurrency"],
          required_hardware="false")
    def test_01_nuage_vpc_network_public_lb_rules_concurrency(self):
        """Test Nuage VSP VPC Network Public Load Balancing Rules Concurrency
        """

        thread_count = 10

        # Creating VPC
        vpc = self.create_Vpc(self.lb_vpc_off, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc, state="Enabled")

        # Creating VPC network and deploying VMs
        lb_tier = self.create_Network(
            self.lb_net_off, gateway='10.1.1.1', vpc=vpc)
        self.validate_Network(lb_tier, state="Implemented")
        vr = self.get_Router(lb_tier)
        self.check_Router_state(vr, state="Running")
        lb_vm_1 = self.create_VM(lb_tier)
        self.check_VM_state(lb_vm_1, state="Running")
        lb_vm_2 = self.create_VM(lb_tier)
        self.check_VM_state(lb_vm_2, state="Running")
        vm_array = [lb_vm_1, lb_vm_2]

        # VSD verification
        self.verify_vsd_network(self.domain.id, lb_tier, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(lb_vm_1)
        self.verify_vsd_vm(lb_vm_2)

        # Creating Public LB Rules
        public_ips = [self.acquire_PublicIPAddress(lb_tier, vpc)
                      for _ in range(thread_count)]
        lb_rules = [self.create_LB_Rule(ip, lb_tier, vpc) for ip in public_ips]
        for lb_rule in lb_rules:
            self.validate_LBRule(lb_rule, state="Add")

        # Concurrency tests for Public LB Rules
        with ThreadPoolExecutor(max_workers=thread_count) as executor:
            self.debug("Concurrently assigning LB VMs to %s rules" %
                       thread_count)
            futures = [executor.submit(LoadBalancerRule.assign, rule,
                                       self.api_client, vms=vm_array)
                       for rule in lb_rules]
            wait(futures)
            assign_vms_result = all(map(lambda f: self.check_result(f),
                                        futures))

            self.debug("Concurrently removing LB VMs from %s rules" %
                       thread_count)
            futures = [executor.submit(LoadBalancerRule.remove, rule,
                                       self.api_client, vms=vm_array)
                       for rule in lb_rules]
            wait(futures)
            remove_vms_result = all(map(lambda f: self.check_result(f),
                                        futures))

            self.debug("Concurrently deleting %s LB rules" % thread_count)
            futures = [executor.submit(LoadBalancerRule.delete, rule,
                                       self.api_client) for rule in lb_rules]
            wait(futures)
            delete_rules_result = all(map(lambda f: self.check_result(f),
                                          futures))
            print(map(Future.result, futures))

            self.assertEqual(assign_vms_result, True,
                             "Failed to assign vms to all lb rules "
                             "concurrently")
            self.assertEqual(remove_vms_result, True,
                             "Failed to remove vms from all lb rules "
                             "concurrently")
            self.assertEqual(delete_rules_result, True,
                             "Failed to delete all lb rules concurrently")
