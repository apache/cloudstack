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

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from pytz import timezone


class TestAsyncJob(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestAsyncJob, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()

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

        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.testdata["disk_offering"]
        )

        cls._cleanup = [
            cls.service_offering,
            cls.disk_offering
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

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

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)

    @attr(
        tags=[
            "advanced",
            "eip",
            "advancedns",
            "basic",
            "sg"],
        required_hardware="true")
    def test_queryAsyncJobResult(self):
        """
        Test queryAsyncJobResult API for expected values
        """
        self.debug("Deploying instance in the account: %s" %
                   self.account.name)
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            hypervisor=self.hypervisor
        )

        response = self.virtual_machine.getState(
            self.apiclient,
            VirtualMachine.RUNNING)
        self.assertEqual(response[0], PASS, response[1])

        result = self.dbclient.execute("select * from async_job where uuid='%s'" % self.virtual_machine.jobid)

        cmd = queryAsyncJobResult.queryAsyncJobResultCmd()
        cmd.jobid = self.virtual_machine.jobid
        cmd_response = self.apiclient.queryAsyncJobResult(cmd)

        # verify that 'completed' value from api equals 'removed' db column value
        completed = cmd_response.completed
        removed = timezone('UTC').localize(result[0][17])
        removed = removed.astimezone(timezone('CET'))
        removed = removed.strftime("%Y-%m-%dT%H:%M:%S%z")
        self.assertEqual(completed, removed, "Expected 'completed' tag value to be equal to 'removed' db column value.")

        return

