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

#Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              validateList,
                              get_hypervisor_type)
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             Volume,
                             DiskOffering,
                             VmSnapshot,
                             Template,
                             listConfigurations)
from marvin.lib.common import (get_domain,list_isos,
                               get_zone,
                               get_template)
from nose.plugins.attrib import attr
from ast import literal_eval
from marvin.codes import PASS
from marvin.cloudstackException import CloudstackAPIException

class TestVMware(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(TestVMware, cls).getClsTestClient()
            cls.api_client = cls.testClient.getApiClient()
            cls.services = cls.testClient.getParsedTestDataConfig()
            cls.hypervisor = cls.testClient.getHypervisorInfo()
            # Get Domain, Zone, Template
            cls.domain = get_domain(cls.api_client)
            cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
            cls.template = get_template(
                cls.api_client,
                cls.zone.id,
                cls.services["ostype"]
            )
            if cls.zone.localstorageenabled:
                cls.storagetype = 'local'
                cls.services["service_offerings"]["tiny"]["storagetype"] = 'local'
                cls.services["disk_offering"]["storagetype"] = 'local'
            else:
                cls.storagetype = 'shared'
                cls.services["service_offerings"]["tiny"]["storagetype"] = 'shared'
                cls.services["disk_offering"]["storagetype"] = 'shared'

            cls.services['mode'] = cls.zone.networktype
            cls.services["virtual_machine"]["hypervisor"] = cls.testClient.getHypervisorInfo()
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
            cls.account = Account.create(
                cls.api_client,
                cls.services["account"],
                domainid=cls.domain.id
            )
            # Getting authentication for user in newly created Account
            cls.user = cls.account.user[0]
            cls.userapiclient = cls.testClient.getUserApiClient(cls.user.username, cls.domain.name)
            cls._cleanup.append(cls.disk_offering)
            cls._cleanup.append(cls.service_offering)
            cls._cleanup.append(cls.account)
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setup : %s" % e)
        return

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        #Clean up, terminate the created volumes
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    @attr(tags=["advanced"], required_hardware="true")
    def test1_attach_volume_ide(self):
        """
        @desc: Exception when attaching data disk to RHEL VM on vSphere
        Step1: Confirm that vmware.root.disk.controller = "ide" in Global Settings.
        Step2: Register RHEl 6.0 template and deploy a VM.
        Step3: Note that the root disk is attached to IDE.
        Step4: Create new DATA disk and attempt to attach it to the VM.
        Verify that step4 succeeds without any exception
        """
        self.hypervisor = str(get_hypervisor_type(self.api_client)).lower()
        if self.hypervisor != "vmware":
            self.skipTest("This test can be run only on vmware")
        cmd = listConfigurations.listConfigurationsCmd()
        cmd.name = "vmware.root.disk.controller"
        cmd.listAll = True
        try:
            config_descs = self.api_client.listConfigurations(cmd)
        except Exception as e:
            raise Exception("Failed to fetch configurations: %s" % e)
        if not isinstance(config_descs, list):
            raise Exception("List configs didn't returned a valid data")
        config_desc = config_descs[0]
        if str(config_desc.value).lower() != "ide":
            self.skipTest("This test is invalid if {} is not set to ide".format(config_desc.name))
        """
        Register RHEL 6.0 template and deploy vm
        """
        template = Template.register(
            self.userapiclient,
            self.services["rhel60template"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            hypervisor=self.hypervisor
        )
        self.assertIsNotNone(template,"Failed to register Rhel6 template")
        self.debug(
            "Registered a template with format {} and id {}".format(
                self.services["rhel60template"]["format"],template.id)
        )
        template.download(self.userapiclient)
        self.cleanup.append(template)
        vm = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=template.id,
            zoneid=self.zone.id
        )
        self.assertIsNotNone(vm,"Failed to deploy virtual machine")
        self.cleanup.append(vm)
        response = VirtualMachine.list(self.userapiclient,id=vm.id)
        status = validateList(response)
        self.assertEqual(status[0],PASS,"list vm response returned invalid list")
        """
        list root disk of the vm created above and make sure that device type is ide
        """
        volume_res = Volume.list(
            self.userapiclient,
            virtualmachineid=vm.id,
            type="root",
            listAll="true"
        )
        self.assertEqual(validateList(volume_res)[0],PASS,"list vm response returned invalid list")
        chaininfo = volume_res[0].chaininfo
        device_Bus = literal_eval(chaininfo)["diskDeviceBusName"]
        if "ide" not in device_Bus:
            self.fail("Root disk is not created with device type IDE")
        disk = Volume.create(
            self.userapiclient,
            self.services["volume"],
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering.id
        )
        self.assertIsNotNone(disk,"Failed to create custom volume")
        self.cleanup.append(disk)
        try:
            vm.attach_volume(self.userapiclient,disk)
            list_volumes = Volume.list(
                self.userapiclient,
                listall=self.services["listall"],
                id=disk.id
            )
            attached_volume = list_volumes[0]
            self.assertEqual(
                disk.id,
                attached_volume.id,
                "list volume response does not match with the volume created and attached to vm"
            )
        except Exception as e:
            self.fail("Failed to attach data disk to RHEL vm whose root disk type is IDE")
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test2_attach_ISO_in_CentOSVM(self):
        """
        @desc:Incorrect guest os mapping in vmware for CentOS 5.9 and above
        Step1 :Register an CentOS 6.3 template
        Step2 :Launch a VM
        Step3: Try to attach VMware Tools ISO
        Step4: Verify VMware tools ISO attached correctly
        """
        self.hypervisor = str(get_hypervisor_type(self.api_client)).lower()
        if self.hypervisor != "vmware":
            self.skipTest("This test can be run only on vmware")
        template = Template.register(
            self.userapiclient,
            self.services["CentOS6.3template"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            hypervisor=self.hypervisor
        )
        self.debug(
            "Registered a template with format {} and id {}".format(
                self.services["CentOS6.3template"]["format"],template.id)
        )
        template.download(self.userapiclient)
        self.cleanup.append(template)
        vm = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=template.id,
            zoneid=self.zone.id
        )
        self.cleanup.append(vm)
        response = VirtualMachine.list(self.userapiclient,id=vm.id)
        status = validateList(response)
        self.assertEqual(status[0],PASS,"list vm response returned invalid list")
        list_default_iso_response = list_isos(
            self.api_client,
            name="vmware-tools.iso",
            account="system",
            isready="true"
        )
        status = validateList(list_default_iso_response)
        self.assertEqual(
                PASS,
                status[0],
                "ISO list is empty")
        self.debug(
            "Registered a ISO with name {}".format(list_default_iso_response[0].name))
        try:
            vm.attach_iso(self.userapiclient,list_default_iso_response[0])
        except CloudstackAPIException as e:
            self.fail("Attached ISO failed : %s" % e)
        response = VirtualMachine.list(self.userapiclient, id=vm.id)
        status = validateList(response)
        self.assertEqual(status[0], PASS,"list vm response returned invalid list")
        attachedIsoName=response[0].isoname;
        self.assertEqual(attachedIsoName, "vmware-tools.iso", "vmware-tools.iso not attached")
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test3_attach_ISO_in_RHEL7OSVM(self):
        """
        @desc:Incorrect guest os mapping in vmware for Rhel7. Add a valid RHEL7 URL to execute this test case
        Step1 :Register an RHEL 7 template
        Step2 :Launch a VM
        Step3: Try to attach VMware Tools ISO
        Step4: Verify VMware tools ISO attached correctly
        """
        self.hypervisor = str(get_hypervisor_type(self.api_client)).lower()
        if self.hypervisor != "vmware":
            self.skipTest("This test can be run only on vmware")
        self.services["Rhel7template"]["url"]="http://10.147.28.7/templates/rhel71.ova",
        template = Template.register(
            self.userapiclient,
            self.services["Rhel7template"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            hypervisor=self.hypervisor
        )
        self.debug(
            "Registered a template with format {} and id {}".format(
                self.services["Rhel7template"]["format"],template.id)
        )
        template.download(self.userapiclient)
        self.cleanup.append(template)
        vm = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=template.id,
            zoneid=self.zone.id
        )
        self.cleanup.append(vm)
        response = VirtualMachine.list(self.userapiclient,id=vm.id)
        status = validateList(response)
        self.assertEqual(status[0],PASS,"list vm response returned invalid list")
        list_default_iso_response = list_isos(
            self.api_client,
            name="vmware-tools.iso",
            account="system",
            isready="true"
        )
        status = validateList(list_default_iso_response)
        self.assertEqual(
                PASS,
                status[0],
                "ISO list is empty")
        self.debug(
            "Registered a ISO with name {}".format(list_default_iso_response[0].name))
        try:
            vm.attach_iso(self.userapiclient,list_default_iso_response[0])
        except CloudstackAPIException as e:
            self.fail("Attached ISO failed : %s" % e)
        response = VirtualMachine.list(self.userapiclient, id=vm.id)
        status = validateList(response)
        self.assertEqual(status[0], PASS,"list vm response returned invalid list")
        attachedIsoName=response[0].isoname;
        self.assertEqual(attachedIsoName, "vmware-tools.iso", "vmware-tools.iso not attached")
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_04_check_vm_snapshot_creation_after_Instance_creation(self):
        """
        @summary: Test  if Snapshot creation is successful
        after VM deployment
        CLOUDSTACK-8830 : VM snapshot creation fails for 12 min

        Step1: Create a VM with any Service offering
        Step2: Create a VM snapshot
        Step3: Verify is VM SS creation is failed
        """

        if self.hypervisor.lower() not in ['vmware']:
            self.skipTest("This test case is only for vmware. Hence, skipping the test")
        vm = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            zoneid=self.zone.id
        )

        snapshot_created_1 = VmSnapshot.create(
            self.userapiclient,
            vm.id
        )
        self.assertIsNotNone(
            snapshot_created_1,
            "VM Snapshot creation failed"
        )



