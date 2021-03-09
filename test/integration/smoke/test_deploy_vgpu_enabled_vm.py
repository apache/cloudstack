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

# Test from the Marvin - Testing in Python wiki

# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

# Import Integration Libraries

# base - contains all resources as entities and defines create, delete,
# list operations on them
from marvin.lib.base import Account, VirtualMachine, ServiceOffering, NetworkOffering, Network, Template

# utils - utility classes for common cleanup, external library wrappers etc
from marvin.lib.utils import cleanup_resources, get_hypervisor_type, validateList

# common - commonly used methods for all tests are listed here
from marvin.lib.common import get_zone, get_domain, get_template, list_hosts

from marvin.sshClient import SshClient

from marvin.codes import FAILED, PASS

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
        self.hostConfig = self.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__
        self._cleanup = []
        self.unsupportedHypervisor = False
        self.noSuitableHost = False
        # Need to add check whether zone containing the xen hypervisor or not
        # as well
        hosts = list_hosts(
            self.apiclient,
            hypervisor="XenServer"
        )
        if hosts is None:
             # GPU feature is supported only on XenServer.Check listhosts response
             self.unsupportedHypervisor = True
             return
        else:
            gpuhosts = 0
            for ghost in hosts:
                if ghost.hypervisorversion >= "6.2.0":
                    sshClient = SshClient(
                        host=ghost.ipaddress,
                        port=self.testdata['configurableData']['host']["publicport"],
                        user=self.hostConfig['username'],
                        passwd=self.hostConfig['password'])
                    if ghost.hypervisorversion == "6.2.0":
                        res = sshClient.execute(
                            "xe patch-list uuid=0850b186-4d47-11e3-a720-001b2151a503")
                        if len(res) == 0:
                            continue
                    res = sshClient.execute(
                        "xe vgpu-type-list model-name=\"GRID K120Q\"")
                    if len(res) != 0:
                        gpuhosts = gpuhosts + 1
                    else:
                        continue
        if gpuhosts == 0:
            # No XenServer available with GPU Drivers installed
            self.noSuitableHost = True
            return
        
    def setUp(self):
        self.testdata = self.testClient.getParsedTestDataConfig()["vgpu"]
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        if self.noSuitableHost or self.unsupportedHypervisor:
            self.hypervisor = get_hypervisor_type(self.apiclient)
            if self.hypervisor.lower() not in ["vmware"]:
	            self.skipTest("Skipping test because suitable hypervisor/host not\
        	            present")
            self.testdata = self.testClient.getParsedTestDataConfig()
        
        self.cleanup = []

        # Get Zone, Domain and Default Built-in template
        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        # Creating Account
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )

        if self.hypervisor.lower() in ["xenserver"]:
            
            # Before running this test for Xen Server, register a windows template with ostype as
            # 'Windows 7 (32-bit)'
            self.template = get_template(
                self.apiclient,
                self.zone.id,
                self.testdata["ostype"])
            self.cleanup.append(self.template)
            
            self.testdata["mode"] = self.zone.networktype

            if self.template == FAILED:
        	    assert False, "get_template() failed to return template with description %s" % self.testdata[
                	"ostype"]

            self.testdata["small"]["zoneid"] = self.zone.id
            self.testdata["small"]["template"] = self.template.id

            self.testdata["service_offerings"]["vgpu260qwin"]["serviceofferingdetails"] = [
        	    {
                	'pciDevice': 'Group of NVIDIA Corporation GK107GL [GRID K1] GPUs'}, {
	                'vgpuType': 'GRID K120Q'}]
        	# create a service offering
            self.service_offering = ServiceOffering.create(
        	    self.apiclient,
	            self.testdata["service_offerings"]["vgpu260qwin"],
        	)
            self.cleanup.append(self.service_offering)
            
        elif self.hypervisor.lower() in ["vmware"]:
            self.testdata["isolated_network"]["zoneid"] = self.zone.id
            
            self.userapiclient = self.testClient.getUserApiClient(
         		UserName=self.account.name,
		        DomainName=self.account.domain
	        )
            self.service_offering = ServiceOffering.create(
	            self.apiclient,
        	    self.testdata["service_offering"])

            # Create Shared Network Offering
            self.isolated_network_offering = NetworkOffering.create(
	            self.apiclient,
        	    self.testdata["isolated_network_offering"])
        	# Enable Isolated Network offering
            self.isolated_network_offering.update(self.apiclient, state='Enabled')
            
            # Register a private template in the account with nic adapter vmxnet3
            # Also add required 3D GPU details for enabling it
            self.template = Template.register(
                self.userapiclient,
                self.testdata["configurableData"]["vmxnet3template"],
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid,
                details=[{"mks.enable3d" : "true", "mks.use3dRenderer" : "automatic",
                     "svga.autodetect" : "false", "svga.vramSize" : "131072"}]
            )
		

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_deploy_vgpu_enabled_vm(self):
        """Test Deploy Virtual Machine

        # Validate the following:
        # 1. Virtual Machine is accessible via SSH
        # 2. Virtual Machine is vGPU enabled (via SSH)
        # 3. listVirtualMachines returns accurate information
        """
        if self.hypervisor.lower() not in ["xenserver"]:
            self.cleanup.append(self.account)
            self.skipTest("This test case is written specifically\
                    for XenServer hypervisor")
            
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.testdata['mode']
        )
        self.cleanup.append(self.virtual_machine)

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
        hosts = list_hosts(
            self.apiclient,
            id=vm.hostid
        )
        hostip = hosts[0].ipaddress
        try:
            sshClient = SshClient(
                host=hostip,
                port=self.testdata['configurableData']['host']["publicport"],
                user=self.testdata['configurableData']['host']["username"],
                passwd=self.testdata['configurableData']['host']["password"])
            res = sshClient.execute(
                "xe vgpu-list vm-name-label=%s params=type-uuid %s" % 
                (vm.instancename))
            self.debug("SSH result: %s" % res)
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % 
                      (hostip, e)
                      )
        result = str(res)
        self.assertEqual(
            result.count("type-uuid"),
            1,
            "VM is vGPU enabled."
        )
        self.cleanup.append(self.account)

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)
        return

    @attr(tags=["advanced"])
    def test_3d_gpu_support(self):
        """Test 3D GPU support

        # 1. Register a template for VMware with nicAdapter vmxnet3 and 3D GPU details
        # 2. Deploy a VM using this template
        # 3. Create an isolated network
        # 4. Add network to VM
        # 5. Verify vm details for 3D GPU details
        """
        if self.hypervisor.lower() not in ["vmware"]:
            self.cleanup.append(self.account)
            self.skipTest("This test case is written specifically\
                    for Vmware hypervisor")

        self.template.download(self.apiclient)

        templates = Template.list(
            self.userapiclient,
            listall=True,
            id=self.template.id,
            templatefilter="self"
        )

        self.assertEqual(
            validateList(templates)[0],
            PASS,
            "Templates list validation failed"
        )

        self.testdata["virtual_machine"]["zoneid"] = self.zone.id
        self.testdata["virtual_machine"]["template"] = self.template.id

        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            templateid=self.template.id,
            serviceofferingid=self.service_offering.id)

        self.isolated_network = Network.create(
            self.apiclient,
            self.testdata["isolated_network"],
            self.account.name,
            self.account.domainid,
            networkofferingid=self.isolated_network_offering.id)

        self.virtual_machine.add_nic(self.apiclient, self.isolated_network.id)
        
        self.cleanup = [self.virtual_machine, self.isolated_network, self.isolated_network_offering, self.service_offering, self.account, self.template]

        qresultset = self.dbclient.execute("select id from vm_instance where uuid = '%s';" % self.virtual_machine.id)
        vm_id = qresultset[0]
        qresultset = self.dbclient.execute("select name, value from user_vm_details where vm_id = '%d';" % vm_id)
        detailKeys = [x[0] for x in qresultset]

        self.assertTrue('mks.enable3d' in detailKeys and 'mks.use3dRenderer' in detailKeys and 'svga.autodetect' in detailKeys and 'svga.vramSize' in detailKeys, "VM details do not contain 3D GPU details")

        self.assertEqual('true', qresultset[detailKeys.index('mks.enable3d')][1], "Expected detail 'mks.enable3d'='true'")

        self.assertEqual('automatic', qresultset[detailKeys.index('mks.use3dRenderer')][1], "Expected detail 'mks.use3dRenderer'='automatic'")

        self.assertEqual('false', qresultset[detailKeys.index('svga.autodetect')][1], "Expected detail 'svga.autodetect'='false'")

        self.assertEqual('131072', qresultset[detailKeys.index('svga.vramSize')][1], "Expected detail 'svga.vramSize'='131072'")

        return
