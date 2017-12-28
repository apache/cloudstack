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

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.dbclient = self.testClient.getDbConnection()
        self.services = self.testClient.getParsedTestDataConfig()
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.pod = get_pod(self.apiclient, self.zone.id)
        self.host = list_hosts(self.apiclient,
            zoneid=self.zone.id,
            type='Routing')[0]
        self.cluster = self.apiclient.listClusters(listClusters.listClustersCmd())[0]
        self.disk_offering = DiskOffering.create(
                                    self.apiclient,
                                    self.services["disk_offering"]
                                    )
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"]
        )
        self.template = get_test_template(
            self.apiclient,
            self.zone.id,
            self.hypervisor
        )

        self.cleanup = []
        self.cleanup.append(self.disk_offering)
        self.cleanup.append(self.service_offering)

    def tearDown(self):
        try:
            #Clean up
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_list_hosts_metrics(self):

        cmd = listHostsMetrics.listHostsMetricsCmd()
        cmd.id = self.host.id
        cmd.type = 'Routing'

        host_metric = self.apiclient.listHostsMetrics(cmd)[0]

        self.assertEqual(host_metric.cpuallocated, self.host.cpuallocated)
        self.assertEqual(host_metric.memoryallocated, self.host.memoryallocated)

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
