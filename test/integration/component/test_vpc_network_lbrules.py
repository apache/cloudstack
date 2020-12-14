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

""" Component tests for VPC network functionality - Load Balancing Rules
"""
#Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.base import (stopRouter,
                                        startRouter,
                                        Account,
                                        VpcOffering,
                                        VPC,
                                        ServiceOffering,
                                        NATRule,
                                        NetworkACL,
                                        PublicIPAddress,
                                        NetworkOffering,
                                        Network,
                                        VirtualMachine,
                                        LoadBalancerRule,
                                        StaticNATRule)
from marvin.lib.common import (get_domain,
                                        get_zone,
                                        get_template,
                                        list_routers)
from marvin.lib.utils import cleanup_resources
import socket
import time

class Services:
    """Test VPC network services Load Balancing Rules Test data
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
                        "host1":None,
                        "host2":None,
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
                                    "supportedservices": 'Vpn,Dhcp,Dns,SourceNat,PortForwarding,Lb,UserData,StaticNat,NetworkACL',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "useVpc": 'on',
                                    "serviceProviderList": {
                                            "Vpn": 'VpcVirtualRouter',
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
                                    "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Lb,UserData,StaticNat',
                                },
                        "vpc": {
                                "name": "TestVPC",
                                "displaytext": "TestVPC",
                                "cidr": '10.0.0.1/24'
                                },
                        "network": {
                                "name": "Test Network",
                                "displaytext": "Test Network",
                                "netmask": '255.255.255.0'
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
                        "lbrule_http": {
                                    "name": "HTTP",
                                    "alg": "leastconn",
                                    # Algorithm used for load balancing
                                    "privateport": 80,
                                    "publicport": 80,
                                    "openfirewall": False,
                                    "startport": 80,
                                    "endport": 80,
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
                        "http_rule": {
                                    "startport": 80,
                                    "endport": 80,
                                    "cidrlist": '0.0.0.0/0',
                                    "protocol": "TCP"
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
                        "sleep": 60,
                        "timeout": 10,
                    }

class TestVPCNetworkLBRules(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        # We want to fail quicker if it's failure
        socket.setdefaulttimeout(60)

        cls.testClient = super(TestVPCNetworkLBRules, cls).getClsTestClient()
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
        cls._cleanup = [cls.service_offering]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            print(("Warning: Exception during cleanup : %s" % e))
            #raise Exception("Warning: Exception during cleanup : %s" % e)
        return


    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.account = Account.create(
                                    self.apiclient,
                                    self.services["account"],
                                    admin=True,
                                    domainid=self.domain.id
                                    )
        self.cleanup = [self.account]
        self.debug("Creating a VPC offering..")
        self.vpc_off = VpcOffering.create(
                                    self.apiclient,
                                    self.services["vpc_offering"]
                                    )

        self.cleanup.append(self.vpc_off)
        self.debug("Enabling the VPC offering created")
        self.vpc_off.update(self.apiclient, state='Enabled')

        self.debug("Creating a VPC network in the account: %s" % self.account.name)
        self.services["vpc"]["cidr"] = '10.1.1.1/16'
        self.vpc = VPC.create(
                        self.apiclient,
                        self.services["vpc"],
                        vpcofferingid=self.vpc_off.id,
                        zoneid=self.zone.id,
                        account=self.account.name,
                        domainid=self.account.domainid
                        )
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning: Exception during cleanup : %s" % e)
            #raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def get_Router_For_VPC(self):
        routers = list_routers(self.apiclient,
                            account=self.account.name,
                            domainid=self.account.domainid,
                            )
        self.assertEqual(isinstance(routers, list),
                        True,
                        "Check for list routers response return valid data"
                        )
        self.assertNotEqual(len(routers),
                            0,
                            "Check list router response"
                            )
        router = routers[0]
        return router


    def stop_VPC_VRouter(self):
        router = self.get_Router_For_VPC()
        self.debug("Stopping router ID: %s" % router.id)
        cmd = stopRouter.stopRouterCmd()
        cmd.id = router.id
        self.apiclient.stopRouter(cmd)

        routers = list_routers(self.apiclient,
                            account=self.account.name,
                            domainid=self.account.domainid,
                            )
        self.assertEqual(isinstance(routers, list),
                        True,
                        "Check for list routers response return valid data"
                        )
        router = routers[0]
        self.assertEqual(router.state,
                    'Stopped',
                    "Check list router response for router state"
                    )
        return router

    def start_VPC_VRouter(self, router):
        # Start the VPC Router
        self.debug("Starting router ID: %s" % router.id)
        cmd = startRouter.startRouterCmd()
        cmd.id = router.id
        self.apiclient.startRouter(cmd)

        routers = list_routers(self.apiclient,
                            account=self.account.name,
                            domainid=self.account.domainid,
                            zoneid=self.zone.id
                            )
        self.assertEqual(isinstance(routers, list),
                        True,
                        "Check for list routers response return valid data"
                        )
        router = routers[0]
        self.assertEqual(router.state,
                    'Running',
                    "Check list router response for router state"
                    )

    def check_ssh_into_vm(self, vm, public_ip, testnegative=False):
        self.debug("Checking if we can SSH into VM=%s on public_ip=%s" % (vm.name, public_ip.ipaddress.ipaddress))
        try:
            vm.get_ssh_client(ipaddress=public_ip.ipaddress.ipaddress)
            if not testnegative:
                self.debug("SSH into VM=%s on public_ip=%s is successfully" % (vm.name, public_ip.ipaddress.ipaddress))
            else:
                self.fail("SSH into VM=%s on public_ip=%s is successfully" % (vm.name, public_ip.ipaddress.ipaddress))
        except:
            if not testnegative:
                self.fail("Failed to SSH into VM - %s" % (public_ip.ipaddress.ipaddress))
            else:
                self.debug("Failed to SSH into VM - %s" % (public_ip.ipaddress.ipaddress))

    def check_wget_from_vm(self, vm, public_ip, testnegative=False):
        import urllib.request, urllib.parse, urllib.error
        self.debug("Checking if we can wget from a VM=%s http server on public_ip=%s"  % (vm.name, public_ip.ipaddress.ipaddress))
        try:
            urllib.request.urlretrieve("http://%s/test.html" % public_ip.ipaddress.ipaddress, filename="test.html")
            if not testnegative:
                self.debug("Successful to wget from VM=%s http server on public_ip=%s" % (vm.name, public_ip.ipaddress.ipaddress))
            else:
                self.fail("Successful to wget from VM=%s http server on public_ip=%s" % (vm.name, public_ip.ipaddress.ipaddress))
        except Exception as e:
            if not testnegative:
                self.fail("Failed to wget from VM=%s http server on public_ip=%s because of %s" % (vm.name, public_ip.ipaddress.ipaddress, e))
            else:
                self.debug("Failed to wget from VM=%s http server on public_ip=%s because of %s" % (vm.name, public_ip.ipaddress.ipaddress, e))

    def create_StaticNatRule_For_VM(self, vm, public_ip, network):
        self.debug("Enabling static NAT for IP: %s" %
                                            public_ip.ipaddress.ipaddress)
        try:
            StaticNATRule.enable(
                            self.apiclient,
                            ipaddressid=public_ip.ipaddress.id,
                            virtualmachineid=vm.id,
                            networkid=network.id
                            )
            self.debug("Static NAT enabled for IP: %s" %
                                            public_ip.ipaddress.ipaddress)
        except Exception as e:
            self.fail("Failed to enable static NAT on IP: %s - %s" % (
                                        public_ip.ipaddress.ipaddress, e))

    def create_NatRule_For_VM(self, vm, public_ip, network):
        self.debug("Creatinng NAT rule in network for vm with public IP")
        nat_rule = NATRule.create(self.apiclient,
                                vm,
                                self.services["natrule"],
                                ipaddressid=public_ip.ipaddress.id,
                                openfirewall=False,
                                networkid=network.id,
                                vpcid=self.vpc.id
                                )

        self.debug("Adding NetwrokACl rules to make NAT rule accessible")
        nwacl_nat = NetworkACL.create(self.apiclient,
                                    networkid=network.id,
                                    services=self.services["natrule"],
                                    traffictype='Ingress'
                                    )
        self.debug('nwacl_nat=%s' % nwacl_nat.__dict__)
        return nat_rule

    def acquire_Public_IP(self, network):
        self.debug("Associating public IP for network: %s" % network.name)
        public_ip = PublicIPAddress.create(self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=None, #network.id,
                                vpcid=self.vpc.id
                                )
        self.debug("Associated %s with network %s" % (public_ip.ipaddress.ipaddress,
                                        network.id
                                        ))
        return public_ip

    def create_VPC(self, cidr='10.1.2.1/16'):
        self.debug("Creating a VPC offering..")
        self.services["vpc_offering"]["name"] = self.services["vpc_offering"]["name"] + str(cidr)
        vpc_off = VpcOffering.create(
                                    self.apiclient,
                                    self.services["vpc_offering"]
                                    )

        self.cleanup.append(vpc_off)
        self.debug("Enabling the VPC offering created")
        vpc_off.update(self.apiclient, state='Enabled')

        self.debug("Creating a VPC network in the account: %s" % self.account.name)
        self.services["vpc"]["cidr"] = cidr
        vpc = VPC.create(
                        self.apiclient,
                        self.services["vpc"],
                        vpcofferingid=vpc_off.id,
                        zoneid=self.zone.id,
                        account=self.account.name,
                        domainid=self.account.domainid
                        )
        return vpc

    def create_Network(self, net_offerring, gateway='10.1.1.1',vpc=None):
        try:
            self.debug('Create NetworkOffering')
            net_offerring["name"] = "NET_OFF-" + str(gateway)
            nw_off = NetworkOffering.create(self.apiclient,
                                            net_offerring,
                                            conservemode=False
                                        )
            # Enable Network offering
            nw_off.update(self.apiclient, state='Enabled')
            self.cleanup.append(nw_off)
            self.debug('Created and Enabled NetworkOffering')

            self.services["network"]["name"] = "NETWORK-" + str(gateway)
            self.debug('Adding Network=%s' % self.services["network"])
            obj_network = Network.create(self.apiclient,
                                    self.services["network"],
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    networkofferingid=nw_off.id,
                                    zoneid=self.zone.id,
                                    gateway=gateway,
                                    vpcid=vpc.id if vpc else self.vpc.id
                                    )
            self.debug("Created network with ID: %s" % obj_network.id)
            return obj_network
        except:
            self.fail('Unable to create a Network with offering=%s' % net_offerring)

    def create_VM_in_Network(self, network, host_id=None):
        try:
            self.debug('Creating VM in network=%s' % network.name)
            vm = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    networkids=[str(network.id)],
                                    hostid=host_id
                                    )
            self.debug('Created VM=%s in network=%s' % (vm.id, network.name))

            return vm
        except:
            self.fail('Unable to create VM in a Network=%s' % network.name)

    def create_LB_Rule(self, public_ip, network, vmarray, services=None):
        self.debug("Creating LB rule for IP address: %s" %
                                        public_ip.ipaddress.ipaddress)
        objservices = None
        if services:
            objservices = services
        else:
            objservices = self.services["lbrule"]

        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    objservices,
                                    ipaddressid=public_ip.ipaddress.id,
                                    accountid=self.account.name,
                                    networkid=network.id,
                                    vpcid=self.vpc.id,
                                    domainid=self.account.domainid
                                )
        self.debug("Adding virtual machines %s and %s to LB rule" % (vmarray[0], vmarray[1]))
        lb_rule.assign(self.apiclient, vmarray)

        self.debug("Adding NetworkACl rules to make NAT rule accessible")
        nwacl_nat = NetworkACL.create(self.apiclient,
                                    objservices,
                                    networkid=network.id,
                                    traffictype='Ingress'
                                    )
        self.debug('nwacl_nat=%s' % nwacl_nat.__dict__)
        return lb_rule

    def create_egress_Internet_Rule(self, network):
        self.debug("Adding Egress rules to network %s and %s to allow access to internet" % (network.name,self.services["http_rule"]))
        nwacl_internet_1 = NetworkACL.create(
                                self.apiclient,
                                networkid=network.id,
                                services=self.services["http_rule"],
                                traffictype='Egress'
                                )

        return nwacl_internet_1

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_01_VPC_LBRulesListing(self):
        """ Test case no 210 and 227: List Load Balancing Rules belonging to a VPC
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 4. Add network2(10.1.2.1/24) using N01 to this VPC.
        # 5. Deploy vm1 and vm2 in network1.
        # 6. Deploy vm3 and vm4 in network2.
        # 7. Use the Create LB rule for vm1 and vm2 in network1.
        # 8. Use the Create LB rule for vm3 amd vm4 in network2, should fail
        # because it's no_lb offering
        # 9. List LB rule

        network_1 = self.create_Network(self.services["network_offering"])
        network_2 = self.create_Network(self.services["network_offering_no_lb"], '10.1.2.1')
        self.debug("deploying VMs in network: %s" % network_2.name)
        vm_1 = self.create_VM_in_Network(network_1)
        vm_2 = self.create_VM_in_Network(network_1)
        vm_3 = self.create_VM_in_Network(network_2)
        vm_4 = self.create_VM_in_Network(network_2)
        public_ip_1 = self.acquire_Public_IP(network_1)
        lb_rule1 = self.create_LB_Rule(public_ip_1, network_1, [vm_1, vm_2]) #
        public_ip_2 = self.acquire_Public_IP(network_2)
        with self.assertRaises(Exception):
            self.create_LB_Rule(public_ip_2, network_2, [vm_3, vm_4])

        lb_rules = LoadBalancerRule.list(self.apiclient,
                                        id=lb_rule1.id,
                                        listall=True
                                        )
        self.assertNotEqual(lb_rules,
                        None,
                        "Failed to list the LB Rule"
                        )
        vms = VirtualMachine.list(self.apiclient,
                                networkid=network_1.id,
                                listall=True
                                )
        self.assertNotEqual(vms,
                        None,
                        "Failed to list the VMs in network=%s" % network_1.name
                        )
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_02_VPC_CreateLBRuleInMultipleNetworks(self):
        """ Test Create LB rules for 1 network which is part of a two/multiple virtual networks of a
            VPC using a new Public IP Address available with the VPC when the Virtual Router is in Running State
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 4. Deploy vm1, vm2 and vm3 in network1 on primary host.
        # 5. Use the Create LB rule for vm1 and vm2 in network1.
        # 6. Add vm3 to LB rule.
        # 7. wget a file and check for LB rule.

        network_1 = self.create_Network(self.services["network_offering"])
        vm_1 = self.create_VM_in_Network(network_1)
        vm_2 = self.create_VM_in_Network(network_1)
        vm_3 = self.create_VM_in_Network(network_1)
        public_ip_1 = self.acquire_Public_IP(network_1)
        lb_rule = self.create_LB_Rule(public_ip_1, network_1, [vm_1, vm_2], self.services["lbrule_http"])
        lb_rule.assign(self.apiclient, [vm_3])
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=False)
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_03_VPC_CreateLBRuleInMultipleNetworksVRStoppedState(self):
        """ Test case no 222 : Create LB rules for a two/multiple virtual networks of a 
            VPC using a new Public IP Address available with the VPC when the Virtual Router is in Stopped State
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 4. Add network2(10.1.2.1/24) using N01 to this VPC.
        # 5. Deploy vm1, vm2 and vm3 in network1 on primary host.
        # 7. Use the Create LB rule for vm1 and vm2 in network1.
        # 8. Add vm3 to LB rule.
        # 9. wget a file and check for LB rule.

        network_1 = self.create_Network(self.services["network_offering"])
        network_2 = self.create_Network(self.services["network_offering_no_lb"], '10.1.2.1')
        vm_1 = self.create_VM_in_Network(network_1)
        vm_2 = self.create_VM_in_Network(network_1)
        vm_3 = self.create_VM_in_Network(network_1)

        # wait until VM is up before stop the VR
        time.sleep(120)

        router = self.stop_VPC_VRouter()

        public_ip_1 = self.acquire_Public_IP(network_1)
        lb_rule = self.create_LB_Rule(public_ip_1, network_1, [vm_1, vm_2], self.services["lbrule_http"])
        lb_rule.assign(self.apiclient, [vm_3])

        self.start_VPC_VRouter(router)

        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=False)
        return    

    @attr(tags=["advanced","advancedns", "intervlan"], required_hardware="true")
    def test_04_VPC_CreateLBRuleInMultipleNetworksVRStoppedState(self):
        """ Test case no 222 : Create LB rules for a two/multiple virtual networks of a
            VPC using a new Public IP Address available with the VPC when the Virtual Router is in Stopped State
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 4. Add network2(10.1.2.1/24) using N01 to this VPC.
        # 5. Deploy vm1, vm2 and vm3 in network1 on primary host.
        # 7. Use the Create LB rule for vm1 and vm2 in network1.
        # 8. Add vm3 to LB rule.
        # 9. wget a file and check for LB rule.

        network_1 = self.create_Network(self.services["network_offering"])
        network_2 = self.create_Network(self.services["network_offering_no_lb"], '10.1.2.1')
        vm_1 = self.create_VM_in_Network(network_1)
        vm_2 = self.create_VM_in_Network(network_1)
        vm_3 = self.create_VM_in_Network(network_2)
        public_ip_1 = self.acquire_Public_IP(network_1)
        lb_rule = self.create_LB_Rule(public_ip_1, network_1, [vm_1, vm_2], self.services["lbrule_http"])
        # In a VPC, the load balancing service is supported only on a single tier.
        # http://cloudstack.apache.org/docs/en-US/Apache_CloudStack/4.0.2/html/Installation_Guide/configure-vpc.html
        with self.assertRaises(Exception):
            lb_rule.assign(self.apiclient, [vm_3])
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=False)
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_05_VPC_CreateAndDeleteLBRule(self):
        """ Test case no 214 : Delete few(not all) LB rules for a single virtual network of a
            VPC belonging to a single Public IP Address when the Virtual Router is in Running State
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 4. Add network2(10.1.2.1/24) using N01 to this VPC.
        # 5. Deploy vm1, vm2 and vm3 in network1 on primary host.
        # 6. Use the Create LB rule for http and ssh vm1, vm2 and vm3 in network1.
        # 7. wget and ssh and check for LB rule.
        # 8. Delete ssh LB Rule.
        # 9. ssh LB should fail.

        network_1 = self.create_Network(self.services["network_offering"])
        vm_1 = self.create_VM_in_Network(network_1)
        vm_2 = self.create_VM_in_Network(network_1)
        vm_3 = self.create_VM_in_Network(network_1)
        public_ip_1  = self.acquire_Public_IP(network_1)
        lb_rule_http = self.create_LB_Rule(public_ip_1, network_1, [vm_1, vm_2, vm_3], self.services["lbrule_http"])
        lb_rule_nat  = self.create_LB_Rule(public_ip_1, network_1, [vm_1, vm_2, vm_3])
        self.debug('lb_rule_http=%s' % lb_rule_http.__dict__)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=False)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        lb_rule_nat.delete(self.apiclient)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=True)
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_06_VPC_CreateAndDeleteLBRuleVRStopppedState(self):
        """ Test Delete few(not all) LB rules for a single virtual network of
            a VPC belonging to a single Public IP Address when the Virtual Router is in Stopped State
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 4. Add network2(10.1.2.1/24) using N01 to this VPC.
        # 5. Deploy vm1, vm2 and vm3 in network1 on primary host.
        # 6. Use the Create LB rule for http and ssh vm1, vm2 and vm3 in network1.
        # 7. wget and ssh and check for LB rule.
        # 8. Delete ssh LB Rule.
        # 9. ssh LB should fail.

        network_1 = self.create_Network(self.services["network_offering"])
        vm_1 = self.create_VM_in_Network(network_1)
        vm_2 = self.create_VM_in_Network(network_1)
        vm_3 = self.create_VM_in_Network(network_1)
        # wait until VM is up before stop the VR
        time.sleep(120)

        router = self.stop_VPC_VRouter()
        public_ip_1  = self.acquire_Public_IP(network_1)
        lb_rule_http = self.create_LB_Rule(public_ip_1, network_1, [vm_1, vm_2, vm_3], self.services["lbrule_http"])
        lb_rule_nat  = self.create_LB_Rule(public_ip_1, network_1, [vm_1, vm_2, vm_3])
        self.debug('lb_rule_http=%s' % lb_rule_http.__dict__)
        self.start_VPC_VRouter(router)

        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=False)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        lb_rule_nat.delete(self.apiclient)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=True)
        return    

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_07_VPC_CreateAndDeleteAllLBRule(self):
        """ Test Delete all LB rules for a single virtual network of a
            VPC belonging to a single Public IP Address when the Virtual Router is in Running State
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 4. Add network2(10.1.2.1/24) using N01 to this VPC.
        # 5. Deploy vm1, vm2 and vm3 in network1 on primary host.
        # 6. Use the Create LB rule for http and ssh vm1, vm2 and vm3 in network1.
        # 7. wget and ssh and check for LB rule.
        # 8. Delete all LB Rule.
        # 9. ssh and http LB should fail.

        network_1 = self.create_Network(self.services["network_offering"])
        vm_1 = self.create_VM_in_Network(network_1)
        vm_2 = self.create_VM_in_Network(network_1)
        vm_3 = self.create_VM_in_Network(network_1)
        public_ip_1  = self.acquire_Public_IP(network_1)
        lb_rule_http = self.create_LB_Rule(public_ip_1, network_1, [vm_1, vm_2, vm_3], self.services["lbrule_http"])
        lb_rule_nat  = self.create_LB_Rule(public_ip_1, network_1, [vm_1, vm_2, vm_3])
        self.debug('lb_rule_http=%s' % lb_rule_http.__dict__)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=False)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        lb_rule_nat.delete(self.apiclient)
        lb_rule_http.delete(self.apiclient)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=True)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=True)
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_08_VPC_CreateAndDeleteAllLBRuleVRStoppedState(self):
        """ Test Delete all LB rules for a single virtual network of a
            VPC belonging to a single Public IP Address when the Virtual Router is in Stopped State
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 4. Add network2(10.1.2.1/24) using N01 to this VPC.
        # 5. Deploy vm1, vm2 and vm3 in network1 on primary host.
        # 6. Use the Create LB rule for http and ssh vm1, vm2 and vm3 in network1.
        # 7. wget and ssh and check for LB rule.
        # 8. Delete all LB Rule.
        # 9. ssh and http LB should fail.

        network_1 = self.create_Network(self.services["network_offering"])
        vm_1 = self.create_VM_in_Network(network_1)
        vm_2 = self.create_VM_in_Network(network_1)
        vm_3 = self.create_VM_in_Network(network_1)
        public_ip_1  = self.acquire_Public_IP(network_1)
        lb_rule_http = self.create_LB_Rule(public_ip_1, network_1, [vm_1, vm_2, vm_3], self.services["lbrule_http"])
        lb_rule_nat  = self.create_LB_Rule(public_ip_1, network_1, [vm_1, vm_2, vm_3])
        self.debug('lb_rule_http=%s' % lb_rule_http.__dict__)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=False)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        lb_rule_nat.delete(self.apiclient)
        lb_rule_http.delete(self.apiclient)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=True)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=True)
        return
    
    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_09_VPC_LBRuleCreateFailMultipleVPC(self):
        """ Test User should not be allowed to create a LB rule for a VM that belongs to a different VPC.
        """

        # Validate the following
        # 1. Create a VPC1 with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC1.
        # 1. Create a VPC2 with cidr - 10.1.2.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 4. Add network2(10.1.2.1/24) using N01 to this VPC2.
        # 5. Deploy vm1 and vm2 in network1 on primary host.
        # 6. Deploy vm3 and vm4 in network2 on secondary host.
        # 7. Use the Create LB rule for vm1 and vm2 in network1.
        # 9. wget and check LB Rule
        # 10. create LB rule for vm3 and vm4 in VPC1
        # 11. LB rule creation should fail

        network_1 = self.create_Network(self.services["network_offering"])

        vm_1 = self.create_VM_in_Network(network_1)
        vm_2 = self.create_VM_in_Network(network_1)
        vpc2 = self.create_VPC()
        network_2 = self.create_Network(self.services["network_offering_no_lb"], '10.1.2.1',vpc2)
        vm_3 = self.create_VM_in_Network(network_2)
        vm_4 = self.create_VM_in_Network(network_2)
        public_ip_1 = self.acquire_Public_IP(network_1)
        lb_rule = self.create_LB_Rule(public_ip_1, network_1, [vm_1, vm_2], self.services["lbrule_http"])
        self.debug('lb_rule=%s' % lb_rule.__dict__)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=False)
        try:
            lb_rule = self.create_LB_Rule(public_ip_1, network_1, [vm_3, vm_4], self.services["lbrule_http"])
            self.fail('Successfully created LB rule for vm_3, vm_4 in network1')
        except:
            self.debug('Failed to Create LB rule vm_3 and vm_4')
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_10_VPC_FailedToCreateLBRuleNonVPCNetwork(self):
        """ Test User should not be allowed to create a LB rule for a VM that does not belong to any VPC.
        """

        # Validate the following
        # 1. Create a VPC1 with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC1.
        # 2. Create a Network offering - NO1 with all supported services
        # 4. Add network2(10.1.2.1/24) using N01 to this VPC1.
        # 5. Deploy vm1 and vm3 in network1 and network 2 on primary host.
        # 6. Deploy vm2 and vm4 in network2 and network 3 on secondary host.
        # 7. Use the Create LB rule for vm1 and vm2 in network1.
        # 9. wget and check LB Rule
        # 10. create LB rule for vm3 and vm4 in VPC1
        # 11. LB rule creation should fail

        network_1 = self.create_Network(self.services["network_offering"])
        vm_1 = self.create_VM_in_Network(network_1)
        vm_2 = self.create_VM_in_Network(network_1)
        network_2 = self.create_Network(self.services["network_offering_no_lb"], '10.1.2.1')
        vm_3 = self.create_VM_in_Network(network_2)
        network_3 = self.create_Network(self.services["network_offering_no_lb"], '10.1.3.1')
        vm_4 = self.create_VM_in_Network(network_3)
        self.debug('vm_4=%s' % vm_4.id)
        public_ip_1 = self.acquire_Public_IP(network_1)
        lb_rule = self.create_LB_Rule(public_ip_1, network_1, [vm_1, vm_2], self.services["lbrule_http"])
        self.debug('lb_rule=%s' % lb_rule.__dict__)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=True)
        try:
            lb_rule = self.create_LB_Rule(public_ip_1, network_2, [vm_3, vm_4], self.services["lbrule_http"])
            self.fail('Successfully created LB rule for vm_3, vm_4 in network2')
        except:
            self.debug('Failed to Create LB rule vm_3 and vm_4 in network2')
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_11_VPC_LBRuleCreateNotAllowed(self):
        """ Test case no 217 and 236: User should not be allowed to create a LB rule for a
            VM that does not belong to the same network but belongs to the same VPC.
        """

        # Validate the following
        # 1. Create a VPC1 with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC1.
        # 2. Create a Network offering - NO1 with all supported services
        # 4. Add network2(10.1.2.1/24) using N01 to this VPC1.
        # 5. Deploy vm1 and vm3 in network1 and network 2 primary host.
        # 6. Deploy vm2 and vm4 in network1 and network 2 on secondary host.
        # 7. Use the Create LB rule for vm1 and vm2 in network1.
        # 9. wget and check LB Rule
        # 10. create LB rule for vm3 and vm1 in VPC1
        # 11. LB rule creation should fail

        network_1 = self.create_Network(self.services["network_offering"])

        vm_1 = self.create_VM_in_Network(network_1)
        vm_2 = self.create_VM_in_Network(network_1)
        network_2 = self.create_Network(self.services["network_offering_no_lb"], '10.1.2.1')
        vm_3 = self.create_VM_in_Network(network_2)
        vm_4 = self.create_VM_in_Network(network_2)
        self.debug('vm_4=%s' % vm_4.id)
        public_ip_1 = self.acquire_Public_IP(network_1)
        lb_rule = self.create_LB_Rule(public_ip_1, network_1, [vm_1, vm_2], self.services["lbrule_http"])
        self.debug('lb_rule=%s' % lb_rule.__dict__)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=False)
        try:
            lb_rule = self.create_LB_Rule(public_ip_1, network_1, [vm_3, vm_1], self.services["lbrule_http"])
            self.fail('Successfully created LB rule for vm_3, vm_1 in network1')
        except:
            self.debug('Failed to Create LB rule vm_3 and vm_1')
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_12_VPC_LBRuleCreateFailForRouterIP(self):
        """ Test User should not be allowed to create a LB rule on an Ipaddress that Source Nat enabled.
        """

        # Validate the following
        # 1. Create a VPC1 with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC1.
        # 2. Create a Network offering - NO1 with all supported services
        # 5. Deploy vm1 and vm2 in network1 and network 2 primary host.
        # 6. Get source NAT public ip of router
        # 7. Use the Create LB rule for vm1 and vm2 in network1.
        # 8. LB rule creation should fail

        network_1 = self.create_Network(self.services["network_offering"])
        vm_1 = self.create_VM_in_Network(network_1)
        vm_2 = self.create_VM_in_Network(network_1)
        router = self.get_Router_For_VPC()
        public_ip_1 = router.publicip
        self.debug('router.publicip=%s' % public_ip_1)
        try:
            lb_rule = self.create_LB_Rule(public_ip_1, network_1, [vm_2, vm_1])
            self.fail('Successfully created LB rule for vm_2, vm_1 in network1 %s=' % lb_rule.__dict__)
        except:
            self.debug('Failed to Create LB rule vm_2 and vm_1')
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_13_VPC_LBRuleCreateFailForPFSourceNATIP(self):
        """ Test User should not be allowed to create a LB rule on an Ipaddress that already has a PF rule.
        """

        # Validate the following
        # 1. Create a VPC1 with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC1.
        # 2. Create a Network offering - NO1 with all supported services
        # 5. Deploy vm1 and vm2 in network1 and network 2 primary host.
        # 6. aquire public ip address
        # 6. Create a PP rule for vm1
        # 7. Use the Create LB rule for vm1 and vm2 in network1.
        # 8. LB rule creation should fail

        network_1 = self.create_Network(self.services["network_offering"])
        vm_1 = self.create_VM_in_Network(network_1)
        vm_2 = self.create_VM_in_Network(network_1)
        public_ip_1 = self.acquire_Public_IP(network_1)
        nat_rule1  = self.create_NatRule_For_VM(vm_1, public_ip_1, network_1)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        self.debug('nat_rule1=%s' % nat_rule1.__dict__)
        try:
            lb_rule = self.create_LB_Rule(public_ip_1, network_1, [vm_2, vm_1])
            self.fail('Successfully created LB rule for vm_2, vm_1 in network1 %s=' % lb_rule.__dict__)
        except:
            self.debug('Failed to Create LB rule vm_2 and vm_1')
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_14_VPC_LBRuleCreateFailForStaticNatRule(self):
        """ Test User should not be allowed to create a LB rule on an Ipaddress that already has a Static Nat rule.
        """

        # Validate the following
        # 1. Create a VPC1 with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC1.
        # 2. Create a Network offering - NO1 with all supported services
        # 5. Deploy vm1 and vm2 in network1 and network 2 primary host.
        # 6. aquire public ip address.
        # 7. Create a StaticNat Rule rule for vm1.
        # 8. Succesessfully wget a file from vm1.
        # 9. Use the Create LB rule for vm1 and vm2 in network1.
        # 10. LB rule creation should fail.

        network_1 = self.create_Network(self.services["network_offering"])
        vm_1 = self.create_VM_in_Network(network_1)
        vm_2 = self.create_VM_in_Network(network_1)
        public_ip_1 = self.acquire_Public_IP(network_1)
        self.create_StaticNatRule_For_VM(vm_1, public_ip_1, network_1)
        try:
            lb_rule = self.create_LB_Rule(public_ip_1, network_1, [vm_2, vm_1])
            self.fail('Successfully created LB rule for vm_2, vm_1 in network1 %s=' % lb_rule.__dict__)
        except:
            self.debug('Failed to Create LB rule vm_2 and vm_1')
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="false")
    def test_15_VPC_ReleaseIPForLBRuleCreated(self):
        """ Test release Ip address that has a LB rule assigned to it.
        """

        # Validate the following
        # 1. Create a VPC1 with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC1.
        # 2. Create a Network offering - NO1 with all supported services
        # 5. Deploy vm1 and vm2 in network1 and network 2 primary host.
        # 6. aquire public ip address
        # 6. Create a StaticNat Rule rule for vm1
        # 7. Use the Create LB rule for vm1 and vm2 in network1.
        # 8. LB rule creation should fail

        network_1 = self.create_Network(self.services["network_offering"])
        vm_1 = self.create_VM_in_Network(network_1)
        vm_2 = self.create_VM_in_Network(network_1)
        public_ip_1 = self.acquire_Public_IP(network_1)
        lb_rule = self.create_LB_Rule(public_ip_1, network_1, [vm_2, vm_1])
        public_ip_1.delete(self.apiclient)

        with self.assertRaises(Exception):
            lb_rules = LoadBalancerRule.list(self.apiclient,
                                        id=lb_rule.id,
                                        listall=True
                                        )
        return
