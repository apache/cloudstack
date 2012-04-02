# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#
""" BVT tests for Primary Storage
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from testcase.libs.utils import *
from testcase.libs.base import *
from testcase.libs.common import *

#Import System modules
import time

class Services:
    """Test Primary storage Services
    """

    def __init__(self):
        self.services = {
                        "nfs": {
                            0: {
                                "url": "nfs://192.168.100.131/Primary",
                                # Format: File_System_Type/Location/Path
                                "name": "Primary XEN",
                                "hypervisor": 'XEN',
                            },
                            1: {
                                "url": "nfs://192.168.100.131/Primary",
                                "name": "Primary KVM",
                                "hypervisor": 'KVM',
                            },
                            2: {
                                "url": "nfs://192.168.100.131/Primary",
                                "name": "Primary VMWare",
                                "hypervisor": 'VMWare',
                            },
                        },
                        "iscsi": {
                            0: {
                                "url": "iscsi://192.168.100.21/iqn.2012-01.localdomain.clo-cstack-cos6:iser/1",
                                # Format : iscsi://IP Address/IQN number/LUN#
                                "name": "Primary iSCSI",
                                "hypervisor": 'XEN',
                            },
                        },
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

    def test_01_primary_storage(self):
        """Test primary storage pools - XEN, KVM, VMWare
        """

        # Validate the following:
        # 1. verify hypervisortype returned by api is Xen/KVM/VMWare
        # 2. verify that the cluster is in 'Enabled' allocation state
        # 3. verify that the host is added successfully and
        #    in Up state with listHosts api response

        #Create NFS storage pools with on XEN/KVM/VMWare clusters
        for k, v in self.services["nfs"].items():

            clusters = list_clusters(
                                     self.apiclient,
                                     zoneid=self.zone.id,
                                     hypervisortype=v["hypervisor"]
                                     )
            self.assertEqual(
                            isinstance(clusters, list),
                            True,
                            "Check list response returns a valid list"
                        )
            cluster = clusters[0]
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
                        "Check list Hosts for hypervisor: " + v["hypervisor"]
                        )

            storage = StoragePool.create(self.apiclient,
                                         v,
                                         clusterid=cluster.id,
                                         zoneid=self.zone.id,
                                         podid=self.pod.id
                                         )
            self.cleanup.append(storage)

            self.debug("Created storage pool in cluster: %s" % cluster.id)

            self.assertEqual(
                storage.state,
                'Up',
                "Check primary storage state for hypervisor: " + v["hypervisor"]
                )

            self.assertEqual(
                storage.type,
                'NetworkFilesystem',
                "Check storage pool type for hypervisor : " + v["hypervisor"]
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
                    "Check storage pool ID for hypervisor: " + v["hypervisor"]
                    )
            self.assertEqual(
                    storage.type,
                    storage_response.type,
                    "Check storage pool type for hypervisor: " + v["hypervisor"]
                )
            # Call cleanup for reusing primary storage
            cleanup_resources(self.apiclient, self.cleanup)
            self.cleanup = []

        # Create iSCSI storage pools with on XEN/KVM clusters
        for k, v in self.services["iscsi"].items():
            clusters = list_clusters(
                                     self.apiclient,
                                     zoneid=self.zone.id,
                                     hypervisortype=v["hypervisor"]
                                     )
            self.assertEqual(
                            isinstance(clusters, list),
                            True,
                            "Check list response returns a valid list"
                        )
            cluster = clusters[0]

            storage = StoragePool.create(self.apiclient,
                                         v,
                                         clusterid=cluster.id,
                                         zoneid=self.zone.id,
                                         podid=self.pod.id
                                         )
            self.cleanup.append(storage)

            self.debug("Created iSCSI storage pool in cluster: %s" % cluster.id)
            
            self.assertEqual(
                storage.state,
                'Up',
                "Check primary storage state for hypervisor: " + v["hypervisor"]
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
                "Check Hosts response for hypervisor: " + v["hypervisor"]
                        )

            storage_response = storage_pools_response[0]
            self.assertEqual(
                    storage_response.id,
                    storage.id,
                    "Check storage pool ID for hypervisor: " + v["hypervisor"]
                )
            self.assertEqual(
                    storage.type,
                    storage_response.type,
                    "Check storage pool type hypervisor: " + v["hypervisor"]
                )

            # Call cleanup for reusing primary storage
            cleanup_resources(self.apiclient, self.cleanup)
            self.cleanup = []
        return