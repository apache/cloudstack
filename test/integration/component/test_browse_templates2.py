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

# Import Local Modules

import marvin
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from marvin.codes import PASS, FAILED, SUCCESS, XEN_SERVER
from marvin.sshClient import SshClient
import requests
requests.packages.urllib3.disable_warnings()
import random
import string
import telnetlib
import os
import urllib.request, urllib.parse, urllib.error
import time
import tempfile
_multiprocess_shared_ = True


class TestBrowseUploadTemplate(cloudstackTestCase):

    """
    Tests for browser based upload template feature. Once all issues in test_browse_templates.py are fixed, this should be merged back
    """
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestBrowseUploadTemplate, cls).getClsTestClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        cls.apiclient = cls.testClient.getApiClient()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls._cleanup = []
        cls.cleanup = []

        hosts = list_hosts(
            cls.apiclient,
            type="Routing"
        )
        if hosts is None:
            cls.SkipTest(
                "There are no hypervisor's available. Check list hosts response")

        cls.uploadtemplateformat = "VHD"
        cls.templatename = "test"
        cls.templatehypervisor = "XenServer"
        cls.templateostypeid = 142

        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.domain = get_domain(cls.apiclient)
        cls.pod = get_pod(cls.apiclient, cls.zone.id)

        cls.account = Account.create(
            cls.apiclient,
            cls.testdata["account"],
            domainid=cls.domain.id
        )

        cls._cleanup = [
            cls.account
        ]

    def waitForSystemVMAgent(self, vmname):
        timeout = self.testdata["timeout"]

        while True:
            list_host_response = list_hosts(
                self.apiclient,
                name=vmname
            )

            if list_host_response and list_host_response[0].state == 'Up':
                break

            if timeout == 0:
                raise Exception("Timed out waiting for SSVM agent to be Up")

            time.sleep(self.testdata["sleep"])
            timeout = timeout - 1

    def destroy_ssvm(self):

        list_ssvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='secondarystoragevm',
            state='Running',
            zoneid=self.zone.id
        )
        self.assertEqual(
            isinstance(list_ssvm_response, list),
            True,
            "Check list response returns a valid list"
        )
        ssvm_response = list_ssvm_response[0]

        old_name = ssvm_response.name

        self.debug("Destroying SSVM: %s" % ssvm_response.id)
        cmd = destroySystemVm.destroySystemVmCmd()
        cmd.id = ssvm_response.id
        self.apiclient.destroySystemVm(cmd)

        timeout = self.testdata["timeout"]
        while True:
            list_ssvm_response = list_ssvms(
                self.apiclient,
                zoneid=self.zone.id,
                systemvmtype='secondarystoragevm'
            )
            if isinstance(list_ssvm_response, list):
                if list_ssvm_response[0].state == 'Running':
                    break
            if timeout == 0:
                raise Exception("List SSVM call failed!")

            time.sleep(self.testdata["sleep"])
            timeout = timeout - 1

        ssvm_response = list_ssvm_response[0]

        # Verify Name, Public IP, Private IP and Link local IP
        # for newly created SSVM
        self.assertNotEqual(
            ssvm_response.name,
            old_name,
            "Check SSVM new name with name of destroyed SSVM"
        )
        self.assertEqual(
            hasattr(ssvm_response, 'privateip'),
            True,
            "Check whether SSVM has private IP field"
        )

        self.assertEqual(
            hasattr(ssvm_response, 'linklocalip'),
            True,
            "Check whether SSVM has link local IP field"
        )

        self.assertEqual(
            hasattr(ssvm_response, 'publicip'),
            True,
            "Check whether SSVM has public IP field"
        )

        # Wait for the agent to be up
        self.waitForSystemVMAgent(ssvm_response.name)

        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_browser_upload_template_incomplete(self):
        """
        Test browser based incomplete template upload, followed by SSVM destroy. Template should go to UploadAbandoned state and get cleaned up.
        """
        try:
            self.debug("========================= Test browser based incomplete template upload ========================")

            #Only register template, without uploading
            cmd = getUploadParamsForTemplate.getUploadParamsForTemplateCmd()
            cmd.zoneid = self.zone.id
            cmd.format = self.uploadtemplateformat
            cmd.name=self.templatename+self.account.name+(random.choice(string.ascii_uppercase))
            cmd.account=self.account.name
            cmd.domainid=self.domain.id
            cmd.displaytext=cmd.name
            cmd.hypervisor=self.templatehypervisor
            cmd.ostypeid=self.templateostypeid
            template_response=self.apiclient.getUploadParamsForTemplate(cmd)

            #Destroy SSVM, and wait for new one to start
            self.destroy_ssvm()
            wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
            #Verify that the template is cleaned up as part of sync-up during new SSVM start
            list_template_response=Template.list(
                                        self.apiclient,
                                        id=template_response.id,
                                        templatefilter="all",
                                        zoneid=self.zone.id)
            self.assertEqual(list_template_response, None, "Template is not cleaned up, some issue with template sync-up")

        except Exception as e:
            self.fail("Exception occurred : %s" % e)
        return

    @classmethod
    def tearDownClass(self):
        try:
            self.apiclient = super(TestBrowseUploadTemplate, self).getClsTestClient().getApiClient()
            cleanup_resources(self.apiclient, self._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return
