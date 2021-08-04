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
""" Test cases for VM/Volume snapshot Test Path
"""
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.utils import (cleanup_resources,
                              is_snapshot_on_nfs,
                              validateList)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             DiskOffering,
                             Template,
                             VirtualMachine,
                             Snapshot,
                             Volume
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_volumes,
                               list_snapshots,
                               list_events,
                               createChecksum,
                               compareChecksum
                               )

from marvin.codes import PASS
from threading import Thread


class TestVolumeSnapshot(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestVolumeSnapshot, cls).getClsTestClient()
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

        if cls.hypervisor.lower() not in [
                "vmware",
                "kvm",
                "xenserver"]:
            raise unittest.SkipTest(
                "Storage migration not supported on %s" %
                cls.hypervisor)

        try:
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
            # Create Service offering
            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"],
            )
            cls._cleanup.append(cls.service_offering)
            # Create Disk offering
            cls.disk_offering = DiskOffering.create(
                cls.apiclient,
                cls.testdata["disk_offering"],
            )
            cls._cleanup.append(cls.disk_offering)
            #Create VM_1 and  VM_2   
            cls.vm_1 = VirtualMachine.create(
                cls.userapiclient,
                cls.testdata["small"],
                templateid=cls.template.id,
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                serviceofferingid=cls.service_offering.id,
                zoneid=cls.zone.id,
                diskofferingid=cls.disk_offering.id,
                mode=cls.zone.networktype
            )

            cls.vm_2 = VirtualMachine.create(
                cls.userapiclient,
                cls.testdata["small"],
                templateid=cls.template.id,
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                serviceofferingid=cls.service_offering.id,
                zoneid=cls.zone.id,
                diskofferingid=cls.disk_offering.id,
                mode=cls.zone.networktype
            )

        except Exception as e:
            cls.tearDownClass()
            raise e
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            root_volume = list_volumes(
                    self.apiclient,
                    virtualmachineid=self.vm_1.id,
                    type='ROOT',
                    listall=True
                    )

            self.vm_1.stop(self.apiclient)
            snaps = []
            for i in range(2):

                root_vol_snap = Snapshot.create(
                    self.apiclient,
                    root_volume[0].id)

               
                self.assertEqual(
                    root_vol_snap.state,
                    "BackedUp",
                    "Check if the data vol snapshot state is correct "
                )

                snaps.append(root_vol_snap)

            for snap in snaps:

                self.assertNotEqual(
                        self.dbclient.execute(
                            "select status from snapshots where name='%s'" %
                            snap.name),
                        "Destroyed"
                        )

            for snap in snaps:
                self.assertTrue(
                       is_snapshot_on_nfs(
                           self.apiclient,
                           self.dbclient,
                           self.config,
                           self.zone.id,
                           snap.id))
            
            self.account.delete(self.apiclient)

            for snap in snaps:
                self.assertEqual(
                        self.dbclient.execute(
                            "select status from snapshots where name='%s'" %
                            snap.name)[0][0],
                        "Destroyed"
                        )

            for snap in snaps:
                self.assertFalse(
                       is_snapshot_on_nfs(
                           self.apiclient,
                           self.dbclient,
                           self.config,
                           self.zone.id,
                           snap.id))

            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_01_volume_snapshot(self):
        """ Test Volume (root) Snapshot
        # 1. Deploy a VM on primary storage and .
        # 2. Take snapshot on root disk
        # 3. Verify the snapshot's entry in the "snapshots" table 
                and presence of the corresponding 
                snapshot on the Secondary Storage
        # 4. Create Template from the Snapshot and Deploy a 
                VM using the Template
        # 5. Log in to the VM from template and make verify 
                the contents of the ROOT disk matches with the snapshot.
        # 6. Delete Snapshot and Deploy a Linux VM from the 
             Template and verify the successful deployment of the VM.
        # 7. Create multiple snapshots on the same volume and 
                Check the integrity of all the snapshots by creating 
                a template from the snapshot and deploying a Vm from it 
                and delete one of the snapshots
        # 8. Verify that the original checksum matches with the checksum 
                of VM's created from remaning snapshots
        # 9. Make verify the contents of the ROOT disk 
                matches with the snapshot
        # 10.Verify that Snapshot of both DATA and ROOT volume should 
                succeed when snapshot of Data disk of a VM is taken 
                when snapshot of ROOT volume of VM is in progress
        # 11.Create snapshot of data disk and verify the original checksum 
                matches with the volume created from snapshot
        # 12.Verify that volume's state should not change when snapshot of 
                a DATA volume is taken that is attached to a VM
        # 13.Verify that volume's state should not change when snapshot of 
                a DATA volume is taken that is not attached to a VM
        # 14.Verify that create Snapshot with quiescevm=True should succeed
        # 15.revertSnapshot() to revert VM to a specified 
                Volume snapshot for root volume
        """

        # Step 1
        # Get ROOT Volume Id
        root_volumes_cluster_list = list_volumes(
            self.apiclient,
            virtualmachineid=self.vm_1.id,
            type='ROOT',
            listall=True
        )

        root_volume_cluster = root_volumes_cluster_list[0]

        disk_volumes_cluster_list = list_volumes(
            self.apiclient,
            virtualmachineid=self.vm_1.id,
            type='DATADISK',
            listall=True
        )

        data_disk = disk_volumes_cluster_list[0]

        root_vol_state = root_volume_cluster.state

        ckecksum_random_root_cluster = createChecksum(
            service=self.testdata,
            virtual_machine=self.vm_1,
            disk=root_volume_cluster,
            disk_type="rootdiskdevice")

        self.vm_1.stop(self.apiclient)
        root_vol_snap = Snapshot.create(
            self.apiclient,
            root_volume_cluster.id)

        self.assertEqual(
            root_vol_snap.state,
            "BackedUp",
            "Check if the snapshot state is correct "
        )

        self.assertEqual(
            root_vol_state,
            root_volume_cluster.state,
            "Check if volume state has changed"
        )

        self.vm_1.start(self.apiclient)
        # Step 2
        snapshot_list = list_snapshots(
            self.apiclient,
            id=root_vol_snap.id
        )

        self.assertNotEqual(
            snapshot_list,
            None,
            "Check if result exists in list item call"
        )
        self.assertEqual(
            snapshot_list[0].id,
            root_vol_snap.id,
            "Check resource id in list resources call"
        )

        self.assertTrue(
            is_snapshot_on_nfs(
                self.apiclient,
                self.dbclient,
                self.config,
                self.zone.id,
                root_vol_snap.id))

        events = list_events(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            type='SNAPSHOT.CREATE')

        event_list_validation_result = validateList(events)

        self.assertEqual(
            event_list_validation_result[0],
            PASS,
            "event list validation failed due to %s" %
            event_list_validation_result[2])
        self.debug("Events list contains event SNAPSHOT.CREATE")

        qresultset = self.dbclient.execute(
            "select * from event where type='SNAPSHOT.CREATE' AND \
                    description like '%%%s%%' AND state='Completed';" %
            root_volume_cluster.id)

        event_validation_result = validateList(qresultset)

        self.assertEqual(
            event_validation_result[0],
            PASS,
            "event list validation failed due to %s" %
            event_validation_result[2])

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )

        qresult = str(qresultset)
        self.assertEqual(
            qresult.count('SNAPSHOT.CREATE') > 0,
            True,
            "Check SNAPSHOT.CREATE event in events table"
        )

        #Usage_Event
        qresultset = self.dbclient.execute(
                "select * from usage_event where type='SNAPSHOT.CREATE' AND \
                        resource_name='%s'" %
                root_vol_snap.name)

        usage_event_validation_result = validateList(qresultset)

        self.assertEqual(
               usage_event_validation_result[0],
               PASS,
               "event list validation failed due to %s" %
               usage_event_validation_result[2])

        self.assertNotEqual(
               len(qresultset),
               0,
               "Check DB Query result set"
              )

        self.assertEqual(
            self.dbclient.execute("select size from usage_event where type='SNAPSHOT.CREATE' AND \
            resource_name='%s'" %
            root_vol_snap.name)[0][0],
            root_vol_snap.physicalsize)

        # Step 3
        # create template from snapshot root_vol_snap
        templateFromSnapshot = Template.create_from_snapshot(
            self.apiclient,
            root_vol_snap,
            self.testdata["template_2"])

        self.assertNotEqual(
            templateFromSnapshot,
            None,
            "Check if result exists in list item call"
        )

        vm_from_temp = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=templateFromSnapshot.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        self.assertNotEqual(
            vm_from_temp,
            None,
            "Check if result exists in list item call"
        )

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=ckecksum_random_root_cluster,
            disk_type="rootdiskdevice",
            virt_machine=vm_from_temp
        )
        vm_from_temp.delete(self.apiclient)
        # Step 4
        root_vol_snap.delete(self.userapiclient)

        self.assertEqual(
            list_snapshots(
                self.apiclient,
                volumeid=root_volume_cluster.id,
            ), None, "Snapshot list should be empty")

        events = list_events(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            type='SNAPSHOT.DELETE')

        event_list_validation_result = validateList(events)

        self.assertEqual(
            event_list_validation_result[0],
            PASS,
            "event list validation failed due to %s" %
            event_list_validation_result[2])

        self.debug("Events list contains event SNAPSHOT.DELETE")

        self.debug("select id from account where uuid = '%s';"
                   % self.account.id)

        qresultset = self.dbclient.execute(
            "select id from account where uuid = '%s';"
            % self.account.id
        )

        account_validation_result = validateList(qresultset)

        self.assertEqual(
            account_validation_result[0],
            PASS,
            "event list validation failed due to %s" %
            account_validation_result[2])

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        account_id = qresult[0]

        qresultset = self.dbclient.execute(
            "select * from event where type='SNAPSHOT.DELETE' AND \
                    account_id='%s' AND state='Completed';" %
            account_id)

        delete_snap_validation_result = validateList(qresultset)

        self.assertEqual(
            delete_snap_validation_result[0],
            PASS,
            "event list validation failed due to %s" %
            delete_snap_validation_result[2])

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )

        qresult = str(qresultset)
        self.assertEqual(
            qresult.count('SNAPSHOT.DELETE') > 0,
            True,
            "Check SNAPSHOT.DELETE event in events table"
        )

        # Step 5
        # delete snapshot and deploy vm from snapshot
        vm_from_temp_2 = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=templateFromSnapshot.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        self.assertNotEqual(
            vm_from_temp_2,
            None,
            "Check if result exists in list item call"
        )

        # Step 6:
        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=ckecksum_random_root_cluster,
            disk_type="rootdiskdevice",
            virt_machine=vm_from_temp_2
        )

        vm_from_temp_2.delete(self.apiclient)
        # Step 7
        # Multiple Snapshots
        self.vm_1.stop(self.apiclient)
        snaps = []
        for i in range(2):

            root_vol_snap = Snapshot.create(
                self.apiclient,
                root_volume_cluster.id)

            self.assertEqual(
                root_vol_snap.state,
                "BackedUp",
                "Check if the data vol snapshot state is correct "
            )

            snaps.append(root_vol_snap)

            templateFromSnapshot = Template.create_from_snapshot(
                self.apiclient,
                root_vol_snap,
                self.testdata["template_2"])

            self.assertNotEqual(
                templateFromSnapshot,
                None,
                "Check if result exists in list item call"
            )

            vm_from_temp = VirtualMachine.create(
                self.userapiclient,
                self.testdata["small"],
                templateid=templateFromSnapshot.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                zoneid=self.zone.id,
                mode=self.zone.networktype
            )

            self.assertNotEqual(
                vm_from_temp,
                None,
                "Check if result exists in list item call"
            )

            compareChecksum(
                self.apiclient,
                service=self.testdata,
                original_checksum=ckecksum_random_root_cluster,
                disk_type="rootdiskdevice",
                virt_machine=vm_from_temp
            )
            vm_from_temp.delete(self.apiclient)
            templateFromSnapshot.delete(self.apiclient)

        self.vm_1.start(self.apiclient)

        delete_snap = snaps.pop(1)
        delete_snap.delete(self.apiclient)

        self.assertEqual(
            Snapshot.list(
                self.apiclient,
                id=delete_snap.id
            ), None, "Snapshot list should be empty")

        # Step 8
        for snap in snaps:

            templateFromSnapshot = Template.create_from_snapshot(
                self.apiclient,
                snap,
                self.testdata["template_2"])

            self.assertNotEqual(
                templateFromSnapshot,
                None,
                "Check if result exists in list item call"
            )

            vm_from_temp = VirtualMachine.create(
                self.userapiclient,
                self.testdata["small"],
                templateid=templateFromSnapshot.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                zoneid=self.zone.id,
                mode=self.zone.networktype
            )

            self.assertNotEqual(
                vm_from_temp,
                None,
                "Check if result exists in list item call"
            )

            compareChecksum(
                self.apiclient,
                service=self.testdata,
                original_checksum=ckecksum_random_root_cluster,
                disk_type="rootdiskdevice",
                virt_machine=vm_from_temp
            )

            templateFromSnapshot.delete(self.apiclient)
            vm_from_temp.delete(self.apiclient)

        for snap in snaps:
            snap.delete(self.apiclient)

        # Step 9
        ckecksum_root_cluster = createChecksum(
            service=self.testdata,
            virtual_machine=self.vm_1,
            disk=root_volume_cluster,
            disk_type="rootdiskdevice")

        self.vm_1.stop(self.apiclient)

        root_vol_snap_2 = Snapshot.create(
            self.apiclient,
            root_volume_cluster.id)

        self.assertEqual(
            root_vol_snap_2.state,
            "BackedUp",
            "Check if the data vol snapshot state is correct "
        )
        snap_list_validation_result = validateList(events)

        self.assertEqual(
            snap_list_validation_result[0],
            PASS,
            "snapshot list validation failed due to %s" %
            snap_list_validation_result[2])

        self.assertNotEqual(
            snapshot_list,
            None,
            "Check if result exists in list item call"
        )

        templateFromSnapshot = Template.create_from_snapshot(
            self.apiclient,
            root_vol_snap_2,
            self.testdata["template_2"])

        self.debug(
            "create template event comlites with template %s name" %
            templateFromSnapshot.name)

        self.assertNotEqual(
            templateFromSnapshot,
            None,
            "Check if result exists in list item call"
        )

        vm_from_temp_2 = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=templateFromSnapshot.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        self.assertNotEqual(
            vm_from_temp_2,
            None,
            "Check if result exists in list item call"
        )

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=ckecksum_root_cluster,
            disk_type="rootdiskdevice",
            virt_machine=vm_from_temp_2
        )

        vm_from_temp_2.delete(self.apiclient)

        # Step 10
        # Take snapshot of Data disk of a VM , when snapshot of ROOT volume of 
        # VM is in progress
        try:
            self.vm_1.stop(self.apiclient)

            t1 = Thread(
                target=Snapshot.create,
                args=(
                    self.apiclient,
                    root_volume_cluster.id
                ))

            t2 = Thread(
                target=Snapshot.create,
                args=(
                    self.apiclient,
                    data_disk.id
                ))

            t1.start()
            t2.start()
            t1.join()
            t2.join()

        except:
            self.debug("Error: unable to start thread")

        # Step 11
        # Data Disk
        self.vm_1.start(self.apiclient)
        ckecksum_data_disk = createChecksum(
            service=self.testdata,
            virtual_machine=self.vm_1,
            disk=data_disk,
            disk_type="datadiskdevice_1")

        data_vol_state = data_disk.state

        self.vm_1.stop(self.apiclient)

        data_vol_snap = Snapshot.create(
            self.apiclient,
            data_disk.id)

        self.assertEqual(
            data_vol_snap.state,
            "BackedUp",
            "Check if the data vol snapshot state is correct "
        )

        self.assertEqual(
            data_vol_state,
            data_disk.state,
            "Check if volume state has changed"
        )

        data_snapshot_list = list_snapshots(
            self.apiclient,
            id=data_vol_snap.id
        )

        self.assertNotEqual(
            data_snapshot_list,
            None,
            "Check if result exists in list item call"
        )
        self.assertEqual(
            data_snapshot_list[0].id,
            data_vol_snap.id,
            "Check resource id in list resources call"
        )

        self.assertTrue(
            is_snapshot_on_nfs(
                self.apiclient,
                self.dbclient,
                self.config,
                self.zone.id,
                data_vol_snap.id))

        events = list_events(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            type='SNAPSHOT.CREATE')

        event_list_validation_result = validateList(events)

        self.assertEqual(
            event_list_validation_result[0],
            PASS,
            "event list validation failed due to %s" %
            event_list_validation_result[2])
        self.debug("Events list contains event SNAPSHOT.CREATE")

	self.testdata["volume"]["zoneid"] = self.zone.id
        volumeFromSnap = Volume.create_from_snapshot(
            self.apiclient,
            data_vol_snap.id,
            self.testdata["volume"],
            account=self.account.name,
            domainid=self.account.domainid,
        )

        self.assertTrue(
            is_snapshot_on_nfs(
                self.apiclient,
                self.dbclient,
                self.config,
                self.zone.id,
                data_vol_snap.id))

        new_vm = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        new_vm.attach_volume(
            self.apiclient,
            volumeFromSnap
        )

        new_vm.reboot(self.apiclient)

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=ckecksum_data_disk,
            disk_type="datadiskdevice_1",
            virt_machine=new_vm
        )

        # Step 12
        data_volume_2 = Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering.id
        )

        self.vm_1.start(self.apiclient)
        self.vm_1.attach_volume(
            self.userapiclient,
            data_volume_2
        )

        self.vm_1.reboot(self.apiclient)       
        self.vm_1.stop(self.apiclient)

        data_vol_snap_1 = Snapshot.create(
            self.apiclient,
            data_volume_2.id)

        self.assertEqual(
            data_vol_snap_1.state,
            "BackedUp",
            "Check if the snapshot state is correct "
        )

        data_disk_2_list = Volume.list(
            self.userapiclient,
            listall=self.testdata["listall"],
            id=data_volume_2.id
        )

        self.vm_1.start(self.apiclient)

        checksum_data_2 = createChecksum(
            service=self.testdata,
            virtual_machine=self.vm_1,
            disk=data_disk_2_list[0],
            disk_type="datadiskdevice_2")

        # Step 13
        self.vm_1.detach_volume(self.apiclient,
                           data_volume_2)

        self.vm_1.reboot(self.apiclient)

        prev_state = data_volume_2.state

        data_vol_snap_2 = Snapshot.create(
            self.apiclient,
            data_volume_2.id)

        self.assertEqual(
            data_vol_snap_2.state,
            prev_state,
            "Check if the volume state is correct "
        )

        data_snapshot_list_2 = list_snapshots(
            self.apiclient,
            id=data_vol_snap_2.id
        )

        self.assertNotEqual(
            data_snapshot_list_2,
            None,
            "Check if result exists in list item call"
        )

        self.assertEqual(
            data_snapshot_list_2[0].id,
            data_vol_snap_2.id,
            "Check resource id in list resources call"
        )

	self.testdata["volume"]["zoneid"] = self.zone.id
        volumeFromSnap_2 = Volume.create_from_snapshot(
            self.apiclient,
            data_vol_snap_2.id,
            self.testdata["volume"],
            account=self.account.name,
            domainid=self.account.domainid,
        )

        self.vm_2.attach_volume(
            self.userapiclient,
            volumeFromSnap_2
        )

        self.vm_2.reboot(self.apiclient)

        data_disk_2_list = Volume.list(
            self.userapiclient,
            listall=self.testdata["listall"],
            id=volumeFromSnap_2.id
        )

        compareChecksum(
            self.apiclient,
            service=self.testdata,
            original_checksum=checksum_data_2,
            disk_type="datadiskdevice_2",
            virt_machine=self.vm_2
        )

        # Step 14
        self.vm_1.stop(self.apiclient)
        with self.assertRaises(Exception):
            root_vol_snap.revertVolToSnapshot(self.apiclient)

        # Step 15
        root_snap = Snapshot.create(
            self.apiclient,
            root_volume_cluster.id)

        with self.assertRaises(Exception):
            root_snap.revertVolToSnapshot(self.apiclient)

        return
