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

""" Component tests for - userdata
"""
# Import Local Modules
from marvin.lib.base import (Account,
                             VirtualMachine,
                             Volume,
                             Template)
from nose.plugins.attrib import attr
from nuageTestCase import nuageTestCase
from marvin.lib.utils import cleanup_resources
from marvin.cloudstackAPI import startVirtualMachine
import base64


class TestNuagePasswordReset(nuageTestCase):

    @classmethod
    def setUpClass(cls):
        super(TestNuagePasswordReset, cls).setUpClass()

        return

    def setUp(self):
        self.cleanup = []
        self.apiclient = self.testClient.getApiClient()

        self.account = Account.create(
            self.apiclient,
            self.test_data["account"],
            admin=True,
            domainid=self.domain.id
        )

        self.cleanup.append(self.account)
        self.remove_vm2 = False
        return

    # tearDown() - Cleans up the setup, removes the VMs
    def tearDown(self):
        self.debug("CLEANUP: TEARDOWN")
        self.apiclient = self.testClient.getApiClient()
        self.updateTemplate(self.defaultTemplateVal)
        self.vm_1.delete(self.apiclient, expunge=True)
        if self.remove_vm2:
            self.vm_2.delete(self.apiclient, expunge=True)

        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning: Exception during cleanup : %s" % e)
        return

    # create_template - Takes the VM object as the argument to create the template
    def create_template(self, vm):
        self.debug("CREATE TEMPLATE")
        list_volume = Volume.list(self.apiclient,
                                  virtualmachineid=vm.id,
                                  type='ROOT',
                                  listall=True)
        if isinstance(list_volume, list):
            self.volume = list_volume[0]
        else:
            raise Exception("Exception: Unable to find root volume for VM: %s" % vm.id)

        self.test_data["template_pr"]["ostype"] = self.test_data["ostype_pr"]
        self.pw_enabled_template = Template.create(
            self.apiclient,
            self.test_data["template_pr"],
            self.volume.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(self.pw_enabled_template.passwordenabled, True, "template is not passwordenabled")
        self.cleanup.append(self.pw_enabled_template)

    # VM object is passed as an argument and its interface id is returned
    def get_vm_interface_id(self, vm):
        self.debug("GET VM INTERFACE ID")
        nic_ext_id = self.get_externalID(vm.nic[0].id)
        vm_interface = self.vsd.get_vm_interface(externalID=nic_ext_id)
        vm_interface_id = vm_interface["ID"]
        return vm_interface_id

    # VM object is passed as an argument and its userdata URL is returned
    def get_userdata_url(self, vm):
        self.debug("GET USER DATA URL")
        nic = vm.nic[0]
        gateway = str(nic.gateway)
        self.debug("GATEWAY: " + gateway)
        user_data_url = 'curl "http://' + gateway + ':80/latest/user-data"'
        return user_data_url

    # Creates and verifies the firewall rule
    def create_and_verify_fw(self, vm, public_ip, network):
        self.debug("CREATE AND VERIFY FIREWALL RULE")
        self.create_StaticNatRule_For_VM(vm, public_ip, network)

        # VSD verification
        self.verify_vsp_floating_ip(network, vm, public_ip.ipaddress)

        fw_rule = self.create_firewall_rule(public_ip, self.test_data["ingress_rule"])
        self.verify_vsp_firewall_rule(fw_rule)
        vm_interface_id = self.get_vm_interface_id(vm)
        pd = self.vsd.get_vm_interface_policydecisions(id=vm_interface_id)
        self.debug(pd)
        egressAcls = pd['egressACLs'][0]['entries']
        gotFirewallPolicy = False
        for acl in egressAcls:
            if acl['destinationPort'] == "22-22":
                gotFirewallPolicy = True
                break
        if not gotFirewallPolicy:
            raise ValueError('No firewall policy decision in vm interface')

    def stop_vm(self, vm):
        self.debug("STOP VM")
        vm.stop(self.apiclient)
        list_vm_response = VirtualMachine.list(self.apiclient,
                                               id=vm.id)
        if isinstance(list_vm_response, list):
            vm = list_vm_response[0]
            if vm.state != 'Stopped':
                raise Exception("Failed to stop VM (ID: %s) " %
                                self.vm.id)
        else:
            raise Exception("Invalid response from list_virtual_machines VM (ID: %s) " %
                            self.vm.id)

    def install_cloud_set_guest_password_script(self, ssh_client):
        self.debug("GET CLOUD-SET-GUEST-PASSWORD")
        cmd = "cd /etc/init.d;wget http://people.apache.org/~tsp/cloud-set-guest-password"
        result = self.execute_cmd(ssh_client, cmd)
        self.debug("WGET CLOUD-SET-GUEST-PASSWORD: " + result)
        if "200 OK" not in result:
            self.fail("failed to get file cloud-set-guest-password")
        cmds = ["chmod +x /etc/init.d/cloud-set-guest-password",
                "chkconfig --add cloud-set-guest-password"
                ]
        for c in cmds:
            result = self.execute_cmd(ssh_client, c)
            self.debug("get_set_password_file cmd " + c)
            self.debug("get_set_password_file result " + result)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_01_UserDataPasswordReset(self):
        self.debug("START USER DATA PASSWORD RESET ON VM")
        """
         Validate the following:
         1) user data
         2) reset vm password.

         Steps:
         1.  Set password enabled to false in the template.
         2.  Create an Isolated network - Test Network (10.1.1.1/24).
         3.  Deploy VM1 in Test Network
         4.  Verify domain,zone subnet, vm.
         5.  create public ip , Create Static Nat rule firewall rule and verify
         6.  SSH to VM should be successful
         7.  verify userdata
         8.  check cloud-set-guest-password exist.
         9.  if cloud-set-guest-password exist.
          9.1    change template password enabled to true
          9.2   verify that template is password enbalded
          9.3   SSH with new password should be successful
         10. else cloud-set-guest-password does not exist.
          10.1  get the cloud-set-guest-password file
          10.2  stop vm
          10.3  create a new template with password enabled. Verify that template is password enabled.
          10.4  create vm 2 with new template in Test Network
          10.5  Verify vm.
          10.6  create public ip , Create Static Nat rule firewall rule and verify
          10.7  SSH to VM 2 should be successful
         11. Reset VM password (VM_1 if guest password file exist.  else it is VM2)
         12  Starting VM and SSH to VM to verify new password
        """

        self.defaultTemplateVal = self.template.passwordenabled
        if self.template.passwordenabled:
            self.updateTemplate(False)

        self.debug("CREATE AN ISOLATED NETWORK")
        self.network_1 = self.create_Network(self.test_data["network_offering_pr"])
        self.cleanup.append(self.network_1)
        expUserData = "hello world vm1"
        userdata = base64.b64encode(expUserData)
        self.test_data["virtual_machine_pr"]["userdata"] = userdata
        self.debug("DEPLOY VM 1 IN TEST NETWORK")
        # Pass the network and name of the vm type from the testdata with the configuration for the vm
        self.vm_1 = self.create_VM_in_Network(self.network_1, "virtual_machine_pr")

        self.vm_1.password = self.test_data["virtual_machine_pr"]["password"]
        user_data_cmd = self.get_userdata_url(self.vm_1)

        # VSD verification
        self.debug("VERIFY DOMAIN, ZONE, NETWORK , and VM 1")
        self.verify_vsp_network(self.domain.id, self.network_1)
        self.verify_vsp_vm(self.vm_1)

        self.debug("CREATE PUBLIC IP, STATIC NAT RULE, FLOATING IP, FIREWALL AND VERIFY")
        public_ip_1 = self.acquire_Public_IP(self.network_1)
        self.create_and_verify_fw(self.vm_1, public_ip_1, self.network_1)

        self.debug("SSH TO VM")
        ssh = self.ssh_into_vm(self.vm_1, public_ip_1)

        self.debug("VERIFY USER DATA")
        self.debug("Get User Data with command: " + user_data_cmd)
        adata = self.execute_cmd(ssh, user_data_cmd)
        actUserData = base64.b64decode(adata)
        self.debug("Response User Data=" + actUserData + ", Expected=" + expUserData)
        self.assertEqual(actUserData, expUserData, "User Data Did Not Match ")

        # check /etc/init.d/cloud-set-quest-password
        ls_cmd = "ls /etc/init.d/cloud-set-guest-password"
        ls_result = self.execute_cmd(ssh, ls_cmd)
        ls_result = ls_result.lower()
        self.debug("reponse from ls_cmd: " + ls_result)
        if "no such file" in ls_result:
            self.debug("NO CLOUD-SET_GUEST_PASSWORD FILE.  NEED TO GET ONE")
            self.install_cloud_set_guest_password_script(ssh)
            self.stop_vm(self.vm_1)
            self.create_template(self.vm_1)
            self.debug("DEPLOY VM 2 IN TEST NETWORK WITH NEW TEMPLATE")
            self.vm_2 = self.create_VM_in_Network(self.network_1, "virtual_machine_pr")
            self.remove_vm2 = True
            self.debug("STARTING VM_2 ")
            startCmd = startVirtualMachine.startVirtualMachineCmd()
            startCmd.id = self.vm_2.id
            vm_2a = self.apiclient.startVirtualMachine(startCmd)
            self.vm_2.password = vm_2a.password.strip()
            self.vm_2.nic = vm_2a.nic
            self.debug("VM - %s password %s !" % (self.vm_2.name, self.vm_2.password))
            self.assertNotEqual(self.vm_2.password,
                                self.test_data["virtual_machine_pr"]["password"],
                                "Password enabled not working. Password same as virtual_machine password "
                                )
            self.verify_vsp_vm(vm_2a)
            self.debug("GET PUBLIC IP.  CREATE AND VERIFIED FIREWALL RULES")
            public_ip_2 = self.acquire_Public_IP(self.network_1)
            self.create_and_verify_fw(self.vm_2, public_ip_2, self.network_1)

            ssh = self.ssh_into_vm(self.vm_2, public_ip_2)
            vm_test = self.vm_2
            vm_test_public_ip = public_ip_2

        else:
            self.debug("UPDATE TEMPLATE TO PASSWORD ENABLED")
            self.updateTemplate(True)
            self.assertEqual(self.template.passwordenabled, True, "Template is not password enabled")
            vm_test = self.vm_1
            vm_test_public_ip = public_ip_1

        self.debug("RESETTING VM PASSWORD for VM: %s" % vm_test.name)
        vm_test.password = vm_test.resetPassword(self.apiclient)
        self.debug("Password reset to: %s" % vm_test.password)
        self.debug("STARTING VM AND SSH TO VM TO VERIFY NEW PASSWORD")
        vm_test.start(self.apiclient)
        self.debug("VM - %s started!" % vm_test.name)
        self.ssh_into_vm(vm_test, vm_test_public_ip)
