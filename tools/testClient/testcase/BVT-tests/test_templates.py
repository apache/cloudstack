# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#
""" BVT tests for Templates ISO
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from settings import *
import remoteSSHClient
from utils import *
from base import *
import urllib
from random import random
#Import System modules
import time


services = TEST_TEMPLATE_SERVICES

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
            raise Exception("Warning: Exception during cleanup : %s" %e)
        return

    @classmethod
    def setUpClass(cls):

        cls.api_client = fetch_api_client()
        #create virtual machine
        cls.virtual_machine = VirtualMachine.create(
                                        cls.api_client,
                                        services["virtual_machine"]
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
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = fetch_api_client()
            #Cleanup resources used
            cls.virtual_machine.delete(cls.api_client)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" %e)

        return

    def test_01_create_template(self):
        """Test create public & private template
        """

        # Validate the following:
        # 1. database (vm_template table) should be updated with newly created template
        # 2. UI should show the newly added template
        # 3. ListTemplates API should show the newly added template

        #Create template from Virtual machine and Volume ID
        template = Template.create(self.apiclient, self.volume, services["template_1"])
        self.cleanup.append(template)

        cmd = listTemplates.listTemplatesCmd()
        cmd.templatefilter = services["templatefilter"]
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
                            services["template_1"]["displaytext"],
                            "Check display text of newly created template"
                        )
        self.assertEqual(
                            template_response.name,
                            services["template_1"]["name"],
                            "Check name of newly created template"
                        )
        self.assertEqual(
                            template_response.ostypeid,
                            services["template_1"]["ostypeid"],
                            "Check osTypeID of newly created template"
                        )

        #Verify the database entry for template
        self.debug(
                   "select name, display_text, guest_os_id from vm_template where id = %s;"
                   %template.id
                   )

        qresultset = self.dbclient.execute(
                                           "select name, display_text, guest_os_id from vm_template where id = %s;"
                                           %template.id
                                           )

        self.assertNotEqual(
                                len(qresultset),
                                0,
                                "Check DB Query result set"
                            )

        qresult = qresultset[0]

        self.assertEqual(
                            qresult[0],
                            services["template_1"]["name"],
                            "Compare template name with database record"
                        )

        self.assertEqual(
                            qresult[1],
                            services["template_1"]["displaytext"],
                            "Compare template display text with database record"
                        )

        self.assertEqual(
                            qresult[2],
                            services["template_1"]["ostypeid"],
                            "Compare template osTypeID with database record"
                        )
        return

class TestTemplates(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.api_client = fetch_api_client()
        #create virtual machines
        cls.virtual_machine = VirtualMachine.create(
                                        cls.api_client,
                                        services["virtual_machine"]
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
        cls.template_1 = Template.create(cls.api_client, cls.volume, services["template_1"])
        cls.template_2 = Template.create(cls.api_client, cls.volume, services["template_2"])

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = fetch_api_client()

            #Cleanup created resources such as templates and VMs
            cls.template_2.delete(cls.api_client)
            cls.virtual_machine.delete(cls.api_client)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" %e)

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
            raise Exception("Warning: Exception during cleanup : %s" %e)

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
        cmd.bootable = services["bootable"]
        cmd.passwordenabled = services["passwordenabled"]
        cmd.ostypeid = services["ostypeid"]

        self.apiclient.updateTemplate(cmd)

        # Verify template response for updated attributes
        cmd = listTemplates.listTemplatesCmd()
        cmd.templatefilter = services["templatefilter"]
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
                            str(services["passwordenabled"]).lower(),
                            "Check passwordenabled field of updated template"
                        )
        self.assertEqual(
                            template_response.ostypeid,
                            services["ostypeid"],
                            "Check OSTypeID of updated template"
                        )
        # Verify database entry for updated template attributes
        self.debug(
                   "select name, display_text, bootable, enable_password, guest_os_id from vm_template where id = %s;"
                   %self.template_1.id
                   )

        qresultset = self.dbclient.execute(
                                           "select name, display_text, bootable, enable_password, guest_os_id  from vm_template where id = %s;"
                                           %self.template_1.id
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
                            int(services["bootable"]),
                            "Compare template enable_password field with database record"
                        )
        self.assertEqual(
                            qresult[3],
                            int(services["passwordenabled"]),
                            "Compare template display text with database record"
                        )
        self.assertEqual(
                            qresult[4],
                            services["ostypeid"],
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
        cmd.templatefilter = services["templatefilter"]
        cmd.id = self.template_1.id
        list_template_response = self.apiclient.listTemplates(cmd)

        # Verify template is deleted properly using ListTemplates
        self.assertEqual(list_template_response, None, "Check if template exists in List Templates")

        # Verify database entry is removed for deleted template
        self.debug(
                   "select name, display_text from vm_template where id = %s;"
                   %self.template_1.id
                   )

        qresultset = self.dbclient.execute(
                                           "select name, display_text  from vm_template where id = %s;"
                                           %self.template_1.id
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
        cmd.mode = services["template_2"]["mode"]
        cmd.zoneid = services["template_2"]["zoneid"]
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
                            services["template_2"]["mode"],
                            "Check mode of extraction"
                        )
        self.assertEqual(
                            list_extract_response.zoneid,
                            services["template_2"]["zoneid"],
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
        cmd.isfeatured = services["isfeatured"]
        cmd.ispublic = services["ispublic"]
        cmd.isextractable =services["isextractable"]

        self.apiclient.updateTemplatePermissions(cmd)

        cmd = listTemplates.listTemplatesCmd()
        cmd.id = self.template_2.id
        cmd.account = services["account"]
        cmd.domainid = services["domainid"]
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
                   %self.template_2.id
                   )
        qresultset = self.dbclient.execute(
                                           "select public, featured, extractable from vm_template where id = %s;"
                                           %self.template_2.id
                                           )

        self.assertNotEqual(
                                len(qresultset),
                                0,
                                "Check DB Query result set"
                            )

        qresult = qresultset[0]

        self.assertEqual(
                            qresult[0],
                            int(services["ispublic"]),
                            "Compare public permission with database record"
                        )

        self.assertEqual(
                            qresult[1],
                            int(services["isfeatured"]),
                            "Compare featured permission with database record"
                        )
        self.assertEqual(
                            qresult[2],
                            int(services["isextractable"]),
                            "Compare extractable permission with database record"
                        )
        return

    def test_06_copy_template(self):
        """Test for copy template from one zone to another"""

        # Validate the following
        # 1. copy template should be successful and secondary storage should contain new copied template.

        cmd = copyTemplate.copyTemplateCmd()
        cmd.id = self.template_2.id
        cmd.destzoneid = services["destzoneid"]
        cmd.sourcezoneid = services["sourcezoneid"]
        self.apiclient.copyTemplate(cmd)

        # Verify template is copied to another zone using ListTemplates
        cmd = listTemplates.listTemplatesCmd()
        cmd.id = self.template_2.id
        cmd.templatefilter = services["templatefilter"]
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
                            services["destzoneid"],
                            "Check zone ID of the copied template"
                        )
        return

    def test_07_list_public_templates(self):
        """Test only public templates are visible to normal user"""

        # Validate the following
        # 1. ListTemplates should show only 'public' templates for normal user

        cmd = listTemplates.listTemplatesCmd()
        cmd.templatefilter = 'featured'
        cmd.account = services["account"]
        cmd.domainid = services["domainid"]

        list_template_response = self.apiclient.listTemplates(cmd)

        self.assertNotEqual(
                            len(list_template_response),
                            0,
                            "Check template available in List Templates"
                        )
        #Template response should list all 'public' templates
        for k in range(len(list_template_response)):
           self.assertEqual(
                            list_template_response[k].ispublic,
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
        cmd.account = services["account"]
        cmd.domainid = services["domainid"]

        list_template_response = self.apiclient.listTemplates(cmd)

        self.assertNotEqual(
                            len(list_template_response),
                            0,
                            "Check template available in List Templates"
                        )

        for k in range(len(list_template_response)):
           self.assertNotEqual(
                            list_template_response[k].templatetype,
                            'SYSTEM',
                            "ListTemplates should not list any system templates"
                        )
        return
