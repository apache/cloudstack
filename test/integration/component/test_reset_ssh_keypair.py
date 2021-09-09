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

""" P1 tests for reset SSH keypair
"""

#Import Local Modules
from marvin.lib.base import (VirtualMachine,
                                         SSHKeyPair,
                                         Account,
                                         Template,
                                         ServiceOffering,
                                         EgressFireWallRule,
                                         Volume)
from marvin.lib.common import (get_domain,
                                           get_zone,
                                           get_template)
from marvin.lib.utils import (cleanup_resources,
                                          random_gen,
                                          validateList)
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.codes import PASS, RUNNING

#Import System modules
import tempfile
import os
from nose.plugins.attrib import attr
import time


class Services:
    """Test remote SSH client Services """

    def __init__(self):
        self.services = {
                "account": {
                        "email": "test@test.com",
                        "firstname": "Test",
                        "lastname": "User",
                        "username": "test",
                        # Random characters are appended in create account to
                        # ensure unique username generated each time
                        "password": "password",
                },
                "virtual_machine": {
                        "displayname": "VM",
                        "username": "root",
                        # VM creds for SSH
                        "password": "password",
                        "ssh_port": 22,
                        "hypervisor": 'XenServer',
                        "privateport": 22,
                        "publicport": 22,
                        "protocol": 'TCP',
                },
                "service_offering": {
                        "name": "Tiny Instance",
                        "displaytext": "Tiny Instance",
                        "cpunumber": 1,
                        "cpuspeed": 100,
                        "memory": 128,
                },
                "egress": {
                    "name": 'web',
                    "protocol": 'TCP',
                    "startport": 80,
                    "endport": 80,
                    "cidrlist": '0.0.0.0/0',
                },
                "template": {
                    "displaytext": "Cent OS Template",
                    "name": "Cent OS Template",
                    "passwordenabled": True,
                    "ispublic": True,
                },
            "ostype": 'CentOS 5.3 (64-bit)',
            # Cent OS 5.3 (64 bit)
            "SSHEnabledTemplate": "SSHkey",
            "SSHPasswordEnabledTemplate": "SSHKeyPassword",
            "sleep": 60,
            "timeout": 20,
            "mode": '',
        }

def wait_vm_start(apiclient, vmid, timeout, sleep):
    while timeout:
        vms = VirtualMachine.list(apiclient, id=vmid)
        vm_list_validation_result = validateList(vms)
        if vm_list_validation_result[0] == PASS and vm_list_validation_result[1].state == RUNNING:
            return timeout
        time.sleep(sleep)
        timeout = timeout - 1

    return timeout

def SetPublicIpForVM(apiclient, vm):
    """ List VM and set the publicip (if available) of VM
    to ssh_ip attribute"""

    vms = VirtualMachine.list(apiclient, id=vm.id, listall=True)
    virtual_machine = vms[0]
    if hasattr(vm, "publicip"):
        vm.ssh_ip = virtual_machine.publicip
    return vm

class TestResetSSHKeypair(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestResetSSHKeypair, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        # Set Zones and disk offerings
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        # Create Account, VMs, NAT Rules etc
        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        # Set Zones and disk offerings
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id

        cls._cleanup = []
        try:
            # Create VMs, NAT Rules etc
            cls.account = Account.create(
                                cls.api_client,
                                cls.services["account"],
                                domainid=domain.id)
            cls._cleanup.append(cls.account)

            cls.service_offering = ServiceOffering.create(
                                        cls.api_client,
                                        cls.services["service_offering"])
            cls._cleanup.append(cls.service_offering)

            cls.virtual_machine = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["virtual_machine"],
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    serviceofferingid=cls.service_offering.id,
                                    mode=cls.services["mode"])

            networkid = cls.virtual_machine.nic[0].networkid

            # create egress rule to allow wget of my cloud-set-guest-password script
            if cls.zone.networktype.lower() == 'advanced':
                EgressFireWallRule.create(cls.api_client,
                                  networkid=networkid,
                                  protocol=cls.services["egress"]["protocol"],
                                  startport=cls.services["egress"]["startport"],
                                  endport=cls.services["egress"]["endport"],
                                  cidrlist=cls.services["egress"]["cidrlist"])

            cls.virtual_machine.password = cls.services["virtual_machine"]["password"]
            ssh = cls.virtual_machine.get_ssh_client()

            # below steps are required to get the new password from VR(reset password)
            # http://cloudstack.org/dl/cloud-set-guest-password
            # Copy this file to /etc/init.d
            # chmod +x /etc/init.d/cloud-set-guest-password
            # chkconfig --add cloud-set-guest-password
            # similar steps to get SSH key from web so as to make it ssh enabled

            cmds = [
                "cd /etc/init.d;wget http://people.apache.org/~tsp/cloud-set-guest-password",
                "chmod +x /etc/init.d/cloud-set-guest-password",
                "chkconfig --add cloud-set-guest-password",
                "cd /etc/init.d;wget http://downloads.sourceforge.net/project/cloudstack/SSH%20Key%20Gen%20Script/" + \
                "cloud-set-guest-sshkey.in?r=http%3A%2F%2Fsourceforge" + \
                ".net%2Fprojects%2Fcloudstack%2Ffiles%2FSSH%2520Key%2520Gen%2520Script%2F&ts=1331225219&use_mirror=iweb",
                "chmod +x /etc/init.d/cloud-set-guest-sshkey.in",
                "chkconfig --add cloud-set-guest-sshkey.in"
                ]
            for c in cmds:
                ssh.execute(c)

            # Adding delay of 120 sec to avoid data loss due to timing issue
            time.sleep(120)

            #Stop virtual machine
            cls.virtual_machine.stop(cls.api_client)

            list_volume = Volume.list(
                            cls.api_client,
                            virtualmachineid=cls.virtual_machine.id,
                            type='ROOT',
                            listall=True)

            if isinstance(list_volume, list):
                cls.volume = list_volume[0]
            else:
                raise Exception(
                "Exception: Unable to find root volume for VM: %s" %
                cls.virtual_machine.id)

            cls.services["template"]["ostype"] = cls.services["ostype"]
            #Create templates for Edit, Delete & update permissions testcases
            cls.pw_ssh_enabled_template = Template.create(
                cls.api_client,
                cls.services["template"],
                cls.volume.id,
                account=cls.account.name,
                domainid=cls.account.domainid
            )
            cls._cleanup.append(cls.pw_ssh_enabled_template)
            # Delete the VM - No longer needed
            cls.virtual_machine.delete(cls.api_client, expunge=True)
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Exception in setUpClass: %s" % e)

    @classmethod
    def tearDownClass(cls):
        # Cleanup VMs, templates etc.
        cleanup_resources(cls.api_client, cls._cleanup)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

        self.keypair = SSHKeyPair.create(
                                    self.apiclient,
                                    name=random_gen() + ".pem",
                                    account=self.account.name,
                                    domainid=self.account.domainid
                                 )
        # Cleanup
        self.cleanup = []
        self.tmp_files = []

    def tearDown(self):
        try:
            #Clean up, terminate the created accounts, domains etc
            #cleanup_resources(self.apiclient, self.cleanup)
            for tmp_file in self.tmp_files:
                os.remove(tmp_file)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["simulator", "basic", "advanced"])
    def test_01_reset_ssh_keys(self):
        """Test Reset SSH keys for VM  already having SSH key"""

        # Validate the following
        # 1. Create a VM  having SSH keyPair
        # 2. Stop the VM
        # 3. Reset SSH key pair. Verify VM is not restarted automatically as
        #    result of API execution. User should be able to ssh into the VM
        #    using new keypair when VM is restarted

        # Spawn an instance
        virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    templateid=self.pw_ssh_enabled_template.id,
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    zoneid=self.zone.id,
                                    serviceofferingid=self.service_offering.id,
                                    keypair=self.keypair.name,
                                    mode=self.services["mode"]
                                    )

        self.debug("Check if the VM is properly deployed or not?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=virtual_machine.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return the valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "VM state should be running after deployment"
                         )

        self.debug("Stopping the virtual machine")
        try:
            virtual_machine.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop virtual machine: %s, %s" %
                                                    (virtual_machine.id, e))

        self.debug("Creating a new SSH keypair for account: %s" %
                                                    self.account.name)
        new_keypair = SSHKeyPair.create(
                                    self.apiclient,
                                    name=random_gen() + ".pem",
                                    account=self.account.name,
                                    domainid=self.account.domainid
                                 )
        self.debug("Created a new keypair with name: %s" % new_keypair.name)

        self.debug("Writing the private key to local file")
        keyPairFilePath = tempfile.gettempdir() + os.sep + new_keypair.name
        # Clenaup at end of execution
        self.tmp_files.append(keyPairFilePath)

        self.debug("File path: %s" % keyPairFilePath)

        f = open(keyPairFilePath, "w+")
        f.write(new_keypair.privatekey)
        f.close()

        os.system("chmod 400 " + keyPairFilePath)

        self.debug("Resetting the SSH key pair for instance: %s" %
                                                        virtual_machine.name)
        try:
            virtual_machine.resetSshKey(
                                        self.apiclient,
                                        keypair=new_keypair.name,
                                        name=new_keypair.name,
                                        account=self.account.name,
                                        domainid=self.account.domainid
                                        )
        except Exception as e:
            self.fail("Failed to reset SSH key: %s, %s" %
                                                (virtual_machine.name, e))
        self.debug("Starting the virtual machine after resetting the keypair")
        try:
            virtual_machine.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start virtual machine: %s, %s" %
                                                    (virtual_machine.name, e))

        timeout = wait_vm_start(self.apiclient, virtual_machine.id, self.services["timeout"],
                            self.services["sleep"])

        if timeout == 0:
            self.fail("The virtual machine %s failed to start even after %s minutes"
                   % (virtual_machine.name, self.services["timeout"]))

        self.debug("SSH key path: %s" % str(keyPairFilePath))

        # In case of EIP setup, public IP changes after VM start operation
        # Assign the new publicip of the VM to its ssh_ip attribute
        # so that correct IP address is used for getting the ssh client of VM
        virtual_machine = SetPublicIpForVM(self.apiclient, virtual_machine)

        try:
            virtual_machine.get_ssh_client(keyPairFileLocation=str(keyPairFilePath))
        except Exception as e:
            self.fail("Failed to SSH into VM with new keypair: %s, %s" %
                                                    (virtual_machine.name, e))
        virtual_machine.delete(self.apiclient, expunge=True)
        return

    @attr(tags=["simulator", "basic", "advanced"])
    def test_02_reset_ssh_key_password_enabled_template(self):
        """Reset SSH keys for VM  created from password enabled template and
            already having SSH key """

        # Validate the following
        # 1. Create VM from password enabled template and having SSH keyPair
        # 2. Stop the VM
        # 3. Reset SSH key pair. Verify VM is not restarted automatically
        #    as a result of API execution
        #    User should be able to ssh into the VM  using new keypair
        #    User should be able to login into VM using new password
        #    returned by  the API

        self.debug("Deploying the virtual machine in default network offering")

        # Spawn an instance
        virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    templateid=self.pw_ssh_enabled_template.id,
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    zoneid=self.zone.id,
                                    serviceofferingid=self.service_offering.id,
                                    keypair=self.keypair.name,
                                    mode=self.services["mode"]
                                    )

        self.debug("Check if the VM is properly deployed or not?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=virtual_machine.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return the valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "VM state should be running after deployment"
                         )
        self.debug("Stopping the virtual machine")
        try:
            virtual_machine.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop virtual machine: %s, %s" %
                                                    (virtual_machine.id, e))

        self.debug("Creating a new SSH keypair for account: %s" %
                                                    self.account.name)
        new_keypair = SSHKeyPair.create(
                                    self.apiclient,
                                    name=random_gen() + ".pem",
                                    account=self.account.name,
                                    domainid=self.account.domainid
                                 )
        self.debug("Created a new keypair with name: %s" % new_keypair.name)

        self.debug("Writing the private key to local file")
        keyPairFilePath = tempfile.gettempdir() + os.sep + new_keypair.name

        self.debug("File path: %s" % keyPairFilePath)

        f = open(keyPairFilePath, "w+")
        f.write(new_keypair.privatekey)
        f.close()

        os.system("chmod 400 " + keyPairFilePath)

        self.debug("Resetting the SSH key pair for instance: %s" %
                                                        virtual_machine.name)
        try:
            virtual_machine.resetSshKey(
                                        self.apiclient,
                                        keypair=new_keypair.name,
                                        name=new_keypair.name,
                                        account=self.account.name,
                                        domainid=self.account.domainid
                                        )
        except Exception as e:
            self.fail("Failed to reset SSH key: %s, %s" %
                                                (virtual_machine.name, e))
        self.debug("Starting the virtual machine after resetting the keypair")
        try:
            virtual_machine.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start virtual machine: %s, %s" %
                                                    (virtual_machine.name, e))

        timeout = wait_vm_start(self.apiclient, virtual_machine.id, self.services["timeout"],
                            self.services["sleep"])

        if timeout == 0:
            self.fail("The virtual machine %s failed to start even after %s minutes"
                   % (virtual_machine.name, self.services["timeout"]))

        # In case of EIP setup, public IP changes after VM start operation
        # Assign the new publicip of the VM to its ssh_ip attribute
        # so that correct IP address is used for getting the ssh client of VM
        virtual_machine = SetPublicIpForVM(self.apiclient, virtual_machine)

        self.debug("SSHing with new keypair")
        try:
            virtual_machine.get_ssh_client(
                                    keyPairFileLocation=str(keyPairFilePath))
        except Exception as e:
            self.fail("Failed to SSH into VM with new keypair: %s, %s" %
                                                    (virtual_machine.name, e))

        try:
            self.debug("SSHing using password")
            virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail("Failed to SSH into VM with password: %s, %s" %
                                                    (virtual_machine.name, e))
        virtual_machine.delete(self.apiclient, expunge=True)
        return

    @attr(tags=["simulator", "basic", "advanced"])
    def test_03_reset_ssh_with_no_key(self):
        """Reset SSH key for VM  having no SSH key"""

        # Validate the following
        # 1.Create a VM
        # 2. Stop the VM
        # 3. Reset SSH key pair

        # Spawn an instance
        virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    templateid=self.pw_ssh_enabled_template.id,
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    zoneid=self.zone.id,
                                    serviceofferingid=self.service_offering.id,
                                    mode=self.services["mode"]
                                    )

        self.debug("Check if the VM is properly deployed or not?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=virtual_machine.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return the valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "VM state should be running after deployment"
                         )
        self.debug("Stopping the virtual machine")
        try:
            virtual_machine.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop virtual machine: %s, %s" %
                                                    (virtual_machine.id, e))

        self.debug("Creating a new SSH keypair for account: %s" %
                                                    self.account.name)
        new_keypair = SSHKeyPair.create(
                                    self.apiclient,
                                    name=random_gen() + ".pem",
                                    account=self.account.name,
                                    domainid=self.account.domainid
                                 )
        self.debug("Created a new keypair with name: %s" % new_keypair.name)

        self.debug("Writing the private key to local file")
        keyPairFilePath = tempfile.gettempdir() + os.sep + new_keypair.name

        self.debug("File path: %s" % keyPairFilePath)

        f = open(keyPairFilePath, "w+")
        f.write(new_keypair.privatekey)
        f.close()

        os.system("chmod 400 " + keyPairFilePath)

        self.debug("Resetting the SSH key pair for instance: %s" %
                                                        virtual_machine.name)
        try:
            virtual_machine.resetSshKey(
                                        self.apiclient,
                                        keypair=new_keypair.name,
                                        name=new_keypair.name,
                                        account=self.account.name,
                                        domainid=self.account.domainid
                                        )
        except Exception as e:
            self.fail("Failed to reset SSH key: %s, %s" %
                                                (virtual_machine.name, e))
        self.debug("Starting the virtual machine after resetting the keypair")
        try:
            virtual_machine.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start virtual machine: %s, %s" %
                                                    (virtual_machine.name, e))

        timeout = wait_vm_start(self.apiclient, virtual_machine.id, self.services["timeout"],
                            self.services["sleep"])

        if timeout == 0:
            self.fail("The virtual machine %s failed to start even after %s minutes"
                   % (virtual_machine.name, self.services["timeout"]))

        # In case of EIP setup, public IP changes after VM start operation
        # Assign the new publicip of the VM to its ssh_ip attribute
        # so that correct IP address is used for getting the ssh client of VM
        virtual_machine = SetPublicIpForVM(self.apiclient, virtual_machine)

        self.debug("SSHing with new keypair")
        try:
            virtual_machine.get_ssh_client(
                                    keyPairFileLocation=str(keyPairFilePath))
        except Exception as e:
            self.fail("Failed to SSH into VM with new keypair: %s, %s" %
                                                    (virtual_machine.name, e))
        virtual_machine.delete(self.apiclient, expunge=True)
        return

    @attr(tags=["simulator", "basic", "advanced"])
    def test_04_reset_key_passwd_enabled_no_key(self):
        """Reset SSH keys for VM  created from password enabled template and
            have no previous SSH key"""

        # Validate the following
        # 1.Create a VM from password enabled template
        # 2. Stop the VM
        # 3. Reset SSH key pair. Verify VM is not restarted automatically as a
        #    result of API execution
        #    User should be able to ssh into the VM  using new keypair when
        #    VM is restarted
        #    User should be able to  login into VM using new password returned
        #    by the API

        self.debug("Deploying the virtual machine in default network offering")

        # Spawn an instance
        virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    templateid=self.pw_ssh_enabled_template.id,
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    zoneid=self.zone.id,
                                    serviceofferingid=self.service_offering.id,
                                    mode=self.services["mode"]
                                    )

        self.debug("Check if the VM is properly deployed or not?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=virtual_machine.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return the valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "VM state should be running after deployment"
                         )
        self.debug("Stopping the virtual machine")
        try:
            virtual_machine.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop virtual machine: %s, %s" %
                                                    (virtual_machine.id, e))

        self.debug("Creating a new SSH keypair for account: %s" %
                                                    self.account.name)
        new_keypair = SSHKeyPair.create(
                                    self.apiclient,
                                    name=random_gen() + ".pem",
                                    account=self.account.name,
                                    domainid=self.account.domainid
                                 )
        self.debug("Created a new keypair with name: %s" % new_keypair.name)

        self.debug("Writing the private key to local file")
        keyPairFilePath = tempfile.gettempdir() + os.sep + new_keypair.name

        self.debug("File path: %s" % keyPairFilePath)

        f = open(keyPairFilePath, "w+")
        f.write(new_keypair.privatekey)
        f.close()

        os.system("chmod 400 " + keyPairFilePath)

        self.debug("Resetting the SSH key pair for instance: %s" %
                                                        virtual_machine.name)
        try:
            virtual_machine.resetSshKey(
                                        self.apiclient,
                                        keypair=new_keypair.name,
                                        name=new_keypair.name,
                                        account=self.account.name,
                                        domainid=self.account.domainid
                                        )
        except Exception as e:
            self.fail("Failed to reset SSH key: %s, %s" %
                                                (virtual_machine.name, e))
        self.debug("Starting the virtual machine after resetting the keypair")
        try:
            virtual_machine.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start virtual machine: %s, %s" %
                                                    (virtual_machine.name, e))

        timeout = wait_vm_start(self.apiclient, virtual_machine.id, self.services["timeout"],
                            self.services["sleep"])

        if timeout == 0:
            self.fail("The virtual machine %s failed to start even after %s minutes"
                   % (virtual_machine.name, self.services["timeout"]))

        # In case of EIP setup, public IP changes after VM start operation
        # Assign the new publicip of the VM to its ssh_ip attribute
        # so that correct IP address is used for getting the ssh client of VM
        virtual_machine = SetPublicIpForVM(self.apiclient, virtual_machine)

        self.debug("SSHing with new keypair")
        try:
            virtual_machine.get_ssh_client(
                                    keyPairFileLocation=str(keyPairFilePath))
        except Exception as e:
            self.fail("Failed to SSH into VM with new keypair: %s, %s" %
                                                    (virtual_machine.name, e))
        virtual_machine.delete(self.apiclient, expunge=True)
        return

    @attr(tags=["simulator", "basic", "advanced"])
    def test_05_reset_key_in_running_state(self):
        """Reset SSH keys for VM  already having SSH key when VM is in running
        state"""

        # Validate the following
        # 1.Create a VM  having SSH keyPair
        # 2. Reset SSH key pair. Api  returns error message VM is running

        # Spawn an instance
        virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    templateid=self.pw_ssh_enabled_template.id,
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    zoneid=self.zone.id,
                                    serviceofferingid=self.service_offering.id,
                                    keypair=self.keypair.name,
                                    mode=self.services["mode"]
                                    )

        self.debug("Check if the VM is properly deployed or not?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=virtual_machine.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return the valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "VM state should be running after deployment"
                         )

        self.debug("Creating a new SSH keypair for account: %s" %
                                                    self.account.name)
        new_keypair = SSHKeyPair.create(
                                    self.apiclient,
                                    name=random_gen() + ".pem",
                                    account=self.account.name,
                                    domainid=self.account.domainid
                                 )
        self.debug("Created a new keypair with name: %s" % new_keypair.name)

        self.debug("Writing the private key to local file")
        keyPairFilePath = tempfile.gettempdir() + os.sep + new_keypair.name

        self.debug("File path: %s" % keyPairFilePath)

        f = open(keyPairFilePath, "w+")
        f.write(new_keypair.privatekey)
        f.close()

        os.system("chmod 400 " + keyPairFilePath)

        self.debug("Resetting the SSH key pair for instance: %s" %
                                                        virtual_machine.name)
        with self.assertRaises(Exception):
            virtual_machine.resetSshKey(
                                        self.apiclient,
                                        keypair=new_keypair.name,
                                        name=new_keypair.name,
                                        account=self.account.name,
                                        domainid=self.account.domainid
                                        )

        virtual_machine.delete(self.apiclient, expunge=True)
        return

    @attr(tags=["simulator", "basic", "advanced"])
    def test_06_reset_key_passwd_enabled_vm_running(self):
        """Reset SSH keys for VM  created from password enabled template and
            already having SSH key  and VM is in running state"""

        # Validate the following
        # 1. Reset SSH keys for VM  created from password enabled template
        #    and already having SSH key and VM is in running state
        # 2. APi returns error message Vm is running

        self.debug("Deploying the virtual machine in default network offering")

        # Spawn an instance
        virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    templateid=self.pw_ssh_enabled_template.id,
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    zoneid=self.zone.id,
                                    serviceofferingid=self.service_offering.id,
                                    keypair=self.keypair.name,
                                    mode=self.services["mode"]
                                    )

        self.debug("Check if the VM is properly deployed or not?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=virtual_machine.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return the valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "VM state should be running after deployment"
                         )

        self.debug("Creating a new SSH keypair for account: %s" %
                                                    self.account.name)
        new_keypair = SSHKeyPair.create(
                                    self.apiclient,
                                    name=random_gen() + ".pem",
                                    account=self.account.name,
                                    domainid=self.account.domainid
                                 )
        self.debug("Created a new keypair with name: %s" % new_keypair.name)

        self.debug("Writing the private key to local file")
        keyPairFilePath = tempfile.gettempdir() + os.sep + new_keypair.name

        self.debug("File path: %s" % keyPairFilePath)

        f = open(keyPairFilePath, "w+")
        f.write(new_keypair.privatekey)
        f.close()

        os.system("chmod 400 " + keyPairFilePath)

        self.debug("Resetting the SSH key pair for instance: %s" %
                                                        virtual_machine.name)
        with self.assertRaises(Exception):
            virtual_machine.resetSshKey(
                                        self.apiclient,
                                        keypair=new_keypair.name,
                                        name=new_keypair.name,
                                        account=self.account.name,
                                        domainid=self.account.domainid
                                        )

        virtual_machine.delete(self.apiclient, expunge=True)
        return

    @attr(tags=["simulator", "basic", "advanced"])
    def test_07_reset_keypair_invalid_params(self):
        """Verify API resetSSHKeyForVirtualMachine with incorrect parameters"""

        # Validate the following
        # 1. Create the VM
        # 2. Stop the VM
        # 3. Call resetSSHKeyForVirtualMachine API with incorrect parameter

        # Spawn an instance
        virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    templateid=self.pw_ssh_enabled_template.id,
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    zoneid=self.zone.id,
                                    serviceofferingid=self.service_offering.id,
                                    keypair=self.keypair.name,
                                    mode=self.services["mode"]
                                    )

        self.debug("Check if the VM is properly deployed or not?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=virtual_machine.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return the valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "VM state should be running after deployment"
                         )

        self.debug("Creating a new SSH keypair for account: %s" %
                                                    self.account.name)
        new_keypair = SSHKeyPair.create(
                                    self.apiclient,
                                    name=random_gen() + ".pem",
                                    account=self.account.name,
                                    domainid=self.account.domainid
                                 )
        self.debug("Created a new keypair with name: %s" % new_keypair.name)

        self.debug("Writing the private key to local file")
        keyPairFilePath = tempfile.gettempdir() + os.sep + new_keypair.name

        self.debug("File path: %s" % keyPairFilePath)

        f = open(keyPairFilePath, "w+")
        f.write(new_keypair.privatekey)
        f.close()

        os.system("chmod 400 " + keyPairFilePath)

        self.debug("Resetting the SSH key pair for instance: %s" %
                                                        virtual_machine.name)
        with self.assertRaises(Exception):
            virtual_machine.resetSshKey(
                                        self.apiclient,
                                        keypair=random_gen() + ".pem",
                                        name=new_keypair.name,
                                        account=self.account.name,
                                        domainid=self.account.domainid
                                        )
        self.debug("Reset SSH key pair failed due to invalid parameters")

        virtual_machine.delete(self.apiclient, expunge=True)
        return

class TestResetSSHKeyUserRights(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestResetSSHKeyUserRights, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        # Set Zones and disk offerings
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        # Create Account, VMs, NAT Rules etc
        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        # Set Zones and disk offerings
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )

        cls.virtual_machine = VirtualMachine.create(
            cls.api_client,
            cls.services["virtual_machine"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            mode=cls.services["mode"]
        )

        networkid = cls.virtual_machine.nic[0].networkid

        # create egress rule to allow wget of my cloud-set-guest-password script
        if cls.zone.networktype.lower() == 'advanced':
            EgressFireWallRule.create(cls.api_client,
                                  networkid=networkid,
                                  protocol=cls.services["egress"]["protocol"],
                                  startport=cls.services["egress"]["startport"],
                                  endport=cls.services["egress"]["endport"],
                                  cidrlist=cls.services["egress"]["cidrlist"])

        cls.virtual_machine.password = cls.services["virtual_machine"]["password"]
        ssh = cls.virtual_machine.get_ssh_client()

        #below steps are required to get the new password from VR(reset password)
        #http://cloudstack.org/dl/cloud-set-guest-password
        #Copy this file to /etc/init.d
        #chmod +x /etc/init.d/cloud-set-guest-password
        #chkconfig --add cloud-set-guest-password
        # Do similar steps to get SSH key from web so as to make it ssh enabled

        cmds = [
            "cd /etc/init.d;wget http://downloads.sourceforge.net/project/cloudstack/SSH%20Key%20Gen%20Script/" + \
            "cloud-set-guest-sshkey.in?r=http%3A%2F%2Fsourceforge" + \
            ".net%2Fprojects%2Fcloudstack%2Ffiles%2FSSH%2520Key%2520Gen%2520Script%2F&ts=1331225219&use_mirror=iweb",
            "chmod +x /etc/init.d/cloud-set-guest-sshkey.in",
            "chkconfig --add cloud-set-guest-sshkey.in"
            ]
        for c in cmds:
            ssh.execute(c)

        # Adding delay of 120 sec to avoid data loss due to timing issue
        time.sleep(120)

        try:
            #Stop virtual machine
            cls.virtual_machine.stop(cls.api_client)
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Exception in setUpClass: %s" % e)

        list_volume = Volume.list(
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
        #Create templates for Edit, Delete & update permissions testcases
        cls.pw_ssh_enabled_template = Template.create(
            cls.api_client,
            cls.services["template"],
            cls.volume.id
        )
        # Delete the VM - No longer needed
        cls.virtual_machine.delete(cls.api_client, expunge=True)

        cls._cleanup = [
                        cls.service_offering,
                        cls.pw_ssh_enabled_template,
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

        # Set Zones and disk offerings
        self.services["virtual_machine"]["zoneid"] = self.zone.id

        self.service_offering = ServiceOffering.create(
                                        self.apiclient,
                                        self.services["service_offering"]
                                       )
        # Cleanup
        self.cleanup = [self.service_offering]

    def tearDown(self):
        try:
            #Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["basic", "advanced"], required_hardware="true")
    def test_01_reset_keypair_normal_user(self):
        """Verify API resetSSHKeyForVirtualMachine for non admin non root
            domain user"""

        # Validate the following
        # 1. Create a VM  for non admin non root domain user
        # 2. Stop the VM
        # 3. Reset SSH key pair for non admin non root domain user. Verify VM
        #    is not restarted automatically as a result of API execution
        #    User should be able to ssh into the VM  using new keypair when
        #    VM is restarted

        # Create account, SSH key pair etc.
        self.debug("Creating a normal user account")
        self.user_account = Account.create(
                                      self.apiclient,
                                      self.services["account"],
                                      admin=False
                                      )
        self.cleanup.append(self.user_account)
        self.debug("Account created: %s" % self.user_account.name)

        # Spawn an instance
        virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    templateid=self.pw_ssh_enabled_template.id,
                                    accountid=self.user_account.name,
                                    domainid=self.user_account.domainid,
                                    zoneid=self.zone.id,
                                    serviceofferingid=self.service_offering.id,
                                    mode=self.services["mode"]
                                    )

        self.debug("Check if the VM is properly deployed or not?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=virtual_machine.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return the valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "VM state should be running after deployment"
                         )
        self.debug("Stopping the virtual machine")
        try:
            virtual_machine.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop virtual machine: %s, %s" %
                                                    (virtual_machine.id, e))

        self.debug("Creating a new SSH keypair for account: %s" %
                                                    self.user_account.name)
        new_keypair = SSHKeyPair.create(
                                    self.apiclient,
                                    name=random_gen() + ".pem",
                                    account=self.user_account.name,
                                    domainid=self.user_account.domainid
                                 )
        self.debug("Created a new keypair with name: %s" % new_keypair.name)

        self.debug("Writing the private key to local file")
        keyPairFilePath = tempfile.gettempdir() + os.sep + new_keypair.name

        self.debug("File path: %s" % keyPairFilePath)

        f = open(keyPairFilePath, "w+")
        f.write(new_keypair.privatekey)
        f.close()

        os.system("chmod 400 " + keyPairFilePath)

        self.debug("Resetting the SSH key pair for instance: %s" %
                                                        virtual_machine.name)
        try:
            virtual_machine.resetSshKey(
                                        self.apiclient,
                                        keypair=new_keypair.name,
                                        name=new_keypair.name,
                                        account=self.user_account.name,
                                        domainid=self.user_account.domainid
                                        )
        except Exception as e:
            self.fail("Failed to reset SSH key: %s, %s" %
                                                (virtual_machine.name, e))
        self.debug("Starting the virtual machine after resetting the keypair")
        try:
            virtual_machine.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start virtual machine: %s, %s" %
                                           (virtual_machine.name, e))

        timeout = wait_vm_start(self.apiclient, virtual_machine.id, self.services["timeout"],
                            self.services["sleep"])

        if timeout == 0:
            self.fail("The virtual machine %s failed to start even after %s minutes"
                   % (vms[0].name, self.services["timeout"]))

        # In case of EIP setup, public IP changes after VM start operation
        # Assign the new publicip of the VM to its ssh_ip attribute
        # so that correct IP address is used for getting the ssh client of VM
        virtual_machine = SetPublicIpForVM(self.apiclient, virtual_machine)

        self.debug("SSHing with new keypair")
        try:
            virtual_machine.get_ssh_client(
                                    keyPairFileLocation=str(keyPairFilePath))
        except Exception as e:
            self.fail("Failed to SSH into VM with new keypair: %s, %s" %
                                                    (virtual_machine.name, e))

        virtual_machine.delete(self.apiclient, expunge=True)
        return

    @attr(tags=["basic", "advanced"], required_hardware="true")
    def test_02_reset_keypair_domain_admin(self):
        """Verify API resetSSHKeyForVirtualMachine for domain admin non root
            domain user"""

        # Validate the following
        # 1.Create a VM  for domain admin non root domain user
        # 2. Stop the VM
        # 3. Reset SSH key pair for domain  admin non root domain user. Verify
        #    VM is not restarted automatically as a result of API execution
        #    User should be able to ssh into the VM  using new keypair when
        #    VM is restarted

        # Create account, SSH key pair etc.
        self.debug("Creating a domain admin account")
        self.account = Account.create(
                                      self.apiclient,
                                      self.services["account"],
                                      admin=True,
                                      domainid=self.domain.id
                                      )
        self.cleanup.append(self.account)
        self.debug("Account created: %s" % self.account.name)

        self.debug("Generating SSH keypair for the account: %s" %
                                            self.account.name)
        self.keypair = SSHKeyPair.create(
                                    self.apiclient,
                                    name=random_gen() + ".pem",
                                    account=self.account.name,
                                    domainid=self.account.domainid
                                 )

        self.debug("Writing the private key to local file")
        keyPairFilePath = tempfile.gettempdir() + os.sep + self.keypair.name

        self.debug("File path: %s" % keyPairFilePath)

        f = open(keyPairFilePath, "w+")
        f.write(self.keypair.privatekey)
        f.close()

        os.system("chmod 400 " + keyPairFilePath)

        # Spawn an instance
        virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    templateid=self.pw_ssh_enabled_template.id,
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    keypair=self.keypair.name,
                                    mode=self.services["mode"]
                                    )

        self.debug("Check if the VM is properly deployed or not?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=virtual_machine.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return the valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "VM state should be running after deployment"
                         )
        self.debug("Stopping the virtual machine")
        try:
            virtual_machine.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop virtual machine: %s, %s" %
                                                    (virtual_machine.id, e))

        self.debug("Creating a new SSH keypair for account: %s" %
                                                    self.account.name)
        new_keypair = SSHKeyPair.create(
                                    self.apiclient,
                                    name=random_gen() + ".pem",
                                    account=self.account.name,
                                    domainid=self.account.domainid
                                 )
        self.debug("Created a new keypair with name: %s" % new_keypair.name)

        self.debug("Writing the private key to local file")
        keyPairFilePath = tempfile.gettempdir() + os.sep + new_keypair.name

        self.debug("File path: %s" % keyPairFilePath)

        f = open(keyPairFilePath, "w+")
        f.write(new_keypair.privatekey)
        f.close()

        os.system("chmod 400 " + keyPairFilePath)

        self.debug("Resetting the SSH key pair for instance: %s" %
                                                        virtual_machine.name)
        try:
            virtual_machine.resetSshKey(
                                        self.apiclient,
                                        keypair=new_keypair.name,
                                        name=new_keypair.name,
                                        account=self.account.name,
                                        domainid=self.account.domainid
                                        )
        except Exception as e:
            self.fail("Failed to reset SSH key: %s, %s" %
                                                (virtual_machine.name, e))
        self.debug("Starting the virtual machine after resetting the keypair")
        try:
            virtual_machine.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start virtual machine: %s, %s" %
                                                    (virtual_machine.name, e))

        timeout = wait_vm_start(self.apiclient, virtual_machine.id, self.services["timeout"],
                            self.services["sleep"])

        if timeout == 0:
            self.fail("The virtual machine %s failed to start even after %s minutes"
                   % (virtual_machine.name, self.services["timeout"]))

        # In case of EIP setup, public IP changes after VM start operation
        # Assign the new publicip of the VM to its ssh_ip attribute
        # so that correct IP address is used for getting the ssh client of VM
        virtual_machine = SetPublicIpForVM(self.apiclient, virtual_machine)

        self.debug("SSHing with new keypair")
        try:
            virtual_machine.get_ssh_client(
                                    keyPairFileLocation=str(keyPairFilePath))
        except Exception as e:
            self.fail("Failed to SSH into VM with new keypair: %s, %s" %
                                                    (virtual_machine.name, e))

        virtual_machine.delete(self.apiclient, expunge=True)
        return

    @attr(tags=["basic", "advanced"], required_hardware="true")
    def test_03_reset_keypair_root_admin(self):
        """Verify API resetSSHKeyForVirtualMachine for domain admin root
            domain user"""

        # Validate the following
        # 1.Create a VM  for domain admin, root domain user
        # 2. Stop the VM
        # 3. Reset SSH key pair for domain  admin non root domain user. Verify
        #    VM is not restarted automatically as a result of API execution
        #    User should be able to ssh into the VM  using new keypair when
        #    VM is restarted

        # Create account, SSH key pair etc.
        self.debug("Creating a ROOT admin account")
        self.account = Account.create(
                                      self.apiclient,
                                      self.services["account"],
                                      admin=True
                                      )
        self.cleanup.append(self.account)
        self.debug("Account created: %s" % self.account.name)

        self.debug("Generating SSH keypair for the account: %s" %
                                            self.account.name)
        self.keypair = SSHKeyPair.create(
                                    self.apiclient,
                                    name=random_gen() + ".pem",
                                    account=self.account.name,
                                    domainid=self.account.domainid
                                 )

        self.debug("Writing the private key to local file")
        keyPairFilePath = tempfile.gettempdir() + os.sep + self.keypair.name

        self.debug("File path: %s" % keyPairFilePath)

        f = open(keyPairFilePath, "w+")
        f.write(self.keypair.privatekey)
        f.close()

        os.system("chmod 400 " + keyPairFilePath)

        self.debug("Deploying the virtual machine in default network offering")

        # Spawn an instance
        virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    templateid=self.pw_ssh_enabled_template.id,
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    keypair=self.keypair.name,
                                    mode=self.services["mode"]
                                    )

        self.debug("Check if the VM is properly deployed or not?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=virtual_machine.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return the valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "VM state should be running after deployment"
                         )
        self.debug("Stopping the virtual machine")
        try:
            virtual_machine.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop virtual machine: %s, %s" %
                                                    (virtual_machine.id, e))

        self.debug("Creating a new SSH keypair for account: %s" %
                                                    self.account.name)
        new_keypair = SSHKeyPair.create(
                                    self.apiclient,
                                    name=random_gen() + ".pem",
                                    account=self.account.name,
                                    domainid=self.account.domainid
                                 )
        self.debug("Created a new keypair with name: %s" % new_keypair.name)

        self.debug("Writing the private key to local file")
        keyPairFilePath = tempfile.gettempdir() + os.sep + new_keypair.name

        self.debug("File path: %s" % keyPairFilePath)

        f = open(keyPairFilePath, "w+")
        f.write(new_keypair.privatekey)
        f.close()

        os.system("chmod 400 " + keyPairFilePath)

        self.debug("Resetting the SSH key pair for instance: %s" %
                                                        virtual_machine.name)
        try:
            virtual_machine.resetSshKey(
                                        self.apiclient,
                                        keypair=new_keypair.name,
                                        name=new_keypair.name,
                                        account=self.account.name,
                                        domainid=self.account.domainid
                                        )
        except Exception as e:
            self.fail("Failed to reset SSH key: %s, %s" %
                                                (virtual_machine.name, e))
        self.debug("Starting the virtual machine after resetting the keypair")
        try:
            virtual_machine.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start virtual machine: %s, %s" %
                                                    (virtual_machine.name, e))

        timeout = wait_vm_start(self.apiclient, virtual_machine.id, self.services["timeout"],
                            self.services["sleep"])

        if timeout == 0:
            self.fail("The virtual machine %s failed to start even after %s minutes"
                   % (virtual_machine.name, self.services["timeout"]))

        # In case of EIP setup, public IP changes after VM start operation
        # Assign the new publicip of the VM to its ssh_ip attribute
        # so that correct IP address is used for getting the ssh client of VM
        virtual_machine = SetPublicIpForVM(self.apiclient, virtual_machine)

        self.debug("SSHing with new keypair")
        try:
            virtual_machine.get_ssh_client(
                                    keyPairFileLocation=str(keyPairFilePath))
        except Exception as e:
            self.fail("Failed to SSH into VM with new keypair: %s, %s" %
                                                    (virtual_machine.name, e))
        virtual_machine.delete(self.apiclient, expunge=True)
        return
