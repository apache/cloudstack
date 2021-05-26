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
""" BVT tests for Secondary Storage
"""
#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr
from marvin.cloudstackAPI import (listImageStores)
from marvin.cloudstackAPI import (updateImageStore)

#Import System modules
import time
_multiprocess_shared_ = True

class TestSecStorageServices(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.apiclient = super(TestSecStorageServices, cls).getClsTestClient().getApiClient()
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        # Get Zone and pod
        self.zones = []
        self.pods = []
        for zone in self.config.zones:
            cmd = listZones.listZonesCmd()
            cmd.name = zone.name
            z = self.apiclient.listZones(cmd)
            if isinstance(z, list) and len(z) > 0:
                self.zones.append(z[0].id)
            for pod in zone.pods:
                podcmd = listPods.listPodsCmd()
                podcmd.zoneid = z[0].id
                p = self.apiclient.listPods(podcmd)
                if isinstance(p, list) and len(p) >0:
                    self.pods.append(p[0].id)

        self.domains = []
        dcmd = listDomains.listDomainsCmd()
        domains = self.apiclient.listDomains(dcmd)
        assert isinstance(domains, list) and len(domains) > 0
        for domain in domains:
            self.domains.append(domain.id)
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "eip", "sg"], required_hardware="false")
    def test_01_sys_vm_start(self):
        """Test system VM start
        """

        # 1. verify listHosts has all 'routing' hosts in UP state
        # 2. verify listStoragePools shows all primary storage pools
        #    in UP state
        # 3. verify that secondary storage was added successfully

        list_hosts_response = list_hosts(
                           self.apiclient,
                           type='Routing',
                           )
        self.assertEqual(
                            isinstance(list_hosts_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        # ListHosts has all 'routing' hosts in UP state
        self.assertNotEqual(
                                len(list_hosts_response),
                                0,
                                "Check list host response"
                            )
        for host in list_hosts_response:
            self.assertEqual(
                        host.state,
                        'Up',
                        "Check state of routing hosts is Up or not"
                        )

        # ListStoragePools shows all primary storage pools in UP state
        list_storage_response = list_storage_pools(
                                                   self.apiclient,
                                                   )
        self.assertEqual(
                            isinstance(list_storage_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                                len(list_storage_response),
                                0,
                                "Check list storage pools response"
                            )

        for primary_storage in list_hosts_response:
            self.assertEqual(
                        primary_storage.state,
                        'Up',
                        "Check state of primary storage pools is Up or not"
                        )
        for _ in range(2):
            list_ssvm_response = list_ssvms(
                                    self.apiclient,
                                    systemvmtype='secondarystoragevm',
                                    )

            self.assertEqual(
                            isinstance(list_ssvm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
            #Verify SSVM response
            self.assertNotEqual(
                            len(list_ssvm_response),
                            0,
                            "Check list System VMs response"
                        )

            for ssvm in list_ssvm_response:
                if ssvm.state != 'Running':
                    time.sleep(30)
                    continue
        for ssvm in list_ssvm_response:
            self.assertEqual(
                            ssvm.state,
                            'Running',
                            "Check whether state of SSVM is running"
                        )

        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "eip", "sg"], required_hardware="false")
    def test_02_sys_template_ready(self):
        """Test system templates are ready
        """

        # Validate the following
        # If SSVM is in UP state and running
        # 1. wait for listTemplates to show all builtin templates downloaded and
        # in Ready state

        hypervisors = {}
        for zone in self.config.zones:
            for pod in zone.pods:
                for cluster in pod.clusters:
                    hypervisors[cluster.hypervisor] = "self"

        for zid in self.zones:
            for k, v in list(hypervisors.items()):
                self.debug("Checking BUILTIN templates in zone: %s" %zid)
                list_template_response = list_templates(
                                        self.apiclient,
                                        hypervisor=k,
                                        zoneid=zid,
                                        templatefilter=v,
                                        listall=True,
                                        account='system'
                                        )
                self.assertEqual(validateList(list_template_response)[0], PASS,\
                        "templates list validation failed")

                # Ensure all BUILTIN templates are downloaded
                templateid = None
                for template in list_template_response:
                    if template.templatetype == "BUILTIN":
                        templateid = template.id

                    template_response = list_templates(
                                    self.apiclient,
                                    id=templateid,
                                    zoneid=zid,
                                    templatefilter=v,
                                    listall=True,
                                    account='system'
                                    )
                    if isinstance(template_response, list):
                        template = template_response[0]
                    else:
                        raise Exception("ListTemplate API returned invalid list")

                    if template.status == 'Download Complete':
                        self.debug("Template %s is ready in zone %s"%(template.templatetype, zid))
                    elif 'Downloaded' not in template.status.split():
                        self.debug("templates status is %s"%template.status)

                    self.assertEqual(
                        template.isready,
                        True,
                        "Builtin template is not ready %s in zone %s"%(template.status, zid)
                    )

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "eip", "sg"], required_hardware="false")
    def test_03_check_read_only_flag(self):
        """Test the secondary storage read-only flag
        """

        # Validate the following
        # It is possible to enable/disable the read-only flag on a secondary storage and filter by it
        # 1. Make the first secondary storage as read-only and verify its state has been changed
        # 2. Search for the read-only storages and make sure ours is in the list
        # 3. Make it again read/write and verify it has been set properly

        first_storage = self.list_secondary_storages(self.apiclient)[0]
        first_storage_id = first_storage['id']
        # Step 1
        self.update_secondary_storage(self.apiclient, first_storage_id, True)
        updated_storage = self.list_secondary_storages(self.apiclient, first_storage_id)[0]
        self.assertEqual(
            updated_storage['readonly'],
            True,
            "Check if the secondary storage status has been set to read-only"
        )

        # Step 2
        readonly_storages = self.list_secondary_storages(self.apiclient, readonly=True)
        self.assertEqual(
            isinstance(readonly_storages, list),
            True,
            "Check list response returns a valid list"
        )
        result = any(d['id'] == first_storage_id for d in readonly_storages)
        self.assertEqual(
            result,
            True,
            "Check if we are able to list storages by their read-only status"
        )

        # Step 3
        self.update_secondary_storage(self.apiclient, first_storage_id, False)
        updated_storage = self.list_secondary_storages(self.apiclient, first_storage_id)[0]
        self.assertEqual(
            updated_storage['readonly'],
            False,
            "Check if the secondary storage status has been set back to read-write"
        )

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "eip", "sg"], required_hardware="false")
    def test_04_migrate_to_read_only_storage(self):
        """Test migrations to a read-only secondary storage
        """

        # Validate the following
        # It is not possible to migrate a storage to a read-only one
        # NOTE: This test requires more than one secondary storage in the system
        # 1. Make the first storage read-only
        # 2. Try complete migration from the second to the first storage - it should fail
        # 3. Try balanced migration from the second to the first storage - it should fail
        # 4. Make the first storage read-write again

        storages = self.list_secondary_storages(self.apiclient)
        if (len(storages)) < 2:
            self.skipTest(
                "This test requires more than one secondary storage")

        first_storage = self.list_secondary_storages(self.apiclient)[0]
        first_storage_id = first_storage['id']
        second_storage = self.list_secondary_storages(self.apiclient)[1]
        second_storage_id = second_storage['id']

        # Set the first storage to read-only
        self.update_secondary_storage(self.apiclient, first_storage_id, True)

        # Try complete migration from second to the first storage


        success = False
        try:
            self.migrate_secondary_storage(self.apiclient, second_storage_id, first_storage_id, "complete")
        except Exception as ex:
            if re.search("No destination valid store\(s\) available to migrate.", str(ex)):
                success = True
            else:
                self.debug("Secondary storage complete migration to a read-only one\
                            did not fail appropriately. Error was actually : " + str(ex));

        self.assertEqual(success, True, "Check if a complete migration to a read-only storage one fails appropriately")

        # Try balanced migration from second to the first storage
        success = False
        try:
            self.migrate_secondary_storage(self.apiclient, second_storage_id, first_storage_id, "balance")
        except Exception as ex:
            if re.search("No destination valid store\(s\) available to migrate.", str(ex)):
                success = True
            else:
                self.debug("Secondary storage balanced migration to a read-only one\
                            did not fail appropriately. Error was actually : " + str(ex))

        self.assertEqual(success, True, "Check if a balanced migration to a read-only storage one fails appropriately")

        # Set the first storage back to read-write
        self.update_secondary_storage(self.apiclient, first_storage_id, False)

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "eip", "sg"], required_hardware="false")
    def test_05_migrate_to_less_free_space(self):
        """Test migrations when the destination storage has less space
        """

        # Validate the following
        # Migration to a secondary storage with less space should be refused
        # NOTE: This test requires more than one secondary storage in the system
        # 1. Try complete migration from a storage with more (or equal) free space - migration should be refused

        storages = self.list_secondary_storages(self.apiclient)
        if (len(storages)) < 2:
            self.skipTest(
                "This test requires more than one secondary storage")

        first_storage = self.list_secondary_storages(self.apiclient)[0]
        first_storage_disksizeused = first_storage['disksizeused']
        first_storage_disksizetotal = first_storage['disksizetotal']
        second_storage = self.list_secondary_storages(self.apiclient)[1]
        second_storage_disksizeused = second_storage['disksizeused']
        second_storage_disksizetotal = second_storage['disksizetotal']

        first_storage_freespace = first_storage_disksizetotal - first_storage_disksizeused
        second_storage_freespace = second_storage_disksizetotal - second_storage_disksizeused

        if first_storage_freespace == second_storage_freespace:
            self.skipTest(
                "This test requires two secondary storages with different free space")

        # Setting the storage with more free space as source storage
        if first_storage_freespace > second_storage_freespace:
            src_storage = first_storage['id']
            dst_storage = second_storage['id']
        else:
            src_storage = second_storage['id']
            dst_storage = first_storage['id']

        response = self.migrate_secondary_storage(self.apiclient, src_storage, dst_storage, "complete")

        success = False
        if re.search("has equal or more free space than destination", str(response)):
            success = True
        else:
            self.debug("Secondary storage complete migration to a storage \
            with less space was not refused. Here is the command output : " + str(response))

        self.assertEqual(success, True, "Secondary storage complete migration to a storage\
                        with less space was properly refused.")

    def list_secondary_storages(self, apiclient, id=None, readonly=None):
        cmd = listImageStores.listImageStoresCmd()
        cmd.id = id
        cmd.readonly = readonly
        return apiclient.listImageStores(cmd)

    def update_secondary_storage(self, apiclient, id, readonly):
        cmd = updateImageStore.updateImageStoreCmd()
        cmd.id = id
        cmd.readonly = readonly
        apiclient.updateImageStore(cmd)

    def migrate_secondary_storage(self, apiclient, first_id, second_id, type):
        cmd = migrateSecondaryStorageData.migrateSecondaryStorageDataCmd()
        cmd.srcpool = first_id
        cmd.destpools = second_id
        cmd.migrationtype = type
        response = apiclient.migrateSecondaryStorageData(cmd)
        return response
