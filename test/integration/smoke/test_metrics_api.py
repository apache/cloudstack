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

import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from marvin.lib.utils import (random_gen)
from nose.plugins.attrib import attr

import time

_multiprocess_shared_ = True

class TestMetrics(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.apiclient = cls.testClient.getApiClient()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.dbclient = cls.testClient.getDbConnection()
        cls.services = cls.testClient.getParsedTestDataConfig()
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.pod = get_pod(cls.apiclient, cls.zone.id)
        cls.host = list_hosts(cls.apiclient,
                               zoneid=cls.zone.id,
                               type='Routing')[0]
        cls.cluster = cls.apiclient.listClusters(listClusters.listClustersCmd())[0]
        cls._cleanup = []
        cls.disk_offering = DiskOffering.create(
            cls.apiclient,
            cls.services["disk_offering"]
        )
        cls._cleanup.append(cls.disk_offering)
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offering"]
        )
        cls.template = get_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor
        )
        cls._cleanup.append(cls.service_offering)

    @classmethod
    def tearDownClass(cls):
        super(TestMetrics, cls).tearDownClass()

    def setUp(self):
        self.cleanup = []

    def tearDown(self):
        super(TestMetrics, self).tearDown();

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_list_hosts_metrics(self):

        cmd = listHostsMetrics.listHostsMetricsCmd()
        cmd.id = self.host.id
        cmd.type = 'Routing'

        host_metric = self.apiclient.listHostsMetrics(cmd)[0]

        self.assertEqual(host_metric.cpuallocated, self.host.cpuallocated)
        self.assertEqual(host_metric.memoryallocated, self.host.memoryallocated)

        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_list_clusters_metrics(self):

        cmd = listClustersMetrics.listClustersMetricsCmd()
        cmd.id = self.cluster.id

        cluster_metric = self.apiclient.listClustersMetrics(cmd)[0]

        self.assertEqual(cluster_metric.id, self.cluster.id)
        self.assertTrue(hasattr(cluster_metric, 'cpuallocated'))
        self.assertTrue(hasattr(cluster_metric, 'cpumaxdeviation'))
        self.assertTrue(hasattr(cluster_metric, 'memoryallocated'))
        self.assertTrue(hasattr(cluster_metric, 'memoryused'))
        self.assertTrue(hasattr(cluster_metric, 'memorymaxdeviation'))

        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_list_zones_metrics(self):
        cmd = listZonesMetrics.listZonesMetricsCmd()
        cmd.id = self.zone.id

        zone_metrics = self.apiclient.listZonesMetrics(cmd)[0]

        self.assertTrue(hasattr(zone_metrics, 'cpuallocated'))
        self.assertTrue(hasattr(zone_metrics, 'cpumaxdeviation'))
        self.assertTrue(hasattr(zone_metrics, 'cputotal'))
        self.assertTrue(hasattr(zone_metrics, 'cpuused'))
        self.assertTrue(hasattr(zone_metrics, 'memoryallocated'))
        self.assertTrue(hasattr(zone_metrics, 'memorymaxdeviation'))
        self.assertTrue(hasattr(zone_metrics, 'memoryused'))

        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_list_vms_metrics(self):
        #deploy VM
        self.small_virtual_machine = VirtualMachine.create(
                                        self.apiclient,
                                        self.services["virtual_machine"],
                                        serviceofferingid=self.service_offering.id,
                                        templateid=self.template.id,
                                        zoneid=self.zone.id
                                        )
        self.cleanup.append(self.small_virtual_machine)


        cmd = listVirtualMachinesMetrics.listVirtualMachinesMetricsCmd()
        cmd.id = self.small_virtual_machine.id

        lvmm = self.apiclient.listVirtualMachinesMetrics(cmd)[0]

        self.assertEqual(lvmm.id, self.small_virtual_machine.id)

        self.assertTrue(hasattr(lvmm, 'cputotal'))
        self.assertTrue(hasattr(lvmm, 'cpuused'))
        self.assertTrue(hasattr(lvmm, 'diskiowrite'))
        self.assertTrue(hasattr(lvmm, 'diskkbswrite'))
        self.assertTrue(hasattr(lvmm, 'networkread'))
        self.assertTrue(hasattr(lvmm, 'networkwrite'))

        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_list_pstorage_metrics(self):
        #list StoragePools
        sp = self.apiclient.listStoragePools(listStoragePools.listStoragePoolsCmd())[0]

        #list StoragePoolsMetrics
        cmd = listStoragePoolsMetrics.listStoragePoolsMetricsCmd()
        cmd.id = sp.id

        sp_metrics = self.apiclient.listStoragePoolsMetrics(cmd)[0]

        self.assertEqual(sp_metrics.disksizeallocated, sp.disksizeallocated)
        self.assertEqual(sp_metrics.state, sp.state)

        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_list_volumes_metrics(self):
        volume = Volume.create(
            self.apiclient,
            self.services['volume'],
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering.id
        )
        self.cleanup.append(volume)

        cmd = listVolumes.listVolumesCmd()
        cmd.id = volume.id

        lv = self.apiclient.listVolumes(cmd)[0]

        cmd = listVolumesMetrics.listVolumesMetricsCmd()
        cmd.id = lv.id
        lvm = self.apiclient.listVolumesMetrics(cmd)[0]

        self.assertEqual(lv.size, lvm.size)
        self.assertTrue(hasattr(lvm, 'diskBytesReadRate'))
        self.assertTrue(hasattr(lvm, 'diskBytesWriteRate'))
        self.assertTrue(hasattr(lvm, 'diskIopsReadRate'))
        self.assertTrue(hasattr(lvm, 'diskIopsWriteRate'))

        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_list_infrastructure_metrics(self):
        cmd = listInfrastructure.listInfrastructureCmd()
        li = self.apiclient.listInfrastructure(cmd)

        self.assertTrue(hasattr(li, 'clusters'))
        self.assertEqual(li.clusters, len(self.apiclient.listClusters(listClusters.listClustersCmd())))
        self.assertTrue(hasattr(li, 'hosts'))

        self.assertEqual(li.hosts, len(list_hosts(self.apiclient,
            zoneid=self.zone.id,
            type='Routing')))

        self.assertTrue(hasattr(li, 'imagestores'))
        self.assertEqual(li.imagestores, len(self.apiclient.listImageStores(listImageStores.listImageStoresCmd())))

        self.assertTrue(hasattr(li, 'pods'))
        self.assertEqual(li.pods, len(self.apiclient.listPods(listPods.listPodsCmd())))

        self.assertTrue(hasattr(li, 'routers'))

        self.assertTrue(hasattr(li, 'storagepools'))
        self.assertEqual(li.storagepools, len(self.apiclient.listStoragePools(listStoragePools.listStoragePoolsCmd())))

        self.assertTrue(hasattr(li, 'zones'))
        self.assertEqual(li.zones, len(self.apiclient.listZones(listZones.listZonesCmd())))

        self.assertTrue(hasattr(li, 'systemvms'))
        self.assertTrue(hasattr(li, 'cpusockets'))

        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_list_management_server_metrics(self):
        cmd = listManagementServersMetrics.listManagementServersMetricsCmd()
        list = self.apiclient.listManagementServersMetrics(cmd)
        cmd = listManagementServers.listManagementServersCmd()
        listMs= self.apiclient.listManagementServers(cmd)

        self.assertEqual(len(list), len(listMs))

        lmsm = list[0]
        self.assertTrue(hasattr(lmsm, 'availableprocessors'))
        self.assertTrue(isinstance(lmsm.availableprocessors, int))
        self.assertTrue(hasattr(lmsm, 'agentcount'))
        self.assertTrue(isinstance(lmsm.agentcount, int))
        self.assertTrue(hasattr(lmsm, 'sessions'))
        self.assertTrue(isinstance(lmsm.sessions, int))
        self.assertTrue(hasattr(lmsm, 'heapmemoryused'))
        self.assertTrue(isinstance(lmsm.heapmemoryused, int))
        self.assertTrue(hasattr(lmsm, 'heapmemorytotal'))
        self.assertTrue(isinstance(lmsm.heapmemorytotal, int))
        self.assertTrue(hasattr(lmsm, 'threadsblockedcount'))
        self.assertTrue(isinstance(lmsm.threadsblockedcount, int))
        self.assertTrue(hasattr(lmsm, 'threadsdaemoncount'))
        self.assertTrue(isinstance(lmsm.threadsdaemoncount, int))
        self.assertTrue(hasattr(lmsm, 'threadsrunnablecount'))
        self.assertTrue(isinstance(lmsm.threadsrunnablecount, int))
        self.assertTrue(hasattr(lmsm, 'threadsteminatedcount'))
        self.assertTrue(isinstance(lmsm.threadsteminatedcount, int))
        self.assertTrue(hasattr(lmsm, 'threadstotalcount'))
        self.assertTrue(isinstance(lmsm.threadstotalcount, int))
        self.assertTrue(hasattr(lmsm, 'threadswaitingcount'))
        self.assertTrue(isinstance(lmsm.threadswaitingcount, int))
        self.assertTrue(hasattr(lmsm, 'systemmemorytotal'))
        self.assertTrue(isinstance(lmsm.systemmemorytotal, int))
        self.assertTrue(hasattr(lmsm, 'systemmemoryfree'))
        self.assertTrue(isinstance(lmsm.systemmemoryfree, int))
        self.assertTrue(hasattr(lmsm, 'systemmemoryused'))
        self.assertTrue(isinstance(lmsm.systemmemoryused, int))
        self.assertTrue(hasattr(lmsm, 'systemmemoryvirtualsize'))
        self.assertTrue(hasattr(lmsm, 'loginfo'))
        self.assertTrue(isinstance(lmsm.loginfo, str))
        self.assertTrue(hasattr(lmsm, 'systemtotalcpucycles'))
        self.assertTrue(isinstance(lmsm.systemtotalcpucycles, float))
        self.assertTrue(hasattr(lmsm, 'systemloadaverages'))
        self.assertEqual(len(lmsm.systemloadaverages), 3)
        self.assertTrue(hasattr(lmsm, 'systemcycleusage'))
        self.assertEqual(len(lmsm.systemcycleusage), 3)
        self.assertTrue(hasattr(lmsm, 'dbislocal'))
        self.assertTrue(isinstance(lmsm.dbislocal, bool))
        self.assertTrue(hasattr(lmsm, 'usageislocal'))
        self.assertTrue(isinstance(lmsm.usageislocal, bool))
        self.assertTrue(hasattr(lmsm, 'collectiontime'))
        self.assertTrue(isinstance(lmsm.collectiontime, str))
        self.assertTrue(hasattr(lmsm, 'id'))
        self.assertTrue(isinstance(lmsm.id, str))
        self.assertTrue(hasattr(lmsm, 'name'))
        self.assertTrue(isinstance(lmsm.name, str))
        self.assertTrue(hasattr(lmsm, 'state'))
        self.assertEqual(lmsm.state, 'Up')
        self.assertTrue(hasattr(lmsm, 'version'))
        self.assertTrue(isinstance(lmsm.version, str))
        self.assertTrue(hasattr(lmsm, 'javadistribution'))
        self.assertTrue(isinstance(lmsm.javadistribution, str))
        self.assertTrue(hasattr(lmsm, 'javaversion'))
        self.assertTrue(isinstance(lmsm.javaversion, str))
        self.assertTrue(hasattr(lmsm, 'osdistribution'))
        self.assertTrue(isinstance(lmsm.osdistribution, str))
        self.assertTrue(hasattr(lmsm, 'lastserverstart'))
        self.assertTrue(isinstance(lmsm.lastserverstart, str))
        self.assertTrue(hasattr(lmsm, 'lastserverstop'))
        self.assertTrue(isinstance(lmsm.lastserverstop, str))
        self.assertTrue(hasattr(lmsm, 'lastboottime'))
        self.assertTrue(isinstance(lmsm.lastboottime, str))

        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "bla"], required_hardware="false")
    def test_list_usage_server_metrics(self):
        cmd = listUsageServerMetrics.listUsageServerMetricsCmd()
        list = self.apiclient.listUsageServerMetrics(cmd)

        self.assertTrue(hasattr(list,'collectiontime'))
        self.assertTrue(isinstance(list.collectiontime, str))
        self.assertTrue(hasattr(list, 'hostname'))
        self.assertTrue(isinstance(list.hostname, str))
        self.assertTrue(hasattr(list, 'lastheartbeat'))
        self.assertTrue(isinstance(list.lastheartbeat, str))
        self.assertTrue(hasattr(list, 'lastsuccesfuljob'))
        self.assertTrue(isinstance(list.lastsuccesfuljob, str))
        self.assertTrue(hasattr(list, 'state'))
        self.assertTrue(list.state == 'Up' or list.state == 'Down')

        return
