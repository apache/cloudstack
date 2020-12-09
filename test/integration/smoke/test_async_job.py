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
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import ServiceOffering, DiskOffering, Account, VirtualMachine,\
    queryAsyncJobResult, PASS
from marvin.lib.common import get_domain, get_zone, get_test_template
from pytz import timezone


class TestAsyncJob(cloudstackTestCase):
    """
    Test queryAsyncJobResult
    """
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

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as exception:
            raise Exception("Warning: Exception during cleanup : %s" % exception)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.testdata["virtual_machine"]["zoneid"] = self.zone.id
        self.testdata["virtual_machine"]["template"] = self.template.id
        self.testdata["iso"]["zoneid"] = self.zone.id
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup = [self.account]

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as exception:
            self.debug("Warning! Exception in tearDown: %s" % exception)

    @attr(tags=["advanced", "eip", "advancedns", "basic", "sg"], required_hardware="false")
    def test_query_async_job_result(self):
        """
        Test queryAsyncJobResult API for expected values
        """
        self.debug("Deploying instance in the account: %s" %
                   self.account.name)
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            hypervisor=self.hypervisor
        )

        response = virtual_machine.getState(
            self.apiclient,
            VirtualMachine.RUNNING)
        self.assertEqual(response[0], PASS, response[1])

        cmd = queryAsyncJobResult.queryAsyncJobResultCmd()
        cmd.jobid = virtual_machine.jobid
        cmd_response = self.apiclient.queryAsyncJobResult(cmd)

        db_result = self.dbclient.execute("select * from async_job where uuid='%s'" %
                                          virtual_machine.jobid)

        # verify that 'completed' value from api equals 'removed' db column value
        completed = cmd_response.completed
        removed = timezone('UTC').localize(db_result[0][17])
        removed = removed.strftime("%Y-%m-%dT%H:%M:%S%z")
        self.assertEqual(completed, removed,
                         "Expected 'completed' timestamp value %s to be equal to "
                         "'removed' db column value %s." % (completed, removed))

        # verify that api job_status value equals db job_status value
        jobstatus_db = db_result[0][8]
        jobstatus_api = cmd_response.jobstatus
        self.assertEqual(jobstatus_api, jobstatus_db,
                         "Expected 'jobstatus' api value %s to be equal to "
                         "'job_status' db column value %s." % (jobstatus_api, jobstatus_db))
