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
""" BVT tests for IPv6 Network"""

#Import Local Modules
from marvin.codes import FAILED
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (createGuestNetworkIpv6Prefix,
                                  listGuestNetworkIpv6Prefixes,
                                  deleteGuestNetworkIpv6Prefix,
                                  listIpv6FirewallRules,
                                  createIpv6FirewallRule,
                                  deleteIpv6FirewallRule)
from marvin.lib.utils import (random_gen,
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
                             NIC,
                             Host)
from marvin.lib.common import (get_domain,
                               get_zone,
                               list_hosts,
                               get_test_template)
from marvin.sshClient import SshClient
from marvin.cloudstackException import CloudstackAPIException
from marvin.lib.decoratorGenerators import skipTestIf

from nose.plugins.attrib import attr
from ipaddress import IPv6Network
from random import getrandbits, choice, randint
import time
import logging
import threading

ipv6_offering_config_name = "ipv6.offering.enabled"
ULA_BASE = IPv6Network("fd00::/8")
PREFIX_OPTIONS = [i for i in range(48, 65, 4)]
FIREWALL_TABLE = "ip6_firewall"
FIREWALL_CHAINS = {
    "Ingress": "fw_chain_ingress",
    "Egress": "fw_chain_egress"
}
CIDR_IPV6_ANY = "::/0"
ICMPV6_TYPE = {
    1: "destination-unreachable",
    2: "packet-too-big",
    3: "time-exceeded",
    4: "parameter-problem",
    128: "echo-request",
    129: "echo-reply",
    130: "mld-listener-query",
    131: "mld-listener-report",
    132: "mld-listener-done",
    133: "nd-router-solicit",
    134: "nd-router-advert",
    135: "nd-neighbor-solicit",
    136: "nd-neighbor-advert",
    137: "nd-redirect",
    138: "router-renumbering",
    141: "ind-neighbor-solicit",
    142: "ind-neighbor-advert",
    143: "mld2-listener-report"
}
ICMPV6_CODE_TYPE = {
    0: "no-route",
    1: "admin-prohibited",
    3: "addr-unreachable",
    4: "port-unreachable",
    5: "policy-fail",
    6: "reject-route"
}
ICMPV6_TYPE_ANY = "{ destination-unreachable, packet-too-big, time-exceeded, parameter-problem, echo-request, echo-reply, mld-listener-query, mld-listener-report, mld-listener-done, nd-router-solicit, nd-router-advert, nd-neighbor-solicit, nd-neighbor-advert, nd-redirect, router-renumbering }"
TCP_UDP_PORT_ANY = "{ 0-65535 }"
SLEEP_BEFORE_VR_CHANGES = 45
PING_RETRIES = 5
PING_SLEEP = 20

class TestIpv6Network(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestIpv6Network, cls).getClsTestClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.apiclient = testClient.getApiClient()
        cls.dbclient = testClient.getDbConnection()
        cls.test_ipv6_guestprefix = None
        cls.initial_ipv6_offering_enabled = None
        cls._cleanup = []
        cls.routerDetailsMap = {}

        cls.logger = logging.getLogger('TestIpv6Network')

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
        try:
            super(TestIpv6Network, cls).tearDownClass()
        finally:
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
        self.userapiclient = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain
        )
        self.dbclient = self.testClient.getDbConnection()
        self.thread = None
        self.cleanup = []
        return

    def tearDown(self):
        try:
            if self.thread and self.thread.is_alive():
                self.thread.join(5*60)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        finally:
            super(TestIpv6Network, self).tearDown()
        return

    def getRandomIpv6Cidr(self):
        prefix_length = choice(PREFIX_OPTIONS)
        random_suffix = getrandbits(40) << (128-prefix_length)
        base_address = ULA_BASE.network_address + random_suffix
        return str(IPv6Network((base_address, prefix_length)))

    def createTinyServiceOffering(self):
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["big"],
        )
        self.cleanup.append(self.service_offering)

    def createNetworkOfferingInternal(self, is_redundant, is_ipv6, egressdefaultpolicy=True):
        off_service = self.services["network_offering"]
        if is_redundant:
            off_service = self.services["nw_off_isolated_RVR"]
        if is_ipv6:
            off_service["internetprotocol"] = "dualstack"
        if egressdefaultpolicy:
            off_service["egress_policy"] = egressdefaultpolicy
        network_offering = NetworkOffering.create(
            self.apiclient,
            off_service
        )
        self.cleanup.append(network_offering)
        network_offering.update(self.apiclient, state='Enabled')
        return network_offering

    def createIpv4NetworkOffering(self, is_redundant=False):
        self.network_offering = self.createNetworkOfferingInternal(is_redundant, False, False)

    def createIpv6NetworkOffering(self, is_redundant=False):
        self.network_offering = self.createNetworkOfferingInternal(is_redundant, True, False)

    def createIpv6NetworkOfferingForUpdate(self, is_redundant=False):
        self.network_offering_update = self.createNetworkOfferingInternal(is_redundant, True)


    def deployNetwork(self):
        self.services["network"]["networkoffering"] = self.network_offering.id
        self.network = Network.create(
            self.userapiclient,
            self.services["network"],
            zoneid=self.zone.id
        )
        self.cleanup.append(self.network)

    def deployNetworkVm(self):
        if self.template == FAILED:
            assert False, "get_test_template() failed to return template"
        self.services["virtual_machine"]["zoneid"] = self.zone.id
        self.virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            networkids=self.network.id,
            serviceofferingid=self.service_offering.id
        )
        self.cleanup.append(self.virtual_machine)

    def checkIpv6NetworkBasic(self):
        self.debug("Listing network: %s" % (self.network.name))
        ipv6_network = Network.list(self.apiclient,listall="true",id=self.network.id)
        self.assertTrue(
            isinstance(ipv6_network, list),
            "Check listNetworks response returns a valid list"
        )
        self.assertEqual(
            len(ipv6_network),
            1,
            "Network not found"
        )
        ipv6_network = ipv6_network[0]
        self.assertNotEqual(ipv6_network,
                    None,
                    "User is not able to retrieve network details %s" % self.network.id)
        self.assertNotEqual(ipv6_network.ip6cidr,
                    None,
                    "IPv6 CIDR for network is empty")
        self.assertNotEqual(ipv6_network.ip6gateway,
                    None,
                    "IPv6 gateway for network is empty")
        self.assertNotEqual(ipv6_network.ip6routes,
                    None,
                    "IPv6 routes for network is empty")
        self.network_ipv6_routes = ipv6_network.ip6routes

    def checkIpv6NetworkRoutersBasic(self):
        self.debug("Listing routers for network: %s" % self.network.name)
        self.routers = Router.list(
            self.apiclient,
            networkid=self.network.id,
            listall=True
        )
        self.assertTrue(
            isinstance(self.routers, list),
            "Check listRouters response returns a valid list"
        )
        self.assertTrue(
            len(self.routers) > 0,
            "Router for the network isn't found"
        )
        for router in self.routers:
            self.assertFalse(
                router.isredundantrouter == True and router.redundantstate == "FAULT",
                "Router for the network is in FAULT state"
            )
            nics = router.nic
            for nic in nics:
                if (nic.traffictype == 'Guest' and router.isredundantrouter == False) or nic.traffictype == 'Public':
                    self.assertNotEqual(nic.ip6address,
                                None,
                                "IPv6 address for router %s NIC is empty" % nic.traffictype)
                    self.assertNotEqual(nic.ip6cidr,
                                None,
                                "IPv6 CIDR for router %s NIC is empty" % nic.traffictype)
                    self.assertNotEqual(nic.ip6gateway,
                                None,
                                "IPv6 gateway for router %s NIC is empty" % nic.traffictype)


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

    def getNetworkGateway(self, network):
        ipv6_network = Network.list(self.apiclient,listall="true",id=network.id)
        self.assertTrue(
            isinstance(ipv6_network, list),
            "Check listNetworks response returns a valid list"
        )
        self.assertEqual(
            len(ipv6_network),
            1,
            "Network not found"
        )
        ipv6_network = ipv6_network[0]
        self.assertNotEqual(ipv6_network.ip6gateway,
                    None,
                    "IPv6 gateway for network is empty")
        return ipv6_network.ip6gateway

    def getNetworkRoutes(self, network):
        ipv6_network = Network.list(self.apiclient,listall="true",id=network.id)
        self.assertTrue(
            isinstance(ipv6_network, list),
            "Check listNetworks response returns a valid list"
        )
        self.assertEqual(
            len(ipv6_network),
            1,
            "Network not found"
        )
        ipv6_network = ipv6_network[0]
        self.assertNotEqual(ipv6_network.ip6routes,
                    None,
                    "IPv6 routes for network is empty")
        return ipv6_network.ip6routes

    def isNetworkEgressDefaultPolicyAllow(self, network):
        ipv6_network = Network.list(self.apiclient,listall="true",id=network.id)
        if len(ipv6_network) == 1:
            ipv6_network = ipv6_network[0]
            return ipv6_network.egressdefaultpolicy
        return False

    def checkRouterNicState(self, router, dev, state):
        st = "state %s" % state
        cmd = "ip link show %s | grep '%s'" % (dev, st)
        res = self.getRouterProcessStatus(router, cmd)
        self.assertTrue(type(res) == str and len(res) > 0 and st in res,
            "%s failed on router %s" % (cmd, router.id))

    def checkIpv6NetworkPrimaryRouter(self, router, network_ip6gateway):
        self.checkRouterNicState(router, "eth0", "UP")
        guest_gateway_check_cmd = "ip -6 address show %s | grep 'inet6 %s'" % ("eth0", network_ip6gateway)
        res = self.getRouterProcessStatus(router, guest_gateway_check_cmd)
        self.assertTrue(type(res) == str and len(res) > 0 and network_ip6gateway in res,
            "%s failed on router %s" % (guest_gateway_check_cmd, router.id))
        self.assertFalse("dadfailed" in res,
            "dadfailed for IPv6 guest gateway on router %s" % router.id)
        self.checkRouterNicState(router, "eth2", "UP")
        public_ipv6 = None
        public_ipv6_gateway = None
        nics = router.nic
        for nic in nics:
            if nic.traffictype == 'Public':
                public_ipv6 = nic.ip6address
                public_ipv6_gateway = nic.ip6gateway
                break
        self.assertNotEqual(public_ipv6,
            None,
            "IPv6 address for router Public NIC is empty")
        public_ip_check_cmd = "ip -6 address show %s | grep 'inet6 %s'" % ("eth2", public_ipv6)
        res = self.getRouterProcessStatus(router, public_ip_check_cmd)
        self.assertTrue(type(res) == str and len(res) > 0 and public_ipv6 in res,
            "%s failed on router %s" % (public_ip_check_cmd, router.id))
        self.assertFalse("dadfailed" in res,
            "dadfailed for public IPv6 on router %s" % router.id)
        self.assertNotEqual(public_ipv6_gateway,
            None,
            "IPv6 gateway for router Public NIC is empty")
        default_route_check_cmd = "ip -6 route | grep 'default via %s'" % (public_ipv6_gateway)
        res = self.getRouterProcessStatus(router, default_route_check_cmd)
        self.assertTrue(type(res) == str and len(res) > 0 and public_ipv6_gateway in res,
            "%s failed on router %s" % (default_route_check_cmd, router.id))

    def checkIpv6NetworkBackupRouter(self, router, network_ip6gateway):
        self.checkRouterNicState(router, "eth0", "UP")
        guest_gateway_check_cmd = "ip -6 address show %s | grep 'inet6 %s'" % ("eth0", network_ip6gateway)
        res = self.getRouterProcessStatus(router, guest_gateway_check_cmd)
        self.assertFalse(type(res) == str and len(res) > 0 and network_ip6gateway in res,
            "%s failed on router %s" % (guest_gateway_check_cmd, router.id))
        self.checkRouterNicState(router, "eth2", "DOWN")

    def checkIpv6NetworkRoutersInternal(self):
        network_ip6gateway = self.getNetworkGateway(self.network)
        for router in self.routers:
            if router.state != "Running":
                continue
            if router.isredundantrouter == True and router.redundantstate == 'BACKUP':
                self.checkIpv6NetworkBackupRouter(router, network_ip6gateway)
                continue
            self.checkIpv6NetworkPrimaryRouter(router, network_ip6gateway)


    def checkIpv6NetworkVm(self):
        self.debug("Listing NICS for VM %s in network: %s" % (self.virtual_machine.name, self.network.name))
        nics = NIC.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            networkid=self.network.id
        )
        self.assertEqual(
            len(nics),
            1,
            "VM NIC for the network isn't found"
        )
        nic = nics[0]
        self.assertNotEqual(nic.ip6address,
                    None,
                    "IPv6 address for VM %s NIC is empty" % nic.traffictype)
        self.virtual_machine_ipv6_address = nic.ip6address
        self.assertNotEqual(nic.ip6cidr,
                    None,
                    "IPv6 CIDR for VM %s NIC is empty" % nic.traffictype)
        self.assertNotEqual(nic.ip6gateway,
                    None,
                    "IPv6 gateway for VM %s NIC is empty" % nic.traffictype)

    def restartNetworkWithCleanup(self):
        self.network.restart(self.userapiclient, cleanup=True)
        time.sleep(SLEEP_BEFORE_VR_CHANGES)

    def updateNetworkWithOffering(self):
        self.network.update(self.userapiclient, networkofferingid=self.network_offering_update.id)
        time.sleep(SLEEP_BEFORE_VR_CHANGES)

    def createIpv6FirewallRuleInNetwork(self, network_id, traffic_type, source_cidr, dest_cidr, protocol,
        start_port, end_port, icmp_type, icmp_code):
        cmd = createIpv6FirewallRule.createIpv6FirewallRuleCmd()
        cmd.networkid = network_id
        cmd.traffictype = traffic_type
        if source_cidr:
            cmd.cidrlist = source_cidr
        if dest_cidr:
            cmd.destcidrlist = dest_cidr
        if protocol:
            cmd.protocol = protocol
        if start_port:
            cmd.startport = start_port
        if end_port:
            cmd.endport = end_port
        if icmp_type is not None:
            cmd.icmptype = icmp_type
        if icmp_code is not None:
            cmd.icmpcode = icmp_code
        fw_rule = self.userapiclient.createIpv6FirewallRule(cmd)
        return fw_rule

    def deployRoutingTestResources(self):
        self.routing_test_network_offering = self.createNetworkOfferingInternal(False, True)
        self.services["network"]["networkoffering"] = self.routing_test_network_offering.id
        self.routing_test_network = Network.create(
            self.apiclient,
            self.services["network"],
            self.account.name,
            self.account.domainid,
            zoneid=self.zone.id
        )
        self.cleanup.append(self.routing_test_network)
        self.services["virtual_machine"]["zoneid"] = self.zone.id
        self.routing_test_vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=[self.routing_test_network.id],
            serviceofferingid=self.service_offering.id,
            mode="advanced"
        )
        self.cleanup.append(self.routing_test_vm)

    def prepareRoutingTestResourcesInBackground(self):
        self.thread = threading.Thread(target=self.deployRoutingTestResources, args=())
        self.thread.daemon = True
        self.thread.start()

    def checkIpv6NetworkRouting(self):
        if not self.thread:
            self.deployRoutingTestResources()
        else:
            self.thread.join(5*60)
        self.assertFalse(not self.routing_test_network or not self.routing_test_vm,
            "Routing resources failure")

        fw1 = self.createIpv6FirewallRuleInNetwork(self.routing_test_network.id, "Ingress", None, None, "icmp",
            None, None, None, None)
        fw2 = self.createIpv6FirewallRuleInNetwork(self.network.id, "Ingress", None, None, "icmp",
            None, None, None, None)

        test_network_router = self.getNetworkRouter(self.routing_test_network)
        routes = self.getNetworkRoutes(self.network)
        self.logger.debug("Adding network routes in routing_test_network %s" % routes)
        for route in routes:
            add_route_cmd = "ip -6 route add %s via %s" % (route.subnet, route.gateway)
            self.getRouterProcessStatus(test_network_router, add_route_cmd)

        network_router = self.getNetworkRouter(self.network)
        routes = self.getNetworkRoutes(self.routing_test_network)
        self.logger.debug("Adding routing_test_network routes in network %s" % routes)
        for route in routes:
            add_route_cmd = "ip -6 route add %s via %s" % (route.subnet, route.gateway)
            self.getRouterProcessStatus(network_router, add_route_cmd)

        ping_cmd = "ping6 -c 4 %s" % self.virtual_machine_ipv6_address
        count = 0
        while count < PING_RETRIES:
            count = count + 1
            res = self.getRouterProcessStatus(test_network_router, ping_cmd)
            if " 0% packet loss" in res:
                break
            time.sleep(PING_SLEEP)
        self.assertTrue(" 0% packet loss" in res,
            "Ping from router %s of network %s to VM %s of network %s is unsuccessful" % (test_network_router.id, self.routing_test_network.id, self.virtual_machine.id, self.network.id))

        ssh = self.routing_test_vm.get_ssh_client(retries=5)
        count = 0
        while count < PING_RETRIES:
            count = count + 1
            res = ssh.execute(ping_cmd)
            if type(res) == list and len(res) > 0 and " 0% packet loss" in '\n'.join(res):
                break
            time.sleep(PING_SLEEP)
        self.assertTrue(type(res) == list and len(res) > 0,
            "%s on VM %s returned invalid result" % (ping_cmd, self.routing_test_vm.id))
        self.logger.debug(res)
        res = '\n'.join(res)

        self.assertTrue(" 0% packet loss" in res,
            "Ping from VM %s of network %s to VM %s of network %s is unsuccessful" % (self.routing_test_vm.id, self.routing_test_network.id, self.virtual_machine.id, self.network.id))

        cmd = deleteIpv6FirewallRule.deleteIpv6FirewallRuleCmd()
        cmd.id = fw2.id
        self.userapiclient.deleteIpv6FirewallRule(cmd)

    def createAndVerifyIpv6FirewallRule(self, traffic_type, source_cidr, dest_cidr, protocol,
        start_port, end_port, icmp_type, icmp_code, parsed_rule, delete=False):
        self.logger.debug("createAndVerifyIpv6FirewallRule - %s" % parsed_rule)
        fw_rule = self.createIpv6FirewallRuleInNetwork(self.network.id, traffic_type, source_cidr, dest_cidr, protocol,
        start_port, end_port, icmp_type, icmp_code)
        cmd = listIpv6FirewallRules.listIpv6FirewallRulesCmd()
        cmd.id = fw_rule.id
        rules = self.userapiclient.listIpv6FirewallRules(cmd)
        self.assertTrue(
            isinstance(rules, list),
            "Check listIpv6FirewallRules response returns a valid list"
        )
        rule = rules[0]
        self.assertEqual(rule.networkid, self.network.id,
            "IPv6 firewall rule network ID mismatch %s, %s" % (rule.networkid, self.network.id))
        self.assertEqual(rule.traffictype, traffic_type,
            "IPv6 firewall rule traffic type mismatch %s, %s" % (rule.traffictype, traffic_type))
        if source_cidr:
            self.assertEqual(rule.cidrlist, source_cidr,
                "IPv6 firewall rule source CIDR mismatch %s, %s" % (rule.cidrlist, source_cidr))
        if dest_cidr:
            self.assertEqual(rule.destcidrlist, dest_cidr,
                "IPv6 firewall rule destination CIDR mismatch %s, %s" % (rule.destcidrlist, dest_cidr))
        if protocol:
            self.assertEqual(rule.protocol, protocol,
                "IPv6 firewall rule protocol mismatch %s, %s" % (rule.protocol, protocol))
        if start_port:
            self.assertEqual(rule.startport, start_port,
                "IPv6 firewall rule start port mismatch %d, %d" % (rule.startport, start_port))
        if end_port:
            self.assertEqual(rule.endport, end_port,
                "IPv6 firewall rule end port mismatch %d, %d" % (rule.endport, end_port))
        if icmp_type is not None:
            self.assertEqual(rule.icmptype, icmp_type,
                "IPv6 firewall rule ICMP type mismatch %d, %d" % (rule.icmptype, icmp_type))
        if icmp_code is not None:
            self.assertEqual(rule.icmpcode, icmp_code,
                "IPv6 firewall rule ICMP code mismatch %d, %d" % (rule.icmpcode, icmp_code))
        routerCmd = "nft list chain ip6 %s %s" % (FIREWALL_TABLE, FIREWALL_CHAINS[traffic_type])
        res = self.getRouterProcessStatus(self.getNetworkRouter(self.network), routerCmd)
        self.assertTrue(parsed_rule in res,
            "Listing firewall rule with nft list chain failure for rule: %s" % parsed_rule)
        if delete == True:
            cmd = deleteIpv6FirewallRule.deleteIpv6FirewallRuleCmd()
            cmd.id = fw_rule.id
            self.userapiclient.deleteIpv6FirewallRule(cmd)
            res = self.getRouterProcessStatus(self.getNetworkRouter(self.network), routerCmd)
            self.assertFalse(parsed_rule in res,
                "Firewall rule present in nft list chain failure despite delete for rule: %s" % parsed_rule)

    def checkIpv6FirewallRule(self):
        traffic_type = "Ingress"

        # Ingress - ip6 saddr SOURCE_CIDR ip6 daddr DEST_CIDR tcp dport { START_PORT-END_PORT } accept
        source_cidr = self.getRandomIpv6Cidr()
        dest_cidr = self.getRandomIpv6Cidr()
        protocol = "tcp"
        start_port = randint(3000, 5000)
        end_port = start_port + randint(1, 8)
        rule = "ip6 saddr %s ip6 daddr %s %s dport { %d-%d } accept" % (source_cidr, dest_cidr, protocol, start_port, end_port)
        self.createAndVerifyIpv6FirewallRule(traffic_type, source_cidr, dest_cidr, protocol,
            start_port, end_port, None, None, rule, True)

        # Ingress - ip6 daddr DEST_CIDR icmpv6 type TYPE code CODE accept
        source_cidr = self.getRandomIpv6Cidr()
        protocol = "icmp"
        icmp_type = choice(list(ICMPV6_TYPE.keys()))
        icmp_code = choice(list(ICMPV6_CODE_TYPE.keys()))
        rule = "ip6 saddr %s ip6 daddr %s %sv6 type %s %sv6 code %s accept" % (source_cidr, CIDR_IPV6_ANY, protocol, ICMPV6_TYPE[icmp_type], protocol, ICMPV6_CODE_TYPE[icmp_code])
        self.createAndVerifyIpv6FirewallRule(traffic_type, source_cidr, None, protocol,
            None, None, icmp_type, icmp_code, rule)

        action = "accept"
        if self.isNetworkEgressDefaultPolicyAllow(self.network):
            action = "drop"
        traffic_type = "Egress"

        # Egress - ip6 saddr ::/0 ip6 daddr ::/0 udp dport { 0-65355 } ACTION
        protocol = "udp"
        rule = "ip6 saddr %s ip6 daddr %s %s dport %s %s" % (CIDR_IPV6_ANY, CIDR_IPV6_ANY, protocol, TCP_UDP_PORT_ANY, action)
        self.createAndVerifyIpv6FirewallRule(traffic_type, None, None, protocol,
            None, None, None, None, rule)

        # Egress - ip6 saddr ::/0 ip6 daddr ::/0 icmpv6 type ANY_TYPE ACTION
        protocol = "icmp"
        rule = "ip6 saddr %s ip6 daddr %s %sv6 type %s %s" % (CIDR_IPV6_ANY, CIDR_IPV6_ANY, protocol, ICMPV6_TYPE_ANY, action)
        self.createAndVerifyIpv6FirewallRule(traffic_type, None, None, protocol,
            None, None, None, None, rule)

        # Egress - ip6 saddr ::/0 ip6 daddr DEST_CIDR ACTION
        protocol = "all"
        dest_cidr = self.getRandomIpv6Cidr()
        rule = "ip6 saddr %s ip6 daddr %s %s" % (CIDR_IPV6_ANY, CIDR_IPV6_ANY, action)
        self.createAndVerifyIpv6FirewallRule(traffic_type, None, None, protocol,
            None, None, None, None, rule)

    def checkNetworkVRRedundancy(self):
        network_ip6gateway = self.getNetworkGateway(self.network)
        primary_router = self.getNetworkRouter(self.network)
        Router.stop(
            self.apiclient,
            id=primary_router.id
        )
        time.sleep(SLEEP_BEFORE_VR_CHANGES)
        new_primary_router = self.getNetworkRouter(self.network)
        self.assertNotEqual(new_primary_router.id, primary_router.id,
            "Original primary router ID: %s of network is still the primary router after stopping" % (primary_router.id))
        self.checkIpv6NetworkPrimaryRouter(new_primary_router, network_ip6gateway)

    def checkIpv6Network(self):
        self.checkIpv6NetworkBasic()
        self.checkIpv6NetworkRoutersBasic()
        self.checkIpv6NetworkRoutersInternal()


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

        self.createIpv6NetworkOffering()
        self.createIpv6NetworkOfferingForUpdate()
        self.createTinyServiceOffering()
        self.deployNetwork()
        self.deployNetworkVm()
        self.checkIpv6Network()
        self.checkIpv6NetworkVm()
        self.prepareRoutingTestResourcesInBackground()
        self.restartNetworkWithCleanup()
        self.checkIpv6Network()
        self.updateNetworkWithOffering()
        self.checkIpv6Network()
        self.checkIpv6NetworkRouting()
        self.checkIpv6FirewallRule()

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
    def test_02_verify_ipv6_network_redundant(self):
        """Test to verify redundant IPv6 network

        # Validate the following:
        # 1. Create IPv6 network, deploy VM
        # 2. Verify network has required IPv6 details
        # 3. List VRs for the network and verify it has required IPv6 details for Guest and Public NIC of the VR
        # 4. SSH into VR(s) and verify correct details are present for its NICs
        # 5. Verify VM in network has required IPv6 details
        # 6. Restart network with cleanup and re-verify network details
        # 7. Update network with a new offering and re-verify network details
        # 8. Deploy another IPv6 network and check routing between two networks and their VM
        # 9. Create IPv6 firewall rules and verify in VR if they get implemented
        # 10. Stop primary router and verify internals in backup VR
        """

        self.createIpv6NetworkOffering(True)
        self.createIpv6NetworkOfferingForUpdate(True)
        self.createTinyServiceOffering()
        self.deployNetwork()
        self.deployNetworkVm()
        self.checkIpv6Network()
        self.checkIpv6NetworkVm()
        self.prepareRoutingTestResourcesInBackground()
        self.restartNetworkWithCleanup()
        self.checkIpv6Network()
        self.updateNetworkWithOffering()
        self.checkIpv6Network()
        self.checkIpv6NetworkRouting()
        self.checkIpv6FirewallRule()
        self.checkNetworkVRRedundancy()

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
    def test_03_verify_upgraded_ipv6_network(self):
        """Test to verify IPv4 network upgraded to IPv6 network

        # Validate the following:
        # 1. Create IPv4 network, deploy VM
        # 2. Update network to a IPv6 offering
        # 3. Verify network has required IPv6 details
        # 4. List VRs for the network and verify it has required IPv6 details for Guest and Public NIC of the VR
        # 5. SSH into VR(s) and verify correct details are present for its NICs
        # 6. Verify VM in network has required IPv6 details
        # 7. Restart network with cleanup and re-verify network details
        # 8. Deploy another IPv6 network and check routing between two networks and their VM
        # 9. Create IPv6 firewall rules and verify in VR if they get implemented
        """

        self.createIpv4NetworkOffering(False)
        self.createIpv6NetworkOfferingForUpdate(False)
        self.createTinyServiceOffering()
        self.prepareRoutingTestResourcesInBackground()
        self.deployNetwork()
        self.deployNetworkVm()
        self.updateNetworkWithOffering()
        self.checkIpv6Network()
        self.checkIpv6NetworkVm()
        self.restartNetworkWithCleanup()
        self.checkIpv6Network()
        self.checkIpv6NetworkRouting()
        self.checkIpv6FirewallRule()

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
    def test_04_verify_upgraded_ipv6_network_redundant(self):
        """Test to verify redundant IPv4 network upgraded to redundant IPv6 network

        # Validate the following:
        # 1. Create IPv4 network, deploy VM
        # 2. Update network to a IPv6 offering
        # 3. Verify network has required IPv6 details
        # 4. List VRs for the network and verify it has required IPv6 details for Guest and Public NIC of the VR
        # 5. SSH into VR(s) and verify correct details are present for its NICs
        # 6. Verify VM in network has required IPv6 details
        # 7. Restart network with cleanup and re-verify network details
        # 8. Deploy another IPv6 network and check routing between two networks and their VM
        # 9. Create IPv6 firewall rules and verify in VR if they get implemented
        # 10. Stop primary router and verify internals in backup VR
        """

        self.createIpv4NetworkOffering(True)
        self.createIpv6NetworkOfferingForUpdate(True)
        self.createTinyServiceOffering()
        self.prepareRoutingTestResourcesInBackground()
        self.deployNetwork()
        self.deployNetworkVm()
        self.updateNetworkWithOffering()
        self.checkIpv6Network()
        self.checkIpv6NetworkVm()
        self.restartNetworkWithCleanup()
        self.checkIpv6Network()
        self.checkIpv6NetworkRouting()
        self.checkIpv6FirewallRule()
        self.checkNetworkVRRedundancy()
