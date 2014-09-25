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

#Test from the Marvin - Testing in Python wiki

#All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

#Import Integration Libraries

#base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import (
                             Account,
                             VirtualMachine,
                             Volume,
                             ServiceOffering,
                             Configurations,
                             DiskOffering,
                             Template)

#utils - utility classes for common cleanup, external library wrappers etc
from marvin.lib.utils import cleanup_resources, validateList

#common - commonly used methods for all tests are listed here
from marvin.lib.common import get_zone, get_domain, get_template
from marvin.codes import PASS

from nose.plugins.attrib import attr
import time


class TestTemplates(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(TestTemplates, cls).getClsTestClient()
            cls.api_client = cls.testClient.getApiClient()
            cls.services = cls.testClient.getParsedTestDataConfig()
            # Get Domain, Zone, Template
            cls.domain = get_domain(cls.api_client)
            cls.zone = get_zone(
                cls.api_client,
                cls.testClient.getZoneForTests())
            cls.template = get_template(
                cls.api_client,
                cls.zone.id,
                cls.services["ostype"]
            )
            cls.services["template"]["ostypeid"] = cls.template.ostypeid
            cls.services["template"]["isextractable"] = 'True'
            if cls.zone.localstorageenabled:
                cls.storagetype = 'local'
                cls.services["service_offerings"][
                    "tiny"]["storagetype"] = 'local'
                cls.services["disk_offering"]["storagetype"] = 'local'
            else:
                cls.storagetype = 'shared'
                cls.services["service_offerings"][
                    "tiny"]["storagetype"] = 'shared'
                cls.services["disk_offering"]["storagetype"] = 'shared'

            cls.services['mode'] = cls.zone.networktype
            cls.services["virtual_machine"][
                "hypervisor"] = cls.testClient.getHypervisorInfo()
            cls.services["virtual_machine"]["zoneid"] = cls.zone.id
            cls.services["virtual_machine"]["template"] = cls.template.id
            cls.services["custom_volume"]["zoneid"] = cls.zone.id
            # Creating Disk offering, Service Offering and Account
            cls.disk_offering = DiskOffering.create(
                cls.api_client,
                cls.services["disk_offering"]
            )
            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.services["service_offerings"]["tiny"]
            )
            cls.account = Account.create(
                cls.api_client,
                cls.services["account"],
                domainid=cls.domain.id
            )
            # Getting authentication for user in newly created Account
            cls.user = cls.account.user[0]
            cls.userapiclient = cls.testClient.getUserApiClient(cls.user.username, cls.domain.name)
            cls._cleanup.append(cls.disk_offering)
            cls._cleanup.append(cls.service_offering)
            cls._cleanup.append(cls.account)
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setup : %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiClient = self.testClient.getApiClient()
        self.cleanup = []
        return

    def tearDown(self):
        #Clean up, terminate the created volumes
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @attr(tags=["advanced", "advancedsg", "sg"], required_hardware='true')
    def test01_template_download_URL_expire(self):
        """
        @Desc:Template files are deleted from secondary storage after download URL expires
        Step1:Deploy vm with default cent os template
        Step2:Stop the vm
        Step3:Create template from the vm's root volume
        Step4:Extract Template and wait for the download url to expire
        Step5:Deploy another vm with the template created at Step3
        Step6:Verify that vm deployment succeeds
        """
        params = ['extract.url.expiration.interval', 'extract.url.cleanup.interval']
        wait_time = 0
        for param in params:
            config = Configurations.list(
                self.apiClient,
                name=param,
                )
            self.assertEqual(validateList(config)[0], PASS, "Config list returned invalid response")
            wait_time = wait_time+int(config[0].value)
        self.debug("Total wait time for url expiry: %s" % wait_time)
        # Creating Virtual Machine
        self.virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            )
        self.assertIsNotNone(self.virtual_machine, "Virtual Machine creation failed")
        self.cleanup.append(self.virtual_machine)
        #Stop virtual machine
        self.virtual_machine.stop(self.userapiclient)
        list_volume = Volume.list(
            self.userapiclient,
            virtualmachineid=self.virtual_machine.id,
            type='ROOT',
            listall=True
        )
        self.assertEqual(validateList(list_volume)[0],
                         PASS,
                         "list volumes with type ROOT returned invalid list"
        )
        self.volume = list_volume[0]
        self.create_template = Template.create(
            self.userapiclient,
            self.services["template"],
            volumeid=self.volume.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertIsNotNone(self.create_template, "Failed to create template from root volume")
        self.cleanup.append(self.create_template)
        """
        Extract template
        """
        try:
            Template.extract(
                self.userapiclient,
                self.create_template.id,
                'HTTP_DOWNLOAD',
                self.zone.id
            )
        except Exception as e:
            self.fail("Extract template failed with error %s" % e)
        self.debug("Waiting for %s seconds for url to expire" % repr(wait_time+20))
        time.sleep(wait_time+20)
        self.debug("Waited for %s seconds for url to expire" % repr(wait_time+20))
        """
        Deploy vm with the template created from the volume. After url expiration interval only
        url should be deleted not the template. To validate this deploy vm with the template
        """
        try:
            self.vm = VirtualMachine.create(
                self.userapiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                templateid=self.create_template.id
            )
            self.cleanup.append(self.vm)
        except Exception as e:
            self.fail("Template is automatically deleted after URL expired.\
                      So vm deployment failed with error: %s" % e)
        return
