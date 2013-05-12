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
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.remoteSSHClient import remoteSSHClient
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from nose.plugins.attrib import attr


_multiprocess_shared_ = True
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
            "service_offerings":
                {
                    "small":
                        {
                            # Small service offering ID to for change VM
                            # service offering from medium to small
                            "name": "Small Instance",
                            "displaytext": "Small Instance",
                            "cpunumber": 1,
                            "cpuspeed": 100,
                            "memory": 256,
                            },
                },
            "template": {
                "displaytext": "Cent OS Template",
                "name": "Cent OS Template",
                "passwordenabled": True,
                },
            "sleep": 60,
            "timeout": 10,
            "ostype": 'CentOS 5.3 (64-bit)',
            # CentOS 5.3 (64-bit)
        }

@unittest.skip("Additional test")
class TestVMPasswordEnabled(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
            TestVMPasswordEnabled,
            cls
        ).getClsTestClient().getApiClient()
        cls.services = Services().services

        # Get Zone, Domain and templates
        domain = get_domain(cls.api_client, cls.services)
        zone = get_zone(cls.api_client, cls.services)
        cls.services['mode'] = cls.zone.networktype
        template = get_template(
            cls.api_client,
            zone.id,
            cls.services["ostype"]
        )
        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = zone.id
        cls.services["small"]["template"] = template.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=domain.id
        )

        cls.small_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offerings"]["small"]
        )

        cls.virtual_machine = VirtualMachine.create(
            cls.api_client,
            cls.services["small"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.small_offering.id,
            mode=cls.services["mode"]
        )
        #Stop virtual machine
        cls.virtual_machine.stop(cls.api_client)

        # Poll listVM to ensure VM is stopped properly
        timeout = cls.services["timeout"]
        while True:
            time.sleep(cls.services["sleep"])

            # Ensure that VM is in stopped state
            list_vm_response = list_virtual_machines(
                cls.api_client,
                id=cls.virtual_machine.id
            )

            if isinstance(list_vm_response, list):

                vm = list_vm_response[0]
                if vm.state == 'Stopped':
                    break

            if timeout == 0:
                raise Exception(
                    "Failed to stop VM (ID: %s) in change service offering" %
                    vm.id)

            timeout = timeout - 1

        list_volume = list_volumes(
            cls.api_client,
            virtualmachineid=cls.virtual_machine.id,
            type='ROOT',
            listall=True
        )
        if isinstance(list_volume, list):
            cls.volume = list_volume[0]
        else:
            raise Exception(
                "Exception: Unable to find root volume foe VM: %s" %
                cls.virtual_machine.id)

        cls.services["template"]["ostype"] = cls.services["ostype"]
        #Create templates for Edit, Delete & update permissions testcases
        cls.pw_enabled_template = Template.create(
            cls.api_client,
            cls.services["template"],
            cls.volume.id,
            account=cls.account.name,
            domainid=cls.account.domainid
        )
        # Delete the VM - No longer needed
        cls.virtual_machine.delete(cls.api_client)
        cls.services["small"]["template"] = cls.pw_enabled_template.id

        cls.vm = VirtualMachine.create(
            cls.api_client,
            cls.services["small"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.small_offering.id,
            mode=cls.services["mode"]
        )
        cls._cleanup = [
            cls.small_offering,
            cls.pw_enabled_template,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        # Cleanup VMs, templates etc.
        cleanup_resources(cls.api_client, cls._cleanup)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        #Clean up, terminate the created instances
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "sg"])
    def test_11_get_vm_password(self):
        """Test get VM password for password enabled template"""

        # Validate the following
        # 1. Create an account
        # 2. Deploy VM with default service offering and "password enabled"
        #    template. Vm should be in running state.
        # 3. Stop VM deployed in step 2
        # 4. Reset VM password. SSH with new password should be successful

        self.debug("Stopping VM: %s" % self.vm.name)
        self.vm.stop(self.apiclient)

        # Sleep to ensure VM is stopped properly
        time.sleep(self.services["sleep"])

        self.debug("Resetting VM password for VM: %s" % self.vm.name)
        password = self.vm.resetPassword(self.apiclient)
        self.debug("Password reset to: %s" % password)

        self.debug("Starting VM to verify new password..")
        self.vm.start(self.apiclient)
        self.debug("VM - %s stated!" % self.vm.name)

        vms = VirtualMachine.list(self.apiclient, id=self.vm.id, listall=True)
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List VMs should retun valid response for VM: %s" % self.vm.name
        )
        virtual_machine = vms[0]

        self.assertEqual(
            virtual_machine.state,
            "Running",
            "VM state should be running"
        )
        try:
            self.debug("SSHing into VM: %s" % self.vm.ssh_ip)
            self.vm.password = password
            ssh = self.vm.get_ssh_client()
        except Exception as e:
            self.fail("SSH into VM: %s failed" % self.vm.ssh_ip)
        return
