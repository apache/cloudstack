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

""" P1 tests for network offering
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
    """Test network offering Services
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
                                    "cpuspeed": 100,    # in MHz
                                    "memory": 64,       # In MBs
                                    },
                         "network_offering": {
                                    "name": 'Network offering-VR services',
                                    "displaytext": 'Network offering-VR services',
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
                                            "Lb": 'VirtualRouter',
                                            "UserData": 'VirtualRouter',
                                            "StaticNat": 'VirtualRouter',
                                        },
                                    },
                         "network_offering_netscaler": {
                                    "name": 'Network offering-netscaler',
                                    "displaytext": 'Network offering-netscaler',
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
                                  "name": "Test Network",
                                  "displaytext": "Test Network",
                                  "vlan": 2370,
                                },
                         "lbrule": {
                                    "name": "SSH",
                                    "alg": "leastconn",
                                    # Algorithm used for load balancing
                                    "privateport": 22,
                                    "publicport": 2222,
                                    "openfirewall": False,
                                },
                         "lbrule_port_2221": {
                                    "name": "SSH",
                                    "alg": "leastconn",
                                    # Algorithm used for load balancing
                                    "privateport": 22,
                                    "publicport": 2221,
                                    "openfirewall": False,
                                },
                         "natrule": {
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": "TCP"
                                },
                         "natrule_port_66": {
                                    "privateport": 22,
                                    "publicport": 66,
                                    "protocol": "TCP"
                                },
                         "fw_rule": {
                                    "startport": 1,
                                    "endport": 6000,
                                    "cidr": '55.55.0.0/11',
                                    # Any network (For creating FW rule)
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
                         "sleep": 60,
                         "timeout": 10,
                         "mode": 'advanced'
                    }


class TestNOVirtualRouter(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestNOVirtualRouter,
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
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls._cleanup = [
                        cls.service_offering,
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
        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.account.delete(self.apiclient)
            interval = list_configurations(
                                    self.apiclient,
                                    name='account.cleanup.interval'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) * 2)
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advanced"])
    def test_01_network_off_without_conserve_mode(self):
        """Test Network offering with Conserve mode off and VR - All services
        """


        # Validate the following
        # 1. Create a Network from the above network offering and deploy a VM.
        # 2. On source NAT ipaddress, we should NOT be allowed to add a
        #    LB rules
        # 3. On source NAT ipaddress, we should be NOT be allowed to add
        #    PF rule
        # 4. On an ipaddress that has PF rules, we should NOT be allowed to
        #    add a LB rules.
        # 5. On an ipaddress that has Lb rules, we should NOT allow PF rules
        #    to be programmed.
        # 6. We should be allowed to program multiple PF rules on the same Ip
        #    address on different public ports.
        # 7. We should be allowed to program multiple LB rules on the same Ip
        #    address for different public port ranges.
        # 8. On source NAT ipaddress, we should be allowed to Enable VPN.
        # 9. On SOurce NAT ipaddress, we will be allowed to add firewall rule

        # Create a network offering with all virtual router services enabled
        self.debug(
            "Creating n/w offering with all services in VR & conserve mode:off"
            )
        self.network_offering = NetworkOffering.create(
                                            self.api_client,
                                            self.services["network_offering"],
                                            conservemode=False
                                            )
        self.cleanup.append(self.network_offering)

        self.debug("Created n/w offering with ID: %s" %
                                                    self.network_offering.id)
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        self.network = Network.create(
                                    self.apiclient,
                                    self.services["network"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account.account.name)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.account.name,
                                  domainid=self.account.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network.id)

        src_nat_list = PublicIPAddress.list(
                                        self.apiclient,
                                        associatednetworkid=self.network.id,
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid,
                                        listall=True,
                                        issourcenat=True,
                                        )
        self.assertEqual(
                         isinstance(src_nat_list, list),
                         True,
                         "List Public IP should return a valid source NAT"
                         )
        self.assertNotEqual(
                    len(src_nat_list),
                    0,
                    "Length of response from listPublicIp should not be 0"
                    )

        src_nat = src_nat_list[0]

        self.debug("Trying to create LB rule on source NAT IP: %s" %
                                                        src_nat.ipaddress)
        # Create Load Balancer rule with source NAT
        with self.assertRaises(Exception):
                LoadBalancerRule.create(
                                          self.apiclient,
                                          self.services["lbrule"],
                                          ipaddressid=src_nat.id,
                                          accountid=self.account.account.name
                                       )
        self.debug(
            "Trying to create a port forwarding rule in source NAT: %s" %
                                                            src_nat.ipaddress)
        #Create NAT rule
        with self.assertRaises(Exception):
            NATRule.create(
                           self.apiclient,
                           virtual_machine,
                           self.services["natrule"],
                           ipaddressid=src_nat.id
                           )

        self.debug("Associating public IP for network: %s" % self.network.id)
        ip_with_nat_rule = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.account.domainid,
                                    networkid=self.network.id
                                    )

        self.debug("Associated %s with network %s" % (
                                        ip_with_nat_rule.ipaddress.ipaddress,
                                        self.network.id
                                        ))
        self.debug("Creating PF rule for IP address: %s" %
                                        ip_with_nat_rule.ipaddress.ipaddress)
        NATRule.create(
                         self.apiclient,
                         virtual_machine,
                         self.services["natrule"],
                         ipaddressid=ip_with_nat_rule.ipaddress.id
                      )

        self.debug("Trying to create LB rule on IP with NAT: %s" %
                                    ip_with_nat_rule.ipaddress.ipaddress)

        # Create Load Balancer rule on IP already having NAT rule
        with self.assertRaises(Exception):
                LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=ip_with_nat_rule.ipaddress.id,
                                    accountid=self.account.account.name
                                    )
        self.debug("Creating PF rule with public port: 66")

        nat_rule = NATRule.create(
                         self.apiclient,
                         virtual_machine,
                         self.services["natrule_port_66"],
                         ipaddressid=ip_with_nat_rule.ipaddress.id
                      )

        # Check if NAT rule created successfully
        nat_rules = NATRule.list(
                                 self.apiclient,
                                 id=nat_rule.id
                                 )

        self.assertEqual(
                         isinstance(nat_rules, list),
                         True,
                         "List NAT rules should return valid list"
                         )

        self.debug("Associating public IP for network: %s" % self.network.id)
        ip_with_lb_rule = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.account.domainid,
                                networkid=self.network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        ip_with_lb_rule.ipaddress.ipaddress,
                                        self.network.id
                                        ))
        self.debug("Creating LB rule for IP address: %s" %
                                        ip_with_lb_rule.ipaddress.ipaddress)

        LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=ip_with_lb_rule.ipaddress.id,
                                    accountid=self.account.account.name
                                )

        self.debug("Trying to create PF rule on IP with LB rule: %s" %
                ip_with_nat_rule.ipaddress.ipaddress)

        with self.assertRaises(Exception):
            NATRule.create(
                         self.apiclient,
                         virtual_machine,
                         self.services["natrule"],
                         ipaddressid=ip_with_lb_rule.ipaddress.id
                      )

        self.debug("Creating LB rule with public port: 2221")
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule_port_2221"],
                                    ipaddressid=ip_with_lb_rule.ipaddress.id,
                                    accountid=self.account.account.name
                                )

        # Check if NAT rule created successfully
        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=lb_rule.id
                                         )

        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List LB rules should return valid list"
                         )

        self.debug("Creating firewall rule on source NAT: %s" %
                                                        src_nat.ipaddress)
        #Create Firewall rule on source NAT
        fw_rule = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=src_nat.id,
                            protocol='TCP',
                            cidrlist=[self.services["fw_rule"]["cidr"]],
                            startport=self.services["fw_rule"]["startport"],
                            endport=self.services["fw_rule"]["endport"]
                            )
        self.debug("Created firewall rule: %s" % fw_rule.id)

        fw_rules = FireWallRule.list(
                                     self.apiclient,
                                     id=fw_rule.id
                                    )
        self.assertEqual(
                         isinstance(fw_rules, list),
                         True,
                         "List fw rules should return a valid firewall rules"
                         )

        self.assertNotEqual(
                            len(fw_rules),
                            0,
                            "Length of fw rules response should not be zero"
                            )
        return

    @attr(tags = ["advanced"])
    def test_02_network_off_with_conserve_mode(self):
        """Test Network offering with Conserve mode ON and VR - All services
        """


        # Validate the following
        # 1. Create a Network from the above network offering and deploy a VM.
        # 2. On source NAT ipaddress, we should be allowed to add a LB rules
        # 3. On source NAT ipaddress, we should be allowed to add a PF rules
        # 4. On source NAT ipaddress, we should be allowed to add a Firewall
        #    rules
        # 5. On an ipaddress that has Lb rules, we should be allowed to
        #    program PF rules.
        # 6. We should be allowed to program multiple PF rules on the same Ip
        #    address on different public ports.
        # 7. We should be allowed to program multiple LB rules on the same Ip
        #    address for different public port ranges.
        # 8. On source NAT ipaddress, we should be allowed to Enable VPN
        #    access.

        # Create a network offering with all virtual router services enabled
        self.debug(
            "Creating n/w offering with all services in VR & conserve mode:off"
            )
        self.network_offering = NetworkOffering.create(
                                            self.api_client,
                                            self.services["network_offering"],
                                            conservemode=True
                                            )
        self.cleanup.append(self.network_offering)

        self.debug("Created n/w offering with ID: %s" %
                                                    self.network_offering.id)
    # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        self.network = Network.create(
                                    self.apiclient,
                                    self.services["network"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account.account.name)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.account.name,
                                  domainid=self.account.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network.id)

        src_nat_list = PublicIPAddress.list(
                                        self.apiclient,
                                        associatednetworkid=self.network.id,
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid,
                                        listall=True,
                                        issourcenat=True,
                                        )
        self.assertEqual(
                         isinstance(src_nat_list, list),
                         True,
                         "List Public IP should return a valid source NAT"
                         )
        self.assertNotEqual(
                    len(src_nat_list),
                    0,
                    "Length of response from listPublicIp should not be 0"
                    )

        src_nat = src_nat_list[0]

        self.debug("Trying to create LB rule on source NAT IP: %s" %
                                                        src_nat.ipaddress)
        # Create Load Balancer rule with source NAT
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=src_nat.id,
                                    accountid=self.account.account.name
                                )
        self.debug("Created LB rule on source NAT: %s" % src_nat.ipaddress)

        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=lb_rule.id
                                         )
        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List lb rules should return a valid lb rules"
                         )
        self.assertNotEqual(
                    len(lb_rules),
                    0,
                    "Length of response from listLbRules should not be 0"
                    )

        self.debug(
            "Trying to create a port forwarding rule in source NAT: %s" %
                                                            src_nat.ipaddress)
        #Create NAT rule
        nat_rule = NATRule.create(
                           self.apiclient,
                           virtual_machine,
                           self.services["natrule"],
                           ipaddressid=src_nat.id
                           )
        self.debug("Created PF rule on source NAT: %s" % src_nat.ipaddress)

        nat_rules = NATRule.list(
                                         self.apiclient,
                                         id=nat_rule.id
                                         )
        self.assertEqual(
                         isinstance(nat_rules, list),
                         True,
                         "List NAT should return a valid port forwarding rules"
                         )
        self.assertNotEqual(
                    len(nat_rules),
                    0,
                    "Length of response from listLbRules should not be 0"
                    )
        self.debug("Creating firewall rule on source NAT: %s" %
                                                        src_nat.ipaddress)
        #Create Firewall rule on source NAT
        fw_rule = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=src_nat.id,
                            protocol='TCP',
                            cidrlist=[self.services["fw_rule"]["cidr"]],
                            startport=self.services["fw_rule"]["startport"],
                            endport=self.services["fw_rule"]["endport"]
                            )
        self.debug("Created firewall rule: %s" % fw_rule.id)

        fw_rules = FireWallRule.list(
                                     self.apiclient,
                                     id=fw_rule.id
                                    )
        self.assertEqual(
                         isinstance(fw_rules, list),
                         True,
                         "List fw rules should return a valid firewall rules"
                         )

        self.assertNotEqual(
                            len(fw_rules),
                            0,
                            "Length of fw rules response should not be zero"
                            )

        self.debug("Associating public IP for network: %s" % self.network.id)
        public_ip = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.account.domainid,
                                    networkid=self.network.id
                                    )

        self.debug("Associated %s with network %s" % (
                                        public_ip.ipaddress.ipaddress,
                                        self.network.id
                                        ))
        self.debug("Creating PF rule for IP address: %s" %
                                        public_ip.ipaddress.ipaddress)
        NATRule.create(
                       self.apiclient,
                       virtual_machine,
                       self.services["natrule"],
                       ipaddressid=public_ip.ipaddress.id
                      )

        self.debug("Trying to create LB rule on IP with NAT: %s" %
                                    public_ip.ipaddress.ipaddress)

        # Create Load Balancer rule on IP already having NAT rule
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=public_ip.ipaddress.id,
                                    accountid=self.account.account.name
                                    )
        self.debug("Creating PF rule with public port: 66")

        nat_rule = NATRule.create(
                         self.apiclient,
                         virtual_machine,
                         self.services["natrule_port_66"],
                         ipaddressid=public_ip.ipaddress.id
                      )

        # Check if NAT rule created successfully
        nat_rules = NATRule.list(
                                 self.apiclient,
                                 id=nat_rule.id
                                 )

        self.assertEqual(
                         isinstance(nat_rules, list),
                         True,
                         "List NAT rules should return valid list"
                         )

        self.debug("Creating LB rule with public port: 2221")
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule_port_2221"],
                                    ipaddressid=public_ip.ipaddress.id,
                                    accountid=self.account.account.name
                                )

        # Check if NAT rule created successfully
        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=lb_rule.id
                                         )

        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List LB rules should return valid list"
                         )

        # User should be able to enable VPN on source NAT
        self.debug("Created VPN with source NAT IP: %s" % src_nat.ipaddress)
        # Assign VPN to source NAT
        vpn = Vpn.create(
                        self.apiclient,
                        src_nat.id,
                        account=self.account.account.name,
                        domainid=self.account.account.domainid
                        )

        vpns = Vpn.list(
                        self.apiclient,
                        publicipid=src_nat.id,
            listall=True,
                        )

        self.assertEqual(
                         isinstance(vpns, list),
                         True,
                         "List VPNs should return a valid VPN list"
                         )

        self.assertNotEqual(
                            len(vpns),
                            0,
                            "Length of list VPN response should not be zero"
                            )
        return


class TestNOWithNetscaler(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestNOWithNetscaler,
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
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls._cleanup = [
                        cls.service_offering,
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
        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.account.delete(self.apiclient)
            interval = list_configurations(
                                    self.apiclient,
                                    name='account.cleanup.interval'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) * 2)
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advancedns"])
    def test_01_network_off_without_conserve_mode(self):
        """Test Nw off with Conserve mode off, VR-All services, LB-netscaler
        """


        # Validate the following
        # 1. Create a Network from the above network offering and deploy a VM.
        # 2. On source NAT ipaddress, we should NOT be allowed to add LB rule
        # 3. On source NAT ipaddress, we should NOT be allowed to add PF rule
        # 4. On an ipaddress that has PF rules, we should NOT be allowed to
        #    add a LB rules.
        # 5. On an ipaddress that has Lb rules , we should NOT allow firewall
        #    rules to be programmed.
        # 6. On an ipaddress that has Lb rules , we should NOT allow PF rules
        #    to be programmed.
        # 7. We should be allowed to program multiple PF rules on the same Ip
        #    address on different public ports.
        # 8. We should be allowed to program multiple LB rules on the same Ip
        #    address for different public port ranges.
        # 9. On source NAT ipaddress, we should NOT be allowed to Enable VPN.

        # Create a network offering with all virtual router services enabled
        self.debug(
            "Creating n/w offering with all services in VR & conserve mode:ON"
            )
        self.network_offering = NetworkOffering.create(
                                            self.api_client,
                                            self.services["network_offering_netscaler"],
                                            conservemode=False
                                            )
        self.cleanup.append(self.network_offering)

        self.debug("Created n/w offering with ID: %s" %
                                                    self.network_offering.id)
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        self.network = Network.create(
                                    self.apiclient,
                                    self.services["network"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account.account.name)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.account.name,
                                  domainid=self.account.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network.id)

        src_nat_list = PublicIPAddress.list(
                                        self.apiclient,
                                        associatednetworkid=self.network.id,
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid,
                                        listall=True,
                                        issourcenat=True,
                                        )
        self.assertEqual(
                         isinstance(src_nat_list, list),
                         True,
                         "List Public IP should return a valid source NAT"
                         )
        self.assertNotEqual(
                    len(src_nat_list),
                    0,
                    "Length of response from listPublicIp should not be 0"
                    )

        src_nat = src_nat_list[0]

        self.debug("Trying to create LB rule on source NAT IP: %s" %
                                                        src_nat.ipaddress)
        # Create Load Balancer rule with source NAT
        with self.assertRaises(Exception):
            LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=src_nat.id,
                                    accountid=self.account.account.name
                                )

        self.debug(
            "Trying to create a port forwarding rule in source NAT: %s" %
                                                            src_nat.ipaddress)
        #Create NAT rule
        with self.assertRaises(Exception):
            NATRule.create(
                           self.apiclient,
                           virtual_machine,
                           self.services["natrule"],
                           ipaddressid=src_nat.id
                           )
        self.debug("Creating firewall rule on source NAT: %s" %
                                                        src_nat.ipaddress)
        #Create Firewall rule on source NAT
        fw_rule = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=src_nat.id,
                            protocol='TCP',
                            cidrlist=[self.services["fw_rule"]["cidr"]],
                            startport=self.services["fw_rule"]["startport"],
                            endport=self.services["fw_rule"]["endport"]
                            )

        self.debug("Created firewall rule: %s" % fw_rule.id)

        fw_rules = FireWallRule.list(
                                     self.apiclient,
                                     id=fw_rule.id
                                    )
        self.assertEqual(
                         isinstance(fw_rules, list),
                         True,
                         "List fw rules should return a valid firewall rules"
                         )

        self.assertNotEqual(
                            len(fw_rules),
                            0,
                            "Length of fw rules response should not be zero"
                            )

        self.debug("Associating public IP for network: %s" % self.network.id)
        ip_with_nat_rule = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.account.domainid,
                                    networkid=self.network.id
                                    )

        self.debug("Associated %s with network %s" % (
                                        ip_with_nat_rule.ipaddress.ipaddress,
                                        self.network.id
                                        ))
        self.debug("Creating PF rule for IP address: %s" %
                                        ip_with_nat_rule.ipaddress.ipaddress)
        NATRule.create(
                         self.apiclient,
                         virtual_machine,
                         self.services["natrule"],
                         ipaddressid=ip_with_nat_rule.ipaddress.id
                      )

        self.debug("Trying to create LB rule on IP with NAT: %s" %
                                    ip_with_nat_rule.ipaddress.ipaddress)

        # Create Load Balancer rule on IP already having NAT rule
        with self.assertRaises(Exception):
                LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=ip_with_nat_rule.ipaddress.id,
                                    accountid=self.account.account.name
                                    )
        self.debug("Creating PF rule with public port: 66")

        nat_rule = NATRule.create(
                         self.apiclient,
                         virtual_machine,
                         self.services["natrule_port_66"],
                         ipaddressid=ip_with_nat_rule.ipaddress.id
                      )

        # Check if NAT rule created successfully
        nat_rules = NATRule.list(
                                 self.apiclient,
                                 id=nat_rule.id
                                 )

        self.assertEqual(
                         isinstance(nat_rules, list),
                         True,
                         "List NAT rules should return valid list"
                         )

        self.debug("Associating public IP for network: %s" % self.network.id)
        ip_with_lb_rule = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.account.domainid,
                                networkid=self.network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        ip_with_lb_rule.ipaddress.ipaddress,
                                        self.network.id
                                        ))
        self.debug("Creating LB rule for IP address: %s" %
                                        ip_with_lb_rule.ipaddress.ipaddress)

        LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=ip_with_lb_rule.ipaddress.id,
                                    accountid=self.account.account.name,
                                    networkid=self.network.id
                                )

        self.debug("Trying to create PF rule on IP with LB rule: %s" %
                                        ip_with_nat_rule.ipaddress.ipaddress)

        with self.assertRaises(Exception):
            NATRule.create(
                         self.apiclient,
                         virtual_machine,
                         self.services["natrule"],
                         ipaddressid=ip_with_lb_rule.ipaddress.id
                      )

        self.debug("Trying to create FW rule on IP with LB rule")
        with self.assertRaises(Exception):
            FireWallRule.create(
                            self.apiclient,
                            ipaddressid=src_nat.id,
                            protocol='TCP',
                            cidrlist=[self.services["fw_rule"]["cidr"]],
                            startport=self.services["fw_rule"]["startport"],
                            endport=self.services["fw_rule"]["endport"]
                            )

        self.debug("Creating LB rule with public port: 2221")
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule_port_2221"],
                                    ipaddressid=ip_with_lb_rule.ipaddress.id,
                                    accountid=self.account.account.name,
                                    networkid=self.network.id
                                )

        # Check if NAT rule created successfully
        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=lb_rule.id
                                         )

        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List LB rules should return valid list"
                         )

        # User should be able to enable VPN on source NAT
        self.debug("Enabling VPN on source NAT IP: %s" % src_nat.ipaddress)
        # Assign VPN to source NAT
        with self.assertRaises(Exception):
            Vpn.create(
                        self.apiclient,
                        src_nat.id,
                        account=self.account.account.name,
                        domainid=self.account.account.domainid
                        )
        return

    @attr(tags = ["advancedns"])
    def test_02_network_off_with_conserve_mode_netscaler(self):
        """Test NW off with Conserve mode ON, LB-Netscaler and VR-All services
        """


        # Validate the following
        # 1. Create a Network from the above network offering and deploy a VM.
        # 2. On source NAT ipaddress, we should NOT be allowed to add LB rule
        # 3. On source NAT ipaddress, we should be allowed to add PF rule and
        #    Fierwall rules.
        # 4. On an ipaddress that has PF rules, we should NOT be allowed to
        #    add a LB rules.
        # 5. On an ipaddress that has Lb rules , we should NOT allow firewall
        #    rules to be programmed.
        # 6. On an ipaddress that has Lb rules , we should NOT allow PF rules
        #    to be programmed.
        # 7. We should be allowed to program multiple PF rules on the same Ip
        #    address on different public ports.
        # 8. We should be allowed to program multiple LB rules on the same Ip
        #    address for different public port ranges.
        # 9. On source NAT ipaddress, we should be allowed to Enable VPN.

        # Create a network offering with all virtual router services enabled
        self.debug(
            "Creating n/w offering with all services in VR & conserve mode:ON"
            )
        self.network_offering = NetworkOffering.create(
                                            self.api_client,
                                            self.services["network_offering_netscaler"],
                                            conservemode=True
                                            )
        self.cleanup.append(self.network_offering)

        self.debug("Created n/w offering with ID: %s" %
                                                    self.network_offering.id)
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        self.network = Network.create(
                                    self.apiclient,
                                    self.services["network"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account.account.name)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.account.name,
                                  domainid=self.account.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network.id)

        src_nat_list = PublicIPAddress.list(
                                        self.apiclient,
                                        associatednetworkid=self.network.id,
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid,
                                        listall=True,
                                        issourcenat=True,
                                        )
        self.assertEqual(
                         isinstance(src_nat_list, list),
                         True,
                         "List Public IP should return a valid source NAT"
                         )
        self.assertNotEqual(
                    len(src_nat_list),
                    0,
                    "Length of response from listPublicIp should not be 0"
                    )

        src_nat = src_nat_list[0]

        self.debug("Trying to create LB rule on source NAT IP: %s" %
                                                        src_nat.ipaddress)
        # Create Load Balancer rule with source NAT
        with self.assertRaises(Exception):
            LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=src_nat.id,
                                    accountid=self.account.account.name
                                )

        self.debug(
            "Trying to create a port forwarding rule in source NAT: %s" %
                                                            src_nat.ipaddress)
        #Create NAT rule
        nat_rule = NATRule.create(
                           self.apiclient,
                           virtual_machine,
                           self.services["natrule"],
                           ipaddressid=src_nat.id
                           )
        self.debug("Created PF rule on source NAT: %s" % src_nat.ipaddress)

        nat_rules = NATRule.list(
                                    self.apiclient,
                                    id=nat_rule.id
                                )
        self.assertEqual(
                         isinstance(nat_rules, list),
                         True,
                         "List NAT should return a valid port forwarding rules"
                         )
        self.assertNotEqual(
                    len(nat_rules),
                    0,
                    "Length of response from listLbRules should not be 0"
                    )
        self.debug("Creating firewall rule on source NAT: %s" %
                                                        src_nat.ipaddress)
        #Create Firewall rule on source NAT
        fw_rule = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=src_nat.id,
                            protocol='TCP',
                            cidrlist=[self.services["fw_rule"]["cidr"]],
                            startport=self.services["fw_rule"]["startport"],
                            endport=self.services["fw_rule"]["endport"]
                            )
        self.debug("Created firewall rule: %s" % fw_rule.id)

        fw_rules = FireWallRule.list(
                                     self.apiclient,
                                     id=fw_rule.id
                                    )
        self.assertEqual(
                         isinstance(fw_rules, list),
                         True,
                         "List fw rules should return a valid firewall rules"
                         )

        self.assertNotEqual(
                            len(fw_rules),
                            0,
                            "Length of fw rules response should not be zero"
                            )
        self.debug("Associating public IP for network: %s" % self.network.id)
        ip_with_nat_rule = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.account.domainid,
                                    networkid=self.network.id
                                    )

        self.debug("Associated %s with network %s" % (
                                        ip_with_nat_rule.ipaddress.ipaddress,
                                        self.network.id
                                        ))
        self.debug("Creating PF rule for IP address: %s" %
                                        ip_with_nat_rule.ipaddress.ipaddress)
        NATRule.create(
                         self.apiclient,
                         virtual_machine,
                         self.services["natrule"],
                         ipaddressid=ip_with_nat_rule.ipaddress.id
                      )

        self.debug("Trying to create LB rule on IP with NAT: %s" %
                                    ip_with_nat_rule.ipaddress.ipaddress)

        # Create Load Balancer rule on IP already having NAT rule
        with self.assertRaises(Exception):
                LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=ip_with_nat_rule.ipaddress.id,
                                    accountid=self.account.account.name
                                    )
        self.debug("Creating PF rule with public port: 66")

        nat_rule = NATRule.create(
                         self.apiclient,
                         virtual_machine,
                         self.services["natrule_port_66"],
                         ipaddressid=ip_with_nat_rule.ipaddress.id
                      )

        # Check if NAT rule created successfully
        nat_rules = NATRule.list(
                                 self.apiclient,
                                 id=nat_rule.id
                                 )

        self.assertEqual(
                         isinstance(nat_rules, list),
                         True,
                         "List NAT rules should return valid list"
                         )

        self.debug("Associating public IP for network: %s" % self.network.id)
        ip_with_lb_rule = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.account.domainid,
                                networkid=self.network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        ip_with_lb_rule.ipaddress.ipaddress,
                                        self.network.id
                                        ))
        self.debug("Creating LB rule for IP address: %s" %
                                        ip_with_lb_rule.ipaddress.ipaddress)

        LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=ip_with_lb_rule.ipaddress.id,
                                    accountid=self.account.account.name,
                                    networkid=self.network.id
                                )

        self.debug("Trying to create PF rule on IP with LB rule: %s" %
                                        ip_with_nat_rule.ipaddress.ipaddress)

        with self.assertRaises(Exception):
            NATRule.create(
                         self.apiclient,
                         virtual_machine,
                         self.services["natrule"],
                         ipaddressid=ip_with_lb_rule.ipaddress.id
                      )

        self.debug("Trying to create FW rule on IP with LB rule")
        with self.assertRaises(Exception):
            FireWallRule.create(
                            self.apiclient,
                            ipaddressid=src_nat.id,
                            protocol='TCP',
                            cidrlist=[self.services["fw_rule"]["cidr"]],
                            startport=self.services["fw_rule"]["startport"],
                            endport=self.services["fw_rule"]["endport"]
                            )

        self.debug("Creating LB rule with public port: 2221")
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule_port_2221"],
                                    ipaddressid=ip_with_lb_rule.ipaddress.id,
                                    accountid=self.account.account.name,
                                    networkid=self.network.id
                                )

        # Check if NAT rule created successfully
        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=lb_rule.id
                                         )

        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List LB rules should return valid list"
                         )

        # User should be able to enable VPN on source NAT
        self.debug("Created VPN with source NAT IP: %s" % src_nat.ipaddress)
        # Assign VPN to source NAT
        vpn = Vpn.create(
                        self.apiclient,
                        src_nat.id,
                        account=self.account.account.name,
                        domainid=self.account.account.domainid
                        )

        vpns = Vpn.list(
                        self.apiclient,
                        publicipid=src_nat.id,
                        listall=True,
                        )

        self.assertEqual(
                         isinstance(vpns, list),
                         True,
                         "List VPNs should return a valid VPN list"
                         )

        self.assertNotEqual(
                            len(vpns),
                            0,
                            "Length of list VNP response should not be zero"
                            )
        return


class TestNetworkUpgrade(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestNetworkUpgrade,
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
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')

        cls._cleanup = [
                        cls.service_offering,
                        cls.network_offering
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
        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.account.delete(self.apiclient)
            interval = list_configurations(
                                    self.apiclient,
                                    name='account.cleanup.interval'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) * 2)
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(speed = "slow")
    @attr(tags = ["advancedns"])
    def test_01_nwupgrade_netscaler_conserve_on(self):
        """Test Nw upgrade to netscaler lb service and conserve mode ON
        """


        # Validate the following
        # 1. Upgrade a network with VR and conserve mode ON TO
        #    A network that has Lb provided by "Netscaler" and all other
        #    services provided by "VR" and Conserve mode ON
        # 2. Have PF and LB rules on the same ip address. Upgrade network
        #    should fail.
        # 3. Have SourceNat,PF and VPN on the same IP address. Upgrade of
        #    network should succeed.

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        self.network = Network.create(
                                    self.apiclient,
                                    self.services["network"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account.account.name)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.account.name,
                                  domainid=self.account.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network.id)

        src_nat_list = PublicIPAddress.list(
                                        self.apiclient,
                                        associatednetworkid=self.network.id,
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid,
                                        listall=True,
                                        issourcenat=True,
                                        )
        self.assertEqual(
                         isinstance(src_nat_list, list),
                         True,
                         "List Public IP should return a valid source NAT"
                         )
        self.assertNotEqual(
                    len(src_nat_list),
                    0,
                    "Length of response from listPublicIp should not be 0"
                    )

        src_nat = src_nat_list[0]
        self.debug("Trying to create LB rule on source NAT IP: %s" %
                                                        src_nat.ipaddress)
        # Create Load Balancer rule with source NAT
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=src_nat.id,
                                    accountid=self.account.account.name
                                )
        self.debug("Created LB rule on source NAT: %s" % src_nat.ipaddress)

        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=lb_rule.id
                                         )
        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List lb rules should return a valid lb rules"
                         )
        self.assertNotEqual(
                    len(lb_rules),
                    0,
                    "Length of response from listLbRules should not be 0"
                    )

        self.debug(
            "Trying to create a port forwarding rule in source NAT: %s" %
                                                            src_nat.ipaddress)
        #Create NAT rule
        nat_rule = NATRule.create(
                           self.apiclient,
                           virtual_machine,
                           self.services["natrule"],
                           ipaddressid=src_nat.id
                           )
        self.debug("Created PF rule on source NAT: %s" % src_nat.ipaddress)

        nat_rules = NATRule.list(
                                         self.apiclient,
                                         id=nat_rule.id
                                         )
        self.assertEqual(
                         isinstance(nat_rules, list),
                         True,
                         "List NAT should return a valid port forwarding rules"
                         )
        self.assertNotEqual(
                    len(nat_rules),
                    0,
                    "Length of response from listLbRules should not be 0"
                    )
        # Create a network offering with all virtual router services enabled
        self.debug(
            "Creating n/w offering with all services in VR & conserve mode:ON LB- Netscaler"
            )
        ns_lb_offering = NetworkOffering.create(
                                            self.api_client,
                                            self.services["network_offering_netscaler"],
                                            conservemode=True
                                            )
        self.cleanup.append(ns_lb_offering)
        ns_lb_offering.update(self.apiclient, state='Enabled')

        #Stop all the VMs associated with network to update cidr
        self.debug("Stopping the VM: %s" % virtual_machine.name)
        virtual_machine.stop(self.apiclient)

        self.debug("Updating network offering for network: %s" %
                                                            self.network.id)
        with self.assertRaises(Exception):
            self.network.update(
                            self.apiclient,
                            networkofferingid=ns_lb_offering.id,
                            changecidr=True
                            )

        self.debug("Network upgrade failed!")

        self.debug("Deleting LB Rule: %s" % lb_rule.id)
        lb_rule.delete(self.apiclient)
        self.debug("LB rule deleted")

        # Assign VPN to source NAT
        self.debug("Enabling VPN on source NAT")
        vpn = Vpn.create(
                        self.apiclient,
                        src_nat.id,
                        account=self.account.account.name,
                        domainid=self.account.account.domainid
                        )

        vpns = Vpn.list(
                        self.apiclient,
                        publicipid=src_nat.id,
                        listall=True,
                        )

        self.assertEqual(
                         isinstance(vpns, list),
                         True,
                         "List VPNs should return a valid VPN list"
                         )

        self.assertNotEqual(
                            len(vpns),
                            0,
                            "Length of list VPN response should not be zero"
                            )
        self.debug("Upgrading the network: %s" % self.network.id)
        self.network.update(
                            self.apiclient,
                            networkofferingid=ns_lb_offering.id,
                            changecidr=True
                            )
        networks = Network.list(
                                self.apiclient,
                                id=self.network.id,
                                listall=True
                                )
        self.assertEqual(
                isinstance(networks, list),
                True,
                "List Networks should return a valid list for given network ID"
                )
        self.assertNotEqual(
                            len(networks),
                            0,
                            "Length of list networks should not be 0"
                            )
        network = networks[0]
        self.assertEqual(
                        network.networkofferingid,
                        ns_lb_offering.id,
                        "Network offering ID should match with new offering ID"
                        )
        return

    @attr(speed = "slow")
    @attr(tags = ["advancedns"])
    def test_02_nwupgrade_netscaler_conserve_off(self):
        """Test Nw upgrade to netscaler lb service and conserve mode OFF
        """


        # Validate the following
        # 1. Upgrade a network with VR and conserve mode ON TO
        #    A network that has Lb provided by "Netscaler" and all other
        #    services provided by "VR" and Conserve mode OFF
        # 2. Have PF and LB rules on the same ip address. Upgrade network
        #    should fail.
        # 3. Have SourceNat,PF and VPN on the same IP address. Upgrade of
        #    network should fail.

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        self.network = Network.create(
                                    self.apiclient,
                                    self.services["network"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account.account.name)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.account.name,
                                  domainid=self.account.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network.id)

        src_nat_list = PublicIPAddress.list(
                                        self.apiclient,
                                        associatednetworkid=self.network.id,
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid,
                                        listall=True,
                                        issourcenat=True,
                                        )
        self.assertEqual(
                         isinstance(src_nat_list, list),
                         True,
                         "List Public IP should return a valid source NAT"
                         )
        self.assertNotEqual(
                    len(src_nat_list),
                    0,
                    "Length of response from listPublicIp should not be 0"
                    )

        src_nat = src_nat_list[0]
        self.debug("Trying to create LB rule on source NAT IP: %s" %
                                                        src_nat.ipaddress)
        # Create Load Balancer rule with source NAT
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=src_nat.id,
                                    accountid=self.account.account.name
                                )
        self.debug("Created LB rule on source NAT: %s" % src_nat.ipaddress)

        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=lb_rule.id
                                         )
        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List lb rules should return a valid lb rules"
                         )
        self.assertNotEqual(
                    len(lb_rules),
                    0,
                    "Length of response from listLbRules should not be 0"
                    )

        self.debug(
            "Trying to create a port forwarding rule in source NAT: %s" %
                                                            src_nat.ipaddress)
        #Create NAT rule
        nat_rule = NATRule.create(
                           self.apiclient,
                           virtual_machine,
                           self.services["natrule"],
                           ipaddressid=src_nat.id
                           )
        self.debug("Created PF rule on source NAT: %s" % src_nat.ipaddress)

        nat_rules = NATRule.list(
                                         self.apiclient,
                                         id=nat_rule.id
                                         )
        self.assertEqual(
                         isinstance(nat_rules, list),
                         True,
                         "List NAT should return a valid port forwarding rules"
                         )
        self.assertNotEqual(
                    len(nat_rules),
                    0,
                    "Length of response from listLbRules should not be 0"
                    )
        # Create a network offering with all virtual router services enabled
        self.debug(
            "Creating n/w offering with all services in VR & conserve mode:ON LB- Netscaler"
            )
        ns_lb_offering = NetworkOffering.create(
                                            self.api_client,
                                            self.services["network_offering_netscaler"],
                                            conservemode=False
                                            )
        self.cleanup.append(ns_lb_offering)
        ns_lb_offering.update(self.apiclient, state='Enabled')

        #Stop all the VMs associated with network to update cidr
        self.debug("Stopping the VM: %s" % virtual_machine.name)
        virtual_machine.stop(self.apiclient)

        self.debug("Updating network offering for network: %s" %
                                                            self.network.id)
        with self.assertRaises(Exception):
            self.network.update(
                            self.apiclient,
                            networkofferingid=ns_lb_offering.id,
                            changecidr=True
                            )

        self.debug("Network upgrade failed!")

        self.debug("Deleting LB Rule: %s" % lb_rule.id)
        lb_rule.delete(self.apiclient)
        self.debug("LB rule deleted")

        # Assign VPN to source NAT
        self.debug("Enabling VPN on source NAT")
        vpn = Vpn.create(
                        self.apiclient,
                        src_nat.id,
                        account=self.account.account.name,
                        domainid=self.account.account.domainid
                        )

        vpns = Vpn.list(
                        self.apiclient,
                        publicipid=src_nat.id,
                        listall=True,
                        )

        self.assertEqual(
                         isinstance(vpns, list),
                         True,
                         "List VPNs should return a valid VPN list"
                         )

        self.assertNotEqual(
                            len(vpns),
                            0,
                            "Length of list VPN response should not be zero"
                            )
        self.debug("Upgrading the network: %s" % self.network.id)
        with self.assertRaises(Exception):
            self.network.update(
                            self.apiclient,
                            networkofferingid=ns_lb_offering.id,
                            changecidr=True
                            )
        return


@unittest.skip("Skipped since shared network requires StartIp/endIp/gateway/netmask")
class TestSharedNetworkWithoutIp(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestSharedNetworkWithoutIp,
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
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )

        cls._cleanup = [
                        cls.service_offering,
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
        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.account.delete(self.apiclient)
            interval = list_configurations(
                                    self.apiclient,
                                    name='account.cleanup.interval'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) * 2)
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "simulator", "network", "api"])
    def test_deployVmSharedNetworkWithoutIpRange(self):
        """Test deployVM in shared network without startIp/endIp
        """

        # Steps for validation
        # 1. create a shared network using shared network offering but do not
        #    specify startIp/endIp arguments
        # 2. create an account
        # 3. deploy a VM in this account using the above network
        # Validate the following
        # 1. listNetworks should return the created network
        # 2. listAccounts to return the created account
        # 3. VM deployment should succeed and NIC is in networks address space
        # 4. delete the account

        self.debug(
                "Fetching default shared network offering from nw offerings")
        network_offerings = NetworkOffering.list(
                                    self.apiclient,
                                    listall=True,
                                    guestiptype="Shared",
                                    name="DefaultSharedNetworkOffering",
                                    displaytext="Offering for Shared networks"
                                    )
        self.assertEqual(
                    isinstance(network_offerings, list),
                    True,
                    "Nw offerings should have atleast a shared nw offering"
                    )
        shared_nw_off = network_offerings[0]
        self.debug("Shared netwrk offering: %s" % shared_nw_off.name)

        self.debug("Creating a network from shared network offering")
        self.network = Network.create(
                                    self.apiclient,
                                    self.services["network"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    networkofferingid=shared_nw_off.id,
                                    zoneid=self.zone.id
                                    )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account.account.name)
        try:
            # Spawn an instance in that network
            VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.account.name,
                                  domainid=self.account.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network.id)]
                                  )
            self.debug("Deployed VM in network: %s" % self.network.id)
        except Exception as e:
            self.fail("Deply Vm in shared network failed! - %s" % e)
        return
