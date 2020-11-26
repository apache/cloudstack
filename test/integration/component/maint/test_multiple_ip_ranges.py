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
""" Tests for Multiple IP Ranges feature
"""
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.utils import cleanup_resources, get_process_status
from marvin.lib.base import (Account,
                             DiskOffering,
                             VirtualMachine,
                             Router,
                             ServiceOffering,
                             PublicIpRange)
from marvin.lib.common import (get_domain,
                               get_zone,
                               list_routers,
                               list_hosts,
                               get_pod,
                               get_template)
import netaddr
from nose.plugins.attrib import attr
from netaddr import IPNetwork, IPAddress
from marvin.sshClient import SshClient
import random


class TestMultipleIpRanges(cloudstackTestCase):

    """Test Multiple IP Ranges for guest network
    """
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestMultipleIpRanges, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.dbclient = cls.testClient.getDbConnection()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.pod = get_pod(cls.api_client, cls.zone.id)
        cls.testdata['mode'] = cls.zone.networktype
        cls.testdata["domainid"] = cls.domain.id
        cls.testdata["zoneid"] = cls.zone.id
        cls.account = Account.create(
            cls.api_client,
            cls.testdata["account"],
            domainid=cls.domain.id
        )
        cls.testdata["account"] = cls.account.name
        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.testdata["disk_offering"]
        )
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )
        cls.testdata["diskoffering"] = cls.disk_offering.id
        cls.dc_id = cls.dbclient.execute(
            "select id from data_center where uuid = '%s';" % str(
                cls.testdata["zoneid"]))
        cls.dc_id = cls.dc_id[0][0]
        cls.ids = cls.dbclient.execute(
            "select id from user_ip_address where allocated is null and data_center_id = '%s';" % str(
                cls.dc_id))
        cls.id_list = []
        for i in range(len(cls.ids)):
            cls.id_list.append(cls.ids[i][0])
        # Check if VR is already present in the setup
        vr_list = Router.list(cls.api_client, listall='true')
        cls.debug("vr list {}".format(vr_list))
        if isinstance(vr_list, list) and len(vr_list) > 0:
            cls.debug("VR is running in the setup")
            cls.vr_state = True
        else:
            cls.debug("VR is not present in the setup")
            cls.vr_state = False
            cls.id_list = cls.id_list[:-2]
        for id in cls.id_list:
            cls.dbclient.execute(
                "update user_ip_address set allocated=now() where id = '%s';" %
                str(id))
        # create new vlan ip range
        # Before creating ip range check the zone's network type
        if cls.zone.networktype.lower() == 'basic':
            cls.new_vlan = cls.createNewVlanRange()
        else:
            raise unittest.SkipTest(
                "These tests can be run only on basic zone.\
                        So skipping the tests")
        # Deploy vm in existing subnet if VR is not present
        if cls.vr_state is False:
            cls.vm_res = VirtualMachine.create(
                cls.api_client,
                cls.testdata["small"],
                templateid=cls.template.id,
                accountid=cls.account.name,
                domainid=cls.testdata["domainid"],
                zoneid=cls.testdata["zoneid"],
                serviceofferingid=cls.service_offering.id,
                mode=cls.testdata["mode"],
            )
        cls._cleanup = [
            cls.new_vlan,
            cls.account,
        ]
        return

    @classmethod
    def createNewVlanRange(cls):
        """ Increment current cidr of vlan range present in network
            and create new range
        """
        publicIpRange = PublicIpRange.list(cls.api_client)
        cls.startIp = publicIpRange[0].startip
        cls.endIp = publicIpRange[0].endip
        cls.gateway = publicIpRange[0].gateway
        cls.netmask = publicIpRange[0].netmask
        # Pass ip address and mask length to IPNetwork to findout the CIDR
        ip = IPNetwork(cls.startIp + "/" + cls.netmask)
        # Take random increment factor to avoid adding the same vlan ip range
        # in each test case
        networkIncrementFactor = random.randint(1,255)
        new_cidr = ip.__iadd__(networkIncrementFactor)
        ip2 = IPNetwork(new_cidr)
        test_nw = ip2.network
        ip = IPAddress(test_nw)
        # Add IP range(5 IPs) in the new CIDR
        test_gateway = ip.__add__(1)
        test_startIp = ip.__add__(3)
        test_endIp = ip.__add__(10)
        # Populating services with new IP range
        cls.testdata["vlan_ip_range"]["startip"] = test_startIp
        cls.testdata["vlan_ip_range"]["endip"] = test_endIp
        cls.testdata["vlan_ip_range"]["gateway"] = test_gateway
        cls.testdata["vlan_ip_range"]["netmask"] = cls.netmask
        cls.testdata["vlan_ip_range"]["zoneid"] = cls.zone.id
        cls.testdata["vlan_ip_range"]["podid"] = cls.pod.id

        return PublicIpRange.create(
                cls.api_client,
                cls.testdata["vlan_ip_range"])

    @classmethod
    def tearDownClass(cls):
        try:
            for id in cls.id_list:
                cls.dbclient.execute(
                    "update user_ip_address set allocated=default where id = '%s';" %
                    str(id))
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        # Deploy guest vm
        try:
            self.virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.testdata["small"],
                templateid=self.template.id,
                accountid=self.account.name,
                domainid=self.testdata["domainid"],
                zoneid=self.testdata["zoneid"],
                serviceofferingid=self.service_offering.id,
                mode=self.testdata["mode"],
            )
        except Exception as e:
            raise Exception(
                "Warning: Exception during vm deployment: {}".format(e))
        self.vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id
        )
        self.assertEqual(
            isinstance(self.vm_response, list),
            True,
            "Check VM list response returned a valid list"
        )
        self.ip_range = list(
            netaddr.iter_iprange(
                str(
                    self.testdata["vlan_ip_range"]["startip"]), str(
                    self.testdata["vlan_ip_range"]["endip"])))
        self.nic_ip = netaddr.IPAddress(
            str(
                self.vm_response[0].nic[0].ipaddress))
        self.debug("vm got {} as ip address".format(self.nic_ip))
        self.assertIn(
            self.nic_ip,
            self.ip_range,
            "VM did not get the ip address from the new ip range"
        )
        ip_alias = self.dbclient.execute(
            "select ip4_address from nic_ip_alias;"
        )
        self.alias_ip = str(ip_alias[0][0])
        self.debug("alias ip : %s" % self.alias_ip)
        self.assertNotEqual(
            self.alias_ip,
            None,
            "Error in creating ip alias. Please check MS logs"
        )
        self.cleanup.append(self.virtual_machine)
        return

    def tearDown(self):
        try:
            # Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def verify_vlan_range(self, vlan, services):
        # compare vlan_list response with configured values
        self.assertEqual(
            isinstance(vlan, list),
            True,
            "Check list response returned a valid list"
        )
        self.assertNotEqual(
            len(vlan),
            0,
            "check list vlan response"
        )
        self.assertEqual(
            str(vlan[0].startip),
            str(services["startip"]),
            "Start IP in vlan ip range is not matched with the\
                    configured start ip"
        )
        self.assertEqual(
            str(vlan[0].endip),
            str(services["endip"]),
            "End IP in vlan ip range is not matched with the configured end ip"
        )
        self.assertEqual(
            str(vlan[0].gateway),
            str(services["gateway"]),
            "gateway in vlan ip range is not matched with the\
                    configured gateway"
        )
        self.assertEqual(
            str(vlan[0].netmask),
            str(services["netmask"]),
            "netmask in vlan ip range is not matched with\
                    the configured netmask"
        )
        return

    @attr(tags=["sg"])
    def test_01_deploy_vm_in_new_cidr(self):
        """Deploy guest vm after adding guest IP range in new CIDR
            1.Deploy guest vm
            2.Verify vm gets the ip address from new cidr
        """
        self.ip_range = list(
            netaddr.iter_iprange(
                str(
                    self.testdata["vlan_ip_range"]["startip"]), str(
                    self.testdata["vlan_ip_range"]["endip"])))
        self.nic_ip = netaddr.IPAddress(
            str(
                self.vm_response[0].nic[0].ipaddress))
        self.debug("vm got {} as ip address".format(self.nic_ip))
        self.assertIn(
            self.nic_ip,
            self.ip_range,
            "VM did not get the ip address from the new ip range"
        )
        return

    @attr(tags=["sg"])
    def test_02_dns_service_on_alias_ip(self):
        """Deploy guest vm in new CIDR and verify dns service on alias ip
            1.Deploy guest vm in new cidr
            2.Verify dns service listens on alias ip in VR
        """
        list_router_response = list_routers(
            self.apiclient,
            zoneid=self.zone.id,
            listall=True
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

        port = self.testdata['configurableData']['host']["publicport"]
        username = self.testdata['configurableData']['host']["username"]
        password = self.testdata['configurableData']['host']["password"]

        # SSH to host so that host key is saved in first
        # attempt
        SshClient(host.ipaddress, port, username, password)

        proc = self.alias_ip + ":53"
        result = get_process_status(
            host.ipaddress,
            port,
            username,
            password,
            router.linklocalip,
            "netstat -atnp | grep %s" % proc
        )
        res = str(result)
        self.debug("Dns process status on alias ip: %s" % res)
        self.assertNotEqual(
            res.find(proc)
            - 1,
            "dnsmasq service is not running on alias ip"
        )
        return

    @attr(tags=["sg"])
    def test_03_passwd_service_on_alias_IP(self):
        """Deploy guest vm in new CIDR and verify passwd service on alias ip
            1.Deploy guest vm in new cidr
            2.Verify password service(passwd_server_ip.py) listens on alias ip in VR
        """
        list_router_response = list_routers(
            self.apiclient,
            zoneid=self.zone.id,
            listall=True
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

        port = self.testdata['configurableData']['host']["publicport"]
        username = self.testdata['configurableData']['host']["username"]
        password = self.testdata['configurableData']['host']["password"]

        # SSH to host so that host key is saved in first
        # attempt
        SshClient(host.ipaddress, port, username, password)

        proc = "python passwd_server_ip.py"
        result = get_process_status(
            host.ipaddress,
            port,
            username,
            password,
            router.linklocalip,
            "netstat -atnp | grep %s" % proc
        )
        res = str(result)
        self.debug("password process status on VR: %s" % res)
        self.assertNotEqual(
            res.find(self.alias_ip)
            - 1,
            "password service is not running on alias ip"
        )
        return

    @attr(tags=["sg"])
    def test_04_userdata_service_on_alias_IP(self):
        """Deploy guest vm in new CIDR and verify userdata service on alias ip
            1.Deploy guest vm in new cidr
            2.Verify userdata service(apache2) listens on alias ip in VR
        """
        list_router_response = list_routers(
            self.apiclient,
            zoneid=self.zone.id,
            listall=True
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

        port = self.testdata['configurableData']['host']["publicport"]
        username = self.testdata['configurableData']['host']["username"]
        password = self.testdata['configurableData']['host']["password"]

        # SSH to host so that host key is saved in first
        # attempt
        SshClient(host.ipaddress, port, username, password)

        proc = "apache2"
        result = get_process_status(
            host.ipaddress,
            port,
            username,
            password,
            router.linklocalip,
            "netstat -atnp | grep %s" % proc
        )
        res = str(result)
        self.debug("userdata process status on VR: %s" % res)
        self.assertNotEqual(
            res.find(self.alias_ip + ":80 ")
            - 1,
            "password service is not running on alias ip"
        )
        return

    @attr(tags=["sg"])
    def test_05_del_cidr_verify_alias_removal(self):
        """Destroy lastvm in the CIDR and verifly alias removal
            1.Deploy guest vm in new cidr
            2.Verify ip alias creation
            3.Destroy vm and wait for it to expunge
            4.Verify ip alias removal after vm expunge
        """
        list_router_response = list_routers(
            self.apiclient,
            zoneid=self.zone.id,
            listall=True
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

        port = self.testdata['configurableData']['host']["publicport"]
        username = self.testdata['configurableData']['host']["username"]
        password = self.testdata['configurableData']['host']["password"]

        # SSH to host so that host key is saved in first
        # attempt
        SshClient(host.ipaddress, port, username, password)

        proc = "ip addr show eth0"
        result = get_process_status(
            host.ipaddress,
            port,
            username,
            password,
            router.linklocalip,
            proc
        )
        res = str(result)
        self.debug("ip alias configuration on VR: %s" % res)
        self.assertNotEqual(
            res.find(self.alias_ip)
            - 1,
            "ip alias is not created on VR eth0"
        )
        self.virtual_machine.delete(self.apiclient)
        self.debug(
            "Verify that expunging the last vm in the CIDR should\
                    delete the ip alias from VR")
        ip_alias2 = self.dbclient.execute(
            "select ip4_address from nic_ip_alias;"
        )
        self.assertEqual(
            isinstance(ip_alias2, list),
            True,
            "Error in sql query"
        )
        self.assertEqual(
            len(ip_alias2),
            0,
            "Failure in clearing ip alias entry from cloud db"
        )

        proc = "ip addr show eth0"
        result = get_process_status(
            host.ipaddress,
            port,
            username,
            password,
            router.linklocalip,
            proc
        )
        res = str(result)
        self.assertEqual(
            res.find(
                self.alias_ip),
            - 1,
            "Failed to clean up ip alias from VR even after\
                    last vm expunge in the CIDR")
        self.debug("IP alias got deleted from VR successfully.")
        self.cleanup.remove(self.virtual_machine)
        return

    @attr(tags=["sg"])
    def test_06_reboot_VR_verify_ip_alias(self):
        """Reboot VR and verify ip alias
            1.Deploy guest vm in new cidr
            2.Verify ip alias creation
            3.Reboot VR
            4.Verify ip alias on VR
        """
        list_router_response = list_routers(
            self.apiclient,
            zoneid=self.zone.id,
            listall=True
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

        port = self.testdata['configurableData']['host']["publicport"]
        username = self.testdata['configurableData']['host']["username"]
        password = self.testdata['configurableData']['host']["password"]

        # SSH to host so that host key is saved in first
        # attempt
        SshClient(host.ipaddress, port, username, password)

        proc = "ip addr show eth0"
        result = get_process_status(
            host.ipaddress,
            port,
            username,
            password,
            router.linklocalip,
            proc
        )
        res = str(result)
        self.debug("ip alias configuration on VR: %s" % res)
        self.assertNotEqual(
            res.find(self.alias_ip)
            - 1,
            "ip alias is not created on VR eth0"
        )
        resp = Router.reboot(
            self.apiclient,
            router.id
        )
        self.debug("Reboot router api response: %s" % resp)
        list_router_response = list_routers(
            self.apiclient,
            zoneid=self.zone.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "Check list response returns a valid list"
        )
        router = list_router_response[0]
        self.assertEqual(
            router.state,
            'Running',
            "Router is not in running state after reboot"
        )
        result = get_process_status(
            host.ipaddress,
            port,
            username,
            password,
            router.linklocalip,
            proc
        )
        res = str(result)
        self.assertNotEqual(
            res.find(self.alias_ip),
            - 1,
            "IP alias not present on VR after VR reboot"
        )
        return

    @attr(tags=["sg"])
    def test_07_stop_start_VR_verify_ip_alias(self):
        """Reboot VR and verify ip alias
            1.Deploy guest vm in new cidr
            2.Verify ip alias creation
            3.Stop and Start VR
            4.Verify ip alias on VR
        """
        list_router_response = list_routers(
            self.apiclient,
            zoneid=self.zone.id,
            listall=True
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

        port = self.testdata['configurableData']['host']["publicport"]
        username = self.testdata['configurableData']['host']["username"]
        password = self.testdata['configurableData']['host']["password"]

        # SSH to host so that host key is saved in first
        # attempt
        SshClient(host.ipaddress, port, username, password)

        proc = "ip addr show eth0"
        result = get_process_status(
            host.ipaddress,
            port,
            username,
            password,
            router.linklocalip,
            proc
        )
        res = str(result)
        self.debug("ip alias configuration on VR: %s" % res)
        self.assertNotEqual(
            res.find(self.alias_ip)
            - 1,
            "ip alias is not created on VR eth0"
        )
        self.debug("Stopping VR")
        Router.stop(
            self.apiclient,
            router.id,
        )
        self.debug("Starting VR")
        Router.start(
            self.apiclient,
            router.id
        )
        list_router_response = list_routers(
            self.apiclient,
            zoneid=self.zone.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "Check list response returns a valid list"
        )
        router = list_router_response[0]
        self.assertEqual(
            router.state,
            'Running',
            "Router is not in running state after reboot"
        )
        self.debug("VR is up and Running")
        result = get_process_status(
            host.ipaddress,
            port,
            username,
            password,
            router.linklocalip,
            proc
        )
        res = str(result)
        self.assertNotEqual(
            res.find(self.alias_ip),
            - 1,
            "IP alias not present on VR after VR stop and start"
        )
        return
