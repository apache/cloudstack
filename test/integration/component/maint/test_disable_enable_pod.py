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
""" Test cases for Disable enable Pod Test Path
"""

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources)
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             Pod,
                             Template,
                             Snapshot,
                             Volume,
                             DiskOffering,
                             Iso
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               get_pod,
                               list_volumes,
                               list_snapshots,
                               get_builtin_template_info
                               )

from marvin.cloudstackAPI import updatePod
from marvin.codes import (ENABLED,
                          DISABLED,
                          RUNNING)


class TestDisableEnablePod(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDisableEnablePod, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.pod = get_pod(
            cls.apiclient,
            zone_id=cls.zone.id)

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
            podList = Pod.list(cls.apiclient, id=cls.pod.id)
            if podList[0].allocationstate == DISABLED:
                cmd = updatePod.updatePodCmd()
                cmd.id = podList[0].id
                cmd.allocationstate = ENABLED
                cls.apiclient.updatePod(cmd)

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
    def test_01_snapshot_hardning(self):
        """disable enable Pod
            1. Disable pod and verify following things:
                For admin user:
                    -- Should be able to create new vm, snapshot,
                            volume,template,iso in the same pod
                For Non-admin user:
                    -- Should not be able to create new vm, snapshot,
                            volume,template,iso in the same pod
            2. Enable the above disabled pod and verify that:
                -All users should be able to create new vm, snapshot,
                volume,template,iso in the same pod
            3. Try to delete the pod and it should fail with error message:
                - "The pod is not deletable because there are servers
                running in this pod"

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

        cmd = updatePod.updatePodCmd()
        cmd.id = self.pod.id
        cmd.allocationstate = DISABLED
        self.apiclient.updatePod(cmd)
        podList = Pod.list(self.apiclient, id=self.pod.id)

        self.assertEqual(podList[0].allocationstate,
                         DISABLED,
                         "Check if the pod is in disabled state"
                         )
        self.assertEqual(vm_user.state,
                         RUNNING,
                         "Verify that the user vm is running")

        self.assertEqual(vm_root.state,
                         RUNNING,
                         "Verify that the admin vm is running")

        VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
        )

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
                            "Verify that admin should be able \
                                    to create snapshot")

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

        with self.assertRaises(Exception):
            VirtualMachine.create(self.userapiclient,
                                  self.testdata["small"],
                                  templateid=self.template.id,
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  zoneid=self.zone.id,
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
        self.apiclient.updatePod(cmd)
        podList = Pod.list(self.apiclient, id=self.pod.id)

        self.assertEqual(podList[0].allocationstate,
                         ENABLED,
                         "Check if the pod is in enabled state"
                         )

        root_vm_new = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
        )
        self.assertNotEqual(root_vm_new,
                            None,
                            "Verify that admin should be able \
                                    to create new VM")

        snap = Snapshot.create(
            self.apiclient,
            root_volume[0].id)

        self.assertNotEqual(snap,
                            None,
                            "Verify that admin should be able to \
                                    create snashot")

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
            zoneid=self.zone.id,
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
        # Deletion of zone should fail if resources are running on the zone
        with self.assertRaises(Exception):
            self.pod.delete(self.apiclient)

        return
