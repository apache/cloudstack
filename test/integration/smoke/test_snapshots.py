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

from marvin.codes import FAILED
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              is_snapshot_on_nfs)
from marvin.lib.base import (VirtualMachine,
                             Account,
                             ServiceOffering,
                             Snapshot)
from marvin.lib.common import (get_domain,
                               get_template,
                               get_zone,
                               list_volumes,
                               list_snapshots)
from marvin.lib.decoratorGenerators import skipTestIf


class TestSnapshotRootDisk(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestSnapshotRootDisk, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        cls.hypervisorNotSupported = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['hyperv', 'lxc']:
            cls.hypervisorNotSupported = True

        cls._cleanup = []
        if not cls.hypervisorNotSupported:
            template = get_template(
                cls.apiclient,
                cls.zone.id,
                cls.services["ostype"]
            )
            if template == FAILED:
                assert False, "get_template() failed to return template with description %s" % cls.services[
                    "ostype"]

            cls.services["domainid"] = cls.domain.id
            cls.services["small"]["zoneid"] = cls.zone.id
            cls.services["templates"]["ostypeid"] = template.ostypeid
            cls.services["zoneid"] = cls.zone.id

            # Create VMs, NAT Rules etc
            cls.account = Account.create(
                cls.apiclient,
                cls.services["account"],
                domainid=cls.domain.id
            )
            cls._cleanup.append(cls.account)
            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.services["service_offerings"]["tiny"]
            )
            cls._cleanup.append(cls.service_offering)
            cls.virtual_machine = cls.virtual_machine_with_disk = \
                VirtualMachine.create(
                    cls.apiclient,
                    cls.services["small"],
                    templateid=template.id,
                    accountid=cls.account.name,
                    domainid=cls.account.domainid,
                    zoneid=cls.zone.id,
                    serviceofferingid=cls.service_offering.id,
                    mode=cls.services["mode"]
                )
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)
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
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @skipTestIf("hypervisorNotSupported")
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_01_snapshot_root_disk(self):
        """Test Snapshot Root Disk
        """

        # Validate the following
        # 1. listSnapshots should list the snapshot that was created.
        # 2. verify that secondary storage NFS share contains
        #    the reqd volume under
        #    /secondary/snapshots//$account_id/$volumeid/$snapshot_uuid
        # 3. verify backup_snap_id was non null in the `snapshots` table
        # 4. Verify that zoneid is returned in listSnapshots API response

        volumes = list_volumes(
            self.apiclient,
            virtualmachineid=self.virtual_machine_with_disk.id,
            type='ROOT',
            listall=True
        )

        snapshot = Snapshot.create(
            self.apiclient,
            volumes[0].id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Snapshot created: ID - %s" % snapshot.id)

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

        self.assertIsNotNone(snapshots[0].zoneid,
                             "Zone id is not none in listSnapshots")
        self.assertEqual(
            snapshots[0].zoneid,
            self.zone.id,
            "Check zone id in the list snapshots"
        )

        self.debug(
            "select backup_snap_id, account_id, volume_id from snapshots where uuid = '%s';"
            % str(snapshot.id)
        )
        qresultset = self.dbclient.execute(
            "select backup_snap_id, account_id, volume_id from snapshots where uuid = '%s';"
            % str(snapshot.id)
        )
        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )

        qresult = qresultset[0]

        snapshot_uuid = qresult[0]      # backup_snap_id = snapshot UUID

        self.assertNotEqual(
            str(snapshot_uuid),
            'NULL',
            "Check if backup_snap_id is not null"
        )

        self.assertTrue(is_snapshot_on_nfs(
            self.apiclient, self.dbclient, self.config, self.zone.id, snapshot.id))
        return
