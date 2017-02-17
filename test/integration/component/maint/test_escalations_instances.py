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
                             Template,
                             Configurations)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
from nose.plugins.attrib import attr
from marvin.codes import PASS
from marvin.sshClient import SshClient
import time

class TestInstance(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(TestInstance, cls).getClsTestClient()
            cls.api_client = cls.testClient.getApiClient()
            cls.services = cls.testClient.getParsedTestDataConfig()
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
                cls.services["service_offerings"]["small"]
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
            cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__
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

    def RestartServers(self):
        """ Restart management server and usage server """

        sshClient = SshClient(
            self.mgtSvrDetails["mgtSvrIp"],
            22,
            self.mgtSvrDetails["user"],
            self.mgtSvrDetails["passwd"]
        )
        command = "service cloudstack-management restart"
        sshClient.execute(command)
        return

    def updateConfigurAndRestart(self,name, value):
        Configurations.update(self.apiClient,
                              name,value )
        self.RestartServers()
        time.sleep(self.services["sleep"])

    @attr(tags=["advanced"], required_hardware="true")
    def test1_attach_volume(self):
        """
        @desc: Unable to attach 7th Disk to windows server 2012R2 instance. Add a valid windows server 2012 URL to execute this test case
        Step1: Set global config vmware.root.disk.controller to 'osdefault'
        Step2: Deploy a Windows 2012 R2 instance.
        Step3:  Attach 6 disks to the VM.
        Step4: Try attaching a 7th disk to the VM
        Verify that step4 succeeds without any exception
        """
        self.hypervisor = str(get_hypervisor_type(self.api_client)).lower()
        if self.hypervisor != "vmware":
            self.skipTest("This test can be run only on vmware")
        self.updateConfigurAndRestart("vmware.root.disk.controller","osdefault")

        self.services["Windows Server 2012"]["url"]="http://10.147.28.7/templates/Windows2012/WindowsServer2012R2.ova.gz",
        template = Template.register(
           self.userapiclient,
          self.services["Windows Server 2012"],
            zoneid=self.zone.id,
           account=self.account.name,
           domainid=self.account.domainid
        )
        self.assertIsNotNone(template,"Failed to register Windows server 2012 R2 template")
        self.debug(
           "Registered a template with format {} and id {}".format(
              self.services["Windows Server 2012"]["format"],template.id)
        )
        template.download(self.userapiclient)
        self.cleanup.append(template)

        # Creating a big service offering for windows VM launch
        big_service_offering = ServiceOffering.create(
                self.apiClient,
                self.services["service_offerings"]["big"]
            )
        self.cleanup.append(big_service_offering)
        vm = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=big_service_offering.id,
            templateid=template.id,
            zoneid=self.zone.id
        )
        self.assertIsNotNone(vm,"Failed to deploy virtual machine")
        self.cleanup.append(vm)
        response = VirtualMachine.list(self.userapiclient,id=vm.id)
        status = validateList(response)
        self.assertEqual(status[0],PASS,"list vm response returned invalid list")

        for i in range(0,7):
            self.services["volume"]["diskname"]=i
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
                self.fail("Failed to attach {} data disk to Windows server 2012 R2 vm ".format(i))
        return
    @attr(tags=["advanced"], required_hardware="true")
    def test_Scale_VM(self):
        """
        @desc:
        1. Enable dynamic scaling in Global settings
        2. Register an CentOS 7 tempplate(with tools) and tick dynamic scaling
        3. Deploy VM with this template
        4.Start the VM and try to change service offering

        """
        self.hypervisor = str(get_hypervisor_type(self.api_client)).lower()
        if self.hypervisor != "xenserver":
            self.skipTest("This test can be run only on xenserver")
        self.updateConfigurAndRestart("enable.dynamic.scale.vm","true")
        template = Template.register(
           self.userapiclient,
          self.services["CentOS7template"],
            zoneid=self.zone.id,
           account=self.account.name,
           domainid=self.account.domainid
        )
        self.assertIsNotNone(template,"Failed to register CentOS 7 template")
        self.debug(
           "Registered a template with format {} and id {}".format(
              self.services["CentOS7template"]["format"],template.id)
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
        self.assertEqual(status[1].state,"Running", "vm is not running")

        service_offering = ServiceOffering.create(
                self.apiClient,
                self.services["service_offerings"]["big"]
            )
        time.sleep(self.services["sleep"])
        vm.scale(self.userapiclient,service_offering.id)
        scaleresponse = VirtualMachine.list(self.userapiclient,id=vm.id)
        scalestatus = validateList(scaleresponse)
        self.assertEqual(scalestatus[0],PASS,"list vm response returned invalid list")
        self.assertEqual(scalestatus[1].serviceofferingname,service_offering.name, " service offering is not same")
        self.assertEqual(scalestatus[1].serviceofferingid,service_offering.id, " service offering ids are not same")


        return
