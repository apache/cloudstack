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

""" P1 tests for networks in advanced zone with security groups
"""
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from ddt import ddt, data
from marvin.lib.base import (Zone,
                                         ServiceOffering,
                                         Account,
                                         NetworkOffering,
                                         Network,
                                         VirtualMachine,
                                         Domain,
                                         VpcOffering,
                                         VPC,
                                         SecurityGroup,
                                         Host,
                                         )

from marvin.lib.common import (get_domain,
                                           get_zone,
                                           get_template,
                                           get_free_vlan,
                                           list_virtual_machines,
                                           wait_for_cleanup,
                                           )

from marvin.lib.utils import (cleanup_resources,
                                          random_gen,
                                          validateList,)
from marvin.cloudstackAPI import (authorizeSecurityGroupIngress,
                                  revokeSecurityGroupIngress,
                                  deleteSecurityGroup,
                                  listCapacity)
from marvin import deployDataCenter
from nose.plugins.attrib import attr
from marvin.codes import PASS,FAIL,FAILED
from netaddr import iter_iprange
import time
import sys
import random
import json
import os
from platform import system


class TestCreateZoneSG(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestCreateZoneSG, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)

        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.api_client = self.testClient.getApiClient()
        self.debug(self.services)
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created network offerings
            cleanup_resources(self.api_client, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def createzone(self, sec_grps=True):

        self.services["advanced_sg"]["zone"]["name"] = "AdvZoneSG-" + random_gen(size=6)
        self.services["advanced_sg"]["zone"]["securitygroupenabled"] = sec_grps

        self.debug(self.services["advanced_sg"]["zone"]["securitygroupenabled"])

        try:
            zone = Zone.create(self.api_client, self.services["advanced_sg"]["zone"])
            self.cleanup.append(zone)
        except Exception as e:
            self.fail("Exception while creating zone: %s" % e)
        return zone

    def assert_on_sg_flag(self, flag, listzones):

        self.assertEqual(listzones[0].securitygroupsenabled, flag,
                        "Security Group enabled flag is %s with created Zone"
                        % listzones[0].securitygroupsenabled)
        return

    @attr(tags = ["advancedsg"])
    def test_01_createZone_secGrp_enabled(self):
        """ Test to verify Advance Zone with security group enabled can be created"""

        # Validate:
        # Create Advance Zone SG enabled using API

        self.debug("Creating zone with secGrps Enabled")
        zone = self.createzone()
        self.debug("Created zone : %s" % zone.id)

        listzones = Zone.list(self.api_client, id=zone.id)

        self.debug("Checking if SecGroup flag is enabled for the zone")
        self.assert_on_sg_flag(True, listzones)

        return

    @attr(tags = ["advancedsg"])
    def test_02_createZone_secGrp_disabled(self):
        """ Test to verify Advance Zone created with flag
            securitygroupsenabled=False"""

        # Validate:
        # Create Advance Zone without SG enabled
        # Verify that the SG enabled flag is false

        self.debug("Creating zone with secGrps Enabled")
        zone = self.createzone(sec_grps=False)
        self.debug("Created zone : %s" % zone.id)

        listzones = Zone.list(self.api_client, id=zone.id)

        self.debug("Checking if SecGroup flag is False for the zone")
        self.assert_on_sg_flag(False, listzones)

        return

class TestNetworksInAdvancedSG(cloudstackTestCase):
    """Test Creation of different types of networks in SG enabled advanced zone"""

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestNetworksInAdvancedSG, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(cls.api_client, cls.zone.id,
                                    cls.services["ostype"])

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(cls.api_client,cls.services["service_offering"])

        cls._cleanup = [
                        cls.service_offering,
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.api_client = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.cleanup_networks = []
        self.cleanup_accounts = []
        self.cleanup_domains = []
        self.cleanup_projects = []
        self.cleanup_vms = []
        self.cleanup_nwOfferings = []

        self.services["shared_network_offering_sg"]["specifyVlan"] = "True"
        self.services["shared_network_offering_sg"]["specifyIpRanges"] = "True"

        #Create Network Offering
        self.shared_network_offering_sg = NetworkOffering.create(self.api_client, self.services["shared_network_offering_sg"],
                                                                 conservemode=False)

        self.cleanup_nwOfferings.append(self.shared_network_offering_sg)

        #Update network offering state from disabled to enabled.
        NetworkOffering.update(self.shared_network_offering_sg,self.api_client,state="enabled")
        return

    def tearDown(self):
        # all exceptions during cleanup will be appended to this list
        exceptions = []
        try:
            #Clean up, terminate the created network offerings
            cleanup_resources(self.api_client, self.cleanup)
        except Exception as e:
            self.debug("Warning: Exception during cleanup : %s" % e)
            exceptions.append(e)

        #below components is not a part of cleanup because to mandate the order and to cleanup network
        try:
            for vm in self.cleanup_vms:
                vm.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during virtual machines cleanup : %s" % e)
            exceptions.append(e)

        # Wait for VMs to expunge
        wait_for_cleanup(self.api_client, ["expunge.delay", "expunge.interval"])

        try:
            for project in self.cleanup_projects:
                project.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during project cleanup : %s" % e)
            exceptions.append(e)

        try:
            for account in self.cleanup_accounts:
                account.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during account cleanup : %s" % e)
            exceptions.append(e)

        #Wait till all resources created are cleaned up completely and then attempt to delete Network
        time.sleep(self.services["sleep"])

        try:
            for network in self.cleanup_networks:
                network.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during network cleanup : %s" % e)
            exceptions.append(e)

        try:
            for domain in self.cleanup_domains:
                domain.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during domain cleanup : %s" % e)
            exceptions.append(e)

        try:
            for network_offering in self.cleanup_nwOfferings:
                network_offering.update(self.api_client, state="disabled")
                network_offering.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during network cleanup : %s" % e)
            exceptions.append(e)

        if len(exceptions) > 0:
            self.fail("There were exceptions during cleanup: %s" % exceptions)
        return

    def setSharedNetworkParams(self, network, range=20):

        # @range: range decides the endip. Pass the range as "x" if you want the difference between the startip
        # and endip as "x"
        # Set the subnet number of shared networks randomly prior to execution
        # of each test case to avoid overlapping of ip addresses
        shared_network_subnet_number = random.randrange(1,254)

        self.services[network]["gateway"] = "172.16."+str(shared_network_subnet_number)+".1"
        self.services[network]["startip"] = "172.16."+str(shared_network_subnet_number)+".2"
        self.services[network]["endip"] = "172.16."+str(shared_network_subnet_number)+"."+str(range+1)
        self.services[network]["netmask"] = "255.255.255.0"

        return

    @attr(tags = ["advancedsg"])
    def test_03_createIsolatedNetwork(self):
        """ Test Isolated Network """

        # Steps,
        #  1. create Isolated Network Offering
        #  2. Enable network offering - updateNetworkOffering - state=Enabled
        #  3. Try to create Isolated Network
        # Validations,
        #  1. Network creation should FAIL since isolated network is not supported in advanced zone with security groups.

        #Create Network Offering

        self.debug("Creating Isolated network offering")
        self.isolated_network_offering = NetworkOffering.create(self.api_client, self.services["isolated_network_offering"],
                                                                conservemode=False)

        self.cleanup.append(self.isolated_network_offering)

        self.debug("Isolated Network offering created: %s" % self.isolated_network_offering.id)

        #Update network offering state from disabled to enabled.
        self.isolated_network_offering.update( self.api_client, state="enabled")

        #create network using the isolated network offering created
        try:
            self.debug("Trying to create Isolated network, this should fail")
            self.isolated_network = Network.create(self.api_client, self.services["isolated_network"],
                                                   networkofferingid=self.isolated_network_offering.id,
                                                   zoneid=self.zone.id)
            self.cleanup_networks.append(self.isolated_network)
            self.fail("Create isolated network is invalid in advanced zone with security groups.")
        except Exception as e:
            self.debug("Network creation failed because creating isolated network is invalid in advanced zone with security groups.\
                        Exception: %s" % e)
        return

    @attr(tags = ["advancedsg"])
    def test_04_createSharedNetwork_withoutSG(self):
        """ Test Shared Network creation without Security Group Service Provider in Network Offering"""

        # Steps,
        #  1. Create a shared Network offering without SG
        #  2. Enable the network offering
        #  3. Try to create shared network using the above offering
        # Validations,
        #  1. Network creation should FAIL since there is no SecurityProvider in the network offering

        self.services["shared_network_offering"]["specifyVlan"] = "True"
        self.services["shared_network_offering"]["specifyIpRanges"] = "True"

        #Create Network Offering
        self.shared_network_offering = NetworkOffering.create(self.api_client, self.services["shared_network_offering"],
                                                                 conservemode=False)

        self.cleanup_nwOfferings.append(self.shared_network_offering)

        #Update network offering state from disabled to enabled.
        NetworkOffering.update(self.shared_network_offering,self.api_client,state="enabled")

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

        #create network using the shared network offering created
        self.services["shared_network"]["acltype"] = "domain"
        self.services["shared_network"]["vlan"] = vlan
        self.services["shared_network"]["networkofferingid"] = self.shared_network_offering.id
        self.services["shared_network"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network")

        try:
            self.shared_network = Network.create(self.api_client, self.services["shared_network"],
                                                 networkofferingid=self.shared_network_offering.id, zoneid=self.zone.id)
            self.cleanup_networks.append(self.shared_network)
            self.fail("Network created without SecurityProvider , which is invalid")
        except Exception as e:
            self.debug("Network creation failed because there is no SecurityProvider in the network offering.\
                        Exception: %s" % e)
        return

    @attr(tags = ["advancedsg"])
    def test_05_deployVM_SharedwithSG(self):
        """ Test VM deployment in shared networks with SecurityGroup Provider """

        # Steps,
        #  1. create an account
        #  2. Create one shared Network with sec group
        #  3. deployVirtualMachine in the above networkid within the user account
        #  4. delete the user account
        # Validations,
        #  1. shared network should be created successfully
        #  2. VM should deploy successfully

        #Create admin account
        self.admin_account = Account.create(self.api_client, self.services["account"], admin=True,
                                            domainid=self.domain.id)

        self.cleanup_accounts.append(self.admin_account)

        self.debug("Admin type account created: %s" % self.admin_account.name)

        self.services["shared_network_offering_sg"]["specifyVlan"] = "True"
        self.services["shared_network_offering_sg"]["specifyIpRanges"] = "True"

        #Create Network Offering
        self.shared_network_offering_sg = NetworkOffering.create(self.api_client,self.services["shared_network_offering_sg"],
                                                                 conservemode=False)

        self.cleanup_nwOfferings.append(self.shared_network_offering_sg)

        self.debug("Shared Network offering created: %s" % self.shared_network_offering_sg.id)

        #Update network offering state from disabled to enabled.
        self.shared_network_offering_sg.update(self.api_client,state="enabled")

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["acltype"] = "domain"
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg")

        self.shared_network_sg = Network.create(self.api_client, self.services["shared_network_sg"], domainid=self.admin_account.domainid,
                                                networkofferingid=self.shared_network_offering_sg.id, zoneid=self.zone.id)

        self.cleanup_networks.append(self.shared_network_sg)

        list_networks_response = Network.list(
                                        self.api_client,
                                        id=self.shared_network_sg.id
                                        )
        self.assertEqual(validateList(list_networks_response)[0], PASS, "Networks list validation failed, list is %s" %
                list_networks_response)
        self.assertEqual(
            list_networks_response[0].specifyipranges,
            True,
            "The network is created with ip range but the flag is set to False."
            )

        self.debug("Shared Network created: %s" % self.shared_network_sg.id)

        try:
            virtual_machine = VirtualMachine.create(self.api_client,self.services["virtual_machine"],accountid=self.admin_account.name,
                                                    domainid=self.admin_account.domainid, networkids=[self.shared_network_sg.id],
						    serviceofferingid=self.service_offering.id
                                                    )
        except Exception as e:
            self.fail("Exception while deploying virtual machine: %s" % e)
        self.cleanup_vms.append(virtual_machine)

        vms = VirtualMachine.list(self.api_client, id=virtual_machine.id,listall=True)

        self.assertEqual(validateList(vms)[0], PASS, "vms list validation failed, list is %s" % vms)
        self.debug("Virtual Machine created: %s" % virtual_machine.id)
        return

    @attr(tags = ["advancedsg"])
    def test_06_SharedNwSGAccountSpecific(self):
        """ Test Account specific shared network creation with SG"""

        # Steps,
        #  1. create a user account
        #  2. Create one shared Network (scope=Account) specific to this account

        # Validations,
        #  1. shared network should be created successfully

        #Create admin account
        self.admin_account = Account.create(self.api_client,self.services["account"],admin=True,
                                            domainid=self.domain.id)

        self.cleanup_accounts.append(self.admin_account)

        #Create user account
        self.user_account = Account.create(self.api_client,self.services["account"],domainid=self.domain.id)

        self.cleanup_accounts.append(self.user_account)

        self.services["shared_network_offering_sg"]["specifyVlan"] = "True"
        self.services["shared_network_offering_sg"]["specifyIpRanges"] = "True"

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["acltype"] = "account"
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg")

        try:
            self.shared_network_sg_admin_account = Network.create(self.api_client, self.services["shared_network_sg"],
                                                                  accountid=self.admin_account.name, domainid=self.admin_account.domainid,
                                                                  networkofferingid=self.shared_network_offering_sg.id,
                                                                  zoneid=self.zone.id)
        except Exception as e:
            self.fail("Exception while creating account specific shared network: %s" % e)

        self.debug("Created shared network %s with admin account" % self.shared_network_sg_admin_account.id)

        list_networks_response = Network.list(
                                        self.api_client,
                                        id=self.shared_network_sg_admin_account.id
                                        )
        self.assertEqual(validateList(list_networks_response)[0], PASS, "networks list validation failed, list is %s" %
                list_networks_response)

        self.debug("Shared Network created: %s" % self.shared_network_sg_admin_account.id)

        self.debug("Creating shared account in user account: %s" % self.user_account.id)

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["vlan"] = vlan

        self.setSharedNetworkParams("shared_network_sg")

        try:
            self.shared_network_sg_user_account = Network.create(self.api_client,self.services["shared_network_sg"],
                                                             accountid=self.user_account.name,domainid=self.user_account.domainid,
                                                             networkofferingid=self.shared_network_offering_sg.id,
                                                             zoneid=self.zone.id)
        except Exception as e:
            self.fail("Exception while creating account specific shared network: %s" % e)

        self.debug("Created shared network %s with user account" % self.shared_network_sg_user_account.id)

        list_networks_response = Network.list(self.api_client,id=self.shared_network_sg_user_account.id)

        self.assertEqual(validateList(list_networks_response)[0], PASS, "networks list validation failed, list is %s" %
                list_networks_response)

        self.debug("Shared Network created: %s" % self.shared_network_sg_user_account.id)

        return

    @attr(tags = ["advancedsg"])
    def test_07_SharedNwSG_DomainWide_SubdomainAcccess(self):
        """ Test Domain wide shared network with SG, with subdomain access set True"""

        # Steps,
        #  1. create a Domain and subdomain
        #  2. Create one shared Network in parent domain with SG and set subdomain access True
        #  3. Deploy a VM in subdomain using the shared network

        # Validations,
        #  1. shared network should be created successfully
        #  2. Shared network should be able to be accessed within subdomain (VM should be deployed)

        #Create Domain
        self.debug("Creating parent domain")
        self.parent_domain = Domain.create(self.api_client, services=self.services["domain"],
                                           parentdomainid=self.domain.id)

        self.debug("Created domain %s" % self.parent_domain.id)
        self.debug("Creating child domain under this domain")
        self.child_domain = Domain.create(self.api_client,services=self.services["domain"],
                                          parentdomainid=self.parent_domain)

        self.debug("Created child domain: %s" % self.child_domain.id)

        self.services["shared_network_offering_sg"]["specifyVlan"] = "True"
        self.services["shared_network_offering_sg"]["specifyIpRanges"] = "True"

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["acltype"] = "domain"
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg")

        try:
            self.shared_network_sg = Network.create(self.api_client,self.services["shared_network_sg"],
                                                    domainid=self.parent_domain.id,networkofferingid=self.shared_network_offering_sg.id,
                                                    zoneid=self.zone.id,subdomainaccess=True)
        except Exception as e:
            self.fail("Exception whle creating domain wide shared network: %s" % e)

        self.debug("Created shared network: %s" % self.shared_network_sg.id)

        self.cleanup_networks.append(self.shared_network_sg)

        list_networks_response = Network.list(self.api_client,id=self.shared_network_sg.id,listall=True)

        self.debug("network response: %s" % list_networks_response)
        self.assertEqual(validateList(list_networks_response)[0], PASS, "networks list validation failed")

        self.debug("Shared Network created: %s" % self.shared_network_sg.id)

        try:
            virtual_machine = VirtualMachine.create(self.api_client, self.services["virtual_machine"],
                                                    domainid=self.child_domain.id,networkids=[self.shared_network_sg.id],
                                                    serviceofferingid=self.service_offering.id)
        except Exception as e:
            self.fail("Exception while deploying VM in domain wide shared network: %s" % e)

        self.debug("Created virtual machine %s within the shared network" % virtual_machine.id)

        self.cleanup_vms.append(virtual_machine)

        return

    @unittest.skip("Skip - Failing - WIP")
    @attr(tags = ["advancedsg"])
    def test_08_SharedNwSGAccountSpecific_samevlan_samesubnet(self):
        """ Test Account specific shared network creation with SG in multiple accounts
            with same subnet and vlan"""

        # Steps,
        #  1. create two different accouts
        #  2. create account specific shared networks in both accounts with same subnet and vlan id

        # Validations,
        #  1. shared networks should be created successfully

        #Create domain 1

        domain1 = Domain.create(self.api_client,services=self.services["domain"],parentdomainid=self.domain.id)

        self.debug("Created domain: %s" % domain1.id)
        self.cleanup_domains.append(domain1)

        account1 = Account.create(self.api_client,self.services["account"],domainid=domain1.id)

        self.debug("Created account %s under domain %s" % (account1.id, domain1.id))
        self.cleanup_accounts.append(account1)

        domain2 = Domain.create(self.api_client, services=self.services["domain"],parentdomainid=self.domain.id)

        self.debug("Created domain %s" % domain2.id)
        self.cleanup_domains.append(domain1)

        account2 = Account.create(self.api_client,self.services["account"],domainid=domain2.id)

        self.debug("Created account %s under domain %s" % (account2.id, domain2.id))
        self.cleanup_accounts.append(account1)

        self.services["shared_network_offering_sg"]["specifyVlan"] = "True"
        self.services["shared_network_offering_sg"]["specifyIpRanges"] = "True"

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["acltype"] = "account"
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg")

        self.debug("Creating shared network in account 1: %s" % account1.name)
        self.shared_network_sg_account1 = Network.create(self.api_client,self.services["shared_network_sg"],
                                                         accountid=account1.name,domainid=account1.domainid,
                                                         networkofferingid=self.shared_network_offering_sg.id,
                                                         zoneid=self.zone.id)

        self.debug("Created shared network: %s" % self.shared_network_sg_account1.id)

        self.debug("Creating shared network in account 2 with same subnet and vlan: %s" % account1.name)

        try:
            self.shared_network_sg_account2 = Network.create(self.api_client,self.services["shared_network_sg"],
                                                             accountid=account2.name,domainid=account2.domainid,
                                                             networkofferingid=self.shared_network_offering_sg.id,
                                                             zoneid=self.zone.id)
        except Exception as e:
            self.fail("Exception while creating account specific shared network with same subnet and vlan: %s" % e)

        self.debug("Created shared network: %s" % self.shared_network_sg_account2.id)

        return

    @unittest.skip("Skip - Failing - WIP")
    @attr(tags = ["advancedsg"])
    def test_09_SharedNwDomainWide_samevlan_samesubnet(self):
        """ Test Domain wide shared network creation with SG in different domains
            with same vlan and same subnet"""

        # Steps,
        #  1. create two different domains
        #  2. Create domain specific shared networks in both the domains using same subnet and vlan id

        # Validations,
        #  1. Shared networks should be created successfully

        #Create domain 1

        domain1 = Domain.create(self.api_client,services=self.services["domain"],parentdomainid=self.domain.id)

        self.debug("Created domain: %s" % domain1.id)
        self.cleanup_domains.append(domain1)

        account1 = Account.create(self.api_client,self.services["account"],
                                  domainid=domain1.id)

        self.debug("Created account %s under domain %s" % (account1.id, domain1.id))
        self.cleanup_accounts.append(account1)

        domain2 = Domain.create(self.api_client,services=self.services["domain"],parentdomainid=self.domain.id)

        self.debug("Created domain %s" % domain2.id)
        self.cleanup_domains.append(domain2)

        account2 = Account.create(self.api_client,self.services["account"],domainid=domain2.id)

        self.debug("Created account %s under domain %s" % (account2.id, domain2.id))
        self.cleanup_accounts.append(account2)

        self.services["shared_network_offering_sg"]["specifyVlan"] = "True"
        self.services["shared_network_offering_sg"]["specifyIpRanges"] = "True"

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["acltype"] = "domain"
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg")

        self.debug("Creating shared network domain 1: %s" % domain1.name)
        self.shared_network_sg_domain1 = Network.create(self.api_client,self.services["shared_network_sg"],
                                                        domainid=domain1.id,networkofferingid=self.shared_network_offering_sg.id,
                                                        zoneid=self.zone.id, subdomainaccess=True)

        self.debug("Created shared network: %s" % self.shared_network_sg_domain1.id)

        list_networks_response = Network.list(self.api_client,id=self.shared_network_sg_domain1.id,listall=True)

        self.assertEqual(validateList(list_networks_response)[0], PASS, "networks list validation failed, list: %s" %
                        list_networks_response)

        self.debug("Creating shared network in domain 2 with same subnet and vlan: %s" % domain2.name)

        try:
            self.shared_network_sg_domain2 = Network.create(self.api_client,self.services["shared_network_sg"],
                                                            domainid=domain2.id,networkofferingid=self.shared_network_offering_sg.id,
                                                            zoneid=self.zone.id,subdomainaccess=True)
        except Exception as e:
            self.fail("Exception while creating domain wide shared network with same subnet and vlan: %s" % e)

        self.debug("Created shared network: %s" % self.shared_network_sg_domain2.id)

        list_networks_response = Network.list(self.api_client,id=self.shared_network_sg_domain2.id)

        self.assertEqual(validateList(list_networks_response)[0], PASS, "networks list validation failed, list: %s" %
                        list_networks_response)

        return

    @attr(tags = ["advancedsg"])
    def test_10_deleteSharedNwSGAccountSpecific_InUse(self):
        """ Test delete Account specific shared network creation with SG which is in use"""

        # Steps,
        #  1. create a user account
        #  2. Create account specific sg enabled shared network
        #  3. Deply vm in the shared network
        #  4. Try to delete shared network while it's stil in use by the vm

        # Validations,
        #  1. shared network deletion should fail

        #Create admin account
        self.admin_account = Account.create(self.api_client,self.services["account"],admin=True,
                                            domainid=self.domain.id)

        self.cleanup_accounts.append(self.admin_account)

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["acltype"] = "account"
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg")

        shared_network_sg_account = Network.create(self.api_client,self.services["shared_network_sg"],
                                                   accountid=self.admin_account.name,domainid=self.admin_account.domainid,
                                                   networkofferingid=self.shared_network_offering_sg.id,
                                                   zoneid=self.zone.id)

        self.debug("Shared Network created: %s" % shared_network_sg_account.id)

        self.debug("Deploying vm in the shared network: %s" % shared_network_sg_account.id)

        vm  = VirtualMachine.create(self.api_client, self.services["virtual_machine"],
                                    accountid=self.admin_account.name,domainid=self.admin_account.domainid,
                                    networkids=[shared_network_sg_account.id,],serviceofferingid=self.service_offering.id)
        self.debug("Created vm %s" % vm.id)

        self.debug("Trying to delete shared network: %s" % shared_network_sg_account.id)

        try:
            shared_network_sg_account.delete(self.api_client)
            self.fail("Exception not raised while deleting network")
        except Exception as e:
            self.debug("Network deletion failed with exception: %s" % e)

        return

    @attr(tags = ["advancedsg"])
    def test_11_deleteSharedNwSG_DomainWide_InUse(self):
        """ Test delete Domain wide shared network with SG which is in use"""

        # Steps,
        #  1. create a domain and its admin account
        #  2. Create domain wide shared network with sg and subdomainaccess enabled
        #  3. Deploy vm in the domain
        #  4. Try to delete the shared network while its still in use

        # Validations,
        #  1. shared network deletion should fail

        domain = Domain.create(self.api_client, services=self.services["domain"],
                               parentdomainid=self.domain.id)

        self.debug("Created domain: %s" % domain.id)

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["acltype"] = "domain"
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg")

        self.debug("Creating shared network domain: %s" % domain.name)
        shared_network_sg_domain = Network.create(self.api_client,self.services["shared_network_sg"],
                                                  domainid=domain.id,networkofferingid=self.shared_network_offering_sg.id,
                                                  zoneid=self.zone.id,subdomainaccess=True)

        self.debug("Created shared network: %s" % shared_network_sg_domain.id)

        self.debug("Deploying vm in the shared network: %s" % shared_network_sg_domain.id)

        vm  = VirtualMachine.create(self.api_client,self.services["virtual_machine"],domainid=domain.id,
                                    networkids=[shared_network_sg_domain.id,],serviceofferingid=self.service_offering.id)
        self.debug("Created vm %s" % vm.id)

        self.debug("Trying to delete shared network: %s" % shared_network_sg_domain.id)

        try:
            shared_network_sg_domain.delete(self.api_client)
            self.fail("Exception not raised while deleting network")
        except Exception as e:
            self.debug("Network deletion failed with exception: %s" % e)

        self.cleanup_networks.append(shared_network_sg_domain)
        self.cleanup_vms.append(vm)
        self.cleanup_domains.append(domain)

        return

    @attr(tags = ["advancedsg"])
    def test_29_deleteSharedNwSG_ZoneWide_InUse(self):
        """ Test delete Zone wide shared network with SG which is in use"""

        # Steps,
        #  1. create zone wide shared network
        #  2. Deploy vm in the shared network
        #  3. Try to delete the shared network while its still in use

        # Validations,
        #  1. shared network deletion should fail

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["acltype"] = "domain"
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg")

        self.debug("Creating shared network in zone: %s" % self.zone.id)
        shared_network_sg = Network.create(self.api_client,self.services["shared_network_sg"],
                                                  networkofferingid=self.shared_network_offering_sg.id,
                                                  zoneid=self.zone.id)

        self.debug("Created shared network: %s" % shared_network_sg.id)

        self.debug("Deploying vm in the shared network: %s" % shared_network_sg.id)

        vm  = VirtualMachine.create(self.api_client,self.services["virtual_machine"],
                                    networkids=[shared_network_sg.id,],serviceofferingid=self.service_offering.id)
        self.debug("Created vm %s" % vm.id)

        self.debug("Trying to delete shared network: %s" % shared_network_sg.id)

        try:
            shared_network_sg.delete(self.api_client)
            self.fail("Exception not raised while deleting network")
        except Exception as e:
            self.debug("Network deletion failed with exception: %s" % e)

        self.cleanup_networks.append(shared_network_sg)
        self.cleanup_vms.append(vm)
        return

    @attr(tags = ["advancedsg"])
    def test_12_deleteSharedNwSGAccountSpecific_NotInUse(self):
        """ Test delete Account specific shared network creation with SG which is not in use"""

        # Steps,
        #  1. create a user account
        #  2. Create account specific sg enabled shared network
        #  3. Try to delete shared network

        # Validations,
        #  1. shared network deletion should succeed

        #Create admin account
        self.admin_account = Account.create(self.api_client,self.services["account"],
                                            admin=True,domainid=self.domain.id)

        self.cleanup_accounts.append(self.admin_account)

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["acltype"] = "account"
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg")

        shared_network_sg_account = Network.create(self.api_client,self.services["shared_network_sg"],
                                                   accountid=self.admin_account.name,domainid=self.admin_account.domainid,
                                                   networkofferingid=self.shared_network_offering_sg.id,
                                                   zoneid=self.zone.id)

        self.debug("Shared Network created: %s" % shared_network_sg_account.id)

        try:
            shared_network_sg_account.delete(self.api_client)
        except Exception as e:
            self.fail("Network deletion failed with exception: %s" % e)

        return

    @attr(tags = ["advancedsg"])
    def test_13_deleteSharedNwSG_DomainWide_NotInUse(self):
        """ Test delete Domain wide shared network with SG which is not in use"""

        # Steps,
        #  1. create a domain and its admin account
        #  2. Create domain wide shared network with sg and subdomainaccess enabled
        #  4. Try to delete the shared network

        # Validations,
        #  1. shared network deletion should succeed

        domain = Domain.create(self.api_client,services=self.services["domain"],
                               parentdomainid=self.domain.id)

        self.debug("Created domain: %s" % domain.id)
        self.cleanup_domains.append(domain)

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["acltype"] = "domain"
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg")

        self.debug("Creating shared network domain: %s" % domain.name)
        shared_network_sg_domain = Network.create(self.api_client,self.services["shared_network_sg"],
                                                  domainid=domain.id,networkofferingid=self.shared_network_offering_sg.id,
                                                  zoneid=self.zone.id,subdomainaccess=True)

        self.debug("Created shared network: %s" % shared_network_sg_domain.id)

        self.debug("Trying to delete shared network: %s" % shared_network_sg_domain.id)

        try:
            shared_network_sg_domain.delete(self.api_client)
        except Exception as e:
            self.fail("Network deletion failed with exception: %s" % e)

        return

    @attr(tags = ["advancedsg"])
    def test_30_deleteSharedNwSG_ZoneWide_NotInUse(self):
        """ Test delete zone wide shared network with SG which is not in use"""

        # Steps,
        #  1. create a zone wide shared network with SG
        #  2. Try to delete the shared network

        # Validations,
        #  1. shared network deletion should succeed

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["acltype"] = "domain"
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg")

        self.debug("Creating shared network in zone: %s" % self.zone.id)
        shared_network_sg = Network.create(self.api_client,self.services["shared_network_sg"],
                                                  networkofferingid=self.shared_network_offering_sg.id,
                                                  zoneid=self.zone.id)

        self.debug("Created shared network: %s" % shared_network_sg.id)

        self.debug("Trying to delete shared network: %s" % shared_network_sg.id)

        try:
            shared_network_sg.delete(self.api_client)
        except Exception as e:
            self.fail("Network deletion failed with exception: %s" % e)

        return

    @attr(tags = ["advancedsg"])
    def test__14_createSharedNwWithSG_withoutParams(self):
        """ Test create shared network with SG without specifying necessary parameters"""

        # Steps,
        #  1. Test create shared network with SG without specoifying necessary parameters
        #     such as vlan, startip, endip, gateway, netmask
        # Validations,
        #  1. shared network creation should fail

        #Create admin account
        self.admin_account = Account.create(self.api_client,self.services["account"], admin=True,
                                            domainid=self.domain.id)

        self.cleanup_accounts.append(self.admin_account)

        self.services["shared_network_offering_sg"]["specifyVlan"] = "True"
        self.services["shared_network_offering_sg"]["specifyIpRanges"] = "True"

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["acltype"] = "domain"
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["gateway"] = ""
        self.services["shared_network_sg"]["startip"] = ""
        self.services["shared_network_sg"]["endip"] = ""
        self.services["shared_network_sg"]["netmask"] = ""


        try:
            self.shared_network_sg = Network.create(self.api_client,self.services["shared_network_sg"],
                                                    domainid=self.admin_account.domainid,
                                                    networkofferingid=self.shared_network_offering_sg.id,
                                                    zoneid=self.zone.id)

            self.cleanup_networks.append(self.shared_network_sg)
            self.fail("Shared network created successfully without specifying essential parameters")
        except Exception as e:
            self.debug("Shared network creation failed as expected with exception: %s" % e)

        return

    @attr(tags = ["advancedsg"])
    def test__15_createVPC(self):
        """ Test create VPC in advanced SG enabled zone"""

        # Steps,
        #  1. Try to create VPC in SG enabled advanced zone
        # Validations,
        #  1. VPC creation should fail

        vpc_off = VpcOffering.create(self.api_client,self.services["vpc_offering"])
        vpc_off.update(self.api_client, state='Enabled')

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("Trying to create a VPC network in SG enabled zone: %s" %
                                                            self.zone.id)
        try:
            vpc_1 = VPC.create(self.api_client,self.services["vpc"],
                               vpcofferingid=vpc_off.id,zoneid=self.zone.id)
            vpc_1.delete(self.api_client)
            self.fail("VPC creation should fail in Security Group Enabled zone")
        except Exception as e:
            self.debug("VPC creation failed as expected with exception: %s" % e)
        finally:
            vpc_off.update(self.api_client, state="Disabled")
            vpc_off.delete(self.api_client)

        return

@ddt
class TestNetworksInAdvancedSG_VmOperations(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestNetworksInAdvancedSG_VmOperations, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(cls.api_client,cls.zone.id,cls.services["ostype"])

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(cls.api_client,cls.services["service_offering"])

        cls.services["shared_network_offering_sg"]["specifyVlan"] = "True"
        cls.services["shared_network_offering_sg"]["specifyIpRanges"] = "True"
        #Create Network Offering
        cls.shared_network_offering_sg = NetworkOffering.create(cls.api_client,cls.services["shared_network_offering_sg"],conservemode=False)

        #Update network offering state from disabled to enabled.
        NetworkOffering.update(cls.shared_network_offering_sg,cls.api_client,state="enabled")

        cls._cleanup = [
                        cls.service_offering,
                        cls.shared_network_offering_sg
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Update network offering state from enabled to disabled.
            cls.shared_network_offering_sg.update(cls.api_client,state="disabled")
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.api_client = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.cleanup_networks = []
        self.cleanup_accounts = []
        self.cleanup_domains = []
        self.cleanup_projects = []
        self.cleanup_vms = []
        self.cleanup_secGrps = []
        return

    def tearDown(self):
        exceptions = []
        try:
            #Clean up, terminate the created network offerings
            cleanup_resources(self.api_client, self.cleanup)
        except Exception as e:
            self.debug("Warning: Exception during cleanup : %s" % e)
            exceptions.append(e)

        #below components is not a part of cleanup because to mandate the order and to cleanup network
        try:
            for vm in self.cleanup_vms:
                vm.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during virtual machines cleanup : %s" % e)
            exceptions.append(e)

        # Wait for VMs to expunge
        wait_for_cleanup(self.api_client, ["expunge.delay", "expunge.interval"])

        try:
            for sec_grp in self.cleanup_secGrps:
                sec_grp.delete(self.api_client)
        except Exception as e:
            self.debug("Warning : Exception during security groups cleanup: %s" % e)
            exceptions.append(e)

        try:
            for project in self.cleanup_projects:
                project.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during project cleanup : %s" % e)
            exceptions.append(e)

        try:
            for account in self.cleanup_accounts:
                account.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during account cleanup : %s" % e)
            exceptions.append(e)

        #Wait till all resources created are cleaned up completely and then attempt to delete Network
        time.sleep(self.services["sleep"])

        try:
            for network in self.cleanup_networks:
                network.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during network cleanup : %s" % e)
            exceptions.append(e)

        try:
            for domain in self.cleanup_domains:
                domain.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during domain cleanup : %s" % e)
            exceptions.append(e)

        if len(exceptions) > 0:
            self.fail("There were exceptions during cleanup: %s" % exceptions)

        return

    def setSharedNetworkParams(self, network, range=20):

        # @range: range decides the endip. Pass the range as "x" if you want the difference between the startip
        # and endip as "x"
        # Set the subnet number of shared networks randomly prior to execution
        # of each test case to avoid overlapping of ip addresses
        shared_network_subnet_number = random.randrange(1,254)

        self.services[network]["gateway"] = "172.16."+str(shared_network_subnet_number)+".1"
        self.services[network]["startip"] = "172.16."+str(shared_network_subnet_number)+".2"
        self.services[network]["endip"] = "172.16."+str(shared_network_subnet_number)+"."+str(range+1)
        self.services[network]["netmask"] = "255.255.255.0"

        return

    def setVmState(self, vm, state):
        """ set VM state and verify if it is reflected correctly
            Currently takes 2 states - running and stopped"""

        if state=="running":
            vm.start(self.api_client)
        elif state=="stopped":
            vm.stop(self.api_client)
        else:
            self.fail("Invalid state passed")
        retriesCount = 5
        while True:
            vm_list = list_virtual_machines(self.api_client, id=vm.id)
            self.assertEqual(validateList(vm_list)[0], PASS, "vm list validation failed, vm list is %s" % vm_list)
            if vm_list[0].state.lower() == state:
                break
            if retriesCount == 0:
                self.fail("Failed to set VM state as %s" % state)
            retriesCount -= 1
            time.sleep(10)

        return

    def dump_config_deploy_DC(self):
        configLines = []
        #Read zone and ip range information from config file
        file = self.services["test_34_DeployVM_in_SecondSGNetwork"]["config"]
        with open(file, 'r') as fp:
            for line in fp:
                ws = line.strip()
                if not ws.startswith("#"):
                    configLines.append(ws)
        config = json.loads("\n".join(configLines))
        config['zones'][0]['name'] = self.services["test_34_DeployVM_in_SecondSGNetwork"]["zone"]
        config['zones'][0]['ipranges'][0]['startip'] = \
            self.services["test_34_DeployVM_in_SecondSGNetwork"]["ipranges"][0]["startip"]
        config['zones'][0]['ipranges'][0]['endip'] = \
            self.services["test_34_DeployVM_in_SecondSGNetwork"]["ipranges"][0]["endip"]
        config['zones'][0]['ipranges'][0]['vlan'] = \
            self.services["test_34_DeployVM_in_SecondSGNetwork"]["ipranges"][0]["vlan"]
        config['zones'][0]['ipranges'][0]['gateway'] = \
            self.services["test_34_DeployVM_in_SecondSGNetwork"]["ipranges"][0]["gateway"]
        config["dbSvr"]["dbSvr"] = self.services["test_34_DeployVM_in_SecondSGNetwork"]["dbSvr"]["dbSvr"]
        config["dbSvr"]["passwd"] = self.services["test_34_DeployVM_in_SecondSGNetwork"]["dbSvr"]["passwd"]
        config["dbSvr"]["db"] = self.services["test_34_DeployVM_in_SecondSGNetwork"]["dbSvr"]["db"]
        config["dbSvr"]["port"] = self.services["test_34_DeployVM_in_SecondSGNetwork"]["dbSvr"]["port"]
        config["dbSvr"]["user"] = self.services["test_34_DeployVM_in_SecondSGNetwork"]["dbSvr"]["user"]
        config['mgtSvr'][0]['mgtSvrIp'] = self.services["test_34_DeployVM_in_SecondSGNetwork"]["mgtSvr"][0]["mgtSvrIp"]
        config['mgtSvr'][0]["passwd"] = self.services["test_34_DeployVM_in_SecondSGNetwork"]["mgtSvr"][0]["passwd"]
        config['mgtSvr'][0]["user"] = self.services["test_34_DeployVM_in_SecondSGNetwork"]["mgtSvr"][0]["user"]
        config['mgtSvr'][0]["port"] = self.services["test_34_DeployVM_in_SecondSGNetwork"]["mgtSvr"][0]["port"]
        config['zones'][0]['pods'][0]['clusters'][0]['primaryStorages'][0]['url'] = \
            "nfs://10.147.28.6:/export/home/sandbox/primary_"+str(random.randrange(0,1000,3))
        config['zones'][0]['pods'][0]['clusters'][0]['primaryStorages'][0]['name'] = \
            "PS_"+str(random.randrange(0,1000,3))
        config['zones'][0]['secondaryStorages'][0]['url'] = \
            "nfs://10.147.28.6:/export/home/sandbox/sstor_"+str(random.randrange(0,1000,3))
        if system().lower() != 'windows':
            config_file = "/tmp/advsg.cfg"
            with open(config_file, 'w+') as fp:
                fp.write(json.dump(config, indent=4))
            cfg_file = file.split('/')[-1]
            file2 = file.replace("/setup/dev/advanced/"+cfg_file, "")
            file2 = file2+"/tools/marvin/marvin/deployDataCenter.py"
        else :
            config_file = "D:\\advsg.cfg"
            with open(config_file, 'w+') as fp:
                fp.write(json.dumps(config, indent=4))
            cfg_file = file.split('\\')[-1]
            file2 = file.replace("\setup\dev\\"+cfg_file, "")
            file2 = file2+"\\tools\marvin\marvin\deployDataCenter.py"
        #Run deployDataCenter with new config file stored in \tmp
        self.debug("Executing deployAndRun")
        status = os.system("%s -i %s" %(file2, config_file))
        return status

    @attr(tags = ["advancedsg"])
    def test__16_AccountSpecificNwAccess(self):
        """ Test account specific network access of users"""

        # Steps,
        #  1. create multiple accounts/users and their account specific SG enabled shared networks
        #  2. Deploy VM in the account specific network with user of that account
        #  3. Try to deploy VM in one account specific network from other account user

        # Validations,
        #  1. VM deployment should be allowed for the users of the same account only for their account
        #     specific networks respectively

        self.debug("Creating user account 1")
        account_1 = Account.create(self.api_client,self.services["account"],
                                   domainid=self.domain.id)
        self.debug("Created account : %s" % account_1.name)

        self.cleanup_accounts.append(account_1)

        self.debug("Creating user account 2")
        account_2 = Account.create(self.api_client,self.services["account"],
                                   domainid=self.domain.id)
        self.debug("Created account: %s" % account_2.name)

        self.cleanup_accounts.append(account_2)

        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id
        self.services["shared_network_sg"]["acltype"] = "account"

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg")

        shared_network_account_1 = Network.create(self.api_client,self.services["shared_network_sg"],
                                                  accountid=account_1.name,domainid=account_1.domainid,
                                                  networkofferingid=self.shared_network_offering_sg.id,zoneid=self.zone.id)

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg")

        shared_network_account_2 = Network.create(self.api_client,self.services["shared_network_sg"],
                                                  accountid=account_2.name,domainid=account_2.domainid,
                                                  networkofferingid=self.shared_network_offering_sg.id,
                                                  zoneid=self.zone.id)

        # Creaint user API client of account 1
        api_client_account_1 = self.testClient.getUserApiClient(UserName=account_1.name,
                                                         DomainName=account_1.domain)

        # Creaint user API client of account 2
        api_client_account_2 = self.testClient.getUserApiClient(UserName=account_2.name,
                                                         DomainName=account_2.domain)

        self.debug("Creating virtual machine in account %s with account specific shared network %s" %
                  (account_1.name, shared_network_account_1.id))

        VirtualMachine.create(api_client_account_1,self.services["virtual_machine"],
                              templateid=self.template.id,accountid=account_1.name,
                              domainid=account_1.domainid,networkids=[shared_network_account_1.id,],
                              serviceofferingid=self.service_offering.id)

        self.debug("Creating virtual machine in account %s with account specific shared network %s" %
                  (account_2.name, shared_network_account_2.id))

        VirtualMachine.create(api_client_account_2,self.services["virtual_machine"],
                              templateid=self.template.id,accountid=account_2.name,
                              domainid=account_2.domainid,networkids=[shared_network_account_2.id,],
                              serviceofferingid=self.service_offering.id)

        try:
            self.debug("Trying to create virtual machine in account specific shared network of\
                        account %s using the api client of account %s, this should fail" %
                        (account_2.name, account_1.name))

            VirtualMachine.create(api_client_account_2,self.services["virtual_machine"],
                                  templateid=self.template.id,accountid=account_1.name,
                                  domainid=account_1.domainid,networkids=[shared_network_account_1.id,],
                                  serviceofferingid=self.service_offering.id)

            self.fail("Vm creation succeded, should have failed")
        except Exception as e:
            self.debug("VM creation failed as expected with exception: %s" % e)

        return

    @attr(tags = ["advancedsg"])
    def test__17_DomainSpecificNwAccess(self):
        """ Test domain specific network access of users"""

        # Steps,
        #  1. create multiple domains with accounts/users in them and their domain wide SG enabled shared networks
        #  2. Deploy VMs in the domain wide network with user belonging to that domain
        #  3. Try to deploy VM in domain wide network with user of other domain

        # Validations,
        #  1. VM deployment should be allowed only for the users of the same domain for their domain
        #     wide networks respectively

        self.debug("Creating domain 1")
        domain_1 = Domain.create(self.api_client,services=self.services["domain"],
                                 parentdomainid=self.domain.id)

        self.debug("Created domain %s" % domain_1.name)
        self.cleanup_domains.append(domain_1)

        self.debug("Creating domain 2")
        domain_2 = Domain.create(self.api_client,services=self.services["domain"],
                                 parentdomainid=self.domain.id)

        self.debug("Created domain: %s" % domain_2.name)
        self.cleanup_domains.append(domain_2)

        self.debug("Creating user account under domain %s" % domain_1.name)
        account_1 = Account.create(self.api_client,self.services["account"],
                                   domainid=domain_1.id)
        self.debug("Created account : %s" % account_1.name)
        self.cleanup_accounts.append(account_1)
        self.debug("Creating user account under domain %s" % domain_2.name)
        account_2 = Account.create(self.api_client,self.services["account"],
                                   domainid=domain_2.id)

        self.debug("Created account: %s" % account_2.name)
        self.cleanup_accounts.append(account_2)

        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id
        self.services["shared_network_sg"]["acltype"] = "domain"

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg")

        shared_network_domain_1 = Network.create(self.api_client,self.services["shared_network_sg"],
                                                 domainid=domain_1.id,networkofferingid=self.shared_network_offering_sg.id,
                                                 zoneid=self.zone.id)

        self.debug("Created network: %s" % shared_network_domain_1)
        self.cleanup_networks.append(shared_network_domain_1)

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg")

        shared_network_domain_2 = Network.create(self.api_client,self.services["shared_network_sg"],
                                                 domainid=domain_2.id,networkofferingid=self.shared_network_offering_sg.id,
                                                 zoneid=self.zone.id)

        self.debug("Created network: %s" % shared_network_domain_2.name)
        self.cleanup_networks.append(shared_network_domain_2)

        # Creaint user API client of account 1
        api_client_domain_1 = self.testClient.getUserApiClient(UserName=account_1.name,
                                                         DomainName=account_1.domain)

        # Creaint user API client of account 2
        api_client_domain_2 = self.testClient.getUserApiClient(UserName=account_2.name,
                                                         DomainName=account_2.domain)

        self.debug("Creating virtual machine in domain %s with domain wide shared network %s" %
                  (domain_1.name, shared_network_domain_1.id))
        vm_1 = VirtualMachine.create(api_client_domain_1,self.services["virtual_machine"],
                                     templateid=self.template.id,domainid=domain_1.id,
                                     networkids=[shared_network_domain_1.id,],
                                     serviceofferingid=self.service_offering.id)
        self.debug("created vm: %s" % vm_1.id)

        self.debug("Creating virtual machine in domain %s with domain wide shared network %s" %
                  (domain_2.name, shared_network_domain_2.id))
        vm_2 = VirtualMachine.create(api_client_domain_2,self.services["virtual_machine"],
                                     templateid=self.template.id,domainid=domain_2.id,
                                     networkids=[shared_network_domain_2.id,],
                                     serviceofferingid=self.service_offering.id)
        self.debug("created vnm: %s" % vm_2.id)

        try:
            self.debug("Trying to create virtual machine in domain wide shared network of\
                        domain %s using the api client of account in domain %s, this should fail" %
                        (domain_2.name, domain_1.name))
            VirtualMachine.create(api_client_domain_2,self.services["virtual_machine"],
                                  templateid=self.template.id,domainid=domain_1.id,
                                  networkids=[shared_network_domain_1.id,],
                                  serviceofferingid=self.service_offering.id)
            self.fail("Vm creation succeded, should have failed")
        except Exception as e:
            self.debug("VM creation failed as expected with exception: %s" % e)

        return

    @data("account", "domain")
    @attr(tags = ["advancedsg"])
    def test_18_DeployVM_NoFreeIPs(self, value):
        """ Test deploy VM in account/domain specific SG enabled shared network when no free IPs are available"""

        # Steps,
        #  1. Create account/domain wide shared network in SG enabled zone.
        #  2. Exhaust the free IPs in the network (by deploying vm in it)
        #  3. Try to deploy VM when no free IPs are available

        # Validations,
        #  1. VM deployment should fail when no free IPs are available

        self.debug("Creating domain")
        domain = Domain.create(self.api_client,services=self.services["domain"],parentdomainid=self.domain.id)
        self.cleanup_domains.append(domain)
        self.debug("Created domain: %s" % domain.name)

        self.debug("Creating account")
        account = Account.create(self.api_client,self.services["account"],domainid=domain.id)
        self.debug("Created account : %s" % account.name)
        self.cleanup_accounts.append(account)
        self.services["shared_network_sg"]["acltype"] = value

        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg", range=2)

        shared_network = Network.create(self.api_client,self.services["shared_network_sg"],
                                        accountid=account.name,domainid=domain.id,
                                        networkofferingid=self.shared_network_offering_sg.id,zoneid=self.zone.id)

        if value == "domain":
            self.cleanup_networks.append(shared_network)

        # Deploying 1 VM will exhaust the IP range because we are passing range as 2, and one of the IPs
        # already gets consumed by the virtual router of the shared network

        self.debug("Creating virtual machine in %s wide shared network %s" %
                (value, shared_network.id))
        VirtualMachine.create(self.api_client,self.services["virtual_machine"],
                              templateid=self.template.id,accountid=account.name,
                              domainid=domain.id,networkids=[shared_network.id,],
                              serviceofferingid=self.service_offering.id)

        try:
            self.debug("Trying to create virtual machine when all the IPs are consumed")
            vm = VirtualMachine.create(self.api_client,self.services["virtual_machine"],
                                       templateid=self.template.id,accountid=account.name,
                                       domainid=domain.id,networkids=[shared_network.id,],
                                       serviceofferingid=self.service_offering.id)
            self.cleanup_vms.append(vm)
            self.fail("Vm creation succeded, should have failed")
        except Exception as e:
            self.debug("VM creation failed as expected with exception: %s" % e)

        return

    @attr(tags = ["advancedsg"])
    def test_19_DeployVM_DefaultSG(self):
        """ Test deploy VM in default security group"""

        # Steps,
        #  1. List security groups with root admin api
        #  2. Deploy VM with the default security group

        # Validations,
        #  1. VM deployment should be successful with default security group

        self.debug("Listing security groups")
        securitygroups = SecurityGroup.list(self.api_client)

        self.assertEqual(validateList(securitygroups)[0], PASS, "securitygroups list validation\
                failed, securitygroups list is %s" % securitygroups)
        defaultSecurityGroup = securitygroups[0]

        self.debug("Default security group: %s" % defaultSecurityGroup.id)

        try:
            self.debug("Trying to create virtual machine with the default security group")
            vm = VirtualMachine.create(self.api_client,self.services["virtual_machine"],
                                       templateid=self.template.id,securitygroupids=[defaultSecurityGroup.id,],
                                       serviceofferingid=self.service_offering.id)
            self.debug("Deployed Vm: %s" % vm.name)
            self.cleanup_vms.append(vm)

            self.debug("Listing vms")
            vm_list = list_virtual_machines(self.api_client, id=vm.id)
            self.assertEqual(validateList(vm_list)[0], PASS, "vm list validation failed,\
                     vm list is %s" % vm_list)
            self.assertTrue(defaultSecurityGroup.id in [secgrp.id for secgrp in vm_list[0].securitygroup],
                    "default sec group %s not present in the vm's sec group list %s" %
                    (defaultSecurityGroup.id, vm_list[0].securitygroup))
        except Exception as e:
            self.fail("VM creation with default security group failed with exception: %s" % e)

        return

    @data("default", "custom")
    @attr(tags = ["advancedsg"])
    def test_20_DeployVM_SecGrp_sharedNetwork(self, value):
        """ Test deploy VM in default/custom security group and shared network"""

        # Steps,
        #  1. Create shared network with SG provider
        #  2. Deploy VM with the default/(or custom) security group and above created shared network

        # Validations,
        #  1. VM deployment should be successful with default security group and shared network

        securityGroup = None
        if value == "default":
            self.debug("Listing security groups")
            securitygroups = SecurityGroup.list(self.api_client)

            self.assertEqual(validateList(securitygroups)[0], PASS, "securitygroups list validation\
                failed, securitygroups list is %s" % securitygroups)
            securityGroup = securitygroups[0]
        else:
            self.services["security_group"]["name"] = "Custom_sec_grp_" + random_gen()
            securityGroup = SecurityGroup.create(self.api_client, self.services["security_group"])
            self.cleanup_secGrps.append(securityGroup)

        self.debug("%s security group: %s" % (value,securityGroup.id))

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id
        self.services["shared_network_sg"]["acltype"] = "domain"
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id

        self.setSharedNetworkParams("shared_network_sg")

        self.debug("Creating shared network")

        shared_network = Network.create(self.api_client,self.services["shared_network_sg"],
                                        networkofferingid=self.shared_network_offering_sg.id,zoneid=self.zone.id)
        self.debug("Created shared network: %s" % shared_network.id)
        self.cleanup_networks.append(shared_network)

        try:
            self.debug("Trying to create virtual machine with the default security group %s and\
                    shared network %s" % (securityGroup.id, shared_network.id))
            vm = VirtualMachine.create(self.api_client,self.services["virtual_machine"],
                                       templateid=self.template.id,securitygroupids=[securityGroup.id,],
                                       serviceofferingid=self.service_offering.id, networkids=[shared_network.id,])
            self.debug("Deployed Vm: %s" % vm.name)
            self.cleanup_vms.append(vm)
        except Exception as e:
            self.fail("VM creation failed with exception: %s" % e)

        return

    @attr(tags = ["advancedsg"])
    def test_24_DeployVM_Multiple_Shared_Networks(self):
        """ Test deploy VM in multiple zone wide shared networks"""

        # Steps,
        #  1. Create multiple zone wide shared networks
        #  2. Try to deploy VM using all these networks

        # Validations,
        #  1. VM deployment should fail saying "error 431 Only support one zone wide network per VM if security group enabled"

        self.services["shared_network_sg"]["acltype"] = "domain"
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg")

        shared_network_1 = Network.create(self.api_client,self.services["shared_network_sg"],
                                          networkofferingid=self.shared_network_offering_sg.id,zoneid=self.zone.id)
        self.cleanup_networks.append(shared_network_1)
        self.debug("Created shared network: %s" % shared_network_1.id)

        self.services["shared_network_sg"]["vlan"] = get_free_vlan(self.api_client, self.zone.id)[1]
        self.setSharedNetworkParams("shared_network_sg")

        shared_network_2 = Network.create(self.api_client,self.services["shared_network_sg"],
                                          networkofferingid=self.shared_network_offering_sg.id,zoneid=self.zone.id)
        self.cleanup_networks.append(shared_network_2)
        self.debug("Created shared network: %s" % shared_network_2.id)

        try:
            self.debug("Creating virtual machine in zone wide shared networks %s and %s, this should fail" %
                      (shared_network_1.id, shared_network_2.id))
            vm = VirtualMachine.create(self.api_client,self.services["virtual_machine"],
                                       templateid=self.template.id, networkids=[shared_network_1.id, shared_network_2.id],
                                       serviceofferingid=self.service_offering.id)
            self.cleanup_vms.append(vm)
            self.fail("Vm creation should have failed, it succeded, created vm %s" % vm.id)
        except Exception as e:
            self.debug("VM creation failed as expected with exception: %s" % e)

        return

    @attr(tags = ["advancedsg"])
    def test_25_Deploy_Multiple_VM_Different_Shared_Networks_Same_SG(self):
        """ Test deploy Multiple VMs in different shared networks but same security group"""

        # Steps,
        #  1. Create multiple zone wide shared networks
        #  2. Create a custom security group
        #  3. Deploy Multiple VMs in different shared networks but using the same custom
        #     security group

        # Validations,
        #  1. VM deployments should succeed

        self.services["shared_network_sg"]["acltype"] = "domain"
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg")

        shared_network_1 = Network.create(self.api_client,self.services["shared_network_sg"],
                                          networkofferingid=self.shared_network_offering_sg.id,zoneid=self.zone.id)
        self.cleanup_networks.append(shared_network_1)
        self.debug("Created shared network: %s" % shared_network_1.id)

        self.services["shared_network_sg"]["vlan"] = get_free_vlan(self.api_client, self.zone.id)[1]
        self.setSharedNetworkParams("shared_network_sg")

        shared_network_2 = Network.create(self.api_client,self.services["shared_network_sg"],
                                          networkofferingid=self.shared_network_offering_sg.id,zoneid=self.zone.id)
        self.cleanup_networks.append(shared_network_2)
        self.debug("Created shared network: %s" % shared_network_2.id)

        self.services["security_group"]["name"] = "Custom_sec_grp_" + random_gen()
        sec_grp_1 = SecurityGroup.create(self.api_client,self.services["security_group"])
        self.debug("Created security groups: %s" % sec_grp_1.id)
        self.cleanup_secGrps.append(sec_grp_1)

        self.debug("Creating virtual machine in shared network %s and security group %s" %
                      (shared_network_1.id, sec_grp_1.id))
        vm_1 = VirtualMachine.create(self.api_client,self.services["virtual_machine"],
                                   templateid=self.template.id, networkids=[shared_network_1.id],
                                   serviceofferingid=self.service_offering.id,
                                   securitygroupids=[sec_grp_1.id])
        self.cleanup_vms.append(vm_1)

        self.debug("Creating virtual machine in shared network %s and security group %s" %
                      (shared_network_2.id, sec_grp_1.id))
        vm_2 = VirtualMachine.create(self.api_client,self.services["virtual_machine"],
                                   templateid=self.template.id, networkids=[shared_network_2.id],
                                   serviceofferingid=self.service_offering.id,
                                   securitygroupids=[sec_grp_1.id])
        self.cleanup_vms.append(vm_2)

        vm_list = list_virtual_machines(self.api_client, listall=True)

        self.assertEqual(validateList(vm_list)[0], PASS, "vm list validation failed, vm list is %s" % vm_list)

        vm_ids = [vm.id for vm in vm_list]
        self.assertTrue(vm_1.id in vm_ids, "vm %s not present in vm list %s" % (vm_1.id, vm_ids))
        self.assertTrue(vm_1.id in vm_ids, "vm %s not present in vm list %s" % (vm_2.id, vm_ids))

        return

    @attr(tags = ["advancedsg"])
    def test_26_Destroy_Deploy_VM_NoFreeIPs(self):
        """ Test destroy VM in zone wide shared nw when IPs are full and then try to deploy vm"""

        # Steps,
        #  1. Create zone wide shared network in SG enabled zone.
        #  2. Exhaust the free IPs in the network by deploying VM.
        #  3. Destroy VM when IPs are full and then again try to deploy VM

        # Validations,
        #  1. VM should be deployed as one IP gets available

        self.services["shared_network_sg"]["acltype"] = "domain"
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg", range=2)

        shared_network = Network.create(self.api_client,self.services["shared_network_sg"],
                                        networkofferingid=self.shared_network_offering_sg.id,zoneid=self.zone.id)
        self.cleanup_networks.append(shared_network)

        # Deploying 1 VM will exhaust the IP range because we are passing range as 2, and one of the IPs
        # already gets consumed by the virtual router of the shared network

        self.debug("Creating virtual machine shared network %s" % shared_network.id)
        vm_1 = VirtualMachine.create(self.api_client,self.services["virtual_machine"],
                              templateid=self.template.id,networkids=[shared_network.id,],
                              serviceofferingid=self.service_offering.id)
        try:
            self.debug("Trying to create virtual machine when all the IPs are consumed")
            vm = VirtualMachine.create(self.api_client,self.services["virtual_machine"],
                                       templateid=self.template.id,networkids=[shared_network.id,],
                                       serviceofferingid=self.service_offering.id)
            self.cleanup_vms.append(vm)
            self.fail("Vm creation succeded, should have failed")
        except Exception as e:
            self.debug("VM creation failed as expected with exception: %s" % e)

        # Now delete VM to free IP in shared network
        vm_1.delete(self.api_client)
        # Wait for VMs to expunge
        wait_for_cleanup(self.api_client, ["expunge.delay", "expunge.interval"])

        # As IP is free now, VM deployment in the shared network should be successful

        try:
            self.debug("Trying to create virtual machine when all the IPs are consumed")
            vm_2 = VirtualMachine.create(self.api_client,self.services["virtual_machine"],
                                       templateid=self.template.id,networkids=[shared_network.id,],
                                       serviceofferingid=self.service_offering.id)
            self.cleanup_vms.append(vm_2)
            self.debug("Deployed VM %s in the shared network %s" % (vm_2.id, shared_network.id))
        except Exception as e:
            self.fail("VM creation failed with exception: %s" % e)

        return

    @data("stopStart","reboot")
    @attr(tags = ["advancedsg"])
    def test_27_start_stop_vm(self, value):
        """ Test start and stop vm"""

        # Steps,
        #  1. Create a security group and authorize ingress rule (port 22-80) for this security group
        #  2. Create a user account and deploy VMs in this account and security group
        #  3. Try to access VM through SSH
        #  4. Start and stop/ Reboot VM
        #  5. Again try to access the VM through SSH

        # Validations,
        # 1. Both the times, SSH should be successful

        #Create user account
        user_account = Account.create(self.api_client,self.services["account"],domainid=self.domain.id)
        self.debug("Created user account : %s" % user_account.name)
        self.cleanup_accounts.append(user_account)

        ingress_rule_ids = []

        # Create custom security group
        self.services["security_group"]["name"] = "Custom_sec_grp_" + random_gen()
        custom_sec_grp = SecurityGroup.create(self.api_client,self.services["security_group"], account=user_account.name, domainid=self.domain.id)
        self.debug("Created security groups: %s" % custom_sec_grp.id)
        self.cleanup_secGrps.append(custom_sec_grp)

        # Authorize Security group to for allowing SSH to VM
        ingress_rule = custom_sec_grp.authorize(self.api_client,self.services["ingress_rule"])
        ingress_rule_ids.append(ingress_rule["ingressrule"][0].ruleid)
        self.debug("Authorized ingress rule for security group: %s" % custom_sec_grp.id)

        # Create virtual machine without passing any security group id
        vm = VirtualMachine.create(self.api_client,self.services["virtual_machine"],
                                   templateid=self.template.id,accountid=user_account.name,
                                   domainid=self.domain.id,serviceofferingid=self.service_offering.id,
                                   securitygroupids = [custom_sec_grp.id,])
        self.debug("Created VM : %s" % vm.id)
        self.cleanup_vms.append(vm)

        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % vm.nic[0].ipaddress)
            vm.get_ssh_client(ipaddress=vm.nic[0].ipaddress)

            self.debug("SSH to VM successful, proceeding for %s operation" % value)

            if value == "stopStart":
                self.setVmState(vm, "stopped")
                self.setVmState(vm, "running")
            elif value == "reboot":
                vm.reboot(self.api_client)

            self.debug("SSH into VM: %s" % vm.nic[0].ipaddress)
            vm.get_ssh_client(ipaddress=vm.nic[0].ipaddress)

        except Exception as e:
            self.fail("SSH Access failed for %s: %s, failed at line %s" % \
                      (vm.nic[0].ipaddress, e, sys.exc_info()[2].tb_lineno)
                      )
        finally:
            cmd = revokeSecurityGroupIngress.revokeSecurityGroupIngressCmd()
            for rule_id in ingress_rule_ids:
                cmd.id = rule_id
                self.api_client.revokeSecurityGroupIngress(cmd)

        return

    @data("recover","expunge")
    @attr(tags = ["advancedsg"])
    def test_28_destroy_recover_expunge_vm(self, value):
        """ Test start and stop vm"""

        # Steps,
        #  1. Create a security group and authorize ingress rule (port 22-80) for this security group
        #  2. Create a user account and deploy VMs in this account and security group
        #  3. Try to access VM through SSH
        #  4. Destroy and recover VM (Or Expunge)
        #  5. Again try to access the VM through SSH

        # Validations,
        # 1. In destroy/recover case Both the times, SSH should be successful
        # 2. In expunge case, SSH should not be suceessful after vm gets expunged

        #Create user account
        user_account = Account.create(self.api_client,self.services["account"],domainid=self.domain.id)
        self.debug("Created user account : %s" % user_account.name)
        self.cleanup_accounts.append(user_account)

        ingress_rule_ids = []

        # Create custom security group
        self.services["security_group"]["name"] = "Custom_sec_grp_" + random_gen()
        custom_sec_grp = SecurityGroup.create(self.api_client,self.services["security_group"], account=user_account.name, domainid=self.domain.id)
        self.debug("Created security groups: %s" % custom_sec_grp.id)
        self.cleanup_secGrps.append(custom_sec_grp)

        # Authorize Security group to for allowing SSH to VM
        ingress_rule = custom_sec_grp.authorize(self.api_client,self.services["ingress_rule"])
        ingress_rule_ids.append(ingress_rule["ingressrule"][0].ruleid)
        self.debug("Authorized ingress rule for security group: %s" % custom_sec_grp.id)

        # Create virtual machine without passing any security group id
        vm = VirtualMachine.create(self.api_client,self.services["virtual_machine"],
                                   templateid=self.template.id,accountid=user_account.name,
                                   domainid=self.domain.id,serviceofferingid=self.service_offering.id,
                                   securitygroupids = [custom_sec_grp.id,])
        self.debug("Created VM : %s" % vm.id)

        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % vm.nic[0].ipaddress)
            vm.get_ssh_client(ipaddress=vm.nic[0].ipaddress)
            self.debug("SSH to VM successful, proceeding for %s operation" % value)
            vm.delete(self.api_client, expunge=False)
            if value == "recover":
                vm.recover(self.api_client)
                vm.start(self.api_client)
                retriesCount = 5
                while True:
                    vm_list = VirtualMachine.list(self.api_client, id=vm.id)
                    self.assertEqual(validateList(vm_list)[0], PASS , "vm list validation failed, vm list is %s" % vm_list)
                    if str(vm_list[0].state).lower() == "running":
                        break
                    if retriesCount == 0:
                        self.fail("Failed to start vm: %s" % vm.id)
                    retriesCount -= 1
                    time.sleep(10)
                self.debug("SSH into VM: %s" % vm.nic[0].ipaddress)
                vm.get_ssh_client(ipaddress=vm.nic[0].ipaddress)
                self.debug("SSH successful")
                self.cleanup_vms.append(vm)
            elif value == "expunge":
                #wait till vm gets expunged
                wait_for_cleanup(self.api_client, ["expunge.delay", "expunge.interval"])
                try:
                    vm_list = VirtualMachine.list(self.api_client, id=vm.id)
                    self.fail("vm listing should fail, instead got vm list: %s" % vm_list)
                except Exception as e:
                    self.debug("Vm listing failed as expected with exception: %s" % e)

                try:
                    self.debug("SSH into VM: %s, this should fail" % vm.nic[0].ipaddress)
                    vm.get_ssh_client(ipaddress=vm.nic[0].ipaddress)
                    self.fail("SSH should have failed, instead it succeeded")
                except Exception as e:
                    self.debug("SSH failed as expected with exception: %s" % e)
        except Exception as e:
            self.fail("SSH Access failed for %s: %s, failed at line %s" % \
                      (vm.nic[0].ipaddress, e, sys.exc_info()[2].tb_lineno)
                      )
        finally:
            cmd = revokeSecurityGroupIngress.revokeSecurityGroupIngressCmd()
            for rule_id in ingress_rule_ids:
                cmd.id = rule_id
                self.api_client.revokeSecurityGroupIngress(cmd)

        return

    @attr(tags = ["advancedsg"])
    def test_31_Deploy_VM_multiple_shared_networks_sg(self):
        """ Test deploy VM in multiple SG enabled shared networks"""

        # Steps,
        #  1. Create multiple SG enabled shared networks
        #  3. Try to deploy VM in all of these networks

        # Validations,
        #  1. VM deployment should fail saying "Only support one network per VM if security group enabled"

        self.services["shared_network_sg"]["acltype"] = "domain"
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

	    #create network using the shared network offering created
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg")

        shared_network_1 = Network.create(self.api_client,self.services["shared_network_sg"],
                                          networkofferingid=self.shared_network_offering_sg.id,zoneid=self.zone.id)
        self.cleanup_networks.append(shared_network_1)
        self.debug("Created shared network: %s" % shared_network_1.id)

        self.services["shared_network_sg"]["vlan"] = get_free_vlan(self.api_client, self.zone.id)[1]
        self.setSharedNetworkParams("shared_network_sg")

        shared_network_2 = Network.create(self.api_client,self.services["shared_network_sg"],
                                          networkofferingid=self.shared_network_offering_sg.id,zoneid=self.zone.id)
        self.cleanup_networks.append(shared_network_2)
        self.debug("Created shared network: %s" % shared_network_2.id)

        try:
            self.debug("Creating virtual machine in shared networks %s and %s, this should fail" %
                      (shared_network_1.id, shared_network_2.id))
            vm = VirtualMachine.create(self.api_client,self.services["virtual_machine"],
                                       templateid=self.template.id, networkids=[shared_network_1.id, shared_network_2.id],
                                       serviceofferingid=self.service_offering.id)
            self.cleanup_vms.append(vm)
            self.fail("Vm deployment should have failed, instead deployed vm %s" % vm.id)
        except Exception as e:
            self.debug("VM deployment failed as expected with exception: %s" % e)

        return

    @unittest.skip("Testing pending on multihost setup")
    @data("account","domain","zone")
    @attr(tags = ["advancedsg"])
    def test_33_VM_Migrate_SharedNwSG(self, value):
        """ Test migration of VM deployed in Account specific shared network"""

        # Steps,
        #  1. create a user account
        #  2. Create one shared Network (scope=Account/domain/zone)
        #  3. Deploy one VM in above shared network
        #  4. Migrate VM to another host

        # Validations,
        #  1. VM migration should be successful

        #Create admin account
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("vm migrate is not supported in %s" % self.hypervisor)

        hosts = Host.list(self.api_client, zoneid=self.zone.id)
        self.assertEqual(validateList(hosts)[0], PASS, "hosts list validation failed, list is %s" % hosts)
        if len(hosts) < 2:
            self.skipTest("This test requires at least two hosts present in the zone")
        domain = self.domain
        account = None
        self.services["shared_network_sg"]["acltype"] = "domain"
        if value == "domain":
            domain = Domain.create(self.api_client, services=self.services["domain"],
                                           parentdomainid=self.domain.id)
            self.cleanup_domains.append(domain)
        elif value == "account":
            account = Account.create(self.api_client,self.services["account"],admin=True,
                                     domainid=self.domain.id)
            self.cleanup_accounts.append(account)
            self.services["shared_network_sg"]["acltype"] = "account"

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)
	    #create network using the shared network offering created
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg")

        shared_network_sg = Network.create(self.api_client, self.services["shared_network_sg"],
                                                              accountid=account.name if account else None,
                                                              domainid=domain.id,
                                                              networkofferingid=self.shared_network_offering_sg.id,
                                                              zoneid=self.zone.id)

        if value == "domain" or value == "zone":
            self.cleanup_networks.append(shared_network_sg)

        self.debug("Created %s wide shared network %s" % (value,shared_network_sg.id))

        virtual_machine = VirtualMachine.create(self.api_client,self.services["virtual_machine"],
                                                accountid=account.name if account else None,
                                                domainid=domain.id,
                                                networkids=[shared_network_sg.id],
						serviceofferingid=self.service_offering.id
                                                )
        self.cleanup_vms.append(virtual_machine)
        hosts_to_migrate = [host for host in hosts if host.id != virtual_machine.hostid]
        self.assertTrue(len(hosts_to_migrate) > 1, "At least one suitable host should be present to migrate VM")

        try:
            self.debug("trying to migrate virtual machine from host %s to host %s" % (virtual_machine.hostid, hosts_to_migrate[0].id))
            virtual_machine.migrate(self.api_client, hosts_to_migrate[0].id)
        except Exception as e:
            self.fail("VM migration failed with exception %s" % e)

        vm_list = list_virtual_machines(self.api_client, id=virtual_machine.id)
        self.assertEqual(validateList(vm_list)[0], PASS, "vm list validation failed, vm list is %s" % vm_list)
        self.assertEqual(vm_list[0].hostid, hosts_to_migrate[0].id, "VM host id does not reflect the migration")
        return

    @attr(tags=["advancedsg"], required_hardware="false")
    def test_34_DeployVM_in_SecondSGNetwork(self):
        """
        @Desc: VM Cannot deploy to second network in advanced SG network
        @step1:Create shared SG network1
        @step2: Consume all ip addresses in network1
        @step3: Create shared SG network2
        @step4: Deploy vm without specifying the network id
        @step5: Verify that vm deployment should pick network2 and should not fail by picking network1
        """
        #Deploy data center with custom data
        status = self.dump_config_deploy_DC()
        if status == 1:
            self.fail("Deploy DataCenter failed.")
        zone_list = Zone.list(
            self.api_client,
            name=self.services["test_34_DeployVM_in_SecondSGNetwork"]["zone"]
        )
        status = validateList(zone_list)
        self.assertEqual(status[0], PASS, "Failed to list the zones")
        count = 0
        """
        In simulator environment default guest os template should be in ready state immediately after the ssvm is up.
        In worst case test would wait for 100sec for the template to get ready else it would fail.
        """
        while (count < 10):
            time.sleep(10)
            template = get_template(
                self.api_client,
                zone_list[0].id
                )
            if template != FAILED and str(template.isready).lower() == 'true':
                break
            else:
                count=count+1
                if count == 10:
                    self.fail("Template is not in ready state even after 100sec. something wrong with the SSVM")
        self.debug("Creating virtual machine in default shared network to consume all IPs")
        vm_1 = VirtualMachine.create(
            self.api_client,
            self.services["virtual_machine"],
            templateid=template.id,
            zoneid=zone_list[0].id,
            serviceofferingid=self.service_offering.id
        )
        self.assertIsNotNone(vm_1,"Failed to deploy vm in default shared network")
        self.cleanup_vms.append(vm_1)
        #verify that all the IPs are consumed in the default shared network
        cmd = listCapacity.listCapacityCmd()
        cmd.type=8
        cmd.zoneid = zone_list[0].id
        cmd.fetchlatest='true'
        count = 0
        """
        Created zone with only 4 guest IP addresses so limiting the loop count to 4
        """
        while count < 5:
            listCapacityRes = self.api_client.listCapacity(cmd)
            self.assertEqual(validateList(listCapacityRes)[0],PASS,"listCapacity returned invalid list")
            percentused = listCapacityRes[0].percentused
            if percentused == '100':
                break
            self.debug("Creating virtual machine in default shared network to consume all IPs")
            vm = VirtualMachine.create(
                self.api_client,
                self.services["virtual_machine"],
                templateid=template.id,
                zoneid=zone_list[0].id,
                serviceofferingid=self.service_offering.id
            )
            self.assertIsNotNone(vm,"Failed to deploy vm in default shared network")
            self.cleanup_vms.append(vm)
            count = count+1
            if count == 5:
                self.fail("IPs are not getting consumed. Please check the setup")
        #Create another SG enabled shared network after consuming all IPs
        self.services["shared_network_sg"]["acltype"] = "domain"
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id
        physical_network, vlan = get_free_vlan(self.api_client, zone_list[0].id)
        #create network using the shared network offering created
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id
        nwIPs = 3
        self.setSharedNetworkParams("shared_network_sg", range=nwIPs)
        self.debug("Creating shared sg network1 with vlan %s" % vlan)
        shared_network = Network.create(
            self.api_client,
            self.services["shared_network_sg"],
            networkofferingid=self.shared_network_offering_sg.id,
            zoneid=zone_list[0].id
        )
        self.assertIsNotNone(shared_network,"shared SG network1 creation failed")
        self.cleanup_networks.append(shared_network)
        # Deploying 1 VM will exhaust the IP range because we are passing range as 2, and one of the IPs
        # already gets consumed by the virtual router of the shared network
        self.debug("Deploying vm2 without passing network id after consuming all IPs from default shared nw")
        try:
            vm_2 = VirtualMachine.create(
                self.api_client,
                self.services["virtual_machine"],
                templateid=template.id,
                zoneid=zone_list[0].id,
                serviceofferingid=self.service_offering.id
            )
            vm2_res = VirtualMachine.list(
                self.api_client,
                id=vm_2.id
            )
            self.assertEqual(validateList(vm2_res)[0],PASS,"Failed to list vms in new network")
            vm_ip = vm2_res[0].nic[0].ipaddress
            ips_in_new_network = []
            ip_gen = iter_iprange(
                self.services["shared_network_sg"]["startip"],
                self.services["shared_network_sg"]["endip"]
            )
            #construct ip list using start and end ips in the network
            for i in range(0,nwIPs):
                ips_in_new_network.append(str(next(ip_gen)))
            if vm_ip not in ips_in_new_network:
                self.fail("vm did not get the ip from new SG enabled shared network")
            self.cleanup_vms.append(vm_2)
        except Exception as e:
            self.fail("Failed to deploy vm with two advanced sg networks %s" % e)
        return

class TestSecurityGroups_BasicSanity(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestSecurityGroups_BasicSanity, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(cls.api_client,cls.zone.id,cls.services["ostype"])

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(cls.api_client,cls.services["service_offering"])

        cls.services["shared_network_offering_sg"]["specifyVlan"] = "True"
        cls.services["shared_network_offering_sg"]["specifyIpRanges"] = "True"
        #Create Network Offering
        cls.shared_network_offering_sg = NetworkOffering.create(cls.api_client,cls.services["shared_network_offering_sg"],conservemode=False)

        #Update network offering state from disabled to enabled.
        NetworkOffering.update(cls.shared_network_offering_sg,cls.api_client,state="enabled")

        physical_network, vlan = get_free_vlan(cls.api_client, cls.zone.id)

	    #create network using the shared network offering created
        cls.services["shared_network_sg"]["vlan"] = vlan
        cls.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id
        cls.services["shared_network_sg"]["acltype"] = "domain"
        cls.services["shared_network_sg"]["networkofferingid"] = cls.shared_network_offering_sg.id

        cls.setSharedNetworkParams("shared_network_sg")

        cls.shared_network = Network.create(cls.api_client,cls.services["shared_network_sg"],
                                        networkofferingid=cls.shared_network_offering_sg.id,zoneid=cls.zone.id)

        cls._cleanup = [
                        cls.service_offering,
                        cls.shared_network,
                        cls.shared_network_offering_sg,
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Update network offering state from enabled to disabled.
            cls.shared_network_offering_sg.update(cls.api_client,state="disabled")
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.api_client = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.cleanup_networks = []
        self.cleanup_accounts = []
        self.cleanup_domains = []
        self.cleanup_projects = []
        self.cleanup_vms = []
        self.cleanup_secGrps = []
        return

    def tearDown(self):
        exceptions = []
        try:
            #Clean up, terminate the created network offerings
            cleanup_resources(self.api_client, self.cleanup)
        except Exception as e:
            self.debug("Warning: Exception during cleanup : %s" % e)
            exceptions.append(e)

        #below components is not a part of cleanup because to mandate the order and to cleanup network
        try:
            for vm in self.cleanup_vms:
                vm.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during virtual machines cleanup : %s" % e)
            exceptions.append(e)

        # Wait for VMs to expunge
        wait_for_cleanup(self.api_client, ["expunge.delay", "expunge.interval"])

        try:
            for sec_grp in self.cleanup_secGrps:
                sec_grp.delete(self.api_client)
        except Exception as e:
            self.debug("Warning : Exception during security groups cleanup: %s" % e)
            exceptions.append(e)

        try:
            for project in self.cleanup_projects:
                project.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during project cleanup : %s" % e)
            exceptions.append(e)

        try:
            for account in self.cleanup_accounts:
                account.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during account cleanup : %s" % e)
            exceptions.append(e)

        #Wait till all resources created are cleaned up completely and then attempt to delete Network
        time.sleep(self.services["sleep"])

        try:
            for network in self.cleanup_networks:
                network.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during network cleanup : %s" % e)
            exceptions.append(e)

        try:
            for domain in self.cleanup_domains:
                domain.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during domain cleanup : %s" % e)
            exceptions.append(e)

        if len(exceptions) > 0:
            self.fail("There were exceptions during cleanup: %s" % exceptions)

        return

    @classmethod
    def setSharedNetworkParams(cls, network, range=20):

        # @range: range decides the endip. Pass the range as "x" if you want the difference between the startip
        # and endip as "x"
        # Set the subnet number of shared networks randomly prior to execution
        # of each test case to avoid overlapping of ip addresses
        shared_network_subnet_number = random.randrange(1,254)

        cls.services[network]["gateway"] = "172.16."+str(shared_network_subnet_number)+".1"
        cls.services[network]["startip"] = "172.16."+str(shared_network_subnet_number)+".2"
        cls.services[network]["endip"] = "172.16."+str(shared_network_subnet_number)+"."+str(range+1)
        cls.services[network]["netmask"] = "255.255.255.0"

        return

    @attr(tags = ["advancedsg"])
    def test_21_DeployVM_WithoutSG(self):
        """ Test deploy VM without passing any security group id"""

        # Steps,
        #  1. List security groups and authorize ingress rule for the default security group
        #  2. Create custom security group and authorize ingress rule for this security group
        #  3. Deploy VM without passing any security group
        #  4. SSH to VM and try pinging outside world from VM

        # Validations,
        # 1. VM deployment should be successful
        # 2. VM should be deployed in default security group and not in custom security group
        # 3. VM should be reached through SSH using any IP and ping to outside world should be successful

        ingress_rule_ids = []

        self.debug("Listing security groups")
        securitygroups = SecurityGroup.list(self.api_client)

        self.assertEqual(validateList(securitygroups)[0], PASS, "securitygroups list validation\
                failed, securitygroups list is %s" % securitygroups)

        defaultSecurityGroup = securitygroups[0]

        self.debug("Default security group: %s" % defaultSecurityGroup.id)

        # Authorize ingress rule for the default security group
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.securitygroupid = defaultSecurityGroup.id
        cmd.protocol = 'TCP'
        cmd.startport = 22
        cmd.endport = 80
        cmd.cidrlist = '0.0.0.0/0'
        ingress_rule_1 = self.api_client.authorizeSecurityGroupIngress(cmd)
        ingress_rule_ids.append(ingress_rule_1.ingressrule[0].ruleid)

        # Create custom security group
        self.services["security_group"]["name"] = "Custom_sec_grp_" + random_gen()
        custom_sec_grp = SecurityGroup.create(self.api_client,self.services["security_group"])
        self.debug("Created security groups: %s" % custom_sec_grp.id)
        self.cleanup_secGrps.append(custom_sec_grp)

        # Authorize Security group to for allowing SSH to VM
        ingress_rule_2 = custom_sec_grp.authorize(self.api_client,self.services["ingress_rule"])
        ingress_rule_ids.append(ingress_rule_2["ingressrule"][0].ruleid)
        self.debug("Authorized ingress rule for security group: %s" % custom_sec_grp.id)

        self.debug(ingress_rule_ids)

        # Create virtual machine without passing any security group id
        vm = VirtualMachine.create(self.api_client,self.services["virtual_machine"],
                                   templateid=self.template.id,
                                   serviceofferingid=self.service_offering.id)
        self.debug("Created VM : %s" % vm.id)
        self.cleanup_vms.append(vm)

        self.debug("listing virtual machines")

        vm_list  = list_virtual_machines(self.api_client, id=vm.id)

        self.assertEqual(validateList(vm_list)[0], PASS, "vm list validation failed, list is %s" % vm_list)

        self.assertEqual(len(vm_list[0].securitygroup), 1, "There should be exactly one security group associated \
                with the vm, vm security group list is : %s" % vm_list[0].securitygroup)
        self.assertEqual(defaultSecurityGroup.id, vm_list[0].securitygroup[0].id, "Vm should be deployed in default security group %s \
                instead it is deployed in security group %s" % (defaultSecurityGroup.id, vm_list[0].securitygroup[0].id))

        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % vm.nic[0].ipaddress)
            ssh = vm.get_ssh_client(ipaddress=vm.nic[0].ipaddress)

            # Ping to outsite world
            res = ssh.execute("ping -c 1 www.google.com")
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (vm.ssh_ip, e)
                      )
        finally:
            cmd = revokeSecurityGroupIngress.revokeSecurityGroupIngressCmd()
            self.debug("ingress_rules: %s" % ingress_rule_ids)
            for rule_id in ingress_rule_ids:
                cmd.id = rule_id
                self.api_client.revokeSecurityGroupIngress(cmd)
        result = str(res)
        self.assertEqual(
                         result.count("1 received"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )
        return

    @attr(tags = ["advancedsg"])
    def test_22_DeployVM_With_Custom_SG(self):
        """ Test deploy VM by passing custom security group id"""

        # Steps,
        #  1. List security groups and authorize ingress rule for the default security group
        #  2. Create custom security group and authorize ingress rule for this security group
        #  3. Deploy VM by passing only custom security group id
        #  4. SSH to VM and try pinging outside world from VM

        # Validations,
        # 1. VM deployment should be successful
        # 2. VM should be deployed in custom security group and not in any other security group
        # 3. VM should be reached through SSH using any IP and ping to outside world should be successful

        ingress_rule_ids = []

        self.debug("Listing security groups")
        securitygroups = SecurityGroup.list(self.api_client)

        self.assertEqual(validateList(securitygroups)[0], PASS, "securitygroups list validation\
                failed, securitygroups list is %s" % securitygroups)

        defaultSecurityGroup = securitygroups[0]

        self.debug("Default security group: %s" % defaultSecurityGroup.id)

        # Authorize ingress rule for the default security group
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.securitygroupid = defaultSecurityGroup.id
        cmd.protocol = 'TCP'
        cmd.startport = 22
        cmd.endport = 80
        cmd.cidrlist = '0.0.0.0/0'
        ingress_rule_1 = self.api_client.authorizeSecurityGroupIngress(cmd)
        ingress_rule_ids.append(ingress_rule_1.ingressrule[0].ruleid)

        # Create custom security group
        self.services["security_group"]["name"] = "Custom_sec_grp_" + random_gen()
        custom_sec_grp = SecurityGroup.create(self.api_client,self.services["security_group"])
        self.debug("Created security groups: %s" % custom_sec_grp.id)
        self.cleanup_secGrps.append(custom_sec_grp)

        # Authorize Security group to for allowing SSH to VM
        ingress_rule_2 = custom_sec_grp.authorize(self.api_client,self.services["ingress_rule"])
        ingress_rule_ids.append(ingress_rule_2["ingressrule"][0].ruleid)
        self.debug("Authorized ingress rule for security group: %s" % custom_sec_grp.id)

        # Create virtual machine without passing any security group id
        vm = VirtualMachine.create(self.api_client,self.services["virtual_machine"],
                                   templateid=self.template.id,
                                   serviceofferingid=self.service_offering.id,
                                   securitygroupids = [custom_sec_grp.id,])
        self.debug("Created VM : %s" % vm.id)
        self.cleanup_vms.append(vm)

        self.debug("listing virtual machines")

        vm_list  = list_virtual_machines(self.api_client, id=vm.id)

        self.assertEqual(validateList(vm_list)[0], PASS, "vm list validation failed, list is %s" % vm_list)

        self.assertEqual(len(vm_list[0].securitygroup), 1, "There should be exactly one security group associated \
                with the vm, vm security group list is : %s" % vm_list[0].securitygroup)
        self.assertEqual(custom_sec_grp.id, vm_list[0].securitygroup[0].id, "Vm should be deployed in custom security group %s \
                instead it is deployed in security group %s" % (custom_sec_grp.id, vm_list[0].securitygroup[0].id))

        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % vm.nic[0].ipaddress)
            ssh = vm.get_ssh_client(ipaddress=vm.nic[0].ipaddress)

            # Ping to outsite world
            res = ssh.execute("ping -c 1 www.google.com")
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (vm.ssh_ip, e)
                      )
        finally:
            cmd = revokeSecurityGroupIngress.revokeSecurityGroupIngressCmd()
            for rule_id in ingress_rule_ids:
                cmd.id = rule_id
                self.api_client.revokeSecurityGroupIngress(cmd)

        result = str(res)
        self.assertEqual(
                         result.count("1 received"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )

        return

    @attr(tags = ["advancedsg"])
    def test_23_DeployVM_MultipleSG(self):
        """ Test deploy VM in multiple security groups"""

        # Steps,
        #  1. Create multiple security groups
        #  2. Authorize ingress rule for all these security groups
        #  3. Deploy VM with all the security groups
        #  4. SSH to VM

        # Validations,
        # 1. VM deployment should be successful
        # 2. VM should list all the security groups
        # 3. VM should be reached through SSH using any IP in the mentioned CIDRs of the ingress rules

        ingress_rule_ids = []

        self.services["security_group"]["name"] = "Custom_sec_grp_" + random_gen()
        sec_grp_1 = SecurityGroup.create(self.api_client,self.services["security_group"])
        self.debug("Created security groups: %s" % sec_grp_1.id)
        self.cleanup_secGrps.append(sec_grp_1)

        self.services["security_group"]["name"] = "Custom_sec_grp_" + random_gen()
        sec_grp_2 = SecurityGroup.create(self.api_client,self.services["security_group"])
        self.debug("Created security groups: %s" % sec_grp_2.id)
        self.cleanup_secGrps.append(sec_grp_2)

        self.services["security_group"]["name"] = "Custom_sec_grp_" + random_gen()
        sec_grp_3 = SecurityGroup.create(self.api_client,self.services["security_group"])
        self.debug("Created security groups: %s" % sec_grp_3.id)
        self.cleanup_secGrps.append(sec_grp_3)

        # Authorize Security group to for allowing SSH to VM
        ingress_rule_1 = sec_grp_1.authorize(self.api_client,self.services["ingress_rule"])
        ingress_rule_ids.append(ingress_rule_1["ingressrule"][0].ruleid)
        self.debug("Authorized ingress rule for security group: %s" % sec_grp_1.id)

        ingress_rule_2 = sec_grp_2.authorize(self.api_client,self.services["ingress_rule"])
        ingress_rule_ids.append(ingress_rule_2["ingressrule"][0].ruleid)
        self.debug("Authorized ingress rule for security group: %s" % sec_grp_2.id)

        ingress_rule_3 = sec_grp_3.authorize(self.api_client,self.services["ingress_rule"])
        ingress_rule_ids.append(ingress_rule_3["ingressrule"][0].ruleid)
        self.debug("Authorized ingress rule for security group: %s" % sec_grp_3.id)

        vm = VirtualMachine.create(self.api_client,self.services["virtual_machine"],
                                   templateid=self.template.id,
                                   securitygroupids=[sec_grp_1.id, sec_grp_2.id, sec_grp_3.id,],
                                   serviceofferingid=self.service_offering.id)
        self.debug("Created VM : %s" % vm.id)
        self.cleanup_vms.append(vm)

        self.debug("listing virtual machines")

        vm_list  = list_virtual_machines(self.api_client, id=vm.id)

        self.assertEqual(validateList(vm_list)[0], PASS, "vm list validation failed, list is %s" % vm_list)

        sec_grp_list = [sec_grp.id for sec_grp in vm_list[0].securitygroup]

        self.assertTrue(sec_grp_1.id in sec_grp_list, "%s not present in security groups of vm: %s" %
                (sec_grp_1.id, sec_grp_list))
        self.assertTrue(sec_grp_2.id in sec_grp_list, "%s not present in security groups of vm: %s" %
                (sec_grp_2.id, sec_grp_list))
        self.assertTrue(sec_grp_3.id in sec_grp_list, "%s not present in security groups of vm: %s" %
                (sec_grp_3.id, sec_grp_list))

        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % vm.ssh_ip)
            ssh = vm.get_ssh_client(ipaddress=vm.nic[0].ipaddress)

            # Ping to outsite world
            res = ssh.execute("ping -c 1 www.google.com")
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (vm.ssh_ip, e)
                      )
        finally:
            cmd = revokeSecurityGroupIngress.revokeSecurityGroupIngressCmd()
            for rule_id in ingress_rule_ids:
                cmd.id = rule_id
                self.api_client.revokeSecurityGroupIngress(cmd)

        result = str(res)
        self.assertEqual(
                         result.count("1 received"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )
        return

    @attr(tags = ["advancedsg"])
    def test_32_delete_default_security_group(self):
        """ Test Delete the default security group when No VMs are deployed"""

        # Steps,
        #  1. create an account
        #  2. List the security groups belonging to the account
        #  3. Delete the default security group
        # Validations,
        #  1. Default security group of the account should not get deleted

        #Create admin account
        account = Account.create(self.api_client, self.services["account"], admin=True,
                                            domainid=self.domain.id)

        self.cleanup_accounts.append(account)

        self.debug("Admin type account created: %s" % account.name)

        securitygroups = SecurityGroup.list(self.api_client, account=account.name, domainid=self.domain.id)
        self.assertEqual(validateList(securitygroups)[0], PASS, "security groups list validation failed, list is %s" % securitygroups)

        defaultSecGroup = securitygroups[0]
        cmd = deleteSecurityGroup.deleteSecurityGroupCmd()
        cmd.id = defaultSecGroup.id

        try:
            self.debug("Deleting default security group %s in account %s" % (defaultSecGroup.id, account.id))
            self.api_client.deleteSecurityGroup(cmd)
            self.fail("Default security group of the account got deleted")
        except Exception as e:
            self.debug("Deleting the default security group failed as expected with exception: %s" % e)
        return

@ddt
class TestSharedNetworkOperations(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestSharedNetworkOperations, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(cls.api_client,cls.zone.id,cls.services["ostype"])

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(cls.api_client,cls.services["service_offering"])

        cls.services["shared_network_offering_sg"]["specifyVlan"] = "True"
        cls.services["shared_network_offering_sg"]["specifyIpRanges"] = "True"
        #Create Network Offering
        cls.shared_network_offering_sg = NetworkOffering.create(cls.api_client,cls.services["shared_network_offering_sg"],conservemode=False)

        #Update network offering state from disabled to enabled.
        NetworkOffering.update(cls.shared_network_offering_sg,cls.api_client,state="enabled")

        cls._cleanup = [
                        cls.service_offering,
                        cls.shared_network_offering_sg
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Update network offering state from enabled to disabled.
            cls.shared_network_offering_sg.update(cls.api_client,state="disabled")
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.api_client = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.cleanup_networks = []
        self.cleanup_accounts = []
        self.cleanup_domains = []
        self.cleanup_projects = []
        self.cleanup_vms = []
        self.cleanup_secGrps = []
        return

    def tearDown(self):
        exceptions = []
        try:
            #Clean up, terminate the created network offerings
            cleanup_resources(self.api_client, self.cleanup)
        except Exception as e:
            self.debug("Warning: Exception during cleanup : %s" % e)
            exceptions.append(e)

        #below components is not a part of cleanup because to mandate the order and to cleanup network
        try:
            for vm in self.cleanup_vms:
                vm.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during virtual machines cleanup : %s" % e)
            exceptions.append(e)

        # Wait for VMs to expunge
        wait_for_cleanup(self.api_client, ["expunge.delay", "expunge.interval"])

        try:
            for sec_grp in self.cleanup_secGrps:
                sec_grp.delete(self.api_client)
        except Exception as e:
            self.debug("Warning : Exception during security groups cleanup: %s" % e)
            exceptions.append(e)

        try:
            for project in self.cleanup_projects:
                project.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during project cleanup : %s" % e)
            exceptions.append(e)

        try:
            for account in self.cleanup_accounts:
                account.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during account cleanup : %s" % e)
            exceptions.append(e)

        #Wait till all resources created are cleaned up completely and then attempt to delete Network
        time.sleep(self.services["sleep"])

        try:
            for network in self.cleanup_networks:
                network.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during network cleanup : %s" % e)
            exceptions.append(e)

        try:
            for domain in self.cleanup_domains:
                domain.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during domain cleanup : %s" % e)
            exceptions.append(e)

        if len(exceptions) > 0:
            self.fail("There were exceptions during cleanup: %s" % exceptions)

        return

    def setSharedNetworkParams(self, network, range=20):

        # @range: range decides the endip. Pass the range as "x" if you want the difference between the startip
        # and endip as "x"
        # Set the subnet number of shared networks randomly prior to execution
        # of each test case to avoid overlapping of ip addresses
        shared_network_subnet_number = random.randrange(1,254)

        self.services[network]["gateway"] = "172.16."+str(shared_network_subnet_number)+".1"
        self.services[network]["startip"] = "172.16."+str(shared_network_subnet_number)+".2"
        self.services[network]["endip"] = "172.16."+str(shared_network_subnet_number)+"."+str(range+1)
        self.services[network]["netmask"] = "255.255.255.0"

        return

    @data("account","domain","zone")
    @attr(tags = ["advancedsg"])
    def test_34_restart_shared_network_sg(self, value):
        """ Test restart account/domain/zone wide shared network"""

        # Steps,
        #  1. Create account/domain/zone wide shared Network
        #  2. Restart shared network

        # Validations,
        #  1. Network restart should be successful

        domain = self.domain
        account = None
        self.services["shared_network_sg"]["acltype"] = "domain"
        if value == "domain":
            domain = Domain.create(self.api_client, services=self.services["domain"],
                                           parentdomainid=self.domain.id)
            self.cleanup_domains.append(domain)
        elif value == "account":
            account = Account.create(self.api_client,self.services["account"],admin=True,
                                     domainid=self.domain.id)
            self.cleanup_accounts.append(account)
            self.services["shared_network_sg"]["acltype"] = "account"

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)
	    #create network using the shared network offering created
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg")

        shared_network_sg = Network.create(self.api_client, self.services["shared_network_sg"],
                                                              accountid=account.name if account else None,
                                                              domainid=domain.id,
                                                              networkofferingid=self.shared_network_offering_sg.id,
                                                              zoneid=self.zone.id)
        if value == "domain" or value == "zone":
            self.cleanup_networks.append(shared_network_sg)

        self.debug("Created %s wide shared network %s" % (value,shared_network_sg.id))

        try:
            self.debug("Restarting shared network: %s" % shared_network_sg)
            shared_network_sg.restart(self.api_client)
        except Exception as e:
            self.fail("Exception while restarting the shared network: %s" % e)
        return

    @unittest.skip("Testing pending on multihost setup")
    @data("account","domain","zone")
    @attr(tags = ["advancedsg"])
    def test_35_Enable_Host_Maintenance(self, value):
        """ Test security group rules of VM after putting host in maintenance mode"""

        # Steps,
        #  1. Deploy vm in account/domain/zone wide shared network
        #  2. Put the host of VM in maintenance mode
        #  3. Verify the security group associated with the VM
        #  4. Cancel the maintenance mode of the host
        #  5. Again verify the security group of the VM

        # Validations,
        #  1. Security group should remain the same throughout all the operations

        #Create admin account

        hosts = Host.list(self.api_client, zoneid=self.zone.id)
        self.assertEqual(validateList(hosts)[0], PASS, "hosts list validation failed, list is %s" % hosts)
        if len(hosts) < 2:
            self.skipTest("This test requires at least two hosts present in the zone")
        domain = self.domain
        account = None
        self.services["shared_network_sg"]["acltype"] = "domain"
        if value == "domain":
            domain = Domain.create(self.api_client, services=self.services["domain"],
                                           parentdomainid=self.domain.id)
            self.cleanup_domains.append(domain)
        elif value == "account":
            account = Account.create(self.api_client,self.services["account"],admin=True,
                                     domainid=self.domain.id)
            self.cleanup_accounts.append(account)
            self.services["shared_network_sg"]["acltype"] = "account"

        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)
	    #create network using the shared network offering created
        self.services["shared_network_sg"]["vlan"] = vlan
        self.services["shared_network_sg"]["networkofferingid"] = self.shared_network_offering_sg.id
        self.services["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        self.setSharedNetworkParams("shared_network_sg")

        shared_network_sg = Network.create(self.api_client, self.services["shared_network_sg"],
                                                              accountid=account.name if account else None,
                                                              domainid=domain.id,
                                                              networkofferingid=self.shared_network_offering_sg.id,
                                                              zoneid=self.zone.id)

        if value == "domain" or value == "zone":
            self.cleanup_networks.append(shared_network_sg)

        self.debug("Created %s wide shared network %s" % (value,shared_network_sg.id))

        virtual_machine = VirtualMachine.create(self.api_client,self.services["virtual_machine"],
                                                accountid=account.name if account else None,
                                                domainid=domain.id,
                                                networkids=[shared_network_sg.id],
						serviceofferingid=self.service_offering.id
                                                )
        self.cleanup_vms.append(virtual_machine)
        hostid = virtual_machine.hostid

        securitygroupid = virtual_machine.securitygroup[0].id

        # Put host in maintenance mode
        Host.enableMaintenance(self.api_client, id=hostid)

        vm_list = list_virtual_machines(self.api_client, id=virtual_machine.id)
        self.assertEqual(validateList(vm_list)[0], PASS, "vm list validation failed, vm list is: %s" % vm_list)
        self.assertEqual(vm_list[0].securitygroup[0].id, securitygroupid, "Security group id should remain same, before\
        it was %s and after putting host in maintenance mode, it is %s" % (securitygroupid, vm_list[0].securitygroup[0].id))

        # Cancel host maintenance
        Host.cancelMaintenance(self.api_client, id=hostid)
        vm_list = list_virtual_machines(self.api_client, id=virtual_machine.id)
        self.assertEqual(validateList(vm_list)[0], PASS, "vm list validation failed, vm list is: %s" % vm_list)
        self.assertEqual(vm_list[0].securitygroup[0].id, securitygroupid, "Security group id should remain same, before\
        it was %s and after putting host in maintenance mode, it is %s" % (securitygroupid, vm_list[0].securitygroup[0].id))

        return

@ddt
class TestAccountBasedIngressRules(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestAccountBasedIngressRules, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(cls.api_client,cls.zone.id,cls.services["ostype"])

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(cls.api_client,cls.services["service_offering"])

        cls.services["shared_network_offering_sg"]["specifyVlan"] = "True"
        cls.services["shared_network_offering_sg"]["specifyIpRanges"] = "True"
        #Create Network Offering
        cls.shared_network_offering_sg = NetworkOffering.create(cls.api_client,cls.services["shared_network_offering_sg"],conservemode=False)

        #Update network offering state from disabled to enabled.
        NetworkOffering.update(cls.shared_network_offering_sg,cls.api_client,state="enabled")

        cls._cleanup = [
                        cls.service_offering,
                        cls.shared_network_offering_sg
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Update network offering state from enabled to disabled.
            cls.shared_network_offering_sg.update(cls.api_client,state="disabled")
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.api_client = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.cleanup_networks = []
        self.cleanup_accounts = []
        self.cleanup_domains = []
        self.cleanup_projects = []
        self.cleanup_vms = []
        self.cleanup_secGrps = []

        #Create account 1
        self.account_1 = Account.create(self.api_client, self.services["account"],domainid=self.domain.id)
        self.cleanup_accounts.append(self.account_1)
        self.debug("Created account 1: %s" % self.account_1.name)

        #Create account 2
        self.account_2 = Account.create(self.api_client, self.services["account"],domainid=self.domain.id)
        self.cleanup_accounts.append(self.account_2)
        self.debug("Created account 2: %s" % self.account_2.name)

        self.debug("Deploying virtual machine in account 1: %s" % self.account_1.name)
        self.virtual_machine_1 = VirtualMachine.create(self.api_client,self.services["virtual_machine"],accountid=self.account_1.name,
                                                domainid=self.account_1.domainid,serviceofferingid=self.service_offering.id)
        self.cleanup_vms.append(self.virtual_machine_1)
        self.debug("Deployed vm: %s" % self.virtual_machine_1.id)

        self.debug("Deploying virtual machine in account 2: %s" % self.account_2.name)
        self.virtual_machine_2 = VirtualMachine.create(self.api_client,self.services["virtual_machine"],accountid=self.account_2.name,
                                                domainid=self.account_2.domainid,serviceofferingid=self.service_offering.id)
        self.cleanup_vms.append(self.virtual_machine_2)
        self.debug("Deployed vm: %s" % self.virtual_machine_2.id)

        # Getting default security group of account 1
        securitygroups_account_1 = SecurityGroup.list(self.api_client, account=self.account_1.name, domainid=self.account_1.domainid)

        self.assertEqual(validateList(securitygroups_account_1)[0], PASS, "securitygroups list validation\
                failed, securitygroups list is %s" % securitygroups_account_1)
        self.sec_grp_1 = securitygroups_account_1[0]

        # Getting default security group of account 2
        securitygroups_account_2 = SecurityGroup.list(self.api_client, account=self.account_2.name, domainid=self.account_2.domainid)

        self.assertEqual(validateList(securitygroups_account_2)[0], PASS, "securitygroups list validation\
                failed, securitygroups list is %s" % securitygroups_account_2)
        self.sec_grp_2 = securitygroups_account_2[0]
        return

    def tearDown(self):
        exceptions = []
        try:
            #Clean up, terminate the created network offerings
            cleanup_resources(self.api_client, self.cleanup)
        except Exception as e:
            self.debug("Warning: Exception during cleanup : %s" % e)
            exceptions.append(e)

        #below components is not a part of cleanup because to mandate the order and to cleanup network
        try:
            for vm in self.cleanup_vms:
                vm.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during virtual machines cleanup : %s" % e)
            exceptions.append(e)

        # Wait for VMs to expunge
        wait_for_cleanup(self.api_client, ["expunge.delay", "expunge.interval"])

        try:
            for sec_grp in self.cleanup_secGrps:
                sec_grp.delete(self.api_client)
        except Exception as e:
            self.debug("Warning : Exception during security groups cleanup: %s" % e)
            exceptions.append(e)

        try:
            for project in self.cleanup_projects:
                project.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during project cleanup : %s" % e)
            exceptions.append(e)

        try:
            for account in self.cleanup_accounts:
                account.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during account cleanup : %s" % e)
            exceptions.append(e)

        #Wait till all resources created are cleaned up completely and then attempt to delete Network
        time.sleep(self.services["sleep"])

        try:
            for network in self.cleanup_networks:
                network.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during network cleanup : %s" % e)
            exceptions.append(e)

        try:
            for domain in self.cleanup_domains:
                domain.delete(self.api_client)
        except Exception as e:
            self.debug("Warning: Exception during domain cleanup : %s" % e)
            exceptions.append(e)

        if len(exceptions) > 0:
            self.fail("There were exceptions during cleanup: %s" % exceptions)

        return

    def setSharedNetworkParams(self, network, range=20):

        # @range: range decides the endip. Pass the range as "x" if you want the difference between the startip
        # and endip as "x"
        # Set the subnet number of shared networks randomly prior to execution
        # of each test case to avoid overlapping of ip addresses
        shared_network_subnet_number = random.randrange(1,254)

        self.services[network]["gateway"] = "172.16."+str(shared_network_subnet_number)+".1"
        self.services[network]["startip"] = "172.16."+str(shared_network_subnet_number)+".2"
        self.services[network]["endip"] = "172.16."+str(shared_network_subnet_number)+"."+str(range+1)
        self.services[network]["netmask"] = "255.255.255.0"

        return

    @data("accessByIp","accessByName")
    @attr(tags = ["advancedsg"])
    def test_36_ssh_vm_other_sg(self, value):
        """ Test access VM in other security group from vm in one security group"""

        # Steps,
        #  1. create two accounts, this will create two default security groups of the accounts
        #  2. Deploy VM in both the accounts
        #  3. Add ingress rule for the security group 2
        #  4. Access vm in security group 2 from vm in security group 1
        # Validations,
        #  1. Vm should be accessible from the other security group because ingress rule has been created

        #Create account 1

        # Authorize ingress rule for the security groups
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.protocol = 'TCP'
        cmd.startport = 22
        cmd.endport = 80
        cmd.cidrlist = ['0.0.0.0/0']
        cmd.securitygroupid = self.sec_grp_1.id
        self.api_client.authorizeSecurityGroupIngress(cmd)

        cmd.securitygroupid = self.sec_grp_2.id
        cmd.cidrlist = []
        # Authorize to only account not CIDR
        cmd.usersecuritygrouplist = [{'account':str(self.account_1.name),'group': str(self.sec_grp_1.name)}]
        self.api_client.authorizeSecurityGroupIngress(cmd)

        self.debug("Getting SSH client of virtual machine 1: %s" % self.virtual_machine_1.id)
        sshClient = self.virtual_machine_1.get_ssh_client(ipaddress=self.virtual_machine_1.nic[0].ipaddress)
        try:
            if value == "accessByIp":
                self.debug("SSHing into vm_2 %s from vm_1 %s" % (self.virtual_machine_2.nic[0].ipaddress,self.virtual_machine_1.nic[0].ipaddress))
                command = "ssh -t -t root@%s" % self.virtual_machine_2.nic[0].ipaddress
            elif value == "accessByName":
                self.debug("SSHing into vm_2 %s from vm_1 %s" % (self.virtual_machine_2.name,self.virtual_machine_1.name))
                command = "ssh -t -t root@%s" % self.virtual_machine_2.name
            else:
                self.fail("Invalid value passed to the test case")
            self.debug("command: --> %s" % command)
            res = sshClient.execute(command)
            self.debug("SSH result: %s" % str(res))

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
              (self.virtual_machine_1.nic[0].ipaddress, e)
              )
        result = str(res)
        self.assertNotEqual(
            result.count("No route to host"),
            1,
            "SSH should be successful"
            )
        return

    @attr(tags = ["advancedsg"])
    def test_37_ping_vm_other_sg(self):
        """ Test access VM in other security group from vm in one security group"""

        # Steps,
        #  1. create two accounts, this will create two default security groups of the accounts
        #  2. Deploy VM in both the accounts
        #  3. Add ingress rule for TCMP protocol the security group 2 allowing traffic from sec group 1
        #  4. Ping vm in security group 2 from vm in security group 1
        # Validations,
        #  1. Vm should be pinged from the other security group because ingress rule for TCMP has been created

        #Create account 1

        # Authorize ingress rule for the security groups
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.protocol = 'TCP'
        cmd.startport = 22
        cmd.endport = 80
        cmd.cidrlist = '0.0.0.0/0'
        cmd.securitygroupid = self.sec_grp_1.id
        self.api_client.authorizeSecurityGroupIngress(cmd)

        cmd.protocol = 'ICMP'
        cmd.icmptype = "-1"
        cmd.icmpcode = "-1"
        cmd.cidrlist = []
        # Authorize to only account not CIDR
        cmd.usersecuritygrouplist = [{'account':str(self.account_1.name),'group': str(self.sec_grp_1.name)}]
        cmd.securitygroupid = self.sec_grp_2.id
        self.api_client.authorizeSecurityGroupIngress(cmd)

        self.debug("Getting SSH client of virtual machine 1: %s" % self.virtual_machine_1.id)
        sshClient = self.virtual_machine_1.get_ssh_client(ipaddress=self.virtual_machine_1.nic[0].ipaddress)
        try:
            self.debug("SSHing into vm_2 %s from vm_1 %s" % (self.virtual_machine_2.nic[0].ipaddress,self.virtual_machine_1.nic[0].ipaddress))
            command = "ping -c 1 %s" % self.virtual_machine_2.nic[0].ipaddress
            self.debug("command: --> %s" % command)
            res = sshClient.execute(command)
            self.debug("SSH result: %s" % str(res))

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
              (self.virtual_machine_1.nic[0].ipaddress, e)
              )
        result = str(res)
        self.assertEqual(
                 result.count("1 received"),
                 1,
                 "Ping to outside world from VM should be successful"
                 )
        return

    @data("accessByIp","accessByName")
    @attr(tags = ["advancedsg"])
    def test_38_ssh_vm_other_sg_new_vm(self, value):
        """ Test access VM in other security group from a new vm in one security group"""

        # Steps,
        #  1. create two accounts, this will create two default security groups of the accounts
        #  2. Deploy VM in both the accounts
        #  3. Add ingress rule for the security group 2 allowing traffic from sec group 1
        #  4. Add new VMs in sec grp 1 and sec grp 2
        #  4. Access new vm in security group 2 from new vm in security group 1
        # Validations,
        #  1. Vm should be accessible from the other security group because ingress rule has been created

        # Authorize ingress rule for the security groups
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.protocol = 'TCP'
        cmd.startport = 22
        cmd.endport = 80
        cmd.cidrlist = '0.0.0.0/0'
        cmd.securitygroupid = self.sec_grp_1.id
        self.api_client.authorizeSecurityGroupIngress(cmd)

        cmd.securitygroupid = self.sec_grp_2.id
        cmd.cidrlist = []
        # Authorize to only account not CIDR
        cmd.usersecuritygrouplist = [{'account':str(self.account_1.name),'group': str(self.sec_grp_1.name)}]
        self.api_client.authorizeSecurityGroupIngress(cmd)

        self.debug("Deploying virtual machine in account 1: %s" % self.account_1.name)
        self.virtual_machine_3 = VirtualMachine.create(self.api_client,self.services["virtual_machine"],accountid=self.account_1.name,
                                                domainid=self.account_1.domainid,serviceofferingid=self.service_offering.id)
        self.cleanup_vms.append(self.virtual_machine_3)
        self.debug("Deployed vm: %s" % self.virtual_machine_3.id)

        self.debug("Deploying virtual machine in account 2: %s" % self.account_2.name)
        self.virtual_machine_4 = VirtualMachine.create(self.api_client,self.services["virtual_machine"],accountid=self.account_2.name,
                                                domainid=self.account_2.domainid,serviceofferingid=self.service_offering.id)
        self.cleanup_vms.append(self.virtual_machine_4)
        self.debug("Deployed vm: %s" % self.virtual_machine_4.id)

        self.debug("Getting SSH client of virtual machine 1: %s" % self.virtual_machine_3.id)
        sshClient = self.virtual_machine_3.get_ssh_client(ipaddress=self.virtual_machine_3.nic[0].ipaddress)
        try:
            if value == "accessByIp":
                self.debug("SSHing into vm_4 %s from vm_3 %s" % (self.virtual_machine_4.nic[0].ipaddress,self.virtual_machine_3.nic[0].ipaddress))
                command = "ssh -t -t root@%s" % self.virtual_machine_4.nic[0].ipaddress
            elif value == "accessByName":
                self.debug("SSHing into vm_2 %s from vm_1 %s" % (self.virtual_machine_4.name,self.virtual_machine_3.name))
                command = "ssh -t -t root@%s" % self.virtual_machine_4.name
            else:
                self.fail("Invalid value passed to the test case")
            self.debug("command: --> %s" % command)
            res = sshClient.execute(command)
            self.debug("SSH result: %s" % str(res))

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
              (self.virtual_machine_3.nic[0].ipaddress, e)
              )
        result = str(res)
        self.assertNotEqual(
            result.count("No route to host"),
            1,
            "SSH should be successful"
            )
        return

    @data("accessByIp","accessByName")
    @attr(tags = ["advancedsg"])
    def test_39_ssh_vm_other_sg_from_multiple_sg_vm(self, value):
        """ Test access VM in other security group from vm belonging to multiple security groups"""

        # Steps,
        #  1. create two accounts, this will create two default security groups of the accounts
        #  2. Deploy VM in both the accounts
        #  3. Add ingress rule for the security group 2 allowing traffic from sec group 1
        #  4. Create additional security group and deploy vm in 2 security groups
        #  4. Access vm in security group 2 from vm in two security groups
        # Validations,
        #  1. Vm should be accessible from the other security group because ingress rule has been created

        # Authorize ingress rule for the security groups
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.protocol = 'TCP'
        cmd.startport = 22
        cmd.endport = 80
        cmd.cidrlist = '0.0.0.0/0'
        cmd.securitygroupid = self.sec_grp_1.id
        self.api_client.authorizeSecurityGroupIngress(cmd)

        cmd.securitygroupid = self.sec_grp_2.id
        cmd.cidrlist = []
        # Authorize to only account not CIDR
        cmd.usersecuritygrouplist = [{'account':str(self.account_1.name),'group': str(self.sec_grp_1.name)}]
        self.api_client.authorizeSecurityGroupIngress(cmd)

        self.sec_grp_3 = SecurityGroup.create(self.api_client, self.services["security_group"], account=self.account_1.name,
                                              domainid=self.account_1.domainid)
        self.sec_grp_3.authorize(self.api_client,self.services["ingress_rule"])

        self.debug("Deploying virtual machine in account 1: %s, with sec groups %s and %s" %
                    (self.account_1.name,self.sec_grp_1.id, self.sec_grp_3.id))

        self.virtual_machine_3 = VirtualMachine.create(self.api_client,self.services["virtual_machine"],accountid=self.account_1.name,
                                                domainid=self.account_1.domainid,serviceofferingid=self.service_offering.id,
                                                securitygroupids = [self.sec_grp_1.id, self.sec_grp_3.id])
        self.cleanup_vms.append(self.virtual_machine_3)
        self.debug("Deployed vm: %s" % self.virtual_machine_3.id)

        self.debug("Getting SSH client of virtual machine 1: %s" % self.virtual_machine_3.id)
        sshClient = self.virtual_machine_3.get_ssh_client(ipaddress=self.virtual_machine_3.nic[0].ipaddress)
        try:
            if value == "accessByIp":
                self.debug("SSHing into vm_2 %s from vm_3 %s" % (self.virtual_machine_2.nic[0].ipaddress,self.virtual_machine_3.nic[0].ipaddress))
                command = "ssh -t -t root@%s" % self.virtual_machine_2.nic[0].ipaddress
            elif value == "accessByName":
                self.debug("SSHing into vm_2 %s from vm_3 %s" % (self.virtual_machine_2.name,self.virtual_machine_3.name))
                command = "ssh -t -t root@%s" % self.virtual_machine_2.name
            else:
                self.fail("Invalid value passed to the test case")
            self.debug("command: --> %s" % command)
            res = sshClient.execute(command)
            self.debug("SSH result: %s" % str(res))

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
              (self.virtual_machine_3.nic[0].ipaddress, e)
              )
        result = str(res)
        self.assertNotEqual(
            result.count("No route to host"),
            1,
            "SSH should be successful"
            )
        return

    @attr(tags = ["advancedsg"])
    def test_40_ssh_vm_other_sg_reboot(self):
        """ Test access VM in other security group from vm in one security group before and after reboot"""

        # Steps,
        #  1. create two accounts, this will create two default security groups of the accounts
        #  2. Deploy VM in both the accounts
        #  3. Add ingress rule for the security group 2 allowing traffic from sec group 1
        #  4. Access vm in security group 2 from vm in security group 1
        #  5. Reboot the vm in security group 1 and again access vm in sec grp 2 from it
        # Validations,
        #  1. Vm should be accessible from the other security group because ingress rule has been created

        #Create account 1

        # Authorize ingress rule for the security groups
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.protocol = 'TCP'
        cmd.startport = 22
        cmd.endport = 80
        cmd.cidrlist = '0.0.0.0/0'
        cmd.securitygroupid = self.sec_grp_1.id
        self.api_client.authorizeSecurityGroupIngress(cmd)

        cmd.securitygroupid = self.sec_grp_2.id
        cmd.cidrlist = []
        # Authorize to only account not CIDR
        cmd.usersecuritygrouplist = [{'account':str(self.account_1.name),'group': str(self.sec_grp_1.name)}]
        self.api_client.authorizeSecurityGroupIngress(cmd)

        #Get ssh client of vm in account 1 and ssh to vm in account 2 from it
        self.debug("Getting SSH client of virtual machine 1: %s" % self.virtual_machine_1.id)

        try:
            sshClient = self.virtual_machine_1.get_ssh_client(ipaddress=self.virtual_machine_1.nic[0].ipaddress)
            self.debug("SSHing into vm_2 %s from vm_1 %s" % (self.virtual_machine_2.nic[0].ipaddress,self.virtual_machine_1.nic[0].ipaddress))
            command = "ssh -t -t root@%s" % self.virtual_machine_2.nic[0].ipaddress
            self.debug("command: --> %s" % command)
            res = sshClient.execute(command)
            self.debug("SSH result: %s" % str(res))
        except Exception as e:
            self.fail("Exception in SSH operation: %s" % e)
        result = str(res)
        self.assertNotEqual(
            result.count("No route to host"),
            1,
            "SSH should be successful"
            )

        self.debug("Rebooting virtual machine %s" % self.virtual_machine_1.id)
        self.virtual_machine_1.reboot(self.api_client)

        # Repeat the same procedure after reboot
        self.debug("Getting SSH client of virtual machine 1: %s" % self.virtual_machine_1.id)

        try:
            sshClient = self.virtual_machine_1.get_ssh_client(ipaddress=self.virtual_machine_1.nic[0].ipaddress)
            self.debug("SSHing into vm_2 %s from vm_1 %s" % (self.virtual_machine_2.nic[0].ipaddress,self.virtual_machine_1.nic[0].ipaddress))
            command = "ssh -t -t root@%s" % self.virtual_machine_2.nic[0].ipaddress
            self.debug("command: --> %s" % command)
            res = sshClient.execute(command)
            self.debug("SSH result: %s" % str(res))
        except Exception as e:
            self.fail("Exception in ssh operation: %s" % e)
        result = str(res)
        self.assertNotEqual(
            result.count("No route to host"),
            1,
            "SSH should be successful"
            )
        return
