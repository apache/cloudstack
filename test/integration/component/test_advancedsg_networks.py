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
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from ddt import ddt, data
from marvin.integration.lib.base import (Zone,
                                         ServiceOffering,
                                         Account,
                                         NetworkOffering,
                                         Network,
                                         VirtualMachine,
                                         Domain,
                                         VpcOffering,
                                         VPC,
                                         SecurityGroup)

from marvin.integration.lib.common import (get_domain,
                                           get_zone,
                                           get_template,
                                           get_free_vlan,
                                           list_virtual_machines,
                                           wait_for_cleanup)

from marvin.integration.lib.utils import (cleanup_resources,
                                          random_gen,
                                          validateList)
from marvin.cloudstackAPI import (authorizeSecurityGroupIngress,
                                  revokeSecurityGroupIngress)
from nose.plugins.attrib import attr
from marvin.codes import PASS
import time
import random

class TestCreateZoneSG(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cloudstackTestClient = super(TestCreateZoneSG,cls).getClsTestClient()
        cls.api_client = cloudstackTestClient.getApiClient()

        # Fill services from the external config file
        cls.services = cloudstackTestClient.getConfigParser().parsedDict

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)

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
        cloudstackTestClient = super(TestNetworksInAdvancedSG,cls).getClsTestClient()
        cls.api_client = cloudstackTestClient.getApiClient()

        cls.services = cloudstackTestClient.getConfigParser().parsedDict

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
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
        # 3. Deploy a VM in subdomain using the shared network

        # Validations,
        #  1. shared network should be created successfully
        # 2. Shared network should be able to be accessed within subdomain (VM should be deployed)

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
        cloudstackTestClient = super(TestNetworksInAdvancedSG_VmOperations,cls).getClsTestClient()
        cls.api_client = cloudstackTestClient.getApiClient()

        cls.services = cloudstackTestClient.getConfigParser().parsedDict

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
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
            self.faill("There were exceptions during cleanup: %s" % exceptions)

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
    def test__16_AccountSpecificNwAccess(self):
        """ Test account specific network access of users"""

        # Steps,
        #  1. create multiple accounts/users and their account specific SG enabled shared networks
        #  2. Deploy VM in the account specific network with user of that account
        # 3. Try to deploy VM in one account specific network from other account user

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
        api_client_account_1 = self.testClient.createUserApiClient(UserName=account_1.name,
                                                         DomainName=account_1.domain)

        # Creaint user API client of account 2
        api_client_account_2 = self.testClient.createUserApiClient(UserName=account_2.name,
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
        api_client_domain_1 = self.testClient.createUserApiClient(UserName=account_1.name,
                                                         DomainName=account_1.domain)

        # Creaint user API client of account 2
        api_client_domain_2 = self.testClient.createUserApiClient(UserName=account_2.name,
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

class TestSecurityGroups_BasicSanity(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cloudstackTestClient = super(TestSecurityGroups_BasicSanity,cls).getClsTestClient()
        cls.api_client = cloudstackTestClient.getApiClient()

        cls.services = cloudstackTestClient.getConfigParser().parsedDict

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
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
    def test_22_DeployVM_WithoutSG(self):
        """ Test deploy VM without passing any security group id"""

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
