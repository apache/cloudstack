#!/usr/bin/env python
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

from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr

class TestPortablePublicIPRange(cloudstackTestCase):

    """
    This test validates functionality where
      - admin can provision a portable public ip range
      - list provisioned portable public ip range
      - delete provisioned portable public ip range
    """
    @classmethod
    def setUpClass(cls):
        testClient = super(TestPortablePublicIPRange, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())

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

    @attr(tags = ["basic", "advanced",  "portablepublicip"], required_hardware="false")
    def test_createPortablePublicIPRange(self):
        """ Test to create a portable public ip range
        """
        self.debug("attempting to create a portable Public IP range")
        self.portable_ip_range = PortablePublicIpRange.create(
                                    self.apiclient,
                                    self.services
                               )
        self.debug("attempting to verify portable Public IP range is created")
        list_portbale_ip_range_response = PortablePublicIpRange.list(
                                            self.apiclient,
                                            id=self.portable_ip_range.id
                                        )
        self.portable_ip_range.delete(self.apiclient)
        return


class TestPortablePublicIPAcquire(cloudstackTestCase):
    """
    This test validates functionality where
      - admin has provisioned a portable public ip range
      - user can acquire portable ip from the provisioned ip range
    """
    @classmethod
    def setUpClass(cls):
        testClient = super(TestPortablePublicIPAcquire, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        # Create Account
        cls.account = Account.create(
                            cls.apiclient,
                            cls.services["account"],
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
        cls.account_network = Network.create(
                                             cls.apiclient,
                                             cls.services["network"],
                                             cls.account.name,
                                             cls.account.domainid
                                             )
        cls._cleanup = [
                        cls.account_network,
                        cls.network_offering,
                        cls.account
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

    @attr(tags = ["advanced",  "portablepublicip"], required_hardware="false")
    def test_createPortablePublicIPAcquire(self):
        """ Test to acquire a provisioned public ip range
        """
        self.debug("attempting to create a portable Public IP range")
        self.portable_ip_range = PortablePublicIpRange.create(
                                    self.apiclient,
                                    self.services
                                    )

        ip_address = PublicIPAddress.create(self.apiclient, self.account.name,
                            self.zone.id, self.account.domainid, isportable=True)

        ip_address.delete(self.apiclient)
        self.portable_ip_range.delete(self.apiclient)
        return
