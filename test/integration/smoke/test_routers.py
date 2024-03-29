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
""" BVT tests for routers
"""
# Import Local Modules
from marvin.codes import FAILED
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (stopRouter,
                                  restartNetwork,
                                  startRouter,
                                  rebootRouter,
                                  getRouterHealthCheckResults)
from marvin.lib.utils import (cleanup_resources,
                              get_process_status,
                              get_host_credentials)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_test_template,
                               list_hosts,
                               list_routers,
                               list_networks,
                               list_zones,
                               list_vlan_ipranges)
from nose.plugins.attrib import attr
# Import System modules
import time


_multiprocess_shared_ = True


class TestRouterServices(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        testClient = super(TestRouterServices, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype
        template = get_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor
        )
        if template == FAILED:
            cls.fail("get_test_template() failed to return template")

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls._cleanup = []

        # Create an account, network, VM and IP addresses
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls._cleanup.append(cls.service_offering)
        cls.vm_1 = VirtualMachine.create(
            cls.apiclient,
            cls.services["virtual_machine"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls._cleanup.append(cls.vm_1)
        return

    @classmethod
    def tearDownClass(cls):
        super(TestRouterServices, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.cleanup = []

    def tearDown(self):
        super(TestRouterServices, self).tearDown()

    @attr(tags=["advanced", "basic", "sg", "smoke"], required_hardware="true")
    def test_01_router_internal_basic(self):
        """Test router internal basic zone
        """
        # Validate the following
        # 1. Router only does dhcp
        # 2. Verify that ports 67 (DHCP) and 53 (DNS) are open on UDP
        #    by checking status of dnsmasq process

        # Find router associated with user account
        if self.zone.networktype == "Basic":
            list_router_response = list_routers(
                self.apiclient,
                listall="true"
            )
        else:
            list_router_response = list_routers(
                self.apiclient,
                account=self.account.name,
                domainid=self.account.domainid
            )
        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "Check list response returns a valid list"
        )
        router = list_router_response[0]

        hosts = list_hosts(
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

        self.debug("Router ID: %s, state: %s" % (router.id, router.state))

        self.assertEqual(
            router.state,
            'Running',
            "Check list router response for router state"
        )

        if self.hypervisor.lower() in ('vmware', 'hyperv'):
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                router.linklocalip,
                "systemctl is-active dnsmasq",
                hypervisor=self.hypervisor
            )
        else:
            try:
                host.user, host.passwd = get_host_credentials(
                    self.config, host.ipaddress)
                result = get_process_status(
                    host.ipaddress,
                    22,
                    host.user,
                    host.passwd,
                    router.linklocalip,
                    "systemctl is-active dnsmasq"
                )

            except KeyError:
                self.skipTest(
                    "Marvin configuration has no host credentials to\
                            check router services")
        res = str(result)
        self.debug("Dnsmasq process status: %s" % res)

        self.assertEqual(
            res.count("active"),
            1,
            "Check dnsmasq service is running or not"
        )
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_02_router_internal_adv(self):
        """Test router internal advanced zone
        """
        # Validate the following
        # 1. Router does dhcp, dns, gateway, LB, PF, FW
        # 2. verify that dhcp, dns ports are open on UDP
        # 3. dnsmasq, haproxy processes should be running

        # Find router associated with user account
        list_router_response = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "Check list response returns a valid list"
        )
        router = list_router_response[0]

        hosts = list_hosts(
            self.apiclient,
            zoneid=router.zoneid,
            type='Routing',
            state='Up',
            id=router.hostid
        )
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check list response returns a valid list"
        )
        host = hosts[0]

        self.debug("Router ID: %s, state: %s" % (router.id, router.state))
        self.assertEqual(
            router.state,
            'Running',
            "Check list router response for router state"
        )

        if self.hypervisor.lower() in ('vmware', 'hyperv'):
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                router.linklocalip,
                "systemctl is-active dnsmasq",
                hypervisor=self.hypervisor
            )
        else:
            try:
                host.user, host.passwd = get_host_credentials(
                    self.config, host.ipaddress)
                result = get_process_status(
                    host.ipaddress,
                    22,
                    host.user,
                    host.passwd,
                    router.linklocalip,
                    "systemctl is-active dnsmasq"
                )
            except KeyError:
                self.skipTest(
                    "Marvin configuration has no host credentials\
                            to check router services")
        res = str(result)
        self.debug("Dnsmasq process status: %s" % res)
        self.assertEqual(
            res.count("active"),
            1,
            "Check dnsmasq service is running or not"
        )

        if self.hypervisor.lower() in ('vmware', 'hyperv'):
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                router.linklocalip,
                "systemctl is-active haproxy",
                hypervisor=self.hypervisor
            )
        else:
            try:
                host.user, host.passwd = get_host_credentials(
                    self.config, host.ipaddress)
                result = get_process_status(
                    host.ipaddress,
                    22,
                    host.user,
                    host.passwd,
                    router.linklocalip,
                    "systemctl is-active haproxy"
                )
            except KeyError:
                self.skipTest(
                    "Marvin configuration has no host credentials\
                            to check router services")
        res = str(result)
        self.assertEqual(
            res.count("active"),
            1,
            "Check haproxy service is running or not"
        )
        self.debug("Haproxy process status: %s" % res)

        routers = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            fetchhealthcheckresults=True
        )

        self.assertEqual(isinstance(routers, list), True,
            "Check for list routers response return valid data"
        )
        self.assertNotEqual(
            len(routers), 0,
            "Check list router response"
        )

        router = routers[0]
        self.info("Router ID: %s & Router state: %s" % (
            router.id, router.state
        ))

        self.assertEqual(isinstance(router.healthcheckresults, list), True,
            "Router response should contain it's health check result as list"
        )

        cmd = getRouterHealthCheckResults.getRouterHealthCheckResultsCmd()
        cmd.routerid = router.id
        cmd.performfreshchecks = True # Perform fresh checks as a newly created router may not have results
        healthData = self.apiclient.getRouterHealthCheckResults(cmd)
        self.info("Router ID: %s & Router state: %s" % (
            router.id, router.state
        ))

        self.assertEqual(router.id, healthData.routerid,
            "Router response should contain it's health check result so id should match"
        )
        self.assertEqual(isinstance(healthData.healthchecks, list), True,
            "Router response should contain it's health check result as list"
        )

        self.verifyCheckTypes(healthData.healthchecks)
        self.verifyCheckNames(healthData.healthchecks)
        self.verifyCheckResults(healthData.healthchecks)

    def verifyCheckTypes(self, healthChecks):
        for checkType in ["basic", "advanced"]:
            foundType = False
            for check in healthChecks:
                if check.checktype == checkType:
                    foundType = True
                    break
            self.assertTrue(foundType,
                "Router should contain health check results info for type: " + checkType
            )

    def verifyCheckNames(self, healthChecks):
        for checkName in ["dns_check.py", "dhcp_check.py", "haproxy_check.py", "disk_space_check.py", "iptables_check.py", "gateways_check.py", "router_version_check.py"]:
            foundCheck = False
            for check in healthChecks:
                if check.checkname == checkName:
                    foundCheck = True
                    break
            self.assertTrue(foundCheck,
                "Router should contain health check results info for check name: " + checkName
            )

    def verifyCheckResults(self, healthChecks):
        failedCheck = 0
        for check in healthChecks:
            if check.success:
                print("check %s is good" % check.checkname)
            else:
                print("check %s failed due to %s" % (check.checkname, check.details))
                failedCheck = failedCheck + 1
        self.assertEquals(failedCheck, 0)

    @attr(
        tags=[
            "advanced",
            "basic",
            "advancedns",
            "smoke",
            "dvs"],
        required_hardware="false")
    def test_03_restart_network_cleanup(self):
        """Test restart network
        """

        # Validate the following
        # 1. When cleanup = true, router is destroyed and a new one created
        # 2. New router should have the same public IP

        # Find router associated with user account
        if self.zone.networktype.lower() == "basic":
            list_router_response = list_routers(
                self.apiclient,
                listall="true"
            )
        else:
            list_router_response = list_routers(
                self.apiclient,
                account=self.account.name,
                domainid=self.account.domainid
            )
        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "Check list response returns a valid list"
        )

        router = list_router_response[0]

        # Store old values before restart
        if self.zone.networktype.lower == "basic":
            old_publicip = router.guestipaddress
        else:
            old_publicip = router.publicip

        timeout = 10
        # Network should be in Implemented or Setup stage before restart
        while True:
            networks = list_networks(
                self.apiclient,
                account=self.account.name,
                domainid=self.account.domainid
            )
            self.assertEqual(
                isinstance(networks, list),
                True,
                "Check list response returns a valid list"
            )
            network = networks[0]
            if network.state in ["Implemented", "Setup"]:
                break
            elif timeout == 0:
                break
            else:
                time.sleep(self.services["sleep"])
                timeout = timeout - 1

        self.debug(
            "Restarting network with ID: %s, Network state: %s" % (
                network.id,
                network.state
            ))
        cmd = restartNetwork.restartNetworkCmd()
        cmd.id = network.id
        cmd.cleanup = True
        self.apiclient.restartNetwork(cmd)

        # Get router details after restart
        if self.zone.networktype.lower() == "basic":
            list_router_response = list_routers(
                self.apiclient,
                listall="true"
            )
        else:
            list_router_response = list_routers(
                self.apiclient,
                account=self.account.name,
                domainid=self.account.domainid
            )
        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "Check list response returns a valid list"
        )
        router = list_router_response[0]

        if self.zone.networktype.lower() == "basic":
            new_publicip = router.guestipaddress
        else:
            new_publicip = router.publicip
        self.assertEqual(
            new_publicip,
            old_publicip,
            "Public IP of the router should remain same after network restart"
        )
        return

    @attr(tags=["advanced", "advancedns", "smoke", "dvs"], required_hardware="true")
    def test_04_restart_network_wo_cleanup(self):
        """Test restart network without cleanup
        """

        # Validate the following
        # 1. When cleanup = false, router is restarted and
        #    all services inside the router are restarted
        # 2. check 'uptime' to see if the actual restart happened

        timeout = 10
        # Network should be in Implemented or Setup stage before restart
        while True:
            networks = list_networks(
                self.apiclient,
                account=self.account.name,
                domainid=self.account.domainid
            )
            self.assertEqual(
                isinstance(networks, list),
                True,
                "Check list response returns a valid list"
            )
            network = networks[0]
            if network.state in ["Implemented", "Setup"]:
                break
            elif timeout == 0:
                break
            else:
                time.sleep(self.services["sleep"])
                timeout = timeout - 1

        self.debug(
            "Restarting network with ID: %s, Network state: %s" % (
                network.id,
                network.state
            ))
        cmd = restartNetwork.restartNetworkCmd()
        cmd.id = network.id
        cmd.cleanup = False
        self.apiclient.restartNetwork(cmd)

        # Get router details after restart
        list_router_response = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "Check list response returns a valid list"
        )
        router = list_router_response[0]

        hosts = list_hosts(
            self.apiclient,
            zoneid=router.zoneid,
            type='Routing',
            state='Up',
            id=router.hostid
        )
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check list response returns a valid list"
        )
        host = hosts[0]

        if self.hypervisor.lower() in ('vmware', 'hyperv'):
            res = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                router.linklocalip,
                "uptime",
                hypervisor=self.hypervisor
            )
        else:
            try:
                host.user, host.passwd = get_host_credentials(
                    self.config, host.ipaddress)
                res = get_process_status(
                    host.ipaddress,
                    22,
                    host.user,
                    host.passwd,
                    router.linklocalip,
                    "uptime"
                )
            except KeyError:
                self.skipTest(
                    "Marvin configuration has no host credentials\
                            to check router services")
        # res = 12:37:14 up 1 min,  0 users,  load average: 0.61, 0.22, 0.08
        # Split result to check the uptime
        result = res[0].split()
        self.debug("Router Uptime: %s" % result)
        self.assertEqual(
            str(result[1]),
            'up',
            "Check router is running or not"
        )
        if str(result[3]) == "min,":
            self.assertEqual(
                (int(result[2]) < 20),
                True,
                "Check uptime is less than 20 mins or not"
            )
        else:
            self.assertEqual(
                str(result[3]),
                'sec,',
                "Check uptime is in seconds"
            )
        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="false")
    def test_05_router_basic(self):
        """Test router basic setup
        """

        # Validate the following:
        # 1. verify that listRouters returned a 'Running' router
        # 2. router will have dns same as that seen in listZones
        # 3. router will have a guestIP and a linkLocalIp"

        list_router_response = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(list_router_response),
            0,
            "Check list router response"
        )
        for router in list_router_response:
            self.assertEqual(
                router.state,
                'Running',
                "Check list router response for router state"
            )

            zones = list_zones(
                self.apiclient,
                id=router.zoneid
            )
            self.assertEqual(
                isinstance(zones, list),
                True,
                "Check list response returns a valid list"
            )
            zone = zones[0]

            self.assertEqual(
                router.dns1,
                zone.dns1,
                "Compare DNS1 of router and zone"
            )
            self.assertEqual(
                router.dns2,
                zone.dns2,
                "Compare DNS2 of router and zone"
            )
            self.assertEqual(
                hasattr(router, 'guestipaddress'),
                True,
                "Check whether router has guest IP field"
            )

            self.assertEqual(
                hasattr(router, 'linklocalip'),
                True,
                "Check whether router has link local IP field"
            )
        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="false")
    def test_06_router_advanced(self):
        """Test router advanced setup
        """

        # Validate the following
        # 1. verify that listRouters returned a 'Running' router
        # 2. router will have dns and gateway as in listZones, listVlanIpRanges
        # 3. router will have guest,public and linklocal IPs

        list_router_response = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(list_router_response),
            0,
            "Check list router response"
        )
        for router in list_router_response:
            self.assertEqual(
                router.state,
                'Running',
                "Check list router response for router state"
            )

            zones = list_zones(
                self.apiclient,
                id=router.zoneid
            )
            self.assertEqual(
                isinstance(zones, list),
                True,
                "Check list response returns a valid list"
            )
            zone = zones[0]

            self.assertEqual(
                router.dns1,
                zone.dns1,
                "Compare DNS1 of router and zone"
            )
            self.assertEqual(
                router.dns2,
                zone.dns2,
                "Compare DNS2 of router and zone"
            )
            self.assertEqual(
                hasattr(router, 'guestipaddress'),
                True,
                "Check whether router has guest IP field"
            )

            self.assertEqual(
                hasattr(router, 'linklocalip'),
                True,
                "Check whether router has link local IP field"
            )

            # Fetch corresponding ip ranges information from listVlanIpRanges
            ipranges_response = list_vlan_ipranges(
                self.apiclient,
                zoneid=router.zoneid
            )
            self.assertEqual(
                isinstance(ipranges_response, list),
                True,
                "Check list response returns a valid list"
            )
            iprange = ipranges_response[0]
            self.assertEqual(
                router.gateway,
                iprange.gateway,
                "Check gateway with that of corresponding IP range"
            )
        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="false")
    def test_07_stop_router(self):
        """Test stop router
        """

        # Validate the following
        # 1. listRouter should report the router for the account as stopped

        list_router_response = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "Check list response returns a valid list"
        )
        router = list_router_response[0]
        self.debug("Stopping the router with ID: %s" % router.id)
        # Stop the router
        cmd = stopRouter.stopRouterCmd()
        cmd.id = router.id
        self.apiclient.stopRouter(cmd)

        # List routers to check state of router
        router_response = list_routers(
            self.apiclient,
            id=router.id
        )
        self.assertEqual(
            isinstance(router_response, list),
            True,
            "Check list response returns a valid list"
        )
        # List router should have router in stopped state
        self.assertEqual(
            router_response[0].state,
            'Stopped',
            "Check list router response for router state"
        )
        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="false")
    def test_08_start_router(self):
        """Test start router
        """

        # Validate the following
        # 1. listRouter should report the router for the account as stopped

        list_router_response = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "Check list response returns a valid list"
        )
        router = list_router_response[0]

        self.debug("Starting the router with ID: %s" % router.id)

        # Start the router
        cmd = startRouter.startRouterCmd()
        cmd.id = router.id
        self.apiclient.startRouter(cmd)

        # List routers to check state of router
        router_response = list_routers(
            self.apiclient,
            id=router.id
        )
        self.assertEqual(
            isinstance(router_response, list),
            True,
            "Check list response returns a valid list"
        )
        # List router should have router in running state
        self.assertEqual(
            router_response[0].state,
            'Running',
            "Check list router response for router state"
        )
        return

    def verifyRouterResponse(self, router_response, ip):
        if (router_response) and (isinstance(router_response, list)) and \
           (router_response[0].state == "Running") and \
           (router_response[0].publicip == ip):
            return True
        return False

    @attr(tags=["advanced", "advancedns", "smoke", "dvs"], required_hardware="false")
    def test_09_reboot_router(self):
        """Test reboot router
        """

        # Validate the following
        # 1. listRouter should report the router for the account as stopped

        list_router_response = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "Check list response returns a valid list"
        )
        router = list_router_response[0]

        public_ip = router.publicip

        self.debug("Rebooting the router with ID: %s" % router.id)
        # Reboot the router
        cmd = rebootRouter.rebootRouterCmd()
        cmd.id = router.id
        self.apiclient.rebootRouter(cmd)

        # List routers to check state of router
        retries_cnt = 6
        while retries_cnt >= 0:
            router_response = list_routers(
                self.apiclient,
                id=router.id
            )
            if self.verifyRouterResponse(router_response, public_ip):
                self.debug("Router is running successfully after reboot")
                return
            time.sleep(10)
            retries_cnt = retries_cnt - 1
        self.fail(
            "Router response after reboot is either is invalid\
                    or in stopped state")
        return

    @attr(tags=["advanced", "advancedns", "smoke", "dvs"], required_hardware="false")
    def test_10_reboot_router_forced(self):
        """Test force reboot router
        """

        list_router_response = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "Check list response returns a valid list"
        )
        router = list_router_response[0]

        public_ip = router.publicip

        self.debug("Force rebooting the router with ID: %s" % router.id)
        # Reboot the router
        cmd = rebootRouter.rebootRouterCmd()
        cmd.id = router.id
        cmd.forced = True
        self.apiclient.rebootRouter(cmd)

        # List routers to check state of router
        retries_cnt = 10
        while retries_cnt >= 0:
            router_response = list_routers(
                self.apiclient,
                id=router.id
            )
            if self.verifyRouterResponse(router_response, public_ip):
                self.debug("Router is running successfully after force reboot")
                return
            time.sleep(10)
            retries_cnt = retries_cnt - 1
        self.fail(
            "Router response after force reboot is either invalid\
                    or router in stopped state")
        return
