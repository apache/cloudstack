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
from marvin.cloudstackTestCase import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from marvin.codes import PASS, FAIL
from nose.plugins.attrib import attr

class TestNetworks_1(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(TestNetworks_1, cls).getClsTestClient()
            cls.api_client = cls.testClient.getApiClient()
            cls.test_data = cls.testClient.getParsedTestDataConfig()
            cls.hypervisor = cls.testClient.getHypervisorInfo()
            # Get Domain, Zone, Template
            cls.domain = get_domain(cls.api_client)
            cls.zone = get_zone(
                cls.api_client,
                cls.testClient.getZoneForTests())
            cls.template = get_template(
                cls.api_client,
                cls.zone.id,
                cls.test_data["ostype"]
            )
            if cls.zone.localstorageenabled:
                cls.storagetype = 'local'
                cls.test_data["service_offerings"][
                    "tiny"]["storagetype"] = 'local'
            else:
                cls.storagetype = 'shared'
                cls.test_data["service_offerings"][
                    "tiny"]["storagetype"] = 'shared'

            cls.test_data['mode'] = cls.zone.networktype
            cls.test_data["virtual_machine"][
                "hypervisor"] = cls.hypervisor
            cls.test_data["virtual_machine"]["zoneid"] = cls.zone.id
            cls.test_data["virtual_machine"]["template"] = cls.template.id
            cls.test_data["network_without_acl"]["zoneid"] = cls.zone.id
            # Create Network offering
            cls.network_offering = NetworkOffering.create(
                cls.api_client,
                cls.test_data["network_offering_vlan"],
            )
            # Enable Network offering
            cls.network_offering.update(cls.api_client, state='Enabled')
            cls.test_data["network_without_acl"][
                "networkoffering"] = cls.network_offering.id
            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.test_data["service_offerings"]["tiny"]
            )
            # Creating Disk offering, Service Offering and Account
            cls.account = Account.create(
                cls.api_client,
                cls.test_data["account"],
                domainid=cls.domain.id
            )
            # Getting authentication for user in newly created Account
            cls.user = cls.account.user[0]
            cls.userapiclient = cls.testClient.getUserApiClient(
                cls.user.username,
                cls.domain.name)
            cls.account_network = Network.create(
                cls.userapiclient,
                cls.test_data["network_without_acl"],
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
        Step2: Verifying length of expected and actual dictionaries is
               matching.
               If not matching returning false
        Step3: Listing all the keys from expected dictionary
        Step4: Looping through each key from step2 and verifying expected
               and actual dictionaries have same value
               If not making return flag to False
        Step5: returning the return flag after all the values are verified
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
                    "expected Value: %s, is not matching with actual value: %s"
                    %
                    (exp_val, act_val))
        return return_flag

    @attr(tags=["advanced"], required_hardware="true")
    def test_01_list_networks_pagination(self):
        """
        @Desc: Test List Networks pagination
        @Steps
        Step1    : Listing the networks for a user
        Step2    : Verifying listed networks for account created at class level
        Step3    : If number of networks is less than (pagesize  1), then
                   creating them
        Step4    : Listing the networks again
        Step5    : Verifying for the length of the networks that it is
                   (pagesize  1)
        Step6    : Listing and verifying all the networks in page1
        Step7    : Listing and verifying all the networks in page2
        Step8    : Verifying that on page 2 only 1 network is present and
                   the network on page 2 is not present in page1
        """
        list_zones = Zone.list(
            self.userapiclient,
            id=self.zone.id
        )
        status = validateList(list_zones)
        self.assertEqual(PASS, status[0], "No Zones found for a given id")
        self.test_data["network_without_acl"]["zoneid"] = list_zones[0].id
        # Listing the networks for a user
        list_networks_before = Network.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            type="Isolated"
        )
        # Verifying listed networks for account created at class level
        if list_networks_before is None:
            self.assertEqual(
                len(list_networks_before),
                0,
                "Network create failed at class level"
            )
        # If number of networks is less than (pagesize  1), then creating
        # network
        elif len(list_networks_before) == 1:
            for i in range(0, (self.test_data["pagesize"])):
                network_created = Network.create(
                    self.userapiclient,
                    self.test_data["network_without_acl"],
                )
                self.cleanup.append(network_created)
                self.assertIsNotNone(
                    network_created,
                    "Network is not created"
                )
                # Creating expected and actual values dictionaries
                expected_dict = {
                    "id": list_zones[0].id,
                    "name": self.test_data["network_without_acl"]["name"],
                }
                actual_dict = {
                    "id": network_created.zoneid,
                    "name": network_created.name,
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
            listall=self.test_data["listall"],
            type="Isolated")
        status = validateList(list_networks_after)
        self.assertEqual(PASS, status[0], "No networks found using list call")
        # Asserting for the length of the networks
        self.assertEqual(
            len(list_networks_after),
            (self.test_data["pagesize"] + 1),
            "Number of networks created is not matching expected"
        )
        # Listing all the networks in page1
        list_networks_page1 = Network.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            page=1,
            pagesize=self.test_data["pagesize"],
            type="Isolated"
        )
        status = validateList(list_networks_page1)
        self.assertEqual(PASS, status[0], "No networks found at page 1")
        self.assertEqual(
            len(list_networks_page1),
            self.test_data["pagesize"],
            "List network response is not matching with the page\
                    size length for page 1")
        # Listing all the networks in page2
        list_networks_page2 = Network.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            page=2,
            pagesize=self.test_data["pagesize"],
            type="Isolated"
        )
        status = validateList(list_networks_page2)
        self.assertEqual(PASS, status[0], "No networks found at page 2")
        self.assertEqual(
            len(list_networks_page2),
            1,
            "List network response is not matching with the page size\
            length for page 2"
        )
        network_page2 = list_networks_page2[0]
        for i in range(0, len(list_networks_page1)):
            network_page1 = list_networks_page1[i]
            self.assertNotEqual(
                network_page2.id,
                network_page1.id,
                "Network listed in page 2 is also listed in page 1"
            )
        return

    @attr(tags=["advanced"], required_hardware="true")
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
            self.test_data["network_offering_without_sourcenat"],
        )
        if network_offering_without_sourcenat is None:
            self.fail("Creation of network offering without sourcenat failed")
        self.cleanup.append(network_offering_without_sourcenat)
        # Enable network offering
        network_offering_without_sourcenat.update(
            self.apiClient,
            state='Enabled')
        self.test_data["network_without_acl"][
            "networkoffering"] = network_offering_without_sourcenat.id
        # Network create call raise an exception
        with self.assertRaises(Exception):
            network_created = Network.create(
                self.userapiclient,
                self.test_data["network_without_acl"],
            )
            if network_created is not None:
                self.cleanup.append(network_created)
        self.test_data["network_without_acl"][
            "networkoffering"] = self.network_offering.id
        return

    @attr(tags=["advanced"], required_hardware="true")
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
        Step7    : Verifying that on page 2 only 1 vpc is present and
                   the vpc on page 2 is not present in page1
        Step8    : Deleting a single vpc and verifying that vpc does
                   not exists on page 2
        """
        if self.hypervisor.lower() in ['hyperv']:
            raise unittest.SkipTest("This feature is not supported on existing hypervisor. Hence, skipping the test")
        # List VPC Offering
        vpc_offs_list = VpcOffering.list(self.userapiclient, isdefault="true", name='Default VPC offering')
        if vpc_offs_list is None:
            self.fail("Default VPC offerings not found")
        else:
            vpc_offs = vpc_offs_list[0]
        # List VPCs
        vpc_list = VPC.list(
            self.userapiclient,
            listall=self.test_data["listall"]
        )
        # verify no vpc is present for newly created user
        status = validateList(vpc_list)
        self.assertEqual(FAIL, status[0], "VPCs found for newly created user")
        for i in range(0, (self.test_data["pagesize"] + 1)):
            vpc_1 = VPC.create(
                self.userapiclient,
                self.test_data["vpc"],
                vpcofferingid=vpc_offs.id,
                zoneid=self.zone.id,
            )
            if(i < (self.test_data["pagesize"])):
                self.cleanup.append(vpc_1)
            # verify vpc is created and not none
            self.assertIsNotNone(vpc_1, "VPC is not created")
            # Verify VPC name with test data
            self.assertNotEqual(
                -1,
                vpc_1.name.find(self.test_data["vpc"]["name"]),
                "VPC name not matched"
            )
            # verify zone with test data
            self.assertEqual(
                self.zone.id,
                vpc_1.zoneid,
                "Zone is not matching in the vpc created"
            )
        # Asserting for the length of the VPCs
        vpc_count_after = VPC.list(
            self.userapiclient,
            listall=self.test_data["listall"])
        status = validateList(vpc_count_after)
        self.assertEqual(PASS, status[0], "VPC list count is null")
        self.assertEqual(
            len(vpc_count_after),
            (self.test_data["pagesize"] + 1),
            "Number of VPCs created is not matching expected"
        )
        # Listing all the VPCs in page1
        list_vpcs_page1 = VPC.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            page=1,
            pagesize=self.test_data["pagesize"]
        )
        status = validateList(list_vpcs_page1)
        self.assertEqual(PASS, status[0], "No vpcs found in Page 1")
        self.assertEqual(
            len(list_vpcs_page1),
            self.test_data["pagesize"],
            "List vpc response is not matching with the page\
            size length for page 1")
        # Listing all the vpcs in page2
        list_vpcs_page2 = VPC.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            page=2,
            pagesize=self.test_data["pagesize"]
        )
        status = validateList(list_vpcs_page2)
        self.assertEqual(PASS, status[0], "No vpc found in Page 2")
        self.assertEqual(
            1,
            len(list_vpcs_page2),
            "List VPCs response is not matching with the\
                    page size length for page 2"
        )
        vpc_page2 = list_vpcs_page2[0]
        # Verifying that on page 2 only 1 vpc is present and the vpc on page 2
        # is not present in page1
        for i in range(0, len(list_vpcs_page1)):
            vpc_page1 = list_vpcs_page1[i]
            self.assertNotEqual(
                vpc_page2.id,
                vpc_page1.id,
                "VPC listed in page 2 is also listed in page 1"
            )
        # Deleting a single vpc and verifying that vpc does not exists on page
        # 2
        VPC.delete(vpc_1, self.userapiclient)
        list_vpc_response = VPC.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            page=2,
            pagesize=self.test_data["pagesize"]
        )
        self.assertEqual(
            list_vpc_response,
            None,
            "vpc was not deleted"
        )
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_04_create_vpc_with_networkdomain(self):
        """
        @Desc: Test create vpc with network domain as parameter
        @Steps
        Step1    : List VPC Offering
        Step2    : List VPCs for newly created user
        Step3    : Create VPC
        Step4    : List VPC and verify that count is increased by 1
        """
        if self.hypervisor.lower() in ['hyperv']:
            raise unittest.SkipTest("This feature is not supported on existing hypervisor. Hence, skipping the test")
        # List VPC Offering
        vpc_offs_list = VpcOffering.list(self.userapiclient, isdefault="true", name='Default VPC offering')
        if vpc_offs_list is None:
            self.fail("Default VPC offerings not found")
        else:
            vpc_offs = vpc_offs_list[0]
        # List VPCs for newly created user
        vpc_list = VPC.list(
            self.userapiclient,
            listall=self.test_data["listall"]
        )
        # No VPCs should be present for newly created user
        status = validateList(vpc_list)
        self.assertEqual(FAIL, status[0], "VPCs found for newly created user")
        vpc_count_before = 0
        vpc_1 = VPC.create(
            self.userapiclient,
            self.test_data["vpc_network_domain"],
            vpcofferingid=vpc_offs.id,
            zoneid=self.zone.id,
        )
        self.assertIsNotNone(vpc_1, "VPC is not created")
        self.cleanup.append(vpc_1)
        # List VPCs
        vpc_list = VPC.list(
            self.userapiclient,
            listall=self.test_data["listall"]
        )
        status = validateList(vpc_list)
        self.assertEqual(PASS, status[0], "VPC is not created")
        self.assertEqual(
            vpc_count_before + 1,
            len(vpc_list),
            "VPC is not created"
        )
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_05_list_network_offerings_with_and_without_vpc(self):
        """
        @Desc: Test list network offerings for vpc true and false parameters
        @Steps
        Step1    : List network offering
        Step2    : Create network offering with default setting of vpc = false
        Step3    : List network offering
        Step4    : Verify that count is incremented by 1
        Step5    : List network offering with additional parameter of
                   vpc = true
        Step6    : Verify that its count is same as step 1
        Step7    : List network offering with additional parameter of
                   vpc = false
        Step8    : Verify that its count is same as step 3
        """
        # List all network offering
        network_offering_before_count = NetworkOffering.list(
            self.userapiclient)
        status = validateList(network_offering_before_count)
        self.assertEqual(
            PASS,
            status[0],
            "Default network offering not present")
        # List network offering for vpc = true
        network_offering_vpc_true_before_count = NetworkOffering.list(
            self.userapiclient,
            forvpc="true",
            zoneid=self.zone.id,
            guestiptype=self.test_data["network_offering_vlan"]["guestiptype"],
            supportedServices="SourceNat",
            specifyvlan=self.test_data["network_offering_vlan"]["specifyvlan"],
            state="Enabled"
        )
        status = validateList(network_offering_vpc_true_before_count)
        self.assertEqual(
            PASS,
            status[0],
            "Default network offering not present for vpc = true")
        # List network offering
        network_offering_vpc_false_before_count = NetworkOffering.list(
            self.userapiclient,
            forvpc="false",
            zoneid=self.zone.id,
            guestiptype=self.test_data["network_offering_vlan"]["guestiptype"],
            supportedServices="SourceNat",
            specifyvlan=self.test_data["network_offering_vlan"]["specifyvlan"],
            state="Enabled"
        )
        status = validateList(network_offering_vpc_false_before_count)
        self.assertEqual(
            PASS,
            status[0],
            "Default network offering not present for vpc = false")
        # Create network Offering
        network_offering = NetworkOffering.create(
            self.apiClient,
            self.test_data["network_offering_vlan"],
        )
        self.assertIsNotNone(
            network_offering,
            "Network offering is not created")
        # Enable Network offering
        network_offering.update(self.apiClient, state='Enabled')
        self.cleanup.append(network_offering)
        # List network offering
        network_offering_after_count = NetworkOffering.list(self.userapiclient)
        status = validateList(network_offering_after_count)
        self.assertEqual(
            PASS,
            status[0],
            "Network Offering list results in null")
        # Verify that count is incremented by 1
        self.assertEqual(
            len(network_offering_before_count) + 1,
            len(network_offering_after_count),
            "Network offering is not created"
        )
        # List network offering with additional parameter of vpc = true
        network_offering_vpc_true_after_count = NetworkOffering.list(
            self.userapiclient,
            forvpc="true",
            zoneid=self.zone.id,
            guestiptype=self.test_data["network_offering_vlan"]["guestiptype"],
            supportedServices="SourceNat",
            specifyvlan=self.test_data["network_offering_vlan"]["specifyvlan"],
            state="Enabled"
        )
        status = validateList(network_offering_vpc_true_after_count)
        self.assertEqual(
            PASS,
            status[0],
            "Network Offering list results in null")
        # Verify that its count is same as step 1
        self.assertEqual(
            len(network_offering_vpc_true_before_count),
            len(network_offering_vpc_true_after_count),
            "Default Network offering is created with vpc as true"
        )
        # List network offering with additional parameter of vpc = false
        network_offering_vpc_false_after_count = NetworkOffering.list(
            self.userapiclient,
            forvpc="false",
            zoneid=self.zone.id,
            guestiptype=self.test_data["network_offering_vlan"]["guestiptype"],
            supportedServices="SourceNat",
            specifyvlan=self.test_data["network_offering_vlan"]["specifyvlan"],
            state="Enabled"
        )
        status = validateList(network_offering_vpc_false_after_count)
        self.assertEqual(
            PASS,
            status[0],
            "Network Offering list results in null")
        # Verify that its count is same as step 3
        self.assertEqual(
            len(network_offering_vpc_false_before_count) + 1,
            len(network_offering_vpc_false_after_count),
            "Default Network offering is not created with vpc as false"
        )
        return

    @attr(tags=["advanced"], required_hardware="true")
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
        Step7    : Verify vpc name matches for newly created vpc name
                   and name from vpc list
        """
        if self.hypervisor.lower() in ['hyperv']:
            raise unittest.SkipTest("This feature is not supported on existing hypervisor. Hence, skipping the test")
        # List VPC Offering
        vpc_offs_list = VpcOffering.list(self.userapiclient, isdefault="true", name='Default VPC offering')

        if vpc_offs_list is None:
            self.fail("Default VPC offerings not found")
        else:
            vpc_offs = vpc_offs_list[0]
        # List VPCs for newly created user
        vpc_list = VPC.list(
            self.userapiclient,
            listall=self.test_data["listall"]
        )
        # No VPCs should be present for newly created user
        status = validateList(vpc_list)
        self.assertEqual(FAIL, status[0], "VPCs found for newly created user")
        vpc_count_before = 0
        vpc_1 = VPC.create(
            self.userapiclient,
            self.test_data["vpc"],
            vpcofferingid=vpc_offs.id,
            zoneid=self.zone.id,
        )
        self.assertIsNotNone(vpc_1, "VPC is not created")
        # List VPCs
        vpc_list = VPC.list(
            self.userapiclient,
            listall=self.test_data["listall"]
        )
        status = validateList(vpc_list)
        self.assertEqual(PASS, status[0], "VPC is not created")
        self.assertEqual(
            vpc_count_before + 1,
            len(vpc_list),
            "VPC is not created"
        )
        # Listing the networks for a user
        list_networks_before = Network.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            type="Isolated")
        # Verifying listed networks for account created at class level
        self.assertIsNotNone(
            list_networks_before,
            "Network create failed at class level")
        # List network offering for vpc = true
        network_offering_vpc_true_list = NetworkOffering.list(
            self.userapiclient,
            forvpc="true",
            zoneid=self.zone.id,
            guestiptype=self.test_data["network_offering_vlan"]["guestiptype"],
            supportedServices="SourceNat",
            specifyvlan=self.test_data["network_offering_vlan"]["specifyvlan"],
            state="Enabled"
        )
        status = validateList(network_offering_vpc_true_list)
        self.assertEqual(
            PASS,
            status[0],
            "Default network offering not present for vpc = true")
        # Listing networks in VPC
        list_networks_in_vpc = Network.list(self.userapiclient,
                                            vpcid=vpc_1.id,
                                            type="Isolated"
                                            )
        self.assertIsNone(
            list_networks_in_vpc,
            "Networks found for newly created VPC")
        # If number of networks is 1, then creating network
        if len(list_networks_before) == 1:
            network_created = Network.create(
                self.userapiclient,
                self.test_data["network_without_acl"],
                networkofferingid=network_offering_vpc_true_list[0].id,
                vpcid=vpc_1.id,
                gateway=self.test_data["ntwk"]["gateway"],
                netmask=self.test_data["ntwk"]["netmask"],
                domainid=self.domain.id,
                accountid=self.account.name,
            )
            self.assertIsNotNone(
                network_created,
                "Network is not created"
            )
            self.cleanup.append(network_created)
            self.cleanup.append(vpc_1)
            # Creating expected and actual values dictionaries
            expected_dict = {
                "id": self.test_data["network_without_acl"]["zoneid"],
                "name": self.test_data["network_without_acl"]["name"],
            }
            actual_dict = {
                "id": network_created.zoneid,
                "name": network_created.name,
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
            listall=self.test_data["listall"],
            type="Isolated")
        status = validateList(list_networks_after)
        self.assertEqual(PASS, status[0], "No networks found using list call")
        # Asserting for the length of the networks
        self.assertEqual(
            2,
            len(list_networks_after),
            "Number of networks created is not matching expected"
        )
        # Listing networks in VPC after creation of network
        list_networks_in_vpc = Network.list(self.userapiclient,
                                            vpcid=vpc_1.id,
                                            type="Isolated"
                                            )
        status = validateList(list_networks_in_vpc)
        self.assertEqual(PASS, status[0], "No networks found in vpc")
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
        self.assertEqual(PASS, status[0], "VPCs not found.")
        # verify vpc name matches for newly created vpc name and vpc list name
        self.assertEqual(
            vpc_1.name,
            vpc_list[0].name,
            "VPC names not matching"
        )
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_07_create_delete_network(self):
        """
        @Desc: Test delete network
        @Steps
        Step1    : Create Network
        Step2    : Verify Network is created
        Step3    : Delete Network
        Step4    : Verify network is deleted
        """
        # Listing the networks for a user
        list_networks_before = Network.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            type="Isolated")
        # Verifying listed networks for account created at class level
        self.assertIsNotNone(
            list_networks_before,
            "Network create failed at class level")
        # List network offering for vpc = false
        network_offering_vpc_false_list = NetworkOffering.list(
            self.userapiclient,
            forvpc="false",
            zoneid=self.zone.id,
            guestiptype=self.test_data["network_offering_vlan"]["guestiptype"],
            supportedServices="SourceNat",
            specifyvlan=self.test_data["network_offering_vlan"]["specifyvlan"],
            state="Enabled"
        )
        status = validateList(network_offering_vpc_false_list)
        self.assertEqual(
            PASS,
            status[0],
            "Default network offering not present for vpc = false")
        # If number of networks is 1, then creating network
        if len(list_networks_before) == 1:
            network_created = Network.create(
                self.userapiclient,
                self.test_data["network_without_acl"],
                networkofferingid=network_offering_vpc_false_list[0].id,
            )
            self.assertIsNotNone(
                network_created,
                "Network is not created"
            )
            # Creating expected and actual values dictionaries
            expected_dict = {
                "id": self.test_data["network_without_acl"]["zoneid"],
                "name": self.test_data["network_without_acl"]["name"],
            }
            actual_dict = {
                "id": network_created.zoneid,
                "name": network_created.name,
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
            listall=self.test_data["listall"],
            type="Isolated"
        )
        status = validateList(list_networks_after)
        self.assertEqual(PASS, status[0], "No networks found using list call")
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
            listall=self.test_data["listall"],
            type="Isolated"
        )
        status = validateList(list_networks_after_delete)
        self.assertEqual(PASS, status[0], "No networks found using list call")
        self.assertEqual(
            1,
            len(list_networks_after_delete),
            "Number of networks created is not matching expected"
        )
        # Verify deleted network is not present
        self.assertNotEqual(
            network_created.id,
            list_networks_after_delete[0].id,
            "Deleted network present"
        )
        return

    @attr(tags=["advanced"], required_hardware="true")
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
        list_networks_before = Network.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            type="Isolated")
        # Verifying listed networks for account created at class level
        self.assertIsNotNone(
            list_networks_before,
            "Network create failed at class level")
        self.assertEqual(
            1,
            len(list_networks_before),
            "More than 1 network created at class level"
        )
        # List network offering for vpc = false
        network_offering_vpc_false_list = NetworkOffering.list(
            self.userapiclient,
            forvpc="false",
            zoneid=self.zone.id,
            guestiptype=self.test_data["network_offering_vlan"]["guestiptype"],
            supportedServices="SourceNat",
            specifyvlan=self.test_data["network_offering_vlan"]["specifyvlan"],
            state="Enabled"
        )
        status = validateList(network_offering_vpc_false_list)
        self.assertEqual(
            PASS,
            status[0],
            "Default network offering not present for vpc = false")
        # creating network
        network_created = Network.create(
            self.userapiclient,
            self.test_data["network_without_acl"],
            networkofferingid=network_offering_vpc_false_list[0].id,
        )
        self.assertIsNotNone(
            network_created,
            "Network is not created"
        )
        self.cleanup.append(network_created)
        # Creating expected and actual values dictionaries
        expected_dict = {
            "id": self.test_data["network_without_acl"]["zoneid"],
            "name": self.test_data["network_without_acl"]["name"],
        }
        actual_dict = {
            "id": network_created.zoneid,
            "name": network_created.name,
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
        # Listing the networks
        list_networks_after = Network.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            type="Isolated")
        status = validateList(list_networks_after)
        self.assertEqual(PASS, status[0], "No networks found using list call")
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
        list_networks_after_update = Network.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            type="Isolated")
        status = validateList(list_networks_after_update)
        self.assertEqual(PASS, status[0], "No networks found using list call")
        self.assertEqual(
            2,
            len(list_networks_after_update),
            "Number of networks created is not matching expected"
        )
        # Creating expected and actual values dictionaries
        expected_dict = {
            "name": "NewNetworkName",
            "displaytext": "NewNetworkDisplayText",
            "networkdomain": "cs13cloud.internal.new"
        }
        actual_dict = {
            "name": network_updated.name,
            "displaytext": network_updated.displaytext,
            "networkdomain": network_updated.networkdomain
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

    @attr(tags=["advanced"], required_hardware="true")
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
        list_networks_before = Network.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            type="Isolated")
        # Verifying listed networks for account created at class level
        self.assertIsNotNone(
            list_networks_before,
            "Network create failed at class level")
        # Create Virtual Machine
        # Listing all the instances for a user
        list_instances_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.test_data["listall"])
        # Verifying listed instances for account created at class level
        self.assertIsNone(
            list_instances_before,
            "Virtual Machine already exists for newly created user"
        )
        # If number of instances are less than (pagesize  1), then creating
        # them
        for i in range(0, (self.test_data["pagesize"] + 1)):
            vm_created = VirtualMachine.create(
                self.userapiclient,
                self.test_data["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkids=list_networks_before[0].id,
                serviceofferingid=self.service_offering.id,
            )
            self.assertIsNotNone(
                vm_created,
                "VM creation failed"
            )
            if(i < (self.test_data["pagesize"])):
                self.cleanup.append(vm_created)

            self.assertEqual(
                self.test_data["virtual_machine"]["displayname"],
                vm_created.displayname,
                "Newly created VM name and the test data VM\
                        name are not matching")
        # Listing all the instances again after creating VM's
        list_instances_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            networkid=list_networks_before[0].id
        )
        status = validateList(list_instances_after)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of instances after creation failed"
        )
        # Verifying the length of the instances is (page size  1)
        self.assertEqual(
            len(list_instances_after),
            (self.test_data["pagesize"] + 1),
            "Number of instances created is not matching as expected"
        )
        # Listing all the volumes in page1
        list_instances_page1 = VirtualMachine.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            page=1,
            pagesize=self.test_data["pagesize"],
            networkid=list_networks_before[0].id
        )
        status = validateList(list_instances_page1)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of instances in page1 failed"
        )
        # Verifying that the length of the instances in page 1 is (page size)
        self.assertEqual(
            self.test_data["pagesize"],
            len(list_instances_page1),
            "List VM response is not matching with the\
                    page size length for page 1")
        # Listing all the VM's in page2
        list_instances_page2 = VirtualMachine.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            page=2,
            pagesize=self.test_data["pagesize"],
            networkid=list_networks_before[0].id
        )
        status = validateList(list_instances_page2)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of instances in page2 failed"
        )
        # Verifying that the length of the VM's in page 2 is 1
        self.assertEqual(
            1,
            len(list_instances_page2),
            "List VM response is not matching with the\
                    page size length for page 2"
        )
        # Deleting a single VM
        VirtualMachine.delete(vm_created, self.apiClient)
        # Listing the VM's in page 2
        list_instance_response = VirtualMachine.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            page=2,
            pagesize=self.test_data["pagesize"],
            networkid=list_networks_before[0].id
        )
        # verifying that VM does not exists on page 2
        self.assertEqual(
            list_instance_response,
            None,
            "VM was not deleted"
        )
        return

    @attr(tags=["advanced"], required_hardware="true")
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
        Step7    : Verify network name matches for newly created network name
                   and name from network list
        """
        if self.hypervisor.lower() in ['hyperv']:
            raise unittest.SkipTest("This feature is not supported on existing hypervisor. Hence, skipping the test")
        # List VPC Offering
        vpc_offs_list = VpcOffering.list(self.userapiclient, isdefault="true", name='Default VPC offering')
        if vpc_offs_list is None:
            self.fail("Default VPC offerings not found")
        else:
            vpc_offs = vpc_offs_list[0]
        # List VPCs for newly created user
        vpc_list = VPC.list(
            self.userapiclient,
            listall=self.test_data["listall"]
        )
        # No VPCs should be present for newly created user
        status = validateList(vpc_list)
        self.assertEqual(FAIL, status[0], "VPCs found for newly created user")
        vpc_count_before = 0
        vpc_1 = VPC.create(
            self.userapiclient,
            self.test_data["vpc"],
            vpcofferingid=vpc_offs.id,
            zoneid=self.zone.id,
        )
        self.assertIsNotNone(vpc_1, "VPC is not created")
        # List VPCs
        vpc_list = VPC.list(
            self.userapiclient,
            listall=self.test_data["listall"]
        )
        status = validateList(vpc_list)
        self.assertEqual(PASS, status[0], "VPC is not created")
        self.assertEqual(
            vpc_count_before + 1,
            len(vpc_list),
            "VPC is not created"
        )
        # Listing the networks for a user
        list_networks_before = Network.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            type="Isolated")
        # Verifying listed networks for account created at class level
        self.assertIsNotNone(
            list_networks_before,
            "Network create failed at class level")
        # List network offering for vpc = true
        network_offering_vpc_true_list = NetworkOffering.list(
            self.userapiclient,
            forvpc="true",
            zoneid=self.zone.id,
            guestiptype=self.test_data["network_offering_vlan"]["guestiptype"],
            supportedServices="SourceNat",
            specifyvlan=self.test_data["network_offering_vlan"]["specifyvlan"],
            state="Enabled"
        )
        status = validateList(network_offering_vpc_true_list)
        self.assertEqual(
            PASS,
            status[0],
            "Default network offering not present for vpc = true")
        # If number of networks is 1, then creating network
        if len(list_networks_before) == 1:
            network_created = Network.create(
                self.userapiclient,
                self.test_data["network_without_acl"],
                networkofferingid=network_offering_vpc_true_list[0].id,
                vpcid=vpc_1.id,
                gateway=self.test_data["ntwk"]["gateway"],
                netmask=self.test_data["ntwk"]["netmask"]
            )
            self.assertIsNotNone(
                network_created,
                "Network is not created"
            )
            self.cleanup.append(network_created)
            self.cleanup.append(vpc_1)
            # Creating expected and actual values dictionaries
            expected_dict = {
                "id": self.test_data["network_without_acl"]["zoneid"],
                "name": self.test_data["network_without_acl"]["name"],
            }
            actual_dict = {
                "id": network_created.zoneid,
                "name": network_created.name,
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
            listall=self.test_data["listall"],
            type="Isolated")
        status = validateList(list_networks_after)
        self.assertEqual(PASS, status[0], "No networks found using list call")
        # Asserting for the length of the networks
        self.assertEqual(
            2,
            len(list_networks_after),
            "Number of networks created is not matching expected"
        )
        # Listing the networks
        list_networks_in_vpc = Network.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            vpcid=vpc_1.id,
            type="Isolated"
        )
        status = validateList(list_networks_in_vpc)
        self.assertEqual(PASS, status[0], "No networks found using list call")
        # Verify network name matches for newly created network name and name
        # from network list
        self.assertEqual(
            network_created.name,
            list_networks_in_vpc[0].name,
            "Network names not matching"
        )
        return

    @attr(tags=["advanced"], required_hardware="true")
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
        if self.hypervisor.lower() in ['hyperv']:
            raise unittest.SkipTest("This feature is not supported on existing hypervisor. Hence, skipping the test")
        # List VPC Offering
        vpc_offs_list = VpcOffering.list(self.userapiclient, isdefault="true", name='Default VPC offering')
        if vpc_offs_list is None:
            self.fail("Default VPC offerings not found")
        else:
            vpc_offs = vpc_offs_list[0]
        # List VPCs
        vpc_list = VPC.list(
            self.userapiclient,
            listall=self.test_data["listall"]
        )
        # verify no vpc is present for newly created user
        status = validateList(vpc_list)
        self.assertEqual(FAIL, status[0], "VPCs found for newly created user")
        vpc_1 = VPC.create(
            self.userapiclient,
            self.test_data["vpc"],
            vpcofferingid=vpc_offs.id,
            zoneid=self.zone.id,
        )
        self.assertIsNotNone(vpc_1, "VPC is not created")
        self.cleanup.append(vpc_1)
        # verify vpc is created and not none
        # Verify VPC name with test data
        self.assertNotEqual(
            -1,
            vpc_1.name.find(self.test_data["vpc"]["name"]),
            "VPC name not matched"
        )
        # verify zone with test data
        self.assertEqual(
            self.zone.id,
            vpc_1.zoneid,
            "Zone is not matching in the vpc created"
        )
        # Asserting for the length of the VPCs
        vpc_count_after = VPC.list(
            self.userapiclient,
            listall=self.test_data["listall"])
        status = validateList(vpc_count_after)
        self.assertEqual(PASS, status[0], "VPC list count is null")
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
            listall=self.test_data["listall"]
        )
        status = validateList(list_vpcs_after_update)
        self.assertEqual(PASS, status[0], "No vpcs found using list call")
        self.assertEqual(
            1,
            len(list_vpcs_after_update),
            "Number of vpcs created is not matching expected"
        )
        # Creating expected and actual values dictionaries
        expected_dict = {
            "name": "NewVPCName",
            "displaytext": "NewVPCDisplayText",
        }
        actual_dict = {
            "name": vpc_updated.name,
            "displaytext": vpc_updated.displaytext,
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

    @attr(tags=["advanced"], required_hardware="true")
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
        if self.hypervisor.lower() in ['hyperv']:
            raise unittest.SkipTest("This feature is not supported on existing hypervisor. Hence, skipping the test")
        # List VPC Offering
        vpc_offs_list = VpcOffering.list(self.userapiclient, isdefault="true", name='Default VPC offering')

        if vpc_offs_list is None:
            self.fail("Default VPC offerings not found")
        else:
            vpc_offs = vpc_offs_list[0]
        # List VPCs for newly created user
        vpc_list = VPC.list(
            self.userapiclient,
            listall=self.test_data["listall"]
        )
        # No VPCs should be present for newly created user
        status = validateList(vpc_list)
        self.assertEqual(FAIL, status[0], "VPCs found for newly created user")
        vpc_count_before = 0
        vpc_1 = VPC.create(
            self.userapiclient,
            self.test_data["vpc"],
            vpcofferingid=vpc_offs.id,
            zoneid=self.zone.id,
        )
        self.assertIsNotNone(vpc_1, "VPC is not created")
        # List VPCs
        vpc_list = VPC.list(
            self.userapiclient,
            listall=self.test_data["listall"]
        )
        status = validateList(vpc_list)
        self.assertEqual(PASS, status[0], "VPC is not created")
        self.assertEqual(
            vpc_count_before + 1,
            len(vpc_list),
            "VPC is not created"
        )
        # Listing the networks for a user
        list_networks_before = Network.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            type="Isolated"
        )
        # Verifying listed networks for account created at class level
        self.assertIsNotNone(
            list_networks_before,
            "Network create failed at class level")
        # List network offering for vpc = true
        network_offering_vpc_true_list = NetworkOffering.list(
            self.userapiclient,
            forvpc="true",
            zoneid=self.zone.id,
            guestiptype=self.test_data["network_offering_vlan"]["guestiptype"],
            supportedServices="SourceNat",
            specifyvlan=self.test_data["network_offering_vlan"]["specifyvlan"],
            state="Enabled"
        )
        status = validateList(network_offering_vpc_true_list)
        self.assertEqual(
            PASS,
            status[0],
            "Default network offering not present for vpc = true")
        # Listing networks in VPC
        list_networks_in_vpc = Network.list(self.userapiclient,
                                            vpcid=vpc_1.id,
                                            type="Isolated"
                                            )
        self.assertIsNone(
            list_networks_in_vpc,
            "Networks found for newly created VPC")
        # If number of networks is 1, then creating network
        if len(list_networks_before) == 1:
            network_created = Network.create(
                self.userapiclient,
                self.test_data["network_without_acl"],
                networkofferingid=network_offering_vpc_true_list[0].id,
                vpcid=vpc_1.id,
                gateway=self.test_data["ntwk"]["gateway"],
                netmask=self.test_data["ntwk"]["netmask"],
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
                "id": self.test_data["network_without_acl"]["zoneid"],
                "name": self.test_data["network_without_acl"]["name"],
            }
            actual_dict = {
                "id": network_created.zoneid,
                "name": network_created.name,
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
            listall=self.test_data["listall"],
            type="Isolated")
        status = validateList(list_networks_after)
        self.assertEqual(PASS, status[0], "No networks found using list call")
        # Asserting for the length of the networks
        self.assertEqual(
            2,
            len(list_networks_after),
            "Number of networks created is not matching expected"
        )
        # Listing networks in VPC after creation of network
        list_networks_in_vpc = Network.list(self.userapiclient,
                                            vpcid=vpc_1.id,
                                            type="Isolated"
                                            )
        status = validateList(list_networks_in_vpc)
        self.assertEqual(PASS, status[0], "No networks found in vpc")
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
        self.assertIsNone(
            list_network_acl,
            "ACL list is not empty for newly created network")
        # Create NetworkACL
        network_acl_created = NetworkACL.create(
            self.userapiclient,
            self.test_data["network_acl_rule"],
            networkid=network_created.id
        )
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
        self.assertEqual(
            PASS,
            status[0],
            "No networks acls found after creating")
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
        self.assertIsNone(
            list_network_acl,
            "ACL list is not empty for newly created network")
        return


class TestNetworks_2(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(TestNetworks_2, cls).getClsTestClient()
            cls.api_client = cls.testClient.getApiClient()
            cls.test_data = cls.testClient.getParsedTestDataConfig()
            cls.hypervisor = cls.testClient.getHypervisorInfo()
            # Get Domain, Zone, Template
            cls.domain = get_domain(cls.api_client)
            cls.zone = get_zone(
                cls.api_client,
                cls.testClient.getZoneForTests())
            cls.template = get_template(
                cls.api_client,
                cls.zone.id,
                cls.test_data["ostype"]
            )
            cls.test_data['mode'] = cls.zone.networktype
            cls.account = Account.create(
                cls.api_client,
                cls.test_data["account"],
                domainid=cls.domain.id
            )
            # Getting authentication for user in newly created Account
            cls.user = cls.account.user[0]
            cls.userapiclient = cls.testClient.getUserApiClient(
                cls.user.username,
                cls.domain.name)
            cls._cleanup.append(cls.account)

            cls.vpc_offering = VpcOffering.create(cls.api_client,
                                                  cls.test_data["vpc_offering"]
                                                  )
            cls.vpc_offering.update(cls.api_client, state='Enabled')
            cls._cleanup.append(cls.vpc_offering)
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
        Step1: Verifying length of expected and actual dictionaries is matching
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
                    "expected Value: %s, is not matching with actual value: %s"
                    %
                    (exp_val, act_val))
        return return_flag

    @attr(tags=["advanced"], required_hardware="true")
    def test_13_list_vpc_byid(self):
        """
        @summary: Test List VPC with Id
        @Steps
        Step1: Listing all the vpc for a user before creating a vpc
        Step2: Verifying no VPCs are listed
        Step3: Creating a vpc
        Step4: Listing the vpc for a user after creating a vpc
        Step5: Verifying the list vpc size is increased by 1
        Step6: Listing the vpc by specifying vpc ID
        Step7: Verifying the list vpc size is 1
        Step8: Verifying the details of the vpc
        """
        if self.hypervisor.lower() in ['hyperv']:
            raise unittest.SkipTest("This feature is not supported on existing hypervisor. Hence, skipping the test")
        # Listing all the vpc's for a user
        list_vpc_before = VPC.list(self.userapiclient)
        # Verifying No VPCs are listed
        self.assertIsNone(
            list_vpc_before,
            "Vpc listed for newly created user"
        )
        # Creating a vpc
        vpc_created = VPC.create(
            self.userapiclient,
            self.test_data["vpc"],
            self.vpc_offering.id,
            self.zone.id
        )
        self.assertIsNotNone(vpc_created, "VPC Creation Failed")
        self.cleanup.append(vpc_created)
        # Listing the vpc for a user after creating a vpc
        list_vpc_after = VPC.list(self.userapiclient)
        status = validateList(list_vpc_after)
        self.assertEqual(
            PASS,
            status[0],
            "list VPC not as expected"
        )
        # Verifying the list vpc size is increased by 1
        self.assertEqual(
            1,
            len(list_vpc_after),
            "list VPC not equal as expected"
        )
        # List the vpc by specifying vpc ID
        list_vpc_byid = VPC.list(
            self.userapiclient,
            id=vpc_created.id,
            listall=self.test_data["listall"]
        )
        status = validateList(list_vpc_byid)
        self.assertEqual(
            PASS,
            status[0],
            "list VPC not as expected"
        )
        # Verifying the list vpc size is 1
        self.assertEqual(
            1,
            len(list_vpc_byid),
            "list VPC not equal as expected"
        )
        # Verifying the details of the vpc
        expected_dict = {
            "cidr": self.test_data["vpc"]["cidr"],
            "id": vpc_created.id,
            "displaytext": vpc_created.displaytext,
            "account": self.account.name,
            "domain": vpc_created.domain,
            "domainid": self.domain.id,
            "name": vpc_created.name
        }
        actual_dict = {
            "cidr": str(list_vpc_byid[0].cidr),
            "id": list_vpc_byid[0].id,
            "displaytext": list_vpc_byid[0].displaytext,
            "account": list_vpc_byid[0].account,
            "domain": list_vpc_byid[0].domain,
            "domainid": list_vpc_byid[0].domainid,
            "name": list_vpc_byid[0].name
        }
        list_vpc_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            list_vpc_status,
            "Listed VPC details are not as expected"
        )
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_14_list_public_ipaddress_by_associated_networkid(self):
        """
        @summary: Test List Public IPAddress with associatednetworkid
        @Steps
        Step1: Listing all the public ipaddresses for a user before creating
               a public ipaddress
        Step2: Verifying no Public ipaddress are listed
        Step3: Creating a network
        Step4: Associating public ipaddress for network created in step3
        Step5: Listing the public ipaddress for a user after creating a
               public ipaddress
        Step6: Verifying the list public ipaddress size is increased by 1
        Step7: Listing the public ipaddress by specifying associatednetworkid
        Step8: Verifying the list public ipaddress size is 1
        Step9: Verifying the details of the public ipaddress
        """
        # Listing all the public ipaddresses for a user
        list_public_ipaddress_before = PublicIPAddress.list(self.userapiclient)
        # Verifying No VPCs are listed
        self.assertIsNone(
            list_public_ipaddress_before,
            "Public ipaddresses listed for newly created user"
        )
        # Listing Network Offerings
        network_offerings_list = NetworkOffering.list(
            self.apiClient,
            forvpc="false",
            guestiptype="Isolated",
            state="Enabled",
            supportedServices="SourceNat",
            zoneid=self.zone.id
        )
        self.assertIsNotNone(
            network_offerings_list,
            "Isolated Network Offerings with sourceNat enabled are not found"
        )
        # Creating a Network
        network = Network.create(
            self.userapiclient,
            self.test_data["network"],
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
        #  Associating public ipaddress for network created
        public_ipaddress_created = PublicIPAddress.create(
            self.userapiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.domain.id,
            networkid=network.id
        )
        self.assertIsNotNone(
            public_ipaddress_created,
            "Failed to Associate Public IpAddress for Network"
        )
        # Listing the public ipaddress for a user after associating a public
        # ipaddress for network
        list_public_ipaddress_after = PublicIPAddress.list(self.userapiclient)
        status = validateList(list_public_ipaddress_after)
        self.assertEqual(
            PASS,
            status[0],
            "list Public IPAddress not as expected"
        )
        # Verifying the list public ipaddress size is increased by 1
        self.assertEqual(
            1,
            len(list_public_ipaddress_after),
            "list Public IPAddress not equal as expected"
        )
        # List the public ipaddress by specifying public ipaddress ID
        list_public_ipaddress_byid = PublicIPAddress.list(
            self.userapiclient,
            associatednetworkid=network.id,
            listall=self.test_data["listall"]
        )
        status = validateList(list_public_ipaddress_byid)
        self.assertEqual(
            PASS,
            status[0],
            "list public ipaddress not as expected"
        )
        # Verifying the list public ipaddress size is 1
        self.assertEqual(
            1,
            len(list_public_ipaddress_byid),
            "list public ipaddress not equal as expected"
        )
        # Verifying the details of the public ipaddress
        expected_dict = {
            "account": public_ipaddress_created.ipaddress.account,
            "associatednetworkid":
            public_ipaddress_created.ipaddress.associatednetworkid,
            "associatednetworkname":
            public_ipaddress_created.ipaddress.associatednetworkname,
            "domainid": public_ipaddress_created.ipaddress.domainid,
            "forvirtualnetwork":
            public_ipaddress_created.ipaddress.forvirtualnetwork,
            "id": public_ipaddress_created.ipaddress.id,
            "ipaddress": public_ipaddress_created.ipaddress.ipaddress,
            "isportable": public_ipaddress_created.ipaddress.isportable,
            "issourcenat": public_ipaddress_created.ipaddress.issourcenat,
            "isstatisnat": public_ipaddress_created.ipaddress.isstaticnat,
            "issystem": public_ipaddress_created.ipaddress.issystem,
            "networkid": public_ipaddress_created.ipaddress.network,
            "physicalnetworkid":
            public_ipaddress_created.ipaddress.physicalnetworkid,
            "zoneid": public_ipaddress_created.ipaddress.zoneid}
        actual_dict = {
            "account": list_public_ipaddress_byid[0].account,
            "associatednetworkid":
            list_public_ipaddress_byid[0].associatednetworkid,
            "associatednetworkname":
            list_public_ipaddress_byid[0].associatednetworkname,
            "domainid": list_public_ipaddress_byid[0].domainid,
            "forvirtualnetwork":
            list_public_ipaddress_byid[0].forvirtualnetwork,
            "id": list_public_ipaddress_byid[0].id,
            "ipaddress": list_public_ipaddress_byid[0].ipaddress,
            "isportable": list_public_ipaddress_byid[0].isportable,
            "issourcenat": list_public_ipaddress_byid[0].issourcenat,
            "isstatisnat": list_public_ipaddress_byid[0].isstaticnat,
            "issystem": list_public_ipaddress_byid[0].issystem,
            "networkid": list_public_ipaddress_byid[0].network,
            "physicalnetworkid":
            list_public_ipaddress_byid[0].physicalnetworkid,
            "zoneid": list_public_ipaddress_byid[0].zoneid}
        list_public_ipaddress_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            list_public_ipaddress_status,
            "Listed Public IPAddress details are not as expected"
        )
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_15_list_privategateway_byvpcid(self):
        """
        @summary: Test List PrivateGateway by vpcid
        @Steps
        Step1: Creating a VPC
        Step2: Listing all the private gateway before creating a
               private gateway
        Step3: Verifying no Private Gateway are listed
        Step4: Listing the Private Gateway  after creating a Private Gateway
        Step5: Verifying the list Private Gateway size is increased by 1
        Step6: Listing the Private Gateway by specifying VPCID
        Step7: Verifying the list Private Gateway size is 1
        Step8: Verifying the details of the Private Gateway
        """
        if self.hypervisor.lower() in ['hyperv']:
            raise unittest.SkipTest("This feature is not supported on existing hypervisor. Hence, skipping the test")
        # Creating a vpc
        vpc_created = VPC.create(
            self.userapiclient,
            self.test_data["vpc"],
            self.vpc_offering.id,
            self.zone.id
        )
        self.assertIsNotNone(vpc_created, "VPC Creation Failed")
        self.cleanup.append(vpc_created)
        # Listing all the PrivateGateways
        list_private_gateways_before = PrivateGateway.list(
            self.apiClient,
            listall=self.test_data["listall"],
            vpcid=vpc_created.id
        )
        # Verifying No PrivateGateways are listed
        self.assertIsNone(
            list_private_gateways_before,
            "Listed private gateways for newly created VPC "
        )
        # Creating a Private Gateway
        private_gateway_created = PrivateGateway.create(
            self.apiClient,
            vpcid=vpc_created.id,
            gateway=self.test_data["private_gateway"]["gateway"],
            ipaddress=self.test_data["private_gateway"]["ipaddress"],
            netmask=self.test_data["private_gateway"]["netmask"],
            vlan=self.test_data["private_gateway"]["vlan"]
        )
        self.assertIsNotNone(
            private_gateway_created,
            "Private Gateway Creation Failed"
        )
        # Listing all the PrivateGateways
        list_private_gateways_after = PrivateGateway.list(
            self.apiClient,
            listall=self.test_data["listall"],
            vpcid=vpc_created.id
        )
        # Verifying PrivateGateways are listed
        status = validateList(list_private_gateways_after)
        self.assertEqual(PASS, status[0], "Private Gateway Creation Failed")
        self.assertEqual(
            1,
            len(list_private_gateways_after),
            "list Private Gateway not equal as expected"
        )
        # Listing Private Gateway by vpcid
        list_privategateway_byvpcid = PrivateGateway.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            vpcid=vpc_created.id
        )
        status = validateList(list_privategateway_byvpcid)
        self.assertEqual(
            PASS,
            status[0],
            "list private gateway not as expected"
        )
        # Verifying the list private gateway size is 1
        self.assertEqual(
            1,
            len(list_privategateway_byvpcid),
            "list private gateway not equal as expected"
        )
        # Verifying the details of the private gateway
        expected_dict = {
            "account": private_gateway_created.account,
            "aclid": private_gateway_created.aclid,
            "domainid": private_gateway_created.domainid,
            "gateway": self.test_data["private_gateway"]["gateway"],
            "id": private_gateway_created.id,
            "ipaddress": self.test_data["private_gateway"]["ipaddress"],
            "netmask": self.test_data["private_gateway"]["netmask"],
            "sourcenatsupported": private_gateway_created.sourcenatsupported,
            "vlan": self.test_data["private_gateway"]["vlan"],
            "vpcid": private_gateway_created.vpcid,
            "zoneid": private_gateway_created.zoneid
        }
        actual_dict = {
            "account": list_privategateway_byvpcid[0].account,
            "aclid": list_privategateway_byvpcid[0].aclid,
            "domainid": list_privategateway_byvpcid[0].domainid,
            "gateway": list_privategateway_byvpcid[0].gateway,
            "id": list_privategateway_byvpcid[0].id,
            "ipaddress": list_privategateway_byvpcid[0].ipaddress,
            "netmask": list_privategateway_byvpcid[0].netmask,
            "sourcenatsupported":
            list_privategateway_byvpcid[0].sourcenatsupported,
            "vlan": list_privategateway_byvpcid[0].vlan,
            "vpcid": list_privategateway_byvpcid[0].vpcid,
            "zoneid": list_privategateway_byvpcid[0].zoneid}
        list_private_gateway_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            list_private_gateway_status,
            "Listed Private Gateway details are not as expected"
        )
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_16_create_list_delete_egressfirewallrule_bynetworkid(self):
        """
        @summary: Test Create List Delete Egress Firewall Rule by Network ID
        @Steps
        Step1: Creating a Nerwork
        Step2: Listing all the egressfirewall rule before creating a
               egressfirewall rule
        Step3: Verifying no egressfirewall rule are listed
        Step4: Creating a egressfirewall rule for a user
        Step5: Listing the egressfirewall rule by specifying Network ID
        Step7: Verifying the list egressfirewall rule size is 1
        Step8: Verifying the details of the egressfirewall rule
        Step9: Deleting the egressfirewall rule by network id
        Step10: Verifying no egressfirewall rule is listed
        """
        # Listing all the Networks's for a user
        list_networks_before = Network.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            type="Isolated"
        )
        # Verifying No Networks are listed
        self.assertIsNone(
            list_networks_before,
            "Networks listed for newly created User"
        )
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
        # Creating a Network
        network = Network.create(
            self.userapiclient,
            self.test_data["network"],
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
        # Listing Networks after
        list_networks_after = Network.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            type="Isolated"
        )
        status = validateList(list_networks_after)
        self.assertEqual(
            PASS,
            status[0],
            "Network Creation Failed"
        )
        # Verifying network list count is increased by 1
        self.assertEqual(
            1,
            len(list_networks_after),
            "Network Creation Failed"
        )
        # Listing EgressFirewall Rule
        list_egressfirewallrule_before = EgressFireWallRule.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            networkid=network.id
        )
        # Verifying No EgressFirewall Rule are listed
        self.assertIsNone(
            list_egressfirewallrule_before,
            "Listed egressfirewall rule for a user"
        )
        # Creating a EgressFirewall Rule
        egressfirewallrule_created = EgressFireWallRule.create(
            self.userapiclient,
            networkid=network.id,
            protocol=self.test_data["ingress_rule"]["protocol"],
            cidrlist=self.test_data["ingress_rule"]["cidrlist"],
            startport=self.test_data["ingress_rule"]["startport"],
            endport=self.test_data["ingress_rule"]["endport"],
        )
        self.assertIsNotNone(
            egressfirewallrule_created,
            "EgressFirewall Rule Creation Failed"
        )
        # ListingEgressFirewall Rule by networkid
        list_egressfirewallrule_bynetworkid = EgressFireWallRule.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            networkid=network.id
        )
        status = validateList(list_egressfirewallrule_bynetworkid)
        self.assertEqual(
            PASS,
            status[0],
            "list EgressFirewall Rule not as expected"
        )
        # Verifying the list EgressFirewall Rule size is 1
        self.assertEqual(
            1,
            len(list_egressfirewallrule_bynetworkid),
            "list EgressFirewall Rule not equal as expected"
        )
        # Verifying the details of the EgressFirewall Rule
        expected_dict = {
            "cidrlist": self.zone.guestcidraddress,
            "id": egressfirewallrule_created.id,
            "networkid": egressfirewallrule_created.networkid,
            "protocol": self.test_data["ingress_rule"]["protocol"]
        }
        actual_dict = {
            "cidrlist": str(
                list_egressfirewallrule_bynetworkid[0].cidrlist),
            "id": list_egressfirewallrule_bynetworkid[0].id,
            "networkid": list_egressfirewallrule_bynetworkid[0].networkid,
            "protocol": str(
                list_egressfirewallrule_bynetworkid[0].protocol).upper()}
        list_egressfirewallrule_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            list_egressfirewallrule_status,
            "Listed EgressFirewall Rule details are not as expected"
        )
        # Deleting the EgressFirewall Rule by networkid
        egressfirewallrule_created.delete(self.userapiclient)
        # Listing the EgressFirewall Rule by networkid
        list_deleted_egressfirewallrule_bynetworkid = EgressFireWallRule.list(
            self.userapiclient,
            listall=self.test_data["listall"],
            networkid=network.id
        )
        # Verifying No EgressFirewall Rule are listed
        self.assertIsNone(
            list_deleted_egressfirewallrule_bynetworkid,
            "EgressFirewall Rule deletion fails"
        )
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_17_restart_vpc(self):
        """
        @summary: Test to restart VPC
        @Steps
        Step1: Listing the VPC  before creating a VPC for a user
        Step2: Verifying No VPCs are listed
        Step3: Creating a VPC
        Step4: Listing the VPC  after creating a VPC for a user
        Step5: Verifying the list VPC size is increased by 1
        Step6: Restarting the VPC
        Step7: Verifying the Restart function return true
        Step8: Listing the VPC by specifying VPCID
        Step9: Verfying state of vpc
        """
        if self.hypervisor.lower() in ['hyperv']:
            raise unittest.SkipTest("This feature is not supported on existing hypervisor. Hence, skipping the test")
        # Listing all the vpc's for a user
        list_vpc_before = VPC.list(self.userapiclient)
        # Verifying No VPCs are listed
        self.assertIsNone(
            list_vpc_before,
            "Vpc listed for newly created user"
        )
        # Creating a vpc
        vpc_created = VPC.create(
            self.userapiclient,
            self.test_data["vpc"],
            self.vpc_offering.id,
            self.zone.id
        )
        self.assertIsNotNone(vpc_created, "VPC Creation Failed")
        # Listing the vpc for a user after creating a vpc
        list_vpc_after = VPC.list(self.userapiclient)
        status = validateList(list_vpc_after)
        self.assertEqual(
            PASS,
            status[0],
            "list VPC not as expected"
        )
        # Verifying the list vpc size is increased by 1
        self.assertEqual(
            1,
            len(list_vpc_after),
            "list VPC not equal as expected"
        )
        self.cleanup.append(vpc_created)
        # Restarting VPC
        vpc_restarted = VPC.restart(vpc_created, self.userapiclient)
        # Verifying restart function resturns true
        self.assertTrue(vpc_restarted, "Restart fails")
        # List the vpc by specifying vpc ID
        list_vpc_byid = VPC.list(
            self.userapiclient,
            id=vpc_created.id,
            listall=self.test_data["listall"]
        )
        status = validateList(list_vpc_byid)
        self.assertEqual(
            PASS,
            status[0],
            "list VPC not as expected"
        )
        # Verifying the list vpc size is 1
        self.assertEqual(
            1,
            len(list_vpc_byid),
            "list VPC not equal as expected"
        )
        # Verfying whether sate is Enabled
        self.assertEqual(
            "Enabled",
            list_vpc_byid[0].state,
            "Restarted VPC is not in Enabled state"
        )
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_18_create_list_vpn_gateway(self):
        """
        @Desc: Test to Create and list Vpn Gateway by VPCid
        @steps:
        Step1: Create VPC
        Step2: Listing VPN gateways for user before creating any VPN gateway
        Step3: Verifying No VPN Gateways are listed
        Step4: Creating a VPN Gateway using the vpc
        Step5: Listing VPN gateway for user by vpcid
        Step6: Verifying the list size is increased by 1
        Step7: Verifying the details of a VPN gateway
         """
        if self.hypervisor.lower() in ['hyperv']:
            raise unittest.SkipTest("This feature is not supported on existing hypervisor. Hence, skipping the test")
        # Listing all the vpc's for a user
        list_vpc_before = VPC.list(self.userapiclient)
        # Verifying No VPCs are listed
        self.assertIsNone(
            list_vpc_before,
            "Vpc listed for newly created user"
        )
        # Creating a vpc
        vpc_created = VPC.create(
            self.userapiclient,
            self.test_data["vpc"],
            self.vpc_offering.id,
            self.zone.id
        )
        self.assertIsNotNone(vpc_created, "VPC Creation Failed")
        self.cleanup.append(vpc_created)
        # Listing the vpc for a user after creating a vpc
        list_vpc_after = VPC.list(self.userapiclient)
        status = validateList(list_vpc_after)
        self.assertEqual(
            PASS,
            status[0],
            "list VPC not as expected"
        )
        # Verifying the list vpc size is increased by 1
        self.assertEqual(
            1,
            len(list_vpc_after),
            "list VPC not equal as expected"
        )
        # Listing all the vpn gateways for a user
        list_vpngateway_before = Vpn.listVpnGateway(
            self.userapiclient,
            listall=self.test_data["listall"],
            vpcid=vpc_created.id
        )
        # Verifying No VPN Gateways are listed
        self.assertIsNone(
            list_vpngateway_before,
            "Vpn gateway listed for newly created user"
        )
        # Creating a VPN Gateway
        vpngateway_created = Vpn.createVpnGateway(
            self.userapiclient,
            vpcid=vpc_created.id,
        )
        self.assertIsNotNone(
            vpngateway_created,
            "VPN Gateway creation failed"
        )
        # Listing all the VPN Gateways for a User
        list_vpngateway_after = Vpn.listVpnGateway(
            self.userapiclient,
            listall=self.test_data["listall"],
            vpcid=vpc_created.id
        )
        status = validateList(list_vpngateway_after)
        self.assertEqual(
            PASS,
            status[0],
            "List VPN Gateway not equal as expected"
        )
        # Verifying that list size is  1
        self.assertEqual(
            1,
            len(list_vpngateway_after
                ),
            "Failed to list vpn gateway"
        )
        # Verifying the details of the vpn gateway
        expected_dict = {
            "account": vpngateway_created["account"],
            "domainid": vpngateway_created["domainid"],
            "id": vpngateway_created["id"],
            "publicip": vpngateway_created["publicip"],
            "vpcid": vpngateway_created["vpcid"]
        }
        actual_dict = {
            "account": list_vpngateway_after[0].account,
            "domainid": list_vpngateway_after[0].domainid,
            "id": list_vpngateway_after[0].id,
            "publicip": list_vpngateway_after[0].publicip,
            "vpcid": list_vpngateway_after[0].vpcid
        }
        list_vpn_gateway_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            list_vpn_gateway_status,
            "Listed VPN Gateway details are not as expected"
        )
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_19_create_list_reset_delete_vpnconnections(self):
        """
        @Desc: Test to List Create Reset and Delete VPN Customer
               Gateways pagination
        @steps:
        Step1: Create VPC
        Step2: Create VPN gateway
        Step3: Create VPN Customer Gateway
        Step4: Listing all the VPN Connections for a user
        Step5: Verifying No VPN Connection are listed
        Step6: Create a VPN Connection using VPN Gateway and
               VPN Customer Gateway
        Step7: Listing all the VPN Connections by vpcid for a user
        Step8: Verifying that list size is 1
        Step9: Reset the vpn connection
        Step10:List the Vpn connection by vpcid
        Step11: Verify the details of VPN Connection after reset
        Step12: Deleting the VPN Connection
        Step13: Listing all the VPN Connection for a user
        Step14: Verifying that no VPN Connection are listed
        """
        if self.hypervisor.lower() in ['hyperv']:
            raise unittest.SkipTest("This feature is not supported on existing hypervisor. Hence, skipping the test")
        # Listing all the vpc's for a user
        list_vpc_before = VPC.list(self.userapiclient)
        # Verifying No VPCs are listed
        self.assertIsNone(
            list_vpc_before,
            "Vpc listed for newly created user"
        )
        # Creating a vpc
        vpc_created = VPC.create(
            self.userapiclient,
            self.test_data["vpc"],
            self.vpc_offering.id,
            self.zone.id
        )
        self.assertIsNotNone(vpc_created, "VPC Creation Failed")
        self.cleanup.append(vpc_created)
        # Listing the vpc for a user after creating a vpc
        list_vpc_after = VPC.list(self.userapiclient)
        status = validateList(list_vpc_after)
        self.assertEqual(
            PASS,
            status[0],
            "list VPC not as expected"
        )
        # Verifying the list vpc size is increased by 1
        self.assertEqual(
            1,
            len(list_vpc_after),
            "list VPC not equal as expected"
        )
        # Listing all the vpn gateways for a user
        list_vpngateway_before = Vpn.listVpnGateway(
            self.userapiclient,
            listall=self.test_data["listall"],
            vpcid=vpc_created.id
        )
        # Verifying No VPN Gateways are listed
        self.assertIsNone(
            list_vpngateway_before,
            "Vpn gateway listed for newly created user"
        )
        # Creating a VPN Gateway
        vpngateway_created = Vpn.createVpnGateway(
            self.userapiclient,
            vpcid=vpc_created.id,
        )
        self.assertIsNotNone(
            vpngateway_created,
            "VPN Gateway creation failed"
        )
        # Listing all the VPN Gateways for a User
        list_vpngateway_after = Vpn.listVpnGateway(
            self.userapiclient,
            listall=self.test_data["listall"],
            vpcid=vpc_created.id
        )
        status = validateList(list_vpngateway_after)
        self.assertEqual(
            PASS,
            status[0],
            "List VPN Gateway not equal as expected"
        )
        # Verifying that list size is  1
        self.assertEqual(
            1,
            len(list_vpngateway_after
                ),
            "Failed to list vpn gateway"
        )
        # Listing all the VPN Customer Gateways for a User
        list_vpncustomergateways_before = VpnCustomerGateway.list(
            self.userapiclient,
            listall=self.test_data["listall"]
        )
        # Verifying that no VPN Customer Gateways are listed
        self.assertIsNone(
            list_vpncustomergateways_before,
            "VPN Customer Gateways listed for newly created User"
        )
        # Creating  VPN Customer Gateways
        vpncustomergateway_created = VpnCustomerGateway.create(
            self.userapiclient,
            self.test_data["vpncustomergateway"],
            name="VPNCustGateway",
            gateway="10.102.153.1",
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
            listall=self.test_data["listall"],
        )
        status = validateList(list_vpncustomergateways_after)
        self.assertEqual(
            PASS,
            status[0],
            "VPN Customer Gateway list failed"
        )
        # Verifying that list size is 1
        self.assertEqual(
            1,
            len(list_vpncustomergateways_after),
            "Failed to list VPN Customer Gateways"
        )
        # Listing all the vpc's for a user
        list_vpn_connection_before = Vpn.listVpnConnection(
            self.userapiclient,
            listall=self.test_data["listall"],
            vpcid=vpc_created.id
        )
        # Verifying No VPC Connections are listed
        self.assertIsNone(
            list_vpn_connection_before,
            "Vpn connection listed for newly created user"
        )
        # Creating VPN Connection
        vpnconnections_created = Vpn.createVpnConnection(
            self.userapiclient,
            s2scustomergatewayid=vpncustomergateway_created.id,
            s2svpngatewayid=vpngateway_created["id"]
        )
        self.assertIsNotNone(
            vpnconnections_created,
            "VPN Connection creation failed"
        )
        # Listing the vpn connection for a user after creating a vpn connection
        list_vpn_connection_after = Vpn.listVpnConnection(
            self.userapiclient,
            listall=self.test_data["listall"],
            vpcid=vpc_created.id
        )
        status = validateList(list_vpn_connection_after)
        self.assertEqual(
            PASS,
            status[0],
            "list VPN Connection not as expected"
        )
        # Verifying the list vpn connection size is increased by 1
        self.assertEqual(
            1,
            len(list_vpn_connection_after),
            "list VPC Connection equal as expected"
        )
        # Resets VPN Connection
        Vpn.resetVpnConnection(
            self.userapiclient,
            id=list_vpn_connection_after[0].id,
        )
        # Lists VPN Connection by vpcid
        list_vpn_connection_after_reset = Vpn.listVpnConnection(
            self.userapiclient,
            listall=self.test_data["listall"],
            vpcid=vpc_created.id
        )
        status = validateList(list_vpn_connection_after_reset)
        self.assertEqual(
            PASS,
            status[0],
            "list VPN Connection not as expected"
        )
        # Verifying the list vpn connection size is increased by 1
        self.assertEqual(
            1,
            len(list_vpn_connection_after_reset),
            "list VPN Connection not equal as expected"
        )
        # Verifying the details of the vpn connection after reset
        expected_dict = {
            "account": list_vpn_connection_after[0].account,
            "cidrlist": list_vpn_connection_after[0].cidrlist,
            "domain": list_vpn_connection_after[0].domain,
            "dpd": list_vpn_connection_after[0].dpd,
            "gateway": list_vpn_connection_after[0].gateway,
            "id": list_vpn_connection_after[0].id,
            "passive": list_vpn_connection_after[0].passive,
            "publicip": list_vpn_connection_after[0].publicip,
            "s2scustomergatewayid":
            list_vpn_connection_after[0].s2scustomergatewayid,
            "s2svpngatewayid": list_vpn_connection_after[0].s2svpngatewayid}
        actual_dict = {
            "account": list_vpn_connection_after_reset[0].account,
            "cidrlist": list_vpn_connection_after_reset[0].cidrlist,
            "domain": list_vpn_connection_after_reset[0].domain,
            "dpd": list_vpn_connection_after_reset[0].dpd,
            "gateway": list_vpn_connection_after_reset[0].gateway,
            "id": list_vpn_connection_after_reset[0].id,
            "passive": list_vpn_connection_after_reset[0].passive,
            "publicip": list_vpn_connection_after_reset[0].publicip,
            "s2scustomergatewayid":
            list_vpn_connection_after_reset[0].s2scustomergatewayid,
            "s2svpngatewayid":
            list_vpn_connection_after_reset[0].s2svpngatewayid}
        list_vpn_connection_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            list_vpn_connection_status,
            "Listed VPN Connection details are not as expected"
        )
        # Delete VPN Connection
        Vpn.deleteVpnConnection(
            self.userapiclient,
            id=list_vpn_connection_after[0].id
        )
        # Listing all the vpc connections for a user
        list_vpn_connection_after_delete = Vpn.listVpnConnection(
            self.userapiclient,
            listall=self.test_data["listall"],
            vpcid=vpc_created.id
        )
        # Verifying No VPN Connections are listed
        self.assertIsNone(
            list_vpn_connection_after_delete,
            "Vpn connection listed for newly created user"
        )
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_20_list_capabilities(self):
        """
        @summary: Test List Capabilities
        @Steps
        Step1: Listing all the Capabilities for a user
        Step2: Verifying the listcapabilities object is not null
        Step3: Verifying list.cloudstack version is not null
         """
        # Listing all the Capabilities for a user
        listCapabilities = Configurations.listCapabilities(self.userapiclient)
        # Verifying the listcapabilities object is not null
        self.assertIsNotNone(
            listCapabilities,
            "Failed to list Capabilities"
        )
        # Verifying list.cloudstack version is not null
        self.assertIsNotNone(
            listCapabilities.cloudstackversion,
            "Failed to list cloud Stack version"
        )
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_21_listNetworkacls_by_privategateway_aclid(self):
        """
        @summary: Test to list Networkacllists by private gateway aclid
        @Steps
        Step1: Listing the Network acl's for a user
        Step2: Verifying list is not none
        Step3: Creating a VPC
        Step4: Listing the privategateway for user
        Step5: Verifying the no private gateway are listed for a user
        Step6: Creating a private gateway using aclid
        Step7: Listing private gateway for a user
        Step8: Verifying list size is increased by 1
        Step9: Listing Networkacllist by paymentgateway aclid
        """
        if self.hypervisor.lower() in ['hyperv']:
            raise unittest.SkipTest("This feature is not supported on existing hypervisor. Hence, skipping the test")
        # Listing all thenetwork acl list for a user
        list_networkacl = NetworkACLList.list(self.userapiclient)
        self.assertIsNotNone(
            list_networkacl,
            "Failed to list network acl list")
        # Verfying list is not none
        status = validateList(list_networkacl)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to list network acl list"
        )
        # Creating a vpc
        vpc_created = VPC.create(
            self.userapiclient,
            self.test_data["vpc"],
            self.vpc_offering.id,
            self.zone.id
        )
        self.assertIsNotNone(vpc_created, "VPC Creation Failed")
        self.cleanup.append(vpc_created)
        # Listing all the PrivateGateways
        list_private_gateways_before = PrivateGateway.list(
            self.apiClient,
            listall=self.test_data["listall"],
            vpcid=vpc_created.id
        )
        # Verifying No PrivateGateways are listed
        self.assertIsNone(
            list_private_gateways_before,
            "Listed private gateways for newly created VPC "
        )
        # Creating a Private Gateway using aclid
        private_gateway_created = PrivateGateway.create(
            self.apiClient,
            vpcid=vpc_created.id,
            gateway=self.test_data["private_gateway"]["gateway"],
            ipaddress=self.test_data["private_gateway"]["ipaddress"],
            netmask=self.test_data["private_gateway"]["netmask"],
            vlan=self.test_data["private_gateway"]["vlan"],
            aclid=list_networkacl[0].id
        )
        self.assertIsNotNone(
            private_gateway_created,
            "Private Gateway Creation Failed"
        )
        # Listing all the PrivateGateways
        list_private_gateways_after = PrivateGateway.list(
            self.apiClient,
            listall=self.test_data["listall"],
            vpcid=vpc_created.id
        )
        # Verifying PrivateGateways are listed
        status = validateList(list_private_gateways_after)
        self.assertEqual(PASS, status[0], "Failed to list Private Gateway")
        self.assertEqual(
            1,
            len(list_private_gateways_after),
            "list Private Gateway not equal as expected"
        )
        # Listing network acl list by paymentgateway.aclid
        list = NetworkACLList.list(self.userapiclient,
                                   listall=self.test_data["listall"],
                                   id=private_gateway_created.aclid
                                   )
        # Verifying the details of the Network acl list
        expected_dict = {
            "description": list_networkacl[0].description,
            "id": list_networkacl[0].id,
            "name": list_networkacl[0].name
        }
        actual_dict = {
            "description": list[0].description,
            "id": list[0].id,
            "name": list[0].name
        }
        list_networkacl_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            list_networkacl_status,
            "Listed Network acl list details are not as expected"
        )
        return
