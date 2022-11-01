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
Tests of network permissions
"""

import logging

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase

from marvin.lib.base import (Account,
                             Configurations,
                             Domain,
                             Project,
                             ServiceOffering,
                             VirtualMachine,
                             Zone,
                             Network,
                             NetworkOffering,
                             NetworkPermission,
                             NIC,
                             PublicIPAddress,
                             LoadBalancerRule,
                             NATRule,
                             StaticNATRule,

                             SSHKeyPair)

from marvin.lib.common import (get_domain,
                               get_zone,
                               get_suitable_test_template)

NETWORK_FILTER_ACCOUNT = 'account'
NETWORK_FILTER_DOMAIN = 'domain'
NETWORK_FILTER_ACCOUNT_DOMAIN = 'accountdomain'
NETWORK_FILTER_SHARED = 'shared'
NETWORK_FILTER_ALL = 'all'

class TestNetworkPermissions(cloudstackTestCase):
    """
    Test user-shared networks
    """
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestNetworkPermissions,
            cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.zone = Zone(zone.__dict__)
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.template = get_suitable_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor)
        cls._cleanup = []

        cls.logger = logging.getLogger("TestNetworkPermissions")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        cls.domain = get_domain(cls.apiclient)

        # Create small service offering
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )
        cls._cleanup.append(cls.service_offering)

        # Create network offering for isolated networks
        cls.network_offering_isolated = NetworkOffering.create(
            cls.apiclient,
            cls.services["isolated_network_offering"]
        )
        cls.network_offering_isolated.update(cls.apiclient, state='Enabled')
        cls._cleanup.append(cls.network_offering_isolated)

        # Create sub-domain
        cls.sub_domain = Domain.create(
            cls.apiclient,
            cls.services["acl"]["domain1"]
        )
        cls._cleanup.append(cls.sub_domain)

        # Create domain admin and normal user
        cls.domain_admin = Account.create(
            cls.apiclient,
            cls.services["acl"]["accountD1A"],
            admin=True,
            domainid=cls.sub_domain.id
        )
        cls._cleanup.append(cls.domain_admin)

        cls.network_owner = Account.create(
            cls.apiclient,
            cls.services["acl"]["accountD11A"],
            domainid=cls.sub_domain.id
        )
        cls._cleanup.append(cls.network_owner)

        cls.other_user = Account.create(
            cls.apiclient,
            cls.services["acl"]["accountD11B"],
            domainid=cls.sub_domain.id
        )
        cls._cleanup.append(cls.other_user)

        # Create project
        cls.project = Project.create(
          cls.apiclient,
          cls.services["project"],
          account=cls.domain_admin.name,
          domainid=cls.domain_admin.domainid
        )
        cls._cleanup.append(cls.project)

        # Create api clients for domain admin and normal user
        cls.domainadmin_user = cls.domain_admin.user[0]
        cls.domainadmin_apiclient = cls.testClient.getUserApiClient(
            cls.domainadmin_user.username, cls.sub_domain.name
        )
        cls.networkowner_user = cls.network_owner.user[0]
        cls.user_apiclient = cls.testClient.getUserApiClient(
            cls.networkowner_user.username, cls.sub_domain.name
        )

        cls.otheruser_user = cls.other_user.user[0]
        cls.otheruser_apiclient = cls.testClient.getUserApiClient(
            cls.otheruser_user.username, cls.sub_domain.name
        )

        # Create networks for domain admin, normal user and project
        cls.services["network"]["name"] = "Test Network Isolated - Project"
        cls.project_network = Network.create(
            cls.apiclient,
            cls.services["network"],
            networkofferingid=cls.network_offering_isolated.id,
            domainid=cls.sub_domain.id,
            projectid=cls.project.id,
            zoneid=cls.zone.id
        )
        cls._cleanup.append(cls.project_network)

        cls.services["network"]["name"] = "Test Network Isolated - Domain admin"
        cls.domainadmin_network = Network.create(
            cls.apiclient,
            cls.services["network"],
            networkofferingid=cls.network_offering_isolated.id,
            domainid=cls.sub_domain.id,
            accountid=cls.domain_admin.name,
            zoneid=cls.zone.id
        )
        cls._cleanup.append(cls.domainadmin_network)

        cls.services["network"]["name"] = "Test Network Isolated - Normal user"
        cls.user_network = Network.create(
            cls.apiclient,
            cls.services["network"],
            networkofferingid=cls.network_offering_isolated.id,
            domainid=cls.sub_domain.id,
            accountid=cls.network_owner.name,
            zoneid=cls.zone.id
        )
        cls._cleanup.append(cls.user_network)

    @classmethod
    def tearDownClass(cls):
        super(TestNetworkPermissions, cls).tearDownClass()

    def setUp(self):
        self.cleanup = []
        self.virtual_machine = None

    def tearDown(self):
        super(TestNetworkPermissions, self).tearDown()

    def list_network(self, apiclient, account, network, project, network_filter=None, expected=True):
        # List networks by apiclient, account, network, project and network network_filter
        # If account is specified, list the networks which can be used by the domain (canusefordeploy=true,listall=false)
        # otherwise canusefordeploy is None and listall is True.
        domain_id = None
        account_name = None
        project_id = None
        canusefordeploy = None
        list_all = True
        if account:
            domain_id = account.domainid
            account_name = account.name
            canusefordeploy = True
            list_all = False
        if project:
            project_id = project.id
        networks = None
        try:
            networks = Network.list(
                apiclient,
                canusefordeploy=canusefordeploy,
                listall=list_all,
                networkfilter= network_filter,
                domainid=domain_id,
                account=account_name,
                projectid=project_id,
                id=network.id
            )
            if isinstance(networks, list) and len(networks) > 0:
                if not expected:
                    self.fail("Found the network, but expected to fail")
            elif expected:
                self.fail("Failed to find the network, but expected to succeed")
        except Exception as ex:
            networks = None
            if expected:
                self.fail(f"Failed to list network, but expected to succeed : {ex}")
        if networks and not expected:
            self.fail("network is listed successfully, but expected to fail")

    def list_network_by_filters(self, apiclient, account, network, project, expected_results=None):
        # expected results in order: account/domain/accountdomain/shared/all
        self.list_network(apiclient, account, network, project, NETWORK_FILTER_ACCOUNT, expected_results[0])
        self.list_network(apiclient, account, network, project, NETWORK_FILTER_DOMAIN, expected_results[1])
        self.list_network(apiclient, account, network, project, NETWORK_FILTER_ACCOUNT_DOMAIN, expected_results[2])
        self.list_network(apiclient, account, network, project, NETWORK_FILTER_SHARED, expected_results[3])
        self.list_network(apiclient, account, network, project, NETWORK_FILTER_ALL, expected_results[4])

    def create_network_permission(self, apiclient, network, account, project, expected=True):
        account_id = None
        project_id = None
        if account:
            account_id = account.id
        if project:
            project_id = project.id
        result = True
        try:
            NetworkPermission.create(
                apiclient,
                networkid=network.id,
                accountids=account_id,
                projectids=project_id
            )
        except Exception as ex:
            result = False
            if expected:
                self.fail(f"Failed to create network permissions, but expected to succeed : {ex}")
        if result and not expected:
            self.fail("network permission is created successfully, but expected to fail")

    def remove_network_permission(self, apiclient, network, account, project, expected=True):
        account_id = None
        project_id = None
        if account:
            account_id = account.id
        if project:
            project_id = project.id
        result = True
        try:
            NetworkPermission.remove(
                apiclient,
                networkid=network.id,
                accountids=account_id,
                projectids=project_id
            )
        except Exception as ex:
            result = False
            if expected:
                self.fail(f"Failed to remove network permissions, but expected to succeed : {ex}")
        if result and not expected:
            self.fail("network permission is removed successfully, but expected to fail")

    def reset_network_permission(self, apiclient, network, expected=True):
        result = True
        try:
            NetworkPermission.reset(
                apiclient,
                networkid=network.id
            )
        except Exception as ex:
            result = False
            if expected:
                self.fail(f"Failed to reset network permissions, but expected to succeed : {ex}")
        if result and not expected:
            self.fail("network permission is reset successfully, but expected to fail")

    def exec_command(self, apiclient_str, command, expected=None):
        result = True
        try:
            command = command.format(apiclient = apiclient_str)
            exec(command)
        except Exception as ex:
            result = False
            if expected:
                self.fail(f"Failed to execute command '{command}' with exception : {ex}")
        if result and expected is False:
            self.fail(f"command {command} is executed successfully, but expected to fail")
        if expected is None:
            # if expected is None, display the command and result
            self.logger.info(f"Result of command '{command}' : {result}")
        return result

    @attr(tags=["advanced"], required_hardware="false")
    def test_01_network_permission_on_project_network(self):
        """ Testing network permissions on project network """

        self.create_network_permission(self.apiclient, self.project_network, self.domain_admin, None, expected=False)
        self.create_network_permission(self.domainadmin_apiclient, self.project_network, self.domain_admin, None, expected=False)
        self.create_network_permission(self.user_apiclient, self.project_network, self.network_owner, None, expected=False)

    @attr(tags=["advanced"], required_hardware="false")
    def test_02_network_permission_on_user_network(self):
        """ Testing network permissions on user network """

        # List user network by domain admin
        self.list_network_by_filters(self.domainadmin_apiclient, None, self.user_network, None, [True, False, True, False, True])
        self.list_network_by_filters(self.domainadmin_apiclient, self.domain_admin, self.user_network, None, [False, False, False, False, False])

        # Create network permissions
        self.create_network_permission(self.apiclient, self.user_network, self.domain_admin, None, expected=True)
        self.create_network_permission(self.domainadmin_apiclient, self.user_network, self.domain_admin, None, expected=True)
        self.create_network_permission(self.user_apiclient, self.user_network, self.network_owner, None, expected=True)
        self.create_network_permission(self.user_apiclient, self.user_network, self.other_user, None, expected=True)
        self.create_network_permission(self.user_apiclient, self.user_network, None, self.project, expected=False)
        self.create_network_permission(self.domainadmin_apiclient, self.user_network, None, self.project, expected=True)
        self.create_network_permission(self.otheruser_apiclient, self.user_network, self.network_owner, None, expected=False)

        # List domain admin network by domain admin
        self.list_network_by_filters(self.domainadmin_apiclient, None, self.domainadmin_network, None, [True, False, True, False, True])
        self.list_network_by_filters(self.domainadmin_apiclient, self.domain_admin, self.domainadmin_network, None, [True, False, True, False, True])
        # List user network by domain admin
        self.list_network_by_filters(self.domainadmin_apiclient, None, self.user_network, None, [True, False, True, True, True])
        self.list_network_by_filters(self.domainadmin_apiclient, self.domain_admin, self.user_network, None, [False, False, False, True, True])
        # List user network by user
        self.list_network_by_filters(self.user_apiclient, None, self.user_network, None, [True, False, True, False, True])
        self.list_network_by_filters(self.user_apiclient, self.network_owner, self.user_network, None, [True, False, True, False, True])
        # List user network by other user
        self.list_network_by_filters(self.otheruser_apiclient, None, self.user_network, None, [False, False, False, True, True])
        self.list_network_by_filters(self.otheruser_apiclient, self.network_owner, self.user_network, None, [False, False, False, False, False])

        # Remove network permissions
        self.remove_network_permission(self.domainadmin_apiclient, self.user_network, self.domain_admin, None, expected=True)
        # List user network by domain admin
        self.list_network_by_filters(self.domainadmin_apiclient, None, self.user_network, None, [True, False, True, True, True])
        self.list_network_by_filters(self.domainadmin_apiclient, self.domain_admin, self.user_network, None, [False, False, False, False, False])

        # Reset network permissions
        self.reset_network_permission(self.domainadmin_apiclient, self.user_network, expected=True)
        # List user network by domain admin
        self.list_network_by_filters(self.domainadmin_apiclient, None, self.user_network, None, [True, False, True, False, True])
        self.list_network_by_filters(self.domainadmin_apiclient, self.domain_admin, self.user_network, None, [False, False, False, False, False])

    @attr(tags=["advanced"], required_hardware="false")
    def test_03_network_operations_on_created_vm_of_otheruser(self):
        """ Testing network operations on a create vm owned by other user"""

        # 1. Create an Isolated network by other user
        self.services["network"]["name"] = "Test Network Isolated - Other user"
        otheruser_network = Network.create(
            self.otheruser_apiclient,
            self.services["network"],
            networkofferingid=self.network_offering_isolated.id,
            zoneid=self.zone.id
        )
        self.cleanup.append(otheruser_network)

        # 2. Deploy vm1 on other user's network
        self.virtual_machine = VirtualMachine.create(
            self.otheruser_apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.service_offering.id,
            networkids=otheruser_network.id,
            zoneid=self.zone.id
        )

        # 3. Add user network to vm1, should fail by vm owner and network owner
        command = """self.virtual_machine.add_nic({apiclient}, self.user_network.id)"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=False)

        # 4. Create network permission for other user, should succeed by network owner
        command = """self.create_network_permission({apiclient}, self.user_network, self.other_user, None, expected=True)"""
        self.exec_command("self.otheruser_apiclient", command, expected=False)
        self.exec_command("self.user_apiclient", command, expected=True)

        # 5. Add user network to vm1, should succeed by vm owner
        command = """self.virtual_machine.add_nic({apiclient}, self.user_network.id)"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=True)

        # 6. Stop vm1 with forced=true, should succeed by vm owner
        command = """self.virtual_machine.stop({apiclient}, forced=True)"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=True)

        # Get id of the additional nic
        list_vms = VirtualMachine.list(
            self.otheruser_apiclient,
            id = self.virtual_machine.id
        )
        self.assertEqual(
            isinstance(list_vms, list),
            True,
            "Check if virtual machine is present"
        )
        self.assertEqual(
            len(list_vms) > 0,
            True,
            "Check if virtual machine list is empty"
        )
        self.vm_default_nic_id = None
        self.vm_new_nic_id = None
        for vm_nic in list_vms[0].nic:
            if vm_nic.networkid == self.user_network.id:
                self.vm_new_nic_id = vm_nic.id
            else:
                self.vm_default_nic_id = vm_nic.id

        # 6. Update vm1 nic IP, should succeed by vm owner
        command = """NIC.updateIp({apiclient}, self.vm_new_nic_id)"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=True)

        # 7. Start vm1, should succeed by vm owner
        command = """self.virtual_machine.start({apiclient})"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=True)

        # 8. Add secondary IP to nic, should succeed by vm owner
        command = """self.secondaryip = NIC.addIp({apiclient}, self.vm_new_nic_id)"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=True)

        # 9 Remove secondary IP from nic, should succeed by vm owner
        command = """NIC.removeIp({apiclient}, self.secondaryip.id)"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=True)

        # 10. Update default NIC, should succeed by vm owner
        command = """self.virtual_machine.update_default_nic({apiclient}, self.vm_new_nic_id)"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=True)

        command = """self.virtual_machine.update_default_nic({apiclient}, self.vm_default_nic_id)"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=True)

        # 11. Stop vm1 with forced=true
        command = """self.virtual_machine.stop({apiclient}, forced=True)"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=True)

        # 12. Remove nic from vm1, should succeed by vm owner
        command = """self.virtual_machine.remove_nic({apiclient}, self.vm_new_nic_id)"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=True)

        # 13. Test operations by domain admin
        command = """self.virtual_machine.add_nic({apiclient}, self.user_network.id)"""
        self.exec_command("self.domainadmin_apiclient", command, expected=True)

        list_vms = VirtualMachine.list(
            self.otheruser_apiclient,
            id = self.virtual_machine.id
        )

        self.vm_default_nic_id = None
        self.vm_new_nic_id = None
        for vm_nic in list_vms[0].nic:
            if vm_nic.networkid == self.user_network.id:
                self.vm_new_nic_id = vm_nic.id
            else:
                self.vm_default_nic_id = vm_nic.id

        command = """NIC.updateIp({apiclient}, self.vm_new_nic_id)"""
        self.exec_command("self.domainadmin_apiclient", command, expected=True)

        command = """self.secondaryip = NIC.addIp({apiclient}, self.vm_new_nic_id)"""
        self.exec_command("self.domainadmin_apiclient", command, expected=True)

        command = """NIC.removeIp({apiclient}, self.secondaryip.id)"""
        self.exec_command("self.domainadmin_apiclient", command, expected=True)

        command = """self.virtual_machine.update_default_nic({apiclient}, self.vm_new_nic_id)"""
        self.exec_command("self.domainadmin_apiclient", command, expected=True)

        command = """self.virtual_machine.update_default_nic({apiclient}, self.vm_default_nic_id)"""
        self.exec_command("self.domainadmin_apiclient", command, expected=True)

        command = """self.virtual_machine.remove_nic({apiclient}, self.vm_new_nic_id)"""
        self.exec_command("self.domainadmin_apiclient", command, expected=True)

        # 14. Test operations by vm owner, when network permission is removed
        command = """self.virtual_machine.add_nic({apiclient}, self.user_network.id)"""
        self.exec_command("self.domainadmin_apiclient", command, expected=True)

        # 15. Reset network permissions, should succeed by network owner
        command = """self.reset_network_permission({apiclient}, self.user_network, expected=True)"""
        self.exec_command("self.otheruser_apiclient", command, expected=False)
        self.exec_command("self.user_apiclient", command, expected=True)

        list_vms = VirtualMachine.list(
            self.otheruser_apiclient,
            id = self.virtual_machine.id
        )

        self.vm_default_nic_id = None
        self.vm_new_nic_id = None
        for vm_nic in list_vms[0].nic:
            if vm_nic.networkid == self.user_network.id:
                self.vm_new_nic_id = vm_nic.id
            else:
                self.vm_default_nic_id = vm_nic.id

        command = """NIC.updateIp({apiclient}, self.vm_new_nic_id)"""
        self.exec_command("self.otheruser_apiclient", command, expected=True)

        command = """self.secondaryip = NIC.addIp({apiclient}, self.vm_new_nic_id)"""
        if self.exec_command("self.otheruser_apiclient", command, expected=True):
            command = """NIC.removeIp({apiclient}, self.secondaryip.id)"""
            self.exec_command("self.otheruser_apiclient", command, expected=True)

        command = """self.virtual_machine.update_default_nic({apiclient}, self.vm_new_nic_id)"""
        if self.exec_command("self.otheruser_apiclient", command, expected=True):
            command = """self.virtual_machine.update_default_nic({apiclient}, self.vm_default_nic_id)"""
            self.exec_command("self.otheruser_apiclient", command, expected=True)

        command = """self.virtual_machine.start({apiclient})"""
        if self.exec_command("self.otheruser_apiclient", command, expected=True):
            command = """self.virtual_machine.stop({apiclient}, forced=True)"""
            self.exec_command("self.otheruser_apiclient", command, expected=True)

        command = """self.virtual_machine.remove_nic({apiclient}, self.vm_new_nic_id)"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=True)
        #self.exec_command("self.domainadmin_apiclient", command, expected=True)

        # 16. Destroy vm1, should succeed by root admin
        self.virtual_machine.delete(self.apiclient, expunge=True)

    @attr(tags=["advanced"], required_hardware="false")
    def test_04_deploy_vm_for_other_user_and_test_vm_operations(self):
        """ Deploy VM for other user and test VM operations by vm owner, network owner and domain admin"""

        # 1. Create network permission for other user, by user
        command = """self.create_network_permission({apiclient}, self.user_network, self.other_user, None, expected=True)"""
        self.exec_command("self.otheruser_apiclient", command, expected=False)
        self.exec_command("self.user_apiclient", command, expected=True)

        # 2. Deploy vm2 on user network
        command = """self.virtual_machine = VirtualMachine.create(
                          {apiclient},
                          self.services["virtual_machine"],
                          templateid=self.template.id,
                          serviceofferingid=self.service_offering.id,
                          networkids=self.user_network.id,
                          accountid=self.other_user.name,
                          domainid=self.other_user.domainid,
                          zoneid=self.zone.id
                      )"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=True)

        if not self.virtual_machine:
            self.fail("Failed to find self.virtual_machine")

        # 3. List vm2
        list_vms = VirtualMachine.list(
            self.user_apiclient,
            id = self.virtual_machine.id
        )
        self.assertEqual(
            isinstance(list_vms, list) and len(list_vms) > 0,
            False,
            "Check if virtual machine is not present"
        )
        list_vms = VirtualMachine.list(
            self.otheruser_apiclient,
            id = self.virtual_machine.id
        )
        self.assertEqual(
            isinstance(list_vms, list) and len(list_vms) > 0,
            True,
            "Check if virtual machine is present"
        )

        # 4. Stop vm2 with forced=true
        command = """self.virtual_machine.stop({apiclient}, forced=True)"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=True)

        # 5. Reset vm password
        if self.template.passwordenabled:
            command = """self.virtual_machine.resetPassword({apiclient})"""
            self.exec_command("self.user_apiclient", command, expected=False)
            self.exec_command("self.otheruser_apiclient", command, expected=True)

        # 6. Reset vm SSH key
        self.keypair = SSHKeyPair.create(
            self.otheruser_apiclient,
            name=self.other_user.name + ".pem"
        )
        command = """self.virtual_machine.resetSshKey({apiclient}, keypair=self.keypair.name)"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=True)

        # 7. Start vm2
        command = """self.virtual_machine.start({apiclient})"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=True)

        # 8. Acquire public IP, should succeed by domain admin and network owner
        command = """self.public_ip = PublicIPAddress.create(
                {apiclient},
                zoneid=self.zone.id,
                networkid=self.user_network.id
            )"""
        self.exec_command("self.otheruser_apiclient", command, expected=False)
        self.exec_command("self.user_apiclient", command, expected=True)
        #self.exec_command("self.domainadmin_apiclient", command, expected=True)

        # 9. Enable static nat, should succeed by domain admin
        command = """StaticNATRule.enable(
                {apiclient},
                ipaddressid=self.public_ip.ipaddress.id,
                virtualmachineid=self.virtual_machine.id
            )"""
        self.exec_command("self.otheruser_apiclient", command, expected=False)
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.domainadmin_apiclient", command, expected=True)

        # 10. Disable static nat, should succeed by domain admin and network owner
        command = """StaticNATRule.disable(
                {apiclient},
                ipaddressid=self.public_ip.ipaddress.id
            )"""
        self.exec_command("self.otheruser_apiclient", command, expected=False)
        self.exec_command("self.user_apiclient", command, expected=True)
        #self.exec_command("self.domainadmin_apiclient", command, expected=True)

        # 11. Create port forwarding rule, should succeed by domain admin
        command = """self.port_forwarding_rule = NATRule.create(
                {apiclient},
                virtual_machine=self.virtual_machine,
                services=self.services["natrule"],
                ipaddressid=self.public_ip.ipaddress.id,
            )"""
        self.exec_command("self.otheruser_apiclient", command, expected=False)
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.domainadmin_apiclient", command, expected=True)

        # 12. Delete port forwarding rule, should succeed by domain admin and network owner
        command = """self.port_forwarding_rule.delete({apiclient})"""
        self.exec_command("self.otheruser_apiclient", command, expected=False)
        self.exec_command("self.user_apiclient", command, expected=True)
        #self.exec_command("self.domainadmin_apiclient", command, expected=True)

        # 13. Create load balancer rule, should succeed by domain admin and network owner
        command = """self.load_balancer_rule = LoadBalancerRule.create(
                {apiclient},
                self.services["lbrule"],
                ipaddressid=self.public_ip.ipaddress.id,
                networkid=self.user_network.id,
            )"""
        self.exec_command("self.otheruser_apiclient", command, expected=False)
        self.exec_command("self.user_apiclient", command, expected=True)
        #self.exec_command("self.domainadmin_apiclient", command, expected=True)

        # 14. Assign virtual machine to load balancing rule, should succeed by domain admin
        command = """self.load_balancer_rule.assign({apiclient}, vms=[self.virtual_machine])"""
        self.exec_command("self.otheruser_apiclient", command, expected=False)
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.domainadmin_apiclient", command, expected=True)

        # 15. Remove virtual machine from load balancing rule, should succeed by domain admin and network owner
        command = """self.load_balancer_rule.remove({apiclient}, vms=[self.virtual_machine])"""
        self.exec_command("self.otheruser_apiclient", command, expected=False)
        self.exec_command("self.user_apiclient", command, expected=True)
        #self.exec_command("self.domainadmin_apiclient", command, expected=True)

        # 16. Delete load balancing rule, should succeed by domain admin and network owner
        command = """self.load_balancer_rule.delete({apiclient})"""
        self.exec_command("self.otheruser_apiclient", command, expected=False)
        self.exec_command("self.user_apiclient", command, expected=True)
        #self.exec_command("self.domainadmin_apiclient", command, expected=True)

        # 17. Release public IP, should succeed by domain admin and network owner
        command = """self.public_ip.delete({apiclient})"""
        self.exec_command("self.otheruser_apiclient", command, expected=False)
        self.exec_command("self.user_apiclient", command, expected=True)
        #self.exec_command("self.domainadmin_apiclient", command, expected=True)

        # 18. Stop vm2 with forced=true, should succeed by vm owner
        command = """self.virtual_machine.stop({apiclient}, forced=True)"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=True)

        # 19. Update vm2, should succeed by vm owner
        command = """self.virtual_machine.update({apiclient}, displayname = self.virtual_machine.displayname + ".new")"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=True)

        # 20. Restore vm2, should succeed by vm owner
        command = """self.virtual_machine.restore({apiclient})"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=True)

        # 21. Scale vm2 to another offering, should succeed by vm owner
        self.service_offering_new = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["big"]
        )
        self.cleanup.append(self.service_offering_new)
        command = """self.virtual_machine.scale_virtualmachine({apiclient}, self.service_offering_new.id)"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=True)

        # 22. Destroy vm2, should succeed by vm owner
        command = """self.virtual_machine.delete({apiclient}, expunge=False)"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=True)

        # 23. Recover vm2, should succeed by vm owner
        allow_expunge_recover_vm = Configurations.list(self.apiclient, name="allow.user.expunge.recover.vm")[0].value
        self.logger.debug("Global configuration allow.user.expunge.recover.vm = %s", allow_expunge_recover_vm)
        if allow_expunge_recover_vm == "true":
            command = """self.virtual_machine.recover({apiclient})"""
            self.exec_command("self.user_apiclient", command, expected=False)
            self.exec_command("self.otheruser_apiclient", command, expected=True)

        # 24. Destroy vm2, should succeed by vm owner
        command = """self.virtual_machine.delete({apiclient}, expunge=False)"""
        self.exec_command("self.user_apiclient", command, expected=False)
        self.exec_command("self.otheruser_apiclient", command, expected=True)

        # 25. Expunge vm2, should succeed by vm owner
        if allow_expunge_recover_vm == "true":
            command = """self.virtual_machine.expunge({apiclient})"""
            self.exec_command("self.user_apiclient", command, expected=False)
            self.exec_command("self.otheruser_apiclient", command, expected=True)
        else:
            self.virtual_machine.expunge(self.apiclient)

        # 26. Reset network permissions, should succeed by network owner
        command = """self.reset_network_permission({apiclient}, self.user_network, expected=True)"""
        self.exec_command("self.otheruser_apiclient", command, expected=False)
        self.exec_command("self.user_apiclient", command, expected=True)

    @attr(tags=["advanced"], required_hardware="false")
    def test_05_list_networks_under_project(self):
        """ Testing list networks under a project """
        self.create_network_permission(self.apiclient, self.user_network, self.domain_admin, self.project, expected=True)
        self.list_network(self.apiclient, self.domain_admin, self.user_network, self.project, None, expected=True)

        self.remove_network_permission(self.apiclient, self.user_network, self.domain_admin, self.project, expected=True)
        self.list_network(self.apiclient, self.domain_admin, self.user_network, self.project, None, expected=False)

    @attr(tags=["advanced"], required_hardware="false")
    def test_06_list_networks_under_account(self):
        """ Testing list networks under a domain admin account and user account """
        self.create_network_permission(self.apiclient, self.user_network, self.domain_admin, None, expected=True)
        self.list_network(self.apiclient, self.domain_admin, self.user_network, None, None, expected=True)
        self.list_network(self.domainadmin_apiclient, self.domain_admin, self.user_network, None, None, expected=True)
        self.list_network(self.user_apiclient, self.domain_admin, self.user_network, None, None, expected=False)

        self.remove_network_permission(self.apiclient, self.user_network, self.domain_admin, None, expected=True)
        self.list_network(self.apiclient, self.domain_admin, self.user_network, None, None, expected=False)
        self.list_network(self.domainadmin_apiclient, self.domain_admin, self.user_network, None, None, expected=False)

        self.create_network_permission(self.apiclient, self.user_network, self.other_user, None, expected=True)
        self.list_network(self.apiclient, self.other_user, self.user_network, None, None, expected=True)
        self.list_network(self.otheruser_apiclient, self.other_user, self.user_network, None, None, expected=True)

        self.remove_network_permission(self.apiclient, self.user_network, self.other_user, None, expected=True)
        self.list_network(self.apiclient, self.other_user, self.user_network, None, None, expected=False)
        self.list_network(self.otheruser_apiclient, self.other_user, self.user_network, None, None, expected=False)