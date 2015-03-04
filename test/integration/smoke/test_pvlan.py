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
""" test for private vlan isolation
"""
#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.sshClient import SshClient
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr
import telnetlib

#Import System modules
import time
_multiprocess_shared_ = True

class TestPVLAN(cloudstackTestCase):

    zoneId = 1
    networkOfferingId = 7
    vlan = 1234
    isolatedpvlan = 567

    def setUp(self):
        self.apiClient = self.testClient.getApiClient()

    @attr(tags = ["advanced"], required_hardware="false")
    def test_create_pvlan_network(self):
        self.debug("Test create pvlan network")
        createNetworkCmd = createNetwork.createNetworkCmd()
        createNetworkCmd.name = "pvlan network"
        createNetworkCmd.displaytext = "pvlan network"
        createNetworkCmd.netmask = "255.255.255.0"
        createNetworkCmd.gateway = "10.10.10.1"
        createNetworkCmd.startip = "10.10.10.10"
        createNetworkCmd.gateway = "10.10.10.20"
        createNetworkCmd.vlan = "1234"
        createNetworkCmd.isolatedpvlan = "567"
        createNetworkCmd.zoneid = self.zoneId
        createNetworkCmd.networkofferingid = self.networkOfferingId
        createNetworkResponse = self.apiClient.createNetwork(createNetworkCmd)
        self.networkId = createNetworkResponse.id
        self.broadcasttype = createNetworkResponse.broadcastdomaintype
        self.broadcasturi = createNetworkResponse.broadcasturi

        self.assertIsNotNone(createNetworkResponse.id, "Network failed to create")
        self.assertTrue(createNetworkResponse.broadcastdomaintype, "Pvlan")
        self.assertTrue(createNetworkResponse.broadcasturi, "pvlan://1234-i567")

        self.debug("Clean up test pvlan network")
        deleteNetworkCmd = deleteNetwork.deleteNetworkCmd()
        deleteNetworkCmd.id = self.networkId;
        self.apiClient.deleteNetwork(deleteNetworkCmd)

        #Test invalid parameter

        # CLOUDSTACK-2392: Should not allow create pvlan with ipv6
        createNetworkCmd.ip6gateway="fc00:1234::1"
        createNetworkCmd.ip6cidr="fc00:1234::/64"
        createNetworkCmd.startipv6="fc00:1234::10"
        createNetworkCmd.endipv6="fc00:1234::20"
        err = 0
        with self.assertRaises(Exception):
            self.apiClient.createNetwork(createNetworkCmd)
