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
from marvin.codes import PASS, FAILED

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
        cls.services["mode"] = cls.zone.networktype
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.domain = get_domain(cls.api_client)

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        if cls.template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]

        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = cls.template.id

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )

        cls.offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offerings"]["small"]
        )

        cls.vm = VirtualMachine.create(
            cls.api_client,
            cls.services["small"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.offering.id,
            mode=cls.services["mode"]
        )

        cls._cleanup = [
            cls.offering,
            cls.account
        ]

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

        if cls.backup_available:
            cls.external_policies = BackupPolicy.listExternal(cls.api_client, cls.zone.id)
            cls.logger.debug("Importing backup policy %s - %s" % (cls.external_policies[0].externalid, cls.external_policies[0].name))
            cls.policy = BackupPolicy.importExisting(cls.api_client, cls.zone.id, cls.external_policies[0].externalid,
                                                     cls.external_policies[0].name, cls.external_policies[0].description)
            cls._cleanup.append(cls.policy)

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

        ext_policy = self.external_policies[1]
        self.logger.debug("Importing backup policy %s - %s" % (ext_policy.externalid, ext_policy.name))
        policy = BackupPolicy.importExisting(self.apiclient, self.zone.id, ext_policy.externalid, ext_policy.name, ext_policy.description)

        imported_policies = BackupPolicy.listInternal(self.apiclient, self.zone.id)
        self.assertIsInstance(
            imported_policies,
            list,
            "List Backup Policies should return a valid response"
        )
        self.assertNotEqual(
            len(imported_policies),
            0,
            "Check if the list API returns a non-empty response"
        )

        self.logger.debug("Listing internal backup policies")
        self.logger.debug(imported_policies)

        self.logger.debug("Deleting backup policy %s" % policy.id)
        policy.delete(self.apiclient)

    @attr(tags=["advanced", "backup"], required_hardware="false")
    def test_AssignVMToBackupPolicy(self):
        """
        Assign a VM to a backup policy
        """
        if not self.backup_available:
            self.skipTest("This test is only available when backup is enabled and dummy provider selected")

        self.logger.debug("Assigning VM %s to backup policy %s" % (self.vm.id, self.policy.id))

        self.policy.assignVM(
            self.apiclient,
            self.vm.id,
            self.zone.id
        )

        qresultset = self.dbclient.execute(
            "select id from vm_instance where uuid='%s';"
            % self.vm.id
        )
        vm_id = qresultset[0][0]

        qresultset = self.dbclient.execute(
            "select id from backup_policy where uuid='%s';"
            % self.policy.id
        )
        policy_id = qresultset[0][0]

        qresultset = self.dbclient.execute(
            "select id from backup_policy_vm_map where policy_id='%d' and vm_id = '%d';"
            % (policy_id, vm_id)
        )

        map = qresultset[0]
        self.assertNotEqual(
            map[0],
            None,
            "A mapping between VM and backup policy should exist on DB"
        )
