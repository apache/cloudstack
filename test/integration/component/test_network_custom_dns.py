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
""" BVT test for custom DNS for Network"""

#Import Local Modules
from marvin.codes import FAILED
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (createGuestNetworkIpv6Prefix,
                                  listGuestNetworkIpv6Prefixes,
                                  deleteGuestNetworkIpv6Prefix)
from marvin.lib.utils import (cleanup_resources,
                              get_process_status,
                              get_host_credentials)
from marvin.lib.base import (Configurations,
                             Domain,
                             NetworkOffering,
                             Account,
                             PublicIpRange,
                             Network,
                             Router,
                             ServiceOffering,
                             VirtualMachine,
                             Host)
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
import threading

ipv6_offering_config_name = "ipv6.offering.enabled"

IP4_DNS1 = "5.5.5.5"
IP4_DNS2 = "6.6.6.6"
IP6_DNS1 = "2001:4860:4860::5555"
IP6_DNS2 = "2001:4860:4860::6666"

class TestNetworkCustomDns(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestNetworkCustomDns, cls).getClsTestClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.apiclient = testClient.getApiClient()
        cls.dbclient = testClient.getDbConnection()
        cls._cleanup = []
        cls.routerDetailsMap = {}

        cls.logger = logging.getLogger('TestNetworkCustomDns')

        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
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

    @classmethod
    def tearDownClass(cls):
        super(TestNetworkCustomDns, cls).tearDownClass()

    def setUp(self):
        self.services = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.thread = None
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, reversed(self.cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def createTinyServiceOffering(self):
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["big"],
        )
        self.cleanup.append(self.service_offering)

    def createNetworkOffering(self):
        off_service = self.services["network_offering"]
        self.network_offering = NetworkOffering.create(
            self.apiclient,
            off_service
        )
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)


    def deployNetwork(self):
        network_service = self.services["network"]
        network_service["networkoffering"] = self.network_offering.id
        network_service["dns1"] = IP4_DNS1
        network_service["dns2"] = IP4_DNS2
        self.network = Network.create(
            self.apiclient,
            network_service,
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

    def getRouterProcessStatus(self, router, cmd):
        if router.id not in self.routerDetailsMap or self.routerDetailsMap[router.id] is None:
            connect_ip = self.apiclient.connection.mgtSvr
            connect_user = self.apiclient.connection.user
            connect_passwd = self.apiclient.connection.passwd
            hypervisor = self.hypervisor
            if self.hypervisor.lower() not in ('vmware', 'hyperv'):
                hosts = Host.list(
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
                connect_ip = host.ipaddress
                hypervisor = None
                try:
                    connect_user, connect_passwd= get_host_credentials(
                        self.config, host.ipaddress)
                except KeyError:
                    self.skipTest(
                        "Marvin configuration has no host credentials to\
                                check router services")
            details = {}
            details['connect_ip'] = connect_ip
            details['connect_user'] = connect_user
            details['connect_passwd'] = connect_passwd
            details['hypervisor'] = hypervisor
            self.routerDetailsMap[router.id] = details
        result = get_process_status(
            self.routerDetailsMap[router.id]['connect_ip'],
            22,
            self.routerDetailsMap[router.id]['connect_user'],
            self.routerDetailsMap[router.id]['connect_passwd'],
            router.linklocalip,
            cmd,
            hypervisor=self.routerDetailsMap[router.id]['hypervisor']
        )
        self.assertTrue(type(result) == list and len(result) > 0,
            "%s on router %s returned invalid result" % (cmd, router.id))
        result = '\n'.join(result)
        return result

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
                    "IP4_DNS1 is not same, expected=%s, actual=%s" % (IP4_DNS1, network.dns1))
        self.assertEqual(network.dns2,
                    IP4_DNS2,
                    "IP4_DNS2 is not same, expected=%s, actual=%s" % (IP4_DNS2, network.dns2))

    def checkNetworkRouter(self):
        router = self.getNetworkRouter(self.network)
        dns_cmd = "cat /etc/resolv.conf"
        res = self.getRouterProcessStatus(router, dns_cmd)
        ns = "nameserver %s" % IP4_DNS1
        self.assertTrue(ns in res,
            "Network router doesn't contain namserver for DNS1: %s" % IP4_DNS1)
        ns = "nameserver %s" % IP4_DNS2
        self.assertTrue(ns in res,
            "Network router doesn't contain namserver for DNS2: %s" % IP4_DNS2)

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns",
            "smoke"],
        required_hardware="false")
    def test_01_verify_network(self):
        """Test to verify custom IPv4 DNS for network

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

class TestIpv6NetworkCustomDns(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestIpv6NetworkCustomDns, cls).getClsTestClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.apiclient = testClient.getApiClient()
        cls.dbclient = testClient.getDbConnection()
        cls.test_ipv6_guestprefix = None
        cls.initial_ipv6_offering_enabled = None
        cls._cleanup = []
        cls.routerDetailsMap = {}

        cls.logger = logging.getLogger('TestIpv6NetworkCustomDns')

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
                name=ipv6_offering_config_name)[0].value
            Configurations.update(cls.apiclient,
                ipv6_offering_config_name,
                "true")
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
        else:
            cls.debug("IPv6 is not supported, skipping tests!")
        return

    @classmethod
    def tearDownClass(cls):
        if cls.initial_ipv6_offering_enabled != None:
            Configurations.update(cls.apiclient,
                ipv6_offering_config_name,
                cls.initial_ipv6_offering_enabled)
        super(TestIpv6NetworkCustomDns, cls).tearDownClass()
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
        self.thread = None
        self.cleanup = []
        return

    def tearDown(self):
        try:
            if self.thread and self.thread.is_alive():
                self.thread.join(5*60)
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, reversed(self.cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def createTinyServiceOffering(self):
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["big"],
        )
        self.cleanup.append(self.service_offering)

    def createNetworkOffering(self):
        off_service = self.services["network_offering"]
        off_service["internetprotocol"] = "dualstack"
        self.network_offering = NetworkOffering.create(
            self.apiclient,
            off_service
        )
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)


    def deployNetwork(self):
        network_service = self.services["network"]
        network_service["networkoffering"] = self.network_offering.id
        network_service["dns1"] = IP4_DNS1
        network_service["dns2"] = IP4_DNS2
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

    def getRouterProcessStatus(self, router, cmd):
        if router.id not in self.routerDetailsMap or self.routerDetailsMap[router.id] is None:
            connect_ip = self.apiclient.connection.mgtSvr
            connect_user = self.apiclient.connection.user
            connect_passwd = self.apiclient.connection.passwd
            hypervisor = self.hypervisor
            if self.hypervisor.lower() not in ('vmware', 'hyperv'):
                hosts = Host.list(
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
                connect_ip = host.ipaddress
                hypervisor = None
                try:
                    connect_user, connect_passwd= get_host_credentials(
                        self.config, host.ipaddress)
                except KeyError:
                    self.skipTest(
                        "Marvin configuration has no host credentials to\
                                check router services")
            details = {}
            details['connect_ip'] = connect_ip
            details['connect_user'] = connect_user
            details['connect_passwd'] = connect_passwd
            details['hypervisor'] = hypervisor
            self.routerDetailsMap[router.id] = details
        result = get_process_status(
            self.routerDetailsMap[router.id]['connect_ip'],
            22,
            self.routerDetailsMap[router.id]['connect_user'],
            self.routerDetailsMap[router.id]['connect_passwd'],
            router.linklocalip,
            cmd,
            hypervisor=self.routerDetailsMap[router.id]['hypervisor']
        )
        self.assertTrue(type(result) == list and len(result) > 0,
            "%s on router %s returned invalid result" % (cmd, router.id))
        result = '\n'.join(result)
        return result

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
        self.assertEqual(network.ip6dns1,
                    IP6_DNS1,
                    "IPv6 DNS1 is not same, expected=%s, actual=%s" % (IP6_DNS1, network.ip6dns1))
        self.assertEqual(network.ip6dns2,
                    IP6_DNS2,
                    "IPv6 DNS2 is not same, expected=%s, actual=%s" % (IP6_DNS2, network.ip6dns2))

    def checkNetworkRouter(self):
        router = self.getNetworkRouter(self.network)
        dns_cmd = "cat /etc/resolv.conf"
        res = self.getRouterProcessStatus(router, dns_cmd)
        ns = "nameserver %s" % IP4_DNS1
        self.assertTrue(ns in res,
            "Network router doesn't contain nameserver for DNS1: %s" % IP4_DNS1)
        ns = "nameserver %s" % IP4_DNS2
        self.assertTrue(ns in res,
            "Network router doesn't contain nameserver for DNS2: %s" % IP4_DNS2)
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
    @skipTestIf("ipv6NotSupported")
    def test_01_verify_ipv6_network(self):
        """Test to verify IPv6 network

        # Validate the following:
        # 1. Create IPv6 network, deploy VM
        # 2. Verify network has required IPv6 details
        # 3. List router for the network and verify it has required IPv6 details for Guest and Public NIC of the VR
        # 4. SSH into VR(s) and verify correct details are present for its NICs
        # 5. Verify VM in network has required IPv6 details
        # 6. Restart network with cleanup and re-verify network details
        # 7. Update network with a new offering and re-verify network details
        # 8. Deploy another IPv6 network and check routing between two networks and their VM
        # 9. Create IPv6 firewall rules and verify in VR if they get implemented
        """

        self.createNetworkOffering()
        self.createTinyServiceOffering()
        self.deployNetwork()
        self.deployNetworkVm()
        self.checkNetwork()
        self.checkNetworkRouter()
