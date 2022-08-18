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
from marvin.cloudstackAPI import (deployVirtualMachine,
                                  queryAsyncJobResult)


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

        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(self.account)

        self.userApiClient = self.testClient.getUserApiClient(UserName=self.account.name, DomainName=self.domain.name)
        virtual_machine = VirtualMachine.create(
            self.userApiClient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            templateid=self.template.id,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            hypervisor=self.hypervisor
        )
        self.cleanup.append(virtual_machine)
        self.networkids = virtual_machine.nic[0].networkid
        virtual_machine.delete(self.apiclient)
        self.cleanup.remove(virtual_machine)
        return

    def update_resource_limit(self, max=1):
        Resources.updateLimit(
            self.apiclient,
            domainid=self.domain.id,
            resourcetype=0,
            max=max
        )

    def tearDown(self):
        super(TestDeployVMsInParallel, self).tearDown()

    @attr(
        tags=[
            "advanced",
            "basic",
            "sg"],
        required_hardware="false")
    def test_deploy_more_vms_than_limit_allows(self):
        """
        Test Deploy Virtual Machines in parallel

        Validate the following:
        1. set limit to 2
        2. deploy more than 2 VMs
        """
        self.test_limits(vm_limit=2)

    def test_limits(self, vm_limit=1):
        self.info(f"==== limit: {vm_limit} ====")

        self.update_resource_limit(max=vm_limit)

        cmd = deployVirtualMachine.deployVirtualMachineCmd()
        cmd.serviceofferingid=self.service_offering.id
        cmd.diskofferingid=self.disk_offering.id
        cmd.templateid=self.template.id
        cmd.accountid=self.account.id
        cmd.domainid=self.account.domainid
        cmd.zoneid=self.zone.id
        cmd.networkids = self.networkids
        cmd.isAsync = "false"

        responses = []
        failed = 0
        for i in range(vm_limit+3):
            try:
                self.info(f"==== deploying instance #{i}")
                response = self.userApiClient.deployVirtualMachine(cmd, method="GET")
                responses.append(response)
            except Exception as e:
                failed += 1

        self.info(f"==== failed deploys: {failed} ====")

        self.assertEqual(failed, 3)
        self.assertEqual(len(responses), vm_limit) # we don´t care if the deploy succeed or failed for some other reason
