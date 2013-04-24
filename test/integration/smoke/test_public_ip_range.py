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
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
import datetime

class Services:
    """Test Dedicating Public IP addresses
    """

    def __init__(self):
        self.services = {
                        "domain": {
                                   "name": "Domain",
                                   },
                        "account": {
                                    "email": "test@test.com",
                                    "firstname": "Test",
                                    "lastname": "User",
                                    "username": "test",
                                    "password": "password",
                         },
                        "gateway": "10.102.197.1",
                        "netmask": "255.255.255.0",
                        "forvirtualnetwork": "true",
                        "startip": "10.102.197.70",
                        "endip": "10.102.197.73",
                        "zoneid": "1",
                        "podid": "",
                        "vlan": "4444",
                    }

class TesDedicatePublicIPRange(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TesDedicatePublicIPRange, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)

        # Create Account
        cls.account = Account.create(
                            cls.api_client,
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
            cleanup_resources(cls.api_client, cls._cleanup)
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

    @attr(tags = ["simulator", "publiciprange", "dedicate", "release"])
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
                                    self.api_client,
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

