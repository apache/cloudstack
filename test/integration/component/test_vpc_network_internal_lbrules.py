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

""" Component tests for VPC network functionality - Internal Load Balancing Rules
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             ApplicationLoadBalancer,
                             Network,
                             NetworkACL,
                             NetworkOffering,
                             PublicIPAddress,
                             Router,
                             ServiceOffering,
                             StaticNATRule,
                             VirtualMachine,
                             VPC,
                             VpcOffering)
from marvin.lib.common import (get_domain,
                               get_template,
                               get_zone)
from marvin.lib.utils import cleanup_resources
from marvin.cloudstackAPI import (listInternalLoadBalancerVMs,
                                  restartVPC,
                                  stopInternalLoadBalancerVM,
                                  startInternalLoadBalancerVM)
# Import System Modules
from nose.plugins.attrib import attr
import copy
import socket
import time


class TestVPCNetworkInternalLBRules(cloudstackTestCase):
    """Test VPC network functionality with Internal Load Balancing Rules
    """

    @classmethod
    def setUpClass(cls):
        # We want to fail quicker, if it's a failure
        socket.setdefaulttimeout(60)

        test_client = super(TestVPCNetworkInternalLBRules, cls).getClsTestClient()
        cls.api_client = test_client.getApiClient()
        cls.db_client = test_client.getDbConnection()
        cls.test_data = test_client.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.api_client)
        cls.domain = get_domain(cls.api_client)
        cls.template = get_template(cls.api_client,
                                    cls.zone.id,
                                    cls.test_data["ostype"]
                                    )
        cls.test_data["virtual_machine"]["zoneid"] = cls.zone.id
        cls.test_data["virtual_machine"]["template"] = cls.template.id

        # Create service offering
        cls.service_offering = ServiceOffering.create(cls.api_client,
                                                      cls.test_data["service_offering"]
                                                      )
        cls._cleanup = [cls.service_offering]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            print(("Warning: Exception during cleanup : %s" % e))
        return

    def setUp(self):
        # Create an account
        self.account = Account.create(self.api_client,
                                      self.test_data["account"],
                                      admin=True,
                                      domainid=self.domain.id
                                      )
        self.cleanup = [self.account]

        # Creating a VPC offering
        self.debug("Creating a VPC offering..")
        self.vpc_off = VpcOffering.create(self.api_client, self.test_data["vpc_offering_multi_lb"])
        self.cleanup.append(self.vpc_off)
        self.debug("Enabling the VPC offering created")
        self.vpc_off.update(self.api_client, state='Enabled')

        # Creating a VPC
        self.debug("Creating a VPC in the account: %s" % self.account.name)
        testdata = self.test_data["vpc"]
        testdata["name"] = "TestVPC"
        testdata["displaytext"] = "TestVPC"
        testdata["cidr"] = "10.1.1.1/16"
        self.vpc = VPC.create(self.api_client,
                              testdata,
                              vpcofferingid=self.vpc_off.id,
                              zoneid=self.zone.id,
                              account=self.account.name,
                              domainid=self.account.domainid
                              )

        # Creating network offerings
        self.debug("Creating Network offering with Internal LB service...")
        self.net_off_1 = NetworkOffering.create(self.api_client,
                                                self.test_data["network_offering_internal_lb"],
                                                conservemode=False)
        self.cleanup.append(self.net_off_1)
        self.debug("Enabling the Network offering created")
        self.net_off_1.update(self.api_client, state="Enabled")

        self.debug("Creating Network offering without Internal LB service...")
        net_offering = copy.deepcopy(self.test_data["network_offering_internal_lb"])
        net_offering["name"] = "Network offering without internal lb service"
        net_offering["displaytext"] = "Network offering without internal lb service"
        net_offering["supportedservices"] = "Vpn,Dhcp,Dns,UserData,SourceNat,StaticNat,PortForwarding,NetworkACL"
        del net_offering["serviceProviderList"]["Lb"]
        del net_offering["serviceCapabilityList"]["Lb"]
        self.net_off_2 = NetworkOffering.create(self.api_client,
                                                net_offering,
                                                conservemode=False)
        self.cleanup.append(self.net_off_2)
        self.debug("Enabling the Network offering created")
        self.net_off_2.update(self.api_client, state="Enabled")
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created network offerings
            cleanup_resources(self.api_client, self.cleanup)
        except Exception as e:
            self.debug("Warning: Exception during cleanup : %s" % e)
        return

    # create_Network - Creates network with the given Network offering in the VPC
    def create_Network(self, nw_off, gateway="10.1.1.1"):
        self.debug("Creating a network in the account - %s" % self.account.name)
        self.test_data["network"]["netmask"] = "255.255.255.0"
        network = Network.create(self.api_client,
                                 self.test_data["network"],
                                 accountid=self.account.name,
                                 domainid=self.account.domainid,
                                 networkofferingid=nw_off.id,
                                 zoneid=self.zone.id,
                                 gateway=gateway,
                                 vpcid=self.vpc.id,
                                 )
        self.debug("Created network with ID - %s" % network.id)
        return network

    # create_VM - Creates VM in the given network
    def create_VM(self, network):
        self.debug("Creating VM in network with ID - %s in the account - %s" % (network.id, self.account.name))
        vm = VirtualMachine.create(self.api_client,
                                   self.test_data["virtual_machine"],
                                   accountid=self.account.name,
                                   domainid=self.account.domainid,
                                   serviceofferingid=self.service_offering.id,
                                   templateid=self.template.id,
                                   zoneid=self.zone.id,
                                   networkids=[str(network.id)],
                                   hostid=None
                                   )
        self.debug("Created VM with ID - %s in network with ID - %s" % (vm.id, network.id))
        return vm

    # restart_Vpc - Restarts the given VPC with/without cleanup
    def restart_Vpc(self, vpc, cleanup=None):
        self.debug("Restarting VPC with ID - %s" % vpc.id)
        cmd = restartVPC.restartVPCCmd()
        cmd.id = vpc.id
        cmd.cleanup = cleanup
        self.api_client.restartVPC(cmd)
        self.debug("Restarted VPC with ID - %s" % vpc.id)

    # get_Router - Returns router for the given network
    def get_Router(self, network):
        self.debug("Finding the virtual router for network with ID - %s" % network.id)
        routers = Router.list(self.api_client,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(isinstance(routers, list), True,
                         "List routers should return a valid virtual router for network"
                         )
        return routers[0]

    # create_Internal_LB_Rule - Creates Internal LB rule in the given VPC network
    def create_Internal_LB_Rule(self, network, vm_array=None, services=None, source_ip=None):
        self.debug("Creating Internal LB rule in VPC network with ID - %s" % network.id)
        if not services:
            services = self.test_data["internal_lbrule"]
        int_lb_rule = ApplicationLoadBalancer.create(self.api_client,
                                                     services=services,
                                                     sourcenetworkid=network.id,
                                                     networkid=network.id,
                                                     sourceipaddress=source_ip
                                                     )
        self.debug("Created Internal LB rule")
        # Assigning VMs to the created Internal Load Balancer rule
        if vm_array:
            self.debug("Assigning virtual machines - %s to the created Internal LB rule" % vm_array)
            int_lb_rule.assign(self.api_client, vms=vm_array)
            self.debug("Assigned VMs to the created Internal LB rule")
        return int_lb_rule

    # validate_Internal_LB_Rule - Validates the given Internal LB rule,
    # matches the given Internal LB rule name and state against the list of Internal LB rules fetched
    def validate_Internal_LB_Rule(self, int_lb_rule, state=None, vm_array=None):
        """Validates the Internal LB Rule"""
        self.debug("Check if the Internal LB Rule is created successfully ?")
        int_lb_rules = ApplicationLoadBalancer.list(self.api_client,
                                                    id=int_lb_rule.id
                                                    )
        self.assertEqual(isinstance(int_lb_rules, list), True,
                         "List Internal LB Rule should return a valid list"
                         )
        self.assertEqual(int_lb_rule.name, int_lb_rules[0].name,
                         "Name of the Internal LB Rule should match with the returned list data"
                         )
        if state:
            self.assertEqual(int_lb_rules[0].loadbalancerrule[0].state, state,
                             "Internal LB Rule state should be '%s'" % state
                             )
        if vm_array:
            instance_ids = [instance.id for instance in int_lb_rules[0].loadbalancerinstance]
            for vm in vm_array:
                self.assertEqual(vm.id in instance_ids, True,
                                 "Internal LB instance list should have the VM with ID - %s" % vm.id
                                 )
        self.debug("Internal LB Rule creation successfully validated for %s" % int_lb_rule.name)

    # list_InternalLbVms - Lists deployed Internal LB VM instances
    def list_InternalLbVms(self, network_id=None, source_ip=None):
        listInternalLoadBalancerVMsCmd = listInternalLoadBalancerVMs.listInternalLoadBalancerVMsCmd()
        listInternalLoadBalancerVMsCmd.account = self.account.name
        listInternalLoadBalancerVMsCmd.domainid = self.account.domainid
        if network_id:
            listInternalLoadBalancerVMsCmd.networkid = network_id
        internal_lb_vms = self.api_client.listInternalLoadBalancerVMs(listInternalLoadBalancerVMsCmd)
        if source_ip:
            return [internal_lb_vm for internal_lb_vm in internal_lb_vms
                    if str(internal_lb_vm.guestipaddress) == source_ip]
        else:
            return internal_lb_vms

    # get_InternalLbVm - Returns Internal LB VM instance for the given VPC network and source ip
    def get_InternalLbVm(self, network, source_ip):
        self.debug("Finding the InternalLbVm for network with ID - %s and source IP address - %s" %
                   (network.id, source_ip))
        internal_lb_vms = self.list_InternalLbVms(network.id, source_ip)
        self.assertEqual(isinstance(internal_lb_vms, list), True,
                         "List InternalLbVms should return a valid list"
                         )
        return internal_lb_vms[0]

    # stop_InternalLbVm - Stops the given Internal LB VM instance
    def stop_InternalLbVm(self, int_lb_vm, force=None):
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

    # check_InternalLbVm_state - Checks if the Internal LB VM instance of the given VPC network and source IP is in the
    # expected state form the list of fetched Internal LB VM instances
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
        self.debug("InternalLbVm instance - %s is in the expected state - %s" % (internal_lb_vms[0].name, state))

    # create_NetworkAclRule - Creates Ingress Network ACL rule in the given network
    def create_NetworkAclRule(self, rule, network):
        self.debug("Adding Ingress NetworkACL rule - %s" % rule)
        return NetworkACL.create(self.api_client,
                                 networkid=network.id,
                                 services=rule,
                                 traffictype="Ingress"
                                 )

    # acquire_PublicIPAddress - Acquires public IP address for the VPC
    def acquire_PublicIPAddress(self):
        self.debug("Acquiring public IP for VPC with ID - %s in the account - %s" % (self.vpc.id, self.account.name))
        public_ip = PublicIPAddress.create(self.api_client,
                                           accountid=self.account.name,
                                           domainid=self.account.domainid,
                                           zoneid=self.zone.id,
                                           vpcid=self.vpc.id
                                           )
        self.debug("Acquired public IP address - %s for VPC with ID - %s" %
                   (public_ip.ipaddress.ipaddress, self.vpc.id))
        return public_ip

    # create_StaticNatRule_For_VM - Creates Static NAT rule on the given public IP for the given VM in the given network
    def create_StaticNatRule_For_VM(self, vm, public_ip, network):
        self.debug("Enabling Static NAT rule on public IP - %s for VM with ID - %s in network with ID - %s" %
                   (public_ip.ipaddress.ipaddress, vm.id, network.id))
        StaticNATRule.enable(self.api_client,
                             ipaddressid=public_ip.ipaddress.id,
                             virtualmachineid=vm.id,
                             networkid=network.id,
                             vmguestip=None
                             )
        self.debug("Static NAT rule enabled on public IP - %s for VM with ID - %s in network with ID - %s" %
                   (public_ip.ipaddress.ipaddress, vm.id, network.id))

    # ssh_into_VM - Gets into the shell of the given VM using its Static NAT rule enabled public IP
    def ssh_into_VM(self, vm, public_ip):
        self.debug("SSH into VM with ID - %s on public IP address - %s" % (vm.id, public_ip.ipaddress.ipaddress))
        ssh_client = vm.get_ssh_client(ipaddress=public_ip.ipaddress.ipaddress)
        return ssh_client

    # execute_cmd - Executes the given command on the given ssh client
    def execute_cmd(self, ssh_client, cmd):
        self.debug("SSH client executing command - %s" % cmd)
        ret_data = ""
        out_list = ssh_client.execute(cmd)
        if out_list is not None:
            ret_data = ' '.join(map(str, out_list)).strip()
            self.debug("SSH client executed command result - %s" % ret_data)
        else:
            self.debug("SSH client executed command result is None")
        return ret_data

    # wget_from_vm_cmd - From within the given VM (ssh client),
    # fetches test.html file of web server running with the given public IP
    def wget_from_vm_cmd(self, ssh_client, ip_address, port):
        cmd = "wget --no-cache -t 1 http://" + ip_address + ":" + str(port) + "/test.html"
        response = self.execute_cmd(ssh_client, cmd)
        if "200 OK" not in response:
            self.fail("Failed to wget from a VM with http server IP address - %s" % ip_address)
        # Removing the wget file
        cmd = "rm -r test.html"
        self.execute_cmd(ssh_client, cmd)

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_01_internallb_rules(self):
        """Test VPC Network Internal LB functionality with different combinations of Internal LB rules
        """

        # 1. Create an Internal LB Rule with source IP Address specified, check if the Internal LB Rule is successfully
        #    created.
        # 2. Create an Internal LB Rule without source IP Address specified, check if the Internal LB Rule is
        #    successfully created.
        # 3. Create an Internal LB Rule when the specified source IP Address is outside the VPC network (tier) CIDR
        #    range, check if the Internal LB Rule creation failed as the requested source IP is not in the network's
        #    CIDR subnet.
        # 4. Create an Internal LB Rule when the specified source IP Address is outside the VPC super CIDR range,
        #    check if the Internal LB Rule creation failed as the requested source IP is not in the network's CIDR
        #    subnet.
        # 5. Create an Internal LB Rule in the tier with LB service provider as VpcInlineLbVm, check if the Internal LB
        #    Rule creation failed as Scheme Internal is not supported by this network offering.
        # 6. Create multiple Internal LB Rules using different Load Balancing source IP Addresses, check if the Internal
        #    LB Rules are successfully created.
        # 7. Create multiple Internal LB Rules with different ports but using the same Load Balancing source IP Address,
        #    check if the Internal LB Rules are successfully created.
        # 8. Create multiple Internal LB Rules with same ports and using the same Load Balancing source IP Address,
        #    check if the second Internal LB Rule creation failed as it conflicts with the first Internal LB rule.
        # 9. Attach a VM to the above created Internal LB Rules, check if the VM is successfully attached to the
        #    Internal LB Rules.
        # 10. Verify the InternalLbVm deployment after successfully creating the first Internal LB Rule and attaching a
        #     VM to it.
        # 11. Verify the failure of attaching a VM from a different tier to an Internal LB Rule created on a tier.
        # 12. Delete the above created Internal LB Rules, check if the Internal LB Rules are successfully deleted.

        # Creating VPC networks in the VPC, and deploying VMs
        self.debug("Creating a VPC network with Internal LB service...")
        internal_tier = self.create_Network(self.net_off_1, gateway='10.1.1.1')

        self.debug("Deploying a VM in the created VPC network...")
        internal_vm = self.create_VM(internal_tier)

        self.debug("Creating a VPC network without Internal LB service...")
        public_tier = self.create_Network(self.net_off_2, gateway='10.1.2.1')

        self.debug("Deploying a VM in the created VPC network...")
        public_vm = self.create_VM(public_tier)

        # Creating Internal LB Rules
        self.debug("Creating an Internal LB Rule without source IP Address specified...")
        int_lb_rule = self.create_Internal_LB_Rule(internal_tier)
        self.validate_Internal_LB_Rule(int_lb_rule, state="Add")

        # Validating InternalLbVm deployment
        with self.assertRaises(Exception):
            self.check_InternalLbVm_state(internal_tier, int_lb_rule.sourceipaddress)
        self.debug("InternalLbVm is not deployed in the network as there are no VMs assigned to this Internal LB Rule")

        self.debug('Deleting the Internal LB Rule - %s' % int_lb_rule.name)
        int_lb_rule.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_Internal_LB_Rule(int_lb_rule)
        self.debug("Internal LB Rule successfully deleted in CloudStack")

        free_source_ip = int_lb_rule.sourceipaddress

        self.debug("Creating an Internal LB Rule with source IP Address specified...")
        int_lb_rule = self.create_Internal_LB_Rule(internal_tier, source_ip=free_source_ip)
        self.validate_Internal_LB_Rule(int_lb_rule, state="Add")

        # Validating InternalLbVm deployment
        with self.assertRaises(Exception):
            self.check_InternalLbVm_state(internal_tier, int_lb_rule.sourceipaddress)
        self.debug("InternalLbVm is not deployed in the network as there are no VMs assigned to this Internal LB Rule")

        self.debug('Deleting the Internal LB Rule - %s' % int_lb_rule.name)
        int_lb_rule.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_Internal_LB_Rule(int_lb_rule)
        self.debug("Internal LB Rule successfully deleted in CloudStack")

        self.debug("Creating an Internal LB Rule when the specified source IP Address is outside the VPC network CIDR "
                   "range...")
        with self.assertRaises(Exception):
            self.create_Internal_LB_Rule(internal_tier, source_ip="10.1.1.256")
        self.debug("Internal LB Rule creation failed as the requested IP is not in the network's CIDR subnet")

        self.debug("Creating an Internal LB Rule when the specified source IP Address is outside the VPC super CIDR "
                   "range...")
        with self.assertRaises(Exception):
            self.create_Internal_LB_Rule(internal_tier, source_ip="10.2.1.256")
        self.debug("Internal LB Rule creation failed as the requested IP is not in the network's CIDR subnet")

        self.debug("Creating an Internal LB Rule in a VPC network without Internal Lb service...")
        with self.assertRaises(Exception):
            self.create_Internal_LB_Rule(public_tier)
        self.debug("Internal LB Rule creation failed as Scheme Internal is not supported by this network offering")

        self.debug("Creating multiple Internal LB Rules using different Load Balancing source IP Addresses...")
        int_lb_rule_1 = self.create_Internal_LB_Rule(internal_tier, vm_array=[internal_vm])
        self.validate_Internal_LB_Rule(int_lb_rule_1, state="Active", vm_array=[internal_vm])
        int_lb_rule_2 = self.create_Internal_LB_Rule(internal_tier, vm_array=[internal_vm])
        self.validate_Internal_LB_Rule(int_lb_rule_2, state="Active", vm_array=[internal_vm])

        # Validating InternalLbVms deployment and state
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Running")
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_2.sourceipaddress, state="Running")

        self.debug('Removing VMs from the Internal LB Rules - %s, %s' % (int_lb_rule_1.name, int_lb_rule_2.name))
        int_lb_rule_1.remove(self.api_client, vms=[internal_vm])
        with self.assertRaises(Exception):
            self.validate_Internal_LB_Rule(int_lb_rule_1, vm_array=[internal_vm])
        self.debug("VMs successfully removed from the Internal LB Rule in CloudStack")
        int_lb_rule_2.remove(self.api_client, vms=[internal_vm])
        with self.assertRaises(Exception):
            self.validate_Internal_LB_Rule(int_lb_rule_2, vm_array=[internal_vm])
        self.debug("VMs successfully removed from the Internal LB Rule in CloudStack")

        # Validating InternalLbVms state
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Running")
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_2.sourceipaddress, state="Running")

        self.debug('Deleting the Internal LB Rules - %s, %s' % (int_lb_rule_1.name, int_lb_rule_2.name))
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
            self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress)
        self.debug("InternalLbVm successfully destroyed in CloudStack")
        with self.assertRaises(Exception):
            self.check_InternalLbVm_state(internal_tier, int_lb_rule_2.sourceipaddress)
        self.debug("InternalLbVm successfully destroyed in CloudStack")

        self.debug("Creating multiple Internal LB Rules with different ports but using the same Load Balancing source "
                   "IP Address...")
        int_lb_rule_1 = self.create_Internal_LB_Rule(internal_tier, vm_array=[internal_vm])
        self.validate_Internal_LB_Rule(int_lb_rule_1, state="Active", vm_array=[internal_vm])
        int_lb_rule_2 = self.create_Internal_LB_Rule(internal_tier,
                                                     vm_array=[internal_vm],
                                                     services=self.test_data["internal_lbrule_http"],
                                                     source_ip=int_lb_rule_1.sourceipaddress
                                                     )
        self.validate_Internal_LB_Rule(int_lb_rule_2, state="Active", vm_array=[internal_vm])

        # Validating InternalLbVm deployment and state
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        self.debug('Removing VMs from the Internal LB Rules - %s, %s' % (int_lb_rule_1.name, int_lb_rule_2.name))
        int_lb_rule_1.remove(self.api_client, vms=[internal_vm])
        with self.assertRaises(Exception):
            self.validate_Internal_LB_Rule(int_lb_rule_1, vm_array=[internal_vm])
        self.debug("VMs successfully removed from the Internal LB Rule in CloudStack")
        int_lb_rule_2.remove(self.api_client, vms=[internal_vm])
        with self.assertRaises(Exception):
            self.validate_Internal_LB_Rule(int_lb_rule_2, vm_array=[internal_vm])
        self.debug("VMs successfully removed from the Internal LB Rule in CloudStack")

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        self.debug('Deleting the Internal LB Rules - %s, %s' % (int_lb_rule_1.name, int_lb_rule_2.name))
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
            self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress)
        self.debug("InternalLbVm successfully destroyed in CloudStack")

        self.debug("Creating multiple Internal LB Rules with same ports and using the same Load Balancing source IP "
                   "Address...")
        int_lb_rule = self.create_Internal_LB_Rule(internal_tier, vm_array=[internal_vm])
        self.validate_Internal_LB_Rule(int_lb_rule, state="Active", vm_array=[internal_vm])
        with self.assertRaises(Exception):
            self.create_Internal_LB_Rule(internal_tier, vm_array=[internal_vm], source_ip=int_lb_rule.sourceipaddress)
        self.debug("Internal LB Rule creation failed as it conflicts with the existing rule")

        # Validating InternalLbVm deployment and state
        self.check_InternalLbVm_state(internal_tier, int_lb_rule.sourceipaddress, state="Running")

        self.debug('Removing VMs from the Internal LB Rule - %s' % int_lb_rule.name)
        int_lb_rule.remove(self.api_client, vms=[internal_vm])
        with self.assertRaises(Exception):
            self.validate_Internal_LB_Rule(int_lb_rule, vm_array=[internal_vm])
        self.debug("VMs successfully removed from the Internal LB Rule in CloudStack")

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(internal_tier, int_lb_rule.sourceipaddress, state="Running")

        self.debug('Deleting the Internal LB Rule - %s' % int_lb_rule.name)
        int_lb_rule.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_Internal_LB_Rule(int_lb_rule)
        self.debug("Internal LB Rule successfully deleted in CloudStack")

        # Validating InternalLbVm un-deployment
        with self.assertRaises(Exception):
            self.check_InternalLbVm_state(internal_tier, int_lb_rule.sourceipaddress)
        self.debug("InternalLbVm successfully destroyed in CloudStack")

        self.debug("Attaching a VM from a different tier to an Internal LB Rule created on a tier...")
        with self.assertRaises(Exception):
            self.create_Internal_LB_Rule(internal_tier, vm_array=[public_vm])
        self.debug("Internal LB Rule creation failed as the VM belongs to a different network")

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_02_internallb_rules_traffic(self):
        """Test VPC Network Internal LB functionality by performing (wget) traffic tests within a VPC
        """

        # 1. Create an Internal LB Rule "internal_lbrule" with source IP Address specified on the Internal tier, check
        #    if the Internal LB Rule is successfully created.
        # 2. Create an Internal LB Rule "internal_lbrule_http" with source IP Address (same as above) specified on the
        #    Internal tier, check if the Internal LB Rule is successfully created.
        # 3. Attach a VM to the above created Internal LB Rules, check if the InternalLbVm is successfully deployed in
        #    the Internal tier.
        # 4. Deploy two more VMs in the Internal tier, check if the VMs are successfully deployed.
        # 5. Attach the newly deployed VMs to the above created Internal LB Rules, verify the validity of the above
        #    created Internal LB Rules over three Load Balanced VMs in the Internal tier.
        # 6. Create the corresponding Network ACL rules to make the created Internal LB rules (SSH & HTTP) accessible,
        #    check if the Network ACL rules are successfully added to the internal tier.
        # 7. Validate the Internal LB functionality by performing (wget) traffic tests from a VM in the Public tier to
        #    the Internal load balanced guest VMs in the Internal tier, using Static NAT functionality to access (ssh)
        #    the VM on the Public tier.
        # 8. Verify that the InternalLbVm gets destroyed when the last Internal LB rule is removed from the Internal
        #    tier.
        # 9. Repeat the above steps for one more Internal tier as well, validate the Internal LB functionality.

        # Creating VPC networks in the VPC, and deploying VMs
        self.debug("Creating a VPC network with Internal LB service...")
        internal_tier_1 = self.create_Network(self.net_off_1, gateway='10.1.1.1')

        self.debug("Deploying a VM in the created VPC network...")
        internal_vm_1 = self.create_VM(internal_tier_1)

        self.debug("Creating one more VPC network with Internal LB service...")
        internal_tier_2 = self.create_Network(self.net_off_1, gateway='10.1.2.1')

        self.debug("Deploying a VM in the created VPC network...")
        internal_vm_2 = self.create_VM(internal_tier_2)

        self.debug("Creating a VPC network without Internal LB service...")
        public_tier = self.create_Network(self.net_off_2, gateway='10.1.3.1')

        self.debug("Deploying a VM in the created VPC network...")
        public_vm = self.create_VM(public_tier)

        # Creating Internal LB Rules in the Internal tiers
        self.debug("Creating three Internal LB Rules (SSH & HTTP) using the same Load Balancing source IP Address...")
        int_lb_rule_1 = self.create_Internal_LB_Rule(internal_tier_1, vm_array=[internal_vm_1])
        self.validate_Internal_LB_Rule(int_lb_rule_1, state="Active", vm_array=[internal_vm_1])
        int_lb_rule_2 = self.create_Internal_LB_Rule(internal_tier_1,
                                                     vm_array=[internal_vm_1],
                                                     services=self.test_data["internal_lbrule_http"],
                                                     source_ip=int_lb_rule_1.sourceipaddress
                                                     )
        self.validate_Internal_LB_Rule(int_lb_rule_2, state="Active", vm_array=[internal_vm_1])
        internal_lbrule_http = copy.deepcopy(self.test_data["internal_lbrule_http"])
        internal_lbrule_http["sourceport"] = 8080
        internal_lbrule_http["instanceport"] = 8080
        int_lb_rule_3 = self.create_Internal_LB_Rule(internal_tier_1,
                                                     vm_array=[internal_vm_1],
                                                     services=internal_lbrule_http,
                                                     source_ip=int_lb_rule_1.sourceipaddress
                                                     )
        self.validate_Internal_LB_Rule(int_lb_rule_3, state="Active", vm_array=[internal_vm_1])

        # Validating InternalLbVm deployment and state
        self.check_InternalLbVm_state(internal_tier_1, int_lb_rule_1.sourceipaddress, state="Running")

        # Deploying more VMs in the Internal tier
        self.debug("Deploying two more VMs in network - %s" % internal_tier_1.name)
        internal_vm_1_1 = self.create_VM(internal_tier_1)
        internal_vm_1_2 = self.create_VM(internal_tier_1)

        # Adding newly deployed VMs to the created Internal LB rules
        self.debug("Adding two more virtual machines to the created Internal LB rules...")
        int_lb_rule_1.assign(self.api_client, [internal_vm_1_1, internal_vm_1_2])
        self.validate_Internal_LB_Rule(int_lb_rule_1, state="Active",
                                       vm_array=[internal_vm_1, internal_vm_1_1, internal_vm_1_2])
        int_lb_rule_2.assign(self.api_client, [internal_vm_1_1, internal_vm_1_2])
        self.validate_Internal_LB_Rule(int_lb_rule_2, state="Active",
                                       vm_array=[internal_vm_1, internal_vm_1_1, internal_vm_1_2])
        int_lb_rule_3.assign(self.api_client, [internal_vm_1_1, internal_vm_1_2])
        self.validate_Internal_LB_Rule(int_lb_rule_3, state="Active",
                                       vm_array=[internal_vm_1, internal_vm_1_1, internal_vm_1_2])

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(internal_tier_1, int_lb_rule_1.sourceipaddress, state="Running")

        # Adding Network ACL rules in the Internal tier
        self.debug("Adding Network ACL rules to make the created Internal LB rules (SSH & HTTP) accessible...")
        self.create_NetworkAclRule(self.test_data["ingress_rule"], internal_tier_1)
        self.create_NetworkAclRule(self.test_data["http_rule"], internal_tier_1)
        http_rule = copy.deepcopy(self.test_data["http_rule"])
        http_rule["privateport"] = 8080
        http_rule["publicport"] = 8080
        http_rule["startport"] = 8080
        http_rule["endport"] = 8080
        self.create_NetworkAclRule(http_rule, internal_tier_1)

        # Creating Internal LB Rules in the Internal tier
        self.debug("Creating three Internal LB Rules (SSH & HTTP) using the same Load Balancing source IP Address...")
        int_lb_rule_4 = self.create_Internal_LB_Rule(internal_tier_2, vm_array=[internal_vm_2])
        self.validate_Internal_LB_Rule(int_lb_rule_4, state="Active", vm_array=[internal_vm_2])
        int_lb_rule_5 = self.create_Internal_LB_Rule(internal_tier_2,
                                                     vm_array=[internal_vm_2],
                                                     services=self.test_data["internal_lbrule_http"],
                                                     source_ip=int_lb_rule_4.sourceipaddress
                                                     )
        self.validate_Internal_LB_Rule(int_lb_rule_5, state="Active", vm_array=[internal_vm_2])
        int_lb_rule_6 = self.create_Internal_LB_Rule(internal_tier_2,
                                                     vm_array=[internal_vm_2],
                                                     services=internal_lbrule_http,
                                                     source_ip=int_lb_rule_4.sourceipaddress
                                                     )
        self.validate_Internal_LB_Rule(int_lb_rule_6, state="Active", vm_array=[internal_vm_2])

        # Validating InternalLbVm deployment and state
        self.check_InternalLbVm_state(internal_tier_2, int_lb_rule_4.sourceipaddress, state="Running")

        # Deploying more VMs in the Internal tier
        self.debug("Deploying two more VMs in network - %s" % internal_tier_2.name)
        internal_vm_2_1 = self.create_VM(internal_tier_2)
        internal_vm_2_2 = self.create_VM(internal_tier_2)

        # Adding newly deployed VMs to the created Internal LB rules
        self.debug("Adding two more virtual machines to the created Internal LB rules...")
        int_lb_rule_4.assign(self.api_client, [internal_vm_2_1, internal_vm_2_2])
        self.validate_Internal_LB_Rule(int_lb_rule_4, state="Active",
                                       vm_array=[internal_vm_2, internal_vm_2_1, internal_vm_2_2])
        int_lb_rule_5.assign(self.api_client, [internal_vm_2_1, internal_vm_2_2])
        self.validate_Internal_LB_Rule(int_lb_rule_5, state="Active",
                                       vm_array=[internal_vm_2, internal_vm_2_1, internal_vm_2_2])
        int_lb_rule_6.assign(self.api_client, [internal_vm_2_1, internal_vm_2_2])
        self.validate_Internal_LB_Rule(int_lb_rule_6, state="Active",
                                       vm_array=[internal_vm_2, internal_vm_2_1, internal_vm_2_2])

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(internal_tier_2, int_lb_rule_4.sourceipaddress, state="Running")

        # Adding Network ACL rules in the Internal tier
        self.debug("Adding Network ACL rules to make the created Internal LB rules (SSH & HTTP) accessible...")
        self.create_NetworkAclRule(self.test_data["ingress_rule"], internal_tier_2)
        self.create_NetworkAclRule(self.test_data["http_rule"], internal_tier_2)
        self.create_NetworkAclRule(http_rule, internal_tier_2)

        # Creating Static NAT rule for the VM in the Public tier
        public_ip = self.acquire_PublicIPAddress()
        self.create_StaticNatRule_For_VM(public_vm, public_ip, public_tier)

        # Adding Network ACL rule in the Public tier
        self.debug("Adding Network ACL rule to make the created NAT rule (SSH) accessible...")
        self.create_NetworkAclRule(self.test_data["ingress_rule"], public_tier)

        # Internal LB (wget) traffic tests
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        self.wget_from_vm_cmd(ssh_client,
                              int_lb_rule_1.sourceipaddress,
                              self.test_data["http_rule"]["publicport"]
                              )
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        self.wget_from_vm_cmd(ssh_client,
                              int_lb_rule_1.sourceipaddress,
                              http_rule["publicport"]
                              )
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        self.wget_from_vm_cmd(ssh_client,
                              int_lb_rule_4.sourceipaddress,
                              self.test_data["http_rule"]["publicport"]
                              )
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        self.wget_from_vm_cmd(ssh_client,
                              int_lb_rule_4.sourceipaddress,
                              http_rule["publicport"]
                              )

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_03_internallb_rules_vpc_network_restarts_traffic(self):
        """Test VPC Network Internal LB functionality with restarts of VPC network components by performing (wget)
        traffic tests within a VPC
        """

        # Repeat the tests in the testcase "test_02_internallb_rules_traffic" with restarts of VPC networks (tiers):
        # 1. Restart tier with InternalLbVm (cleanup = false), verify that the InternalLbVm gets destroyed and deployed
        #    again in the Internal tier.
        # 2. Restart tier with InternalLbVm (cleanup = true), verify that the InternalLbVm gets destroyed and deployed
        #    again in the Internal tier.
        # 3. Restart tier without InternalLbVm (cleanup = false), verify that this restart has no effect on the
        #    InternalLbVm functionality.
        # 4. Restart tier without InternalLbVm (cleanup = true), verify that this restart has no effect on the
        #    InternalLbVm functionality.
        # 5. Stop all the VMs configured with InternalLbVm, verify that the InternalLbVm gets destroyed in the Internal
        #    tier.
        # 6. Start all the VMs configured with InternalLbVm, verify that the InternalLbVm gets deployed again in the
        #    Internal tier.
        # 7. Restart VPC (cleanup = false), verify that the VPC VR gets rebooted and this restart has no effect on the
        #    InternalLbVm functionality.
        # 7. Restart VPC (cleanup = true), verify that the VPC VR gets rebooted and this restart has no effect on the
        #    InternalLbVm functionality.
        # Verify the above restarts of VPC networks (tiers) by performing (wget) traffic tests within a VPC.

        # Creating VPC networks in the VPC, and deploying VMs
        self.debug("Creating a VPC network with Internal LB service...")
        internal_tier = self.create_Network(self.net_off_1, gateway='10.1.1.1')

        self.debug("Deploying a VM in the created VPC network...")
        internal_vm = self.create_VM(internal_tier)

        self.debug("Creating a VPC network without Internal LB service...")
        public_tier = self.create_Network(self.net_off_2, gateway='10.1.2.1')

        self.debug("Deploying a VM in the created VPC network...")
        public_vm = self.create_VM(public_tier)

        # Creating Internal LB Rules in the Internal tiers
        self.debug("Creating three Internal LB Rules (SSH & HTTP) using the same Load Balancing source IP Address...")
        int_lb_rule_1 = self.create_Internal_LB_Rule(internal_tier, vm_array=[internal_vm])
        self.validate_Internal_LB_Rule(int_lb_rule_1, state="Active", vm_array=[internal_vm])
        int_lb_rule_2 = self.create_Internal_LB_Rule(internal_tier,
                                                     vm_array=[internal_vm],
                                                     services=self.test_data["internal_lbrule_http"],
                                                     source_ip=int_lb_rule_1.sourceipaddress
                                                     )
        self.validate_Internal_LB_Rule(int_lb_rule_2, state="Active", vm_array=[internal_vm])

        # Validating InternalLbVm deployment and state
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # Deploying more VMs in the Internal tier
        self.debug("Deploying two more VMs in network - %s" % internal_tier.name)
        internal_vm_1 = self.create_VM(internal_tier)
        internal_vm_2 = self.create_VM(internal_tier)

        # Adding newly deployed VMs to the created Internal LB rules
        self.debug("Adding two more virtual machines to the created Internal LB rules...")
        int_lb_rule_1.assign(self.api_client, [internal_vm_1, internal_vm_2])
        self.validate_Internal_LB_Rule(int_lb_rule_1, state="Active",
                                       vm_array=[internal_vm, internal_vm_1, internal_vm_2])
        int_lb_rule_2.assign(self.api_client, [internal_vm_1, internal_vm_2])
        self.validate_Internal_LB_Rule(int_lb_rule_2, state="Active",
                                       vm_array=[internal_vm, internal_vm_1, internal_vm_2])

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # Adding Network ACL rules in the Internal tier
        self.debug("Adding Network ACL rules to make the created Internal LB rules (SSH & HTTP) accessible...")
        self.create_NetworkAclRule(self.test_data["ingress_rule"], internal_tier)
        self.create_NetworkAclRule(self.test_data["http_rule"], internal_tier)

        # Creating Static NAT rule for the VM in the Public tier
        public_ip = self.acquire_PublicIPAddress()
        self.create_StaticNatRule_For_VM(public_vm, public_ip, public_tier)

        # Adding Network ACL rule in the Public tier
        self.debug("Adding Network ACL rule to make the created NAT rule (SSH) accessible...")
        self.create_NetworkAclRule(self.test_data["ingress_rule"], public_tier)

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        self.wget_from_vm_cmd(ssh_client,
                              int_lb_rule_1.sourceipaddress,
                              self.test_data["http_rule"]["publicport"]
                              )

        # Restart Internal tier (cleanup = false)
        # InternalLbVm gets destroyed and deployed again in the Internal tier
        self.debug("Restarting the Internal tier without cleanup...")
        Network.restart(internal_tier, self.api_client, cleanup=False)

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        tries = 0
        while tries < 10:
            try:
                self.wget_from_vm_cmd(ssh_client,
                                      int_lb_rule_1.sourceipaddress,
                                      self.test_data["http_rule"]["publicport"]
                                      )
            except Exception as e:
                self.debug("Failed to wget file via the InternalLbVm after re-starting the Internal tier: %s" % e)
                self.debug("Waiting for the InternalLbVm in the Internal tier to be fully resolved for (wget) traffic "
                           "test...")
                time.sleep(30)
                tries += 1
                continue
            self.debug("Internal LB (wget) traffic test is successful after re-starting the Internal tier")
            break

        # Restart Internal tier (cleanup = true)
        # InternalLbVm gets destroyed and deployed again in the Internal tier
        self.debug("Restarting the Internal tier with cleanup...")
        Network.restart(internal_tier, self.api_client, cleanup=True)

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        tries = 0
        while tries < 10:
            try:
                self.wget_from_vm_cmd(ssh_client,
                                      int_lb_rule_1.sourceipaddress,
                                      self.test_data["http_rule"]["publicport"]
                                      )
            except Exception as e:
                self.debug("Failed to wget file via the InternalLbVm after re-starting the Internal tier with cleanup: "
                           "%s" % e)
                self.debug("Waiting for the InternalLbVm in the Internal tier to be fully resolved for (wget) traffic "
                           "test...")
                time.sleep(30)
                tries += 1
                continue
            self.debug("Internal LB (wget) traffic test is successful after re-starting the Internal tier with cleanup")
            break

        # Restart Public tier (cleanup = false)
        # This restart has no effect on the InternalLbVm functionality
        self.debug("Restarting the Public tier without cleanup...")
        Network.restart(public_tier, self.api_client, cleanup=False)

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        self.wget_from_vm_cmd(ssh_client,
                              int_lb_rule_1.sourceipaddress,
                              self.test_data["http_rule"]["publicport"]
                              )

        # Restart Public tier (cleanup = true)
        # This restart has no effect on the InternalLbVm functionality
        self.debug("Restarting the Public tier with cleanup...")
        Network.restart(public_tier, self.api_client, cleanup=True)

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        self.wget_from_vm_cmd(ssh_client,
                              int_lb_rule_1.sourceipaddress,
                              self.test_data["http_rule"]["publicport"]
                              )

        # Stopping VMs in the Internal tier
        # wget traffic test fails as all the VMs in the Internal tier are in stopped state
        self.debug("Stopping all the VMs in the Internal tier...")
        internal_vm.stop(self.api_client)
        internal_vm_1.stop(self.api_client)
        internal_vm_2.stop(self.api_client)

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        with self.assertRaises(Exception):
            self.wget_from_vm_cmd(ssh_client,
                                  int_lb_rule_1.sourceipaddress,
                                  self.test_data["http_rule"]["publicport"]
                                  )
        self.debug("Failed to wget file as all the VMs in the Internal tier are in stopped state")

        # Starting VMs in the Internal tier
        # wget traffic test succeeds as all the VMs in the Internal tier are back in running state
        self.debug("Starting all the VMs in the Internal tier...")
        internal_vm.start(self.api_client)
        internal_vm_1.start(self.api_client)
        internal_vm_2.start(self.api_client)

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        tries = 0
        while tries < 10:
            try:
                self.wget_from_vm_cmd(ssh_client,
                                      int_lb_rule_1.sourceipaddress,
                                      self.test_data["http_rule"]["publicport"]
                                      )
            except Exception as e:
                self.debug("Failed to wget file via the InternalLbVm after re-starting all the VMs in the Internal tier"
                           ": %s" % e)
                self.debug("Waiting for the InternalLbVm and all the VMs in the Internal tier to be fully resolved for "
                           "(wget) traffic test...")
                time.sleep(30)
                tries += 1
                continue
            self.debug("Internal LB (wget) traffic test is successful after re-starting all the VMs in the Internal "
                       "tier")
            break

        # Restarting VPC (cleanup = false)
        # VPC VR gets destroyed and deployed again in the VPC
        # This restart has no effect on the InternalLbVm functionality
        self.debug("Restarting the VPC without cleanup...")
        self.restart_Vpc(self.vpc, cleanup=False)

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        self.wget_from_vm_cmd(ssh_client,
                              int_lb_rule_1.sourceipaddress,
                              self.test_data["http_rule"]["publicport"]
                              )

        # Restarting VPC (cleanup = true)
        # VPC VR gets destroyed and deployed again in the VPC
        # This restart has no effect on the InternalLbVm functionality
        self.debug("Restarting the VPC with cleanup...")
        self.restart_Vpc(self.vpc, cleanup=True)

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        self.wget_from_vm_cmd(ssh_client,
                              int_lb_rule_1.sourceipaddress,
                              self.test_data["http_rule"]["publicport"]
                              )

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_04_internallb_appliance_operations_traffic(self):
        """Test VPC Network Internal LB functionality with InternalLbVm appliance operations by performing (wget)
        traffic tests within a VPC
        """

        # Repeat the tests in the testcase "test_02_internallb_rules_traffic" with InternalLbVm appliance operations:
        # 1. Verify the InternalLbVm deployment by creating the Internal LB Rules when the VPC VR is in Stopped state,
        #    VPC VR has no effect on the InternalLbVm functionality.
        # 2. Stop the InternalLbVm when the VPC VR is in Stopped State
        # 3. Start the InternalLbVm when the VPC VR is in Stopped state
        # 4. Stop the InternalLbVm when the VPC VR is in Running State
        # 5. Start the InternalLbVm when the VPC VR is in Running state
        # 6. Force stop the InternalLbVm when the VPC VR is in Running State
        # 7. Start the InternalLbVm when the VPC VR is in Running state
        # Verify the above restarts of VPC networks by performing (wget) traffic tests within a VPC.

        # Creating VPC networks in the VPC, and deploying VMs
        self.debug("Creating a VPC network with Internal LB service...")
        internal_tier = self.create_Network(self.net_off_1, gateway='10.1.1.1')

        self.debug("Deploying a VM in the created VPC network...")
        internal_vm = self.create_VM(internal_tier)

        self.debug("Creating a VPC network without Internal LB service...")
        public_tier = self.create_Network(self.net_off_2, gateway='10.1.2.1')

        self.debug("Deploying a VM in the created VPC network...")
        public_vm = self.create_VM(public_tier)

        # Stopping the VPC VR
        # VPC VR has no effect on the InternalLbVm functionality
        vpc_vr = self.get_Router(internal_tier)
        Router.stop(self.api_client, id=vpc_vr.id)

        # Creating Internal LB Rules in the Internal tiers
        self.debug("Creating three Internal LB Rules (SSH & HTTP) using the same Load Balancing source IP Address...")
        int_lb_rule_1 = self.create_Internal_LB_Rule(internal_tier, vm_array=[internal_vm])
        self.validate_Internal_LB_Rule(int_lb_rule_1, state="Active", vm_array=[internal_vm])
        int_lb_rule_2 = self.create_Internal_LB_Rule(internal_tier,
                                                     vm_array=[internal_vm],
                                                     services=self.test_data["internal_lbrule_http"],
                                                     source_ip=int_lb_rule_1.sourceipaddress
                                                     )
        self.validate_Internal_LB_Rule(int_lb_rule_2, state="Active", vm_array=[internal_vm])

        # Validating InternalLbVm deployment and state
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # Deploying more VMs in the Internal tier
        self.debug("Deploying two more VMs in network - %s" % internal_tier.name)
        internal_vm_1 = self.create_VM(internal_tier)
        internal_vm_2 = self.create_VM(internal_tier)

        # Adding newly deployed VMs to the created Internal LB rules
        self.debug("Adding two more virtual machines to the created Internal LB rules...")
        int_lb_rule_1.assign(self.api_client, [internal_vm_1, internal_vm_2])
        self.validate_Internal_LB_Rule(int_lb_rule_1, state="Active",
                                       vm_array=[internal_vm, internal_vm_1, internal_vm_2])
        int_lb_rule_2.assign(self.api_client, [internal_vm_1, internal_vm_2])
        self.validate_Internal_LB_Rule(int_lb_rule_2, state="Active",
                                       vm_array=[internal_vm, internal_vm_1, internal_vm_2])

        # Validating InternalLbVm state
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # Adding Network ACL rules in the Internal tier
        self.debug("Adding Network ACL rules to make the created Internal LB rules (SSH & HTTP) accessible...")
        self.create_NetworkAclRule(self.test_data["ingress_rule"], internal_tier)
        self.create_NetworkAclRule(self.test_data["http_rule"], internal_tier)

        # Creating Static NAT rule for the VM in the Public tier
        public_ip = self.acquire_PublicIPAddress()
        self.create_StaticNatRule_For_VM(public_vm, public_ip, public_tier)

        # Adding Network ACL rule in the Public tier
        self.debug("Adding Network ACL rule to make the created NAT rule (SSH) accessible...")
        self.create_NetworkAclRule(self.test_data["ingress_rule"], public_tier)

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        self.wget_from_vm_cmd(ssh_client,
                              int_lb_rule_1.sourceipaddress,
                              self.test_data["http_rule"]["publicport"]
                              )

        # # Stopping the InternalLbVm when the VPC VR is in Stopped state
        int_lb_vm = self.get_InternalLbVm(internal_tier, int_lb_rule_1.sourceipaddress)
        self.stop_InternalLbVm(int_lb_vm)
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Stopped")

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        with self.assertRaises(Exception):
            self.wget_from_vm_cmd(ssh_client,
                                  int_lb_rule_1.sourceipaddress,
                                  self.test_data["http_rule"]["publicport"]
                                  )
        self.debug("Failed to wget file as the InternalLbVm is in stopped state")

        # # Starting the InternalLbVm when the VPC VR is in Stopped state
        self.start_InternalLbVm(int_lb_vm)
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        tries = 0
        while tries < 10:
            try:
                self.wget_from_vm_cmd(ssh_client,
                                      int_lb_rule_1.sourceipaddress,
                                      self.test_data["http_rule"]["publicport"]
                                      )
            except Exception as e:
                self.debug("Failed to wget file via the InternalLbVm after re-starting the InternalLbVm appliance: %s"
                           % e)
                self.debug("Waiting for the InternalLbVm to be fully resolved for (wget) traffic test...")
                time.sleep(30)
                tries += 1
                continue
            self.debug("Internal LB (wget) traffic test is successful after re-starting the InternalLbVm appliance")
            break

        # Starting the VPC VR
        # VPC VR has no effect on the InternalLbVm functionality
        Router.start(self.api_client, id=vpc_vr.id)

        # # Stopping the InternalLbVm when the VPC VR is in Running state
        self.stop_InternalLbVm(int_lb_vm)
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Stopped")

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        with self.assertRaises(Exception):
            self.wget_from_vm_cmd(ssh_client,
                                  int_lb_rule_1.sourceipaddress,
                                  self.test_data["http_rule"]["publicport"]
                                  )
        self.debug("Failed to wget file as the InternalLbVm is in stopped state")

        # # Starting the InternalLbVm when the VPC VR is in Running state
        self.start_InternalLbVm(int_lb_vm)
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        tries = 0
        while tries < 10:
            try:
                self.wget_from_vm_cmd(ssh_client,
                                      int_lb_rule_1.sourceipaddress,
                                      self.test_data["http_rule"]["publicport"]
                                      )
            except Exception as e:
                self.debug("Failed to wget file via the InternalLbVm after re-starting the InternalLbVm appliance: %s"
                           % e)
                self.debug("Waiting for the InternalLbVm to be fully resolved for (wget) traffic test...")
                time.sleep(30)
                tries += 1
                continue
            self.debug("Internal LB (wget) traffic test is successful after re-starting the InternalLbVm appliance")
            break

        # # Force Stopping the InternalLbVm when the VPC VR is in Running state
        self.stop_InternalLbVm(int_lb_vm, force=True)
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Stopped")

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        with self.assertRaises(Exception):
            self.wget_from_vm_cmd(ssh_client,
                                  int_lb_rule_1.sourceipaddress,
                                  self.test_data["http_rule"]["publicport"]
                                  )
        self.debug("Failed to wget file as the InternalLbVm is in stopped state")

        # # Starting the InternalLbVm when the VPC VR is in Running state
        self.start_InternalLbVm(int_lb_vm)
        self.check_InternalLbVm_state(internal_tier, int_lb_rule_1.sourceipaddress, state="Running")

        # Internal LB (wget) traffic test
        ssh_client = self.ssh_into_VM(public_vm, public_ip)
        tries = 0
        while tries < 10:
            try:
                self.wget_from_vm_cmd(ssh_client,
                                      int_lb_rule_1.sourceipaddress,
                                      self.test_data["http_rule"]["publicport"]
                                      )
            except Exception as e:
                self.debug("Failed to wget file via the InternalLbVm after re-starting the InternalLbVm appliance: %s"
                           % e)
                self.debug("Waiting for the InternalLbVm to be fully resolved for (wget) traffic test...")
                time.sleep(30)
                tries += 1
                continue
            self.debug("Internal LB (wget) traffic test is successful after re-starting the InternalLbVm appliance")
            break
