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
""" P1 tests for VM Schedule
"""
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import Account, ServiceOffering, VirtualMachine, VMSchedule
from marvin.lib.common import get_domain, get_zone, get_template
from marvin.lib.utils import cleanup_resources

# Import Local Modules
from nose.plugins.attrib import attr

import datetime
import time


class Services:
    """Test Snapshots Services"""

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
            "service_offering": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 200,  # in MHz
                "memory": 256,  # In MBs
            },
            "disk_offering": {
                "displaytext": "Small Disk",
                "name": "Small Disk",
                "disksize": 1,
            },
            "server": {
                "displayname": "TestVM",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "privateport": 22,
                "publicport": 22,
                "protocol": "TCP",
            },
            "mgmt_server": {
                "ipaddress": "192.168.100.21",
                "username": "root",
                "password": "password",
                "port": 22,
            },
            "templates": {
                "displaytext": "Template",
                "name": "Template",
                "ostype": "CentOS 5.3 (64-bit)",
                "templatefilter": "self",
            },
            "ostype": "CentOS 5.3 (64-bit)",
        }


class TestVMSchedule(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVMSchedule, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls._cleanup = []

        cls.hypervisor = cls.testClient.getHypervisorInfo()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services["mode"] = cls.zone.networktype
        template = get_template(cls.api_client, cls.zone.id, cls.services["ostype"])

        cls.services["domainid"] = cls.domain.id
        cls.services["server"]["zoneid"] = cls.zone.id

        cls.services["templates"]["ostypeid"] = template.ostypeid
        cls.services["zoneid"] = cls.zone.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.api_client, cls.services["account"], domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
            cls.api_client, cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        cls.virtual_machine = VirtualMachine.create(
            cls.api_client,
            cls.services["server"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
        )
        return

    @classmethod
    def tearDownClass(cls):
        super(TestVMSchedule, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestVMSchedule, self).tearDown()

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_01_vmschedule_create(self):
        """Test VM Schedule Creation in cron format and validate responses"""

        # Validate the following
        # 1. Create VM Schedule in cron format
        # 2. List VM Schedule and verify the response
        # 3. Delete VM Schedule and verify the response

        # Create VM Schedule
        schedule = "0 0 1 * *"
        vmschedule = VMSchedule.create(
            self.apiclient,
            self.virtual_machine.id,
            "start",
            schedule,
            datetime.datetime.now().astimezone().tzinfo,
            # Current date minutes in format "2014-01-01 00:00:00"
            (datetime.datetime.now() + datetime.timedelta(minutes=5)).strftime(
                "%Y-%m-%d %H:%M:%S"
            ),
            enabled=True,
        )

        self.cleanup.append(vmschedule)

        self.debug("Created VM Schedule with ID: %s" % vmschedule.id)

        # List VM Schedule
        vmschedules = VMSchedule.list(
            self.apiclient, self.virtual_machine.id, id=vmschedule.id
        )

        self.assertEqual(
            isinstance(vmschedules, list),
            True,
            "Check list response returns a valid list",
        )

        self.assertNotEqual(len(vmschedules), 0, "Check VM Schedule list")

        self.debug("List VM Schedule response: %s" % vmschedules[0].__dict__)

        self.assertEqual(
            vmschedules[0].id,
            vmschedule.id,
            "Check VM Schedule ID in list resources call",
        )

        self.assertEqual(
            vmschedules[0].virtualmachineid,
            self.virtual_machine.id,
            "Check VM ID in list resources call",
        )

        self.assertEqual(
            vmschedules[0].schedule,
            schedule,
            "Check VM Schedule in list resources call",
        )

        self.assertEqual(
            vmschedules[0].timezone,
            str(datetime.datetime.now().astimezone().tzinfo),
            "Check VM Schedule timezone in list resources call",
        )

        # Check for entry in vm_scheduled_job in db
        vmscheduled_job = self.dbclient.execute(
            "select * from vm_scheduled_job where vm_schedule_id IN (SELECT id FROM vm_schedule WHERE uuid = '%s')"
            % vmschedule.id,
            db="cloud",
        )

        self.assertIsInstance(
            vmscheduled_job,
            list,
            "Check if VM Schedule exists in vm_scheduled_job table",
        )

        self.assertGreater(
            len(vmscheduled_job),
            0,
            "Check if VM Schedule exists in vm_scheduled_job table",
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_02_vmschedule_create_parameter_exceptions(self):
        """Test VM Schedule Creation exceptions with invalid parameters"""

        # Validate the following
        # 1. Create VM Schedule with invalid virtual machine ID
        # 2. Create VM Schedule with invalid schedule
        # 3. Create VM Schedule with invalid start date
        # 5. Create VM Schedule with invalid action
        # 6. Create VM Schedule with invalid end date

        # Create VM Schedule with invalid virtual machine ID
        with self.assertRaises(Exception):
            VMSchedule.create(
                self.apiclient,
                "invalid",
                "start",
                "0 0 1 * *",
                datetime.datetime.now().astimezone().tzinfo,
                # Current date minutes in format "2014-01-01 00:00:00"
                (datetime.datetime.now() + datetime.timedelta(minutes=5)).strftime(
                    "%Y-%m-%d %H:%M:%S"
                ),
            )

        # Create VM Schedule with invalid schedule
        with self.assertRaises(Exception):
            VMSchedule.create(
                self.apiclient,
                self.virtual_machine.id,
                "start",
                "invalid",
                datetime.datetime.now().astimezone().tzinfo,
                # Current date minutes in format "2014-01-01 00:00:00"
                (datetime.datetime.now() + datetime.timedelta(minutes=5)).strftime(
                    "%Y-%m-%d %H:%M:%S"
                ),
            )

        # Create VM Schedule with invalid start date
        with self.assertRaises(Exception):
            VMSchedule.create(
                self.apiclient,
                self.virtual_machine.id,
                "start",
                "0 0 1 * *",
                datetime.datetime.now().astimezone().tzinfo,
                # Current date minutes in format "2014-01-01 00:00:00"
                "invalid",
            )

        # Create VM Schedule with invalid action
        with self.assertRaises(Exception):
            VMSchedule.create(
                self.apiclient,
                self.virtual_machine.id,
                "invalid",
                "0 0 1 * *",
                datetime.datetime.now().astimezone().tzinfo,
                # Current date minutes in format "2014-01-01 00:00:00"
                (datetime.datetime.now() + datetime.timedelta(minutes=5)).strftime(
                    "%Y-%m-%d %H:%M:%S"
                ),
            )

        # test invalid end date
        with self.assertRaises(Exception):
            VMSchedule.create(
                self.apiclient,
                self.virtual_machine.id,
                "start",
                "0 0 1 * *",
                datetime.datetime.now().astimezone().tzinfo,
                # Current date minutes in format "2014-01-01 00:00:00"
                (datetime.datetime.now() + datetime.timedelta(minutes=5)).strftime(
                    "%Y-%m-%d %H:%M:%S"
                ),
                enddate="invalid",
            )
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_03_vmschedule_update(self):
        """Test VM Schedule Update in cron format and validate responses"""

        # Validate the following
        # 1. Create VM Schedule in cron format
        # 2. Update VM Schedule and verify the response

        # Create VM Schedule
        schedule = "0 0 1 * *"
        vmschedule = VMSchedule.create(
            self.apiclient,
            self.virtual_machine.id,
            "start",
            schedule,
            datetime.datetime.now().astimezone().tzinfo,
            # Current date minutes in format "2014-01-01 00:00:00"
            (datetime.datetime.now() + datetime.timedelta(minutes=5)).strftime(
                "%Y-%m-%d %H:%M:%S"
            ),
        )

        self.cleanup.append(vmschedule)

        self.debug("Created VM Schedule with ID: %s" % vmschedule.id)

        # Update VM Schedule
        new_schedule = "0 0 2 * *"
        vmschedule.update(
            self.apiclient,
            id=vmschedule.id,
            virtualmachineid=self.virtual_machine.id,
            description="TestVM",
            schedule=new_schedule,
            timezone=datetime.datetime.now().astimezone().tzinfo,
            startdate=(
                datetime.datetime.now() + datetime.timedelta(minutes=10)
            ).strftime("%Y-%m-%d %H:%M:%S"),
            enddate=(datetime.datetime.now() + datetime.timedelta(hours=10)).strftime(
                "%Y-%m-%d %H:%M:%S"
            ),
        )

        self.debug("Updated VM Schedule with ID: %s" % vmschedule.id)

        # List VM Schedule
        vmschedules = VMSchedule.list(
            self.apiclient, self.virtual_machine.id, id=vmschedule.id
        )
        self.assertEqual(
            isinstance(vmschedules, list),
            True,
            "Check list response returns a valid list",
        )

        self.assertNotEqual(len(vmschedules), 0, "Check VM Schedule list")

        self.debug("List VM Schedule response: %s" % vmschedules[0].__dict__)

        self.assertEqual(
            vmschedules[0].id,
            vmschedule.id,
            "Check VM Schedule ID in list resources call",
        )

        self.assertEqual(
            vmschedules[0].virtualmachineid,
            self.virtual_machine.id,
            "Check VM ID in list resources call",
        )

        self.assertEqual(
            vmschedules[0].schedule,
            new_schedule,
            "Check VM Schedule in list resources call",
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_04_vmschedule_update_parameter_exceptions(self):
        """Test VM Schedule Update exceptions with invalid parameters"""

        # Validate the following
        # 1. Update VM Schedule with invalid schedule
        # 2. Update VM Schedule with invalid start date
        # 3. Update VM Schedule with invalid ID
        # 4. Update VM Schedule with invalid end date

        # Create VM Schedule
        schedule = "0 0 1 * *"
        vmschedule = VMSchedule.create(
            self.apiclient,
            self.virtual_machine.id,
            "start",
            schedule,
            datetime.datetime.now().astimezone().tzinfo,
            # Current date minutes in format "2014-01-01 00:00:00"
            (datetime.datetime.now() + datetime.timedelta(minutes=5)).strftime(
                "%Y-%m-%d %H:%M:%S"
            ),
        )

        self.cleanup.append(vmschedule)

        self.debug("Created VM Schedule with ID: %s" % vmschedule.id)

        # Update VM Schedule with invalid schedule
        with self.assertRaises(Exception):
            vmschedule.update(
                self.apiclient,
                id=vmschedule.id,
                virtualmachineid=self.virtual_machine.id,
                description="TestVM",
                schedule="invalid",
                timezone=datetime.datetime.now().astimezone().tzinfo,
                startdate=(
                    datetime.datetime.now() + datetime.timedelta(minutes=5)
                ).strftime("%Y-%m-%d %H:%M:%S"),
            )

        # Update VM Schedule with invalid start date
        with self.assertRaises(Exception):
            vmschedule.update(
                self.apiclient,
                id=vmschedule.id,
                virtualmachineid=self.virtual_machine.id,
                description="TestVM",
                schedule=schedule,
                timezone=datetime.datetime.now().astimezone().tzinfo,
                startdate=(
                    datetime.datetime.now() - datetime.timedelta(days=1)
                ).strftime("%Y-%m-%d %H:%M:%S"),
            )

        # Update VM Schedule with invalid ID
        with self.assertRaises(Exception):
            vmschedule.update(
                self.apiclient,
                id="invalid",
                virtualmachineid=self.virtual_machine.id,
                description="TestVM",
                schedule=schedule,
                timezone=datetime.datetime.now().astimezone().tzinfo,
                startdate=(
                    datetime.datetime.now() + datetime.timedelta(minutes=5)
                ).strftime("%Y-%m-%d %H:%M:%S"),
            )

        # Update VM Schedule with invalid end date
        with self.assertRaises(Exception):
            vmschedule.update(
                self.apiclient,
                id=vmschedule.id,
                virtualmachineid=self.virtual_machine.id,
                description="TestVM",
                schedule=schedule,
                timezone=datetime.datetime.now().astimezone().tzinfo,
                startdate=(
                    datetime.datetime.now() + datetime.timedelta(minutes=5)
                ).strftime("%Y-%m-%d %H:%M:%S"),
                enddate=(
                    datetime.datetime.now() - datetime.timedelta(minutes=5)
                ).strftime("%Y-%m-%d %H:%M:%S"),
            )

        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_05_vmschedule_test_e2e(self):
        # Validate the following
        # 1. Create 2 VM Schedules - start and stop
        # 2. Verify VM Schedule is created
        # 3. Verify VM is stopped after schedule time
        # 4. Verify VM is started after schedule time
        # 5. Delete VM Schedule
        # 6. Verify VM Schedule is deleted
        # 7. Verify VM is not stopped after schedule time
        # 8. Verify VM is not started after schedule time

        # Create VM Schedule - start
        start_schedule = "*/2 * * * *"
        start_vmschedule = VMSchedule.create(
            self.apiclient,
            self.virtual_machine.id,
            "start",
            start_schedule,
            datetime.datetime.now().astimezone().tzinfo,
            # Current date minutes in format "2014-01-01 00:00:00"
            (datetime.datetime.now() + datetime.timedelta(seconds=5)).strftime(
                "%Y-%m-%d %H:%M:%S"
            ),
            enabled=True,
        )

        self.debug("Created VM Schedule with ID: %s" % start_vmschedule.id)

        # Create VM Schedule - stop
        stop_schedule = "*/1 * * * *"
        stop_vmschedule = VMSchedule.create(
            self.apiclient,
            self.virtual_machine.id,
            "stop",
            stop_schedule,
            datetime.datetime.now().astimezone().tzinfo,
            # Current date minutes in format "2014-01-01 00:00:00"
            (datetime.datetime.now() + datetime.timedelta(seconds=5)).strftime(
                "%Y-%m-%d %H:%M:%S"
            ),
            enabled=True,
        )

        self.debug("Created VM Schedule with ID: %s" % stop_vmschedule.id)

        # Verify VM Schedule is created
        vmschedules = VMSchedule.list(
            self.apiclient, self.virtual_machine.id, id=start_vmschedule.id
        )

        self.assertEqual(
            isinstance(vmschedules, list),
            True,
            "Check list response returns a valid list",
        )
        self.assertNotEqual(len(vmschedules), 0, "Check VM Schedule is created")

        # poll every 10 seconds (max waiting time is 6 minutes) and check VM's state for changes
        previous_state = self.virtual_machine.state
        self.debug("VM state: %s" % self.virtual_machine.state)
        is_stop_schedule_working = False
        is_start_schedule_working = False
        for i in range(0, 36):
            time.sleep(10)
            current_state = self.virtual_machine.update(self.apiclient).state
            self.debug("Polling VM state: %s" % current_state)
            if previous_state in ("Running", "Starting") and current_state in (
                "Stopped",
                "Stopping",
            ):
                is_stop_schedule_working = True
            elif previous_state in ("Stopped", "Stopping") and current_state in (
                "Running",
                "Starting",
            ):
                is_start_schedule_working = True
            if is_start_schedule_working and is_stop_schedule_working:
                break
            previous_state = current_state

        self.debug("Is stop schedule working: %s" % is_stop_schedule_working)
        self.debug("Is start schedule working: %s" % is_start_schedule_working)

        self.assertTrue(
            is_stop_schedule_working,
            "VM switched states from Running to Stopped at least once",
        )

        self.assertTrue(
            is_start_schedule_working,
            "VM switched states from Stopped to Running at least once",
        )

        # Delete VM Schedule
        start_vmschedule.delete(self.apiclient)
        stop_vmschedule.delete(self.apiclient)

        # To ensure that all vm schedules have been deleted and all of their jobs have been completed
        time.sleep(60)

        # Verify VM Schedule is deleted
        self.assertEqual(
            VMSchedule.list(
                self.apiclient, self.virtual_machine.id, id=start_vmschedule.id
            ),
            None,
            "Check VM Schedule is deleted",
        )
        self.assertEqual(
            VMSchedule.list(
                self.apiclient, self.virtual_machine.id, id=stop_vmschedule.id
            ),
            None,
            "Check VM Schedule is deleted",
        )

        # Verify VM does not switch states after deleting schedules at least for 2 minutes
        previous_state = self.virtual_machine.update(self.apiclient).state
        state_changed = False
        for i in range(0, 4):
            time.sleep(30)
            current_state = self.virtual_machine.update(self.apiclient).state
            if previous_state != current_state:
                self.debug(
                    "VM changed state from %s to %s" % (previous_state, current_state)
                )
                state_changed = True
                break

        self.assertFalse(
            state_changed,
            "VM did not switch states after schedule time",
        )
        return
