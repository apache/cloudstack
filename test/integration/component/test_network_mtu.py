# Licensed to the Apache Software Foundation (ASF) under one
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
""" BVT tests for Network Life Cycle
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources, get_process_status, get_host_credentials
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             NATRule,
                             PublicIPAddress,
                             StaticNATRule,
                             FireWallRule,
                             Network,
                             NetworkOffering,
                             LoadBalancerRule,
                             Router,
                             NIC,
                             Cluster)
from marvin.lib.common import (get_domain,
                               get_free_vlan,
                               get_zone,
                               get_template,
                               get_test_template,
                               list_hosts,
                               list_publicIP,
                               list_nat_rules,
                               list_routers,
                               list_virtual_machines,
                               list_lb_rules,
                               list_configurations,
                               verifyGuestTrafficPortGroups,
                               verifyNetworkState)

from nose.plugins.attrib import attr
import logging

_multiprocess_shared_ = True

logger = logging.getLogger('TestNetworkWithMtuConfiguration')
stream_handler = logging.StreamHandler()
logger.setLevel(logging.DEBUG)
logger.addHandler(stream_handler)

class TestNetworkWithMtuConfiguration(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestNetworkWithMtuConfiguration, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.dbclient = cls.testClient.getDbConnection()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.domain = get_domain(cls.api_client)
        cls.skip = False
        if cls.hypervisor.lower() == 'simulator':
            cls.skip = True

        cls.services = cls.testClient.getParsedTestDataConfig()
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][
            0].__dict__
        # Get Zone and templates
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        logger.debug("Creating Admin Account for domain %s on zone %s" % (cls.domain.id, cls.zone.id))
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls.isolated_network_offering = NetworkOffering.create(
            cls.api_client,
            cls.services["network_offering"]
        )
        cls.isolated_network_offering.update(cls.api_client, state='Enabled')

        cls._cleanup = [
            cls.service_offering,
            cls.isolated_network_offering,
            cls.account
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        if self.skip:
            self.skipTest("Skip test for simulator hypervisor")

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
            self.cleanup[:] = []
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    '''
    This test performs the following:
    1. creates an Isolated network with Public and Private MTUs being configured on the respective interfaces
        a. Set one of the MTUs to a value greater that the configured max - 1500 and observe that it gets set
        to the maximum configurable value
    2. Check router nic MTUs
    3. Validates that the network has the right MTU set in the DB
    '''

    @attr(tags=["advanced", "isolated", "network", "xx"], required_hardware="false")
    def test_01_create_isolated_network_with_mtu(self):
        public_mtu = 1450
        private_mtu = 2000
        logger.debug("Creating an isolated network with MTU defined for both public and private interfaces")
        self.isolated_network = Network.create(
            self.apiclient,
            self.services["isolated_network"],
            networkofferingid=self.isolated_network_offering.id,
            zoneid=self.zone.id,
            publicmtu=public_mtu,
            privatemtu=private_mtu)

        logger.debug("Deploying VM")
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.service_offering.id,
            networkids=[str(self.isolated_network.id)])
        self.cleanup.append(virtual_machine)
        self.cleanup.append(self.isolated_network)

        # Validate network is in the Implemented state

        response = verifyNetworkState(
            self.apiclient,
            virtual_machine.nic[0].networkid,
            "implemented")
        exceptionOccurred = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccurred or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)

        public_ips = list_publicIP(
            self.apiclient,
            associatednetworkid=self.isolated_network.id
        )

        self.assertEqual(
            isinstance(public_ips, list),
            True,
            "Check for list public IPs response return valid data"
        )

        public_ip = public_ips[0]

        logger.debug("Creating Firewall rule for VM ID: %s" % virtual_machine.id)
        FireWallRule.create(
            self.apiclient,
            ipaddressid=public_ip.id,
            protocol=self.services["natrule"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["natrule"]["publicport"],
            endport=self.services["natrule"]["publicport"]
        )

        logger.debug("Creating NAT rule for VM ID: %s" % virtual_machine.id)
        nat_rule = NATRule.create(
            self.apiclient,
            virtual_machine,
            self.services["natrule"],
            public_ip.id
        )

        logger.debug("Ping the external network with packet size greater than configured mtu %s : %s", public_mtu, 1500)
        expected = 1
        check_string = "Frag needed and DF set"
        cmd = "ping -M do -s {mtu} 8.8.8.8 -c1"
        result = self.check_router_command(virtual_machine, nat_rule.ipaddress, cmd.format(mtu="1500"), check_string, self)
        self.assertEqual(
            result,
            expected,
            "Ping to outside world from VM should be successful!"
        )

        logger.debug("Ping the external network with packet size lesser than configured mtu %s : %s", public_mtu, 1200)
        expected = 1
        check_string = "0% packet loss"
        cmd = "ping -M do -s {mtu} 8.8.8.8 -c1"
        result = self.check_router_command(virtual_machine, nat_rule.ipaddress, cmd.format(mtu="1200"), check_string, self)
        self.assertEqual(
            result,
            expected,
            "Ping to outside world from VM should be successful!"
        )

        # Verify network mtu set
        self.verify_network_mtu(self.isolated_network, public_mtu, 1500)
        self.checkRouterNicMtus(self.isolated_network, public_mtu, 1500)
        self.checkNetworkMtu(self.isolated_network, public_mtu, 1500)

    @attr(tags=["advanced", "isolated", "network", "xx"], required_hardware="false")
    def test_02_update_isolated_network_with_mtu(self):
        public_mtu = 1200
        private_mtu = 1100
        logger.debug("Creating an isolated network with MTU defined for both public and private interfaces")
        isolated_network1 = Network.create(
            self.apiclient,
            self.services["isolated_network"],
            networkofferingid=self.isolated_network_offering.id,
            zoneid=self.zone.id,
            publicmtu=public_mtu,
            privatemtu=private_mtu)

        logger.debug("Deploying VM")
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.service_offering.id,
            networkids=[str(isolated_network1.id)])
        self.cleanup.append(virtual_machine)
        self.cleanup.append(isolated_network1)

        # Validate network is in the Implemented state
        response = verifyNetworkState(
            self.apiclient,
            virtual_machine.nic[0].networkid,
            "implemented")
        exceptionOccurred = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccurred or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)

        public_ips = list_publicIP(
            self.apiclient,
            associatednetworkid=isolated_network1.id
        )

        self.assertEqual(
            isinstance(public_ips, list),
            True,
            "Check for list public IPs response return valid data"
        )

        public_ip = public_ips[0]

        logger.debug("Creating Firewall rule for VM ID: %s" % virtual_machine.id)
        FireWallRule.create(
            self.apiclient,
            ipaddressid=public_ip.id,
            protocol=self.services["natrule"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["natrule"]["publicport"],
            endport=self.services["natrule"]["publicport"]
        )

        logger.debug("Creating NAT rule for VM ID: %s" % virtual_machine.id)
        nat_rule = NATRule.create(
            self.apiclient,
            virtual_machine,
            self.services["natrule"],
            public_ip.id
        )
        # Verify network mtu set
        self.verify_network_mtu(isolated_network1, public_mtu, private_mtu)

        self.checkRouterNicMtus(isolated_network1, public_mtu, private_mtu)
        self.checkNetworkMtu(isolated_network1, public_mtu, private_mtu)

        logger.debug("Update the network's MTU")
        updated_network = isolated_network1.update(self.apiclient, id=isolated_network1.id, public_mtu=1450)

        logger.debug("Ping the external network with packet size greater than configured mtu %s : %s", public_mtu, 1500)
        expected = 1
        check_string = "Frag needed and DF set"
        cmd = "ping -M do -s {mtu} 8.8.8.8 -c1"
        result = self.check_router_command(virtual_machine, nat_rule.ipaddress, cmd.format(mtu="1500"), check_string, self)
        self.assertEqual(
            result,
            expected,
            "Ping to outside world from VM should be successful!"
        )

        logger.debug("Ping the external network with packet size lesser than configured mtu %s : %s", public_mtu, 1100)
        expected = 1
        check_string = "0% packet loss"
        cmd = "ping -M do -s {mtu} 8.8.8.8 -c1"
        result = self.check_router_command(virtual_machine, nat_rule.ipaddress, cmd.format(mtu="1200"), check_string, self)
        self.assertEqual(
            result,
            expected,
            "Ping to outside world from VM should be successful!"
        )

        self.checkRouterNicMtus(updated_network, public_mtu, private_mtu)
        self.checkNetworkMtu(updated_network, public_mtu, private_mtu)

    def verify_network_mtu(self, network, public_mtu, private_mtu):
        # Verify network mtu set
        result = self.dbclient.execute("select public_mtu, private_mtu from networks where uuid='%s'"
                                       % str(network.id))
        self.assertEqual(isinstance(result, list),
                         True,
                         "Check DB query result set for valid data")

        self.assertNotEqual(len(result),
                            0,
                            "Check DB Query result set")
        self.assertEqual(result[0][0],
                         public_mtu if public_mtu < 1500 else 1500,
                         "DB results not matching, expected: %s found: %s " % (public_mtu, result[0][0]))
        self.assertEqual(result[0][1],
                         private_mtu if private_mtu < 1500 else 1500,
                         "DB results not matching, expected: %s found: %s " % (private_mtu, result[0][1]))

    def check_router_command(self, virtual_machine, public_ip, ssh_command, check_string, test_case, retries=5):
        result = 'failed'
        try:
            ssh = virtual_machine.get_ssh_client(ipaddress=public_ip, retries=retries)
            result = str(ssh.execute(ssh_command))
        except Exception as e:
            test_case.fail("Failed to SSH into the Virtual Machine: %s" % e)

        logging.debug("Result from SSH into the Virtual Machine: %s" % result)
        return result.count(check_string)

    def checkRouterNicMtus(self, network, public_mtu, private_mtu):
        self.debug("Listing routers for network: %s" % network.name)
        self.routers = Router.list(
            self.apiclient,
            networkid=network.id,
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
            nics = router.nic
            for nic in nics:
                if nic.traffictype == 'Guest':
                    self.assertEqual(private_mtu, nic.mtu, "MTU not properly configured on private interface of VR")
                if nic.traffictype == 'Public':
                    self.assertEqual(public_mtu, nic.mtu, "MTU not properly configured on public interface of VR")

    def checkNetworkMtu(self, network, expected_public_mtu, expected_private_mtu):
        self.assertEqual(expected_public_mtu, network.publicmtu)
        self.assertEqual(expected_private_mtu, network.privatemtu)
