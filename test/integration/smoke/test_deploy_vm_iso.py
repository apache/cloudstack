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

""" P1 for Deploy VM from ISO
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             Iso,
                             DiskOffering)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_test_template)
from marvin.codes import PASS


class TestDeployVMFromISO(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestDeployVMFromISO, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        cls.template = get_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor
        )

        # Create service, disk offerings  etc
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.testdata["service_offering"]
        )

        cls.disk_offering = DiskOffering.create(
            cls.apiclient,
            cls.testdata["disk_offering"]
        )

        cls._cleanup = [
            cls.service_offering,
            cls.disk_offering
        ]
        return

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.testdata["virtual_machine"]["zoneid"] = self.zone.id
        self.testdata["virtual_machine"]["template"] = self.template.id
        self.testdata["iso"]["zoneid"] = self.zone.id
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup = [self.account]
        return

    @attr(
        tags=[
            "advanced",
            "eip",
            "advancedns",
            "basic",
            "sg"
        ],
        required_hardware="true"
    )
    def test_deploy_vm_from_iso(self):
        """Test Deploy Virtual Machine from ISO
        """

        # Validate the following:
        # 1. Create an ISO
        # 2. Deploy a VM from the ISO
        # 3. VM should be in 'Running' state

        self.iso = Iso.create(
            self.apiclient,
            self.testdata["configurableData"]["bootableIso"],
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id
        )
        self.cleanup.append(self.iso)

        self.debug("ISO created with ID: %s" % self.iso.id)

        list_iso_response = Iso.list(
            self.apiclient,
            id=self.iso.id
        )
        self.assertEqual(
            isinstance(list_iso_response, list),
            True,
            "Check list response returns a valid list"
        )

        try:
            # Download the ISO
            self.iso.download(self.apiclient)
        except Exception as e:
            raise Exception("Exception while downloading ISO %s: %s"
                            % (self.iso.id, e))

        self.debug(f"Registered ISO: {self.iso.name}")
        self.debug(f"Deploying instance in the account: {self.account.name}")
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            templateid=self.iso.id,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            hypervisor=self.hypervisor
        )
        self.cleanup.append(self.virtual_machine)

        self.debug("VM created with ID: %s" % self.virtual_machine.id)

        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id
        )

        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )
        vm_response = list_vm_response[0]

        self.assertEqual(
            vm_response.state,
            "Running",
            "Check virtual machine is in running state"
        )

        self.assertEqual(
            vm_response.isoid,
            self.iso.id,
            "Check virtual machine is booted from the ISO"
        )
        return
