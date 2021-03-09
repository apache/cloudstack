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
            cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
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
        self.assertEqual(
                          PASS,
                          status[0],
                          "ISO's creation failed"
                          )
        # Verifying that list size is pagesize + 1
        self.assertEqual(
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
        self.assertEqual(
                          PASS,
                          status[0],
                          "Failed to list ISO's in page 1"
                          )
        # Verifying the list size to be equal to pagesize
        self.assertEqual(
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
        self.assertEqual(
                          PASS,
                          status[0],
                          "Failed to list ISo's in page 2"
                          )
        # Verifying the list size to be equal to 1
        self.assertEqual(
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
            self.assertEqual(
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

    @attr(tags=["advanced", "basic"], required_hardware="true")
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
        self.assertEqual(
                          PASS,
                          status[0],
                          "ISO's creation failed"
                          )
        # Verifying that list size is 1
        self.assertEqual(
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
            self.assertEqual(
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
        self.assertEqual(
                          "DOWNLOAD_URL_CREATED",
                          download_iso.state,
                          "Download URL not created for ISO"
                          )
        self.assertIsNotNone(
                             download_iso.url,
                             "Download URL not created for ISO"
                             )
        self.assertEqual(
                          iso_created.id,
                          download_iso.id,
                          "Download ISO details are not same as ISO created"
                          )
        del self.services["iso"]["zoneid"]
        del self.services["iso"]["isextractable"]
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
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
        self.assertEqual(
                          PASS,
                          status[0],
                          "ISO's creation failed"
                          )
        # Verifying that list size is 1
        self.assertEqual(
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
            self.assertEqual(
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
        self.assertEqual(
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

    @attr(tags=["advanced", "basic"], required_hardware="true")
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
        self.assertEqual(
                          PASS,
                          status[0],
                          "Failed to list Zones"
                          )
        if not len(zones_list) > 1:
            self.skipTest("Not enough zones exist to copy iso")
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
            self.assertEqual(
                              PASS,
                              status[0],
                              "ISO creation failed in Zone1"
                              )
            # Verifying that list size is 1
            self.assertEqual(
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
                self.assertEqual(
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
            self.assertEqual(
                              PASS,
                              status[0],
                              "ISO creation failed in Zone1"
                              )
            # Verifying that list size is 1
            self.assertEqual(
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
            self.assertEqual(
                              PASS,
                              status[0],
                              "ISO failed to copy into Zone2"
                              )
            # Verifying that list size is 1
            self.assertEqual(
                              1,
                              len(list_isos_zone2),
                              "ISO failed to copy into Zone2"
                              )
            self.assertNotEqual(
                                 "Connection refused",
                                 list_isos_zone2[0].status,
                                 "Failed to copy ISO"
                                 )
            self.assertEqual(
                              True,
                              list_isos_zone2[0].isready,
                              "Failed to copy ISO"
                              )
        del self.services["iso"]["zoneid"]
        return
