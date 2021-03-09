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
from marvin.lib.base import (PublicIPAddress,
                             NetworkOffering,
                             Autoscale,
                             Network,
                             NetworkServiceProvider,
                             Template,
                             VirtualMachine,
                             VPC,
                             VpcOffering,
                             StaticNATRule,
                             FireWallRule,
                             NATRule,
                             Vpn,
                             VpnUser,
                             LoadBalancerRule,
                             Account,
                             ServiceOffering,
                             PhysicalNetwork,
                             User)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
from marvin.lib.utils import validateList, cleanup_resources
from marvin.codes import PASS
from nose.plugins.attrib import attr

class TestIpAddresses(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(TestIpAddresses, cls).getClsTestClient()
            cls.api_client = cls.testClient.getApiClient()
            cls.services = cls.testClient.getParsedTestDataConfig()
            cls.hypervisor = cls.testClient.getHypervisorInfo()
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
            else:
                cls.storagetype = 'shared'
                cls.services["service_offerings"][
                    "tiny"]["storagetype"] = 'shared'

            cls.services['mode'] = cls.zone.networktype
            cls.services["virtual_machine"][
                "hypervisor"] = cls.hypervisor
            cls.services["virtual_machine"]["zoneid"] = cls.zone.id
            cls.services["virtual_machine"]["template"] = cls.template.id
            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.services["service_offerings"]["tiny"]
            )
            cls._cleanup.append(cls.service_offering)
            cls.services['mode'] = cls.zone.networktype

            cls.vpc_offering = VpcOffering.create(cls.api_client,
                                                  cls.services["vpc_offering"]
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

    @attr(tags=["advanced", "dvs"], required_hardware="true")
    def test_01_list_ipaddresses_pagination(self):
        """
        @summary: Test List IP Addresses pagination
        @Steps:
        Step1: Creating a network for the user
        Step2: Listing all the IP Addresses for a user
        Step3: Verifying that no IP Addresses are listed
        Step4: Associating (pagesize + 1) number of IP Addresses
        Step5: Listing all the IP Addresses again
        Step6: Verifying the length of the IP Addresses is (page size + 1)
        Step7: Listing all the IP Addresses in page1
        Step8: Verifying that the length of the IP Addresses in page 1 is
               (page size)
        Step9: Listing all the IP Addresses in page2
        Step10: Verifying that the length of the IP Addresses in page 2 is 1
        Step11: Dis-Associating the IP Addresses present in page 2
        Step12: Listing for the IP Addresses on page 2
        Step13: Verifying that no IP Addresses are listed
        """
        # Listing all the networks available
        networks_list_before = Network.list(
            self.userapiclient,
            forvpc="false",
            domainid=self.domain.id,
            account=self.account.name,
            type="Isolated"
        )
        self.assertIsNone(
            networks_list_before,
            "Networks listed for newly created user"
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
        status = validateList(network_offerings_list)
        self.assertEqual(
            PASS,
            status[0],
            "Isolated Network Offerings with sourceNat enabled are not found"
        )
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
        # Listing all the networks available
        networks_list_after = Network.list(
            self.userapiclient,
            forvpc="false",
            domainid=self.domain.id,
            account=self.account.name,
            type="Isolated"
        )
        status = validateList(networks_list_after)
        self.assertEqual(
            PASS,
            status[0],
            "Network Creation Failed"
        )
        self.assertEqual(
            1,
            len(networks_list_after),
            "Network creation failed"
        )
        # Listing all the IP Addresses for a user
        list_ipaddresses_before = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        self.assertIsNone(
            list_ipaddresses_before,
            "IP Addresses listed for newly created user"
        )
        # Associating (pagesize + 1) number of IP Addresses
        for i in range(0, (self.services["pagesize"] + 1)):
            ipaddress = PublicIPAddress.create(
                self.userapiclient,
                services=self.services["network"],
                networkid=network.id
            )
            self.assertIsNotNone(
                ipaddress,
                "Failed to Associate IP Address"
            )

        # Listing all the IP Addresses for a user
        list_ipaddresses_after = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddresses_after)
        self.assertEqual(
            PASS,
            status[0],
            "IP Addresses Association Failed"
        )
        # Verifying the length of the volumes is (page size + 1)
        self.assertEqual(
            (self.services["pagesize"] + 1),
            len(list_ipaddresses_after),
            "Number of IP Addresses associated are not matching expected"
        )
        # Listing IP Address in page 1
        list_ipaddress_page1 = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"]
        )
        status = validateList(list_ipaddress_page1)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to list IP Addresses in page1"
        )
        # Verifying that list size is equals to pagesize
        self.assertEqual(
            self.services["pagesize"],
            len(list_ipaddress_page1),
            "Failed to list pagesize number of IP Addresses in page1"
        )
        # Listing IP Address in page 2
        list_ipaddress_page2 = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=2,
            pagesize=self.services["pagesize"]
        )
        status = validateList(list_ipaddress_page2)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to list IP Addresses in page2"
        )
        # Verifying that List size is equal to 1
        self.assertEqual(
            1,
            len(list_ipaddress_page2),
            "Failed to list IP Addresses in page2"
        )
        # Dis-associating an IP Address
        ipaddress.delete(self.userapiclient)
        # Listing IP Address in page 2
        list_ipaddress_page2 = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=2,
            pagesize=self.services["pagesize"]
        )
        # Verifying that no IP Addresses are listed
        self.assertIsNone(
            list_ipaddress_page2,
            "Disassociation of IP Address Failed"
        )
        self.cleanup.append(self.account)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_02_list_ipaddresses_byid(self):
        """
        @summary: Test List IP Addresses details by ID
        @Steps:
        Step1: Creating a network for the user
        Step2: Listing all the IP Addresses for a user
        Step3: Verifying that no IP Addresses are listed
        Step4: Associating an IP Addresses for Network
        Step5: Listing all the IP Addresses again
        Step6: Verifying the length of the IP Addresses is 1
        Step7: Listing  the IP Addresses by Id
        Step8: Verifying that the length of the IP Addresses list is 1
        Step9: Verifying the details of the Listed IP Address
        """
        # Listing all the networks available
        networks_list_before = Network.list(
            self.userapiclient,
            forvpc="false",
            domainid=self.domain.id,
            account=self.account.name,
            type="Isolated"
        )
        self.assertIsNone(
            networks_list_before,
            "Networks listed for newly created user"
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
        status = validateList(network_offerings_list)
        self.assertEqual(
            PASS,
            status[0],
            "Isolated Network Offerings with sourceNat enabled are not found"
        )
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
        # Listing all the networks available
        networks_list_after = Network.list(
            self.userapiclient,
            forvpc="false",
            domainid=self.domain.id,
            account=self.account.name,
            type="Isolated"
        )
        status = validateList(networks_list_after)
        self.assertEqual(
            PASS,
            status[0],
            "Network Creation Failed"
        )
        self.assertEqual(
            1,
            len(networks_list_after),
            "Network creation failed"
        )
        # Listing the Network By ID
        network_list_byid = Network.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=network.id
        )
        status = validateList(network_list_byid)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to list Network by Id"
        )
        self.assertEqual(
            1,
            len(network_list_byid),
            "Failed to list Network by Id"
        )
        # Listing all the IP Addresses for a user
        list_ipaddresses_before = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        self.assertIsNone(
            list_ipaddresses_before,
            "IP Addresses listed for newly created user"
        )
        # Associating an IP Addresses to Network created
        associated_ipaddress = PublicIPAddress.create(
            self.userapiclient,
            services=self.services["network"],
            networkid=network_list_byid[0].id
        )
        self.assertIsNotNone(
            associated_ipaddress,
            "Failed to Associate IP Address"
        )
        # Listing all the IP Addresses for a user
        list_ipaddresses_after = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddresses_after)
        self.assertEqual(
            PASS,
            status[0],
            "IP Addresses Association Failed"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_ipaddresses_after),
            "Number of IP Addresses associated are not matching expected"
        )
        # Listing IP Address by id
        list_ipaddress_byid = PublicIPAddress.list(
            self.userapiclient,
            id=associated_ipaddress.ipaddress.id
        )
        status = validateList(list_ipaddress_byid)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to list IP Addresses by ID"
        )
        # Verifying that list size is equals to 1
        self.assertEqual(
            1,
            len(list_ipaddress_byid),
            "Failed to list IP Addresses by ID"
        )
        # Verifying details of the listed IP Address to be same as
        # IP Address created above
        # Creating expected and actual values dictionaries
        expected_dict = {
            "id": associated_ipaddress.ipaddress.id,
            "associatednetworkid":
            associated_ipaddress.ipaddress.associatednetworkid,
            "associatednetworkname":
            associated_ipaddress.ipaddress.associatednetworkname,
            "ipaddress": associated_ipaddress.ipaddress.ipaddress,
            "issourcenat": associated_ipaddress.ipaddress.issourcenat,
            "isstaticnat": associated_ipaddress.ipaddress.isstaticnat,
            "networkid": associated_ipaddress.ipaddress.networkid}
        actual_dict = {
            "id": list_ipaddress_byid[0].id,
            "associatednetworkid":
            list_ipaddress_byid[0].associatednetworkid,
            "associatednetworkname":
            list_ipaddress_byid[0].associatednetworkname,
            "ipaddress": list_ipaddress_byid[0].ipaddress,
            "issourcenat": list_ipaddress_byid[0].issourcenat,
            "isstaticnat": list_ipaddress_byid[0].isstaticnat,
            "networkid": list_ipaddress_byid[0].networkid}
        ipaddress_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            ipaddress_status,
            "Listed IP Address details are not as expected"
        )
        self.cleanup.append(self.account)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_03_associate_ipaddress_for_vpc(self):
        """
        @summary: Test to Associate IP Address for VPC
        @Steps:
        Step1: Creating a VPC for the user
        Step2: Listing all the IP Addresses for a user
        Step3: Verifying that 1 IP Addresses is listed
        Step4: Associating an IP Addresses for VPC
        Step5: Listing all the IP Addresses again
        Step6: Verifying the length of the IP Addresses list is 2
        Step7: Listing  the IP Addresses by Id
        Step8: Verifying that the length of the IP Addresses list is 1
        Step9: Verifying the details of the Listed IP Address
        """
        if self.hypervisor.lower() in ['hyperv']:
            self.skipTest("This feature is not supported on existing hypervisor. Hence, skipping the test")
        # Listing all the vpc's for a user
        list_vpc_before = VPC.list(self.userapiclient)
        # Verifying No VPCs are listed
        self.assertIsNone(
            list_vpc_before,
            "VPC's Listed for newly Created User"
        )
        # Creating a vpc
        vpc_created = VPC.create(
            self.userapiclient,
            self.services["vpc"],
            self.vpc_offering.id,
            self.zone.id
        )
        self.assertIsNotNone(
            vpc_created,
            "VPC Creation Failed"
        )
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
        # Listing all the IP Addresses for a user
        list_ipaddresses_before = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddresses_before)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to List VPC IP Address"
        )
        self.assertEqual(
            1,
            len(list_ipaddresses_before),
            "Failed to List VPC IP Address"
        )
        # Associating an IP Addresses to VPC created
        associated_ipaddress = PublicIPAddress.create(
            self.userapiclient,
            services=self.services["network"],
            vpcid=vpc_created.id
        )
        self.assertIsNotNone(
            associated_ipaddress,
            "Failed to Associate IP Address"
        )
        # Listing all the IP Addresses for a user
        list_ipaddresses_after = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddresses_after)
        self.assertEqual(
            PASS,
            status[0],
            "IP Addresses Association Failed"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            len(list_ipaddresses_before) + 1,
            len(list_ipaddresses_after),
            "Number of IP Addresses associated are not matching expected"
        )
        # Listing IP Address by id
        list_ipaddress_byid = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            id=associated_ipaddress.ipaddress.id
        )
        status = validateList(list_ipaddress_byid)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to list IP Addresses by ID"
        )
        # Verifying that list size is equals to 1
        self.assertEqual(
            1,
            len(list_ipaddress_byid),
            "Failed to list IP Addresses by ID"
        )
        # Verifying details of the listed IP Address to be same as
        # IP Address created above
        # Creating expected and actual values dictionaries
        expected_dict = {
            "id": associated_ipaddress.ipaddress.id,
            "associatednetworkid":
            associated_ipaddress.ipaddress.associatednetworkid,
            "associatednetworkname":
            associated_ipaddress.ipaddress.associatednetworkname,
            "ipaddress": associated_ipaddress.ipaddress.ipaddress,
            "issourcenat": associated_ipaddress.ipaddress.issourcenat,
            "isstaticnat": associated_ipaddress.ipaddress.isstaticnat,
            "networkid": associated_ipaddress.ipaddress.networkid,
            "vpcid": associated_ipaddress.ipaddress.vpcid}
        actual_dict = {
            "id": list_ipaddress_byid[0].id,
            "associatednetworkid":
            list_ipaddress_byid[0].associatednetworkid,
            "associatednetworkname":
            list_ipaddress_byid[0].associatednetworkname,
            "ipaddress": list_ipaddress_byid[0].ipaddress,
            "issourcenat": list_ipaddress_byid[0].issourcenat,
            "isstaticnat": list_ipaddress_byid[0].isstaticnat,
            "networkid": list_ipaddress_byid[0].networkid,
            "vpcid": list_ipaddress_byid[0].vpcid}
        ipaddress_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            ipaddress_status,
            "Listed IP Address details are not as expected"
        )
        self.cleanup.append(self.account)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_04_create_delete_lbrule_fornonvpc(self):
        """
        @summary: Test to list, create and delete Load Balancer Rule
        for IP Address associated to Non VPC network
        @Steps:
        Step1: Creating a Network for the user
        Step2: Associating an IP Addresses for Network
        Step3: Listing Load Balancer Rules for the IP Address associated in
               Step2
        Step4: Verifying that no Load Balancer Rules are listed
        Step5: Creating a Load Balancer Rule for IP Address associated in
               Step2
        Step6: Listing Load Balancer Rules for the IP Address associated in
               Step2
        Step7: Verifying 1 Load Balancer Rule is listed
        Step8: Deleting the Load Balancer Rule created in Step5
        Step9: Listing Load Balancer Rules for the IP Address associated in
               Step2
        Step10: Verifying that no Load Balancer Rules are listed
        """
        # Listing all the Networks's for a user
        list_networks_before = Network.list(
            self.userapiclient,
            listall=self.services["listall"],
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
            supportedservices="SourceNat,Lb",
            zoneid=self.zone.id
        )
        status = validateList(network_offerings_list)
        self.assertEqual(
            PASS,
            status[0],
            "Isolated Network Offerings with sourceNat, Lb enabled are\
                    not found"
        )
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
        # Listing all the IP Addresses for a user
        list_ipaddresses_before = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        # Verifying no IP Addresses are listed
        self.assertIsNone(
            list_ipaddresses_before,
            "IP Addresses listed for newly created User"
        )
        # Associating an IP Addresses to Network created
        associated_ipaddress = PublicIPAddress.create(
            self.userapiclient,
            services=self.services["network"],
            networkid=network.id
        )
        self.assertIsNotNone(
            associated_ipaddress,
            "Failed to Associate IP Address"
        )
        # Listing all the IP Addresses for a user
        list_ipaddresses_after = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddresses_after)
        self.assertEqual(
            PASS,
            status[0],
            "IP Addresses Association Failed"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_ipaddresses_after),
            "Number of IP Addresses associated are not matching expected"
        )
        # Listing Load Balancer Rules for the Ip Address
        list_lbrules_before = LoadBalancerRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            publicipid=associated_ipaddress.ipaddress.id
        )
        # Verifying no Load Balancer Rules are listed
        self.assertIsNone(
            list_lbrules_before,
            "Load Balancer Rules listed for newly Acquired Ip Address"
        )
        self.services["lbrule"]["openfirewall"] = 'false'
        # Creating a Load Balancer Rule for Ip Address
        lb_rule = LoadBalancerRule.create(
            self.userapiclient,
            self.services["lbrule"],
            ipaddressid=associated_ipaddress.ipaddress.id,
        )
        self.assertIsNotNone(
            lb_rule,
            "Failed to create Load Balancer Rule"
        )
        # Verifying details of created Load Balancer Rule
        # Creating expected and actual values dictionaries
        expected_dict = {
            "algorithm": self.services["lbrule"]["alg"],
            "privateport": str(self.services["lbrule"]["privateport"]),
            "publicport": str(self.services["lbrule"]["publicport"]),
            "name": self.services["lbrule"]["name"],
        }
        actual_dict = {
            "algorithm": str(lb_rule.algorithm),
            "privateport": str(lb_rule.privateport),
            "publicport": str(lb_rule.publicport),
            "name": str(lb_rule.name),
        }
        lbrule_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            lbrule_status,
            "Created Load Balancer Rule details are not as expected"
        )
        # Listing Load Balancer Rules for the Ip Address
        list_lbrules_after = LoadBalancerRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            publicipid=associated_ipaddress.ipaddress.id
        )
        status = validateList(list_lbrules_after)
        self.assertEqual(
            PASS,
            status[0],
            "Load Balancer Rule creation Failed"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_lbrules_after),
            "Load Balancer Rule creation Failed"
        )
        # Deleting Load Balancer Rule
        lb_rule.delete(self.userapiclient)
        # Listing Load Balancer Rules for the Ip Address
        list_lbrules_after = LoadBalancerRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            publicipid=associated_ipaddress.ipaddress.id
        )
        # Verifying no Load Balancer Rules are Listed
        self.assertIsNone(
            list_lbrules_after,
            "Failed to delete Load Balancer Rule"
        )
        self.cleanup.append(self.account)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_05_create_delete_lbrule_forvpc(self):
        """
        @summary: Test to list, create and delete Load Balancer Rule for IP
        Address associated to VPC
        @Steps:
        Step1: Creating a VPC for the user
        Step2: Creating Network inside VPC
        Step3: Associating an IP Addresses for VPC
        Step4: Listing Load Balancer Rules for the IP Address associated in
               Step2
        Step5: Verifying that no Load Balancer Rules are listed
        Step6: Creating a Load Balancer Rule for IP Address associated in
               Step2
        Step7: Listing Load Balancer Rules for the IP Address associated in
               Step2
        Step8: Verifying 1 Load Balancer Rule is listed
        Step9: Deleting the Load Balancer Rule created in Step5
        Step10: Listing Load Balancer Rules for the IP Address associated in
               Step2
        Step11: Verifying that no Load Balancer Rules are listed
        """
        if self.hypervisor.lower() in ['hyperv']:
            self.skipTest("This feature is not supported on existing hypervisor. Hence, skipping the test")
        # Listing all the vpc's for a user
        list_vpc_before = VPC.list(self.userapiclient)
        # Verifying No VPCs are listed
        self.assertIsNone(
            list_vpc_before,
            "VPC's Listed for newly Created User"
        )
        # Creating a vpc
        vpc_created = VPC.create(
            self.userapiclient,
            self.services["vpc"],
            self.vpc_offering.id,
            self.zone.id
        )
        self.assertIsNotNone(
            vpc_created,
            "VPC Creation Failed"
        )
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
        # List network offering for vpc = true
        network_offering_vpc_true_list = NetworkOffering.list(
            self.userapiclient,
            forvpc="true",
            zoneid=self.zone.id,
            supportedServices="Lb",
            state="Enabled"
        )
        status = validateList(network_offering_vpc_true_list)
        self.assertEqual(
            PASS,
            status[0],
            "Default network offering not present for vpc = true with Lb")
        # Creating network under VPC
        network_created = Network.create(
            self.userapiclient,
            self.services["ntwk"],
            networkofferingid=network_offering_vpc_true_list[0].id,
            vpcid=vpc_created.id,
            zoneid=self.zone.id,
            gateway=self.services["ntwk"]["gateway"],
            netmask=self.services["ntwk"]["netmask"]
        )
        self.cleanup.append(network_created)
        self.assertIsNotNone(
            network_created,
            "Network is not created"
        )
        self.cleanup.append(vpc_created)
        # Listing all the IP Addresses for a user
        list_ipaddresses_before = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddresses_before)
        self.assertEqual(
            PASS,
            status[0],
            "list IP Addresses not as expected"
        )
        # Verifying the list vpc size is increased by 1
        self.assertEqual(
            1,
            len(list_ipaddresses_before),
            "list IP Addresses not equal as expected"
        )
        # Associating an IP Addresses to VPC created
        associated_ipaddress = PublicIPAddress.create(
            self.userapiclient,
            services=self.services["network"],
            vpcid=vpc_created.id
        )
        self.assertIsNotNone(
            associated_ipaddress,
            "Failed to Associate IP Address"
        )
        # Listing all the IP Addresses for a user
        list_ipaddresses_after = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddresses_after)
        self.assertEqual(
            PASS,
            status[0],
            "IP Addresses Association Failed"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            len(list_ipaddresses_before) + 1,
            len(list_ipaddresses_after),
            "Number of IP Addresses associated are not matching expected"
        )
        # Listing Load Balancer Rules for the Ip Address
        list_lbrules_before = LoadBalancerRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            publicipid=associated_ipaddress.ipaddress.id
        )
        # Verifying no Load Balancer Rules are listed
        self.assertIsNone(
            list_lbrules_before,
            "Load Balancer Rules listed for newly Acquired Ip Address"
        )
        self.services["lbrule"]["openfirewall"] = 'false'
        # Creating a Load Balancer Rule for Ip Address
        lb_rule = LoadBalancerRule.create(
            self.userapiclient,
            self.services["lbrule"],
            ipaddressid=associated_ipaddress.ipaddress.id,
            networkid=network_created.id
        )
        self.assertIsNotNone(
            lb_rule,
            "Failed to create Load Balancer Rule"
        )
        # Verifying details of created Load Balancer Rule
        # Creating expected and actual values dictionaries
        expected_dict = {
            "algorithm": self.services["lbrule"]["alg"],
            "privateport": str(self.services["lbrule"]["privateport"]),
            "publicport": str(self.services["lbrule"]["publicport"]),
            "name": self.services["lbrule"]["name"],
        }
        actual_dict = {
            "algorithm": str(lb_rule.algorithm),
            "privateport": str(lb_rule.privateport),
            "publicport": str(lb_rule.publicport),
            "name": str(lb_rule.name),
        }
        lbrule_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            lbrule_status,
            "Created Load Balancer Rule details are not as expected"
        )
        # Listing Load Balancer Rules for the Ip Address
        list_lbrules_after = LoadBalancerRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            publicipid=associated_ipaddress.ipaddress.id,
        )
        status = validateList(list_lbrules_after)
        self.assertEqual(
            PASS,
            status[0],
            "Load Balancer Rule creation Failed"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_lbrules_after),
            "Load Balancer Rule creation Failed"
        )
        # Deleting Load Balancer Rule
        lb_rule.delete(self.userapiclient)
        # Listing Load Balancer Rules for the Ip Address
        list_lbrules_after = LoadBalancerRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            publicipid=associated_ipaddress.ipaddress.id
        )
        # Verifying no Load Balancer Rules are Listed
        self.assertIsNone(
            list_lbrules_after,
            "Failed to delete Load Balancer Rule"
        )
        self.cleanup.append(self.account)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_06_update_lbrule_name(self):
        """
        @summary: Test to Update Load Balancer Rule Name for
                  IP Address associated to Non VPC network
        @Steps:
        Step1: Creating a Network for the user
        Step2: Associating an IP Addresses for Network
        Step3: Listing Load Balancer Rules for the IP Address associated
               in Step2
        Step4: Verifying that no Load Balancer Rules are listed
        Step5: Creating a Load Balancer Rule for IP Address associated in Step2
        Step6: Listing Load Balancer Rules for the IP Address associated in
               Step2
        Step7: Verifying 1 Load Balancer Rule is listed
        Step8: Updating the Load Balancer Rule created in Step5
        Step9: Verifying that Load Balancer Rule details are updated
        """
        # Listing all the Networks's for a user
        list_networks_before = Network.list(
            self.userapiclient,
            listall=self.services["listall"],
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
            supportedservices="SourceNat,Lb",
            zoneid=self.zone.id
        )
        status = validateList(network_offerings_list)
        self.assertEqual(
            PASS,
            status[0],
            "Isolated Network Offerings with sourceNat,\
                    Lb enabled are not found"
        )
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
        # Listing Networks again
        list_networks_after = Network.list(
            self.userapiclient,
            listall=self.services["listall"],
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
        # Listing all the IP Addresses for a user
        list_ipaddresses_before = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        # Verifying no IP Addresses are listed
        self.assertIsNone(
            list_ipaddresses_before,
            "IP Addresses listed for newly created User"
        )
        # Associating an IP Addresses to Network created
        associated_ipaddress = PublicIPAddress.create(
            self.userapiclient,
            services=self.services["network"],
            networkid=network.id
        )
        self.assertIsNotNone(
            associated_ipaddress,
            "Failed to Associate IP Address"
        )
        # Listing all the IP Addresses for a user
        list_ipaddresses_after = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddresses_after)
        self.assertEqual(
            PASS,
            status[0],
            "IP Addresses Association Failed"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_ipaddresses_after),
            "Number of IP Addresses associated are not matching expected"
        )
        # Listing Load Balancer Rules for the Ip Address
        list_lbrules_before = LoadBalancerRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            publicipid=associated_ipaddress.ipaddress.id
        )
        # Verifying no Load Balancer Rules are listed
        self.assertIsNone(
            list_lbrules_before,
            "Load Balancer Rules listed for newly Acquired Ip Address"
        )
        self.services["lbrule"]["openfirewall"] = 'false'
        # Creating a Load Balancer Rule for Ip Address
        lb_rule = LoadBalancerRule.create(
            self.userapiclient,
            self.services["lbrule"],
            ipaddressid=associated_ipaddress.ipaddress.id,
        )
        self.assertIsNotNone(
            lb_rule,
            "Failed to create Load Balancer Rule"
        )
        # Listing Load Balancer Rules for the Ip Address
        list_lbrules_after = LoadBalancerRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            publicipid=associated_ipaddress.ipaddress.id
        )
        status = validateList(list_lbrules_after)
        self.assertEqual(
            PASS,
            status[0],
            "Load Balancer Rule creation Failed"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_lbrules_after),
            "Load Balancer Rule creation Failed"
        )
        # Updating Load Balancer Rule Name
        updated_lb_rule = LoadBalancerRule.update(
            lb_rule,
            self.userapiclient,
            algorithm="source",
            name="NewLBRuleName"
        )
        self.assertIsNotNone(
            updated_lb_rule,
            "Failed to update Load Balancer Rule details"
        )
        # Verifying details of the updated Load Balancer Rule
        # Creating expected and actual values dictionaries
        expected_dict = {
            "id": lb_rule.id,
            "account": lb_rule.account,
            "algorithm": "source",
                         "domainid": lb_rule.domainid,
                         "name": "NewLBRuleName",
                         "networkid": lb_rule.networkid,
                         "zoneid": lb_rule.zoneid,
                         "privateport": lb_rule.privateport,
                         "publicip": lb_rule.publicip,
                         "publicport": lb_rule.publicport,
        }
        actual_dict = {
            "id": updated_lb_rule.id,
            "account": updated_lb_rule.account,
            "algorithm": updated_lb_rule.algorithm,
            "domainid": updated_lb_rule.domainid,
            "name": updated_lb_rule.name,
            "networkid": updated_lb_rule.networkid,
            "zoneid": updated_lb_rule.zoneid,
            "privateport": updated_lb_rule.privateport,
            "publicip": updated_lb_rule.publicip,
            "publicport": updated_lb_rule.publicport,
        }
        lbrule_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            lbrule_status,
            "Updated Load Balancer Rule details are not as expected"
        )
        self.cleanup.append(self.account)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_07_assign_remove_lbrule_toinstance(self):
        """
        @summary: Test to Assign and Remove Load Balancer Rule to an Instance
        @Steps:
        Step1: Creating a Network for the user
        Step2: Associating an IP Addresses for Network
        Step3: Launching a VM using the network created in Step 1
        Step4: Creating a Load Balancer Rule for IP Address associated in Step2
        Step5: Listing Load Balancer Rule Instances for applied as true
        Step6: Verifying no Load balancer rule instances are listed
        Step7: Listing Load Balancer Rule Instances for applied as false
        Step8: Verifying that list size is 1
        Step9: Assigning the Instance to Load Balancer Rule
        Step10: Listing Load Balancer Rule Instances for applied as true
        Step11: Verifying list size is 1
        Step12: Listing Load Balancer Rule Instances for applied as false
        Step13: Verifying no Load balancer rule instances are listed
        Step14: Removing the Load Balancer Rule assigned form Instance
        Step15: Listing Load Balancer Rule Instances for applied as true
        Step16: Verifying no Load balancer rule instances are listed
        Step17: Listing Load Balancer Rule Instances for applied as false
        Step18: Verifying that list size is 1
        """
        # Listing all the Networks's for a user
        list_networks_before = Network.list(
            self.userapiclient,
            listall=self.services["listall"],
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
            supportedservices="SourceNat,Lb",
            zoneid=self.zone.id
        )
        status = validateList(network_offerings_list)
        self.assertEqual(
            PASS,
            status[0],
            "Isolated Network Offerings with sourceNat,\
                    Lb enabled are not found"
        )
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
        # Listing all the IP Addresses for a user
        list_ipaddresses_before = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        # Verifying no IP Addresses are listed
        self.assertIsNone(
            list_ipaddresses_before,
            "IP Addresses listed for newly created User"
        )
        # Associating an IP Addresses to Network created
        associated_ipaddress = PublicIPAddress.create(
            self.userapiclient,
            services=self.services["network"],
            networkid=network.id
        )
        self.assertIsNotNone(
            associated_ipaddress,
            "Failed to Associate IP Address"
        )
        # Listing all the IP Addresses for a user
        list_ipaddresses_after = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddresses_after)
        self.assertEqual(
            PASS,
            status[0],
            "IP Addresses Association Failed"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_ipaddresses_after),
            "Number of IP Addresses associated are not matching expected"
        )
        # Launching a Virtual Machine with above created Network
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=network.id,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "Failed to launch a VM under network created"
        )
        self.cleanup.append(network)
        # Listing Virtual Machines in Running state in the network created
        # above
        list_vms_running = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            account=self.account.name,
            domainid=self.domain.id,
            state="Running",
            networkid=network.id
        )
        status = validateList(list_vms_running)
        self.assertEqual(
            PASS,
            status[0],
            "VM Created is not in Running state"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_vms_running),
            "VM Created is not in Running state"
        )
        self.assertEqual(
            vm_created.id,
            list_vms_running[0].id,
            "VM Created is not in Running state"
        )
        # Listing Virtual Machines in Stopped state in the network created
        # above
        list_vms_stopped = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            account=self.account.name,
            domainid=self.domain.id,
            state="Stopped",
            networkid=network.id
        )
        # Verifying that no vms are listed
        self.assertIsNone(
            list_vms_stopped,
            "Created VM is in Stopped state"
        )
        # Listing Load Balancer Rules for the Ip Address
        list_lbrules_before = LoadBalancerRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            publicipid=associated_ipaddress.ipaddress.id
        )
        # Verifying no Load Balancer Rules are listed
        self.assertIsNone(
            list_lbrules_before,
            "Load Balancer Rules listed for newly Acquired Ip Address"
        )
        self.services["lbrule"]["openfirewall"] = 'false'
        # Creating a Load Balancer Rule for Ip Address
        lb_rule = LoadBalancerRule.create(
            self.userapiclient,
            self.services["lbrule"],
            ipaddressid=associated_ipaddress.ipaddress.id,
        )
        self.assertIsNotNone(
            lb_rule,
            "Failed to create Load Balancer Rule"
        )
        # Listing Load Balancer Rules for the Ip Address
        list_lbrules_after = LoadBalancerRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            publicipid=associated_ipaddress.ipaddress.id
        )
        status = validateList(list_lbrules_after)
        self.assertEqual(
            PASS,
            status[0],
            "Load Balancer Rule creation Failed"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_lbrules_after),
            "Load Balancer Rule creation Failed"
        )
        # Listing Load Balancer Rule Instances for applied as true
        list_lbruleinstance_applied_true =\
            LoadBalancerRule.listLoadBalancerRuleInstances(
                self.userapiclient,
                id=lb_rule.id,
                applied="true"
            )
        # Verifying No Instances are assigned to the Load Balancer Rule
        self.assertIsNone(
            list_lbruleinstance_applied_true,
            "Instances are assigned to Newly created Load Balancer Rule"
        )
        # Listing Load Balancer Rule Instances for applied as false
        list_lbruleinstance_applied_false =\
            LoadBalancerRule.listLoadBalancerRuleInstances(
                self.userapiclient,
                id=lb_rule.id,
                applied="false"
            )
        status = validateList(list_lbruleinstance_applied_false)
        self.assertEqual(
            PASS,
            status[0],
            "No Instances are available to assign to Load Balancer Rule"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_lbruleinstance_applied_false),
            "No Instances are available to assign to Load Balancer Rule"
        )
        # Verifying that Instance created above is listed
        self.assertEqual(
            vm_created.id,
            list_lbruleinstance_applied_false[0].id,
            "Failed to list Instance available to asign a Load Balancer Rule"
        )
        # Assigning Instance created to Load Balancer Rule
        LoadBalancerRule.assign(
            lb_rule,
            self.userapiclient,
            vms=[vm_created]
        )
        # Listing Load Balancer Rule Instances for applied as true
        list_lbruleinstance_applied_true =\
            LoadBalancerRule.listLoadBalancerRuleInstances(
                self.userapiclient,
                id=lb_rule.id,
                applied="true"
            )
        status = validateList(list_lbruleinstance_applied_false)
        self.assertEqual(
            PASS,
            status[0],
            "No Instances are available to assign to Load Balancer Rule"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_lbruleinstance_applied_false),
            "No Instances are available to assign to Load Balancer Rule"
        )
        # Verifying Instances is assigned to the Load Balancer Rule
        self.assertEqual(
            vm_created.id,
            list_lbruleinstance_applied_true[0].id,
            "Failed to assign Load Balancer Rule to given Instance"
        )
        # Listing Load Balancer Rule Instances for applied as false
        list_lbruleinstance_applied_false =\
            LoadBalancerRule.listLoadBalancerRuleInstances(
                self.userapiclient,
                id=lb_rule.id,
                applied="false"
            )
        # Verifying No Load Balancer Rules Instances are available to assign
        self.assertIsNone(
            list_lbruleinstance_applied_false,
            "Instances are available for assigning a Load Balancer Rule"
        )
        # Removing Load balancer Rule from Instance
        LoadBalancerRule.remove(
            lb_rule,
            self.userapiclient,
            vms=[vm_created]
        )
        # Listing Load Balancer Rule Instances for applied as true
        list_lbruleinstance_applied_true =\
            LoadBalancerRule.listLoadBalancerRuleInstances(
                self.userapiclient,
                id=lb_rule.id,
                applied="true"
            )
        # Verifying that there are no Instances assigned to the Load Balancer
        # Rule
        self.assertIsNone(
            list_lbruleinstance_applied_true,
            "Instances is assigned to Load balancer Rule"
        )
        # Listing Load Balancer Rule Instances for applied as false
        list_lbruleinstance_applied_false =\
            LoadBalancerRule.listLoadBalancerRuleInstances(
                self.userapiclient,
                id=lb_rule.id,
                applied="false"
            )
        status = validateList(list_lbruleinstance_applied_false)
        self.assertEqual(
            PASS,
            status[0],
            "No Instances are available to assign to Load Balancer Rule"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_lbruleinstance_applied_false),
            "No Instances are available to assign to Load Balancer Rule"
        )
        # Verifying that Instance created above is listed
        self.assertEqual(
            vm_created.id,
            list_lbruleinstance_applied_false[0].id,
            "Failed to list Instance available to asign a Load Balancer Rule"
        )
        # Destroying the VM Launched
        vm_created.delete(self.apiClient)
        self.cleanup.append(self.account)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_08_list_create_delete_lbsticky_policy(self):
        """
        @summary: Test to List, Create, Delete Load Balancer Stickyness Policy
        @Steps:
        Step1: Creating a Network for the user
        Step2: Associating an IP Addresses for Network
        Step3: Creating a Load Balancer Rule for IP Address associated
               in Step2
        Step4: Listing Load Balancer Sticky Policies for LB Rule created in
               Step3
        Step5: Verifying that no Load Balancer Sticky Policies are listed
        Step6: Creating a Load Balancer Sticky Policies for LB Rule created in
               Step3
        Step7: Listing Load Balancer Sticky Policies for LB Rule created in
               Step3
        Step8: Verifying 1 Load Balancer Sticky Policy is listed
        Step9: Deleting the Load Balancer Sticky Policies
        Step10: Listing Load Balancer Sticky Policies for LB Rule created in
                Step3
        Step11: Verifying that no Load Balancer Sticky Policies are listed
        """
        # Listing all the Networks's for a user
        list_networks_before = Network.list(
            self.userapiclient,
            listall=self.services["listall"],
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
            supportedservices="SourceNat,Lb",
            zoneid=self.zone.id
        )
        status = validateList(network_offerings_list)
        self.assertEqual(
            PASS,
            status[0],
            "Isolated Network Offerings with sourceNat,\
                    Lb enabled are not found"
        )
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
        # Listing Networks again
        list_networks_after = Network.list(
            self.userapiclient,
            listall=self.services["listall"],
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
        # Listing all the IP Addresses for a user
        list_ipaddresses_before = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        # Verifying no IP Addresses are listed
        self.assertIsNone(
            list_ipaddresses_before,
            "IP Addresses listed for newly created User"
        )
        # Associating an IP Addresses to Network created
        associated_ipaddress = PublicIPAddress.create(
            self.userapiclient,
            services=self.services["network"],
            networkid=network.id
        )
        self.assertIsNotNone(
            associated_ipaddress,
            "Failed to Associate IP Address"
        )
        # Listing all the IP Addresses for a user
        list_ipaddresses_after = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddresses_after)
        self.assertEqual(
            PASS,
            status[0],
            "IP Addresses Association Failed"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_ipaddresses_after),
            "Number of IP Addresses associated are not matching expected"
        )
        # Listing Load Balancer Rules for the Ip Address
        list_lbrules_before = LoadBalancerRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            publicipid=associated_ipaddress.ipaddress.id
        )
        # Verifying no Load Balancer Rules are listed
        self.assertIsNone(
            list_lbrules_before,
            "Load Balancer Rules listed for newly Acquired Ip Address"
        )
        self.services["lbrule"]["openfirewall"] = 'false'
        # Creating a Load Balancer Rule for Ip Address
        lb_rule = LoadBalancerRule.create(
            self.userapiclient,
            self.services["lbrule"],
            ipaddressid=associated_ipaddress.ipaddress.id,
        )
        self.assertIsNotNone(
            lb_rule,
            "Failed to create Load Balancer Rule"
        )
        # Listing Load Balancer Rules for the Ip Address
        list_lbrules_after = LoadBalancerRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            publicipid=associated_ipaddress.ipaddress.id
        )
        status = validateList(list_lbrules_after)
        self.assertEqual(
            PASS,
            status[0],
            "Load Balancer Rule creation Failed"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_lbrules_after),
            "Load Balancer Rule creation Failed"
        )
        # Listing Load Balancer Stickyness Policies for LB Rule
        list_lbstickypolicy_before = LoadBalancerRule.listStickyPolicies(
            self.userapiclient,
            lbruleid=lb_rule.id,
            listall=self.services["listall"]
        )
        # Verifying no Sticky Policies are listed
        self.assertEqual(
            0,
            len(list_lbstickypolicy_before[0].stickinesspolicy),
            "Sticky Policy listed for newly created Load Balancer Rule"
        )
        # Creating a Sticy Policy for Load Balancer Rule
        sticky_policy = LoadBalancerRule.createSticky(
            lb_rule,
            self.userapiclient,
            methodname='LbCookie',
            name='LbCookieSticky'
        )
        self.assertIsNotNone(
            sticky_policy,
            "Failed to create Sticky Policy for Load Balancer Rule"
        )
        # Verifying details of Sticky Policy created
        # Creating expected and actual values dictionaries
        expected_dict = {
            "account": self.account.name,
            "domainid": self.domain.id,
            "lbruleid": lb_rule.id,
            "methodname": "LbCookie",
            "name": "LbCookieSticky",
        }
        actual_dict = {
            "account": sticky_policy.account,
            "domainid": sticky_policy.domainid,
            "lbruleid": sticky_policy.lbruleid,
            "methodname": sticky_policy.stickinesspolicy[0].methodname,
            "name": sticky_policy.stickinesspolicy[0].name,
        }
        lbstickypolicy_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            lbstickypolicy_status,
            "Created Load Balancer Sticky Policy details are not as expected"
        )
        # Listing Load Balancer Stickyness Policies for LB Rule
        list_lbstickypolicy_after = LoadBalancerRule.listStickyPolicies(
            self.userapiclient,
            lbruleid=lb_rule.id,
            listall=self.services["listall"]
        )
        status = validateList(list_lbstickypolicy_after[0].stickinesspolicy)
        self.assertEqual(
            PASS,
            status[0],
            "Load Balancer Sticky Policy creation Failed"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_lbstickypolicy_after[0].stickinesspolicy),
            "Load Balancer Sticky Policy creation Failed"
        )
        # Deleting the Sticky Policy
        LoadBalancerRule.deleteSticky(
            lb_rule,
            self.userapiclient,
            id=sticky_policy.stickinesspolicy[0].id
        )
        # Listing Load Balancer Stickyness Policies for LB Rule
        list_lbstickypolicy_after = LoadBalancerRule.listStickyPolicies(
            self.userapiclient,
            lbruleid=lb_rule.id,
            listall=self.services["listall"]
        )
        # Verifying no Sticky Policies are listed
        self.assertEqual(
            0,
            len(list_lbstickypolicy_after[0].stickinesspolicy),
            "Sticky Policy listed for newly created Load Balancer Rule"
        )
        self.cleanup.append(self.account)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_09_create_delete_portforwarding_fornonvpc(self):
        """
        @summary: Test to list, create and delete Port Forwarding for
        IP Address associated to Non VPC network
        @Steps:
        Step1: Creating a Network for the user
        Step2: Associating an IP Addresses for Network
        Step3: Launching Virtual Machine in network created in step 2
        Step4: Listing Port Forwarding Rules for the IP Address associated
               in Step2
        Step5: Verifying that no Port Forwarding Rules are listed
        Step6: Creating a Port Forwarding Rule for IP Address associated in
               Step2
        Step7: Listing Port Forwarding Rules for the IP Address associated in
               Step2
        Step8: Verifying 1 Port Forwarding Rule is listed
        Step9: Deleting the Port Forwarding Rule created in Step6
        Step10: Listing Port Forwarding Rules for the IP Address associated in
               Step2
        Step11: Verifying that no Port Forwarding Rules are listed
        """
        # Listing all the Networks's for a user
        list_networks_before = Network.list(
            self.userapiclient,
            listall=self.services["listall"],
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
            supportedservices="SourceNat,PortForwarding",
            zoneid=self.zone.id
        )
        status = validateList(network_offerings_list)
        self.assertEqual(
            PASS,
            status[0],
            "Isolated Network Offerings with sourceNat,\
                    PortForwarding enabled are not found"
        )
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
        # Listing all the IP Addresses for a user
        list_ipaddresses_before = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        # Verifying no IP Addresses are listed
        self.assertIsNone(
            list_ipaddresses_before,
            "IP Addresses listed for newly created User"
        )
        # Associating an IP Addresses to Network created
        associated_ipaddress = PublicIPAddress.create(
            self.userapiclient,
            services=self.services["network"],
            networkid=network.id
        )
        self.assertIsNotNone(
            associated_ipaddress,
            "Failed to Associate IP Address"
        )
        # Listing all the IP Addresses for a user
        list_ipaddresses_after = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddresses_after)
        self.assertEqual(
            PASS,
            status[0],
            "IP Addresses Association Failed"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_ipaddresses_after),
            "Number of IP Addresses associated are not matching expected"
        )
        # Launching a Virtual Machine with above created Network
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=network.id,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "Failed to launch a VM under network created"
        )
        self.cleanup.append(network)
        # Listing Virtual Machines in running state in above created network
        list_vms_running = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            state="Running",
            networkid=network.id
        )
        status = validateList(list_vms_running)
        self.assertEqual(
            PASS,
            status[0],
            "VM Created is not in Running state"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_ipaddresses_after),
            "VM Created is not in Running state"
        )
        self.assertEqual(
            vm_created.id,
            list_vms_running[0].id,
            "VM Created is not in Running state"
        )
        # Listing Virtual Machines in stopped state in above created network
        list_vms_stopped = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            state="Stopped",
            networkid=network.id
        )
        # Verifying no VMs are in stopped state
        self.assertIsNone(
            list_vms_stopped,
            "VM Created is in stopped state"
        )
        # Listing Port Forwarding Rules for the IP Address associated
        list_prtfwdrule_before = NATRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            ipaddressid=associated_ipaddress.ipaddress.id
        )
        # Verifying no port forwarding rules are listed
        self.assertIsNone(
            list_prtfwdrule_before,
            "Port Forwarding Rules listed for newly associated IP Address"
        )
        # Creating a Port Forwarding rule
        portfwd_rule = NATRule.create(
            self.userapiclient,
            virtual_machine=vm_created,
            services=self.services["natrule"],
            ipaddressid=associated_ipaddress.ipaddress.id,
        )
        self.assertIsNotNone(
            portfwd_rule,
            "Failed to create Port Forwarding Rule"
        )
        # Verifying details of Sticky Policy created
        # Creating expected and actual values dictionaries
        expected_dict = {
            "ipaddressid": associated_ipaddress.ipaddress.id,
            "privateport": str(self.services["natrule"]["privateport"]),
            "publicport": str(self.services["natrule"]["publicport"]),
            "protocol": str(self.services["natrule"]["protocol"]).lower(),
        }
        actual_dict = {
            "ipaddressid": portfwd_rule.ipaddressid,
            "privateport": str(portfwd_rule.privateport),
            "publicport": str(portfwd_rule.publicport),
            "protocol": portfwd_rule.protocol,
        }
        portfwd_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            portfwd_status,
            "Created Port Forward Rule details are not as expected"
        )
        # Listing Port Forwarding Rules for the IP Address associated
        list_prtfwdrule_after = NATRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            ipaddressid=associated_ipaddress.ipaddress.id
        )
        status = validateList(list_prtfwdrule_after)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to create Port Forwarding Rule"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_prtfwdrule_after),
            "Failed to create Port Forwarding Rule"
        )
        # Deleting Port Forwarding Rule
        portfwd_rule.delete(self.userapiclient)
        # Listing Port Forwarding Rules for the IP Address associated
        list_prtfwdrule_after = NATRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            ipaddressid=associated_ipaddress.ipaddress.id
        )
        # Verifying no port forwarding rules are listed
        self.assertIsNone(
            list_prtfwdrule_after,
            "Port Forwarding Rules listed after deletion"
        )
        # Destroying the VM Launched
        vm_created.delete(self.apiClient)
        self.cleanup.append(self.account)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_10_create_delete_portforwarding_forvpc(self):
        """
        @summary: Test to list, create and delete Port Forwarding Rule
                  for IP Address associated to VPC
        @Steps:
        Step1: Creating a VPC for the user
        Step2: Creating Network inside VPC
        Step3: Associating an IP Addresses for VPC
        Step4: Launching a VM in the Network created in Step 2
        Step5: Listing Port Forwarding Rules for the IP Address
               associated in Step3
        Step6: Verifying that no Port Forwarding Rules are listed
        Step7: Creating a Port Forwarding Rule for IP Address associated
               in Step3
        Step8: Listing Port Forwarding Rules for the IP Address
               associated in Step3
        Step9: Verifying 1 Port Forwarding Rule is listed
        Step10: Deleting the Port Forwarding Rule created in Step7
        Step11: Listing Port Forwarding Rules for the IP Address
                associated in Step3
        Step12: Verifying that no Port Forwarding Rules are listed
        """
        if self.hypervisor.lower() in ['hyperv']:
            raise unittest.SkipTest("This feature is not supported on existing hypervisor. Hence, skipping the test")
        # Listing all the vpc's for a user
        list_vpc_before = VPC.list(self.userapiclient)
        # Verifying No VPCs are listed
        self.assertIsNone(
            list_vpc_before,
            "VPC's Listed for newly Created User"
        )
        # Creating a vpc
        vpc_created = VPC.create(
            self.userapiclient,
            self.services["vpc"],
            self.vpc_offering.id,
            self.zone.id
        )
        self.assertIsNotNone(
            vpc_created,
            "VPC Creation Failed"
        )
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
        # List network offering for vpc = true
        network_offering_vpc_true_list = NetworkOffering.list(
            self.userapiclient,
            forvpc="true",
            zoneid=self.zone.id,
            supportedServices="SourceNat,PortForwarding",
            state="Enabled"
        )
        status = validateList(network_offering_vpc_true_list)
        self.assertEqual(
            PASS,
            status[0],
            "Default network offering not present for\
                    vpc = true with PortForwarding")
        # Creating network under VPC
        network_created = Network.create(
            self.userapiclient,
            self.services["ntwk"],
            networkofferingid=network_offering_vpc_true_list[0].id,
            vpcid=vpc_created.id,
            zoneid=self.zone.id,
            gateway=self.services["ntwk"]["gateway"],
            netmask=self.services["ntwk"]["netmask"]
        )
        self.assertIsNotNone(
            network_created,
            "Network is not created"
        )
        # Listing all the IP Addresses for a user
        list_ipaddresses_before = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddresses_before)
        self.assertEqual(
            PASS,
            status[0],
            "list IP Addresses not as expected"
        )
        # Verifying the list vpc size is increased by 1
        self.assertEqual(
            1,
            len(list_ipaddresses_before),
            "list IP Addresses not equal as expected"
        )
        # Associating an IP Addresses to VPC created
        associated_ipaddress = PublicIPAddress.create(
            self.userapiclient,
            services=self.services["network"],
            vpcid=vpc_created.id
        )
        self.assertIsNotNone(
            associated_ipaddress,
            "Failed to Associate IP Address"
        )
        # Listing all the IP Addresses for a user
        list_ipaddresses_after = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddresses_after)
        self.assertEqual(
            PASS,
            status[0],
            "IP Addresses Association Failed"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            len(list_ipaddresses_before) + 1,
            len(list_ipaddresses_after),
            "Number of IP Addresses associated are not matching expected"
        )
        # Launching a Virtual Machine with above created Network
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=network_created.id,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "Failed to launch a VM under network created"
        )
        self.cleanup.append(network_created)
        self.cleanup.append(vpc_created)
        # Listing Port Forwarding Rules for the IP Address associated
        list_prtfwdrule_before = NATRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            ipaddressid=associated_ipaddress.ipaddress.id
        )
        # Verifying no port forwarding rules are listed
        self.assertIsNone(
            list_prtfwdrule_before,
            "Port Forwarding Rules listed for newly associated IP Address"
        )
        # Creating a Port Forwarding rule
        portfwd_rule = NATRule.create(
            self.userapiclient,
            virtual_machine=vm_created,
            services=self.services["natrule"],
            ipaddressid=associated_ipaddress.ipaddress.id,
            networkid=network_created.id
        )
        self.assertIsNotNone(
            portfwd_rule,
            "Failed to create Port Forwarding Rule"
        )
        # Verifying details of Sticky Policy created
        # Creating expected and actual values dictionaries
        expected_dict = {
            "ipaddressid": associated_ipaddress.ipaddress.id,
            "privateport": str(self.services["natrule"]["privateport"]),
            "publicport": str(self.services["natrule"]["publicport"]),
            "protocol": str(self.services["natrule"]["protocol"]).lower(),
        }
        actual_dict = {
            "ipaddressid": portfwd_rule.ipaddressid,
            "privateport": str(portfwd_rule.privateport),
            "publicport": str(portfwd_rule.publicport),
            "protocol": portfwd_rule.protocol,
        }
        portfwd_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            portfwd_status,
            "Created Port Forward Rule details are not as expected"
        )
        # Listing Port Forwarding Rules for the IP Address associated
        list_prtfwdrule_after = NATRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            ipaddressid=associated_ipaddress.ipaddress.id
        )
        status = validateList(list_prtfwdrule_after)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to create Port Forwarding Rule"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_prtfwdrule_after),
            "Failed to create Port Forwarding Rule"
        )
        # Deleting Port Forwarding Rule
        portfwd_rule.delete(self.userapiclient)
        # Listing Port Forwarding Rules for the IP Address associated
        list_prtfwdrule_after = NATRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            ipaddressid=associated_ipaddress.ipaddress.id
        )
        # Verifying no port forwarding rules are listed
        self.assertIsNone(
            list_prtfwdrule_after,
            "Port Forwarding Rules listed after deletion"
        )
        # Destroying the VM Launched
        vm_created.delete(self.apiClient)
        self.cleanup.append(self.account)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_11_create_delete_firewallrule(self):
        """
        @summary: Test to list, create and delete Firewall Rule for
                  IP Address associated to Non VPC network
        @Steps:
        Step1: Creating a Network for the user
        Step2: Associating an IP Addresses for Network
        Step3: Listing Firewall Rules for the IP Address associated in Step2
        Step4: Verifying that no Rules are listed
        Step5: Creating a Firewall Rule for IP Address associated in Step2
        Step6: Listing Firewall Rules for the IP Address associated in Step2
        Step7: Verifying 1 Firewall Rule is listed
        Step8: Deleting the Firewall Rule created in Step5
        Step9: Listing Firewall Rules for the IP Address associated in Step2
        Step10: Verifying that no Firewall Rules are listed
        """
        # Listing all the Networks's for a user
        list_networks_before = Network.list(
            self.userapiclient,
            listall=self.services["listall"],
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
            supportedservices="SourceNat,Firewall",
            zoneid=self.zone.id
        )
        status = validateList(network_offerings_list)
        self.assertEqual(
            PASS,
            status[0],
            "Isolated Network Offerings with sourceNat,\
                    Firewall enabled are not found"
        )
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
        # Listing all the IP Addresses for a user
        list_ipaddresses_before = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        # Verifying no IP Addresses are listed
        self.assertIsNone(
            list_ipaddresses_before,
            "IP Addresses listed for newly created User"
        )
        # Associating an IP Addresses to Network created
        associated_ipaddress = PublicIPAddress.create(
            self.userapiclient,
            services=self.services["network"],
            networkid=network.id
        )
        self.assertIsNotNone(
            associated_ipaddress,
            "Failed to Associate IP Address"
        )
        # Listing all the IP Addresses for a user
        list_ipaddresses_after = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddresses_after)
        self.assertEqual(
            PASS,
            status[0],
            "IP Addresses Association Failed"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_ipaddresses_after),
            "Number of IP Addresses associated are not matching expected"
        )
        # Listing Firewall rules for the IP Associated
        list_firewalls_before = FireWallRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            ipaddressid=associated_ipaddress.ipaddress.id
        )
        # Verifying no firewall rules are listed
        self.assertIsNone(
            list_firewalls_before,
            "Firewall Rules listed for newly associated IP Address"
        )
        # Creating a Firewall Rule
        firewall_rule = FireWallRule.create(
            self.userapiclient,
            ipaddressid=associated_ipaddress.ipaddress.id,
            protocol='tcp',
            cidrlist='10.1.1.1/16',
            startport='22',
            endport='2222'
        )
        self.assertIsNotNone(
            firewall_rule,
            "Failed to create Firewall Rule"
        )
        # Verifying details of the created Firewall Rule
        # Creating expected and actual values dictionaries
        expected_dict = {
            "ipaddressid": associated_ipaddress.ipaddress.id,
            "startport": 22,
                         "endport": 2222,
                         "protocol": "tcp",
                         "cidrlist": "10.1.1.1/16"
        }
        actual_dict = {
            "ipaddressid": firewall_rule.ipaddressid,
            "startport": firewall_rule.startport,
            "endport": firewall_rule.endport,
            "protocol": firewall_rule.protocol,
            "cidrlist": firewall_rule.cidrlist
        }
        firewall_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            firewall_status,
            "Created Firewall Rule details are not as expected"
        )
        # Listing Firewall rules for the IP Associated
        list_firewalls_after = FireWallRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            ipaddressid=associated_ipaddress.ipaddress.id
        )
        status = validateList(list_firewalls_after)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to create Firewall Rule"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_firewalls_after),
            "Failed to create Firewall Rule"
        )
        # Deleting the Firewall Rule
        firewall_rule.delete(self.userapiclient)
        # Listing Firewall rules for the IP Associated
        list_firewalls_after = FireWallRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            ipaddressid=associated_ipaddress.ipaddress.id
        )
        # Verifying no firewall rules are listed
        self.assertIsNone(
            list_firewalls_after,
            "Failed to create Firewall Rule"
        )
        self.cleanup.append(self.account)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_12_create_delete_remoteaccessvpn(self):
        """
        @summary: Test to list, create and delete Remote Access VPNs
        @Steps:
        Step1: Creating a Network for the user
        Step2: Associating an IP Addresses for Network
        Step3: Listing Remore Access VPNs for the IP Address associated
               in Step2
        Step4: Verifying that no Remore Access VPNs are listed
        Step5: Creating a Remore Access VPN for IP Address associated
               in Step2
        Step6: Listing Remore Access VPNs for the IP Address associated in
               Step2
        Step7: Verifying 1 Remore Access VPN is listed
        Step8: Deleting the Remore Access VPNs created in Step5
        Step9: Listing Remore Access VPNs for the IP Address associated i
               Step2
        Step10: Verifying that no Remore Access VPNs are listed
        """
        # Listing all the Networks's for a user
        list_networks_before = Network.list(
            self.userapiclient,
            listall=self.services["listall"],
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
            supportedservices="SourceNat,Vpn",
            zoneid=self.zone.id
        )
        status = validateList(network_offerings_list)
        self.assertEqual(
            PASS,
            status[0],
            "Isolated Network Offerings with sourceNat,\
                    Vpn enabled are not found"
        )
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
        # Listing all the IP Addresses for a user
        list_ipaddresses_before = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        # Verifying no IP Addresses are listed
        self.assertIsNone(
            list_ipaddresses_before,
            "IP Addresses listed for newly created User"
        )
        # Associating an IP Addresses to Network created
        associated_ipaddress = PublicIPAddress.create(
            self.userapiclient,
            services=self.services["network"],
            networkid=network.id
        )
        self.assertIsNotNone(
            associated_ipaddress,
            "Failed to Associate IP Address"
        )
        # Listing all the IP Addresses for a user
        list_ipaddresses_after = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddresses_after)
        self.assertEqual(
            PASS,
            status[0],
            "IP Addresses Association Failed"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_ipaddresses_after),
            "Number of IP Addresses associated are not matching expected"
        )
        # Listing Remote access VPNs for the IP Associated
        list_vpns_before = Vpn.list(
            self.userapiclient,
            listall=self.services["listall"],
            ipaddressid=associated_ipaddress.ipaddress.id
        )
        # Verifying no Remote Access VPNs are listed
        self.assertIsNone(
            list_vpns_before,
            "Remote Access VPNs listed for newly associated IP Address"
        )
        # Creating a Remote Access VPN
        vpn_created = Vpn.create(
            self.userapiclient,
            publicipid=associated_ipaddress.ipaddress.id,
            account=self.account.name,
            domainid=self.domain.id,
            openfirewall='false'
        )
        self.assertIsNotNone(
            vpn_created,
            "Failed to create Remote Access VPN"
        )
        # Verifying details of the created Remote Access VPN
        # Creating expected and actual values dictionaries
        expected_dict = {
            "ipaddressid": associated_ipaddress.ipaddress.id,
            "account": self.account.name,
            "domainid": self.domain.id,
            "state": "Running",
        }
        actual_dict = {
            "ipaddressid": vpn_created.publicipid,
            "account": vpn_created.account,
            "domainid": vpn_created.domainid,
            "state": vpn_created.state,
        }
        vpn_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            vpn_status,
            "Created Remote Access VPN details are not as expected"
        )
        # Listing Remote Access VPNs for the IP Associated
        list_vpns_after = Vpn.list(
            self.userapiclient,
            listall=self.services["listall"],
            ipaddressid=associated_ipaddress.ipaddress.id
        )
        status = validateList(list_vpns_after)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to create Remote Access VPN"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_vpns_after),
            "Failed to create Remote Access VPN"
        )
        # Deleting the Remote Access VPN
        vpn_created.delete(self.userapiclient)
        # Listing Remote Access VPNs for the IP Associated
        list_vpns_after = Vpn.list(
            self.userapiclient,
            listall=self.services["listall"],
            ipaddressid=associated_ipaddress.ipaddress.id
        )
        # Verifying no Remote Access VPNs are listed
        self.assertIsNone(
            list_vpns_after,
            "Failed to create Remote Access VPN"
        )
        self.cleanup.append(self.account)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_13_add_remove_vpnusers(self):
        """
        @summary: Test to list, add and remove VPN Users
        @Steps:
        Step1: Creating a Network for the user
        Step2: Associating an IP Addresses for Network
        Step3: Launching a VM under network created in Step1
        Step4: Creating Remote Access VPN
        Step5: Listing VPN Users
        Step6: Verifying that no VPN Users are listed
        Step7: Adding a VPN user
        Step8: Listing VPN Users
        Step9: Verifying 1 VPN is listed
        Step10: Deleting VPN user
        Step11: Listing VPN Users
        Step12: Verifying that no VPN Users are listed
        """
        # Listing all the Networks's for a user
        list_networks_before = Network.list(
            self.userapiclient,
            listall=self.services["listall"],
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
            supportedservices="SourceNat,Vpn",
            zoneid=self.zone.id
        )
        status = validateList(network_offerings_list)
        self.assertEqual(
            PASS,
            status[0],
            "Isolated Network Offerings with sourceNat,\
                    Vpn enabled are not found"
        )
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
        # Listing all the IP Addresses for a user
        list_ipaddresses_before = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        # Verifying no IP Addresses are listed
        self.assertIsNone(
            list_ipaddresses_before,
            "IP Addresses listed for newly created User"
        )
        # Associating an IP Addresses to Network created
        associated_ipaddress = PublicIPAddress.create(
            self.userapiclient,
            services=self.services["network"],
            networkid=network.id
        )
        self.assertIsNotNone(
            associated_ipaddress,
            "Failed to Associate IP Address"
        )
        # Listing all the IP Addresses for a user
        list_ipaddresses_after = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddresses_after)
        self.assertEqual(
            PASS,
            status[0],
            "IP Addresses Association Failed"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_ipaddresses_after),
            "Number of IP Addresses associated are not matching expected"
        )
        # Launching Virtual Machine
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=network.id,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "Failed to launch a VM under network created"
        )
        # Listing Remote access VPNs for the IP Associated
        list_vpns_before = Vpn.list(
            self.userapiclient,
            listall=self.services["listall"],
            ipaddressid=associated_ipaddress.ipaddress.id
        )
        # Verifying no Remote Access VPNs are listed
        self.assertIsNone(
            list_vpns_before,
            "Remote Access VPNs listed for newly associated IP Address"
        )
        # Creating a Remote Access VPN
        vpn_created = Vpn.create(
            self.userapiclient,
            publicipid=associated_ipaddress.ipaddress.id,
            account=self.account.name,
            domainid=self.domain.id,
            openfirewall='false'
        )
        self.assertIsNotNone(
            vpn_created,
            "Failed to create Remote Access VPN"
        )
        # Listing Remote Access VPNs for the IP Associated
        list_vpns_after = Vpn.list(
            self.userapiclient,
            listall=self.services["listall"],
            ipaddressid=associated_ipaddress.ipaddress.id
        )
        status = validateList(list_vpns_after)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to create Remote Access VPN"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_vpns_after),
            "Failed to create Remote Access VPN"
        )
        # Listing VPN Users
        list_vpnusers_beore = VpnUser.list(
            self.userapiclient,
            listall=self.services["listall"],
            account=self.account.name,
            domainid=self.domain.id
        )
        # Verifying no VPN Users listed
        self.assertIsNone(
            list_vpnusers_beore,
            "VPN Users listed for newly created VPN"
        )
        # Creating a VPN User
        vpnuser_created = VpnUser.create(
            self.userapiclient,
            username=self.services["vpn_user"]["username"],
            password=self.services["vpn_user"]["password"],
            account=self.account.name,
            domainid=self.domain.id
        )
        self.assertIsNotNone(
            vpnuser_created,
            "Failed to create VPN User"
        )
        # Listing VPN Users
        list_vpnusers_after = VpnUser.list(
            self.userapiclient,
            listall=self.services["listall"],
            account=self.account.name,
            domainid=self.domain.id
        )
        status = validateList(list_vpnusers_after)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to list VPN user after creation"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_vpnusers_after),
            "Failed to list VPN user after creation"
        )
        # Deleting the VPN User
        vpnuser_created.delete(self.userapiclient)
        # Listing VPN Users
        list_vpnusers_after = VpnUser.list(
            self.userapiclient,
            listall=self.services["listall"],
            account=self.account.name,
            domainid=self.domain.id
        )
        # Verifying no VPN Users are listed
        self.assertIsNone(
            list_vpnusers_after,
            "VPN User listed after deletion"
        )
        # Destroying the VM
        vm_created.delete(self.apiClient)
        self.cleanup.append(self.account)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_14_enable_disable_staticnat_fornonvpc(self):
        """
        @summary: Test to Enable and Disable StaticNat for
                  IP Address associated to Non VPC Network
        @Steps:
        Step1: Creating a Network for the user
        Step2: Associating an IP Addresses for Network created in step1
        Step3: Associating one more Ip Address to Network created in step1
        Step4: Launching the VM in network created in step1
        Step5: Enabling the staticNat to IP Associated in Step3
        Step6: Verifying that StaticNat is enabled
        Step7: Disabling the staticNat to IP Associated in Step3
        Step8: Verifying that StaticNat is disabled
        """
        # Listing all the Networks's for a user
        list_networks_before = Network.list(
            self.userapiclient,
            listall=self.services["listall"],
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
            supportedservices="SourceNat,StaticNat",
            zoneid=self.zone.id
        )
        status = validateList(network_offerings_list)
        self.assertEqual(
            PASS,
            status[0],
            "Isolated Network Offerings with sourceNat,\
                    Vpn enabled are not found"
        )
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
        # Listing all the IP Addresses for a user
        list_ipaddresses_before = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        # Verifying no IP Addresses are listed
        self.assertIsNone(
            list_ipaddresses_before,
            "IP Addresses listed for newly created User"
        )
        # Associating an IP Addresses to Network created
        associated_ipaddress1 = PublicIPAddress.create(
            self.userapiclient,
            services=self.services["network"],
            networkid=network.id
        )
        self.assertIsNotNone(
            associated_ipaddress1,
            "Failed to Associate IP Address"
        )
        # Associating another IP Addresses to Network created
        associated_ipaddress2 = PublicIPAddress.create(
            self.userapiclient,
            services=self.services["network"],
            networkid=network.id
        )
        self.assertIsNotNone(
            associated_ipaddress2,
            "Failed to Associate IP Address"
        )
        # Listing all the IP Addresses for a user
        list_ipaddresses_after = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddresses_after)
        self.assertEqual(
            PASS,
            status[0],
            "IP Addresses Association Failed"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            2,
            len(list_ipaddresses_after),
            "Number of IP Addresses associated are not matching expected"
        )
        # Launching a Virtual Machine
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=network.id,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "Failed to launch a VM under network created"
        )
        # Enabling static Nat for Ip Address associated
        StaticNATRule.enable(
            self.userapiclient,
            ipaddressid=associated_ipaddress2.ipaddress.id,
            virtualmachineid=vm_created.id,
        )
        # Listing Ip Address by id
        list_ipaddress = PublicIPAddress.list(
            self.userapiclient,
            id=associated_ipaddress2.ipaddress.id,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddress)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to List IP Address"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_ipaddress),
            "Failed to List IP Address"
        )
        self.assertEqual(
            True,
            list_ipaddress[0].isstaticnat,
            "Failed to Enable Static Nat"
        )
        # Disabling static Nat
        StaticNATRule.disable(
            self.userapiclient,
            ipaddressid=associated_ipaddress2.ipaddress.id
        )
        # Listing Ip Address by id
        list_ipaddress = PublicIPAddress.list(
            self.userapiclient,
            id=associated_ipaddress2.ipaddress.id,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddress)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to List IP Address"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_ipaddress),
            "Failed to List IP Address"
        )
        self.assertEqual(
            False,
            list_ipaddress[0].isstaticnat,
            "Failed to Disable Static Nat"
        )
        # Destroying the VM
        vm_created.delete(self.apiClient)
        self.cleanup.append(self.account)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_15_enable_disable_staticnat_forvpc(self):
        """
        @summary: Test to Enable and Disable StaticNat for
                  IP Address associated to VPC Network
        @Steps:
        Step1: Creating a VPC
        Step2: Creating a Network under VPC for the user
        Step3: Associating an IP Addresses for Network created in step1
        Step4: Launching the VM in network created in step2
        Step5: Enabling the staticNat to IP Associated in Step3
        Step6: Verifying that StaticNat is enabled
        Step7: Disabling the staticNat to IP Associated in Step3
        Step8: Verifying that StaticNat is disabled
        """
        if self.hypervisor.lower() in ['hyperv']:
            self.skipTest("This feature is not supported on existing hypervisor. Hence, skipping the test")
        # Listing all the vpc's for a user
        list_vpc_before = VPC.list(self.userapiclient)
        # Verifying No VPCs are listed
        self.assertIsNone(
            list_vpc_before,
            "VPC's Listed for newly Created User"
        )
        # Creating a vpc
        vpc_created = VPC.create(
            self.userapiclient,
            self.services["vpc"],
            self.vpc_offering.id,
            self.zone.id
        )
        self.assertIsNotNone(
            vpc_created,
            "VPC Creation Failed"
        )
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
        # List network offering for vpc = true
        network_offering_vpc_true_list = NetworkOffering.list(
            self.userapiclient,
            forvpc="true",
            zoneid=self.zone.id,
            supportedServices="SourceNat,PortForwarding,StaticNat",
            state="Enabled"
        )
        status = validateList(network_offering_vpc_true_list)
        self.assertEqual(
            PASS,
            status[0],
            "Default network offering not present\
                    for vpc = true with PortForwarding")
        # Creating network under VPC
        network_created = Network.create(
            self.userapiclient,
            self.services["ntwk"],
            networkofferingid=network_offering_vpc_true_list[0].id,
            vpcid=vpc_created.id,
            zoneid=self.zone.id,
            gateway=self.services["ntwk"]["gateway"],
            netmask=self.services["ntwk"]["netmask"]
        )
        self.assertIsNotNone(
            network_created,
            "Network is not created"
        )
        # Listing all the IP Addresses for a user
        list_ipaddresses_before = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddresses_before)
        self.assertEqual(
            PASS,
            status[0],
            "list IP Addresses not as expected"
        )
        # Verifying the list vpc size is increased by 1
        self.assertEqual(
            1,
            len(list_ipaddresses_before),
            "list IP Addresses not equal as expected"
        )
        # Associating an IP Addresses to VPC created
        associated_ipaddress = PublicIPAddress.create(
            self.userapiclient,
            services=self.services["network"],
            vpcid=vpc_created.id
        )
        self.assertIsNotNone(
            associated_ipaddress,
            "Failed to Associate IP Address"
        )
        # Listing all the IP Addresses for a user
        list_ipaddresses_after = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddresses_after)
        self.assertEqual(
            PASS,
            status[0],
            "IP Addresses Association Failed"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            len(list_ipaddresses_before) + 1,
            len(list_ipaddresses_after),
            "Number of IP Addresses associated are not matching expected"
        )
        # Launching a Virtual Machine with above created Network
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=network_created.id,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "Failed to launch a VM under network created"
        )
        self.cleanup.append(network_created)
        self.cleanup.append(vpc_created)
        # Enabling static Nat for Ip Address associated
        StaticNATRule.enable(
            self.userapiclient,
            ipaddressid=associated_ipaddress.ipaddress.id,
            virtualmachineid=vm_created.id,
            networkid=network_created.id
        )
        # Listing Ip Address by id
        list_ipaddress = PublicIPAddress.list(
            self.userapiclient,
            id=associated_ipaddress.ipaddress.id,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddress)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to List IP Address"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_ipaddress),
            "Failed to List IP Address"
        )
        self.assertEqual(
            True,
            list_ipaddress[0].isstaticnat,
            "Failed to Enable Static Nat"
        )
        # Disabling static Nat
        StaticNATRule.disable(
            self.userapiclient,
            ipaddressid=associated_ipaddress.ipaddress.id
        )
        # Listing Ip Address by id
        list_ipaddress = PublicIPAddress.list(
            self.userapiclient,
            id=associated_ipaddress.ipaddress.id,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddress)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to List IP Address"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_ipaddress),
            "Failed to List IP Address"
        )
        self.assertEqual(
            False,
            list_ipaddress[0].isstaticnat,
            "Failed to Disable Static Nat"
        )
        # Destroying the VM
        vm_created.delete(self.apiClient)
        self.cleanup.append(self.account)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_16_create_delete_ipforwardingrule(self):
        """
        @summary: Test to list, create and
                  delete IP Forwarding Rules for IP Address
        @Steps:
        Step1: Creating a Network for the user
        Step2: Associating an IP Addresses for Network created in step1
        Step3: Associating one more Ip Address to Network created in step1
        Step4: Launching the VM in network created in step1
        Step5: Enabling the staticNat to IP Associated in Step3
        Step6: Listing IP Forwarding rules
        Step7: Verifying no IP Forwarding rules are listed
        Step8: Creating a IP Forwarding Rule
        Step9: Listing IP Forwarding rules
        Step10: Verifying 1 IP Forwarding rule is listed
        Step11: Deleting the IP Forwarding rule
        Step12: Listing IP Forwarding rules
        Step13: Verifying no IP Forwarding rules are listed
        """
        # Listing all the Networks's for a user
        list_networks_before = Network.list(
            self.userapiclient,
            listall=self.services["listall"],
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
            supportedservices="SourceNat,Vpn",
            zoneid=self.zone.id
        )
        status = validateList(network_offerings_list)
        self.assertEqual(
            PASS,
            status[0],
            "Isolated Network Offerings with sourceNat,\
                    Vpn enabled are not found"
        )
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
        # Listing all the IP Addresses for a user
        list_ipaddresses_before = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        # Verifying no IP Addresses are listed
        self.assertIsNone(
            list_ipaddresses_before,
            "IP Addresses listed for newly created User"
        )
        # Associating an IP Addresses to Network created
        associated_ipaddress1 = PublicIPAddress.create(
            self.userapiclient,
            services=self.services["network"],
            networkid=network.id
        )
        self.assertIsNotNone(
            associated_ipaddress1,
            "Failed to Associate IP Address"
        )
        # Associating another IP Addresses to Network created
        associated_ipaddress2 = PublicIPAddress.create(
            self.userapiclient,
            services=self.services["network"],
            networkid=network.id
        )
        self.assertIsNotNone(
            associated_ipaddress2,
            "Failed to Associate IP Address"
        )
        # Listing all the IP Addresses for a user
        list_ipaddresses_after = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddresses_after)
        self.assertEqual(
            PASS,
            status[0],
            "IP Addresses Association Failed"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            2,
            len(list_ipaddresses_after),
            "Number of IP Addresses associated are not matching expected"
        )
        # Launching a Virtual Machine
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=network.id,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "Failed to launch a VM under network created"
        )
        # Enabling static Nat for Ip Address associated
        StaticNATRule.enable(
            self.userapiclient,
            ipaddressid=associated_ipaddress2.ipaddress.id,
            virtualmachineid=vm_created.id,
        )
        # Listing Ip Address by id
        list_ipaddress = PublicIPAddress.list(
            self.userapiclient,
            id=associated_ipaddress2.ipaddress.id,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddress)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to List IP Address"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_ipaddress),
            "Failed to List IP Address"
        )
        self.assertEqual(
            True,
            list_ipaddress[0].isstaticnat,
            "Failed to Enable Static Nat"
        )
        # Listing IP Forwarding Rules
        list_ipfwdrule_before = StaticNATRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            ipaddressid=associated_ipaddress2.ipaddress.id
        )
        # Verifying no IP Forwarding Rules are listed
        self.assertIsNone(
            list_ipfwdrule_before,
            "IP Forwardign Rule listed for newly enabled StaticNat IP"
        )
        # Creating IP Forwardign Rule
        ipfwd_rule = StaticNATRule.createIpForwardingRule(
            self.userapiclient,
            startport='80',
            endport='89',
            protocol='tcp',
            ipaddressid=associated_ipaddress2.ipaddress.id,
            openfirewall=False
        )
        self.assertIsNotNone(
            ipfwd_rule,
            "Failed to create IP Forwarding Rule"
        )
        # Listing IP Forwarding Rules
        list_ipfwdrule_after = StaticNATRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            ipaddressid=associated_ipaddress2.ipaddress.id
        )
        status = validateList(list_ipfwdrule_after)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to List IP Forwarding Rule after Creation"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_ipfwdrule_after),
            "Failed to List IP Forwarding Rule after Creation"
        )
        # Deleting IP Forwarding Rule
        ipfwd_rule.delete(self.userapiclient)
        # Listing IP Forwarding Rules
        list_ipfwdrule_after = StaticNATRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            ipaddressid=associated_ipaddress2.ipaddress.id
        )
        # Verifying no IP Forwarding Rules are listed
        self.assertIsNone(
            list_ipfwdrule_after,
            "IP Forwardign Rule listed after deletion"
        )
        # Destroying the VM
        vm_created.delete(self.apiClient)
        self.cleanup.append(self.account)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_17_create_update_autoscalepolicy(self):
        """
        @summary: Test to list, create and update Autoscale Policy
        @Steps:
        Step1: Creating a Network Offering for Supported
               Service LB with Netscaler
        Step2: Create a Network using Network Offering created in Step1
        Step3: Launching a VM using Network created in Step2
        Step4: Associating IP Address for Network created in Step2
        Step5: Listing Counters
        Step6: Listing Conditions for the user
        Step7: Verifying no Conditions are listed
        Step8: Creating a 2 conditions
        Step9: Listing conditions again
        Step10: Verifying 2 conditions are listed
        Step11: Listing Autoscale Policies for User
        Step12: Verifying No Autoscale policy is listed
        Step13: Creating Autoscale Policy using Condition1
        Step14: Verifying that Autoscale Policy is created with Condition1
        Step15: Listing Autoscale Policies
        Step16: Verifying 1 Autoscale Policy is listed
        Step17: Updating Autoscale Policy created in step13 with condition2
        Step18: Verifying Autoscale policy is updated with condition2
        """
        if self.hypervisor.lower() == 'kvm':
            self.skipTest(
                "ScaleVM is not supported on KVM. Hence, skipping the test")

        list_physical_networks = PhysicalNetwork.list(
            self.apiClient,
            zoneid=self.zone.id
        )
        run_flag = False
        for i in range(0, len(list_physical_networks)):
            list_network_serviceprovider = NetworkServiceProvider.list(
                self.apiClient,
                physicalnetworkid=list_physical_networks[i].id
            )
            for j in range(0, len(list_network_serviceprovider)):
                if((list_network_serviceprovider[j].name == 'Netscaler') and
                        (list_network_serviceprovider[j].state == 'Enabled')):
                    run_flag = True
                    break

        if not run_flag:
            self.debug(
                "Netscaler is not enabled and auto scale VM is\
                        applicable only for Netscaler")
        else:
            # Listing Network Offerings
            list_nwoff_before = NetworkOffering.list(
                self.apiClient,
                forvpc="false",
                guestiptype="Isolated",
                state="Enabled",
                supportedservices="SourceNat,Lb",
                zoneid=self.zone.id
            )
            # Creating Network Offerign with LB as Netscalar
            nwoff_created = NetworkOffering.create(
                self.apiClient,
                self.services["nw_off_isolated_netscaler"]
            )
            self.assertIsNotNone(
                nwoff_created,
                "Failed to Create Network Offering with\
                        LB sercvice for Netscaler")
            # Enable Network offering
            nwoff_created.update(self.apiClient, state='Enabled')
            # Listing Network Offerings again
            list_nwoff_after = NetworkOffering.list(
                self.apiClient,
                forvpc="false",
                guestiptype="Isolated",
                state="Enabled",
                supportedservices="SourceNat,Lb",
                zoneid=self.zone.id
            )
            self.assertEqual(
                len(list_nwoff_before) + 1,
                len(list_nwoff_after),
                "Failed to create Network Offering"
            )
            # Creating a Network Using the Network Offering
            network = Network.create(
                self.userapiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.domain.id,
                networkofferingid=nwoff_created.id,
                zoneid=self.zone.id
            )
            self.assertIsNotNone(
                network,
                "Network creation failed"
            )
            self.cleanup.append(network)
            self.cleanup.append(nwoff_created)
            # Launching a Virtual Machine
            vm_created = VirtualMachine.create(
                self.userapiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkids=network.id,
                serviceofferingid=self.service_offering.id,
            )
            self.assertIsNotNone(
                vm_created,
                "Failed to launch a VM under network created"
            )
            # Listing all the IP Addresses for a user
            list_ipaddresses_before = PublicIPAddress.list(
                self.userapiclient,
                listall=self.services["listall"]
            )
            status = validateList(list_ipaddresses_before)
            self.assertEqual(
                PASS,
                status[0],
                "IP Addresses Association Failed while launching a VM"
            )
            # Verifying the length of the list is 1
            self.assertEqual(
                1,
                len(list_ipaddresses_before),
                "IP Addresses Association Failed while launching a VM"
            )
            # Associating an IP Addresses to Network created
            associated_ipaddress = PublicIPAddress.create(
                self.userapiclient,
                services=self.services["network"],
                networkid=network.id
            )
            self.assertIsNotNone(
                associated_ipaddress,
                "Failed to Associate IP Address"
            )
            # Listing all the IP Addresses for a user
            list_ipaddresses_after = PublicIPAddress.list(
                self.userapiclient,
                listall=self.services["listall"]
            )
            status = validateList(list_ipaddresses_after)
            self.assertEqual(
                PASS,
                status[0],
                "IP Addresses Association Failed"
            )
            # Verifying the length of the list is 1
            self.assertEqual(
                2,
                len(list_ipaddresses_after),
                "Number of IP Addresses associated are not matching expected"
            )
            # Listing Counters
            list_counters = Autoscale.listCounters(
                self.userapiclient,
            )
            status = validateList(list_counters)
            self.assertEqual(
                PASS,
                status[0],
                "Failed to list counters"
            )
            #  Listing Conditions
            list_conditions_before = Autoscale.listConditions(
                self.userapiclient,
                listall=self.services["listall"],
                account=self.account.name,
                domainid=self.domain.id
            )
            self.assertIsNone(
                list_conditions_before,
                "Listed Conditions for newly created user"
            )
            # Creating first Condition
            condition_created1 = Autoscale.createCondition(
                self.userapiclient,
                counterid=list_counters[0].id,
                relationaloperator='GT',
                threshold='1000'
            )
            self.assertIsNotNone(
                condition_created1,
                "Failed to create Condition"
            )
            # Creating second Condition
            condition_created2 = Autoscale.createCondition(
                self.userapiclient,
                counterid=list_counters[0].id,
                relationaloperator='GT',
                threshold='1500'
            )
            self.assertIsNotNone(
                condition_created2,
                "Failed to create Condition"
            )
            # Listing Conditions again
            list_conditions_after = Autoscale.listConditions(
                self.userapiclient,
                listall=self.services["listall"],
                account=self.account.name,
                domainid=self.domain.id
            )
            status = validateList(list_conditions_after)
            self.assertEqual(
                PASS,
                status[0],
                "Failed to list Conditions after creation"
            )
            # Listing Autoscale policies
            list_autoscalepolicies_before = Autoscale.listAutoscalePolicies(
                self.userapiclient,
                listall=self.services["listall"]
            )
            # Verifying no Autoscale policies are listed
            self.assertIsNone(
                list_autoscalepolicies_before,
                "Autoscale policies listed"
            )
            # Creating Autoscale Policy
            autoscalepolicy_created = Autoscale.createAutoscalePolicy(
                self.userapiclient,
                action='scaleup',
                conditionids=condition_created1.id,
                duration='100',
                quiettime='100'
            )
            self.assertIsNotNone(
                autoscalepolicy_created,
                "Failed to create Autoscale VM Policy"
            )
            # Verifying autoscalepolicy is created using condition1
            self.assertEqual(
                condition_created1.id,
                autoscalepolicy_created.conditions[0].id,
                "Autoscale Policy not created by given condition"
            )
            # Listing Autoscale policies
            list_autoscalepolicies_after = Autoscale.listAutoscalePolicies(
                self.userapiclient,
                listall=self.services["listall"]
            )
            status = validateList(list_autoscalepolicies_after)
            self.assertEqual(
                PASS,
                status[0],
                "Failed to list Autoscale Policy after creation"
            )
            self.assertEqual(
                1,
                len(list_autoscalepolicies_after),
                "Autoscale Policies count is not matching"
            )
            # Updating Autoscale Policy
            autoscalepolicy_updated = Autoscale.updateAutoscalePolicy(
                self.userapiclient,
                id=autoscalepolicy_created.id,
                conditionids=condition_created2.id,
                duration='100',
                quiettime='100'
            )
            self.assertIsNotNone(
                autoscalepolicy_updated,
                "Failed to update Autoscale Policy"
            )
            # Verifying the Autoscale Policy is updated
            self.assertEqual(
                condition_created2.id,
                autoscalepolicy_updated.conditions[0].id,
                "Autoscale Policy not updated to given condition"
            )
            # Destroying the VM
            vm_created.delete(self.apiClient)
        self.cleanup.append(self.account)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_18_create_update_autoscaleprofiles(self):
        """
        @summary: Test to list, create and update Autoscale VM Profiles
        @Steps:
        Step1: Creating a Network Offering for Supported Service LB with
               Netscaler
        Step2: Create a Network using Network Offering created in Step1
        Step3: Launching a VM using Network created in Step2
        Step4: Associating IP Address for Network created in Step2
        Step5: Listing Available Service Offerings
        Step6: Listing all types of templates
        Step7: Verifying only featured templates are listed for
               newly created user
        Step8: Listing autoscale vm profiles
        Step9: Verifying no Autoscale VM Profiles are listed
        Step10: Creating a Autoscale VM Profile
        Step11: Listing Autoscale VM Profile
        Step12: Verifying 1 Autoscale VM Profile is listed
        Step13: Listing Autoscale VM Profile by id
        Step14: Verifying details of the created autoscale
                vm profile are matching with listed autoscal vm profile
        Step15: Updating Autoscale VM profile with destroy vm grace period
        Step16: Verifying that Autoscale VM is updated
        """
        if self.hypervisor.lower() == 'kvm':
            self.skipTest(
                "ScaleVM is not supported on KVM. Hence, skipping the test")

        list_physical_networks = PhysicalNetwork.list(
            self.apiClient,
            zoneid=self.zone.id
        )

        run_flag = False
        for i in range(0, len(list_physical_networks)):
            list_network_serviceprovider = NetworkServiceProvider.list(
                self.apiClient,
                physicalnetworkid=list_physical_networks[i].id
            )
            for j in range(0, len(list_network_serviceprovider)):
                if((list_network_serviceprovider[j].name == 'Netscaler') and
                        (list_network_serviceprovider[j].state == 'Enabled')):
                    run_flag = True
                    break

        if not run_flag:
            self.debug(
                "Netscaler is not enabled and auto scale VM\
                        is applicable only for Netscaler")
        else:
            # Listing Network Offerings
            list_nwoff_before = NetworkOffering.list(
                self.apiClient,
                forvpc="false",
                guestiptype="Isolated",
                state="Enabled",
                supportedservices="SourceNat,Lb",
                zoneid=self.zone.id
            )
            # Creating Network Offerign with LB as Netscalar
            nwoff_created = NetworkOffering.create(
                self.apiClient,
                self.services["nw_off_isolated_netscaler"]
            )
            self.assertIsNotNone(
                nwoff_created,
                "Failed to Create Network Offering with\
                        LB sercvice for Netscaler")
            # Enable Network offering
            nwoff_created.update(self.apiClient, state='Enabled')
            # Listing Network Offerings again
            list_nwoff_after = NetworkOffering.list(
                self.apiClient,
                forvpc="false",
                guestiptype="Isolated",
                state="Enabled",
                supportedservices="SourceNat,Lb",
                zoneid=self.zone.id
            )
            self.assertEqual(
                len(list_nwoff_before) + 1,
                len(list_nwoff_after),
                "Failed to create Network Offering"
            )
            # Creating a Network Using the Network Offering
            network = Network.create(
                self.userapiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.domain.id,
                networkofferingid=nwoff_created.id,
                zoneid=self.zone.id
            )
            self.assertIsNotNone(
                network,
                "Network creation failed"
            )
            self.cleanup.append(network)
            self.cleanup.append(nwoff_created)
            # Launching a Virtual Machine
            vm_created = VirtualMachine.create(
                self.userapiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkids=network.id,
                serviceofferingid=self.service_offering.id,
            )
            self.assertIsNotNone(
                vm_created,
                "Failed to launch a VM under network created"
            )
            # Listing all the IP Addresses for a user
            list_ipaddresses_before = PublicIPAddress.list(
                self.userapiclient,
                listall=self.services["listall"]
            )
            status = validateList(list_ipaddresses_before)
            self.assertEqual(
                PASS,
                status[0],
                "IP Addresses Association Failed while launching a VM"
            )
            # Verifying the length of the list is 1
            self.assertEqual(
                1,
                len(list_ipaddresses_before),
                "IP Addresses Association Failed while launching a VM"
            )
            # Associating an IP Addresses to Network created
            associated_ipaddress = PublicIPAddress.create(
                self.userapiclient,
                services=self.services["network"],
                networkid=network.id
            )
            self.assertIsNotNone(
                associated_ipaddress,
                "Failed to Associate IP Address"
            )
            # Listing all the IP Addresses for a user
            list_ipaddresses_after = PublicIPAddress.list(
                self.userapiclient,
                listall=self.services["listall"]
            )
            status = validateList(list_ipaddresses_after)
            self.assertEqual(
                PASS,
                status[0],
                "IP Addresses Association Failed"
            )
            # Verifying the length of the list is 1
            self.assertEqual(
                2,
                len(list_ipaddresses_after),
                "Number of IP Addresses associated are not matching expected"
            )
            # Listing Service Offerings
            list_service_offerings = ServiceOffering.list(
                self.userapiclient,
                listall=self.services["listall"],
                issystem='false'
            )
            status = validateList(list_service_offerings)
            self.assertEqual(
                PASS,
                status[0],
                "Failed to list Service Offerings"
            )
            # Listing Users
            list_users = User.list(
                self.apiClient,
                listall=self.services["listall"],
                account=self.account.name,
                domainid=self.domain.id
            )
            status = validateList(list_users)
            self.assertEqual(
                PASS,
                status[0],
                "Failed to list Users"
            )
            # Listing Featured Templates
            list_templates_featured = Template.list(
                self.userapiclient,
                listall=self.services["listall"],
                templatefilter="featured",
                zoneid=self.zone.id
            )
            status = validateList(list_templates_featured)
            self.assertEqual(
                PASS,
                status[0],
                "Failed to list Featured Templates"
            )
            # Listing Community Templates
            list_templates_community = Template.list(
                self.userapiclient,
                listall=self.services["listall"],
                templatefilter="community",
                zoneid=self.zone.id
            )
            self.assertIsNone(
                list_templates_community,
                "Community Templates listed for newly created User"
            )
            # Listing selfexecutable Templates
            list_templates_selfexecutable = Template.list(
                self.userapiclient,
                listall=self.services["listall"],
                templatefilter="selfexecutable",
                zoneid=self.zone.id
            )
            self.assertIsNone(
                list_templates_selfexecutable,
                "Self Executable Templates listed for newly created User"
            )
            # Listing Autoscale VM Profiles
            list_autoscalevm_profiles_before =\
                Autoscale.listAutoscaleVmPofiles(
                    self.userapiclient,
                    listall=self.services["listall"]
                )
            self.assertIsNone(
                list_autoscalevm_profiles_before,
                "Autoscale VM Profiles listed"
            )
            # Creating Autoscale VM Profile
            counterparam = {"snmpcommunity": "public", "snmpport": "161"}
            autoscalevm_profile = Autoscale.createAutoscaleVmProfile(
                self.userapiclient,
                serviceofferingid=list_service_offerings[0].id,
                zoneid=self.zone.id,
                templateid=list_templates_featured[0].id,
                autoscaleuserid=list_users[0].id,
                destroyvmgraceperiod='100',
                counterparam=counterparam
            )
            self.assertIsNotNone(
                autoscalevm_profile,
                "Failed to create Autoscale VM Profile"
            )
            # Listing Autoscale VM Profiles
            list_autoscalevm_profiles_after = Autoscale.listAutoscaleVmPofiles(
                self.userapiclient,
                listall=self.services["listall"]
            )
            status = validateList(list_autoscalevm_profiles_after)
            self.assertEqual(
                PASS,
                status[0],
                "Failed to list Autoscale VM Profile after creation"
            )
            # Verifying only 1 autoscale vm profile is created
            self.assertEqual(
                1,
                len(list_autoscalevm_profiles_after),
                "Count of Autoscale VM profiles listed is not matching"
            )
            # Listing the Autoscale VM Profile by id
            list_autoscalvmprofile = Autoscale.listAutoscaleVmPofiles(
                self.userapiclient,
                listall=self.services["listall"],
                id=autoscalevm_profile.id
            )
            status = validateList(list_autoscalvmprofile)
            self.assertEqual(
                PASS,
                status[0],
                "Failed to list Autoscale VM Profile by Id after creation"
            )
            # Verifying details of the listed Autoscale VM Profile
            # Creating expected and actual values dictionaries
            expected_dict = {
                "id": autoscalevm_profile.id,
                "account": self.account.name,
                "domainid": self.domain.id,
                "autoscaleuserid": list_users[0].id,
                "serviceofferingid": list_service_offerings[0].id,
                "zoneid": self.zone.id,
                "templateid": list_templates_featured[0].id,
                "destroyvmgraceperiod":
                autoscalevm_profile.destroyvmgraceperiod}
            actual_dict = {
                "id": list_autoscalvmprofile[0].id,
                "account": list_autoscalvmprofile[0].account,
                "domainid": list_autoscalvmprofile[0].domainid,
                "autoscaleuserid":
                list_autoscalvmprofile[0].autoscaleuserid,
                "serviceofferingid":
                list_autoscalvmprofile[0].serviceofferingid,
                "zoneid": list_autoscalvmprofile[0].zoneid,
                "templateid": list_autoscalvmprofile[0].templateid,
                "destroyvmgraceperiod":
                list_autoscalvmprofile[0].destroyvmgraceperiod}
            autoscalevm_profile_status = self.__verify_values(
                expected_dict,
                actual_dict
            )
            self.assertEqual(
                True,
                autoscalevm_profile_status,
                "Created Autoscale VM Profile details are not as expected"
            )
            # Updating destroyvmgrageperiod for created Autoscale VM Profile
            autoscalevm_profile_updated = Autoscale.updateAutoscaleVMProfile(
                self.userapiclient,
                id=autoscalevm_profile.id,
                autoscaleuserid=list_users[0].id,
                destroyvmgraceperiod='200',
                templateid=list_templates_featured[0].id,
            )
            self.assertIsNotNone(
                autoscalevm_profile_updated,
                "Failed to update Autoscale VM Profile"
            )
            # Verifyign that Destroy VM Graceperiod is updated in autoscale VM
            # Profile
            self.assertEqual(
                200,
                autoscalevm_profile_updated.destroyvmgraceperiod,
                "Failed to update destroy vm grace period"
            )
            # Destroying the VM
            vm_created.delete(self.apiClient)
        self.cleanup.append(self.account)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_19_create_update_autoscalevmgroup(self):
        """
        @summary: Test to list, create, update, enable,\
                disable Autoscale VM Profiles
        @Steps:
        Step1: Creating a Network Offering for Supported Service LB with
               Netscaler
        Step2: Create a Network using Network Offering created in Step1
        Step3: Launching a VM using Network created in Step2
        Step4: Associating IP Address for Network created in Step2
        Step5: Creating Scaleup condition and scaleup policy
        Step6: Creating Scaledown condition and scaledown policy
        Step7: Creating Autoscale VM profile
        Step8: Creating Load Balancer Rule
        Step9: Listing Autoscale Vm groups and verifying no Autoscale VM groups
               are listed
        Step10: Creating Autoscale VM Group and verifying it was created
        Step11: Listing Autoscale Vm groups and verifying 1 Autoscale VM groups
                is listed
        Step12: Disabling Autoscale VM group and verifying it was disabled
        Step13: Updating Autoscale VM group and verifying it was updated
        Step14: Enabling Autoscale VM group and verifying it was enabled
        """
        if self.hypervisor.lower() == 'kvm':
            self.skipTest(
                "ScaleVM is not supported on KVM. Hence, skipping the test")

        list_physical_networks = PhysicalNetwork.list(
            self.apiClient,
            zoneid=self.zone.id
        )

        run_flag = False
        for i in range(0, len(list_physical_networks)):
            list_network_serviceprovider = NetworkServiceProvider.list(
                self.apiClient,
                physicalnetworkid=list_physical_networks[i].id
            )
            for j in range(0, len(list_network_serviceprovider)):
                if((list_network_serviceprovider[j].name == 'Netscaler') and
                        (list_network_serviceprovider[j].state == 'Enabled')):
                    run_flag = True
                    break

        if not run_flag:
            self.debug(
                "Netscaler is not enabled and auto scale VM is\
                        applicable only for Netscaler")
        else:
            # Listing Network Offerings
            list_nwoff_before = NetworkOffering.list(
                self.apiClient,
                forvpc="false",
                guestiptype="Isolated",
                state="Enabled",
                supportedservices="SourceNat,Lb",
                zoneid=self.zone.id
            )
            # Creating Network Offerign with LB as Netscalar
            nwoff_created = NetworkOffering.create(
                self.apiClient,
                self.services["nw_off_isolated_netscaler"]
            )
            self.assertIsNotNone(
                nwoff_created,
                "Failed to Create Network Offering with\
                        LB sercvice for Netscaler")
            # Enable Network offering
            nwoff_created.update(self.apiClient, state='Enabled')
            # Listing Network Offerings again
            list_nwoff_after = NetworkOffering.list(
                self.apiClient,
                forvpc="false",
                guestiptype="Isolated",
                state="Enabled",
                supportedservices="SourceNat,Lb",
                zoneid=self.zone.id
            )
            self.assertEqual(
                len(list_nwoff_before) + 1,
                len(list_nwoff_after),
                "Failed to create Network Offering"
            )
            # Creating a Network Using the Network Offering
            network = Network.create(
                self.userapiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.domain.id,
                networkofferingid=nwoff_created.id,
                zoneid=self.zone.id
            )
            self.assertIsNotNone(
                network,
                "Network creation failed"
            )
            self.cleanup.append(network)
    #         self.cleanup.append(nwoff_created)
            # Launching a Virtual Machine
            vm_created = VirtualMachine.create(
                self.userapiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkids=network.id,
                serviceofferingid=self.service_offering.id,
            )
            self.assertIsNotNone(
                vm_created,
                "Failed to launch a VM under network created"
            )
            # Listing all the IP Addresses for a user
            list_ipaddresses_before = PublicIPAddress.list(
                self.userapiclient,
                listall=self.services["listall"]
            )
            status = validateList(list_ipaddresses_before)
            self.assertEqual(
                PASS,
                status[0],
                "IP Addresses Association Failed while launching a VM"
            )
            # Verifying the length of the list is 1
            self.assertEqual(
                1,
                len(list_ipaddresses_before),
                "IP Addresses Association Failed while launching a VM"
            )
            # Associating an IP Addresses to Network created
            associated_ipaddress = PublicIPAddress.create(
                self.userapiclient,
                services=self.services["network"],
                networkid=network.id
            )
            self.assertIsNotNone(
                associated_ipaddress,
                "Failed to Associate IP Address"
            )
            # Listing all the IP Addresses for a user
            list_ipaddresses_after = PublicIPAddress.list(
                self.userapiclient,
                listall=self.services["listall"]
            )
            status = validateList(list_ipaddresses_after)
            self.assertEqual(
                PASS,
                status[0],
                "IP Addresses Association Failed"
            )
            # Verifying the length of the list is 1
            self.assertEqual(
                2,
                len(list_ipaddresses_after),
                "Number of IP Addresses associated are not matching expected"
            )
            # Listing Users
            list_users = User.list(
                self.apiClient,
                listall=self.services["listall"],
                account=self.account.name,
                domainid=self.domain.id
            )
            status = validateList(list_users)
            self.assertEqual(
                PASS,
                status[0],
                "Failed to list Users"
            )
            # Listing counters
            list_counters = Autoscale.listCounters(self.userapiclient)
            status = validateList(list_counters)
            self.assertEqual(
                PASS,
                status[0],
                "Failed to list Counters"
            )
            # Create Condition for scaleup Vm Policy
            condition_scaleup = Autoscale.createCondition(
                self.userapiclient,
                counterid=list_counters[0].id,
                relationaloperator='GT',
                threshold='40'
            )
            self.assertIsNotNone(
                condition_scaleup,
                "Failed to create Scaleup Condition"
            )
            # Creating scaleup Vm Policy
            scaleup_policy = Autoscale.createAutoscalePolicy(
                self.userapiclient,
                action='scaleup',
                conditionids=condition_scaleup.id,
                duration='40',
                quiettime='300'
            )
            self.assertIsNotNone(
                scaleup_policy,
                "Failed to create Scaleup VM Policy"
            )
            # Create Condition for scaledown Vm Policy
            condition_scaledown = Autoscale.createCondition(
                self.userapiclient,
                counterid=list_counters[0].id,
                relationaloperator='GT',
                threshold='10'
            )
            self.assertIsNotNone(
                condition_scaledown,
                "Failed to create Scaledown Condition"
            )
            # Creating scaledown Vm Policy
            scaledown_policy = Autoscale.createAutoscalePolicy(
                self.userapiclient,
                action='scaledown',
                conditionids=condition_scaledown.id,
                duration='40',
                quiettime='300'
            )
            self.assertIsNotNone(
                scaledown_policy,
                "Failed to create Scaledown VM Policy"
            )
            counterparam = {"snmpcommunity": "public", "snmpport": "161"}
            # Create Autoscale VM Profile
            vmprofile = Autoscale.createAutoscaleVmProfile(
                self.userapiclient,
                serviceofferingid=self.service_offering.id,
                zoneid=self.zone.id,
                templateid=self.template.id,
                autoscaleuserid=list_users[0].id,
                destroyvmgraceperiod='30',
                counterparam=counterparam
            )
            self.assertIsNotNone(
                vmprofile,
                "Failed to create Autoscale Vm Profile"
            )
            self.services["lbrule"]["openfirewall"] = False
            # Creating Load Balancer Rule
            lbrule = LoadBalancerRule.create(
                self.userapiclient,
                self.services["lbrule"],
                ipaddressid=associated_ipaddress.ipaddress.id,
                accountid=self.account.name,
                networkid=network.id,
                domainid=self.domain.id
            )
            self.assertIsNotNone(
                lbrule,
                "Failed to create Load Balancer Rule"
            )
            # Listing Autoscale VM Groups
            list_vmgroup_before = Autoscale.listAutoscaleVmGroup(
                self.userapiclient,
                listall=self.services["listall"],
                lbruleid=lbrule.id
            )
            # Verifying No Autoscale VM Groups are listed
            self.assertIsNone(
                list_vmgroup_before,
                "Listed Autoscale VM Groups for newly created LB Rule"
            )
            # Creating Autoscale VM Group
            vmgroup = Autoscale.createAutoscaleVmGroup(
                self.userapiclient,
                lbruleid=lbrule.id,
                minmembers='3',
                maxmembers='10',
                scaledownpolicyids=scaledown_policy.id,
                scaleuppolicyids=scaleup_policy.id,
                vmprofileid=vmprofile.id,
                interval='30'
            )
            self.assertIsNotNone(
                vmgroup,
                "Failed to create Autoscale VM Group"
            )
            # Listing Autoscale VM Groups
            list_vmgroup_after = Autoscale.listAutoscaleVmGroup(
                self.userapiclient,
                listall=self.services["listall"],
                lbruleid=lbrule.id
            )
            status = validateList(list_vmgroup_after)
            self.assertEqual(
                PASS,
                status[0],
                "Failed to list Autoscale VM group after creation"
            )
            # Verifying only 1 Autoscale VM group is listed
            self.assertEqual(
                1,
                len(list_vmgroup_after),
                "Autoscale VM group list count is not matching"
            )
            # Disabling Autoscale VM group
            vmgroup_disabled = Autoscale.disableAutoscaleVmGroup(
                self.userapiclient,
                id=vmgroup.id
            )
            self.assertIsNotNone(
                vmgroup_disabled,
                "Failed to disable Autoscale VM group"
            )
            # Verifyign the state of the VM Group afte renabling
            self.assertEqual(
                "disabled",
                vmgroup_disabled.state,
                "Disabled VM Group state is not matching"
            )
            # Updating Autoscale VM Group
            vmgroup_updated = Autoscale.updateAutoscaleVMGroup(
                self.userapiclient,
                id=vmgroup.id,
                minmembers='3',
                maxmembers='10',
                scaledownpolicyids=scaledown_policy.id,
                scaleuppolicyids=scaleup_policy.id,
                interval='40'
            )
            self.assertIsNotNone(
                vmgroup_updated,
                "Failed to update Autoscale VM group"
            )
            self.assertEqual(
                40,
                vmgroup_updated.interval,
                "Updated Autoscale VM group interval value is not matching"
            )
            # Enabling Autoscale VM group
            vmgroup_enabled = Autoscale.enableAutoscaleVmGroup(
                self.userapiclient,
                id=vmgroup.id
            )
            self.assertIsNotNone(
                vmgroup_enabled,
                "Failed to enable Autoscale VM group"
            )
            # Verifyign the state of the VM Group afte renabling
            self.assertEqual(
                "enabled",
                vmgroup_enabled.state,
                "Enabled VM Group state is not matching"
            )
            # Destroying the VM
            vm_created.delete(self.apiClient)
        self.cleanup.append(self.account)
        return
