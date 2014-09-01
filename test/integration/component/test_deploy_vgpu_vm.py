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

#Test from the Marvin - Testing in Python wiki

#All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.cloudstackAPI import changeServiceForVirtualMachine,updateServiceOffering,listConfigurations

#Import Integration Libraries

#base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import Account, VirtualMachine, ServiceOffering , Template , Host

#utils - utility classes for common cleanup, external library wrappers etc
from marvin.lib.utils import cleanup_resources

#common - commonly used methods for all tests are listed here
from marvin.lib.common import get_zone, get_domain, get_template, list_hosts ,list_service_offering, get_windows_template, get_pod, list_clusters, list_virtual_machines

from marvin.sshClient import SshClient

from marvin.codes import FAILED, XEN_SERVER

from nose.plugins.attrib import attr

import time

class TestvGPUWindowsVm(cloudstackTestCase):
    """
    Testing vGPU VM with All vGPU service offerings
    """
    @classmethod
    def setUpClass(cls):
        testClient = super(TestvGPUWindowsVm, cls).getClsTestClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        cls.apiclient = cls.testClient.getApiClient()
        cls._cleanup = []
        cls.cleanup = []
        hosts = list_hosts(
               cls.apiclient,
               hypervisor="XenServer"
               )
        if hosts is None:
            raise unittest.SkipTest("There are no XenServers available. GPU feature is supported only on XenServer.Check listhosts response")
        else:
             cls.k140qgpuhosts=0
             cls.k120qgpuhosts=0
             cls.k100gpuhosts=0
             cls.k260qgpuhosts=0
             cls.k240qgpuhosts=0
             cls.k220qgpuhosts=0
             cls.k200gpuhosts=0
             cls.k1passthroughgpuhosts=0
             cls.k2passthroughgpuhosts=0
             cls.nongpuhosts=[]
             k2hosts=0
             k1hosts=0

             for ghost in hosts :
                 if ghost.hypervisorversion >= "6.2.0":
                    #sshClient = SshClient(host=ghost.ipaddress, port=22, user='root',passwd="host_password")
                    sshClient = SshClient(host=ghost.ipaddress, port=22, user='root',passwd="host_password")

                    if ghost.hypervisorversion == "6.2.0":
                       res = len(sshClient.execute("xe patch-list uuid=0850b186-4d47-11e3-a720-001b2151a503"))
                       if res == 0:
                           continue
                    k1card= len(sshClient.execute("lspci | grep \"GRID K1\""))
                    k2card= len(sshClient.execute("lspci | grep \"GRID K2\""))
                    cls.debug("k1 card and k2 card details are :%s %s "%(k1card,k2card))
                    if (k2card==0) and (k1card==0):
                       cls.nongpuhosts.append(ghost.ipaddress)
                    if k2card !=0:
                          k2hosts=k2hosts+1
                          k260q = len(sshClient.execute("xe vgpu-type-list model-name=\"GRID K260Q\""))
                          k240q = len(sshClient.execute("xe vgpu-type-list model-name=\"GRID K240Q\""))
                          k220q = len(sshClient.execute("xe vgpu-type-list model-name=\"GRID K220Q\""))
                          k200 = len(sshClient.execute("xe vgpu-type-list model-name=\"GRID K200\""))
                          k2passthrough = len(sshClient.execute("xe vgpu-type-list model-name='passthrough'"))
                          if ((k260q==0) and (k240q==0)  and (k220q==0) and (k200==0) and (k2passthrough== 0)): 
                              continue
                          else:
                               if k260q != 0 :
                                  cls.k260qgpuhosts=cls.k260qgpuhosts+1
                               if k240q != 0 :
                                  cls.k240qgpuhosts=cls.k240qgpuhosts+1
                               if k220q != 0 :
                                  cls.k220qgpuhosts=cls.k220qgpuhosts+1
                               if k200 != 0 :
                                  cls.k200gpuhosts=cls.k200gpuhosts+1
                               if k2passthrough != 0:
                                  cls.k2passthroughgpuhosts=cls.k2passthroughgpuhosts+1

                    if k1card != 0:
                            k1hosts=k1hosts+1
                            k100 = len(sshClient.execute("xe vgpu-type-list model-name=\"GRID K100\""))
                            k120q = len(sshClient.execute("xe vgpu-type-list model-name=\"GRID K120Q\""))
                            k140q = len(sshClient.execute("xe vgpu-type-list model-name=\"GRID K140Q\""))
                            k1passthrough = len(sshClient.execute("xe vgpu-type-list model-name='passthrough'"))
                            if ((k100 == 0) and (k120q == 0) and (k140q == 0) and (k1passthrough == 0)):
                                continue
                            else:
                                if k140q != 0 :
                                   cls.k140qgpuhosts=cls.k140qgpuhosts+1
                                if k120q != 0 :
                                   cls.k120qgpuhosts=cls.k120qgpuhosts+1
                                if k100 != 0 :
                                   cls.k100gpuhosts=cls.k100gpuhosts+1
                                if k1passthrough != 0 :
                                   cls.k1passthroughgpuhosts=cls.k1passthroughgpuhosts+1
        if (k1hosts == 0) and (k2hosts == 0):
           raise unittest.SkipTest("No XenServer available with GPU Drivers installed")

        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.domain = get_domain(cls.apiclient)
        cls.pod = get_pod(cls.apiclient, cls.zone.id)
        cls.account = Account.create(
               cls.apiclient,
               cls.testdata["account"],
               domainid=cls.domain.id
               )

        cls.template = get_windows_template(cls.apiclient, cls.zone.id ,ostype_desc="Windows 8 (64-bit)")

        if  cls.template == FAILED:
            cls.template = Template.register(
                   cls.apiclient,
                   cls.testdata["vgpu"] ["templateregister1"],
                   hypervisor = "XenServer",
                   zoneid=cls.zone.id,
                   domainid=cls.account.domainid,
                   account=cls.account.name
                   )
            timeout = cls.testdata["vgpu"]["timeout"]

            while True:
                  time.sleep(cls.testdata["vgpu"]["sleep"])
                  list_template_response = Template.list(
                       cls.apiclient,
                       templatefilter=\
                       cls.testdata["templatefilter"],
                       id=cls.template.id
                        )
                  if (isinstance(list_template_response, list)) is not True:
                      raise unittest.SkipTest("Check list template api response returns a valid list")

                  if len(list_template_response) is None :
                      raise unittest.SkipTest("Check template registered is in List Templates")

                  template_response = list_template_response[0]
                  if template_response.isready == True:
                      break
                  
                  if timeout == 0:
                      raise unittest.SkipTest("Failed to download template(ID: %s)" % template_response.id)

                  timeout = timeout - 1


        """
        Create Service Offerings for Both K1 and K2 cards to be used for VM life cycle tests
        """

        if(k1hosts != 0):
            if(cls.k140qgpuhosts != 0):
                gtype = "GRID K140Q"
            elif(cls.k120qgpuhosts != 0):
                gtype = "GRID K120Q"
            elif(cls.k100gpuhosts !=0):
                gtype = "GRID K100"
            else:
                gtype = "passthrough"

            cls.testdata["vgpu"]["service_offerings"][gtype]["serviceofferingdetails"] = [{'pciDevice':'Group of NVIDIA Corporation GK107GL [GRID K1] GPUs'},
                                                                                       {'vgpuType':gtype}]
            try:
               cls.k100_vgpu_service_offering = ServiceOffering.create(
                                                                    cls.apiclient,
                                                                    cls.testdata["vgpu"]["service_offerings"][gtype]
                                                                    )
            except Exception as e:
               cls.fail("Failed to create the service offering, %s" % e)

        if(k2hosts != 0):
            if(cls.k240qgpuhosts != 0):
                gtype = "GRID K240Q"
            elif(cls.k220qgpuhosts != 0):
                gtype = "GRID K220Q"
            elif(cls.k200gpuhosts !=0):
                gtype = "GRID K200"
            else:
                gtype = "passthrough"

            cls.testdata["vgpu"]["service_offerings"][gtype]["serviceofferingdetails"] = [{'pciDevice': 'Group of NVIDIA Corporation GK104GL [GRID K2] GPUs'},
                                                                                       {'vgpuType':gtype}]
            try:
               cls.k200_vgpu_service_offering = ServiceOffering.create(
                                                                    cls.apiclient,
                                                                    cls.testdata["vgpu"]["service_offerings"][gtype]
                                                                    )
            except Exception as e:
              cls.fail("Failed to create the service offering, %s" % e)

        """
        Create Virtual Machines for Both K1 and K2 cards to be used for VM life cycle tests
        """

        if(k1hosts != 0):
            cls.vm_k1_card = VirtualMachine.create(
            cls.apiclient,
            cls.testdata["virtual_machine"],
            accountid=cls.account.name,
            zoneid=cls.zone.id,
            domainid=cls.account.domainid,
            serviceofferingid=cls.k100_vgpu_service_offering.id,
            templateid=cls.template.id
            )
        if(k2hosts !=0):
            cls.vm_k2_card = VirtualMachine.create(
            cls.apiclient,
            cls.testdata["virtual_machine"],
            accountid=cls.account.name,
            zoneid=cls.zone.id,
            domainid=cls.account.domainid,
            serviceofferingid=cls.k200_vgpu_service_offering.id,
            templateid=cls.template.id
            )


        cls._cleanup = [
                         cls.account,
                         cls.k100_vgpu_service_offering,
                         cls.k200_vgpu_service_offering
                        ]

    @attr(tags = ['advanced', 'basic', 'vgpu'], required_hardware="true")
    def setUp(self):
        self.testdata = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    def check_host_vgpu_capacity(self,gpucard,gtype):
        gputhosts = list_hosts(
               self.apiclient,
               hypervisor="XenServer"
               )
        vgpucapacity=0
        for ghost in gputhosts:
            if ghost.gpugroup is not None:
                        for gp in ghost.gpugroup:
                            if gp.gpugroupname == gpucard:
                               for gptype in gp.vgpu:
                                   if gptype.vgputype == gtype:
                                     vgpucapacity=vgpucapacity+gptype.remainingcapacity

        return(vgpucapacity)


    @attr(tags = ['advanced', 'basic', 'vgpu'], required_hardware="true")
    def vgpu_serviceoffering_creation(self,gtype,nvidiamodel):
        if gtype=="nonvgpuoffering" and nvidiamodel=="None":
           self.service_offering = ServiceOffering.create(
                                                       self.apiclient,
                                                       self.testdata["vgpu"]["service_offerings"][gtype]
                                                          )
           list_service_response = ServiceOffering.list(
               self.apiclient,
               id=self.service_offering.id
               )

           if list_service_response is None:
               raise unittest.SkipTest("Check Service Offering list for %s service offering" %(gtype))

           self.assertEqual(
                 list_service_response[0].displaytext,
                 self.testdata["vgpu"]["service_offerings"][gtype]["displaytext"],
                 "Check server displaytext in createServiceOfferings"
                )
           self.assertEqual(
               list_service_response[0].name,
               self.testdata["vgpu"]["service_offerings"][gtype]["name"],
               "Check name in createServiceOffering"
                )
        else:
              self.testdata["vgpu"]["service_offerings"][gtype]["serviceofferingdetails"] = [{'pciDevice': nvidiamodel},
                                                                                       {'vgpuType':gtype}]
              self.service_offering = ServiceOffering.create(
                                                       self.apiclient,
                                                       self.testdata["vgpu"]["service_offerings"][gtype]
                                                          )
              list_service_response = ServiceOffering.list(
               self.apiclient,
               id=self.service_offering.id
               )

              if list_service_response is None:
                 raise unittest.SkipTest("Check Service Offering list for %s service offering" %(gtype))

              self.assertEqual(
                 list_service_response[0].serviceofferingdetails.vgpuType,
                        gtype,
                    "Failed To Create Service Offering . Check vGPU Service Offering list for K2 passthrough"
                  )
              self.assertEqual(
                 list_service_response[0].displaytext,
                 self.testdata["vgpu"]["service_offerings"][gtype]["displaytext"],
                 "Check server displaytext in createServiceOfferings"
                )
              self.assertEqual(
               list_service_response[0].name,
               self.testdata["vgpu"]["service_offerings"][gtype]["name"],
               "Check name in createServiceOffering"
                )
        return(self.service_offering)


    @attr(tags = ['advanced', 'basic', 'vgpu'], required_hardware="true")
    def check_for_vGPU_resource(self,hostid,vminstancename,serviceofferingid,vgputype):
        """
        Validate the VM for VGPU resources
        """
        """Create SSH Client for Host Connection
        """
        vgpu_host = list_hosts(
                               self.apiclient,
                               id=hostid
                               )
        ssh_client = SshClient(host=vgpu_host[0].ipaddress, port=22, user='root',passwd="host_password")
        """
        Get vGPU type model
        """
        vgpu_type_model = ssh_client.execute("xe vgpu-list vm-name-label=" + vminstancename + " params=type-model-name --minimal")
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
        self.assertEqual(
                      list_service_offering_response[0].serviceofferingdetails.vgpuType,
                      vgpu_type_model[0],
                      "Vm does not have the correct GPU resources, verified on the host"
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

    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")

    def deploy_vGPU_windows_vm(self,vgpuofferingid,vgput):
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
        list_vms = VirtualMachine.list(self.apiclient, id=self.virtual_machine.id)

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"\
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
        if vgput != "None":
           self.assertEqual(
              vm.vgpu,
              vgput,
              msg="Failed to deploy VM with vGPU card"
           )
        return

    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")
    def delete_vgpu_service_offering(self,serviceoffering):
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

    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")
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

    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")
    def stop_vm(self):
        """Stop Virtual Machine
        """

        try:
            self.virtual_machine.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop VM: %s" % e)
        return

    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")
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
                "Verify listVirtualMachines response for virtual machine: %s" \
                % self.virtual_machine.id
                )
        self.assertEqual(
                            list_vm_response[0].state,
                            "Running",
                            "Check virtual machine is in running state"
                        )
        return


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
        k260q=0
        k240q=0
        k220q=0
        k200=0
        k2pass=0

        k140q=0
        k120q=0
        k100=0
        k1pass=0

        for ggroup in hhosts:
            if ggroup.ipaddress not in self.nongpuhosts:
               for gp in ggroup.gpugroup:
                   if gp.gpugroupname == "Group of NVIDIA Corporation GK104GL [GRID K2] GPUs":
                      for gptype in gp.vgpu:
                         if gptype.vgputype == "GRID K260Q":
                             k260q=k260q+1
                         if gptype.vgputype == "GRID K220Q":
                               k220q=k220q+1
                         if gptype.vgputype == "GRID K240Q":
                              k240q=k240q+1
                         if gptype.vgputype == "GRID K200":
                               k200=k200+1
                         if gptype.vgputype != "passthrough":
                              k2pass=k2pass+1
                   if gp.gpugroupname == "Group of NVIDIA Corporation GK107GL [GRID K1] GPUs":
                      for gptype in gp.vgpu:
                          if gptype.vgputype == "GRID K140Q":
                             k140q=k140q+1
                          if gptype.vgputype == "GRID K120Q":
                             k120q=k120q+1
                          if gptype.vgputype == "GRID K100":
                             k100=k100+1
                          if gptype.vgputype == "passthrough":
                             k1pass=k1pass+1
            else:
                self.debug("This is nongpuhost:%s"%(ggroup.ipaddress))
        if self.k260qgpuhosts>0:
           if not k260q:
               self.fail("list host details with K260Q vgpu are not correct")
        if self.k240qgpuhosts>0:
           if not k240q:
               self.fail("list host details with K240Q vgpu are not correct")
        if self.k220qgpuhosts>0:
           if not k220q:
               self.fail("list host details with K220Q vgpu are not correct")
        if self.k200gpuhosts>0:
           if not k200:
               self.fail("list host details with K200 vgpu are not correct")
        if self.k2passthroughgpuhosts>0:
            if not k2pass:
               self.fail("list host details with K2 passthrough vgpu are not correct")
        if self.k140qgpuhosts>0:
           if not k140q:
               self.fail("list host details with K140Q vgpu are not correct")
        if self.k120qgpuhosts>0:
           if not k120q:
               self.fail("list host details with K120Q vgpu are not correct")
        if self.k100gpuhosts>0:
           if not k100:
               self.fail("list host details with K100 vgpu are not correct")
        if self.k1passthroughgpuhosts>0:
           if not k200:
               self.fail("list host details with K1 Passthrough vgpu are not correct")

    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")

    def test_01_create_deploy_windows_vm_with_k100_vgpu_service_offering(self):
        """Test to create and deploy vm with K100 vGPU service offering"""
        k100capacity=self.check_host_vgpu_capacity("Group of NVIDIA Corporation GK107GL [GRID K1] GPUs","GRID K100")
        self.debug("k100capacity is:%s"%(k100capacity))
        self.debug("k100gpuhosts is:%s"%(self.k100gpuhosts))
        if (self.k100gpuhosts == 0) or (k100capacity == 0):
           raise unittest.SkipTest("No XenServer available with K100 vGPU Drivers installed")
        self.vgpuoffering=self.vgpu_serviceoffering_creation("GRID K100","Group of NVIDIA Corporation GK107GL [GRID K1] GPUs")
        self.deploy_vGPU_windows_vm(self.vgpuoffering.id,"GRID K100")
        self.destroy_vm()
        self.delete_vgpu_service_offering(self.vgpuoffering)

    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")

    def test_03_create_deploy_windows_vm_with_k120q_vgpu_service_offering(self):
        """Test to create and deploy vm with K120Q vGPU service offering"""

        k120qcapacity=self.check_host_vgpu_capacity("Group of NVIDIA Corporation GK107GL [GRID K1] GPUs","GRID K120Q")

        if (self.k120qgpuhosts == 0) or (k120qcapacity==0):
           raise unittest.SkipTest("No XenServer available with K120Q vGPU Drivers installed")

        self.vgpuoffering=self.vgpu_serviceoffering_creation("GRID K120Q","Group of NVIDIA Corporation GK107GL [GRID K1] GPUs")
        if self.vgpuoffering is not None:
              self.deploy_vGPU_windows_vm(self.vgpuoffering.id,"GRID K120Q")
              self.delete_vgpu_service_offering(self.vgpuoffering)
              self.destroy_vm()
              self.delete_vgpu_service_offering(self.vgpuoffering)

    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")

    def test_04_create_deploy_windows_vm_with_k140q_vgpu_service_offering(self):
        """Test to create and deploy vm with K140Q vGPU service offering"""

        k140qcapacity=self.check_host_vgpu_capacity("Group of NVIDIA Corporation GK107GL [GRID K1] GPUs","GRID K140Q")

        if (self.k140qgpuhosts == 0) or (k140qcapacity ==0):
           raise unittest.SkipTest("No XenServer available with K140Q vGPU Drivers installed")

        self.vgpuoffering=self.vgpu_serviceoffering_creation("GRID K140Q","Group of NVIDIA Corporation GK107GL [GRID K1] GPUs")
        if self.vgpuoffering is not None:
              self.deploy_vGPU_windows_vm(self.vgpuoffering.id,"GRID K140Q")
              self.delete_vgpu_service_offering(self.vgpuoffering)
              self.destroy_vm()
              self.delete_vgpu_service_offering(self.vgpuoffering)

    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")

    def test_05_create_deploy_windows_vm_with_k1_passthrough_vgpu_service_offering(self):
        """Test to create and deploy vm with K1 passthrough vGPU service offering"""

        k1passcapacity=self.check_host_vgpu_capacity("Group of NVIDIA Corporation GK107GL [GRID K1] GPUs","passthrough")

        if (self.k1passthroughgpuhosts == 0) or (k1passcapacity==0):
           raise unittest.SkipTest("No XenServer available with K1 passthrough installed")

        self.vgpuoffering=self.vgpu_serviceoffering_creation("passthrough","Group of NVIDIA Corporation GK107GL [GRID K1] GPUs")
        if self.vgpuoffering is not None:
              self.deploy_vGPU_windows_vm(self.vgpuoffering.id,"passthrough")
              self.delete_vgpu_service_offering(self.vgpuoffering)
              self.destroy_vm()
              self.delete_vgpu_service_offering(self.vgpuoffering)

    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")

    def test_06_create_deploy_windows_vm_with_k2_passthrough_vgpu_service_offering(self):
        """Test to create and deploy vm with K2 pasthrough vGPU service offering"""

        k2passcapacity=self.check_host_vgpu_capacity("Group of NVIDIA Corporation GK104GL [GRID K2] GPUs","passthrough")
        
        if (self.k2passthroughgpuhosts == 0) or (k2passcapacity==0):
           raise unittest.SkipTest("No XenServer available with K2 passthrough installed")

        self.vgpuoffering=self.vgpu_serviceoffering_creation("passthrough","Group of NVIDIA Corporation GK104GL [GRID K2] GPUs")
        if self.vgpuoffering is not None:
              self.deploy_vGPU_windows_vm(self.vgpuoffering.id,"passthrough")
              self.delete_vgpu_service_offering(self.vgpuoffering)
              self.destroy_vm()
              self.delete_vgpu_service_offering(self.vgpuoffering)

    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")

    def test_07_create_deploy_windows_vm_with_k260q_vgpu_service_offering(self):
        """Test to create and deploy vm with K260Q vGPU service offering"""

        k260qcapacity=self.check_host_vgpu_capacity("Group of NVIDIA Corporation GK104GL [GRID K2] GPUs","GRID K260Q")

        if (self.k260qgpuhosts == 0) or (k260qcapacity == 0):
           raise unittest.SkipTest("No XenServer available with K260Q vGPU Drivers installed")

        self.vgpuoffering=self.vgpu_serviceoffering_creation("GRID K260Q","Group of NVIDIA Corporation GK104GL [GRID K2] GPUs")
        if self.vgpuoffering is not None:
              self.deploy_vGPU_windows_vm(self.vgpuoffering.id,"GRID K260Q")
              self.delete_vgpu_service_offering(self.vgpuoffering)
              self.destroy_vm()
              self.delete_vgpu_service_offering(self.vgpuoffering)

    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")

    def test_08_create_deploy_windows_vm_with_k240q_vgpu_service_offering(self):
        """   Test to create and deploy vm with K240Q vGPU service offering """

        k240qcapacity=self.check_host_vgpu_capacity("Group of NVIDIA Corporation GK104GL [GRID K2] GPUs","GRID K240Q")

        if (self.k240qgpuhosts == 0) or (k240qcapacity==0):
           raise unittest.SkipTest("No XenServer available with K240Q vGPU Drivers installed")

        self.vgpuoffering=self.vgpu_serviceoffering_creation("GRID K240Q","Group of NVIDIA Corporation GK104GL [GRID K2] GPUs")
        if self.vgpuoffering is not None:
              self.deploy_vGPU_windows_vm(self.vgpuoffering.id,"GRID K240Q")
              self.delete_vgpu_service_offering(self.vgpuoffering)
              self.destroy_vm()
              self.delete_vgpu_service_offering(self.vgpuoffering)

    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")

    def test_09_create_deploy_windows_vm_with_k220q_vgpu_service_offering(self):
        """  Test to create and deploy vm with K220Q vGPU service offering """

        k220qcapacity=self.check_host_vgpu_capacity("Group of NVIDIA Corporation GK104GL [GRID K2] GPUs","GRID K220Q")

        if (self.k220qgpuhosts == 0) or (k220qcapacity==0):
           raise unittest.SkipTest("No XenServer available with K220Q vGPU Drivers installed")

        self.vgpuoffering=self.vgpu_serviceoffering_creation("GRID K220Q","Group of NVIDIA Corporation GK104GL [GRID K2] GPUs")
        if self.vgpuoffering is not None:
              self.deploy_vGPU_windows_vm(self.vgpuoffering.id,"GRID K220Q")
              self.delete_vgpu_service_offering(self.vgpuoffering)
              self.destroy_vm()
              self.delete_vgpu_service_offering(self.vgpuoffering)

    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")

    def test_10_create_deploy_windows_vm_with_k200_vgpu_service_offering(self):
        """   Test to create and deploy vm with K200 vGPU service offering  """

        k200capacity=self.check_host_vgpu_capacity("Group of NVIDIA Corporation GK104GL [GRID K2] GPUs","GRID K200")

        if (self.k200gpuhosts == 0) or (k200capacity==0):
           raise unittest.SkipTest("No XenServer available with K200 vGPU Drivers installed")

        self.vgpuoffering=self.vgpu_serviceoffering_creation("GRID K200","Group of NVIDIA Corporation GK104GL [GRID K2] GPUs")
        if self.vgpuoffering is not None:
              self.deploy_vGPU_windows_vm(self.vgpuoffering.id,"GRID K200")
              self.delete_vgpu_service_offering(self.vgpuoffering)
              self.destroy_vm()
              self.delete_vgpu_service_offering(self.vgpuoffering)


    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")

    def test_11_add_nonvgpu_host_to_vgpucluster(self):
        """   Test to add non vGPU host to an existing cluster  """
        countx=0
        #hostuuids=[]
        huuids=[]
        lhosts = list_hosts(
               self.apiclient,
               hypervisor="XenServer"
               )

        sshClient1 = SshClient(host=lhosts[0].ipaddress, port=22, user='root',passwd="host_password")
        totalxenhosts=len(sshClient1.execute("xe host-list | grep uuid"))

        for hostlist in lhosts:
            countx=countx+1

        if totalxenhosts<=countx:
           raise unittest.SkipTest("No additional Host is available in the XS pool to add host")

        huuids.append(sshClient1.execute("xe host-list --minimal"))
        self.debug("host uuids are:%s"%huuids)
        hostuuids=huuids[0][0].split(',')
        addhost="FAIL"

        for hid in hostuuids:
            self.debug("host id: %s"%(hid))
            haddress=sshClient1.execute("xe host-param-get param-name=address --minimal uuid="+hid)
            self.debug("host address is %s"%(haddress[0]))
            if haddress[0]==self.testdata["vgpu"]["nongpu_host_ip"]:
               addhost="PASS"
               break

        if addhost!="PASS":
            raise unittest.SkipTest("XS Pool new host ip is not matching with test data host ip. Skipping the test.Please update test data file with hostip added to XS pool")
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
        self.debug("Cluster response is:%s"%(cluster_response))
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
                    "Created host (ID: %s) in cluster ID %s" %(
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
                #Cleanup Host
        self.cleanup.append(nongpuhost)



    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")

    def test_13_validate_deployed_vGPU_windows_vm(self):
        """ Test deploy virtual machine
        """
        self.debug("Check if deployed VMs are in running state?")
        if(self.vm_k1_card):
          self.vm_k1_card.getState(
                                   self.apiclient,
                                   "Running")
          self.check_for_vGPU_resource(self.vm_k1_card.hostid,self.vm_k1_card.instancename,self.vm_k1_card.serviceofferingid,self.vm_k1_card.vgpu)

        if(self.vm_k2_card):
          self.vm_k2_card.getState(
                                   self.apiclient,
                                   "Running")

          self.check_for_vGPU_resource(self.vm_k2_card.hostid,self.vm_k2_card.instancename,self.vm_k2_card.serviceofferingid,self.vm_k2_card.vgpu)

        return

    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")
    def test_14_stop_vGPU_windows_vm(self):
        """ Test stop virtual machine
        """
        if(self.vm_k1_card):
          self.vm_k1_card.stop(self.apiclient)

        if(self.vm_k2_card):
          self.vm_k2_card.stop(self.apiclient)

        return

    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")
    def test_15_start_vGPU_windows_vm(self):
        """ Test start virtual machine
        """
        if(self.vm_k1_card):
          self.vm_k1_card.start(self.apiclient)
          time.sleep(200)
          self.check_for_vGPU_resource(self.vm_k1_card.hostid,self.vm_k1_card.instancename,self.vm_k1_card.serviceofferingid,self.vm_k1_card.vgpu)

        if(self.vm_k2_card):
          self.vm_k2_card.start(self.apiclient)
          time.sleep(200)
          self.check_for_vGPU_resource(self.vm_k2_card.hostid,self.vm_k2_card.instancename,self.vm_k2_card.serviceofferingid,self.vm_k2_card.vgpu)

        return

    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")
    def test_16_restore_vGPU_windows_vm(self):
        """Test restore Virtual Machine
        """
        if(self.vm_k1_card):
          self.vm_k1_card.restore(self.apiclient)
          time.sleep(200)
          self.vm_k1_card.getState(
                                   self.apiclient,
                                   "Running")

          self.check_for_vGPU_resource(self.vm_k1_card.hostid,self.vm_k1_card.instancename,self.vm_k1_card.serviceofferingid,self.vm_k1_card.vgpu)

        if(self.vm_k2_card):
          self.vm_k2_card.restore(self.apiclient)
          time.sleep(200)
          self.vm_k2_card.getState(
                                   self.apiclient,
                                   "Running")

          self.check_for_vGPU_resource(self.vm_k2_card.hostid,self.vm_k2_card.instancename,self.vm_k2_card.serviceofferingid,self.vm_k2_card.vgpu)

        return

    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")
    def test_17_reboot_vGPU_windows_vm(self):
        """ Test reboot virtual machine
        """
        if(self.vm_k1_card):
          self.vm_k1_card.reboot(self.apiclient)
          time.sleep(200)
          self.vm_k1_card.getState(
                                   self.apiclient,
                                   "Running")

          self.check_for_vGPU_resource(self.vm_k1_card.hostid,self.vm_k1_card.instancename,self.vm_k1_card.serviceofferingid,self.vm_k1_card.vgpu)

        if(self.vm_k2_card):
          self.vm_k2_card.reboot(self.apiclient)
          time.sleep(200)
          self.vm_k2_card.getState(
                                   self.apiclient,
                                   "Running")

          self.check_for_vGPU_resource(self.vm_k2_card.hostid,self.vm_k2_card.instancename,self.vm_k2_card.serviceofferingid,self.vm_k2_card.vgpu)

        return

    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")
    def test_18_destroy_vGPU_windows_vm(self):
        """Test destroy Virtual Machine
        """
        if(self.vm_k1_card):
          self.vm_k1_card.delete(self.apiclient)
          self.vm_k1_card.getState(
                                   self.apiclient,
                                   "Destroyed")

        if(self.vm_k2_card):
          self.vm_k2_card.delete(self.apiclient)
          self.vm_k2_card.getState(
                                   self.apiclient,
                                   "Destroyed")

        return

    @attr(tags = ['advanced', 'basic' , 'vgpu'], required_hardware="true")
    def test_19_recover_vGPU_windows_vm(self):
        """Test recover Virtual Machine
        """
        if(self.vm_k1_card):
          self.vm_k1_card.recover(self.apiclient)
          self.vm_k1_card.getState(
                                   self.apiclient,
                                   "Stopped")


        if(self.vm_k2_card):
          self.vm_k2_card.recover(self.apiclient)
          self.vm_k2_card.getState(
                                   self.apiclient,
                                   "Stopped")

        return


    @classmethod
    def tearDownClass(self):
        try:
            self.apiclient = super(TestvGPUWindowsVm, self).getClsTestClient().getApiClient()
            cleanup_resources(self.apiclient, self._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)




