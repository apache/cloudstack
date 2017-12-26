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

""" Component tests for VPC Internal Load Balancer functionality with
Nuage VSP SDN plugin
"""
# Import Local Modules
from nuageTestCase import nuageTestCase
from marvin.lib.base import (Account,
                             ApplicationLoadBalancer,
                             Network,
                             Router)
from marvin.cloudstackAPI import (listInternalLoadBalancerVMs,
                                  stopInternalLoadBalancerVM,
                                  startInternalLoadBalancerVM)
# Import System Modules
from nose.plugins.attrib import attr
from unittest import skip
import copy
import time


class TestNuageInternalLb(nuageTestCase):
    """Test VPC Internal LB functionality with Nuage VSP SDN plugin
    """

    @classmethod
    def setUpClass(cls):
        super(TestNuageInternalLb, cls).setUpClass()
        return

    def setUp(self):
        # Create an account
        self.account = Account.create(self.api_client,
                                      self.test_data["account"],
                                      admin=True,
                                      domainid=self.domain.id
                                      )
        self.cleanup = [self.account]
        return

    # create_Internal_LB_Rule - Creates Internal LB rule in the given
    # VPC network
    def create_Internal_LB_Rule(self, network, vm_array=None, services=None,
                                source_ip=None):
        self.debug("Creating Internal LB rule in VPC network with ID - %s" %
                   network.id)
        if not services:
            services = self.test_data["internal_lbrule"]
        int_lb_rule = ApplicationLoadBalancer.create(
            self.api_client,
            services=services,
            sourcenetworkid=network.id,
            networkid=network.id,
            sourceipaddress=source_ip
        )
        self.debug("Created Internal LB rule")
        # Assigning VMs to the created Internal Load Balancer rule
        if vm_array:
            self.debug("Assigning virtual machines - %s to the created "
                       "Internal LB rule" % vm_array)
            int_lb_rule.assign(self.api_client, vms=vm_array)
            self.debug("Assigned VMs to the created Internal LB rule")
        return int_lb_rule

    # validate_Internal_LB_Rule - Validates the given Internal LB rule,
    # matches the given Internal LB rule name and state against the list of
    # Internal LB rules fetched
    def validate_Internal_LB_Rule(self, int_lb_rule, state=None,
                                  vm_array=None):
        """Validates the Internal LB Rule"""
        self.debug("Check if the Internal LB Rule is created successfully ?")
        int_lb_rules = ApplicationLoadBalancer.list(self.api_client,
                                                    id=int_lb_rule.id
                                                    )
        self.assertEqual(isinstance(int_lb_rules, list), True,
                         "List Internal LB Rule should return a valid list"
                         )
        self.assertEqual(int_lb_rule.name, int_lb_rules[0].name,
                         "Name of the Internal LB Rule should match with the "
                         "returned list data"
                         )
        if state:
            self.assertEqual(int_lb_rules[0].loadbalancerrule[0].state, state,
                             "Internal LB Rule state should be '%s'" % state
                             )
        if vm_array:
            instance_ids = [instance.id for instance in
                            int_lb_rules[0].loadbalancerinstance]
            for vm in vm_array:
                self.assertEqual(vm.id in instance_ids, True,
                                 "Internal LB instance list should have the "
                                 "VM with ID - %s" % vm.id
                                 )
        self.debug("Internal LB Rule creation successfully validated for %s" %
                   int_lb_rule.name)

    # list_InternalLbVms - Lists deployed Internal LB VM instances
    def list_InternalLbVms(self, network_id=None, source_ip=None):
        listInternalLoadBalancerVMsCmd = \
            listInternalLoadBalancerVMs.listInternalLoadBalancerVMsCmd()
        listInternalLoadBalancerVMsCmd.account = self.account.name
        listInternalLoadBalancerVMsCmd.domainid = self.account.domainid
        if network_id:
            listInternalLoadBalancerVMsCmd.networkid = network_id
        internal_lb_vms = self.api_client.listInternalLoadBalancerVMs(
            listInternalLoadBalancerVMsCmd)
        if source_ip:
            return [internal_lb_vm for internal_lb_vm in internal_lb_vms
                    if str(internal_lb_vm.guestipaddress) == source_ip]
        else:
            return internal_lb_vms

    # get_InternalLbVm - Returns Internal LB VM instance for the given VPC
    # network and source ip
    def get_InternalLbVm(self, network, source_ip):
        self.debug("Finding the InternalLbVm for network with ID - %s and "
                   "source IP address - %s" % (network.id, source_ip))
        internal_lb_vms = self.list_InternalLbVms(network.id, source_ip)
        self.assertEqual(isinstance(internal_lb_vms, list), True,
                         "List InternalLbVms should return a valid list"
                         )
        return internal_lb_vms[0]

    # stop_InternalLbVm - Stops the given Internal LB VM instance
    def stop_InternalLbVm(self, int_lb_vm, force=False):
        self.debug("Stopping InternalLbVm with ID - %s" % int_lb_vm.id)
        cmd = stopInternalLoadBalancerVM.stopInternalLoadBalancerVMCmd()
        cmd.id = int_lb_vm.id
        if force:
            cmd.forced = force
        self.api_client.stopInternalLoadBalancerVM(cmd)

    # start_InternalLbVm - Starts the given Internal LB VM instance
    def start_InternalLbVm(self, int_lb_vm):
        self.debug("Starting InternalLbVm with ID - %s" % int_lb_vm.id)
        cmd = startInternalLoadBalancerVM.startInternalLoadBalancerVMCmd()
        cmd.id = int_lb_vm.id
        self.api_client.startInternalLoadBalancerVM(cmd)

    # check_InternalLbVm_state - Checks if the Internal LB VM instance of the
    # given VPC network and source IP is in the expected state form the list of
    # fetched Internal LB VM instances
    def check_InternalLbVm_state(self, network, source_ip, state=None):
        self.debug("Check if the InternalLbVm is in state - %s" % state)
        internal_lb_vms = self.list_InternalLbVms(network.id, source_ip)
        self.assertEqual(isinstance(internal_lb_vms, list), True,
                         "List InternalLbVm should return a valid list"
                         )
        if state:
            self.assertEqual(internal_lb_vms[0].state, state,
                             "InternalLbVm is not in the expected state"
                             )
        self.debug("InternalLbVm instance - %s is in the expected state - %s" %
                   (internal_lb_vms[0].name, state))

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

    # wget_from_vm_cmd - From within the given VM (ssh client),
    # fetches index.html file of web server running with the given public IP
    def wget_from_vm_cmd(self, ssh_client, ip_address, port):
        wget_file = ""
        cmd = "rm -rf index.html*"
        self.execute_cmd(ssh_client, cmd)
        cmd = "wget --no-cache -t 1 http://" + ip_address + ":" + str(port) + \
              "/"
        response = self.execute_cmd(ssh_client, cmd)
        if "200 OK" in response:
            self.debug("wget from a VM with http server IP address "
                       "- %s is successful" % ip_address)
            # Reading the wget file
            cmd = "cat index.html"
            wget_file = self.execute_cmd(ssh_client, cmd)
            # Removing the wget file
            cmd = "rm -rf index.html*"
            self.execute_cmd(ssh_client, cmd)
        else:
            self.debug("Failed to wget from a VM with http server IP address "
                       "- %s" % ip_address)
        return wget_file

    # verify_lb_wget_file - Verifies that the given wget file (index.html)
    # belongs to the given Internal LB rule
    # assigned VMs (vm array)
    def verify_lb_wget_file(self, wget_file, vm_array):
        wget_server_ip = None
        for vm in vm_array:
            for nic in vm.nic:
                if str(nic.ipaddress) in str(wget_file):
                    wget_server_ip = str(nic.ipaddress)
        if wget_server_ip:
            self.debug("Verified wget file from Internal Load Balanced VMs - "
                       "%s" % vm_array)
        else:
            self.fail("Failed to verify wget file from Internal Load Balanced "
                      "VMs - %s" % vm_array)
        return wget_server_ip

    # validate_internallb_algorithm_traffic - Validates Internal LB algorithms
    # by performing multiple wget traffic tests against the given Internal LB
    # VM instance (source port)
    def validate_internallb_algorithm_traffic(self, ssh_client, source_ip,
                                              port, vm_array, algorithm):
        # Internal LB (wget) traffic tests
        iterations = 2 * len(vm_array)
        wget_files = []
        for i in range(iterations):
            wget_files.append(
                self.wget_from_vm_cmd(ssh_client, source_ip, port))
        # Verifying Internal LB (wget) traffic tests
        wget_servers_ip_list = []
        for i in range(iterations):
            wget_servers_ip_list.append(
                self.verify_lb_wget_file(wget_files[i], vm_array))
        # Validating Internal LB algorithm
        if algorithm == "roundrobin" or algorithm == "leastconn":
            for i in range(iterations):
                if wget_servers_ip_list.count(wget_servers_ip_list[i]) \
                        is not 2:
                    self.fail("Round Robin Internal LB algorithm validation "
                              "failed - %s" % wget_servers_ip_list)
            self.debug("Successfully validated Round Robin/Least connections "
                       "Internal LB algorithm - %s" % wget_servers_ip_list)
        if algorithm == "source":
            for i in range(iterations):
                if wget_servers_ip_list.count(wget_servers_ip_list[i]) \
                        is not iterations:
                    self.fail("Source Internal LB algorithm validation failed "
                              "- %s" % wget_servers_ip_list)
            self.debug("Successfully validated Source Internal LB algorithm - "
                       "%s" % wget_servers_ip_list)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_01_nuage_internallb_vpc_Offering(self):
        """Test Nuage VSP VPC Offering with different combinations of LB
        service providers
        """

        # 1. Verify that the network service providers supported by Nuage VSP
        #    for VPC Internal LB functionality are all successfully created and
        #    enabled.
        # 2. Create Nuage VSP VPC offering with LB service provider as
        #    "InternalLbVm", check if it is successfully created and enabled.
        #    Verify that the VPC creation succeeds with this VPC offering.
        # 3. Create Nuage VSP VPC offering with LB service provider as
        #    "VpcVirtualRouter", check if it is successfully created and
        #    enabled. Verify that the VPC creation fails with this VPC offering
        #    as Nuage VSP does not support provider "VpcVirtualRouter" for
        #    service LB.
        # 4. Create Nuage VSP VPC offering with LB service provider as
        #    "Netscaler", check if it is successfully created and enabled.
        #    Verify that the VPC creation fails with this VPC offering as Nuage
        #    VSP does not support provider "Netscaler" for service LB.
        # 5. Delete all the created objects (cleanup).

        self.debug("Validating network service providers supported by Nuage "
                   "VSP for VPC Internal LB functionality")
        providers = ["NuageVsp", "VpcVirtualRouter", "InternalLbVm"]
        for provider in providers:
            self.validate_NetworkServiceProvider(provider, state="Enabled")

        # Creating VPC offerings
        self.debug("Creating Nuage VSP VPC offering with LB service provider "
                   "as InternalLbVm...")
        vpc_off_1 = self.create_VpcOffering(
            self.test_data["nuagevsp"]["vpc_offering_lb"])
        self.validate_VpcOffering(vpc_off_1, state="Enabled")

        self.debug("Creating Nuage VSP VPC offering with LB service provider "
                   "as VpcVirtualRouter...")
        vpc_offering_lb = copy.deepcopy(
            self.test_data["nuagevsp"]["vpc_offering_lb"])
        vpc_offering_lb["serviceProviderList"]["Lb"] = "VpcVirtualRouter"
        vpc_off_2 = self.create_VpcOffering(vpc_offering_lb)
        self.validate_VpcOffering(vpc_off_2, state="Enabled")

        self.debug("Creating Nuage VSP VPC offering with LB service provider "
                   "as Netscaler...")
        vpc_offering_lb["serviceProviderList"]["Lb"] = "Netscaler"
        vpc_off_3 = self.create_VpcOffering(vpc_offering_lb)
        self.validate_VpcOffering(vpc_off_3, state="Enabled")

        self.debug("Creating Nuage VSP VPC offering without LB service...")
        vpc_off_4 = self.create_VpcOffering(
            self.test_data["nuagevsp"]["vpc_offering"])
        self.validate_VpcOffering(vpc_off_4, state="Enabled")

        # Creating VPCs
        self.debug("Creating a VPC with LB service provider as "
                   "InternalLbVm...")
        vpc_1 = self.create_Vpc(vpc_off_1, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc_1, state="Enabled")

        self.debug("Creating a VPC with LB service provider as "
                   "VpcVirtualRouter...")
        with self.assertRaises(Exception):
            self.create_Vpc(vpc_off_2, cidr='10.1.0.0/16')
        self.debug("Nuage VSP does not support provider VpcVirtualRouter for "
                   "service LB for VPCs")

        self.debug("Creating a VPC with LB service provider as Netscaler...")
        with self.assertRaises(Exception):
            self.create_Vpc(vpc_off_3, cidr='10.1.0.0/16')
        self.debug("Nuage VSP does not support provider Netscaler for service "
                   "LB for VPCs")

        self.debug("Creating a VPC without LB service...")
        vpc_2 = self.create_Vpc(vpc_off_4, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc_2, state="Enabled")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_02_nuage_internallb_vpc_network_offering(self):
        """Test Nuage VSP VPC Network Offering with and without Internal LB
        service
        """

        # 1. Create Nuage VSP VPC Network offering with LB Service Provider as
        #    "InternalLbVm" and LB Service Capability "lbSchemes" as
        #    "internal", check if it is successfully created and enabled.
        #    Verify that the VPC network creation succeeds with this Network
        #    offering.
        # 2. Recreate above Network offering with ispersistent False, check if
        #    it is successfully created and enabled.Verify that the VPC network
        #    creation fails with this Network offering as Nuage VSP does not
        #    support non persistent VPC networks.
        # 3. Recreate above Network offering with conserve mode On, check if
        #    the network offering creation failed as only networks with
        #    conserve mode Off can belong to VPC.
        # 4. Create Nuage VSP VPC Network offering with LB Service Provider as
        #    "InternalLbVm" and LB Service Capability "lbSchemes" as "public",
        #    check if the network offering creation failed as "public" lbScheme
        #    is not supported for LB Service Provider "InternalLbVm".
        # 5. Create Nuage VSP VPC Network offering without Internal LB Service,
        #    check if it is successfully created and enabled. Verify that the
        #    VPC network creation succeeds with this Network offering.
        # 6. Recreate above Network offering with ispersistent False, check if
        #    it is successfully created and enabled. Verify that the VPC
        #    network creation fails with this Network offering as Nuage VSP
        #    does not support non persistent VPC networks.
        # 7. Recreate the above Network offering with conserve mode On, check
        #    if the network offering creation failed as only networks with
        #    conserve mode Off can belong to VPC.
        # 8. Delete all the created objects (cleanup).

        # Creating VPC offering
        self.debug("Creating Nuage VSP VPC offering with Internal LB "
                   "service...")
        vpc_off = self.create_VpcOffering(
            self.test_data["nuagevsp"]["vpc_offering_lb"])
        self.validate_VpcOffering(vpc_off, state="Enabled")

        # Creating VPC
        self.debug("Creating a VPC with Internal LB service...")
        vpc = self.create_Vpc(vpc_off, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc, state="Enabled")

        # Creating network offerings
        self.debug("Creating Nuage VSP VPC Network offering with LB Service "
                   "Provider as InternalLbVm and LB Service Capability "
                   "lbSchemes as internal...")
        net_off_1 = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering_internal_lb"])
        self.validate_NetworkOffering(net_off_1, state="Enabled")

        self.debug("Recreating above Network offering with ispersistent "
                   "False...")
        vpc_net_off_lb_non_persistent = copy.deepcopy(
            self.test_data["nuagevsp"]["vpc_network_offering_internal_lb"])
        vpc_net_off_lb_non_persistent["ispersistent"] = "False"
        net_off_2 = self.create_NetworkOffering(vpc_net_off_lb_non_persistent)
        self.validate_NetworkOffering(net_off_2, state="Enabled")

        self.debug("Recreating above Network offering with conserve mode "
                   "On...")
        with self.assertRaises(Exception):
            self.create_NetworkOffering(
                self.test_data["nuagevsp"]["vpc_network_offering_internal_lb"],
                conserve_mode=True)
        self.debug("Network offering creation failed as only networks with "
                   "conserve mode Off can belong to VPC")

        self.debug("Creating Nuage VSP VPC Network offering with LB Service "
                   "Provider as InternalLbVm and LB Service Capability "
                   "lbSchemes as public...")
        network_offering_internal_lb = copy.deepcopy(
            self.test_data["nuagevsp"]["vpc_network_offering_internal_lb"])
        service_list = network_offering_internal_lb["serviceCapabilityList"]
        service_list["Lb"]["lbSchemes"] = "public"
        network_offering_internal_lb["serviceCapabilityList"] = service_list
        with self.assertRaises(Exception):
            self.create_NetworkOffering(network_offering_internal_lb)
        self.debug("Network offering creation failed as public lbScheme is "
                   "not supported for LB Service Provider InternalLbVm")

        self.debug("Creating Nuage VSP VPC Network offering without Internal "
                   "LB service...")
        net_off_3 = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        self.validate_NetworkOffering(net_off_3, state="Enabled")

        self.debug("Recreating above Network offering with ispersistent "
                   "False...")
        vpc_net_off_non_persistent = copy.deepcopy(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        vpc_net_off_non_persistent["ispersistent"] = "False"
        net_off_4 = self.create_NetworkOffering(vpc_net_off_non_persistent)
        self.validate_NetworkOffering(net_off_4, state="Enabled")

        self.debug("Recreating above Network offering with conserve mode "
                   "On...")
        with self.assertRaises(Exception):
            self.create_NetworkOffering(
                self.test_data["nuagevsp"]["vpc_network_offering"],
                conserve_mode=True)
        self.debug("Network offering creation failed as only networks with "
                   "conserve mode Off can belong to VPC")

        # Creating VPC networks in the VPC
        self.debug("Creating a persistent VPC network with Internal LB "
                   "service...")
        internal_tier = self.create_Network(
            net_off_1, gateway='10.1.1.1', vpc=vpc)
        self.validate_Network(internal_tier, state="Implemented")
        vr = self.get_Router(internal_tier)
        self.check_Router_state(vr, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, internal_tier, vpc)
        self.verify_vsd_router(vr)

        self.debug("Creating a non persistent VPC network with Internal LB "
                   "service...")
        with self.assertRaises(Exception):
            self.create_Network(net_off_2, gateway='10.1.2.1', vpc=vpc)
        self.debug("Nuage VSP does not support non persistent VPC networks")

        self.debug("Creating a persistent VPC network without Internal LB "
                   "service...")
        public_tier = self.create_Network(
            net_off_3, gateway='10.1.3.1', vpc=vpc)
        self.validate_Network(public_tier, state="Implemented")
        vr = self.get_Router(public_tier)
        self.check_Router_state(vr, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, public_tier, vpc)
        self.verify_vsd_router(vr)

        self.debug("Creating a non persistent VPC network without Internal LB "
                   "service...")
        with self.assertRaises(Exception):
            self.create_Network(net_off_4, gateway='10.1.4.1', vpc=vpc)
        self.debug("Nuage VSP does not support non persistent VPC networks")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_03_nuage_internallb_vpc_networks(self):
        """Test Nuage VSP VPC Networks with and without Internal LB service
        """

        # 1. Create Nuage VSP VPC offering with Internal LB service, check if
        #    it is successfully created and enabled.
        # 2. Create Nuage VSP VPC offering without Internal LB service, check
        #    if it is successfully created and enabled.
        # 3. Create a VPC "vpc_1" with Internal LB service, check if it is
        #    successfully created and enabled.
        # 4. Create a VPC "vpc_2" without Internal LB service, check if it is
        #    successfully created and enabled.
        # 5. Create Nuage VSP VPC Network offering with Internal LB service,
        #    check if it is successfully created and enabled.
        # 6. Create Nuage VSP VPC Network offering without Internal LB service,
        #    check if it is successfully created and enabled.
        # 7. Create a VPC network in vpc_1 with Internal LB service and spawn a
        #    VM, check if the tier is added to the VPC VR, and the VM is
        #    deployed successfully in the tier.
        # 8. Create one more VPC network in vpc_1 with Internal LB service and
        #    spawn a VM, check if the tier is added to the VPC VR, and the VM
        #    is deployed successfully in the tier.
        # 9. Create a VPC network in vpc_2 with Internal LB service, check if
        #    the tier creation failed.
        # 10. Create a VPC network in vpc_1 without Internal LB service and
        #     spawn a VM, check if the tier is added to the VPC VR, and the VM
        #     is deployed successfully in the tier.
        # 11. Create a VPC network in vpc_2 without Internal LB service and
        #     spawn a VM, check if the tier is added to the VPC VR, and the VM
        #     is deployed successfully in the tier.
        # 12. Upgrade the VPC network with Internal LB service to one with no
        #     Internal LB service and vice-versa, check if the VPC Network
        #     offering upgrade passed in both directions.
        # 13. Delete the VPC network with Internal LB service, check if the
        #     tier is successfully deleted.
        # 14. Recreate the VPC network with Internal LB service, check if the
        #     tier is successfully re-created.
        # 15. Delete all the created objects (cleanup).

        # Creating VPC offerings
        self.debug("Creating Nuage VSP VPC offering with Internal LB "
                   "service...")
        vpc_off_1 = self.create_VpcOffering(
            self.test_data["nuagevsp"]["vpc_offering_lb"])
        self.validate_VpcOffering(vpc_off_1, state="Enabled")

        self.debug("Creating Nuage VSP VPC offering without Internal LB "
                   "service...")
        vpc_off_2 = self.create_VpcOffering(
            self.test_data["nuagevsp"]["vpc_offering"])
        self.validate_VpcOffering(vpc_off_2, state="Enabled")

        # Creating VPCs
        self.debug("Creating a VPC with Internal LB service...")
        vpc_1 = self.create_Vpc(vpc_off_1, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc_1, state="Enabled")

        self.debug("Creating a VPC without Internal LB service...")
        vpc_2 = self.create_Vpc(vpc_off_2, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc_2, state="Enabled")

        # Creating network offerings
        self.debug("Creating Nuage VSP VPC Network offering with Internal LB "
                   "service...")
        net_off_1 = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering_internal_lb"])
        self.validate_NetworkOffering(net_off_1, state="Enabled")

        self.debug("Creating Nuage VSP VPC Network offering without Internal "
                   "LB service...")
        net_off_2 = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        self.validate_NetworkOffering(net_off_2, state="Enabled")

        # Creating VPC networks in VPCs, and deploying VMs
        self.debug("Creating a VPC network in vpc_1 with Internal LB "
                   "service...")
        internal_tier_1 = self.create_Network(
            net_off_1, gateway='10.1.1.1', vpc=vpc_1)
        self.validate_Network(internal_tier_1, state="Implemented")
        vr_1 = self.get_Router(internal_tier_1)
        self.check_Router_state(vr_1, state="Running")

        self.debug("Deploying a VM in the created VPC network...")
        internal_vm_1 = self.create_VM(internal_tier_1)
        self.check_VM_state(internal_vm_1, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, internal_tier_1, vpc_1)
        self.verify_vsd_router(vr_1)
        self.verify_vsd_vm(internal_vm_1)

        self.debug("Creating one more VPC network in vpc_1 with Internal LB "
                   "service...")
        internal_tier_2 = self.create_Network(
            net_off_1, gateway='10.1.2.1', vpc=vpc_1)
        self.validate_Network(internal_tier_2, state="Implemented")
        vr_1 = self.get_Router(internal_tier_2)
        self.check_Router_state(vr_1, state="Running")

        self.debug("Deploying a VM in the created VPC network...")
        internal_vm_2 = self.create_VM(internal_tier_2)
        self.check_VM_state(internal_vm_2, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, internal_tier_2, vpc_1)
        self.verify_vsd_router(vr_1)
        self.verify_vsd_vm(internal_vm_2)

        self.debug("Creating a VPC network in vpc_2 with Internal LB "
                   "service...")
        with self.assertRaises(Exception):
            self.create_Network(net_off_1, gateway='10.1.1.1', vpc=vpc_2)
        self.debug("VPC Network creation failed as vpc_2 does not support "
                   "Internal Lb service")

        self.debug("Creating a VPC network in vpc_1 without Internal LB "
                   "service...")
        public_tier_1 = self.create_Network(
            net_off_2, gateway='10.1.3.1', vpc=vpc_1)
        self.validate_Network(public_tier_1, state="Implemented")
        vr_1 = self.get_Router(public_tier_1)
        self.check_Router_state(vr_1, state="Running")

        self.debug("Deploying a VM in the created VPC network...")
        public_vm_1 = self.create_VM(public_tier_1)
        self.check_VM_state(public_vm_1, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, public_tier_1, vpc_1)
        self.verify_vsd_router(vr_1)
        self.verify_vsd_vm(public_vm_1)

        self.debug("Creating a VPC network in vpc_2 without Internal LB "
                   "service...")
        public_tier_2 = self.create_Network(
            net_off_2, gateway='10.1.1.1', vpc=vpc_2)
        self.validate_Network(public_tier_2, state="Implemented")
        vr_2 = self.get_Router(public_tier_2)
        self.check_Router_state(vr_2, state="Running")

        self.debug("Deploying a VM in the created VPC network...")
        public_vm_2 = self.create_VM(public_tier_2)
        self.check_VM_state(public_vm_2, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, public_tier_2, vpc_2)
        self.verify_vsd_router(vr_2)
        self.verify_vsd_vm(public_vm_2)

        # Upgrading a VPC network
        self.debug("Upgrading a VPC network with Internal LB Service to one "
                   "without Internal LB Service...")
        self.upgrade_Network(net_off_2, internal_tier_2)
        self.validate_Network(internal_tier_2, state="Implemented")
        vr_1 = self.get_Router(internal_tier_2)
        self.check_Router_state(vr_1, state="Running")
        self.check_VM_state(internal_vm_2, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, internal_tier_2, vpc_1)
        self.verify_vsd_router(vr_1)
        self.verify_vsd_vm(internal_vm_2)

        self.debug("Upgrading a VPC network without Internal LB Service to "
                   "one with Internal LB Service...")
        self.upgrade_Network(net_off_1, internal_tier_2)
        self.validate_Network(internal_tier_2, state="Implemented")
        vr_1 = self.get_Router(internal_tier_2)
        self.check_Router_state(vr_1, state="Running")
        self.check_VM_state(internal_vm_2, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, internal_tier_2, vpc_1)
        self.verify_vsd_router(vr_1)
        self.verify_vsd_vm(internal_vm_2)

        # Deleting and re-creating a VPC network
        self.debug("Deleting a VPC network with Internal LB Service...")
        self.delete_VM(internal_vm_2)
        self.delete_Network(internal_tier_2)
        with self.assertRaises(Exception):
            self.validate_Network(internal_tier_2)
        self.debug("VPC network successfully deleted in CloudStack")

        # VSD verification
        with self.assertRaises(Exception):
            self.verify_vsd_network(self.domain.id, internal_tier_2, vpc_1)
        self.debug("VPC network successfully deleted in VSD")

        self.debug("Recreating a VPC network with Internal LB Service...")
        internal_tier_2 = self.create_Network(
            net_off_1, gateway='10.1.2.1', vpc=vpc_1)
        internal_vm_2 = self.create_VM(internal_tier_2)
        self.validate_Network(internal_tier_2, state="Implemented")
        vr_1 = self.get_Router(internal_tier_2)
        self.check_Router_state(vr_1, state="Running")
        self.check_VM_state(internal_vm_2, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, internal_tier_2, vpc_1)
        self.verify_vsd_router(vr_1)
        self.verify_vsd_vm(internal_vm_2)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_04_nuage_internallb_rules(self):
        """Test Nuage VSP VPC Internal LB functionality with different
        combinations of Internal LB rules
        """

        # 1. Create an Internal LB Rule with source IP Address specified, check
        #    if the Internal LB Rule is successfully created.
        # 2. Create an Internal LB Rule without source IP Address specified,
        #    check if the Internal LB Rule is successfully created.
        # 3. Create an Internal LB Rule when the specified source IP Address is
        #    outside the VPC network (tier) CIDR range, check if the Internal
        #    LB Rule creation failed as the requested source IP is not in the
        #    network's CIDR subnet.
        # 4. Create an Internal LB Rule when the specified source IP Address is
        #    outside the VPC super CIDR range, check if the Internal LB Rule
        #    creation failed as the requested source IP is not in the network's
        #    CIDR subnet.
        # 5. Create an Internal LB Rule in the tier with LB service provider as
        #    VpcInlineLbVm, check if the Internal LB Rule creation failed as
        #    Scheme Internal is not supported by this network offering.
        # 6. Create multiple Internal LB Rules using different Load Balancing
        #    source IP Addresses, check if the Internal LB Rules are
        #    successfully created.
        # 7. Create multiple Internal LB Rules with different ports but using
        #    the same Load Balancing source IP Address, check if the Internal
        #    LB Rules are successfully created.
        # 8. Create multiple Internal LB Rules with same ports and using the
        #    same Load Balancing source IP Address, check if the second
        #    Internal LB Rule creation failed as it conflicts with the first
        #    Internal LB rule.
        # 9. Attach a VM to the above created Internal LB Rules, check if the
        #    VM is successfully attached to the Internal LB Rules.
        # 10. Verify the InternalLbVm deployment after successfully creating
        #     the first Internal LB Rule and attaching a VM to it.
        # 11. Verify the failure of attaching a VM from a different tier to an
        #     Internal LB Rule created on a tier.
        # 12. Delete the above created Internal LB Rules, check if the Internal
        #     LB Rules are successfully deleted.
        # 13. Delete all the created objects (cleanup).

        # Creating a VPC offering
        self.debug("Creating Nuage VSP VPC offering with Internal LB "
                   "service...")
        vpc_off = self.create_VpcOffering(
            self.test_data["nuagevsp"]["vpc_offering_lb"])
        self.validate_VpcOffering(vpc_off, state="Enabled")

        # Creating a VPC
        self.debug("Creating a VPC with Internal LB service...")
        vpc = self.create_Vpc(vpc_off, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc, state="Enabled")

        # Creating network offerings
        self.debug("Creating Nuage VSP VPC Network offering with Internal LB "
                   "service...")
        net_off_1 = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering_internal_lb"])
        self.validate_NetworkOffering(net_off_1, state="Enabled")

        self.debug("Creating Nuage VSP VPC Network offering without Internal "
                   "LB service...")
        net_off_2 = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        self.validate_NetworkOffering(net_off_2, state="Enabled")

        # Creating VPC networks in the VPC, and deploying VMs
        self.debug("Creating a VPC network with Internal LB service...")
        internal_tier = self.create_Network(
            net_off_1, gateway='10.1.1.1', vpc=vpc)
        self.validate_Network(internal_tier, state="Implemented")
        vr = self.get_Router(internal_tier)
        self.check_Router_state(vr, state="Running")

        self.debug("Deploying a VM in the created VPC network...")
        internal_vm = self.create_VM(internal_tier)
        self.check_VM_state(internal_vm, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, internal_tier, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(internal_vm)

        self.debug("Creating a VPC network without Internal LB service...")
        public_tier = self.create_Network(
            net_off_2, gateway='10.1.2.1', vpc=vpc)
        self.validate_Network(public_tier, state="Implemented")
        vr = self.get_Router(public_tier)
        self.check_Router_state(vr, state="Running")

        self.debug("Deploying a VM in the created VPC network...")
        public_vm = self.create_VM(public_tier)
        self.check_VM_state(public_vm, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, public_tier, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(public_vm)

        # Creating Internal LB Rules
        self.debug("Creating an Internal LB Rule without source IP Address "
                   "specified...")
        int_lb_rule = self.create_Internal_LB_Rule(internal_tier)
        self.validate_Internal_LB_Rule(int_lb_rule, state="Add")

        # Validating InternalLbVm deployment
        with self.assertRaises(Exception):
            self.check_InternalLbVm_state(
                internal_tier, int_lb_rule.sourceipaddress)
        self.debug("InternalLbVm is not deployed in the network as there are "
                   "no VMs assigned to this Internal LB Rule")

        self.debug('Deleting the Internal LB Rule - %s' % int_lb_rule.name)
        int_lb_rule.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_Internal_LB_Rule(int_lb_rule)
        self.debug("Internal LB Rule successfully deleted in CloudStack")

        free_source_ip = int_lb_rule.sourceipaddress

        self.debug("Creating an Internal LB Rule with source IP Address "
                   "specified...")
        int_lb_rule = self.create_Internal_LB_Rule(
            internal_tier, source_ip=free_source_ip)
        self.validate_Internal_LB_Rule(int_lb_rule, state="Add")

        # Validating InternalLbVm deployment
        with self.assertRaises(Exception):
            self.check_InternalLbVm_state(
                internal_tier, int_lb_rule.sourceipaddress)
        self.debug("InternalLbVm is not deployed in the network as there are "
                   "no VMs assigned to this Internal LB Rule")

        self.debug('Deleting the Internal LB Rule - %s' % int_lb_rule.name)
        int_lb_rule.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_Internal_LB_Rule(int_lb_rule)
        self.debug("Internal LB Rule successfully deleted in CloudStack")

        self.debug("Creating an Internal LB Rule when the specified source IP "
                   "Address is outside the VPC network CIDR range...")
        with self.assertRaises(Exception):
            self.create_Internal_LB_Rule(internal_tier, source_ip="10.1.1.256")
        self.debug("Internal LB Rule creation failed as the requested IP is "
                   "not in the network's CIDR subnet")

        self.debug("Creating an Internal LB Rule when the specified source IP "
                   "Address is outside the VPC super CIDR range...")
        with self.assertRaises(Exception):
            self.create_Internal_LB_Rule(internal_tier, source_ip="10.2.1.256")
        self.debug("Internal LB Rule creation failed as the requested IP is "
                   "not in the network's CIDR subnet")

        self.debug("Creating an Internal LB Rule in a VPC network without "
                   "Internal Lb service...")
        with self.assertRaises(Exception):
            self.create_Internal_LB_Rule(public_tier)
        self.debug("Internal LB Rule creation failed as Scheme Internal is "
                   "not supported by this network offering")

        self.debug("Creating multiple Internal LB Rules using different Load "
                   "Balancing source IP Addresses...")
        int_lb_rule_1 = self.create_Internal_LB_Rule(
            internal_tier, vm_array=[internal_vm])
        self.validate_Internal_LB_Rule(
            int_lb_rule_1, state="Active", vm_array=[internal_vm])
        int_lb_rule_2 = self.create_Internal_LB_Rule(
            internal_tier, vm_array=[internal_vm])
        self.validate_Internal_LB_Rule(
            int_lb_rule_2, state="Active", vm_array=[internal_vm])

        # Validating InternalLbVms deployment and state
        int_lb_vm_1 = self.get_InternalLbVm(
            internal_tier, int_lb_rule_1.sourceipaddress)
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Running")
        int_lb_vm_2 = self.get_InternalLbVm(
            internal_tier, int_lb_rule_2.sourceipaddress)
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_2.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm_1)
        self.verify_vsd_lb_device(int_lb_vm_2)

        self.debug('Removing VMs from the Internal LB Rules - %s, %s' %
                   (int_lb_rule_1.name, int_lb_rule_2.name))
        int_lb_rule_1.remove(self.api_client, vms=[internal_vm])
        with self.assertRaises(Exception):
            self.validate_Internal_LB_Rule(
                int_lb_rule_1, vm_array=[internal_vm])
        self.debug("VMs successfully removed from the Internal LB Rule in "
                   "CloudStack")
        int_lb_rule_2.remove(self.api_client, vms=[internal_vm])
        with self.assertRaises(Exception):
            self.validate_Internal_LB_Rule(
                int_lb_rule_2, vm_array=[internal_vm])
        self.debug("VMs successfully removed from the Internal LB Rule in "
                   "CloudStack")

        # Validating InternalLbVms state
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Running")
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_2.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm_1)
        self.verify_vsd_lb_device(int_lb_vm_2)

        self.debug('Deleting the Internal LB Rules - %s, %s' %
                   (int_lb_rule_1.name, int_lb_rule_2.name))
        int_lb_rule_1.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_Internal_LB_Rule(int_lb_rule_1)
        self.debug("Internal LB Rule successfully deleted in CloudStack")
        int_lb_rule_2.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_Internal_LB_Rule(int_lb_rule_2)
        self.debug("Internal LB Rule successfully deleted in CloudStack")

        # Validating InternalLbVms un-deployment
        with self.assertRaises(Exception):
            self.check_InternalLbVm_state(
                internal_tier, int_lb_rule_1.sourceipaddress)
        self.debug("InternalLbVm successfully destroyed in CloudStack")
        with self.assertRaises(Exception):
            self.check_InternalLbVm_state(
                internal_tier, int_lb_rule_2.sourceipaddress)
        self.debug("InternalLbVm successfully destroyed in CloudStack")

        # VSD Verification
        with self.assertRaises(Exception):
            self.verify_vsd_lb_device(int_lb_vm_1)
        self.debug("InternalLbVm successfully destroyed in VSD")
        with self.assertRaises(Exception):
            self.verify_vsd_lb_device(int_lb_vm_2)
        self.debug("InternalLbVm successfully destroyed in VSD")

        self.debug("Creating multiple Internal LB Rules with different ports "
                   "but using the same Load Balancing source IP Address...")
        int_lb_rule_1 = self.create_Internal_LB_Rule(
            internal_tier, vm_array=[internal_vm])
        self.validate_Internal_LB_Rule(
            int_lb_rule_1, state="Active", vm_array=[internal_vm])
        int_lb_rule_2 = self.create_Internal_LB_Rule(
            internal_tier, vm_array=[internal_vm],
            services=self.test_data["internal_lbrule_http"],
            source_ip=int_lb_rule_1.sourceipaddress)
        self.validate_Internal_LB_Rule(
            int_lb_rule_2, state="Active", vm_array=[internal_vm])

        # Validating InternalLbVm deployment and state
        int_lb_vm = self.get_InternalLbVm(
            internal_tier, int_lb_rule_1.sourceipaddress)
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm)

        self.debug('Removing VMs from the Internal LB Rules - %s, %s' %
                   (int_lb_rule_1.name, int_lb_rule_2.name))
        int_lb_rule_1.remove(self.api_client, vms=[internal_vm])
        with self.assertRaises(Exception):
            self.validate_Internal_LB_Rule(
                int_lb_rule_1, vm_array=[internal_vm])
        self.debug("VMs successfully removed from the Internal LB Rule in "
                   "CloudStack")
        int_lb_rule_2.remove(self.api_client, vms=[internal_vm])
        with self.assertRaises(Exception):
            self.validate_Internal_LB_Rule(
                int_lb_rule_2, vm_array=[internal_vm])
        self.debug("VMs successfully removed from the Internal LB Rule in "
                   "CloudStack")

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm)

        self.debug('Deleting the Internal LB Rules - %s, %s' %
                   (int_lb_rule_1.name, int_lb_rule_2.name))
        int_lb_rule_1.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_Internal_LB_Rule(int_lb_rule_1)
        self.debug("Internal LB Rule successfully deleted in CloudStack")
        int_lb_rule_2.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_Internal_LB_Rule(int_lb_rule_2)
        self.debug("Internal LB Rule successfully deleted in CloudStack")

        # Validating InternalLbVm un-deployment
        with self.assertRaises(Exception):
            self.check_InternalLbVm_state(
                internal_tier, int_lb_rule_1.sourceipaddress)
        self.debug("InternalLbVm successfully destroyed in CloudStack")

        # VSD Verification
        with self.assertRaises(Exception):
            self.verify_vsd_lb_device(int_lb_vm)
        self.debug("InternalLbVm successfully destroyed in VSD")

        self.debug("Creating multiple Internal LB Rules with same ports and "
                   "using the same Load Balancing source IP Address...")
        int_lb_rule = self.create_Internal_LB_Rule(
            internal_tier, vm_array=[internal_vm])
        self.validate_Internal_LB_Rule(
            int_lb_rule, state="Active", vm_array=[internal_vm])
        with self.assertRaises(Exception):
            self.create_Internal_LB_Rule(
                internal_tier, vm_array=[internal_vm],
                source_ip=int_lb_rule.sourceipaddress)
        self.debug("Internal LB Rule creation failed as it conflicts with the "
                   "existing rule")

        # Validating InternalLbVm deployment and state
        int_lb_vm = self.get_InternalLbVm(
            internal_tier, int_lb_rule.sourceipaddress)
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm)

        self.debug('Removing VMs from the Internal LB Rule - %s' %
                   int_lb_rule.name)
        int_lb_rule.remove(self.api_client, vms=[internal_vm])
        with self.assertRaises(Exception):
            self.validate_Internal_LB_Rule(int_lb_rule, vm_array=[internal_vm])
        self.debug("VMs successfully removed from the Internal LB Rule in "
                   "CloudStack")

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm)

        self.debug('Deleting the Internal LB Rule - %s' % int_lb_rule.name)
        int_lb_rule.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_Internal_LB_Rule(int_lb_rule)
        self.debug("Internal LB Rule successfully deleted in CloudStack")

        # Validating InternalLbVm un-deployment
        with self.assertRaises(Exception):
            self.check_InternalLbVm_state(
                internal_tier, int_lb_rule.sourceipaddress)
        self.debug("InternalLbVm successfully destroyed in CloudStack")

        # VSD Verification
        with self.assertRaises(Exception):
            self.verify_vsd_lb_device(int_lb_vm)
        self.debug("InternalLbVm successfully destroyed in VSD")

        self.debug("Attaching a VM from a different tier to an Internal LB "
                   "Rule created on a tier...")
        with self.assertRaises(Exception):
            self.create_Internal_LB_Rule(internal_tier, vm_array=[public_vm])
        self.debug("Internal LB Rule creation failed as the VM belongs to a "
                   "different network")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_05_nuage_internallb_traffic(self):
        """Test Nuage VSP VPC Internal LB functionality by performing (wget)
        traffic tests within a VPC
        """

        # 1. Create three different Internal LB Rules with a single source IP
        #    Address specified on the Internal tier, check if the Internal LB
        #    Rules are created successfully.
        # 2. Attach a VM to the above created Internal LB Rules, check if the
        #    InternalLbVm is successfully deployed in the Internal tier.
        # 3. Deploy two more VMs in the Internal tier, check if the VMs are
        #    successfully deployed.
        # 4. Attach the newly deployed VMs to the above created Internal LB
        #    Rules, verify the validity of the above created Internal LB Rules
        #    over three Load Balanced VMs in the Internal tier.
        # 5. Create the corresponding Network ACL rules to make the created
        #    Internal LB rules (SSH & HTTP) accessible, check if the Network
        #    ACL rules are successfully added to the internal tier.
        # 6. Validate the Internal LB functionality by performing (wget)
        #    traffic tests from a VM in the Public tier to the Internal load
        #    balanced guest VMs in the Internal tier, using Static NAT
        #    functionality to access (ssh) the VM on the Public tier.
        # 7. Verify that the InternalLbVm gets destroyed when the last Internal
        #    LB rule is removed from the Internal tier.
        # 8. Repeat the above steps for one more Internal tier as well,
        #    validate the Internal LB functionality.
        # 9. Delete all the created objects (cleanup).

        # Creating a VPC offering
        self.debug("Creating Nuage VSP VPC offering with Internal LB "
                   "service...")
        vpc_off = self.create_VpcOffering(
            self.test_data["nuagevsp"]["vpc_offering_lb"])
        self.validate_VpcOffering(vpc_off, state="Enabled")

        # Creating a VPC
        self.debug("Creating a VPC with Internal LB service...")
        vpc = self.create_Vpc(vpc_off, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc, state="Enabled")

        # Creating network offerings
        self.debug("Creating Nuage VSP VPC Network offering with Internal LB "
                   "service...")
        net_off_1 = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering_internal_lb"])
        self.validate_NetworkOffering(net_off_1, state="Enabled")

        self.debug("Creating Nuage VSP VPC Network offering without Internal "
                   "LB service...")
        net_off_2 = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        self.validate_NetworkOffering(net_off_2, state="Enabled")

        # Creating VPC networks in the VPC, and deploying VMs
        self.debug("Creating a VPC network with Internal LB service...")
        internal_tier_1 = self.create_Network(
            net_off_1, gateway='10.1.1.1', vpc=vpc)
        self.validate_Network(internal_tier_1, state="Implemented")
        vr = self.get_Router(internal_tier_1)
        self.check_Router_state(vr, state="Running")

        self.debug("Deploying a VM in the created VPC network...")
        internal_vm_1 = self.create_VM(internal_tier_1)
        self.check_VM_state(internal_vm_1, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, internal_tier_1, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(internal_vm_1)

        self.debug("Creating one more VPC network with Internal LB service...")
        internal_tier_2 = self.create_Network(
            net_off_1, gateway='10.1.2.1', vpc=vpc)
        self.validate_Network(internal_tier_2, state="Implemented")
        vr = self.get_Router(internal_tier_2)
        self.check_Router_state(vr, state="Running")

        self.debug("Deploying a VM in the created VPC network...")
        internal_vm_2 = self.create_VM(internal_tier_2)
        self.check_VM_state(internal_vm_2, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, internal_tier_2, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(internal_vm_2)

        self.debug("Creating a VPC network without Internal LB service...")
        public_tier = self.create_Network(
            net_off_2, gateway='10.1.3.1', vpc=vpc)
        self.validate_Network(public_tier, state="Implemented")
        vr = self.get_Router(public_tier)
        self.check_Router_state(vr, state="Running")

        self.debug("Deploying a VM in the created VPC network...")
        public_vm = self.create_VM(public_tier)
        self.check_VM_state(public_vm, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, public_tier, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(public_vm)

        # Creating Internal LB Rules in the Internal tiers
        self.debug("Creating three Internal LB Rules (SSH & HTTP) using the "
                   "same Load Balancing source IP Address...")
        int_lb_rule_1 = self.create_Internal_LB_Rule(
            internal_tier_1, vm_array=[internal_vm_1])
        self.validate_Internal_LB_Rule(
            int_lb_rule_1, state="Active", vm_array=[internal_vm_1])
        int_lb_rule_2 = self.create_Internal_LB_Rule(
            internal_tier_1, vm_array=[internal_vm_1],
            services=self.test_data["internal_lbrule_http"],
            source_ip=int_lb_rule_1.sourceipaddress)
        self.validate_Internal_LB_Rule(
            int_lb_rule_2, state="Active", vm_array=[internal_vm_1])
        internal_lbrule_http = copy.deepcopy(
            self.test_data["internal_lbrule_http"])
        internal_lbrule_http["sourceport"] = 8080
        internal_lbrule_http["instanceport"] = 8080
        int_lb_rule_3 = self.create_Internal_LB_Rule(
            internal_tier_1,
            vm_array=[internal_vm_1],
            services=internal_lbrule_http,
            source_ip=int_lb_rule_1.sourceipaddress)
        self.validate_Internal_LB_Rule(
            int_lb_rule_3, state="Active", vm_array=[internal_vm_1])

        # Validating InternalLbVm deployment and state
        int_lb_vm_1 = self.get_InternalLbVm(
            internal_tier_1, int_lb_rule_1.sourceipaddress)
        self.check_InternalLbVm_state(
            internal_tier_1, int_lb_rule_1.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm_1)

        # Deploying more VMs in the Internal tier
        self.debug("Deploying two more VMs in network - %s" %
                   internal_tier_1.name)
        internal_vm_1_1 = self.create_VM(internal_tier_1)
        internal_vm_1_2 = self.create_VM(internal_tier_1)

        # VSD verification
        self.verify_vsd_vm(internal_vm_1_1)
        self.verify_vsd_vm(internal_vm_1_2)

        # Adding newly deployed VMs to the created Internal LB rules
        self.debug("Adding two more virtual machines to the created Internal "
                   "LB rules...")
        int_lb_rule_1.assign(
            self.api_client, [internal_vm_1_1, internal_vm_1_2])
        self.validate_Internal_LB_Rule(
            int_lb_rule_1, state="Active",
            vm_array=[internal_vm_1, internal_vm_1_1, internal_vm_1_2])
        int_lb_rule_2.assign(
            self.api_client, [internal_vm_1_1, internal_vm_1_2])
        self.validate_Internal_LB_Rule(
            int_lb_rule_2, state="Active",
            vm_array=[internal_vm_1, internal_vm_1_1, internal_vm_1_2])
        int_lb_rule_3.assign(
            self.api_client, [internal_vm_1_1, internal_vm_1_2])
        self.validate_Internal_LB_Rule(
            int_lb_rule_3, state="Active",
            vm_array=[internal_vm_1, internal_vm_1_1, internal_vm_1_2])

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(
            internal_tier_1, int_lb_rule_1.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm_1)

        # Adding Network ACL rules in the Internal tier
        self.debug("Adding Network ACL rules to make the created Internal LB "
                   "rules (HTTP) accessible...")
        http_rule_1 = self.create_NetworkAclRule(
            self.test_data["http_rule"], network=internal_tier_1)
        http_rule = copy.deepcopy(self.test_data["http_rule"])
        http_rule["privateport"] = 8080
        http_rule["publicport"] = 8080
        http_rule["startport"] = 8080
        http_rule["endport"] = 8080
        http_rule_2 = self.create_NetworkAclRule(
            http_rule, network=internal_tier_1)

        # VSD verification
        self.verify_vsd_firewall_rule(http_rule_1)
        self.verify_vsd_firewall_rule(http_rule_2)

        # Creating Internal LB Rules in the Internal tier
        self.debug("Creating three Internal LB Rules (SSH & HTTP) using the "
                   "same Load Balancing source IP Address...")
        int_lb_rule_4 = self.create_Internal_LB_Rule(
            internal_tier_2, vm_array=[internal_vm_2])
        self.validate_Internal_LB_Rule(
            int_lb_rule_4, state="Active", vm_array=[internal_vm_2])
        int_lb_rule_5 = self.create_Internal_LB_Rule(
            internal_tier_2,
            vm_array=[internal_vm_2],
            services=self.test_data["internal_lbrule_http"],
            source_ip=int_lb_rule_4.sourceipaddress)
        self.validate_Internal_LB_Rule(
            int_lb_rule_5, state="Active", vm_array=[internal_vm_2])
        int_lb_rule_6 = self.create_Internal_LB_Rule(
            internal_tier_2,
            vm_array=[internal_vm_2],
            services=internal_lbrule_http,
            source_ip=int_lb_rule_4.sourceipaddress)
        self.validate_Internal_LB_Rule(
            int_lb_rule_6, state="Active", vm_array=[internal_vm_2])

        # Validating InternalLbVm deployment and state
        int_lb_vm_2 = self.get_InternalLbVm(
            internal_tier_2, int_lb_rule_4.sourceipaddress)
        self.check_InternalLbVm_state(
            internal_tier_2, int_lb_rule_4.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm_2)

        # Deploying more VMs in the Internal tier
        self.debug("Deploying two more VMs in network - %s" %
                   internal_tier_2.name)
        internal_vm_2_1 = self.create_VM(internal_tier_2)
        internal_vm_2_2 = self.create_VM(internal_tier_2)

        # VSD verification
        self.verify_vsd_vm(internal_vm_2_1)
        self.verify_vsd_vm(internal_vm_2_2)

        # Adding newly deployed VMs to the created Internal LB rules
        self.debug("Adding two more virtual machines to the created Internal "
                   "LB rules...")
        int_lb_rule_4.assign(
            self.api_client, [internal_vm_2_1, internal_vm_2_2])
        self.validate_Internal_LB_Rule(
            int_lb_rule_4, state="Active",
            vm_array=[internal_vm_2, internal_vm_2_1, internal_vm_2_2])
        int_lb_rule_5.assign(
            self.api_client, [internal_vm_2_1, internal_vm_2_2])
        self.validate_Internal_LB_Rule(
            int_lb_rule_5, state="Active",
            vm_array=[internal_vm_2, internal_vm_2_1, internal_vm_2_2])
        int_lb_rule_6.assign(
            self.api_client, [internal_vm_2_1, internal_vm_2_2])
        self.validate_Internal_LB_Rule(
            int_lb_rule_6, state="Active",
            vm_array=[internal_vm_2, internal_vm_2_1, internal_vm_2_2])

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(
            internal_tier_2, int_lb_rule_4.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm_2)

        # Adding Network ACL rules in the Internal tier
        self.debug("Adding Network ACL rules to make the created Internal LB "
                   "rules (HTTP) accessible...")
        http_rule_1 = self.create_NetworkAclRule(
            self.test_data["http_rule"], network=internal_tier_2)
        http_rule_2 = self.create_NetworkAclRule(
            http_rule, network=internal_tier_2)

        # VSD verification
        self.verify_vsd_firewall_rule(http_rule_1)
        self.verify_vsd_firewall_rule(http_rule_2)

        # Verifying Internal Load Balanced VMs ingress traffic
        # (SSH into VM via Static NAT rule)
        self.debug("Verifying Internal Load Balanced VMs ingress traffic "
                   "(SSH into VM via Static NAT rule)...")
        self.verify_vpc_vm_ingress_traffic(
            internal_vm_1, internal_tier_1, vpc)
        self.verify_vpc_vm_ingress_traffic(
            internal_vm_1_1, internal_tier_1, vpc)
        self.verify_vpc_vm_ingress_traffic(
            internal_vm_1_2, internal_tier_1, vpc)
        self.verify_vpc_vm_ingress_traffic(
            internal_vm_2, internal_tier_2, vpc)
        self.verify_vpc_vm_ingress_traffic(
            internal_vm_2_1, internal_tier_2, vpc)
        self.verify_vpc_vm_ingress_traffic(
            internal_vm_2_2, internal_tier_2, vpc)

        # Creating Static NAT rule for the VM in the Public tier
        public_ip = self.acquire_PublicIPAddress(public_tier, vpc)
        self.validate_PublicIPAddress(public_ip, public_tier)
        self.create_StaticNatRule_For_VM(public_vm, public_ip, public_tier)
        self.validate_PublicIPAddress(
            public_ip, public_tier, static_nat=True, vm=public_vm)

        # VSD verification
        self.verify_vsd_floating_ip(
            public_tier, public_vm, public_ip.ipaddress, vpc)

        # Adding Network ACL rule in the Public tier
        self.debug("Adding Network ACL rule to make the created NAT rule "
                   "(SSH) accessible...")
        public_ssh_rule = self.create_NetworkAclRule(
            self.test_data["ingress_rule"], network=public_tier)

        # VSD verification
        self.verify_vsd_firewall_rule(public_ssh_rule)

        # Internal LB (wget) traffic tests
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        wget_file_1 = self.wget_from_vm_cmd(
            ssh_client,
            int_lb_rule_1.sourceipaddress,
            self.test_data["http_rule"]["publicport"])
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        wget_file_2 = self.wget_from_vm_cmd(
            ssh_client,
            int_lb_rule_1.sourceipaddress,
            http_rule["publicport"])
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        wget_file_3 = self.wget_from_vm_cmd(
            ssh_client,
            int_lb_rule_4.sourceipaddress,
            self.test_data["http_rule"]["publicport"])
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        wget_file_4 = self.wget_from_vm_cmd(
            ssh_client,
            int_lb_rule_4.sourceipaddress,
            http_rule["publicport"])

        # Verifying Internal LB (wget) traffic tests
        # Bug CLOUDSTACK-9749
        self.verify_lb_wget_file(
            wget_file_1, [internal_vm_1, internal_vm_1_1, internal_vm_1_2])
        self.verify_lb_wget_file(
            wget_file_2, [internal_vm_1, internal_vm_1_1, internal_vm_1_2])
        self.verify_lb_wget_file(
            wget_file_3, [internal_vm_2, internal_vm_2_1, internal_vm_2_2])
        self.verify_lb_wget_file(
            wget_file_4, [internal_vm_2, internal_vm_2_1, internal_vm_2_2])

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_06_nuage_internallb_algorithms_traffic(self):
        """Test Nuage VSP VPC Internal LB functionality with different LB
        algorithms by performing (wget) traffic tests within a VPC
        """

        # Repeat the tests in the testcase "test_05_nuage_internallb_traffic"
        # with different Internal LB algorithms:
        # 1. Round Robin
        # 2. Least connections
        # 3. Source
        # Verify the above Internal LB algorithms by performing multiple (wget)
        # traffic tests within a VPC.
        # Delete all the created objects (cleanup).

        # Creating a VPC offering
        self.debug("Creating Nuage VSP VPC offering with Internal LB "
                   "service...")
        vpc_off = self.create_VpcOffering(
            self.test_data["nuagevsp"]["vpc_offering_lb"])
        self.validate_VpcOffering(vpc_off, state="Enabled")

        # Creating a VPC
        self.debug("Creating a VPC with Internal LB service...")
        vpc = self.create_Vpc(vpc_off, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc, state="Enabled")

        # Creating network offerings
        self.debug("Creating Nuage VSP VPC Network offering with Internal LB "
                   "service...")
        net_off_1 = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering_internal_lb"])
        self.validate_NetworkOffering(net_off_1, state="Enabled")

        self.debug("Creating Nuage VSP VPC Network offering without Internal "
                   "LB service...")
        net_off_2 = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        self.validate_NetworkOffering(net_off_2, state="Enabled")

        # Creating VPC networks in the VPC, and deploying VMs
        self.debug("Creating a VPC network with Internal LB service...")
        internal_tier = self.create_Network(
            net_off_1, gateway='10.1.1.1', vpc=vpc)
        self.validate_Network(internal_tier, state="Implemented")
        vr = self.get_Router(internal_tier)
        self.check_Router_state(vr, state="Running")

        self.debug("Deploying a VM in the created VPC network...")
        internal_vm = self.create_VM(internal_tier)
        self.check_VM_state(internal_vm, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, internal_tier, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(internal_vm)

        self.debug("Creating a VPC network without Internal LB service...")
        public_tier = self.create_Network(
            net_off_2, gateway='10.1.2.1', vpc=vpc)
        self.validate_Network(public_tier, state="Implemented")
        vr = self.get_Router(public_tier)
        self.check_Router_state(vr, state="Running")

        self.debug("Deploying a VM in the created VPC network...")
        public_vm = self.create_VM(public_tier)
        self.check_VM_state(public_vm, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, public_tier, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(public_vm)

        # Creating Internal LB Rules in the Internal tier with Round Robin
        # Algorithm
        self.debug("Creating two Internal LB Rules (SSH & HTTP) with Round "
                   "Robin Algorithm...")
        int_lb_rule_1 = self.create_Internal_LB_Rule(
            internal_tier, vm_array=[internal_vm])
        self.validate_Internal_LB_Rule(
            int_lb_rule_1, state="Active", vm_array=[internal_vm])
        int_lb_rule_2 = self.create_Internal_LB_Rule(
            internal_tier, vm_array=[internal_vm],
            services=self.test_data["internal_lbrule_http"],
            source_ip=int_lb_rule_1.sourceipaddress)
        self.validate_Internal_LB_Rule(
            int_lb_rule_2, state="Active", vm_array=[internal_vm])

        # Validating InternalLbVm deployment and state
        int_lb_vm_1 = self.get_InternalLbVm(
            internal_tier, int_lb_rule_1.sourceipaddress)
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm_1)

        # Deploying more VMs in the Internal tier
        self.debug("Deploying two more VMs in network - %s" %
                   internal_tier.name)
        internal_vm_1 = self.create_VM(internal_tier)
        internal_vm_2 = self.create_VM(internal_tier)

        # VSD verification
        self.verify_vsd_vm(internal_vm_1)
        self.verify_vsd_vm(internal_vm_2)

        # Adding newly deployed VMs to the created Internal LB rules
        self.debug("Adding two more virtual machines to the created Internal "
                   "LB rules...")
        int_lb_rule_1.assign(self.api_client, [internal_vm_1, internal_vm_2])
        self.validate_Internal_LB_Rule(
            int_lb_rule_1, state="Active",
            vm_array=[internal_vm, internal_vm_1, internal_vm_2])
        int_lb_rule_2.assign(self.api_client, [internal_vm_1, internal_vm_2])
        self.validate_Internal_LB_Rule(
            int_lb_rule_2, state="Active",
            vm_array=[internal_vm, internal_vm_1, internal_vm_2])

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm_1)

        # Creating Internal LB Rules in the Internal tier with Least
        # connections Algorithm
        self.debug("Creating two Internal LB Rules (SSH & HTTP) with Least "
                   "connections Algorithm...")
        self.test_data["internal_lbrule"]["algorithm"] = "leastconn"
        int_lb_rule_3 = self.create_Internal_LB_Rule(
            internal_tier,
            vm_array=[internal_vm, internal_vm_1, internal_vm_2],
            services=self.test_data["internal_lbrule"])
        self.validate_Internal_LB_Rule(
            int_lb_rule_3, state="Active",
            vm_array=[internal_vm, internal_vm_1, internal_vm_2])
        self.test_data["internal_lbrule_http"]["algorithm"] = "leastconn"
        int_lb_rule_4 = self.create_Internal_LB_Rule(
            internal_tier,
            vm_array=[internal_vm, internal_vm_1, internal_vm_2],
            services=self.test_data["internal_lbrule_http"],
            source_ip=int_lb_rule_3.sourceipaddress)
        self.validate_Internal_LB_Rule(
            int_lb_rule_4, state="Active",
            vm_array=[internal_vm, internal_vm_1, internal_vm_2])

        # Validating InternalLbVm deployment and state
        int_lb_vm_2 = self.get_InternalLbVm(
            internal_tier, int_lb_rule_3.sourceipaddress)
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_3.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm_2)

        # Creating Internal LB Rules in the Internal tier with Source Algorithm
        self.debug("Creating two Internal LB Rules (SSH & HTTP) with Source "
                   "Algorithm...")
        self.test_data["internal_lbrule"]["algorithm"] = "source"
        int_lb_rule_5 = self.create_Internal_LB_Rule(
            internal_tier,
            vm_array=[internal_vm, internal_vm_1, internal_vm_2],
            services=self.test_data["internal_lbrule"])
        self.validate_Internal_LB_Rule(
            int_lb_rule_5, state="Active",
            vm_array=[internal_vm, internal_vm_1, internal_vm_2])
        self.test_data["internal_lbrule_http"]["algorithm"] = "source"
        int_lb_rule_6 = self.create_Internal_LB_Rule(
            internal_tier,
            vm_array=[internal_vm, internal_vm_1, internal_vm_2],
            services=self.test_data["internal_lbrule_http"],
            source_ip=int_lb_rule_5.sourceipaddress)
        self.validate_Internal_LB_Rule(
            int_lb_rule_6, state="Active",
            vm_array=[internal_vm, internal_vm_1, internal_vm_2])

        # Validating InternalLbVm deployment and state
        int_lb_vm_3 = self.get_InternalLbVm(
            internal_tier, int_lb_rule_5.sourceipaddress)
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_5.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm_3)

        # Adding Network ACL rules in the Internal tier
        self.debug("Adding Network ACL rules to make the created Internal LB "
                   "rules (HTTP) accessible...")
        http_rule = self.create_NetworkAclRule(
            self.test_data["http_rule"], network=internal_tier)

        # VSD verification
        self.verify_vsd_firewall_rule(http_rule)

        # Verifying Internal Load Balanced VMs ingress traffic
        # (SSH into VM via Static NAT rule)
        self.debug("Verifying Internal Load Balanced VMs ingress traffic "
                   "(SSH into VM via Static NAT rule)...")
        self.verify_vpc_vm_ingress_traffic(internal_vm, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_1, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_2, internal_tier, vpc)

        # Creating Static NAT rule for the VM in the Public tier
        public_ip = self.acquire_PublicIPAddress(public_tier, vpc)
        self.validate_PublicIPAddress(public_ip, public_tier)
        self.create_StaticNatRule_For_VM(public_vm, public_ip, public_tier)
        self.validate_PublicIPAddress(
            public_ip, public_tier, static_nat=True, vm=public_vm)

        # VSD verification
        self.verify_vsd_floating_ip(
            public_tier, public_vm, public_ip.ipaddress, vpc)

        # Adding Network ACL rule in the Public tier
        self.debug("Adding Network ACL rule to make the created NAT rule "
                   "(SSH) accessible...")
        public_ssh_rule = self.create_NetworkAclRule(
            self.test_data["ingress_rule"], network=public_tier)

        # VSD verification
        self.verify_vsd_firewall_rule(public_ssh_rule)

        # Internal LB (wget) traffic tests with Round Robin Algorithm
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        self.validate_internallb_algorithm_traffic(
            ssh_client, int_lb_rule_1.sourceipaddress,
            self.test_data["http_rule"]["publicport"],
            [internal_vm, internal_vm_1, internal_vm_2], "roundrobin")

        # Internal LB (wget) traffic tests with Least connections Algorithm
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        self.validate_internallb_algorithm_traffic(
            ssh_client, int_lb_rule_3.sourceipaddress,
            self.test_data["http_rule"]["publicport"],
            [internal_vm, internal_vm_1, internal_vm_2], "leastconn")

        # Internal LB (wget) traffic tests with Source Algorithm
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        self.validate_internallb_algorithm_traffic(
            ssh_client, int_lb_rule_5.sourceipaddress,
            self.test_data["http_rule"]["publicport"],
            [internal_vm, internal_vm_1, internal_vm_2], "source")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_07_nuage_internallb_vpc_network_restarts_traffic(self):
        """Test Nuage VSP VPC Internal LB functionality with restarts of VPC
        network components by performing (wget) traffic tests within a VPC
        """

        # Repeat the tests in the testcase "test_05_nuage_internallb_traffic"
        # with restarts of VPC networks (tiers):
        # 1. Restart tier with InternalLbVm (cleanup = false), verify that the
        #    InternalLbVm gets destroyed and deployed again in the Internal
        #    tier.
        # 2. Restart tier with InternalLbVm (cleanup = true), verify that the
        #    InternalLbVm gets destroyed and deployed again in the Internal
        #    tier.
        # 3. Restart tier without InternalLbVm (cleanup = false), verify that
        #    this restart has no effect on the InternalLbVm functionality.
        # 4. Restart tier without InternalLbVm (cleanup = true), verify that
        #    this restart has no effect on the InternalLbVm functionality.
        # 5. Stop all the VMs configured with InternalLbVm, verify that the
        #    InternalLbVm gets destroyed in the Internal tier.
        # 6. Start all the VMs configured with InternalLbVm, verify that the
        #    InternalLbVm gets deployed again in the Internal tier.
        # 7. Restart VPC (cleanup = false), verify that the VPC VR gets
        #    rebooted and this restart has no effect on the InternalLbVm
        #    functionality.
        # 7. Restart VPC (cleanup = true), verify that the VPC VR gets rebooted
        #    and this restart has no effect on the InternalLbVm functionality.
        # Verify the above restarts of VPC networks (tiers) by performing
        # (wget) traffic tests within a VPC.
        # Delete all the created objects (cleanup).

        # Creating a VPC offering
        self.debug("Creating Nuage VSP VPC offering with Internal LB "
                   "service...")
        vpc_off = self.create_VpcOffering(
            self.test_data["nuagevsp"]["vpc_offering_lb"])
        self.validate_VpcOffering(vpc_off, state="Enabled")

        # Creating a VPC
        self.debug("Creating a VPC with Internal LB service...")
        vpc = self.create_Vpc(vpc_off, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc, state="Enabled")

        # Creating network offerings
        self.debug("Creating Nuage VSP VPC Network offering with Internal LB "
                   "service...")
        net_off_1 = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering_internal_lb"])
        self.validate_NetworkOffering(net_off_1, state="Enabled")

        self.debug("Creating Nuage VSP VPC Network offering without Internal "
                   "LB service...")
        net_off_2 = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        self.validate_NetworkOffering(net_off_2, state="Enabled")

        # Creating VPC networks in the VPC, and deploying VMs
        self.debug("Creating a VPC network with Internal LB service...")
        internal_tier = self.create_Network(
            net_off_1, gateway='10.1.1.1', vpc=vpc)
        self.validate_Network(internal_tier, state="Implemented")
        vr = self.get_Router(internal_tier)
        self.check_Router_state(vr, state="Running")

        self.debug("Deploying a VM in the created VPC network...")
        internal_vm = self.create_VM(internal_tier)
        self.check_VM_state(internal_vm, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, internal_tier, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(internal_vm)

        self.debug("Creating a VPC network without Internal LB service...")
        public_tier = self.create_Network(
            net_off_2, gateway='10.1.2.1', vpc=vpc)
        self.validate_Network(public_tier, state="Implemented")
        vr = self.get_Router(public_tier)
        self.check_Router_state(vr, state="Running")

        self.debug("Deploying a VM in the created VPC network...")
        public_vm = self.create_VM(public_tier)
        self.check_VM_state(public_vm, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, public_tier, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(public_vm)

        # Creating Internal LB Rules in the Internal tier
        self.debug("Creating two Internal LB Rules (SSH & HTTP) using the "
                   "same Load Balancing source IP Address...")
        int_lb_rule_1 = self.create_Internal_LB_Rule(
            internal_tier, vm_array=[internal_vm])
        self.validate_Internal_LB_Rule(
            int_lb_rule_1, state="Active", vm_array=[internal_vm])
        int_lb_rule_2 = self.create_Internal_LB_Rule(
            internal_tier, vm_array=[internal_vm],
            services=self.test_data["internal_lbrule_http"],
            source_ip=int_lb_rule_1.sourceipaddress)
        self.validate_Internal_LB_Rule(
            int_lb_rule_2, state="Active", vm_array=[internal_vm])

        # Validating InternalLbVm deployment and state
        int_lb_vm = self.get_InternalLbVm(
            internal_tier, int_lb_rule_1.sourceipaddress)
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm)

        # Deploying more VMs in the Internal tier
        self.debug("Deploying two more VMs in network - %s" %
                   internal_tier.name)
        internal_vm_1 = self.create_VM(internal_tier)
        internal_vm_2 = self.create_VM(internal_tier)

        # VSD verification
        self.verify_vsd_vm(internal_vm_1)
        self.verify_vsd_vm(internal_vm_2)

        # Adding newly deployed VMs to the created Internal LB rules
        self.debug("Adding two more virtual machines to the created Internal "
                   "LB rules...")
        int_lb_rule_1.assign(self.api_client, [internal_vm_1, internal_vm_2])
        self.validate_Internal_LB_Rule(
            int_lb_rule_1, state="Active",
            vm_array=[internal_vm, internal_vm_1, internal_vm_2])
        int_lb_rule_2.assign(self.api_client, [internal_vm_1, internal_vm_2])
        self.validate_Internal_LB_Rule(
            int_lb_rule_2, state="Active",
            vm_array=[internal_vm, internal_vm_1, internal_vm_2])

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm)

        # Adding Network ACL rules in the Internal tier
        self.debug("Adding Network ACL rules to make the created Internal LB "
                   "rules (HTTP) accessible...")
        http_rule = self.create_NetworkAclRule(
            self.test_data["http_rule"], network=internal_tier)

        # VSD verification
        self.verify_vsd_firewall_rule(http_rule)

        # Verifying Internal Load Balanced VMs ingress traffic
        # (SSH into VM via Static NAT rule)
        self.debug("Verifying Internal Load Balanced VMs ingress traffic "
                   "(SSH into VM via Static NAT rule)...")
        self.verify_vpc_vm_ingress_traffic(internal_vm, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_1, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_2, internal_tier, vpc)

        # Creating Static NAT rule for the VM in the Public tier
        public_ip = self.acquire_PublicIPAddress(public_tier, vpc)
        self.validate_PublicIPAddress(public_ip, public_tier)
        self.create_StaticNatRule_For_VM(public_vm, public_ip, public_tier)
        self.validate_PublicIPAddress(
            public_ip, public_tier, static_nat=True, vm=public_vm)

        # VSD verification
        self.verify_vsd_floating_ip(
            public_tier, public_vm, public_ip.ipaddress, vpc)

        # Adding Network ACL rule in the Public tier
        self.debug("Adding Network ACL rule to make the created NAT rule "
                   "(SSH) accessible...")
        public_ssh_rule = self.create_NetworkAclRule(
            self.test_data["ingress_rule"], network=public_tier)

        # VSD verification
        self.verify_vsd_firewall_rule(public_ssh_rule)

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        wget_file = self.wget_from_vm_cmd(
            ssh_client, int_lb_rule_1.sourceipaddress,
            self.test_data["http_rule"]["publicport"])

        # Verifying Internal LB (wget) traffic test
        self.verify_lb_wget_file(
            wget_file, [internal_vm, internal_vm_1, internal_vm_2])

        # Restart Internal tier (cleanup = false)
        # InternalLbVm gets destroyed and deployed again in the Internal tier
        self.debug("Restarting the Internal tier without cleanup...")
        Network.restart(internal_tier, self.api_client, cleanup=False)
        self.validate_Network(internal_tier, state="Implemented")
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(internal_vm, state="Running")
        self.check_VM_state(internal_vm_1, state="Running")
        self.check_VM_state(internal_vm_2, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, internal_tier, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(internal_vm)
        self.verify_vsd_vm(internal_vm_1)
        self.verify_vsd_vm(internal_vm_2)
        self.verify_vsd_firewall_rule(http_rule)

        # Validating InternalLbVm state
        # InternalLbVm gets destroyed and deployed again in the Internal tier
        int_lb_vm = self.get_InternalLbVm(
            internal_tier, int_lb_rule_1.sourceipaddress)
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm)

        # Verifying Internal Load Balanced VMs ingress traffic
        # (SSH into VM via Static NAT rule)
        self.debug("Verifying Internal Load Balanced VMs ingress traffic "
                   "(SSH into VM via Static NAT rule)...")
        self.verify_vpc_vm_ingress_traffic(internal_vm, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_1, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_2, internal_tier, vpc)

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        tries = 0
        while tries < 120:
            wget_file = self.wget_from_vm_cmd(
                ssh_client, int_lb_rule_1.sourceipaddress,
                self.test_data["http_rule"]["publicport"])
            if wget_file != "":
                break
            self.debug("Waiting for the InternalLbVm in the Internal tier to "
                       "be fully resolved for (wget) traffic test...")
            time.sleep(5)
            tries += 1

        # Verifying Internal LB (wget) traffic test
        self.verify_lb_wget_file(
            wget_file, [internal_vm, internal_vm_1, internal_vm_2])

        # Restart Internal tier (cleanup = true)
        # InternalLbVm gets destroyed and deployed again in the Internal tier
        self.debug("Restarting the Internal tier with cleanup...")
        Network.restart(internal_tier, self.api_client, cleanup=True)
        self.validate_Network(internal_tier, state="Implemented")
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(internal_vm, state="Running")
        self.check_VM_state(internal_vm_1, state="Running")
        self.check_VM_state(internal_vm_2, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, internal_tier, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(internal_vm)
        self.verify_vsd_vm(internal_vm_1)
        self.verify_vsd_vm(internal_vm_2)
        self.verify_vsd_firewall_rule(http_rule)

        # Validating InternalLbVm state
        # InternalLbVm gets destroyed and deployed again in the Internal tier
        int_lb_vm = self.get_InternalLbVm(
            internal_tier, int_lb_rule_1.sourceipaddress)
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm)

        # Verifying Internal Load Balanced VMs ingress traffic
        # (SSH into VM via Static NAT rule)
        self.debug("Verifying Internal Load Balanced VMs ingress traffic "
                   "(SSH into VM via Static NAT rule)...")
        self.verify_vpc_vm_ingress_traffic(internal_vm, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_1, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_2, internal_tier, vpc)

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        tries = 0
        while tries < 120:
            wget_file = self.wget_from_vm_cmd(
                ssh_client, int_lb_rule_1.sourceipaddress,
                self.test_data["http_rule"]["publicport"])
            if wget_file != "":
                break
            self.debug("Waiting for the InternalLbVm in the Internal tier to "
                       "be fully resolved for (wget) traffic test...")
            time.sleep(5)
            tries += 1

        # Verifying Internal LB (wget) traffic test
        self.verify_lb_wget_file(
            wget_file, [internal_vm, internal_vm_1, internal_vm_2])

        # Restart Public tier (cleanup = false)
        # This restart has no effect on the InternalLbVm functionality
        self.debug("Restarting the Public tier without cleanup...")
        Network.restart(public_tier, self.api_client, cleanup=False)
        self.validate_Network(public_tier, state="Implemented")
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(public_vm, state="Running")
        self.validate_PublicIPAddress(
            public_ip, public_tier, static_nat=True, vm=public_vm)

        # VSD verification
        self.verify_vsd_network(self.domain.id, public_tier, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(public_vm)
        self.verify_vsd_floating_ip(
            public_tier, public_vm, public_ip.ipaddress, vpc)
        self.verify_vsd_firewall_rule(public_ssh_rule)

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm)

        # Verifying Internal Load Balanced VMs ingress traffic
        # (SSH into VM via Static NAT rule)
        self.debug("Verifying Internal Load Balanced VMs ingress traffic "
                   "(SSH into VM via Static NAT rule)...")
        self.verify_vpc_vm_ingress_traffic(internal_vm, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_1, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_2, internal_tier, vpc)

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        wget_file = self.wget_from_vm_cmd(
            ssh_client, int_lb_rule_1.sourceipaddress,
            self.test_data["http_rule"]["publicport"])

        # Verifying Internal LB (wget) traffic test
        self.verify_lb_wget_file(
            wget_file, [internal_vm, internal_vm_1, internal_vm_2])

        # Restart Public tier (cleanup = true)
        # This restart has no effect on the InternalLbVm functionality
        self.debug("Restarting the Public tier with cleanup...")
        Network.restart(public_tier, self.api_client, cleanup=True)
        self.validate_Network(public_tier, state="Implemented")
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(public_vm, state="Running")
        self.validate_PublicIPAddress(
            public_ip, public_tier, static_nat=True, vm=public_vm)

        # VSD verification
        self.verify_vsd_network(self.domain.id, public_tier, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(public_vm)
        self.verify_vsd_floating_ip(
            public_tier, public_vm, public_ip.ipaddress, vpc)
        self.verify_vsd_firewall_rule(public_ssh_rule)

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm)

        # Verifying Internal Load Balanced VMs ingress traffic
        # (SSH into VM via Static NAT rule)
        self.debug("Verifying Internal Load Balanced VMs ingress traffic "
                   "(SSH into VM via Static NAT rule)...")
        self.verify_vpc_vm_ingress_traffic(internal_vm, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_1, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_2, internal_tier, vpc)

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        wget_file = self.wget_from_vm_cmd(
            ssh_client, int_lb_rule_1.sourceipaddress,
            self.test_data["http_rule"]["publicport"])

        # Verifying Internal LB (wget) traffic test
        self.verify_lb_wget_file(
            wget_file, [internal_vm, internal_vm_1, internal_vm_2])

        # Stopping VMs in the Internal tier
        # wget traffic test fails as all the VMs in the Internal tier are in
        # stopped state
        self.debug("Stopping all the VMs in the Internal tier...")
        internal_vm.stop(self.api_client)
        internal_vm_1.stop(self.api_client)
        internal_vm_2.stop(self.api_client)
        self.validate_Network(internal_tier, state="Implemented")
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(internal_vm, state="Stopped")
        self.check_VM_state(internal_vm_1, state="Stopped")
        self.check_VM_state(internal_vm_2, state="Stopped")

        # VSD verification
        self.verify_vsd_network(self.domain.id, internal_tier, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(internal_vm, stopped=True)
        self.verify_vsd_vm(internal_vm_1, stopped=True)
        self.verify_vsd_vm(internal_vm_2, stopped=True)
        self.verify_vsd_firewall_rule(http_rule)

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm)

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        wget_file = self.wget_from_vm_cmd(
            ssh_client, int_lb_rule_1.sourceipaddress,
            self.test_data["http_rule"]["publicport"])

        # Verifying Internal LB (wget) traffic test
        with self.assertRaises(Exception):
            self.verify_lb_wget_file(
                wget_file, [internal_vm, internal_vm_1, internal_vm_2])
        self.debug("Failed to wget file as all the VMs in the Internal tier "
                   "are in stopped state")

        # Starting VMs in the Internal tier
        # wget traffic test succeeds as all the VMs in the Internal tier are
        # back in running state
        self.debug("Starting all the VMs in the Internal tier...")
        internal_vm.start(self.api_client)
        internal_vm_1.start(self.api_client)
        internal_vm_2.start(self.api_client)
        self.validate_Network(internal_tier, state="Implemented")
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(internal_vm, state="Running")
        self.check_VM_state(internal_vm_1, state="Running")
        self.check_VM_state(internal_vm_2, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, internal_tier, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(internal_vm)
        self.verify_vsd_vm(internal_vm_1)
        self.verify_vsd_vm(internal_vm_2)
        self.verify_vsd_firewall_rule(http_rule)

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm)

        # Verifying Internal Load Balanced VMs ingress traffic
        # (SSH into VM via Static NAT rule)
        self.debug("Verifying Internal Load Balanced VMs ingress traffic "
                   "(SSH into VM via Static NAT rule)...")
        self.verify_vpc_vm_ingress_traffic(internal_vm, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_1, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_2, internal_tier, vpc)

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        tries = 0
        while tries < 120:
            wget_file = self.wget_from_vm_cmd(
                ssh_client, int_lb_rule_1.sourceipaddress,
                self.test_data["http_rule"]["publicport"])
            if wget_file != "":
                break
            self.debug("Waiting for the InternalLbVm in the Internal tier to "
                       "be fully resolved for (wget) traffic test...")
            time.sleep(5)
            tries += 1

        # Verifying Internal LB (wget) traffic test
        self.verify_lb_wget_file(
            wget_file, [internal_vm, internal_vm_1, internal_vm_2])

        # Restarting VPC (cleanup = false)
        # VPC VR gets destroyed and deployed again in the VPC
        # This restart has no effect on the InternalLbVm functionality
        self.debug("Restarting the VPC without cleanup...")
        self.restart_Vpc(vpc, cleanup=False)
        self.validate_Network(public_tier, state="Implemented")
        self.validate_Network(internal_tier, state="Implemented")
        vr = self.get_Router(public_tier)
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(public_vm, state="Running")
        self.check_VM_state(internal_vm, state="Running")
        self.check_VM_state(internal_vm_1, state="Running")
        self.check_VM_state(internal_vm_2, state="Running")
        self.validate_PublicIPAddress(
            public_ip, public_tier, static_nat=True, vm=public_vm)

        # VSD verification
        self.verify_vsd_network(self.domain.id, public_tier, vpc)
        self.verify_vsd_network(self.domain.id, internal_tier, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(public_vm)
        self.verify_vsd_vm(internal_vm)
        self.verify_vsd_vm(internal_vm_1)
        self.verify_vsd_vm(internal_vm_2)
        self.verify_vsd_floating_ip(
            public_tier, public_vm, public_ip.ipaddress, vpc)
        self.verify_vsd_firewall_rule(public_ssh_rule)
        self.verify_vsd_firewall_rule(http_rule)

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm)

        # Verifying Internal Load Balanced VMs ingress traffic
        # (SSH into VM via Static NAT rule)
        self.debug("Verifying Internal Load Balanced VMs ingress traffic "
                   "(SSH into VM via Static NAT rule)...")
        self.verify_vpc_vm_ingress_traffic(internal_vm, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_1, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_2, internal_tier, vpc)

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        wget_file = self.wget_from_vm_cmd(
            ssh_client, int_lb_rule_1.sourceipaddress,
            self.test_data["http_rule"]["publicport"])

        # Verifying Internal LB (wget) traffic test
        self.verify_lb_wget_file(
            wget_file, [internal_vm, internal_vm_1, internal_vm_2])

        # Restarting VPC (cleanup = true)
        # VPC VR gets destroyed and deployed again in the VPC
        # This restart has no effect on the InternalLbVm functionality
        self.debug("Restarting the VPC with cleanup...")
        self.restart_Vpc(vpc, cleanup=True)
        self.validate_Network(public_tier, state="Implemented")
        self.validate_Network(internal_tier, state="Implemented")
        vr = self.get_Router(public_tier)
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(public_vm, state="Running")
        self.check_VM_state(internal_vm, state="Running")
        self.check_VM_state(internal_vm_1, state="Running")
        self.check_VM_state(internal_vm_2, state="Running")
        self.validate_PublicIPAddress(
            public_ip, public_tier, static_nat=True, vm=public_vm)

        # VSD verification
        self.verify_vsd_network(self.domain.id, public_tier, vpc)
        self.verify_vsd_network(self.domain.id, internal_tier, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(public_vm)
        self.verify_vsd_vm(internal_vm)
        self.verify_vsd_vm(internal_vm_1)
        self.verify_vsd_vm(internal_vm_2)
        self.verify_vsd_floating_ip(
            public_tier, public_vm, public_ip.ipaddress, vpc)
        self.verify_vsd_firewall_rule(public_ssh_rule)
        self.verify_vsd_firewall_rule(http_rule)

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm)

        # Verifying Internal Load Balanced VMs ingress traffic
        # (SSH into VM via Static NAT rule)
        self.debug("Verifying Internal Load Balanced VMs ingress traffic "
                   "(SSH into VM via Static NAT rule)...")
        self.verify_vpc_vm_ingress_traffic(internal_vm, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_1, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_2, internal_tier, vpc)

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        wget_file = self.wget_from_vm_cmd(
            ssh_client, int_lb_rule_1.sourceipaddress,
            self.test_data["http_rule"]["publicport"])

        # Verifying Internal LB (wget) traffic test
        self.verify_lb_wget_file(
            wget_file, [internal_vm, internal_vm_1, internal_vm_2])

    @skip
    # Skip until CLOUDSTACK-9837 is fixed
    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_08_nuage_internallb_appliance_operations_traffic(self):
        """Test Nuage VSP VPC Internal LB functionality with InternalLbVm
        appliance operations by performing (wget) traffic tests within a VPC
        """

        # Repeat the tests in the testcase "test_05_nuage_internallb_traffic"
        # with InternalLbVm appliance operations:
        # 1. Verify the InternalLbVm deployment by creating the Internal LB
        #    Rules when the VPC VR is in Stopped state, VPC VR has no effect on
        #    the InternalLbVm functionality.
        # 2. Stop the InternalLbVm when the VPC VR is in Stopped State
        # 3. Start the InternalLbVm when the VPC VR is in Stopped state
        # 4. Stop the InternalLbVm when the VPC VR is in Running State
        # 5. Start the InternalLbVm when the VPC VR is in Running state
        # 6. Force stop the InternalLbVm when the VPC VR is in Running State
        # 7. Start the InternalLbVm when the VPC VR is in Running state
        # Verify the above restarts of VPC networks by performing (wget)
        # traffic tests within a VPC.
        # Delete all the created objects (cleanup).

        # Creating a VPC offering
        self.debug("Creating Nuage VSP VPC offering with Internal LB "
                   "service...")
        vpc_off = self.create_VpcOffering(
            self.test_data["nuagevsp"]["vpc_offering_lb"])
        self.validate_VpcOffering(vpc_off, state="Enabled")

        # Creating a VPC
        self.debug("Creating a VPC with Internal LB service...")
        vpc = self.create_Vpc(vpc_off, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc, state="Enabled")

        # Creating network offerings
        self.debug("Creating Nuage VSP VPC Network offering with Internal LB "
                   "service...")
        net_off_1 = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering_internal_lb"])
        self.validate_NetworkOffering(net_off_1, state="Enabled")

        self.debug("Creating Nuage VSP VPC Network offering without Internal "
                   "LB service...")
        net_off_2 = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        self.validate_NetworkOffering(net_off_2, state="Enabled")

        # Creating VPC networks in the VPC, and deploying VMs
        self.debug("Creating a VPC network with Internal LB service...")
        internal_tier = self.create_Network(
            net_off_1, gateway='10.1.1.1', vpc=vpc)
        self.validate_Network(internal_tier, state="Implemented")
        vr = self.get_Router(internal_tier)
        self.check_Router_state(vr, state="Running")

        self.debug("Deploying a VM in the created VPC network...")
        internal_vm = self.create_VM(internal_tier)
        self.check_VM_state(internal_vm, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, internal_tier, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(internal_vm)

        self.debug("Creating a VPC network without Internal LB service...")
        public_tier = self.create_Network(
            net_off_2, gateway='10.1.2.1', vpc=vpc)
        self.validate_Network(public_tier, state="Implemented")
        vr = self.get_Router(public_tier)
        self.check_Router_state(vr, state="Running")

        self.debug("Deploying a VM in the created VPC network...")
        public_vm = self.create_VM(public_tier)
        self.check_VM_state(public_vm, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, public_tier, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(public_vm)

        # Stopping the VPC VR
        # VPC VR has no effect on the InternalLbVm functionality
        Router.stop(self.api_client, id=vr.id)
        self.check_Router_state(vr, state="Stopped")
        self.validate_Network(public_tier, state="Implemented")
        self.validate_Network(internal_tier, state="Implemented")

        # VSD verification
        self.verify_vsd_router(vr, stopped=True)
        self.verify_vsd_network(self.domain.id, public_tier, vpc)
        self.verify_vsd_network(self.domain.id, internal_tier, vpc)

        # Creating Internal LB Rules in the Internal tier
        self.debug("Creating two Internal LB Rules (SSH & HTTP) using the "
                   "same Load Balancing source IP Address...")
        int_lb_rule_1 = self.create_Internal_LB_Rule(
            internal_tier, vm_array=[internal_vm])
        self.validate_Internal_LB_Rule(
            int_lb_rule_1, state="Active", vm_array=[internal_vm])
        int_lb_rule_2 = self.create_Internal_LB_Rule(
            internal_tier, vm_array=[internal_vm],
            services=self.test_data["internal_lbrule_http"],
            source_ip=int_lb_rule_1.sourceipaddress)
        self.validate_Internal_LB_Rule(
            int_lb_rule_2, state="Active", vm_array=[internal_vm])

        # Validating InternalLbVm deployment and state
        int_lb_vm = self.get_InternalLbVm(
            internal_tier, int_lb_rule_1.sourceipaddress)
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm)

        # Deploying more VMs in the Internal tier
        self.debug("Deploying two more VMs in network - %s" %
                   internal_tier.name)
        internal_vm_1 = self.create_VM(internal_tier)
        internal_vm_2 = self.create_VM(internal_tier)

        # VSD verification
        self.verify_vsd_vm(internal_vm_1)
        self.verify_vsd_vm(internal_vm_2)

        # Adding newly deployed VMs to the created Internal LB rules
        self.debug("Adding two more virtual machines to the created Internal "
                   "LB rules...")
        int_lb_rule_1.assign(self.api_client, [internal_vm_1, internal_vm_2])
        self.validate_Internal_LB_Rule(
            int_lb_rule_1, state="Active",
            vm_array=[internal_vm, internal_vm_1, internal_vm_2])
        int_lb_rule_2.assign(self.api_client, [internal_vm_1, internal_vm_2])
        self.validate_Internal_LB_Rule(
            int_lb_rule_2, state="Active",
            vm_array=[internal_vm, internal_vm_1, internal_vm_2])

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm)

        # Adding Network ACL rules in the Internal tier
        self.debug("Adding Network ACL rules to make the created Internal LB "
                   "rules (HTTP) accessible...")
        http_rule = self.create_NetworkAclRule(
            self.test_data["http_rule"], network=internal_tier)

        # VSD verification
        self.verify_vsd_firewall_rule(http_rule)

        # Verifying Internal Load Balanced VMs ingress traffic
        # (SSH into VM via Static NAT rule)
        self.debug("Verifying Internal Load Balanced VMs ingress traffic "
                   "(SSH into VM via Static NAT rule)...")
        self.verify_vpc_vm_ingress_traffic(internal_vm, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_1, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_2, internal_tier, vpc)

        # Creating Static NAT rule for the VM in the Public tier
        public_ip = self.acquire_PublicIPAddress(public_tier, vpc)
        self.validate_PublicIPAddress(public_ip, public_tier)
        self.create_StaticNatRule_For_VM(public_vm, public_ip, public_tier)
        self.validate_PublicIPAddress(
            public_ip, public_tier, static_nat=True, vm=public_vm)

        # VSD verification
        self.verify_vsd_floating_ip(
            public_tier, public_vm, public_ip.ipaddress, vpc)

        # Adding Network ACL rule in the Public tier
        self.debug("Adding Network ACL rule to make the created NAT rule "
                   "(SSH) accessible...")
        public_ssh_rule = self.create_NetworkAclRule(
            self.test_data["ingress_rule"], network=public_tier)

        # VSD verification
        self.verify_vsd_firewall_rule(public_ssh_rule)

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        wget_file = self.wget_from_vm_cmd(
            ssh_client, int_lb_rule_1.sourceipaddress,
            self.test_data["http_rule"]["publicport"])

        # Verifying Internal LB (wget) traffic test
        self.verify_lb_wget_file(
            wget_file, [internal_vm, internal_vm_1, internal_vm_2])

        # # Stopping the InternalLbVm when the VPC VR is in Stopped state
        self.stop_InternalLbVm(int_lb_vm)
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Stopped")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm, stopped=True)

        # Verifying Internal Load Balanced VMs ingress traffic
        # (SSH into VM via Static NAT rule)
        self.debug("Verifying Internal Load Balanced VMs ingress traffic "
                   "(SSH into VM via Static NAT rule)...")
        self.verify_vpc_vm_ingress_traffic(internal_vm, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_1, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_2, internal_tier, vpc)

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        wget_file = self.wget_from_vm_cmd(
            ssh_client, int_lb_rule_1.sourceipaddress,
            self.test_data["http_rule"]["publicport"])

        # Verifying Internal LB (wget) traffic test
        with self.assertRaises(Exception):
            self.verify_lb_wget_file(
                wget_file, [internal_vm, internal_vm_1, internal_vm_2])
        self.debug("Failed to wget file as the InternalLbVm is in stopped"
                   " state")

        # # Starting the InternalLbVm when the VPC VR is in Stopped state
        self.start_InternalLbVm(int_lb_vm)
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm)

        # Verifying Internal Load Balanced VMs ingress traffic
        # (SSH into VM via Static NAT rule)
        self.debug("Verifying Internal Load Balanced VMs ingress traffic "
                   "(SSH into VM via Static NAT rule)...")
        self.verify_vpc_vm_ingress_traffic(internal_vm, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_1, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_2, internal_tier, vpc)

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        tries = 0
        while tries < 120:
            wget_file = self.wget_from_vm_cmd(
                ssh_client, int_lb_rule_1.sourceipaddress,
                self.test_data["http_rule"]["publicport"])
            if wget_file != "":
                break
            self.debug("Waiting for the InternalLbVm in the Internal tier to "
                       "be fully resolved for (wget) traffic test...")
            time.sleep(5)
            tries += 1

        # Verifying Internal LB (wget) traffic test
        # Bug CLOUDSTACK-9837
        self.verify_lb_wget_file(
            wget_file, [internal_vm, internal_vm_1, internal_vm_2])

        # Starting the VPC VR
        # VPC VR has no effect on the InternalLbVm functionality
        Router.start(self.api_client, id=vr.id)
        self.check_Router_state(vr)
        self.validate_Network(public_tier, state="Implemented")
        self.validate_Network(internal_tier, state="Implemented")

        # VSD verification
        self.verify_vsd_router(vr)
        self.verify_vsd_network(self.domain.id, public_tier, vpc)
        self.verify_vsd_network(self.domain.id, internal_tier, vpc)

        # # Stopping the InternalLbVm when the VPC VR is in Running state
        self.stop_InternalLbVm(int_lb_vm)
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Stopped")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm, stopped=True)

        # Verifying Internal Load Balanced VMs ingress traffic
        # (SSH into VM via Static NAT rule)
        self.debug("Verifying Internal Load Balanced VMs ingress traffic "
                   "(SSH into VM via Static NAT rule)...")
        self.verify_vpc_vm_ingress_traffic(internal_vm, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_1, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_2, internal_tier, vpc)

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        wget_file = self.wget_from_vm_cmd(
            ssh_client, int_lb_rule_1.sourceipaddress,
            self.test_data["http_rule"]["publicport"])

        # Verifying Internal LB (wget) traffic test
        with self.assertRaises(Exception):
            self.verify_lb_wget_file(
                wget_file, [internal_vm, internal_vm_1, internal_vm_2])
        self.debug("Failed to wget file as the InternalLbVm is in stopped"
                   " state")

        # # Starting the InternalLbVm when the VPC VR is in Running state
        self.start_InternalLbVm(int_lb_vm)
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm)

        # Verifying Internal Load Balanced VMs ingress traffic
        # (SSH into VM via Static NAT rule)
        self.debug("Verifying Internal Load Balanced VMs ingress traffic "
                   "(SSH into VM via Static NAT rule)...")
        self.verify_vpc_vm_ingress_traffic(internal_vm, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_1, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_2, internal_tier, vpc)

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        tries = 0
        while tries < 120:
            wget_file = self.wget_from_vm_cmd(
                ssh_client, int_lb_rule_1.sourceipaddress,
                self.test_data["http_rule"]["publicport"])
            if wget_file != "":
                break
            self.debug("Waiting for the InternalLbVm in the Internal tier to "
                       "be fully resolved for (wget) traffic test...")
            time.sleep(5)
            tries += 1

        # Verifying Internal LB (wget) traffic test
        self.verify_lb_wget_file(
            wget_file, [internal_vm, internal_vm_1, internal_vm_2])

        # # Force Stopping the InternalLbVm when the VPC VR is in Running state
        self.stop_InternalLbVm(int_lb_vm, force=True)
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Stopped")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm, stopped=True)

        # Verifying Internal Load Balanced VMs ingress traffic
        # (SSH into VM via Static NAT rule)
        self.debug("Verifying Internal Load Balanced VMs ingress traffic "
                   "(SSH into VM via Static NAT rule)...")
        self.verify_vpc_vm_ingress_traffic(internal_vm, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_1, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_2, internal_tier, vpc)

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        wget_file = self.wget_from_vm_cmd(
            ssh_client, int_lb_rule_1.sourceipaddress,
            self.test_data["http_rule"]["publicport"])

        # Verifying Internal LB (wget) traffic test
        with self.assertRaises(Exception):
            self.verify_lb_wget_file(
                wget_file, [internal_vm, internal_vm_1, internal_vm_2])
        self.debug("Failed to wget file as the InternalLbVm is in stopped"
                   " state")

        # # Starting the InternalLbVm when the VPC VR is in Running state
        self.start_InternalLbVm(int_lb_vm)
        self.check_InternalLbVm_state(
            internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # VSD Verification
        self.verify_vsd_lb_device(int_lb_vm)

        # Verifying Internal Load Balanced VMs ingress traffic
        # (SSH into VM via Static NAT rule)
        self.debug("Verifying Internal Load Balanced VMs ingress traffic "
                   "(SSH into VM via Static NAT rule)...")
        self.verify_vpc_vm_ingress_traffic(internal_vm, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_1, internal_tier, vpc)
        self.verify_vpc_vm_ingress_traffic(internal_vm_2, internal_tier, vpc)

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        tries = 0
        while tries < 120:
            wget_file = self.wget_from_vm_cmd(
                ssh_client, int_lb_rule_1.sourceipaddress,
                self.test_data["http_rule"]["publicport"])
            if wget_file != "":
                break
            self.debug("Waiting for the InternalLbVm in the Internal tier to "
                       "be fully resolved for (wget) traffic test...")
            time.sleep(5)
            tries += 1

        # Verifying Internal LB (wget) traffic test
        self.verify_lb_wget_file(
            wget_file, [internal_vm, internal_vm_1, internal_vm_2])
