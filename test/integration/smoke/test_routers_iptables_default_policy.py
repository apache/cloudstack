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
                               list_routers,
                               list_hosts)
from marvin.lib.utils import (cleanup_resources,
                              get_process_status)
import socket
import time
import inspect
import logging


class Services:
    """Test VPC network services - Port Forwarding Rules Test Data Class.
    """

    def __init__(self):
        self.services = {
            "configurableData": {
                "host": {
                    "port": 22
                },
                "input": "INPUT",
                "forward": "FORWARD"
            },
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
            "shared_network_offering_sg": {
                "name": "MySharedOffering-sg",
                "displaytext": "MySharedOffering-sg",
                "guestiptype": "Shared",
                "supportedservices": "Dhcp,Dns,UserData,SecurityGroup",
                "specifyVlan": "False",
                "specifyIpRanges": "False",
                "traffictype": "GUEST",
                "serviceProviderList": {
                    "Dhcp": "VirtualRouter",
                    "Dns": "VirtualRouter",
                    "UserData": "VirtualRouter",
                    "SecurityGroup": "SecurityGroupProvider"
                }
            },
            "network_offering": {
                "name": 'Test Network offering',
                "displaytext": 'Test Network offering',
                "guestiptype": 'Isolated',
                "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding',
                "traffictype": 'GUEST',
                "availability": 'Optional',
                "serviceProviderList": {
                    "Dhcp": 'VirtualRouter',
                    "Dns": 'VirtualRouter',
                    "SourceNat": 'VirtualRouter',
                    "PortForwarding": 'VirtualRouter',
                },
            },
            "vpc_network_offering": {
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
            "vpc_network_offering_no_lb": {
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
            "redundant_vpc_offering": {
                "name": 'Redundant VPC off',
                "displaytext": 'Redundant VPC off',
                "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Lb,UserData,StaticNat',
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
                "serviceCapabilityList": {
                    "SourceNat": {
                        "RedundantRouter": 'true'
                    }
                },
            },
            "vpc": {
                "name": "TestVPC",
                "displaytext": "TestVPC",
                "cidr": '10.1.1.1/16'
            },
            "network": {
                "name": "Test Network",
                "displaytext": "Test Network",
                "netmask": '255.255.255.0'
            },
            "natrule": {
                "privateport": 22,
                "publicport": 22,
                "startport": 22,
                "endport": 22,
                "protocol": "TCP",
                "cidrlist": '0.0.0.0/0',
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


class TestVPCIpTablesPolicies(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        # We want to fail quicker if it's failure
        socket.setdefaulttimeout(60)

        cls.testClient = super(TestVPCIpTablesPolicies, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.template = get_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor)

        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id)
        
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offering"])
        

        cls.logger = logging.getLogger('TestVPCIpTablesPolicies')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        cls.entity_manager = EntityManager(cls.apiclient, cls.services, cls.service_offering, cls.account, cls.zone, cls.logger)

        cls._cleanup = [cls.service_offering, cls.account]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.logger.debug("Creating a VPC offering.")
        self.vpc_off = VpcOffering.create(
            self.apiclient,
            self.services["vpc_offering"])

        self.logger.debug("Enabling the VPC offering created")
        self.vpc_off.update(self.apiclient, state='Enabled')

        self.logger.debug("Creating a VPC network in the account: %s" % self.account.name)

        self.vpc = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid)

        self.cleanup = [self.vpc, self.vpc_off]
        self.entity_manager.set_cleanup(self.cleanup)
        return

    def tearDown(self):
        try:
            self.entity_manager.destroy_routers()
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_01_single_VPC_iptables_policies(self):
        """ Test iptables default INPUT/FORWARD policies on VPC router """
        self.logger.debug("Starting test_01_single_VPC_iptables_policies")
        
        routers = self.entity_manager.query_routers()

        self.assertEqual(
            isinstance(routers, list), True,
            "Check for list routers response return valid data")

        self.entity_manager.create_network(self.services["vpc_network_offering"], self.vpc.id, "10.1.1.1")
        self.entity_manager.create_network(self.services["vpc_network_offering_no_lb"], self.vpc.id, "10.1.2.1")

        self.entity_manager.add_nat_rules(self.vpc.id)
        self.entity_manager.do_vpc_test()

        for router in routers:
            if not router.isredundantrouter and router.vpcid:
                hosts = list_hosts(
                    self.apiclient,
                    id=router.hostid)
                self.assertEqual(
                    isinstance(hosts, list),
                    True,
                    "Check for list hosts response return valid data")
    
                host = hosts[0]
                host.user = self.hostConfig['username']
                host.passwd = self.hostConfig['password']
                host.port = self.services["configurableData"]["host"]["port"]
                tables = [self.services["configurableData"]["input"], self.services["configurableData"]["forward"]]
                
                for table in tables:
                    result = None
                    if self.hypervisor.lower() in ('vmware', 'hyperv'):
                        result = get_process_status(
                            self.apiclient.connection.mgtSvr,
                            22,
                            self.apiclient.connection.user,
                            self.apiclient.connection.passwd,
                            router.linklocalip,
                            'iptables -L %s' % table,
                            hypervisor=self.hypervisor)
                    else:
                        try:
                            result = get_process_status(
                                host.ipaddress,
                                host.port,
                                host.user,
                                host.passwd,
                                router.linklocalip,
                                'iptables -L %s' % table)
                        except KeyError:
                            self.skipTest(
                                "Provide a marvin config file with host\
                                        credentials to run %s" %
                                self._testMethodName)
        
                    self.logger.debug("iptables -L %s: %s" % (table, result))
                    res = str(result)
                    
                    self.assertEqual(
                        res.count("DROP"),
                        1,
                        "%s Default Policy should be DROP" % table)


class TestRouterIpTablesPolicies(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        # We want to fail quicker if it's failure
        socket.setdefaulttimeout(60)

        cls.testClient = super(TestRouterIpTablesPolicies, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.template = get_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor)

        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id)
        
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offering"])
        
        cls.logger = logging.getLogger('TestRouterIpTablesPolicies')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        cls.entity_manager = EntityManager(cls.apiclient, cls.services, cls.service_offering, cls.account, cls.zone, cls.logger)

        cls._cleanup = [cls.service_offering, cls.account]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.cleanup = []
        self.entity_manager.set_cleanup(self.cleanup)
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_02_routervm_iptables_policies(self):
        """ Test iptables default INPUT/FORWARD policy on RouterVM """

        self.logger.debug("Starting test_02_routervm_iptables_policies")

        vm1 = self.entity_manager.deployvm()

        routers = self.entity_manager.query_routers()

        self.assertEqual(
            isinstance(routers, list), True,
            "Check for list routers response return valid data")

        for router in routers:
            if not router.isredundantrouter and not router.vpcid:
                hosts = list_hosts(
                    self.apiclient,
                    id=router.hostid)
                self.assertEqual(
                    isinstance(hosts, list),
                    True,
                    "Check for list hosts response return valid data")

                host = hosts[0]
                host.user = self.hostConfig['username']
                host.passwd = self.hostConfig['password']
                host.port = self.services["configurableData"]["host"]["port"]
                tables = [self.services["configurableData"]["input"], self.services["configurableData"]["forward"]]

                for table in tables:
                    result = None
                    if self.hypervisor.lower() in ('vmware', 'hyperv'):
                        result = get_process_status(
                            self.apiclient.connection.mgtSvr,
                            22,
                            self.apiclient.connection.user,
                            self.apiclient.connection.passwd,
                            router.linklocalip,
                            'iptables -L %s' % table,
                            hypervisor=self.hypervisor)
                    else:
                        try:
                            result = get_process_status(
                                host.ipaddress,
                                host.port,
                                host.user,
                                host.passwd,
                                router.linklocalip,
                                'iptables -L %s' % table)
                        except KeyError:
                            self.skipTest(
                                "Provide a marvin config file with host\
                                        credentials to run %s" %
                                self._testMethodName)

                    self.logger.debug("iptables -L %s: %s" % (table, result))
                    res = str(result)

                    self.assertEqual(
                        res.count("DROP"),
                        1,
                        "%s Default Policy should be DROP" % table)


class EntityManager(object):

    def __init__(self, apiclient, services, service_offering, account, zone, logger):
        self.apiclient = apiclient
        self.services = services
        self.service_offering = service_offering
        self.account = account
        self.zone = zone
        self.logger = logger

        self.cleanup = []
        self.networks = []
        self.routers = []
        self.ips = []
    
    def set_cleanup(self, cleanup):
        self.cleanup = cleanup

    def add_nat_rules(self, vpc_id):
        for o in self.networks:
            for vm in o.get_vms():
                if vm.get_ip() is None:
                    vm.set_ip(self.acquire_publicip(o.get_net(), vpc_id))
                if vm.get_nat() is None:
                    vm.set_nat(self.create_natrule(vm.get_vm(), vm.get_ip(), o.get_net(), vpc_id))
                    time.sleep(5)

    def do_vpc_test(self):
        for o in self.networks:
            for vm in o.get_vms():
                self.check_ssh_into_vm(vm.get_vm(), vm.get_ip())

    def create_natrule(self, vm, public_ip, network, vpc_id):
        self.logger.debug("Creating NAT rule in network for vm with public IP")

        nat_rule_services = self.services["natrule"]

        nat_rule = NATRule.create(
            self.apiclient,
            vm,
            nat_rule_services,
            ipaddressid=public_ip.ipaddress.id,
            openfirewall=False,
            networkid=network.id,
            vpcid=vpc_id)

        self.logger.debug("Adding NetworkACL rules to make NAT rule accessible")
        nwacl_nat = NetworkACL.create(
            self.apiclient,
            networkid=network.id,
            services=nat_rule_services,
            traffictype='Ingress'
        )
        self.logger.debug('nwacl_nat=%s' % nwacl_nat.__dict__)
        return nat_rule

    def check_ssh_into_vm(self, vm, public_ip):
        self.logger.debug("Checking if we can SSH into VM=%s on public_ip=%s" % 
            (vm.name, public_ip.ipaddress.ipaddress))
        vm.ssh_client = None
        try:
            vm.get_ssh_client(ipaddress=public_ip.ipaddress.ipaddress)
            self.logger.debug("SSH into VM=%s on public_ip=%s is successful" %
                       (vm.name, public_ip.ipaddress.ipaddress))
        except:
            raise Exception("Failed to SSH into VM - %s" % (public_ip.ipaddress.ipaddress))

    def create_network(self, net_offerring, vpc_id, gateway='10.1.1.1'):
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
            self.logger.debug('Adding Network=%s to VPC ID %s' % (self.services["network"], vpc_id))
            obj_network = Network.create(
                self.apiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=nw_off.id,
                zoneid=self.zone.id,
                gateway=gateway,
                vpcid=vpc_id)

            self.logger.debug("Created network with ID: %s" % obj_network.id)
        except Exception as e:
            raise Exception('Unable to create a Network with offering=%s because of %s ' % (net_offerring, e))

        o = networkO(obj_network)

        vm1 = self.deployvm_in_network(obj_network)
        self.cleanup.insert(1, obj_network)
        self.cleanup.insert(2, nw_off)

        o.add_vm(vm1)
        self.networks.append(o)
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
                networkids=[str(network.id)])

            self.logger.debug('Created VM=%s in network=%s' % (vm.id, network.name))
            self.cleanup.insert(0, vm)
            return vm
        except:
            raise Exception('Unable to create VM in a Network=%s' % network.name)

    def deployvm(self):
        try:
            self.logger.debug('Creating VM')
            vm = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id)

            self.cleanup.insert(0, vm)
            self.logger.debug('Created VM=%s' % vm.id)
            return vm
        except:
            raise Exception('Unable to create VM')

    def acquire_publicip(self, network, vpc_id):
        self.logger.debug("Associating public IP for network: %s" % network.name)
        public_ip = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network.id,
            vpcid=vpc_id)
        self.logger.debug("Associated %s with network %s" % (
            public_ip.ipaddress.ipaddress,
            network.id))

        self.ips.append(public_ip)
        return public_ip

    def query_routers(self):
        self.routers = list_routers(self.apiclient,
                                    account=self.account.name,
                                    domainid=self.account.domainid)

        return self.routers

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

    def start_routers(self):
        self.logger.debug('Starting routers')
        for router in self.routers:
            cmd = startRouter.startRouterCmd()
            cmd.id = router.id
            self.apiclient.startRouter(cmd)


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
