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
from marvin.lib.base import (Account, ServiceOffering, VirtualMachine, BackupOffering, Configurations, Backup)
from marvin.lib.common import (get_domain, get_zone, get_template)
from nose.plugins.attrib import attr
from marvin.codes import FAILED

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
        backup_enabled_cfg = Configurations.list(cls.api_client, name='backup.framework.enabled', zoneid=cls.zone.id)
        backup_provider_cfg = Configurations.list(cls.api_client, name='backup.framework.provider.plugin', zoneid=cls.zone.id)
        cls.backup_enabled = backup_enabled_cfg[0].value
        cls.backup_provider = backup_provider_cfg[0].value

        if cls.backup_enabled == "false":
            Configurations.update(cls.api_client, 'backup.framework.enabled', value='true', zoneid=cls.zone.id)
        if cls.backup_provider != "dummy":
            Configurations.update(cls.api_client, 'backup.framework.provider.plugin', value='dummy', zoneid=cls.zone.id)

        if cls.hypervisor.lower() != 'simulator':
            return

        cls.account = Account.create(cls.api_client, cls.services["account"], domainid=cls.domain.id)
        cls.offering = ServiceOffering.create(cls.api_client,cls.services["service_offerings"]["small"])
        cls.vm = VirtualMachine.create(cls.api_client, cls.services["small"], accountid=cls.account.name,
                                       domainid=cls.account.domainid, serviceofferingid=cls.offering.id,
                                       mode=cls.services["mode"])
        cls._cleanup = [cls.offering, cls.account]

        # Import a dummy backup offering to use on tests

        cls.provider_offerings = BackupOffering.listExternal(cls.api_client, cls.zone.id)
        cls.debug("Importing backup offering %s - %s" % (cls.provider_offerings[0].externalid, cls.provider_offerings[0].name))
        cls.offering = BackupOffering.importExisting(cls.api_client, cls.zone.id, cls.provider_offerings[0].externalid,
                                                   cls.provider_offerings[0].name, cls.provider_offerings[0].description)
        cls._cleanup.append(cls.offering)

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

            # Restore original backup framework values values
            if cls.backup_enabled == "false":
                Configurations.update(cls.api_client, 'backup.framework.enabled', value=cls.backup_enabled, zoneid=cls.zone.id)
            if cls.backup_provider != "dummy":
                Configurations.update(cls.api_client, 'backup.framework.provider.plugin', value=cls.backup_provider, zoneid=cls.zone.id)
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
        self.offering.assignOffering(self.apiclient, self.vm.id)
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
        self.offering.removeOffering(self.apiclient, self.vm.id)
