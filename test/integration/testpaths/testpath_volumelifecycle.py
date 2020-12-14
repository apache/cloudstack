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
"""Utilities functions
"""
# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
# Import Integration Libraries
from marvin.codes import FAILED, PASS
# base - contains all resources as entities and defines create, delete,
# list operations on them
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             User,
                             DiskOffering,
                             Volume,
                             Template,
                             StoragePool,
                             Resources)
from marvin.lib.utils import cleanup_resources, validateList

#common - commonly used methods for all tests are listed here
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               list_virtual_machines,
                               find_storage_pool_type)
from nose.plugins.attrib import attr
import os
import urllib.request, urllib.parse, urllib.error
import tempfile


def verify_attach_volume(self, vmid, volid):
    list_volumes = Volume.list(self.userapiclient,
                               id=volid
                               )
    self.assertEqual(
        validateList(list_volumes)[0],
        PASS,
        "Check List volume response for volume %s" %
        volid)
    self.assertEqual(
        len(list_volumes),
        1,
        "There is no data disk attached to vm id:%s" %
        vmid)
    self.assertEqual(
        list_volumes[0].virtualmachineid,
        vmid,
        "Check if volume state (attached) is reflected")
    self.debug("volume id:%s successfully attached to vm id%s" % (volid, vmid))
    return


def verify_detach_volume(self, vmid, volid):
    list_volumes = Volume.list(self.userapiclient,
                               id=volid
                               )
    self.assertEqual(
        validateList(list_volumes)[0],
        PASS,
        "Check List volume response for volume %s" %
        volid)
    self.assertEqual(
        len(list_volumes),
        1,
        "Detach data disk id: %s  for vm id :%s was not successful" %
        (volid,
         vmid))
    self.assertEqual(
        list_volumes[0].virtualmachineid,
        None,
        "Check if volume state (attached) is reflected")
    self.debug(
        "volume id: %s successfully detached from vm id:%s" %
        (volid, vmid))


def verify_vm(self, vmid):
    list_vm = list_virtual_machines(self.userapiclient,
                                    account=self.account.name,
                                    domainid=self.account.domainid,
                                    id=vmid
                                    )
    self.assertEqual(
        validateList(list_vm)[0],
        PASS,
        "Check List vm response for vmid: %s" %
        vmid)
    self.assertGreater(
        len(list_vm),
        0,
        "Check the list vm response for vm id:  %s" %
        vmid)
    vm = list_vm[0]
    self.assertEqual(
        vm.id,
        str(vmid),
        "Vm deployed is different from the test")
    self.assertEqual(vm.state, "Running", "VM is not in Running state")
    self.debug("VM got created successfully %s" % vmid)


class TestPathVolume(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        testClient = super(TestPathVolume, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        # Get Zone,Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient)
        cls.testdata["mode"] = cls.zone.networktype
        cls.hypervisor = testClient.getHypervisorInfo()
        cls._cleanup = []
        cls.insuffStorage = False
        cls.unsupportedHypervisor = False

        #for LXC if the storage pool of type 'rbd' ex: ceph is not available, skip the test
        if cls.hypervisor.lower() == 'lxc':
            if not find_storage_pool_type(cls.apiclient, storagetype='rbd'):
                cls.insuffStorage   = True
                return

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata["ostype"])
        cls.testdata["template"]["ostypeid"] = cls.template.ostypeid
        if cls.template == FAILED:
            cls.fail(
                "get_template() failed to return template with description \
                %s" %
                cls.testdata["ostype"])

        try:
            cls.account = Account.create(cls.apiclient,
                                         cls.testdata["account"],
                                         domainid=cls.domain.id
                                         )
            cls._cleanup.append(cls.account)
            # createa two service offerings
            cls.service_offering_1 = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offerings"]["small"])
            cls._cleanup.append(cls.service_offering_1)
            # Create Disk offerings
            cls.disk_offering_1 = DiskOffering.create(
                cls.apiclient,
                cls.testdata["disk_offering"])
            cls._cleanup.append(cls.disk_offering_1)
            # check if zone wide storage is enable
            cls.list_storage = StoragePool.list(cls.apiclient,
                                                scope="ZONE"
                                                )
            if cls.list_storage:
                cls.zone_wide_storage = cls.list_storage[0]
                cls.debug(
                    "zone wide storage id is %s" %
                    cls.zone_wide_storage.id)
                cls.testdata["tags"] = "zp"
                update1 = StoragePool.update(cls.apiclient,
                                             id=cls.zone_wide_storage.id,
                                             tags=cls.testdata["tags"]
                                             )
                cls.debug(
                    "Storage %s pool tag%s" %
                    (cls.zone_wide_storage.id, update1.tags))
                cls.testdata["service_offerings"]["tags"] = "zp"
                cls.tagged_so = ServiceOffering.create(
                    cls.apiclient,
                    cls.testdata["service_offerings"])
                cls.testdata["service_offerings"]["tags"] = " "
                cls._cleanup.append(cls.tagged_so)
                # create tagged disk offerings
                cls.testdata["disk_offering"]["tags"] = "zp"
                cls.disk_offering_tagged = DiskOffering.create(
                    cls.apiclient,
                    cls.testdata["disk_offering"])
                cls._cleanup.append(cls.disk_offering_tagged)
            else:
                cls.debug("No zone wide storage found")
            # check if local storage is enable
            if cls.zone.localstorageenabled:
                cls.testdata["disk_offering"]["tags"] = " "
                cls.testdata["service_offerings"]["storagetype"] = 'local'
                cls.service_offering_2 = ServiceOffering.create(
                    cls.apiclient,
                    cls.testdata["service_offerings"])
                cls._cleanup.append(cls.service_offering_2)
                # craete a compute offering with local storage
                cls.testdata["disk_offering"]["storagetype"] = 'local'
                cls.disk_offering_local = DiskOffering.create(
                    cls.apiclient,
                    cls.testdata["disk_offering"])
                cls._cleanup.append(cls.disk_offering_local)
                cls.testdata["disk_offering"]["storagetype"] = ' '
            else:
                cls.debug("No local storage found")
            cls.userapiclient = testClient.getUserApiClient(
                UserName=cls.account.name,
                DomainName=cls.account.domain)
            # Check if login is successful with new account
            response = User.login(cls.userapiclient,
                                  username=cls.account.name,
                                  password=cls.testdata["account"]["password"]
                                  )
            assert response.sessionkey is not None
            #response should have non null value
        except Exception as e:
                cls.tearDownClass()
                raise e
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        if self.unsupportedHypervisor or self.insuffStorage:
            self.skipTest("Skipping test because of insuff resources\
                    %s" % self.hypervisor)

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning:Exception during cleanup: %s" % e)

    @attr(
        tags=[
            "advanced",
            "advancedsg",
            "basic",
        ],
        required_hardware="True")
    def test_01_positive_path(self):
        """
        positive test for volume life cycle
        # 1. Deploy a vm [vm1] with shared storage and data disk
        # 2. Deploy a vm [vm2]with shared storage without data disk
        # 3. TBD
        # 4. Create a new volume and attache to vm2
        # 5. Detach data disk from vm1 and download it
        #  Variance(1-9)
        # 6. Upload volume by providing url of downloaded volume in step 5
        # 7. Attach the volume to a different vm - vm2
        # 8. Try to delete an attached volume
        # 9. Create template from root volume of VM1
        # 10. Create new VM using the template created in step 9
        # 11. Delete the template
        # 12. Detach the disk from VM2 and re-attach the disk to VM1
        # 13.TBD
        # 14.TBD
        # 15.Migrate volume(detached) and then attach to a vm and live-migrate
        # 16.Upload volume of size smaller  than
            storage.max.volume.upload.size(leaving the negative case)
        # 17.TBD
        # 18.TBD
        # 19.TBD
        # 20.Detach data disks from VM2 and delete volume

        """
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest(
                "feature is not supported in %s" %
                self.hypervisor)
        # 1. Deploy a vm [vm1] with shared storage and data disk
        self.virtual_machine_1 = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_1.id,
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering_1.id,
            mode=self.testdata["mode"])
        verify_vm(self, self.virtual_machine_1.id)
        # List data volume for vm1
        list_volume = Volume.list(self.userapiclient,
                                  virtualmachineid=self.virtual_machine_1.id,
                                  type='DATADISK'
                                  )
        self.assertEqual(
            validateList(list_volume)[0],
            PASS,
            "Check List volume response for vm id  %s" %
            self.virtual_machine_1.id)
        list_data_volume_for_vm1 = list_volume[0]
        self.assertEqual(
            len(list_volume),
            1,
            "There is no data disk attached to vm id:%s" %
            self.virtual_machine_1.id)
        self.assertEqual(
            list_data_volume_for_vm1.virtualmachineid, str(
                self.virtual_machine_1.id),
            "Check if volume state (attached) is reflected")
        # 2. Deploy a vm [vm2]with shared storage without data disk
        self.virtual_machine_2 = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_1.id,
            zoneid=self.zone.id,
            mode=self.testdata["mode"])
        verify_vm(self, self.virtual_machine_2.id)

        # 4. Create a new volume and attache to vm2
        self.volume = Volume.create(self.userapiclient,
                                    services=self.testdata["volume"],
                                    diskofferingid=self.disk_offering_1.id,
                                    zoneid=self.zone.id
                                    )

        list_data_volume = Volume.list(self.userapiclient,
                                       id=self.volume.id
                                       )
        self.assertEqual(
            validateList(list_data_volume)[0],
            PASS,
            "Check List volume response for volume %s" %
            self.volume.id)
        self.assertEqual(
            list_data_volume[0].id,
            self.volume.id,
            "check list volume response for volume id:  %s" %
            self.volume.id)
        self.debug(
            "volume id %s got created successfully" %
            list_data_volume[0].id)
        # Attach volume to vm2
        self.virtual_machine_2.attach_volume(self.userapiclient,
                                             self.volume
                                             )
        verify_attach_volume(self, self.virtual_machine_2.id, self.volume.id)

        # Variance
        if self.zone.localstorageenabled:
            # V1.Create vm3 with local storage offering
            self.virtual_machine_local_3 = VirtualMachine.create(
                self.userapiclient,
                self.testdata["small"],
                templateid=self.template.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering_2.id,
                zoneid=self.zone.id,
                mode=self.testdata["mode"])
            verify_vm(self, self.virtual_machine_local_3.id)

            # V2.create two data disk on local storage
            self.local_volumes = []
            for i in range(2):

                local_volume = Volume.create(
                    self.userapiclient,
                    services=self.testdata["volume"],
                    diskofferingid=self.disk_offering_local.id,
                    zoneid=self.zone.id)

                list_local_data_volume = Volume.list(self.userapiclient,
                                                     id=local_volume.id
                                                     )
                self.assertEqual(
                    validateList(list_local_data_volume)[0],
                    PASS,
                    "Check List volume response for volume %s" %
                    local_volume.id)
                self.assertEqual(
                    list_local_data_volume[0].id,
                    local_volume.id,
                    "check list volume response for volume id:  %s" %
                    local_volume.id)
                self.debug(
                    "volume id %s got created successfully" %
                    list_local_data_volume[0].id)
                self.local_volumes.append(local_volume)
            # V3.Attach local disk to vm1
            self.virtual_machine_1.attach_volume(self.userapiclient,
                                                 self.local_volumes[0]
                                                 )
            verify_attach_volume(
                self,
                self.virtual_machine_1.id,
                self.local_volumes[0].id)
        if self.list_storage:
            # V4.create vm4 with zone wide storage
            self.virtual_machine_zone_4 = VirtualMachine.create(
                self.userapiclient,
                self.testdata["small"],
                templateid=self.template.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.tagged_so.id,
                zoneid=self.zone.id,
                mode=self.testdata["mode"])
            verify_vm(self, self.virtual_machine_zone_4.id)

            # V5.Create two data disk on zone  wide storage
            self.zone_volumes = []
            for i in range(2):

                zone_volume = Volume.create(
                    self.userapiclient,
                    services=self.testdata["volume"],
                    diskofferingid=self.disk_offering_tagged.id,
                    zoneid=self.zone.id)

                list_zone_data_volume = Volume.list(self.userapiclient,
                                                    id=zone_volume.id
                                                    )
                self.assertEqual(
                    validateList(list_zone_data_volume)[0],
                    PASS,
                    "Check List volume response for volume %s" %
                    zone_volume.id)
                self.assertEqual(
                    list_zone_data_volume[0].id,
                    zone_volume.id,
                    "check list volume response for volume id:  %s" %
                    zone_volume.id)
                self.debug(
                    "volume id:%s got created successfully" %
                    list_zone_data_volume[0].id)
                self.zone_volumes.append(zone_volume)

            # V6.Attach data disk running on ZWPS to VM1 (root disk on shared)
            self.virtual_machine_1.attach_volume(self.userapiclient,
                                                 self.zone_volumes[0]
                                                 )
            verify_attach_volume(
                self,
                self.virtual_machine_1.id,
                self.zone_volumes[0].id)
            # V7. Create a cluster wide volume and attach to vm running on zone
            # wide storage
            self.cluster_volume = Volume.create(
                self.userapiclient,
                services=self.testdata["volume"],
                diskofferingid=self.disk_offering_1.id,
                zoneid=self.zone.id)
            list_cluster_volume = Volume.list(self.userapiclient,
                                              id=self.cluster_volume.id
                                              )
            self.assertEqual(
                validateList(list_cluster_volume)[0],
                PASS,
                "Check List volume response for volume %s" %
                self.cluster_volume.id)
            self.assertEqual(
                list_cluster_volume[0].id, str(
                    self.cluster_volume.id), "volume does not exist %s" %
                self.cluster_volume.id)
            self.debug(
                "volume id %s got created successfuly" %
                list_cluster_volume[0].id)
            self.virtual_machine_zone_4.attach_volume(self.userapiclient,
                                                      self.cluster_volume
                                                      )
            verify_attach_volume(
                self,
                self.virtual_machine_zone_4.id,
                self.cluster_volume.id)
        if self.list_storage and self.zone.localstorageenabled:
            # V8.Attach zone wide volume to vm running on local storage
            self.virtual_machine_local_3.attach_volume(self.userapiclient,
                                                       self.zone_volumes[1]
                                                       )
            verify_attach_volume(
                self,
                self.virtual_machine_local_3.id,
                self.zone_volumes[1].id)
            # V9.Attach local volume to a vm running on zone wide storage
            self.virtual_machine_zone_4.attach_volume(self.userapiclient,
                                                      self.local_volumes[1]
                                                      )
            verify_attach_volume(
                self,
                self.virtual_machine_zone_4.id,
                self.local_volumes[1].id)
        # 5. Detach data disk from vm1 and download it
        self.virtual_machine_1.detach_volume(self.userapiclient,
                                             volume=list_data_volume_for_vm1
                                             )
        verify_detach_volume(
            self,
            self.virtual_machine_1.id,
            list_data_volume_for_vm1.id)
        # download detached volume
        self.extract_volume = Volume.extract(
            self.userapiclient,
            volume_id=list_data_volume_for_vm1.id,
            zoneid=self.zone.id,
            mode='HTTP_DOWNLOAD')

        self.debug("extracted url is%s  :" % self.extract_volume.url)
        try:

            formatted_url = urllib.parse.unquote_plus(self.extract_volume.url)
            self.debug(
                "Attempting to download volume at url %s" %
                formatted_url)
            response = urllib.request.urlopen(formatted_url)
            self.debug("response from volume url %s" % response.getcode())
            fd, path = tempfile.mkstemp()
            self.debug(
                "Saving volume %s to path %s" %
                (list_data_volume_for_vm1.id, path))
            os.close(fd)
            with open(path, 'wb') as fd:
                fd.write(response.read())
            self.debug("Saved volume successfully")
        except Exception:
            self.fail(
                "Extract Volume Failed with invalid URL %s (vol id: %s)" %
                (self.extract_volume, list_data_volume_for_vm1.id))
        # checking  format of  downloaded volume and assigning to
        # testdata["volume_upload"]
        if "OVA" in self.extract_volume.url.upper():
            self.testdata["configurableData"]["upload_volume"]["format"] = "OVA"
        if "QCOW2" in self.extract_volume.url.upper():
            self.testdata["configurableData"]["upload_volume"]["format"] = "QCOW2"
        # 6. Upload volume by providing url of downloaded volume in step 5
        self.upload_response = Volume.upload(
            self.userapiclient,
            zoneid=self.zone.id,
            url=self.extract_volume.url,
            services=self.testdata["configurableData"]["upload_volume"])
        self.upload_response.wait_for_upload(self.userapiclient
                                             )
        self.debug("uploaded volume id is %s" % self.upload_response.id)
        # 7. Attach the volume to a different vm - vm2
        self.virtual_machine_2.attach_volume(self.userapiclient,
                                             volume=self.upload_response
                                             )
        verify_attach_volume(
            self,
            self.virtual_machine_2.id,
            self.upload_response.id)
        # 8. Try to delete an attached volume
        try:
            self.volume.delete(self.userapiclient
                               )
            self.fail(
                "Volume got deleted in attached state %s " %
                self.volume.id)
        except Exception as e:
            self.debug("Attached volume deletion failed because  %s" % e)
        # 9. Create template from root volume of VM1(stop VM->create template
        # -> start vm)

        self.virtual_machine_1.stop(self.userapiclient
                                    )

        self.list_root_disk_for_vm1 = Volume.list(
            self.userapiclient,
            virtualmachineid=self.virtual_machine_1.id,
            type='ROOT')
        self.assertEqual(
            validateList(
                self.list_root_disk_for_vm1)[0],
            PASS,
            "Check List volume response for vm %s" %
            self.virtual_machine_1.id)
        self.assertEqual(
            len(
                self.list_root_disk_for_vm1),
            1,
            "list root disk for vm1 is empty : %s" %
            self.virtual_machine_1.id)
        self.template_from_vm1_root_disk = Template.create(
            self.userapiclient,
            self.testdata["template"],
            self.list_root_disk_for_vm1[0].id,
            account=self.account.name,
            domainid=self.account.domainid)
        list_template = Template.list(
            self.userapiclient,
            templatefilter=self.testdata["templatefilter"],
            id=self.template_from_vm1_root_disk.id)
        self.assertEqual(
            validateList(list_template)[0],
            PASS,
            "Check List template response for template id %s" %
            self.template_from_vm1_root_disk.id)
        self.assertEqual(
            len(list_template),
            1,
            "list template response is empty for template id  : %s" %
            list_template[0].id)
        self.assertEqual(
            list_template[0].id,
            self.template_from_vm1_root_disk.id,
            "list template id is not same as created template")
        self.debug(
            "Template id:%s got created successfully" %
            self.template_from_vm1_root_disk.id)
        self.virtual_machine_1.start(self.userapiclient
                                     )
        # 10. Deploy a vm using template ,created  from vm1's root disk

        self.virtual_machine_3 = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template_from_vm1_root_disk.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_1.id,
            zoneid=self.zone.id,
            mode=self.testdata["mode"])
        verify_vm(self, self.virtual_machine_3.id)

        # 11.delete the template created from root disk of vm1
        try:
            self.template_from_vm1_root_disk.delete(self.userapiclient
                                                    )
            self.debug(
                "Template id: %s got deleted successfuly" %
                self.template_from_vm1_root_disk.id)
        except Exception as e:
            raise Exception("Template deletion failed with error %s" % e)
        list_template = Template.list(
            self.userapiclient,
            templatefilter=self.testdata["templatefilter"],
            id=self.template_from_vm1_root_disk.id)
        self.assertEqual(
            list_template,
            None,
            "Template is not deleted, id %s:" %
            self.template_from_vm1_root_disk.id)
        self.debug(
            "Template id%s got deleted successfully" %
            self.template_from_vm1_root_disk.id)

        # List vm and check the state of vm
        verify_vm(self, self.virtual_machine_3.id)

        # 12.Detach the disk from VM2 and re-attach the disk to VM1
        self.virtual_machine_2.detach_volume(self.userapiclient,
                                             volume=self.upload_response
                                             )
        verify_detach_volume(
            self,
            self.virtual_machine_2.id,
            self.upload_response.id)

        self.virtual_machine_1.attach_volume(self.userapiclient,
                                             volume=self.upload_response
                                             )

        verify_attach_volume(
            self,
            self.virtual_machine_1.id,
            self.upload_response.id)

        # 15.Migrate volume(detached) and then attach to a vm and live-migrate
        self.migrate_volume = Volume.create(
            self.userapiclient,
            services=self.testdata["volume"],
            diskofferingid=self.disk_offering_1.id,
            zoneid=self.zone.id)
        list_volume = Volume.list(self.apiclient,
                                  id=self.migrate_volume.id
                                  )
        self.assertEqual(
            validateList(list_volume)[0],
            PASS,
            "Check List volume response for volume %s" %
            self.migrate_volume.id)
        self.assertEqual(
            list_volume[0].id, str(
                self.migrate_volume.id), "volume does not exist %s" %
            self.migrate_volume.id)
        self.debug("volume id %s got created successfuly" % list_volume[0].id)

        self.virtual_machine_1.attach_volume(self.userapiclient,
                                             self.migrate_volume
                                             )
        verify_attach_volume(
            self,
            self.virtual_machine_1.id,
            self.migrate_volume.id)

        self.virtual_machine_1.detach_volume(self.userapiclient,
                                             volume=self.migrate_volume
                                             )
        verify_detach_volume(
            self,
            self.virtual_machine_1.id,
            self.migrate_volume.id)

        list_volume = Volume.list(self.apiclient,
                                  id=self.migrate_volume.id
                                  )
        self.assertEqual(
            validateList(list_volume)[0],
            PASS,
            "Check List volume response for volume %s" %
            self.migrate_volume.id)
        self.assertEqual(
            list_volume[0].id, str(
                self.migrate_volume.id), "volume does not exist %s" %
            self.migrate_volume.id)
        self.debug("volume id %s got created successfuly" % list_volume[0].id)
        list_pool = StoragePool.list(self.apiclient,
                                     id=list_volume[0].storageid
                                     )
        self.assertEqual(
            validateList(list_pool)[0],
            PASS,
            "Check List pool response for storage id %s" %
            list_volume[0].storageid)
        self.assertGreater(
            len(list_pool),
            0,
            "Check the list list storagepoolresponse for vm id:  %s" %
            list_volume[0].storageid)
        list_pools = StoragePool.list(self.apiclient,
                                      scope=list_pool[0].scope
                                      )
        self.assertEqual(
            validateList(list_pools)[0],
            PASS,
            "Check List pool response for scope %s" %
            list_pool[0].scope)
        self.assertGreater(
            len(list_pools),
            0,
            "Check the list vm response for scope :%s" %
            list_volume[0].scope)
        storagepoolid = None
        for i in range(len(list_pools)):
            if list_volume[0].storageid != list_pools[i].id:
                storagepoolid = list_pools[i].id
                break
            else:
                self.debug("No pool available for volume migration ")

        if storagepoolid is not None:
            try:
                volume_migrate = Volume.migrate(self.apiclient,
                                                storageid=storagepoolid,
                                                volumeid=self.migrate_volume.id
                                                )
            except Exception as e:
                raise Exception("Volume migration failed with error %s" % e)

            self.virtual_machine_2.attach_volume(self.userapiclient,
                                                 self.migrate_volume
                                                 )
            verify_attach_volume(
                self,
                self.virtual_machine_2.id,
                self.migrate_volume.id)

            pool_for_migration = StoragePool.listForMigration(
                self.apiclient,
                id=self.migrate_volume.id)
            self.assertEqual(
                validateList(pool_for_migration)[0],
                PASS,
                "Check list pool For Migration response for volume %s" %
                self.migrate_volume.id)
            self.assertGreater(
                len(pool_for_migration),
                0,
                "Check the listForMigration response for volume :%s" %
                self.migrate_volume.id)
            try:
                volume_migrate = Volume.migrate(
                    self.apiclient,
                    storageid=pool_for_migration[0].id,
                    volumeid=self.migrate_volume.id,
                    livemigrate=True)
            except Exception as e:
                raise Exception("Volume migration failed with error %s" % e)
        else:
            try:
                self.migrate_volume.delete(self.userapiclient
                                           )
                self.debug(
                    "volume id:%s got deleted successfully " %
                    self.migrate_volume.id)
            except Exception as e:
                raise Exception("Volume deletion failed with error %s" % e)
        # 16.Upload volume of size smaller  than
        # storage.max.volume.upload.size(leaving the negative case)
        self.testdata["configurableData"]["upload_volume"]["format"] = "VHD"
        volume_upload = Volume.upload(self.userapiclient,
                                      self.testdata["configurableData"]["upload_volume"],
                                      zoneid=self.zone.id
                                      )
        volume_upload.wait_for_upload(self.userapiclient
                                      )
        self.debug(
            "volume id :%s got uploaded successfully is " %
            volume_upload.id)

        # 20.Detach data disk from vm 2 and delete the volume
        self.virtual_machine_2.detach_volume(self.userapiclient,
                                             volume=self.volume
                                             )
        verify_detach_volume(self, self.virtual_machine_2.id, self.volume.id)

        try:
            self.volume.delete(self.userapiclient
                               )
            self.debug("volume id:%s got deleted successfully " %
                       self.volume.id)
        except Exception as e:
            raise Exception("Volume deletion failed with error %s" % e)

    @attr(
        tags=[
            "advanced",
            "advancedsg",
            "basic",
        ],
        required_hardware="True")
    def test_02_negative_path(self):
        """
        negative test for volume life cycle
        # 1. Deploy a vm [vm1] with shared storage and  data disk
        #v1. Create VM2 with local storage offering disk offerings
        # 2.TBD
        # 3. Detach the data disk from VM1 and Download the volume
        # 4.TBD
        # 5. Attach volume with deviceid = 0
        # 6. Attach volume, specify a VM which is destroyed
        # 7.TBD
        # 8.TBD
        # 9.TBD
        # 10.TBD
        # 11.Upload the volume from T3 by providing the URL of the downloaded
             volume, but specify a wrong format (not supported by the
             hypervisor)
        # 12.Upload the same volume from T4 by providing a wrong URL
        # 13.Upload volume, provide wrong checksum
        # 14.Upload a volume when maximum limit for the account is reached
        # 15.TBD
        # 16.Upload volume with all correct parameters
             (covered in positive test path)
        # 17.TBD
        # 18.TBD
        # 19.Now attach the volume with all correct parameters
            (covered in positive test path)
        # 20.Destroy and expunge all VMs

        """

        # 1. Deploy a vm [vm1] with shared storage and data disk
        self.virtual_machine_1 = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_1.id,
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering_1.id,
            mode=self.testdata["mode"])
        verify_vm(self, self.virtual_machine_1.id)
        # List data volume for vm1
        list_volume = Volume.list(self.userapiclient,
                                  virtualmachineid=self.virtual_machine_1.id,
                                  type='DATADISK'
                                  )
        self.assertEqual(
            validateList(list_volume)[0],
            PASS,
            "Check List volume response for vm id  %s" %
            self.virtual_machine_1.id)
        list_data_volume_for_vm1 = list_volume[0]
        self.assertEqual(
            len(list_volume),
            1,
            "There is no data disk attached to vm id:%s" %
            self.virtual_machine_1.id)
        self.assertEqual(
            list_data_volume_for_vm1.virtualmachineid, str(
                self.virtual_machine_1.id),
            "Check if volume state (attached) is reflected")
        # Variance
        if self.zone.localstorageenabled:
            # V1.Create vm3 with local storage offering
            self.virtual_machine_local_2 = VirtualMachine.create(
                self.userapiclient,
                self.testdata["small"],
                templateid=self.template.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering_2.id,
                zoneid=self.zone.id,
                mode=self.testdata["mode"])
            verify_vm(self, self.virtual_machine_local_2.id)

        # 3. Detach the data disk from VM1 and Download the volume
        self.virtual_machine_1.detach_volume(self.userapiclient,
                                             volume=list_data_volume_for_vm1
                                             )
        verify_detach_volume(
            self,
            self.virtual_machine_1.id,
            list_data_volume_for_vm1.id)
        # download detached volume
        self.extract_volume = Volume.extract(
            self.userapiclient,
            volume_id=list_data_volume_for_vm1.id,
            zoneid=self.zone.id,
            mode='HTTP_DOWNLOAD')

        self.debug("extracted url is%s  :" % self.extract_volume.url)
        try:

            formatted_url = urllib.parse.unquote_plus(self.extract_volume.url)
            self.debug(
                "Attempting to download volume at url %s" %
                formatted_url)
            response = urllib.request.urlopen(formatted_url)
            self.debug("response from volume url %s" % response.getcode())
            fd, path = tempfile.mkstemp()
            self.debug(
                "Saving volume %s to path %s" %
                (list_data_volume_for_vm1.id, path))
            os.close(fd)
            with open(path, 'wb') as fd:
                fd.write(response.read())
            self.debug("Saved volume successfully")
        except Exception:
            self.fail(
                "Extract Volume Failed with invalid URL %s (vol id: %s)" %
                (self.extract_volume, list_data_volume_for_vm1.id))

        # 6. Attach volume, specify a VM which is destroyed
        self.virtual_machine_2 = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_1.id,
            zoneid=self.zone.id,
            mode=self.testdata["mode"])
        verify_vm(self, self.virtual_machine_2.id)
        try:
            self.virtual_machine_2.delete(self.apiclient)
        except Exception as e:
            raise Exception("Vm deletion failed with error %s" % e)
        # Create a new volume
        self.volume = Volume.create(self.userapiclient,
                                    services=self.testdata["volume"],
                                    diskofferingid=self.disk_offering_1.id,
                                    zoneid=self.zone.id
                                    )

        list_data_volume = Volume.list(self.userapiclient,
                                       id=self.volume.id
                                       )
        self.assertEqual(
            validateList(list_data_volume)[0],
            PASS,
            "Check List volume response for volume %s" %
            self.volume.id)
        self.assertEqual(
            list_data_volume[0].id,
            self.volume.id,
            "check list volume response for volume id:  %s" %
            self.volume.id)
        self.debug(
            "volume id %s got created successfully" %
            list_data_volume[0].id)
        # try  Attach volume to vm2
        try:
            self.virtual_machine_2.attach_volume(self.userapiclient,
                                                 self.volume
                                                 )
            self.fail("Volume got attached to a destroyed vm ")
        except Exception:
            self.debug("Volume cant not be attached to a destroyed vm ")

        # 11.Upload the volume  by providing the URL of the downloaded
        # volume, but specify a wrong format (not supported by the hypervisor)
        if "OVA" in self.extract_volume.url.upper():
            self.testdata["configurableData"]["upload_volume"]["format"] = "VHD"
        else:
            self.testdata["configurableData"]["upload_volume"]["format"] = "OVA"
        try:
            self.upload_response = Volume.upload(
                self.userapiclient,
                zoneid=self.zone.id,
                url=self.extract_volume.url,
                services=self.testdata["configurableData"]["upload_volume"])
            self.fail("Volume got uploaded with invalid format")
        except Exception as e:
            self.debug("upload volume failed due %s" % e)
        # 12. Upload the same volume from T4 by providing a wrong URL
        self.testdata["configurableData"]["upload_volume"]["format"] = "VHD"
        if "OVA" in self.extract_volume.url.upper():
            self.testdata["configurableData"]["upload_volume"]["format"] = "OVA"
        if "QCOW2" in self.extract_volume.url.upper():
            self.testdata["configurableData"]["upload_volume"]["format"] = "QCOW2"
        u1 = self.extract_volume.url.split('.')
        u1[-2] = "wrong"
        wrong_url = ".".join(u1)
        try:
            self.upload_response = Volume.upload(
                self.userapiclient,
                zoneid=self.zone.id,
                url=wrong_url,
                services=self.testdata["configurableData"]["upload_volume"])
            self.upload_response.wait_for_upload(self.userapiclient
                                                 )
            self.fail("volume got uploaded with wrong url")
        except Exception as e:
            self.debug("upload volume failed due to %s" % e)
        # 13.Upload volume, provide wrong checksum
        try:
            self.upload_response = Volume.upload(
                self.userapiclient,
                zoneid=self.zone.id,
                url=self.extract_volume.url,
                services=self.testdata["configurableData"]["upload_volume"],
                checksome="123456")
            self.upload_response.wait_for_upload(self.userapiclient
                                                 )
            self.fail("volume got uploaded with wrong checksome")
        except Exception as e:
            self.debug("upload volume failed due to %s" % e)

        # 14.Upload a volume when maximum limit for the account is reached
        account_update = Resources.updateLimit(self.apiclient,
                                               resourcetype=2,
                                               account=self.account.name,
                                               domainid=self.account.domainid,
                                               max=1
                                               )
        list_resource = Resources.list(self.apiclient,
                                       account=self.account.name,
                                       domainid=self.account.domainid,
                                       resourcetype=2
                                       )
        self.assertEqual(
            validateList(list_resource)[0],
            PASS,
            "Check List resource response for volume %s" %
            self.account.name)
        self.assertEqual(
            str(
                list_resource[0].max),
            '1',
            "check list List resource response for account id:  %s" %
            self.account.name)
        self.debug(
            "Max resources got updated successfully for account %s" %
            self.account.name)
        try:
            self.upload_response = Volume.upload(
                self.userapiclient,
                zoneid=self.zone.id,
                url=self.extract_volume.url,
                services=self.testdata["configurableData"]["upload_volume"])
            self.upload_response.wait_for_upload(self.userapiclient
                                                 )
            self.fail("volume got uploaded after account reached max limit for\
                      volumes ")
        except Exception as e:
            self.debug("upload volume failed due to %s" % e)
