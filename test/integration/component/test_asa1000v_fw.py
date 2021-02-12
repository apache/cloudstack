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

""" Cisco ASA1000v external firewall
"""
#Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (NetworkOffering,
                             PhysicalNetwork,
                             VNMC,
                             ASA1000V,
                             Cluster)
from marvin.lib.common import get_zone


class Services:
    """Test Cisco ASA1000v services
    """

    def __init__(self):
        self.services = {
                        "vnmc": {
                                    "ipaddress": '10.147.28.236',
                                    "username": 'admin',
                                    "password": 'Password_123',
                        },
                        "asa": {
                                    "ipaddress": '10.147.28.238',
                                    "insideportprofile": 'asa-in123',
                        },
                        "network_offering": {
                                    "name": 'CiscoVnmc',
                                    "displaytext": 'CiscoVnmc',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Firewall,UserData,StaticNat',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "serviceProviderList": {
                                            "Dhcp": 'VirtualRouter',
                                            "Dns": 'VirtualRouter',
                                            "SourceNat": 'CiscoVnmc',
                                            "PortForwarding": 'CiscoVnmc',
                                            "Firewall": 'CiscoVnmc',
                                            "UserData": 'VirtualRouter',
                                            "StaticNat": 'CiscoVnmc',
                                    },
                        },
                        "network": {
                                    "name": "CiscoVnmc",
                                    "displaytext": "CiscoVnmc",
                        },
                    }

class TestASASetup(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.apiclient = super(
                            TestASASetup,
                            cls
                            ).getClsTestClient().getApiClient()
        cls.services = Services().services
        cls.network_offering = NetworkOffering.create(
                            cls.apiclient,
                            cls.services["network_offering"],
                            conservemode=True)
        # Enable network offering
        cls.network_offering.update(cls.apiclient, state='Enabled')

        cls._cleanup = [
                        cls.network_offering,
                      ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.physicalnetworks = PhysicalNetwork.list(self.apiclient, zoneid=self.zone.id)
        self.assertNotEqual(len(self.physicalnetworks), 0, "Check if the list physical network API returns a non-empty response")
        self.clusters = Cluster.list(self.apiclient, hypervisor='VMware')
        self.assertNotEqual(len(self.clusters), 0, "Check if the list cluster API returns a non-empty response")

        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["device", "asa"])
    def test_registerVnmc(self):
        Vnmc = VNMC.create(self.apiclient, self.services["vnmc"]["ipaddress"], self.services["vnmc"]["username"], self.services["vnmc"]["password"], self.physicalnetworks[0].id)
        self.debug("Cisco VNMC appliance with id %s deployed"%(Vnmc.id))
        VnmcList = VNMC.list(self.apiclient, physicalnetworkid = self.physicalnetworks[0].id)
        self.assertNotEqual(len(VnmcList), 0, "List VNMC API returned an empty response")
        Vnmc.delete(self.apiclient)

    @attr(tags=["device", "asa"])
    def test_registerAsa1000v(self):
        Asa = ASA1000V.create(self.apiclient, self.services["asa"]["ipaddress"], self.services["asa"]["insideportprofile"], self.clusters[0].id, self.physicalnetworks[0].id)
        self.debug("Cisco ASA 1000v appliance with id %s deployed"%(Asa.id))
        AsaList = ASA1000V.list(self.apiclient, physicalnetworkid = self.physicalnetworks[0].id)
        self.assertNotEqual(len(AsaList), 0, "List ASA 1000v API returned an empty response")
        Asa.delete(self.apiclient)
