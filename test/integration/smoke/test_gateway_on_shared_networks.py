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

#Import Local Modules
from marvin.cloudstackAPI import *
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.base import (NetworkOffering,
                             Network,
                             ServiceOffering,
                             VirtualMachine,
                             Template,
                             PublicIpRange,
                             NIC
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               find_storage_pool_type,
                               get_builtin_template_info)
from marvin.codes import (
    PASS,
    FAILED,
    JOB_FAILED,
    JOB_CANCELLED,
    JOB_SUCCEEDED
)
from nose.plugins.attrib import attr
import time


class TestGatewayOnSharedNetwork(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            testClient = super(TestGatewayOnSharedNetwork, cls).getClsTestClient()
            cls.apiclient = testClient.getApiClient()
            cls.services = testClient.getParsedTestDataConfig()
            cls._cleanup = []
            # Get Zone, Domain and templates
            cls.domain = get_domain(cls.apiclient)
            cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
            cls.services['mode'] = cls.zone.networktype
            cls.hypervisor = testClient.getHypervisorInfo()

            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.services["service_offering"]
            )
            cls._cleanup.append(cls.service_offering)

            builtin_info = get_builtin_template_info(cls.apiclient, cls.zone.id)
            cls.services["template"]["url"] = builtin_info[0]
            cls.services["template"]["hypervisor"] = builtin_info[1]
            cls.services["template"]["format"] = builtin_info[2]

            cls.template = get_template(
                cls.apiclient,
                cls.zone.id
            )

            # # Register new template
            # cls.template = Template.register(
            #     cls.apiclient,
            #     cls.services["template"],
            #     zoneid=cls.zone.id,
            #     hypervisor=cls.hypervisor
            # )
            # cls._cleanup.append(cls.template)

            cls.services["shared_network_offering"]["specifyVlan"] = "True"
            cls.services["shared_network_offering"]["specifyIpRanges"] = "True"
            cls.shared_network_offering = NetworkOffering.create(
                cls.apiclient,
                cls.services["shared_network_offering"],
                conservemode=False
            )
            cls._cleanup.append(cls.shared_network_offering)
            NetworkOffering.update(
                cls.shared_network_offering,
                cls.apiclient,
                id=cls.shared_network_offering.id,
                state="enabled"
            )

        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Exception in setUpClass: %s" % e)


    @classmethod
    def tearDownClass(cls):
        cls.debug(f"tearing down TestGatewayOnSharedNetwork")
        super(TestGatewayOnSharedNetwork, cls).tearDownClass()

    def setUp(self):
        self.cleanup = []

    def tearDown(self):
        self.debug(f"tearing down test in TestGatewayOnSharedNetwork")
        super(TestGatewayOnSharedNetwork, self).tearDown()

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_updating_nics_on_two_shared_networks(self):
        """
        Test updating vm nics on two shared networks
        Works as follows:
        - Create two shared networks, with different IP ranges (eg network1, network2).
        - Create two VMs on each shared network (eg vm1 on network1, vm2 on network2) and stop them.
        - Add additional IP range to each shared network. The two additional IP ranges have same start IP and end IP,
            but different gateway and netmask (eg network1-iprange2, network2-iprange2).
        - Update IP address of the two vms, by specifying new IP address on additional IP ranges. The new gateway and
            netmask should be updated correctly (eg vm1 on network1-iprange2, vm2 on network2-iprange2).
        """
        # - Create two shared networks, with different IP ranges (eg network1, network2).
        self.services["shared_network"]["netmask"] = "255.255.255.0"
        self.services["shared_network"]["gateway"] = "192.168.1.1"
        self.services["shared_network"]["startip"] = "192.168.1.2"
        self.services["shared_network"]["endip"] = "192.168.1.20"
        self.services["shared_network"]["vlan"] = "111"
        testnet = self.services["shared_network"]
        self.debug(f"========= network 1 : {testnet}")

        self.network1 = Network.create(
            self.apiclient,
            self.services["shared_network"],
            networkofferingid=self.shared_network_offering.id,
            zoneid=self.zone.id,
        )
        self._cleanup.append(self.network1)

        self.services["shared_network"]["netmask"] = "255.255.255.0"
        self.services["shared_network"]["gateway"] = "192.168.2.1"
        self.services["shared_network"]["startip"] = "192.168.2.2"
        self.services["shared_network"]["endip"] = "192.168.2.20"
        self.services["shared_network"]["vlan"] = "222"
        self.network2 = Network.create(
            self.apiclient,
            self.services["shared_network"],
            networkofferingid=self.shared_network_offering.id,
            zoneid=self.zone.id,
        )
        self._cleanup.append(self.network2)

        # - Create two VMs on each shared network (eg vm1 on network1, vm2 on network2) and stop them.
        vm_data = self.services["virtual_machine"]
        self.debug(f"============virtual machine data : {vm_data}")
        virtual_machine1 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.service_offering.id,
            networkids=[self.network1.id],
            zoneid=self.zone.id
        )
        self.cleanup.append(virtual_machine1)
        virtual_machine1.stop(self.apiclient)
        virtual_machine2 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.service_offering.id,
            networkids=[self.network2.id],
            zoneid=self.zone.id
        )
        self.cleanup.append(virtual_machine2)
        virtual_machine2.stop(self.apiclient)
        # - Add additional IP range to each shared network. The two additional IP ranges have same start IP and end IP,
        #   but different gateway and netmask (eg network1-iprange2, network2-iprange2).
        self.services["vlan_ip_range"]["netmask"] = "255.255.255.224"
        self.services["vlan_ip_range"]["gateway"] = "192.168.3.1"
        self.services["vlan_ip_range"]["startip"] = "192.168.3.2"
        self.services["vlan_ip_range"]["endip"] = "192.168.3.20"
        self.services["vlan_ip_range"]["vlan"] = "111"
        range1 = PublicIpRange.create(self.apiclient, self.services["vlan_ip_range"], networkid=self.network1.id)
        self.debug(f"============range 1 : {range1}")

        self.services["vlan_ip_range"]["netmask"] = "255.255.255.192"
        self.services["vlan_ip_range"]["gateway"] = "192.168.3.21"
        self.services["vlan_ip_range"]["startip"] = "192.168.3.2"
        self.services["vlan_ip_range"]["endip"] = "192.168.3.20"
        self.services["vlan_ip_range"]["vlan"] = "222"
        range2 = PublicIpRange.create(self.apiclient, self.services["vlan_ip_range"], networkid=self.network2.id)

        # - Update IP address of the two vms, by specifying new IP address on additional IP ranges. The new gateway and
        #   netmask should be updated correctly (eg vm1 on network1-iprange2, vm2 on network2-iprange2).
        nic1id = NIC.list(self.apiclient, virtualmachineid=virtual_machine1.id, networkid=self.network1.id)[0].id
        NIC.updateIp(self.apiclient, id=nic1id, ipaddress="192.168.3.2")
        nics = NIC.list(self.apiclient, virtualmachineid=virtual_machine1.id, networkid=self.network1.id)
        self.check_nic(nics, self.network1, range1)

        nic2id = NIC.list(self.apiclient, virtualmachineid=virtual_machine2.id, networkid=self.network2.id)[0].id
        NIC.updateIp(self.apiclient, id=nic2id, ipaddress="192.168.3.2")
        nics = NIC.list(self.apiclient, virtualmachineid=virtual_machine2.id, networkid=self.network2.id)
        self.check_nic(nics, self.network2, range2)


        return

    def check_nic(self, nics, network, range):
        # network = Network.list(self.apiclient, id=network_obj.id)[0]
        self.assertEqual(
            len(nics),
            1,
            "VM should have only one nic"
        )
        self.assertNotEqual(
            nics[0].netmask,
            network.netmask,
            "netmask is from the primary range of the network"
        )
        self.assertNotEqual(
            nics[0].gateway,
            network.gateway,
            "gateway is from the primary range of the network"
        )
        self.assertEqual(
            nics[0].netmask,
            range.vlan.netmask,
            f"netmask should be from the extra range {range}, of network {network}."
        )
        self.assertEqual(
            nics[0].gateway,
            range.vlan.gateway,
            f"gateway should be from the extra range {range}, of network {network}."
        )
