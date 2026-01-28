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

import time

# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (createSnapshot,
                                  deleteSnapshot,
                                  copySnapshot,
                                  createVolume,
                                  createTemplate,
                                  listOsTypes)
from marvin.lib.utils import (cleanup_resources,
                              random_gen)
from marvin.lib.base import (Account,
                             Zone,
                             ServiceOffering,
                             DiskOffering,
                             VirtualMachine,
                             Volume,
                             Snapshot,
                             Template,
                             StoragePool)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_disk_offering)
from marvin.lib.decoratorGenerators import skipTestIf
from marvin.codes import FAILED, PASS
from nose.plugins.attrib import attr
import logging
from sp_util import (TestData, StorPoolHelper)
import math
import uuid

class TestSnapshotCopy(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestSnapshotCopy, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        cls._cleanup = []
        cls.logger = logging.getLogger('TestSnapshotCopy')
        cls.testsNotSupported = False
        cls.zones = Zone.list(cls.apiclient)
        cls.pools = StoragePool.list(cls.apiclient, status="Up")
        enabled_core_zones = []
        if not isinstance(cls.zones, list):
            cls.testsNotSupported = True
        elif len(cls.zones) < 2:
            cls.testsNotSupported = True
        else:
            for z in cls.zones:
                if z.type == 'Core' and z.allocationstate == 'Enabled':
                    enabled_core_zones.append(z)
            if len(enabled_core_zones) < 2:
                cls.testsNotSupported = True

        if cls.testsNotSupported == True:
            cls.logger.info("Unsupported")
            return

        cls.additional_zone = None
        for z in enabled_core_zones:
            if z.id != cls.zone.id:
                cls.additional_zone = z

        cls.storpool_pool = None
        for pool in cls.pools:
            if pool.provider == "StorPool" and pool.zoneid != cls.zone.id:
                cls.storpool_pool = pool
                break

        template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"])
        if template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]

        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id
        cls.services["iso"]["zoneid"] = cls.zone.id

        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id)
        cls._cleanup.append(cls.account)

        compute_offering_service = cls.services["service_offerings"]["tiny"].copy()
        td = TestData()
        cls.testdata = td.testdata
        cls.helper = StorPoolHelper()
        cls.disk_offerings  = cls.create_do_if_not_exists(cls.testdata[TestData.diskOfferingCustomAdditionalZone])
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            compute_offering_service)
        cls._cleanup.append(cls.service_offering)
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id
        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["virtual_machine"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            mode=cls.services["mode"]
        )
        cls._cleanup.append(cls.virtual_machine)
        cls.volume = Volume.list(
            cls.apiclient,
            virtualmachineid=cls.virtual_machine.id,
            type='ROOT',
            listall=True
        )[0]

    @classmethod
    def tearDownClass(cls):
        super(TestSnapshotCopy, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.userapiclient = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain
        )
        self.dbclient = self.testClient.getDbConnection()
        self.snapshot_id = None
        self.cleanup = []

    def tearDown(self):
        super(TestSnapshotCopy, self).tearDown()


    @skipTestIf("testsNotSupported")
    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_01_take_snapshot_multi_zone(self):
        """Test to take volume snapshot in multiple StorPool primary storage pools
        """

        snapshot = Snapshot.create(self.userapiclient, volume_id=self.volume.id, zoneids=[str(self.additional_zone.id)], usestoragereplication=True)
        self._cleanup.append(snapshot)
        self.snapshot_id = snapshot.id
        self.helper.verify_snapshot_copies(self.userapiclient, self.snapshot_id, [self.zone.id, self.additional_zone.id])
        return

    @skipTestIf("testsNotSupported")
    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_02_copy_snapshot_multi_pools(self):
        """Test to take volume snapshot on StorPool primary storage and then copy on StorPool primary storage in another pool
        """

        snapshot = Snapshot.create(self.userapiclient, volume_id=self.volume.id)
        self._cleanup.append(snapshot)
        self.snapshot_id = snapshot.id
        Snapshot.copy(self.userapiclient, self.snapshot_id, zone_ids=[str(self.additional_zone.id)], source_zone_id=self.zone.id, usestoragereplication=True)
        self.helper.verify_snapshot_copies(self.userapiclient, self.snapshot_id, [self.zone.id, self.additional_zone.id])

        return

    @skipTestIf("testsNotSupported")
    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_03_take_snapshot_multi_pools_delete_single_zone(self):
        """Test to take volume snapshot in multiple StorPool storages in diff zones and delete from one zone
        """

        snapshot = Snapshot.create(self.userapiclient, volume_id=self.volume.id, zoneids=[str(self.additional_zone.id)], usestoragereplication=True)
        self.snapshot_id = snapshot.id
        self.helper.verify_snapshot_copies(self.userapiclient, self.snapshot_id, [self.zone.id, self.additional_zone.id])
        time.sleep(420)
        Snapshot.delete(snapshot, self.userapiclient, self.zone.id)
        self.helper.verify_snapshot_copies(self.userapiclient, self.snapshot_id, [self.additional_zone.id])
        self._cleanup.append(snapshot)
        return

    @skipTestIf("testsNotSupported")
    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_04_copy_snapshot_multi_zone_delete_all(self):
        """Test to take volume snapshot on StorPool, copy in another StorPool primary storage in another zone and delete for all
        """

        snapshot = Snapshot.create(self.userapiclient, volume_id=self.volume.id)
        self.snapshot_id = snapshot.id
        Snapshot.copy(self.userapiclient, self.snapshot_id, zone_ids=[str(self.additional_zone.id)], source_zone_id=self.zone.id, usestoragereplication=True)
        self.helper.verify_snapshot_copies(self.userapiclient, self.snapshot_id, [self.zone.id, self.additional_zone.id])
        time.sleep(420)
        Snapshot.delete(snapshot, self.userapiclient)
        snapshot_entries = Snapshot.list(self.userapiclient, id=snapshot.id)
        if snapshot_entries and isinstance(snapshot_entries, list) and len(snapshot_entries) > 0:
            self.fail("Snapshot delete for all zones failed")
        return

    @skipTestIf("testsNotSupported")
    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_05_take_snapshot_multi_zone_create_volume_additional_zone(self):
        """Test to take volume snapshot on StorPool in multiple zones and create a volume in one of the additional zones
        """

        snapshot = Snapshot.create(self.userapiclient,volume_id=self.volume.id, zoneids=[str(self.additional_zone.id)], usestoragereplication=True)
        self._cleanup.append(snapshot)
        self.snapshot_id = snapshot.id
        self.helper.verify_snapshot_copies(self.userapiclient, self.snapshot_id, [self.zone.id, self.additional_zone.id])
        disk_offering_id = None
        if snapshot.volumetype == 'ROOT':
            service = self.services["disk_offering"]
            service["disksize"] = math.ceil(snapshot.virtualsize/(1024*1024*1024))
            self.disk_offering = DiskOffering.create(
                self.apiclient,
                service
            )
            self._cleanup.append(self.disk_offering)
            disk_offering_id = self.disk_offering.id

        self.volume = Volume.create(self.userapiclient, {"diskname":"StorPoolDisk-1" }, snapshotid=self.snapshot_id, zoneid=self.zone.id, diskofferingid=disk_offering_id)
        self.cleanup.append(self.volume)
        if self.zone.id != self.volume.zoneid:
            self.fail("Volume from snapshot not created in the additional zone")
        return

    @skipTestIf("testsNotSupported")
    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_06_take_snapshot_multi_zone_create_template_additional_zone(self):
        """Test to take volume snapshot in multiple StorPool primary storages in diff zones and create a volume in one of the additional zones
        """
        snapshot = Snapshot.create(self.userapiclient, volume_id=self.volume.id, zoneids=[str(self.additional_zone.id)], usestoragereplication=True)
        self._cleanup.append(snapshot)
        self.snapshot_id = snapshot.id
        self.helper.verify_snapshot_copies(self.userapiclient, self.snapshot_id, [self.zone.id, self.additional_zone.id])
        self.template = self.helper.create_snapshot_template(self.userapiclient, self.services, self.snapshot_id, self.additional_zone.id)
        self.cleanup.append(self.template)
        if self.additional_zone.id != self.template.zoneid:
            self.fail("Template from snapshot not created in the additional zone")
        return

    @skipTestIf("testsNotSupported")
    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_07_take_snapshot_multi_zone_deploy_vm_additional_zone(self):
        """Test to take volume snapshot in multiple StorPool primary storages in diff zones and deploy a VM from snapshot in one of the additional zones
        """
        snapshot = Snapshot.create(self.userapiclient, volume_id=self.volume.id, zoneids=[str(self.additional_zone.id)], usestoragereplication=True)
        self._cleanup.append(snapshot)
        self.snapshot_id = snapshot.id
        self.helper.verify_snapshot_copies(self.userapiclient, self.snapshot_id, [self.zone.id, self.additional_zone.id])
        vm = self.deploy_vm_from_snapshot(snapshot, self.additional_zone.id)
        if self.additional_zone.id != vm.zoneid:
            self.fail("VM from snapshot not created in the additional zone")
        return


    @skipTestIf("testsNotSupported")
    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_08_take_snapshot_multi_zone_create_volume_additional_zone_deploy_vm(self):
        """Test to take volume snapshot in multiple StorPool primary storages in diff zones
         create a volume from the snapshot in the additional zone
         and deploy a VM from the volume in one of the additional zones
        """
        snapshot = Snapshot.create(self.userapiclient, volume_id=self.volume.id, zoneids=[str(self.additional_zone.id)], usestoragereplication=True)
        self.cleanup.append(snapshot)
        self.snapshot_id = snapshot.id
        self.helper.verify_snapshot_copies(self.userapiclient, self.snapshot_id, [self.zone.id, self.additional_zone.id])
        vm = self.create_volume_from_snapshot_deploy_vm(snapshotid=self.snapshot_id, zoneid=self.additional_zone.id)
        time.sleep(420)
        if self.additional_zone.id != vm.zoneid:
            self.fail("VM from snapshot not created in the additional zone")
        return

    def create_volume_from_snapshot_deploy_vm(self, snapshotid, zoneid=None):
        volume = Volume.create_from_snapshot(
            self.apiclient,
            snapshot_id=snapshotid,
            services=self.services,
            disk_offering=self.disk_offerings.id,
            size=8,
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=zoneid,
        )
        virtual_machine = VirtualMachine.create(self.apiclient,
                                                {"name": "Test-%s" % uuid.uuid4()},
                                                accountid=self.account.name,
                                                domainid=self.account.domainid,
                                                zoneid=zoneid,
                                                serviceofferingid=self.service_offering.id,
                                                volumeid=volume.id,
                                                mode="basic",
                                                )
        self.cleanup.append(virtual_machine)
        try:
            virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" %
                      (virtual_machine.ipaddress, e))
        return virtual_machine

    def deploy_vm_from_snapshot(self, snapshot, zoneid=None):
        virtual_machine = VirtualMachine.create(self.apiclient,
                                                {"name": "Test-%s" % uuid.uuid4()},
                                                accountid=self.account.name,
                                                domainid=self.account.domainid,
                                                zoneid=zoneid,
                                                serviceofferingid=self.service_offering.id,
                                                snapshotid=snapshot.id,
                                                mode="basic",
                                                )
        self.cleanup.append(virtual_machine)
        try:
            virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" %
                      (virtual_machine.ipaddress, e))
        return virtual_machine

    @classmethod
    def create_do_if_not_exists(cls, data):
        disk_offerings = list_disk_offering(
            cls.apiclient,
            name=data["name"]
        )
        if disk_offerings is None:
            disk_offerings = DiskOffering.create(cls.apiclient, services=data, custom=True)
        else:
            disk_offerings = disk_offerings[0]
        return disk_offerings
