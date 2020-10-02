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
""" tests for template/iso tags
"""
from nose.plugins.attrib import attr
from marvin.cloudstackAPI import listZones, updateIso
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Account,
                             Iso,
                             Template,
                             Domain,
                             Zone)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               list_isos)
import logging
import time

class TestTemplateTags(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestTemplateTags, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.services["iso1"]["zoneid"] = cls.zone.id
        cls.logger = logging.getLogger("TestTemplateTags")
        cls.domain = get_domain(cls.apiclient)

        cls._cleanup = []
        cls.unsupportedHypervisor = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['lxc']:
            cls.unsupportedHypervisor = True
            return

        # Create new domain1
        cls.domain1 = Domain.create(
            cls.apiclient,
            services=cls.services["acl"]["domain1"],
            parentdomainid=cls.domain.id)

        # Create account1
        cls.account1 = Account.create(
            cls.apiclient,
            cls.services["acl"]["accountD1"],
            domainid=cls.domain1.id
        )

        if cls.hypervisor.lower() in ['kvm', "simulator"]:
            # register template
            cls.template = Template.register(cls.apiclient,
                                             cls.services["test_templates"]["kvm"],
                                             zoneid=cls.zone.id,
                                             domainid=cls.domain1.id,
                                             account=cls.account1.name,
                                             hypervisor=cls.hypervisor.lower())
            cls.services["test_templates"]["kvm"]["name"] = cls.account1.name

            cls.iso = Iso.create(
                cls.apiclient,
                cls.services["iso1"],
                account=cls.account1.name,
                domainid=cls.account1.domainid
            )

            cls._cleanup.append(cls.template)
            cls._cleanup.append(cls.iso)
        else:
            return

        cls._cleanup.append(cls.account1)
        cls._cleanup.append(cls.domain1)

    @classmethod
    def tearDownClass(cls):
        try:
            cls.apiclient = super(
                TestTemplateTags,
                cls).getClsTestClient().getApiClient()
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def validate_uploaded_template(self, apiclient, template_id, retries=24, interval=5):
        """Check if template download will finish in 2 minutes"""
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

    def verify_upload_iso(self, iso):
        try:
            iso.download(self.apiclient)
        except Exception as e:
            self.fail("Exception while downloading ISO %s: %s" \
                      % (iso.id, e))

    def list_templates(self, tag):
        return Template.list(
            self.apiclient,
            templatefilter='all',
            zoneid=self.zone.id,
            domainid=self.domain1.id,
            account=self.account1.name,
            templatetag=tag
        )

    def list_isos(self, tag):
        return list_isos(
            self.apiclient,
            account=self.account1.name,
            domainid=self.account1.domainid,
            templatetag=tag
        )

    def update_iso(self, isoid, tag):
        cmd = updateIso.updateIsoCmd()
        # Assign new values to attributes
        cmd.id = isoid
        cmd.templatetag = tag
        self.apiclient.updateIso(cmd)

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_01_update_template_tags(self):
        """
        Verify that the template and iso can be updated with new template tags
        and listing the templates/iso with tags should return the result

        Steps:
        1. Update the template tag of uploaded template as "TEST_TEMPLATE"
        2. List all the templates matching the tag "TEST_TEMPLATE"
        3. Make sure that the count is 1
        4. Update the iso tag of uploaded iso as "TEST_ISO"
        5. List all the isos matching the tag "TEST_ISO"
        6. Make sure that the count is 1
        7. Now list the template and iso with tag "dummy"
        8. Make sure that the count is 0
        :return:
        """
        template_tag = "TEST_TEMPLATE"
        iso_tag = "TEST_ISO"

        self.validate_uploaded_template(self.apiclient, self.template.id)

        list_template_response = self.list_templates(None)

        # Verify template response to check whether template added successfully
        self.assertEqual(
            isinstance(list_template_response, list),
            True,
            "No templates found"
        )

        template_response = list_template_response[0]
        self.assertEqual(
            template_response.isready,
            True,
            "Template state is not ready, it is %s" % template_response.isready
        )

        # step 1
        self.template.update(self.apiclient,
                             templatetag = template_tag)

        # Step 2
        list_template_response = self.list_templates(template_tag)

        # Step 3
        self.assertEqual(
            len(list_template_response),
            1,
            "Template tag not updated properly to %s" % template_tag
        )

        self.verify_upload_iso(self.iso)
        list_iso_response = self.list_isos(None)
        self.assertEqual(
            isinstance(list_iso_response, list),
            True,
            "No iso found"
        )

        # Step 4
        self.update_iso(self.iso.id, iso_tag)

        # Step 5
        list_iso_response = self.list_isos(iso_tag)

        # Step 6
        self.assertEqual(
            isinstance(list_iso_response, list),
            True,
            "Iso tag is not updated properly to %s" % iso_tag
        )
        self.assertEqual(
            len(list_iso_response),
            1,
            "Check template available in List ISOs"
        )

        # Step 7
        list_template_response = self.list_templates("dummy")
        self.assertEqual(
            list_template_response,
            None,
            "Template tag should not be updated to dummy"
        )

        list_iso_response = self.list_isos("dummy")

        # Step 8
        self.assertEqual(
            list_iso_response,
            None,
            "ISO tag should not be updated to dummy"
        )

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_02_register_template_with_tags(self):
        """
        Register template and iso with new template tags
        Verify that it can be registered with tags
        Listing the templates/iso with tags should return the result

        Steps:
        1. Register the template with tag "REGISTER_TEMPLATE"
        2. Register the iso tag of uploaded iso as "REGISTER_ISO"
        3. List all the templates matching the tag "REGISTER_TEMPLATE"
        4. Make sure that the count is 1
        5. List all the isos matching the tag "REGISTER_ISO"
        6. Make sure that the count is 1
        7. Update the template tag to "dummy"
        8. List the templates matching tag "dummy"
        9. Make sure that the count is 1
        10. List the iso matching tag "dummy"
        11. Make sure that the count is 1
        12. List all the templates matching the tag "REGISTER_TEMPLATE"
        13. Make sure that the count is 0
        14. List all the isos matching the tag "REGISTER_ISO"
        15. Make sure that the count is 0
        :return:
        """
        template_tag = "REGISTER_TEMPLATE"
        iso_tag = "REGISTER_ISO"

        # Step 1
        self.services["test_templates"]["kvm"]["templatetag"] = template_tag
        template1 = Template.register(self.apiclient,
                                      self.services["test_templates"]["kvm"],
                                      zoneid=self.zone.id,
                                      domainid=self.domain1.id,
                                      account=self.account1.name,
                                      hypervisor=self.hypervisor.lower()
                                      )
        self.services["test_templates"]["kvm"]["name"] = self.account1.name

        # Step 2
        self.services["iso1"]["templatetag"] = iso_tag
        iso1 = Iso.create(self.apiclient,
                          self.services["iso1"],
                          zoneid=self.zone.id,
                          account=self.account1.name,
                          domainid=self.account1.domainid
                         )

        self.validate_uploaded_template(self.apiclient, template1.id)

        # Step 3
        list_template_response = self.list_templates(template_tag)

        # Verify template response to check whether template added successfully
        self.assertEqual(
            isinstance(list_template_response, list),
            True,
            "No templates found matching tag %s" % template_tag
        )

        # Step 4
        template_response = list_template_response[0]
        self.assertEqual(
            template_response.isready,
            True,
            "Template state is not ready, it is %s" % template_response.isready
        )
        self.assertEqual(
            len(list_template_response),
            1,
            "No templates found"
        )
        self.assertEqual(
            template_response.templatetag,
            template_tag,
            "Template tag does not match"
        )

        self.verify_upload_iso(iso1)

        # Step 5
        list_iso_response = self.list_isos(iso_tag)

        # Step 6
        iso_response = list_iso_response[0]
        self.assertEqual(
            isinstance(list_iso_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertEqual(
            len(list_iso_response),
            1,
            "NO iso found with tag %s" % iso_tag
        )
        self.assertEqual(
            iso_response.templatetag,
            iso_tag,
            "ISO tag does not match"
        )

        # Step 7
        template1.update(self.apiclient,
                         templatetag = "dummy")

        # Step 8
        list_template_response = self.list_templates("dummy")

        # Step 9
        template_response = list_template_response[0]
        self.assertEqual(
            len(list_template_response),
            1,
            "Check template available in List Templates"
        )
        self.assertEqual(
            template_response.templatetag,
            "dummy",
            "Template tag does not match"
        )

        # Step 10
        self.update_iso(iso1.id, "dummy")

        list_iso_response = self.list_isos("dummy")

        # Step 11
        iso_response = list_iso_response[0]
        self.assertEqual(
            isinstance(list_iso_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertEqual(
            len(list_iso_response),
            1,
            "ISO tag does not match"
        )
        self.assertEqual(
            iso_response.templatetag,
            "dummy",
            "ISO tag does not match"
        )

        # Step 12
        list_template_response = self.list_templates(template_tag)

        # Step 13
        self.assertEqual(
            list_template_response,
            None,
            "Template tag not updated properly"
        )

        # Step 14
        list_iso_response = self.list_isos(iso_tag)

        # Step 15
        self.assertEqual(
            list_iso_response,
            None,
            "ISO tag not updated properly"
        )
