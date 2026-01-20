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
from marvin.lib.utils import (cleanup_resources)
from marvin.lib.base import (Account, ServiceOffering, DiskOffering, VirtualMachine, BackupOffering, Configurations, Backup, Volume)
from marvin.lib.common import (get_domain, get_zone, get_template)
from nose.plugins.attrib import attr
from marvin.codes import FAILED
import time

class TestDummyBackupAndRecovery(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        # Setup

        cls.testClient = super(TestDummyBackupAndRecovery, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services["mode"] = cls.zone.networktype
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.domain = get_domain(cls.api_client)
        cls.template = get_template(cls.api_client, cls.zone.id, cls.services["ostype"])
        if cls.template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = cls.template.id
        cls._cleanup = []

        # Check backup configuration values, set them to enable the dummy provider
        backup_enabled_cfg = Configurations.list(cls.api_client, name='backup.framework.enabled')
        backup_provider_cfg = Configurations.list(cls.api_client, name='backup.framework.provider.plugin')
        cls.backup_enabled = backup_enabled_cfg[0].value
        cls.backup_provider = backup_provider_cfg[0].value

        if cls.backup_enabled == "false":
            cls.skipTest(cls, reason="Test can be run only if the config backup.framework.enabled is true")
        if cls.backup_provider != "dummy":
            Configurations.update(cls.api_client, 'backup.framework.provider.plugin', value='dummy')

        if cls.hypervisor.lower() != 'simulator':
            return

        cls.account = Account.create(cls.api_client, cls.services["account"], domainid=cls.domain.id)
        cls.offering = ServiceOffering.create(cls.api_client,cls.services["service_offerings"]["small"])
        cls.diskoffering = DiskOffering.create(cls.api_client, cls.services["disk_offering"])
        cls.vm = VirtualMachine.create(cls.api_client, cls.services["small"], accountid=cls.account.name,
                                       domainid=cls.account.domainid, serviceofferingid=cls.offering.id,
                                       diskofferingid=cls.diskoffering.id, mode=cls.services["mode"])
        cls._cleanup = [cls.offering, cls.diskoffering, cls.account]

        # Import a dummy backup offering to use on tests

        cls.provider_offerings = BackupOffering.listExternal(cls.api_client, cls.zone.id)
        cls.debug("Importing backup offering %s - %s" % (cls.provider_offerings[0].externalid, cls.provider_offerings[0].name))
        cls.backup_offering = BackupOffering.importExisting(cls.api_client, cls.zone.id, cls.provider_offerings[0].externalid,
                                                   cls.provider_offerings[0].name, cls.provider_offerings[0].description)
        cls._cleanup.append(cls.backup_offering)

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

            # Restore original backup framework values values
            if cls.backup_provider != "dummy":
                Configurations.update(cls.api_client, 'backup.framework.provider.plugin', value=cls.backup_provider)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        if self.hypervisor.lower() != 'simulator':
            raise self.skipTest("Skipping test cases which must only run for Simulator")
        self.cleanup = []

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    @attr(tags=["advanced", "backup"], required_hardware="false")
    def test_import_backup_offering(self):
        """
        Import provider backup offering from Dummy Backup and Recovery Provider
        """

        # Import backup offering
        provider_offering = self.provider_offerings[1]
        self.debug("Importing backup offering %s - %s" % (provider_offering.externalid, provider_offering.name))
        offering = BackupOffering.importExisting(self.apiclient, self.zone.id, provider_offering.externalid,
                                             provider_offering.name, provider_offering.description)

        # Verify offering is listed
        imported_offering = BackupOffering.listByZone(self.apiclient, self.zone.id)
        self.assertIsInstance(imported_offering, list, "List Backup Offerings should return a valid response")
        self.assertNotEqual(len(imported_offering), 0, "Check if the list API returns a non-empty response")
        matching_offerings = [x for x in imported_offering if x.id == offering.id]
        self.assertNotEqual(len(matching_offerings), 0, "Check if there is a matching offering")

        # Delete backup offering
        self.debug("Deleting backup offering %s" % offering.id)
        offering.delete(self.apiclient)

        #  Verify offering is not listed
        imported_offering = BackupOffering.listByZone(self.apiclient, self.zone.id)
        self.assertIsInstance(imported_offering, list, "List Backup Offerings should return a valid response")
        matching_offerings = [x for x in imported_offering if x.id == offering.id]
        self.assertEqual(len(matching_offerings), 0, "Check there is not a matching offering")

    @attr(tags=["advanced", "backup"], required_hardware="false")
    def test_vm_backup_lifecycle(self):
        """
        Test VM backup lifecycle
        """

        # Verify there are no backups for the VM
        backups = Backup.list(self.apiclient, self.vm.id)
        self.assertEqual(backups, None, "There should not exist any backup for the VM")

        # Assign VM to offering and create ad-hoc backup
        self.backup_offering.assignOffering(self.apiclient, self.vm.id)
        Backup.create(self.apiclient, self.vm.id)

        # Verify backup is created for the VM
        backups = Backup.list(self.apiclient, self.vm.id)
        self.assertEqual(len(backups), 1, "There should exist only one backup for the VM")
        backup = backups[0]

        # Delete backup
        Backup.delete(self.apiclient, backup.id)

        # Verify backup is deleted
        backups = Backup.list(self.apiclient, self.vm.id)
        self.assertEqual(backups, None, "There should not exist any backup for the VM")

        # Remove VM from offering
        self.backup_offering.removeOffering(self.apiclient, self.vm.id)

    @attr(tags=["advanced", "backup"], required_hardware="false")
    def test_vm_backup_create_vm_from_backup(self):
        """
        Test creating a new VM from a backup
        """
        self.backup_offering.assignOffering(self.apiclient, self.vm.id)

        Backup.create(self.apiclient, self.vm.id, "backup1")
        Backup.create(self.apiclient, self.vm.id, "backup2")

        # Verify backup is created for the VM
        backups = Backup.list(self.apiclient, self.vm.id)
        #self.cleanup.extend(backups)
        #self.cleanup.append(backups[0])
        self.assertEqual(len(backups), 2, "There should exist two backups for the VM")

        # Remove VM from offering
        self.backup_offering.removeOffering(self.apiclient, self.vm.id)

        # Verify no. of backups after removing the backup offering
        backups = Backup.list(self.apiclient, self.vm.id)
        self.assertEqual(len(backups), 2, "There should exist two backups for the VM")

        # Create a new VM from first backup
        new_vm_name = "vm-from-backup1-" + str(int(time.time()))
        new_vm = Backup.createVMFromBackup(
            self.apiclient,
            self.services["small"],
            mode=self.services["mode"],
            backupid=backups[0].id,
            vmname=new_vm_name,
            accountname=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id
        )
        self.cleanup.append(new_vm)

        # Verify the new VM was created successfully
        self.assertIsNotNone(new_vm, "Failed to create VM from backup")
        self.assertEqual(new_vm.name, new_vm_name, "VM name does not match the requested name")

        # Verify the new VM is running
        self.assertEqual(new_vm.state, "Running", "New VM should be in Running state")

        # Verify the new VM has the correct service offering
        self.assertEqual(new_vm.serviceofferingid, self.offering.id,
                        "New VM should have the correct service offering")

        # Verify the new VM has the correct zone
        self.assertEqual(new_vm.zoneid, self.zone.id, "New VM should be in the correct zone")

        # Verify the new VM has the correct number of volumes (ROOT + DATADISK)
        volumes = Volume.list(
            self.apiclient,
            virtualmachineid=new_vm.id,
            listall=True
        )
        self.assertTrue(isinstance(volumes, list), "List volumes should return a valid list")
        self.assertEqual(2, len(volumes), "The new VM should have 2 volumes (ROOT + DATADISK)")

        # Delete backups
        Backup.delete(self.apiclient, backups[0].id)
        Backup.delete(self.apiclient, backups[1].id)
