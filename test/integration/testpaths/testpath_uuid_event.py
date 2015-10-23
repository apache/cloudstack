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

""" Test cases to verify presentation of volume id in events table
    for 'SNAPSHOT.CREATE' type.
"""

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             Snapshot,
                             VirtualMachine,
                             Configurations
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_volumes,
                               )

from marvin.codes import PASS


class TestVerifyEventsTable(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestVerifyEventsTable, cls).getClsTestClient()
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

            cls.unsupportedHypervisor = False
            if cls.hypervisor.lower() in ['hyperv', 'lxc', 'kvm']:
                if cls.hypervisor.lower() == 'kvm':
                    configs = Configurations.list(
                        cls.apiclient,
                        name='kvm.snapshot.enabled'
                    )

                    if configs[0].value == "false":
                        cls.unsupportedHypervisor = True
                else:
                    cls.unsupportedHypervisor = True

                return
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

        self.cleanup = []
        if self.unsupportedHypervisor:
            self.skipTest(
                "snapshots are not supported on %s" %
                self.hypervisor.lower())
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_01_verify_events_table(self):
        """ Test events table

        # 1. Deploy a VM.
        # 2. Take VM snapshot.
        # 3. Verify that events table records UUID of the volume in descrption
            instead of volume ID
        """
        # Step 1
        # Create VM
        vm = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
        )
        volumes_list = list_volumes(
            self.apiclient,
            virtualmachineid=vm.id,
            type='ROOT',
            listall=True
        )

        volume_list_validation = validateList(volumes_list)
        self.assertEqual(
            volume_list_validation[0],
            PASS,
            "volume list validation failed due to %s" %
            volume_list_validation[2]
        )
        root_volume = volumes_list[0]

        # Step 2
        # Create snapshot of root volume
        snapshot = Snapshot.create(
            self.apiclient,
            root_volume.id)

        self.assertNotEqual(
            len(snapshot),
            0,
            "Check if snapshot gets created properly"
        )

        # Step 3
        qresultset = self.dbclient.execute(
            "select  description from event where type='SNAPSHOT.CREATE' AND \
                        description like '%%%s%%'" % root_volume.id)

        event_validation_result = validateList(qresultset)

        self.assertEqual(
            event_validation_result[0],
            PASS,
            "event list validation failed due to %s" %
            event_validation_result[2]
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check if events table records UUID of the volume"
        )

        return
