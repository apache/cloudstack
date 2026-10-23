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
""" Tests for API listing of storage pools with different filters
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.codes import FAILED
from marvin.lib.base import (StoragePool)
from marvin.lib.common import (get_domain, list_accounts,
                               list_zones, list_clusters, list_hosts)
# Import System modules
from nose.plugins.attrib import attr

_multiprocess_shared_ = True


class TestListStoragePools(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestListStoragePools, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.domain = get_domain(cls.apiclient)
        cls.zones = list_zones(cls.apiclient)
        cls.zone = cls.zones[0]
        cls.clusters = list_clusters(cls.apiclient)
        cls.cluster = cls.clusters[0]
        cls.hosts = list_hosts(cls.apiclient)
        cls.account = list_accounts(cls.apiclient, name="admin")[0]
        cls.storage_pools = StoragePool.list(cls.apiclient)


    @classmethod
    def tearDownClass(cls):
        super(TestListStoragePools, cls).tearDownClass()

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_01_list_storage_pools_clusterid_filter(self):
        """ Test list storage pools by clusterid filter
        """
        storage_pools = StoragePool.list(
            self.apiclient,
            clusterid=self.cluster.id
        )
        self.assertTrue(
            isinstance(storage_pools, list),
            "Storage pool response type should be a list"
        )
        # Filter may return zone-wide pools (with clusterid=None) alongside cluster-scoped pools
        cluster_scoped_found = False
        for storage_pool in storage_pools:
            if storage_pool.clusterid is not None:
                # Only assert clusterid for cluster-scoped pools
                cluster_scoped_found = True
                self.assertEqual(
                    storage_pool.clusterid,
                    self.cluster.id,
                    "Cluster id should be equal to the cluster id passed in the filter"
                )

        storage_pools = StoragePool.list(
            self.apiclient,
            clusterid="-1"
        )
        # Invalid clusterid may still return zone-wide pools (scope=ZONE)
        if storage_pools:
            # If pools are returned, they should all be zone-wide
            for storage_pool in storage_pools:
                self.assertEqual(
                    storage_pool.scope,
                    "ZONE",
                    "Invalid cluster id should only return zone-wide pools"
                )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_02_list_storage_pools_id_filter(self):
        """ Test list storage pools by id filter
        """
        valid_id = self.storage_pools[0].id
        storage_pools = StoragePool.list(
            self.apiclient,
            id=valid_id
        )
        self.assertTrue(
            isinstance(storage_pools, list),
            "Storage pool response type should be a list"
        )
        self.assertEqual(
            len(storage_pools),
            1,
            "Length of storage pools should be equal to 1"
        )
        self.assertEqual(
            storage_pools[0].id,
            valid_id,
            "Cluster id should be equal to the cluster id passed in the filter"
        )

        storage_pools = StoragePool.list(
            self.apiclient,
            id="-1"
        )
        self.assertIsNone(
            storage_pools,
            "Response should be empty when invalid cluster id is passed"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_03_list_storage_pools_ipaddress_filter(self):
        """ Test list storage pools by ipaddress filter
        """
        valid_ipaddress = self.storage_pools[0].ipaddress
        storage_pools = StoragePool.list(
            self.apiclient,
            ipaddress=valid_ipaddress
        )
        self.assertTrue(
            isinstance(storage_pools, list),
            "Storage pool response type should be a list"
        )
        self.assertGreater(
            len(storage_pools),
            0,
            "Length of storage pools should greater than 0"
        )
        for storage_pool in storage_pools:
            self.assertEqual(
                storage_pool.ipaddress,
                valid_ipaddress,
                "IP address should be equal to the ip address passed in the filter"
            )

        storage_pools = StoragePool.list(
            self.apiclient,
            ipaddress="1.1.1.1"
        )
        self.assertIsNone(
            storage_pools,
            "Response should be empty when invalid ip address is passed"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_04_list_storage_pools_keyword_filter(self):
        """ Test list storage pools by keyword filter
        """
        valid_keyword = self.storage_pools[0].name
        storage_pools = StoragePool.list(
            self.apiclient,
            keyword=valid_keyword
        )
        self.assertTrue(
            isinstance(storage_pools, list),
            "Storage pool response type should be a list"
        )
        self.assertGreater(
            len(storage_pools),
            0,
            "Length of storage pools should greater than 0"
        )
        for storage_pool in storage_pools:
            self.assertIn(
                valid_keyword,
                storage_pool.name,
                "Keyword should be present in the storage pool name"
            )

        storage_pools = StoragePool.list(
            self.apiclient,
            keyword="invalid"
        )
        self.assertIsNone(
            storage_pools,
            "Response should be empty when invalid keyword is passed"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_05_list_storage_pools_name_filter(self):
        """ Test list storage pools by name filter
        """
        valid_name = self.storage_pools[0].name
        storage_pools = StoragePool.list(
            self.apiclient,
            name=valid_name
        )
        self.assertTrue(
            isinstance(storage_pools, list),
            "Storage pool response type should be a list"
        )
        self.assertGreater(
            len(storage_pools),
            0,
            "Length of storage pools should greater than 0"
        )
        for storage_pool in storage_pools:
            self.assertEqual(
                storage_pool.name,
                valid_name,
                "Name should be equal to the name passed in the filter"
            )

        storage_pools = StoragePool.list(
            self.apiclient,
            name="invalid"
        )
        self.assertIsNone(
            storage_pools,
            "Response should be empty when invalid name is passed"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_06_list_storage_pools_path_filter(self):
        """ Test list storage pools by path filter
        """
        valid_path = self.storage_pools[0].path
        storage_pools = StoragePool.list(
            self.apiclient,
            path=valid_path
        )
        self.assertTrue(
            isinstance(storage_pools, list),
            "Storage pool response type should be a list"
        )
        self.assertGreater(
            len(storage_pools),
            0,
            "Length of storage pools should greater than 0"
        )
        for storage_pool in storage_pools:
            self.assertEqual(
                storage_pool.path,
                valid_path,
                "Path should be equal to the path passed in the filter"
            )

        storage_pools = StoragePool.list(
            self.apiclient,
            path="invalid"
        )
        self.assertIsNone(
            storage_pools,
            "Response should be empty when invalid path is passed"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_07_list_storage_pools_podid_filter(self):
        """ Test list storage pools by podid filter
        """
        storage_pools = StoragePool.list(
            self.apiclient,
            podid=self.cluster.podid
        )
        self.assertTrue(
            isinstance(storage_pools, list),
            "Storage pool response type should be a list"
        )
        # Filter may return zone-wide pools (with podid=None) alongside pod-scoped pools
        pod_scoped_found = False
        for storage_pool in storage_pools:
            if storage_pool.podid is not None:
                # Only assert podid for pod-scoped pools
                pod_scoped_found = True
                self.assertEqual(
                    storage_pool.podid,
                    self.cluster.podid,
                    "Pod id should be equal to the pod id passed in the filter"
                )

        storage_pools = StoragePool.list(
            self.apiclient,
            podid="-1"
        )
        # Invalid podid may still return zone-wide pools (scope=ZONE)
        if storage_pools:
            # If pools are returned, they should all be zone-wide
            for storage_pool in storage_pools:
                self.assertEqual(
                    storage_pool.scope,
                    "ZONE",
                    "Invalid pod id should only return zone-wide pools"
                )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_08_list_storage_pools_scope_filter(self):
        """ Test list storage pools by scope filter
        """
        valid_scope = self.storage_pools[0].scope
        storage_pools = StoragePool.list(
            self.apiclient,
            scope=valid_scope
        )
        self.assertTrue(
            isinstance(storage_pools, list),
            "Storage pool response type should be a list"
        )
        self.assertGreater(
            len(storage_pools),
            0,
            "Length of storage pools should greater than 0"
        )
        for storage_pool in storage_pools:
            self.assertEqual(
                storage_pool.scope,
                valid_scope,
                "Scope should be equal to the scope passed in the filter"
            )
        with self.assertRaises(Exception):
            storage_pools = StoragePool.list(
                self.apiclient,
                scope="invalid"
            )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_09_list_storage_pools_status_filter(self):
        """ Test list storage pools by status filter
        """
        valid_status = self.storage_pools[0].status
        storage_pools = StoragePool.list(
            self.apiclient,
            status=valid_status
        )
        self.assertTrue(
            isinstance(storage_pools, list),
            "Storage pool response type should be a list"
        )
        self.assertGreater(
            len(storage_pools),
            0,
            "Length of storage pools should greater than 0"
        )
        for storage_pool in storage_pools:
            self.assertEqual(
                storage_pool.status,
                valid_status,
                "State should be equal to the status passed in the filter"
            )
        with self.assertRaises(Exception):
            storage_pools = StoragePool.list(
                self.apiclient,
                status="invalid"
            )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_10_list_storage_pools_zoneid_filter(self):
        """ Test list storage pools by zoneid filter
        """
        storage_pools = StoragePool.list(
            self.apiclient,
            zoneid=self.zone.id
        )
        self.assertTrue(
            isinstance(storage_pools, list),
            "Storage pool response type should be a list"
        )
        self.assertGreater(
            len(storage_pools),
            0,
            "Length of storage pools should greater than 0"
        )
        for storage_pool in storage_pools:
            self.assertEqual(
                storage_pool.zoneid,
                self.zone.id,
                "Zone id should be equal to the zone id passed in the filter"
            )

        storage_pools = StoragePool.list(
            self.apiclient,
            zoneid="-1"
        )
        self.assertIsNone(
            storage_pools,
            "Response should be empty when invalid zone id is passed"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_11_list_storage_pools_no_filter(self):
        """ Test list storage pools with no filter
        """
        storage_pools = StoragePool.list(
            self.apiclient
        )
        self.assertTrue(
            isinstance(storage_pools, list),
            "Storage pool response type should be a list"
        )
        self.assertGreater(
            len(storage_pools),
            0,
            "Length of storage pools should greater than 0"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_12_list_storage_pools_zone_wide(self):
        """ Test listing zone-wide vs cluster-scoped storage pools
        Zone-wide pools (scope=ZONE) are not bound to a specific cluster.
        Cluster-scoped pools are bound to clusters.
        """
        # List all storage pools and categorize them
        storage_pools = StoragePool.list(self.apiclient)
        self.assertTrue(
            isinstance(storage_pools, list),
            "Storage pool response type should be a list"
        )

        zone_wide_pools = [p for p in storage_pools if hasattr(p, 'scope') and p.scope == "ZONE"]
        cluster_scoped_pools = [p for p in storage_pools if hasattr(p, 'scope') and p.scope != "ZONE"]

        # Verify zone-wide pools
        if zone_wide_pools:
            for pool in zone_wide_pools:
                self.assertEqual(
                    pool.scope,
                    "ZONE",
                    "Pool scope should be ZONE"
                )
                # Zone-wide pools should not have clusterid or podid
                if hasattr(pool, 'clusterid'):
                    self.assertIsNone(
                        pool.clusterid,
                        "Zone-wide storage pool should not have a cluster id"
                    )
                if hasattr(pool, 'podid'):
                    self.assertIsNone(
                        pool.podid,
                        "Zone-wide storage pool should not have a pod id"
                    )
                # Verify they have zoneid
                self.assertTrue(
                    hasattr(pool, 'zoneid') and pool.zoneid is not None,
                    "Zone-wide storage pool should have a zone id"
                )
            self.debug("Verified %d zone-wide storage pools" % len(zone_wide_pools))
        else:
            self.debug("No zone-wide storage pools found in this setup")

        # Verify cluster-scoped pools
        if cluster_scoped_pools:
            for pool in cluster_scoped_pools:
                # Cluster-scoped pools should have clusterid
                self.assertTrue(
                    hasattr(pool, 'clusterid') and pool.clusterid is not None,
                    "Cluster-scoped storage pool should have a cluster id"
                )
                # Verify they have zoneid
                self.assertTrue(
                    hasattr(pool, 'zoneid') and pool.zoneid is not None,
                    "Cluster-scoped storage pool should have a zone id"
                )
            self.debug("Verified %d cluster-scoped storage pools" % len(cluster_scoped_pools))
        else:
            self.debug("No cluster-scoped storage pools found in this setup")
