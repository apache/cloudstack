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
""" Test cases for Disable enable Zone, Pod, Cluster, Host Test Path
"""

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             Zone,
                             Cluster,
                             Host,
                             Pod,
                             Template,
                             Snapshot,
                             Volume,
                             DiskOffering,
                             Iso
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_pod,
                               get_template,
                               list_volumes,
                               list_snapshots,
                               get_builtin_template_info
                               )

from marvin.cloudstackAPI import (updateZone,
                                  updateHost,
                                  reconnectHost,
                                  updatePod,
                                  updateCluster
                                  )

from marvin.codes import (ENABLED,
                          DISABLED,
                          ENABLE,
                          DISABLE,
                          PASS
                          )
import time
from marvin.sshClient import SshClient


class TestDisableEnableZone(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDisableEnableZone, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.snapshotSupported = True
        if cls.hypervisor.lower() in ["hyperv", "lxc"]:
            cls.snapshotSupported = False

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

            # Create root admin account

            cls.admin_account = Account.create(
                cls.apiclient,
                cls.testdata["account2"],
                admin=True

            )
            cls._cleanup.append(cls.admin_account)

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
            if zoneList[0].allocationstate.lower() == DISABLED.lower():
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
            accountid=self.admin_account.name,
            domainid=self.admin_account.domainid,
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
        self.assertEqual(vm_user.state.lower(),
                         "running",
                         "Verify that the user vm is running")

        self.assertEqual(vm_root.state.lower(),
                         "running",
                         "Verify that the admin vm is running")

        vm_root.stop(self.apiclient)
        vm_user.stop(self.apiclient)

        root_state = self.dbclient.execute(
            "select state from vm_instance where name='%s'" %
            vm_root.name)[0][0]

        user_state = self.dbclient.execute(
            "select state from vm_instance where name='%s'" %
            vm_user.name)[0][0]

        self.assertEqual(root_state.lower(),
                         "stopped",
                         "verify that vm is Stopped")

        self.assertEqual(user_state.lower(),
                         "stopped",
                         "verify that vm is stopped")

        root_volume = list_volumes(
            self.apiclient,
            virtualmachineid=vm_root.id,
            type='ROOT',
            listall=True
        )

        self.assertEqual(validateList(root_volume)[0],
                         PASS,
                         "list volume  is empty for vmid %s" % vm_root.id
                         )
        root_vm_new = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.admin_account.name,
            domainid=self.admin_account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id
        )

        self.assertEqual(root_vm_new.state.lower(),
                         "running",
                         "Verify that admin should create new VM")

        if self.snapshotSupported:
            Snapshot.create(
                self.apiclient,
                root_volume[0].id)

            snapshots = list_snapshots(
                self.apiclient,
                volumeid=root_volume[0].id,
                listall=True)

            self.assertEqual(validateList(snapshots)[0],
                             PASS,
                             "list snapshot  is empty for volume id %s"
                             % root_volume[0].id
                             )

            Template.create_from_snapshot(
                self.apiclient,
                snapshots[0],
                self.testdata["privatetemplate"])

        builtin_info = get_builtin_template_info(self.apiclient, self.zone.id)
        self.testdata["privatetemplate"]["url"] = builtin_info[0]
        self.testdata["privatetemplate"]["hypervisor"] = builtin_info[1]
        self.testdata["privatetemplate"]["format"] = builtin_info[2]
        """
        //commenting it for now will uncomment  once expected behaviour is known
        Template.register(
            self.apiclient,
            self.testdata["privatetemplate"],
            zoneid=self.zone.id)
        """
        Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.admin_account.name,
            domainid=self.admin_account.domainid,
            diskofferingid=self.disk_offering.id
        )
        """
        //commenting it for now will uncomment  once expected behaviour is known
        Iso.create(
            self.apiclient,
            self.testdata["iso2"],
            zoneid=self.zone.id,
            account=self.admin_account.name,
            domainid=self.admin_account.domainid,
        )
        """
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
        self.assertEqual(validateList(root_volume)[0],
                         PASS,
                         "list volume  is empty for vmid id %s" % vm_user.id
                         )

        if self.snapshotSupported:
            with self.assertRaises(Exception):
                Snapshot.create(
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
            Iso.create(
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
            accountid=self.admin_account.name,
            domainid=self.admin_account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id
        )

        self.assertEqual(root_vm_new.state.lower(),
                         "running",
                         "Verify that admin should create new VM")

        if self.snapshotSupported:
            Snapshot.create(
                self.apiclient,
                root_volume[0].id)

            snapshots = list_snapshots(
                self.apiclient,
                volumeid=root_volume[0].id,
                listall=True)

            self.assertEqual(validateList(snapshots)[0],
                             PASS,
                             "list snapshot  is empty for volume id %s"
                             % root_volume[0].id
                             )

            Template.create_from_snapshot(
                self.apiclient,
                snapshots[0],
                self.testdata["privatetemplate"])

        Template.register(
            self.apiclient,
            self.testdata["privatetemplate"],
            zoneid=self.zone.id)

        Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.admin_account.name,
            domainid=self.admin_account.domainid,
            diskofferingid=self.disk_offering.id
        )

        Iso.create(
            self.apiclient,
            self.testdata["iso2"],
            zoneid=self.zone.id,
            account=self.admin_account.name,
            domainid=self.admin_account.domainid,
        )

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

        self.assertEqual(user_vm_new.state.lower(),
                         "running",
                         "Verify that admin should create new VM")

        if self.snapshotSupported:
            Snapshot.create(
                self.userapiclient,
                root_volume[0].id)

            snapshots = list_snapshots(
                self.userapiclient,
                volumeid=root_volume[0].id,
                listall=True)

            self.assertEqual(validateList(snapshots)[0],
                             PASS,
                             "list snapshot  is empty for volume id %s"
                             % root_volume[0].id
                             )

        Template.register(
            self.userapiclient,
            self.testdata["privatetemplate"],
            zoneid=self.zone.id)

        Volume.create(
            self.userapiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering.id
        )
        Iso.create(
            self.userapiclient,
            self.testdata["iso2"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
        )

        # Step 3
        # Deletion of zone should fail if vm,volume is present on the zone
        with self.assertRaises(Exception):
            self.zone.delete(self.apiclient)

        return


class TestDisableEnablePod(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDisableEnablePod, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        cls.snapshotSupported = True
        if cls.hypervisor.lower() in ["hyperv", "lxc"]:
            cls.snapshotSupported = False

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

            # Create root admin account

            cls.admin_account = Account.create(
                cls.apiclient,
                cls.testdata["account2"],
                admin=True

            )
            cls._cleanup.append(cls.admin_account)

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
            if podList[0].allocationstate.lower() == DISABLED.lower():
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
    def test_01_disable_enable_pod(self):
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
            accountid=self.admin_account.name,
            domainid=self.admin_account.domainid,
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
        self.assertEqual(vm_user.state.lower(),
                         "running",
                         "Verify that the user vm is running")

        self.assertEqual(vm_root.state.lower(),
                         "running",
                         "Verify that the admin vm is running")

        VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.admin_account.name,
            domainid=self.admin_account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
        )

        root_volume = list_volumes(
            self.apiclient,
            virtualmachineid=vm_root.id,
            type='ROOT',
            listall=True
        )
        self.assertEqual(
            validateList(root_volume)[0],
            PASS,
            "list snapshot  is empty for volume id %s" %
            vm_root.id)

        if self.snapshotSupported:
            Snapshot.create(
                self.apiclient,
                root_volume[0].id)

            snapshots = list_snapshots(
                self.apiclient,
                volumeid=root_volume[0].id,
                listall=True)
            self.assertEqual(
                validateList(snapshots)[0],
                PASS,
                "list snapshot  is empty for volume id %s" %
                root_volume[0].id)

            Template.create_from_snapshot(
                self.apiclient,
                snapshots[0],
                self.testdata["privatetemplate"])

        builtin_info = get_builtin_template_info(self.apiclient, self.zone.id)
        self.testdata["privatetemplate"]["url"] = builtin_info[0]
        self.testdata["privatetemplate"]["hypervisor"] = builtin_info[1]
        self.testdata["privatetemplate"]["format"] = builtin_info[2]

        Template.register(
            self.apiclient,
            self.testdata["privatetemplate"],
            zoneid=self.zone.id)

        Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.admin_account.name,
            domainid=self.admin_account.domainid,
            diskofferingid=self.disk_offering.id
        )

        Iso.create(
            self.apiclient,
            self.testdata["iso2"],
            zoneid=self.zone.id,
            account=self.admin_account.name,
            domainid=self.admin_account.domainid,
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

        self.assertEqual(validateList(root_volume)[0],
                         PASS,
                         "list volume  is empty for volume id %s" % vm_user.id
                         )
        if self.snapshotSupported:
            Snapshot.create(
                self.userapiclient,
                root_volume[0].id)

        Template.register(
            self.userapiclient,
            self.testdata["privatetemplate"],
            zoneid=self.zone.id)

        Volume.create(
            self.userapiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering.id
        )

        Iso.create(
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
            accountid=self.admin_account.name,
            domainid=self.admin_account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
        )
        self.assertEqual(root_vm_new.state.lower(),
                         "running",
                         "Verify that admin should be able \
                                    to create new VM")

        if self.snapshotSupported:
            Snapshot.create(
                self.apiclient,
                root_volume[0].id)

            snapshots = list_snapshots(
                self.apiclient,
                volumeid=root_volume[0].id,
                listall=True)

            self.assertEqual(
                validateList(snapshots)[0],
                PASS,
                "list snapshot  is empty for volume id %s" %
                root_volume[0].id)

            Template.create_from_snapshot(
                self.apiclient,
                snapshots[0],
                self.testdata["privatetemplate"])

        Template.register(
            self.apiclient,
            self.testdata["privatetemplate"],
            zoneid=self.zone.id)

        Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.admin_account.name,
            domainid=self.admin_account.domainid,
            diskofferingid=self.disk_offering.id
        )

        Iso.create(
            self.apiclient,
            self.testdata["iso2"],
            zoneid=self.zone.id,
            account=self.admin_account.name,
            domainid=self.admin_account.domainid,
        )

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
        self.assertEqual(user_vm_new.state.lower(),
                         "running",
                         "Verify that admin should create new VM")

        if self.snapshotSupported:
            Snapshot.create(
                self.userapiclient,
                root_volume[0].id)

            snapshots = list_snapshots(
                self.userapiclient,
                volumeid=root_volume[0].id,
                listall=True)
            self.assertEqual(
                validateList(snapshots)[0],
                PASS,
                "list snapshot  is empty for volume id %s" %
                root_volume[0].id)

        Template.register(
            self.userapiclient,
            self.testdata["privatetemplate"],
            zoneid=self.zone.id)

        Volume.create(
            self.userapiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering.id
        )

        Iso.create(
            self.userapiclient,
            self.testdata["iso2"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
        )

        user_vm_new.delete(self.apiclient)
        # Step 3
        # Deletion of zone should fail if resources are running on the zone
        with self.assertRaises(Exception):
            self.pod.delete(self.apiclient)

        return


class TestDisableEnableCluster(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDisableEnableCluster, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.snapshotSupported = True
        if cls.hypervisor.lower() in ["hyperv", "lxc"]:
            cls.snapshotSupported = False

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.pod = get_pod(
            cls.apiclient,
            zone_id=cls.zone.id)

        hostList = Host.list(cls.apiclient, zoneid=cls.zone.id, type="routing")
        clusterList = Cluster.list(cls.apiclient, id=hostList[0].clusterid)
        cls.cluster = Cluster(clusterList[0].__dict__)

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

            # Create root admin account

            cls.admin_account = Account.create(
                cls.apiclient,
                cls.testdata["account2"],
                admin=True

            )

            cls._cleanup.append(cls.admin_account)

            # Create user api client of the account
            cls.userapiclient = testClient.getUserApiClient(
                UserName=cls.account.name,
                DomainName=cls.account.domain
            )
            cls.vm_list = []

        except Exception as e:
            cls.tearDownClass()
            raise e
        return

    @classmethod
    def tearDownClass(cls):
        try:
            clusterList = Cluster.list(cls.apiclient, id=cls.cluster.id)
            if clusterList[0].allocationstate.lower() == DISABLED.lower():
                cmd = updateCluster.updateClusterCmd()
                cmd.id = clusterList[0].id
                cmd.allocationstate = ENABLED
                cls.apiclient.updateCluster(cmd)

            if clusterList[0].managedstate.lower() == "unmanaged":
                cmd = updateCluster.updateClusterCmd()
                cmd.id = clusterList[0].id
                cmd.managedstate = "Managed"
                cls.apiclient.updateCluster(cmd)

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
    def test_01_disable_enable_cluster(self):
        """disable enable cluster
            1. Disable cluster and verify following things:
                For admin user:
                     --Should be able to create new vm, snapshot,
                     volume,template,iso in the same cluster
                For Non-admin user:
                     --Should not be able create new vm, snapshot,
                     volume,template,iso in the same cluster
            2. Enable the above disabled cluster and verify that:
                -All users should be create to deploy new vm, snapshot,
                volume,template,iso in the same cluster
            3. Disable the managestate of the cluster and verify that:
                --Host in the cluster should get disconnected
                --VM's in the cluster are ping-able and ssh to
                --Creation of new VM in the cluster should fail
            4. Enable the managestate of the cluster and verify that:
                --Hosts in the cluster get connected
                --VM's in the cluster are accessible
            5. Try to delete the cluster and it should fail with error message:
                -"The cluster is not deletable because there are
                servers running in this cluster"

        """
        # Step 1
        vm_user = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        self.vm_list.append(vm_user)

        vm_root = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.admin_account.name,
            domainid=self.admin_account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        self.vm_list.append(vm_root)

        cmd = updateCluster.updateClusterCmd()
        cmd.id = self.cluster.id
        cmd.allocationstate = DISABLED
        self.apiclient.updateCluster(cmd)
        clusterList = Cluster.list(self.apiclient, id=self.cluster.id)

        self.assertEqual(clusterList[0].allocationstate,
                         DISABLED,
                         "Check if the cluster is in disabled state"
                         )

        # Verify the existing vms should be running
        self.assertEqual(vm_user.state.lower(),
                         "running",
                         "Verify that the user vm is running")

        self.assertEqual(vm_root.state.lower(),
                         "running",
                         "Verify that the root vm is running")

        VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.admin_account.name,
            domainid=self.admin_account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
        )

        root_volume = list_volumes(
            self.apiclient,
            virtualmachineid=vm_root.id,
            type='ROOT',
            listall=True
        )

        self.assertEqual(
            validateList(root_volume)[0],
            PASS,
            "list root volume response is empty for volume id %s" %
            vm_root.id)

        if self.snapshotSupported:
            Snapshot.create(
                self.apiclient,
                root_volume[0].id)

            snapshots = list_snapshots(
                self.apiclient,
                volumeid=root_volume[0].id,
                listall=True)
            self.assertEqual(
                validateList(snapshots)[0],
                PASS,
                "list snapshot  is empty for volume id %s" %
                root_volume[0].id)

            Template.create_from_snapshot(
                self.apiclient,
                snapshots[0],
                self.testdata["privatetemplate"])

        builtin_info = get_builtin_template_info(self.apiclient, self.zone.id)
        self.testdata["privatetemplate"]["url"] = builtin_info[0]
        self.testdata["privatetemplate"]["hypervisor"] = builtin_info[1]
        self.testdata["privatetemplate"]["format"] = builtin_info[2]

        Template.register(
            self.apiclient,
            self.testdata["privatetemplate"],
            zoneid=self.zone.id)

        Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.admin_account.name,
            domainid=self.admin_account.domainid,
            diskofferingid=self.disk_offering.id
        )

        Iso.create(
            self.apiclient,
            self.testdata["iso2"],
            zoneid=self.zone.id,
            account=self.admin_account.name,
            domainid=self.admin_account.domainid,
        )

        # non-admin user should fail to create vm, snap, temp etc
        with self.assertRaises(Exception):
            VirtualMachine.create(self.userapiclient,
                                  self.testdata["small"],
                                  templateid=self.template.id,
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  zoneid=self.zone.id,
                                  mode=self.zone.networktype
                                  )

        root_volume = list_volumes(
            self.userapiclient,
            virtualmachineid=vm_user.id,
            type='ROOT',
            listall=True
        )

        self.assertEqual(
            validateList(root_volume)[0],
            PASS,
            "list root volume response is empty for volume id %s" %
            vm_user.id)

        if self.snapshotSupported:
            Snapshot.create(
                self.userapiclient,
                root_volume[0].id)

        Template.register(
            self.userapiclient,
            self.testdata["privatetemplate"],
            zoneid=self.zone.id)

        Volume.create(
            self.userapiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering.id
        )

        Iso.create(
            self.userapiclient,
            self.testdata["iso2"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
        )

        # Step 2
        cmd.allocationstate = ENABLED
        self.apiclient.updateCluster(cmd)
        clusterList = Cluster.list(self.apiclient, id=self.cluster.id)
        self.assertEqual(clusterList[0].allocationstate,
                         ENABLED,
                         "Check if the cluster is in disabled state"
                         )

        # After enabling the zone all users should be able to add new VM,
        # volume, templatee and iso

        root_vm_new = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.admin_account.name,
            domainid=self.admin_account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
        )

        self.assertEqual(root_vm_new.state.lower(),
                         "running",
                         "Verify that admin should create new VM")

        if self.snapshotSupported:
            Snapshot.create(
                self.apiclient,
                root_volume[0].id)

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

        self.assertEqual(user_vm_new.state.lower(),
                         "running",
                         "Verify that admin should create new VM")

        if self.snapshotSupported:
            Snapshot.create(
                self.userapiclient,
                root_volume[0].id)

        # Step 3

        cmd = updateCluster.updateClusterCmd()
        cmd.id = self.cluster.id
        cmd.managedstate = "Unmanaged"
        self.apiclient.updateCluster(cmd)
        clusterList = Cluster.list(self.apiclient, id=self.cluster.id)

        self.assertEqual(clusterList[0].managedstate.lower(),
                         "unmanaged",
                         "Check if the cluster is in unmanaged  state"
                         )
        # Hosts in the cluster takes some time to go into disconnected state
        time.sleep(60)
        hostList = Host.list(self.apiclient,
                             clusterid=self.cluster.id)

        for host in hostList:
            self.assertEqual(host.state.lower(),
                             "disconnected",
                             "Check if host in the cluster gets disconnected"
                             )
        exception_list = []
        for vm in self.vm_list:
            try:
                SshClient(
                    vm.ssh_ip,
                    vm.ssh_port,
                    vm.username,
                    vm.password
                )

            except Exception as e:
                exception_list.append(e)

        self.assertEqual(len(exception_list),
                         0,
                         "Check if vm's are accesible"
                         )

        # non-admin user should fail to create vm, snap, temp etc
        with self.assertRaises(Exception):
            VirtualMachine.create(self.userapiclient,
                                  self.testdata["small"],
                                  templateid=self.template.id,
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  zoneid=self.zone.id,
                                  mode=self.zone.networktype
                                  )

        root_volume = list_volumes(
            self.userapiclient,
            virtualmachineid=vm_user.id,
            type='ROOT',
            listall=True
        )

        if self.snapshotSupported:
            with self.assertRaises(Exception):
                Snapshot.create(
                    self.userapiclient,
                    root_volume[0].id)

        Template.register(
            self.userapiclient,
            self.testdata["privatetemplate"],
            zoneid=self.zone.id)

        # Step 4
        cmd.managedstate = "Managed"
        self.apiclient.updateCluster(cmd)
        # After changing the cluster's managestate to Managed hosts in the
        # cluster takes some time to come back to Up state
        time.sleep(120)
        hostList = Host.list(self.apiclient,
                             clusterid=self.cluster.id)
        for host in hostList:
            self.assertEqual(host.state.lower(),
                             "up",
                             "Check if host in the cluster gets up"
                             )

        vm_root.stop(self.apiclient)
        vm_user.stop(self.apiclient)
        root_state = self.dbclient.execute(
            "select state from vm_instance where name='%s'" %
            vm_root.name)[0][0]

        user_state = self.dbclient.execute(
            "select state from vm_instance where name='%s'" %
            vm_user.name)[0][0]

        self.assertEqual(root_state,
                         "Stopped",
                         "verify that vm should stop"
                         )

        self.assertEqual(user_state,
                         "Stopped",
                         "verify that vm should stop"
                         )

        root_vm_new = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.admin_account.name,
            domainid=self.admin_account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
        )

        self.assertEqual(root_vm_new.state.lower(),
                         "running",
                         "Verify that admin should create new VM")

        # Step 5
        # Deletion of zone should fail if resources are running on the zone
        with self.assertRaises(Exception):
            self.pod.delete(self.apiclient)

        return


class TestDisableEnableHost(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDisableEnableHost, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        cls.snapshotSupported = True
        if cls.hypervisor.lower() in ["hyperv", "lxc"]:
            cls.snapshotSupported = False

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.pod = get_pod(
            cls.apiclient,
            zone_id=cls.zone.id)

        hostList = Host.list(cls.apiclient, zoneid=cls.zone.id, type="routing")
        clusterList = Cluster.list(cls.apiclient, id=hostList[0].clusterid)
        cls.host = Host(hostList[0].__dict__)
        cls.cluster = Cluster(clusterList[0].__dict__)

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata["ostype"])

        cls._cleanup = []
        cls.disabledHosts = []

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

            # Create root admin account

            cls.admin_account = Account.create(
                cls.apiclient,
                cls.testdata["account2"],
                admin=True

            )
            cls._cleanup.append(cls.admin_account)

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
            for hostid in cls.disabledHosts:
                hosts = Host.list(cls.apiclient,
                                  id=hostid)
                assert validateList(hosts)[0] == PASS, "hosts\
                        list validation failed"
                if hosts[0].resourcestate.lower() == DISABLED.lower():
                    cmd = updateHost.updateHostCmd()
                    cmd.id = hostid
                    cmd.resourcestate = ENABLED
                    cmd.allocationstate = ENABLE
                    cls.apiclient.updateHost(cmd)
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
    def test_01_disable_enable_host(self):
        """disable enable host
            1. Disable host and verify following things:
                For admin user:
                    1. Should be able to  stop exsiting vms but can not start.
                    2. Should not be able to  deploy new vm,
                       and create snapshot on the same host
                For Non-admin user:
                    1. Should not be able to stop exsiting vms but
                       cant not start
                    2. Should not be create to deploy new vm,
                    snapshot on the same host
            2. Enable the above disabled host and verify that:
                -All users should be create to deploy new vm,
                snapshot on the same host
            3. Try to reconnect the host :
                -Host should get reconnected successfully
        """
        # Step 1
        vm_root = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.admin_account.name,
            domainid=self.admin_account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
        )
        hostid = vm_root.hostid

        vm_user = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
        )

        cmd = updateHost.updateHostCmd()
        cmd.id = hostid
        cmd.resourcestate = DISABLED
        cmd.allocationstate = DISABLE
        self.apiclient.updateHost(cmd)
        self.disabledHosts.append(hostid)

        hostList = Host.list(self.apiclient, id=hostid)

        self.assertEqual(hostList[0].resourcestate,
                         DISABLED,
                         "Check if the host is in disabled state"
                         )
        # Verify the exsisting vms should be running
        self.assertEqual(vm_user.state.lower(),
                         "running",
                         "Verify that the user vm is running")

        self.assertEqual(vm_root.state.lower(),
                         "running",
                         "Verify that the root vm is running")

        vm_root.stop(self.apiclient)
        vm_user.stop(self.apiclient)
        root_state = self.dbclient.execute(
            "select state from vm_instance where name='%s'" %
            vm_root.name)[0][0]
        user_state = self.dbclient.execute(
            "select state from vm_instance where name='%s'" %
            vm_user.name)[0][0]

        self.assertEqual(root_state.lower(),
                         "stopped",
                         "verify that vm should stop")

        self.assertEqual(user_state.lower(),
                         "stopped",
                         "verify that vm should stop")

        with self.assertRaises(Exception):
            VirtualMachine.create(
                self.apiclient,
                self.testdata["small"],
                templateid=self.template.id,
                accountid=self.admin_account.name,
                domainid=self.admin_account.domainid,
                serviceofferingid=self.service_offering.id,
                zoneid=self.zone.id,
                hostid=hostid)

        root_volume = list_volumes(
            self.apiclient,
            virtualmachineid=vm_root.id,
            type='ROOT',
            listall=True
        )

        self.assertEqual(
            validateList(root_volume)[0],
            PASS,
            "list root volume response is empty for volume id %s" %
            vm_root.id)

        if self.snapshotSupported:
            with self.assertRaises(Exception):
                Snapshot.create(
                    self.apiclient,
                    root_volume[0].id)

        # non-admin user should fail to create vm, snap, temp etc
        with self.assertRaises(Exception):
            VirtualMachine.create(
                self.apiclient,
                self.testdata["small"],
                templateid=self.template.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                zoneid=self.zone.id,
                hostid=hostid)
        root_volume = list_volumes(
            self.apiclient,
            virtualmachineid=vm_user.id,
            type='ROOT',
            listall=True
        )
        self.assertEqual(
            validateList(root_volume)[0],
            PASS,
            "list root volume response is empty for volume id %s" %
            vm_user.id)
        if self.snapshotSupported:
            with self.assertRaises(Exception):
                Snapshot.create(
                    self.userapiclient,
                    root_volume[0].id)

        # Step 2
        cmd.resourcestate = ENABLED
        cmd.allocationstate = ENABLE
        self.apiclient.updateHost(cmd)

        hostList = Host.list(self.apiclient, id=hostid)

        self.assertEqual(hostList[0].resourcestate,
                         ENABLED,
                         "Check if the host is in enabled state"
                         )
        # After enabling the zone all users should be able to add new VM,
        # volume, templatee and iso

        root_vm_new = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.admin_account.name,
            domainid=self.admin_account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            hostid=hostid)

        self.assertEqual(
            root_vm_new.state.lower(),
            "running",
            "Verify that admin should create new VM in running state")

        root_volume = list_volumes(
            self.apiclient,
            virtualmachineid=vm_root.id,
            type='ROOT',
            listall=True
        )

        self.assertEqual(
            validateList(root_volume)[0],
            PASS,
            "list root volume response is empty for volume id %s" %
            vm_root.id)

        if self.snapshotSupported:
            Snapshot.create(
                self.apiclient,
                root_volume[0].id)

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

        self.assertEqual(user_vm_new.state.lower(),
                         "running",
                         "Verify that admin should create new VM")

        root_volume = list_volumes(
            self.apiclient,
            virtualmachineid=vm_user.id,
            type='ROOT',
            listall=True
        )

        self.assertEqual(
            validateList(root_volume)[0],
            PASS,
            "list root volume response is empty for volume id %s" %
            vm_user.id)
        if self.snapshotSupported:
            Snapshot.create(
                self.userapiclient,
                root_volume[0].id)

        # Step 4
        # reconnect the host
        cmd = reconnectHost.reconnectHostCmd()
        cmd.id = hostid
        self.apiclient.reconnectHost(cmd)
        # Host takes some time to come back to Up state so included sleep
        time.sleep(90)
        hostList = Host.list(self.apiclient, id=hostid)

        self.assertEqual(hostList[0].state.lower(),
                         "up",
                         "Check if the host get reconnected successfully"
                         )

        return
