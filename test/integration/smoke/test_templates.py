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
from marvin.cloudstackException import *
from marvin.cloudstackAPI import *
from marvin.codes import FAILED
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.cloudstackAPI import listZones
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
import urllib.request, urllib.parse, urllib.error
#Import System modules
import time
from marvin.cloudstackAPI import (createTemplate, listOsTypes)

_multiprocess_shared_ = True

class TestCreateTemplateWithChecksum(cloudstackTestCase):
    def setUp(self):
        self.testClient = super(TestCreateTemplateWithChecksum, self).getClsTestClient()
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        self.services = self.testClient.getParsedTestDataConfig()
        self.unsupportedHypervisor = False
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            # Template creation from root volume is not supported in LXC
            self.unsupportedHypervisor = True
            return

        # Get Zone, Domain and templates
        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())

        if "kvm" in self.hypervisor.lower():
            self.test_template = registerTemplate.registerTemplateCmd()
            self.test_template = registerTemplate.registerTemplateCmd()
            self.test_template.checksum = "{SHA-1}" + "6952e58f39b470bd166ace11ffd20bf479bed936"
            self.test_template.hypervisor = self.hypervisor
            self.test_template.zoneid = self.zone.id
            self.test_template.name = 'test sha-2333'
            self.test_template.displaytext = 'test sha-1'
            self.test_template.url = "http://dl.openvm.eu/cloudstack/macchinina/x86_64/macchinina-kvm.qcow2.bz2"
            self.test_template.format = "QCOW2"
            self.test_template.ostypeid = self.getOsType("Other Linux (64-bit)")
            self.md5 = "88c60fd500ce7ced985cf845df0db9da"
            self.sha256 = "bc4cc040bbab843000fab78db6cb4a33f3a06ae1ced2cf563d36b38c7fee3049"

        if "vmware" in self.hypervisor.lower():
            self.test_template = registerTemplate.registerTemplateCmd()
            self.test_template = registerTemplate.registerTemplateCmd()
            self.test_template.checksum = "{SHA-1}" + "3c00872599c6e1e46a358aac51080db88266cf5c"
            self.test_template.hypervisor = self.hypervisor
            self.test_template.zoneid = self.zone.id
            self.test_template.name = 'test sha-2333'
            self.test_template.displaytext = 'test sha-1'
            self.test_template.url = "http://dl.openvm.eu/cloudstack/macchinina/x86_64/macchinina-vmware.ova"
            self.test_template.format = "OVA"
            self.md5 = "27f3c56a8c7ec7b2f3ff2199f7078006"
            self.sha256 = "a7b04c1eb507f3f5de844bda352df1ea5e20335b465409493ca6ae07dfd0a158"

        if "xen" in self.hypervisor.lower():
            self.test_template = registerTemplate.registerTemplateCmd()
            self.test_template = registerTemplate.registerTemplateCmd()
            self.test_template.checksum = "{SHA-1}" + "80af2c18f96e94273188808c3d56e561a1cda717"
            self.test_template.hypervisor = self.hypervisor
            self.test_template.zoneid = self.zone.id
            self.test_template.name = 'test sha-2333'
            self.test_template.displaytext = 'test sha-1'
            self.test_template.url = "http://dl.openvm.eu/cloudstack/macchinina/x86_64/macchinina-xen.vhd.bz2"
            self.test_template.format = "VHD"
            self.test_template.ostypeid = self.getOsType("Other Linux (64-bit)")
            self.md5 = "1662bbf224e41bb62b1dee043d785731"
            self.sha256 = "80fba5a7a83842ec4e5f67cc6755d61d4fca46ae170d59b0c6ed47ebf7162722"

        if self.unsupportedHypervisor:
            self.skipTest("Skipping test because unsupported hypervisor\
                            %s" % self.hypervisor)
        return

    def tearDown(self):
        try:
            # Clean up the created templates
            for temp in self.cleanup:
                cmd = deleteTemplate.deleteTemplateCmd()
                cmd.id = temp.id
                cmd.zoneid = self.zone.id
                self.apiclient.deleteTemplate(cmd)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_02_create_template_with_checksum_sha1(self):
        template = self.registerTemplate(self.test_template)
        self.download(self.apiclient, template.id)

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_03_create_template_with_checksum_sha256(self):
        self.test_template.checksum = "{SHA-256}" + self.sha256
        template = self.registerTemplate(self.test_template)
        self.download(self.apiclient, template.id)

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_04_create_template_with_checksum_md5(self):
        self.test_template.checksum = "{md5}" + self.md5
        template = self.registerTemplate(self.test_template)
        self.download(self.apiclient, template.id)

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_02_1_create_template_with_checksum_sha1_negative(self):
        self.test_template.checksum = "{sha-1}" + "someInvalidValue"
        template = self.registerTemplate(self.test_template)

        try:
            self.download(self.apiclient, template.id)
        except Exception as e:
            print("Negative Test Passed - Exception Occurred Under template download " \
                  "%s" % GetDetailExceptionInfo(e))
        else:
            self.fail("Negative Test Failed - Exception DID NOT Occurred Under template download ")

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_03_1_create_template_with_checksum_sha256_negative(self):
        self.test_template.checksum = "{SHA-256}" + "someInvalidValue"
        template = self.registerTemplate(self.test_template)

        try:
            self.download(self.apiclient, template.id)
        except Exception as e:
            print("Negative Test Passed - Exception Occurred Under template download " \
                  "%s" % GetDetailExceptionInfo(e))
        else:
            self.fail("Negative Test Failed - Exception DID NOT Occurred Under template download ")

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_04_1_create_template_with_checksum_md5_negative(self):
        self.test_template.checksum = "{md5}" + "someInvalidValue"
        template = self.registerTemplate(self.test_template)

        try:
            self.download(self.apiclient, template.id)
        except Exception as e:
            print("Negative Test Passed - Exception Occurred Under template download " \
                  "%s" % GetDetailExceptionInfo(e))
        else:
            self.fail("Negative Test Failed - Exception DID NOT Occurred Under template download ")

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_05_create_template_with_no_checksum(self):
        self.test_template.checksum = None
        template = self.registerTemplate(self.test_template)
        self.download(self.apiclient, template.id)

    def registerTemplate(self, cmd):
        temp = self.apiclient.registerTemplate(cmd)[0]
        if not temp:
            self.cleanup.append(temp)
        return temp

    def getOsType(self, param):
        cmd = listOsTypes.listOsTypesCmd()
        cmd.description = param
        return self.apiclient.listOsTypes(cmd)[0].id

    def download(self, apiclient, template_id, retries=12, interval=5):
        """Check if template download will finish in 1 minute"""
        while retries > -1:
            time.sleep(interval)
            template_response = Template.list(
                apiclient,
                id=template_id,
                zoneid=self.zone.id,
                templatefilter='self'
            )

            if isinstance(template_response, list):
                template = template_response[0]
                if not hasattr(template, 'status') or not template or not template.status:
                    retries = retries - 1
                    continue

                # If template is ready,
                # template.status = Download Complete
                # Downloading - x% Downloaded
                # if Failed
                # Error - Any other string
                if 'Failed' in template.status:
                    raise Exception(
                        "Failed to download template: status - %s" %
                        template.status)

                elif template.status == 'Download Complete' and template.isready:
                    return

                elif 'Downloaded' in template.status:
                    retries = retries - 1
                    continue

                elif 'Installing' not in template.status:
                    if retries >= 0:
                        retries = retries - 1
                        continue
                    raise Exception(
                        "Error in downloading template: status - %s" %
                        template.status)

            else:
                retries = retries - 1
        raise Exception("Template download failed exception.")


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
            cleanup_resources(self.apiclient, reversed(self.cleanup))

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
            #Cleanup resources used
            cleanup_resources(cls.apiclient, reversed(cls._cleanup))

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
        template1 = Template.create(self.apiclient,
                                    self.services["template"],
                                    self.volume.id,
                                    account=self.account.name,
                                    randomise=False)
        self.cleanup.append(template1)
        template2 = Template.create(self.apiclient,
                                    self.services["template"],
                                    self.volume.id,
                                    account=self.account.name,
                                    randomise=False)
        self.cleanup.append(template2)

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
        cls.services["isdynamicallyscalable"] = cls.template_1.isdynamicallyscalable
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
        self.assertEqual(
                            template_response.isdynamicallyscalable,
                            self.services["isdynamicallyscalable"],
                            "Check isdynamicallyscalable of updated template"
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
            formatted_url = urllib.parse.unquote_plus(list_extract_response.url)
            url_response = urllib.request.urlopen(formatted_url)
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

        self.services["destzoneid"] = [z for z in self.zones if z.id != self.services["sourcezoneid"]][0].id

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

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_09_list_templates_download_details(self):
        """Test if list templates returns download details"""

        # Validate the following
        # 1. ListTemplates API has been extended to support viewing the download details - progress, download states and datastore

        list_template_response = Template.list(
                                    self.apiclient,
                                    templatefilter='all',
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
                        len(template.downloaddetails),
                        0,
                        "Not all templates have download details"
                        )

        return

class TestCopyAndDeleteTemplatesAcrossZones(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestCopyAndDeleteTemplatesAcrossZones, cls).getClsTestClient()
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
            #Cleanup resources used
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

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_09_copy_delete_template(self):
        cmd = listZones.listZonesCmd()
        zones = self.apiclient.listZones(cmd)
        if not isinstance(zones, list):
            raise Exception("Failed to find zones.")
        if len(zones) < 2:
            self.skipTest("Skipping test due to there are less than two zones.")
            return

        self.sourceZone = zones[0]
        self.destZone = zones[1]

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
            templatefilter= self.services["templatefilter"],
            id=template.id
        )

        self.assertEqual(
            isinstance(list_template_response, list),
            True,
            "Check list response returns a valid list"
        )
        # Verify template response to check whether template added successfully
        self.assertNotEqual(
            len(list_template_response),
            0,
            "Check template available in List Templates"
        )
        # todo: check for template ready
        # Copy template from zone1 to zone2
        self.copytemplate = template.copy(
            self.apiclient,
            sourcezoneid=self.sourceZone.id,
            destzoneid=self.destZone.id
        )
        self.cleanup.append(self.copytemplate)

        list_template_response = Template.list(
            self.apiclient,
            templatefilter=self.services["template"]["templatefilter"],
            id=self.template.id,
            zoneid=self.destZone.id
        )
        self.assertEqual(
            list_template_response,
            None,
            "Check template available in List Templates"
        )

        self.deltemplate = list_template_response[0]

        self.debug("Deleting template: %s" % self.deltemplate)
        # Delete the template
        self.deltemplate.delete(self.apiclient)
        self.debug("Delete template: %s successful" % self.deltemplate)

        copytemplate = template.copy(
            self.apiclient,
            sourcezoneid=self.sourceZone.id,
            destzoneid = self.destZone.id
        )
        self.cleanup.append(copytemplate)

        removed = self.dbclient.execute("select removed from template_zone_ref where zone_id='%s' and template_id='%s';" % self.destZone.id, self.template.id)

        self.assertEqual(
            removed,
            None,
            "Removed state is not correct."
        )
        return

class TestCreateTemplateWithDirectDownload(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestCreateTemplateWithDirectDownload, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.dbclient = cls.testClient.getDbConnection()
        cls._cleanup = []
        cls.templates = []

        cls.services = cls.testClient.getParsedTestDataConfig()
        cls.unsupportedHypervisor = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() not in ['kvm']:
            # Direct Download is only available for KVM hypervisor
            cls.unsupportedHypervisor = True
            return

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.services["mode"] = cls.zone.networktype
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)
        cls.user = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.user)
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls._cleanup.append(cls.service_offering)

        cls.template = {
            "name": "tiny-kvm",
            "displaytext": "tiny kvm",
            "format": "QCOW2",
            "url": "http://dl.openvm.eu/cloudstack/macchinina/x86_64/macchinina-kvm.qcow2.bz2",
            "requireshvm": "True",
            "ispublic": "True",
            "isextractable": "True",
            "checksum": "{SHA-1}" + "6952e58f39b470bd166ace11ffd20bf479bed936",
            "hypervisor": cls.hypervisor,
            "zoneid": cls.zone.id,
            "ostype": "Other Linux (64-bit)",
            "directdownload": True
        }

        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.unsupportedHypervisor:
            self.skipTest("Skipping test because unsupported hypervisor %s" % self.hypervisor)
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_01_register_template_direct_download_flag(self):
        """
        Register a template using Direct Download flag
        """
        self.bypassed_template = Template.register(self.apiclient, self.template, zoneid=self.zone.id, hypervisor=self.hypervisor, randomize_name=False)
        self._cleanup.append(self.bypassed_template)
        self.templates.append(self.bypassed_template)

        tmplt = self.dbclient.execute("select id, direct_download from vm_template where uuid='%s';" % self.bypassed_template.id)
        det = tmplt[0]

        self.assertEqual(det[1],
                         1,
                         "Template should be marked as Direct Download"
                         )
        qresultset = self.dbclient.execute("select download_state, state from template_store_ref where template_id='%s' and store_id is NULL;"
                                           % det[0])
        ref = qresultset[0]
        self.assertEqual(ref[0],
                         "BYPASSED",
                         "Template store ref download state should be marked as BYPASSED"
                         )
        self.assertEqual(ref[1],
                         "Ready",
                         "Template store ref state should be marked as Ready"
                         )
        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_02_deploy_vm_from_direct_download_template(self):
        """
        Deploy a VM from a Direct Download registered template
        """
        bp = self.templates[0]
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=bp.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id
        )
        self.cleanup.append(virtual_machine)
        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_03_deploy_vm_wrong_checksum(self):
        """
        Deploy a VM from a Direct Download registered template with wrong checksum
        """
        self.template["checksum"]="{MD5}XXXXXXX"
        tmpl = Template.register(self.apiclient, self.template, zoneid=self.zone.id, hypervisor=self.hypervisor, randomize_name=False)

        try:
            virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                templateid=tmpl.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id
            )
            self.cleanup.append(tmpl)
            self.fail("Expected to fail deployment")
        except Exception as e:
            self.debug("Expected exception")

        self.cleanup.append(virtual_machine)
        self.cleanup.append(tmpl)
        return
