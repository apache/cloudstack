# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#
""" BVT tests for Secondary Storage
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

services = TEST_SEC_STORAGE_SERVICES

class TestSecStorageServices(cloudstackTestCase):

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

    def test_01_add_sec_storage(self):
        """Test secondary storage
        """

        # Validate the following:
        # 1. secondary storage should be added to the zone.
        # 2. Verify with listHosts and type secondarystorage

        cmd = addSecondaryStorage.addSecondaryStorageCmd()
        cmd.zoneid = services["storage"]["zoneid"]
        cmd.url = services["storage"]["url"]
        sec_storage = self.apiclient.addSecondaryStorage(cmd)

        self.assertEqual(
                            sec_storage.zoneid,
                            services["storage"]["zoneid"],
                            "Check zoneid where sec storage is added"
                        )

        cmd = listHosts.listHostsCmd()
        cmd.type = 'SecondaryStorage'
        cmd.id = sec_storage.id
        list_hosts_response = self.apiclient.listHosts(cmd)

        self.assertNotEqual(
                        len(list_hosts_response),
                        0,
                        "Check list Hosts response"
                        )

        host_response = list_hosts_response[0]
        #Check if host is Up and running
        self.assertEqual(
                        host_response.id,
                        sec_storage.id,
                        "Check ID of secondary storage"
                        )
        self.assertEqual(
                        sec_storage.type,
                        host_response.type,
                        "Check type of host from list hosts response"
                        )
        return

    def test_02_sys_vm_start(self):
        """Test system VM start
        """

        # 1. verify listHosts has all 'routing' hosts in UP state
        # 2. verify listStoragePools shows all primary storage pools in UP state
        # 3. verify that secondary storage was added successfully

        cmd = listHosts.listHostsCmd()
        cmd.type = 'Routing'
        list_hosts_response = self.apiclient.listHosts(cmd)
        # ListHosts has all 'routing' hosts in UP state
        self.assertNotEqual(
                                len(list_hosts_response),
                                0,
                                "Check list host response"
                            )
        for i in range(len(list_hosts_response)):

            host_response = list_hosts_response[i]
            self.assertEqual(
                        host_response.state,
                        'Up',
                        "Check state of routing hosts is Up or not"
                        )

        # ListStoragePools shows all primary storage pools in UP state
        cmd = listStoragePools.listStoragePoolsCmd()
        cmd.name = 'Primary'
        list_storage_response = self.apiclient.listStoragePools(cmd)

        self.assertNotEqual(
                                len(list_storage_response),
                                0,
                                "Check list storage pools response"
                            )

        for i in range(len(list_hosts_response)):
            storage_response = list_storage_response[i]
            self.assertEqual(
                        storage_response.state,
                        'Up',
                        "Check state of primary storage pools is Up or not"
                        )

        # Secondary storage is added successfully
        cmd = listHosts.listHostsCmd()
        cmd.type = 'SecondaryStorage'
        list_hosts_response = self.apiclient.listHosts(cmd)

        self.assertNotEqual(
                        len(list_hosts_response),
                        0,
                        "Check list Hosts response"
                        )

        host_response = list_hosts_response[0]
        #Check if host is Up and running
        self.assertEqual(
                        host_response.state,
                        'Up',
                        "Check state of secondary storage"
                        )
        return

    def test_03_sys_template_ready(self):
        """Test system templates are ready
        """

        # TODO
        return