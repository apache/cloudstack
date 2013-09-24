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
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.integration.lib.base import Account
from marvin.integration.lib.base import PhysicalNetwork
from marvin.integration.lib.common import *
from nose.plugins.attrib import attr

class Services():
    def __init__(self):
        self.services = {

            "vlan":             {
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
        cls.api_client = super(TestNonContiguousVLANRanges, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, pod, domain
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.pod = get_pod(cls.api_client, cls.zone.id, cls.services)
        cls.domain = get_domain(cls.api_client, cls.services)

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
        self.vlan = self.services["vlan"]
        self.apiClient = self.testClient.getApiClient()

        self.setNonContiguousVlanIds(self.apiclient, self.zone.id)

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

    def setNonContiguousVlanIds(self, apiclient, zoneid):
        """
        Form the non contiguous ranges based on currently assigned range in physical network
        """

        NonContigVlanIdsAcquired = False

        list_physical_networks_response = PhysicalNetwork.list(
            apiclient,
            zoneid=zoneid
        )
        assert isinstance(list_physical_networks_response, list)
        assert len(list_physical_networks_response) > 0, "No physical networks found in zone %s" % zoneid

        for physical_network in list_physical_networks_response:

            self.physicalnetwork = physical_network
            self.physicalnetworkid = physical_network.id
            self.existingvlan = physical_network.vlan

            vlans = xsplit(self.existingvlan, ['-', ','])

            assert len(vlans) > 0
            assert int(vlans[0]) < int(vlans[-1]), "VLAN range  %s was improperly split" % self.existingvlan

            # Keep some gap between existing vlan and the new vlans which we are going to add
            # So that they are non contiguous

            non_contig_end_vlan_id = int(vlans[-1]) + 6
            non_contig_start_vlan_id = int(vlans[0]) - 6

            # Form ranges which are consecutive to existing ranges but not immediately contiguous
            # There should be gap in between existing range and new non contiguous ranage

            # If you can't add range after existing range, because it's crossing 4095, then
            # select VLAN ids before the existing range such that they are greater than 0, and
            # then add this non contiguoud range

            if non_contig_end_vlan_id < 4095:

                self.vlan["partial_range"][0] = str(non_contig_end_vlan_id - 4) + '-' + str(non_contig_end_vlan_id - 3)
                self.vlan["partial_range"][1] = str(non_contig_end_vlan_id - 1) + '-' + str(non_contig_end_vlan_id)
                self.vlan["full_range"] = str(non_contig_end_vlan_id - 4) + '-' + str(non_contig_end_vlan_id)
                NonContigVlanIdsAcquired = True

            elif non_contig_start_vlan_id > 0:

                self.vlan["partial_range"][0] = str(non_contig_start_vlan_id) + '-' + str(non_contig_start_vlan_id + 1)
                self.vlan["partial_range"][1] = str(non_contig_start_vlan_id + 3) + '-' + str(non_contig_start_vlan_id + 4)
                self.vlan["full_range"] = str(non_contig_start_vlan_id) + '-' + str(non_contig_start_vlan_id + 4)
                NonContigVlanIdsAcquired = True

            else:
               NonContigVlanIdsAcquired = False

            # If failed to get relevant vlan ids, continue to next physical network
            # else break from loop as we have hot the non contiguous vlan ids for the test purpose

            if not NonContigVlanIdsAcquired:
                continue
            else:
                break

        # If even through looping from all existing physical networks, failed to get relevant non
        # contiguous vlan ids, then fail the test case

        if not NonContigVlanIdsAcquired:
            self.fail("Failed to set non contiguous vlan ids to test. Free some ids from \
                        from existing physical networks at extreme ends")

        return

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
            self.assert_(physicalnetworks[0].vlan.find(virtualLan) != -1, "vlan range %s \
                        is not present in physical network: %s" % (virtualLan, physicalNetworkId))

        return

    @attr(tags = ["simulator", "advanced"])
    def test_01_add_non_contiguous_ranges(self):
        """
        Test adding different non contiguous vlan ranges
        """
        # 1. Add new non contiguous vlan-range in addition to existing range
        # 2. Add another non contiguous range
        # 3. Both the ranges should get added successfully

        vlan1 = self.existingvlan + "," + self.vlan["partial_range"][0]
        updatePhysicalNetworkResponse = self.physicalnetwork.update(self.apiClient, id = self.physicalnetworkid, vlan = vlan1)

        self.assert_(updatePhysicalNetworkResponse is not None,
            msg="couldn't add non contiguous range in the physical network with vlan %s"%vlan1)

        self.debug("Verifying the VLAN of the updated physical network: %s, It should match with \
                    the passed vlan: %s" % (self.physicalnetworkid,vlan1))

        self.validatePhysicalNetworkVlan(self.physicalnetworkid, vlan1)

        vlan2 = vlan1 + "," + self.vlan["partial_range"][1]
        updatePhysicalNetworkResponse2 = self.physicalnetwork.update(self.apiClient, id = self.physicalnetworkid, vlan = vlan2)

        self.assert_(updatePhysicalNetworkResponse2 is not None,
            msg="couldn't add non contiguous range in the physical network with vlan %s"%vlan2)

        self.debug("Verifying the VLAN of the updated physical network: %s, It should match with \
                    the passed vlan: %s" % (self.physicalnetworkid,vlan2))

        self.validatePhysicalNetworkVlan(self.physicalnetworkid, vlan2)

        return

    @attr(tags = ["simulator", "advanced"])
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

    @attr(tags = ["simulator", "advanced"])
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

        self.assert_(updatePhysicalNetworkResponse is not None,
            msg="couldn't extend the physical network with vlan %s"%vlan2)

        extendedvlan = self.existingvlan + "," + self.vlan["full_range"]

        self.debug("Verifying the VLAN of the updated physical network: %s, It should match with \
                    the extended vlan: %s" % (self.physicalnetworkid, extendedvlan))

        self.validatePhysicalNetworkVlan(self.physicalnetworkid, extendedvlan)

        return

    @attr(tags = ["simulator", "advanced"])
    def test_04_remove_unused_range(self):
        """
        Test removing unused vlan range
        """
        # 1. Add new non contiguous range to existing vlan range
        # 2. Remove unused vlan range
        # 3. Unused vlan range should gte removed successfully

        vlan1 = self.existingvlan+","+self.vlan["partial_range"][0]
        self.physicalnetwork.update(self.apiClient, id = self.physicalnetworkid, vlan = vlan1)

        vlan2 = vlan1+","+self.vlan["partial_range"][1]
        self.physicalnetwork.update(self.apiClient, id = self.physicalnetworkid, vlan = vlan2)

        self.debug("Removing vlan : %s" % self.vlan["partial_range"][1])

        self.physicalnetwork.update(self.apiClient, id = self.physicalnetworkid, vlan = vlan1)

        physicalnetworks = PhysicalNetwork.list(self.apiclient, id=self.physicalnetworkid)

        self.assertTrue(isinstance(physicalnetworks, list), "PhysicalNetwork.list should return a \
                        valid list object")

        self.assertTrue(len(physicalnetworks) > 0, "physical networks list should not be empty")

        vlanranges= physicalnetworks[0].vlan

        self.assert_(vlanranges.find(self.vlan["partial_range"][1]) == -1, "vlan range is not removed")

        return

    @attr(tags = ["simulator", "advanced"])
    def test_05_remove_used_range(self):
        """
        Test removing used vlan range
        """
        # 1. If vlan id from existing range is in use, try to delete this range and add different range,
        #    this operation should fail
        # 2. If any of existing vlan id is not in use, delete this range and add new vlan range
        # 3. Use a vlan id from this new range by deploying an instance which
        #    will create a network with vlan id from this range
        # 4. Now try to remove this vlan range
        # 5. Vlan range should not get removed, should throw error

        vlans = xsplit(self.existingvlan, ['-', ','])
        vlanstartid = int(vlans[0])
        vlanendid = int(vlans[1])

        networks = list_networks(self.apiclient)
        existingvlaninuse = False


        # Check if any of the vlan id from existing range is in use
        if isinstance(networks,list) and len(networks) > 0:

            self.debug("networks: %s" % networks)

            vlansinuse = [network for network in networks if network.vlan and (vlanstartid <= int(network.vlan) <= vlanendid)]

            self.debug("Total no. of vlans in use : %s" % len(vlansinuse))

            if len(vlansinuse) > 0:
                existingvlaninuse = True
            else:
                existingvlaninuse = False

        vlan1 = self.vlan["partial_range"][0]

        # If existing vlan id is in use, then try to delete this range, the operation should fail
        # This serves the test case purpose, hence test case has completed successfully
        if existingvlaninuse:
            self.debug("Trying to remove existing vlan in use, This should fail")
            with self.assertRaises(Exception) as e:
                self.physicalnetwork.update(self.apiClient, id = self.physicalnetworkid, vlan = vlan1)

            self.debug("operation failed with exception: %s" % e.exception)

        # If any of the existing vlan id is not in use, then add new range and deploy an instance which
        # will create a network using vlan id from this new range, hence now the new range is in use
        # Now try to delete this new range and add another range, operation should fail
        # This serves the test case purpose, hence test case has completed successfully
        else:

            self.debug("No vlan in use, hence adding a new vlan and using it by deploying an instance")

            self.physicalnetwork.update(self.apiClient, id = self.physicalnetworkid, vlan = vlan1)

            self.debug("Verifying the VLAN of the updated physical network: %s, It should match with \
                    the passed vlan: %s" % (self.physicalnetworkid,vlan1))

            self.validatePhysicalNetworkVlan(self.physicalnetworkid, vlan1)

            account = Account.create(
                            self.apiclient,
                            self.services["account"],
                            domainid=self.domain.id
                            )

            self.debug("Deploying instance in the account: %s" %
                                                account.name)

            self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=account.name,
                                    domainid=account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    mode=self.zone.networktype
                                )

            self.debug("Deployed instance in account: %s" %
                                                    account.name)



            self.debug("Trying to remove vlan range : %s , This should fail" % self.vlan["partial_range"][0])

            with self.assertRaises(Exception) as e:
                self.physicalnetwork.update(self.apiClient, id = self.physicalnetworkid, vlan = self.existingvlan)

            self.debug("operation failed with exception: %s" % e.exception)

            account.delete(self.apiclient)

        return
