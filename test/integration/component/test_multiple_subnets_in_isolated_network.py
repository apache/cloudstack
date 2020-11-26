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

"""
Tests of acquiring IPs in multiple subnets for isolated network or vpc
"""

from nose.plugins.attrib import attr
from marvin.cloudstackAPI import rebootRouter
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.utils import (validateList,
                              get_host_credentials,
                              get_process_status,
                              cleanup_resources)
from marvin.lib.base import (Account,
                             Domain,
                             VirtualMachine,
                             ServiceOffering,
                             Zone,
                             Network,
                             NetworkOffering,
                             VPC,
                             VpcOffering,
                             StaticNATRule,
                             NATRule,
                             PublicIPAddress,
                             PublicIpRange)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_free_vlan,
                               get_template,
                               list_hosts,
                               list_routers)
import logging
import random
class TestMultiplePublicIpSubnets(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestMultiplePublicIpSubnets,
            cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.zone = Zone(zone.__dict__)
        cls.template = get_template(cls.apiclient, cls.zone.id)
        cls._cleanup = []
        cls.skip = False

        if str(cls.zone.securitygroupsenabled) == "True":
            cls.skip = True
            return

        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() not in ['kvm']:
            cls.skip = True
            return

        cls.logger = logging.getLogger("TestMultiplePublicIpSubnets")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)

        # Create small service offering
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )
        cls._cleanup.append(cls.service_offering)

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(cls):
        if cls.skip:
            cls.skipTest("Test can be run only on advanced zone and KVM hypervisor")
        cls.apiclient = cls.testClient.getApiClient()
        cls.cleanup = []
        return

    def tearDown(cls):
        try:
            cleanup_resources(cls.apiclient, cls.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def get_router(self, router_id):
        routers = list_routers(
            self.apiclient,
            id=router_id,
            listall=True)
        self.assertEqual(
            isinstance(routers, list),
            True,
            "Check for list routers response return valid data"
        )
        self.assertNotEqual(
            len(routers),
            0,
            "Check list router response"
        )
        return routers[0]

    def get_routers(self, network_id):
        routers = list_routers(
            self.apiclient,
            networkid=network_id,
            listall=True)
        self.assertEqual(
            isinstance(routers, list),
            True,
            "Check for list routers response return valid data"
        )
        self.assertNotEqual(
            len(routers),
            0,
            "Check list router response"
        )
        return routers

    def get_vpc_routers(self, vpc_id):
        routers = list_routers(
            self.apiclient,
            vpcid=vpc_id,
            listall=True)
        self.assertEqual(
            isinstance(routers, list),
            True,
            "Check for list routers response return valid data"
        )
        self.assertNotEqual(
            len(routers),
            0,
            "Check list router response"
        )
        return routers

    def get_router_host(self, router):
        self.assertEqual(
            router.state,
            'Running',
            "Check list router response for router state"
        )
        hosts = list_hosts(
            self.apiclient,
            id=router.hostid)
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check for list hosts response return valid data")
        host = hosts[0]
        if host.hypervisor.lower() not in "kvm":
            return
        host.user, host.password = get_host_credentials(self.config, host.ipaddress)
        host.port=22
        return host

    def get_router_ips(self, router):
        guestIp = None
        controlIp = None
        sourcenatIp = None
        for nic in router.nic:
            if guestIp is None and nic.traffictype == "Guest":
                guestIp = nic.ipaddress
            elif nic.traffictype == "Control":
                controlIp = nic.ipaddress
            elif sourcenatIp is None and nic.traffictype == "Public":
                sourcenatIp = nic.ipaddress
        return guestIp, controlIp, sourcenatIp

    def verify_router_publicnic_state(self, router, host, publicNics):
        command = '/opt/cloud/bin/checkrouter.sh | cut -d ":" -f2 |tr -d " "'
        self.logger.debug("Executing command '%s'" % command)
        result = get_process_status(
            host.ipaddress,
            host.port,
            host.user,
            host.password,
            router.linklocalip,
            command)
        self.assertTrue(len(result) > 0, "Cannot get router %s redundant state" % router.name)
        redundant_state = result[0]
        self.logger.debug("router %s redudnant state is %s" % (router.name, redundant_state))
        if redundant_state == "FAULT":
            self.logger.debug("Skip as redundant_state is %s" % redundant_state)
            return
        elif redundant_state == "MASTER":
            command = 'ip link show |grep BROADCAST | egrep "%s" |grep "state DOWN" |wc -l' % publicNics
        elif redundant_state == "BACKUP":
            command = 'ip link show |grep BROADCAST | egrep "%s" |grep "state UP" |wc -l' % publicNics
        result = get_process_status(
            host.ipaddress,
            host.port,
            host.user,
            host.password,
            router.linklocalip,
            command)
        self.assertTrue(len(result) > 0 and result[0] == "0", "Expected result is 0 but actual result is %s" % result[0])

    def verify_network_interfaces_in_router(self, router, host, expectedNics):
        command = 'ip link show |grep BROADCAST | cut -d ":" -f2 |tr -d " "|tr "\n" ","'
        self.logger.debug("Executing command '%s'" % command)
        result = get_process_status(
            host.ipaddress,
            host.port,
            host.user,
            host.password,
            router.linklocalip,
            command)
        self.assertTrue(len(result) > 0 and result[0] == expectedNics, "Expected nics are %s but actual nics are %s" %(expectedNics, result))

    def verify_ip_address_in_router(self, router, host, ipaddress, device, isExist=True):
        command = 'ip addr show %s |grep "inet "|cut -d " " -f6 |cut -d "/" -f1 |grep -w %s' % (device,ipaddress)
        self.logger.debug("Executing command '%s'" % command)
        result = get_process_status(
            host.ipaddress,
            host.port,
            host.user,
            host.password,
            router.linklocalip,
            command)
        self.assertEqual(len(result) > 0 and result[0] == ipaddress, isExist, "ip %s verification failed" % ipaddress)

    def get_free_ipaddress(self, vlanId):
        ipaddresses = PublicIPAddress.list(
            self.apiclient,
            vlanid=vlanId,
            state='Free'
        )
        self.assertEqual(
            isinstance(ipaddresses, list),
            True,
            "List ipaddresses should return a valid response for Free ipaddresses"
             )
        random.shuffle(ipaddresses)
        return ipaddresses[0].ipaddress

    @attr(tags=["advanced"], required_hardware="false")
    def test_01_acquire_public_ips_in_isolated_network_with_single_vr(self):
        """ Acquire IPs in multiple subnets in isolated networks with single VR

        # Steps
        # 1. Create network offering with single VR, and enable it
        # 2. create isolated network with the network offering
        # 3. create a vm in the network.
        #   verify the available nics in VR should be "eth0,eth1,eth2"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP
        # 4. get a free public ip, assign to network, and create port forwarding rules (ssh) to the vm
        #   verify the available nics in VR should be "eth0,eth1,eth2"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP and new ip
        # 5. remove the port forwarding rule, and release the new ip
        #   verify the available nics in VR should be "eth0,eth1,eth2"
        #   verify the IPs in VR. eth0 -> guest nic IP, eth2 -> source nat IP

        # 6. create new public ip range 1
        # 7. get a free ip 4 in new ip range 2, assign to network, and enable static nat to vm
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 1
        # 8. get a free ip in new ip range, assign to network, and create port forwarding rules (ssh) to the vm
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 1, new ip 2,
        # 9. get a free ip in new ip range, assign to network, and create port forwarding rules (ssh) to the vm
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 1, new ip 2, new ip 3
        # 10. release new ip 2
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 1, new ip 3
        # 11. release new ip 1
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 3
        # 12. create new public ip range 2
        # 13. get a free ip 4 in new ip range 2, assign to network, and create port forwarding rules (ssh) to the vm
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3,eth4,"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 3, eth4 -> new ip 4
        # 14. get a free ip 5 in new ip range 2, assign to network, and create port forwarding rules (ssh) to the vm
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3,eth4,"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 3, eth4 -> new ip 4/5
        # 15. get a free ip 6 in new ip range 2, assign to network, and create port forwarding rules (ssh) to the vm
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3,eth4,"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 3, eth4 -> new ip 4/5/6
        # 16. release new ip 5
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3,eth4,"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 3, eth4 -> new ip 4/6
        # 17. release new ip 4
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3,eth4,"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 3, eth4 -> new ip 6
        # 18. release new ip 3
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth4,"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth4 -> new ip 6
        # 19. restart network
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth4,"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth4 -> new ip 6
        # 20. reboot router
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3,"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 6
        # 21. restart network with cleanup
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3,"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 6
        # 22. restart network with cleanup, makeredundant=true
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3,"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 6
        """

        # Create new domain1
        self.domain1 = Domain.create(
            self.apiclient,
            services=self.services["acl"]["domain1"],
            parentdomainid=self.domain.id)
        # Create account1
        self.account1 = Account.create(
            self.apiclient,
            self.services["acl"]["accountD1"],
            domainid=self.domain1.id
        )
        self.cleanup.append(self.account1)
        self.cleanup.append(self.domain1)

        # 1. Create network offering with single VR, and enable it
        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["isolated_network_offering"],
        )
        self.network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(self.network_offering)

        # 2. create isolated network with the network offering
        self.services["network"]["zoneid"] = self.zone.id
        self.services["network"]["networkoffering"] = self.network_offering.id
        self.network1 = Network.create(
            self.apiclient,
            self.services["network"],
            self.account1.name,
            self.account1.domainid
        )

        # 3. create a vm in the network.
        try:
            self.virtual_machine1 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account1.name,
                domainid=self.account1.domainid,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                zoneid=self.zone.id,
                networkids=self.network1.id
            )
        except Exception as e:
            self.fail("Exception while deploying virtual machine: %s" % e)

        #   verify the available nics in VR should be "eth0,eth1,eth2"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP
        routers = self.get_routers(self.network1.id)
        for router in routers:
            host = self.get_router_host(router)
            self.verify_network_interfaces_in_router(router, host, "eth0,eth1,eth2,")
            guestIp, controlIp, sourcenatIp = self.get_router_ips(router)
            self.verify_ip_address_in_router(router, host, guestIp, "eth0", True)
            self.verify_ip_address_in_router(router, host, controlIp, "eth1", True)
            self.verify_ip_address_in_router(router, host, sourcenatIp, "eth2", True)
            self.verify_router_publicnic_state(router, host, "eth2")

        # 4. get a free public ip, assign to network, and create port forwarding rules (ssh) to the vm
        ipaddress = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            networkid=self.network1.id,
        )
        nat_rule = NATRule.create(
            self.apiclient,
            self.virtual_machine1,
            self.services["natrule"],
            ipaddressid=ipaddress.ipaddress.id,
            openfirewall=True
        )
        #   verify the available nics in VR should be "eth0,eth1,eth2"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP/new ip
        routers = self.get_routers(self.network1.id)
        for router in routers:
            host = self.get_router_host(router)
            self.verify_network_interfaces_in_router(router, host, "eth0,eth1,eth2,")
            guestIp, controlIp, sourcenatIp = self.get_router_ips(router)
            self.verify_ip_address_in_router(router, host, guestIp, "eth0", True)
            self.verify_ip_address_in_router(router, host, controlIp, "eth1", True)
            self.verify_ip_address_in_router(router, host, sourcenatIp, "eth2", True)
            self.verify_ip_address_in_router(router, host, ipaddress.ipaddress.ipaddress, "eth2", True)
            self.verify_router_publicnic_state(router, host, "eth2")

        # 5. release the new ip
        ipaddress.delete(self.apiclient)

        #   verify the available nics in VR should be "eth0,eth1,eth2"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP
        routers = self.get_routers(self.network1.id)
        for router in routers:
            host = self.get_router_host(router)
            self.verify_network_interfaces_in_router(router, host, "eth0,eth1,eth2,")
            guestIp, controlIp, sourcenatIp = self.get_router_ips(router)
            self.verify_ip_address_in_router(router, host, guestIp, "eth0", True)
            self.verify_ip_address_in_router(router, host, controlIp, "eth1", True)
            self.verify_ip_address_in_router(router, host, sourcenatIp, "eth2", True)
            self.verify_ip_address_in_router(router, host, ipaddress.ipaddress.ipaddress, "eth2", False)
            self.verify_router_publicnic_state(router, host, "eth2")

        # 6. create new public ip range 1
        self.services["publiciprange"]["zoneid"] = self.zone.id
        self.services["publiciprange"]["forvirtualnetwork"] = "true"
        random_subnet_number = random.randrange(10,50)
        self.services["publiciprange"]["vlan"] = get_free_vlan(
            self.apiclient,
            self.zone.id)[1]
        self.services["publiciprange"]["gateway"] = "172.16." + str(random_subnet_number) + ".1"
        self.services["publiciprange"]["startip"] = "172.16." + str(random_subnet_number) + ".2"
        self.services["publiciprange"]["endip"] = "172.16." + str(random_subnet_number) + ".10"
        self.services["publiciprange"]["netmask"] = "255.255.255.0"
        self.public_ip_range1 = PublicIpRange.create(
            self.apiclient,
            self.services["publiciprange"]
        )
        self.cleanup.append(self.public_ip_range1)

        # 7. get a free ip 4 in new ip range 2, assign to network, and enable static nat to vm
        ip_address_1 = self.get_free_ipaddress(self.public_ip_range1.vlan.id)
        ipaddress_1 = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            networkid=self.network1.id,
            ipaddress=ip_address_1
        )

        StaticNATRule.enable(
            self.apiclient,
            virtualmachineid=self.virtual_machine1.id,
            ipaddressid=ipaddress_1.ipaddress.id,
            networkid=self.network1.id
        )

        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 1
        routers = self.get_routers(self.network1.id)
        for router in routers:
            host = self.get_router_host(router)
            self.verify_network_interfaces_in_router(router, host, "eth0,eth1,eth2,eth3,")
            guestIp, controlIp, sourcenatIp = self.get_router_ips(router)
            self.verify_ip_address_in_router(router, host, guestIp, "eth0", True)
            self.verify_ip_address_in_router(router, host, controlIp, "eth1", True)
            self.verify_ip_address_in_router(router, host, sourcenatIp, "eth2", True)
            self.verify_ip_address_in_router(router, host, ipaddress_1.ipaddress.ipaddress, "eth3", True)
            self.verify_router_publicnic_state(router, host, "eth2|eth3")

        # 8. get a free ip in new ip range, assign to network, and create port forwarding rules (ssh) to the vm
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 1, new ip 2,
        ip_address_2 = self.get_free_ipaddress(self.public_ip_range1.vlan.id)
        ipaddress_2 = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            networkid=self.network1.id,
            ipaddress=ip_address_2
        )

        nat_rule = NATRule.create(
            self.apiclient,
            self.virtual_machine1,
            self.services["natrule"],
            ipaddressid=ipaddress_2.ipaddress.id,
            openfirewall=True
        )
        routers = self.get_routers(self.network1.id)
        for router in routers:
            host = self.get_router_host(router)
            self.verify_network_interfaces_in_router(router, host, "eth0,eth1,eth2,eth3,")
            guestIp, controlIp, sourcenatIp = self.get_router_ips(router)
            self.verify_ip_address_in_router(router, host, guestIp, "eth0", True)
            self.verify_ip_address_in_router(router, host, controlIp, "eth1", True)
            self.verify_ip_address_in_router(router, host, sourcenatIp, "eth2", True)
            self.verify_ip_address_in_router(router, host, ipaddress_1.ipaddress.ipaddress, "eth3", True)
            self.verify_ip_address_in_router(router, host, ipaddress_2.ipaddress.ipaddress, "eth3", True)
            self.verify_router_publicnic_state(router, host, "eth2|eth3")

        # 9. get a free ip in new ip range, assign to network, and create port forwarding rules (ssh) to the vm
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 1, new ip 2, new ip 3
        ip_address_3 = self.get_free_ipaddress(self.public_ip_range1.vlan.id)
        ipaddress_3 = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            networkid=self.network1.id,
            ipaddress=ip_address_3
        )

        nat_rule = NATRule.create(
            self.apiclient,
            self.virtual_machine1,
            self.services["natrule"],
            ipaddressid=ipaddress_3.ipaddress.id,
            openfirewall=True
        )
        routers = self.get_routers(self.network1.id)
        for router in routers:
            host = self.get_router_host(router)
            self.verify_network_interfaces_in_router(router, host, "eth0,eth1,eth2,eth3,")
            guestIp, controlIp, sourcenatIp = self.get_router_ips(router)
            self.verify_ip_address_in_router(router, host, guestIp, "eth0", True)
            self.verify_ip_address_in_router(router, host, controlIp, "eth1", True)
            self.verify_ip_address_in_router(router, host, sourcenatIp, "eth2", True)
            self.verify_ip_address_in_router(router, host, ipaddress_1.ipaddress.ipaddress, "eth3", True)
            self.verify_ip_address_in_router(router, host, ipaddress_2.ipaddress.ipaddress, "eth3", True)
            self.verify_ip_address_in_router(router, host, ipaddress_3.ipaddress.ipaddress, "eth3", True)
            self.verify_router_publicnic_state(router, host, "eth2|eth3")

        # 10. release new ip 2
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 1, new ip 3
        ipaddress_2.delete(self.apiclient)

        routers = self.get_routers(self.network1.id)
        for router in routers:
            host = self.get_router_host(router)
            self.verify_network_interfaces_in_router(router, host, "eth0,eth1,eth2,eth3,")
            guestIp, controlIp, sourcenatIp = self.get_router_ips(router)
            self.verify_ip_address_in_router(router, host, guestIp, "eth0", True)
            self.verify_ip_address_in_router(router, host, controlIp, "eth1", True)
            self.verify_ip_address_in_router(router, host, sourcenatIp, "eth2", True)
            self.verify_ip_address_in_router(router, host, ipaddress_1.ipaddress.ipaddress, "eth3", True)
            self.verify_ip_address_in_router(router, host, ipaddress_2.ipaddress.ipaddress, "eth3", False)
            self.verify_ip_address_in_router(router, host, ipaddress_3.ipaddress.ipaddress, "eth3", True)
            self.verify_router_publicnic_state(router, host, "eth2|eth3")

        # 11. release new ip 1
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 3
        ipaddress_1.delete(self.apiclient)
        routers = self.get_routers(self.network1.id)
        for router in routers:
            host = self.get_router_host(router)
            self.verify_network_interfaces_in_router(router, host, "eth0,eth1,eth2,eth3,")
            guestIp, controlIp, sourcenatIp = self.get_router_ips(router)
            self.verify_ip_address_in_router(router, host, guestIp, "eth0", True)
            self.verify_ip_address_in_router(router, host, controlIp, "eth1", True)
            self.verify_ip_address_in_router(router, host, sourcenatIp, "eth2", True)
            self.verify_ip_address_in_router(router, host, ipaddress_1.ipaddress.ipaddress, "eth3", False)
            self.verify_ip_address_in_router(router, host, ipaddress_2.ipaddress.ipaddress, "eth3", False)
            self.verify_ip_address_in_router(router, host, ipaddress_3.ipaddress.ipaddress, "eth3", True)
            self.verify_router_publicnic_state(router, host, "eth2|eth3")

        # 12. create new public ip range 2
        self.services["publiciprange"]["zoneid"] = self.zone.id
        self.services["publiciprange"]["forvirtualnetwork"] = "true"
        self.services["publiciprange"]["vlan"] = get_free_vlan(
            self.apiclient,
            self.zone.id)[1]
        self.services["publiciprange"]["gateway"] = "172.16." + str(random_subnet_number + 1) + ".1"
        self.services["publiciprange"]["startip"] = "172.16." + str(random_subnet_number + 1) + ".2"
        self.services["publiciprange"]["endip"] = "172.16." + str(random_subnet_number + 1) + ".10"
        self.services["publiciprange"]["netmask"] = "255.255.255.0"
        self.public_ip_range2 = PublicIpRange.create(
            self.apiclient,
            self.services["publiciprange"]
        )
        self.cleanup.append(self.public_ip_range2)

        # 13. get a free ip 4 in new ip range 2, assign to network, and create port forwarding rules (ssh) to the vm
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3,eth4,"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 3, eth4 -> new ip 4

        ip_address_4 = self.get_free_ipaddress(self.public_ip_range2.vlan.id)
        ipaddress_4 = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            networkid=self.network1.id,
            ipaddress=ip_address_4
        )

        StaticNATRule.enable(
            self.apiclient,
            virtualmachineid=self.virtual_machine1.id,
            ipaddressid=ipaddress_4.ipaddress.id,
            networkid=self.network1.id
        )


        routers = self.get_routers(self.network1.id)
        for router in routers:
            host = self.get_router_host(router)
            self.verify_network_interfaces_in_router(router, host, "eth0,eth1,eth2,eth3,eth4,")
            guestIp, controlIp, sourcenatIp = self.get_router_ips(router)
            self.verify_ip_address_in_router(router, host, guestIp, "eth0", True)
            self.verify_ip_address_in_router(router, host, controlIp, "eth1", True)
            self.verify_ip_address_in_router(router, host, sourcenatIp, "eth2", True)
            self.verify_ip_address_in_router(router, host, ipaddress_3.ipaddress.ipaddress, "eth3", True)
            self.verify_ip_address_in_router(router, host, ipaddress_4.ipaddress.ipaddress, "eth4", True)
            self.verify_router_publicnic_state(router, host, "eth2|eth3|eth4")

        # 14. get a free ip 5 in new ip range 2, assign to network, and create port forwarding rules (ssh) to the vm
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3,eth4,"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 3, eth4 -> new ip 4/5
        ip_address_5 = self.get_free_ipaddress(self.public_ip_range2.vlan.id)
        ipaddress_5 = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            networkid=self.network1.id,
            ipaddress=ip_address_5
        )

        nat_rule = NATRule.create(
            self.apiclient,
            self.virtual_machine1,
            self.services["natrule"],
            ipaddressid=ipaddress_5.ipaddress.id,
            openfirewall=True
        )
        routers = self.get_routers(self.network1.id)
        for router in routers:
            host = self.get_router_host(router)
            self.verify_network_interfaces_in_router(router, host, "eth0,eth1,eth2,eth3,eth4,")
            guestIp, controlIp, sourcenatIp = self.get_router_ips(router)
            self.verify_ip_address_in_router(router, host, guestIp, "eth0", True)
            self.verify_ip_address_in_router(router, host, controlIp, "eth1", True)
            self.verify_ip_address_in_router(router, host, sourcenatIp, "eth2", True)
            self.verify_ip_address_in_router(router, host, ipaddress_3.ipaddress.ipaddress, "eth3", True)
            self.verify_ip_address_in_router(router, host, ipaddress_4.ipaddress.ipaddress, "eth4", True)
            self.verify_ip_address_in_router(router, host, ipaddress_5.ipaddress.ipaddress, "eth4", True)
            self.verify_router_publicnic_state(router, host, "eth2|eth3|eth4")

        # 15. get a free ip 6 in new ip range 2, assign to network, and create port forwarding rules (ssh) to the vm
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3,eth4,"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 3, eth4 -> new ip 4/5/6
        ip_address_6 = self.get_free_ipaddress(self.public_ip_range2.vlan.id)
        ipaddress_6 = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            networkid=self.network1.id,
            ipaddress=ip_address_6
        )

        nat_rule = NATRule.create(
            self.apiclient,
            self.virtual_machine1,
            self.services["natrule"],
            ipaddressid=ipaddress_6.ipaddress.id,
            openfirewall=True
        )
        routers = self.get_routers(self.network1.id)
        for router in routers:
            host = self.get_router_host(router)
            self.verify_network_interfaces_in_router(router, host, "eth0,eth1,eth2,eth3,eth4,")
            guestIp, controlIp, sourcenatIp = self.get_router_ips(router)
            self.verify_ip_address_in_router(router, host, guestIp, "eth0", True)
            self.verify_ip_address_in_router(router, host, controlIp, "eth1", True)
            self.verify_ip_address_in_router(router, host, sourcenatIp, "eth2", True)
            self.verify_ip_address_in_router(router, host, ipaddress_3.ipaddress.ipaddress, "eth3", True)
            self.verify_ip_address_in_router(router, host, ipaddress_4.ipaddress.ipaddress, "eth4", True)
            self.verify_ip_address_in_router(router, host, ipaddress_5.ipaddress.ipaddress, "eth4", True)
            self.verify_ip_address_in_router(router, host, ipaddress_6.ipaddress.ipaddress, "eth4", True)
            self.verify_router_publicnic_state(router, host, "eth2|eth3|eth4")

        # 16. release new ip 5
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3,eth4,"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 3, eth4 -> new ip 4/6
        ipaddress_5.delete(self.apiclient)

        routers = self.get_routers(self.network1.id)
        for router in routers:
            host = self.get_router_host(router)
            self.verify_network_interfaces_in_router(router, host, "eth0,eth1,eth2,eth3,eth4,")
            guestIp, controlIp, sourcenatIp = self.get_router_ips(router)
            self.verify_ip_address_in_router(router, host, guestIp, "eth0", True)
            self.verify_ip_address_in_router(router, host, controlIp, "eth1", True)
            self.verify_ip_address_in_router(router, host, sourcenatIp, "eth2", True)
            self.verify_ip_address_in_router(router, host, ipaddress_3.ipaddress.ipaddress, "eth3", True)
            self.verify_ip_address_in_router(router, host, ipaddress_4.ipaddress.ipaddress, "eth4", True)
            self.verify_ip_address_in_router(router, host, ipaddress_5.ipaddress.ipaddress, "eth4", False)
            self.verify_ip_address_in_router(router, host, ipaddress_6.ipaddress.ipaddress, "eth4", True)
            self.verify_router_publicnic_state(router, host, "eth2|eth3|eth4")

        # 17. release new ip 4
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3,eth4,"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 3, eth4 -> new ip 6
        ipaddress_4.delete(self.apiclient)
        routers = self.get_routers(self.network1.id)
        for router in routers:
            host = self.get_router_host(router)
            self.verify_network_interfaces_in_router(router, host, "eth0,eth1,eth2,eth3,eth4,")
            guestIp, controlIp, sourcenatIp = self.get_router_ips(router)
            self.verify_ip_address_in_router(router, host, guestIp, "eth0", True)
            self.verify_ip_address_in_router(router, host, controlIp, "eth1", True)
            self.verify_ip_address_in_router(router, host, sourcenatIp, "eth2", True)
            self.verify_ip_address_in_router(router, host, ipaddress_3.ipaddress.ipaddress, "eth3", True)
            self.verify_ip_address_in_router(router, host, ipaddress_4.ipaddress.ipaddress, "eth4", False)
            self.verify_ip_address_in_router(router, host, ipaddress_5.ipaddress.ipaddress, "eth4", False)
            self.verify_ip_address_in_router(router, host, ipaddress_6.ipaddress.ipaddress, "eth4", True)
            self.verify_router_publicnic_state(router, host, "eth2|eth3|eth4")

        # 18. release new ip 3
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth4,"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth4 -> new ip 6
        ipaddress_3.delete(self.apiclient)
        routers = self.get_routers(self.network1.id)
        for router in routers:
            host = self.get_router_host(router)
            self.verify_network_interfaces_in_router(router, host, "eth0,eth1,eth2,eth4,")
            guestIp, controlIp, sourcenatIp = self.get_router_ips(router)
            self.verify_ip_address_in_router(router, host, guestIp, "eth0", True)
            self.verify_ip_address_in_router(router, host, controlIp, "eth1", True)
            self.verify_ip_address_in_router(router, host, sourcenatIp, "eth2", True)
            self.verify_ip_address_in_router(router, host, ipaddress_4.ipaddress.ipaddress, "eth4", False)
            self.verify_ip_address_in_router(router, host, ipaddress_5.ipaddress.ipaddress, "eth4", False)
            self.verify_ip_address_in_router(router, host, ipaddress_6.ipaddress.ipaddress, "eth4", True)
            self.verify_router_publicnic_state(router, host, "eth2|eth4")

        # 19. restart network
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth4,"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth4 -> new ip 6
        self.network1.restart(self.apiclient)
        routers = self.get_routers(self.network1.id)
        for router in routers:
            host = self.get_router_host(router)
            self.verify_network_interfaces_in_router(router, host, "eth0,eth1,eth2,eth4,")
            guestIp, controlIp, sourcenatIp = self.get_router_ips(router)
            self.verify_ip_address_in_router(router, host, guestIp, "eth0", True)
            self.verify_ip_address_in_router(router, host, controlIp, "eth1", True)
            self.verify_ip_address_in_router(router, host, sourcenatIp, "eth2", True)
            self.verify_ip_address_in_router(router, host, ipaddress_4.ipaddress.ipaddress, "eth4", False)
            self.verify_ip_address_in_router(router, host, ipaddress_5.ipaddress.ipaddress, "eth4", False)
            self.verify_ip_address_in_router(router, host, ipaddress_6.ipaddress.ipaddress, "eth4", True)
            self.verify_router_publicnic_state(router, host, "eth2|eth4")

        # 20. reboot router
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3,"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 6
        if len(routers) > 0:
            router = routers[0]
            cmd = rebootRouter.rebootRouterCmd()
            cmd.id = router.id
            self.apiclient.rebootRouter(cmd)
            router = self.get_router(router.id)
            host = self.get_router_host(router)
            self.verify_network_interfaces_in_router(router, host, "eth0,eth1,eth2,eth3,")
            guestIp, controlIp, sourcenatIp = self.get_router_ips(router)
            self.verify_ip_address_in_router(router, host, guestIp, "eth0", True)
            self.verify_ip_address_in_router(router, host, controlIp, "eth1", True)
            self.verify_ip_address_in_router(router, host, sourcenatIp, "eth2", True)
            self.verify_ip_address_in_router(router, host, ipaddress_4.ipaddress.ipaddress, "eth3", False)
            self.verify_ip_address_in_router(router, host, ipaddress_5.ipaddress.ipaddress, "eth3", False)
            self.verify_ip_address_in_router(router, host, ipaddress_6.ipaddress.ipaddress, "eth3", True)
            self.verify_router_publicnic_state(router, host, "eth2|eth3")

        # 21. restart network with cleanup
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3,"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 6
        self.network1.restart(self.apiclient, cleanup=True)
        routers = self.get_routers(self.network1.id)
        for router in routers:
            host = self.get_router_host(router)
            self.verify_network_interfaces_in_router(router, host, "eth0,eth1,eth2,eth3,")
            guestIp, controlIp, sourcenatIp = self.get_router_ips(router)
            self.verify_ip_address_in_router(router, host, guestIp, "eth0", True)
            self.verify_ip_address_in_router(router, host, controlIp, "eth1", True)
            self.verify_ip_address_in_router(router, host, sourcenatIp, "eth2", True)
            self.verify_ip_address_in_router(router, host, ipaddress_4.ipaddress.ipaddress, "eth3", False)
            self.verify_ip_address_in_router(router, host, ipaddress_5.ipaddress.ipaddress, "eth3", False)
            self.verify_ip_address_in_router(router, host, ipaddress_6.ipaddress.ipaddress, "eth3", True)

        # 22. restart network with cleanup, makeredundant=true
        #   verify the available nics in VR should be "eth0,eth1,eth2,eth3,"
        #   verify the IPs in VR. eth0 -> guest nic, eth2 -> source nat IP, eth3 -> new ip 6
        self.network1.restart(self.apiclient, cleanup=True, makeredundant=True)
        routers = self.get_routers(self.network1.id)
        for router in routers:
            host = self.get_router_host(router)
            self.verify_network_interfaces_in_router(router, host, "eth0,eth1,eth2,eth3,")
            guestIp, controlIp, sourcenatIp = self.get_router_ips(router)
            self.verify_ip_address_in_router(router, host, guestIp, "eth0", True)
            self.verify_ip_address_in_router(router, host, controlIp, "eth1", True)
            self.verify_ip_address_in_router(router, host, sourcenatIp, "eth2", True)
            self.verify_ip_address_in_router(router, host, ipaddress_4.ipaddress.ipaddress, "eth3", False)
            self.verify_ip_address_in_router(router, host, ipaddress_5.ipaddress.ipaddress, "eth3", False)
            self.verify_ip_address_in_router(router, host, ipaddress_6.ipaddress.ipaddress, "eth3", True)
            self.verify_router_publicnic_state(router, host, "eth2|eth3")
