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
                                         LoadBalancerRule)
from marvin.lib.common import (get_domain,
                                           get_zone,
                                           get_template,
                                           list_routers)
from marvin.lib.utils import cleanup_resources
import socket
import time
import sys


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
            "host1": None,
            "host2": None,
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
            "natrule": {
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
                # "hypervisor": 'XenServer',
                # Hypervisor type should be same as
                # hypervisor type of cluster
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },
            "ostype": 'CentOS 5.3 (64-bit)',
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
            raise Exception("Warning: Exception during cleanup : %s" % e)
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
        return

    def get_vpcrouter(self):
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


    def stop_vpcrouter(self):
        router = self.get_vpcrouter()
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

    def start_vpcrouter(self, router):
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

    def check_wget_from_vm(self, vm, public_ip, network=None, testnegative=False, isVmAccessible=True):
        import urllib.request, urllib.parse, urllib.error
        self.debug("Checking if we can wget from a VM=%s http server on public_ip=%s"  % (vm.name, public_ip.ipaddress.ipaddress))
        try:
                if not isVmAccessible:
                    self.create_natrule(vm, public_ip, network)
		self.setup_webserver(vm)

                urllib.request.urlretrieve("http://%s/test.html" % public_ip.ipaddress.ipaddress, filename="test.html")
                if not testnegative:
                    self.debug("Successesfull to wget from VM=%s http server on public_ip=%s" % (vm.name, public_ip.ipaddress.ipaddress))
                else:
                    self.fail("Successesfull to wget from VM=%s http server on public_ip=%s" % (vm.name, public_ip.ipaddress.ipaddress))
        except Exception as e:
                if not testnegative:
                    self.fail("Failed to wget from VM=%s http server on public_ip=%s: %s" % (vm.name, public_ip.ipaddress.ipaddress, e))
                else:
                    self.debug("Failed to wget from VM=%s http server on public_ip=%s: %s" % (vm.name, public_ip.ipaddress.ipaddress, e))

    def setup_webserver(self, vm):
        # Start httpd service on VM first
	sshClient = vm.get_ssh_client()
	# Test to see if we are on a tiny linux box (using busybox)
	res = str(sshClient.execute("busybox")).lower()
        if "hexdump" in res:
            self.setup_busybox(sshClient)
        else:
            self.setup_apache(sshClient)

    def setup_busybox(self, sshClient):
        """ Create a dummy test.html file and fire up the busybox web server """
	sshClient.execute('echo test > test.html')
	sshClient.execute("/usr/sbin/httpd")
	self.debug("Setup webserver using busybox")

    def setup_apache(self, sshClient):
	sshClient.execute("service httpd start")
	time.sleep(5)
	ssh_response = str(sshClient.execute("service httpd status")).lower()
	self.debug("httpd service status is: %s" % ssh_response)
	if "httpd: unrecognized service" in ssh_response or "inactive" in ssh_response:
	    ssh_res = sshClient.execute("yum install httpd -y")
	    if "Complete!" not in ssh_res:
		raise Exception("Failed to install http server")
	    sshClient.execute("service httpd start")
	    time.sleep(5)
	    ssh_response = str(sshClient.execute("service httpd status")).lower()
	if not "running" in ssh_response:
	    raise Exception("Failed to start httpd service")
	self.debug("Setup webserver using apache")

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

    def acquire_publicip(self, network):
        self.debug("Associating public IP for network: %s" % network.name)
        public_ip = PublicIPAddress.create(self.apiclient,
                                        accountid=self.account.name,
                                        zoneid=self.zone.id,
                                        domainid=self.account.domainid,
                                        networkid=network.id,
                                        vpcid=self.vpc.id
                                        )
        self.debug("Associated %s with network %s" % (public_ip.ipaddress.ipaddress,
                                                    network.id
                                                    ))
        return public_ip

    def create_vpc(self, cidr='10.1.2.1/16'):
        self.debug("Creating a VPC offering..")
        self.services["vpc_offering"]["name"] = self.services["vpc_offering"]["name"] + str(cidr)
        vpc_off = VpcOffering.create(
                                                self.apiclient,
                                                self.services["vpc_offering"]
                                                )

        self._cleanup.append(vpc_off)
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

    def create_network(self, net_offerring, gateway='10.1.1.1',vpc=None):
        try:
                self.debug('Create NetworkOffering')
                net_offerring["name"] = "NET_OFF-" + str(gateway)
                nw_off = NetworkOffering.create(self.apiclient,
                                                        net_offerring,
                                                        conservemode=False
                                                        )
                # Enable Network offering
                nw_off.update(self.apiclient, state='Enabled')
                self._cleanup.append(nw_off)
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
        except Exception as e:
                self.fail('Unable to create a Network with offering=%s because of %s ' % (net_offerring, e))

    def deployvm_in_network(self, network, host_id=None):
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

    def create_lbrule(self, public_ip, network, vmarray, services=None):
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

    def open_egress_to_world(self, network):
        self.debug("Adding Egress rules to network %s and %s to allow access to internet" % (network.name,self.services["http_rule"]))
        nwacl_internet_1 = NetworkACL.create(
                                        self.apiclient,
                                        networkid=network.id,
                                        services=self.services["http_rule"],
                                        traffictype='Ingress'
                                        )

        return nwacl_internet_1


    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_01_network_services_VPC_StopCreatePF(self):
        """ Test : Create VPC PF rules on acquired public ip when VpcVirtualRouter is stopped
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

        network_1 = self.create_network(self.services["network_offering"])
        vm_1 = self.deployvm_in_network(network_1)
        public_ip_1 = self.acquire_publicip(network_1)
        #ensure vm is accessible over public ip
        nat_rule = self.create_natrule(vm_1, public_ip_1, network_1)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        #remove the nat rule
        nat_rule.delete(self.apiclient)

        router = self.stop_vpcrouter()
        #recreate nat rule
        self.create_natrule(vm_1, public_ip_1, network_1)
        self.start_vpcrouter(router)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_02_network_services_VPC_CreatePF(self):
        """ Test Create VPC PF rules on acquired public ip when VpcVirtualRouter is Running
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 4. Deploy vm1 in network1.
        # 5. Use the Create PF rule for vm in network1.
        # 6. Successfully ssh into the Guest VM using the PF rule

        network_1 = self.create_network(self.services["network_offering"])
        vm_1 = self.deployvm_in_network(network_1)
        public_ip_1 = self.acquire_publicip(network_1)
        self.create_natrule( vm_1, public_ip_1, network_1)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_03_network_services_VPC_StopCreateMultiplePF(self):
        """ Test Create multiple VPC PF rules on acquired public ip in diff't networks when VpcVirtualRouter is stopped
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

        network_1 = self.create_network(self.services["network_offering_no_lb"])
        network_2 = self.create_network(self.services["network_offering_no_lb"], '10.1.2.1')
        vm_1 = self.deployvm_in_network(network_1)
        vm_2 = self.deployvm_in_network(network_2)

        # wait until VM is up before stop the VR
        time.sleep(120)

        public_ip_1 = self.acquire_publicip(network_1)
        public_ip_2 = self.acquire_publicip(network_2)
        router = self.stop_vpcrouter()
        self.create_natrule(vm_1, public_ip_1, network_1)
        self.create_natrule(vm_2, public_ip_2, network_2)
        self.start_vpcrouter(router)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        self.check_ssh_into_vm(vm_2, public_ip_2, testnegative=False)
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_04_network_services_VPC_CreateMultiplePF(self):
        """ Test Create multiple VPC PF rules on acquired public ip in diff't networks when VpcVirtualRouter is running
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
        # 9. Successfully ssh into the Guest VM1 and VM2 using the PF rule

        network_1 = self.create_network(self.services["network_offering"])
        network_2 = self.create_network(self.services["network_offering_no_lb"], '10.1.2.1')
        vm_1 = self.deployvm_in_network(network_1)
        vm_2 = self.deployvm_in_network(network_2)
        public_ip_1 = self.acquire_publicip(network_1)
        public_ip_2 = self.acquire_publicip(network_2)
        self.create_natrule(vm_1, public_ip_1, network_1)
        self.create_natrule(vm_2, public_ip_2, network_2)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        self.check_ssh_into_vm(vm_2, public_ip_2, testnegative=False)
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_05_network_services_VPC_StopDeletePF(self):
        """ Test delete a PF rule in VPC when VpcVirtualRouter is Stopped
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 4. Deploy vm1 in network1.
        # 5. Use the Create PF rule for vm in network1.
        # 6. Successfully ssh into the Guest VM using the PF rule.
        # 7. Successfully wget a file on http server of VM1.
        # 8. Stop the VPC Virtual Router.
        # 9. Delete internet PF rule
        # 10. Start VPC Virtual Router.
        # 11. wget a file present on http server of VM1 should fail

        network_1 = self.create_network(self.services["network_offering"])
        vm_1 = self.deployvm_in_network(network_1)
        public_ip_1 = self.acquire_publicip(network_1)
        self.create_natrule(vm_1, public_ip_1, network_1)
        http_rule = self.create_natrule(vm_1, public_ip_1, network_1, self.services["http_rule"])
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=False)
        router = self.stop_vpcrouter()
        http_rule.delete(self.apiclient)
        self.start_vpcrouter(router)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=True)
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_06_network_services_VPC_DeletePF(self):
        """ Test delete a PF rule in VPC when VpcVirtualRouter is Running
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 4. Deploy vm1 in network1.
        # 5. Use the Create PF rule for vm in network1.
        # 6. Successfully ssh into the Guest VM using the PF rule.
        # 7. Successfully wget a file on http server of VM1.
        # 9. Delete internet PF rule
        # 10. wget a file present on http server of VM1 should fail

        network_1 = self.create_network(self.services["network_offering"])
        vm_1 = self.deployvm_in_network(network_1)
        public_ip_1 = self.acquire_publicip(network_1)
        self.create_natrule(vm_1, public_ip_1, network_1)
        http_rule=self.create_natrule(vm_1, public_ip_1, network_1, self.services["http_rule"])
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=False)
        http_rule.delete(self.apiclient)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=True)
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_07_network_services_VPC_StopDeleteAllPF(self):
        """ Test delete all PF rules in VPC when VpcVirtualRouter is Stopped
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 4. Deploy vm1 in network1.
        # 5. Use the Create PF rule for vm in network1.
        # 6. Successfully ssh into the Guest VM using the PF rule.
        # 7. Successfully wget a file on http server of VM1.
        # 8. Stop the VPC Virtual Router.
        # 9. Delete all PF rule
        # 10. Start VPC Virtual Router.
        # 11. wget a file present on http server of VM1 should fail
        # 12. ssh into Guest VM using the PF rule should fail

        network_1 = self.create_network(self.services["network_offering"])
        vm_1 = self.deployvm_in_network(network_1)
        public_ip_1 = self.acquire_publicip(network_1)
        nat_rule  = self.create_natrule(vm_1, public_ip_1, network_1)
        http_rule = self.create_natrule(vm_1, public_ip_1, network_1, self.services["http_rule"])
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=False)
        router = self.stop_vpcrouter()
        http_rule.delete(self.apiclient)
        nat_rule.delete(self.apiclient)
        self.start_vpcrouter(router)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=True)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=True,
                isVmAccessible=False, network=network_1)
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_08_network_services_VPC_DeleteAllPF(self):
        """ Test delete all PF rules in VPC when VpcVirtualRouter is Running
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Create a Network offering - NO1 with all supported services
        # 3. Add network1(10.1.1.1/24) using N01 to this VPC.
        # 4. Deploy vm1 in network1.
        # 5. Use the Create PF rule for vm in network1.
        # 6. Successfully ssh into the Guest VM using the PF rule.
        # 7. Successfully wget a file on http server of VM1.
        # 8. Delete all PF rule
        # 9. wget a file present on http server of VM1 should fail
        # 10. ssh into Guest VM using the PF rule should fail

        network_1 = self.create_network(self.services["network_offering"])
        vm_1 = self.deployvm_in_network(network_1)
        public_ip_1 = self.acquire_publicip(network_1)
        nat_rule  = self.create_natrule(vm_1, public_ip_1, network_1)
        http_rule = self.create_natrule(vm_1, public_ip_1, network_1, self.services["http_rule"])
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=False)
        http_rule.delete(self.apiclient)
        nat_rule.delete(self.apiclient)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=True)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=True,
                isVmAccessible=False, network=network_1)
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_09_network_services_VPC_StopDeleteAllMultiplePF(self):
        """ Test delete all PF rules in VPC across multiple networks when VpcVirtualRouter is Stopped
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
        # 11. Stop VPC Virtual Router.
        # 12. Delete all PF rultes for vm1, vm2, vm3 and vm4.
        # 12. Start VPC Virtual Router.
        # 13. Fail to ssh and http to vm1, vm2, vm3 and vm4.

        network_1 = self.create_network(self.services["network_offering"])
        network_2 = self.create_network(self.services["network_offering_no_lb"], '10.1.2.1')
        vm_1 = self.deployvm_in_network(network_1)
        vm_2 = self.deployvm_in_network(network_1)
        vm_3 = self.deployvm_in_network(network_2)
        vm_4 = self.deployvm_in_network(network_2)
        public_ip_1 = self.acquire_publicip(network_1)
        public_ip_2 = self.acquire_publicip(network_1)
        nat_rule1  = self.create_natrule(vm_1, public_ip_1, network_1)
        nat_rule2  = self.create_natrule(vm_2, public_ip_2, network_1)
        http_rule1 = self.create_natrule(vm_1, public_ip_1, network_1, self.services["http_rule"])
        http_rule2 = self.create_natrule(vm_2, public_ip_2, network_1, self.services["http_rule"])
        public_ip_3 = self.acquire_publicip(network_2)
        public_ip_4 = self.acquire_publicip(network_2)
        nat_rule3  = self.create_natrule(vm_3, public_ip_3, network_2)
        nat_rule4  = self.create_natrule(vm_4, public_ip_4, network_2)
        http_rule3 = self.create_natrule(vm_3, public_ip_3, network_2, self.services["http_rule"])
        http_rule4 = self.create_natrule(vm_4, public_ip_4, network_2, self.services["http_rule"])
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        self.check_ssh_into_vm(vm_2, public_ip_2, testnegative=False)
        self.check_ssh_into_vm(vm_3, public_ip_3, testnegative=False)
        self.check_ssh_into_vm(vm_4, public_ip_4, testnegative=False)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=False)
        self.check_wget_from_vm(vm_2, public_ip_2, testnegative=False)
        self.check_wget_from_vm(vm_3, public_ip_3, testnegative=False)
        self.check_wget_from_vm(vm_4, public_ip_4, testnegative=False)
        router = self.stop_vpcrouter()
        nat_rule1.delete(self.apiclient)
        nat_rule2.delete(self.apiclient)
        nat_rule3.delete(self.apiclient)
        nat_rule4.delete(self.apiclient)
        http_rule1.delete(self.apiclient)
        http_rule2.delete(self.apiclient)
        http_rule3.delete(self.apiclient)
        http_rule4.delete(self.apiclient)
        self.start_vpcrouter(router)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=True)
        self.check_ssh_into_vm(vm_2, public_ip_2, testnegative=True)
        self.check_ssh_into_vm(vm_3, public_ip_3, testnegative=True)
        self.check_ssh_into_vm(vm_4, public_ip_4, testnegative=True)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=True,
                isVmAccessible=False, network=network_1)
        self.check_wget_from_vm(vm_2, public_ip_2, testnegative=True,
                isVmAccessible=False, network=network_1)
        self.check_wget_from_vm(vm_3, public_ip_3, testnegative=True,
                isVmAccessible=False, network=network_2)
        self.check_wget_from_vm(vm_4, public_ip_4, testnegative=True,
                isVmAccessible=False, network=network_2)
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_10_network_services_VPC_DeleteAllMultiplePF(self):
        """ Test delete all PF rules in VPC across multiple networks when VpcVirtualRouter is Running
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

        network_1 = self.create_network(self.services["network_offering"])
        network_2 = self.create_network(self.services["network_offering_no_lb"], '10.1.2.1')
        vm_1 = self.deployvm_in_network(network_1)
        vm_2 = self.deployvm_in_network(network_1)
        vm_3 = self.deployvm_in_network(network_2)
        vm_4 = self.deployvm_in_network(network_2)
        public_ip_1 = self.acquire_publicip(network_1)
        public_ip_2 = self.acquire_publicip(network_1)
        nat_rule1  = self.create_natrule(vm_1, public_ip_1, network_1)
        nat_rule2  = self.create_natrule(vm_2, public_ip_2, network_1)
        http_rule1 = self.create_natrule(vm_1, public_ip_1, network_1, self.services["http_rule"])
        http_rule2 = self.create_natrule(vm_2, public_ip_2, network_1, self.services["http_rule"])
        public_ip_3 = self.acquire_publicip(network_2)
        public_ip_4 = self.acquire_publicip(network_2)
        nat_rule3  = self.create_natrule(vm_3, public_ip_3, network_2)
        nat_rule4  = self.create_natrule(vm_4, public_ip_4, network_2)
        http_rule3 = self.create_natrule(vm_3, public_ip_3, network_2, self.services["http_rule"])
        http_rule4 = self.create_natrule(vm_4, public_ip_4, network_2, self.services["http_rule"])
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=False)
        self.check_ssh_into_vm(vm_2, public_ip_2, testnegative=False)
        self.check_ssh_into_vm(vm_3, public_ip_3, testnegative=False)
        self.check_ssh_into_vm(vm_4, public_ip_4, testnegative=False)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=False)
        self.check_wget_from_vm(vm_2, public_ip_2, testnegative=False)
        self.check_wget_from_vm(vm_3, public_ip_3, testnegative=False)
        self.check_wget_from_vm(vm_4, public_ip_4, testnegative=False)
        nat_rule1.delete(self.apiclient)
        nat_rule2.delete(self.apiclient)
        nat_rule3.delete(self.apiclient)
        nat_rule4.delete(self.apiclient)
        http_rule1.delete(self.apiclient)
        http_rule2.delete(self.apiclient)
        http_rule3.delete(self.apiclient)
        http_rule4.delete(self.apiclient)
        self.check_ssh_into_vm(vm_1, public_ip_1, testnegative=True)
        self.check_ssh_into_vm(vm_2, public_ip_2, testnegative=True)
        self.check_ssh_into_vm(vm_3, public_ip_3, testnegative=True)
        self.check_ssh_into_vm(vm_4, public_ip_4, testnegative=True)
        self.check_wget_from_vm(vm_1, public_ip_1, testnegative=True,
                isVmAccessible=False, network=network_1)
        self.check_wget_from_vm(vm_2, public_ip_2, testnegative=True,
                isVmAccessible=False, network=network_1)
        self.check_wget_from_vm(vm_3, public_ip_3, testnegative=True,
                isVmAccessible=False, network=network_2)
        self.check_wget_from_vm(vm_4, public_ip_4, testnegative=True,
                isVmAccessible=False, network=network_2)
        return
