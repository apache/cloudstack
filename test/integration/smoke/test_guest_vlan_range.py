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
""" P1 tests for Dedicating Guest Vlan Ranges
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

class TestDedicateGuestVlanRange(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDedicateGuestVlanRange, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())

        # Create Account
        cls.account = Account.create(
                            cls.apiclient,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls._cleanup = [
                        cls.account,
                        ]

        cls.physical_network, cls.free_vlan = setNonContiguousVlanIds(cls.apiclient, cls.zone.id)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            removeGuestVlanRangeResponse = \
                cls.physical_network.update(cls.apiclient,
                        id=cls.physical_network.id,
                        vlan=cls.physical_network.vlan)
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

    @attr(tags=["advanced", "guestvlanrange", "dedicate", "release"], required_hardware="false")
    def test_dedicateGuestVlanRange(self):
        """Test guest vlan range dedication
        """

        """Assume a physical network is available
        """
        """
        # Validate the following:
        # 1. List the available physical network using ListPhysicalNetwork
        # 2. Add a Guest Vlan range to the available physical network using UpdatePhysicalNetwork
        # 3. Dedicate the created guest vlan range to user account using DedicateGuestVlanRange
        # 4. Verify vlan range is dedicated with listDedicatedGuestVlanRanges
        # 5. Release the dedicated guest vlan range back to the system
        # 6. Verify guest vlan range has been released, verify with listDedicatedGuestVlanRanges
        # 7. Remove the added guest vlan range using UpdatePhysicalNetwork
        """
        self.debug("Adding guest vlan range")

        new_vlan = self.physical_network.vlan + "," + self.free_vlan["partial_range"][0]
        #new_vlan = self.free_vlan["partial_range"][0]
        addGuestVlanRangeResponse = self.physical_network.update(self.apiclient,
                id=self.physical_network.id, vlan=new_vlan)
                #id=self.physical_network.id, vlan=self.free_vlan["partial_range"][0])

        self.debug("Dedicating guest vlan range");
        dedicate_guest_vlan_range_response = PhysicalNetwork.dedicate(
                                                self.apiclient,
                                                self.free_vlan["partial_range"][0],
                                                physicalnetworkid=self.physical_network.id,
                                                account=self.account.name,
                                                domainid=self.account.domainid
                                            )
        list_dedicated_guest_vlan_range_response = PhysicalNetwork.listDedicated(
                                                self.apiclient,
                                                id=dedicate_guest_vlan_range_response.id
                                        )
        dedicated_guest_vlan_response = list_dedicated_guest_vlan_range_response[0]
        self.assertEqual(
                            dedicated_guest_vlan_response.account,
                            self.account.name,
                            "Check account name is in listDedicatedGuestVlanRanges as the account the range is dedicated to"
                        )

        self.debug("Releasing guest vlan range");
        dedicate_guest_vlan_range_response.release(self.apiclient)
        list_dedicated_guest_vlan_range_response = PhysicalNetwork.listDedicated(self.apiclient)
        self.assertEqual(
                        list_dedicated_guest_vlan_range_response,
                        None,
                        "Check vlan range is not available in listDedicatedGuestVlanRanges"

                        )                    
