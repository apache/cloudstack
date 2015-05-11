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
""" Test cases for Disable enable Zone Test Path
"""

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources)
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             Zone,
                             Template,
                             Snapshot,
                             Volume,
                             DiskOffering,
                             Iso
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_volumes,
                               list_snapshots,
                               get_builtin_template_info
                               )

from marvin.cloudstackAPI import updateZone
from marvin.codes import (ENABLED,
                          DISABLED,
                          STOPPED,
                          RUNNING)


class TestDisableEnableZone(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDisableEnableZone, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata["ostype"])

        cls._cleanup = []

        try:
            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"],
            )
            cls._cleanup.append(cls.service_offering)

            cls.disk_offering = DiskOffering.create(
                cls.apiclient,
                cls.testdata["disk_offering"],
            )
            cls._cleanup.append(cls.disk_offering)

            # Create an account
            cls.account = Account.create(
                cls.apiclient,
                cls.testdata["account"],
                domainid=cls.domain.id
            )
            cls._cleanup.append(cls.account)

            # Create user api client of the account
            cls.userapiclient = testClient.getUserApiClient(
                UserName=cls.account.name,
                DomainName=cls.account.domain
            )

        except Exception as e:
            cls.tearDownClass()
            raise e
        return

    @classmethod
    def tearDownClass(cls):
        try:
            zoneList = Zone.list(cls.apiclient, id=cls.zone.id)
            if zoneList[0].allocationstate == DISABLED:
                cmd = updateZone.updateZoneCmd()
                cmd.id = zoneList[0].id
                cmd.allocationstate = ENABLED
                cls.apiclient.updateZone(cmd)
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_01_disable_enable_zone(self):
        """disable enable zone
            1. Disable zone and verify following things:
                For admin user:
                    1. Should be create to start/stop exsiting vms
                    2. Should be create to deploy new vm, snapshot,volume,
                       template,iso in the same zone
                For Non-admin user:
                    1. Should be create to start/stop exsiting vms
                    2. Should not be create to deploy new vm, snapshot,volume,
                       template,iso in the same zone
            2. Enable the above disabled zone and verify that:
                -All users should be create to deploy new vm,
                    snapshot,volume,template,iso in the same zone
            3. Try to delete the zone and it should fail with error message:
                -"The zone is not deletable because there are
                    servers running in this zone"
        """
        # Step 1
        vm_user = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id
        )

        vm_root = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id
        )

        cmd = updateZone.updateZoneCmd()
        cmd.id = self.zone.id
        cmd.allocationstate = DISABLED
        self.apiclient.updateZone(cmd)
        zoneList = Zone.list(self.apiclient, id=self.zone.id)

        self.assertEqual(zoneList[0].allocationstate,
                         DISABLED,
                         "Check if the zone is in disabled state"
                         )

        # Both user and admin vms shoul be running
        self.assertEqual(vm_user.state,
                         RUNNING,
                         "Verify that the user vm is running")

        self.assertEqual(vm_root.state,
                         RUNNING,
                         "Verify that the admin vm is running")

        vm_root.stop(self.apiclient)
        vm_user.stop(self.apiclient)

        root_state = self.dbclient.execute(
            "select state from vm_instance where name='%s'" %
            vm_root.name)[0][0]

        user_state = self.dbclient.execute(
            "select state from vm_instance where name='%s'" %
            vm_user.name)[0][0]

        self.assertEqual(root_state,
                         STOPPED,
                         "verify that vm is Stopped")

        self.assertEqual(user_state,
                         STOPPED,
                         "verify that vm is stopped")

        root_volume = list_volumes(
            self.userapiclient,
            virtualmachineid=vm_root.id,
            type='ROOT',
            listall=True
        )

        snap = Snapshot.create(
            self.apiclient,
            root_volume[0].id)

        self.assertNotEqual(snap,
                            None,
                            "Verify that admin should be \
                                    able to create snapshot")

        snapshots = list_snapshots(
            self.apiclient,
            volumeid=root_volume[0].id,
            listall=True)

        template_from_snapshot = Template.create_from_snapshot(
            self.apiclient,
            snapshots[0],
            self.testdata["privatetemplate"])

        self.assertNotEqual(
            template_from_snapshot,
            None,
            "Verify that admin should be able to create template"
        )

        builtin_info = get_builtin_template_info(self.apiclient, self.zone.id)
        self.testdata["privatetemplate"]["url"] = builtin_info[0]
        self.testdata["privatetemplate"]["hypervisor"] = builtin_info[1]
        self.testdata["privatetemplate"]["format"] = builtin_info[2]

        template_regis = Template.register(
            self.apiclient,
            self.testdata["privatetemplate"],
            zoneid=self.zone.id)

        self.assertNotEqual(
            template_regis,
            None,
            "Check if template gets created"
        )
        self.assertNotEqual(
            template_from_snapshot,
            None,
            "Check if template gets created"
        )

        data_volume = Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering.id
        )
        self.assertNotEqual(
            data_volume,
            None,
            "Check if volume gets created"
        )

        ISO = Iso.create(
            self.apiclient,
            self.testdata["iso2"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
        )

        self.assertNotEqual(
            ISO,
            None,
            "Check if volume gets created"
        )
        # non-admin user should fail to create vm, snap, temp etc
        with self.assertRaises(Exception):
            VirtualMachine.create(self.userapiclient,
                                  self.testdata["small"],
                                  templateid=self.template.id,
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  zoneid=self.zone.id
                                  )

        root_volume = list_volumes(
            self.userapiclient,
            virtualmachineid=vm_user.id,
            type='ROOT',
            listall=True
        )

        with self.assertRaises(Exception):
            snap = Snapshot.create(
                self.userapiclient,
                root_volume[0].id)

        with self.assertRaises(Exception):
            Template.register(
                self.userapiclient,
                self.testdata["privatetemplate"],
                zoneid=self.zone.id)

        with self.assertRaises(Exception):
            Volume.create(
                self.userapiclient,
                self.testdata["volume"],
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid,
                diskofferingid=self.disk_offering.id
            )

        with self.assertRaises(Exception):
            ISO = Iso.create(
                self.userapiclient,
                self.testdata["iso2"],
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid,
            )

        # Step 2
        cmd.allocationstate = ENABLED
        self.apiclient.updateZone(cmd)

        # After enabling the zone all users should be able to add new VM,
        # volume, template and iso

        root_vm_new = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id
        )

        self.assertNotEqual(root_vm_new,
                            None,
                            "Verify that admin should create new VM")

        snap = Snapshot.create(
            self.apiclient,
            root_volume[0].id)

        self.assertNotEqual(snap,
                            None,
                            "Verify that admin should snashot")

        snapshots = list_snapshots(
            self.apiclient,
            volumeid=root_volume[0].id,
            listall=True)

        template_from_snapshot = Template.create_from_snapshot(
            self.apiclient,
            snapshots[0],
            self.testdata["privatetemplate"])

        self.assertNotEqual(
            template_from_snapshot,
            None,
            "Check if template gets created"
        )

        template_regis = Template.register(
            self.apiclient,
            self.testdata["privatetemplate"],
            zoneid=self.zone.id)

        self.assertNotEqual(
            template_regis,
            None,
            "Check if template gets created"
        )
        self.assertNotEqual(
            template_from_snapshot,
            None,
            "Check if template gets created"
        )

        data_volume = Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering.id
        )
        self.assertNotEqual(
            data_volume,
            None,
            "Check if volume gets created"
        )

        ISO = Iso.create(
            self.apiclient,
            self.testdata["iso2"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
        )

        self.assertNotEqual(
            ISO,
            None,
            "Check if volume gets created"
        )
        root_vm_new.delete(self.apiclient)
        # Non root user
        user_vm_new = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id
        )

        self.assertNotEqual(user_vm_new,
                            None,
                            "Verify that admin should create new VM")

        snap = Snapshot.create(
            self.userapiclient,
            root_volume[0].id)

        self.assertNotEqual(snap,
                            None,
                            "Verify that admin should snashot")

        snapshots = list_snapshots(
            self.userapiclient,
            volumeid=root_volume[0].id,
            listall=True)

        template_regis = Template.register(
            self.userapiclient,
            self.testdata["privatetemplate"],
            zoneid=self.zone.id)

        self.assertNotEqual(
            template_regis,
            None,
            "Check if template gets created"
        )
        self.assertNotEqual(
            template_from_snapshot,
            None,
            "Check if template gets created"
        )

        data_volume = Volume.create(
            self.userapiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering.id
        )
        self.assertNotEqual(
            data_volume,
            None,
            "Check if volume gets created"
        )

        ISO = Iso.create(
            self.userapiclient,
            self.testdata["iso2"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
        )

        self.assertNotEqual(
            ISO,
            None,
            "Check if volume gets created"
        )
        user_vm_new.delete(self.apiclient)

        # Step 3
        # Deletion of zone should fail if vm,volume is present on the zone
        with self.assertRaises(Exception):
            self.zone.delete(self.apiclient)

        return
