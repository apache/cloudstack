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
""" Tests for IP reservation feature

    Test Plan: https://cwiki.apache.org/confluence/display/CLOUDSTACK/IP+Range+Reservation+within+a+Network+Test+Cases

    Issue Link: https://issues.apache.org/jira/browse/CLOUDSTACK-2266

    Feature Specifications: https://cwiki.apache.org/confluence/display/CLOUDSTACK/FS+-+IP+Range+Reservation+within+a+Network
"""
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.utils import validateList, cleanup_resources, verifyRouterState
from marvin.lib.base import (Account,
                             Network,
                             VirtualMachine,
                             Router,
                             ServiceOffering,
                             NetworkOffering)
from marvin.lib.common import (get_zone,
                               get_template,
                               get_domain,
                               wait_for_cleanup,
                               createEnabledNetworkOffering,
                               createNetworkRulesForVM,
                               verifyNetworkState)
from marvin.codes import (PASS, FAIL, FAILED, UNKNOWN, FAULT, MASTER,
                          NAT_RULE, STATIC_NAT_RULE)
import netaddr

import random
from nose.plugins.attrib import attr
from ddt import ddt, data

def createIsolatedNetwork(self, network_offering_id, gateway=None):
    """Create isolated network with given network offering and gateway if provided
    and return"""
    try:
        isolated_network = Network.create(self.apiclient, self.testData["isolated_network"],
                         networkofferingid=network_offering_id,accountid=self.account.name,
                         domainid=self.domain.id,zoneid=self.zone.id,
                         gateway=gateway, netmask='255.255.255.0' if gateway else None)
    except Exception as e:
        return [FAIL, e]
    return [PASS, isolated_network]

def matchNetworkGuestVmCIDR(self, networkid, guestvmcidr):
    """List networks with given network id and check if the guestvmcidr matches
    with the given cidr"""

    networks = Network.list(self.apiclient, id=networkid, listall=True)
    self.assertEqual(validateList(networks)[0], PASS, "network list validation failed")
    self.assertEqual(str(networks[0].cidr), guestvmcidr, "guestvmcidr of network %s \
                does not match with the given value %s" % (networks[0].cidr, guestvmcidr))

    return

def createVirtualMachine(self, network_id=None, ip_address=None):
    """Create and return virtual machine within network and ipaddress"""
    virtual_machine = VirtualMachine.create(self.apiclient,
                                            self.testData["virtual_machine"],
                                            networkids=network_id,
                                            serviceofferingid=self.service_offering.id,
                                            accountid=self.account.name,
                                            domainid=self.domain.id,
                                            ipaddress=ip_address)
    return virtual_machine

def CreateEnabledNetworkOffering(apiclient, networkServices):
    """Create network offering of given test data and enable it"""

    result = createEnabledNetworkOffering(apiclient, networkServices)
    assert result[0] == PASS, "Network offering creation/enabling failed due to %s" % result[2]
    return result[1]

@ddt
class TestIpReservation(cloudstackTestCase):
    """Test IP Range Reservation with a Network
    """
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestIpReservation, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill services from the external config file
        cls.testData = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.testData["ostype"]
                            )
        if cls.template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.testData["ostype"]
        cls.testData["domainid"] = cls.domain.id
        cls.testData["zoneid"] = cls.zone.id
        cls.testData["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testData["virtual_machine"]["template"] = cls.template.id
        cls._cleanup = []
        try:
            cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.testData["service_offering"]
                                            )
            cls._cleanup.append(cls.service_offering)
            cls.isolated_network_offering = CreateEnabledNetworkOffering(cls.api_client,
                                            cls.testData["isolated_network_offering"])
            cls._cleanup.append(cls.isolated_network_offering)
            cls.isolated_persistent_network_offering = CreateEnabledNetworkOffering(cls.api_client,
                                            cls.testData["nw_off_isolated_persistent"])
            cls._cleanup.append(cls.isolated_persistent_network_offering)
            cls.isolated_network_offering_RVR = CreateEnabledNetworkOffering(cls.api_client,
                                            cls.testData["nw_off_isolated_RVR"])
            cls._cleanup.append(cls.isolated_network_offering_RVR)
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Failure in setUpClass: %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        try:
            self.account = Account.create(self.apiclient, self.testData["account"],
                                      domainid=self.domain.id)
            self.cleanup.append(self.account)
        except Exception as e:
            self.skipTest("Failed to create account: %s" % e)
        return

    def tearDown(self):
        try:
            # Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"])
    def test_vm_create_after_reservation(self):
        """ Test creating VM in network after IP reservation
        # steps
        # 1. Create vm in isolated network (LB through VR or Netscaler) with ip in guestvmcidr
        # 2. Update guestvmcidr
        # 3. Create another VM
        # validation
        # 1. Guest vm cidr should be successfully updated with correct value
        # 2. Existing guest vm ip should not be changed after reservation
        # 3. Newly created VM should get ip in guestvmcidr"""

        networkOffering = self.isolated_network_offering
        subnet = "10.1."+str(random.randrange(1,254))
        gateway = subnet +".1"
        resultSet = createIsolatedNetwork(self, networkOffering.id, gateway=gateway)
        if resultSet[0] == FAIL:
            self.fail("Failed to create isolated network")
        else:
            isolated_network = resultSet[1]
        guest_vm_cidr = subnet +".0/29"

        try:
            virtual_machine_1 = createVirtualMachine(self, network_id=isolated_network.id,
                    ip_address = subnet+".3")
        except Exception as e:
            self.fail("VM creation failed: %s" % e)

        isolated_network.update(self.apiclient, guestvmcidr=guest_vm_cidr)
        matchNetworkGuestVmCIDR(self, isolated_network.id, guest_vm_cidr)
        vms = VirtualMachine.list(self.apiclient,
                                       id=virtual_machine_1.id)
        self.assertEqual(validateList(vms)[0], PASS, "vm list validation failed")
        self.assertEqual(vms[0].nic[0].ipaddress,
                          virtual_machine_1.ipaddress,
                           "VM IP should not change after reservation")
        try:
            virtual_machine_2 = createVirtualMachine(self, network_id=isolated_network.id)
            if netaddr.IPAddress(virtual_machine_2.ipaddress) not in netaddr.IPNetwork(guest_vm_cidr):
                self.fail("Newly created VM doesn't get IP from reserverd CIDR")
        except Exception as e:
            self.fail("VM creation failed, cannot validate the condition: %s" % e)
        return

    @attr(tags=["advanced"])
    def test_vm_create_outside_cidr_after_reservation(self):
        """ Test create VM outside the range of reserved IPs
        # steps
        # 1. update guestvmcidr of persistent isolated network (LB through VR or
        #    Netscaler
        # 2. create another VM with ip outside guestvmcidr
        """
        # validation
        # 1. guest vm cidr should be successfully updated with correct value
        # 2  newly created VM should not be created and result in exception

        networkOffering = self.isolated_persistent_network_offering
        subnet = "10.1."+str(random.randrange(1,254))
        gateway = subnet +".1"
        resultSet = createIsolatedNetwork(self, networkOffering.id, gateway=gateway)
        if resultSet[0] == FAIL:
            self.fail("Failed to create isolated network")
        else:
            isolated_network = resultSet[1]
        guest_vm_cidr = subnet+".0/29"

        isolated_network.update(self.apiclient, guestvmcidr=guest_vm_cidr)
        matchNetworkGuestVmCIDR(self, isolated_network.id, guest_vm_cidr)
        try:
            createVirtualMachine(self, network_id=self.isolated_network.id,
                                 ip_address=subnet+".9")
            self.fail("vm should not be created ")
        except Exception as e:
            self.debug("exception as IP is outside of guestvmcidr %s" % e)
        return

    @attr(tags=["advanced"])
    def test_update_cidr_multiple_vms_not_all_inclusive(self):
        """ Test reserve IP range such that one of the VM is not included
        # steps
        # 1. Create two vms in isolated network
        # 2. Update guestvmcidr of network such that only one of the ipaddress of vms
        #    is in the given range
        # validation
        # 1. Network updation with this new range should fail"""

        subnet = "10.1."+str(random.randrange(1,254))
        gateway = subnet +".1"
        resultSet = createIsolatedNetwork(self, self.isolated_network_offering.id,
                                          gateway=gateway)
        if resultSet[0] == FAIL:
            self.fail("Failed to create isolated network")
        else:
            isolated_network = resultSet[1]
        guest_vm_cidr = subnet+".0/29"

        try:
            createVirtualMachine(self, network_id=isolated_network.id,
                    ip_address=subnet+".3")
        except Exception as e:
            self.fail("VM creation failed: %s" % e)

        try:
            createVirtualMachine(self, network_id=isolated_network.id,
                    ip_address=subnet+".9")
        except Exception as e:
            self.fail("VM creation failed: %s" % e)

        with self.assertRaises(Exception):
            isolated_network.update(self.apiclient, guestvmcidr=guest_vm_cidr)
        return

    @attr(tags=["advanced"])
    def test_update_cidr_single_vm_not_inclusive(self):
        """ Test reserving IP range in network such that existing VM is outside the range
        # steps
        # 1. Create vm in isolated network
        # 2. Update guestvmcidr of network such that ip address of vm
        #    is outside the given range
        #
        # validation
        # 1. Network updation with this new range should fail"""
        subnet = "10.1."+str(random.randrange(1,254))
        gateway = subnet +".1"
        resultSet = createIsolatedNetwork(self, self.isolated_network_offering.id,
                                          gateway=gateway)
        if resultSet[0] == FAIL:
            self.fail("Failed to create isolated network")
        else:
            isolated_network = resultSet[1]
        guest_vm_cidr = subnet+".0/29"

        try:
            createVirtualMachine(self, network_id=isolated_network.id,
                    ip_address=subnet+".9")
        except Exception as e:
            self.fail("VM creation failed: %s" % e)

        with self.assertRaises(Exception):
            isolated_network.update(self.apiclient, guestvmcidr=guest_vm_cidr)
        return

    @data(NAT_RULE, STATIC_NAT_RULE)
    @attr(tags=["advanced"], required_hardware="true")
    def test_nat_rules(self, value):
        """ Test NAT rules working with IP reservation
        # steps
        # 1. Create vm in persistent isolated network with ip in guestvmcidr
        # 2. Create NAT/static NAT rule for this VM
        # 3. Update guestvmcidr
        # 4. Create another VM and create network rules for this vm too
        #
        # validation
        # 1. Guest vm cidr should be successfully updated with correct value
        # 2. Existing guest vm ip should not be changed after reservation
        # 3. Newly created VM should get ip in guestvmcidr
        # 4. The network rules should be working"""

        subnet = "10.1."+str(random.randrange(1,254))
        gateway = subnet +".1"
        resultSet = createIsolatedNetwork(self, self.isolated_network_offering.id,
                                          gateway=gateway)
        if resultSet[0] == FAIL:
            self.fail("Failed to create isolated network")
        else:
            isolated_network = resultSet[1]
        guest_vm_cidr = subnet+".0/29"

        try:
            virtual_machine_1 = createVirtualMachine(self, network_id=isolated_network.id,
                    ip_address=subnet+".3")
        except Exception as e:
            self.fail("VM creation failed: %s" % e)

        result = createNetworkRulesForVM(self.apiclient, virtual_machine_1,value,
                                         self.account, self.testData)
        if result[0] == FAIL:
            self.fail("Failed to create network rules for VM: %s" % result[1])
        else:
            ipaddress_1 = result[1]
        virtual_machine_1.get_ssh_client(ipaddress=ipaddress_1.ipaddress.ipaddress)

        isolated_network.update(self.apiclient, guestvmcidr=guest_vm_cidr)
        matchNetworkGuestVmCIDR(self, isolated_network.id, guest_vm_cidr)
        vms = VirtualMachine.list(self.apiclient, id=virtual_machine_1.id)
        self.assertEqual(validateList(vms)[0], PASS, "vm list validation failed")
        self.assertEqual(vms[0].nic[0].ipaddress,
                          virtual_machine_1.ipaddress,
                           "VM IP should not change after reservation")
        try:
            virtual_machine_2 = createVirtualMachine(self, network_id=isolated_network.id)
            if netaddr.IPAddress(virtual_machine_2.ipaddress) not in netaddr.IPNetwork(guest_vm_cidr):
                self.fail("Newly created VM doesn't get IP from reserverd CIDR")
        except Exception as e:
            self.fail("VM creation failed, cannot validate the condition: %s" % e)

        result = createNetworkRulesForVM(self.apiclient, virtual_machine_2, value,
                                         self.account, self.testData)
        if result[0] == FAIL:
            self.fail("Failed to create network rules for VM: %s" % result[1])
        else:
            ipaddress_2 = result[1]
        virtual_machine_2.get_ssh_client(ipaddress=ipaddress_2.ipaddress.ipaddress)
        return

    @unittest.skip("Skip - WIP")
    @attr(tags=["advanced"])
    def test_RVR_network(self):
        """ Test IP reservation in network with RVR
        # steps
        # 1. create vm in isolated network with RVR and ip in guestvmcidr
        # 2. update guestvmcidr
        # 3. List routers and stop the master router, wait till backup router comes up
        # 4. create another VM
        #
        # validation
        # 1. Guest vm cidr should be successfully updated with correct value
        # 2. Existing guest vm ip should not be changed after reservation
        # 3. Newly created VM should get ip in guestvmcidr
        # 4. Verify that the network has two routers associated with it
        # 5. Backup router should come up when master router is stopped"""

        subnet = "10.1."+str(random.randrange(1,254))
        gateway = subnet +".1"
        resultSet = createIsolatedNetwork(self, self.isolated_network_offering_RVR.id,
                                          gateway=gateway)
        if resultSet[0] == FAIL:
            self.fail("Failed to create isolated network")
        else:
            isolated_network_RVR= resultSet[1]
        guest_vm_cidr = subnet+".0/29"

        try:
            virtual_machine_1 = createVirtualMachine(self, network_id=isolated_network_RVR.id,
                    ip_address=subnet+".3")
        except Exception as e:
            self.fail("VM creation failed: %s" % e)

        isolated_network_RVR.update(self.apiclient, guestvmcidr=guest_vm_cidr)
        matchNetworkGuestVmCIDR(self, isolated_network_RVR.id, guest_vm_cidr)
        vms = VirtualMachine.list(self.apiclient,
                                       id=virtual_machine_1.id)
        self.assertEqual(validateList(vms)[0], PASS, "vm list validation failed")
        self.assertEqual(vms[0].nic[0].ipaddress,
                          virtual_machine_1.ipaddress,
                           "VM IP should not change after reservation")

        self.debug("Listing routers for network: %s" % isolated_network_RVR.name)
        routers = Router.list(self.apiclient, networkid=isolated_network_RVR.id, listall=True)
        self.assertEqual(validateList(routers)[0], PASS, "Routers list validation failed")
        self.assertEqual(len(routers), 2, "Length of the list router should be 2 (Backup & master)")

        if routers[0].redundantstate == MASTER:
            master_router = routers[0]
            backup_router = routers[1]
        else:
            master_router = routers[1]
            backup_router = routers[0]

        self.debug("Stopping router ID: %s" % master_router.id)

        try:
            Router.stop(self.apiclient, id=master_router.id)
        except Exception as e:
            self.fail("Failed to stop master router due to error %s" % e)

        # wait for VR to update state
        wait_for_cleanup(self.apiclient, ["router.check.interval"])

        result = verifyRouterState(master_router.id, [UNKNOWN,FAULT])
        if result[0] == FAIL:
            self.fail(result[1])
        result = verifyRouterState(backup_router.id, [MASTER])
        if result[0] == FAIL:
            self.fail(result[1])

        try:
            virtual_machine_2 = createVirtualMachine(self, network_id=isolated_network_RVR.id)
            if netaddr.IPAddress(virtual_machine_2.ipaddress) not in netaddr.IPNetwork(guest_vm_cidr):
                self.fail("Newly created VM doesn't get IP from reserverd CIDR")
        except Exception as e:
            self.fail("VM creation failed, cannot validate the condition: %s" % e)
        return

    @attr(tags=["advanced"])
    def test_ip_reservation_in_multiple_networks_same_account(self):
        """ Test IP reservation in multiple networks created in same account
        # steps
        # 1. Create two isolated networks with user defined cidr in same account
        # Test below conditions for both the networks in the account
        # 2. Create vm in persistent isolated network with ip in guestvmcidr
        # 3. Update guestvmcidr
        # 4. Create another VM
        #
        # validation
        # 1. Guest vm cidr should be successfully updated with correct value
        # 2. Existing guest vm ip should not be changed after reservation
        # 3. Newly created VM should get ip in guestvmcidr"""

        account_1 = Account.create(self.apiclient, self.testData["account"],
                                      domainid=self.domain.id)
        self.cleanup.append(account_1)

        random_subnet = str(random.randrange(1,254))
        gateway = "10.1." + random_subnet +".1"
        isolated_network_1 = Network.create(self.apiclient, self.testData["isolated_network"],
                         networkofferingid=self.isolated_network_offering.id,accountid=account_1.name,
                         domainid=self.domain.id,zoneid=self.zone.id,
                         gateway=gateway, netmask='255.255.255.0')
        guest_vm_cidr = "10.1."+random_subnet+".0/29"

        try:
            virtual_machine_1 = VirtualMachine.create(self.apiclient, self.testData["virtual_machine"],
                                            networkids=isolated_network_1.id, serviceofferingid=self.service_offering.id,
                                            accountid=account_1.name, domainid=self.domain.id,
                                            ipaddress="10.1."+random_subnet+".3")
        except Exception as e:
            self.fail("VM creation failed: %s" % e)

        isolated_network_1.update(self.apiclient, guestvmcidr=guest_vm_cidr)
        matchNetworkGuestVmCIDR(self, isolated_network_1.id, guest_vm_cidr)
        vms = VirtualMachine.list(self.apiclient,
                                       id=virtual_machine_1.id)
        self.assertEqual(validateList(vms)[0], PASS, "vm list validation failed")
        self.assertEqual(vms[0].nic[0].ipaddress,
                          virtual_machine_1.ipaddress,
                           "VM IP should not change after reservation")

        try:
            virtual_machine_2 = VirtualMachine.create(self.apiclient, self.testData["virtual_machine"],
                                            networkids=isolated_network_1.id, serviceofferingid=self.service_offering.id,
                                            accountid=account_1.name, domainid=self.domain.id)
            if netaddr.IPAddress(virtual_machine_2.ipaddress) not in netaddr.IPNetwork(guest_vm_cidr):
                self.fail("Newly created VM doesn't get IP from reserverd CIDR")
        except Exception as e:
            self.fail("VM creation failed, cannot validate the condition: %s" % e)

        random_subnet = str(random.randrange(1,254))
        gateway = "10.1." + random_subnet +".1"
        isolated_network_2 = Network.create(self.apiclient, self.testData["isolated_network"],
                         networkofferingid=self.isolated_network_offering.id,accountid=account_1.name,
                         domainid=self.domain.id,zoneid=self.zone.id,
                         gateway=gateway, netmask='255.255.255.0')
        guest_vm_cidr = "10.1."+random_subnet+".0/29"

        try:
            virtual_machine_3 = VirtualMachine.create(self.apiclient, self.testData["virtual_machine"],
                                            networkids=isolated_network_2.id, serviceofferingid=self.service_offering.id,
                                            accountid=account_1.name, domainid=self.domain.id,
                                            ipaddress="10.1."+random_subnet+".3")
        except Exception as e:
            self.fail("VM creation failed: %s" % e)

        isolated_network_2.update(self.apiclient, guestvmcidr=guest_vm_cidr)
        matchNetworkGuestVmCIDR(self, isolated_network_2.id, guest_vm_cidr)
        vms = VirtualMachine.list(self.apiclient,
                                       id=virtual_machine_3.id)
        self.assertEqual(validateList(vms)[0], PASS, "vm list validation failed")
        self.assertEqual(vms[0].nic[0].ipaddress,
                          virtual_machine_3.ipaddress,
                           "VM IP should not change after reservation")

        try:
            virtual_machine_4 = VirtualMachine.create(self.apiclient, self.testData["virtual_machine"],
                                            networkids=isolated_network_2.id, serviceofferingid=self.service_offering.id,
                                            accountid=account_1.name, domainid=self.domain.id)
            if netaddr.IPAddress(virtual_machine_4.ipaddress) not in netaddr.IPNetwork(guest_vm_cidr):
                self.fail("Newly created VM doesn't get IP from reserverd CIDR")
        except Exception as e:
            self.fail("VM creation failed, cannot validate the condition: %s" % e)
        return

@ddt
class TestRestartNetwork(cloudstackTestCase):
    """Test Restart Network
    """
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestRestartNetwork, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill services from the external config file
        cls.testData = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.testData["ostype"]
                            )
        if cls.template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.testData["ostype"]
        cls.testData["domainid"] = cls.domain.id
        cls.testData["zoneid"] = cls.zone.id
        cls.testData["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testData["virtual_machine"]["template"] = cls.template.id
        cls._cleanup = []
        try:
            cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.testData["service_offering"]
                                            )
            cls._cleanup.append(cls.service_offering)
            cls.isolated_network_offering = CreateEnabledNetworkOffering(cls.api_client,
                                                cls.testData["isolated_network_offering"])
            cls._cleanup.append(cls.isolated_network_offering)
            cls.isolated_persistent_network_offering = CreateEnabledNetworkOffering(cls.api_client,
                                                        cls.testData["nw_off_isolated_persistent"])
            cls._cleanup.append(cls.isolated_persistent_network_offering)
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Failure in setUpClass: %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        try:
            self.account = Account.create(self.apiclient, self.testData["account"],
                                      domainid=self.domain.id)
            self.cleanup.append(self.account)
        except Exception as e:
            self.skipTest("Failed to create account: %s" % e)
        return

    def tearDown(self):
        try:
            # Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @data(True, False)
    @attr(tags=["advanced"])
    def test_restart_network_with_cleanup(self, value):
        """ Test IP reservation rules with network restart operation
        # steps
        # 1. Create vm in isolated network with ip in guestvmcidr
        # 2. Update guestvmcidr
        # 3. Restart network with cleanup True/False
        # 4. Deploy another VM in the network
        #
        # validation
        # 1. Guest vm cidr should be successfully updated with correct value
        # 2. Existing guest vm ip should not be changed after reservation
        # 3. Network should be restarted successfully with and without cleanup
        # 4. Newly created VM should get ip in guestvmcidr"""

        subnet = "10.1."+str(random.randrange(1,254))
        gateway = subnet +".1"
        resultSet = createIsolatedNetwork(self, self.isolated_network_offering.id,
                                          gateway=gateway)
        if resultSet[0] == FAIL:
            self.fail("Failed to create isolated network")
        else:
            isolated_network= resultSet[1]
        guest_vm_cidr = subnet+".0/29"

        try:
            virtual_machine_1 = createVirtualMachine(self, network_id=isolated_network.id,
                    ip_address=subnet+".3")
        except Exception as e:
            self.fail("VM creation failed: %s" % e)

        isolated_network.update(self.apiclient, guestvmcidr=guest_vm_cidr)
        matchNetworkGuestVmCIDR(self, isolated_network.id, guest_vm_cidr)
        vms = VirtualMachine.list(self.apiclient,
                                       id=virtual_machine_1.id)
        self.assertEqual(validateList(vms)[0], PASS, "vm list validation failed")
        self.assertEqual(vms[0].nic[0].ipaddress,
                          virtual_machine_1.ipaddress,
                           "VM IP should not change after reservation")

        #Restart Network
        isolated_network.restart(self.apiclient, cleanup=value)

        try:
            virtual_machine_2 = createVirtualMachine(self, network_id=isolated_network.id)
            if netaddr.IPAddress(virtual_machine_2.ipaddress) not in netaddr.IPNetwork(guest_vm_cidr):
                self.fail("Newly created VM doesn't get IP from reserverd CIDR")
        except Exception as e:
            self.fail("VM creation failed, cannot validate the condition: %s" % e)
        return

@ddt
class TestUpdateIPReservation(cloudstackTestCase):
    """Test Updating IP reservation multiple times
    """
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestUpdateIPReservation, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill services from the external config file
        cls.testData = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.testData["ostype"]
                            )
        if cls.template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.testData["ostype"]
        cls.testData["domainid"] = cls.domain.id
        cls.testData["zoneid"] = cls.zone.id
        cls.testData["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testData["virtual_machine"]["template"] = cls.template.id
        cls._cleanup = []
        try:
            cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.testData["service_offering"]
                                            )
            cls._cleanup.append(cls.service_offering)
            cls.isolated_network_offering = CreateEnabledNetworkOffering(cls.api_client,
                                            cls.testData["isolated_network_offering"])
            cls._cleanup.append(cls.isolated_network_offering)
            cls.isolated_persistent_network_offering = CreateEnabledNetworkOffering(cls.api_client,
                                                        cls.testData["nw_off_isolated_persistent"])
            cls._cleanup.append(cls.isolated_persistent_network_offering)
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Failure in setUpClass: %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        try:
            self.account = Account.create(self.apiclient, self.testData["account"],
                                      domainid=self.domain.id)
            self.cleanup.append(self.account)
        except Exception as e:
            self.skipTest("Failed to create account: %s" % e)
        return

    def tearDown(self):
        try:
            # Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @data("existingVmInclusive", "existingVmExclusive")
    @attr(tags=["advanced"])
    def test_update_network_guestvmcidr(self, value):
        """ Test updating guest vm cidr of the network after
        VMs are already deployed in previous guest VM cidr

        # steps
        # 1. Create isolated network with user defined cidr
        # 2. Deploy VM in the network
        # 3. Try to update the guestvmcidr of the network with VM ip in the guestvmcidr and
        #    deploy another VM
        # 4. Try to update the guestvmcidr of the network with VM ip outside the guestvmcidr
        #
        # validation
        # 1. When vm IP is in the guestvmcidr, updation should be successful and
        #    new VM should get IP from this range
        # 2. When VM IP is outside the guestvmcidr, updation should be unsuccessful"""

        random_subnet = str(random.randrange(1,254))
        gateway = "10.1." + random_subnet +".1"
        resultSet = createIsolatedNetwork(self, self.isolated_network_offering.id,
                                          gateway=gateway)
        if resultSet[0] == FAIL:
            self.fail("Failed to create isolated network")
        else:
            isolated_network= resultSet[1]
        guest_vm_cidr = "10.1."+random_subnet+".0/29"

        try:
            virtual_machine_1 = createVirtualMachine(self, network_id=isolated_network.id,
                    ip_address="10.1."+random_subnet+".3")
        except Exception as e:
            self.fail("VM creation failed: %s" % e)

        isolated_network.update(self.apiclient, guestvmcidr=guest_vm_cidr)
        matchNetworkGuestVmCIDR(self, isolated_network.id, guest_vm_cidr)
        vms = VirtualMachine.list(self.apiclient,
                                       id=virtual_machine_1.id)
        self.assertEqual(validateList(vms)[0], PASS, "vm list validation failed")
        self.assertEqual(vms[0].nic[0].ipaddress,
                          virtual_machine_1.ipaddress,
                           "VM IP should not change after reservation")

        try:
            virtual_machine_2 = createVirtualMachine(self, network_id=isolated_network.id)
            if netaddr.IPAddress(virtual_machine_2.ipaddress) not in netaddr.IPNetwork(guest_vm_cidr):
                self.fail("Newly created VM doesn't get IP from reserverd CIDR")
        except Exception as e:
            self.fail("VM creation failed, cannot validate the condition: %s" % e)

        # Update guest vm cidr of network again
        if value == "existingVmExclusive":
            guest_vm_cidr = "10.1."+random_subnet+".10/29"

            try:
                isolated_network.update(self.apiclient, guestvmcidr=guest_vm_cidr)
                self.fail("Network updation should fail")
            except Exception as e:
                self.debug("Failed to update guest VM cidr of network: %s" % e)
        elif value == "existingVmInclusive":
            guest_vm_cidr = "10.1."+random_subnet+".0/28"

            try:
                isolated_network.update(self.apiclient, guestvmcidr=guest_vm_cidr)
            except Exception as e:
                self.fail("Failed to update guest VM cidr of network: %s" % e)

            matchNetworkGuestVmCIDR(self, isolated_network.id, guest_vm_cidr)

            try:
                virtual_machine_3 = createVirtualMachine(self, network_id=isolated_network.id)
                if netaddr.IPAddress(virtual_machine_3.ipaddress) not in netaddr.IPNetwork(guest_vm_cidr):
                    self.fail("Newly created VM doesn't get IP from reserverd CIDR")
            except Exception as e:
                self.fail("VM creation failed, cannot validate the condition: %s" % e)
        return

@ddt
class TestRouterOperations(cloudstackTestCase):
    """Test Router operations of network with IP reservation
    """
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestRouterOperations, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill services from the external config file
        cls.testData = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.testData["ostype"]
                            )
        if cls.template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.testData["ostype"]
        cls.testData["domainid"] = cls.domain.id
        cls.testData["zoneid"] = cls.zone.id
        cls.testData["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testData["virtual_machine"]["template"] = cls.template.id
        cls._cleanup = []
        try:
            cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.testData["service_offering"]
                                            )
            cls._cleanup.append(cls.service_offering)
            cls.isolated_network_offering = CreateEnabledNetworkOffering(cls.api_client,
                                                cls.testData["isolated_network_offering"])
            cls._cleanup.append(cls.isolated_network_offering)
            cls.isolated_persistent_network_offering = CreateEnabledNetworkOffering(cls.api_client,
                                                        cls.testData["nw_off_isolated_persistent"])
            cls._cleanup.append(cls.isolated_persistent_network_offering)
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Failure in setUpClass: %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        try:
            self.account = Account.create(self.apiclient, self.testData["account"],
                                      domainid=self.domain.id)
            self.cleanup.append(self.account)
        except Exception as e:
            self.skipTest("Failed to create account: %s" % e)
        return

    def tearDown(self):
        try:
            # Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"])
    def test_reservation_after_router_restart(self):
        """ Test IP reservation working before and after router is restarted
        # steps
        # 1. Update guestvmcidr of persistent isolated network
        # 2. Reboot router
        #
        # validation
        # 1. Guest vm cidr should be successfully updated with correct value
        # 2. Network cidr should remain same after router restart"""
        subnet = "10.1."+str(random.randrange(1,254))
        gateway = subnet +".1"
        resultSet = createIsolatedNetwork(self, self.isolated_persistent_network_offering.id,
                                          gateway=gateway)
        if resultSet[0] == FAIL:
            self.fail("Failed to create isolated network")
        else:
            isolated_network= resultSet[1]
        guest_vm_cidr = subnet+".0/29"

        isolated_network.update(self.apiclient, guestvmcidr=guest_vm_cidr)
        matchNetworkGuestVmCIDR(self, isolated_network.id, guest_vm_cidr)

        routers = Router.list(self.apiclient,
                             networkid=isolated_network.id,
                             listall=True)
        self.assertEqual(validateList(routers)[0], PASS, "routers list validation failed")
        if not routers:
            self.fail("Router list should not be empty")

        Router.reboot(self.apiclient, routers[0].id)
        networks = Network.list(self.apiclient, id=isolated_network.id)
        self.assertEqual(validateList(networks)[0], PASS, "networks list validation failed")
        self.assertEqual(networks[0].cidr, guest_vm_cidr, "guestvmcidr should match after router reboot")
        return

    @attr(tags=["advanced"])
    def test_destroy_recreate_router(self):
        """ Test IP reservation working after destroying and recreating router
        # steps
        # 1. Create isolated network and deploy VM in it and update network with
        #    guestvmcidr
        # 2. List the router associated with network and destroy the router
        # 3. Restart the network
        # 3. Deploy another VM in the network
        #
        # validation
        # 1. Guest vm cidr should be successfully updated with correct value
        # 2. existing guest vm ip should not be changed after reservation
        # 3. Router should be destroyed and recreated when network is restarted
        # 4. New VM should be deployed in the guestvmcidr"""

        subnet = "10.1."+str(random.randrange(1,254))
        gateway = subnet +".1"
        resultSet = createIsolatedNetwork(self, self.isolated_network_offering.id,
                                          gateway=gateway)
        if resultSet[0] == FAIL:
            self.fail("Failed to create isolated network")
        else:
            isolated_network= resultSet[1]
        guest_vm_cidr = subnet+".0/29"

        try:
            virtual_machine_1 = createVirtualMachine(self, network_id=isolated_network.id,
                    ip_address=subnet+".3")
        except Exception as e:
            self.fail("VM creation failed: %s" % e)

        isolated_network.update(self.apiclient, guestvmcidr=guest_vm_cidr)
        matchNetworkGuestVmCIDR(self, isolated_network.id, guest_vm_cidr)
        vms = VirtualMachine.list(self.apiclient,
                                       id=virtual_machine_1.id)
        self.assertEqual(validateList(vms)[0], PASS, "vm list validation failed")
        self.assertEqual(vms[0].nic[0].ipaddress,
                          virtual_machine_1.ipaddress,
                           "VM IP should not change after reservation")

        # List router and destroy it
        routers = Router.list(self.apiclient, networkid=isolated_network.id, listall=True)
        self.assertEqual(validateList(routers)[0], PASS, "Routers list validation failed")

        # Destroy Router
        Router.destroy(self.apiclient, id=routers[0].id)

        #Restart Network
        isolated_network.restart(self.apiclient)

        try:
            virtual_machine_2 = createVirtualMachine(self, network_id=isolated_network.id)
            if netaddr.IPAddress(virtual_machine_2.ipaddress) not in netaddr.IPNetwork(guest_vm_cidr):
                self.fail("Newly created VM doesn't get IP from reserverd CIDR")
        except Exception as e:
            self.fail("VM creation failed, cannot validate the condition: %s" % e)
        return

@ddt
class TestFailureScnarios(cloudstackTestCase):
    """Test failure scenarios related to IP reservation in network
    """
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestFailureScnarios, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill services from the external config file
        cls.testData = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.testData["ostype"]
                            )
        if cls.template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.testData["ostype"]
        cls.testData["domainid"] = cls.domain.id
        cls.testData["zoneid"] = cls.zone.id
        cls.testData["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testData["virtual_machine"]["template"] = cls.template.id
        cls._cleanup = []
        try:
            cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.testData["service_offering"]
                                            )
            cls._cleanup.append(cls.service_offering)
            cls.isolated_network_offering = CreateEnabledNetworkOffering(cls.api_client,
                                            cls.testData["isolated_network_offering"])
            cls._cleanup.append(cls.isolated_network_offering)
            cls.isolated_persistent_network_offering = CreateEnabledNetworkOffering(cls.api_client,
                                                        cls.testData["nw_off_isolated_persistent"])
            cls._cleanup.append(cls.isolated_persistent_network_offering)

            cls.testData["shared_network_offering"]["specifyVlan"] = "True"
            cls.testData["shared_network_offering"]["specifyIpRanges"] = "True"

            #Create Network Offering
            cls.shared_network_offering = NetworkOffering.create(cls.api_client,
                                        cls.testData["shared_network_offering"],
                                        conservemode=False)
            cls._cleanup.append(cls.shared_network_offering)

            #Update network offering state from disabled to enabled.
            NetworkOffering.update(cls.shared_network_offering,cls.api_client,state="enabled")
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Failure in setUpClass: %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        try:
            self.account = Account.create(self.apiclient, self.testData["account"],
                                      domainid=self.domain.id)
            self.cleanup.append(self.account)
        except Exception as e:
            self.skipTest("Failed to create account: %s" % e)
        return

    def tearDown(self):
        try:
            # Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_network_not_implemented(self):
        # steps
        # 1. update guestvmcidr of isolated network (non persistent)
        #
        # validation
        # should throw exception as network is not in implemented state as no vm is created

        networkOffering = self.isolated_network_offering
        resultSet = createIsolatedNetwork(self, networkOffering.id)
        if resultSet[0] == FAIL:
            self.fail("Failed to create isolated network")
        else:
            isolated_network = resultSet[1]

        with self.assertRaises(Exception):
            isolated_network.update(self.apiclient, guestvmcidr="10.1.1.0/26")
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_vm_create_after_reservation(self):
        # steps
        # 1. create vm in persistent isolated network with ip in guestvmcidr
        # 2. update guestvmcidr
        # 3. create another VM
        #
        # validation
        # 1. guest vm cidr should be successfully updated with correct value
        # 2. existing guest vm ip should not be changed after reservation
        # 3. newly created VM should get ip in guestvmcidr

        networkOffering = self.isolated_persistent_network_offering
        subnet = "10.1."+str(random.randrange(1,254))
        gateway = subnet + ".1"
        isolated_persistent_network = None
        resultSet = createIsolatedNetwork(self, networkOffering.id, gateway=gateway)
        if resultSet[0] == FAIL:
            self.fail("Failed to create isolated network")
        else:
            isolated_persistent_network = resultSet[1]
        guest_vm_cidr = subnet +".0/29"
        virtual_machine_1 = None
        try:
            virtual_machine_1 = VirtualMachine.create(self.apiclient,
                                                          self.testData["virtual_machine"],
                                                          networkids=isolated_persistent_network.id,
                                                          serviceofferingid=self.service_offering.id,
                                                          accountid=self.account.name,
                                                          domainid=self.domain.id,
                                                          ipaddress=subnet+".3"
                                                          )
        except Exception as e:
            self.fail("VM creation fails in network: %s" % e)

        update_response = Network.update(isolated_persistent_network, self.apiclient, id=isolated_persistent_network.id, guestvmcidr=guest_vm_cidr)
        self.assertEqual(guest_vm_cidr, update_response.cidr, "cidr in response is not as expected")
        vm_list = VirtualMachine.list(self.apiclient,
                                       id=virtual_machine_1.id)
        self.assertEqual(isinstance(vm_list, list),
                         True,
                         "VM list response in not a valid list")
        self.assertEqual(vm_list[0].nic[0].ipaddress,
                          virtual_machine_1.ipaddress,
                           "VM IP should not change after reservation")
        try:
            virtual_machine_2 = VirtualMachine.create(self.apiclient,
                                                          self.testData["virtual_machine"],
                                                          networkids=isolated_persistent_network.id,
                                                          serviceofferingid=self.service_offering.id,
                                                          accountid=self.account.name,
                                                          domainid=self.domain.id
                                                          )
            if netaddr.IPAddress(virtual_machine_2.ipaddress) not in netaddr.IPNetwork(guest_vm_cidr):
                self.fail("Newly created VM doesn't get IP from reserverd CIDR")
        except Exception as e:
            self.skipTest("VM creation fails, cannot validate the condition: %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_reservation_after_router_restart(self):
        # steps
        # 1. update guestvmcidr of persistent isolated network
        # 2. reboot router
        #
        # validation
        # 1. guest vm cidr should be successfully updated with correct value
        # 2. network cidr should remain same after router restart
        networkOffering = self.isolated_persistent_network_offering
        subnet = "10.1."+str(random.randrange(1,254))
        gateway = subnet + ".1"
        isolated_persistent_network = None
        resultSet = createIsolatedNetwork(self, networkOffering.id, gateway=gateway)
        if resultSet[0] == FAIL:
            self.fail("Failed to create isolated network")
        else:
            isolated_persistent_network = resultSet[1]

        response = verifyNetworkState(self.apiclient, isolated_persistent_network.id,\
                        "implemented")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        guest_vm_cidr = subnet +".0/29"

        update_response = Network.update(isolated_persistent_network, self.apiclient, id=isolated_persistent_network.id, guestvmcidr=guest_vm_cidr)
        self.assertEqual(guest_vm_cidr, update_response.cidr, "cidr in response is not as expected")

        routers = Router.list(self.apiclient,
                             networkid=isolated_persistent_network.id,
                             listall=True)
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return valid response"
                    )
        if not routers:
            self.skipTest("Router list should not be empty, skipping test")

        Router.reboot(self.apiclient, routers[0].id)
        networks = Network.list(self.apiclient, id=isolated_persistent_network.id)
        self.assertEqual(
                    isinstance(networks, list),
                    True,
                    "list Networks should return valid response"
                    )
        self.assertEqual(networks[0].cidr, guest_vm_cidr, "guestvmcidr should match after router reboot")
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_vm_create_outside_cidr_after_reservation(self):
        # steps
        # 1. update guestvmcidr of persistent isolated network
        # 2. create another VM with ip outside guestvmcidr
        #
        # validation
        # 1. guest vm cidr should be successfully updated with correct value
        # 2  newly created VM should not be created and result in exception
        networkOffering = self.isolated_persistent_network_offering
        subnet = "10.1."+str(random.randrange(1,254))
        gateway = subnet + ".1"
        isolated_persistent_network = None
        resultSet = createIsolatedNetwork(self, networkOffering.id, gateway=gateway)
        if resultSet[0] == FAIL:
            self.fail("Failed to create isolated network")
        else:
            isolated_persistent_network = resultSet[1]
        guest_vm_cidr = subnet +".0/29"
        update_response = Network.update(isolated_persistent_network, self.apiclient, id=isolated_persistent_network.id, guestvmcidr=guest_vm_cidr)
        self.assertEqual(guest_vm_cidr, update_response.cidr, "cidr in response is not as expected")
        with self.assertRaises(Exception):
            VirtualMachine.create(self.apiclient,
                                  self.testData["virtual_machine"],
                                  networkids=isolated_persistent_network.id,
                                  serviceofferingid=self.service_offering.id,
                                  accountid=self.account.name,
                                  domainid=self.domain.id,
                                  ipaddress="10.1.1.9"
                                  )
        return
