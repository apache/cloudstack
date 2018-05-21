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
from marvin.lib.utils import (random_gen,
                              cleanup_resources,
                              validateList)
from marvin.cloudstackAPI import *
from marvin.lib.base import (Domain,
                             Account,
                             ServiceOffering,
                             VirtualMachine,
                             Network,
                             User,
                             NATRule,
                             Template,
                             PublicIPAddress,
                             BackupPolicy,
                             Configurations)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_accounts,
                               list_virtual_machines,
                               list_service_offering,
                               list_templates,
                               list_users,
                               get_builtin_template_info,
                               wait_for_cleanup)
from nose.plugins.attrib import attr
import logging
from marvin.cloudstackException import CloudstackAPIException
from marvin.codes import PASS

class TestBackupAndRecovery(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestBackupAndRecovery, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.logger = logging.getLogger('TestBackupAndRecovery')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        cls.services = cls.testClient.getParsedTestDataConfig()
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        backup_enabled_cfg = Configurations.list(
            cls.api_client,
            name='backup.framework.enabled'
        )
        backup_provider_cfg = Configurations.list(
            cls.api_client,
            name='backup.framework.provider.plugin'
        )
        cls.backup_enabled = backup_enabled_cfg[0].value
        cls.backup_provider = backup_provider_cfg[0].value

        cls.backup_available = cls.backup_enabled and cls.backup_provider == "dummy"

        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
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
            # Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "backup"], required_hardware="false")
    def test_ImportBackupPolicies(self):
        """
        Import existing backup policies
        """
        if not self.backup_available:
            self.skipTest("This test is only available when backup is enabled and dummy provider selected")

        external_policies = BackupPolicy.listExternal(self.apiclient, self.zone.id)
        self.logger.debug("External policies on dummy provider")
        self.logger.debug(external_policies)

        for p in external_policies:
            self.logger.debug("Importing backup policy %s - %s" % (p.policyid, p.name))
            policy = BackupPolicy.importExisting(self.apiclient, self.zone.id, p.policyid, p.name)

        imported_policies = BackupPolicy.listInternal(self.apiclient, self.zone.id)
        self.logger.debug("Listing internal backup policies")
        self.logger.debug(imported_policies)

        for p in imported_policies:
            self.logger.debug("Deleting backup policy %s" % p.id)
            BackupPolicy.delete(self.apiclient, p.id)