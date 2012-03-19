# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#
""" BVT tests for Templates ISO
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
import datetime


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
                                    "cpuspeed": 100,    # in MHz
                                    "memory": 64,       # In MBs
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
                         "template_1": {
                                "displaytext": "Cent OS Template",
                                "name": "Cent OS Template",
                                "ostypeid": 12,
                         },
                         "template_2": {
                                "displaytext": "Public Template",
                                "name": "Public template",
                                "ostypeid": 12,
                                "isfeatured": True,
                                "ispublic": True,
                                "isextractable": True,
                                "mode": "HTTP_DOWNLOAD",
                         },
                        "templatefilter": 'self',
                        "destzoneid": 5,
                        # For Copy template (Destination zone)
                        "isfeatured": True,
                        "ispublic": True,
                        "isextractable": False,
                        "bootable": True,
                        "passwordenabled": True,
                        "ostypeid": 12,
                        "mode": 'advanced',
                        # Networking mode: Advanced, basic
                        "sleep": 30,
                        "timeout": 10,
                     }


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
        cls.disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering"]
                                    )
        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostypeid"]
                            )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["volume"]["diskoffering"] = cls.disk_offering.id
        cls.services["volume"]["zoneid"] = cls.zone.id
        cls.services["sourcezoneid"] = cls.zone.id

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
        #create virtual machine
        cls.virtual_machine = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["virtual_machine"],
                                    templateid=template.id,
                                    accountid=cls.account.account.name,
                                    domainid=cls.account.account.domainid,
                                    serviceofferingid=cls.service_offering.id,
                                    mode=cls.services["mode"]
                                    )

        #Stop virtual machine
        cls.virtual_machine.stop(cls.api_client)

        # Poll listVM to ensure VM is stopped properly
        timeout = cls.services["timeout"]
        while True:
            time.sleep(cls.services["sleep"])

            # Ensure that VM is in stopped state
            list_vm_response = list_virtual_machines(
                                            cls.api_client,
                                            id=cls.virtual_machine.id
                                            )

            if isinstance(list_vm_response, list):

                vm = list_vm_response[0]
                if vm.state == 'Stopped':
                    break

            if timeout == 0:
                    raise Exception(
                        "Failed to stop VM (ID: %s) in change service offering" %
                                                                        vm.id)

            timeout = timeout - 1

        list_volume = list_volumes(
                                   cls.api_client,
                                   virtualmachineid=cls.virtual_machine.id,
                                   type='ROOT'
                                   )

        cls.volume = list_volume[0]
        cls._cleanup = [
                        cls.account,
                        cls.service_offering,
                        cls.disk_offering,
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
        # 1. database (vm_template table) should be updated
        #    with newly created template
        # 2. UI should show the newly added template
        # 3. ListTemplates API should show the newly added template

        #Create template from Virtual machine and Volume ID
        template = Template.create(
                                self.apiclient,
                                self.services["template_1"],
                                self.volume.id,
                                account=self.account.account.name,
                                domainid=self.account.account.domainid
                                )
        self.cleanup.append(template)

        self.debug("Created template with ID: %s" % template.id)

        list_template_response = list_templates(
                                    self.apiclient,
                                    templatefilter=\
                                    self.services["templatefilter"],
                                    id=template.id
                                    )

        self.assertEqual(
                            isinstance(list_template_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        #Verify template response to check whether template added successfully
        self.assertNotEqual(
                            len(list_template_response),
                            0,
                            "Check template available in List Templates"
                        )
        template_response = list_template_response[0]

        self.assertEqual(
                            template_response.displaytext,
                            self.services["template_1"]["displaytext"],
                            "Check display text of newly created template"
                        )
        name = template_response.name
        self.assertEqual(
                            name.count(self.services["template_1"]["name"]),
                            1,
                            "Check name of newly created template"
                        )
        self.assertEqual(
                            template_response.ostypeid,
                            self.services["template_1"]["ostypeid"],
                            "Check osTypeID of newly created template"
                        )
        return


class TestTemplates(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.services = Services().services
        cls.api_client = fetch_api_client()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering"]
                                    )
        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostypeid"]
                            )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["volume"]["diskoffering"] = cls.disk_offering.id
        cls.services["volume"]["zoneid"] = cls.zone.id
        cls.services["template_2"]["zoneid"] = cls.zone.id
        cls.services["sourcezoneid"] = cls.zone.id

        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain.id
                            )

        cls.user = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )

        cls.services["account"] = cls.account.account.name

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                        )
        #create virtual machine
        cls.virtual_machine = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["virtual_machine"],
                                    templateid=template.id,
                                    accountid=cls.account.account.name,
                                    domainid=cls.account.account.domainid,
                                    serviceofferingid=cls.service_offering.id,
                                    mode=cls.services["mode"]
                                    )
        #Stop virtual machine
        cls.virtual_machine.stop(cls.api_client)

        # Poll listVM to ensure VM is stopped properly
        timeout = cls.services["timeout"]
        while True:
            time.sleep(cls.services["sleep"])

            # Ensure that VM is in stopped state
            list_vm_response = list_virtual_machines(
                                            cls.api_client,
                                            id=cls.virtual_machine.id
                                            )

            if isinstance(list_vm_response, list):

                vm = list_vm_response[0]
                if vm.state == 'Stopped':
                    break

            if timeout == 0:
                    raise Exception(
                        "Failed to stop VM (ID: %s) in change service offering" %
                                                                        vm.id)

            timeout = timeout - 1

        list_volume = list_volumes(
                                   cls.api_client,
                                   virtualmachineid=cls.virtual_machine.id,
                                   type='ROOT'
                                   )
        try:
            cls.volume = list_volume[0]
        except Exception as e:
            raise Exception(
                "Exception: Unable to find root volume foe VM: %s" %
                                                    cls.virtual_machine.id)

        #Create templates for Edit, Delete & update permissions testcases
        cls.template_1 = Template.create(
                                         cls.api_client,
                                         cls.services["template_1"],
                                         cls.volume.id,
                                         account=cls.account.account.name,
                                         domainid=cls.account.account.domainid
                                         )
        cls.template_2 = Template.create(
                                         cls.api_client,
                                         cls.services["template_2"],
                                         cls.volume.id,
                                         account=cls.account.account.name,
                                         domainid=cls.account.account.domainid
                                         )
        cls._cleanup = [
                        cls.service_offering,
                        cls.disk_offering,
                        cls.account,
                        cls.user
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

            self.dbclient.close()
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def test_02_edit_template(self):
        """Test Edit template
        """

        # Validate the following:
        # 1. UI should show the edited values for template
        # 2. database (vm_template table) should have updated values

        new_displayText = random_gen()
        new_name = random_gen()

        cmd = updateTemplate.updateTemplateCmd()
        # Update template attributes
        cmd.id = self.template_1.id
        cmd.displaytext = new_displayText
        cmd.name = new_name
        cmd.bootable = self.services["bootable"]
        cmd.passwordenabled = self.services["passwordenabled"]

        self.apiclient.updateTemplate(cmd)

        self.debug("Edited template with new name: %s" % new_name)
        
        # Sleep to ensure update reflected across all the calls
        time.sleep(self.services["sleep"])
        
        timeout = self.services["timeout"]
        while True:
            # Verify template response for updated attributes
            list_template_response = list_templates(
                                    self.apiclient,
                                    templatefilter=\
                                    self.services["templatefilter"],
                                    id=self.template_1.id,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                    )
            if isinstance(list_template_response, list):
                break
            elif timeout == 0:
                raise Exception("List Template failed!")

            time.sleep(10)
            timeout = timeout -1
            
        self.assertEqual(
                            isinstance(list_template_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            len(list_template_response),
                            0,
                            "Check template available in List Templates"
                        )
        template_response = list_template_response[0]
        
        self.debug("New Name: %s" % new_displayText)
        self.debug("Name in Template response: %s"
                                % template_response.displaytext)
        self.assertEqual(
                            template_response.displaytext,
                            new_displayText,
                            "Check display text of updated template"
                        )
        self.assertEqual(
                            template_response.name,
                            new_name,
                            "Check name of updated template"
                        )
        self.assertEqual(
                            str(template_response.passwordenabled).lower(),
                            str(self.services["passwordenabled"]).lower(),
                            "Check passwordenabled field of updated template"
                        )
        self.assertEqual(
                            template_response.ostypeid,
                            self.services["ostypeid"],
                            "Check OSTypeID of updated template"
                        )
        return

    def test_03_delete_template(self):
        """Test delete template
        """

        # Validate the following:
        # 1. UI should not show the deleted template
        # 2. database (vm_template table) should not contain deleted template

        self.debug("Deleting Template ID: %s" % self.template_1.id)

        self.template_1.delete(self.apiclient)

        list_template_response = list_templates(
                                    self.apiclient,
                                    templatefilter=\
                                    self.services["templatefilter"],
                                    id=self.template_1.id,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                    )
        # Verify template is deleted properly using ListTemplates
        self.assertEqual(
                         list_template_response,
                         None,
                         "Check if template exists in List Templates"
                         )
        return

    def test_04_extract_template(self):
        "Test for extract template"

        # Validate the following
        # 1. Admin should able  extract and download the templates
        # 2. ListTemplates should display all the public templates
        #    for all kind of users
        # 3 .ListTemplates should not display the system templates

        self.debug("Extracting template with ID: %s" % self.template_2.id)

        cmd = extractTemplate.extractTemplateCmd()
        cmd.id = self.template_2.id
        cmd.mode = self.services["template_2"]["mode"]
        cmd.zoneid = self.zone.id
        list_extract_response = self.apiclient.extractTemplate(cmd)

        try:
            # Format URL to ASCII to retrieve response code
            formatted_url = urllib.unquote_plus(list_extract_response.url)
            url_response = urllib.urlopen(formatted_url)
            response_code = url_response.getcode()
        
        except Exception:
            self.fail(
                "Extract Template Failed with invalid URL %s (template id: %s)" \
                % (formatted_url, self.template_2.id)
            )
        self.assertEqual(
                            list_extract_response.id,
                            self.template_2.id,
                            "Check ID of the extracted template"
                        )
        self.assertEqual(
                            list_extract_response.extractMode,
                            self.services["template_2"]["mode"],
                            "Check mode of extraction"
                        )
        self.assertEqual(
                            list_extract_response.zoneid,
                            self.services["template_2"]["zoneid"],
                            "Check zone ID of extraction"
                        )
        self.assertEqual(
                         response_code,
                         200,
                         "Check for a valid response download URL"
                         )
        return

    def test_05_template_permissions(self):
        """Update & Test for template permissions"""

        # Validate the following
        # 1. listTemplatePermissions returns valid
        #    permissions set for template
        # 2. permission changes should be reflected in vm_template
        #    table in database

        self.debug("Updating Template permissions ID:%s" % self.template_2.id)

        cmd = updateTemplatePermissions.updateTemplatePermissionsCmd()
        # Update template permissions
        cmd.id = self.template_2.id
        cmd.isfeatured = self.services["isfeatured"]
        cmd.ispublic = self.services["ispublic"]
        cmd.isextractable = self.services["isextractable"]
        self.apiclient.updateTemplatePermissions(cmd)

        list_template_response = list_templates(
                                    self.apiclient,
                                    templatefilter='featured',
                                    id=self.template_2.id,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                    )
        self.assertEqual(
                            isinstance(list_template_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        # Verify template response for updated permissions for normal user
        template_response = list_template_response[0]

        self.assertEqual(
                            template_response.id,
                            self.template_2.id,
                            "Check template ID"
                        )
        self.assertEqual(
                            template_response.ispublic,
                            int(True),
                            "Check ispublic permission of template"
                        )

        self.assertNotEqual(
                        template_response.templatetype,
                        'SYSTEM',
                        "ListTemplates should not list any system templates"
                        )
        return

    def test_06_copy_template(self):
        """Test for copy template from one zone to another"""

        # Validate the following
        # 1. copy template should be successful and
        #    secondary storage should contain new copied template.

        self.debug("Copy template from Zone: %s to %s" % (
                                            self.services["sourcezoneid"],
                                            self.services["destzoneid"]
                                            ))
        cmd = copyTemplate.copyTemplateCmd()
        cmd.id = self.template_2.id
        cmd.destzoneid = self.services["destzoneid"]
        cmd.sourcezoneid = self.services["sourcezoneid"]
        self.apiclient.copyTemplate(cmd)

        # Verify template is copied to another zone using ListTemplates
        list_template_response = list_templates(
                                    self.apiclient,
                                    templatefilter=\
                                    self.services["templatefilter"],
                                    id=self.template_2.id,
                                    zoneid=self.services["destzoneid"]
                                    )
        self.assertEqual(
                            isinstance(list_template_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            len(list_template_response),
                            0,
                            "Check template extracted in List Templates"
                        )

        template_response = list_template_response[0]
        self.assertEqual(
                            template_response.id,
                            self.template_2.id,
                            "Check ID of the downloaded template"
                        )
        self.assertEqual(
                            template_response.zoneid,
                            self.services["destzoneid"],
                            "Check zone ID of the copied template"
                        )

        # Cleanup- Delete the copied template
        cmd = deleteTemplate.deleteTemplateCmd()
        cmd.id = template_response.id
        cmd.zoneid = self.services["destzoneid"]
        self.apiclient.deleteTemplate(cmd)
        return

    def test_07_list_public_templates(self):
        """Test only public templates are visible to normal user"""

        # Validate the following
        # 1. ListTemplates should show only 'public' templates for normal user

        list_template_response = list_templates(
                                    self.apiclient,
                                    templatefilter='featured',
                                    account=self.user.account.name,
                                    domainid=self.user.account.domainid
                                    )
        self.assertEqual(
                            isinstance(list_template_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            len(list_template_response),
                            0,
                            "Check template available in List Templates"
                        )
        #Template response should list all 'public' templates
        for template in list_template_response:
            self.assertEqual(
                            template.ispublic,
                            True,
                            "ListTemplates should list only public templates"
                        )
        return

    def test_08_list_system_templates(self):
        """Test System templates are not visible to normal user"""

        # Validate the following
        # 1. ListTemplates should not show 'SYSTEM' templates for normal user

        list_template_response = list_templates(
                                    self.apiclient,
                                    templatefilter='featured',
                                    account=self.user.account.name,
                                    domainid=self.user.account.domainid
                                    )
        self.assertEqual(
                            isinstance(list_template_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            len(list_template_response),
                            0,
                            "Check template available in List Templates"
                        )

        for template in list_template_response:
            self.assertNotEqual(
                        template.templatetype,
                        'SYSTEM',
                        "ListTemplates should not list any system templates"
                        )
        return
