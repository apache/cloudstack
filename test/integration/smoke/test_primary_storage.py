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
#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from nose.plugins.attrib import attr

#Import System modules
import time
_multiprocess_shared_ = True

class Services:
    """Test Primary storage Services
    """

    def __init__(self):
        self.services = {
                        "nfs":
                             {
                                "url": "nfs://10.147.28.7/export/home/talluri/testprimary",
                                # Format: File_System_Type/Location/Path
                                "name": "Primary XEN"
                            },
                        "iscsi": {
                                "url": "iscsi://192.168.100.21/iqn.2012-01.localdomain.clo-cstack-cos6:iser/1",
                                # Format : iscsi://IP Address/IQN number/LUN#
                                "name": "Primary iSCSI"
                            }
                 }

class TestPrimaryStorageServices(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.services = Services().services
        self.cleanup = []
        # Get Zone and pod
        self.zone = get_zone(self.apiclient, self.services)
        self.pod = get_pod(self.apiclient, self.zone.id)

        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "sg"])
    def test_01_primary_storage_nfs(self):
        """Test primary storage pools - XEN, KVM, VMWare
        """

        # Validate the following:
        # 1. List Clusters
        # 2. verify that the cluster is in 'Enabled' allocation state
        # 3. verify that the host is added successfully and
        #    in Up state with listHosts api response

        #Create NFS storage pools with on XEN/KVM/VMWare clusters


        clusters = list_clusters(
            self.apiclient,
            zoneid=self.zone.id
        )
        assert isinstance(clusters,list) and len(clusters)>0
        for cluster in clusters:

            #Host should be present before adding primary storage
            list_hosts_response = list_hosts(
                                             self.apiclient,
                                             clusterid=cluster.id
                                             )
            self.assertEqual(
                            isinstance(list_hosts_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

            self.assertNotEqual(
                        len(list_hosts_response),
                        0,
                        "Check list Hosts in the cluster: " + cluster.name
                        )

            storage = StoragePool.create(self.apiclient,
                                         self.services["nfs"],
                                         clusterid=cluster.id,
                                         zoneid=self.zone.id,
                                         podid=self.pod.id
                                         )
            self.cleanup.append(storage)

            self.debug("Created storage pool in cluster: %s" % cluster.id)

            self.assertEqual(
                storage.state,
                'Up',
                "Check primary storage state "
                )

            self.assertEqual(
                storage.type,
                'NetworkFilesystem',
                "Check storage pool type "
                )

            #Verify List Storage pool Response has newly added storage pool
            storage_pools_response = list_storage_pools(
                                                        self.apiclient,
                                                        id=storage.id,
                                                        )
            self.assertEqual(
                            isinstance(storage_pools_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
            self.assertNotEqual(
                            len(storage_pools_response),
                            0,
                            "Check list Hosts response"
                        )

            storage_response = storage_pools_response[0]
            self.assertEqual(
                    storage_response.id,
                    storage.id,
                    "Check storage pool ID"
                    )
            self.assertEqual(
                    storage.type,
                    storage_response.type,
                    "Check storage pool type "
                )
            # Call cleanup for reusing primary storage
            cleanup_resources(self.apiclient, self.cleanup)
            self.cleanup = []
            return


    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "sg"])
    def test_01_primary_storage_iscsi(self):
        """Test primary storage pools - XEN, KVM, VMWare
        """

        # Validate the following:
        # 1. List Clusters
        # 2. verify that the cluster is in 'Enabled' allocation state
        # 3. verify that the host is added successfully and
        #    in Up state with listHosts api response

        # Create iSCSI storage pools with on XEN/KVM clusters
        clusters = list_clusters(
            self.apiclient,
            zoneid=self.zone.id
        )
        assert isinstance(clusters,list) and len(clusters)>0
        for cluster in clusters:

            #Host should be present before adding primary storage
            list_hosts_response = list_hosts(
                                             self.apiclient,
                                             clusterid=cluster.id
                                             )
            self.assertEqual(
                            isinstance(list_hosts_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

            self.assertNotEqual(
                        len(list_hosts_response),
                        0,
                        "Check list Hosts in the cluster: " + cluster.name
                        )


            storage = StoragePool.create(self.apiclient,
                                         self.services["iscsi"],
                                         clusterid=cluster.id,
                                         zoneid=self.zone.id,
                                         podid=self.pod.id
                                         )
            self.cleanup.append(storage)

            self.debug("Created storage pool in cluster: %s" % cluster.id)

            self.assertEqual(
                storage.state,
                'Up',
                "Check primary storage state "
                )

            self.assertEqual(
                storage.type,
                'NetworkFilesystem',
                "Check storage pool type "
                )

            #Verify List Storage pool Response has newly added storage pool
            storage_pools_response = list_storage_pools(
                                                        self.apiclient,
                                                        id=storage.id,
                                                        )
            self.assertEqual(
                            isinstance(storage_pools_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
            self.assertNotEqual(
                            len(storage_pools_response),
                            0,
                            "Check list Hosts response"
                        )

            storage_response = storage_pools_response[0]
            self.assertEqual(
                    storage_response.id,
                    storage.id,
                    "Check storage pool ID"
                    )
            self.assertEqual(
                    storage.type,
                    storage_response.type,
                    "Check storage pool type "
                )
            # Call cleanup for reusing primary storage
            cleanup_resources(self.apiclient, self.cleanup)
            self.cleanup = []

        return
