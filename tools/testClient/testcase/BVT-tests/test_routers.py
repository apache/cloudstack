# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#
""" BVT tests for routers
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from settings import *
import remoteSSHClient
from utils import *
from base import *

#Import System modules
import time

services = TEST_ROUTER_SERVICES

class TestRouterServices(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.api_client = fetch_api_client()

        #Create an account, network, VM and IP addresses
        cls.account = Account.create(cls.api_client, services["account"])
        cls.vm_1 = VirtualMachine.create(
                                            cls.api_client,
                                            services["virtual_machine"],
                                            accountid=cls.account.account.name,
                                        )

        cls.cleanup = [cls.vm_1, cls.account]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = fetch_api_client()
            #Clean up, terminate the created templates
            cleanup_resources(cls.api_client, cls.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" %e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        return

    def test_01_router_basic(self):
        """Test router basic setup
        """

        # Validate the following:
        # 1. verify that listRouters returned a 'Running' router
        # 2. router will have dns same as that seen in listZones
        # 3. router will have a guestIP and a linkLocalIp"

        cmd = listRouters.listRoutersCmd()
        list_router_response = self.apiclient.listRouters(cmd)

        self.assertNotEqual(
                             len(list_router_response),
                             0,
                             "Check list router response"
                             )
        for i in range(len(list_router_response)):
            self.assertEqual(
                            list_router_response[i].state,
                            'Running',
                            "Check list router response for router state"
                        )

            cmd = listZones.listZonesCmd()
            cmd.zoneid = list_router_response[i].zoneid
            zone = self.apiclient.listZones(cmd)[0]

            self.assertEqual(
                            list_router_response[i].dns1,
                            zone.dns1,
                            "Compare DNS1 of router and zone"
                        )
            self.assertEqual(
                            list_router_response[i].dns2,
                            zone.dns2,
                            "Compare DNS2 of router and zone"
                        )
            self.assertEqual(
                            hasattr(list_router_response[i],'guestipaddress'),
                            True,
                            "Check whether router has guest IP field"
                            )

            self.assertEqual(
                            hasattr(list_router_response[i],'linklocalip'),
                            True,
                            "Check whether router has link local IP field"
                            )
        return

    def test_02_router_advanced(self):
        """Test router advanced setup
        """

        # Validate the following
        # 1. verify that listRouters returned a 'Running' router
        # 2. router will have dns and gateway as in listZones, listVlanIpRanges
        # 3. router will have guest,public and linklocal IPs

        cmd = listRouters.listRoutersCmd()
        cmd.account = self.account.account.name
        cmd.domainid = services["virtual_machine"]["domainid"]
        list_router_response = self.apiclient.listRouters(cmd)

        self.assertNotEqual(
                             len(list_router_response),
                             0,
                             "Check list router response"
                             )
        for i in range(len(list_router_response)):
            self.assertEqual(
                            list_router_response[i].state,
                            'Running',
                            "Check list router response for router state"
                        )

            cmd = listZones.listZonesCmd()
            cmd.zoneid = list_router_response[i].zoneid
            zone = self.apiclient.listZones(cmd)[0]

            self.assertEqual(
                            list_router_response[i].dns1,
                            zone.dns1,
                            "Compare DNS1 of router and zone"
                        )
            self.assertEqual(
                            list_router_response[i].dns2,
                            zone.dns2,
                            "Compare DNS2 of router and zone"
                        )
            self.assertEqual(
                            hasattr(list_router_response[i],'guestipaddress'),
                            True,
                            "Check whether router has guest IP field"
                            )

            self.assertEqual(
                            hasattr(list_router_response[i],'linklocalip'),
                            True,
                            "Check whether router has link local IP field"
                            )

            #Fetch corresponding ip ranges information from listVlanIpRanges
            cmd = listVlanIpRanges.listVlanIpRangesCmd()
            cmd.id = list_ssvm_response[i].zoneid
            ipranges_response = self.apiclient.listVlanIpRanges(cmd)[0]

            self.assertEqual(
                            list_router_response[i].gateway,
                            ipranges_response.gateway,
                            "Check gateway with that of corresponding IP range"
                            )
        return

    def test_03_stop_router(self):
        """Test stop router
        """

        # Validate the following
        # 1. listRouter should report the router for the account as stopped

        cmd = listRouters.listRoutersCmd()
        cmd.account = self.account.account.name
        cmd.domainid = services["virtual_machine"]["domainid"]
        router = self.apiclient.listRouters(cmd)[0]

        #Stop the router
        cmd = stopRouter.stopRouterCmd()
        cmd.id = router.id
        self.apiclient.stopRouter(cmd)

        #List routers to check state of router

        cmd = listRouters.listRoutersCmd()
        cmd.id = router.id
        router_response = self.apiclient.listRouters(cmd)[0]

        #List router should have router in stopped state
        self.assertEqual(
                            router_response.state,
                            'Stopped',
                            "Check list router response for router state"
                        )
        return

    def test_04_start_router(self):
        """Test start router
        """

        # Validate the following
        # 1. listRouter should report the router for the account as stopped

        cmd = listRouters.listRoutersCmd()
        cmd.account = self.account.account.name
        cmd.domainid = services["virtual_machine"]["domainid"]
        router = self.apiclient.listRouters(cmd)[0]

        #Start the router
        cmd = startRouter.startRouterCmd()
        cmd.id = router.id
        self.apiclient.startRouter(cmd)

        #List routers to check state of router

        cmd = listRouters.listRoutersCmd()
        cmd.id = router.id
        router_response = self.apiclient.listRouters(cmd)[0]

        #List router should have router in running state
        self.assertEqual(
                            router_response.state,
                            'Running',
                            "Check list router response for router state"
                        )
        return

    def test_05_reboot_router(self):
        """Test reboot router
        """

        # Validate the following
        # 1. listRouter should report the router for the account as stopped

        cmd = listRouters.listRoutersCmd()
        cmd.account = self.account.account.name
        cmd.domainid = services["virtual_machine"]["domainid"]
        router = self.apiclient.listRouters(cmd)[0]

        public_ip = router.publicip

        #Reboot the router
        cmd = rebootRouter.rebootRouterCmd()
        cmd.id = router.id
        self.apiclient.rebootRouter(cmd)

        #List routers to check state of router
        cmd = listRouters.listRoutersCmd()
        cmd.id = router.id
        router_response = self.apiclient.listRouters(cmd)[0]

        #List router should have router in running state and same public IP
        self.assertEqual(
                            router_response.state,
                            'Running',
                            "Check list router response for router state"
                        )

        self.assertEqual(
                            router_response.publicip,
                            public_ip,
                            "Check list router response for router public IP"
                        )
        return

    def test_05_network_gc(self):
        """Test network GC
        """

        # Validate the following
        # 1. stop All User VMs in the account
        # 2. wait for network.gc.interval time"
        # 3. After network.gc.interval, router should be stopped
        # 4. ListRouters should return the router in Stopped state

        cmd = listVirtualMachines.listVirtualMachinesCmd()
        cmd.account = self.account.account.name
        cmd.domainid = services["virtual_machine"]["domainid"]
        list_vms = self.apiclient.listVirtualMachines(cmd)

        self.assertNotEqual(
                            len(list_vms),
                            0,
                            "Check length of list VM response"
                        )

        for i in range(len(list_vms)):
            # Stop all virtual machines associated with that account
            cmd = stopVirtualMachine.stopVirtualMachineCmd()
            cmd.id = list_vms[i].id
            self.apiclient.stopVirtualMachine(cmd)

        # Wait for network.gc.interval
        cmd = listConfigurations.listConfigurationsCmd()
        cmd.name = 'network.gc.interval'
        response = self.apiclient.listConfigurations(cmd)[0]

        time.sleep(int(response.value))

        #Check status of network router
        cmd = listRouters.listRoutersCmd()
        cmd.account = self.account.account.name
        cmd.domainid = services["virtual_machine"]["domainid"]
        router = self.apiclient.listRouters(cmd)[0]

        self.assertEqual(
                            router.state,
                            'Stopped',
                            "Check state of the router after stopping all VMs associated with that account"
                        )
        return

    def test_06_router_internal_basic(self):
        """Test router internal basic zone
        """
        # Validate the following
        # 1. Router only does dhcp
        # 2. Verify that ports 67 (DHCP) and 53 (DNS) are open on UDP by checking status of dnsmasq process

        # Find router associated with user account
        cmd = listRouters.listRoutersCmd()
        cmd.account = self.account.account.name
        cmd.domainid = services["virtual_machine"]["domainid"]
        router = self.apiclient.listRouters(cmd)[0]

        cmd = listHosts.listHostsCmd()
        cmd.zoneid = router.zoneid
        cmd.type = 'Routing'
        cmd.state = 'Up'
        host = self.apiclient.listHosts(cmd)[0]

        #SSH to the machine
        ssh = remoteSSHClient.remoteSSHClient(
                                                host.ipaddress,
                                                services['virtual_machine']["publicport"],
                                                services['virtual_machine']["username"],
                                                services['virtual_machine']["password"]
                                            )
        ssh_command = "ssh -i ~/.ssh/id_rsa.cloud -p 3922 %s " % router.linklocalip

        # Double hop into router
        timeout = 5
        # Ensure the SSH login is successful
        c = ssh_command + "service dnsmasq status"
        while True:
            res = ssh.execute(c)[0]
            if res != "Host key verification failed.":
                break
            elif timeout == 0:
                break
            time.sleep(5)
            timeout = timeout - 1
        self.assertEqual(
                            res.count("is running"),
                            1,
                            "Check dnsmasq service is running or not"
                        )
        return

    def test_07_router_internal_adv(self):
        """Test router internal advanced zone
        """
        # Validate the following
        # 1. Router does dhcp, dns, gateway, LB, PF, FW
        # 2. verify that dhcp, dns ports are open on UDP
        # 3. dnsmasq, haproxy processes should be running

        # Find router associated with user account
        cmd = listRouters.listRoutersCmd()
        cmd.account = self.account.account.name
        cmd.domainid = services["virtual_machine"]["domainid"]
        router = self.apiclient.listRouters(cmd)[0]

        cmd = listHosts.listHostsCmd()
        cmd.zoneid = router.zoneid
        cmd.type = 'Routing'
        cmd.state = 'Up'
        host = self.apiclient.listHosts(cmd)[0]

        #SSH to the machine
        ssh = remoteSSHClient.remoteSSHClient(
                                                host.ipaddress,
                                                services['virtual_machine']["publicport"],
                                                services['virtual_machine']["username"],
                                                services['virtual_machine']["password"]
                                            )
        ssh_command = "ssh -i ~/.ssh/id_rsa.cloud -p 3922 %s " % router.linklocalip

        # Double hop into router
        timeout = 5
        # Ensure the SSH login is successful
        c = ssh_command + "service dnsmasq status"
        while True:
            res = ssh.execute(c)[0]
            if res != "Host key verification failed.":
                break
            elif timeout == 0:
                break
            time.sleep(5)
            timeout = timeout - 1
        self.assertEqual(
                            res.count("running"),
                            1,
                            "Check dnsmasq service is running or not"
                        )

        timeout = 5
        # Ensure the SSH login is successful
        c = ssh_command + "service haproxy status"
        while True:
            res = ssh.execute(c)[0]
            if res != "Host key verification failed.":
                break
            elif timeout == 0:
                break
            time.sleep(5)
            timeout = timeout - 1
        self.assertEqual(
                            res.count("running"),
                            1,
                            "Check haproxy service is running or not"
                        )
        return

    def test_08_restart_network_cleanup(self):
        """Test restart network
        """

        # Validate the following
        # 1. When cleanup = true, router is destroyed and a new one created
        # 2. New router will have new publicIp and linkLocalIp and all it's services should resume

        # Find router associated with user account
        cmd = listRouters.listRoutersCmd()
        cmd.account = self.account.account.name
        cmd.domainid = services["virtual_machine"]["domainid"]
        router = self.apiclient.listRouters(cmd)[0]

        #Store old values before restart
        old_linklocalip = router.linklocalip

        cmd = listNetworks.listNetworksCmd()
        cmd.account = self.account.account.name
        cmd.domainid = services["virtual_machine"]["domainid"]
        network = self.apiclient.listNetworks(cmd)[0]

        cmd = restartNetwork.restartNetworkCmd()
        cmd.id = network.id
        cmd.cleanup = True
        self.apiclient.restartNetwork(cmd)

        # Get router details after restart
        cmd = listRouters.listRoutersCmd()
        cmd.account = self.account.account.name
        cmd.domainid = services["virtual_machine"]["domainid"]
        router = self.apiclient.listRouters(cmd)[0]

        self.assertNotEqual(
                            router.linklocalip,
                            old_linklocalip,
                            "Check linklocal IP after restart"
                        )
        return

    def test_08_restart_network_wo_cleanup(self):
        """Test restart network without cleanup
        """

        # Validate the following
        # 1. When cleanup = false, router is restarted and all services inside the router are restarted
        # 2. check 'uptime' to see if the actual restart happened

        cmd = listNetworks.listNetworksCmd()
        cmd.account = self.account.account.name
        cmd.domainid = services["virtual_machine"]["domainid"]
        network = self.apiclient.listNetworks(cmd)[0]

        cmd = restartNetwork.restartNetworkCmd()
        cmd.id = network.id
        cmd.cleanup = False
        self.apiclient.restartNetwork(cmd)

        # Get router details after restart
        cmd = listRouters.listRoutersCmd()
        cmd.account = self.account.account.name
        cmd.domainid = services["virtual_machine"]["domainid"]
        router = self.apiclient.listRouters(cmd)[0]

        cmd = listHosts.listHostsCmd()
        cmd.zoneid = router.zoneid
        cmd.type = 'Routing'
        cmd.state = 'Up'
        host = self.apiclient.listHosts(cmd)[0]

        #SSH to the machine
        ssh = remoteSSHClient.remoteSSHClient(
                                                host.ipaddress,
                                                services['virtual_machine']["publicport"],
                                                services['virtual_machine']["username"],
                                                services['virtual_machine']["password"]
                                            )
        ssh_command = "ssh -i ~/.ssh/id_rsa.cloud -p 3922 %s uptime" % router.linklocalip

        # Double hop into router to check router uptime
        timeout = 5
        while True:
            res = ssh.execute(ssh_command)[0]
            if res != "Host key verification failed.":
                break
            elif timeout == 0:
                break
            time.sleep(5)
            timeout = timeout - 1

        # res = 12:37:14 up 1 min,  0 users,  load average: 0.61, 0.22, 0.08
        # Split result to check the uptime
        result = res.split()
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