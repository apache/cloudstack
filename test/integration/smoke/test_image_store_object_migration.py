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

""" Image store object migration test
"""
import logging

#Import Local Modules
from marvin.cloudstackException import *
from marvin.cloudstackAPI import *
from marvin.codes import FAILED
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.cloudstackAPI import listZones
from marvin.lib.utils import random_gen, cleanup_resources
from marvin.lib.base import (Template,
                             ImageStore)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
from nose.plugins.attrib import attr
import urllib.request, urllib.parse, urllib.error
#Import System modules
import time
from marvin.cloudstackAPI import (createTemplate, listOsTypes)

_multiprocess_shared_ = True

class TestImageStoreObjectMigration(cloudstackTestCase):
    """Test Image Store Object migration
    """
    def setUp(self):
        self.testClient = super(TestImageStoreObjectMigration, self).getClsTestClient()
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
            self.test_template.checksum = "{SHA-1}" + "8b82224fd3c6429b6914f32d8339e650770c7526"
            self.test_template.hypervisor = self.hypervisor
            self.test_template.zoneid = self.zone.id
            self.test_template.name = 'test sha-2333'
            self.test_template.displaytext = 'test sha-1'
            self.test_template.url = "http://dl.openvm.eu/cloudstack/macchinina/x86_64/macchinina-vmware.ova"
            self.test_template.format = "OVA"
            self.test_template.ostypeid = self.getOsType("Other Linux (64-bit)")
            self.md5 = "b4e8bff3882b23175974e692533b4381"
            self.sha256 = "e1dffca3c3ab545a753cb42d838a341624cf25841d1bcf3d1e45556c9fce7cf3"

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

    @attr(tags = ["advanced", "basic", "smoke"], required_hardware="true")
    def test_01_browser_migrate_template(self):
        """
        Test storage browser and template migration to another secondary storage
        """
        template = self.registerTemplate(self.test_template)
        self.download(self.apiclient, template.id)

        list_template_response=Template.list(
            self.apiclient,
            id=template.id,
            templatefilter="all",
            zoneid=self.zone.id)

        datastoreid = list_template_response[0].downloaddetails[0].datastoreId

        qresultset = self.dbclient.execute(
            "select account_id, id from vm_template where uuid = '%s';"
            % template.id
        )

        account_id = qresultset[0][0]
        template_id = qresultset[0][1]

        originalSecondaryStore = ImageStore({"id": datastoreid})

        storeObjects = originalSecondaryStore.listObjects(self.apiclient, path="template/tmpl/" + str(account_id) + "/" + str(template_id))

        self.assertGreaterEqual(len(storeObjects), 2, "Check template is uploaded on secondary storage")

        # Migrate template to another secondary storage
        secondaryStores = ImageStore.list(self.apiclient, zoneid=self.zone.id)

        if len(secondaryStores) < 2:
            self.skipTest("Only one secondary storage available hence skipping")

        for store in secondaryStores:
            if store.id != datastoreid:
                destSecondaryStore = ImageStore({"id": store.id})
                break

        originalSecondaryStore.migrateResources(self.apiclient, destSecondaryStore.id, templateIdList=[template.id])

        try:
            originalSecondaryStore.listObjects(self.apiclient, path="template/tmpl/" + str(account_id) + "/" + str(template_id))
        except Exception as exc:
            self.assertTrue("template/tmpl/" + str(account_id) + "/" + str(template_id) + " doesn't exist in store" in str(exc),
                            "Check template is deleted from original secondary storage")
        else:
            self.fail("Template is not deleted from original secondary storage")

        storeObjects = destSecondaryStore.listObjects(self.apiclient, path="template/tmpl/" + str(account_id) + "/" + str(template_id))

        self.assertGreaterEqual(len(storeObjects), 2, "Check template is uploaded on destination secondary storage")

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
