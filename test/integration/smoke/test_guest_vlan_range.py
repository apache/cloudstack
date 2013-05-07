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
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
import datetime


class Services:
    """Test Dedicating Guest Vlan Ranges
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
                        "name": "testphysicalnetwork",
                        "vlan": "2118-2120",
                    }


class TesDedicateGuestVlanRange(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TesDedicateGuestVlanRange, cls).getClsTestClient().getApiClient()
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
            list_physical_network_response = PhysicalNetwork.list(cls.api_client)
            if list_physical_network_response is not None and len(list_physical_network_response) > 0:
                physical_network = list_physical_network_response[0]
                removeGuestVlanRangeResponse = \
                physical_network.update(cls.api_client,
                        id=physical_network.id,
                        removevlan=cls.services["vlan"])
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

    @attr(tags=["simulator", "advanced", "guestvlanrange", "dedicate", "release"])
    def test_dedicateGuestVlanRange(self):
        """Test guest vlan range dedication
        """

        """Assume a physical network is available
        """
        # Validate the following:
        # 1. List the available physical network using ListPhysicalNetwork
        # 2. Add a Guest Vlan range to the available physical network using UpdatePhysicalNetwork
        # 3. Dedicate the created guest vlan range to user account using DedicateGuestVlanRange
        # 4. Verify vlan range is dedicated with listDedicatedGuestVlanRanges
        # 5. Release the dedicated guest vlan range back to the system
        # 6. Verify guest vlan range has been released, verify with listDedicatedGuestVlanRanges
        # 7. Remove the added guest vlan range using UpdatePhysicalNetwork

        self.debug("Listing available physical network")
        list_physical_network_response = PhysicalNetwork.list(
                             self.apiclient
                             )
        self.assertEqual(
                         isinstance(list_physical_network_response, list),
                         True,
                         "Check for list guest vlan range response"
                         )
        physical_network_response = list_physical_network_response[0]

        self.debug("Adding guest vlan range")
        addGuestVlanRangeResponse = physical_network_response.update(self.apiclient, id=physical_network_response.id, vlan=self.services["vlan"])

        self.debug("Dedicating guest vlan range");
        dedicate_guest_vlan_range_response = PhysicalNetwork.dedicate(
                                                self.apiclient,
                                                self.services["vlan"],
                                                physicalnetworkid=physical_network_response.id,
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
        dedicated_guest_vlan_response.release(self.apiclient)
        list_dedicated_guest_vlan_range_response = PhysicalNetwork.listDedicated(
                                                self.apiclient,
                                                id=dedicate_guest_vlan_range_response.id
                                        )
        dedicated_guest_vlan_response = list_dedicated_guest_vlan_range_response[0]
        self.assertEqual(
                            dedicated_guest_vlan_response.account,
                            "system",
                            "Check account name is system account in listDedicatedGuestVlanRanges"
                        )
        
