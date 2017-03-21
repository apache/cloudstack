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

""" Component Tests for VpcInlineLbVm Functionality
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.base import (Hypervisor,
                             Network,
                             NetworkACL,
                             NetworkOffering,
                             NetworkServiceProvider,
                             PhysicalNetwork,
                             PublicIPAddress,
                             Router,
                             ServiceOffering,
                             StaticNATRule,
                             VirtualMachine,
                             VPC,
                             Account,
                             LoadBalancerRule,
                             VpcOffering)
from marvin.lib.common import (get_domain,
                               get_template,
                               get_zone)
from marvin.cloudstackAPI import (listVpcInlineLoadBalancerVMs,
                                  restartVPC,
                                  stopVpcInlineLoadBalancerVM,
                                  startVpcInlineLoadBalancerVM
                                  )
from marvin.lib.utils import cleanup_resources
# Import System Modules
from nose.plugins.attrib import attr
from retry import retry
import functools
import socket
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


class VpcInlineLbVm(cloudstackTestCase):
    """Test VpcInlineLbVm Functionality
    """

    @classmethod
    def setUpClass(cls):

        # We want to fail quicker, if it's a failure
        socket.setdefaulttimeout(60)

        test_client = super(VpcInlineLbVm, cls).getClsTestClient()
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
                                                      cls.test_data[
                                                          "service_offering"]
                                                      )
        cls._cleanup = [cls.service_offering]

        try:
            physical_networks = PhysicalNetwork.list(
                cls.api_client, zoneid=cls.zone.id)
            print physical_networks
            for pn in physical_networks:
                if pn.isolationmethods == "VLAN":
                    cls.physical_network = pn
                    break
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Warning: Couldn't get configured VLAN "
                                    "network device details: %s" % e)

        # Check if the host hypervisor type is simulator
        hypervisors = Hypervisor.list(cls.api_client, zoneid=cls.zone.id)
        assert hypervisors is not None and len(hypervisors) > 0, \
            "Expected at least one hypervisor"
        cls.isSimulator = any(map(lambda h: h.name == "Simulator",
                                  hypervisors))

        # Enabling VpcInlineLbVm network service provider
        vpcInlineLbProvider = NetworkServiceProvider.list(
            cls.api_client,
            name="VpcInlineLbVm",
            physicalnetworkid=cls.physical_network.id)[0]
        NetworkServiceProvider.update(
            cls.api_client, vpcInlineLbProvider.id, state="Enabled")
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
            key="network_offering_vpcinlinelbvm",
            description="with vpcinlinelbvm")

        self.net_off_no_lb = self.given_a_network_offering(
            key="vpc_network_offering",
            description="without vpcinlinelbvm")

        self.acl_rules = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            cls.debug("Warning: Exception during cleanup: %s" % e)
        return

    def tearDown(self):
        # Cleanup resources used
        self.debug("Cleaning up the resources")
        for obj in reversed(self.cleanup):
            try:
                if isinstance(obj, VirtualMachine):
                    obj.delete(self.api_client, expunge=True)
                else:
                    obj.delete(self.api_client)
            except Exception as e:
                self.error("Failed to cleanup %s, got %s" % (obj, e))
        # cleanup_resources(self.api_client, self.cleanup)
        self.cleanup = []
        self.debug("Cleanup complete!")
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

    # wget_from_server - Fetches file with the given file name from a web
    # server listening on the given public IP address and port
    def wget_from_server(self, public_ip, port=80, file_name="index.html",
                         disable_system_proxies=True):
        if not self.isSimulator:
            import urllib
            if disable_system_proxies:
                urllib.getproxies = lambda: {}
            self.debug("wget file - %s from a http web server listening on "
                       "public IP address - %s and port - %s" %
                       (file_name, public_ip.ipaddress.ipaddress, port))
            filename, headers = urllib.urlretrieve(
                "http://%s:%s/%s" %
                (public_ip.ipaddress.ipaddress, port, file_name)
            )
            self.debug("Successful to wget file - %s from a http web server "
                       "listening on public IP address - %s and port - %s" %
                       (file_name, public_ip.ipaddress.ipaddress, port))
            return filename, headers

    # validate_NetworkServiceProvider - Validates the given Network Service
    # Provider in the Physical Network, matches the given provider name and
    # state against the list of providers fetched
    def validate_NetworkServiceProvider(self, provider_name, state=None):
        """Validates the Network Service Provider in the Physical Network"""
        self.debug(
            "Check if the Network Service Provider is created successfully ?")
        providers = NetworkServiceProvider.list(
            self.api_client,
            name=provider_name,
            physicalnetworkid=self.physical_network.id)
        self.assertEqual(isinstance(providers, list), True,
                         "List Network Service Provider should return a "
                         "valid list"
                         )
        self.assertEqual(provider_name, providers[0].name,
                         "Name of the Network Service Provider should match "
                         "with the returned list data"
                         )
        if state:
            self.assertEqual(providers[0].state, state,
                             "Network Service Provider state should be in "
                             "state - %s" % state
                             )
        self.debug(
            "Network Service Provider creation successfully validated for %s"
            % provider_name)

    # create_VpcOffering - Creates VPC offering
    def create_VpcOffering(self, vpc_offering, suffix=None):
        self.debug("Creating VPC offering")
        if suffix:
            vpc_offering["name"] = "VPC_OFF-" + str(suffix)
        vpc_off = VpcOffering.create(self.api_client,
                                     vpc_offering
                                     )
        # Enable VPC offering
        vpc_off.update(self.api_client, state="Enabled")
        self.cleanup.append(vpc_off)
        self.debug("Created and Enabled VPC offering")
        return vpc_off

    # create_Vpc - Creates VPC with the given VPC offering
    def create_Vpc(self, vpc_offering, cidr="10.1.1.1/16", account=None,
                   cleanup=True):
        if not account:
            account = self.account
        self.debug("Creating a VPC in the account - %s" % account.name)
        self.test_data["vpc"]["name"] = "TestVPC"
        self.test_data["vpc"]["displaytext"] = "TestVPC"
        self.test_data["vpc"]["cidr"] = cidr
        vpc = VPC.create(self.api_client,
                         self.test_data["vpc"],
                         vpcofferingid=vpc_offering.id,
                         zoneid=self.zone.id,
                         account=account.name,
                         domainid=account.domainid
                         )
        self.debug("Created VPC with ID - %s" % vpc.id)
        if cleanup:
            self.cleanup.append(vpc)
        return vpc

    # restart_Vpc - Restarts the given VPC with/without cleanup
    def restart_Vpc(self, vpc, cleanup=False):
        self.debug("Restarting VPC with ID - %s" % vpc.id)
        cmd = restartVPC.restartVPCCmd()
        cmd.id = vpc.id
        cmd.cleanup = cleanup
        cmd.makeredundant = False
        self.api_client.restartVPC(cmd)
        self.debug("Restarted VPC with ID - %s" % vpc.id)

    # create_NetworkOffering - Creates Network offering
    def create_NetworkOffering(self, net_offering, suffix=None,
                               conserve_mode=False):
        self.debug("Creating Network offering")
        if suffix:
            net_offering["name"] = "NET_OFF-" + str(suffix)
        nw_off = NetworkOffering.create(self.api_client,
                                        net_offering,
                                        conservemode=conserve_mode
                                        )
        # Enable Network offering
        nw_off.update(self.api_client, state="Enabled")
        self.cleanup.append(nw_off)
        self.debug("Created and Enabled Network offering")
        return nw_off

    # create_Network - Creates network with the given Network offering
    def create_Network(self, nw_off, gateway="10.1.1.1",
                       netmask="255.255.255.0", vpc=None, testdata=None,
                       account=None, acl_list=None):
        if not account:
            account = self.account
        self.debug("Creating a network in the account - %s" % account.name)
        if not testdata:
            testdata = self.test_data["network"]
            testdata["name"] = "TestNet-" + gateway + "-" + str(nw_off.name)
            testdata["displaytext"] = "Test Network"
            testdata["netmask"] = netmask
        network = Network.create(self.api_client,
                                 testdata,
                                 accountid=account.name,
                                 domainid=account.domainid,
                                 networkofferingid=nw_off.id,
                                 zoneid=self.zone.id,
                                 gateway=gateway,
                                 vpcid=vpc.id if vpc else self.vpc.id
                                 if hasattr(self, "vpc") else None,
                                 aclid=acl_list.id if acl_list else None
                                 )
        self.debug("Created network with ID - %s" % network.id)
        self.cleanup.append(network)
        return network

    # validate_NetworkOffering - Validates the given Network offering,
    # matches the given network offering name and state against the list of
    # network offerings fetched
    def validate_NetworkOffering(self, net_offering, state=None):
        """Validates the Network offering"""
        self.debug("Check if the Network offering is created successfully ?")
        net_offs = NetworkOffering.list(self.api_client,
                                        id=net_offering.id
                                        )
        self.assertEqual(isinstance(net_offs, list), True,
                         "List Network offering should return a valid list"
                         )
        self.assertEqual(net_offering.name, net_offs[0].name,
                         "Name of the Network offering should match with the "
                         "returned list data"
                         )
        if state:
            self.assertEqual(net_offs[0].state, state,
                             "Network offering state should be in state - %s"
                             % state
                             )
        self.debug(
            "Network offering creation successfully validated for %s" %
            net_offering.name)

    # validate_VpcOffering - Validates the given VPC offering,
    # matches the given VPC offering name and state against the list of VPC
    # offerings fetched
    def validate_VpcOffering(self, vpc_offering, state=None):
        """Validates the VPC offering"""
        self.debug("Check if the VPC offering is created successfully ?")
        vpc_offs = VpcOffering.list(self.api_client,
                                    id=vpc_offering.id
                                    )
        self.assertEqual(isinstance(vpc_offs, list), True,
                         "List VPC offering should return a valid list"
                         )
        self.assertEqual(vpc_offering.name, vpc_offs[0].name,
                         "Name of the VPC offering should match with the "
                         "returned list data"
                         )
        if state:
            self.assertEqual(vpc_offs[0].state, state,
                             "VPC offering state should be in state - %s" %
                             state
                             )
        self.debug("VPC offering creation successfully validated for %s" %
                   vpc_offering.name)

    # validate_Vpc - Validates the given VPC,
    # matches the given VPC name and state against the list of VPCs fetched
    def validate_Vpc(self, vpc, state=None):
        """Validates the VPC"""
        self.debug("Check if the VPC is created successfully ?")
        vpcs = VPC.list(self.api_client,
                        id=vpc.id
                        )
        self.assertEqual(isinstance(vpcs, list), True,
                         "List VPC should return a valid list"
                         )
        self.assertEqual(vpc.name, vpcs[0].name,
                         "Name of the VPC should match with the returned list "
                         "data"
                         )
        if state:
            self.assertEqual(vpcs[0].state, state,
                             "VPC state should be in state - %s" % state
                             )
        self.debug("VPC creation successfully validated for %s" % vpc.name)

    # validate_Network - Validates the given network,
    # matches the given network name and state against the list of networks
    # fetched
    def validate_Network(self, network, state=None):
        """Validates the Network"""
        self.debug("Check if the network is created successfully ?")
        networks = Network.list(self.api_client,
                                id=network.id
                                )
        self.assertEqual(isinstance(networks, list), True,
                         "List network should return a valid list"
                         )
        self.assertEqual(network.name, networks[0].name,
                         "Name of the network should match with with the "
                         "returned list data"
                         )
        if state:
            self.assertEqual(networks[0].state, state,
                             "Network state should be in state - %s" % state
                             )
        self.debug("Network creation successfully validated for %s" %
                   network.name)

    # check_VM_state - Checks if the given VM is in the expected state form
    # the list of fetched VMs
    def check_VM_state(self, vm, state=None):
        """Validates the VM state"""
        self.debug("Check if the VM instance is in state - %s" % state)
        vms = VirtualMachine.list(self.api_client,
                                  id=vm.id,
                                  listall=True
                                  )
        self.assertEqual(isinstance(vms, list), True,
                         "List virtual machine should return a valid list"
                         )
        if state:
            self.assertEqual(vms[0].state, state,
                             "Virtual machine is not in the expected state"
                             )
        self.debug("Virtual machine instance - %s is in the expected state - "
                   "%s" % (vm.name, state))

    # check_Router_state - Checks if the given router is in the expected state
    # form the list of fetched routers
    def check_Router_state(self, router, state=None):
        """Validates the Router state"""
        self.debug(
            "Check if the virtual router instance is in state - %s" % state)
        routers = Router.list(self.api_client,
                              id=router.id,
                              listall=True
                              )
        self.assertEqual(isinstance(routers, list), True,
                         "List router should return a valid list"
                         )
        if state:
            self.assertEqual(routers[0].state, state,
                             "Virtual router is not in the expected state"
                             )
        self.debug("Virtual router instance - %s is in the expected state - %s"
                   % (router.name, state))

    # validate_PublicIPAddress - Validates if the given public IP address is in
    # the expected state form the list of fetched public IP addresses
    def validate_PublicIPAddress(self, public_ip, network, static_nat=False,
                                 vm=None):
        """Validates the Public IP Address"""
        self.debug(
            "Check if the public IP is successfully assigned to the network ?")
        public_ips = PublicIPAddress.list(self.api_client,
                                          id=public_ip.ipaddress.id,
                                          networkid=network.id,
                                          isstaticnat=static_nat,
                                          listall=True
                                          )
        self.assertEqual(isinstance(public_ips, list), True,
                         "List public IP for network should return a valid "
                         "list"
                         )
        self.assertEqual(public_ips[0].ipaddress,
                         public_ip.ipaddress.ipaddress,
                         "List public IP for network should list the assigned "
                         "public IP address"
                         )
        self.assertEqual(public_ips[0].state, "Allocated",
                         "Assigned public IP is not in the allocated state"
                         )
        if static_nat and vm:
            self.assertEqual(public_ips[0].virtualmachineid, vm.id,
                             "Static NAT Rule not enabled for the VM using "
                             "the assigned public IP"
                             )
        self.debug("Assigned Public IP address - %s is successfully validated"
                   % public_ip.ipaddress.ipaddress)

    # delete_Network - Deletes the given network
    def delete_Network(self, network):
        self.debug("Deleting Network with ID - %s" % network.id)
        network.delete(self.api_client)
        if network in self.cleanup:
            self.cleanup.remove(network)
        self.debug("Deleted Network with ID - %s" % network.id)

    # create_VM - Creates VM in the given network, vm_key - Key for the
    # services on the VM
    def create_VM(self, network, vm_key="virtual_machine", host_id=None,
                  start_vm=True):
        self.debug("Creating VM in network with ID - %s" % network.id)
        self.debug("Passed vm_key - %s" % vm_key)
        self.test_data[vm_key]["zoneid"] = self.zone.id
        self.test_data[vm_key]["template"] = self.template.id
        vm = VirtualMachine.create(self.api_client,
                                   self.test_data[vm_key],
                                   accountid=self.account.name,
                                   domainid=self.account.domainid,
                                   serviceofferingid=self.service_offering.id,
                                   networkids=[str(network.id)],
                                   startvm=start_vm,
                                   hostid=host_id
                                   )
        self.debug("Created VM with ID - %s in network with ID - %s" %
                   (vm.id, network.id))
        self.cleanup.append(vm)
        return vm

    # delete_VM - Deletes the given VM
    def delete_VM(self, vm, expunge=True):
        self.debug("Deleting VM with ID - %s" % vm.id)
        vm.delete(self.api_client, expunge=expunge)
        if vm in self.cleanup:
            self.cleanup.remove(vm)
        self.debug("Deleted VM with ID - %s" % vm.id)

    # get_Router - Returns router for the given network
    def get_Router(self, network):
        self.debug(
            "Finding the virtual router for network with ID - %s" % network.id)
        routers = Router.list(self.api_client,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(isinstance(routers, list), True,
                         "List routers should return a valid virtual router "
                         "for network"
                         )
        return routers[0]

    # create_NetworkAclRule - Creates Ingress/Egress network ACL rule in the
    # given network/acl list
    def create_NetworkAclRule(self, rule, traffic_type="Ingress",
                              network=None, acl_list=None):
        self.debug("Adding NetworkACL rule - %s" % rule)
        if acl_list:
            return NetworkACL.create(self.api_client,
                                     networkid=network.id if network else None,
                                     services=rule,
                                     traffictype=traffic_type,
                                     aclid=acl_list.id
                                     )
        else:
            return NetworkACL.create(self.api_client,
                                     networkid=network.id if network else None,
                                     services=rule,
                                     traffictype=traffic_type
                                     )

    # acquire_PublicIPAddress - Acquires public IP address for the given
    # network/VPC
    def acquire_PublicIPAddress(self, network, vpc=None):
        self.debug("Associating public IP for network with ID - %s" %
                   network.id)
        public_ip = PublicIPAddress.create(
            self.api_client,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network.id if vpc is None else None,
            vpcid=vpc.id if vpc else self.vpc.id
            if hasattr(self, "vpc") else None
            )
        self.debug("Associated public IP address - %s with network with ID - "
                   "%s" % (public_ip.ipaddress.ipaddress, network.id))
        return public_ip

    # create_StaticNatRule_For_VM - Creates Static NAT rule on the given
    # public IP for the given VM in the given network
    def create_StaticNatRule_For_VM(self, vm, public_ip, network,
                                    vmguestip=None):
        self.debug("Enabling Static NAT rule on public IP - %s for VM with ID "
                   "- %s in network with ID - %s" %
                   (public_ip.ipaddress.ipaddress, vm.id, network.id))
        static_nat_rule = StaticNATRule.enable(
            self.api_client,
            ipaddressid=public_ip.ipaddress.id,
            virtualmachineid=vm.id,
            networkid=network.id,
            vmguestip=vmguestip
        )
        self.debug("Static NAT rule enabled on public IP - %s for VM with ID "
                   "- %s in network with ID - %s" %
                   (public_ip.ipaddress.ipaddress, vm.id, network.id))
        return static_nat_rule

    # ssh_into_VM - Gets into the shell of the given VM using its public IP
    def ssh_into_VM(self, vm, public_ip, reconnect=True, negative_test=False):
        if not self.isSimulator:
            self.debug("SSH into VM with ID - %s on public IP address - %s" %
                       (vm.id, public_ip.ipaddress.ipaddress))
            tries = 1 if negative_test else 3

            @retry(tries=tries)
            def retry_ssh():
                ssh_client = vm.get_ssh_client(
                    ipaddress=public_ip.ipaddress.ipaddress,
                    reconnect=reconnect,
                    retries=3 if negative_test else 30
                )
                self.debug("Successful to SSH into VM with ID - %s on "
                           "public IP address - %s" %
                           (vm.id, public_ip.ipaddress.ipaddress))
                return ssh_client

            return retry_ssh()

    # delete_StaticNatRule_For_VM - Deletes Static NAT rule on the given
    # public IP
    def delete_StaticNatRule_For_VM(self, public_ip):
        self.debug("Disabling Static NAT rule on public IP - %s" %
                   public_ip.ipaddress.ipaddress)
        StaticNATRule.disable(self.api_client,
                              ipaddressid=public_ip.ipaddress.id
                              )
        self.debug("Static NAT rule disabled on public IP - %s" %
                   public_ip.ipaddress.ipaddress)

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

        # Adding Network ACL rule in the given VPC network
        self.debug("Creating Network ACL rule ...")
        test_public_ssh_rule = self.create_NetworkAclRule(
            self.test_data["ingress_rule"], network=network)

        # SSH into VM
        self.debug("Verifying VM ingress traffic (SSH into VM)...")
        # self.ssh_into_VM(vm, test_public_ip)

        # Removing Network ACL rule in the given VPC network
        self.debug("Removing the created Network ACL rule...")
        test_public_ssh_rule.delete(self.api_client)

        # Deleting Static NAT Rule
        self.debug("Deleting the created Static NAT Rule...")
        self.delete_StaticNatRule_For_VM(test_public_ip)
        with self.assertRaises(Exception):
            self.validate_PublicIPAddress(
                test_public_ip, network, static_nat=True, vm=vm)
        self.debug("Static NAT Rule successfully deleted in CloudStack")

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
        self.debug("Validating network service providers supported for "
                   "VpcInline LB functionality")
        providers = ["VpcVirtualRouter", "VpcInlineLbVm"]
        for provider in providers:
            self.validate_NetworkServiceProvider(provider, state="Enabled")

    @gherkin
    def given_a_vpc_offering_with_vpcinlinelb(self):
        # Creating VPC offering
        self.debug("Creating VPC offering with LB service provider as "
                   "vpcinlinelbvm")
        vpc_off = self.create_VpcOffering(
            self.test_data["vpc_offering_multi_lb"])
        self.validate_VpcOffering(vpc_off, state="Enabled")
        return vpc_off

    @gherkin
    def given_a_network_offering(self, testdata=None, key=None,
                                 description=None):
        if not testdata:
            testdata = self.test_data[key]
        if not description:
            description = testdata["name"]

        # Creating network offering
        self.debug("Creating VPC Network offering %s" % description)
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
    def when_i_stop_vr(self, network_array, vm_array):
        # Stopping VR
        self.debug("Stopping VR...")
        for network in network_array:
            self.validate_Network(network, state="Implemented")
            vr = self.get_Router(network)
            Router.stop(self.api_client, id=vr.id)
            self.check_Router_state(vr, state="Stopped")
        for vm in vm_array:
            self.check_VM_state(vm, state="Running")

    @gherkin
    def when_i_start_vr(self, network_array, vm_array):
        # Starting VR
        self.debug("Starting VR...")
        for network in network_array:
            self.validate_Network(network, state="Implemented")
            vr = self.get_Router(network)
            Router.start(self.api_client, id=vr.id)
            self.check_Router_state(vr, state="Running")
        for vm in vm_array:
            self.check_VM_state(vm, state="Running")

    @gherkin
    def when_i_stop_vms(self, network, vm_array):
        # Stopping VMs
        self.debug("Stopping VMs...")
        for vm in vm_array:
            vm.stop(self.api_client)
            self.check_VM_state(vm, state="Stopped")
        self.validate_Network(network, state="Implemented")
        vr = self.get_Router(network)
        self.check_Router_state(vr, state="Running")

    @gherkin
    def when_i_start_vms(self, network, vm_array):
        # Starting VMs
        self.debug("Starting VMs...")
        for vm in vm_array:
            vm.start(self.api_client)
            self.check_VM_state(vm, state="Running")
        self.validate_Network(network, state="Implemented")
        vr = self.get_Router(network)
        self.check_Router_state(vr, state="Running")

    @gherkin
    def when_i_restart_network(self, network, vm_array, cleanup=False):
        # Restarting network without cleanup
        self.debug("Restarting network without cleanup...")
        self.restart_network(network, cleanup=cleanup)
        self.validate_Network(network, state="Implemented")
        vr = self.get_Router(network)
        self.check_Router_state(vr, state="Running")
        for vm in vm_array:
            self.check_VM_state(vm, state="Running")

    @gherkin
    def when_i_restart_vpc(self, vpc, network_array, vm_array, cleanup=False):
        # Restarting VPC without cleanup
        self.debug("Restarting VPC without cleanup...")
        self.restart_Vpc(vpc, cleanup=cleanup)
        for network in network_array:
            self.validate_Network(network, state="Implemented")
            vr = self.get_Router(network)
            self.check_Router_state(vr, state="Running")
        for vm in vm_array:
            self.check_VM_state(vm, state="Running")

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
        return lb_vm

    @gherkin
    def then_i_have_a_stopped_vpcinlinelbvm(self, lb_tier, vpc, lb_ips):
        expected_public_ips = [lb_ip.ipaddress.ipaddress for lb_ip in lb_ips]

        # Validating VpcInlineLbVm deployment and state
        lb_vm = self.get_VpcInlineLbVm(vpc, lb_tier, expected_public_ips)
        self.check_VpcInlineLbVm_state(lb_vm, state="Stopped")
        return lb_vm

    @gherkin
    def then_vpcinlinelbvm_is_removed(self, lb_tier, vpc, lb_ips):
        expected_public_ips = [lb_ip.ipaddress.ipaddress for lb_ip in lb_ips]

        # Validating VpcInlineLbVm deletion
        with self.assertRaises(Exception):
            self.get_VpcInlineLbVm(vpc, lb_tier, expected_public_ips)
        self.debug("LB appliance is successfully deleted")

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

    # =========================================================================
    #                            TEST CASES
    # =========================================================================
    @attr(tags=["advanced"], required_hardware="false")
    def test_01_vpcinlinelbvm(self):
        """Test VpcInlineLbVm basic functionality
        """

        # 1. Create VPC offering with VpcInlineLb LB service, check if it is
        #    successfully created and enabled.
        # 2. Create a VPC with VpcInlineLb LB service, check if it is
        #    successfully created and enabled.
        # 3. Create VPC Network offering with VpcInlineLb LB service, check if
        #    it is successfully created and enabled.
        # 4. Create VPC Network offering without VpcInlineLb LB service, check
        #    if it is successfully created and enabled.
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

    @attr(tags=["advanced"], required_hardware="true")
    def test_02_vpcinlinelbvm_with_traffic(self):
        """Test VpcInlineLbVm basic functionality with traffic
        """

        # 1. Create VPC offering with VpcInlineLb LB service, check if it is
        #    successfully created and enabled.
        # 2. Create a VPC with VpcInlineLb LB service, check if it is
        #    successfully created and enabled.
        # 3. Create VPC Network offering with VpcInlineLb LB service, check if
        #    it is successfully created and enabled.
        # 4. Create VPC Network offering without VpcInlineLb LB service, check
        #    if it is successfully created and enabled.
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
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on(public_ip_1, lb_rule_1)
        self.then_load_balancing_does_not_work_on(public_ip_1, port=70)

        lb_rule_2 = self.when_i_create_a_lb_rule(
            public_ip_1, lb_tier, lb_vm_array, vpc,
            self.test_data["vpclbrulehttp"],
            ingress_acl_rule_testdata=self.test_data["http_rule"])

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip_1])
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

    @attr(tags=["advanced"], required_hardware="true")
    def test_03_vpcinlinelbvm_with_multiple_public_ips_traffic(self):
        """Test VpcInlineLbVm functionality with multiple public IP addresses
        by performing traffic tests
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
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip_1,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_works_on_all(public_ip_2,
                                              lb_rules=[lb_rule_3, lb_rule_4])
        self.then_load_balancing_does_not_work_on(public_ip_1, port=70)
        self.then_load_balancing_does_not_work_on(public_ip_2, port=70)

        self.when_i_release_a_public_ip(public_ip_1, lb_tier)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip_2])
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip_2,
                                              lb_rules=[lb_rule_3, lb_rule_4])
        self.then_load_balancing_does_not_work_on_all(
            public_ip_1, lb_rules=[lb_rule_1, lb_rule_2])

        self.when_i_release_a_public_ip(public_ip_2, lb_tier)

        self.then_vpcinlinelbvm_is_removed(lb_tier, vpc, [public_ip_2])
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
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip_3,
                                              lb_rules=[lb_rule_5, lb_rule_6])
        self.then_load_balancing_works_on_all(public_ip_4,
                                              lb_rules=[lb_rule_7, lb_rule_8])
        self.then_load_balancing_does_not_work_on(public_ip_3, port=70)
        self.then_load_balancing_does_not_work_on(public_ip_4, port=70)

    @attr(tags=["advanced"], required_hardware="true")
    def test_04_vpcinlinelbvm_with_lb_rule_events_traffic(self):
        """Test VpcInlineLbVm functionality with lb rule events by performing
        traffic tests
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
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        lb_vm_array = \
            lb_vm_array + self.when_i_assign_vms_to_a_existing_lb_rule(
                lb_rule_1, lb_tier=lb_tier, number_of_vms=2)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_remove_vms_from_a_existing_lb_rule(lb_rule_1, lb_vm_array)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on(public_ip, lb_rule_2)
        self.then_load_balancing_does_not_work_on(public_ip,
                                                  lb_rule=lb_rule_1)
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_assign_vms_to_a_existing_lb_rule(lb_rule_1, lb_vm_array)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_delete_a_lb_rule(lb_rule_1)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on(public_ip, lb_rule_2)
        self.then_load_balancing_does_not_work_on(public_ip,
                                                  lb_rule=lb_rule_1)
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_delete_a_lb_rule(lb_rule_2)

        self.then_vpcinlinelbvm_is_removed(lb_tier, vpc, [public_ip])
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
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_3, lb_rule_4])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

    @attr(tags=["advanced"], required_hardware="true")
    def test_05_vpcinlinelbvm_with_algorithms_traffic(self):
        """Test VpcInlineLbVm functionality with different LB algorithms by
        performing traffic tests
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
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_update_lb_rule_algorithm(lb_rule_2, "leastconn")

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_update_lb_rule_algorithm(lb_rule_2, "source")

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_update_lb_rule_algorithm(lb_rule_2, "roundrobin")

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

    @attr(tags=["advanced"], required_hardware="true")
    def test_06_vpcinlinelbvm_appliance_operations_traffic(self):
        """Test VpcInlineLbVm functionality with VpcInlineLbVm appliance
        operations by performing traffic tests
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
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_stop_a_vpcInlineLbVm(lb_tier, vpc, [public_ip])

        self.then_i_have_a_stopped_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_load_balancing_does_not_work_on_all(
            public_ip, lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_start_a_vpcInlineLbVm(lb_tier, vpc, [public_ip])

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2],
                                              multiple_retries=True)
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_stop_a_vpcInlineLbVm(lb_tier, vpc, [public_ip], force=True)

        self.then_i_have_a_stopped_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_load_balancing_does_not_work_on_all(
            public_ip, lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_start_a_vpcInlineLbVm(lb_tier, vpc, [public_ip])

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2],
                                              multiple_retries=True)
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

    @attr(tags=["advanced"], required_hardware="true")
    def test_07_vpcinlinelbvm_with_network_restarts_and_reboots(self):
        """Test VpcInlineLbVm provider with network restarts and reboots by
        performing traffic tests
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
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_stop_vr([lb_tier, no_lb_tier], lb_vm_array+no_lb_vm_array)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_does_not_work_on_all(
            public_ip, lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_start_vr([lb_tier, no_lb_tier], lb_vm_array+no_lb_vm_array)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_stop_vms(lb_tier, lb_vm_array)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_load_balancing_does_not_work_on(public_ip,
                                                  lb_rule=lb_rule_1)
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_start_vms(lb_tier, lb_vm_array)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2],
                                              multiple_retries=True)
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_restart_network(lb_tier, lb_vm_array)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2],
                                              multiple_retries=True)
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_restart_network(lb_tier, lb_vm_array, cleanup=True)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2],
                                              multiple_retries=True)
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_restart_network(no_lb_tier, no_lb_vm_array)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_restart_network(no_lb_tier, no_lb_vm_array, cleanup=True)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2])
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_restart_vpc(
            vpc, [lb_tier, no_lb_tier], lb_vm_array+no_lb_vm_array)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2],
                                              multiple_retries=True)
        self.then_load_balancing_does_not_work_on(public_ip, port=70)

        self.when_i_restart_vpc(
            vpc, [lb_tier, no_lb_tier], lb_vm_array+no_lb_vm_array,
            cleanup=True)

        self.then_i_have_a_running_vpcinlinelbvm(lb_tier, vpc, [public_ip])
        self.then_all_load_balanced_vms_are_fully_resolved(
            lb_vm_array, lb_tier, vpc)
        self.then_load_balancing_works_on_all(public_ip,
                                              lb_rules=[lb_rule_1, lb_rule_2],
                                              multiple_retries=True)
        self.then_load_balancing_does_not_work_on(public_ip, port=70)
