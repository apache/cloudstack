#!/usr/bin/env python
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
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from nose.plugins.attrib import attr
import time
from marvin.cloudstackAPI import (registerTemplate, listTemplates, listOsTypes, activateSystemVMTemplate, createZone, listConfigurations, getSystemVMTemplateDefaultUrl, deleteTemplate)
from marvin.lib.utils import cleanup_resources

class TestActivateTemplate(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestActivateTemplate, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.dbclient = cls.testClient.getDbConnection()
        cls._cleanup = []

        cls.services = cls.testClient.getParsedTestDataConfig()
        cls.unsupportedHypervisor = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
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

        cls.listTemplatesCmd = listTemplates.listTemplatesCmd()
        cls.listTemplatesCmd.templatefilter= 'system'
        # Save the current system template
        cls.templates = cls.apiclient.listTemplates(cls.listTemplatesCmd)
        cls.firstRun = True
    
    @classmethod
    def tearDownClass(cls):
        try:
            if cls.firstRun:
                # Save currently active template
                activeTemplate = cls.apiclient.listTemplates(cls.listTemplatesCmd)[0]
                cmd = deleteTemplate.deleteTemplateCmd()
                cls.debug(activeTemplate)
                cmd.id = activeTemplate.id
                cmd.zoneid = cls.zone.id

                # Activate initial system template
                for temp in cls.templates:
                    cmd = activateSystemVMTemplate.activateSystemVMTemplateCmd()
                    cmd.id = temp.id
                    cls.apiclient.activateSystemVMTemplate(cmd)

                # Remove test template
                cls.apiclient.deleteTemplate(cmd)
                cls.firstRun = False

            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        # Default URL
        self.getDefaultURLCmd = getSystemVMTemplateDefaultUrl.getSystemVMTemplateDefaultUrlCmd()
        self.getDefaultURLCmd.hypervisor = self.hypervisor
        urlResponse = self.apiclient.getSystemVMTemplateDefaultUrl(self.getDefaultURLCmd)
        
        # Official cloudstack system vm template
        self.test_template = registerTemplate.registerTemplateCmd()
        self.test_template.hypervisor = self.hypervisor
        self.test_template.zoneid = self.zone.id
        self.test_template.name = 'test-system-' + self.hypervisor + '-4.11.3'
        self.test_template.displaytext = 'test-system-' + self.hypervisor + '-4.11.3'
        self.test_template.url = urlResponse.url.url #"http://download.cloudstack.org/systemvm/4.11/systemvmtemplate-4.11.3-kvm.qcow2.bz2"
        self.test_template.format = "QCOW2"
        self.test_template.system = True
        self.test_template.ostypeid = self.getOsType("Debian GNU/Linux 9 (64-bit)")
        self.md5 = "d40bce40b2d5bb4ba73e56d1e95aeae5"

        self.activateTemplateCmd = activateSystemVMTemplate.activateSystemVMTemplateCmd()
        self.listConfigurationsCmd = listConfigurations.listConfigurationsCmd()
        self.listConfigurationsCmd.name = 'router.template.' + self.hypervisor
        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_01_activate_sytem_vm_template(self):
        """
        Test activating registered template
        """
        template = self.registerTemplate(self.test_template)
        # Registering and Downloading System VM Template
        self.download(self.apiclient, template.id)
        self.activateTemplateCmd.id = template.id
        # Activating downloaded template
        self.activateTemplate(self.activateTemplateCmd)
        response = self.checkConfiguration(self.listConfigurationsCmd)
        # Checking template activation
        self.assertEqual(response[0].value, "test-system-"+ self.hypervisor + "-4.11.3", "Expected template name to be test-system-"+ self.hypervisor +"-4.11.3")
        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_02_get_default_url(self):
        response = self.apiclient.getSystemVMTemplateDefaultUrl(self.getDefaultURLCmd)
        self.assertNotEqual(response.url.url, "", "Expected a value")
        # This test will have to be updated for every official template version
        if self.hypervisor == "kvm":
            self.assertEqual(response.url.url, "https://download.cloudstack.org/systemvm/4.11/systemvmtemplate-4.11.3-kvm.qcow2.bz2", "Default url mismatch")

    def registerTemplate(self, cmd):
        temp = self.apiclient.registerTemplate(cmd)[0]
        if not temp:
            self.cleanup.append(temp)
        return temp

    def activateTemplate(self, cmd):
        response = self.apiclient.activateSystemVMTemplate(cmd)[0]
        return response
    
    def checkConfiguration(self, cmd):
        response = self.apiclient.listConfigurations(cmd)
        return response

    def copyTemplate(self, cmd):
        response = self.apiclient.copyTemplate(cmd)
        return response

    def listTestTemplates(self, cmd):
        response = self.apiclient.listTemplates(cmd)
        return response

    def getOsType(self, param):
        cmd = listOsTypes.listOsTypesCmd()
        cmd.description = param
        return self.apiclient.listOsTypes(cmd)[0].id

    def download(self, apiclient, template_id, retries=12, interval=10):
        """Check if template download will finish"""
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