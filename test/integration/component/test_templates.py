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
""" P1 tests for Templates
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.cloudstackAPI import listZones
from marvin.lib.utils import (cleanup_resources)
from marvin.lib.base import (Account,
                             Domain,
                             Template,
                             ServiceOffering,
                             VirtualMachine,
                             Snapshot,
                             Volume)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               get_builtin_template_info)
# Import System modules
import time


class Services:

    """Test Templates Services
    """

    def __init__(self):
        self.services = {
            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                # Random characters are appended for unique
                # username
                "password": "password",
            },
            "testdomain": {"name": "test"},
            "service_offering": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100,    # in MHz
                                    "memory": 128,       # In MBs
            },
            "disk_offering": {
                "displaytext": "Small",
                "name": "Small",
                "disksize": 1
            },
            "virtual_machine": {
                "displayname": "testVM",
                "hypervisor": 'XenServer',
                "protocol": 'TCP',
                "ssh_port": 22,
                "username": "root",
                "password": "password",
                "privateport": 22,
                "publicport": 22,
            },
            "volume": {
                "diskname": "Test Volume",
            },
            "templates": {
                # Configs for different Template formats
                # For Eg. raw image, zip etc
                0: {
                    "displaytext": "Public Template",
                    "name": "Public template",
                    "ostype": 'CentOS 5.3 (64-bit)',
                    "url": "http://download.cloudstack.org/releases/2.0.0/UbuntuServer-10-04-64bit.vhd.bz2",
                    "hypervisor": 'XenServer',
                    "format": 'VHD',
                    "isfeatured": True,
                    "ispublic": True,
                    "isextractable": True,
                },
            },
            "template": {
                "displaytext": "Cent OS Template",
                "name": "Cent OS Template",
                "ostype": 'CentOS 5.3 (64-bit)',
                "templatefilter": 'self',
                "isfeatured": True,
                "ispublic": True,
            },
            "templatefilter": 'self',
            "ostype": 'CentOS 5.3 (64-bit)',
            "sleep": 60,
            "timeout": 10,
        }


class TestCreateTemplate(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.unsupportedHypervisor:
            self.skipTest(
                "Template creation from root volume is not supported in LXC")
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestCreateTemplate, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls._cleanup = []
        cls.unsupportedHypervisor = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['lxc']:
            cls.unsupportedHypervisor = True
            return
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)
        cls.services["account"] = cls.account.name
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(
                TestCreateTemplate,
                cls).getClsTestClient().getApiClient()
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    def test_01_create_template(self):
        """Test create public & private template
        """
        # Validate the following:
        # 1. Upload a templates in raw img format. Create a Vm instances from
        #    raw img template.
        # 2. Upload a templates in  zip file format. Create a Vm instances from
        #    zip template.
        # 3. Upload a templates in tar format.Create a Vm instances from tar
        #    template.
        # 4. Upload a templates in tar gzip format.Create a Vm instances from
        #    tar gzip template.
        # 5. Upload a templates in  tar bzip format. Create a Vm instances from
        #    tar bzip template.
        # 6. Verify VMs & Templates is up and in ready state

        builtin_info = get_builtin_template_info(self.apiclient, self.zone.id)
        self.services["templates"][0]["url"] = builtin_info[0]
        self.services["templates"][0]["hypervisor"] = builtin_info[1]
        self.services["templates"][0]["format"] = builtin_info[2]

        # Register new template
        template = Template.register(
            self.apiclient,
            self.services["templates"][0],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            hypervisor=self.hypervisor
        )
        self.debug(
            "Registered a template of format: %s with ID: %s" % (
                self.services["templates"][0]["format"],
                template.id
            ))
        # Wait for template to download
        template.download(self.apiclient)
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
                account=self.account.name,
                domainid=self.account.domainid)
            if isinstance(list_template_response, list):
                break
            elif timeout == 0:
                raise Exception("List template failed!")

            time.sleep(5)
            timeout = timeout - 1
        # Verify template response to check whether template added successfully
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

        # Deploy new virtual machine using template
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.services["mode"]
        )
        self.debug("creating an instance with template ID: %s" % template.id)
        vm_response = VirtualMachine.list(self.apiclient,
                                          id=virtual_machine.id,
                                          account=self.account.name,
                                          domainid=self.account.domainid)
        self.assertEqual(
            isinstance(vm_response, list),
            True,
            "Check for list VMs response after VM deployment"
        )
        # Verify VM response to check whether VM deployment was successful
        self.assertNotEqual(
            len(vm_response),
            0,
            "Check VMs available in List VMs response"
        )
        vm = vm_response[0]
        self.assertEqual(
            vm.state,
            'Running',
            "Check the state of VM created from Template"
        )
        return


class TestTemplates(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestTemplates, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls._cleanup = []
        cls.unsupportedHypervisor = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['lxc']:
            cls.unsupportedHypervisor = True
            return
        # populate second zone id for iso copy
        cmd = listZones.listZonesCmd()
        zones = cls.api_client.listZones(cmd)
        if not isinstance(zones, list):
            raise Exception("Failed to find zones.")
        if len(zones) >= 2:
            cls.services["destzoneid"] = zones[1].id

        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        try:
            cls.account = Account.create(
                cls.api_client,
                cls.services["account"],
                domainid=cls.domain.id
            )
            cls._cleanup.append(cls.account)

            cls.services["account"] = cls.account.name
            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.services["service_offering"]
            )
            cls._cleanup.append(cls.service_offering)

            # create virtual machine
            cls.virtual_machine = VirtualMachine.create(
                cls.api_client,
                cls.services["virtual_machine"],
                templateid=template.id,
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                serviceofferingid=cls.service_offering.id,
            )
            # Stop virtual machine
            cls.virtual_machine.stop(cls.api_client)

            timeout = cls.services["timeout"]

            while True:
                list_volume = Volume.list(
                    cls.api_client,
                    virtualmachineid=cls.virtual_machine.id,
                    type='ROOT',
                    listall=True)
                if isinstance(list_volume, list):
                    break
                elif timeout == 0:
                    raise Exception("List volumes failed.")

                time.sleep(5)
                timeout = timeout - 1

            cls.volume = list_volume[0]

            # Create template from volume
            cls.template = Template.create(
                cls.api_client,
                cls.services["template"],
                cls.volume.id
            )
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Failure in setUpClass: %s" % e)

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(
                TestTemplates,
                cls).getClsTestClient().getApiClient()
            # Cleanup created resources such as templates and VMs
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.unsupportedHypervisor:
            self.skipTest(
                "Template creation from root volume is not supported in LXC")
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_01_create_template_volume(self):
        """Test Create template from volume
        """

        # Validate the following:
        # 1. Deploy new VM using the template created from Volume
        # 2. VM should be in Up and Running state

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
        )

        self.debug(
            "creating an instance with template ID: %s" %
            self.template.id)
        self.cleanup.append(virtual_machine)
        vm_response = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine.id,
            account=self.account.name,
            domainid=self.account.domainid)
        # Verify VM response to check whether VM deployment was successful
        self.assertNotEqual(
            len(vm_response),
            0,
            "Check VMs available in List VMs response"
        )
        vm = vm_response[0]
        self.assertEqual(
            vm.state,
            'Running',
            "Check the state of VM created from Template"
        )
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_03_delete_template(self):
        """Test Delete template
        """

        # Validate the following:
        # 1. Create a template and verify it is shown in list templates response
        # 2. Delete the created template and again verify list template
        # response

        # Verify template response for updated attributes
        list_template_response = Template.list(
            self.apiclient,
            templatefilter=self.services["template"]["templatefilter"],
            id=self.template.id,
            zoneid=self.zone.id)
        self.assertEqual(
            isinstance(list_template_response, list),
            True,
            "Check for list template response return valid list"
        )

        self.assertNotEqual(
            len(list_template_response),
            0,
            "Check template available in List Templates"
        )
        template_response = list_template_response[0]

        self.assertEqual(
            template_response.id,
            self.template.id,
            "Template id %s in the list is not matching with created template id %s" %
            (template_response.id, self.template.id)
        )

        self.debug("Deleting template: %s" % self.template)
        # Delete the template
        self.template.delete(self.apiclient)
        self.debug("Delete template: %s successful" % self.template)

        list_template_response = Template.list(
            self.apiclient,
            templatefilter=self.services["template"]["templatefilter"],
            id=self.template.id,
            zoneid=self.zone.id
        )
        self.assertEqual(
            list_template_response,
            None,
            "Check template available in List Templates"
        )
        return

    @attr(speed="slow")
    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    def test_04_template_from_snapshot(self):
        """Create Template from snapshot
        """

        # Validate the following
        # 2. Snapshot the Root disk
        # 3. Create Template from snapshot
        # 4. Deploy Virtual machine using this template
        # 5. VM should be in running state

        if self.hypervisor.lower() in ['hyperv', 'lxc']:
            self.skipTest(
                "Snapshots feature is not supported on %s" %
                self.hypervisor.lower())

        userapiclient = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain)

        volumes = Volume.list(
            userapiclient,
            virtualmachineid=self.virtual_machine.id,
            type='ROOT',
            listall=True
        )
        volume = volumes[0]

        self.debug("Creating a snapshot from volume: %s" % volume.id)
        # Create a snapshot of volume
        snapshot = Snapshot.create(
            userapiclient,
            volume.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Creating a template from snapshot: %s" % snapshot.id)
        # Generate template from the snapshot
        template = Template.create_from_snapshot(
            userapiclient,
            snapshot,
            self.services["template"]
        )
        self.cleanup.append(template)
        # Verify created template
        templates = Template.list(
            userapiclient,
            templatefilter=self.services["template"]["templatefilter"],
            id=template.id
        )
        self.assertNotEqual(
            templates,
            None,
            "Check if result exists in list item call"
        )

        self.assertEqual(
            templates[0].id,
            template.id,
            "Check new template id in list resources call"
        )
        self.debug("Deploying a VM from template: %s" % template.id)
        # Deploy new virtual machine using template
        virtual_machine = VirtualMachine.create(
            userapiclient,
            self.services["virtual_machine"],
            templateid=template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
        )
        self.cleanup.append(virtual_machine)

        vm_response = VirtualMachine.list(
            userapiclient,
            id=virtual_machine.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(vm_response, list),
            True,
            "Check for list VM response return valid list"
        )

        # Verify VM response to check whether VM deployment was successful
        self.assertNotEqual(
            len(vm_response),
            0,
            "Check VMs available in List VMs response"
        )
        vm = vm_response[0]
        self.assertEqual(
            vm.state,
            'Running',
            "Check the state of VM created from Template"
        )
        return


class TestListTemplate(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        self.services = Services().services
        # Get Zone, Domain and templates
        self.domain = get_domain(self.apiclient)
        self.account = Account.create(
                            self.apiclient,
                            self.services["account"],
                            domainid=self.domain.id
                            )
        self.newdomain = Domain.create(
                           self.apiclient,
                           self.services["testdomain"],
                           parentdomainid=self.domain.id
                           )
        self.newdomain_account = Account.create(
                           self.apiclient,
                           self.services["account"],
                           admin=True,
                           domainid=self.newdomain.id
                           )
        self.cleanup = [
                        self.account,
                        self.newdomain_account,
                        self.newdomain,
                        ]


    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)


    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_01_list_templates_with_templatefilter_all_normal_user(self):
        """
            Test list templates with templatefilter=all is not permitted for normal user
        """

        user_api_client = self.testClient.getUserApiClient(
                                    UserName=self.account.name,
                                    DomainName=self.account.domain)
        try:
            list_template_response = Template.list(user_api_client, templatefilter='all')
            self.fail("Regular User is able to use templatefilter='all' in listTemplates API call")
        except Exception as e:
            self.debug("ListTemplates API with templatefilter='all' is not permitted for normal user")


    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_02_list_templates_with_templatefilter_all_domain_admin(self):
        """
            Test list templates with templatefilter=all is not permitted for domain admin
        """

        domain_user_api_client = self.testClient.getUserApiClient(
                                    UserName=self.newdomain_account.name,
                                    DomainName=self.newdomain_account.domain)
        try:
            list_template_response = Template.list(domain_user_api_client, templatefilter='all')
        except Exception as e:
            self.fail("Domain admin should be able to use templatefilter='all' in listTemplates API call")
