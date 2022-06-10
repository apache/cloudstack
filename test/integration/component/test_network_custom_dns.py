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
from marvin.lib.utils import (cleanup_resources,
                              get_process_status,
                              get_host_credentials)
from marvin.lib.base import (Domain,
                             NetworkOffering,
                             Account,
                             Network,
                             Router,
                             ServiceOffering,
                             VirtualMachine,
                             Host)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_test_template)
from marvin.sshClient import SshClient
from marvin.lib.decoratorGenerators import skipTestIf

from nose.plugins.attrib import attr
import logging

IP4_DNS1 = "5.5.5.5"
IP4_DNS2 = "6.6.6.6"

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
