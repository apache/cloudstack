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
from marvin.codes import FAILED, KVM, PASS, XEN_SERVER, RUNNING
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import random_gen, cleanup_resources, validateList, is_snapshot_on_nfs, isAlmostEqual
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             VmSnapshot)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_suitable_test_template,
                               list_snapshots,
                               list_virtual_machines)
import time


class TestVmSnapshot(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestVmSnapshot, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls._cleanup = []
        cls.unsupportedHypervisor = False
        cls.hypervisor = testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ("hyperv", "lxc"):
            cls.unsupportedHypervisor = True
            return

        cls.services = testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())

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
        cls._cleanup.append(cls.account)

        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls._cleanup.append(cls.service_offering)
        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["small"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            mode=cls.zone.networktype
        )
        cls.random_data_0 = random_gen(size=100)
        cls.test_dir = "$HOME"
        cls.random_data = "random.data"
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

        if self.unsupportedHypervisor:
            self.skipTest("Skipping test because unsupported hypervisor\
                    %s" % self.hypervisor)
        return

    def tearDown(self):
        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_01_create_vm_snapshots(self):
        """Test to create VM snapshots
        """

        try:
            # Login to VM and write data to file system
            ssh_client = self.virtual_machine.get_ssh_client()

            cmds = [
                "echo %s > %s/%s" %
                (self.random_data_0, self.test_dir, self.random_data),
                "cat %s/%s" %
                (self.test_dir, self.random_data)]

            for c in cmds:
                self.debug(c)
                result = ssh_client.execute(c)
                self.debug(result)

        except Exception:
            self.fail("SSH failed for Virtual machine: %s" %
                      self.virtual_machine.ipaddress)
        self.assertEqual(
            self.random_data_0,
            result[0],
            "Check the random data has be write into temp file!"
        )

        time.sleep(self.services["sleep"])

        #KVM VM Snapshot needs to set snapshot with memory
        MemorySnapshot = False
        if self.hypervisor.lower() in (KVM.lower()):
           MemorySnapshot = True

        vm_snapshot = VmSnapshot.create(
            self.apiclient,
            self.virtual_machine.id,
            MemorySnapshot,
            "TestSnapshot",
            "Display Text"
        )
        self.assertEqual(
            vm_snapshot.state,
            "Ready",
            "Check the snapshot of vm is ready!"
        )

        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_02_revert_vm_snapshots(self):
        """Test to revert VM snapshots
        """

        try:
            ssh_client = self.virtual_machine.get_ssh_client()

            cmds = [
                "rm -rf %s/%s" % (self.test_dir, self.random_data),
                "ls %s/%s" % (self.test_dir, self.random_data)
            ]

            for c in cmds:
                self.debug(c)
                result = ssh_client.execute(c)
                self.debug(result)

        except Exception:
            self.fail("SSH failed for Virtual machine: %s" %
                      self.virtual_machine.ipaddress)

        if str(result[0]).index("No such file or directory") == -1:
            self.fail("Check the random data has be delete from temp file!")

        time.sleep(self.services["sleep"])

        list_snapshot_response = VmSnapshot.list(
            self.apiclient,
            vmid=self.virtual_machine.id,
            listall=True)

        self.assertEqual(
            isinstance(list_snapshot_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            list_snapshot_response,
            None,
            "Check if snapshot exists in ListSnapshot"
        )

        self.assertEqual(
            list_snapshot_response[0].state,
            "Ready",
            "Check the snapshot of vm is ready!"
        )

        #We don't need to stop the VM when taking a VM Snapshot on KVM
        if self.hypervisor.lower() in (KVM.lower()):
           pass
        else:
           self.virtual_machine.stop(self.apiclient)

        VmSnapshot.revertToSnapshot(
            self.apiclient,
            list_snapshot_response[0].id)

        #We don't need to start the VM when taking a VM Snapshot on KVM
        if self.hypervisor.lower() in (KVM.lower()):
           pass
        else:
           self.virtual_machine.start(self.apiclient)

        try:
            ssh_client = self.virtual_machine.get_ssh_client(reconnect=True)

            cmds = [
                "cat %s/%s" % (self.test_dir, self.random_data)
            ]

            for c in cmds:
                self.debug(c)
                result = ssh_client.execute(c)
                self.debug(result)

        except Exception:
            self.fail("SSH failed for Virtual machine: %s" %
                      self.virtual_machine.ipaddress)

        self.assertEqual(
            self.random_data_0,
            result[0],
            "Check the random data is equal with the ramdom file!"
        )

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_03_delete_vm_snapshots(self):
        """Test to delete vm snapshots
        """

        list_snapshot_response = VmSnapshot.list(
            self.apiclient,
            vmid=self.virtual_machine.id,
            listall=True)

        self.assertEqual(
            isinstance(list_snapshot_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            list_snapshot_response,
            None,
            "Check if snapshot exists in ListSnapshot"
        )
        VmSnapshot.deleteVMSnapshot(
            self.apiclient,
            list_snapshot_response[0].id)

        time.sleep(self.services["sleep"] * 3)

        list_snapshot_response = VmSnapshot.list(
            self.apiclient,
            vmid=self.virtual_machine.id,
            listall=True)

        self.assertEqual(
            list_snapshot_response,
            None,
            "Check list vm snapshot has be deleted"
        )

class Utils:

    def __init__(self):
        self.added_service_offerings = {
            'testOffering1' : {'displaytext': 'Test Offering 1', 'cpuspeed': 600, 'cpunumber': 1, 'name': 'Test Offering 1', 'memory': 256},
            'testOffering2' : {'displaytext': 'Test Offering 2', 'cpuspeed': 600, 'cpunumber': 2, 'name': 'Test Offering 2', 'memory': 512}
        }

class TestChangeServiceOfferingForVmWithSnapshots(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(TestChangeServiceOfferingForVmWithSnapshots, cls).getClsTestClient()
            cls.api_client = cls.testClient.getApiClient()
            cls.services = cls.testClient.getParsedTestDataConfig()
            cls.hypervisor = cls.testClient.getHypervisorInfo()
            cls.unsupportedHypervisor = False
            if cls.hypervisor.lower() in (KVM.lower(), "hyperv", "lxc"):
                cls.unsupportedHypervisor = True
                return

            cls.domain = get_domain(cls.api_client)
            cls.zone = get_zone(
                cls.api_client,
                cls.testClient.getZoneForTests()
            )
            cls.services["small"]["zoneid"] = cls.zone.id

            cls.template = get_suitable_test_template(
                cls.api_client,
                cls.zone.id,
                cls.services["ostype"],
                cls.hypervisor
            )
            if cls.template == FAILED:
                assert False, "get_suitable_test_template() failed to return template\
                    with description %s" % cls.services["ostype"]

            test_offerings = Utils().added_service_offerings
            for offering in test_offerings:
                cls.services["service_offerings"][offering] = test_offerings[offering]

            # Create 2 different service offerings
            cls.service_offering_1 = ServiceOffering.create(
                cls.api_client,
                cls.services["service_offerings"]["testOffering1"]
            )
            cls._cleanup.append(cls.service_offering_1)

            cls.service_offering_2 = ServiceOffering.create(
                cls.api_client,
                cls.services["service_offerings"]["testOffering2"]
            )
            cls._cleanup.append(cls.service_offering_2)

            cls.account = Account.create(
                cls.api_client,
                cls.services["account"],
                domainid=cls.domain.id
            )
            cls._cleanup.append(cls.account)

        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.unsupportedHypervisor:
            self.skipTest("Skipping test because unsupported hypervisor\
                    %s" % self.hypervisor)

    def tearDown(self):
        # Clean up, terminate the created resources
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def wait_vm_start(self, apiclient, vmid, timeout, sleep):
        while timeout:
            vms = VirtualMachine.list(apiclient, id=vmid)
            vm_list_validation_result = validateList(vms)
            if vm_list_validation_result[0] == PASS and vm_list_validation_result[1].state == RUNNING:
                return timeout
            time.sleep(sleep)
            timeout = timeout - 1

        return timeout

    def checkCPUAndMemory(self, ssh, service_offering):
        cpuinfo = ssh.execute("cat /proc/cpuinfo")
        cpu_cnt = len([i for i in cpuinfo if "processor" in i])
        # 'cpu MHz\t\t: 2660.499'
        cpu_speed = [i for i in cpuinfo if "cpu MHz" in i][0].split()[3]
        meminfo = ssh.execute("cat /proc/meminfo")
        # MemTotal:        1017464 kB
        total_mem = [i for i in meminfo if "MemTotal" in i][0].split()[1]

        self.debug(
            "CPU count: %s, CPU Speed: %s, Mem Info: %s" % (cpu_cnt, cpu_speed, total_mem)
        )
        self.assertAlmostEqual(
            int(cpu_cnt),
            service_offering.cpunumber,
            "Check CPU Count for service offering"
        )

        range = 40
        if self.hypervisor.lower() == "hyperv":
            range = 200
        self.assertTrue(
            isAlmostEqual(int(int(total_mem) / 1024),
                          int(service_offering.memory),
                          range=range
            ),
            "Check Memory(kb) for service offering"
        )

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_change_service_offering_for_vm_with_snapshots(self):
        """Test to change service offering for instances with vm snapshots
        """

        # 1) Create Virtual Machine using service offering 1
        self.debug("Creating VM using Service Offering 1")
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            templateid=self.template.id,
            zoneid=self.zone.id,
            hypervisor=self.hypervisor,
            mode=self.zone.networktype,
            serviceofferingid=self.service_offering_1.id
        )

        # Verify Service OFfering 1 CPU cores and memory
        try:
            ssh_client = virtual_machine.get_ssh_client(reconnect=True)
            self.checkCPUAndMemory(ssh_client, self.service_offering_1)
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" % (virtual_machine.ipaddress, e))

        # 2) Take VM Snapshot
        self.debug("Taking VM Snapshot for VM - ID: %s" % virtual_machine.id)
        vm_snapshot = VmSnapshot.create(
            self.apiclient,
            virtual_machine.id,
        )

        # 3) Stop Virtual Machine
        self.debug("Stopping VM - ID: %s" % virtual_machine.id)
        try:
            virtual_machine.stop(self.apiclient)

            timeout = self.services["timeout"]

            while True:
                time.sleep(self.services["sleep"])

                # Ensure that VM is in stopped state
                list_vm_response = list_virtual_machines(
                    self.apiclient,
                    id=virtual_machine.id
                )

                if isinstance(list_vm_response, list):
                    vm = list_vm_response[0]
                    if vm.state == 'Stopped':
                        self.debug("VM state: %s" % vm.state)
                        break

                if timeout == 0:
                    raise Exception(
                        "Failed to stop VM (ID: %s) in change service offering" % vm.id)

                timeout = timeout - 1
        except Exception as e:
            self.fail("Failed to stop VM: %s" % e)

        # 4) Change service offering for VM with snapshots from Service Offering 1 to Service Offering 2
        self.debug("Changing service offering from Service Offering 1 to Service Offering 2 for VM - ID: %s" % virtual_machine.id)
        virtual_machine.change_service_offering(self.apiclient, self.service_offering_2.id)

        # 5) Start VM
        self.debug("Starting VM - ID: %s" % virtual_machine.id)
        try:
            virtual_machine.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start virtual machine: %s, %s" % (virtual_machine.name, e))

        # Wait for vm to start
        timeout = self.wait_vm_start(self.apiclient, virtual_machine.id, self.services["timeout"],
                            self.services["sleep"])
        if timeout == 0:
            self.fail("The virtual machine %s failed to start even after %s minutes"
                   % (virtual_machine.name, self.services["timeout"]))

        list_vm_response = list_virtual_machines(
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
        self.assertEqual(
            list_vm_response[0].state,
            "Running",
            "Check virtual machine is in running state"
        )
        self.assertEqual(
            list_vm_response[0].id,
            virtual_machine.id,
            "Check virtual machine id"
        )

        # 6) Verify service offering has changed
        try:
            ssh_client_2 = virtual_machine.get_ssh_client(reconnect=True)
            self.checkCPUAndMemory(ssh_client_2, self.service_offering_2)
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" % (virtual_machine.ipaddress, e))

        # 7) Stop Virtual Machine
        self.debug("Stopping VM - ID: %s" % virtual_machine.id)
        try:
            virtual_machine.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop VM: %s" % e)

        time.sleep(30)
        # 8) Revert to VM Snapshot
        self.debug("Revert to vm snapshot: %s" % vm_snapshot.id)
        try:
            VmSnapshot.revertToSnapshot(
                self.apiclient,
                vm_snapshot.id
            )
        except Exception as e:
            self.fail("Failed to revert to VM Snapshot: %s - %s" % (vm_snapshot.id, e))

        # 9) Start VM
        self.debug("Starting VM - ID: %s" % virtual_machine.id)
        try:
            virtual_machine.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start virtual machine: %s, %s" % (virtual_machine.name, e))

        # 10) Verify service offering has changed to Service Offering 1 (from VM Snapshot)
        try:
            ssh_client_3 = virtual_machine.get_ssh_client(reconnect=True)
            self.checkCPUAndMemory(ssh_client_3, self.service_offering_1)
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" % (virtual_machine.ipaddress, e))

        return
