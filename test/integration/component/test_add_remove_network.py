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

"""
    P1 tests for Add Remove Network to VM

    Test Plan: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Add+Remove+networks+to+VM+Test+cases

    Issue Link: https://issues.apache.org/jira/browse/CLOUDSTACK-645

    Feature Specifications: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Add+Remove+Networks+to+VMs
"""

# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from ddt import ddt, data
from marvin.lib.base import (
    Account,
    Domain,
    ServiceOffering,
    VirtualMachine,
    NetworkOffering,
    Network,
    VpcOffering,
    VPC,
    PublicIPAddress,
    FireWallRule,
    NATRule
)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_virtual_machines,
                               list_events,
                               list_zones,
                               get_free_vlan,
                               update_resource_limit,
                               list_nat_rules
                               )

from marvin.lib.utils import (validateList,
                              random_gen,
                              get_hypervisor_type,
                              cleanup_resources)

from marvin.cloudstackAPI import (addNicToVirtualMachine,
                                  removeNicFromVirtualMachine,
                                  updateDefaultNicForVirtualMachine)

from marvin.codes import PASS
import random
import time


class Services:
    """Test Add Remove Network Services
    """

    def __init__(self):
        self.services = {
            "sleep": 60,
            "ostype": "CentOS 5.3 (64-bit)",
            # Cent OS 5.3 (64 bit)

            "isolated_network_offering": {
                "name": 'Test Isolated Network offering',
                "displaytext": 'Test Isolated Network offering',
                "guestiptype": 'Isolated',
                "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding',
                "traffictype": 'GUEST',
                "availability": 'Optional',
                "serviceProviderList": {
                    "Dhcp": 'VirtualRouter',
                    "Dns": 'VirtualRouter',
                    "SourceNat": 'VirtualRouter',
                    "PortForwarding": 'VirtualRouter',
                },
            },

            "shared_network_offering": {
                "name": 'Test Shared Network Offering',
                "displaytext": 'Test Shared Network Offering',
                "guestiptype": 'Shared',
                "supportedservices": 'Dhcp,Dns,UserData',
                "specifyVlan": "True",
                "specifyIpRanges": "True",
                "traffictype": 'GUEST',
                "serviceProviderList": {
                    "Dhcp": 'VirtualRouter',
                    "Dns": 'VirtualRouter',
                    "UserData": 'VirtualRouter'
                },
            },

            "shared_network": {
                "name": "Test Shared Network",
                "displaytext": "Test Shared Network",
                "gateway": "172.16.17.1",
                "netmask": "255.255.255.0",
                "startip": "172.16.17.2",
                "endip": "172.16.17.20",
            },

            "shared_network_2": {
                "name": "Test Shared Network",
                "displaytext": "Test Shared Network",
                "gateway": "172.16.18.1",
                "netmask": "255.255.255.0",
                "startip": "172.16.18.2",
                "endip": "172.16.18.20",
            },

            "isolated_network": {
                "name": "Test Isolated Network",
                "displaytext": "Test Isolated Network",
            },

            "service_offering": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100,
                # in MHz
                "memory": 256,
                # In MBs
            },

            "account": {
                "email": "test@test.com",
                "firstname": "Test_add_remove_network_vm",
                "lastname": "User",
                "username": "test_add_remove_network_vm",
                "password": "password",
            },
            "domain": {
                "name": "Domain_add_nw_to_vm",
            },
            "virtual_machine": {
                "displayname": "testserver",
                "username": "root",  # VM creds for SSH
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'XenServer',
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },

            "vpc_offering": {
                "name": 'VPC off add remove network',
                "displaytext": 'VPC off add remove network',
                "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Lb,UserData,StaticNat,NetworkACL',
            },

            "vpc": {
                "name": "TestVPC add remove network",
                "displaytext": "TestVPC add remove network",
                "cidr": '10.0.0.1/24'
            },
            "natrule": {
                "privateport": 22,
                "publicport": 22,
                "protocol": "TCP"
            },
        }


@ddt
class TestAddNetworkToVirtualMachine(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestAddNetworkToVirtualMachine, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services

        hypervisor = get_hypervisor_type(cls.api_client)
        if hypervisor.lower() not in ["xenserver", "kvm"]:
            raise unittest.SkipTest("This feature is supported only on XenServer and KVM")

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        template = get_template(cls.api_client, cls.zone.id, cls.services["ostype"])
        # Set Zones and disk offerings
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id

        # Create Accounts & networks
        cls.services["isolated_network"]["zoneid"] = cls.zone.id
        cls.services["shared_network"]["zoneid"] = cls.zone.id
        cls._cleanup = []

        cls.account = Account.create(cls.api_client, cls.services["account"], domainid=cls.domain.id)
        cls._cleanup.append(cls.account)

        cls.service_offering = ServiceOffering.create(cls.api_client, cls.services["service_offering"])
        cls._cleanup.append(cls.service_offering)

        cls.virtual_machine = VirtualMachine.create(cls.api_client, cls.services["virtual_machine"], accountid=cls.account.name,
                                                    domainid=cls.account.domainid, serviceofferingid=cls.service_offering.id,
                                                    mode=cls.zone.networktype)

        cls.defaultNetworkId = cls.virtual_machine.nic[0].networkid

        # Create Shared Network Offering
        cls.isolated_network_offering = NetworkOffering.create(cls.api_client, cls.services["isolated_network_offering"])
        cls._cleanup.append(cls.isolated_network_offering)
        # Enable Isolated Network offering
        cls.isolated_network_offering.update(cls.api_client, state='Enabled')

        # Create Shared Network Offering
        cls.shared_network_offering = NetworkOffering.create(cls.api_client, cls.services["shared_network_offering"])
        # Enable shared Network offering
        cls.shared_network_offering.update(cls.api_client, state='Enabled')

        cls.isolated_network = Network.create(cls.api_client, cls.services["isolated_network"], cls.account.name,
                                              cls.account.domainid, networkofferingid=cls.isolated_network_offering.id)

        cls.services["shared_network"]["vlan"] = get_free_vlan(cls.api_client, cls.zone.id)[1]

        shared_network_subnet_number = random.randrange(1, 254)

        cls.services["shared_network"]["gateway"] = "172.16." + str(shared_network_subnet_number) + ".1"
        cls.services["shared_network"]["startip"] = "172.16." + str(shared_network_subnet_number) + ".2"
        cls.services["shared_network"]["endip"] = "172.16." + str(shared_network_subnet_number) + ".20"

        cls.shared_nw_endip = cls.services["shared_network"]["endip"]

        cls.shared_network = Network.create(cls.api_client, cls.services["shared_network"], cls.account.name,
                                            cls.account.domainid, networkofferingid=cls.shared_network_offering.id)
        cls._cleanup.append(cls.shared_network)
        cls._cleanup.append(cls.shared_network_offering)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.addednics = []

        shared_network_subnet_number = random.randrange(1, 254)

        self.services["shared_network"]["gateway"] = "172.16." + str(shared_network_subnet_number) + ".1"
        self.services["shared_network"]["startip"] = "172.16." + str(shared_network_subnet_number) + ".2"
        self.services["shared_network"]["endip"] = "172.16." + str(shared_network_subnet_number) + ".20"

        self.services["shared_network_2"]["gateway"] = "172.16." + str(shared_network_subnet_number + 1) + ".1"
        self.services["shared_network_2"]["startip"] = "172.16." + str(shared_network_subnet_number + 1) + ".2"
        self.services["shared_network_2"]["endip"] = "172.16." + str(shared_network_subnet_number + 1) + ".20"
        self.cleanup = []

    def tearDown(self):
        try:
            for nic in self.addednics:
                self.virtual_machine.remove_nic(self.apiclient, nic.id)
            # Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Disable Network Offerings
            cls.isolated_network_offering.update(cls.api_client, state='Disabled')
            cls.shared_network_offering.update(cls.api_client, state='Disabled')

            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def addNetworkToVm(self, network, vm, ipaddress=None):
        """Add network to VM and check if new nic added in the VM"""

        self.debug("Adding %s Network: %s to virtual machine %s" %
                   (network.type, network.id, vm.id))
        vm.add_nic(self.apiclient, network.id, ipaddress=ipaddress)

        vm_list = list_virtual_machines(self.apiclient, id=vm.id)
        vm_list_validation_result = validateList(vm_list)
        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         vm_list_validation_result[2])

        self.debug("virtual machine nics: %s" % vm_list[0].nic)
        nics = [x for x in vm_list[0].nic if x.networkid == network.id]
        self.debug("Filtered nics list: %s:" % nics)

        # Only the nics added to self.virtual_machine should be added to this list
        # Nics added to his list are removed before execution of next test case because we are using
        # same virtual machine in all test cases, so it is important that the common
        # virtual machine should contain only the default nic whenever new test case
        # execution starts
        if vm.id == self.virtual_machine.id:
            self.addednics.append(nics[-1])

        self.assertTrue(len(nics) == 1, "nics list should contain the nic of added isolated network,\
                        the number of nics for the network should be 1, instead they are %s" %
                        len(nics))

        if ipaddress is not None:
            self.assertEqual(nics[0].ipaddress, ipaddress, "The ip address of nic does not match with \
                            the ip address passed while adding network to vm. ip address of nic is %s \
                            while passed ip address is %s" % (nics[0].ipaddress, ipaddress))

        return

    @attr(tags=["advanced", "dvs"])
    @data("isolated", "shared")
    def test_01_add_nw_running_vm(self, value):
        """Add network to running VM"""

        # 1. Deploy VM in an account
        # 2. Add isolated/shared network to the VM which is in running state

        # Validate the following:
        # 1. New nic is generated for the added network
        # 2. Event NIC.CREATE is generated

        network = None  # The network which we are adding to the vm

        if value == "isolated":
            network = self.isolated_network
        elif value == "shared":
            network = self.shared_network
        if network is None:
            self.skipTest("Network should not be none. Case not handled for Network of type %s" % value)

        self.addNetworkToVm(network, self.virtual_machine)
        self.debug("Retrieving the list of events matching 'NIC.CREATE' in account: %s" % self.account.name)
        events = list_events(self.apiclient, account=self.account.name, domainid=self.account.domainid,
                             type='NIC.CREATE')
        event_list_validation_result = validateList(events)
        self.assertEqual(event_list_validation_result[0], PASS, "event list validation failed due to %s" %
                         event_list_validation_result[2])
        self.debug("Events list contains event NIC.CREATE")

        return

    @attr(tags=["advanced", "dvs"])
    @data("isolated", "shared")
    def test_02_add_nw_stopped_vm(self, value):
        """Add network to stopped VM"""

        # 1. Deploy VM in an account
        # 2. Stop the VM
        # 3. Add isolated/shared network to the stopped VM

        # Validate the following:
        # 1. New nic is generated for the added network

        try:
            self.virtual_machine.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop VM: %s" % e)

        network = None  # The network which we are adding to the vm
        if value == "isolated":
            network = self.isolated_network
        elif value == "shared":
            network = self.shared_network
        if network is None:
            self.skipTest("Network should not be none. Case not handled for Network of type %s" % value)

        self.addNetworkToVm(network, self.virtual_machine)
        self.debug("Starting Virtual Machine: %s" % self.virtual_machine.id)
        self.virtual_machine.start(self.apiclient)

        return

    @attr(tags=["advanced", "dvs"])
    @data("isolated", "shared")
    def test_03_add_nw_multiple_times(self, value):
        """Add same network multiple times to running VM"""

        # 1. Deploy VM in an account
        # 2. Add isolated/shared network to the VM
        # 3. Try Adding same network again to the VM

        # Validate the following:
        # 1. Adding same network to vm multiple times fails

        network = None  # The network which we are adding to the vm
        if value == "isolated":
            network = self.isolated_network
        elif value == "shared":
            network = self.shared_network
        if network is None:
            self.skipTest("Network should not be none. Case not handled for Network of type %s" % value)

        try:
            virtual_machine = VirtualMachine.create(
                self.api_client, self.services["virtual_machine"],
                accountid=self.account.name, domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                mode=self.zone.networktype,
                networkids=[self.defaultNetworkId])
            self.cleanup.append(virtual_machine)
        except Exception as e:
            self.fail("Failed to deply virtual machine: %s" % e)

        # Adding network to vm for the first time
        self.addNetworkToVm(network, virtual_machine)

        # Trying to add same network to vm for the second time
        with self.assertRaises(Exception) as e:
            self.addNetworkToVm(network, virtual_machine)
            self.debug("Adding same network again failed with exception: %s" % e.exception)

        return

    @attr(tags=["advanced", "dvs"])
    @data("isolated")
    def test_04_vpc_nw_running_vm(self, value):
        """Add VPC network to running VM belonging to isolated network"""

        # 1. Deploy VM in an account
        # 2. Add isolated network to the VM
        # 3. Create VPC
        # 4. Try adding VPC to the VM

        # Validate the following:
        # 1. Adding VPC to vm should fail

        try:
            virtual_machine = VirtualMachine.create(
                self.api_client, self.services["virtual_machine"],
                accountid=self.account.name, domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                mode=self.zone.networktype,
                networkids=[self.defaultNetworkId])
            self.cleanup.append(virtual_machine)
        except Exception as e:
            self.fail("Failed to deply virtual machine: %s" % e)

        network = self.isolated_network
        self.addNetworkToVm(network, virtual_machine)

        self.debug("Creating VPC offering")
        vpc_off = VpcOffering.create(self.api_client, self.services["vpc_offering"])
        self.debug("Created VPC offering: %s" % vpc_off.id)
        self.debug("Enabling the VPC offering")
        vpc_off.update(self.apiclient, state='Enabled')
        self.debug("Creating VPC")
        vpc = VPC.create(self.apiclient, self.services["vpc"], vpcofferingid=vpc_off.id, zoneid=self.zone.id,
                         account=self.account.name, domainid=self.account.domainid)
        # Appending to cleanup list
        self.cleanup.append(vpc)
        self.cleanup.append(vpc_off)

        self.debug("Trying to add VPC to vm belonging to isolated network, this should fail")
        with self.assertRaises(Exception):
            virtual_machine.add_nic(self.apiclient, vpc.id)

        self.debug("Disabling vpc offering: %s" % vpc_off.id)
        vpc_off.update(self.apiclient, state='Disabled')
        return

    @attr(tags=["advanced", "dvs"])
    @data("isolated")
    def test_05_add_vpc_nw_stopped_vm(self, value):
        """Add VPC network to stopped VM belonging to isolated network"""

        # 1. Deploy VM in an account
        # 2. Stop the VM
        # 3. Add isolated network to the VM
        # 4. Create VPC
        # 5. Try adding VPC to the stopped VM

        # Validate the following:
        # 1. Adding VPC to vm should fail

        try:
            self.virtual_machine.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop virtual machine: %s" % e)

        self.addNetworkToVm(self.isolated_network, self.virtual_machine)

        self.debug("Creating VPC offering")
        vpc_off = VpcOffering.create(self.api_client, self.services["vpc_offering"])
        self.debug("Created VPC offering: %s" % vpc_off.id)
        self.debug("Enabling the VPC offering")
        vpc_off.update(self.apiclient, state='Enabled')
        self.debug("Creating VPC")
        vpc = VPC.create(self.apiclient, self.services["vpc"], vpcofferingid=vpc_off.id, zoneid=self.zone.id,
                         account=self.account.name, domainid=self.account.domainid)
        # Appending to cleanup list
        self.cleanup.append(vpc)
        self.cleanup.append(vpc_off)
        self.debug("Trying to add VPC to vm belonging to isolated network, this should fail")
        with self.assertRaises(Exception):
            self.virtual_machine.add_nic(self.apiclient, vpc.id)
        self.debug("Starting virtual machine")
        self.virtual_machine.start(self.apiclient)
        self.debug("Disabling vpc offering: %s" % vpc_off.id)
        vpc_off.update(self.apiclient, state='Disabled')

        return

    @attr(tags=["advanced", "dvs"])
    def test_06_add_nw_ipaddress_running_vm(self):
        """Add network and ip address to running VM"""

        # 1. Deploy VM in an account
        # 2. Add shared network and ip address to this VM

        # Validate the following:
        # 1. New nic gets added for the shared network
        # 2. The newly added nic has the ip address same as
        #    that passed while adding the network

        try:
            virtual_machine = VirtualMachine.create(
                self.api_client, self.services["virtual_machine"],
                accountid=self.account.name, domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                mode=self.zone.networktype,
                networkids=[self.defaultNetworkId])
            self.cleanup.append(virtual_machine)
        except Exception as e:
            self.fail("Failed to deply virtual machine: %s" % e)

        ipaddress = self.shared_nw_endip
        self.debug("Adding network to vm with ip address %s: " % ipaddress)
        self.addNetworkToVm(self.shared_network, virtual_machine, ipaddress=ipaddress)
        return

    @attr(tags=["advanced", "dvs"])
    def test_10_add_nw_invalid_ipaddress_running_vm(self):
        """Add network with invalid ip address to running VM"""

        # 1. Deploy VM in an account
        # 2. Add shared network with invalid ip address to this VM

        # Validate the following:
        # 1. Adding network to VM should fail because of invalid ip address

        ipaddress = "257.257.257.257"  # Invalid ip address
        self.debug("Adding network to vm with ip address %s: " % ipaddress)
        with self.assertRaises(Exception) as e:
            self.addNetworkToVm(self.shared_network, self.virtual_machine,
                                ipaddress=ipaddress)
            self.debug("API failed with exception: %s" % e.exception)

        return

    @attr(tags=["advanced", "dvs"])
    @data("isolated", "shared")
    def test_14_add_nw_different_account(self, value):
        """Add network to running VM"""

        # 1. Deploy VM in an account
        # 2. Create new account under same domain and create network in that account
        # 3. Add isolated/shared network belonging to other account to the VM in first account

        # Validate the following:
        # 1. Adding network should fail

        network = None  # The network which we are adding to the vm
        account = Account.create(self.apiclient, self.services["account"], domainid=self.domain.id)
        self.cleanup.append(account)

        if value == "isolated":
            network = Network.create(self.api_client, self.services["isolated_network"], account.name,
                                     account.domainid, networkofferingid=self.isolated_network_offering.id)
        elif value == "shared":
            self.services["shared_network_2"]["zoneid"] = self.zone.id
            self.services["shared_network_2"]["vlan"] = get_free_vlan(self.apiclient, self.zone.id)[1]
            network = Network.create(self.api_client, self.services["shared_network_2"], account.name,
                                     account.domainid, networkofferingid=self.shared_network_offering.id)
            self.cleanup.append(network)

        if network is None:
            self.skipTest("Network should not be none. Case not handled for Network of type %s" % value)

        self.debug("Trying to %s network in account %s to a vm in account %s, This should fail" %
                   (network.type, account.name, self.account.name))

        try:
            self.virtual_machine.add_nic(self.apiclient, network.id)
        except Exception:
            pass
        else:
            self.fail("User was able to add NIC, test failed! This issue has been hit: CLOUDSTACK-10071")
        return

    @attr(tags=["advanced", "dvs"])
    def test_24_add_nw_different_domain(self):
        """Add network to running VM"""

        # 1. Create two domains
        # 2. Create network in one domain and create virtual machine in other domain
        # 3. Ad isolated/shared network belonging to one domain to the vm belonging to other domain

        # Validate the following:
        # 1. Adding network should fail

        network = None  # The network which we are adding to the vm

        try:
            tempCleanupList = []
            self.child_domain_1 = Domain.create(self.apiclient,
                                                services=self.services["domain"],
                                                parentdomainid=self.domain.id)
            tempCleanupList.append(self.child_domain_1)

            self.child_do_admin_1 = Account.create(
                self.apiclient,
                self.services["account"],
                admin=True,
                domainid=self.child_domain_1.id
            )
            tempCleanupList.append(self.child_do_admin_1)

            self.child_domain_2 = Domain.create(self.apiclient,
                                                services=self.services["domain"],
                                                parentdomainid=self.domain.id)
            tempCleanupList.append(self.child_domain_2)

            self.child_do_admin_2 = Account.create(
                self.apiclient,
                self.services["account"],
                admin=True,
                domainid=self.child_domain_2.id)
            tempCleanupList.append(self.child_do_admin_2)
        except Exception as e:
            self.fail(e)
        finally:
            tempCleanupList.reverse()
            self.cleanup += tempCleanupList

        network = Network.create(self.api_client, self.services["isolated_network"], self.child_do_admin_1.name,
                                 self.child_do_admin_1.domainid, networkofferingid=self.isolated_network_offering.id)

        virtual_machine = VirtualMachine.create(self.apiclient, self.services["virtual_machine"], accountid=self.child_do_admin_2.name,
                                                domainid=self.child_do_admin_2.domainid, serviceofferingid=self.service_offering.id,
                                                mode=self.zone.networktype)

        time.sleep(self.services["sleep"])
        self.debug("Trying to %s network in domain %s to a vm in domain %s, This should fail" %
                   (network.type, self.child_domain_1.name, self.child_domain_2.name))

        with self.assertRaises(Exception) as e:
            virtual_machine.add_nic(self.apiclient, network.id)
            self.debug("Operation failed with exception %s" % e.exception)
        return

    @attr(tags=["advanced", "dvs"])
    def test_25_add_nw_above_account_limit(self):
        """Add network to VM with maximum network limit reached"""

        # 1. Create an account and create maximum allowed networks in the account
        # 2. Deploy VM in this account
        # 3. Create a network in other account and add to this VM

        # Validate the following:
        # 1. Adding network should fail

        self.debug("Creating account 1")
        account_1 = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(account_1)

        self.debug("setting network limit of account: %s as 1" % account_1.name)
        update_resource_limit(
            self.apiclient,
            6,  # Network
            max=1,
            account=account_1.name,
            domainid=account_1.domainid
        )

        self.debug("Creating isolated network in account: %s" % account_1.name)

        network_1 = Network.create(self.api_client, self.services["isolated_network"], account_1.name,
                                   account_1.domainid, networkofferingid=self.isolated_network_offering.id)

        self.debug("created network %s" % network_1.name)

        self.debug("Deploying virtual machine in account: %s" % account_1.name)

        virtual_machine = VirtualMachine.create(self.apiclient, self.services["virtual_machine"], accountid=account_1.name,
                                                domainid=account_1.domainid, serviceofferingid=self.service_offering.id,
                                                mode=self.zone.networktype)

        self.debug("Deployed virtual machine : %s" % virtual_machine.id)

        self.debug("Creating another account")

        account_2 = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id
        )

        self.debug("Created account %s" % account_2.name)

        self.cleanup.append(account_2)

        self.debug("Creating network in account %s" % account_2.name)
        network_2 = Network.create(self.api_client, self.services["isolated_network"], account_2.name,
                                   account_2.domainid, networkofferingid=self.isolated_network_offering.id)

        self.debug("Created network %s" % network_2.name)

        self.debug("Trying to add netwrok %s to VM %s, this should fail" %
                   (network_2.name, virtual_machine.id))

        with self.assertRaises(Exception) as e:
            virtual_machine.add_nic(self.apiclient, network_2.id)
            self.debug("Operation failed with exception %s" % e.exception)

        return


class TestRemoveNetworkFromVirtualMachine(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestRemoveNetworkFromVirtualMachine, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services

        hypervisor = get_hypervisor_type(cls.api_client)
        if hypervisor.lower() not in ["xenserver", "kvm"]:
            raise unittest.SkipTest("This feature is supported only on XenServer and KVM")

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        template = get_template(cls.api_client, cls.zone.id, cls.services["ostype"])
        # Set Zones and disk offerings
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id

        # Create Accounts & networks
        cls.services["isolated_network"]["zoneid"] = cls.zone.id
        cls.services["shared_network"]["zoneid"] = cls.zone.id
        cls._cleanup = []

        cls.account = Account.create(cls.api_client, cls.services["account"], domainid=cls.domain.id)
        cls._cleanup.append(cls.account)

        cls.service_offering = ServiceOffering.create(cls.api_client, cls.services["service_offering"])
        cls._cleanup.append(cls.service_offering)

        cls.virtual_machine = VirtualMachine.create(cls.api_client, cls.services["virtual_machine"], accountid=cls.account.name,
                                                    domainid=cls.account.domainid, serviceofferingid=cls.service_offering.id,
                                                    mode=cls.zone.networktype)
        # Create Shared Network Offering
        cls.isolated_network_offering = NetworkOffering.create(cls.api_client, cls.services["isolated_network_offering"])
        cls._cleanup.append(cls.isolated_network_offering)

        # Enable Isolated Network offering
        cls.isolated_network_offering.update(cls.api_client, state='Enabled')
        cls.isolated_network = Network.create(cls.api_client, cls.services["isolated_network"], cls.account.name,
                                              cls.account.domainid, networkofferingid=cls.isolated_network_offering.id)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            # Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Disable Network Offerings
            cls.isolated_network_offering.update(cls.api_client, state='Disabled')
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def addNetworkToVm(self, network, vm):
        """Add network to VM and check if new nic added in the VM"""

        self.debug("Adding %s Network: %s to virtual machine %s" %
                   (network.type, network.id, vm.id))
        vm.add_nic(self.apiclient, network.id)
        vm_list = list_virtual_machines(self.apiclient, id=vm.id)
        vm_list_validation_result = validateList(vm_list)
        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         vm_list_validation_result[2])
        self.debug("virtual machine nics: %s" % vm_list[0].nic)
        # Add nic of network to list so that it can be deleted later accessing its id from this list
        self.nics = [x for x in vm_list[0].nic if x.networkid == network.id]
        self.debug("Filtered nics list: %s:" % self.nics)
        self.assertTrue(len(self.nics) == 1, "nics list should contain the nic of added isolated network,\
                        the number of nics for the network should be 1, instead they are %s" %
                        len(self.nics))
        return self.nics

    @attr(tags=["advanced", "dvs"])
    def test_07_remove_nic_running_vm(self):
        """Remove nic from running VM"""

        # 1. Deploy Vm in account
        # 2. Add network to VM
        # 3. Remove the nic added by the newly added network

        # Validate the following:
        # 1. Newly added nic is removed
        # 2. Event NIC.DELETE is generated

        self.addNetworkToVm(self.isolated_network, self.virtual_machine)
        # Access the nic of the added network from self.nics object which is fillled
        # in addNetworkToVm function
        self.debug("Removing added nic %s from vm %s" %
                   (self.nics[0].id, self.virtual_machine.id))
        self.virtual_machine.remove_nic(self.apiclient, self.nics[0].id)
        vm_list = list_virtual_machines(self.apiclient, id=self.virtual_machine.id)
        vm_list_validation_result = validateList(vm_list)
        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         vm_list_validation_result[2])
        self.debug("virtual machine nics: %s" % vm_list[0].nic)
        # Verify the nic is removed from the virtual machine
        self.debug("Verifying the nic is removed from the virtual machine")
        self.assertFalse(any(x.networkid == self.isolated_network.id for x in vm_list[0].nic),
                         "nic still present in the virtual machine nic list")
        self.debug("nic removed successfully")
        self.debug("Retrieving events list matching events 'NIC.DELETE'")
        events = list_events(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            type='NIC.DELETE'
        )
        event_list_validation_result = validateList(events)
        self.assertEqual(event_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         event_list_validation_result[2])
        self.debug("Events list contains event NIC.DELETE")
        self.debug("events: %s" % events)
        return

    @attr(tags=["advanced", "dvs"])
    def test_08_remove_default_nic(self):
        """Test Remove default nic of running VM"""

        # 1. Deploy Vm in account
        # 2. Try to remove the default nic of the VM

        # Validate the following:
        # 1. Default nic of vm is not removed

        vm_list = list_virtual_machines(self.apiclient, id=self.virtual_machine.id)
        vm_list_validation_result = validateList(vm_list)
        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         vm_list_validation_result[2])
        self.debug("virtual machine nics: %s" % vm_list[0].nic)
        self.assertEqual(len(vm_list[0].nic), 1, "There should only be default nic present in the vm")
        self.debug("Trying to remove the default nic of vm : %s, this should fail" %
                   self.virtual_machine.id)
        with self.assertRaises(Exception):
            self.virtual_machine.remove_nic(self.apiclient, vm_list[0].nic[0].id)
            self.debug("Removing default nic of vm failed")
        return

    @attr(tags=["advanced", "dvs"])
    def test_09_remove_foreign_nic(self):
        """Remove nic which does not belong to VM"""

        # 1. Add VM in an account
        # 1. Add new account and deploy vm in it
        # 2. Try to remove nic of the new vm from first vm

        # Validate the following:
        # 1. Nic remove operation should fail

        self.debug("Creating new account")
        account = Account.create(
            self.api_client,
            self.services["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(account)
        self.debug("created new account : %s" % account.name)
        self.debug("Deploying virtual machine in this account")
        virtual_machine = VirtualMachine.create(self.apiclient, self.services["virtual_machine"], accountid=account.name,
                                                domainid=account.domainid, serviceofferingid=self.service_offering.id,
                                                mode=self.zone.networktype)
        self.debug("Deployed virtual machine: %s" % virtual_machine.id)
        self.debug("Trying to remove nic of new virtual machine from existing virtual machine, This \
                    operation should fail")
        with self.assertRaises(Exception) as e:
            self.virtual_machine.remove_nic(self.apiclient, virtual_machine.nic[0].id)
            self.debug("Operation failed with exception: %s" % e.exception)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_29_remove_nic_CS22503(self):
        """Test to verify remove nic from vm if the nic ip is same as another vm ip in another network"""

        # 1. Deploy vm v1 with networks n1 and n2
        # 2. Check the ip address of nic in n2 say ip1
        # 3. Deployed vm v2 in another network say n3 with same IP address as ip1 using
        # 'deployVirtualMachine' api with 'ipaddress' as one of the parameters.
        # 4. Acquire public IP in n3 network.
        # 5. Configure PF on the acquired IP and assign it to vm v2
        # 6. Try to remove nic  n2 from v1. Should be successfull
        # There was a bug due to both vms has same ip address, so not allowing to remove nic

        vm1 = self.virtual_machine
        nic2 = self.addNetworkToVm(self.isolated_network, vm1)
        # get the ip address of the nic added in 2nd network
        vm1_ip = nic2[0].ipaddress
        self.assertIsNotNone(vm1_ip, "New nic did not get the ip address")
        # Create network n3
        self.network3 = Network.create(
            self.api_client,
            self.services["isolated_network"],
            self.account.name,
            self.account.domainid,
            networkofferingid=self.isolated_network_offering.id
        )
        self.cleanup.append(self.network3)
        self.vm2 = VirtualMachine.create(
            self.api_client,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[self.network3.id],
            ipaddress=vm1_ip,
            mode=self.zone.networktype
        )
        self.cleanup.append(self.vm2)
        vm2 = VirtualMachine.list(
            self.api_client,
            id=self.vm2.id
        )
        self.assertEqual(validateList(vm2)[0], PASS, "list vms returned invalid response")
        self.assertIsNotNone(vm2[0].nic[0].ipaddress, "vm2 didn't get the ip address")
        self.assertEqual(
            vm1_ip,
            vm2[0].nic[0].ipaddress,
            "vm2 did not get the ip address passed while deploying vm"
        )
        ip_address = PublicIPAddress.create(
            self.apiclient,
            self.account.name,
            self.zone.id,
            self.account.domainid,
            self.services["virtual_machine"],
            self.network3.id
        )
        self.cleanup.append(ip_address)
        self.cleanup = self.cleanup[::-1]
        # Open up firewall port for SSH
        FireWallRule.create(
            self.apiclient,
            ipaddressid=ip_address.ipaddress.id,
            protocol=self.services["natrule"]["protocol"],
            cidrlist=['0.0.0.0/0'],
            startport=self.services["natrule"]["publicport"],
            endport=self.services["natrule"]["publicport"]
        )
        # Create NAT rule
        nat_rule = NATRule.create(
            self.apiclient,
            self.vm2,
            self.services["natrule"],
            ip_address.ipaddress.id
        )
        list_nat_rule_response = list_nat_rules(
            self.apiclient,
            id=nat_rule.id
        )
        self.assertEqual(
            validateList(list_nat_rule_response)[0],
            PASS,
            "Check list response returns a valid list"
        )
        self.assertEqual(
            list_nat_rule_response[0].id,
            nat_rule.id,
            "Check Correct Port forwarding Rule is returned"
        )
        # Try to remove nic 2 from vm1
        try:
            vm1.remove_nic(self.apiclient, self.nics[0].id)
            vm1_res = VirtualMachine.list(self.apiclient, id=vm1.id)
            self.assertEqual(validateList(vm1_res)[0], PASS, "invalid listvm response")
            self.assertEqual(
                len(vm1_res[0].nic),
                1,
                "VM has more than one nic even after removing the 2nd nic"
            )
        except Exception as e:
            self.fail("Failed to delete the nic from vm")
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_30_remove_nic_reattach(self):
        """
         Test to verify vm start after NIC removal and reattach

         # 1.Create vm which has 3 nics(e.g. #0,#1,#2)
         # 2.Stop the vm
         # 3.Remove second nic(#1)
         # 4.Add/Reattach same network(#1)
         # 5.Start the instance
        """
        self.ntwk2 = Network.create(
            self.apiclient,
            self.services["isolated_network"],
            self.account.name,
            self.account.domainid,
            networkofferingid=self.isolated_network_offering.id
        )
        self.ntwk3 = Network.create(
            self.apiclient,
            self.services["isolated_network"],
            self.account.name,
            self.account.domainid,
            networkofferingid=self.isolated_network_offering.id
        )
        self.test_vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.zone.networktype,
            networkids=[self.isolated_network.id, self.ntwk2.id, self.ntwk3.id]
        )
        self.assertIsNotNone(self.test_vm, "Failed to create vm with 3 nics")
        list(map(lambda x: self.cleanup.append(x), [self.test_vm, self.ntwk2, self.ntwk3]))
        vm_res = VirtualMachine.list(
            self.apiclient,
            id=self.test_vm.id
        )
        self.assertEqual(validateList(vm_res)[0], PASS, "Invalid list vm response")
        self.nics = vm_res[0].nic
        self.assertEqual(
            validateList(self.nics)[0],
            PASS,
            "vm response does not contain nics info"
        )
        self.assertEqual(len(self.nics), 3, "Not all nics found in vm response")
        self.test_vm.stop(self.apiclient)
        vm_res2 = VirtualMachine.list(
            self.apiclient,
            id=self.test_vm.id
        )
        self.assertEqual(validateList(vm_res2)[0], PASS, "Invalid response")
        self.assertEqual(
            vm_res2[0].state,
            "Stopped",
            "VM did not stop properly"
        )

        """
        get the network id of the nic which we are remove from the nic, so that we can
        use that network id for reattach
        """
        nic_to_attach = [x for x in [self.isolated_network, self.ntwk2, self.ntwk3] \
                         if x.id == self.nics[1].networkid]
        self.assertEqual(validateList(nic_to_attach)[0], PASS, "No matching nics")
        self.assertEqual(len(nic_to_attach), 1, "More than one nic in same network")
        try:
            self.test_vm.remove_nic(self.apiclient, nicId=self.nics[1].id)
            self.test_vm.add_nic(
                self.apiclient,
                nic_to_attach[0].id
            )
            self.test_vm.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start vm after nic removal and attachment")
        vm_res3 = VirtualMachine.list(self.apiclient, id=self.test_vm.id)
        self.assertEqual(
            validateList(vm_res3)[0],
            PASS,
            "Invalid listvm response after nic detach and attach"
        )
        self.assertEqual(
            vm_res3[0].state,
            "Running",
            "VM didn't come to running state after nic detach and attach"
        )
        vm_nics = vm_res3[0].nic
        self.assertEqual(validateList(vm_nics)[0], PASS, "Invalid nics after vm stop/start")
        self.assertEqual(
            len(vm_nics),
            3,
            "Nic is not attached/detected"
        )
        return


class TestUpdateVirtualMachineNIC(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestUpdateVirtualMachineNIC, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services

        hypervisor = get_hypervisor_type(cls.api_client)
        if hypervisor.lower() not in ["xenserver", "kvm"]:
            raise unittest.SkipTest("This feature is supported only on XenServer and KVM")

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        template = get_template(cls.api_client, cls.zone.id, cls.services["ostype"])
        # Set Zones and disk offerings
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id

        # Create Accounts & networks
        cls.services["isolated_network"]["zoneid"] = cls.zone.id
        cls.services["shared_network"]["zoneid"] = cls.zone.id
        cls._cleanup = []

        cls.account = Account.create(cls.api_client, cls.services["account"], domainid=cls.domain.id)
        cls._cleanup.append(cls.account)

        cls.service_offering = ServiceOffering.create(cls.api_client, cls.services["service_offering"])
        cls._cleanup.append(cls.service_offering)

        cls.virtual_machine = VirtualMachine.create(cls.api_client, cls.services["virtual_machine"],
                                                    accountid=cls.account.name, domainid=cls.account.domainid,
                                                    serviceofferingid=cls.service_offering.id,
                                                    mode=cls.zone.networktype)
        # Create Shared Network Offering
        cls.isolated_network_offering = NetworkOffering.create(cls.api_client, cls.services["isolated_network_offering"])
        cls._cleanup.append(cls.isolated_network_offering)
        # Enable Isolated Network offering
        cls.isolated_network_offering.update(cls.api_client, state='Enabled')
        cls.isolated_network = Network.create(cls.api_client, cls.services["isolated_network"], cls.account.name,
                                              cls.account.domainid, networkofferingid=cls.isolated_network_offering.id)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            # Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Disable Network Offerings
            cls.isolated_network_offering.update(cls.api_client, state='Disabled')
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def addNetworkToVm(self, network, vm):
        """Add network to VM and check if new nic added in the VM"""

        self.debug("Adding %s Network: %s to virtual machine %s" %
                   (network.type, network.id, vm.id))
        vm.add_nic(self.apiclient, network.id)
        vm_list = list_virtual_machines(self.apiclient, id=vm.id)
        vm_list_validation_result = validateList(vm_list)
        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         vm_list_validation_result[2])
        self.debug("virtual machine nics: %s" % vm_list[0].nic)
        # Add nic of network to list so that it can be deleted later accessing its id from this list
        self.nics = [x for x in vm_list[0].nic if x.networkid == network.id]
        self.debug("Filtered nics list: %s:" % self.nics)
        self.assertTrue(len(self.nics) == 1, "nics list should contain the nic of added isolated network,\
                        the number of nics for the network should be 1, instead they are %s" %
                        len(self.nics))
        return

    @attr(tags=["advanced", "dvs"])
    def test_11_update_nic_running_vm(self):
        """update default nic of running VM"""

        # 1. Deploy Vm in account
        # 2. Add network to VM
        # 3. Update default nic of VM (Make the newly added NIC as default)

        # Validate the following:
        # 1. Default nic is updated
        # 2. Previous default nic is now non-default
        # 3. Event NIC.UPDATE is generated

        self.addNetworkToVm(self.isolated_network, self.virtual_machine)
        self.debug("Listing virtual machine so that to retrive the list of non-default and default nic")
        vm_list = list_virtual_machines(self.apiclient, id=self.virtual_machine.id)
        vm_list_validation_result = validateList(vm_list)
        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         vm_list_validation_result[2])
        if len(vm_list[0].nic) != 2:
            self.fail("VM should have exactly two NICs")

        defaultNicIdBeforeUpdate = None
        nonDefaultNicIdBeforeUpdate = None

        for nic in vm_list[0].nic:
            if nic.isdefault:
                defaultNicIdBeforeUpdate = nic.id
            else:
                nonDefaultNicIdBeforeUpdate = nic.id

        self.debug("Default nic of VM is %s and non default nic of VM is %s"
                   % (defaultNicIdBeforeUpdate, nonDefaultNicIdBeforeUpdate))

        self.debug("Making non default nic as default nic")
        self.virtual_machine.update_default_nic(self.apiclient, nicId=nonDefaultNicIdBeforeUpdate)
        self.debug("Again listing the NIC list of VM to verify the update operation was successful")
        vm_list = list_virtual_machines(self.apiclient, id=self.virtual_machine.id)
        vm_list_validation_result = validateList(vm_list)
        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         vm_list_validation_result[2])

        if len(vm_list[0].nic) != 2:
            self.fail("VM should have exactly two NICs")

        for nic in vm_list[0].nic:
            if nic.isdefault:
                defaultNicIdAfterUpdate = nic.id

        self.assertEqual(nonDefaultNicIdBeforeUpdate, defaultNicIdAfterUpdate, "old non default NIC not made\
                        default one, update_default_nic API failed")
        self.debug("Retrieving events list matching events 'NIC.UPDATE'")
        events = list_events(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            type='NIC.UPDATE'
        )
        event_list_validation_result = validateList(events)
        self.assertEqual(event_list_validation_result[0], PASS, "event list validation failed due to %s" %
                         event_list_validation_result[2])
        self.debug("Events list contains event NIC.UPDATE")
        self.debug("events: %s" % events)
        return

    @attr(tags=["advanced", "dvs"])
    def test_12_make_default_nic_as_default(self):
        """Try to set default nic of vm again as default"""

        # 1. Deploy Vm in account
        # 2. Set default nic of vm again as default

        # Validate the following:
        # 1. updateDefaultNic API fails

        self.debug("Listing virtual machine to get default nic")
        vm_list = list_virtual_machines(self.apiclient, id=self.virtual_machine.id)
        vm_list_validation_result = validateList(vm_list)
        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         vm_list_validation_result[2])

        defaultNicId = None

        for nic in vm_list[0].nic:
            if nic.isdefault:
                defaultNicId = nic.id

        self.debug("Trying to set default nic again as default nic, This should fail")
        with self.assertRaises(Exception) as e:
            self.virtual_machine.update_default_nic(self.apiclient, nicId=defaultNicId)
            self.debug("updateDefaultNic operation failed as expected with exception: %s" %
                       e.exception)

        return

    @attr(tags=["advanced", "dvs"])
    def test_13_set_foreign_nic_as_default(self):
        """set nic which does not belong to VM as its default one"""

        # 1. Add VM in an account
        # 1. Add new account and deploy vm in it
        # 2. Try to set nic of the new vm as default nic of first vm

        # Validate the following:
        # 1. updateDefaultNic operation should fail

        self.debug("Creating new account")

        account = Account.create(self.api_client, self.services["account"], domainid=self.domain.id)
        self.cleanup.append(account)
        self.debug("created new account : %s" % account.name)
        self.debug("Deploying virtual machine in this account")
        virtual_machine = VirtualMachine.create(self.apiclient, self.services["virtual_machine"],
                                                accountid=account.name, domainid=account.domainid,
                                                serviceofferingid=self.service_offering.id, mode=self.zone.networktype)
        time.sleep(self.services["sleep"])
        self.debug("Deployed virtual machine: %s" % virtual_machine.id)
        foreignNicId = virtual_machine.nic[0].id

        self.debug("Trying to set nic of new virtual machine as default nic of existing virtual machine, This \
                    operation should fail")
        with self.assertRaises(Exception) as e:
            self.virtual_machine.update_default_nic(self.apiclient, nicId=foreignNicId)
            self.debug("updateDefaultNic operation failed as expected with exception: %s" %
                       e.exception)

        return


class TestFailureScenariosAddNetworkToVM(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestFailureScenariosAddNetworkToVM, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services

        hypervisor = get_hypervisor_type(cls.api_client)
        if hypervisor.lower() not in ["xenserver", "kvm"]:
            raise unittest.SkipTest("This feature is supported only on XenServer and KVM")

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        template = get_template(cls.api_client, cls.zone.id, cls.services["ostype"])
        # Set Zones and disk offerings
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id
        # Create Accounts & networks
        cls.services["isolated_network"]["zoneid"] = cls.zone.id
        cls._cleanup = []
        cls.account = Account.create(cls.api_client, cls.services["account"], domainid=cls.domain.id)
        cls._cleanup.append(cls.account)

        cls.service_offering = ServiceOffering.create(cls.api_client, cls.services["service_offering"])
        cls._cleanup.append(cls.service_offering)

        cls.virtual_machine = VirtualMachine.create(cls.api_client, cls.services["virtual_machine"],
                                                    accountid=cls.account.name, domainid=cls.account.domainid,
                                                    serviceofferingid=cls.service_offering.id, mode=cls.zone.networktype)
        # Create Shared Network Offering
        cls.isolated_network_offering = NetworkOffering.create(cls.api_client, cls.services["isolated_network_offering"], )
        cls._cleanup.append(cls.isolated_network_offering)

        # Enable Isolated Network offering
        cls.isolated_network_offering.update(cls.api_client, state='Enabled')

        cls.isolated_network = Network.create(cls.api_client, cls.services["isolated_network"], cls.account.name,
                                              cls.account.domainid, networkofferingid=cls.isolated_network_offering.id)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            # Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Disable Network Offerings
            cls.isolated_network_offering.update(cls.api_client, state='Disabled')
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "dvs"])
    def test_15_add_nic_wrong_vm_id(self):
        """Add network to vm with wrong vm id"""

        # 1. Call add network to VM API with correct network id but wrong vm id

        # Validate the following:
        # 1. API should throw exception saying unable to find virtual machine

        cmd = addNicToVirtualMachine.addNicToVirtualMachineCmd()
        cmd.virtualmachineid = random_gen(id="virtual_machine", size=30)
        cmd.networkid = self.isolated_network.id

        with self.assertRaises(Exception) as e:
            self.apiclient.addNicToVirtualMachine(cmd)
            self.debug("addNicToVirtualMachine API failed with exception: %s" % e.exception)

        return

    @attr(tags=["advanced", "dvs"])
    def test_16_add_nic_wrong_network_id(self):
        """Add network to vm with wrong network id"""

        # 1. Call add network to VM API with correct network id but wrong network id

        # Validate the following:
        # 1. API should throw exception saying unable to find a network

        cmd = addNicToVirtualMachine.addNicToVirtualMachineCmd()
        cmd.virtualmachineid = self.virtual_machine.id
        cmd.networkid = random_gen(id="network_id", size=30)

        with self.assertRaises(Exception) as e:
            self.apiclient.addNicToVirtualMachine(cmd)
            self.debug("addNicToVirtualMachine API failed with exception: %s" % e.exception)

        return

    @attr(tags=["advanced", "dvs"])
    def test_17_add_nic_different_zone(self):
        """Add network to vm where both belong to different zones"""

        # 1. Deploy a VM in zone 1
        # 2. Create a network in zone 2
        # 3. Try to add this network to the VM (both belong to different zones)

        # Validate the following:
        # 1. API should throw exception vminstance is in zone<id>, but  network is in zone <id>

        foreignZoneId = None

        zones = list_zones(self.apiclient, available=True)
        list_zones_validation_result = validateList(zones)
        self.assertEqual(list_zones_validation_result[0], PASS, "list zones validation failed due to: %s" %
                         list_zones_validation_result[2])
        if len(zones) >= 2:
            for zone in zones:
                if zone.id != self.zone.id:
                    foreignZoneId = zone.id
                    break
            else:
                self.skipTest("This test requires at least two zones to be present in the setup")

            self.services["isolated_network"]["zoneid"] = foreignZoneId

        self.debug("Creating isolated network in zone %s which is foreign to VM" %
                   foreignZoneId)
        isolated_network = Network.create(self.apiclient, self.services["isolated_network"],
                                          self.account.name, self.account.domainid,
                                          networkofferingid=self.isolated_network_offering.id)
        self.debug("Created isolated network %s in zone %s" %
                   (isolated_network.id, foreignZoneId))

        self.debug("Trying to add network to VM, both belonging to different zones")
        cmd = addNicToVirtualMachine.addNicToVirtualMachineCmd()
        cmd.virtualmachineid = self.virtual_machine.id
        cmd.networkid = isolated_network.id

        with self.assertRaises(Exception) as e:
            time.sleep(5)
            self.apiclient.addNicToVirtualMachine(cmd)
            self.debug("addNicToVirtualMachine API failed with exception: %s" % e.exception)

        return

    @attr(tags=["invalid"])
    def test_18_add_nic_basic_zone(self):
        """Add network to vm in basic zone"""

        # 1. Deploy a vm and create network in basic zone
        # 2. Try adding network to vm

        # Validate following
        # 1. API should throw exception saying Can't add a new nic to vm in basic network

        basicZone = None

        zones = list_zones(self.apiclient, available=True)
        list_zones_validation_result = validateList(zones)
        self.assertEqual(list_zones_validation_result[0], PASS, "list zones validation failed due to: %s" %
                         list_zones_validation_result[2])
        for zone in zones:
            if zone.networktype.lower() == 'BASIC':
                basicZone = zone.id
                break
        if basicZone is None:
            self.skipTest("This test requires at least one basic zone to be present in the setup")
        self.services["isolated_network"]["zoneid"] = basicZone.id
        self.debug("Creating isolated network in basic zone: %s" % basicZone.id)
        isolated_network = Network.create(self.apiclient, self.services["isolated_network"],
                                          networkofferingid=self.isolated_network_offering.id)

        self.debug("Created isolated network %s:" % isolated_network.id)
        self.cleanup.append(isolated_network)

        self.services["virtual_machine"]["zoneid"] = basicZone.id

        self.debug("Deploying virtual machine in basic zone: %s" % basicZone.id)
        virtual_machine = VirtualMachine.create(self.apiclient, self.services["virtual_machine"],
                                                serviceofferingid=self.service_offering.id,
                                                mode=basicZone.networktype)
        time.sleep(self.services["sleep"])
        self.debug("Deployed virtual machine %s: " % virtual_machine.id)

        cmd = addNicToVirtualMachine.addNicToVirtualMachineCmd()
        cmd.virtualmachineid = virtual_machine.id
        cmd.networkid = isolated_network.id

        self.dedbug("Trying to add isolated network to VM (both in basic zone,\
                    this operation should fail")
        with self.assertRaises(Exception) as e:
            time.sleep(5)
            self.apiclient.addNicToVirtualMachine(cmd)
            self.debug("addNicToVirtualMachine API failed with exception: %s" % e.exception)

        return

    @attr(tags=["advanced", "dvs"])
    def test_26_add_nic_insufficient_permission(self):
        """Try to add network to vm with insufficient permission"""

        # 1. Call add network to VM API with api client of other account

        # Validate the following:
        # 1. API should throw exception saying insufficient permission

        cmd = addNicToVirtualMachine.addNicToVirtualMachineCmd()
        cmd.virtualmachineid = self.virtual_machine.id
        cmd.networkid = self.isolated_network.id

        self.debug("Creating new account")

        account = Account.create(self.apiclient, self.services["account"], domainid=self.domain.id)
        self.cleanup.append(account)

        self.debug("Created account %s" % account.name)

        self.debug("creating user api client for account: %s" % account.name)
        api_client = self.testClient.getUserApiClient(UserName=account.name, DomainName=self.account.domain)

        self.debug("Trying to add network to vm with this api client, this should fail due to \
                    insufficient permission")

        with self.assertRaises(Exception) as e:
            time.sleep(5)
            api_client.addNicToVirtualMachine(cmd)
            self.debug("addNicToVirtualMachine API failed with exception: %s" % e.exception)

        return


class TestFailureScenariosRemoveNicFromVM(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestFailureScenariosRemoveNicFromVM, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services

        hypervisor = get_hypervisor_type(cls.api_client)
        if hypervisor.lower() not in ["xenserver", "kvm"]:
            raise unittest.SkipTest("This feature is supported only on XenServer and KVM")

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        template = get_template(cls.api_client, cls.zone.id, cls.services["ostype"])
        # Set Zones and disk offerings
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id

        # Create Accounts & networks
        cls.services["isolated_network"]["zoneid"] = cls.zone.id
        cls.services["shared_network"]["zoneid"] = cls.zone.id
        cls._cleanup = []

        cls.account = Account.create(cls.api_client, cls.services["account"], domainid=cls.domain.id)
        cls._cleanup.append(cls.account)

        cls.service_offering = ServiceOffering.create(cls.api_client, cls.services["service_offering"])
        cls._cleanup.append(cls.service_offering)

        cls.virtual_machine = VirtualMachine.create(cls.api_client, cls.services["virtual_machine"],
                                                    accountid=cls.account.name, domainid=cls.account.domainid,
                                                    serviceofferingid=cls.service_offering.id,
                                                    mode=cls.zone.networktype)

        # Create Shared Network Offering
        cls.isolated_network_offering = NetworkOffering.create(cls.api_client, cls.services["isolated_network_offering"], )
        cls._cleanup.append(cls.isolated_network_offering)
        # Enable Isolated Network offering
        cls.isolated_network_offering.update(cls.api_client, state='Enabled')
        cls.isolated_network = Network.create(cls.api_client, cls.services["isolated_network"], cls.account.name,
                                              cls.account.domainid, networkofferingid=cls.isolated_network_offering.id)

        # Add network to VM
        cls.virtual_machine.add_nic(cls.api_client, cls.isolated_network.id)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            # Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Disable Network Offerings
            cls.isolated_network_offering.update(cls.api_client, state='Disabled')
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "dvs"])
    def test_19_remove_nic_wrong_vm_id(self):
        """Try to remove nic from a vm providing wrong vm id to API"""

        # (Frist two steps are perfromed in setupClass)
        # 1. Deploy Vm in account
        # 2. Add network to VM
        # 3. Remove the nic added by the newly added network providing wrong vm id to the API

        # Validate the following:
        # 1. API throws exception unable to find a virtual machine with id

        vm_list = list_virtual_machines(self.apiclient, id=self.virtual_machine.id)
        vm_list_validation_result = validateList(vm_list)
        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         vm_list_validation_result[2])
        vm = vm_list_validation_result[1]

        nics = [x for x in vm.nic if x.networkid == self.isolated_network.id]

        self.assertEqual(len(nics), 1, "There should be exactly one nic corresponding to the isolate\
                        network %s" % self.isolated_network.id)

        cmd = removeNicFromVirtualMachine.removeNicFromVirtualMachineCmd()
        cmd.virtualmachineid = self.virtual_machine.id + random_gen()
        cmd.nicid = nics[0].id

        with self.assertRaises(Exception) as e:
            self.apiclient.removeNicFromVirtualMachine(cmd)
            self.debug("removeNicFromVirtualMachine API failed with exception: %s" % e.exception)

        return

    @attr(tags=["advanced", "dvs"])
    def test_20_remove_nic_wrong_nic_id(self):
        """Try to remove nic from a vm providing wrong nic id to API"""

        # (Frist two steps are perfromed in setupClass)
        # 1. Deploy Vm in account
        # 2. Add network to VM
        # 3. Remove the nic added by the newly added network providing wrong nic id to the API

        # Validate the following:
        # 1. API throws exception unable to find nic with id

        vm_list = list_virtual_machines(self.apiclient, id=self.virtual_machine.id)
        vm_list_validation_result = validateList(vm_list)
        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         vm_list_validation_result[2])
        vm = vm_list_validation_result[1]

        nics = [x for x in vm.nic if x.networkid == self.isolated_network.id]

        self.assertEqual(len(nics), 1, "There should be exactly one nic corresponding to the isolate\
                        network %s" % self.isolated_network.id)

        cmd = removeNicFromVirtualMachine.removeNicFromVirtualMachineCmd()
        cmd.virtualmachineid = self.virtual_machine.id
        cmd.nicid = nics[0].id + random_gen()

        with self.assertRaises(Exception) as e:
            self.apiclient.removeNicFromVirtualMachine(cmd)
            self.debug("removeNicFromVirtualMachine API failed with exception: %s" % e.exception)

        return

    @attr(tags=["advanced", "dvs"])
    def test_27_remove_nic_insufficient_permission(self):
        """Try to remove nic from vm with insufficient permission"""

        # 1. Call remove network from VM API with api client of other account

        # Validate the following:
        # 1. API should throw exception saying insufficient permission

        vm_list = list_virtual_machines(self.apiclient, id=self.virtual_machine.id)
        vm_list_validation_result = validateList(vm_list)
        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         vm_list_validation_result[2])
        vm = vm_list_validation_result[1]

        nics = [x for x in vm.nic if x.networkid == self.isolated_network.id]

        self.assertEqual(len(nics), 1, "There should be exactly one nic corresponding to the isolate\
                        network %s" % self.isolated_network.id)

        cmd = removeNicFromVirtualMachine.removeNicFromVirtualMachineCmd()
        cmd.virtualmachineid = self.virtual_machine.id
        cmd.nicid = nics[0].id

        self.debug("Creating new account")

        account = Account.create(self.apiclient, self.services["account"], domainid=self.domain.id)
        self.cleanup.append(account)

        self.debug("Created account %s" % account.name)

        self.debug("creating user api client for account: %s" % account.name)
        api_client = self.testClient.getUserApiClient(UserName=account.name, DomainName=self.account.domain)

        self.debug("Trying to add network to vm with this api client, this should fail due to \
                    insufficient permission")

        with self.assertRaises(Exception) as e:
            api_client.removeNicFromVirtualMachine(cmd)
            self.debug("removeNicFromVirtualMachine API failed with exception: %s" % e.exception)

        return


class TestFailureScenariosUpdateVirtualMachineNIC(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestFailureScenariosUpdateVirtualMachineNIC, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services

        hypervisor = get_hypervisor_type(cls.api_client)
        if hypervisor.lower() not in ["xenserver", "kvm"]:
            raise unittest.SkipTest("This feature is supported only on XenServer and KVM")

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        template = get_template(cls.api_client, cls.zone.id, cls.services["ostype"])
        # Set Zones and disk offerings
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id

        # Create Accounts & networks
        cls.services["isolated_network"]["zoneid"] = cls.zone.id
        cls.services["shared_network"]["zoneid"] = cls.zone.id
        cls._cleanup = []

        cls.account = Account.create(cls.api_client, cls.services["account"], domainid=cls.domain.id)
        cls._cleanup.append(cls.account)

        cls.service_offering = ServiceOffering.create(cls.api_client, cls.services["service_offering"])
        cls._cleanup.append(cls.service_offering)

        cls.virtual_machine = VirtualMachine.create(cls.api_client, cls.services["virtual_machine"],
                                                    accountid=cls.account.name, domainid=cls.account.domainid,
                                                    serviceofferingid=cls.service_offering.id, mode=cls.zone.networktype)

        cls.defaultNetworkId = cls.virtual_machine.nic[0].networkid

        # Create Shared Network Offering
        cls.isolated_network_offering = NetworkOffering.create(cls.api_client, cls.services["isolated_network_offering"], )
        cls._cleanup.append(cls.isolated_network_offering)
        # Enable Isolated Network offering
        cls.isolated_network_offering.update(cls.api_client, state='Enabled')

        cls.isolated_network = Network.create(cls.api_client, cls.services["isolated_network"],
                                              cls.account.name, cls.account.domainid,
                                              networkofferingid=cls.isolated_network_offering.id)
        cls.virtual_machine.add_nic(cls.api_client, cls.isolated_network.id)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            # Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Disable Network Offerings
            cls.isolated_network_offering.update(cls.api_client, state='Disabled')
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "dvs"])
    def test_21_update_nic_wrong_vm_id(self):
        """update default nic of vm providing wrong vm id to the API"""

        # (First two steps are performed in setupClass)
        # 1. Deploy Vm in account
        # 2. Add network to VM
        # 3. Update default nic of VM (Make the newly added NIC as default) by providing wrong
        # vm id to the API

        # Validate the following:
        # 1. API throws exception saying can't find the virtual machine

        self.debug("Listing virtual machine so that to retrive the list of non-default and default nic")
        vm_list = list_virtual_machines(self.apiclient, id=self.virtual_machine.id)
        vm_list_validation_result = validateList(vm_list)
        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         vm_list_validation_result[2])
        if len(vm_list[0].nic) != 2:
            self.fail("VM should have exactly two NICs")

        defaultNicIdBeforeUpdate = None
        nonDefaultNicIdBeforeUpdate = None

        for nic in vm_list[0].nic:
            if nic.isdefault:
                defaultNicIdBeforeUpdate = nic.id
            else:
                nonDefaultNicIdBeforeUpdate = nic.id

        self.debug("Default nic of VM is %s and non default nic of VM is %s"
                   % (defaultNicIdBeforeUpdate, nonDefaultNicIdBeforeUpdate))
        self.debug("Making non default nic as default nic")

        cmd = updateDefaultNicForVirtualMachine.updateDefaultNicForVirtualMachineCmd()
        cmd.virtualmachineid = self.virtual_machine.id + random_gen()
        cmd.nicid = nonDefaultNicIdBeforeUpdate

        with self.assertRaises(Exception) as e:
            self.apiclient.updateDefaultNicForVirtualMachine(cmd)
            self.debug("updateDefaultNicForVirtualMachine API failed with exception: %s" %
                       e.exception)

        return

    @attr(tags=["advanced", "dvs"])
    def test_22_update_nic_wrong_nic_id(self):
        """update default nic of vm providing wrong nic id to the API"""

        # (First two steps are performed in setupClass)
        # 1. Deploy Vm in account
        # 2. Add network to VM
        # 3. Update default nic of VM (Make the newly added NIC as default) by providing wrong
        # nic id to the API

        # Validate the following:
        # 1. API throws exception saying can't find the nic with id

        self.debug("Listing virtual machine so that to retrive the list of non-default and default nic")
        vm_list = list_virtual_machines(self.apiclient, id=self.virtual_machine.id)
        vm_list_validation_result = validateList(vm_list)
        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         vm_list_validation_result[2])

        if len(vm_list[0].nic) != 2:
            self.fail("VM should have exactly two NICs")

        defaultNicIdBeforeUpdate = None
        nonDefaultNicIdBeforeUpdate = None

        for nic in vm_list[0].nic:
            if nic.isdefault:
                defaultNicIdBeforeUpdate = nic.id
            else:
                nonDefaultNicIdBeforeUpdate = nic.id

        self.debug("Default nic of VM is %s and non default nic of VM is %s"
                   % (defaultNicIdBeforeUpdate, nonDefaultNicIdBeforeUpdate))
        self.debug("Making non default nic as default nic")

        cmd = updateDefaultNicForVirtualMachine.updateDefaultNicForVirtualMachineCmd()
        cmd.virtualmachineid = self.virtual_machine.id
        cmd.nicid = nonDefaultNicIdBeforeUpdate + random_gen()

        with self.assertRaises(Exception) as e:
            self.apiclient.updateDefaultNicForVirtualMachine(cmd)
            self.debug("updateDefaultNicForVirtualMachine API failed with exception: %s" %
                       e.exception)

        return

    @attr(tags=["advanced", "dvs"])
    def test_23_update_nic_incorrect_vm_state(self):
        """update default nic of vm when vm is state is not Running or Stopped"""

        # (First two steps are performed in setupClass)
        # 1. Deploy Vm in account
        # 2. Add network to VM
        # 3. Destroy virtual machine so that the VM state becomes Destroyed or Expunging
        # 4. Update default nic of VM (Make the newly added NIC as default)

        # Validate the following:
        # 1. API throws exception instance is not Running or Stopped

        self.debug("Creating new account")
        account = Account.create(self.apiclient, self.services["account"], domainid=self.domain.id)
        self.cleanup.append(account)

        self.debug("Creating virtual machine in the account %s" % account.name)
        virtual_machine = VirtualMachine.create(self.api_client, self.services["virtual_machine"],
                                                accountid=account.name, domainid=account.domainid,
                                                serviceofferingid=self.service_offering.id,
                                                mode=self.zone.networktype)
        time.sleep(self.services["sleep"])
        self.debug("Created virtual machine %s" % virtual_machine.id)

        self.debug("Creating isolated network in account %s" % account.name)
        isolated_network = Network.create(self.apiclient, self.services["isolated_network"], account.name,
                                          account.domainid, networkofferingid=self.isolated_network_offering.id)

        self.debug("Created isolated network %s" % isolated_network.id)

        self.debug("Adding isolated network %s to vm %s" % (isolated_network.id, virtual_machine.id))
        virtual_machine.add_nic(self.apiclient, isolated_network.id)

        self.debug("Listing virtual machine so that to retrive the list of non-default and default nic")
        vm_list = list_virtual_machines(self.apiclient, id=virtual_machine.id, listall=True)
        vm_list_validation_result = validateList(vm_list)
        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         vm_list_validation_result[2])

        if len(vm_list[0].nic) != 2:
            self.fail("VM should have exactly two NICs")

        defaultNicIdBeforeUpdate = None
        nonDefaultNicIdBeforeUpdate = None

        for nic in vm_list[0].nic:
            if nic.isdefault:
                defaultNicIdBeforeUpdate = nic.id
            else:
                nonDefaultNicIdBeforeUpdate = nic.id

        self.debug("Default nic of VM is %s and non default nic of VM is %s"
                   % (defaultNicIdBeforeUpdate, nonDefaultNicIdBeforeUpdate))
        self.debug("Destroying VM %s" % virtual_machine.id)
        virtual_machine.delete(self.apiclient, expunge=False)

        self.debug("Making non default nic as default nic")

        cmd = updateDefaultNicForVirtualMachine.updateDefaultNicForVirtualMachineCmd()
        cmd.virtualmachineid = virtual_machine.id
        cmd.nicid = nonDefaultNicIdBeforeUpdate

        with self.assertRaises(Exception) as e:
            self.apiclient.updateDefaultNicForVirtualMachine(cmd)
            self.debug("updateDefaultNicForVirtualMachine API failed with exception: %s" %
                       e.exception)
        return

    @attr(tags=["advanced", "dvs"])
    def test_28_update_nic_insufficient_permission(self):
        """Try to update default nic of vm with insufficient permission"""

        # 1. Call update nic of VM API with api client of other account

        # Validate the following:
        # 1. API should throw exception saying insufficient permission

        account = Account.create(self.apiclient, self.services["account"], domainid=self.domain.id)
        self.cleanup.append(account)

        self.debug("Created account %s" % account.name)

        self.debug("creating user api client for account: %s" % account.name)
        api_client = self.testClient.getUserApiClient(UserName=account.name, DomainName=self.account.domain)

        self.debug("Listing virtual machine so that to retrive the list of non-default and default nic")
        vm_list = list_virtual_machines(self.apiclient, id=self.virtual_machine.id)
        vm_list_validation_result = validateList(vm_list)
        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         vm_list_validation_result[2])

        if len(vm_list[0].nic) != 2:
            self.fail("VM should have exactly two NICs")

        defaultNicIdBeforeUpdate = None
        nonDefaultNicIdBeforeUpdate = None

        for nic in vm_list[0].nic:
            if nic.isdefault:
                defaultNicIdBeforeUpdate = nic.id
            else:
                nonDefaultNicIdBeforeUpdate = nic.id

        self.debug("Default nic of VM is %s and non default nic of VM is %s"
                   % (defaultNicIdBeforeUpdate, nonDefaultNicIdBeforeUpdate))
        self.debug("Making non default nic as default nic")

        cmd = updateDefaultNicForVirtualMachine.updateDefaultNicForVirtualMachineCmd()
        cmd.virtualmachineid = self.virtual_machine.id
        cmd.nicid = nonDefaultNicIdBeforeUpdate

        with self.assertRaises(Exception) as e:
            api_client.updateDefaultNicForVirtualMachine(cmd)
            self.debug("updateDefaultNicForVirtualMachine API failed with exception: %s" %
                       e.exception)

        return
