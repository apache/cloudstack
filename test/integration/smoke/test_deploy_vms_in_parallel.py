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
                             DiskOffering,
                             Domain,
                             Resources,
                             ServiceOffering,
                             VirtualMachine)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_test_template)
from marvin.codes import PASS


class TestDeployVMsInParallel(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestDeployVMsInParallel, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        cls._cleanup = []

        cls.template = get_test_template(
            cls.api_client,
            cls.zone.id,
            cls.hypervisor
        )

        # Create service, disk offerings  etc
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)

        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.testdata["disk_offering"]
        )
        cls._cleanup.append(cls.disk_offering)

        return

    @classmethod
    def tearDownClass(cls):
        super(TestDeployVMsInParallel, cls).tearDownClass()

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.testdata["virtual_machine"]["zoneid"] = self.zone.id
        self.testdata["virtual_machine"]["template"] = self.template.id
        self.testdata["iso"]["zoneid"] = self.zone.id
        self.domain = Domain.create(
            self.apiclient,
            self.testdata["domain"]
        )
        self.cleanup.append(self.domain)
        self.update_resource_limit()
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(self.account)
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            templateid=self.template.id,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            hypervisor=self.hypervisor
        )
        self.cleanup.append(virtual_machine)
        virtual_machine.delete(self.apiclient)
        self.cleanup.remove(virtual_machine)
        return

    def update_resource_limit(self):
        Resources.updateLimit(
            self.apiclient,
            domainid=self.domain.id,
            resourcetype=0,
            max=1
        )

    def tearDown(self):
        super(TestDeployVMsInParallel, self).tearDown()

    @attr(
        tags=[
            "advanced",
            "basic",
            "sg"],
        required_hardware="false")
    def test_deploy_vms(self):
        """
        Test Deploy Virtual Machines in parallel

        Validate the following:
        1. set limits
        2. deploy multiple VMs
        """

        self.debug("Deploying instance in the account: %s" %
                   self.account.name)
        self.virtual_machines = []
        for i in range(4):
            vm = VirtualMachine.create(
                self.apiclient,
                self.testdata["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                templateid=self.template.id,
                serviceofferingid=self.service_offering.id,
                diskofferingid=self.disk_offering.id,
                hypervisor=self.hypervisor
            )
            self.cleanup.append(vm)
            self.virtual_machines.append(vm)

        deployed = 0
        failed = 0
        for vm in self.virtual_machines:
            response = vm.getState(self.apiclient, VirtualMachine.RUNNING)
            if response[0] == PASS:
                deployed += 1
            else:
                failed += 1
        self.assertEqual(deployed, 1)
        self.assertEqual(failed, 3)
        # response = self.virtual_machine.getState(
        #     self.apiclient,
        #     VirtualMachine.RUNNING)
        # self.assertEqual(response[0], PASS, response[1])
        return
