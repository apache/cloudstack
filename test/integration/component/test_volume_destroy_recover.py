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
""" tests for Volume improvement (Destroy/Recover) in cloudstack 4.14.0.0

"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (deleteVolume, extractVolume, recoverVolume)
from marvin.lib.utils import (validateList,
                              cleanup_resources)
from marvin.lib.base import (Resources,
                             Volume,
                             Account,
                             Domain,
                             Network,
                             NetworkOffering,
                             VirtualMachine,
                             ServiceOffering,
                             DiskOffering,
                             Zone)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               matchResourceCount,
                               isAccountResourceCountEqualToExpectedCount)
from marvin.codes import (PASS, FAILED, RESOURCE_PRIMARY_STORAGE, RESOURCE_VOLUME)
import logging
import random
import time

class TestVolumeDestroyRecover(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestVolumeDestroyRecover,
            cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()
        zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.zone = Zone(zone.__dict__)
        cls._cleanup = []

        cls.logger = logging.getLogger("TestVolumeDestroyRecover")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        # Get Domain and templates
        cls.domain = get_domain(cls.apiclient)

        cls.template = get_template(cls.apiclient, cls.zone.id, hypervisor="KVM")
        if cls.template == FAILED:
            sys.exit(1)
        cls.templatesize = (cls.template.size / (1024 ** 3))

        cls.services['mode'] = cls.zone.networktype
        # Create Account
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account);
        accounts = Account.list(cls.apiclient, id=cls.account.id)
        cls.expectedCount = int(accounts[0].primarystoragetotal)
        cls.volumeTotal = int(accounts[0].volumetotal)

        if cls.zone.securitygroupsenabled:
            cls.services["shared_network_offering"]["specifyVlan"] = 'True'
            cls.services["shared_network_offering"]["specifyIpRanges"] = 'True'

            cls.network_offering = NetworkOffering.create(
                cls.apiclient,
                cls.services["shared_network_offering"]
            )
            cls._cleanup.append(cls.network_offering)
            cls.network_offering.update(cls.apiclient, state='Enabled')

            cls.account_network = Network.create(
                cls.apiclient,
                cls.services["network2"],
                networkofferingid=cls.network_offering.id,
                zoneid=cls.zone.id,
                accountid=cls.account.name,
                domainid=cls.account.domainid
            )
            cls._cleanup.append(cls.account_network)
        else:
            cls.network_offering = NetworkOffering.create(
                cls.apiclient,
                cls.services["isolated_network_offering"],
            )
            cls._cleanup.append(cls.network_offering)
            # Enable Network offering
            cls.network_offering.update(cls.apiclient, state='Enabled')

            # Create account network
            cls.services["network"]["zoneid"] = cls.zone.id
            cls.services["network"]["networkoffering"] = cls.network_offering.id
            cls.account_network = Network.create(
                cls.apiclient,
                cls.services["network"],
                cls.account.name,
                cls.account.domainid
            )
            cls._cleanup.append(cls.account_network)

        # Create small service offering
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )
        cls._cleanup.append(cls.service_offering)

        # Create disk offering
        cls.disk_offering = DiskOffering.create(
            cls.apiclient,
            cls.services["disk_offering"],
        )
        cls._cleanup.append(cls.disk_offering)

    @classmethod
    def tearDownClass(cls):
        super(TestVolumeDestroyRecover, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestVolumeDestroyRecover, self).tearDown()

    def verify_resource_count_primary_storage(self, expectedCount, volumeTotal):
        response = matchResourceCount(
                        self.apiclient, expectedCount,
                        RESOURCE_PRIMARY_STORAGE,
                        accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])

        result = isAccountResourceCountEqualToExpectedCount(
            self.apiclient, self.account.domainid, self.account.name,
            expectedCount, RESOURCE_PRIMARY_STORAGE)
        self.assertFalse(result[0], result[1])
        self.assertTrue(result[2], "Resource count of primary storage does not match")

        response = matchResourceCount(
                        self.apiclient, volumeTotal,
                        RESOURCE_VOLUME,
                        accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])

        result = isAccountResourceCountEqualToExpectedCount(
            self.apiclient, self.account.domainid, self.account.name,
            volumeTotal, RESOURCE_VOLUME)
        self.assertFalse(result[0], result[1])
        self.assertTrue(result[2], "Resource count of volume does not match")

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_01_create_vm_with_data_disk(self):
        """Create VM with DATA disk, then destroy it (expunge=False) and expunge it

            Steps:
            # 1. create vm with root disk and data disk
            # 2. destroy vm, resource count of primary storage is not changed
            # 3. expunge vm, resource count of primary storage decreased with size of root disk.
            # 4. delete volume (data disk), resource count of primary storage decreased with size of data disk
        """

        try:
            virtual_machine_1 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                diskofferingid=self.disk_offering.id,
                templateid=self.template.id,
                zoneid=self.zone.id
            )
            self.cleanup.append(virtual_machine_1)
        except Exception as e:
            self.fail("Exception while deploying virtual machine: %s" % e)

        self.expectedCount = self.expectedCount + self.templatesize + self.disk_offering.disksize
        self.volumeTotal = self.volumeTotal + 2
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal);

        root_volumes_list = Volume.list(
            self.apiclient,
            virtualmachineid=virtual_machine_1.id,
            type='ROOT',
            listall=True
        )
        status = validateList(root_volumes_list)
        self.assertEqual(status[0], PASS, "ROOT Volume List Validation Failed")
        root_volume_id = root_volumes_list[0].id

        data_volumes_list = Volume.list(
            self.apiclient,
            virtualmachineid=virtual_machine_1.id,
            type='DATADISK',
            listall=True
        )
        status = validateList(data_volumes_list)
        self.assertEqual(status[0], PASS, "DATADISK Volume List Validation Failed")
        data_volume_id = data_volumes_list[0].id

        # destroy vm
        virtual_machine_1.delete(self.apiclient, expunge=False)
        self.cleanup.remove(virtual_machine_1)
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal)

        # expunge vm
        virtual_machine_1.expunge(self.apiclient)
        self.expectedCount = self.expectedCount - self.templatesize
        self.volumeTotal = self.volumeTotal - 1
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal)

        # delete datadisk
        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = data_volume_id
        self.apiclient.deleteVolume(cmd)
        self.expectedCount = self.expectedCount - self.disk_offering.disksize
        self.volumeTotal = self.volumeTotal - 1
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal)

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_02_destroy_allocated_volume(self):
        """Create volume, destroy it when expunge=false and expunge=true

            Steps:
            # 1. create volume, resource count increases.
            # 2. destroy volume (expunge = false), Exception happened. resource count no changes
            # 3. destroy volume (expunge = True), resource count of primary storage decreased with size of volume.
        """

        # Create volume
        volume = Volume.create(
            self.apiclient, self.services["volume"],
            zoneid=self.zone.id, account=self.account.name,
            domainid=self.account.domainid, diskofferingid=self.disk_offering.id
        )
        self.cleanup.append(volume)
        self.expectedCount = self.expectedCount + self.disk_offering.disksize
        self.volumeTotal = self.volumeTotal + 1
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal);

        # Destroy volume (expunge=False)
        with self.assertRaises(Exception):
            volume.destroy(self.apiclient)

        # Destroy volume (expunge=True)
        volume.destroy(self.apiclient, expunge=True)
        self.cleanup.remove(volume)

        self.expectedCount = self.expectedCount - self.disk_offering.disksize
        self.volumeTotal = self.volumeTotal - 1
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal);

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_03_destroy_detached_volume(self):
        """Create volume, attach/detach it, then destroy it when expunge=false and expunge=true

            Steps:
            # 1. create vm without data disk, resource count increases.
            # 2. create volume, resource count increases.
            # 3. attach volume to a vm. resource count  no changes.
            # 4. detach volume from a vm. resource count no changes.
            # 5. destroy volume (expunge = false), volume is Destroy.  resource count decreased with size of volume.
            # 6. destroy volume (expunge = true), volume is not found. resource count no changes.
            # 7. destroy vm (expunge=True). resource count decreased with size of root disk
        """
        # Create vm
        try:
            virtual_machine_2 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                zoneid=self.zone.id
            )
            self.cleanup.append(virtual_machine_2)
        except Exception as e:
            self.fail("Exception while deploying virtual machine: %s" % e)

        self.expectedCount = self.expectedCount + self.templatesize
        self.volumeTotal = self.volumeTotal + 1
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal);

        # Create volume
        volume = Volume.create(
            self.apiclient, self.services["volume"],
            zoneid=self.zone.id, account=self.account.name,
            domainid=self.account.domainid, diskofferingid=self.disk_offering.id
        )
        self.cleanup.append(volume)
        self.expectedCount = self.expectedCount + self.disk_offering.disksize
        self.volumeTotal = self.volumeTotal + 1
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal);

        # Attach volume to vm
        virtual_machine_2.attach_volume(self.apiclient, volume)
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal);

        # Detach volume from vm
        virtual_machine_2.stop(self.apiclient)
        virtual_machine_2.detach_volume(self.apiclient, volume)
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal);

        # save id for later recovery and expunge
        volumeUuid = volume.id

        # Destroy volume (expunge=False)
        volume.destroy(self.apiclient, expunge=False)
        self.cleanup.remove(volume)
        self.expectedCount = self.expectedCount - self.disk_offering.disksize
        self.volumeTotal = self.volumeTotal - 1
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal);

        # Destroy volume (expunge=True)
        volume.destroy(self.apiclient, expunge=True)
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal);

        # Destroy VM (expunge=True)
        virtual_machine_2.delete(self.apiclient, expunge=True)
        self.cleanup.remove(virtual_machine_2)
        self.expectedCount = self.expectedCount - self.templatesize
        self.volumeTotal = self.volumeTotal - 1
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal);

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_04_recover_root_volume_after_restorevm(self):
        """Restore VM, recover/delete old root disk

            Steps:
            # 1. create vm without data disk, resource count increases.
            # 2. restore vm. resource count no changes.
            # 3. check old root disk , should be Destroy state
            # 4. recover old root disk. resource count increases.
            # 5. delete old root disk . resource count decreases.
            # 6. destroy vm (expunge=True). resource count decreased with size of root disk
        """

        # Create vm
        try:
            virtual_machine_3 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                zoneid=self.zone.id
            )
            self.cleanup.append(virtual_machine_3)
        except Exception as e:
            self.fail("Exception while deploying virtual machine: %s" % e)

        self.expectedCount = self.expectedCount + self.templatesize
        self.volumeTotal = self.volumeTotal + 1
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal);

        # Get id of root disk
        root_volumes_list = Volume.list(
            self.apiclient,
            virtualmachineid=virtual_machine_3.id,
            type='ROOT',
            listall=True
        )
        status = validateList(root_volumes_list)
        self.assertEqual(status[0], PASS, "ROOT Volume List Validation Failed")
        root_volume_id = root_volumes_list[0].id

        # restore vm
        virtual_machine_3.restore(self.apiclient)
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal);

        # check old root disk state
        root_volumes_list = Volume.list(
            self.apiclient,
            id=root_volume_id,
            listall=True
        )
        status = validateList(root_volumes_list)
        self.assertEqual(status[0], PASS, "ROOT Volume List Validation Failed")
        root_volume = root_volumes_list[0]
        self.assertEqual(root_volume['state'], 'Destroy', "ROOT volume should be Destroy after restorevm")

        # recover old root disk
        cmd = recoverVolume.recoverVolumeCmd()
        cmd.id = root_volume.id
        self.apiclient.recoverVolume(cmd)
        self.expectedCount = self.expectedCount + self.templatesize
        self.volumeTotal = self.volumeTotal + 1
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal);

        # delete old root disk
        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = root_volume.id
        self.apiclient.deleteVolume(cmd)
        self.expectedCount = self.expectedCount - self.templatesize
        self.volumeTotal = self.volumeTotal - 1
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal)

        # Destroy VM (expunge=True)
        virtual_machine_3.delete(self.apiclient, expunge=True)
        self.cleanup.remove(virtual_machine_3)
        self.expectedCount = self.expectedCount - self.templatesize
        self.volumeTotal = self.volumeTotal - 1
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal);

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_05_extract_root_volume_and_destroy_vm(self):
        """Create VM, extract root volume, then destroy vm and volume

            Steps:
            # 1. create vm without data disk, resource count increases.
            # 2. stop vm
            # 3. extract root volume
            # 4. expunge vm, root volume in Expunged state. resource count decreased with size of root disk.
            # 5. destroy volume (expunge = false), Exception happened. resource count no changes
            # 6. destroy volume (expunge = true). volume is not found. resource count no changes.
        """

        # Create vm
        try:
            virtual_machine_4 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                zoneid=self.zone.id
            )
            self.cleanup.append(virtual_machine_4)
        except Exception as e:
            self.fail("Exception while deploying virtual machine: %s" % e)

        self.expectedCount = self.expectedCount + self.templatesize
        self.volumeTotal = self.volumeTotal + 1
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal);

        # Get id of root disk
        root_volumes_list = Volume.list(
            self.apiclient,
            virtualmachineid=virtual_machine_4.id,
            type='ROOT',
            listall=True
        )
        status = validateList(root_volumes_list)
        self.assertEqual(status[0], PASS, "ROOT Volume List Validation Failed")
        root_volume_id = root_volumes_list[0].id

        # Stop vm
        virtual_machine_4.stop(self.apiclient)

        # extract root volume
        cmd = extractVolume.extractVolumeCmd()
        cmd.id = root_volume_id
        cmd.mode = "HTTP_DOWNLOAD"
        cmd.zoneid = self.zone.id
        self.apiclient.extractVolume(cmd)

        # Destroy VM (expunge=True)
        virtual_machine_4.delete(self.apiclient, expunge=True)
        self.cleanup.remove(virtual_machine_4)
        self.expectedCount = self.expectedCount - self.templatesize
        self.volumeTotal = self.volumeTotal - 1
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal);

        # check root disk state
        root_volumes_list = Volume.list(
            self.apiclient,
            id=root_volume_id,
            listall=True
        )
        status = validateList(root_volumes_list)
        self.assertEqual(status[0], PASS, "ROOT Volume List Validation Failed")
        root_volume = root_volumes_list[0]
        self.assertEqual(root_volume['state'], 'Expunged', "ROOT volume should be Destroy after restorevm")

        # delete root disk
        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = root_volume.id
        self.apiclient.deleteVolume(cmd)
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal)

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_06_delete_network(self):
        """Delete account network, resource count should not be changed

            Steps:
            # 1. Delete account network
            # 2. resource count should not be changed
        """
        self.account_network.delete(self.apiclient)
        self._cleanup.remove(self.account_network)
        self.verify_resource_count_primary_storage(self.expectedCount, self.volumeTotal)
