# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#
""" BVT tests for Primary Storage
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from settings import *
import remoteSSHClient
from utils import *
from base import *

#Import System modules
import time

services = TEST_PRIMARY_STORAGE_SERVICES

class TestPrimaryStorageServices(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" %e)
        return

    def test_01_primary_storage(self):
        """Test primary storage pools - XEN, KVM, VMWare
        """

        # Validate the following:
        # 1. verify hypervisortype returned by api is Xen/KVM/VMWare
        # 2. verify that the cluster is in 'Enabled' allocation state
        # 3. verify that the host is added successfully and in Up state with listHosts api response

        #Create NFS storage pools with on XEN/KVM/VMWare clusters
        for k,v in services["nfs"].items():

            #Host should be present before adding primary storage
            cmd = listHosts.listHostsCmd()
            cmd.clusterid = v["clusterid"]
            list_hosts_response = self.apiclient.listHosts(cmd)

            self.assertNotEqual(
                            len(list_hosts_response),
                            0,
                            "Check list Hosts response for hypervisor type : " + v["hypervisor"]
                        )

            storage = StoragePool.create(self.apiclient, v)
            self.cleanup.append(storage)

            self.assertEqual(
                            storage.state,
                            'Up',
                            "Check state of primary storage is Up or not for hypervisor type : " + v["hypervisor"]
                        )

            self.assertEqual(
                            storage.type,
                            'NetworkFilesystem',
                            "Check type of the storage pool created for hypervisor type : " + v["hypervisor"]
                        )

           #Verify List Storage pool Response has newly added storage pool
            cmd = listStoragePools.listStoragePoolsCmd()
            cmd.id = storage.id
            storage_pools_response = self.apiclient.listStoragePools(cmd)

            self.assertNotEqual(
                            len(storage_pools_response),
                            0,
                            "Check list Hosts response"
                        )

            storage_response = storage_pools_response[0]
            self.assertEqual(
                            storage_response.id,
                            storage.id,
                            "Check storage pool ID with list storage pools response for hypervisor type : " + v["hypervisor"]
                        )
            self.assertEqual(
                            storage.type,
                            storage_response.type,
                            "Check type of the storage pool for hypervisor type : " + v["hypervisor"]
                        )
            # Call cleanup for reusing primary storage
            cleanup_resources(self.apiclient, self.cleanup)
            self.cleanup = []

        # Create iSCSI storage pools with on XEN/KVM clusters
        for k,v in services["iscsi"].items():
            storage = StoragePool.create(self.apiclient, v)
            self.cleanup.append(storage)

            self.assertEqual(
                            storage.state,
                            'Up',
                            "Check state of primary storage is Up or not for hypervisor type : " + v["hypervisor"]
                        )

            #Verify List Storage pool Response has newly added storage pool
            cmd = listStoragePools.listStoragePoolsCmd()
            cmd.id = storage.id
            storage_pools_response = self.apiclient.listStoragePools(cmd)

            self.assertNotEqual(
                            len(storage_pools_response),
                            0,
                            "Check list Hosts response for hypervisor type : " + v["hypervisor"]
                        )

            storage_response = storage_pools_response[0]
            self.assertEqual(
                            storage_response.id,
                            storage.id,
                            "Check storage pool ID with list storage pools response for hypervisor type : " + v["hypervisor"]
                        )
            self.assertEqual(
                            storage.type,
                            storage_response.type,
                            "Check type of the storage pool for hypervisor type : " + v["hypervisor"]
                        )

            # Call cleanup for reusing primary storage
            cleanup_resources(self.apiclient, self.cleanup)
            self.cleanup = []
        return