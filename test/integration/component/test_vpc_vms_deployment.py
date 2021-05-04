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

""" Component tests VM deployment in VPC network functionality
"""
#Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.base import (VirtualMachine,
                                         NetworkOffering,
                                         VpcOffering,
                                         VPC,
                                         NetworkACL,
                                         PrivateGateway,
                                         StaticRoute,
                                         Router,
                                         Network,
                                         Account,
                                         ServiceOffering,
                                         PublicIPAddress,
                                         NATRule,
                                         StaticNATRule,
                                         Configurations)

from marvin.lib.common import (get_domain,
                                           get_zone,
                                           get_template,
                                           wait_for_cleanup,
                                           get_free_vlan)

from marvin.lib.utils import (cleanup_resources, validateList)
from marvin.codes import *
from marvin.cloudstackAPI import rebootRouter



class Services:
    """Test VM deployment in VPC network services
    """

    def __init__(self):
        self.services = {
                         "account": {
                                    "email": "test@test.com",
                                    "firstname": "Test",
                                    "lastname": "User",
                                    "username": "test",
                                    # Random characters are appended for unique
                                    # username
                                    "password": "password",
                                    },
                         "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 100,
                                    "memory": 128,
                                    },
                         "network_offering": {
                                    "name": 'VPC Network offering',
                                    "displaytext": 'VPC Network off',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Lb,UserData,StaticNat,NetworkACL',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "useVpc": 'on',
                                    "serviceProviderList": {
                                            "Dhcp": 'VpcVirtualRouter',
                                            "Dns": 'VpcVirtualRouter',
                                            "SourceNat": 'VpcVirtualRouter',
                                            "PortForwarding": 'VpcVirtualRouter',
                                            "Lb": 'VpcVirtualRouter',
                                            "UserData": 'VpcVirtualRouter',
                                            "StaticNat": 'VpcVirtualRouter',
                                            "NetworkACL": 'VpcVirtualRouter'
                                        },
                                },
                         "network_offering_no_lb": {
                                    "name": 'VPC Network offering',
                                    "displaytext": 'VPC Network off',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,UserData,StaticNat,NetworkACL',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "useVpc": 'on',
                                    "serviceProviderList": {
                                            "Dhcp": 'VpcVirtualRouter',
                                            "Dns": 'VpcVirtualRouter',
                                            "SourceNat": 'VpcVirtualRouter',
                                            "PortForwarding": 'VpcVirtualRouter',
                                            "UserData": 'VpcVirtualRouter',
                                            "StaticNat": 'VpcVirtualRouter',
                                            "NetworkACL": 'VpcVirtualRouter'
                                        },
                                },
                         "vpc_offering": {
                                    "name": 'VPC off',
                                    "displaytext": 'VPC off',
                                    "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Lb,UserData,StaticNat',
                                },
                         "vpc": {
                                 "name": "TestVPC",
                                 "displaytext": "TestVPC",
                                 "cidr": '10.0.0.1/24'
                                 },
                         "network": {
                                  "name": "Test Network",
                                  "displaytext": "Test Network",
                                  "netmask": '255.255.255.0',
                                  "limit": 5,
                                  # Max networks allowed as per hypervisor
                                  # Xenserver -> 5, VMWare -> 9
                                },
                         "natrule": {
                                    "privateport": 22,
                                    "publicport": 22,
                                    "startport": 22,
                                    "endport": 22,
                                    "protocol": "TCP",
                                    "cidrlist": '0.0.0.0/0',
                                },
                         "fw_rule": {
                                    "startport": 1,
                                    "endport": 6000,
                                    "cidr": '0.0.0.0/0',
                                    # Any network (For creating FW rule)
                                    "protocol": "TCP"
                                },
                         "icmp_rule": {
                                    "cidrlist": '0.0.0.0/0',
                                    "protocol": "ICMP"
                                },
                         "virtual_machine": {
                                    "displayname": "Test VM",
                                    "username": "root",
                                    "password": "password",
                                    "ssh_port": 22,
                                    "hypervisor": 'XenServer',
                                    # Hypervisor type should be same as
                                    # hypervisor type of cluster
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": 'TCP',
                                },
                         "ostype": 'CentOS 5.3 (64-bit)',
                         # Cent OS 5.3 (64 bit)
                         "timeout": 10,
                         "mode": 'advanced'
                    }


class TestVMDeployVPC(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVMDeployVPC, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.vpc_off = VpcOffering.create(
                                     cls.api_client,
                                     cls.services["vpc_offering"]
                                     )
        cls.vpc_off.update(cls.api_client, state='Enabled')
        cls._cleanup = [
                        cls.service_offering,
                        cls.vpc_off
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.account = Account.create(
                                     self.apiclient,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )
        self.cleanup = [self.account]
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def validate_vpc_offering(self, vpc_offering):
        """Validates the VPC offering"""

        self.debug("Check if the VPC offering is created successfully?")
        vpc_offs = VpcOffering.list(
                                    self.apiclient,
                                    id=vpc_offering.id
                                    )
        self.assertEqual(
                         isinstance(vpc_offs, list),
                         True,
                         "List VPC offerings should return a valid list"
                         )
        self.assertEqual(
                 vpc_offering.name,
                 vpc_offs[0].name,
                "Name of the VPC offering should match with listVPCOff data"
                )
        self.debug(
                "VPC offering is created successfully - %s" %
                                                        vpc_offering.name)
        return

    def validate_vpc_network(self, network, state=None):
        """Validates the VPC network"""

        self.debug("Check if the VPC network is created successfully?")
        vpc_networks = VPC.list(
                                    self.apiclient,
                                    id=network.id
                          )
        self.assertEqual(
                         isinstance(vpc_networks, list),
                         True,
                         "List VPC network should return a valid list"
                         )
        self.assertEqual(
                 network.name,
                 vpc_networks[0].name,
                "Name of the VPC network should match with listVPC data"
                )
        if state:
            self.assertEqual(
                 vpc_networks[0].state,
                 state,
                "VPC state should be '%s'" % state
                )
        self.debug("VPC network validated - %s" % network.name)
        return

    def acquire_publicip(self, network):
        self.debug("Associating public IP for network: %s" % network.name)
        public_ip = PublicIPAddress.create(self.apiclient,
                                           accountid=self.account.name,
                                           zoneid=self.zone.id,
                                           domainid=self.account.domainid,
                                           networkid=network.id,
                                           vpcid=self.vpc.id
        )
        self.debug("Associated {} with network {}".format(public_ip.ipaddress.ipaddress, network.id))
        return public_ip

    def create_natrule(self, vm, public_ip, network, services=None):
        self.debug("Creating NAT rule in network for vm with public IP")
        if not services:
            services = self.services["natrule"]
        nat_rule = NATRule.create(self.apiclient,
                                  vm,
                                  services,
                                  ipaddressid=public_ip.ipaddress.id,
                                  openfirewall=False,
                                  networkid=network.id,
                                  vpcid=self.vpc.id
        )
        self.debug("Adding NetworkACL rules to make NAT rule accessible")
        nwacl_nat = NetworkACL.create(self.apiclient,
                                      networkid=network.id,
                                      services=services,
                                      traffictype='Ingress'
        )
        self.debug('nwacl_nat=%s' % nwacl_nat.__dict__)
        return nat_rule

    def check_ssh_into_vm(self, vm, public_ip, testnegative=False):
        self.debug("Checking if we can SSH into VM={} on public_ip={}".format(vm.name, public_ip.ipaddress.ipaddress))
        try:
            vm.get_ssh_client(ipaddress=public_ip.ipaddress.ipaddress)
            if not testnegative:
                self.debug("SSH into VM={} on public_ip={} is successful".format(vm.name, public_ip.ipaddress.ipaddress))
            else:
                self.fail("SSH into VM={} on public_ip={} is successful".format(vm.name, public_ip.ipaddress.ipaddress))
        except:
            if not testnegative:
                self.fail("Failed to SSH into VM - %s" % (public_ip.ipaddress.ipaddress))
            else:
                self.debug("Failed to SSH into VM - %s" % (public_ip.ipaddress.ipaddress))

    def deployVM_and_verify_ssh_access(self, network, ip):
        # Spawn an instance in that network
        vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(network.id)],
            ipaddress=ip,
            )
        self.assertIsNotNone(
            vm,
            "Failed to deploy vm with ip address {} and hostname {}".format(ip, self.services["virtual_machine"]["name"])
        )
        vm_response = VirtualMachine.list(
            self.apiclient,
            id=vm.id,
            )
        status = validateList(vm_response)
        self.assertEqual(
            PASS,
            status[0],
            "vm list api returned invalid response after vm {} deployment".format(vm)
        )
        public_ip_1 = self.acquire_publicip(network)
        #ensure vm is accessible over public ip
        nat_rule = self.create_natrule(vm, public_ip_1, network)
        self.check_ssh_into_vm(vm, public_ip_1, testnegative=False)
        #remove the nat rule
        nat_rule.delete(self.apiclient)
        return vm

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_01_deploy_vms_in_network(self):
        """ Test deploy VMs in VPC networks
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a network offering with guest type=Isolated that has all
        #    the supported Services(Vpn,dhcpdns,UserData, SourceNat,Static NAT
        #    and PF,LB,NetworkAcl ) provided by VPCVR and conserver mode is ON
        # 3. Create a network - N1 using the network offering created in step2
        #    as part of this VPC.
        # 4. Create a network - N2 using a network offering similar to the one
        #    created in step2 but without Lb services enabled,as part of VPC
        # 5. Create a network - N3 using the network offering similar to one
        #    created in step2 but without Lb services , as part of this VPC
        # 6. Deploy few vms in all the 3 networks
        # Steps:
        # 1. Delete the 1st network
        # 2. Vms that are part of other network should still be accessible
        #    and in "Running" state.

        self.debug("Creating a VPC offering..")
        vpc_off = VpcOffering.create(
                                     self.apiclient,
                                     self.services["vpc_offering"]
                                     )

        self.validate_vpc_offering(vpc_off)

        self.debug("Enabling the VPC offering created")
        vpc_off.update(self.apiclient, state='Enabled')

        self.debug("creating a VPC network in the account: %s" %
                                                    self.account.name)
        self.services["vpc"]["cidr"] = '10.1.1.1/16'
        vpc = VPC.create(
                         self.apiclient,
                         self.services["vpc"],
                         vpcofferingid=vpc_off.id,
                         zoneid=self.zone.id,
                         account=self.account.name,
                         domainid=self.account.domainid
                         )
        self.validate_vpc_network(vpc)

        nw_off = NetworkOffering.create(
                                            self.apiclient,
                                            self.services["network_offering"],
                                            conservemode=False
                                            )
        # Enable Network offering
        nw_off.update(self.apiclient, state='Enabled')
        self._cleanup.append(nw_off)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" % nw_off.id)
        network_1 = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off.id,
                                zoneid=self.zone.id,
                                gateway='10.1.1.1',
                                vpcid=vpc.id
                                )
        self.debug("Created network with ID: %s" % network_1.id)

        nw_off_no_lb = NetworkOffering.create(
                                    self.apiclient,
                                    self.services["network_offering_no_lb"],
                                    conservemode=False
                                    )
        # Enable Network offering
        nw_off_no_lb.update(self.apiclient, state='Enabled')
        self._cleanup.append(nw_off_no_lb)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    nw_off_no_lb.id)
        network_2 = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off_no_lb.id,
                                zoneid=self.zone.id,
                                gateway='10.1.2.1',
                                vpcid=vpc.id
                                )
        self.debug("Created network with ID: %s" % network_2.id)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    nw_off_no_lb.id)
        network_3 = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off_no_lb.id,
                                zoneid=self.zone.id,
                                gateway='10.1.3.1',
                                vpcid=vpc.id
                                )
        self.debug("Created network with ID: %s" % network_3.id)

        self.debug("deploying VMs in network: %s" % network_1.name)
        # Spawn an instance in that network
        vm_1 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_1.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network_1.id)

        self.debug("deploying VMs in network: %s" % network_2.name)
        # Spawn an instance in that network
        vm_2 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_2.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network_2.id)

        self.debug("deploying VMs in network: %s" % network_3.name)
        vm_3 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_3.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network_3.id)

        self.debug("Check if deployed VMs are in running state?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return a valid response"
                         )
        for vm in vms:
            self.debug("VM name: %s, VM state: %s" % (vm.name, vm.state))
            self.assertEqual(
                             vm.state,
                             "Running",
                             "Vm state should be running for each VM deployed"
                             )
        self.debug("Deleting the virtual machine in network1: %s" %
                                                            network_1.name)
        try:
            vm_1.delete(self.apiclient)
        except Exception as e:
            raise Exception("Failed to delete Virtual machine: %s" % e)

        # Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

        # wait for network.gc to ensure that routers are deleted
        wait_for_cleanup(
                         self.apiclient,
                         ["network.gc.interval", "network.gc.wait"]
                        )

        self.debug("Deleting the network: %s" % network_1.name)
        try:
            network_1.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to deleted network: %s" % e)

        self.debug("After deleting first network other VMs" +
                   "should still be accessible")

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  state="Running",
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return a valid response"
                         )
        self.assertEqual(
                 len(vms),
                 2,
                "Only 2 VMs should be in running state as first nw is deleted"
                )

        for vm in vms:
            self.debug("VM name: %s, VM state: %s" % (vm.name, vm.state))
            self.assertEqual(
                             vm.state,
                             "Running",
                             "Vm state should be running for each VM deployed"
                             )
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_02_deploy_vms_delete_network(self):
        """ Test deploy VMs in VPC networks and delete one of the network
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a network offering with guest type=Isolated that has all
        #    the supported Services(Vpn,dhcpdns,UserData, SourceNat,Static NAT
        #    and PF,LB,NetworkAcl ) provided by VPCVR and conserver mode is ON
        # 3. Create a network - N1 using the network offering created in step2
        #    as part of this VPC.
        # 4. Create a network - N2 using a network offering similar to the one
        #    created in step2 but without Lb services enabled,as part of VPC
        # 5. Create a network - N3 using the network offering similar to one
        #    created in step2 but without Lb services , as part of this VPC
        # 6. Deploy few vms in all the 3 networks
        # Steps:
        # 1. Delete the 2nd network
        # 2. Vms that are part of other network should still be accessible
        #    and in "Running" state.

        self.debug("Creating a VPC offering..")
        vpc_off = VpcOffering.create(
                                     self.apiclient,
                                     self.services["vpc_offering"]
                                     )

        self._cleanup.append(vpc_off)
        self.validate_vpc_offering(vpc_off)

        self.debug("Enabling the VPC offering created")
        vpc_off.update(self.apiclient, state='Enabled')

        self.debug("creating a VPC network in the account: %s" %
                                                    self.account.name)
        self.services["vpc"]["cidr"] = '10.1.1.1/16'
        vpc = VPC.create(
                         self.apiclient,
                         self.services["vpc"],
                         vpcofferingid=vpc_off.id,
                         zoneid=self.zone.id,
                         account=self.account.name,
                         domainid=self.account.domainid
                         )
        self.validate_vpc_network(vpc)

        nw_off = NetworkOffering.create(
                                            self.apiclient,
                                            self.services["network_offering"],
                                            conservemode=False
                                            )
        # Enable Network offering
        nw_off.update(self.apiclient, state='Enabled')
        self._cleanup.append(nw_off)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" % nw_off.id)
        network_1 = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off.id,
                                zoneid=self.zone.id,
                                gateway='10.1.1.1',
                                vpcid=vpc.id
                                )
        self.debug("Created network with ID: %s" % network_1.id)

        nw_off_no_lb = NetworkOffering.create(
                                    self.apiclient,
                                    self.services["network_offering_no_lb"],
                                    conservemode=False
                                    )
        # Enable Network offering
        nw_off_no_lb.update(self.apiclient, state='Enabled')
        self._cleanup.append(nw_off_no_lb)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    nw_off_no_lb.id)
        network_2 = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off_no_lb.id,
                                zoneid=self.zone.id,
                                gateway='10.1.2.1',
                                vpcid=vpc.id
                                )
        self.debug("Created network with ID: %s" % network_2.id)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    nw_off_no_lb.id)
        network_3 = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off_no_lb.id,
                                zoneid=self.zone.id,
                                gateway='10.1.3.1',
                                vpcid=vpc.id
                                )
        self.debug("Created network with ID: %s" % network_3.id)

        self.debug("deploying VMs in network: %s" % network_1.name)
        # Spawn an instance in that network
        vm_1 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_1.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network_1.id)

        self.debug("deploying VMs in network: %s" % network_2.name)
        # Spawn an instance in that network
        vm_2 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_2.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network_2.id)

        self.debug("deploying VMs in network: %s" % network_3.name)
        vm_3 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_3.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network_3.id)

        self.debug("Check if deployed VMs are in running state?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return a valid response"
                         )
        for vm in vms:
            self.debug("VM name: %s, VM state: %s" % (vm.name, vm.state))
            self.assertEqual(
                             vm.state,
                             "Running",
                             "Vm state should be running for each VM deployed"
                             )
        self.debug("Finding the VPC virtual router for network: %s" %
                                                            network_2.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=network_2.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "List routers should retirn a valid router for network2"
                 )
        router = routers[0]

        self.debug("Deleting the virtual machine in network1: %s" %
                                                            network_2.name)
        try:
            vm_2.delete(self.apiclient)
        except Exception as e:
            raise Exception("Failed to delete Virtual machine: %s" % e)

        # Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

        # wait for network.gc to ensure that routers are deleted
        wait_for_cleanup(
                         self.apiclient,
                         ["network.gc.interval", "network.gc.wait"]
                         )

        self.debug("Deleting the network: %s" % network_2.name)
        try:
            network_2.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to deleted network: %s" % e)

        self.debug("Restarting the VPCVR: %s" % router.name)
        cmd = rebootRouter.rebootRouterCmd()
        cmd.id = router.id
        self.apiclient.rebootRouter(cmd)

        self.debug("Check status of router after reboot")
        routers = Router.list(
                              self.apiclient,
                              id=router.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "List routers should retirn a valid router for network2"
                 )
        router = routers[0]
        self.assertEqual(
                         router.state,
                         "Running",
                         "Router state should be running after reboot"
                         )

        self.debug("After deleting first network other VMs" +
                   "should still be accessible")

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  state="Running",
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return a valid response"
                         )
        self.assertEqual(
                 len(vms),
                 2,
                "Only 2 VMs should be in running state as first nw is deleted"
                )

        for vm in vms:
            self.debug("VM name: %s, VM state: %s" % (vm.name, vm.state))
            self.assertEqual(
                             vm.state,
                             "Running",
                             "Vm state should be running for each VM deployed"
                             )
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_03_deploy_vms_delete_add_network(self):
        """ Test deploy VMs, delete one of the network and add another one
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a network offering with guest type=Isolated that has all
        #    the supported Services(Vpn,dhcpdns,UserData, SourceNat,Static NAT
        #    and PF,LB,NetworkAcl ) provided by VPCVR and conserver mode is ON
        # 3. Create a network - N1 using the network offering created in step2
        #    as part of this VPC.
        # 4. Create a network - N2 using a network offering similar to the one
        #    created in step2 but without Lb services enabled,as part of VPC
        # 5. Create a network - N3 using the network offering similar to one
        #    created in step2 but without Lb services , as part of this VPC
        # 6. Deploy few vms in all the 3 networks
        # Steps:
        # 1. Delete the 1st network
        # 2. Add another network in VPC and deploy VM in that network
        # 2. Vms that are part of other network should still be accessible
        #    and in "Running" state.

        self.debug("Creating a VPC offering..")
        vpc_off = VpcOffering.create(
                                     self.apiclient,
                                     self.services["vpc_offering"]
                                     )

        self._cleanup.append(vpc_off)
        self.validate_vpc_offering(vpc_off)

        self.debug("Enabling the VPC offering created")
        vpc_off.update(self.apiclient, state='Enabled')

        self.debug("creating a VPC network in the account: %s" %
                                                    self.account.name)
        self.services["vpc"]["cidr"] = '10.1.1.1/16'
        vpc = VPC.create(
                         self.apiclient,
                         self.services["vpc"],
                         vpcofferingid=vpc_off.id,
                         zoneid=self.zone.id,
                         account=self.account.name,
                         domainid=self.account.domainid
                         )
        self.validate_vpc_network(vpc)

        nw_off = NetworkOffering.create(
                                            self.apiclient,
                                            self.services["network_offering"],
                                            conservemode=False
                                            )
        # Enable Network offering
        nw_off.update(self.apiclient, state='Enabled')
        self._cleanup.append(nw_off)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" % nw_off.id)
        network_1 = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off.id,
                                zoneid=self.zone.id,
                                gateway='10.1.1.1',
                                vpcid=vpc.id
                                )
        self.debug("Created network with ID: %s" % network_1.id)

        nw_off_no_lb = NetworkOffering.create(
                                    self.apiclient,
                                    self.services["network_offering_no_lb"],
                                    conservemode=False
                                    )
        # Enable Network offering
        nw_off_no_lb.update(self.apiclient, state='Enabled')
        self._cleanup.append(nw_off_no_lb)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    nw_off_no_lb.id)
        network_2 = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off_no_lb.id,
                                zoneid=self.zone.id,
                                gateway='10.1.2.1',
                                vpcid=vpc.id
                                )
        self.debug("Created network with ID: %s" % network_2.id)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    nw_off_no_lb.id)
        network_3 = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off_no_lb.id,
                                zoneid=self.zone.id,
                                gateway='10.1.3.1',
                                vpcid=vpc.id
                                )
        self.debug("Created network with ID: %s" % network_3.id)

        self.debug("deploying VMs in network: %s" % network_1.name)
        # Spawn an instance in that network
        vm_1 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_1.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network_1.id)

        self.debug("deploying VMs in network: %s" % network_2.name)
        # Spawn an instance in that network
        vm_2 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_2.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network_2.id)

        self.debug("deploying VMs in network: %s" % network_3.name)
        vm_3 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_3.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network_3.id)

        self.debug("Check if deployed VMs are in running state?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return a valid response"
                         )
        for vm in vms:
            self.debug("VM name: %s, VM state: %s" % (vm.name, vm.state))
            self.assertEqual(
                             vm.state,
                             "Running",
                             "Vm state should be running for each VM deployed"
                             )
        self.debug("Finding the VPC virtual router for network: %s" %
                                                            network_2.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=network_2.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "List routers should retirn a valid router for network2"
                 )
        router = routers[0]

        self.debug("Deleting the virtual machine in network1: %s" %
                                                            network_1.name)
        try:
            vm_1.delete(self.apiclient)
        except Exception as e:
            raise Exception("Failed to delete Virtual machine: %s" % e)

        # Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

        # wait for network.gc to ensure that routers are deleted
        wait_for_cleanup(
                         self.apiclient,
                         ["network.gc.interval", "network.gc.wait"]
                         )

        self.debug("Deleting the network: %s" % network_1.name)
        try:
            network_1.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to deleted network: %s" % e)

        self.debug("Check if the network is deleted or not?")
        networks = Network.list(
                                self.apiclient,
                                id=network_1.id,
                                listall=True
                                )

        self.assertEqual(
                 networks,
                 None,
                "ListNetwork response should be empty as network is deleted"
                )

        self.debug("Create a new netowrk in VPC: %s" % vpc.name)
        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" % nw_off.id)

        network_4 = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off.id,
                                zoneid=self.zone.id,
                                gateway='10.1.4.1',
                                vpcid=vpc.id
                                )

        self.debug("deploying VMs in network: %s" % network_4.name)
        # Spawn an instance in that network
        vm_4 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_4.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network_4.id)

        self.debug("After deleting first network other VMs" +
                   "should still be accessible")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  state="Running",
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return a valid response"
                         )
        self.assertEqual(
                 len(vms),
                 3,
                "Only 2 VMs should be in running state as first nw is deleted"
                )

        for vm in vms:
            self.debug("VM name: %s, VM state: %s" % (vm.name, vm.state))
            self.assertEqual(
                             vm.state,
                             "Running",
                             "Vm state should be running for each VM deployed"
                             )
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_04_deploy_vms_delete_add_network_noLb(self):
        """ Test deploy VMs, delete one network without LB and add another one
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a network offering with guest type=Isolated that has all
        #    the supported Services(Vpn,dhcpdns,UserData, SourceNat,Static NAT
        #    and PF,LB,NetworkAcl ) provided by VPCVR and conserver mode is ON
        # 3. Create a network - N1 using the network offering created in step2
        #    as part of this VPC.
        # 4. Create a network - N2 using a network offering similar to the one
        #    created in step2 but without Lb services enabled,as part of VPC
        # 5. Create a network - N3 using the network offering similar to one
        #    created in step2 but without Lb services , as part of this VPC
        # 6. Deploy few vms in all the 3 networks
        # Steps:
        # 1. Delete the 2nd network
        # 2. Add another network in VPC and deploy VM in that network
        # 2. Vms that are part of other network should still be accessible
        #    and in "Running" state.

        self.debug("Creating a VPC offering..")
        vpc_off = VpcOffering.create(
                                     self.apiclient,
                                     self.services["vpc_offering"]
                                     )

        self._cleanup.append(vpc_off)
        self.validate_vpc_offering(vpc_off)

        self.debug("Enabling the VPC offering created")
        vpc_off.update(self.apiclient, state='Enabled')

        self.debug("creating a VPC network in the account: %s" %
                                                    self.account.name)
        self.services["vpc"]["cidr"] = '10.1.1.1/16'
        vpc = VPC.create(
                         self.apiclient,
                         self.services["vpc"],
                         vpcofferingid=vpc_off.id,
                         zoneid=self.zone.id,
                         account=self.account.name,
                         domainid=self.account.domainid
                         )
        self.validate_vpc_network(vpc)

        nw_off = NetworkOffering.create(
                                            self.apiclient,
                                            self.services["network_offering"],
                                            conservemode=False
                                            )
        # Enable Network offering
        nw_off.update(self.apiclient, state='Enabled')
        self._cleanup.append(nw_off)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" % nw_off.id)
        network_1 = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off.id,
                                zoneid=self.zone.id,
                                gateway='10.1.1.1',
                                vpcid=vpc.id
                                )
        self.debug("Created network with ID: %s" % network_1.id)

        nw_off_no_lb = NetworkOffering.create(
                                    self.apiclient,
                                    self.services["network_offering_no_lb"],
                                    conservemode=False
                                    )
        # Enable Network offering
        nw_off_no_lb.update(self.apiclient, state='Enabled')
        self._cleanup.append(nw_off_no_lb)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    nw_off_no_lb.id)
        network_2 = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off_no_lb.id,
                                zoneid=self.zone.id,
                                gateway='10.1.2.1',
                                vpcid=vpc.id
                                )
        self.debug("Created network with ID: %s" % network_2.id)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    nw_off_no_lb.id)
        network_3 = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off_no_lb.id,
                                zoneid=self.zone.id,
                                gateway='10.1.3.1',
                                vpcid=vpc.id
                                )
        self.debug("Created network with ID: %s" % network_3.id)

        self.debug("deploying VMs in network: %s" % network_1.name)
        # Spawn an instance in that network
        vm_1 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_1.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network_1.id)

        self.debug("deploying VMs in network: %s" % network_2.name)
        # Spawn an instance in that network
        vm_2 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_2.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network_2.id)

        self.debug("deploying VMs in network: %s" % network_3.name)
        vm_3 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_3.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network_3.id)

        self.debug("Check if deployed VMs are in running state?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return a valid response"
                         )
        for vm in vms:
            self.debug("VM name: %s, VM state: %s" % (vm.name, vm.state))
            self.assertEqual(
                             vm.state,
                             "Running",
                             "Vm state should be running for each VM deployed"
                             )
        self.debug("Finding the VPC virtual router for network: %s" %
                                                            network_2.name)

        routers = Router.list(
                              self.apiclient,
                              networkid=network_2.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "List routers should retirn a valid router for network2"
                 )
        router = routers[0]

        self.debug("Deleting the virtual machine in network1: %s" %
                                                            network_1.name)
        try:
            vm_1.delete(self.apiclient)
        except Exception as e:
            raise Exception("Failed to delete Virtual machine: %s" % e)

        # Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

        # wait for network.gc to ensure that routers are deleted
        wait_for_cleanup(
                         self.apiclient,
                         ["network.gc.interval", "network.gc.wait"]
                         )

        self.debug("Deleting the network: %s" % network_1.name)
        try:
            network_1.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to deleted network: %s" % e)

        self.debug("Check if the network is deleted or not?")
        networks = Network.list(
                                self.apiclient,
                                id=network_1.id,
                                listall=True
                                )

        self.assertEqual(
                 networks,
                 None,
                "ListNetwork response should be empty as network is deleted"
                )

        self.debug("Create a new netowrk in VPC: %s" % vpc.name)
        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                        nw_off_no_lb.id)

        network_4 = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off_no_lb.id,
                                zoneid=self.zone.id,
                                gateway='10.1.4.1',
                                vpcid=vpc.id
                                )

        self.debug("deploying VMs in network: %s" % network_4.name)
        # Spawn an instance in that network
        vm_4 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_4.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network_4.id)

        self.debug("Restarting the VPCVR: %s" % router.name)
        cmd = rebootRouter.rebootRouterCmd()
        cmd.id = router.id
        self.apiclient.rebootRouter(cmd)

        self.debug("Check status of router after reboot")
        routers = Router.list(
                              self.apiclient,
                              id=router.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "List routers should retirn a valid router for network2"
                 )
        router = routers[0]
        self.assertEqual(
                         router.state,
                         "Running",
                         "Router state should be running after reboot"
                         )

        self.debug("After deleting first network other VMs" +
                   "should still be accessible")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  state="Running",
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return a valid response"
                         )
        self.assertEqual(
                 len(vms),
                 3,
                "Only 2 VMs should be in running state as first nw is deleted"
                )

        for vm in vms:
            self.debug("VM name: %s, VM state: %s" % (vm.name, vm.state))
            self.assertEqual(
                             vm.state,
                             "Running",
                             "Vm state should be running for each VM deployed"
                             )
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_05_create_network_max_limit(self):
        """ Test create networks in VPC upto maximum limit for hypervisor
        """

        # Validate the following
        # 1. Create a VPC and add maximum # of supported networks to the VPC.
        # 2. Deploy Vms in each of these networks.

        self.debug("Creating a VPC offering..")
        vpc_off = VpcOffering.create(
                                     self.apiclient,
                                     self.services["vpc_offering"]
                                     )

        self._cleanup.append(vpc_off)
        self.validate_vpc_offering(vpc_off)

        self.debug("Enabling the VPC offering created")
        vpc_off.update(self.apiclient, state='Enabled')

        self.debug("creating a VPC network in the account: %s" %
                                                    self.account.name)
        self.services["vpc"]["cidr"] = '10.1.1.1/16'
        vpc = VPC.create(
                         self.apiclient,
                         self.services["vpc"],
                         vpcofferingid=vpc_off.id,
                         zoneid=self.zone.id,
                         account=self.account.name,
                         domainid=self.account.domainid
                         )
        self.validate_vpc_network(vpc)

        nw_off = NetworkOffering.create(
                                            self.apiclient,
                                            self.services["network_offering"],
                                            conservemode=False
                                            )
        # Enable Network offering
        nw_off.update(self.apiclient, state='Enabled')
        self._cleanup.append(nw_off)

        # Empty list to store all of the network and VM elements
        networks = []
        vms = []

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" % nw_off.id)
        network_1 = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off.id,
                                zoneid=self.zone.id,
                                gateway='10.1.0.1',
                                vpcid=vpc.id
                                )
        self.debug("Created network with ID: %s" % network_1.id)

        nw_off_no_lb = NetworkOffering.create(
                                    self.apiclient,
                                    self.services["network_offering_no_lb"],
                                    conservemode=False
                                    )
        # Enable Network offering
        nw_off_no_lb.update(self.apiclient, state='Enabled')
        self._cleanup.append(nw_off_no_lb)

        configs = Configurations.list(
                                self.apiclient,
                                name='vpc.max.networks',
                                listall=True
                            )
        if not isinstance(configs, list):
            raise Exception("Failed to find max network allowed for VPC")

        self.services["network"]["limit"] = int(configs[0].value)

        # Create networks till max limit of hypervisor
        for i in range(self.services["network"]["limit"] - 1):
            # Creating network using the network offering created
            self.debug("Creating network with network offering: %s" %
                                                    nw_off_no_lb.id)
            gateway = '10.1.' + str(i + 1) + '.1'
            self.debug("Gateway for new network: %s" % gateway)

            network = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off_no_lb.id,
                                zoneid=self.zone.id,
                                gateway=gateway,
                                vpcid=vpc.id
                                )
            self.debug("Created network with ID: %s" % network.id)
            networks.append(network)

        self.debug(
        "Trying to create one more network than limit in VPC: %s" % vpc.name)
        gateway = '10.1.' + str(self.services["network"]["limit"]) + '.1'

        with self.assertRaises(Exception):
            Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off_no_lb.id,
                                zoneid=self.zone.id,
                                gateway=gateway,
                                vpcid=vpc.id
                                )

        self.debug("Deleting one of the existing networks")
        try:
            network_1.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete network: %s - %s" %
                                                    (network_1.name, e))

        self.debug("Creating a new network in VPC: %s" % vpc.name)
        network = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off.id,
                                zoneid=self.zone.id,
                                gateway=gateway,
                                vpcid=vpc.id
                                )
        self.debug("Created a new network: %s" % network.name)
        networks.append(network)

        self.debug("Deploying VMs in each of the networks created in VPC")
        for network in networks:
            self.debug("deploying VMs in network: %s" % network.name)
            # Spawn an instance in that network
            vm = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network.id)]
                                  )
            self.debug("Deployed VM in network: %s" % network.id)
            vms.append(vm)

            self.debug("Check if VM deployed successfully or not?")
            list_vms = VirtualMachine.list(
                                           self.apiclient,
                                           id=vm.id,
                                           listall=True
                                           )
            self.assertEqual(
                             isinstance(list_vms, list),
                             True,
                             "List VMs should return a valid response"
                             )
            self.assertEqual(
                             list_vms[0].state,
                             "Running",
                             "Vm should be in running state"
                             )
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_06_delete_network_vm_running(self):
        """ Test delete network having running instances in VPC
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1(10.1.1.1/24) and network2(10.1.2.1/24) to this VPC.
        # 3. Deploy vm1 and vm2 in network1 and vm3 and vm4 in network2.
        # 4. Attempt to delete network1. Delete network should fail.
        # 5. Destroy all Vms in network1 & wait for the Vms to get expunged
        # 6. Attempt to delete network1. Delete network shall succeed

        self.debug("Creating a VPC offering..")
        vpc_off = VpcOffering.create(
                                     self.apiclient,
                                     self.services["vpc_offering"]
                                     )

        self._cleanup.append(vpc_off)
        self.validate_vpc_offering(vpc_off)

        self.debug("Enabling the VPC offering created")
        vpc_off.update(self.apiclient, state='Enabled')

        self.debug("creating a VPC network in the account: %s" %
                                                    self.account.name)
        self.services["vpc"]["cidr"] = '10.1.1.1/16'
        vpc = VPC.create(
                         self.apiclient,
                         self.services["vpc"],
                         vpcofferingid=vpc_off.id,
                         zoneid=self.zone.id,
                         account=self.account.name,
                         domainid=self.account.domainid
                         )
        self.validate_vpc_network(vpc)

        nw_off = NetworkOffering.create(
                                            self.apiclient,
                                            self.services["network_offering"],
                                            conservemode=False
                                            )
        # Enable Network offering
        nw_off.update(self.apiclient, state='Enabled')
        self._cleanup.append(nw_off)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" % nw_off.id)
        network_1 = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off.id,
                                zoneid=self.zone.id,
                                gateway='10.1.1.1',
                                vpcid=vpc.id
                                )
        self.debug("Created network with ID: %s" % network_1.id)

        nw_off_no_lb = NetworkOffering.create(
                                    self.apiclient,
                                    self.services["network_offering_no_lb"],
                                    conservemode=False
                                    )
        # Enable Network offering
        nw_off_no_lb.update(self.apiclient, state='Enabled')
        self._cleanup.append(nw_off_no_lb)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    nw_off_no_lb.id)
        network_2 = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off_no_lb.id,
                                zoneid=self.zone.id,
                                gateway='10.1.2.1',
                                vpcid=vpc.id
                                )
        self.debug("Created network with ID: %s" % network_2.id)

        self.debug("deploying VMs in network: %s" % network_1.name)
        # Spawn an instance in that network
        vm_1 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_1.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network_1.id)
        vm_2 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_1.id)]
                                  )
        self.debug("Deployed another VM in network: %s" % network_1.id)

        self.debug("deploying VMs in network: %s" % network_2.name)
        # Spawn an instance in that network
        vm_3 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_2.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network_2.id)
        vm_4 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_2.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network_2.id)

        self.debug("Check if deployed VMs are in running state?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return a valid response"
                         )
        for vm in vms:
            self.debug("VM name: %s, VM state: %s" % (vm.name, vm.state))
            self.assertEqual(
                             vm.state,
                             "Running",
                             "Vm state should be running for each VM deployed"
                             )
        self.debug("Trying to delete network: %s" % network_1.name)
        with self.assertRaises(Exception):
            network_1.delete(self.apiclient)
        self.debug("Delete netwpork failed as there are running instances")

        self.debug("Destroying all the instances in network1: %s" %
                                                            network_1.name)
        try:
            vm_1.delete(self.apiclient)
            vm_2.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to destroy virtual machines - %s" % e)

        # Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

        # wait for network.gc to ensure that routers are deleted
        wait_for_cleanup(
                         self.apiclient,
                         ["network.gc.interval", "network.gc.wait"]
                         )

        self.debug("List virtual machines to ensure that VMs are expunged")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  networkid=network_1.id,
                                  listall=True
                                  )
        self.assertEqual(
                          vms,
                          None,
                          "List Vms shall return an empty response"
                          )
        self.debug("Trying to delete network again now (should succeed)..")
        try:
            network_1.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete network: %s, %s" % (network_1.name, e))

        self.debug("Destroying all the instances in network1: %s" %
                                                            network_2.name)
        try:
            vm_3.delete(self.apiclient)
            vm_4.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to destroy virtual machines - %s" % e)

        # Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

        # wait for network.gc to ensure that routers are deleted
        wait_for_cleanup(
                         self.apiclient,
                         ["network.gc.interval", "network.gc.wait"])

        self.debug("List virtual machines to ensure that VMs are expunged")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  networkid=network_2.id,
                                  listall=True
                                  )
        self.assertEqual(
                          vms,
                          None,
                          "List Vms shall return an empty response"
                          )
        self.debug("Trying to delete network again now (should succeed)..")
        try:
            network_2.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete network: %s, %s" % (network_2.name, e))

        self.debug("Virtual router should be in running state")
        routers = Router.list(
                              self.apiclient,
                              account=self.account.name,
                              domainid=self.account.domainid,
                              listall=True
                              )
        self.assertEqual(
                         isinstance(routers, list),
                         True,
                         "List routers shall not return an emptty response"
                         )
        for router in routers:
            self.assertEqual(
                             router.state,
                             "Running",
                             "Router state should be running"
                             )
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_07_delete_network_with_rules(self):
        """ Test delete network that has PF/staticNat/LB rules/Network Acl
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1(10.1.1.1/24) and network2(10.1.2.1/24) to this VPC.
        # 3. Deploy vm1 and vm2 in network1 and vm3 and vm4 in network2.
        # 4. Create a PF /Static Nat/LB rule for vms in network1.
        # 5. Create a PF /Static Nat/LB rule for vms in network2.
        # 6. Create ingress network ACL for allowing all the above rules from
        #    public ip range on network1 and network2.
        # 7. Create egress network ACL for network1 and network2 to access
        #    google.com.
        # 8. Create a private gateway for this VPC and add a static route to
        #    this gateway
        # 9. Create a VPN gateway for this VPC and add a static route to this
        #    gateway.
        # 10. Make sure that all the PF,LB, Static NAT rules work as expected
        # 11. Make sure that we are able to access google from all user Vms
        # 12. Make sure that the newly added private gateway's and VPN
        #    gateway's static routes work as expected.
        # Steps:
        # 1. Delete the 1st network.
        # 2. Delete account
        # Validations:
        # 1. As part of network deletion all the resources attached with
        #    network should get deleted. All other VMs and rules shall work as
        #    expected
        # 2. All the resources associated with account should be deleted

        self.debug("Creating a VPC offering..")
        vpc_off = VpcOffering.create(
                                     self.apiclient,
                                     self.services["vpc_offering"]
                                     )

        self._cleanup.append(vpc_off)
        self.validate_vpc_offering(vpc_off)

        self.debug("Enabling the VPC offering created")
        vpc_off.update(self.apiclient, state='Enabled')

        self.debug("creating a VPC network in the account: %s" %
                                                    self.account.name)
        self.services["vpc"]["cidr"] = '10.1.1.1/16'
        vpc = VPC.create(
                         self.apiclient,
                         self.services["vpc"],
                         vpcofferingid=vpc_off.id,
                         zoneid=self.zone.id,
                         account=self.account.name,
                         domainid=self.account.domainid
                         )
        self.validate_vpc_network(vpc)

        nw_off = NetworkOffering.create(
                                            self.apiclient,
                                            self.services["network_offering"],
                                            conservemode=False
                                            )
        # Enable Network offering
        nw_off.update(self.apiclient, state='Enabled')
        self._cleanup.append(nw_off)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" % nw_off.id)
        network_1 = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off.id,
                                zoneid=self.zone.id,
                                gateway='10.1.1.1',
                                vpcid=vpc.id
                                )
        self.debug("Created network with ID: %s" % network_1.id)

        nw_off_no_lb = NetworkOffering.create(
                                    self.apiclient,
                                    self.services["network_offering_no_lb"],
                                    conservemode=False
                                    )
        # Enable Network offering
        nw_off_no_lb.update(self.apiclient, state='Enabled')
        self._cleanup.append(nw_off_no_lb)

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    nw_off_no_lb.id)
        network_2 = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=nw_off_no_lb.id,
                                zoneid=self.zone.id,
                                gateway='10.1.2.1',
                                vpcid=vpc.id
                                )
        self.debug("Created network with ID: %s" % network_2.id)

        self.debug("deploying VMs in network: %s" % network_1.name)
        # Spawn an instance in that network
        vm_1 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_1.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network_1.id)
        vm_2 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_1.id)]
                                  )
        self.debug("Deployed another VM in network: %s" % network_1.id)

        self.debug("deploying VMs in network: %s" % network_2.name)
        # Spawn an instance in that network
        vm_3 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_2.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network_2.id)
        vm_4 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network_2.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network_2.id)

        self.debug("Check if deployed VMs are in running state?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return a valid response"
                         )
        for vm in vms:
            self.debug("VM name: %s, VM state: %s" % (vm.name, vm.state))
            self.assertEqual(
                             vm.state,
                             "Running",
                             "Vm state should be running for each VM deployed"
                             )

        self.debug("Associating public IP for network: %s" % network_1.name)
        public_ip_1 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network_1.id,
                                vpcid=vpc.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_1.ipaddress.ipaddress,
                                        network_1.id
                                        ))

        NATRule.create(
                       self.apiclient,
                       vm_1,
                       self.services["natrule"],
                       ipaddressid=public_ip_1.ipaddress.id,
                       openfirewall=False,
                       networkid=network_1.id,
                       vpcid=vpc.id
                       )

        self.debug("Associating public IP for network: %s" % network_1.name)
        public_ip_2 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network_1.id,
                                vpcid=vpc.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_2.ipaddress.ipaddress,
                                        network_1.id
                                        ))
        self.debug("Enabling static NAT for IP: %s" %
                                            public_ip_2.ipaddress.ipaddress)
        try:
            StaticNATRule.enable(
                              self.apiclient,
                              ipaddressid=public_ip_2.ipaddress.id,
                              virtualmachineid=vm_2.id,
                              networkid=network_1.id
                              )
            self.debug("Static NAT enabled for IP: %s" %
                                            public_ip_2.ipaddress.ipaddress)
        except Exception as e:
            self.fail("Failed to enable static NAT on IP: %s - %s" % (
                                        public_ip_2.ipaddress.ipaddress, e))

        public_ips = PublicIPAddress.list(
                                    self.apiclient,
                                    networkid=network_1.id,
                                    listall=True,
                                    isstaticnat=True,
                                    account=self.account.name,
                                    domainid=self.account.domainid
                                  )
        self.assertEqual(
                         isinstance(public_ips, list),
                         True,
                         "List public Ip for network should list the Ip addr"
                         )
        self.assertEqual(
                         public_ips[0].ipaddress,
                         public_ip_2.ipaddress.ipaddress,
                         "List public Ip for network should list the Ip addr"
                         )

        self.debug("Associating public IP for network: %s" % vpc.name)
        public_ip_3 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network_2.id,
                                vpcid=vpc.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_3.ipaddress.ipaddress,
                                        network_2.id
                                        ))

        NATRule.create(
                                  self.apiclient,
                                  vm_3,
                                  self.services["natrule"],
                                  ipaddressid=public_ip_3.ipaddress.id,
                                  openfirewall=False,
                                  networkid=network_2.id,
                                  vpcid=vpc.id
                                  )

        self.debug("Associating public IP for network: %s" % network_2.name)
        public_ip_4 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network_2.id,
                                vpcid=vpc.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_4.ipaddress.ipaddress,
                                        network_2.id
                                        ))
        self.debug("Enabling static NAT for IP: %s" %
                                            public_ip_4.ipaddress.ipaddress)
        try:
            StaticNATRule.enable(
                              self.apiclient,
                              ipaddressid=public_ip_4.ipaddress.id,
                              virtualmachineid=vm_3.id,
                              networkid=network_2.id
                              )
            self.debug("Static NAT enabled for IP: %s" %
                                            public_ip_4.ipaddress.ipaddress)
        except Exception as e:
            self.fail("Failed to enable static NAT on IP: %s - %s" % (
                                        public_ip_4.ipaddress.ipaddress, e))

        self.debug("Adding NetwrokACl rules to make NAT rule accessible with network %s" % network_1.id)
        NetworkACL.create(
                                         self.apiclient,
                                         networkid=network_1.id,
                                         services=self.services["natrule"],
                                         traffictype='Ingress'
                                         )

        self.debug("Adding NetworkACl rules to make NAT rule accessible with network: %s" % network_2.id)
        NetworkACL.create(
                                         self.apiclient,
                                         networkid=network_2.id,
                                         services=self.services["natrule"],
                                         traffictype='Ingress'
                                         )

        self.debug(
            "Adding Egress rules to network to allow access to internet")
        NetworkACL.create(
                                self.apiclient,
                                networkid=network_1.id,
                                services=self.services["icmp_rule"],
                                traffictype='Egress'
                                )
        NetworkACL.create(
                                self.apiclient,
                                networkid=network_2.id,
                                services=self.services["icmp_rule"],
                                traffictype='Egress'
                                )

        vlan = get_free_vlan(self.apiclient, self.zone.id)[1]
        if vlan is None:
            self.fail("Failed to get free vlan id in the zone")

        self.debug("Creating private gateway in VPC: %s" % vpc.name)
        private_gateway = PrivateGateway.create(
                                                self.apiclient,
                                                gateway='10.2.3.1',
                                                ipaddress='10.2.3.2',
                                                netmask='255.255.255.0',
                                                vlan=vlan,
                                                vpcid=vpc.id
                                                )
        self.debug("Check if the private gateway created successfully?")
        gateways = PrivateGateway.list(
                                       self.apiclient,
                                       id=private_gateway.id,
                                       listall=True
                                       )
        self.assertEqual(
                        isinstance(gateways, list),
                        True,
                        "List private gateways should return a valid response"
                        )
        self.debug("Creating static route for this gateway")
        static_route = StaticRoute.create(
                                          self.apiclient,
                                          cidr='10.2.3.0/24',
                                          gatewayid=private_gateway.id
                                          )
        self.debug("Check if the static route created successfully?")
        static_routes = StaticRoute.list(
                                       self.apiclient,
                                       id=static_route.id,
                                       listall=True
                                       )
        self.assertEqual(
                        isinstance(static_routes, list),
                        True,
                        "List static route should return a valid response"
                        )

        self.debug("Restaring the network 1 (%s) with cleanup=True" %
                                                            network_1.name)
        try:
            network_1.restart(self.apiclient, cleanup=True)
        except Exception as e:
            self.fail(
                "Failed to restart network: %s, %s" %
                                                        (network_1.name, e))

        self.debug("Checking if we can SSH into VM_1?")
        try:
            ssh_1 = vm_1.get_ssh_client(
                                ipaddress=public_ip_1.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["natrule"]["publicport"]
                                )
            self.debug("SSH into VM is successfully")

            self.debug("Verifying if we can ping to outside world from VM?")
            # Ping to outsite world
            res = ssh_1.execute("ping -c 1 www.google.com")
            # res = 64 bytes from maa03s17-in-f20.1e100.net (74.125.236.212):
            # icmp_req=1 ttl=57 time=25.9 ms
            # --- www.l.google.com ping statistics ---
            # 1 packets transmitted, 1 received, 0% packet loss, time 0ms
            # rtt min/avg/max/mdev = 25.970/25.970/25.970/0.000 ms
        except Exception as e:
            self.fail("Failed to SSH into VM - %s, %s" %
                                        (public_ip_1.ipaddress.ipaddress, e))

        result = str(res)
        self.debug("result = %s, result.count = %s" % (result, result.count("1 received")))
        self.debug("Public IP = %s" % public_ip_1.ipaddress.ipaddress)
        self.assertEqual(
                         result.count("1 received"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )

        self.debug("Checking if we can SSH into VM_2?")
        try:
            ssh_2 = vm_2.get_ssh_client(
                                ipaddress=public_ip_2.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["natrule"]["publicport"]
                                )
            self.debug("SSH into VM is successfully")

            self.debug("Verifying if we can ping to outside world from VM?")
            res = ssh_2.execute("ping -c 1 www.google.com")
        except Exception as e:
            self.fail("Failed to SSH into VM - %s, %s" %
                                        (public_ip_2.ipaddress.ipaddress, e))

        result = str(res)
        self.assertEqual(
                         result.count("1 received"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )

        self.debug("Trying to delete network: %s" % network_1.name)
        with self.assertRaises(Exception):
            network_1.delete(self.apiclient)
        self.debug("Delete network failed as there are running instances")

        self.debug("Destroying all the instances in network1: %s" %
                                                            network_1.name)
        try:
            vm_1.delete(self.apiclient)
            vm_2.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to destroy virtual machines - %s" % e)

        # Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

        # wait for network.gc to ensure that routers are deleted
        wait_for_cleanup(
                         self.apiclient,
                         ["network.gc.interval", "network.gc.wait"]
                         )

        self.debug("List virtual machines to ensure that VMs are expunged")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  networkid=network_1.id,
                                  listall=True
                                  )
        self.assertEqual(
                          vms,
                          None,
                          "List Vms shall return an empty response"
                          )
        self.debug("Trying to delete network again now (should succeed)..")
        try:
            network_1.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete network: %s, %s" % (network_1.name, e))

        self.debug("Restaring the network 2 (%s) with cleanup=True" %
                                                            network_2.name)
        try:
            network_2.restart(self.apiclient, cleanup=True)
        except Exception as e:
            self.fail(
                "Failed to restart network: %s, %s" %
                                                        (network_2.name, e))

        self.debug("Checking if we can SSH into VM_3?")
        try:
            ssh_3 = vm_3.get_ssh_client(
                                ipaddress=public_ip_3.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["natrule"]["publicport"]
                                )
            self.debug("SSH into VM is successfully")

            self.debug("Verifying if we can ping to outside world from VM?")
            # Ping to outsite world
            res = ssh_3.execute("ping -c 1 www.google.com")
            # res = 64 bytes from maa03s17-in-f20.1e100.net (74.125.236.212):
            # icmp_req=1 ttl=57 time=25.9 ms
            # --- www.l.google.com ping statistics ---
            # 1 packets transmitted, 1 received, 0% packet loss, time 0ms
            # rtt min/avg/max/mdev = 25.970/25.970/25.970/0.000 ms
        except Exception as e:
            self.fail("Failed to SSH into VM - %s, %s" %
                                        (public_ip_3.ipaddress.ipaddress, e))

        result = str(res)
        self.assertEqual(
                         result.count("1 received"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )

        self.debug("Checking if we can SSH into VM_4?")
        try:
            ssh_4 = vm_4.get_ssh_client(
                                ipaddress=public_ip_4.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["natrule"]["publicport"]
                                )
            self.debug("SSH into VM is successfully")

            self.debug("Verifying if we can ping to outside world from VM?")
            res = ssh_4.execute("ping -c 1 www.google.com")
        except Exception as e:
            self.fail("Failed to SSH into VM - %s, %s" %
                                        (public_ip_4.ipaddress.ipaddress, e))

        result = str(res)
        self.assertEqual(
                         result.count("1 received"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )

        self.debug("Deleting the account..")
        try:
            self.account.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete account: %s" %
                                                self.account.name)
        wait_for_cleanup(self.apiclient, ["account.cleanup.interval"])

        # Remove account from cleanup list, we've already deleted it
        self.cleanup.remove(self.account)

        self.debug("Check if the VPC network is created successfully?")
        vpc_networks = VPC.list(
                                    self.apiclient,
                                    id=vpc.id
                          )
        self.assertEqual(
                         vpc_networks,
                         None,
                         "List VPC network should not return a valid list"
                         )

        self.debug("Trying to list the networks in the account, this should fail as account does not exist now")
        with self.assertRaises(Exception):
            Network.list(
                         self.apiclient,
                         account=self.account.name,
                         domainid=self.account.domainid
                         )
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_08_ip_reallocation_CS5986(self):
        """
        @Desc: Test to verify dnsmasq dhcp conflict issue due to /ect/hosts not getting udpated
	    @Steps:
	    Step1: Create a VPC
        Step2: Create one network in vpc
        Step3: Deploy vm1 with hostname hostA and ip address IP A in the above network
        Step4: List the vm and verify the ip address in the response and verify ssh access to vm
        Step5: Deploy vm2 with hostname hostB and ip address IP B in the same network
        Step6: Repeat step4
        Step7: Destroy vm1 and vm2
        Step8: Deploy vm3 with hostname hostA and ip address IP B
        Step9: Repeat step4
        Step10: Deploy vm4 with IP A and hostC
        Step11: Repeat step4
        """

        self.debug("creating a VPC network in the account: %s" % self.account.name)
        self.services["vpc"]["cidr"] = '10.1.1.1/16'
        self.vpc = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.validate_vpc_network(self.vpc)
        self.nw_off = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        self.nw_off.update(self.apiclient, state='Enabled')
        self._cleanup.append(self.nw_off)
        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" % self.nw_off.id)
        network_1 = Network.create(
            self.apiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.nw_off.id,
            zoneid=self.zone.id,
            gateway='10.1.1.1',
            vpcid=self.vpc.id
        )
        self.debug("Created network with ID: %s" % network_1.id)
        # Spawn vm1 in that network
        vm1_ip = "10.1.1.10"
        name1 = "hostA"
        self.services["virtual_machine"]["name"] = name1
        vm1 = self.deployVM_and_verify_ssh_access(network_1, vm1_ip)
        #Deploy vm2 with host name "hostB" and ip address "10.1.1.20"
        vm2_ip = "10.1.1.20"
        name2 = "hostB"
        self.services["virtual_machine"]["name"] = name2
        vm2 = self.deployVM_and_verify_ssh_access(network_1, vm2_ip)
        #Destroy both the vms
        try:
            vm1.delete(self.apiclient, expunge=True)
            vm2.delete(self.apiclient, expunge=True)
        except Exception as e:
            raise Exception("Warning: Exception in expunging vms: %s" % e)
        """
        Deploy vm3 with ip address of vm1 and host name of vm2 so both the vm1 and vm2 entries
        would be deleted from dhcphosts file on VR becase dhcprelease matches entries with
        host name and ip address so it matches both the entries.
        """
        # Deploying a VM3 with ip1 and name2
        self.services["virtual_machine"]["name"] = name2
        vm3 = self.deployVM_and_verify_ssh_access(network_1, vm1_ip)
        #Deploy 4th vm
        """
        Deploy vm4 with ip address of vm2. dnsmasq and dhcprelase should be in sync.
    	We should not see dhcp lease block due to IP reallocation.
     	"""
        name3 = "hostC"
        self.services["virtual_machine"]["name"] = name3
        vm4 = self.deployVM_and_verify_ssh_access(network_1, vm2_ip)
        try:
            vm3.delete(self.apiclient, expunge=True)
            vm4.delete(self.apiclient, expunge=True)
        except Exception as e:
            raise Exception("Warning: Excepting in expunging vms vm3 and vm4:  %s" % e)
        return



