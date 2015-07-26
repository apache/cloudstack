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
""" Test case for Create Custom DiskOffering with size Test Path
"""

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (createDiskOffering, deleteDiskOffering)
from marvin.lib.utils import (cleanup_resources)
from marvin.lib.common import (get_domain,
                               get_zone,
                               )


class TestCustomDiskOfferingWithSize(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(
            TestCustomDiskOfferingWithSize,
            cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls._cleanup = []

        cls.tearDownClass()

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
            if hasattr(self, 'disk_offering'):
                cmd = deleteDiskOffering.deleteDiskOfferingCmd()
                cmd.id = self.disk_offering.id
                self.apiclient.deleteDiskOffering(cmd)

            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["basic", "advanced"], required_hardware="false")
    def test_create_custom_disk_offering_with_size(self):
        """ Create custom disk offerign with size
            1.   Create custom disk offering with size.
            2.   Should not allow to create custom disk offering
                 with size mentioned.(Exception should be raised)
        """

        with self.assertRaises(Exception):
            cmd = createDiskOffering.createDiskOfferingCmd()
            cmd.displaytext = "Custom Disk Offering"
            cmd.name = "Custom Disk Offering"
            cmd.customized = True
            cmd.disksize = 2
            self.disk_offering = self.apiclient.createDiskOffering(cmd)

        return
