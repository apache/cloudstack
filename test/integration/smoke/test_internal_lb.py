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
""" Tests for configuring Internal Load Balancing Rules.
"""
# Import Local Modules
from marvin.codes import PASS, FAILED
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              get_process_status,
                              get_host_credentials)
from marvin.lib.base import (Domain,
                             Account,
                             Configurations,
                             VPC,
                             VpcOffering,
                             ServiceOffering,
                             NetworkOffering,
                             Network,
                             PublicIPAddress,
                             NATRule,
                             NetworkACL,
                             LoadBalancerRule,
                             ApplicationLoadBalancer,
                             VirtualMachine,
                             Template,
                             FireWallRule,
                             StaticNATRule,
                             NetworkACLList
                             )

from marvin.sshClient import SshClient


from marvin.lib.common import (get_zone,
                               get_domain,
                               get_test_template,
                               list_network_offerings)

from nose.plugins.attrib import attr

import logging
import time
import math


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
            "compute_offering": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
            },
            "network_offering": {
                "name": 'VPC Network offering',
                "displaytext": 'VPC Network',
                "guestiptype": 'Isolated',
                "supportedservices": 'Vpn,Dhcp,Dns,SourceNat,Lb,PortForwarding,UserData,StaticNat,NetworkACL',
                "traffictype": 'GUEST',
                "availability": 'Optional',
                "useVpc": 'on',
                "serviceProviderList": {
                    "Vpn": 'VpcVirtualRouter',
                    "Dhcp": 'VpcVirtualRouter',
                    "Dns": 'VpcVirtualRouter',
                    "SourceNat": 'VpcVirtualRouter',
                    "Lb": 'VpcVirtualRouter',
                    "PortForwarding": 'VpcVirtualRouter',
                    "UserData": 'VpcVirtualRouter',
                    "StaticNat": 'VpcVirtualRouter',
                    "NetworkACL": 'VpcVirtualRouter'
                },
            },
            "network_offering_internal_lb": {
                "name": 'VPC Network Internal Lb offering',
                "displaytext": 'VPC Network internal lb',
                "guestiptype": 'Isolated',
                "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,UserData,StaticNat,NetworkACL,Lb',
                "traffictype": 'GUEST',
                "availability": 'Optional',
                "useVpc": 'on',
                "serviceCapabilityList": {
                    "Lb": {
                        "SupportedLbIsolation": 'dedicated',
                        "lbSchemes": 'internal'
                    }
                },
                "serviceProviderList": {
                    "Dhcp": 'VpcVirtualRouter',
                    "Dns": 'VpcVirtualRouter',
                    "SourceNat": 'VpcVirtualRouter',
                    "PortForwarding": 'VpcVirtualRouter',
                    "UserData": 'VpcVirtualRouter',
                    "StaticNat": 'VpcVirtualRouter',
                    "NetworkACL": 'VpcVirtualRouter',
                    "Lb": 'InternalLbVm'
                },
                "egress_policy": "true",
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
                    "Lb" : ["InternalLbVm", "VpcVirtualRouter"],
                    "PortForwarding": 'VpcVirtualRouter',
                    "UserData": 'VpcVirtualRouter',
                    "StaticNat": 'VpcVirtualRouter',
                    "NetworkACL": 'VpcVirtualRouter'
                },
                "serviceCapabilityList": {
                    "SourceNat": {
                        "RedundantRouter": 'true'
                    },
                },
            },
            "vpc_offering": {
                "name": "VPC off",
                "displaytext": "VPC off",
                "supportedservices": "Dhcp,Dns,SourceNat,PortForwarding,Vpn,Lb,UserData,StaticNat,NetworkACL",
                "serviceProviderList": {
                    "Vpn": 'VpcVirtualRouter',
                    "Dhcp": 'VpcVirtualRouter',
                    "Dns": 'VpcVirtualRouter',
                    "SourceNat": 'VpcVirtualRouter',
                    "Lb" : ["InternalLbVm", "VpcVirtualRouter"],
                    "PortForwarding": 'VpcVirtualRouter',
                    "UserData": 'VpcVirtualRouter',
                    "StaticNat": 'VpcVirtualRouter',
                    "NetworkACL": 'VpcVirtualRouter'
                },
            },
            "vpc": {
                "name": "TestVPC",
                "displaytext": "TestVPC",
                "cidr": '10.1.0.0/16'
            },
            "network": {
                "name": "Test Network",
                "displaytext": "Test Network",
                "netmask": '255.255.255.0'
            },
            "lbrule": {
                "name": "SSH",
                "alg": "roundrobin",
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
                "alg": "roundrobin",
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
            }
        }


class TestInternalLb(cloudstackTestCase):

    """Test Internal LB
    """

    @classmethod
    def setUpClass(cls):

        cls.logger = logging.getLogger('TestInternalLb')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        testClient = super(TestInternalLb, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = Services().services

        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.domain = get_domain(cls.apiclient)
        cls.logger.debug("Creating compute offering: %s" %cls.services["compute_offering"]["name"])
        cls.compute_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["compute_offering"]
        )

        cls.account = Account.create(
            cls.apiclient, services=cls.services["account"])

        cls.hypervisor = testClient.getHypervisorInfo()

        cls.template = get_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor
        )

        if cls.template == FAILED:
            assert False, "get_test_template() failed to return template"

        cls.logger.debug("Successfully created account: %s, id: \
                   %s" % (cls.account.name,
                          cls.account.id))

        cls._cleanup = [cls.account, cls.compute_offering]
        return

    def setUp(self):
        self.cleanup = []

    def get_networkoffering_state(self, offering):
        result = list_network_offerings(self.apiclient, id=offering.id)
        if result:
            offering = result[0]
            return offering.state
        else:
            return None

    def create_and_enable_network_serviceoffering(self, services):

        try:
            # Create offering
            offering = NetworkOffering.create(
                self.apiclient, services, conservemode=False)

            self.assertIsNotNone(offering, "Failed to create network offering")
            self.logger.debug("Created network offering: %s" % offering.id)

            if offering:
                # Enable offeringq
                offering.update(self.apiclient, state="Enabled")
                self.assertEqual(self.get_networkoffering_state(
                    offering), "Enabled", "Failed to enable network offering")

                self.logger.debug("Enabled network offering: %s" % offering.id)

                self.cleanup.insert(0, offering)
                return offering

        except Exception as e:
            self.fail("Failed to create and enable network offering due to %s" % e)

    def create_vpc(self, vpc_offering):
        self.logger.debug("Creating VPC using offering ==> ID %s / Name %s" % (vpc_offering.id, vpc_offering.name))
        try:
            vpc = VPC.create(
                apiclient=self.apiclient,
                services=self.services["vpc"],
                networkDomain="vpc.internallb",
                vpcofferingid=vpc_offering.id,
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.domain.id
            )

            self.assertIsNotNone(vpc, "VPC creation failed")
            self.logger.debug("Created VPC %s" % vpc.id)

            self.cleanup.insert(0, vpc)
            return vpc

        except Exception as e:
            self.fail("Failed to create VPC due to %s" % e)

    def create_network_tier(self, name, vpcid, gateway, network_offering):
        self.services["network"]["name"] = name
        self.services["network"]["displaytext"] = name

        default_acl = NetworkACLList.list(self.apiclient, name="default_allow")[0]

        try:
            network = Network.create(
                apiclient=self.apiclient,
                services=self.services["network"],
                accountid=self.account.name,
                domainid=self.domain.id,
                networkofferingid=network_offering.id,
                zoneid=self.zone.id,
                vpcid=vpcid,
                gateway=gateway,
                netmask=self.services["network"]["netmask"],
                aclid=default_acl.id
            )

            self.assertIsNotNone(network, "Network failed to create")
            self.logger.debug(
                "Created network %s in VPC %s" % (network.id, vpcid))

            self.cleanup.insert(0, network)
            return network

        except Exception as e:
            raise Exception("Create network failed: %s" % e)

    def deployvm_in_network(self, vpc, networkid):

        try:
            self.services["virtual_machine"]["networkids"] = networkid
            vm = VirtualMachine.create(self.apiclient, services=self.services["virtual_machine"],
                                       templateid=self.template.id,
                                       zoneid=self.zone.id,
                                       accountid=self.account.name,
                                       domainid=self.domain.id,
                                       serviceofferingid=self.compute_offering.id,
                                       hypervisor=self.hypervisor
                                       )
            self.assertIsNotNone(
                vm, "Failed to deploy vm in network: %s" % networkid)
            self.assertTrue(vm.state == 'Running', "VM is not running")
            self.logger.debug("Deployed VM id: %s in VPC %s" % (vm.id, vpc.id))

            self.cleanup.insert(0, vm)
            return vm

        except Exception as e:
            raise Exception("Deployment failed of VM: %s" % e)

    def create_internal_loadbalancer(self, intport, sport, algorithm, networkid):
        try:
            # 5) Create an Internal Load Balancer
            applb = ApplicationLoadBalancer.create(self.apiclient, services=self.services,
                                                   name="lbrule",
                                                   sourceport=sport,
                                                   instanceport=intport,
                                                   algorithm=algorithm,
                                                   scheme="Internal",
                                                   sourcenetworkid=networkid,
                                                   networkid=networkid
                                                   )

            self.assertIsNotNone(applb, "Failed to create loadbalancer")
            self.logger.debug("Created LB %s in VPC" % applb.id)

            return applb

        except Exception as e:
            self.fail(e)

    def acquire_publicip(self, vpc, network):
        self.logger.debug(
            "Associating public IP for network: %s" % network.name)
        public_ip = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network.id,
            vpcid=vpc.id
        )
        self.assertIsNotNone(public_ip, "Failed to acquire public IP")
        self.logger.debug("Associated %s with network %s" % (
            public_ip.ipaddress.ipaddress,
            network.id
        ))
        return public_ip

    def create_natrule(self, vpc, vm, public_port, private_port, public_ip, network, services=None):
        self.logger.debug("Creating NAT rule in network for vm with public IP")
        if not services:
            self.services["natrule"]["privateport"] = private_port
            self.services["natrule"]["publicport"] = public_port
            self.services["natrule"]["startport"] = public_port
            self.services["natrule"]["endport"] = public_port
            services = self.services["natrule"]

        nat_rule = NATRule.create(
            apiclient=self.apiclient,
            services=services,
            ipaddressid=public_ip.ipaddress.id,
            virtual_machine=vm,
            networkid=network.id
        )
        self.assertIsNotNone(
            nat_rule, "Failed to create NAT Rule for %s" % public_ip.ipaddress.ipaddress)
        self.logger.debug(
            "Adding NetworkACL rules to make NAT rule accessible")

        vm.ssh_ip = nat_rule.ipaddress
        vm.public_ip = nat_rule.ipaddress
        vm.public_port = int(public_port)
        return nat_rule

    def get_ssh_client(self, vm, retries):
        """ Setup ssh client connection and return connection
        vm requires attributes public_ip, public_port, username, password """

        ssh_client = None
        try:
            ssh_client = SshClient(
                vm.public_ip,
                vm.public_port,
                vm.username,
                vm.password,
                retries
            )
        except Exception as e:
            self.fail("Unable to create ssh connection: " % e)

        self.assertIsNotNone(
            ssh_client, "Failed to setup ssh connection to vm=%s on public_ip=%s" % (vm.name, vm.public_ip))
        return ssh_client

    def setup_http_daemon(self, vm):
        """ Creates a index.html in /tmp with private ip as content and
            starts httpd daemon on all interfaces port 80 serving /tmp/
            (only tested on the busybox based tiny vm)
            vm requires attributes public_ip, public_port, username, password
        """
        commands = [
            # using ip address instead of hostname
            "/sbin/ip addr show eth0 |grep 'inet '| cut -f6 -d' ' > /tmp/index.html",
            "/usr/sbin/httpd -v -p 0.0.0.0:80 -h /tmp/"
        ]
        try:
            ssh_client = self.get_ssh_client(vm, 8)
            for cmd in commands:
                ssh_client.execute(cmd)
        except Exception as e:
            self.fail("Failed to ssh into vm: %s due to %s" % (vm, e))

    def run_ssh_test_accross_hosts(self, clienthost, lb_address, max_requests=30):
        """ Uses clienthost to run wgets on hosts port 80 expects a unique output from url.
            returns a list of outputs to evaluate.
        """
        # Setup ssh connection
        ssh_client = self.get_ssh_client(clienthost, 8)
        self.logger.debug(ssh_client)
        results = []

        try:
            for x in range(0, max_requests):
                cmd_test_http = "/usr/bin/wget -T2 -qO- http://" + \
                    lb_address + "/ 2>/dev/null"
                # self.debug( "SSH into VM public address: %s and port: %s"
                # %(.public_ip, vm.public_port))
                results.append(ssh_client.execute(cmd_test_http)[0])
                self.logger.debug(results)

        except Exception as e:
            self.fail("%s: SSH failed for VM with IP Address: %s" %
                      (e, clienthost.public_ip))

        return results

    def get_std_deviation(self, data):
        """ Calculates and outputs a mean, variance and standard deviation from an input list of values """
        num_val = len(data)
        mean = sum(data) / num_val
        sqrt = [math.pow(abs(x - mean), 2) for x in data]
        variance = (sum(sqrt) / num_val - 1)
        stddev = math.sqrt(variance)
        return (mean, variance, stddev)

    def evaluate_http_responses(self, responses, algorithm):
        """ Evaluates response values from http test and verifies algorithm used"""
        if algorithm == 'roundrobin':
            # get a list of unique values
            unique_values = set(responses)
            # count the occurence of each value in the responses
            dataset = [responses.count(value) for value in unique_values]

            if len(set(dataset)) == 1:
                # all values in dataset are equal, perfect result distribution
                # woohoo!
                self.logger.debug(
                    "HTTP responses are evenly distributed! SUCCESS!")
                return True
            else:
                # calculate mean, var, stddev on dataset
                mean, variance, stddev = self.get_std_deviation(dataset)
                for value in dataset:
                    # determine how much value difference is there from the
                    # mean
                    difference = abs(value - mean)
                    # difference between response count of a host and the mean
                    # should be less than the standard deviation
                    self.assertLess(
                        difference, stddev, "Internal LB RoundRobin test Failed because http responsest are not evenly distributed")
                    self.logger.debug(
                        "Response distribution count: %d difference to mean: %d within standard deviation: %d" % (value, mean, stddev))
                return True

    @attr(tags=["smoke", "advanced"], required_hardware="true")
    def test_01_internallb_roundrobin_1VPC_3VM_HTTP_port80(self):
        """
           Test create, assign, remove of an Internal LB with roundrobin http traffic to 3 vm's in a Single VPC
        """
        self.logger.debug("Starting test_01_internallb_roundrobin_1VPC_3VM_HTTP_port80")

        self.logger.debug("Creating a VPC offering..")
        vpc_offering = VpcOffering.create(
            self.apiclient,
            self.services["vpc_offering"])

        self.logger.debug("Enabling the VPC offering created")
        vpc_offering.update(self.apiclient, state='Enabled')

        self.cleanup.insert(0, vpc_offering)
        self.execute_internallb_roundrobin_tests(vpc_offering)

    @attr(tags=["smoke", "advanced"], required_hardware="true")
    def test_02_internallb_roundrobin_1RVPC_3VM_HTTP_port80(self):
        """
           Test create, assign, remove of an Internal LB with roundrobin http traffic to 3 vm's in a Redundant VPC
        """
        self.logger.debug("Starting test_02_internallb_roundrobin_1RVPC_3VM_HTTP_port80")

        self.logger.debug("Creating a Redundant VPC offering..")
        redundant_vpc_offering = VpcOffering.create(
            self.apiclient,
            self.services["redundant_vpc_offering"])

        self.logger.debug("Enabling the Redundant VPC offering created")
        redundant_vpc_offering.update(self.apiclient, state='Enabled')

        self.cleanup.insert(0, redundant_vpc_offering)
        self.execute_internallb_roundrobin_tests(redundant_vpc_offering)

    def execute_internallb_roundrobin_tests(self, vpc_offering):
        max_http_requests = 30
        algorithm = "roundrobin"
        public_lb_port = 80
        private_http_port = 80
        public_ssh_start_port = 2000
        num_app_vms = 3

        self.logger.debug("Starting test_01_internallb_roundrobin_1VPC_3VM_HTTP_port80")
        # Create and enable network offerings
        network_offering_guestnet = self.create_and_enable_network_serviceoffering(
            self.services["network_offering"])
        network_offering_intlb = self.create_and_enable_network_serviceoffering(
            self.services["network_offering_internal_lb"])

        # Create VPC
        vpc = self.create_vpc(vpc_offering)

        # Create network tiers
        network_guestnet = self.create_network_tier(
            "guestnet_test01", vpc.id, "10.1.1.1",  network_offering_guestnet)
        network_internal_lb = self.create_network_tier(
            "intlb_test01", vpc.id, "10.1.2.1",  network_offering_intlb)

        # Create 1 lb client vm in guestnet network tier
        client_vm = self.deployvm_in_network(vpc, network_guestnet.id)

        # Create X app vm's in internal lb network tier
        app_vms = []
        for x in range(0, num_app_vms):
            vm = None
            vm = self.deployvm_in_network(vpc, network_internal_lb.id)
            app_vms.append(vm)

        # Acquire public ip to access guestnet app vms
        guestnet_public_ip = self.acquire_publicip(vpc, network_guestnet)
        intlb_public_ip = self.acquire_publicip(vpc, network_internal_lb)

        # Create nat rule to access client vm
        self.create_natrule(vpc, client_vm, public_ssh_start_port, 22, guestnet_public_ip, network_guestnet)

        # Create nat rule to access app vms directly and start a http daemon on
        # the vm
        public_port = public_ssh_start_port + 1
        for vm in app_vms:
            self.create_natrule(vpc, vm, public_port, 22, intlb_public_ip, network_internal_lb)
            public_port += 1
            time.sleep(10)
            # start http daemon on vm's
            self.setup_http_daemon(vm)

        # Create a internal loadbalancer in the internal lb network tier
        applb = self.create_internal_loadbalancer( private_http_port, public_lb_port, algorithm, network_internal_lb.id)
        # wait for the loadbalancer to boot and be configured
        time.sleep(10)
        # Assign the 2 VMs to the Internal Load Balancer
        self.logger.debug("Assigning virtual machines to LB: %s" % applb.id)
        try:
            applb.assign(self.apiclient, vms=app_vms)
        except Exception as e:
            self.fail(
                "Failed to assign virtual machine(s) to loadbalancer: %s" % e)

        time.sleep(120)
        # self.logger.debug(dir(applb))
        results = self.run_ssh_test_accross_hosts(
            client_vm, applb.sourceipaddress, max_http_requests)
        success = self.evaluate_http_responses(results, algorithm)
        self.assertTrue(success, "Test failed on algorithm: %s" % algorithm)

        self.logger.debug(
            "Removing virtual machines and networks for test_01_internallb_roundrobin_2VM_port80")

        # Remove the virtual machines from the Internal LoadBalancer
        self.logger.debug("Remove virtual machines from LB: %s" % applb.id)
        applb.remove(self.apiclient, vms=app_vms)

        # Remove the Load Balancer
        self.logger.debug("Deleting LB: %s" % applb.id)
        applb.delete(self.apiclient)

    def get_lb_stats_settings(self):
        self.logger.debug("Retrieving haproxy stats settings")
        settings = {}
        try:
            settings["stats_port"] = Configurations.list(
                self.apiclient, name="network.loadbalancer.haproxy.stats.port")[0].value
            settings["stats_uri"] = Configurations.list(
                self.apiclient, name="network.loadbalancer.haproxy.stats.uri")[0].value
            # Update global setting network.loadbalancer.haproxy.stats.auth to a known value
            haproxy_auth = "admin:password"
            Configurations.update(self.apiclient, "network.loadbalancer.haproxy.stats.auth", haproxy_auth)
            self.logger.debug(
                "Updated global setting stats network.loadbalancer.haproxy.stats.auth to %s" % (haproxy_auth))
            settings["username"], settings["password"] = haproxy_auth.split(":")
            settings["visibility"] = Configurations.list(
                self.apiclient, name="network.loadbalancer.haproxy.stats.visibility")[0].value
            self.logger.debug(settings)
        except Exception as e:
            self.fail("Failed to retrieve stats settings " % e)

        return settings

    def verify_lb_stats(self, stats_ip, ssh_client, settings):

        word_to_verify = "uptime"

        url = "http://" + stats_ip + ":" + \
            settings["stats_port"] + settings["stats_uri"]
        get_contents = "/usr/bin/wget -T3 -qO- --user=" + \
            settings["username"] + " --password=" + \
            settings["password"] + " " + url
        try:
            self.logger.debug(
                "Trying to connect to the haproxy stats url %s" % url)
            result = ssh_client.execute(get_contents)
        except Exception as e:
            self.fail("Failed to verify admin stats url %s from: %s" %
                      (url, ssh_client))
        finally:
            del ssh_client

        found = any(word_to_verify in word for word in result)

        if found:
            return True
        else:
            return False

    @attr(tags=["smoke", "advanced"], required_hardware="true")
    def test_03_vpc_internallb_haproxy_stats_on_all_interfaces(self):
        """ Test to verify access to loadbalancer haproxy admin stats page
            when global setting network.loadbalancer.haproxy.stats.visibility is set to 'all'
            with credentials from global setting network.loadbalancer.haproxy.stats.auth
            using the uri from global setting network.loadbalancer.haproxy.stats.uri.

            It uses a Single Router VPC
        """
        self.logger.debug("Starting test_03_vpc_internallb_haproxy_stats_on_all_interfaces")

        self.logger.debug("Creating a VPC offering..")
        vpc_offering = VpcOffering.create(
            self.apiclient,
            self.services["vpc_offering"])

        self.logger.debug("Enabling the VPC offering created")
        vpc_offering.update(self.apiclient, state='Enabled')

        self.execute_internallb_haproxy_tests(vpc_offering)

    @attr(tags=["smoke", "advanced"], required_hardware="true")
    def test_04_rvpc_internallb_haproxy_stats_on_all_interfaces(self):
        """ Test to verify access to loadbalancer haproxy admin stats page
            when global setting network.loadbalancer.haproxy.stats.visibility is set to 'all'
            with credentials from global setting network.loadbalancer.haproxy.stats.auth
            using the uri from global setting network.loadbalancer.haproxy.stats.uri.

            It uses a Redundant Routers VPC
        """
        self.logger.debug("Starting test_04_rvpc_internallb_haproxy_stats_on_all_interfaces")

        self.logger.debug("Creating a Redundant VPC offering..")
        redundant_vpc_offering = VpcOffering.create(
            self.apiclient,
            self.services["redundant_vpc_offering"])

        self.logger.debug("Enabling the Redundant VPC offering created")
        redundant_vpc_offering.update(self.apiclient, state='Enabled')

        self.execute_internallb_haproxy_tests(redundant_vpc_offering)

    def execute_internallb_haproxy_tests(self, vpc_offering):

        settings = self.get_lb_stats_settings()

        dummy_port = 90
        network_gw = "10.1.2.1"
        default_visibility = "global"

        # Update global setting if it is not set to our test default
        if settings["visibility"] != default_visibility:
            config_update = Configurations.update(
                self.apiclient, "network.loadbalancer.haproxy.stats.visibility", default_visibility)
            self.logger.debug(
                "Updated global setting stats haproxy.stats.visibility to %s" % (default_visibility))
            settings = self.get_lb_stats_settings()

        # Create and enable network offering
        network_offering_intlb = self.create_and_enable_network_serviceoffering(
            self.services["network_offering_internal_lb"])

        # Create VPC
        vpc = self.create_vpc(vpc_offering)

        # Create network tier with internal lb service enabled
        network_internal_lb = self.create_network_tier(
            "intlb_test02", vpc.id, network_gw,  network_offering_intlb)

        # Create 1 lb vm in internal lb network tier
        vm = self.deployvm_in_network(vpc, network_internal_lb.id)

        # Acquire 1 public ip and attach to the internal lb network tier
        public_ip = self.acquire_publicip(vpc, network_internal_lb)

        # Create an internal loadbalancer in the internal lb network tier
        applb = self.create_internal_loadbalancer(
            dummy_port, dummy_port, "leastconn", network_internal_lb.id)

        # Assign the 1 VM to the Internal Load Balancer
        self.logger.debug("Assigning virtual machines to LB: %s" % applb.id)
        try:
            applb.assign(self.apiclient, vms=[vm])
        except Exception as e:
            self.fail(
                "Failed to assign virtual machine(s) to loadbalancer: %s" % e)

        # Create nat rule to access client vm
        self.create_natrule(
            vpc, vm, "22", "22", public_ip, network_internal_lb)

        # Verify access to and the contents of the admin stats page on the
        # private address via a vm in the internal lb tier
        stats = self.verify_lb_stats(
            applb.sourceipaddress, self.get_ssh_client(vm, 5), settings)
        self.assertTrue(stats, "Failed to verify LB HAProxy stats")

    @classmethod
    def tearDownClass(cls):
        try:
            cls.logger.debug("Cleaning up class resources")
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Cleanup failed with %s" % e)

    def tearDown(self):
        try:
            self.logger.debug("Cleaning up test resources")
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Cleanup failed with %s" % e)
