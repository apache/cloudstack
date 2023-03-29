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
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import random_gen, cleanup_resources, validateList, is_snapshot_on_nfs, isAlmostEqual
from marvin.cloudstackAPI import (deleteVMSchedule,
                                  createVMSchedule,
                                  listVMSchedules,
                                  updateVMSchedule,
                                  enableVMSchedule,
                                  disableVMSchedule)

from marvin.lib.base import * # import all resources like Account
from marvin.lib.common import * # import all utility methods like get_zone etc.

import time
import os

class Services:

    """Test VM Schedule Services
    """

    def __init__(self):
        self.services = {
            "domain": {
                "name": "Domain",
            },
            "vm_schedule": {
                "virtualmachineid" : 9,
                "description": "start vm",
                "action": "start",
                "schedule": "30",
                "intervaltype": "HOURLY",
                "tag": "hello",
                "timezone": "Asia/Kolkata"
            }
        }

class TestVmSchedule(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestVmSchedule, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get attributes
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.domain = get_domain(cls.apiclient)
        cls.hypervisor = testClient.getHypervisorInfo()

        cls.cleanup = []

        template = get_suitable_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )

        if template == FAILED:
            assert False, "get_suitable_test_template() failed to return template\
                    with description %s" % cls.services["ostype"]

        cls.services["domainid"] = cls.domain.id
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["templates"]["ostypeid"] = template.ostypeid
        cls.services["zoneid"] = cls.zone.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls.cleanup.append(cls.account)

        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls.cleanup.append(cls.service_offering)

        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["small"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            mode=cls.zone.networktype
        )
   #     cls.cleanup.append(cls.virtual_machine)

    # Class's tearDown method, runs only once per test class after test cases
    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    # Define setUp that runs before each test case
    def setUp(self):
        # Define probes here
        # Probe: apiclient to make API calls
        self.apiclient = self.testClient.getApiClient()
        # Probe: DB client to query the database
        self.dbclient = self.testClient.getDbConnection()
        # Get hypervisor detail
        self.hypervisor = self.testClient.getHypervisorInfo()
        # Command to get the default test_data config
        self.services = self.testClient.getParsedTestDataConfig()
        # List to hold any resources requiring cleanup
        self.cleanup = []

    # Define tearDown that runs after each test case
    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    @attr(tags=["advanced", "advancedns", "smoke", "simulator"], required_hardware="true")
    def test_01_create_vm_schedule(self):
        """Test to create VM schedule
        """
        cmd = createVMSchedule.createVMScheduleCmd()
        cmd.virtualmachineid = self.virtual_machine.id
        cmd.action = "start"
        cmd.schedule = "30"
        cmd.intervaltype = "HOURLY"
        cmd.description = "start vm"
        cmd.tag = "hello"
        cmd.timezone = "Asia/Kolkata"

        response = self.apiclient.createVMSchedule(cmd)
        print(response)

        self.assertEqual(
            response.action,
            "start",
            "Check the action of vm is start!"
        )
        return

    @attr(tags=["advanced", "advancedns", "smoke", "simulator"], required_hardware="true")
    def test_02_list_vm_schedules(self):
        """Test to list VM schedules
        """

        cmd = listVMSchedules.listVMSchedulesCmd()
        cmd.virtualmachineid = self.virtual_machine.id

        response = self.apiclient.listVMSchedules(cmd)

        self.assertEqual(response[0].action,
        "start",
        "check the action of vm schedule is start!"
        )
        return

    @attr(tags=["advanced", "advancedns", "smoke", "simulator"], required_hardware="true")
    def test_03_update_vm_schedule(self):
        """Test to update VM schedules
        """

        cmd = updateVMSchedule.updateVMScheduleCmd()
        cmd.virtualmachineid = self.virtual_machine.id
        cmd.description = "update description of vm"
        cmd.tag = "bye"
        cmd.intervaltype = "DAILY"
        cmd.schedule = "45"

        response = self.apiclient.updateVMSchedule(cmd)

        self.assertEqual(
        response.schedule,
        "45",
        "Check the action of vm is start!"
        )
        return

    @attr(tags=["advanced", "advancedns", "smoke", "simulator"], required_hardware="true")
    def test_04_enable_vm_schedule(self):
        """Test to enable VM schedules
        """

        cmd = enableVMSchedule.enableVMScheduleCmd()
        cmd.vmscheduleid = 91

        response = self.apiclient.enableVMSchedule(cmd)

        self.assertEqual(
        response.success,
        True,
        "VM schedule is not enabled "
        )
        return

    @attr(tags=["advanced", "advancedns", "smoke", "simulator"], required_hardware="true")
    def test_05_disable_vm_schedule(self):
        """Test to disable VM schedules
        """

        cmd = disableVMSchedule.disableVMScheduleCmd()
        cmd.vmscheduleid = 91

        response = self.apiclient.disableVMSchedule(cmd)

        self.assertEqual(
        response.success,
        True,
        "VM schedule is not disabled"
        )
        return

    @attr(tags=["advanced", "advancedns", "smoke", "simulator"], required_hardware="true")
    def test_06_delete_vm_schedule(self):
        """Test to delete VM schedules
        """

        cmd = deleteVMSchedule.deleteVMScheduleCmd()
        cmd.vmscheduleid = 91

        response = self.apiclient.deleteVMSchedule(cmd)

        self.assertEqual(
        response.success,
        True,
        "VM schedule is not removed "
        )
        return
