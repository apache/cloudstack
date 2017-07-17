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
""" P1 tests for Snapshots
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.config.test_data import test_data

from marvin.lib.base import (Snapshot,
                             VirtualMachine,
                             Account,
                             ServiceOffering,
                             DiskOffering)

from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_volumes,
                               list_snapshots,
                               )

from marvin.lib.utils import (cleanup_resources,
                              get_hypervisor_type)


class TestSnapshots(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestSnapshots, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = test_data
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls._cleanup = []
        cls.unsupportedHypervisor = False
        cls.hypervisor = str(get_hypervisor_type(cls.api_client)).lower()
        if cls.hypervisor.lower() in ['hyperv', 'lxc']:
            cls.unsupportedHypervisor = True
            return
        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.services["disk_offering"]
        )
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )

        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["diskoffering"] = cls.disk_offering.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )

        cls._cleanup = [
            cls.service_offering,
            cls.disk_offering
        ]
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

        if self.unsupportedHypervisor:
            self.skipTest("Skipping test because unsupported hypervisor: %s" %
                    self.hypervisor)


        # Create VMs, NAT Rules etc
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(self.account)
        self.virtual_machine_with_disk = VirtualMachine.create(
                self.api_client,
                self.services["small"],
                templateid=self.template.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                mode=self.zone.networktype
            )
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(speed="slow")
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_01_snapshot_data_disk(self):
        """Test Snapshot Data Disk
        """

        volume = list_volumes(
            self.apiclient,
            virtualmachineid=self.virtual_machine_with_disk.id,
            type='DATADISK',
            listall=True
        )
        self.assertEqual(
            isinstance(volume, list),
            True,
            "Check list response returns a valid list"
        )

        self.debug("Creating a Snapshot from data volume: %s" % volume[0].id)
        snapshot = Snapshot.create(
            self.apiclient,
            volume[0].id,
            account=self.account.name,
            domainid=self.account.domainid,
            asyncbackup=True
        )
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
        self.assertEqual(
            snapshot.state,
            "BackingUp",
            "Check resource state in list resources call"
        )
        return
