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

""" Component tests VM life cycle in VPC network functionality
"""
#Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.utils import cleanup_resources, validateList
from marvin.lib.base import (VirtualMachine,
                                         NATRule,
                                         LoadBalancerRule,
                                         StaticNATRule,
                                         PublicIPAddress,
                                         VPC,
                                         VpcOffering,
                                         Network,
                                         NetworkOffering,
                                         NetworkACL,
                                         Router,
                                         Account,
                                         ServiceOffering,
                                         Host,
                                         Cluster)
from marvin.lib.common import (get_domain,
                                           get_zone,
                                           get_template,
                                           get_free_vlan,
                                           wait_for_cleanup,
                                           list_virtual_machines,
                                           list_hosts,
                                           findSuitableHostForMigration,
                                           verifyGuestTrafficPortGroups)

from marvin.codes import PASS, ERROR_NO_HOST_FOR_MIGRATION

import time

class Services:
    """Test VM life cycle in VPC network services
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
            "service_offering_1": {
                "name": "Tiny Instance- tagged host 1",
                "displaytext": "Tiny off-tagged host2",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
                "hosttags": "host1"
            },
            "service_offering_2": {
                "name": "Tiny Instance- tagged host 2",
                "displaytext": "Tiny off-tagged host2",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
                "hosttags": "host2"
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
                "serviceCapabilityList": {
                    "SourceNat": {"SupportedSourceNatTypes": "peraccount"},
                    "Lb": {"lbSchemes": "public", "SupportedLbIsolation": "dedicated"}
                },
            },
            "network_offering_no_lb": {
                "name": 'VPC Network offering no LB',
                "displaytext": 'VPC Network off no LB',
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
            "network_off_shared": {
                "name": 'Shared Network offering',
                "displaytext": 'Shared Network offering',
                "guestiptype": 'Shared',
                "traffictype": 'GUEST',
                "availability": 'Optional',
                "useVpc": 'on',
                "specifyIpRanges": True,
                "specifyVlan": True
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
            "lbrule": {
                "name": "SSH",
                "alg": "leastconn",
                # Algorithm used for load balancing
                "privateport": 22,
                "publicport": 22,
                "openfirewall": False,
                "startport": 22,
                "endport": 22,
                "protocol": "TCP",
                "cidrlist": '0.0.0.0/0',
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
                    "icmptype": -1,
                    "icmpcode": -1,
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
                "userdata": 'This is sample data',
            },
            "ostype": 'CentOS 5.3 (64-bit)',
            # Cent OS 5.3 (64 bit)
            "sleep": 60,
            "timeout": 10,
            "mode": 'advanced'
        }

class TestVMLifeCycleVPC(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVMLifeCycleVPC, cls).getClsTestClient()
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

        cls.account = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     admin=True,
                                     domainid=cls.domain.id
                                     )

        cls.vpc_off = VpcOffering.create(
                                     cls.api_client,
                                     cls.services["vpc_offering"]
                                     )

        cls.vpc_off.update(cls.api_client, state='Enabled')

        cls.services["vpc"]["cidr"] = '10.1.1.1/16'
        cls.vpc = VPC.create(
                         cls.api_client,
                         cls.services["vpc"],
                         vpcofferingid=cls.vpc_off.id,
                         zoneid=cls.zone.id,
                         account=cls.account.name,
                         domainid=cls.account.domainid
                         )

        cls.nw_off = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=False
                                            )
        # Enable Network offering
        cls.nw_off.update(cls.api_client, state='Enabled')

        # Creating network using the network offering created
        cls.network_1 = Network.create(
                                cls.api_client,
                                cls.services["network"],
                                accountid=cls.account.name,
                                domainid=cls.account.domainid,
                                networkofferingid=cls.nw_off.id,
                                zoneid=cls.zone.id,
                                gateway='10.1.1.1',
                                vpcid=cls.vpc.id
                                )
        cls.nw_off_no_lb = NetworkOffering.create(
                                    cls.api_client,
                                    cls.services["network_offering_no_lb"],
                                    conservemode=False
                                    )
        # Enable Network offering
        cls.nw_off_no_lb.update(cls.api_client, state='Enabled')
        # Spawn an instance in that network
        cls.vm_1 = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering.id,
                                  networkids=[str(cls.network_1.id)]
                                  )
        cls.vm_2 = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering.id,
                                  networkids=[str(cls.network_1.id)]
                                  )
        cls.public_ip_1 = PublicIPAddress.create(
                                cls.api_client,
                                accountid=cls.account.name,
                                zoneid=cls.zone.id,
                                domainid=cls.account.domainid,
                                networkid=cls.network_1.id,
                                vpcid=cls.vpc.id
                                )
        cls.lb_rule = LoadBalancerRule.create(
                                    cls.api_client,
                                    cls.services["lbrule"],
                                    ipaddressid=cls.public_ip_1.ipaddress.id,
                                    accountid=cls.account.name,
                                    networkid=cls.network_1.id,
                                    vpcid=cls.vpc.id,
                                    domainid=cls.account.domainid
                                )
        cls.lb_rule.assign(cls.api_client, [cls.vm_1, cls.vm_2])

        cls.public_ip_2 = PublicIPAddress.create(
                                cls.api_client,
                                accountid=cls.account.name,
                                zoneid=cls.zone.id,
                                domainid=cls.account.domainid,
                                networkid=cls.network_1.id,
                                vpcid=cls.vpc.id
                                )

        cls.nat_rule = NATRule.create(
                                  cls.api_client,
                                  cls.vm_1,
                                  cls.services["natrule"],
                                  ipaddressid=cls.public_ip_2.ipaddress.id,
                                  openfirewall=False,
                                  networkid=cls.network_1.id,
                                  vpcid=cls.vpc.id
                                  )

        # Opening up the ports in VPC
        cls.nwacl_nat = NetworkACL.create(
                                         cls.api_client,
                                         networkid=cls.network_1.id,
                                         services=cls.services["natrule"],
                                         traffictype='Ingress'
                                    )

        cls.nwacl_lb = NetworkACL.create(
                                cls.api_client,
                                networkid=cls.network_1.id,
                                services=cls.services["lbrule"],
                                traffictype='Ingress'
                                )
        cls.services["icmp_rule"]["protocol"] = "all"
        cls.nwacl_internet_1 = NetworkACL.create(
                                        cls.api_client,
                                        networkid=cls.network_1.id,
                                        services=cls.services["icmp_rule"],
                                        traffictype='Egress'
                                        )
        cls._cleanup = [
                        cls.account,
                        cls.service_offering,
                        cls.nw_off,
                        cls.nw_off_no_lb
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
        self.cleanup = []
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

    def validate_network_rules(self):
        """Validates if the network rules work properly or not?"""
        try:
            self.debug("Checking if we can SSH into VM_1 through %s?" %
                    (self.public_ip_1.ipaddress.ipaddress))
            ssh_1 = self.vm_1.get_ssh_client(
                                ipaddress=self.public_ip_1.ipaddress.ipaddress,
                                reconnect=True)
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
                                    (self.public_ip_1.ipaddress.ipaddress, e))

        result = str(res)
        self.assertEqual(
                         result.count("1 received"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )

        self.debug("Checking if we can SSH into VM_1 through %s?" %
                (self.public_ip_2.ipaddress.ipaddress))
        try:
            ssh_2 = self.vm_1.get_ssh_client(
                            ipaddress=self.public_ip_2.ipaddress.ipaddress,
                            reconnect=True)
            self.debug("SSH into VM is successfully")

            self.debug("Verifying if we can ping to outside world from VM?")
            res = ssh_2.execute("ping -c 1 www.google.com")
        except Exception as e:
            self.fail("Failed to SSH into VM - %s, %s" %
                                    (self.public_ip_2.ipaddress.ipaddress, e))

        result = str(res)
        self.assertEqual(
                         result.count("1 received"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )
        return

    @attr(tags=["advanced", "intervlan", "dvs"])
    def test_01_deploy_instance_in_network(self):
        """ Test deploy an instance in VPC networks
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1(10.1.1.1/24) and network2(10.1.2.1/24) to this VPC.
        # Steps:
        # 1. Deploy vm1 and vm2 in network1 and vm3 and vm4 in network2 using
        #    the default CentOS 6.2 Template

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
        return

    @attr(tags=["advanced", "intervlan", "dvs"])
    def test_02_stop_instance_in_network(self):
        """ Test stop an instance in VPC networks
        """

        # Validate the following
        # 1. Stop the virtual machines.
        # 2. Rules should be still configured on virtual router.

        self.debug("Stopping the virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_1.stop(self.apiclient)
            self.vm_2.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop the virtual instances, %s" % e)

        # Check if the network rules still exists after Vm stop
        self.debug("Checking if NAT rules ")
        nat_rules = NATRule.list(
                                 self.apiclient,
                                 id=self.nat_rule.id,
                                 listall=True
                                 )
        self.assertEqual(
                         isinstance(nat_rules, list),
                         True,
                         "List NAT rules shall return a valid list"
                         )

        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=self.lb_rule.id,
                                         listall=True
                                         )
        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List LB rules shall return a valid list"
                         )
        return

    @attr(tags=["advanced", "intervlan", "dvs"], required_hardware="true")
    def test_03_start_instance_in_network(self):
        """ Test start an instance in VPC networks
        """

        # Validate the following
        # 1. Start the virtual machines.
        # 2. Vm should be started successfully.
        # 3. Make sure that all the PF,LB and Static NAT rules on this VM
        #    works as expected.
        # 3. Make sure that we are able to access google.com from this user Vm

        self.debug("Starting the virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_1.start(self.apiclient)
            self.vm_2.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start the virtual instances, %s" % e)
        # Wait until vms are up
        time.sleep(120)
        self.debug("Validating if the network rules work properly or not?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced", "intervlan", "dvs"], required_hardware="true")
    def test_04_reboot_instance_in_network(self):
        """ Test reboot an instance in VPC networks
        """

        # Validate the following
        # 1. Reboot the virtual machines.
        # 2. Vm should be started successfully.
        # 3. Make sure that all the PF,LB and Static NAT rules on this VM
        #    works as expected.
        # 3. Make sure that we are able to access google.com from this user Vm

        self.debug("Validating if the network rules work properly or not?")
        self.validate_network_rules()

        self.debug("Starting the virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_1.reboot(self.apiclient)
            self.vm_2.reboot(self.apiclient)
        except Exception as e:
            self.fail("Failed to reboot the virtual instances, %s" % e)

        # Wait until vms are up
        time.sleep(120)
        self.debug("Validating if the network rules work properly or not?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced","multihost", "intervlan", "dvs"], required_hardware="true")
    def test_05_destroy_instance_in_network(self):
        """ Test destroy an instance in VPC networks
        """

        # Validate the following
        # 1. Destory the virtual machines.
        # 2. Rules should be still configured on virtual router.
        # 3. Recover the virtual machines.
        # 4. Vm should be in stopped state. State both the instances
        # 5. Make sure that all the PF,LB and Static NAT rules on this VM
        #    works as expected.
        # 6. Make sure that we are able to access google.com from this user Vm

        self.debug("Validating if the network rules work properly or not?")
        self.validate_network_rules()

        self.debug("Destroying the virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_1.delete(self.apiclient, expunge=False)

            list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.vm_1.id
                                                 )

            vm_response = list_vm_response[0]

            self.assertEqual(
                    vm_response.state,
                    'Destroyed',
                    "VM state should be destroyed"
                    )
            
        except Exception as e:
            self.fail("Failed to stop the virtual instances, %s" % e)
            
        # Check if the network rules still exists after Vm stop
        self.debug("Checking if NAT rules ")
        nat_rules = NATRule.list(
                                 self.apiclient,
                                 id=self.nat_rule.id,
                                 listall=True
                                 )
        self.assertEqual(
                         isinstance(nat_rules, list),
                         True,
                         "List NAT rules shall return a valid list"
                         )

        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=self.lb_rule.id,
                                         listall=True
                                         )
        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List LB rules shall return a valid list"
                         )

        self.debug("Recovering the expunged virtual machine vm1 in account: %s" %
                                                self.account.name)
        try:
            self.vm_1.recover(self.apiclient)

            list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.vm_1.id
                                                 )

            vm_response = list_vm_response[0]

            self.assertEqual(
                    vm_response.state,
                    'Stopped',
                    "VM state should be stopped"
                    )

        except Exception as e:
            self.fail("Failed to recover the virtual instances, %s" % e)
            
        try:
            self.vm_2.delete(self.apiclient, expunge=False)

            list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.vm_2.id
                                                 )

            vm_response = list_vm_response[0]

            self.assertEqual(
                    vm_response.state,
                    'Destroyed',
                    "VM state should be destroyed"
                    )




        except Exception as e:
            self.fail("Failed to stop the virtual instances, %s" % e)

        self.debug("Recovering the expunged virtual machine vm2 in account: %s" %
                                                self.account.name)            
        try:
            self.vm_2.recover(self.apiclient)

            list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.vm_2.id
                                                 )

            vm_response = list_vm_response[0]

            self.assertEqual(
                    vm_response.state,
                    'Stopped',
                    "VM state should be stopped"
                    )
        except Exception as e:
            self.fail("Failed to recover the virtual instances, %s" % e)

        self.debug("Starting the two instances..")
        try:
            self.vm_1.start(self.apiclient)

            list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.vm_1.id
                                                 )

            vm_response = list_vm_response[0]

            self.assertEqual(
                    vm_response.state,
                    'Running',
                    "VM state should be running"
                    )

            self.vm_2.start(self.apiclient)

            list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.vm_2.id
                                                 )

            vm_response = list_vm_response[0]

            self.assertEqual(
                    vm_response.state,
                    'Running',
                    "VM state should be running"
                    )
        except Exception as e:
            self.fail("Failed to start the instances, %s" % e)

        # Wait until vms are up
        time.sleep(120)
        self.debug("Validating if the network rules work properly or not?")
        self.validate_network_rules()

        return


    @attr(tags=["advanced", "intervlan", "dvs"], required_hardware="true")
    def test_07_migrate_instance_in_network(self):
        """ Test migrate an instance in VPC networks
        """

        # Validate the following
        # 1. Migrate the virtual machines to other hosts
        # 2. Vm should be in stopped state. State both the instances
        # 3. Make sure that all the PF,LB and Static NAT rules on this VM
        #    works as expected.
        # 3. Make sure that we are able to access google.com from this user Vm
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("vm migrate is not supported in %s" % self.hypervisor)

        self.debug("Validating if the network rules work properly or not?")
        self.validate_network_rules()

        host = findSuitableHostForMigration(self.apiclient, self.vm_1.id)
        if host is None:
            self.skipTest(ERROR_NO_HOST_FOR_MIGRATION)

        self.debug("Migrating VM-ID: %s to Host: %s" % (
                                                        self.vm_1.id,
                                                        host.id
                                                        ))

        try:
            self.vm_1.migrate(self.apiclient, hostid=host.id)
        except Exception as e:
            self.fail("Failed to migrate instance, %s" % e)

        self.debug("Validating if the network rules work properly or not?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced", "intervlan", "dvs"], required_hardware="true")
    def test_08_user_data(self):
        """ Test user data in virtual machines
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1(10.1.1.1/24) and network2(10.1.2.1/24) to this VPC.
        # 3. Deploy a vm in network1 and a vm in network2 using userdata
        # Steps
        # 1.Query for the user data for both the user vms from both networks
        #   User should be able to query the user data for the vms belonging to
        #   both the networks from the VR

        try:
            ssh = self.vm_1.get_ssh_client(
                                ipaddress=self.public_ip_1.ipaddress.ipaddress,
                                reconnect=True)
            self.debug("SSH into VM is successfully")
            ssh.execute("yum install wget -y")
        except Exception as e:
            self.fail("Failed to SSH into instance")

        self.debug("check the userdata with that of present in router")
        try:
            cmds = [
               "wget http://%s/latest/user-data" % self.network_1.gateway,
               "cat user-data",
               ]
            for c in cmds:
                result = ssh.execute(c)
                self.debug("%s: %s" % (c, result))
        except Exception as e:
            self.fail("Failed to SSH in Virtual machine: %s" % e)

        res = str(result)
        self.assertEqual(
                            res.count(
                                self.services["virtual_machine"]["userdata"]),
                            1,
                            "Verify user data from router"
                        )
        return

    @attr(tags=["advanced", "intervlan", "dvs"], required_hardware="true")
    def test_09_meta_data(self):
        """ Test meta data in virtual machines
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1(10.1.1.1/24) and network2(10.1.2.1/24) to this VPC.
        # 3. Deploy a vm in network1 and a vm in network2 using userdata
        # Steps
        # 1.Query for the meta data for both the user vms from both networks
        #   User should be able to query the user data for the vms belonging to
        #   both the networks from the VR

        try:
            ssh = self.vm_1.get_ssh_client(
                                ipaddress=self.public_ip_1.ipaddress.ipaddress,
                                reconnect=True)
            self.debug("SSH into VM is successfully")
        except Exception as e:
            self.fail("Failed to SSH into instance")

        self.debug("check the metadata with that of present in router")
        try:
            cmds = [
               "wget http://%s/latest/vm-id" % self.network_1.gateway,
               "cat vm-id",
               ]
            for c in cmds:
                result = ssh.execute(c)
                self.debug("%s: %s" % (c, result))
        except Exception as e:
            self.fail("Failed to SSH in Virtual machine: %s" % e)

        res = str(result)
        self.assertNotEqual(
                         res,
                         None,
                         "Meta data should be returned from router"
                        )
        return

    @attr(tags=["advanced", "intervlan", "dvs"], required_hardware="true")
    def test_10_expunge_instance_in_network(self):
        """ Test expunge an instance in VPC networks
        """

        # Validate the following
        # 1. Recover the virtual machines.
        # 2. Vm should be in stopped state. State both the instances
        # 3. Make sure that all the PF,LB and Static NAT rules on this VM
        #    works as expected.
        # 3. Make sure that we are able to access google.com from this user Vm

        self.debug("Validating if the network rules work properly or not?")
        self.validate_network_rules()

        self.debug("Delete virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_1.delete(self.apiclient)
            self.vm_2.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to destroy the virtual instances, %s" % e)

        self.debug(
            "Waiting for expunge interval to cleanup the network and VMs")

        wait_for_cleanup(
                         self.apiclient,
                         ["expunge.interval", "expunge.delay"]
                        )

        # Check if the network rules still exists after Vm stop
        self.debug("Checking if NAT rules existed")
        with self.assertRaises(Exception):
            NATRule.list(
                         self.apiclient,
                         id=self.nat_rule.id,
                         listall=True
                         )

            LoadBalancerRule.list(
                                  self.apiclient,
                                  id=self.lb_rule.id,
                                  listall=True
                                  )
        return

class TestVMLifeCycleSharedNwVPC(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVMLifeCycleSharedNwVPC, cls).getClsTestClient()
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

        cls.account = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     admin=True,
                                     domainid=cls.domain.id
                                     )

        cls.services["vpc"]["cidr"] = '10.1.1.1/16'
        cls.vpc = VPC.create(
                         cls.api_client,
                         cls.services["vpc"],
                         vpcofferingid=cls.vpc_off.id,
                         zoneid=cls.zone.id,
                         account=cls.account.name,
                         domainid=cls.account.domainid
                         )

        cls.nw_off = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=False
                                            )
        # Enable Network offering
        cls.nw_off.update(cls.api_client, state='Enabled')

        # Creating network using the network offering created
        cls.network_1 = Network.create(
                                cls.api_client,
                                cls.services["network"],
                                accountid=cls.account.name,
                                domainid=cls.account.domainid,
                                networkofferingid=cls.nw_off.id,
                                zoneid=cls.zone.id,
                                gateway='10.1.1.1',
                                vpcid=cls.vpc.id
                                )
        cls.nw_off_no_lb = NetworkOffering.create(
                                    cls.api_client,
                                    cls.services["network_offering_no_lb"],
                                    conservemode=False
                                    )

        cls.shared_nw_off = NetworkOffering.create(
                                        cls.api_client,
                                        cls.services["network_off_shared"],
                                        conservemode=False
                                        )
        # Enable Network offering
        cls.shared_nw_off.update(cls.api_client, state='Enabled')


        physical_network, shared_vlan = get_free_vlan(cls.api_client, cls.zone.id)
        if shared_vlan is None:
            assert False, "Failed to get free vlan id for shared network creation in the zone"

        #create network using the shared network offering created
        cls.services["network"]["acltype"] = "Domain"
        cls.services["network"]["physicalnetworkid"] = physical_network.id
        cls.services["network"]["vlan"] = shared_vlan

        # Start Ip and End Ip should be specified for shared network
        cls.services["network"]["startip"] = '10.1.2.20'
        cls.services["network"]["endip"] = '10.1.2.30'

        # Creating network using the network offering created
        cls.network_2 = Network.create(
                                cls.api_client,
                                cls.services["network"],
                                accountid=cls.account.name,
                                domainid=cls.account.domainid,
                                networkofferingid=cls.shared_nw_off.id,
                                zoneid=cls.zone.id,
                                gateway='10.1.2.1',
                                )

        cls.vm_1 = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering.id,
                                  networkids=[str(cls.network_1.id),
                                              str(cls.network_2.id)]
                                  )

        cls.vm_2 = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering.id,
                                  networkids=[str(cls.network_1.id),
                                              str(cls.network_2.id)]
                                  )


        cls.vm_3 = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering.id,
                                  networkids=[str(cls.network_1.id),
                                              str(cls.network_2.id)]
                                  )

        cls.public_ip_1 = PublicIPAddress.create(
                                cls.api_client,
                                accountid=cls.account.name,
                                zoneid=cls.zone.id,
                                domainid=cls.account.domainid,
                                networkid=cls.network_1.id,
                                vpcid=cls.vpc.id
                                )
        cls.lb_rule = LoadBalancerRule.create(
                                    cls.api_client,
                                    cls.services["lbrule"],
                                    ipaddressid=cls.public_ip_1.ipaddress.id,
                                    accountid=cls.account.name,
                                    networkid=cls.network_1.id,
                                    vpcid=cls.vpc.id,
                                    domainid=cls.account.domainid
                                )

        # Only the vms in the same network can be added to load balancing rule
        # hence we can't add vm_2 with vm_1
        cls.lb_rule.assign(cls.api_client, [cls.vm_1])

        cls.public_ip_2 = PublicIPAddress.create(
                                cls.api_client,
                                accountid=cls.account.name,
                                zoneid=cls.zone.id,
                                domainid=cls.account.domainid,
                                networkid=cls.network_1.id,
                                vpcid=cls.vpc.id
                                )

        cls.nat_rule = NATRule.create(
                                  cls.api_client,
                                  cls.vm_1,
                                  cls.services["natrule"],
                                  ipaddressid=cls.public_ip_2.ipaddress.id,
                                  openfirewall=False,
                                  networkid=cls.network_1.id,
                                  vpcid=cls.vpc.id
                                  )

        # Opening up the ports in VPC
        cls.nwacl_nat = NetworkACL.create(
                                         cls.api_client,
                                         networkid=cls.network_1.id,
                                         services=cls.services["natrule"],
                                         traffictype='Ingress'
                                    )

        cls.nwacl_lb = NetworkACL.create(
                                cls.api_client,
                                networkid=cls.network_1.id,
                                services=cls.services["lbrule"],
                                traffictype='Ingress'
                                )
        cls.services["icmp_rule"]["protocol"] = "all"
        cls.nwacl_internet_1 = NetworkACL.create(
                                        cls.api_client,
                                        networkid=cls.network_1.id,
                                        services=cls.services["icmp_rule"],
                                        traffictype='Egress'
                                        )
        cls._cleanup = [
                        cls.account,
                        cls.network_2,
                        cls.nw_off,
                        cls.shared_nw_off,
                        cls.vpc_off,
                        cls.service_offering,
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.vpc_off.update(cls.api_client, state='Disabled')
            cls.shared_nw_off.update(cls.api_client, state='Disabled')
            cls.nw_off.update(cls.api_client, state='Disabled')
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
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

    def validate_network_rules(self):
        """Validating if the network rules (PF/LB) works properly or not?"""

        try:
            self.debug("Checking if we can SSH into VM_1 through %s?" %
                    (self.public_ip_1.ipaddress.ipaddress))
            ssh_1 = self.vm_1.get_ssh_client(
                                ipaddress=self.public_ip_1.ipaddress.ipaddress,
                                reconnect=True)
            self.debug("SSH into VM is successfully")

            self.debug("Verifying if we can ping to outside world from VM?")
            # Ping to outsite world
            res = ssh_1.execute("ping -c 1 www.google.com")
            # res = 64 bytes from maa03s17-in-f20.1e100.net (74.125.236.212):
            # icmp_req=1 ttl=57 time=25.9 ms
            # --- www.l.google.com ping statistics ---
            # 1 packets transmitted, 1 received, 0% packet loss, time 0ms
            # rtt min/avg/max/mdev = 25.970/25.970/25.970/0.000 ms
            result = str(res)
            self.assertEqual(
                         result.count("1 received"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )

            self.debug("We should be allowed to ping virtual gateway")
            self.debug("Finding the gateway corresponding to isolated network")
            gateways = [nic.gateway for nic in self.vm_1.nic if nic.networkid == self.network_1.id]

            gateway_list_validation_result = validateList(gateways)

            self.assertEqual(gateway_list_validation_result[0], PASS, "gateway list validation failed due to %s" %
                             gateway_list_validation_result[2])

            gateway = gateway_list_validation_result[1]

            self.debug("VM gateway: %s" % gateway)

            res = ssh_1.execute("ping -c 1 %s" % gateway)
            self.debug("ping -c 1 %s: %s" % (gateway, res))

            result = str(res)
            self.assertEqual(
                         result.count("1 received"),
                         1,
                         "Ping to VM gateway should be successful"
                         )
        except Exception as e:
            self.fail("Failed to SSH into VM - %s, %s" %
                                    (self.public_ip_1.ipaddress.ipaddress, e))
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_01_deploy_instance_in_network(self):
        """ Test deploy an instance in VPC networks
        """

        # Validate the following
        # 1. Successful deployment of the User VM.
        # 2. Ping any host in the public Internet successfully.
        # 3. Ping the gateways of the VPC's guest network and the
        #    Shared Guest Network successfully.

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

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_02_stop_instance_in_network(self):
        """ Test stop an instance in VPC networks
        """

        # Validate the following
        # 1. Stop the virtual machines.
        # 2. Rules should be still configured on virtual router.

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()

        self.debug("Stopping one of the virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_2.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop the virtual instances, %s" % e)

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_03_start_instance_in_network(self):
        """ Test start an instance in VPC networks
        """

        # Validate the following
        # 1. Start the virtual machines.
        # 2. Rules should be still configured on virtual router.

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()

        self.debug("Starting one of the virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_2.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start the virtual instances, %s" % e)

        self.debug("Check if the instance is in stopped state?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=self.vm_2.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List virtual machines should return a valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "Virtual machine should be in running state"
                         )

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_04_reboot_instance_in_network(self):
        """ Test reboot an instance in VPC networks
        """

        # Validate the following
        # 1. Reboot the virtual machines.
        # 2. Rules should be still configured on virtual router.

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()

        self.debug("Restarting the virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_1.reboot(self.apiclient)
            self.vm_2.reboot(self.apiclient)
        except Exception as e:
            self.fail("Failed to reboot the virtual instances, %s" % e)

        self.debug("Check if the instance is in stopped state?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List virtual machines should return a valid list"
                         )
        for vm in vms:
            self.assertEqual(
                         vm.state,
                         "Running",
                         "Virtual machine should be in running state"
                         )

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_05_destroy_instance_in_network(self):
        """ Test destroy an instance in VPC networks
        """

        # Validate the following
        # 1. Destroy one of the virtual machines.
        # 2. Rules should be still configured on virtual router.

        self.debug("Destroying one of the virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_2.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to destroy the virtual instances, %s" % e)

        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

        self.debug("Check if the instance is in stopped state?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=self.vm_2.id,
                                  listall=True
                                  )
        self.assertEqual(
                         vms,
                         None,
                         "List virtual machines should not return anything"
                         )

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_06_recover_instance_in_network(self):
        """ Test recover an instance in VPC networks
        """

        self.debug("Deploying vm")

        self.vm_2 = VirtualMachine.create(
                                  self.api_client,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_1.id),
                                              str(self.network_2.id)]
                                  )

        self.cleanup.append(self.vm_2)

        try:
            self.vm_2.delete(self.apiclient, expunge=False)
        except Exception as e:
            self.fail("Failed to destroy the virtual instances, %s" % e)

        try:
            self.vm_2.recover(self.apiclient)
        except Exception as e:
            self.fail("Failed to recover the virtual instances, %s" % e)

        self.debug("Check if the instance is in stopped state?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=self.vm_2.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List virtual machines should return a valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Stopped",
                         "Virtual machine should be in stopped state"
                         )

        self.debug("Starting the instance: %s" % self.vm_2.name)
        try:
            self.vm_2.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start the instances, %s" % e)

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=self.vm_2.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List virtual machines should return a valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "Virtual machine should be in running state"
                         )

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_07_migrate_instance_in_network(self):
        """ Test migrate an instance in VPC networks
        """

        # Validate the following
        # 1. Migrate the virtual machines to other hosts
        # 2. Vm should be in stopped state. State both the instances
        # 3. Make sure that all the PF,LB and Static NAT rules on this VM
        #    works as expected.
        # 3. Make sure that we are able to access google.com from this user Vm
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("vm migrate is not supported in %s" % self.hypervisor)

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()

        host = findSuitableHostForMigration(self.apiclient, self.vm_1.id)
        if host is None:
            self.skipTest(ERROR_NO_HOST_FOR_MIGRATION)

        self.debug("Migrating VM-ID: %s to Host: %s" % (
                                                        self.vm_1.id,
                                                        host.id
                                                        ))

        try:
            self.vm_1.migrate(self.apiclient, hostid=host.id)
        except Exception as e:
            self.fail("Failed to migrate instance, %s" % e)

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_08_user_data(self):
        """ Test user data in virtual machines
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1(10.1.1.1/24) and network2(10.1.2.1/24) to this VPC.
        # 3. Deploy a vm in network1 and a vm in network2 using userdata
        # Steps
        # 1.Query for the user data for both the user vms from both networks
        #   User should be able to query the user data for the vms belonging to
        #   both the networks from the VR

        try:
            ssh = self.vm_1.get_ssh_client(
                                ipaddress=self.public_ip_1.ipaddress.ipaddress,
                                reconnect=True)
            self.debug("SSH into VM is successfully")
            ssh.execute("yum install wget -y")
        except Exception as e:
            self.fail("Failed to SSH into instance")

        self.debug("check the userdata with that of present in router")
        try:
            cmds = [
               "wget http://%s/latest/user-data" % self.network_1.gateway,
               "cat user-data",
               ]
            for c in cmds:
                result = ssh.execute(c)
                self.debug("%s: %s" % (c, result))
        except Exception as e:
            self.fail("Failed to SSH in Virtual machine: %s" % e)

        res = str(result)
        self.assertEqual(
                            res.count(
                                self.services["virtual_machine"]["userdata"]),
                            1,
                            "Verify user data from router"
                        )
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_09_meta_data(self):
        """ Test meta data in virtual machines
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1(10.1.1.1/24) and network2(10.1.2.1/24) to this VPC.
        # 3. Deploy a vm in network1 and a vm in network2 using userdata
        # Steps
        # 1.Query for the meta data for both the user vms from both networks
        #   User should be able to query the user data for the vms belonging to
        #   both the networks from the VR

        try:
            ssh = self.vm_1.get_ssh_client(
                                ipaddress=self.public_ip_1.ipaddress.ipaddress,
                                reconnect=True)
            self.debug("SSH into VM is successfully")
        except Exception as e:
            self.fail("Failed to SSH into instance")

        self.debug("check the metadata with that of present in router")
        try:
            cmds = [
               "wget http://%s/latest/vm-id" % self.network_1.gateway,
               "cat vm-id",
               ]
            for c in cmds:
                result = ssh.execute(c)
                self.debug("%s: %s" % (c, result))
        except Exception as e:
            self.fail("Failed to SSH in Virtual machine: %s" % e)

        res = str(result)
        self.assertNotEqual(
                         res,
                         None,
                         "Meta data should be returned from router"
                        )
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_10_expunge_instance_in_network(self):
        """ Test expunge an instance in VPC networks
        """

        # Validate the following
        # 1. Recover the virtual machines.
        # 2. Vm should be in stopped state. State both the instances
        # 3. Make sure that all the PF,LB and Static NAT rules on this VM
        #    works as expected.
        # 3. Make sure that we are able to access google.com from this user Vm

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()

        self.debug("Delete virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_3.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to destroy the virtual instances, %s" % e)

        self.debug(
            "Waiting for expunge interval to cleanup the network and VMs")

        wait_for_cleanup(
                         self.apiclient,
                         ["expunge.interval", "expunge.delay"]
                        )

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()

        self.debug(
            "Deleting the rest of the virtual machines in account: %s" %
                                                    self.account.name)
        try:
            self.vm_1.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to destroy the virtual instances, %s" % e)

        self.debug(
            "Waiting for expunge interval to cleanup the network and VMs")

        wait_for_cleanup(
                         self.apiclient,
                         ["expunge.interval", "expunge.delay"]
                        )

        # Check if the network rules still exists after Vm expunged
        self.debug("Checking if NAT rules existed ")
        with self.assertRaises(Exception):
            NATRule.list(
                         self.apiclient,
                         id=self.nat_rule.id,
                         listall=True
                         )

            LoadBalancerRule.list(
                                  self.apiclient,
                                  id=self.lb_rule.id,
                                  listall=True
                                  )
        return

class TestVMLifeCycleBothIsolated(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVMLifeCycleBothIsolated, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.hypervisor = cls.testClient.getHypervisorInfo()
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

        cls.account = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     admin=True,
                                     domainid=cls.domain.id
                                     )

        cls.vpc_off = VpcOffering.create(
                                     cls.api_client,
                                     cls.services["vpc_offering"]
                                     )

        cls.vpc_off.update(cls.api_client, state='Enabled')

        cls.services["vpc"]["cidr"] = '10.1.1.1/16'
        cls.vpc = VPC.create(
                         cls.api_client,
                         cls.services["vpc"],
                         vpcofferingid=cls.vpc_off.id,
                         zoneid=cls.zone.id,
                         account=cls.account.name,
                         domainid=cls.account.domainid
                         )

        cls.nw_off = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=False
                                            )
        # Enable Network offering
        cls.nw_off.update(cls.api_client, state='Enabled')

        # Creating network using the network offering created
        cls.network_1 = Network.create(
                                cls.api_client,
                                cls.services["network"],
                                accountid=cls.account.name,
                                domainid=cls.account.domainid,
                                networkofferingid=cls.nw_off.id,
                                zoneid=cls.zone.id,
                                gateway='10.1.1.1',
                                vpcid=cls.vpc.id
                                )
        cls.nw_off_no_lb = NetworkOffering.create(
                                    cls.api_client,
                                    cls.services["network_offering_no_lb"],
                                    conservemode=False
                                    )

        # Enable Network offering
        cls.nw_off_no_lb.update(cls.api_client, state='Enabled')

        # Creating network using the network offering created
        cls.network_2 = Network.create(
                                cls.api_client,
                                cls.services["network"],
                                accountid=cls.account.name,
                                domainid=cls.account.domainid,
                                networkofferingid=cls.nw_off_no_lb.id,
                                zoneid=cls.zone.id,
                                gateway='10.1.2.1',
                                vpcid=cls.vpc.id
                                )
        cls._cleanup = [
                        cls.account,
                        cls.service_offering,
                        cls.nw_off,
                        cls.nw_off_no_lb,
                        cls.vpc_off
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
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

    def validate_network_rules(self):
        """Validating if the network rules (PF/LB) works properly or not?"""

        try:
            self.debug("Checking if we can SSH into VM_1 through %s?" %
                    (self.public_ip_1.ipaddress.ipaddress))
            ssh_1 = self.vm_1.get_ssh_client(
                                ipaddress=self.public_ip_1.ipaddress.ipaddress,
                                reconnect=True)
            self.debug("SSH into VM is successfully")

            self.debug("Verifying if we can ping to outside world from VM?")
            # Ping to outsite world
            res = ssh_1.execute("ping -c 1 www.google.com")
            # res = 64 bytes from maa03s17-in-f20.1e100.net (74.125.236.212):
            # icmp_req=1 ttl=57 time=25.9 ms
            # --- www.l.google.com ping statistics ---
            # 1 packets transmitted, 1 received, 0% packet loss, time 0ms
            # rtt min/avg/max/mdev = 25.970/25.970/25.970/0.000 ms
            result = str(res)
            self.assertEqual(
                         result.count("1 received"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )

            self.debug("We should be allowed to ping virtual gateway")
            self.debug("VM gateway: %s" % self.vm_1.nic[0].gateway)

            res = ssh_1.execute("ping -c 1 %s" % self.vm_1.nic[0].gateway)
            self.debug("ping -c 1 %s: %s" % (self.vm_1.nic[0].gateway, res))

            result = str(res)
            self.assertEqual(
                         result.count("1 received"),
                         1,
                         "Ping to VM gateway should be successful"
                         )
        except Exception as e:
            self.fail("Failed to SSH into VM - %s, %s" %
                                    (self.public_ip_1.ipaddress.ipaddress, e))
        return

    @attr(tags=["advanced", "intervlan"])
    def test_01_deploy_vm_two_isolated_nw(self):
        """ Test deploy virtual machine in two isolated networks"""

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1(10.1.1.1/24) and network2(10.1.2.1/24) to this VPC.
        # Steps:
        # 1. Deploy a VM such that the VM is part of both networks-network1
        #    and network2. Fail to deploy a VM.

        self.debug("Validating the VPC offering created")
        self.validate_vpc_offering(self.vpc_off)

        self.debug("Validating VPC created in setup class")
        self.validate_vpc_network(self.vpc)

        self.debug("Deploying virtual machine in two isolated networks")
        with self.assertRaises(Exception):
            VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_1.id),
                                              str(self.network_2.id)]
                                  )
        self.debug("Deploy VM in 2 isolated networks failed")
        return

    @attr(tags=["advanced", "intervlan"])
    def test_02_deploy_vm_vpcvr_stopped(self):
        """ Test deploy virtual machine when VPC VR in stopped state"""

        # Validate the following
        # Pre-Req:
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1(10.1.1.1/24) to this VPC.
        # 3. Stop the VPC Virtual Router
        # Steps:
        # 1. Deploy a VM using the default CentOS 6.2 Template

        self.debug("Finding the virtual router for vpc: %s" % self.vpc.id)

        routers = Router.list(
                              self.apiclient,
                              zoneid=self.zone.id,
                              listall=True
                              )
        self.assertEqual(
                         isinstance(routers, list),
                         True,
                         "List routers should return router for vpc: %s" %
                                                        self.vpc.id
                         )
        router = routers[0]

        self.debug("Check state of VPC virtual router, state: %s" %
                                                                router.state)
        if router.state == "Running":
            self.debug("Router state is running, stop it!")
            Router.stop(self.apiclient, id=router.id)

            self.debug("Check the router state again")
            routers = Router.list(
                              self.apiclient,
                              id=router.id,
                              listall=True
                              )
            self.assertEqual(
                         isinstance(routers, list),
                         True,
                         "List routers should return router for vpc: %s" %
                                                        self.vpc.id
                         )
            router = routers[0]
            self.debug("router.state %s" %
                    router.state)

            self.assertEqual(
                             router.state,
                             "Stopped",
                             "Router state should be stopped"
                             )
        self.debug("Deploy an instance in network: %s with stopped VPCVR" %
                                                        self.network_1.name)
        try:
            vm = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_1.id)]
                                  )
        except Exception as e:
            self.fail("Failed to deploy the virtual instance: %s" % e)

        self.debug("Verify the deployment of virtual instace")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=vm.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List vms shall return a valid resposnse"
                         )
        vm_response = vms[0]
        self.assertEqual(
                         vm_response.state,
                         "Running",
                         "VM state should be running after deployment"
                         )
        return

    @attr(tags=["dvs"], required_hardware="true")
    def test_guest_traffic_port_groups_vpc_network(self):
        """ Verify port groups are created for guest traffic
        used by vpc network """

        if self.hypervisor.lower() == "vmware":
            response = verifyGuestTrafficPortGroups(self.apiclient,
                                                    self.config,
                                                    self.zone)
            assert response[0] == PASS, response[1]

class TestVMLifeCycleStoppedVPCVR(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVMLifeCycleStoppedVPCVR, cls).getClsTestClient()
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

        cls.account = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     admin=True,
                                     domainid=cls.domain.id
                                     )

        cls.vpc_off = VpcOffering.create(
                                     cls.api_client,
                                     cls.services["vpc_offering"]
                                     )

        cls.vpc_off.update(cls.api_client, state='Enabled')

        cls.services["vpc"]["cidr"] = '10.1.1.1/16'
        cls.vpc = VPC.create(
                         cls.api_client,
                         cls.services["vpc"],
                         vpcofferingid=cls.vpc_off.id,
                         zoneid=cls.zone.id,
                         account=cls.account.name,
                         domainid=cls.account.domainid
                         )

        cls.nw_off = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=False
                                            )
        # Enable Network offering
        cls.nw_off.update(cls.api_client, state='Enabled')

        # Creating network using the network offering created
        cls.network_1 = Network.create(
                                cls.api_client,
                                cls.services["network"],
                                accountid=cls.account.name,
                                domainid=cls.account.domainid,
                                networkofferingid=cls.nw_off.id,
                                zoneid=cls.zone.id,
                                gateway='10.1.1.1',
                                vpcid=cls.vpc.id
                                )
        cls.nw_off_no_lb = NetworkOffering.create(
                                    cls.api_client,
                                    cls.services["network_offering_no_lb"],
                                    conservemode=False
                                    )
        # Enable Network offering
        cls.nw_off_no_lb.update(cls.api_client, state='Enabled')

        # Creating network using the network offering created
        cls.network_2 = Network.create(
                                cls.api_client,
                                cls.services["network"],
                                accountid=cls.account.name,
                                domainid=cls.account.domainid,
                                networkofferingid=cls.nw_off_no_lb.id,
                                zoneid=cls.zone.id,
                                gateway='10.1.2.1',
                                vpcid=cls.vpc.id
                                )
        # Spawn an instance in that network
        cls.vm_1 = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering.id,
                                  networkids=[str(cls.network_1.id)]
                                  )
        # Spawn an instance in that network
        cls.vm_2 = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering.id,
                                  networkids=[str(cls.network_1.id)]
                                  )
        cls.vm_3 = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering.id,
                                  networkids=[str(cls.network_2.id)]
                                  )

        cls.public_ip_1 = PublicIPAddress.create(
                                cls.api_client,
                                accountid=cls.account.name,
                                zoneid=cls.zone.id,
                                domainid=cls.account.domainid,
                                networkid=cls.network_1.id,
                                vpcid=cls.vpc.id
                                )
        cls.lb_rule = LoadBalancerRule.create(
                                    cls.api_client,
                                    cls.services["lbrule"],
                                    ipaddressid=cls.public_ip_1.ipaddress.id,
                                    accountid=cls.account.name,
                                    networkid=cls.network_1.id,
                                    vpcid=cls.vpc.id,
                                    domainid=cls.account.domainid
                                )
        cls.lb_rule.assign(cls.api_client, [cls.vm_1, cls.vm_2])

        cls.public_ip_2 = PublicIPAddress.create(
                                cls.api_client,
                                accountid=cls.account.name,
                                zoneid=cls.zone.id,
                                domainid=cls.account.domainid,
                                networkid=cls.network_1.id,
                                vpcid=cls.vpc.id
                                )

        cls.nat_rule = NATRule.create(
                                  cls.api_client,
                                  cls.vm_1,
                                  cls.services["natrule"],
                                  ipaddressid=cls.public_ip_2.ipaddress.id,
                                  openfirewall=False,
                                  networkid=cls.network_1.id,
                                  vpcid=cls.vpc.id
                                  )

        # Opening up the ports in VPC
        cls.nwacl_nat = NetworkACL.create(
                                         cls.api_client,
                                         networkid=cls.network_1.id,
                                         services=cls.services["natrule"],
                                         traffictype='Ingress'
                                    )

        cls.nwacl_lb = NetworkACL.create(
                                cls.api_client,
                                networkid=cls.network_1.id,
                                services=cls.services["lbrule"],
                                traffictype='Ingress'
                                )

        cls.nwacl_internet = NetworkACL.create(
                                        cls.api_client,
                                        networkid=cls.network_1.id,
                                        services=cls.services["icmp_rule"],
                                        traffictype='Egress'
                                        )
        cls._cleanup = [
                        cls.account,
                        cls.service_offering,
                        cls.nw_off,
                        cls.nw_off_no_lb
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.debug("Check the status of VPC virtual router")
        routers = Router.list(
                              self.apiclient,
                              zoneid=self.zone.id,
                              listall=True
                              )
        if not isinstance(routers, list):
            raise Exception("No response from list routers API")

        self.router = routers[0]
        if self.router.state == "Running":
            Router.stop(self.apiclient, id=self.router.id)

        self.cleanup = []
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

    def validate_network_rules(self):
        """Validates if the network rules work properly or not?"""
        try:
            self.debug("Checking if we can SSH into VM_1 through %s?" %
                    (self.public_ip_1.ipaddress.ipaddress))
            ssh_1 = self.vm_1.get_ssh_client(
                                ipaddress=self.public_ip_1.ipaddress.ipaddress,
                                reconnect=True)
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
                                    (self.public_ip_1.ipaddress.ipaddress, e))

        result = str(res)
        self.assertEqual(
                         result.count("1 received"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )

        self.debug("Checking if we can SSH into VM_1?")
        try:
            ssh_2 = self.vm_1.get_ssh_client(
                            ipaddress=self.public_ip_2.ipaddress.ipaddress,
                            reconnect=True)
            self.debug("SSH into VM is successfully")

            self.debug("Verifying if we can ping to outside world from VM?")
            res = ssh_2.execute("ping -c 1 www.google.com")
        except Exception as e:
            self.fail("Failed to SSH into VM - %s, %s" %
                                    (self.public_ip_2.ipaddress.ipaddress, e))

        result = str(res)
        self.assertEqual(
                         result.count("1 received"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )
        return

    @attr(tags=["advanced", "intervlan"])
    def test_01_deploy_instance_in_network(self):
        """ Test deploy an instance in VPC networks
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1(10.1.1.1/24) and network2(10.1.2.1/24) to this VPC.
        # Steps:
        # 1. Deploy vm1 and vm2 in network1 and vm3 and vm4 in network2 using
        #    the default CentOS 6.2 Template

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
        return

    @attr(tags=["advanced", "intervlan"])
    def test_02_stop_instance_in_network(self):
        """ Test stop an instance in VPC networks
        """

        # Validate the following
        # 1. Stop the virtual machines.
        # 2. Rules should be still configured on virtual router.

        self.debug("Stopping the virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_1.stop(self.apiclient)
            self.vm_2.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop the virtual instances, %s" % e)

        # Check if the network rules still exists after Vm stop
        self.debug("Checking if NAT rules ")
        nat_rules = NATRule.list(
                                 self.apiclient,
                                 id=self.nat_rule.id,
                                 listall=True
                                 )
        self.assertEqual(
                         isinstance(nat_rules, list),
                         True,
                         "List NAT rules shall return a valid list"
                         )

        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=self.lb_rule.id,
                                         listall=True
                                         )
        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List LB rules shall return a valid list"
                         )
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_03_start_instance_in_network(self):
        """ Test start an instance in VPC networks
        """

        # Validate the following
        # 1. Start the virtual machines.
        # 2. Vm should be started successfully.
        # 3. Make sure that all the PF,LB and Static NAT rules on this VM
        #    works as expected.
        # 3. Make sure that we are able to access google.com from this user Vm

        self.debug("Starting the virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_1.start(self.apiclient)
            self.vm_2.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start the virtual instances, %s" % e)
        self.debug("Validating if the network rules work properly or not?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_04_reboot_instance_in_network(self):
        """ Test reboot an instance in VPC networks
        """

        # Validate the following
        # 1. Reboot the virtual machines.
        # 2. Vm should be started successfully.
        # 3. Make sure that all the PF,LB and Static NAT rules on this VM
        #    works as expected.
        # 3. Make sure that we are able to access google.com from this user Vm

        self.debug("Validating if the network rules work properly or not?")
        self.validate_network_rules()

        self.debug("Starting the virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_1.reboot(self.apiclient)
            self.vm_2.reboot(self.apiclient)
        except Exception as e:
            self.fail("Failed to reboot the virtual instances, %s" % e)

        self.debug("Validating if the network rules work properly or not?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced","multihost", "intervlan"], required_hardware="true")
    def test_05_destroy_instance_in_network(self):
        """ Test destroy an instance in VPC networks
        """

        # Validate the following
        # 1. Destory the virtual machines.
        # 2. Rules should be still configured on virtual router.
        # 3. Recover the virtual machines.
        # 4. Vm should be in stopped state. State both the instances
        # 5. Make sure that all the PF,LB and Static NAT rules on this VM
        #    works as expected.
        # 6. Make sure that we are able to access google.com from this user Vm

        self.debug("Validating if the network rules work properly or not?")
        self.validate_network_rules()

        self.debug("Destroying the virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_1.delete(self.apiclient, expunge=False)

            list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.vm_1.id
                                                 )

            vm_response = list_vm_response[0]

            self.assertEqual(
                    vm_response.state,
                    'Destroyed',
                    "VM state should be destroyed"
                    )
            
        except Exception as e:
            self.fail("Failed to stop the virtual instances, %s" % e)
            
        # Check if the network rules still exists after Vm stop
        self.debug("Checking if NAT rules ")
        nat_rules = NATRule.list(
                                 self.apiclient,
                                 id=self.nat_rule.id,
                                 listall=True
                                 )
        self.assertEqual(
                         isinstance(nat_rules, list),
                         True,
                         "List NAT rules shall return a valid list"
                         )

        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=self.lb_rule.id,
                                         listall=True
                                         )
        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List LB rules shall return a valid list"
                         )

        self.debug("Recovering the expunged virtual machine vm1 in account: %s" %
                                                self.account.name)
        try:
            self.vm_1.recover(self.apiclient)

            list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.vm_1.id
                                                 )

            vm_response = list_vm_response[0]

            self.assertEqual(
                    vm_response.state,
                    'Stopped',
                    "VM state should be stopped"
                    )

        except Exception as e:
            self.fail("Failed to recover the virtual instances, %s" % e)
            
        try:
            self.vm_2.delete(self.apiclient, expunge=False)

            list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.vm_2.id
                                                 )

            vm_response = list_vm_response[0]

            self.assertEqual(
                    vm_response.state,
                    'Destroyed',
                    "VM state should be destroyed"
                    )

        except Exception as e:
            self.fail("Failed to stop the virtual instances, %s" % e)

        self.debug("Recovering the expunged virtual machine vm2 in account: %s" %
                                                self.account.name)            
        try:
            self.vm_2.recover(self.apiclient)

            list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.vm_2.id
                                                 )

            vm_response = list_vm_response[0]

            self.assertEqual(
                    vm_response.state,
                    'Stopped',
                    "VM state should be stopped"
                    )
        except Exception as e:
            self.fail("Failed to recover the virtual instances, %s" % e)

        self.debug("Starting the two instances..")
        try:
            self.vm_1.start(self.apiclient)

            list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.vm_1.id
                                                 )

            vm_response = list_vm_response[0]

            self.assertEqual(
                    vm_response.state,
                    'Running',
                    "VM state should be running"
                    )

            self.vm_2.start(self.apiclient)

            list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.vm_2.id
                                                 )

            vm_response = list_vm_response[0]

            self.assertEqual(
                    vm_response.state,
                    'Running',
                    "VM state should be running"
                    )
        except Exception as e:
            self.fail("Failed to start the instances, %s" % e)

        # Wait until vms are up
        time.sleep(120)
        self.debug("Validating if the network rules work properly or not?")
        self.validate_network_rules()

        return

class TestVMLifeCycleDiffHosts(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVMLifeCycleDiffHosts, cls).getClsTestClient()
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

            # 2 hosts are needed within cluster to run the test cases and
            # 3rd host is needed to run the migrate test case
            # Even if only 2 hosts are present, remaining test cases will be run and
            # migrate test will be skipped automatically
        cluster = cls.FindClusterWithSufficientHosts(numberofhosts = 3)
        if cluster is None:
            raise unittest.SkipTest("Skipping as unable to find a cluster with\
                    sufficient number of hosts")


        hosts = list_hosts(cls.api_client, type="Routing", listall=True, clusterid=cluster.id)

        assert isinstance(hosts, list), "list_hosts should return a list response,\
                                    instead got %s" % hosts

        Host.update(cls.api_client, id=hosts[0].id, hosttags="host1")
        Host.update(cls.api_client, id=hosts[1].id, hosttags="host2")

        if len(hosts) > 2:
            Host.update(cls.api_client, id=hosts[2].id, hosttags="host1")

        cls.service_offering_1 = ServiceOffering.create(
                                        cls.api_client,
                                        cls.services["service_offering_1"]
                                        )
        cls.service_offering_2 = ServiceOffering.create(
                                        cls.api_client,
                                        cls.services["service_offering_2"]
                                        )

        cls.account = Account.create(
                                 cls.api_client,
                                 cls.services["account"],
                                 admin=True,
                                 domainid=cls.domain.id
                                 )

        cls.vpc_off = VpcOffering.create(
                                 cls.api_client,
                                 cls.services["vpc_offering"]
                                 )

        cls.vpc_off.update(cls.api_client, state='Enabled')

        cls.services["vpc"]["cidr"] = '10.1.1.1/16'
        cls.vpc = VPC.create(
                     cls.api_client,
                     cls.services["vpc"],
                     vpcofferingid=cls.vpc_off.id,
                     zoneid=cls.zone.id,
                     account=cls.account.name,
                     domainid=cls.account.domainid
                     )

        cls.nw_off = NetworkOffering.create(
                                        cls.api_client,
                                        cls.services["network_offering"],
                                        conservemode=False
                                        )
        # Enable Network offering
        cls.nw_off.update(cls.api_client, state='Enabled')

        # Creating network using the network offering created
        cls.network_1 = Network.create(
                            cls.api_client,
                            cls.services["network"],
                            accountid=cls.account.name,
                            domainid=cls.account.domainid,
                            networkofferingid=cls.nw_off.id,
                            zoneid=cls.zone.id,
                            gateway='10.1.1.1',
                            vpcid=cls.vpc.id
                            )
        cls.nw_off_no_lb = NetworkOffering.create(
                                cls.api_client,
                                cls.services["network_offering_no_lb"],
                                conservemode=False
                                )
        # Enable Network offering
        cls.nw_off_no_lb.update(cls.api_client, state='Enabled')

        # Creating network using the network offering created
        cls.network_2 = Network.create(
                            cls.api_client,
                            cls.services["network"],
                            accountid=cls.account.name,
                            domainid=cls.account.domainid,
                            networkofferingid=cls.nw_off_no_lb.id,
                            zoneid=cls.zone.id,
                            gateway='10.1.2.1',
                            vpcid=cls.vpc.id
                            )
        # Spawn an instance in that network
        cls.vm_1 = VirtualMachine.create(
                              cls.api_client,
                              cls.services["virtual_machine"],
                              accountid=cls.account.name,
                              domainid=cls.account.domainid,
                              serviceofferingid=cls.service_offering_1.id,
                              networkids=[str(cls.network_1.id)]
                              )
        # Spawn an instance in that network
        cls.vm_2 = VirtualMachine.create(
                              cls.api_client,
                              cls.services["virtual_machine"],
                              accountid=cls.account.name,
                              domainid=cls.account.domainid,
                              serviceofferingid=cls.service_offering_1.id,
                              networkids=[str(cls.network_1.id)]
                              )

        cls.vm_3 = VirtualMachine.create(
                              cls.api_client,
                              cls.services["virtual_machine"],
                              accountid=cls.account.name,
                              domainid=cls.account.domainid,
                              serviceofferingid=cls.service_offering_2.id,
                              networkids=[str(cls.network_2.id)]
                              )

        cls.public_ip_static = PublicIPAddress.create(
                            cls.api_client,
                            accountid=cls.account.name,
                            zoneid=cls.zone.id,
                            domainid=cls.account.domainid,
                            networkid=cls.network_1.id,
                            vpcid=cls.vpc.id
                            )
        StaticNATRule.enable(
                          cls.api_client,
                          ipaddressid=cls.public_ip_static.ipaddress.id,
                          virtualmachineid=cls.vm_1.id,
                          networkid=cls.network_1.id
                          )

        cls.public_ip_1 = PublicIPAddress.create(
                            cls.api_client,
                            accountid=cls.account.name,
                            zoneid=cls.zone.id,
                            domainid=cls.account.domainid,
                            networkid=cls.network_1.id,
                            vpcid=cls.vpc.id
                            )

        cls.nat_rule = NATRule.create(
                              cls.api_client,
                              cls.vm_1,
                              cls.services["natrule"],
                              ipaddressid=cls.public_ip_1.ipaddress.id,
                              openfirewall=False,
                              networkid=cls.network_1.id,
                              vpcid=cls.vpc.id
                              )

        cls.public_ip_2 = PublicIPAddress.create(
                            cls.api_client,
                            accountid=cls.account.name,
                            zoneid=cls.zone.id,
                            domainid=cls.account.domainid,
                            networkid=cls.network_1.id,
                            vpcid=cls.vpc.id
                            )

        cls.lb_rule = LoadBalancerRule.create(
                                cls.api_client,
                                cls.services["lbrule"],
                                ipaddressid=cls.public_ip_2.ipaddress.id,
                                accountid=cls.account.name,
                                networkid=cls.network_1.id,
                                vpcid=cls.vpc.id,
                                domainid=cls.account.domainid
                            )
        cls.lb_rule.assign(cls.api_client, [cls.vm_1, cls.vm_2])

        # Opening up the ports in VPC
        cls.nwacl_nat = NetworkACL.create(
                                     cls.api_client,
                                     networkid=cls.network_1.id,
                                     services=cls.services["natrule"],
                                     traffictype='Ingress'
                                )

        cls.nwacl_lb = NetworkACL.create(
                            cls.api_client,
                            networkid=cls.network_1.id,
                            services=cls.services["lbrule"],
                            traffictype='Ingress'
                            )
        cls.services["icmp_rule"]["protocol"] = "all"
        cls.nwacl_internet = NetworkACL.create(
                                    cls.api_client,
                                    networkid=cls.network_1.id,
                                    services=cls.services["icmp_rule"],
                                    traffictype='Egress'
                                    )
        cls._cleanup = [
                    cls.service_offering_1,
                    cls.service_offering_2,
                    cls.nw_off,
                    cls.nw_off_no_lb,
                    ]

        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.account.delete(cls.api_client)
            wait_for_cleanup(cls.api_client, ["account.cleanup.interval"])
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

            # Waiting for network cleanup to delete vpc offering
            wait_for_cleanup(cls.api_client, ["network.gc.wait",
                                              "network.gc.interval"])
            cls.vpc_off.delete(cls.api_client)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            wait_for_cleanup(self.apiclient, [
                                              "network.gc.interval",
                                              "network.gc.wait"])

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def FindClusterWithSufficientHosts(cls, numberofhosts = 3):
        """ Find a cluster in the zone with given number of hosts
            or at most 1 less than the given number as the extra host
            is needed only for migrate"""

        clusters = Cluster.list(cls.api_client, zoneid=cls.zone.id)
        for cluster in clusters:
            hosts = Host.list(cls.api_client, clusterid=cluster.id)
            if len(hosts) >= (numberofhosts - 1):
                return cluster
        #end for
        return None


    def validate_vm_deployment(self):
        """Validates VM deployment on different hosts"""

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  networkid=self.network_1.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs shall return a valid response"
                         )
        host_1 = vms[0].hostid
        self.debug("Host for network 1: %s" % vms[0].hostid)

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  networkid=self.network_2.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs shall return a valid response"
                         )
        host_2 = vms[0].hostid
        self.debug("Host for network 2: %s" % vms[0].hostid)

        self.assertNotEqual(
                host_1,
                host_2,
                "Both the virtual machines should be deployed on diff hosts "
                )
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

    def validate_network_rules(self):
        """Validates if the network rules work properly or not?"""
        for ip in [self.public_ip_1.ipaddress.ipaddress, self.public_ip_2.ipaddress.ipaddress, self.public_ip_static.ipaddress.ipaddress]:
            try:
                self.debug("Checking if we can SSH into VM_1 through %s?" %
                        (ip))
                ssh = self.vm_1.get_ssh_client(
                                    ipaddress=ip,
                                    reconnect=True)

                self.assertNotEqual(ssh, None,
                                    "SSH client should be returned successfully")

                self.debug("SSH into VM is successfully")

                self.debug("Verifying if we can ping to outside world from VM?")
                # Ping to outsite world
                res = ssh.execute("ping -c 1 www.google.com")
                # res = 64 bytes from maa03s17-in-f20.1e100.net (74.125.236.212):
                # icmp_req=1 ttl=57 time=25.9 ms
                # --- www.l.google.com ping statistics ---
                # 1 packets transmitted, 1 received, 0% packet loss, time 0ms
                # rtt min/avg/max/mdev = 25.970/25.970/25.970/0.000 ms
                result = str(res)
                self.assertEqual(
                             result.count("1 received"),
                             1,
                             "Ping to outside world from VM should be successful"
                             )
            except Exception as e:
                self.fail("Failed to SSH into VM - %s, %s" %
                                        (ip, e))

        return

    @attr(tags=["advanced","multihost", "intervlan", "dvs"], required_hardware="true")
    def test_01_deploy_instance_in_network(self):
        """ Test deploy an instance in VPC networks
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1(10.1.1.1/24) and network2(10.1.2.1/24) to this VPC.
        # Steps:
        # 1. Deploy vm1 and vm2 in network1 and vm3 and vm4 in network2 using
        #    the default CentOS 6.2 Template

        self.validate_vm_deployment()
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
        return

    @attr(tags=["advanced","multihost", "intervlan", "dvs"], required_hardware="true")
    def test_02_stop_instance_in_network(self):
        """ Test stop an instance in VPC networks
        """

        # Validate the following
        # 1. Stop the virtual machines.
        # 2. Rules should be still configured on virtual router.

        self.debug("Stopping the virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_1.stop(self.apiclient)
            self.vm_2.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop the virtual instances, %s" % e)

        # Check if the network rules still exists after Vm stop
        self.debug("Checking if NAT rules ")
        nat_rules = NATRule.list(
                                 self.apiclient,
                                 id=self.nat_rule.id,
                                 listall=True
                                 )
        self.assertEqual(
                         isinstance(nat_rules, list),
                         True,
                         "List NAT rules shall return a valid list"
                         )

        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=self.lb_rule.id,
                                         listall=True
                                         )
        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List LB rules shall return a valid list"
                         )
        return

    @attr(tags=["advanced","multihost", "intervlan", "dvs"], required_hardware="true")
    def test_03_start_instance_in_network(self):
        """ Test start an instance in VPC networks
        """

        # Validate the following
        # 1. Start the virtual machines.
        # 2. Vm should be started successfully.
        # 3. Make sure that all the PF,LB and Static NAT rules on this VM
        #    works as expected.
        # 3. Make sure that we are able to access google.com from this user Vm

        self.debug("Starting the virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_1.start(self.apiclient)

            list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.vm_1.id
                                                 )

            vm_response = list_vm_response[0]

            self.assertEqual(
                    vm_response.state,
                    'Running',
                    "VM state should be running"
                    )

            self.vm_2.start(self.apiclient)

            list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.vm_2.id
                                                 )

            vm_response = list_vm_response[0]

            self.assertEqual(
                    vm_response.state,
                    'Running',
                    "VM state should be running"
                    )

        except Exception as e:
            self.fail("Failed to start the virtual instances, %s" % e)
        self.debug("Validating if the network rules work properly or not?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced","multihost", "intervlan", "dvs"], required_hardware="true")
    def test_04_reboot_instance_in_network(self):
        """ Test reboot an instance in VPC networks
        """

        # Validate the following
        # 1. Reboot the virtual machines.
        # 2. Vm should be started successfully.
        # 3. Make sure that all the PF,LB and Static NAT rules on this VM
        #    works as expected.
        # 3. Make sure that we are able to access google.com from this user Vm

        self.debug("Validating if the network rules work properly or not?")
        self.validate_network_rules()

        self.debug("Starting the virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_1.reboot(self.apiclient)
            self.vm_2.reboot(self.apiclient)
        except Exception as e:
            self.fail("Failed to reboot the virtual instances, %s" % e)

        self.debug("Validating if the network rules work properly or not?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced","multihost", "intervlan", "dvs"], required_hardware="true")
    def test_05_destroy_instance_in_network(self):
        """ Test destroy an instance in VPC networks
        """

        # Validate the following
        # 1. Destory the virtual machines.
        # 2. Rules should be still configured on virtual router.
        # 3. Recover the virtual machines.
        # 4. Vm should be in stopped state. State both the instances
        # 5. Make sure that all the PF,LB and Static NAT rules on this VM
        #    works as expected.
        # 6. Make sure that we are able to access google.com from this user Vm

        self.debug("Validating if the network rules work properly or not?")
        self.validate_network_rules()

        self.debug("Destroying the virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_1.delete(self.apiclient, expunge=False)

            list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.vm_1.id
                                                 )

            vm_response = list_vm_response[0]

            self.assertEqual(
                    vm_response.state,
                    'Destroyed',
                    "VM state should be destroyed"
                    )
            
        except Exception as e:
            self.fail("Failed to stop the virtual instances, %s" % e)
            
        # Check if the network rules still exists after Vm stop
        self.debug("Checking if NAT rules ")
        nat_rules = NATRule.list(
                                 self.apiclient,
                                 id=self.nat_rule.id,
                                 listall=True
                                 )
        self.assertEqual(
                         isinstance(nat_rules, list),
                         True,
                         "List NAT rules shall return a valid list"
                         )

        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=self.lb_rule.id,
                                         listall=True
                                         )
        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List LB rules shall return a valid list"
                         )

        self.debug("Recovering the expunged virtual machine vm1 in account: %s" %
                                                self.account.name)
        try:
            self.vm_1.recover(self.apiclient)

            list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.vm_1.id
                                                 )

            vm_response = list_vm_response[0]

            self.assertEqual(
                    vm_response.state,
                    'Stopped',
                    "VM state should be stopped"
                    )

        except Exception as e:
            self.fail("Failed to recover the virtual instances, %s" % e)
            
        try:
            self.vm_2.delete(self.apiclient, expunge=False)

            list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.vm_2.id
                                                 )

            vm_response = list_vm_response[0]

            self.assertEqual(
                    vm_response.state,
                    'Destroyed',
                    "VM state should be destroyed"
                    )

        except Exception as e:
            self.fail("Failed to stop the virtual instances, %s" % e)

        self.debug("Recovering the expunged virtual machine vm2 in account: %s" %
                                                self.account.name)            
        try:
            self.vm_2.recover(self.apiclient)

            list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.vm_2.id
                                                 )

            vm_response = list_vm_response[0]

            self.assertEqual(
                    vm_response.state,
                    'Stopped',
                    "VM state should be stopped"
                    )
        except Exception as e:
            self.fail("Failed to recover the virtual instances, %s" % e)

        self.debug("Starting the two instances..")
        try:
            self.vm_1.start(self.apiclient)

            list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.vm_1.id
                                                 )

            vm_response = list_vm_response[0]

            self.assertEqual(
                    vm_response.state,
                    'Running',
                    "VM state should be running"
                    )

            self.vm_2.start(self.apiclient)

            list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.vm_2.id
                                                 )

            vm_response = list_vm_response[0]

            self.assertEqual(
                    vm_response.state,
                    'Running',
                    "VM state should be running"
                    )
        except Exception as e:
            self.fail("Failed to start the instances, %s" % e)

        # Wait until vms are up
        time.sleep(120)
        self.debug("Validating if the network rules work properly or not?")
        self.validate_network_rules()

        return

    @attr(tags=["advanced","multihost", "intervlan", "dvs"], required_hardware="true")
    def test_06_migrate_instance_in_network(self):
        """ Test migrate an instance in VPC networks
        """

        # Validate the following
        # 1. Migrate the virtual machines to other hosts
        # 2. Vm should be in stopped state. State both the instances
        # 3. Make sure that all the PF,LB and Static NAT rules on this VM
        #    works as expected.
        # 3. Make sure that we are able to access google.com from this user Vm
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("vm migrate is not supported in %s" % self.hypervisor)

        self.debug("Validating if the network rules work properly or not?")
        self.validate_network_rules()

        host = findSuitableHostForMigration(self.apiclient, self.vm_1.id)
        if host is None:
            self.skipTest(ERROR_NO_HOST_FOR_MIGRATION)

        self.debug("Migrating VM-ID: %s to Host: %s" % (
                                                        self.vm_1.id,
                                                        host.id
                                                        ))

        try:
            self.vm_1.migrate(self.apiclient, hostid=host.id)
        except Exception as e:
            self.fail("Failed to migrate instance, %s" % e)

        self.debug("Validating if the network rules work properly or not?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced","multihost", "intervlan", "dvs"], required_hardware="true")
    def test_07_user_data(self):
        """ Test user data in virtual machines
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1(10.1.1.1/24) and network2(10.1.2.1/24) to this VPC.
        # 3. Deploy a vm in network1 and a vm in network2 using userdata
        # Steps
        # 1.Query for the user data for both the user vms from both networks
        #   User should be able to query the user data for the vms belonging to
        #   both the networks from the VR
        self.debug("Checking if we can SSH into VM_1 through %s" %
                    (self.public_ip_2.ipaddress.ipaddress))
        try:
            ssh = self.vm_1.get_ssh_client(
                                ipaddress=self.public_ip_2.ipaddress.ipaddress,
                                reconnect=True)

            self.assertNotEqual(ssh, None,
                                "get_ssh_client should return ssh handle")

            self.debug("SSH into VM is successfully")
            ssh.execute("yum install wget -y")
        except Exception as e:
            self.fail("Failed to SSH into instance: %s" % e)

        self.debug("check the userdata with that of present in router")
        try:
            cmds = [
               "wget http://%s/latest/user-data" % self.network_1.gateway,
               "cat user-data",
               ]
            for c in cmds:
                result = ssh.execute(c)
                self.debug("%s: %s" % (c, result))
        except Exception as e:
            self.fail("Failed to SSH in Virtual machine: %s" % e)

        res = str(result)
        self.assertEqual(
                            res.count(
                                self.services["virtual_machine"]["userdata"]),
                            1,
                            "Verify user data from router"
                        )
        return

    @attr(tags=["advanced","multihost", "intervlan", "dvs"], required_hardware="true")
    def test_08_meta_data(self):
        """ Test meta data in virtual machines
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1(10.1.1.1/24) and network2(10.1.2.1/24) to this VPC.
        # 3. Deploy a vm in network1 and a vm in network2 using userdata
        # Steps
        # 1.Query for the meta data for both the user vms from both networks
        #   User should be able to query the user data for the vms belonging to
        #   both the networks from the VR
        self.debug("Checking if we can SSH into VM_1 through %s" %
                    (self.public_ip_2.ipaddress.ipaddress))
        try:
            ssh = self.vm_1.get_ssh_client(
                                ipaddress=self.public_ip_2.ipaddress.ipaddress,
                                reconnect=True)

            self.assertNotEqual(ssh, None,
                                "get_ssh_client should return ssh handle")

            self.debug("SSH into VM is successfully")
        except Exception as e:
            self.fail("Failed to SSH into instance: %s" % e)

        self.debug("check the metadata with that of present in router")
        try:
            cmds = [
               "wget http://%s/latest/vm-id" % self.network_1.gateway,
               "cat vm-id",
               ]
            for c in cmds:
                result = ssh.execute(c)
                self.debug("%s: %s" % (c, result))
        except Exception as e:
            self.fail("Failed to SSH in Virtual machine: %s" % e)

        res = str(result)
        self.assertNotEqual(
                         res,
                         None,
                         "Meta data should be returned from router"
                        )
        return

    @attr(tags=["advanced","multihost", "intervlan", "dvs"], required_hardware="true")
    def test_09_expunge_instance_in_network(self):
        """ Test expunge an instance in VPC networks
        """

        # Validate the following
        # 1. Recover the virtual machines.
        # 2. Vm should be in stopped state. State both the instances
        # 3. Make sure that all the PF,LB and Static NAT rules on this VM
        #    works as expected.
        # 3. Make sure that we are able to access google.com from this user Vm

        self.debug("Validating if the network rules work properly or not?")
        self.validate_network_rules()

        self.debug("Delete virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_1.delete(self.apiclient)
            self.vm_2.delete(self.apiclient)
            self.vm_3.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to destroy the virtual instances, %s" % e)

        # Check if the network rules still exists after Vm stop
        self.debug("Checking if NAT rules existed")
        with self.assertRaises(Exception):
            NATRule.list(
                         self.apiclient,
                         id=self.nat_rule.id,
                         listall=True
                        )

            LoadBalancerRule.list(
                                  self.apiclient,
                                  id=self.lb_rule.id,
                                  listall=True
                                 )
        return
