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
Tests of Non-Strict (host anti-affinity and host affinity) affinity groups
"""

import logging
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Cluster, Configurations, Host, ServiceOffering, VirtualMachine, Zone)
from marvin.lib.common import (get_domain, get_zone, get_template)
from nose.plugins.attrib import attr


class TestClusterDRS(cloudstackTestCase):
    """
    Test Non-Strict (host anti-affinity and host affinity) affinity groups
    """

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestClusterDRS, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.zone = Zone(zone.__dict__)
        cls.template = get_template(cls.apiclient, cls.zone.id)
        cls._cleanup = []

        cls.logger = logging.getLogger("TestClusterDRS")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        cls.skipTests = False
        clusters = Cluster.list(cls.apiclient, zoneid=cls.zone.id, allocationstate='Enabled',
                                clustertype='CloudManaged')

        if not clusters or not isinstance(clusters, list) or len(clusters) < 1:
            cls.logger.debug("This test requires at least 1 (Up and Enabled) cluster in the zone")
            cls.skipTests = True
            return

        for cluster in clusters:
            cls.hosts = Host.list(cls.apiclient, zoneid=cls.zone.id, clusterid=cluster.id, state='Up',
                                  resourcestate='Enabled')
            if not cls.hosts or not isinstance(cls.hosts, list) or len(cls.hosts) < 2:
                cls.logger.debug("This test requires at least two (Up and Enabled) hosts in the zone")
                cls.skipTests = True
                return
            else:
                cls.cluster = cluster
                break

        cls.domain = get_domain(cls.apiclient)

        # 1. Create small service offering
        cls.service_offering = ServiceOffering.create(cls.apiclient, cls.services["service_offerings"]["small"])
        cls._cleanup.append(cls.service_offering)

    @classmethod
    def tearDownClass(cls):
        super(TestClusterDRS, cls).tearDownClass()

    def setUp(self):
        if self.skipTests:
            self.skipTest("This test requires at least two (Up and Enabled) hosts in the zone")
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        super(TestClusterDRS, self).tearDown()

    @classmethod
    def get_vm_host_id(cls, vm_id):
        list_vms = VirtualMachine.list(cls.apiclient, id=vm_id)
        vm = list_vms[0]
        return vm.hostid

    @attr(tags=["advanced"], required_hardware="false")
    def test_01_condensed_drs_algorithm(self):
        """ Verify Non-Strict host anti-affinity """
        # 1. Deploy vm-1 on host 1
        # 2. Deploy vm-2 on host 2
        # 3. Execute DRS to move all VMs on the same host
        self.logger.debug("=== Running test_01_condensed_drs_algorithm ===")

        # 1. Deploy vm-1 on host 1
        self.services["virtual_machine"]["name"] = "virtual-machine-1"
        self.services["virtual_machine"]["displayname"] = "virtual-machine-1"
        self.services["virtual_machine"]["displayname"] = "virtual-machine-1"
        self.virtual_machine_1 = VirtualMachine.create(self.apiclient, self.services["virtual_machine"],
                                                       serviceofferingid=self.service_offering.id,
                                                       templateid=self.template.id, zoneid=self.zone.id,
                                                       networkids=self.user_network.id, hostid=self.hosts[0].id)
        self.cleanup.append(self.virtual_machine_1)
        vm_1_host_id = self.get_vm_host_id(self.virtual_machine_1.id)

        # 2. Deploy vm-2 on host 2
        self.services["virtual_machine"]["name"] = "virtual-machine-2"
        self.services["virtual_machine"]["displayname"] = "virtual-machine-2"
        self.virtual_machine_2 = VirtualMachine.create(self.apiclient, self.services["virtual_machine"],
                                                       serviceofferingid=self.service_offering.id,
                                                       templateid=self.template.id, zoneid=self.zone.id,
                                                       networkids=self.user_network.id, hostid=self.hosts[1].id)
        vm_2_host_id = self.get_vm_host_id(self.virtual_machine_2.id)
        self.cleanup.append(self.virtual_machine_2)

        self.assertNotEqual(vm_1_host_id, vm_2_host_id, msg="Both VMs should be on different hosts")

        # 3. Execute DRS to move all VMs on the same host
        Configurations.update(self.apiclient, "drs.algorithm", "condensed")
        Configurations.update(self.apiclient, "drs.imbalance.threshold", "1")
        self.cluster.executeDRS(iterations=1.0)

        vm_1_host_id = self.get_vm_host_id(self.virtual_machine_1.id)
        vm_2_host_id = self.get_vm_host_id(self.virtual_machine_2.id)
        self.assertEqual(vm_1_host_id, vm_2_host_id, msg="Both VMs should be on the same host")

    @attr(tags=["advanced"], required_hardware="false")
    def test_02_balanced_drs_algorithm(self):
        """ Verify Non-Strict host anti-affinity """

        # 1. Deploy vm-1 on host 1
        # 2. Deploy vm-2 on host 2
        # 3. Execute DRS to move all VMs on different hosts

        self.logger.debug("=== Running test_02_balanced_drs_algorithm ===")
        # 1. Deploy vm-1 on host 1
        self.services["virtual_machine"]["name"] = "virtual-machine-1"
        self.services["virtual_machine"]["displayname"] = "virtual-machine-1"
        self.services["virtual_machine"]["displayname"] = "virtual-machine-1"
        self.virtual_machine_1 = VirtualMachine.create(self.apiclient, self.services["virtual_machine"],
                                                       serviceofferingid=self.service_offering.id,
                                                       templateid=self.template.id, zoneid=self.zone.id,
                                                       networkids=self.user_network.id, hostid=self.hosts[0].id)
        self.cleanup.append(self.virtual_machine_1)
        vm_1_host_id = self.get_vm_host_id(self.virtual_machine_1.id)

        # 2. Deploy vm-2 on host 1
        self.services["virtual_machine"]["name"] = "virtual-machine-2"
        self.services["virtual_machine"]["displayname"] = "virtual-machine-2"
        self.virtual_machine_2 = VirtualMachine.create(self.apiclient, self.services["virtual_machine"],
                                                       serviceofferingid=self.service_offering.id,
                                                       templateid=self.template.id, zoneid=self.zone.id,
                                                       networkids=self.user_network.id, hostid=self.hosts[0].id)
        vm_2_host_id = self.get_vm_host_id(self.virtual_machine_2.id)
        self.cleanup.append(self.virtual_machine_2)

        self.assertEqual(vm_1_host_id, vm_2_host_id, msg="Both VMs should be on different hosts")

        # 3. Execute DRS to move all VMs on different hosts
        Configurations.update(self.apiclient, "drs.algorithm", "balanced")
        Configurations.update(self.apiclient, "drs.imbalance.threshold", "0")
        self.cluster.executeDRS(iterations=1.0)

        vm_1_host_id = self.get_vm_host_id(self.virtual_machine_1.id)
        vm_2_host_id = self.get_vm_host_id(self.virtual_machine_2.id)
        self.assertNotEqual(vm_1_host_id, vm_2_host_id, msg="Both VMs should be on different hosts")
