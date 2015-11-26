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
""" Test Path for Deploy VM in stopped state
"""
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             User,
                             Network,
                             Router,
                             DiskOffering,
                             Volume,
                             Iso,
                             Template,
                             Resources)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               createEnabledNetworkOffering,
                               wait_for_cleanup)
from marvin.codes import (PASS,
                          STOPPED,
                          RUNNING,
                          FAIL)
from marvin.sshClient import SshClient
import time


def VerifyChangeInServiceOffering(self, virtualmachine, serviceoffering):
    """List the VM and verify that the new values for cpuspeed,
       cpunumber and memory match with the new service offering"""

    exceptionOccured = False
    exceptionMessage = ""
    try:
        vmlist = VirtualMachine.list(self.userapiclient, id=virtualmachine.id)
        self.assertEqual(
            validateList(vmlist)[0],
            PASS,
            "vm list validation failed")
        vm = vmlist[0]

        # Verify the custom values
        self.assertEqual(str(vm.cpunumber), str(serviceoffering.cpunumber),
                         "vm cpu number %s not matching with cpu number in\
                      service offering %s" %
                         (vm.cpunumber, serviceoffering.cpunumber))

        self.assertEqual(str(vm.cpuspeed), str(serviceoffering.cpuspeed),
                         "vm cpu speed %s not matching with cpu speed in\
                     service offering %s" %
                         (vm.cpuspeed, serviceoffering.cpuspeed))

        self.assertEqual(str(vm.memory), str(serviceoffering.memory),
                         "vm memory %s not matching with memory in\
                     service offering %s" %
                         (vm.memory, serviceoffering.memory))
    except Exception as e:
        exceptionOccured = True
        exceptionMessage = e
    return [exceptionOccured, exceptionMessage]


def VerifyRouterState(apiclient, account, domainid, desiredState,
                      retries=0):
    """List the router associated with the account and
       verify that router state matches with the desired state
       Return PASS/FAIL depending upon whether router state matches
       or not"""

    isRouterStateDesired = False
    failureMessage = ""
    while retries >= 0:
        routers = Router.list(
            apiclient,
            account=account,
            domainid=domainid,
            listall=True
        )
        if str(routers[0].state).lower() == str(desiredState).lower():
            isRouterStateDesired = True
            break
        time.sleep(60)
        retries -= 1
    # whileEnd

    if not isRouterStateDesired:
        failureMessage = "Router state should be %s,\
                but it is %s" %\
            (desiredState, routers[0].state)
    return [isRouterStateDesired, failureMessage]


def CreateEnabledNetworkOffering(apiclient, networkServices):
    """Create network offering of given services and enable it"""

    result = createEnabledNetworkOffering(apiclient, networkServices)
    assert result[0] == PASS,\
        "Network offering creation/enabling failed due to %s" % result[2]
    return result[1]


class TestAdvancedZoneStoppedVM(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestAdvancedZoneStoppedVM, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient)
        cls._cleanup = []

        try:
            # Create an account
            cls.account = Account.create(
                cls.apiclient,
                cls.testdata["account"],
                domainid=cls.domain.id
            )
            cls._cleanup.append(cls.account)

            # If local storage is enabled, alter the offerings to use
            # localstorage
            if cls.zone.localstorageenable:
                cls.testdata["service_offering"]["storagetype"] = 'local'

            # Create 2 service offerings with different values for
            # for cpunumber, cpuspeed, and memory

            cls.testdata["service_offering"]["cpunumber"] = 1
            cls.testdata["service_offering"]["cpuspeed"] = 128
            cls.testdata["service_offering"]["memory"] = 256

            if cls.hypervisor.lower() == "hyperv":
                cls.testdata["service_offering"]["cpuspeed"] = 1024
                cls.testdata["service_offering"]["memory"] = 1024

            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"]
            )
            cls._cleanup.append(cls.service_offering)

            cls.testdata["service_offering"]["cpunumber"] = 2

            cls.service_offering_2 = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"]
            )
            cls._cleanup.append(cls.service_offering_2)

            # Create isolated network offering
            cls.isolated_network_offering = CreateEnabledNetworkOffering(
                cls.apiclient,
                cls.testdata["isolated_network_offering"]
            )
            cls._cleanup.append(cls.isolated_network_offering)

            cls.networkid = None
            if str(cls.zone.networktype).lower() == "advanced":
                cls.network = Network.create(
                    cls.apiclient, cls.testdata["isolated_network"],
                    networkofferingid=cls.isolated_network_offering.id,
                    accountid=cls.account.name,
                    domainid=cls.account.domainid,
                    zoneid=cls.zone.id)
                cls.networkid = cls.network.id

            # Create user api client of the account
            cls.userapiclient = testClient.getUserApiClient(
                UserName=cls.account.name,
                DomainName=cls.account.domain
            )

            template = get_template(
                cls.apiclient,
                cls.zone.id,
                cls.testdata["ostype"])
            cls.defaultTemplateId = template.id
            # Set Zones and disk offerings

            # Check that we are able to login to the created account
            respose = User.login(
                cls.apiclient,
                username=cls.account.name,
                password=cls.testdata["account"]["password"]
            )

            assert respose.sessionkey is not None,\
                "Login to the CloudStack should be successful\
                            response shall have non Null key"

        except Exception as e:
            cls.tearDownClass()
            raise e
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
            # Wait for Router cleanup before runnign further test case
            wait_for_cleanup(
                self.apiclient, [
                    "network.gc.interval", "network.gc.wait"])
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"], required_hardware="True")
    def test_01_pt_deploy_vm_without_startvm(self):
        """ Positive test for stopped VM test path - T1

        # 1.  Deploy VM in the network without specifying startvm parameter
        # 2.  List VMs and verify that VM is in running state
        # 3.  Verify that router is in running state (Advanced zone)
        # 4.  Add network rules for VM (done in base.py itself) to make
        #     it accessible
        # 5.  Verify that VM is accessible
        # 6.  Destroy and expunge the VM
        # 7.  Wait for network gc time interval and check that router is
        #     in Stopped state
        """
        # Create VM in account
        virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.defaultTemplateId,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[self.networkid, ] if self.networkid else None,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        response = virtual_machine.getState(
            self.userapiclient,
            VirtualMachine.RUNNING)
        self.assertEqual(response[0], PASS, response[1])

        if str(self.zone.networktype).lower() == "advanced":
            response = VerifyRouterState(
                self.apiclient,
                self.account.name,
                self.account.domainid,
                RUNNING
            )
            self.assertTrue(response[0], response[1])

        # Check VM accessibility
        try:
            SshClient(host=virtual_machine.ssh_ip,
                      port=self.testdata["natrule"]["publicport"],
                      user=virtual_machine.username,
                      passwd=virtual_machine.password)
        except Exception as e:
            self.fail("Exception while SSHing to VM: %s" % e)

        virtual_machine.delete(self.apiclient)

        if str(self.zone.networktype).lower() == "advanced":
            # Wait for router to go into stopped state
            wait_for_cleanup(
                self.apiclient, [
                    "network.gc.interval", "network.gc.wait"])

            response = VerifyRouterState(
                self.apiclient,
                self.account.name,
                self.account.domainid,
                STOPPED,
                retries=10
            )
            self.assertTrue(response[0], response[1])
        return

    @attr(tags=["advanced", "basic"], required_hardware="True")
    def test_02_pt_deploy_vm_with_startvm_true(self):
        """ Positive test for stopped VM test path - T1 variant

        # 1.  Deploy VM in the network specifying startvm parameter as True
        # 2.  List VMs and verify that VM is in running state
        # 3.  Verify that router is in running state (Advanced zone)
        # 4.  Add network rules for VM (done in base.py itself) to make
        #     it accessible
        # 5.  Verify that VM is accessible
        # 6.  Destroy and expunge the VM
        # 7.  Wait for network gc time interval and check that router is
        #     in stopped state
        """
        # Create VM in account
        virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.defaultTemplateId,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[self.networkid, ] if self.networkid else None,
            zoneid=self.zone.id,
            startvm=True,
            mode=self.zone.networktype
        )

        response = virtual_machine.getState(
            self.userapiclient,
            VirtualMachine.RUNNING)
        self.assertEqual(response[0], PASS, response[1])

        if str(self.zone.networktype).lower() == "advanced":
            response = VerifyRouterState(
                self.apiclient,
                self.account.name,
                self.account.domainid,
                RUNNING
            )
            self.assertTrue(response[0], response[1])

        # Check VM accessibility
        try:
            SshClient(host=virtual_machine.ssh_ip,
                      port=self.testdata["natrule"]["publicport"],
                      user=virtual_machine.username,
                      passwd=virtual_machine.password)
        except Exception as e:
            self.fail("Exception while SSHing to VM: %s" % e)

        virtual_machine.delete(self.apiclient)

        if str(self.zone.networktype).lower() == "advanced":
            # Wait for router to get router in stopped state
            wait_for_cleanup(
                self.apiclient, [
                    "network.gc.interval", "network.gc.wait"])

            response = VerifyRouterState(
                self.apiclient,
                self.account.name,
                self.account.domainid,
                STOPPED,
                retries=10
            )
            self.assertTrue(response[0], response[1])
        return

    @attr("simulator_only", tags=["advanced", "basic"], required_hardware="false")
    def test_03_pt_deploy_vm_with_startvm_false(self):
        """ Positive test for stopped VM test path - T2

        # 1.  Deploy VM in the network with specifying startvm parameter
        #     as False
        # 2.  List VMs and verify that VM is in stopped state
        # For Advanced zone
        # 3.  Start the VM, now router should be in running state
        """
        # Create VM in account
        virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.defaultTemplateId,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[self.networkid, ] if self.networkid else None,
            zoneid=self.zone.id,
            startvm=False,
            mode=self.zone.networktype
        )
        self.cleanup.append(virtual_machine)

        response = virtual_machine.getState(
            self.apiclient,
            VirtualMachine.STOPPED)
        self.assertEqual(response[0], PASS, response[1])

        if str(self.zone.networktype).lower() == "advanced":
            virtual_machine.start(self.userapiclient)

            response = VerifyRouterState(
                self.apiclient,
                self.account.name,
                self.account.domainid,
                RUNNING
            )
            self.assertTrue(response[0], response[1])
        return

    @attr("simulator_only", tags=["advanced", "basic"], required_hardware="false")
    def test_04_pt_startvm_false_attach_disk(self):
        """ Positive test for stopped VM test path - T3 and variant, T9

        # 1.  Deploy VM in the network with specifying startvm parameter
        #     as False
        # 2.  List VMs and verify that VM is in stopped state
        # 3.  Create a data disk and attach it to VM
        # 4.  Now detach the disk from the VM
        # 5.  Verify that attach and detach operations are successful
        # 6.  Deploy another VM in the account with startVM = False
        # 7.  Attach the same volume to this VM
        # 8.  Detach the volume
        # 9.  Both attach and detach operations should be successful
        """

        disk_offering = DiskOffering.create(
            self.apiclient,
            self.testdata["disk_offering"]
        )
        self.cleanup.append(disk_offering)

        volume = Volume.create(
            self.apiclient, self.testdata["volume"],
            zoneid=self.zone.id, account=self.account.name,
            domainid=self.account.domainid, diskofferingid=disk_offering.id
        )
        self.cleanup.append(volume)

        # Create VM in account
        virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.defaultTemplateId,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[self.networkid, ] if self.networkid else None,
            zoneid=self.zone.id,
            startvm=False,
            mode=self.zone.networktype
        )
        self.cleanup.append(virtual_machine)

        response = virtual_machine.getState(
            self.apiclient,
            VirtualMachine.STOPPED)
        self.assertEqual(response[0], PASS, response[1])

        virtual_machine.attach_volume(self.userapiclient, volume=volume)

        volumes = Volume.list(
            self.userapiclient,
            virtualmachineid=virtual_machine.id,
            type="DATADISK",
            listall=True
        )
        self.assertEqual(
            validateList(volumes)[0],
            PASS,
            "Volumes list validation failed"
        )
        self.assertEqual(
            volumes[0].id,
            volume.id,
            "Listed Volume id not matching with attached volume id"
        )

        virtual_machine.detach_volume(
            self.userapiclient,
            volume)

        volumes = Volume.list(
            self.userapiclient,
            virtualmachineid=virtual_machine.id,
            type="DATADISK",
            listall=True
        )
        self.assertEqual(
            validateList(volumes)[0],
            FAIL,
            "Detached volume should not be listed"
        )

        virtual_machine_2 = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.defaultTemplateId,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[self.networkid, ] if self.networkid else None,
            zoneid=self.zone.id,
            startvm=False,
            mode=self.zone.networktype
        )
        self.cleanup.append(virtual_machine_2)

        response = virtual_machine_2.getState(
            self.apiclient,
            VirtualMachine.STOPPED)
        self.assertEqual(response[0], PASS, response[1])

        virtual_machine_2.attach_volume(self.userapiclient, volume=volume)

        volumes = Volume.list(
            self.userapiclient,
            virtualmachineid=virtual_machine_2.id,
            type="DATADISK",
            listall=True
        )
        self.assertEqual(
            validateList(volumes)[0],
            PASS,
            "Volumes list validation failed"
        )
        self.assertEqual(
            volumes[0].id,
            volume.id,
            "Listed Volume id not matching with attached volume id"
        )

        virtual_machine_2.detach_volume(
            self.userapiclient,
            volume)

        volumes = Volume.list(
            self.userapiclient,
            virtualmachineid=virtual_machine_2.id,
            type="DATADISK",
            listall=True
        )
        self.assertEqual(
            validateList(volumes)[0],
            FAIL,
            "Detached volume should not be listed"
        )
        return

    @attr("simulator_only", tags=["advanced", "basic"], required_hardware="false")
    def test_05_pt_startvm_false_attach_disk_change_SO(self):
        """ Positive test for stopped VM test path - T4

        # 1.  Deploy VM in the network with specifying startvm parameter
        #     as False
        # 2.  List VMs and verify that VM is in stopped state
        # 3.  Create a data disk and attach it to VM
        # 4.  Change the service offering of VM from small to medium,
              verify that the operation is successful
        # 5.  Start the VM
        # 6.  Now detach the disk from the VM
        # 7.  Verify that attach and detach operations are successful
        """

        disk_offering = DiskOffering.create(
            self.apiclient,
            self.testdata["disk_offering"]
        )
        self.cleanup.append(disk_offering)

        volume = Volume.create(
            self.apiclient, self.testdata["volume"],
            zoneid=self.zone.id, account=self.account.name,
            domainid=self.account.domainid, diskofferingid=disk_offering.id
        )
        self.cleanup.append(volume)

        # Create VM in account
        virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.defaultTemplateId,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[self.networkid, ] if self.networkid else None,
            zoneid=self.zone.id,
            startvm=False,
            mode=self.zone.networktype
        )
        self.cleanup.append(virtual_machine)

        response = virtual_machine.getState(
            self.apiclient,
            VirtualMachine.STOPPED)
        self.assertEqual(response[0], PASS, response[1])

        virtual_machine.attach_volume(self.userapiclient, volume=volume)

        volumes = Volume.list(
            self.userapiclient,
            virtualmachineid=virtual_machine.id,
            type="DATADISK",
            listall=True
        )
        self.assertEqual(
            validateList(volumes)[0],
            PASS,
            "Volumes list validation failed"
        )
        self.assertEqual(
            volumes[0].id,
            volume.id,
            "Listed Volume id not matching with attached volume id"
        )

        # Change service offering of VM and verify that it is changed
        virtual_machine.change_service_offering(
            self.userapiclient,
            serviceOfferingId=self.service_offering_2.id
        )

        response = VerifyChangeInServiceOffering(
            self,
            virtual_machine,
            self.service_offering_2
        )
        exceptionOccured, exceptionMessage = response[0], response[1]
        self.assertFalse(exceptionOccured, exceptionMessage)

        virtual_machine.detach_volume(
            self.userapiclient,
            volume)

        volumes = Volume.list(
            self.userapiclient,
            virtualmachineid=virtual_machine.id,
            type="DATADISK",
            listall=True
        )
        self.assertEqual(
            validateList(volumes)[0],
            FAIL,
            "Detached volume should not be listed"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="True")
    def test_06_pt_startvm_false_attach_iso(self):
        """ Positive test for stopped VM test path - T5

        # 1.  Deploy VM in the network with specifying startvm parameter
        #     as False
        # 2.  List VMs and verify that VM is in stopped state
        # 3.  Register an ISO and attach it to the VM
        # 4.  Verify that ISO is attached to the VM
        """

        if self.hypervisor.lower() in ['lxc']:
            self.skipTest(
                "feature is not supported in %s" %
                self.hypervisor)
        # Create VM in account
        virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.defaultTemplateId,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[self.networkid, ] if self.networkid else None,
            zoneid=self.zone.id,
            startvm=False,
            mode=self.zone.networktype
        )
        self.cleanup.append(virtual_machine)

        response = virtual_machine.getState(
            self.apiclient,
            VirtualMachine.STOPPED)
        self.assertEqual(response[0], PASS, response[1])

        iso = Iso.create(
            self.userapiclient,
            self.testdata["iso"],
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id
        )

        iso.download(self.userapiclient)
        virtual_machine.attach_iso(self.userapiclient, iso)

        vms = VirtualMachine.list(
            self.userapiclient,
            id=virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            validateList(vms)[0],
            PASS,
            "List vms should return a valid list"
        )
        vm = vms[0]
        self.assertEqual(
            vm.isoid,
            iso.id,
            "The ISO status should be reflected in list Vm call"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="True")
    def test_07_pt_startvm_false_attach_iso_running_vm(self):
        """ Positive test for stopped VM test path - T5 variant

        # 1.  Deploy VM in the network with specifying startvm parameter
        #     as False
        # 2.  List VMs and verify that VM is in stopped state
        # 3.  Start the VM and verify that it is in running state
        # 3.  Register an ISO and attach it to the VM
        # 4.  Verify that ISO is attached to the VM
        """

        if self.hypervisor.lower() in ['lxc']:
            self.skipTest(
                "feature is not supported in %s" %
                self.hypervisor)

        # Create VM in account
        virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.defaultTemplateId,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[self.networkid, ] if self.networkid else None,
            zoneid=self.zone.id,
            startvm=False,
            mode=self.zone.networktype
        )
        self.cleanup.append(virtual_machine)

        response = virtual_machine.getState(
            self.apiclient,
            VirtualMachine.STOPPED)
        self.assertEqual(response[0], PASS, response[1])

        virtual_machine.start(self.userapiclient)

        response = virtual_machine.getState(
            self.apiclient,
            VirtualMachine.RUNNING)
        self.assertEqual(response[0], PASS, response[1])

        iso = Iso.create(
            self.userapiclient,
            self.testdata["iso"],
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id
        )

        iso.download(self.userapiclient)
        virtual_machine.attach_iso(self.userapiclient, iso)

        vms = VirtualMachine.list(
            self.userapiclient,
            id=virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            validateList(vms)[0],
            PASS,
            "List vms should return a valid list"
        )
        vm = vms[0]
        self.assertEqual(
            vm.isoid,
            iso.id,
            "The ISO status should be reflected in list Vm call"
        )
        return

    @attr(tags=["advanced", "basic"], required_hardware="True")
    def test_08_pt_startvm_false_password_enabled_template(self):
        """ Positive test for stopped VM test path - T10

        # 1   Create a password enabled template
        # 2.  Deploy a new VM with password enabled template
        # 3.  Verify that VM is in stopped state
        # 4.  Start the VM, verify that it is in running state
        # 5.  Verify that new password is generated for the VM
        """

        if self.hypervisor.lower() in ['lxc']:
            self.skipTest(
                "feature is not supported in %s" %
                self.hypervisor)
        vm_for_template = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.defaultTemplateId,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype,
            networkids=[self.networkid, ] if self.networkid else None)

        vm_for_template.password = self.testdata["virtual_machine"]["password"]
        ssh = vm_for_template.get_ssh_client()

        # below steps are required to get the new password from
        # VR(reset password)
        # http://cloudstack.org/dl/cloud-set-guest-password
        # Copy this file to /etc/init.d
        # chmod +x /etc/init.d/cloud-set-guest-password
        # chkconfig --add cloud-set-guest-password
        # similar steps to get SSH key from web so as to make it ssh enabled

        cmds = [
            "cd /etc/init.d;wget http://people.apache.org/~tsp/\
                    cloud-set-guest-password",
            "chmod +x /etc/init.d/cloud-set-guest-password",
            "chkconfig --add cloud-set-guest-password"]
        for c in cmds:
            ssh.execute(c)

        # Stop virtual machine
        vm_for_template.stop(self.userapiclient)

        list_volume = Volume.list(
            self.userapiclient,
            virtualmachineid=vm_for_template.id,
            type='ROOT',
            listall=True)

        if isinstance(list_volume, list):
            self.volume = list_volume[0]
        else:
            raise Exception(
                "Exception: Unable to find root volume for VM: %s" %
                vm_for_template.id)

        self.testdata["template"]["ostype"] = self.testdata["ostype"]
        # Create templates for Edit, Delete & update permissions testcases
        pw_ssh_enabled_template = Template.create(
            self.userapiclient,
            self.testdata["template"],
            self.volume.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(pw_ssh_enabled_template)
        # Delete the VM - No longer needed
        vm_for_template.delete(self.apiclient)

        # Create VM in account
        virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.defaultTemplateId,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            startvm=False,
            mode=self.zone.networktype,
            networkids=[self.networkid, ] if self.networkid else None
        )
        self.cleanup.append(virtual_machine)

        response = virtual_machine.getState(
            self.apiclient,
            VirtualMachine.STOPPED)
        self.assertEqual(response[0], PASS, response[1])

        virtual_machine.start(self.userapiclient)

        vms = virtual_machine.list(
            self.userapiclient,
            id=virtual_machine.id,
            listall=True)

        self.assertEqual(
            validateList(vms)[0],
            PASS,
            "vms list validation failed"
        )
        self.assertNotEqual(
            str(vms[0].password),
            str(virtual_machine.password),
            "New password should be generated for the VM"
        )
        return

    @attr("simulator_only", tags=["advanced", "basic"], required_hardware="false")
    def test_09_pt_destroy_stopped_vm(self):
        """ Positive test for stopped VM test path - T11

        # 1.  Deploy VM in the network with specifying startvm parameter
        #     as False
        # 2.  Verify that VM is in Stopped state
        # 3.  Start the VM and verify that it is in running state
        # 3.  Stop the VM, verify that it is in Stopped state
        # 4.  Destroy the VM and verify that it is in Destroyed state
        """
        # Create VM in account
        virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.defaultTemplateId,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[self.networkid, ] if self.networkid else None,
            zoneid=self.zone.id,
            startvm=False,
            mode=self.zone.networktype
        )

        response = virtual_machine.getState(
            self.apiclient,
            VirtualMachine.STOPPED)
        self.assertEqual(response[0], PASS, response[1])

        virtual_machine.start(self.userapiclient)

        response = virtual_machine.getState(
            self.apiclient,
            VirtualMachine.RUNNING)
        self.assertEqual(response[0], PASS, response[1])

        virtual_machine.stop(self.userapiclient)

        response = virtual_machine.getState(
            self.apiclient,
            VirtualMachine.STOPPED)
        self.assertEqual(response[0], PASS, response[1])

        virtual_machine.delete(self.apiclient, expunge=False)
        response = virtual_machine.getState(
            self.apiclient,
            VirtualMachine.DESTROYED)
        self.assertEqual(response[0], PASS, response[1])
        return

    @attr("simulator_only", tags=["advanced", "basic"], required_hardware="false")
    def test_10_max_account_limit(self):
        """ Positive test for stopped VM test path - T12

        # 1.  Create an account in root domain and set the VM limit
        #     to max 2 VMs
        # 2.  Deploy two VMs in the account with startvm parameter
        #     as false
        # 3.  Deployment of both VMs should be successful
        # 4.  Try to deploy 3rd VM with startvm False, deployment should fail
        """

        # Create an account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(account)

        Resources.updateLimit(
            self.apiclient,
            resourcetype=0,
            max=2,
            account=account.name,
            domainid=account.domainid
        )

        VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.defaultTemplateId,
            accountid=account.name,
            domainid=account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            startvm=False,
            mode=self.zone.networktype
        )

        VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.defaultTemplateId,
            accountid=account.name,
            domainid=account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            startvm=False,
            mode=self.zone.networktype
        )

        with self.assertRaises(Exception):
            VirtualMachine.create(
                self.apiclient,
                self.testdata["small"],
                templateid=self.defaultTemplateId,
                accountid=account.name,
                domainid=account.domainid,
                serviceofferingid=self.service_offering.id,
                zoneid=self.zone.id,
                startvm=False,
                mode=self.zone.networktype
            )
        return
