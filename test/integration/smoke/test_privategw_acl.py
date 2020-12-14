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
from marvin.cloudstackAPI.createStaticRoute import createStaticRouteCmd
""" Tests for Network ACLs in VPC
"""
#Import Local Modules
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr
from marvin.codes import PASS

import time
import logging
import random


class Services:
    """Test VPC network services - Port Forwarding Rules Test Data Class.
    """

    def __init__(self):
        self.services = {
            "configurableData": {
                "host": {
                    "port": 22
                }
            },
            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                # Random characters are appended for unique
                # username
                "password": "password",
            },
            "host1": None,
            "service_offering": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
            },
            "network_offering": {
                "name": 'VPC Network offering',
                "displaytext": 'VPC Network off',
                "guestiptype": 'Isolated',
                "supportedservices": 'Vpn,Dhcp,Dns,SourceNat,PortForwarding,Lb,UserData,StaticNat,NetworkACL',
                "traffictype": 'GUEST',
                "availability": 'Optional',
                "useVpc": 'on',
                "serviceProviderList": {
                    "Vpn": 'VpcVirtualRouter',
                    "Dhcp": 'VpcVirtualRouter',
                    "Dns": 'VpcVirtualRouter',
                    "SourceNat": 'VpcVirtualRouter',
                    "PortForwarding": 'VpcVirtualRouter',
                    "Lb": 'VpcVirtualRouter',
                    "UserData": 'VpcVirtualRouter',
                    "StaticNat": 'VpcVirtualRouter',
                    "NetworkACL": 'VpcVirtualRouter'
                },
            },
            "network_offering_no_lb": {
                "name": 'VPC Network offering',
                "displaytext": 'VPC Network off',
                "guestiptype": 'Isolated',
                "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,UserData,StaticNat,NetworkACL',
                "traffictype": 'GUEST',
                "availability": 'Optional',
                "useVpc": 'on',
                "serviceProviderList": {
                    "Dhcp": 'VpcVirtualRouter',
                    "Dns": 'VpcVirtualRouter',
                    "SourceNat": 'VpcVirtualRouter',
                    "PortForwarding": 'VpcVirtualRouter',
                    "UserData": 'VpcVirtualRouter',
                    "StaticNat": 'VpcVirtualRouter',
                    "NetworkACL": 'VpcVirtualRouter'
                },
            },
            "redundant_vpc_offering": {
                "name": 'Redundant VPC off',
                "displaytext": 'Redundant VPC off',
                "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Lb,UserData,StaticNat',
                "serviceProviderList": {
                    "Vpn": 'VpcVirtualRouter',
                    "Dhcp": 'VpcVirtualRouter',
                    "Dns": 'VpcVirtualRouter',
                    "SourceNat": 'VpcVirtualRouter',
                    "PortForwarding": 'VpcVirtualRouter',
                    "Lb": 'VpcVirtualRouter',
                    "UserData": 'VpcVirtualRouter',
                    "StaticNat": 'VpcVirtualRouter',
                    "NetworkACL": 'VpcVirtualRouter'
                },
                "serviceCapabilityList": {
                    "SourceNat": {
                        "RedundantRouter": 'true'
                    }
                },
            },
            "vpc_offering": {
                "name": "VPC off",
                "displaytext": "VPC off",
                "supportedservices":
                    "Dhcp,Dns,SourceNat,PortForwarding,Vpn,Lb,UserData,StaticNat,NetworkACL"
            },
            "vpc": {
                "name": "TestVPC",
                "displaytext": "TestVPC",
                "cidr": '10.0.0.1/24'
            },
            "network": {
                "name": "Test Network",
                "displaytext": "Test Network",
                "netmask": '255.255.255.0'
            },
            "virtual_machine": {
                "displayname": "Test VM",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },
            "natrule": {
                "privateport": 22,
                "publicport": 22,
                "startport": 22,
                "endport": 22,
                "protocol": "TCP",
                "cidrlist": '0.0.0.0/0',
            },
            "ostype": 'CentOS 5.3 (64-bit)',
            "timeout": 10,
        }

class TestPrivateGwACL(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestPrivateGwACL, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype
        cls.template = get_test_template(
            cls.api_client,
            cls.zone.id,
            cls.hypervisor)

        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"])
        cls._cleanup = [cls.service_offering]

        cls.logger = logging.getLogger('TestPrivateGwACL')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.hypervisor = self.testClient.getHypervisorInfo()

        self.logger.debug("Creating Admin Account for Domain ID ==> %s" % self.domain.id)
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
            domainid=self.domain.id)

        self.cleanup = []
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def _replaceAcl(self, command):
        try:
            successResponse = self.apiclient.replaceNetworkACLList(command);
        except Exception as e:
            self.fail("Failed to replace ACL list due to %s" % e)

        self.assertTrue(successResponse.success, "Failed to replace ACL list.")

    @attr(tags=["advanced"], required_hardware="true")
    def test_01_vpc_privategw_acl(self):
        self.logger.debug("Creating a VPC offering..")
        vpc_off = VpcOffering.create(
            self.apiclient,
            self.services["vpc_offering"])

        self.logger.debug("Enabling the VPC offering created")
        vpc_off.update(self.apiclient, state='Enabled')

        vpc = self.createVPC(vpc_off)

        self.cleanup = [vpc, vpc_off, self.account]

        physical_network = self.get_guest_traffic_physical_network(self.apiclient, self.zone.id)
        if not physical_network:
            self.fail("No Physical Networks found!")

        qresultset = self.dbclient.execute(
            "select vnet from op_dc_vnet_alloc where physical_network_id=\
            (select id from physical_network where uuid='%s');" % physical_network.id)
        self.assertEqual(validateList(qresultset)[0],
                         PASS,
                         "Invalid sql query response"
                         )

        # Find all the vlans that are for dynamic vlan allocation
        dc_vlans = sorted([x[0] for x in qresultset])

        # Use VLAN id that is not in physical network vlan range for dynamic vlan allocation
        vlan_1 = int(physical_network.vlan.split('-')[-1]) + 1
        if vlan_1 in dc_vlans:
            vlan_1 = dc_vlans[-1] + random.randint(1, 5)

        acl = self.createACL(vpc)
        self.createACLItem(acl.id)
        self.createNetwork(vpc)
        privateGw = self.createPvtGw(vpc, "10.0.3.99", "10.0.3.100", acl.id, vlan_1)
        self.replacePvtGwACL(acl.id, privateGw.id)

    @attr(tags=["advanced"], required_hardware="true")
    def test_02_vpc_privategw_static_routes(self):
        self.logger.debug("Creating a VPC offering..")
        vpc_off = VpcOffering.create(
            self.apiclient,
            self.services["vpc_offering"])

        self.logger.debug("Enabling the VPC offering created")
        vpc_off.update(self.apiclient, state='Enabled')

        self.performVPCTests(vpc_off)

    @attr(tags=["advanced"], required_hardware="true")
    def test_03_vpc_privategw_restart_vpc_cleanup(self):
        self.logger.debug("Creating a VPC offering..")
        vpc_off = VpcOffering.create(
            self.apiclient,
            self.services["vpc_offering"])

        self.logger.debug("Enabling the VPC offering created")
        vpc_off.update(self.apiclient, state='Enabled')

        self.performVPCTests(vpc_off, restart_with_cleanup = True)

    @attr(tags=["advanced"], required_hardware="true")
    def test_04_rvpc_privategw_static_routes(self):
        self.logger.debug("Creating a Redundant VPC offering..")
        vpc_off = VpcOffering.create(
            self.apiclient,
            self.services["redundant_vpc_offering"])

        self.logger.debug("Enabling the Redundant VPC offering created")
        vpc_off.update(self.apiclient, state='Enabled')

        self.performVPCTests(vpc_off)

    @attr(tags=["advanced"], required_hardware="true")
    def _test_05_rvpc_privategw_check_interface(self):
        self.logger.debug("Creating a Redundant VPC offering..")
        vpc_off = VpcOffering.create(
            self.apiclient,
            self.services["redundant_vpc_offering"])

        self.logger.debug("Enabling the Redundant VPC offering created")
        vpc_off.update(self.apiclient, state='Enabled')

        self.performPrivateGWInterfaceTests(vpc_off)

    def performVPCTests(self, vpc_off, restart_with_cleanup = False):
        self.logger.debug("Creating VPCs with  offering ID %s" % vpc_off.id)
        vpc_1 = self.createVPC(vpc_off, cidr = '10.0.1.0/24')
        vpc_2 = self.createVPC(vpc_off, cidr = '10.0.2.0/24')

        self.cleanup = [vpc_1, vpc_2, vpc_off, self.account]

        network_1 = self.createNetwork(vpc_1, gateway = '10.0.1.1')
        network_2 = self.createNetwork(vpc_2, gateway = '10.0.2.1')

        vm1 = self.createVM(network_1)
        vm2 = self.createVM(network_2)

        self.cleanup.insert(0, vm1)
        self.cleanup.insert(0, vm2)

        physical_network = self.get_guest_traffic_physical_network(self.apiclient, self.zone.id)
        if not physical_network:
            self.fail("No Physical Networks found!")

        qresultset = self.dbclient.execute(
            "select vnet from op_dc_vnet_alloc where physical_network_id=\
            (select id from physical_network where uuid='%s');" % physical_network.id)
        self.assertEqual(validateList(qresultset)[0],
                         PASS,
                         "Invalid sql query response"
                         )

        # Find all the vlans that are for dynamic vlan allocation
        dc_vlans = sorted([x[0] for x in qresultset])

        # Use VLAN id that is not in physical network vlan range for dynamic vlan allocation
        vlan_1 = int(physical_network.vlan.split('-')[-1]) + 1
        if vlan_1 in dc_vlans:
            vlan_1 = dc_vlans[-1] + random.randint(1, 5)

        acl1 = self.createACL(vpc_1)
        self.createACLItem(acl1.id, cidr = "0.0.0.0/0")
        privateGw_1 = self.createPvtGw(vpc_1, "10.0.3.100", "10.0.3.101", acl1.id, vlan_1)
        self.replacePvtGwACL(acl1.id, privateGw_1.id)

        acl2 = self.createACL(vpc_2)
        self.createACLItem(acl2.id, cidr = "0.0.0.0/0")
        privateGw_2 = self.createPvtGw(vpc_2, "10.0.3.101", "10.0.3.100", acl2.id, vlan_1)
        self.replacePvtGwACL(acl2.id, privateGw_2.id)

        self.replaceNetworkAcl(acl1.id, network_1)
        self.replaceNetworkAcl(acl2.id, network_2)

        staticRoute_1 = self.createStaticRoute(privateGw_1.id, cidr = '10.0.2.0/24')
        staticRoute_2 = self.createStaticRoute(privateGw_2.id, cidr = '10.0.1.0/24')

        public_ip_1 = self.acquire_publicip(vpc_1, network_1)
        public_ip_2 = self.acquire_publicip(vpc_2, network_2)

        nat_rule_1 = self.create_natrule(vpc_1, vm1, public_ip_1, network_1)
        nat_rule_2 = self.create_natrule(vpc_2, vm2, public_ip_2, network_2)

        self.check_pvt_gw_connectivity(vm1, public_ip_1, [vm2.nic[0].ipaddress, vm1.nic[0].ipaddress])
        self.check_pvt_gw_connectivity(vm2, public_ip_2, [vm2.nic[0].ipaddress, vm1.nic[0].ipaddress])

        if restart_with_cleanup:
            self.reboot_vpc_with_cleanup(vpc_1, cleanup = restart_with_cleanup)
            self.reboot_vpc_with_cleanup(vpc_2, cleanup = restart_with_cleanup)

            self.check_pvt_gw_connectivity(vm1, public_ip_1, [vm2.nic[0].ipaddress, vm1.nic[0].ipaddress])
            self.check_pvt_gw_connectivity(vm2, public_ip_2, [vm2.nic[0].ipaddress, vm1.nic[0].ipaddress])

    def performPrivateGWInterfaceTests(self, vpc_off):
        self.logger.debug("Creating VPCs with  offering ID %s" % vpc_off.id)
        vpc_1 = self.createVPC(vpc_off, cidr = '10.0.0.0/16')

        self.cleanup = [vpc_1, vpc_off, self.account]

        physical_network = self.get_guest_traffic_physical_network(self.apiclient, self.zone.id)
        if not physical_network:
            self.fail("No Physical Networks found!")

        qresultset = self.dbclient.execute(
            "select vnet from op_dc_vnet_alloc where physical_network_id=\
            (select id from physical_network where uuid='%s');" % physical_network.id)
        self.assertEqual(validateList(qresultset)[0],
                         PASS,
                         "Invalid sql query response"
                         )

        # Find all the vlans that are for dynamic vlan allocation
        dc_vlans = sorted([x[0] for x in qresultset])

        # Use VLAN id that is not in physical network vlan range for dynamic vlan allocation
        vlan_1 = int(physical_network.vlan.split('-')[-1]) + 1
        if vlan_1 in dc_vlans:
            vlan_1 = dc_vlans[-1] + random.randint(1, 5)

        acl1 = self.createACL(vpc_1)
        self.createACLItem(acl1.id, cidr = "0.0.0.0/0")
        net_offering_no_lb = "network_offering_no_lb"

        network_1 = self.createNetwork(vpc_1, gateway = '10.0.0.1')
        network_2 = self.createNetwork(vpc_1, net_offering = net_offering_no_lb, gateway = '10.0.1.1')
        network_3 = self.createNetwork(vpc_1, net_offering = net_offering_no_lb, gateway = '10.0.2.1')
        network_4 = self.createNetwork(vpc_1, net_offering = net_offering_no_lb, gateway = '10.0.3.1')

        vm1 = self.createVM(network_1)
        vm2 = self.createVM(network_2)
        vm3 = self.createVM(network_3)
        vm4 = self.createVM(network_4)

        self.cleanup.insert(0, vm1)
        self.cleanup.insert(0, vm2)
        self.cleanup.insert(0, vm3)
        self.cleanup.insert(0, vm4)

        acl1 = self.createACL(vpc_1)
        self.createACLItem(acl1.id, cidr = "0.0.0.0/0")
        privateGw_1 = self.createPvtGw(vpc_1, "10.1.0.100", "10.1.0.101", acl1.id, vlan_1)
        self.replacePvtGwACL(acl1.id, privateGw_1.id)

        self.replaceNetworkAcl(acl1.id, network_1)
        self.replaceNetworkAcl(acl1.id, network_2)
        self.replaceNetworkAcl(acl1.id, network_3)
        self.replaceNetworkAcl(acl1.id, network_4)

        public_ip_1 = self.acquire_publicip(vpc_1, network_1)
        nat_rule_1 = self.create_natrule(vpc_1, vm1, public_ip_1, network_1)

        public_ip_2 = self.acquire_publicip(vpc_2, network_2)
        nat_rule_2 = self.create_natrule(vpc_2, vm2, public_ip_2, network_2)

        self.check_private_gateway_interfaces()

        self.check_pvt_gw_connectivity(vm1, public_ip_1, [vm2.nic[0].ipaddress, vm3.nic[0].ipaddress, vm4.nic[0].ipaddress])
        self.check_pvt_gw_connectivity(vm2, public_ip_2, [vm2.nic[0].ipaddress, vm3.nic[0].ipaddress, vm4.nic[0].ipaddress])

        self.reboot_vpc_with_cleanup(vpc_1, cleanup = True)
        self.check_routers_state()

        self.check_pvt_gw_connectivity(vm1, public_ip_1, [vm2.nic[0].ipaddress, vm3.nic[0].ipaddress, vm4.nic[0].ipaddress])
        self.check_pvt_gw_connectivity(vm2, public_ip_2, [vm2.nic[0].ipaddress, vm3.nic[0].ipaddress, vm4.nic[0].ipaddress])

        self.stop_router_by_type("MASTER")
        self.check_routers_state()

        self.check_private_gateway_interfaces()
        self.check_pvt_gw_connectivity(vm1, public_ip_1, [vm2.nic[0].ipaddress, vm3.nic[0].ipaddress, vm4.nic[0].ipaddress])
        self.check_pvt_gw_connectivity(vm2, public_ip_2, [vm2.nic[0].ipaddress, vm3.nic[0].ipaddress, vm4.nic[0].ipaddress])

        self.start_routers()
        self.check_routers_state()
        self.check_private_gateway_interfaces()
        self.check_pvt_gw_connectivity(vm1, public_ip_1, [vm2.nic[0].ipaddress, vm3.nic[0].ipaddress, vm4.nic[0].ipaddress])
        self.check_pvt_gw_connectivity(vm2, public_ip_2, [vm2.nic[0].ipaddress, vm3.nic[0].ipaddress, vm4.nic[0].ipaddress])

        self.deletePvtGw(privateGw_1.id)
        self.check_private_gateway_interfaces(status_to_check = "DOWN")

    def query_routers(self):
        routers = list_routers(self.apiclient,
                       account=self.account.name,
                       domainid=self.account.domainid)

        self.assertEqual(isinstance(routers, list), True,
            "Check for list routers response return valid data")

        self.assertEqual(len(routers), 2,
            "Check for list routers size returned '%s' instead of 2" % len(routers))

        return routers

    def stop_router_by_type(self, redundant_state):
        self.logger.debug('Stopping %s router' % redundant_state)
        routers = self.query_routers()
        for router in routers:
            if router.redundantstate == redundant_state:
                self.stop_router(router)
                break

    def start_routers(self):
        self.logger.debug('Starting stopped routers')
        routers = self.query_routers()
        for router in routers:
            self.logger.debug('Router %s has state %s' % (router.id, router.state))
            if router.state == "Stopped":
                self.logger.debug('Starting stopped router %s' % router.id)
                cmd = startRouter.startRouterCmd()
                cmd.id = router.id
                self.apiclient.startRouter(cmd)

    def stop_router(self, router):
        self.logger.debug('Stopping router %s' % router.id)
        cmd = stopRouter.stopRouterCmd()
        cmd.id = router.id
        self.apiclient.stopRouter(cmd)

    def createVPC(self, vpc_offering, cidr = '10.1.1.1/16'):
        try:
            self.logger.debug("Creating a VPC network in the account: %s" % self.account.name)
            self.services["vpc"]["cidr"] = cidr

            vpc = VPC.create(
                self.apiclient,
                self.services["vpc"],
                vpcofferingid=vpc_offering.id,
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid)

            self.logger.debug("Created VPC with ID: %s" % vpc.id)
        except Exception as e:
            self.fail('Unable to create VPC due to %s ' % e)

        return vpc

    def createVM(self, network):
        try:
            self.logger.debug('Creating VM in network=%s' % network.name)
            vm = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                networkids=[str(network.id)]
            )
            self.logger.debug("Created VM with ID: %s" % vm.id)
        except Exception as e:
            self.fail('Unable to create virtual machine due to %s ' % e)

        return vm

    def createStaticRoute(self, privateGwId, cidr = '10.0.0.0/16'):
        staticRouteCmd = createStaticRoute.createStaticRouteCmd()
        staticRouteCmd.cidr = cidr
        staticRouteCmd.gatewayid = privateGwId

        try:
            staticRoute = self.apiclient.createStaticRoute(staticRouteCmd)
            self.assertIsNotNone(staticRoute.id, "Failed to create static route.")

            self.logger.debug("Created staticRoute with ID: %s" % staticRoute.id)
        except Exception as e:
            self.fail('Unable to create static route due to %s ' % e)

        return staticRoute

    def createACL(self, vpc):
        createAclCmd = createNetworkACLList.createNetworkACLListCmd()
        createAclCmd.name = "ACL-Test-%s" % vpc.id
        createAclCmd.description = createAclCmd.name
        createAclCmd.vpcid = vpc.id
        try:
            acl = self.apiclient.createNetworkACLList(createAclCmd)
            self.assertIsNotNone(acl.id, "Failed to create ACL.")

            self.logger.debug("Created ACL with ID: %s" % acl.id)
        except Exception as e:
            self.fail('Unable to create ACL due to %s ' % e)

        return acl

    def createACLItem(self, aclId, cidr = "0.0.0.0/0"):
        createAclItemCmd = createNetworkACL.createNetworkACLCmd()
        createAclItemCmd.cidr = cidr
        createAclItemCmd.protocol = "All"
        createAclItemCmd.number = "1"
        createAclItemCmd.action = "Allow"
        createAclItemCmd.aclid = aclId
        try:
            aclItem = self.apiclient.createNetworkACL(createAclItemCmd)
            self.assertIsNotNone(aclItem.id, "Failed to create ACL item.")

            self.logger.debug("Created ACL Item ID: %s" % aclItem.id)
        except Exception as e:
            self.fail('Unable to create ACL Item due to %s ' % e)

    def createNetwork(self, vpc, net_offering = "network_offering", gateway = '10.1.1.1'):
        try:
            self.logger.debug('Create NetworkOffering')
            net_offerring = self.services[net_offering]
            net_offerring["name"] = "NET_OFF-%s" % gateway
            nw_off = NetworkOffering.create(
                self.apiclient,
                net_offerring,
                conservemode=False)

            nw_off.update(self.apiclient, state='Enabled')

            self.logger.debug('Created and Enabled NetworkOffering')

            self.services["network"]["name"] = "NETWORK-%s" % gateway

            self.logger.debug('Adding Network=%s' % self.services["network"])
            obj_network = Network.create(
                self.apiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=nw_off.id,
                zoneid=self.zone.id,
                gateway=gateway,
                vpcid=vpc.id
            )

            self.logger.debug("Created network with ID: %s" % obj_network.id)
        except Exception as e:
            self.fail('Unable to create a Network with offering=%s because of %s ' % (net_offerring, e))

        self.cleanup.insert(0, nw_off)
        self.cleanup.insert(0, obj_network)

        return obj_network

    def createPvtGw(self, vpc, ip_address, gateway, aclId, vlan):
        physical_network = self.get_guest_traffic_physical_network(self.apiclient, self.zone.id)
        if not physical_network:
            self.fail("No Physical Networks found!")

        self.logger.debug('::: Physical Networks ::: ==> %s' % physical_network)

        createPrivateGatewayCmd = createPrivateGateway.createPrivateGatewayCmd()
        createPrivateGatewayCmd.physicalnetworkid = physical_network.id
        createPrivateGatewayCmd.gateway = gateway
        createPrivateGatewayCmd.netmask = "255.255.255.0"
        createPrivateGatewayCmd.ipaddress = ip_address
        createPrivateGatewayCmd.vlan = vlan
        createPrivateGatewayCmd.bypassvlanoverlapcheck = "true"
        createPrivateGatewayCmd.vpcid = vpc.id
        createPrivateGatewayCmd.sourcenatsupported = "false"
        createPrivateGatewayCmd.aclid = aclId

        try:
            privateGw = self.apiclient.createPrivateGateway(createPrivateGatewayCmd)
        except Exception as e:
            self.fail("Failed to create Private Gateway ==> %s" % e)

        self.assertIsNotNone(privateGw.id, "Failed to create Private Gateway.")

        return privateGw

    def deletePvtGw(self, private_gw_id):
        deletePrivateGatewayCmd = deletePrivateGateway.deletePrivateGatewayCmd()
        deletePrivateGatewayCmd.id = private_gw_id

        privateGwResponse = None
        try:
            privateGwResponse = self.apiclient.deletePrivateGateway(deletePrivateGatewayCmd)
        except Exception as e:
            self.fail("Failed to create Private Gateway ==> %s" % e)

        self.assertIsNotNone(privateGwResponse, "Failed to Delete Private Gateway.")
        self.assertTrue(privateGwResponse.success, "Failed to Delete Private Gateway.")

    def replaceNetworkAcl(self, aclId, network):
        self.logger.debug("Replacing Network ACL with ACL ID ==> %s" % aclId)

        replaceNetworkACLListCmd = replaceNetworkACLList.replaceNetworkACLListCmd()
        replaceNetworkACLListCmd.aclid = aclId
        replaceNetworkACLListCmd.networkid = network.id

        self._replaceAcl(replaceNetworkACLListCmd)

    def replacePvtGwACL(self, aclId, privateGwId):
        self.logger.debug("Replacing Private GW ACL with ACL ID ==> %s" % aclId)

        replaceNetworkACLListCmd = replaceNetworkACLList.replaceNetworkACLListCmd()
        replaceNetworkACLListCmd.aclid = aclId
        replaceNetworkACLListCmd.gatewayid = privateGwId

        self._replaceAcl(replaceNetworkACLListCmd)

    def acquire_publicip(self, vpc, network):
        self.logger.debug("Associating public IP for network: %s" % network.name)
        public_ip = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network.id,
            vpcid=vpc.id
        )
        self.logger.debug("Associated %s with network %s" % (
            public_ip.ipaddress.ipaddress,
            network.id
        ))

        return public_ip

    def create_natrule(self, vpc, virtual_machine, public_ip, network):
        self.logger.debug("Creating NAT rule in network for vm with public IP")

        nat_service = self.services["natrule"]
        nat_rule = NATRule.create(
            self.apiclient,
            virtual_machine,
            nat_service,
            ipaddressid=public_ip.ipaddress.id,
            openfirewall=False,
            networkid=network.id,
            vpcid=vpc.id)

        self.logger.debug("Adding NetworkACL rules to make NAT rule accessible")
        nwacl_nat = NetworkACL.create(
            self.apiclient,
            networkid=network.id,
            services=nat_service,
            traffictype='Ingress'
        )
        self.logger.debug('nwacl_nat=%s' % nwacl_nat.__dict__)

        return nat_rule

    def check_pvt_gw_connectivity(self, virtual_machine, public_ip, vms_ips):
        sleep_time = 5
        succeeded_pings = 0
        minimum_vms_to_pass = 2
        for vm_ip in vms_ips:
            ssh_command = "ping -c 10 %s" % vm_ip

            # Should be able to SSH VM
            packet_loss = 100
            try:
                self.logger.debug("SSH into VM: %s" % public_ip.ipaddress.ipaddress)

                ssh = virtual_machine.get_ssh_client(ipaddress=public_ip.ipaddress.ipaddress)

                self.logger.debug("Sleeping for %s seconds in order to get the firewall applied..." % sleep_time)
                time.sleep(sleep_time)

                self.logger.debug("Ping to VM inside another Network Tier")
                result = ssh.execute(ssh_command)

                for line in result:
                    if "packet loss" in line:
                        packet_loss = int(line.split("% packet loss")[0].split(" ")[-1])
                        break

                self.logger.debug("SSH result: %s; COUNT is ==> %s" % (result, packet_loss < 50))
            except Exception as e:
                self.fail("SSH Access failed for %s: %s" % (virtual_machine, e))

            if packet_loss < 50:
                succeeded_pings += 1


        self.assertTrue(succeeded_pings >= minimum_vms_to_pass,
                        "Ping to VM on Network Tier N from VM in Network Tier A should be successful at least for 2 out of 3 VMs"
                       )

    def reboot_vpc_with_cleanup(self, vpc, cleanup = True):
        self.logger.debug("Restarting VPC %s with cleanup" % vpc.id)

        # Reboot the router
        cmd = restartVPC.restartVPCCmd()
        cmd.id = vpc.id
        cmd.cleanup = cleanup
        cmd.makeredundant = False
        self.api_client.restartVPC(cmd)

    def check_private_gateway_interfaces(self, status_to_check = "UP"):
        routers = self.query_routers()

        state_holder = {routers[0].linklocalip : {"state" : None, "mac" : None},
                        routers[1].linklocalip : {"state" : None, "mac" : None}}
        state = None
        mac = None
        for router in routers:
            hosts = list_hosts(
                    self.apiclient,
                    zoneid=router.zoneid,
                    type='Routing',
                    state='Up',
                    id=router.hostid)

            self.assertEqual(
                isinstance(hosts, list),
                True,
                "Check for list hosts response return valid data")

            host = hosts[0]
            host.user = self.hostConfig['username']
            host.passwd = self.hostConfig['password']
            host.port = self.services["configurableData"]["host"]["port"]

            if self.hypervisor.lower() in ('vmware', 'hyperv'):
                state = get_process_status(
                        self.apiclient.connection.mgtSvr,
                        22,
                        self.apiclient.connection.user,
                        self.apiclient.connection.passwd,
                        router.linklocalip,
                        "ip addr | grep eth6 | grep state | awk '{print $9;}'",
                        hypervisor=self.hypervisor)

                mac = get_process_status(
                        self.apiclient.connection.mgtSvr,
                        22,
                        self.apiclient.connection.user,
                        self.apiclient.connection.passwd,
                        router.linklocalip,
                        "ip addr | grep link/ether | awk '{print $2;}' | sed -n 7p",
                        hypervisor=self.hypervisor)
            else:
                try:
                    state = get_process_status(
                        host.ipaddress,
                        host.port,
                        host.user,
                        host.passwd,
                        router.linklocalip,
                        "ip addr | grep eth6 | grep state | awk '{print $9;}'")

                    mac = get_process_status(
                        host.ipaddress,
                        host.port,
                        host.user,
                        host.passwd,
                        router.linklocalip,
                        "ip addr | grep link/ether | awk '{print $2;}' | sed -n 7p")
                except KeyError:
                    self.skipTest("Provide a marvin config file with host credentials to run %s" % self._testMethodName)

            state = str(state[0])
            mac = str(mac[0])

            self.logger.debug("Result from the Router on IP '%s' is -> state: '%s', mac: '%s'" % (router.linklocalip, state, mac))
            state_holder[router.linklocalip]["state"] = str(state)
            state_holder[router.linklocalip]["mac"] = str(mac)


        if status_to_check == "UP":
            check_state = state_holder[routers[0].linklocalip]["state"].count(state_holder[routers[1].linklocalip]["state"])
            check_mac = state_holder[routers[0].linklocalip]["mac"].count(state_holder[routers[1].linklocalip]["mac"])

            self.assertTrue(check_state == 0, "Routers private gateway interface should not be on the same state!")
            self.assertTrue(check_mac == 0, "Routers private gateway interface should not have the same mac address!")
        else:
            self.assertTrue(check_state == 1, "Routers private gateway interface should should have been removed!")

    def check_routers_state(self, status_to_check="MASTER", expected_count=1):
        routers = self.query_routers()

        vals = ["MASTER", "BACKUP", "UNKNOWN"]
        cnts = [0, 0, 0]

        result = "UNKNOWN"
        for router in routers:
            if router.state == "Running":
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

                if self.hypervisor.lower() in ('vmware', 'hyperv'):
                        result = str(get_process_status(
                            self.apiclient.connection.mgtSvr,
                            22,
                            self.apiclient.connection.user,
                            self.apiclient.connection.passwd,
                            router.linklocalip,
                            "sh /opt/cloud/bin/checkrouter.sh ",
                            hypervisor=self.hypervisor
                        ))
                else:
                    try:
                        host.user, host.passwd = get_host_credentials(
                            self.config, host.ipaddress)
                        result = str(get_process_status(
                            host.ipaddress,
                            22,
                            host.user,
                            host.passwd,
                            router.linklocalip,
                            "sh /opt/cloud/bin/checkrouter.sh "
                        ))

                    except KeyError:
                        self.skipTest(
                            "Marvin configuration has no host credentials to\
                                    check router services")

                if result.count(status_to_check) == 1:
                    cnts[vals.index(status_to_check)] += 1

        if cnts[vals.index(status_to_check)] != expected_count:
            self.fail("Expected '%s' routers at state '%s', but found '%s'!" % (expected_count, status_to_check, cnts[vals.index(status_to_check)]))

    def get_guest_traffic_physical_network(self, apiclient, zoneid):
        physical_networks = get_physical_networks(apiclient, zoneid)
        if not physical_networks:
            return None
        for physical_network in physical_networks:
            if not physical_network.removed and physical_network.vlan:
                traffic_type_list = self.dbclient.execute(
                    "select traffic_type from physical_network_traffic_types where physical_network_id=\
                    (select id from physical_network where uuid='%s');" % physical_network.id
                )
                for traffic_type in traffic_type_list:
                    if "Guest" in  str(traffic_type[0]):
                        return physical_network
        return None
