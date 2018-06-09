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
from marvin.lib.base import (Account, ServiceOffering, VirtualMachine, BackupPolicy, Configurations, VMBackup)
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
        cls.account = Account.create(cls.api_client, cls.services["account"], domainid=cls.domain.id)
        cls.offering = ServiceOffering.create(cls.api_client,cls.services["service_offerings"]["small"])
        cls.vm = VirtualMachine.create(cls.api_client, cls.services["small"], accountid=cls.account.name,
                                       domainid=cls.account.domainid, serviceofferingid=cls.offering.id,
                                       mode=cls.services["mode"])
        cls._cleanup = [cls.offering, cls.account]

        # Check backup configuration values, set them to enable the dummy provider

        backup_enabled_cfg = Configurations.list(cls.api_client, name='backup.framework.enabled', zoneid=cls.zone.id)
        backup_provider_cfg = Configurations.list(cls.api_client, name='backup.framework.provider.plugin', zoneid=cls.zone.id)
        cls.backup_enabled = backup_enabled_cfg[0].value
        cls.backup_provider = backup_provider_cfg[0].value

        if not cls.backup_enabled:
            Configurations.update(cls.api_client, 'backup.framework.enabled', 'true', zoneid=cls.zone.id)
        if not cls.backup_provider == "dummy":
            Configurations.update(cls.api_client, 'backup.framework.provider.plugin', 'dummy', zoneid=cls.zone.id)

        # Import a dummy backup policy to use on tests

        cls.external_policies = BackupPolicy.listExternal(cls.api_client, cls.zone.id)
        cls.debug("Importing backup policy %s - %s" % (cls.external_policies[0].externalid, cls.external_policies[0].name))
        cls.policy = BackupPolicy.importExisting(cls.api_client, cls.zone.id, cls.external_policies[0].externalid,
                                                 cls.external_policies[0].name, cls.external_policies[0].description)
        cls._cleanup.append(cls.policy)

        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Restore original backup framework values values
            if not cls.backup_enabled:
                Configurations.update(cls.api_client, 'backup.framework.enabled', cls.backup_enabled, zoneid=cls.zone.id)
            if not cls.backup_provider == "dummy":
                Configurations.update(cls.api_client, 'backup.framework.provider.plugin', cls.backup_provider, zoneid=cls.zone.id)

            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
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
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "backup"], required_hardware="false")
    def test_import_backup_policies(self):
        """
        Import existing backup policies from Dummy Backup and Recovery Provider
        """

        # Validate the following:
        # 1. Import a backup policy from the dummy provider
        # 2. List internal backup policies, policy id should be listed
        # 3. Delete backup policy
        # 4. List internal backup policies, policy id should not be listed

        # Import backup policy
        ext_policy = self.external_policies[1]
        self.debug("Importing backup policy %s - %s" % (ext_policy.externalid, ext_policy.name))
        policy = BackupPolicy.importExisting(self.apiclient, self.zone.id, ext_policy.externalid,
                                             ext_policy.name, ext_policy.description)

        # Verify policy is listed
        imported_policies = BackupPolicy.listInternal(self.apiclient, self.zone.id)
        self.assertIsInstance(imported_policies, list, "List Backup Policies should return a valid response")
        self.assertNotEqual(len(imported_policies), 0, "Check if the list API returns a non-empty response")
        matching_policies = [x for x in imported_policies if x.id == policy.id]
        self.assertNotEqual(len(matching_policies), 0, "Check if there is a matching policy")

        # Delete backup policy
        self.debug("Deleting backup policy %s" % policy.id)
        policy.delete(self.apiclient)

        #  Verify policy is not listed
        imported_policies = BackupPolicy.listInternal(self.apiclient, self.zone.id)
        self.assertIsInstance(imported_policies, list, "List Backup Policies should return a valid response")
        matching_policies = [x for x in imported_policies if x.id == policy.id]
        self.assertEqual(len(matching_policies), 0, "Check there is not a matching policy")

    @attr(tags=["advanced", "backup"], required_hardware="false")
    def test_add_vm_to_backup_Policy(self):
        """
        Assign a VM to a backup policy
        """

        # Validate the following:
        # 1. Add VM to backup policy
        # 2. Verify a mapping between the VM and the backup policy exists
        # 3. Remove VM from backup policy
        # 4. Verify there is no mapping between the VM and the backup policy

        # Add VM to backup policy
        self.debug("Adding VM %s to backup policy %s" % (self.vm.id, self.policy.id))
        self.policy.addVM(self.apiclient, self.vm.id)

        # Verify a mapping between backup policy and VM is created on DB
        mappings = BackupPolicy.listVMMappings(self.apiclient, self.policy.id, self.vm.id, self.zone.id)
        self.assertNotEqual(len(mappings), 0, "A mapping between VM and backup policy should exist")
        self.assertNotEqual(mappings[0], None, "A mapping between VM and backup policy should exist")

        # Remove VM from backup policy
        self.debug("Removing VM %s from backup policy %s" % (self.vm.id, self.policy.id))
        self.policy.removeVM(self.apiclient, self.vm.id)

        # Verify mapping is removed
        zone_mappings = BackupPolicy.listVMMappings(self.apiclient, zoneid=self.zone.id)
        matching_mappings = [x for x in zone_mappings if x.policyid == self.policy.id and x.virtualmachineid == self.vm.id]
        self.assertEqual(len(matching_mappings), 0, "The mapping between VM and backup policy should be removed")

    @attr(tags=["advanced", "backup"], required_hardware="false")
    def test_vm_backup_lifecycle(self):
        """
        Test VM backup lifecycle
        """

        # Validate the following:
        # 1. List VM backups, verify no backups are created
        # 2. Add VM to policy
        # 3. Create VM backup
        # 4. List VM backups, verify backup is created
        # 5. Delete VM backup
        # 6. List VM backups, verify backup is deleted
        # 7. Remove VM from policy

        # Verify there are no backups for the VM
        backups = VMBackup.list(self.apiclient, self.vm.id)
        self.assertEqual(backups, None, "There should not exist any backup for the VM")

        # Create a VM backup
        self.policy.addVM(self.apiclient, self.vm.id)
        VMBackup.create(self.apiclient, self.vm.id)

        # Verify backup is created for the VM
        backups = VMBackup.list(self.apiclient, self.vm.id)
        self.assertEqual(len(backups), 1, "There should exist only one backup for the VM")
        backup = backups[0]

        # Delete backup
        VMBackup.delete(self.apiclient, backup.id)

        # Verify backup is deleted
        backups = VMBackup.list(self.apiclient, self.vm.id)
        self.assertEqual(backups, None, "There should not exist any backup for the VM")

        # Remove VM from policy
        self.policy.removeVM(self.apiclient, self.vm.id)