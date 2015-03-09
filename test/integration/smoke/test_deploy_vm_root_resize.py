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
from marvin.cloudstackTestCase import cloudstackTestCase

#Import Integration Libraries

#base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import Account, VirtualMachine, ServiceOffering

#utils - utility classes for common cleanup, external library wrappers etc
from marvin.lib.utils import cleanup_resources

#common - commonly used methods for all tests are listed here
from marvin.lib.common import get_zone, get_domain, get_template, list_volumes

from marvin.codes import FAILED

from nose.plugins.attrib import attr

import re

class TestData(object):
    """Test data object that is required to create resources
    """
    def __init__(self):
        self.testdata = {
            #data to create an account
            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                "password": "password",
            },
            #data reqd for virtual machine creation
            "virtual_machine" : {
                "name" : "testvm",
                "displayname" : "Test VM",
            },
            #small service offering
            "service_offering": {
                "small": {
                    "name": "Small Instance",
                    "displaytext": "Small Instance",
                    "cpunumber": 1,
                    "cpuspeed": 100,
                    "memory": 256,
                },
            },
            "ostype": 'CentOS 5.3 (64-bit)',
        }

class TestDeployVM(cloudstackTestCase):
    """Test deploy a VM into a user account
    """

    def setUp(self):
        self.testdata = TestData().testdata
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()

        # Get Zone, Domain and Default Built-in template
        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.testdata["mode"] = self.zone.networktype
        self.template = get_template(self.apiclient, self.zone.id, self.testdata["ostype"])
        if self.template == FAILED:
            assert False, "get_template() failed to return template "
#       for testing with specific template
#        self.template = get_template(self.apiclient, self.zone.id, self.testdata["ostype"], templatetype='USER', services = {"template":'31f52a4d-5681-43f7-8651-ad4aaf823618'})
        

        #create a user account
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        #create a service offering
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.testdata["service_offering"]["small"]
        )
        #build cleanup list
        self.cleanup = [
            self.service_offering,
            self.account
        ]

    @attr(tags = ['advanced', 'basic', 'sg'], required_hardware="true")
    def test_00_deploy_vm_root_resize(self):
        """Test deploy virtual machine with root resize

        # Validate the following:
        # 1. listVirtualMachines returns accurate information
        # 2. root disk has new size per listVolumes
        # 3. Rejects non-supported hypervisor types
        """
        if(self.hypervisor.lower() == 'kvm'):
            newrootsize = (self.template.size >> 30) + 2
            self.virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.testdata["virtual_machine"],
                accountid=self.account.name,
                zoneid=self.zone.id,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                rootdisksize=newrootsize
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

            # get root vol from created vm, verify it is correct size
            list_volume_response = list_volumes(
                                                self.apiclient,
                                                virtualmachineid=self.virtual_machine.id,
                                                type='ROOT',
                                                listall=True
                                                )

            rootvolume = list_volume_response[0]
            success = False
            if rootvolume is not None and rootvolume.size  == (newrootsize << 30):
                success = True

            self.assertEqual(
                             success,
                             True,
                             "Check if the root volume resized appropriately"
                            )
        else:
            self.debug("hypervisor %s unsupported for test 00, verifying it errors properly" % self.hypervisor)

            newrootsize = (self.template.size >> 30) + 2
            success = False
            try:
                self.virtual_machine = VirtualMachine.create(
                    self.apiclient,
                    self.testdata["virtual_machine"],
                    accountid=self.account.name,
                    zoneid=self.zone.id,
                    domainid=self.account.domainid,
                    serviceofferingid=self.service_offering.id,
                    templateid=self.template.id,
                    rootdisksize=newrootsize
                )
            except Exception as ex:
                if re.search("Hypervisor \S+ does not support rootdisksize override", str(ex)):
                    success = True
                else:
                    self.debug("virtual machine create did not fail appropriately. Error was actually : " + str(ex));

            self.assertEqual(success, True, "Check if unsupported hypervisor %s fails appropriately" % self.hypervisor)

    @attr(tags = ['advanced', 'basic', 'sg'], required_hardware="true")
    def test_01_deploy_vm_root_resize(self):
        """Test proper failure to deploy virtual machine with rootdisksize of 0
        """
        if (self.hypervisor.lower() == 'kvm'):
            newrootsize = 0
            success = False
            try:
                self.virtual_machine = VirtualMachine.create(
                    self.apiclient,
                    self.testdata["virtual_machine"],
                    accountid=self.account.name,
                    zoneid=self.zone.id,
                    domainid=self.account.domainid,
                    serviceofferingid=self.service_offering.id,
                    templateid=self.template.id,
                    rootdisksize=newrootsize
                )
            except Exception as ex:
                if "rootdisk size should be a non zero number" in str(ex):
                    success = True
                else:
                    self.debug("virtual machine create did not fail appropriately. Error was actually : " + str(ex));

            self.assertEqual(success, True, "Check if passing 0 as rootdisksize fails appropriately")
        else:
            self.debug("test 01 does not support hypervisor type " + self.hypervisor);

    @attr(tags = ['advanced', 'basic', 'sg'], required_hardware="true", BugId="CLOUDSTACK-6984")
    def test_02_deploy_vm_root_resize(self):
        """Test proper failure to deploy virtual machine with rootdisksize less than template size
        """
        if (self.hypervisor.lower() == 'kvm'):
            newrootsize = (self.template.size >> 30) - 1

            self.assertEqual(newrootsize > 0, True, "Provided template is less than 1G in size, cannot run test")

            success = False
            try:
                self.virtual_machine = VirtualMachine.create(
                    self.apiclient,
                    self.testdata["virtual_machine"],
                    accountid=self.account.name,
                    zoneid=self.zone.id,
                    domainid=self.account.domainid,
                    serviceofferingid=self.service_offering.id,
                    templateid=self.template.id,
                    rootdisksize=newrootsize
                )
            except Exception as ex:
                if "rootdisksize override is smaller than template size" in str(ex):
                    success = True
                else:
                    self.debug("virtual machine create did not fail appropriately. Error was actually : " + str(ex));

            self.assertEqual(success, True, "Check if passing rootdisksize < templatesize fails appropriately")
        else:
            self.debug("test 01 does not support hypervisor type " + self.hypervisor);

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)
