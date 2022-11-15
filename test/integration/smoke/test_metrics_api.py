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

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "bla"], required_hardware="false")
    def test_list_management_server_metrics(self):
        cmd = listManagementServersMetrics.listManagementServersMetricsCmd()
        listMSMs = self.apiclient.listManagementServersMetrics(cmd)
        cmd = listManagementServers.listManagementServersCmd()
        listMSs= self.apiclient.listManagementServers(cmd)

        self.assertEqual(len(listMSMs), len(listMSs))

        metrics = listMSMs[0]
        self.assertTrue(hasattr(metrics, 'availableprocessors'))
        self.assertTrue(isinstance(metrics.availableprocessors, int))
        self.assertTrue(hasattr(metrics, 'agentcount'))
        self.assertTrue(isinstance(metrics.agentcount, int))
        self.assertTrue(hasattr(metrics, 'sessions'))
        self.assertTrue(isinstance(metrics.sessions, int))

        self.assertTrue(hasattr(metrics, 'heapmemoryused'))
        self.assertTrue(isinstance(metrics.heapmemoryused, int))
        self.assertTrue(hasattr(metrics, 'heapmemorytotal'))
        self.assertTrue(isinstance(metrics.heapmemorytotal, int))
        self.assertTrue(metrics.heapmemoryused <= metrics.heapmemorytotal)

        self.assertTrue(hasattr(metrics, 'threadsblockedcount'))
        self.assertTrue(isinstance(metrics.threadsblockedcount, int))
        self.assertTrue(hasattr(metrics, 'threadsdaemoncount'))
        self.assertTrue(isinstance(metrics.threadsdaemoncount, int))
        self.assertTrue(hasattr(metrics, 'threadsrunnablecount'))
        self.assertTrue(isinstance(metrics.threadsrunnablecount, int))
        self.assertTrue(hasattr(metrics, 'threadsteminatedcount'))
        self.assertTrue(isinstance(metrics.threadsteminatedcount, int))
        self.assertTrue(hasattr(metrics, 'threadstotalcount'))
        self.assertTrue(isinstance(metrics.threadstotalcount, int))
        self.assertTrue(hasattr(metrics, 'threadswaitingcount'))
        self.assertTrue(isinstance(metrics.threadswaitingcount, int))
        self.assertTrue(metrics.threadsblockedcount   <= metrics.threadstotalcount)
        self.assertTrue(metrics.threadsdaemoncount    <= metrics.threadstotalcount)
        self.assertTrue(metrics.threadsrunnablecount  <= metrics.threadstotalcount)
        self.assertTrue(metrics.threadsteminatedcount <= metrics.threadstotalcount)
        self.assertTrue(metrics.threadswaitingcount   <= metrics.threadstotalcount)

        self.assertTrue(hasattr(metrics, 'systemmemorytotal'))
        self.assertTrue(isinstance(metrics.systemmemorytotal, str))
        self.assertTrue(hasattr(metrics, 'systemmemoryfree'))
        self.assertTrue(isinstance(metrics.systemmemoryfree, str))
        self.assertTrue(hasattr(metrics, 'systemmemoryvirtualsize'))
        self.assertTrue(isinstance(metrics.systemmemoryvirtualsize, str))

        self.assertTrue(hasattr(metrics, 'loginfo'))
        self.assertTrue(isinstance(metrics.loginfo, str))
        self.assertTrue(hasattr(metrics, 'systemtotalcpucycles'))
        self.assertTrue(isinstance(metrics.systemtotalcpucycles, float))
        self.assertTrue(hasattr(metrics, 'systemloadaverages'))
        self.assertTrue(isinstance(metrics.systemloadaverages, list))
        self.assertEqual(len(metrics.systemloadaverages), 3)
        self.assertTrue(hasattr(metrics, 'systemcycleusage'))
        self.assertTrue(isinstance(metrics.systemcycleusage, list))
        self.assertEqual(len(metrics.systemcycleusage), 3)
        self.assertTrue(hasattr(metrics, 'dbislocal'))
        self.assertTrue(isinstance(metrics.dbislocal, bool))
        self.assertTrue(hasattr(metrics, 'usageislocal'))
        self.assertTrue(isinstance(metrics.usageislocal, bool))
        self.assertTrue(hasattr(metrics, 'collectiontime'))
        self.assertTrue(isinstance(metrics.collectiontime, str))
        self.assertTrue(self.valid_date(metrics.collectiontime))
        self.assertTrue(hasattr(metrics, 'id'))
        self.assertTrue(isinstance(metrics.id, str))
        self.assertTrue(hasattr(metrics, 'name'))
        self.assertTrue(isinstance(metrics.name, str))
        self.assertTrue(hasattr(metrics, 'state'))
        self.assertEqual(metrics.state, 'Up')
        self.assertTrue(hasattr(metrics, 'version'))
        self.assertTrue(isinstance(metrics.version, str))
        self.assertTrue(hasattr(metrics, 'javadistribution'))
        self.assertTrue(isinstance(metrics.javadistribution, str))
        self.assertTrue(hasattr(metrics, 'javaversion'))
        self.assertTrue(isinstance(metrics.javaversion, str))
        self.assertTrue(hasattr(metrics, 'osdistribution'))
        self.assertTrue(isinstance(metrics.osdistribution, str))
        self.assertTrue(hasattr(metrics, 'lastserverstart'))
        self.assertTrue(isinstance(metrics.lastserverstart, str))
        self.assertTrue(self.valid_date(metrics.lastserverstart))
        if hasattr(metrics, 'lastserverstop') and metrics.lastserverstop:
            self.debug(f"=== lastserverstop {metrics.lastserverstop} ===")
            self.assertTrue(isinstance(metrics.lastserverstop, str))
            self.assertTrue(self.valid_date(metrics.lastserverstop))
        self.assertTrue(hasattr(metrics, 'lastboottime'))
        self.assertTrue(isinstance(metrics.lastboottime, str))
        self.assertTrue(self.valid_date(metrics.lastboottime))

        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_list_usage_server_metrics(self):
        cmd = listUsageServerMetrics.listUsageServerMetricsCmd()
        metrics = self.apiclient.listUsageServerMetrics(cmd)

        self.assertTrue(hasattr(metrics,'collectiontime'))
        self.assertTrue(isinstance(metrics.collectiontime, str))
        self.assertTrue(self.valid_date(metrics.collectiontime))
        self.assertTrue(hasattr(metrics, 'hostname'))
        self.assertTrue(isinstance(metrics.hostname, str))
        if hasattr(metrics, 'lastheartbeat') and metrics.lastheartbeat :
            self.debug(f"=== lastheartbeat {metrics.lastheartbeat} ===")
            self.assertTrue(isinstance(metrics.lastheartbeat, str))
            self.assertTrue(self.valid_date(metrics.lastheartbeat))
        if hasattr(metrics, 'lastsuccessfuljob') and metrics.lastsuccessfuljob:
            self.debug(f"=== lastsuccessfuljob {metrics.lastsuccessfuljob} ===")
            self.assertTrue(isinstance(metrics.lastsuccessfuljob, str))
            self.assertTrue(self.valid_date(metrics.lastsuccessfuljob))
        self.assertTrue(hasattr(metrics, 'state'))
        self.assertTrue(metrics.state == 'Up' or metrics.state == 'Down')

        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_list_db_metrics(self):
        cmd = listDbMetrics.listDbMetricsCmd()
        metrics = self.apiclient.listDbMetrics(cmd)

        self.assertTrue(hasattr(metrics,'collectiontime'))
        self.assertTrue(isinstance(metrics.collectiontime, str))
        self.assertTrue(self.valid_date(metrics.collectiontime))
        self.assertTrue(hasattr(metrics, 'connections'))
        self.assertTrue(isinstance(metrics.connections, int))

        cmd = listConfigurations.listConfigurationsCmd()
        cmd.name = 'database.server.stats.retention'
        configuration = self.apiclient.listConfigurations(cmd)
        retention = int(configuration[0].value)
        self.assertTrue(hasattr(metrics, 'dbloadaverages'))
        self.assertTrue(isinstance(metrics.dbloadaverages, list))
        self.assertTrue(len(metrics.dbloadaverages) <= retention)

        self.assertTrue(hasattr(metrics, 'hostname'))
        self.assertTrue(isinstance(metrics.hostname, str))
        self.assertTrue(hasattr(metrics, 'queries'))
        self.assertTrue(isinstance(metrics.queries, int))
        self.assertTrue(hasattr(metrics, 'replicas'))
        self.assertTrue(isinstance(metrics.replicas, list))
        self.assertTrue(hasattr(metrics, 'uptime'))
        self.assertTrue(isinstance(metrics.uptime, int))
        self.assertTrue(hasattr(metrics, 'version'))
        self.assertTrue(isinstance(metrics.version, str))
        self.assertTrue(hasattr(metrics, 'versioncomment'))
        self.assertTrue(isinstance(metrics.versioncomment, str))

        return

    def valid_date(cls, date_text):
        try:
            datetime.datetime.strptime(date_text, '%Y-%m-%dT%H:%M:%S%z')
            return True
        except ValueError:
            return False
