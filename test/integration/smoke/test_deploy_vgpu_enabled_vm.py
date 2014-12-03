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

#Test from the Marvin - Testing in Python wiki

#All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase, unittest

#Import Integration Libraries

#base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import Account, VirtualMachine, ServiceOffering

#utils - utility classes for common cleanup, external library wrappers etc
from marvin.lib.utils import cleanup_resources

#common - commonly used methods for all tests are listed here
from marvin.lib.common import get_zone, get_domain, get_template, list_hosts

from marvin.sshClient import SshClient

from marvin.codes import FAILED, XEN_SERVER

from nose.plugins.attrib import attr

class TestDeployvGPUenabledVM(cloudstackTestCase):
    """
    Test deploy a vGPU enabled VM into a user account
    """
    @classmethod
    def setUpClass(self):
        testClient = super(TestDeployvGPUenabledVM, self).getClsTestClient()
        self.apiclient = testClient.getApiClient()
        self.testdata = self.testClient.getParsedTestDataConfig()
        #Need to add check whether zone containing the xen hypervisor or not as well
        hosts = list_hosts(
               self.apiclient,
               hypervisor="XenServer"
               )
        if hosts is None:
            raise unittest.SkipTest("There are no XenServers available. GPU feature is supported only on XenServer.Check listhosts response")
        else: 
             gpuhosts=0
             for ghost in hosts :
                    if ghost.hypervisorversion >= "6.2.0":
                       sshClient = SshClient(host=ghost.ipaddress, port=22, user='root',passwd=self.testdata["host_password"]) 
                       if ghost.hypervisorversion == "6.2.0":
                          res = sshClient.execute("xe patch-list uuid=0850b186-4d47-11e3-a720-001b2151a503")
                          if len(res) == 0:
                              continue
                       res = sshClient.execute("xe vgpu-type-list model-name=\"GRID K120Q\"")
                       if len(res) != 0 :
                           gpuhosts=gpuhosts+1 
                       else:        
                           continue
        if gpuhosts == 0:
           raise unittest.SkipTest("No XenServer available with GPU Drivers installed")

        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        #Creating Account 
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
            )
        self._cleanup = [
                         self.account
                        ]
    def setUp(self):
        self.testdata = self.testClient.getParsedTestDataConfig()["vgpu"]
        self.apiclient = self.testClient.getApiClient()

        # Get Zone, Domain and Default Built-in template
        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.testdata["mode"] = self.zone.networktype
        # Before running this test, register a windows template with ostype as 'Windows 7 (32-bit)'
        self.template = get_template(self.apiclient, self.zone.id, self.testdata["ostype"])

        if self.template == FAILED:
            assert False, "get_template() failed to return template with description %s" % self.testdata["ostype"]
        #create a user account
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )

        self.testdata["vgpu260q"]["zoneid"] = self.zone.id
        self.testdata["vgpu260q"]["template"] = self.template.id

        self.testdata["vgpu140q"]["zoneid"] = self.zone.id
        self.testdata["vgpu140q"]["template"] = self.template.id
        self.testdata["service_offerings"]["vgpu260qwin"]["serviceofferingdetails"] = [{'pciDevice': 'Group of NVIDIA Corporation GK107GL [GRID K1] GPUs'},
                                                                                       {'vgpuType':'GRID K120Q'}]
        #create a service offering
        self.service_offering = ServiceOffering.create(
                self.apiclient,
                self.testdata["service_offerings"]["vgpu260qwin"],
        )
        #build cleanup list
        self.cleanup = [
            self.service_offering,
            self.account
        ]

    @attr(tags = ['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_deploy_vgpu_enabled_vm(self):
        """Test Deploy Virtual Machine

        # Validate the following:
        # 1. Virtual Machine is accessible via SSH
        # 2. Virtual Machine is vGPU enabled (via SSH)
        # 3. listVirtualMachines returns accurate information
        """
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["vgpu260q"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.testdata['mode']
        )

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
        hosts = list_hosts(
               self.apiclient,
               id=vm.hostid
               )
        hostip = hosts[0].ipaddress
        try:
            sshClient = SshClient(host=hostip, port=22, user='root',passwd=self.testdata["host_password"])
            res = sshClient.execute("xe vgpu-list vm-name-label=%s params=type-uuid %s" % (
                                   vm.instancename
                                 ))
            self.debug("SSH result: %s" % res)
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (hostip, e)
                      )
        result = str(res)
        self.assertEqual(
                    result.count("type-uuid"),
                    1,
                    "VM is vGPU enabled."
                    )

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)
