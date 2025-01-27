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
""" P1 tests for Scaling up Vm
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (VirtualMachine, Volume, DiskOffering, ServiceOffering, Template)
from marvin.lib.common import (get_zone, get_domain)
from nose.plugins.attrib import attr

_multiprocess_shared_ = True


class TestRestoreVM(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestRestoreVM, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls._cleanup = []

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.service_offering = ServiceOffering.create(cls.apiclient, cls.services["service_offering"])
        cls._cleanup.append(cls.service_offering)

        cls.disk_offering = DiskOffering.create(cls.apiclient, cls.services["disk_offering"], disksize='8')
        cls._cleanup.append(cls.disk_offering)

        template_t1 = Template.register(cls.apiclient, cls.services["test_templates"][
            cls.hypervisor.lower() if cls.hypervisor.lower() != 'simulator' else 'xenserver'],
                                        zoneid=cls.zone.id, hypervisor=cls.hypervisor.lower())
        cls._cleanup.append(template_t1)
        template_t1.download(cls.apiclient)
        cls.template_t1 = Template.list(cls.apiclient, templatefilter='all', id=template_t1.id)[0]

        template_t2 = Template.register(cls.apiclient, cls.services["test_templates"][
            cls.hypervisor.lower() if cls.hypervisor.lower() != 'simulator' else 'xenserver'],
                                        zoneid=cls.zone.id, hypervisor=cls.hypervisor.lower())
        cls._cleanup.append(template_t2)
        template_t2.download(cls.apiclient)
        cls.template_t2 = Template.list(cls.apiclient, templatefilter='all', id=template_t2.id)[0]

    @classmethod
    def tearDownClass(cls):
        super(TestRestoreVM, cls).tearDownClass()
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_01_restore_vm(self):
        """Test restore virtual machine
        """
        # create a virtual machine
        virtual_machine = VirtualMachine.create(self.apiclient, self.services["virtual_machine"], zoneid=self.zone.id,
                                                templateid=self.template_t1.id,
                                                serviceofferingid=self.service_offering.id)
        self._cleanup.append(virtual_machine)

        old_root_vol = Volume.list(self.apiclient, virtualmachineid=virtual_machine.id)[0]
        self.assertEqual(old_root_vol.state, 'Ready', "Volume should be in Ready state")
        self.assertEqual(old_root_vol.size, self.template_t1.size, "Size of volume and template should match")

        virtual_machine.restore(self.apiclient, self.template_t2.id, expunge=True)

        restored_vm = VirtualMachine.list(self.apiclient, id=virtual_machine.id)[0]
        self.assertEqual(restored_vm.state, 'Running', "VM should be in a running state")
        self.assertEqual(restored_vm.templateid, self.template_t2.id, "VM's template after restore is incorrect")

        root_vol = Volume.list(self.apiclient, virtualmachineid=restored_vm.id)[0]
        self.assertEqual(root_vol.state, 'Ready', "Volume should be in Ready state")
        self.assertEqual(root_vol.size, self.template_t2.size, "Size of volume and template should match")

        old_root_vol = Volume.list(self.apiclient, id=old_root_vol.id)
        self.assertEqual(old_root_vol, None, "Old volume should be deleted")

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_02_restore_vm_with_disk_offering(self):
        """Test restore virtual machine
        """
        # create a virtual machine
        virtual_machine = VirtualMachine.create(self.apiclient, self.services["virtual_machine"], zoneid=self.zone.id,
                                                templateid=self.template_t1.id,
                                                serviceofferingid=self.service_offering.id)
        self._cleanup.append(virtual_machine)

        old_root_vol = Volume.list(self.apiclient, virtualmachineid=virtual_machine.id)[0]
        self.assertEqual(old_root_vol.state, 'Ready', "Volume should be in Ready state")
        self.assertEqual(old_root_vol.size, self.template_t1.size, "Size of volume and template should match")

        virtual_machine.restore(self.apiclient, self.template_t2.id, self.disk_offering.id, expunge=True)

        restored_vm = VirtualMachine.list(self.apiclient, id=virtual_machine.id)[0]
        self.assertEqual(restored_vm.state, 'Running', "VM should be in a running state")
        self.assertEqual(restored_vm.templateid, self.template_t2.id, "VM's template after restore is incorrect")

        root_vol = Volume.list(self.apiclient, virtualmachineid=restored_vm.id)[0]
        self.assertEqual(root_vol.diskofferingid, self.disk_offering.id, "Disk offering id should match")
        self.assertEqual(root_vol.state, 'Ready', "Volume should be in Ready state")
        self.assertEqual(root_vol.size, self.disk_offering.disksize * 1024 * 1024 * 1024,
                         "Size of volume and disk offering should match")

        old_root_vol = Volume.list(self.apiclient, id=old_root_vol.id)
        self.assertEqual(old_root_vol, None, "Old volume should be deleted")

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_03_restore_vm_with_disk_offering_custom_size(self):
        """Test restore virtual machine
        """
        # create a virtual machine
        virtual_machine = VirtualMachine.create(self.apiclient, self.services["virtual_machine"], zoneid=self.zone.id,
                                                templateid=self.template_t1.id,
                                                serviceofferingid=self.service_offering.id)
        self._cleanup.append(virtual_machine)

        old_root_vol = Volume.list(self.apiclient, virtualmachineid=virtual_machine.id)[0]
        self.assertEqual(old_root_vol.state, 'Ready', "Volume should be in Ready state")
        self.assertEqual(old_root_vol.size, self.template_t1.size, "Size of volume and template should match")

        virtual_machine.restore(self.apiclient, self.template_t2.id, self.disk_offering.id, rootdisksize=16)

        restored_vm = VirtualMachine.list(self.apiclient, id=virtual_machine.id)[0]
        self.assertEqual(restored_vm.state, 'Running', "VM should be in a running state")
        self.assertEqual(restored_vm.templateid, self.template_t2.id, "VM's template after restore is incorrect")

        root_vol = Volume.list(self.apiclient, virtualmachineid=restored_vm.id)[0]
        self.assertEqual(root_vol.diskofferingid, self.disk_offering.id, "Disk offering id should match")
        self.assertEqual(root_vol.state, 'Ready', "Volume should be in Ready state")
        self.assertEqual(root_vol.size, 16 * 1024 * 1024 * 1024, "Size of volume and custom disk size should match")

        old_root_vol = Volume.list(self.apiclient, id=old_root_vol.id)[0]
        self.assertEqual(old_root_vol.state, "Destroy", "Old volume should be in Destroy state")
        Volume.delete(old_root_vol, self.apiclient)

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_04_restore_vm_allocated_root(self):
        """Test restore virtual machine with root disk in allocated state
        """
        # create a virtual machine with allocated root disk by setting startvm=False
        virtual_machine = VirtualMachine.create(self.apiclient, self.services["virtual_machine"], zoneid=self.zone.id,
                                                templateid=self.template_t1.id,
                                                serviceofferingid=self.service_offering.id,
                                                startvm=False)
        self._cleanup.append(virtual_machine)
        old_root_vol = Volume.list(self.apiclient, virtualmachineid=virtual_machine.id)[0]
        self.assertEqual(old_root_vol.state, 'Allocated', "Volume should be in Allocated state")
        self.assertEqual(old_root_vol.size, self.template_t1.size, "Size of volume and template should match")

        virtual_machine.restore(self.apiclient, self.template_t2.id)
        restored_vm = VirtualMachine.list(self.apiclient, id=virtual_machine.id)[0]
        self.assertEqual(restored_vm.state, 'Stopped', "Check the state of VM")
        self.assertEqual(restored_vm.templateid, self.template_t2.id, "Check the template of VM")

        root_vol = Volume.list(self.apiclient, virtualmachineid=restored_vm.id)[0]
        self.assertEqual(root_vol.state, 'Allocated', "Volume should be in Allocated state")
        self.assertEqual(root_vol.size, self.template_t2.size, "Size of volume and template should match")

        virtual_machine.start(self.apiclient)
        root_vol = Volume.list(self.apiclient, virtualmachineid=restored_vm.id)[0]
        self.assertEqual(root_vol.state, 'Ready', "Volume should be in Ready state")

        old_root_vol = Volume.list(self.apiclient, id=old_root_vol.id)
        self.assertEqual(old_root_vol, None, "Old volume should be deleted")
