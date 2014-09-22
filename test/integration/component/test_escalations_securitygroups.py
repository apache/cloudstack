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
from marvin.lib.base import (SecurityGroup,
                             Account)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template)
from marvin.lib.utils import (validateList,
                              cleanup_resources)
from marvin.codes import (PASS, EMPTY_LIST)
from nose.plugins.attrib import attr

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

    @attr(tags=["basic"], required_hardware="true")
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
        self.cleanup.remove(securitygroup_created)
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

    @attr(tags=["basic"], required_hardware="true")
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

    @attr(tags=["basic"], required_hardware="true")
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
