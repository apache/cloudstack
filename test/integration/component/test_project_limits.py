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
""" P1 tests for Resource limits
"""
#Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
#from marvin.cloudstackAPI import *
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.lib.base import (Account,
                             Domain,
                             Project,
                             Template,
                             Snapshot,
                             Volume,
                             NetworkOffering,
                             ServiceOffering,
                             VirtualMachine,
                             DiskOffering,
                             Configurations,
                             Network,
                             PublicIPAddress)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_snapshots,
                               list_volumes,
                               list_configurations,
                               list_resource_limits,
                               update_resource_limit,
                               get_builtin_template_info,
                               find_storage_pool_type)
from marvin.codes import PASS
import time

class Services:
    """Test Resource Limits Services
    """

    def __init__(self):
        self.services = {
                        "domain": {
                                   "name": "Domain",
                        },
                        "project": {
                                    "name": "Project",
                                    "displaytext": "Test project",
                        },
                        "account": {
                                    "email": "administrator@clogeny.com",
                                    "firstname": "Test",
                                    "lastname": "User",
                                    "username": "test",
                                    # Random characters are appended for unique
                                    # username
                                    "password": "password",
                         },
                         "user": {
                                    "email": "administrator@clogeny.com",
                                    "firstname": "User",
                                    "lastname": "User",
                                    "username": "User",
                                    # Random characters are appended for unique
                                    # username
                                    "password": "password",
                         },
                         "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 100, # in MHz
                                    "memory": 128, # In MBs
                        },
                        "disk_offering": {
                                    "displaytext": "Tiny Disk Offering",
                                    "name": "Tiny Disk Offering",
                                    "disksize": 1
                        },
                        "volume": {
                                   "diskname": "Test Volume",
                        },
                        "server": {
                                    "displayname": "TestVM",
                                    "username": "root",
                                    "password": "password",
                                    "ssh_port": 22,
                                    "hypervisor": 'XenServer',
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": 'TCP',
                        },
                        "template": {
                                    "displaytext": "Cent OS Template",
                                    "name": "Cent OS Template",
                                    "ostype": 'CentOS 5.3 (64-bit)',
                                    "templatefilter": 'self',
                        },
                         "network_offering": {
                                    "name": 'Network offering-VR services',
                                    "displaytext": 'Network offering-VR services',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Firewall,Lb,UserData,StaticNat',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "serviceProviderList": {
                                            "Dhcp": 'VirtualRouter',
                                            "Dns": 'VirtualRouter',
                                            "SourceNat": 'VirtualRouter',
                                            "PortForwarding": 'VirtualRouter',
                                            "Vpn": 'VirtualRouter',
                                            "Firewall": 'VirtualRouter',
                                            "Lb": 'VirtualRouter',
                                            "UserData": 'VirtualRouter',
                                            "StaticNat": 'VirtualRouter',
                                        },
                                    },
                         "network": {
                                  "name": "Test Network",
                                  "displaytext": "Test Network",
                                },
                         "ostype": 'CentOS 5.3 (64-bit)',
                        # Cent OS 5.3 (64 bit)
                        "sleep": 60,
                        "timeout": 10,
                    }


class TestProjectLimits(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestProjectLimits, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        # Create domains, account etc.
        cls.domain = Domain.create(
                                   cls.api_client,
                                   cls.services["domain"]
                                   )

        cls.admin = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain.id
                            )
        cls.user = Account.create(
                            cls.api_client,
                            cls.services["user"],
                            domainid=cls.domain.id
                            )
        cls.disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering"]
                                    )
        cls._cleanup = [
            cls.admin,
            cls.user,
            cls.domain,
            cls.disk_offering
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
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic", "sg", "eip", "advancedns", "simulator"], required_hardware="false")
    def test_01_project_limits(self):
        """ Test project limits for domain admin
        """
        # Validate the following
        # 1. Create a Project. Verify once projects are created, they inherit
        #    a default set of resource limits as configured by the Cloud Stack
        #    ROOT admin.
        # 2. Reduce Project resources limits. Verify limits can be reduced by
        #    the Project Owner of each project and project limit applies to
        #    number of virtual instances, disk volumes, snapshots, IP address.
        #    Also, verify resource limits for the project are independent of
        #    account resource limits
        # 3. Increase Projects Resources limits above domains limit. Verify
        #    project can't have more resources than domain level limit allows.
        # 4. Create Resource more than its set limit for the parent domain.
        #    Verify resource allocation should fail giving proper message

        # Create project as a domain admin
        project = Project.create(
                                 self.apiclient,
                                 self.services["project"],
                                 account=self.admin.name,
                                 domainid=self.admin.domainid
                                 )
        # Cleanup created project at end of test
        self.cleanup.append(project)
        self.debug("Created project with domain admin with ID: %s" %
                                                                project.id)

        list_projects_reponse = Project.list(
                                             self.apiclient,
                                             id=project.id,
                                             listall=True
                                             )

        self.assertEqual(
                            isinstance(list_projects_reponse, list),
                            True,
                            "Check for a valid list projects response"
                            )
        list_project = list_projects_reponse[0]

        self.assertNotEqual(
                    len(list_projects_reponse),
                    0,
                    "Check list project response returns a valid project"
                    )

        self.assertEqual(
                            project.name,
                            list_project.name,
                            "Check project name from list response"
                            )
        # Get the resource limits for ROOT domain
        resource_limits = list_resource_limits(self.apiclient)

        self.assertEqual(
                         isinstance(resource_limits, list),
                         True,
                         "List resource API should return a valid list"
                         )
        self.assertNotEqual(
                         len(resource_limits),
                         0,
                         "List resource API response should not be empty"
                         )

        # Reduce resource limits for project
        # Resource: 0 - Instance. Number of instances a user can create.
        # Resource: 1 - IP. Number of public IP addresses a user can own.
        # Resource: 2 - Volume. Number of disk volumes a user can create.
        # Resource: 3 - Snapshot. Number of snapshots a user can create.
        # Resource: 4 - Template. Number of templates that a user can
        #               register/create
        for resource in resource_limits:
            update_resource_limit(
                                    self.apiclient,
                                    resource.resourcetype,
                                    max=1,
                                    projectid=project.id
                                    )
            self.debug(
            "Updating resource (ID: %s) limit for project: %s" % (
                                                                  resource,
                                                                  project.id
                                                                  ))
        resource_limits = list_resource_limits(
                                                self.apiclient,
                                                projectid=project.id
                                                )
        self.assertEqual(
                         isinstance(resource_limits, list),
                         True,
                         "List resource API should return a valid list"
                         )
        self.assertNotEqual(
                         len(resource_limits),
                         0,
                         "List resource API response should not be empty"
                         )
        for resource in resource_limits:
            self.assertEqual(
                         resource.max,
                         1,
                         "Resource limit should be updated to 1"
                         )

        # Get the resource limits for domain
        resource_limits = list_resource_limits(
                                                self.apiclient,
                                                domainid=self.domain.id
                                                )
        self.assertEqual(
                         isinstance(resource_limits, list),
                         True,
                         "List resource API should return a valid list"
                         )
        self.assertNotEqual(
                         len(resource_limits),
                         0,
                         "List resource API response should not be empty"
                         )

        for resource in resource_limits:
            # Update domain resource limits to 2
            update_resource_limit(
                                        self.apiclient,
                                        resource.resourcetype,
                                        domainid=self.domain.id,
                                        max=1
                                      )
            max_value = 2
            self.debug(
                "Attempting to update project: %s resource limit to: %s" % (
                                                            project.id,
                                                            max_value
                                                            ))
            # Update project resource limits to 3
            update_resource_limit(
                                    self.apiclient,
                                    resource.resourcetype,
                                    max=max_value,
                                    projectid=project.id
                                  )

        # Verify project can't have more resources then limit set for domain by adding volumes.
        volume = Volume.create(
                          self.apiclient,
                          self.services["volume"],
                          zoneid=self.zone.id,
                          diskofferingid=self.disk_offering.id,
                          projectid=project.id
                        )
        # Exception should be raised for second volume
        with self.assertRaises(Exception):
            Volume.create(
                          self.apiclient,
                          self.services["volume"],
                          zoneid=self.zone.id,
                          diskofferingid=self.disk_offering.id,
                          projectid=project.id
                        )
        volume.delete(self.apiclient);

        return

    @attr(tags=["advanced", "basic", "sg", "eip", "advancedns", "simulator"], required_hardware="false")
    def test_02_project_limits_normal_user(self):
        """ Test project limits for normal user
        """
        # Validate the following
        # 1. Create a Project
        # 2. Reduce the projects limits as a domain admin. Verify resource
        #    count is updated
        # 3. Reduce the projects limits as a project user owner who is not a
        #    domain admin. Resource count should fail

        # Create project as a domain admin
        project = Project.create(
                                 self.apiclient,
                                 self.services["project"],
                                 account=self.admin.name,
                                 domainid=self.admin.domainid
                                 )
        # Cleanup created project at end of test
        self.cleanup.append(project)
        self.debug("Created project with domain admin with ID: %s" %
                                                                project.id)

        list_projects_reponse = Project.list(
                                             self.apiclient,
                                             id=project.id,
                                             listall=True
                                             )

        self.assertEqual(
                            isinstance(list_projects_reponse, list),
                            True,
                            "Check for a valid list projects response"
                            )
        list_project = list_projects_reponse[0]

        self.assertNotEqual(
                    len(list_projects_reponse),
                    0,
                    "Check list project response returns a valid project"
                    )

        self.assertEqual(
                            project.name,
                            list_project.name,
                            "Check project name from list response"
                            )
        # Get the resource limits for ROOT domain
        resource_limits = list_resource_limits(self.apiclient)

        self.assertEqual(
                         isinstance(resource_limits, list),
                         True,
                         "List resource API should return a valid list"
                         )
        self.assertNotEqual(
                         len(resource_limits),
                         0,
                         "List resource API response should not be empty"
                         )

        # Reduce resource limits for project
        # Resource: 0 - Instance. Number of instances a user can create.
        # Resource: 1 - IP. Number of public IP addresses a user can own.
        # Resource: 2 - Volume. Number of disk volumes a user can create.
        # Resource: 3 - Snapshot. Number of snapshots a user can create.
        # Resource: 4 - Template. Number of templates that a user can
        #               register/create
        for resource in resource_limits:
            update_resource_limit(
                                    self.apiclient,
                                    resource.resourcetype,
                                    max=1,
                                    projectid=project.id
                                    )
            self.debug(
            "Updating resource (ID: %s) limit for project: %s" % (
                                                                  resource,
                                                                  project.id
                                                                  ))
        resource_limits = list_resource_limits(
                                                self.apiclient,
                                                projectid=project.id
                                                )
        self.assertEqual(
                         isinstance(resource_limits, list),
                         True,
                         "List resource API should return a valid list"
                         )
        self.assertNotEqual(
                         len(resource_limits),
                         0,
                         "List resource API response should not be empty"
                         )
        for resource in resource_limits:
            self.assertEqual(
                         resource.max,
                         1,
                         "Resource limit should be updated to 1"
                         )

        self.debug("Adding %s user to project: %s" % (
                                                self.user.name,
                                                project.name
                                                ))

        # Add user to the project
        project.addAccount(
                           self.apiclient,
                           self.user.name,
                           )

        # Get the resource limits for domain
        resource_limits = list_resource_limits(
                                                self.apiclient,
                                                domainid=self.domain.id
                                                )
        self.assertEqual(
                         isinstance(resource_limits, list),
                         True,
                         "List resource API should return a valid list"
                         )
        self.assertNotEqual(
                         len(resource_limits),
                         0,
                         "List resource API response should not be empty"
                         )

        for resource in resource_limits:
            #with self.assertRaises(Exception):
            self.debug(
                    "Attempting to update resource limit by user: %s" % (
                                                        self.user.name
                                                        ))
            # Update project resource limits to 3
            update_resource_limit(
                                    self.apiclient,
                                    resource.resourcetype,
                                    account=self.user.name,
                                    domainid=self.user.domainid,
                                    max=3,
                                    projectid=project.id
                                )
        return


class TestResourceLimitsProject(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestResourceLimitsProject, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.services = Services().services
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["server"]["zoneid"] = cls.zone.id

        # Create Domains, Account etc
        cls.domain = Domain.create(
                                   cls.api_client,
                                   cls.services["domain"]
                                   )

        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )

        cls.userapiclient = cls.testClient.getUserApiClient(
                                    UserName=cls.account.name,
                                    DomainName=cls.account.domain)

        # Create project as a domain admin
        cls.project = Project.create(
                                 cls.api_client,
                                 cls.services["project"],
                                 account=cls.account.name,
                                 domainid=cls.account.domainid
                                 )
        cls.services["account"] = cls.account.name

        # Create Service offering and disk offerings etc
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering"]
                                    )
        cls._cleanup = [
                        cls.project,
                        cls.service_offering,
                        cls.disk_offering,
                        cls.account,
                        cls.domain
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
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic", "sg", "eip", "advancedns", "simulator"], required_hardware="false")
    def test_03_vm_per_project(self):
        """Test VM limit per project
        """
        # Validate the following
        # 1. Set max VM per project to 2
        # 2. Create account and start 2 VMs. Verify VM state is Up and Running
        # 3. Try to create 3rd VM instance. The appropriate error or alert
        #    should be raised

        self.debug(
            "Updating instance resource limits for project: %s" %
                                                        self.project.id)
        # Set usage_vm=1 for Account 1
        update_resource_limit(
                              self.apiclient,
                              0, # Instance
                              max=2,
                              projectid=self.project.id
                              )

        self.debug("Deploying VM for project: %s" % self.project.id)
        virtual_machine_1 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                serviceofferingid=self.service_offering.id,
                                projectid=self.project.id
                                )
        self.cleanup.append(virtual_machine_1)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_1.state,
                            'Running',
                            "Check VM state is Running or not"
                        )
        self.debug("Deploying VM for project: %s" % self.project.id)
        virtual_machine_2 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                serviceofferingid=self.service_offering.id,
                                projectid=self.project.id
                                )
        self.cleanup.append(virtual_machine_2)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_2.state,
                            'Running',
                            "Check VM state is Running or not"
                        )
        # Exception should be raised for second instance
        with self.assertRaises(Exception):
            VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                serviceofferingid=self.service_offering.id,
                                projectid=self.project.id
                                )
        return

    @attr(tags=["advanced", "eip", "advancedns", "simulator"], required_hardware="false")
    def test_04_publicip_per_project(self):
        """Test Public IP limit per project
        """
        # Validate the following
        # 1. set max no of IPs per project to 2.
        # 2. Create an account in this domain
        # 3. Create 1 VM in this domain
        # 4. Acquire 1 IP in the domain. IP should be successfully acquired
        # 5. Try to acquire 3rd IP in this domain. It should give the user an
        #    appropriate error and an alert should be generated.

        self.debug(
            "Updating public IP resource limits for project: %s" %
                                                            self.project.id)
        # Set usage_vm=1 for Account 1
        update_resource_limit(
                              self.apiclient,
                              1, # Public Ip
                              max=2,
                              projectid=self.project.id
                              )

        self.debug("Deploying VM for Project: %s" % self.project.id)
        virtual_machine_1 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                serviceofferingid=self.service_offering.id,
                                projectid=self.project.id
                                )
        self.cleanup.append(virtual_machine_1)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_1.state,
                            'Running',
                            "Check VM state is Running or not"
                        )
        networks = Network.list(
                                self.apiclient,
                                projectid=self.project.id,
                                listall=True
                                )
        self.assertEqual(
                    isinstance(networks, list),
                    True,
                    "Check list networks response returns a valid response"
                    )
        self.assertNotEqual(
                    len(networks),
                    0,
                    "Check list networks response returns a valid network"
                    )
        network = networks[0]
        self.debug("Associating public IP for project: %s" %
                                                            self.project.id)
        public_ip_1 = PublicIPAddress.create(
                                           self.apiclient,
                                           zoneid=virtual_machine_1.zoneid,
                                           services=self.services["server"],
                                           networkid=network.id,
                                           projectid=self.project.id
                                           )
        self.cleanup.append(public_ip_1)
        # Verify Public IP state
        self.assertEqual(
                            public_ip_1.ipaddress.state in [
                                                 'Allocated',
                                                 'Allocating'
                                                 ],
                            True,
                            "Check Public IP state is allocated or not"
                        )

        # Exception should be raised for second Public IP
        with self.assertRaises(Exception):
            PublicIPAddress.create(
                                           self.apiclient,
                                           zoneid=virtual_machine_1.zoneid,
                                           services=self.services["server"],
                                           networkid=network.id,
                                           projectid=self.project.id
                                           )
        return

    @attr(tags=["advanced", "basic", "sg", "eip", "advancedns", "simulator"], required_hardware="false")
    def test_05_snapshots_per_project(self):
        """Test Snapshot limit per project
        """
        # Validate the following
        # 1. set max no of snapshots per project to 1.
        # 2. Create one snapshot in the project. Snapshot should be
        #    successfully created
        # 5. Try to create another snapshot in this project. It should give
        #    user an appropriate error and an alert should be generated.

        if self.hypervisor.lower() in ['hyperv', 'lxc']:
            raise self.skipTest("Snapshots feature is not supported on %s" % self.hypervisor.lower())
        self.debug(
            "Updating snapshot resource limits for project: %s" %
                                        self.project.id)
        # Set usage_vm=1 for Account 1
        update_resource_limit(
                              self.apiclient,
                              3, # Snapshot
                              max=1,
                              projectid=self.project.id
                              )

        self.debug("Deploying VM for account: %s" % self.account.name)
        virtual_machine_1 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                serviceofferingid=self.service_offering.id,
                                projectid=self.project.id
                                )
        self.cleanup.append(virtual_machine_1)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_1.state,
                            'Running',
                            "Check VM state is Running or not"
                        )

        # Get the Root disk of VM
        volumes = list_volumes(
                            self.apiclient,
                            virtualmachineid=virtual_machine_1.id,
                            projectid=self.project.id,
                            type='ROOT'
                            )
        self.assertEqual(
                        isinstance(volumes, list),
                        True,
                        "Check for list volume response return valid data"
                        )

        self.debug("Creating snapshot from volume: %s" % volumes[0].id)
        # Create a snapshot from the ROOTDISK
        snapshot_1 = Snapshot.create(self.apiclient,
                            volumes[0].id,
                            projectid=self.project.id
                            )
        self.cleanup.append(snapshot_1)

        #list snapshots
        snapshots = list_snapshots(self.apiclient, projectid=self.project.id)

        self.debug("snapshots list: %s" % snapshots)

        self.assertEqual(validateList(snapshots)[0], PASS, "Snapshots list validation failed")
        self.assertEqual(len(snapshots), 1, "Snapshots list should have exactly one entity")

        # Exception should be raised for second snapshot
        with self.assertRaises(Exception):
            Snapshot.create(self.apiclient,
                            volumes[0].id,
                            projectid=self.project.id
                            )
        return

    @attr(tags=["advanced", "basic", "sg", "eip", "advancedns", "simulator"], required_hardware="false")
    def test_06_volumes_per_project(self):
        """Test Volumes limit per project
        """
        # Validate the following
        # 1. set max no of volume per project to 1.
        # 2. Create 1 VM in this project
        # 4. Try to Create another VM in the project. It should give the user
        #    an appropriate error that Volume limit is exhausted and an alert
        #    should be generated.

        if self.hypervisor.lower() == 'lxc':
            if not find_storage_pool_type(self.apiclient, storagetype='rbd'):
                self.skipTest("RBD storage type is required for data volumes for LXC")
        self.project_1 = Project.create(
                         self.api_client,
                         self.services["project"],
                         account=self.account.name,
                         domainid=self.account.domainid
                         )
        self.cleanup.append(self.project_1)

        self.debug(
            "Updating volume resource limits for project: %s" %
                                                    self.project_1.id)
        # Set usage_vm=1 for Account 1
        update_resource_limit(
                              self.apiclient,
                              2, # Volume
                              max=1,
                              projectid=self.project_1.id
                              )

        self.debug("Deploying VM for project: %s" % self.project_1.id)
        virtual_machine_1 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                serviceofferingid=self.service_offering.id,
                                projectid=self.project_1.id
                                )
        # Verify VM state
        self.assertEqual(
                            virtual_machine_1.state,
                            'Running',
                            "Check VM state is Running or not"
                        )

        # Exception should be raised for second volume
        with self.assertRaises(Exception):
            Volume.create(
                          self.apiclient,
                          self.services["volume"],
                          zoneid=self.zone.id,
                          diskofferingid=self.disk_offering.id,
                          projectid=self.project_1.id
                        )
        return

    @attr(tags=["advanced", "basic", "sg", "eip", "advancedns"], required_hardware="true")
    def test_07_templates_per_project(self):
        """Test Templates limit per project
        """
        # 1. set max no of templates per project to 1.
        # 2. Create a template in this project. Both template should be in
        #    ready state
        # 3. Try create 2nd template in the project. It should give the user
        #    appropriate error and an alert should be generated.

        # Reset the volume limits
        update_resource_limit(
                              self.apiclient,
                              2, # Volume
                              max=5,
                              projectid=self.project.id
                              )
        self.debug(
            "Updating template resource limits for domain: %s" %
                                        self.account.domainid)
        # Set usage_vm=1 for Account 1
        update_resource_limit(
                              self.apiclient,
                              4, # Template
                              max=1,
                              projectid=self.project.id
                              )
        
        # Register the First Template in the project
        self.debug("Register the First Template in the project")
        builtin_info = get_builtin_template_info(self.apiclient, self.zone.id)
        self.services["template"]["url"] = builtin_info[0]
        self.services["template"]["hypervisor"] = builtin_info[1]
        self.services["template"]["format"] = builtin_info[2]

        # Register new template
        template = Template.register(
                                        self.userapiclient,
                                        self.services["template"],
                                        zoneid=self.zone.id,
                                        projectid=self.project.id
                                        )
        self.debug(
                "Registered a template of format: %s with ID: %s" % (
                                                                self.services["template"]["format"],
                                                                template.id
                                                                ))
        self.cleanup.append(template)

        # Wait for template status to be changed across
        time.sleep(self.services["sleep"])
        timeout = self.services["timeout"]
        while True:
            list_template_response = Template.list(
                                            self.apiclient,
                                            templatefilter='all',
                                            id=template.id,
                                            zoneid=self.zone.id,
                                            projectid=self.project.id,
                                            )
            if list_template_response[0].isready is True:
                break
            elif timeout == 0:
                raise Exception("Template state is not ready, it is %s" % list_template_response[0].isready)

            time.sleep(self.services["sleep"])
            timeout = timeout - 1
            
        #Verify template response to check whether template added successfully
        self.assertEqual(
                        isinstance(list_template_response, list),
                        True,
                        "Check for list template response return valid data"
                        )

        self.assertNotEqual(
                            len(list_template_response),
                            0,
                            "Check template available in List Templates"
                        )

        template_response = list_template_response[0]
        self.assertEqual(
                            template_response.isready,
                            True,
                            "Template state is not ready, it is %s" % template_response.isready
                        )

        # Exception should be raised for second template
        with self.assertRaises(Exception):
            Template.register(
                                self.userapiclient,
                                self.services["template"],
                                zoneid=self.zone.id,
                                projectid=self.project.id
                            )
        return

class TestMaxProjectNetworks(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestMaxProjectNetworks, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.domain = get_domain(cls.api_client)
        cls.services['mode'] = cls.zone.networktype
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')

        cls._cleanup = [
                        cls.service_offering,
                        cls.network_offering
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
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.account = Account.create(
                                     self.apiclient,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            self.account.delete(self.apiclient)
            interval = list_configurations(
                                    self.apiclient,
                                    name='account.cleanup.interval'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) * 2)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "simulator",
                "api"])
    def test_maxAccountNetworks(self):
        """Test Limit number of guest account specific networks
        """

        # Steps for validation
        # 1. Fetch max.account.networks from configurations
        # 2. Create an account. Create account more that max.accout.network
        # 3. Create network should fail

        self.debug("Creating project with '%s' as admin" %
                                            self.account.name)
        # Create project as a domain admin
        project = Project.create(
                                 self.apiclient,
                                 self.services["project"],
                                 account=self.account.name,
                                 domainid=self.account.domainid
                                 )
        # Cleanup created project at end of test
        self.cleanup.append(project)
        self.debug("Created project with domain admin with ID: %s" %
                                                                project.id)

        config = Configurations.list(
                                    self.apiclient,
                                    name='max.project.networks',
                                    listall=True
                                    )
        self.assertEqual(
                isinstance(config, list),
                True,
                "List configurations hsould have max.project.networks"
                )

        config_value = int(config[0].value)
        self.debug("max.project.networks: %s" % config_value)

        for ctr in range(config_value):
            # Creating network using the network offering created
            self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
            network = Network.create(
                                    self.apiclient,
                                    self.services["network"],
                                    projectid=project.id,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )
            self.debug("Created network with ID: %s" % network.id)
        self.debug(
            "Creating network in account already having networks : %s" %
                                                            config_value)

        with self.assertRaises(Exception):
            Network.create(
                                    self.apiclient,
                                    self.services["network"],
                                    projectid=project.id,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )
        self.debug('Create network failed (as expected)')
        return
