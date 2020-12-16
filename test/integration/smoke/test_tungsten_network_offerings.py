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
""" BVT tests for tungsten network offerings"""

#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr


_multiprocess_shared_ = True

class TestCreateTungstenNetworkOffering(cloudstackTestCase):

    def setUp(self):
        self.services = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.invalid_tungsten_network_offering = {
            "name": 'Test Tungsten Network offering',
            "displaytext": 'Test Tungsten Network offering',
            "guestiptype": 'Isolated',
            "supportedservices": 'Dhcp,Dns,SourceNat,StaticNat,UserData,Connectivity',
            "traffictype": 'GUEST',
            "useTungsten": 'on',
            "availability": 'Optional',
            "serviceProviderList": {
                "Dhcp": 'Tungsten',
                "Dns": 'VirtualRouter',
                "SourceNat": 'Tungsten',
                "StaticNat": 'Tungsten',
                "UserData": 'Tungsten',
                "Connectivity": 'Tungsten',
            },
        }
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic", "eip", "sg", "advancedns", "smoke"], required_hardware="false")
    def test_01_create_tungsten_network_offering(self):
        """Test to create tungsten network offering"""

        # Validate the following:
        # 1. createNetworkOfferings should return a valid information
        #    for newly created offering
        # 2. The Cloud Database contains the valid information

        network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["tungsten_network_offering"]
        )
        self.cleanup.append(network_offering)

        self.debug(
            "Created tungsten network offering with ID: %s" %
            network_offering.id)

        self.assertEqual(
            network_offering.fortungsten,
            True,
            "Check if the network offering is for tungsten"
        )

        list_network_response = list_network_offerings(
            self.apiclient,
            id=network_offering.id
        )
        self.assertEqual(
            isinstance(list_network_response, list),
            True,
            "Check list response returns a valid list"
        )
        return

    @attr(tags=["advanced", "basic", "eip", "sg", "advancedns", "smoke"], required_hardware="false")
    def test_02_create_tungsten_network_offering_with_invalid_data(self):
        """Test creating a tungsten network offering with invalid data"""

        # Validate the following:
        # 1. createNetworkOfferings should throw an exception since we use a
        #    network offering with invalid data
        with self.assertRaises(Exception):
            network_offering = NetworkOffering.create(
                self.apiclient,
                self.invalid_tungsten_network_offering
            )
        return