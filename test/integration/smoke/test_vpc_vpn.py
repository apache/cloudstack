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
            }
        }


class TestVpcVpn(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.apiclient = super(TestVpcVpn, cls).getClsTestClient().getApiClient()
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
    def test_vpc_vpn(self):
        """Test VPN in VPC"""

        # 0) Get the default network offering for VPC
        networkOffering = NetworkOffering.list(self.apiclient, name="DefaultIsolatedNetworkOfferingForVpcNetworks")
        self.assert_(networkOffering is not None and len(networkOffering) > 0, "No VPC based network offering")

        # 1) Create VPC
        vpcOffering = VpcOffering.list(self.apiclient,isdefault=True)
        self.assert_(vpcOffering is not None and len(vpcOffering)>0, "No VPC offerings found")
        self.services["vpc"] = {}
        self.services["vpc"]["name"] = "vpc-vpn"
        self.services["vpc"]["displaytext"] = "vpc-vpn"
        self.services["vpc"]["cidr"] = "10.1.1.0/24"
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

        # 2) Create network in VPC
        self.services["vpcnetwork"] = {}
        self.services["vpcnetwork"]["name"] = "vpcntwk"
        self.services["vpcnetwork"]["displaytext"] = "vpcntwk"
        ntwk = Network.create(
            apiclient=self.apiclient,
            services=self.services["vpcnetwork"],
            accountid=self.account.name,
            domainid=self.domain.id,
            networkofferingid=networkOffering[0].id,
            zoneid=self.zone.id,
            vpcid=vpc.id,
            gateway="10.1.1.1",
            netmask="255.255.255.192"
        )
        self.assertIsNotNone(ntwk, "Network failed to create")
        self.debug("Network %s created in VPC %s" %(ntwk.id, vpc.id))

        # 3) Deploy a vm
        self.services["virtual_machine"]["networkids"] = ntwk.id
        vm = VirtualMachine.create(self.apiclient, services=self.services["virtual_machine"],
            templateid=self.template.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid= self.domain.id,
            serviceofferingid=self.service_offering.id,
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
