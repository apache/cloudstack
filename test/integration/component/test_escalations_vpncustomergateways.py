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
            cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
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

    @attr(tags=["advanced", "basic"], required_hardware="true")
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
        self.cleanup.remove(vpncustomergateway_created)
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

    @attr(tags=["advanced", "basic"], required_hardware="true")
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
