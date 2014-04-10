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
from marvin.lib.common import get_zone, get_domain, get_template

from marvin.codes import FAILED

from nose.plugins.attrib import attr


class Services:
    """Test VM Life Cycle Services
    """

    def __init__(self):
        self.services = {
                "disk_offering":{
                    "displaytext": "Small",
                    "name": "Small",
                    "disksize": 1
                },
                "account": {
                    "email": "test@test.com",
                    "firstname": "Test",
                    "lastname": "User",
                    "username": "test",
                    # Random characters are appended in create account to
                    # ensure unique username generated each time
                    "password": "password",
                },
                "vgpu260q":   # Create a virtual machine instance with vgpu type as 260q
                {
                    "displayname": "testserver",
                    "username": "root", # VM creds for SSH
                    "password": "password",
                    "ssh_port": 22,
                    "hypervisor": 'XenServer',
                    "privateport": 22,
                    "publicport": 22,
                    "protocol": 'TCP',
                },
                "vgpu140q":   # Create a virtual machine instance with vgpu type as 140q
                {
                    "displayname": "testserver",
                    "username": "root",
                    "password": "password",
                    "ssh_port": 22,
                    "hypervisor": 'XenServer',
                    "privateport": 22,
                    "publicport": 22,
                    "protocol": 'TCP',
                },
                "service_offerings":
                {
                 "vgpu260qwin":
                   {
                        "name": "Windows Instance with vGPU260Q",
                        "displaytext": "Windows Instance with vGPU260Q",
                        "cpunumber": 2,
                        "cpuspeed": 1600, # in MHz
                        "memory": 3072, # In MBs
                    },
                 "vgpu140qwin":
                    {
                     # Small service offering ID to for change VM
                     # service offering from medium to small
                        "name": "Windows Instance with vGPU140Q",
                        "displaytext": "Windows Instance with vGPU140Q",
                        "cpunumber": 2,
                        "cpuspeed": 1600,
                        "memory": 3072,
                    }
                },
            "diskdevice": ['/dev/vdc',  '/dev/vdb', '/dev/hdb', '/dev/hdc', '/dev/xvdd', '/dev/cdrom', '/dev/sr0', '/dev/cdrom1' ],
            # Disk device where ISO is attached to instance
            "mount_dir": "/mnt/tmp",
            "sleep": 60,
            "timeout": 10,
            #Migrate VM to hostid
            "ostype": 'Windows 7 (32-bit)',
            # CentOS 5.3 (64-bit)
        }


class TestDeployvGPUenabledVM(cloudstackTestCase):
    """Test deploy a vGPU enabled VM into a user account
    """

    def setUp(self):
        self.services = Services().services
        self.apiclient = self.testClient.getApiClient()

        # Get Zone, Domain and Default Built-in template
        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.services["mode"] = self.zone.networktype
        # Before running this test, register a windows template with ostype as 'Windows 7 (32-bit)'
        self.services["ostype"] = 'Windows 7 (32-bit)'
        self.template = get_template(self.apiclient, self.zone.id, self.services["ostype"])

        if self.template == FAILED:
            assert False, "get_template() failed to return template with description %s" % self.services["ostype"]
        #create a user account
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id
        )

        self.services["vgpu260q"]["zoneid"] = self.zone.id
        self.services["vgpu260q"]["template"] = self.template.id

        self.services["vgpu140q"]["zoneid"] = self.zone.id
        self.services["vgpu140q"]["template"] = self.template.id
        #create a service offering
        self.service_offering = ServiceOffering.create(
                self.apiclient,
                self.services["service_offerings"]["vgpu260qwin"],
                serviceofferingdetails={'pciDevice': 'VGPU'}
        )
        #build cleanup list
        self.cleanup = [
            self.service_offering,
            self.account
        ]

    @attr(tags = ['advanced', 'simulator', 'basic', 'vgpu', 'provisioning'])
    def test_deploy_vgpu_enabled_vm(self):
        """Test Deploy Virtual Machine

        # Validate the following:
        # 1. Virtual Machine is accessible via SSH
        # 2. Virtual Machine is vGPU enabled (via SSH)
        # 3. listVirtualMachines returns accurate information
        """
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["vgpu260q"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.services['mode']
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
        list_hosts = list_hosts(
               self.apiclient,
               id=vm.hostid
               )
        hostip = list_hosts[0].ipaddress
        try:
            sshClient = SshClient(host=hostip, port=22, user='root',passwd=self.services["host_password"])
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