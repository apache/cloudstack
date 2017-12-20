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
from nuageTestCase import nuageTestCase
from marvin.lib.base import (Account,
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

    def verify_pingtovmipaddress(self, ssh, pingtovmipaddress):
        """verify ping to ipaddress of the vm and retry 3 times"""

        if self.isSimulator:
            return

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

    def tearDown(self):
        super(TestNuageManagedSubnets, self).tearDown()
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
        self.debug("Cloudstack Cleanup complete!")
        enterprise = self.fetch_by_externalID(self._session.user.enterprises,
                                              self.domain)
        domain_template = enterprise.domain_templates.get_first()
        domain_template.delete()
        self.debug("VSD Cleanup complete!")
        return

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

        domain3 = self.create_vsd_domain(domain_template, enterprise,
                                         "3rdL3DomainToBeConsumedByACS")
        zone3 = self.create_vsd_zone(domain3, "3rdZoneToBeConsumedByACS")
        subnet3 = self.create_vsd_subnet(zone3, "3rdSubnetToBeConsumedByACS",
                                         "10.2.0.1/24")

        # On ACS create network using non-persistent nw offering allow
        isolated_network = self.create_Network(
                self.nuage_isolated_network_offering,
                gateway="10.0.0.1", netmask="255.255.255.0",
                externalid=subnet1.id)

        # On ACS create network using persistent nw offering allow
        isolated_network2 = self.create_Network(
                self.nuage_isolated_network_offering_persistent,
                gateway="10.1.0.1", netmask="255.255.255.0",
                externalid=subnet2.id)

        try:
            self.create_Network(
                self.nuage_shared_network_offering, gateway="10.2.0.1",
                netmask="255.255.255.0", vlan=1201, externalid=subnet3.id)
        except Exception as e:
            self.debug("Shared Network Creation fails with %s" % e)

        # verify floating ip and intra subnet connectivity
        vm_1 = self.create_VM(isolated_network)
        vm_2 = self.create_VM(isolated_network)
        self.debug("Creating Static NAT rule for the deployed VM in the "
                   "non persistently created Isolated network...")
        public_ip = self.acquire_PublicIPAddress(isolated_network)
        self.validate_PublicIPAddress(public_ip, isolated_network)
        self.create_StaticNatRule_For_VM(vm_1, public_ip, isolated_network)
        self.validate_PublicIPAddress(
                public_ip, isolated_network, static_nat=True, vm=vm_1)
        self.create_FirewallRule(public_ip, self.test_data["ingress_rule"])

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

        vm_3 = self.create_VM(isolated_network2)
        vm_4 = self.create_VM(isolated_network2)
        self.debug("Creating Static NAT rule for the deployed VM in the "
                   "persistently created Isolated network...")
        public_ip2 = self.acquire_PublicIPAddress(isolated_network2)
        self.validate_PublicIPAddress(public_ip2, isolated_network2)
        self.create_StaticNatRule_For_VM(vm_3, public_ip2, isolated_network2)
        self.validate_PublicIPAddress(
                public_ip2, isolated_network2, static_nat=True, vm=vm_3)
        self.create_FirewallRule(public_ip2, self.test_data["ingress_rule"])

        if not self.isSimulator:
            vm_public_ip2 = public_ip2.ipaddress.ipaddress
            try:
                vm_3.ssh_ip = vm_public_ip2
                vm_3.ssh_port = self.test_data["virtual_machine"]["ssh_port"]
                vm_3.username = self.test_data["virtual_machine"]["username"]
                vm_3.password = self.test_data["virtual_machine"]["password"]
                self.debug("SSHing into VM: %s with %s" %
                           (vm_3.ssh_ip, vm_3.password))

                ssh2 = vm_3.get_ssh_client(ipaddress=vm_public_ip2)

            except Exception as e:
                self.fail("SSH into VM failed with exception %s" % e)

            self.verify_pingtovmipaddress(ssh2, vm_4.ipaddress)

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
        self.debug("Creating a VPC tier network with Static NAT service")
        vpc_tier = self.create_Network(self.nuage_vpc_network_offering,
                                       gateway='10.1.0.1',
                                       vpc=vpc,
                                       acl_list=acl_list,
                                       externalid=subnet1.id)
        self.validate_Network(vpc_tier, state="Implemented")
        self.debug("Creating 2nd VPC tier network with Static NAT service")
        vpc_2ndtier = self.create_Network(self.nuage_vpc_network_offering,
                                          gateway='10.1.128.1',
                                          vpc=vpc,
                                          acl_list=acl_list,
                                          externalid=subnet2.id)
        self.validate_Network(vpc_2ndtier, state="Implemented")
        vpc_vr = self.get_Router(vpc_tier)
        self.check_Router_state(vpc_vr, state="Running")

        self.debug("Deploying a VM in the created VPC tier network")
        self.test_data["virtual_machine"]["displayname"] = "vpcvm1"
        self.test_data["virtual_machine"]["name"] = "vpcvm1"
        vpc_vm_1 = self.create_VM(vpc_tier)
        self.check_VM_state(vpc_vm_1, state="Running")
        self.debug("Deploying another VM in the created VPC tier network")
        self.test_data["virtual_machine"]["displayname"] = "vpcvm2"
        self.test_data["virtual_machine"]["name"] = "vpcvm2"
        vpc_vm_2 = self.create_VM(vpc_tier)
        self.check_VM_state(vpc_vm_2, state="Running")
        self.debug("Deploying a VM in the 2nd VPC tier network")
        self.test_data["virtual_machine"]["displayname"] = "vpcvm12"
        self.test_data["virtual_machine"]["name"] = "vpcvm12"
        vpc_vm_12 = self.create_VM(vpc_2ndtier)
        self.check_VM_state(vpc_vm_2, state="Running")
        self.test_data["virtual_machine"]["displayname"] = None
        self.test_data["virtual_machine"]["name"] = None
        self.debug("Creating Static NAT rule for the deployed VM "
                   "in the created VPC network...")
        public_ip_1 = self.acquire_PublicIPAddress(vpc_tier, vpc=vpc)
        self.validate_PublicIPAddress(public_ip_1, vpc_tier)
        self.create_StaticNatRule_For_VM(vpc_vm_1, public_ip_1, vpc_tier)
        self.validate_PublicIPAddress(
                public_ip_1, vpc_tier, static_nat=True, vm=vpc_vm_1)

        if not self.isSimulator:
            vm_public_ip_1 = public_ip_1.ipaddress.ipaddress
            try:
                vpc_vm_1.ssh_ip = vm_public_ip_1
                vpc_vm_1.ssh_port = \
                    self.test_data["virtual_machine"]["ssh_port"]
                vpc_vm_1.username = \
                    self.test_data["virtual_machine"]["username"]
                vpc_vm_1.password = \
                    self.test_data["virtual_machine"]["password"]
                self.debug("SSHing into VM: %s with %s" %
                           (vpc_vm_1.ssh_ip, vpc_vm_1.password))

                ssh = vpc_vm_1.get_ssh_client(ipaddress=vm_public_ip_1)

            except Exception as e:
                self.fail("SSH into VM failed with exception %s" % e)

            self.verify_pingtovmipaddress(ssh, vpc_vm_2.ipaddress)
            self.verify_pingtovmipaddress(ssh, vpc_vm_12.ipaddress)

    def create_vsd_ingress_acl_template(self, domain_template, priority_type="TOP"):
        name = "Ingress ACL " + str(priority_type).capitalize()
        acl_template = self.vsdk.NUIngressACLTemplate()
        acl_template.name = name
        acl_template.description = name
        acl_template.priority_type = priority_type
        acl_template.active = True
        (acl_template, connection) = \
            domain_template.create_child(acl_template)
        return acl_template

    def create_vsd_egress_acl_template(self, domain_template, priority_type='TOP'):
        name = "Egress ACL " + str(priority_type).capitalize()
        acl_template = self.vsdk.NUEgressACLTemplate()
        acl_template.name = name
        acl_template.description = name
        acl_template.priority_type = priority_type
        acl_template.active = True
        (acl_template, connection) = \
            domain_template.create_child(acl_template)
        return acl_template

    def create_vsd_domain_template(self, enterprise):
        domain_template = self.vsdk.NUDomainTemplate()
        domain_template.name = "L3DomainTemplateToBeConsumedByACS"
        domain_template.description = "L3DomainTemplateToBeConsumedByACS"
        domain_template.external_id = "L3DomainTemplateToBeConsumedByACS@" \
                                      + str(self.cms_id)
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
        ingress_vsd_acl_entry1.external_id = "ToBeConsumedByACS@" \
                                             + str(self.cms_id)
        ingress_vsd_acl_entry1.priority = '1'
        ingress_vsd_acl_entry1.protocol = 'ANY'
        ingress_vsd_acl_template1.create_child(ingress_vsd_acl_entry1)

        ingress_vsd_acl_entry2 = self.vsdk.NUIngressACLEntryTemplate()
        ingress_vsd_acl_entry2.name = "Default Allow TCP"
        ingress_vsd_acl_entry2.description = "Default Allow TCP"
        ingress_vsd_acl_entry2.external_id = "ToBeConsumedByACS@" \
                                             + str(self.cms_id)
        ingress_vsd_acl_entry2.priority = '1'
        ingress_vsd_acl_entry2.protocol = '6'
        ingress_vsd_acl_entry2.source_port = '*'
        ingress_vsd_acl_entry2.destination_port = '*'
        ingress_vsd_acl_template2.create_child(ingress_vsd_acl_entry2)

        ingress_vsd_acl_entry3 = self.vsdk.NUIngressACLEntryTemplate()
        ingress_vsd_acl_entry3.name = "Default Allow UDP"
        ingress_vsd_acl_entry3.description = "Default Allow UDP"
        ingress_vsd_acl_entry3.external_id = "ToBeConsumedByACS@" \
                                             + str(self.cms_id)
        ingress_vsd_acl_entry3.priority = '2'
        ingress_vsd_acl_entry3.protocol = '17'
        ingress_vsd_acl_entry3.source_port = '*'
        ingress_vsd_acl_entry3.destination_port = '*'
        ingress_vsd_acl_template2.create_child(ingress_vsd_acl_entry3)
        ingress_vsd_acl_entry4 = self.vsdk.NUIngressACLEntryTemplate()
        ingress_vsd_acl_entry4.name = "Default Allow ICMP"
        ingress_vsd_acl_entry4.description = "Default Allow ICMP"
        ingress_vsd_acl_entry4.external_id = "ToBeConsumedByACS@" \
                                             + str(self.cms_id)
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
        egress_vsd_acl_entry1.external_id = "ToBeConsumedByACS@" \
                                            + str(self.cms_id)
        egress_vsd_acl_entry1.priority = '1'
        egress_vsd_acl_entry1.protocol = 'ANY'
        egress_vsd_acl_template1.create_child(egress_vsd_acl_entry1)
        egress_vsd_acl_entry2 = self.vsdk.NUEgressACLEntryTemplate()
        egress_vsd_acl_entry2.name = "Default Allow ICMP"
        egress_vsd_acl_entry2.description = "Default Allow ICMP"
        egress_vsd_acl_entry2.external_id = "ToBeConsumedByACS@" \
                                            + str(self.cms_id)
        egress_vsd_acl_entry2.priority = '3'
        egress_vsd_acl_entry2.protocol = '1'
        egress_vsd_acl_template2.create_child(egress_vsd_acl_entry2)

    def create_vsd_domain(self, domain_template, enterprise, name):
        domain = self.vsdk.NUDomain()
        domain.name = name
        domain.description = name
        domain.external_id = name + "@" + str(self.cms_id)
        (domain, connection) = \
            enterprise.instantiate_child(domain, domain_template)
        return domain

    def create_vsd_zone(self, domain, name):
        zone = self.vsdk.NUZone()
        zone.name = name
        zone.description = name
        zone.external_id = name + "@" + str(self.cms_id)
        (zone, connection) = domain.create_child(zone)
        return zone

    def create_vsd_subnet(self, zone, name, cidr):
        subnet = self.vsdk.NUSubnet()
        subnet.name = name
        subnet.description = name
        subnet.external_id = name + "@" + str(self.cms_id)
        (subnet.gateway, subnet.netmask, subnet.address) = \
            self._cidr_to_netmask(cidr)
        (subnet, connection) = zone.create_child(subnet)
        return subnet

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
