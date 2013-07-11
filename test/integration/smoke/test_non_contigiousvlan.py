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

from marvin import cloudstackTestCase
from marvin.cloudstackAPI import *
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.integration.lib.base import Account
from marvin.integration.lib.base import PhysicalNetwork
from nose.plugins.attrib import attr

class Services():
    def __init__(self):
        self.services = {
            "vlan": {
                "part": ["4090-4091", "4092-4095"],
                "full": "4090-4095",
            }
        }


@attr(tags = ["simulator", "advanced"])
class TestUpdatePhysicalNetwork(cloudstackTestCase):
    """
    Test to extend physical network vlan range
    """
    def setUp(self):
        self.vlan = Services().services["vlan"]
        self.apiClient = self.testClient.getApiClient()


    def test_extendPhysicalNetworkVlan(self):
        """
        Test to update a physical network and extend its vlan
        """
        phy_networks = PhysicalNetwork.list(self.apiClient)
        self.assertNotEqual(len(phy_networks), 0,
            msg="There are no physical networks in the zone")

        self.network = phy_networks[0]
        self.networkid = phy_networks[0].id
        vlan1 = self.vlan["part"][0]
        updatePhysicalNetworkResponse = self.network.update(self.apiClient, id = self.networkid, vlan = vlan1)
        self.assert_(updatePhysicalNetworkResponse is not None,
            msg="couldn't extend the physical network with vlan %s"%vlan1)
        self.assert_(isinstance(self.network, PhysicalNetwork))

        vlan2 = self.vlan["part"][1]
        updatePhysicalNetworkResponse2 = self.network.update(self.apiClient, id = self.networkid, vlan = vlan2)
        self.assert_(updatePhysicalNetworkResponse2 is not None,
            msg="couldn't extend the physical network with vlan %s"%vlan2)
        self.assert_(isinstance(self.network, PhysicalNetwork))

        vlanranges= updatePhysicalNetworkResponse2.vlan
        self.assert_(vlanranges is not None,
            "No VLAN ranges found on the deployment")
        self.assert_(vlanranges.find(self.vlan["full"]) > 0, "vlan ranges are not extended")


    def tearDown(self):
        """
        Teardown to update a physical network and shrink its vlan
        @return:
        """
        phy_networks = PhysicalNetwork.list(self.apiClient)
        self.assertNotEqual(len(phy_networks), 0,
            msg="There are no physical networks in the zone")
        self.network = phy_networks[0]
        self.networkid = phy_networks[0].id
        updateResponse = self.network.update(self.apiClient, id = self.networkid, removevlan = self.vlan["full"])
        self.assert_(updateResponse.vlan.find(self.vlan["full"]) < 0,
            "VLAN was not removed successfully")

