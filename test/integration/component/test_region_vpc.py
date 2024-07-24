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
import unittest

""" Component tests for region level VPC functionality
"""
#Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources, validateList
from marvin.lib.base import (VPC,
                             VpcOffering,
                             ServiceOffering,
                             PhysicalNetwork,
                             Account,
                             NetworkOffering,
                             Network,
                             VirtualMachine,
                             NetworkServiceProvider,
                             PublicIPAddress,
                             LoadBalancerRule,
                             NATRule,
                             NetworkACL,
                             StaticNATRule)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
from marvin.codes import FAILED, PASS


class Services:
    """Test inter VLAN services
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
                                    "supportedservices": 'Vpn,Dhcp,Dns,SourceNat,PortForwarding,Lb,UserData,StaticNat,NetworkACL, Connectivity',
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
                                            "NetworkACL": 'VpcVirtualRouter',
                                            "Connectivity": 'Ovs'
                                        },
                                    "serviceCapabilityList": {
                                        "Connectivity": {
                                            "StretchedL2Subnet": "true"
                                        },
                                    },
                                },
                         "vpc_offering": {
                                    "name": 'VPC off',
                                    "displaytext": 'VPC off',
                                    "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Lb,UserData,StaticNat,Connectivity',
                                    "serviceProviderList": {
                                            "Vpn": 'VpcVirtualRouter',
                                            "Dhcp": 'VpcVirtualRouter',
                                            "Dns": 'VpcVirtualRouter',
                                            "SourceNat": 'VpcVirtualRouter',
                                            "PortForwarding": 'VpcVirtualRouter',
                                            "Lb": 'VpcVirtualRouter',
                                            "UserData": 'VpcVirtualRouter',
                                            "StaticNat": 'VpcVirtualRouter',
                                            "Connectivity": 'Ovs'
                                        },
                                    "serviceCapabilityList": {
                                        "Connectivity": {
                                            "RegionLevelVpc": "true"
                                        },
                                    },
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
                                    "startport": 2222,
                                    "endport": 2222,
                                    "cidrlist": '0.0.0.0/0',
                                    "protocol": 'TCP'
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
                    }


class TestRegionVpcOffering(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestRegionVpcOffering, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = Services().services

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype


        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"]
        )
        if cls.template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]


        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.apiclient,
                                            cls.services["service_offering"]
                                            )
        cls._cleanup = [cls.service_offering, ]

        try:
            list_physical_networks = PhysicalNetwork.list(
                                                     cls.apiclient,
                                                     zoneid=cls.zone.id
                                                     )
            assert validateList(list_physical_networks)[0] == PASS,\
                "physical networks list validation failed"

            cls.isOvsPluginEnabled = False
            for i in range(0, len(list_physical_networks)):
                list_network_serviceprovider = NetworkServiceProvider.list(
                                                                       cls.apiclient,
                                                                       physicalnetworkid=list_physical_networks[i].id
                                                                       )
                for j in range(0, len(list_network_serviceprovider)):
                    if((str(list_network_serviceprovider[j].name).lower() == 'ovs') and
                        (str(list_network_serviceprovider[j].state).lower() == 'enabled')):
                        cls.isOvsPluginEnabled = True
                        break
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest(e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)
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
        self.cleanup.insert(0, self.account)
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
        self.assertEqual(
                 vpc_offering.name,
                 vpc_offs[0].name,
                "Name of the VPC offering should match with listVPCOff data"
                )
        self.assertEqual(
                 vpc_offs[0].supportsregionLevelvpc,True,
                 "VPC offering is not set up for region level VPC"
                )
        self.debug(
                "VPC offering is created successfully - %s" %
                                                        vpc_offering.name)
        return

    def validate_vpc_network(self, network):
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
        self.debug("VPC network created successfully - %s" % network.name)
        return

    @attr(tags=["advanced", "intervlan"])
    def test_01_create_vpc_offering_with_regionlevelvpc_service_capability(self):
        """ Test create VPC offering
        """

        # Steps for validation
        # 1. Create VPC Offering by specifying all supported Services
        # 2. VPC offering should be created successfully.

        self.debug("Creating inter VPC offering")
        vpc_off = VpcOffering.create(
                                     self.apiclient,
                                     self.services["vpc_offering"]
                                     )

        self.debug("Check if the VPC offering is created successfully?")
        self.cleanup.append(vpc_off)
        self.validate_vpc_offering(vpc_off)
        return

    @attr(tags=["advanced", "intervlan"])
    def test_02_create_vpc_from_offering_with_regionlevelvpc_service_capability(self):
        """ Test create VPC offering
        """

        # Steps for validation
        # 1. Create VPC Offering by specifying all supported Services
        # 2. VPC offering should be created successfully.

        if not self.isOvsPluginEnabled:
            self.skipTest("OVS plugin should be enabled to run this test case")

        self.debug("Creating inter VPC offering")
        vpc_off = VpcOffering.create(
                                     self.apiclient,
                                     self.services["vpc_offering"]
                                     )
        vpc_off.update(self.apiclient, state='Enabled')
        vpc = VPC.create(
                         self.apiclient,
                         self.services["vpc"],
                         vpcofferingid=vpc_off.id,
                         zoneid=self.zone.id,
                         account=self.account.name,
                         domainid=self.account.domainid,
                         networkDomain=self.account.domainid
                         )
        self.assertEqual(vpc.distributedvpcrouter, True, "VPC created should have 'distributedvpcrouter' set to True")

        try:
            vpc.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete VPC network - %s" % e)
        return

    @attr(tags=["advanced", "intervlan"])
    def test_03_deploy_vms_in_vpc_with_regionlevelvpc(self):
        """Test deploy virtual machines in VPC networks"""

        # 1. Create VPC Offering by specifying all supported Services
        #   (Vpn,dhcpdns,UserData, SourceNat,Static NAT and PF,LB,NetworkAcl)
        # 2. Create a VPC using the above VPC offering
        # 3. Create a network as part of this VPC.
        # 4. Deploy few Vms.
        # 5. Create a LB rule for this VM.
        # 6. Create a PF rule for this VM.
        # 7. Create a  Static Nat rule for this VM.
        # 8. Create Ingress rules on the network to open the above created
        #    LB PF and Static Nat rule
        # 9. Create Egress Network ACL for this network to access google.com.
        # 10. Enable VPN services

        if not self.isOvsPluginEnabled:
            self.skipTest("OVS plugin should be enabled to run this test case")

        self.debug("Creating a VPC offering..")
        vpc_off = VpcOffering.create(
                                     self.apiclient,
                                     self.services["vpc_offering"]
                                     )

        vpc_off.update(self.apiclient, state='Enabled')

        self.debug("creating a VPC network in the account: %s" %
                                                    self.account.name)
        vpc = VPC.create(
                         self.apiclient,
                         self.services["vpc"],
                         vpcofferingid=vpc_off.id,
                         zoneid=self.zone.id,
                         account=self.account.name,
                         domainid=self.account.domainid,
                         networkDomain=self.account.domainid
                         )
        self.validate_vpc_network(vpc)

        self.network_offering = NetworkOffering.create(
                                            self.apiclient,
                                            self.services["network_offering"],
                                            conservemode=False
                                            )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')

        gateway = vpc.cidr.split('/')[0]
        # Split the cidr to retrieve gateway
        # for eg. cidr = 10.0.0.1/24
        # Gateway = 10.0.0.1

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        network = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=self.network_offering.id,
                                zoneid=self.zone.id,
                                gateway=gateway,
                                vpcid=vpc.id
                                )
        self.debug("Created network with ID: %s" % network.id)
        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network.id)

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id,
                                vpcid=vpc.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip.ipaddress.ipaddress,
                                        network.id
                                        ))

        self.debug("Creating LB rule for IP address: %s" %
                                        public_ip.ipaddress.ipaddress)

        LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=public_ip.ipaddress.id,
                                    accountid=self.account.name,
                                    networkid=network.id,
                                    vpcid=vpc.id,
                                    domainid=self.account.domainid
                                )

        self.debug("Associating public IP for network: %s" % vpc.name)
        public_ip_2 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id,
                                vpcid=vpc.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_2.ipaddress.ipaddress,
                                        network.id
                                        ))

        NATRule.create(
                                  self.apiclient,
                                  virtual_machine,
                                  self.services["natrule"],
                                  ipaddressid=public_ip_2.ipaddress.id,
                                  openfirewall=False,
                                  networkid=network.id,
                                  vpcid=vpc.id
                                  )

        self.debug("Adding NetworkACl rules to make PF and LB accessible")
        NetworkACL.create(
                self.apiclient,
                networkid=network.id,
                services=self.services["natrule"],
                traffictype='Ingress'
                )

        NetworkACL.create(
                                self.apiclient,
                                networkid=network.id,
                                services=self.services["lbrule"],
                                traffictype='Ingress'
                                )
        self.debug("Checking if we can SSH into VM?")
        try:
            virtual_machine.get_ssh_client(
                ipaddress=public_ip_2.ipaddress.ipaddress,
                )
            self.debug("SSH into VM is successfully")
        except Exception as e:
            self.fail("Failed to SSH into VM - %s, %s" %
                    (public_ip_2.ipaddress.ipaddress, e))

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip_3 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id,
                                vpcid=vpc.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_3.ipaddress.ipaddress,
                                        network.id
                                        ))
        self.debug("Enabling static NAT for IP: %s" %
                                            public_ip_3.ipaddress.ipaddress)
        try:
            StaticNATRule.enable(
                              self.apiclient,
                              ipaddressid=public_ip_3.ipaddress.id,
                              virtualmachineid=virtual_machine.id,
                              networkid=network.id
                              )
            self.debug("Static NAT enabled for IP: %s" %
                                            public_ip_3.ipaddress.ipaddress)
        except Exception as e:
            self.fail("Failed to enable static NAT on IP: %s - %s" % (
                                            public_ip_3.ipaddress.ipaddress, e))

        public_ips = PublicIPAddress.list(
                                          self.apiclient,
                                          networkid=network.id,
                                          listall=True,
                                          isstaticnat=True,
                                          account=self.account.name,
                                          domainid=self.account.domainid
                                          )
        self.assertEqual(
                         isinstance(public_ips, list),
                         True,
                         "List public Ip for network should list the Ip addr"
                         )
        self.assertEqual(
                         public_ips[0].ipaddress,
                         public_ip_3.ipaddress.ipaddress,
                         "List public Ip for network should list the Ip addr"
                         )
        # TODO: Remote Access VPN is not yet supported in VPC
        return
