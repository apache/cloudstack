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

""" Smoke tests for VNF templates/appliances
"""
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             Domain,
                             Configurations,
                             ServiceOffering,
                             VirtualMachine,
                             Network,
                             NetworkOffering,
                             VnfAppliance,
                             VnfTemplate,
                             Zone)
from marvin.lib.common import get_zone, get_template
from nose.plugins.attrib import attr

import time

VNF_NICS = [{"deviceid": "0", "name": "WAN", "required": "true", "description": "Public WAN"},
          {"deviceid": "1", "name": "LAN-1", "required": "true", "description": "Private LAN-1"}]
NEW_VNF_NICS = [{"deviceid": "0", "name": "WAN", "required": "true", "description": "Public WAN"},
          {"deviceid": "1", "name": "LAN-1", "required": "true", "description": "Private LAN-1"},
          {"deviceid": "2", "name": "LAN-2", "required": "false", "description": "Private LAN-2"}]
VNF_DETAILS = [{"access_methods": "console,https,http", "username": "root"}]
NEW_VNF_DETAILS = [{"access_methods": "console,https,http", "username": "root", "password": "cloudstack"}]

class TestVnfTemplates(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        testClient = super(TestVnfTemplates, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls._cleanup = []
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.zone = Zone(zone.__dict__)
        cls.template = get_template(cls.apiclient, cls.zone.id)

        cls.domain = Domain.create(
            cls.apiclient,
            cls.services["domain"]
        )
        cls._cleanup.append(cls.domain)

        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.user = cls.account.user[0]
        cls.user_apiclient = cls.testClient.getUserApiClient(
            cls.user.username, cls.domain.name
        )

        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["big"]
        )
        cls._cleanup.append(cls.service_offering)

        cls.vnf_template_config = {
            "name": "pfsense",
            "displaytext": "pfsense",
            "format": cls.template.format,
            "url": cls.template.url,
            "requireshvm": "True",
            "ispublic": "True",
            "isextractable": "True",
            "hypervisor": cls.hypervisor,
            "zoneid": cls.zone.id,
            "ostype": "FreeBSD 12 (64-bit)",
            "directdownload": False
        }

        cls.initial_setting = Configurations.list(
            cls.apiclient,
            name="vnf.template.appliance.enabled")[0].value

        Configurations.update(cls.apiclient, "vnf.template.appliance.enabled", "true")

        cls.vnf_templates = []

    @classmethod
    def tearDownClass(cls):
        Configurations.update(cls.apiclient, "vnf.template.appliance.enabled", cls.initial_setting)
        if len(cls.vnf_templates) > 0:
            for vnf_template in cls.vnf_templates:
                vnf_template.delete(cls.user_apiclient)
        super(TestVnfTemplates, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        super(TestVnfTemplates, self).tearDown()

    def ensureVnfTemplateExists(self):
        if len(self.vnf_templates) == 0:
            self.vnf_template = VnfTemplate.register(self.user_apiclient,
                                                     self.vnf_template_config,
                                                     zoneid=self.zone.id,
                                                     hypervisor=self.hypervisor,
                                                     vnfnics=VNF_NICS,
                                                     vnfdetails=VNF_DETAILS)
            self.vnf_templates.append(self.vnf_template)
        else:
            self.vnf_template = self.vnf_templates[0]

    def ensureVnfTemplateDownloaded(self):
        """Check if template download will finish in 5 minutes"""
        retries = 30
        interval = 10
        while retries > -1:
            time.sleep(interval)
            templates_response = VnfTemplate.list(
                self.user_apiclient,
                id=self.vnf_template.id,
                zoneid=self.zone.id,
                templatefilter='self'
            )
            template = templates_response[0]

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

    @attr(tags=["advanced"], required_hardware="false")
    def test_01_register_vnf_template(self):
        """Test register VNF template
        """
        self.ensureVnfTemplateExists()

    @attr(tags=["advanced"], required_hardware="false")
    def test_02_list_vnf_template(self):
        """Test list VNF template
        """
        self.ensureVnfTemplateExists()

        templates_response = VnfTemplate.list(
            self.user_apiclient,
            id=self.vnf_template.id,
            zoneid=self.zone.id,
            templatefilter='self'
        )

        if isinstance(templates_response, list) and len(templates_response) > 0:
            template = templates_response[0]
            self.assertEqual("VNF", template.templatetype,
                             "The template type of VNF template should be VNF but actually it is %s" % template.templatetype)
            self.assertTrue(isinstance(template.vnfnics, list), "The template vnfnics must be a list")
            self.assertEqual(2, len(template.vnfnics), "The VNF template should have 2 VNF nics")
            self.assertEqual(2, len(template.vnfdetails.__dict__), "The VNF template should have 2 VNF details")
        else:
            self.fail("Failed to get VNF templates by listVnfTemplates API")

    @attr(tags=["advanced"], required_hardware="false")
    def test_03_edit_vnf_template(self):
        """Test edit VNF template
        """
        self.ensureVnfTemplateExists()

        self.vnf_template.update(
            self.user_apiclient,
            id=self.vnf_template.id,
            vnfnics=NEW_VNF_NICS,
            vnfdetails=NEW_VNF_DETAILS
        )

        templates_response = VnfTemplate.list(
            self.user_apiclient,
            id=self.vnf_template.id,
            zoneid=self.zone.id,
            templatefilter='self'
        )

        if isinstance(templates_response, list) and len(templates_response) > 0:
            template = templates_response[0]
            self.assertEqual("VNF", template.templatetype,
                             "The template type of VNF template should be VNF but actually it is %s" % template.templatetype)
            self.assertEqual(3, len(template.vnfnics), "The VNF template should have 2 VNF nics")
            self.assertEqual(3, len(template.vnfdetails.__dict__), "The VNF template should have 3 VNF details")
        else:
            self.fail("Failed to get VNF templates by listVnfTemplates API")

    @attr(tags=["advanced"], required_hardware="false")
    def test_04_deploy_vnf_appliance(self):
        """Test deploy VNF appliance
        """
        self.ensureVnfTemplateExists()
        self.ensureVnfTemplateDownloaded()

        templates_response = VnfTemplate.list(
            self.user_apiclient,
            id=self.vnf_template.id,
            zoneid=self.zone.id,
            templatefilter='self'
        )

        if isinstance(templates_response, list) and len(templates_response) > 0:
            template = templates_response[0]
            if not template.isready:
                self.fail("VNF template is not Ready")
        else:
            self.fail("Failed to find VNF template")

        # Create network offerings
        self.isolated_network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["isolated_network_offering"])
        self.cleanup.append(self.isolated_network_offering)
        self.isolated_network_offering.update(
            self.apiclient,
            state='Enabled')

        self.l2_network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["l2-network_offering"])
        self.cleanup.append(self.l2_network_offering)
        self.l2_network_offering.update(
            self.apiclient,
            state='Enabled')

        # Create networks
        isolated_network = Network.create(
            self.user_apiclient,
            self.services["network"],
            networkofferingid=self.isolated_network_offering.id,
            zoneid=self.zone.id
        )
        self.cleanup.append(isolated_network)

        l2_network_1 = Network.create(
            self.user_apiclient,
            self.services["l2-network"],
            networkofferingid=self.l2_network_offering.id,
            zoneid=self.zone.id
        )
        self.cleanup.append(l2_network_1)

        l2_network_2 = Network.create(
            self.user_apiclient,
            self.services["l2-network"],
            networkofferingid=self.l2_network_offering.id,
            zoneid=self.zone.id
        )
        self.cleanup.append(l2_network_2)

        # failed deployment
        try:
            self.virtual_machine = VirtualMachine.create(
                self.user_apiclient,
                self.services["virtual_machine"],
                zoneid=self.zone.id,
                templateid=self.vnf_template.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                networkids=[isolated_network.id]
            )
            self.cleanup.append(self.virtual_machine)
            self.fail("The deployment should fail")
        except Exception as e:
            pass

        # success deployment
        self.vnf_appliance = VnfAppliance.create(
            self.user_apiclient,
            self.services["virtual_machine"],
            zoneid=self.zone.id,
            templateid=self.vnf_template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[isolated_network.id, l2_network_1.id, l2_network_2.id],
            vnfconfiguremanagement='true'
        )
        self.cleanup.append(self.vnf_appliance)

    @attr(tags=["advanced"], required_hardware="false")
    def test_05_delete_vnf_template(self):
        """Test delete VNF template
        """
        self.ensureVnfTemplateExists()

        self.vnf_template.delete(self.user_apiclient)

        templates_response = VnfTemplate.list(
            self.user_apiclient,
            id=self.vnf_template.id,
            zoneid=self.zone.id,
            templatefilter='self'
        )
        self.assertIsNone(templates_response, "The VNF template should be removed")

        self.vnf_templates.remove(self.vnf_template)
