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

""" Component tests for Static NAT functionality with Nuage VSP SDN plugin
"""
# Import Local Modules
from nuageTestCase import nuageTestCase
from marvin.lib.base import (Account,
                             PublicIpRange,
                             Network,
                             VirtualMachine)
from marvin.cloudstackAPI import (enableNuageUnderlayVlanIpRange,
                                  disableNuageUnderlayVlanIpRange,
                                  listNuageUnderlayVlanIpRanges)
from marvin.lib.common import list_virtual_machines

# Import System Modules
from nose.plugins.attrib import attr
import threading
import copy
import time


class TestNuageStaticNat(nuageTestCase):
    """Test Static NAT functionality with Nuage VSP SDN plugin
    """

    @classmethod
    def setUpClass(cls):
        super(TestNuageStaticNat, cls).setUpClass()
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

    # enable_NuageUnderlayPublicIpRange - Enables/configures underlay
    # networking for the given public IP range in Nuage VSP
    def enable_NuageUnderlayPublicIpRange(self, public_ip_range):
        cmd = enableNuageUnderlayVlanIpRange.\
            enableNuageUnderlayVlanIpRangeCmd()
        cmd.id = public_ip_range.vlan.id
        self.api_client.enableNuageUnderlayVlanIpRange(cmd)

    # disable_NuageUnderlayPublicIpRange - Disables/de-configures underlay
    # networking for the given public IP range in Nuage VSP
    def disable_NuageUnderlayPublicIpRange(self, public_ip_range):
        cmd = disableNuageUnderlayVlanIpRange.\
            disableNuageUnderlayVlanIpRangeCmd()
        cmd.id = public_ip_range.vlan.id
        self.api_client.enableNuageUnderlayVlanIpRange(cmd)

    # list_NuageUnderlayPublicIpRanges - Lists underlay networking
    # enabled/configured public IP ranges in Nuage VSP
    def list_NuageUnderlayPublicIpRanges(self, public_ip_range=None):
        cmd = listNuageUnderlayVlanIpRanges.listNuageUnderlayVlanIpRangesCmd()
        if public_ip_range:
            cmd.id = public_ip_range.vlan.id
        cmd.underlay = True
        return self.api_client.listNuageUnderlayVlanIpRanges(cmd)

    # create_PublicIpRange - Creates public IP range
    def create_PublicIpRange(self):
        self.debug("Creating public IP range")
        self.test_data["vlan_ip_range"]["startip"] = "20.200.200.100"
        self.test_data["vlan_ip_range"]["endip"] = "20.200.200.200"
        self.test_data["vlan_ip_range"]["netmask"] = "255.255.255.0"
        self.test_data["vlan_ip_range"]["gateway"] = "20.200.200.1"
        self.test_data["vlan_ip_range"]["forvirtualnetwork"] = "true"
        self.test_data["vlan_ip_range"]["zoneid"] = self.zone.id
        public_ip_range = PublicIpRange.create(self.api_client,
                                               self.test_data["vlan_ip_range"]
                                               )
        self.debug("Created public IP range")
        return public_ip_range

    # validate_PublicIpRange - Validates public IP range creation and state
    def validate_PublicIpRange(self, public_ip_range):
        public_ip_ranges = PublicIpRange.list(self.api_client,
                                              id=public_ip_range.vlan.id
                                              )
        self.assertEqual(isinstance(public_ip_ranges, list), True,
                         "List Public IP Range should return a valid list"
                         )
        self.assertEqual(public_ip_range.vlan.startip,
                         public_ip_ranges[0].startip,
                         "Start IP of the public IP range should match with "
                         "the returned list data"
                         )
        self.assertEqual(public_ip_range.vlan.endip, public_ip_ranges[0].endip,
                         "End IP of the public IP range should match with the "
                         "returned list data"
                         )

    # validate_NuageUnderlayPublicIpRange - Validates Nuage underlay enabled
    # public IP range creation and state
    def validate_NuageUnderlayPublicIpRange(self, public_ip_range):
        nuage_underlay_public_ip_ranges = \
            self.list_NuageUnderlayPublicIpRanges(public_ip_range)
        self.assertEqual(isinstance(nuage_underlay_public_ip_ranges, list),
                         True,
                         "List Nuage Underlay Public IP Range should return "
                         "a valid list"
                         )
        self.assertEqual(public_ip_range.vlan.startip,
                         nuage_underlay_public_ip_ranges[0].startip,
                         "Start IP of the public IP range should match with "
                         "the returned list data"
                         )
        self.assertEqual(public_ip_range.vlan.endip,
                         nuage_underlay_public_ip_ranges[0].endip,
                         "End IP of the public IP range should match with the "
                         "returned list data"
                         )

    # verify_StaticNAT_traffic - Verifies Static NAT traffic by performing
    # wget traffic test with the given Static NAT enabled public IP, http web
    # server running on the corresponding VM in the given network
    def verify_StaticNAT_traffic(self, network, public_ip, vpc=None,
                                 non_default_nic=False):
        # Adding Ingress Firewall/Network ACL rule
        self.debug("Adding Ingress Firewall/Network ACL rule to make the "
                   "created Static NAT rule (wget) accessible...")
        if vpc:
            public_http_rule = self.create_NetworkAclRule(
                self.test_data["http_rule"], network=network)
        else:
            public_http_rule = self.create_FirewallRule(
                public_ip, self.test_data["http_rule"])

        # VSD verification
        self.verify_vsd_firewall_rule(public_http_rule)

        # wget from VM
        tries = 0
        max_tries = 3 if non_default_nic else 120
        filename = None
        headers = None
        while tries < max_tries:
            try:
                filename, headers = self.wget_from_server(public_ip)
                if filename and headers:
                    self.debug("wget from VM is successful")
                break
            except Exception as e:
                self.debug("Failed to wget from VM - %s" % e)
                self.debug("Retrying wget from VM after some time...")
                time.sleep(5)
                tries += 1

        try:
            if not filename and not headers:
                if non_default_nic:
                    self.debug("Failed to wget from VM via this NIC as it "
                               "is not the default NIC")
                else:
                    self.fail("Failed to wget from VM")
        finally:
            # Removing Ingress Firewall/Network ACL rule
            self.debug("Removing the created Ingress Firewall/Network ACL "
                       "rule in the network...")
            public_http_rule.delete(self.api_client)

            # VSD verification
            with self.assertRaises(Exception):
                self.verify_vsd_firewall_rule(public_http_rule)
            self.debug("Ingress Firewall/Network ACL rule successfully "
                       "deleted in VSD")

            self.debug("Successfully verified Static NAT traffic by "
                       "performing wget traffic test with the given Static "
                       "NAT enabled public IP - %s" % public_ip)

    # wget_from_internet - From within the given VM (ssh client),
    # fetches index.html file of an Internet web server, wget www.google.com
    def wget_from_Internet(self, ssh_client, timeout):
        if self.http_proxy:
            cmd = "wget --no-cache --output-document=index.html " \
                  "http://www.google.com/ -e use_proxy=yes -e http_proxy=" + \
                  self.http_proxy + " --timeout=" + str(timeout)
        else:
            cmd = "wget --no-cache --output-document=index.html " \
                  "http://www.google.com/ --timeout=" + str(timeout)
        test_result = self.execute_cmd(ssh_client, cmd)
        if "200 OK" in test_result:
            cmd = "rm -rf index.html*"
            self.execute_cmd(ssh_client, cmd)
        return test_result

    # verify_StaticNAT_Internet_traffic - Verifies Static NAT traffic to the
    # Internet (wget www.google.com) from the given VM
    def verify_StaticNAT_Internet_traffic(self, vm, network, public_ip,
                                          vpc=None, non_default_nic=False,
                                          negative_test=False):
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

        # Adding Egress Network ACL rule
        if vpc and self.http_proxy and not negative_test:
            self.debug("Adding Egress Network ACL rule in the created VPC "
                       "network to allow access to the configured Internet "
                       "proxy servers...")
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

        # SSH into VM
        ssh_client = None
        try:
            if non_default_nic:
                with self.assertRaises(Exception):
                    self.ssh_into_VM(vm, public_ip, negative_test=True)
                self.debug("Can not SSH into the VM via this NIC as it is "
                           "not the default NIC")
            else:
                ssh_client = self.ssh_into_VM(vm, public_ip)

            # wget from Internet
            test_result = None
            if ssh_client and self.isInternetConnectivityAvailable:
                timeout = 100 if negative_test else 300
                test_result = self.wget_from_Internet(ssh_client, timeout)
        finally:
            # Removing Ingress Firewall/Network ACL rule
            self.debug("Removing the created Ingress Firewall/Network ACL "
                       "rule in the network...")
            public_ssh_rule.delete(self.api_client)

            # VSD verification
            with self.assertRaises(Exception):
                self.verify_vsd_firewall_rule(public_ssh_rule)
            self.debug("Ingress Firewall/Network ACL rule successfully "
                       "deleted in VSD")

        # Removing Egress Network ACL rule
        if vpc and self.http_proxy:
            self.debug("Removing the created Egress Network ACL rule in the "
                       "VPC network...")
            internet_proxy_server_rule.delete(self.api_client)

            # VSD verification
            with self.assertRaises(Exception):
                self.verify_vsd_firewall_rule(internet_proxy_server_rule)
            self.debug("Egress Network ACL rule successfully deleted in VSD")

        if test_result:
            if "200 OK" in test_result:
                self.debug("Successfully verified Static NAT Internet traffic "
                           "(wget www.google.com) from VM - %s" % vm.name)
            else:
                self.fail("Failed to verify Static NAT Internet traffic "
                          "(wget www.google.com) from VM - %s" % vm.name)
        else:
            if negative_test:
                self.fail("Skipped Static NAT Internet traffic "
                          "(wget www.google.com) test from VM as there is no "
                          "Internet connectivity in the data center")
            else:
                self.debug("Skipped Static NAT Internet traffic "
                           "(wget www.google.com) test from VM as there is no "
                           "Internet connectivity in the data center")

    # enable_staticNat_on_a_starting_vm - Enables Static Nat on a starting VM
    # in the given network with the given public IP.
    def enable_staticNat_on_a_starting_vm(self):
        self.debug("Enables Static Nat on a starting VM in the network - %s "
                   "with the given public IP - %s" %
                   (self.network, self.public_ip))
        time.sleep(15)
        vm_list = list_virtual_machines(self.api_client, listall=True)
        self.create_StaticNatRule_For_VM(
            vm_list[0], self.public_ip, self.network)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_01_nuage_StaticNAT_public_ip_range(self):
        """Test Nuage VSP Public IP Range creation and deletion
        """

        # 1. Create a public IP range (VLAN IP range), check if it is
        #    successfully created in the zone and the physical network.
        # 2. Delete the created public IP range (VLAN IP range), check if it is
        #    successfully deleted from the zone and the physical network.
        # 3. Delete all the created objects (cleanup).

        self.debug("Creating a public IP range...")
        public_ip_range = self.create_PublicIpRange()
        self.validate_PublicIpRange(public_ip_range)
        self.debug("Public IP range successfully created")

        self.debug("Deleting the created public IP range...")
        public_ip_range.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_PublicIpRange(public_ip_range)
        self.debug("Public IP range successfully deleted")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_02_nuage_StaticNAT_underlay_public_ip_range(self):
        """Test Nuage VSP Nuage Underlay (underlay networking) enabled Public
        IP Range creation and deletion
        """

        # 1. Create a public IP range (VLAN IP range), check if it is
        #    successfully created in the zone and the physical network.
        # 2. Enable Nuage underlay capability (underlay networking) for the
        #    created public IP range (VLAN IP range), check if the Nuage
        #    underlay (underlay networking) capability is successfully enabled
        #    for the created public IP range (VLAN IP range).
        # 3. Disable Nuage underlay capability (underlay networking) for the
        #    created public IP range (VLAN IP range), check if the Nuage
        #    underlay (underlay networking) capability is successfully disabled
        #    for the created public IP range (VLAN IP range).
        # 4. Delete the created public IP range (VLAN IP range), check if it is
        #    successfully deleted from the zone and the physical network.
        # 5. Delete all the created objects (cleanup).

        self.debug("Creating a public IP range...")
        public_ip_range = self.create_PublicIpRange()
        self.validate_PublicIpRange(public_ip_range)
        self.debug("Public IP range successfully created")

        self.debug("Enabling Nuage underlay capability (underlay networking) "
                   "for the created public IP range...")
        self.enable_NuageUnderlayPublicIpRange(public_ip_range)
        self.validate_NuageUnderlayPublicIpRange(public_ip_range)
        self.debug("Nuage underlay capability (underlay networking) for the "
                   "created public IP range is successfully enabled")

        self.debug("Disabling Nuage underlay capability (underlay networking) "
                   "for the created public IP range...")
        self.disable_NuageUnderlayPublicIpRange(public_ip_range)
        with self.assertRaises(Exception):
            self.validate_NuageUnderlayPublicIpRange(public_ip_range)
        self.debug("Nuage underlay capability (underlay networking) for the "
                   "created public IP range is successfully disabled")

        self.debug("Deleting the created public IP range...")
        public_ip_range.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_PublicIpRange(public_ip_range)
        self.debug("Public IP range successfully deleted")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_03_nuage_StaticNAT_isolated_networks(self):
        """Test Nuage VSP Isolated networks with different combinations of
        Static NAT service providers
        """

        # 1. Create Nuage VSP Isolated Network offering with different
        #    combinations of Static NAT service providers
        #    (NuageVsp, VirtualRouter, no StaticNat service), check if all the
        #    network offerings are successfully created and enabled.
        # 2. Recreate the above created Network offering
        #    (Static NAT service provider as NuageVsp) with ispersistent flag
        #    set to True, check if the network offering is successfully created
        #    and enabled.
        # 3. Recreate the above created Network offering
        #    (Static NAT service provider as NuageVsp) with conserve mode On
        #    (conserve_mode flag set to True), check if the network offering is
        #    successfully created and enabled.
        # 4. Create an Isolated network with Static NAT service provider as
        #    NuageVsp, spawn a VM, and create a Static NAT rule. Check if the
        #    network is successfully created, and the VM along with the VR is
        #    deployed successfully in the network, verify if the Static NAT
        #    functionality for this network is successfully enabled in VSD.
        # 5. Create a persistent Isolated network with Static NAT service
        #    provider as NuageVsp, spawn a VM, and create a Static NAT rule.
        #    Check if the network is successfully created, and the VM along
        #    with the VR is deployed successfully in the network, verify if the
        #    Static NAT functionality for this network is successfully enabled
        #    in VSD.
        # 6. Create a conserved Isolated network (conserve mode On) with Static
        #    NAT service provider as NuageVsp, spawn a VM, and create a Static
        #    NAT rule. Check if the network is successfully created, and the VM
        #    along with the VR is deployed successfully in the network, verify
        #    if the Static NAT functionality for this network is successfully
        #    enabled in VSD.
        # 7. Create an Isolated network with Static NAT service provider as
        #    VirtualRouter, spawn a VM, and create a Static NAT rule. Check if
        #    the network is successfully created, and the VM along with the VR
        #    is deployed successfully in the network, verify if the Static NAT
        #    functionality for this network is not enabled in VSD as Nuage VSP
        #    does not support VirtualRouter as the Static NAT service provider.
        # 8. Create an Isolated network with no Static NAT service, spawn a VM,
        #    and create a Static NAT rule. Check if the network is successfully
        #    created, and the VM along with the VR is deployed successfully in
        #    the network, verify if the Static NAT functionality for this
        #    network is not enabled in both CloudStack and VSD as the network
        #    does not support Static NAT service.
        # 9. Delete all the created objects (cleanup).

        # Creating network offerings
        self.debug("Creating Nuage VSP Isolated Network offering with Static "
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

        self.debug("Creating Nuage VSP Isolated Network offering with Static "
                   "NAT service provider as VirtualRouter...")
        network_offering = copy.deepcopy(
            self.test_data["nuagevsp"]["isolated_network_offering"])
        network_offering["serviceProviderList"]["StaticNat"] = "VirtualRouter"
        net_off_4 = self.create_NetworkOffering(network_offering)
        self.validate_NetworkOffering(net_off_4, state="Enabled")

        self.debug("Creating Nuage VSP Isolated Network offering without "
                   "Static NAT service...")
        network_offering = copy.deepcopy(
            self.test_data["nuagevsp"]["isolated_network_offering"])
        network_offering["supportedservices"] = \
            'Dhcp,SourceNat,Connectivity,UserData,Firewall,Dns'
        del network_offering["serviceProviderList"]["StaticNat"]
        net_off_5 = self.create_NetworkOffering(network_offering)
        self.validate_NetworkOffering(net_off_5, state="Enabled")

        # Creating Isolated networks, and deploying VMs
        self.debug("Creating an Isolated network with Static NAT service "
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

        # Creating Static NAT rule
        self.debug("Creating Static NAT rule for the deployed VM in the "
                   "created Isolated network...")
        public_ip = self.acquire_PublicIPAddress(network_1)
        self.validate_PublicIPAddress(public_ip, network_1)
        self.create_StaticNatRule_For_VM(vm_1, public_ip, network_1)
        self.validate_PublicIPAddress(
            public_ip, network_1, static_nat=True, vm=vm_1)

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(network_1, vm_1, public_ip.ipaddress)

        # Deleting Static NAT Rule
        self.debug("Deleting Static NAT Rule for the deployed VM...")
        self.delete_StaticNatRule_For_VM(public_ip)
        with self.assertRaises(Exception):
            self.validate_PublicIPAddress(
                public_ip, network_1, static_nat=True, vm=vm_1)
        self.debug("Static NAT Rule for the deployed VM successfully deleted "
                   "in CloudStack")

        # VSD verification
        with self.assertRaises(Exception):
            self.verify_vsd_floating_ip(network_1, vm_1, public_ip.ipaddress)
        self.debug("Floating IP for the deployed VM successfully deleted in "
                   "VSD")

        # Releasing acquired public IP
        self.debug("Releasing the acquired public IP in the created Isolated "
                   "network...")
        public_ip.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_PublicIPAddress(public_ip, network_1)
        self.debug("Acquired public IP in the created Isolated network "
                   "successfully released in CloudStack")
        self.delete_VM(vm_1)
        # Bug CLOUDSTACK-9398
        """
        self.debug("Creating a persistent Isolated network with Static NAT "
                   "service...")
        network_2 = self.create_Network(net_off_2, gateway='10.1.1.1')
        self.validate_Network(network_2, state="Implemented")
        vr_2 = self.get_Router(network_2)
        self.check_Router_state(vr_2, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, network_2)
        self.verify_vsd_router(vr_2)

        self.debug("Deploying a VM in the created Isolated network...")
        vm_2 = self.create_VM(network_2)
        self.check_VM_state(vm_2, state="Running")

        # VSD verification
        self.verify_vsd_vm(vm_2)

        # Creating Static NAT rule
        self.debug("Creating Static NAT rule in the created Isolated network "
                   "with its deployed VM...")
        public_ip = self.acquire_PublicIPAddress(network_2)
        self.validate_PublicIPAddress(public_ip, network_2)
        self.create_StaticNatRule_For_VM(vm_2, public_ip, network_2)
        self.validate_PublicIPAddress(
            public_ip, network_2, static_nat=True, vm=vm_2)

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(network_2, vm_2, public_ip.ipaddress)

        # Deleting Static NAT Rule
        self.debug("Deleting Static NAT Rule for the deployed VM...")
        self.delete_StaticNatRule_For_VM(public_ip)
        with self.assertRaises(Exception):
            self.validate_PublicIPAddress(
                public_ip, network_2, static_nat=True, vm=vm_2)
        self.debug("Static NAT Rule for the deployed VM successfully deleted "
                   "in CloudStack")

        # VSD verification
        with self.assertRaises(Exception):
            self.verify_vsd_floating_ip(network_2, vm_2, public_ip.ipaddress)
        self.debug("Floating IP for the deployed VM successfully deleted in "
                   "VSD")

        # Releasing acquired public IP
        self.debug("Releasing the acquired public IP in the created Isolated "
                   "network...")
        public_ip.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_PublicIPAddress(public_ip, network_2)
        self.debug("Acquired public IP in the created Isolated network "
                   "successfully released in CloudStack")
        self.delete_VM(vm_2)
        """

        self.debug("Creating an Isolated network with Static NAT service and "
                   "conserve mode On...")
        network_3 = self.create_Network(net_off_3, gateway='10.1.1.1')
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

        # Creating Static NAT rule
        self.debug("Creating Static NAT rule in the created Isolated network "
                   "with its deployed VM...")
        public_ip = self.acquire_PublicIPAddress(network_3)
        self.validate_PublicIPAddress(public_ip, network_3)
        self.create_StaticNatRule_For_VM(vm_3, public_ip, network_3)
        self.validate_PublicIPAddress(
            public_ip, network_3, static_nat=True, vm=vm_3)

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(network_3, vm_3, public_ip.ipaddress)

        # Deleting Static NAT Rule
        self.debug("Deleting Static NAT Rule for the deployed VM...")
        self.delete_StaticNatRule_For_VM(public_ip)
        with self.assertRaises(Exception):
            self.validate_PublicIPAddress(
                public_ip, network_3, static_nat=True, vm=vm_3)
        self.debug("Static NAT Rule for the deployed VM successfully deleted "
                   "in CloudStack")

        # VSD verification
        with self.assertRaises(Exception):
            self.verify_vsd_floating_ip(network_3, vm_3, public_ip.ipaddress)
        self.debug("Floating IP for the deployed VM successfully deleted in "
                   "VSD")

        # Releasing acquired public IP
        self.debug("Releasing the acquired public IP in the created Isolated "
                   "network...")
        public_ip.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_PublicIPAddress(public_ip, network_3)
        self.debug("Acquired public IP in the created Isolated network "
                   "successfully released in CloudStack")
        self.delete_VM(vm_3)

        self.debug("Creating an Isolated network with Static NAT service "
                   "provider as VirtualRouter...")
        network_4 = self.create_Network(net_off_4, gateway='10.1.1.1')
        self.validate_Network(network_4, state="Allocated")

        self.debug("Deploying a VM in the created Isolated network...")
        vm_4 = self.create_VM(network_4)
        self.validate_Network(network_4, state="Implemented")
        vr_4 = self.get_Router(network_4)
        self.check_Router_state(vr_4, state="Running")
        self.check_VM_state(vm_4, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, network_4)
        self.verify_vsd_router(vr_4)
        self.verify_vsd_vm(vm_4)

        # Creating Static NAT rule
        self.debug("Creating Static NAT rule in the created Isolated network "
                   "with its deployed VM...")
        public_ip = self.acquire_PublicIPAddress(network_4)
        self.validate_PublicIPAddress(public_ip, network_4)
        self.create_StaticNatRule_For_VM(vm_4, public_ip, network_4)
        self.validate_PublicIPAddress(
            public_ip, network_4, static_nat=True, vm=vm_4)

        # VSD verification for Static NAT functionality
        with self.assertRaises(Exception):
            self.verify_vsd_floating_ip(network_4, vm_4, public_ip.ipaddress)
        self.debug("Nuage VSP does not support VirtualRouter as the Static "
                   "NAT service provider")

        # Deleting Static NAT Rule
        self.debug("Deleting Static NAT Rule for the deployed VM...")
        self.delete_StaticNatRule_For_VM(public_ip)
        with self.assertRaises(Exception):
            self.validate_PublicIPAddress(
                public_ip, network_4, static_nat=True, vm=vm_4)
        self.debug("Static NAT Rule for the deployed VM successfully deleted "
                   "in CloudStack")

        # Releasing acquired public IP
        self.debug("Releasing the acquired public IP in the created Isolated "
                   "network...")
        public_ip.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_PublicIPAddress(public_ip, network_4)
        self.debug("Acquired public IP in the created Isolated network "
                   "successfully released in CloudStack")
        self.delete_VM(vm_4)

        self.debug("Creating an Isolated network with no Static NAT "
                   "service...")
        network_5 = self.create_Network(net_off_5, gateway='10.1.1.1')
        self.validate_Network(network_5, state="Allocated")

        self.debug("Deploying a VM in the created Isolated network...")
        vm_5 = self.create_VM(network_5)
        self.validate_Network(network_5, state="Implemented")
        vr_5 = self.get_Router(network_5)
        self.check_Router_state(vr_5, state="Running")
        self.check_VM_state(vm_5, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, network_5)
        self.verify_vsd_router(vr_5)
        self.verify_vsd_vm(vm_5)

        # Creating Static NAT rule
        self.debug("Creating Static NAT rule in the created Isolated network "
                   "with its deployed VM...")
        public_ip = self.acquire_PublicIPAddress(network_5)
        self.validate_PublicIPAddress(public_ip, network_5)
        with self.assertRaises(Exception):
            self.create_StaticNatRule_For_VM(vm_5, public_ip, network_5)
        self.debug("Static NAT rule creation failed as the network does not "
                   "support Static NAT service")

        # Releasing acquired public IP
        self.debug("Releasing the acquired public IP in the created Isolated "
                   "network...")
        public_ip.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_PublicIPAddress(public_ip, network_5)
        self.debug("Acquired public IP in the created Isolated network "
                   "successfully released in CloudStack")
        self.delete_VM(vm_5)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_04_nuage_StaticNAT_vpc_networks(self):
        """Test Nuage VSP VPC networks with different combinations of Static
            NAT service providers
        """

        # 1. Create Nuage VSP VPC offering with different combinations of
        #    Static NAT service providers
        #    (NuageVsp, VirtualRouter, no StaticNat service), check if all the
        #    VPC offerings are successfully created and enabled.
        # 2. Create VPCs with different combinations of Static NAT service
        #    providers (NuageVsp, VirtualRouter, no StaticNat service), check
        #    if only the VPCs with Static NAT service provider as NuageVsp and
        #    no StaticNat service are successfully created and enabled.
        # 3. Create Nuage VSP VPC Network offering with different combinations
        #    of Static NAT service providers
        #    (NuageVsp, VirtualRouter, no StaticNat service), check if all the
        #    network offerings are successfully created and enabled.
        # 4. Recreate the above created Network offering
        #    (Static NAT service provider as NuageVsp) with ispersistent flag
        #    set to False, check if the network offering is successfully
        #    created and enabled.
        # 5. Recreate the above created Network offering
        #    (Static NAT service provider as NuageVsp) with conserve mode On
        #    (conserve_mode flag set to True), check if the network offering
        #    creation failed as only networks with conserve mode Off can belong
        #    to VPC.
        # 6. Create a VPC network with Static NAT service provider as NuageVsp
        #    in the VPC with StaticNat service, spawn a VM, and create a Static
        #    NAT rule. Check if the tier is added to the VPC VR, and the VM is
        #    deployed successfully in the tier, verify if the Static NAT
        #    functionality for this network is successfully enabled in VSD.
        # 7. Create a non persistent VPC network with Static NAT service
        #    provider as NuageVsp in the VPC with StaticNat service, spawn a
        #    VM, and create a Static NAT rule. Check if the tier creation
        #    failed as Nuage VSP does not support non persistent VPC networks.
        # 8. Create a VPC network with Static NAT service provider as
        #    VpcVirtualRouter in the VPC with StaticNat service, spawn a VM,
        #    and create a Static NAT rule. Check if the tier is added to the
        #    VPC VR, and the VM is deployed successfully in the tier, verify if
        #    the Static NAT functionality for this network is not enabled in
        #    VSD as Nuage VSP does not support VirtualRouter as the Static NAT
        #    service provider.
        # 9. Create a VPC network with no Static NAT service in the VPC with
        #    StaticNat service, spawn a VM, and create a Static NAT rule. Check
        #    if the tier is added to the VPC VR, and the VM is deployed
        #    successfully in the tier, verify if the Static NAT functionality
        #    for this network is not enabled in both CloudStack and VSD as the
        #    network does not support Static NAT service.
        # 10. Create a VPC network with Static NAT service provider as NuageVsp
        #     in the VPC without StaticNat service, check if the tier creation
        #     failed as the VPC does not support Static NAT service.
        # 11. Delete all the created objects (cleanup).

        # Creating VPC offerings
        self.debug("Creating Nuage VSP VPC offering with Static NAT service "
                   "provider as NuageVsp...")
        vpc_off_1 = self.create_VpcOffering(
            self.test_data["nuagevsp"]["vpc_offering"])
        self.validate_VpcOffering(vpc_off_1, state="Enabled")

        self.debug("Creating Nuage VSP VPC offering with Static NAT service "
                   "provider as VpcVirtualRouter...")
        vpc_offering = copy.deepcopy(
            self.test_data["nuagevsp"]["vpc_offering"])
        vpc_offering["serviceProviderList"]["StaticNat"] = "VpcVirtualRouter"
        vpc_off_2 = self.create_VpcOffering(vpc_offering)
        self.validate_VpcOffering(vpc_off_2, state="Enabled")

        self.debug("Creating Nuage VSP VPC offering without Static NAT "
                   "service...")
        vpc_offering = copy.deepcopy(
            self.test_data["nuagevsp"]["vpc_offering"])
        vpc_offering["supportedservices"] = \
            'Dhcp,SourceNat,NetworkACL,Connectivity,UserData,Dns'
        del vpc_offering["serviceProviderList"]["StaticNat"]
        vpc_off_3 = self.create_VpcOffering(vpc_offering)
        self.validate_VpcOffering(vpc_off_3, state="Enabled")

        # Creating VPCs
        self.debug("Creating a VPC with Static NAT service provider as "
                   "NuageVsp...")
        vpc_1 = self.create_Vpc(vpc_off_1, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc_1, state="Enabled")

        self.debug("Creating a VPC with Static NAT service provider as "
                   "VpcVirtualRouter...")
        with self.assertRaises(Exception):
            self.create_Vpc(vpc_off_2, cidr='10.1.0.0/16')
        self.debug("Nuage VSP does not support provider VpcVirtualRouter for "
                   "service Static NAT for VPCs")

        self.debug("Creating a VPC without Static NAT service...")
        vpc_2 = self.create_Vpc(vpc_off_3, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc_2, state="Enabled")

        # Creating network offerings
        self.debug("Creating Nuage VSP VPC Network offering with Static NAT "
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

        self.debug("Creating Nuage VSP VPC Network offering with Static NAT "
                   "service provider as VpcVirtualRouter...")
        network_offering = copy.deepcopy(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        network_offering["serviceProviderList"]["StaticNat"] = \
            "VpcVirtualRouter"
        net_off_3 = self.create_NetworkOffering(network_offering)
        self.validate_NetworkOffering(net_off_3, state="Enabled")

        self.debug("Creating Nuage VSP VPC Network offering without Static "
                   "NAT service...")
        network_offering = copy.deepcopy(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        network_offering["supportedservices"] = \
            'Dhcp,SourceNat,NetworkACL,Connectivity,UserData,Dns'
        del network_offering["serviceProviderList"]["StaticNat"]
        net_off_4 = self.create_NetworkOffering(network_offering)
        self.validate_NetworkOffering(net_off_4, state="Enabled")

        # Creating VPC networks in the VPCs, and deploying VMs
        self.debug("Creating a VPC network with Static NAT service provider "
                   "as NuageVsp in vpc_1...")
        vpc_tier_1 = self.create_Network(
            net_off_1, gateway='10.1.1.1', vpc=vpc_1)
        self.validate_Network(vpc_tier_1, state="Implemented")
        vpc_vr = self.get_Router(vpc_tier_1)
        self.check_Router_state(vpc_vr, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, vpc_tier_1, vpc_1)
        self.verify_vsd_router(vpc_vr)

        self.debug("Deploying a VM in the created VPC network...")
        vpc_vm_1 = self.create_VM(vpc_tier_1)
        self.check_VM_state(vpc_vm_1, state="Running")

        # VSD verification
        self.verify_vsd_vm(vpc_vm_1)

        # Creating Static NAT rule
        self.debug("Creating Static NAT rule in the created VPC network with "
                   "its deployed VM...")
        public_ip = self.acquire_PublicIPAddress(vpc_tier_1, vpc=vpc_1)
        self.validate_PublicIPAddress(public_ip, vpc_tier_1)
        self.create_StaticNatRule_For_VM(vpc_vm_1, public_ip, vpc_tier_1)
        self.validate_PublicIPAddress(
            public_ip, vpc_tier_1, static_nat=True, vm=vpc_vm_1)

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(
            vpc_tier_1, vpc_vm_1, public_ip.ipaddress, vpc=vpc_1)

        # Deleting Static NAT Rule
        self.debug("Deleting Static NAT Rule for the deployed VM...")
        self.delete_StaticNatRule_For_VM(public_ip)
        with self.assertRaises(Exception):
            self.validate_PublicIPAddress(
                public_ip, vpc_tier_1, static_nat=True, vm=vpc_vm_1)
        self.debug("Static NAT Rule for the deployed VM successfully deleted "
                   "in CloudStack")

        # VSD verification
        with self.assertRaises(Exception):
            self.verify_vsd_floating_ip(
                vpc_tier_1, vpc_vm_1, public_ip.ipaddress, vpc=vpc_1)
        self.debug("Floating IP for the deployed VM successfully deleted in "
                   "VSD")

        # Releasing acquired public IP
        self.debug("Releasing the acquired public IP in the created VPC "
                   "network...")
        public_ip.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_PublicIPAddress(public_ip, vpc_tier_1)
        self.debug("Acquired public IP in the created VPC network "
                   "successfully released in CloudStack")

        self.debug("Creating a non persistent VPC network with Static NAT "
                   "service in vpc_1...")
        with self.assertRaises(Exception):
            self.create_Network(net_off_2, gateway='10.1.2.1', vpc=vpc_1)
        self.debug("Nuage VSP does not support non persistent VPC networks")

        self.debug("Creating a VPC network with Static NAT service provider "
                   "as VpcVirtualRouter in vpc_1...")
        with self.assertRaises(Exception):
            self.create_Network(net_off_3, gateway='10.1.2.1', vpc=vpc_1)
        self.debug("Provider VpcVirtualRouter is not supported for Static NAT "
                   "service in VPC vpc_1")

        self.debug("Creating a VPC network with no Static NAT service in "
                   "vpc_1...")
        vpc_tier_2 = self.create_Network(
            net_off_4, gateway='10.1.2.1', vpc=vpc_1)
        self.validate_Network(vpc_tier_2, state="Implemented")
        vpc_vr = self.get_Router(vpc_tier_2)
        self.check_Router_state(vpc_vr, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, vpc_tier_2, vpc_1)
        self.verify_vsd_router(vpc_vr)

        self.debug("Deploying a VM in the created VPC network...")
        vpc_vm_2 = self.create_VM(vpc_tier_2)
        self.check_VM_state(vpc_vm_2, state="Running")

        # VSD verification
        self.verify_vsd_vm(vpc_vm_2)

        # Creating Static NAT rule
        self.debug("Creating Static NAT rule in the created VPC network with "
                   "its deployed VM...")
        public_ip = self.acquire_PublicIPAddress(vpc_tier_2, vpc=vpc_1)
        self.validate_PublicIPAddress(public_ip, vpc_tier_2)
        with self.assertRaises(Exception):
            self.create_StaticNatRule_For_VM(vpc_vm_2, public_ip, vpc_tier_2)
        self.debug("Static NAT rule creation failed as the network does not "
                   "support Static NAT service")

        # Releasing acquired public IP
        self.debug("Releasing the acquired public IP in the created VPC "
                   "network...")
        public_ip.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_PublicIPAddress(public_ip, vpc_tier_2)
        self.debug("Acquired public IP in the created VPC network "
                   "successfully released in CloudStack")

        self.debug("Creating a VPC network with Static NAT service provider "
                   "as NuageVsp in vpc_2...")
        with self.assertRaises(Exception):
            self.create_Network(net_off_1, gateway='10.1.1.1', vpc=vpc_2)
        self.debug("VPC Network creation failed as vpc_2 does not support "
                   "Static NAT service")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_05_nuage_StaticNAT_isolated_networks_traffic(self):
        """Test Nuage VSP Static NAT functionality for Isolated network by
        performing (wget) traffic tests to the Internet
        """

        # 1. Create an Isolated network with Static NAT service provider as
        #    NuageVsp, spawn a VM, and create a Static NAT rule. Check if the
        #    network is successfully created, and the VM along with the VR is
        #    deployed successfully in the network, verify if the Static NAT
        #    functionality for this network is successfully enabled in VSD.
        # 2. Perform and verify Static NAT traffic test (wget www.google.com)
        #    to the Internet from the deployed VM.
        # 3. Deploy another VM in the created Isolated network and create a
        #    Static NAT rule. Check if the VM is deployed successfully in the
        #    network, verify if the Static NAT functionality for this network
        #    is successfully enabled in VSD.
        # 4. Perform and verify Static NAT traffic test (wget www.google.com)
        #    to the Internet from the deployed VM.
        # 5. Delete all the created objects (cleanup).
        # Note: Above mentioned Static NAT traffic test is done by SSHing into
        #       the VM using a Static NAT rule, and performing wget traffic
        #       test (wget www.google.com) to the Internet from the VM.

        # Creating network offering
        self.debug("Creating Nuage VSP Isolated Network offering with Static "
                   "NAT service provider as NuageVsp...")
        net_off = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["isolated_network_offering"])
        self.validate_NetworkOffering(net_off, state="Enabled")

        # Creating Isolated network, deploying VMs, and verifying Static NAT
        # traffic
        self.debug("Creating an Isolated network with Static NAT service...")
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

        # Creating Static NAT rule
        self.debug("Creating Static NAT rule for the deployed VM in the "
                   "created Isolated network...")
        public_ip_1 = self.acquire_PublicIPAddress(network)
        self.validate_PublicIPAddress(public_ip_1, network)
        self.create_StaticNatRule_For_VM(vm_1, public_ip_1, network)
        self.validate_PublicIPAddress(
            public_ip_1, network, static_nat=True, vm=vm_1)

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(network, vm_1, public_ip_1.ipaddress)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(network, public_ip_1)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(vm_1, network, public_ip_1)

        self.debug("Deploying another VM in the created Isolated network...")
        vm_2 = self.create_VM(network)
        self.check_VM_state(vm_2, state="Running")

        # VSD verification
        self.verify_vsd_vm(vm_2)

        # Creating Static NAT rule
        self.debug("Creating Static NAT rule for the deployed VM in the "
                   "created Isolated network...")
        public_ip_2 = self.acquire_PublicIPAddress(network)
        self.validate_PublicIPAddress(public_ip_2, network)
        self.create_StaticNatRule_For_VM(vm_2, public_ip_2, network)
        self.validate_PublicIPAddress(
            public_ip_2, network, static_nat=True, vm=vm_2)

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(network, vm_2, public_ip_2.ipaddress)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(network, public_ip_2)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(vm_2, network, public_ip_2)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_06_nuage_StaticNAT_vpc_network_traffic(self):
        """Test Nuage VSP Static NAT functionality for VPC network by
        performing (wget) traffic tests to the Internet
        """

        # 1. Create a VPC network with Static NAT service provider as NuageVsp
        #    in the VPC with StaticNat service, spawn a VM, and create a Static
        #    NAT rule. Check if the tier is added to the VPC VR, and the VM is
        #    deployed successfully in the tier, verify if the Static NAT
        #    functionality for this network is successfully enabled in VSD.
        # 2. Perform and verify Static NAT traffic test (wget www.google.com)
        #    to the Internet from the deployed VM.
        # 3. Deploy another VM in the created VPC network and create a Static
        #    NAT rule. Check if the VM is deployed successfully in the network,
        #    verify if the Static NAT functionality for this network is
        #    successfully enabled in VSD.
        # 4. Perform and verify Static NAT traffic test (wget www.google.com)
        #    to the Internet from the deployed VM.
        # 5. Delete all the created objects (cleanup).
        # Note: Above mentioned Static NAT traffic test is done by SSHing into
        #       the VM using a Static NAT rule, and performing wget traffic
        #       test (wget www.google.com) to the Internet from the VM.

        # Creating VPC offering
        self.debug("Creating Nuage VSP VPC offering with Static NAT service "
                   "provider as NuageVsp...")
        vpc_off = self.create_VpcOffering(
            self.test_data["nuagevsp"]["vpc_offering"])
        self.validate_VpcOffering(vpc_off, state="Enabled")

        # Creating VPC
        self.debug("Creating a VPC with Static NAT service provider as "
                   "NuageVsp...")
        vpc = self.create_Vpc(vpc_off, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc, state="Enabled")

        # Creating network offering
        self.debug("Creating Nuage VSP VPC Network offering with Static NAT "
                   "service provider as NuageVsp...")
        net_off = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        self.validate_NetworkOffering(net_off, state="Enabled")

        # Creating VPC network in the VPC, deploying VMs, and verifying Static
        # NAT traffic
        self.debug("Creating a VPC network with Static NAT service...")
        vpc_tier = self.create_Network(net_off, gateway='10.1.1.1', vpc=vpc)
        self.validate_Network(vpc_tier, state="Implemented")
        vpc_vr = self.get_Router(vpc_tier)
        self.check_Router_state(vpc_vr, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, vpc_tier, vpc)
        self.verify_vsd_router(vpc_vr)

        # Adding Egress Network ACL rules
        self.debug("Adding Egress Network ACL rules in the created VPC "
                   "network to allow Static NAT (DNS & HTTP) traffic to the "
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

        # Creating Static NAT rule
        self.debug("Creating Static NAT rule for the deployed VM in the "
                   "created VPC network...")
        public_ip_1 = self.acquire_PublicIPAddress(vpc_tier, vpc=vpc)
        self.validate_PublicIPAddress(public_ip_1, vpc_tier)
        self.create_StaticNatRule_For_VM(vpc_vm_1, public_ip_1, vpc_tier)
        self.validate_PublicIPAddress(
            public_ip_1, vpc_tier, static_nat=True, vm=vpc_vm_1)

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(
            vpc_tier, vpc_vm_1, public_ip_1.ipaddress, vpc=vpc)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(vpc_tier, public_ip_1, vpc=vpc)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(
            vpc_vm_1, vpc_tier, public_ip_1, vpc=vpc)

        self.debug("Deploying another VM in the created VPC network...")
        vpc_vm_2 = self.create_VM(vpc_tier)
        self.check_VM_state(vpc_vm_2, state="Running")

        # VSD verification
        self.verify_vsd_vm(vpc_vm_2)

        # Creating Static NAT rule
        self.debug("Creating Static NAT rule for the deployed VM in the "
                   "created VPC network...")
        public_ip_2 = self.acquire_PublicIPAddress(vpc_tier, vpc=vpc)
        self.validate_PublicIPAddress(public_ip_2, vpc_tier)
        self.create_StaticNatRule_For_VM(vpc_vm_2, public_ip_2, vpc_tier)
        self.validate_PublicIPAddress(
            public_ip_2, vpc_tier, static_nat=True, vm=vpc_vm_2)

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(
            vpc_tier, vpc_vm_2, public_ip_2.ipaddress, vpc=vpc)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(vpc_tier, public_ip_2, vpc=vpc)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(
            vpc_vm_2, vpc_tier, public_ip_2, vpc=vpc)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_07_nuage_StaticNAT_acl_rules_traffic(self):
        """Test Nuage VSP Static NAT functionality with different Egress
        Firewall/Network ACL rules by performing (wget) traffic tests to the
        Internet
        """

        # Repeat the tests in the testcases
        # "test_05_nuage_StaticNAT_isolated_networks_traffic" and
        # "test_06_nuage_StaticNAT_vpc_network_traffic" with different Egress
        # Firewall/Network ACL rules:
        # 1. Allow and block Egress Firewall rules
        # 2. Allow and block Egress Network ACL rules
        # Verify the above Egress Firewall/Network ACL rules by performing and
        # verifying Static NAT traffic test (wget www.google.com) to the
        # Internet from the VM.
        # Delete all the created objects (cleanup).

        # Creating network offering
        self.debug("Creating Nuage VSP Isolated Network offering with Static "
                   "NAT service provider as NuageVsp...")
        net_off = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["isolated_network_offering"])
        self.validate_NetworkOffering(net_off, state="Enabled")

        # Creating Isolated network, deploying VMs, and verifying Static NAT
        # traffic with Egress Firewall rules
        self.debug("Creating an Isolated network with Static NAT service...")
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

        # Creating Static NAT rule
        self.debug("Creating Static NAT rule for the deployed VM in the "
                   "created Isolated network...")
        public_ip_1 = self.acquire_PublicIPAddress(network)
        self.validate_PublicIPAddress(public_ip_1, network)
        self.create_StaticNatRule_For_VM(vm, public_ip_1, network)
        self.validate_PublicIPAddress(
            public_ip_1, network, static_nat=True, vm=vm)

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(network, vm, public_ip_1.ipaddress)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(network, public_ip_1)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(vm, network, public_ip_1)

        # Adding Egress Firewall rule
        self.debug("Adding an Egress Firewall rule in the created Isolated "
                   "network to block/drop Static NAT (DNS) traffic to the "
                   "Internet from the VMs in the network...")
        dns_rule_1 = self.create_EgressFirewallRule(
            network, self.test_data["dns_rule"])

        # VSD verification for added Egress Firewall rule
        self.verify_vsd_firewall_rule(dns_rule_1, traffic_type="Egress")

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(network, vm, public_ip_1.ipaddress)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(network, public_ip_1)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        with self.assertRaises(Exception):
            self.verify_StaticNAT_Internet_traffic(
                vm, network, public_ip_1, negative_test=True)
        self.debug("Static NAT (DNS) traffic to the Internet from the "
                   "deployed VM is blocked/dropped by the added Egress "
                   "Firewall rule")

        # Removing Egress Firewall rule
        self.debug("Removing the added Egress Firewall rule in the created "
                   "Isolated network to allow Static NAT (DNS) traffic to "
                   "the Internet from the VMs in the network "
                   "(Default Egress Firewall rule)...")
        dns_rule_1.delete(self.api_client)

        # VSD verification for removed Egress Firewall rule
        with self.assertRaises(Exception):
            self.verify_vsd_firewall_rule(dns_rule_1, traffic_type="Egress")
        self.debug("Egress Firewall rule successfully deleted in VSD")

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(network, vm, public_ip_1.ipaddress)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(network, public_ip_1)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(vm, network, public_ip_1)

        # Creating VPC offering
        self.debug("Creating Nuage VSP VPC offering with Static NAT service "
                   "provider as NuageVsp...")
        vpc_off = self.create_VpcOffering(
            self.test_data["nuagevsp"]["vpc_offering"])
        self.validate_VpcOffering(vpc_off, state="Enabled")

        # Creating VPC
        self.debug("Creating a VPC with Static NAT service provider as "
                   "NuageVsp...")
        vpc = self.create_Vpc(vpc_off, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc, state="Enabled")

        # Creating network offering
        self.debug("Creating Nuage VSP VPC Network offering with Static NAT "
                   "service provider as NuageVsp...")
        net_off = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        self.validate_NetworkOffering(net_off, state="Enabled")

        # Creating VPC network in the VPC, deploying VMs, and verifying Static
        # NAT traffic with Network ACl rules
        self.debug("Creating a VPC network with Static NAT service...")
        vpc_tier = self.create_Network(net_off, gateway='10.1.1.1', vpc=vpc)
        self.validate_Network(vpc_tier, state="Implemented")
        vpc_vr = self.get_Router(vpc_tier)
        self.check_Router_state(vpc_vr, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, vpc_tier, vpc)
        self.verify_vsd_router(vpc_vr)

        # Adding Egress Network ACL rules
        self.debug("Adding Egress Network ACL rules in the created VPC "
                   "network to allow Static NAT (DNS & HTTP) traffic to the "
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

        # Creating Static NAT rule
        self.debug("Creating Static NAT rule for the deployed VM in the "
                   "created VPC network...")
        public_ip_2 = self.acquire_PublicIPAddress(vpc_tier, vpc=vpc)
        self.validate_PublicIPAddress(public_ip_2, vpc_tier)
        self.create_StaticNatRule_For_VM(vpc_vm, public_ip_2, vpc_tier)
        self.validate_PublicIPAddress(
            public_ip_2, vpc_tier, static_nat=True, vm=vpc_vm)

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(
            vpc_tier, vpc_vm, public_ip_2.ipaddress, vpc=vpc)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(vpc_tier, public_ip_2, vpc=vpc)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(
            vpc_vm, vpc_tier, public_ip_2, vpc=vpc)

        # Removing Egress Network ACL rule
        self.debug("Removing the added Egress Network ACL rule in the created "
                   "VPC network to block Static NAT (DNS) traffic to the "
                   "Internet from the VMs in the network "
                   "(Default Egress Network ACL rule)...")
        dns_rule_2.delete(self.api_client)

        # VSD verification for removed Egress Network ACL rule
        with self.assertRaises(Exception):
            self.verify_vsd_firewall_rule(dns_rule_2, traffic_type="Egress")
        self.debug("Egress Network ACL rule successfully deleted in VSD")

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(
            vpc_tier, vpc_vm, public_ip_2.ipaddress, vpc=vpc)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(vpc_tier, public_ip_2, vpc=vpc)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        with self.assertRaises(Exception):
            self.verify_StaticNAT_Internet_traffic(
                vpc_vm, vpc_tier, public_ip_2, vpc=vpc, negative_test=True)
        self.debug("Static NAT (DNS) traffic to the Internet from the "
                   "deployed VM is blocked by the Default Egress Network ACL "
                   "rule")

        # Re-adding Egress Network ACL rule
        self.debug("Re-adding the Egress Network ACL rule in the created VPC "
                   "network to allow Static NAT (DNS) traffic to the "
                   "Internet from the VMs in the network...")
        dns_rule_2 = self.create_NetworkAclRule(
            self.test_data["dns_rule"], traffic_type="Egress",
            network=vpc_tier)

        # VSD verification for re-added Egress Network ACL rule
        self.verify_vsd_firewall_rule(dns_rule_2, traffic_type="Egress")

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(
            vpc_tier, vpc_vm, public_ip_2.ipaddress, vpc=vpc)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(vpc_tier, public_ip_2, vpc=vpc)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(
            vpc_vm, vpc_tier, public_ip_2, vpc=vpc)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_08_nuage_StaticNAT_vm_nic_operations_traffic(self):
        """Test Nuage VSP Static NAT functionality with VM NIC operations by
        performing (wget) traffic tests to the Internet
        """

        # Repeat the tests in the testcase
        # "test_05_nuage_StaticNAT_isolated_networks_traffic" with VM NIC
        # operations:
        # 1. Updating default VM NIC
        # 2. Removing non-default VM NIC
        # 3. Adding and updating default VM NIC
        # Verify the above VM NIC operations by performing and verifying Static
        # NAT traffic test (wget www.google.com) to the Internet from the VM.
        # Delete all the created objects (cleanup).

        # Creating network offering
        self.debug("Creating Nuage VSP Isolated Network offering with Static "
                   "NAT service provider as NuageVsp...")
        net_off = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["isolated_network_offering"])
        self.validate_NetworkOffering(net_off, state="Enabled")

        # Creating Isolated networks, deploying a multi-nic VM, and verifying
        # Static NAT traffic with VM NIC operations
        self.debug("Creating an Isolated network with Static NAT service...")
        network_1 = self.create_Network(net_off, gateway='10.1.1.1')
        self.validate_Network(network_1, state="Allocated")

        self.debug("Creating another Isolated network with Static NAT "
                   "service...")
        network_2 = self.create_Network(net_off, gateway='10.1.1.1')
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

        # Creating Static NAT rule
        self.debug("Creating Static NAT rule for the deployed VM in the "
                   "created Isolated network...")
        public_ip_1 = self.acquire_PublicIPAddress(network_1)
        self.validate_PublicIPAddress(public_ip_1, network_1)
        self.create_StaticNatRule_For_VM(vm, public_ip_1, network_1)
        self.validate_PublicIPAddress(
            public_ip_1, network_1, static_nat=True, vm=vm)

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(network_1, vm, public_ip_1.ipaddress)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(network_1, public_ip_1)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(vm, network_1, public_ip_1)

        # Creating Static NAT rule
        self.debug("Creating Static NAT rule for the deployed VM in the "
                   "created Isolated network...")
        public_ip_2 = self.acquire_PublicIPAddress(network_2)
        self.validate_PublicIPAddress(public_ip_2, network_2)
        self.create_StaticNatRule_For_VM(vm, public_ip_2, network_2)
        self.validate_PublicIPAddress(
            public_ip_2, network_2, static_nat=True, vm=vm)

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(network_2, vm, public_ip_2.ipaddress)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(
            network_2, public_ip_2, non_default_nic=True)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(
            vm, network_2, public_ip_2, non_default_nic=True)

        # Updating default VM NIC
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

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(network_1, vm, public_ip_1.ipaddress)
        self.verify_vsd_floating_ip(network_2, vm, public_ip_2.ipaddress)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(
            network_1, public_ip_1, non_default_nic=True)
        self.verify_StaticNAT_traffic(network_2, public_ip_2)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(
            vm, network_1, public_ip_1, non_default_nic=True)
        self.verify_StaticNAT_Internet_traffic(vm, network_2, public_ip_2)

        # Removing non-default VM NIC
        self.debug("Removing the non-default nic of the multi-nic VM...")
        with self.assertRaises(Exception):
            self.nic_operation_VM(vm, network_1, operation="remove")
        self.debug("Can not remove this NIC as Static NAT rule is enabled on "
                   "it")

        # Deleting Static NAT Rule
        self.debug("Deleting Static NAT Rule for the deployed VM...")
        self.delete_StaticNatRule_For_VM(public_ip_1)
        with self.assertRaises(Exception):
            self.validate_PublicIPAddress(
                public_ip_1, network_1, static_nat=True, vm=vm)
        self.debug("Static NAT Rule for the deployed VM successfully deleted "
                   "in CloudStack")

        # VSD verification
        with self.assertRaises(Exception):
            self.verify_vsd_floating_ip(network_1, vm, public_ip_1.ipaddress)
        self.debug("Floating IP for the deployed VM successfully deleted in "
                   "VSD")

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

        # VSD verification for Static NAT functionality
        with self.assertRaises(Exception):
            self.verify_vsd_floating_ip(network_1, vm, public_ip_1.ipaddress)
        self.debug("Static NAT rule not enabled in this VM NIC")
        self.verify_vsd_floating_ip(network_2, vm, public_ip_2.ipaddress)

        # Verifying Static NAT traffic
        with self.assertRaises(AssertionError):
            self.verify_StaticNAT_traffic(network_1, public_ip_1)
        self.debug("Static NAT rule not enabled in this VM NIC")
        self.verify_StaticNAT_traffic(network_2, public_ip_2)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        with self.assertRaises(Exception):
            self.verify_StaticNAT_Internet_traffic(vm, network_1, public_ip_1)
        self.debug("Static NAT rule not enabled in this VM NIC")
        self.verify_StaticNAT_Internet_traffic(vm, network_2, public_ip_2)

        # Adding and updating default VM NIC
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

        # Creating Static NAT rule
        self.debug("Creating Static NAT rule for the deployed VM in the "
                   "created Isolated network...")
        self.create_StaticNatRule_For_VM(vm, public_ip_1, network_1)
        self.validate_PublicIPAddress(
            public_ip_1, network_1, static_nat=True, vm=vm)

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(network_1, vm, public_ip_1.ipaddress)
        self.verify_vsd_floating_ip(network_2, vm, public_ip_2.ipaddress)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(network_1, public_ip_1)
        self.verify_StaticNAT_traffic(
            network_2, public_ip_2, non_default_nic=True)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(vm, network_1, public_ip_1)
        self.verify_StaticNAT_Internet_traffic(
            vm, network_2, public_ip_2, non_default_nic=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_09_nuage_StaticNAT_vm_migration_traffic(self):
        """Test Nuage VSP Static NAT functionality with VM migration by
        performing (wget) traffic tests to the Internet
        """

        # Repeat the tests in the testcase
        # "test_05_nuage_StaticNAT_isolated_networks_traffic" with migration of
        # one of the VMs to another host (if available).
        # Verify the above VM migration by performing and verifying Static NAT
        # traffic test (wget www.google.com) to the Internet from the VM.
        # Delete all the created objects (cleanup).

        # Creating network offering
        self.debug("Creating Nuage VSP Isolated Network offering with Static "
                   "NAT service provider as NuageVsp...")
        net_off = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["isolated_network_offering"])
        self.validate_NetworkOffering(net_off, state="Enabled")

        # Creating an Isolated network, deploying VMs, and verifying Static
        # NAT traffic with VM migrations
        self.debug("Creating an Isolated network with Static NAT service...")
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

        # Creating Static NAT rule
        self.debug("Creating Static NAT rule for the deployed VM in the "
                   "created Isolated network...")
        public_ip_1 = self.acquire_PublicIPAddress(network)
        self.validate_PublicIPAddress(public_ip_1, network)
        self.create_StaticNatRule_For_VM(vm_1, public_ip_1, network)
        self.validate_PublicIPAddress(
            public_ip_1, network, static_nat=True, vm=vm_1)

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(network, vm_1, public_ip_1.ipaddress)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(network, public_ip_1)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(vm_1, network, public_ip_1)

        self.debug("Deploying another VM in the created Isolated network...")
        vm_2 = self.create_VM(network)
        self.check_VM_state(vm_2, state="Running")

        # VSD verification
        self.verify_vsd_vm(vm_2)

        # Creating Static NAT rule
        self.debug("Creating Static NAT rule for the deployed VM in the "
                   "created Isolated network...")
        public_ip_2 = self.acquire_PublicIPAddress(network)
        self.validate_PublicIPAddress(public_ip_2, network)
        self.create_StaticNatRule_For_VM(vm_2, public_ip_2, network)
        self.validate_PublicIPAddress(
            public_ip_2, network, static_nat=True, vm=vm_2)

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(network, vm_2, public_ip_2.ipaddress)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(network, public_ip_2)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(vm_2, network, public_ip_2)

        # VM migration
        # This VM migration has no effect on the Static NAT functionality
        self.debug("Migrating one of the VMs in the created Isolated network "
                   "to another host, if available...")
        self.migrate_VM(vm_1)

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(network, vm_1, public_ip_1.ipaddress)
        self.verify_vsd_floating_ip(network, vm_2, public_ip_2.ipaddress)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(network, public_ip_1)
        self.verify_StaticNAT_traffic(network, public_ip_2)

        # VSD verification for Static NAT functionality
        self.verify_StaticNAT_Internet_traffic(vm_1, network, public_ip_1)
        self.verify_StaticNAT_Internet_traffic(vm_2, network, public_ip_2)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_10_nuage_StaticNAT_network_restarts_traffic(self):
        """Test Nuage VSP Static NAT functionality with network restarts by
        performing (wget) traffic tests to the Internet
        """

        # Repeat the tests in the testcases
        # "test_05_nuage_StaticNAT_isolated_networks_traffic" and
        # "test_06_nuage_StaticNAT_vpc_network_traffic" with network restarts:
        # 1. Restart Isolated Network (cleanup = false)
        # 2. Restart Isolated Network (cleanup = true)
        # 3. Reboot VM in the Isolated Network
        # 4. Restart VPC Network (cleanup = false)
        # 5. Restart VPC Network (cleanup = true)
        # 6. Reboot VM in the VPC Network
        # 7. Restart VPC (cleanup = false)
        # 8. Restart VPC (cleanup = true)
        # Verify the above network restarts by performing and verifying Static
        # NAT traffic test (wget www.google.com) to the Internet from the VM.
        # Delete all the created objects (cleanup).

        # Creating network offering
        self.debug("Creating Nuage VSP Isolated Network offering with Static "
                   "NAT service provider as NuageVsp...")
        net_off = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["isolated_network_offering"])
        self.validate_NetworkOffering(net_off, state="Enabled")

        # Creating an Isolated network, deploying a VM, and verifying Static
        # NAT traffic with Isolated network restarts
        self.debug("Creating an Isolated network with Static NAT service...")
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

        # Creating Static NAT rule
        self.debug("Creating Static NAT rule for the deployed VM in the "
                   "created Isolated network...")
        public_ip_1 = self.acquire_PublicIPAddress(network)
        self.validate_PublicIPAddress(public_ip_1, network)
        self.create_StaticNatRule_For_VM(vm, public_ip_1, network)
        self.validate_PublicIPAddress(
            public_ip_1, network, static_nat=True, vm=vm)

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(network, vm, public_ip_1.ipaddress)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(network, public_ip_1)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(vm, network, public_ip_1)

        # Restarting Isolated network (cleanup = false)
        # VR gets destroyed and deployed again in the Isolated network
        # This restart has no effect on the Static NAT functionality
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

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(network, vm, public_ip_1.ipaddress)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(network, public_ip_1)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(vm, network, public_ip_1)

        # Restarting Isolated network (cleanup = true)
        # VR gets destroyed and deployed again in the Isolated network
        # This restart has no effect on the Static NAT functionality
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

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(network, vm, public_ip_1.ipaddress)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(network, public_ip_1)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(vm, network, public_ip_1)

        # Rebooting (stop - start) VM
        # This reboot has no effect on the Static NAT functionality
        self.debug("Rebooting the deployed VM in the created Isolated "
                   "network...")
        vm.stop(self.api_client)
        vm.start(self.api_client)
        self.validate_Network(network, state="Implemented")
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(vm, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, network)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(vm)

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(network, vm, public_ip_1.ipaddress)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(network, public_ip_1)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(vm, network, public_ip_1)

        # Creating VPC offering
        self.debug("Creating Nuage VSP VPC offering with Static NAT service "
                   "provider as NuageVsp...")
        vpc_off = self.create_VpcOffering(
            self.test_data["nuagevsp"]["vpc_offering"])
        self.validate_VpcOffering(vpc_off, state="Enabled")

        # Creating VPC
        self.debug("Creating a VPC with Static NAT service provider as "
                   "NuageVsp...")
        vpc = self.create_Vpc(vpc_off, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc, state="Enabled")

        # Creating VPC network offering
        self.debug("Creating Nuage VSP VPC Network offering with Static NAT "
                   "service provider as NuageVsp...")
        net_off = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering"])
        self.validate_NetworkOffering(net_off, state="Enabled")

        # Creating a VPC network in the VPC, deploying a VM, and verifying
        # Static NAT traffic with VPC network restarts
        self.debug("Creating a VPC network with Static NAT service...")
        vpc_tier = self.create_Network(net_off, gateway='10.1.1.1', vpc=vpc)
        self.validate_Network(vpc_tier, state="Implemented")
        vpc_vr = self.get_Router(vpc_tier)
        self.check_Router_state(vpc_vr, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, vpc_tier, vpc)
        self.verify_vsd_router(vpc_vr)

        # Adding Egress Network ACL rules
        self.debug("Adding Egress Network ACL rules in the created VPC "
                   "network to allow Static NAT (DNS & HTTP) traffic to the "
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

        # Creating Static NAT rule
        self.debug("Creating Static NAT rule for the deployed VM in the "
                   "created VPC network...")
        public_ip_2 = self.acquire_PublicIPAddress(vpc_tier, vpc=vpc)
        self.validate_PublicIPAddress(public_ip_2, vpc_tier)
        self.create_StaticNatRule_For_VM(vpc_vm, public_ip_2, vpc_tier)
        self.validate_PublicIPAddress(
            public_ip_2, vpc_tier, static_nat=True, vm=vpc_vm)

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(
            vpc_tier, vpc_vm, public_ip_2.ipaddress, vpc=vpc)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(vpc_tier, public_ip_2, vpc=vpc)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(
            vpc_vm, vpc_tier, public_ip_2, vpc=vpc)

        # Restarting VPC network (cleanup = false)
        # This restart has no effect on the Static NAT functionality
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

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(
            vpc_tier, vpc_vm, public_ip_2.ipaddress, vpc=vpc)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(vpc_tier, public_ip_2, vpc=vpc)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(
            vpc_vm, vpc_tier, public_ip_2, vpc=vpc)

        # Restarting VPC network (cleanup = true)
        # This restart has no effect on the Static NAT functionality
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

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(
            vpc_tier, vpc_vm, public_ip_2.ipaddress, vpc=vpc)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(vpc_tier, public_ip_2, vpc=vpc)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(
            vpc_vm, vpc_tier, public_ip_2, vpc=vpc)

        # Rebooting (stop - start) VM
        # This reboot has no effect on the Static NAT functionality
        self.debug("Rebooting the deployed VM in the created VPC network...")
        vpc_vm.stop(self.api_client)
        vpc_vm.start(self.api_client)
        self.validate_Network(vpc_tier, state="Implemented")
        self.check_Router_state(vpc_vr, state="Running")
        self.check_VM_state(vpc_vm, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, vpc_tier, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(vm)
        self.verify_vsd_firewall_rule(dns_rule, traffic_type="Egress")
        self.verify_vsd_firewall_rule(http_rule, traffic_type="Egress")

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(
            vpc_tier, vpc_vm, public_ip_2.ipaddress, vpc=vpc)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(vpc_tier, public_ip_2, vpc=vpc)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(
            vpc_vm, vpc_tier, public_ip_2, vpc=vpc)

        # Restarting VPC (cleanup = false)
        # VPC VR gets destroyed and deployed again in the VPC
        # This restart has no effect on the Static NAT functionality
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

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(
            vpc_tier, vpc_vm, public_ip_2.ipaddress, vpc=vpc)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(vpc_tier, public_ip_2, vpc=vpc)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(
            vpc_vm, vpc_tier, public_ip_2, vpc=vpc)

        # Restarting VPC (cleanup = true)
        # VPC VR gets destroyed and deployed again in the VPC
        # This restart has no effect on the Static NAT functionality
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

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(
            vpc_tier, vpc_vm, public_ip_2.ipaddress, vpc=vpc)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(vpc_tier, public_ip_2, vpc=vpc)

        # Verifying Static NAT traffic (wget www.google.com) to the Internet
        # from the deployed VM
        self.verify_StaticNAT_Internet_traffic(
            vpc_vm, vpc_tier, public_ip_2, vpc=vpc)

    # Bug CLOUDSTACK-9751
    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_11_nuage_enable_staticNat_when_vr_is_in_starting_state(self):
        """Test Nuage VSP Static NAT functionality by enabling Static Nat when
        VR is in starting state
        """

        # 1. Create a Nuage VSP Isolated network offering.
        # 2. Create an Isolated network with above created offering.
        # 3. Deploy a VM in the above created Isolated network,
        #    which starts a VR.
        # 4. While VR is in the starting state, acquire a public IP and enable
        #    static nat in another thread.
        # 5. Verify that Static NAT is successfully enabled in both CloudStack
        #    and VSD.
        # 6. Delete all the created objects (cleanup).

        # Creating network offering
        self.debug("Creating Nuage VSP Isolated Network offering with Static "
                   "NAT service provider as NuageVsp...")
        net_off = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["isolated_network_offering"])
        self.validate_NetworkOffering(net_off, state="Enabled")

        # Creating an Isolated network
        self.debug("Creating an Isolated network with Static NAT service...")
        self.network = self.create_Network(net_off, gateway='10.1.1.1')
        self.validate_Network(self.network, state="Allocated")

        # Acquiring a Public IP
        self.debug("Acquiring a Public IP in the created Isolated network...")
        self.public_ip = self.acquire_PublicIPAddress(self.network)
        self.validate_PublicIPAddress(self.public_ip, self.network)

        # Enabling Static NAT on a starting VM
        self.debug("Creating a thread for enabling Static Nat on a starting "
                   "VM...")
        static_nat_thread = threading.Thread(
            name='enable_static_nat',
            target=self.enable_staticNat_on_a_starting_vm)
        static_nat_thread.start()

        vm = self.create_VM(self.network)

        # Check the status of Static Nat thread and if it is not finished then
        # below command will wait for it to finish
        self.debug("Waiting for for enabling Static Nat on a starting VM "
                   "thread to finish...")
        static_nat_thread.join()

        # CloudStack verification for the implemented Isolated Network
        self.validate_Network(self.network, state="Implemented")
        vr = self.get_Router(self.network)
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(vm, state="Running")

        # VSD verification for the implemented Isolated Network
        self.verify_vsd_network(self.domain.id, self.network)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(vm)

        # CloudStack verification for Static NAT functionality
        self.validate_PublicIPAddress(
            self.public_ip, self.network, static_nat=True, vm=vm)

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(self.network, vm, self.public_ip.ipaddress)

        # Verifying Static NAT traffic
        self.verify_StaticNAT_traffic(self.network, self.public_ip)
