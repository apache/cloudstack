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

# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.codes import PASS
from marvin.lib.base import (PublicIPAddress,
                             NetworkOffering,
                             Network,
                             VirtualMachine,
                             VpcOffering,
                             NATRule,
                             Account,
                             ServiceOffering)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_test_template)
from marvin.lib.utils import validateList, cleanup_resources
from nose.plugins.attrib import attr


class TestPortForwardingRules(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(TestPortForwardingRules, cls).getClsTestClient()
            cls.api_client = cls.testClient.getApiClient()
            cls.services = cls.testClient.getParsedTestDataConfig()
            cls.hypervisor = cls.testClient.getHypervisorInfo()
            # Get Domain, Zone, Template
            cls.domain = get_domain(cls.api_client)
            cls.zone = get_zone(
                cls.api_client,
                cls.testClient.getZoneForTests())
            cls.template = get_test_template(
                cls.api_client,
                cls.zone.id,
                cls.hypervisor
            )
            if cls.zone.localstorageenabled:
                cls.storagetype = 'local'
                cls.services["service_offerings"][
                    "tiny"]["storagetype"] = 'local'
            else:
                cls.storagetype = 'shared'
                cls.services["service_offerings"][
                    "tiny"]["storagetype"] = 'shared'

            cls.services['mode'] = cls.zone.networktype
            cls.services["virtual_machine"][
                "hypervisor"] = cls.hypervisor
            cls.services["virtual_machine"]["zoneid"] = cls.zone.id
            cls.services["virtual_machine"]["template"] = cls.template.id
            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.services["service_offerings"]["tiny"]
            )
            cls._cleanup.append(cls.service_offering)
            cls.services['mode'] = cls.zone.networktype

            cls.vpc_offering = VpcOffering.create(cls.api_client,
                                                  cls.services["vpc_offering"]
                                                  )
            cls.vpc_offering.update(cls.api_client, state='Enabled')
            cls._cleanup.append(cls.vpc_offering)

        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setup : %s" % e)
        return

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()
        self.cleanup = []
        self.account = Account.create(
            self.apiClient,
            self.services["account"],
            domainid=self.domain.id
        )
        # Getting authentication for user in newly created Account
        self.user = self.account.user[0]
        self.userapiclient = self.testClient.getUserApiClient(
            self.user.username,
            self.domain.name)

    def tearDown(self):
        # Clean up, terminate the created volumes
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def __verify_values(self, expected_vals, actual_vals):
        """
        @summary: Function to verify expected and actual values
        Step1: Initializing return flag to True
        Step1: Verifying length of expected and actual dictionaries is matching
               If not matching returning false
        Step2: Listing all the keys from expected dictionary
        Step3: Looping through each key from step2 and verifying expected
               and actual dictionaries have same value
               If not making return flag to False
        Step4: returning the return flag after all the values are verified
        """
        return_flag = True

        if len(expected_vals) != len(actual_vals):
            return False

        keys = list(expected_vals.keys())
        for i in range(0, len(expected_vals)):
            exp_val = expected_vals[keys[i]]
            act_val = actual_vals[keys[i]]
            if exp_val == act_val:
                return_flag = return_flag and True
            else:
                return_flag = return_flag and False
                self.debug(
                    "expected Value: %s, is not matching with actual value: %s"
                    %
                    (exp_val, act_val))
        return return_flag

    @attr(tags=["advanced"], required_hardware="true")
    def test_01_create_delete_portforwarding_fornonvpc(self):
        """
        @summary: Test to list, create and delete Port Forwarding for
        IP Address associated to Non VPC network
        @Steps:
        Step1: Creating a Network for the user
        Step2: Associating an IP Addresses for Network
        Step3: Launching Virtual Machine in network created in step 2
        Step4: Listing Port Forwarding Rules for the IP Address associated
               in Step2
        Step5: Verifying that no Port Forwarding Rules are listed
        Step6: Creating a Port Forwarding Rule for IP Address associated in
               Step2
        Step7: Listing Port Forwarding Rules for the IP Address associated in
               Step2
        Step8: Verifying 1 Port Forwarding Rule is listed
        Step9: Deleting the Port Forwarding Rule created in Step6
        Step10: Listing Port Forwarding Rules for the IP Address associated in
               Step2
        Step11: Verifying that no Port Forwarding Rules are listed
        """
        # Listing all the Networks's for a user
        list_networks_before = Network.list(
            self.userapiclient,
            listall=self.services["listall"],
            type="Isolated"
        )
        # Verifying No Networks are listed
        self.assertIsNone(
            list_networks_before,
            "Networks listed for newly created User"
        )
        # Listing Network Offerings
        network_offerings_list = NetworkOffering.list(
            self.apiClient,
            forvpc="false",
            guestiptype="Isolated",
            state="Enabled",
            supportedservices="SourceNat,PortForwarding",
            zoneid=self.zone.id
        )
        status = validateList(network_offerings_list)
        self.assertEqual(
            PASS,
            status[0],
            "Isolated Network Offerings with sourceNat,\
                    PortForwarding enabled are not found"
        )
        # Creating a network
        network = Network.create(
            self.userapiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.domain.id,
            networkofferingid=network_offerings_list[0].id,
            zoneid=self.zone.id
        )

        self.assertIsNotNone(
            network,
            "Network creation failed"
        )
        # Listing all the IP Addresses for a user
        list_ipaddresses_before = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        # Verifying no IP Addresses are listed
        self.assertIsNone(
            list_ipaddresses_before,
            "IP Addresses listed for newly created User"
        )

        service_offering = ServiceOffering.create(
            self.apiClient,
            self.services["service_offerings"]["tiny"],
        )

        self.services["virtual_machine"]["zoneid"] = self.zone.id

        vm = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=network.id,
            serviceofferingid=service_offering.id
        )

        VirtualMachine.delete(vm, self.apiClient, expunge=True)

        # Associating an IP Addresses to Network created
        associated_ipaddress = PublicIPAddress.create(
            self.userapiclient,
            services=self.services["network"],
            networkid=network.id
        )
        self.assertIsNotNone(
            associated_ipaddress,
            "Failed to Associate IP Address"
        )
        # Listing all the IP Addresses for a user
        list_ipaddresses_after = PublicIPAddress.list(
            self.userapiclient,
            listall=self.services["listall"]
        )
        status = validateList(list_ipaddresses_after)
        self.assertEqual(
            PASS,
            status[0],
            "IP Addresses Association Failed"
        )
        # Verifying the length of the list is 2
        self.assertEqual(
            2,
            len(list_ipaddresses_after),
            "Number of IP Addresses associated are not matching expected"
        )
        # Launching a Virtual Machine with above created Network
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=network.id,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "Failed to launch a VM under network created"
        )
        self.cleanup.append(network)
        # Listing Virtual Machines in running state in above created network
        list_vms_running = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            state="Running",
            networkid=network.id
        )
        status = validateList(list_vms_running)
        self.assertEqual(
            PASS,
            status[0],
            "VM Created is not in Running state"
        )
        # Verifying the length of the list is 2
        self.assertEqual(
            2,
            len(list_ipaddresses_after),
            "VM Created is not in Running state"
        )
        self.assertEqual(
            vm_created.id,
            list_vms_running[0].id,
            "VM Created is not in Running state"
        )
        # Listing Virtual Machines in stopped state in above created network
        list_vms_stopped = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            state="Stopped",
            networkid=network.id
        )
        # Verifying no VMs are in stopped state
        self.assertIsNone(
            list_vms_stopped,
            "VM Created is in stopped state"
        )
        # Listing Port Forwarding Rules for the IP Address associated
        list_prtfwdrule_before = NATRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            ipaddressid=associated_ipaddress.ipaddress.id
        )
        # Verifying no port forwarding rules are listed
        self.assertIsNone(
            list_prtfwdrule_before,
            "Port Forwarding Rules listed for newly associated IP Address"
        )
        # Creating a Port Forwarding rule
        portfwd_rule = NATRule.create(
            self.userapiclient,
            virtual_machine=vm_created,
            services=self.services["natrule"],
            ipaddressid=associated_ipaddress.ipaddress.id,
        )
        self.assertIsNotNone(
            portfwd_rule,
            "Failed to create Port Forwarding Rule"
        )
        # Verifying details of Sticky Policy created
        # Creating expected and actual values dictionaries
        expected_dict = {
            "ipaddressid": associated_ipaddress.ipaddress.id,
            "privateport": str(self.services["natrule"]["privateport"]),
            "publicport": str(self.services["natrule"]["publicport"]),
            "protocol": str(self.services["natrule"]["protocol"]).lower(),
        }
        actual_dict = {
            "ipaddressid": portfwd_rule.ipaddressid,
            "privateport": str(portfwd_rule.privateport),
            "publicport": str(portfwd_rule.publicport),
            "protocol": portfwd_rule.protocol,
        }
        portfwd_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            portfwd_status,
            "Created Port Forward Rule details are not as expected"
        )
        # Listing Port Forwarding Rules for the IP Address associated
        list_prtfwdrule_after = NATRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            ipaddressid=associated_ipaddress.ipaddress.id
        )
        status = validateList(list_prtfwdrule_after)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to create Port Forwarding Rule"
        )
        # Verifying the length of the list is 1
        self.assertEqual(
            1,
            len(list_prtfwdrule_after),
            "Failed to create Port Forwarding Rule"
        )
        # Deleting Port Forwarding Rule
        portfwd_rule.delete(self.userapiclient)

        # Creating a Port Forwarding rule with port range
        portfwd_rule = NATRule.create(
            self.userapiclient,
            virtual_machine=vm_created,
            services=self.services["natrulerange"],
            ipaddressid=associated_ipaddress.ipaddress.id,
        )
        self.assertIsNotNone(
            portfwd_rule,
            "Failed to create Port Forwarding Rule"
        )
        # update the private port for port forwarding rule
        updatefwd_rule = portfwd_rule.update(self.userapiclient,
                                             portfwd_rule.id,
                                             virtual_machine=vm_created,
                                             services=self.services["updatenatrulerange"],
                                             )

        # Verifying details of Sticky Policy created
        # Creating expected and actual values dictionaries
        expected_dict = {
            "privateport": str(self.services["updatenatrulerange"]["privateport"]),
            "privateendport": str(self.services["updatenatrulerange"]["privateendport"]),
        }
        actual_dict = {
            "privateport": str(updatefwd_rule.privateport),
            "privateendport": str(updatefwd_rule.privateendport),
        }
        portfwd_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            portfwd_status,
            "Updated Port Forward Rule details are not as expected"
        )
        # Deleting Port Forwarding Rule
        portfwd_rule.delete(self.userapiclient)
        # Listing Port Forwarding Rules for the IP Address associated
        list_prtfwdrule_after = NATRule.list(
            self.userapiclient,
            listall=self.services["listall"],
            ipaddressid=associated_ipaddress.ipaddress.id
        )
        # Verifying no port forwarding rules are listed
        self.assertIsNone(
            list_prtfwdrule_after,
            "Port Forwarding Rules listed after deletion"
        )
        # Destroying the VM Launched
        vm_created.delete(self.apiClient)
        self.cleanup.append(self.account)
        return
