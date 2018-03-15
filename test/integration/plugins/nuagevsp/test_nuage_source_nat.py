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

""" Component tests for Source NAT functionality with Nuage VSP SDN plugin
"""
# Import Local Modules
from nuageTestCase import nuageTestCase
from marvin.lib.base import (Account,
                             Network,
                             VirtualMachine)
# Import System Modules
from nose.plugins.attrib import attr
import copy
import time


class TestNuageSourceNat(nuageTestCase):
    """Test Source NAT functionality with Nuage VSP SDN plugin
    """

    @classmethod
    def setUpClass(cls):
        super(TestNuageSourceNat, cls).setUpClass()
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

    # verify_vsd_SourceNAT_network - Verifies if Source NAT functionality of
    # the given network is enabled in VSD
    def verify_vsd_SourceNAT_network(self, network, vpc=None):
        self.debug("Verifying if Source NAT functionality of Network - %s is "
                   "enabled in VSD" % network.name)
        ext_network_id_filter = self.get_externalID_filter(vpc.id) if vpc \
            else self.get_externalID_filter(network.id)
        vsd_domain = self.vsd.get_domain(filter=ext_network_id_filter)
        self.assertEqual(vsd_domain.pat_enabled, "ENABLED",
                         "VSD domain address translation support "
                         "(pat_enabled flag) should be enabled for Source NAT "
                         "service enabled network in CloudStack"
                         )
        self.assertEqual(vsd_domain.underlay_enabled, "ENABLED",
                         "VSD domain underlay support (underlay_enabled flag) "
                         "should be enabled for Source NAT service enabled "
                         "network in CloudStack"
                         )
        self.debug("Successfully verified that Source NAT functionality of "
                   "Network - %s is enabled in VSD" % network.name)

    # verify_SourceNAT_VM_traffic - Verifies Source NAT traffic
    # (wget www.google.com) to the Internet from the given VM. This Source NAT
    # traffic test is done through a custom init script in the guest VM
    # template upon the VM boot up.
    def verify_SourceNAT_VM_traffic(self, vm, network, vpc=None,
                                    negative_test=False):
        self.debug("Verifying Source NAT traffic (wget www.google.com) to the "
                   "Internet from VM - %s" % vm.name)
        if self.isInternetConnectivityAvailable:
            # Adding Egress Network ACL rule
            if vpc and self.http_proxy and not negative_test:
                self.debug("Adding Egress Network ACL rule in the created VPC "
                           "network to allow access to the configured "
                           "Internet proxy servers...")
                proxy_rule = copy.deepcopy(self.test_data["http_rule"])
                proxy_rule["privateport"] = 1080
                proxy_rule["publicport"] = 1080
                proxy_rule["startport"] = 1080
                proxy_rule["endport"] = 1080
                internet_proxy_server_rule = self.create_NetworkAclRule(
                    proxy_rule, traffic_type="Egress", network=network)

                # VSD verification
                self.verify_vsd_firewall_rule(
                    internet_proxy_server_rule, traffic_type="Egress")

            # Triggering Source NAT traffic test
            # Rebooting (stop - start) VM
            self.debug("Triggering the Source NAT traffic test by rebooting "
                       "the given VM...")
            vm.stop(self.api_client)
            vm.start(self.api_client)
            self.check_VM_state(vm, state="Running")

            # VSD verification
            self.verify_vsd_vm(vm)

            # VSD verification for Source NAT functionality
            self.verify_vsd_SourceNAT_network(network, vpc)

            self.debug("Waiting for the VM to perform the Source NAT traffic "
                       "test (wget www.google.com) to the Internet...")
            time.sleep(180) if negative_test else time.sleep(300)

            # Creating Static NAT rule
            self.debug("Creating Static NAT rule to SSH into the VM for "
                       "verifying its Source NAT traffic test...")
            public_ip = self.acquire_PublicIPAddress(network, vpc=vpc)
            self.validate_PublicIPAddress(public_ip, network)
            self.create_StaticNatRule_For_VM(vm, public_ip, network)
            self.validate_PublicIPAddress(
                public_ip, network, static_nat=True, vm=vm)

            # VSD verification
            updated_vm_info = VirtualMachine.list(self.api_client, id=vm.id)[0]
            self.verify_vsd_floating_ip(
                network, updated_vm_info, public_ip.ipaddress, vpc=vpc)

            # Adding Ingress Firewall/Network ACL rule
            self.debug("Adding Ingress Firewall/Network ACL rule to make the "
                       "created Static NAT rule (SSH) accessible...")
            if vpc:
                public_ssh_rule = self.create_NetworkAclRule(
                    self.test_data["ingress_rule"], network=network)
            else:
                public_ssh_rule = self.create_FirewallRule(
                    public_ip, self.test_data["ingress_rule"])

            # VSD verification
            self.verify_vsd_firewall_rule(public_ssh_rule)

            # Checking for wget file
            is_in_file_list = None
            if not self.isSimulator:
                ssh_client = self.ssh_into_VM(vm, public_ip)
                cmd = "ls /"
                file_list = self.execute_cmd(ssh_client, cmd)
                is_in_file_list = "index.html" in str(file_list)
                if is_in_file_list:
                    cmd = "rm -rf /index.html*"
                    self.execute_cmd(ssh_client, cmd)

            # Removing Ingress Firewall/Network ACL rule
            self.debug("Removing the created Ingress Firewall/Network ACL "
                       "rule in the network...")
            public_ssh_rule.delete(self.api_client)

            # VSD verification
            with self.assertRaises(Exception):
                self.verify_vsd_firewall_rule(public_ssh_rule)
            self.debug("Ingress Firewall/Network ACL rule successfully "
                       "deleted in VSD")

            # Deleting Static NAT Rule
            self.debug("Deleting Static NAT Rule for the VM...")
            self.delete_StaticNatRule_For_VM(public_ip)
            with self.assertRaises(Exception):
                self.validate_PublicIPAddress(
                    public_ip, network, static_nat=True, vm=vm)
            self.debug("Static NAT Rule for the VM successfully deleted in "
                       "CloudStack")

            # VSD verification
            with self.assertRaises(Exception):
                self.verify_vsd_floating_ip(
                    network, updated_vm_info, public_ip.ipaddress, vpc=vpc)
            self.debug("Floating IP for the VM successfully deleted in VSD")

            # Releasing acquired public IP
            self.debug("Releasing the acquired public IP in the network...")
            public_ip.delete(self.api_client)
            with self.assertRaises(Exception):
                self.validate_PublicIPAddress(public_ip, network)
            self.debug("Acquired public IP in the network successfully "
                       "released in CloudStack")

            # Removing Egress Network ACL rule
            if vpc and self.http_proxy:
                self.debug("Removing the created Egress Network ACL rule in "
                           "the VPC network...")
                internet_proxy_server_rule.delete(self.api_client)

                # VSD verification
                with self.assertRaises(Exception):
                    self.verify_vsd_firewall_rule(internet_proxy_server_rule)
                self.debug("Egress Network ACL rule successfully deleted in "
                           "VSD")

            # Final test result
            if is_in_file_list:
                self.debug("Successfully verified Source NAT traffic "
                           "(wget www.google.com) to the Internet from VM - %s"
                           % vm.name)
            elif not self.isSimulator:
                self.fail("Failed to verify Source NAT traffic "
                          "(wget www.google.com) to the Internet from VM - %s"
                          % vm.name)
        else:
            if negative_test:
                self.fail("Skipping Source NAT traffic (wget www.google.com) "
                          "verification to the Internet from VM as there is "
                          "no Internet connectivity in the data center")
            else:
                self.debug("Skipping Source NAT traffic (wget www.google.com) "
                           "verification to the Internet from VM as there is "
                           "no Internet connectivity in the data center")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_01_nuage_SourceNAT_isolated_networks(self):
        """Test Nuage VSP Isolated networks with different combinations of
        Source NAT service providers
        """

        # 1. Create Nuage VSP Isolated Network offerings and corresponding
        #    Isolated networks with different combinations of Source NAT
        #    service providers (NuageVsp, VirtualRouter, no SourceNat service),
        #    check if only the Isolated networks with Source NAT service
        #    provider as NuageVsp are successfully created.
        # 2. Recreate the above created Network offering with ispersistent flag
        #    set to True, check if the network offering is successfully created
        #    and enabled.
        # 3. Recreate the above created Network offering with conserve mode On
        #    (conserve_mode flag set to True), check if the network offering is
        #    successfully created and enabled.
        # 4. Recreate the above created Network offering with Source NAT
        #    Service Capability SupportedSourceNatTypes as per account, check
        #    if the network offering creation failed as Nuage VSP supports only
        #    SupportedSourceNatTypes as per zone.
        # 5. Create an Isolated network with Source NAT service provider as
        #    NuageVsp and spawn a VM, check if the network is successfully
        #    created, and the VM along with the VR is deployed successfully in
        #    the network. Verify if the Source NAT functionality for this
        #    network is successfully enabled in VSD.
        # 6. Create a persistent Isolated network with Source NAT service
        #    provider as NuageVsp and spawn a VM, check if the network is
        #    successfully created, and the VM along with the VR is deployed
        #    successfully in the network. Verify if the Source NAT
        #    functionality for this network is successfully enabled in VSD.
        # 7. Create a conserved Isolated network (conserve mode On) with Source
        #    NAT service provider as NuageVsp and spawn a VM, check if the
        #    network is successfully created, and the VM along with the VR is
        #    deployed successfully in the network. Verify if the Source NAT
        #    functionality for this network is successfully enabled in VSD.
        # 8. Delete all the created objects (cleanup).

        # Creating network offerings
        self.debug("Creating Nuage VSP Isolated Network offering with Source "
                   "NAT service provider as NuageVsp...")
        net_off_1 = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["isolated_network_offering"])
        self.validate_NetworkOffering(net_off_1, state="Enabled")

        self.debug("Recreating above Network offering with ispersistent "
                   "True...")
        network_offering = copy.deepcopy(
            self.test_data["nuagevsp"]["isolated_network_offering"])
        network_offering["ispersistent"] = "True"
        net_off_2 = self.create_NetworkOffering(network_offering)
        self.validate_NetworkOffering(net_off_2, state="Enabled")

        self.debug("Recreating above Network offering with conserve mode "
                   "On...")
        net_off_3 = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["isolated_network_offering"],
            conserve_mode=True)
        self.validate_NetworkOffering(net_off_3, state="Enabled")

        self.debug("Recreating above Network offering with Source NAT Service "
                   "Capability SupportedSourceNatTypes as per account...")
        network_offering = copy.deepcopy(
            self.test_data["nuagevsp"]["isolated_network_offering"])
        service_list = network_offering["serviceCapabilityList"]
        service_list["SourceNat"]["SupportedSourceNatTypes"] = "peraccount"
        network_offering["serviceCapabilityList"] = service_list
        with self.assertRaises(Exception):
            self.create_NetworkOffering(network_offering)
        self.debug("Nuage VSP does not support Network offerings with Source "
                   "NAT Service Capability "
                   "SupportedSourceNatTypes as per account")

        self.debug("Creating Nuage VSP Isolated Network offering with Source "
                   "NAT service provider as VirtualRouter...")
        network_offering = copy.deepcopy(
            self.test_data["nuagevsp"]["isolated_network_offering"])
        network_offering["serviceProviderList"]["SourceNat"] = "VirtualRouter"
        with self.assertRaises(Exception):
            self.create_NetworkOffering(network_offering)
        self.debug("Nuage VSP does not support Network offerings with Source "
                   "NAT service provider as VirtualRouter")

        self.debug("Creating Nuage VSP Isolated Network offering without "
                   "Source NAT service...")
        network_offering = copy.deepcopy(
            self.test_data["nuagevsp"]["isolated_network_offering"])
        network_offering["supportedservices"] = \
            'Dhcp,Connectivity,StaticNat,UserData,Firewall,Dns'
        del network_offering["serviceProviderList"]["SourceNat"]
        del network_offering["serviceCapabilityList"]
        net_off_4 = self.create_NetworkOffering(network_offering)

        # Creating Isolated networks, and deploying VMs
        self.debug("Creating an Isolated network with Source NAT service "
                   "provider as NuageVsp...")
        network_1 = self.create_Network(net_off_1, gateway='10.1.1.1')
        self.validate_Network(network_1, state="Allocated")

        self.debug("Deploying a VM in the created Isolated network...")
        vm_1 = self.create_VM(network_1)
        self.validate_Network(network_1, state="Implemented")
        vr_1 = self.get_Router(network_1)
        self.check_Router_state(vr_1, state="Running")
        self.check_VM_state(vm_1, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, network_1)
        self.verify_vsd_router(vr_1)
        self.verify_vsd_vm(vm_1)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(network_1)

        # Bug CLOUDSTACK-9398
        """
        self.debug("Creating a persistent Isolated network with Source NAT "
                   "service...")
        network_2 = self.create_Network(net_off_2, gateway='10.1.2.1')
        self.validate_Network(network_2, state="Implemented")
        vr_2 = self.get_Router(network_2)
        self.check_Router_state(vr_2, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, network_2)
        self.verify_vsd_router(vr_2)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(network_2)

        self.debug("Deploying a VM in the created Isolated network...")
        vm_2 = self.create_VM(network_2)
        self.check_VM_state(vm_2, state="Running")

        # VSD verification
        self.verify_vsd_vm(vm_2)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(network_2)
        """

        self.debug("Creating an Isolated network with Source NAT service and "
                   "conserve mode On...")
        network_3 = self.create_Network(net_off_3, gateway='10.1.3.1')
        self.validate_Network(network_3, state="Allocated")

        self.debug("Deploying a VM in the created Isolated network...")
        vm_3 = self.create_VM(network_3)
        self.validate_Network(network_3, state="Implemented")
        vr_3 = self.get_Router(network_3)
        self.check_Router_state(vr_3, state="Running")
        self.check_VM_state(vm_3, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, network_3)
        self.verify_vsd_router(vr_3)
        self.verify_vsd_vm(vm_3)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(network_3)

        self.debug("Creating an Isolated network without Source NAT "
                   "service...")
        with self.assertRaises(Exception):
            self.create_Network(net_off_4, gateway='10.1.4.1')
        self.debug("Nuage VSP does not support creation of Isolated networks "
                   "without Source NAT service")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_02_nuage_SourceNAT_vpc_networks(self):
        """Test Nuage VSP VPC networks with different combinations of Source
        NAT service providers
        """

        # 1. Create Nuage VSP VPC offering with different combinations of
        #    Source NAT service providers
        #    (NuageVsp, VirtualRouter, no SourceNat service), check if all the
        #    VPC offerings are successfully created and enabled.
        # 2. Create VPCs with different combinations of Source NAT service
        #    providers (NuageVsp, VirtualRouter, no SourceNat service), check
        #    if only the VPCs with Source NAT service provider as NuageVsp and
        #    no SourceNat service are successfully created and enabled.
        # 3. Create Nuage VSP VPC Network offering with different combinations
        #    of Source NAT service providers
        #    (NuageVsp, VirtualRouter, no SourceNat service), check if only the
        #    network offering with Source NAT service provider as NuageVsp is
        #    successfully created and enabled.
        # 4. Recreate the above created Network offering with ispersistent flag
        #    set to False, check if the network offering is successfully
        #    created and enabled.
        # 5. Recreate the above created Network offering with conserve mode On
        #    (conserve_mode flag set to True), check if the network offering
        #    creation failed as only networks with conserve mode Off can belong
        #    to VPC.
        # 6. Recreate the above created Network offering with with Source NAT
        #    Service Capability SupportedSourceNatTypes as per account, check
        #    if the network offering creation failed as Nuage VSP supports only
        #    SupportedSourceNatTypes as per zone.
        # 7. Create a VPC network with Source NAT service provider as NuageVsp
        #    in the VPC with SourceNat service and spawn a VM, check if the
        #    tier is added to the VPC VR, and the VM is deployed successfully
        #    in the tier. Verify if the Source NAT functionality for this
        #    network is successfully enabled in VSD.
        # 8. Create a non persistent VPC network with Source NAT service
        #    provider as NuageVsp in the VPC with SourceNat service and spawn a
        #    VM, check if the tier creation failed as Nuage VSP does not
        #    support non persistent VPC networks.
        # 9. Create another VPC network with Source NAT service provider as
        #    NuageVsp in the VPC with SourceNat service and spawn a VM, check
        #    if the tier is added to the VPC VR, and the VM is deployed
        #    successfully in the tier. Verify if the Source NAT functionality
        #    for this network is successfully enabled in VSD.
        # 10. Create a VPC network with Source NAT service provider as NuageVsp
        #     in the VPC without SourceNat service, check if the tier creation
        #     failed as the VPC does not support Source NAT service.
        # 11. Delete all the created objects (cleanup).

        # Creating VPC offerings
        self.debug("Creating Nuage VSP VPC offering with Source NAT service "
                   "provider as NuageVsp...")
        vpc_off_1 = self.create_VpcOffering(
            self.test_data["nuagevsp"]["vpc_offering"])
        self.validate_VpcOffering(vpc_off_1, state="Enabled")

        self.debug("Creating Nuage VSP VPC offering with Source NAT service "
                   "provider as VpcVirtualRouter...")
        vpc_offering = copy.deepcopy(
            self.test_data["nuagevsp"]["vpc_offering"])
        vpc_offering["serviceProviderList"]["SourceNat"] = "VpcVirtualRouter"
        vpc_off_2 = self.create_VpcOffering(vpc_offering)
        self.validate_VpcOffering(vpc_off_2, state="Enabled")

        self.debug("Creating Nuage VSP VPC offering without Source NAT "
                   "service...")
        vpc_offering = copy.deepcopy(
            self.test_data["nuagevsp"]["vpc_offering"])
        vpc_offering["supportedservices"] = \
            'Dhcp,StaticNat,NetworkACL,Connectivity,UserData,Dns'
        del vpc_offering["serviceProviderList"]["SourceNat"]
        vpc_off_3 = self.create_VpcOffering(vpc_offering)
        self.validate_VpcOffering(vpc_off_3, state="Enabled")

        # Creating VPCs
        self.debug("Creating a VPC with Source NAT service provider as "
                   "NuageVsp...")
        vpc_1 = self.create_Vpc(vpc_off_1, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc_1, state="Enabled")

        self.debug("Creating a VPC with Source NAT service provider as "
                   "VpcVirtualRouter...")
        with self.assertRaises(Exception):
            self.create_Vpc(vpc_off_2, cidr='10.1.0.0/16')
        self.debug("Nuage VSP does not support provider VpcVirtualRouter for "
                   "service Source NAT for VPCs")

        self.debug("Creating a VPC without Source NAT service...")
        with self.assertRaises(Exception):
            self.create_Vpc(vpc_off_3, cidr='10.1.0.0/16')
        self.debug("Nuage VSP does not support VPCs without Source NAT "
                   "service")

        # Creating network offerings
        self.debug("Creating Nuage VSP VPC Network offering with Source NAT "
                   "service provider as NuageVsp...")
        net_off_1 = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        self.validate_NetworkOffering(net_off_1, state="Enabled")

        self.debug("Recreating above Network offering with ispersistent "
                   "False...")
        network_offering = copy.deepcopy(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        network_offering["ispersistent"] = "False"
        net_off_2 = self.create_NetworkOffering(network_offering)
        self.validate_NetworkOffering(net_off_2, state="Enabled")

        self.debug("Recreating above Network offering with conserve mode "
                   "On...")
        with self.assertRaises(Exception):
            self.create_NetworkOffering(
                self.test_data["nuagevsp"]["vpc_network_offering"],
                conserve_mode=True)
        self.debug("Network offering creation failed as only networks with "
                   "conserve mode Off can belong to VPC")

        self.debug("Recreating above Network offering with Source NAT Service "
                   "Capability SupportedSourceNatTypes as per account...")
        network_offering = copy.deepcopy(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        service_list = network_offering["serviceCapabilityList"]
        service_list["SourceNat"]["SupportedSourceNatTypes"] = "peraccount"
        network_offering["serviceCapabilityList"] = service_list
        with self.assertRaises(Exception):
            self.create_NetworkOffering(network_offering)
        self.debug("Nuage VSP does not support Network offerings with Source "
                   "NAT Service Capability SupportedSourceNatTypes as per "
                   "account")

        self.debug("Creating Nuage VSP VPC Network offering with Source NAT "
                   "service provider as VpcVirtualRouter...")
        network_offering = copy.deepcopy(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        network_offering["serviceProviderList"]["SourceNat"] = \
            "VpcVirtualRouter"
        with self.assertRaises(Exception):
            self.create_NetworkOffering(network_offering)
        self.debug("Nuage VSP does not support Network offerings with Source "
                   "NAT service provider as VpcVirtualRouter")

        self.debug("Creating Nuage VSP VPC Network offering without Source "
                   "NAT service...")
        network_offering = copy.deepcopy(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        network_offering["supportedservices"] = \
            'Dhcp,StaticNat,NetworkACL,Connectivity,UserData,Dns'
        del network_offering["serviceProviderList"]["SourceNat"]
        del network_offering["serviceCapabilityList"]
        with self.assertRaises(Exception):
            self.create_NetworkOffering(network_offering)
        self.debug("Nuage VSP does not support Network offerings without "
                   "Source NAT service")

        # Creating VPC networks in the VPCs, and deploying VMs
        self.debug("Creating a VPC network with Source NAT service provider "
                   "as NuageVsp in vpc_1...")
        vpc_tier_1 = self.create_Network(
            net_off_1, gateway='10.1.1.1', vpc=vpc_1)
        self.validate_Network(vpc_tier_1, state="Implemented")
        vpc_vr = self.get_Router(vpc_tier_1)
        self.check_Router_state(vpc_vr, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, vpc_tier_1, vpc_1)
        self.verify_vsd_router(vpc_vr)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(vpc_tier_1, vpc=vpc_1)

        self.debug("Deploying a VM in the created VPC network...")
        vpc_vm_1 = self.create_VM(vpc_tier_1)
        self.check_VM_state(vpc_vm_1, state="Running")

        # VSD verification
        self.verify_vsd_vm(vpc_vm_1)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(vpc_tier_1, vpc=vpc_1)

        self.debug("Creating a non persistent VPC network with Source NAT "
                   "service in vpc_1...")
        with self.assertRaises(Exception):
            self.create_Network(net_off_2, gateway='10.1.2.1', vpc=vpc_1)
        self.debug("Nuage VSP does not support non persistent VPC networks")

        self.debug("Creating another VPC network with Source NAT service in "
                   "vpc_1...")
        vpc_tier_2 = self.create_Network(
            net_off_1, gateway='10.1.2.1', vpc=vpc_1)
        self.validate_Network(vpc_tier_2, state="Implemented")
        vpc_vr = self.get_Router(vpc_tier_2)
        self.check_Router_state(vpc_vr, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, vpc_tier_2, vpc_1)
        self.verify_vsd_router(vpc_vr)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(vpc_tier_2, vpc=vpc_1)

        self.debug("Deploying a VM in the created VPC network...")
        vpc_vm_2 = self.create_VM(vpc_tier_2)
        self.check_VM_state(vpc_vm_2, state="Running")

        # VSD verification
        self.verify_vsd_vm(vpc_vm_2)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(vpc_tier_2, vpc=vpc_1)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_03_nuage_SourceNAT_isolated_network_traffic(self):
        """Test Nuage VSP Source NAT functionality for Isolated network by
        performing (wget) traffic tests to the Internet
        """

        # 1. Check if the configured Nuage VSP SDN platform infrastructure
        #    supports underlay networking, if not, skip this test.
        # 2. Create an Isolated network with Source NAT service provider as
        #    NuageVsp and spawn a VM, check if the network is successfully
        #    created, and the VM along with the VR is deployed successfully in
        #    the network. Verify if the Source NAT functionality for this
        #    network is successfully enabled in VSD.
        # 3. Verify Source NAT traffic test (wget www.google.com) to the
        #    Internet from the deployed VM.
        # 4. Deploy another VM in the created Isolated network, check if the VM
        #    is deployed successfully in the network. Verify if the Source NAT
        #    functionality for this network is successfully enabled in VSD.
        # 5. Verify Source NAT traffic test (wget www.google.com) to the
        #    Internet from the deployed VM.
        # 6. Delete all the created objects (cleanup).
        # Note: Above mentioned Source NAT traffic tests are done through a
        #       custom init script in the guest VM template upon the VM boot
        #       up. This traffic tests are verified by SSHing into the VM
        #       using a Static NAT rule.

        if not self.isNuageInfraUnderlay:
            self.skipTest("Configured Nuage VSP SDN platform infrastructure "
                          "does not support underlay networking: "
                          "skipping test")

        # Creating network offering
        self.debug("Creating Nuage VSP Isolated Network offering with Source "
                   "NAT service provider as NuageVsp...")
        net_off = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["isolated_network_offering"])
        self.validate_NetworkOffering(net_off, state="Enabled")

        # Creating Isolated networks, deploying VMs, and verifying Source NAT
        # traffic
        self.debug("Creating an Isolated network with Source NAT service...")
        network = self.create_Network(net_off, gateway='10.1.1.1')
        self.validate_Network(network, state="Allocated")

        self.debug("Deploying a VM in the created Isolated network...")
        vm_1 = self.create_VM(network)
        self.validate_Network(network, state="Implemented")
        vr = self.get_Router(network)
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(vm_1, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, network)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(vm_1)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(network)

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vm_1, network)

        self.debug("Deploying another VM in the created Isolated network...")
        vm_2 = self.create_VM(network)
        self.check_VM_state(vm_2, state="Running")

        # VSD verification
        self.verify_vsd_vm(vm_2)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(network)

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vm_2, network)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_04_nuage_SourceNAT_vpc_network_traffic(self):
        """Test Nuage VSP Source NAT functionality for VPC network by
        performing (wget) traffic tests to the Internet
        """

        # 1. Check if the configured Nuage VSP SDN platform infrastructure
        #    supports underlay networking, if not, skip this test.
        # 2. Create a VPC network with Source NAT service provider as NuageVsp
        #    in the VPC with SourceNat service and spawn a VM, check if the
        #    tier is added to the VPC VR, and the VM is deployed successfully
        #    in the tier. Verify if the Source NAT functionality for this
        #    network is successfully enabled in VSD.
        # 3. Verify Source NAT traffic test (wget www.google.com) to the
        #    Internet from the deployed VM.
        # 4. Deploy another VM in the created VPC network, check if the VM is
        #    deployed successfully in the network. Verify if the Source NAT
        #    functionality for this network is successfully enabled in VSD.
        # 5. Verify Source NAT traffic test (wget www.google.com) to the
        #    Internet from the deployed VM.
        # 6. Delete all the created objects (cleanup).
        # Note: Above mentioned Source NAT traffic tests are done through a
        #       custom init script in the guest VM template upon the VM boot
        #       up. This traffic tests are verified by SSHing into the VM using
        #       a Static NAT rule.

        if not self.isNuageInfraUnderlay:
            self.skipTest("Configured Nuage VSP SDN platform infrastructure "
                          "does not support underlay networking: "
                          "skipping test")

        # Creating VPC offering
        self.debug("Creating Nuage VSP VPC offering with Source NAT service "
                   "provider as NuageVsp...")
        vpc_off = self.create_VpcOffering(
            self.test_data["nuagevsp"]["vpc_offering"])
        self.validate_VpcOffering(vpc_off, state="Enabled")

        # Creating VPC
        self.debug("Creating a VPC with Source NAT service provider as "
                   "NuageVsp...")
        vpc = self.create_Vpc(vpc_off, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc, state="Enabled")

        # Creating network offering
        self.debug("Creating Nuage VSP VPC Network offering with Source NAT "
                   "service provider as NuageVsp...")
        net_off = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        self.validate_NetworkOffering(net_off, state="Enabled")

        # Creating VPC networks in the VPC, deploying VMs, and verifying
        # Source NAT traffic
        self.debug("Creating a VPC network with Source NAT service...")
        vpc_tier = self.create_Network(net_off, gateway='10.1.1.1', vpc=vpc)
        self.validate_Network(vpc_tier, state="Implemented")
        vpc_vr = self.get_Router(vpc_tier)
        self.check_Router_state(vpc_vr, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, vpc_tier, vpc)
        self.verify_vsd_router(vpc_vr)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(vpc_tier, vpc=vpc)

        # Adding Egress Network ACL rules
        self.debug("Adding Egress Network ACL rules in the created VPC "
                   "network to allow Source NAT (DNS & HTTP) traffic to the "
                   "Internet from the VMs in the network...")
        dns_rule = self.create_NetworkAclRule(
            self.test_data["dns_rule"], traffic_type="Egress",
            network=vpc_tier)
        http_rule = self.create_NetworkAclRule(
            self.test_data["http_rule"], traffic_type="Egress",
            network=vpc_tier)

        # VSD verification for added Egress Network ACL rules
        self.verify_vsd_firewall_rule(dns_rule, traffic_type="Egress")
        self.verify_vsd_firewall_rule(http_rule, traffic_type="Egress")

        self.debug("Deploying a VM in the created VPC network...")
        vpc_vm_1 = self.create_VM(vpc_tier)
        self.check_VM_state(vpc_vm_1, state="Running")

        # VSD verification
        self.verify_vsd_vm(vpc_vm_1)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(vpc_tier, vpc=vpc)

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vpc_vm_1, vpc_tier, vpc=vpc)

        self.debug("Deploying another VM in the created VPC network...")
        vpc_vm_2 = self.create_VM(vpc_tier)
        self.check_VM_state(vpc_vm_2, state="Running")

        # VSD verification
        self.verify_vsd_vm(vpc_vm_2)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(vpc_tier, vpc=vpc)

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vpc_vm_2, vpc_tier, vpc=vpc)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_05_nuage_SourceNAT_acl_rules_traffic(self):
        """Test Nuage VSP Source NAT functionality with different Egress
        Firewall/Network ACL rules by performing (wget) traffic tests to the
        Internet
        """

        # Check if the configured Nuage VSP SDN platform infrastructure
        # supports underlay networking, if not, skip this test.
        # Repeat the tests in the testcases
        # "test_03_nuage_SourceNAT_isolated_network_traffic" and
        # "test_04_nuage_SourceNAT_vpc_network_traffic" with different Egress
        # Firewall/Network ACL rules:
        # 1. Allow and block Egress Firewall rules
        # 2. Allow and block Egress Network ACL rules
        # Verify the above Egress Firewall/Network ACL rules by verifying its
        # Source NAT traffic test (wget www.google.com) to the Internet.
        # Delete all the created objects (cleanup).

        if not self.isNuageInfraUnderlay:
            self.skipTest("Configured Nuage VSP SDN platform infrastructure "
                          "does not support underlay networking: "
                          "skipping test")

        # Creating Isolated network offering
        self.debug("Creating Nuage VSP Isolated Network offering with Source "
                   "NAT service provider as NuageVsp...")
        net_off = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["isolated_network_offering"])
        self.validate_NetworkOffering(net_off, state="Enabled")

        # Creating an Isolated network, deploying a VM, and verifying Source
        # NAT traffic with Egress Firewall rules
        self.debug("Creating an Isolated network with Source NAT service...")
        network = self.create_Network(net_off, gateway='10.1.1.1')
        self.validate_Network(network, state="Allocated")

        self.debug("Deploying a VM in the created Isolated network...")
        vm = self.create_VM(network)
        self.validate_Network(network, state="Implemented")
        vr = self.get_Router(network)
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(vm, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, network)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(vm)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(network)

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vm, network)

        # Adding Egress Firewall rule
        self.debug("Adding an Egress Firewall rule in the created Isolated "
                   "network to block/drop Source NAT (DNS) traffic to the "
                   "Internet from the VMs in the network...")
        dns_rule_1 = self.create_EgressFirewallRule(
            network, self.test_data["dns_rule"])

        # VSD verification for added Egress Firewall rule
        self.verify_vsd_firewall_rule(dns_rule_1, traffic_type="Egress")

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        with self.assertRaises(Exception):
            self.verify_SourceNAT_VM_traffic(vm, network, negative_test=True)
        self.debug("Source NAT (DNS) traffic to the Internet from the "
                   "deployed VM is blocked/dropped by the added Egress "
                   "Firewall rule")

        # Removing Egress Firewall rule
        self.debug("Removing the added Egress Firewall rule in the created "
                   "Isolated network to allow Source NAT (DNS) traffic to "
                   "the Internet from the VMs in the network "
                   "(Default Egress Firewall rule)...")
        dns_rule_1.delete(self.api_client)

        # VSD verification for removed Egress Firewall rule
        with self.assertRaises(Exception):
            self.verify_vsd_firewall_rule(dns_rule_1, traffic_type="Egress")
        self.debug("Egress Firewall rule successfully deleted in VSD")

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vm, network)

        # Creating VPC offering
        self.debug("Creating Nuage VSP VPC offering with Source NAT service "
                   "provider as NuageVsp...")
        vpc_off = self.create_VpcOffering(
            self.test_data["nuagevsp"]["vpc_offering"])
        self.validate_VpcOffering(vpc_off, state="Enabled")

        # Creating VPC
        self.debug("Creating a VPC with Source NAT service provider as "
                   "NuageVsp...")
        vpc = self.create_Vpc(vpc_off, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc, state="Enabled")

        # Creating VPC network offering
        self.debug("Creating Nuage VSP VPC Network offering with Source NAT "
                   "service provider as NuageVsp...")
        net_off = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        self.validate_NetworkOffering(net_off, state="Enabled")

        # Creating a VPC network in the VPC, deploying a VM, and verifying
        # Source NAT traffic with Network ACl rules
        self.debug("Creating a VPC network with Source NAT service...")
        vpc_tier = self.create_Network(net_off, gateway='10.1.1.1', vpc=vpc)
        self.validate_Network(vpc_tier, state="Implemented")
        vpc_vr = self.get_Router(vpc_tier)
        self.check_Router_state(vpc_vr, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, vpc_tier, vpc)
        self.verify_vsd_router(vpc_vr)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(vpc_tier, vpc=vpc)

        # Adding Egress Network ACL rules
        self.debug("Adding Egress Network ACL rules in the created VPC "
                   "network to allow Source NAT (DNS & HTTP) traffic to the "
                   "Internet from the VMs in the network...")
        dns_rule_2 = self.create_NetworkAclRule(
            self.test_data["dns_rule"], traffic_type="Egress",
            network=vpc_tier)
        http_rule = self.create_NetworkAclRule(
            self.test_data["http_rule"], traffic_type="Egress",
            network=vpc_tier)

        # VSD verification for added Egress Network ACL rules
        self.verify_vsd_firewall_rule(dns_rule_2, traffic_type="Egress")
        self.verify_vsd_firewall_rule(http_rule, traffic_type="Egress")

        self.debug("Deploying a VM in the created VPC network...")
        vpc_vm = self.create_VM(vpc_tier)
        self.check_VM_state(vpc_vm, state="Running")

        # VSD verification
        self.verify_vsd_vm(vpc_vm)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(vpc_tier, vpc=vpc)

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vpc_vm, vpc_tier, vpc=vpc)

        # Removing Egress Network ACL rule
        self.debug("Removing the added Egress Network ACL rule in the created "
                   "VPC network to block Source NAT (DNS) traffic to the "
                   "Internet from the VMs in the network "
                   "(Default Egress Network ACL rule)...")
        dns_rule_2.delete(self.api_client)

        # VSD verification for removed Egress Network ACL rule
        with self.assertRaises(Exception):
            self.verify_vsd_firewall_rule(dns_rule_2, traffic_type="Egress")
        self.debug("Egress Network ACL rule successfully deleted in VSD")

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        with self.assertRaises(Exception):
            self.verify_SourceNAT_VM_traffic(vpc_vm, vpc_tier, vpc=vpc,
                                             negative_test=True)
        self.debug("Source NAT (DNS) traffic to the Internet from the "
                   "deployed VM is blocked by the Default Egress Network ACL "
                   "rule")

        # Re-adding Egress Network ACL rule
        self.debug("Re-adding the Egress Network ACL rule in the created VPC "
                   "network to allow Source NAT (DNS) traffic to the "
                   "Internet from the VMs in the network...")
        dns_rule_2 = self.create_NetworkAclRule(
            self.test_data["dns_rule"], traffic_type="Egress",
            network=vpc_tier)

        # VSD verification for re-added Egress Network ACL rule
        self.verify_vsd_firewall_rule(dns_rule_2, traffic_type="Egress")

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vpc_vm, vpc_tier, vpc=vpc)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_06_nuage_SourceNAT_vm_nic_operations_traffic(self):
        """Test Nuage VSP Source NAT functionality with VM NIC operations by
        performing (wget) traffic tests to the
        Internet
        """

        # Check if the configured Nuage VSP SDN platform infrastructure
        # supports underlay networking, if not, skip this test.
        # Repeat the tests in the testcase "test_03_nuage_SourceNAT_isolated_
        # network_traffic" with VM NIC operations:
        # 1. Updating default VM NIC
        # 2. Removing non-default VM NIC
        # 3. Adding and updating default VM NIC
        # Verify the above VM NIC operations by verifying its Source NAT
        # traffic test (wget www.google.com) to the Internet.
        # Delete all the created objects (cleanup).

        if not self.isNuageInfraUnderlay:
            self.skipTest("Configured Nuage VSP SDN platform infrastructure "
                          "does not support underlay networking: "
                          "skipping test")

        # Creating network offering
        self.debug("Creating Nuage VSP Isolated Network offering with Source "
                   "NAT service provider as NuageVsp...")
        net_off = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["isolated_network_offering"])
        self.validate_NetworkOffering(net_off, state="Enabled")

        # Creating Isolated networks, deploying a multi-nic VM, and verifying
        # Source NAT traffic with VM NIC operations
        self.debug("Creating an Isolated network with Source NAT service...")
        network_1 = self.create_Network(net_off, gateway='10.1.1.1')
        self.validate_Network(network_1, state="Allocated")

        self.debug("Creating another Isolated network with Source NAT "
                   "service...")
        network_2 = self.create_Network(net_off, gateway='10.1.2.1')
        self.validate_Network(network_2, state="Allocated")

        self.debug("Deploying a multi-nic VM in the created Isolated "
                   "networks...")
        vm = self.create_VM([network_1, network_2])
        self.validate_Network(network_1, state="Implemented")
        vr_1 = self.get_Router(network_1)
        self.check_Router_state(vr_1, state="Running")
        self.validate_Network(network_2, state="Implemented")
        vr_2 = self.get_Router(network_2)
        self.check_Router_state(vr_2, state="Running")
        self.check_VM_state(vm, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, network_1)
        self.verify_vsd_router(vr_1)
        self.verify_vsd_network(self.domain.id, network_2)
        self.verify_vsd_router(vr_2)
        self.verify_vsd_vm(vm)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(network_1)
        self.verify_vsd_SourceNAT_network(network_2)

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vm, network_1)

        # Updating default VM NIC
        # This VM NIC operation has no effect on the Source NAT functionality
        self.debug("Updating the default nic of the multi-nic VM...")
        self.nic_operation_VM(vm, network_2, operation="update")

        # Rebooting (stop - start) VM
        self.debug("Rebooting the multi-nic VM after updating its default nic "
                   "for changes to apply to the VM...")
        vm.stop(self.api_client)
        vm.start(self.api_client)
        self.check_VM_state(vm, state="Running")

        # VSD verification
        updated_vm_info = VirtualMachine.list(self.api_client, id=vm.id)[0]
        self.verify_vsd_vm(updated_vm_info)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(network_1)
        self.verify_vsd_SourceNAT_network(network_2)

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vm, network_2)

        # Removing non-default VM NIC
        # This VM NIC operation has no effect on the Source NAT functionality
        self.debug("Removing the non-default nic of the multi-nic VM...")
        self.nic_operation_VM(vm, network_1, operation="remove")

        # Rebooting (stop - start) VM
        self.debug("Rebooting the multi-nic VM after removing its non-default "
                   "nic for changes to apply to the VM...")
        vm.stop(self.api_client)
        vm.start(self.api_client)
        self.check_VM_state(vm, state="Running")

        # VSD verification
        updated_vm_info = VirtualMachine.list(self.api_client, id=vm.id)[0]
        self.verify_vsd_vm(updated_vm_info)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(network_1)
        self.verify_vsd_SourceNAT_network(network_2)

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vm, network_2)

        # Adding and updating default VM NIC
        # This VM NIC operation has no effect on the Source NAT functionality
        self.debug("Re-adding the non-default nic and updating the default "
                   "nic of the multi-nic VM...")
        self.nic_operation_VM(vm, network_1, operation="add")
        self.nic_operation_VM(vm, network_1, operation="update")

        # Rebooting (stop - start) VM
        self.debug("Rebooting the multi-nic VM after re-adding its "
                   "non-default nic for changes to apply to the VM...")
        vm.stop(self.api_client)
        vm.start(self.api_client)
        self.check_VM_state(vm, state="Running")

        # VSD verification
        updated_vm_info = VirtualMachine.list(self.api_client, id=vm.id)[0]
        self.verify_vsd_vm(updated_vm_info)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(network_1)
        self.verify_vsd_SourceNAT_network(network_2)

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vm, network_1)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_07_nuage_SourceNAT_vm_migration_traffic(self):
        """Test Nuage VSP Source NAT functionality with VM migration by
        performing (wget) traffic tests to the Internet
        """

        # Check if the configured Nuage VSP SDN platform infrastructure
        # supports underlay networking, if not, skip this test.
        # Repeat the tests in the testcase
        # "test_03_nuage_SourceNAT_isolated_network_traffic" with migration of
        # one of the VMs to another host (if available).
        # Verify the above VM migration by verifying its Source NAT traffic
        # test (wget www.google.com) to the Internet.
        # Delete all the created objects (cleanup).

        if not self.isNuageInfraUnderlay:
            self.skipTest("Configured Nuage VSP SDN platform infrastructure "
                          "does not support underlay networking: "
                          "skipping test")

        # Creating network offering
        self.debug("Creating Nuage VSP Isolated Network offering with Source "
                   "NAT service provider as NuageVsp...")
        net_off = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["isolated_network_offering"])
        self.validate_NetworkOffering(net_off, state="Enabled")

        # Creating an Isolated network, deploying VMs, and verifying Source NAT
        # traffic with VM migrations
        self.debug("Creating an Isolated network with Source NAT service...")
        network = self.create_Network(net_off, gateway='10.1.1.1')
        self.validate_Network(network, state="Allocated")

        self.debug("Deploying a VM in the created Isolated network...")
        vm_1 = self.create_VM(network)
        self.validate_Network(network, state="Implemented")
        vr = self.get_Router(network)
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(vm_1, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, network)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(vm_1)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(network)

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vm_1, network)

        self.debug("Deploying another VM in the created Isolated network...")
        vm_2 = self.create_VM(network)
        self.check_VM_state(vm_2, state="Running")

        # VSD verification
        self.verify_vsd_vm(vm_2)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(network)

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vm_2, network)

        # VM migration
        # This VM migration has no effect on the Source NAT functionality
        self.debug("Migrating one of the VMs in the created Isolated network "
                   "to another host, if available...")
        self.migrate_VM(vm_1)

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vm_1, network)

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vm_2, network)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_08_nuage_SourceNAT_network_restarts_traffic(self):
        """Test Nuage VSP Source NAT functionality with network restarts by
        performing (wget) traffic tests to the Internet
        """

        # Check if the configured Nuage VSP SDN platform infrastructure
        # supports underlay networking, if not, skip this test.
        # Repeat the tests in the testcases
        # "test_03_nuage_SourceNAT_isolated_network_traffic" and
        # "test_04_nuage_SourceNAT_vpc_network_traffic" with network restarts:
        # 1. Restart Isolated Network (cleanup = false)
        # 2. Restart Isolated Network (cleanup = true)
        # 3. Reboot VM in the Isolated Network
        # 4. Restart VPC Network (cleanup = false)
        # 5. Restart VPC Network (cleanup = true)
        # 6. Reboot VM in the VPC Network
        # 7. Restart VPC (cleanup = false)
        # 8. Restart VPC (cleanup = true)
        # Verify the above with network restarts by verifying its Source NAT
        # traffic test (wget www.google.com) to the Internet.
        # Delete all the created objects (cleanup).

        if not self.isNuageInfraUnderlay:
            self.skipTest("Configured Nuage VSP SDN platform infrastructure "
                          "does not support underlay networking: "
                          "skipping test")

        # Creating network offering
        self.debug("Creating Nuage VSP Isolated Network offering with Source "
                   "NAT service provider as NuageVsp...")
        net_off = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["isolated_network_offering"])
        self.validate_NetworkOffering(net_off, state="Enabled")

        # Creating an Isolated network, deploying a VM, and verifying Source
        # NAT traffic with Isolated network restarts
        self.debug("Creating an Isolated network with Source NAT service...")
        network = self.create_Network(net_off, gateway='10.1.1.1')
        self.validate_Network(network, state="Allocated")

        self.debug("Deploying a VM in the created Isolated network...")
        vm = self.create_VM(network)
        self.validate_Network(network, state="Implemented")
        vr = self.get_Router(network)
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(vm, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, network)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(vm)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(network)

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vm, network)

        # Restarting Isolated network (cleanup = false)
        # VR gets destroyed and deployed again in the Isolated network
        # This restart has no effect on the Source NAT functionality
        self.debug("Restarting the created Isolated network without "
                   "cleanup...")
        Network.restart(network, self.api_client, cleanup=False)
        self.validate_Network(network, state="Implemented")
        vr = self.get_Router(network)
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(vm, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, network)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(vm)

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vm, network)

        # Restarting Isolated network (cleanup = true)
        # VR gets destroyed and deployed again in the Isolated network
        # This restart has no effect on the Source NAT functionality
        self.debug("Restarting the created Isolated network with cleanup...")
        Network.restart(network, self.api_client, cleanup=True)
        self.validate_Network(network, state="Implemented")
        vr = self.get_Router(network)
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(vm, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, network)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(vm)

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vm, network)

        # Creating VPC offering
        self.debug("Creating Nuage VSP VPC offering with Source NAT service "
                   "provider as NuageVsp...")
        vpc_off = self.create_VpcOffering(
            self.test_data["nuagevsp"]["vpc_offering"])
        self.validate_VpcOffering(vpc_off, state="Enabled")

        # Creating VPC
        self.debug("Creating a VPC with Source NAT service provider as "
                   "NuageVsp...")
        vpc = self.create_Vpc(vpc_off, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc, state="Enabled")

        # Creating VPC network offering
        self.debug("Creating Nuage VSP VPC Network offering with Source NAT "
                   "service provider as NuageVsp...")
        net_off = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        self.validate_NetworkOffering(net_off, state="Enabled")

        # Creating a VPC network in the VPC, deploying a VM, and verifying
        # Source NAT traffic with VPC network restarts
        self.debug("Creating a VPC network with Source NAT service...")
        vpc_tier = self.create_Network(net_off, gateway='10.1.1.1', vpc=vpc)
        self.validate_Network(vpc_tier, state="Implemented")
        vpc_vr = self.get_Router(vpc_tier)
        self.check_Router_state(vpc_vr, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, vpc_tier, vpc)
        self.verify_vsd_router(vpc_vr)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(vpc_tier, vpc=vpc)

        # Adding Egress Network ACL rules
        self.debug("Adding Egress Network ACL rules in the created VPC "
                   "network to allow Source NAT (DNS & HTTP) traffic to the "
                   "Internet from the VMs in the network...")
        dns_rule = self.create_NetworkAclRule(
            self.test_data["dns_rule"], traffic_type="Egress",
            network=vpc_tier)
        http_rule = self.create_NetworkAclRule(
            self.test_data["http_rule"], traffic_type="Egress",
            network=vpc_tier)

        # VSD verification for added Egress Network ACL rules
        self.verify_vsd_firewall_rule(dns_rule, traffic_type="Egress")
        self.verify_vsd_firewall_rule(http_rule, traffic_type="Egress")

        self.debug("Deploying a VM in the created VPC network...")
        vpc_vm = self.create_VM(vpc_tier)
        self.check_VM_state(vpc_vm, state="Running")

        # VSD verification
        self.verify_vsd_vm(vpc_vm)

        # VSD verification for Source NAT functionality
        self.verify_vsd_SourceNAT_network(vpc_tier, vpc=vpc)

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vpc_vm, vpc_tier, vpc=vpc)

        # Restarting VPC network (cleanup = false)
        # This restart has no effect on the Source NAT functionality
        self.debug("Restarting the created VPC network without cleanup...")
        Network.restart(vpc_tier, self.api_client, cleanup=False)
        self.validate_Network(vpc_tier, state="Implemented")
        self.check_Router_state(vpc_vr, state="Running")
        self.check_VM_state(vpc_vm, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, vpc_tier, vpc)
        self.verify_vsd_router(vpc_vr)
        self.verify_vsd_vm(vpc_vm)
        self.verify_vsd_firewall_rule(dns_rule, traffic_type="Egress")
        self.verify_vsd_firewall_rule(http_rule, traffic_type="Egress")

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vpc_vm, vpc_tier, vpc=vpc)

        # Restarting VPC network (cleanup = true)
        # This restart has no effect on the Source NAT functionality
        self.debug("Restarting the created VPC network with cleanup...")
        Network.restart(vpc_tier, self.api_client, cleanup=True)
        self.validate_Network(vpc_tier, state="Implemented")
        self.check_Router_state(vpc_vr, state="Running")
        self.check_VM_state(vpc_vm, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, vpc_tier, vpc)
        self.verify_vsd_router(vpc_vr)
        self.verify_vsd_vm(vpc_vm)
        self.verify_vsd_firewall_rule(dns_rule, traffic_type="Egress")
        self.verify_vsd_firewall_rule(http_rule, traffic_type="Egress")

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vpc_vm, vpc_tier, vpc=vpc)

        # Restarting VPC (cleanup = false)
        # VPC VR gets destroyed and deployed again in the VPC
        # This restart has no effect on the Source NAT functionality
        self.debug("Restarting the VPC without cleanup...")
        self.restart_Vpc(vpc, cleanup=False)
        self.validate_Network(vpc_tier, state="Implemented")
        vpc_vr = self.get_Router(vpc_tier)
        self.check_Router_state(vpc_vr, state="Running")
        self.check_VM_state(vpc_vm, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, vpc_tier, vpc)
        self.verify_vsd_router(vpc_vr)
        self.verify_vsd_vm(vpc_vm)
        self.verify_vsd_firewall_rule(dns_rule, traffic_type="Egress")
        self.verify_vsd_firewall_rule(http_rule, traffic_type="Egress")

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vpc_vm, vpc_tier, vpc=vpc)

        # Restarting VPC (cleanup = true)
        # VPC VR gets destroyed and deployed again in the VPC
        # This restart has no effect on the Source NAT functionality
        self.debug("Restarting the VPC with cleanup...")
        self.restart_Vpc(vpc, cleanup=True)
        self.validate_Network(vpc_tier, state="Implemented")
        vpc_vr = self.get_Router(vpc_tier)
        self.check_Router_state(vpc_vr, state="Running")
        self.check_VM_state(vpc_vm, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, vpc_tier, vpc)
        self.verify_vsd_router(vpc_vr)
        self.verify_vsd_vm(vpc_vm)
        self.verify_vsd_firewall_rule(dns_rule, traffic_type="Egress")
        self.verify_vsd_firewall_rule(http_rule, traffic_type="Egress")

        # Verifying Source NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM. This Source NAT traffic test is done through a
        # custom init script in the guest VM template upon the VM boot up.
        self.verify_SourceNAT_VM_traffic(vpc_vm, vpc_tier, vpc=vpc)
