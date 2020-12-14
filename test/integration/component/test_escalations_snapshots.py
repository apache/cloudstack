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
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.base import (VmSnapshot,
                             Snapshot,
                             DiskOffering,
                             ServiceOffering,
                             VirtualMachine,
                             Account,
                             Volume)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
from marvin.lib.utils import validateList, cleanup_resources
from marvin.codes import PASS
from nose.plugins.attrib import attr

class TestSnapshots(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(TestSnapshots, cls).getClsTestClient()
            cls.api_client = cls.testClient.getApiClient()
            cls.services = cls.testClient.getParsedTestDataConfig()
            cls.unsupportedHypervisor = False
            cls.hypervisor = cls.testClient.getHypervisorInfo()
            if cls.hypervisor.lower() in ("lxc", "hyperv"):
                cls.unsupportedHypervisor = True
                return
            # Get Domain, Zone, Template
            cls.domain = get_domain(cls.api_client)
            cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
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
            cls.services["virtual_machine"]["hypervisor"] = cls.hypervisor
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
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setup : %s" % e)
        return

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()
        self.cleanup = []
        if self.unsupportedHypervisor:
            self.skipTest("Snapshots are not supported on %s" %self.hypervisor)

    def tearDown(self):
        # Clean up, terminate the created resources
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

        keys = list(expected_vals.keys())
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

    @attr(tags=["advanced", "basic"], required_hardware="true")
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
        if self.hypervisor.lower() in ['hyperv']:
            raise unittest.SkipTest("This feature is not supported on existing hypervisor. Hence, skipping the test")
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
        self.assertEqual(
                          PASS,
                          status[0],
                          "Root volume did not get created while deploying a VM"
                          )
        # Verifying list size to be 1
        self.assertEqual(
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
        self.assertEqual(
                          PASS,
                          status[0],
                          "Volume snapshot creation failed"
                          )
        # Verifying that list size is pagesize + 1
        self.assertEqual(
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
        self.assertEqual(
                          PASS,
                          status[0],
                          "Failed to list volume snapshots in page 1"
                          )
        # Verifying the list size to be equal to pagesize
        self.assertEqual(
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
        self.assertEqual(
                          PASS,
                          status[0],
                          "Failed to list volume snapshots in page 2"
                          )
        # Verifying the list size to be equal to pagesize
        self.assertEqual(
                          1,
                          len(list_vol_snaps_page2),
                          "Size of volume snapshots in page 2 is not matching"
                          )
        # Deleting the volume snapshot present in page 2
        Snapshot.delete(
                        snapshot_created,
                        self.userapiclient
                        )
        self.cleanup.remove(snapshot_created)
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

    @attr(tags=["advanced", "basic"], required_hardware="true")
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
        if self.hypervisor.lower() in ['hyperv']:
            raise unittest.SkipTest("This feature is not supported on existing hypervisor. Hence, skipping the test")
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
        self.assertEqual(
                          PASS,
                          status[0],
                          "Root volume did not get created while deploying a VM"
                          )
        # Verifying list size to be 1
        self.assertEqual(
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
        self.assertEqual(
                          PASS,
                          status[0],
                          "Volume snapshot creation failed"
                          )
        # Verifying that list size is 1
        self.assertEqual(
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
        self.assertEqual(
                          PASS,
                          status[0],
                          "Failed to list Volume snapshot by Id"
                          )
        # Verifying that list size is 1
        self.assertEqual(
                          1,
                          len(list_vol_snapshot),
                          "Size of the list volume snapshot by Id is not matching"
                          )
        # Verifying details of the listed snapshot to be same as snapshot created above
        # Creating expected and actual values dictionaries
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

    @attr(tags=["advanced", "basic"], required_hardware="true")
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
        if self.hypervisor.lower() in ['kvm', 'hyperv']:
            raise unittest.SkipTest("This feature is not supported on existing hypervisor. Hence, skipping the test")
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
        self.assertEqual(
                          PASS,
                          status[0],
                          "VM snapshot creation failed"
                          )
        # Verifying that list size is pagesize + 1
        self.assertEqual(
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
        self.assertEqual(
                          PASS,
                          status[0],
                          "Failed to list vm snapshots in page 1"
                          )
        # Verifying the list size to be equal to pagesize
        self.assertEqual(
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
        self.assertEqual(
                          PASS,
                          status[0],
                          "Failed to list vm snapshots in page 2"
                          )
        # Verifying the list size to be equal to pagesize
        self.assertEqual(
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
        list_vm_snaps = VmSnapshot.list(
                                         self.userapiclient,
                                         listall=self.services["listall"],
                                         )
        status = validateList(list_vm_snaps)
        self.assertEqual(
                          PASS,
                          status[0],
                          "All VM snapshots deleted"
                          )
        # Verifying that list size is equal to page size
        self.assertEqual(
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

    @attr(tags=["advanced", "basic"], required_hardware="true")
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
        if self.hypervisor.lower() in ['kvm', 'hyperv']:
            raise unittest.SkipTest("This feature is not supported on existing hypervisor. Hence, skipping the test")
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
        self.assertEqual(
                          PASS,
                          status[0],
                          "VM snapshot creation failed"
                          )
        # Verifying that list size is 1
        self.assertEqual(
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
        self.assertEqual(
                          PASS,
                          status[0],
                          "Failed to list VM snapshot by Id"
                          )
        # Verifying that list size is 1
        self.assertEqual(
                          1,
                          len(list_vm_snapshot),
                          "Size of the list vm snapshot by Id is not matching"
                          )
        # Verifying details of the listed snapshot to be same as snapshot created above
        # Creating expected and actual values dictionaries
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
