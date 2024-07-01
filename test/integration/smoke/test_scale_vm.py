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
from marvin.codes import FAILED
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import scaleVirtualMachine
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Account,
                             Host,
                             VirtualMachine,
                             ServiceOffering,
                             DiskOffering,
                             Template,
                             Configurations,
                             Volume)
from marvin.lib.common import (get_zone,
                               get_template,
                               get_test_template,
                               get_domain)
from nose.plugins.attrib import attr
from marvin.sshClient import SshClient
import time

_multiprocess_shared_ = True


class TestScaleVm(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestScaleVm, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][
                    0].__dict__
        cls._cleanup = []
        cls.unsupportedHypervisor = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ('kvm', 'hyperv', 'lxc'):
            cls.unsupportedHypervisor = True
            return

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        if cls.hypervisor.lower() in ['simulator', 'vmware']:
            cls.template = get_template(
                cls.apiclient,
                cls.zone.id,
                cls.services["ostype"]
            )
            if cls.template == FAILED:
                assert False, "get_template() failed to return template\
                        with description %s" % cls.services["ostype"]
            cls.template = Template.update(
                cls.template,
                cls.apiclient,
                isdynamicallyscalable='true'
            )
        else:
            cls.template = get_test_template(
                cls.apiclient,
                cls.zone.id,
                cls.hypervisor
            )
            if cls.template == FAILED:
                assert False, "get_test_template() failed to return template\
                        with hypervisor %s" % cls.hypervisor
            cls.template = Template.update(
                cls.template,
                cls.apiclient,
                isdynamicallyscalable='true'
            )

        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = cls.template.id

        # Create account, service offerings, vm.
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.small_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )
        cls._cleanup.append(cls.small_offering)

        cls.big_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["big"]
        )
        cls._cleanup.append(cls.big_offering)

        Configurations.update(
                    cls.apiclient,
                    name="enable.dynamic.scale.vm",
                    value="true"
                )

        cls.small_offering_dynamic_scaling_disabled = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"],
            dynamicscalingenabled=False
        )

        cls.big_offering_dynamic_scaling_disabled = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"],
            dynamicscalingenabled=False
        )

        cls._cleanup = [
            cls.small_offering,
            cls.big_offering,
            cls.small_offering_dynamic_scaling_disabled,
            cls.big_offering_dynamic_scaling_disabled,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        Configurations.update(
            cls.apiclient,
            name="enable.dynamic.scale.vm",
            value="false"
        )
        Configurations.update(
            cls.apiclient,
            name="allow.diskOffering.change.during.scale.vm",
            value="false"
        )
        super(TestScaleVm,cls).tearDownClass()
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.services["disk_offering"]["disksize"] = 2

        if self.unsupportedHypervisor:
            self.skipTest("Skipping test because unsupported hypervisor\
                    %s" % self.hypervisor)

    def tearDown(self):
        # Clean up, terminate the created ISOs
        super(TestScaleVm,self).tearDown()
        return

    def get_ssh_client(self, ip, username, password, retries=10):
        """ Setup ssh client connection and return connection """
        try:
            ssh_client = SshClient(ip, 22, username, password, retries)
        except Exception as e:
            raise self.skipTest("Unable to create ssh connection: " % e)

        self.assertIsNotNone(
            ssh_client, "Failed to setup ssh connection to ip=%s" % ip)

        return ssh_client

    def is_host_xcpng8(self, hostname):
        return type(hostname) == list and len(hostname) > 0 and 'XCP-ng 8' in hostname[0]

    @attr(hypervisor="xenserver")
    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_01_scale_vm(self):
        """Test scale virtual machine
        """
        # Validate the following
        # Scale up the vm and see if it scales to the new svc offering and is
        # finally in running state

        #        VirtualMachine should be updated to tell cloudstack
        #        it has PV tools
        #        available and successfully scaled. We will only mock
        #        that behaviour
        #        here but it is not expected in production since the VM
        #        scaling is not
        #        guaranteed until tools are installed, vm rebooted

        # create a virtual machine
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.small_offering.id,
            mode=self.services["mode"]
        )
        self.cleanup.append(self.virtual_machine)

        # If hypervisor is Vmware, then check if
        # the vmware tools are installed and the process is running
        # Vmware tools are necessary for scale VM operation
        if self.hypervisor.lower() == "vmware":
            sshClient = self.virtual_machine.get_ssh_client()
            result = str(
                sshClient.execute("service vmware-tools status")).lower()
            self.debug("and result is: %s" % result)
            if "running" not in result:
                self.skipTest("Skipping scale VM operation because\
                    VMware tools are not installed on the VM")
        res = None
        if self.hypervisor.lower() != 'simulator':
            hostid = self.virtual_machine.hostid
            host = Host.list(
                       self.apiclient,
                       zoneid=self.zone.id,
                       hostid=hostid,
                       type='Routing'
                   )[0]

            try:
                username = self.hostConfig["username"]
                password = self.hostConfig["password"]
                ssh_client = self.get_ssh_client(host.ipaddress, username, password)
                res = ssh_client.execute("hostnamectl | grep 'Operating System' | cut -d':' -f2")
            except Exception as e:
                pass

            if 'XenServer' in res[0]:
                self.skipTest("Skipping test for XenServer as it's License does not allow scaling")

        self.virtual_machine.update(
            self.apiclient,
            isdynamicallyscalable='true')

        if self.is_host_xcpng8(res):
            self.debug("Only scaling for CPU for XCP-ng 8")
            offering_data = self.services["service_offerings"]["big"]
            offering_data["cpunumber"] = 2
            offering_data["memory"] = self.virtual_machine.memory
            self.bigger_offering = ServiceOffering.create(
                self.apiclient,
                offering_data
            )
            self.cleanup.append(self.bigger_offering)
        else:
            self.bigger_offering = self.big_offering

        self.debug("Scaling VM-ID: %s to service offering: %s and state %s" % (
            self.virtual_machine.id,
            self.bigger_offering.id,
            self.virtual_machine.state
        ))

        cmd = scaleVirtualMachine.scaleVirtualMachineCmd()
        cmd.serviceofferingid = self.bigger_offering.id
        cmd.id = self.virtual_machine.id

        try:
            self.apiclient.scaleVirtualMachine(cmd)
        except Exception as e:
            if "LicenceRestriction" in str(e):
                self.skipTest("Your XenServer License does not allow scaling")
            else:
                self.fail("Scaling failed with the following exception: " + str(e))

        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id
        )
        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            list_vm_response,
            None,
            "Check virtual machine is in listVirtualMachines"
        )

        vm_response = list_vm_response[0]
        self.assertEqual(
            vm_response.id,
            self.virtual_machine.id,
            "Check virtual machine ID of scaled VM"
        )

        self.debug(
            "Scaling VM-ID: %s from service offering: %s to new service\
                    offering %s and the response says %s" %
            (self.virtual_machine.id,
             self.virtual_machine.serviceofferingid,
             self.bigger_offering.id,
             vm_response.serviceofferingid))
        self.assertEqual(
            vm_response.serviceofferingid,
            self.bigger_offering.id,
            "Check service offering of the VM"
        )

        self.assertEqual(
            vm_response.state,
            'Running',
            "Check the state of VM"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_02_scale_vm_negative_offering_disable_scaling(self):
        """Test scale virtual machine which is created from a service offering for which dynamicscalingenabled is false. Scaling operation should fail.
        """

        #        VirtualMachine should be updated to tell cloudstack
        #        it has PV tools
        #        available and successfully scaled. We will only mock
        #        that behaviour
        #        here but it is not expected in production since the VM
        #        scaling is not
        #        guaranteed until tools are installed, vm rebooted

        # create a virtual machine which cannot be dynamically scalable
        self.virtual_machine_with_service_offering_dynamic_scaling_disabled = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.small_offering_dynamic_scaling_disabled.id,
            mode=self.services["mode"]
        )
        self.cleanup.append(self.virtual_machine_with_service_offering_dynamic_scaling_disabled)

        # If hypervisor is Vmware, then check if
        # the vmware tools are installed and the process is running
        # Vmware tools are necessary for scale VM operation
        if self.hypervisor.lower() == "vmware":
            sshClient = self.virtual_machine_with_service_offering_dynamic_scaling_disabled.get_ssh_client()
            result = str(
                sshClient.execute("service vmware-tools status")).lower()
            self.debug("and result is: %s" % result)
            if "running" not in result:
                self.skipTest("Skipping scale VM operation because\
                    VMware tools are not installed on the VM")

        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine_with_service_offering_dynamic_scaling_disabled.id
        )
        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            list_vm_response,
            None,
            "Check virtual machine is in listVirtualMachines"
        )

        vm_response = list_vm_response[0]
        self.assertEqual(
            vm_response.id,
            self.virtual_machine_with_service_offering_dynamic_scaling_disabled.id,
            "Check virtual machine ID of scaled VM"
        )

        self.assertEqual(
            vm_response.isdynamicallyscalable,
            False,
            "Check if VM is not dynamically scalable"
        )

        self.debug("Scaling VM-ID: %s to service offering: %s for which dynamic scaling is disabled and VM state %s" % (
            self.virtual_machine_with_service_offering_dynamic_scaling_disabled.id,
            self.big_offering_dynamic_scaling_disabled.id,
            self.virtual_machine_with_service_offering_dynamic_scaling_disabled.state
        ))

        cmd = scaleVirtualMachine.scaleVirtualMachineCmd()
        cmd.serviceofferingid = self.big_offering_dynamic_scaling_disabled.id
        cmd.id = self.virtual_machine_with_service_offering_dynamic_scaling_disabled.id

        try:
            self.apiclient.scaleVirtualMachine(cmd)
        except Exception as e:
            if "LicenceRestriction" in str(e):
                self.skipTest("Your XenServer License does not allow scaling")
            else:
                pass
        else:
            self.fail("Expected an exception to be thrown, failing")

        self.debug("Scaling VM-ID: %s to service offering: %s for which dynamic scaling is enabled and VM state %s" % (
            self.virtual_machine_with_service_offering_dynamic_scaling_disabled.id,
            self.big_offering.id,
            self.virtual_machine_with_service_offering_dynamic_scaling_disabled.state
        ))

        cmd = scaleVirtualMachine.scaleVirtualMachineCmd()
        cmd.serviceofferingid = self.big_offering.id
        cmd.id = self.virtual_machine_with_service_offering_dynamic_scaling_disabled.id

        try:
            self.apiclient.scaleVirtualMachine(cmd)
        except Exception as e:
            if "LicenceRestriction" in str(e):
                self.skipTest("Your XenServer License does not allow scaling")
            else:
                pass
        else:
            self.fail("Expected an exception to be thrown, failing")

        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_03_scale_vm_negative_vm_disable_scaling(self):
        """Test scale virtual machine which is not dynamically scalable to a service offering. Scaling operation should fail.
        """
        # Validate the following
        # Scale up the vm which is not dynamically scalable and see if scaling operation fails

        #        VirtualMachine should be updated to tell cloudstack
        #        it has PV tools
        #        available and successfully scaled. We will only mock
        #        that behaviour
        #        here but it is not expected in production since the VM
        #        scaling is not
        #        guaranteed until tools are installed, vm rebooted

        # create a virtual machine which cannot be dynamically scalable
        self.virtual_machine_not_dynamically_scalable = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.small_offering.id,
            mode=self.services["mode"],
            dynamicscalingenabled=False
        )
        self.cleanup.append(self.virtual_machine_not_dynamically_scalable)

        # If hypervisor is Vmware, then check if
        # the vmware tools are installed and the process is running
        # Vmware tools are necessary for scale VM operation
        if self.hypervisor.lower() == "vmware":
            sshClient = self.virtual_machine_not_dynamically_scalable.get_ssh_client()
            result = str(
                sshClient.execute("service vmware-tools status")).lower()
            self.debug("and result is: %s" % result)
            if "running" not in result:
                self.skipTest("Skipping scale VM operation because\
                    VMware tools are not installed on the VM")

        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine_not_dynamically_scalable.id
        )
        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            list_vm_response,
            None,
            "Check virtual machine is in listVirtualMachines"
        )
        vm_response = list_vm_response[0]
        self.assertEqual(
            vm_response.id,
            self.virtual_machine_not_dynamically_scalable.id,
            "Check virtual machine ID of scaled VM"
        )
        self.assertEqual(
            vm_response.isdynamicallyscalable,
            False,
            "Check if VM is not dynamically scalable"
        )

        self.debug("Scaling VM-ID: %s to service offering: %s for which dynamic scaling is enabled and VM state %s" % (
            self.virtual_machine_not_dynamically_scalable.id,
            self.big_offering.id,
            self.virtual_machine_not_dynamically_scalable.state
        ))

        cmd = scaleVirtualMachine.scaleVirtualMachineCmd()
        cmd.serviceofferingid = self.big_offering.id
        cmd.id = self.virtual_machine_not_dynamically_scalable.id

        try:
            self.apiclient.scaleVirtualMachine(cmd)
        except Exception as e:
            if "LicenceRestriction" in str(e):
                self.skipTest("Your XenServer License does not allow scaling")
            else:
                pass
        else:
            self.fail("Expected an exception to be thrown, failing")

        return

    @attr(hypervisor="xenserver")
    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_04_scale_vm_with_user_account(self):
        """Test scale virtual machine under useraccount
        """
        # Validate the following
        # Create a user Account and create a VM in it.
        # Scale up the vm and see if it scales to the new svc offering

        # Create an user account
        self.userAccount = Account.create(
            self.apiclient,
            self.services["account"],
            admin=False,
            domainid=self.domain.id
        )

        self.cleanup.append(self.userAccount)
        # Create user api client of the user account
        self.userapiclient = self.testClient.getUserApiClient(
            UserName=self.userAccount.name,
            DomainName=self.userAccount.domain
        )

        # create a virtual machine
        self.virtual_machine_in_user_account = VirtualMachine.create(
            self.userapiclient,
            self.services["small"],
            accountid=self.userAccount.name,
            domainid=self.userAccount.domainid,
            serviceofferingid=self.small_offering.id,
            mode=self.services["mode"]
        )

        if self.hypervisor.lower() == "vmware":
            sshClient = self.virtual_machine_in_user_account.get_ssh_client()
            result = str(
                sshClient.execute("service vmware-tools status")).lower()
            self.debug("and result is: %s" % result)
            if "running" not in result:
                self.skipTest("Skipping scale VM operation because\
                    VMware tools are not installed on the VM")
        res = None
        if self.hypervisor.lower() != 'simulator':
            list_vm_response = VirtualMachine.list(
                self.apiclient,
                id=self.virtual_machine_in_user_account.id
            )[0]
            hostid = list_vm_response.hostid
            host = Host.list(
                       self.apiclient,
                       zoneid=self.zone.id,
                       hostid=hostid,
                       type='Routing'
                   )[0]

            try:
                username = self.hostConfig["username"]
                password = self.hostConfig["password"]
                ssh_client = self.get_ssh_client(host.ipaddress, username, password)
                res = ssh_client.execute("hostnamectl | grep 'Operating System' | cut -d':' -f2")
            except Exception as e:
                pass

            if 'XenServer' in res[0]:
                self.skipTest("Skipping test for XenServer as it's License does not allow scaling")

        self.virtual_machine_in_user_account.update(
            self.userapiclient,
            isdynamicallyscalable='true')

        if self.is_host_xcpng8(res):
            self.debug("Only scaling for CPU for XCP-ng 8")
            offering_data = self.services["service_offerings"]["big"]
            offering_data["cpunumber"] = 2
            offering_data["memory"] = self.virtual_machine_in_user_account.memory
            self.bigger_offering = ServiceOffering.create(
                self.apiclient,
                offering_data
            )
            self.cleanup.append(self.bigger_offering)
        else:
            self.bigger_offering = self.big_offering

        self.debug("Scaling VM-ID: %s to service offering: %s and state %s" % (
            self.virtual_machine_in_user_account.id,
            self.bigger_offering.id,
            self.virtual_machine_in_user_account.state
        ))

        cmd = scaleVirtualMachine.scaleVirtualMachineCmd()
        cmd.serviceofferingid = self.bigger_offering.id
        cmd.id = self.virtual_machine_in_user_account.id

        try:
            self.userapiclient.scaleVirtualMachine(cmd)
        except Exception as e:
            if "LicenceRestriction" in str(e):
                self.skipTest("Your XenServer License does not allow scaling")
            else:
                self.fail("Scaling failed with the following exception: " + str(e))

        list_vm_response = VirtualMachine.list(
            self.userapiclient,
            id=self.virtual_machine_in_user_account.id
        )

        vm_response = list_vm_response[0]

        self.debug(
            "Scaling VM-ID: %s from service offering: %s to new service\
                    offering %s and the response says %s" %
            (self.virtual_machine_in_user_account.id,
             self.virtual_machine_in_user_account.serviceofferingid,
             self.bigger_offering.id,
             vm_response.serviceofferingid))
        self.assertEqual(
            vm_response.serviceofferingid,
            self.bigger_offering.id,
            "Check service offering of the VM"
        )

        self.assertEqual(
            vm_response.state,
            'Running',
            "Check the state of VM"
        )
        return

    @attr(hypervisor="xenserver")
    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_05_scale_vm_dont_allow_disk_offering_change(self):
        """Test scale virtual machine with disk offering changes
        """
        # Validate the following two cases

        # 1
        # create serviceoffering1 with diskoffering1
        # create serviceoffering2 with diskoffering2
        # create a VM with serviceoffering1
        # Scale up the vm to serviceoffering2
        # Check disk offering of root volume to be diskoffering1 since setting allow.diskOffering.change.during.scale.vm is false

        # 2
        # create serviceoffering3 with diskoffering3
        # update setting allow.diskOffering.change.during.scale.vm to true
        # scale up the VM to serviceoffering3
        # Check disk offering of root volume to be diskoffering3 since setting allow.diskOffering.change.during.scale.vm is true

        self.disk_offering1 = DiskOffering.create(
                                    self.apiclient,
                                    self.services["disk_offering"],
                                    )
        self._cleanup.append(self.disk_offering1)
        offering_data = {
            'displaytext': 'ServiceOffering1WithDiskOffering1',
            'cpuspeed': 500,
            'cpunumber': 1,
            'name': 'ServiceOffering1WithDiskOffering1',
            'memory': 512,
            'diskofferingid': self.disk_offering1.id
        }

        self.ServiceOffering1WithDiskOffering1 = ServiceOffering.create(
            self.apiclient,
            offering_data,
        )
        self._cleanup.append(self.ServiceOffering1WithDiskOffering1)

        # create a virtual machine
        self.virtual_machine_test = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.ServiceOffering1WithDiskOffering1.id,
            mode=self.services["mode"]
        )

        if self.hypervisor.lower() == "vmware":
            sshClient = self.virtual_machine_test.get_ssh_client()
            result = str(
                sshClient.execute("service vmware-tools status")).lower()
            self.debug("and result is: %s" % result)
            if "running" not in result:
                self.skipTest("Skipping scale VM operation because\
                    VMware tools are not installed on the VM")
        res = None
        if self.hypervisor.lower() != 'simulator':
            hostid = self.virtual_machine_test.hostid
            host = Host.list(
                       self.apiclient,
                       zoneid=self.zone.id,
                       hostid=hostid,
                       type='Routing'
                   )[0]

            try:
                username = self.hostConfig["username"]
                password = self.hostConfig["password"]
                ssh_client = self.get_ssh_client(host.ipaddress, username, password)
                res = ssh_client.execute("hostnamectl | grep 'Operating System' | cut -d':' -f2")
            except Exception as e:
                pass

            if 'XenServer' in res[0]:
                self.skipTest("Skipping test for XenServer as it's License does not allow scaling")

        self.virtual_machine_test.update(
            self.apiclient,
            isdynamicallyscalable='true')

        self.disk_offering2 = DiskOffering.create(
                                    self.apiclient,
                                    self.services["disk_offering"],
                                    )
        self._cleanup.append(self.disk_offering2)
        offering_data = {
            'displaytext': 'ServiceOffering2WithDiskOffering2',
            'cpuspeed': 1000,
            'cpunumber': 2,
            'name': 'ServiceOffering2WithDiskOffering2',
            'memory': 1024,
            'diskofferingid': self.disk_offering2.id
        }
        if self.is_host_xcpng8(res):
            self.debug("Only scaling for CPU for XCP-ng 8")
            offering_data["memory"] = self.virtual_machine_test.memory

        self.ServiceOffering2WithDiskOffering2 = ServiceOffering.create(
            self.apiclient,
            offering_data,
        )
        self._cleanup.append(self.ServiceOffering2WithDiskOffering2)

        self.debug("Scaling VM-ID: %s to service offering: %s and state %s" % (
            self.virtual_machine_test.id,
            self.ServiceOffering2WithDiskOffering2.id,
            self.virtual_machine_test.state
        ))

        cmd = scaleVirtualMachine.scaleVirtualMachineCmd()
        cmd.serviceofferingid = self.ServiceOffering2WithDiskOffering2.id
        cmd.id = self.virtual_machine_test.id

        try:
            self.apiclient.scaleVirtualMachine(cmd)
        except Exception as e:
            if "LicenceRestriction" in str(e):
                self.skipTest("Your XenServer License does not allow scaling")
            else:
                self.fail("Scaling failed with the following exception: " + str(e))

        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine_test.id
        )

        vm_response = list_vm_response[0]

        self.debug(
            "Scaling VM-ID: %s from service offering: %s to new service\
                    offering %s and the response says %s" %
            (self.virtual_machine_test.id,
             self.virtual_machine_test.serviceofferingid,
             self.ServiceOffering2WithDiskOffering2.id,
             vm_response.serviceofferingid))

        vm_response = list_vm_response[0]

        self.assertEqual(
            vm_response.serviceofferingid,
            self.ServiceOffering2WithDiskOffering2.id,
            "Check service offering of the VM"
        )

        volume_response = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine_test.id,
            listall=True
        )[0]

        self.assertEqual(
            volume_response.diskofferingid,
            self.disk_offering1.id,
            "Check disk offering of the VM, this should not change since allow.diskOffering.change.during.scale.vm is false"
        )

        # Do same scale vm operation with allow.diskOffering.change.during.scale.vm value to true
        if self.hypervisor.lower() in ['simulator']:
            self.debug("Simulator doesn't support changing disk offering, volume resize")
            return
        disk_offering_data = self.services["disk_offering"]
        if self.hypervisor.lower() in ['xenserver']:
            self.debug("For hypervisor %s, do not resize volume and just change try to change the disk offering")
            volume_response = Volume.list(
                self.apiclient,
                virtualmachineid=self.virtual_machine_test.id,
                listall=True
            )[0]
            disk_offering_data["disksize"] = int(volume_response.size / (1024 ** 3))
        self.disk_offering3 = DiskOffering.create(
                                    self.apiclient,
                                    disk_offering_data,
                                    )
        self._cleanup.append(self.disk_offering3)
        offering_data = {
            'displaytext': 'ServiceOffering3WithDiskOffering3',
            'cpuspeed': 1500,
            'cpunumber': 2,
            'name': 'ServiceOffering3WithDiskOffering3',
            'memory': 1024,
            'diskofferingid': self.disk_offering3.id
        }
        if self.is_host_xcpng8(res):
            self.debug("Only scaling for CPU for XCP-ng 8")
            offering_data["memory"] = vm_response.memory

        self.ServiceOffering3WithDiskOffering3 = ServiceOffering.create(
            self.apiclient,
            offering_data,
        )
        self._cleanup.append(self.ServiceOffering3WithDiskOffering3)

        Configurations.update(
            self.apiclient,
            name="allow.diskOffering.change.during.scale.vm",
            value="true"
        )

        self.debug("Scaling VM-ID: %s to service offering: %s and state %s" % (
            self.virtual_machine_test.id,
            self.ServiceOffering3WithDiskOffering3.id,
            self.virtual_machine_test.state
        ))

        cmd = scaleVirtualMachine.scaleVirtualMachineCmd()
        cmd.serviceofferingid = self.ServiceOffering3WithDiskOffering3.id
        cmd.id = self.virtual_machine_test.id

        try:
            self.apiclient.scaleVirtualMachine(cmd)
        except Exception as e:
            if "LicenceRestriction" in str(e):
                self.skipTest("Your XenServer License does not allow scaling")
            else:
                self.fail("Scaling failed with the following exception: " + str(e))

        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine_test.id
        )

        vm_response = list_vm_response[0]

        self.debug(
            "Scaling VM-ID: %s from service offering: %s to new service\
                    offering %s and the response says %s" %
            (self.virtual_machine_test.id,
             self.virtual_machine_test.serviceofferingid,
             self.ServiceOffering3WithDiskOffering3.id,
             vm_response.serviceofferingid))

        self.assertEqual(
            vm_response.serviceofferingid,
            self.ServiceOffering3WithDiskOffering3.id,
            "Check service offering of the VM"
        )

        volume_response = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine_test.id,
            listall=True
        )[0]

        self.assertEqual(
            volume_response.diskofferingid,
            self.disk_offering3.id,
            "Check disk offering of the VM, this should change since allow.diskOffering.change.during.scale.vm is true"
        )

        return
