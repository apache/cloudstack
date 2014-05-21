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
""" P1 tests for OVA templates with multiple disks
"""
import marvin
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
import urllib
from random import random
import time
from ddt import ddt

class Services:
    """Test OVA template with mutiple disks
    """

    def __init__(self):
        self.services = {
                        "account": {
                                    "email": "test@test.com",
                                    "firstname": "Test",
                                    "lastname": "User",
                                    "username": "test",
                                    "password": "password",
                         },
                         "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 100, # in MHz
                                    "memory": 128, # In MBs
                        },
                        "disk_offering": {
                                    "displaytext": "Small",
                                    "name": "Small",
                        },
                        "virtual_machine": {
                                    "displayname": "testVM",
                                    "hypervisor": 'VMware',
                                    "protocol": 'TCP',
                                    "ssh_port": 22,
                                    "username": "root",
                                    "password": "password",
                                    "privateport": 22,
                                    "publicport": 22,
                         },
                        "template": {
                                "displaytext": "Template with multiple disks",
                                "name": "Template with multiple disks",
                                "isfeatured": True,
                                "ispublic": True,
                                "isextractable": False,
                        },
                        "sleep": 60,
                        "timeout": 10,
                        "format": 'ova',
                     }

@ddt
class TestOVATemplateWithMupltipleDisks(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
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
        cls.testClient = super(TestOVATemplateWithMupltipleDisks, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.domain = get_domain(cls.api_client)
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        # Disk offering size should be greater than datadisk template size
        cls.services["disk_offering"]["disksize"] = 10
        cls.disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering"]
                                    )
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.services["account"] = cls.account.name

        cls._cleanup = [
                        cls.account,
                        cls.service_offering,
                        cls.disk_offering,
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(TestOVATemplateWithMupltipleDisks, cls).getClsTestClient().getApiClient()
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags = ["vmware"])
    def test_01_template_with_multiple_disks(self):
        """Test template with 1 data disks
        """
        # Validate the following:
        # 1. Register a template in OVA format that contains 1 data disks
        # 2. Verify template is in READY state
        # 3. Veriy 1 additonal Datadisk Template got created
        # 3. Deploy a VM from the registered template and 1 datadisk template
        # 4. Verify VM is in Running state
        # 5. Verify an additional data disk attached to the VM

        # Register new template
        self.services["template"]["url"] = 'http://10.147.28.7/templates/single-datadisk-template.ova'
        self.services["template"]["format"] = 'OVA'
        self.services["template"]["ostype"] = 'CentOS 5.3 (64-bit)'
        registered_template = Template.register(
                                        self.apiclient,
                                        self.services["template"],
                                        zoneid=self.zone.id,
                                        account=self.account.name,
                                        domainid=self.account.domainid,
                                        hypervisor='VMware'
                                        )
        self.debug(
                "Registered a template of format: %s with id: %s" % (
                                                                self.services["template"]["format"],
                                                                registered_template.id
                                                                ))
        # Wait for template to download
        registered_template.download(self.apiclient)
        self.cleanup.append(registered_template)

        # Wait for template status to be changed across
        time.sleep(self.services["sleep"])
        timeout = self.services["timeout"]
        while True:
            list_template_response = list_templates(
                                    self.apiclient,
                                    templatefilter='all',
                                    id=registered_template.id,
                                    zoneid=self.zone.id,
                                    account=self.account.name,
                                    domainid=self.account.domainid
                                    )
            if isinstance(list_template_response, list):
                break
            elif timeout == 0:
                raise Exception("List template failed!")

            time.sleep(5)
            timeout = timeout - 1
        # Verify template response to check if template was successfully added
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

        # Veriy 1 additonal Datadisk Templates got created
        list_datadisk_template_response = list_templates(
                                    self.apiclient,
                                    templatefilter='self',
                                    parenttemplateid=registered_template.id,
                                    zoneid=self.zone.id,
                                    account=self.account.name,
                                    domainid=self.account.domainid
                                    )

        self.assertEqual(
                        isinstance(list_datadisk_template_response, list),
                        True,
                        "Check for datadisk list template response return valid data"
                        )

        self.assertNotEqual(
                            len(list_datadisk_template_response),
                            0,
                            "Check datadisk template available in List Templates"
                        )

        datadisk_template_response = list_datadisk_template_response[0]
        self.assertEqual(
                            datadisk_template_response.isready,
                            True,
                            "Datadisk template state is not ready, it is %s" % datadisk_template_response.isready
                        )

        # Deploy new virtual machine using template
        datadisktemplate_diskoffering_list = {datadisk_template_response.id: self.disk_offering.id}
        virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    templateid=registered_template.id,
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    datadisktemplate_diskoffering_list=datadisktemplate_diskoffering_list
                                    )
        self.debug("Creating an instance with template ID: %s" % registered_template.id)
        vm_response = list_virtual_machines(
                                        self.apiclient,
                                        id=virtual_machine.id,
                                        account=self.account.name,
                                        domainid=self.account.domainid
                                        )
        self.assertEqual(
                             isinstance(vm_response, list),
                             True,
                             "Check for list VMs response after VM deployment"
                             )
        # Verify VM response to check if VM deployment was successful
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

        # Check 1 DATA volume is attached to the VM
        list_volume_response = list_volumes(
                                    self.apiclient,
                                    virtualmachineid=vm.id,
                                    type='DATADISK',
                                    listall=True
                                    )
        self.assertNotEqual(
            list_volume_response,
            None,
            "Check if additinal data volume is attached to VM %s "
        )
        self.assertEqual(
            isinstance(list_volume_response, list),
            True,
            "Check list volumes response for valid list"
        )
        self.assertEqual(
            len(list_volume_response),
            1,
            "Additional DATA volume attached to the VM %s. Expected %s" % (len(list_volume_response), 1)
        )

        return
