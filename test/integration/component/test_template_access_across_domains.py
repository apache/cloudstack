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
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.cloudstackAPI import listZones, deleteTemplate, updateConfiguration
from marvin.lib.utils import (cleanup_resources)
from marvin.lib.base import (Account,
                             Domain,
                             Network,
                             NetworkOffering,
                             Template,
                             ServiceOffering,
                             VirtualMachine,
                             Snapshot,
                             Volume)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               get_builtin_template_info)
# Import System modules
import time
import logging

class TestTemplateAccessAcrossDomains(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestTemplateAccessAcrossDomains, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()

        cls.services = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.logger = logging.getLogger("TestRouterResources")
        cls._cleanup = []
        cls.unsupportedHypervisor = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['lxc']:
            cls.unsupportedHypervisor = True
            return
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

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

        # Create new sub-domain
        cls.sub_domain = Domain.create(
            cls.apiclient,
            services=cls.services["acl"]["domain11"],
            parentdomainid=cls.domain1.id)

        # Create account for sub-domain
        cls.sub_account = Account.create(
            cls.apiclient,
            cls.services["acl"]["accountD11"],
            domainid=cls.sub_domain.id
        )

        # Create new domain2
        cls.domain2 = Domain.create(
            cls.apiclient,
            services=cls.services["acl"]["domain2"],
            parentdomainid=cls.domain.id)

        # Create account2
        cls.account2 = Account.create(
            cls.apiclient,
            cls.services["acl"]["accountD2"],
            domainid=cls.domain2.id
        )

        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        if cls.hypervisor.lower() in ['kvm']:
            # register template under ROOT domain
            cls.root_template = Template.register(cls.apiclient,
                                                  cls.services["test_templates"]["kvm"],
                                                  zoneid=cls.zone.id,
                                                  domainid=cls.domain.id,
                                                  hypervisor=cls.hypervisor.lower())
            cls.services["test_templates"]["kvm"]["name"] = cls.account1.name
            cls.template1 = Template.register(cls.apiclient,
                                              cls.services["test_templates"]["kvm"],
                                              zoneid=cls.zone.id,
                                              account=cls.account1.name,
                                              domainid=cls.domain1.id,
                                              hypervisor=cls.hypervisor.lower())
            cls.services["test_templates"]["kvm"]["name"] = cls.sub_account.name
            cls.sub_template = Template.register(cls.apiclient,
                                                 cls.services["test_templates"]["kvm"],
                                                 zoneid=cls.zone.id,
                                                 account=cls.sub_account.name,
                                                 domainid=cls.sub_domain.id,
                                                 hypervisor=cls.hypervisor.lower())
            cls.services["test_templates"]["kvm"]["name"] = cls.account2.name
            cls.template2 = Template.register(cls.apiclient,
                                              cls.services["test_templates"]["kvm"],
                                              zoneid=cls.zone.id,
                                              account=cls.account2.name,
                                              domainid=cls.domain2.id,
                                              hypervisor=cls.hypervisor.lower())

            cls._cleanup.append(cls.root_template)
            cls._cleanup.append(cls.template1)
            cls._cleanup.append(cls.template2)
            cls._cleanup.append(cls.sub_template)
        else:
            return

        cls._cleanup.append(cls.account1)
        cls._cleanup.append(cls.account2)
        cls._cleanup.append(cls.sub_account)
        cls._cleanup.append(cls.sub_domain)
        cls._cleanup.append(cls.domain1)
        cls._cleanup.append(cls.domain2)

    @classmethod
    def tearDownClass(cls):
        try:
            cls.apiclient = super(
                TestTemplateAccessAcrossDomains,
                cls).getClsTestClient().getApiClient()
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_01_check_cross_domain_template_access(self):
        """
        Verify that templates belonging to one domain should not be accessible
        by other domains except for parent and ROOT domains

        Steps:
        1. Set global setting restrict.public.access.to.templates to true
        2. Make sure template of domain2 should not be accessible by domain1
        3. Make sure template of domain1 should not be accessible by domain2
        4. Make sure parent and ROOT domain can still access above templates
        :return:
        """

        # Step 1
        self.update_configuration("true")

        self.validate_uploaded_template(self.apiclient, self.template1.id)

        # Step 2
        self.validate_template_ownership(self.template2, self.domain1, self.domain2, False)

        self.validate_uploaded_template(self.apiclient, self.template2.id)

        # Step 3
        self.validate_template_ownership(self.template1, self.domain2, self.domain1, False)

        # Make sure root domain can still access all subdomain templates
        # Step 4
        self.validate_template_ownership(self.template1, self.domain, self.domain1, True)
        self.validate_template_ownership(self.template2, self.domain, self.domain2, True)


    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_02_create_template(self):
        """
        Verify that templates belonging to one domain can be accessible
        by other domains by default

        Steps:
        1. Set global setting restrict.public.access.to.templates to false (default behavior)
        2. Make sure template of domain2 can be accessible by domain1
        3. Make sure template of domain1 can be accessible by domain2
        4. Make sure parent and ROOT domain can still access above templates
        5. Deploy virtual machine in domain1 using template from domain2
        6. Make sure that virtual machine can be deployed and is in running state
        :return:
        """

        # Step 1
        self.update_configuration("false")

        # Step 2
        self.validate_template_ownership(self.template2, self.domain1, self.domain2, True)

        # Step 3
        self.validate_template_ownership(self.template1, self.domain2, self.domain1, True)

        # Step 4
        # Make sure root domain can still access all subdomain templates
        self.validate_template_ownership(self.template1, self.domain, self.domain1, True)
        self.validate_template_ownership(self.template2, self.domain, self.domain2, True)

        # Step 5
        # Deploy new virtual machine using template
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template2.id,
            accountid=self.account1.name,
            domainid=self.account1.domainid,
            serviceofferingid=self.service_offering.id,
        )
        self.debug("creating an instance with template ID: %s" % self.template2.id)
        vm_response = VirtualMachine.list(self.apiclient,
                                          id=virtual_machine.id,
                                          account=self.account1.name,
                                          domainid=self.account1.domainid)
        self.assertEqual(
            isinstance(vm_response, list),
            True,
            "Check for list VMs response after VM deployment"
        )
        # Verify VM response to check whether VM deployment was successful
        self.assertNotEqual(
            len(vm_response),
            0,
            "Check VMs available in List VMs response"
        )

        # Step 6
        vm = vm_response[0]
        self.assertEqual(
            vm.state,
            'Running',
            "Check the state of VM created from Template"
        )

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_03_check_subdomain_template_access(self):
        """
        Verify that templates belonging to parent domain can be accessible
        by sub domains

        Steps:
        1. Set global setting restrict.public.access.to.templates to true
        2. Make sure template of ROOT domain can be accessible by domain1
        3. Make sure template of ROOT domain can be accessible by domain2
        """

        # Step 1
        self.update_configuration("true")
        # Make sure child domains can still access parent domain templates
        self.validate_uploaded_template(self.apiclient, self.root_template.id)

        # Step 2
        self.validate_template_ownership(self.root_template, self.domain1, self.domain, True)

        # Step 3
        self.validate_template_ownership(self.root_template, self.domain2, self.domain, True)

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_04_check_non_public_template_access(self):
        """
        Verify that non public templates belonging to one domain
        should not be accessible by other domains by default

        Steps:
        1. Set global setting restrict.public.access.to.templates to true
        2. Change the permission level of "ispublic" of template to false
        3. Make sure other domains should not be able to access the template
        4. Make sure that ONLY ROOT domain can access the non public template
        5. Set global setting restrict.public.access.to.templates to false
        6. Repeat the steps 3 and 4
        """

        # Step 1
        self.update_configuration("true")

        # Step 2
        self.template2.updatePermissions(self.apiclient,
                                         ispublic="False")

        list_template_response = self.list_templates('all', self.domain2)
        self.assertEqual(
            isinstance(list_template_response, list),
            True,
            "Check list response returns a valid list"
        )
        for template_response in list_template_response:
            if template_response.id == self.template2.id:
                break

        self.assertIsNotNone(
            template_response,
            "Check template %s failed" % self.template2.id
        )
        self.assertEqual(
            template_response.ispublic,
            int(False),
            "Check ispublic permission of template"
        )

        # Step 3
        # Other domains should not access non public template
        self.validate_template_ownership(self.template2, self.domain1, self.domain2, False)

        # Step 4
        # Only ROOT domain can access non public templates of child domain
        self.validate_template_ownership(self.template2, self.domain, self.domain2, True)

        # Step 5
        self.update_configuration("false")

        # Step 6
        self.validate_template_ownership(self.template2, self.domain1, self.domain2, False)
        self.validate_template_ownership(self.template2, self.domain, self.domain2, True)

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_05_check_non_public_template_subdomain_access(self):
        """
        Verify that non public templates belonging to ROOT domain
        should not be accessible by sub domains by default

        Steps:
        1. Set global setting restrict.public.access.to.templates to true
        2. Change the permission level of "ispublic" of template to false
        3. Make sure other domains should not be able to access the template
        4. Make sure that ONLY ROOT domain can access the non public template
        5. Set global setting restrict.public.access.to.templates to false
        6. Repeat the steps 3 and 4
        """
        self.update_configuration("true")
        self.root_template.updatePermissions(self.apiclient,
                                             ispublic="False")

        list_template_response = self.list_templates('all', self.domain)
        self.assertEqual(
            isinstance(list_template_response, list),
            True,
            "Check list response returns a valid list"
        )
        for template_response in list_template_response:
            if template_response.id == self.root_template.id:
                break

        self.assertIsNotNone(
            template_response,
            "Check template %s failed" % self.root_template.id
        )
        self.assertEqual(
            template_response.ispublic,
            int(False),
            "Check ispublic permission of template"
        )

        # Other domains should not access non public template
        self.validate_template_ownership(self.root_template, self.domain1, self.domain, False)
        # Only ROOT domain can access non public templates of child domain
        self.validate_template_ownership(self.root_template, self.domain2, self.domain, False)

        self.update_configuration("false")
        self.validate_template_ownership(self.root_template, self.domain1, self.domain2, False)
        self.validate_template_ownership(self.root_template, self.domain2, self.domain2, False)

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_06_check_sub_public_template_sub_domain_access(self):
        """
        Verify that non root admin sub-domains can access parents templates

        Steps:
        1. Set global setting restrict.public.access.to.templates to true
        2. Make sure that sub-domain account can access root templates
        3. Make sure that sub-domain account can access parent templates
        4. Make sure that ROOT domain can access the sub-domain template
        5. Make sure that sibling domain cannot access templates of sub-domain
        """

        self.root_template.updatePermissions(self.apiclient,
                                             ispublic="True")
        # Step 1
        self.update_configuration("true")
        # Make sure child domains can still access parent domain templates
        self.validate_uploaded_template(self.apiclient, self.sub_template.id)

        # Step 2
        self.validate_template_ownership(self.root_template, self.sub_domain, self.domain, True)

        # Step 3
        self.validate_template_ownership(self.template1, self.sub_domain, self.domain1, True)

        # Step 4
        self.validate_template_ownership(self.sub_template, self.domain, self.sub_domain, True)

        # Step 5
        self.validate_template_ownership(self.sub_template, self.domain2, self.sub_domain, False)

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_07_check_default_public_template_sub_domain_access(self):
        """
        Verify that non root admin sub-domains can access parents templates by default

        Steps:
        1. Set global setting restrict.public.access.to.templates to false
        2. Make sure that sub-domain account can access root templates
        3. Make sure that sub-domain account can access parent templates
        4. Make sure that ROOT domain can access the sub-domain template
        5. Make sure that sibling domain cannot access templates of sub-domain
        """

        # Step 1
        self.update_configuration("false")
        # Make sure child domains can still access parent domain templates
        self.validate_uploaded_template(self.apiclient, self.sub_template.id)

        # Step 2
        self.validate_template_ownership(self.root_template, self.sub_domain, self.domain, True)

        # Step 3
        self.validate_template_ownership(self.template1, self.sub_domain, self.domain1, True)

        # Step 4
        self.validate_template_ownership(self.sub_template, self.domain, self.sub_domain, True)

        # Step 5
        self.validate_template_ownership(self.sub_template, self.domain2, self.sub_domain, True)

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_08_check_non_public_template_sub_domain_access(self):
        """
        Verify that non public templates belonging to one domain
        should not be accessible by other domains by default except ROOT domain

        Steps:
        1. Set global setting restrict.public.access.to.templates to true
        2. Change the permission level of "ispublic" of template1 to false
        3. Make sure other domains should not be able to access the template
        4. Make sure that ONLY ROOT domain can access the non public template
        5. Set global setting restrict.public.access.to.templates to false
        6. Repeat the steps 3 and 4
        """

        # Step 1
        self.update_configuration("true")

        # Step 2
        self.template1.updatePermissions(self.apiclient,
                                         ispublic="False")

        list_template_response = self.list_templates('all', self.domain1)
        for template_response in list_template_response:
            if template_response.id == self.template1.id:
                break

        self.assertEqual(
            isinstance(list_template_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertIsNotNone(
            template_response,
            "Check template %s failed" % self.template1.id
        )
        self.assertEqual(
            template_response.ispublic,
            int(False),
            "Check ispublic permission of template"
        )

        # Step 3
        # Other domains should not access non public template
        self.validate_template_ownership(self.template1, self.domain2, self.domain1, False)

        # Even child domain should not access non public template
        self.validate_template_ownership(self.template1, self.sub_domain, self.domain1, False)

        # Step 4
        # Only ROOT domain can access non public templates of child domain
        self.validate_template_ownership(self.template1, self.domain, self.domain1, True)

        # Step 5
        self.update_configuration("false")

        # Step 6
        self.validate_template_ownership(self.template1, self.domain2, self.domain1, False)
        self.validate_template_ownership(self.template1, self.sub_domain, self.domain1, False)
        self.validate_template_ownership(self.template1, self.domain, self.domain1, True)

    def validate_uploaded_template(self, apiclient, template_id, retries=70, interval=5):
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

    def list_templates(self, templatefilter, domain):
        return Template.list(
                    self.apiclient,
                    templatefilter=templatefilter,
                    zoneid=self.zone.id,
                    domainid=domain.id)

    def validate_template_ownership(self, template, owner, nonowner, include_cross_domain_template):
        """List the template belonging to domain which created it
           Make sure that other domain can't access it.
        """
        list_template_response = self.list_templates('all', owner)
        if list_template_response is not None:
            """If global setting is false then public templates of any domain should
               be accessible by any other domain
            """
            if include_cross_domain_template:
                for temp in list_template_response:
                    if template.name == temp.name:
                        return

                raise Exception("Template %s belonging to domain %s should "
                                "be accessible by domain %s"
                                % (template.name, nonowner.name, owner.name))
            else:
                """If global setting is true then public templates of any domain should not
                   be accessible by any other domain except for root domain
                """
                for temp in list_template_response:
                    if template.name == temp.name:
                        raise Exception("Template %s belonging to domain %s should "
                                        "not be accessible by domain %s"
                                        % (template.name, nonowner.name, owner.name))

    def update_configuration(self, value):
        """
        Function to update the global setting "restrict.public.access.to.templates"
        :param value:
        :return:
        """
        update_configuration_cmd = updateConfiguration.updateConfigurationCmd()
        update_configuration_cmd.name = "restrict.public.template.access.to.domain"
        update_configuration_cmd.value = value
        return self.apiclient.updateConfiguration(update_configuration_cmd)
