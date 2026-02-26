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
""" BVT tests for Vpc offerings"""

#Import Local Modules
from marvin.codes import FAILED
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (createVPCOffering,
                                  listVPCOfferings,
                                  updateVPCOffering)
from marvin.lib.utils import (isAlmostEqual,
                              cleanup_resources,
                              random_gen)
from marvin.lib.base import (Domain,
                             VpcOffering,
                             Account,
                             VPC,
                             NetworkOffering,
                             Network,
                             VirtualMachine,
                             ServiceOffering,
                             PublicIPAddress,
                             NATRule)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_test_template)
from nose.plugins.attrib import attr

import time
from marvin.sshClient import SshClient
from marvin.cloudstackException import CloudstackAPIException
from marvin.lib.decoratorGenerators import skipTestIf


_multiprocess_shared_ = True

class Services:
    """Test VPC network services - Port Forwarding Rules Test Data Class.
    """

    def __init__(self):
        self.services = {
            "vpc_offering": {
                "name": 'Redundant VPC off',
                "displaytext": 'Redundant VPC off',
                "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Lb,UserData,StaticNat',
                "serviceProviderList": {
                    "Vpn": 'VpcVirtualRouter',
                    "Dhcp": 'VpcVirtualRouter',
                    "Dns": 'VpcVirtualRouter',
                    "SourceNat": 'VpcVirtualRouter',
                    "PortForwarding": 'VpcVirtualRouter',
                    "Lb": 'VpcVirtualRouter',
                    "UserData": 'VpcVirtualRouter',
                    "StaticNat": 'VpcVirtualRouter',
                    "NetworkACL": 'VpcVirtualRouter'
                },
            },
            "vpc": {
                "name": "TestVPC",
                "displaytext": "TestVPC",
                "cidr": '10.0.0.0/16'
            }
        }

class TestCreateDomainsVpcOffering(cloudstackTestCase):

    def setUp(self):
        self.services = self.testClient.getParsedTestDataConfig()
        self.localservices = Services().services
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        testClient = super(TestCreateDomainsVpcOffering, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        # Create domains
        cls.domain_1 = Domain.create(
            cls.apiclient,
            cls.services["acl"]["domain1"]
        )
        cls.domain_11 = Domain.create(
            cls.apiclient,
            cls.services["acl"]["domain11"],
            parentdomainid=cls.domain_1.id
        )
        cls.domain_2 = Domain.create(
            cls.apiclient,
            cls.services["acl"]["domain2"]
        )
        cls._cleanup = [
            cls.domain_11,
            cls.domain_1,
            cls.domain_2
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.apiclient = super(
                TestCreateDomainsVpcOffering,
                cls).getClsTestClient().getApiClient()
            # Clean up, terminate the created templates
            cleanup_resources(cls.apiclient, cls._cleanup)

        except Exception as e:
            raise

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns",
            "smoke"],
        required_hardware="false")
    def test_01_create_vpc_offering(self):
        """Test to create vpc offering

        # Validate the following:
        # 1. createVPCOfferings should return valid info for new offering
        # 2. The Cloud Database contains the valid information
        """
        offering_data_domainid = "{0},{1}".format(self.domain_11.id, self.domain_2.id)
        offering_data = self.localservices["vpc_offering"]
        cmd = createVPCOffering.createVPCOfferingCmd()
        cmd.name = "-".join([offering_data["name"], random_gen()])
        cmd.displaytext = offering_data["displaytext"]
        cmd.supportedServices = offering_data["supportedservices"]
        cmd.domainid = offering_data_domainid
        if "serviceProviderList" in offering_data:
            for service, provider in list(offering_data["serviceProviderList"].items()):
                providers = provider
                if isinstance(provider, str):
                    providers = [provider]

                for provider_item in providers:
                    cmd.serviceproviderlist.append({
                        'service': service,
                        'provider': provider_item
                    })

        vpc_offering = VpcOffering(self.apiclient.createVPCOffering(cmd).__dict__)
        self.cleanup.append(vpc_offering)

        self.debug("Created Vpc offering with ID: %s" % vpc_offering.id)

        cmd = listVPCOfferings.listVPCOfferingsCmd()
        cmd.id = vpc_offering.id
        list_vpc_response = self.apiclient.listVPCOfferings(cmd)
        self.assertEqual(
            isinstance(list_vpc_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(list_vpc_response),
            0,
            "Check Vpc offering is created"
        )
        vpc_response = list_vpc_response[0]

        self.assertEqual(
            vpc_response.id,
            vpc_offering.id,
            "Check server id in createVPCOffering"
        )
        self.assertEqual(
            vpc_response.displaytext,
            self.localservices["vpc_offering"]["displaytext"],
            "Check server displaytext in createVPCOffering"
        )
        self.assertCountEqual(
            vpc_response.domainid.split(","),
            offering_data_domainid.split(","),
            "Check domainid in createVPCOffering"
        )
        return

class TestDomainsVpcOfferings(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):

        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDomainsVpcOfferings, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.localservices = Services().services
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        # Create domains
        cls.domain_1 = Domain.create(
            cls.apiclient,
            cls.services["acl"]["domain1"]
        )
        cls.domain_11 = Domain.create(
            cls.apiclient,
            cls.services["acl"]["domain11"],
            parentdomainid=cls.domain_1.id
        )
        cls.domain_2 = Domain.create(
            cls.apiclient,
            cls.services["acl"]["domain2"]
        )
        cls.domain_3 = Domain.create(
            cls.apiclient,
            cls.services["acl"]["domain12"]
        )

        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())

        cls.vpc_offering = VpcOffering.create(
                                    cls.apiclient,
                                    cls.services["vpc_offering"]
                                )
        # Enable Vpc offering
        cls.vpc_offering.update(cls.apiclient, state='Enabled')
        cls._cleanup = [
            cls.vpc_offering,
            cls.domain_11,
            cls.domain_1,
            cls.domain_2,
            cls.domain_3
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.apiclient = super(TestDomainsVpcOfferings, cls).getClsTestClient().getApiClient()
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic", "eip", "sg", "advancedns",  "smoke"], required_hardware="false")
    def test_02_edit_vpc_offering(self):
        """Test to update existing vpc offering"""

        # 1. updateVPCOffering should return a valid information for the updated offering
        # 2. updateVPCOffering should fail while tring to add child domain but parent domain
        #    is already present
        # 3. updateVPCOffering should be able to add new domain to the offering
        self.debug("Updating vpc offering with ID: %s" %
                   self.vpc_offering.id)

        cmd = updateVPCOffering.updateVPCOfferingCmd()
        cmd.id = self.vpc_offering.id
        input_domainid ="{0},{1},{2}".format(self.domain_1.id, self.domain_11.id, self.domain_2.id)
        result_domainid = "{0},{1}".format(self.domain_1.id, self.domain_2.id)
        cmd.domainid = input_domainid
        self.apiclient.updateVPCOffering(cmd)

        cmd = listVPCOfferings.listVPCOfferingsCmd()
        cmd.id = self.vpc_offering.id
        list_vpc_response = self.apiclient.listVPCOfferings(cmd)
        self.assertEqual(
            isinstance(list_vpc_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_vpc_response),
            0,
            "Check Vpc offering is updated"
        )

        try:
            self.assertCountEqual(
                list_vpc_response[0].domainid.split(","),
                input_domainid.split(","),
                "Check child domainid in updateServiceOffering, should fail"
            )
            self.fail("Child domain added to offering when parent domain already exist. Must be an error.")
        except AssertionError:
            self.debug("Child domain check successful")

        self.assertCountEqual(
            list_vpc_response[0].domainid.split(","),
            result_domainid.split(","),
            "Check domainid in createVPCOffering"
        )

    @attr(
        tags=[
            "advanced",
            "eip",
            "sg",
            "advancedns",
            "smoke"],
        required_hardware="false")
    def test_03_create_vpc_domain_vpc_offering(self):
        """Test to creating vpc for an existing domain specified vpc offering"""

        # Validate the following:
        # 1. Vpc creation should fail for the user from a different domain
        # 2. Vpc creation should work for users from domains for which offering is specified
        self.debug("Deploying VM using vpc offering with ID: %s" %
                   self.vpc_offering.id)

        self.invalid_account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain_3.id
        )
        self.cleanup.append(self.invalid_account)

        try:
            VPC.create(
                apiclient=self.apiclient,
                services=self.services["vpc"],
                account=self.invalid_account.name,
                domainid=self.invalid_account.domainid,
                zoneid=self.zone.id,
                vpcofferingid=self.vpc_offering.id
            )
            self.fail("Vpc created for a user from domain which has not been specified for service offering. Must be an error.")
        except CloudstackAPIException:
            self.debug("Vpc creation for invalid user check")

        self.valid_account_1 = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain_1.id
        )
        self.cleanup.append(self.valid_account_1)
        VPC.create(
            apiclient=self.apiclient,
            services=self.services["vpc"],
            account=self.valid_account_1.name,
            domainid=self.valid_account_1.domainid,
            zoneid=self.zone.id,
            vpcofferingid=self.vpc_offering.id
        )
        self.debug("Vpc created for first subdomain %s" % self.valid_account_1.domainid)

        self.valid_account_2 = Account.create(
            self.apiclient,
            self.services["account2"],
            domainid=self.domain_2.id
        )
        self.cleanup.append(self.valid_account_2)
        VPC.create(
            apiclient=self.apiclient,
            services=self.services["vpc"],
            account=self.valid_account_2.name,
            domainid=self.valid_account_2.domainid,
            zoneid=self.zone.id,
            vpcofferingid=self.vpc_offering.id
        )
        self.debug("Vpc created for second subdomain %s" % self.valid_account_2.domainid)

        self.valid_account_3 = Account.create(
            self.apiclient,
            self.services["user"],
            domainid=self.domain_11.id
        )
        self.cleanup.append(self.valid_account_3)
        VPC.create(
            apiclient=self.apiclient,
            services=self.services["vpc"],
            account=self.valid_account_3.name,
            domainid=self.valid_account_3.domainid,
            zoneid=self.zone.id,
            vpcofferingid=self.vpc_offering.id
        )
        self.debug("Vpc created for first child subdomain %s" % self.valid_account_3.domainid)

        return

    @attr(
        tags=[
            "advanced",
            "eip",
            "sg",
            "advancedns",
            "smoke"],
        required_hardware="true")
    def test_04_validate_vpc_offering_conserve_mode_disabled(self):
        """Test to create and validate vpc with conserve mode disabled for an existing domain specified vpc offering"""

        # Validate the following:
        # 1. Create Vpc for user in domain for which offering is specified
        # 2. Validate that conserve mode is disabled for the vpc (cannot reuse ip address on multiple VPC tiers)

        template = get_test_template(
            self.apiclient,
            self.zone.id,
            self.hypervisor)
        if template == FAILED:
            assert False, "get_test_template() failed to return template"

        valid_account_1 = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain_1.id
        )
        self.cleanup.append(valid_account_1)

        service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["tiny"]
        )
        self.cleanup.append(service_offering)

        self.services["vpc"]["cidr"] = "10.10.20.0/24"
        vpc = VPC.create(
            apiclient=self.apiclient,
            services=self.services["vpc"],
            account=valid_account_1.name,
            domainid=valid_account_1.domainid,
            zoneid=self.zone.id,
            vpcofferingid=self.vpc_offering.id
        )
        self.debug("Vpc created for subdomain %s" % valid_account_1.domainid)

        self.services["network_offering"]["supportedservices"] = 'Vpn,Dhcp,Dns,SourceNat,Lb,UserData,StaticNat,NetworkACL,PortForwarding'
        self.services["network_offering"]["serviceProviderList"] = {
            "Vpn": 'VpcVirtualRouter',
            "Dhcp": 'VpcVirtualRouter',
            "Dns": 'VpcVirtualRouter',
            "SourceNat": 'VpcVirtualRouter',
            "Lb": 'VpcVirtualRouter',
            "UserData": 'VpcVirtualRouter',
            "StaticNat": 'VpcVirtualRouter',
            "NetworkACL": 'VpcVirtualRouter',
            "PortForwarding": 'VpcVirtualRouter'
        }
        network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"]
        )
        network_offering.update(self.apiclient, state="Enabled")
        self.cleanup.append(network_offering)

        gateway_tier1 = "10.10.20.1"
        netmask_tiers = "255.255.255.240"

        self.services["network_offering"]["name"] = "tier1-" + vpc.id
        self.services["network_offering"]["displayname"] = "tier1-" + vpc.id
        tier1 = Network.create(
            self.apiclient,
            services=self.services["network_offering"],
            accountid=valid_account_1.name,
            domainid=valid_account_1.domainid,
            networkofferingid=network_offering.id,
            zoneid=self.zone.id,
            vpcid=vpc.id,
            gateway=gateway_tier1,
            netmask=netmask_tiers,
        )
        self.cleanup.append(tier1)

        gateway_tier2 = "10.10.20.17"
        self.services["network_offering"]["name"] = "tier2-" + vpc.id
        self.services["network_offering"]["displayname"] = "tier2-" + vpc.id
        tier2 = Network.create(
            self.apiclient,
            services=self.services["network_offering"],
            accountid=valid_account_1.name,
            domainid=valid_account_1.domainid,
            networkofferingid=network_offering.id,
            zoneid=self.zone.id,
            vpcid=vpc.id,
            gateway=gateway_tier2,
            netmask=netmask_tiers,
        )
        self.cleanup.append(tier2)

        self.services["virtual_machine"]["displayname"] = "vm1" + vpc.id
        vm1 = VirtualMachine.create(
            self.apiclient,
            services=self.services["virtual_machine"],
            templateid=template.id,
            zoneid=self.zone.id,
            accountid=valid_account_1.name,
            domainid=valid_account_1.domainid,
            serviceofferingid=service_offering.id,
            networkids=[tier1.id],
        )

        self.services["virtual_machine"]["displayname"] = "vm2" + vpc.id
        vm2 = VirtualMachine.create(
            self.apiclient,
            services=self.services["virtual_machine"],
            templateid=template.id,
            zoneid=self.zone.id,
            accountid=valid_account_1.name,
            domainid=valid_account_1.domainid,
            serviceofferingid=service_offering.id,
            networkids=[tier2.id],
        )
        self.cleanup.append(vm1)
        self.cleanup.append(vm2)

        public_ip = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            accountid=valid_account_1.name,
            domainid=valid_account_1.domainid,
            vpcid=vpc.id,
        )

        nat_rule = NATRule.create(
            self.apiclient,
            vm1,
            self.services["natrule"],
            ipaddressid=public_ip.ipaddress.id,
            vpcid=vpc.id,
            networkid=tier1.id,
        )

        self.services["natrule"]["privateport"] = 80
        self.services["natrule"]["publicport"] = 80
        try:
            NATRule.create(
                self.apiclient,
                vm2,
                self.services["natrule"],
                ipaddressid=public_ip.ipaddress.id,
                vpcid=vpc.id,
                networkid=tier2.id,
            )
            self.fail(
                "Expected cross-tier rule creation to fail with conserveMode=False, but succeeded"
            )
        except CloudstackAPIException as e:
            self.debug("Expected cross-tier rule creation to failure with conserveMode=False")
