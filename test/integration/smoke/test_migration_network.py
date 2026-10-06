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
""" Smoke test for the management-plane surface of the dedicated live-migration feature.

    The dedicated migration network is designated once at the zone level as a Migration
    traffic type on a physical network; each KVM host then resolves its own IP on that
    network and the data stream is transparent to the existing VM life-cycle tests. This
    test covers what is testable without specific hardware: that the Migration traffic type
    can be added to a physical network, and that the cluster.cpu.baseline.model setting is
    registered.
"""

from marvin.cloudstackAPI import deleteTrafficType, listTrafficTypes
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import Cluster, Configurations, PhysicalNetwork
from nose.plugins.attrib import attr


class TestLiveMigrationClusterSettings(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.test_client = super(TestLiveMigrationClusterSettings, cls).getClsTestClient()
        cls.api_client = cls.test_client.getApiClient()
        clusters = Cluster.list(cls.api_client)
        cls.cluster_id = clusters[0].id if clusters else None
        cls.added_traffic_type_ids = []

    @classmethod
    def tearDownClass(cls):
        for traffic_type_id in cls.added_traffic_type_ids:
            try:
                cmd = deleteTrafficType.deleteTrafficTypeCmd()
                cmd.id = traffic_type_id
                cls.api_client.deleteTrafficType(cmd)
            except Exception:
                pass
        super(TestLiveMigrationClusterSettings, cls).tearDownClass()

    def _list_traffic_types(self, physical_network_id):
        cmd = listTrafficTypes.listTrafficTypesCmd()
        cmd.physicalnetworkid = physical_network_id
        return self.api_client.listTrafficTypes(cmd) or []

    def _assert_cluster_setting(self, name, value):
        Configurations.update(self.api_client, name=name, value=value, clusterid=self.cluster_id)
        read = Configurations.list(self.api_client, name=name, clusterid=self.cluster_id)
        self.assertTrue(read and len(read) >= 1, "setting %s should be listable" % name)
        self.assertEqual(str(read[0].value), str(value), "setting %s should read back as set" % name)

    @attr(tags=["advanced"], required_hardware="false")
    def test_01_cpu_baseline_setting_is_registered(self):
        """The cluster CPU baseline setting exists and clearing it is accepted."""
        if self.cluster_id is None:
            self.skipTest("no cluster available")
        listed = Configurations.list(self.api_client, name="cluster.cpu.baseline.model", clusterid=self.cluster_id)
        self.assertTrue(listed and len(listed) >= 1, "cluster.cpu.baseline.model should be registered")
        # an empty baseline is always valid (no host can fail to support "no baseline")
        self._assert_cluster_setting("cluster.cpu.baseline.model", "")

    @attr(tags=["advanced"], required_hardware="false")
    def test_02_migration_traffic_type_can_be_added(self):
        """The Migration traffic type is accepted on a physical network and listed back."""
        networks = PhysicalNetwork.list(self.api_client)
        if not networks:
            self.skipTest("no physical network available")
        physical_network = networks[0]
        zone_id = getattr(physical_network, "zoneid", None)
        # the Migration traffic type is unique per zone, so skip if any physical network in the zone has it
        zone_networks = [n for n in networks if getattr(n, "zoneid", None) == zone_id]
        for net in zone_networks:
            if any(t.traffictype == "Migration" for t in self._list_traffic_types(net.id)):
                self.skipTest("Migration traffic type already present in the zone")
        response = physical_network.addTrafficType(self.api_client, "Migration")
        self.added_traffic_type_ids.append(response.id)
        self.assertEqual(response.traffictype, "Migration",
                         "addTrafficType should report the Migration traffic type")
        self.assertTrue(any(t.traffictype == "Migration" for t in self._list_traffic_types(physical_network.id)),
                        "Migration traffic type should be listable after it is added")
