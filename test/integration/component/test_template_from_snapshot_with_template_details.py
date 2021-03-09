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
""" P1 tests for Templates
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.cloudstackAPI import listZones
from marvin.lib.utils import (cleanup_resources)
from marvin.lib.base import (Account,
                             Domain,
                             Template,
                             ServiceOffering,
                             VirtualMachine,
                             Snapshot,
                             Volume)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               get_builtin_template_info,
                               list_volumes,
                               list_snapshots)
# Import System modules
import time


class Services:

    """Test Templates Services
    """

    def __init__(self):
        self.services = {
            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                # Random characters are appended for unique
                # username
                "password": "password",
            },
            "testdomain": {"name": "test"},
            "service_offering": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100,    # in MHz
                "memory": 128,       # In MBs
            },
            "disk_offering": {
                "displaytext": "Small",
                "name": "Small",
                "disksize": 1
            },
            "virtual_machine": {
                "displayname": "testVM",
                "hypervisor": 'XenServer',
                "protocol": 'TCP',
                "ssh_port": 22,
                "username": "root",
                "password": "password",
                "privateport": 22,
                "publicport": 22,
            },
            "volume": {
                "diskname": "Test Volume",
            },
            "templates": {
                # Configs for different Template formats
                # For Eg. raw image, zip etc
                0: {
                    "displaytext": "Public Template",
                    "name": "Public template",
                    "ostype": 'CentOS 5.3 (64-bit)',
                    "url": "http://download.cloudstack.org/releases/2.0.0/UbuntuServer-10-04-64bit.vhd.bz2",
                    "hypervisor": 'XenServer',
                    "format": 'VHD',
                    "isfeatured": True,
                    "ispublic": True,
                    "isextractable": True,
                },
            },
            "template": {
                "displaytext": "Cent OS Template",
                "name": "Cent OS Template",
                "ostype": 'CentOS 5.3 (64-bit)',
                "templatefilter": 'self',
                "isfeatured": True,
                "ispublic": True,
            },
            "templatefilter": 'self',
            "ostype": 'CentOS 5.3 (64-bit)',
            "sleep": 60,
            "timeout": 10,
        }


class TestCreateTemplate(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.unsupportedHypervisor:
            self.skipTest(
                "Template creation from root volume is not supported in LXC")
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestCreateTemplate, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls._cleanup = []
        cls.unsupportedHypervisor = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['lxc']:
            cls.unsupportedHypervisor = True
            return
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)
        cls.services["account"] = cls.account.name
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(
                TestCreateTemplate,
                cls).getClsTestClient().getApiClient()
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return
    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    def test_01_create_template_snampshot(self):

        builtin_info = get_builtin_template_info(self.apiclient, self.zone.id)
        self.services["templates"][0]["url"] = builtin_info[0]
        self.services["templates"][0]["hypervisor"] = builtin_info[1]
        self.services["templates"][0]["format"] = builtin_info[2]

        # Register new template
        template = Template.register(
            self.apiclient,
            self.services["templates"][0],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            hypervisor=self.hypervisor,
            details=[{"keyboard":"us","nicAdapter":"e1000","rootDiskController":"scsi"}]
        )
        self.debug(
            "Registered a template of format: %s with ID: %s" % (
                self.services["templates"][0]["format"],
                template.id
            ))
        # Wait for template to download
        template.download(self.apiclient)
        self.cleanup.append(template)

        # Wait for template status to be changed across
        time.sleep(self.services["sleep"])
        timeout = self.services["timeout"]
        while True:
            list_template_response = Template.list(
                self.apiclient,
                templatefilter='all',
                id=template.id,
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid)
            if isinstance(list_template_response, list):
                break
            elif timeout == 0:
                raise Exception("List template failed!")

            time.sleep(5)
            timeout = timeout - 1
        # Verify template response to check whether template added successfully
        self.assertEqual(
            isinstance(list_template_response, list),
            True,
            "Check for list template response return valid data"
        )

        self.assertNotEqual(
            len(list_template_response),
            0,
            "Check template available in List Templates"
        )

        template_response = list_template_response[0]
        self.assertEqual(
            template_response.isready,
            True,
            "Template state is not ready, it is %s" % template_response.isready
        )

        self.assertIsNotNone(
            template_response.details,
            "Template details is %s" % template_response.details
        )

        # Deploy new virtual machine using template
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.services["mode"]
        )
        self.debug("creating an instance with template ID: %s" % template.id)
        vm_response = VirtualMachine.list(self.apiclient,
                                          id=virtual_machine.id,
                                          account=self.account.name,
                                          domainid=self.account.domainid)
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
        vm = vm_response[0]
        self.assertEqual(
            vm.state,
            'Running',
            "Check the state of VM created from Template"
        )

        volumes = list_volumes(
            self.apiclient,
            virtualmachineid=vm.id,
            type='ROOT',
            listall=True
        )

        snapshot = Snapshot.create(
            self.apiclient,
            volumes[0].id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        time.sleep(self.services["sleep"])
        self.cleanup.append(snapshot)
        self.debug("Snapshot created: ID - %s" % snapshot.id)

        snapshots = list_snapshots(
            self.apiclient,
            id=snapshot.id
        )
        self.assertEqual(
            isinstance(snapshots, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            snapshots,
            None,
            "Check if result exists in list item call"
        )
        self.assertEqual(
            snapshots[0].id,
            snapshot.id,
            "Check resource id in list resources call"
        )

        virtual_machine.delete(self.apiclient, expunge=False)

        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine.id
        )

        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_vm_response),
            0,
            "Check VM avaliable in List Virtual Machines"
        )

        template = Template.create_from_snapshot(
            self.apiclient,
            snapshot,
            self.services["template"]
        )
        self.cleanup.append(template)
        # Verify created template
        templates = Template.list(
            self.apiclient     ,
            templatefilter=self.services["template"]["templatefilter"],
            id=template.id
        )
        self.assertNotEqual(
            templates,
            None,
            "Check if result exists in list item call"
        )

        self.assertEqual(
            templates[0].id,
            template.id,
            "Check new template id in list resources call"
        )


        self.assertIsNotNone(
            templates[0].details,
            "Template details is %s" % template_response.details
        )

        return
