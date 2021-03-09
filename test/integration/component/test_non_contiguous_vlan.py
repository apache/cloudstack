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

""" P1 tests for Non contiguous VLAN ranges

    Test Plan: https://cwiki.apache.org/confluence/download/attachments/30760993/Non-Contiguous_VLAN_Ranges_TestPlan.xlsx

    Issue Link: https://issues.apache.org/jira/browse/CLOUDSTACK-2238

    Feature Specifications: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Support+non-contiguous+VLAN+ranges
"""

#Import local modules


from marvin.cloudstackTestCase import (cloudstackTestCase,unittest)
from marvin.lib.base import (Account,
                                         ServiceOffering,
                                         PhysicalNetwork,
                                         VirtualMachine,
                                         )
from marvin.lib.common import (get_zone,
                                           get_pod,
                                           get_domain,
                                           get_template,
                                           setNonContiguousVlanIds)
from marvin.lib.utils import (cleanup_resources,
                                          xsplit)

from nose.plugins.attrib import attr

class Services():
    def __init__(self):
        self.services = {

            "vlan_nc":             {
                                 "partial_range": ["",""],
                                 "full_range": "",
                                },
            "account":          {
                                 "email": "test@test.com",
                                 "firstname": "Test",
                                 "lastname": "User",
                                 "username": "test",
                                 # Random characters are appended in create account to
                                 # ensure unique username generated each time
                                 "password": "password",
                                },
            "virtual_machine":  {
                                 "displayname": "testserver",
                                 "username": "root",     # VM creds for SSH
                                 "password": "password",
                                 "ssh_port": 22,
                                 "hypervisor": 'XenServer',
                                 "privateport": 22,
                                 "publicport": 22,
                                 "protocol": 'TCP',
                                },
            "service_offering": {
                                 "name": "Tiny Instance",
                                 "displaytext": "Tiny Instance",
                                 "cpunumber": 1,
                                 "cpuspeed": 100,    # in MHz
                                 "memory": 128,      # In MBs
                                },

            "ostype":            'CentOS 5.6 (64-bit)',
                        }


@attr(tags = ["simulator", "advanced"])
class TestNonContiguousVLANRanges(cloudstackTestCase):
    """
    Test to add non contiguous vlan ranges into existing physical network
    """
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestNonContiguousVLANRanges, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.pod = get_pod(cls.api_client, cls.zone.id)
        cls.domain = get_domain(cls.api_client)

        cls.service_offering = ServiceOffering.create(
                                    cls.api_client,
                                    cls.services["service_offering"]
                                    )

        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )

        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls._cleanup = [cls.service_offering]

        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.vlan = self.services["vlan_nc"]
        self.apiClient = self.testClient.getApiClient()

        self.physicalnetwork, self.vlan = setNonContiguousVlanIds(self.apiclient, self.zone.id)

        self.physicalnetworkid = self.physicalnetwork.id
        self.existingvlan = self.physicalnetwork.vlan
        
        if self.vlan == None:
            self.fail("Failed to set non contiguous vlan ids to test. Free some ids from \
                        from existing physical networks at extreme ends")
        self.cleanup = []

    def tearDown(self):
        """
        Teardown to update a physical network and shrink its vlan
        Cleanup all used resource
        """
        self.physicalnetwork.update(self.apiClient, id = self.physicalnetworkid, vlan=self.existingvlan)

        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def validatePhysicalNetworkVlan(self, physicalNetworkId, vlan):
        """Validate whether the physical network has the updated vlan

        params:

        @physicalNetworkId: The id of physical network which needs to be validated
        @vlan: vlan with which physical network was updated. This should match with the vlan of listed
               physical network

        Raise Exception if not matched
        """

        self.debug("Listing physical networks with id: %s" % physicalNetworkId)

        physicalnetworks = PhysicalNetwork.list(self.apiclient, id=physicalNetworkId)

        self.assertTrue(isinstance(physicalnetworks, list), "PhysicalNetwork.list should return a \
                        valid list object")

        self.assertTrue(len(physicalnetworks) > 0, "physical networks list should not be empty")

        self.debug("Checking if physical network vlan matches with the passed vlan")

        vlans = xsplit(vlan,[','])

        for virtualLan in vlans:
            self.assertTrue(physicalnetworks[0].vlan.find(virtualLan) != -1, "vlan range %s \
                        is not present in physical network: %s" % (virtualLan, physicalNetworkId))

        return

    @attr(tags = ["simulator", "advanced", "dvs"])
    def test_01_add_non_contiguous_ranges(self):
        """
        Test adding different non contiguous vlan ranges
        """
        # 1. Add new non contiguous vlan-range in addition to existing range
        # 2. Add another non contiguous range
        # 3. Both the ranges should get added successfully

        vlan1 = self.existingvlan + "," + self.vlan["partial_range"][0]
        updatePhysicalNetworkResponse = self.physicalnetwork.update(self.apiClient, id = self.physicalnetworkid, vlan = vlan1)

        self.assertTrue(updatePhysicalNetworkResponse is not None,
            msg="couldn't add non contiguous range in the physical network with vlan %s"%vlan1)

        self.debug("Verifying the VLAN of the updated physical network: %s, It should match with \
                    the passed vlan: %s" % (self.physicalnetworkid,vlan1))

        self.validatePhysicalNetworkVlan(self.physicalnetworkid, vlan1)

        vlan2 = vlan1 + "," + self.vlan["partial_range"][1]
        updatePhysicalNetworkResponse2 = self.physicalnetwork.update(self.apiClient, id = self.physicalnetworkid, vlan = vlan2)

        self.assertTrue(updatePhysicalNetworkResponse2 is not None,
            msg="couldn't add non contiguous range in the physical network with vlan %s"%vlan2)

        self.debug("Verifying the VLAN of the updated physical network: %s, It should match with \
                    the passed vlan: %s" % (self.physicalnetworkid,vlan2))

        self.validatePhysicalNetworkVlan(self.physicalnetworkid, vlan2)

        return

    @attr(tags = ["simulator", "advanced", "dvs"])
    def test_02_add_existing_vlan_range(self):
        """
        Test adding same non contiguous range twice
        """
        # 1. Add non contiguous range to existing range
        # 2. Add the same range again
        # 3. It should get added successfully

        vlan1 = self.existingvlan+","+self.vlan["partial_range"][0]
        self.physicalnetwork.update(self.apiClient, id = self.physicalnetworkid, vlan = vlan1)

        self.debug("Updating physical network with same vlan range" )
        self.physicalnetwork.update(self.apiClient, id = self.physicalnetworkid, vlan = vlan1)

        self.debug("Verifying the VLAN of the updated physical network: %s, It should match with \
                    the passed vlan: %s" % (self.physicalnetworkid,vlan1))

        self.validatePhysicalNetworkVlan(self.physicalnetworkid, vlan1)

        return

    @attr(tags = ["simulator", "advanced", "dvs"])
    def test_03_extend_contiguous_range(self):
        """
        Test adding non contiguous range and extend it
        """

        # 1. Add new non contiguous range
        # 2. Add new range which extends previously added range
        # 3. Newly added range should get extended successfully

        vlan1 = self.existingvlan + "," + self.vlan["partial_range"][0]
        self.physicalnetwork.update(self.apiClient, id = self.physicalnetworkid, vlan = vlan1)

        vlan2 = vlan1 + "," + self.vlan["full_range"]
        updatePhysicalNetworkResponse = self.physicalnetwork.update(self.apiClient, id = self.physicalnetworkid, vlan = vlan2)

        self.assertTrue(updatePhysicalNetworkResponse is not None,
            msg="couldn't extend the physical network with vlan %s"%vlan2)

        extendedvlan = self.existingvlan + "," + self.vlan["full_range"]

        self.debug("Verifying the VLAN of the updated physical network: %s, It should match with \
                    the extended vlan: %s" % (self.physicalnetworkid, extendedvlan))

        self.validatePhysicalNetworkVlan(self.physicalnetworkid, extendedvlan)

        return

    @attr(tags = ["simulator", "advanced", "dvs"])
    def test_04_remove_unused_range(self):
        """
        Test removing unused vlan range
        """
        # 1. Add new non contiguous range to existing vlan range
        # 2. Remove unused vlan range
        # 3. Unused vlan range should gte removed successfully

        vlan1 = self.existingvlan+","+self.vlan["partial_range"][0]
        self.physicalnetwork.update(self.apiClient, id = self.physicalnetworkid, vlan = vlan1)

        self.debug("Removing vlan : %s" % self.vlan["partial_range"][0])

        self.physicalnetwork.update(self.apiClient, id = self.physicalnetworkid, vlan = self.existingvlan)

        physicalnetworks = PhysicalNetwork.list(self.apiclient, id=self.physicalnetworkid)

        self.assertTrue(isinstance(physicalnetworks, list), "PhysicalNetwork.list should return a \
                        valid list object")

        self.assertTrue(len(physicalnetworks) > 0, "physical networks list should not be empty")

        vlanranges= physicalnetworks[0].vlan

        self.assertTrue(vlanranges.find(self.vlan["partial_range"][0]) == -1, "vlan range is not removed")

        return

    @attr(tags = ["simulator", "advanced", "dvs"])
    def test_05_remove_used_range(self):
        """
        Test removing used vlan range
        """
        # 1. Use a vlan id from existing range by deploying an instance which
        #    will create a network with vlan id from this range
        # 4. Now try to remove this vlan range
        # 5. Vlan range should not get removed, should throw error

        account = Account.create(self.apiclient,self.services["account"],
                                 domainid=self.domain.id)

        self.debug("Deploying instance in the account: %s" % account.name)

        try:

            self.virtual_machine = VirtualMachine.create(self.apiclient,self.services["virtual_machine"],
                                                     accountid=account.name,domainid=account.domainid,
                                                     serviceofferingid=self.service_offering.id,
                                                     mode=self.zone.networktype)
            self.debug("Deployed instance in account: %s" % account.name)
            self.debug("Trying to remove vlan range : %s , This should fail" % self.vlan["partial_range"][0])

            with self.assertRaises(Exception) as e:
                self.physicalnetwork.update(self.apiClient, id = self.physicalnetworkid, vlan = self.vlan["partial_range"][0])

            self.debug("operation failed with exception: %s" % e.exception)
            account.delete(self.apiclient)

        except Exception as e:
            self.fail("Exception in test case: %s" % e)

        return
