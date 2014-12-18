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
""" P1 tests for Dedicating guest VLAN ranges

    Test Plan: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Dedicated+Resources+-+Public+IP+Addresses+and+VLANs+per+Tenant+Test+Plan

    Issue Link: https://issues.apache.org/jira/browse/CLOUDSTACK-2251

    Feature Specifications: https://cwiki.apache.org/confluence/display/CLOUDSTACK/FS-+Dedicate+Guest+VLANs+per+tenant
"""
#Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.utils import (validateList,
                              cleanup_resources,
                              random_gen,
			                  xsplit)
from marvin.lib.base import (Account,
                             Domain,
                             PhysicalNetwork,
                             NetworkOffering,
                             Network,
                             ServiceOffering,
                             Project)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               setNonContiguousVlanIds,
                               isNetworkDeleted)
from marvin.codes import PASS

def LimitVlanRange(self, vlanrange, range=2):
    """Limits the length of vlan range"""
    vlan_endpoints = str(vlanrange).split("-")
    vlan_startid = int(vlan_endpoints[1])
    vlan_endid = vlan_startid + (range-1)
    return str(vlan_startid) + "-" + str(vlan_endid)

class TestDedicateGuestVLANRange(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestDedicateGuestVLANRange, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.testdata =  cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient)
        cls.testdata["isolated_network"]["zoneid"] = cls.zone.id
        cls.testdata['mode'] = cls.zone.networktype
        template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata["ostype"]
            )
        cls._cleanup = []

        try:
            cls.isolated_network_offering = NetworkOffering.create(
                          cls.apiclient,
                          cls.testdata["nw_off_isolated_persistent"])
            cls._cleanup.append(cls.isolated_network_offering)
            cls.isolated_network_offering.update(cls.apiclient, state='Enabled')

            cls.testdata["nw_off_isolated_persistent"]["specifyVlan"] = True
            cls.isolated_network_offering_vlan = NetworkOffering.create(
                          cls.apiclient,
                          cls.testdata["nw_off_isolated_persistent"])
            cls._cleanup.append(cls.isolated_network_offering_vlan)
            cls.isolated_network_offering_vlan.update(cls.apiclient, state='Enabled')

            cls.service_offering = ServiceOffering.create(
                                                          cls.apiclient,
                                                          cls.testdata["service_offering"])
            cls._cleanup.append(cls.service_offering)

            cls.testdata["small"]["zoneid"] = cls.zone.id
            cls.testdata["small"]["template"] = template.id
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest(e)
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
        self.physical_network, self.free_vlan = setNonContiguousVlanIds(self.apiclient,
                                                                            self.zone.id)
        return

    def tearDown(self):
        try:
            # Clean up
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        finally:
            self.physical_network.update(self.apiclient,
                        id=self.physical_network.id,
                        vlan=self.physical_network.vlan)
        return

    @attr(tags = ["advanced", "selfservice"], required_hardware="false")
    def test_01_dedicate_guest_vlan_range_root_domain(self):
        """Dedicate guest vlan range to account in root domain

        # Validate the following:
        # 1. Create two accounts under root domain
        # 2. Dedicate a new vlan range to account 1
        # 3. Verify that the new vlan range is dedicated to account 1
             by listing the dedicated range and checking the account name
        # 4. Try to create a guest network in account 2 usign the vlan in dedicated range
        # 5. The operation should fail
        # 6. Create a guest network in account 2
        # 7. Verify that the vlan for guest network is acquired from the dedicated range
        # 8. Delete the guest network in account 2
        # 9. Verify that the network is deleted
        # 10.Verify that the vlan is still dedicated to account 1 after deleting the network
        # 11.Release the vlan range back to the system
        # 12.Verify that ther list of dedicated vlans doesn't contain the vlan
        """
        self.account1 = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.domain.id
                            )
        self.cleanup.append(self.account1)

        self.account2 = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.domain.id
                            )
        self.cleanup.append(self.account2)

        new_vlan = self.physical_network.vlan + "," + self.free_vlan["partial_range"][0]
        self.physical_network.update(self.apiclient,
                id=self.physical_network.id, vlan=new_vlan)

        # Dedicating guest vlan range
        dedicate_guest_vlan_range_response = PhysicalNetwork.dedicate(
                                                self.apiclient,
                                                self.free_vlan["partial_range"][0],
                                                physicalnetworkid=self.physical_network.id,
                                                account=self.account1.name,
                                                domainid=self.account1.domainid
                                            )
        list_dedicated_guest_vlan_range_response = PhysicalNetwork.listDedicated(
                                                self.apiclient,
                                                id=dedicate_guest_vlan_range_response.id
                                        )
        dedicated_guest_vlan_response = list_dedicated_guest_vlan_range_response[0]
        self.assertEqual(
                            dedicated_guest_vlan_response.account,
                            self.account1.name,
                            "Check account name is in listDedicatedGuestVlanRanges as the account the range is dedicated to"
                        )

        dedicatedvlans = str(self.free_vlan["partial_range"][0]).split("-")

        with self.assertRaises(Exception):
            isolated_network1 = Network.create(
                                   self.apiclient,
                                   self.testdata["isolated_network"],
                                   self.account2.name,
                                   self.account2.domainid,
                                   networkofferingid=self.isolated_network_offering_vlan.id,
                                   vlan=int(dedicatedvlans[0]))
            isolated_network1.delete(self.apiclient)

        isolated_network2 = Network.create(
                                   self.apiclient,
                                   self.testdata["isolated_network"],
                                   self.account1.name,
                                   self.account1.domainid,
                                   networkofferingid=self.isolated_network_offering.id)

        networks = Network.list(self.apiclient, id=isolated_network2.id)
        self.assertEqual(validateList(networks)[0], PASS, "networks list validation failed")

        self.assertTrue(int(dedicatedvlans[0]) <= int(networks[0].vlan) <= int(dedicatedvlans[1]),
                        "Vlan of the network should be from the dedicated range")

        isolated_network2.delete(self.apiclient)
        self.assertTrue(isNetworkDeleted(self.apiclient, networkid=isolated_network2.id),
                        "Network not deleted in timeout period")

        # List after deleting all networks, it should still be dedicated to the account
        list_dedicated_guest_vlan_range_response = PhysicalNetwork.listDedicated(
                                                self.apiclient,
                                                id=dedicate_guest_vlan_range_response.id
                                        )
        dedicated_guest_vlan_response = list_dedicated_guest_vlan_range_response[0]
        self.assertEqual(
                            dedicated_guest_vlan_response.account,
                            self.account1.name,
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
        return

    @attr(tags = ["advanced", "selfservice"], required_hardware="false")
    def test_02_dedicate_guest_vlan_range_user_domain(self):
        """Dedicate guest vlan range to account in user domain

        # Validate the following:
        # 1. Create two accounts under user domain
        # 2. Dedicate a new vlan range to account 1
        # 3. Verify that the new vlan range is dedicated to account 1
             by listing the dedicated range and checking the account name
        # 4. Try to create a guest network in account 2 usign the vlan in dedicated range
        # 5. The operation should fail
        # 6. Create a guest network in account 2
        # 7. Verify that the vlan for guest network is acquired from the dedicated range
        # 8. Delete the guest network in account 2
        # 9. Verify that the network is deleted
        # 10.Verify that the vlan is still dedicated to account 1 after deleting the network
        # 11.Release the vlan range back to the system
        # 12.Verify that ther list of dedicated vlans doesn't contain the vlan
        """
        self.user_domain1 = Domain.create(
                                self.apiclient,
                                services=self.testdata["domain"],
                                parentdomainid=self.domain.id)
        self.cleanup.append(self.user_domain1)

        #Create Account
        self.account1 = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.user_domain1.id
                            )
        self.cleanup.insert(-1, self.account1)

        #Create Account
        self.account2 = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.user_domain1.id
                            )
        self.cleanup.insert(-1, self.account2)

        new_vlan = self.physical_network.vlan + "," + self.free_vlan["partial_range"][0]
        self.physical_network.update(self.apiclient,
                id=self.physical_network.id, vlan=new_vlan)

        # Dedicating guest vlan range
        dedicate_guest_vlan_range_response = PhysicalNetwork.dedicate(
                                                self.apiclient,
                                                self.free_vlan["partial_range"][0],
                                                physicalnetworkid=self.physical_network.id,
                                                account=self.account1.name,
                                                domainid=self.account1.domainid
                                            )
        list_dedicated_guest_vlan_range_response = PhysicalNetwork.listDedicated(
                                                self.apiclient,
                                                id=dedicate_guest_vlan_range_response.id
                                        )
        dedicated_guest_vlan_response = list_dedicated_guest_vlan_range_response[0]
        self.assertEqual(
                            dedicated_guest_vlan_response.account,
                            self.account1.name,
                            "Check account name is in listDedicatedGuestVlanRanges as the account the range is dedicated to"
                        )

        dedicatedvlans = str(self.free_vlan["partial_range"][0]).split("-")

        with self.assertRaises(Exception):
            isolated_network1 = Network.create(
                                   self.apiclient,
                                   self.testdata["isolated_network"],
                                   self.account2.name,
                                   self.account2.domainid,
                                   networkofferingid=self.isolated_network_offering_vlan.id,
                                   vlan=int(dedicatedvlans[0]))
            isolated_network1.delete(self.apiclient)

        isolated_network2 = Network.create(
                                   self.apiclient,
                                   self.testdata["isolated_network"],
                                   self.account1.name,
                                   self.account1.domainid,
                                   networkofferingid=self.isolated_network_offering.id)

        networks = Network.list(self.apiclient, id=isolated_network2.id, listall=True)
        self.assertEqual(validateList(networks)[0], PASS, "networks list validation failed")

        self.assertTrue(int(dedicatedvlans[0]) <= int(networks[0].vlan) <= int(dedicatedvlans[1]),
                        "Vlan of the network should be from the dedicated range")

        isolated_network2.delete(self.apiclient)
        self.assertTrue(isNetworkDeleted(self.apiclient, networkid=isolated_network2.id),
                        "Network not deleted in timeout period")

        self.debug("Releasing guest vlan range");
        dedicate_guest_vlan_range_response.release(self.apiclient)
        list_dedicated_guest_vlan_range_response = PhysicalNetwork.listDedicated(self.apiclient)
        self.assertEqual(
                        list_dedicated_guest_vlan_range_response,
                        None,
                        "Check vlan range is not available in listDedicatedGuestVlanRanges"

                        )
        return

    @attr(tags = ["advanced", "selfservice"], required_hardware="false")
    def test_03_multiple_guest_netwoks(self):
        """Dedicate multiple guest networks in account with dedicated vlan range

        # Validate the following:
        # 1. Create account under user domain
        # 2. Dedicate a new vlan range of range 2 to account
        # 3. Verify that the new vlan range is dedicated to account
             by listing the dedicated range and checking the account name
        # 4. Create a guest network in the account
        # 5. Verify that the vlan of the network is from dedicated range
        # 6. Repeat steps 4 and 5 for network 2
        # 7. Now create 3rd guest network in the account
        # 8. Verify that the vlan of the network is not from the dedicated range, as
             all the vlans in dedicated range are now exhausted
        """
        self.user_domain = Domain.create(
                                self.apiclient,
                                services=self.testdata["domain"],
                                parentdomainid=self.domain.id)
        self.cleanup.append(self.user_domain)

        #Create Account
        self.account = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.user_domain.id
                            )
        self.cleanup.insert(-1, self.account)

        self.free_vlan["partial_range"][0] = LimitVlanRange(self, self.free_vlan["partial_range"][0], range=2)
        vlan_startid = int(str(self.free_vlan["partial_range"][0]).split("-")[0])
        vlan_endid = vlan_startid + 1

        new_vlan = self.physical_network.vlan + "," + self.free_vlan["partial_range"][0]
        self.physical_network.update(self.apiclient,
                id=self.physical_network.id, vlan=new_vlan)

        # Dedicating guest vlan range
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

        isolated_network1 = Network.create(
                                   self.apiclient,
                                   self.testdata["isolated_network"],
                                   self.account.name,
                                   self.account.domainid,
                                   networkofferingid=self.isolated_network_offering.id)

        networks = Network.list(self.apiclient, id=isolated_network1.id, listall=True)
        self.assertEqual(validateList(networks)[0], PASS, "networks list validation failed")

        self.assertTrue(vlan_startid <= int(networks[0].vlan) <= vlan_endid,
                        "Vlan of the network should be from the dedicated range")

        isolated_network2 = Network.create(
                                   self.apiclient,
                                   self.testdata["isolated_network"],
                                   self.account.name,
                                   self.account.domainid,
                                   networkofferingid=self.isolated_network_offering.id)

        networks = Network.list(self.apiclient, id=isolated_network2.id, listall=True)
        self.assertEqual(validateList(networks)[0], PASS, "networks list validation failed")

        self.assertTrue(vlan_startid <= int(networks[0].vlan) <= vlan_endid,
                        "Vlan of the network should be from the dedicated range")

        isolated_network3 = Network.create(
                                   self.apiclient,
                                   self.testdata["isolated_network"],
                                   self.account.name,
                                   self.account.domainid,
                                   networkofferingid=self.isolated_network_offering.id)

        networks = Network.list(self.apiclient, id=isolated_network3.id, listall=True)
        self.assertEqual(validateList(networks)[0], PASS, "networks list validation failed")

        self.assertFalse(vlan_startid <= int(networks[0].vlan) <= vlan_endid,
                        "Vlan of the network should not be from the dedicated range")
        return


    @attr(tags = ["invalid"])
    def test_04_dedicate_guest_vlan_in_project(self):
        """Dedicate guest vlan range project owner account and test guest network vlan in project

        # Validate the following:
        # 1. Create account under user domain
        # 2. Create a project with this account
        # 3. Dedicate a new vlan range to the account
        # 4. Verify that the new vlan range is dedicated to account
             by listing the dedicated range and checking the account name
        # 5. Create a guest network in the project
        # 6. Verify that the vlan of the network is from dedicated range
        # 7. Repeat steps 4 and 5 for network 2
        # 8. Now create 3rd guest network in the account
        # 9. Verify that the vlan of the network is not from the dedicated range, as
             all the vlans in dedicated range are now exhausted
        """
        user_domain = Domain.create(
                                self.apiclient,
                                services=self.testdata["domain"],
                                parentdomainid=self.domain.id)
        self.cleanup.append(user_domain)
        #Create Account
        self.account = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=user_domain.id
                            )
        self.cleanup.insert(-1, self.account)
        # Create project as a domain admin
        project = Project.create(self.apiclient,
                                 self.testdata["project"],
                                 account=self.account.name,
                                 domainid=self.account.domainid)
        self.cleanup.insert(-2, project)

        self.free_vlan["partial_range"][0] = LimitVlanRange(self, self.free_vlan["partial_range"][0], range=2)
        vlan_startid = int(str(self.free_vlan["partial_range"][0]).split("-")[0])
        vlan_endid = vlan_startid + 1

        new_vlan = self.physical_network.vlan + "," + self.free_vlan["partial_range"][0]
        self.physical_network.update(self.apiclient,
                id=self.physical_network.id, vlan=new_vlan)

        # Dedicating guest vlan range
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

        isolated_network1 = Network.create(
                                   self.apiclient,
                                   self.testdata["isolated_network"],
                                   projectid=project.id,
                                   networkofferingid=self.isolated_network_offering.id)

        networks = Network.list(self.apiclient, id=isolated_network1.id, projectid=project.id, listall=True)
        self.assertEqual(validateList(networks)[0], PASS, "networks list validation failed")

        self.assertTrue(vlan_startid <= int(networks[0].vlan) <= vlan_endid,
                        "Vlan of the network should be from the dedicated range")

        isolated_network2 = Network.create(
                                   self.apiclient,
                                   self.testdata["isolated_network"],
                                   projectid=project.id,
                                   networkofferingid=self.isolated_network_offering.id)

        networks = Network.list(self.apiclient, id=isolated_network2.id, projectid=project.id, listall=True)
        self.assertEqual(validateList(networks)[0], PASS, "networks list validation failed")

        self.assertTrue(vlan_startid <= int(networks[0].vlan) <= vlan_endid,
                        "Vlan of the network should be from the dedicated range")

        isolated_network3 = Network.create(
                                   self.apiclient,
                                   self.testdata["isolated_network"],
                                   projectid=project.id,
                                   networkofferingid=self.isolated_network_offering.id)

        networks = Network.list(self.apiclient, id=isolated_network3.id, projectid=project.id, listall=True)
        self.assertEqual(validateList(networks)[0], PASS, "networks list validation failed")

        self.assertFalse(vlan_startid <= int(networks[0].vlan) <= vlan_endid,
                        "Vlan of the network should be from the dedicated range")
        return

    @attr(tags = ["advanced", "selfservice"], required_hardware="false")
    def test_05_dedicate_range_different_accounts(self):
        """Dedicate two different vlan ranges to two different accounts

        # Validate the following:
        # 1. Create two accounts in root domain
        # 2. Update the physical network with two different vlan ranges
        # 3. Dedicate first vlan range to the account 1
        # 4. Dedicate 2nd vlan range to account 2
        # 5. Both the operations should be successful
        """
        self.account1 = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.domain.id
                            )
        self.cleanup.append(self.account1)

        self.account2 = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.domain.id
                            )
        self.cleanup.append(self.account2)

        new_vlan = self.physical_network.vlan + "," + self.free_vlan["partial_range"][0] + ","+\
                   self.free_vlan["partial_range"][1]
        self.physical_network.update(self.apiclient,
                id=self.physical_network.id, vlan=new_vlan)

        # Dedicating guest vlan range
        dedicate_guest_vlan_range_response = PhysicalNetwork.dedicate(
                                                 self.apiclient,
                                                 self.free_vlan["partial_range"][0],
                                                 physicalnetworkid=self.physical_network.id,
                                                 account=self.account1.name,
                                                 domainid=self.account1.domainid
                                                 )
        list_dedicated_guest_vlan_range_response = PhysicalNetwork.listDedicated(
                                                       self.apiclient,
                                                       id=dedicate_guest_vlan_range_response.id
                                                       )
        dedicated_guest_vlan_response = list_dedicated_guest_vlan_range_response[0]
        self.assertEqual(
                            dedicated_guest_vlan_response.account,
                            self.account1.name,
                            "Check account name is in listDedicatedGuestVlanRanges as the account the range is dedicated to"
                        )

        # Dedicating guest vlan range
        dedicate_guest_vlan_range_response = PhysicalNetwork.dedicate(
                                                 self.apiclient,
                                                 self.free_vlan["partial_range"][1],
                                                 physicalnetworkid=self.physical_network.id,
                                                 account=self.account2.name,
                                                 domainid=self.account2.domainid
                                                 )

        list_dedicated_guest_vlan_range_response = PhysicalNetwork.listDedicated(
                                                       self.apiclient,
                                                       id=dedicate_guest_vlan_range_response.id
                                                       )
        dedicated_guest_vlan_response = list_dedicated_guest_vlan_range_response[0]
        self.assertEqual(
                         dedicated_guest_vlan_response.account,
                         self.account2.name,
                         "Check account name is in listDedicatedGuestVlanRanges as the account the range is dedicated to"
                         )
        return

    @attr(tags = ["advanced", "selfservice"], required_hardware="false")
    def test_07_extend_vlan_range(self):
        """Dedicate vlan range to an account when some vlan in range are already acquired by same account

        # Validate the following:
        # 1. Create account under root domain
        # 2. Add a new vlan range to the physical network
        # 3. Create a guest network in account using the vlan id from the newly added range
        # 4. Try to dedicate the vlan range to account
        # 5. Operation should succeed
        """
        self.account = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.domain.id
                            )
        self.cleanup.append(self.account)

        vlans = str(self.free_vlan["partial_range"][0]).split("-")
        startid = int(vlans[0])
        endid = int(vlans[1])

        vlan_range1 = str(startid) + "-" + str(endid)
        vlan_range2 = str(endid+1) + "-" + str(endid+2)
        full_range = str(startid) + "-" + str(endid+2)

        new_vlan = self.physical_network.vlan + "," + full_range
        self.physical_network.update(self.apiclient,
                id=self.physical_network.id, vlan=new_vlan)

        # Dedicating first range
        PhysicalNetwork.dedicate(
                                 self.apiclient,
                                 vlan_range1,
                                 physicalnetworkid=self.physical_network.id,
                                 account=self.account.name,
                                 domainid=self.account.domainid
                                 )

        # Dedicating second range
        PhysicalNetwork.dedicate(
                                 self.apiclient,
                                 vlan_range2,
                                 physicalnetworkid=self.physical_network.id,
                                 account=self.account.name,
                                 domainid=self.account.domainid
                                 )

        dedicated_ranges = PhysicalNetwork.listDedicated(
                                                self.apiclient,
                                                account=self.account.name,
                                                domainid=self.account.domainid,
                                                listall=True
                                                )
        self.assertEqual(str(dedicated_ranges[0].guestvlanrange), full_range, "Dedicated vlan\
                         range not matching with expcted extended range")

        return

class TestFailureScenarios(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestFailureScenarios, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.testdata =  cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient)
        cls.testdata["isolated_network"]["zoneid"] = cls.zone.id
        cls.testdata['mode'] = cls.zone.networktype
        template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata["ostype"]
            )
        cls._cleanup = []

        try:
            cls.isolated_network_offering = NetworkOffering.create(
                          cls.apiclient,
                          cls.testdata["nw_off_isolated_persistent"])
            cls._cleanup.append(cls.isolated_network_offering)
            cls.isolated_network_offering.update(cls.apiclient, state='Enabled')

            cls.testdata["nw_off_isolated_persistent"]["specifyVlan"] = True
            cls.isolated_network_offering_vlan = NetworkOffering.create(
                          cls.apiclient,
                          cls.testdata["nw_off_isolated_persistent"])
            cls._cleanup.append(cls.isolated_network_offering_vlan)
            cls.isolated_network_offering_vlan.update(cls.apiclient, state='Enabled')

            cls.service_offering = ServiceOffering.create(
                                                          cls.apiclient,
                                                          cls.testdata["service_offering"])
            cls._cleanup.append(cls.service_offering)

            cls.testdata["small"]["zoneid"] = cls.zone.id
            cls.testdata["small"]["template"] = template.id
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest(e)
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
        self.physical_network, self.free_vlan = setNonContiguousVlanIds(self.apiclient,
                                                                            self.zone.id)
        return

    def tearDown(self):
        try:
            # Clean up
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        finally:
            self.physical_network.update(self.apiclient,
                        id=self.physical_network.id,
                        vlan=self.physical_network.vlan)
        return

    @attr(tags = ["advanced", "selfservice"], required_hardware="false")
    def test_01_dedicate_wrong_vlan_range(self):
        """Dedicate invalid vlan range to account

        # Validate the following:
        # 1. Create an account in root domain
        # 2. Try to update physical network with invalid range (5000-5001)
             and dedicate it to account
        # 3. The operation should fail
        """
        self.account = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.domain.id
                            )
        self.cleanup.append(self.account)

        vlan_range = "5000-5001"

        new_vlan = self.physical_network.vlan + "," + vlan_range

        with self.assertRaises(Exception):
            self.physical_network.update(self.apiclient,
                                         id=self.physical_network.id,
                                         vlan=new_vlan)

            # Dedicating guest vlan range
            PhysicalNetwork.dedicate(
                                     self.apiclient,
                                     vlan_range,
                                     physicalnetworkid=self.physical_network.id,
                                     account=self.account.name,
                                     domainid=self.account.domainid
                                    )
        return

    @attr(tags = ["advanced", "selfservice"], required_hardware="false")
    def test_02_dedicate_vlan_range_invalid_account(self):
        """Dedicate a guest vlan range to invalid account

        # Validate the following:
        # 1. Create an account in root domain
        # 2. Update physical network with new guest vlan range
        # 3. Try to dedicate it to invalid account
        # 4. The operation should fail
        """
        self.account = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.domain.id
                            )
        self.cleanup.append(self.account)

        new_vlan = self.physical_network.vlan + "," + self.free_vlan["partial_range"][0]
        self.physical_network.update(self.apiclient,
                                     id=self.physical_network.id,
                                     vlan=new_vlan)

        with self.assertRaises(Exception):
            # Dedicating guest vlan range
            PhysicalNetwork.dedicate(
                                     self.apiclient,
                                     self.free_vlan["partial_range"][0],
                                     physicalnetworkid=self.physical_network.id,
                                     account=self.account.name+random_gen(),
                                     domainid=self.account.domainid
                                    )
        return

    @attr(tags = ["advanced", "selfservice"], required_hardware="false")
    def test_03_dedicate_already_dedicated_range(self):
        """Dedicate a guest vlan range which is already dedicated

        # Validate the following:
        # 1. Create two accounts in root domain
        # 2. Update physical network with new guest vlan range
        # 3. Dedicate the vlan range to account 1
        # 4. Try to dedicate the same range to account 2, operation should fail
        """
        self.account1 = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.domain.id
                            )
        self.cleanup.append(self.account1)

        self.account2 = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.domain.id
                            )
        self.cleanup.append(self.account2)

        new_vlan = self.physical_network.vlan + "," + self.free_vlan["partial_range"][0]
        self.physical_network.update(self.apiclient,
                                     id=self.physical_network.id,
                                     vlan=new_vlan)

        # Dedicating guest vlan range
        PhysicalNetwork.dedicate(
                                     self.apiclient,
                                     self.free_vlan["partial_range"][0],
                                     physicalnetworkid=self.physical_network.id,
                                     account=self.account1.name,
                                     domainid=self.account1.domainid
                                    )

        with self.assertRaises(Exception):
            # Dedicating guest vlan range
            PhysicalNetwork.dedicate(
                                     self.apiclient,
                                     self.free_vlan["partial_range"][0],
                                     physicalnetworkid=self.physical_network.id,
                                     account=self.account2.name,
                                     domainid=self.account2.domainid
                                    )
        return

class TestDeleteVlanRange(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestDeleteVlanRange, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.testdata =  cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient)
        cls.testdata["isolated_network"]["zoneid"] = cls.zone.id
        cls.testdata['mode'] = cls.zone.networktype
        template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata["ostype"]
            )
        cls._cleanup = []

        try:
            cls.isolated_persistent_network_offering = NetworkOffering.create(
                          cls.apiclient,
                          cls.testdata["nw_off_isolated_persistent"])
            cls._cleanup.append(cls.isolated_persistent_network_offering)
            cls.isolated_persistent_network_offering.update(cls.apiclient, state='Enabled')

            cls.isolated_network_offering = NetworkOffering.create(
                          cls.apiclient,
                          cls.testdata["isolated_network_offering"])
            cls._cleanup.append(cls.isolated_network_offering)
            cls.isolated_network_offering.update(cls.apiclient, state='Enabled')

            cls.testdata["nw_off_isolated_persistent"]["specifyvlan"] = True
            cls.isolated_network_offering_vlan = NetworkOffering.create(
                          cls.apiclient,
                          cls.testdata["nw_off_isolated_persistent"])
            cls._cleanup.append(cls.isolated_network_offering_vlan)
            cls.isolated_network_offering_vlan.update(cls.apiclient, state='Enabled')

            cls.service_offering = ServiceOffering.create(
                                                          cls.apiclient,
                                                          cls.testdata["service_offering"])
            cls._cleanup.append(cls.service_offering)

            cls.testdata["small"]["zoneid"] = cls.zone.id
            cls.testdata["small"]["template"] = template.id
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest(e)
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
        self.physical_network, self.free_vlan = setNonContiguousVlanIds(self.apiclient,
                                                                            self.zone.id)
        return

    def tearDown(self):
        try:
            # Clean up
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        finally:
            self.physical_network.update(self.apiclient,
                        id=self.physical_network.id,
                        vlan=self.physical_network.vlan)
        return

    @attr(tags = ["advanced", "selfservice"], required_hardware="false")
    def test_01_delete_dedicated_vlan_range(self):
        """Try to delete a dedicated vlan range which is not in use

        # Validate the following:
        # 1. Creat an account in the root domain
        # 2. update the physical network with a new vlan range
        # 3. Dedicated this vlan range to the account
        # 4. Verify that the vlan range is dedicated to the account by listing it
             and verifying the account name
        # 5. Try to delete the vlan range by updating physical network vlan, operation should fail
        # 6. Release the dedicted range and then delete the vlan range
        # 7. The operation should succeed
        """
        self.account = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.domain.id
                            )
        self.cleanup.append(self.account)
        new_vlan = self.free_vlan["partial_range"][0]
        extended_vlan = self.physical_network.vlan + "," + new_vlan

        self.physical_network.update(self.apiclient,
                                         id=self.physical_network.id,
                                         vlan=extended_vlan)

        # Dedicating guest vlan range
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

        with self.assertRaises(Exception):
            # Deleting the dedicated vlan range
            self.physical_network.update(self.apiclient,
                        id=self.physical_network.id,
                        vlan=self.physical_network.vlan)

        dedicate_guest_vlan_range_response.release(self.apiclient)
        self.physical_network.update(self.apiclient,
                        id=self.physical_network.id,
                        vlan=self.physical_network.vlan)
        physical_networks = PhysicalNetwork.list(self.apiclient, id=self.physical_network.id, listall=True)
        self.assertEqual(validateList(physical_networks)[0], PASS, "Physical networks list validation failed")
        vlans = xsplit(physical_networks[0].vlan, [','])
        self.assertFalse(new_vlan in vlans, "newly added vlan is not deleted from physical network")

    @attr(tags = ["advanced", "selfservice"], required_hardware="false")
    def test_02_delete_dedicated_vlan_range_vlan_in_use(self):
        """Try to delete a dedicated vlan rang which is in use

        # Validate the following:
        # 1. Creat an account in the root domain
        # 2. update the physical network with a new vlan range
        # 3. Dedicated this vlan range to the account
        # 4. Verify that the vlan range is dedicated to the account by listing it
             and verifying the account name
        # 5. Create a guest network in the account and verify that the vlan of network
             is from the dedicated range
        # 6. Try to delete the vlan range by updating physical network vlan
        # 7. The operation should fail
        """
        self.account = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.domain.id
                            )
        self.cleanup.append(self.account)

        new_vlan = self.physical_network.vlan + "," + self.free_vlan["partial_range"][0]

        self.physical_network.update(self.apiclient,
                                         id=self.physical_network.id,
                                         vlan=new_vlan)

        # Dedicating guest vlan range
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

        Network.create(
                                   self.apiclient,
                                   self.testdata["isolated_network"],
                                   self.account.name,
                                   self.account.domainid,
                                   networkofferingid=self.isolated_persistent_network_offering.id)

        with self.assertRaises(Exception):
            # Deleting the dedicated vlan range
            self.physical_network.update(self.apiclient,
                        id=self.physical_network.id,
                        vlan=self.physical_network.vlan)
        return

    @attr(tags = ["advanced", "selfservice"], required_hardware="false")
    def test_03_delete_account(self):
        """Try to delete a dedicated vlan rang which is in use

        # Validate the following:
        # 1. Creat an account in the root domain
        # 2. Update the physical network with a new vlan range
        # 3. Dedicated this vlan range to the account
        # 4. Verify that the vlan range is dedicated to the account by listing it
             and verifying the account name
        # 5. Create a guest network in the account which consumes vlan from dedicated range
        # 6. Delete the account
        # 7. Verify that the vlan of the physical network remains the same
        """
        self.account = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.domain.id
                            )
        self.cleanup.append(self.account)

        new_vlan = self.physical_network.vlan + "," + self.free_vlan["partial_range"][0]

        self.physical_network.update(self.apiclient,
                                         id=self.physical_network.id,
                                         vlan=new_vlan)

        # Dedicating guest vlan range
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

        Network.create(
                                   self.apiclient,
                                   self.testdata["isolated_network"],
                                   self.account.name,
                                   self.account.domainid,
                                   networkofferingid=self.isolated_persistent_network_offering.id)

        self.account.delete(self.apiclient)
        self.cleanup.remove(self.account)

        physical_networks = PhysicalNetwork.list(self.apiclient, id=self.physical_network.id, listall=True)
        self.assertEqual(validateList(physical_networks)[0], PASS, "Physical networks list validation failed")
        self.assertEqual(physical_networks[0].vlan, new_vlan, "The vlan of physical network \
                         should be same after deleting account")
        return

    @attr(tags = ["advanced", "selfservice"], required_hardware="false")
    def test_04_release_range_no_vlan_in_use(self):
        """Release a dedicated vlan range when no vlan id is in use

        # Validate the following:
        # 1. Create account in root domain
        # 2. Dedicate a new vlan range to account
        # 3. Verify that the new vlan range is dedicated to account
             by listing the dedicated range and checking the account name
        # 4. Release the range
        # 5. Verify the range is released back to system by listing dedicated ranges (list should be empty)
        """
        self.account1 = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.domain.id
                            )
        self.cleanup.append(self.account1)

        new_vlan = self.physical_network.vlan + "," + self.free_vlan["partial_range"][0]
        self.physical_network.update(self.apiclient,
                id=self.physical_network.id, vlan=new_vlan)

        # Dedicating guest vlan range
        dedicate_guest_vlan_range_response = PhysicalNetwork.dedicate(
                                                self.apiclient,
                                                self.free_vlan["partial_range"][0],
                                                physicalnetworkid=self.physical_network.id,
                                                account=self.account1.name,
                                                domainid=self.account1.domainid
                                            )
        list_dedicated_guest_vlan_range_response = PhysicalNetwork.listDedicated(
                                                self.apiclient,
                                                id=dedicate_guest_vlan_range_response.id
                                        )
        dedicated_guest_vlan_response = list_dedicated_guest_vlan_range_response[0]
        self.assertEqual(
                            dedicated_guest_vlan_response.account,
                            self.account1.name,
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
        return

    @attr(tags = ["advanced", "selfservice"], required_hardware="false")
    def test_05_release_range_vlan_in_use(self):
        """Release a dedicated vlan range when no vlan id is in use

        # Validate the following:
        # 1. Create account in root domain
        # 2. Dedicate a new vlan range to account
        # 3. Verify that the new vlan range is dedicated to account
             by listing the dedicated range and checking the account name
        # 4. Release the range
        # 5. The operation should succeed, as all vlans which are not in use should be released
        """
        self.account1 = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.domain.id
                            )
        self.cleanup.append(self.account1)

        new_vlan = self.physical_network.vlan + "," + self.free_vlan["partial_range"][0]
        self.physical_network.update(self.apiclient,
                id=self.physical_network.id, vlan=new_vlan)

        # Dedicating guest vlan range
        dedicate_guest_vlan_range_response = PhysicalNetwork.dedicate(
                                                self.apiclient,
                                                self.free_vlan["partial_range"][0],
                                                physicalnetworkid=self.physical_network.id,
                                                account=self.account1.name,
                                                domainid=self.account1.domainid
                                            )
        list_dedicated_guest_vlan_range_response = PhysicalNetwork.listDedicated(
                                                self.apiclient,
                                                id=dedicate_guest_vlan_range_response.id
                                                )
        dedicated_guest_vlan_response = list_dedicated_guest_vlan_range_response[0]
        self.assertEqual(
                            dedicated_guest_vlan_response.account,
                            self.account1.name,
                            "Check account name is in listDedicatedGuestVlanRanges as the account the range is dedicated to"
                        )

        dedicatedvlans = str(self.free_vlan["partial_range"][0]).split("-")

        isolated_network = Network.create(
                                   self.apiclient,
                                   self.testdata["isolated_network"],
                                   self.account1.name,
                                   self.account1.domainid,
                                   networkofferingid=self.isolated_persistent_network_offering.id)

        networks = Network.list(self.apiclient, id=isolated_network.id)
        self.assertEqual(validateList(networks)[0], PASS, "networks list validation failed")

        self.assertTrue(int(dedicatedvlans[0]) <= int(networks[0].vlan) <= int(dedicatedvlans[1]),
                        "Vlan of the network should be from the dedicated range")

        self.debug("Releasing guest vlan range");
        dedicate_guest_vlan_range_response.release(self.apiclient)
        list_dedicated_guest_vlan_range_response = PhysicalNetwork.listDedicated(self.apiclient)
        self.assertEqual(
                        list_dedicated_guest_vlan_range_response,
                        None,
                        "Check vlan range is not available in listDedicatedGuestVlanRanges"

                        )
        return
