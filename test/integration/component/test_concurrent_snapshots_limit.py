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
""" Test cases for validating global limit for concurrent snapshots
"""
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             Snapshot,
                             Volume,
                             Configurations
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template
                               )

from marvin.codes import PASS, BACKED_UP
from threading import Thread


class TestConcurrentSnapshotLimit(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestConcurrentSnapshotLimit, cls).getClsTestClient()
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
        cls.supportedHypervisor = True

        if cls.hypervisor.lower() in [
                "hyperv",
                "lxc"]:
            cls.supportedHypervisor = False
            return

        # Create Service offering
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.testdata["service_offering"],
        )
        cls._cleanup.append(cls.service_offering)
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

        self.exceptionOccured = False

        if not self.supportedHypervisor:
            self.skipTest("Snapshot not supported on %s" % self.hypervisor)

    def createSnapshot(self, volumeid):
        try:
            Snapshot.create(
                self.apiclient,
                volumeid
            )
        except Exception as e:
            self.debug("Exception occured: %s" % e)
            self.exceptionOccured = True

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_01_concurrent_snapshot_global_limit(self):
        """ Test if global value concurrent.snapshots.threshold.perhost
            value respected
            This is positive test cases and tests if we are able to create
            as many snapshots mentioned in global value
        # 1. Create an account and a VM in it
        # 2. Read the global value for concurrent.snapshots.threshold.perhost
        # 3. If the value is Null, create at least 10 concurrent snapshots
             and verify they are created successfully
        # 4. Else, create as many snapshots specified in the global value, and
             verify they are created successfully
        """

        # Create an account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )

        self.cleanup.append(account)
        # Create user api client of the account
        userapiclient = self.testClient.getUserApiClient(
            UserName=account.name,
            DomainName=account.domain
        )

        # Create VM
        virtual_machine = VirtualMachine.create(
            userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=account.name,
            domainid=account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id
        )

        # Create 10 concurrent snapshots by default
        # We can have any value, so keeping it 10 as it
        # seems good enough to test
        concurrentSnapshots = 10

        # Step 1
        # Get ROOT Volume Id
        volumes = Volume.list(
            self.apiclient,
            virtualmachineid=virtual_machine.id,
            type='ROOT',
            listall=True
        )

        self.assertEqual(validateList(volumes)[0], PASS,
                         "Volumes list validation failed")

        root_volume = volumes[0]

        config = Configurations.list(
            self.apiclient,
            name="concurrent.snapshots.threshold.perhost"
        )
        if config[0].value:
		self.assertEqual(
				isinstance(
					config,
					list),
				True,
				"concurrent.snapshots.threshold.perhost should be present\
				in global config")
		concurrentSnapshots = int(config[0].value)
        self.debug("concurrent Snapshots: %s" % concurrentSnapshots)

        threads = []
        for i in range(0, (concurrentSnapshots)):
            thread = Thread(
                target=Snapshot.create,
                args=(
                    self.apiclient,
                    root_volume.id
                ))
            threads.append(thread)
            thread.start()
        for thread in threads:
            thread.join()

        snapshots = Snapshot.list(self.apiclient,
                                  volumeid=root_volume.id,
                                  listall=True)

        self.assertEqual(validateList(snapshots)[0], PASS,
                         "Snapshots list validation failed")
        self.assertEqual(
            len(snapshots),
            concurrentSnapshots,
            "There should be exactly %s snapshots present" %
            concurrentSnapshots)

        for snapshot in snapshots:
            self.assertEqual(str(snapshot.state).lower(), BACKED_UP,
                             "Snapshot state should be backedUp but it is\
                            %s" % snapshot.state)
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_02_concurrent_snapshot_global_limit(self):
        """ Test if global value concurrent.snapshots.threshold.perhost
            value is respected
            This is negative test cases and tests no more concurrent
            snapshots as specified in global value are created
        # 1. Read the global value for concurrent.snapshots.threshold.perhost
        # 2. If the value is Null, skip the test case
        # 3. Create an account and a VM in it
        # 4. Create more concurrent snapshots than specified in
             global allowed limit
        # 5. Verify that exception is raised while creating snapshots
        """

        config = Configurations.list(
            self.apiclient,
            name="concurrent.snapshots.threshold.perhost"
        )
        if config[0].value:
		self.assertEqual(
				isinstance(
					config,
					list),
				True,
				"concurrent.snapshots.threshold.perhost should be present\
				in global config")
		concurrentSnapshots = int(config[0].value)
        else:
            self.skipTest("Skipping tests as the config value \
                    concurrent.snapshots.threshold.perhost is Null")

        # Create an account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )

        self.cleanup.append(account)
        # Create user api client of the account
        userapiclient = self.testClient.getUserApiClient(
            UserName=account.name,
            DomainName=account.domain
        )

        # Create VM
        virtual_machine = VirtualMachine.create(
            userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=account.name,
            domainid=account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id
        )

        # Step 1
        # Get ROOT Volume Id
        volumes = Volume.list(
            self.apiclient,
            virtualmachineid=virtual_machine.id,
            type='ROOT',
            listall=True
        )

        self.assertEqual(validateList(volumes)[0], PASS,
                         "Volumes list validation failed")

        root_volume = volumes[0]

        threads = []
        for i in range(0, (concurrentSnapshots + 1)):
            thread = Thread(
                target=self.createSnapshot,
                args=(
                    self.apiclient,
                    root_volume.id
                ))
            threads.append(thread)
            thread.start()

        for thread in threads:
            thread.join()

        self.assertTrue(self.exceptionOccured, "Concurrent snapshots\
                more than concurrent.snapshots.threshold.perhost\
                value successfully created")
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_03_concurrent_snapshot_global_value_assignment(self):
        """ Test verifies that exception is raised if string value is assigned to
             concurrent.snapshots.threshold.perhost parameter.
        """
        with self.assertRaises(Exception):
           Configurations.update(
             self.apiclient,
             "concurrent.snapshots.threshold.perhost",
             "String"
           )
        return
