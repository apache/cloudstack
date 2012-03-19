# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#
""" P1 tests for Templates
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from testcase.libs.utils import *
from testcase.libs.base import *
from testcase.libs.common import *
import urllib
from random import random
#Import System modules
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
                                    "password": "fr3sca",
                         },
                         "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 100, # in MHz
                                    "memory": 64, # In MBs
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
                            0:{
                                "displaytext": "Public Template",
                                "name": "Public template",
                                "ostypeid": '144f66aa-7f74-4cfe-9799-80cc21439cb3',
                                "url": "http://download.cloud.com/releases/2.0.0/UbuntuServer-10-04-64bit.vhd.bz2",
                                "hypervisor": 'XenServer',
                                "format" : 'VHD',
                                "isfeatured": True,
                                "ispublic": True,
                                "isextractable": True,
                                },
                        },
                        "template": {
                                "displaytext": "Cent OS Template",
                                "name": "Cent OS Template",
                                "ostypeid": '144f66aa-7f74-4cfe-9799-80cc21439cb3',
                                "templatefilter": 'self',
                        },
                        "templatefilter": 'self',
                        "destzoneid": 2, # For Copy template (Destination zone)
                        "ostypeid": '144f66aa-7f74-4cfe-9799-80cc21439cb3',
                        "sleep": 60,
                        "timeout": 10,
                        "mode": 'advanced', # Networking mode: Advanced, basic
                     }


@unittest.skip("Open questions")
class TestCreateTemplate(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.dbclient.close()
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.services = Services().services
        cls.api_client = fetch_api_client()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.services["account"] = cls.account.account.name

        cls._cleanup = [
                        cls.account,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = fetch_api_client()
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

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

        for k, v in self.services["templates"].items():

            # Register new template
            template = Template.register(
                                        self.apiclient,
                                        v,
                                        zoneid=self.zone.id,
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )
            self.debug(
                "Registered a template of format: %s with ID: %s" % (
                                                                v["format"],
                                                                template.id
                                                                ))
            # Wait for template to download
            template.download(self.apiclient)
            self.cleanup.append(template)

            # Wait for template status to be changed across
            time.sleep(self.services["sleep"])
            timeout = self.services["timeout"]
            while True:
                list_template_response = list_templates(
                                    self.apiclient,
                                    templatefilter=\
                                    self.services["templatefilter"],
                                    id=template.id,
                                    zoneid=self.zone.id,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                    )
                if isinstance(list_template_response, list):
                    break
                elif timeout == 0:
                    raise Exception("List template failed!")
                
                time.sleep(5)
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
                            "Check display text of newly created template"
                        )

            # Deploy new virtual machine using template
            virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    templateid=template.id,
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    mode=self.services["mode"]
                                    )
            self.debug("creating an instance with template ID: %s" % template.id)
            vm_response = list_virtual_machines(
                                        self.apiclient,
                                        id=virtual_machine.id,
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )
            self.assertEqual(
                             isinstance(vm_response, list),
                             True,
                             "Check for list VMs response after VM deployment"
                             )
            #Verify VM response to check whether VM deployment was successful
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

        cls.services = Services().services
        cls.api_client = fetch_api_client()

        # Get Zone, templates etc
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)

        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostypeid"]
                            )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )

        cls.services["account"] = cls.account.account.name
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                        )

        # create virtual machine
        cls.virtual_machine = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["virtual_machine"],
                                    templateid=template.id,
                                    accountid=cls.account.account.name,
                                    domainid=cls.account.account.domainid,
                                    serviceofferingid=cls.service_offering.id,
                                    )
        #Stop virtual machine
        cls.virtual_machine.stop(cls.api_client)

        timeout = cls.services["timeout"]        
        #Wait before server has be successfully stopped
        time.sleep(cls.services["sleep"])
        
        while True:
            list_volume = list_volumes(
                                   cls.api_client,
                                   virtualmachineid=cls.virtual_machine.id,
                                   type='ROOT',
                                   listall=True
                                   )
            if isinstance(list_volume, list):
                break
            elif timeout == 0:
                raise Exception("List volumes failed.")
            
            time.sleep(5)
            timeout = timeout -1
            
        cls.volume = list_volume[0]

        #Create template from volume
        cls.template = Template.create(
                                         cls.api_client,
                                         cls.services["template"],
                                         cls.volume.id
                                         )
        cls._cleanup = [
                        cls.service_offering,
                        cls.account,
                        ]

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = fetch_api_client()
            #Cleanup created resources such as templates and VMs
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
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

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
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    )
        
        self.debug("creating an instance with template ID: %s" % self.template.id)
        self.cleanup.append(virtual_machine)
        vm_response = list_virtual_machines(
                                        self.apiclient,
                                        id=virtual_machine.id,
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )
        #Verify VM response to check whether VM deployment was successful
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

    def test_02_copy_template(self):
        """Test for copy template from one zone to another"""

        # Validate the following
        # 1. copy template should be successful and
        #    secondary storage should contain new copied template.
        
        self.debug(
            "Copying template from zone: %s to %s" % (
                                                self.template.id,
                                                self.services["destzoneid"]
                                                ))
        cmd = copyTemplate.copyTemplateCmd()
        cmd.id = self.template.id
        cmd.destzoneid = self.services["destzoneid"]
        cmd.sourcezoneid = self.zone.id
        self.apiclient.copyTemplate(cmd)

        # Verify template is copied to another zone using ListTemplates
        list_template_response = list_templates(
                                    self.apiclient,
                                    templatefilter=\
                                    self.services["templatefilter"],
                                    id=self.template.id,
                                    zoneid=self.services["destzoneid"]
                                    )
        self.assertEqual(
                        isinstance(list_template_response, list),
                        True,
                        "Check for list template response return valid list"
                        )
        
        self.assertNotEqual(
                            len(list_template_response),
                            0,
                            "Check template extracted in List Templates"
                        )

        template_response = list_template_response[0]
        self.assertEqual(
                            template_response.id,
                            self.template.id,
                            "Check ID of the downloaded template"
                        )
        self.assertEqual(
                            template_response.zoneid,
                            self.services["destzoneid"],
                            "Check zone ID of the copied template"
                        )

        # Cleanup- Delete the copied template
        cmd = deleteTemplate.deleteTemplateCmd()
        cmd.id = self.template.id
        cmd.zoneid = self.services["destzoneid"]
        self.apiclient.deleteTemplate(cmd)
        return

    def test_03_delete_template(self):
        """Test Delete template
        """

        # Validate the following:
        # 1. Create a template and verify it is shown in list templates response
        # 2. Delete the created template and again verify list template response

        # Verify template response for updated attributes
        list_template_response = list_templates(
                                    self.apiclient,
                                    templatefilter=\
                                    self.services["template"]["templatefilter"],
                                    id=self.template.id,
                                    zoneid=self.zone.id
                                    )
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
                            "Check display text of updated template"
                        )
        
        self.debug("Deleting template: %s" % self.template)
        # Delete the template
        self.template.delete(self.apiclient)
        self.debug("Delete template: %s successful" % self.template)
        
        list_template_response = list_templates(
                                    self.apiclient,
                                    templatefilter=\
                                    self.services["template"]["templatefilter"],
                                    id=self.template.id,
                                    zoneid=self.zone.id
                                    )
        self.assertEqual(
                            list_template_response,
                            None,
                            "Check template available in List Templates"
                        )
        return

    def test_04_template_from_snapshot(self):
        """Create Template from snapshot
        """

        # Validate the following
        # 2. Snapshot the Root disk
        # 3. Create Template from snapshot
        # 4. Deploy Virtual machine using this template
        # 5. VM should be in running state

        volumes = list_volumes(
                        self.apiclient,
                        virtualmachineid=self.virtual_machine.id,
                        type='ROOT',
                        listall=True
                        )
        volume = volumes[0]
        
        self.debug("Creating a snapshot from volume: %s" % volume.id)
        #Create a snapshot of volume
        snapshot = Snapshot.create(
                                   self.apiclient,
                                   volume.id,
                                   account=self.account.account.name,
                                   domainid=self.account.account.domainid
                                   )
        self.debug("Creating a template from snapshot: %s" % snapshot.id)
        # Generate template from the snapshot
        template = Template.create_from_snapshot(
                                    self.apiclient,
                                    snapshot,
                                    self.services["template"]
                                    )
        self.cleanup.append(template)
        # Verify created template
        templates = list_templates(
                                self.apiclient,
                                templatefilter=\
                                self.services["template"]["templatefilter"],
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
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    templateid=template.id,
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    )
        self.cleanup.append(virtual_machine)

        vm_response = list_virtual_machines(
                                        self.apiclient,
                                        id=virtual_machine.id,
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )
        self.assertEqual(
                        isinstance(vm_response, list),
                        True,
                        "Check for list VM response return valid list"
                        )

        #Verify VM response to check whether VM deployment was successful
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
