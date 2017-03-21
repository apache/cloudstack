# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

""" Component tests for VpcInlineLbVm Functionality with Nuage VSP SDN plugin
"""
# Import Local Modules
from nuageTestCase import nuageTestCase
from marvin.lib.base import (Account,
                             LoadBalancerRule,
                             Router)
from marvin.cloudstackAPI import (listVpcInlineLoadBalancerVMs,
                                  stopVpcInlineLoadBalancerVM,
                                  startVpcInlineLoadBalancerVM)
# Import System Modules
from nose.plugins.attrib import attr
import functools
import time


class gherkin(object):
    BLACK = "\033[0;30m"
    BLUE = "\033[0;34m"
    GREEN = "\033[0;32m"
    CYAN = "\033[0;36m"
    RED = "\033[0;31m"
    BOLDBLUE = "\033[1;34m"
    NORMAL = "\033[0m"

    def __init__(self, method):
        self.method = method

    def __get__(self, obj=None, objtype=None):
        @functools.wraps(self.method)
        def _wrapper(*args, **kwargs):
            gherkin_step = self.method.__name__.replace("_", " ").capitalize()
            obj.info("=G= %s%s%s" % (self.BOLDBLUE, gherkin_step, self.NORMAL))
            try:
                result = self.method(obj, *args, **kwargs)
                obj.info("=G= %s%s: [SUCCESS]%s" %
                         (self.GREEN, gherkin_step, self.NORMAL))
                return result
            except Exception as e:
                obj.info("=G= %s%s: [FAILED]%s" %
                         (self.RED, gherkin_step, self.NORMAL))
                raise e
        return _wrapper


class VpcInlineLbVm(nuageTestCase):
    """Test VpcInlineLbVm Functionality with Nuage VSP SDN plugin
    """

    @classmethod
    def setUpClass(cls):
        super(VpcInlineLbVm, cls).setUpClass()
        return

    def setUp(self):
        # Create an account
        self.account = Account.create(self.api_client,
                                      self.test_data["account"],
                                      admin=True,
                                      domainid=self.domain.id
                                      )
        self.cleanup = [self.account]

        self.given_a_physical_network_with_vpcinlinelb_enabled()

        self.vpc_off_lb = self.given_a_vpc_offering_with_vpcinlinelb()

        self.net_off_lb = self.given_a_network_offering(
            key="vpc_network_offering_vpcinlinelbvm",
            description="with vpcinlinelbvm")

        self.net_off_no_lb = self.given_a_network_offering(
            key="vpc_network_offering",
            description="without vpcinlinelbvm")

        self.acl_rules = []
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

    # update_LB_Rule_algorithm - Updates the given lb rule's algorithm
    def update_LB_Rule_algorithm(self, lb_rule, algorithm):
        self.debug("Updating the lb rule %s with algorithm %s" %
                   (lb_rule.id, algorithm))
        lb_rule.update(self.api_client, algorithm)

    # assign_vms_LB_Rule - Assigns virtual machines to the given lb rule
    def assign_vms_LB_Rule(self, lb_rule, vm_array):
        self.debug("Adding virtual machines %s to LB rule " % vm_array)
        lb_rule.assign(self.api_client, vm_array)

    # remove_vms_LB_Rule - Removes virtual machines from the given lb rule
    def remove_vms_LB_Rule(self, lb_rule, vm_array):
        self.debug("Delete virtual machines %s from LB rule " % vm_array)
        lb_rule.remove(self.api_client, vm_array)

    # restart_network - Network restart with/without cleanup
    def restart_network(self, network, cleanup=None):
        self.debug("Network restart %s " % network.name)
        if cleanup:
            network.restart(self.api_client, cleanup=True)
        network.restart(self.api_client)

    # delete_LB_Rule - Deletes load balancer rule from public address
    def delete_LB_Rule(self, lb_rule):
        self.debug("Deleting lb rule for public ip address : %s" %
                   lb_rule.publicip)
        lb_rule.delete(self.api_client)

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

    # list_VpcInlineLbVms - List VpcInlineLbVms
    def list_VpcInlineLbVms(self, vpc_id=None, network_id=None):
        listVpcInlineLoadBalancerVMsCmd = \
            listVpcInlineLoadBalancerVMs.listVpcInlineLoadBalancerVMsCmd()
        listVpcInlineLoadBalancerVMsCmd.account = self.account.name
        listVpcInlineLoadBalancerVMsCmd.domainid = self.account.domainid
        if vpc_id:
            listVpcInlineLoadBalancerVMsCmd.vpcid = vpc_id
        if network_id:
            listVpcInlineLoadBalancerVMsCmd.networkid = network_id
        public_lb_vms = self.api_client.listVpcInlineLoadBalancerVMs(
            listVpcInlineLoadBalancerVMsCmd)
        return public_lb_vms

    # get_VpcInlineLbVm - Gets the VpcInlineLbVm for given VPC and public IPs
    def get_VpcInlineLbVm(self, vpc, network, expected_public_ips=[]):
        self.debug("Finding the VpcInlineLbVm in the VPC - %s with public IPs "
                   "- %s" % (vpc.name, expected_public_ips))
        public_lb_vms = self.list_VpcInlineLbVms(vpc.id, network.id)
        self.assertEqual(isinstance(public_lb_vms, list), True,
                         "List VpcInlineLbVms should return a valid list"
                         )
        self.assertEqual(len(public_lb_vms), 1,
                         "Only one VpcInlineLbVm should be deployed per a VPC "
                         "network/tier"
                         )
        # Get the public ip address into the list
        public_ips = public_lb_vms[0].publicip.replace(' ', '').split(',')
        if public_ips == expected_public_ips:
            self.debug("The VpcInlineLbVm associated public IP addresses are "
                       "as expected")
        else:
            raise Exception("The VpcInlineLbVm associated public IP addresses "
                            "are not as expected")
        return public_lb_vms[0]

    # stop_VpcInlineLbVm - Stop VpcInlineLbVm appliance
    def stop_VpcInlineLbVm(self, lb_vm, force=None):
        self.debug("Stopping VpcInlineLbVm with ID: %s" % lb_vm.id)
        cmd = stopVpcInlineLoadBalancerVM.stopVpcInlineLoadBalancerVMCmd()
        cmd.id = lb_vm.id
        if force:
            cmd.forced = force
        self.api_client.stopVpcInlineLoadBalancerVM(cmd)

    # start_VpcInlineLbVm - Start VpcInlineLbVm appliance
    def start_VpcInlineLbVm(self, lb_vm):
        self.debug("Starting VpcInlineLbVm with ID: %s" % lb_vm.id)
        cmd = startVpcInlineLoadBalancerVM.startVpcInlineLoadBalancerVMCmd()
        cmd.id = lb_vm.id
        self.api_client.startVpcInlineLoadBalancerVM(cmd)

    # check_VpcInlineLbVm_state - Check VpcInlineLbVm appliance state
    def check_VpcInlineLbVm_state(self, lb_vm, state="Running"):
        """Validates the VpcInlineLbVm state"""
        self.debug("Validating the state of VpcInlineLbVm appliance - %s" %
                   lb_vm.id)
        self.assertEqual(lb_vm.state, state,
                         "VpcInlineLbVm is not in the expected state"
                         )
        self.debug("Successfully validated the state of VpcInlineLbVm "
                   "appliance - %s" % lb_vm.id)

    # verify_lb_wget_file - Verifies that the given wget file (index.html)
    # belongs to the given Public LB rule assigned VMs (vm array)
    def verify_lb_wget_file(self, wget_file, vm_array):
        wget_server_ip = None
        for vm in vm_array:
            for nic in vm.nic:
                if str(nic.ipaddress) in str(wget_file):
                    wget_server_ip = str(nic.ipaddress)
        if wget_server_ip:
            self.debug("Verified wget file from Public Load Balanced VMs - "
                       "%s" % vm_array)
        else:
            self.fail("Failed to verify wget file from Public Load Balanced "
                      "VMs - %s" % vm_array)
        return wget_server_ip

    # verify_vpc_vm_ingress_traffic - Verifies ingress traffic to the given VM
    # (SSH into VM) via a created Static NAT rule in the given VPC network
    def verify_vpc_vm_ingress_traffic(self, vm, network, vpc):
        self.debug("Verifying ingress traffic to the VM (SSH into VM) - %s "
                   "via a created Static NAT rule in the VPC network - %s" %
                   (vm, network))

        # Creating Static NAT rule for the given VM in the given VPC network
        self.debug("Creating Static NAT Rule...")
        test_public_ip = self.acquire_PublicIPAddress(network, vpc)
        self.validate_PublicIPAddress(test_public_ip, network)
        self.create_StaticNatRule_For_VM(vm, test_public_ip, network)
        self.validate_PublicIPAddress(
            test_public_ip, network, static_nat=True, vm=vm)

        # VSD verification
        self.verify_vsd_floating_ip(network, vm, test_public_ip.ipaddress, vpc)

        # Adding Network ACL rule in the given VPC network
        self.debug("Creating Network ACL rule ...")
        test_public_ssh_rule = self.create_NetworkAclRule(
            self.test_data["ingress_rule"], network=network)

        # VSD verification
        self.verify_vsd_firewall_rule(test_public_ssh_rule)

        # SSH into VM
        self.debug("Verifying VM ingress traffic (SSH into VM)...")
        self.ssh_into_VM(vm, test_public_ip)

        # Removing Network ACL rule in the given VPC network
        self.debug("Removing the created Network ACL rule...")
        test_public_ssh_rule.delete(self.api_client)

        # VSD verification
        with self.assertRaises(Exception):
            self.verify_vsd_firewall_rule(test_public_ssh_rule)
        self.debug("Network ACL rule successfully deleted in VSD")

        # Deleting Static NAT Rule
        self.debug("Deleting the created Static NAT Rule...")
        self.delete_StaticNatRule_For_VM(test_public_ip)
        with self.assertRaises(Exception):
            self.validate_PublicIPAddress(
                test_public_ip, network, static_nat=True, vm=vm)
        self.debug("Static NAT Rule successfully deleted in CloudStack")

        # VSD verification
        with self.assertRaises(Exception):
            self.verify_vsd_floating_ip(
                network, vm, test_public_ip.ipaddress, vpc=vpc)
        self.debug("Floating IP successfully deleted in VSD")

        # Releasing acquired public IP
        self.debug("Releasing the acquired public IP...")
        test_public_ip.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_PublicIPAddress(test_public_ip, network)
        self.debug("Acquired public IP in the network successfully released "
                   "in CloudStack")

        self.debug("Successfully verified ingress traffic to the VM "
                   "(SSH into VM) - %s via a created Static NAT rule in the "
                   "VPC network - %s" % (vm, network))

    # =========================================================================
    #                    Gherkin style helper methods
    # =========================================================================
    @gherkin
    def given_a_physical_network_with_vpcinlinelb_enabled(self):
        # Validating network service providers
        self.debug("Validating network service providers supported by Nuage "
                   "VSP for VpcInline LB functionality")
        providers = ["NuageVsp", "VpcVirtualRouter", "VpcInlineLbVm"]
        for provider in providers:
            self.validate_NetworkServiceProvider(provider, state="Enabled")

    @gherkin
    def given_a_vpc_offering_with_vpcinlinelb(self):
        # Creating VPC offering
        self.debug("Creating Nuage VSP VPC offering with LB service provider "
                   "as vpcinlinelbvm")
        vpc_off = self.create_VpcOffering(self.test_data["nuagevsp"][
            "vpc_offering_lb"])
        self.validate_VpcOffering(vpc_off, state="Enabled")
        return vpc_off

    @gherkin
    def given_a_network_offering(self, testdata=None, key=None,
                                 description=None):
        if not testdata:
            testdata = self.test_data["nuagevsp"][key]
        if not description:
            description = testdata["name"]

        # Creating network offering
        self.debug("Creating Nuage VSP VPC Network offering %s" % description)
        net_off = self.create_NetworkOffering(testdata)
        self.validate_NetworkOffering(net_off, state="Enabled")
        return net_off

    @gherkin
    def when_i_create_a_vpc_with_vpcinlinelb(self, vpc_off_lb,
                                             cidr='10.1.0.0/16', account=None):
        if not account:
            account = self.account

        # Creating VPC
        self.debug("Creating a VPC with VPC offering %s" % vpc_off_lb.name)
        vpc = self.create_Vpc(vpc_off_lb, cidr=cidr, account=account)
        self.validate_Vpc(vpc, state="Enabled")
        return vpc

    @gherkin
    def when_i_create_a_tier_with_vms(self, vpc, net_off, testdata=None,
                                      gateway=None, number_of_vms=1,
                                      account=None):
        if not account:
            account = self.account

        # Creating VPC network/tier
        self.debug("Creating a VPC network with network offering %s" %
                   net_off.name)
        tier = self.create_Network(net_off, testdata=testdata, gateway=gateway,
                                   vpc=vpc, account=account)
        self.validate_Network(tier, state="Implemented")
        vr = self.get_Router(tier)
        self.check_Router_state(vr, state="Running")

        # Deploying VMs
        self.debug("Deploying %s VMs in network %s" %
                   (number_of_vms, tier.name))
        vm_array = []
        for _ in range(number_of_vms):
            vm = self.create_VM(tier)
            self.check_VM_state(vm, state="Running")

            # VSD verification
            self.verify_vsd_network(account.domainid, tier, vpc)
            self.verify_vsd_router(vr)
            self.verify_vsd_vm(vm)

            vm_array.append(vm)
        return tier, vr, vm_array

    @gherkin
    def when_i_acquire_a_public_ip(self, network, vpc):
        # Acquiring Public IP
        self.debug("Acquiring Public IP in network %s" % network.name)
        public_ip = self.acquire_PublicIPAddress(network, vpc)
        self.validate_PublicIPAddress(public_ip, network)
        return public_ip

    @gherkin
    def when_i_release_a_public_ip(self, public_ip, network):
        # Releasing Public IP
        self.debug("Releasing Public IP %s" % public_ip.ipaddress.ipaddress)
        public_ip.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_PublicIPAddress(public_ip, network)

    @gherkin
    def when_i_create_a_lb_rule(self, public_ip, lb_tier, vm_array, vpc,
                                lb_rule_testdata,
                                ingress_acl_rule_testdata=None):
        # Creating a load balancer rule
        self.debug("Creating a LB Rule")
        lb_rule = self.create_LB_Rule(public_ip,
                                      lb_tier,
                                      vpc,
                                      vm_array=vm_array,
                                      services=lb_rule_testdata
                                      )
        self.validate_LBRule(lb_rule, state="Active")

        if ingress_acl_rule_testdata:
            # Adding Ingress Network ACL rule
            ingress_acl_rule = self.create_NetworkAclRule(
                ingress_acl_rule_testdata,
                traffic_type="Ingress",
                network=lb_tier
            )

            # VSD verification for Network ACL rule
            self.verify_vsd_firewall_rule(
                ingress_acl_rule, traffic_type="Ingress")

            self.acl_rules.append(ingress_acl_rule)
        return lb_rule

    @gherkin
    def when_i_update_lb_rule_algorithm(self, lb_rule, algorithm):
        # Updating LB rule's algorithm
        self.update_LB_Rule_algorithm(lb_rule, algorithm)
        self.validate_LBRule(lb_rule, state="Active")
        return lb_rule

    @gherkin
    def when_i_assign_vms_to_a_existing_lb_rule(self, lb_rule,
                                                vm_array=None, lb_tier=None,
                                                number_of_vms=1):
        if not vm_array:
            # Deploying VMs
            self.debug("Deploying %s VMs in network %s" %
                       (number_of_vms, lb_tier.name))
            vm_array = []
            for _ in range(number_of_vms):
                vm = self.create_VM(lb_tier)
                self.check_VM_state(vm, state="Running")

                # VSD verification
                self.verify_vsd_vm(vm)

                vm_array.append(vm)

        # Assigning VMs to a existing lb rule
        self.assign_vms_LB_Rule(lb_rule, vm_array)
        self.validate_LBRule(lb_rule, state="Active")
        return vm_array

    @gherkin
    def when_i_remove_vms_from_a_existing_lb_rule(self, lb_rule, vm_array):
        # Removing VMs from a existing lb rule
        self.remove_vms_LB_Rule(lb_rule, vm_array)
        self.validate_LBRule(lb_rule, state="Add")

    @gherkin
    def when_i_delete_a_lb_rule(self, lb_rule):
        # Deleting a load balancer rule
        self.debug("Deleting a LB Rule")
        self.delete_LB_Rule(lb_rule)
        with self.assertRaises(Exception):
            self.validate_LBRule(lb_rule, state="Active")

    @gherkin
    def when_i_stop_a_vpcInlineLbVm(self, lb_tier, vpc, lb_ips, force=False):
        expected_public_ips = [lb_ip.ipaddress.ipaddress for lb_ip in lb_ips]

        # Stopping VpcInlineLbVm appliance
        lb_vm = self.get_VpcInlineLbVm(vpc, lb_tier, expected_public_ips)
        self.stop_VpcInlineLbVm(lb_vm, force=force)

    @gherkin
    def when_i_start_a_vpcInlineLbVm(self, lb_tier, vpc, lb_ips):
        expected_public_ips = [lb_ip.ipaddress.ipaddress for lb_ip in lb_ips]

        # Starting VpcInlineLbVm appliance
        lb_vm = self.get_VpcInlineLbVm(vpc, lb_tier, expected_public_ips)
        self.start_VpcInlineLbVm(lb_vm)

    @gherkin
    def when_i_stop_vr(self, vpc, network_array, vm_array, account=None):
        if not account:
            account = self.account

        # Stopping VR
        self.debug("Stopping VR...")
        for network in network_array:
            self.validate_Network(network, state="Implemented")
            vr = self.get_Router(network)
            Router.stop(self.api_client, id=vr.id)
            self.check_Router_state(vr, state="Stopped")
        for vm in vm_array:
            self.check_VM_state(vm, state="Running")

        # VSD verification
        for network in network_array:
            self.verify_vsd_network(account.domainid, network, vpc)
            vr = self.get_Router(network)
            self.verify_vsd_router(vr, stopped=True)
        for vm in vm_array:
            self.verify_vsd_vm(vm)

    @gherkin
    def when_i_start_vr(self, vpc, network_array, vm_array, account=None):
        if not account:
            account = self.account

        # Starting VR
        self.debug("Starting VR...")
        for network in network_array:
            self.validate_Network(network, state="Implemented")
            vr = self.get_Router(network)
            Router.start(self.api_client, id=vr.id)
            self.check_Router_state(vr, state="Running")
        for vm in vm_array:
            self.check_VM_state(vm, state="Running")

        # VSD verification
        for network in network_array:
            self.verify_vsd_network(account.domainid, network, vpc)
            vr = self.get_Router(network)
            self.verify_vsd_router(vr)
        for vm in vm_array:
            self.verify_vsd_vm(vm)

    @gherkin
    def when_i_stop_vms(self, network, vpc, vm_array, account=None):
        if not account:
            account = self.account

        # Stopping VMs
        self.debug("Stopping VMs...")
        for vm in vm_array:
            vm.stop(self.api_client)
            self.check_VM_state(vm, state="Stopped")
        self.validate_Network(network, state="Implemented")
        vr = self.get_Router(network)
        self.check_Router_state(vr, state="Running")

        # VSD verification
        self.verify_vsd_network(account.domainid, network, vpc)
        self.verify_vsd_router(vr)
        for vm in vm_array:
            self.verify_vsd_vm(vm, stopped=True)

    @gherkin
    def when_i_start_vms(self, network, vpc, vm_array, account=None):
        if not account:
            account = self.account

        # Starting VMs
        self.debug("Starting VMs...")
        for vm in vm_array:
            vm.start(self.api_client)
            self.check_VM_state(vm, state="Running")
        self.validate_Network(network, state="Implemented")
        vr = self.get_Router(network)
        self.check_Router_state(vr, state="Running")

        # VSD verification
        self.verify_vsd_network(account.domainid, network, vpc)
        self.verify_vsd_router(vr)
        for vm in vm_array:
            self.verify_vsd_vm(vm)

    @gherkin
    def when_i_restart_network(self, network, vpc, vm_array, cleanup=False,
                               account=None):
        if not account:
            account = self.account

        # Restarting network without cleanup
        self.debug("Restarting network without cleanup...")
        self.restart_network(network, cleanup=cleanup)
        self.validate_Network(network, state="Implemented")
        vr = self.get_Router(network)
        self.check_Router_state(vr, state="Running")
        for vm in vm_array:
            self.check_VM_state(vm, state="Running")

        # VSD verification
        self.verify_vsd_network(account.domainid, network, vpc)
        self.verify_vsd_router(vr)
        for vm in vm_array:
            self.verify_vsd_vm(vm)

    @gherkin
    def when_i_restart_vpc(self, vpc, network_array, vm_array, cleanup=False,
                           account=None):
        if not account:
            account = self.account

        # Restarting VPC without cleanup
        self.debug("Restarting VPC without cleanup...")
        self.restart_Vpc(vpc, cleanup=cleanup)
        for network in network_array:
            self.validate_Network(network, state="Implemented")
            vr = self.get_Router(network)
            self.check_Router_state(vr, state="Running")
        for vm in vm_array:
            self.check_VM_state(vm, state="Running")

        # VSD verification
        for network in network_array:
            self.verify_vsd_network(account.domainid, network, vpc)
            vr = self.get_Router(network)
            self.verify_vsd_router(vr)
        for vm in vm_array:
            self.verify_vsd_vm(vm)

    @gherkin
    def then_all_deployed_vms_in_tier_are_fully_resolved(self, vm_array, tier,
                                                         vpc):
        # Verifying Public Load Balanced VMs ingress traffic
        # (SSH into VM via Static NAT rule)
        self.debug("Verifying Public Load Balanced VMs ingress traffic "
                   "(SSH into VM via Static NAT rule)...")
        for vm in vm_array:
            self.verify_vpc_vm_ingress_traffic(vm, tier, vpc)

    @gherkin
    def then_i_have_a_running_vpcinlinelbvm(self, lb_tier, vpc, lb_ips):
        expected_public_ips = [lb_ip.ipaddress.ipaddress for lb_ip in lb_ips]

        # Validating VpcInlineLbVm deployment and state
        lb_vm = self.get_VpcInlineLbVm(vpc, lb_tier, expected_public_ips)
        self.check_VpcInlineLbVm_state(lb_vm, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(lb_vm)
        self.verify_vsd_vm_vip(lb_vm, lb_tier)
        return lb_vm

    @gherkin
    def then_i_have_a_stopped_vpcinlinelbvm(self, lb_tier, vpc, lb_ips):
        expected_public_ips = [lb_ip.ipaddress.ipaddress for lb_ip in lb_ips]

        # Validating VpcInlineLbVm deployment and state
        lb_vm = self.get_VpcInlineLbVm(vpc, lb_tier, expected_public_ips)
        self.check_VpcInlineLbVm_state(lb_vm, state="Stopped")

        # VSD Verification
        self.verify_vsd_lb_device(lb_vm, stopped=True)
        self.verify_vsd_vm_vip(lb_vm, lb_tier)
        return lb_vm

    @gherkin
    def then_vpcinlinelbvm_is_removed(self, lb_tier, vpc, lb_ips,
                                      old_lb_vm=None):
        expected_public_ips = [lb_ip.ipaddress.ipaddress for lb_ip in lb_ips]

        # Validating VpcInlineLbVm deletion
        with self.assertRaises(Exception):
            self.get_VpcInlineLbVm(vpc, lb_tier, expected_public_ips)
        self.debug("LB appliance is successfully deleted")

        if old_lb_vm:
            # VSD Verification
            with self.assertRaises(Exception):
                self.verify_vsd_lb_device(old_lb_vm)
                self.verify_vsd_vm_vip(old_lb_vm, lb_tier)
            self.debug("LB appliance is successfully deleted in VSD")

    @gherkin
    def then_acl_rules_remain_unchanged(self, acl_rules):
        # VSD verification for Network ACL rule
        for acl_rule in acl_rules:
            self.verify_vsd_firewall_rule(acl_rule, traffic_type="Ingress")

    @gherkin
    def then_all_load_balanced_vms_are_fully_resolved(self, vm_array, lb_tier,
                                                      vpc):
        # Verifying Public Load Balanced VMs ingress traffic
        # (SSH into VM via Static NAT rule)
        self.debug("Verifying Public Load Balanced VMs ingress traffic "
                   "(SSH into VM via Static NAT rule)...")
        for vm in vm_array:
            self.verify_vpc_vm_ingress_traffic(vm, lb_tier, vpc)

    @gherkin
    def then_load_balancing_works_on(self, lb_ip, lb_rule=None, port=None,
                                     multiple_retries=False):
        if not port and lb_rule:
            port = lb_rule.publicport
        tries = 0
        retries = 120 if multiple_retries else 3
        while tries < retries:
            try:
                self.wget_from_server(lb_ip, port)
                break
            except Exception as e:
                self.debug("LB appliance and/or guest VMs are not yet fully "
                           "resolved - %s" % e)
            self.debug("Retrying...")
            time.sleep(5)
            tries += 1
        self.wget_from_server(lb_ip, port)

    @gherkin
    def then_load_balancing_works_on_all(self, lb_ip, lb_rules=[],
                                         multiple_retries=False):
        ports = []
        for lb_rule in lb_rules:
            ports.append(lb_rule.publicport)
        for port in ports:
            tries = 0
            retries = 120 if multiple_retries else 3
            while tries < retries:
                try:
                    self.wget_from_server(lb_ip, port)
                    break
                except Exception as e:
                    self.debug("LB appliance and/or guest VMs are not yet "
                               "fully resolved - %s" % e)
                self.debug("Retrying...")
                time.sleep(5)
                tries += 1
            self.wget_from_server(lb_ip, port)

    @gherkin
    def then_load_balancing_does_not_work_on(self, lb_ip, port=None,
                                             lb_rule=None):
        if not port and lb_rule:
            port = lb_rule.publicport
        with self.assertRaises(Exception):
            self.wget_from_server(lb_ip, port)

    @gherkin
    def then_load_balancing_does_not_work_on_all(self, lb_ip, lb_rules=[]):
        ports = []
        for lb_rule in lb_rules:
            ports.append(lb_rule.publicport)
        for port in ports:
            with self.assertRaises(Exception):
                self.wget_from_server(lb_ip, port)

    @gherkin
    def then_load_balancing_algorithm_works_on(self, lb_ip, vm_array,
                                               lb_rule=None, algorithm=None,
                                               port=None):
        if not port and lb_rule:
            port = lb_rule.publicport
        if not algorithm and lb_rule:
            algorithm = lb_rule.algorithm
        # Public LB (wget) traffic tests
        iterations = 2 * len(vm_array)
        wget_files = []
        for i in range(iterations):
            (filename, headers) = self.wget_from_server(lb_ip, port)
            wget_file = open(filename, "r").read()
            wget_files.append(wget_file)
        # Verifying Public LB (wget) traffic tests
        wget_servers_ip_list = []
        for i in range(iterations):
            wget_servers_ip_list.append(
                self.verify_lb_wget_file(wget_files[i], vm_array))
        # Validating Public LB algorithm
        if algorithm == "roundrobin" or algorithm == "leastconn":
            for i in range(iterations):
                if wget_servers_ip_list.count(wget_servers_ip_list[i]) \
                        is not 2:
                    self.fail("Round Robin/Least connections Public LB "
                              "algorithm validation failed - %s" %
                              wget_servers_ip_list)
            self.debug("Successfully validated Round Robin/Least connections "
                       "Public LB algorithm - %s" % wget_servers_ip_list)
        if algorithm == "source":
            for i in range(iterations):
                if wget_servers_ip_list.count(wget_servers_ip_list[i]) \
                        is not iterations:
                    self.fail("Source Public LB algorithm validation failed "
                              "- %s" % wget_servers_ip_list)

    # =========================================================================
    #                            TEST CASES
    # =========================================================================
    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_01_nuage_vpcinlinelbvm(self):
        """Test Nuage VSP VpcInlineLbVm basic functionality
        """

        # 1. Create Nuage VSP VPC offering with VpcInlineLb LB service, check
        #    if it is successfully created and enabled.
        # 2. Create a VPC with VpcInlineLb LB service, check if it is
        #    successfully created and enabled.
        # 3. Create Nuage VSP VPC Network offering with VpcInlineLb LB service,
        #    check if it is successfully created and enabled.
        # 4. Create Nuage VSP VPC Network offering without VpcInlineLb LB
        #    service, check if it is successfully created and enabled.
        # 5. Create a VPC network with VpcInlineLb LB service and spawn two
        #    VMs, check if the tier is added to the VPC VR and the VMs are
        #    deployed successfully in the tier.
        # 6. Create one more VPC network without VpcInlineLb LB service and
        #    spawn a VM, check if the tier is added to the VPC VR and the VMs
        #    are deployed successfully in the tier.
        # 7. Create a ssh load balancer rule on a public IP and attach the
        #    deployed vms to it. Check if the rule is in active state.
        # 8. Create a http load balancer rule on the above public IP and attach
        #    the deployed vms to them. Check if the rule is in active state.
        # 9. Delete all the created objects (cleanup).

        vpc = self.when_i_create_a_vpc_with_vpcinlinelb(self.vpc_off_lb)

        (lb_tier, vr, lb_vm_array) = self.when_i_create_a_tier_with_vms(
            vpc, self.net_off_lb, gateway='10.1.1.1', number_of_vms=2)

        self.then_all_deployed_vms_in_tier_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)

        (no_lb_tier, vr, no_lb_vm_array) = self.when_i_create_a_tier_with_vms(
            vpc, self.net_off_no_lb, gateway='10.1.2.1')

        self.then_all_deployed_vms_in_tier_are_fully_resolved(
            no_lb_vm_array, no_lb_tier, vpc)

        public_ip_1 = self.when_i_acquire_a_public_ip(lb_tier, vpc)

        self.when_i_create_a_lb_rule(
            public_ip_1, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrule"])

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip_1])

        self.when_i_create_a_lb_rule(
            public_ip_1, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrulehttp"])

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip_1])

        public_ip_2 = self.when_i_acquire_a_public_ip(no_lb_tier, vpc)

        with self.assertRaises(Exception):
            self.create_LB_Rule(
                public_ip_2, no_lb_tier, vpc, vm_array=no_lb_vm_array,
                services=self.test_data["vpclbrule"]
            )
        self.debug("Public LB service is not supported in the network/tier")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_02_nuage_vpcinlinelbvm_with_traffic(self):
        """Test Nuage VSP VpcInlineLbVm basic functionality with traffic
        """

        # 1. Create Nuage VSP VPC offering with VpcInlineLb LB service, check
        #    if it is successfully created and enabled.
        # 2. Create a VPC with VpcInlineLb LB service, check if it is
        #    successfully created and enabled.
        # 3. Create Nuage VSP VPC Network offering with VpcInlineLb LB service,
        #    check if it is successfully created and enabled.
        # 4. Create Nuage VSP VPC Network offering without VpcInlineLb LB
        #    service, check if it is successfully created and enabled.
        # 5. Create a VPC network with VpcInlineLb LB service and spawn two
        #    VMs, check if the tier is added to the VPC VR and the VMs are
        #    deployed successfully in the tier.
        # 6. Create one more VPC network without VpcInlineLb LB service and
        #    spawn a VM, check if the tier is added to the VPC VR and the VMs
        #    are deployed successfully in the tier.
        # 7. Create a ssh load balancer rule on a public IP and attach the
        #    deployed vms to it. Check if the rule is in active state.
        # 8. Create a http load balancer rule on the above public IP and attach
        #    the deployed vms to them. Check if the rule is in active state.
        # 9. Attach ingress acl rules for end to end traffic verification.
        # 10. Verify that the traffic to the public IP is load balanced on
        #     ssh and http ports and not on any other port.
        # 11. Delete all the created objects (cleanup).

        vpc = self.when_i_create_a_vpc_with_vpcinlinelb(self.vpc_off_lb)

        (lb_tier, vr, lb_vm_array) = self.when_i_create_a_tier_with_vms(
            vpc, self.net_off_lb, gateway='10.1.1.1', number_of_vms=2)

        self.then_all_deployed_vms_in_tier_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)

        (no_lb_tier, vr, no_lb_vm_array) = self.when_i_create_a_tier_with_vms(
            vpc, self.net_off_no_lb, gateway='10.1.2.1')

        self.then_all_deployed_vms_in_tier_are_fully_resolved(
            no_lb_vm_array, no_lb_tier, vpc)

        public_ip_1 = self.when_i_acquire_a_public_ip(lb_tier, vpc)

        lb_rule_1 = self.when_i_create_a_lb_rule(
            public_ip_1, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrule"],
            ingress_acl_rule_testdata=self.test_data["ingress_rule"])

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip_1])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on(public_ip_1, lb_rule_1)
        self.then_load_balancing_does_not_work_on(public_ip_1, port=70)

        lb_rule_2 = self.when_i_create_a_lb_rule(
            public_ip_1, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrulehttp"],
            ingress_acl_rule_testdata=self.test_data["http_rule"])

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip_1])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip_1,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip_1, port=70)

        public_ip_2 = self.when_i_acquire_a_public_ip(no_lb_tier, vpc)

        with self.assertRaises(Exception):
            self.create_LB_Rule(
                public_ip_2, no_lb_tier, vpc, vm_array=no_lb_vm_array,
                services=self.test_data["vpclbrule"]
            )
        self.debug("Public LB service is not supported in the network/tier")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_03_nuage_vpcinlinelbvm_with_multiple_public_ips_traffic(self):
        """Test Nuage VSP VpcInlineLbVm functionality with multiple public IP
        addresses by performing traffic tests
        """

        # 1. Create load balancing rules on a public ip, verify that the lb
        #    appliance is spawned and traffic is load balanced.
        # 2. Add another public ip in the lb tier with load balancing rules,
        #    verify that the same lb appliance is used to load balance traffic.
        # 3. Delete the public ip which was used first time to bring up the lb
        #    appliance, verify that the lb appliance is still present and load
        #    balances traffic for second public ip.
        # 4. Delete the second public ip, verify that the lb appliance is also
        #    destroyed.
        # 5. Re-create the load balancing rules on the public ips, verify that
        #    a new lb appliance is spawned and traffic is load balanced again.
        # 6. Delete all the created objects (cleanup).

        vpc = self.when_i_create_a_vpc_with_vpcinlinelb(self.vpc_off_lb)

        (lb_tier, vr, lb_vm_array) = self.when_i_create_a_tier_with_vms(
            vpc, self.net_off_lb, gateway='10.1.1.1', number_of_vms=2)

        self.then_all_deployed_vms_in_tier_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)

        (no_lb_tier, vr, no_lb_vm_array) = self.when_i_create_a_tier_with_vms(
            vpc, self.net_off_no_lb, gateway='10.1.2.1')

        self.then_all_deployed_vms_in_tier_are_fully_resolved(
            no_lb_vm_array, no_lb_tier, vpc)

        public_ip_1 = self.when_i_acquire_a_public_ip(lb_tier, vpc)

        lb_rule_1 = self.when_i_create_a_lb_rule(
            public_ip_1, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrule"],
            ingress_acl_rule_testdata=self.test_data["ingress_rule"])

        lb_rule_2 = self.when_i_create_a_lb_rule(
            public_ip_1, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrulehttp"],
            ingress_acl_rule_testdata=self.test_data["http_rule"])

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip_1])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip_1,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip_1, port=70)

        public_ip_2 = self.when_i_acquire_a_public_ip(lb_tier, vpc)

        lb_rule_3 = self.when_i_create_a_lb_rule(
            public_ip_2, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrule"])

        lb_rule_4 = self.when_i_create_a_lb_rule(
            public_ip_2, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrulehttp"])

        self.then_i_have_a_running_vpcinlinelbvm(
            lb_tier, vpc, [public_ip_1, public_ip_2])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip_1,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_works_on_all(public_ip_2,
                                              lb_rules=[lb_rule_3, lb_rule_4])
        self.then_load_balancing_does_not_work_on(public_ip_1, port=70)
        self.then_load_balancing_does_not_work_on(public_ip_2, port=70)

        self.when_i_release_a_public_ip(public_ip_1, lb_tier)

        lb_vm = self.then_i_have_a_running_vpcinlinelbvm(
            lb_tier, vpc, [public_ip_2])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip_2,
                                              lb_rules=[lb_rule_3, lb_rule_4])
        self.then_load_balancing_does_not_work_on_all(
            public_ip_1, lb_rules=[lb_rule_1, lb_rule_2])

        self.when_i_release_a_public_ip(public_ip_2, lb_tier)

        self.then_vpcinlinelbvm_is_removed(
            lb_tier, vpc, [public_ip_2], old_lb_vm=lb_vm)
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_load_balancing_does_not_work_on_all(
            public_ip_2, lb_rules=[lb_rule_3, lb_rule_4])

        public_ip_3 = self.when_i_acquire_a_public_ip(lb_tier, vpc)

        lb_rule_5 = self.when_i_create_a_lb_rule(
            public_ip_3, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrule"])

        lb_rule_6 = self.when_i_create_a_lb_rule(
            public_ip_3, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrulehttp"])

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip_3])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip_3,
                                              lb_rules=[lb_rule_5, lb_rule_6])
        self.then_load_balancing_does_not_work_on(public_ip_3, port=70)

        public_ip_4 = self.when_i_acquire_a_public_ip(lb_tier, vpc)

        lb_rule_7 = self.when_i_create_a_lb_rule(
            public_ip_4, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrule"])

        lb_rule_8 = self.when_i_create_a_lb_rule(
            public_ip_4, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrulehttp"])

        self.then_i_have_a_running_vpcinlinelbvm(
            lb_tier, vpc, [public_ip_3, public_ip_4])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip_3,
                                              lb_rules=[lb_rule_5, lb_rule_6])
        self.then_load_balancing_works_on_all(public_ip_4,
                                              lb_rules=[lb_rule_7, lb_rule_8])
        self.then_load_balancing_does_not_work_on(public_ip_3, port=70)
        self.then_load_balancing_does_not_work_on(public_ip_4, port=70)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_04_nuage_vpcinlinelbvm_with_lb_rule_events_traffic(self):
        """Test Nuage VSP VpcInlineLbVm functionality with lb rule events by
        performing traffic tests
        """

        # 1. Verify the VpcInlineLbVm deployment by creating the Public LB
        #    Rules and with traffic tests.
        # 2. Add guest vms to an existing lb rule, verify that the traffic is
        #    also load balanced on it.
        # 3. Remove all the vms from the lb rule, verify that the traffic
        #    stops flowing.
        # 4. Add guest vms to the above rule, verify that the traffic starts
        #    flowing and is load balanced again.
        # 5. Remove load balancing rules, verify that the traffic stops
        #    flowing and the lb appliance is also destroyed.
        # 6. Re-add the load balancing rules, verify that a new lb appliance is
        #    spawned and traffic starts flowing and is load balanced again.
        # 7. Delete all the created objects (cleanup).

        vpc = self.when_i_create_a_vpc_with_vpcinlinelb(self.vpc_off_lb)

        (lb_tier, vr, lb_vm_array) = self.when_i_create_a_tier_with_vms(
            vpc, self.net_off_lb, gateway='10.1.1.1', number_of_vms=2)

        self.then_all_deployed_vms_in_tier_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)

        (no_lb_tier, vr, no_lb_vm_array) = self.when_i_create_a_tier_with_vms(
            vpc, self.net_off_no_lb, gateway='10.1.2.1')

        self.then_all_deployed_vms_in_tier_are_fully_resolved(
            no_lb_vm_array, no_lb_tier, vpc)

        public_ip = self.when_i_acquire_a_public_ip(lb_tier, vpc)

        lb_rule_1 = self.when_i_create_a_lb_rule(
            public_ip, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrule"],
            ingress_acl_rule_testdata=self.test_data["ingress_rule"])

        lb_rule_2 = self.when_i_create_a_lb_rule(
            public_ip, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrulehttp"],
            ingress_acl_rule_testdata=self.test_data["http_rule"])

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        lb_vm_array = \
            lb_vm_array + self.when_i_assign_vms_to_a_existing_lb_rule(
                lb_rule_1, lb_tier=lb_tier, number_of_vms=2)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_remove_vms_from_a_existing_lb_rule(lb_rule_1, lb_vm_array)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on(public_ip, lb_rule_2)
        self.then_load_balancing_does_not_work_on(public_ip,
                                                  lb_rule=lb_rule_1)
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_assign_vms_to_a_existing_lb_rule(lb_rule_1, lb_vm_array)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_delete_a_lb_rule(lb_rule_1)

        lb_vm = self.then_i_have_a_running_vpcinlinelbvm(
            lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on(public_ip, lb_rule_2)
        self.then_load_balancing_does_not_work_on(public_ip,
                                                  lb_rule=lb_rule_1)
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_delete_a_lb_rule(lb_rule_2)

        self.then_vpcinlinelbvm_is_removed(
            lb_tier, vpc, [public_ip], old_lb_vm=lb_vm)
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_load_balancing_does_not_work_on_all(
            public_ip, lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        lb_rule_3 = self.when_i_create_a_lb_rule(
            public_ip, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrule"])

        lb_rule_4 = self.when_i_create_a_lb_rule(
            public_ip, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrulehttp"])

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_3, lb_rule_4])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_05_nuage_vpcinlinelbvm_with_algorithms_traffic(self):
        """Test Nuage VSP VpcInlineLbVm functionality with different LB
        algorithms by performing traffic tests
        """

        # 1. Verify the VpcInlineLbVm deployment by creating the public LB
        #    Rules and with traffic tests.
        # 2. Update the HTTP LB rule's algorithm, verify that traffic is load
        #    balanced according to the configured LB algorithm, verify
        #    this behaviour for the following LB algorithms:
        #       1. Round Robin
        #       2. Least connections
        #       3. Source
        # 3. Delete all the created objects (cleanup).

        vpc = self.when_i_create_a_vpc_with_vpcinlinelb(self.vpc_off_lb)

        (lb_tier, vr, lb_vm_array) = self.when_i_create_a_tier_with_vms(
            vpc, self.net_off_lb, gateway='10.1.1.1', number_of_vms=2)

        self.then_all_deployed_vms_in_tier_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)

        (no_lb_tier, vr, no_lb_vm_array) = self.when_i_create_a_tier_with_vms(
            vpc, self.net_off_no_lb, gateway='10.1.2.1')

        self.then_all_deployed_vms_in_tier_are_fully_resolved(
            no_lb_vm_array, no_lb_tier, vpc)

        public_ip = self.when_i_acquire_a_public_ip(lb_tier, vpc)

        lb_rule_1 = self.when_i_create_a_lb_rule(
            public_ip, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrule"],
            ingress_acl_rule_testdata=self.test_data["ingress_rule"])

        lb_rule_2 = self.when_i_create_a_lb_rule(
            public_ip, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrulehttp"],
            ingress_acl_rule_testdata=self.test_data["http_rule"])

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_algorithm_works_on(
            public_ip, lb_vm_array, lb_rule_2, algorithm="roundrobin")
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_update_lb_rule_algorithm(lb_rule_2, "leastconn")

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_algorithm_works_on(
            public_ip, lb_vm_array, lb_rule_2, algorithm="leastconn")

        self.when_i_update_lb_rule_algorithm(lb_rule_2, "source")

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_algorithm_works_on(
            public_ip, lb_vm_array, lb_rule_2, algorithm="source")

        self.when_i_update_lb_rule_algorithm(lb_rule_2, "roundrobin")

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_algorithm_works_on(
            public_ip, lb_vm_array, lb_rule_2, algorithm="roundrobin")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_06_nuage_vpcinlinelbvm_appliance_operations_traffic(self):
        """Test Nuage VSP VpcInlineLbVm functionality with VpcInlineLbVm
        appliance operations by performing traffic tests
        """

        # 1. Verify the VpcInlineLbVm deployment by creating the public LB
        #    Rules and with traffic tests.
        # 2. Stop the VpcInlineLbVm appliance, verify that the traffic stops
        #    flowing.
        # 3. Start the VpcInlineLbVm appliance, verify that it comes back up
        #    and traffic is load balanced again.
        # 4. Force stop the VpcInlineLbVm appliance, verify that the traffic
        #    stops flowing.
        # 5. Start the VpcInlineLbVm appliance, verify that it comes back up
        #    and traffic is load balanced again.
        # 4. Delete all the created objects (cleanup).

        vpc = self.when_i_create_a_vpc_with_vpcinlinelb(self.vpc_off_lb)

        (lb_tier, vr, lb_vm_array) = self.when_i_create_a_tier_with_vms(
            vpc, self.net_off_lb, gateway='10.1.1.1', number_of_vms=2)

        self.then_all_deployed_vms_in_tier_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)

        (no_lb_tier, vr, no_lb_vm_array) = self.when_i_create_a_tier_with_vms(
            vpc, self.net_off_no_lb, gateway='10.1.2.1')

        self.then_all_deployed_vms_in_tier_are_fully_resolved(
            no_lb_vm_array, no_lb_tier, vpc)

        public_ip = self.when_i_acquire_a_public_ip(lb_tier, vpc)

        lb_rule_1 = self.when_i_create_a_lb_rule(
            public_ip, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrule"],
            ingress_acl_rule_testdata=self.test_data["ingress_rule"])

        lb_rule_2 = self.when_i_create_a_lb_rule(
            public_ip, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrulehttp"],
            ingress_acl_rule_testdata=self.test_data["http_rule"])

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_stop_a_vpcInlineLbVm(lb_tier, vpc, [public_ip])

        self.then_i_have_a_stopped_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_load_balancing_does_not_work_on_all(
            public_ip, lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_start_a_vpcInlineLbVm(lb_tier, vpc, [public_ip])

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2],
                                              multiple_retries=True)
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_stop_a_vpcInlineLbVm(lb_tier, vpc, [public_ip], force=True)

        self.then_i_have_a_stopped_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_load_balancing_does_not_work_on_all(
            public_ip, lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_start_a_vpcInlineLbVm(lb_tier, vpc, [public_ip])

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2],
                                              multiple_retries=True)
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_07_nuage_vpcinlinelbvm_with_network_restarts_and_reboots(self):
        """Test Nuage VSP VpcInlineLbVm functionality with network reboots and
        restarts by performing traffic tests
        """

        # 1. Verify the VpcInlineLbVm deployment by creating the public LB
        #    Rules and with traffic tests.
        # 2. Stop the VPC VR, verify that this operation has no affect on the
        #    VpcInlineLbVm functionality.
        # 3. Start the VPC VR, verify that this operation has no affect on the
        #    VpcInlineLbVm functionality.
        # 4. Stop all the VMs configured with VpcInlineLbVm, verify that the
        #    traffic stops flowing.
        # 5. Start all the VMs configured with VpcInlineLbVm, verify that the
        #    traffic starts flowing and is is load balanced again.
        # 6. Restart tier with VpcInlineLbVm (cleanup = false), verify that the
        #    VpcInlineLbVm does not get destroyed and deployed again in the lb
        #    tier.
        # 7. Restart tier with VpcInlineLbVm (cleanup = true), verify that the
        #    VpcInlineLbVm gets destroyed and deployed again in the lb tier.
        # 8. Restart tier without VpcInlineLbVm (cleanup = false), verify that
        #    this restart has no affect on the VpcInlineLbVm functionality.
        # 9. Restart tier without VpcInlineLbVm (cleanup = true), verify that
        #    this restart has no affect on the VpcInlineLbVm functionality.
        # 10. Restart VPC (cleanup = false), verify that the VPC VR gets
        #    rebooted and this restart has no affect on the VpcInlineLbVm
        #    functionality.
        # 11. Restart VPC (cleanup = true), verify that the VPC VR gets
        #     rebooted and this restart has no affect on the VpcInlineLbVm
        #     functionality.
        # 12. Delete all the created objects (cleanup).

        vpc = self.when_i_create_a_vpc_with_vpcinlinelb(self.vpc_off_lb)

        (lb_tier, vr, lb_vm_array) = self.when_i_create_a_tier_with_vms(
            vpc, self.net_off_lb, gateway='10.1.1.1', number_of_vms=2)

        self.then_all_deployed_vms_in_tier_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)

        (no_lb_tier, vr, no_lb_vm_array) = self.when_i_create_a_tier_with_vms(
            vpc, self.net_off_no_lb, gateway='10.1.2.1')

        self.then_all_deployed_vms_in_tier_are_fully_resolved(
            no_lb_vm_array, no_lb_tier, vpc)

        public_ip = self.when_i_acquire_a_public_ip(lb_tier, vpc)

        lb_rule_1 = self.when_i_create_a_lb_rule(
            public_ip, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrule"],
            ingress_acl_rule_testdata=self.test_data["ingress_rule"])

        lb_rule_2 = self.when_i_create_a_lb_rule(
            public_ip, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrulehttp"],
            ingress_acl_rule_testdata=self.test_data["http_rule"])

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_stop_vr(
            vpc, [lb_tier, no_lb_tier], lb_vm_array+no_lb_vm_array)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_start_vr(
            vpc, [lb_tier, no_lb_tier], lb_vm_array+no_lb_vm_array)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_stop_vms(lb_tier, vpc, lb_vm_array)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_load_balancing_does_not_work_on(public_ip,
                                                  lb_rule=lb_rule_1)
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_start_vms(lb_tier, vpc, lb_vm_array)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2],
                                              multiple_retries=True)
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_restart_network(lb_tier, vpc, lb_vm_array)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2],
                                              multiple_retries=True)
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_restart_network(lb_tier, vpc, lb_vm_array, cleanup=True)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2],
                                              multiple_retries=True)
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_restart_network(no_lb_tier, vpc, no_lb_vm_array)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_restart_network(
            no_lb_tier, vpc, no_lb_vm_array, cleanup=True)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_restart_vpc(
            vpc, [lb_tier, no_lb_tier], lb_vm_array+no_lb_vm_array)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_restart_vpc(
            vpc, [lb_tier, no_lb_tier], lb_vm_array+no_lb_vm_array,
            cleanup=True)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_acl_rules_remain_unchanged(self.acl_rules)
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)
