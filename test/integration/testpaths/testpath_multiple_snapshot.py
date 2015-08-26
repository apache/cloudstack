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

""" Test case for Multiple Volume Snapshot in ZWPS
"""
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             DiskOffering,
                             Snapshot,
                             VirtualMachine,
                             StoragePool
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               list_volumes,
                               list_clusters,
                               get_template
                               )

from marvin.codes import PASS, ZONETAG1, ROOT, DATA


class TestMultipleVolumeSnapshots(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestMultipleVolumeSnapshots, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata["ostype"])

        cls._cleanup = []

        cls.skiptest = False

        clus_list = list_clusters(cls.apiclient)

        if cls.hypervisor.lower() not in ['vmware'] or len(clus_list) < 2:
            cls.skiptest = True
            return

        try:
            # Create an account
            cls.account = Account.create(
                cls.apiclient,
                cls.testdata["account"],
                domainid=cls.domain.id
            )

            # Create user api client of the account
            cls.userapiclient = testClient.getUserApiClient(
                UserName=cls.account.name,
                DomainName=cls.account.domain
            )
            # Create Service offering
            cls.service_offering_zwps = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"],
                tags=ZONETAG1
            )

            cls.disk_offering_zwps = DiskOffering.create(
                cls.apiclient,
                cls.testdata["disk_offering"],
                tags=ZONETAG1
            )

            cls._cleanup = [
                cls.account,
                cls.service_offering_zwps,
                cls.disk_offering_zwps,
            ]
        except Exception as e:
            cls.tearDownClass()
            raise e
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):

        self.cleanup = []
        if self.skiptest:
            self.skipTest("Skip test as setup is either not VMWare or  \
                    having less than 2 clusters %s" % self.hypervisor)

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.pools = []

    def tearDown(self):
        try:
            for storagePool in self.pools:
                StoragePool.update(self.apiclient, id=storagePool.id, tags="")

            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_01_multiple_snapshot_in_zwps(self):
        """ Test multiple volume snapshot in zwps

        # 1. Verify if setup has a ZWPS and 2 CWPS
        # 2. Deploy a VM with data disk in ZWPS
        # 1. Verify ROOT and DATA Disk of the VM is in ZWPS.
        # 2. Take a snapshot of VM.
        # 3. Create Multiple Snapshots till operation fails.
        """
        try:
            self.pools = StoragePool.list(self.apiclient, zoneid=self.zone.id)
            status = validateList(self.pools)

            self.assertEqual(
                status[0],
                PASS,
                "Check: Failed to list storage pools due to %s" %
                status[2])

            zonepoolList = list(storagePool for storagePool in self.pools
                                if storagePool.scope == "ZONE")

            if len(zonepoolList) < 1:
                self.skipTest("There must be at least one zone wide\
                storage pools available in the setup")
            if len(list(storagePool for storagePool in self.pools
                        if storagePool.scope == "CLUSTER")) < 2:
                self.skipTest("There must be at atleast two cluster wide\
                storage pools available in the setup")
        except Exception as e:
            self.skipTest(e)

        # Adding tags to Storage Pools
        zone_no = 1
        StoragePool.update(
            self.apiclient,
            id=zonepoolList[0].id,
            tags=[ZONETAG1[:-1] + repr(zone_no)])

        self.vm_zwps = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_zwps.id,
            diskofferingid=self.disk_offering_zwps.id,
            zoneid=self.zone.id,
        )

        self.cleanup.append(self.vm_zwps)

        # Step 1
        volumes_root_list = list_volumes(
            self.apiclient,
            virtualmachineid=self.vm_zwps.id,
            type=ROOT,
            listall=True
        )
        status = validateList(volumes_root_list)

        self.assertEqual(
            status[0],
            PASS,
            "Check: Failed to list root vloume due to %s" %
            status[2])

        root_volume = volumes_root_list[0]

        if root_volume.storage != zonepoolList[0].name:
            self.fail("Root Volume not in Zone-Wide Storage Pool !")

        volumes_data_list = list_volumes(
            self.apiclient,
            virtualmachineid=self.vm_zwps.id,
            type=DATA,
            listall=True
        )
        status = validateList(volumes_data_list)

        self.assertEqual(
            status[0],
            PASS,
            "Check: Failed to list data vloume due to %s" %
            status[2])

        data_volume = volumes_data_list[0]

        if data_volume.storage != zonepoolList[0].name:
            self.fail("Data Volume not in Zone-Wide Storage Pool !")

        # Step 2
        self.vm_zwps.stop(self.apiclient)

        self.debug(
            "Creation of Snapshot of Data Volume after VM is stopped.....")

        Snapshot.create(
            self.apiclient,
            data_volume.id)

        snapshots_list = Snapshot.list(
            self.apiclient,
            volumeid=data_volume.id,
            listall=True)

        snap_list_validation_result = validateList(snapshots_list)

        self.assertEqual(
            snap_list_validation_result[0],
            PASS,
            "snapshot list validation failed due to %s" %
            snap_list_validation_result[2])

        snap_count = len(snapshots_list)

        # Step 3
        self.debug(
            "Creating Multiple Snapshots(Should create more than 10).....")
        try:
            while snap_count <= 12:
                Snapshot.create(
                    self.apiclient,
                    data_volume.id)

                snapshots_list = Snapshot.list(
                    self.apiclient,
                    volumeid=data_volume.id,
                    listall=True)

                snap_list_validation_result = validateList(snapshots_list)

                self.assertEqual(
                    snap_list_validation_result[0],
                    PASS,
                    "snapshot list validation failed due to %s" %
                    snap_list_validation_result[2])

                snap_count = len(snapshots_list)
        except Exception as e:
            snapshots_list = Snapshot.list(
                self.apiclient,
                volumeid=data_volume.id,
                listall=True)

            snap_list_validation_result = validateList(snapshots_list)

            self.assertEqual(
                snap_list_validation_result[0],
                PASS,
                "snapshot list validation failed due to %s" %
                snap_list_validation_result[2])

            assert len(snapshots_list) >= 10,\
                "Less than 10 snapshots created...."
            raise Exception("Snapshot creation failed !: %s" % e)

        return
