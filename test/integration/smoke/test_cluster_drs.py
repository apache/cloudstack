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
Tests DRS on a cluster
"""

import logging
import time
from collections.abc import Iterable

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (migrateSystemVm, listRouters, listSystemVms)
from marvin.lib.base import (Cluster, Configurations, Host, Network, NetworkOffering, ServiceOffering, VirtualMachine,
                             Zone)
from marvin.lib.common import (get_domain, get_zone, get_template)
from marvin.lib.utils import wait_until
from marvin import jsonHelper
from nose.plugins.attrib import attr


class TestClusterDRS(cloudstackTestCase):

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
        clusters = Cluster.list(cls.apiclient, zoneid=cls.zone.id, allocationstate='Enabled')

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
                cls.cluster = Cluster(jsonHelper.jsonDump.dump(cluster))
                break

        cls.domain = get_domain(cls.apiclient)

        # 1. Create large service offering
        cls.service_offering = ServiceOffering.create(cls.apiclient, cls.services["service_offerings"]["large"])
        cls._cleanup.append(cls.service_offering)

        # 2. Create a network
        cls.services["network"]["name"] = "Test Network"
        cls.network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.services["l2-network_offering"]
        )
        cls._cleanup.append(cls.network_offering)
        NetworkOffering.update(
            cls.network_offering,
            cls.apiclient,
            id=cls.network_offering.id,
            state="enabled"
        )

        cls.network = Network.create(
            cls.apiclient,
            cls.services["l2-network"],
            networkofferingid=cls.network_offering.id,
            zoneid=cls.zone.id,
            accountid="admin",
            domainid=cls.domain.id,
        )
        cls._cleanup.append(cls.network)

        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() not in ['simulator']:
            cls.migrateSvms(cls.cluster)

    @classmethod
    def migrateSvms(cls, cluster):
        """
            for testing the balanced algorithm we must make sure there is at least as more free memory on host[1] than on
            host[0]. As a grude measure we migrate any and all system vms to host[0] before the testing commences

        :param cluster: the cluser to check
        :return: None
        """

        systemVmIds = []
        cmds = listSystemVms.listSystemVmsCmd()
        responseS = cls.apiclient.listSystemVms(cmds)
        if isinstance(responseS, Iterable):
            for svm in responseS:
                if svm.hostid != cls.hosts[0].id:
                    systemVmIds.append(svm.id)
        cmdv = listRouters.listRoutersCmd()
        responseR = cls.apiclient.listRouters(cmdv)
        if isinstance(responseR, Iterable):
            for svm in responseR:
                if svm.hostid != cls.hosts[0].id:
                    systemVmIds.append(svm.id)
        numToMigrate = len(systemVmIds)
        cls.logger.debug(f'system vms and routers to migrate -- {numToMigrate}')
        cmdM = migrateSystemVm.migrateSystemVmCmd()
        cmdM.hostId=cls.hosts[0].id
        for id in systemVmIds:
            cmdM.virtualmachineid=id
            responseM = cls.apiclient.migrateSystemVm(cmdM)
            cls.logger.debug(f'migrated {responseM}')


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

    def get_vm_host_id(cls, vm_id):
        list_vms = VirtualMachine.list(cls.apiclient, id=vm_id)
        vm = list_vms[0]
        return vm.hostid

    def wait_for_vm_start(self, vm):
        """ Wait until vm is Running """
        def check_vm_state():
            vms = VirtualMachine.list(
                self.apiclient,
                id=vm.id,
                listall=True
            )
            if isinstance(vms, list):
                if vms[0].state == 'Running':
                    return True, vms[0].state
            return False, vms[0].state

        res = wait_until(10, 30, check_vm_state)
        if not res:
            raise Exception("Failed to wait for VM %s (%s) to be Running" % (vm.name, vm.id))
        return res

    def wait_for_plan_completion(self, plan):
        """ Wait until plan is completed """
        def check_plan_status():
            plans = self.cluster.listDrsPlans(self.apiclient, id=plan.id)
            if isinstance(plans, list):
                if plans[0].status == 'COMPLETED':
                    return True, plans[0].status
            return False, plans[0].status

        res = wait_until(10, 30, check_plan_status)
        if not res:
            raise Exception("Failed to wait for completion of plan %s" % (plan.id))
        return res

    def get_migrations(self):
        """ Wait until migrations are generated. Sometimes it takes a little bit of time for stats to get updated. We generate migrations
        until we get at least one migration """
        def generate_migrations():
            drs_plan = self.cluster.generateDrsPlan(self.apiclient, migrations=4)
            if len(drs_plan["migrations"]) > 0:
                return True, drs_plan["migrations"]
            return False, drs_plan["migrations"]

        res, migrations = wait_until(10, 30, generate_migrations)
        if not res:
            raise Exception("Failed to generate drs migrations")
        return migrations

    @attr(tags=["advanced"], required_hardware="false")
    def test_01_condensed_drs_algorithm(self):
        """ Verify DRS algorithm - condensed"""
        # 1. Deploy vm-1 on host 1
        # 2. Deploy vm-2 on host 2
        # 3. Execute DRS to move all VMs on the same host
        self.logger.debug("=== Running test_01_condensed_drs_algorithm ===")

        # 1. Deploy vm-1 on host 1
        self.services["virtual_machine"]["name"] = "virtual-machine-1"
        self.services["virtual_machine"]["displayname"] = "virtual-machine-1"
        self.virtual_machine_1 = VirtualMachine.create(self.apiclient, self.services["virtual_machine"],
                                                       serviceofferingid=self.service_offering.id,
                                                       templateid=self.template.id, zoneid=self.zone.id,
                                                       networkids=self.network.id, hostid=self.hosts[0].id)
        self.cleanup.append(self.virtual_machine_1)
        vm_1_host_id = self.get_vm_host_id(self.virtual_machine_1.id)

        # 2. Deploy vm-2 on host 2
        self.services["virtual_machine"]["name"] = "virtual-machine-2"
        self.services["virtual_machine"]["displayname"] = "virtual-machine-2"
        self.virtual_machine_2 = VirtualMachine.create(self.apiclient, self.services["virtual_machine"],
                                                       serviceofferingid=self.service_offering.id,
                                                       templateid=self.template.id, zoneid=self.zone.id,
                                                       networkids=self.network.id, hostid=self.hosts[1].id)
        self.cleanup.append(self.virtual_machine_2)
        vm_2_host_id = self.get_vm_host_id(self.virtual_machine_2.id)

        self.assertNotEqual(vm_1_host_id, vm_2_host_id, msg="Both VMs should be on different hosts")
        self.wait_for_vm_start(self.virtual_machine_1)
        self.wait_for_vm_start(self.virtual_machine_2)

        # 3. Generate & execute DRS to move all VMs on the same host
        Configurations.update(self.apiclient, "drs.algorithm", "condensed", clusterid=self.cluster.id)
        Configurations.update(self.apiclient, "drs.imbalance", "1.0", clusterid=self.cluster.id)

        migrations = self.get_migrations()
        vm_to_dest_host_map = {
            migration["virtualmachineid"]: migration["destinationhostid"] for migration in migrations
        }

        self.assertEqual(len(vm_to_dest_host_map), 1, msg="DRS plan should have 1 migrations")

        executed_plan = self.cluster.executeDrsPlan(self.apiclient, vm_to_dest_host_map)
        self.wait_for_plan_completion(executed_plan)

        vm_1_host_id = self.get_vm_host_id(self.virtual_machine_1.id)
        vm_2_host_id = self.get_vm_host_id(self.virtual_machine_2.id)

        self.assertEqual(vm_1_host_id, vm_2_host_id, msg="Both VMs should be on the same host")

    @attr(tags=["advanced"], required_hardware="false")
    def test_02_balanced_drs_algorithm(self):
        """
            Verify DRS algorithm - balanced

            # 1. Deploy vm-1 on host 1
            # 2. Deploy vm-2 on host 2
            # 3. Execute DRS to move all VMs on different hosts
        """
        self.logger.debug("=== Running test_02_balanced_drs_algorithm ===")

        # 1. Deploy vm-1 on host 1
        self.services["virtual_machine"]["name"] = "virtual-machine-1"
        self.services["virtual_machine"]["displayname"] = "virtual-machine-1"
        self.virtual_machine_1 = VirtualMachine.create(self.apiclient, self.services["virtual_machine"],
                                                       serviceofferingid=self.service_offering.id,
                                                       templateid=self.template.id, zoneid=self.zone.id,
                                                       networkids=self.network.id, hostid=self.hosts[0].id)
        self.cleanup.append(self.virtual_machine_1)
        vm_1_host_id = self.get_vm_host_id(self.virtual_machine_1.id)

        # 2. Deploy vm-2 on host 1
        self.services["virtual_machine"]["name"] = "virtual-machine-2"
        self.services["virtual_machine"]["displayname"] = "virtual-machine-2"
        self.virtual_machine_2 = VirtualMachine.create(self.apiclient, self.services["virtual_machine"],
                                                       serviceofferingid=self.service_offering.id,
                                                       templateid=self.template.id, zoneid=self.zone.id,
                                                       networkids=self.network.id, hostid=self.hosts[0].id)
        self.cleanup.append(self.virtual_machine_2)
        vm_2_host_id = self.get_vm_host_id(self.virtual_machine_2.id)

        self.assertEqual(vm_1_host_id, vm_2_host_id, msg="Both VMs should be on same hosts")
        self.wait_for_vm_start(self.virtual_machine_1)
        self.wait_for_vm_start(self.virtual_machine_2)

        # 3. Execute DRS to move all VMs on different hosts
        Configurations.update(self.apiclient, "drs.algorithm", "balanced", clusterid=self.cluster.id)
        Configurations.update(self.apiclient, "drs.imbalance", "1.0", clusterid=self.cluster.id)

        migrations = self.get_migrations()
        vm_to_dest_host_map = {
            migration["virtualmachineid"]: migration["destinationhostid"] for migration in migrations
        }

        # this is one if no svm is considered to be migrated, it might be higher
        self.assertTrue(len(vm_to_dest_host_map) >= 1, msg="DRS plan should have at least 1 migrations")

        executed_plan = self.cluster.executeDrsPlan(self.apiclient, vm_to_dest_host_map)
        self.wait_for_plan_completion(executed_plan)

        vm_1_host_id = self.get_vm_host_id(self.virtual_machine_1.id)
        vm_2_host_id = self.get_vm_host_id(self.virtual_machine_2.id)

        self.assertTrue(
            vm_1_host_id != self.virtual_machine_1.hostid or vm_2_host_id != self.virtual_machine_2.hostid,
                        msg="At least one VM should have been migrated to a different host")
