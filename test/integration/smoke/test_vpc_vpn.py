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
""" Tests for VPN in VPC
"""
# Import Local Modules
from marvin.codes import PASS, FAILED
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (validateList,
                              wait_until)

from marvin.lib.base import (Account,
                             VPC,
                             VpcOffering,
                             ServiceOffering,
                             NetworkOffering,
                             Network,
                             PublicIPAddress,
                             NATRule,
                             NetworkACLList,
                             VirtualMachine,
                             Vpn,
                             VpnCustomerGateway,
                             VpnUser
                             )

from marvin.sshClient import SshClient


from marvin.lib.common import (get_zone,
                               get_domain,
                               get_test_template)

from nose.plugins.attrib import attr

import logging
import time


class Services:

    """Test VPC VPN Services.
    """

    def __init__(self):
        self.services = {
            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
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
                "cidr": '10.1.0.0/16'
            },
            "vpc1": {
                "name": "TestVPC",
                "displaytext": "VPC1",
                "cidr": '10.1.0.0/16'
            },
            "vpc2": {
                "name": "TestVPC",
                "displaytext": "VPC2",
                "cidr": '10.3.0.0/16'
            },
            "network_1": {
                "name": "Test Network",
                "displaytext": "Test Network",
                "netmask": '255.255.255.0',
                "gateway": "10.1.1.1"
            },
            "network_2": {
                "name": "Test Network",
                "displaytext": "Test Network",
                "netmask": '255.255.255.0',
                "gateway": "10.3.1.1"
            },
            "vpn": {
                "vpn_user": "root",
                "vpn_pass": "Md1sdc",
                "vpn_pass_fail": "abc!123",  # too short
                "iprange": "10.3.2.1-10.3.2.10",
                "fordisplay": "true"
            },
            "vpncustomergateway": {
                "esppolicy": "3des-md5;modp1536",
                "ikepolicy": "3des-md5;modp1536",
                "ipsecpsk": "ipsecpsk"
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


class TestVpcRemoteAccessVpn(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.logger = logging.getLogger('TestVPCRemoteAccessVPN')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        testClient = super(TestVpcRemoteAccessVpn, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = Services().services

        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.domain = get_domain(cls.apiclient)

        cls._cleanup = []

        cls.compute_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["compute_offering"]
        )
        cls._cleanup.append(cls.compute_offering)
        cls.account = Account.create(
            cls.apiclient, services=cls.services["account"])
        cls._cleanup.append(cls.account)

        cls.hypervisor = testClient.getHypervisorInfo()

        cls.template = get_test_template(cls.apiclient, cls.zone.id, cls.hypervisor)
        if cls.template == FAILED:
            assert False, "get_test_template() failed to return template"

        cls.logger.debug("Successfully created account: %s, id: \
                   %s" % (cls.account.name,
                          cls.account.id))

        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_01_vpc_remote_access_vpn(self):
        """Test Remote Access VPN in VPC"""

        self.logger.debug("Starting test: test_01_vpc_remote_access_vpn")

        # 0) Get the default network offering for VPC
        self.logger.debug("Retrieving default VPC offering")
        networkOffering = NetworkOffering.list(
            self.apiclient, name="DefaultIsolatedNetworkOfferingForVpcNetworks")
        self.assertTrue(networkOffering is not None and len(
            networkOffering) > 0, "No VPC based network offering")

        # 1) Create VPC
        vpcOffering = VpcOffering.list(self.apiclient, name="Default VPC offering")
        self.assertTrue(vpcOffering is not None and len(
            vpcOffering) > 0, "No VPC offerings found")

        vpc = None
        try:
            vpc = VPC.create(
                apiclient=self.apiclient,
                services=self.services["vpc"],
                networkDomain="vpc.vpn",
                vpcofferingid=vpcOffering[0].id,
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.domain.id
            )
            self.cleanup.append(vpc)
        except Exception as e:
            self.fail(e)
        finally:
            self.assertTrue(vpc is not None, "VPC creation failed")
            self.logger.debug("VPC %s created" % (vpc.id))

        try:
            # 2) Create network in VPC
            ntwk = Network.create(
                apiclient=self.apiclient,
                services=self.services["network_1"],
                accountid=self.account.name,
                domainid=self.domain.id,
                networkofferingid=networkOffering[0].id,
                zoneid=self.zone.id,
                vpcid=vpc.id
            )
        except Exception as e:
            self.fail(e)
        finally:
            self.assertIsNotNone(ntwk, "Network failed to create")
            self.cleanup.append(ntwk)
            self.logger.debug(
                "Network %s created in VPC %s" % (ntwk.id, vpc.id))

        try:
            # 3) Deploy a vm
            vm = VirtualMachine.create(self.apiclient, services=self.services["virtual_machine"],
                                       templateid=self.template.id,
                                       zoneid=self.zone.id,
                                       accountid=self.account.name,
                                       domainid=self.domain.id,
                                       serviceofferingid=self.compute_offering.id,
                                       networkids=ntwk.id,
                                       hypervisor=self.hypervisor
                                       )
            self.assertTrue(vm is not None, "VM failed to deploy")
            self.cleanup.append(vm)
            self.assertTrue(vm.state == 'Running', "VM is not running")
            self.debug("VM %s deployed in VPC %s" % (vm.id, vpc.id))
        except Exception as e:
            self.fail(e)
        finally:
            self.logger.debug("Deployed virtual machine: OK")

        try:
            # 4) Enable VPN for VPC
            src_nat_list = PublicIPAddress.list(
                self.apiclient,
                account=self.account.name,
                domainid=self.account.domainid,
                listall=True,
                issourcenat=True,
                vpcid=vpc.id
            )
            ip = src_nat_list[0]
        except Exception as e:
            self.fail(e)
        finally:
            self.logger.debug("Acquired public ip address: OK")

        vpn = None
        try:
            vpn = Vpn.create(self.apiclient,
                             publicipid=ip.id,
                             account=self.account.name,
                             domainid=self.account.domainid,
                             iprange=self.services["vpn"]["iprange"],
                             fordisplay=self.services["vpn"]["fordisplay"]
                             )
        except Exception as e:
            self.fail(e)
        finally:
            self.assertIsNotNone(vpn, "Failed to create Remote Access VPN")
            self.logger.debug("Created Remote Access VPN: OK")

        vpnUser = None
        # 5) Add VPN user for VPC
        try:
            vpnUser = VpnUser.create(self.apiclient,
                                     account=self.account.name,
                                     domainid=self.account.domainid,
                                     username=self.services["vpn"]["vpn_user"],
                                     password=self.services["vpn"]["vpn_pass"]
                                     )
        except Exception as e:
            self.fail(e)
        finally:
            self.assertIsNotNone(
                vpnUser, "Failed to create Remote Access VPN User")
            self.logger.debug("Created VPN User: OK")

        # TODO: Add an actual remote vpn connection test from a remote vpc

        try:
            # 9) Disable VPN for VPC
            vpn.delete(self.apiclient)
        except Exception as e:
            self.fail(e)
        finally:
            self.logger.debug("Deleted the Remote Access VPN: OK")

    @classmethod
    def tearDownClass(cls):
        super(TestVpcRemoteAccessVpn, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.cleanup = []

    def tearDown(self):
        super(TestVpcRemoteAccessVpn, self).tearDown()


class TestVpcSite2SiteVpn(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.logger = logging.getLogger('TestVPCSite2SiteVPN')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        testClient = super(TestVpcSite2SiteVpn, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = Services().services

        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.domain = get_domain(cls.apiclient)

        cls._cleanup = []

        cls.compute_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["compute_offering"]
        )
        cls._cleanup.append(cls.compute_offering)

        cls.account = Account.create(
            cls.apiclient, services=cls.services["account"])
        cls._cleanup.append(cls.account)

        cls.hypervisor = testClient.getHypervisorInfo()

        cls.template = get_test_template(cls.apiclient, cls.zone.id, cls.hypervisor)
        if cls.template == FAILED:
            assert False, "get_test_template() failed to return template"

        cls.logger.debug("Successfully created account: %s, id: \
                   %s" % (cls.account.name,
                          cls.account.id))
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.cleanup = []

    def _get_ssh_client(self, virtual_machine, services, retries):
        """ Setup ssh client connection and return connection
        vm requires attributes public_ip, public_port, username, password """

        try:
            ssh_client = SshClient(
                virtual_machine.public_ip,
                services["virtual_machine"]["ssh_port"],
                services["virtual_machine"]["username"],
                services["virtual_machine"]["password"],
                retries)

        except Exception as e:
            self.fail("Unable to create ssh connection: " % e)

        self.assertIsNotNone(
            ssh_client, "Failed to setup ssh connection to vm=%s on public_ip=%s" % (virtual_machine.name, virtual_machine.public_ip))

        return ssh_client

    def _create_natrule(self, vpc, vm, public_port, private_port, public_ip, network, services=None):
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

    def _validate_vpc_offering(self, vpc_offering):

        self.logger.debug("Check if the VPC offering is created successfully?")
        vpc_offs = VpcOffering.list(
            self.apiclient,
            id=vpc_offering.id
        )
        offering_list = validateList(vpc_offs)
        self.assertEqual(offering_list[0],
                         PASS,
                         "List VPC offerings should return a valid list"
                         )
        self.assertEqual(
            vpc_offering.name,
            vpc_offs[0].name,
            "Name of the VPC offering should match with listVPCOff data"
        )
        self.logger.debug(
            "VPC offering is created successfully - %s" %
            vpc_offering.name)
        return

    def _create_vpc_offering(self, offering_name):

        vpc_off = None
        if offering_name is not None:

            self.logger.debug("Creating VPC offering: %s", offering_name)
            vpc_off = VpcOffering.create(
                self.apiclient,
                self.services[offering_name]
            )

            self._validate_vpc_offering(vpc_off)
            self.cleanup.append(vpc_off)

        return vpc_off

    @attr(tags=["advanced"], required_hardware="true")
    def test_01_vpc_site2site_vpn(self):
        """Test Site 2 Site VPN Across VPCs"""
        self.logger.debug("Starting test: test_01_vpc_site2site_vpn")
        # 0) Get the default network offering for VPC
        networkOffering = NetworkOffering.list(
            self.apiclient, name="DefaultIsolatedNetworkOfferingForVpcNetworks")
        self.assertTrue(networkOffering is not None and len(
            networkOffering) > 0, "No VPC based network offering")

        # Create and Enable VPC offering
        vpc_offering = self._create_vpc_offering('vpc_offering')
        self.assertTrue(vpc_offering is not None, "Failed to create VPC Offering")
        vpc_offering.update(self.apiclient, state='Enabled')

        vpc1 = None
        # Create VPC 1
        try:
            vpc1 = VPC.create(
                apiclient=self.apiclient,
                services=self.services["vpc"],
                networkDomain="vpc1.vpn",
                vpcofferingid=vpc_offering.id,
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.domain.id
            )
        except Exception as e:
            self.fail(e)
        finally:
            self.assertTrue(vpc1 is not None, "VPC1 creation failed")
        self.cleanup.append(vpc1)
        self.logger.debug("VPC1 %s created" % vpc1.id)

        vpc2 = None
        # Create VPC 2
        try:
            vpc2 = VPC.create(
                apiclient=self.apiclient,
                services=self.services["vpc2"],
                networkDomain="vpc2.vpn",
                vpcofferingid=vpc_offering.id,
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid
            )
        except Exception as e:
            self.fail(e)
        finally:
            self.assertTrue(vpc2 is not None, "VPC2 creation failed")
        self.cleanup.append(vpc2)
        self.logger.debug("VPC2 %s created" % vpc2.id)

        default_acl = NetworkACLList.list(
            self.apiclient, name="default_allow")[0]

        ntwk1 = None
        # Create network in VPC 1
        try:
            ntwk1 = Network.create(
                apiclient=self.apiclient,
                services=self.services["network_1"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=networkOffering[0].id,
                zoneid=self.zone.id,
                vpcid=vpc1.id,
                aclid=default_acl.id
            )
        except Exception as e:
            self.fail(e)
        finally:
            self.assertIsNotNone(ntwk1, "Network failed to create")
        self.cleanup.append(ntwk1)
        self.logger.debug("Network %s created in VPC %s" % (ntwk1.id, vpc1.id))

        ntwk2 = None
        # Create network in VPC 2
        try:
            ntwk2 = Network.create(
                apiclient=self.apiclient,
                services=self.services["network_2"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=networkOffering[0].id,
                zoneid=self.zone.id,
                vpcid=vpc2.id,
                aclid=default_acl.id
            )
        except Exception as e:
            self.fail(e)
        finally:
            self.assertIsNotNone(ntwk2, "Network failed to create")
        self.cleanup.append(ntwk2)
        self.logger.debug("Network %s created in VPC %s" % (ntwk2.id, vpc2.id))

        vm1 = None
        # Deploy a vm in network 2
        try:
            vm1 = VirtualMachine.create(self.apiclient, services=self.services["virtual_machine"],
                                        templateid=self.template.id,
                                        zoneid=self.zone.id,
                                        accountid=self.account.name,
                                        domainid=self.account.domainid,
                                        serviceofferingid=self.compute_offering.id,
                                        networkids=ntwk1.id,
                                        hypervisor=self.hypervisor
                                        )
        except Exception as e:
            self.fail(e)
        finally:
            self.assertTrue(vm1 is not None, "VM failed to deploy")
            self.assertTrue(vm1.state == 'Running', "VM is not running")
        self.cleanup.append(vm1)
        self.logger.debug("VM %s deployed in VPC %s" % (vm1.id, vpc1.id))

        vm2 = None
        # Deploy a vm in network 2
        try:
            vm2 = VirtualMachine.create(self.apiclient, services=self.services["virtual_machine"],
                                        templateid=self.template.id,
                                        zoneid=self.zone.id,
                                        accountid=self.account.name,
                                        domainid=self.account.domainid,
                                        serviceofferingid=self.compute_offering.id,
                                        networkids=ntwk2.id,
                                        hypervisor=self.hypervisor
                                        )
        except Exception as e:
            self.fail(e)
        finally:
            self.assertTrue(vm2 is not None, "VM failed to deploy")
            self.assertTrue(vm2.state == 'Running', "VM is not running")
        self.cleanup.append(vm2)
        self.debug("VM %s deployed in VPC %s" % (vm2.id, vpc2.id))

        # 4) Enable Site-to-Site VPN for VPC
        vpn1_response = Vpn.createVpnGateway(self.apiclient, vpc1.id)
        self.assertTrue(
            vpn1_response is not None, "Failed to enable VPN Gateway 1")
        self.logger.debug("VPN gateway for VPC %s enabled" % vpc1.id)

        vpn2_response = Vpn.createVpnGateway(self.apiclient, vpc2.id)
        self.assertTrue(
            vpn2_response is not None, "Failed to enable VPN Gateway 2")
        self.logger.debug("VPN gateway for VPC %s enabled" % vpc2.id)

        # 5) Add VPN Customer gateway info
        src_nat_list = PublicIPAddress.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True,
            issourcenat=True,
            vpcid=vpc1.id
        )
        ip1 = src_nat_list[0]
        src_nat_list = PublicIPAddress.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True,
            issourcenat=True,
            vpcid=vpc2.id
        )
        ip2 = src_nat_list[0]

        services = self.services["vpncustomergateway"]
        customer1_response = VpnCustomerGateway.create(
            self.apiclient, services, "Peer VPC1", ip1.ipaddress, vpc1.cidr, self.account.name, self.domain.id)
        self.debug("VPN customer gateway added for VPC %s enabled" % vpc1.id)
        self.logger.debug(vars(customer1_response))

        customer2_response = VpnCustomerGateway.create(
            self.apiclient, services, "Peer VPC2", ip2.ipaddress, vpc2.cidr, self.account.name, self.domain.id)
        self.debug("VPN customer gateway added for VPC %s enabled" % vpc2.id)
        self.logger.debug(vars(customer2_response))

        # 6) Connect two VPCs
        vpnconn1_response = Vpn.createVpnConnection(
            self.apiclient, customer1_response.id, vpn2_response['id'], True)
        self.debug("VPN passive connection created for VPC %s" % vpc2.id)

        vpnconn2_response = Vpn.createVpnConnection(
            self.apiclient, customer2_response.id, vpn1_response['id'])
        self.debug("VPN connection created for VPC %s" % vpc1.id)

        def checkVpnConnected():
            connections = Vpn.listVpnConnection(
                self.apiclient,
                listall='true',
                vpcid=vpc2.id)
            if isinstance(connections, list):
                return connections[0].state == 'Connected', None
            return False, None

        # Wait up to 60 seconds for passive connection to show up as Connected
        res, _ = wait_until(2, 30, checkVpnConnected)
        if not res:
            self.fail("Failed to connect between VPCs, see VPN state as Connected")

        # acquire an extra ip address to use to ssh into vm2
        try:
            vm2.public_ip = PublicIPAddress.create(
                apiclient=self.apiclient,
                accountid=self.account.name,
                zoneid=self.zone.id,
                domainid=self.account.domainid,
                services=self.services,
                networkid=ntwk2.id,
                vpcid=vpc2.id)
        except Exception as e:
            self.fail(e)
        finally:
            self.assertTrue(
                vm2.public_ip is not None, "Failed to aqcuire public ip for vm2")

        natrule = None
        # Create port forward to be able to ssh into vm2
        try:
            natrule = self._create_natrule(
                vpc2, vm2, 22, 22, vm2.public_ip, ntwk2)
        except Exception as e:
            self.fail(e)
        finally:
            self.assertTrue(
                natrule is not None, "Failed to create portforward for vm2")
            time.sleep(20)

        # setup ssh connection to vm2
        ssh_client = self._get_ssh_client(vm2, self.services, 10)

        if ssh_client:
            # run ping test
            packet_loss = ssh_client.execute("/bin/ping -c 3 -t 10 " + vm1.nic[0].ipaddress + " | grep packet | sed 's/.*received, //g' | sed 's/[% ]*packet.*//g'")[0]
            # during startup, some packets may not reply due to link/ipsec-route setup
            self.assertTrue(int(packet_loss) < 50, "Ping did not succeed")
        else:
            self.fail("Failed to setup ssh connection to %s" % vm2.public_ip)

    @classmethod
    def tearDownClass(cls):
        super(TestVpcSite2SiteVpn, cls).tearDownClass()

    def tearDown(self):
        super(TestVpcSite2SiteVpn, self).tearDown()


class TestRVPCSite2SiteVpn(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.logger = logging.getLogger('TestRVPCSite2SiteVPN')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        testClient = super(TestRVPCSite2SiteVpn, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = Services().services

        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.domain = get_domain(cls.apiclient)
        cls._cleanup = []

        cls.compute_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["compute_offering"]
        )
        cls._cleanup.append(cls.compute_offering)

        cls.account = Account.create(
            cls.apiclient, services=cls.services["account"])
        cls._cleanup.append(cls.account)

        cls.hypervisor = testClient.getHypervisorInfo()

        cls.template = get_test_template(cls.apiclient, cls.zone.id, cls.hypervisor)
        if cls.template == FAILED:
            assert False, "get_test_template() failed to return template"

        cls.logger.debug("Successfully created account: %s, id: \
                   %s" % (cls.account.name,
                          cls.account.id))
        return

    def _validate_vpc_offering(self, vpc_offering):

        self.logger.debug("Check if the VPC offering is created successfully?")
        vpc_offs = VpcOffering.list(
            self.apiclient,
            id=vpc_offering.id
        )
        offering_list = validateList(vpc_offs)
        self.assertEqual(offering_list[0],
                         PASS,
                         "List VPC offerings should return a valid list"
                         )
        self.assertEqual(
            vpc_offering.name,
            vpc_offs[0].name,
            "Name of the VPC offering should match with listVPCOff data"
        )
        self.logger.debug(
            "VPC offering is created successfully - %s" %
            vpc_offering.name)
        return

    def _create_vpc_offering(self, offering_name):

        vpc_off = None
        if offering_name is not None:

            self.logger.debug("Creating VPC offering: %s", offering_name)
            vpc_off = VpcOffering.create(
                self.apiclient,
                self.services[offering_name]
            )

            self._validate_vpc_offering(vpc_off)
            self.cleanup.append(vpc_off)

        return vpc_off

    def _get_ssh_client(self, virtual_machine, services, retries):
        """ Setup ssh client connection and return connection
        vm requires attributes public_ip, public_port, username, password """

        try:
            ssh_client = SshClient(
                virtual_machine.public_ip,
                services["virtual_machine"]["ssh_port"],
                services["virtual_machine"]["username"],
                services["virtual_machine"]["password"],
                retries)

        except Exception as e:
            self.fail("Unable to create ssh connection: %s" % e)

        self.assertIsNotNone(
            ssh_client, "Failed to setup ssh connection to vm=%s on public_ip=%s" % (virtual_machine.name, virtual_machine.public_ip))

        return ssh_client

    def _create_natrule(self, vpc, vm, public_port, private_port, public_ip, network, services=None):
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

    @attr(tags=["advanced"], required_hardware="true")
    def test_01_redundant_vpc_site2site_vpn(self):
        """Test Site 2 Site VPN Across redundant VPCs"""
        self.logger.debug("Starting test: test_02_redundant_vpc_site2site_vpn")

        # 0) Get the default network offering for VPC
        networkOffering = NetworkOffering.list(
            self.apiclient, name="DefaultIsolatedNetworkOfferingForVpcNetworks")
        self.assertTrue(networkOffering is not None and len(
            networkOffering) > 0, "No VPC based network offering")

        # Create and enable redundant VPC offering
        redundant_vpc_offering = self._create_vpc_offering(
            'redundant_vpc_offering')
        self.assertTrue(redundant_vpc_offering is not None,
                     "Failed to create redundant VPC Offering")
        redundant_vpc_offering.update(self.apiclient, state='Enabled')

        # Create VPC 1
        vpc1 = None
        try:
            vpc1 = VPC.create(
                apiclient=self.apiclient,
                services=self.services["vpc"],
                networkDomain="vpc1.vpn",
                vpcofferingid=redundant_vpc_offering.id,
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.domain.id
            )
        except Exception as e:
            self.fail(e)
        finally:
            self.assertTrue(vpc1 is not None, "VPC1 creation failed")
        self.cleanup.append(vpc1)
        self.logger.debug("VPC1 %s created" % vpc1.id)

        # Create VPC 2
        vpc2 = None
        try:
            vpc2 = VPC.create(
                apiclient=self.apiclient,
                services=self.services["vpc2"],
                networkDomain="vpc2.vpn",
                vpcofferingid=redundant_vpc_offering.id,
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid
            )
        except Exception as e:
            self.fail(e)
        finally:
            self.assertTrue(vpc2 is not None, "VPC2 creation failed")
        self.cleanup.append(vpc2)
        self.logger.debug("VPC2 %s created" % vpc2.id)

        default_acl = NetworkACLList.list(
            self.apiclient, name="default_allow")[0]

        # Create network in VPC 1
        ntwk1 = None
        try:
            ntwk1 = Network.create(
                apiclient=self.apiclient,
                services=self.services["network_1"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=networkOffering[0].id,
                zoneid=self.zone.id,
                vpcid=vpc1.id,
                aclid=default_acl.id
            )
        except Exception as e:
            self.fail(e)
        finally:
            self.assertIsNotNone(ntwk1, "Network failed to create")
        self.cleanup.append(ntwk1)
        self.logger.debug("Network %s created in VPC %s" % (ntwk1.id, vpc1.id))

        # Create network in VPC 2
        ntwk2 = None
        try:
            ntwk2 = Network.create(
                apiclient=self.apiclient,
                services=self.services["network_2"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=networkOffering[0].id,
                zoneid=self.zone.id,
                vpcid=vpc2.id,
                aclid=default_acl.id
            )
        except Exception as e:
            self.fail(e)
        finally:
            self.assertIsNotNone(ntwk2, "Network failed to create")
        self.cleanup.append(ntwk2)
        self.logger.debug("Network %s created in VPC %s" % (ntwk2.id, vpc2.id))

        # Deploy a vm in network 2
        vm1 = None
        try:
            vm1 = VirtualMachine.create(self.apiclient, services=self.services["virtual_machine"],
                                        templateid=self.template.id,
                                        zoneid=self.zone.id,
                                        accountid=self.account.name,
                                        domainid=self.account.domainid,
                                        serviceofferingid=self.compute_offering.id,
                                        networkids=ntwk1.id,
                                        hypervisor=self.hypervisor
                                        )
        except Exception as e:
            self.fail(e)
        finally:
            self.assertTrue(vm1 is not None, "VM failed to deploy")
            self.assertTrue(vm1.state == 'Running', "VM is not running")
        self.cleanup.append(vm1)
        self.logger.debug("VM %s deployed in VPC %s" % (vm1.id, vpc1.id))

        # Deploy a vm in network 2
        vm2 = None
        try:
            vm2 = VirtualMachine.create(self.apiclient, services=self.services["virtual_machine"],
                                        templateid=self.template.id,
                                        zoneid=self.zone.id,
                                        accountid=self.account.name,
                                        domainid=self.account.domainid,
                                        serviceofferingid=self.compute_offering.id,
                                        networkids=ntwk2.id,
                                        hypervisor=self.hypervisor
                                        )
        except Exception as e:
            self.fail(e)
        finally:
            self.assertTrue(vm2 is not None, "VM failed to deploy")
            self.assertTrue(vm2.state == 'Running', "VM is not running")
        self.cleanup.append(vm2)
        self.debug("VM %s deployed in VPC %s" % (vm2.id, vpc2.id))

        # 4) Enable Site-to-Site VPN for VPC
        vpn1_response = Vpn.createVpnGateway(self.apiclient, vpc1.id)
        self.assertTrue(
            vpn1_response is not None, "Failed to enable VPN Gateway 1")
        self.logger.debug("VPN gateway for VPC %s enabled" % vpc1.id)

        vpn2_response = Vpn.createVpnGateway(self.apiclient, vpc2.id)
        self.assertTrue(
            vpn2_response is not None, "Failed to enable VPN Gateway 2")
        self.logger.debug("VPN gateway for VPC %s enabled" % vpc2.id)

        # 5) Add VPN Customer gateway info
        src_nat_list = PublicIPAddress.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True,
            issourcenat=True,
            vpcid=vpc1.id
        )
        ip1 = src_nat_list[0]
        src_nat_list = PublicIPAddress.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True,
            issourcenat=True,
            vpcid=vpc2.id
        )
        ip2 = src_nat_list[0]

        services = self.services["vpncustomergateway"]
        customer1_response = VpnCustomerGateway.create(
            self.apiclient, services, "Peer VPC1", ip1.ipaddress, vpc1.cidr, self.account.name, self.domain.id)
        self.debug("VPN customer gateway added for VPC %s enabled" % vpc1.id)
        self.logger.debug(vars(customer1_response))

        customer2_response = VpnCustomerGateway.create(
            self.apiclient, services, "Peer VPC2", ip2.ipaddress, vpc2.cidr, self.account.name, self.domain.id)
        self.debug("VPN customer gateway added for VPC %s enabled" % vpc2.id)
        self.logger.debug(vars(customer2_response))

        # 6) Connect two VPCs
        vpnconn1_response = Vpn.createVpnConnection(
            self.apiclient, customer1_response.id, vpn2_response['id'], True)
        self.debug("VPN passive connection created for VPC %s" % vpc2.id)

        vpnconn2_response = Vpn.createVpnConnection(
            self.apiclient, customer2_response.id, vpn1_response['id'])
        self.debug("VPN connection created for VPC %s" % vpc1.id)

        def checkVpnConnected():
            connections = Vpn.listVpnConnection(
                self.apiclient,
                listall='true',
                vpcid=vpc2.id)
            if isinstance(connections, list):
                return connections[0].state == 'Connected', None
            return False, None

        # Wait up to 60 seconds for passive connection to show up as Connected
        res, _ = wait_until(2, 30, checkVpnConnected)
        if not res:
            self.fail("Failed to connect between VPCs, see VPN state as Connected")

        # acquire an extra ip address to use to ssh into vm2
        try:
            vm2.public_ip = PublicIPAddress.create(
                apiclient=self.apiclient,
                accountid=self.account.name,
                zoneid=self.zone.id,
                domainid=self.account.domainid,
                services=self.services,
                networkid=ntwk2.id,
                vpcid=vpc2.id)
        except Exception as e:
            self.fail(e)
        finally:
            self.assertTrue(
                vm2.public_ip is not None, "Failed to aqcuire public ip for vm2")

        # Create port forward to be able to ssh into vm2
        natrule = None
        try:
            natrule = self._create_natrule(
                vpc2, vm2, 22, 22, vm2.public_ip, ntwk2)
        except Exception as e:
            self.fail(e)
        finally:
            self.assertTrue(
                natrule is not None, "Failed to create portforward for vm2")
            time.sleep(20)

        # setup ssh connection to vm2
        ssh_client = self._get_ssh_client(vm2, self.services, 10)

        if ssh_client:
            # run ping test
            packet_loss = ssh_client.execute("/bin/ping -c 3 -t 10 " + vm1.nic[0].ipaddress + " | grep packet | sed 's/.*received, //g' | sed 's/[% ]*packet.*//g'")[0]
            self.assertTrue(int(packet_loss) < 50, "Ping did not succeed")
        else:
            self.fail("Failed to setup ssh connection to %s" % vm2.public_ip)

    @classmethod
    def tearDownClass(cls):
        super(TestRVPCSite2SiteVpn, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.cleanup = []

    def tearDown(self):
        super(TestRVPCSite2SiteVpn, self).tearDown()

class TestVPCSite2SiteVPNMultipleOptions(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.logger = logging.getLogger('TestVPCSite2SiteVPNMultipleOptions')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        testClient = super(TestVPCSite2SiteVPNMultipleOptions, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = Services().services

        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.domain = get_domain(cls.apiclient)

        cls._cleanup = []

        cls.compute_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["compute_offering"]
        )
        cls._cleanup.append(cls.compute_offering)

        cls.account = Account.create(
            cls.apiclient, services=cls.services["account"])
        cls._cleanup.append(cls.account)

        cls.hypervisor = testClient.getHypervisorInfo()

        cls.template = get_test_template(cls.apiclient, cls.zone.id, cls.hypervisor)
        if cls.template == FAILED:
            assert False, "get_test_template() failed to return template"

        cls.logger.debug("Successfully created account: %s, id: \
                   %s" % (cls.account.name,
                          cls.account.id))
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.cleanup = []

    def _get_ssh_client(self, virtual_machine, services, retries):
        """ Setup ssh client connection and return connection
        vm requires attributes public_ip, public_port, username, password """

        try:
            ssh_client = SshClient(
                virtual_machine.public_ip,
                services["virtual_machine"]["ssh_port"],
                services["virtual_machine"]["username"],
                services["virtual_machine"]["password"],
                retries)

        except Exception as e:
            self.fail("Unable to create ssh connection: " % e)

        self.assertIsNotNone(
            ssh_client, "Failed to setup ssh connection to vm=%s on public_ip=%s" % (virtual_machine.name, virtual_machine.public_ip))

        return ssh_client

    def _create_natrule(self, vpc, vm, public_port, private_port, public_ip, network, services=None):
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

    def _validate_vpc_offering(self, vpc_offering):

        self.logger.debug("Check if the VPC offering is created successfully?")
        vpc_offs = VpcOffering.list(
            self.apiclient,
            id=vpc_offering.id
        )
        offering_list = validateList(vpc_offs)
        self.assertEqual(offering_list[0],
                         PASS,
                         "List VPC offerings should return a valid list"
                         )
        self.assertEqual(
            vpc_offering.name,
            vpc_offs[0].name,
            "Name of the VPC offering should match with listVPCOff data"
        )
        self.logger.debug(
            "VPC offering is created successfully - %s" %
            vpc_offering.name)
        return

    def _create_vpc_offering(self, offering_name):

        vpc_off = None
        if offering_name is not None:

            self.logger.debug("Creating VPC offering: %s", offering_name)
            vpc_off = VpcOffering.create(
                self.apiclient,
                self.services[offering_name]
            )

            self._validate_vpc_offering(vpc_off)
            self.cleanup.append(vpc_off)

        return vpc_off

    @attr(tags=["advanced"], required_hardware="true")
    def test_01_vpc_site2site_vpn_multiple_options(self):
        """Test Site 2 Site VPN Across VPCs"""
        self.logger.debug("Starting test: test_01_vpc_site2site_vpn_multiple_options")
        # 0) Get the default network offering for VPC
        networkOffering = NetworkOffering.list(
            self.apiclient, name="DefaultIsolatedNetworkOfferingForVpcNetworks")
        self.assertTrue(networkOffering is not None and len(
            networkOffering) > 0, "No VPC based network offering")

        # Create and Enable VPC offering
        vpc_offering = self._create_vpc_offering('vpc_offering')
        self.assertTrue(vpc_offering is not None, "Failed to create VPC Offering")
        vpc_offering.update(self.apiclient, state='Enabled')

        vpc1 = None
        # Create VPC 1
        try:
            vpc1 = VPC.create(
                apiclient=self.apiclient,
                services=self.services["vpc"],
                networkDomain="vpc1.vpn",
                vpcofferingid=vpc_offering.id,
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.domain.id
            )
        except Exception as e:
            self.fail(e)
        finally:
            self.assertTrue(vpc1 is not None, "VPC1 creation failed")
        self.cleanup.append(vpc1)
        self.logger.debug("VPC1 %s created" % vpc1.id)

        vpc2 = None
        # Create VPC 2
        try:
            vpc2 = VPC.create(
                apiclient=self.apiclient,
                services=self.services["vpc2"],
                networkDomain="vpc2.vpn",
                vpcofferingid=vpc_offering.id,
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.domain.id
            )
        except Exception as e:
            self.fail(e)
        finally:
            self.assertTrue(vpc2 is not None, "VPC2 creation failed")
        self.cleanup.append(vpc2)
        self.logger.debug("VPC2 %s created" % vpc2.id)

        default_acl = NetworkACLList.list(
            self.apiclient, name="default_allow")[0]

        ntwk1 = None
        # Create network in VPC 1
        try:
            ntwk1 = Network.create(
                apiclient=self.apiclient,
                services=self.services["network_1"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=networkOffering[0].id,
                zoneid=self.zone.id,
                vpcid=vpc1.id,
                aclid=default_acl.id
            )
        except Exception as e:
            self.fail(e)
        finally:
            self.assertIsNotNone(ntwk1, "Network failed to create")
        self.cleanup.append(ntwk1)
        self.logger.debug("Network %s created in VPC %s" % (ntwk1.id, vpc1.id))

        ntwk2 = None
        # Create network in VPC 2
        try:
            ntwk2 = Network.create(
                apiclient=self.apiclient,
                services=self.services["network_2"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=networkOffering[0].id,
                zoneid=self.zone.id,
                vpcid=vpc2.id,
                aclid=default_acl.id
            )
        except Exception as e:
            self.fail(e)
        finally:
            self.assertIsNotNone(ntwk2, "Network failed to create")
        self.cleanup.append(ntwk2)
        self.logger.debug("Network %s created in VPC %s" % (ntwk2.id, vpc2.id))

        vm1 = None
        # Deploy a vm in network 2
        try:
            vm1 = VirtualMachine.create(self.apiclient, services=self.services["virtual_machine"],
                                        templateid=self.template.id,
                                        zoneid=self.zone.id,
                                        accountid=self.account.name,
                                        domainid=self.account.domainid,
                                        serviceofferingid=self.compute_offering.id,
                                        networkids=ntwk1.id,
                                        hypervisor=self.hypervisor
                                        )
        except Exception as e:
            self.fail(e)
        finally:
            self.assertTrue(vm1 is not None, "VM failed to deploy")
            self.assertTrue(vm1.state == 'Running', "VM is not running")
        self.cleanup.append(vm1)
        self.logger.debug("VM %s deployed in VPC %s" % (vm1.id, vpc1.id))

        vm2 = None
        # Deploy a vm in network 2
        try:
            vm2 = VirtualMachine.create(self.apiclient, services=self.services["virtual_machine"],
                                        templateid=self.template.id,
                                        zoneid=self.zone.id,
                                        accountid=self.account.name,
                                        domainid=self.account.domainid,
                                        serviceofferingid=self.compute_offering.id,
                                        networkids=ntwk2.id,
                                        hypervisor=self.hypervisor
                                        )
        except Exception as e:
            self.fail(e)
        finally:
            self.assertTrue(vm2 is not None, "VM failed to deploy")
            self.assertTrue(vm2.state == 'Running', "VM is not running")
        self.cleanup.append(vm2)
        self.debug("VM %s deployed in VPC %s" % (vm2.id, vpc2.id))

        # default config
        config = {
            'ike_enc'       :'aes128',
            'ike_hash'      :'sha1',
            'ike_dh'        :'modp1536',
            'esp_enc'       :'aes128',
            'esp_hash'      :'sha1',
            'esp_pfs'       :'modp1536',
            'psk'           :'secreatKey',
            'ike_life'      :86400,
            'esp_life'      :3600,
            'dpd'           :True,
            'force_encap'   :False,
            'passive_1'     :False,
            'passive_2'     :False
        }
        test_confs = [
            {}, # default
            {'force_encap': True},
            {'ike_life': ''},
            {'esp_life': ''},
            {'ike_life': '', 'esp_life': ''},
            {'passive_1': True, 'passive_2': True},
            {'passive_1': False, 'passive_2': True},
            {'passive_1': True, 'passive_2': False},
            {'passive_1': False, 'passive_2': False, 'dpd': False},
            {'passive_1': True, 'passive_2': True, 'dpd': False},
            {'passive_1': True, 'passive_2': False, 'dpd': False},
            {'passive_1': False, 'passive_2': True, 'dpd': False},
            {'passive_1': True, 'passive_2': False, 'esp_pfs': ''},
            {'ike_dh': 'modp3072', 'ike_hash': 'sha256', 'esp_pfs': 'modp2048', 'esp_hash':'sha384'},
            {'ike_dh': 'modp4096', 'ike_hash': 'sha384', 'esp_pfs': 'modp6144', 'esp_hash':'sha512'},
            {'ike_dh': 'modp8192', 'ike_hash': 'sha512', 'esp_pfs': 'modp8192', 'esp_hash':'sha384'}
        ]

        # 4) Enable Site-to-Site VPN for VPC
        vpn1_response = Vpn.createVpnGateway(self.apiclient, vpc1.id)
        self.assertTrue(
            vpn1_response is not None, "Failed to enable VPN Gateway 1")
        self.logger.debug("VPN gateway for VPC %s enabled" % vpc1.id)

        vpn2_response = Vpn.createVpnGateway(self.apiclient, vpc2.id)
        self.assertTrue(
            vpn2_response is not None, "Failed to enable VPN Gateway 2")
        self.logger.debug("VPN gateway for VPC %s enabled" % vpc2.id)

        # 5) Add VPN Customer gateway info
        src_nat_list = PublicIPAddress.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True,
            issourcenat=True,
            vpcid=vpc1.id
        )
        ip1 = src_nat_list[0]
        src_nat_list = PublicIPAddress.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True,
            issourcenat=True,
            vpcid=vpc2.id
        )
        ip2 = src_nat_list[0]

        # acquire an extra ip address to use to ssh into vm2
        try:
            vm2.public_ip = PublicIPAddress.create(
                apiclient=self.apiclient,
                accountid=self.account.name,
                zoneid=self.zone.id,
                domainid=self.account.domainid,
                services=self.services,
                networkid=ntwk2.id,
                vpcid=vpc2.id)
        except Exception as e:
            self.fail(e)
        finally:
            self.assertTrue(
                vm2.public_ip is not None, "Failed to aqcuire public ip for vm2")

        natrule = None
        # Create port forward to be able to ssh into vm2
        try:
            natrule = self._create_natrule(
                vpc2, vm2, 22, 22, vm2.public_ip, ntwk2)
        except Exception as e:
            self.fail(e)
        finally:
            self.assertTrue(
                natrule is not None, "Failed to create portforward for vm2")
            time.sleep(20)

        # setup ssh connection to vm2
        ssh_client = self._get_ssh_client(vm2, self.services, 10)
        if not ssh_client:
            self.fail("Failed to setup ssh connection to %s" % vm2.public_ip)

        for test_c in test_confs:
            c = config.copy()
            c.update(test_c)
            services = self._get_vpn_config(c)
            self.logger.debug(services)
            customer1_response = VpnCustomerGateway.create(
                self.apiclient,
                services,
                "Peer VPC1",
                ip1.ipaddress,
                vpc1.cidr,
                account=self.account.name,
                domainid=self.account.domainid)
            self.logger.debug("VPN customer gateway added for VPC %s enabled" % vpc1.id)

            customer2_response = VpnCustomerGateway.create(
                self.apiclient,
                services,
                "Peer VPC2",
                ip2.ipaddress,
                vpc2.cidr,
                account=self.account.name,
                domainid=self.account.domainid)
            self.logger.debug("VPN customer gateway added for VPC %s enabled" % vpc2.id)

            # 6) Connect two VPCs
            vpnconn1_response = Vpn.createVpnConnection(
                self.apiclient, customer1_response.id, vpn2_response['id'], c['passive_1'])
            self.logger.debug("VPN connection created for VPC %s" % vpc2.id)
            time.sleep(5)
            vpnconn2_response = Vpn.createVpnConnection(
                self.apiclient, customer2_response.id, vpn1_response['id'], c['passive_2'])
            self.logger.debug("VPN connection created for VPC %s" % vpc1.id)

            def checkVpnConnected():
                connections = Vpn.listVpnConnection(
                    self.apiclient,
                    listall='true',
                    vpcid=vpc2.id)
                if isinstance(connections, list):
                    return connections[0].state == 'Connected', None
                return False, None

            # Wait up to 60 seconds for passive connection to show up as Connected
            res, _ = wait_until(2, 30, checkVpnConnected)
            if not res:
                self.logger.debug("Failed to see VPN state as Connected, we'll attempt ssh+pinging")

            # run ping test
            packet_loss = ssh_client.execute("/bin/ping -c 3 -t 10 " + vm1.nic[0].ipaddress + " | grep packet | sed 's/.*received, //g' | sed 's/[% ]*packet.*//g'")[0]
            self.logger.debug("Packet loss %s" % packet_loss)
            self.assertTrue(int(packet_loss) < 50, "Ping did not succeed")

            # Cleanup
            Vpn.deleteVpnConnection(self.apiclient, vpnconn1_response['id'])
            Vpn.deleteVpnConnection(self.apiclient, vpnconn2_response['id'])
            customer1_response.delete(self.apiclient)
            customer2_response.delete(self.apiclient)

    def _get_vpn_config(self, c):
        ike_policy = '%s-%s;%s' % (c['ike_enc'], c['ike_hash'], c['ike_dh']) if c['ike_dh'] else '%s-%s' % (c['ike_enc'], c['ike_hash'])
        esp_policy = '%s-%s;%s' % (c['esp_enc'], c['esp_hash'], c['esp_pfs']) if c['esp_pfs'] else '%s-%s' % (c['esp_enc'], c['esp_hash'])
        out =  {
            'ipsecpsk': c['psk'],
            'ikepolicy':ike_policy,
            'esppolicy':esp_policy,
            'dpd':c['dpd'],
            'forceencap':c['force_encap']
        }
        if c['ike_life']:
            out['ikelifetime'] = c['ike_life']
        if c['esp_life']:
            out['esplifetime'] = c['esp_life']
        return out

    @classmethod
    def tearDownClass(cls):
        super(TestVPCSite2SiteVPNMultipleOptions, cls).tearDownClass()
