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
""" Test case to check if queryAsyncJobResult returns jobinstanceid
"""
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               )

from marvin.cloudstackAPI import queryAsyncJobResult


class TestJobinstanceid(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestJobinstanceid, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()

        cls.hypervisor = cls.testClient.getHypervisorInfo()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata["ostype"])

        cls._cleanup = []

        try:

            # Create an account
            cls.account = Account.create(
                cls.apiclient,
                cls.testdata["account"],
                domainid=cls.domain.id
            )
            # Create user api client of the account
            cls.userapiclient = testClient.getUserApiClient(
                UserName=cls.account.name,
                DomainName=cls.account.domain
            )

            # Create Service offering
            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"],
            )

            cls._cleanup = [
                cls.account,
                cls.service_offering,
            ]
        except Exception as e:
            cls.tearDownClass()
            raise e
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_queryAsyncJobResult_jobinstanceid(self):
        """ Test queryAsyncJobResult api return jobinstanceid

        # 1. Deploy a VM
        # 2. Call queryAsyncJobResult API with jobid of previous step
        # 3. Verify that queryAsyncJobResult returns jobinstanceid

        """
        # Step 1

        vm = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
        )

        # Step 2
        cmd = queryAsyncJobResult.queryAsyncJobResultCmd()
        cmd.jobid = vm.jobid
        result = self.apiclient.queryAsyncJobResult(cmd)

        # Step 3
        self.assertTrue(
            "jobinstanceid" in dir(result),
            "Check if jobinstanceid is returned by queryAsyncJobResult API")

        return
