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

""" Component tests for VSP Managed Subnets functionality
    with Nuage VSP SDN plugin
"""
# Import Local Modules
from nuageTestCase import nuageTestCase, needscleanup
from marvin.lib.base import (Account,
                             Domain,
                             VirtualMachine)
from marvin.cloudstackAPI import updateZone

# Import System Modules
from nose.plugins.attrib import attr
import time


class TestNuageManagedSubnets(nuageTestCase):
    """Test Managed Subnets functionality with Nuage VSP SDN plugin
    """

    @classmethod
    def setUpClass(cls):
        super(TestNuageManagedSubnets, cls).setUpClass()

        # create a nuage vpc offering
        cls.nuage_vpc_offering = \
            cls.create_VpcOffering(cls.test_data["nuagevsp"]["vpc_offering"])

        # tier network offerings
        cls.nuage_vpc_network_offering = \
            cls.create_NetworkOffering(cls.test_data["nuagevsp"]
                                       ["vpc_network_offering"])

        # create a Nuage isolated network offering with vr
        cls.nuage_isolated_network_offering = cls.create_NetworkOffering(
                cls.test_data["nuagevsp"]["isolated_network_offering"], True)

        # create a Nuage isolated network offering with vr and persistent
        cls.nuage_isolated_network_offering_persistent = \
            cls.create_NetworkOffering(
                    cls.test_data["nuagevsp"]
                    ["isolated_network_offering_persistent"],
                    True)

        # create a Nuage isolated network offering without vr
        cls.nuage_isolated_network_offering_without_vr = \
            cls.create_NetworkOffering(
                    cls.test_data["nuagevsp"]
                    ["isolated_network_offering_without_vr"],
                    True)

        # create a Nuage isolated network offering without vr but persistent
        cls.nuage_isolated_network_offering_without_vr_persistent = \
            cls.create_NetworkOffering(
                    cls.test_data["nuagevsp"]
                    ["isolated_network_offering_without_vr_persistent"],
                    True)

        # create a Nuage shared network offering
        cls.nuage_shared_network_offering = cls.create_NetworkOffering(
                cls.test_data["nuagevsp"]["shared_nuage_network_offering"],
                False)

        cls._cleanup = [
            cls.nuage_isolated_network_offering,
            cls.nuage_isolated_network_offering_persistent,
            cls.nuage_isolated_network_offering_without_vr,
            cls.nuage_isolated_network_offering_without_vr_persistent,
            cls.nuage_vpc_offering,
            cls.nuage_vpc_network_offering,
            cls.nuage_shared_network_offering
        ]
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

    def verify_ping_to_vm(self, src_vm, dst_vm, public_ip, dst_hostname=None):
        if self.isSimulator:
            self.debug("Simulator Environment: not verifying pinging")
            return
        try:
            src_vm.ssh_ip = public_ip.ipaddress.ipaddress
            src_vm.ssh_port = self.test_data["virtual_machine"]["ssh_port"]
            src_vm.username = self.test_data["virtual_machine"]["username"]
            src_vm.password = self.test_data["virtual_machine"]["password"]
            self.debug("SSHing into VM: %s with %s" %
                       (src_vm.ssh_ip, src_vm.password))

            ssh = self.ssh_into_VM(src_vm, public_ip)

        except Exception as e:
            self.fail("SSH into VM failed with exception %s" % e)

        self.verify_pingtovmipaddress(ssh, dst_vm.ipaddress)
        if dst_hostname:
            self.verify_pingtovmipaddress(ssh, dst_hostname)

    def verify_pingtovmipaddress(self, ssh, pingtovmipaddress):
        """verify ping to ipaddress of the vm and retry 3 times"""
        successfull_ping = False
        nbr_retries = 0
        max_retries = 5
        cmd = 'ping -c 2 ' + pingtovmipaddress

        while not successfull_ping and nbr_retries < max_retries:
            self.debug("ping vm by ipaddress with command: " + cmd)
            outputlist = ssh.execute(cmd)
            self.debug("command is executed properly " + cmd)
            completeoutput = str(outputlist).strip('[]')
            self.debug("complete output is " + completeoutput)
            if '2 received' in completeoutput:
                self.debug("PASS as vm is pingeable: " + completeoutput)
                successfull_ping = True
            else:
                self.debug("FAIL as vm is not pingeable: " + completeoutput)
                time.sleep(3)
                nbr_retries = nbr_retries + 1

        if not successfull_ping:
            self.fail("FAILED TEST as excepted value not found in vm")

    # verify_vsd_vm - Verifies the given CloudStack VM deployment and status in
    # VSD
    def verify_vsdmngd_vm(self, vm, vsdmngd_subnet, stopped=False):
        self.debug("Verifying the deployment and state of VSD Managed VM "
                   "- %s in VSD" % vm.name)
        vsd_vm = self.vsd.get_vm(filter=self.get_externalID_filter(vm.id))
        self.assertNotEqual(vsd_vm, None,
                            "VM data format in VSD should not be of type None"
                            )
        vm_info = VirtualMachine.list(self.api_client, id=vm.id)[0]
        for nic in vm_info.nic:
            vsd_subnet = vsdmngd_subnet
            vsd_vport = self.vsd.get_vport(
                subnet=vsd_subnet, filter=self.get_externalID_filter(nic.id))
            vsd_vm_interface = self.vsd.get_vm_interface(
                    filter=self.get_externalID_filter(nic.id))
            self.assertEqual(vsd_vport.active, True,
                             "VSD VM vport should be active"
                             )
            self.assertEqual(vsd_vm_interface.ip_address, nic.ipaddress,
                             "VSD VM interface IP address should match VM's "
                             "NIC IP address in CloudStack"
                             )
        if not self.isSimulator:
            self.verify_vsd_object_status(vm, stopped)
        self.debug("Successfully verified the deployment and state of VM - %s "
                   "in VSD" % vm.name)

    @attr(tags=["advanced", "nuagevsp", "isonw"], required_hardware="false")
    def test_01_nuage_mngd_subnets_isonw(self):
        """Test Nuage VSP Managed Subnets for isolated networks
        """

        # 1. Create multiple L3DomainTemplate with Zone and Subnet on VSP
        #    Create Ingress & Egress ACL Top & Bottom Templates
        #    Add ACL rules to allow intra-subnet traffic
        #    Instiantiate these L3Domains and store its Subnet VSD ID
        # 2. Create a persistent and non persistent isolated network offering
        #    create offerings with and without VirtualRouter
        # 3. Create isolated networks specifying above offerings and
        #    specifying the stored Subnet ID's of VSP
        # 4. Verify ACL rules and connectivity via deploying VM's ,
        #    Enabling staticNAT, applying firewall and egress rules
        # 5. Verify negative tests like uniqueness of vsd subnet

        # Create all items on vsd required for this test
        enterprise = self.fetch_by_externalID(self._session.user.enterprises,
                                              self.domain)
        domain_template = self.create_vsd_domain_template(enterprise)

        self.create_vsd_default_acls(domain_template)

        domain1 = self.create_vsd_domain(domain_template, enterprise,
                                         "L3DomainToBeConsumedByACS")
        zone1 = self.create_vsd_zone(domain1, "ZoneToBeConsumedByACS")
        subnet1 = self.create_vsd_subnet(zone1, "SubnetToBeConsumedByACS",
                                         "10.0.0.1/24")

        domain2 = self.create_vsd_domain(domain_template, enterprise,
                                         "2ndL3DomainToBeConsumedByACS")
        zone2 = self.create_vsd_zone(domain2, "2ndZoneToBeConsumedByACS")
        subnet2 = self.create_vsd_subnet(zone2, "2ndSubnetToBeConsumedByACS",
                                         "10.1.0.1/24")

        self.create_vsd_dhcp_option(subnet2, 15, ["nuagenetworks2.net"])

        domain3 = self.create_vsd_domain(domain_template, enterprise,
                                         "3rdL3DomainToBeConsumedByACS")
        zone3 = self.create_vsd_zone(domain3, "3rdZoneToBeConsumedByACS")
        subnet3 = self.create_vsd_subnet(zone3, "3rdSubnetToBeConsumedByACS",
                                         "10.2.0.1/24")
        for i in range(1, 3):
            # On ACS create network using non-persistent nw offering allow
            isolated_network = self.create_Network(
                    self.nuage_isolated_network_offering,
                    gateway="10.0.0.1", netmask="255.255.255.0",
                    externalid=subnet1.id, cleanup=False)

            # On ACS create network using persistent nw offering allow
            isolated_network2 = self.create_Network(
                    self.nuage_isolated_network_offering_persistent,
                    gateway="10.5.0.1", netmask="255.255.255.0",
                    externalid=subnet2.id, cleanup=False)

            with self.assertRaises(Exception):
                self.create_Network(
                    self.nuage_shared_network_offering, gateway="10.2.0.1",
                    netmask="255.255.255.0", vlan=1201, externalid=subnet3.id)

            # On ACS create network when VSDSubnet is already in use
            with self.assertRaises(Exception):
                self.create_Network(
                        self.nuage_isolated_network_offering_persistent,
                        gateway="10.3.0.1", netmask="255.255.255.0",
                        externalid=subnet2.id)

            # On ACS create network when VSDSubnet is non-existing
            with self.assertRaises(Exception):
                self.create_Network(
                        self.nuage_isolated_network_offering_persistent,
                        gateway="10.4.0.1", netmask="255.255.255.0",
                        externalid=subnet2.id+1)

            # verify floating ip and intra subnet connectivity
            vm_1 = self.create_VM(isolated_network, cleanup=False)

            self.test_data["virtual_machine"]["displayname"] = "vm2"
            self.test_data["virtual_machine"]["name"] = "vm2"
            vm_2 = self.create_VM(isolated_network, cleanup=False)
            self.test_data["virtual_machine"]["displayname"] = None
            self.test_data["virtual_machine"]["name"] = None

            # VSD verification
            self.verify_vsd_network_not_present(isolated_network)
            self.verify_vsdmngd_vm(vm_1, subnet1)
            self.verify_vsdmngd_vm(vm_2, subnet1)
            self.debug("Creating Static NAT rule for the deployed VM in the "
                       "non persistently created Isolated network...")
            public_ip = self.acquire_PublicIPAddress(isolated_network)
            self.validate_PublicIPAddress(public_ip, isolated_network)
            self.create_StaticNatRule_For_VM(vm_1, public_ip, isolated_network)
            self.validate_PublicIPAddress(
                    public_ip, isolated_network, static_nat=True, vm=vm_1)
            self.create_FirewallRule(public_ip,
                                     self.test_data["ingress_rule"])
            self.verify_ping_to_vm(vm_1, vm_2, public_ip, "vm2")

            vm_3 = self.create_VM(isolated_network2, cleanup=False)
            self.test_data["virtual_machine"]["displayname"] = "vm4"
            self.test_data["virtual_machine"]["name"] = "vm4"
            vm_4 = self.create_VM(isolated_network2, cleanup=False)
            self.test_data["virtual_machine"]["displayname"] = None
            self.test_data["virtual_machine"]["name"] = None
            self.verify_vsd_network_not_present(isolated_network2)
            self.verify_vsdmngd_vm(vm_3, subnet2)
            self.verify_vsdmngd_vm(vm_4, subnet2)
            self.debug("Creating Static NAT rule for the deployed VM in the "
                       "persistently created Isolated network...")
            public_ip2 = self.acquire_PublicIPAddress(isolated_network2)
            self.validate_PublicIPAddress(public_ip2, isolated_network2)
            self.create_StaticNatRule_For_VM(vm_3, public_ip2,
                                             isolated_network2)
            self.validate_PublicIPAddress(
                public_ip2, isolated_network2, static_nat=True, vm=vm_3)
            self.create_FirewallRule(public_ip2,
                                     self.test_data["ingress_rule"])

            self.verify_ping_to_vm(vm_3, vm_4, public_ip2)
            vm_4.delete(self.api_client, expunge=True)
            vm_3.delete(self.api_client, expunge=True)
            vm_2.delete(self.api_client, expunge=True)
            vm_1.delete(self.api_client, expunge=True)
            isolated_network2.delete(self.api_client)
            isolated_network.delete(self.api_client)
            self.debug("Number of loops %s" % i)

    @attr(tags=["advanced", "nuagevsp", "vpc"], required_hardware="false")
    def test_02_nuage_mngd_subnets_vpc(self):
        """Test Nuage VSP Managed Subnets for vpc and tier networks
        """

        # 1. Create multiple L3DomainTemplate with Zone and Subnet on VSP
        #    Create Ingress & Egress ACL Top & Bottom Templates
        #    Add ACL rules to allow intra-subnet traffic
        #    Instiantiate these L3Domains and store its Subnet VSD ID
        # 2. Create a vpc network offering and create a VPC
        #    create vpc tier network offerings with and without VirtualRouter
        # 3. Create vpc tier networks specifying above offerings and
        #    specifying the stored Subnet ID's of VSP
        # 4. Verify ACL rules and connectivity via deploying VM's ,
        #    Enabling staticNAT, applying firewall and egress rules
        # 5. Verify negative tests like uniqueness of vsd subnet

        # Create all items on vsd required for this test
        enterprise = self.fetch_by_externalID(self._session.user.enterprises,
                                              self.domain)
        domain_template = self.create_vsd_domain_template(enterprise)

        self.create_vsd_default_acls(domain_template)

        domain1 = self.create_vsd_domain(domain_template, enterprise,
                                         "L3DomainToBeConsumedByACS")
        zone1 = self.create_vsd_zone(domain1, "ZoneToBeConsumedByACS")
        subnet1 = self.create_vsd_subnet(zone1, "SubnetToBeConsumedByACS",
                                         "10.1.0.1/24")
        subnet2 = self.create_vsd_subnet(zone1, "2ndSubnetToBeConsumedByACS",
                                         "10.1.128.1/24")

        domain2 = self.create_vsd_domain(domain_template, enterprise,
                                         "2ndL3DomainToBeConsumedByACS")
        zone2 = self.create_vsd_zone(domain2, "2ndZoneToBeConsumedByACS")
        subnet3 = self.create_vsd_subnet(zone2, "3rdSubnetToBeConsumedByACS",
                                         "10.2.128.1/24")

        cmd = updateZone.updateZoneCmd()
        cmd.id = self.zone.id
        cmd.domain = "vpc.com"
        self.api_client.updateZone(cmd)
        self.debug("Creating a VPC with Static NAT service provider as "
                   "VpcVirtualRouter")
        vpc = self.create_Vpc(self.nuage_vpc_offering, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc, state="Enabled")
        acl_list = self.create_NetworkAclList(
            name="acl", description="acl", vpc=vpc)
        self.create_NetworkAclRule(
            self.test_data["ingress_rule"], acl_list=acl_list)
        self.create_NetworkAclRule(
            self.test_data["icmprule"], acl_list=acl_list)

        self.debug("Creating another VPC with Static NAT service provider "
                   "as VpcVirtualRouter")
        vpc2 = self.create_Vpc(self.nuage_vpc_offering, cidr='10.2.0.0/16')
        self.validate_Vpc(vpc2, state="Enabled")
        acl_list2 = self.create_NetworkAclList(
                name="acl", description="acl", vpc=vpc2)
        self.create_NetworkAclRule(
                self.test_data["ingress_rule"], acl_list=acl_list2)
        self.create_NetworkAclRule(
                self.test_data["icmprule"], acl_list=acl_list2)

        self.debug("Creating an unmanaged VPC tier network with Static NAT")
        vpc2_tier_unmngd = self.create_Network(self.nuage_vpc_network_offering,
                                               gateway='10.2.0.1',
                                               vpc=vpc2,
                                               acl_list=acl_list2)
        self.validate_Network(vpc2_tier_unmngd, state="Implemented")

        # VPC Tier Network creation should fail as VPC is unmanaged already
        with self.assertRaises(Exception):
            self.create_Network(self.nuage_vpc_network_offering,
                                gateway='10.2.128.1',
                                vpc=vpc2,
                                acl_list=acl_list2,
                                externalid=subnet3.id)

        vpc2_tier_unmngd.delete(self.api_client)
        vpc2.delete(self.api_client)

        # VPC tier network creation fails when cidr does not match on VSD
        with self.assertRaises(Exception):
            self.create_Network(self.nuage_vpc_network_offering,
                                gateway='10.1.1.1',
                                vpc=vpc,
                                acl_list=acl_list,
                                externalid=subnet1.id)

        for i in range(1, 3):
            self.debug("Creating a mngd VPC tier with Static NAT service")
            vpc_tier = self.create_Network(self.nuage_vpc_network_offering,
                                           gateway='10.1.0.1',
                                           vpc=vpc,
                                           acl_list=acl_list,
                                           externalid=subnet1.id,
                                           cleanup=False)
            self.validate_Network(vpc_tier, state="Implemented")
            self.debug("Creating 2nd VPC tier network with Static NAT service")

            # VPC 2nd tier creation fails when cidr doesn't match on VSD
            with self.assertRaises(Exception):
                self.create_Network(self.nuage_vpc_network_offering,
                                    gateway='10.1.129.1',
                                    vpc=vpc,
                                    acl_list=acl_list,
                                    externalid=subnet2.id)

            vpc_2ndtier = self.create_Network(self.nuage_vpc_network_offering,
                                              gateway='10.1.128.1',
                                              vpc=vpc,
                                              acl_list=acl_list,
                                              externalid=subnet2.id,
                                              cleanup=False)
            self.validate_Network(vpc_2ndtier, state="Implemented")
            vpc_vr = self.get_Router(vpc_tier)
            self.check_Router_state(vpc_vr, state="Running")

            # VSD verification
            self.verify_vsd_network_not_present(vpc_tier, vpc)
            self.verify_vsd_network_not_present(vpc_2ndtier, vpc)

            # On ACS create VPCTier network when VSDSubnet is already in use
            with self.assertRaises(Exception):
                self.create_Network(self.nuage_vpc_network_offering,
                                    gateway='10.1.128.1',
                                    vpc=vpc,
                                    acl_list=acl_list,
                                    externalid=subnet2.id)

            # On ACS create VPCTier network when VSDSubnet does not exist
            with self.assertRaises(Exception):
                self.create_Network(self.nuage_vpc_network_offering,
                                    gateway='10.1.128.1',
                                    vpc=vpc,
                                    acl_list=acl_list,
                                    externalid=subnet2.id+1)

            # On ACS create VPCTier network without VSDSubnet should fail
            with self.assertRaises(Exception):
                self.create_Network(self.nuage_vpc_network_offering,
                                    gateway='10.1.203.1',
                                    vpc=vpc,
                                    acl_list=acl_list)

            self.debug("Creating another VPC with Static NAT service provider "
                       "as VpcVirtualRouter With same CIDR")
            vpc3 = self.create_Vpc(self.nuage_vpc_offering, cidr='10.1.0.0/16')
            self.validate_Vpc(vpc3, state="Enabled")
            acl_list3 = self.create_NetworkAclList(
                    name="acl", description="acl", vpc=vpc3)
            self.create_NetworkAclRule(
                    self.test_data["ingress_rule"], acl_list=acl_list3)
            self.create_NetworkAclRule(
                    self.test_data["icmprule"], acl_list=acl_list3)

            self.debug("Creating a mngd VPC tier with Static NAT service")
            vpc3_tier_unmngd = \
                self.create_Network(self.nuage_vpc_network_offering,
                                    gateway='10.1.0.1',
                                    vpc=vpc3,
                                    acl_list=acl_list3)
            self.validate_Network(vpc3_tier_unmngd, state="Implemented")
            vpc3_tier_unmngd.delete(self.api_client)
            vpc3.delete(self.api_client)

            self.debug("Deploying a VM in the created VPC tier network")
            self.test_data["virtual_machine"]["displayname"] = "vpcvm1"
            self.test_data["virtual_machine"]["name"] = "vpcvm1"
            vpc_vm_1 = self.create_VM(vpc_tier, cleanup=False)
            self.check_VM_state(vpc_vm_1, state="Running")
            self.debug("Deploying another VM in the created VPC tier network")
            self.test_data["virtual_machine"]["displayname"] = "vpcvm2"
            self.test_data["virtual_machine"]["name"] = "vpcvm2"
            vpc_vm_2 = self.create_VM(vpc_tier, cleanup=False)
            self.check_VM_state(vpc_vm_2, state="Running")
            self.debug("Deploying a VM in the 2nd VPC tier network")
            self.test_data["virtual_machine"]["displayname"] = "vpcvm12"
            self.test_data["virtual_machine"]["name"] = "vpcvm12"
            vpc_vm_12 = self.create_VM(vpc_2ndtier, cleanup=False)
            self.check_VM_state(vpc_vm_2, state="Running")
            self.test_data["virtual_machine"]["displayname"] = None
            self.test_data["virtual_machine"]["name"] = None

            # VSD verification
            self.verify_vsdmngd_vm(vpc_vm_1, subnet1)
            self.verify_vsdmngd_vm(vpc_vm_2, subnet1)
            self.verify_vsdmngd_vm(vpc_vm_12, subnet2)

            self.debug("Creating Static NAT rule for the deployed VM "
                       "in the created VPC network...")
            public_ip_1 = self.acquire_PublicIPAddress(vpc_tier, vpc=vpc)
            self.validate_PublicIPAddress(public_ip_1, vpc_tier)
            self.create_StaticNatRule_For_VM(vpc_vm_1, public_ip_1, vpc_tier)
            self.validate_PublicIPAddress(
                    public_ip_1, vpc_tier, static_nat=True, vm=vpc_vm_1)

            self.verify_ping_to_vm(vpc_vm_1, vpc_vm_2, public_ip_1)
            self.verify_ping_to_vm(vpc_vm_1, vpc_vm_12, public_ip_1)

            vpc_vm_1.delete(self.api_client, expunge=True)
            vpc_vm_2.delete(self.api_client, expunge=True)
            vpc_vm_12.delete(self.api_client, expunge=True)
            vpc_tier.delete(self.api_client)
            vpc_2ndtier.delete(self.api_client)
            self.debug("Number of loops %s" % i)

    @attr(tags=["advanced", "nuagevsp", "domains"], required_hardware="false")
    def test_03_nuage_mngd_subnets_domains(self):
        """Test Nuage VSP Managed Subnets for ACS domains
        """
        vsd_enterprise = self.create_vsd_enterprise()
        vsd_domain_template = self.create_vsd_domain_template(vsd_enterprise)

        self.create_vsd_default_acls(vsd_domain_template)

        vsd_domain1 = self.create_vsd_domain(vsd_domain_template,
                                             vsd_enterprise,
                                             "L3DomainToBeConsumedByACS")
        vsd_zone1 = self.create_vsd_zone(vsd_domain1, "ZoneToBeConsumedByACS")
        vsd_subnet1 = self.create_vsd_subnet(vsd_zone1,
                                             "SubnetToBeConsumedByACS",
                                             "10.0.0.1/24")
        acs_domain_1 = Domain.create(
                self.api_client,
                {},
                name="DomainManagedbyVsd",
                domainid=vsd_enterprise.id
                )
        # Create an admin and an user account under domain D1
        acs_account_1 = Account.create(
                self.api_client,
                self.test_data["acl"]["accountD1"],
                admin=True,
                domainid=acs_domain_1.id
                )
        self.cleanup.append(acs_domain_1)
        self.cleanup.append(acs_account_1)

        # On ACS create network using non-persistent nw offering allow
        isolated_network = self.create_Network(
                self.nuage_isolated_network_offering,
                gateway="10.0.0.1", netmask="255.255.255.0",
                account=acs_account_1,
                externalid=vsd_subnet1.id)

        # Creation of a domain with inUse domain UUID is not allowed
        with self.assertRaises(Exception):
            Domain.create(
                    self.api_client,
                    {},
                    name="AnotherDomainManagedbyVsd",
                    domainid=vsd_enterprise.id
            )

        # Creation of a domain with unexisting domain UUID is not allowed
        with self.assertRaises(Exception):
            Domain.create(
                    self.api_client,
                    {},
                    name="YetAnotherDomainManagedbyVsd",
                    domainid=vsd_enterprise.id+1
            )
        vm_1 = self.create_VM(isolated_network, account=acs_account_1)
        vm_2 = self.create_VM(isolated_network, account=acs_account_1)
        # VSD verification
        self.verify_vsd_network_not_present(isolated_network)
        self.verify_vsdmngd_vm(vm_1, vsd_subnet1)
        self.verify_vsdmngd_vm(vm_2, vsd_subnet1)
        self.debug("Creating Static NAT rule for the deployed VM in the "
                   "non persistently created Isolated network...")
        public_ip = self.acquire_PublicIPAddress(isolated_network,
                                                 account=acs_account_1)
        self.validate_PublicIPAddress(public_ip, isolated_network)
        self.create_StaticNatRule_For_VM(vm_1, public_ip, isolated_network)
        self.validate_PublicIPAddress(
                public_ip, isolated_network, static_nat=True, vm=vm_1)
        self.create_FirewallRule(public_ip,
                                 self.test_data["ingress_rule"])
        if not self.isSimulator:
            vm_public_ip = public_ip.ipaddress.ipaddress
            try:
                vm_1.ssh_ip = vm_public_ip
                vm_1.ssh_port = self.test_data["virtual_machine"]["ssh_port"]
                vm_1.username = self.test_data["virtual_machine"]["username"]
                vm_1.password = self.test_data["virtual_machine"]["password"]
                self.debug("SSHing into VM: %s with %s" %
                           (vm_1.ssh_ip, vm_1.password))

                ssh = vm_1.get_ssh_client(ipaddress=vm_public_ip)

            except Exception as e:
                self.fail("SSH into VM failed with exception %s" % e)

            self.verify_pingtovmipaddress(ssh, vm_2.ipaddress)

    @attr(tags=["advanced", "nuagevsp", "account"], required_hardware="false")
    def test_04_nuage_mngd_subnets_noadminaccount(self):
        """Test Nuage VSP Managed Subnets for ACS domains without admin account
        """
        vsd_enterprise = self.create_vsd_enterprise()
        vsd_domain_template = self.create_vsd_domain_template(vsd_enterprise)

        self.create_vsd_default_acls(vsd_domain_template)

        vsd_domain1 = self.create_vsd_domain(vsd_domain_template,
                                             vsd_enterprise,
                                             "L3DomainToBeConsumedByACS")
        vsd_zone1 = self.create_vsd_zone(vsd_domain1, "ZoneToBeConsumedByACS")
        vsd_subnet1 = self.create_vsd_subnet(vsd_zone1,
                                             "SubnetToBeConsumedByACS",
                                             "10.0.0.1/24")
        acs_domain_1 = Domain.create(
                self.api_client,
                {},
                name="DomainManagedbyVsd",
                domainid=vsd_enterprise.id
        )
        # Create an no admin and an user account under domain D1
        acs_account_1 = Account.create(
                self.api_client,
                self.test_data["acl"]["accountD1"],
                admin=False,
                domainid=acs_domain_1.id
        )
        self.cleanup.append(acs_domain_1)
        self.cleanup.append(acs_account_1)

        # On ACS create network fails as non admin account
        with self.assertRaises(Exception):
            self.create_Network(
                self.nuage_isolated_network_offering,
                gateway="10.0.0.1", netmask="255.255.255.0",
                account=acs_account_1,
                externalid=vsd_subnet1.id)

    @needscleanup
    def create_vsd_enterprise(self):
        enterprise = self.vsdk.NUEnterprise()
        enterprise.name = "EnterpriseToBeConsumedByACS"
        enterprise.description = "EnterpriseToBeConsumedByACS"
        (enterprise, connection) = self._session.user.create_child(enterprise)
        return enterprise

    def create_vsd_ingress_acl_template(self, domain_template,
                                        priority_type="TOP"):
        name = "Ingress ACL " + str(priority_type).capitalize()
        acl_template = self.vsdk.NUIngressACLTemplate()
        acl_template.name = name
        acl_template.description = name
        acl_template.priority_type = priority_type
        acl_template.active = True
        (acl_template, connection) = \
            domain_template.create_child(acl_template)
        return acl_template

    def create_vsd_egress_acl_template(self, domain_template,
                                       priority_type='TOP'):
        name = "Egress ACL " + str(priority_type).capitalize()
        acl_template = self.vsdk.NUEgressACLTemplate()
        acl_template.name = name
        acl_template.description = name
        acl_template.priority_type = priority_type
        acl_template.active = True
        (acl_template, connection) = \
            domain_template.create_child(acl_template)
        return acl_template

    @needscleanup
    def create_vsd_domain_template(self, enterprise):
        domain_template = self.vsdk.NUDomainTemplate()
        domain_template.name = "L3DomainTemplateToBeConsumedByACS"
        domain_template.description = "L3DomainTemplateToBeConsumedByACS"
        (domain_template, connection) = \
            enterprise.create_child(domain_template)
        return domain_template

    def create_vsd_default_acls(self, domain_template):
        ingress_vsd_acl_template1 = self.create_vsd_ingress_acl_template(
            domain_template, "TOP")
        ingress_vsd_acl_template2 = self.create_vsd_ingress_acl_template(
            domain_template, "BOTTOM")
        ingress_vsd_acl_entry1 = self.vsdk.NUIngressACLEntryTemplate()
        ingress_vsd_acl_entry1.name = "Default Intra-Subnet Allow"
        ingress_vsd_acl_entry1.description = "Default Intra-Subnet Allow"
        ingress_vsd_acl_entry1.priority = '1'
        ingress_vsd_acl_entry1.protocol = 'ANY'
        ingress_vsd_acl_template1.create_child(ingress_vsd_acl_entry1)

        ingress_vsd_acl_entry2 = self.vsdk.NUIngressACLEntryTemplate()
        ingress_vsd_acl_entry2.name = "Default Allow TCP"
        ingress_vsd_acl_entry2.description = "Default Allow TCP"
        ingress_vsd_acl_entry2.priority = '1'
        ingress_vsd_acl_entry2.protocol = '6'
        ingress_vsd_acl_entry2.source_port = '*'
        ingress_vsd_acl_entry2.destination_port = '*'
        ingress_vsd_acl_template2.create_child(ingress_vsd_acl_entry2)

        ingress_vsd_acl_entry3 = self.vsdk.NUIngressACLEntryTemplate()
        ingress_vsd_acl_entry3.name = "Default Allow UDP"
        ingress_vsd_acl_entry3.description = "Default Allow UDP"
        ingress_vsd_acl_entry3.priority = '2'
        ingress_vsd_acl_entry3.protocol = '17'
        ingress_vsd_acl_entry3.source_port = '*'
        ingress_vsd_acl_entry3.destination_port = '*'
        ingress_vsd_acl_template2.create_child(ingress_vsd_acl_entry3)
        ingress_vsd_acl_entry4 = self.vsdk.NUIngressACLEntryTemplate()
        ingress_vsd_acl_entry4.name = "Default Allow ICMP"
        ingress_vsd_acl_entry4.description = "Default Allow ICMP"
        ingress_vsd_acl_entry4.priority = '3'
        ingress_vsd_acl_entry4.protocol = '1'
        ingress_vsd_acl_template2.create_child(ingress_vsd_acl_entry4)

        egress_vsd_acl_template1 = self.create_vsd_egress_acl_template(
            domain_template, 'TOP')

        egress_vsd_acl_template2 = self.create_vsd_egress_acl_template(
            domain_template, 'BOTTOM')

        egress_vsd_acl_entry1 = self.vsdk.NUEgressACLEntryTemplate()
        egress_vsd_acl_entry1.name = "Default Intra-Subnet Allow"
        egress_vsd_acl_entry1.description = "Default Intra-Subnet Allow"
        egress_vsd_acl_entry1.priority = '1'
        egress_vsd_acl_entry1.protocol = 'ANY'
        egress_vsd_acl_template1.create_child(egress_vsd_acl_entry1)
        egress_vsd_acl_entry2 = self.vsdk.NUEgressACLEntryTemplate()
        egress_vsd_acl_entry2.name = "Default Allow ICMP"
        egress_vsd_acl_entry2.description = "Default Allow ICMP"
        egress_vsd_acl_entry2.priority = '3'
        egress_vsd_acl_entry2.protocol = '1'
        egress_vsd_acl_template2.create_child(egress_vsd_acl_entry2)

    def create_vsd_domain(self, domain_template, enterprise, name):
        domain = self.vsdk.NUDomain()
        domain.name = name
        domain.description = name
        (domain, connection) = \
            enterprise.instantiate_child(domain, domain_template)
        return domain

    def create_vsd_zone(self, domain, name):
        zone = self.vsdk.NUZone()
        zone.name = name
        zone.description = name
        (zone, connection) = domain.create_child(zone)
        return zone

    def create_vsd_subnet(self, zone, name, cidr):
        subnet = self.vsdk.NUSubnet()
        subnet.name = name
        subnet.description = name
        (subnet.gateway, subnet.netmask, subnet.address) = \
            self._cidr_to_netmask(cidr)
        (subnet, connection) = zone.create_child(subnet)
        return subnet

    def create_vsd_dhcp_option(self, subnet, type, value):
        dhcp_option = self.vsdk.NUDHCPOption()
        dhcp_option.actual_type = type
        dhcp_option.actual_values = value
        (dhcp_option, connection) = subnet.create_child(dhcp_option)
        return dhcp_option

    def _cidr_to_netmask(self, cidr):
        import socket
        import struct
        network, net_bits = cidr.split('/')
        host_bits = 32 - int(net_bits)
        netmask_bits = (1 << 32) - (1 << host_bits)
        netmask = socket.inet_ntoa(struct.pack('!I', netmask_bits))
        network_bits = struct.unpack('!I', socket.inet_aton(network))[0]
        network_masked = socket.inet_ntoa(
            struct.pack('!I', netmask_bits & network_bits)
        )
        return network, netmask, network_masked
