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
""" BVT tests for volume snapshot copy functionality
"""
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
                             Template)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
from marvin.lib.decoratorGenerators import skipTestIf
from marvin.codes import FAILED, PASS
from nose.plugins.attrib import attr
import logging
# Import System modules
import math


_multiprocess_shared_ = True


class TestSnapshotCopy(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestSnapshotCopy, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        cls._cleanup = []
        cls.logger = logging.getLogger('TestSnapshotCopy')
        cls.testsNotSupported = False
        cls.zones = Zone.list(cls.apiclient)
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
            self.logger.info("Unsupported")
            return

        cls.additional_zone = None
        for z in enabled_core_zones:
            if z.id != cls.zone.id:
                cls.additional_zone = z

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

    def create_snapshot(self, apiclient, zoneids):
        cmd = createSnapshot.createSnapshotCmd()
        cmd.volumeid = self.volume.id
        cmd.account = self.account.name
        cmd.domainid = self.account.domainid
        if zoneids:
            cmd.zoneids = zoneids
        snapshot = Snapshot(apiclient.createSnapshot(cmd).__dict__)
        self.cleanup.append(snapshot)
        return snapshot

    def delete_snapshot(self, apiclient, snapshot_id, zone_id=None):
        cmd = deleteSnapshot.deleteSnapshotCmd()
        cmd.id = snapshot_id
        if zone_id:
            cmd.zoneid = zone_id
        apiclient.deleteSnapshot(cmd)

    def copy_snapshot(self, apiclient, snapshot_id, zone_ids, source_zone_id=None):
        cmd = copySnapshot.copySnapshotCmd()
        cmd.id = snapshot_id
        cmd.destzoneids = zone_ids
        if source_zone_id:
            cmd.sourcezoneid = source_zone_id
        return apiclient.copySnapshot(cmd)

    def create_snapshot_volume(self, apiclient, snapshot_id, zone_id=None, disk_offering_id=None):
        cmd = createVolume.createVolumeCmd()
        cmd.name = "-".join(["VolumeFromSnap", random_gen()])
        cmd.snapshotid = snapshot_id
        if zone_id:
            cmd.zoneid = zone_id
        if disk_offering_id:
            cmd.diskofferingid = disk_offering_id
        volume_from_snapshot = Volume(apiclient.createVolume(cmd).__dict__)
        self.cleanup.append(volume_from_snapshot)
        return volume_from_snapshot

    def create_snapshot_template(self, apiclient, services, snapshot_id, zone_id):
        cmd = createTemplate.createTemplateCmd()
        cmd.displaytext = "TemplateFromSnap"
        name = "-".join([cmd.displaytext, random_gen()])
        cmd.name = name
        if "ostypeid" in services:
            cmd.ostypeid = services["ostypeid"]
        elif "ostype" in services:
            # Find OSTypeId from Os type
            sub_cmd = listOsTypes.listOsTypesCmd()
            sub_cmd.description = services["ostype"]
            ostypes = apiclient.listOsTypes(sub_cmd)

            if not isinstance(ostypes, list):
                self.fail("Unable to find Ostype id with desc: %s" %
                    services["ostype"])
            cmd.ostypeid = ostypes[0].id
        else:
            self.fail("Unable to find Ostype is required for creating template")

        cmd.isfeatured = True
        cmd.ispublic = True
        cmd.isextractable =  False

        cmd.snapshotid = snapshot_id
        cmd.zoneid = zone_id
        apiclient.createTemplate(cmd)
        templates = Template.list(apiclient, name=name, templatefilter="self")
        if not isinstance(templates, list) and len(templates) < 0:
            self.fail("Unable to find created template with name %s" % name)
        template = Template(templates[0].__dict__)
        self.cleanup.append(template)
        return template

    def verify_snapshot_copies(self, snapshot_id, zone_ids):
        snapshot_entries = Snapshot.list(self.userapiclient, id=snapshot_id, showunique=False, locationtype="Secondary")
        if not isinstance(snapshot_entries, list):
            self.fail("Unable to list snapshot for multiple zones")
        elif len(snapshot_entries) != len(zone_ids):
            self.fail("Undesired list snapshot size for multiple zones")
        for zone_id in zone_ids:
            zone_found = False
            for entry in snapshot_entries:
                if entry.zoneid == zone_id:
                    zone_found = True
                    break
            if zone_found == False:
                self.fail("Unable to find snapshot entry for the zone ID: %s" % zone_id)

    @skipTestIf("testsNotSupported")
    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_01_take_snapshot_multi_zone(self):
        """Test to take volume snapshot in multiple zones
        """
        # Validate the following:
        # 1. Take snapshot in multiple zone
        # 2. Verify

        snapshot = self.create_snapshot(self.userapiclient, [str(self.additional_zone.id)])
        self.snapshot_id = snapshot.id
        self.verify_snapshot_copies(self.snapshot_id, [self.zone.id, self.additional_zone.id])
        return

    @skipTestIf("testsNotSupported")
    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_02_copy_snapshot_multi_zone(self):
        """Test to take volume snapshot in a zone and then copy
        """
        # Validate the following:
        # 1. Take snapshot in the native zone
        # 2. Copy snapshot in the additional zone
        # 3. Verify

        snapshot = self.create_snapshot(self.userapiclient, None)
        self.snapshot_id = snapshot.id
        self.copy_snapshot(self.userapiclient, self.snapshot_id, [str(self.additional_zone.id)], self.zone.id)
        self.verify_snapshot_copies(self.snapshot_id, [self.zone.id, self.additional_zone.id])
        return

    @skipTestIf("testsNotSupported")
    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_03_take_snapshot_multi_zone_delete_single_zone(self):
        """Test to take volume snapshot in multiple zones and delete from one zone
        """
        # Validate the following:
        # 1. Take snapshot in multiple zone
        # 2. Verify
        # 3. Delete from one zone
        # 4. Verify

        snapshot = self.create_snapshot(self.userapiclient, [str(self.additional_zone.id)])
        self.snapshot_id = snapshot.id
        self.verify_snapshot_copies(self.snapshot_id, [self.zone.id, self.additional_zone.id])
        self.delete_snapshot(self.userapiclient, self.snapshot_id, self.zone.id)
        self.verify_snapshot_copies(self.snapshot_id, [self.additional_zone.id])
        return

    @skipTestIf("testsNotSupported")
    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_04_copy_snapshot_multi_zone_delete_all(self):
        """Test to take volume snapshot in a zone, copy in another zone and delete for all
        """
        # Validate the following:
        # 1. Take snapshot in the native zone
        # 2. Copy snapshot in the additional zone
        # 3. Verify
        # 4. Delete for all zones
        # 5. Verify

        snapshot = self.create_snapshot(self.userapiclient, None)
        self.snapshot_id = snapshot.id
        self.copy_snapshot(self.userapiclient, self.snapshot_id, [str(self.additional_zone.id)], self.zone.id)
        self.verify_snapshot_copies(self.snapshot_id, [self.zone.id, self.additional_zone.id])
        self.delete_snapshot(self.userapiclient, self.snapshot_id)
        snapshot_entries = Snapshot.list(self.userapiclient, id=snapshot.id)
        if snapshot_entries and isinstance(snapshot_entries, list) and len(snapshot_entries) > 0:
            self.fail("Snapshot delete for all zones failed")
        self.cleanup.remove(snapshot)
        return

    @skipTestIf("testsNotSupported")
    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_05_take_snapshot_multi_zone_create_volume_additional_zone(self):
        """Test to take volume snapshot in multiple zones and create a volume in one of the additional zones
        """
        # Validate the following:
        # 1. Take snapshot in multiple zone
        # 2. Verify
        # 3. Create volume in the additional zone
        # 4. Verify volume zone

        snapshot = self.create_snapshot(self.userapiclient, [str(self.additional_zone.id)])
        self.snapshot_id = snapshot.id
        self.verify_snapshot_copies(self.snapshot_id, [self.zone.id, self.additional_zone.id])
        disk_offering_id = None
        if snapshot.volumetype == 'ROOT':
            service = self.services["disk_offering"]
            service["disksize"] = math.ceil(snapshot.virtualsize/(1024*1024*1024))
            self.disk_offering = DiskOffering.create(
                self.apiclient,
                service
            )
            self.cleanup.append(self.disk_offering)
            disk_offering_id = self.disk_offering.id
        self.volume = self.create_snapshot_volume(self.userapiclient, self.snapshot_id, self.additional_zone.id, disk_offering_id)
        if self.additional_zone.id != self.volume.zoneid:
            self.fail("Volume from snapshot not created in the additional zone")
        return

    @skipTestIf("testsNotSupported")
    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_06_take_snapshot_multi_zone_create_template_additional_zone(self):
        """Test to take volume snapshot in multiple zones and create a volume in one of the additional zones
        """
        # Validate the following:
        # 1. Take snapshot in multiple zone
        # 2. Verify
        # 3. Create template in the additional zone
        # 4. Verify template zone

        snapshot = self.create_snapshot(self.userapiclient, [str(self.additional_zone.id)])
        self.snapshot_id = snapshot.id
        self.verify_snapshot_copies(self.snapshot_id, [self.zone.id, self.additional_zone.id])
        self.template = self.create_snapshot_template(self.userapiclient, self.services, self.snapshot_id, self.additional_zone.id)
        if self.additional_zone.id != self.template.zoneid:
            self.fail("Template from snapshot not created in the additional zone")
        return
