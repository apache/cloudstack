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

""" Component tests for extra dhcp options functionality with
Nuage VSP SDN plugin
"""
# Import Local Modules
from nuageTestCase import (nuageTestCase, gherkin)
from marvin.cloudstackAPI import updateVirtualMachine, updateZone
from marvin.lib.base import (Account,
                             Network,
                             VirtualMachine,
                             Configurations,
                             NetworkOffering)
# Import System Modules
from concurrent.futures import ThreadPoolExecutor, wait
from nose.plugins.attrib import attr
import copy
import time


class TestNuageExtraDhcp(nuageTestCase):
    """ Test basic VPC Network functionality with Nuage VSP SDN plugin
    """

    @classmethod
    def setUpClass(cls, zone=None):
        super(TestNuageExtraDhcp, cls).setUpClass()
        cls.vmdata = cls.test_data["virtual_machine"]
        cls.sharednetworkdata = cls.test_data["acl"]

        # Create an account
        cls.account = Account.create(cls.api_client,
                                     cls.test_data["account"],
                                     admin=True,
                                     domainid=cls.domain.id
                                     )

        cmd = updateZone.updateZoneCmd()
        cmd.id = cls.zone.id
        cmd.domain = "testvpc.com"
        cls.api_client.updateZone(cmd)
        cls.vpc_offering = cls.create_VpcOffering(
            cls.test_data["nuagevsp"]["vpc_offering_nuage_dhcp"])
        cls.vpc1 = cls.create_Vpc(cls.vpc_offering, cidr="10.0.0.0/16",
                                  networkDomain="testvpc.com")

        cls.vpc_network_offering = cls.create_NetworkOffering(
            cls.test_data["nuagevsp"]["vpc_network_offering_nuage_dhcp"])

        cls.vpc_network = cls.create_Network(
            cls.vpc_network_offering, gateway="10.0.0.1", vpc=cls.vpc1)

        cmd.domain = "testisolated.com"
        cls.api_client.updateZone(cmd)

        # create the isolated network
        cls.isolated_network_offering = cls.create_NetworkOffering(
            cls.test_data["nuagevsp"]["isolated_network_offering"], True)
        cls.isolated_network = cls.create_Network(
            cls.isolated_network_offering, gateway="10.0.0.1",
            netmask="255.255.255.0")

        cmd.domain = "testshared.com"
        cls.api_client.updateZone(cmd)
        # Create Shared Network
        cls.shared_network_offering = NetworkOffering.create(
            cls.api_client,
            cls.test_data["nuagevsp"]["shared_nuage_network_offering"],
            conservemode=False
        )

        cls.shared_network_offering.update(cls.api_client, state='Enabled')
        cls.shared_network_offering_id = cls.shared_network_offering.id

        cls.shared_network_all = Network.create(
            cls.api_client,
            cls.test_data["nuagevsp"]["network_all"],
            networkofferingid=cls.shared_network_offering_id,
            zoneid=cls.zone.id
        )
        cls.dhcp_options_map = {}
        cls.dhcp_options_to_verify_map = {}
        cls.expected_dhcp_options_on_vm = {}
        cls.dhcp_options_map_keys = [1, 16, 28, 41, 64, 93]

        cls._cleanup = [
            cls.shared_network_all,
            cls.shared_network_offering,
            cls.account
            ]
        return

    def setUp(self):
        self.vmdata["displayname"] = "vm"
        self.vmdata["name"] = "vm"
        self.update_NuageVspGlobalDomainTemplateName(name="")
        self.dhcp_options_map.update({1: {"dhcp:1": "255.255.255.0",
                                          "dhcp:2": "10",
                                          "dhcp:4": "10.0.0.2,"
                                                    "10.0.0.3,"
                                                    "10.0.0.4",
                                          "dhcp:7": "10.0.0.5,10.0.0.6",
                                          "dhcp:9": "10.0.0.7",
                                          "dhcp:13": "255",
                                          }})
        self.dhcp_options_map.update({16: {"dhcp:16": "10.0.0.8",
                                           "dhcp:17": "/tmp/",
                                           "dhcp:18": "/ext/",
                                           "dhcp:19": "1",
                                           "non-local-source-routing": "0",
                                           "policy-filter": "10.1.2.12,"
                                                            "255.255.255.0",
                                           "max-datagram-reassembly": "1000",
                                           "default-ttl": "255",
                                           "mtu": "1024",
                                           "all-subnets-local": "1"}})
        self.dhcp_options_map.update({28: {"broadcast": "10.1.2.255",
                                           "router-discovery": "0",
                                           "router-solicitation": "10.1.2.2",
                                           "static-route": "10.0.0.1,10.0.0.2",
                                           "trailer-encapsulation": "1",
                                           "arp-timeout": "255",
                                           "ethernet-encap": "1",
                                           "tcp-ttl": "255",
                                           "tcp-keepalive": "255",
                                           "nis-domain": "www.test.com"}})
        self.dhcp_options_map.update({41: {"nis-server": "10.1.1.1,10.1.1.2",
                                           "ntp-server": "10.1.1.3,10.1.1.4",
                                           "netbios-ns": "10.1.1.5,10.1.1.6",
                                           "netbios-dd": "10.1.1.8,10.1.1.7",
                                           "netbios-nodetype": "08",
                                           "netbios-scope": "test2",
                                           "x-windows-fs": "10.1.2.13,"
                                                           "10.1.2.16",
                                           "x-windows-dm": "10.1.2.14,"
                                                           "10.1.2.15",
                                           "requested-address": "10.1.2.16",
                                           "vendor-class": "01"}})
        self.dhcp_options_map.update({64: {"nis+-domain": "www.test3.com",
                                           "nis+-server": "10.1.2.255,"
                                                          "10.1.2.254",
                                           "tftp-server": "www.test4.com",
                                           "bootfile-name": "file.txt",
                                           "mobile-ip-home": "10.1.2.18,"
                                                             "10.1.2.19",
                                           "smtp-server": "10.1.2.20,"
                                                          "10.1.2.21",
                                           "pop3-server": "10.1.2.2,10.1.2.3",
                                           "nntp-server": "10.1.2.5,10.1.2.4",
                                           "irc-server": "10.1.2.1,10.1.2.4",
                                           "user-class": "user-class"}})
        self.dhcp_options_map.update({93: {"client-arch": "16",
                                           "client-interface-id": "01",
                                           "client-machine-id": "01",
                                           "dhcp:114":
                                               "http://www.testdhcpfeature."
                                               "com/adfsgbfgtdhh125ki-23-fdh"
                                               "-09",
                                           "domain-search": "www.domain.com",
                                           "sip-server": "www.sip.com",
                                           "classless-static-route":
                                               "10.1.0.0/16,10.1.0.1",
                                           "vendor-id-encap":
                                               "0000ACEF04CAFEBABE"
                                           }})
        self.dhcp_options_to_verify_map.update({1: {1: "255.255.255.0",
                                                2: "10",
                                                4: ["10.0.0.2",
                                                    "10.0.0.3",
                                                    "10.0.0.4"],
                                                7: ["10.0.0.5",
                                                    "10.0.0.6"],
                                                9: "10.0.0.7",
                                                13: "255"}})
        self.dhcp_options_to_verify_map.update({16: {16: "10.0.0.8",
                                                     17: "/tmp/",
                                                     18: "/ext/",
                                                     19: "1",
                                                     20: "0",
                                                     21: ["10.1.2.12",
                                                          "255.255.255.0"],
                                                     22: "1000",
                                                     23: "255",
                                                     26: "1024",
                                                     27: "1"}})
        self.dhcp_options_to_verify_map.update({28: {28: "10.1.2.255",
                                                     31: "0",
                                                     32: "10.1.2.2",
                                                     33: ["10.0.0.1",
                                                          "10.0.0.2"],
                                                     34: "1",
                                                     35: "255",
                                                     36: "1",
                                                     37: "255",
                                                     38: "255",
                                                     40: "www.test.com"}})
        self.dhcp_options_to_verify_map.update({41: {41: ["10.1.1.1",
                                                          "10.1.1.2"],
                                                     42: ["10.1.1.3",
                                                          "10.1.1.4"],
                                                     44: ["10.1.1.5",
                                                          "10.1.1.6"],
                                                     45: ["10.1.1.8",
                                                          "10.1.1.7"],
                                                     46: "H-node",
                                                     47: "test2",
                                                     48: ["10.1.2.13",
                                                          "10.1.2.16"],
                                                     49: ["10.1.2.14",
                                                          "10.1.2.15"],
                                                     50: "10.1.2.16",
                                                     60: "01"}})
        self.dhcp_options_to_verify_map.update({64: {64: "www.test3.com",
                                                     65: ["10.1.2.255",
                                                          "10.1.2.254"],
                                                     66: "www.test4.com",
                                                     67: "file.txt",
                                                     68: ["10.1.2.18",
                                                          "10.1.2.19"],
                                                     69: ["10.1.2.20",
                                                          "10.1.2.21"],
                                                     70: ["10.1.2.2",
                                                          "10.1.2.3"],
                                                     71: ["10.1.2.5",
                                                          "10.1.2.4"],
                                                     74: ["10.1.2.1",
                                                          "10.1.2.4"],
                                                     77: "user-class"}})
        self.dhcp_options_to_verify_map.update({93: {93: "16",
                                                     94: "01",
                                                     97: "01",
                                                     114: "http://www.testdhcp"
                                                          "feature.com/adfsgbf"
                                                          "gtdhh125ki-23-fdh-"
                                                          "09",
                                                     119: "www.domain.com",
                                                     120: "www.sip.com",
                                                     121: ["10.1.0.0/16",
                                                           "10.1.0.1"],
                                                     125: "0000acef04cafebabe"
                                                     }})
        self.expected_dhcp_options_on_vm.update({46: "netbios-node-type 8",
                                                 60: "vendor-class-identifier"
                                                     " 1",
                                                 94: "unknown-94 1",
                                                 93: "unknown-93",
                                                 97: "unknown-97 1",
                                                 119: "unknown-119",
                                                 120: "unknown-120",
                                                 121: "unknown-121",
                                                 125: "0:0:ac:ef:4:ca:fe:ba:be"
                                                 })
        self.cleanup = []
        return

    def tearDown(self):
        super(TestNuageExtraDhcp, self).tearDown()
        # Cleanup resources used
        self.debug("Cleaning up the resources")
        self.update_NuageVspGlobalDomainTemplateName(name="")
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

    def retrieve_dhcp_values_to_verify_on_vm_based_on(self, dhcp_map):
        vm_dhcp_map = copy.deepcopy(dhcp_map)
        for dhcpcode, dhcpval in self.expected_dhcp_options_on_vm.iteritems():
            if dhcpcode in dhcp_map:
                vm_dhcp_map[dhcpcode] = dhcpval

        return vm_dhcp_map

    def verify_vsd_dhcp_option_subnet(self, dhcp_type, value, subnet):
        self.debug("Verifying the creation and value of DHCP option type -"
                   " %s in VSD" % dhcp_type)
        found_dhcp_type = False
        dhcp_options = self.vsd.get_subnet_dhcpoptions(
            filter=self.get_externalID_filter(subnet.id))
        for dhcp_option in dhcp_options:
            self.debug("dhcptype option in vsd is : %s"
                       % dhcp_option.actual_type)
            self.debug("dhcptype expected value is: %s" % value)
            if dhcp_option.actual_type == dhcp_type:
                found_dhcp_type = True
                if isinstance(dhcp_option.actual_values, list):
                    self.debug("dhcptype actual value on vsd is %s:"
                               "" % dhcp_option.actual_values)
                    if value in dhcp_option.actual_values:
                        self.debug("Excepted DHCP option value found in"
                                   " VSD")
                    else:
                        self.fail("Excepted DHCP option value not found"
                                  " in VSD")
                else:
                    self.debug("dhcptype actual value on vsd is %s:"
                               % dhcp_option.actual_values)
                    self.assertEqual(dhcp_option.actual_values, value,
                                     "Expected DHCP option value is not same"
                                     " in both CloudStack and VSD")
        if not found_dhcp_type:
            self.fail("Expected DHCP option type and value not found"
                      " in the VSD")
        self.debug("Successfully verified the creation and value of DHCP"
                   " option type - %s in VSD" % dhcp_type)

    def verify_vsd_dhcp_option(self, dhcp_type, value, vm_interface):
        self.debug("Verifying the creation and value of DHCP option type -"
                   " %s in VSD" % dhcp_type)
        self.debug("Expected value for this dhcp option is - %s in VSD"
                   % value)
        found_dhcp_type = False
        dhcp_options = self.vsd.get_vm_interface_dhcpoptions(
            filter=self.get_externalID_filter(vm_interface.id))
        for dhcp_option in dhcp_options:
            self.debug("dhcptype on vsd is %s:" % dhcp_option.actual_type)
            self.debug("dhcp value on vsd is: %s:" % dhcp_option.actual_values)
            if dhcp_option.actual_type == dhcp_type:
                found_dhcp_type = True
                if isinstance(dhcp_option.actual_values, list):
                    self.debug("dhcptype actual value is %s:" %
                               dhcp_option.actual_values)
                    if type(value) is list:
                        for val in value:
                            self.is_value_in_options(dhcp_option.actual_values,
                                                     val)
                    else:
                        self.is_value_in_options(dhcp_option.actual_values,
                                                 value)
                else:
                    self.assertEqual(dhcp_option.actual_values, value,
                                     "Expected DHCP option value is not same"
                                     " in both CloudStack and VSD")
        if not found_dhcp_type:
            self.fail("Expected DHCP option type and value not found in "
                      "the VSD for dhcp type %s " % dhcp_type)
        self.debug("Successfully verified the creation and value of DHCP"
                   " option type - %s in VSD" % dhcp_type)

    def is_value_in_options(self, actual_options, value):
        if value in actual_options:
            self.debug("Excepted DHCP option value found in VSD")
        else:
            self.fail("Excepted DHCP option value not found in VSD")

    def verify_vsd_dhcp_option_empty(self, dhcp_type, vm_interface):
        self.debug("Verifying the creation and value of DHCP option"
                   " type - %s in VSD" % dhcp_type)
        self.debug("Expected value is empty string")
        dhcp_options = self.vsd.get_vm_interface_dhcpoptions(
            filter=self.get_externalID_filter(vm_interface.id))
        for dhcp_option in dhcp_options:
            self.debug("dhcptype on vsd is %s:" % dhcp_option.actual_type)
            self.debug("dhcp value on vsd is: %s:" % dhcp_option.value)
            if dhcp_option.actual_type == dhcp_type:
                if dhcp_type == 15:
                    self.assertEqual(dhcp_option.value, "\x00",
                                     "Expected DHCP option value is not"
                                     " same in both CloudStack and VSD")
                else:
                    self.assertEqual(dhcp_option.value, "00",
                                     "Expected DHCP option value is not"
                                     " same in both CloudStack and VSD")
        self.debug("Successfully verified the creation and value of"
                   " DHCP option type - %s in VSD" % dhcp_type)

    def verify_vsd_dhcp_value_notpresent(self, value, vm_interface):
        self.debug("Verifying that on vminterface value is not present- %s"
                   % value)
        dhcp_options = self.vsd.get_vm_interface_dhcpoptions(
            filter=self.get_externalID_filter(vm_interface.id))
        for dhcp_option in dhcp_options:
            self.debug("dhcptype option is %s:" % dhcp_option.actual_type)
            if isinstance(dhcp_option.actual_values, list):
                self.debug("dhcptype actual value is %s:"
                           % dhcp_option.actual_values)
                if value in dhcp_option.actual_values:
                    self.fail("This value is not expected on vminterface but "
                              "present as dhcp_type %s"
                              % dhcp_option.actual_type)
                else:
                    self.debug("As Excepted DHCP value not found in VSD")
            else:
                try:
                    self.assertEqual(dhcp_option.actual_values, value,
                                     "Expected DHCP option value is not same "
                                     "in both CloudStack and VSD")
                    self.fail("This value is not expected on vm interface but "
                              "present as dhcp_type %s"
                              % dhcp_option.actual_type)
                except Exception:
                    self.debug("As Expected DHCP value not found in VSD")
        self.debug("Successfully verified dhcp value is not present - %s "
                   "in VSD" % value)

    def verify_vsd_dhcp_type_notpresent(self, dhcp_types, vm_interface):
        if type(dhcp_types) is not list:
            dhcp_types = [dhcp_types]

        for dhcp_type in dhcp_types:
            self.debug("Verifying that DHCP option type - %s not present"
                       " in VSD" % dhcp_type)
            dhcp_options = self.vsd.get_vm_interface_dhcpoptions(
                filter=self.get_externalID_filter(vm_interface.id))
            for dhcp_option in dhcp_options:
                self.debug("dhcptype on vsd is %s:" % dhcp_option.actual_type)
                if dhcp_option.actual_type == dhcp_type:
                    self.fail("Expected DHCP option type is not expected in "
                              "the VSD: %s" % dhcp_type)
            self.debug("Successfully verified DHCP option type - %s "
                       "not present in the VSD" % dhcp_type)

    def verify_dhcp_on_vm(
            self, dhcpleasefile, dhcp_option_map, ssh_client, cleanlease=True):
        cmd = 'cat /var/lib/dhclient/'+dhcpleasefile
        self.debug("get content of dhcp lease file " + cmd)
        outputlist = ssh_client.execute(cmd)
        self.debug("command is executed properly " + cmd)
        completeoutput = str(outputlist).strip('[]')
        self.debug("complete output is " + completeoutput)

        for key, value in dhcp_option_map.iteritems():
            if type(value) is list:
                for val in value:
                    self.check_if_value_contains(completeoutput, val)
            else:
                self.check_if_value_contains(completeoutput, value)
        if cleanlease:
            self.remove_lease_file(ssh_client, dhcpleasefile)

    def check_if_value_contains(self, completeoutput, value):
        if value in completeoutput:
            self.debug("excepted value found in vm: " + value)
        else:
            self.fail("excepted value not found in vm: " + value)

    def remove_lease_file(self, ssh_client, dhcpleasefile):
        cmd = 'rm -rf /var/lib/dhclient/'+dhcpleasefile
        outputlist = ssh_client.execute(cmd)
        completeoutput = str(outputlist).strip('[]')
        self.debug("clear lease is done properly:" + completeoutput)

    def update_zone_details(self, value):
        """Updates the VM data"""
        # update Network Domain at zone level
        cmd = updateZone.updateZoneCmd()
        cmd.id = self.zone.id
        cmd.domain = value
        self.api_client.updateZone(cmd)

    def update_NuageVspGlobalDomainTemplateName(self, name):
        self.debug("Updating global setting nuagevsp.vpc.domaintemplate.name "
                   "with value - %s" % name)
        Configurations.update(self.api_client,
                              name="nuagevsp.vpc.domaintemplate.name",
                              value=name)
        self.debug("Successfully updated global setting "
                   "nuagevsp.vpc.domaintemplate.name with value - %s" % name)

    def create_isolated_network(
            self, network_offering=None, gateway="10.1.1.1",
            netmask="255.255.255.0"):
        # create a isolated network
        self.debug("Creating an Isolated network...")
        if not network_offering:
            network_offering = self.create_isolated_network_offering()

        network = self.create_Network(network_offering, gateway, netmask)

        return network

    def validate_isolated_network(
            self, network_offering, network):
        self.debug("Validating network...")
        self.validate_NetworkOffering(network_offering, state="Enabled")
        self.validate_Network(network)

    def validate_vpc(self, vpc, vpc_offering):
        self.debug("Validating vpc...")
        self.validate_Vpc(vpc)
        self.validate_VpcOffering(vpc_offering)

    def verify_dhcp_options_on_vm(
            self, vm, network, vpc, dhcp_options, remove_lease_file=True,
            lease_file="dhclient-eth0.leases", verify_on_vsd=True):
        # Adding Ingress Firewall/Network ACL rule
        self.debug("Adding Ingress Firewall/Network ACL rule to make the "
                   "created Static NAT rule (wget) accessible...")
        public_ip = self.acquire_PublicIPAddress(network, vpc=vpc)
        self.create_StaticNatRule_For_VM(
                vm, public_ip, network)

        if vpc:
            public_http_rule = self.create_NetworkAclRule(
                self.test_data["ingress_rule"], network=network)
        else:
            public_http_rule = self.create_FirewallRule(public_ip)

        # VSD verification
        if verify_on_vsd:
            self.verify_vsd_firewall_rule(public_http_rule)

        ssh_client = self.ssh_into_VM(vm, public_ip)
        dhcp_options_to_verify_on_vm =\
            self.retrieve_dhcp_values_to_verify_on_vm_based_on(
                dhcp_options)
        self.verify_dhcp_on_vm(
            lease_file, dhcp_options_to_verify_on_vm,
            ssh_client, remove_lease_file)

        # Removing Ingress Firewall/Network ACL rule
        self.debug("Removing the created Ingress Firewall/Network ACL "
                   "rule in the network...")
        public_http_rule.delete(self.api_client)

        public_ip.delete(self.api_client)
        with self.assertRaises(Exception):
            self.validate_PublicIPAddress(public_ip, network)

    def verify_dhcp_options_on_vsd(
            self, vm, dhcp_options,
            networks_with_options=None,
            verify_vm_on_vsd=True):

        if verify_vm_on_vsd:
            self.verify_vsd_vm(vm)

        if networks_with_options:
            for nic in vm.nic:
                if self.is_nic_in_network_list(nic, networks_with_options):
                    for key, value in dhcp_options.iteritems():
                        self.verify_vsd_dhcp_option(key, value, nic)
        else:
            for nic in vm.nic:
                for key, value in dhcp_options.iteritems():
                    self.verify_vsd_dhcp_option(key, value, nic)

    @staticmethod
    def is_nic_in_network_list(nic, network_list):
        if type(network_list) is list:
            for network in network_list:
                if network.id == nic.networkid:
                    return True
            return False
        elif network_list.id == nic.networkid:
            return True
        return False

    def validate_network_on_vsd_based_on_networktype(
            self, network, vpc=None, is_shared_network=False):
        if is_shared_network:
            self.verify_vsd_shared_network(
                self.domain.id, network,
                gateway=self.test_data["nuagevsp"]["network_all"]["gateway"])
        else:
            self.verify_vsd_network(self.domain.id, network, vpc)

    def create_vpc_offering_with_nuage_dhcp(self):
        # Creating a VPC offering
        self.debug("Creating Nuage VSP VPC offering without dhcp...")
        vpc_offering = self.create_VpcOffering(
            self.test_data["nuagevsp"]["vpc_offering_nuage_dhcp"])
        self.validate_VpcOffering(vpc_offering, state="Enabled")

        return vpc_offering

    def create_isolated_network_offering(self):
        network_offering = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["isolated_network_offering"])
        self.validate_NetworkOffering(network_offering, state="Enabled")
        return network_offering

    def create_vpc_network_offering(self):
        network_offering = self.create_NetworkOffering(
            self.test_data["nuagevsp"]["vpc_network_offering_nuage_dhcp"])
        self.validate_NetworkOffering(network_offering, state="Enabled")
        return network_offering

    def create_vpc(self, vpc_offering, cidr="10.0.0.0/16"):
        # Creating a VPC
        self.debug("Creating a VPC with Nuage VSP VPC offering...")
        vpc = self.create_Vpc(vpc_offering, cidr=cidr,
                              networkDomain="testvpc.com")
        self.validate_Vpc(vpc, state="Enabled")

        return vpc

    def create_vpc_with_tier(self, domain_name="testvpc.com"):
        vpc_offering = self.create_vpc_offering_with_nuage_dhcp()
        vpc = self.create_vpc(vpc_offering)

        vpc_network_offering = self.create_vpc_network_offering()
        acl_list = self.create_acl_list_with_item(vpc)
        vpc_first_tier = \
            self.when_i_create_a_first_vpc_network_with_nuage_dhcp(
                vpc, vpc_network_offering, acl_list)

        self.verify_vsd_dhcp_option_subnet(15, domain_name, vpc_first_tier)

        return {"vpc": vpc, "tier": vpc_first_tier}

    def create_acl_list_with_item(self, vpc):
        # Creating an ACL list
        acl_list = self.create_NetworkAclList(name="acl", description="acl",
                                              vpc=vpc)

        # Creating an ACL item
        self.create_NetworkAclRule(self.test_data["ingress_rule"],
                                   acl_list=acl_list)

        return acl_list

    @staticmethod
    def add_extra_dhcp_options_to_check(dhcp_options_to_verify, domain_name,
                                        remove_dns_options=False):
        if not remove_dns_options:
            dhcp_options_to_verify[12] = "vm1"
            dhcp_options_to_verify[15] = domain_name
        return dhcp_options_to_verify

    def get_extra_dhcp_options_starting_with(self, dhcp_option_code,
                                             network=None):
        dhcp_options =\
            copy.deepcopy(self.dhcp_options_map.get(dhcp_option_code))
        if network:
            dhcp_options["networkid"] = network.id
        return dhcp_options

    def get_extra_dhcp_options_to_verify_starting_with(
            self, number, domain_name, remove_dns_options=False):
        dhcp_options_to_verify = copy.deepcopy(
            self.dhcp_options_to_verify_map.get(number))
        self.add_extra_dhcp_options_to_check(
            dhcp_options_to_verify, domain_name, remove_dns_options)
        return dhcp_options_to_verify

    @gherkin
    def when_i_update_the_zone_details_and_restart_a_vpc(self, vpc):
        self.update_zone_details("testvpc.com")
        vpc.restart(self.api_client)

    @gherkin
    def when_i_create_a_first_vpc_network_with_nuage_dhcp(
            self, vpc, network_offering, acl_list, gateway="10.0.0.1"):
        # Creating a VPC network in the VPC
        self.debug(
            "Creating a VPC network with Nuage VSP VPC Network offering...")
        vpc_network = self.create_Network(network_offering, gateway=gateway,
                                          vpc=vpc, acl_list=acl_list)
        self.validate_Network(vpc_network, state="Implemented")

        return vpc_network

    @gherkin
    def when_i_create_a_second_vpc_network_with_nuage_dhcp(
            self, vpc, network_offering, acl_list):
        vpc_network_1 = self.create_Network(
            network_offering, gateway='10.1.2.1', vpc=vpc, acl_list=acl_list)
        self.validate_Network(vpc_network_1, state="Implemented")

        return vpc_network_1

    @gherkin
    def when_i_stop_and_start_a_vm(self, vm):
        vm.stop(self.api_client)
        vm.start(self.api_client)

    @gherkin
    def when_i_add_an_extra_nic_to_a_vm(self, vm, network, dhcp_options=None):
        dhcp_options_list = None

        if dhcp_options:
            if type(dhcp_options) is list:
                dhcp_options_list = []
                for item in dhcp_options:
                    dhcp_options_list.extend([item])
            else:
                dhcp_options_list = [dhcp_options]

        return vm.add_nic(self.api_client, network.id,
                          dhcpoptions=dhcp_options_list)

    @gherkin
    def when_i_restart_a_network(self, network):
        network.restart(self.api_client, cleanup=True)

    @gherkin
    def when_i_create_a_vm(
            self, network, vpc, vm_name, dhcp_options,
            start_vm=True, is_shared_network=False,
            ip_address=None):
        vm_data = copy.deepcopy(self.vmdata)
        if dhcp_options:
            if type(dhcp_options) is list:
                dhcp_options_list = []
                for item in dhcp_options:
                    dhcp_options_list.extend([item])
            else:
                dhcp_options_list = [dhcp_options]

            vm_data["dhcpoptionsnetworklist"] = dhcp_options_list
        elif "dhcpoptionsnetworklist" in vm_data:
            del vm_data["dhcpoptionsnetworklist"]

        if ip_address:
            vm_data["ipaddress"] = ip_address
        if vm_name:
            vm_data["displayname"] = vm_name
            vm_data["name"] = vm_name

        vm = self.create_VM(network, start_vm=start_vm, testdata=vm_data)
        if start_vm:
            self.check_VM_state(vm, state="Running")
        else:
            self.check_VM_state(vm, state="Stopped")
        # VSD verification
        if type(network) is not list:
            self.validate_network_on_vsd_based_on_networktype(
                network, vpc, is_shared_network)

        return vm

    @gherkin
    def when_i_update_extra_dhcp_options_on_a_vm(self, vm, dhcp_options):
        """Updates the VM data"""
        if type(dhcp_options) is list:
            dhcp_options_list = []
            for item in dhcp_options:
                dhcp_options_list.extend([item])
        else:
            dhcp_options_list = [dhcp_options]

        cmd = updateVirtualMachine.updateVirtualMachineCmd()
        cmd.id = vm.id
        cmd.dhcpoptionsnetworklist = dhcp_options_list
        self.api_client.updateVirtualMachine(cmd)

    @gherkin
    def then_verify_domain_name_and_router_options_multi_nic_set(
            self, multinic_vm, primary_network, domain_name="testvpc.com"):
        for nic in multinic_vm.nic:
            if nic.networkid != primary_network.id:
                self.verify_vsd_dhcp_option(3, "0.0.0.0", nic)
                self.verify_vsd_dhcp_option(15, "\x00", nic)
            else:
                self.verify_vsd_dhcp_option(15, domain_name, nic)

    @gherkin
    def then_verify_dhcp_options_on_vsd_and_vm(self, vm, network,
                                               dhcp_options_to_verify,
                                               network_with_options=None,
                                               is_shared_network=False,
                                               verify_on_vm=False,
                                               default_network=None,
                                               vpc=None,
                                               remove_lease_file=False,
                                               verify_on_vsd=True):

        if verify_on_vsd:
            self.verify_dhcp_options_on_vsd(
                vm, dhcp_options_to_verify, network_with_options,
                not is_shared_network)
        if verify_on_vm and not self.isSimulator and not is_shared_network:
            if default_network:
                network = default_network
                lease_file = "dhclient-eth1.leases"
            else:
                lease_file = "dhclient-eth0.leases"

            self.verify_dhcp_options_on_vm(
                vm=vm,
                network=network,
                vpc=vpc,
                dhcp_options=dhcp_options_to_verify,
                lease_file=lease_file,
                remove_lease_file=remove_lease_file,
                verify_on_vsd=verify_on_vsd)

    @gherkin
    def then_no_dhcp_options_present_on_vsd(self, dhcp_options_map, vm,
                                            excluded_nics=None):
        for nic in vm.nic:
            if not excluded_nics or nic not in excluded_nics:
                self.verify_vsd_dhcp_type_notpresent(
                    dhcp_options_map.keys(), nic)
                self.verify_vsd_dhcp_value_notpresent(
                    dhcp_options_map.values(), nic)

    def validate_all_extra_dhcp_for_add_remove_nic_after_migrate(
            self, network, domain_name="testisolated.com",
            is_shared_network=False, verify_all_options=False):
        # 1 - create an extra isolated network
        # 2 - for each extra dhc option:
            # a - deploy a vm
            # b - migrate vm
            # c - plug nic
            # d - verify the dhcp options are correctly set on the nic
            # e - remove nic
            # f - verify the dhcp options are no longer present on the vsd
            # and vm
        # 3 - try to remove the default nic which has extra dhcp options set
        # (this should fail)

        isolated_network2 =\
            self.create_isolated_network(gateway="10.0.1.1")

        if verify_all_options:
            options_to_verify = self.dhcp_options_map_keys
        else:
            options_to_verify = [1]

        for number in options_to_verify:
            vm1 = self.when_i_create_a_vm(
                isolated_network2, None, "vm1",
                dhcp_options=None,
                is_shared_network=False)

            if not self.isSimulator:
                self.migrate_VM(vm1)

            result = self.when_i_add_an_extra_nic_to_a_vm(vm1, network, None)
            dhcp_options_network = self.get_extra_dhcp_options_starting_with(
                number, network)
            self.when_i_update_extra_dhcp_options_on_a_vm(
                vm1, dhcp_options_network)
            self.when_i_stop_and_start_a_vm(vm1)
            dhcp_options_to_verify =\
                self.get_extra_dhcp_options_to_verify_starting_with(
                    number, domain_name, remove_dns_options=True)

            self.then_verify_dhcp_options_on_vsd_and_vm(
                vm1, network, dhcp_options_to_verify,
                network_with_options=network,
                is_shared_network=is_shared_network,
                verify_on_vm=True,
                default_network=isolated_network2,
                vpc=None)

            vm1.remove_nic(
                self.api_client,
                [nic for nic in result.nic
                 if nic.networkid == network.id][0].id)

            dhcp_options_to_verify =\
                self.get_extra_dhcp_options_to_verify_starting_with(
                    number, domain_name, remove_dns_options=True)
            self.then_no_dhcp_options_present_on_vsd(dhcp_options_to_verify,
                                                     vm1)
            self.delete_VM(vm1)

    def validate_vm_deploy_concurrency(
            self, network,
            vpc=None,
            domain_name="testisolated.com",
            is_shared_network=False):

        old_dhcp_options =\
            self.get_extra_dhcp_options_starting_with(1, network)
        old_dhcp_options_to_verify =\
            self.get_extra_dhcp_options_to_verify_starting_with(
                1, domain_name, True)

        new_dhcp_options =\
            self.get_extra_dhcp_options_starting_with(16, network)
        new_dhcp_options_to_verify =\
            self.get_extra_dhcp_options_to_verify_starting_with(
                16, domain_name, True)

        def deploy_update_validate_vm(number):
            vm = self.when_i_create_a_vm(
                [network], vpc, "vm-%02d" % number,
                old_dhcp_options,
                is_shared_network=is_shared_network)

            self.then_verify_dhcp_options_on_vsd_and_vm(
                vm, network, old_dhcp_options_to_verify,
                is_shared_network=is_shared_network,
                verify_on_vm=True,
                vpc=vpc,
                verify_on_vsd=False)

            self.when_i_update_extra_dhcp_options_on_a_vm(vm, new_dhcp_options)
            self.when_i_stop_and_start_a_vm(vm)

            self.then_verify_dhcp_options_on_vsd_and_vm(
                vm, network, new_dhcp_options_to_verify,
                is_shared_network=is_shared_network,
                verify_on_vm=True,
                vpc=vpc,
                verify_on_vsd=False)

            self.delete_VM(vm)

        try:
            executor = ThreadPoolExecutor(max_workers=10)

            vm_futures = [executor.submit(
                deploy_update_validate_vm, i)
                    for i in range(10)]

            wait(vm_futures)

            [f.result()
             for f in vm_futures]

        finally:
            executor.shutdown(wait=True)

    def validate_all_extra_dhcp_for_network_actions_in_network(
            self, network,
            vpc=None,
            domain_name="testisolated.com",
            is_shared_network=False,
            verify_all_options=False):
        # 1 - for each extra dhcp option:
            # a - deploy a vm with dhcp options
            # b - restart the network
            # c - check if the extra dhcp options are still correct
            # d - restart the network with clean up = false
            # e - check if the extra dhcp options are still correct
            # f - if the network is a vpc, restart the vpc
            # g - check if the extra dhcp options are still correct
            # h - delete the vm
            # i - create a vm
            # j - stop the vm
            # k - start the vm in a seperate thread
            # l - add an extra nic while the vn is still in starting state
            # m - delete the the vm
        # 2 - deploy a vm
        # 3 - wait for the network to go into allocated state
        # 4 - deploy a new vm in the network
        # 5 - check if all options are set correctly
        # 6 - delete the network

        if verify_all_options:
            options_to_verify = self.dhcp_options_map_keys
        else:
            options_to_verify = [1]

        for number in options_to_verify:
            dhcp_options_network =\
                self.get_extra_dhcp_options_starting_with(number, network)
            vm1 = self.when_i_create_a_vm(
                network, vpc, "vm1", dhcp_options_network,
                is_shared_network=is_shared_network)
            dhcp_options_to_verify =\
                self.get_extra_dhcp_options_to_verify_starting_with(
                    number, domain_name, is_shared_network)
            self.then_verify_dhcp_options_on_vsd_and_vm(
                vm1, network, dhcp_options_to_verify,
                is_shared_network=is_shared_network, verify_on_vm=True,
                vpc=vpc, remove_lease_file=False)

            network.restart(self.api_client, True)
            self.then_verify_dhcp_options_on_vsd_and_vm(
                vm1, network, dhcp_options_to_verify,
                is_shared_network=is_shared_network, verify_on_vm=True,
                vpc=vpc, remove_lease_file=False)

            network.restart(self.api_client, False)
            self.then_verify_dhcp_options_on_vsd_and_vm(
                vm1, network, dhcp_options_to_verify,
                is_shared_network=is_shared_network, verify_on_vm=True,
                vpc=vpc, remove_lease_file=False)

            if vpc:
                self.restart_Vpc(vpc)
                self.then_verify_dhcp_options_on_vsd_and_vm(
                    vm1, network, dhcp_options_to_verify,
                    is_shared_network=is_shared_network, verify_on_vm=True,
                    vpc=vpc, remove_lease_file=False)

                self.restart_Vpc(vpc, True)
                self.then_verify_dhcp_options_on_vsd_and_vm(
                    vm1, network, dhcp_options_to_verify,
                    is_shared_network=is_shared_network, verify_on_vm=True,
                    vpc=vpc)

            self.delete_VM(vm1)

            dhcp_options_network = \
                self.get_extra_dhcp_options_starting_with(number, network)

            vm2 = self.when_i_create_a_vm(
                network, vpc, "vm2", dhcp_options_network,
                is_shared_network=is_shared_network)

            isolated_network2 =\
                self.create_isolated_network(gateway="10.0.1.1")

            dhcp_options_network =\
                self.get_extra_dhcp_options_starting_with(
                    number, None)
            vm_nic = self.when_i_add_an_extra_nic_to_a_vm(
                vm2, isolated_network2, dhcp_options=dhcp_options_network)

            dhcp_options_to_verify = \
                self.get_extra_dhcp_options_to_verify_starting_with(
                    number, domain_name, remove_dns_options=True)
            self.then_verify_dhcp_options_on_vsd_and_vm(
                vm2, isolated_network2, dhcp_options_to_verify,
                is_shared_network=is_shared_network, verify_on_vm=False)

            if not is_shared_network:
                self.then_verify_domain_name_and_router_options_multi_nic_set(
                    vm2, network, domain_name)

            vm2.update_default_nic(
                self.api_client,
                [nic
                 for nic in vm_nic.nic
                    if not nic.isdefault][0].id)
            self.when_i_stop_and_start_a_vm(vm2)

            if not is_shared_network:
                self.then_verify_domain_name_and_router_options_multi_nic_set(
                    vm2, isolated_network2, domain_name)

            self.then_verify_dhcp_options_on_vsd_and_vm(
                vm2, isolated_network2, dhcp_options_to_verify,
                is_shared_network=is_shared_network, verify_on_vm=True)
            self.delete_VM(vm2)

    def validate_all_extra_dhcp_for_network_in_allocated(
            self, network,
            vpc=None,
            domain_name="testisolated.com",
            is_shared_network=False):
        dhcp_options_network =\
            self.get_extra_dhcp_options_starting_with(1, network)
        vm3 = self.when_i_create_a_vm(
            network, vpc, "vm3", dhcp_options_network,
            is_shared_network=is_shared_network)
        vm3.stop(self.api_client)
        # wait 1 min for network to go into allocated state
        time.sleep(60)
        dhcp_options_network =\
            self.get_extra_dhcp_options_starting_with(64, network)
        vm4 = self.when_i_create_a_vm(
            network, vpc, "vm1", dhcp_options_network,
            is_shared_network=is_shared_network)
        dhcp_options_to_verify = \
            self.get_extra_dhcp_options_to_verify_starting_with(
                64, domain_name, is_shared_network)
        self.then_verify_dhcp_options_on_vsd_and_vm(
            vm4, network, dhcp_options_to_verify,
            is_shared_network=is_shared_network, verify_on_vm=True, vpc=vpc)

        self.delete_VM(vm4)
        self.delete_VM(vm3)
        self.delete_Network(network)
        if vpc:
            vpc.delete(self.api_client)

    def validate_all_extra_dhcp_for_vm_actions_in_network(
            self, network,
            vpc=None,
            domain_name="testisolated.com",
            is_shared_network=False,
            verify_all_options=False):
        # 1 - for each extra dhcp options:
            # a - create a vm with dhcp options
            # b - start and stop the vm
            # c - check if the dhcp options are set correctly
            # d - reboot the vm
            # e - check if the dhcp options are set correctly
            # f - delete a vm without expunging it
            # g - recover the vm
            # h - start the vm
            # i - check if the dhcp options are set correctly
            # j - delete the vm
        # 2 - create a vm with extra dhcp options set
        # 3 - check if the dhcp options are set correctly
        # 4 - update the vm with new extra dhcp options
        # 5 - reboot the vm
        # 6 - verify the dhcp options on the vm and the vsd are not updated
        # 7 - delete the vm

        if verify_all_options:
            options_to_verify = self.dhcp_options_map_keys
        else:
            options_to_verify = [1]

        for number in options_to_verify:
            dhcp_options_network =\
                self.get_extra_dhcp_options_starting_with(number, network)
            vm1 = self.when_i_create_a_vm(
                network, vpc, "vm1", dhcp_options_network,
                is_shared_network=is_shared_network)

            dhcp_options_to_verify =\
                self.get_extra_dhcp_options_to_verify_starting_with(
                    number, domain_name, is_shared_network)
            self.then_verify_dhcp_options_on_vsd_and_vm(
                vm1, network, dhcp_options_to_verify,
                is_shared_network=is_shared_network,
                verify_on_vm=True,
                vpc=vpc)

            self.when_i_stop_and_start_a_vm(vm1)
            self.then_verify_dhcp_options_on_vsd_and_vm(
                vm1, network, dhcp_options_to_verify,
                is_shared_network=is_shared_network,
                verify_on_vm=True,
                vpc=vpc)

            vm1.reboot(self.api_client)
            self.then_verify_dhcp_options_on_vsd_and_vm(
                vm1, network, dhcp_options_to_verify,
                is_shared_network=is_shared_network,
                verify_on_vm=True,
                vpc=vpc)

            vm1.delete(self.api_client, False)
            vm1.recover(self.api_client)
            vm1.start(self.api_client)
            self.then_verify_dhcp_options_on_vsd_and_vm(
                vm1, network, dhcp_options_to_verify,
                is_shared_network=is_shared_network,
                verify_on_vm=True,
                vpc=vpc)

            vm1.restore(self.api_client)
            self.then_verify_dhcp_options_on_vsd_and_vm(
                vm1, network, dhcp_options_to_verify,
                is_shared_network=is_shared_network,
                verify_on_vm=True,
                vpc=vpc)

            if not self.isSimulator:
                self.migrate_VM(vm1)
                self.then_verify_dhcp_options_on_vsd_and_vm(
                    vm1, network, dhcp_options_to_verify,
                    is_shared_network=is_shared_network, verify_on_vm=True,
                    vpc=vpc)

            vm1.delete(self.api_client, True)

        dhcp_options_network = self.get_extra_dhcp_options_starting_with(
            1, network)
        vm1 = self.when_i_create_a_vm(
            network, vpc, "vm1", dhcp_options_network,
            is_shared_network=is_shared_network)
        dhcp_options_to_verify =\
            self.get_extra_dhcp_options_to_verify_starting_with(
                1, domain_name, is_shared_network)
        self.then_verify_dhcp_options_on_vsd_and_vm(
            vm1, network, dhcp_options_to_verify,
            is_shared_network=is_shared_network,
            verify_on_vm=True,
            vpc=vpc)

        dhcp_options_network_not_present =\
            self.get_extra_dhcp_options_starting_with(93, network)
        dhcp_options_to_verify_network_not_present =\
            self.get_extra_dhcp_options_to_verify_starting_with(
                93, domain_name, True)
        self.when_i_update_extra_dhcp_options_on_a_vm(
            vm1, dhcp_options_network_not_present)
        vm1.reboot(self.api_client)

        self.then_verify_dhcp_options_on_vsd_and_vm(
            vm1, network, dhcp_options_to_verify,
            is_shared_network=is_shared_network,
            verify_on_vm=True,
            vpc=vpc)
        self.then_no_dhcp_options_present_on_vsd(
            dhcp_options_to_verify_network_not_present, vm1)

        if not self.isSimulator:
            self.migrate_VM(vm1)
            self.then_verify_dhcp_options_on_vsd_and_vm(
                vm1, network, dhcp_options_to_verify,
                is_shared_network=is_shared_network,
                verify_on_vm=True,
                vpc=vpc)

        dhcp_options_network = self.get_extra_dhcp_options_starting_with(
            64, network)
        dhcp_options_to_verify = \
            self.get_extra_dhcp_options_to_verify_starting_with(
                64, domain_name, is_shared_network)
        self.when_i_update_extra_dhcp_options_on_a_vm(
            vm1, dhcp_options_network)
        vm1.restore(self.api_client)
        self.then_verify_dhcp_options_on_vsd_and_vm(
            vm1, network, dhcp_options_to_verify,
            is_shared_network=is_shared_network,
            verify_on_vm=True,
            vpc=vpc)

        self.delete_VM(vm1)

    def validate_all_extra_dhcp_for_remove_nic_from_vm(
            self, network,
            vpc=None,
            domain_name="testisolated.com",
            is_shared_network=False,
            verify_all_options=False):
        # 1 - create an extra isolated network
        # 2 - for each extra dhc option:
            # a - deploy a vm
            # b - plug nic
            # c - verify the dhcp options are correctly set on the nic
            # d - remove nic
            # e - verify the dhcp options are no longer present on the vsd
            # and vm
        # 3 - try to remove the default nic which has extra dhcp options set
        # (this should fail)

        isolated_network2 =\
            self.create_isolated_network(gateway="10.0.1.1")

        if verify_all_options:
            options_to_verify = self.dhcp_options_map_keys
        else:
            options_to_verify = [1]

        for number in options_to_verify:
            vm1 = self.when_i_create_a_vm(
                isolated_network2, None, "vm1", dhcp_options=None, is_shared_network=False)
            result = self.when_i_add_an_extra_nic_to_a_vm(vm1, network, None)
            dhcp_options_network = self.get_extra_dhcp_options_starting_with(
                number, network)
            self.when_i_update_extra_dhcp_options_on_a_vm(
                vm1, dhcp_options_network)
            self.when_i_stop_and_start_a_vm(vm1)
            dhcp_options_to_verify =\
                self.get_extra_dhcp_options_to_verify_starting_with(
                    number, domain_name, True)
            self.then_verify_dhcp_options_on_vsd_and_vm(
                vm1, network, dhcp_options_to_verify=dhcp_options_to_verify,
                network_with_options=network,
                is_shared_network=is_shared_network, verify_on_vm=True,
                default_network=isolated_network2, vpc=None)
            vm1.remove_nic(
                self.api_client,
                [nic for nic in result.nic
                 if nic.networkid == network.id][0].id)

            dhcp_options_to_verify =\
                self.get_extra_dhcp_options_to_verify_starting_with(
                    number, domain_name, True)
            self.then_no_dhcp_options_present_on_vsd(dhcp_options_to_verify,
                                                     vm1)
            self.delete_VM(vm1)

        # invalid remove option
        vm1 = self.when_i_create_a_vm(
            network, vpc, "vm1", None, is_shared_network=is_shared_network)
        result = self.when_i_add_an_extra_nic_to_a_vm(
            vm1, isolated_network2, None)
        self.when_i_update_extra_dhcp_options_on_a_vm(
            vm1, dhcp_options_network)
        self.when_i_stop_and_start_a_vm(vm1)
        with self.assertRaises(Exception):
            vm1.remove_nic(
                self.api_client, [nic for nic in result.nic
                                  if nic.networkid == network.id][0])

    def validate_all_extra_dhcp_for_update_multinic(
            self, network,
            vpc=None,
            domain_name="testisolated.com",
            is_shared_network=False,
            verify_all_options=False):
        # 1 - create an extra isolated network
        # 2 - for each extra dhcp option:
            # a - deploy a vm and ad an extra nic
            # b - update the dhcp options on both nics
            # c - verify the dhcp options are not yet set on the vsd and vm
            # d - start and stop the vm
            # e - verify the new dhcp options are set on the vsd and vm
        # 3 - try to update a multi nic vm with invalid options
        # (this should fail)

        isolated_network2 =\
            self.create_isolated_network(gateway="10.0.1.1")

        if verify_all_options:
            options_to_verify = self.dhcp_options_map_keys
        else:
            options_to_verify = [1]

        for number in options_to_verify:
            vm1 = self.when_i_create_a_vm(
                isolated_network2, None, "vm1",
                dhcp_options=None,
                is_shared_network=False)
            self.when_i_add_an_extra_nic_to_a_vm(vm1, network, None)

            dhcp_options_network = self.get_extra_dhcp_options_starting_with(
                number, network)
            dhcp_options_network2 = self.get_extra_dhcp_options_starting_with(
                number, isolated_network2)
            dhcp_options_list = [dhcp_options_network, dhcp_options_network2]
            dhcp_options_to_verify =\
                self.get_extra_dhcp_options_to_verify_starting_with(
                    number, domain_name, True)
            self.when_i_update_extra_dhcp_options_on_a_vm(vm1,
                                                          dhcp_options_list)
            self.then_no_dhcp_options_present_on_vsd(dhcp_options_to_verify,
                                                     vm1)

            self.when_i_stop_and_start_a_vm(vm1)
            dhcp_options_to_verify =\
                self.get_extra_dhcp_options_to_verify_starting_with(
                    number, domain_name, True)
            self.then_verify_dhcp_options_on_vsd_and_vm(
                vm1, network, dhcp_options_to_verify=dhcp_options_to_verify,
                is_shared_network=is_shared_network, verify_on_vm=True,
                default_network=isolated_network2, vpc=None)
            self.delete_VM(vm1)

        invalid_dhcp_options_list = [{"networkid": network.id,
                                      "dhcp:":
                                          "http://www.testdhcpfeature.com/"
                                          "adfsgbfgtdhh125ki-23-fdh-09"},
                                     {"networkid": network.id,
                                      "dhcp:241":
                                          "http://www.testdhcpfeature.com/"
                                          "adfsgbfgtdhh125ki-23-fdh-09"},
                                     {"networkid": network.id,
                                      "unknownvalue":
                                          "http://www.testdhcpfeature.com/"
                                          "adfsgbfgtdhh125ki-23-fdh-09"},
                                     {"networkid": "invalidnetworkid",
                                      "dhcp:114":
                                          "http://www.testdhcpfeature.com/"
                                          "adfsgbfgtdhh125ki-23-fdh-09"}]
        valid_dhcp_option = {"networkid": isolated_network2.id,
                             "dhcp:124":
                                 "http://www.testdhcpfeature.com/"
                                 "adfsgbfgtdhh125ki-23-fdh-09"}
        for invalid_dhcp_option in invalid_dhcp_options_list:
            vm1 = self.when_i_create_a_vm(
                isolated_network2, vpc, "vm1",
                dhcp_options=None,
                is_shared_network=False)
            dhcp_options_to_verify =\
                self.get_extra_dhcp_options_to_verify_starting_with(
                    93, domain_name, True)
            self.then_no_dhcp_options_present_on_vsd(dhcp_options_to_verify,
                                                     vm1)
            combined_options = [invalid_dhcp_option, valid_dhcp_option]
            with self.assertRaises(Exception):
                self.when_i_update_extra_dhcp_options_on_a_vm(
                    vm1, combined_options)
            self.delete_VM(vm1)

    def validate_all_extra_dhcp_for_multi_nic(
            self, network,
            vpc=None,
            domain_name="testisolated.com",
            is_shared_network=False,
            verify_all_options=False):
        # 1 - create an extra isolated network
        # 2 - for each extra dhcp option:
            # a - deploy a vm with a nic in two networks
            # b - verify that the dhcp options are correctly set on the vsd
            # and vn
            # c - deploy a vm with a nic in two networks but now, let the other
            # network be the default network of the vm
            # d - verify that the dhcp options are correctly set on the vsd
            # and vm
        # 3 - try to deploy a multi nic vm with invalid dhcp options
        # (should fail)

        isolated_network2 =\
            self.create_isolated_network(gateway="10.0.1.1")

        if verify_all_options:
            options_to_verify = self.dhcp_options_map_keys
        else:
            options_to_verify = [1]

        for number in options_to_verify:
            dhcp_options_network = self.get_extra_dhcp_options_starting_with(
                number, network)
            dhcp_options_network2 = self.get_extra_dhcp_options_starting_with(
                number, isolated_network2)
            dhcp_options = [dhcp_options_network, dhcp_options_network2]

            # default nic is the network provided
            multinic_vm = self.when_i_create_a_vm(
                [network, isolated_network2], vpc, "vm1", dhcp_options,
                is_shared_network=is_shared_network)

            if not is_shared_network:
                self.then_verify_domain_name_and_router_options_multi_nic_set(
                    multinic_vm, network, domain_name)

            dhcp_options_to_verify =\
                self.get_extra_dhcp_options_to_verify_starting_with(
                    number, domain_name, is_shared_network)
            self.then_verify_dhcp_options_on_vsd_and_vm(
                multinic_vm, network, dhcp_options_to_verify,
                network_with_options=network,
                is_shared_network=is_shared_network, verify_on_vm=True,
                vpc=vpc)

            # is not primary nic so no option 12
            dhcp_options_to_verify =\
                self.get_extra_dhcp_options_to_verify_starting_with(
                    number, domain_name, remove_dns_options=True)
            self.then_verify_dhcp_options_on_vsd_and_vm(
                multinic_vm, network, dhcp_options_to_verify,
                network_with_options=isolated_network2,
                is_shared_network=is_shared_network, verify_on_vm=False,
                default_network=network, vpc=vpc)
            self.delete_VM(multinic_vm)

            # default nic is isolated_network2
            multinic_vm = self.when_i_create_a_vm(
                [isolated_network2, network], vpc, "vm1", dhcp_options,
                is_shared_network=is_shared_network)

            if not is_shared_network:
                self.then_verify_domain_name_and_router_options_multi_nic_set(
                    multinic_vm, isolated_network2, domain_name)
            dhcp_options_to_verify =\
                self.get_extra_dhcp_options_to_verify_starting_with(
                    number, domain_name, is_shared_network)
            self.then_verify_dhcp_options_on_vsd_and_vm(
                multinic_vm, network, dhcp_options_to_verify,
                network_with_options=isolated_network2,
                is_shared_network=is_shared_network, verify_on_vm=False,
                vpc=vpc)

            # is not primary nic so no option 12
            dhcp_options_to_verify =\
                self.get_extra_dhcp_options_to_verify_starting_with(
                    number, domain_name, True)
            self.then_verify_dhcp_options_on_vsd_and_vm(
                multinic_vm, network,
                dhcp_options_to_verify=dhcp_options_to_verify,
                network_with_options=network,
                is_shared_network=is_shared_network, verify_on_vm=True,
                default_network=isolated_network2, vpc=None)
            self.delete_VM(multinic_vm)

        invalid_dhcp_options_list = [{"networkid": network.id,
                                      "dhcp:":
                                          "http://www.testdhcpfeature.com"
                                          "/adfsgbfgtdhh125ki-23-fdh-09"},
                                     {"networkid": network.id,
                                      "dhcp:241":
                                          "http://www.testdhcpfeature.com"
                                          "/adfsgbfgtdhh125ki-23-fdh-09"},
                                     {"networkid": network.id,
                                      "unknownvalue":
                                          "http://www.testdhcpfeature.com"
                                          "/adfsgbfgtdhh125ki-23-fdh-09"},
                                     {"networkid": "invalidnetworkid",
                                      "dhcp:114":
                                          "http://www.testdhcpfeature.com"
                                          "/adfsgbfgtdhh125ki-23-fdh-09"}]
        for invalid_dhcp_option in invalid_dhcp_options_list:
            with self.assertRaises(Exception):
                self.when_i_create_a_vm(
                    [isolated_network2, network], vpc, "vm1",
                    dhcp_options=invalid_dhcp_option,
                    is_shared_network=is_shared_network)

    def validate_all_extra_dhcp_after_plug_nic(
            self, network,
            vpc=None,
            domain_name="testisolated.com",
            is_shared_network=False,
            verify_all_options=False):
        # 1 - create an extra isolated network
        # 2 - for each extra dchp options:
            # a - deploy a vm in the created isolated network
            # b - add an extra nic
            # c - verify if the dhcp options are correctly set
        # 3 - try to add a nic with invalid dhcp options (this should fail)

        isolated_network2 =\
            self.create_isolated_network(gateway="10.0.1.1")
        if verify_all_options:
            options_to_verify = self.dhcp_options_map_keys
        else:
            options_to_verify = [1]

        for number in options_to_verify:
            dhcp_options_to_verify =\
                self.get_extra_dhcp_options_to_verify_starting_with(
                    number, domain_name, True)
            vm1 = self.when_i_create_a_vm(
                isolated_network2, vpc, "vm1", None,
                is_shared_network=False)

            self.then_no_dhcp_options_present_on_vsd(
                dhcp_options_to_verify, vm1)

            dhcp_options_to_verify =\
                self.get_extra_dhcp_options_to_verify_starting_with(
                    number, domain_name, True)

            dhcp_options = self.get_extra_dhcp_options_starting_with(number)
            self.when_i_add_an_extra_nic_to_a_vm(vm1, network,
                                                 dhcp_options=dhcp_options)
            vm1.reboot(self.api_client)
            self.then_verify_dhcp_options_on_vsd_and_vm(
                vm1, network, dhcp_options_to_verify=dhcp_options_to_verify,
                network_with_options=network,
                is_shared_network=is_shared_network, verify_on_vm=True,
                default_network=isolated_network2, vpc=None)
            self.delete_VM(vm1)

        invalid_dhcp_options_list = [
            {"dhcp:": "http://www.testdhcpfeature.com/"
                      "adfsgbfgtdhh125ki-23-fdh-09"},
            {"dhcp:241": "http://www.testdhcpfeature.com/"
                         "adfsgbfgtdhh125ki-23-fdh-09"},
            {"unknownvalue": "http://www.testdhcpfeature.com/"
                             "adfsgbfgtdhh125ki-23-fdh-09"}]
        for invalid_dhcp_option in invalid_dhcp_options_list:
            vm1 = self.when_i_create_a_vm(
                isolated_network2, vpc, "vm1", None,
                is_shared_network=False)
            dhcp_options_to_verify =\
                self.get_extra_dhcp_options_to_verify_starting_with(
                    93, domain_name, remove_dns_options=True)
            self.then_no_dhcp_options_present_on_vsd(dhcp_options_to_verify,
                                                     vm1)
            with self.assertRaises(Exception):
                self.when_i_add_an_extra_nic_to_a_vm(
                    vm1, network, dhcp_options=invalid_dhcp_option)
            self.delete_VM(vm1)

    def validate_all_extra_dhcp_after_vm_update(
            self, network,
            vpc=None,
            domain_name="testisolated.com",
            is_shared_network=False,
            verify_all_options=False):
        # 1 - deploy a vm without extra dhcp options
        # 2 - verify no dhcp options are present
        # 3 - For each extra dhcp options
            # a - update the vm with extra dhcp options
            # b - check that the vm options are yet not updated on the vsd
            # and vm
            # c - stop and start the vm
            # d - check that the dhcp options are set on the vsd an vm
        # 4 - update a vm zith invalid dhcp options (this should fail)

        # option 1 to 13 is special because we start a vm here
        # instead of update
        dhcp_options_to_verify =\
            self.get_extra_dhcp_options_to_verify_starting_with(1, domain_name,
                                                                True)
        vm1 = self.when_i_create_a_vm(
            network, vpc, "vm1", None,
            is_shared_network=is_shared_network)
        self.then_no_dhcp_options_present_on_vsd(dhcp_options_to_verify, vm1)

        if verify_all_options:
            options_to_verify = self.dhcp_options_map_keys
        else:
            options_to_verify = [1]

        for number in options_to_verify:
            dhcp_options = self.get_extra_dhcp_options_starting_with(number,
                                                                     network)
            self.when_i_update_extra_dhcp_options_on_a_vm(vm1, dhcp_options)
            dhcp_options_to_verify =\
                self.get_extra_dhcp_options_to_verify_starting_with(
                    number, domain_name, True)
            self.then_no_dhcp_options_present_on_vsd(dhcp_options_to_verify,
                                                     vm1)

            # dhcp options get applied after start stop vm
            self.when_i_stop_and_start_a_vm(vm1)
            dhcp_options_to_verify =\
                self.get_extra_dhcp_options_to_verify_starting_with(
                    number, domain_name, is_shared_network)

            self.then_verify_dhcp_options_on_vsd_and_vm(
                vm1, network, dhcp_options_to_verify,
                is_shared_network=is_shared_network, verify_on_vm=True,
                vpc=vpc)

        invalid_dhcp_options_list = [{"networkid": network.id,
                                      "dhcp:": "http://www.testdhcpfeature.com"
                                               "/adfsgbfgtdhh125ki-23-fdh-09"},
                                     {"networkid": network.id,
                                      "dhcp:241": "http://www.testdhcpfeature"
                                                  ".com/adfsgbfgtdhh125ki-23"
                                                  "-fdh-09"},
                                     {"networkid": network.id,
                                      "unknownvalue": "http://www.testdhcp"
                                                      "feature.com/"
                                                      "adfsgbfgtdhh125ki-23-"
                                                      "fdh-09"},
                                     {"networkid": "invalidnetworkid",
                                      "dhcp:114": "http://www.testdhcpfeature"
                                                  ".com/adfsgbfgtdhh125ki-23-"
                                                  "fdh-09"}]
        for invalid_dhcp_option in invalid_dhcp_options_list:
            with self.assertRaises(Exception):
                self.when_i_update_extra_dhcp_options_on_a_vm(
                    vm1, invalid_dhcp_option)

    def validate_all_extra_dhcp_deploy_vm(
            self, network,
            vpc=None,
            domain_name="testisolated.com",
            is_shared_network=False,
            verify_all_options=False):
        # 1 - For each extra dhcp option:
            # a - deploy a vm with extra dhcp options
            # b - verify if the options are present on the vsd and vm
            # c - delete the VM
        # 2 - create a vm with different invalid dhcp options
        # (this should fail)

        if verify_all_options:
            options_to_verify = self.dhcp_options_map_keys
        else:
            options_to_verify = [1]

        for number in options_to_verify:
            dhcp_options = self.get_extra_dhcp_options_starting_with(
                number, network)

            dhcp_options_to_verify =\
                self.get_extra_dhcp_options_to_verify_starting_with(
                    number, domain_name, is_shared_network)
            vm1 = self.when_i_create_a_vm(
                network, vpc, "vm1", dhcp_options,
                is_shared_network=is_shared_network)

            self.then_verify_dhcp_options_on_vsd_and_vm(
                vm1, network, dhcp_options_to_verify,
                is_shared_network=is_shared_network,
                verify_on_vm=True,
                vpc=vpc)
            self.delete_VM(vm1)

        invalid_dhcp_options_list = [{"networkid": network.id,
                                      "dhcp:": "http://www.testdhcpfeature.com"
                                               "/adfsgbfgtdhh125ki-23-fdh"
                                               "-09"},
                                     {"networkid": network.id,
                                      "dhcp:241":
                                          "http://www.testdhcpfeature.com"
                                          "/adfsgbfgtdhh125ki-23-fdh-09"},
                                     {"networkid": network.id,
                                     "unknownvalue":
                                         "http://www.testdhcpfeature"
                                         ".com/adfsgbfgtdhh125ki-23-fdh-09"},
                                     {"networkid": "invalidnetworkid",
                                     "dhcp:114":
                                         "http://www.testdhcpfeature.com"
                                         "/adfsgbfgtdhh125ki-23-fdh-09"}]
        for invalid_dhcp_option in invalid_dhcp_options_list:
            with self.assertRaises(Exception):
                self.when_i_create_a_vm(
                    network, vpc, "vm2", invalid_dhcp_option,
                    is_shared_network=is_shared_network)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_01_nuage_extra_dhcp_single_nic_in_isolated_network(self):
        self.update_zone_details("testisolated.com")
        self.validate_isolated_network(
            self.isolated_network_offering, self.isolated_network)
        self.validate_all_extra_dhcp_deploy_vm(self.isolated_network,
                                               verify_all_options=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_02_nuage_extra_dhcp_single_nic_in_vpc(self):
        self.update_zone_details("testvpc.com")
        self.validate_vpc(self.vpc1, self.vpc_offering)
        self.validate_Network(self.vpc_network)

        self.validate_all_extra_dhcp_deploy_vm(
            self.vpc_network,
            self.vpc1,
            domain_name="testvpc.com")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_03_nuage_extra_dhcp_single_nic_in_shared_network(self):
        self.update_zone_details("testshared.com")
        self.validate_all_extra_dhcp_deploy_vm(
            self.shared_network_all,
            domain_name="testshared.com",
            is_shared_network=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_04_nuage_extra_dhcp_update_vm_in_isoltated_network(self):
        self.update_zone_details("testisolated.com")
        self.validate_isolated_network(
            self.isolated_network_offering, self.isolated_network)
        self.validate_all_extra_dhcp_after_vm_update(
            self.isolated_network)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_05_nuage_extra_dhcp_update_vm_in_vpc(self):
        self.update_zone_details("testvpc.com")
        self.validate_vpc(self.vpc1, self.vpc_offering)
        self.validate_Network(self.vpc_network)

        self.validate_all_extra_dhcp_after_vm_update(
            self.vpc_network, self.vpc1,
            domain_name="testvpc.com")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_06_nuage_extra_dhcp_update_vm_in_shared_network(self):
        self.update_zone_details("testshared.com")
        self.validate_all_extra_dhcp_after_vm_update(
            self.shared_network_all,
            domain_name="testshared.com",
            is_shared_network=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_07_nuage_extra_dhcp_add_nic_in_isolated_network(self):
        self.update_zone_details("testisolated.com")
        self.validate_isolated_network(
            self.isolated_network_offering, self.isolated_network)
        self.validate_all_extra_dhcp_after_plug_nic(
            self.isolated_network)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_08_nuage_extra_dhcp_add_nic_in_vpc(self):
        self.update_zone_details("testvpc.com")
        self.validate_vpc(self.vpc1, self.vpc_offering)
        self.validate_Network(self.vpc_network)

        self.validate_all_extra_dhcp_after_plug_nic(
            self.vpc_network, self.vpc1,
            domain_name="testvpc.com")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_09_nuage_extra_dhcp_add_nic_in_shared_network(self):
        self.update_zone_details("testshared.com")
        self.validate_all_extra_dhcp_after_plug_nic(
            self.shared_network_all,
            domain_name="testshared.com",
            is_shared_network=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_10_nuage_extra_dhcp_deploy_multi_nic_vm_in_isolated_network(self):
        self.update_zone_details("testisolated.com")
        self.validate_isolated_network(
            self.isolated_network_offering, self.isolated_network)
        self.validate_all_extra_dhcp_for_multi_nic(
            self.isolated_network)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_11_nuage_extra_dhcp_deploy_multi_nic_vm_in_vpc(self):
        self.update_zone_details("testvpc.com")
        self.validate_vpc(self.vpc1, self.vpc_offering)
        self.validate_Network(self.vpc_network)

        self.validate_all_extra_dhcp_for_multi_nic(
            self.vpc_network, self.vpc1,
            domain_name="testvpc.com")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_12_nuage_extra_dhcp_deploy_multi_nic_vm_in_shared_network(self):
        self.update_zone_details("testshared.com")
        self.validate_all_extra_dhcp_for_multi_nic(
            self.shared_network_all,
            domain_name="testshared.com",
            is_shared_network=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_13_nuage_extra_dhcp_update_multi_nic_in_isolated_network(self):
        self.update_zone_details("testisolated.com")
        self.validate_isolated_network(
            self.isolated_network_offering, self.isolated_network)
        self.validate_all_extra_dhcp_for_update_multinic(
            self.isolated_network)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_14_nuage_extra_dhcp_update_multi_nic_in_vpc(self):
        self.update_zone_details("testvpc.com")
        self.validate_vpc(self.vpc1, self.vpc_offering)
        self.validate_Network(self.vpc_network)

        self.validate_all_extra_dhcp_for_update_multinic(
            self.vpc_network, self.vpc1,
            domain_name="testvpc.com")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_15_nuage_extra_dhcp_update_multi_nic_in_shared_network(self):
        self.update_zone_details("testshared.com")
        self.validate_all_extra_dhcp_for_update_multinic(
            self.shared_network_all,
            domain_name="testshared.com",
            is_shared_network=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_16_nuage_extra_dhcp_remove_nic_in_isolated_network(self):
        self.update_zone_details("testisolated.com")
        self.validate_isolated_network(
            self.isolated_network_offering, self.isolated_network)
        self.validate_all_extra_dhcp_for_remove_nic_from_vm(
            self.isolated_network)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_17_nuage_extra_dhcp_remove_nic_in_vpc(self):
        self.update_zone_details("testvpc.com")
        self.validate_vpc(self.vpc1, self.vpc_offering)
        self.validate_Network(self.vpc_network)

        self.validate_all_extra_dhcp_for_remove_nic_from_vm(
            network=self.vpc_network,
            vpc=self.vpc1,
            domain_name="testvpc.com")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_18_nuage_extra_dhcp_remove_nic_in_shared_network(self):
        self.update_zone_details("testshared.com")
        self.validate_all_extra_dhcp_for_remove_nic_from_vm(
            self.shared_network_all,
            domain_name="testshared.com",
            is_shared_network=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_19_nuage_extra_dhcp_vm_actions_in_isolated_network(self):
        self.update_zone_details("testisolated.com")
        self.validate_isolated_network(
            self.isolated_network_offering, self.isolated_network)
        self.validate_all_extra_dhcp_for_vm_actions_in_network(
            self.isolated_network)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_20_nuage_nuage_extra_dhcp_vm_actions_in_vpc(self):
        self.update_zone_details("testvpc.com")
        self.validate_vpc(self.vpc1, self.vpc_offering)
        self.validate_Network(self.vpc_network)

        self.validate_all_extra_dhcp_for_vm_actions_in_network(
            network=self.vpc_network,
            vpc=self.vpc1,
            domain_name="testvpc.com")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_21_nuage_extra_dhcp_vm_actions_in_shared_network(self):
        self.update_zone_details("testshared.com")
        self.validate_all_extra_dhcp_for_vm_actions_in_network(
            self.shared_network_all,
            domain_name="testshared.com",
            is_shared_network=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_22_nuage_extra_dhcp_network_actions_in_isolated_network(self):
        self.update_zone_details("testisolated.com")
        self.validate_isolated_network(
            self.isolated_network_offering, self.isolated_network)
        self.validate_all_extra_dhcp_for_network_actions_in_network(
            self.isolated_network)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_23_nuage_nuage_extra_dhcp_network_actions_in_vpc(self):
        self.update_zone_details("testvpc.com")
        self.validate_vpc(self.vpc1, self.vpc_offering)
        self.validate_Network(self.vpc_network)

        self.validate_all_extra_dhcp_for_network_actions_in_network(
            self.vpc_network,
            vpc=self.vpc1,
            domain_name="testvpc.com")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_24_nuage_extra_dhcp_network_actions_in_shared_network(self):
        self.update_zone_details("testshared.com")
        self.validate_all_extra_dhcp_for_network_actions_in_network(
            self.shared_network_all,
            domain_name="testshared.com",
            is_shared_network=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_25_nuage_extra_dhcp_nic_after_migrate_in_isolated_network(self):
        self.update_zone_details("testisolated.com")
        self.validate_isolated_network(
            self.isolated_network_offering, self.isolated_network)
        self.validate_all_extra_dhcp_for_add_remove_nic_after_migrate(
            self.isolated_network)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_26_nuage_nuage_extra_dhcp_nic_after_migrate_in_vpc(self):
        self.update_zone_details("testvpc.com")
        self.validate_vpc(self.vpc1, self.vpc_offering)
        self.validate_Network(self.vpc_network)

        self.validate_all_extra_dhcp_for_add_remove_nic_after_migrate(
            self.vpc_network,
            domain_name="testvpc.com")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_27_nuage_extra_dhcp_nic_after_migrate_in_shared_network(self):
        self.update_zone_details("testshared.com")
        self.validate_all_extra_dhcp_for_add_remove_nic_after_migrate(
            self.shared_network_all,
            domain_name="testshared.com",
            is_shared_network=True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_28_nuage_extra_dhcp_deploy_multiple_vms(self):
        self.update_zone_details("testisolated.com")
        isolated_network =\
            self.create_isolated_network(gateway="10.0.0.1")
        self.validate_vm_deploy_concurrency(
            isolated_network)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_29_nuage_extra_dhcp_allocated_isolated_network(self):
        self.update_zone_details("testisolated.com")
        self.validate_isolated_network(
            self.isolated_network_offering, self.isolated_network,)
        self.validate_all_extra_dhcp_for_network_in_allocated(
            self.isolated_network)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_30_nuage_extra_dhcp_allocated_vpc(self):
        self.update_zone_details("testvpc.com")
        self.validate_vpc(self.vpc1, self.vpc_offering)
        self.validate_Network(self.vpc_network)

        self.validate_all_extra_dhcp_for_network_in_allocated(
            self.vpc_network, self.vpc1,
            domain_name="testvpc.com")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_31_nuage_extra_dhcp_allocated_shared_network(self):
        self.update_zone_details("testshared.com")
        self.validate_all_extra_dhcp_for_network_in_allocated(
            self.shared_network_all,
            domain_name="testshared.com",
            is_shared_network=True)

    @attr(tags=["advanced", "nuagevsp", "smoke"], required_hardware="false")
    def smoke_test(self):
        # This test does basic sanity checks to see if basic
        # DHCP options still work.
        # 1 - deploy vm in an isolated network
        # 2 - verify dhcp options
        # 3 - update dhcp options
        # 4 - add nic to a vpc_network with different dhcp options
        # 5 - restart the vm
        # 6 - check if dhcp options are on the extra nic and the default nic
        # 7 - restart the network
        # 8 - verify if the dhcp options are set correctly
        # 9 - remove the vm

        network = self.isolated_network
        domain_name = "testisolated.com"
        self.update_zone_details(domain_name)

        dhcp_options_isolated_network =\
            self.get_extra_dhcp_options_starting_with(1, network)

        dhcp_options_to_verify = \
            self.get_extra_dhcp_options_to_verify_starting_with(
                1, domain_name, remove_dns_options=False)
        vm1 = self.when_i_create_a_vm(
            network,
            vpc=None,
            vm_name="vm1",
            dhcp_options=dhcp_options_isolated_network)

        self.then_verify_dhcp_options_on_vsd_and_vm(
            vm1, network, dhcp_options_to_verify,
            verify_on_vm=True)

        dhcp_options_isolated_network =\
            self.get_extra_dhcp_options_starting_with(16, network)
        self.when_i_update_extra_dhcp_options_on_a_vm(
            vm1, dhcp_options_isolated_network)

        dhcp_options_vpc_network = self.get_extra_dhcp_options_starting_with(
            28, None)
        self.when_i_add_an_extra_nic_to_a_vm(
            vm1, self.vpc_network, dhcp_options_vpc_network)

        self.when_i_stop_and_start_a_vm(vm1)

        dhcp_options_to_verify_on_default_nic = \
            self.get_extra_dhcp_options_to_verify_starting_with(
                16, domain_name, False)

        dhcp_options_to_verify_on_second_nic = \
            self.get_extra_dhcp_options_to_verify_starting_with(
                28, domain_name, remove_dns_options=True)

        # dhcp options get applied after start stop vm
        self.then_verify_dhcp_options_on_vsd_and_vm(
            vm1, network, dhcp_options_to_verify_on_default_nic,
            verify_on_vm=True)

        self.then_verify_dhcp_options_on_vsd_and_vm(
            vm1, network, dhcp_options_to_verify_on_second_nic,
            network_with_options=self.vpc_network,
            verify_on_vm=True,
            default_network=network,
            vpc=None)

        network.restart(self.api_client, True)
        self.vpc_network.restart(self.api_client, True)
        self.then_verify_dhcp_options_on_vsd_and_vm(
            vm1, network, dhcp_options_to_verify_on_default_nic,
            verify_on_vm=True)

        self.then_verify_dhcp_options_on_vsd_and_vm(
            vm1, network, dhcp_options_to_verify_on_second_nic,
            network_with_options=self.vpc_network,
            verify_on_vm=True,
            default_network=network,
            vpc=None)

        self.delete_VM(vm1)
