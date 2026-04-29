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

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             Domain, Template
                             )
from marvin.lib.utils import (cleanup_resources, validateList)
from marvin.lib.common import (get_zone, get_builtin_template_info)
from nose.plugins.attrib import attr
from marvin.codes import PASS
import time


class TestlistTemplatesDomainAdmin(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        testClient = super(
            TestlistTemplatesDomainAdmin, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        builtin_info = get_builtin_template_info(cls.apiclient, cls.zone.id)
        cls.testdata["privatetemplate"]["url"] = builtin_info[0]
        cls.testdata["privatetemplate"]["hypervisor"] = builtin_info[1]
        cls.testdata["privatetemplate"]["format"] = builtin_info[2]
        cls.cleanup = []

# Create 2 domain admin accounts

        cls.domain1 = Domain.create(
            cls.apiclient,
            cls.testdata["domain"])

        cls.domain2 = Domain.create(
            cls.apiclient,
            cls.testdata["domain"])

        cls.account1 = Account.create(
            cls.apiclient,
            cls.testdata["account"],
            admin=True,
            domainid=cls.domain1.id)

        cls.account2 = Account.create(
            cls.apiclient,
            cls.testdata["account2"],
            admin=True,
            domainid=cls.domain2.id)

        cls.debug("Created account %s in domain %s" %
                  (cls.account1.name, cls.domain1.id))
        cls.debug("Created account %s in domain %s" %
                  (cls.account2.name, cls.domain2.id))

        cls.cleanup.append(cls.account1)
        cls.cleanup.append(cls.account2)
        cls.cleanup.append(cls.domain1)
        cls.cleanup.append(cls.domain2)

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

# test that the template register under root/domain1->account1 is not
# listed under root/domain2->account2

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_listtemplate(self):

        # Register template under one domain admin(root/domain1->account 1)

        template_register = Template.register(
            self.apiclient,
            self.testdata["privatetemplate"],
            zoneid=self.zone.id,
            hypervisor=self.hypervisor,
            account=self.account1.name,
            domainid=self.domain1.id)

        template_register.download(self.apiclient)

        self.download(self.apiclient, template_register.id)

        listtemplate = Template.list(
            self.apiclient,
            zoneid=self.zone.id,
            hypervisor=self.hypervisor,
            account=self.account2.name,
            domainid=self.account2.domainid,
            templatefilter="executable")

        self.assertEqual(
            listtemplate,
            None,
            "Check templates are not listed - CLOUDSTACK-10149"
        )
        return

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
