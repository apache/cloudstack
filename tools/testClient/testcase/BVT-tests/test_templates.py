# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#
""" BVT tests for Templates ISO
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from utils import *
from base import *
import urllib
from random import random
#Import System modules
import time

class Services:
    """Test Templates Services
    """

    def __init__(self):
        self.services = {
                            "virtual_machine":
                                        {
                                            "template": 206, # Template used for VM creation
                                            "zoneid": 1,
                                            "serviceoffering": 1,
                                            "displayname": "testVM",
                                            "hypervisor": 'XenServer',
                                            "account": 'admin', # Account for which VM should be created
                                            "domainid": 1,
                                            "protocol": 'TCP',
                                         },
                            "volume":
                                        {
                                            "offerings": 1,
                                            "volumeoffering": 3,
                                            "diskname": "TestVolumeTemplate",
                                            "zoneid": 1,
                                            "diskofferingid": 3,
                                         },
                            "template_1":
                                        {
                                            "displaytext": "Test Template Type 1",
                                            "name": "testTemplate",
                                            "ostypeid": 12,
                                         },
                            "template_2":
                                        {
                                            "displaytext": "Test Template Type 2",
                                            "name": "testTemplate",
                                            "ostypeid": 12,
                                            "isfeatured": True,
                                            "ispublic": True,
                                            "isextractable": True,
                                            "mode": "HTTP_DOWNLOAD",
                                            "zoneid": 1,
                                         },
                            "templatefilter": 'self',
                            "destzoneid": 2, # For Copy template (Destination zone)
                            "sourcezoneid": 1, # For Copy template (Source zone)
                            "isfeatured": True,
                            "ispublic": True,
                            "isextractable": False,
                            "bootable": True,
                            "passwordenabled": True,
                            "ostypeid": 15,
                            "account": 'testuser', # Normal user
                            "domainid": 1,
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
        #create virtual machine
        cls.virtual_machine = VirtualMachine.create(
                                        cls.api_client,
                                        cls.services["virtual_machine"]
                                        )

        #Stop virtual machine
        cmd = stopVirtualMachine.stopVirtualMachineCmd()
        cmd.id = cls.virtual_machine.id
        cls.api_client.stopVirtualMachine(cmd)

        #Wait before server has be successfully stopped
        time.sleep(30)
        cmd = listVolumes.listVolumesCmd()
        cmd.virtualmachineid = cls.virtual_machine.id
        cmd.type = 'ROOT'
        list_volume = cls.api_client.listVolumes(cmd)
        cls.volume = list_volume[0]
        cls._cleanup = [cls.virtual_machine]
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
        # 1. database (vm_template table) should be updated with newly created template
        # 2. UI should show the newly added template
        # 3. ListTemplates API should show the newly added template

        #Create template from Virtual machine and Volume ID
        template = Template.create(self.apiclient, self.volume, self.services["template_1"])
        self.cleanup.append(template)

        cmd = listTemplates.listTemplatesCmd()
        cmd.templatefilter = self.services["templatefilter"]
        cmd.id = template.id
        list_template_response = self.apiclient.listTemplates(cmd)

        #Verify template response to check whether template added successfully or not
        template_response = list_template_response[0]
        self.assertNotEqual(
                            len(list_template_response),
                            0,
                            "Check template avaliable in List Templates"
                        )

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

        #Verify the database entry for template
        self.debug(
                   "select name, display_text, guest_os_id from vm_template where id = %s;"
                   % template.id
                   )

        qresultset = self.dbclient.execute(
                                           "select name, display_text, guest_os_id from vm_template where id = %s;"
                                           % template.id
                                           )

        self.assertNotEqual(
                                len(qresultset),
                                0,
                                "Check DB Query result set"
                            )

        qresult = qresultset[0]

        name = qresult[0]
        self.assertEqual(
                            name.count(self.services["template_1"]["name"]),
                            1,
                            "Compare template name with database record"
                        )

        self.assertEqual(
                            qresult[1],
                            self.services["template_1"]["displaytext"],
                            "Compare template display text with database record"
                        )

        self.assertEqual(
                            qresult[2],
                            self.services["template_1"]["ostypeid"],
                            "Compare template osTypeID with database record"
                        )
        return

class TestTemplates(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.services = Services().services
        cls.api_client = fetch_api_client()
        #create virtual machines
        cls.virtual_machine = VirtualMachine.create(
                                        cls.api_client,
                                        cls.services["virtual_machine"]
                                        )

        #Stop virtual machine
        cmd = stopVirtualMachine.stopVirtualMachineCmd()
        cmd.id = cls.virtual_machine.id
        cls.api_client.stopVirtualMachine(cmd)

        #Wait before server has be successfully stopped
        time.sleep(30)
        cmd = listVolumes.listVolumesCmd()
        cmd.virtualmachineid = cls.virtual_machine.id
        cmd.type = 'ROOT'
        list_volume = cls.api_client.listVolumes(cmd)
        cls.volume = list_volume[0]

        #Create templates for Edit, Delete & update permissions testcases
        cls.template_1 = Template.create(cls.api_client, cls.volume, cls.services["template_1"])
        cls.template_2 = Template.create(cls.api_client, cls.volume, cls.services["template_2"])
        cls._cleanup = [cls.template_2, cls.virtual_machine]

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
        cmd.ostypeid = self.services["ostypeid"]

        self.apiclient.updateTemplate(cmd)

        # Verify template response for updated attributes
        cmd = listTemplates.listTemplatesCmd()
        cmd.templatefilter = self.services["templatefilter"]
        cmd.id = self.template_1.id

        list_template_response = self.apiclient.listTemplates(cmd)
        template_response = list_template_response[0]

        self.assertNotEqual(
                            len(list_template_response),
                            0,
                            "Check template available in List Templates"
                        )

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
        # Verify database entry for updated template attributes
        self.debug(
                   "select name, display_text, bootable, enable_password, guest_os_id from vm_template where id = %s;"
                   % self.template_1.id
                   )

        qresultset = self.dbclient.execute(
                                           "select name, display_text, bootable, enable_password, guest_os_id  from vm_template where id = %s;"
                                           % self.template_1.id
                                           )

        self.assertNotEqual(
                                len(qresultset),
                                0,
                                "Check DB Query result set"
                            )

        qresult = qresultset[0]

        self.assertEqual(
                            qresult[0],
                            new_name,
                            "Compare template name with database record"
                        )

        self.assertEqual(
                            qresult[1],
                            new_displayText,
                            "Compare template bootable field with database record"
                        )
        self.assertEqual(
                            qresult[2],
                            int(self.services["bootable"]),
                            "Compare template enable_password field with database record"
                        )
        self.assertEqual(
                            qresult[3],
                            int(self.services["passwordenabled"]),
                            "Compare template display text with database record"
                        )
        self.assertEqual(
                            qresult[4],
                            self.services["ostypeid"],
                            "Compare template guest OS ID with database record"
                        )
        return

    def test_03_delete_template(self):
        """Test delete template
        """

         # Validate the following:
         # 1. UI should not show the deleted template
         # 2. database (vm_template table) should not contain deleted template

        self.template_1.delete(self.apiclient)

        cmd = listTemplates.listTemplatesCmd()
        cmd.templatefilter = self.services["templatefilter"]
        cmd.id = self.template_1.id
        list_template_response = self.apiclient.listTemplates(cmd)

        # Verify template is deleted properly using ListTemplates
        self.assertEqual(list_template_response, None, "Check if template exists in List Templates")

        # Verify database entry is removed for deleted template
        self.debug(
                   "select name, display_text from vm_template where id = %s;"
                   % self.template_1.id
                   )

        qresultset = self.dbclient.execute(
                                           "select name, display_text  from vm_template where id = %s;"
                                           % self.template_1.id
                                           )

        self.assertEqual(
                                len(qresultset),
                                1,
                                "Check DB Query result set"
                            )

        return

    def test_04_extract_template(self):
        "Test for extract template"

         # Validate the following
         # 1. Admin should able  extract and download the templates
         # 2. ListTemplates should display all the public templates for all kind of users
         # 3 .ListTemplates should not display the system templates

        cmd = extractTemplate.extractTemplateCmd()
        cmd.id = self.template_2.id
        cmd.mode = self.services["template_2"]["mode"]
        cmd.zoneid = self.services["template_2"]["zoneid"]
        list_extract_response = self.apiclient.extractTemplate(cmd)

        # Format URL to ASCII to retrieve response code
        formatted_url = urllib.unquote_plus(list_extract_response.url)
        url_response = urllib.urlopen(formatted_url)
        response_code = url_response.getcode()

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
         # 1. listTemplatePermissions returns valid permissions set for template
         # 2. permission changes should be reflected in vm_template table in database

        cmd = updateTemplatePermissions.updateTemplatePermissionsCmd()
        # Update template permissions
        cmd.id = self.template_2.id
        cmd.isfeatured = self.services["isfeatured"]
        cmd.ispublic = self.services["ispublic"]
        cmd.isextractable = self.services["isextractable"]

        self.apiclient.updateTemplatePermissions(cmd)

        cmd = listTemplates.listTemplatesCmd()
        cmd.id = self.template_2.id
        cmd.account = self.services["account"]
        cmd.domainid = self.services["domainid"]
        cmd.templatefilter = 'featured'
        list_template_response = self.apiclient.listTemplates(cmd)

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

        # Verify database entries for updated permissions
        self.debug(
                   "select public, featured, extractable from vm_template where id = %s;"
                   % self.template_2.id
                   )
        qresultset = self.dbclient.execute(
                                           "select public, featured, extractable from vm_template where id = %s;"
                                           % self.template_2.id
                                           )

        self.assertNotEqual(
                                len(qresultset),
                                0,
                                "Check DB Query result set"
                            )

        qresult = qresultset[0]

        self.assertEqual(
                            qresult[0],
                            int(self.services["ispublic"]),
                            "Compare public permission with database record"
                        )

        self.assertEqual(
                            qresult[1],
                            int(self.services["isfeatured"]),
                            "Compare featured permission with database record"
                        )
        self.assertEqual(
                            qresult[2],
                            int(self.services["isextractable"]),
                            "Compare extractable permission with database record"
                        )
        return

    def test_06_copy_template(self):
        """Test for copy template from one zone to another"""

        # Validate the following
        # 1. copy template should be successful and secondary storage should contain new copied template.

        cmd = copyTemplate.copyTemplateCmd()
        cmd.id = self.template_2.id
        cmd.destzoneid = self.services["destzoneid"]
        cmd.sourcezoneid = self.services["sourcezoneid"]
        self.apiclient.copyTemplate(cmd)

        # Verify template is copied to another zone using ListTemplates
        cmd = listTemplates.listTemplatesCmd()
        cmd.id = self.template_2.id
        cmd.templatefilter = self.services["templatefilter"]
        list_template_response = self.apiclient.listTemplates(cmd)

        template_response = list_template_response[0]
        self.assertNotEqual(
                            len(list_template_response),
                            0,
                            "Check template extracted in List Templates"
                        )
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
        return

    def test_07_list_public_templates(self):
        """Test only public templates are visible to normal user"""

        # Validate the following
        # 1. ListTemplates should show only 'public' templates for normal user

        cmd = listTemplates.listTemplatesCmd()
        cmd.templatefilter = 'featured'
        cmd.account = self.services["account"]
        cmd.domainid = self.services["domainid"]

        list_template_response = self.apiclient.listTemplates(cmd)

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

        cmd = listTemplates.listTemplatesCmd()
        cmd.templatefilter = 'featured'
        cmd.account = self.services["account"]
        cmd.domainid = self.services["domainid"]

        list_template_response = self.apiclient.listTemplates(cmd)

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
