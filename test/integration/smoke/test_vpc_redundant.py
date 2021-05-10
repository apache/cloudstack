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

""" Test redundancy features for VPC routers
"""

from marvin.codes import PASS, FAILED
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (stopRouter,
                             startRouter,
                             destroyRouter,
                             rebootRouter,
                             Account,
                             Template,
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
                             Configurations)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_test_template,
                               list_routers,
                               list_hosts)
from marvin.lib.utils import (cleanup_resources,
                              get_process_status,
                              get_host_credentials)
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
                "cidr": '10.0.0.0/16'
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
            "timeout": 10
        }


class TestVPCRedundancy(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        # We want to fail quicker if it's failure
        socket.setdefaulttimeout(60)

        cls.testClient = super(TestVPCRedundancy, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        cls.hypervisor = cls.testClient.getHypervisorInfo()

        cls.template = get_test_template(cls.api_client, cls.zone.id, cls.hypervisor)
        if cls.template == FAILED:
            assert False, "get_test_template() failed to return template"

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"])
        cls._cleanup = [cls.service_offering]

        cls.logger = logging.getLogger('TestVPCRedundancy')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        cls.advert_int = int(Configurations.list(cls.api_client, name="router.redundant.vrrp.interval")[0].value)

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.routers = []
        self.networks = []
        self.ips = []

        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()

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
            #Stop/Destroy the routers so we are able to remove the networks. Issue CLOUDSTACK-8935
            self.destroy_routers()
            cleanup_resources(self.api_client, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def query_routers(self, count=2, showall=False):
        self.routers = list_routers(self.apiclient,
                                    account=self.account.name,
                                    domainid=self.account.domainid,
                                    )
        if not showall:
            self.routers = [r for r in self.routers if r.state != "Stopped"]
        self.assertEqual(
            isinstance(self.routers, list), True,
            "Check for list routers response return valid data")

        self.assertEqual(
            len(self.routers), count,
            "Check that %s routers were indeed created" % count)

    def wait_for_vrrp(self):
        # Wait until 3*advert_int+skew time to get one of the routers as MASTER
        time.sleep(3 * self.advert_int + 5)

    def check_routers_state(self,count=2, status_to_check="MASTER", expected_count=1, showall=False):
        vals = ["MASTER", "BACKUP", "UNKNOWN", "FAULT"]
        cnts = [0, 0, 0, 0]

        self.wait_for_vrrp()

        result = "UNKNOWN"
        self.query_routers(count, showall)
        for router in self.routers:
            if router.state == "Running":
                hosts = list_hosts(
                    self.apiclient,
                    zoneid=router.zoneid,
                    type='Routing',
                    state='Up',
                    id=router.hostid
                )
                self.assertEqual(
                    isinstance(hosts, list),
                    True,
                    "Check list host returns a valid list"
                )
                host = hosts[0]

                if self.hypervisor.lower() in ('vmware', 'hyperv'):
                        result = str(get_process_status(
                            self.apiclient.connection.mgtSvr,
                            22,
                            self.apiclient.connection.user,
                            self.apiclient.connection.passwd,
                            router.linklocalip,
                            "sh /opt/cloud/bin/checkrouter.sh ",
                            hypervisor=self.hypervisor
                        ))
                else:
                    try:
                        host.user, host.passwd = get_host_credentials(
                            self.config, host.ipaddress)
                        result = str(get_process_status(
                            host.ipaddress,
                            22,
                            host.user,
                            host.passwd,
                            router.linklocalip,
                            "sh /opt/cloud/bin/checkrouter.sh "
                        ))

                    except KeyError:
                        self.skipTest(
                            "Marvin configuration has no host credentials to\
                                    check router services")
            
                if result.count(status_to_check) == 1:
                    cnts[vals.index(status_to_check)] += 1

        if cnts[vals.index(status_to_check)] != expected_count:
            self.fail("Expected '%s' routers at state '%s', but found '%s'!" % (expected_count, status_to_check, cnts[vals.index(status_to_check)]))

    def stop_router(self, router):
        self.logger.debug('Stopping router %s' % router.id)
        cmd = stopRouter.stopRouterCmd()
        cmd.id = router.id
        cmd.forced = True
        self.apiclient.stopRouter(cmd)

    def reboot_router(self, router):
        self.logger.debug('Rebooting router %s' % router.id)
        cmd = rebootRouter.rebootRouterCmd()
        cmd.id = router.id
        self.apiclient.rebootRouter(cmd)

    def stop_router_by_type(self, type):
        self.check_routers_state()
        self.logger.debug('Stopping %s router' % type)
        for router in self.routers:
            if router.redundantstate == type:
                self.stop_router(router)

    def reboot_router_by_type(self, type):
        self.check_routers_state()
        self.logger.debug('Rebooting %s router' % type)
        for router in self.routers:
            if router.redundantstate == type:
                self.reboot_router(router)

    def destroy_routers(self):
        self.logger.debug('Destroying routers')
        for router in self.routers:
            self.stop_router(router)
            cmd = destroyRouter.destroyRouterCmd()
            cmd.id = router.id
            self.apiclient.destroyRouter(cmd)
        self.routers = []

    def start_routers(self):
        self.check_routers_state(showall=True)
        self.logger.debug('Starting stopped routers')
        for router in self.routers:
            self.logger.debug('Router %s has state %s' % (router.id, router.state))
            if router.state == "Stopped":
                self.logger.debug('Starting stopped router %s' % router.id)
                cmd = startRouter.startRouterCmd()
                cmd.id = router.id
                self.apiclient.startRouter(cmd)

    def create_network(self, net_offerring, gateway='10.1.1.1', vpc=None, nr_vms=2, mark_net_cleanup=True):
        if not nr_vms or nr_vms <= 0:
            self.fail("At least 1 VM has to be created. You informed nr_vms < 1")
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

        self.cleanup.insert(0, nw_off)
        if mark_net_cleanup:
            self.cleanup.insert(0, obj_network)

        for i in range(0, nr_vms):
            vm1 = self.deployvm_in_network(obj_network, mark_vm_cleanup=mark_net_cleanup)
            o.add_vm(vm1)

        return o

    def deployvm_in_network(self, network, host_id=None, mark_vm_cleanup=True):
        try:
            self.logger.debug('Creating VM in network=%s' % network.name)
            vm = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                networkids=[str(network.id)],
                hostid=host_id
            )

            self.logger.debug('Created VM=%s in network=%s' % (vm.id, network.name))
            if mark_vm_cleanup:
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

    def check_ssh_into_vm(self, vm, public_ip, expectFail=False, retries=5):
        self.logger.debug("Checking if we can SSH into VM=%s on public_ip=%s (%r)" %
                   (vm.name, public_ip.ipaddress.ipaddress, expectFail))
        vm.ssh_client = None
        try:
            if 'retries' in inspect.getargspec(vm.get_ssh_client).args:
                vm.get_ssh_client(ipaddress=public_ip.ipaddress.ipaddress, retries=retries)
            else:
                vm.get_ssh_client(ipaddress=public_ip.ipaddress.ipaddress)
            if expectFail:
                self.fail("SSH into VM=%s on public_ip=%s is successful (Not Expected)" %
                          (vm.name, public_ip.ipaddress.ipaddress))
            else:
                self.logger.debug("SSH into VM=%s on public_ip=%s is successful" %
                           (vm.name, public_ip.ipaddress.ipaddress))
        except:
            if expectFail:
                self.logger.debug("Failed to SSH into VM - %s (Expected)" % (public_ip.ipaddress.ipaddress))
            else:
                self.fail("Failed to SSH into VM - %s" % (public_ip.ipaddress.ipaddress))

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_01_create_redundant_VPC_2tiers_4VMs_4IPs_4PF_ACL(self):
        """ Create a redundant VPC with two networks with two VMs in each network """
        self.logger.debug("Starting test_01_create_redundant_VPC_2tiers_4VMs_4IPs_4PF_ACL")
        self.query_routers()
        self.networks.append(self.create_network(self.services["network_offering"], "10.1.1.1"))
        self.networks.append(self.create_network(self.services["network_offering_no_lb"], "10.1.2.1"))
        self.check_routers_state()
        self.add_nat_rules()
        self.do_vpc_test(False)
        
        self.stop_router_by_type("MASTER")
        self.check_routers_state(1)
        self.do_vpc_test(False)

        self.delete_nat_rules()
        self.check_routers_state(count=1)
        self.do_vpc_test(True)
        self.delete_public_ip()

        self.start_routers()
        self.add_nat_rules()
        self.check_routers_state()
        self.do_vpc_test(False)

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_02_redundant_VPC_default_routes(self):
        """ Create a redundant VPC with two networks with two VMs in each network and check default routes"""
        self.logger.debug("Starting test_02_redundant_VPC_default_routes")
        self.query_routers()
        self.networks.append(self.create_network(self.services["network_offering"], "10.1.1.1"))
        self.networks.append(self.create_network(self.services["network_offering_no_lb"], "10.1.2.1"))
        self.check_routers_state()
        self.add_nat_rules()
        self.do_default_routes_test()
    
    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_03_create_redundant_VPC_1tier_2VMs_2IPs_2PF_ACL_reboot_routers(self):
        """ Create a redundant VPC with two networks with two VMs in each network """
        self.logger.debug("Starting test_01_create_redundant_VPC_2tiers_4VMs_4IPs_4PF_ACL")
        self.query_routers()
        self.networks.append(self.create_network(self.services["network_offering"], "10.1.1.1"))
        self.check_routers_state()
        self.add_nat_rules()
        self.do_vpc_test(False)
        
        self.reboot_router_by_type("MASTER")
        self.check_routers_state()
        self.do_vpc_test(False)

        self.reboot_router_by_type("MASTER")
        self.check_routers_state()
        self.do_vpc_test(False)

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_04_rvpc_network_garbage_collector_nics(self):
        """ Create a redundant VPC with 1 Tier, 1 VM, 1 ACL, 1 PF and test Network GC Nics"""
        self.logger.debug("Starting test_04_rvpc_network_garbage_collector_nics")
        self.query_routers()
        self.networks.append(self.create_network(self.services["network_offering"], "10.1.1.1", nr_vms=1))
        self.check_routers_state()
        self.add_nat_rules()
        self.do_vpc_test(False)

        self.stop_vm()

        gc_wait = Configurations.list(self.apiclient, name="network.gc.wait")
        gc_interval = Configurations.list(self.apiclient, name="network.gc.interval")

        self.logger.debug("network.gc.wait is ==> %s" % gc_wait)
        self.logger.debug("network.gc.interval is ==> %s" % gc_interval)

        total_sleep = 120
        if gc_wait and gc_interval:
            total_sleep = int(gc_wait[0].value) + int(gc_interval[0].value)
        else:
            self.logger.debug("Could not retrieve the keys 'network.gc.interval' and 'network.gc.wait'. Sleeping for 2 minutes.")

        time.sleep(total_sleep * 3)

        # Router will be in FAULT state, i.e. keepalived is stopped
        self.check_routers_state(status_to_check="FAULT", expected_count=2)
        self.start_vm()
        self.check_routers_state(status_to_check="MASTER")

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_05_rvpc_multi_tiers(self):
        """ Create a redundant VPC with multiple tiers"""
        self.logger.debug("Starting test_05_rvpc_multi_tiers")
        self.query_routers()

        network = self.create_network(self.services["network_offering"], "10.1.1.1", nr_vms=1, mark_net_cleanup=False)
        self.networks.append(network)
        self.networks.append(self.create_network(self.services["network_offering_no_lb"], "10.1.2.1", nr_vms=1))
        self.networks.append(self.create_network(self.services["network_offering_no_lb"], "10.1.3.1", nr_vms=1))
        
        self.check_routers_state()
        self.add_nat_rules()
        self.do_vpc_test(False)

        self.destroy_vm(network)
        network.get_net().delete(self.apiclient)
        self.networks.remove(network)
        
        self.check_routers_state(status_to_check="MASTER")
        self.do_vpc_test(False)

    def destroy_vm(self, network):
        vms_to_delete = []
        for vm in network.get_vms():
            vm.get_vm().delete(self.apiclient, expunge=True)
            vms_to_delete.append(vm)

        all_vms = network.get_vms()
        [all_vms.remove(vm) for vm in vms_to_delete]

    def stop_vm(self):
        for o in self.networks:
            for vm in o.get_vms():
                vm.get_vm().stop(self.apiclient)

    def start_vm(self):
        for o in self.networks:
            for vm in o.get_vms():
                vm.get_vm().start(self.apiclient)

    def delete_nat_rules(self):
        for o in self.networks:
            for vm in o.get_vms():
                if vm.get_nat() is not None:
                    vm.get_nat().delete(self.apiclient)
                    vm.set_nat(None)

    def delete_public_ip(self):
        for o in self.networks:
            for vm in o.get_vms():
                if vm.get_ip() is not None:
                    vm.get_ip().delete(self.apiclient)
                    vm.set_ip(None)
                    vm.set_nat(None)

    def add_nat_rules(self):
        for o in self.networks:
            for vm in o.get_vms():
                if vm.get_ip() is None:
                    vm.set_ip(self.acquire_publicip(o.get_net()))
                if vm.get_nat() is None:
                    vm.set_nat(self.create_natrule(vm.get_vm(), vm.get_ip(), o.get_net()))

    def do_vpc_test(self, expectFail):
        retries = 5
        if expectFail:
            retries = 2
        for o in self.networks:
            for vm in o.get_vms():
                self.check_ssh_into_vm(vm.get_vm(), vm.get_ip(), expectFail=expectFail, retries=retries)

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
