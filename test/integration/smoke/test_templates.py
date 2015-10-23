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
""" BVT tests for Templates ISO
"""
#Import Local Modules
from marvin.codes import FAILED
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.utils import random_gen, cleanup_resources
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             DiskOffering,
                             Template,
                             Volume,
                             Zone)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
from nose.plugins.attrib import attr
import urllib
#Import System modules
import time
from marvin.cloudstackAPI import (createTemplate, listOsTypes)

_multiprocess_shared_ = True

# Function to create template with name existing in test_data without any extensions


def create(apiclient, services, volumeid=None, account=None, domainid=None, projectid=None):
    cmd = createTemplate.createTemplateCmd()
    cmd.displaytext = services["displaytext"]
    cmd.name = services["name"]
    if "ostypeid" in services:
        cmd.ostypeid = services["ostypeid"]
    elif "ostype" in services:
        sub_cmd = listOsTypes.listOsTypesCmd()
        sub_cmd.description = services["ostype"]
        ostypes = apiclient.listOsTypes(sub_cmd)

        if not isinstance(ostypes, list):
            raise Exception(
                "Unable to find Ostype id with desc: %s" % services["ostype"]
            )
        cmd.ostypeid = ostypes[0].id
    else:
        raise Exception(
            "Unable to find Ostype is required for creating template")

    cmd.isfeatured = services[
        "isfeatured"] if "isfeatured" in services else False

    cmd.ispublic = services[
        "ispublic"] if "ispublic" in services else False
    cmd.isextractable = services[
        "isextractable"] if "isextractable" in services else False
    cmd.passwordenabled = services[
        "passwordenabled"] if "passwordenabled" in services else False

    if volumeid:
        cmd.volumeid = volumeid
    if account:
        cmd.account = account
    if domainid:
        cmd.domainid = domainid
    if projectid:
        cmd.projectid = projectid
    return apiclient.createTemplate(cmd)


class TestCreateTemplate(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.unsupportedHypervisor:
            self.skipTest("Skipping test because unsupported hypervisor\
                    %s" % self.hypervisor)
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
        testClient = super(TestCreateTemplate, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls._cleanup = []
        cls.services = testClient.getParsedTestDataConfig()
        cls.unsupportedHypervisor = False
        cls.hypervisor = testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['lxc']:
            # Template creation from root volume is not supported in LXC
            cls.unsupportedHypervisor = True
            return

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        try:
            cls.disk_offering = DiskOffering.create(
                                    cls.apiclient,
                                    cls.services["disk_offering"]
                                    )
            cls._cleanup.append(cls.disk_offering)
            template = get_template(
                            cls.apiclient,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
            if template == FAILED:
                assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]

            cls.services["template"]["ostypeid"] = template.ostypeid
            cls.services["template_2"]["ostypeid"] = template.ostypeid
            cls.services["ostypeid"] = template.ostypeid

            cls.services["virtual_machine"]["zoneid"] = cls.zone.id
            cls.services["volume"]["diskoffering"] = cls.disk_offering.id
            cls.services["volume"]["zoneid"] = cls.zone.id
            cls.services["sourcezoneid"] = cls.zone.id
            cls.account = Account.create(
                            cls.apiclient,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
            cls._cleanup.append(cls.account)
            cls.service_offering = ServiceOffering.create(
                                            cls.apiclient,
                                            cls.services["service_offerings"]["tiny"]
                                            )
            cls._cleanup.append(cls.service_offering)
            #create virtual machine
            cls.virtual_machine = VirtualMachine.create(
                                    cls.apiclient,
                                    cls.services["virtual_machine"],
                                    templateid=template.id,
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    serviceofferingid=cls.service_offering.id,
                                    mode=cls.services["mode"]
                                    )
            #Stop virtual machine
            cls.virtual_machine.stop(cls.apiclient)

            list_volume = Volume.list(
                                   cls.apiclient,
                                   virtualmachineid=cls.virtual_machine.id,
                                   type='ROOT',
                                   listall=True
                                   )

            cls.volume = list_volume[0]
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Exception in setUpClass: %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.apiclient = super(TestCreateTemplate, cls).getClsTestClient().getApiClient()
            #Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags = ["advanced", "advancedns", "smoke"], required_hardware="false")
    def test_CreateTemplateWithDuplicateName(self):
        """Test when createTemplate is used to create templates having the same name all of them get
        different unique names so that the templates with same name does not get deleted during template sync"""

        #1. Create 2 templates with same name
        #2. check the db that the templates with same name have different unique_name

        #Create templates from Virtual machine and Volume ID
        template1 = create(self.apiclient,
                           self.services["template"],
                           self.volume.id,
                           account=self.account.name)
        template2 = create(self.apiclient,
                           self.services["template"],
                           self.volume.id,
                           account=self.account.name)

        self.debug("Created template with ID: %s" % template1.id)
        self.debug("Created template with ID: %s" % template2.id)

        self.assertEqual(
            template1.name, template2.name, "Created templates with same name")

        self.debug("select unique_name from vm_template where name='%s';"
                   % template1.name)

        #Db query to check for unique_name for the templates with same name

        qresultset = self.dbclient.execute("select unique_name from vm_template where name='%s' and removed is NULL ;"
                                          % template1.name)


        self.debug("unique_name of template1 is '%s' and unique_name of template2 is '%s'", qresultset[0],
                    qresultset[1])

        self.assertNotEqual(qresultset[0], qresultset[1],
                            "unique names are different")


    @attr(tags = ["advanced", "advancedns", "smoke"], required_hardware="false")
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
                                self.services["template"],
                                self.volume.id,
                                account=self.account.name,
                                domainid=self.account.domainid
                                )
        self.cleanup.append(template)

        self.debug("Created template with ID: %s" % template.id)

        list_template_response = Template.list(
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
                            self.services["template"]["displaytext"],
                            "Check display text of newly created template"
                        )
        name = template_response.name
        self.assertEqual(
                            name.count(self.services["template"]["name"]),
                            1,
                            "Check name of newly created template"
                        )
        self.assertEqual(
                            template_response.ostypeid,
                            self.services["template"]["ostypeid"],
                            "Check osTypeID of newly created template"
                        )
        return


class TestTemplates(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        testClient = super(TestTemplates, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls._cleanup = []
        cls.services = testClient.getParsedTestDataConfig()
        cls.unsupportedHypervisor = False
        cls.hypervisor = testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['lxc']:
            # Template creation from root volume is not supported in LXC
            cls.unsupportedHypervisor = True
            return

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        #populate second zone id for iso copy
        cls.zones = Zone.list(cls.apiclient)
        if not isinstance(cls.zones, list):
            raise Exception("Failed to find zones.")

        cls.disk_offering = DiskOffering.create(
                                    cls.apiclient,
                                    cls.services["disk_offering"]
                                    )
        template = get_template(
                            cls.apiclient,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        if template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["volume"]["diskoffering"] = cls.disk_offering.id
        cls.services["volume"]["zoneid"] = cls.zone.id
        cls.services["template_2"]["zoneid"] = cls.zone.id
        cls.services["sourcezoneid"] = cls.zone.id

        cls.services["template"]["ostypeid"] = template.ostypeid
        cls.services["template_2"]["ostypeid"] = template.ostypeid
        cls.services["ostypeid"] = template.ostypeid
        cls.account = Account.create(
                            cls.apiclient,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain.id
                            )
        cls.user = Account.create(
                            cls.apiclient,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.service_offering = ServiceOffering.create(
                                            cls.apiclient,
                                            cls.services["service_offerings"]["tiny"]
                                        )
        #create virtual machine
        cls.virtual_machine = VirtualMachine.create(
                                    cls.apiclient,
                                    cls.services["virtual_machine"],
                                    templateid=template.id,
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    serviceofferingid=cls.service_offering.id,
                                    mode=cls.services["mode"]
                                    )
        #Stop virtual machine
        cls.virtual_machine.stop(cls.apiclient)

        list_volume = Volume.list(
                                   cls.apiclient,
                                   virtualmachineid=cls.virtual_machine.id,
                                   type='ROOT',
                                   listall=True
                                   )
        try:
            cls.volume = list_volume[0]
        except Exception as e:
            raise Exception(
                "Exception: Unable to find root volume foe VM: %s - %s" %
                 (cls.virtual_machine.id, e))

        #Create templates for Edit, Delete & update permissions testcases
        cls.template_1 = Template.create(
                                         cls.apiclient,
                                         cls.services["template"],
                                         cls.volume.id,
                                         account=cls.account.name,
                                         domainid=cls.account.domainid
                                         )
        cls.template_2 = Template.create(
                                         cls.apiclient,
                                         cls.services["template_2"],
                                         cls.volume.id,
                                         account=cls.account.name,
                                         domainid=cls.account.domainid
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
            cls.apiclient = super(TestTemplates, cls).getClsTestClient().getApiClient()
            #Cleanup created resources such as templates and VMs
            cleanup_resources(cls.apiclient, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.unsupportedHypervisor:
            self.skipTest("Skipping test because unsupported hypervisor\
                    %s" % self.hypervisor)
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_02_edit_template(self):
        """Test Edit template
        """

        # Validate the following:
        # 1. UI should show the edited values for template
        # 2. database (vm_template table) should have updated values

        new_displayText = random_gen()
        new_name = random_gen()

        self.template_1.update(self.apiclient,
                               displaytext = new_displayText,
                               name = new_name,
                               bootable = self.services["bootable"],
                               passwordenabled = self.services["passwordenabled"])

        self.debug("Edited template with new name: %s" % new_name)

        # Sleep to ensure update reflected across all the calls
        time.sleep(self.services["sleep"])

        timeout = self.services["timeout"]
        while True:
            # Verify template response for updated attributes
            list_template_response = Template.list(
                                    self.apiclient,
                                    templatefilter=\
                                    self.services["templatefilter"],
                                    id=self.template_1.id,
                                    account=self.account.name,
                                    domainid=self.account.domainid
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

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_03_delete_template(self):
        """Test delete template
        """

        # Validate the following:
        # 1. UI should not show the deleted template
        # 2. database (vm_template table) should not contain deleted template

        self.debug("Deleting Template ID: %s" % self.template_1.id)

        self.template_1.delete(self.apiclient)

        list_template_response = Template.list(
                                    self.apiclient,
                                    templatefilter=\
                                    self.services["templatefilter"],
                                    id=self.template_1.id,
                                    account=self.account.name,
                                    domainid=self.account.domainid
                                    )
        # Verify template is deleted properly using ListTemplates
        self.assertEqual(
                         list_template_response,
                         None,
                         "Check if template exists in List Templates"
                         )
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_04_extract_template(self):
        "Test for extract template"

        # Validate the following
        # 1. Admin should able  extract and download the templates
        # 2. ListTemplates should display all the public templates
        #    for all kind of users
        # 3 .ListTemplates should not display the system templates

        self.debug("Extracting template with ID: %s" % self.template_2.id)
        list_extract_response = Template.extract(self.apiclient,
                                                 id=self.template_2.id,
                                                 mode= self.services["template_2"]["mode"],
                                                 zoneid=self.zone.id)

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

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_05_template_permissions(self):
        """Update & Test for template permissions"""

        # Validate the following
        # 1. listTemplatePermissions returns valid
        #    permissions set for template
        # 2. permission changes should be reflected in vm_template
        #    table in database

        self.debug("Updating Template permissions ID:%s" % self.template_2.id)

        self.template_2.updatePermissions(self.apiclient,
                                          isfeatured = self.services["isfeatured"],
                                          ispublic = self.services["ispublic"],
                                          isextractable = self.services["isextractable"])

        list_template_response = Template.list(
                                    self.apiclient,
                                    templatefilter='featured',
                                    id=self.template_2.id,
                                    account=self.account.name,
                                    domainid=self.account.domainid
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

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "sg", "multizone"], required_hardware="true")
    def test_06_copy_template(self):
        """Test for copy template from one zone to another"""

        # Validate the following
        # 1. copy template should be successful and
        #    secondary storage should contain new copied template.

        if len(self.zones) <= 1:
            self.skipTest("Not enough zones available to perform copy template")

        self.services["destzoneid"] = filter(lambda z: z.id != self.services["sourcezoneid"], self.zones)[0].id

        self.debug("Copy template from Zone: %s to %s" % (
                                            self.services["sourcezoneid"],
                                            self.services["destzoneid"]
                                            ))

        self.template_2.copy(self.apiclient,
                             sourcezoneid=self.services["sourcezoneid"],
                             destzoneid=self.services["destzoneid"])

        # Verify template is copied to another zone using ListTemplates
        list_template_response = Template.list(
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
        timeout = self.services["timeout"]
        while True:
            time.sleep(self.services["sleep"])
            list_template_response = Template.list(
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
            if template_response.isready == True:
                break

            if timeout == 0:
                raise Exception(
                        "Failed to download copied template(ID: %s)" % template_response.id)

            timeout = timeout - 1
        self.template_2.delete(self.apiclient, zoneid=self.services["destzoneid"])
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_07_list_public_templates(self):
        """Test only public templates are visible to normal user"""

        # Validate the following
        # 1. ListTemplates should show only 'public' templates for normal user

        list_template_response = Template.list(
                                    self.apiclient,
                                    templatefilter='featured',
                                    account=self.user.name,
                                    domainid=self.user.domainid
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

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_08_list_system_templates(self):
        """Test System templates are not visible to normal user"""

        # Validate the following
        # 1. ListTemplates should not show 'SYSTEM' templates for normal user

        list_template_response = Template.list(
                                    self.apiclient,
                                    templatefilter='featured',
                                    account=self.user.name,
                                    domainid=self.user.domainid
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
