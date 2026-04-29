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

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Host,
                             Pod,
                             Zone,
                             Cluster,
                             StoragePool)
from marvin.lib.common import get_zone, get_template
from marvin.codes import FAILED
class Services:
    """Test Resource Limits Services
    """

    def __init__(self):
        self.services = {
                        "domain": {
                                   "name": "Domain",
                                   },
                        "account": {
                                    "email": "test@test.com",
                                    "firstname": "Test",
                                    "lastname": "User",
                                    "username": "test",
                                    # Random characters are appended for unique
                                    # username
                                    "password": "password",
                         },
                         "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 100,    # in MHz
                                    "memory": 128,       # In MBs
                        },
                        "disk_offering": {
                                    "displaytext": "Small",
                                    "name": "Small",
                                    "disksize": 1
                        },
                        "volume": {
                                   "diskname": "TestDiskServ",
                        },
                        "server": {
                                    "displayname": "TestVM",
                                    "username": "root",
                                    "password": "password",
                                    "ssh_port": 22,
                                    "hypervisor": 'XenServer',
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": 'TCP',
                        },
                        "template": {
                                    "displaytext": "Cent OS Template",
                                    "name": "Cent OS Template",
                                    "ostypeid": '01853327-513e-4508-9628-f1f55db1946f',
                                    "templatefilter": 'self',
                        },
                        "ostype": 'CentOS 5.3 (64-bit)',
                        # Cent OS 5.3 (64 bit)
                        "sleep": 60,
                        "timeout": 10,
                    }


class TestAllocationState(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestAllocationState, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services

        # Get Zone, Domain and templates

        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        if cls.template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]


        cls.services['mode'] = cls.zone.networktype
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "simulator"], required_hardware="false")
    def test_01_zones(self):
        """Check the status of zones"""

        # Validate the following
        # 1. List zones
        # 2. Check allocation state is "enabled" or not

        zones = Zone.list(
                          self.apiclient,
                          id=self.zone.id,
              listall=True
                          )
        self.assertEqual(
                         isinstance(zones, list),
                         True,
                         "Check if listZones returns a valid response"
                         )
        for zone in zones:
            self.assertEqual(
                             zone.allocationstate,
                             'Enabled',
                             "Zone allocation state should be enabled"
                             )
        return

    @attr(tags=["advanced", "advancedns", "simulator"], required_hardware="false")
    def test_02_pods(self):
        """Check the status of pods"""

        # Validate the following
        # 1. List pods
        # 2. Check allocation state is "enabled" or not

        pods = Pod.list(
                          self.apiclient,
                          zoneid=self.zone.id,
              listall=True
                          )
        self.assertEqual(
                         isinstance(pods, list),
                         True,
                         "Check if listPods returns a valid response"
                         )
        for pod in pods:
            self.assertEqual(
                             pod.allocationstate,
                             'Enabled',
                             "Pods allocation state should be enabled"
                             )
        return

    @attr(tags=["advanced", "advancedns", "simulator"], required_hardware="false")
    def test_03_clusters(self):
        """Check the status of clusters"""

        # Validate the following
        # 1. List clusters
        # 2. Check allocation state is "enabled" or not

        clusters = Cluster.list(
                          self.apiclient,
                          zoneid=self.zone.id,
              listall=True
                          )
        self.assertEqual(
                         isinstance(clusters, list),
                         True,
                         "Check if listClusters returns a valid response"
                         )
        for cluster in clusters:
            self.assertEqual(
                             cluster.allocationstate,
                             'Enabled',
                             "Clusters allocation state should be enabled"
                             )
        return

    @attr(tags=["advanced", "advancedns", "simulator"], required_hardware="false")
    def test_04_hosts(self):
        """Check the status of hosts"""

        # Validate the following
        # 1. List hosts with type=Routing
        # 2. Check state is "Up" or not

        hosts = Host.list(
                          self.apiclient,
                          zoneid=self.zone.id,
                          type='Routing',
              listall=True
                          )
        self.assertEqual(
                         isinstance(hosts, list),
                         True,
                         "Check if listHosts returns a valid response"
                         )
        for host in hosts:
            self.assertEqual(
                             host.state,
                             'Up',
                             "Host should be in Up state and running"
                             )
        return

    @attr(tags=["advanced", "advancedns", "simulator"], required_hardware="false")
    def test_05_storage_pools(self):
        """Check the status of Storage pools"""

        # Validate the following
        # 1. List storage pools for the zone
        # 2. Check state is "enabled" or not

        storage_pools = StoragePool.list(
                          self.apiclient,
                          zoneid=self.zone.id,
              listall=True
                          )
        self.assertEqual(
                         isinstance(storage_pools, list),
                         True,
                         "Check if listStoragePools returns a valid response"
                         )
        for storage_pool in storage_pools:
            self.assertEqual(
                             storage_pool.state,
                             'Up',
                             "storage pool should be in Up state and running"
                             )
        return

    @attr(tags=["advanced", "advancedns", "simulator"], required_hardware="false")
    def test_06_secondary_storage(self):
        """Check the status of secondary storage"""

        # Validate the following
        # 1. List secondary storage
        # 2. Check state is "Up" or not

        if self.hypervisor.lower() == 'simulator':
            self.skipTest("Hypervisor is simulator skipping")

        sec_storages = Host.list(
                          self.apiclient,
                          zoneid=self.zone.id,
                          type='SecondaryStorageVM',
                          listall=True
                          )

        if sec_storages is None:
            self.skipTest("SSVM is not provisioned yet, skipping")

        self.assertEqual(
                         isinstance(sec_storages, list),
                         True,
                         "Check if listHosts returns a valid response"
                         )
        for sec_storage in sec_storages:
            self.assertEqual(
                             sec_storage.state,
                             'Up',
                             "Secondary storage should be in Up state"
                             )
        return
