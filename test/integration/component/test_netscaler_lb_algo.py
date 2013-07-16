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

""" P1 tests for netscaler load balancing
"""
#Import Local Modules
import marvin
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from marvin.remoteSSHClient import remoteSSHClient
import datetime


class Services:
    """Test netscaler services
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
                                    "cpuspeed": 100, # in MHz
                                    "memory": 128, # In MBs
                         },
                         "virtual_machine": {
                                    "displayname": "TestVM",
                                    "username": "root",
                                    "password": "password",
                                    "ssh_port": 22,
                                    "hypervisor": 'XenServer',
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": 'TCP',
                                },
                         "netscaler": {
                                "ipaddress": '10.147.60.27',
                                "username": 'nsroot',
                                "password": 'nsroot',
                                "networkdevicetype": 'NetscalerVPXLoadBalancer',
                                "publicinterface": '1/1',
                                "privateinterface": '1/1',
                                "numretries": 2,
                                "lbdevicededicated": False,
                                "lbdevicecapacity": 50,
                                "port": 22,
                         },
                         "network_offering": {
                                    "name": 'Netscaler',
                                    "displaytext": 'Netscaler',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Firewall,Lb,UserData,StaticNat',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "serviceProviderList": {
                                            "Dhcp": 'VirtualRouter',
                                            "Dns": 'VirtualRouter',
                                            "SourceNat": 'VirtualRouter',
                                            "PortForwarding": 'VirtualRouter',
                                            "Vpn": 'VirtualRouter',
                                            "Firewall": 'VirtualRouter',
                                            "Lb": 'Netscaler',
                                            "UserData": 'VirtualRouter',
                                            "StaticNat": 'VirtualRouter',
                                    },
                         },
                         "network": {
                                  "name": "Netscaler",
                                  "displaytext": "Netscaler",
                         },
                         "lbrule": {
                                    "name": "SSH",
                                    "alg": "leastconn",
                                    # Algorithm used for load balancing
                                    "privateport": 22,
                                    "publicport": 22,
                                    "openfirewall": False,
                         },
                         "ostype": 'CentOS 5.3 (64-bit)',
                         # Cent OS 5.3 (64 bit)
                         "sleep": 60,
                         "timeout": 10,
                         "mode": 'advanced'
                    }


class TestLbWithRoundRobin(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestLbWithRoundRobin,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )

        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls._cleanup = [
                        cls.network_offering,
                        cls.service_offering
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
            self.debug("Cleaning up the resources")
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            interval = list_configurations(
                                    self.apiclient,
                                    name='network.gc.interval'
                                    )
            wait = list_configurations(
                                    self.apiclient,
                                    name='network.gc.wait'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) + int(wait[0].value))
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advancedns"])
    def test_lb_with_round_robin(self):
        """Test Create LB rule with round robin algorithm
        """

        # Validate the following
        # 1. Deploy the first VM using a network from the above created
        #    Network offering.
        # 2. Deploy another VM.
        # 3. Acquire Ip address and create LB rule with round robin algorithm
        #    on it. Verify that "Roundrobin" algorithm is applied when using
        #    this load balancing rule.

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        self.network = Network.create(
                                    self.apiclient,
                                    self.services["network"],
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        self.virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=self.virtual_machine.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % self.virtual_machine.id
            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Running",
                            "VM state should be running after deployment"
                        )

        self.debug("Associating public IP for network: %s" % self.network.id)

        ip_with_lb_rule = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=self.network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        ip_with_lb_rule.ipaddress.ipaddress,
                                        self.network.id
                                        ))
        self.debug(
            "Creating LB rule for IP address: %s with round robin algo" %
                                        ip_with_lb_rule.ipaddress.ipaddress)

        self.services["lbrule"]["alg"] = 'roundrobin'
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=ip_with_lb_rule.ipaddress.id,
                                    accountid=self.account.name,
                                    networkid=self.network.id
                                )

        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=lb_rule.id,
                                         listall=True
                                         )
        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List LB rules should return a newly created LB rule"
                         )
        self.debug("Adding %s to the LB rule %s" % (
                                                self.virtual_machine.name,
                                                lb_rule.name
                                                ))
        lb_rule.assign(self.apiclient, [self.virtual_machine])
        self.debug("SSH into Netscaler to check whether algorithm is configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
                                    )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                                        ip_with_lb_rule.ipaddress.ipaddress,
                                        lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                    result.count("Configured Method: ROUNDROBIN"),
                    1,
                    "'ROUNDROBIN' algorithm should be configured on NS"
                    )

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))
        return


class TestLbWithLeastConn(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestLbWithLeastConn,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )

        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )

        cls._cleanup = [
                        cls.network_offering,
                        cls.service_offering
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
            self.debug("Cleaning up the resources")
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            interval = list_configurations(
                                    self.apiclient,
                                    name='network.gc.interval'
                                    )
            wait = list_configurations(
                                    self.apiclient,
                                    name='network.gc.wait'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) + int(wait[0].value))
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advancedns"])
    def test_lb_with_least_conn(self):
        """Test Create LB rule with least connection algorithm
        """

        # Validate the following
        # 1. Deploy the first VM using a network from the above created
        #    Network offering.
        # 2. Deploy another VM.
        # 3. Acquire Ip address and create LB rule with round robin algorithm
        #    on it. Verify that "leastconn" algorithm is applied when using
        #    this load balancing rule.

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        self.network = Network.create(
                                    self.apiclient,
                                    self.services["network"],
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        self.virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=self.virtual_machine.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % self.virtual_machine.id
            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Running",
                            "VM state should be running after deployment"
                        )

        self.debug("Associating public IP for network: %s" % self.network.id)

        PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=self.network.id
                                )
        ip_with_lb_rule = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=self.network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        ip_with_lb_rule.ipaddress.ipaddress,
                                        self.network.id
                                        ))
        self.debug(
            "Creating LB rule for IP address: %s with round robin algo" %
                                        ip_with_lb_rule.ipaddress.ipaddress)

        self.services["lbrule"]["alg"] = 'leastconn'
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=ip_with_lb_rule.ipaddress.id,
                                    accountid=self.account.name,
                                    networkid=self.network.id
                                )

        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=lb_rule.id,
                                         listall=True
                                         )
        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List LB rules should return a newly created LB rule"
                         )
        self.debug("Adding %s to the LB rule %s" % (
                                                self.virtual_machine.name,
                                                lb_rule.name
                                                ))
        lb_rule.assign(self.apiclient, [self.virtual_machine])

        self.debug("SSH into Netscaler to check whether algorithm is configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
                                    )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                                        ip_with_lb_rule.ipaddress.ipaddress,
                                        lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                    result.count("Configured Method: LEASTCONNECTION"),
                    1,
                    "'LEASTCONNECTION' algorithm should be configured on NS"
                    )

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))
        return


class TestLbWithSourceIp(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestLbWithSourceIp,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )

        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls._cleanup = [
                        cls.network_offering,
                        cls.service_offering
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
            self.debug("Cleaning up the resources")
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            interval = list_configurations(
                                    self.apiclient,
                                    name='network.gc.interval'
                                    )
            wait = list_configurations(
                                    self.apiclient,
                                    name='network.gc.wait'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) + int(wait[0].value))
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advancedns"])
    def test_lb_with_source_ip(self):
        """Test Create LB rule with source Ip algorithm
        """

        # Validate the following
        # 1. Deploy the first VM using a network from the above created
        #    Network offering.
        # 2. Deploy another VM.
        # 3. Acquire Ip address and create LB rule with round robin algorithm
        #    on it. Verify that "sourceIp" algorithm is applied when using
        #    this load balancing rule.

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        self.network = Network.create(
                                    self.apiclient,
                                    self.services["network"],
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        self.virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
                                        self.apiclient,
                                        id=self.virtual_machine.id
                                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % self.virtual_machine.id
            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Running",
                            "VM state should be running after deployment"
                        )

        self.debug("Associating public IP for network: %s" % self.network.id)

        ip_with_lb_rule = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=self.network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        ip_with_lb_rule.ipaddress.ipaddress,
                                        self.network.id
                                        ))
        self.debug(
            "Creating LB rule for IP address: %s with round robin algo" %
                                        ip_with_lb_rule.ipaddress.ipaddress)

        self.services["lbrule"]["alg"] = 'source'
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=ip_with_lb_rule.ipaddress.id,
                                    accountid=self.account.name,
                                    networkid=self.network.id
                                )

        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=lb_rule.id,
                                         listall=True
                                         )
        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List LB rules should return a newly created LB rule"
                         )

        self.debug("Adding %s to the LB rule %s" % (
                                                self.virtual_machine.name,
                                                lb_rule.name
                                                ))
        lb_rule.assign(self.apiclient, [self.virtual_machine])
        self.debug("SSH into Netscaler to check whether algorithm is configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
                                    )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                                        ip_with_lb_rule.ipaddress.ipaddress,
                                        lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                    result.count("Configured Method: SOURCEIPHASH"),
                    1,
                    "'SOURCEIPHASH' algorithm should be configured on NS"
                    )

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))
        return


class TestLbAlgoRrLc(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestLbAlgoRrLc,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')
        cls.account = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     admin=True,
                                     domainid=cls.domain.id
                                     )
        cls.network = Network.create(
                                    cls.api_client,
                                    cls.services["network"],
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    networkofferingid=cls.network_offering.id,
                                    zoneid=cls.zone.id
                                    )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )

        # Spawn an instance in that network
        cls.virtual_machine = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering.id,
                                  networkids=[str(cls.network.id)]
                                  )
        cls.public_ip = PublicIPAddress.create(
                                cls.api_client,
                                accountid=cls.account.name,
                                zoneid=cls.zone.id,
                                domainid=cls.account.domainid,
                                networkid=cls.network.id
                                )
        cls._cleanup = [
                        cls.account,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
            interval = list_configurations(
                                    cls.api_client,
                                    name='network.gc.interval'
                                    )
            wait = list_configurations(
                                    cls.api_client,
                                    name='network.gc.wait'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) + int(wait[0].value))
            cls.network_offering.delete(cls.api_client)
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
            self.debug("Cleaning up the resources")
            # Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advancedns"])
    def test_lb_round_robin_to_least_conn(self):
        """Test edit LB rule from round robin to least connection algo
        """

        # Validate the following
        # 1. Deploy the first VM using a network from the above created
        #    Network offering.
        # 2. Deploy another VM.
        # 3. Acquire Ip address and create an Lb rule for couple of Vms using
        #    "RounbRobin" algorithm. Make sure this algorithm is respected.
        # 4. Edit this existing LB rule with "Round robin" algorithm to
        #    "LeastConn" After the update, Verify that "least Connection"
        #    algorithm is applied when using this load balancing rule.

        self.debug(
            "Creating LB rule for IP address: %s with round robin algo" %
                                        self.public_ip.ipaddress.ipaddress)

        self.services["lbrule"]["alg"] = 'roundrobin'
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=self.public_ip.ipaddress.id,
                                    accountid=self.account.name,
                                    networkid=self.network.id
                                )
        self.cleanup.append(lb_rule)

        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=lb_rule.id,
                                         listall=True
                                         )
        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List LB rules should return a newly created LB rule"
                         )

        self.debug("Adding %s to the LB rule %s" % (
                                                self.virtual_machine.name,
                                                lb_rule.name
                                                ))
        lb_rule.assign(self.apiclient, [self.virtual_machine])

        self.debug("SSH into Netscaler to check whether algorithm is configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
                                    )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                                        self.public_ip.ipaddress.ipaddress,
                                        lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                    result.count("Configured Method: ROUNDROBIN"),
                    1,
                    "'ROUNDROBIN' algorithm should be configured on NS"
                    )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))

        self.debug(
                "Updating LB rule: %s with new algorithm: %s" % (
                                                                lb_rule.name,
                                                                'leastconn'))
        lb_rule.update(self.apiclient, algorithm='leastconn')

        self.debug("SSH into Netscaler to check whether algorithm is configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
                                    )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                                        self.public_ip.ipaddress.ipaddress,
                                        lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                    result.count("Configured Method: LEASTCONNECTION"),
                    1,
                    "'LEASTCONNECTION' algorithm should be configured on NS"
                    )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))
        return


class TestLbAlgoLcRr(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestLbAlgoLcRr,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')
        cls.account = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     admin=True,
                                     domainid=cls.domain.id
                                     )
        cls.network = Network.create(
                                    cls.api_client,
                                    cls.services["network"],
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    networkofferingid=cls.network_offering.id,
                                    zoneid=cls.zone.id
                                    )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )

        # Spawn an instance in that network
        cls.virtual_machine = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering.id,
                                  networkids=[str(cls.network.id)]
                                  )
        cls.public_ip = PublicIPAddress.create(
                                cls.api_client,
                                accountid=cls.account.name,
                                zoneid=cls.zone.id,
                                domainid=cls.account.domainid,
                                networkid=cls.network.id
                                )
        cls._cleanup = [
                        cls.account,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
            interval = list_configurations(
                                    cls.api_client,
                                    name='network.gc.interval'
                                    )
            wait = list_configurations(
                                    cls.api_client,
                                    name='network.gc.wait'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) + int(wait[0].value))
            cls.network_offering.delete(cls.api_client)
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
            self.debug("Cleaning up the resources")
            # Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advancedns"])
    def test_lb_least_conn_to_round_robin(self):
        """Test edit LB rule from least conn to round robin algo
        """

        # Validate the following
        # 1. Deploy the first VM using a network from the above created
        #    Network offering.
        # 2. Deploy another VM.
        # 3. Acquire Ip address and create an Lb rule for couple of Vms using
        #    "Leastconn" algorithm. Make sure this algorithm is respected.
        # 4. Edit this existing LB rule with "Least conn" algorithm to
        #    "roundrobin" After the update, Verify that "round robin"
        #    algorithm is applied when using this load balancing rule.

        self.debug(
            "Creating LB rule for IP address: %s with least conn algo" %
                                        self.public_ip.ipaddress.ipaddress)

        self.services["lbrule"]["alg"] = 'leastconn'
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=self.public_ip.ipaddress.id,
                                    accountid=self.account.name,
                                    networkid=self.network.id
                                )
        self.cleanup.append(lb_rule)

        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=lb_rule.id,
                                         listall=True
                                         )
        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List LB rules should return a newly created LB rule"
                         )
        self.debug("Adding %s to the LB rule %s" % (
                                                self.virtual_machine.name,
                                                lb_rule.name
                                                ))
        lb_rule.assign(self.apiclient, [self.virtual_machine])
        self.debug("SSH into Netscaler to check whether algorithm is configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
                                    )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                                        self.public_ip.ipaddress.ipaddress,
                                        lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                    result.count("Configured Method: LEASTCONNECTION"),
                    1,
                    "'LEASTCONNECTION' algorithm should be configured on NS"
                    )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))

        self.debug(
                "Updating LB rule: %s with new algorithm: %s" % (
                                                                lb_rule.name,
                                                                'roundrobin'))
        lb_rule.update(self.apiclient, algorithm='roundrobin')
        self.debug("SSH into Netscaler to check whether algorithm is configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
                                    )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                                        self.public_ip.ipaddress.ipaddress,
                                        lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                    result.count("Configured Method: ROUNDROBIN"),
                    1,
                    "'ROUNDROBIN' algorithm should be configured on NS"
                    )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))
        return


class TestLbAlgoRrSb(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestLbAlgoRrSb,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')
        cls.account = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     admin=True,
                                     domainid=cls.domain.id
                                     )
        cls.network = Network.create(
                                    cls.api_client,
                                    cls.services["network"],
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    networkofferingid=cls.network_offering.id,
                                    zoneid=cls.zone.id
                                    )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )

        # Spawn an instance in that network
        cls.virtual_machine = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering.id,
                                  networkids=[str(cls.network.id)]
                                  )

        cls.public_ip = PublicIPAddress.create(
                                cls.api_client,
                                accountid=cls.account.name,
                                zoneid=cls.zone.id,
                                domainid=cls.account.domainid,
                                networkid=cls.network.id
                                )
        cls._cleanup = [
                        cls.account,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
            interval = list_configurations(
                                    cls.api_client,
                                    name='network.gc.interval'
                                    )
            wait = list_configurations(
                                    cls.api_client,
                                    name='network.gc.wait'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) + int(wait[0].value))
            cls.network_offering.delete(cls.api_client)
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
            self.debug("Cleaning up the resources")
            # Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advancedns"])
    def test_lb_round_robin_to_source(self):
        """Test edit LB rule from round robin to source algo
        """

        # Validate the following
        # 1. Deploy the first VM using a network from the above created
        #    Network offering.
        # 2. Deploy another VM.
        # 3. Acquire Ip address and create an Lb rule for couple of Vms using
        #    "RounbRobin" algorithm. Make sure this algorithm is respected.
        # 4. Edit this existing LB rule with "Round robin" algorithm to
        #    "Source" After the update, Verify that "Source"
        #    algorithm is applied when using this load balancing rule.

        self.debug(
            "Creating LB rule for IP address: %s with round robin algo" %
                                        self.public_ip.ipaddress.ipaddress)

        self.services["lbrule"]["alg"] = 'roundrobin'
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=self.public_ip.ipaddress.id,
                                    accountid=self.account.name,
                                    networkid=self.network.id
                                )
        self.cleanup.append(lb_rule)

        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=lb_rule.id,
                                         listall=True
                                         )
        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List LB rules should return a newly created LB rule"
                         )
        self.debug("Adding %s to the LB rule %s" % (
                                                self.virtual_machine.name,
                                                lb_rule.name
                                                ))
        lb_rule.assign(self.apiclient, [self.virtual_machine])
        self.debug("SSH into Netscaler to check whether algorithm is configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
                                    )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                                        self.public_ip.ipaddress.ipaddress,
                                        lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                    result.count("Configured Method: ROUNDROBIN"),
                    1,
                    "'ROUNDROBIN' algorithm should be configured on NS"
                    )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))

        self.debug(
                "Updating LB rule: %s with new algorithm: %s" % (
                                                                lb_rule.name,
                                                                'source'))
        lb_rule.update(self.apiclient, algorithm='source')

        self.debug("SSH into Netscaler to check whether algorithm is configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
                                    )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                                        self.public_ip.ipaddress.ipaddress,
                                        lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                    result.count("Configured Method: SOURCEIPHASH"),
                    1,
                    "'SOURCEIPHASH' algorithm should be configured on NS"
                    )

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))
        return


class TestLbAlgoSbRr(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestLbAlgoSbRr,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')

        cls.account = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     admin=True,
                                     domainid=cls.domain.id
                                     )
        cls.network = Network.create(
                                    cls.api_client,
                                    cls.services["network"],
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    networkofferingid=cls.network_offering.id,
                                    zoneid=cls.zone.id
                                    )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )

        # Spawn an instance in that network
        cls.virtual_machine = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering.id,
                                  networkids=[str(cls.network.id)]
                                  )

        cls.public_ip = PublicIPAddress.create(
                                cls.api_client,
                                accountid=cls.account.name,
                                zoneid=cls.zone.id,
                                domainid=cls.account.domainid,
                                networkid=cls.network.id
                                )
        cls._cleanup = [
                        cls.account
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
            interval = list_configurations(
                                    cls.api_client,
                                    name='network.gc.interval'
                                    )
            wait = list_configurations(
                                    cls.api_client,
                                    name='network.gc.wait'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) + int(wait[0].value))
            cls.network_offering.delete(cls.api_client)
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
            self.debug("Cleaning up the resources")
            # Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advancedns"])
    def test_lb_source_to_round_robin(self):
        """Test edit LB rule from source to round robin algo
        """

        # Validate the following
        # 1. Deploy the first VM using a network from the above created
        #    Network offering.
        # 2. Deploy another VM.
        # 3. Acquire Ip address and create an Lb rule for couple of Vms using
        #    "source" algorithm. Make sure this algorithm is respected.
        # 4. Edit this existing LB rule with "source" algorithm to
        #    "roundrobin" After the update, Verify that "round robin"
        #    algorithm is applied when using this load balancing rule.

        self.debug(
            "Creating LB rule for IP address: %s with source algo" %
                                        self.public_ip.ipaddress.ipaddress)

        self.services["lbrule"]["alg"] = 'source'
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=self.public_ip.ipaddress.id,
                                    accountid=self.account.name,
                                    networkid=self.network.id
                                )
        self.cleanup.append(lb_rule)

        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=lb_rule.id,
                                         listall=True
                                         )
        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List LB rules should return a newly created LB rule"
                         )

        self.debug("Adding %s to the LB rule %s" % (
                                                self.virtual_machine.name,
                                                lb_rule.name
                                                ))
        lb_rule.assign(self.apiclient, [self.virtual_machine])

        self.debug("SSH into Netscaler to check whether algorithm is configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
                                    )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                                        self.public_ip.ipaddress.ipaddress,
                                        lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                    result.count("Configured Method: SOURCEIPHASH"),
                    1,
                    "'SOURCEIPHASH' algorithm should be configured on NS"
                    )

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))

        self.debug(
                "Updating LB rule: %s with new algorithm: %s" % (
                                                                lb_rule.name,
                                                                'roundrobin'))
        lb_rule.update(self.apiclient, algorithm='roundrobin')

        self.debug("SSH into Netscaler to check whether algorithm is configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
                                    )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                                        self.public_ip.ipaddress.ipaddress,
                                        lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                    result.count("Configured Method: ROUNDROBIN"),
                    1,
                    "'ROUNDROBIN' algorithm should be configured on NS"
                    )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))
        return


class TestLbAlgoSbLc(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestLbAlgoSbLc,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')

        cls.account = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     admin=True,
                                     domainid=cls.domain.id
                                     )
        cls.network = Network.create(
                                    cls.api_client,
                                    cls.services["network"],
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    networkofferingid=cls.network_offering.id,
                                    zoneid=cls.zone.id
                                    )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )

        # Spawn an instance in that network
        cls.virtual_machine = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering.id,
                                  networkids=[str(cls.network.id)]
                                  )

        cls.public_ip = PublicIPAddress.create(
                                cls.api_client,
                                accountid=cls.account.name,
                                zoneid=cls.zone.id,
                                domainid=cls.account.domainid,
                                networkid=cls.network.id
                                )
        cls._cleanup = [
                        cls.service_offering,
                        cls.account
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
            interval = list_configurations(
                                    cls.api_client,
                                    name='network.gc.interval'
                                    )
            wait = list_configurations(
                                    cls.api_client,
                                    name='network.gc.wait'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) + int(wait[0].value))
            cls.network_offering.delete(cls.api_client)
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
            self.debug("Cleaning up the resources")
            # Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advancedns"])
    def test_lb_source_to_least_conn(self):
        """Test edit LB rule from source to least conn algo
        """

        # Validate the following
        # 1. Deploy the first VM using a network from the above created
        #    Network offering.
        # 2. Deploy another VM.
        # 3. Acquire Ip address and create an Lb rule for couple of Vms using
        #    "source" algorithm. Make sure this algorithm is respected.
        # 4. Edit this existing LB rule with "source" algorithm to
        #    "leastconn" After the update, Verify that "leastconn"
        #    algorithm is applied when using this load balancing rule.

        self.debug(
            "Creating LB rule for IP address: %s with source algo" %
                                        self.public_ip.ipaddress.ipaddress)

        self.services["lbrule"]["alg"] = 'source'
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=self.public_ip.ipaddress.id,
                                    accountid=self.account.name,
                                    networkid=self.network.id
                                )
        self.cleanup.append(lb_rule)

        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=lb_rule.id,
                                         listall=True
                                         )
        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List LB rules should return a newly created LB rule"
                         )

        self.debug("Adding %s to the LB rule %s" % (
                                                self.virtual_machine.name,
                                                lb_rule.name
                                                ))
        lb_rule.assign(self.apiclient, [self.virtual_machine])

        self.debug("SSH into Netscaler to check whether algorithm is configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
                                    )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                                        self.public_ip.ipaddress.ipaddress,
                                        lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                    result.count("Configured Method: SOURCEIPHASH"),
                    1,
                    "'SOURCEIPHASH' algorithm should be configured on NS"
                    )

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))
        self.debug(
                "Updating LB rule: %s with new algorithm: %s" % (
                                                                lb_rule.name,
                                                                'leastconn'))
        lb_rule.update(self.apiclient, algorithm='leastconn')

        self.debug("SSH into Netscaler to check whether algorithm is configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
                                    )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                                        self.public_ip.ipaddress.ipaddress,
                                        lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                    result.count("Configured Method: LEASTCONNECTION"),
                    1,
                    "'LEASTCONNECTION' algorithm should be configured on NS"
                    )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))
        return


class TestLbAlgoLcSb(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestLbAlgoLcSb,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')

        cls.account = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     admin=True,
                                     domainid=cls.domain.id
                                     )
        cls.network = Network.create(
                                    cls.api_client,
                                    cls.services["network"],
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    networkofferingid=cls.network_offering.id,
                                    zoneid=cls.zone.id
                                    )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )

        # Spawn an instance in that network
        cls.virtual_machine = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering.id,
                                  networkids=[str(cls.network.id)]
                                  )
        cls.public_ip = PublicIPAddress.create(
                                cls.api_client,
                                accountid=cls.account.name,
                                zoneid=cls.zone.id,
                                domainid=cls.account.domainid,
                                networkid=cls.network.id
                                )
        cls._cleanup = [
                        cls.account,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
            interval = list_configurations(
                                    cls.api_client,
                                    name='network.gc.interval'
                                    )
            wait = list_configurations(
                                    cls.api_client,
                                    name='network.gc.wait'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) + int(wait[0].value))
            cls.network_offering.delete(cls.api_client)
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
            self.debug("Cleaning up the resources")
            # Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advancedns"])
    def test_lb_leastconn_to_source(self):
        """Test edit LB rule from round robin to source algo
        """

        # Validate the following
        # 1. Deploy the first VM using a network from the above created
        #    Network offering.
        # 2. Deploy another VM.
        # 3. Acquire Ip address and create an Lb rule for couple of Vms using
        #    "leastconn" algorithm. Make sure this algorithm is respected.
        # 4. Edit this existing LB rule with "leastconn" algorithm to
        #    "Source" After the update, Verify that "Source"
        #    algorithm is applied when using this load balancing rule.

        self.debug(
            "Creating LB rule for IP address: %s with leastconn algo" %
                                        self.public_ip.ipaddress.ipaddress)

        self.services["lbrule"]["alg"] = 'leastconn'
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=self.public_ip.ipaddress.id,
                                    accountid=self.account.name,
                                    networkid=self.network.id
                                )
        self.cleanup.append(lb_rule)

        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=lb_rule.id,
                                         listall=True
                                         )
        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List LB rules should return a newly created LB rule"
                         )
        self.debug("Adding %s to the LB rule %s" % (
                                                self.virtual_machine.name,
                                                lb_rule.name
                                                ))
        lb_rule.assign(self.apiclient, [self.virtual_machine])

        self.debug("SSH into Netscaler to check whether algorithm is configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
                                    )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                                        self.public_ip.ipaddress.ipaddress,
                                        lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                    result.count("Configured Method: LEASTCONNECTION"),
                    1,
                    "'LEASTCONNECTION' algorithm should be configured on NS"
                    )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))

        self.debug(
                "Updating LB rule: %s with new algorithm: %s" % (
                                                                lb_rule.name,
                                                                'source'))
        lb_rule.update(self.apiclient, algorithm='source')

        self.debug("SSH into Netscaler to check whether algorithm is configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
                                    )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                                        self.public_ip.ipaddress.ipaddress,
                                        lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                    result.count("Configured Method: SOURCEIPHASH"),
                    1,
                    "'SOURCEIPHASH' algorithm should be configured on NS"
                    )

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))
        return
