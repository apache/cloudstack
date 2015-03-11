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
""" Test cases for checking usage events
"""
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources, validateList)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
from marvin.codes import (PASS)

class TestUsageEvents(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestUsageEvents, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata["ostype"])
        cls._cleanup = []

        try:
            # Create large service offering so that VM creation fails
            cls.testdata["service_offering"]["cpunumber"] = "8"
            cls.testdata["service_offering"]["cpuspeed"] = "8096"
            cls.testdata["service_offering"]["memory"] = "8096"

            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"]
            )
            cls._cleanup.append(cls.service_offering)
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
        # Create an account
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(self.account)

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced, basic"], required_hardware="true")
    def test_01_positive_tests_usage(self):
        """ Check events in usage_events table when VM creation fails

        Steps:
        1. Create service offering with large resource numbers
        2. Try to deploy a VM
        3. VM creation should fail and VM should be in error state
        4. Destroy the VM with expunge parameter True
        5. Check the events for the account in usage_events table
        6. There should be VM.CREATE, VM.DESTROY, VOLUME.CREATE and
            VOLUME.DELETE events present in the table
        """
        # Create VM in account
        with self.assertRaises(Exception):
            VirtualMachine.create(
                self.apiclient,
                self.testdata["small"],
                templateid=self.template.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                zoneid=self.zone.id
            )

        vms = VirtualMachine.list(self.apiclient,
                                  account=self.account.name,
                                  domaind=self.account.domainid)

        self.assertEqual(validateList(vms)[0], PASS,
                         "Vm list validation failed")

        self.assertEqual(vms[0].state.lower(), "error",
                         "VM should be in error state")

        qresultset = self.dbclient.execute(
            "select id from account where uuid = '%s';"
            % self.account.id
        )
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        account_id = qresult[0]
        self.debug("select type from usage_event where account_id = '%s';"
                   % account_id)

        qresultset = self.dbclient.execute(
            "select type from usage_event where account_id = '%s';"
            % account_id
        )
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = str(qresultset)
        self.debug("Query result: %s" % qresult)

        # Check if VM.CREATE, VM.DESTROY events present in usage_event table
        self.assertEqual(
            qresult.count('VM.CREATE'),
            1,
            "Check VM.CREATE event in events table"
        )

        self.assertEqual(
            qresult.count('VM.DESTROY'),
            1,
            "Check VM.DESTROY in list events"
        )

        # Check if VOLUME.CREATE, VOLUME.DELETE events present in usage_event
        # table
        self.assertEqual(
            qresult.count('VOLUME.CREATE'),
            1,
            "Check VOLUME.CREATE in events table"
        )

        self.assertEqual(
            qresult.count('VOLUME.DELETE'),
            1,
            "Check VM.DELETE in events table"
        )
        return
