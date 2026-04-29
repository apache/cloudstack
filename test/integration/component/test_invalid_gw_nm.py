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
""" BVT tests for Network Life Cycle
"""

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Account,
                             Network,
                             NetworkOffering
                             )
from marvin.lib.common import (get_domain,
                               get_zone
                               )
from nose.plugins.attrib import attr


class TestIsolatedNetworkInvalidGw(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()

    @classmethod
    def setUpClass(cls):
        testClient = super(TestIsolatedNetworkInvalidGw, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        # Create Accounts
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )

        cls.services["network"]["zoneid"] = cls.zone.id

        cls.network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.services["network_offering"],
        )
        # Enable Network offering
        cls.network_offering.update(cls.apiclient, state='Enabled')

        cls.services["network"]["networkoffering"] = cls.network_offering.id
        cls._cleanup = [
            cls.account,
            cls.network_offering
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "smoke", "dvs"], required_hardware="false")
    def test_isolated_nw_invalid_gw(self):

        self.debug("Trying to create a network with Gateway as 192.168.3.0. This should fail")
        with self.assertRaises(Exception):
            Network.create(
                self.apiclient,
                self.services["network"],
                self.account.name,
                self.account.domainid,
                gateway="192.168.3.0",
                netmask="255.255.255.0",
            )

        self.debug("Trying to create a network with Gateway as 192.168.3.255")
        with self.assertRaises(Exception):
            Network.create(
                self.apiclient,
                self.services["network"],
                self.account.name,
                self.account.domainid,
                gateway="192.168.3.255",
                netmask="255.255.255.0"
            )

        self.debug("Trying to create a network with Gateway as 192.168.3.0 and Subnet mask as 255.0.255.0")
        with self.assertRaises(Exception):
            Network.create(
                self.apiclient,
                self.services["network"],
                self.account.name,
                self.account.domainid,
                gateway="192.168.3.0",
                netmask="255.0.255.0",
            )

        self.debug("Trying to create a network with Subnet mask as 255.0.255.0")
        with self.assertRaises(Exception):
            Network.create(
                self.apiclient,
                self.services["network"],
                self.account.name,
                self.account.domainid,
                gateway="192.168.3.1",
                netmask="255.0.255.0",
            )
        return
