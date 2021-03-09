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

""" Component tests for VPC network functionality - Port Forwarding Rules.
"""
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.base import (Account,
                                         VpcOffering,
                                         VPC,
                                         ServiceOffering,
                                         NetworkACL,
                                         PublicIPAddress,
                                         NetworkOffering,
                                         Network,
                                         VirtualMachine,
                                         LoadBalancerRule,
                                         StaticNATRule)
from marvin.cloudstackAPI import (stopRouter,
                                  startRouter)
from marvin.lib.common import (get_domain,
                                           get_zone,
                                           get_template,
                                           list_routers)
from marvin.lib.utils import cleanup_resources
import socket
import time


class Services:
    """Test VPC network services - Port Forwarding Rules Test Data Class.
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
                                                "publicport": 2222,
                                                "openfirewall": False,
                                                "startport": 22,
                                                "endport": 2222,
                                                "protocol": "TCP",
                                                "cidrlist": '0.0.0.0/0',
                                        },
                                "lbrule_http": {
                                                "name": "HTTP",
                                                "alg": "leastconn",
                                                # Algorithm used for load balancing
                                                "privateport": 80,
                                                "publicport": 8888,
                                                "openfirewall": False,
                                                "startport": 80,
                                                "endport": 8888,
                                                "protocol": "TCP",
                                                "cidrlist": '0.0.0.0/0',
                                        },
                                "ssh_rule": {
                                                "privateport": 22,
                                                "publicport": 22,
                                                "startport": 22,
                                                "endport": 22,
                                                "protocol": "TCP",
                                                "cidrlist": '0.0.0.0/0',
                                        },
                                "http_rule": {
                                                "privateport": 80,
                                                "publicport": 80,
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


class TestVPCNetworkPFRules(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        # We want to fail quicker if it's failure
        socket.setdefaulttimeout(60)

        cls.testClient = super(TestVPCNetworkPFRules, cls).getClsTestClient()
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
        except:
                if not testnegative:
                    self.fail("Failed to wget from VM=%s http server on public_ip=%s" % (vm.name, public_ip.ipaddress.ipaddress))
                else:
                    self.debug("Failed to wget from VM=%s http server on public_ip=%s" % (vm.name, public_ip.ipaddress.ipaddress))

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

    def delete_StaticNatRule_For_VM(self, vm, public_ip):
        self.debug("Disabling static NAT for IP: %s" %
                                                        public_ip.ipaddress.ipaddress)
        try:
                StaticNATRule.disable(
                                        self.apiclient,
                                        ipaddressid=public_ip.ipaddress.id,
                                        virtualmachineid=vm.id,
                                        )
                self.debug("Static NAT disabled for IP: %s" %
                                                        public_ip.ipaddress.ipaddress)
        except Exception as e:
                self.fail("Failed to disabled static NAT on IP: %s - %s" % (
                                                    public_ip.ipaddress.ipaddress, e))

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

        self.cleanup.append(self.vpc_off)
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
        self.debug("Adding virtual machines %s and %s to LB rule" % (vmarray))
        lb_rule.assign(self.apiclient, vmarray)
        return lb_rule

    def create_ingress_rule(self, network, services=None):
        if not services:
            services = self.services["ssh_rule"]
        self.debug("Adding NetworkACL rules to make NAT rule accessible")
        nwacl_nat = NetworkACL.create(self.apiclient,
                                        services,
                                        networkid=network.id,
                                        traffictype='Ingress'
                                        )
        return nwacl_nat


    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_01_VPC_StaticNatRuleCreateStoppedState(self):
        """ Test case no extra : 
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 4. Deploy vm1 in network1.
        # 5. Stop the VPC Virtual Router.
        # 6. Use the Create PF rule for vm in network1.
        # 7. Start VPC Virtual Router.
        # 8. Successfully ssh into the Guest VM using the PF rule

        network_1 = self.create_Network(self.services["network_offering"])
        self.create_ingress_rule(network_1)

        vm_1 = self.create_VM_in_Network(network_1)
        public_ip_1 = self.acquire_Public_IP(network_1)

        router = self.stop_VPC_VRouter()
        self.create_StaticNatRule_For_VM( vm_1, public_ip_1, network_1)
        self.start_VPC_VRouter(router)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)

        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_02_VPC_CreateStaticNatRule(self):
        """ Test case no 229 : Create Static NAT Rule for a single virtual network of 
            a VPC using a new Public IP Address available with the VPC when the Virtual Router is in Running State
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 4. Deploy vm1 in network1.
        # 5. Use the Create Static Nat rule for vm in network1.
        # 6. Successfully ssh into the Guest VM using the PF rule

        network_1 = self.create_Network(self.services["network_offering"])
        self.create_ingress_rule(network_1)

        vm_1 = self.create_VM_in_Network(network_1)
        public_ip_1 = self.acquire_Public_IP(network_1)
        self.create_StaticNatRule_For_VM( vm_1, public_ip_1, network_1)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_03_VPC_StopCreateMultipleStaticNatRuleStopppedState(self):
        """ Test case no extra : Create Static Nat Rule rules for a two/multiple virtual networks of a VPC using
                a new Public IP Address available with the VPC when Virtual Router is in Stopped State
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 4. Add network2(10.1.2.1/24) using N01 to this VPC.
        # 5. Deploy vm1 in network1.
        # 6. Deploy vm2 in network2.
        # 7. Stop the VPC Virtual Router.
        # 8. Use the Create PF rule for vm1 in network1.
        # 9. Use the Create PF rule for vm2 in network2.
        # 10. Start VPC Virtual Router.
        # 11. Successfully ssh into the Guest VM1 and VM2 using the PF rule

        network_1 = self.create_Network(self.services["network_offering_no_lb"])
        network_2 = self.create_Network(self.services["network_offering_no_lb"], '10.1.2.1')
        self.create_ingress_rule(network_1)
        self.create_ingress_rule(network_2)

        vm_1 = self.create_VM_in_Network(network_1)
        vm_2 = self.create_VM_in_Network(network_2)
        public_ip_1 = self.acquire_Public_IP(network_1)
        public_ip_2 = self.acquire_Public_IP(network_2)

        # wait for VM to boot up
        time.sleep(120)

        router = self.stop_VPC_VRouter()
        self.create_StaticNatRule_For_VM(vm_1, public_ip_1, network_1)
        self.create_StaticNatRule_For_VM(vm_2, public_ip_2, network_2)
        self.start_VPC_VRouter(router)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        self.check_ssh_into_vm(vm_2, public_ip_2, testnegative=False)
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_04_VPC_CreateMultipleStaticNatRule(self):
        """ Test case no 230 : Create Static NAT Rules for a two/multiple virtual networks of 
            a VPC using a new Public IP Address available with the VPC when the Virtual Router is in Running State
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 4. Add network2(10.1.2.1/24) using N01 to this VPC.
        # 5. Deploy vm1 in network1.
        # 6. Deploy vm2 in network2.
        # 7. Use the Create PF rule for vm1 in network1.
        # 8. Use the Create PF rule for vm2 in network2.
        # 9. Start VPC Virtual Router.
        # 10. Successfully ssh into the Guest VM1 and VM2 using the PF rule

        network_1 = self.create_Network(self.services["network_offering"])
        network_2 = self.create_Network(self.services["network_offering_no_lb"], '10.1.2.1')
        self.create_ingress_rule(network_1)
        self.create_ingress_rule(network_2)

        vm_1 = self.create_VM_in_Network(network_1)
        vm_2 = self.create_VM_in_Network(network_2)
        public_ip_1 = self.acquire_Public_IP(network_1)
        public_ip_2 = self.acquire_Public_IP(network_2)
        self.create_StaticNatRule_For_VM(vm_1, public_ip_1, network_1)
        self.create_StaticNatRule_For_VM(vm_2, public_ip_2, network_2)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        self.check_ssh_into_vm(vm_2, public_ip_2, testnegative=False)
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_05_network_services_VPC_DeleteAllPF(self):
        """ Test case no 232: Delete all Static NAT Rules for a single virtual network of 
            a VPC belonging to a single Public IP Address when the Virtual Router is in Running State
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 4. Deploy vm1 in network1.
        # 5. Use the Create static nat rule for vm in network1.
        # 6. Successfully ssh into the Guest VM using the PF rule.
        # 7. Successfully wget a file on http server of VM1.
        # 8. Delete all PF rule
        # 9. wget a file present on http server of VM1 should fail
        # 10. ssh into Guest VM using the PF rule should fail

        network_1 = self.create_Network(self.services["network_offering"])
        self.create_ingress_rule(network_1)
        self.create_ingress_rule(network_1, self.services["http_rule"])

        vm_1 = self.create_VM_in_Network(network_1)
        public_ip_1 = self.acquire_Public_IP(network_1)
        self.create_StaticNatRule_For_VM(vm_1, public_ip_1, network_1)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=False)
        self.delete_StaticNatRule_For_VM(vm_1, public_ip_1)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=True)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=True)
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_06_network_services_VPC_DeleteAllMultiplePF(self):
        """ Test case no 233: Delete all Static NAT rules for two/multiple virtual networks of a VPC. 
            Observe the status of the Public IP Addresses of the rules when the Virtual Router is in Running State.
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16.
        # 2. Create a Network offering - NO1 with all supported services.
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 4. Add network2(10.1.2.1/24) using N01 to this VPC.
        # 5. Deploy vm1 and vm2 in network1.
        # 6. Deploy vm3 and vm4 in network2.
        # 7. Use the Create PF rule ssh and http for vm1 and vm2 in network1.
        # 8. Use the Create PF rule ssh and http for vm3 and vm4  in network2.
        # 9. Successfully ssh into the Guest vm1, vm2, vm3 and vm4 using the PF rule.
        # 10. Succesfully wget a file from http server present on vm1, vm2, vm3 and vm4.
        # 12. Delete all PF rultes for vm1, vm2, vm3 and vm4.
        # 13. Fail to ssh and http to vm1, vm2, vm3 and vm4.

        network_1 = self.create_Network(self.services["network_offering"])
        network_2 = self.create_Network(self.services["network_offering_no_lb"], '10.1.2.1')
        self.create_ingress_rule(network_1)
        self.create_ingress_rule(network_2)
        self.create_ingress_rule(network_1, self.services["http_rule"])
        self.create_ingress_rule(network_2, self.services["http_rule"])

        vm_1 = self.create_VM_in_Network(network_1)
        vm_2 = self.create_VM_in_Network(network_1)
        vm_3 = self.create_VM_in_Network(network_2)
        vm_4 = self.create_VM_in_Network(network_2)
        public_ip_1 = self.acquire_Public_IP(network_1)
        public_ip_2 = self.acquire_Public_IP(network_1)
        public_ip_3 = self.acquire_Public_IP(network_2)
        public_ip_4 = self.acquire_Public_IP(network_2)
        self.create_StaticNatRule_For_VM(vm_1, public_ip_1, network_1)
        self.create_StaticNatRule_For_VM(vm_2, public_ip_2, network_1)
        self.create_StaticNatRule_For_VM(vm_3, public_ip_3, network_2)
        self.create_StaticNatRule_For_VM(vm_4, public_ip_4, network_2)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        self.check_ssh_into_vm(vm_2, public_ip_2, testnegative=False)
        self.check_ssh_into_vm(vm_3, public_ip_1, testnegative=False)
        self.check_ssh_into_vm(vm_4, public_ip_2, testnegative=False)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=False)
        self.check_wget_from_vm(vm_2, public_ip_2, testnegative=False)
        self.check_wget_from_vm(vm_3, public_ip_1, testnegative=False)
        self.check_wget_from_vm(vm_4, public_ip_2, testnegative=False)
        self.delete_StaticNatRule_For_VM(vm_1, public_ip_1)
        self.delete_StaticNatRule_For_VM(vm_2, public_ip_2)
        self.delete_StaticNatRule_For_VM(vm_3, public_ip_3)
        self.delete_StaticNatRule_For_VM(vm_4, public_ip_4)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=True)
        self.check_ssh_into_vm(vm_2, public_ip_2, testnegative=True)
        self.check_ssh_into_vm(vm_3, public_ip_1, testnegative=True)
        self.check_ssh_into_vm(vm_4, public_ip_2, testnegative=True)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=True)
        self.check_wget_from_vm(vm_2, public_ip_2, testnegative=True)
        self.check_wget_from_vm(vm_3, public_ip_1, testnegative=True)
        self.check_wget_from_vm(vm_4, public_ip_2, testnegative=True)
        return
