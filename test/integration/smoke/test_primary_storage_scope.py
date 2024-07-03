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
""" BVT tests for Primary Storage
"""

# Import System modules
# Import Local Modules
from marvin.cloudstackTestCase import *
from marvin.lib.base import (Host, StoragePool, Cluster, updateStoragePool, changeStoragePoolScope)
from marvin.lib.common import (get_zone, get_pod, list_clusters)
from marvin.lib.utils import cleanup_resources
from nose.plugins.attrib import attr

class TestPrimaryStorageScope(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.services = self.testClient.getParsedTestDataConfig()
        self.cleanup = []
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.pod = get_pod(self.apiclient, self.zone.id)
        self.debug(self.services)
        self.cluster1 = list_clusters(self.apiclient)[0]
        self.debug(self.cluster1)
        if (self.cluster1 == None):
            cloudstackTestCase.skipTest(self, "Cluster not found. Skipping test.")
        if (self.cluster1.hypervisortype not in ['KVM', 'VMware', 'Simulator']):
            cloudstackTestCase.skipTest(self, "Supported hypervisors (KVM, VMware, Simulator) not found. Skipping test.")
        self.cluster = {
            'clustername': 'C0_testScope',
            'clustertype': 'CloudManaged'
        }
        return

    def tearDown(self):
        super(TestPrimaryStorageScope, self).tearDown()

    @attr(tags=["advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_01_primary_storage_scope_change(self):
        """Test primary storage pool scope change
        """

        # Create cluster
        self.cluster2 = Cluster.create(self.apiclient,
                                       self.cluster,
                                       zoneid=self.zone.id,
                                       podid=self.pod.id,
                                       hypervisor=self.cluster1.hypervisortype
                                       )
        self.cleanup.append(self.cluster2)

        # Create zone-wide storage pool
        self.storage = StoragePool.create(self.apiclient,
                                          self.services["nfs"],
                                          scope = 'ZONE',
                                          zoneid=self.zone.id,
                                          hypervisor=self.cluster1.hypervisortype
                                          )
        self.cleanup.append(self.storage)
        self.debug("Created storage pool %s in zone scope", self.storage.id)

        # Disable storage pool
        cmd = updateStoragePool.updateStoragePoolCmd()
        cmd.id = self.storage.id
        cmd.enabled = False
        self.apiclient.updateStoragePool(cmd)

        self.debug("Disabled storage pool : %s" % self.storage.id)

        # Change storage pool scope to Cluster2
        cmd = changeStoragePoolScope.changeStoragePoolScopeCmd()
        cmd.id = self.storage.id
        cmd.scope = "CLUSTER"
        cmd.clusterid = self.cluster2.id
        self.apiclient.changeStoragePoolScope(cmd)

        self.debug("Changed scope of storage pool %s to cluster" % self.storage.id)

        pool_id = self.dbclient.execute("select id from storage_pool where uuid=\"" + self.storage.id + "\"")[0][0]
        host1 = Host.list(self.apiclient, clusterid=self.cluster1.id, listall=True)[0]
        host1_id = self.dbclient.execute("select id from host where uuid=\"" + host1.id + "\"")[0][0]

        pool_row = self.dbclient.execute("select cluster_id, pod_id, scope from storage_pool where id=" + str(pool_id))[0]
        capacity_row = self.dbclient.execute("select cluster_id, pod_id from op_host_capacity where capacity_type=3 and host_id=" + str(pool_id))[0]
        pool_host_rows = self.dbclient.execute("select id from storage_pool_host_ref where host_id=" + str(host1_id) + " and pool_id=" + str(pool_id))

        self.assertIsNotNone(
            pool_row[0],
            "Cluster id should not be NULL for cluster scope"
        )
        self.assertIsNotNone(
            pool_row[1],
            "Pod id should not be NULL for cluster scope"
        )
        self.assertEqual(
            pool_row[2],
            "CLUSTER",
            "Storage pool scope not changed to Cluster"
        )
        self.assertIsNotNone(
            capacity_row[0],
            "Cluster id should not be NULL in the op_host_capacity table"
        )
        self.assertIsNotNone(
            capacity_row[1],
            "Pod id set should not be NULL in the op_host_capacity table"
        )
        self.assertEqual(
            len(pool_host_rows),
            0,
            "Storage pool not removed from the storage_pool_host_ref table for host on another cluster"
        )

        # Change storage pool scope to Zone
        cmd = changeStoragePoolScope.changeStoragePoolScopeCmd()
        cmd.id = self.storage.id
        cmd.scope = "ZONE"
        self.apiclient.changeStoragePoolScope(cmd)

        self.debug("Changed scope of storage pool %s to zone" % self.storage.id)

        pool_row = self.dbclient.execute("select cluster_id, pod_id, scope from storage_pool where id=" + str(pool_id))[0]
        capacity_row = self.dbclient.execute("select cluster_id, pod_id from op_host_capacity where capacity_type=3 and host_id=" + str(pool_id))[0]
        pool_host_rows = self.dbclient.execute("select id from storage_pool_host_ref where host_id=" + str(host1_id) + " and pool_id=" + str(pool_id))

        self.assertIsNone(
            pool_row[0],
            "Cluster id not set to NULL for zone scope"
        )
        self.assertIsNone(
            pool_row[1],
            "Pod id not set to NULL for zone scope"
        )
        self.assertEqual(
            pool_row[2],
            "ZONE",
            "Storage pool scope not changed to ZONE"
        )
        self.assertIsNone(
            capacity_row[0],
            "Cluster id not set to NULL in the op_host_capacity table"
        )
        self.assertIsNone(
            capacity_row[1],
            "Pod id not set to NULL in the op_host_capacity table"
        )
        self.assertEqual(
            len(pool_host_rows),
            1,
            "Storage pool not added to the storage_pool_host_ref table for host on another cluster"
        )

        # Enable storage pool
        cmd = updateStoragePool.updateStoragePoolCmd()
        cmd.id = self.storage.id
        cmd.enabled = True
        response = self.apiclient.updateStoragePool(cmd)
        self.assertEqual(
            response.state,
            "Up",
            "Storage pool couldn't be enabled"
        )
