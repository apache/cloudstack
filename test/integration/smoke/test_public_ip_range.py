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
""" P1 tests for Dedicating Public IP addresses
"""
#Import Local Modules
import marvin
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
import datetime

class TestDedicatePublicIPRange(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestDedicatePublicIPRange, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services =  cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.services["zoneid"] = cls.zone.id
        cls.pod = get_pod(cls.apiclient, cls.zone.id)
        # Create Account
        cls.account = Account.create(
                            cls.apiclient,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls._cleanup = [
                        cls.account,
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

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advanced", "publiciprange", "dedicate", "release"], required_hardware="false")
    def test_dedicatePublicIpRange(self):
        """Test public IP range dedication
        """

        # Validate the following:
        # 1. Create a Public IP range
        # 2. Created IP range should be present, verify with listVlanIpRanges
        # 3. Dedicate the created IP range to user account
        # 4. Verify IP range is dedicated, verify with listVlanIpRanges
        # 5. Release the dedicated Public IP range back to the system
        # 6. Verify IP range has been released, verify with listVlanIpRanges
        # 7. Delete the Public IP range

        self.debug("Creating Public IP range")
        self.public_ip_range = PublicIpRange.create(
                                    self.apiclient,
                                    self.services
                               )
        list_public_ip_range_response = PublicIpRange.list(
                                            self.apiclient,
                                            id=self.public_ip_range.vlan.id
                                        )
        self.debug(
                "Verify listPublicIpRanges response for public ip ranges: %s" \
                % self.public_ip_range.vlan.id
            )
        self.assertEqual(
                         isinstance(list_public_ip_range_response, list),
                         True,
                         "Check for list Public IP range response"
                         )
        public_ip_response = list_public_ip_range_response[0]
        self.assertEqual(
                            public_ip_response.id,
                            self.public_ip_range.vlan.id,
                            "Check public ip range response id is in listVlanIpRanges"
                        )

        self.debug("Dedicating Public IP range");
        dedicate_public_ip_range_response = PublicIpRange.dedicate(
                                                self.apiclient,
                                                self.public_ip_range.vlan.id,
                                                account=self.account.name,
                                                domainid=self.account.domainid
                                            )
        list_public_ip_range_response = PublicIpRange.list(
                                            self.apiclient,
                                            id=self.public_ip_range.vlan.id
                                        )
        public_ip_response = list_public_ip_range_response[0]
        self.assertEqual(
                            public_ip_response.account,
                            self.account.name,
                            "Check account name is in listVlanIpRanges as the account public ip range is dedicated to"
                        )

        self.debug("Releasing Public IP range");
        self.public_ip_range.release(self.apiclient)
        list_public_ip_range_response = PublicIpRange.list(
                                            self.apiclient,
                                            id=self.public_ip_range.vlan.id
                                        )
        public_ip_response = list_public_ip_range_response[0]
        self.assertEqual(
                            public_ip_response.account,
                            "system",
                            "Check account name is system account in listVlanIpRanges"
                        )

        self.debug("Deleting Public IP range");
        self.public_ip_range.delete(self.apiclient)

        return

