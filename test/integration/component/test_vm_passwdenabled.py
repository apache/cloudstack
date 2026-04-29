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
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             EgressFireWallRule,
                             Template)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_virtual_machines,
                               list_volumes)
from nose.plugins.attrib import attr
import time


_multiprocess_shared_ = True


class Services:

    """Test VM Life Cycle Services
    """

    def __init__(self):
        self.services = {
            "disk_offering": {
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
            "small":
            # Create a small virtual machine instance with disk offering
                {
                    "displayname": "testserver",
                    "username": "root",  # VM creds for SSH
                    "password": "password",
                    "ssh_port": 22,
                    "hypervisor": 'XenServer',
                    "privateport": 22,
                    "publicport": 22,
                    "protocol": 'TCP',
            },
            "egress": {
                    "name": 'web',
                    "protocol": 'TCP',
                    "startport": 80,
                    "endport": 80,
                    "cidrlist": '0.0.0.0/0',
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


class TestVMPasswordEnabled(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVMPasswordEnabled, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        domain = get_domain(cls.api_client)
        zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = zone.networktype
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

        networkid = cls.virtual_machine.nic[0].networkid
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['lxc']:
            raise unittest.SkipTest("template creation is not supported on %s" % cls.hypervisor)

        # create egress rule to allow wget of my cloud-set-guest-password
        # script
        if zone.networktype.lower() == 'advanced':
            EgressFireWallRule.create(
                cls.api_client,
                networkid=networkid,
                protocol=cls.services["egress"]["protocol"],
                startport=cls.services["egress"]["startport"],
                endport=cls.services["egress"]["endport"],
                cidrlist=cls.services["egress"]["cidrlist"])

        cls.virtual_machine.password = cls.services["small"]["password"]
        ssh = cls.virtual_machine.get_ssh_client()

        # below steps are required to get the new password from VR
        # (reset password)
        # http://cloudstack.org/dl/cloud-set-guest-password
        # Copy this file to /etc/init.d
        # chmod +x /etc/init.d/cloud-set-guest-password
        # chkconfig --add cloud-set-guest-password

        cmds = [
            "cd /etc/init.d;wget http://people.apache.org/~tsp/cloud-set-guest-password",
            "chmod +x /etc/init.d/cloud-set-guest-password",
            "chkconfig --add cloud-set-guest-password",
        ]
        for c in cmds:
            ssh.execute(c)

        # Adding delay of 120 sec to avoid data loss due to timing issue
        time.sleep(120)

        # Stop virtual machine
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
                    "Failed to stop VM (ID: %s) " %
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
                "Exception: Unable to find root volume for VM: %s" %
                cls.virtual_machine.id)

        cls.services["template"]["ostype"] = cls.services["ostype"]
        cls.services["template"]["ispublic"] = True
        # Create templates for Edit, Delete & update permissions testcases
        cls.pw_enabled_template = Template.create(
            cls.api_client,
            cls.services["template"],
            cls.volume.id,
        )
        # Delete the VM - No longer needed
        cls.virtual_machine.delete(cls.api_client, expunge=True)
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
        # Clean up, terminate the created instances
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "sg"],
        required_hardware="true")
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
            "List VMs should return valid response for VM: %s" % self.vm.name
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
            self.vm.get_ssh_client()
        except Exception as e:
            self.fail("SSH into VM: %s failed: %s" %
                      (self.vm.ssh_ip, e))
        return
