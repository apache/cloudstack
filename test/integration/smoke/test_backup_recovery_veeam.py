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
from marvin.lib.utils import (cleanup_resources, wait_until)
from marvin.lib.base import (Account, ServiceOffering, VirtualMachine, BackupOffering, Configurations, Backup)
from marvin.lib.common import (get_domain, get_zone, get_template)
from nose.plugins.attrib import attr
from marvin.codes import FAILED

import time

class TestVeeamBackupAndRecovery(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        # Setup

        cls.testClient = super(TestVeeamBackupAndRecovery, cls).getClsTestClient()
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

        # Check backup configuration values, set them to enable the veeam provider
        backup_enabled_cfg = Configurations.list(cls.api_client, name='backup.framework.enabled', zoneid=cls.zone.id)
        backup_provider_cfg = Configurations.list(cls.api_client, name='backup.framework.provider.plugin', zoneid=cls.zone.id)
        cls.backup_enabled = backup_enabled_cfg[0].value
        cls.backup_provider = backup_provider_cfg[0].value

        if cls.backup_enabled == "false":
            Configurations.update(cls.api_client, 'backup.framework.enabled', value='true', zoneid=cls.zone.id)
        if cls.backup_provider != "veeam":
            return

        if cls.hypervisor.lower() != 'vmware':
            return

        cls.account = Account.create(cls.api_client, cls.services["account"], domainid=cls.domain.id)
        cls.user_user = cls.account.user[0]
        cls.user_apiclient = cls.testClient.getUserApiClient(
            cls.user_user.username, cls.domain.name
        )
        cls.service_offering = ServiceOffering.create(cls.api_client, cls.services["service_offerings"]["small"])
        cls._cleanup = [cls.service_offering, cls.account]

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
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

            # Restore original backup framework values values
            if cls.backup_enabled == "false":
                Configurations.update(cls.api_client, 'backup.framework.enabled', value=cls.backup_enabled, zoneid=cls.zone.id)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        if self.backup_provider != "veeam":
            raise self.skipTest("Skipping test cases which must only run for veeam")
        if self.hypervisor.lower() != 'vmware':
            raise self.skipTest("Skipping test cases which must only run for VMware")
        self.cleanup = []

    def tearDown(self):
        try:
            cleanup_resources(self.api_client, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    @attr(tags=["advanced", "backup"], required_hardware="false")
    def test_import_backup_offering(self):
        """
        Import provider backup offering from Veeam Backup and Recovery Provider
        """

        # Import backup offering
        offering = None
        existing_offerings = BackupOffering.listByZone(self.api_client, self.zone.id)
        provider_offerings = BackupOffering.listExternal(self.api_client, self.zone.id)
        if not provider_offerings:
            self.skipTest("Skipping test cases as the provider offering is None")
        for provider_offering in provider_offerings:
            if not self.isBackupOfferingUsed(existing_offerings, provider_offering):
                self.debug("Importing backup offering %s - %s" % (provider_offering.externalid, provider_offering.name))
                offering = BackupOffering.importExisting(self.api_client, self.zone.id, provider_offering.externalid,
                                                         provider_offering.name, provider_offering.description)
                if not offering:
                    self.fail("Failed to import backup offering %s" % provider_offering.name)
                break
        if not offering:
            self.skipTest("Skipping test cases as there is no available provider offerings to import")

        # Verify offering is listed by user
        imported_offering = BackupOffering.listByZone(self.user_apiclient, self.zone.id)
        self.assertIsInstance(imported_offering, list, "List Backup Offerings should return a valid response")
        self.assertNotEqual(len(imported_offering), 0, "Check if the list API returns a non-empty response")
        matching_offerings = [x for x in imported_offering if x.id == offering.id]
        self.assertNotEqual(len(matching_offerings), 0, "Check if there is a matching offering")

        # Delete backup offering
        self.debug("Deleting backup offering %s" % offering.id)
        offering.delete(self.api_client)

        #  Verify offering is not listed by user
        imported_offering = BackupOffering.listByZone(self.user_apiclient, self.zone.id)
        if imported_offering:
            self.assertIsInstance(imported_offering, list, "List Backup Offerings should return a valid response")
            matching_offerings = [x for x in imported_offering if x.id == offering.id]
            self.assertEqual(len(matching_offerings), 0, "Check there is not a matching offering")

    @attr(tags=["advanced", "backup"], required_hardware="false")
    def test_vm_backup_lifecycle(self):
        """
        Test VM backup lifecycle
        """

        # Import backup offering
        offering = None
        existing_offerings = BackupOffering.listByZone(self.api_client, self.zone.id)
        provider_offerings = BackupOffering.listExternal(self.api_client, self.zone.id)
        if not provider_offerings:
            self.skipTest("Skipping test cases as the provider offering is None")
        for provider_offering in provider_offerings:
            if not self.isBackupOfferingUsed(existing_offerings, provider_offering):
                self.debug("Importing backup offering %s - %s" % (provider_offering.externalid, provider_offering.name))
                offering = BackupOffering.importExisting(self.api_client, self.zone.id, provider_offering.externalid,
                                                         provider_offering.name, provider_offering.description)
                if not offering:
                    self.fail("Failed to import backup offering %s" % provider_offering.name)
                self.cleanup.append(offering)
                break
        if not offering:
            self.skipTest("Skipping test cases as there is no available provider offerings to import")

        self.vm = VirtualMachine.create(self.user_apiclient, self.services["small"], accountid=self.account.name,
                                       domainid=self.account.domainid, serviceofferingid=self.service_offering.id,
                                       mode=self.services["mode"])

        # Verify there are no backups for the VM
        backups = Backup.list(self.user_apiclient, self.vm.id)
        self.assertEqual(backups, None, "There should not exist any backup for the VM")

        # Assign VM to offering and create ad-hoc backup
        offering.assignOffering(self.user_apiclient, self.vm.id)
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
        self.assertEqual(offering.id, vms[0].backupofferingid, "The virtual machine should have backup offering %s" % offering.id)

        # Create backup
        Backup.create(self.user_apiclient, self.vm.id)

        # Verify backup is created for the VM
        self.waitForBackUp(self.vm)
        backups = Backup.list(self.user_apiclient, self.vm.id)
        self.assertEqual(len(backups), 1, "There should exist only one backup for the VM")
        backup = backups[0]

        # Delete backup
        Backup.delete(self.user_apiclient, backup.id, forced=True)

        # Verify backup is deleted
        backups = Backup.list(self.user_apiclient, self.vm.id)
        self.assertEqual(backups, None, "There should not exist any backup for the VM")

        # Remove VM from offering
        offering.removeOffering(self.user_apiclient, self.vm.id)