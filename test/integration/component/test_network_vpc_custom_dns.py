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
""" BVT test for custom DNS for Isolated network and VPC"""

#Import Local Modules
from marvin.codes import (PASS,
                          FAIL,
                          FAILED)
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (createGuestNetworkIpv6Prefix,
                                  listGuestNetworkIpv6Prefixes,
                                  deleteGuestNetworkIpv6Prefix)
from marvin.lib.utils import (get_process_status,
                              get_host_credentials)
from marvin.lib.base import (Configurations,
                             Domain,
                             NetworkOffering,
                             VpcOffering,
                             Account,
                             PublicIpRange,
                             Network,
                             VPC,
                             Router,
                             ServiceOffering,
                             VirtualMachine,
                             Host,
                             NetworkACLList,
                             NetworkACL)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_test_template)
from marvin.sshClient import SshClient
from marvin.cloudstackException import CloudstackAPIException
from marvin.lib.decoratorGenerators import skipTestIf

from nose.plugins.attrib import attr
from ipaddress import IPv6Network
from random import getrandbits
import time
import logging

IP6_OFFERING_CONFIG_NAME = "ipv6.offering.enabled"

IP4_DNS1 = "5.5.5.5"
IP4_DNS2 = "6.6.6.6"
IP6_DNS1 = "2001:4860:4860::5555"
IP6_DNS2 = "2001:4860:4860::6666"

VPC_DATA = {
    "cidr": "10.1.0.0/22",
    "tier1_gateway": "10.1.1.1",
    "tier2_gateway": "10.1.2.1",
    "tier_netmask": "255.255.255.0"
}

routerDetailsMap = {}

def getRouterProcessStatus(apiclient, hypervisor, config, router, cmd):
    if router.id not in routerDetailsMap or routerDetailsMap[router.id] is None:
        connect_ip = apiclient.connection.mgtSvr
        connect_user = apiclient.connection.user
        connect_passwd = apiclient.connection.passwd
        if hypervisor.lower() not in ('vmware', 'hyperv'):
            hosts = Host.list(
                apiclient,
                zoneid=router.zoneid,
                type='Routing',
                state='Up',
                id=router.hostid
            )
            if not isinstance(hosts, list):
                return [FAIL, "list host returns an invalid list"]
            host = hosts[0]
            connect_ip = host.ipaddress
            hypervisor = None
            try:
                connect_user, connect_passwd= get_host_credentials(
                    config, host.ipaddress)
            except KeyError:
                return [FAIL, "Marvin configuration has no host credentials to check router services"]
        details = {}
        details['connect_ip'] = connect_ip
        details['connect_user'] = connect_user
        details['connect_passwd'] = connect_passwd
        details['hypervisor'] = hypervisor
        routerDetailsMap[router.id] = details
    result = get_process_status(
        routerDetailsMap[router.id]['connect_ip'],
        22,
        routerDetailsMap[router.id]['connect_user'],
        routerDetailsMap[router.id]['connect_passwd'],
        router.linklocalip,
        cmd,
        hypervisor=routerDetailsMap[router.id]['hypervisor']
    )
    if type(result) != list or len(result) == 0:
        return [FAIL, "%s on router %s returned invalid result" % (cmd, router.id)]
    result = '\n'.join(result)
    return [PASS, result]

class TestNetworkCustomDns(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestNetworkCustomDns, cls).getClsTestClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.apiclient = testClient.getApiClient()
        cls.dbclient = testClient.getDbConnection()
        cls.test_ipv6_guestprefix = None
        cls.initial_ipv6_offering_enabled = None
        cls._cleanup = []
        routerDetailsMap = {}

        cls.logger = logging.getLogger('TestNetworkCustomDns')

        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.ipv6NotSupported = False

        ipv6_guestprefix = cls.getGuestIpv6Prefix()
        if ipv6_guestprefix == None:
            cls.ipv6NotSupported = True
        if cls.ipv6NotSupported == False:
            ipv6_publiciprange = cls.getPublicIpv6Range()
            if ipv6_publiciprange == None:
                cls.ipv6NotSupported = True

        if cls.ipv6NotSupported == False:
            cls.initial_ipv6_offering_enabled = Configurations.list(
                cls.apiclient,
                name=IP6_OFFERING_CONFIG_NAME)[0].value
            Configurations.update(cls.apiclient,
                IP6_OFFERING_CONFIG_NAME,
                "true")
        else:
            cls.debug("IPv6 is not supported, skipping testing IPv6 details!")
        cls.domain = get_domain(cls.apiclient)
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.template = get_test_template(
           cls.apiclient,
           cls.zone.id,
           cls.hypervisor)
        return

    @classmethod
    def tearDownClass(cls):
        if cls.initial_ipv6_offering_enabled != None:
            Configurations.update(cls.apiclient,
                IP6_OFFERING_CONFIG_NAME,
                cls.initial_ipv6_offering_enabled)
        super(TestNetworkCustomDns, cls).tearDownClass()
        if cls.test_ipv6_guestprefix != None:
            cmd = deleteGuestNetworkIpv6Prefix.deleteGuestNetworkIpv6PrefixCmd()
            cmd.id = cls.test_ipv6_guestprefix.id
            cls.apiclient.deleteGuestNetworkIpv6Prefix(cmd)

    @classmethod
    def getGuestIpv6Prefix(cls):
        cmd = listGuestNetworkIpv6Prefixes.listGuestNetworkIpv6PrefixesCmd()
        cmd.zoneid = cls.zone.id
        ipv6_prefixes_response = cls.apiclient.listGuestNetworkIpv6Prefixes(cmd)
        if isinstance(ipv6_prefixes_response, list) == True and len(ipv6_prefixes_response) > 0:
            return ipv6_prefixes_response[0]
        ipv6_guestprefix_service = cls.services["guestip6prefix"]
        cmd = createGuestNetworkIpv6Prefix.createGuestNetworkIpv6PrefixCmd()
        cmd.zoneid = cls.zone.id
        cmd.prefix = ipv6_guestprefix_service["prefix"]
        ipv6_guestprefix = cls.apiclient.createGuestNetworkIpv6Prefix(cmd)
        cls.test_ipv6_guestprefix = ipv6_guestprefix
        return ipv6_guestprefix

    @classmethod
    def getPublicIpv6Range(cls):
        list_public_ip_range_response = PublicIpRange.list(
            cls.apiclient,
            zoneid=cls.zone.id
        )
        ipv4_range_vlan = None
        if isinstance(list_public_ip_range_response, list) == True and len(list_public_ip_range_response) > 0:
            for ip_range in list_public_ip_range_response:
                if ip_range.ip6cidr != None and ip_range.ip6gateway != None:
                    return ip_range
                if ip_range.netmask != None and ip_range.gateway != None:
                    vlan = ip_range.vlan
                    if ipv4_range_vlan == None and vlan.startswith("vlan://"):
                        vlan = vlan.replace("vlan://", "")
                        if vlan == "untagged":
                            ipv4_range_vlan = None
                        else:
                            ipv4_range_vlan = int(vlan)
        ipv6_publiciprange_service = cls.services["publicip6range"]
        ipv6_publiciprange_service["zoneid"] = cls.zone.id
        ipv6_publiciprange_service["vlan"] = ipv4_range_vlan
        ipv6_publiciprange = PublicIpRange.create(
            cls.apiclient,
            ipv6_publiciprange_service
        )
        cls._cleanup.append(ipv6_publiciprange)
        return ipv6_publiciprange

    def setUp(self):
        self.services = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestNetworkCustomDns, self).tearDown()

    def createTinyServiceOffering(self):
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["big"],
        )
        self.cleanup.append(self.service_offering)

    def createNetworkOffering(self):
        off_service = self.services["network_offering"]
        if not self.ipv6NotSupported:
            off_service["internetprotocol"] = "dualstack"
        self.network_offering = NetworkOffering.create(
            self.apiclient,
            off_service
        )
        self.cleanup.append(self.network_offering)
        self.network_offering.update(self.apiclient, state='Enabled')


    def deployNetwork(self):
        network_service = self.services["network"]
        network_service["networkoffering"] = self.network_offering.id
        network_service["dns1"] = IP4_DNS1
        network_service["dns2"] = IP4_DNS2
        if not self.ipv6NotSupported:
            network_service["ip6dns1"] = IP6_DNS1
            network_service["ip6dns2"] = IP6_DNS2
        self.network = Network.create(
            self.apiclient,
            self.services["network"],
            self.account.name,
            self.account.domainid,
            zoneid=self.zone.id
        )
        self.cleanup.append(self.network)

    def deployNetworkVm(self):
        if self.template == FAILED:
            assert False, "get_test_template() failed to return template"
        self.services["virtual_machine"]["zoneid"] = self.zone.id
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=self.network.id,
            serviceofferingid=self.service_offering.id
        )
        self.cleanup.append(self.virtual_machine)

    def getNetworkRouter(self, network, red_state="PRIMARY"):
        routers = Router.list(
            self.apiclient,
            networkid=network.id,
            listall=True
        )
        self.assertTrue(
            isinstance(routers, list) and len(routers) > 0,
            "No routers found for network %s" % network.id
        )
        if len(routers) == 1:
            return routers[0]
        for router in routers:
            if router.redundantstate == red_state:
                return router

    def checkNetwork(self):
        self.debug("Listing network: %s" % (self.network.name))
        network = Network.list(self.apiclient,listall="true",id=self.network.id)
        self.assertTrue(
            isinstance(network, list),
            "Check listNetworks response returns a valid list"
        )
        self.assertEqual(
            len(network),
            1,
            "Network not found"
        )
        network = network[0]
        self.assertNotEqual(network,
                    None,
                    "User is not able to retrieve network details %s" % self.network.id)
        self.assertEqual(network.dns1,
                    IP4_DNS1,
                    "IPv4 DNS1 is not same, expected=%s, actual=%s" % (IP4_DNS1, network.dns1))
        self.assertEqual(network.dns2,
                    IP4_DNS2,
                    "IPv4 DNS2 is not same, expected=%s, actual=%s" % (IP4_DNS2, network.dns2))
        if not self.ipv6NotSupported:
            self.assertEqual(network.ip6dns1,
                        IP6_DNS1,
                        "IPv6 DNS1 is not same, expected=%s, actual=%s" % (IP6_DNS1, network.ip6dns1))
            self.assertEqual(network.ip6dns2,
                        IP6_DNS2,
                        "IPv6 DNS2 is not same, expected=%s, actual=%s" % (IP6_DNS2, network.ip6dns2))

    def checkNetworkRouter(self):
        router = self.getNetworkRouter(self.network)
        dns_cmd = "cat /etc/resolv.conf"
        res = getRouterProcessStatus(self.apiclient, self.hypervisor, self.config, router, dns_cmd)
        if res[0] == FAIL:
            self.fail("Failed to get router command result. %s" % res[1])
        res = res[1]
        ns = "nameserver %s" % IP4_DNS1
        self.assertTrue(ns in res,
            "Network router doesn't contain nameserver for DNS1: %s" % IP4_DNS1)
        ns = "nameserver %s" % IP4_DNS2
        self.assertTrue(ns in res,
            "Network router doesn't contain nameserver for DNS2: %s" % IP4_DNS2)
        if not self.ipv6NotSupported:
            ns = "nameserver %s" % IP6_DNS1
            self.assertTrue(ns in res,
                "Network router doesn't contain nameserver for IPv6 DNS1: %s" % IP6_DNS1)
            ns = "nameserver %s" % IP6_DNS2
            self.assertTrue(ns in res,
                "Network router doesn't contain nameserver for IPv6 DNS2: %s" % IP6_DNS2)

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns",
            "smoke"],
        required_hardware="false")
    def test_01_verify_network_dns(self):
        """Test to verify custom DNS for network

        # Validate the following:
        # 1. Create network, deploy VM
        # 2. Verify network has required DNS details
        # 3. SSH into VR for the network and verify it has required DNS details as nameserver
        """

        self.createNetworkOffering()
        self.createTinyServiceOffering()
        self.deployNetwork()
        self.deployNetworkVm()
        self.checkNetwork()
        self.checkNetworkRouter()

class TestVpcCustomDns(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestVpcCustomDns, cls).getClsTestClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.apiclient = testClient.getApiClient()
        cls.dbclient = testClient.getDbConnection()
        cls.test_ipv6_guestprefix = None
        cls.initial_ipv6_offering_enabled = None
        cls._cleanup = []
        routerDetailsMap = {}
        cls.vpcAllowAllAclDetailsMap = {}

        cls.logger = logging.getLogger('TestVpcCustomDns')

        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.ipv6NotSupported = False

        ipv6_guestprefix = cls.getGuestIpv6Prefix()
        if ipv6_guestprefix == None:
            cls.ipv6NotSupported = True
        if cls.ipv6NotSupported == False:
            ipv6_publiciprange = cls.getPublicIpv6Range()
            if ipv6_publiciprange == None:
                cls.ipv6NotSupported = True

        if cls.ipv6NotSupported == False:
            cls.initial_ipv6_offering_enabled = Configurations.list(
                cls.apiclient,
                name=IP6_OFFERING_CONFIG_NAME)[0].value
            Configurations.update(cls.apiclient,
                IP6_OFFERING_CONFIG_NAME,
                "true")
        else:
            cls.debug("IPv6 is not supported, skipping testing IPv6 details!")
        cls.domain = get_domain(cls.apiclient)
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.template = get_test_template(
           cls.apiclient,
           cls.zone.id,
               cls.hypervisor)
        return

    @classmethod
    def tearDownClass(cls):
        if cls.initial_ipv6_offering_enabled != None:
            Configurations.update(cls.apiclient,
                IP6_OFFERING_CONFIG_NAME,
                cls.initial_ipv6_offering_enabled)
        super(TestVpcCustomDns, cls).tearDownClass()
        if cls.test_ipv6_guestprefix != None:
            cmd = deleteGuestNetworkIpv6Prefix.deleteGuestNetworkIpv6PrefixCmd()
            cmd.id = cls.test_ipv6_guestprefix.id
            cls.apiclient.deleteGuestNetworkIpv6Prefix(cmd)

    @classmethod
    def getGuestIpv6Prefix(cls):
        cmd = listGuestNetworkIpv6Prefixes.listGuestNetworkIpv6PrefixesCmd()
        cmd.zoneid = cls.zone.id
        ipv6_prefixes_response = cls.apiclient.listGuestNetworkIpv6Prefixes(cmd)
        if isinstance(ipv6_prefixes_response, list) == True and len(ipv6_prefixes_response) > 0:
            return ipv6_prefixes_response[0]
        ipv6_guestprefix_service = cls.services["guestip6prefix"]
        cmd = createGuestNetworkIpv6Prefix.createGuestNetworkIpv6PrefixCmd()
        cmd.zoneid = cls.zone.id
        cmd.prefix = ipv6_guestprefix_service["prefix"]
        ipv6_guestprefix = cls.apiclient.createGuestNetworkIpv6Prefix(cmd)
        cls.test_ipv6_guestprefix = ipv6_guestprefix
        return ipv6_guestprefix

    @classmethod
    def getPublicIpv6Range(cls):
        list_public_ip_range_response = PublicIpRange.list(
            cls.apiclient,
            zoneid=cls.zone.id
        )
        ipv4_range_vlan = None
        if isinstance(list_public_ip_range_response, list) == True and len(list_public_ip_range_response) > 0:
            for ip_range in list_public_ip_range_response:
                if ip_range.ip6cidr != None and ip_range.ip6gateway != None:
                    return ip_range
                if ip_range.netmask != None and ip_range.gateway != None:
                    vlan = ip_range.vlan
                    if ipv4_range_vlan == None and vlan.startswith("vlan://"):
                        vlan = vlan.replace("vlan://", "")
                        if vlan == "untagged":
                            ipv4_range_vlan = None
                        else:
                            ipv4_range_vlan = int(vlan)
        ipv6_publiciprange_service = cls.services["publicip6range"]
        ipv6_publiciprange_service["zoneid"] = cls.zone.id
        ipv6_publiciprange_service["vlan"] = ipv4_range_vlan
        ipv6_publiciprange = PublicIpRange.create(
            cls.apiclient,
            ipv6_publiciprange_service
        )
        cls._cleanup.append(ipv6_publiciprange)
        return ipv6_publiciprange

    def setUp(self):
        self.services = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestVpcCustomDns, self).tearDown()

    def createTinyServiceOffering(self):
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["big"],
        )
        self.cleanup.append(self.service_offering)

    def createVpcOfferingInternal(self, is_redundant, is_ipv6):
        off_service = self.services["vpc_offering"]
        if is_redundant:
            off_service["serviceCapabilityList"] = {
                "SourceNat": {
                    "RedundantRouter": 'true'
                },
            }
        if is_ipv6:
            off_service["internetprotocol"] = "dualstack"
        vpc_offering = VpcOffering.create(
            self.apiclient,
            off_service
        )
        self.cleanup.append(vpc_offering)
        vpc_offering.update(self.apiclient, state='Enabled')
        return vpc_offering

    def createVpcOffering(self, is_redundant=False):
        self.vpc_offering = self.createVpcOfferingInternal(is_redundant, self.ipv6NotSupported == False)

    def deployAllowAllVpcInternal(self, cidr):
        service = self.services["vpc"]
        service["cidr"] = cidr
        ip6Dns1 = None
        ip6Dns2 = None
        if not self.ipv6NotSupported:
            ip6Dns1 = IP6_DNS1
            ip6Dns2 = IP6_DNS2

        vpc = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_offering.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            dns1=IP4_DNS1,
            dns2=IP4_DNS2,
            ip6dns1=ip6Dns1,
            ip6dns2=ip6Dns2
        )
        self.cleanup.append(vpc)
        acl = NetworkACLList.create(
            self.apiclient,
            services={},
            name="allowall",
            description="allowall",
            vpcid=vpc.id
        )
        rule ={
            "protocol": "all",
            "traffictype": "ingress",
        }
        NetworkACL.create(self.apiclient,
            services=rule,
            aclid=acl.id
        )
        rule["traffictype"] = "egress"
        NetworkACL.create(self.apiclient,
            services=rule,
            aclid=acl.id
        )
        self.vpcAllowAllAclDetailsMap[vpc.id] = acl.id
        return vpc

    def deployVpc(self):
        self.vpc = self.deployAllowAllVpcInternal(VPC_DATA["cidr"])

    def createNetworkTierOfferingInternal(self, is_ipv6, remove_lb=True):
        off_service = self.services["nw_offering_isolated_vpc"]
        if not remove_lb: # Remove Lb service
            if "serviceProviderList" in off_service and "Lb" in off_service["serviceProviderList"].keys():
                providers = off_service["serviceProviderList"]
                providers.pop("Lb")
                off_service["serviceProviderList"] = providers
            if "supportedservices" in off_service and "Lb" in off_service["supportedservices"]:
                supportedServices = off_service["supportedservices"].split(",")
                supportedServices.remove("Lb")
                off_service["supportedservices"] = ",".join(supportedServices)
        if is_ipv6:
            off_service["internetprotocol"] = "dualstack"
        network_offering = NetworkOffering.create(
            self.apiclient,
            off_service,
            conservemode=False
        )
        self.cleanup.append(network_offering)
        network_offering.update(self.apiclient, state='Enabled')
        return network_offering

    def createNetworkTierOffering(self):
        self.network_offering = self.createNetworkTierOfferingInternal(self.ipv6NotSupported == False)

    def deployNetworkTierInternal(self, network_offering_id, vpc_id, tier_gateway, tier_netmask, acl_id=None, tier_name=None):
        if not acl_id and vpc_id in self.vpcAllowAllAclDetailsMap:
            acl_id = self.vpcAllowAllAclDetailsMap[vpc_id]
        service = self.services["ntwk"]
        if tier_name:
            service["name"] = tier_name
            service["displaytext"] = "vpc-%s" % tier_name
        network = Network.create(
            self.apiclient,
            service,
            self.account.name,
            self.account.domainid,
            networkofferingid=network_offering_id,
            vpcid=vpc_id,
            zoneid=self.zone.id,
            gateway=tier_gateway,
            netmask=tier_netmask,
            aclid=acl_id
        )
        self.cleanup.append(network)
        return network

    def deployNetworkTier(self):
        self.network = self.deployNetworkTierInternal(
            self.network_offering.id,
            self.vpc.id,
            VPC_DATA["tier1_gateway"],
            VPC_DATA["tier_netmask"]
        )

    def deployNetworkTierVmInternal(self, network):
        if self.template == FAILED:
            assert False, "get_test_template() failed to return template"
        self.services["virtual_machine"]["zoneid"] = self.zone.id
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=network,
            serviceofferingid=self.service_offering.id
        )
        self.cleanup.append(virtual_machine)
        return virtual_machine

    def deployNetworkTierVm(self):
        self.virtual_machine = self.deployNetworkTierVmInternal(self.network.id)

    def checkVpcBasic(self):
        self.debug("Listing VPC: %s" % (self.vpc.name))
        vpc = VPC.list(self.apiclient,listall="true",id=self.vpc.id)
        self.assertTrue(
            isinstance(vpc, list),
            "Check listVpcs response returns a valid list"
        )
        self.assertEqual(
            len(vpc),
            1,
            "Network not found"
        )
        vpc = vpc[0]
        self.assertEqual(vpc.dns1,
                    IP4_DNS1,
                    "IPv4 DNS1 is not same, expected=%s, actual=%s" % (IP4_DNS1, vpc.dns1))
        self.assertEqual(vpc.dns2,
                    IP4_DNS2,
                    "IPv4 DNS2 is not same, expected=%s, actual=%s" % (IP4_DNS2, vpc.dns2))
        if not self.ipv6NotSupported:
            self.assertEqual(vpc.ip6dns1,
                        IP6_DNS1,
                        "IPv6 DNS1 is not same, expected=%s, actual=%s" % (IP6_DNS1, vpc.ip6dns1))
            self.assertEqual(vpc.ip6dns2,
                        IP6_DNS2,
                        "IPv6 DNS2 is not same, expected=%s, actual=%s" % (IP6_DNS2, vpc.ip6dns2))

    def getVpcRouter(self, vpc, red_state="PRIMARY"):
        routers = Router.list(
            self.apiclient,
            vpcid=vpc.id,
            listall=True
        )
        self.assertTrue(
            isinstance(routers, list) and len(routers) > 0,
            "No routers found for VPC %s" % vpc.id
        )
        if len(routers) == 1:
            return routers[0]
        for router in routers:
            if router.redundantstate == red_state:
                return router

    def checkVpcRouter(self):
        router = self.getVpcRouter(self.vpc)
        dns_cmd = "cat /etc/resolv.conf"
        res = getRouterProcessStatus(self.apiclient, self.hypervisor, self.config, router, dns_cmd)
        if res[0] == FAIL:
            self.fail("Failed to get router command result. %s" % res[1])
        res = res[1]
        ns = "nameserver %s" % IP4_DNS1
        self.assertTrue(ns in res,
            "Network router doesn't contain nameserver for DNS1: %s" % IP4_DNS1)
        ns = "nameserver %s" % IP4_DNS2
        self.assertTrue(ns in res,
            "Network router doesn't contain nameserver for DNS2: %s" % IP4_DNS2)
        if not self.ipv6NotSupported:
            ns = "nameserver %s" % IP6_DNS1
            self.assertTrue(ns in res,
                "Network router doesn't contain nameserver for IPv6 DNS1: %s" % IP6_DNS1)
            ns = "nameserver %s" % IP6_DNS2
            self.assertTrue(ns in res,
                "Network router doesn't contain nameserver for IPv6 DNS2: %s" % IP6_DNS2)

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns",
            "smoke"],
        required_hardware="false")
    def test_01_verify_vpc_dns(self):
        """Test to verify custom DNS for VPC
        # Validate the following:
        # 1. Create VPC, deploy network tier and VM in it
        # 2. Verify VPC details
        # 3. Verify VPC router details
        """

        self.createVpcOffering()
        self.createNetworkTierOffering()
        self.createTinyServiceOffering()
        self.deployVpc()
        self.deployNetworkTier()
        self.deployNetworkTierVm()
        self.checkVpcBasic()
        self.checkVpcRouter()
