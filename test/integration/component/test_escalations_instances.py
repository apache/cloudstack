# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.utils import cleanup_resources, validateList
from marvin.lib.base import (Account,
                             ServiceOffering,
                             NetworkOffering,
                             Network,
                             VirtualMachine,
                             SecurityGroup,
                             DiskOffering,
                             Resources,
                             Iso,
                             Configurations,
                             SSHKeyPair,
                             Volume,
                             VmSnapshot,
                             Zone,
                             Template,
                             Host)
from marvin.lib.common import (get_zone,
                               get_template,
                               get_domain,
                               find_storage_pool_type)
from marvin.codes import PASS
from marvin.sshClient import SshClient
from nose.plugins.attrib import attr
import time
from marvin.cloudstackException import CloudstackAPIException

class TestListInstances(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(TestListInstances, cls).getClsTestClient()
            cls.api_client = cls.testClient.getApiClient()
            cls.services = cls.testClient.getParsedTestDataConfig()
            cls.hypervisor = cls.testClient.getHypervisorInfo()
            # Get Domain, Zone, Template
            cls.domain = get_domain(cls.api_client)
            cls.zone = get_zone(
                cls.api_client,
                cls.testClient.getZoneForTests())
            cls.template = get_template(
                cls.api_client,
                cls.zone.id,
                cls.services["ostype"]
            )
            if cls.zone.localstorageenabled:
                cls.storagetype = 'local'
                cls.services["service_offerings"][
                    "tiny"]["storagetype"] = 'local'
                cls.services["disk_offering"]["storagetype"] = 'local'
            else:
                cls.storagetype = 'shared'
                cls.services["service_offerings"][
                    "tiny"]["storagetype"] = 'shared'
                cls.services["disk_offering"]["storagetype"] = 'shared'

            cls.services['mode'] = cls.zone.networktype
            cls.services["virtual_machine"]["hypervisor"] = cls.hypervisor
            cls.services["virtual_machine"]["zoneid"] = cls.zone.id
            cls.services["virtual_machine"]["template"] = cls.template.id
            cls.services["custom_volume"]["zoneid"] = cls.zone.id
            # Creating Disk offering, Service Offering and Account
            cls.disk_offering = DiskOffering.create(
                cls.api_client,
                cls.services["disk_offering"]
            )
            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.services["service_offerings"]["tiny"]
            )
            cls._cleanup.append(cls.service_offering)
            cls._cleanup.append(cls.disk_offering)
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setup : %s" % e)
        return

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()
        self.account = Account.create(
            self.apiClient,
            self.services["account"],
            domainid=self.domain.id
        )
        # Getting authentication for user in newly created Account
        self.user = self.account.user[0]
        self.userapiclient = self.testClient.getUserApiClient(
            self.user.username,
            self.domain.name)
        # Updating resource Limits
        for i in range(0, 12):
            Resources.updateLimit(
                self.api_client,
                account=self.account.name,
                domainid=self.domain.id,
                max=-1,
                resourcetype=i
            )

        self.cleanup = [self.account, ]

    def tearDown(self):
        # Clean up, terminate the created resources
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def __verify_values(self, expected_vals, actual_vals):
        """
        @Desc: Function to verify expected and actual values
        @Steps:
        Step1: Initializing return flag to True
        Step1: Verifying length of expected and actual dictionaries is matching
               If not matching returning false
        Step2: Listing all the keys from expected dictionary
        Step3: Looping through each key from step2 and verifying expected and
               actual dictionaries have same value
               If not making return flag to False
        Step4: returning the return flag after all the values are verified
        """
        return_flag = True

        if len(expected_vals) != len(actual_vals):
            return False

        keys = list(expected_vals.keys())
        for i in range(0, len(expected_vals)):
            exp_val = expected_vals[keys[i]]
            act_val = actual_vals[keys[i]]
            if exp_val == act_val:
                return_flag = return_flag and True
            else:
                return_flag = return_flag and False
                self.debug(
                    "expected Value: %s, is not matching with\
                            actual value: %s" %
                    (exp_val, act_val))
        return return_flag

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_01_list_instances_pagination(self):
        """
        @Desc: Test List Instances pagination
        @Steps:
        Step1: Listing all the Instances for a user
        Step2: Verifying listed Instances for account created at class level
        Step3: If number of volumes is less than
                (page size + 1), then creating them
        Step4: Listing all the volumes again after creation of volumes
        Step5: Verifying the length of the volumes is (page size + 1)
        Step6: Listing all the volumes in page1
        Step7: Verifying that the length of the volumes in page 1 is
               (page size)
        Step8: Listing all the volumes in page2
        Step9: Verifying that the length of the volumes in page 2 is 1
        Step10: Deleting the volume present in page 2
        Step11: Listing for the volumes on page 2
        Step12: Verifying that there are no volumes present in page 2
        """
        # Listing all the instances for a user
        list_instances_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"])

        # Verifying listed instances for account created at class level
        self.assertIsNone(
            list_instances_before,
            "Virtual Machine already exists for newly created user"
        )
        # If number of instances are less than (pagesize + 1), then creating
        # them
        for i in range(0, (self.services["pagesize"] + 1)):
            vm_created = VirtualMachine.create(
                self.userapiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
            )
            self.assertIsNotNone(
                vm_created,
                "VM creation failed"
            )

            self.assertEqual(
                self.services["virtual_machine"]["displayname"],
                vm_created.displayname,
                "Newly created VM name and the test data VM name\
                        are not matching")

        # Listing all the instances again after creating VM's
        list_instances_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"])
        status = validateList(list_instances_after)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of instances after creation failed"
        )
        # Verifying the length of the instances is (page size + 1)
        self.assertEqual(
            len(list_instances_after),
            (self.services["pagesize"] + 1),
            "Number of instances created is not matching as expected"
        )

        # Listing all the volumes in page1
        list_instances_page1 = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid
        )
        status = validateList(list_instances_page1)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of instances in page1 failed"
        )
        # Verifying that the length of the instances in page 1 is (page size)
        self.assertEqual(
            self.services["pagesize"],
            len(list_instances_page1),
            "List VM response is not matching with the page size\
                    length for page 1")

        # Listing all the VM's in page2
        list_instances_page2 = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=2,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid
        )
        status = validateList(list_instances_page2)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of instances in page2 failed"
        )
        # Verifying that the length of the VM's in page 2 is 1
        self.assertEqual(
            1,
            len(list_instances_page2),
            "List VM response is not matching with the\
                    page size length for page 2"
        )
        instance_page2 = list_instances_page2[0]

        # Verifying that the VM on page 2 is not present in page1
        for i in range(0, len(list_instances_page1)):
            instance_page1 = list_instances_page1[i]
            self.assertNotEqual(
                instance_page2.id,
                instance_page1.id,
                "VM listed in page 2 is also listed in page 1"
            )

        # Deleting a single VM
        VirtualMachine.delete(vm_created, self.apiClient, expunge=True)

        # Listing the VM's in page 2
        list_instance_response = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=2,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid
        )
        # verifying that VM does not exists on page 2
        self.assertEqual(
            list_instance_response,
            None,
            "VM was not deleted"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_02_list_Running_vm(self):
        """
        @Desc: Test List Running VM's
        @Steps:
        Step1: Listing all the Running VMs for a user
        Step2: Verifying that the size of the list is 0
        Step3: Deploying a VM
        Step4: Listing all the Running VMs for a user again
        Step5: Verifying that the size of the list is increased by 1
        Step6: Verifying that the details of the Running VM listed are
               same as the VM deployed in Step3
        """
        # Listing all the Running VM's for a User
        list_running_vms_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
            state="Running"
        )
        self.assertIsNone(
            list_running_vms_before,
            "Virtual Machine already exists for newly created user"
        )
        # Deploying a VM
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "VM creation failed"
        )
        # Listing all the Running VM's for a User
        list_running_vms_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
            state="Running"
        )
        status = validateList(list_running_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "Newly created VM is not in Running state"
        )
        # Verifying list size is 1
        self.assertEqual(
            1,
            len(list_running_vms_after),
            "Running VM list count is not matching"
        )
        running_vm = list_running_vms_after[0]

        # Creating expected and actual values dictionaries
        expected_dict = {
            "id": vm_created.id,
            "name": vm_created.name,
            "displayname": vm_created.displayname,
            "state": "Running",
            "zoneid": vm_created.zoneid,
            "account": vm_created.account,
            "template": vm_created.templateid
        }
        actual_dict = {
            "id": running_vm.id,
            "name": running_vm.name,
            "displayname": running_vm.displayname,
            "state": running_vm.state,
            "zoneid": running_vm.zoneid,
            "account": running_vm.account,
            "template": running_vm.templateid
        }
        running_vm_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            running_vm_status,
            "Listed Running VM details are not as expected"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_03_list_Stopped_vm(self):
        """
        @Desc: Test List Stopped VM's
        @Steps:
        Step1: Listing all the Stopped VMs for a user
        Step2: Verifying that the size of the list is 0
        Step3: Deploying a VM
        Step4: Stopping the VM deployed in step3
        Step5: Listing all the Stopped VMs for a user again
        Step6: Verifying that the size of the list is increased by 1
        Step7: Verifying that the details of the Stopped VM listed are
               same as the VM stopped in Step4
        """
        # Listing all the Stopped VM's for a User
        list_stopped_vms_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
            state="Stopped"
        )
        self.assertIsNone(
            list_stopped_vms_before,
            "Virtual Machine already exists for newly created user"
        )
        # Deploying a VM
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "VM creation failed"
        )
        # Stopping the VM
        VirtualMachine.stop(vm_created, self.userapiclient)
        # Listing all the Stopped VM's for a User
        list_stopped_vms_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
            state="Stopped"
        )
        status = validateList(list_stopped_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "Stopped VM is not in Stopped state"
        )
        # Verifying list size is 1
        self.assertEqual(
            1,
            len(list_stopped_vms_after),
            "Stopped VM list count is not matching"
        )
        stopped_vm = list_stopped_vms_after[0]
        # Creating expected and actual values dictionaries
        expected_dict = {
            "id": vm_created.id,
            "name": vm_created.name,
            "displayname": vm_created.displayname,
            "state": "Stopped",
            "zoneid": vm_created.zoneid,
            "account": vm_created.account,
            "template": vm_created.templateid
        }
        actual_dict = {
            "id": stopped_vm.id,
            "name": stopped_vm.name,
            "displayname": stopped_vm.displayname,
            "state": stopped_vm.state,
            "zoneid": stopped_vm.zoneid,
            "account": stopped_vm.account,
            "template": stopped_vm.templateid
        }
        stopped_vm_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            stopped_vm_status,
            "Listed Stopped VM details are not as expected"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_04_list_Destroyed_vm(self):
        """
        @Desc: Test List Destroyed VM's
        @Steps:
        Step1: Listing all the Destroyed VMs for a user
        Step2: Verifying that the size of the list is 0
        Step3: Deploying a VM
        Step4: Destroyed the VM deployed in step3
        Step5: Listing all the Destroyed VMs for a user again
        Step6: Verifying that destroyed VM is not listed for User
        Step7: Listing all the destroyed VMs as admin
        Step8: Verifying that the size of the list is 1
        Step9: Verifying that the details of the Destroyed VM listed
                are same as the VM destroyed in Step4
        """
        # Listing all the Destroyed VM's for a User
        list_destroyed_vms_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
            state="Destroyed"
        )
        self.assertIsNone(
            list_destroyed_vms_before,
            "Virtual Machine in Destroyed state already exists\
                    for newly created user")
        # Deploying a VM
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "VM creation failed"
        )
        # Destroying the VM
        VirtualMachine.delete(vm_created, self.userapiclient, expunge=False)
        # Listing all the Destroyed VM's for a User
        list_destroyed_vms_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
            state="Destroyed"
        )
        self.assertIsNone(
            list_destroyed_vms_after,
            "Destroyed VM is not in destroyed state"
        )
        # Listing destroyed VMs as admin user
        list_destroyed_vms_admin = VirtualMachine.list(
            self.apiClient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
            state="Destroyed",
            id=vm_created.id
        )
        status = validateList(list_destroyed_vms_admin)
        self.assertEqual(
            PASS,
            status[0],
            "Destroyed VM is not in Destroyed state"
        )
        # Verifying that the length of the destroyed VMs list should be 1
        self.assertEqual(
            1,
            len(list_destroyed_vms_admin),
            "Destroyed VM list count is not matching"
        )
        destroyed_vm = list_destroyed_vms_admin[0]
        # Creating expected and actual values dictionaries
        expected_dict = {
            "id": vm_created.id,
            "name": vm_created.name,
            "displayname": vm_created.displayname,
            "state": "Destroyed",
            "zoneid": vm_created.zoneid,
            "account": vm_created.account,
            "template": vm_created.templateid
        }
        actual_dict = {
            "id": destroyed_vm.id,
            "name": destroyed_vm.name,
            "displayname": destroyed_vm.displayname,
            "state": destroyed_vm.state,
            "zoneid": destroyed_vm.zoneid,
            "account": destroyed_vm.account,
            "template": destroyed_vm.templateid
        }
        destroyed_vm_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            destroyed_vm_status,
            "Listed Destroyed VM details are not as expected"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_05_list_vm_by_id(self):
        """
        @Desc: Test List VM by Id
        @Steps:
        Step1: Listing all the VMs for a user
        Step2: Verifying that the size of the list is 0
        Step3: Deploying a VM
        Step4: Listing all the VMs for a user again
        Step5: Verifying that the size of the list is increased by 1
        Step6: List a VM by specifying the Id if the VM deployed in Step3
        Step7: Verifying that the details of the Listed VM are same as
               the VM deployed in Step3
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
            account=self.account.name
        )
        self.assertIsNone(
            list_vms_before,
            "Virtual Machine already exists for newly created user"
        )
        # Deploying a VM
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "VM creation failed"
        )
        # Listing all the VM's for a User
        list_vms_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
            account=self.account.name
        )
        status = validateList(list_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of VM after creation failed"
        )
        self.assertEqual(
            1,
            len(list_vms_after),
            "VM list count is not matching"
        )
        # Listing a VM by Id
        list_vm_byid = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            id=vm_created.id
        )
        status = validateList(list_vm_byid)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of VM by Id failed"
        )
        listed_vm = list_vm_byid[0]
        # Creating expected and actual values dictionaries
        expected_dict = {
            "id": vm_created.id,
            "name": vm_created.name,
            "displayname": vm_created.displayname,
            "state": vm_created.state,
            "zoneid": vm_created.zoneid,
            "account": vm_created.account,
            "template": vm_created.templateid
        }
        actual_dict = {
            "id": listed_vm.id,
            "name": listed_vm.name,
            "displayname": listed_vm.displayname,
            "state": listed_vm.state,
            "zoneid": listed_vm.zoneid,
            "account": listed_vm.account,
            "template": listed_vm.templateid
        }
        list_vm_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            list_vm_status,
            "Listed VM by Id details are not as expected"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_06_list_vm_by_name(self):
        """
        @Desc: Test List VM's by Name
        @Steps:
        Step1: Listing all the VMs for a user
        Step2: Verifying that the size of the list is 0
        Step3: Deploying a 2 VM's
        Step4: Listing all the VMs for a user again
        Step5: Verifying that list size is increased by 2
        Step6: Listing the VM by specifying complete name of
               VM-1 created in step3
        Step7: Verifying that the size of the list is 1
        Step8: Verifying that the details of the listed VM are same as the
               VM-1 created in step3
        Step9: Listing the VM by specifying the partial name of VM
        Step10: Verifying that the size of the list is 2
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
        )
        self.assertIsNone(
            list_vms_before,
            "Virtual Machine already exists for newly created user"
        )
        vms = {}
        for i in range(0, 2):
            # Deploying a VM
            vm_created = VirtualMachine.create(
                self.userapiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
            )
            self.assertIsNotNone(
                vm_created,
                "VM creation failed"
            )
            vms.update({i: vm_created})

        # Listing all the VM's for a User
        list_vms_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
        )
        status = validateList(list_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "VM's creation failed"
        )
        self.assertEqual(
            2,
            len(list_vms_after),
            "VM's list count is not matching"
        )
        # Listing the VM by complete name
        list_vm_byfullname = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
            name=vms[0].name
        )
        status = validateList(list_vm_byfullname)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to list VM by Name"
        )
        # Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(list_vm_byfullname),
            "VM list by full name count is not matching"
        )
        # Verifying that the details of the listed VM are same
        # as the VM created above
        # Creating expected and actual values dictionaries
        expected_dict = {
            "id": vms[0].id,
            "name": vms[0].name,
            "displayname": vms[0].displayname,
            "state": vms[0].state,
            "zoneid": vms[0].zoneid,
            "account": vms[0].account,
            "template": vms[0].templateid
        }
        actual_dict = {
            "id": list_vm_byfullname[0].id,
            "name": list_vm_byfullname[0].name,
            "displayname": list_vm_byfullname[0].displayname,
            "state": list_vm_byfullname[0].state,
            "zoneid": list_vm_byfullname[0].zoneid,
            "account": list_vm_byfullname[0].account,
            "template": list_vm_byfullname[0].templateid
        }
        list_vm_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            list_vm_status,
            "Listed VM details are not as expected"
        )
        # Listing the VM by partial name
        list_vm_bypartialname = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            domainid=self.account.domainid,
            name=vms[0].name[:1]
        )
        status = validateList(list_vm_bypartialname)
        self.assertEqual(
            PASS,
            status[0],
            "Failed to list VM by Name"
        )
        # Verifying that the size of the list is 2
        self.assertEqual(
            2,
            len(list_vm_bypartialname),
            "VM list by full name count is not matching"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_07_list_vm_by_name_state(self):
        """
        @Desc: Test List VM's by Name and State
        @Steps:
        Step1: Listing all the VMs for a user
        Step2: Verifying that the size of the list is 0
        Step3: Deploying a VM
        Step4: Listing all the VMs for a user again
        Step5: Verifying that list size is increased by 1
        Step6: Listing the VM by specifying name of VM created in step3 and
               state as Running (matching name and state)
        Step7: Verifying that the size of the list is 1
        Step8: Verifying that the details of the listed VM are same as
               the VM created in step3
        Step9: Listing the VM by specifying name of VM created in step3
               and state as Stopped (non matching state)
        Step10: Verifying that the size of the list is 0
        Step11: Listing the VM by specifying non matching name and
                state as Running (non matching name)
        Step12: Verifying that the size of the list is 0
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
        )
        self.assertIsNone(
            list_vms_before,
            "Virtual Machine already exists for newly created user"
        )
        # Deploying a VM
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "VM creation failed"
        )
        # Listing all the VM's for a User
        list_vms_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
        )
        status = validateList(list_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "VM's creation failed"
        )
        self.assertEqual(
            1,
            len(list_vms_after),
            "VM's list count is not matching"
        )
        # Listing the VM by matching Name and State
        list_running_vm = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
            name=vm_created.name,
            state="Running"
        )
        status = validateList(list_running_vm)
        self.assertEqual(
            PASS,
            status[0],
            "List VM by name and state failed"
        )
        # Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(list_running_vm),
            "Count of VM list by name and state is not matching"
        )
        # Verifying that the details of the listed VM are same
        # as the VM created above
        # Creating expected and actual values dictionaries
        expected_dict = {
            "id": vm_created.id,
            "name": vm_created.name,
            "displayname": vm_created.displayname,
            "state": "Running",
            "zoneid": vm_created.zoneid,
            "account": vm_created.account,
            "template": vm_created.templateid
        }
        actual_dict = {
            "id": list_running_vm[0].id,
            "name": list_running_vm[0].name,
            "displayname": list_running_vm[0].displayname,
            "state": list_running_vm[0].state,
            "zoneid": list_running_vm[0].zoneid,
            "account": list_running_vm[0].account,
            "template": list_running_vm[0].templateid
        }
        list_vm_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            list_vm_status,
            "Listed VM details are not as expected"
        )
        # Listing the VM by matching name and non matching state
        list_running_vm = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
            name=vm_created.name,
            state="Stopped"
        )
        self.assertIsNone(
            list_running_vm,
            "Listed VM with non matching state"
        )
        # Listing the VM by non matching name and matching state
        list_running_vm = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
            name="name",
            state="Running"
        )
        self.assertIsNone(
            list_running_vm,
            "Listed VM with non matching name"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_08_list_vm_by_zone(self):
        """
        @Desc: Test List VM by Zone.
        This test case is applicable for a setup having multiple zones.
        @Steps:
        Step1: Listing all the zones
        Step2: Checking if there are multiple zones in the setup.
               Continuing below steps only if there are multiple zones
        Step3: Listing template for zone
        Step4: Listing all the VMs for a user
        Step5: Verifying that the size of the list is 0
        Step6: Deploying a VM
        Step7: Listing all the VMs for a user again for matching zone
        Step8: Verifying that the size of the list is 1
        Step9: Verifying that the details of the Listed VM are same
               as the VM deployed in Step6
        Step10: Listing all the VMs for a user again for non-matching zone
        Step11: Verifying that the size of the list is 0
        """
        # Listing all the zones available
        zones_list = Zone.list(self.apiClient)
        status = validateList(zones_list)
        self.assertEqual(
            PASS,
            status[0],
            "zones not available in the given setup"
        )
        current_zone = self.services["virtual_machine"]["zoneid"]
        current_template = self.services["virtual_machine"]["template"]
        # Checking if there are multiple zones in the setup.
        if not len(zones_list) > 1:
            self.debug("Setup is not having multiple zones")
        else:
            # Getting the template available under the zone
            template = get_template(
                self.apiClient,
                zones_list[0].id,
                self.services["ostype"]
            )
            self.assertIsNotNone(
                template,
                "Template not found for zone"
            )
            self.services["virtual_machine"]["zoneid"] = zones_list[0].id
            self.services["virtual_machine"]["template"] = template.id
            # Listing all the VM's for a User
            list_vms_before = VirtualMachine.list(
                self.userapiclient,
                listall=self.services["listall"],
                page=1,
                pagesize=self.services["pagesize"],
                domainid=self.account.domainid,
                zoneid=zones_list[0].id
            )
            self.assertIsNone(
                list_vms_before,
                "Virtual Machine already exists for newly created user"
            )
            # Deploying a VM
            vm_created = VirtualMachine.create(
                self.userapiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
            )
            self.assertIsNotNone(
                vm_created,
                "VM creation failed"
            )
            # Listing all the VMs for a user again for matching zone
            list_vms_after = VirtualMachine.list(
                self.userapiclient,
                listall=self.services["listall"],
                page=1,
                pagesize=self.services["pagesize"],
                domainid=self.account.domainid,
                zoneid=zones_list[0].id
            )
            status = validateList(list_vms_after)
            self.assertEqual(
                PASS,
                status[0],
                "VM creation failed"
            )
            # Verifying that the size of the list is 1
            self.assertEqual(
                1,
                len(list_vms_after),
                "VM list count is not matching"
            )
            listed_vm = list_vms_after[0]
            # Verifying that the details of the Listed VM are
            # same as the VM deployed above
            # Creating expected and actual values dictionaries
            expected_dict = {
                "id": vm_created.id,
                "name": vm_created.name,
                "displayname": vm_created.displayname,
                "state": vm_created.state,
                "zoneid": vm_created.zoneid,
                "account": vm_created.account,
                "template": vm_created.templateid
            }
            actual_dict = {
                "id": listed_vm.id,
                "name": listed_vm.name,
                "displayname": listed_vm.displayname,
                "state": listed_vm.state,
                "zoneid": listed_vm.zoneid,
                "account": listed_vm.account,
                "template": listed_vm.templateid
            }
            list_vm_status = self.__verify_values(
                expected_dict,
                actual_dict
            )
            self.assertEqual(
                True,
                list_vm_status,
                "Listed VM by Id details are not as expected"
            )
            # Listing all the VMs for a user again for non-matching zone
            list_vms = VirtualMachine.list(
                self.userapiclient,
                listall=self.services["listall"],
                page=1,
                pagesize=self.services["pagesize"],
                domainid=self.account.domainid,
                zoneid=zones_list[1].id
            )
            self.assertIsNone(
                list_vms,
                "VM's listed for non matching zone"
            )
            self.services["virtual_machine"]["zoneid"] = current_zone
            self.services["virtual_machine"]["template"] = current_template
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_09_list_vm_by_zone_name(self):
        """
        @Desc: Test List VM by Zone.
        This test case is applicable for a setup having multiple zones.
        @Steps:
        Step1: Listing all the zones
        Step2: Checking if there are multiple zones in the setup.
               Continuing below steps only if there are multiple zones
        Step3: Listing template for zone
        Step4: Listing all the VMs for a user
        Step5: Verifying that the size of the list is 0
        Step6: Deploying a VM
        Step7: Listing all the VMs for a user again
        Step8: Verifying that list size is increased by 1
        Step9: Listing the VM by specifying name of VM created in step6
               and matching zone (matching name and zone)
        Step10: Verifying that the size of the list is 1
        Step11: Verifying that the details of the listed VM are same
                as the VM created in step3
        Step12: Listing the VM by specifying name of VM created in step6
                and non matching zone (non matching zone)
        Step13: Verifying that the size of the list is 0
        Step14: Listing the VM by specifying non matching name and
                matching zone (non matching name)
        Step15: Verifying that the size of the list is 0
        """
        # Listing all the zones available
        zones_list = Zone.list(self.apiClient)
        status = validateList(zones_list)
        self.assertEqual(
            PASS,
            status[0],
            "zones not available in the given setup"
        )
        current_zone = self.services["virtual_machine"]["zoneid"]
        current_template = self.services["virtual_machine"]["template"]
        # Checking if there are multiple zones in the setup.
        if not len(zones_list) > 1:
            self.debug("Setup is not having multiple Zones")
        else:
            # Getting the template available under the zone
            template = get_template(
                self.apiClient,
                zones_list[0].id,
                self.services["ostype"]
            )
            self.assertIsNotNone(
                template,
                "Template not found for zone"
            )
            self.services["virtual_machine"]["zoneid"] = zones_list[0].id
            self.services["virtual_machine"]["template"] = template.id
            # Listing all the VM's for a User
            list_vms_before = VirtualMachine.list(
                self.userapiclient,
                listall=self.services["listall"],
                page=1,
                pagesize=self.services["pagesize"],
                domainid=self.account.domainid,
                zoneid=zones_list[0].id,
                account=self.account.name
            )
            self.assertIsNone(
                list_vms_before,
                "Virtual Machine already exists for newly created user"
            )
            # Deploying a VM
            vm_created = VirtualMachine.create(
                self.userapiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
            )
            self.assertIsNotNone(
                vm_created,
                "VM creation failed"
            )
            # Listing all the VMs for a user again for matching zone
            list_vms_after = VirtualMachine.list(
                self.userapiclient,
                listall=self.services["listall"],
                page=1,
                pagesize=self.services["pagesize"],
                domainid=self.account.domainid,
                zoneid=zones_list[0].id,
                account=self.account.name
            )
            status = validateList(list_vms_after)
            self.assertEqual(
                PASS,
                status[0],
                "VM creation failed"
            )
            # Verifying that the size of the list is 1
            self.assertEqual(
                1,
                len(list_vms_after),
                "VM list count is not matching"
            )
            # Listing the VM by specifying name of VM created in above and
            # matching zone
            list_vms = VirtualMachine.list(
                self.userapiclient,
                listall=self.services["listall"],
                page=1,
                pagesize=self.services["pagesize"],
                domainid=self.account.domainid,
                zoneid=zones_list[0].id,
                name=vm_created.name
            )
            status = validateList(list_vms)
            self.assertEqual(
                PASS,
                status[0],
                "Listing VM's by name and zone failed"
            )
            # Verifying Verifying that the size of the list is 1
            self.assertEqual(
                1,
                len(list_vms),
                "Count of listed VM's by name and zone is not as expected"
            )
            listed_vm = list_vms[0]
            # Verifying that the details of the Listed VM are same
            # as the VM deployed above
            # Creating expected and actual values dictionaries
            expected_dict = {
                "id": vm_created.id,
                "name": vm_created.name,
                "displayname": vm_created.displayname,
                "state": vm_created.state,
                "zoneid": vm_created.zoneid,
                "account": vm_created.account,
                "template": vm_created.templateid
            }
            actual_dict = {
                "id": listed_vm.id,
                "name": listed_vm.name,
                "displayname": listed_vm.displayname,
                "state": listed_vm.state,
                "zoneid": listed_vm.zoneid,
                "account": listed_vm.account,
                "template": listed_vm.templateid
            }
            list_vm_status = self.__verify_values(
                expected_dict,
                actual_dict
            )
            self.assertEqual(
                True,
                list_vm_status,
                "Listed VM by Id details are not as expected"
            )
            # Listing the VM by specifying name of VM created in step3 and non
            # matching zone
            list_vms = VirtualMachine.list(
                self.userapiclient,
                listall=self.services["listall"],
                page=1,
                pagesize=self.services["pagesize"],
                domainid=self.account.domainid,
                zoneid=zones_list[1].id,
                name=vm_created.name
            )
            self.assertIsNone(
                list_vms,
                "VM's listed for non matching zone"
            )
            # Listing the VM by specifying non matching name of VM and matching
            # zone
            list_vms = VirtualMachine.list(
                self.userapiclient,
                listall=self.services["listall"],
                page=1,
                pagesize=self.services["pagesize"],
                domainid=self.account.domainid,
                zoneid=zones_list[0].id,
                name="name"
            )
            self.assertIsNone(
                list_vms,
                "VM's listed for non matching zone"
            )
            self.services["virtual_machine"]["zoneid"] = current_zone
            self.services["virtual_machine"]["template"] = current_template
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_10_list_vm_by_zone_name_state(self):
        """
        @Desc: Test List VM by Zone.
        @Steps:
        Step1: Listing all the VMs for a user
        Step2: Verifying that the size of the list is 0
        Step3: Deploying a VM
        Step4: Listing all the VMs for a user again
        Step5: Verifying that list size is increased by 1
        Step6: Listing the VM by specifying name of VM created in step3
               and matching zone and state as Running
        Step7: Verifying that the size of the list is 1
        Step8: Verifying that the details of the listed VM are same
               as the VM created in step3
        Step9: Listing the VM by specifying name of VM created in step3 and
               matching zone and state as Stopped
        Step10: Verifying that the size of the list is 0
        Step11: Listing the VM by name, Zone and account
        Step12: Verifying that the size of the list is 1
        Step13: Verifying that the details of the listed VM are same
                as the VM created in step3
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            account=self.account.name
        )
        self.assertIsNone(
            list_vms_before,
            "Virtual Machine already exists for newly created user"
        )
        # Deploying a VM
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "VM creation failed"
        )
        # Listing all the VMs for a user again for matching zone
        list_vms_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            account=self.account.name
        )
        status = validateList(list_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "VM creation failed"
        )
        # Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(list_vms_after),
            "VM list count is not matching"
        )
        # Listing the VM by specifying name of VM created in step3 and matching
        # zone and state as Running
        list_vms = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            name=vm_created.name,
            state="Running"
        )
        status = validateList(list_vms)
        self.assertEqual(
            PASS,
            status[0],
            "Listing VM's by name and zone failed"
        )
        # Verifying Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(list_vms),
            "Count of listed VM's by name, zone and state is not as expected"
        )
        listed_vm = list_vms[0]
        # Verifying that the details of the Listed VM are same
        # as the VM deployed above
        # Creating expected and actual values dictionaries
        expected_dict = {
            "id": vm_created.id,
            "name": vm_created.name,
            "displayname": vm_created.displayname,
            "state": vm_created.state,
            "zoneid": vm_created.zoneid,
            "account": vm_created.account,
            "template": vm_created.templateid
        }
        actual_dict = {
            "id": listed_vm.id,
            "name": listed_vm.name,
            "displayname": listed_vm.displayname,
            "state": listed_vm.state,
            "zoneid": listed_vm.zoneid,
            "account": listed_vm.account,
            "template": listed_vm.templateid
        }
        list_vm_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            list_vm_status,
            "Listed VM by Id details are not as expected"
        )
        # Listing the VM by specifying name of VM created in step3, zone and
        # State as Stopped
        list_vms = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            name=vm_created.name,
            state="Stopped"
        )
        self.assertIsNone(
            list_vms,
            "VM's listed for non matching zone"
        )
        # Listing the VM by name, zone and account
        list_vms = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            name=vm_created.name,
            account=self.account.name
        )
        status = validateList(list_vms)
        self.assertEqual(
            PASS,
            status[0],
            "Listing VM's by name, account and zone failed"
        )
        # Verifying Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(list_vms),
            "Count of listed VM's by name, zone and account is not as expected"
        )
        listed_vm = list_vms[0]
        # Verifying that the details of the Listed VM are same
        # as the VM deployed above
        # Creating expected and actual values dictionaries
        expected_dict = {
            "id": vm_created.id,
            "name": vm_created.name,
            "displayname": vm_created.displayname,
            "state": vm_created.state,
            "zoneid": vm_created.zoneid,
            "account": vm_created.account,
            "template": vm_created.templateid
        }
        actual_dict = {
            "id": listed_vm.id,
            "name": listed_vm.name,
            "displayname": listed_vm.displayname,
            "state": listed_vm.state,
            "zoneid": listed_vm.zoneid,
            "account": listed_vm.account,
            "template": listed_vm.templateid
        }
        list_vm_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            list_vm_status,
            "Listed VM by Id details are not as expected"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_11_register_reset_vm_sshkey(self):
        """
        @Desc: Test to verify registering and reset of SSH Key for VM
        @Steps:
        Step1: Deploying a VM
        Step2: Stopping the VM deployed in step1
        Step3: Listing all the SSH Key pairs
        Step4: Registering a SSH Key pair
        Step5: Listing all the SSh Key pairs again
        Step6: Verifying that the key pairs list is increased by 1
        Step7: Resetting the VM SSH Key to the key pair registered in step4
        Step8: Verifying that the registered SSH Key pair is set to the VM
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        self.assertIsNone(
            list_vms_before,
            "Virtual Machine already exists for newly created user"
        )
        # Deploying a VM
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "VM creation failed"
        )
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        status = validateList(list_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "VM creation failed"
        )
        # Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(list_vms_after),
            "VM list count is not matching"
        )
        # Stopping the VM deployed above
        vm_created.stop(
            self.userapiclient,
            forced=True
        )
        # Listing all the SSH Key pairs
        list_keypairs_before = SSHKeyPair.list(
            self.userapiclient
        )
        list_keypairs_before_size = 0
        if list_keypairs_before is not None:
            list_keypairs_before_size = len(list_keypairs_before)

        # Registering new Key pair
        new_keypair = SSHKeyPair.register(
            self.userapiclient,
            name="keypair1",
            publickey="ssh-rsa: e6:9a:1e:b5:98:75:88:5d:56:bc:92:7b:43:48:05:b2")
        self.assertIsNotNone(
            new_keypair,
            "New Key pair generation failed"
        )
        self.assertEqual(
            "keypair1",
            new_keypair.name,
            "Key Pair not created with given name"
        )
        # Listing all the SSH Key pairs again
        list_keypairs_after = SSHKeyPair.list(
            self.userapiclient
        )
        status = validateList(list_keypairs_after)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of Key pairs failed"
        )
        # Verifying that list size is increased by 1
        self.assertEqual(
            list_keypairs_before_size + 1,
            len(list_keypairs_after),
            "List count is not matching"
        )
        # Resetting the VM SSH key to the Key pair created above
        vm_created.resetSshKey(
            self.userapiclient,
            keypair=new_keypair.name
        )
        # Listing VM details again
        list_vm = VirtualMachine.list(
            self.userapiclient,
            id=vm_created.id
        )
        status = validateList(list_vm)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of VM failed"
        )
        self.assertEqual(
            1,
            len(list_vm),
            "VMs list is not as expected"
        )
        # Verifying that VM's SSH keypair is set to newly created keypair
        self.assertEqual(
            new_keypair.name,
            list_vm[0].keypair,
            "VM is not set to newly created SSH Key pair"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_12_running_vm_change_service(self):
        """
        @Desc: Test to verify change service for Running VM
        @Steps:
        Step1: Deploying a VM
        Step2: Listing all the existing service offerings
        Step3: If there is a matching Service Offering for
                change service of stopped VM
                use that service offering. If not create
                one service offering for change service.
        Step4: Perform change service for the Running VM
        Step5: Verifying that change service is not possible for Running VM
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        self.assertIsNone(
            list_vms_before,
            "Virtual Machine already exists for newly created user"
        )
        # Deploying a VM
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "VM creation failed"
        )
        # Listing details of current Service Offering
        vm_so_list = ServiceOffering.list(
            self.userapiclient,
            id=vm_created.serviceofferingid
        )
        status = validateList(vm_so_list)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of VM Service offering failed"
        )
        current_so = vm_so_list[0]
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        status = validateList(list_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "VM creation failed"
        )
        # Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(list_vms_after),
            "VM list count is not matching"
        )
        # Listing all the service offerings
        service_offerings_list = ServiceOffering.list(
            self.userapiclient,
            virtualmachineid=vm_created.id
        )
        # Verifying if any Service offering available for change service of VM
        so_exists = False
        if service_offerings_list is not None:
            for i in range(0, len(service_offerings_list)):
                if ((current_so.id != service_offerings_list[i].id) and (
                            current_so.storagetype ==
                            service_offerings_list[i].storagetype)):
                    so_exists = True
                    new_so = service_offerings_list[i]
                    break
        # If service offering does not exists, then creating one service
        # offering for scale up
        if not so_exists:
            self.services["service_offerings"]["small"][
                "storagetype"] = current_so.storagetype
            new_so = ServiceOffering.create(
                self.apiClient,
                self.services["service_offerings"]["small"]
            )
            self.cleanup.append(new_so)
        # Changing service for the Running VM
        with self.assertRaises(Exception):
            vm_created.change_service_offering(
                self.userapiclient,
                new_so.id
            )
        # Listing VM details again
        list_vms_after = VirtualMachine.list(
            self.userapiclient,
            id=vm_created.id
        )
        status = validateList(list_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of VM failed"
        )
        self.assertEqual(
            1,
            len(list_vms_after),
            "VMs list is not as expected"
        )
        # Verifying that VM's service offerings is not changed
        self.assertEqual(
            current_so.id,
            list_vms_after[0].serviceofferingid,
            "VM is not containing old Service Offering"
        )
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_13_vm_nics(self):
        """
        @Desc: Test to verify Nics for a VM
        @Steps:
        Step1: Deploying a VM
        Step2: Listing all the Networks
        Step3: Verifying that the list size is 1
        Step4: Creating 1 network
        Step5: Listing all the networks again
        Step6: Verifying that the list size is 2
        Step7: Verifying that VM deployed in step1 has only 1 nic
                and it is same as network listed in step3
        Step8: Adding the networks created in step4 to VM deployed in step1
        Step9: Verifying that VM deployed in step1 has 2 nics
        Step10: Verifying that isdefault is set to true for only 1 nic
        Step11: Verifying that isdefault is set to true for the Network
                created when deployed a VM
        Step12: Making the nic created in step4 as default nic
        Step13: Verifying that isdefault is set to true for only 1 nic
        Step14: Verifying that the isdefault is set to true for the
                nic created in step4
        Step15: Removing the non-default nic from VM
        Step16: Verifying that VM deployed in step1 has only 1 nic
        """
        if self.hypervisor.lower() in ['hyperv']:
            self.skipTest(
                "This feature is not supported on existing hypervisor.\
                        Hence, skipping the test")

        hypervisorIsVmware = False

        if self.hypervisor.lower() == "vmware":
            hypervisorIsVmware = True

        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        self.assertIsNone(
            list_vms_before,
            "Virtual Machine already exists for newly created user"
        )
        # Deploying a VM
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.zone.networktype if hypervisorIsVmware else "default"
        )
        self.assertIsNotNone(
            vm_created,
            "VM creation failed"
        )
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        status = validateList(list_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "VM creation failed"
        )
        # Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(list_vms_after),
            "VM list count is not matching"
        )
        # Listing all the networks before
        list_network_before = Network.list(
            self.userapiclient,
            isdefault="true",
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.domain.id,
            type="Isolated"
        )
        status = validateList(list_network_before)
        self.assertEqual(
            PASS,
            status[0],
            "Default Network not created when deploying a VM"
        )
        # Verifying that only 1 network is created while deploying a VM
        self.assertEqual(
            1,
            len(list_network_before),
            "More than 1 default network exists"
        )
        network1 = list_network_before[0]
        # Listing Network Offerings
        network_offerings_list = NetworkOffering.list(
            self.apiClient,
            forvpc="false",
            guestiptype="Isolated",
            state="Enabled",
            supportedservices="SourceNat",
            zoneid=self.zone.id
        )
        self.assertIsNotNone(
            network_offerings_list,
            "Isolated Network Offerings with sourceNat enabled are not found"
        )
        # Creating one more network
        network2 = Network.create(
            self.userapiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.domain.id,
            networkofferingid=network_offerings_list[0].id,
            zoneid=self.zone.id
        )
        self.assertIsNotNone(
            network2,
            "Network creation failed"
        )
        # Listing all the networks again
        list_network_after = Network.list(
            self.userapiclient,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.domain.id,
            type="Isolated"
        )
        status = validateList(list_network_after)
        self.assertEqual(
            PASS,
            status[0],
            "List of Networks failed"
        )
        # Verifying that list size is 2
        self.assertEqual(
            2,
            len(list_network_after),
            "More than 1 default network exists"
        )
        # Verifying that VM created is having only 1 nic
        vm_nics_before = vm_created.nic
        self.assertIsNotNone(
            vm_nics_before,
            "Nic not found for the VM deployed"
        )
        self.assertEqual(
            1,
            len(vm_nics_before),
            "VM Nic count is not matching"
        )
        # Verifying that the nic is same as the default network listed above
        self.assertEqual(
            network1.id,
            vm_nics_before[0].networkid,
            "Default NIC for VM is not as expected"
        )
        # Adding network2 created above to VM
        VirtualMachine.add_nic(
            vm_created,
            self.userapiclient,
            network2.id
        )
        # Listing the Vm details again
        list_vms_after = VirtualMachine.list(
            self.userapiclient,
            id=vm_created.id
        )
        status = validateList(list_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of VM failed"
        )
        vm = list_vms_after[0]
        # Verifying that VM nics size is 2 now
        vm_nics_after = vm.nic
        self.assertIsNotNone(
            vm_nics_after,
            "Nic not found for the deployed VM"
        )
        self.assertEqual(
            2,
            len(vm_nics_after),
            "VM NIC's count is not matching"
        )
        # Verifying that isdefault is set to true for only 1 nic
        default_count = 0
        for i in range(0, len(vm_nics_after)):
            if vm_nics_after[i].isdefault is True:
                default_count = default_count + 1
                default_nic = vm_nics_after[i]
            else:
                non_default_nic = vm_nics_after[i]
        self.assertEqual(
            1,
            default_count,
            "Default NIC count is not matching"
        )
        # Verifying that default NIC is same the network created when VM is
        # deployed
        self.assertEqual(
            network1.id,
            default_nic.networkid,
            "Default NIC is not matching for VM"
        )
        # Updating network 2 as default NIC
        vm_created.update_default_nic(
            self.userapiclient,
            non_default_nic.id
        )
        # Listing the Vm details again
        list_vms_after = VirtualMachine.list(
            self.userapiclient,
            id=vm_created.id
        )
        status = validateList(list_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of VM failed"
        )
        vm = list_vms_after[0]
        # Verifying that VM nics size is 2 now
        vm_nics_after = vm.nic
        self.assertIsNotNone(
            vm_nics_after,
            "Nic not found for the deployed VM"
        )
        self.assertEqual(
            2,
            len(vm_nics_after),
            "VM NIC's count is not matching"
        )
        # Verifying that isdefault is set to true for only 1 nic
        default_count = 0
        for i in range(0, len(vm_nics_after)):
            if vm_nics_after[i].isdefault is True:
                default_count = default_count + 1
                default_nic = vm_nics_after[i]
            else:
                non_default_nic = vm_nics_after[i]

        self.assertEqual(
            1,
            default_count,
            "Default NIC count is not matching"
        )
        # Verifying that default NIC is same the newly updated network (network
        # 2)
        self.assertEqual(
            network2.id,
            default_nic.networkid,
            "Default NIC is not matching for VM"
        )

        # If hypervisor is Vmware, then check if
        # the vmware tools are installed and the process is running
        # Vmware tools are necessary for remove nic operations (vmware 5.5+)
        isVmwareToolInstalled = False
        if hypervisorIsVmware:
            sshClient = vm_created.get_ssh_client()
            result = str(
                sshClient.execute("service vmware-tools status")).lower()
            self.debug("and result is: %s" % result)
            if "running" in result:
                isVmwareToolInstalled = True

        goForUnplugOperation = True
        # If Vmware tools are not installed in case of vmware hypervisor
        # then don't go further for unplug operation (remove nic) as it won't
        # be supported
        if hypervisorIsVmware and not isVmwareToolInstalled:
            goForUnplugOperation = False

        if goForUnplugOperation:
            # Deleting non default NIC
            vm_created.remove_nic(
                self.userapiclient,
                non_default_nic.id
            )
            # Listing the Vm details again
            list_vms_after = VirtualMachine.list(
                self.userapiclient,
                id=vm_created.id
            )
            status = validateList(list_vms_after)
            self.assertEqual(
                PASS,
                status[0],
                "Listing of VM failed"
            )
            vm = list_vms_after[0]
            # Verifying that VM nics size is 1 now
            vm_nics_after = vm.nic
            self.assertIsNotNone(
                vm_nics_after,
                "Nic not found for the deployed VM"
            )
            self.assertEqual(
                1,
                len(vm_nics_after),
                "VM NIC's count is not matching"
            )
            # Verifying the nic network is same as the default nic network
            self.assertEqual(
                network2.id,
                vm_nics_after[0].networkid,
                "VM NIC is not same as expected"
            )
        return
    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_14_Create_vm_with_same_sshkey(self):
        """
        @Desc: Test to verify API call Register ssh key pair fails when uses same public key for differnet key name
        """


        # Listing all the SSH Key pairs
        list_keypairs_before = SSHKeyPair.list(
            self.userapiclient
        )
        list_keypairs_before_size = 0
        if list_keypairs_before is not None:
            list_keypairs_before_size = len(list_keypairs_before)

        # Registering first Key pair
        new_keypair1 = SSHKeyPair.register(
            self.userapiclient,
            name="keypair1",
            publickey="ssh-rsa: e6:9a:1e:b5:98:75:88:5d:56:bc:92:7b:43:48:05:b2")
        self.assertIsNotNone(
            new_keypair1,
            "New Key pair generation failed"
        )
        self.assertEqual(
            "keypair1",
            new_keypair1.name,
            "Key Pair not created with given name"
        )
        # Listing all the SSH Key pairs again
        list_keypairs_after = SSHKeyPair.list(
            self.userapiclient
        )
        status = validateList(list_keypairs_after)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of Key pairs failed"
        )
        # Verifying that list size is increased by 1
        self.assertEqual(
            list_keypairs_before_size + 1,
            len(list_keypairs_after),
            "List count is not matching"
        )

        # Deploying a VM with keypair 1
        first_vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            keypair=new_keypair1.name
        )
        self.assertIsNotNone(
            first_vm_created,
            "VM creation failed"
        )
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
            self.userapiclient,id=first_vm_created.id,
            listall=True,
        )
        status = validateList(list_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "VM creation failed"
        )
        vm = list_vms_after[0]
        self.assertEqual(
            vm.state,
            "Running",
            "VM state should be running after deployment")
        self.assertEqual(vm.keypair , new_keypair1.name , "VM keypair name is not keypair1")

        try:

            # Registering second key pair using same public key
            new_keypair2 = SSHKeyPair.register(
                self.userapiclient,
                name="keypair2",
                publickey="ssh-rsa: e6:9a:1e:b5:98:75:88:5d:56:bc:92:7b:43:48:05:b2")
            self.fail("SSH Key creation passed using same public key ")
        except CloudstackAPIException as e:
            self.assertRaises("Exception Raised : %s" % e)

        return


class TestInstances(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(TestInstances, cls).getClsTestClient()
            cls.api_client = cls.testClient.getApiClient()
            cls.services = cls.testClient.getParsedTestDataConfig()
            cls.hypervisor = cls.testClient.getHypervisorInfo()
            # Get Domain, Zone, Template
            cls.domain = get_domain(cls.api_client)
            cls.zone = get_zone(
                cls.api_client,
                cls.testClient.getZoneForTests())
            cls.template = get_template(
                cls.api_client,
                cls.zone.id,
                cls.services["ostype"]
            )
            if cls.zone.localstorageenabled:
                cls.storagetype = 'local'
                cls.services["service_offerings"][
                    "tiny"]["storagetype"] = 'local'
                cls.services["disk_offering"]["storagetype"] = 'local'
            else:
                cls.storagetype = 'shared'
                cls.services["service_offerings"][
                    "tiny"]["storagetype"] = 'shared'
                cls.services["disk_offering"]["storagetype"] = 'shared'

            cls.services['mode'] = cls.zone.networktype
            cls.services["virtual_machine"]["hypervisor"] = cls.hypervisor
            cls.services["virtual_machine"]["zoneid"] = cls.zone.id
            cls.services["virtual_machine"]["template"] = cls.template.id
            cls.services["custom_volume"]["zoneid"] = cls.zone.id

            # Creating Disk offering, Service Offering and Account
            cls.disk_offering = DiskOffering.create(
                cls.api_client,
                cls.services["disk_offering"]
            )
            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.services["service_offerings"]["tiny"]
            )
            cls._cleanup.append(cls.service_offering)
            cls._cleanup.append(cls.disk_offering)
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setup : %s" % e)
        return

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()
        self.cleanup = []
        self.account = Account.create(
            self.apiClient,
            self.services["account"],
            domainid=self.domain.id
        )
        # Getting authentication for user in newly created Account
        self.user = self.account.user[0]
        self.userapiclient = self.testClient.getUserApiClient(
            self.user.username,
            self.domain.name)
        # Updating resource Limits
        for i in range(0, 8):
            Resources.updateLimit(
                self.api_client,
                account=self.account.name,
                domainid=self.domain.id,
                max=-1,
                resourcetype=i
            )
        self.cleanup.append(self.account)

    def tearDown(self):
        # Clean up, terminate the created resources
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def __verify_values(self, expected_vals, actual_vals):
        """
        @Desc: Function to verify expected and actual values
        @Steps:
        Step1: Initializing return flag to True
        Step1: Verifying length of expected and actual
               dictionaries is matching.
               If not matching returning false
        Step2: Listing all the keys from expected dictionary
        Step3: Looping through each key from step2 and verifying expected
               and actual dictionaries have same value
               If not making return flag to False
        Step4: returning the return flag after all the values are verified
        """
        return_flag = True

        if len(expected_vals) != len(actual_vals):
            return False

        keys = list(expected_vals.keys())
        for i in range(0, len(expected_vals)):
            exp_val = expected_vals[keys[i]]
            act_val = actual_vals[keys[i]]
            if exp_val == act_val:
                return_flag = return_flag and True
            else:
                return_flag = return_flag and False
                self.debug(
                    "expected Value: %s, is not matching\
                    with actual value: %s" %
                    (exp_val, act_val))
        return return_flag

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_27_VM_restore_ES3467(self):
        """
        @Desc:Test to verify  order of root and data disk remains same on Xenserver after VM reset
        @Steps :
        1.Create VM with data disk from Windows template
        5.check disk sequence on hypervisor: 1st = root disk, 2nd = data disk
        6. Issue "reset VM" command on CCP
        7. check disk sequence on hypervisor remains same and VM starts successfully
        """
        if self.hypervisor.lower() in ['kvm', 'hyperv', 'lxc', 'vmware']:
            self.skipTest(
                "This test not applicable on existing hypervisor. Hence,\
                        skipping the test")
        try:

            template = Template.register(self.apiClient,
                                         self.services["Windows 7 (64-bit)"],
                                         zoneid=self.zone.id,
                                         account=self.account.name,
                                         domainid=self.domain.id)

            self.debug(
                "Registered a template of format: %s with ID: %s" % (
                    self.services["Windows 7 (64-bit)"]["format"],
                    template.id
                ))
            template.download(self.apiClient)
            self.cleanup.append(template)
            # Wait for template status to be changed across
            time.sleep(self.services["sleep"])
            timeout = self.services["timeout"]
            while True:
                list_template_response = Template.list(
                    self.apiClient,
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
                timeout -= 1
            # Verify template response to check whether template added successfully
            status = validateList(list_template_response)
            self.assertEqual(PASS, status[0], "Template download failed")

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
            disk_offering = DiskOffering.create(
                self.api_client,
                self.services["disk_offering"]
            )
            self.cleanup.append(disk_offering)
            # Deploy new virtual machine using template
            virtual_machine = VirtualMachine.create(
                self.apiClient,
                self.services["virtual_machine"],
                templateid=template.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                diskofferingid=disk_offering.id
            )
            self.debug("creating an instance with template ID: %s" % template.id)
            vm_response = VirtualMachine.list(self.apiClient,
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
            self.cleanup.append(virtual_machine)
            list_volume_response = Volume.list(
                self.apiClient,
                virtualmachineid=virtual_machine.id,
                type='ROOT',
                listall=True
            )
            cmd = "xe vbd-list  vm-name-label=" + virtual_machine.instancename + " vdi-name-label=" + \
                  list_volume_response[0].name + " userdevice=0 --minimal"
            hosts = Host.list(self.apiClient, id=virtual_machine.hostid)
            self.assertEqual(
                isinstance(hosts, list),
                True,
                "Check list host returns a valid list")
            host = hosts[0]
            if self.hypervisor.lower() in 'xenserver':
                #
                ssh = SshClient(host.ipaddress, 22, self.services["configurableData"]["host"]["username"],
                                self.services["configurableData"]["host"]["password"])
                result = ssh.execute(cmd)
                res = str(result)
                self.assertNotEqual(res, "", "root disk should have user device=0")

            self.debug("Restoring  the VM: %s" % virtual_machine.id)
            # Restore VM
            virtual_machine.restore(self.apiClient, template.id)
            vm_response = VirtualMachine.list(
                self.apiClient,
                id=virtual_machine.id,
            )
            hosts = Host.list(self.apiClient, id=virtual_machine.hostid)
            self.assertEqual(
                isinstance(hosts, list),
                True,
                "Check list host returns a valid list")
            host = hosts[0]
            if self.hypervisor.lower() in 'xenserver':
                #
                ssh = SshClient(host.ipaddress, 22, self.services["configurableData"]["host"]["username"],
                                self.services["configurableData"]["host"]["password"])
                result = ssh.execute(cmd)
                res = str(result)
                self.assertNotEqual(res, "", "root disk should have user device=0")

            #
            # Verify VM response to check whether VM deployment was successful
            self.assertNotEqual(
                len(vm_response),
                0,
                "Check VMs available in List VMs response"
            )
            self.assertEqual(
                isinstance(vm_response, list),
                True,
                "Check list VM response for valid list"
            )
            vm = vm_response[0]
            self.assertEqual(
                vm.state,
                'Running',
                "Check the state of VM"
            )
        except Exception as e:
            self.fail("Exception occurred: %s" % e)
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_28_VM_restore_ES3467(self):
        """
        @Desc:Test to verify  order of root and data disk remains same on Xenserver after VM reset
        @Steps :
        1.Create VM from Centos template
        3.Add data disk to VM
        5.check disk sequence on hypervisor: 1st = root disk, 2nd = data disk
        6. Issue "reset VM" command on CCP
        7. check disk sequence on hypervisor remains same and VM starts successfully
        """
        try:
            # Deploy new virtual machine using template
            virtual_machine = VirtualMachine.create(
                self.apiClient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id

            )
            self.debug("creating an instance with template ID: %s" % self.template.id)
            vm_response = VirtualMachine.list(self.apiClient,
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
            disk_offering = DiskOffering.create(
                self.api_client,
                self.services["disk_offering"]
            )
            self.cleanup.append(disk_offering)
            volume = Volume.create(
                self.apiClient,
                self.services["volume"],
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid,
                diskofferingid=disk_offering.id
            )
            # Check List Volume response for newly created volume
            list_volume_response = Volume.list(
                self.apiClient,
                id=volume.id
            )
            self.assertNotEqual(
                list_volume_response,
                None,
                "Check if volume exists in ListVolumes"
            )
            # Attach volume to VM
            virtual_machine.attach_volume(
                self.apiClient,
                volume
            )
            # Check volumes attached to same VM
            list_volume_response = Volume.list(
                self.apiClient,
                virtualmachineid=virtual_machine.id,
                type='DATADISK',
                listall=True
            )

            self.assertNotEqual(
                list_volume_response,
                None,
                "Check if volume exists in ListVolumes")
            self.assertEqual(
                isinstance(list_volume_response, list),
                True,
                "Check list volumes response for valid list")
            list_volume_response = Volume.list(
                self.apiClient,
                virtualmachineid=virtual_machine.id,
                type='ROOT',
                listall=True
            )
            cmd = "xe vbd-list  vm-name-label=" + virtual_machine.instancename + " vdi-name-label=" + \
                  list_volume_response[0].name + " userdevice=0 --minimal"
            hosts = Host.list(self.apiClient, id=virtual_machine.hostid)
            self.assertEqual(
                isinstance(hosts, list),
                True,
                "Check list host returns a valid list")
            host = hosts[0]
            if self.hypervisor.lower() in 'xenserver':
                #
                # host.user, host.passwd = get_host_credentials(self, host.ipaddress)
                ssh = SshClient(host.ipaddress, 22, self.services["configurableData"]["host"]["username"],
                                self.services["configurableData"]["host"]["password"])
                result = ssh.execute(cmd)
                res = str(result)
                self.assertNotEqual(res, "", "root disk should have user device=0")

            # Stop VM
            virtual_machine.stop(self.apiClient)

            self.debug("Restoring the VM: %s" % virtual_machine.id)
            # Restore VM
            virtual_machine.restore(self.apiClient, self.template.id)
            vm_response = VirtualMachine.list(
                self.apiClient,
                id=virtual_machine.id,
            )
            # Verify VM response to check whether VM deployment was successful
            self.assertNotEqual(
                len(vm_response),
                0,
                "Check VMs available in List VMs response"
            )
            self.assertEqual(
                isinstance(vm_response, list),
                True,
                "Check list VM response for valid list"
            )
            vm = vm_response[0]
            self.assertEqual(
                vm.state,
                'Stopped',
                "Check the state of VM"
            )
            # Start VM
            virtual_machine.start(self.apiClient)
            if self.hypervisor.lower() in 'xenserver':
                #
                # host.user, host.passwd = get_host_credentials(self, host.ipaddress)
                ssh = SshClient(host.ipaddress, 22, self.services["configurableData"]["host"]["username"],
                                self.services["configurableData"]["host"]["password"])
                result = ssh.execute(cmd)
                res = str(result)
                self.assertNotEqual(res, "", "root disk should have user device=0")

            vm_response = VirtualMachine.list(
                self.apiClient,
                id=virtual_machine.id,
            )
            self.assertEqual(
                isinstance(vm_response, list),
                True,
                "Check list VM response for valid list"
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
                "Check the state of VM"
            )
        except Exception as e:
            self.fail("Exception occurred: %s" % e)
        return


    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_13_attach_detach_iso(self):
        """
        @Desc: Test Attach ISO to VM and Detach ISO from VM.
        @Steps:
        Step1: Listing all the VMs for a user
        Step2: Verifying that the size of the list is 0
        Step3: Deploying a VM
        Step4: Listing all the VMs for a user again
        Step5: Verifying that list size is increased by 1
        Step6: Listing all the ready ISO's
        Step7: If size of the list is >= 1 continuing to next steps
        Step8: Attaching the ISO listed to VM deployed in Step3
        Step9: Verifying that the attached ISO details are associated with VM
        Step10: Detaching the ISO attached in step8
        Step11: Verifying that detached ISO details are not associated with VM
        """
        if self.hypervisor.lower() in ['kvm', 'hyperv', 'lxc']:
            self.skipTest(
                "This feature is not supported on existing hypervisor. Hence,\
                        skipping the test")
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            account=self.account.name
        )
        self.assertIsNone(
            list_vms_before,
            "Virtual Machine already exists for newly created user"
        )
        # Deploying a VM
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "VM creation failed"
        )
        # Listing all the VMs for a user again for matching zone
        list_vms_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
            page=1,
            pagesize=self.services["pagesize"],
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            account=self.account.name
        )
        status = validateList(list_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "VM creation failed"
        )
        # Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(list_vms_after),
            "VM list count is not matching"
        )
        # Listing the ISO's in ready state
        isos_list = Iso.list(
            self.userapiclient,
            isready="true",
            isofilter="executable",
            zoneid=self.zone.id
        )

        self.assertEqual(validateList(isos_list)[0],
                         PASS,
                         "isos list validation failed")

        if self.hypervisor.lower() == "xenserver":
            isoDesc = "xen"
        else:
            isoDesc = "vmware"

        validIsosToAttach = [iso for iso in isos_list
                             if isoDesc in iso.displaytext.lower()]
        if not validIsosToAttach:
            self.skipTest(
                "Valid ISO not present in setup suitable to attach to VM")

        iso_toattach = validIsosToAttach[0]
        # Attaching ISO listed to VM deployed
        VirtualMachine.attach_iso(
            vm_created,
            self.userapiclient,
            iso_toattach
        )
        list_vm = VirtualMachine.list(
            self.userapiclient,
            id=vm_created.id
        )
        status = validateList(list_vm)
        self.assertEqual(
            PASS,
            status[0],
            "VM listing by Id failed"
        )
        # Verifying that attached ISO details are present in VM
        self.assertEqual(
            iso_toattach.name,
            list_vm[0].isoname,
            "Attached ISO name is not matching"
        )
        self.assertEqual(
            iso_toattach.displaytext,
            list_vm[0].isodisplaytext,
            "Attached ISO display is not matching"
        )
        # Detaching ISO from VM
        VirtualMachine.detach_iso(
            vm_created,
            self.userapiclient
        )
        list_vm = VirtualMachine.list(
            self.userapiclient,
            id=vm_created.id
        )
        status = validateList(list_vm)
        self.assertEqual(
            PASS,
            status[0],
            "VM listing by Id failed"
        )
        # Verifying that ISO details are NOT present in VM
        self.assertIsNone(
            list_vm[0].isoname,
            "ISO not detached from VM"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_14_vm_snapshot_pagination(self):
        """
        @Desc: Test VM Snapshots pagination.
        @Steps:
        Step1: Deploying a VM
        Step2: Listing all the Snapshots of the VM deployed in Step 1
        Step3: Verifying that the list size is 0
        Step4: Creating (pagesize + 1) number of Snapshots for the VM
        Step5: Listing all the Snapshots of the VM deployed in Step 1
        Step6: Verifying that the list size is (pagesize + 1)
        Step7: Listing all the VM snapshots in Page 1 with page size
        Step8: Verifying that size of the list is same as page size
        Step9: Listing all the VM snapshots in Page 2 with page size
        Step10: Verifying that size of the list is 1
        Step11: Deleting VM snapshot in page 2
        Step12: Listing all the VM snapshots in Page 2 with page size
        Step13: Verifying that size of the list is 0
        """
        if self.hypervisor.lower() in ['kvm', 'hyperv', 'lxc']:
            self.skipTest(
                "This feature is not supported on existing hypervisor. Hence,\
                        skipping the test")
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        self.assertIsNone(
            list_vms_before,
            "Virtual Machine already exists for newly created user"
        )
        # Deploying a VM
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "VM creation failed"
        )
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        status = validateList(list_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "VM creation failed"
        )
        # Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(list_vms_after),
            "VM list count is not matching"
        )
        # Listing all the VM snapshots for VM deployed above
        list_snapshots_before = VmSnapshot.list(
            self.userapiclient,
            listall=self.services["listall"],
            virtualmachineid=vm_created.id
        )
        # Verifying that the VM snapshot list is None
        self.assertIsNone(
            list_snapshots_before,
            "Snapshots already exists for newly created VM"
        )
        # Creating pagesize + 1 number of VM snapshots
        for i in range(0, (self.services["pagesize"] + 1)):
            snapshot_created = VmSnapshot.create(
                self.userapiclient,
                vm_created.id,
            )
            self.assertIsNotNone(
                snapshot_created,
                "Snapshot creation failed"
            )

        # Listing all the VM snapshots for VM again
        list_snapshots_after = VmSnapshot.list(
            self.userapiclient,
            listall=self.services["listall"],
            virtualmachineid=vm_created.id
        )
        status = validateList(list_snapshots_after)
        self.assertEqual(
            PASS,
            status[0],
            "VM Snapshots creation failed"
        )
        self.assertEqual(
            self.services["pagesize"] + 1,
            len(list_snapshots_after),
            "Count of VM Snapshots is not matching"
        )
        # Listing all the VM snapshots in Page 1 with page size
        list_snapshots_page1 = VmSnapshot.list(
            self.userapiclient,
            listall=self.services["listall"],
            virtualmachineid=vm_created.id,
            page=1,
            pagesize=self.services["pagesize"],
        )
        status = validateList(list_snapshots_page1)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of VM Snapshots failed in page 1"
        )
        # Verifying the list size is equal to pagesize
        self.assertEqual(
            self.services["pagesize"],
            len(list_snapshots_page1),
            "List VM Snapshot count is not matching in page 1"
        )
        # Listing all the VM Snapshots in page 2
        list_snapshots_page2 = VmSnapshot.list(
            self.userapiclient,
            listall=self.services["listall"],
            virtualmachineid=vm_created.id,
            page=2,
            pagesize=self.services["pagesize"],
        )
        status = validateList(list_snapshots_page2)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of VM Snapshots failed in page 2"
        )
        # Verifying the list size is equal to 1
        self.assertEqual(
            1,
            len(list_snapshots_page2),
            "List VM Snapshot count is not matching in page 2"
        )
        # Deleting VM Snapshot in page 2
        VmSnapshot.deleteVMSnapshot(
            self.userapiclient,
            snapshot_created.id
        )
        # Listing all the VM Snapshots in page 2 again
        list_snapshots_page2 = VmSnapshot.list(
            self.userapiclient,
            listall=self.services["listall"],
            virtualmachineid=vm_created.id,
            page=2,
            pagesize=self.services["pagesize"],
        )
        # Verifying the list size is equal to 0
        self.assertIsNone(
            list_snapshots_page2,
            "VM Snapshots exists in page 2"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_15_revert_vm_to_snapshot(self):
        """
        @Desc: Test Revert VM to Snapshot functionality.
        @Steps:
        Step1: Deploying a VM
        Step2: Listing all the Snapshots of the VM deployed in Step 1
        Step3: Verifying that the list size is 0
        Step4: Creating 2 Snapshots for the VM
        Step5: Listing all the Snapshots of the VM deployed in Step 1
        Step6: Verifying that the list size is 2
        Step7: Verifying that only 1 snapshot is have current flag set to True
        Step8: Verifying that the VM snapshot with current flag set as true is
               the latest snapshot created
        Step9: Reverting VM to snapshot having current flag as false
               (non current snapshot)
        Step10: Verifying that only 1 VM snapshot is having current
                flag set as true.
        Step11: Verifying that the VM Snapshot with current flag set to true
                is the reverted snapshot in Step 8
        """
        if self.hypervisor.lower() in ['kvm', 'hyperv', 'lxc']:
            self.skipTest(
                "This feature is not supported on existing hypervisor.\
                        Hence, skipping the test")
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        self.assertIsNone(
            list_vms_before,
            "Virtual Machine already exists for newly created user"
        )
        # Deploying a VM
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "VM creation failed"
        )
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        status = validateList(list_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "VM creation failed"
        )
        # Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(list_vms_after),
            "VM list count is not matching"
        )
        # Listing all the VM snapshots for VM deployed above
        list_snapshots_before = VmSnapshot.list(
            self.userapiclient,
            listall=self.services["listall"],
            virtualmachineid=vm_created.id
        )
        # Verifying that the VM snapshot list is None
        self.assertIsNone(
            list_snapshots_before,
            "Snapshots already exists for newly created VM"
        )
        # Creating 2 of VM snapshots
        snapshot1 = VmSnapshot.create(
            self.userapiclient,
            vm_created.id,
            snapshotmemory=True
        )
        self.assertIsNotNone(
            snapshot1,
            "Snapshot creation failed"
        )
        snapshot2 = VmSnapshot.create(
            self.userapiclient,
            vm_created.id,
            snapshotmemory=True
        )
        self.assertIsNotNone(
            snapshot2,
            "Snapshot creation failed"
        )
        # Listing all the VM snapshots for VM again
        list_snapshots_after = VmSnapshot.list(
            self.userapiclient,
            listall=self.services["listall"],
            virtualmachineid=vm_created.id
        )
        status = validateList(list_snapshots_after)
        self.assertEqual(
            PASS,
            status[0],
            "VM Snapshots creation failed"
        )
        self.assertEqual(
            2,
            len(list_snapshots_after),
            "Count of VM Snapshots is not matching"
        )
        # Verifying that only 1 snapshot is having current flag set to true
        # and that snapshot is the latest snapshot created (snapshot2)
        current_count = 0
        for i in range(0, len(list_snapshots_after)):
            if (list_snapshots_after[i].current is True):
                current_count = current_count + 1
                current_snapshot = list_snapshots_after[i]

        self.assertEqual(
            1,
            current_count,
            "count of VM Snapshot with current flag as true is not matching"
        )
        self.assertEqual(
            snapshot2.id,
            current_snapshot.id,
            "Latest snapshot taken is not marked as current"
        )

        # Reverting the VM to Snapshot 1
        VmSnapshot.revertToSnapshot(
            self.userapiclient,
            snapshot1.id
        )
        # Listing the VM snapshots again
        list_snapshots_after = VmSnapshot.list(
            self.userapiclient,
            listall=self.services["listall"],
            virtualmachineid=vm_created.id
        )
        status = validateList(list_snapshots_after)
        self.assertEqual(
            PASS,
            status[0],
            "VM Snapshots creation failed"
        )
        self.assertEqual(
            2,
            len(list_snapshots_after),
            "Count of VM Snapshots is not matching"
        )
        # Verifying that only 1 snapshot is having current flag set to true
        # and that snapshot is snapshot1
        current_count = 0
        for i in range(0, len(list_snapshots_after)):
            if (list_snapshots_after[i].current is True):
                current_count = current_count + 1
                current_snapshot = list_snapshots_after[i]
        self.assertEqual(
            1,
            current_count,
            "count of VM Snapshot with current flag as true is not matching"
        )
        self.assertEqual(
            snapshot1.id,
            current_snapshot.id,
            "Current flag was set properly after reverting the VM to snapshot"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_16_list_vm_volumes_pagination(self):
        """
        @Desc: Test to verify pagination of Volumes for a VM
        @Steps:
        Step1: Deploying a VM
        Step2: Listing all the Volumes of the VM deployed in Step 1
        Step3: Verifying that the list size is 1
        Step4: Creating page size number of volumes
        Step5: Attaching all the volumes created in step4 to
               VM deployed in Step1
        Step6: Listing all the Volumes for the VM in step1
        Step7: Verifying that the list size is equal to page size + 1
        Step8: Listing all the volumes of VM in page 1
        Step9: Verifying that the list size is equal to page size
        Step10: Listing all the Volumes in Page 2
        Step11: Verifying that the list size is 1
        Step12: Detaching the volume from the VM
        Step13: Listing all the Volumes in Page 2
        Step14: Verifying that list size is 0
        """
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() == 'lxc':
            if not find_storage_pool_type(self.api_client, storagetype='rbd'):
                self.skipTest("RBD storage type is required for data volumes for LXC")
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        self.assertIsNone(
            list_vms_before,
            "Virtual Machine already exists for newly created user"
        )
        # Deploying a VM
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "VM creation failed"
        )
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        status = validateList(list_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "VM creation failed"
        )
        # Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(list_vms_after),
            "VM list count is not matching"
        )
        # Listing all the Volumes for the VM deployed
        list_volumes_before = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            virtualmachineid=vm_created.id
        )
        status = validateList(list_volumes_before)
        self.assertEqual(
            PASS,
            status[0],
            "Root volume is not created for VM deployed"
        )
        # Verifying the size of the list is 1
        self.assertEqual(
            1,
            len(list_volumes_before),
            "Volumes count is not matching"
        )
        # Creating Page size number of volumes
        for i in range(0, self.services["pagesize"]):
            volume_created = Volume.create(
                self.userapiclient,
                self.services["volume"],
                zoneid=self.zone.id,
                diskofferingid=self.disk_offering.id
            )
            self.assertIsNotNone(
                volume_created,
                "Volume is not created"
            )
            # Attaching all the volumes created to VM
            vm_created.attach_volume(
                self.userapiclient,
                volume_created
            )

        # List all the volumes for the VM again
        list_volumes_after = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            virtualmachineid=vm_created.id
        )
        status = validateList(list_volumes_after)
        self.assertEqual(
            PASS,
            status[0],
            "Volumes are not listed"
        )
        # Verifying that size of the list is equal to page size + 1
        self.assertEqual(
            self.services["pagesize"] + 1,
            len(list_volumes_after),
            "VM's volume count is not matching"
        )
        # Listing all the volumes for a VM in page 1
        list_volumes_page1 = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            virtualmachineid=vm_created.id,
            page=1,
            pagesize=self.services["pagesize"]
        )
        status = validateList(list_volumes_page1)
        self.assertEqual(
            PASS,
            status[0],
            "Volumes not listed in page1"
        )
        # Verifying that list size is equal to page size
        self.assertEqual(
            self.services["pagesize"],
            len(list_volumes_page1),
            "VM's volume count is not matching in page 1"
        )
        # Listing all the volumes for a VM in page 2
        list_volumes_page2 = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            virtualmachineid=vm_created.id,
            page=2,
            pagesize=self.services["pagesize"]
        )
        status = validateList(list_volumes_page2)
        self.assertEqual(
            PASS,
            status[0],
            "Volumes not listed in page2"
        )
        # Verifying that list size is equal to 1
        self.assertEqual(
            1,
            len(list_volumes_page2),
            "VM's volume count is not matching in page 1"
        )
        # Detaching 1 volume from VM
        vm_created.detach_volume(
            self.userapiclient,
            volume_created
        )
        # Listing all the volumes for a VM in page 2 again
        list_volumes_page2 = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            virtualmachineid=vm_created.id,
            page=2,
            pagesize=self.services["pagesize"]
        )
        # Verifying that there are no volumes present in page 2
        self.assertIsNone(
            list_volumes_page2,
            "Volumes listed in page 2"
        )
        # Listing all the volumes for a VM again in page 1
        list_volumes_page1 = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            virtualmachineid=vm_created.id,
            page=1,
            pagesize=self.services["pagesize"]
        )
        status = validateList(list_volumes_page1)
        self.assertEqual(
            PASS,
            status[0],
            "Volumes not listed in page1"
        )
        # Verifying that list size is equal to page size
        self.assertEqual(
            self.services["pagesize"],
            len(list_volumes_page1),
            "VM's volume count is not matching in page 1"
        )

        list_data_disks = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            virtualmachineid=vm_created.id,
            page=1,
            pagesize=self.services["pagesize"],
            type="DATADISK"
        )

        for volume in list_data_disks:
            vm_created.detach_volume(self.userapiclient, volume)

        volumes = Volume.list(
            self.userapiclient,
            listall=self.services["listall"],
            virtualmachineid=vm_created.id,
            page=1,
            pagesize=self.services["pagesize"]
        )

        self.assertEqual(
            len(volumes),
            len(list_volumes_page1) - len(list_data_disks),
            "The volumes number should match with (volumes initially\
                        present minus volumes detached")

        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_17_running_vm_scaleup(self):
        """
        @Desc: Test to verify change service for Running VM
        @Steps:
        Step1: Checking if dynamic scaling of virtual machines is
               enabled in zone and template.
               If yes then continuing.
               If not then printing message that scale up is
               not possible for Running VM
        Step2: Deploying a VM
        Step3: Listing all the existing service offerings
        Step4: If there is a matching Service Offering for scale-up of
               running VM
               use that service offering. If not create one
               service offering for scale up.
        Step5: Perform change service (scale up) the Running VM
               deployed in step1
        Step6: Verifying that VM's service offerings is changed
        """
        if self.hypervisor.lower() == 'kvm':
            self.skipTest(
                "ScaleVM is not supported on KVM. Hence, skipping the test")
        # Checking if Dynamic scaling of VM is supported or not
        list_config = Configurations.list(
            self.apiClient,
            zoneid=self.zone.id,
            name="enable.dynamic.scale.vm"
        )
        status = validateList(list_config)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of configuration failed"
        )
        # Checking if dynamic scaling is allowed in Zone and Template
        if not (
                    (list_config[0].value is True) and (
                        self.template.isdynamicallyscalable)):
            self.debug(
                "Scale up of Running VM is not possible as Zone/Template\
                        does not support")
        else:
            # Listing all the VM's for a User
            list_vms_before = VirtualMachine.list(
                self.userapiclient,
                listall=self.services["listall"],
            )
            self.assertIsNone(
                list_vms_before,
                "Virtual Machine already exists for newly created user"
            )
            # Deploying a VM
            vm_created = VirtualMachine.create(
                self.userapiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
            )
            self.assertIsNotNone(
                vm_created,
                "VM creation failed"
            )
            # Listing details of current Service Offering
            vm_so_list = ServiceOffering.list(
                self.userapiclient,
                id=vm_created.serviceofferingid
            )
            status = validateList(vm_so_list)
            self.assertEqual(
                PASS,
                status[0],
                "Listing of VM Service offering failed"
            )
            current_so = vm_so_list[0]
            # Listing all the VMs for a user again
            list_vms_after = VirtualMachine.list(
                self.userapiclient,
                listall=self.services["listall"],
            )
            status = validateList(list_vms_after)
            self.assertEqual(
                PASS,
                status[0],
                "VM creation failed"
            )
            # Verifying that the size of the list is 1
            self.assertEqual(
                1,
                len(list_vms_after),
                "VM list count is not matching"
            )
            # Listing all the existing service offerings
            service_offerings_list = ServiceOffering.list(
                self.userapiclient,
                virtualmachineid=vm_created.id
            )
            # Verifying if any Service offering available for scale up of VM
            so_exists = False
            if service_offerings_list is not None:
                for i in range(0, len(service_offerings_list)):
                    if not ((current_so.cpunumber >
                                 service_offerings_list[i].cpunumber or
                                     current_so.cpuspeed >
                                     service_offerings_list[i].cpuspeed or
                                     current_so.memory >
                                     service_offerings_list[i].memory) or
                                (current_so.cpunumber ==
                                     service_offerings_list[i].cpunumber and
                                         current_so.cpuspeed ==
                                         service_offerings_list[i].cpuspeed and
                                         current_so.memory ==
                                         service_offerings_list[i].memory)):
                        if (current_so.storagetype ==
                                service_offerings_list[i].storagetype):
                            so_exists = True
                            new_so = service_offerings_list[i]
                            break
            # If service offering does not exists, then creating one service
            # offering for scale up
            if not so_exists:
                self.services["service_offerings"]["small"][
                    "storagetype"] = current_so.storagetype
                new_so = ServiceOffering.create(
                    self.apiClient,
                    self.services["service_offerings"]["small"]
                )
                self.cleanup.append(new_so)
            # Scaling up the VM
            vm_created.scale_virtualmachine(
                self.userapiclient,
                new_so.id
            )
            # Listing VM details again
            list_vms_after = VirtualMachine.list(
                self.userapiclient,
                id=vm_created.id
            )
            status = validateList(list_vms_after)
            self.assertEqual(
                PASS,
                status[0],
                "Listing of VM failed"
            )
            self.assertEqual(
                1,
                len(list_vms_after),
                "VMs list is not as expected"
            )
            # Verifying that VM's service offerings is changed
            self.assertEqual(
                new_so.id,
                list_vms_after[0].serviceofferingid,
                "VM is not containing New Service Offering"
            )
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_18_stopped_vm_change_service(self):
        """
        @Desc: Test to verify change service for Stopped VM
        @Steps:
        Step1: Deploying a VM
        Step2: Stopping the VM deployed in step1
        Step3: Listing all the existing service offerings
        Step4: If there is a matching Service Offering for change service
               of stopped VM
               use that service offering. If not create one service
               offering for change service.
        Step5: Perform change service for the Stopped VM
        Step6: Verifying that VM's service offerings is changed
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        self.assertIsNone(
            list_vms_before,
            "Virtual Machine already exists for newly created user"
        )
        # Deploying a VM
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "VM creation failed"
        )
        # Listing details of current Service Offering
        vm_so_list = ServiceOffering.list(
            self.userapiclient,
            id=vm_created.serviceofferingid
        )
        status = validateList(vm_so_list)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of VM Service offering failed"
        )
        current_so = vm_so_list[0]
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        status = validateList(list_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "VM creation failed"
        )
        # Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(list_vms_after),
            "VM list count is not matching"
        )
        # Stopping the VM deployed above
        vm_created.stop(
            self.userapiclient,
            forced=True
        )
        # Listing all the service offerings
        service_offerings_list = ServiceOffering.list(
            self.userapiclient,
            virtualmachineid=vm_created.id
        )
        # Verifying if any Service offering available for change service of VM
        so_exists = False
        if service_offerings_list is not None:
            for i in range(0, len(service_offerings_list)):
                if ((current_so.id != service_offerings_list[i].id) and (
                            current_so.storagetype ==
                            service_offerings_list[i].storagetype)):
                    so_exists = True
                    new_so = service_offerings_list[i]
                    break
        # If service offering does not exists, then creating one service
        # offering for scale up
        if not so_exists:
            self.services["service_offerings"]["small"][
                "storagetype"] = current_so.storagetype
            new_so = ServiceOffering.create(
                self.apiClient,
                self.services["service_offerings"]["small"]
            )
            self.cleanup.append(new_so)
        # Changing service for the VM
        vm_created.scale_virtualmachine(
            self.userapiclient,
            new_so.id
        )
        # Listing VM details again
        list_vm = VirtualMachine.list(
            self.userapiclient,
            id=vm_created.id
        )
        status = validateList(list_vm)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of VM failed"
        )
        self.assertEqual(
            1,
            len(list_vm),
            "VMs list is not as expected"
        )
        # Verifying that VM's service offerings is changed
        self.assertEqual(
            new_so.id,
            list_vm[0].serviceofferingid,
            "VM is not containing New Service Offering"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_19_create_reset_vm_sshkey(self):
        """
        @Desc: Test to verify creation and reset of SSH Key for VM
        @Steps:
        Step1: Deploying a VM
        Step2: Stopping the VM deployed in step1
        Step3: Listing all the SSH Key pairs
        Step4: Creating a new SSH Key pair
        Step5: Listing all the SSh Key pairs again
        Step6: Verifying that the key pairs list is increased by 1
        Step7: Resetting the VM SSH Key to the key pair created in step4
        Step8: Verifying that the new SSH Key pair is set to the VM
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        self.assertIsNone(
            list_vms_before,
            "Virtual Machine already exists for newly created user"
        )
        # Deploying a VM
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "VM creation failed"
        )
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        status = validateList(list_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "VM creation failed"
        )
        # Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(list_vms_after),
            "VM list count is not matching"
        )
        # Stopping the VM deployed above
        vm_created.stop(
            self.userapiclient,
            forced=True
        )
        # Listing all the SSH Key pairs
        list_keypairs_before = SSHKeyPair.list(
            self.userapiclient
        )
        list_keypairs_before_size = 0
        if list_keypairs_before is not None:
            list_keypairs_before_size = len(list_keypairs_before)

        # Creating a new Key pair
        new_keypair = SSHKeyPair.create(
            self.userapiclient,
            name="keypair1",
            account=self.account.name,
            domainid=self.domain.id
        )
        self.assertIsNotNone(
            new_keypair,
            "New Key pair generation failed"
        )
        self.assertEqual(
            "keypair1",
            new_keypair.name,
            "Key Pair not created with given name"
        )
        # Listing all the SSH Key pairs again
        list_keypairs_after = SSHKeyPair.list(
            self.userapiclient
        )
        status = validateList(list_keypairs_after)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of Key pairs failed"
        )
        # Verifying that list size is increased by 1
        self.assertEqual(
            list_keypairs_before_size + 1,
            len(list_keypairs_after),
            "List count is not matching"
        )
        # Resetting the VM SSH key to the Key pair created above
        vm_created.resetSshKey(
            self.userapiclient,
            keypair=new_keypair.name
        )
        # Listing VM details again
        list_vm = VirtualMachine.list(
            self.userapiclient,
            id=vm_created.id
        )
        status = validateList(list_vm)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of VM failed"
        )
        self.assertEqual(
            1,
            len(list_vm),
            "VMs list is not as expected"
        )
        # Verifying that VM's SSH keypair is set to newly created keypair
        self.assertEqual(
            new_keypair.name,
            list_vm[0].keypair,
            "VM is not set to newly created SSH Key pair"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_20_update_vm_displayname_group(self):
        """
        @Desc: Test to verify Update VM details
        @Steps:
        Step1: List all the VM's for a user
        Step2: Deploy a VM with all parameters
        Step3: Listing all the VM's again for the user
        Step4: Verifying that list size is increased by 1
        Step5: Updating VM details - displayname, group
        Step6: Listing the VM deployed in step 2 by ID
        Step7: Verifying that displayname, group details of the VM are updated
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        self.assertIsNone(
            list_vms_before,
            "Virtual Machine already exists for newly created user"
        )
        self.services["virtual_machine"]["keyboard"] = "us"
        # Deploying a VM
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            group="groupName"
        )
        self.assertIsNotNone(
            vm_created,
            "VM creation failed"
        )
        # Verifying the displayname and group details for deployed VM
        self.assertEqual(
            self.services["virtual_machine"]["displayname"],
            vm_created.displayname,
            "Display name of VM is not as expected"
        )
        self.assertEqual(
            "groupName",
            vm_created.group,
            "Group of VM is not as expected"
        )
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        status = validateList(list_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "VM creation failed"
        )
        # Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(list_vms_after),
            "VM list count is not matching"
        )
        # Updating the VM details - displayname and group
        vm_created.update(
            self.userapiclient,
            displayname="DisplayName",
            group="Group",
            haenable=False
        )
        # Listing VM details again
        list_vm = VirtualMachine.list(
            self.userapiclient,
            id=vm_created.id,
        )
        status = validateList(list_vm)
        self.assertEqual(
            PASS,
            status[0],
            "Listing of VM by Id failed"
        )
        self.assertEqual(
            1,
            len(list_vm),
            "Count of List VM by Id is not matching"
        )
        # Verifying that displayname and group details are updated
        self.assertEqual(
            "DisplayName",
            list_vm[0].displayname,
            "Displayname of VM is not updated"
        )
        self.assertEqual(
            "Group",
            list_vm[0].group,
            "Group of VM is not updated"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_21_restore_vm(self):
        """
        @Desc: Test to verify Restore VM
        @Steps:
        Step1: List all the VM's for a user
        Step2: Deploy a VM with all parameters
        Step3: Listing all the VM's again for the user
        Step4: Verifying that list size is increased by 1
        Step5: Restoring the VM deployed in step2
        Step6: Verifying that restored VM details are same as
               the VM deployed in step2
        """
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        self.assertIsNone(
            list_vms_before,
            "Virtual Machine already exists for newly created user"
        )
        # Deploying a VM
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "VM creation failed"
        )
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        status = validateList(list_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "VM creation failed"
        )
        # Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(list_vms_after),
            "VM list count is not matching"
        )
        # Restoring the VM
        restored_vm = vm_created.restore(self.userapiclient)
        self.assertIsNotNone(
            restored_vm,
            "VM restore failed"
        )
        # Verifying the restored VM details
        expected_dict = {
            "id": vm_created.id,
            "name": vm_created.name,
            "displayname": vm_created.displayname,
            "state": vm_created.state,
            "zoneid": vm_created.zoneid,
            "account": vm_created.account,
            "template": vm_created.templateid
        }
        actual_dict = {
            "id": restored_vm.id,
            "name": restored_vm.name,
            "displayname": restored_vm.displayname,
            "state": restored_vm.state,
            "zoneid": restored_vm.zoneid,
            "account": restored_vm.account,
            "template": restored_vm.templateid
        }
        restored_vm_status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            restored_vm_status,
            "Restored VM details are not as expected"
        )
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_22_deploy_vm_multiple_networks(self):
        """
        @Desc: Test to verify deploy VM with multiple networks
        @Steps:
        Step1: List all the networks for user
        Step2: If size of list networks is greater than 2 then
               get all the networks id's
                Else create 2 networks and get network id's
        Step3: List all the VM's for a user
        Step4: Deploy a VM with multiple network id's
        Step5: Listing all the VM's again for the user
        Step6: Verifying that list size is increased by 1
        Step7: Verify that VM is associated with multiple networks
        """
        # Listing all the networks available
        networks_list_before = Network.list(
            self.userapiclient,
            listall=self.services["listall"],
            type="Isolated"
        )
        networks_list_size = 0
        if networks_list_before is not None:
            networks_list_size = len(networks_list_before)

        # Listing Network Offerings
        network_offerings_list = NetworkOffering.list(
            self.apiClient,
            forvpc="false",
            guestiptype="Isolated",
            state="Enabled",
            supportedservices="SourceNat",
            zoneid=self.zone.id
        )
        status = validateList(network_offerings_list)
        self.assertEqual(
            PASS,
            status[0],
            "Isolated Network Offerings with sourceNat enabled are not found"
        )
        while networks_list_size < 2:
            # Creating a network
            network = Network.create(
                self.userapiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.domain.id,
                networkofferingid=network_offerings_list[0].id,
                zoneid=self.zone.id
            )
            self.assertIsNotNone(
                network,
                "Network creation failed"
            )
            networks_list_size = networks_list_size + 1

        # Listing the networks again
        networks_list_after = Network.list(
            self.userapiclient,
            listall=self.services["listall"],
            type="Isolated"
        )
        status = validateList(network_offerings_list)
        self.assertEqual(
            PASS,
            status[0],
            "Listing networks failed"
        )
        # populating network id's
        networkids = networks_list_after[
                         0].id + "," + networks_list_after[1].id
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        self.assertIsNone(
            list_vms_before,
            "Virtual Machine already exists for newly created user"
        )
        # Deploying a VM
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=networkids,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "VM creation failed"
        )
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        status = validateList(list_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "VM creation failed"
        )
        # Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(list_vms_after),
            "VM list count is not matching"
        )
        # Verifying that the NIC's in VM created are same as provided
        vm_nics = vm_created.nic
        # Verifying that the size of nics is 2
        self.assertEqual(
            2,
            len(vm_nics),
            "NIC's count in VM created is not matching"
        )
        # Verifying that NIC network ID's are as expected
        for i in range(0, len(vm_nics)):
            if vm_nics[i].isdefault is True:
                self.assertEqual(
                    networks_list_after[0].id,
                    vm_nics[i].networkid,
                    "Default NIC is not as expected"
                )
            else:
                self.assertEqual(
                    networks_list_after[1].id,
                    vm_nics[i].networkid,
                    "Non Default NIC is not as expected"
                )
        return

    @attr(tags=["basic"], required_hardware="true")
    def test_23_deploy_vm_multiple_securitygroups(self):
        """
        @Desc: Test to verify deploy VM with multiple Security Groups
        @Steps:
        Step1: List all the security groups for user
        Step2: If size of list security groups is greater than 2 then get
               all the security groups id's
               Else creating 2 security groups and get security groups id's
        Step3: List all the VM's for a user
        Step4: Deploy a VM with multiple security groups id's
        Step5: Listing all the VM's again for the user
        Step6: Verifying that list size is increased by 1
        Step7: Verify that VM is associated with multiple security groups
        """
        # Listing all the security groups available
        security_groups_list = SecurityGroup.list(
            self.userapiclient,
            listall=self.services["listall"],
            domainid=self.domain.id
        )
        security_groups_list_size = 0
        if security_groups_list is not None:
            security_groups_list_size = len(security_groups_list)

        while security_groups_list_size < 2:
            # Creating a security group
            security_group = SecurityGroup.create(
                self.userapiclient,
                self.services["security_group"],
                account=self.account.name,
                domainid=self.domain.id
            )
            self.assertIsNotNone(
                security_group,
                "Security Group creation failed"
            )
            security_groups_list_size = security_groups_list_size + 1

        # Listing the networks again
        security_groups_list = SecurityGroup.list(
            self.userapiclient,
            listall=self.services["listall"],
            domainid=self.domain.id
        )
        status = validateList(security_groups_list)
        self.assertEqual(
            PASS,
            status[0],
            "Listing Security Groups failed"
        )
        # populating Security Groups id's
        securitygroupids = {
            security_groups_list[0].id,
            security_groups_list[1].id}
        # Listing all the VM's for a User
        list_vms_before = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        self.assertIsNone(
            list_vms_before,
            "Virtual Machine already exists for newly created user"
        )
        # Deploying a VM
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            securitygroupids=securitygroupids,
        )
        self.assertIsNotNone(
            vm_created,
            "VM creation failed"
        )
        # Listing all the VMs for a user again
        list_vms_after = VirtualMachine.list(
            self.userapiclient,
            listall=self.services["listall"],
        )
        status = validateList(list_vms_after)
        self.assertEqual(
            PASS,
            status[0],
            "VM creation failed"
        )
        # Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(list_vms_after),
            "VM list count is not matching"
        )
        # Verifying that the Security Groups's in VM created are same as
        # provided
        vm_securitygroups = vm_created.securitygroup
        # Verifying that the size of security groups is 2
        self.assertEqual(
            2,
            len(vm_securitygroups),
            "Security Groups count in VM created is not matching"
        )
        # Verifying that Security Group network ID's are as expected
        vm_securitygroups_flag = True
        for i in range(0, len(vm_securitygroups)):
            if ((vm_securitygroups[i].id != security_groups_list[0].id) and
                    (vm_securitygroups[i].id != security_groups_list[1].id)):
                vm_securitygroups_flag = False
                break

        self.assertEqual(
            True,
            vm_securitygroups_flag,
            "Security Groups in VM are not same as created"
        )
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_24_deploy_vm_with_static_ip_ES1662(self):
        """
        @Desc: Test to verify deploy VM with static ip address assignment
        @Steps:
        Step1: Create a network for the user
        Step2: List the network and check that it is created for the user
        Step3: Deploy vm with ip address in the above network
        Step4: List the vm and verify the ip address in the response
        """
        # Listing Network Offerings
        network_offerings_list = NetworkOffering.list(
            self.apiClient,
            forvpc="false",
            guestiptype="Isolated",
            state="Enabled",
            supportedservices="SourceNat",
            zoneid=self.zone.id
        )
        status = validateList(network_offerings_list)
        self.assertEqual(
            PASS,
            status[0],
            "Isolated Network Offerings with sourceNat enabled are not found"
        )
        """
        Create Isolated netwrok with ip range
        """
        self.services["network"]["startip"] = "10.1.1.2"
        self.services["network"]["endip"] = "10.1.1.254"
        self.services["network"]["gateway"] = "10.1.1.1"
        self.services["network"]["netmask"] = "255.255.255.0"
        vm_ip = "10.1.1.10"
        """
        Creating isolated/guest network with ip range
        """
        network = Network.create(
            self.userapiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.domain.id,
            networkofferingid=network_offerings_list[0].id,
            zoneid=self.zone.id
        )
        self.assertIsNotNone(
            network,
            "Network creation failed"
        )
        # Deploying a VM
        vm_created = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=network.id,
            ipaddress=vm_ip,
            serviceofferingid=self.service_offering.id,
        )
        self.assertIsNotNone(
            vm_created,
            "VM creation failed"
        )
        # Listing all the VMs for a user again
        vm_response = VirtualMachine.list(
            self.userapiclient,
            id=vm_created.id,
        )
        status = validateList(vm_response)
        self.assertEqual(
            PASS,
            status[0],
            "vm list returned invalid response"
        )
        # Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(vm_response),
            "VM list count is not matching"
        )
        # Verifying that the NIC's in VM created are same as provided
        vm_nics = vm_created.nic
        status = validateList(vm_nics)
        self.assertEqual(
            PASS,
            status[0],
            "vm list returned invalid response for vm nics"
        )
        # Verifying that the size of nics is 1
        self.assertEqual(
            1,
            len(vm_nics),
            "VM is created with more than one nic which is not expected"
        )
        """
        Verifying that NIC IP address is as expected
        """
        self.assertEqual(
            str(vm_nics[0].ipaddress),
            vm_ip,
            "VM is not created with static ip address used in vm deployment"
        )
        try:
            vm_created.delete(self.apiClient, expunge=True)
        except Exception as e:
            raise Exception("Warning: Exception in expunging vm : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_25_ip_reallocation_ES1377(self):
        """
        @Desc: Test to verify dnsmasq dhcp conflict issue due to
               /ect/hosts not getting udpated
        @Steps:
        Step1: Create a network for the user
        Step2: List the network and check that it is created for the user
        Step3: Deploy vm1 with hostname hostA and ip address IP A in the
               above network
        Step4: List the vm and verify the ip address in the response and
               verify ssh access to vm
        Step5: Deploy vm2 with hostname hostB and ip address IP B in the
               same network
        Step6: Repeat step4
        Step7: Destroy vm1 and vm2
        Step8: Deploy vm3 with hostname hostA and ip address IP B
        Step9: Repeat step4
        Step10: Deploy vm4 with IP A and hostC
        Step11: Repeat step4
        """
        # Listing Network Offerings
        network_offerings_list = NetworkOffering.list(
            self.apiClient,
            forvpc="false",
            guestiptype="Isolated",
            state="Enabled",
            supportedservices="SourceNat",
            zoneid=self.zone.id
        )
        status = validateList(network_offerings_list)
        self.assertEqual(
            PASS,
            status[0],
            "Isolated Network Offerings with sourceNat enabled are not found"
        )
        """
        Create Isolated netwrok with ip range
        """
        self.services["network"]["startip"] = "10.1.1.2"
        self.services["network"]["endip"] = "10.1.1.254"
        self.services["network"]["gateway"] = "10.1.1.1"
        self.services["network"]["netmask"] = "255.255.255.0"
        """
        Creating isolated/guest network with ip range
        """
        network = Network.create(
            self.userapiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.domain.id,
            networkofferingid=network_offerings_list[0].id,
            zoneid=self.zone.id
        )
        self.assertIsNotNone(
            network,
            "Network creation failed"
        )
        vm_ip1 = "10.1.1.10"
        name1 = "hostA"
        self.debug("network id:%s" % network.id)
        self.services["virtual_machine"]["name"] = name1
        # Deploying a VM
        vm1 = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=[network.id],
            ipaddress=vm_ip1,
            serviceofferingid=self.service_offering.id,
            mode="advanced",
        )
        self.assertIsNotNone(
            vm1, "VM1 creation failed with ip address %s and host name %s" %
            (vm_ip1, name1))
        # self.cleanup.append(vm_created)
        # Listing all the VMs for a user again
        vm_response = VirtualMachine.list(
            self.userapiclient,
            id=vm1.id,
        )
        status = validateList(vm_response)
        self.assertEqual(
            PASS,
            status[0],
            "vm list api returned invalid response for vm1"
        )
        # Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(vm_response),
            "VM list count is not matching"
        )
        # Deploying a VM
        vm_ip2 = "10.1.1.20"
        name2 = "hostB"
        self.services["virtual_machine"]["name"] = name2
        vm2 = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=[network.id],
            ipaddress=vm_ip2,
            serviceofferingid=self.service_offering.id,
            mode="advanced",
        )
        self.assertIsNotNone(
            vm2,
            "VM2 creation failed"
        )
        vm_response = VirtualMachine.list(
            self.userapiclient,
            id=vm2.id,
        )
        status = validateList(vm_response)
        self.assertEqual(
            PASS,
            status[0],
            "vm list api returned invalid response for vm2"
        )
        # Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(vm_response),
            "VM list count is not matching after vm2 deployment"
        )
        try:
            vm1.delete(self.apiClient, expunge=True)
            vm2.delete(self.apiClient, expunge=True)
        except Exception as e:
            raise Exception("Warning: Exception in expunging vms : %s" % e)
        """
        Deploy vm3 with ip address of vm1 and host name of vm2 so both the vm1
        and vm2 entries would be deleted from dhcphosts file on VR becase
        dhcprelease matches entries with host name and ip address so it
        matches both the entries.
        """
        # Deploying a VM
        self.services["virtual_machine"]["name"] = name2
        vm3 = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=[network.id],
            ipaddress=vm_ip1,
            serviceofferingid=self.service_offering.id,
            mode="advanced",
        )
        self.assertIsNotNone(
            vm3,
            "VM3 creation failed"
        )
        vm_response = VirtualMachine.list(
            self.userapiclient,
            id=vm3.id,
        )
        status = validateList(vm_response)
        self.assertEqual(
            PASS,
            status[0],
            "vm list api returned invalid response for vm3"
        )
        # Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(vm_response),
            "VM list count is not matching after vm2 deployment"
        )
        # Deploying a VM
        """
        Deploy vm4 with ip address of vm2. dnsmasq and dhcprelase
        should be in sync. We should not see dhcp lease block due to IP
        reallocation.
        """
        name3 = "hostC"
        self.services["virtual_machine"]["name"] = name3
        vm4 = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=[network.id],
            ipaddress=vm_ip2,
            serviceofferingid=self.service_offering.id,
            mode="advanced",
        )
        self.assertIsNotNone(
            vm4,
            "VM4 creation failed"
        )
        vm_response = VirtualMachine.list(
            self.userapiclient,
            id=vm4.id,
        )
        status = validateList(vm_response)
        self.assertEqual(
            PASS,
            status[0],
            "vm list api returned invalid response for vm4"
        )
        # Verifying that the size of the list is 1
        self.assertEqual(
            1,
            len(vm_response),
            "VM list count is not matching after vm2 deployment"
        )
        try:
            vm3.delete(self.apiClient, expunge=True)
            vm4.delete(self.apiClient, expunge=True)
        except Exception as e:
            raise Exception(
                "Warning: Exception in expunging vms vm3 and vm4 : %s" %
                e)
        return



