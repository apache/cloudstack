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

""" P1 tests for netscaler load balancing
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import migrateVirtualMachine
from marvin.lib.utils import (cleanup_resources,
                              random_gen)
from marvin.lib.base import (Account,
                             VirtualMachine,
                             PublicIPAddress,
                             LoadBalancerRule,
                             ServiceOffering,
                             NetworkOffering,
                             Host,
                             Network,
                             NATRule,
                             Configurations)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               add_netscaler)
from marvin.sshClient import SshClient
import time


class TestLbSourceNat(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls._cleanup = []
        cls.testClient = super(TestLbSourceNat, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )

        cls.testdata["lbrule"]["publicport"] = 22
        cls.testdata["configurableData"]["netscaler"]["lbdevicededicated"] = False

        try:
            cls.netscaler = add_netscaler(cls.api_client, cls.zone.id, cls.testdata["configurableData"]["netscaler"])
            cls._cleanup.append(cls.netscaler)
            cls.network_offering = NetworkOffering.create(
                cls.api_client,
                cls.testdata["nw_off_isolated_netscaler"],
                conservemode=True
            )
            # Enable Network offering
            cls.network_offering.update(cls.api_client, state='Enabled')
            cls.testdata["small"]["zoneid"] = cls.zone.id
            cls.testdata["small"]["template"] = cls.template.id
            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.testdata["service_offering"]
            )
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setUpClass: %s" % e)
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
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup = [self.account]
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            # Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advancedns"])
    def test_add_lb_on_source_nat(self):
        """Test Create LB rule for sourceNat IP address
        """

        # Validate the following
        # 1. Deploy the first VM using a network from the above created
        #    Network offering.
        # 2. Deploy another VM.
        # 3. Create LB rule for the sourceNat IP address. User should NOT be
        #    allowed to create an LB rule on source NAT

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        self.network = Network.create(
            self.apiclient,
            self.testdata["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id
        )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        virtual_machine_1 = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(self.network.id)]
        )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_1.id
        )

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % virtual_machine_1.id
        )

        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )
        vm_response = list_vm_response[0]

        self.assertEqual(
            vm_response.state,
            "Running",
            "VM state should be running after deployment"
        )

        self.debug("Deploying another VM in account: %s" %
                   self.account.name)

        # Spawn an instance in that network
        virtual_machine_2 = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(self.network.id)]
        )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_2.id
        )

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % virtual_machine_2.id
        )

        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )
        vm_response = list_vm_response[0]

        self.assertEqual(
            vm_response.state,
            "Running",
            "VM state should be running after deployment"
        )
        src_nat_list = PublicIPAddress.list(
            self.apiclient,
            associatednetworkid=self.network.id,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True,
            issourcenat=True,
        )
        self.assertEqual(
            isinstance(src_nat_list, list),
            True,
            "List Public IP should return a valid source NAT"
        )
        self.assertNotEqual(
            len(src_nat_list),
            0,
            "Length of response from listPublicIp should not be 0"
        )

        src_nat = src_nat_list[0]

        self.debug("Trying to create LB rule on source NAT IP: %s" %
                   src_nat.ipaddress)
        # Create Load Balancer rule with source NAT
        with self.assertRaises(Exception):
            LoadBalancerRule.create(
                self.apiclient,
                self.testdata["lbrule"],
                ipaddressid=src_nat.id,
                accountid=self.account.name
            )
        return


class TestLbOnIpWithPf(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls._cleanup = []
        cls.testClient = super(TestLbOnIpWithPf, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )

        cls.testdata["lbrule"]["publicport"] = 22
        cls.testdata["configurableData"]["netscaler"]["lbdevicededicated"] = False

        try:
            cls.netscaler = add_netscaler(cls.api_client, cls.zone.id, cls.testdata["configurableData"]["netscaler"])
            cls._cleanup.append(cls.netscaler)
            cls.network_offering = NetworkOffering.create(
                cls.api_client,
                cls.testdata["nw_off_isolated_netscaler"],
                conservemode=True
            )
            # Enable Network offering
            cls.network_offering.update(cls.api_client, state='Enabled')
            cls.testdata["small"]["zoneid"] = cls.zone.id
            cls.testdata["small"]["template"] = cls.template.id

            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.testdata["service_offering"]
            )
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setUpClass: %s" % e)
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
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup = [self.account]
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            # Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advancedns"])
    def test_lb_on_ip_with_pf(self):
        """Test Create LB rule for sourceNat IP address
        """

        # Validate the following
        # 1. Deploy the first VM using a network from the above created
        #    Network offering.
        # 2. Deploy another VM.
        # 3. Create LB rule on Ip with PF rule. User should NOT be
        #    allowed to create an LB rule on Ip with PF

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        self.network = Network.create(
            self.apiclient,
            self.testdata["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id
        )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        virtual_machine_1 = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(self.network.id)]
        )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_1.id
        )

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % virtual_machine_1.id
        )

        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )
        vm_response = list_vm_response[0]

        self.assertEqual(
            vm_response.state,
            "Running",
            "VM state should be running after deployment"
        )

        self.debug("Deploying another VM in account: %s" %
                   self.account.name)

        # Spawn an instance in that network
        virtual_machine_2 = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(self.network.id)]
        )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_2.id
        )

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % virtual_machine_2.id
        )

        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )
        vm_response = list_vm_response[0]

        self.assertEqual(
            vm_response.state,
            "Running",
            "VM state should be running after deployment"
        )
        self.debug("Associating public IP for network: %s" % self.network.id)
        ip_with_nat_rule = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=self.network.id
        )

        self.debug("Associated %s with network %s" % (
            ip_with_nat_rule.ipaddress.ipaddress,
            self.network.id
        ))
        self.debug("Creating PF rule for IP address: %s" %
                   ip_with_nat_rule.ipaddress.ipaddress)
        NATRule.create(
            self.apiclient,
            virtual_machine_1,
            self.testdata["natrule"],
            ipaddressid=ip_with_nat_rule.ipaddress.id
        )

        self.debug("Trying to create LB rule on IP with NAT: %s" %
                   ip_with_nat_rule.ipaddress.ipaddress)

        # Create Load Balancer rule on IP already having NAT rule
        with self.assertRaises(Exception):
            LoadBalancerRule.create(
                self.apiclient,
                self.testdata["lbrule"],
                ipaddressid=ip_with_nat_rule.ipaddress.id,
                accountid=self.account.name
            )
        return


class TestPfOnIpWithLb(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls._cleanup = []
        cls.testClient = super(TestPfOnIpWithLb, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )

        cls.testdata["lbrule"]["publicport"] = 22
        cls.testdata["configurableData"]["netscaler"]["lbdevicededicated"] = False

        try:
            cls.netscaler = add_netscaler(
                    cls.api_client,
                    cls.zone.id,
                    cls.testdata["configurableData"]["netscaler"])
            cls._cleanup.append(cls.netscaler)
            cls.network_offering = NetworkOffering.create(
                cls.api_client,
                cls.testdata["nw_off_isolated_netscaler"],
                conservemode=True
            )
            # Enable Network offering
            cls.network_offering.update(cls.api_client, state='Enabled')
            cls.testdata["small"]["zoneid"] = cls.zone.id
            cls.testdata["small"]["template"] = cls.template.id

            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.testdata["service_offering"]
            )
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setUpClass: %s" % e)
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
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup = [self.account]
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            # Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advancedns"])
    def test_pf_on_ip_with_lb(self):
        """Test Create a port forwarding rule  on an Ip address
        that already has a LB rule.
        """

        # Validate the following
        # 1. Deploy the first VM using a network from the above created
        #    Network offering.
        # 2. Deploy another VM.
        # 3. Create PF rule on Ip with LB rule. User should NOT be
        #    allowed to create an LB rule on Ip with LB

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        self.network = Network.create(
            self.apiclient,
            self.testdata["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id
        )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        virtual_machine_1 = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(self.network.id)]
        )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_1.id
        )

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % virtual_machine_1.id
        )

        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )
        vm_response = list_vm_response[0]

        self.assertEqual(
            vm_response.state,
            "Running",
            "VM state should be running after deployment"
        )

        self.debug("Deploying another VM in account: %s" %
                   self.account.name)

        # Spawn an instance in that network
        virtual_machine_2 = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(self.network.id)]
        )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_2.id
        )

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % virtual_machine_2.id
        )

        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )
        vm_response = list_vm_response[0]

        self.assertEqual(
            vm_response.state,
            "Running",
            "VM state should be running after deployment"
        )
        self.debug("Associating public IP for network: %s" % self.network.id)

        ip_with_lb_rule = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=self.network.id
        )
        self.debug("Associated %s with network %s" % (
            ip_with_lb_rule.ipaddress.ipaddress,
            self.network.id
        ))
        self.debug("Creating LB rule for IP address: %s" %
                   ip_with_lb_rule.ipaddress.ipaddress)

        LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=ip_with_lb_rule.ipaddress.id,
            accountid=self.account.name,
            networkid=self.network.id
        )

        self.debug("Trying to create PF rule on IP with LB rule: %s" %
                   ip_with_lb_rule.ipaddress.ipaddress)

        with self.assertRaises(Exception):
            NATRule.create(
                self.apiclient,
                virtual_machine_1,
                self.testdata["natrule"],
                ipaddressid=ip_with_lb_rule.ipaddress.id
            )
        return


class TestLbOnNonSourceNat(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls._cleanup = []
        cls.testClient = super(TestLbOnNonSourceNat, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )

        cls.testdata["lbrule"]["publicport"] = 22
        cls.testdata["configurableData"]["netscaler"]["lbdevicededicated"] = False

        try:
            cls.netscaler = add_netscaler(
                    cls.api_client,
                    cls.zone.id,
                    cls.testdata["configurableData"]["netscaler"])
            cls._cleanup.append(cls.netscaler)
            cls.network_offering = NetworkOffering.create(
                cls.api_client,
                cls.testdata["nw_off_isolated_netscaler"],
                conservemode=False
            )
            # Enable Network offering
            cls.network_offering.update(cls.api_client, state='Enabled')
            cls.testdata["small"]["zoneid"] = cls.zone.id
            cls.testdata["small"]["template"] = cls.template.id

            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.testdata["service_offering"]
            )
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setUpClass: %s" % e)
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
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup = [self.account]
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            # Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advancedns"])
    def test_lb_on_non_source_nat(self):
        """Test Create LB rule for non-sourceNat IP address
        """

        # Validate the following
        # 1. Deploy the first VM using a network from the above created
        #    Network offering.
        # 2. Deploy another VM.
        # 3. Acquire Ip address and create LB rule on it. LB rule should be
        #    created successfully

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        self.network = Network.create(
            self.apiclient,
            self.testdata["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id
        )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        virtual_machine_1 = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(self.network.id)]
        )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_1.id
        )

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % virtual_machine_1.id
        )

        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )
        vm_response = list_vm_response[0]

        self.assertEqual(
            vm_response.state,
            "Running",
            "VM state should be running after deployment"
        )

        self.debug("Deploying another VM in account: %s" %
                   self.account.name)

        # Spawn an instance in that network
        virtual_machine_2 = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(self.network.id)]
        )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_2.id
        )

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % virtual_machine_2.id
        )

        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )
        vm_response = list_vm_response[0]

        self.assertEqual(
            vm_response.state,
            "Running",
            "VM state should be running after deployment"
        )
        self.debug("Associating public IP for network: %s" % self.network.id)

        ip_with_lb_rule = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=self.network.id
        )
        self.debug("Associated %s with network %s" % (
            ip_with_lb_rule.ipaddress.ipaddress,
            self.network.id
        ))
        self.debug("Creating LB rule for IP address: %s" %
                   ip_with_lb_rule.ipaddress.ipaddress)

        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=ip_with_lb_rule.ipaddress.id,
            accountid=self.account.name,
            networkid=self.network.id
        )

        self.debug("Trying to create PF rule on IP with LB rule: %s" %
                   ip_with_lb_rule.ipaddress.ipaddress)

        lb_rules = LoadBalancerRule.list(
            self.apiclient,
            id=lb_rule.id,
            listall=True
        )
        self.assertEqual(
            isinstance(lb_rules, list),
            True,
            "List LB rules should return a newly created LB rule"
        )
        return


class TestAddMultipleVmsLb(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls._cleanup = []
        cls.testClient = super(TestAddMultipleVmsLb, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )

        cls.testdata["lbrule"]["publicport"] = 22
        cls.testdata["configurableData"]["netscaler"]["lbdevicededicated"] = False

        try:
            cls.netscaler = add_netscaler(
                    cls.api_client,
                    cls.zone.id,
                    cls.testdata["configurableData"]["netscaler"])
            cls._cleanup.append(cls.netscaler)
            cls.network_offering = NetworkOffering.create(
                cls.api_client,
                cls.testdata["nw_off_isolated_netscaler"],
                conservemode=True
            )
            # Enable Network offering
            cls.network_offering.update(cls.api_client, state='Enabled')
            cls.testdata["small"]["zoneid"] = cls.zone.id
            cls.testdata["small"]["template"] = cls.template.id

            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.testdata["service_offering"]
            )
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setUpClass: %s" % e)
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
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup = [self.account]
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            # Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advancedns"])
    def test_add_multiple_vms_lb(self):
        """Test Add multiple Vms to an existing LB rule.
        """

        # Validate the following
        # 1. Deploy the first VM using a network from the above created
        #    Network offering.
        # 2. Deploy another VM.
        # 3. Acquire Ip address and create LB rule on it. Add multiple VMs to
        #    this rule. LB rule should be
        # In Netscaler: For every Vm added to the LB rule:
        # 1. A server and service instance is created using guest VMs IP and
        #    port number on the Netscaler LB device,
        # 2. This service is bound to lb virtual server corresponding to lb
        #    rule.

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        self.network = Network.create(
            self.apiclient,
            self.testdata["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id
        )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        virtual_machine_1 = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(self.network.id)]
        )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_1.id
        )

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % virtual_machine_1.id
        )

        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )
        vm_response = list_vm_response[0]

        self.assertEqual(
            vm_response.state,
            "Running",
            "VM state should be running after deployment"
        )

        self.debug("Deploying another VM in account: %s" %
                   self.account.name)

        # Spawn an instance in that network
        virtual_machine_2 = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(self.network.id)]
        )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_2.id
        )

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % virtual_machine_2.id
        )

        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )
        vm_response = list_vm_response[0]

        self.assertEqual(
            vm_response.state,
            "Running",
            "VM state should be running after deployment"
        )
        self.debug("Associating public IP for network: %s" % self.network.id)

        ip_with_lb_rule = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=self.network.id
        )
        self.debug("Associated %s with network %s" % (
            ip_with_lb_rule.ipaddress.ipaddress,
            self.network.id
        ))
        self.debug("Creating LB rule for IP address: %s" %
                   ip_with_lb_rule.ipaddress.ipaddress)

        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=ip_with_lb_rule.ipaddress.id,
            accountid=self.account.name,
            networkid=self.network.id
        )

        self.debug("Trying to create PF rule on IP with LB rule: %s" %
                   ip_with_lb_rule.ipaddress.ipaddress)

        lb_rules = LoadBalancerRule.list(
            self.apiclient,
            id=lb_rule.id,
            listall=True
        )
        self.assertEqual(
            isinstance(lb_rules, list),
            True,
            "List LB rules should return a newly created LB rule"
        )
        self.debug("Assigning virtual machines to LB rule")
        lb_rule.assign(self.apiclient, [virtual_machine_1, virtual_machine_2])

        self.debug("SSH into netscaler: %s" %
                   self.testdata["configurableData"]["netscaler"]["ipaddress"])
        try:
            ssh_client = SshClient(
                self.testdata["configurableData"]["netscaler"]["ipaddress"],
                22,
                self.testdata["configurableData"]["netscaler"]["username"],
                self.testdata["configurableData"]["netscaler"]["password"],
            )
            self.debug("command: show server")
            res = ssh_client.execute("show server")
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count(virtual_machine_1.ipaddress),
                2,
                "Server must be configured for virtual machines"
            )
            self.assertEqual(
                result.count(virtual_machine_2.ipaddress),
                2,
                "Server must be configured for virtual machines"
            )

            self.debug("Command:show service")
            res = ssh_client.execute("show service")

            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count(virtual_machine_1.ipaddress),
                3,
                "Service must be configured for virtual machines"
            )
            self.assertEqual(
                result.count(virtual_machine_2.ipaddress),
                3,
                "Service must be configured for virtual machines"
            )
            self.debug("Command:show lb vserver")
            res = ssh_client.execute("show lb vserver")

            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count(ip_with_lb_rule.ipaddress.ipaddress),
                2,
                "virtual server must be configured for public IP address"
            )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.testdata["configurableData"]["netscaler"]["ipaddress"], e))
        return


class TestMultipleLbRules(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls._cleanup = []
        cls.testClient = super(TestMultipleLbRules, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )

        cls.testdata["lbrule"]["publicport"] = 22
        cls.testdata["configurableData"]["netscaler"]["lbdevicededicated"] = False

        try:
            cls.netscaler = add_netscaler(
                    cls.api_client,
                    cls.zone.id,
                    cls.testdata["configurableData"]["netscaler"])
            cls._cleanup.append(cls.netscaler)
            cls.network_offering = NetworkOffering.create(
                cls.api_client,
                cls.testdata["nw_off_isolated_netscaler"],
                conservemode=True
            )
            # Enable Network offering
            cls.network_offering.update(cls.api_client, state='Enabled')
            cls.testdata["small"]["zoneid"] = cls.zone.id
            cls.testdata["small"]["template"] = cls.template.id

            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.testdata["service_offering"]
            )
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setUpClass: %s" % e)

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
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup = [self.account]
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            # Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advancedns"])
    def test_multiple_lb_publicip(self):
        """Test Create multiple LB rules using different public Ips acquired
        """

        # Validate the following
        # 1. Deploy the first VM using a network from the above created
        #    Network offering.
        # 2. Deploy few more VMs.
        # 3. Acquire an Ipaddress and create an LB rule for multiple Vms.
        #    Repeat step2 for few times Requests to all these LB rules should
        #    be serviced correctly.

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        self.network = Network.create(
            self.apiclient,
            self.testdata["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id
        )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        virtual_machine_1 = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(self.network.id)]
        )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_1.id
        )

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % virtual_machine_1.id
        )

        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )
        vm_response = list_vm_response[0]

        self.assertEqual(
            vm_response.state,
            "Running",
            "VM state should be running after deployment"
        )

        self.debug("Deploying another VM in account: %s" %
                   self.account.name)

        # Spawn an instance in that network
        virtual_machine_2 = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(self.network.id)]
        )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_2.id
        )

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % virtual_machine_2.id
        )

        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )
        vm_response = list_vm_response[0]

        self.assertEqual(
            vm_response.state,
            "Running",
            "VM state should be running after deployment"
        )
        self.debug(
            "Associating first public IP for network: %s" %
            self.network.id)

        public_ip_1 = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=self.network.id
        )
        self.debug("Associated %s with network %s" % (
            public_ip_1.ipaddress.ipaddress,
            self.network.id
        ))

        self.debug(
            "Creating LB rule for IP address: %s with round robin algo" %
            public_ip_1.ipaddress.ipaddress)

        self.testdata["lbrule"]["alg"] = 'roundrobin'
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=public_ip_1.ipaddress.id,
            accountid=self.account.name,
            networkid=self.network.id
        )

        lb_rules = LoadBalancerRule.list(
            self.apiclient,
            id=lb_rule.id,
            listall=True
        )
        self.assertEqual(
            isinstance(lb_rules, list),
            True,
            "List LB rules should return a newly created LB rule"
        )
        self.debug("Adding %s, %s to the LB rule %s" % (
            virtual_machine_1.name,
            virtual_machine_2.name,
            lb_rule.name
        ))
        lb_rule.assign(self.apiclient, [virtual_machine_1, virtual_machine_2])

        self.debug(
            "Associating second public IP for network: %s" %
            self.network.id)

        public_ip_2 = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=self.network.id
        )
        self.debug("Associated %s with network %s" % (
            public_ip_2.ipaddress.ipaddress,
            self.network.id
        ))

        self.debug(
            "Creating LB rule for IP address: %s with round robin algo" %
            public_ip_2.ipaddress.ipaddress)

        self.testdata["lbrule"]["alg"] = 'roundrobin'
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=public_ip_2.ipaddress.id,
            accountid=self.account.name,
            networkid=self.network.id
        )

        self.debug("Trying to create PF rule on IP with LB rule: %s" %
                   public_ip_2.ipaddress.ipaddress)

        lb_rules = LoadBalancerRule.list(
            self.apiclient,
            id=lb_rule.id,
            listall=True
        )
        self.assertEqual(
            isinstance(lb_rules, list),
            True,
            "List LB rules should return a newly created LB rule"
        )
        self.debug("Adding %s, %s to the LB rule %s" % (
            virtual_machine_1.name,
            virtual_machine_2.name,
            lb_rule.name
        ))
        lb_rule.assign(self.apiclient, [virtual_machine_1, virtual_machine_2])

        try:
            self.debug(
                "Verifying VMs are accessible with different public Ips")
            hostnames = []
            ssh = virtual_machine_1.get_ssh_client(
                ipaddress=public_ip_1.ipaddress.ipaddress)
            self.debug("Command: hostname")
            result = ssh.execute("hostname")
            self.debug("Output: %s" % result)
            hostnames.append(result)

            res = str(result[0])
            self.assertIn(
                res,
                [virtual_machine_1.name, virtual_machine_2.name],
                "The hostname should match with at least one of instance name"
            )
        except Exception as e:
            self.fail("Exception occurred during SSH: %s - %s" % (
                public_ip_1.ipaddress.ipaddress,
                e))
        try:
            ssh = virtual_machine_1.get_ssh_client(
                ipaddress=public_ip_2.ipaddress.ipaddress,
                reconnect=True
            )
            self.debug("Command: hostname")
            result = ssh.execute("hostname")
            self.debug("Output: %s" % result)
            hostnames.append(result)
            self.debug("Hostnames: %s" % str(hostnames))

            res = str(result[0])
            self.assertIn(
                res,
                [virtual_machine_1.name, virtual_machine_2.name],
                "The hostname should match with at least one of instance name"
            )
        except Exception as e:
            self.fail("Exception occurred during SSH: %s - %s" % (
                public_ip_2.ipaddress.ipaddress,
                e))
        return


class TestMultipleLbRulesSameIp(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls._cleanup = []
        cls.testClient = super(
            TestMultipleLbRulesSameIp,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )

        cls.testdata["lbrule"]["publicport"] = 22
        cls.testdata["configurableData"]["netscaler"]["lbdevicededicated"] = False

        try:
            cls.netscaler = add_netscaler(cls.api_client, cls.zone.id, cls.testdata["configurableData"]["netscaler"])
            cls._cleanup.append(cls.netscaler)
            cls.network_offering = NetworkOffering.create(
                cls.api_client,
                cls.testdata["nw_off_isolated_netscaler"],
                conservemode=True
            )
            # Enable Network offering
            cls.network_offering.update(cls.api_client, state='Enabled')
            cls.testdata["small"]["zoneid"] = cls.zone.id
            cls.testdata["small"]["template"] = cls.template.id

            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.testdata["service_offering"]
            )
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setUpClass: %s" % e)
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
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup = [self.account]
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            # Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advancedns"])
    def test_multiple_lb_same_publicip(self):
        """Test Create multiple LB rules using same public Ips on diff ports
        """

        # Validate the following
        # 1. Deploy the first VM using a network from the above created
        #    Network offering.
        # 2. Deploy few more VMs.
        # 3. Acquire an Ipaddress and create an LB rule for multiple Vms.
        #    Create another Lb rule on the same Ipaddress pointing to
        #    different public port. Requests to all these LB rules should be
        #    serviced correctly.

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                   self.network_offering.id)
        self.network = Network.create(
            self.apiclient,
            self.testdata["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id
        )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        virtual_machine_1 = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(self.network.id)]
        )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_1.id
        )

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % virtual_machine_1.id
        )

        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )
        vm_response = list_vm_response[0]

        self.assertEqual(
            vm_response.state,
            "Running",
            "VM state should be running after deployment"
        )

        self.debug("Deploying another VM in account: %s" %
                   self.account.name)

        # Spawn an instance in that network
        virtual_machine_2 = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(self.network.id)]
        )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_2.id
        )

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % virtual_machine_2.id
        )

        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )
        vm_response = list_vm_response[0]

        self.assertEqual(
            vm_response.state,
            "Running",
            "VM state should be running after deployment"
        )
        self.debug(
            "Associating first public IP for network: %s" %
            self.network.id)

        public_ip = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=self.network.id
        )
        self.debug("Associated %s with network %s" % (
            public_ip.ipaddress.ipaddress,
            self.network.id
        ))

        self.debug(
            "Creating LB rule for IP address: %s with round robin algo" %
            public_ip.ipaddress.ipaddress)

        self.testdata["lbrule"]["alg"] = 'roundrobin'
        lb_rule_1 = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.network.id
        )

        self.debug("Trying to create PF rule on IP with LB rule: %s" %
                   public_ip.ipaddress.ipaddress)

        lb_rules = LoadBalancerRule.list(
            self.apiclient,
            id=lb_rule_1.id,
            listall=True
        )
        self.assertEqual(
            isinstance(lb_rules, list),
            True,
            "List LB rules should return a newly created LB rule"
        )
        self.debug("Adding %s, %s to the LB rule %s" % (
            virtual_machine_1.name,
            virtual_machine_2.name,
            lb_rule_1.name
        ))
        lb_rule_1.assign(self.apiclient, [
            virtual_machine_1,
            virtual_machine_2
        ])
        self.debug(
            "Trying to create LB rule on IP: %s with on same ports" %
            public_ip.ipaddress.ipaddress)
        with self.assertRaises(Exception):
            LoadBalancerRule.create(
                self.apiclient,
                self.testdata["lbrule"],
                ipaddressid=public_ip.ipaddress.id,
                accountid=self.account.name,
                networkid=self.network.id
            )
        self.debug("Create LB rule on same port failed!")
        self.debug("Creating LB rule on IP: %s & public port: %s" % (
            public_ip.ipaddress.ipaddress,
            str(2222)))

        self.testdata["lbrule"]["alg"] = 'roundrobin'
        self.testdata["lbrule"]["publicport"] = 2222
        self.testdata["lbrule"]["name"] = 'SSH2'
        lb_rule_2 = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.network.id
        )

        lb_rules = LoadBalancerRule.list(
            self.apiclient,
            id=lb_rule_2.id,
            listall=True
        )
        self.assertEqual(
            isinstance(lb_rules, list),
            True,
            "List LB rules should return a newly created LB rule"
        )
        self.debug("Adding %s, %s to the LB rule %s" % (
            virtual_machine_1.name,
            virtual_machine_2.name,
            lb_rule_2.name
        ))
        lb_rule_2.assign(self.apiclient, [
            virtual_machine_1,
            virtual_machine_2
        ])

        try:
            self.debug("Verifying VMs are accessible on all LB rules")
            hostnames = []
            ssh = virtual_machine_1.get_ssh_client(
                ipaddress=public_ip.ipaddress.ipaddress,
                reconnect=True
            )
            self.debug("Command: hostname")
            result = ssh.execute("hostname")
            self.debug("Output: %s" % result)
            hostnames.append(result)

            res = str(result[0])
            self.assertIn(
                res,
                [virtual_machine_1.name, virtual_machine_2.name],
                "The hostname should match with at least one of instance name"
            )
        except Exception as e:
            self.fail("Exception occurred during SSH: %s - %s" % (
                public_ip.ipaddress.ipaddress,
                e))
        try:
            ssh = virtual_machine_1.get_ssh_client(
                ipaddress=public_ip.ipaddress.ipaddress,
                reconnect=True,
                port=self.testdata["lbrule"]["publicport"]
            )
            self.debug("Command: hostname")
            result = ssh.execute("hostname")
            self.debug("Output: %s" % result)
            hostnames.append(result)
            self.debug("Hostnames: %s" % str(hostnames))

            res = str(result[0])
            self.assertIn(
                res,
                [virtual_machine_1.name, virtual_machine_2.name],
                "The hostname should match with at least one of instance name"
            )
        except Exception as e:
            self.fail("Exception occurred during SSH: %s - %s" % (
                public_ip.ipaddress.ipaddress,
                e))
        return


class TestLoadBalancingRule(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls._cleanup = []
        cls.testClient = super(TestLoadBalancingRule, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )

        cls.testdata["lbrule"]["publicport"] = 22
        cls.testdata["configurableData"]["netscaler"]["lbdevicededicated"] = False

        try:
            cls.netscaler = add_netscaler(cls.api_client, cls.zone.id, cls.testdata["configurableData"]["netscaler"])
            cls._cleanup.append(cls.netscaler)
            cls.network_offering = NetworkOffering.create(
                cls.api_client,
                cls.testdata["nw_off_isolated_netscaler"],
                conservemode=True
            )
            # Enable Network offering
            cls.network_offering.update(cls.api_client, state='Enabled')
            cls.testdata["small"]["zoneid"] = cls.zone.id
            cls.testdata["small"]["template"] = cls.template.id

            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.testdata["service_offering"]
            )
            cls.account = Account.create(
                cls.api_client,
                cls.testdata["account"],
                admin=True,
                domainid=cls.domain.id
            )
            cls._cleanup.insert(0, cls.account)
            # Creating network using the network offering created
            cls.network = Network.create(
                cls.api_client,
                cls.testdata["network"],
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                networkofferingid=cls.network_offering.id,
                zoneid=cls.zone.id
            )

            # Spawn an instance in that network
            cls.virtual_machine = VirtualMachine.create(
                cls.api_client,
                cls.testdata["small"],
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                serviceofferingid=cls.service_offering.id,
                networkids=[str(cls.network.id)]
            )
            cls.public_ip = PublicIPAddress.create(
                cls.api_client,
                accountid=cls.account.name,
                zoneid=cls.zone.id,
                domainid=cls.account.domainid,
                networkid=cls.network.id
            )
            cls.lb_rule = LoadBalancerRule.create(
                cls.api_client,
                cls.testdata["lbrule"],
                ipaddressid=cls.public_ip.ipaddress.id,
                accountid=cls.account.name,
                networkid=cls.network.id
            )
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setUpClass: %s" % e)
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
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            # Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advancedns"])
    def test_01_edit_name_lb_rule(self):
        """Test edit name of LB rule
        """

        # Validate the following
        # 1. Create an Lb rule for couple of Vms .
        # 2. Edit the name of the existing LB rule. When all the existing
        #    Lbrules are listed , we should see the edited name.

        self.debug("Assigning VMs to LB rule: %s" % self.lb_rule.name)
        self.lb_rule.assign(self.apiclient, [self.virtual_machine])

        self.debug("Editing name of the LB rule: %s" % self.lb_rule.name)
        new_name = random_gen()
        self.lb_rule.update(self.apiclient, name=new_name)

        self.debug("Verifing the name change in list Lb rules call")
        lb_rules = LoadBalancerRule.list(self.apiclient, id=self.lb_rule.id)

        self.assertEqual(
            isinstance(lb_rules, list),
            True,
            "List LB call should return a valid list"
        )
        lb = lb_rules[0]
        self.assertEqual(
            lb.name,
            new_name,
            "LB name should be updated with the new name"
        )
        return

    @attr(tags=["advancedns"])
    def test_02_edit_lb_ports(self):
        """Test edit public port of LB rule
        """

        # Validate the following
        # 1. Create an Lb rule for couple of Vms .
        # 2. Edit the public/private of the existing LB rule. When all the
        #    existing Lbrules are listed, this should not be allowed.

        self.debug(
            "Editing public port of the LB rule: %s" %
            self.lb_rule.name)
        port = 8888
        with self.assertRaises(Exception):
            self.lb_rule.update(self.apiclient, publicport=port)

        self.debug(
            "Editing private port of the LB rule: %s" %
            self.lb_rule.name)
        with self.assertRaises(Exception):
            self.lb_rule.update(self.apiclient, privateport=port)

        return

    @attr(tags=["advancedns"])
    def test_03_delete_lb_rule(self):
        """Test delete LB rule
        """

        # Validate the following
        # 1. Delete existing load balancing rule
        # 2. In netscaler service and port corresponding to LB rule should get
        #    deleted. Any request to IP should error out.

        self.debug("Deleting existing load balancing rule")
        self.lb_rule.delete(self.apiclient)

        self.debug("SSH into Netscaler to verify other resources are deleted")
        try:
            ssh_client = SshClient(
                self.testdata["configurableData"]["netscaler"]["ipaddress"],
                self.testdata["configurableData"]["netscaler"]["port"],
                self.testdata["configurableData"]["netscaler"]["username"],
                self.testdata["configurableData"]["netscaler"]["password"],
            )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                self.public_ip.ipaddress.ipaddress,
                self.lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count("ERROR: No such resource"),
                1,
                "Virtual server should get deleted after removing LB rule"
            )

            cmd = "show ip"
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count(self.public_ip.ipaddress.ipaddress),
                0,
                "Virtual server should get deleted after removing LB rule"
            )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.testdata["configurableData"]["netscaler"]["ipaddress"], e))
        return


class TestDeleteCreateLBRule(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls._cleanup = []
        cls.testClient = super(TestDeleteCreateLBRule, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )

        cls.testdata["lbrule"]["publicport"] = 22
        cls.testdata["configurableData"]["netscaler"]["lbdevicededicated"] = False

        try:
            cls.netscaler = add_netscaler(cls.api_client, cls.zone.id, cls.testdata["configurableData"]["netscaler"])
            cls._cleanup.append(cls.netscaler)
            cls.network_offering = NetworkOffering.create(
                cls.api_client,
                cls.testdata["nw_off_isolated_netscaler"],
                conservemode=True
            )
            # Enable Network offering
            cls.network_offering.update(cls.api_client, state='Enabled')
            cls.testdata["small"]["zoneid"] = cls.zone.id
            cls.testdata["small"]["template"] = cls.template.id
            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.testdata["service_offering"]
            )
            cls.account = Account.create(
                cls.api_client,
                cls.testdata["account"],
                admin=True,
                domainid=cls.domain.id
            )
            cls._cleanup.insert(0, cls.account)
            # Creating network using the network offering created
            cls.network = Network.create(
                cls.api_client,
                cls.testdata["network"],
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                networkofferingid=cls.network_offering.id,
                zoneid=cls.zone.id
            )

            # Spawn an instance in that network
            cls.virtual_machine = VirtualMachine.create(
                cls.api_client,
                cls.testdata["small"],
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                serviceofferingid=cls.service_offering.id,
                networkids=[str(cls.network.id)]
            )
            cls.public_ip = PublicIPAddress.create(
                cls.api_client,
                accountid=cls.account.name,
                zoneid=cls.zone.id,
                domainid=cls.account.domainid,
                networkid=cls.network.id
            )
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setUpClass: %s" % e)
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
        self.lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=self.public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.network.id
        )
        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            # Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advancedns"])
    def test_01_create_with_same_public_port(self):
        """Test create LB rule with same public port after deleting rule"""

        # Validate the following
        # 1. Delete existing rule and create exactly same rule with different
        #    public port
        # 2. Requests should be served correctly

        self.debug("Delete the existing LB rule: %s" % self.lb_rule.name)
        self.lb_rule.delete(self.apiclient)
        self.debug("LB rule deleted")

        self.debug("Create a new LB rule with different public port")
        self.testdata["lbrule"]["publicport"] = 23
        LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=self.public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.network.id
        )


class TestVmWithLb(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls._cleanup = []
        cls.testClient = super(TestVmWithLb, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )

        cls.testdata["lbrule"]["publicport"] = 22
        cls.testdata["configurableData"]["netscaler"]["lbdevicededicated"] = False

        try:
            cls.netscaler = add_netscaler(cls.api_client, cls.zone.id, cls.testdata["configurableData"]["netscaler"])
            cls._cleanup.append(cls.netscaler)
            cls.network_offering = NetworkOffering.create(
                cls.api_client,
                cls.testdata["nw_off_isolated_netscaler"],
                conservemode=True
            )
            # Enable Network offering
            cls.network_offering.update(cls.api_client, state='Enabled')
            cls.testdata["small"]["zoneid"] = cls.zone.id
            cls.testdata["small"]["template"] = cls.template.id

            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.testdata["service_offering"]
            )
            cls.account = Account.create(
                cls.api_client,
                cls.testdata["account"],
                admin=True,
                domainid=cls.domain.id
            )
            cls._cleanup.insert(0, cls.account)
            # Creating network using the network offering created
            cls.network = Network.create(
                cls.api_client,
                cls.testdata["network"],
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                networkofferingid=cls.network_offering.id,
                zoneid=cls.zone.id
            )

            # Spawn an instance in that network
            cls.vm_1 = VirtualMachine.create(
                cls.api_client,
                cls.testdata["small"],
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                serviceofferingid=cls.service_offering.id,
                networkids=[str(cls.network.id)]
            )
            cls.vm_2 = VirtualMachine.create(
                cls.api_client,
                cls.testdata["small"],
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                serviceofferingid=cls.service_offering.id,
                networkids=[str(cls.network.id)]
            )
            cls.public_ip_1 = PublicIPAddress.create(
                cls.api_client,
                accountid=cls.account.name,
                zoneid=cls.zone.id,
                domainid=cls.account.domainid,
                networkid=cls.network.id
            )
            cls.lb_rule_1 = LoadBalancerRule.create(
                cls.api_client,
                cls.testdata["lbrule"],
                ipaddressid=cls.public_ip_1.ipaddress.id,
                accountid=cls.account.name,
                networkid=cls.network.id
            )
            cls.public_ip_2 = PublicIPAddress.create(
                cls.api_client,
                accountid=cls.account.name,
                zoneid=cls.zone.id,
                domainid=cls.account.domainid,
                networkid=cls.network.id
            )
            cls.lb_rule_2 = LoadBalancerRule.create(
                cls.api_client,
                cls.testdata["lbrule"],
                ipaddressid=cls.public_ip_2.ipaddress.id,
                accountid=cls.account.name,
                networkid=cls.network.id
            )
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setUpClass: %s" % e)
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
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            # Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advancedns"])
    def test_01_delete_public_ip(self):
        """Test delete one public Ip with LB rules"""

        # Validate the following
        # 1. Associate 2 public Ips and create load balancing rules in it
        # 2. Delete one of the public Ip
        # 3. All the LB rules should be removed from that public Ip
        # 4. In netscaler, make sure that all LB rules associated with that
        #    public Ip should get removed.
        # 5. All servers and testdata service to that public Ip get deleted

        self.debug("Deleting public IP: %s from network: %s" % (
            self.public_ip_2.ipaddress.ipaddress,
            self.network.name
        ))
        self.public_ip_2.delete(self.apiclient)
        self.debug(
            "Public Ip: %s is deleted!" %
            self.public_ip_2.ipaddress.ipaddress)
        lb_rules = LoadBalancerRule.list(
            self.apiclient,
            publicipid=self.public_ip_2.ipaddress.id,
            listall=True,
        )
        self.assertEqual(
            lb_rules,
            None,
            "LB rules associated with the public Ip should get deleted"
        )
        self.debug("SSH into Netscaler to verify other resources are deleted")
        try:
            ssh_client = SshClient(
                self.testdata["configurableData"]["netscaler"]["ipaddress"],
                self.testdata["configurableData"]["netscaler"]["port"],
                self.testdata["configurableData"]["netscaler"]["username"],
                self.testdata["configurableData"]["netscaler"]["password"],
            )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                self.public_ip_2.ipaddress.ipaddress,
                self.lb_rule_2.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count("ERROR: No such resource"),
                1,
                "Virtual server should get deleted after removing LB rule"
            )

            cmd = "show ip"
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count(self.public_ip_2.ipaddress.ipaddress),
                0,
                "Virtual server should get deleted after removing LB rule"
            )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.testdata["configurableData"]["netscaler"]["ipaddress"], e))
        return

    @attr(tags=["advancedns"])
    def test_02_stop_user_vm(self):
        """Test stop user VM with LB"""

        # Validate the following
        # 1. Create 2 instances and add these two for load balancing
        # 2. Stop one of the user VM
        # 3. Test whether the request are not sent to stopped user VM
        # 4. In netscaler, LB rules for this VM  still remain configured.But
        #    it will be marked as being down

        try:
            self.lb_rule_1.assign(self.apiclient, [self.vm_1, self.vm_2])
            self.vm_2.stop(self.apiclient)
        except Exception as e:
            self.fail("Exception occurred: %s" % e)

        try:
            self.debug(
                "Verifying request served by only running instances")
            hostnames = []
            ssh = self.vm_1.get_ssh_client(
                ipaddress=self.public_ip_1.ipaddress.ipaddress)
            self.debug("Command: hostname")
            result = ssh.execute("hostname")
            self.debug("Output: %s" % result)
            hostnames.append(result)

            ssh = self.vm_1.get_ssh_client(
                ipaddress=self.public_ip_1.ipaddress.ipaddress,
                reconnect=True
            )
            self.debug("Command: hostname")
            result = ssh.execute("hostname")
            self.debug("Output: %s" % result)
            hostnames.append(result)
            self.debug("Hostnames: %s" % str(hostnames))

            self.assertEqual(
                hostnames[0],
                hostnames[1],
                "Hostnames must be same as another VM is stopped"
            )
        except Exception as e:
            self.fail("Exception occurred during SSH: %s - %s" % (
                self.public_ip_1.ipaddress.ipaddress,
                e))
        self.debug("SSH into Netscaler to rules still persist")
        try:
            ssh_client = SshClient(
                self.testdata["configurableData"]["netscaler"]["ipaddress"],
                self.testdata["configurableData"]["netscaler"]["port"],
                self.testdata["configurableData"]["netscaler"]["username"],
                self.testdata["configurableData"]["netscaler"]["password"],
            )

            cmd = "show server"
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertNotEqual(
                result.count(self.vm_2.ipaddress),
                0,
                "The server should be present in netscaler after VM stop"
            )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.testdata["configurableData"]["netscaler"]["ipaddress"], e))
        return

    @attr(tags=["advancedns"])
    def test_03_start_user_vm(self):
        """Test start user VM with LB"""

        # Validate the following
        # 1. Create 2 instances and add these two for load balancing
        # 2. Stop one of the user VM
        # 3. Test whether the request are not sent to stopped user VM
        # 4. In netscaler, LB rules for this VM  still remain configured.But
        #    it will be marked as being down

        self.debug("Starting VM instance: %s" % self.vm_2.name)
        self.vm_2.start(self.apiclient)
        self.debug("Starting VM: %s" % self.vm_2.name)
        self.debug("Sleeping for netscaler to recognize service is up")
        time.sleep(120)

        try:
            self.debug(
                "Verifying request served by only running instances")
            hostnames = []
            ssh_1 = self.vm_1.get_ssh_client(
                ipaddress=self.public_ip_1.ipaddress.ipaddress)
            self.debug("Command: hostname")
            result = ssh_1.execute("hostname")
            self.debug("Output: %s" % result)
            hostnames.append(result)

            ssh_2 = self.vm_2.get_ssh_client(
                ipaddress=self.public_ip_1.ipaddress.ipaddress,
                reconnect=True
            )
            self.debug("Command: hostname")
            result = ssh_2.execute("hostname")
            self.debug("Output: %s" % result)
            hostnames.append(result)
            self.debug("Hostnames: %s" % str(hostnames))

            self.assertNotEqual(
                hostnames[0],
                hostnames[1],
                "Both request should be served by different instances"
            )
        except Exception as e:
            self.fail("Exception occurred during SSH: %s - %s" % (
                self.public_ip_1.ipaddress.ipaddress,
                e))
        self.debug("SSH into Netscaler to rules still persist")
        try:
            ssh_client = SshClient(
                self.testdata["configurableData"]["netscaler"]["ipaddress"],
                self.testdata["configurableData"]["netscaler"]["port"],
                self.testdata["configurableData"]["netscaler"]["username"],
                self.testdata["configurableData"]["netscaler"]["password"],
            )

            cmd = "show server"
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertNotEqual(
                result.count(self.vm_2.ipaddress),
                0,
                "The server should be present in netscaler after VM stop"
            )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.testdata["configurableData"]["netscaler"]["ipaddress"], e))
        return

    @attr(tags=["advancedns", "multihost"])
    def test_04_migrate_user_vm(self):
        """Test migrate user VM with LB"""

        # Validate the following
        # 1. Create 2 instances and add these two for load balancing
        # 2. migrate one Vm to another host.
        # 3. Test whether the request are sent to stopped user VM after migrate
        # 4. In netscaler, the LB rules are still configured.
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("vm migrate is not supported in %s" % self.hypervisor)

        hosts = Host.list(
            self.apiclient,
            zoneid=self.vm_2.zoneid,
            type='Routing'
        )

        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check the number of hosts in the zone"
        )
        self.assertGreaterEqual(
            len(hosts),
            2,
            "Atleast 2 hosts should be present in a zone for VM migration"
        )

        # Remove the host of current VM from the hosts list
        hosts[:] = [host for host in hosts if host.id != self.vm_2.hostid]

        host = hosts[0]
        self.debug("Migrating VM-ID: %s to Host: %s" % (self.vm_2.id, host.id))

        cmd = migrateVirtualMachine.migrateVirtualMachineCmd()
        cmd.hostid = host.id
        cmd.virtualmachineid = self.vm_2.id
        self.apiclient.migrateVirtualMachine(cmd)

        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.vm_2.id
        )
        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )
        vm_response = list_vm_response[0]

        self.assertEqual(
            vm_response.hostid,
            host.id,
            "Check destination hostID of migrated VM"
        )
        self.debug("Migrated VM-ID: %s to Host: %s" % (self.vm_2.id, host.id))
        try:
            self.debug(
                "Verifying request served by only running instances")
            hostnames = []
            ssh_1 = self.vm_1.get_ssh_client(
                ipaddress=self.public_ip_1.ipaddress.ipaddress,
                reconnect=True
            )
            self.debug("Command: hostname")
            result = ssh_1.execute("hostname")
            self.debug("Output: %s" % result)
            hostnames.append(result)

            ssh_2 = self.vm_2.get_ssh_client(
                ipaddress=self.public_ip_1.ipaddress.ipaddress,
                reconnect=True
            )
            self.debug("Command: hostname")
            result = ssh_2.execute("hostname")
            self.debug("Output: %s" % result)
            hostnames.append(result)
            self.debug("Hostnames: %s" % str(hostnames))

            self.assertNotEqual(
                hostnames[0],
                hostnames[1],
                "Both request should be served by different instances"
            )
        except Exception as e:
            self.fail("Exception occurred during SSH: %s - %s" % (
                self.public_ip_1.ipaddress.ipaddress,
                e))
        self.debug("SSH into Netscaler to rules still persist")
        try:
            ssh_client = SshClient(
                self.testdata["configurableData"]["netscaler"]["ipaddress"],
                self.testdata["configurableData"]["netscaler"]["port"],
                self.testdata["configurableData"]["netscaler"]["username"],
                self.testdata["configurableData"]["netscaler"]["password"],
            )

            cmd = "show server"
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertNotEqual(
                result.count(self.vm_2.ipaddress),
                0,
                "The server should be present in netscaler after migrate"
            )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.testdata["configurableData"]["netscaler"]["ipaddress"], e))
        return

    @attr(tags=["advancedns"])
    def test_05_reboot_user_vm(self):
        """Test reboot user VM with LB"""

        # Validate the following
        # 1. Create 2 instances and add these two for load balancing
        # 2. Reboot one of the user VM
        # 3. Test whether the request are sent to both VMs after reboot
        # 4. In netscaler, LB rules for this VM  still remain configured.

        self.debug("Rebooting VM instance: %s" % self.vm_2.name)
        self.vm_2.reboot(self.apiclient)
        self.debug("Rebooting VM: %s" % self.vm_2.name)
        self.debug("Waiting for Netscaler to get the service up")
        time.sleep(120)

        try:
            self.debug(
                "Verifying request served by only running instances")
            hostnames = []
            ssh_1 = self.vm_1.get_ssh_client(
                ipaddress=self.public_ip_1.ipaddress.ipaddress)
            self.debug("Command: hostname")
            result = ssh_1.execute("hostname")
            self.debug("Output: %s" % result)
            hostnames.append(result)

            ssh_2 = self.vm_2.get_ssh_client(
                ipaddress=self.public_ip_1.ipaddress.ipaddress,
                reconnect=True
            )
            self.debug("Command: hostname")
            result = ssh_2.execute("hostname")
            self.debug("Output: %s" % result)
            hostnames.append(result)
            self.debug("Hostnames: %s" % str(hostnames))

            if hostnames[0] == hostnames[1]:
                ssh_3 = self.vm_2.get_ssh_client(
                    ipaddress=self.public_ip_1.ipaddress.ipaddress,
                    reconnect=True
                )
                self.debug("Command: hostname")
                result = ssh_3.execute("hostname")
                self.debug("Output: %s" % result)
                hostnames.append(result)

                self.assertNotEqual(
                    hostnames[0],
                    hostnames[2],
                    "Both request should be served by different instances"
                )
            else:
                self.assertNotEqual(
                    hostnames[0],
                    hostnames[1],
                    "Both request should be served by different instances"
                )
        except Exception as e:
            self.fail("Exception occurred during SSH: %s - %s" % (
                self.public_ip_1.ipaddress.ipaddress,
                e))
        self.debug("SSH into Netscaler to rules still persist")
        try:
            ssh_client = SshClient(
                self.testdata["configurableData"]["netscaler"]["ipaddress"],
                self.testdata["configurableData"]["netscaler"]["port"],
                self.testdata["configurableData"]["netscaler"]["username"],
                self.testdata["configurableData"]["netscaler"]["password"],
            )

            cmd = "show server"
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertNotEqual(
                result.count(self.vm_2.ipaddress),
                0,
                "The server should be present in netscaler after reboot"
            )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.testdata["configurableData"]["netscaler"]["ipaddress"], e))
        return

    @attr(tags=["advancedns"])
    def test_06_destroy_user_vm(self):
        """Test destroy user VM with LB"""

        # Validate the following
        # 1. Create 2 instances and add these two for load balancing
        # 2. Destroy one of the user VM
        # 3. Until the time the Vm is in "Destroyed" state, the servies
        #    relating to this Vm will be marked as "Down".
        # 4. Once the Vm gets expunged, then the servers and testdata
        #    associated with this VM should get deleted and the LB rules
        #    should not be pointing to this Vm anymore.

        self.debug("Destroying VM instance: %s" % self.vm_2.name)
        self.vm_2.delete(self.apiclient)
        self.debug("Destroying VM: %s" % self.vm_2.name)

        try:
            self.debug(
                "Verifying request served by only running instances")
            hostnames = []
            ssh = self.vm_1.get_ssh_client(
                ipaddress=self.public_ip_1.ipaddress.ipaddress)
            self.debug("Command: hostname")
            result = ssh.execute("hostname")
            self.debug("Output: %s" % result)
            hostnames.append(result)

            ssh = self.vm_1.get_ssh_client(
                ipaddress=self.public_ip_1.ipaddress.ipaddress,
                reconnect=True
            )
            self.debug("Command: hostname")
            result = ssh.execute("hostname")
            self.debug("Output: %s" % result)
            hostnames.append(result)
            self.debug("Hostnames: %s" % str(hostnames))

            self.assertEqual(
                hostnames[0],
                hostnames[1],
                "Both request should be served by same instance"
            )
        except Exception as e:
            self.fail("Exception occurred during SSH: %s - %s" % (
                self.public_ip_1.ipaddress.ipaddress,
                e))
        delay = Configurations.list(
            self.apiclient,
            name='expunge.delay'
        )
        wait = Configurations.list(
            self.apiclient,
            name='expunge.interval'
        )
        # Sleep to ensure that all resources are deleted
        time.sleep(int(delay[0].value) + int(wait[0].value))
        self.debug("SSH into Netscaler to rules still persist")
        try:
            ssh_client = SshClient(
                self.testdata["configurableData"]["netscaler"]["ipaddress"],
                self.testdata["configurableData"]["netscaler"]["port"],
                self.testdata["configurableData"]["netscaler"]["username"],
                self.testdata["configurableData"]["netscaler"]["password"],
            )

            cmd = "show server"
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count(self.vm_2.ipaddress),
                0,
                "The server should not be present in netscaler after destroy"
            )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.testdata["configurableData"]["netscaler"]["ipaddress"], e))
        return

    @attr(tags=["advancedns"])
    def test_07_delete_all_public_ip(self):
        """Test delete all public Ip with LB rules"""

        # Validate the following
        # 1. Associate 2 public Ips and create load balancing rules in it
        # 2. Delete all of the public Ip
        # 3. All the LB rules should be removed from that public Ip
        # 4. In netscaler, make sure that all LB rules associated with that
        #    public Ip should get removed.
        # 5. All servers and testdata service to that public Ip get deleted

        self.debug("Deleting public IP: %s from network: %s" % (
            self.public_ip_1.ipaddress.ipaddress,
            self.network.name
        ))
        self.public_ip_1.delete(self.apiclient)
        self.debug(
            "Public Ip: %s is deleted!" %
            self.public_ip_1.ipaddress.ipaddress)
        lb_rules = LoadBalancerRule.list(
            self.apiclient,
            publicipid=self.public_ip_1.ipaddress.id,
            listall=True,
        )
        self.assertEqual(
            lb_rules,
            None,
            "LB rules associated with the public Ip should get deleted"
        )
        self.debug("SSH into Netscaler to verify other resources are deleted")
        try:
            ssh_client = SshClient(
                self.testdata["configurableData"]["netscaler"]["ipaddress"],
                self.testdata["configurableData"]["netscaler"]["port"],
                self.testdata["configurableData"]["netscaler"]["username"],
                self.testdata["configurableData"]["netscaler"]["password"],
            )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                self.public_ip_1.ipaddress.ipaddress,
                self.lb_rule_1.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count("ERROR: No such resource"),
                1,
                "Virtual server should get deleted after removing LB rule"
            )

            cmd = "show ip"
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count(self.public_ip_1.ipaddress.ipaddress),
                0,
                "Virtual server should get deleted after removing LB rule"
            )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.testdata["configurableData"]["netscaler"]["ipaddress"], e))
        return
