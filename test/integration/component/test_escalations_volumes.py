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

# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.cloudstackAPI import createVolume, createTemplate
from marvin.lib.utils import (cleanup_resources,
                              random_gen, validateList)
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             Volume,
                             DiskOffering,
                             Snapshot,
                             Template,
                             SnapshotPolicy)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               find_storage_pool_type)
from nose.plugins.attrib import attr
from marvin.codes import PASS


class TestVolumes(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(TestVolumes, cls).getClsTestClient()
            cls.api_client = cls.testClient.getApiClient()
            cls.services = cls.testClient.getParsedTestDataConfig()
            cls.hypervisor = cls.testClient.getHypervisorInfo()
            cls.find_storage_pool = True
            if cls.hypervisor.lower() == 'lxc':
                if not find_storage_pool_type(cls.api_client, storagetype='rbd'):
                    cls.find_storage_pool = False
                    return
            # Get Domain, Zone, Template
            cls.domain = get_domain(cls.api_client)
            cls.zone = get_zone(
                cls.api_client,
                cls.testClient.getZoneForTests())
            cls.template = get_template(
                cls.api_client,
                cls.zone.id,
                cls.services["ostype"]
            )
            if cls.zone.localstorageenabled:
                cls.storagetype = 'local'
                cls.services["service_offerings"][
                    "tiny"]["storagetype"] = 'local'
                cls.services["disk_offering"]["storagetype"] = 'local'
            else:
                cls.storagetype = 'shared'
                cls.services["service_offerings"][
                    "tiny"]["storagetype"] = 'shared'
                cls.services["disk_offering"]["storagetype"] = 'shared'

            cls.services['mode'] = cls.zone.networktype
            cls.services["virtual_machine"][
                "hypervisor"] = cls.hypervisor
            cls.services["virtual_machine"]["zoneid"] = cls.zone.id
            cls.services["virtual_machine"]["template"] = cls.template.id
            cls.services["custom_volume"]["zoneid"] = cls.zone.id
            # Creating Disk offering, Service Offering and Account
            cls.disk_offering = DiskOffering.create(
                cls.api_client,
                cls.services["disk_offering"]
            )
            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.services["service_offerings"]["tiny"]
            )
            cls._cleanup.append(cls.disk_offering)
            cls._cleanup.append(cls.service_offering)
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setup : %s" % e)
        return

    def setUp(self):

        if not self.find_storage_pool:
            self.skipTest("Skipping tests since RBD storage type is required for data volumes for LXC")
        self.apiClient = self.testClient.getApiClient()
        self.account = Account.create(
            self.apiClient,
            self.services["account"],
            domainid=self.domain.id
        )
        # Getting authentication for user in newly created Account
        self.user = self.account.user[0]
        self.userapiclient = self.testClient.getUserApiClient(
            self.user.username,
            self.domain.name)
        # Creating Virtual Machine
        self.virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
        )
        self.cleanup = [self.account, ]

    def tearDown(self):
        # Clean up, terminate the created volumes
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def __verify_values(self, expected_vals, actual_vals):
        """
        @summary: Function to verify expected and actual values
        Step1: Initializing return flag to True
        Step1: Verifying length of expected and actual dictionaries is
               matching
               If not matching returning false
        Step2: Listing all the keys from expected dictionary
        Step3: Looping through each key from step2 and verifying expected
               and actual dictionaries have same value
               If not making return flag to False
        Step4: returning the return flag after all the values are verified
        """
        return_flag = True

        if len(expected_vals) != len(actual_vals):
            return False

        keys = list(expected_vals.keys())
        for i in range(0, len(expected_vals)):
            exp_val = expected_vals[keys[i]]
            act_val = actual_vals[keys[i]]
            if exp_val == act_val:
                return_flag = return_flag and True
            else:
                return_flag = return_flag and False
                self.debug(
                    "expected Value: %s, is not matching with actual value:\
                    %s" %
                    (exp_val, act_val))
        return return_flag

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_01_list_volumes_pagination(self):
        """
        @summary: Test List Volumes pagination

        Step1: Listing all the volumes for a user
        Step2: Verifying listed volumes for account created at class level
        Step3: If number of volumes is less than (page size + 1),
               then creating them
        Step4: Listing all the volumes again after creation of volumes
        Step5: Verifying the length of the volumes is (page size + 1)
        Step6: Listing all the volumes in page1
        Step7: Verifying that the length of the volumes in page 1 is
               (page size)
        Step8: Listing all the volumes in page2
        Step9: Verifying that the length of the volumes in page 2 is 1
        Step10: Deleting the volume present in page 2
        Step11: Listing for the volumes on page 2
        Step12: Verifying that there are no volumes present in page 2
        """
        # Listing all the volumes for a user
        list_volumes_before = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])

        # Verifying listed volumes for account created at class level
        self.assertIsNotNone(
            list_volumes_before,
            "create volume from VM failed at class setup method"
        )
        self.assertEqual(
            len(list_volumes_before),
            1,
            "more than 1 volume created from VM at class level"
        )

        # If number of volumes is less than (pagesize + 1), then creating them
        for i in range(0, (self.services["pagesize"])):
            volume_created = Volume.create(
                self.userapiclient,
                self.services["volume"],
                zoneid=self.zone.id,
                diskofferingid=self.disk_offering.id
            )
            self.assertIsNotNone(
                volume_created,
                "Volume is not created"
            )

        # Listing all the volumes again after creation of volumes
        list_volumes_after = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])

        # Verifying the length of the volumes is (page size + 1)
        self.assertEqual(
            len(list_volumes_after),
            (self.services["pagesize"] + 1),
            "Number of volumes created is not matching expected"
        )

        # Listing all the volumes in page1
        list_volumes_page1 = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"]
        )
        self.assertIsNotNone(
            list_volumes_page1,
            "No volumes found in Page 1"
        )
        # Verifying that the length of the volumes in page 1 is (page size)
        self.assertEqual(
            len(list_volumes_page1),
            self.services["pagesize"],
            "List Volume response is not matching with\
            the page size length for page 1"
        )

        # Listing all the volumes in page2
        list_volumes_page2 = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=2,
            pagesize=self.services["pagesize"]
        )
        self.assertIsNotNone(
            list_volumes_page2,
            "No volumes found in Page 2"
        )
        # Verifying that the length of the volumes in page 2 is 1
        self.assertEqual(
            len(list_volumes_page2),
            1,
            "List Volume response is not matching with\
            the page size length for page 2"
        )
        volume_page2 = list_volumes_page2[0]

        # Verifying that the volume on page 2 is not present in page1
        for i in range(0, len(list_volumes_page1)):
            volume_page1 = list_volumes_page1[i]
            self.assertNotEqual(
                volume_page2.id,
                volume_page1.id,
                "Volume listed in page 2 is also listed in page 1"
            )

        # Deleting a single volume
        Volume.delete(volume_created, self.userapiclient)

        # Listing the volumes in page 2
        list_volume_response = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=2,
            pagesize=self.services["pagesize"]
        )
        # verifying that volume does not exists on page 2
        self.assertEqual(
            list_volume_response,
            None,
            "Volume was not deleted"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_02_list_volume_byid(self):
        """
        @summary: Test List Volumes with Id

        Step1: Listing all the volumes for a user before creating a data volume
        Step2: Verifying the length of the list as 1
        Step3: Creating a data volume
        Step4: Listing all the volumes for a user after creating a data volume
        Step5: Verifying the list volume size is increased by 1
        Step6: List the volumes by specifying root volume Id
        Step7: Verifying the details of the root volume
        Step8: List the volumes by specifying data volume Id
        Step9: Verifying the details of the data volume
        """
        # Listing all the volumes for a user before creating a data volume
        list_volumes_before = Volume.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        self.assertIsNotNone(
            list_volumes_before,
            "create volume from VM failed at class setup method")
        # Verifying the length of the list as 1
        self.assertEqual(
            len(list_volumes_before),
            1,
            "more than 1 volume created at class level"
        )
        root_volume = list_volumes_before[0]

        # Creating a data volume
        volume_created = Volume.create(
            self.userapiclient,
            self.services["volume"],
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering.id
        )
        self.assertIsNotNone(
            volume_created,
            "Volume is not created"
        )

        # Listing all the volumes for a user after creating a data volume
        list_volumes_after = Volume.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        self.assertIsNotNone(
            list_volumes_after,
            "Volume creation failed"
        )
        # Verifying the list volume size is increased by 1
        self.assertEqual(
            len(list_volumes_before) + 1,
            len(list_volumes_after),
            "list volume is not matching with Number of volumes created"
        )

        # Listing a Root Volume by Id and verifying the volume details
        list_volumes_by_id = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=root_volume.id
        )
        self.assertIsNotNone(
            list_volumes_by_id,
            "Root volume is not listed"
        )
        self.assertEqual(
            1,
            len(list_volumes_by_id),
            "list volume is not matching with Number of volumes created"
        )
        obtained_volume = list_volumes_by_id[0]

        # Creating expected and actual values dictionaries
        expected_dict = {
            "id": root_volume.id,
            "name": root_volume.name,
            "vmname": self.virtual_machine.name,
            "state": "Ready",
            "type": "ROOT",
            "zoneid": self.zone.id,
            "account": self.account.name,
            "storagetype": self.storagetype,
            "size": self.template.size
        }
        actual_dict = {
            "id": obtained_volume.id,
            "name": obtained_volume.name,
            "vmname": obtained_volume.vmname,
            "state": obtained_volume.state,
            "type": obtained_volume.type,
            "zoneid": obtained_volume.zoneid,
            "account": obtained_volume.account,
            "storagetype": obtained_volume.storagetype,
            "size": obtained_volume.size,
        }
        root_volume_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            root_volume_status,
            "Listed Root Volume details are not as expected"
        )
        # Listing a Data Volume by Id and verifying the volume details
        list_volumes_by_id = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=volume_created.id
        )
        self.assertIsNotNone(
            list_volumes_by_id,
            "Data volume is not listed"
        )
        self.assertEqual(
            len(list_volumes_by_id),
            1,
            "list volume is not matching with Number of volumes created"
        )
        obtained_volume = list_volumes_by_id[0]

        # Creating expected and actual values dictionaries
        expected_dict = {
            "id": volume_created.id,
            "name": volume_created.name,
            "state": "Allocated",
            "type": "DATADISK",
            "zoneid": self.zone.id,
            "account": self.account.name,
            "storagetype": self.storagetype,
            "size": self.disk_offering.disksize
        }
        actual_dict = {
            "id": obtained_volume.id,
            "name": obtained_volume.name,
            "state": obtained_volume.state,
            "type": obtained_volume.type,
            "zoneid": obtained_volume.zoneid,
            "account": obtained_volume.account,
            "storagetype": obtained_volume.storagetype,
            "size": obtained_volume.size / (1024 * 1024 * 1024),
        }
        root_volume_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            root_volume_status,
            "Listed Data Volume details are not as expected"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_03_data_volume_resize(self):
        """
        @summary: Test to verify creation and resize of data volume

        Step1: Listing the volumes for a user before creating data volume
        Step2: Creating a data volume
        Step3: Listing the volumes for a user after creating data volume
        Step4: Attaching and Detaching data volume created to Virtual Machine
        Step5: Verifying if there exists a disk offering with higher size
                If not present creating it
        Step6: Resizing data volume
        """
        if self.hypervisor.lower() in ['hyperv']:
            self.skipTest(
                "This featureis not supported on existing\
                        hypervisor. Hence, skipping the test")
        # Listing volumes for a user before creating a volume
        list_volumes_before = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])

        # Creating a data volume
        volume_created = Volume.create(
            self.userapiclient,
            self.services["volume"],
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering.id
        )
        self.assertIsNotNone(volume_created, "Data volume creation failed")

        # Listing volumes for a user after creating data volume
        list_volumes_after = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])
        self.assertEqual(
            len(list_volumes_before) + 1,
            len(list_volumes_after),
            "Data volume creation failed"
        )

        # Attaching data volume created to Virtual Machine
        self.virtual_machine.attach_volume(
            self.userapiclient,
            volume_created
        )

        list_volumes = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=volume_created.id
        )
        attached_volume = list_volumes[0]

        self.assertIsNotNone(
            attached_volume.vmname,
            "VM is not attached to Volume"
        )
        self.assertEqual(
            self.virtual_machine.name,
            attached_volume.vmname,
            "VM Name is not matching with attached vm"
        )

        # Detaching data volume from Virtual Machine
        self.virtual_machine.detach_volume(
            self.userapiclient,
            volume_created
        )
        list_volumes = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=volume_created.id
        )
        detached_volume = list_volumes[0]
        self.assertIsNone(
            detached_volume.vmname,
            "VM is not detached from volume"
        )

        # Verifying if there exists a disk offering with higher size. If not
        # present creating it
        list_disk_offerings = DiskOffering.list(self.apiClient)

        large_disk_offering_exists = False
        # Converting disk_size in bytes to GB
        current_disk_size = volume_created.size / (1024 * 1024 * 1024)

        for disk_offering in list_disk_offerings:
            if ((disk_offering.disksize > current_disk_size) and (
                    not disk_offering.iscustomized) and
                    disk_offering.storagetype == self.storagetype):
                new_disk_offering = disk_offering
                large_disk_offering_exists = True
                break

        if not large_disk_offering_exists:
            new_size = (volume_created.size / (1024 * 1024 * 1024)) + 1
            self.services["disk_offering"]["disksize"] = new_size
            new_disk_offering = DiskOffering.create(
                self.apiClient,
                self.services["disk_offering"]
            )
            if new_disk_offering is not None:
                self.cleanup.append(new_disk_offering)
        else:
            new_size = new_disk_offering.disksize

        # Resizing data volume
        resized_volume = volume_created.resize(
            self.userapiclient,
            diskofferingid=new_disk_offering.id,
            shrinkok='false',
        )
        self.assertIsNotNone(resized_volume, "Resize Volume failed")
        # Verifying data volume size is increased
        self.assertEqual(
            new_size,
            (resized_volume.size / (1024 * 1024 * 1024)),
            "volume not resized to expected value"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_04_custom_volume_resize(self):
        """
        @summary: Test to verify creation and resize of custom volume

        Step1: Checking if Custom disk offering already exists.
               If not present then creating custom Disk Offering
        Step2: Listing the volumes for a user before creating custom volume
        Step3: Creating a custom volume
        Step4: Listing the volumes for a user after creating custom volume
        Step5: Attaching and Detaching custom volume created to Virtual Machine
        Step6: Resizing custom volume
        """
        if self.hypervisor.lower() in ['hyperv']:
            self.skipTest(
                "This featureis not supported on existing\
                        hypervisor. Hence, skipping the test")
        # Listing all the disk offerings
        list_disk_offerings = DiskOffering.list(self.apiClient)

        custom_disk_offering_exists = False

        # Verifying if a custom disk offering already exists
        if list_disk_offerings is not None:
            for disk_offering in list_disk_offerings:
                if (disk_offering.iscustomized and disk_offering.storagetype ==
                        self.storagetype):
                    custom_disk_offering = disk_offering
                    custom_disk_offering_exists = True
                    break

        # If a custom disk offering does not exists, then creating a custom
        # disk offering
        if not custom_disk_offering_exists:
            custom_disk_offering = DiskOffering.create(
                self.apiClient,
                self.services["disk_offering"],
                custom=True
            )
            if custom_disk_offering is not None:
                self.cleanup.append(custom_disk_offering)

        # Listing the volumes for a user before creating custom volume
        list_volumes_before = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])

        # Creating a custom volume
        volume_created = Volume.create_custom_disk(
            self.userapiclient,
            self.services["custom_volume"],
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=custom_disk_offering.id
        )
        self.assertIsNotNone(
            volume_created,
            "Custom volume did not get created"
        )

        # Listing the volumes for a user after creating custom volume
        list_volumes_after = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])

        # Verifyign that volume list is increased by 1 after creation of
        # custion volume
        self.assertEqual(
            len(list_volumes_before) + 1,
            len(list_volumes_after),
            "Custom volume did not get created"
        )

        # Attaching custom volume created to Virtual Machine
        self.virtual_machine.attach_volume(
            self.userapiclient,
            volume_created
        )
        list_volumes = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=volume_created.id
        )
        attached_volume = list_volumes[0]

        self.assertIsNotNone(
            attached_volume.vmname,
            "VM is not attached to Volume"
        )
        self.assertEqual(
            self.virtual_machine.name,
            attached_volume.vmname,
            "VM Name is not matching with attached vm"
        )

        # Detaching custom volume from Virtual Machine
        self.virtual_machine.detach_volume(
            self.userapiclient,
            volume_created
        )
        list_volumes = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=volume_created.id
        )
        detached_volume = list_volumes[0]
        self.assertIsNone(
            detached_volume.vmname,
            "VM is not detached from volume"
        )

        # Resizing custom volume
        # Increasing custom disk size by 1
        new_size = self.services["custom_volume"]["customdisksize"] + 1
        resized_volume = volume_created.resize(
            self.userapiclient,
            diskofferingid=custom_disk_offering.id,
            shrinkok='false',
            size=new_size)
        self.assertIsNotNone(resized_volume, "Resize Volume failed")
        # Verifying that custom disk size is increased
        self.assertEqual(
            new_size,
            (resized_volume.size / (1024 * 1024 * 1024)),
            "volume not resized to expected value"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_05_volume_snapshot(self):
        """
        @summary: Test to verify creation of snapshot from volume
        and creation of template, volume from snapshot

        Step1: Creating a volume
        Step2: Attaching and Detaching custom volume created to Virtual Machine
        Step3: Creating Snapshot from volume
        Step4: Creating Volume from snapshot
        Step5: Creating Template from Snapshot
        """
        if self.hypervisor.lower() in ["hyperv", "lxc"]:
            self.skipTest(
                "This featureis not supported on existing\
                        hypervisor. Hence, skipping the test")
        list_volumes_before = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])

        volume_created = Volume.create(
            self.userapiclient,
            self.services["volume"],
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering.id
        )

        self.assertIsNotNone(volume_created, "Volume not created")

        list_volumes_after = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])

        self.assertEqual(
            len(list_volumes_before) + 1,
            len(list_volumes_after),
            "Volume not created"
        )
        # Attaching and Detaching custom volume created to Virtual Machine
        self.virtual_machine.attach_volume(
            self.userapiclient,
            volume_created
        )

        list_volumes = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=volume_created.id
        )
        attached_volume = list_volumes[0]

        self.assertIsNotNone(
            attached_volume.vmname,
            "VM is not attached to Volume"
        )
        self.assertEqual(
            self.virtual_machine.name,
            attached_volume.vmname,
            "VM Name is not matching with attached vm"
        )
        self.virtual_machine.detach_volume(
            self.userapiclient,
            volume_created
        )
        list_volumes = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=volume_created.id
        )
        detached_volume = list_volumes[0]
        self.assertIsNone(
            detached_volume.vmname,
            "VM is not detached from volume"
        )
        # Creating Snapshot from volume
        snapshot_created = Snapshot.create(
            self.userapiclient,
            volume_created.id,
        )

        self.assertIsNotNone(snapshot_created, "Snapshot not created")

        # Creating expected and actual values dictionaries
        expected_dict = {
            "id": volume_created.id,
            "intervaltype": "MANUAL",
            "snapshottype": "MANUAL",
            "volumetype": volume_created.type,
            "domain": self.domain.id
        }
        actual_dict = {
            "id": snapshot_created.volumeid,
            "intervaltype": snapshot_created.intervaltype,
            "snapshottype": snapshot_created.snapshottype,
            "volumetype": snapshot_created.volumetype,
            "domain": snapshot_created.domainid,
        }
        status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            status,
            "Snapshot created from Volume details are not as expected"
        )
        # Creating Volume from snapshot
        cmd = createVolume.createVolumeCmd()
        cmd.name = "-".join([self.services["volume"]
                             ["diskname"], random_gen()])
        cmd.snapshotid = snapshot_created.id

        volume_from_snapshot = Volume(
            self.userapiclient.createVolume(cmd).__dict__)

        self.assertIsNotNone(
            volume_from_snapshot,
            "Volume creation failed from snapshot"
        )

        # Creating expected and actual values dictionaries
        expected_dict = {
            "snapshotid": snapshot_created.id,
            "volumetype": snapshot_created.volumetype,
            "size": self.disk_offering.disksize,
            "accounr": self.account.name,
            "domain": self.domain.id,
            "storagetype": self.storagetype,
            "zone": self.zone.id
        }
        actual_dict = {
            "snapshotid": volume_from_snapshot.snapshotid,
            "volumetype": volume_from_snapshot.type,
            "size": volume_from_snapshot.size / (1024 * 1024 * 1024),
            "accounr": volume_from_snapshot.account,
            "domain": volume_from_snapshot.domainid,
            "storagetype": volume_from_snapshot.storagetype,
            "zone": volume_from_snapshot.zoneid,
        }
        status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            status,
            "Volume created from Snapshot details are not as expected"
        )
        # Creating Template from Snapshot
        list_templates_before = Template.list(
            self.userapiclient,
            templatefilter='self')

        if list_templates_before is None:
            templates_before_size = 0
        else:
            templates_before_size = len(list_templates_before)

        cmd = createTemplate.createTemplateCmd()
        cmd.name = self.services["ostype"]
        cmd.displaytext = self.services["ostype"]
        cmd.ostypeid = self.template.ostypeid
        cmd.snapshotid = snapshot_created.id
        cmd.ispublic = False
        cmd.passwordenabled = False

        template_from_snapshot = Template(
            self.userapiclient.createTemplate(cmd).__dict__)

        self.assertIsNotNone(
            template_from_snapshot,
            "Template creation failed from snapshot"
        )

        self.cleanup.append(template_from_snapshot)

        # Creating expected and actual values dictionaries
        expected_dict = {
            "name": self.services["ostype"],
            "ostypeid": self.template.ostypeid,
            "type": "USER",
            "zone": self.zone.id,
            "domain": self.domain.id,
            "account": self.account.name,
            "passwordenabled": False,
            "ispublic": False,
            "size": self.disk_offering.disksize
        }
        actual_dict = {
            "name": template_from_snapshot.name,
            "ostypeid": template_from_snapshot.ostypeid,
            "type": template_from_snapshot.templatetype,
            "zone": template_from_snapshot.zoneid,
            "domain": template_from_snapshot.domainid,
            "account": template_from_snapshot.account,
            "passwordenabled": template_from_snapshot.passwordenabled,
            "ispublic": template_from_snapshot.ispublic,
            "size": template_from_snapshot.size / (1024 * 1024 * 1024)
        }
        status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            status,
            "Template created from Snapshot details are not as expected"
        )

        list_templates_after = Template.list(
            self.userapiclient,
            templatefilter='self')

        self.assertEqual(
            templates_before_size + 1,
            len(list_templates_after),
            "Template creation failed from snapshot"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_06_volume_snapshot_policy_hourly(self):
        """
        @summary: Test to verify creation of Hourly Snapshot policies
        from volume

        Step1: Creating a Volume.
        Step2: Attaching volume created in Step2 to virtual machine
        Step3: Detaching the volume created in step2 from virtual machine
        Step4: Listing snapshot policies for a volume created in step1
        Step5: Creating Hourly snapshot policy
        Step6: Listing snapshot policies for a volume created in step1 again
        Step7: Verifyign that the list snapshot policy length is increased by 1
        """

        if self.hypervisor.lower() in ["hyperv", "lxc"]:
            self.skipTest(
                "This featureis not supported on existing\
                        hypervisor. Hence, skipping the test")

        list_volumes_before = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])

        volume_created = Volume.create(
            self.userapiclient,
            self.services["volume"],
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering.id
        )

        self.assertIsNotNone(volume_created, "Volume not created")

        list_volumes_after = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])

        self.assertEqual(
            len(list_volumes_before) + 1,
            len(list_volumes_after),
            "Volume not created"
        )

        # Attaching volume created to Virtual Machine
        self.virtual_machine.attach_volume(
            self.userapiclient,
            volume_created
        )

        list_volumes = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=volume_created.id
        )
        attached_volume = list_volumes[0]

        self.assertIsNotNone(
            attached_volume.vmname,
            "VM is not attached to Volume"
        )
        self.assertEqual(
            self.virtual_machine.name,
            attached_volume.vmname,
            "VM Name is not matching with attached vm"
        )
        # Detaching volume created from Virtual Machine
        self.virtual_machine.detach_volume(
            self.userapiclient,
            volume_created
        )
        list_volumes = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=volume_created.id
        )
        detached_volume = list_volumes[0]
        self.assertIsNone(
            detached_volume.vmname,
            "VM is not detached from volume"
        )
        # Creating Hourly Snapshot Policy from volume
        self.services["recurring_snapshot"]["intervaltype"] = 'hourly'
        self.services["recurring_snapshot"]["schedule"] = '1'

        list_snapshot_policy_before = SnapshotPolicy.list(
            self.userapiclient,
            volumeid=volume_created.id)

        snapshot_policy_before_size = 0

        if list_snapshot_policy_before is not None:
            snapshot_policy_before_size = len(list_snapshot_policy_before)

        snapshot_policy_hourly = SnapshotPolicy.create(
            self.userapiclient,
            volume_created.id,
            self.services["recurring_snapshot"]
        )
        self.assertIsNotNone(
            snapshot_policy_hourly,
            "Hourly Snapshot policy creation failed"
        )
        # Creating expected and actual values dictionaries
        expected_dict = {
            "schedule": self.services["recurring_snapshot"]["schedule"],
            "intervaltype": 0,
            "volumeid": volume_created.id
        }
        actual_dict = {
            "schedule": snapshot_policy_hourly.schedule,
            "intervaltype": snapshot_policy_hourly.intervaltype,
            "volumeid": snapshot_policy_hourly.volumeid
        }
        status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            status,
            "Hourly Snapshot Policy details are not as expected"
        )

        list_snapshot_policy_after = SnapshotPolicy.list(
            self.userapiclient,
            volumeid=volume_created.id)

        self.assertIsNotNone(
            list_snapshot_policy_after,
            "Hourly Snapshot policy creation failed"
        )
        self.assertEqual(
            snapshot_policy_before_size + 1,
            len(list_snapshot_policy_after),
            "Hourly Snapshot policy creation failed"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_07_volume_snapshot_policy_daily(self):
        """
        @summary: Test to verify creation of Daily Snapshot policies
        from volume

        Step1: Creating a Volume.
        Step2: Attaching volume created in Step2 to virtual machine
        Step3: Detaching the volume created in step2 from virtual machine
        Step4: Listing snapshot policies for a volume created in step1
        Step5: Creating Daily snapshot policy
        Step6: Listing snapshot policies for a volume created in step1 again
        Step7: Verifyign that the list snapshot policy length is increased by 1
        """

        if self.hypervisor.lower() in ["hyperv", "lxc"]:
            self.skipTest(
                "This featureis not supported on existing\
                        hypervisor. Hence, skipping the test")

        list_volumes_before = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])

        volume_created = Volume.create(
            self.userapiclient,
            self.services["volume"],
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering.id
        )

        self.assertIsNotNone(volume_created, "Volume not created")

        list_volumes_after = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])

        self.assertEqual(
            len(list_volumes_before) + 1,
            len(list_volumes_after),
            "Volume not created"
        )
        # Attaching volume created to Virtual Machine
        self.virtual_machine.attach_volume(
            self.userapiclient,
            volume_created
        )

        list_volumes = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=volume_created.id
        )
        attached_volume = list_volumes[0]

        self.assertIsNotNone(
            attached_volume.vmname,
            "VM is not attached to Volume"
        )
        self.assertEqual(
            self.virtual_machine.name,
            attached_volume.vmname,
            "VM Name is not matching with attached vm"
        )
        # Detaching volume created from Virtual Machine
        self.virtual_machine.detach_volume(
            self.userapiclient,
            volume_created
        )
        list_volumes = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=volume_created.id
        )
        detached_volume = list_volumes[0]
        self.assertIsNone(
            detached_volume.vmname,
            "VM is not detached from volume"
        )
        # Creating Daily Snapshot Policy from volume
        self.services["recurring_snapshot"]["intervaltype"] = 'daily'
        self.services["recurring_snapshot"]["schedule"] = '00:00'

        list_snapshot_policy_before = SnapshotPolicy.list(
            self.userapiclient,
            volumeid=volume_created.id)

        snapshot_policy_before_size = 0

        if list_snapshot_policy_before is not None:
            snapshot_policy_before_size = len(list_snapshot_policy_before)

        snapshot_policy_daily = SnapshotPolicy.create(
            self.userapiclient,
            volume_created.id,
            self.services["recurring_snapshot"]
        )
        self.assertIsNotNone(
            snapshot_policy_daily,
            "Daily Snapshot policy creation failed"
        )
        # Creating expected and actual values dictionaries
        expected_dict = {
            "schedule": self.services["recurring_snapshot"]["schedule"],
            "intervaltype": 1,
            "volumeid": volume_created.id
        }
        actual_dict = {
            "schedule": snapshot_policy_daily.schedule,
            "intervaltype": snapshot_policy_daily.intervaltype,
            "volumeid": snapshot_policy_daily.volumeid
        }
        status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            status,
            "Daily Snapshot Policy details are not as expected"
        )

        list_snapshot_policy_after = SnapshotPolicy.list(
            self.userapiclient,
            volumeid=volume_created.id)

        self.assertIsNotNone(
            list_snapshot_policy_after,
            "Daily Snapshot policy creation failed"
        )
        self.assertEqual(
            snapshot_policy_before_size + 1,
            len(list_snapshot_policy_after),
            "Daily Snapshot policy creation failed"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_08_volume_snapshot_policy_weekly(self):
        """
        @summary: Test to verify creation of Weekly Snapshot policies
        from volume

        Step1: Creating a Volume.
        Step2: Attaching volume created in Step2 to virtual machine
        Step3: Detaching the volume created in step2 from virtual machine
        Step4: Listing snapshot policies for a volume created in step1
        Step5: Creating Weekly snapshot policy
        Step6: Listing snapshot policies for a volume created in step1 again
        Step7: Verifyign that the list snapshot policy length is increased by 1
        """

        if self.hypervisor.lower() in ["hyperv", "lxc"]:
            self.skipTest(
                "This featureis not supported on existing\
                        hypervisor. Hence, skipping the test")

        list_volumes_before = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])

        volume_created = Volume.create(
            self.userapiclient,
            self.services["volume"],
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering.id
        )

        self.assertIsNotNone(volume_created, "Volume not created")

        list_volumes_after = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])

        self.assertEqual(
            len(list_volumes_before) + 1,
            len(list_volumes_after),
            "Volume not created"
        )

        # Attaching volume created to Virtual Machine
        self.virtual_machine.attach_volume(
            self.userapiclient,
            volume_created
        )
        list_volumes = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=volume_created.id
        )
        attached_volume = list_volumes[0]

        self.assertIsNotNone(
            attached_volume.vmname,
            "VM is not attached to Volume"
        )
        self.assertEqual(
            self.virtual_machine.name,
            attached_volume.vmname,
            "VM Name is not matching with attached vm"
        )
        # Detaching volume created to Virtual Machine
        self.virtual_machine.detach_volume(
            self.userapiclient,
            volume_created
        )
        list_volumes = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=volume_created.id
        )
        detached_volume = list_volumes[0]
        self.assertIsNone(
            detached_volume.vmname,
            "VM is not detached from volume"
        )
        # Creating Weekly Snapshot Policy from volume
        self.services["recurring_snapshot"]["intervaltype"] = 'weekly'
        self.services["recurring_snapshot"]["schedule"] = '00:00:1'

        list_snapshot_policy_before = SnapshotPolicy.list(
            self.userapiclient,
            volumeid=volume_created.id)

        snapshot_policy_before_size = 0

        if list_snapshot_policy_before is not None:
            snapshot_policy_before_size = len(list_snapshot_policy_before)

        snapshot_policy_weekly = SnapshotPolicy.create(
            self.userapiclient,
            volume_created.id,
            self.services["recurring_snapshot"]
        )
        self.assertIsNotNone(
            snapshot_policy_weekly,
            "Weekly Snapshot policy creation failed"
        )
        # Creating expected and actual values dictionaries
        expected_dict = {
            "schedule": self.services["recurring_snapshot"]["schedule"],
            "intervaltype": 2,
            "volumeid": volume_created.id
        }
        actual_dict = {
            "schedule": snapshot_policy_weekly.schedule,
            "intervaltype": snapshot_policy_weekly.intervaltype,
            "volumeid": snapshot_policy_weekly.volumeid
        }
        status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            status,
            "Weekly Snapshot Policy details are not as expected"
        )

        list_snapshot_policy_after = SnapshotPolicy.list(
            self.userapiclient,
            volumeid=volume_created.id)

        self.assertIsNotNone(
            list_snapshot_policy_after,
            "Weekly Snapshot policy creation failed"
        )
        self.assertEqual(
            snapshot_policy_before_size + 1,
            len(list_snapshot_policy_after),
            "Weekly Snapshot policy creation failed"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_09_volume_snapshot_policy_monthly(self):
        """
        @summary: Test to verify creation of Monthly Snapshot policies
        from volume

        Step1: Creating a Volume.
        Step2: Attaching volume created in Step2 to virtual machine
        Step3: Detaching the volume created in step2 from virtual machine
        Step4: Listing snapshot policies for a volume created in step1
        Step5: Creating Monthly snapshot policy
        Step6: Listing snapshot policies for a volume created in step1 again
        Step7: Verifyign that the list snapshot policy length is increased by 1
        Step8: Deleting monthly snapshot policy created in step5
        Step9: List snapshot policies for a volume again
        Step10:Verifying that the list snapshot policy length is decreased
               by 1
        """
        if self.hypervisor.lower() in ["hyperv", "lxc"]:
            self.skipTest(
                "This featureis not supported on existing\
                        hypervisor. Hence, skipping the test")

        list_volumes_before = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])

        volume_created = Volume.create(
            self.userapiclient,
            self.services["volume"],
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering.id
        )
        self.assertIsNotNone(volume_created, "Volume not created")

        list_volumes_after = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])

        self.assertEqual(
            len(list_volumes_before) + 1,
            len(list_volumes_after),
            "Volume not created"
        )
        # Attaching and Detaching custom volume created to Virtual Machine
        self.virtual_machine.attach_volume(
            self.userapiclient,
            volume_created
        )
        list_volumes = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=volume_created.id
        )
        attached_volume = list_volumes[0]

        self.assertIsNotNone(
            attached_volume.vmname,
            "VM is not attached to Volume"
        )
        self.assertEqual(
            self.virtual_machine.name,
            attached_volume.vmname,
            "VM Name is not matching with attached vm"
        )
        self.virtual_machine.detach_volume(
            self.userapiclient,
            volume_created
        )
        list_volumes = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=volume_created.id
        )
        detached_volume = list_volumes[0]
        self.assertIsNone(
            detached_volume.vmname,
            "VM is not detached from volume"
        )
        # Creating Monthly Snapshot Policy from volume
        self.services["recurring_snapshot"]["intervaltype"] = 'monthly'
        self.services["recurring_snapshot"]["schedule"] = '00:00:1'

        list_snapshot_policy_before = SnapshotPolicy.list(
            self.userapiclient,
            volumeid=volume_created.id)

        snapshot_policy_before_size = 0

        if list_snapshot_policy_before is not None:
            snapshot_policy_before_size = len(list_snapshot_policy_before)

        snapshot_policy_monthly = SnapshotPolicy.create(
            self.userapiclient,
            volume_created.id,
            self.services["recurring_snapshot"])
        self.assertIsNotNone(
            snapshot_policy_monthly,
            "Monthly Snapshot policy creation failed"
        )
        # Creating expected and actual values dictionaries
        expected_dict = {
            "schedule": self.services["recurring_snapshot"]["schedule"],
            "intervaltype": 3,
            "volumeid": volume_created.id
        }
        actual_dict = {
            "schedule": snapshot_policy_monthly.schedule,
            "intervaltype": snapshot_policy_monthly.intervaltype,
            "volumeid": snapshot_policy_monthly.volumeid
        }
        status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            status,
            "Monthly Snapshot Policy details are not as expected"
        )

        list_snapshot_policy_after = SnapshotPolicy.list(
            self.userapiclient,
            volumeid=volume_created.id)

        self.assertIsNotNone(
            list_snapshot_policy_after,
            "Monthly Snapshot policy creation failed"
        )
        self.assertEqual(
            snapshot_policy_before_size + 1,
            len(list_snapshot_policy_after),
            "Monthly Snapshot policy creation failed"
        )
        # Deleting monthly snapshot policy
        SnapshotPolicy.delete(snapshot_policy_monthly, self.userapiclient)

        list_snapshot_policies = SnapshotPolicy.list(
            self.userapiclient,
            volumeid=volume_created.id)

        self.assertIsNone(
            list_snapshot_policies,
            "Deletion of Monthly Snapshot policy failed"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_10_volume_snapshots_pagination(self):
        """
        @summary: Test to verify pagination of snapshots for Volume

        Step1: Creating a Volume.
        Step2: Attaching volume created in Step2 to virtual machine
        Step3: Detaching the volume created in step2 from virtual machine
        Step4: Listing all the snapshots for a volume
        Step5: Creating Pagesize + 1 number of snapshots for a volume
        Step6: Listing all the snapshots for a volume
        Step7: Verifying that there are pagesize + 1 number of snapshots
               listed
        Step8: Listing all the snapshots in page 1
        Step9: Listing all the snapshots in page 2
        Step10: Deleting the snapshot present in page 2
        Step11: Listign the snapshots from page 2 again and verifyign that
                list returns none
        """
        if self.hypervisor.lower() in ["hyperv", "lxc"]:
            self.skipTest(
                "This featureis not supported on existing\
                        hypervisor. Hence, skipping the test")

        list_volumes_before = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])

        # Creating a Volume
        volume_created = Volume.create(
            self.userapiclient,
            self.services["volume"],
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering.id
        )
        self.assertIsNotNone(volume_created, "Volume not created")

        list_volumes_after = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])

        self.assertEqual(
            len(list_volumes_before) + 1,
            len(list_volumes_after),
            "Volume not created"
        )
        # Attaching volume to virtual machine
        self.virtual_machine.attach_volume(
            self.userapiclient,
            volume_created
        )
        list_volumes = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=volume_created.id
        )
        attached_volume = list_volumes[0]

        self.assertIsNotNone(
            attached_volume.vmname,
            "VM is not attached to Volume"
        )
        self.assertEqual(
            self.virtual_machine.name,
            attached_volume.vmname,
            "VM Name is not matching with attached vm"
        )
        # Detaching volume from virtual machine
        self.virtual_machine.detach_volume(
            self.userapiclient,
            volume_created
        )
        list_volumes = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=volume_created.id
        )
        detached_volume = list_volumes[0]
        self.assertIsNone(
            detached_volume.vmname,
            "VM is not detached from volume"
        )

        # Creating 3 Snapshots from volume
        list_snapshot_before = Snapshot.list(
            self.userapiclient,
            volumeid=volume_created.id,
            listall=self.services["listall"]
        )
        self.assertIsNone(
            list_snapshot_before,
            "Newly created volume is already having snapshots"
        )

        list_snapshot_before_size = 0
        for i in range(0, 3):
            snapshot_created = Snapshot.create(
                self.userapiclient,
                volume_created.id,
            )
            self.assertIsNotNone(snapshot_created, "Snapshot not created")

            self.assertEqual(
                volume_created.id,
                snapshot_created.volumeid,
                "Snapshot not created for given volume"
            )

        list_snapshot_after = Snapshot.list(
            self.userapiclient,
            volumeid=volume_created.id,
            listall=self.services["listall"]
        )
        self.assertEqual(
            list_snapshot_before_size + 3,
            len(list_snapshot_after),
            "Number of snapshots created is not matching expected"
        )
        # Listing all the snapshots in page1
        list_snapshots_page1 = Snapshot.list(
            self.userapiclient,
            volumeid=volume_created.id,
            listall=self.services["listall"],
            page=1,
            pagesize=2
        )
        self.assertEqual(
            2,
            len(list_snapshots_page1),
            "List snapshots response is not matching with the\
            page size length for page 1"
        )

        # Listing all the snapshots in page2 and ensuring only 1 snapshot is
        # present
        list_snapshots_page2 = Snapshot.list(
            self.userapiclient,
            volumeid=volume_created.id,
            listall=self.services["listall"],
            page=2,
            pagesize=2
        )
        self.assertEqual(
            len(list_snapshots_page2),
            1,
            "List snapshots response is not matching with\
            the page size length for page 2"
        )
        snapshot_page2 = list_snapshots_page2[0]

        # Verifying that the snapshot on page 2 is not present in page1
        for i in range(0, len(list_snapshots_page1)):
            snapshot_page1 = list_snapshots_page1[i]
            self.assertNotEqual(
                snapshot_page2.id,
                snapshot_page1.id,
                "Snapshot listed in page 2 is also listed in page 1"
            )
        # Deleting a single snapshot and verifying that snapshot does not
        # exists on page 2
        Snapshot.delete(snapshot_created, self.userapiclient)

        list_snapshot_page2 = Snapshot.list(
            self.userapiclient,
            volumeid=volume_created.id,
            listall=self.services["listall"],
            page=2,
            pagesize=2
        )
        self.assertEqual(
            None,
            list_snapshot_page2,
            "Snapshot was not deleted"
        )
        list_snapshot_page1 = Snapshot.list(
            self.userapiclient,
            volumeid=volume_created.id,
            listall=self.services["listall"],
            page=1,
            pagesize=2
        )
        self.assertEqual(
            2,
            len(list_snapshot_page1),
            "Snapshots on page 1 are not matching"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_11_volume_extract(self):
        """
        @summary: Test to verify extract/download a Volume

        Step1: Listing Volumes before creating a Volume
        Step2: Creating a Volume.
        Step3: Verifying that created volume is not none and adding to clean up
        Step4: Listing the volumes after creation
        Step5: Verifying that the list volume size is increased by 1
        Step6: Attaching volume created in Step2 to virtual machine
        Step7: Detaching the volume created in step2 from virtual machine
        Step8: Extracting/Downloadign the volume
        Step9: Verifyign that a download URL is created for volume download
        """
        list_volumes_before = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])

        self.assertIsNotNone(
            list_volumes_before,
            "volume not created for the vm launched at class level"
        )
        volume_created = Volume.create(
            self.userapiclient,
            self.services["volume"],
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering.id
        )

        self.assertIsNotNone(volume_created, "Volume not created")

        list_volumes_after = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])

        self.assertIsNotNone(
            list_volumes_after,
            "volume creation failed"
        )
        self.assertEqual(
            len(list_volumes_before) + 1,
            len(list_volumes_after),
            "Volume not created"
        )
        # Attaching and Detaching volume created to Virtual Machine
        self.virtual_machine.attach_volume(
            self.userapiclient,
            volume_created
        )
        list_volumes = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=volume_created.id
        )
        attached_volume = list_volumes[0]

        self.assertIsNotNone(
            attached_volume.vmname,
            "VM is not attached to Volume"
        )
        self.assertEqual(
            self.virtual_machine.name,
            attached_volume.vmname,
            "VM Name is not matching with attached vm"
        )
        self.virtual_machine.detach_volume(
            self.userapiclient,
            volume_created
        )
        list_volumes = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=volume_created.id
        )
        detached_volume = list_volumes[0]
        self.assertIsNone(
            detached_volume.vmname,
            "VM is not detached from volume"
        )
        # Extract/Download the volume
        self.services["mode"] = "HTTP_DOWNLOAD"

        extract_volume_response = Volume.extract(
            self.userapiclient,
            volume_created.id,
            self.zone.id,
            self.services["mode"]
        )
        self.assertIsNotNone(
            extract_volume_response,
            "Extract/Download volume failed")

        self.assertEqual(
            "DOWNLOAD_URL_CREATED",
            extract_volume_response.state,
            "Failed to create Download URL"
        )
        self.assertIsNotNone(
            extract_volume_response.url,
            "Extract/Download volume URL is NULL"
        )
        self.assertTrue(
            (extract_volume_response.url.find("http") != -1),
            "Extract/Download volume URL doesnot contain http"
        )
        self.assertEqual(
            volume_created.id,
            extract_volume_response.id,
            "Extracted/Downloaded volume is not matching with original volume"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_12_volume_upload(self):
        """
        @summary: Test to verify upload volume

        Step1: Listing the volumes for a user before uploading volume
        Step2: Uploading a volume
        Step3: Listing the volumes for a user after uploading data volume
        Step4: Verifying that the list volume length after upload is
               increased by 1
        """
        list_volumes_before = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])

        self.assertIsNotNone(
            list_volumes_before,
            "volume not created for the vm launched at class level"
        )
        # Uploading a Volume
        volume_uploaded = Volume.upload(
            self.userapiclient,
            self.services["configurableData"]["upload_volume"],
            self.zone.id
        )
        self.assertIsNotNone(volume_uploaded, "volume uploading failed")

        # Listing the volumes for a user after uploading data volume
        list_volumes_after = Volume.list(
            self.userapiclient,
            listall=self.services["listall"])

        self.assertIsNotNone(
            list_volumes_after,
            "volume not created for the vm launched at class level"
        )
        # Asserting that the list volume length after upload is increased by 1
        self.assertEqual(
            len(list_volumes_before) + 1,
            len(list_volumes_after),
            "upload volume failed"
        )
        return

    @attr(tags=["advanced", "basic", "sg"], required_hardware="true")
    def test_13_volume_custom_disk_size(self):
        """
        @Desc:Create volume from custom disk offering does not work as expected
        Step1:Create custom disk offering
        Step2:Create Volume with size x
        Step3:Attach that volume to a vm
        Step4:Create another volume with size y
        Step5:Verify that the new volume is created with size Y
              but not with size X
        """
        if self.hypervisor.lower() in ['hyperv']:
            self.skipTest(
                "This featureis not supported on existing\
                        hypervisor. Hence, skipping the test")
        disk_offering = DiskOffering.create(
            self.api_client,
            self.services["disk_offering"],
            custom=True
        )
        self.assertIsNotNone(
            disk_offering,
            "Failed to create custom disk offering")
        self.cleanup.append(disk_offering)
        self.services["custom_volume"]["customdisksize"] = 2
        vol1 = Volume.create_custom_disk(
            self.userapiclient,
            self.services["custom_volume"],
            account=self.account.name,
            domainid=self.domain.id,
            diskofferingid=disk_offering.id
        )
        self.assertIsNotNone(
            vol1,
            "Volume creation failed with custom disk size")
        vol1_res = Volume.list(
            self.userapiclient,
            id=vol1.id
        )
        self.assertEqual(
            validateList(vol1_res)[0],
            PASS,
            "Volume list returned invalid response")
        vol1_size = vol1_res[0].size
        try:
            self.virtual_machine.attach_volume(self.userapiclient, vol1)
        except Exception as e:
            self.fail(
                "Attaching custom data disk to vm failed\
                        with error{}".format(e))
        self.services["custom_volume"]["customdisksize"] = 3
        vol2 = Volume.create_custom_disk(
            self.userapiclient,
            self.services["custom_volume"],
            account=self.account.name,
            domainid=self.domain.id,
            diskofferingid=disk_offering.id
        )
        self.assertIsNotNone(
            vol2,
            "Failed to create custom data disk with size %s" %
            self.services["custom_volume"]["customdisksize"])
        vol2_res = Volume.list(
            self.userapiclient,
            id=vol2.id
        )
        self.assertEqual(
            validateList(vol2_res)[0],
            PASS,
            "Volume list returned invalid response")
        vol2_size = vol2_res[0].size
        self.assertNotEqual(
            vol1_size,
            vol2_size,
            "Creating volume from custom disk offering does not work\
                    as expected"
        )
        try:
            self.virtual_machine.detach_volume(self.userapiclient, vol1)
        except Exception as e:
            self.fail("Detaching volume failed with error %s" % e)
        return
