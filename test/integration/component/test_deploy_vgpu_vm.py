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
# Unless   by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# Test from the Marvin - Testing in Python wiki

# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.cloudstackAPI import changeServiceForVirtualMachine, startVirtualMachine

# Import Integration Libraries

# base - contains all resources as entities and defines create, delete,
# list operations on them
from marvin.lib.base import Account, VirtualMachine, ServiceOffering, Template, Host, VmSnapshot

# utils - utility classes for common cleanup, external library wrappers etc
from marvin.lib.utils import cleanup_resources

# common - commonly used methods for all tests are listed here
from marvin.lib.common import get_zone, get_domain, list_hosts, list_service_offering, get_windows_template, get_pod, list_clusters, list_virtual_machines

from marvin.sshClient import SshClient

from marvin.codes import FAILED

from nose.plugins.attrib import attr

import time


class TestvGPUWindowsVm(cloudstackTestCase):

    """
    Testing vGPU VM with All vGPU service offerings
    """
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestvGPUWindowsVm, cls).getClsTestClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        cls.apiclient = cls.testClient.getApiClient()
        cls._cleanup = []
        cls.cleanup = []
        hosts = list_hosts(
            cls.apiclient,
            hypervisor="XenServer"
        )
        if hosts is None:
            raise unittest.SkipTest(
                "There are no XenServers available. GPU feature is supported only on XenServer.Check listhosts response")
        else:
            cls.nongpuhosts = []
            cls.nonvgpu = []
            cls.nonvgpu_service_offering = []

            cls.k1hosts = 0
            cls.k1passthroughgpuhosts = 0
            cls.k100gpuhosts = 0
            cls.k120qgpuhosts = 0
            cls.k140qgpuhosts = 0
            cls.k160qgpuhosts = 0
            cls.k180qgpuhosts = 0
            cls.k100_vgpu_service_offering = []
            cls.vm_k1_card = []

            cls.k2hosts = 0
            cls.k2passthroughgpuhosts = 0
            cls.k200gpuhosts = 0
            cls.k220qgpuhosts = 0
            cls.k240qgpuhosts = 0
            cls.k260qgpuhosts = 0
            cls.k280qgpuhosts = 0
            cls.k200_vgpu_service_offering = []
            cls.vm_k2_card = []
            cls.vm2_k2_card = []

            cls.m60hosts = 0
            cls.m60passthroughgpuhosts = 0
            cls.m60_0qgpuhosts = 0
            cls.m60_1qgpuhosts = 0
            cls.m60_2qgpuhosts = 0
            cls.m60_4qgpuhosts = 0
            cls.m60_8qgpuhosts = 0
            cls.m60_0bgpuhosts = 0
            cls.m60_1bgpuhosts = 0
            cls.m60_1agpuhosts = 0
            cls.m60_2agpuhosts = 0
            cls.m60_4agpuhosts = 0
            cls.m60_8agpuhosts = 0
            cls.m60_vgpu_service_offering = []
            cls.vm_m60_card = []

            cls.vmlifecycletest = 0
            cls.vmsnapwomemory = 0
            cls.vmsnapwithmemory = 0

            for ghost in hosts:
                if ghost.hypervisorversion >= "6.2.0":

                    sshClient = SshClient(
                        host=ghost.ipaddress,
                        port=cls.testdata['configurableData']['host']["publicport"],
                        user=cls.testdata['configurableData']['host']["username"],
                        passwd=cls.testdata['configurableData']['host']["password"])

                    if ghost.hypervisorversion == "6.2.0":
                        res = len(
                            sshClient.execute("xe patch-list uuid=0850b186-4d47-11e3-a720-001b2151a503"))
                        if res == 0:
                            continue

                    k1card = len(sshClient.execute("lspci | grep \"GRID K1\""))
                    k2card = len(sshClient.execute("lspci | grep \"GRID K2\""))

                    m60card = 0
                    if ghost.hypervisorversion >= "7.0.0":
                        m60card = len(sshClient.execute("lspci | grep \"Tesla M60\""))

                    cls.debug(
                        "K1/K2 cards and M60 card details are :%s %s %s" %
                        (k1card, k2card, m60card))

                    if (k1card == 0) and (k2card == 0) and (m60card == 0):
                        cls.nongpuhosts.append(ghost.ipaddress)

                    if k1card != 0:
                        cls.k1hosts = cls.k1hosts + 1
                        k1passthrough = len(
                            sshClient.execute("xe vgpu-type-list model-name='passthrough'"))
                        k100 = len(
                            sshClient.execute("xe vgpu-type-list model-name=\"GRID K100\""))
                        k120q = len(
                            sshClient.execute("xe vgpu-type-list model-name=\"GRID K120Q\""))
                        k140q = len(
                            sshClient.execute("xe vgpu-type-list model-name=\"GRID K140Q\""))
                        k160q = len(
                            sshClient.execute("xe vgpu-type-list model-name=\"GRID K160Q\""))
                        k180q = len(
                            sshClient.execute("xe vgpu-type-list model-name=\"GRID K180Q\""))
                        if ((k1passthrough == 0) and (k100 == 0) and (k120q == 0) and (
                                k140q == 0) and (k160q == 0) and (k180q == 0)):
                            continue
                        else:
                            if k1passthrough != 0:
                                cls.k1passthroughgpuhosts = cls.k1passthroughgpuhosts + 1
                            if k100 != 0:
                                cls.k100gpuhosts = cls.k100gpuhosts + 1
                            if k120q != 0:
                                cls.k120qgpuhosts = cls.k120qgpuhosts + 1
                            if k140q != 0:
                                cls.k140qgpuhosts = cls.k140qgpuhosts + 1
                            if k160q != 0:
                                cls.k160qgpuhosts = cls.k160qgpuhosts + 1
                            if k180q != 0:
                                cls.k180qgpuhosts = cls.k180qgpuhosts + 1

                    if k2card != 0:
                        cls.k2hosts = cls.k2hosts + 1
                        k2passthrough = len(
                            sshClient.execute("xe vgpu-type-list model-name='passthrough'"))
                        k200 = len(
                            sshClient.execute("xe vgpu-type-list model-name=\"GRID K200\""))
                        k220q = len(
                            sshClient.execute("xe vgpu-type-list model-name=\"GRID K220Q\""))
                        k240q = len(
                            sshClient.execute("xe vgpu-type-list model-name=\"GRID K240Q\""))
                        k260q = len(
                            sshClient.execute("xe vgpu-type-list model-name=\"GRID K260Q\""))
                        k280q = len(
                            sshClient.execute("xe vgpu-type-list model-name=\"GRID K280Q\""))
                        if ((k2passthrough == 0) and (k200 == 0) and (k220q == 0) and (k240q == 0)
                            and (k260q == 0) and (k280q == 0)):
                            continue
                        else:
                            if k2passthrough != 0:
                                cls.k2passthroughgpuhosts = cls.k2passthroughgpuhosts + 1
                            if k200 != 0:
                                cls.k200gpuhosts = cls.k200gpuhosts + 1
                            if k220q != 0:
                                cls.k220qgpuhosts = cls.k220qgpuhosts + 1
                            if k240q != 0:
                                cls.k240qgpuhosts = cls.k240qgpuhosts + 1
                            if k260q != 0:
                                cls.k260qgpuhosts = cls.k260qgpuhosts + 1
                            if k280q != 0:
                                cls.k280qgpuhosts = cls.k280qgpuhosts + 1

                    if m60card != 0:
                        cls.m60hosts = cls.m60hosts + 1
                        m60passthrough = len(
                            sshClient.execute("xe vgpu-type-list model-name='passthrough'"))
                        m60_0q = len(
                            sshClient.execute("xe vgpu-type-list model-name=\"Tesla M60-0Q\""))
                        m60_1q = len(
                            sshClient.execute("xe vgpu-type-list model-name=\"Tesla M60-1Q\""))
                        m60_2q = len(
                            sshClient.execute("xe vgpu-type-list model-name=\"Tesla M60-2Q\""))
                        m60_4q = len(
                            sshClient.execute("xe vgpu-type-list model-name=\"Tesla M60-4Q\""))
                        m60_8q = len(
                            sshClient.execute("xe vgpu-type-list model-name=\"Tesla M60-8Q\""))
                        m60_0b = len(
                            sshClient.execute("xe vgpu-type-list model-name=\"Tesla M60-0B\""))
                        m60_1b = len(
                            sshClient.execute("xe vgpu-type-list model-name=\"Tesla M60-1B\""))
                        m60_1a = len(
                            sshClient.execute("xe vgpu-type-list model-name=\"Tesla M60-1A\""))
                        m60_2a = len(
                            sshClient.execute("xe vgpu-type-list model-name=\"Tesla M60-2A\""))
                        m60_4a = len(
                            sshClient.execute("xe vgpu-type-list model-name=\"Tesla M60-4A\""))
                        m60_8a = len(
                            sshClient.execute("xe vgpu-type-list model-name=\"Tesla M60-8A\""))
                        if ((m60passthrough == 0) and (m60_0q == 0) and (m60_1q == 0) and (
                                    m60_2q == 0) and (m60_4q == 0) and (m60_8q == 0) and (
                                    m60_0b == 0) and (m60_1b == 0) and (m60_1a == 0) and (
                                    m60_2a == 0) and (m60_4a == 0) and (m60_8a == 0)):
                            continue
                        else:
                            if m60passthrough != 0:
                                cls.m60passthroughgpuhosts = cls.m60passthroughgpuhosts + 1
                            if m60_0q != 0:
                                cls.m60_0qgpuhosts = cls.m60_0qgpuhosts + 1
                            if m60_1q != 0:
                                cls.m60_1qgpuhosts = cls.m60_1qgpuhosts + 1
                            if m60_2q != 0:
                                cls.m60_2qgpuhosts = cls.m60_2qgpuhosts + 1
                            if m60_4q != 0:
                                cls.m60_4qgpuhosts = cls.m60_4qgpuhosts + 1
                            if m60_8q != 0:
                                cls.m60_8qgpuhosts = cls.m60_8qgpuhosts + 1
                            if m60_0b != 0:
                                cls.m60_0bgpuhosts = cls.m60_0bgpuhosts + 1
                            if m60_1b != 0:
                                cls.m60_1bgpuhosts = cls.m60_1bgpuhosts + 1
                            if m60_1a != 0:
                                cls.m60_1agpuhosts = cls.m60_1agpuhosts + 1
                            if m60_2a != 0:
                                cls.m60_2agpuhosts = cls.m60_2agpuhosts + 1
                            if m60_4a != 0:
                                cls.m60_4agpuhosts = cls.m60_4agpuhosts + 1
                            if m60_8a != 0:
                                cls.m60_8agpuhosts = cls.m60_8agpuhosts + 1
        if (cls.k1hosts == 0) and (cls.k2hosts == 0) and (cls.m60hosts == 0):
            raise unittest.SkipTest(
                "No XenServer available with GPU Drivers installed")

        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.domain = get_domain(cls.apiclient)
        cls.pod = get_pod(cls.apiclient, cls.zone.id)
        cls.account = Account.create(
            cls.apiclient,
            cls.testdata["account"],
            domainid=cls.domain.id
        )

        cls.template = get_windows_template(
            cls.apiclient,
            cls.zone.id,
            ostype_desc="Windows 8 (64-bit)")
        #cls.template = get_windows_template(cls.apiclient, cls.zone.id ,ostype_desc="Windows Server 2012 (64-bit)")

        if cls.template == FAILED:
            if "http://pleaseupdateURL/dummy.vhd" in cls.testdata[
                    "vgpu"]["templateregister1"]["url"]:
                raise unittest.SkipTest(
                    "Check Test Data file if it has the valid template URL")
            cls.template = Template.register(
                cls.apiclient,
                cls.testdata["vgpu"]["templateregister1"],
                hypervisor="XenServer",
                zoneid=cls.zone.id,
                domainid=cls.account.domainid,
                account=cls.account.name
            )
            timeout = cls.testdata["vgpu"]["timeout"]

            while True:
                time.sleep(cls.testdata["vgpu"]["sleep"])
                list_template_response = Template.list(
                    cls.apiclient,
                    templatefilter=cls.testdata["templatefilter"],
                    id=cls.template.id
                )
                if (isinstance(list_template_response, list)) is not True:
                    raise unittest.SkipTest(
                        "Check list template api response returns a valid list")

                if len(list_template_response) is None:
                    raise unittest.SkipTest(
                        "Check template registered is in List Templates")

                template_response = list_template_response[0]
                if template_response.isready:
                    break

                if timeout == 0:
                    raise unittest.SkipTest(
                        "Failed to download template(ID: %s). " %
                        template_response.id)

                timeout = timeout - 1

        cls._cleanup = [
            cls.account,
            # cls.k100_vgpu_service_offering,
            # cls.k200_vgpu_service_offering
        ]

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def setUp(self):
        self.testdata = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    def check_host_vgpu_capacity(self, gpucard, gtype):
        gputhosts = list_hosts(
            self.apiclient,
            hypervisor="XenServer"
        )
        vgpucapacity = 0
        for ghost in gputhosts:
            if ghost.gpugroup is not None:
                for gp in ghost.gpugroup:
                    if gp.gpugroupname == gpucard:
                        for gptype in gp.vgpu:
                            if gptype.vgputype == gtype:
                                vgpucapacity = vgpucapacity + \
                                    gptype.remainingcapacity

        return(vgpucapacity)

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def vgpu_serviceoffering_creation(self, gtype, nvidiamodel):
        if gtype == "nonvgpuoffering" and nvidiamodel == "None":
            self.service_offering = ServiceOffering.create(
                self.apiclient,
                self.testdata["vgpu"]["service_offerings"][gtype]
            )
            self.debug(
                "service offering details are:%s" %
                self.service_offering)
            list_service_response = ServiceOffering.list(
                self.apiclient,
                id=self.service_offering.id
            )

            if list_service_response is None:
                raise unittest.SkipTest(
                    "Check Service Offering list for %s service offering" %
                    (gtype))

            self.assertEqual(
                list_service_response[0].displaytext,
                self.testdata["vgpu"]["service_offerings"][gtype]["displaytext"],
                "Check server display text in createServiceOfferings")
            self.assertEqual(
                list_service_response[0].name,
                self.testdata["vgpu"]["service_offerings"][gtype]["name"],
                "Check name in createServiceOffering"
            )
        else:
            self.testdata["vgpu"]["service_offerings"][gtype]["serviceofferingdetails"] = [
                {'pciDevice': nvidiamodel}, {'vgpuType': gtype}]
            self.service_offering = ServiceOffering.create(
                self.apiclient,
                self.testdata["vgpu"]["service_offerings"][gtype]
            )
            list_service_response = ServiceOffering.list(
                self.apiclient,
                id=self.service_offering.id
            )

            if list_service_response is None:
                raise unittest.SkipTest(
                    "Check Service Offering list for %s service offering" %
                    (gtype))

            self.assertEqual(
                list_service_response[0].serviceofferingdetails.vgpuType,
                gtype,
                "Failed To Create Service Offering . Check vGPU Service Offering list")
            self.assertEqual(
                list_service_response[0].displaytext,
                self.testdata["vgpu"]["service_offerings"][gtype]["displaytext"],
                "Check server displaytext in createServiceOfferings")
            self.assertEqual(
                list_service_response[0].name,
                self.testdata["vgpu"]["service_offerings"][gtype]["name"],
                "Check name in createServiceOffering"
            )
        return(self.service_offering)

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def check_for_vGPU_resource(
            self,
            hostid,
            vminstancename,
            serviceofferingid,
            vgputype):
        """
        Validate the VM for VGPU resources
        """
        """Create SSH Client for Host Connection
        """
        vgpu_host = list_hosts(
            self.apiclient,
            id=hostid
        )
        ssh_client = SshClient(
            host=vgpu_host[0].ipaddress,
            port=self.testdata['configurableData']['host']["publicport"],
            user=self.testdata['configurableData']['host']["username"],
            passwd=self.testdata['configurableData']['host']["password"])
        """
        Get vGPU type model
        """
        vgpu_type_model = ssh_client.execute(
            "xe vgpu-list vm-name-label=" +
            vminstancename +
            " params=type-model-name --minimal")
        self.debug(
            "vgpu type model is %s and value is %s and length is %s" %
            (vgpu_type_model, vgpu_type_model[0], len(
                vgpu_type_model[0])))
        if vgputype is None:
            if len(vgpu_type_model[0]) == 0:
                self.debug("This is non GPU instance")
                return
            else:
                self.fail(
                    "Non vGPU VM has GPU cards @ host:%s" %
                    vminstancename)
        self.assertNotEqual(
            len(vgpu_type_model),
            0,
            "The VM is NOT deployed with vGPU cards"
        )
        """
        List service offering from which VM is deployed
        """
        list_service_offering_response = ServiceOffering.list(
            self.apiclient,
            id=serviceofferingid
        )
        """ Check whether the vgputype in the service offering is same as the one obtained by listing the vgpu type model
        """
        self.debug(
            "VM vGPU type is: %s and vGPU type on XenServer is : %s" %
            (list_service_offering_response[0].serviceofferingdetails.vgpuType,
             vgpu_type_model[0]))
        self.assertEqual(
            list_service_offering_response[0].serviceofferingdetails.vgpuType,
            vgpu_type_model[0],
            "VM does not have the correct GPU resources, verified on the host"
        )
        """
        Check whether the VM is deployed with the card which was mentioned in the service offering
        """

        self.assertEqual(
            vgputype,
            list_service_offering_response[0].serviceofferingdetails.vgpuType,
            "The VM is NOT deployed with correct cards, verified from CS"
        )
        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def deploy_vGPU_windows_vm(self, vgpuofferingid, vgput):
        """
        Validate vGPU K1 windows instances
        """
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            serviceofferingid=vgpuofferingid,
            templateid=self.template.id
        )
        time.sleep(self.testdata["vgpu"]["sleep"])
        list_vms = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id)

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % self.virtual_machine.id
        )

        self.assertEqual(
            isinstance(list_vms, list),
            True,
            "List VM response was not a valid list"
        )
        self.assertNotEqual(
            len(list_vms),
            0,
            "List VM response was empty"
        )

        vm = list_vms[0]
        self.assertEqual(
            vm.id,
            self.virtual_machine.id,
            "Virtual Machine ids do not match"
        )
        self.assertEqual(
            vm.name,
            self.virtual_machine.name,
            "Virtual Machine names do not match"
        )
        self.assertEqual(
            vm.state,
            "Running",
            msg="VM is not in Running state"
        )
        if vgput != "nonvgpuoffering":
            self.assertEqual(
                vm.vgpu,
                vgput,
                msg="Failed to deploy VM with vGPU card"
            )
        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def delete_vgpu_service_offering(self, serviceoffering):
        """
        delete vGPU Service Offering
        """
        serviceoffering.delete(self.apiclient)

        list_service_response = list_service_offering(
            self.apiclient,
            id=serviceoffering.id
        )

        self.assertEqual(
            list_service_response,
            None,
            "Check if service offering exists in listServiceOfferings"
        )
        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def check_for_vm(self, vgpucard, vgpuofferingid, vmid):
        list_vm_response = list_virtual_machines(
            self.apiclient,
            id=vmid
        )

        if isinstance(list_vm_response, list):
            vm = list_vm_response[0]
            if vm.state == 'Running':
                self.debug("VM state: %s" % vm.state)
            else:
                self.fail("Failed to start VM (ID: %s)" % vm.id)
            if vm.serviceofferingid == vgpuofferingid:
                self.debug(
                    "VM service offering id : %s" %
                    vm.serviceofferingid)
            else:
                self.fail("Service Offering is not matching " % vm.id)
            if vgpucard != "nonvgpuoffering":
                if vm.vgpu == vgpucard:
                    self.debug("VM vGPU card is : %s" % vm.vgpu)
                else:
                    self.fail(
                        "Failed to start VM (ID: %s) with %s vGPU card " %
                        (vm.id, vgpucard))
        return(list_vm_response[0])

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def destroy_vm(self):
        """Destroy Virtual Machine
        """

        self.virtual_machine.delete(self.apiclient)

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
            len(list_vm_response),
            0,
            "Check VM avaliable in List Virtual Machines"
        )

        self.assertEqual(
            list_vm_response[0].state,
            "Destroyed",
            "Check virtual machine is in destroyed state"
        )
        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def stop_vm(self):
        """Stop Virtual Machine
        """

        try:
            self.virtual_machine.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop VM: %s" % e)
        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def start_vm(self):
        """Start Virtual Machine
        """

        self.debug("Starting VM - ID: %s" % self.virtual_machine.id)
        self.virtual_machine.start(self.apiclient)

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
            len(list_vm_response),
            0,
            "Check VM available in List Virtual Machines"
        )

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % self.virtual_machine.id
        )
        self.assertEqual(
            list_vm_response[0].state,
            "Running",
            "Check virtual machine is in running state"
        )
        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def serviceoffering_upgrade(
            self,
            sourcetype,
            sourcemodel,
            desttype,
            destmodel):

        self.sourcevgpuoffering = self.vgpu_serviceoffering_creation(
            sourcetype,
            sourcemodel)
        self.deploy_vGPU_windows_vm(self.sourcevgpuoffering.id, sourcetype)
        time.sleep(self.testdata["vgpu"]["sleep"])

        vm = self.check_for_vm(
            sourcetype,
            self.sourcevgpuoffering.id,
            self.virtual_machine.id)
        self.check_for_vGPU_resource(
            vm.hostid,
            vm.instancename,
            vm.serviceofferingid,
            vm.vgpu)

        self.stop_vm()

        self.destvgpuoffering = self.vgpu_serviceoffering_creation(
            desttype,
            destmodel)
        cmd = changeServiceForVirtualMachine.changeServiceForVirtualMachineCmd()
        cmd.id = vm.id
        cmd.serviceofferingid = self.destvgpuoffering.id
        self.apiclient.changeServiceForVirtualMachine(cmd)

        self.debug("Starting VM - ID: %s" % vm.id)
        self.start_vm()
        time.sleep(self.testdata["vgpu"]["sleep"])

        # Ensure that VM is in running state
        vm = self.check_for_vm(desttype, self.destvgpuoffering.id, vm.id)
        self.check_for_vGPU_resource(
            vm.hostid,
            vm.instancename,
            vm.serviceofferingid,
            vm.vgpu)
        self.delete_vgpu_service_offering(self.destvgpuoffering)
        self.delete_vgpu_service_offering(self.sourcevgpuoffering)
        self.destroy_vm()
        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def vm_snapshot_serviceoffering_upgrade(
            self,
            sourcetype,
            sourcemodel,
            desttype,
            destmodel):

        self.sourcevgpuoffering = self.vgpu_serviceoffering_creation(
            sourcetype,
            sourcemodel)
        self.deploy_vGPU_windows_vm(self.sourcevgpuoffering.id, sourcetype)
        time.sleep(self.testdata["vgpu"]["sleep"])

        vm = self.check_for_vm(
            sourcetype,
            self.sourcevgpuoffering.id,
            self.virtual_machine.id)
        self.check_for_vGPU_resource(
            vm.hostid,
            vm.instancename,
            vm.serviceofferingid,
            vm.vgpu)

        self.stop_vm()

        self.destvgpuoffering = self.vgpu_serviceoffering_creation(
            desttype,
            destmodel)
        cmd = changeServiceForVirtualMachine.changeServiceForVirtualMachineCmd()
        cmd.id = vm.id
        cmd.serviceofferingid = self.destvgpuoffering.id
        self.apiclient.changeServiceForVirtualMachine(cmd)

        self.debug("Starting VM - ID: %s" % vm.id)
        self.start_vm()
        time.sleep(self.testdata["vgpu"]["sleep"])

        # Ensure that VM is in running state
        vm = self.check_for_vm(desttype, self.destvgpuoffering.id, vm.id)
        self.check_for_vGPU_resource(
            vm.hostid,
            vm.instancename,
            vm.serviceofferingid,
            vm.vgpu)
        self.delete_vgpu_service_offering(self.destvgpuoffering)
        self.delete_vgpu_service_offering(self.sourcevgpuoffering)
        self.destroy_vm()
        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def deploy_vm(self, type, model):

        self.vgpuoffering = self.vgpu_serviceoffering_creation(type, model)
        if self.vgpuoffering is not None:
            self.deploy_vGPU_windows_vm(self.vgpuoffering.id, type)
            time.sleep(self.testdata["vgpu"]["sleep"])
            vm = self.check_for_vm(
                type,
                self.vgpuoffering.id,
                self.virtual_machine.id)
            self.check_for_vGPU_resource(
                vm.hostid,
                vm.instancename,
                vm.serviceofferingid,
                vm.vgpu)
            self.destroy_vm()
            self.delete_vgpu_service_offering(self.vgpuoffering)
        return

    def new_template_register(self, guestostype):

        template1 = get_windows_template(
            self.apiclient,
            self.zone.id,
            ostype_desc=guestostype)

        if template1 == FAILED:
            if "http://pleaseupdateURL/dummy.vhd" in self.testdata[
                    "vgpu"][guestostype]["url"]:
                raise unittest.SkipTest(
                    "Check Test Data file if it has the valid template URL")
            template1 = Template.register(
                self.apiclient,
                self.testdata["vgpu"][guestostype],
                hypervisor="XenServer",
                zoneid=self.zone.id,
                domainid=self.account.domainid,
                account=self.account.name
            )
            timeout = self.testdata["vgpu"]["timeout"]

            while True:
                time.sleep(self.testdata["vgpu"]["sleep"])
                list_template_response = Template.list(
                    self.apiclient,
                    templatefilter=self.testdata["templatefilter"],
                    id=template1.id
                )
                if (isinstance(list_template_response, list)) is not True:
                    raise unittest.SkipTest(
                        "Check list template api response returns a valid list")

                if len(list_template_response) is None:
                    raise unittest.SkipTest(
                        "Check template registered is in List Templates")

                template_response = list_template_response[0]
                if template_response.isready:
                    break
                if timeout == 0:
                    raise unittest.SkipTest(
                        "Failed to download template(ID: %s)" %
                        template_response.id)

                timeout = timeout - 1
        return(template1.id)

    def deploy_vm_lifecycle(self):
        """
        Create Service Offerings for K1, K2 and M60 cards to be used for VM life cycle tests
        """

        if(self.k1hosts != 0):
            if(self.k100gpuhosts != 0):
                gtype = "GRID K100"
            elif(self.k120qgpuhosts != 0):
                gtype = "GRID K120Q"
            elif(self.k140qgpuhosts != 0):
                gtype = "GRID K140Q"
            elif(self.k160qgpuhosts != 0):
                gtype = "GRID K160Q"
            elif(self.k180qgpuhosts != 0):
                gtype = "GRID K180Q"
            else:
                gtype = "passthrough"

            self.testdata["vgpu"]["service_offerings"][gtype]["serviceofferingdetails"] = [
                {'pciDevice': 'Group of NVIDIA Corporation GK107GL [GRID K1] GPUs'}, {'vgpuType': gtype}]
            try:
                self.__class__.k100_vgpu_service_offering = ServiceOffering.create(
                    self.apiclient,
                    self.testdata["vgpu"]["service_offerings"][gtype]
                )
            except Exception as e:
                self.fail("Failed to create the service offering, %s" % e)

        if(self.k2hosts != 0):
            if(self.k200gpuhosts != 0):
                gtype = "GRID K200"
            elif(self.k220qgpuhosts != 0):
                gtype = "GRID K220Q"
            elif(self.k240qgpuhosts != 0):
                gtype = "GRID K240Q"
            elif(self.k260qgpuhosts != 0):
                gtype = "GRID K260Q"
            elif(self.k280qgpuhosts != 0):
                gtype = "GRID K280Q"
            else:
                gtype = "passthrough"

            self.testdata["vgpu"]["service_offerings"][gtype]["serviceofferingdetails"] = [
                {'pciDevice': 'Group of NVIDIA Corporation GK104GL [GRID K2] GPUs'}, {'vgpuType': gtype}]
            try:
                self.__class__.k200_vgpu_service_offering = ServiceOffering.create(
                    self.apiclient,
                    self.testdata["vgpu"]["service_offerings"][gtype]
                )
            except Exception as e:
                self.fail("Failed to create the service offering, %s" % e)

        if(self.m60hosts != 0):
            if(self.m60_0qgpuhosts != 0):
                gtype = "Tesla M60-0Q"
            elif(self.m60_1qgpuhosts != 0):
                gtype = "Tesla M60-1Q"
            elif(self.m60_2qgpuhosts != 0):
                gtype = "Tesla M60-2Q"
            elif(self.m60_4qgpuhosts != 0):
                gtype = "Tesla M60-4Q"
            elif(self.m60_8qgpuhosts != 0):
                gtype = "Tesla M60-8Q"
            elif(self.m60_0bgpuhosts != 0):
                gtype = "Tesla M60-0B"
            elif(self.m60_1bgpuhosts != 0):
                gtype = "Tesla M60-1B"
            elif(self.m60_1agpuhosts != 0):
                gtype = "Tesla M60-1A"
            elif(self.m60_2agpuhosts != 0):
                gtype = "Tesla M60-2A"
            elif(self.m60_4agpuhosts != 0):
                gtype = "Tesla M60-4A"
            elif(self.m60_8agpuhosts != 0):
                gtype = "Tesla M60-8A"
            else:
                gtype = "passthrough"

            self.testdata["vgpu"]["service_offerings"][gtype]["serviceofferingdetails"] = [
                {'pciDevice': 'Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs'}, {'vgpuType': gtype}]
            try:
                self.__class__.m60_vgpu_service_offering = ServiceOffering.create(
                    self.apiclient,
                    self.testdata["vgpu"]["service_offerings"][gtype]
                )
            except Exception as e:
                self.fail("Failed to create the service offering, %s" % e)

        win8templateid = self.new_template_register("Windows 8 (64-bit)")
        win2012templateid = self.new_template_register(
            "Windows Server 2012 (64-bit)")
        win7templateid = self.new_template_register("Windows 7 (64-bit)")

        self.__class__.nonvgpu_service_offering = ServiceOffering.create(
            self.apiclient,
            self.testdata["vgpu"]["service_offerings"]["nonvgpuoffering"]
        )

        """
        Create Virtual Machines for K1, K2 and M60 cards to be used for VM life cycle tests
       """

        if(self.k1hosts != 0):
            self.__class__.vm_k1_card = VirtualMachine.create(
                self.apiclient,
                self.testdata["virtual_machine"],
                accountid=self.account.name,
                zoneid=self.zone.id,
                domainid=self.account.domainid,
                serviceofferingid=self.k100_vgpu_service_offering.id,
                templateid=win8templateid
            )

        if(self.k2hosts != 0):
            self.__class__.vm_k2_card = VirtualMachine.create(
                self.apiclient,
                self.testdata["virtual_machine"],
                accountid=self.account.name,
                zoneid=self.zone.id,
                domainid=self.account.domainid,
                serviceofferingid=self.k200_vgpu_service_offering.id,
                templateid=win2012templateid
            )
        if(self.k2hosts != 0):
            self.__class__.vm2_k2_card = VirtualMachine.create(
                self.apiclient,
                self.testdata["virtual_machine"],
                accountid=self.account.name,
                zoneid=self.zone.id,
                domainid=self.account.domainid,
                serviceofferingid=self.k200_vgpu_service_offering.id,
                templateid=win7templateid
            )

        if(self.m60hosts != 0):
            self.__class__.vm_m60_card = VirtualMachine.create(
                self.apiclient,
                self.testdata["virtual_machine"],
                accountid=self.account.name,
                zoneid=self.zone.id,
                domainid=self.account.domainid,
                serviceofferingid=self.m60_vgpu_service_offering.id,
                templateid=win8templateid
            )

        self.__class__.nonvgpu = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            serviceofferingid=self.__class__.nonvgpu_service_offering.id,
            templateid=win7templateid
        )

        return

    def check_gpu_resources_released_vm(
            self,
            gpuhostid,
            vm_vgpu_type,
            rcapacity):
        hhosts = list_hosts(
            self.apiclient,
            hypervisor="XenServer",
            id=gpuhostid
        )
        self.assertEqual(
            isinstance(hhosts, list),
            True,
            "Check list hosts response returns a valid list"
        )

        self.assertNotEqual(
            len(hhosts),
            0,
            "Check Host details are available in List Hosts"
        )
        for ggroup in hhosts:
            if ggroup.ipaddress not in self.nongpuhosts:
                for gp in ggroup.gpugroup:
                    # if gp.gpugroupname == "Group of NVIDIA Corporation
                    # GK104GL [GRID K2] GPUs":
                    for gptype in gp.vgpu:
                        if gptype.vgputype == vm_vgpu_type:
                            self.debug(
                                "Latest remainingcapacity is %s and before remainingcapacity is %s" %
                                (gptype.remainingcapacity, rcapacity))
                            if gptype.remainingcapacity != rcapacity + 1:
                                self.fail(
                                    "Host capacity is not updated .GPU resources should be released when VM is stopped/Destroyed ")
        return

    def check_vm_state(self, vmid):
        list_vm_response = list_virtual_machines(
            self.apiclient,
            id=vmid
        )

        if list_vm_response is None:
            return("Expunge")
        return(list_vm_response[0].state)

    def check_host_vgpu_remaining_capacity(self, gpuhostid, gtype):
        gputhosts = list_hosts(
            self.apiclient,
            hypervisor="XenServer",
            id=gpuhostid
        )
        vgpucapacity = 0
        for ghost in gputhosts:
            if ghost.gpugroup is not None:
                for gp in ghost.gpugroup:
                    # if gp.gpugroupname == gpucard:
                    for gptype in gp.vgpu:
                        if gptype.vgputype == gtype:
                            vgpucapacity = vgpucapacity + \
                                gptype.remainingcapacity

        return(vgpucapacity)

    def verify_vm(self, vm_gpu_card):
        if(vm_gpu_card):
            vm_gpu_card.getState(
                self.apiclient,
                "Running")
            time.sleep(self.testdata["vgpu"]["sleep"] * 3)
            self.check_for_vGPU_resource(
                vm_gpu_card.hostid,
                vm_gpu_card.instancename,
                vm_gpu_card.serviceofferingid,
                vm_gpu_card.vgpu)

    def stop_life_cycle_vm(self, vm_gpu_card):

        if(vm_gpu_card):
            vm_gpu_card.stop(self.apiclient)
            time.sleep(self.testdata["vgpu"]["sleep"])
            vm_gpu_card.getState(
                self.apiclient,
                "Stopped")

    def start_life_cycle_vm(self, vm_gpu_card):

        if(vm_gpu_card):
            vm_gpu_card.start(self.apiclient)
            time.sleep(self.testdata["vgpu"]["sleep"])
            vm_gpu_card.getState(
                self.apiclient,
                "Running")

    def restore_life_cycle_vm(self, vm_gpu_card):
        if(vm_gpu_card):
            vm_gpu_card.restore(self.apiclient)
            time.sleep(self.testdata["vgpu"]["sleep"])

    def reboot_life_cycle_vm(self, vm_gpu_card):

        if(vm_gpu_card):
            vm_gpu_card.reboot(self.apiclient)
            time.sleep(self.testdata["vgpu"]["sleep"])

    def delete_vm_life_cycle_vm(self, vm_gpu_card):
        if(vm_gpu_card):
            vm_gpu_card.delete(self.apiclient)
            time.sleep(self.testdata["vgpu"]["sleep"])
            vm_gpu_card.getState(
                self.apiclient,
                "Destroyed")

    def recover_vm_life_cycle_vm(self, vm_gpu_card):
        if(vm_gpu_card):
            vm_gpu_card.recover(self.apiclient)
            vm_gpu_card.getState(
                self.apiclient,
                "Stopped")

    def recovervm(self, vm_gpu_card):
        if self.check_vm_state(vm_gpu_card.id) == "Expunge":
            raise unittest.SkipTest("VM is already deleted hence skipping")
        self.recover_vm_life_cycle_vm(vm_gpu_card)
        self.start_life_cycle_vm(vm_gpu_card)
        self.verify_vm(vm_gpu_card)
        return

    def startvm(self, vm_gpu_card):

        self.start_life_cycle_vm(vm_gpu_card)
        self.verify_vm(vm_gpu_card)
        return

    def stopvm(self, vm_gpu_card):

        rcapacity = self.check_host_vgpu_remaining_capacity(
            vm_gpu_card.hostid,
            vm_gpu_card.vgpu)
        self.stop_life_cycle_vm(vm_gpu_card)
        time.sleep(self.testdata["vgpu"]["sleep"] * 3)
        self.check_gpu_resources_released_vm(
            vm_gpu_card.hostid,
            vm_gpu_card.vgpu,
            rcapacity)
        return

    def deletevm(self, vm_gpu_card):

        rcapacity = self.check_host_vgpu_remaining_capacity(
            vm_gpu_card.hostid,
            vm_gpu_card.vgpu)
        hostid = vm_gpu_card.hostid
        vgputype = vm_gpu_card.vgpu
        self.delete_vm_life_cycle_vm(vm_gpu_card)
        time.sleep(self.testdata["vgpu"]["sleep"] * 3)
        self.check_gpu_resources_released_vm(hostid, vgputype, rcapacity)
        return

    def restorevm(self, vm_gpu_card):

        self.restore_life_cycle_vm(vm_gpu_card)
        self.verify_vm(vm_gpu_card)
        return

    def create_vm_snapshot(self, vmcard):

        self.debug("Check if deployed VMs are in running state?")
        if(vmcard):
            vmcard.getState(
                self.apiclient,
                "Running")

        if hasattr(vmcard, "vgpu"):
            self.check_for_vGPU_resource(
                vmcard.hostid,
                vmcard.instancename,
                vmcard.serviceofferingid,
                vmcard.vgpu)

        VmSnapshot.create(
            self.apiclient,
            vmcard.id,
            "false",
            "TestSnapshot",
            "Display Text"
        )

        list_snapshot_response = VmSnapshot.list(
            self.apiclient,
            virtualmachineid=vmcard.id,
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

        vgpu_host = list_hosts(
            self.apiclient,
            id=vmcard.hostid
        )
        ssh_client = SshClient(
            host=vgpu_host[0].ipaddress,
            port=self.testdata['configurableData']['host']["publicport"],
            user=self.testdata['configurableData']['host']["username"],
            passwd=self.testdata['configurableData']['host']["password"])
        """
        Get vGPU type model
        """
        vgpu_snapshot = ssh_client.execute(
            "xe snapshot-list name-label=" +
            list_snapshot_response[0].name +
            " --minimal")
        #vgpu_snapshot = ssh_client.execute("xe snapshot-param-get param-name=name-label uuid="+vgpu_snapshot_uuid[0]+ " params=type-model-name --minimal")

#        self.debug("vgpu type model is %s and value is %s and length is %s"%(vgpu_type_model,vgpu_type_model[0],len(vgpu_type_model[0])))

        self.assertNotEqual(
            len(vgpu_snapshot[0]),
            0,
            "VM Snapshot is not created"
        )
        self.__class__.vmsnapwomemory = 1
        return

    def revert_vm_snapshot(self, vmcard):

        self.debug("Check if deployed VMs are in running state?")
        # if(vmcard):
        #  vmcard.getState(
        #                         self.apiclient,
        #                         "Running")
        if hasattr(vmcard, "vgpu"):
            self.check_for_vGPU_resource(
                vmcard.hostid,
                vmcard.instancename,
                vmcard.serviceofferingid,
                vmcard.vgpu)

        list_snapshot_response = VmSnapshot.list(
            self.apiclient,
            virtualmachineid=vmcard.id,
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

        VmSnapshot.revertToSnapshot(
            self.apiclient,
            list_snapshot_response[0].id)
        time.sleep(self.testdata["vgpu"]["sleep"])
        list_vm_response = list_virtual_machines(
            self.apiclient,
            id=vmcard.id
        )
        vm = list_vm_response[0]
        self.assertEqual(
            list_vm_response[0].state,
            "Stopped",
            "Check the state of vm is Stopped"
        )

        cmd = startVirtualMachine.startVirtualMachineCmd()
        cmd.id = vm.id
        self.apiclient.startVirtualMachine(cmd)
        time.sleep(self.testdata["vgpu"]["sleep"])

        list_vm_response = list_virtual_machines(
            self.apiclient,
            id=vm.id
        )
        vm = list_vm_response[0]
        if vm is None:
            self.fail("Failed to list VM details after Vm Snapshot")
        self.assertEqual(
            vm.state,
            "Running",
            "Check the state of vm is Running"
        )

        if hasattr(vmcard, "vgpu"):
            self.check_for_vGPU_resource(
                vmcard.hostid,
                vmcard.instancename,
                vmcard.serviceofferingid,
                vmcard.vgpu)

    def delete_vm_snapshot(self, vmcard):

        list_snapshot_response = VmSnapshot.list(
            self.apiclient,
            virtualmachineid=vmcard.id,
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
        Tobedeletedsnapname = list_snapshot_response[0].name
        VmSnapshot.deleteVMSnapshot(
            self.apiclient,
            list_snapshot_response[0].id)

        time.sleep(self.testdata["vgpu"]["sleep"])

        list_snapshot_response = VmSnapshot.list(
            self.apiclient,
            virtualmachineid=vmcard.id,
            listall=True)
        self.debug(
            "List Snapshot response after snapshot deletion %s" %
            list_snapshot_response)
        self.assertEqual(
            list_snapshot_response,
            None,
            "Check if vm snapshot is deleted"
        )

        vgpu_host = list_hosts(
            self.apiclient,
            id=vmcard.hostid
        )
        ssh_client = SshClient(
            host=vgpu_host[0].ipaddress,
            port=self.testdata['configurableData']['host']["publicport"],
            user=self.testdata['configurableData']['host']["username"],
            passwd=self.testdata['configurableData']['host']["password"])

        vgpu_snapshot = ssh_client.execute(
            "xe snapshot-list name-label=" +
            Tobedeletedsnapname +
            " --minimal")
        #vgpu_snapshot = ssh_client.execute("xe snapshot-param-get param-name=name-label uuid="+vgpu_snapshot_uuid[0]+ " params=type-model-name --minimal")

#        self.debug("vgpu type model is %s and value is %s and length is %s"%(vgpu_type_model,vgpu_type_model[0],len(vgpu_type_model[0])))
        self.debug(
            "List Snapshot response after snapshot deletion is %s %s" %
            (vgpu_snapshot, vgpu_snapshot[0]))
        self.assertEqual(
            len(vgpu_snapshot[0]),
            0,
            "check if VM Snapshot is not deleted"
        )

        return

    def create_vm_snapshot_with_memory(self, vmcard):

        self.debug("Check if deployed VMs are in running state?")
        if(vmcard):
            vmcard.getState(
                self.apiclient,
                "Running")
            self.check_for_vGPU_resource(
                vmcard.hostid,
                vmcard.instancename,
                vmcard.serviceofferingid,
                vmcard.vgpu)

        VmSnapshot.create(
            self.apiclient,
            vmcard.id,
            "true",
            "TestSnapshotwithmemory",
            "Display Text"
        )

        time.sleep(self.testdata["vgpu"]["sleep"] * 3)

        list_snapshot_response = VmSnapshot.list(
            self.apiclient,
            virtualmachineid=vmcard.id,
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

        vgpu_host = list_hosts(
            self.apiclient,
            id=vmcard.hostid
        )
        ssh_client = SshClient(
            host=vgpu_host[0].ipaddress,
            port=self.testdata['configurableData']['host']["publicport"],
            user=self.testdata['configurableData']['host']["username"],
            passwd=self.testdata['configurableData']['host']["password"])

        vgpu_snapshot = ssh_client.execute(
            "xe snapshot-list name-label=" +
            list_snapshot_response[0].name +
            " --minimal")
        #vgpu_snapshot = ssh_client.execute("xe snapshot-param-get param-name=name-label uuid="+vgpu_snapshot_uuid[0]+ " params=type-model-name --minimal")

#        self.debug("vgpu type model is %s and value is %s and length is %s"%(vgpu_type_model,vgpu_type_model[0],len(vgpu_type_model[0])))

        self.assertNotEqual(
            len(vgpu_snapshot[0]),
            0,
            "VM Snapshot is not created"
        )
        self.__class__.vmsnapwithmemory = 1
        return

    def revert_vm_snapshot_with_memory(self, vmcard):

        self.debug("Check if deployed VMs are in running state?")
        if(vmcard):
            vmcard.getState(
                self.apiclient,
                "Running")
            self.check_for_vGPU_resource(
                vmcard.hostid,
                vmcard.instancename,
                vmcard.serviceofferingid,
                vmcard.vgpu)

        list_snapshot_response = VmSnapshot.list(
            self.apiclient,
            virtualmachineid=vmcard.id,
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

        VmSnapshot.revertToSnapshot(
            self.apiclient,
            list_snapshot_response[0].id)

        list_vm_response = list_virtual_machines(
            self.apiclient,
            id=vmcard.id
        )
        vm = list_vm_response[0]
        if vm is None:
            self.fail("Failed to list VM details after Vm Snapshot")
        self.assertEqual(
            vm.state,
            "Running",
            "Check the state of vm is Running"
        )

        self.check_for_vGPU_resource(
            vmcard.hostid,
            vmcard.instancename,
            vmcard.serviceofferingid,
            vmcard.vgpu)

        return

    def rebootvm(self, vm_vgpu_card):

        self.reboot_life_cycle_vm(vm_vgpu_card)
        self.verify_vm(vm_vgpu_card)
        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def vm_snapshot_vgpu(
            self,
            vmcard,
            sourcetype,
            sourcemodel,
            desttype,
            destmodel):

        self.create_vm_snapshot(vmcard)
        self.start_life_cycle_vm(vmcard)
        self.destvgpuoffering = self.vgpu_serviceoffering_creation(
            desttype,
            destmodel)
        self.stop_life_cycle_vm(vmcard)
        cmd = changeServiceForVirtualMachine.changeServiceForVirtualMachineCmd()
        cmd.id = vmcard.id
        cmd.serviceofferingid = self.destvgpuoffering.id
        self.apiclient.changeServiceForVirtualMachine(cmd)

        self.debug("Starting VM - ID: %s" % vmcard.id)
        self.start_life_cycle_vm(vmcard)
        time.sleep(self.testdata["vgpu"]["sleep"])
        vm = self.check_for_vm(desttype, self.destvgpuoffering.id, vmcard.id)
        # Ensure that VM is in running state
        if destmodel != "None":
            self.check_for_vGPU_resource(
                vm.hostid,
                vm.instancename,
                vm.serviceofferingid,
                vm.vgpu)

        self.revert_vm_snapshot(vm)

        self.delete_vm_snapshot(vm)

    def test_01_list_vgpu_host_details(self):
        """   list vGPU host details  """
        hhosts = list_hosts(
            self.apiclient,
            hypervisor="XenServer",
        )
        self.assertEqual(
            isinstance(hhosts, list),
            True,
            "Check list hosts response returns a valid list"
        )

        self.assertNotEqual(
            len(hhosts),
            0,
            "Check Host details are available in List Hosts"
        )

        k1pass = 0
        k100 = 0
        k120q = 0
        k140q = 0
        k160q = 0
        k180q = 0

        k2pass = 0
        k200 = 0
        k220q = 0
        k240q = 0
        k260q = 0
        k280q = 0

        m60pass = 0
        m60_0q = 0
        m60_1q = 0
        m60_2q = 0
        m60_4q = 0
        m60_8q = 0
        m60_0b = 0
        m60_1b = 0
        m60_1a = 0
        m60_2a = 0
        m60_4a = 0
        m60_8a = 0

        for ggroup in hhosts:
            if ggroup.ipaddress not in self.nongpuhosts:
                for gp in ggroup.gpugroup:
                    if gp.gpugroupname == "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs":
                        for gptype in gp.vgpu:
                            if gptype.vgputype == "passthrough":
                                k1pass = k1pass + 1
                            if gptype.vgputype == "GRID K100":
                                k100 = k100 + 1
                            if gptype.vgputype == "GRID K120Q":
                                k120q = k120q + 1
                            if gptype.vgputype == "GRID K140Q":
                                k140q = k140q + 1
                            if gptype.vgputype == "GRID K160Q":
                                k160q = k160q + 1
                            if gptype.vgputype == "GRID K180Q":
                                k180q = k180q + 1
                    if gp.gpugroupname == "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs":
                        for gptype in gp.vgpu:
                            if gptype.vgputype != "passthrough":
                                k2pass = k2pass + 1
                            if gptype.vgputype == "GRID K200":
                                k200 = k200 + 1
                            if gptype.vgputype == "GRID K220Q":
                                k220q = k220q + 1
                            if gptype.vgputype == "GRID K240Q":
                                k240q = k240q + 1
                            if gptype.vgputype == "GRID K260Q":
                                k260q = k260q + 1
                            if gptype.vgputype == "GRID K280Q":
                                k280q = k280q + 1
                    if gp.gpugroupname == "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs":
                        for gptype in gp.vgpu:
                            if gptype.vgputype != "passthrough":
                                m60pass = m60pass + 1
                            if gptype.vgputype == "Tesla M60-0Q":
                                m60_0q = m60_0q + 1
                            if gptype.vgputype == "Tesla M60-1Q":
                                m60_1q = m60_1q + 1
                            if gptype.vgputype == "Tesla M60-2Q":
                                m60_2q = m60_2q + 1
                            if gptype.vgputype == "Tesla M60-4Q":
                                m60_4q = m60_4q + 1
                            if gptype.vgputype == "Tesla M60-8Q":
                                m60_8q = m60_8q + 1
                            if gptype.vgputype != "Tesla M60-0B":
                                m60_0b = m60_0b + 1
                            if gptype.vgputype == "Tesla M60-1B":
                                m60_1b = m60_1b + 1
                            if gptype.vgputype == "Tesla M60-1A":
                                m60_1a = m60_1a + 1
                            if gptype.vgputype == "Tesla M60-2A":
                                m60_2a = m60_2a + 1
                            if gptype.vgputype == "Tesla M60-4A":
                                m60_4a = m60_4a + 1
                            if gptype.vgputype == "Tesla M60-8A":
                                m60_8a = m60_8a + 1
            else:
                self.debug("This is nongpuhost:%s" % (ggroup.ipaddress))
        if self.k1passthroughgpuhosts > 0:
            if not k1pass:
                self.fail(
                    "list host details with K1 Passthrough vgpu are not correct")
        if self.k100gpuhosts > 0:
            if not k100:
                self.fail("list host details with K100 vgpu are not correct")
        if self.k120qgpuhosts > 0:
            if not k120q:
                self.fail("list host details with K120Q vgpu are not correct")
        if self.k140qgpuhosts > 0:
            if not k140q:
                self.fail("list host details with K140Q vgpu are not correct")
        if self.k160qgpuhosts > 0:
            if not k160q:
                self.fail("list host details with K160Q vgpu are not correct")
        if self.k180qgpuhosts > 0:
            if not k180q:
                self.fail("list host details with K180Q vgpu are not correct")

        if self.k2passthroughgpuhosts > 0:
            if not k2pass:
                self.fail(
                    "list host details with K2 passthrough vgpu are not correct")
        if self.k200gpuhosts > 0:
            if not k200:
                self.fail("list host details with K200 vgpu are not correct")
        if self.k220qgpuhosts > 0:
            if not k220q:
                self.fail("list host details with K220Q vgpu are not correct")
        if self.k240qgpuhosts > 0:
            if not k240q:
                self.fail("list host details with K240Q vgpu are not correct")
        if self.k260qgpuhosts > 0:
            if not k260q:
                self.fail("list host details with K260Q vgpu are not correct")
        if self.k280qgpuhosts > 0:
            if not k280q:
                self.fail("list host details with K280Q vgpu are not correct")

        if self.m60passthroughgpuhosts > 0:
            if not m60pass:
                self.fail(
                    "list host details with M60 passthrough vgpu are not correct")
        if self.m60_0qgpuhosts > 0:
            if not m60_0q:
                self.fail("list host details with M60-0Q vgpu are not correct")
        if self.m60_1qgpuhosts > 0:
            if not m60_1q:
                self.fail("list host details with M60-1Q vgpu are not correct")
        if self.m60_2qgpuhosts > 0:
            if not m60_2q:
                self.fail("list host details with M60-2Q vgpu are not correct")
        if self.m60_4qgpuhosts > 0:
            if not m60_4q:
                self.fail("list host details with M60-4Q vgpu are not correct")
        if self.m60_8qgpuhosts > 0:
            if not m60_8q:
                self.fail("list host details with M60-8Q vgpu are not correct")
        if self.m60_0bgpuhosts > 0:
            if not m60_0b:
                self.fail("list host details with M60-0B vgpu are not correct")
        if self.m60_1bgpuhosts > 0:
            if not m60_1b:
                self.fail("list host details with M60-1B vgpu are not correct")
        if self.m60_1agpuhosts > 0:
            if not m60_1a:
                self.fail("list host details with M60-1A vgpu are not correct")
        if self.m60_2agpuhosts > 0:
            if not m60_2a:
                self.fail("list host details with M60-2A vgpu are not correct")
        if self.m60_4agpuhosts > 0:
            if not m60_4a:
                self.fail("list host details with M60-4A vgpu are not correct")
        if self.m60_8agpuhosts > 0:
            if not m60_8a:
                self.fail("list host details with M60-8A vgpu are not correct")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_02_create_deploy_windows_vm_with_k100_vgpu_service_offering(self):
        """Test to create and deploy vm with K100 vGPU service offering"""

        k100capacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs",
            "GRID K100")

        if (self.k100gpuhosts == 0) or (k100capacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with K100 vGPU Drivers installed")

        self.deploy_vm(
            "GRID K100",
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_03_create_deploy_windows_vm_with_k120q_vgpu_service_offering(
            self):
        """Test to create and deploy vm with K120Q vGPU service offering"""

        k120qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs",
            "GRID K120Q")

        if (self.k120qgpuhosts == 0) or (k120qcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with K120Q vGPU Drivers installed")

        self.deploy_vm(
            "GRID K120Q",
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_04_create_deploy_windows_vm_with_k140q_vgpu_service_offering(
            self):
        """Test to create and deploy vm with K140Q vGPU service offering"""

        k140qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs",
            "GRID K140Q")

        if (self.k140qgpuhosts == 0) or (k140qcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with K140Q vGPU Drivers installed")

        self.deploy_vm(
            "GRID K140Q",
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_05_create_deploy_windows_vm_with_k1_passthrough_vgpu_service_offering(
            self):
        """Test to create and deploy vm with K1 passthrough vGPU service offering"""

        k1passcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs",
            "passthrough")

        if (self.k1passthroughgpuhosts == 0) or (k1passcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with K1 passthrough installed")

        self.deploy_vm(
            "passthrough",
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_06_create_deploy_windows_vm_with_k2_passthrough_vgpu_service_offering(
            self):
        """Test to create and deploy vm with K2 pasthrough vGPU service offering"""

        k2passcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs",
            "passthrough")

        if (self.k2passthroughgpuhosts == 0) or (k2passcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with K2 passthrough installed")

        self.deploy_vm(
            "passthrough",
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_07_create_deploy_windows_vm_with_k260q_vgpu_service_offering(
            self):
        """Test to create and deploy vm with K260Q vGPU service offering"""

        k260qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs",
            "GRID K260Q")

        if (self.k260qgpuhosts == 0) or (k260qcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with K260Q vGPU Drivers installed")

        self.deploy_vm(
            "GRID K260Q",
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_08_create_deploy_windows_vm_with_k240q_vgpu_service_offering(
            self):
        """   Test to create and deploy vm with K240Q vGPU service offering """

        k240qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs",
            "GRID K240Q")

        if (self.k240qgpuhosts == 0) or (k240qcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with K240Q vGPU Drivers installed")

        self.deploy_vm(
            "GRID K240Q",
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_09_create_deploy_windows_vm_with_k220q_vgpu_service_offering(
            self):
        """  Test to create and deploy vm with K220Q vGPU service offering """

        k220qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs",
            "GRID K220Q")

        if (self.k220qgpuhosts == 0) or (k220qcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with K220Q vGPU Drivers installed")

        self.deploy_vm(
            "GRID K220Q",
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_10_create_deploy_windows_vm_with_k200_vgpu_service_offering(self):
        """   Test to create and deploy vm with K200 vGPU service offering  """

        k200capacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs",
            "GRID K200")

        if (self.k200gpuhosts == 0) or (k200capacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with K200 vGPU Drivers installed")

        self.deploy_vm(
            "GRID K200",
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_11_add_nonvgpu_host_to_vgpucluster(self):
        """   Test to add non vGPU host to an existing cluster  """
        countx = 0
        # hostuuids=[]
        huuids = []
        lhosts = list_hosts(
            self.apiclient,
            hypervisor="XenServer"
        )

        sshClient1 = SshClient(
            host=lhosts[0].ipaddress,
            port=self.testdata['configurableData']['host']["publicport"],
            user=self.testdata['configurableData']['host']["username"],
            passwd=self.testdata['configurableData']['host']["password"])
        totalxenhosts = len(sshClient1.execute("xe host-list | grep uuid"))

        for hostlist in lhosts:
            countx = countx + 1

        if totalxenhosts <= countx:
            raise unittest.SkipTest(
                "No additional Host is available in the XS pool to add host")

        huuids.append(sshClient1.execute("xe host-list --minimal"))
        self.debug("host uuids are:%s" % huuids)
        hostuuids = huuids[0][0].split(',')
        addhost = "FAIL"

        for hid in hostuuids:
            self.debug("host id: %s" % (hid))
            haddress = sshClient1.execute(
                "xe host-param-get param-name=address --minimal uuid=" +
                hid)
            self.debug("host address is %s" % (haddress[0]))
            if haddress[0] == self.testdata["vgpu"]["nongpu_host_ip"]:
                addhost = "PASS"
                break

        if addhost != "PASS":
            raise unittest.SkipTest(
                "XS Pool new host ip is not matching with test data host ip. Skipping the test.Please update test data file with hostip added to XS pool")
        list_cluster_response = list_clusters(
            self.apiclient,
            hypervisor="XenServer",
            allocationstate="Enabled"
        )
        self.assertEqual(
            isinstance(list_cluster_response, list),
            True,
            "Check list clusters response returns a valid list"
        )
        self.assertNotEqual(
            len(list_cluster_response),
            0,
            "Check list clusters response"
        )
        cluster_response = list_cluster_response[0]

        self.assertEqual(
            cluster_response.allocationstate,
            'Enabled',
            "Check whether allocation state of cluster is enabled"
        )
        self.debug("Cluster response is:%s" % (cluster_response))
        self.assertEqual(
            cluster_response.hypervisortype,
            "XenServer",
            "Check hypervisor type is XenServer or not"
        )

        nongpuhost = Host.create(
            self.apiclient,
            cluster_response,
            self.testdata["vgpu"]["hosts"]["nonvgpuxenserver"],
            zoneid=self.zone.id,
            podid=self.pod.id,
            hypervisor="XenServer"
        )
        if nongpuhost == FAILED:
            self.fail("Host Creation Failed")
        self.debug(
            "Created host (ID: %s) in cluster ID %s" % (
                nongpuhost.id,
                cluster_response.id
            ))
        listhosts = list_hosts(
            self.apiclient,
            hypervisor="XenServer",
            id=nongpuhost.id
        )
        self.assertEqual(
            isinstance(listhosts, list),
            True,
            "Check list hosts response returns a valid list"
        )

        self.assertNotEqual(
            len(listhosts),
            0,
            "Check Host details are available in List Hosts"
        )

        for ggroup in listhosts:
            if ggroup.gpugroup is not None:
                self.fail("This is not a non vGPU host")
                # Cleanup Host
        self.cleanup.append(nongpuhost)

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_12_validate_deployed_vGPU_windows_vm(self):
        """ Test deploy virtual machine
        """

        self.deploy_vm_lifecycle()

        self.debug("Check if deployed VMs are in running state?")

        if self.__class__.vm_k1_card is not None:
            self.verify_vm(self.__class__.vm_k1_card)

        if self.__class__.vm_k2_card is not None:
            self.verify_vm(self.__class__.vm_k2_card)

        if self.__class__.vm2_k2_card is not None:
            self.verify_vm(self.__class__.vm2_k2_card)

        if self.__class__.vm_m60_card is not None:
            self.verify_vm(self.__class__.vm_m60_card)

        self.__class__.vmlifecycletest = 1
        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_13_stop_vGPU_windows_vm(self):
        """ Test stop virtual machine
        """
        if self.__class__.vmlifecycletest == 0:
            raise unittest.SkipTest(
                "VM Life Cycle Deploy VM test failed hence skipping")

        if self.__class__.vm_k1_card:
            self.stopvm(self.__class__.vm_k1_card)

        if self.__class__.vm_k2_card:
            self.stopvm(self.__class__.vm_k2_card)

        if self.__class__.vm2_k2_card:
            self.stopvm(self.__class__.vm2_k2_card)

        if self.__class__.vm_m60_card:
            self.stopvm(self.__class__.vm_m60_card)

        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_14_start_vGPU_windows_vm(self):
        """ Test start virtual machine
        """
        if self.__class__.vmlifecycletest == 0:
            raise unittest.SkipTest(
                "VM Life Cycle Deploy VM test failed hence skipping")

        if self.__class__.vm_k1_card:
            self.startvm(self.__class__.vm_k1_card)

        if self.__class__.vm_k2_card:
            self.startvm(self.__class__.vm_k2_card)

        if self.__class__.vm2_k2_card:
            self.startvm(self.__class__.vm2_k2_card)

        if self.__class__.vm_m60_card:
            self.startvm(self.__class__.vm_m60_card)

        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_15_restore_vGPU_windows_vm(self):
        """Test restore Virtual Machine
        """

        if self.__class__.vmlifecycletest == 0:
            raise unittest.SkipTest(
                "VM Life Cycle Deploy VM test failed hence skipping")

        if self.__class__.vm_k1_card:
            self.restorevm(self.__class__.vm_k1_card)

        if self.__class__.vm_k2_card:
            self.restorevm(self.__class__.vm_k2_card)

        if self.__class__.vm2_k2_card:
            self.restorevm(self.__class__.vm2_k2_card)

        if self.__class__.vm_m60_card:
            self.restorevm(self.__class__.vm_m60_card)

        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_16_reboot_vGPU_windows_vm(self):
        """ Test reboot virtual machine
        """

        if self.__class__.vmlifecycletest == 0:
            raise unittest.SkipTest(
                "VM Life Cycle Deploy VM test failed hence skipping")

        if self.__class__.vm_k1_card:
            self.rebootvm(self.__class__.vm_k1_card)

        if self.__class__.vm_k2_card:
            self.rebootvm(self.__class__.vm_k2_card)

        if self.__class__.vm2_k2_card:
            self.rebootvm(self.__class__.vm2_k2_card)

        if self.__class__.vm_m60_card:
            self.rebootvm(self.__class__.vm_m60_card)

        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_17_create_k1_vm_snapshot_wo_memory(self):
        """Test to create VM snapshots
        """

        if self.__class__.vmlifecycletest == 0:
            raise unittest.SkipTest(
                "VM Life Cycle Deploy VM test failed hence skipping")

        if not (self.__class__.vm_k1_card):
            raise unittest.SkipTest(" No VM available.Hence skipping")

        self.create_vm_snapshot(self.__class__.vm_k1_card)

        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_18_revert_k1_vm_snapshot_wo_memory(self):
        """Test to revert VM snapshots
        """

        if self.__class__.vmlifecycletest == 0:
            raise unittest.SkipTest(
                "VM Life Cycle Deploy VM test failed hence skipping")

        if self.__class__.vmsnapwomemory == 0:
            raise unittest.SkipTest(
                "VM Snapshot creation test failed hence skipping")

        if not self.__class__.vm_k1_card:
            raise unittest.SkipTest("No VM available.Hence skipping")

        # Stop the VM before revert. CloudStack doesn't support reverting running VMs.
        # Assuming it would stop successfully. Else we need one parameter to track stopping of VMs.
        self.stopvm(self.__class__.vm_k1_card)

        self.revert_vm_snapshot(self.__class__.vm_k1_card)

        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_19_delete_k1_vm_snapshot_wo_memory(self):
        """Test to delete vm snapshots
        """

        if self.__class__.vmlifecycletest == 0:
            raise unittest.SkipTest(
                "VM Life Cycle Deploy VM test failed hence skipping")

        if self.__class__.vmsnapwomemory == 0:
            raise unittest.SkipTest(
                "VM Snapshot creation test failed hence skipping")

        if not (self.__class__.vm_k1_card):
            raise unittest.SkipTest("No VM available.Hence skipping")

        self.delete_vm_snapshot(self.__class__.vm_k1_card)

        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_20_create_k2_vm_snapshot_with_memory(self):
        """Test to create VM snapshots
        """

        # CloudStack doesn't support creating a VM snapshot with memory for vGPU VMs.
        # For now skipping the test, Later we can modify or remove this test.
        if self.__class__.vmlifecycletest == 1:
            raise unittest.SkipTest(
                "CloudStack does not support this operation on vGPU VMs")

        if self.__class__.vmlifecycletest == 0:
            raise unittest.SkipTest(
                "VM Life Cycle Deploy VM test failed hence skipping")

        if not (self.__class__.vm_k2_card):
            raise unittest.SkipTest("No VM available.Hence skipping")

        self.create_vm_snapshot_with_memory(self.__class__.vm_k2_card)

        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_21_revert_k2_vm_snapshot_with_memory(self):
        """Test to revert VM snapshots
        """

        # Same as above test, Later we can modify or remove this test.
        if self.__class__.vmlifecycletest == 1:
            raise unittest.SkipTest(
                "CloudStack does not support this operation on vGPU VMs")

        if self.__class__.vmlifecycletest == 0:
            raise unittest.SkipTest(
                "VM Life Cycle Deploy VM test failed hence skipping")

        # Anyway the test will always be skipped here but did it early.
        if self.__class__.vmsnapwithmemory == 0:
            raise unittest.SkipTest(
                "VM Snapshot creation with memory test failed hence skipping")

        if not (self.__class__.vm_k2_card):
            raise unittest.SkipTest("No VM available.Hence skipping")

        self.revert_vm_snapshot_with_memory(self.__class__.vm_k2_card)

        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_22_delete_k2_vm_snapshot_with_memory(self):
        """Test to delete vm snapshots
        """

        # Same as above test, Later we can modify or remove this test.
        if self.__class__.vmlifecycletest == 1:
            raise unittest.SkipTest(
                "CloudStack does not support this operation on vGPU VMs")

        if self.__class__.vmlifecycletest == 0:
            raise unittest.SkipTest(
                "VM Life Cycle Deploy VM test failed hence skipping")

        # Anyway the test will always be skipped here but did it early.
        if self.__class__.vmsnapwithmemory == 0:
            raise unittest.SkipTest(
                "VM Snapshot creation with memory test failed hence skipping")

        if not(self.__class__.vm_k2_card):
            raise unittest.SkipTest("No VM available.Hence skipping")

        self.delete_vm_snapshot(self.__class__.vm_k2_card)

        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_23_vm_snapshot_without_memory_from_k1_vgpu_nonvgpu_vgpu(self):
        """Test to verify VM snapshot from vGPU to non vgpu to vGPU snap
        """

        if self.__class__.vmlifecycletest == 0:
            raise unittest.SkipTest(
                "VM Life Cycle Deploy VM test failed hence skipping")

        k1qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs",
            self.__class__.vm_k1_card.vgpu)

        if (k1qcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with K1 vGPU Drivers installed. Skipping ")

        if not(self.__class__.vm_k1_card):
            raise unittest.SkipTest("No VM available.Hence skipping")

        self.vm_snapshot_vgpu(
            self.__class__.vm_k1_card,
            self.__class__.vm_k1_card.vgpu,
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs",
            "nonvgpuoffering",
            "None")

        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_24_vm_snapshot_without_memory_from_nonvgpu_k1vgpu_nonvgpu(self):
        """Test to verify VM snapshot from non vGPU snap to vGPU snap to non vGPU snap
        """

        if self.__class__.vmlifecycletest == 0:
            raise unittest.SkipTest(
                "VM Life Cycle Deploy VM test failed hence skipping")

        k1qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs",
            self.__class__.vm_k1_card.vgpu)

        if (k1qcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with vGPU Drivers installed. Skipping ")

        if not(self.__class__.vm_k1_card):
            raise unittest.SkipTest("No VM available.Hence skipping")

        self.vm_snapshot_vgpu(
            self.__class__.nonvgpu,
            "nonvgpuoffering",
            "None",
            self.__class__.vm_k1_card.vgpu,
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs")

        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_25_vm_snapshot_without_memory_from_k2vgpu_k1vgpu_k2vgpu(self):
        """Test to verify VM snapshot from K2 vGPU snap to K1 vGPU snap to K2 vGPU snap
        """

        if self.__class__.vmlifecycletest == 0:
            raise unittest.SkipTest(
                "VM Life Cycle Deploy VM test failed hence skipping")

        k1qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs",
            self.__class__.vm_k1_card.vgpu)

        if (k1qcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with vGPU Drivers installed. Skipping ")

        if not(self.__class__.vm_k2_card):
            raise unittest.SkipTest("No VM available.Hence skipping")

        self.vm_snapshot_vgpu(
            self.__class__.vm_k2_card,
            self.__class__.vm_k2_card.vgpu,
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs",
            self.__class__.vm_k1_card.vgpu,
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs")

        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_26_destroy_vGPU_windows_vm(self):
        """Test destroy Virtual Machine
        """

        if self.__class__.vmlifecycletest == 0:
            raise unittest.SkipTest(
                "VM Life Cycle Deploy VM test failed hence skipping")

        if self.__class__.vm_k1_card:
            self.deletevm(self.__class__.vm_k1_card)

        if self.__class__.vm_k2_card:
            self.deletevm(self.__class__.vm_k2_card)

        if self.__class__.vm2_k2_card:
            self.deletevm(self.__class__.vm2_k2_card)

        if self.__class__.vm_m60_card:
            self.deletevm(self.__class__.vm_m60_card)

        if self.__class__.nonvgpu:
            self.deletevm(self.__class__.nonvgpu)

        self.cleanup.append(self.__class__.nonvgpu_service_offerin)
        self.cleanup.append(self.__class__.k100_vgpu_service_offering)
        self.cleanup.append(self.__class__.k200_vgpu_service_offering)
        self.cleanup.append(self.__class__.m60_vgpu_service_offering)

        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_27_recover_vGPU_windows_vm(self):
        """Test recover Virtual Machine
        """

        if self.__class__.vmlifecycletest == 0:
            raise unittest.SkipTest(
                "VM Life Cycle Deploy VM test failed hence skipping")

        if self.__class__.vm_k1_card is not None:
            self.recovervm(self.__class__.vm_k1_card)

        if self.__class__.vm_k2_card is not None:
            self.recovervm(self.__class__.vm_k2_card)

        if self.__class__.vm2_k2_card is not None:
            self.recovervm(self.__class__.vm2_k2_card)

        if self.__class__.vm_m60_card is not None:
            self.recovervm(self.__class__.vm_m60_card)

        return

    def test_28_destroy_vGPU_windows_vm_after_recover(self):
        """Test destroy Virtual Machine
        """

        if self.__class__.vmlifecycletest == 0:
            raise unittest.SkipTest(
                "VM Life Cycle Deploy VM test failed hence skipping")

        if self.__class__.vm_k1_card:
            if self.check_vm_state(self.__class__.vm_k1_card.id) == "Expunge":
                raise unittest.SkipTest("VM is already deleted hence skipping")
            self.deletevm(self.__class__.vm_k1_card)

        if self.__class__.vm_k2_card:
            if self.check_vm_state(self.__class__.vm_k2_card.id) == "Expunge":
                raise unittest.SkipTest("VM is already deleted hence skipping")
            self.deletevm(self.__class__.vm_k2_card)

        if self.__class__.vm2_k2_card:
            if self.check_vm_state(self.__class__.vm2_k2_card.id) == "Expunge":
                raise unittest.SkipTest("VM is already deleted hence skipping")
            self.deletevm(self.__class__.vm2_k2_card)

        if self.__class__.vm_m60_card:
            if self.check_vm_state(self.__class__.vm_m60_card.id) == "Expunge":
                raise unittest.SkipTest("VM is already deleted hence skipping")
            self.deletevm(self.__class__.vm_m60_card)

        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_29_nonvgpuvm_k2vgpuvm_offline(self):
        """Test to change service from non vgpu to vgpu K200"""
        k200capacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs",
            "GRID K200")
        if (self.k200gpuhosts == 0) or (k200capacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with K200 vGPU Drivers installed. Skipping non gpu to K200 Offering Upgrade")

        self.serviceoffering_upgrade(
            "nonvgpuoffering",
            "None",
            "GRID K200",
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs")

        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_30_K2_vgpuvm_vgpuvm_offline(self):
        """Test to change service from vgpu K200 to vgpu K240Q"""
        k240qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs",
            "GRID K240Q")
        k200capacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs",
            "GRID K200")
        if (self.k240qgpuhosts == 0) or (self.k200gpuhosts == 0) or (
                k240qcapacity == 0) or (k200capacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with K240Q,K200Q vGPU Drivers installed. Skipping K200 to K240Q Offering Upgrade")

        self.serviceoffering_upgrade(
            "GRID K200",
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs",
            "GRID K240Q",
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs")

        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_31_K1_vgpuvm_vgpuvm_offline(self):
        """Test to change service from K1 vgpu K120Q to K1 vgpu K140Q"""

        k140qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs",
            "GRID K140Q")
        k120qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs",
            "GRID K120Q")

        if (self.k140qgpuhosts == 0) or (self.k120qgpuhosts == 0) or (
                k140qcapacity == 0) or (k120qcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with K140Q,K120Q vGPU Drivers installed. Skipping K200 to K240Q Offering Upgrade")

        self.serviceoffering_upgrade(
            "GRID K120Q",
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs",
            "GRID K140Q",
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs")

        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_32_nonvgpuvm_k1vgpuvm_offline(self):
        """Test to change service from non vgpu to vgpu K100"""

        k100capacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs",
            "GRID K100")
        if (self.k100gpuhosts == 0) or (k100capacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with K100 vGPU Drivers installed. Skipping non gpu to K100 Offering Upgrade")
        self.serviceoffering_upgrade(
            "nonvgpuoffering",
            "None",
            "GRID K100",
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs")

        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_33_K2_vgpuvm_nonvgpuvm_offline(self):
        """Test to change service from non vgpu to vgpu K240Q"""

        k240qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs",
            "GRID K240Q")
        if (self.k240qgpuhosts == 0) or (k240qcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with K240Q vGPU Drivers installed. Skipping K2 vgpu 240Q to nonvgpu Offering Upgrade")

        self.serviceoffering_upgrade(
            "GRID K240Q",
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs",
            "nonvgpuoffering",
            "None")

        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_34_K1_vgpuvm_nonvgpuvm_offline(self):
        """Test to change service from non vgpu to vgpu K140Q"""

        k140qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs",
            "GRID K140Q")

        if (self.k140qgpuhosts == 0) or (k140qcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with K140Q vGPU Drivers installed. Skipping K1 vgpu 140Q to nonvgpu Offering Upgrade")

        self.serviceoffering_upgrade(
            "GRID K140Q",
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs",
            "nonvgpuoffering",
            "None")

        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_35_K140Q_vgpuvm_K240Q_vgpuvm_offline(self):
        """Test to change service from K1 vgpu K140Q to K2 vgpu K240Q"""

        k140qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs",
            "GRID K140Q")
        k240qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs",
            "GRID K240Q")

        if (self.k140qgpuhosts == 0) or (self.k240qgpuhosts == 0) or (
                k140qcapacity == 0) or (k240qcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with K140Q,K240Q vGPU Drivers installed. Skipping K140Q to K240Q Offering Upgrade")

        self.serviceoffering_upgrade(
            "GRID K140Q",
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs",
            "GRID K240Q",
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs")
        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_36_K240Q_vgpuvm_K140Q_vgpuvm_offline(self):
        """Test to change service from K2 vgpu K240Q to K1 vgpu K140Q"""

        k140qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs",
            "GRID K140Q")
        k240qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs",
            "GRID K240Q")

        if (self.k140qgpuhosts == 0) or (self.k240qgpuhosts == 0) or (
                k140qcapacity == 0) or (k240qcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with K140Q,K240Q vGPU Drivers installed. Skipping K140Q to K240Q Offering Upgrade")

        self.serviceoffering_upgrade(
            "GRID K240Q",
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs",
            "GRID K140Q",
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs")
        return

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_37_create_deploy_windows_vm_with_k160q_vgpu_service_offering(
            self):
        """Test to create and deploy vm with K160Q vGPU service offering"""

        k160qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs",
            "GRID K160Q")

        if (self.k160qgpuhosts == 0) or (k160qcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with K160Q vGPU Drivers installed")

        self.deploy_vm(
            "GRID K160Q",
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_38_create_deploy_windows_vm_with_k180q_vgpu_service_offering(
            self):
        """Test to create and deploy vm with K180Q vGPU service offering"""

        k180qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs",
            "GRID K180Q")

        if (self.k180qgpuhosts == 0) or (k180qcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with K180Q vGPU Drivers installed")

        self.deploy_vm(
            "GRID K180Q",
            "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_39_create_deploy_windows_vm_with_k280q_vgpu_service_offering(
            self):
        """Test to create and deploy vm with K280Q vGPU service offering"""

        k280qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs",
            "GRID K280Q")

        if (self.k280qgpuhosts == 0) or (k280qcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with K280Q vGPU Drivers installed")

        self.deploy_vm(
            "GRID K280Q",
            "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_40_create_deploy_windows_vm_with_m60_passthrough_vgpu_service_offering(
            self):
        """Test to create and deploy vm with M60 passthrough vGPU service offering"""

        m60passcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs",
            "passthrough")

        if (self.m60passthroughgpuhosts == 0) or (m60passcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with M60 passthrough installed")

        self.deploy_vm(
            "passthrough",
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_41_create_deploy_windows_vm_with_m60_0q_vgpu_service_offering(
            self):
        """Test to create and deploy vm with M60-0Q vGPU service offering"""

        m60_0qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs",
            "Tesla M60-0Q")

        if (self.m60_0qgpuhosts == 0) or (m60_0qcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with M60-0Q vGPU Drivers installed")

        self.deploy_vm(
            "Tesla M60-0Q",
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_42_create_deploy_windows_vm_with_m60_1q_vgpu_service_offering(
            self):
        """Test to create and deploy vm with M60-1Q vGPU service offering"""

        m60_1qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs",
            "Tesla M60-1Q")

        if (self.m60_1qgpuhosts == 0) or (m60_1qcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with M60-1Q vGPU Drivers installed")

        self.deploy_vm(
            "Tesla M60-1Q",
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_43_create_deploy_windows_vm_with_m60_2q_vgpu_service_offering(
            self):
        """Test to create and deploy vm with M60-2Q vGPU service offering"""

        m60_2qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs",
            "Tesla M60-2Q")

        if (self.m60_2qgpuhosts == 0) or (m60_2qcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with M60-2Q vGPU Drivers installed")

        self.deploy_vm(
            "Tesla M60-2Q",
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_44_create_deploy_windows_vm_with_m60_4q_vgpu_service_offering(
            self):
        """Test to create and deploy vm with M60-4Q vGPU service offering"""

        m60_4qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs",
            "Tesla M60-4Q")

        if (self.m60_4qgpuhosts == 0) or (m60_4qcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with M60-4Q vGPU Drivers installed")

        self.deploy_vm(
            "Tesla M60-4Q",
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_45_create_deploy_windows_vm_with_m60_8q_vgpu_service_offering(
            self):
        """Test to create and deploy vm with M60-8Q vGPU service offering"""

        m60_8qcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs",
            "Tesla M60-8Q")

        if (self.m60_8qgpuhosts == 0) or (m60_8qcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with M60-8Q vGPU Drivers installed")

        self.deploy_vm(
            "Tesla M60-8Q",
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_46_create_deploy_windows_vm_with_m60_0b_vgpu_service_offering(
            self):
        """Test to create and deploy vm with M60-0B vGPU service offering"""

        m60_0bcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs",
            "Tesla M60-0B")

        if (self.m60_0bgpuhosts == 0) or (m60_0bcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with M60-0B vGPU Drivers installed")

        self.deploy_vm(
            "Tesla M60-0B",
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_47_create_deploy_windows_vm_with_m60_1b_vgpu_service_offering(
            self):
        """Test to create and deploy vm with M60-1B vGPU service offering"""

        m60_1bcapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs",
            "Tesla M60-1B")

        if (self.m60_1bgpuhosts == 0) or (m60_1bcapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with M60-1B vGPU Drivers installed")

        self.deploy_vm(
            "Tesla M60-1B",
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_48_create_deploy_windows_vm_with_m60_1a_vgpu_service_offering(
            self):
        """Test to create and deploy vm with M60-1A vGPU service offering"""

        m60_1acapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs",
            "Tesla M60-1A")

        if (self.m60_1agpuhosts == 0) or (m60_1acapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with M60-1A vGPU Drivers installed")

        self.deploy_vm(
            "Tesla M60-1A",
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_49_create_deploy_windows_vm_with_m60_2a_vgpu_service_offering(
            self):
        """Test to create and deploy vm with M60-2A vGPU service offering"""

        m60_2acapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs",
            "Tesla M60-2A")

        if (self.m60_2agpuhosts == 0) or (m60_2acapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with M60-2A vGPU Drivers installed")

        self.deploy_vm(
            "Tesla M60-2A",
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_50_create_deploy_windows_vm_with_m60_4a_vgpu_service_offering(
            self):
        """Test to create and deploy vm with M60-4A vGPU service offering"""

        m60_4acapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs",
            "Tesla M60-4A")

        if (self.m60_4agpuhosts == 0) or (m60_4acapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with M60-4A vGPU Drivers installed")

        self.deploy_vm(
            "Tesla M60-4A",
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs")

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_51_create_deploy_windows_vm_with_m60_8a_vgpu_service_offering(
            self):
        """Test to create and deploy vm with M60-8A vGPU service offering"""

        m60_8acapacity = self.check_host_vgpu_capacity(
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs",
            "Tesla M60-8A")

        if (self.m60_8agpuhosts == 0) or (m60_8acapacity == 0):
            raise unittest.SkipTest(
                "No XenServer available with M60-8A vGPU Drivers installed")

        self.deploy_vm(
            "Tesla M60-8A",
            "Group of NVIDIA Corporation GM204GL [Tesla M60] GPUs")

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_52_create_m60_vm_snapshot_with_memory(self):
        """Test to create VM snapshots
        """

        if self.__class__.vmlifecycletest == 0:
            raise unittest.SkipTest(
                "VM Life Cycle Deploy VM test failed hence skipping")

        if not (self.__class__.vm_m60_card):
            raise unittest.SkipTest("No VM available.Hence skipping")

        self.create_vm_snapshot_with_memory(self.__class__.vm_m60_card)

        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_53_revert_m60_vm_snapshot_with_memory(self):
        """Test to revert VM snapshots
        """

        if self.__class__.vmlifecycletest == 0:
            raise unittest.SkipTest(
                "VM Life Cycle Deploy VM test failed hence skipping")

        if self.__class__.vmsnapwithmemory == 0:
            raise unittest.SkipTest(
                "VM Snapshot creation with memory test failed hence skipping")

        if not (self.__class__.vm_m60_card):
            raise unittest.SkipTest("No VM available.Hence skipping")

        self.revert_vm_snapshot_with_memory(self.__class__.vm_m60_card)

        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_54_delete_m60_vm_snapshot_with_memory(self):
        """Test to delete vm snapshots
        """

        if self.__class__.vmlifecycletest == 0:
            raise unittest.SkipTest(
                "VM Life Cycle Deploy VM test failed hence skipping")

        if self.__class__.vmsnapwithmemory == 0:
            raise unittest.SkipTest(
                "VM Snapshot creation with memory test failed hence skipping")

        if not(self.__class__.vm_m60_card):
            raise unittest.SkipTest("No VM available.Hence skipping")

        self.delete_vm_snapshot(self.__class__.vm_m60_card)

        return

    @classmethod
    def tearDownClass(self):
        try:
            self.apiclient = super(
                TestvGPUWindowsVm,
                self).getClsTestClient().getApiClient()
            cleanup_resources(self.apiclient, self._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)
