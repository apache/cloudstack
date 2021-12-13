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
from marvin.lib.utils import cleanup_resources

from marvin.lib.base import (Account,
                             Domain,
                             Project,
                             ServiceOffering,
                             VirtualMachine,
                             Zone,
                             Network,
                             NetworkOffering,
                             NetworkPermission)

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
        cls.admin_apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        zone = get_zone(cls.admin_apiclient, cls.testClient.getZoneForTests())
        cls.zone = Zone(zone.__dict__)
        cls.template = get_template(cls.admin_apiclient, cls.zone.id)
        cls._cleanup = []

        cls.logger = logging.getLogger("TestNetworkPermissions")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        cls.domain = get_domain(cls.admin_apiclient)

        # Create sub-domain
        cls.sub_domain = Domain.create(
            cls.admin_apiclient,
            cls.services["acl"]["domain1"]
        )

        # Create domain admin and normal user
        cls.domain_admin = Account.create(
            cls.admin_apiclient,
            cls.services["acl"]["accountD1A"],
            admin=True,
            domainid=cls.sub_domain.id
        )
        cls.normal_user = Account.create(
            cls.admin_apiclient,
            cls.services["acl"]["accountD1B"],
            domainid=cls.sub_domain.id
        )
        # Create project
        cls.project = Project.create(
          cls.admin_apiclient,
          cls.services["project"],
          account=cls.domain_admin.name,
          domainid=cls.domain_admin.domainid
        )
        cls._cleanup.append(cls.project)
        cls._cleanup.append(cls.domain_admin)
        cls._cleanup.append(cls.normal_user)
        cls._cleanup.append(cls.sub_domain)

        # Create small service offering
        cls.service_offering = ServiceOffering.create(
            cls.admin_apiclient,
            cls.services["service_offerings"]["small"]
        )
        cls._cleanup.append(cls.service_offering)

        # Create network offering for isolated networks
        cls.network_offering_isolated = NetworkOffering.create(
            cls.admin_apiclient,
            cls.services["network_offering"]
        )
        cls.network_offering_isolated.update(cls.admin_apiclient, state='Enabled')
        cls._cleanup.append(cls.network_offering_isolated)

        # Create api clients for domain admin and normal user
        cls.domainadmin_user = cls.domain_admin.user[0]
        cls.domainadmin_apiclient = cls.testClient.getUserApiClient(
            cls.domainadmin_user.username, cls.sub_domain.name
        )
        cls.normaluser_user = cls.normal_user.user[0]
        cls.normaluser_apiclient = cls.testClient.getUserApiClient(
            cls.normaluser_user.username, cls.sub_domain.name
        )

        # Create networks for domain admin, normal user and project
        cls.services["network"]["name"] = "Test Network Isolated - Project"
        cls.project_network = Network.create(
            cls.admin_apiclient,
            cls.services["network"],
            networkofferingid=cls.network_offering_isolated.id,
            domainid=cls.sub_domain.id,
            projectid=cls.project.id,
            zoneid=cls.zone.id
        )

        cls.services["network"]["name"] = "Test Network Isolated - Domain admin"
        cls.domainadmin_network = Network.create(
            cls.admin_apiclient,
            cls.services["network"],
            networkofferingid=cls.network_offering_isolated.id,
            domainid=cls.sub_domain.id,
            accountid=cls.domain_admin.name,
            zoneid=cls.zone.id
        )

        cls.services["network"]["name"] = "Test Network Isolated - Normal user"
        cls.user_network = Network.create(
            cls.admin_apiclient,
            cls.services["network"],
            networkofferingid=cls.network_offering_isolated.id,
            domainid=cls.sub_domain.id,
            accountid=cls.normal_user.name,
            zoneid=cls.zone.id
        )

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.admin_apiclient, cls._cleanup)
        except Exception as ex:
            raise Exception(f"Warning: Exception during cleanup : {ex}") from ex

    def setUp(self):
        self.cleanup = []

    def tearDown(self):
        try:
            cleanup_resources(self.admin_apiclient, self.cleanup)
        except Exception as ex:
            raise Exception(f"Warning: Exception during cleanup : {ex}") from ex

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

        self.create_network_permission(self.admin_apiclient, self.project_network, self.domain_admin, None, expected=False)
        self.create_network_permission(self.domainadmin_apiclient, self.project_network, self.domain_admin, None, expected=False)
        self.create_network_permission(self.normaluser_apiclient, self.project_network, self.normal_user, None, expected=False)

    @attr(tags=["advanced"], required_hardware="false")
    def test_02_network_permission_on_user_network(self):
        """ Testing network permissions on user network """

        # List user network by domain admin
        self.list_network_by_filters(self.domainadmin_apiclient, None, self.user_network, None, [True, False, True, False, True])
        self.list_network_by_filters(self.domainadmin_apiclient, self.domain_admin, self.user_network, None, [False, False, False, False, False])

        # Create network permissions
        self.create_network_permission(self.admin_apiclient, self.user_network, self.domain_admin, None, expected=True)
        self.create_network_permission(self.domainadmin_apiclient, self.user_network, self.domain_admin, None, expected=True)
        self.create_network_permission(self.normaluser_apiclient, self.user_network, self.normal_user, None, expected=True)
        self.create_network_permission(self.normaluser_apiclient, self.user_network, None, self.project, expected=False)
        self.create_network_permission(self.domainadmin_apiclient, self.user_network, None, self.project, expected=True)

        # List domain admin network by domain admin
        self.list_network_by_filters(self.domainadmin_apiclient, None, self.domainadmin_network, None, [True, False, True, False, True])
        self.list_network_by_filters(self.domainadmin_apiclient, self.domain_admin, self.domainadmin_network, None, [True, False, True, False, True])
        # List user network by domain admin
        self.list_network_by_filters(self.domainadmin_apiclient, None, self.user_network, None, [True, False, True, True, True])
        self.list_network_by_filters(self.domainadmin_apiclient, self.domain_admin, self.user_network, None, [False, False, False, True, True])
        # List user network by user
        self.list_network_by_filters(self.normaluser_apiclient, None, self.user_network, None, [True, False, True, False, True])
        self.list_network_by_filters(self.normaluser_apiclient, self.normal_user, self.user_network, None, [True, False, True, False, True])

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
