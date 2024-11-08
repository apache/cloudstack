# -- coding: utf-8 --
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
""" BVT tests for resource names with emojis / unicode
"""
from marvin.cloudstackTestCase import cloudstackTestCase

from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             Template,
                             Iso,
                             Volume,
                             DiskOffering)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_suitable_test_template,
                               get_builtin_template_info)
from marvin.codes import FAILED
from nose.plugins.attrib import attr
# Import System modules
import time

_multiprocess_shared_ = True

class TestResourceNames(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestResourceNames, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype
        cls._cleanup = []

        template = get_suitable_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % cls.services[
                "ostype"]

        # Set Zones and disk offerings
        cls.services["domainid"] = cls.domain.id
        cls.services["zoneid"] = cls.zone.id
        cls.services["template"] = template.id
        cls.services["iso1"]["zoneid"] = cls.zone.id
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id

        cls.services["account"]["firstname"] = "test🎉"
        cls.services["account"]["lastname"] = "account🙂"
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.services["service_offerings"]["tiny"]["name"] = "test🎉svcoffering🙂"
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls._cleanup.append(cls.service_offering)

        cls.services["disk_offering"]["name"] = "test🎉diskoffering🙂"
        cls.disk_offering = DiskOffering.create(
            cls.apiclient,
            cls.services["disk_offering"]
        )
        cls._cleanup.append(cls.disk_offering)

        cls.services["small"]["displayname"] = "test🎉vm🙂"
        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["small"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            mode=cls.services['mode']
        )

    @classmethod
    def tearDownClass(cls):
        super(TestResourceNames, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        super(TestResourceNames, self).tearDown()

    @attr(tags=["advanced", "smoke", "basic"], required_hardware="false")
    def test_01_deploy_vm(self):
        """Test for deploy virtual machine
        """
        # Validate the following:
        # 1. listVirtualMachines returns accurate information, and check name
        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id
        )

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s" \
            % self.virtual_machine.id
        )
        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(list_vm_response),
            0,
            "Check VM available in List Virtual Machines"
        )

        vm_response = list_vm_response[0]
        self.assertEqual(
            vm_response.id,
            self.virtual_machine.id,
            "Check virtual machine id in listVirtualMachines"
        )
        self.assertEqual(
            vm_response.name,
            self.virtual_machine.name,
            "Check virtual machine name in listVirtualMachines"
        )
        self.assertEqual(
            vm_response.displayname,
            self.virtual_machine.displayname,
            "Check virtual machine display name in listVirtualMachines"
        )
        self.assertEqual(
            vm_response.state,
            'Running',
            msg="VM is not in Running state"
        )
        return

    @attr(tags=["advanced", "smoke", "basic"], required_hardware="true")
    def test_02_create_volume(self):
        """Test for create volume
        """
        # Validate the following:
        # 1. Create volume and check name

        self.services["diskname"] = "test🎉data🙂volume"
        self.volume = Volume.create(
            self.apiclient,
            self.services,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering.id
        )
        # self.cleanup.append(self.volume)
        self.virtual_machine.attach_volume(self.apiclient, self.volume)
        list_volume_response = Volume.list(
            self.apiclient,
            id=self.volume.id
        )
        self.assertEqual(
            isinstance(list_volume_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            list_volume_response,
            None,
            "Check if volume exists in ListVolumes"
        )

        volume_response = list_volume_response[0]
        self.assertNotEqual(
            volume_response.virtualmachineid,
            None,
            "Check if volume state (attached) is reflected"
        )
        self.assertEqual(
            volume_response.name,
            self.volume.name,
            "Check virtual machine display name in listVirtualMachines"
        )

    @attr(tags=["advanced", "smoke", "basic"], required_hardware="true")
    def test_03_register_template(self):
        """Test for register template
        """
        # Validate the following:
        # 1. Register template and check name

        if self.hypervisor.lower() in ["lxc"]:
            self.skipTest("Skipping test, unsupported hypervisor %s" % self.hypervisor)

        builtin_info = get_builtin_template_info(self.apiclient, self.zone.id)
        self.services["template_2"]["url"] = builtin_info[0]
        self.services["template_2"]["hypervisor"] = builtin_info[1]
        self.services["template_2"]["format"] = builtin_info[2]
        self.services["template_2"]["name"] = "test🎉tmpl🙂"
        self.services["template_2"]["displaytext"] = "test🎉tmpl🙂"

        template = Template.register(self.apiclient,
                                     self.services["template_2"],
                                     zoneid=self.zone.id,
                                     account=self.account.name,
                                     domainid=self.account.domainid
                                     )
        self.debug("Successfully registered template with ID: %s" % template.id)
        self.cleanup.append(template)

        # Get template response
        timeout = 600
        list_template_response = None
        while timeout >= 0:
            list_template_response = Template.list(self.apiclient,
                                                   templatefilter=self.services["template_2"]["templatefilter"],
                                                   id=template.id)

            if list_template_response is not None and list_template_response[0].isready:
                break

            time.sleep(30)
            timeout -= 30

        template_response = list_template_response[0]
        self.assertEqual(
            template_response.displaytext,
            template.displaytext,
            "Check template displaytext in response"
        )

    @attr(tags=["advanced", "smoke", "basic"], required_hardware="true")
    def test_04_register_iso(self):
        """Test for register ISO
        """
        # Validate the following:
        # 1. Register ISO and check name

        if self.hypervisor.lower() in ["lxc"]:
            self.skipTest("Skipping test, unsupported hypervisor %s" % self.hypervisor)

        self.services["iso1"]["displaytext"] = "test🎉iso🙂"
        self.services["iso1"]["name"] = "test🎉iso🙂"
        iso = Iso.create(
            self.apiclient,
            self.services["iso1"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Successfully registered ISO with ID: %s" % iso.id)
        self.cleanup.append(iso)

        # Get ISO response
        timeout = 600
        list_iso_response = None
        while timeout >= 0:
            list_iso_response = Iso.list(
                self.apiclient,
                isofilter="self",
                id=iso.id
            )

            if list_iso_response is not None and list_iso_response[0].isready:
                break

            time.sleep(30)
            timeout -= 30

        iso_response = list_iso_response[0]
        self.assertEqual(
            iso_response.displaytext,
            iso.displaytext,
            "Check ISO displaytext in response"
        )
