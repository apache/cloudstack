#!/usr/bin/env python
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

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import wait_until
from marvin.lib.base import (Account, ServiceOffering, DiskOffering, Volume, VirtualMachine,
                             BackupOffering, Configurations, Backup, BackupSchedule)
from marvin.lib.common import (get_domain, get_zone, get_template)
from nose.plugins.attrib import attr
from marvin.codes import FAILED

import time

class TestVeeamBackupAndRecovery(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        # Setup

        cls.testClient = super(TestVeeamBackupAndRecovery, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.services["mode"] = cls.zone.networktype
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.domain = get_domain(cls.apiclient)
        cls.template = get_template(cls.apiclient, cls.zone.id, cls.services["ostype"])
        if cls.template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = cls.template.id
        cls._cleanup = []

        # Check backup configuration values, set them to enable the veeam provider
        backup_enabled_cfg = Configurations.list(cls.apiclient, name='backup.framework.enabled', zoneid=cls.zone.id)
        backup_provider_cfg = Configurations.list(cls.apiclient, name='backup.framework.provider.plugin', zoneid=cls.zone.id)
        cls.backup_enabled = backup_enabled_cfg[0].value
        cls.backup_provider = backup_provider_cfg[0].value

        if cls.backup_enabled == "false":
            Configurations.update(cls.apiclient, 'backup.framework.enabled', value='true', zoneid=cls.zone.id)
        if cls.backup_provider != "veeam":
            return

        if cls.hypervisor.lower() != 'vmware':
            return

        cls.service_offering = ServiceOffering.create(cls.apiclient, cls.services["service_offerings"]["small"])
        cls._cleanup.append(cls.service_offering)
        cls.disk_offering = DiskOffering.create(cls.apiclient, cls.services["disk_offering"])
        cls._cleanup.append(cls.disk_offering)

    @classmethod
    def isBackupOfferingUsed(cls, existing_offerings, provider_offering):
        if not existing_offerings:
            return False
        for existing_offering in existing_offerings:
            if existing_offering.externalid == provider_offering.externalid:
                return True
        return False

    def waitForBackUp(self, vm):
        def checkBackUp():
            backups = Backup.list(self.user_apiclient, vm.id)
            if isinstance(backups, list) and len(backups) != 0:
                return True, None
            return False, None

        res, _ = wait_until(10, 60, checkBackUp)
        if not res:
            self.fail("Failed to wait for backup of VM %s to be Up" % vm.id)

    @classmethod
    def tearDownClass(cls):
        if cls.backup_enabled == "false":
            Configurations.update(cls.apiclient, 'backup.framework.enabled', value=cls.backup_enabled, zoneid=cls.zone.id)
        super(TestVeeamBackupAndRecovery, cls).tearDownClass()

    def setUp(self):
        if self.backup_provider != "veeam":
            raise self.skipTest("Skipping test cases which must only run for veeam")
        if self.hypervisor.lower() != 'vmware':
            raise self.skipTest("Skipping test cases which must only run for VMware")
        self.cleanup = []

        # Import backup offering
        self.offering = None
        existing_offerings = BackupOffering.listByZone(self.apiclient, self.zone.id)
        provider_offerings = BackupOffering.listExternal(self.apiclient, self.zone.id)
        if not provider_offerings:
            self.skipTest("Skipping test cases as the provider offering is None")
        for provider_offering in provider_offerings:
            if not self.isBackupOfferingUsed(existing_offerings, provider_offering):
                self.debug("Importing backup offering %s - %s" % (provider_offering.externalid, provider_offering.name))
                self.offering = BackupOffering.importExisting(self.apiclient, self.zone.id, provider_offering.externalid,
                                                         provider_offering.name, provider_offering.description)
                if not self.offering:
                    self.fail("Failed to import backup offering %s" % provider_offering.name)
                break
        if not self.offering:
            self.skipTest("Skipping test cases as there is no available provider offerings to import")

        # Create user account
        self.account = Account.create(self.apiclient, self.services["account"], domainid=self.domain.id)
        self.user_user = self.account.user[0]
        self.user_apiclient = self.testClient.getUserApiClient(
            self.user_user.username, self.domain.name
        )
        self.cleanup.append(self.account)

    def tearDown(self):
        super(TestVeeamBackupAndRecovery, self).tearDown()

    @attr(tags=["advanced", "backup"], required_hardware="false")
    def test_01_import_list_delete_backup_offering(self):
        """
        Import provider backup offering from Veeam Backup and Recovery Provider
        """

        # Verify offering is listed by user
        imported_offering = BackupOffering.listByZone(self.user_apiclient, self.zone.id)
        self.assertIsInstance(imported_offering, list, "List Backup Offerings should return a valid response")
        self.assertNotEqual(len(imported_offering), 0, "Check if the list API returns a non-empty response")
        matching_offerings = [x for x in imported_offering if x.id == self.offering.id]
        self.assertNotEqual(len(matching_offerings), 0, "Check if there is a matching offering")

        # Delete backup offering
        self.debug("Deleting backup offering %s" % self.offering.id)
        self.offering.delete(self.apiclient)

        #  Verify offering is not listed by user
        imported_offering = BackupOffering.listByZone(self.user_apiclient, self.zone.id)
        if imported_offering:
            self.assertIsInstance(imported_offering, list, "List Backup Offerings should return a valid response")
            matching_offerings = [x for x in imported_offering if x.id == self.offering.id]
            self.assertEqual(len(matching_offerings), 0, "Check there is not a matching offering")

    @attr(tags=["advanced", "backup"], required_hardware="false")
    def test_02_vm_backup_lifecycle(self):
        """
        Test VM backup lifecycle
        """

        if self.offering:
            self.cleanup.insert(0, self.offering)

        self.vm = VirtualMachine.create(self.user_apiclient, self.services["small"], accountid=self.account.name,
                                       domainid=self.account.domainid, serviceofferingid=self.service_offering.id,
                                       diskofferingid=self.disk_offering.id)

        # Verify there are no backups for the VM
        backups = Backup.list(self.user_apiclient, self.vm.id)
        self.assertEqual(backups, None, "There should not exist any backup for the VM")

        # Assign VM to offering and create ad-hoc backup
        self.offering.assignOffering(self.user_apiclient, self.vm.id)
        vms = VirtualMachine.list(
            self.user_apiclient,
            id=self.vm.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List virtual machines should return a valid list"
        )
        self.assertEqual(1, len(vms), "List of the virtual machines should have 1 vm")
        self.assertEqual(self.offering.id, vms[0].backupofferingid, "The virtual machine should have backup offering %s" % self.offering.id)

        # Create backup schedule on 01:00AM every Sunday
        BackupSchedule.create(self.user_apiclient, self.vm.id, intervaltype="WEEKLY", timezone="CET", schedule="00:01:1")
        backupSchedule = BackupSchedule.list(self.user_apiclient, self.vm.id)
        self.assertIsNotNone(backupSchedule)
        self.assertEqual("WEEKLY", backupSchedule.intervaltype)
        self.assertEqual("00:01:1", backupSchedule.schedule)
        self.assertEqual("CET", backupSchedule.timezone)
        self.assertEqual(self.vm.id, backupSchedule.virtualmachineid)
        self.assertEqual(self.vm.name, backupSchedule.virtualmachinename)

        # Update backup schedule on 02:00AM every 20th
        BackupSchedule.update(self.user_apiclient, self.vm.id, intervaltype="MONTHLY", timezone="CET", schedule="00:02:20")
        backupSchedule = BackupSchedule.list(self.user_apiclient, self.vm.id)
        self.assertIsNotNone(backupSchedule)
        self.assertEqual("MONTHLY", backupSchedule.intervaltype)
        self.assertEqual("00:02:20", backupSchedule.schedule)

        # Delete backup schedule
        BackupSchedule.delete(self.user_apiclient, self.vm.id)

        # Create backup
        Backup.create(self.user_apiclient, self.vm.id)

        # Verify backup is created for the VM
        self.waitForBackUp(self.vm)
        backups = Backup.list(self.user_apiclient, self.vm.id)
        self.assertEqual(len(backups), 1, "There should exist only one backup for the VM")
        backup = backups[0]

        # Stop VM
        self.vm.stop(self.user_apiclient, forced=True)
        # Restore backup
        Backup.restoreVM(self.user_apiclient, backup.id)

        # Delete backup
        Backup.delete(self.user_apiclient, backup.id, forced=True)

        # Verify backup is deleted
        backups = Backup.list(self.user_apiclient, self.vm.id)
        self.assertEqual(backups, None, "There should not exist any backup for the VM")

        # Remove VM from offering
        self.offering.removeOffering(self.user_apiclient, self.vm.id)

    @attr(tags=["advanced", "backup"], required_hardware="false")
    def test_03_restore_volume_attach_vm(self):
        """
        Test Volume Restore from Backup and Attach to VM
        """

        if self.offering:
            self.cleanup.insert(0, self.offering)

        self.vm_with_datadisk = VirtualMachine.create(self.user_apiclient, self.services["small"], accountid=self.account.name,
                                                      domainid=self.account.domainid, serviceofferingid=self.service_offering.id,
                                                      diskofferingid=self.disk_offering.id)

        self.vm = VirtualMachine.create(self.user_apiclient, self.services["small"], accountid=self.account.name,
                                                      domainid=self.account.domainid, serviceofferingid=self.service_offering.id)

        # Assign VM to offering and create ad-hoc backup
        self.offering.assignOffering(self.user_apiclient, self.vm_with_datadisk.id)

        # Create backup
        Backup.create(self.user_apiclient, self.vm_with_datadisk.id)

        # Verify backup is created for the VM with datadisk
        self.waitForBackUp(self.vm_with_datadisk)
        backups = Backup.list(self.user_apiclient, self.vm_with_datadisk.id)
        self.assertEqual(len(backups), 1, "There should exist only one backup for the VM with datadisk")
        backup = backups[0]

        try:
            volumes = Volume.list(
                self.user_apiclient,
                virtualmachineid=self.vm_with_datadisk.id,
                listall=True
            )
            rootDiskId = None
            dataDiskId = None
            for volume in volumes:
                if volume.type == 'ROOT':
                    rootDiskId = volume.id
                elif volume.type == 'DATADISK':
                    dataDiskId = volume.id
            if rootDiskId:
                # Restore ROOT volume of vm_with_datadisk and attach to vm
                Backup.restoreVolumeFromBackupAndAttachToVM(
                    self.user_apiclient,
                    backupid=backup.id,
                    volumeid=rootDiskId,
                    virtualmachineid=self.vm.id
                )
                vm_volumes = Volume.list(
                    self.user_apiclient,
                    virtualmachineid=self.vm.id,
                    listall=True
                )
                self.assertTrue(isinstance(vm_volumes, list), "List volumes should return a valid list")
                self.assertEqual(2, len(vm_volumes), "The number of volumes should be 2")
            if dataDiskId:
                # Restore DATADISK volume of vm_with_datadisk and attach to vm
                Backup.restoreVolumeFromBackupAndAttachToVM(
                    self.user_apiclient,
                    backupid=backup.id,
                    volumeid=dataDiskId,
                    virtualmachineid=self.vm.id
                )
                vm_volumes = Volume.list(
                    self.user_apiclient,
                    virtualmachineid=self.vm.id,
                    listall=True
                )
                self.assertTrue(isinstance(vm_volumes, list), "List volumes should return a valid list")
                self.assertEqual(3, len(vm_volumes), "The number of volumes should be 3")
        finally:
            # Delete backup
            Backup.delete(self.user_apiclient, backup.id, forced=True)
            # Remove VM from offering
            self.offering.removeOffering(self.user_apiclient, self.vm_with_datadisk.id)
            # Delete vm
            self.vm.delete(self.apiclient)
            # Delete vm with datadisk
            self.vm_with_datadisk.delete(self.apiclient)
