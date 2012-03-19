# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#
""" BVT tests for routers
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
import remoteSSHClient
from testcase.libs.utils import *
from testcase.libs.base import *
from testcase.libs.common import *

#Import System modules
import time


class Services:
    """Test router Services
    """

    def __init__(self):
        self.services = {
                         "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 100, # in MHz
                                    "memory": 64, # In MBs
                                    },
                        "virtual_machine":
                                    {
                                        "displayname": "Test VM",
                                        "username": "root",
                                        "password": "fr3sca",
                                        "ssh_port": 22,
                                        "hypervisor": 'XenServer',
                                        "privateport": 22,
                                        "publicport": 22,
                                        "protocol": 'TCP',
                                },
                        "account": {
                                        "email": "test@test.com",
                                        "firstname": "Test",
                                        "lastname": "User",
                                        "username": "testuser",
                                        "password": "fr3sca",
                                        },
                         "ostypeid":12,
                         "sleep": 60,
                         "timeout": 10,
                         "mode": 'advanced', #Networking mode: Basic, Advanced
                        }


class TestRouterServices(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.api_client = fetch_api_client()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostypeid"]
                            )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        #Create an account, network, VM and IP addresses
        cls.account = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     domainid=cls.domain.id
                                     )
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.vm_1 = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["virtual_machine"],
                                    templateid=template.id,
                                    accountid=cls.account.account.name,
                                    domainid=cls.account.account.domainid,
                                    serviceofferingid=cls.service_offering.id
                                    )
        cls.cleanup = [
                       cls.vm_1,
                       cls.account,
                       cls.service_offering
                       ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = fetch_api_client()
            #Clean up, terminate the created templates
            cleanup_resources(cls.api_client, cls.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        return

    def test_01_router_internal_basic(self):
        """Test router internal basic zone
        """
        # Validate the following
        # 1. Router only does dhcp
        # 2. Verify that ports 67 (DHCP) and 53 (DNS) are open on UDP
        #    by checking status of dnsmasq process

        # Find router associated with user account
        list_router_response = list_routers(
                                    self.apiclient,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
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
                           state='Up'
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

        result = get_process_status(
                                host.ipaddress,
                                self.services['virtual_machine']["publicport"],
                                self.vm_1.username,
                                self.vm_1.password,
                                router.linklocalip,
                                "service dnsmasq status"
                                )
        res = str(result)
        self.debug("Dnsmasq process status: %s" % res)

        self.assertEqual(
                            res.count("running"),
                            1,
                            "Check dnsmasq service is running or not"
                        )
        return

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
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
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
                           state='Up'
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

        result = get_process_status(
                                host.ipaddress,
                                self.services['virtual_machine']["publicport"],
                                self.vm_1.username,
                                self.vm_1.password,
                                router.linklocalip,
                                "service dnsmasq status"
                                )
        res = str(result)
        self.debug("Dnsmasq process status: %s" % res)
        
        self.assertEqual(
                            res.count("running"),
                            1,
                            "Check dnsmasq service is running or not"
                        )

        result = get_process_status(
                                host.ipaddress,
                                self.services['virtual_machine']["publicport"],
                                self.vm_1.username,
                                self.vm_1.password,
                                router.linklocalip,
                                "service haproxy status"
                                )
        res = str(result)
        self.assertEqual(
                            res.count("running"),
                            1,
                            "Check haproxy service is running or not"
                        )
        self.debug("Haproxy process status: %s" % res)
        return

    def test_03_restart_network_cleanup(self):
        """Test restart network
        """

        # Validate the following
        # 1. When cleanup = true, router is destroyed and a new one created
        # 2. New router will have new publicIp and linkLocalIp and
        #    all it's services should resume

        # Find router associated with user account
        list_router_response = list_routers(
                                    self.apiclient,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                    )
        self.assertEqual(
                            isinstance(list_router_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        router = list_router_response[0]

        #Store old values before restart
        old_linklocalip = router.linklocalip

        timeout = 10
        # Network should be in Implemented or Setup stage before restart
        while True:
            networks = list_networks(
                                 self.apiclient,
                                 account=self.account.account.name,
                                 domainid=self.account.account.domainid
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
        list_router_response = list_routers(
                                    self.apiclient,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                    )
        self.assertEqual(
                            isinstance(list_router_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        router = list_router_response[0]

        self.assertNotEqual(
                            router.linklocalip,
                            old_linklocalip,
                            "Check link-local IP after restart"
                        )
        return

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
                                 account=self.account.account.name,
                                 domainid=self.account.account.domainid
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
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
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
                           state='Up'
                           )
        self.assertEqual(
                            isinstance(hosts, list),
                            True,
                            "Check list response returns a valid list"
                        )
        host = hosts[0]

        res = get_process_status(
                                host.ipaddress,
                                self.services['virtual_machine']["publicport"],
                                self.vm_1.username,
                                self.vm_1.password,
                                router.linklocalip,
                                "uptime"
                                )
        
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
                                (int(result[2]) < 3),
                                True,
                                "Check uptime is less than 3 mins or not"
                                )
        else:
            self.assertEqual(
                                str(result[3]),
                                'sec,',
                                "Check uptime is in seconds"
                                )
        return

    def test_05_router_basic(self):
        """Test router basic setup
        """

        # Validate the following:
        # 1. verify that listRouters returned a 'Running' router
        # 2. router will have dns same as that seen in listZones
        # 3. router will have a guestIP and a linkLocalIp"

        list_router_response = list_routers(
                                        self.apiclient,
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
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

    def test_06_router_advanced(self):
        """Test router advanced setup
        """

        # Validate the following
        # 1. verify that listRouters returned a 'Running' router
        # 2. router will have dns and gateway as in listZones, listVlanIpRanges
        # 3. router will have guest,public and linklocal IPs

        list_router_response = list_routers(
                                    self.apiclient,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
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

            #Fetch corresponding ip ranges information from listVlanIpRanges
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

    def test_07_stop_router(self):
        """Test stop router
        """

        # Validate the following
        # 1. listRouter should report the router for the account as stopped

        list_router_response = list_routers(
                                    self.apiclient,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                    )
        self.assertEqual(
                            isinstance(list_router_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        router = list_router_response[0]
        
        self.debug("Stopping the router with ID: %s" % router.id)
        #Stop the router
        cmd = stopRouter.stopRouterCmd()
        cmd.id = router.id
        self.apiclient.stopRouter(cmd)

        #List routers to check state of router
        router_response = list_routers(
                                    self.apiclient,
                                    id=router.id
                                    )
        self.assertEqual(
                            isinstance(router_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        #List router should have router in stopped state
        self.assertEqual(
                            router_response[0].state,
                            'Stopped',
                            "Check list router response for router state"
                        )
        return

    def test_08_start_router(self):
        """Test start router
        """

        # Validate the following
        # 1. listRouter should report the router for the account as stopped

        list_router_response = list_routers(
                                    self.apiclient,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                    )
        self.assertEqual(
                            isinstance(list_router_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        router = list_router_response[0]

        self.debug("Starting the router with ID: %s" % router.id)

        #Start the router
        cmd = startRouter.startRouterCmd()
        cmd.id = router.id
        self.apiclient.startRouter(cmd)

        #List routers to check state of router
        router_response = list_routers(
                                    self.apiclient,
                                    id=router.id
                                    )
        self.assertEqual(
                            isinstance(router_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        #List router should have router in running state
        self.assertEqual(
                            router_response[0].state,
                            'Running',
                            "Check list router response for router state"
                        )
        return

    def test_09_reboot_router(self):
        """Test reboot router
        """

        # Validate the following
        # 1. listRouter should report the router for the account as stopped

        list_router_response = list_routers(
                                    self.apiclient,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                    )
        self.assertEqual(
                            isinstance(list_router_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        router = list_router_response[0]

        public_ip = router.publicip

        self.debug("Rebooting the router with ID: %s" % router.id)
        
        #Reboot the router
        cmd = rebootRouter.rebootRouterCmd()
        cmd.id = router.id
        self.apiclient.rebootRouter(cmd)

        #List routers to check state of router
        router_response = list_routers(
                                    self.apiclient,
                                    id=router.id
                                    )
        self.assertEqual(
                            isinstance(router_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        #List router should have router in running state and same public IP
        self.assertEqual(
                            router_response[0].state,
                            'Running',
                            "Check list router response for router state"
                        )

        self.assertEqual(
                            router_response[0].publicip,
                            public_ip,
                            "Check list router response for router public IP"
                        )
        return

    def test_10_network_gc(self):
        """Test network GC
        """

        # Validate the following
        # 1. stop All User VMs in the account
        # 2. wait for network.gc.interval time"
        # 3. After network.gc.interval, router should be stopped
        # 4. ListRouters should return the router in Stopped state

        list_vms = list_virtual_machines(
                                         self.apiclient,
                                         account=self.account.account.name,
                                         domainid=self.account.account.domainid
                                         )
        self.assertEqual(
                            isinstance(list_vms, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            len(list_vms),
                            0,
                            "Check length of list VM response"
                        )

        for vm in list_vms:
            self.debug("Stopping the VM with ID: %s" % vm.id)
            # Stop all virtual machines associated with that account
            cmd = stopVirtualMachine.stopVirtualMachineCmd()
            cmd.id = vm.id
            self.apiclient.stopVirtualMachine(cmd)

        config = list_configurations(
                                     self.apiclient,
                                     name='network.gc.interval'
                                     )
        self.assertEqual(
                            isinstance(config, list),
                            True,
                            "Check list response returns a valid list"
                        )
        response = config[0]

        # Wait for network.gc.interval * 3 time to cleanup all the resources
        time.sleep(int(response.value) * 3)
        
        timeout = self.services["timeout"]
        while True:
            #Check status of network router
            list_router_response = list_routers(
                                    self.apiclient,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                    )
            if isinstance(list_router_response, list):
                break
            elif timeout == 0:
                raise Exception("List router call failed!")
            time.sleep(5)
            timeout = timeout -1
            
        self.assertEqual(
                            isinstance(list_router_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        router = list_router_response[0]
        
        self.debug("Router state after network.gc.interval: %s" % router.state)
        self.assertEqual(
            router.state,
            'Stopped',
            "Check state of the router after stopping all VMs associated"
            )
        return
