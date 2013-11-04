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
#Import Local Modules
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from nose.plugins.attrib import attr

import time

class Services:
    def __init__(self):
        self.services = {
            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                "password": "password",
            },
            "virtual_machine": {
                "displayname": "Test VM",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'XenServer',
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },
            "ostype": 'CentOS 5.3 (64-bit)',
            "service_offering": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 256,
            },
            "network_offering": {
                "name": "Network offering for internal vpc",
                "displaytext": "Network offering for internal vpc",
                "guestiptype": "Isolated",
                "traffictype": "Guest",
                "supportedservices": "Vpn,Dhcp,Dns,Lb,UserData,SourceNat,StaticNat,PortForwarding,NetworkACL",
                "serviceProviderList": {
                    "Dhcp": "VpcVirtualRouter",
                    "Dns": "VpcVirtualRouter",
                    "Vpn": "VpcVirtualRouter",
                    "UserData": "VpcVirtualRouter",
                    "Lb": "InternalLbVM",
                    "SourceNat": "VpcVirtualRouter",
                    "StaticNat": "VpcVirtualRouter",
                    "PortForwarding": "VpcVirtualRouter",
                    "NetworkACL": "VpcVirtualRouter",
                },
                "serviceCapabilityList": {
                    "SourceNat": {"SupportedSourceNatTypes": "peraccount"},
                    "Lb": {"lbSchemes": "internal", "SupportedLbIsolation": "dedicated"}
                }
            },
            "vpn_user": {
                "username": "test",
                "password": "password",
            },
            "vpc": {
                "name": "vpc_vpn",
                "displaytext": "vpc-vpn",
                "cidr": "10.1.1.0/24"
            },
            "ntwk": {
                "name": "tier1",
                "displaytext": "vpc-tier1",
                "gateway" : "10.1.1.1",
                "netmask" : "255.255.255.192"
            },
            "vpc2": {
                "name": "vpc2_vpn",
                "displaytext": "vpc2-vpn",
                "cidr": "10.2.1.0/24"
            },
            "ntwk2": {
                "name": "tier2",
                "displaytext": "vpc-tier2",
                "gateway" : "10.2.1.1",
                "netmask" : "255.255.255.192"
            }
        }


class TestVpcRemoteAccessVpn(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.apiclient = super(TestVpcRemoteAccessVpn, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        cls.zone = get_zone(cls.apiclient, cls.services)
        cls.domain = get_domain(cls.apiclient)
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offering"]
        )
        cls.account = Account.create(cls.apiclient, services=cls.services["account"])
        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.cleanup = [cls.account]

    @attr(tags=["advanced"])
    def test_vpc_remote_access_vpn(self):
        """Test VPN in VPC"""

        # 0) Get the default network offering for VPC
        networkOffering = NetworkOffering.list(self.apiclient, name="DefaultIsolatedNetworkOfferingForVpcNetworks")
        self.assert_(networkOffering is not None and len(networkOffering) > 0, "No VPC based network offering")

        # 1) Create VPC
        vpcOffering = VpcOffering.list(self.apiclient,isdefault=True)
        self.assert_(vpcOffering is not None and len(vpcOffering)>0, "No VPC offerings found")
        vpc = VPC.create(
                apiclient=self.apiclient,
                services=self.services["vpc"],
                networkDomain="vpc.vpn",
                vpcofferingid=vpcOffering[0].id,
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.domain.id
        )
        self.assert_(vpc is not None, "VPC creation failed")
        self.debug("VPC %s created" %(vpc.id))

        # 2) Create network in VPC
        ntwk = Network.create(
            apiclient=self.apiclient,
            services=self.services["ntwk"],
            accountid=self.account.name,
            domainid=self.domain.id,
            networkofferingid=networkOffering[0].id,
            zoneid=self.zone.id,
            vpcid=vpc.id
        )
        self.assertIsNotNone(ntwk, "Network failed to create")
        self.debug("Network %s created in VPC %s" %(ntwk.id, vpc.id))

        # 3) Deploy a vm
        vm = VirtualMachine.create(self.apiclient, services=self.services["virtual_machine"],
            templateid=self.template.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid= self.domain.id,
            serviceofferingid=self.service_offering.id,
            networkids=ntwk.id
        )
        self.assert_(vm is not None, "VM failed to deploy")
        self.assert_(vm.state == 'Running', "VM is not running")
        self.debug("VM %s deployed in VPC %s" %(vm.id, vpc.id))

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
        vpn = Vpn.create(self.apiclient,
                         publicipid=ip.id,
                         account=self.account.name,
                         domainid=self.account.domainid)

        # 5) Add VPN user for VPC
        vpnUser = VpnUser.create(self.apiclient,
                                 account=self.account.name,
                                 domainid=self.account.domainid,
                                 username=self.services["vpn_user"]["username"],
                                 password=self.services["vpn_user"]["password"])

        # 6) Disable VPN for VPC
        vpn.delete(self.apiclient)

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls.cleanup)
        except Exception, e:
            raise Exception("Cleanup failed with %s" % e)

class TestVpcSite2SiteVpn(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.apiclient = super(TestVpcSite2SiteVpn, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        cls.zone = get_zone(cls.apiclient, cls.services)
        cls.domain = get_domain(cls.apiclient)
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offering"]
        )
        cls.account = Account.create(cls.apiclient, services=cls.services["account"])
        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.cleanup = [cls.account]

    @attr(tags=["advanced"])
    def test_vpc_site2site_vpn(self):
        """Test VPN in VPC"""

        # 0) Get the default network offering for VPC
        networkOffering = NetworkOffering.list(self.apiclient, name="DefaultIsolatedNetworkOfferingForVpcNetworks")
        self.assert_(networkOffering is not None and len(networkOffering) > 0, "No VPC based network offering")

        # 1) Create VPC
        vpcOffering = VpcOffering.list(self.apiclient,isdefault=True)
        self.assert_(vpcOffering is not None and len(vpcOffering)>0, "No VPC offerings found")

        vpc1 = VPC.create(
                apiclient=self.apiclient,
                services=self.services["vpc"],
                networkDomain="vpc1.vpn",
                vpcofferingid=vpcOffering[0].id,
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.domain.id
        )
        self.assert_(vpc1 is not None, "VPC creation failed")
        self.debug("VPC1 %s created" %(vpc1.id))

        vpc2 = VPC.create(
                apiclient=self.apiclient,
                services=self.services["vpc2"],
                networkDomain="vpc2.vpn",
                vpcofferingid=vpcOffering[0].id,
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.domain.id
        )
        self.assert_(vpc2 is not None, "VPC2 creation failed")
        self.debug("VPC2 %s created" %(vpc1.id))

        # 2) Create network in VPC
        ntwk1 = Network.create(
            apiclient=self.apiclient,
            services=self.services["ntwk"],
            accountid=self.account.name,
            domainid=self.domain.id,
            networkofferingid=networkOffering[0].id,
            zoneid=self.zone.id,
            vpcid=vpc1.id
        )
        self.assertIsNotNone(ntwk1, "Network failed to create")
        self.debug("Network %s created in VPC %s" %(ntwk1.id, vpc1.id))

        ntwk2 = Network.create(
            apiclient=self.apiclient,
            services=self.services["ntwk2"],
            accountid=self.account.name,
            domainid=self.domain.id,
            networkofferingid=networkOffering[0].id,
            zoneid=self.zone.id,
            vpcid=vpc2.id
        )
        self.assertIsNotNone(ntwk2, "Network failed to create")
        self.debug("Network %s created in VPC %s" %(ntwk2.id, vpc2.id))

        # 3) Deploy a vm
        vm1 = VirtualMachine.create(self.apiclient, services=self.services["virtual_machine"],
            templateid=self.template.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid= self.domain.id,
            serviceofferingid=self.service_offering.id,
            networkids=ntwk1.id
        )
        self.assert_(vm1 is not None, "VM failed to deploy")
        self.assert_(vm1.state == 'Running', "VM is not running")
        self.debug("VM %s deployed in VPC %s" %(vm1.id, vpc1.id))

        vm2 = VirtualMachine.create(self.apiclient, services=self.services["virtual_machine"],
            templateid=self.template.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid= self.domain.id,
            serviceofferingid=self.service_offering.id,
            networkids=ntwk2.id
        )
        self.assert_(vm2 is not None, "VM failed to deploy")
        self.assert_(vm2.state == 'Running', "VM is not running")
        self.debug("VM %s deployed in VPC %s" %(vm2.id, vpc2.id))

        # 4) Enable Site-to-Site VPN for VPC
        cmd=createVpnGateway.createVpnGatewayCmd()
        cmd.vpcid=vpc1.id
        vpn1_response = self.apiclient.createVpnGateway(cmd)

        self.debug("VPN gateway for VPC %s enabled" % (vpc1.id))

        cmd=createVpnGateway.createVpnGatewayCmd()
        cmd.vpcid=vpc2.id
        vpn2_response = self.apiclient.createVpnGateway(cmd)

        self.debug("VPN gateway for VPC %s enabled" %(vpc2.id))

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

        cmd=createVpnCustomerGateway.createVpnCustomerGatewayCmd()
        cmd.esppolicy="3des-md5;modp1536"
        cmd.ikepolicy="3des-md5;modp1536"
        cmd.domainid=self.account.domainid
        cmd.account=self.account.name
        cmd.ipsecpsk="ipsecpsk"

        cmd.name="Peer VPC1"
        cmd.gateway=ip1.ipaddress
        cmd.cidrlist=vpc1.cidr
        customer1_response = self.apiclient.createVpnCustomerGateway(cmd)
        self.debug("VPN customer gateway added for VPC %s enabled" %(vpc1.id))

        cmd.name="Peer VPC2"
        cmd.gateway=ip2.ipaddress
        cmd.cidrlist=vpc2.cidr
        customer2_response = self.apiclient.createVpnCustomerGateway(cmd)
        self.debug("VPN customer gateway added for VPC %s enabled" %(vpc2.id))

        # 6) Connect two VPCs 
        cmd = createVpnConnection.createVpnConnectionCmd()
        cmd.s2svpngatewayid = vpn2_response.id
        cmd.s2scustomergatewayid = customer1_response.id
        cmd.passive="true"
        vpnconn1_response = self.apiclient.createVpnConnection(cmd)
        self.debug("VPN passive connection created for VPC %s" %(vpc2.id))

        cmd = createVpnConnection.createVpnConnectionCmd()
        cmd.s2svpngatewayid = vpn1_response.id
        cmd.s2scustomergatewayid = customer2_response.id
        vpnconn2_response = self.apiclient.createVpnConnection(cmd)
        self.debug("VPN connection created for VPC %s" %(vpc1.id))

        self.assertEqual(vpnconn2_response.state, "Connected", "Failed to connect between VPCs!")

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls.cleanup)
        except Exception, e:
            raise Exception("Cleanup failed with %s" % e)
