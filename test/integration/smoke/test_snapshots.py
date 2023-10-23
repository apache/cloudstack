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

from marvin.codes import FAILED
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              is_snapshot_on_nfs,
                              validateList)
from marvin.lib.base import (VirtualMachine,
                             Account,
                             Template,
                             ServiceOffering,
                             Snapshot,
                             StoragePool,
                             Volume,
                             DiskOffering)
from marvin.lib.common import (get_domain,
                               get_test_template,
                               get_zone,
                               get_pod,
                               list_volumes,
                               list_snapshots,
                               list_storage_pools,
                               list_clusters)
from marvin.lib.decoratorGenerators import skipTestIf
from marvin.codes import PASS


class TestSnapshotRootDisk(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestSnapshotRootDisk, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.pod = get_pod(cls.apiclient, cls.zone.id)
        cls.services['mode'] = cls.zone.networktype

        cls.hypervisorNotSupported = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['hyperv', 'lxc'] or 'kvm-centos6' in cls.testClient.getZoneForTests():
            cls.hypervisorNotSupported = True

        cls._cleanup = []
        if not cls.hypervisorNotSupported:
            cls.template = get_test_template(cls.apiclient, cls.zone.id, cls.hypervisor)
            if cls.template == FAILED:
                assert False, "get_test_template() failed to return template"

            cls.services["domainid"] = cls.domain.id
            cls.services["small"]["zoneid"] = cls.zone.id
            cls.services["templates"]["ostypeid"] = cls.template.ostypeid
            cls.services["zoneid"] = cls.zone.id

            # Create VMs, NAT Rules etc
            cls.account = Account.create(
                cls.apiclient,
                cls.services["account"],
                domainid=cls.domain.id
            )
            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.services["service_offerings"]["tiny"]
            )
            cls.disk_offering = DiskOffering.create(
                cls.apiclient,
                cls.services["disk_offering"]
            )
            cls.virtual_machine = cls.virtual_machine_with_disk = \
                VirtualMachine.create(
                    cls.apiclient,
                    cls.services["small"],
                    templateid=cls.template.id,
                    accountid=cls.account.name,
                    domainid=cls.account.domainid,
                    zoneid=cls.zone.id,
                    serviceofferingid=cls.service_offering.id,
                    mode=cls.services["mode"]
                )

            cls._cleanup.append(cls.virtual_machine_with_disk)
            cls._cleanup.append(cls.service_offering)
            cls._cleanup.append(cls.account)
            cls._cleanup.append(cls.disk_offering)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)
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
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @skipTestIf("hypervisorNotSupported")
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_01_snapshot_root_disk(self):
        """Test Snapshot Root Disk
        """

        # Validate the following
        # 1. listSnapshots should list the snapshot that was created.
        # 2. verify that secondary storage NFS share contains
        #    the reqd volume under
        #    /secondary/snapshots//$account_id/$volumeid/$snapshot_uuid
        # 3. verify backup_snap_id was non null in the `snapshots` table
        # 4. Verify that zoneid is returned in listSnapshots API response

        volumes = list_volumes(
            self.apiclient,
            virtualmachineid=self.virtual_machine_with_disk.id,
            type='ROOT',
            listall=True
        )

        snapshot = Snapshot.create(
            self.apiclient,
            volumes[0].id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(snapshot)
        self.debug("Snapshot created: ID - %s" % snapshot.id)

        snapshots = list_snapshots(
            self.apiclient,
            id=snapshot.id
        )
        self.assertEqual(
            isinstance(snapshots, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            snapshots,
            None,
            "Check if result exists in list item call"
        )
        self.assertEqual(
            snapshots[0].id,
            snapshot.id,
            "Check resource id in list resources call"
        )

        self.assertIsNotNone(snapshots[0].zoneid,
                             "Zone id is not none in listSnapshots")
        self.assertEqual(
            snapshots[0].zoneid,
            self.zone.id,
            "Check zone id in the list snapshots"
        )

        self.debug(
            "select backup_snap_id, account_id, volume_id from snapshots where uuid = '%s';"
            % str(snapshot.id)
        )
        qresultset = self.dbclient.execute(
            "select backup_snap_id, account_id, volume_id from snapshots where uuid = '%s';"
            % str(snapshot.id)
        )
        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )

        qresult = qresultset[0]

        snapshot_uuid = qresult[0]      # backup_snap_id = snapshot UUID

        self.assertNotEqual(
            str(snapshot_uuid),
            'NULL',
            "Check if backup_snap_id is not null"
        )

        self.assertTrue(is_snapshot_on_nfs(
            self.apiclient, self.dbclient, self.config, self.zone.id, snapshot.id))
        return

    @skipTestIf("hypervisorNotSupported")
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_02_list_snapshots_with_removed_data_store(self):
        """Test listing volume snapshots with removed data stores
        """

        # 1 - Create new volume -> V
        # 2 - Create new Primary Storage -> PS
        # 3 - Attach and detach volume V from vm
        # 4 - Migrate volume V to PS
        # 5 - Take volume V snapshot -> S
        # 6 - List snapshot and verify it gets properly listed although Primary Storage was removed

        # Create new volume
        vol = Volume.create(
            self.apiclient,
            self.services["volume"],
            diskofferingid=self.disk_offering.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
        )
        self.cleanup.append(vol)
        self.assertIsNotNone(vol, "Failed to create volume")
        vol_res = Volume.list(
            self.apiclient,
            id=vol.id
        )
        self.assertEqual(
            validateList(vol_res)[0],
            PASS,
            "Invalid response returned for list volumes")
        vol_uuid = vol_res[0].id
        clusters = list_clusters(
            self.apiclient,
            zoneid=self.zone.id
        )
        assert isinstance(clusters,list) and len(clusters)>0

        # Attach created volume to vm, then detach it to be able to migrate it
        self.virtual_machine_with_disk.stop(self.apiclient)
        self.virtual_machine_with_disk.attach_volume(
            self.apiclient,
            vol
        )

        # Create new Primary Storage
        storage = StoragePool.create(self.apiclient,
                                     self.services["nfs2"],
                                     clusterid=clusters[0].id,
                                     zoneid=self.zone.id,
                                     podid=self.pod.id
                                     )

        self.cleanup.append(storage)
        self.assertEqual(
            storage.state,
            'Up',
            "Check primary storage state"
        )
        self.assertEqual(
            storage.type,
            'NetworkFilesystem',
            "Check storage pool type"
        )
        storage_pools_response = list_storage_pools(self.apiclient,
                                                    id=storage.id)
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

        self.virtual_machine_with_disk.detach_volume(
            self.apiclient,
            vol
        )

        # Migrate volume to new Primary Storage
        Volume.migrate(self.apiclient,
                       storageid=storage.id,
                       volumeid=vol.id
                       )

        volume_response = list_volumes(
            self.apiclient,
            id=vol.id,
        )
        self.assertNotEqual(
            len(volume_response),
            0,
            "Check list Volumes response"
        )
        volume_migrated = volume_response[0]
        self.assertEqual(
            volume_migrated.storageid,
            storage.id,
            "Check volume storage id"
        )

        # Take snapshot of new volume
        snapshot = Snapshot.create(
            self.apiclient,
            volume_migrated.id,
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.debug("Snapshot created: ID - %s" % snapshot.id)

        # Delete volume, VM and created Primary Storage
        cleanup_resources(self.apiclient, self.cleanup)

        # List snapshot and verify it gets properly listed although Primary Storage was removed
        snapshot_response = Snapshot.list(
            self.apiclient,
            id=snapshot.id
        )
        self.assertNotEqual(
            len(snapshot_response),
            0,
            "Check list Snapshot response"
        )
        self.assertEqual(
            snapshot_response[0].id,
            snapshot.id,
            "Check snapshot id"
        )

        # Delete snapshot and verify it gets properly deleted (should not be listed)
        self.cleanup = [snapshot]
        cleanup_resources(self.apiclient, self.cleanup)

        self.cleanup = []
        snapshot_response_2 = Snapshot.list(
            self.apiclient,
            id=snapshot.id
        )
        self.assertEqual(
            snapshot_response_2,
            None,
            "Check list Snapshot response"
        )

        return


class TestSnapshotStandaloneBackup(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestSnapshotStandaloneBackup, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        cls.hypervisorNotSupported = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['hyperv', 'lxc'] or 'kvm-centos6' in cls.testClient.getZoneForTests():
            cls.hypervisorNotSupported = True

        cls._cleanup = []
        if not cls.hypervisorNotSupported:
            cls.services["domainid"] = cls.domain.id
            cls.services["small"]["zoneid"] = cls.zone.id
            cls.services["zoneid"] = cls.zone.id

            # Create VMs, NAT Rules etc
            cls.account = Account.create(
                cls.apiclient,
                cls.services["account"],
                domainid=cls.domain.id
            )
            cls._cleanup.append(cls.account)
            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.services["service_offerings"]["tiny"]
            )
            cls._cleanup.append(cls.service_offering)
            cls.userapiclient = cls.testClient.getUserApiClient(
                UserName=cls.account.name,
                DomainName=cls.account.domain
            )
            cls.template = Template.register(
                cls.userapiclient,
                cls.services["test_templates"][cls.hypervisor.lower()],
                zoneid=cls.zone.id,
                hypervisor=cls.hypervisor
            )
            cls._cleanup.append(cls.template)
            cls.template.download(cls.apiclient)
            cls.ostypeid = cls.template.ostypeid
            cls.virtual_machine = VirtualMachine.create(
                cls.userapiclient,
                cls.services["small"],
                templateid=cls.template.id,
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                zoneid=cls.zone.id,
                serviceofferingid=cls.service_offering.id,
                mode=cls.services["mode"]
            )
            cls._cleanup.append(cls.virtual_machine)

            volumes =Volume.list(
                cls.userapiclient,
                virtualmachineid=cls.virtual_machine.id,
                type='ROOT',
                listall=True
            )
            cls.snapshot = Snapshot.create(
                cls.userapiclient,
                volumes[0].id,
                account=cls.account.name,
                domainid=cls.account.domainid
            )
            cls._cleanup.append(cls.snapshot)

            cls.virtual_machine.delete(cls.apiclient, expunge=True)
            cls._cleanup.remove(cls.virtual_machine)
            cls.template.delete(cls.userapiclient)
            cls._cleanup.remove(cls.template)

        return

    @classmethod
    def tearDownClass(cls):
        super(TestSnapshotStandaloneBackup, cls).tearDownClass()

    def setUp(self):
        self.cleanup = []
        return

    def tearDown(self):
        super(TestSnapshotStandaloneBackup, self).tearDown()

    @skipTestIf("hypervisorNotSupported")
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_01_snapshot_to_volume(self):
        """Test creating volume from snapshot
        """
        self.services['volume_from_snapshot']['zoneid'] = self.zone.id
        self.volume_from_snap = Volume.create_from_snapshot(
            self.userapiclient,
            snapshot_id=self.snapshot.id,
            services=self.services["volume_from_snapshot"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(self.volume_from_snap)

        self.assertEqual(
            self.volume_from_snap.state,
            'Ready',
            "Check state of the volume created from snapshot"
        )

    @skipTestIf("hypervisorNotSupported")
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_02_snapshot_to_template(self):
        """Test creating volume from snapshot
        """


        services = {"displaytext": "Template-1", "name": "Template-1-name", "ostypeid": self.ostypeid, "ispublic": "true"}
        self.template_from_snap = Template.create_from_snapshot(
            self.userapiclient,
            self.snapshot,
            services
        )
        self.cleanup.append(self.template_from_snap)

        self.assertEqual(
            self.template_from_snap.isready,
            True,
            "Check state of the template created from snapshot"
        )

        self.virtual_machine1 = VirtualMachine.create(
            self.userapiclient,
            self.services["small"],
            templateid=self.template_from_snap.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            mode=self.services["mode"]
        )
        self.cleanup.append(self.virtual_machine1)

        self.assertEqual(
            self.virtual_machine1.state,
            'Running',
            "Check state of the VM deployed using the template created from snapshot"
        )

        return
