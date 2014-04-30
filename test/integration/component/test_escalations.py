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

#Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (createVolume,
                                  createTemplate)
from marvin.lib.base import (Volume,
                             Iso,
                             VirtualMachine,
                             Template,
                             Snapshot,
                             SecurityGroup,
                             Account,
                             Zone,
                             Network,
                             NetworkOffering,
                             DiskOffering,
                             ServiceOffering,
                             VmSnapshot,
                             SnapshotPolicy,
                             SSHKeyPair,
                             Resources,
                             Configurations,
                             VpnCustomerGateway,
                             Hypervisor,
                             VpcOffering,
                             VPC,
                             NetworkACL)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               list_os_types)
from marvin.lib.utils import (validateList,
                              cleanup_resources,
                              random_gen)
from marvin.codes import (PASS, FAIL, EMPTY_LIST)
from nose.plugins.attrib import attr
import time

class TestVolumes(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestVolumes, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()
        # Get Domain, Zone, Template
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client)
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        if cls.zone.localstorageenabled:
            cls.storagetype = 'local'
            cls.services["service_offerings"]["tiny"]["storagetype"] = 'local'
            cls.services["disk_offering"]["storagetype"] = 'local'
        else:
            cls.storagetype = 'shared'
            cls.services["service_offerings"]["tiny"]["storagetype"] = 'shared'
            cls.services["disk_offering"]["storagetype"] = 'shared'

        cls.services['mode'] = cls.zone.networktype
        cls.services["virtual_machine"]["hypervisor"] = 'XenServer'
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
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )

        # Getting authentication for user in newly created Account
        cls.user = cls.account.user[0]

        cls.userapiclient = cls.testClient.getUserApiClient(cls.user.username, cls.domain.name)

        # Creating Virtual Machine
        cls.virtual_machine = VirtualMachine.create(
                                    cls.userapiclient,
                                    cls.services["virtual_machine"],
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    serviceofferingid=cls.service_offering.id,
                                )
        cls._cleanup = [
                        cls.virtual_machine,
                        cls.disk_offering,
                        cls.service_offering,
                        cls.account
                        ]

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        #Clean up, terminate the created volumes
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.apiclient = super(TestVolumes, cls).getClsTestClient().getApiClient()
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)


    def __verify_values(self, expected_vals, actual_vals):
        """
        @summary: Function to verify expected and actual values
        Step1: Initializing return flag to True
        Step1: Verifying length of expected and actual dictionaries is matching.
               If not matching returning false
        Step2: Listing all the keys from expected dictionary
        Step3: Looping through each key from step2 and verifying expected and actual dictionaries have same value
               If not making return flag to False
        Step4: returning the return flag after all the values are verified
        """
        return_flag = True

        if len(expected_vals) != len(actual_vals):
            return False

        keys = expected_vals.keys()
        for i in range(0, len(expected_vals)):
            exp_val = expected_vals[keys[i]]
            act_val = actual_vals[keys[i]]
            if exp_val == act_val:
                return_flag = return_flag and True
            else:
                return_flag = return_flag and False
                self.debug("expected Value: %s, is not matching with actual value: %s" % (
                                                                                          exp_val,
                                                                                          act_val
                                                                                          ))
        return return_flag

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_01_list_volumes_pagination(self):
        """  
        @summary: Test List Volumes pagination
        
        Step1: Listing all the volumes for a user
        Step2: Verifying listed volumes for account created at class level
        Step3: If number of volumes is less than (page size + 1), then creating them
        Step4: Listing all the volumes again after creation of volumes
        Step5: Verifying the length of the volumes is (page size + 1)
        Step6: Listing all the volumes in page1
        Step7: Verifying that the length of the volumes in page 1 is (page size)
        Step8: Listing all the volumes in page2
        Step9: Verifying that the length of the volumes in page 2 is 1
        Step10: Deleting the volume present in page 2
        Step11: Listing for the volumes on page 2
        Step12: Verifying that there are no volumes present in page 2
        """
        # Listing all the volumes for a user
        list_volumes_before = Volume.list(self.userapiclient, listall=self.services["listall"])

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
            if(i < (self.services["pagesize"] - 1)):
                self.cleanup.append(volume_created)
                
            self.assertEqual(
                             self.services["volume"]["diskname"],
                             volume_created.name,
                             "Newly created volume name and the test data volume name are not matching"
                             )

        # Listing all the volumes again after creation of volumes        
        list_volumes_after = Volume.list(self.userapiclient, listall=self.services["listall"]) 

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
                         "List Volume response is not matching with the page size length for page 1"
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
                         "List Volume response is not matching with the page size length for page 2"
                         )
        volume_page2 = list_volumes_page2[0]

        # Verifying that the volume on page 2 is not present in page1
        for i in range(0, len(list_volumes_page1)):
            volume_page1 = list_volumes_page1[i]
            self.assertNotEquals(
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

    @attr(tags=["advanced", "basic", "provisioning"]) 
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
        self.cleanup.append(volume_created)
        
        self.assertEqual(
                         self.services["volume"]["diskname"],
                         volume_created.name,
                         "Newly created volume name and the test data volume name are not matching"
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

        #Creating expected and actual values dictionaries
        expected_dict = {
                           "id":root_volume.id,
                           "name":root_volume.name,
                           "vmname":self.virtual_machine.name,
                           "state":"Ready",
                           "type":"ROOT",
                           "zoneid":self.zone.id,
                           "account":self.account.name,
                           "storagetype":self.storagetype,
                           "size":self.template.size
                           }
        actual_dict = {
                           "id":obtained_volume.id,
                           "name":obtained_volume.name,
                           "vmname":obtained_volume.vmname,
                           "state":obtained_volume.state,
                           "type":obtained_volume.type,
                           "zoneid":obtained_volume.zoneid,
                           "account":obtained_volume.account,
                           "storagetype":obtained_volume.storagetype,
                           "size":obtained_volume.size,
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

        #Creating expected and actual values dictionaries
        expected_dict = {
                           "id":volume_created.id,
                           "name":volume_created.name,
                           "state":"Allocated",
                           "type":"DATADISK",
                           "zoneid":self.zone.id,
                           "account":self.account.name,
                           "storagetype":self.storagetype,
                           "size":self.disk_offering.disksize
                           }
        actual_dict = {
                           "id":obtained_volume.id,
                           "name":obtained_volume.name,
                           "state":obtained_volume.state,
                           "type":obtained_volume.type,
                           "zoneid":obtained_volume.zoneid,
                           "account":obtained_volume.account,
                           "storagetype":obtained_volume.storagetype,
                           "size":obtained_volume.size/(1024*1024*1024),
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

    @attr(tags=["advanced", "basic", "provisioning"])
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
        # Listing volumes for a user before creating a volume
        list_volumes_before = Volume.list(self.userapiclient, listall=self.services["listall"])

        # Creating a data volume
        volume_created = Volume.create(
                                       self.userapiclient,
                                       self.services["volume"],
                                       zoneid=self.zone.id,
                                       diskofferingid=self.disk_offering.id
                                       )
        self.assertIsNotNone(volume_created, "Data volume creation failed")

        self.cleanup.append(volume_created)

        # Listing volumes for a user after creating data volume
        list_volumes_after = Volume.list(self.userapiclient, listall=self.services["listall"])
        self.assertEquals(
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
        self.assertEquals(
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

        # Verifying if there exists a disk offering with higher size. If not present creating it
        list_disk_offerings = DiskOffering.list(self.apiClient)

        large_disk_offering_exists = False
        # Converting disk_size in bytes to GB
        current_disk_size = volume_created.size/(1024*1024*1024)

        for disk_offering in list_disk_offerings:
            if ((disk_offering.disksize > current_disk_size) and (not disk_offering.iscustomized) and disk_offering.storagetype == self.storagetype):
                new_disk_offering = disk_offering
                large_disk_offering_exists = True
                break

        if large_disk_offering_exists == False:
            new_size = (volume_created.size/(1024*1024*1024)) + 1
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
        self.assertEquals(
                          new_size,
                          (resized_volume.size/(1024*1024*1024)),
                          "volume not resized to expected value"
                          )
        return

    @attr(tags=["advanced", "basic", "provisioning"])
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
        # Listing all the disk offerings
        list_disk_offerings = DiskOffering.list(self.apiClient)

        custom_disk_offering_exists = False

        # Verifying if a custom disk offering already exists
        if list_disk_offerings is not None:
            for disk_offering in list_disk_offerings:
                if (disk_offering.iscustomized and disk_offering.storagetype == self.storagetype):
                    custom_disk_offering = disk_offering
                    custom_disk_offering_exists = True
                    break

        # If a custom disk offering does not exists, then creating a custom disk offering
        if custom_disk_offering_exists == False:
            custom_disk_offering = DiskOffering.create(
                                    self.apiClient,
                                    self.services["disk_offering"],
                                    custom=True
                                    )
            if custom_disk_offering is not None:
                self.cleanup.append(custom_disk_offering)

        # Listing the volumes for a user before creating custom volume
        list_volumes_before = Volume.list(self.userapiclient, listall=self.services["listall"])

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

        self.cleanup.append(volume_created)

        # Listing the volumes for a user after creating custom volume
        list_volumes_after = Volume.list(self.userapiclient, listall=self.services["listall"])

        # Verifyign that volume list is increased by 1 after creation of custion volume
        self.assertEquals(
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
        self.assertEquals(
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
        self.assertEquals(
                          new_size,
                          (resized_volume.size/(1024*1024*1024)),
                          "volume not resized to expected value"
                          )
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_05_volume_snapshot(self):
        """  
        @summary: Test to verify creation of snapshot from volume and creation of template, volume from snapshot
         
        Step1: Creating a volume
        Step2: Attaching and Detaching custom volume created to Virtual Machine
        Step3: Creating Snapshot from volume
        Step4: Creating Volume from snapshot
        Step5: Creating Template from Snapshot
        """
        list_volumes_before = Volume.list(self.userapiclient, listall=self.services["listall"])

        volume_created = Volume.create(
                                    self.userapiclient,
                                    self.services["volume"],
                                    zoneid=self.zone.id,
                                    diskofferingid=self.disk_offering.id
                                    )

        self.assertIsNotNone(volume_created, "Volume not created")

        if volume_created is not None:
            self.cleanup.append(volume_created)

        list_volumes_after = Volume.list(self.userapiclient, listall=self.services["listall"])

        self.assertEquals(
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
        self.assertEquals(
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

        self.cleanup.append(snapshot_created)

        #Creating expected and actual values dictionaries
        expected_dict = {
                           "id":volume_created.id,
                           "intervaltype":"MANUAL",
                           "snapshottype":"MANUAL",
                           "volumetype":volume_created.type,
                           "domain":self.domain.id
                           }
        actual_dict = {
                           "id":snapshot_created.volumeid,
                           "intervaltype":snapshot_created.intervaltype,
                           "snapshottype":snapshot_created.snapshottype,
                           "volumetype":snapshot_created.volumetype,
                           "domain":snapshot_created.domainid,
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
        cmd.name = "-".join([self.services["volume"]["diskname"], random_gen()])
        cmd.snapshotid = snapshot_created.id

        volume_from_snapshot = Volume(self.userapiclient.createVolume(cmd).__dict__)

        self.assertIsNotNone(
                             volume_from_snapshot,
                             "Volume creation failed from snapshot"
                             )
        self.cleanup.append(volume_from_snapshot)

        #Creating expected and actual values dictionaries
        expected_dict = {
                           "snapshotid":snapshot_created.id,
                           "volumetype":snapshot_created.volumetype,
                           "size":self.disk_offering.disksize,
                           "accounr":self.account.name,
                           "domain":self.domain.id,
                           "storagetype":self.storagetype,
                           "zone":self.zone.id
                           }
        actual_dict = {
                           "snapshotid":volume_from_snapshot.snapshotid,
                           "volumetype":volume_from_snapshot.type,
                           "size":volume_from_snapshot.size/(1024*1024*1024),
                           "accounr":volume_from_snapshot.account,
                           "domain":volume_from_snapshot.domainid,
                           "storagetype":volume_from_snapshot.storagetype,
                           "zone":volume_from_snapshot.zoneid,
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
        list_templates_before = Template.list(self.userapiclient, templatefilter='self')

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

        template_from_snapshot = Template(self.userapiclient.createTemplate(cmd).__dict__)

        self.assertIsNotNone(
                             template_from_snapshot,
                             "Template creation failed from snapshot"
                             )

        self.cleanup.append(template_from_snapshot)

        #Creating expected and actual values dictionaries
        expected_dict = {
                           "name":self.services["ostype"],
                           "ostypeid":self.template.ostypeid,
                           "type":"USER",
                           "zone":self.zone.id,
                           "domain":self.domain.id,
                           "account":self.account.name,
                           "passwordenabled":False,
                           "ispublic":False,
                           "size":self.disk_offering.disksize
                           }
        actual_dict = {
                           "name":template_from_snapshot.name,
                           "ostypeid":template_from_snapshot.ostypeid,
                           "type":template_from_snapshot.templatetype,
                           "zone":template_from_snapshot.zoneid,
                           "domain":template_from_snapshot.domainid,
                           "account":template_from_snapshot.account,
                           "passwordenabled":template_from_snapshot.passwordenabled,
                           "ispublic":template_from_snapshot.ispublic,
                           "size":template_from_snapshot.size/(1024*1024*1024)
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

        list_templates_after = Template.list(self.userapiclient, templatefilter='self')

        self.assertEquals(
                          templates_before_size + 1,
                          len(list_templates_after),
                          "Template creation failed from snapshot"
                          )
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_06_volume_snapshot_policy_hourly(self):
        """  
        @summary: Test to verify creation of Hourly Snapshot policies from volume
         
        Step1: Creating a Volume.
        Step2: Attaching volume created in Step2 to virtual machine
        Step3: Detaching the volume created in step2 from virtual machine
        Step4: Listing snapshot policies for a volume created in step1
        Step5: Creating Hourly snapshot policy
        Step6: Listing snapshot policies for a volume created in step1 again
        Step7: Verifyign that the list snapshot policy length is increased by 1
        """
        list_volumes_before = Volume.list(self.userapiclient, listall=self.services["listall"])

        volume_created = Volume.create(
                                    self.userapiclient,
                                    self.services["volume"],
                                    zoneid=self.zone.id,
                                    diskofferingid=self.disk_offering.id
                                    )

        self.assertIsNotNone(volume_created, "Volume not created")
        self.cleanup.append(volume_created)

        list_volumes_after = Volume.list(self.userapiclient, listall=self.services["listall"])

        self.assertEquals(
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
        self.assertEquals(
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

        list_snapshot_policy_before = SnapshotPolicy.list(self.userapiclient, volumeid=volume_created.id)

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
        #Creating expected and actual values dictionaries
        expected_dict = {
                         "schedule":self.services["recurring_snapshot"]["schedule"],
                         "intervaltype":0,
                         "volumeid":volume_created.id
                         }
        actual_dict = {
                       "schedule":snapshot_policy_hourly.schedule,
                       "intervaltype":snapshot_policy_hourly.intervaltype,
                       "volumeid":snapshot_policy_hourly.volumeid
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

        list_snapshot_policy_after = SnapshotPolicy.list(self.userapiclient, volumeid=volume_created.id)

        self.assertIsNotNone(
                             list_snapshot_policy_after,
                             "Hourly Snapshot policy creation failed"
                             ) 
        self.assertEquals(
                          snapshot_policy_before_size + 1,
                          len(list_snapshot_policy_after),
                          "Hourly Snapshot policy creation failed"
                          )
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_07_volume_snapshot_policy_daily(self):
        """  
        @summary: Test to verify creation of Daily Snapshot policies from volume
         
        Step1: Creating a Volume.
        Step2: Attaching volume created in Step2 to virtual machine
        Step3: Detaching the volume created in step2 from virtual machine
        Step4: Listing snapshot policies for a volume created in step1
        Step5: Creating Daily snapshot policy
        Step6: Listing snapshot policies for a volume created in step1 again
        Step7: Verifyign that the list snapshot policy length is increased by 1
        """
        list_volumes_before = Volume.list(self.userapiclient, listall=self.services["listall"])

        volume_created = Volume.create(
                                    self.userapiclient,
                                    self.services["volume"],
                                    zoneid=self.zone.id,
                                    diskofferingid=self.disk_offering.id
                                    )

        self.assertIsNotNone(volume_created, "Volume not created")
        self.cleanup.append(volume_created)

        list_volumes_after = Volume.list(self.userapiclient, listall=self.services["listall"])

        self.assertEquals(
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
        self.assertEquals(
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

        list_snapshot_policy_before = SnapshotPolicy.list(self.userapiclient, volumeid=volume_created.id)

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
        #Creating expected and actual values dictionaries
        expected_dict = {
                         "schedule":self.services["recurring_snapshot"]["schedule"],
                         "intervaltype":1,
                         "volumeid":volume_created.id
                         }
        actual_dict = {
                       "schedule":snapshot_policy_daily.schedule,
                       "intervaltype":snapshot_policy_daily.intervaltype,
                       "volumeid":snapshot_policy_daily.volumeid
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

        list_snapshot_policy_after = SnapshotPolicy.list(self.userapiclient, volumeid=volume_created.id)

        self.assertIsNotNone(
                             list_snapshot_policy_after,
                             "Daily Snapshot policy creation failed"
                             )
        self.assertEquals(
                          snapshot_policy_before_size + 1,
                          len(list_snapshot_policy_after),
                          "Daily Snapshot policy creation failed"
                          )
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_08_volume_snapshot_policy_weekly(self):
        """  
        @summary: Test to verify creation of Weekly Snapshot policies from volume
         
        Step1: Creating a Volume.
        Step2: Attaching volume created in Step2 to virtual machine
        Step3: Detaching the volume created in step2 from virtual machine
        Step4: Listing snapshot policies for a volume created in step1
        Step5: Creating Weekly snapshot policy
        Step6: Listing snapshot policies for a volume created in step1 again
        Step7: Verifyign that the list snapshot policy length is increased by 1
        """
        list_volumes_before = Volume.list(self.userapiclient, listall=self.services["listall"])

        volume_created = Volume.create(
                                    self.userapiclient,
                                    self.services["volume"],
                                    zoneid=self.zone.id,
                                    diskofferingid=self.disk_offering.id
                                    )

        self.assertIsNotNone(volume_created, "Volume not created")
        self.cleanup.append(volume_created)

        list_volumes_after = Volume.list(self.userapiclient, listall=self.services["listall"])

        self.assertEquals(
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
        self.assertEquals(
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

        list_snapshot_policy_before = SnapshotPolicy.list(self.userapiclient, volumeid=volume_created.id)

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
        #Creating expected and actual values dictionaries
        expected_dict = {
                         "schedule":self.services["recurring_snapshot"]["schedule"],
                         "intervaltype":2,
                         "volumeid":volume_created.id
                         }
        actual_dict = {
                       "schedule":snapshot_policy_weekly.schedule,
                       "intervaltype":snapshot_policy_weekly.intervaltype,
                       "volumeid":snapshot_policy_weekly.volumeid
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

        list_snapshot_policy_after = SnapshotPolicy.list(self.userapiclient, volumeid=volume_created.id)

        self.assertIsNotNone(
                             list_snapshot_policy_after,
                             "Weekly Snapshot policy creation failed"
                             )
        self.assertEquals(
                          snapshot_policy_before_size + 1,
                          len(list_snapshot_policy_after),
                          "Weekly Snapshot policy creation failed"
                          )
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_09_volume_snapshot_policy_monthly(self):
        """  
        @summary: Test to verify creation of Monthly Snapshot policies from volume
         
        Step1: Creating a Volume.
        Step2: Attaching volume created in Step2 to virtual machine
        Step3: Detaching the volume created in step2 from virtual machine
        Step4: Listing snapshot policies for a volume created in step1
        Step5: Creating Monthly snapshot policy
        Step6: Listing snapshot policies for a volume created in step1 again
        Step7: Verifyign that the list snapshot policy length is increased by 1
        Step8: Deleting monthly snapshot policy created in step5
        Step9: List snapshot policies for a volume again
        Step10: Verifying that the list snapshot policy length is decreased by 1
        """
        list_volumes_before = Volume.list(self.userapiclient, listall=self.services["listall"])

        volume_created = Volume.create(
                                    self.userapiclient,
                                    self.services["volume"],
                                    zoneid=self.zone.id,
                                    diskofferingid=self.disk_offering.id
                                    )
        self.assertIsNotNone(volume_created, "Volume not created")
        self.cleanup.append(volume_created)

        list_volumes_after = Volume.list(self.userapiclient, listall=self.services["listall"])

        self.assertEquals(
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
        self.assertEquals(
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

        list_snapshot_policy_before = SnapshotPolicy.list(self.userapiclient, volumeid=volume_created.id)

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
        #Creating expected and actual values dictionaries
        expected_dict = {
                         "schedule":self.services["recurring_snapshot"]["schedule"],
                         "intervaltype":3,
                         "volumeid":volume_created.id
                         }
        actual_dict = {
                       "schedule":snapshot_policy_monthly.schedule,
                       "intervaltype":snapshot_policy_monthly.intervaltype,
                       "volumeid":snapshot_policy_monthly.volumeid
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

        list_snapshot_policy_after = SnapshotPolicy.list(self.userapiclient, volumeid=volume_created.id)

        self.assertIsNotNone(
                             list_snapshot_policy_after,
                             "Monthly Snapshot policy creation failed"
                             )
        self.assertEquals(
                          snapshot_policy_before_size + 1,
                          len(list_snapshot_policy_after),
                          "Monthly Snapshot policy creation failed"
                          )
        # Deleting monthly snapshot policy 
        SnapshotPolicy.delete(snapshot_policy_monthly, self.userapiclient)

        list_snapshot_policies = SnapshotPolicy.list(self.userapiclient, volumeid=volume_created.id)

        self.assertIsNone(
                          list_snapshot_policies, 
                          "Deletion of Monthly Snapshot policy failed"
                          )
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_10_volume_snapshots_pagination(self):
        """  
        @summary: Test to verify pagination of snapshots for Volume
         
        Step1: Creating a Volume.
        Step2: Attaching volume created in Step2 to virtual machine
        Step3: Detaching the volume created in step2 from virtual machine
        Step4: Listing all the snapshots for a volume
        Step5: Creating Pagesize + 1 number of snapshots for a volume
        Step6: Listing all the snapshots for a volume
        Step7: Verifying that there are pagesize + 1 number of snapshots listsed
        Step8: Listing all the snapshots in page 1
        Step9: Listing all the snapshots in page 2
        Step10: Deleting the snapshot present in page 2
        Step11: Listign the snapshots from page 2 again and verifyign that list returns none
        """
        list_volumes_before = Volume.list(self.userapiclient, listall=self.services["listall"])

        # Creating a Volume 
        volume_created = Volume.create(
                                    self.userapiclient,
                                    self.services["volume"],
                                    zoneid=self.zone.id,
                                    diskofferingid=self.disk_offering.id
                                    )
        self.assertIsNotNone(volume_created, "Volume not created")
        self.cleanup.append(volume_created)

        list_volumes_after = Volume.list(self.userapiclient, listall=self.services["listall"])

        self.assertEquals(
                          len(list_volumes_before) + 1,
                          len(list_volumes_after),
                          "Volume not created"
                          )
        #Attaching volume to virtual machine
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
        self.assertEquals(
                          self.virtual_machine.name,
                          attached_volume.vmname,
                          "VM Name is not matching with attached vm"
                          )
        #Detaching volume from virtual machine
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

        #Creating 3 Snapshots from volume
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
            self.cleanup.append(snapshot_created)
     
            self.assertEquals(
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
                         list_snapshot_before_size+3,
                         len(list_snapshot_after),
                         "Number of snapshots created is not matching expected"
                         )
        #Listing all the snapshots in page1
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
                         "List snapshots response is not matching with the page size length for page 1"
                         )

        #Listing all the snapshots in page2 and ensuring only 1 snapshot is present 
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
                         "List snapshots response is not matching with the page size length for page 2"
                         )
        snapshot_page2 = list_snapshots_page2[0]

        # Verifying that the snapshot on page 2 is not present in page1
        for i in range(0, len(list_snapshots_page1)):
            snapshot_page1 = list_snapshots_page1[i]
            self.assertNotEquals(
                                 snapshot_page2.id,
                                 snapshot_page1.id,
                                 "Snapshot listed in page 2 is also listed in page 1"
                                 )
        # Deleting a single snapshot and verifying that snapshot does not exists on page 2
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

    @attr(tags=["advanced", "basic", "provisioning"])
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
        list_volumes_before = Volume.list(self.userapiclient, listall=self.services["listall"])

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
        self.cleanup.append(volume_created)

        list_volumes_after = Volume.list(self.userapiclient, listall=self.services["listall"])

        self.assertIsNotNone(
                             list_volumes_after,
                             "volume creation failed"
                             )
        self.assertEquals(
                          len(list_volumes_before) + 1,
                          len(list_volumes_after),
                          "Volume not created"
                          )
        #Attaching and Detaching volume created to Virtual Machine
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
        self.assertEquals(
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
        #Extract/Download the volume
        self.services["mode"] = "HTTP_DOWNLOAD"

        extract_volume_response = Volume.extract(
                                                 self.userapiclient,
                                                 volume_created.id,
                                                 self.zone.id,
                                                 self.services["mode"]
                                                 )
        self.assertIsNotNone(extract_volume_response, "Extract/Download volume failed")

        self.assertEquals(
                          "DOWNLOAD_URL_CREATED",
                          extract_volume_response.state,
                          "Failed to create Download URL"
                          )
        self.assertIsNotNone(
                             extract_volume_response.url,
                             "Extract/Download volume URL is NULL"
                             )
        self.assertTrue(
                        (extract_volume_response.url.find("https://")!=-1),
                        "Extract/Download volume URL doesnot contain https://"
                        )
        self.assertEquals(
                          volume_created.id,
                          extract_volume_response.id,
                          "Extracted/Downloaded volume is not matching with original volume"
                          )
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_12_volume_upload(self):
        """  
        @summary: Test to verify upload volume
        
        Step1: Listing the volumes for a user before uploading volume
        Step2: Uploading a volume
        Step3: Listing the volumes for a user after uploading data volume
        Step4: Verifying that the list volume length after upload is increased by 1
        """
        list_volumes_before = Volume.list(self.userapiclient, listall=self.services["listall"])

        self.assertIsNotNone(
                             list_volumes_before,
                             "volume not created for the vm launched at class level"
                             )
        #Uploading a Volume
        volume_uploaded = Volume.upload(
                                       self.userapiclient,
                                       self.services["upload_volume"],
                                       self.zone.id
                                       )
        self.assertIsNotNone(volume_uploaded, "volume uploading failed")

        self.assertEquals(
                          self.services["upload_volume"]["diskname"],
                          volume_uploaded.name,
                          "Uploaded volume name is not matching with name provided while uploading")

        #Listing the volumes for a user after uploading data volume
        list_volumes_after = Volume.list(self.userapiclient, listall=self.services["listall"])

        self.assertIsNotNone(
                             list_volumes_after,
                             "volume not created for the vm launched at class level"
                             )
        #Asserting that the list volume length after upload is increased by 1
        self.assertEquals(
                          len(list_volumes_before) + 1,
                          len(list_volumes_after),
                          "upload volume failed"
                          )
        return

class TestListInstances(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []        
            cls.testClient = super(TestListInstances, cls).getClsTestClient()
            cls.api_client = cls.testClient.getApiClient()
            cls.services = cls.testClient.getParsedTestDataConfig()

            # Get Domain, Zone, Template
            cls.domain = get_domain(cls.api_client)
            cls.zone = get_zone(cls.api_client)
            cls.template = get_template(
                                        cls.api_client,
                                        cls.zone.id,
                                        cls.services["ostype"]
                                        )
            if cls.zone.localstorageenabled:
                cls.storagetype = 'local'
                cls.services["service_offerings"]["tiny"]["storagetype"] = 'local'
                cls.services["disk_offering"]["storagetype"] = 'local'
            else:
                cls.storagetype = 'shared'
                cls.services["service_offerings"]["tiny"]["storagetype"] = 'shared'
                cls.services["disk_offering"]["storagetype"] = 'shared'

            cls.services['mode'] = cls.zone.networktype
            cls.services["virtual_machine"]["hypervisor"] = cls.testClient.getHypervisorInfo()
            cls.services["virtual_machine"]["zoneid"] = cls.zone.id
            cls.services["virtual_machine"]["template"] = cls.template.id
            cls.services["custom_volume"]["zoneid"] = cls.zone.id

            # Creating Disk offering, Service Offering and Account
            cls.disk_offering = DiskOffering.create(
                                                    cls.api_client,
                                                    cls.services["disk_offering"]
                                                    )
            cls._cleanup.append(cls.disk_offering)
            cls.service_offering = ServiceOffering.create(
                                                          cls.api_client,
                                                          cls.services["service_offerings"]["tiny"]
                                                          )
            cls._cleanup.append(cls.service_offering)
            cls.account = Account.create(
                                         cls.api_client,
                                         cls.services["account"],
                                         domainid=cls.domain.id
                                         )
            # Getting authentication for user in newly created Account
            cls.user = cls.account.user[0]
            cls.userapiclient = cls.testClient.getUserApiClient(cls.user.username, cls.domain.name)
            # Updating resource Limits
            for i in range(0,12):
                Resources.updateLimit(
                                      cls.api_client,
                                      account=cls.account.name,
                                      domainid=cls.domain.id,
                                      max=-1,
                                      resourcetype=i
                                      )

            cls._cleanup.append(cls.account)
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setup : %s" % e)
        return

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        #Clean up, terminate the created resources
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def __verify_values(self, expected_vals, actual_vals):
        """  
        @Desc: Function to verify expected and actual values
        @Steps:
        Step1: Initializing return flag to True
        Step1: Verifying length of expected and actual dictionaries is matching.
               If not matching returning false
        Step2: Listing all the keys from expected dictionary
        Step3: Looping through each key from step2 and verifying expected and actual dictionaries have same value
               If not making return flag to False
        Step4: returning the return flag after all the values are verified
        """
        return_flag = True

        if len(expected_vals) != len(actual_vals):
            return False

        keys = expected_vals.keys()
        for i in range(0, len(expected_vals)):
            exp_val = expected_vals[keys[i]]
            act_val = actual_vals[keys[i]]
            if exp_val == act_val:
                return_flag = return_flag and True
            else:
                return_flag = return_flag and False
                self.debug("expected Value: %s, is not matching with actual value: %s" % (
                                                                                          exp_val,
                                                                                          act_val
                                                                                          ))
        return return_flag

    @attr(tags=["advanced", "basic", "selfservice"])
    def test_01_list_instances_pagination(self):
        """  
        @Desc: Test List Instances pagination
        @Steps:
        Step1: Listing all the Instances for a user
        Step2: Verifying listed Instances for account created at class level
        Step3: If number of volumes is less than (page size + 1), then creating them
        Step4: Listing all the volumes again after creation of volumes
        Step5: Verifying the length of the volumes is (page size + 1)
        Step6: Listing all the volumes in page1
        Step7: Verifying that the length of the volumes in page 1 is (page size)
        Step8: Listing all the volumes in page2
        Step9: Verifying that the length of the volumes in page 2 is 1
        Step10: Deleting the volume present in page 2
        Step11: Listing for the volumes on page 2
        Step12: Verifying that there are no volumes present in page 2
        """
        # Listing all the instances for a user
        list_instances_before = VirtualMachine.list(self.userapiclient, listall=self.services["listall"])

        # Verifying listed instances for account created at class level
        self.assertIsNone(
                          list_instances_before,
                          "Virtual Machine already exists for newly created user"
                          )
        # If number of instances are less than (pagesize + 1), then creating them    
        for i in range(0, (self.services["pagesize"] + 1)):
            vm_created = VirtualMachine.create(
                                               self.userapiclient,
                                               self.services["virtual_machine"],
                                               accountid=self.account.name,
                                               domainid=self.account.domainid,
                                               serviceofferingid=self.service_offering.id,
                                               )
            self.assertIsNotNone(
                                 vm_created,
                                 "VM creation failed"
                                 )
            if(i < (self.services["pagesize"])):
                self.cleanup.append(vm_created)

            self.assertEqual(
                             self.services["virtual_machine"]["displayname"],
                             vm_created.displayname,
                             "Newly created VM name and the test data VM name are not matching"
                             )

        # Listing all the instances again after creating VM's        
        list_instances_after = VirtualMachine.list(self.userapiclient, listall=self.services["listall"])
        status = validateList(list_instances_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of instances after creation failed"
                          )
        # Verifying the length of the instances is (page size + 1)
        self.assertEqual(
                         len(list_instances_after),
                         (self.services["pagesize"] + 1),
                         "Number of instances created is not matching as expected"
                         )

        # Listing all the volumes in page1
        list_instances_page1 = VirtualMachine.list(
                                                   self.userapiclient,
                                                   listall=self.services["listall"],
                                                   page=1,
                                                   pagesize=self.services["pagesize"],
                                                   domainid=self.account.domainid
                                                   )
        status = validateList(list_instances_page1)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of instances in page1 failed"
                          )
        # Verifying that the length of the instances in page 1 is (page size)
        self.assertEqual(
                         self.services["pagesize"],
                         len(list_instances_page1),
                         "List VM response is not matching with the page size length for page 1"
                         )

        # Listing all the VM's in page2
        list_instances_page2 = VirtualMachine.list(
                                                   self.userapiclient,
                                                   listall=self.services["listall"],
                                                   page=2,
                                                   pagesize=self.services["pagesize"],
                                                   domainid=self.account.domainid
                                                   )
        status = validateList(list_instances_page2)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of instances in page2 failed"
                          )
        # Verifying that the length of the VM's in page 2 is 1
        self.assertEqual(
                         1,
                         len(list_instances_page2),
                         "List VM response is not matching with the page size length for page 2"
                         )
        instance_page2 = list_instances_page2[0]

        # Verifying that the VM on page 2 is not present in page1
        for i in range(0, len(list_instances_page1)):
            instance_page1 = list_instances_page1[i]
            self.assertNotEquals(
                                 instance_page2.id,
                                 instance_page1.id,
                                 "VM listed in page 2 is also listed in page 1"
                                 )

        # Deleting a single VM
        VirtualMachine.delete(vm_created, self.userapiclient)

        # Listing the VM's in page 2
        list_instance_response = VirtualMachine.list(
                                                     self.userapiclient,
                                                     listall=self.services["listall"],
                                                     page=2,
                                                     pagesize=self.services["pagesize"],
                                                     domainid=self.account.domainid
                                                     )
        # verifying that VM does not exists on page 2
        self.assertEqual(
                        list_instance_response,
                        None,
                        "VM was not deleted"
                        )
        return
 
    @attr(tags=["advanced", "basic", "selfservice"])
    def test_02_list_Running_vm(self):
        """  
        @Desc: Test List Running VM's
        @Steps:
        Step1: Listing all the Running VMs for a user
        Step2: Verifying that the size of the list is 0
        Step3: Deploying a VM
        Step4: Listing all the Running VMs for a user again
        Step5: Verifying that the size of the list is increased by 1
        Step6: Verifying that the details of the Running VM listed are same as the VM deployed in Step3
        """
        # Listing all the Running VM's for a User
        list_running_vms_before = VirtualMachine.list(
                                                      self.userapiclient,
                                                      listall=self.services["listall"],
                                                      page=1,
                                                      pagesize=self.services["pagesize"],
                                                      domainid=self.account.domainid,
                                                      state="Running"
                                                      )
        self.assertIsNone(
                          list_running_vms_before,
                          "Virtual Machine already exists for newly created user"
                          )
        # Deploying a VM
        vm_created = VirtualMachine.create(
                                           self.userapiclient,
                                           self.services["virtual_machine"],
                                           accountid=self.account.name,
                                           domainid=self.account.domainid,
                                           serviceofferingid=self.service_offering.id,
                                           )
        self.assertIsNotNone(
                             vm_created,
                             "VM creation failed"
                             )
        self.cleanup.append(vm_created)
        # Listing all the Running VM's for a User
        list_running_vms_after = VirtualMachine.list(
                                                      self.userapiclient,
                                                      listall=self.services["listall"],
                                                      page=1,
                                                      pagesize=self.services["pagesize"],
                                                      domainid=self.account.domainid,
                                                      state="Running"
                                                      )
        status = validateList(list_running_vms_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Newly created VM is not in Running state"
                          )
        # Verifying list size is 1
        self.assertEquals(
                          1,
                          len(list_running_vms_after),
                          "Running VM list count is not matching"
                          )
        running_vm = list_running_vms_after[0]

        #Creating expected and actual values dictionaries
        expected_dict = {
                         "id":vm_created.id,
                         "name":vm_created.name,
                         "displayname":vm_created.displayname,
                         "state":"Running",
                         "zoneid":vm_created.zoneid,
                         "account":vm_created.account,
                         "template":vm_created.templateid
                         }
        actual_dict = {
                       "id":running_vm.id,
                       "name":running_vm.name,
                       "displayname":running_vm.displayname,
                       "state":running_vm.state,
                       "zoneid":running_vm.zoneid,
                       "account":running_vm.account,
                       "template":running_vm.templateid
                       }
        running_vm_status = self.__verify_values(
                                                  expected_dict,
                                                  actual_dict
                                                  )
        self.assertEqual(
                         True,
                         running_vm_status,
                         "Listed Running VM details are not as expected"
                         )
        return

    @attr(tags=["advanced", "basic", "selfservice"])
    def test_03_list_Stopped_vm(self):
        """  
        @Desc: Test List Stopped VM's
        @Steps:
        Step1: Listing all the Stopped VMs for a user
        Step2: Verifying that the size of the list is 0
        Step3: Deploying a VM
        Step4: Stopping the VM deployed in step3
        Step5: Listing all the Stopped VMs for a user again
        Step6: Verifying that the size of the list is increased by 1
        Step7: Verifying that the details of the Stopped VM listed are same as the VM stopped in Step4
        """
        # Listing all the Stopped VM's for a User
        list_stopped_vms_before = VirtualMachine.list(
                                                      self.userapiclient,
                                                      listall=self.services["listall"],
                                                      page=1,
                                                      pagesize=self.services["pagesize"],
                                                      domainid=self.account.domainid,
                                                      state="Stopped"
                                                      )
        self.assertIsNone(
                           list_stopped_vms_before,
                           "Virtual Machine already exists for newly created user"
                           )
        # Deploying a VM
        vm_created = VirtualMachine.create(
                                           self.userapiclient,
                                           self.services["virtual_machine"],
                                           accountid=self.account.name,
                                           domainid=self.account.domainid,
                                           serviceofferingid=self.service_offering.id,
                                           )
        self.assertIsNotNone(
                             vm_created,
                             "VM creation failed"
                             )
        self.cleanup.append(vm_created)
        # Stopping the VM
        VirtualMachine.stop(vm_created, self.userapiclient)
        # Listing all the Stopped VM's for a User
        list_stopped_vms_after = VirtualMachine.list(
                                                      self.userapiclient,
                                                      listall=self.services["listall"],
                                                      page=1,
                                                      pagesize=self.services["pagesize"],
                                                      domainid=self.account.domainid,
                                                      state="Stopped"
                                                      )
        status = validateList(list_stopped_vms_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Stopped VM is not in Stopped state"
                          )
        # Verifying list size is 1
        self.assertEquals(
                          1,
                          len(list_stopped_vms_after),
                          "Stopped VM list count is not matching"
                          )
        stopped_vm = list_stopped_vms_after[0]
        #Creating expected and actual values dictionaries
        expected_dict = {
                         "id":vm_created.id,
                         "name":vm_created.name,
                         "displayname":vm_created.displayname,
                         "state":"Stopped",
                         "zoneid":vm_created.zoneid,
                         "account":vm_created.account,
                         "template":vm_created.templateid
                         }
        actual_dict = {
                       "id":stopped_vm.id,
                       "name":stopped_vm.name,
                       "displayname":stopped_vm.displayname,
                       "state":stopped_vm.state,
                       "zoneid":stopped_vm.zoneid,
                       "account":stopped_vm.account,
                       "template":stopped_vm.templateid
                       }
        stopped_vm_status = self.__verify_values(
                                                  expected_dict,
                                                  actual_dict
                                                  )
        self.assertEqual(
                         True,
                         stopped_vm_status,
                         "Listed Stopped VM details are not as expected"
                         )
        return

    @attr(tags=["advanced", "basic", "selfservice"])
    def test_04_list_Destroyed_vm(self):
        """
        @Desc: Test List Destroyed VM's
        @Steps:
        Step1: Listing all the Destroyed VMs for a user
        Step2: Verifying that the size of the list is 0
        Step3: Deploying a VM
        Step4: Destroyed the VM deployed in step3
        Step5: Listing all the Destroyed VMs for a user again
        Step6: Verifying that destroyed VM is not listed for User
        Step7: Listing all the destroyed VMs as admin
        Step8: Verifying that the size of the list is 1
        Step9: Verifying that the details of the Destroyed VM listed are same as the VM destroyed in Step4
        """
        # Listing all the Destroyed VM's for a User
        list_destroyed_vms_before = VirtualMachine.list(
                                                      self.userapiclient,
                                                      listall=self.services["listall"],
                                                      page=1,
                                                      pagesize=self.services["pagesize"],
                                                      domainid=self.account.domainid,
                                                      state="Destroyed"
                                                      )
        self.assertIsNone(
                           list_destroyed_vms_before,
                           "Virtual Machine in Destroyed state already exists for newly created user"
                           )
        # Deploying a VM
        vm_created = VirtualMachine.create(
                                           self.userapiclient,
                                           self.services["virtual_machine"],
                                           accountid=self.account.name,
                                           domainid=self.account.domainid,
                                           serviceofferingid=self.service_offering.id,
                                           )
        self.assertIsNotNone(
                             vm_created,
                             "VM creation failed"
                             )
        # Destroying the VM
        VirtualMachine.delete(vm_created, self.userapiclient)
        # Listing all the Destroyed VM's for a User
        list_destroyed_vms_after = VirtualMachine.list(
                                                      self.userapiclient,
                                                      listall=self.services["listall"],
                                                      page=1,
                                                      pagesize=self.services["pagesize"],
                                                      domainid=self.account.domainid,
                                                      state="Destroyed"
                                                      )
        self.assertIsNone(
                          list_destroyed_vms_after,
                          "Destroyed VM is not in destroyed state"
                          )
        # Listing destroyed VMs as admin user
        list_destroyed_vms_admin = VirtualMachine.list(
                                                       self.apiClient,
                                                       listall=self.services["listall"],
                                                       page=1,
                                                       pagesize=self.services["pagesize"],
                                                       domainid=self.account.domainid,
                                                       state="Destroyed",
                                                       id=vm_created.id
                                                       )
        status = validateList(list_destroyed_vms_admin)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Destroyed VM is not in Destroyed state"
                          )
        # Verifying that the length of the destroyed VMs list should be 1
        self.assertEquals(
                          1,
                          len(list_destroyed_vms_admin),
                          "Destroyed VM list count is not matching"
                          )
        destroyed_vm = list_destroyed_vms_admin[0]
        #Creating expected and actual values dictionaries
        expected_dict = {
                         "id":vm_created.id,
                         "name":vm_created.name,
                         "displayname":vm_created.displayname,
                         "state":"Destroyed",
                         "zoneid":vm_created.zoneid,
                         "account":vm_created.account,
                         "template":vm_created.templateid
                         }
        actual_dict = {
                       "id":destroyed_vm.id,
                       "name":destroyed_vm.name,
                       "displayname":destroyed_vm.displayname,
                       "state":destroyed_vm.state,
                       "zoneid":destroyed_vm.zoneid,
                       "account":destroyed_vm.account,
                       "template":destroyed_vm.templateid
                       }
        destroyed_vm_status = self.__verify_values(
                                                  expected_dict,
                                                  actual_dict
                                                  )
        self.assertEqual(
                         True,
                         destroyed_vm_status,
                         "Listed Destroyed VM details are not as expected"
                         )
        return

    @attr(tags=["advanced", "basic", "selfservice"])
    def test_05_list_vm_by_id(self):
        """
        @Desc: Test List VM by Id
        @Steps:
        Step1: Listing all the VMs for a user
        Step2: Verifying that the size of the list is 0
        Step3: Deploying a VM
        Step4: Listing all the VMs for a user again
        Step5: Verifying that the size of the list is increased by 1
        Step6: List a VM by specifying the Id if the VM deployed in Step3
        Step7: Verifying that the details of the Listed VM are same as the VM deployed in Step3
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              page=1,
                                              pagesize=self.services["pagesize"],
                                              domainid=self.account.domainid,
                                              account=self.account.name
                                              )
        self.assertIsNone(
                           list_vms_before,
                           "Virtual Machine already exists for newly created user"
                           )
        # Deploying a VM
        vm_created = VirtualMachine.create(
                                           self.userapiclient,
                                           self.services["virtual_machine"],
                                           accountid=self.account.name,
                                           domainid=self.account.domainid,
                                           serviceofferingid=self.service_offering.id,
                                           )
        self.assertIsNotNone(
                             vm_created,
                             "VM creation failed"
                             )
        self.cleanup.append(vm_created)
        # Listing all the VM's for a User
        list_vms_after = VirtualMachine.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             page=1,
                                             pagesize=self.services["pagesize"],
                                             domainid=self.account.domainid,
                                             account=self.account.name
                                             )
        status = validateList(list_vms_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of VM after creation failed"
                          )
        self.assertEquals(
                          1,
                          len(list_vms_after),
                          "VM list count is not matching"
                          )
        # Listing a VM by Id
        list_vm_byid = VirtualMachine.list(
                                           self.userapiclient,
                                           listall=self.services["listall"],
                                           id=vm_created.id
                                           )
        status = validateList(list_vm_byid)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of VM by Id failed"
                          )
        listed_vm = list_vm_byid[0]
        #Creating expected and actual values dictionaries
        expected_dict = {
                         "id":vm_created.id,
                         "name":vm_created.name,
                         "displayname":vm_created.displayname,
                         "state":vm_created.state,
                         "zoneid":vm_created.zoneid,
                         "account":vm_created.account,
                         "template":vm_created.templateid
                         }
        actual_dict = {
                       "id":listed_vm.id,
                       "name":listed_vm.name,
                       "displayname":listed_vm.displayname,
                       "state":listed_vm.state,
                       "zoneid":listed_vm.zoneid,
                       "account":listed_vm.account,
                       "template":listed_vm.templateid
                       }
        list_vm_status = self.__verify_values(
                                              expected_dict,
                                              actual_dict
                                              )
        self.assertEqual(
                         True,
                         list_vm_status,
                         "Listed VM by Id details are not as expected"
                         )
        return

    @attr(tags=["advanced", "basic", "selfservice"])
    def test_06_list_vm_by_name(self):
        """
        @Desc: Test List VM's by Name
        @Steps:
        Step1: Listing all the VMs for a user
        Step2: Verifying that the size of the list is 0
        Step3: Deploying a 2 VM's
        Step4: Listing all the VMs for a user again
        Step5: Verifying that list size is increased by 2
        Step6: Listing the VM by specifying complete name of VM-1 created in step3
        Step7: Verifying that the size of the list is 1
        Step8: Verifying that the details of the listed VM are same as the VM-1 created in step3
        Step9: Listing the VM by specifying the partial name of VM
        Step10: Verifying that the size of the list is 2
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              page=1,
                                              pagesize=self.services["pagesize"],
                                              domainid=self.account.domainid,
                                              )
        self.assertIsNone(
                           list_vms_before,
                           "Virtual Machine already exists for newly created user"
                           )
        vms = {}
        for i in range(0, 2):
            # Deploying a VM
            vm_created = VirtualMachine.create(
                                               self.userapiclient,
                                               self.services["virtual_machine"],
                                               accountid=self.account.name,
                                               domainid=self.account.domainid,
                                               serviceofferingid=self.service_offering.id,
                                               )
            self.assertIsNotNone(
                                 vm_created,
                                 "VM creation failed"
                                 )
            self.cleanup.append(vm_created)
            vms.update({i: vm_created})

        # Listing all the VM's for a User
        list_vms_after = VirtualMachine.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             page=1,
                                             pagesize=self.services["pagesize"],
                                             domainid=self.account.domainid,
                                             )
        status = validateList(list_vms_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VM's creation failed"
                          )
        self.assertEquals(
                          2,
                          len(list_vms_after),
                          "VM's list count is not matching"
                          )
        # Listing the VM by complete name
        list_vm_byfullname = VirtualMachine.list(
                                                 self.userapiclient,
                                                 listall=self.services["listall"],
                                                 page=1,
                                                 pagesize=self.services["pagesize"],
                                                 domainid=self.account.domainid,
                                                 name=vms[0].name
                                                 )
        status = validateList(list_vm_byfullname)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Failed to list VM by Name"
                          )
        # Verifying that the size of the list is 1
        self.assertEquals(
                          1,
                          len(list_vm_byfullname),
                          "VM list by full name count is not matching"
                          )
        # Verifying that the details of the listed VM are same as the VM created above
        #Creating expected and actual values dictionaries
        expected_dict = {
                         "id":vms[0].id,
                         "name":vms[0].name,
                         "displayname":vms[0].displayname,
                         "state":vms[0].state,
                         "zoneid":vms[0].zoneid,
                         "account":vms[0].account,
                         "template":vms[0].templateid
                         }
        actual_dict = {
                       "id":list_vm_byfullname[0].id,
                       "name":list_vm_byfullname[0].name,
                       "displayname":list_vm_byfullname[0].displayname,
                       "state":list_vm_byfullname[0].state,
                       "zoneid":list_vm_byfullname[0].zoneid,
                       "account":list_vm_byfullname[0].account,
                       "template":list_vm_byfullname[0].templateid
                       }
        list_vm_status = self.__verify_values(
                                              expected_dict,
                                              actual_dict
                                              )
        self.assertEqual(
                         True,
                         list_vm_status,
                         "Listed VM details are not as expected"
                         )
        # Listing the VM by partial name
        list_vm_bypartialname = VirtualMachine.list(
                                                 self.userapiclient,
                                                 listall=self.services["listall"],
                                                 domainid=self.account.domainid,
                                                 name=vms[0].name[:1]
                                                 )
        status = validateList(list_vm_bypartialname)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Failed to list VM by Name"
                          )
        # Verifying that the size of the list is 2
        self.assertEquals(
                          2,
                          len(list_vm_bypartialname),
                          "VM list by full name count is not matching"
                          )
        return

    @attr(tags=["advanced", "basic", "selfservice"])
    def test_07_list_vm_by_name_state(self):
        """  
        @Desc: Test List VM's by Name and State
        @Steps:
        Step1: Listing all the VMs for a user
        Step2: Verifying that the size of the list is 0
        Step3: Deploying a VM
        Step4: Listing all the VMs for a user again
        Step5: Verifying that list size is increased by 1
        Step6: Listing the VM by specifying name of VM created in step3 and state as Running (matching name and state)
        Step7: Verifying that the size of the list is 1
        Step8: Verifying that the details of the listed VM are same as the VM created in step3
        Step9: Listing the VM by specifying name of VM created in step3 and state as Stopped (non matching state)
        Step10: Verifying that the size of the list is 0
        Step11: Listing the VM by specifying non matching name and state as Running (non matching name)
        Step12: Verifying that the size of the list is 0
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              page=1,
                                              pagesize=self.services["pagesize"],
                                              domainid=self.account.domainid,
                                              )
        self.assertIsNone(
                           list_vms_before,
                           "Virtual Machine already exists for newly created user"
                           )
        # Deploying a VM
        vm_created = VirtualMachine.create(
                                           self.userapiclient,
                                           self.services["virtual_machine"],
                                           accountid=self.account.name,
                                           domainid=self.account.domainid,
                                           serviceofferingid=self.service_offering.id,
                                           )
        self.assertIsNotNone(
                             vm_created,
                             "VM creation failed"
                             )
        self.cleanup.append(vm_created)
        # Listing all the VM's for a User
        list_vms_after = VirtualMachine.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             page=1,
                                             pagesize=self.services["pagesize"],
                                             domainid=self.account.domainid,
                                             )
        status = validateList(list_vms_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VM's creation failed"
                          )
        self.assertEquals(
                          1,
                          len(list_vms_after),
                          "VM's list count is not matching"
                          )
        # Listing the VM by matching Name and State
        list_running_vm = VirtualMachine.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              page=1,
                                              pagesize=self.services["pagesize"],
                                              domainid=self.account.domainid,
                                              name=vm_created.name,
                                              state="Running"
                                              )
        status = validateList(list_running_vm)
        self.assertEquals(
                          PASS,
                          status[0],
                          "List VM by name and state failed"
                          )
        # Verifying that the size of the list is 1
        self.assertEquals(
                          1,
                          len(list_running_vm),
                          "Count of VM list by name and state is not matching"
                          )
        # Verifying that the details of the listed VM are same as the VM created above
        #Creating expected and actual values dictionaries
        expected_dict = {
                         "id":vm_created.id,
                         "name":vm_created.name,
                         "displayname":vm_created.displayname,
                         "state":"Running",
                         "zoneid":vm_created.zoneid,
                         "account":vm_created.account,
                         "template":vm_created.templateid
                         }
        actual_dict = {
                       "id":list_running_vm[0].id,
                       "name":list_running_vm[0].name,
                       "displayname":list_running_vm[0].displayname,
                       "state":list_running_vm[0].state,
                       "zoneid":list_running_vm[0].zoneid,
                       "account":list_running_vm[0].account,
                       "template":list_running_vm[0].templateid
                       }
        list_vm_status = self.__verify_values(
                                              expected_dict,
                                              actual_dict
                                              )
        self.assertEqual(
                         True,
                         list_vm_status,
                         "Listed VM details are not as expected"
                         )
        # Listing the VM by matching name and non matching state
        list_running_vm = VirtualMachine.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              page=1,
                                              pagesize=self.services["pagesize"],
                                              domainid=self.account.domainid,
                                              name=vm_created.name,
                                              state="Stopped"
                                              )
        self.assertIsNone(
                          list_running_vm,
                          "Listed VM with non matching state"
                          )
        # Listing the VM by non matching name and matching state
        list_running_vm = VirtualMachine.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              page=1,
                                              pagesize=self.services["pagesize"],
                                              domainid=self.account.domainid,
                                              name="name",
                                              state="Running"
                                              )
        self.assertIsNone(
                          list_running_vm,
                          "Listed VM with non matching name"
                          )
        return

    @attr(tags=["advanced", "basic", "selfservice"])
    def test_08_list_vm_by_zone(self):
        """  
        @Desc: Test List VM by Zone. 
        This test case is applicable for a setup having multiple zones.
        @Steps:
        Step1: Listing all the zones
        Step2: Checking if there are multiple zones in the setup.
               Continuing below steps only if there are multiple zones
        Step3: Listing template for zone
        Step4: Listing all the VMs for a user
        Step5: Verifying that the size of the list is 0
        Step6: Deploying a VM
        Step7: Listing all the VMs for a user again for matching zone
        Step8: Verifying that the size of the list is 1
        Step9: Verifying that the details of the Listed VM are same as the VM deployed in Step6
        Step10: Listing all the VMs for a user again for non-matching zone
        Step11: Verifying that the size of the list is 0
        """
        # Listing all the zones available
        zones_list = Zone.list(self.apiClient)
        status = validateList(zones_list)
        self.assertEquals(
                          PASS,
                          status[0],
                          "zones not available in the given setup"
                          )
        current_zone = self.services["virtual_machine"]["zoneid"]
        current_template = self.services["virtual_machine"]["template"]
        # Checking if there are multiple zones in the setup.
        if not len(zones_list) > 1:
            self.debug("Setup is not having multiple zones")
        else:
            # Getting the template available under the zone
            template = get_template(
                                    self.apiClient,
                                    zones_list[0].id,
                                    self.services["ostype"]
                                    )
            self.assertIsNotNone(
                                 template,
                                 "Template not found for zone"
                                 )
            self.services["virtual_machine"]["zoneid"] = zones_list[0].id
            self.services["virtual_machine"]["template"] = template.id
            # Listing all the VM's for a User
            list_vms_before = VirtualMachine.list(
                                                  self.userapiclient,
                                                  listall=self.services["listall"],
                                                  page=1,
                                                  pagesize=self.services["pagesize"],
                                                  domainid=self.account.domainid,
                                                  zoneid=zones_list[0].id
                                                  )
            self.assertIsNone(
                               list_vms_before,
                               "Virtual Machine already exists for newly created user"
                               )
            # Deploying a VM
            vm_created = VirtualMachine.create(
                                               self.userapiclient,
                                               self.services["virtual_machine"],
                                               accountid=self.account.name,
                                               domainid=self.account.domainid,
                                               serviceofferingid=self.service_offering.id,
                                               )
            self.assertIsNotNone(
                                 vm_created,
                                 "VM creation failed"
                                 )
            self.cleanup.append(vm_created)
            # Listing all the VMs for a user again for matching zone
            list_vms_after = VirtualMachine.list(
                                                 self.userapiclient,
                                                 listall=self.services["listall"],
                                                 page=1,
                                                 pagesize=self.services["pagesize"],
                                                 domainid=self.account.domainid,
                                                 zoneid=zones_list[0].id
                                                 )
            status = validateList(list_vms_after)
            self.assertEquals(
                              PASS,
                              status[0],
                              "VM creation failed"
                              )
            # Verifying that the size of the list is 1
            self.assertEquals(
                              1,
                              len(list_vms_after),
                              "VM list count is not matching"
                              )
            listed_vm = list_vms_after[0]
            # Verifying that the details of the Listed VM are same as the VM deployed above
            #Creating expected and actual values dictionaries
            expected_dict = {
                               "id":vm_created.id,
                               "name":vm_created.name,
                               "displayname":vm_created.displayname,
                               "state":vm_created.state,
                               "zoneid":vm_created.zoneid,
                               "account":vm_created.account,
                               "template":vm_created.templateid
                               }
            actual_dict = {
                               "id":listed_vm.id,
                               "name":listed_vm.name,
                               "displayname":listed_vm.displayname,
                               "state":listed_vm.state,
                               "zoneid":listed_vm.zoneid,
                               "account":listed_vm.account,
                               "template":listed_vm.templateid
                               }
            list_vm_status = self.__verify_values(
                                                  expected_dict,
                                                  actual_dict
                                                  )
            self.assertEqual(
                             True,
                             list_vm_status,
                             "Listed VM by Id details are not as expected"
                             )
            # Listing all the VMs for a user again for non-matching zone
            list_vms = VirtualMachine.list(
                                           self.userapiclient,
                                           listall=self.services["listall"],
                                           page=1,
                                           pagesize=self.services["pagesize"],
                                           domainid=self.account.domainid,
                                           zoneid=zones_list[1].id
                                           )
            self.assertIsNone(
                              list_vms,
                              "VM's listed for non matching zone"
                              )
            self.services["virtual_machine"]["zoneid"] = current_zone
            self.services["virtual_machine"]["template"] = current_template
        return

    @attr(tags=["advanced", "basic", "selfservice"])
    def test_09_list_vm_by_zone_name(self):
        """  
        @Desc: Test List VM by Zone. 
        This test case is applicable for a setup having multiple zones.
        @Steps:
        Step1: Listing all the zones
        Step2: Checking if there are multiple zones in the setup.
               Continuing below steps only if there are multiple zones
        Step3: Listing template for zone
        Step4: Listing all the VMs for a user
        Step5: Verifying that the size of the list is 0
        Step6: Deploying a VM
        Step7: Listing all the VMs for a user again
        Step8: Verifying that list size is increased by 1
        Step9: Listing the VM by specifying name of VM created in step6 and matching zone (matching name and zone)
        Step10: Verifying that the size of the list is 1
        Step11: Verifying that the details of the listed VM are same as the VM created in step3
        Step12: Listing the VM by specifying name of VM created in step6 and non matching zone (non matching zone)
        Step13: Verifying that the size of the list is 0
        Step14: Listing the VM by specifying non matching name and matching zone (non matching name)
        Step15: Verifying that the size of the list is 0
        """
        # Listing all the zones available
        zones_list = Zone.list(self.apiClient)
        status = validateList(zones_list)
        self.assertEquals(
                          PASS,
                          status[0],
                          "zones not available in the given setup"
                          )
        current_zone = self.services["virtual_machine"]["zoneid"]
        current_template = self.services["virtual_machine"]["template"]
        # Checking if there are multiple zones in the setup.
        if not len(zones_list) > 1:
            self.debug("Setup is not having multiple Zones")
        else:
            # Getting the template available under the zone
            template = get_template(
                                    self.apiClient,
                                    zones_list[0].id,
                                    self.services["ostype"]
                                    )
            self.assertIsNotNone(
                                 template,
                                 "Template not found for zone"
                                 )
            self.services["virtual_machine"]["zoneid"] = zones_list[0].id
            self.services["virtual_machine"]["template"] = template.id
            # Listing all the VM's for a User
            list_vms_before = VirtualMachine.list(
                                                  self.userapiclient,
                                                  listall=self.services["listall"],
                                                  page=1,
                                                  pagesize=self.services["pagesize"],
                                                  domainid=self.account.domainid,
                                                  zoneid=zones_list[0].id,
                                                  account=self.account.name
                                                  )
            self.assertIsNone(
                               list_vms_before,
                               "Virtual Machine already exists for newly created user"
                               )
            # Deploying a VM
            vm_created = VirtualMachine.create(
                                               self.userapiclient,
                                               self.services["virtual_machine"],
                                               accountid=self.account.name,
                                               domainid=self.account.domainid,
                                               serviceofferingid=self.service_offering.id,
                                               )
            self.assertIsNotNone(
                                 vm_created,
                                 "VM creation failed"
                                 )
            self.cleanup.append(vm_created)
            # Listing all the VMs for a user again for matching zone
            list_vms_after = VirtualMachine.list(
                                                 self.userapiclient,
                                                 listall=self.services["listall"],
                                                 page=1,
                                                 pagesize=self.services["pagesize"],
                                                 domainid=self.account.domainid,
                                                 zoneid=zones_list[0].id,
                                                 account=self.account.name
                                                 )
            status = validateList(list_vms_after)
            self.assertEquals(
                              PASS,
                              status[0],
                              "VM creation failed"
                              )
            # Verifying that the size of the list is 1
            self.assertEquals(
                              1,
                              len(list_vms_after),
                              "VM list count is not matching"
                              )
            # Listing the VM by specifying name of VM created in above and matching zone
            list_vms = VirtualMachine.list(
                                           self.userapiclient,
                                           listall=self.services["listall"],
                                           page=1,
                                           pagesize=self.services["pagesize"],
                                           domainid=self.account.domainid,
                                           zoneid=zones_list[0].id,
                                           name=vm_created.name
                                           )
            status = validateList(list_vms)
            self.assertEquals(
                              PASS,
                              status[0],
                              "Listing VM's by name and zone failed"
                              )
            # Verifying Verifying that the size of the list is 1
            self.assertEquals(
                              1,
                              len(list_vms),
                              "Count of listed VM's by name and zone is not as expected"
                              )
            listed_vm = list_vms[0]
            # Verifying that the details of the Listed VM are same as the VM deployed above
            #Creating expected and actual values dictionaries
            expected_dict = {
                             "id":vm_created.id,
                             "name":vm_created.name,
                             "displayname":vm_created.displayname,
                             "state":vm_created.state,
                             "zoneid":vm_created.zoneid,
                             "account":vm_created.account,
                             "template":vm_created.templateid
                               }
            actual_dict = {
                               "id":listed_vm.id,
                               "name":listed_vm.name,
                               "displayname":listed_vm.displayname,
                               "state":listed_vm.state,
                               "zoneid":listed_vm.zoneid,
                               "account":listed_vm.account,
                               "template":listed_vm.templateid
                               }
            list_vm_status = self.__verify_values(
                                                  expected_dict,
                                                  actual_dict
                                                  )
            self.assertEqual(
                             True,
                             list_vm_status,
                             "Listed VM by Id details are not as expected"
                             )
            # Listing the VM by specifying name of VM created in step3 and non matching zone
            list_vms = VirtualMachine.list(
                                           self.userapiclient,
                                           listall=self.services["listall"],
                                           page=1,
                                           pagesize=self.services["pagesize"],
                                           domainid=self.account.domainid,
                                           zoneid=zones_list[1].id,
                                           name=vm_created.name
                                           )
            self.assertIsNone(
                              list_vms,
                              "VM's listed for non matching zone"
                              )
            # Listing the VM by specifying non matching name of VM and matching zone
            list_vms = VirtualMachine.list(
                                           self.userapiclient,
                                           listall=self.services["listall"],
                                           page=1,
                                           pagesize=self.services["pagesize"],
                                           domainid=self.account.domainid,
                                           zoneid=zones_list[0].id,
                                           name="name"
                                           )
            self.assertIsNone(
                              list_vms,
                              "VM's listed for non matching zone"
                              )
            self.services["virtual_machine"]["zoneid"] = current_zone
            self.services["virtual_machine"]["template"] = current_template
        return

    @attr(tags=["advanced", "basic", "selfservice"])
    def test_10_list_vm_by_zone_name_state(self):
        """  
        @Desc: Test List VM by Zone. 
        @Steps:
        Step1: Listing all the VMs for a user
        Step2: Verifying that the size of the list is 0
        Step3: Deploying a VM
        Step4: Listing all the VMs for a user again
        Step5: Verifying that list size is increased by 1
        Step6: Listing the VM by specifying name of VM created in step3 and matching zone and state as Running
        Step7: Verifying that the size of the list is 1
        Step8: Verifying that the details of the listed VM are same as the VM created in step3
        Step9: Listing the VM by specifying name of VM created in step3 and matching zone and state as Stopped
        Step10: Verifying that the size of the list is 0
        Step11: Listing the VM by name, Zone and account
        Step12: Verifying that the size of the list is 1
        Step13: Verifying that the details of the listed VM are same as the VM created in step3
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              page=1,
                                              pagesize=self.services["pagesize"],
                                              domainid=self.account.domainid,
                                              zoneid=self.zone.id,
                                              account=self.account.name
                                              )
        self.assertIsNone(
                           list_vms_before,
                           "Virtual Machine already exists for newly created user"
                           )
        # Deploying a VM
        vm_created = VirtualMachine.create(
                                           self.userapiclient,
                                           self.services["virtual_machine"],
                                           accountid=self.account.name,
                                           domainid=self.account.domainid,
                                           serviceofferingid=self.service_offering.id,
                                           )
        self.assertIsNotNone(
                             vm_created,
                             "VM creation failed"
                             )
        self.cleanup.append(vm_created)
        # Listing all the VMs for a user again for matching zone
        list_vms_after = VirtualMachine.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             page=1,
                                             pagesize=self.services["pagesize"],
                                             domainid=self.account.domainid,
                                             zoneid=self.zone.id,
                                             account=self.account.name
                                             )
        status = validateList(list_vms_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VM creation failed"
                          )
        # Verifying that the size of the list is 1
        self.assertEquals(
                          1,
                          len(list_vms_after),
                          "VM list count is not matching"
                          )
        # Listing the VM by specifying name of VM created in step3 and matching zone and state as Running
        list_vms = VirtualMachine.list(
                                       self.userapiclient,
                                       listall=self.services["listall"],
                                       page=1,
                                       pagesize=self.services["pagesize"],
                                       domainid=self.account.domainid,
                                       zoneid=self.zone.id,
                                       name=vm_created.name,
                                       state="Running"
                                       )
        status = validateList(list_vms)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing VM's by name and zone failed"
                          )
        # Verifying Verifying that the size of the list is 1
        self.assertEquals(
                          1,
                          len(list_vms),
                          "Count of listed VM's by name, zone and state is not as expected"
                          )
        listed_vm = list_vms[0]
        # Verifying that the details of the Listed VM are same as the VM deployed above
        #Creating expected and actual values dictionaries
        expected_dict = {
                         "id":vm_created.id,
                         "name":vm_created.name,
                         "displayname":vm_created.displayname,
                         "state":vm_created.state,
                         "zoneid":vm_created.zoneid,
                         "account":vm_created.account,
                         "template":vm_created.templateid
                       }
        actual_dict = {
                       "id":listed_vm.id,
                       "name":listed_vm.name,
                       "displayname":listed_vm.displayname,
                       "state":listed_vm.state,
                       "zoneid":listed_vm.zoneid,
                       "account":listed_vm.account,
                       "template":listed_vm.templateid
                       }
        list_vm_status = self.__verify_values(
                                              expected_dict,
                                              actual_dict
                                              )
        self.assertEqual(
                         True,
                         list_vm_status,
                         "Listed VM by Id details are not as expected"
                         )
        # Listing the VM by specifying name of VM created in step3, zone and State as Stopped
        list_vms = VirtualMachine.list(
                                       self.userapiclient,
                                       listall=self.services["listall"],
                                       page=1,
                                       pagesize=self.services["pagesize"],
                                       domainid=self.account.domainid,
                                       zoneid=self.zone.id,
                                       name=vm_created.name,
                                       state="Stopped"
                                       )
        self.assertIsNone(
                          list_vms,
                          "VM's listed for non matching zone"
                          )
        # Listing the VM by name, zone and account
        list_vms = VirtualMachine.list(
                                       self.userapiclient,
                                       listall=self.services["listall"],
                                       page=1,
                                       pagesize=self.services["pagesize"],
                                       domainid=self.account.domainid,
                                       zoneid=self.zone.id,
                                       name=vm_created.name,
                                       account=self.account.name
                                       )
        status = validateList(list_vms)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing VM's by name, account and zone failed"
                          )
        # Verifying Verifying that the size of the list is 1
        self.assertEquals(
                          1,
                          len(list_vms),
                          "Count of listed VM's by name, zone and account is not as expected"
                          )
        listed_vm = list_vms[0]
        # Verifying that the details of the Listed VM are same as the VM deployed above
        #Creating expected and actual values dictionaries
        expected_dict = {
                         "id":vm_created.id,
                         "name":vm_created.name,
                         "displayname":vm_created.displayname,
                         "state":vm_created.state,
                         "zoneid":vm_created.zoneid,
                         "account":vm_created.account,
                         "template":vm_created.templateid
                         }
        actual_dict = {
                       "id":listed_vm.id,
                       "name":listed_vm.name,
                       "displayname":listed_vm.displayname,
                       "state":listed_vm.state,
                       "zoneid":listed_vm.zoneid,
                       "account":listed_vm.account,
                       "template":listed_vm.templateid
                       }
        list_vm_status = self.__verify_values(
                                              expected_dict,
                                              actual_dict
                                              )
        self.assertEqual(
                         True,
                         list_vm_status,
                         "Listed VM by Id details are not as expected"
                         )
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_11_register_reset_vm_sshkey(self):
        """  
        @Desc: Test to verify registering and reset of SSH Key for VM
        @Steps:
        Step1: Deploying a VM
        Step2: Stopping the VM deployed in step1
        Step3: Listing all the SSH Key pairs
        Step4: Registering a SSH Key pair
        Step5: Listing all the SSh Key pairs again
        Step6: Verifying that the key pairs list is increased by 1
        Step7: Resetting the VM SSH Key to the key pair registered in step4
        Step8: Verifying that the registered SSH Key pair is set to the VM
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              )
        self.assertIsNone(
                           list_vms_before,
                           "Virtual Machine already exists for newly created user"
                           )
        # Deploying a VM
        vm_created = VirtualMachine.create(
                                           self.userapiclient,
                                           self.services["virtual_machine"],
                                           accountid=self.account.name,
                                           domainid=self.account.domainid,
                                           serviceofferingid=self.service_offering.id,
                                           )
        self.assertIsNotNone(
                             vm_created,
                             "VM creation failed"
                             )
        self.cleanup.append(vm_created)
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             )
        status = validateList(list_vms_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VM creation failed"
                          )
        # Verifying that the size of the list is 1
        self.assertEquals(
                          1,
                          len(list_vms_after),
                          "VM list count is not matching"
                          )
        # Stopping the VM deployed above
        vm_created.stop(
                        self.userapiclient,
                        forced=True
                        )
        # Listing all the SSH Key pairs
        list_keypairs_before = SSHKeyPair.list(
                                               self.userapiclient
                                               )
        list_keypairs_before_size = 0
        if list_keypairs_before is not None:
            list_keypairs_before_size = len(list_keypairs_before)

        # Registering new Key pair
        new_keypair = SSHKeyPair.register(
                                          self.userapiclient,
                                          name="keypair1",
                                          publickey="ssh-rsa: e6:9a:1e:b5:98:75:88:5d:56:bc:92:7b:43:48:05:b2"
                                          )
        self.assertIsNotNone(
                             new_keypair,
                             "New Key pair generation failed"
                             )
        self.assertEquals(
                          "keypair1",
                          new_keypair.name,
                          "Key Pair not created with given name"
                          )
        # Listing all the SSH Key pairs again
        list_keypairs_after = SSHKeyPair.list(
                                              self.userapiclient
                                              )
        status = validateList(list_keypairs_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of Key pairs failed"
                          )
        # Verifying that list size is increased by 1
        self.assertEquals(
                          list_keypairs_before_size + 1,
                          len(list_keypairs_after),
                          "List count is not matching"
                          )
        # Resetting the VM SSH key to the Key pair created above
        vm_created.resetSshKey(
                               self.userapiclient,
                               keypair=new_keypair.name
                               )
        # Listing VM details again
        list_vm = VirtualMachine.list(
                                      self.userapiclient,
                                      id=vm_created.id
                                     )
        status = validateList(list_vm)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of VM failed"
                          )
        self.assertEquals(
                          1,
                          len(list_vm),
                          "VMs list is not as expected"
                          )
        # Verifying that VM's SSH keypair is set to newly created keypair
        self.assertEquals(
                          new_keypair.name,
                          list_vm[0].keypair,
                          "VM is not set to newly created SSH Key pair"
                          )
        return

    @attr(tags=["advanced", "provisioning"])
    def test_12_vm_nics(self):
        """
        @Desc: Test to verify Nics for a VM
        @Steps:
        Step1: Deploying a VM
        Step2: Listing all the Networks
        Step3: Verifying that the list size is 1
        Step4: Creating 1 network
        Step5: Listing all the networks again
        Step6: Verifying that the list size is 2
        Step7: Verifying that VM deployed in step1 has only 1 nic
                and it is same as network listed in step3
        Step8: Adding the networks created in step4 to VM deployed in step1
        Step9: Verifying that VM deployed in step1 has 2 nics
        Step10: Verifying that isdefault is set to true for only 1 nic
        Step11: Verifying that isdefault is set to true for the Network created when deployed a VM
        Step12: Making the nic created in step4 as default nic
        Step13: Verifying that isdefault is set to true for only 1 nic
        Step14: Verifying that the isdefault is set to true for the nic created in step4
        Step15: Removing the non-default nic from VM
        Step16: Verifying that VM deployed in step1 has only 1 nic
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              )
        self.assertIsNone(
                           list_vms_before,
                           "Virtual Machine already exists for newly created user"
                           )
        # Deploying a VM
        vm_created = VirtualMachine.create(
                                           self.userapiclient,
                                           self.services["virtual_machine"],
                                           accountid=self.account.name,
                                           domainid=self.account.domainid,
                                           serviceofferingid=self.service_offering.id,
                                           )
        self.assertIsNotNone(
                             vm_created,
                             "VM creation failed"
                             )
        self.cleanup.append(vm_created)
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             )
        status = validateList(list_vms_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VM creation failed"
                          )
        # Verifying that the size of the list is 1
        self.assertEquals(
                          1,
                          len(list_vms_after),
                          "VM list count is not matching"
                          )
        # Listing all the networks before
        list_network_before = Network.list(
                                           self.userapiclient,
                                           isdefault="true",
                                           zoneid=self.zone.id,
                                           account=self.account.name,
                                           domainid=self.domain.id
                                           )
        status = validateList(list_network_before)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Default Network not created when deploying a VM"
                          )
        # Verifying that only 1 network is created while deploying a VM
        self.assertEquals(
                          1,
                          len(list_network_before),
                          "More than 1 default network exists"
                          )
        network1 = list_network_before[0]
        # Listing Network Offerings
        network_offerings_list = NetworkOffering.list(
                                                      self.apiClient,
                                                      forvpc="false",
                                                      guestiptype="Isolated",
                                                      state="Enabled",
                                                      supportedservices="SourceNat",
                                                      zoneid=self.zone.id
                                                      )
        self.assertIsNotNone(
                             network_offerings_list,
                             "Isolated Network Offerings with sourceNat enabled are not found"
                             )
        # Creating one more network
        network2 = Network.create(
                                  self.userapiclient,
                                  self.services["network"],
                                  accountid=self.account.name,
                                  domainid=self.domain.id,
                                  networkofferingid=network_offerings_list[0].id,
                                  zoneid=self.zone.id
                                  )
        self.assertIsNotNone(
                             network2,
                             "Network creation failed"
                             )
        self.cleanup.append(network2)
        # Listing all the networks again
        list_network_after = Network.list(
                                          self.userapiclient,
                                          zoneid=self.zone.id,
                                          account=self.account.name,
                                          domainid=self.domain.id
                                          )
        status = validateList(list_network_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "List of Networks failed"
                          )
        # Verifying that list size is 2
        self.assertEquals(
                          2,
                          len(list_network_after),
                          "More than 1 default network exists"
                          )
        # Verifying that VM created is having only 1 nic
        vm_nics_before = vm_created.nic
        self.assertIsNotNone(
                             vm_nics_before,
                             "Nic not found for the VM deployed"
                             )
        self.assertEquals(
                          1,
                          len(vm_nics_before),
                          "VM Nic count is not matching"
                          )
        # Verifying that the nic is same as the default network listed above
        self.assertEquals(
                          network1.id,
                          vm_nics_before[0].networkid,
                          "Default NIC for VM is not as expected"
                          )
        # Adding network2 created above to VM
        VirtualMachine.add_nic(
                               vm_created,
                               self.userapiclient,
                               network2.id
                               )
        # Listing the Vm details again
        list_vms_after = VirtualMachine.list(
                                             self.userapiclient,
                                             id=vm_created.id
                                             )
        status = validateList(list_vms_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of VM failed"
                          )
        vm = list_vms_after[0]
        # Verifying that VM nics size is 2 now
        vm_nics_after = vm.nic
        self.assertIsNotNone(
                             vm_nics_after,
                             "Nic not found for the deployed VM"
                             )
        self.assertEquals(
                          2,
                          len(vm_nics_after),
                          "VM NIC's count is not matching"
                          )
        # Verifying that isdefault is set to true for only 1 nic
        default_count = 0
        for i in range(0, len(vm_nics_after)):
            if vm_nics_after[i].isdefault is True:
                default_count = default_count + 1
                default_nic = vm_nics_after[i]
            else:
                non_default_nic = vm_nics_after[i]
        self.assertEquals(
                          1,
                          default_count,
                          "Default NIC count is not matching"
                          )
        # Verifying that default NIC is same the network created when VM is deployed
        self.assertEquals(
                          network1.id,
                          default_nic.networkid,
                          "Default NIC is not matching for VM"
                          )
        # Updating network 2 as default NIC
        vm_created.update_default_nic(
                                      self.userapiclient,
                                      non_default_nic.id
                                      )
        # Listing the Vm details again
        list_vms_after = VirtualMachine.list(
                                             self.userapiclient,
                                             id=vm_created.id
                                             )
        status = validateList(list_vms_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of VM failed"
                          )
        vm = list_vms_after[0]
        # Verifying that VM nics size is 2 now
        vm_nics_after = vm.nic
        self.assertIsNotNone(
                             vm_nics_after,
                             "Nic not found for the deployed VM"
                             )
        self.assertEquals(
                          2,
                          len(vm_nics_after),
                          "VM NIC's count is not matching"
                          )
        # Verifying that isdefault is set to true for only 1 nic
        default_count = 0
        for i in range(0, len(vm_nics_after)):
            if vm_nics_after[i].isdefault is True:
                default_count = default_count + 1
                default_nic = vm_nics_after[i]
            else:
                non_default_nic = vm_nics_after[i]

        self.assertEquals(
                          1,
                          default_count,
                          "Default NIC count is not matching"
                          )
        # Verifying that default NIC is same the newly updated network (network 2)
        self.assertEquals(
                          network2.id,
                          default_nic.networkid,
                          "Default NIC is not matching for VM"
                          )
        # Deleting non default NIC
        vm_created.remove_nic(
                              self.userapiclient,
                              non_default_nic.id
                              )
        # Listing the Vm details again
        list_vms_after = VirtualMachine.list(
                                             self.userapiclient,
                                             id=vm_created.id
                                             )
        status = validateList(list_vms_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of VM failed"
                          )
        vm = list_vms_after[0]
        # Verifying that VM nics size is 1 now
        vm_nics_after = vm.nic
        self.assertIsNotNone(
                             vm_nics_after,
                             "Nic not found for the deployed VM"
                             )
        self.assertEquals(
                          1,
                          len(vm_nics_after),
                          "VM NIC's count is not matching"
                          )
        # Verifying the nic network is same as the default nic network
        self.assertEquals(
                          network2.id,
                          vm_nics_after[0].networkid,
                          "VM NIC is not same as expected"
                          )
        return

class TestInstances(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(TestInstances, cls).getClsTestClient()
            cls.api_client = cls.testClient.getApiClient()
            cls.services = cls.testClient.getParsedTestDataConfig()

            # Get Domain, Zone, Template
            cls.domain = get_domain(cls.api_client)
            cls.zone = get_zone(cls.api_client)
            cls.template = get_template(
                                        cls.api_client,
                                        cls.zone.id,
                                        cls.services["ostype"]
                                        )
            if cls.zone.localstorageenabled:
                cls.storagetype = 'local'
                cls.services["service_offerings"]["tiny"]["storagetype"] = 'local'
                cls.services["disk_offering"]["storagetype"] = 'local'
            else:
                cls.storagetype = 'shared'
                cls.services["service_offerings"]["tiny"]["storagetype"] = 'shared'
                cls.services["disk_offering"]["storagetype"] = 'shared'

            cls.services['mode'] = cls.zone.networktype
            cls.services["virtual_machine"]["hypervisor"] = cls.testClient.getHypervisorInfo()
            cls.services["virtual_machine"]["zoneid"] = cls.zone.id
            cls.services["virtual_machine"]["template"] = cls.template.id
            cls.services["custom_volume"]["zoneid"] = cls.zone.id

            # Creating Disk offering, Service Offering and Account
            cls.disk_offering = DiskOffering.create(
                                                    cls.api_client,
                                                    cls.services["disk_offering"]
                                                    )
            cls._cleanup.append(cls.disk_offering)
            cls.service_offering = ServiceOffering.create(
                                                          cls.api_client,
                                                          cls.services["service_offerings"]["tiny"]
                                                          )
            cls._cleanup.append(cls.service_offering)
            cls.account = Account.create(
                                         cls.api_client,
                                         cls.services["account"],
                                         domainid=cls.domain.id
                                         )
            # Getting authentication for user in newly created Account
            cls.user = cls.account.user[0]
            cls.userapiclient = cls.testClient.getUserApiClient(cls.user.username, cls.domain.name)
            # Updating resource Limits
            for i in range(0,12):
                Resources.updateLimit(
                                      cls.api_client,
                                      account=cls.account.name,
                                      domainid=cls.domain.id,
                                      max=-1,
                                      resourcetype=i
                                      )
            cls._cleanup.append(cls.account)
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setup : %s" % e)
        return

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        #Clean up, terminate the created resources
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def __verify_values(self, expected_vals, actual_vals):
        """
        @Desc: Function to verify expected and actual values
        @Steps:
        Step1: Initializing return flag to True
        Step1: Verifying length of expected and actual dictionaries is matching.
               If not matching returning false
        Step2: Listing all the keys from expected dictionary
        Step3: Looping through each key from step2 and verifying expected and actual dictionaries have same value
               If not making return flag to False
        Step4: returning the return flag after all the values are verified
        """
        return_flag = True

        if len(expected_vals) != len(actual_vals):
            return False

        keys = expected_vals.keys()
        for i in range(0, len(expected_vals)):
            exp_val = expected_vals[keys[i]]
            act_val = actual_vals[keys[i]]
            if exp_val == act_val:
                return_flag = return_flag and True
            else:
                return_flag = return_flag and False
                self.debug("expected Value: %s, is not matching with actual value: %s" % (
                                                                                          exp_val,
                                                                                          act_val
                                                                                          ))
        return return_flag

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_13_attach_detach_iso(self):
        """
        @Desc: Test Attach ISO to VM and Detach ISO from VM.
        @Steps:
        Step1: Listing all the VMs for a user
        Step2: Verifying that the size of the list is 0
        Step3: Deploying a VM
        Step4: Listing all the VMs for a user again
        Step5: Verifying that list size is increased by 1
        Step6: Listing all the ready ISO's
        Step7: If size of the list is >= 1 continuing to next steps
        Step8: Attaching the ISO listed to VM deployed in Step3
        Step9: Verifying that the attached ISO details are associated with VM
        Step10: Detaching the ISO attached in step8
        Step11: Verifying that detached ISO details are not associated with VM
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              page=1,
                                              pagesize=self.services["pagesize"],
                                              domainid=self.account.domainid,
                                              zoneid=self.zone.id,
                                              account=self.account.name
                                              )
        self.assertIsNone(
                           list_vms_before,
                           "Virtual Machine already exists for newly created user"
                           )
        # Deploying a VM
        vm_created = VirtualMachine.create(
                                           self.userapiclient,
                                           self.services["virtual_machine"],
                                           accountid=self.account.name,
                                           domainid=self.account.domainid,
                                           serviceofferingid=self.service_offering.id,
                                           )
        self.assertIsNotNone(
                             vm_created,
                             "VM creation failed"
                             )
        self.cleanup.append(vm_created)
        # Listing all the VMs for a user again for matching zone
        list_vms_after = VirtualMachine.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             page=1,
                                             pagesize=self.services["pagesize"],
                                             domainid=self.account.domainid,
                                             zoneid=self.zone.id,
                                             account=self.account.name
                                             )
        status = validateList(list_vms_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VM creation failed"
                          )
        # Verifying that the size of the list is 1
        self.assertEquals(
                          1,
                          len(list_vms_after),
                          "VM list count is not matching"
                          )
        # Listing the ISO's in ready state
        isos_list = Iso.list(
                             self.userapiclient,
                             isready="true",
                             isofilter="executable",
                             zoneid=self.zone.id
                             )
        # Verifying if size of the list is >= 1
        if isos_list is not None:
            iso_toattach = isos_list[0]
            # Attaching ISO listed to VM deployed
            VirtualMachine.attach_iso(
                                      vm_created,
                                      self.userapiclient,
                                      iso_toattach
                                      )
            list_vm = VirtualMachine.list(
                                          self.userapiclient,
                                          id=vm_created.id
                                          )
            status = validateList(list_vm)
            self.assertEquals(
                              PASS,
                              status[0],
                              "VM listing by Id failed"
                              )
            # Verifying that attached ISO details are present in VM
            self.assertEquals(
                              iso_toattach.name,
                              list_vm[0].isoname,
                              "Attached ISO name is not matching"
                              )
            self.assertEquals(
                              iso_toattach.displaytext,
                              list_vm[0].isodisplaytext,
                              "Attached ISO display is not matching"
                              )
            # Detaching ISO from VM
            VirtualMachine.detach_iso(
                                      vm_created,
                                      self.userapiclient
                                      )
            list_vm = VirtualMachine.list(
                                          self.userapiclient,
                                          id=vm_created.id
                                          )
            status = validateList(list_vm)
            self.assertEquals(
                              PASS,
                              status[0],
                              "VM listing by Id failed"
                              )
            # Verifying that ISO details are NOT present in VM
            self.assertIsNone(
                              list_vm[0].isoname,
                              "ISO not detached from VM"
                              )
        else:
            self.fail("Executable ISO in Ready is not found in the given setup")

        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_14_vm_snapshot_pagination(self):
        """
        @Desc: Test VM Snapshots pagination.
        @Steps:
        Step1: Deploying a VM
        Step2: Listing all the Snapshots of the VM deployed in Step 1
        Step3: Verifying that the list size is 0
        Step4: Creating (pagesize + 1) number of Snapshots for the VM
        Step5: Listing all the Snapshots of the VM deployed in Step 1
        Step6: Verifying that the list size is (pagesize + 1)
        Step7: Listing all the VM snapshots in Page 1 with page size
        Step8: Verifying that size of the list is same as page size
        Step9: Listing all the VM snapshots in Page 2 with page size
        Step10: Verifying that size of the list is 1
        Step11: Deleting VM snapshot in page 2
        Step12: Listing all the VM snapshots in Page 2 with page size
        Step13: Verifying that size of the list is 0
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              )
        self.assertIsNone(
                           list_vms_before,
                           "Virtual Machine already exists for newly created user"
                           )
        # Deploying a VM
        vm_created = VirtualMachine.create(
                                           self.userapiclient,
                                           self.services["virtual_machine"],
                                           accountid=self.account.name,
                                           domainid=self.account.domainid,
                                           serviceofferingid=self.service_offering.id,
                                           )
        self.assertIsNotNone(
                             vm_created,
                             "VM creation failed"
                             )
        self.cleanup.append(vm_created)
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             )
        status = validateList(list_vms_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VM creation failed"
                          )
        # Verifying that the size of the list is 1
        self.assertEquals(
                          1,
                          len(list_vms_after),
                          "VM list count is not matching"
                          )
        # Listing all the VM snapshots for VM deployed above
        list_snapshots_before = VmSnapshot.list(
                                                self.userapiclient,
                                                listall=self.services["listall"],
                                                virtualmachineid=vm_created.id
                                                )
        # Verifying that the VM snapshot list is None
        self.assertIsNone(
                          list_snapshots_before,
                          "Snapshots already exists for newly created VM"
                          )
        # Creating pagesize + 1 number of VM snapshots
        for i in range(0, (self.services["pagesize"] + 1)):
            snapshot_created = VmSnapshot.create(
                                                 self.userapiclient,
                                                 vm_created.id,
                                                 )
            self.assertIsNotNone(
                                 snapshot_created,
                                 "Snapshot creation failed"
                                 )

        # Listing all the VM snapshots for VM again
        list_snapshots_after = VmSnapshot.list(
                                                self.userapiclient,
                                                listall=self.services["listall"],
                                                virtualmachineid=vm_created.id
                                                )
        status = validateList(list_snapshots_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VM Snapshots creation failed"
                          )
        self.assertEquals(
                          self.services["pagesize"] + 1,
                          len(list_snapshots_after),
                          "Count of VM Snapshots is not matching"
                          )
        # Listing all the VM snapshots in Page 1 with page size
        list_snapshots_page1 = VmSnapshot.list(
                                               self.userapiclient,
                                               listall=self.services["listall"],
                                               virtualmachineid=vm_created.id,
                                               page=1,
                                               pagesize=self.services["pagesize"],
                                               )
        status = validateList(list_snapshots_page1)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of VM Snapshots failed in page 1"
                          )
        # Verifying the list size is equal to pagesize
        self.assertEquals(
                          self.services["pagesize"],
                          len(list_snapshots_page1),
                          "List VM Snapshot count is not matching in page 1"
                          )
        # Listing all the VM Snapshots in page 2
        list_snapshots_page2 = VmSnapshot.list(
                                               self.userapiclient,
                                               listall=self.services["listall"],
                                               virtualmachineid=vm_created.id,
                                               page=2,
                                               pagesize=self.services["pagesize"],
                                               )
        status = validateList(list_snapshots_page2)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of VM Snapshots failed in page 2"
                          )
        # Verifying the list size is equal to 1
        self.assertEquals(
                          1,
                          len(list_snapshots_page2),
                          "List VM Snapshot count is not matching in page 2"
                          )
        # Deleting VM Snapshot in page 2
        VmSnapshot.deleteVMSnapshot(
                                    self.userapiclient,
                                    snapshot_created.id
                                    )
        # Listing all the VM Snapshots in page 2 again
        list_snapshots_page2 = VmSnapshot.list(
                                               self.userapiclient,
                                               listall=self.services["listall"],
                                               virtualmachineid=vm_created.id,
                                               page=2,
                                               pagesize=self.services["pagesize"],
                                               )
        # Verifying the list size is equal to 0
        self.assertIsNone(
                          list_snapshots_page2,
                          "VM Snapshots exists in page 2"
                          )
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_15_revert_vm_to_snapshot(self):
        """
        @Desc: Test Revert VM to Snapshot functionality.
        @Steps:
        Step1: Deploying a VM
        Step2: Listing all the Snapshots of the VM deployed in Step 1
        Step3: Verifying that the list size is 0
        Step4: Creating 2 Snapshots for the VM
        Step5: Listing all the Snapshots of the VM deployed in Step 1
        Step6: Verifying that the list size is 2
        Step7: Verifying that only 1 snapshot is have current flag set to True
        Step8: Verifying that the VM snapshot with current flag set as true is the latest snapshot created
        Step9: Reverting VM to snapshot having current flag as false (non current snapshot)
        Step10: Verifying that only 1 VM snapshot is having current flag set as true.
        Step11: Verifying that the VM Snapshot with current flag set to true is the reverted snapshot in Step 8
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              )
        self.assertIsNone(
                           list_vms_before,
                           "Virtual Machine already exists for newly created user"
                           )
        # Deploying a VM
        vm_created = VirtualMachine.create(
                                           self.userapiclient,
                                           self.services["virtual_machine"],
                                           accountid=self.account.name,
                                           domainid=self.account.domainid,
                                           serviceofferingid=self.service_offering.id,
                                           )
        self.assertIsNotNone(
                             vm_created,
                             "VM creation failed"
                             )
        self.cleanup.append(vm_created)
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             )
        status = validateList(list_vms_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VM creation failed"
                          )
        # Verifying that the size of the list is 1
        self.assertEquals(
                          1,
                          len(list_vms_after),
                          "VM list count is not matching"
                          )
        # Listing all the VM snapshots for VM deployed above
        list_snapshots_before = VmSnapshot.list(
                                                self.userapiclient,
                                                listall=self.services["listall"],
                                                virtualmachineid=vm_created.id
                                                )
        # Verifying that the VM snapshot list is None
        self.assertIsNone(
                          list_snapshots_before,
                          "Snapshots already exists for newly created VM"
                          )
        # Creating 2 of VM snapshots
        snapshot1 = VmSnapshot.create(
                                      self.userapiclient,
                                      vm_created.id,
                                      )
        self.assertIsNotNone(
                             snapshot1,
                             "Snapshot creation failed"
                             )
        snapshot2 = VmSnapshot.create(
                                      self.userapiclient,
                                      vm_created.id,
                                      )
        self.assertIsNotNone(
                             snapshot2,
                             "Snapshot creation failed"
                             )
        # Listing all the VM snapshots for VM again
        list_snapshots_after = VmSnapshot.list(
                                                self.userapiclient,
                                                listall=self.services["listall"],
                                                virtualmachineid=vm_created.id
                                                )
        status = validateList(list_snapshots_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VM Snapshots creation failed"
                          )
        self.assertEquals(
                          2,
                          len(list_snapshots_after),
                          "Count of VM Snapshots is not matching"
                          )
        # Verifying that only 1 snapshot is having current flag set to true
        # and that snapshot is the latest snapshot created (snapshot2)
        current_count = 0
        for i in range(0, len(list_snapshots_after)):
            if(list_snapshots_after[i].current is True):
                current_count = current_count + 1
                current_snapshot = list_snapshots_after[i]

        self.assertEquals(
                          1,
                          current_count,
                          "count of VM Snapshot with current flag as true is not matching"
                          )
        self.assertEquals(
                          snapshot2.id,
                          current_snapshot.id,
                          "Latest snapshot taken is not marked as current"
                          )
        # Reverting the VM to Snapshot 1
        VmSnapshot.revertToSnapshot(
                                    self.userapiclient,
                                    snapshot1.id
                                    )
        # Listing the VM snapshots again
        list_snapshots_after = VmSnapshot.list(
                                                self.userapiclient,
                                                listall=self.services["listall"],
                                                virtualmachineid=vm_created.id
                                                )
        status = validateList(list_snapshots_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VM Snapshots creation failed"
                          )
        self.assertEquals(
                          2,
                          len(list_snapshots_after),
                          "Count of VM Snapshots is not matching"
                          )
        # Verifying that only 1 snapshot is having current flag set to true
        # and that snapshot is snapshot1
        current_count = 0
        for i in range(0, len(list_snapshots_after)):
            if(list_snapshots_after[i].current is True):
                current_count = current_count + 1
                current_snapshot = list_snapshots_after[i]
        self.assertEquals(
                          1,
                          current_count,
                          "count of VM Snapshot with current flag as true is not matching"
                          )
        self.assertEquals(
                          snapshot1.id,
                          current_snapshot.id,
                          "Current flag was set properly after reverting the VM to snapshot"
                          )
        return

    @attr(tags=["advanced", "basic", "selfservice"])
    def test_16_list_vm_volumes_pagination(self):
        """
        @Desc: Test to verify pagination of Volumes for a VM
        @Steps:
        Step1: Deploying a VM
        Step2: Listing all the Volumes of the VM deployed in Step 1
        Step3: Verifying that the list size is 1
        Step4: Creating page size number of volumes
        Step5: Attaching all the volumes created in step4 to VM deployed in Step1
        Step6: Listing all the Volumes for the VM in step1
        Step7: Verifying that the list size is equal to page size + 1
        Step8: Listing all the volumes of VM in page 1
        Step9: Verifying that the list size is equal to page size
        Step10: Listing all the Volumes in Page 2
        Step11: Verifying that the list size is 1
        Step12: Detaching the volume from the VM
        Step13: Listing all the Volumes in Page 2
        Step14: Verifying that list size is 0
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              )
        self.assertIsNone(
                           list_vms_before,
                           "Virtual Machine already exists for newly created user"
                           )
        # Deploying a VM
        vm_created = VirtualMachine.create(
                                           self.userapiclient,
                                           self.services["virtual_machine"],
                                           accountid=self.account.name,
                                           domainid=self.account.domainid,
                                           serviceofferingid=self.service_offering.id,
                                           )
        self.assertIsNotNone(
                             vm_created,
                             "VM creation failed"
                             )
        self.cleanup.append(vm_created)
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             )
        status = validateList(list_vms_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VM creation failed"
                          )
        # Verifying that the size of the list is 1
        self.assertEquals(
                          1,
                          len(list_vms_after),
                          "VM list count is not matching"
                          )
        # Listing all the Volumes for the VM deployed
        list_volumes_before = Volume.list(
                                          self.userapiclient,
                                          listall=self.services["listall"],
                                          virtualmachineid=vm_created.id
                                          )
        status = validateList(list_volumes_before)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Root volume is not created for VM deployed"
                          )
        # Verifying the size of the list is 1
        self.assertEquals(
                          1,
                          len(list_volumes_before),
                          "Volumes count is not matching"
                          )
        # Creating Page size number of volumes
        for i in range(0, self.services["pagesize"]):
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
            self.cleanup.append(volume_created)
            # Attaching all the volumes created to VM
            vm_created.attach_volume(
                                     self.userapiclient,
                                     volume_created
                                     )

        # List all the volumes for the VM again
        list_volumes_after = Volume.list(
                                          self.userapiclient,
                                          listall=self.services["listall"],
                                          virtualmachineid=vm_created.id
                                          )
        status = validateList(list_volumes_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Volumes are not listed"
                          )
        # Verifying that size of the list is equal to page size + 1
        self.assertEquals(
                          self.services["pagesize"] + 1,
                          len(list_volumes_after),
                          "VM's volume count is not matching"
                          )
        # Listing all the volumes for a VM in page 1
        list_volumes_page1 = Volume.list(
                                         self.userapiclient,
                                         listall=self.services["listall"],
                                         virtualmachineid=vm_created.id,
                                         page=1,
                                         pagesize=self.services["pagesize"]
                                         )
        status = validateList(list_volumes_page1)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Volumes not listed in page1"
                          )
        # Verifying that list size is equal to page size
        self.assertEquals(
                          self.services["pagesize"],
                          len(list_volumes_page1),
                          "VM's volume count is not matching in page 1"
                          )
        # Listing all the volumes for a VM in page 2
        list_volumes_page2 = Volume.list(
                                         self.userapiclient,
                                         listall=self.services["listall"],
                                         virtualmachineid=vm_created.id,
                                         page=2,
                                         pagesize=self.services["pagesize"]
                                         )
        status = validateList(list_volumes_page2)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Volumes not listed in page2"
                          )
        # Verifying that list size is equal to 1
        self.assertEquals(
                          1,
                          len(list_volumes_page2),
                          "VM's volume count is not matching in page 1"
                          )
        # Detaching 1 volume from VM
        vm_created.detach_volume(
                                 self.userapiclient,
                                 volume_created
                                 )
        # Listing all the volumes for a VM in page 2 again
        list_volumes_page2 = Volume.list(
                                         self.userapiclient,
                                         listall=self.services["listall"],
                                         virtualmachineid=vm_created.id,
                                         page=2,
                                         pagesize=self.services["pagesize"]
                                         )
        # Verifying that there are no volumes present in page 2
        self.assertIsNone(
                          list_volumes_page2,
                          "Volumes listed in page 2"
                          )
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_17_running_vm_scaleup(self):
        """
        @Desc: Test to verify change service for Running VM
        @Steps:
        Step1: Checking if dynamic scaling of virtual machines is enabled in zone and template.
                If yes then continuing.
                If not then printing message that scale up is not possible for Running VM
        Step2: Deploying a VM
        Step3: Listing all the existing service offerings
        Step4: If there is a matching Service Offering for scale-up of running VM
                use that service offering. If not create one service offering for scale up.
        Step5: Perform change service (scale up) the Running VM deployed in step1
        Step6: Verifying that VM's service offerings is changed
        """
        # Checking if Dynamic scaling of VM is supported or not
        list_config = Configurations.list(
                                          self.apiClient,
                                          zoneid=self.zone.id,
                                          name="enable.dynamic.scale.vm"
                                          )
        status = validateList(list_config)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of configuration failed"
                          )
        # Checking if dynamic scaling is allowed in Zone and Template
        if not ((list_config[0].value is True) and (self.template.isdynamicallyscalable)):
             self.debug("Scale up of Running VM is not possible as Zone/Template does not support")
        else:
            # Listing all the VM's for a User
            list_vms_before = VirtualMachine.list(
                                                  self.userapiclient,
                                                  listall=self.services["listall"],
                                                  )
            self.assertIsNone(
                               list_vms_before,
                               "Virtual Machine already exists for newly created user"
                               )
            # Deploying a VM
            vm_created = VirtualMachine.create(
                                               self.userapiclient,
                                               self.services["virtual_machine"],
                                               accountid=self.account.name,
                                               domainid=self.account.domainid,
                                               serviceofferingid=self.service_offering.id,
                                               )
            self.assertIsNotNone(
                                 vm_created,
                                 "VM creation failed"
                                 )
            self.cleanup.append(vm_created)
            # Listing details of current Service Offering
            vm_so_list = ServiceOffering.list(
                                              self.userapiclient,
                                              id=vm_created.serviceofferingid
                                              )
            status = validateList(vm_so_list)
            self.assertEquals(
                              PASS,
                              status[0],
                              "Listing of VM Service offering failed"
                              )
            current_so = vm_so_list[0]
            # Listing all the VMs for a user again
            list_vms_after = VirtualMachine.list(
                                                 self.userapiclient,
                                                 listall=self.services["listall"],
                                                 )
            status = validateList(list_vms_after)
            self.assertEquals(
                              PASS,
                              status[0],
                              "VM creation failed"
                              )
            # Verifying that the size of the list is 1
            self.assertEquals(
                              1,
                              len(list_vms_after),
                              "VM list count is not matching"
                              )
            # Listing all the existing service offerings
            service_offerings_list = ServiceOffering.list(
                                                          self.userapiclient,
                                                          virtualmachineid=vm_created.id
                                                          )
            # Verifying if any Service offering available for scale up of VM
            so_exists = False
            if service_offerings_list is not None:
                for i in range(0, len(service_offerings_list)):
                    if not ((current_so.cpunumber > service_offerings_list[i].cpunumber or\
                            current_so.cpuspeed > service_offerings_list[i].cpuspeed or\
                            current_so.memory > service_offerings_list[i].memory) or\
                            (current_so.cpunumber == service_offerings_list[i].cpunumber and\
                            current_so.cpuspeed == service_offerings_list[i].cpuspeed and\
                            current_so.memory == service_offerings_list[i].memory)):
                        if(current_so.storagetype == service_offerings_list[i].storagetype):
                            so_exists = True
                            new_so = service_offerings_list[i]
                            break
            # If service offering does not exists, then creating one service offering for scale up
            if not so_exists:
                self.services["service_offerings"]["small"]["storagetype"] = current_so.storagetype
                new_so = ServiceOffering.create(
                                                self.apiClient,
                                                self.services["service_offerings"]["small"]
                                                )
                self.cleanup.append(new_so)
            # Scaling up the VM
            vm_created.scale_virtualmachine(
                                            self.userapiclient,
                                            new_so.id
                                            )
            # Listing VM details again
            list_vms_after = VirtualMachine.list(
                                                 self.userapiclient,
                                                 id=vm_created.id
                                                 )
            status = validateList(list_vms_after)
            self.assertEquals(
                              PASS,
                              status[0],
                              "Listing of VM failed"
                              )
            self.assertEquals(
                              1,
                              len(list_vms_after),
                              "VMs list is not as expected"
                              )
            # Verifying that VM's service offerings is changed
            self.assertEquals(
                              new_so.id,
                              list_vms_after[0].serviceofferingid,
                              "VM is not containing New Service Offering"
                              )
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_18_stopped_vm_change_service(self):
        """
        @Desc: Test to verify change service for Stopped VM
        @Steps:
        Step1: Deploying a VM
        Step2: Stopping the VM deployed in step1
        Step3: Listing all the existing service offerings
        Step4: If there is a matching Service Offering for change service of stopped VM
                use that service offering. If not create one service offering for change service.
        Step5: Perform change service for the Stopped VM
        Step6: Verifying that VM's service offerings is changed
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              )
        self.assertIsNone(
                           list_vms_before,
                           "Virtual Machine already exists for newly created user"
                           )
        # Deploying a VM
        vm_created = VirtualMachine.create(
                                           self.userapiclient,
                                           self.services["virtual_machine"],
                                           accountid=self.account.name,
                                           domainid=self.account.domainid,
                                           serviceofferingid=self.service_offering.id,
                                           )
        self.assertIsNotNone(
                             vm_created,
                             "VM creation failed"
                             )
        self.cleanup.append(vm_created)
        # Listing details of current Service Offering
        vm_so_list = ServiceOffering.list(
                                          self.userapiclient,
                                          id=vm_created.serviceofferingid
                                          )
        status = validateList(vm_so_list)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of VM Service offering failed"
                          )
        current_so = vm_so_list[0]
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             )
        status = validateList(list_vms_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VM creation failed"
                          )
        # Verifying that the size of the list is 1
        self.assertEquals(
                          1,
                          len(list_vms_after),
                          "VM list count is not matching"
                          )
        # Stopping the VM deployed above
        vm_created.stop(
                        self.userapiclient,
                        forced=True
                        )
        # Listing all the service offerings
        service_offerings_list = ServiceOffering.list(
                                                      self.userapiclient,
                                                      virtualmachineid=vm_created.id
                                                      )
        # Verifying if any Service offering available for change service of VM
        so_exists = False
        if service_offerings_list is not None:
            for i in range(0, len(service_offerings_list)):
                if ((current_so.id != service_offerings_list[i].id) and\
                   (current_so.storagetype == service_offerings_list[i].storagetype)):
                    so_exists = True
                    new_so = service_offerings_list[i]
                    break
        # If service offering does not exists, then creating one service offering for scale up
        if not so_exists:
            self.services["service_offerings"]["small"]["storagetype"] = current_so.storagetype
            new_so = ServiceOffering.create(
                                            self.apiClient,
                                            self.services["service_offerings"]["small"]
                                            )
            self.cleanup.append(new_so)
        # Changing service for the VM
        vm_created.scale_virtualmachine(
                                        self.userapiclient,
                                        new_so.id
                                        )
        # Listing VM details again
        list_vm = VirtualMachine.list(
                                      self.userapiclient,
                                      id=vm_created.id
                                     )
        status = validateList(list_vm)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of VM failed"
                          )
        self.assertEquals(
                          1,
                          len(list_vm),
                          "VMs list is not as expected"
                          )
        # Verifying that VM's service offerings is changed
        self.assertEquals(
                          new_so.id,
                          list_vm[0].serviceofferingid,
                          "VM is not containing New Service Offering"
                          )
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_19_create_reset_vm_sshkey(self):
        """
        @Desc: Test to verify creation and reset of SSH Key for VM
        @Steps:
        Step1: Deploying a VM
        Step2: Stopping the VM deployed in step1
        Step3: Listing all the SSH Key pairs
        Step4: Creating a new SSH Key pair
        Step5: Listing all the SSh Key pairs again
        Step6: Verifying that the key pairs list is increased by 1
        Step7: Resetting the VM SSH Key to the key pair created in step4
        Step8: Verifying that the new SSH Key pair is set to the VM
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              )
        self.assertIsNone(
                           list_vms_before,
                           "Virtual Machine already exists for newly created user"
                           )
        # Deploying a VM
        vm_created = VirtualMachine.create(
                                           self.userapiclient,
                                           self.services["virtual_machine"],
                                           accountid=self.account.name,
                                           domainid=self.account.domainid,
                                           serviceofferingid=self.service_offering.id,
                                           )
        self.assertIsNotNone(
                             vm_created,
                             "VM creation failed"
                             )
        self.cleanup.append(vm_created)
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             )
        status = validateList(list_vms_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VM creation failed"
                          )
        # Verifying that the size of the list is 1
        self.assertEquals(
                          1,
                          len(list_vms_after),
                          "VM list count is not matching"
                          )
        # Stopping the VM deployed above
        vm_created.stop(
                        self.userapiclient,
                        forced=True
                        )
        # Listing all the SSH Key pairs
        list_keypairs_before = SSHKeyPair.list(
                                               self.userapiclient
                                               )
        list_keypairs_before_size = 0
        if list_keypairs_before is not None:
            list_keypairs_before_size = len(list_keypairs_before)

        # Creating a new Key pair
        new_keypair = SSHKeyPair.create(
                                        self.userapiclient,
                                        name="keypair1",
                                        account=self.account.name,
                                        domainid=self.domain.id
                                        )
        self.assertIsNotNone(
                             new_keypair,
                             "New Key pair generation failed"
                             )
        self.assertEquals(
                          "keypair1",
                          new_keypair.name,
                          "Key Pair not created with given name"
                          )
        # Listing all the SSH Key pairs again
        list_keypairs_after = SSHKeyPair.list(
                                              self.userapiclient
                                              )
        status = validateList(list_keypairs_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of Key pairs failed"
                          )
        # Verifying that list size is increased by 1
        self.assertEquals(
                          list_keypairs_before_size + 1,
                          len(list_keypairs_after),
                          "List count is not matching"
                          )
        # Resetting the VM SSH key to the Key pair created above
        vm_created.resetSshKey(
                               self.userapiclient,
                               keypair=new_keypair.name
                               )
        # Listing VM details again
        list_vm = VirtualMachine.list(
                                      self.userapiclient,
                                      id=vm_created.id
                                     )
        status = validateList(list_vm)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of VM failed"
                          )
        self.assertEquals(
                          1,
                          len(list_vm),
                          "VMs list is not as expected"
                          )
        # Verifying that VM's SSH keypair is set to newly created keypair
        self.assertEquals(
                          new_keypair.name,
                          list_vm[0].keypair,
                          "VM is not set to newly created SSH Key pair"
                          )
        return

    @attr(tags=["advanced", "basic", "selfservice"])
    def test_20_update_vm_displayname_group(self):
        """
        @Desc: Test to verify Update VM details
        @Steps:
        Step1: List all the VM's for a user
        Step2: Deploy a VM with all parameters
        Step3: Listing all the VM's again for the user
        Step4: Verifying that list size is increased by 1
        Step5: Updating VM details - displayname, group
        Step6: Listing the VM deployed in step 2 by ID
        Step7: Verifying that displayname, group details of the VM are updated
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              )
        self.assertIsNone(
                           list_vms_before,
                           "Virtual Machine already exists for newly created user"
                           )
        self.services["virtual_machine"]["keyboard"] = "us"
        # Deploying a VM
        vm_created = VirtualMachine.create(
                                           self.userapiclient,
                                           self.services["virtual_machine"],
                                           accountid=self.account.name,
                                           domainid=self.account.domainid,
                                           serviceofferingid=self.service_offering.id,
                                           group="groupName"
                                           )
        self.assertIsNotNone(
                             vm_created,
                             "VM creation failed"
                             )
        self.cleanup.append(vm_created)
        # Verifying the displayname and group details for deployed VM
        self.assertEquals(
                          self.services["virtual_machine"]["displayname"],
                          vm_created.displayname,
                          "Display name of VM is not as expected"
                          )
        self.assertEquals(
                          "groupName",
                          vm_created.group,
                          "Group of VM is not as expected"
                          )
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             )
        status = validateList(list_vms_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VM creation failed"
                          )
        # Verifying that the size of the list is 1
        self.assertEquals(
                          1,
                          len(list_vms_after),
                          "VM list count is not matching"
                          )
        # Updating the VM details - displayname and group
        vm_created.update(
                          self.userapiclient,
                          displayname="DisplayName",
                          group="Group",
                          haenable=False
                          )
        # Listing VM details again
        list_vm = VirtualMachine.list(
                                      self.userapiclient,
                                      id=vm_created.id,
                                      )
        status = validateList(list_vm)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of VM by Id failed"
                          )
        self.assertEquals(
                          1,
                          len(list_vm),
                          "Count of List VM by Id is not matching"
                          )
        # Verifying that displayname and group details are updated
        self.assertEquals(
                          "DisplayName",
                          list_vm[0].displayname,
                          "Displayname of VM is not updated"
                          )
        self.assertEquals(
                          "Group",
                          list_vm[0].group,
                          "Group of VM is not updated"
                          )
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_21_restore_vm(self):
        """
        @Desc: Test to verify Restore VM
        @Steps:
        Step1: List all the VM's for a user
        Step2: Deploy a VM with all parameters
        Step3: Listing all the VM's again for the user
        Step4: Verifying that list size is increased by 1
        Step5: Restoring the VM deployed in step2
        Step6: Verifying that restored VM details are same as the VM deployed in step2
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              )
        self.assertIsNone(
                           list_vms_before,
                           "Virtual Machine already exists for newly created user"
                           )
        # Deploying a VM
        vm_created = VirtualMachine.create(
                                           self.userapiclient,
                                           self.services["virtual_machine"],
                                           accountid=self.account.name,
                                           domainid=self.account.domainid,
                                           serviceofferingid=self.service_offering.id,
                                           )
        self.assertIsNotNone(
                             vm_created,
                             "VM creation failed"
                             )
        self.cleanup.append(vm_created)
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             )
        status = validateList(list_vms_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VM creation failed"
                          )
        # Verifying that the size of the list is 1
        self.assertEquals(
                          1,
                          len(list_vms_after),
                          "VM list count is not matching"
                          )
        # Restoring the VM
        restored_vm = vm_created.restore(self.userapiclient)
        self.assertIsNotNone(
                             restored_vm,
                             "VM restore failed"
                             )
        # Verifying the restored VM details
        expected_dict = {
                         "id":vm_created.id,
                         "name":vm_created.name,
                         "displayname":vm_created.displayname,
                         "state":vm_created.state,
                         "zoneid":vm_created.zoneid,
                         "account":vm_created.account,
                         "template":vm_created.templateid
                         }
        actual_dict = {
                       "id":restored_vm.id,
                       "name":restored_vm.name,
                       "displayname":restored_vm.displayname,
                       "state":restored_vm.state,
                       "zoneid":restored_vm.zoneid,
                       "account":restored_vm.account,
                       "template":restored_vm.templateid
                       }
        restored_vm_status = self.__verify_values(
                                                  expected_dict,
                                                  actual_dict
                                                  )
        self.assertEqual(
                         True,
                         restored_vm_status,
                         "Restored VM details are not as expected"
                         )
        return

    @attr(tags=["advanced", "selfservice"])
    def test_22_deploy_vm_multiple_networks(self):
        """
        @Desc: Test to verify deploy VM with multiple networks
        @Steps:
        Step1: List all the networks for user
        Step2: If size of list networks is greater than 2 then get all the networks id's
                Else create 2 networks and get network id's
        Step3: List all the VM's for a user
        Step4: Deploy a VM with multiple network id's
        Step5: Listing all the VM's again for the user
        Step6: Verifying that list size is increased by 1
        Step7: Verify that VM is associated with multiple networks
        """
        # Listing all the networks available
        networks_list_before = Network.list(
                                            self.userapiclient,
                                            listall=self.services["listall"]
                                            )
        networks_list_size = 0
        if networks_list_before is not None:
            networks_list_size = len(networks_list_before)

        # Listing Network Offerings
        network_offerings_list = NetworkOffering.list(
                                                      self.apiClient,
                                                      forvpc="false",
                                                      guestiptype="Isolated",
                                                      state="Enabled",
                                                      supportedservices="SourceNat",
                                                      zoneid=self.zone.id
                                                      )
        status = validateList(network_offerings_list)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Isolated Network Offerings with sourceNat enabled are not found"
                          )
        while networks_list_size < 2:
            # Creating a network
            network = Network.create(
                                      self.userapiclient,
                                      self.services["network"],
                                      accountid=self.account.name,
                                      domainid=self.domain.id,
                                      networkofferingid=network_offerings_list[0].id,
                                      zoneid=self.zone.id
                                      )
            self.assertIsNotNone(
                                 network,
                                 "Network creation failed"
                                 )
            self.cleanup.append(network)
            networks_list_size = networks_list_size + 1

        # Listing the networks again
        networks_list_after = Network.list(
                                           self.userapiclient,
                                           listall=self.services["listall"]
                                           )
        status = validateList(network_offerings_list)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing networks failed"
                          )
        # populating network id's
        networkids = networks_list_after[0].id + "," + networks_list_after[1].id
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              )
        self.assertIsNone(
                           list_vms_before,
                           "Virtual Machine already exists for newly created user"
                           )
        # Deploying a VM
        vm_created = VirtualMachine.create(
                                           self.userapiclient,
                                           self.services["virtual_machine"],
                                           accountid=self.account.name,
                                           domainid=self.account.domainid,
                                           networkids=networkids,
                                           serviceofferingid=self.service_offering.id,
                                           )
        self.assertIsNotNone(
                             vm_created,
                             "VM creation failed"
                             )
        self.cleanup.append(vm_created)
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             )
        status = validateList(list_vms_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VM creation failed"
                          )
        # Verifying that the size of the list is 1
        self.assertEquals(
                          1,
                          len(list_vms_after),
                          "VM list count is not matching"
                          )
        # Verifying that the NIC's in VM created are same as provided
        vm_nics = vm_created.nic
        # Verifying that the size of nics is 2
        self.assertEquals(
                          2,
                          len(vm_nics),
                          "NIC's count in VM created is not matching"
                          )
        # Verifying that NIC network ID's are as expected
        for i in range(0, len(vm_nics)):
            if vm_nics[i].isdefault is True:
                self.assertEquals(
                                  networks_list_after[0].id,
                                  vm_nics[i].networkid,
                                  "Default NIC is not as expected"
                                  )
            else:
                self.assertEquals(
                                  networks_list_after[1].id,
                                  vm_nics[i].networkid,
                                  "Non Default NIC is not as expected"
                                  )
        return

    @attr(tags=["basic", "provisioning"])
    def test_23_deploy_vm_multiple_securitygroups(self):
        """
        @Desc: Test to verify deploy VM with multiple Security Groups
        @Steps:
        Step1: List all the security groups for user
        Step2: If size of list security groups is greater than 2 then get all the security groups id's
                Else creating 2 security groups and get security groups id's
        Step3: List all the VM's for a user
        Step4: Deploy a VM with multiple security groups id's
        Step5: Listing all the VM's again for the user
        Step6: Verifying that list size is increased by 1
        Step7: Verify that VM is associated with multiple security groups
        """
        # Listing all the security groups available
        security_groups_list = SecurityGroup.list(
                                                  self.userapiclient,
                                                  listall=self.services["listall"],
                                                  domainid=self.domain.id
                                                  )
        security_groups_list_size = 0
        if security_groups_list is not None:
            security_groups_list_size = len(security_groups_list)

        while security_groups_list_size < 2:
            # Creating a security group
            security_group = SecurityGroup.create(
                                                  self.userapiclient,
                                                  self.services["security_group"],
                                                  account=self.account.name,
                                                  domainid=self.domain.id
                                                  )
            self.assertIsNotNone(
                                 security_group,
                                 "Security Group creation failed"
                                 )
            self.cleanup.append(security_group)
            security_groups_list_size = security_groups_list_size + 1

        # Listing the networks again
        security_groups_list = SecurityGroup.list(
                                                  self.userapiclient,
                                                  listall=self.services["listall"],
                                                  domainid=self.domain.id
                                                  )
        status = validateList(security_groups_list)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing Security Groups failed"
                          )
        # populating Security Groups id's
        securitygroupids = {security_groups_list[0].id , security_groups_list[1].id}
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              )
        self.assertIsNone(
                           list_vms_before,
                           "Virtual Machine already exists for newly created user"
                           )
        # Deploying a VM
        vm_created = VirtualMachine.create(
                                           self.userapiclient,
                                           self.services["virtual_machine"],
                                           accountid=self.account.name,
                                           domainid=self.account.domainid,
                                           serviceofferingid=self.service_offering.id,
                                           securitygroupids=securitygroupids,
                                           )
        self.assertIsNotNone(
                             vm_created,
                             "VM creation failed"
                             )
        self.cleanup.append(vm_created)
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             )
        status = validateList(list_vms_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VM creation failed"
                          )
        # Verifying that the size of the list is 1
        self.assertEquals(
                          1,
                          len(list_vms_after),
                          "VM list count is not matching"
                          )
        # Verifying that the Security Groups's in VM created are same as provided
        vm_securitygroups = vm_created.securitygroup
        # Verifying that the size of security groups is 2
        self.assertEquals(
                          2,
                          len(vm_securitygroups),
                          "Security Groups count in VM created is not matching"
                          )
        # Verifying that Security Group network ID's are as expected
        vm_securitygroups_flag = True
        for i in range(0, len(vm_securitygroups)):
            if ((vm_securitygroups[i].id != security_groups_list[0].id) and\
                (vm_securitygroups[i].id != security_groups_list[1].id)):
                vm_securitygroups_flag = False
                break

        self.assertEquals(
                          True,
                          vm_securitygroups_flag,
                          "Security Groups in VM are not same as created"
                          )
        return

class TestSnapshots(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(TestSnapshots, cls).getClsTestClient()
            cls.api_client = cls.testClient.getApiClient()
            cls.services = cls.testClient.getParsedTestDataConfig()
            # Get Domain, Zone, Template
            cls.domain = get_domain(cls.api_client)
            cls.zone = get_zone(cls.api_client)
            cls.template = get_template(
                                cls.api_client,
                                cls.zone.id,
                                cls.services["ostype"]
                                )
            if cls.zone.localstorageenabled:
                cls.storagetype = 'local'
                cls.services["service_offerings"]["tiny"]["storagetype"] = 'local'
                cls.services["disk_offering"]["storagetype"] = 'local'
            else:
                cls.storagetype = 'shared'
                cls.services["service_offerings"]["tiny"]["storagetype"] = 'shared'
                cls.services["disk_offering"]["storagetype"] = 'shared'

            cls.services['mode'] = cls.zone.networktype
            cls.services["virtual_machine"]["hypervisor"] = cls.testClient.getHypervisorInfo()
            cls.services["virtual_machine"]["zoneid"] = cls.zone.id
            cls.services["virtual_machine"]["template"] = cls.template.id
            cls.services["custom_volume"]["zoneid"] = cls.zone.id
            # Creating Disk offering, Service Offering and Account
            cls.disk_offering = DiskOffering.create(
                                        cls.api_client,
                                        cls.services["disk_offering"]
                                        )
            cls._cleanup.append(cls.disk_offering)
            cls.service_offering = ServiceOffering.create(
                                                cls.api_client,
                                                cls.services["service_offerings"]["tiny"]
                                                )
            cls._cleanup.append(cls.service_offering)
            cls.account = Account.create(
                                cls.api_client,
                                cls.services["account"],
                                domainid=cls.domain.id
                                )
            # Getting authentication for user in newly created Account
            cls.user = cls.account.user[0]
            cls.userapiclient = cls.testClient.getUserApiClient(cls.user.username, cls.domain.name)
            cls._cleanup.append(cls.account)
            # Creating Virtual Machine
            cls.virtual_machine = VirtualMachine.create(
                                        cls.userapiclient,
                                        cls.services["virtual_machine"],
                                        accountid=cls.account.name,
                                        domainid=cls.account.domainid,
                                        serviceofferingid=cls.service_offering.id,
                                    )
            cls._cleanup.append(cls.virtual_machine)
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setup : %s" % e)
        return

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        #Clean up, terminate the created resources
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def __verify_values(self, expected_vals, actual_vals):
        """
        @Desc: Function to verify expected and actual values
        @Steps:
        Step1: Initializing return flag to True
        Step1: Verifying length of expected and actual dictionaries is matching.
               If not matching returning false
        Step2: Listing all the keys from expected dictionary
        Step3: Looping through each key from step2 and verifying expected and actual dictionaries have same value
               If not making return flag to False
        Step4: returning the return flag after all the values are verified
        """
        return_flag = True

        if len(expected_vals) != len(actual_vals):
            return False

        keys = expected_vals.keys()
        for i in range(0, len(expected_vals)):
            exp_val = expected_vals[keys[i]]
            act_val = actual_vals[keys[i]]
            if exp_val == act_val:
                return_flag = return_flag and True
            else:
                return_flag = return_flag and False
                self.debug("expected Value: %s, is not matching with actual value: %s" % (
                                                                                          exp_val,
                                                                                          act_val
                                                                                          ))
        return return_flag

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_01_list_volume_snapshots_pagination(self):
        """
        @Desc: Test to List Volume Snapshots pagination
        @steps:
        Step1: Listing all the volume snapshots for a user
        Step2: Verifying that list size is 0
        Step3: Creating (page size + 1) number of volume snapshots
        Step4: Listing all the volume snapshots again for a user
        Step5: Verifying that list size is (page size + 1)
        Step6: Listing all the volume snapshots in page1
        Step7: Verifying that list size is (page size)
        Step8: Listing all the volume snapshots in page2
        Step9: Verifying that list size is 1
        Step10: Deleting the volume snapshot present in page 2
        Step11: Listing all the volume snapshots in page2
        Step12: Verifying that list size is 0
        """
        # Listing all the volume snapshots for a User
        list_vol_snaps_before = Snapshot.list(
                                              self.userapiclient,
                                              listall=self.services["listall"]
                                              )
        # Verifying list size is 0
        self.assertIsNone(
                          list_vol_snaps_before,
                          "Volume snapshots exists for newly created user"
                          )
        # Listing the root volumes available for the user
        volumes_list = Volume.list(
                                   self.userapiclient,
                                   listall=self.services["listall"]
                                   )
        status = validateList(volumes_list)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Root volume did not get created while deploying a VM"
                          )
        # Verifying list size to be 1
        self.assertEquals(
                          1,
                          len(volumes_list),
                          "More than 1 root volume created for deployed VM"
                          )
        root_volume = volumes_list[0]
        # Creating pagesize + 1 number of volume snapshots
        for i in range(0, (self.services["pagesize"] + 1)):
            snapshot_created = Snapshot.create(
                                               self.userapiclient,
                                               root_volume.id,
                                               )
            self.assertIsNotNone(
                                 snapshot_created,
                                 "Snapshot creation failed"
                                 )
            self.cleanup.append(snapshot_created)

        # Listing all the volume snapshots for user again
        list_vol_snaps_after = Snapshot.list(
                                             self.userapiclient,
                                             listall=self.services["listall"]
                                             )
        status = validateList(list_vol_snaps_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Volume snapshot creation failed"
                          )
        # Verifying that list size is pagesize + 1
        self.assertEquals(
                          self.services["pagesize"] + 1,
                          len(list_vol_snaps_after),
                          "Failed to create pagesize + 1 number of Volume snapshots"
                          )
        # Listing all the volume snapshots in page 1
        list_vol_snaps_page1 = Snapshot.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             page=1,
                                             pagesize=self.services["pagesize"]
                                             )
        status = validateList(list_vol_snaps_page1)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Failed to list volume snapshots in page 1"
                          )
        # Verifying the list size to be equal to pagesize
        self.assertEquals(
                          self.services["pagesize"],
                          len(list_vol_snaps_page1),
                          "Size of volume snapshots in page 1 is not matching"
                          )
        # Listing all the volume snapshots in page 2
        list_vol_snaps_page2 = Snapshot.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             page=2,
                                             pagesize=self.services["pagesize"]
                                             )
        status = validateList(list_vol_snaps_page2)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Failed to list volume snapshots in page 2"
                          )
        # Verifying the list size to be equal to pagesize
        self.assertEquals(
                          1,
                          len(list_vol_snaps_page2),
                          "Size of volume snapshots in page 2 is not matching"
                          )
        # Deleting the volume snapshot present in page 2
        Snapshot.delete(
                        snapshot_created,
                        self.userapiclient
                        )
        # Listing all the snapshots in page 2 again
        list_vol_snaps_page2 = Snapshot.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             page=2,
                                             pagesize=self.services["pagesize"]
                                             )
        # Verifying that list size is 0
        self.assertIsNone(
                          list_vol_snaps_page2,
                          "Volume snapshot not deleted from page 2"
                          )
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_02_list_volume_snapshots_byid(self):
        """
        @Desc: Test to List Volume Snapshots by Id
        @Steps:
        Step1: Listing all the volume snapshots for a user
        Step2: Verifying that list size is 0
        Step3: Creating a volume snapshot
        Step4: Listing all the volume snapshots again for a user
        Step5: Verifying that list size is 1
        Step6: Listing all the volume snapshots by specifying snapshot id
        Step7: Verifying that list size is 1
        Step8: Verifying details of the listed volume snapshot
        """
        # Listing all the volume snapshots for a User
        list_vol_snaps_before = Snapshot.list(
                                              self.userapiclient,
                                              listall=self.services["listall"]
                                              )
        # Verifying list size is 0
        self.assertIsNone(
                          list_vol_snaps_before,
                          "Volume snapshots exists for newly created user"
                          )
        # Listing the root volumes available for the user
        volumes_list = Volume.list(
                                   self.userapiclient,
                                   listall=self.services["listall"]
                                   )
        status = validateList(volumes_list)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Root volume did not get created while deploying a VM"
                          )
        # Verifying list size to be 1
        self.assertEquals(
                          1,
                          len(volumes_list),
                          "More than 1 root volume created for deployed VM"
                          )
        root_volume = volumes_list[0]
        # Creating a volume snapshot
        snapshot_created = Snapshot.create(
                                           self.userapiclient,
                                           root_volume.id,
                                           )
        self.assertIsNotNone(
                             snapshot_created,
                             "Snapshot creation failed"
                             )
        self.cleanup.append(snapshot_created)
        # Listing all the volume snapshots for user again
        list_vol_snaps_after = Snapshot.list(
                                             self.userapiclient,
                                             listall=self.services["listall"]
                                             )
        status = validateList(list_vol_snaps_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Volume snapshot creation failed"
                          )
        # Verifying that list size is 1
        self.assertEquals(
                          1,
                          len(list_vol_snaps_after),
                          "Failed to create Volume snapshot"
                          )
        # Listing volume snapshot by id
        list_vol_snapshot = Snapshot.list(
                                          self.userapiclient,
                                          listall=self.services["listall"],
                                          id=snapshot_created.id
                                          )
        status = validateList(list_vol_snapshot)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Failed to list Volume snapshot by Id"
                          )
        # Verifying that list size is 1
        self.assertEquals(
                          1,
                          len(list_vol_snapshot),
                          "Size of the list volume snapshot by Id is not matching"
                          )
        # Verifying details of the listed snapshot to be same as snapshot created above
        #Creating expected and actual values dictionaries
        expected_dict = {
                         "id":snapshot_created.id,
                         "name":snapshot_created.name,
                         "state":snapshot_created.state,
                         "intervaltype":snapshot_created.intervaltype,
                         "account":snapshot_created.account,
                         "domain":snapshot_created.domainid,
                         "volume":snapshot_created.volumeid
                         }
        actual_dict = {
                       "id":list_vol_snapshot[0].id,
                       "name":list_vol_snapshot[0].name,
                       "state":list_vol_snapshot[0].state,
                       "intervaltype":list_vol_snapshot[0].intervaltype,
                       "account":list_vol_snapshot[0].account,
                       "domain":list_vol_snapshot[0].domainid,
                       "volume":list_vol_snapshot[0].volumeid
                       }
        vol_snapshot_status = self.__verify_values(
                                                   expected_dict,
                                                   actual_dict
                                                   )
        self.assertEqual(
                         True,
                         vol_snapshot_status,
                         "Listed Volume Snapshot details are not as expected"
                         )
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_03_list_vm_snapshots_pagination(self):
        """
        @Desc: Test to List VM Snapshots pagination
        @Steps:
        Step1: Listing all the VM snapshots for a user
        Step2: Verifying that list size is 0
        Step3: Creating (page size + 1) number of VM snapshots
        Step4: Listing all the VM snapshots again for a user
        Step5: Verifying that list size is (page size + 1)
        Step6: Listing all the VM snapshots in page1
        Step7: Verifying that list size is (page size)
        Step8: Listing all the VM snapshots in page2
        Step9: Verifying that list size is 1
        Step10: Deleting the VM snapshot present in page 2
        Step11: Listing all the volume snapshots in page2
        Step12: Verifying that list size is 0
        """
        # Listing all the VM snapshots for a User
        list_vm_snaps_before = VmSnapshot.list(
                                               self.userapiclient,
                                               listall=self.services["listall"]
                                               )
        # Verifying list size is 0
        self.assertIsNone(
                          list_vm_snaps_before,
                          "VM snapshots exists for newly created user"
                          )
        # Creating pagesize + 1 number of VM snapshots
        for i in range(0, (self.services["pagesize"] + 1)):
            snapshot_created = VmSnapshot.create(
                                                 self.userapiclient,
                                                 self.virtual_machine.id,
                                                 )
            self.assertIsNotNone(
                                 snapshot_created,
                                 "Snapshot creation failed"
                                 )

        # Listing all the VM snapshots for user again
        list_vm_snaps_after = VmSnapshot.list(
                                              self.userapiclient,
                                              listall=self.services["listall"]
                                              )
        status = validateList(list_vm_snaps_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VM snapshot creation failed"
                          )
        # Verifying that list size is pagesize + 1
        self.assertEquals(
                          self.services["pagesize"] + 1,
                          len(list_vm_snaps_after),
                          "Failed to create pagesize + 1 number of VM snapshots"
                          )
        # Listing all the VM snapshots in page 1
        list_vm_snaps_page1 = VmSnapshot.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              page=1,
                                              pagesize=self.services["pagesize"]
                                              )
        status = validateList(list_vm_snaps_page1)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Failed to list vm snapshots in page 1"
                          )
        # Verifying the list size to be equal to pagesize
        self.assertEquals(
                          self.services["pagesize"],
                          len(list_vm_snaps_page1),
                          "Size of vm snapshots in page 1 is not matching"
                          )
        # Listing all the vm snapshots in page 2
        list_vm_snaps_page2 = VmSnapshot.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              page=2,
                                              pagesize=self.services["pagesize"]
                                              )
        status = validateList(list_vm_snaps_page2)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Failed to list vm snapshots in page 2"
                          )
        # Verifying the list size to be equal to pagesize
        self.assertEquals(
                          1,
                          len(list_vm_snaps_page2),
                          "Size of vm snapshots in page 2 is not matching"
                          )
        # Deleting the vm snapshot present in page 2
        VmSnapshot.deleteVMSnapshot(
                                    self.userapiclient,
                                    snapshot_created.id
                                    )
        # Listing all the snapshots in page 2 again
        list_vm_snaps_page2 = VmSnapshot.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              page=2,
                                              pagesize=self.services["pagesize"]
                                              )
        # Verifying that list size is 0
        self.assertIsNone(
                          list_vm_snaps_page2,
                          "VM snapshot not deleted from page 2"
                          )
        # Deleting all the existing VM snapshots
        list_vm_snaps =  VmSnapshot.list(
                                         self.userapiclient,
                                         listall=self.services["listall"],
                                         )
        status = validateList(list_vm_snaps)
        self.assertEquals(
                          PASS,
                          status[0],
                          "All VM snapshots deleted"
                          )
        # Verifying that list size is equal to page size
        self.assertEquals(
                          self.services["pagesize"],
                          len(list_vm_snaps),
                          "VM Snapshots count is not matching"
                          )
        # Deleting all the existing VM snapshots
        for i in range(0, len(list_vm_snaps)):
            VmSnapshot.deleteVMSnapshot(
                                    self.userapiclient,
                                    list_vm_snaps[i].id
                                    )
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_04_list_vm_snapshots_byid(self):
        """
        @summary: Test to List VM Snapshots by Id

        Step1: Listing all the VM snapshots for a user
        Step2: Verifying that list size is 0
        Step3: Creating a VM snapshot
        Step4: Listing all the VM snapshots again for a user
        Step5: Verifying that list size is 1
        Step6: Listing all the VM snapshots by specifying snapshot id
        Step7: Verifying that list size is 1
        Step8: Verifying details of the listed VM snapshot
        """
        # Listing all the VM snapshots for a User
        list_vm_snaps_before = VmSnapshot.list(
                                               self.userapiclient,
                                               listall=self.services["listall"]
                                               )
        # Verifying list size is 0
        self.assertIsNone(
                          list_vm_snaps_before,
                          "VM snapshots exists for newly created user"
                          )
        # Creating a VM snapshot
        snapshot_created = VmSnapshot.create(
                                             self.userapiclient,
                                             self.virtual_machine.id,
                                             )
        self.assertIsNotNone(
                             snapshot_created,
                             "Snapshot creation failed"
                             )
        # Listing all the VM snapshots for user again
        list_vm_snaps_after = VmSnapshot.list(
                                              self.userapiclient,
                                              listall=self.services["listall"]
                                              )
        status = validateList(list_vm_snaps_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VM snapshot creation failed"
                          )
        # Verifying that list size is 1
        self.assertEquals(
                          1,
                          len(list_vm_snaps_after),
                          "Failed to create VM snapshot"
                          )
        # Listing vm snapshot by id
        list_vm_snapshot = VmSnapshot.list(
                                           self.userapiclient,
                                           listall=self.services["listall"],
                                           vmsnapshotid=snapshot_created.id
                                          )
        status = validateList(list_vm_snapshot)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Failed to list VM snapshot by Id"
                          )
        # Verifying that list size is 1
        self.assertEquals(
                          1,
                          len(list_vm_snapshot),
                          "Size of the list vm snapshot by Id is not matching"
                          )
        # Verifying details of the listed snapshot to be same as snapshot created above
        #Creating expected and actual values dictionaries
        expected_dict = {
                         "id":snapshot_created.id,
                         "name":snapshot_created.name,
                         "state":snapshot_created.state,
                         "vmid":snapshot_created.virtualmachineid,
                         }
        actual_dict = {
                       "id":list_vm_snapshot[0].id,
                       "name":list_vm_snapshot[0].name,
                       "state":list_vm_snapshot[0].state,
                       "vmid":list_vm_snapshot[0].virtualmachineid,
                       }
        vm_snapshot_status = self.__verify_values(
                                                  expected_dict,
                                                  actual_dict
                                                  )
        self.assertEqual(
                         True,
                         vm_snapshot_status,
                         "Listed VM Snapshot details are not as expected"
                         )
        return

class TestSecurityGroups(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(TestSecurityGroups, cls).getClsTestClient()
            cls.api_client = cls.testClient.getApiClient()
            cls.services = cls.testClient.getParsedTestDataConfig()
            # Get Domain, Zone, Template
            cls.domain = get_domain(cls.api_client)
            cls.zone = get_zone(cls.api_client)
            cls.template = get_template(
                                cls.api_client,
                                cls.zone.id,
                                cls.services["ostype"]
                                )
            cls.services['mode'] = cls.zone.networktype
            cls.account = Account.create(
                                cls.api_client,
                                cls.services["account"],
                                domainid=cls.domain.id
                                )
            # Getting authentication for user in newly created Account
            cls.user = cls.account.user[0]
            cls.userapiclient = cls.testClient.getUserApiClient(cls.user.username, cls.domain.name)
            cls._cleanup.append(cls.account)
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setup : %s" % e)
        return

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        #Clean up, terminate the created resources
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def __verify_values(self, expected_vals, actual_vals):
        """
        @Desc: Function to verify expected and actual values
        @Steps:
        Step1: Initializing return flag to True
        Step1: Verifying length of expected and actual dictionaries is matching.
               If not matching returning false
        Step2: Listing all the keys from expected dictionary
        Step3: Looping through each key from step2 and verifying expected and actual dictionaries have same value
               If not making return flag to False
        Step4: returning the return flag after all the values are verified
        """
        return_flag = True

        if len(expected_vals) != len(actual_vals):
            return False

        keys = expected_vals.keys()
        for i in range(0, len(expected_vals)):
            exp_val = expected_vals[keys[i]]
            act_val = actual_vals[keys[i]]
            if exp_val == act_val:
                return_flag = return_flag and True
            else:
                return_flag = return_flag and False
                self.debug("expected Value: %s, is not matching with actual value: %s" % (
                                                                                          exp_val,
                                                                                          act_val
                                                                                          ))
        return return_flag

    @attr(tags=["basic", "provisioning"])
    def test_01_list_securitygroups_pagination(self):
        """
        @Desc: Test to List Security Groups pagination
        @steps:
        Step1: Listing all the Security Groups for a user
        Step2: Verifying that list size is 1
        Step3: Creating (page size) number of Security Groups
        Step4: Listing all the Security Groups again for a user
        Step5: Verifying that list size is (page size + 1)
        Step6: Listing all the Security Groups in page1
        Step7: Verifying that list size is (page size)
        Step8: Listing all the Security Groups in page2
        Step9: Verifying that list size is 1
        Step10: Deleting the Security Group present in page 2
        Step11: Listing all the Security Groups in page2
        Step12: Verifying that no security groups are listed
        """
        # Listing all the Security Groups for a User
        list_securitygroups_before = SecurityGroup.list(
                                                        self.userapiclient,
                                                        listall=self.services["listall"]
                                                        )
        # Verifying that default security group is created
        status = validateList(list_securitygroups_before)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Default Security Groups creation failed"
                          )
        # Verifying the size of the list is 1
        self.assertEquals(
                          1,
                          len(list_securitygroups_before),
                          "Count of Security Groups list is not matching"
                          )
        # Creating pagesize number of security groups
        for i in range(0, (self.services["pagesize"])):
            securitygroup_created = SecurityGroup.create(
                                                         self.userapiclient,
                                                         self.services["security_group"],
                                                         account=self.account.name,
                                                         domainid=self.domain.id,
                                                         description=self.services["security_group"]["name"]
                                                         )
            self.assertIsNotNone(
                                 securitygroup_created,
                                 "Security Group creation failed"
                                 )
            if (i < self.services["pagesize"]):
                self.cleanup.append(securitygroup_created)

        # Listing all the security groups for user again
        list_securitygroups_after = SecurityGroup.list(
                                                       self.userapiclient,
                                                       listall=self.services["listall"]
                                                       )
        status = validateList(list_securitygroups_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Security Groups creation failed"
                          )
        # Verifying that list size is pagesize + 1
        self.assertEquals(
                          self.services["pagesize"] + 1,
                          len(list_securitygroups_after),
                          "Failed to create pagesize + 1 number of Security Groups"
                          )
        # Listing all the security groups in page 1
        list_securitygroups_page1 = SecurityGroup.list(
                                                       self.userapiclient,
                                                       listall=self.services["listall"],
                                                       page=1,
                                                       pagesize=self.services["pagesize"]
                                                       )
        status = validateList(list_securitygroups_page1)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Failed to list security groups in page 1"
                          )
        # Verifying the list size to be equal to pagesize
        self.assertEquals(
                          self.services["pagesize"],
                          len(list_securitygroups_page1),
                          "Size of security groups in page 1 is not matching"
                          )
        # Listing all the security groups in page 2
        list_securitygroups_page2 = SecurityGroup.list(
                                                       self.userapiclient,
                                                       listall=self.services["listall"],
                                                       page=2,
                                                       pagesize=self.services["pagesize"]
                                                       )
        status = validateList(list_securitygroups_page2)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Failed to list security groups in page 2"
                          )
        # Verifying the list size to be equal to pagesize
        self.assertEquals(
                          1,
                          len(list_securitygroups_page2),
                          "Size of security groups in page 2 is not matching"
                          )
        # Deleting the security group present in page 2
        SecurityGroup.delete(
                             securitygroup_created,
                             self.userapiclient)
        # Listing all the security groups in page 2 again
        list_securitygroups_page2 = SecurityGroup.list(
                                                       self.userapiclient,
                                                       listall=self.services["listall"],
                                                       page=2,
                                                       pagesize=self.services["pagesize"]
                                                       )
        # Verifying that there are no security groups listed
        self.assertIsNone(
                          list_securitygroups_page2,
                          "Security Groups not deleted from page 2"
                          )
        return

    @attr(tags=["basic", "provisioning"])
    def test_02_securitygroups_authorize_revoke_ingress(self):
        """
        @Desc: Test to Authorize and Revoke Ingress for Security Group
        @steps:
        Step1: Listing all the Security Groups for a user
        Step2: Verifying that list size is 1
        Step3: Creating a Security Groups
        Step4: Listing all the Security Groups again for a user
        Step5: Verifying that list size is 2
        Step6: Authorizing Ingress for the security group created in step3
        Step7: Listing the security groups by passing id of security group created in step3
        Step8: Verifying that list size is 1
        Step9: Verifying that Ingress is authorized to the security group
        Step10: Verifying the details of the Ingress rule are as expected
        Step11: Revoking Ingress for the security group created in step3
        Step12: Listing the security groups by passing id of security group created in step3
        Step13: Verifying that list size is 1
        Step14: Verifying that Ingress is revoked from the security group
        """
        # Listing all the Security Groups for a User
        list_securitygroups_before = SecurityGroup.list(
                                                        self.userapiclient,
                                                        listall=self.services["listall"]
                                                        )
        # Verifying that default security group is created
        status = validateList(list_securitygroups_before)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Default Security Groups creation failed"
                          )
        # Verifying the size of the list is 1
        self.assertEquals(
                          1,
                          len(list_securitygroups_before),
                          "Count of Security Groups list is not matching"
                          )
        # Creating a security group
        securitygroup_created = SecurityGroup.create(
                                                     self.userapiclient,
                                                     self.services["security_group"],
                                                     account=self.account.name,
                                                     domainid=self.domain.id,
                                                     description=self.services["security_group"]["name"]
                                                     )
        self.assertIsNotNone(
                             securitygroup_created,
                             "Security Group creation failed"
                             )
        self.cleanup.append(securitygroup_created)

        # Listing all the security groups for user again
        list_securitygroups_after = SecurityGroup.list(
                                                       self.userapiclient,
                                                       listall=self.services["listall"]
                                                       )
        status = validateList(list_securitygroups_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Security Groups creation failed"
                          )
        # Verifying that list size is 2
        self.assertEquals(
                          2,
                          len(list_securitygroups_after),
                          "Failed to create Security Group"
                          )
        # Authorizing Ingress for the security group created in step3
        securitygroup_created.authorize(
                                        self.userapiclient,
                                        self.services["ingress_rule"],
                                        self.account.name,
                                        self.domain.id,
                                        )
        # Listing the security group by Id
        list_securitygroups_byid = SecurityGroup.list(
                                                      self.userapiclient,
                                                      listall=self.services["listall"],
                                                      id=securitygroup_created.id,
                                                      domainid=self.domain.id
                                                      )
        # Verifying that security group is listed
        status = validateList(list_securitygroups_byid)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of Security Groups by id failed"
                          )
        # Verifying size of the list is 1
        self.assertEquals(
                          1,
                          len(list_securitygroups_byid),
                          "Count of the listing security group by id is not matching"
                          )
        securitygroup_ingress = list_securitygroups_byid[0].ingressrule
        # Validating the Ingress rule
        status = validateList(securitygroup_ingress)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Security Groups Ingress rule authorization failed"
                          )
        self.assertEquals(
                          1,
                          len(securitygroup_ingress),
                          "Security Group Ingress rules count is not matching"
                          )
        # Verifying the details of the Ingress rule are as expected
        #Creating expected and actual values dictionaries
        expected_dict = {
                         "cidr":self.services["ingress_rule"]["cidrlist"],
                         "protocol":self.services["ingress_rule"]["protocol"],
                         "startport":self.services["ingress_rule"]["startport"],
                         "endport":self.services["ingress_rule"]["endport"],
                         }
        actual_dict = {
                       "cidr":str(securitygroup_ingress[0].cidr),
                       "protocol":str(securitygroup_ingress[0].protocol.upper()),
                       "startport":str(securitygroup_ingress[0].startport),
                       "endport":str(securitygroup_ingress[0].endport),
                       }
        ingress_status = self.__verify_values(
                                              expected_dict,
                                              actual_dict
                                              )
        self.assertEqual(
                         True,
                         ingress_status,
                         "Listed Security group Ingress rule details are not as expected"
                         )
        # Revoking the Ingress rule from Security Group
        securitygroup_created.revoke(self.userapiclient, securitygroup_ingress[0].ruleid)
        # Listing the security group by Id
        list_securitygroups_byid = SecurityGroup.list(
                                                      self.userapiclient,
                                                      listall=self.services["listall"],
                                                      id=securitygroup_created.id,
                                                      domainid=self.domain.id
                                                      )
        # Verifying that security group is listed
        status = validateList(list_securitygroups_byid)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of Security Groups by id failed"
                          )
        # Verifying size of the list is 1
        self.assertEquals(
                          1,
                          len(list_securitygroups_byid),
                          "Count of the listing security group by id is not matching"
                          )
        securitygroup_ingress = list_securitygroups_byid[0].ingressrule
        # Verifying that Ingress rule is empty(revoked)
        status = validateList(securitygroup_ingress)
        self.assertEquals(
                          EMPTY_LIST,
                          status[2],
                          "Security Groups Ingress rule is not revoked"
                          )
        return

    @attr(tags=["basic", "provisioning"])
    def test_03_securitygroups_authorize_revoke_egress(self):
        """
        @Desc: Test to Authorize and Revoke Egress for Security Group
        @steps:
        Step1: Listing all the Security Groups for a user
        Step2: Verifying that list size is 1
        Step3: Creating a Security Groups
        Step4: Listing all the Security Groups again for a user
        Step5: Verifying that list size is 2
        Step6: Authorizing Egress for the security group created in step3
        Step7: Listing the security groups by passing id of security group created in step3
        Step8: Verifying that list size is 1
        Step9: Verifying that Egress is authorized to the security group
        Step10: Verifying the details of the Egress rule are as expected
        Step11: Revoking Egress for the security group created in step3
        Step12: Listing the security groups by passing id of security group created in step3
        Step13: Verifying that list size is 1
        Step14: Verifying that Egress is revoked from the security group
        """
        # Listing all the Security Groups for a User
        list_securitygroups_before = SecurityGroup.list(
                                                        self.userapiclient,
                                                        listall=self.services["listall"]
                                                        )
        # Verifying that default security group is created
        status = validateList(list_securitygroups_before)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Default Security Groups creation failed"
                          )
        # Verifying the size of the list is 1
        self.assertEquals(
                          1,
                          len(list_securitygroups_before),
                          "Count of Security Groups list is not matching"
                          )
        # Creating a security group
        securitygroup_created = SecurityGroup.create(
                                                     self.userapiclient,
                                                     self.services["security_group"],
                                                     account=self.account.name,
                                                     domainid=self.domain.id,
                                                     description=self.services["security_group"]["name"]
                                                     )
        self.assertIsNotNone(
                             securitygroup_created,
                             "Security Group creation failed"
                             )
        self.cleanup.append(securitygroup_created)

        # Listing all the security groups for user again
        list_securitygroups_after = SecurityGroup.list(
                                                       self.userapiclient,
                                                       listall=self.services["listall"]
                                                       )
        status = validateList(list_securitygroups_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Security Groups creation failed"
                          )
        # Verifying that list size is 2
        self.assertEquals(
                          2,
                          len(list_securitygroups_after),
                          "Failed to create Security Group"
                          )
        # Authorizing Egress for the security group created in step3
        securitygroup_created.authorizeEgress(
                                              self.userapiclient,
                                              self.services["ingress_rule"],
                                              self.account.name,
                                              self.domain.id,
                                              )
        # Listing the security group by Id
        list_securitygroups_byid = SecurityGroup.list(
                                                      self.userapiclient,
                                                      listall=self.services["listall"],
                                                      id=securitygroup_created.id,
                                                      domainid=self.domain.id
                                                      )
        # Verifying that security group is listed
        status = validateList(list_securitygroups_byid)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of Security Groups by id failed"
                          )
        # Verifying size of the list is 1
        self.assertEquals(
                          1,
                          len(list_securitygroups_byid),
                          "Count of the listing security group by id is not matching"
                          )
        securitygroup_egress = list_securitygroups_byid[0].egressrule
        # Validating the Ingress rule
        status = validateList(securitygroup_egress)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Security Groups Egress rule authorization failed"
                          )
        self.assertEquals(
                          1,
                          len(securitygroup_egress),
                          "Security Group Egress rules count is not matching"
                          )
        # Verifying the details of the Egress rule are as expected
        #Creating expected and actual values dictionaries
        expected_dict = {
                         "cidr":self.services["ingress_rule"]["cidrlist"],
                         "protocol":self.services["ingress_rule"]["protocol"],
                         "startport":self.services["ingress_rule"]["startport"],
                         "endport":self.services["ingress_rule"]["endport"],
                         }
        actual_dict = {
                       "cidr":str(securitygroup_egress[0].cidr),
                       "protocol":str(securitygroup_egress[0].protocol.upper()),
                       "startport":str(securitygroup_egress[0].startport),
                       "endport":str(securitygroup_egress[0].endport),
                       }
        ingress_status = self.__verify_values(
                                              expected_dict,
                                              actual_dict
                                              )
        self.assertEqual(
                         True,
                         ingress_status,
                         "Listed Security group Egress rule details are not as expected"
                         )
        # Revoking the Egress rule from Security Group
        securitygroup_created.revokeEgress(self.userapiclient, securitygroup_egress[0].ruleid)
        # Listing the security group by Id
        list_securitygroups_byid = SecurityGroup.list(
                                                      self.userapiclient,
                                                      listall=self.services["listall"],
                                                      id=securitygroup_created.id,
                                                      domainid=self.domain.id
                                                      )
        # Verifying that security group is listed
        status = validateList(list_securitygroups_byid)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of Security Groups by id failed"
                          )
        # Verifying size of the list is 1
        self.assertEquals(
                          1,
                          len(list_securitygroups_byid),
                          "Count of the listing security group by id is not matching"
                          )
        securitygroup_egress = list_securitygroups_byid[0].egressrule
        # Verifying that Ingress rule is empty(revoked)
        status = validateList(securitygroup_egress)
        self.assertEquals(
                          EMPTY_LIST,
                          status[2],
                          "Security Groups Egress rule is not revoked"
                          )
        return

class TestVpnCustomerGateways(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(TestVpnCustomerGateways, cls).getClsTestClient()
            cls.api_client = cls.testClient.getApiClient()
            cls.services = cls.testClient.getParsedTestDataConfig()
            # Get Domain, Zone, Template
            cls.domain = get_domain(cls.api_client)
            cls.zone = get_zone(cls.api_client)
            cls.template = get_template(
                                cls.api_client,
                                cls.zone.id,
                                cls.services["ostype"]
                                )
            cls.services['mode'] = cls.zone.networktype
            cls.account = Account.create(
                                cls.api_client,
                                cls.services["account"],
                                domainid=cls.domain.id
                                )
            # Getting authentication for user in newly created Account
            cls.user = cls.account.user[0]
            cls.userapiclient = cls.testClient.getUserApiClient(cls.user.username, cls.domain.name)
            cls._cleanup.append(cls.account)
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setup : %s" % e)
        return

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        #Clean up, terminate the created resources
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def __verify_values(self, expected_vals, actual_vals):
        """
        @Desc: Function to verify expected and actual values
        @Steps:
        Step1: Initializing return flag to True
        Step1: Verifying length of expected and actual dictionaries is matching.
               If not matching returning false
        Step2: Listing all the keys from expected dictionary
        Step3: Looping through each key from step2 and verifying expected and actual dictionaries have same value
               If not making return flag to False
        Step4: returning the return flag after all the values are verified
        """
        return_flag = True

        if len(expected_vals) != len(actual_vals):
            return False

        keys = expected_vals.keys()
        for i in range(0, len(expected_vals)):
            exp_val = expected_vals[keys[i]]
            act_val = actual_vals[keys[i]]
            if exp_val == act_val:
                return_flag = return_flag and True
            else:
                return_flag = return_flag and False
                self.debug("expected Value: %s, is not matching with actual value: %s" % (
                                                                                          exp_val,
                                                                                          act_val
                                                                                          ))
        return return_flag

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_01_list_vpncustomergateways_pagination(self):
        """
        @Desc: Test to List VPN Customer Gateways pagination
        @steps:
        Step1: Listing all the VPN Customer Gateways for a user
        Step2: Verifying that no VPN Customer Gateways are listed
        Step3: Creating (page size + 1) number of VPN Customer Gateways
        Step4: Listing all the VPN Customer Gateways again for a user
        Step5: Verifying that list size is (page size + 1)
        Step6: Listing all the VPN Customer Gateways in page1
        Step7: Verifying that list size is (page size)
        Step8: Listing all the VPN Customer Gateways in page2
        Step9: Verifying that list size is 1
        Step10: Deleting the VPN Customer Gateways present in page 2
        Step11: Listing all the VPN Customer Gateways in page2
        Step12: Verifying that no VPN Customer Gateways are listed
        """
        # Listing all the VPN Customer Gateways for a User
        list_vpncustomergateways_before = VpnCustomerGateway.list(
                                                                  self.userapiclient,
                                                                  listall=self.services["listall"]
                                                                  )
        # Verifying that no VPN Customer Gateways are listed
        self.assertIsNone(
                          list_vpncustomergateways_before,
                          "VPN Customer Gateways listed for newly created User"
                          )
        # Creating pagesize + 1 number of VPN Customer Gateways
        for i in range(0, (self.services["pagesize"] + 1)):
            vpncustomergateway_created = VpnCustomerGateway.create(
                                                                   self.userapiclient,
                                                                   self.services["vpncustomergateway"],
                                                                   name="VPNCustGateway"+str(i+1),
                                                                   gateway="10.102.153." + str(i+1),
                                                                   cidrlist="10.0.0.0/24",
                                                                   account=self.account.name,
                                                                   domainid=self.domain.id
                                                                   )
            self.assertIsNotNone(
                                 vpncustomergateway_created,
                                 "VPN Customer Gateway creation failed"
                                 )
            if (i < self.services["pagesize"] + 1):
                self.cleanup.append(vpncustomergateway_created)

        # Listing all the VPN Customer Gateways for a User
        list_vpncustomergateways_after = VpnCustomerGateway.list(
                                                                 self.userapiclient,
                                                                 listall=self.services["listall"]
                                                                 )
        status = validateList(list_vpncustomergateways_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VPN Customer Gateway creation failed"
                          )
        # Verifying that list size is pagesize + 1
        self.assertEquals(
                          self.services["pagesize"] + 1,
                          len(list_vpncustomergateways_after),
                          "Failed to create pagesize + 1 number of VPN Customer Gateways"
                          )
        # Listing all the VPN Customer Gateways in page 1
        list_vpncustomergateways_page1 = VpnCustomerGateway.list(
                                                                 self.userapiclient,
                                                                 listall=self.services["listall"],
                                                                 page=1,
                                                                 pagesize=self.services["pagesize"]
                                                                 )
        status = validateList(list_vpncustomergateways_page1)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Failed to list VPN Customer Gateways in page 1"
                          )
        # Verifying the list size to be equal to pagesize
        self.assertEquals(
                          self.services["pagesize"],
                          len(list_vpncustomergateways_page1),
                          "Size of VPN Customer Gateways in page 1 is not matching"
                          )
        # Listing all the VPN Customer Gateways in page 2
        list_vpncustomergateways_page2 = VpnCustomerGateway.list(
                                                                 self.userapiclient,
                                                                 listall=self.services["listall"],
                                                                 page=2,
                                                                 pagesize=self.services["pagesize"]
                                                                 )
        status = validateList(list_vpncustomergateways_page2)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Failed to list VPN Customer Gateways in page 2"
                          )
        # Verifying the list size to be equal to 1
        self.assertEquals(
                          1,
                          len(list_vpncustomergateways_page2),
                          "Size of VPN Customer Gateways in page 2 is not matching"
                          )
        # Deleting the VPM Customer Gateway present in page 2
        VpnCustomerGateway.delete(
                                  vpncustomergateway_created,
                                  self.userapiclient
                                  )
        # Listing all the VPN Customer Gateways in page 2 again
        list_vpncustomergateways_page2 = VpnCustomerGateway.list(
                                                                 self.userapiclient,
                                                                 listall=self.services["listall"],
                                                                 page=2,
                                                                 pagesize=self.services["pagesize"]
                                                                 )
        # Verifying that there are no VPN Customer Gateways listed
        self.assertIsNone(
                          list_vpncustomergateways_page2,
                          "VPN Customer Gateways not deleted from page 2"
                          )
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_02_update_vpncustomergateways(self):
        """
        @Desc: Test to update VPN Customer Gateways pagination
        @steps:
        Step1: Listing all the VPN Customer Gateways for a user
        Step2: Verifying that no VPN Customer Gateways are listed
        Step3: Creating a VPN Customer Gateways
        Step4: Listing all the VPN Customer Gateways again for a user
        Step5: Verifying that list size is 1
        Step6: Updating the VPN Customer Gateways created in step3
        Step7: Listing the VPN customer gateway by id
        Step8: Verifying that list size is 1
        Step9: Verifying the details of the listed VPN customer gateway are same as updated in step6
        """
        # Listing all the VPN Customer Gateways for a User
        list_vpncustomergateways_before = VpnCustomerGateway.list(
                                                                  self.userapiclient,
                                                                  listall=self.services["listall"]
                                                                  )
        # Verifying that no VPN Customer Gateways are listed
        self.assertIsNone(
                          list_vpncustomergateways_before,
                          "VPN Customer Gateways listed for newly created User"
                          )
        # Creating A VPN Customer Gateways
        vpncustomergateway_created = VpnCustomerGateway.create(
                                                               self.userapiclient,
                                                               self.services["vpncustomergateway"],
                                                               name="VPNCustGateway",
                                                               gateway="10.102.153.90",
                                                               cidrlist="10.0.0.0/24",
                                                               account=self.account.name,
                                                               domainid=self.domain.id
                                                               )
        self.assertIsNotNone(
                             vpncustomergateway_created,
                             "VPN Customer Gateway creation failed"
                             )
        self.cleanup.append(vpncustomergateway_created)
        # Listing all the VPN Customer Gateways for a User
        list_vpncustomergateways_after = VpnCustomerGateway.list(
                                                                 self.userapiclient,
                                                                 listall=self.services["listall"]
                                                                 )
        status = validateList(list_vpncustomergateways_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "VPN Customer Gateway creation failed"
                          )
        # Verifying that list size is 1
        self.assertEquals(
                          1,
                          len(list_vpncustomergateways_after),
                          "Failed to create VPN Customer Gateways"
                          )
        # Updating the VPN Customer gateway
        vpncustomergateway_updated = VpnCustomerGateway.update(
                                                               vpncustomergateway_created,
                                                               self.userapiclient,
                                                               self.services["vpncustomergateway"],
                                                               name="NewVPNCustGateway",
                                                               gateway="10.102.153.90",
                                                               cidrlist="10.0.0.0/24",
                                                               )
        self.assertIsNotNone(
                             vpncustomergateway_updated,
                             "Updation of VPN Customer Gateway failed"
                             )
        # Listing the VPN Customer Gateways by Id
        list_vpncustomergateway = VpnCustomerGateway.list(
                                                          self.userapiclient,
                                                          listall=self.services["listall"],
                                                          id=vpncustomergateway_created.id
                                                          )
        status = validateList(list_vpncustomergateway)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Failed to list VPN Customer Gateways by Id"
                          )
        # Verifying the list size to be equal to 1
        self.assertEquals(
                          1,
                          len(list_vpncustomergateway),
                          "Size of VPN Customer Gateways by id is not matching"
                          )
        # Verifying the details of the listed VPN Customer Gateway are same as updated
        #Creating expected and actual values dictionaries
        expected_dict = {
                         "name":vpncustomergateway_updated.name,
                         "id":vpncustomergateway_updated.id,
                         "account":vpncustomergateway_updated.account,
                         "domainid":vpncustomergateway_updated.domainid,
                         "gateway":vpncustomergateway_updated.gateway,
                         "cidrlist":vpncustomergateway_updated.cidrlist,
                         "seckey":vpncustomergateway_updated.ipsecpsk,
                         "ikepolicy":vpncustomergateway_updated.ikepolicy,
                         "ikelifetime":vpncustomergateway_updated.ikelifetime,
                         "esppolicy":vpncustomergateway_updated.esppolicy,
                         "esplifetime":vpncustomergateway_updated.esplifetime,
                         }
        actual_dict = {
                       "name":list_vpncustomergateway[0].name,
                       "id":list_vpncustomergateway[0].id,
                       "account":list_vpncustomergateway[0].account,
                       "domainid":list_vpncustomergateway[0].domainid,
                       "gateway":list_vpncustomergateway[0].gateway,
                       "cidrlist":list_vpncustomergateway[0].cidrlist,
                       "seckey":list_vpncustomergateway[0].ipsecpsk,
                       "ikepolicy":list_vpncustomergateway[0].ikepolicy,
                       "ikelifetime":list_vpncustomergateway[0].ikelifetime,
                       "esppolicy":list_vpncustomergateway[0].esppolicy,
                       "esplifetime":list_vpncustomergateway[0].esplifetime,
                       }
        vpncustomergateway_status = self.__verify_values(
                                                         expected_dict,
                                                         actual_dict
                                                         )
        self.assertEqual(
                         True,
                         vpncustomergateway_status,
                         "Listed VPN Customer Gateway details are not as Updated"
                         )
        return

class TestTemplates(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(TestTemplates, cls).getClsTestClient()
            cls.api_client = cls.testClient.getApiClient()
            cls.services = cls.testClient.getParsedTestDataConfig()
            # Get Domain, Zone, Template
            cls.domain = get_domain(cls.api_client)
            cls.zone = get_zone(cls.api_client)
            cls.template = get_template(
                                cls.api_client,
                                cls.zone.id,
                                cls.services["ostype"]
                                )
            cls.hypervisor = cls.testClient.getHypervisorInfo()
            cls.services['mode'] = cls.zone.networktype
            cls.account = Account.create(
                                cls.api_client,
                                cls.services["account"],
                                domainid=cls.domain.id
                                )
            # Getting authentication for user in newly created Account
            cls.user = cls.account.user[0]
            cls.userapiclient = cls.testClient.getUserApiClient(cls.user.username, cls.domain.name)
            cls._cleanup.append(cls.account)
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setup : %s" % e)
        return

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        #Clean up, terminate the created resources
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def __verify_values(self, expected_vals, actual_vals):
        """
        @Desc: Function to verify expected and actual values
        @Steps:
        Step1: Initializing return flag to True
        Step1: Verifying length of expected and actual dictionaries is matching.
               If not matching returning false
        Step2: Listing all the keys from expected dictionary
        Step3: Looping through each key from step2 and verifying expected and actual dictionaries have same value
               If not making return flag to False
        Step4: returning the return flag after all the values are verified
        """
        return_flag = True

        if len(expected_vals) != len(actual_vals):
            return False

        keys = expected_vals.keys()
        for i in range(0, len(expected_vals)):
            exp_val = expected_vals[keys[i]]
            act_val = actual_vals[keys[i]]
            if exp_val == act_val:
                return_flag = return_flag and True
            else:
                return_flag = return_flag and False
                self.debug("expected Value: %s, is not matching with actual value: %s" % (
                                                                                          exp_val,
                                                                                          act_val
                                                                                          ))
        return return_flag

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_01_list_templates_pagination(self):
        """
        @Desc: Test to List Templates pagination
        @steps:
        Step1: Listing all the Templates for a user
        Step2: Verifying that no Templates are listed
        Step3: Creating (page size + 1) number of Templates
        Step4: Listing all the Templates again for a user
        Step5: Verifying that list size is (page size + 1)
        Step6: Listing all the Templates in page1
        Step7: Verifying that list size is (page size)
        Step8: Listing all the Templates in page2
        Step9: Verifying that list size is 1
        Step10: Listing the template by Id
        Step11: Verifying if the template is downloaded and ready.
                If yes the continuing
                If not waiting and checking for template to be ready till timeout
        Step12: Deleting the Template present in page 2
        Step13: Listing all the Templates in page2
        Step14: Verifying that no Templates are listed
        """
        # Listing all the Templates for a User
        list_templates_before = Template.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              templatefilter=self.services["templatefilter"]
                                              )
        # Verifying that no Templates are listed
        self.assertIsNone(
                          list_templates_before,
                          "Templates listed for newly created User"
                          )
        self.services["template"]["url"] = "http://10.147.28.7/templates/ttylinux_pv.vhd"
        self.services["template"]["format"] = "VHD"
        self.services["template"]["ostype"] = self.services["ostype"]
        # Creating pagesize + 1 number of Templates
        for i in range(0, (self.services["pagesize"] + 1)):
            template_created = Template.register(
                                                 self.userapiclient,
                                                 self.services["template"],
                                                 self.zone.id,
                                                 hypervisor=self.hypervisor
                                                 )
            self.assertIsNotNone(
                                 template_created,
                                 "Template creation failed"
                                 )
            if(i < self.services["pagesize"]):
                self.cleanup.append(template_created)

        # Listing all the Templates for a User
        list_templates_after = Template.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             templatefilter=self.services["templatefilter"]
                                             )
        status = validateList(list_templates_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Templates creation failed"
                          )
        # Verifying that list size is pagesize + 1
        self.assertEquals(
                          self.services["pagesize"] + 1,
                          len(list_templates_after),
                          "Failed to create pagesize + 1 number of Templates"
                          )
        # Listing all the Templates in page 1
        list_templates_page1 = Template.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             templatefilter=self.services["templatefilter"],
                                             page=1,
                                             pagesize=self.services["pagesize"]
                                             )
        status = validateList(list_templates_page1)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Failed to list Templates in page 1"
                          )
        # Verifying the list size to be equal to pagesize
        self.assertEquals(
                          self.services["pagesize"],
                          len(list_templates_page1),
                          "Size of Templates in page 1 is not matching"
                          )
        # Listing all the Templates in page 2
        list_templates_page2 = Template.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             templatefilter=self.services["templatefilter"],
                                             page=2,
                                             pagesize=self.services["pagesize"]
                                             )
        status = validateList(list_templates_page2)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Failed to list Templates in page 2"
                          )
        # Verifying the list size to be equal to 1
        self.assertEquals(
                          1,
                          len(list_templates_page2),
                          "Size of Templates in page 2 is not matching"
                          )
        # Verifying the state of the template to be ready. If not waiting for state to become ready
        template_ready = False
        count = 0
        while template_ready is False:
            list_template = Template.list(
                                          self.userapiclient,
                                          id=template_created.id,
                                          listall=self.services["listall"],
                                          templatefilter=self.services["templatefilter"],
                                          )
            status = validateList(list_template)
            self.assertEquals(
                              PASS,
                              status[0],
                              "Failed to list Templates by Id"
                              )
            if list_template[0].isready is True:
                template_ready = True
            elif (str(list_template[0].status) == "Error"):
                self.fail("Created Template is in Errored state")
                break
            elif count > 10:
                self.fail("Timed out before Template came into ready state")
                break
            else:
                time.sleep(self.services["sleep"])
                count = count + 1

        # Deleting the Template present in page 2
        Template.delete(
                        template_created,
                        self.userapiclient
                        )
        # Listing all the Templates in page 2 again
        list_templates_page2 = Template.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             templatefilter=self.services["templatefilter"],
                                             page=2,
                                             pagesize=self.services["pagesize"]
                                             )
        # Verifying that there are no Templates listed
        self.assertIsNone(
                          list_templates_page2,
                          "Templates not deleted from page 2"
                          )
        del self.services["template"]["url"]
        del self.services["template"]["format"]
        del self.services["template"]["ostype"]
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_02_download_template(self):
        """
        @Desc: Test to Download Template
        @steps:
        Step1: Listing all the Templates for a user
        Step2: Verifying that no Templates are listed
        Step3: Creating a Templates
        Step4: Listing all the Templates again for a user
        Step5: Verifying that list size is 1
        Step6: Verifying if the template is in ready state.
                If yes the continuing
                If not waiting and checking for template to be ready till timeout
        Step7: Downloading the template (Extract)
        Step8: Verifying that Template is downloaded
        """
        # Listing all the Templates for a User
        list_templates_before = Template.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              templatefilter=self.services["templatefilter"]
                                              )
        # Verifying that no Templates are listed
        self.assertIsNone(
                          list_templates_before,
                          "Templates listed for newly created User"
                          )
        self.services["template"]["url"] = "http://10.147.28.7/templates/ttylinux_pv.vhd"
        self.services["template"]["format"] = "VHD"
        self.services["template"]["ostype"] = self.services["ostype"]
        self.services["template"]["isextractable"] = True
        # Creating aTemplate
        template_created = Template.register(
                                             self.userapiclient,
                                             self.services["template"],
                                             self.zone.id,
                                             hypervisor=self.hypervisor
                                             )
        self.assertIsNotNone(
                             template_created,
                             "Template creation failed"
                             )
        self.cleanup.append(template_created)
        # Listing all the Templates for a User
        list_templates_after = Template.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             templatefilter=self.services["templatefilter"]
                                             )
        status = validateList(list_templates_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Templates creation failed"
                          )
        # Verifying that list size is 1
        self.assertEquals(
                          1,
                          len(list_templates_after),
                          "Failed to create a Template"
                          )
        # Verifying the state of the template to be ready. If not waiting for state to become ready till time out
        template_ready = False
        count = 0
        while template_ready is False:
            list_template = Template.list(
                                          self.userapiclient,
                                          id=template_created.id,
                                          listall=self.services["listall"],
                                          templatefilter=self.services["templatefilter"],
                                          )
            status = validateList(list_template)
            self.assertEquals(
                              PASS,
                              status[0],
                              "Failed to list Templates by Id"
                              )
            if list_template[0].isready is True:
                template_ready = True
            elif (str(list_template[0].status) == "Error"):
                self.fail("Created Template is in Errored state")
                break
            elif count > 10:
                self.fail("Timed out before Template came into ready state")
                break
            else:
                time.sleep(self.services["sleep"])
                count = count + 1

        # Downloading the Template name
        download_template = Template.extract(
                                             self.userapiclient,
                                             template_created.id,
                                             mode="HTTP_DOWNLOAD",
                                             zoneid=self.zone.id
                                             )
        self.assertIsNotNone(
                             download_template,
                             "Download Template failed"
                             )
         # Verifying the details of downloaded template
        self.assertEquals(
                          "DOWNLOAD_URL_CREATED",
                          download_template.state,
                          "Download URL not created for Template"
                          )
        self.assertIsNotNone(
                             download_template.url,
                             "Download URL not created for Template"
                             )
        self.assertEquals(
                          template_created.id,
                          download_template.id,
                          "Download Template details are not same as Template created"
                          )
        del self.services["template"]["url"]
        del self.services["template"]["format"]
        del self.services["template"]["ostype"]
        del self.services["template"]["isextractable"]
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_03_edit_template_details(self):
        """
        @Desc: Test to Edit Template name, displaytext, OSType
        @steps:
        Step1: Listing all the Templates for a user
        Step2: Verifying that no Templates are listed
        Step3: Creating a Templates
        Step4: Listing all the Templates again for a user
        Step5: Verifying that list size is 1
        Step6: Verifying if the template is in ready state.
                If yes the continuing
                If not waiting and checking for template to be ready till timeout
        Step7: Editing the template name
        Step8: Verifying that Template name is edited
        Step9: Editing the template displaytext
        Step10: Verifying that Template displaytext is edited
        Step11: Editing the template ostypeid
        Step12: Verifying that Template ostypeid is edited
        Step13: Editing the template name, displaytext
        Step14: Verifying that Template name, displaytext are edited
        Step15: Editing the template name, displaytext, ostypeid
        Step16: Verifying that Template name, displaytext and ostypeid are edited
        """
        # Listing all the Templates for a User
        list_templates_before = Template.list(
                                              self.userapiclient,
                                              listall=self.services["listall"],
                                              templatefilter=self.services["templatefilter"]
                                              )
        # Verifying that no Templates are listed
        self.assertIsNone(
                          list_templates_before,
                          "Templates listed for newly created User"
                          )
        self.services["template"]["url"] = "http://10.147.28.7/templates/ttylinux_pv.vhd"
        self.services["template"]["format"] = "VHD"
        self.services["template"]["ostype"] = self.services["ostype"]
        # Creating aTemplate
        template_created = Template.register(
                                             self.userapiclient,
                                             self.services["template"],
                                             self.zone.id,
                                             hypervisor=self.hypervisor
                                             )
        self.assertIsNotNone(
                             template_created,
                             "Template creation failed"
                             )
        self.cleanup.append(template_created)
        # Listing all the Templates for a User
        list_templates_after = Template.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             templatefilter=self.services["templatefilter"]
                                             )
        status = validateList(list_templates_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Templates creation failed"
                          )
        # Verifying that list size is 1
        self.assertEquals(
                          1,
                          len(list_templates_after),
                          "Failed to create a Template"
                          )
        # Verifying the state of the template to be ready. If not waiting for state to become ready till time out
        template_ready = False
        count = 0
        while template_ready is False:
            list_template = Template.list(
                                          self.userapiclient,
                                          id=template_created.id,
                                          listall=self.services["listall"],
                                          templatefilter=self.services["templatefilter"],
                                          )
            status = validateList(list_template)
            self.assertEquals(
                              PASS,
                              status[0],
                              "Failed to list Templates by Id"
                              )
            if list_template[0].isready is True:
                template_ready = True
            elif (str(list_template[0].status) == "Error"):
                self.fail("Created Template is in Errored state")
                break
            elif count > 10:
                self.fail("Timed out before Template came into ready state")
                break
            else:
                time.sleep(self.services["sleep"])
                count = count + 1

        # Editing the Template name
        edited_template = Template.update(
                                          template_created,
                                          self.userapiclient,
                                          name="NewTemplateName"
                                          )
        self.assertIsNotNone(
                             edited_template,
                             "Editing Template failed"
                             )
         # Verifying the details of edited template
        expected_dict = {
                         "id":template_created.id,
                         "name":"NewTemplateName",
                         "displaytest":template_created.displaytext,
                         "account":template_created.account,
                         "domainid":template_created.domainid,
                         "format":template_created.format,
                         "ostypeid":template_created.ostypeid,
                         "templatetype":template_created.templatetype,
                         }
        actual_dict = {
                       "id":edited_template.id,
                       "name":edited_template.name,
                       "displaytest":edited_template.displaytext,
                       "account":edited_template.account,
                       "domainid":edited_template.domainid,
                       "format":edited_template.format,
                       "ostypeid":edited_template.ostypeid,
                       "templatetype":edited_template.templatetype,
                       }
        edit_template_status = self.__verify_values(
                                                    expected_dict,
                                                    actual_dict
                                                    )
        self.assertEqual(
                         True,
                         edit_template_status,
                         "Edited Template details are not as expected"
                         )
        # Editing the Template displaytext
        edited_template = Template.update(
                                          template_created,
                                          self.userapiclient,
                                          displaytext="TemplateDisplaytext"
                                          )
        self.assertIsNotNone(
                             edited_template,
                             "Editing Template failed"
                             )
         # Verifying the details of edited template
        expected_dict = {
                         "id":template_created.id,
                         "name":"NewTemplateName",
                         "displaytest":"TemplateDisplaytext",
                         "account":template_created.account,
                         "domainid":template_created.domainid,
                         "format":template_created.format,
                         "ostypeid":template_created.ostypeid,
                         "templatetype":template_created.templatetype,
                         }
        actual_dict = {
                       "id":edited_template.id,
                       "name":edited_template.name,
                       "displaytest":edited_template.displaytext,
                       "account":edited_template.account,
                       "domainid":edited_template.domainid,
                       "format":edited_template.format,
                       "ostypeid":edited_template.ostypeid,
                       "templatetype":edited_template.templatetype,
                       }
        edit_template_status = self.__verify_values(
                                                    expected_dict,
                                                    actual_dict
                                                    )
        self.assertEqual(
                         True,
                         edit_template_status,
                         "Edited Template details are not as expected"
                         )
        # Editing the Template ostypeid
        ostype_list = list_os_types(self.userapiclient)
        status = validateList(ostype_list)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Failed to list OS Types"
                          )
        for i in range(0, len(ostype_list)):
            if ostype_list[i].id != template_created.ostypeid:
                newostypeid = ostype_list[i].id
                break

        edited_template = Template.update(
                                          template_created,
                                          self.userapiclient,
                                          ostypeid=newostypeid
                                          )
        self.assertIsNotNone(
                             edited_template,
                             "Editing Template failed"
                             )
         # Verifying the details of edited template
        expected_dict = {
                         "id":template_created.id,
                         "name":"NewTemplateName",
                         "displaytest":"TemplateDisplaytext",
                         "account":template_created.account,
                         "domainid":template_created.domainid,
                         "format":template_created.format,
                         "ostypeid":newostypeid,
                         "templatetype":template_created.templatetype,
                         }
        actual_dict = {
                       "id":edited_template.id,
                       "name":edited_template.name,
                       "displaytest":edited_template.displaytext,
                       "account":edited_template.account,
                       "domainid":edited_template.domainid,
                       "format":edited_template.format,
                       "ostypeid":edited_template.ostypeid,
                       "templatetype":edited_template.templatetype,
                       }
        edit_template_status = self.__verify_values(
                                                    expected_dict,
                                                    actual_dict
                                                    )
        self.assertEqual(
                         True,
                         edit_template_status,
                         "Edited Template details are not as expected"
                         )
        # Editing the Template name, displaytext
        edited_template = Template.update(
                                          template_created,
                                          self.userapiclient,
                                          name=template_created.name,
                                          displaytext=template_created.displaytext
                                          )
        self.assertIsNotNone(
                             edited_template,
                             "Editing Template failed"
                             )
         # Verifying the details of edited template
        expected_dict = {
                         "id":template_created.id,
                         "name":template_created.name,
                         "displaytest":template_created.displaytext,
                         "account":template_created.account,
                         "domainid":template_created.domainid,
                         "format":template_created.format,
                         "ostypeid":newostypeid,
                         "templatetype":template_created.templatetype,
                         }
        actual_dict = {
                       "id":edited_template.id,
                       "name":edited_template.name,
                       "displaytest":edited_template.displaytext,
                       "account":edited_template.account,
                       "domainid":edited_template.domainid,
                       "format":edited_template.format,
                       "ostypeid":edited_template.ostypeid,
                       "templatetype":edited_template.templatetype,
                       }
        edit_template_status = self.__verify_values(
                                                    expected_dict,
                                                    actual_dict
                                                    )
        self.assertEqual(
                         True,
                         edit_template_status,
                         "Edited Template details are not as expected"
                         )
        # Editing the Template name, displaytext, ostypeid
        edited_template = Template.update(
                                          template_created,
                                          self.userapiclient,
                                          name="NewTemplateName",
                                          displaytext="TemplateDisplaytext",
                                          ostypeid=template_created.ostypeid
                                          )
        self.assertIsNotNone(
                             edited_template,
                             "Editing Template failed"
                             )
         # Verifying the details of edited template
        expected_dict = {
                         "id":template_created.id,
                         "name":"NewTemplateName",
                         "displaytest":"TemplateDisplaytext",
                         "account":template_created.account,
                         "domainid":template_created.domainid,
                         "format":template_created.format,
                         "ostypeid":template_created.ostypeid,
                         "templatetype":template_created.templatetype,
                         }
        actual_dict = {
                       "id":edited_template.id,
                       "name":edited_template.name,
                       "displaytest":edited_template.displaytext,
                       "account":edited_template.account,
                       "domainid":edited_template.domainid,
                       "format":edited_template.format,
                       "ostypeid":edited_template.ostypeid,
                       "templatetype":edited_template.templatetype,
                       }
        edit_template_status = self.__verify_values(
                                                    expected_dict,
                                                    actual_dict
                                                    )
        self.assertEqual(
                         True,
                         edit_template_status,
                         "Edited Template details are not as expected"
                         )
        del self.services["template"]["url"]
        del self.services["template"]["format"]
        del self.services["template"]["ostype"]
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_04_copy_template(self):
        """
        @Desc: Test to copy Template from one zone to another
        @steps:
        Step1: Listing Zones available for a user
        Step2: Verifying if the zones listed are greater than 1.
               If Yes continuing.
               If not halting the test.
        Step3: Listing all the templates for a user in zone1
        Step4: Verifying that no templates are listed
        Step5: Listing all the templates for a user in zone2
        Step6: Verifying that no templates are listed
        Step7: Creating a Template in zone 1
        Step8: Listing all the Templates again for a user in zone1
        Step9: Verifying that list size is 1
        Step10: Listing all the templates for a user in zone2
        Step11: Verifying that no templates are listed
        Step12: Copying the template created in step7 from zone1 to zone2
        Step13: Listing all the templates for a user in zone2
        Step14: Verifying that list size is 1
        Step15: Listing all the Templates for a user in zone1
        Step16: Verifying that list size is 1
        """
        # Listing Zones available for a user
        zones_list = Zone.list(
                               self.userapiclient,
                               available=True
                               )
        status = validateList(zones_list)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Failed to list Zones"
                          )
        if not len(zones_list) > 1:
            self.fail("Enough zones doesnot exists to copy template")
        else:
            # Listing all the Templates for a User in Zone 1
            list_templates_zone1 = Template.list(
                                                 self.userapiclient,
                                                 listall=self.services["listall"],
                                                 templatefilter=self.services["templatefilter"],
                                                 zoneid=zones_list[0].id
                                                 )
            # Verifying that no Templates are listed
            self.assertIsNone(
                              list_templates_zone1,
                              "Templates listed for newly created User in Zone1"
                              )
            # Listing all the Templates for a User in Zone 2
            list_templates_zone2 = Template.list(
                                                 self.userapiclient,
                                                 listall=self.services["listall"],
                                                 templatefilter=self.services["templatefilter"],
                                                 zoneid=zones_list[1].id
                                                 )
            # Verifying that no Templates are listed
            self.assertIsNone(
                              list_templates_zone2,
                              "Templates listed for newly created User in Zone2"
                              )
            self.services["template"]["url"] = "http://10.147.28.7/templates/ttylinux_pv.vhd"
            self.services["template"]["format"] = "VHD"
            self.services["template"]["ostype"] = self.services["ostype"]
            #Listing Hypervisors in Zone 1
            hypervisor_list = Hypervisor.list(
                                              self.apiClient,
                                              zoneid=zones_list[0].id
                                              )
            status = validateList(zones_list)
            self.assertEquals(
                              PASS,
                              status[0],
                              "Failed to list Hypervisors in Zone 1"
                              )
            # Creating aTemplate in Zone 1
            template_created = Template.register(
                                                 self.userapiclient,
                                                 self.services["template"],
                                                 zones_list[0].id,
                                                 hypervisor=hypervisor_list[0].name
                                                 )
            self.assertIsNotNone(
                                 template_created,
                                 "Template creation failed"
                                 )
            self.cleanup.append(template_created)
            # Listing all the Templates for a User in Zone 1
            list_templates_zone1 = Template.list(
                                                 self.userapiclient,
                                                 listall=self.services["listall"],
                                                 templatefilter=self.services["templatefilter"],
                                                 zoneid=zones_list[0].id
                                                 )
            status = validateList(list_templates_zone1)
            self.assertEquals(
                              PASS,
                              status[0],
                              "Templates creation failed in Zone1"
                              )
            # Verifying that list size is 1
            self.assertEquals(
                              1,
                              len(list_templates_zone1),
                              "Failed to create a Template"
                              )
            # Listing all the Templates for a User in Zone 2
            list_templates_zone2 = Template.list(
                                                 self.userapiclient,
                                                 listall=self.services["listall"],
                                                 templatefilter=self.services["templatefilter"],
                                                 zoneid=zones_list[1].id
                                                 )
            # Verifying that no Templates are listed
            self.assertIsNone(
                              list_templates_zone2,
                              "Templates listed for newly created User in Zone2"
                              )
            # Verifying the state of the template to be ready. If not waiting for state to become ready till time out
            template_ready = False
            count = 0
            while template_ready is False:
                list_template = Template.list(
                                              self.userapiclient,
                                              id=template_created.id,
                                              listall=self.services["listall"],
                                              templatefilter=self.services["templatefilter"],
                                              )
                status = validateList(list_template)
                self.assertEquals(
                                  PASS,
                                  status[0],
                                  "Failed to list Templates by Id"
                                  )
                if list_template[0].isready is True:
                    template_ready = True
                elif (str(list_template[0].status) == "Error"):
                    self.fail("Created Template is in Errored state")
                    break
                elif count > 10:
                    self.fail("Timed out before Template came into ready state")
                    break
                else:
                    time.sleep(self.services["sleep"])
                    count = count + 1

            # Copying the Template from Zone1 to Zone2
            copied_template = Template.copy(
                                            self.userapiclient,
                                            template_created.id,
                                            sourcezoneid=template_created.zoneid,
                                            destzoneid=zones_list[1].id
                                            )
            self.assertIsNotNone(
                                 copied_template,
                                 "Copying Template from Zone1 to Zone2 failed"
                                 )
            # Listing all the Templates for a User in Zone 1
            list_templates_zone1 = Template.list(
                                                 self.userapiclient,
                                                 listall=self.services["listall"],
                                                 templatefilter=self.services["templatefilter"],
                                                 zoneid=zones_list[0].id
                                                 )
            status = validateList(list_templates_zone1)
            self.assertEquals(
                              PASS,
                              status[0],
                              "Templates creation failed in Zone1"
                              )
            # Verifying that list size is 1
            self.assertEquals(
                              1,
                              len(list_templates_zone1),
                              "Failed to create a Template"
                              )
            # Listing all the Templates for a User in Zone 2
            list_templates_zone2 = Template.list(
                                                 self.userapiclient,
                                                 listall=self.services["listall"],
                                                 templatefilter=self.services["templatefilter"],
                                                 zoneid=zones_list[1].id
                                                 )
            status = validateList(list_templates_zone2)
            self.assertEquals(
                              PASS,
                              status[0],
                              "Template failed to copy into Zone2"
                              )
            # Verifying that list size is 1
            self.assertEquals(
                              1,
                              len(list_templates_zone2),
                              "Template failed to copy into Zone2"
                              )
            self.assertNotEquals(
                                 "Connection refused",
                                 list_templates_zone2[0].status,
                                 "Failed to copy Template"
                                 )
            self.assertEquals(
                              True,
                              list_templates_zone2[0].isready,
                              "Failed to copy Template"
                              )
        del self.services["template"]["url"]
        del self.services["template"]["format"]
        del self.services["template"]["ostype"]
        return

class TestIsos(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(TestIsos, cls).getClsTestClient()
            cls.api_client = cls.testClient.getApiClient()
            cls.services = cls.testClient.getParsedTestDataConfig()
            # Get Domain, Zone, Template
            cls.domain = get_domain(cls.api_client)
            cls.zone = get_zone(cls.api_client)
            cls.template = get_template(
                                cls.api_client,
                                cls.zone.id,
                                cls.services["ostype"]
                                )
            cls.hypervisor = cls.testClient.getHypervisorInfo()
            cls.services['mode'] = cls.zone.networktype
            cls.account = Account.create(
                                cls.api_client,
                                cls.services["account"],
                                domainid=cls.domain.id
                                )
            # Getting authentication for user in newly created Account
            cls.user = cls.account.user[0]
            cls.userapiclient = cls.testClient.getUserApiClient(cls.user.username, cls.domain.name)
            cls._cleanup.append(cls.account)
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setup : %s" % e)
        return

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        #Clean up, terminate the created resources
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def __verify_values(self, expected_vals, actual_vals):
        """
        @Desc: Function to verify expected and actual values
        @Steps:
        Step1: Initializing return flag to True
        Step1: Verifying length of expected and actual dictionaries is matching.
               If not matching returning false
        Step2: Listing all the keys from expected dictionary
        Step3: Looping through each key from step2 and verifying expected and actual dictionaries have same value
               If not making return flag to False
        Step4: returning the return flag after all the values are verified
        """
        return_flag = True

        if len(expected_vals) != len(actual_vals):
            return False

        keys = expected_vals.keys()
        for i in range(0, len(expected_vals)):
            exp_val = expected_vals[keys[i]]
            act_val = actual_vals[keys[i]]
            if exp_val == act_val:
                return_flag = return_flag and True
            else:
                return_flag = return_flag and False
                self.debug("expected Value: %s, is not matching with actual value: %s" % (
                                                                                          exp_val,
                                                                                          act_val
                                                                                          ))
        return return_flag

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_01_list_isos_pagination(self):
        """
        @Desc: Test to List ISO's pagination
        @steps:
        Step1: Listing all the ISO's for a user
        Step2: Verifying that no ISO's are listed
        Step3: Creating (page size + 1) number of ISO's
        Step4: Listing all the ISO's again for a user
        Step5: Verifying that list size is (page size + 1)
        Step6: Listing all the ISO's in page1
        Step7: Verifying that list size is (page size)
        Step8: Listing all the ISO's in page2
        Step9: Verifying that list size is 1
        Step10: Listing the ISO's by Id
        Step11: Verifying if the ISO is downloaded and ready.
                If yes the continuing
                If not waiting and checking for iso to be ready till timeout
        Step12: Deleting the ISO present in page 2
        Step13: Listing all the ISO's in page2
        Step14: Verifying that no ISO's are listed
        """
        # Listing all the ISO's for a User
        list_iso_before = Iso.list(
                                   self.userapiclient,
                                   listall=self.services["listall"],
                                   isofilter=self.services["templatefilter"]
                                   )
        # Verifying that no ISOs are listed
        self.assertIsNone(
                          list_iso_before,
                          "ISOs listed for newly created User"
                          )
        self.services["iso"]["zoneid"] = self.zone.id
        # Creating pagesize + 1 number of ISO's
        for i in range(0, (self.services["pagesize"] + 1)):
            iso_created = Iso.create(
                                     self.userapiclient,
                                     self.services["iso"]
                                     )
            self.assertIsNotNone(
                                 iso_created,
                                 "ISO creation failed"
                                 )
            if(i < self.services["pagesize"]):
                self.cleanup.append(iso_created)

        # Listing all the ISO's for a User
        list_iso_after = Iso.list(
                                  self.userapiclient,
                                  listall=self.services["listall"],
                                  isofilter=self.services["templatefilter"]
                                  )
        status = validateList(list_iso_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "ISO's creation failed"
                          )
        # Verifying that list size is pagesize + 1
        self.assertEquals(
                          self.services["pagesize"] + 1,
                          len(list_iso_after),
                          "Failed to create pagesize + 1 number of ISO's"
                          )
        # Listing all the ISO's in page 1
        list_iso_page1 = Iso.list(
                                  self.userapiclient,
                                  listall=self.services["listall"],
                                  isofilter=self.services["templatefilter"],
                                  page=1,
                                  pagesize=self.services["pagesize"]
                                  )
        status = validateList(list_iso_page1)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Failed to list ISO's in page 1"
                          )
        # Verifying the list size to be equal to pagesize
        self.assertEquals(
                          self.services["pagesize"],
                          len(list_iso_page1),
                          "Size of ISO's in page 1 is not matching"
                          )
        # Listing all the Templates in page 2
        list_iso_page2 = Iso.list(
                                  self.userapiclient,
                                  listall=self.services["listall"],
                                  isofilter=self.services["templatefilter"],
                                  page=2,
                                  pagesize=self.services["pagesize"]
                                  )
        status = validateList(list_iso_page2)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Failed to list ISo's in page 2"
                          )
        # Verifying the list size to be equal to 1
        self.assertEquals(
                          1,
                          len(list_iso_page2),
                          "Size of ISO's in page 2 is not matching"
                          )
        # Verifying the state of the ISO to be ready. If not waiting for state to become ready
        iso_ready = False
        count = 0
        while iso_ready is False:
            list_iso = Iso.list(
                                self.userapiclient,
                                listall=self.services["listall"],
                                isofilter=self.services["templatefilter"],
                                id=iso_created.id
                                )
            status = validateList(list_iso)
            self.assertEquals(
                              PASS,
                              status[0],
                              "Failed to list ISO by Id"
                              )
            if list_iso[0].isready is True:
                iso_ready = True
            elif (str(list_iso[0].status) == "Error"):
                self.fail("Created ISO is in Errored state")
                break
            elif count > 10:
                self.fail("Timed out before ISO came into ready state")
                break
            else:
                time.sleep(self.services["sleep"])
                count = count + 1

        # Deleting the ISO present in page 2
        Iso.delete(
                   iso_created,
                   self.userapiclient
                   )
        # Listing all the ISO's in page 2 again
        list_iso_page2 = Iso.list(
                                  self.userapiclient,
                                  listall=self.services["listall"],
                                  isofilter=self.services["templatefilter"],
                                  page=2,
                                  pagesize=self.services["pagesize"]
                                  )
        # Verifying that there are no ISO's listed
        self.assertIsNone(
                          list_iso_page2,
                          "ISO's not deleted from page 2"
                          )
        del self.services["iso"]["zoneid"]
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_02_download_iso(self):
        """
        @Desc: Test to Download ISO
        @steps:
        Step1: Listing all the ISO's for a user
        Step2: Verifying that no ISO's are listed
        Step3: Creating an ISO
        Step4: Listing all the ISO's again for a user
        Step5: Verifying that list size is 1
        Step6: Verifying if the ISO is in ready state.
                If yes the continuing
                If not waiting and checking for template to be ready till timeout
        Step7: Downloading the ISO (Extract)
        Step8: Verifying the details of downloaded ISO
        """
        # Listing all the ISO's for a User
        list_iso_before = Iso.list(
                                   self.userapiclient,
                                   listall=self.services["listall"],
                                   isofilter=self.services["templatefilter"]
                                   )
        # Verifying that no ISOs are listed
        self.assertIsNone(
                          list_iso_before,
                          "ISOs listed for newly created User"
                          )
        self.services["iso"]["zoneid"] = self.zone.id
        self.services["iso"]["isextractable"] = True
        # Creating an ISO's
        iso_created = Iso.create(
                                 self.userapiclient,
                                 self.services["iso"]
                                 )
        self.assertIsNotNone(
                             iso_created,
                             "ISO creation failed"
                             )
        self.cleanup.append(iso_created)
        # Listing all the ISO's for a User
        list_iso_after = Iso.list(
                                  self.userapiclient,
                                  listall=self.services["listall"],
                                  isofilter=self.services["templatefilter"]
                                  )
        status = validateList(list_iso_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "ISO's creation failed"
                          )
        # Verifying that list size is 1
        self.assertEquals(
                          1,
                          len(list_iso_after),
                          "Failed to create an ISO's"
                          )
        # Verifying the state of the ISO to be ready. If not waiting for state to become ready
        iso_ready = False
        count = 0
        while iso_ready is False:
            list_iso = Iso.list(
                                self.userapiclient,
                                listall=self.services["listall"],
                                isofilter=self.services["templatefilter"],
                                id=iso_created.id
                                )
            status = validateList(list_iso)
            self.assertEquals(
                              PASS,
                              status[0],
                              "Failed to list ISO by Id"
                              )
            if list_iso[0].isready is True:
                iso_ready = True
            elif (str(list_iso[0].status) == "Error"):
                self.fail("Created ISO is in Errored state")
                break
            elif count > 10:
                self.fail("Timed out before ISO came into ready state")
                break
            else:
                time.sleep(self.services["sleep"])
                count = count + 1

        # Downloading the ISO
        download_iso = Iso.extract(
                                   self.userapiclient,
                                   iso_created.id,
                                   mode="HTTP_DOWNLOAD",
                                   zoneid=self.zone.id
                                   )
        self.assertIsNotNone(
                             download_iso,
                             "Download ISO failed"
                             )
         # Verifying the details of downloaded ISO
        self.assertEquals(
                          "DOWNLOAD_URL_CREATED",
                          download_iso.state,
                          "Download URL not created for ISO"
                          )
        self.assertIsNotNone(
                             download_iso.url,
                             "Download URL not created for ISO"
                             )
        self.assertEquals(
                          iso_created.id,
                          download_iso.id,
                          "Download ISO details are not same as ISO created"
                          )
        del self.services["iso"]["zoneid"]
        del self.services["iso"]["isextractable"]
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_03_edit_iso_details(self):
        """
        @Desc: Test to Edit ISO name, displaytext, OSType
        @steps:
        Step1: Listing all the ISO's for a user
        Step2: Verifying that no ISO's are listed
        Step3: Creating an ISO
        Step4: Listing all the ISO's again for a user
        Step5: Verifying that list size is 1
        Step6: Verifying if the ISO is in ready state.
                If yes the continuing
                If not waiting and checking for template to be ready till timeout
        Step7: Editing the ISO's name, displaytext
        Step8: Verifying that ISO name and displaytext are edited
        Step9: Editing the ISO name, displaytext, ostypeid
        Step10: Verifying that ISO name, displaytext and ostypeid are edited
        """
        # Listing all the ISO's for a User
        list_iso_before = Iso.list(
                                   self.userapiclient,
                                   listall=self.services["listall"],
                                   isofilter=self.services["templatefilter"]
                                   )
        # Verifying that no ISOs are listed
        self.assertIsNone(
                          list_iso_before,
                          "ISOs listed for newly created User"
                          )
        self.services["iso"]["zoneid"] = self.zone.id
        # Creating an ISO's
        iso_created = Iso.create(
                                 self.userapiclient,
                                 self.services["iso"]
                                 )
        self.assertIsNotNone(
                             iso_created,
                             "ISO creation failed"
                             )
        self.cleanup.append(iso_created)
        # Listing all the ISO's for a User
        list_iso_after = Iso.list(
                                  self.userapiclient,
                                  listall=self.services["listall"],
                                  isofilter=self.services["templatefilter"]
                                  )
        status = validateList(list_iso_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "ISO's creation failed"
                          )
        # Verifying that list size is 1
        self.assertEquals(
                          1,
                          len(list_iso_after),
                          "Failed to create an ISO's"
                          )
        # Verifying the state of the ISO to be ready. If not waiting for state to become ready
        iso_ready = False
        count = 0
        while iso_ready is False:
            list_iso = Iso.list(
                                self.userapiclient,
                                listall=self.services["listall"],
                                isofilter=self.services["templatefilter"],
                                id=iso_created.id
                                )
            status = validateList(list_iso)
            self.assertEquals(
                              PASS,
                              status[0],
                              "Failed to list ISO by Id"
                              )
            if list_iso[0].isready is True:
                iso_ready = True
            elif (str(list_iso[0].status) == "Error"):
                self.fail("Created ISO is in Errored state")
                break
            elif count > 10:
                self.fail("Timed out before ISO came into ready state")
                break
            else:
                time.sleep(self.services["sleep"])
                count = count + 1

        # Editing the ISO name, displaytext
        edited_iso = Iso.update(
                                iso_created,
                                self.userapiclient,
                                name="NewISOName",
                                displaytext="NewISODisplayText"
                                )
        self.assertIsNotNone(
                             edited_iso,
                             "Editing ISO failed"
                             )
         # Verifying the details of edited template
        expected_dict = {
                         "id":iso_created.id,
                         "name":"NewISOName",
                         "displaytest":"NewISODisplayText",
                         "account":iso_created.account,
                         "domainid":iso_created.domainid,
                         "isfeatured":iso_created.isfeatured,
                         "ostypeid":iso_created.ostypeid,
                         "ispublic":iso_created.ispublic,
                         }
        actual_dict = {
                       "id":edited_iso.id,
                       "name":edited_iso.name,
                       "displaytest":edited_iso.displaytext,
                       "account":edited_iso.account,
                       "domainid":edited_iso.domainid,
                       "isfeatured":edited_iso.isfeatured,
                       "ostypeid":edited_iso.ostypeid,
                       "ispublic":edited_iso.ispublic,
                       }
        edit_iso_status = self.__verify_values(
                                               expected_dict,
                                               actual_dict
                                               )
        self.assertEqual(
                         True,
                         edit_iso_status,
                         "Edited ISO details are not as expected"
                         )
        # Editing the ISO name, displaytext, ostypeid
        ostype_list = list_os_types(self.userapiclient)
        status = validateList(ostype_list)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Failed to list OS Types"
                          )
        for i in range(0, len(ostype_list)):
            if ostype_list[i].id != iso_created.ostypeid:
                newostypeid = ostype_list[i].id
                break

        edited_iso = Iso.update(
                                iso_created,
                                self.userapiclient,
                                name=iso_created.name,
                                displaytext=iso_created.displaytext,
                                ostypeid=newostypeid
                                )
        self.assertIsNotNone(
                             edited_iso,
                             "Editing ISO failed"
                             )
        # Verifying the details of edited template
        expected_dict = {
                         "id":iso_created.id,
                         "name":iso_created.name,
                         "displaytest":iso_created.displaytext,
                         "account":iso_created.account,
                         "domainid":iso_created.domainid,
                         "isfeatured":iso_created.isfeatured,
                         "ostypeid":newostypeid,
                         "ispublic":iso_created.ispublic,
                         }
        actual_dict = {
                       "id":edited_iso.id,
                       "name":edited_iso.name,
                       "displaytest":edited_iso.displaytext,
                       "account":edited_iso.account,
                       "domainid":edited_iso.domainid,
                       "isfeatured":edited_iso.isfeatured,
                       "ostypeid":edited_iso.ostypeid,
                       "ispublic":edited_iso.ispublic,
                       }
        edit_iso_status = self.__verify_values(
                                               expected_dict,
                                               actual_dict
                                               )
        self.assertEqual(
                         True,
                         edit_iso_status,
                         "Edited ISO details are not as expected"
                         )
        del self.services["iso"]["zoneid"]
        return

    @attr(tags=["advanced", "basic", "provisioning"])
    def test_04_copy_iso(self):
        """
        @Desc: Test to copy ISO from one zone to another
        @steps:
        Step1: Listing Zones available for a user
        Step2: Verifying if the zones listed are greater than 1.
               If Yes continuing.
               If not halting the test.
        Step3: Listing all the ISO's for a user in zone1
        Step4: Verifying that no ISO's are listed
        Step5: Listing all the ISO's for a user in zone2
        Step6: Verifying that no ISO's are listed
        Step7: Creating an ISO in zone 1
        Step8: Listing all the ISO's again for a user in zone1
        Step9: Verifying that list size is 1
        Step10: Listing all the ISO's for a user in zone2
        Step11: Verifying that no ISO's are listed
        Step12: Copying the ISO created in step7 from zone1 to zone2
        Step13: Listing all the ISO's for a user in zone2
        Step14: Verifying that list size is 1
        Step15: Listing all the ISO's for a user in zone1
        Step16: Verifying that list size is 1
        """
        # Listing Zones available for a user
        zones_list = Zone.list(
                               self.userapiclient,
                               available=True
                               )
        status = validateList(zones_list)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Failed to list Zones"
                          )
        if not len(zones_list) > 1:
            self.fail("Enough zones doesnot exists to copy iso")
        else:
            # Listing all the ISO's for a User in Zone 1
            list_isos_zone1 = Iso.list(
                                       self.userapiclient,
                                       listall=self.services["listall"],
                                       isofilter=self.services["templatefilter"],
                                       zoneid=zones_list[0].id
                                       )
            # Verifying that no ISO's are listed
            self.assertIsNone(
                              list_isos_zone1,
                              "ISO's listed for newly created User in Zone1"
                              )
            # Listing all the ISO's for a User in Zone 2
            list_isos_zone2 = Iso.list(
                                       self.userapiclient,
                                       listall=self.services["listall"],
                                       isofilter=self.services["templatefilter"],
                                       zoneid=zones_list[1].id
                                       )
            # Verifying that no ISO's are listed
            self.assertIsNone(
                              list_isos_zone2,
                              "ISO's listed for newly created User in Zone2"
                              )
            self.services["iso"]["zoneid"] = zones_list[0].id
            # Creating an ISO in Zone 1
            iso_created = Iso.create(
                                     self.userapiclient,
                                     self.services["iso"]
                                     )
            self.assertIsNotNone(
                                 iso_created,
                                 "ISO creation failed"
                                 )
            self.cleanup.append(iso_created)
            # Listing all the ISO's for a User in Zone 1
            list_isos_zone1 = Iso.list(
                                       self.userapiclient,
                                       listall=self.services["listall"],
                                       isofilter=self.services["templatefilter"],
                                       zoneid=zones_list[0].id
                                       )
            status = validateList(list_isos_zone1)
            self.assertEquals(
                              PASS,
                              status[0],
                              "ISO creation failed in Zone1"
                              )
            # Verifying that list size is 1
            self.assertEquals(
                              1,
                              len(list_isos_zone1),
                              "Failed to create a Template"
                              )
            # Listing all the ISO's for a User in Zone 2
            list_isos_zone2 = Iso.list(
                                       self.userapiclient,
                                       listall=self.services["listall"],
                                       isofilter=self.services["templatefilter"],
                                       zoneid=zones_list[1].id
                                       )
            # Verifying that no ISO's are listed
            self.assertIsNone(
                              list_isos_zone2,
                              "ISO's listed for newly created User in Zone2"
                              )
            # Verifying the state of the ISO to be ready. If not waiting for state to become ready
            iso_ready = False
            count = 0
            while iso_ready is False:
                list_iso = Iso.list(
                                    self.userapiclient,
                                    listall=self.services["listall"],
                                    isofilter=self.services["templatefilter"],
                                    id=iso_created.id
                                    )
                status = validateList(list_iso)
                self.assertEquals(
                                  PASS,
                                  status[0],
                                  "Failed to list ISO by Id"
                                  )
                if list_iso[0].isready is True:
                    iso_ready = True
                elif (str(list_iso[0].status) == "Error"):
                    self.fail("Created ISO is in Errored state")
                    break
                elif count > 10:
                    self.fail("Timed out before ISO came into ready state")
                    break
                else:
                    time.sleep(self.services["sleep"])
                    count = count + 1

            # Copying the ISO from Zone1 to Zone2
            copied_iso = Iso.copy(
                                  self.userapiclient,
                                  iso_created.id,
                                  sourcezoneid=iso_created.zoneid,
                                  destzoneid=zones_list[1].id
                                  )
            self.assertIsNotNone(
                                 copied_iso,
                                 "Copying ISO from Zone1 to Zone2 failed"
                                 )
            # Listing all the ISO's for a User in Zone 1
            list_isos_zone1 = Iso.list(
                                       self.userapiclient,
                                       listall=self.services["listall"],
                                       isofilter=self.services["templatefilter"],
                                       zoneid=zones_list[0].id
                                       )
            status = validateList(list_isos_zone1)
            self.assertEquals(
                              PASS,
                              status[0],
                              "ISO creation failed in Zone1"
                              )
            # Verifying that list size is 1
            self.assertEquals(
                              1,
                              len(list_isos_zone1),
                              "Failed to create a Template"
                              )
            # Listing all the ISO's for a User in Zone 2
            list_isos_zone2 = Iso.list(
                                       self.userapiclient,
                                       listall=self.services["listall"],
                                       isofilter=self.services["templatefilter"],
                                       zoneid=zones_list[1].id
                                       )
            status = validateList(list_isos_zone2)
            self.assertEquals(
                              PASS,
                              status[0],
                              "ISO failed to copy into Zone2"
                              )
            # Verifying that list size is 1
            self.assertEquals(
                              1,
                              len(list_isos_zone2),
                              "ISO failed to copy into Zone2"
                              )
            self.assertNotEquals(
                                 "Connection refused",
                                 list_isos_zone2[0].status,
                                 "Failed to copy ISO"
                                 )
            self.assertEquals(
                              True,
                              list_isos_zone2[0].isready,
                              "Failed to copy ISO"
                              )
        del self.services["iso"]["zoneid"]
        return

class TestNetworks(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(TestNetworks, cls).getClsTestClient()
            cls.api_client = cls.testClient.getApiClient()
            cls.services = cls.testClient.getParsedTestDataConfig()
            # Get Domain, Zone, Template
            cls.domain = get_domain(cls.api_client)
            cls.zone = get_zone(cls.api_client)
            cls.template = get_template(
                                cls.api_client,
                                cls.zone.id,
                                cls.services["ostype"]
                                )
            if cls.zone.localstorageenabled:
                cls.storagetype = 'local'
                cls.services["service_offerings"]["tiny"]["storagetype"] = 'local'
            else:
                cls.storagetype = 'shared'
                cls.services["service_offerings"]["tiny"]["storagetype"] = 'shared'

            cls.services['mode'] = cls.zone.networktype
            cls.services["virtual_machine"]["hypervisor"] = cls.testClient.getHypervisorInfo()
            cls.services["virtual_machine"]["zoneid"] = cls.zone.id
            cls.services["virtual_machine"]["template"] = cls.template.id
            cls.services["network_without_acl"]["zoneid"] = cls.zone.id
            # Create Network offering
            cls.network_offering = NetworkOffering.create(
                                        cls.api_client,
                                        cls.services["network_offering_vlan"],
                                        )
            # Enable Network offering
            cls.network_offering.update(cls.api_client, state='Enabled')
            cls.services["network_without_acl"]["networkoffering"] = cls.network_offering.id
            cls.service_offering = ServiceOffering.create(
                                          cls.api_client,
                                          cls.services["service_offerings"]["tiny"]
                                          )
            # Creating Disk offering, Service Offering and Account
            cls.account = Account.create(
                                cls.api_client,
                                cls.services["account"],
                                domainid=cls.domain.id
                                )
            # Getting authentication for user in newly created Account
            cls.user = cls.account.user[0]
            cls.userapiclient = cls.testClient.getUserApiClient(cls.user.username, cls.domain.name)
            cls.account_network = Network.create(
                                                 cls.userapiclient,
                                                 cls.services["network_without_acl"],
                                                 cls.account.name,
                                                 cls.account.domainid
                                                 )
            cls._cleanup.append(cls.account_network)
            cls._cleanup.append(cls.account)
            cls._cleanup.append(cls.service_offering)
            cls._cleanup.append(cls.network_offering)
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setup : %s" % e)
        return

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()
        self.cleanup = []

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
        Step1: Verifying length of expected and actual dictionaries is matching.
               If not matching returning false
        Step2: Listing all the keys from expected dictionary
        Step3: Looping through each key from step2 and verifying expected and actual dictionaries have same value
               If not making return flag to False
        Step4: returning the return flag after all the values are verified
        """
        return_flag = True

        if len(expected_vals) != len(actual_vals):
            return False

        keys = expected_vals.keys()
        for i in range(0, len(expected_vals)):
            exp_val = expected_vals[keys[i]]
            act_val = actual_vals[keys[i]]
            if exp_val == act_val:
                return_flag = return_flag and True
            else:
                return_flag = return_flag and False
                self.debug("expected Value: %s, is not matching with actual value: %s" % (
                                                                                          exp_val,
                                                                                          act_val
                                                                                          ))
        return return_flag

    @attr(tags=["advanced"])
    def test_01_list_networks_pagination(self):
        """
        @Desc: Test List Networks pagination  
        @Steps 
        Step1    : Listing the networks for a user
        Step2    : Verifying listed networks for account created at class level
        Step3    : If number of networks is less than (pagesize + 1), then creating them
        Step4    : Listing the networks again
        Step5    : Verifying for the length of the networks that it is (pagesize + 1)
        Step6    : Listing and verifying all the networks in page1
        Step7    : Listing and verifying all the networks in page2
        Step8    : Verifying that on page 2 only 1 network is present and the network on page 2 is not present in page1
        """
        list_zones = Zone.list(
                                 self.userapiclient,
                                 id=self.zone.id
                               )
        status = validateList(list_zones)
        self.assertEquals(PASS, status[0], "No Zones found for a given id")
        self.services["network_without_acl"]["zoneid"] = list_zones[0].id   
        # Listing the networks for a user
        list_networks_before = Network.list(
                                            self.userapiclient, 
                                            listall=self.services["listall"]
                                            )
        # Verifying listed networks for account created at class level
        if list_networks_before is None:
            self.assertEqual(
                             len(list_networks_before),
                             0,
                             "Network create failed at class level"
                             )
        # If number of networks is less than (pagesize + 1), then creating network    
        elif len(list_networks_before) == 1:
            for i in range(0, (self.services["pagesize"])):
                network_created = Network.create(
                                             self.userapiclient,
                                             self.services["network_without_acl"],
                                             )
                self.cleanup.append(network_created)
                self.assertIsNotNone(
                                     network_created,
                                     "Network is not created"
                                     )
                # Creating expected and actual values dictionaries
                expected_dict = {
                                   "id":list_zones[0].id,
                                   "name":self.services["network_without_acl"]["name"],
                                   }
                actual_dict = {
                                   "id":network_created.zoneid,
                                   "name":network_created.name,
                                   }
                network_status = self.__verify_values(
                                                          expected_dict,
                                                          actual_dict
                                                          )
                self.assertEqual(
                                 True,
                                 network_status,
                                 "Listed network details are not as expected"
                                 )
        else:
            self.assertEqual(
                             len(list_networks_before),
                             1,
                             "more than 1 network created at class level"
                             )
        # Listing the networks  
        list_networks_after = Network.list(self.userapiclient, listall=self.services["listall"])   
        status = validateList(list_networks_after)
        self.assertEquals(PASS, status[0], "No networks found using list call")
        # Asserting for the length of the networks
        self.assertEqual(
                         len(list_networks_after),
                         (self.services["pagesize"] + 1),
                         "Number of networks created is not matching expected"
                         )
        # Listing all the networks in page1
        list_networks_page1 = Network.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             page=1,
                                             pagesize=self.services["pagesize"]
                                             )
        status = validateList(list_networks_page1)
        self.assertEquals(PASS, status[0], "No networks found at page 1")
        self.assertEqual(
                         len(list_networks_page1),
                         self.services["pagesize"],
                         "List network response is not matching with the page size length for page 1"
                         )
        # Listing all the networks in page2
        list_networks_page2 = Network.list(
                                             self.userapiclient,
                                             listall=self.services["listall"],
                                             page=2,
                                             pagesize=self.services["pagesize"]
                                             )
        status = validateList(list_networks_page2)
        self.assertEquals(PASS, status[0], "No networks found at page 2")
        self.assertEqual(
                         len(list_networks_page2),
                         1,
                         "List network response is not matching with the page size length for page 2"
                         )
        network_page2 = list_networks_page2[0]
        for i in range(0, len(list_networks_page1)):
            network_page1 = list_networks_page1[i]
            self.assertNotEquals(
                                 network_page2.id,
                                 network_page1.id,
                                 "Network listed in page 2 is also listed in page 1"
                                 )
        return

    @attr(tags=["advanced"])
    def test_02_create_network_without_sourcenat(self):  
        """
        @Desc: Test create network if supported services doesn't have sourcenat
        @Steps 
        Step1    : Create Network Offering without sourcenat
        Step2    : Enable network offering
        Step3    : Create network with sourcenat diasbled network offering
        Step4    : Verifying that it raises an exception
        """
        # Create Network offering specifically sourcenat disabled
        network_offering_without_sourcenat = NetworkOffering.create(
                                                                    self.apiClient,
                                                                    self.services["network_offering_without_sourcenat"],
                                                                    )
        if network_offering_without_sourcenat is None:
            self.fail("Creation of network offering without sourcenat failed")
        self.cleanup.append(network_offering_without_sourcenat)
            
        # Enable network offering
        network_offering_without_sourcenat.update(self.apiClient, state='Enabled')                            
        self.services["network_without_acl"]["networkoffering"] = network_offering_without_sourcenat.id
            
        # Network create call raise an exception 
        with self.assertRaises(Exception):
            network_created = Network.create(
                                             self.userapiclient,
                                             self.services["network_without_acl"],
                                             )
            if network_created is not None:
                self.cleanup.append(network_created)
        self.services["network_without_acl"]["networkoffering"] = self.network_offering.id
        return

    @attr(tags=["advanced"])
    def test_03_list_vpc_pagination(self):  
        """
        @Desc: Test create vpc with network domain as parameter
        @Steps 
        Step1    : List VPC Offering
        Step2    : List VPCs for newly created user
        Step3    : Create VPCs without network domain based on page size
        Step4    : Verify count of VPCs created
        Step5    : Listing all the VPCs in page1
        Step6    : Listing all the VPCs in page2
        Step7    : Verifying that on page 2 only 1 vpc is present and the vpc on page 2 is not present in page1
        Step8    : Deleting a single vpc and verifying that vpc does not exists on page 2
        """
        # List VPC Offering
        vpc_offs_list = VpcOffering.list(self.userapiclient, isdefault="true")
        if vpc_offs_list is None:
            self.fail("Default VPC offerings not found")
        else:
            vpc_offs = vpc_offs_list[0]
        # List VPCs
        vpc_list = VPC.list(
                            self.userapiclient,
                            listall=self.services["listall"]
                            )
        # verify no vpc is present for newly created user
        status = validateList(vpc_list)
        self.assertEquals(FAIL, status[0], "VPCs found for newly created user")    
        vpc_count_before = 0 
        if vpc_list is None:
            for i in range(0, (self.services["pagesize"] + 1)):
                vpc_1 = VPC.create(
                                   self.userapiclient,
                                   self.services["vpc"],
                                   vpcofferingid=vpc_offs.id,
                                   zoneid=self.zone.id,
                                   )
                # verify vpc is created and not none 
                self.assertIsNotNone(vpc_1, "VPC is not created")
                    
                if(i < (self.services["pagesize"])):
                    self.cleanup.append(vpc_1)
                # Verify VPC name with test data  
                self.assertNotEquals(
                                     -1,
                                     vpc_1.name.find(self.services["vpc"]["name"]),
                                     "VPC name not matched"
                                     )
                # verify zone with test data
                self.assertEquals(
                                  self.zone.id,
                                  vpc_1.zoneid,
                                  "Zone is not matching in the vpc created"
                                  )
        else:
            self.fail("VPCs found for newly created user")
        # Asserting for the length of the VPCs
        vpc_count_after = VPC.list(self.userapiclient, listall=self.services["listall"])
        status = validateList(vpc_count_after)
        self.assertEquals(PASS, status[0], "VPC list count is null")          
        self.assertEqual(
                         len(vpc_count_after),
                         (self.services["pagesize"] + 1),
                         "Number of VPCs created is not matching expected"
                         )
        # Listing all the VPCs in page1
        list_vpcs_page1 = VPC.list(
                                     self.userapiclient,
                                     listall=self.services["listall"],
                                     page=1,
                                     pagesize=self.services["pagesize"]
                                     )
        status = validateList(list_vpcs_page1)
        self.assertEquals(PASS, status[0], "No vpcs found in Page 1")  
        self.assertEqual(
                         len(list_vpcs_page1),
                         self.services["pagesize"],
                         "List vpc response is not matching with the page size length for page 1"
                         )
        # Listing all the vpcs in page2
        list_vpcs_page2 = VPC.list(
                                     self.userapiclient,
                                     listall=self.services["listall"],
                                     page=2,
                                     pagesize=self.services["pagesize"]
                                     )
        status = validateList(list_vpcs_page2)
        self.assertEquals(PASS, status[0], "No vpc found in Page 2")  
        self.assertEqual(
                         1,
                         len(list_vpcs_page2),
                         "List VPCs response is not matching with the page size length for page 2"
                         )
        vpc_page2 = list_vpcs_page2[0]
        # Verifying that on page 2 only 1 vpc is present and the vpc on page 2 is not present in page1
        for i in range(0, len(list_vpcs_page1)):
            vpc_page1 = list_vpcs_page1[i]
            self.assertNotEquals(
                                 vpc_page2.id,
                                 vpc_page1.id,
                                 "VPC listed in page 2 is also listed in page 1"
                                 )
        # Deleting a single vpc and verifying that vpc does not exists on page 2
        VPC.delete(vpc_1, self.userapiclient)
        list_vpc_response = VPC.list(
                                     self.userapiclient,
                                     listall=self.services["listall"],
                                     page=2,
                                     pagesize=self.services["pagesize"]
                                     )
        self.assertEqual(
                        list_vpc_response,
                        None,
                        "vpc was not deleted"
                    )
        return

    @attr(tags=["advanced"])
    def test_04_create_vpc_with_networkdomain(self):  
        """
        @Desc: Test create vpc with network domain as parameter
        @Steps 
        Step1    : List VPC Offering
        Step2    : List VPCs for newly created user
        Step3    : Create VPC
        Step4    : List VPC and verify that count is increased by 1
        """
        # List VPC Offering
        vpc_offs_list = VpcOffering.list(self.userapiclient, isdefault="true")
        if vpc_offs_list is None:
            self.fail("Default VPC offerings not found")
        else:
            vpc_offs = vpc_offs_list[0]
        # List VPCs for newly created user
        vpc_list = VPC.list(
                            self.userapiclient,
                            listall=self.services["listall"]
                            )
        # No VPCs should be present for newly created user
        status = validateList(vpc_list)
        self.assertEquals(FAIL, status[0], "VPCs found for newly created user")   
        vpc_count_before = 0 
        vpc_1 = VPC.create(
                           self.userapiclient,
                           self.services["vpc_network_domain"],
                           vpcofferingid=vpc_offs.id,
                           zoneid=self.zone.id,
                           )
        self.assertIsNotNone(vpc_1, "VPC is not created")
        self.cleanup.append(vpc_1)
        # List VPCs
        vpc_list = VPC.list(
                            self.userapiclient,
                            listall=self.services["listall"]
                            )
        status = validateList(vpc_list)
        self.assertEquals(PASS, status[0], "VPC is not created") 
        self.assertEquals(
                          vpc_count_before + 1,
                          len(vpc_list),
                          "VPC is not created"
                          )
        return

    @attr(tags=["advanced"])
    def test_05_list_network_offering(self):  
        """
        @Desc: Test list network offerings for vpc true and false parameters
        @Steps 
        Step1    : List network offering
        Step2    : Create network offering with default setting of vpc = false
        Step3    : List network offering 
        Step4    : Verify that count is incremented by 1
        Step5    : List network offering with additional parameter of vpc = true
        Step6    : Verify that its count is same as step 1
        Step7    : List network offering with additional parameter of vpc = false
        Step8    : Verify that its count is same as step 3
        """
        # List all network offering
        network_offering_before_count = NetworkOffering.list(self.userapiclient)
        status = validateList(network_offering_before_count)
        self.assertEquals(PASS, status[0], "Default network offering not present")
        # List network offering for vpc = true
        network_offering_vpc_true_before_count = NetworkOffering.list(
                                                                      self.userapiclient,
                                                                      forvpc="true",
                                                                      zoneid=self.zone.id,
                                                                      guestiptype=self.services["network_offering_vlan"]["guestiptype"],
                                                                      supportedServices="SourceNat",
                                                                      specifyvlan=self.services["network_offering_vlan"]["specifyvlan"],
                                                                      state="Enabled"
                                                                      )
        status = validateList(network_offering_vpc_true_before_count)
        self.assertEquals(PASS, status[0], "Default network offering not present for vpc = true") 
        # List network offering 
        network_offering_vpc_false_before_count = NetworkOffering.list(
                                                                      self.userapiclient,
                                                                      forvpc="false",
                                                                      zoneid=self.zone.id,
                                                                      guestiptype=self.services["network_offering_vlan"]["guestiptype"],
                                                                      supportedServices="SourceNat",
                                                                      specifyvlan=self.services["network_offering_vlan"]["specifyvlan"],
                                                                      state="Enabled"
                                                                       )
        status = validateList(network_offering_vpc_false_before_count)
        self.assertEquals(PASS, status[0], "Default network offering not present for vpc = false")  
        # Create network Offering
        network_offering = NetworkOffering.create(
                                    self.apiClient,
                                    self.services["network_offering_vlan"],
                                    )
        # Enable Network offering
        network_offering.update(self.apiClient, state='Enabled')
        self.assertIsNotNone(network_offering, "Network offering is not created")
        self.cleanup.append(network_offering)
        # List network offering
        network_offering_after_count = NetworkOffering.list(self.userapiclient)
        status = validateList(network_offering_after_count)
        self.assertEquals(PASS, status[0], "Network Offering list results in null")          
        # Verify that count is incremented by 1
        self.assertEquals(
                          len(network_offering_before_count) + 1,
                          len(network_offering_after_count),
                          "Network offering is not created"
                          )
        # List network offering with additional parameter of vpc = true
        network_offering_vpc_true_after_count = NetworkOffering.list(
                                                                      self.userapiclient,
                                                                      forvpc="true",
                                                                      zoneid=self.zone.id,
                                                                      guestiptype=self.services["network_offering_vlan"]["guestiptype"],
                                                                      supportedServices="SourceNat",
                                                                      specifyvlan=self.services["network_offering_vlan"]["specifyvlan"],
                                                                      state="Enabled"
                                                                     )
        status = validateList(network_offering_vpc_true_after_count)
        self.assertEquals(PASS, status[0], "Network Offering list results in null") 
        # Verify that its count is same as step 1
        self.assertEquals(
                          len(network_offering_vpc_true_before_count),
                          len(network_offering_vpc_true_after_count),
                          "Default Network offering is created with vpc as true"
                          )
        # List network offering with additional parameter of vpc = false
        network_offering_vpc_false_after_count = NetworkOffering.list(
                                                                      self.userapiclient,
                                                                      forvpc="false",
                                                                      zoneid=self.zone.id,
                                                                      guestiptype=self.services["network_offering_vlan"]["guestiptype"],
                                                                      supportedServices="SourceNat",
                                                                      specifyvlan=self.services["network_offering_vlan"]["specifyvlan"],
                                                                      state="Enabled"
                                                                      )
        status = validateList(network_offering_vpc_false_after_count)
        self.assertEquals(PASS, status[0], "Network Offering list results in null")         
        # Verify that its count is same as step 3
        self.assertEquals(
                          len(network_offering_vpc_false_before_count) + 1,
                          len(network_offering_vpc_false_after_count),
                          "Default Network offering is not created with vpc as false"
                          )
        return

    @attr(tags=["advanced"])
    def test_06_create_network_in_vpc(self):  
        """
        @Desc: Test create network in vpc and verify VPC name
        @Steps 
        Step1    : List VPC Offering
        Step2    : List VPCs for newly created user
        Step3    : Create VPC
        Step4    : List VPC and verify that count is increased by 1
        Step5    : Create network 
        Step6    : List VPCs for specific network created in vpc
        Step7    : Verify vpc name matches for newly created vpc name and name from vpc list
        """
        # List VPC Offering
        vpc_offs_list = VpcOffering.list(self.userapiclient,
                                         isdefault="true",
                                         )
        if vpc_offs_list is None:
            self.fail("Default VPC offerings not found")
        else:
            vpc_offs = vpc_offs_list[0]
        # List VPCs for newly created user
        vpc_list = VPC.list(
                            self.userapiclient,
                            listall=self.services["listall"]
                            )
        # No VPCs should be present for newly created user
        status = validateList(vpc_list)
        self.assertEquals(FAIL, status[0], "VPCs found for newly created user")   
        vpc_count_before = 0 
        vpc_1 = VPC.create(
                           self.userapiclient,
                           self.services["vpc"],
                           vpcofferingid=vpc_offs.id,
                           zoneid=self.zone.id,
                           )
        self.assertIsNotNone(vpc_1, "VPC is not created")
        # List VPCs
        vpc_list = VPC.list(
                            self.userapiclient,
                            listall=self.services["listall"]
                            )
        status = validateList(vpc_list)
        self.assertEquals(PASS, status[0], "VPC is not created") 
        self.assertEquals(
                          vpc_count_before + 1,
                          len(vpc_list),
                          "VPC is not created"
                          )
        # Listing the networks for a user
        list_networks_before = Network.list(self.userapiclient, listall=self.services["listall"])
        # Verifying listed networks for account created at class level
        self.assertIsNotNone(list_networks_before, "Network create failed at class level")
        # List network offering for vpc = true
        network_offering_vpc_true_list = NetworkOffering.list(
                                                               self.userapiclient,
                                                               forvpc="true",
                                                               zoneid=self.zone.id,
                                                               guestiptype=self.services["network_offering_vlan"]["guestiptype"],
                                                               supportedServices="SourceNat",
                                                               specifyvlan=self.services["network_offering_vlan"]["specifyvlan"],
                                                               state="Enabled"
                                                               )
        status = validateList(network_offering_vpc_true_list)
        self.assertEquals(PASS, status[0], "Default network offering not present for vpc = true") 
        # Listing networks in VPC
        list_networks_in_vpc = Network.list(self.userapiclient,
                                            vpcid=vpc_1.id
                                            )
        self.assertIsNone(list_networks_in_vpc, "Networks found for newly created VPC")
        # If number of networks is 1, then creating network    
        if len(list_networks_before) == 1:
                network_created = Network.create(
                                                 self.userapiclient,
                                                 self.services["network_without_acl"],
                                                 networkofferingid=network_offering_vpc_true_list[0].id,
                                                 vpcid=vpc_1.id,
                                                 gateway=self.services["ntwk"]["gateway"],
                                                 netmask=self.services["ntwk"]["netmask"],
                                                 domainid=self.domain.id,
                                                 accountid=self.account.name,
                                             )
                self.cleanup.append(network_created)
                self.cleanup.append(vpc_1)
                self.assertIsNotNone(
                                     network_created,
                                     "Network is not created"
                                     )
                # Creating expected and actual values dictionaries
                expected_dict = {
                                   "id":self.services["network_without_acl"]["zoneid"],
                                   "name":self.services["network_without_acl"]["name"],
                                   }
                actual_dict = {
                                   "id":network_created.zoneid,
                                   "name":network_created.name,
                                   }
                network_status = self.__verify_values(
                                                          expected_dict,
                                                          actual_dict
                                                          )
                self.assertEqual(
                                 True,
                                 network_status,
                                 "Listed network details are not as expected"
                                 )
        else:
            self.assertEqual(
                             len(list_networks_before),
                             1,
                             "more than 1 network created at class level"
                             )
        # Listing the networks  
        list_networks_after = Network.list(self.userapiclient, listall=self.services["listall"])   
        status = validateList(list_networks_after)
        self.assertEquals(PASS, status[0], "No networks found using list call")
        # Asserting for the length of the networks
        self.assertEqual(
                         2,
                         len(list_networks_after),
                         "Number of networks created is not matching expected"
                         )
        # Listing networks in VPC after creation of network
        list_networks_in_vpc = Network.list(self.userapiclient,
                                            vpcid=vpc_1.id
                                            )
        status = validateList(list_networks_in_vpc)
        self.assertEquals(PASS, status[0], "No networks found in vpc")
        # Asserting for the length of the networks
        self.assertEqual(
                         1,
                         len(list_networks_in_vpc),
                         "Number of networks created in vpc is not matching expected"
                         )
        # List VPCs for specific network created in vpc
        vpc_list = VPC.list(
                             self.userapiclient,
                             id=network_created.vpcid
                             )
        # verify no vpc is present for newly created user
        status = validateList(vpc_list)
        self.assertEquals(PASS, status[0], "VPCs not found.")    
        # verify vpc name matches for newly created vpc name and vpc list name
        self.assertEqual(
                         vpc_1.name,
                         vpc_list[0].name,
                         "VPC names not matching"
                         )
        return

    @attr(tags=["advanced"])
    def test_07_delete_network(self):  
        """
        @Desc: Test delete network
        @Steps 
        Step1    : Create Network
        Step2    : Verify Network is created
        Step3    : Delete Network
        Step4    : Verify network is deleted
        """
        # Listing the networks for a user
        list_networks_before = Network.list(self.userapiclient, listall=self.services["listall"])
        # Verifying listed networks for account created at class level
        self.assertIsNotNone(list_networks_before, "Network create failed at class level")
        # List network offering for vpc = false
        network_offering_vpc_false_list = NetworkOffering.list(
                                                               self.userapiclient,
                                                               forvpc="false",
                                                               zoneid=self.zone.id,
                                                               guestiptype=self.services["network_offering_vlan"]["guestiptype"],
                                                               supportedServices="SourceNat",
                                                               specifyvlan=self.services["network_offering_vlan"]["specifyvlan"],
                                                               state="Enabled"
                                                               )
        status = validateList(network_offering_vpc_false_list)
        self.assertEquals(PASS, status[0], "Default network offering not present for vpc = false") 
        # If number of networks is 1, then creating network    
        if len(list_networks_before) == 1:
                network_created = Network.create(
                                                 self.userapiclient,
                                                 self.services["network_without_acl"],
                                                 networkofferingid=network_offering_vpc_false_list[0].id,
                                             )
                self.cleanup.append(network_created)
                self.assertIsNotNone(
                                     network_created,
                                     "Network is not created"
                                     )
                # Creating expected and actual values dictionaries
                expected_dict = {
                                   "id":self.services["network_without_acl"]["zoneid"],
                                   "name":self.services["network_without_acl"]["name"],
                                   }
                actual_dict = {
                                   "id":network_created.zoneid,
                                   "name":network_created.name,
                                   }
                network_status = self.__verify_values(
                                                          expected_dict,
                                                          actual_dict
                                                          )
                self.assertEqual(
                                 True,
                                 network_status,
                                 "Listed network details are not as expected"
                                 )
        else:
            self.assertEqual(
                             len(list_networks_before),
                             1,
                             "more than 1 network created at class level"
                             )
        # Listing the networks  
        list_networks_after = Network.list(
                                           self.userapiclient, 
                                           listall=self.services["listall"]
                                           )   
        status = validateList(list_networks_after)
        self.assertEquals(PASS, status[0], "No networks found using list call")
        # Asserting for the length of the networks
        self.assertEqual(
                         2,
                         len(list_networks_after),
                         "Number of networks created is not matching expected"
                         )
        # Delete Network
        Network.delete(network_created, self.userapiclient)
        # List Networks
        list_networks_after_delete = Network.list(
                                                  self.userapiclient, 
                                                  listall=self.services["listall"]
                                                  )   
        status = validateList(list_networks_after_delete)
        self.assertEquals(PASS, status[0], "No networks found using list call") 
        self.assertEqual(
                         1,
                         len(list_networks_after_delete),
                         "Number of networks created is not matching expected"
                         )      
        # Verify deleted network is not present  
        self.assertNotEquals(
                             network_created.id,
                             list_networks_after_delete[0].id,
                             "Deleted network present"
                             ) 
        return

    # @attr(tags=["advanced"])
    def test_08_update_network(self):  
        """
        @Desc: Test update network
        @Steps 
        Step1    : Create Network
        Step2    : Verify Network is created
        Step3    : Update Network name, display text and network domain
        Step4    : Verify network is updated
        """
        # Listing the networks for a user
        list_networks_before = Network.list(self.userapiclient, listall=self.services["listall"])
        # Verifying listed networks for account created at class level
        self.assertIsNotNone(list_networks_before, "Network create failed at class level")
        # List network offering for vpc = false
        network_offering_vpc_false_list = NetworkOffering.list(
                                                               self.userapiclient,
                                                               forvpc="false",
                                                               zoneid=self.zone.id,
                                                               guestiptype=self.services["network_offering_vlan"]["guestiptype"],
                                                               supportedServices="SourceNat",
                                                               specifyvlan=self.services["network_offering_vlan"]["specifyvlan"],
                                                               state="Enabled"
                                                               )
        status = validateList(network_offering_vpc_false_list)
        self.assertEquals(PASS, status[0], "Default network offering not present for vpc = false") 
        # If number of networks is 1, then creating network    
        if len(list_networks_before) == 1:
                network_created = Network.create(
                                                 self.userapiclient,
                                                 self.services["network_without_acl"],
                                                 networkofferingid=network_offering_vpc_false_list[0].id,
                                             )
                self.cleanup.append(network_created)
                self.assertIsNotNone(
                                     network_created,
                                     "Network is not created"
                                     )
                # Creating expected and actual values dictionaries
                expected_dict = {
                                   "id":self.services["network_without_acl"]["zoneid"],
                                   "name":self.services["network_without_acl"]["name"],
                                   }
                actual_dict = {
                                   "id":network_created.zoneid,
                                   "name":network_created.name,
                                   }
                network_status = self.__verify_values(
                                                          expected_dict,
                                                          actual_dict
                                                          )
                self.assertEqual(
                                 True,
                                 network_status,
                                 "Listed network details are not as expected"
                                 )
        else:
            self.assertEqual(
                             len(list_networks_before),
                             1,
                             "more than 1 network created at class level"
                             )
        # Listing the networks  
        list_networks_after = Network.list(self.userapiclient, listall=self.services["listall"])   
        status = validateList(list_networks_after)
        self.assertEquals(PASS, status[0], "No networks found using list call")
        # Asserting for the length of the networks
        self.assertEqual(
                         2,
                         len(list_networks_after),
                         "Number of networks created is not matching expected"
                         )
        # Update Network
        network_updated = Network.update(network_created,
                                         self.userapiclient,
                                         name="NewNetworkName",
                                         displaytext="NewNetworkDisplayText",
                                         networkdomain="cs13cloud.internal.new"
                                         )
        # List Networks
        list_networks_after_update = Network.list(self.userapiclient, listall=self.services["listall"])   
        status = validateList(list_networks_after_update)
        self.assertEquals(PASS, status[0], "No networks found using list call") 
        self.assertEqual(
                         2,
                         len(list_networks_after_update),
                         "Number of networks created is not matching expected"
                         )      
        # Creating expected and actual values dictionaries
        expected_dict = {
                         "name":"NewNetworkName",
                         "displaytext":"NewNetworkDisplayText",
                         "networkdomain":"cs13cloud.internal.new"
                         }
        actual_dict = {
                        "name":network_updated.name,
                        "displaytext":network_updated.displaytext,
                        "networkdomain":network_updated.networkdomain
                        }
        network_status = self.__verify_values(
                                              expected_dict,
                                              actual_dict
                                              )
        self.assertEqual(
                         True,
                         network_status,
                         "Listed network details are not as expected"
                         )
        return

    @attr(tags=["advanced"])
    def test_09_list_virtual_machines_single_network(self):  
        """
        @Desc: Test update network
        @Steps 
        Step1    : Create Network
        Step2    : Verify Network is created
        Step3    : Create Virtual Machine as per page size
        Step4    : Verify list Virtual machines and pagination
        """
        # Listing the networks for a user
        list_networks_before = Network.list(self.userapiclient, listall=self.services["listall"])
        # Verifying listed networks for account created at class level
        self.assertIsNotNone(list_networks_before, "Network create failed at class level")
        # Create Virtual Machine
        # Listing all the instances for a user
        list_instances_before = VirtualMachine.list(self.userapiclient, listall=self.services["listall"])
        # Verifying listed instances for account created at class level
        self.assertIsNone(
                          list_instances_before,
                          "Virtual Machine already exists for newly created user"
                          )
        # If number of instances are less than (pagesize + 1), then creating them    
        for i in range(0, (self.services["pagesize"] + 1)):
            vm_created = VirtualMachine.create(
                                               self.userapiclient,
                                               self.services["virtual_machine"],
                                               accountid=self.account.name,
                                               domainid=self.account.domainid,
                                               networkids=list_networks_before[0].id,
                                               serviceofferingid=self.service_offering.id,
                                               )
            self.assertIsNotNone(
                                 vm_created,
                                 "VM creation failed"
                                 )
            if(i < (self.services["pagesize"])):
                self.cleanup.append(vm_created)
   
            self.assertEqual(
                             self.services["virtual_machine"]["displayname"],
                             vm_created.displayname,
                             "Newly created VM name and the test data VM name are not matching"
                             )
        # Listing all the instances again after creating VM's        
        list_instances_after = VirtualMachine.list(
                                                   self.userapiclient,
                                                   listall=self.services["listall"],
                                                   networkid=list_networks_before[0].id
                                                   )
        status = validateList(list_instances_after)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of instances after creation failed"
                          )
        # Verifying the length of the instances is (page size + 1)
        self.assertEqual(
                         len(list_instances_after),
                         (self.services["pagesize"] + 1),
                         "Number of instances created is not matching as expected"
                         )
        # Listing all the volumes in page1
        list_instances_page1 = VirtualMachine.list(
                                                   self.userapiclient,
                                                   listall=self.services["listall"],
                                                   page=1,
                                                   pagesize=self.services["pagesize"],
                                                   networkid=list_networks_before[0].id
                                                   )
        status = validateList(list_instances_page1)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of instances in page1 failed"
                          )
        # Verifying that the length of the instances in page 1 is (page size)
        self.assertEqual(
                         self.services["pagesize"],
                         len(list_instances_page1),
                         "List VM response is not matching with the page size length for page 1"
                         )
       # Listing all the VM's in page2
        list_instances_page2 = VirtualMachine.list(
                                                   self.userapiclient,
                                                   listall=self.services["listall"],
                                                   page=2,
                                                   pagesize=self.services["pagesize"],
                                                   networkid=list_networks_before[0].id
                                                   )
        status = validateList(list_instances_page2)
        self.assertEquals(
                          PASS,
                          status[0],
                          "Listing of instances in page2 failed"
                          )
        # Verifying that the length of the VM's in page 2 is 1
        self.assertEqual(
                         1,
                         len(list_instances_page2),
                         "List VM response is not matching with the page size length for page 2"
                         )
        instance_page2 = list_instances_page2[0]
        # Deleting a single VM
        VirtualMachine.delete(vm_created, self.userapiclient)
        # Listing the VM's in page 2
        list_instance_response = VirtualMachine.list(
                                                     self.userapiclient,
                                                     listall=self.services["listall"],
                                                     page=2,
                                                     pagesize=self.services["pagesize"],
                                                     networkid=list_networks_before[0].id
                                                     )
        # verifying that VM does not exists on page 2
        self.assertEqual(
                        list_instance_response,
                        None,
                        "VM was not deleted"
                        )
        return

    @attr(tags=["advanced"])
    def test_10_list_networks_in_vpc(self):  
        """
        @Desc: Test list networks in vpc and verify VPC name
        @Steps 
        Step1    : List VPC Offering
        Step2    : List VPCs for newly created user
        Step3    : Create VPC
        Step4    : List VPC and verify that count is increased by 1
        Step5    : Create network 
        Step6    : List Networks in created vpc
        Step7    : Verify network name matches for newly created network name and name from network list
        """
        # List VPC Offering
        vpc_offs_list = VpcOffering.list(self.userapiclient, isdefault="true")
        if vpc_offs_list is None:
            self.fail("Default VPC offerings not found")
        else:
            vpc_offs = vpc_offs_list[0]
        # List VPCs for newly created user
        vpc_list = VPC.list(
                            self.userapiclient,
                            listall=self.services["listall"]
                            )
        # No VPCs should be present for newly created user
        status = validateList(vpc_list)
        self.assertEquals(FAIL, status[0], "VPCs found for newly created user")   
        vpc_count_before = 0 
        vpc_1 = VPC.create(
                           self.userapiclient,
                           self.services["vpc"],
                           vpcofferingid=vpc_offs.id,
                           zoneid=self.zone.id,
                           )
        self.assertIsNotNone(vpc_1, "VPC is not created")
        # List VPCs
        vpc_list = VPC.list(
                            self.userapiclient,
                            listall=self.services["listall"]
                            )
        status = validateList(vpc_list)
        self.assertEquals(PASS, status[0], "VPC is not created") 
        self.assertEquals(
                          vpc_count_before + 1,
                          len(vpc_list),
                          "VPC is not created"
                          )
        # Listing the networks for a user
        list_networks_before = Network.list(self.userapiclient, listall=self.services["listall"])
        # Verifying listed networks for account created at class level
        self.assertIsNotNone(list_networks_before, "Network create failed at class level")
        # List network offering for vpc = true
        network_offering_vpc_true_list = NetworkOffering.list(
                                                               self.userapiclient,
                                                               forvpc="true",
                                                               zoneid=self.zone.id,
                                                               guestiptype=self.services["network_offering_vlan"]["guestiptype"],
                                                               supportedServices="SourceNat",
                                                               specifyvlan=self.services["network_offering_vlan"]["specifyvlan"],
                                                               state="Enabled"
                                                               )
        status = validateList(network_offering_vpc_true_list)
        self.assertEquals(PASS, status[0], "Default network offering not present for vpc = true") 
        # If number of networks is 1, then creating network    
        if len(list_networks_before) == 1:
                network_created = Network.create(
                                                 self.userapiclient,
                                                 self.services["network_without_acl"],
                                                 networkofferingid=network_offering_vpc_true_list[0].id,
                                                 vpcid=vpc_1.id,
                                                 gateway=self.services["ntwk"]["gateway"],
                                                 netmask=self.services["ntwk"]["netmask"]
                                             )
                self.cleanup.append(network_created)
                self.cleanup.append(vpc_1)
                self.assertIsNotNone(
                                     network_created,
                                     "Network is not created"
                                     )
                # Creating expected and actual values dictionaries
                expected_dict = {
                                   "id":self.services["network_without_acl"]["zoneid"],
                                   "name":self.services["network_without_acl"]["name"],
                                   }
                actual_dict = {
                                   "id":network_created.zoneid,
                                   "name":network_created.name,
                                   }
                network_status = self.__verify_values(
                                                          expected_dict,
                                                          actual_dict
                                                          )
                self.assertEqual(
                                 True,
                                 network_status,
                                 "Listed network details are not as expected"
                                 )
        else:
            self.assertEqual(
                             len(list_networks_before),
                             1,
                             "more than 1 network created at class level"
                             )
        # Listing the networks  
        list_networks_after = Network.list(self.userapiclient, listall=self.services["listall"])   
        status = validateList(list_networks_after)
        self.assertEquals(PASS, status[0], "No networks found using list call")
        # Asserting for the length of the networks
        self.assertEqual(
                         2,
                         len(list_networks_after),
                         "Number of networks created is not matching expected"
                         )
        # Listing the networks  
        list_networks_in_vpc = Network.list(
                                            self.userapiclient,
                                            listall=self.services["listall"],
                                            vpcid=vpc_1.id
                                            )   
        status = validateList(list_networks_in_vpc)
        self.assertEquals(PASS, status[0], "No networks found using list call")  
        # Verify network name matches for newly created network name and name from network list
        self.assertEqual(
                         network_created.name,
                         list_networks_in_vpc[0].name,
                         "Network names not matching"
                         )
        return

    @attr(tags=["advanced"])
    def test_11_update_vpc(self):  
        """
        @Desc: Test create vpc with network domain as parameter
        @Steps 
        Step1    : List VPC Offering
        Step2    : List VPCs for newly created user
        Step3    : Create VPCs
        Step4    : Verify count of VPCs created
        Step5    : Update VPC name and display text
        Step6    : Verify name and display text is updated
        """
        # List VPC Offering
        vpc_offs_list = VpcOffering.list(self.userapiclient, isdefault="true")
        if vpc_offs_list is None:
            self.fail("Default VPC offerings not found")
        else:
            vpc_offs = vpc_offs_list[0]
        # List VPCs
        vpc_list = VPC.list(
                            self.userapiclient,
                            listall=self.services["listall"]
                            )
        # verify no vpc is present for newly created user
        status = validateList(vpc_list)
        self.assertEquals(FAIL, status[0], "VPCs found for newly created user")    
        vpc_count_before = 0 
             
        if vpc_list is None:
                vpc_1 = VPC.create(
                                   self.userapiclient,
                                   self.services["vpc"],
                                   vpcofferingid=vpc_offs.id,
                                   zoneid=self.zone.id,
                                   )
                self.cleanup.append(vpc_1)
                # verify vpc is created and not none 
                self.assertIsNotNone(vpc_1, "VPC is not created")
                # Verify VPC name with test data  
                self.assertNotEquals(
                                     -1,
                                     vpc_1.name.find(self.services["vpc"]["name"]),
                                     "VPC name not matched"
                                     )
                # verify zone with test data
                self.assertEquals(
                          self.zone.id,
                          vpc_1.zoneid,
                          "Zone is not matching in the vpc created"
                          )
        else:
            self.fail("VPCs found for newly created user")
        # Asserting for the length of the VPCs
        vpc_count_after = VPC.list(self.userapiclient, listall=self.services["listall"])
        status = validateList(vpc_count_after)
        self.assertEquals(PASS, status[0], "VPC list count is null")          
        self.assertEqual(
                         1,
                         len(vpc_count_after),
                         "Number of VPCs created is not matching expected"
                         )
        # Update VPC
        vpc_updated = VPC.update(
                                 vpc_1,
                                 self.userapiclient,
                                 name="NewVPCName",
                                 displaytext="NewVPCDisplayText",
                                 )
        # List Networks
        list_vpcs_after_update = VPC.list(
                                          self.userapiclient, 
                                          listall=self.services["listall"]
                                          )   
        status = validateList(list_vpcs_after_update)
        self.assertEquals(PASS, status[0], "No vpcs found using list call") 
        self.assertEqual(
                         1,
                         len(list_vpcs_after_update),
                         "Number of vpcs created is not matching expected"
                         )      
        # Creating expected and actual values dictionaries
        expected_dict = {
                           "name":"NewVPCName",
                           "displaytext":"NewVPCDisplayText",
                           }
        actual_dict = {
                           "name":vpc_updated.name,
                           "displaytext":vpc_updated.displaytext,
                           }
        vpc_status = self.__verify_values(
                                          expected_dict,
                                          actual_dict
                                          )
        self.assertEqual(
                         True,
                         vpc_status,
                         "Listed vpc details are not as expected"
                         )
        return

    @attr(tags=["advanced"])
    def test_12_list_create_delete_networkACL(self):  
        """
        @Desc: Test create network in vpc and verify VPC name
        @Steps 
        Step1    : List VPC Offering
        Step2    : List VPCs for newly created user
        Step3    : Create VPC
        Step4    : List VPC and verify that count is increased by 1
        Step5    : Create network 
        Step6    : Verify network is created
        Step7    : List Network ACLs
        Step8    : Create Network ACL
        Step9    : Verify NetworkACL is created
        Step10   : Delete NetworkACL
        Step11   : Verify NetworkACL is deleted
        """
        # List VPC Offering
        vpc_offs_list = VpcOffering.list(self.userapiclient,
                                         isdefault="true",
                                         )
        if vpc_offs_list is None:
            self.fail("Default VPC offerings not found")
        else:
            vpc_offs = vpc_offs_list[0]
        # List VPCs for newly created user
        vpc_list = VPC.list(
                            self.userapiclient,
                            listall=self.services["listall"]
                            )
        # No VPCs should be present for newly created user
        status = validateList(vpc_list)
        self.assertEquals(FAIL, status[0], "VPCs found for newly created user")   
        vpc_count_before = 0 
        vpc_1 = VPC.create(
                           self.userapiclient,
                           self.services["vpc"],
                           vpcofferingid=vpc_offs.id,
                           zoneid=self.zone.id,
                           )
        self.assertIsNotNone(vpc_1, "VPC is not created")
        # List VPCs
        vpc_list = VPC.list(
                            self.userapiclient,
                            listall=self.services["listall"]
                            )
        status = validateList(vpc_list)
        self.assertEquals(PASS, status[0], "VPC is not created") 
        self.assertEquals(
                          vpc_count_before + 1,
                          len(vpc_list),
                          "VPC is not created"
                          )
        # Listing the networks for a user
        list_networks_before = Network.list(
                                            self.userapiclient, 
                                            listall=self.services["listall"]
                                            )
        # Verifying listed networks for account created at class level
        self.assertIsNotNone(list_networks_before, "Network create failed at class level")
        # List network offering for vpc = true
        network_offering_vpc_true_list = NetworkOffering.list(
                                                               self.userapiclient,
                                                               forvpc="true",
                                                               zoneid=self.zone.id,
                                                               guestiptype=self.services["network_offering_vlan"]["guestiptype"],
                                                               supportedServices="SourceNat",
                                                               specifyvlan=self.services["network_offering_vlan"]["specifyvlan"],
                                                               state="Enabled"
                                                               )
        status = validateList(network_offering_vpc_true_list)
        self.assertEquals(PASS, status[0], "Default network offering not present for vpc = true") 
        # Listing networks in VPC
        list_networks_in_vpc = Network.list(self.userapiclient,
                                            vpcid=vpc_1.id
                                            )
        self.assertIsNone(list_networks_in_vpc, "Networks found for newly created VPC")
        # If number of networks is 1, then creating network    
        if len(list_networks_before) == 1:
                network_created = Network.create(
                                                 self.userapiclient,
                                                 self.services["network_without_acl"],
                                                 networkofferingid=network_offering_vpc_true_list[0].id,
                                                 vpcid=vpc_1.id,
                                                 gateway=self.services["ntwk"]["gateway"],
                                                 netmask=self.services["ntwk"]["netmask"],
                                                 domainid=self.domain.id,
                                                 accountid=self.account.name,
                                             )
                self.cleanup.append(network_created)
                self.cleanup.append(vpc_1)
                self.assertIsNotNone(
                                     network_created,
                                     "Network is not created"
                                     )
                # Creating expected and actual values dictionaries
                expected_dict = {
                                   "id":self.services["network_without_acl"]["zoneid"],
                                   "name":self.services["network_without_acl"]["name"],
                                   }
                actual_dict = {
                                   "id":network_created.zoneid,
                                   "name":network_created.name,
                                   }
                network_status = self.__verify_values(
                                                          expected_dict,
                                                          actual_dict
                                                          )
                self.assertEqual(
                                 True,
                                 network_status,
                                 "Listed network details are not as expected"
                                 )
        else:
            self.assertEqual(
                             len(list_networks_before),
                             1,
                             "more than 1 network created at class level"
                             )
        # Listing the networks  
        list_networks_after = Network.list(self.userapiclient, listall=self.services["listall"])   
        status = validateList(list_networks_after)
        self.assertEquals(PASS, status[0], "No networks found using list call")
        # Asserting for the length of the networks
        self.assertEqual(
                         2,
                         len(list_networks_after),
                         "Number of networks created is not matching expected"
                         )
        # Listing networks in VPC after creation of network
        list_networks_in_vpc = Network.list(self.userapiclient,
                                            vpcid=vpc_1.id
                                            )
        status = validateList(list_networks_in_vpc)
        self.assertEquals(PASS, status[0], "No networks found in vpc")
        # Asserting for the length of the networks
        self.assertEqual(
                         1,
                         len(list_networks_in_vpc),
                         "Number of networks created in vpc is not matching expected"
                         )
        # List VPCs for specific network created in vpc
        vpc_list = VPC.list(
                             self.userapiclient,
                             id=network_created.vpcid
                             )
        # List Network ACLs
        list_network_acl = NetworkACL.list(
                                           self.userapiclient,
                                           networkid=network_created.id
                                           )
        self.assertIsNone(list_network_acl, "ACL list is not empty for newly created network")
        # Create NetworkACL
        network_acl_created = NetworkACL.create(
                                                self.userapiclient,
                                                self.services["network_acl_rule"],
                                                networkid=network_created.id
                                                )
        self.cleanup.append(network_acl_created)
        self.assertIsNotNone(
                             network_acl_created,
                             "NetworkACL is not created"
                             )
        # List Network ACL
        list_network_acl = NetworkACL.list(
                                           self.userapiclient,
                                           networkid=network_created.id
                                           )
        status = validateList(list_network_acl)
        self.assertEquals(PASS, status[0], "No networks acls found after creating")
        # Asserting for the length of the networks
        self.assertEqual(
                         1,
                         len(list_network_acl),
                         "Number of networks acls reated is not matching expected"
                         )
        # Delete Network ACL
        NetworkACL.delete(network_acl_created, self.userapiclient)
        # List Network ACL
        list_network_acl = NetworkACL.list(
                                           self.userapiclient,
                                           networkid=network_created.id
                                           )
        self.assertIsNone(list_network_acl, "ACL list is not empty for newly created network")
        return
