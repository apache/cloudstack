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

""" Test VPC nics after router is destroyed """

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (stopRouter,
                             startRouter,
                             destroyRouter,
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
                               get_test_template,
                               list_routers)
from marvin.lib.utils import cleanup_resources
import socket
import time
import inspect
import logging

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
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },
            "ostype": 'CentOS 5.3 (64-bit)',
            "timeout": 10,
        }


class TestVPCNics(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        # We want to fail quicker if it's failure
        socket.setdefaulttimeout(60)

        cls.testClient = super(TestVPCNics, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.template = get_test_template(
            cls.api_client,
            cls.zone.id,
            cls.hypervisor)
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"])
        cls._cleanup = [cls.service_offering]

        cls.logger = logging.getLogger('TestVPCNics')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.routers = []
        self.networks = []
        self.ips = []
        self.apiclient = self.testClient.getApiClient()
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
            domainid=self.domain.id)

        self.logger.debug("Creating a VPC offering..")
        self.vpc_off = VpcOffering.create(
            self.apiclient,
            self.services["vpc_offering"])

        self.logger.debug("Enabling the VPC offering created")
        self.vpc_off.update(self.apiclient, state='Enabled')

        self.logger.debug("Creating a VPC network in the account: %s" % self.account.name)
        self.services["vpc"]["cidr"] = '10.1.1.1/16'
        self.vpc = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid)
        
        self.cleanup = [self.vpc, self.vpc_off, self.account]
        return

    def tearDown(self):
        try:
            self.destroy_routers()
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.logger.debug("Warning: Exception during cleanup : %s" % e)
        return

    def query_routers(self):
        self.routers = list_routers(self.apiclient,
                                    account=self.account.name,
                                    domainid=self.account.domainid,
                                    )

        self.assertEqual(
            isinstance(self.routers, list), True,
            "Check for list routers response return valid data")

    def stop_router(self, router):
        self.logger.debug('Stopping router')
        cmd = stopRouter.stopRouterCmd()
        cmd.id = router.id
        self.apiclient.stopRouter(cmd)

    def destroy_routers(self):
        self.logger.debug('Destroying routers')
        for router in self.routers:
            self.stop_router(router)
            cmd = destroyRouter.destroyRouterCmd()
            cmd.id = router.id
            self.apiclient.destroyRouter(cmd)
        self.routers = []

    def create_network(self, net_offerring, gateway='10.1.1.1', vpc=None):
        try:
            self.logger.debug('Create NetworkOffering')
            net_offerring["name"] = "NET_OFF-" + str(gateway)
            nw_off = NetworkOffering.create(
                self.apiclient,
                net_offerring,
                conservemode=False)

            nw_off.update(self.apiclient, state='Enabled')
            self.logger.debug('Created and Enabled NetworkOffering')

            self.services["network"]["name"] = "NETWORK-" + str(gateway)
            self.logger.debug('Adding Network=%s' % self.services["network"])
            obj_network = Network.create(
                self.apiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=nw_off.id,
                zoneid=self.zone.id,
                gateway=gateway,
                vpcid=vpc.id if vpc else self.vpc.id
            )


            self.logger.debug("Created network with ID: %s" % obj_network.id)
        except Exception as e:
            self.fail('Unable to create a Network with offering=%s because of %s ' % (net_offerring, e))
        o = networkO(obj_network)
        
        vm1 = self.deployvm_in_network(obj_network)

        self.cleanup.insert(1, obj_network)
        self.cleanup.insert(2, nw_off)

        o.add_vm(vm1)
        return o

    def deployvm_in_network(self, network):
        try:
            self.logger.debug('Creating VM in network=%s' % network.name)
            vm = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                networkids=[str(network.id)]
            )

            self.logger.debug('Created VM=%s in network=%s' % (vm.id, network.name))
            self.cleanup.insert(0, vm)
            return vm
        except:
            self.fail('Unable to create VM in a Network=%s' % network.name)

    def acquire_publicip(self, network):
        self.logger.debug("Associating public IP for network: %s" % network.name)
        public_ip = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network.id,
            vpcid=self.vpc.id
        )
        self.logger.debug("Associated %s with network %s" % (
            public_ip.ipaddress.ipaddress,
            network.id
        ))
        return public_ip

    def create_natrule(self, vm, public_ip, network, services=None):
        self.logger.debug("Creating NAT rule in network for vm with public IP")
        if not services:
            services = self.services["natrule"]
        nat_rule = NATRule.create(
            self.apiclient,
            vm,
            services,
            ipaddressid=public_ip.ipaddress.id,
            openfirewall=False,
            networkid=network.id,
            vpcid=self.vpc.id)

        self.logger.debug("Adding NetworkACL rules to make NAT rule accessible")
        nwacl_nat = NetworkACL.create(
            self.apiclient,
            networkid=network.id,
            services=services,
            traffictype='Ingress'
        )
        self.logger.debug('nwacl_nat=%s' % nwacl_nat.__dict__)
        return nat_rule

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_01_VPC_nics_after_destroy(self):
        """ Create a VPC with two networks with one VM in each network and test nics after destroy"""
        self.logger.debug("Starting test_01_VPC_nics_after_destroy")
        self.query_routers()

        net1 = self.create_network(self.services["network_offering"], "10.1.1.1")
        net2 = self.create_network(self.services["network_offering_no_lb"], "10.1.2.1")

        self.networks.append(net1)
        self.networks.append(net2)

        self.add_nat_rules()
        self.check_ssh_into_vm()

        self.destroy_routers()
        time.sleep(30)

        net1.add_vm(self.deployvm_in_network(net1.get_net()))
        self.query_routers()
        
        self.add_nat_rules()
        self.check_ssh_into_vm()

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_02_VPC_default_routes(self):
        """ Create a VPC with two networks with one VM in each network and test default routes"""
        self.logger.debug("Starting test_02_VPC_default_routes")
        self.query_routers()

        net1 = self.create_network(self.services["network_offering"], "10.1.1.1")
        net2 = self.create_network(self.services["network_offering_no_lb"], "10.1.2.1")

        self.networks.append(net1)
        self.networks.append(net2)

        self.add_nat_rules()
        self.do_default_routes_test()

    def delete_nat_rules(self):
        for o in self.networks:
            for vm in o.get_vms():
                if vm.get_nat() is not None:
                    vm.get_nat().delete(self.apiclient)
                    vm.set_nat(None)

    def add_nat_rules(self):
        for o in self.networks:
            for vm in o.get_vms():
                if vm.get_ip() is None:
                    vm.set_ip(self.acquire_publicip(o.get_net()))
                if vm.get_nat() is None:
                    vm.set_nat(self.create_natrule(vm.get_vm(), vm.get_ip(), o.get_net()))
                    time.sleep(5)

    def check_ssh_into_vm(self):
        for o in self.networks:
            for vm in o.get_vms():
                try:
                    virtual_machine = vm.get_vm()
                    virtual_machine.ssh_client = None

                    public_ip = vm.get_ip()

                    self.logger.debug("Checking if we can SSH into VM=%s on public_ip=%s" %
                               (virtual_machine.name, public_ip.ipaddress.ipaddress))

                    virtual_machine.get_ssh_client(ipaddress=public_ip.ipaddress.ipaddress)
                    self.logger.debug("SSH into VM=%s on public_ip=%s is successful" %
                               (virtual_machine.name, public_ip.ipaddress.ipaddress))
                except:
                    self.fail("Failed to SSH into VM - %s" % (public_ip.ipaddress.ipaddress))

    def do_default_routes_test(self):
        for o in self.networks:
            for vmObj in o.get_vms():
                ssh_command = "ping -c 10 8.8.8.8"

                # Should be able to SSH VM
                packet_loss = 100
                try:
                    vm = vmObj.get_vm()
                    public_ip = vmObj.get_ip()
                    self.logger.debug("SSH into VM: %s" % public_ip.ipaddress.ipaddress)
                    
                    ssh = vm.get_ssh_client(ipaddress=public_ip.ipaddress.ipaddress)
        
                    self.logger.debug("Ping to google.com from VM")
                    result = ssh.execute(ssh_command)

                    for line in result:
                        if "packet loss" in line:
                            packet_loss = int(line.split("% packet loss")[0].split(" ")[-1])
                            break

                    self.logger.debug("SSH result: %s; packet loss is ==> %s" % (result, packet_loss))
                except Exception as e:
                    self.fail("SSH Access failed for %s: %s" % \
                              (vmObj.get_ip(), e)
                              )

                # Most pings should be successful
                self.assertTrue(packet_loss < 50,
                                 "Ping to outside world from VM should be successful")


class networkO(object):
    def __init__(self, net):
        self.network = net
        self.vms = []

    def get_net(self):
        return self.network

    def add_vm(self, vm):
        self.vms.append(vmsO(vm))

    def get_vms(self):
        return self.vms


class vmsO(object):
    def __init__(self, vm):
        self.vm = vm
        self.ip = None
        self.nat = None

    def get_vm(self):
        return self.vm

    def get_ip(self):
        return self.ip

    def get_nat(self):
        return self.nat

    def set_ip(self, ip):
        self.ip = ip

    def set_nat(self, nat):
        self.nat = nat
