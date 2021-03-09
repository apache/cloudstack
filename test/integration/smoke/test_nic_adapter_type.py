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

"""
    P1 test for testing specific adapter type
    for added network for vmware
"""

# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.base import (
                             Account,
                             ServiceOffering,
                             VirtualMachine,
                             NetworkOffering,
                             Network,
                             Template
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_test_template
                               )
from marvin.lib.utils import (
                              get_hypervisor_type,
                              cleanup_resources,
                              validateList)
from marvin.codes import PASS


class TestAdapterTypeForNic(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestAdapterTypeForNic, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()

        cls.hypervisor = get_hypervisor_type(cls.api_client)

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        cls.template = get_test_template(
            cls.api_client,
            cls.zone.id,
            cls.hypervisor)

        # Create Accounts & networks
        cls.testdata["isolated_network"]["zoneid"] = cls.zone.id
        cls._cleanup = []

        cls.account = Account.create(
            cls.api_client,
            cls.testdata["account"],
            domainid=cls.domain.id)
        cls._cleanup.append(cls.account)

        cls.userapiclient = cls.testClient.getUserApiClient(
            UserName=cls.account.name,
            DomainName=cls.account.domain
        )

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"])
        cls._cleanup.append(cls.service_offering)

        # Create Shared Network Offering
        cls.isolated_network_offering = NetworkOffering.create(
            cls.api_client,
            cls.testdata["isolated_network_offering"])
        cls._cleanup.append(cls.isolated_network_offering)
        # Enable Isolated Network offering
        cls.isolated_network_offering.update(cls.api_client, state='Enabled')
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @unittest.skip("VCenter API Integration Remaining")
    @attr(tags=["advanced"])
    def test_vm_nic_adapter_vmxnet3(self):
        """

        # 1. Register a template for VMware with nicAdapter vmxnet3
        # 2. Deploy a VM using this template
        # 3. Create an isolated network
        # 4. Add network to VM
        # 5. Verify that the NIC adapter for VM for both the nics
        #    is vmxnet3
        """

        if self.hypervisor.lower() not in ["vmware"]:
            self.skipTest("This test case is written specifically\
                    for Vmware hypervisor")

        # Register a private template in the account with nic adapter vmxnet3
        template = Template.register(
            self.userapiclient,
            self.testdata["configurableData"]["vmxnet3template"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            details=[{"nicAdapter": self.testdata["configurableData"]["vmxnet3template"]["nicadapter"]}]
        )
        self.cleanup.append(template)
        template.download(self.apiclient)

        templates = Template.list(
            self.userapiclient,
            listall=True,
            id=template.id,
            templatefilter="self")

        self.assertEqual(
            validateList(templates)[0],
            PASS,
            "Templates list validation failed")

        self.testdata["virtual_machine"]["zoneid"] = self.zone.id
        self.testdata["virtual_machine"]["template"] = template.id

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id)

        isolated_network = Network.create(
            self.apiclient,
            self.testdata["isolated_network"],
            self.account.name,
            self.account.domainid,
            networkofferingid=self.isolated_network_offering.id)

        virtual_machine.add_nic(self.apiclient, isolated_network.id)

        # TODO: Add steps to check the Nic Adapter type in VCenter
        # using VCenter APIs
        return
