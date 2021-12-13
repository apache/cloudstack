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
                             SSHKeyPair)

from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)

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
        cls.template = get_template(cls.apiclient, cls.zone.id)
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

        cls.services["network"]["name"] = "Test Network Isolated - Domain admin"
        cls.domainadmin_network = Network.create(
            cls.apiclient,
            cls.services["network"],
            networkofferingid=cls.network_offering_isolated.id,
            domainid=cls.sub_domain.id,
            accountid=cls.domain_admin.name,
            zoneid=cls.zone.id
        )

        cls.services["network"]["name"] = "Test Network Isolated - Normal user"
        cls.user_network = Network.create(
            cls.apiclient,
            cls.services["network"],
            networkofferingid=cls.network_offering_isolated.id,
            domainid=cls.sub_domain.id,
            accountid=cls.network_owner.name,
            zoneid=cls.zone.id
        )

    @classmethod
    def tearDownClass(cls):
        super().tearDownClass()

    def setUp(self):
        self.cleanup = []

    def tearDown(self):
        super().tearDown()

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
    def test_03_created_vm_network_operations_by_other_user(self):
        """ Testing network operations by other user"""

        # 1. Create network permission for other user, by user
        self.create_network_permission(self.user_apiclient, self.user_network, self.other_user, None, expected=True)

        # 2. Create an Isolated network by other user
        self.services["network"]["name"] = "Test Network Isolated - Other user"
        otheruser_network = Network.create(
            self.otheruser_apiclient,
            self.services["network"],
            networkofferingid=self.network_offering_isolated.id,
            zoneid=self.zone.id
        )
        self.cleanup = [otheruser_network]

        # 3. Deploy vm1 on other user's network
        virtual_machine = VirtualMachine.create(
            self.otheruser_apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.service_offering.id,
            networkids=otheruser_network.id,
            zoneid=self.zone.id
        )

        # 4. Add user network to vm1
        virtual_machine.add_nic(self.otheruser_apiclient, self.user_network.id)

        # 5. Stop vm1 with forced=true
        virtual_machine.stop(self.otheruser_apiclient, forced=True)

        # Get id of the additional nic
        list_vms = VirtualMachine.list(
            self.otheruser_apiclient,
            id = virtual_machine.id
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
        vm_default_nic_id = None
        vm_new_nic_id = None
        for vm_nic in list_vms[0].nic:
            if vm_nic.networkid == self.user_network.id:
                vm_new_nic_id = vm_nic.id
            else:
                vm_default_nic_id = vm_nic.id

        # 6. Update vm1 nic IP
        NIC.updateIp(self.otheruser_apiclient, vm_new_nic_id)

        # 7. Start vm1
        virtual_machine.start(self.otheruser_apiclient)

        # 8. Add secondary IP to nic
        secondaryip = NIC.addIp(self.otheruser_apiclient, vm_new_nic_id)

        # 9. Remove secondary IP from nic
        NIC.removeIp(self.otheruser_apiclient, secondaryip.id)

        # 10. Update default NIC
        virtual_machine.update_default_nic(self.otheruser_apiclient, vm_new_nic_id)
        virtual_machine.update_default_nic(self.otheruser_apiclient, vm_default_nic_id)

        # 11. Remove nic from vm1
        virtual_machine.remove_nic(self.otheruser_apiclient, vm_new_nic_id)

        # 12. Destroy vm1
        virtual_machine.delete(self.apiclient, expunge=True)

        # 13. Reset network permissions
        self.reset_network_permission(self.user_apiclient, self.user_network, expected=True)

    @attr(tags=["advanced"], required_hardware="false")
    def test_04_deploy_vm_and_vm_operations_by_other_user(self):
        """ Testing VM operations of VM on the network by other user"""

        # 1. Create network permission for other user, by user
        self.create_network_permission(self.user_apiclient, self.user_network, self.other_user, None, expected=True)

        # 2. Deploy vm2 on user network
        virtual_machine = VirtualMachine.create(
            self.otheruser_apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.service_offering.id,
            networkids=self.user_network.id,
            zoneid=self.zone.id
        )

        # 3. List vm2
        list_vms = VirtualMachine.list(
            self.otheruser_apiclient,
            id = virtual_machine.id
        )
        self.assertEqual(
            isinstance(list_vms, list),
            True,
            "Check if virtual machine is present"
        )

        # 4. Stop vm2 with forced=true
        virtual_machine.stop(self.otheruser_apiclient, forced=True)

        # 5. Reset vm password
        if self.template.passwordenabled:
            virtual_machine.resetPassword(self.otheruser_apiclient)

        # 6. Reset vm SSH key
        keypair = SSHKeyPair.create(
            self.otheruser_apiclient,
            name=self.other_user.name + ".pem"
        )
        virtual_machine.resetSshKey(self.otheruser_apiclient, keypair=keypair.name)

        # 7. Start vm2
        virtual_machine.start(self.otheruser_apiclient)

        # 8. Stop vm2 with forced=true
        virtual_machine.stop(self.otheruser_apiclient, forced=True)

        # 9. Update vm2
        virtual_machine.update(self.otheruser_apiclient, displayname = virtual_machine.displayname + ".new")

        # 10. Restore vm2
        virtual_machine.restore(self.otheruser_apiclient)

        # 11. Scale vm2 to another offering
        service_offering_new = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["big"]
        )
        self.cleanup = [service_offering_new]
        virtual_machine.scale_virtualmachine(self.otheruser_apiclient, service_offering_new.id)

        # 12. Destroy vm2
        virtual_machine.delete(self.otheruser_apiclient, expunge=False)

        # 13. Recover vm2
        allow_expunge_recover_vm = Configurations.list(self.apiclient, name="allow.user.expunge.recover.vm")[0].value
        self.logger.debug("Global configuration allow.user.expunge.recover.vm = %s", allow_expunge_recover_vm)
        if allow_expunge_recover_vm == "true":
            virtual_machine.recover(self.otheruser_apiclient)

        # 14. Destroy vm2
        virtual_machine.delete(self.apiclient, expunge=False)
        # 15. Expunge vm2
        if allow_expunge_recover_vm == "true":
            virtual_machine.expunge(self.otheruser_apiclient)
        else:
            virtual_machine.expunge(self.apiclient)

        # 16. Reset network permissions
        self.reset_network_permission(self.user_apiclient, self.user_network, expected=True)
