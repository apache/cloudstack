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
""" P1 tests for Resource creation
"""
#Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.base import (VirtualMachine,
                                         Account,
                                         Project,
                                         NATRule,
                                         PublicIPAddress,
                                         Network,
                                         Snapshot,
                                         Template,
                                         FireWallRule,
                                         SecurityGroup,
                                         ServiceOffering,
                                         Domain,
                                         Volume,
                                         DiskOffering,
                                         LoadBalancerRule)

from marvin.lib.common import (get_zone,
                                           get_template,
                                           get_domain,
                                           list_volumes,
                                           list_network_offerings,
                                           list_lb_rules,
                                           get_free_vlan)

from marvin.lib.utils import cleanup_resources
import random
import time

class Services:
    """Test Resource creation Services
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
                                    "cpuspeed": 100,    # in MHz
                                    "memory": 128,       # In MBs
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
                                    "ispublic": False,
                        },
                        "network": {
                                    "name": "Test Network",
                                    "displaytext": "Test Network",
                        },
                        "domain_network": {
                                    "name": "Domainwide Network",
                                    "displaytext": "Domainwide Network",
                                    "gateway": '',
                                    "netmask": '255.255.255.0',
                                    "startip": '',
                                    "endip": '',
                                    "vlan": '',
                                    "acltype": 'domain'
                        },
                        "natrule": {
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": "TCP"
                        },
                        "lbrule": {
                                    "name": "SSH",
                                    "alg": "roundrobin",
                                    # Algorithm used for load balancing
                                    "privateport": 22,
                                    "publicport": 2222,
                        },
                        "fw_rule": {
                                    "startport": 1,
                                    "endport": 6000,
                                    "cidr": '55.55.0.0/11',
                                    # Any network (For creating FW rule)
                        },
                        "security_group": {
                                    "name": 'SSH',
                                    "protocol": 'TCP',
                                    "startport": 22,
                                    "endport": 22,
                                    "cidrlist": '0.0.0.0/0',
                        },
                        "ostype": 'CentOS 5.3 (64-bit)',
                        # Cent OS 5.3 (64 bit)
                        "sleep": 60,
                        "timeout": 10,
                    }


class TestOfferings(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestOfferings, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["server"]["zoneid"] = cls.zone.id

        # Create domains, account etc.
        cls.domain = Domain.create(
                                   cls.api_client,
                                   cls.services["domain"]
                                   )

        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain.id
                            )
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"],
                                            domainid=cls.domain.id
                                            )
        cls.disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering"]
                                    )
        cls._cleanup = [
                        cls.account,
                        cls.service_offering,
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
            cleanup_resources(self.apiclient, reversed(self.cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic", "sg", "eip", "advancedns", "simulator"], required_hardware="false")
    def test_01_service_offerings(self):
        """ Test service offerings in a project
        """
        # Validate the following
        # 1. Create a project.
        # 2. List service offerings for the project. All SO available in the
        #    domain can be used for project resource creation.

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

        self.debug(
            "Deploying VM instance for project: %s & service offering: %s" % (
                                                    project.id,
                                                    self.service_offering.id
                                                    ))
        virtual_machine = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                serviceofferingid=self.service_offering.id,
                                projectid=project.id
                                )
        # Verify VM state
        self.assertEqual(
                            virtual_machine.state,
                            'Running',
                            "Check VM state is Running or not"
                        )

        return

    @attr(tags=["advanced", "basic", "sg", "eip", "advancedns", "simulator"], required_hardware="false")
    def test_02_project_disk_offerings(self):
        """ Test project disk offerings
        """

        # Validate the following
        # 1. Create a project.
        # 2. List service offerings for the project. All disk offerings
        #    available in the domain can be used for project resource creation

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
        self.debug(
            "Create a data volume for project: %s" % project.id)
        # Create a volume for project
        volume = Volume.create(
                               self.apiclient,
                               self.services["volume"],
                               zoneid=self.zone.id,
                               diskofferingid=self.disk_offering.id,
                               projectid=project.id
                               )
        self.cleanup.append(volume)
        # Verify Volume state
        self.assertEqual(
                            volume.state in [
                                                 'Allocated',
                                                 'Ready'
                                                 ],
                            True,
                            "Check Volume state is Ready or not"
                        )
        return


class TestNetwork(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestNetwork, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["server"]["zoneid"] = cls.zone.id

        # Create domains, account etc.
        cls.domain = Domain.create(
                                   cls.api_client,
                                   cls.services["domain"]
                                   )

        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain.id
                            )
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"],
                                            domainid=cls.domain.id
                                            )
        cls._cleanup = [
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
            #Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "simulator"], required_hardware="false")
    def test_03_network_create(self):
        """ Test create network in project
        """
        # Validate the following
        # 1. Create a project.
        # 2. Add virtual/direct network resource to the project. User shared
        #    network resource for the project
        # 3. Verify any number of Project level Virtual/Direct networks can be
        #    created and used for vm deployment within the project.
        # 4. Verify shared networks (zone and domain wide) from outside the
        #    project can also be used in a project.

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

        network_offerings = list_network_offerings(
                                                self.apiclient,
                                                projectid=project.id,
                                                supportedServices='SourceNat',
                                                type='isolated',
                                                state='Enabled'
                                                )
        self.assertEqual(
                         isinstance(network_offerings, list),
                         True,
                         "Check for the valid network offerings"
                         )
        network_offering = network_offerings[0]

        self.debug("creating a network with network offering ID: %s" %
                                                        network_offering.id)
        self.services["network"]["zoneid"] = self.zone.id
        network = Network.create(
                                 self.apiclient,
                                 self.services["network"],
                                 networkofferingid=network_offering.id,
                                 projectid=project.id
                                 )
        self.debug("Created network with ID: %s" % network.id)
        networks = Network.list(
                               self.apiclient,
                               projectid=project.id,
                               listall=True
                               )
        self.assertEqual(
                         isinstance(networks, list),
                         True,
                         "Check for the valid network list response"
                         )

        self.debug("Deploying VM with network: %s" % network.id)

        virtual_machine = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                networkids=[str(network.id)],
                                serviceofferingid=self.service_offering.id,
                                projectid=project.id
                                )
        self.debug("Deployed VM with ID: %s" % virtual_machine.id)
        # Verify VM state
        self.assertEqual(
                            virtual_machine.state,
                            'Running',
                            "Check VM state is Running or not"
                        )

        network_offerings = list_network_offerings(
                                        self.apiclient,
                                        state='Enabled',
                                        guestiptype='Shared',
                                        name='DefaultSharedNetworkOffering',
                                        displaytext='Offering for Shared networks'
                                        )
        self.assertEqual(
                         isinstance(network_offerings, list),
                         True,
                         "Check for the valid network offerings"
                         )
        network_offering = network_offerings[0]

        self.debug("creating a shared network in domain: %s" %
                                                        self.domain.id)

        # Getting physical network and free vlan in it
        physical_network, vlan = get_free_vlan(self.apiclient, self.zone.id)

        self.services["domain_network"]["vlan"] = vlan
        self.services["domain_network"]["physicalnetworkid"] = physical_network.id

        # Generating random subnet number for shared network creation
        shared_network_subnet_number = random.randrange(1,254)

        self.services["domain_network"]["gateway"] = "172.16."+str(shared_network_subnet_number)+".1"
        self.services["domain_network"]["startip"] = "172.16."+str(shared_network_subnet_number)+".2"
        self.services["domain_network"]["endip"] = "172.16."+str(shared_network_subnet_number)+".20"

        domain_network = Network.create(
                                 self.apiclient,
                                 self.services["domain_network"],
                                 domainid=self.domain.id,
                                 networkofferingid=network_offering.id,
                                 zoneid=self.zone.id
                                 )
        self.cleanup.append(domain_network)
        self.debug("Created network with ID: %s" % domain_network.id)

        virtual_machine = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                networkids=[str(domain_network.id)],
                                serviceofferingid=self.service_offering.id,
                                projectid=project.id
                                )
        self.debug("Deployed VM with ID: %s" % virtual_machine.id)
        # Verify VM state
        self.assertEqual(
                            virtual_machine.state,
                            'Running',
                            "Check VM state is Running or not"
                        )

        # Delete VM before network gets deleted in cleanup
        virtual_machine.delete(self.apiclient, expunge=True)
        return


class TestTemplates(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestTemplates, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['lxc']:
            raise unittest.SkipTest("create template from volume is not supported on %s" % cls.hypervisor.lower())

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
        cls.user = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
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
        cls.userapiclient = cls.testClient.getUserApiClient(
                                UserName=cls.account.name,
                                DomainName=cls.domain.name
				)

        cls._cleanup = [
                        cls.project,
                        cls.service_offering,
                        cls.account,
                        cls.user,
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

    @attr(tags=["advanced", "basic", "sg", "eip", "advancedns"], required_hardware="false")
    def test_04_public_private_template_use_in_project(self):
        """Test Templates creation in projects
        """
        # 1. Create a project
        # 2. Verify Public templates can be used without any restriction
        # 3. Verify that private template created in project belongs to this project
        # Verify that list template api wth project id list this template



        try:
            self.debug("Deploying VM for with public template: %s" %
                                                        self.template.id)
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
            virtual_machine_1.stop(self.apiclient)
            # Get the Root disk of VM
            volumes = list_volumes(
                            self.apiclient,
                            projectid=self.project.id,
                            type='ROOT',
                            listall=True
                            )
            self.assertEqual(
                        isinstance(volumes, list),
                        True,
                        "Check for list volume response return valid data"
                        )
            volume = volumes[0]

            self.debug("Creating template from volume: %s" % volume.id)
            # Create a template from the ROOTDISK
            template_1 = Template.create(
                            self.apiclient,
                            self.services["template"],
                            volumeid=volume.id,
                            projectid=self.project.id
                            )

            self.cleanup.append(template_1)
            # Verify Template state
            self.assertEqual(
                            template_1.isready,
                            True,
                            "Check Template is in ready state or not"
                        )
            # Verify list template with project id is listing this template
            templatelist = Template.list(self.apiclient,projectid=self.project.id,id=template_1.id,templatefilter="all")
            self.assertEqual(templatelist[0].id,template_1.id,"template created does not belong to the project")


        except Exception as e:
            self.fail("Exception occured: %s" % e)
        return

    @attr(tags=["advanced", "basic", "sg", "eip", "advancedns"], required_hardware="false")
    def test_05_use_private_template_in_project(self):
        """Test use of private template in a project
        """
        # 1. Create a project
        # 2. Verify that in order to use somebody's Private template for vm
        #    creation in the project, permission to use the template has to
        #    be granted to the Project (use API 'updateTemplatePermissions'
        #    with project id to achieve that).

        try:
            self.debug("Deploying VM for with public template: %s" %
                                                        self.template.id)
            virtual_machine_1 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                serviceofferingid=self.service_offering.id,
                                projectid=self.project.id
                                )
            self.cleanup.append(virtual_machine_1)
            # Verify VM state
            self.assertEqual(virtual_machine_1.state,
                         'Running',
                         "Check VM state is Running or not")
            virtual_machine_1.stop(self.apiclient)
            # Get the Root disk of VM
            volumes = list_volumes(
                            self.apiclient,
                            projectid=self.project.id,
                            type='ROOT',
                            listall=True
                            )
            self.assertEqual(
                        isinstance(volumes, list),
                        True,
                        "Check for list volume response return valid data"
                        )
            volume = volumes[0]

            self.debug("Creating template from volume: %s" % volume.id)
            # Create a template from the ROOTDISK
            template_1 = Template.create(
                            self.userapiclient,
                            self.services["template"],
                            volumeid=volume.id
                            )

            self.cleanup.append(template_1)
            # Verify Template state
            self.assertEqual(
                            template_1.isready,
                            True,
                            "Check Template is in ready state or not"
                        )

            # Update template permissions to grant permission to project
            self.debug(
                        "Updating template permissions:%s to grant access to project: %s" % (
                                                            template_1.id,
                                                            self.project.id
                                                        ))

            template_1.updatePermissions(
                                     self.apiclient,
                                     op='add',
                                     projectids=self.project.id
                                     )
            self.debug("Deploying VM for with privileged template: %s" %
                                                        self.template.id)
            virtual_machine_2 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=template_1.id,
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
        except Exception as e:
            self.fail("Exception occured: %s" % e)
        return


class TestSnapshots(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestSnapshots, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls._cleanup = []
        cls.snapshotSupported = True
        if cls.hypervisor.lower() in ['hyperv', 'lxc']:
            cls.snapshotSupported = False

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
        cls._cleanup = [
                        cls.project,
                        cls.service_offering,
                        cls.account
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

    @attr(speed = "slow")
    @attr(tags=["advanced", "basic", "sg", "eip", "advancedns", "simulator"], required_hardware="false")
    def test_06_create_snapshots_in_project(self):
        """Test create snapshots in project
        """
        # Validate the following
        # 1. Create a project
        # 2. Add some snapshots to the project
        # 3. Verify snapshot created inside project can only be used in inside
        #    the project

        if not self.snapshotSupported:
            self.skipTest("Snapshot is not supported on %s" % self.hypervisor)

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

        # Get the Root disk of VM
        volumes = list_volumes(
                            self.apiclient,
                            projectid=self.project.id,
                            type='ROOT',
                            listall=True
                            )
        self.assertEqual(
                        isinstance(volumes, list),
                        True,
                        "Check for list volume response return valid data"
                        )

        self.debug("Creating snapshot from volume: %s" % volumes[0].id)
        # Create a snapshot from the ROOTDISK
        snapshot = Snapshot.create(self.apiclient,
                            volumes[0].id,
                            projectid=self.project.id
                            )
        self.cleanup.append(snapshot)
        # Verify Snapshot state
        self.assertEqual(
                            snapshot.state in [
                                                 'BackedUp',
                                                 'CreatedOnPrimary',
                                                 'Allocated'
                                                 ],
                            True,
                            "Check Snapshot state is in one of the mentioned possible states, \
                                    It is currently: %s" % snapshot.state
                        )

        snapshots = Snapshot.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid
                                  )
        self.assertEqual(
                    snapshots,
                    None,
                    "Snapshots should not be available outside the project"
                    )
        return


class TestPublicIpAddress(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestPublicIpAddress, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

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
        cls.virtual_machine = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["server"],
                                    templateid=cls.template.id,
                                    serviceofferingid=cls.service_offering.id,
                                    projectid=cls.project.id
                                )

        cls._cleanup = [
                        cls.project,
                        cls.service_offering,
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

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_07_associate_public_ip(self):
        """Test associate public IP within the project
        """
        # Validate the following
        # 1. Create a project
        # 2. Add some public Ips to the project
        # 3. Verify public IP assigned can only used to create PF/LB rules
        #    inside project

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
        self.debug("Associating public IP for project: %s" % self.project.id)
        public_ip = PublicIPAddress.create(
                                           self.apiclient,
                                           zoneid=self.virtual_machine.zoneid,
                                           services=self.services["server"],
                                           networkid=network.id,
                                           projectid=self.project.id
                                           )
        self.cleanup.append(public_ip)

        #Create NAT rule
        self.debug(
                "Creating a NAT rule within project, VM ID: %s" %
                                                    self.virtual_machine.id)
        nat_rule = NATRule.create(
                                  self.apiclient,
                                  self.virtual_machine,
                                  self.services["natrule"],
                                  public_ip.ipaddress.id,
                                  projectid=self.project.id
                                  )
        self.debug("created a NAT rule with ID: %s" % nat_rule.id)
        nat_rule_response = NATRule.list(
                                            self.apiclient,
                                            id=nat_rule.id
                                        )
        self.assertEqual(
                            isinstance(nat_rule_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            len(nat_rule_response),
                            0,
                            "Check Port Forwarding Rule is created"
                            )
        self.assertEqual(
                            nat_rule_response[0].id,
                            nat_rule.id,
                            "Check Correct Port forwarding Rule is returned"
                        )

        #Create Load Balancer rule and assign VMs to rule
        self.debug("Created LB rule for public IP: %s" %
                                        public_ip.ipaddress)
        lb_rule = LoadBalancerRule.create(
                                          self.apiclient,
                                          self.services["lbrule"],
                                          public_ip.ipaddress.id,
                                          projectid=self.project.id
                                          )
        self.debug("Assigning VM: %s to LB rule: %s" % (
                                                    self.virtual_machine.name,
                                                    lb_rule.id
                                                    ))
        lb_rule.assign(self.apiclient, [self.virtual_machine])

        lb_rules = list_lb_rules(
                                 self.apiclient,
                                 id=lb_rule.id
                                 )
        self.assertEqual(
                            isinstance(lb_rules, list),
                            True,
                            "Check list response returns a valid list"
                        )
        #verify listLoadBalancerRules lists the added load balancing rule
        self.assertNotEqual(
                            len(lb_rules),
                            0,
                            "Check Load Balancer Rule in its List"
                        )
        self.assertEqual(
                            lb_rules[0].id,
                            lb_rule.id,
                            "Check List Load Balancer Rules returns valid Rule"
                        )

        #Create Firewall rule with configurations from settings file
        fw_rule = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=public_ip.ipaddress.id,
                            protocol='TCP',
                            cidrlist=[self.services["fw_rule"]["cidr"]],
                            startport=self.services["fw_rule"]["startport"],
                            endport=self.services["fw_rule"]["endport"],
                            projectid=self.project.id
                            )
        self.debug("Created firewall rule: %s" % fw_rule.id)

        # After Router start, FW rule should be in Active state
        fw_rules = FireWallRule.list(
                                     self.apiclient,
                                     id=fw_rule.id,
                                     )
        self.assertEqual(
                        isinstance(fw_rules, list),
                        True,
                        "Check for list FW rules response return valid data"
                        )

        self.assertEqual(
                    fw_rules[0].state,
                    'Active',
                    "Check list load balancing rules"
                    )
        self.assertEqual(
                    fw_rules[0].startport,
                    self.services["fw_rule"]["startport"],
                    "Check start port of firewall rule"
                    )

        self.assertEqual(
                    fw_rules[0].endport,
                    self.services["fw_rule"]["endport"],
                    "Check end port of firewall rule"
                    )

        self.debug("Deploying VM for account: %s" % self.account.name)
        virtual_machine_1 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                serviceofferingid=self.service_offering.id,
                                )
        self.cleanup.append(virtual_machine_1)

        self.debug("VM state after deploy: %s" % virtual_machine_1.state)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_1.state,
                            'Running',
                            "Check VM state is Running or not"
                        )

        self.debug("Creating NAT rule for VM (ID: %s) outside project" %
                                                    virtual_machine_1.id)
        with self.assertRaises(Exception):
            NATRule.create(
                                  self.apiclient,
                                  virtual_machine_1,
                                  self.services["natrule"],
                                  public_ip.ipaddress.id,
                                  )

        self.debug("Creating LB rule for public IP: %s outside project" %
                                                public_ip.ipaddress)
        with self.assertRaises(Exception):
            LoadBalancerRule.create(
                                          self.apiclient,
                                          self.services["lbrule"],
                                          public_ip.ipaddress.id,
                                          accountid=self.account.name
                                          )
        return


class TestSecurityGroup(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestSecurityGroup, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.domain = get_domain(cls.api_client)
        cls.services['mode'] = cls.zone.networktype

        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["server"]["zoneid"] = cls.zone.id
        cls.services["server"]["template"] = template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain.id
                            )
        # Create project as a domain admin
        cls.project = Project.create(
                                 cls.api_client,
                                 cls.services["project"],
                                 account=cls.account.name,
                                 domainid=cls.account.domainid
                                 )
        cls.services["account"] = cls.account.name

        cls._cleanup = [
                        cls.project,
                        cls.account,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(TestSecurityGroup, cls).getClsTestClient().getApiClient()
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags = ["sg", "eip"])
    def test_08_security_group(self):
        """Test security groups in project
        """
        # Validate the following:
        # 1. Create a project
        # 2. Assign some security groups to that project
        # 3. Verify the security groups can only be assigned to VM belonging
        #    to that project.

        security_group = SecurityGroup.create(
                                              self.apiclient,
                                              self.services["security_group"],
                                              projectid=self.project.id
                                              )
        self.debug("Created security group with ID: %s" % security_group.id)
        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                              self.apiclient,
                                              projectid=self.project.id
                                              )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )

        self.assertNotEqual(
                            len(sercurity_groups),
                            0,
                            "Check List Security groups response"
                            )
        # Authorize Security group to SSH to VM
        ingress_rule = security_group.authorize(
                                self.apiclient,
                                self.services["security_group"],
                                projectid=self.project.id
                                )
        self.assertEqual(
                          isinstance(ingress_rule, dict),
                          True,
                          "Check ingress rule created properly"
                    )

        self.debug(
            "Authorizing ingress rule for sec group ID: %s for ssh access"
                                                        % security_group.id)
        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["server"],
                                    serviceofferingid=self.service_offering.id,
                                    securitygroupids=[security_group.id],
                                    projectid=self.project.id
                                )
        self.debug("Deployed VM (ID: %s) in project: %s" % (
                                                    self.virtual_machine.id,
                                                    self.project.id
                                                    ))
        self.assertEqual(
                         self.virtual_machine.state,
                         'Running',
                         "VM state should be running after deployment"
                         )
        # Deploy another VM with same security group outside the project
        self.debug(
            "Deploying VM with security group: %s outside project:%s" % (
                                                            security_group.id,
                                                            self.project.id
                                                            ))
        with self.assertRaises(Exception):
            VirtualMachine.create(
                                    self.apiclient,
                                    self.services["server"],
                                    serviceofferingid=self.service_offering.id,
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    securitygroupids=[security_group.id],
                                )
        return
