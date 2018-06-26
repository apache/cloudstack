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

""" Component tests for user data and password reset functionality with
Nuage VSP SDN plugin
"""
# Import Local Modules
from nuageTestCase import nuageTestCase
from marvin.lib.base import (Account,
                             Template,
                             VirtualMachine,
                             Volume)
from marvin.lib.common import list_templates
from marvin.cloudstackAPI import updateTemplate
# Import System Modules
from nose.plugins.attrib import attr
import base64


class TestNuagePasswordReset(nuageTestCase):
    """Test user data and password reset functionality with
    Nuage VSP SDN plugin
    """

    @classmethod
    def setUpClass(cls):
        super(TestNuagePasswordReset, cls).setUpClass()
        return

    def setUp(self):
        # Create an account
        self.account = Account.create(self.api_client,
                                      self.test_data["account"],
                                      admin=True,
                                      domainid=self.domain.id
                                      )
        self.cleanup = [self.account]
        return

    # create_template - Creates guest VM template with the given VM object
    def create_template(self, vm):
        self.debug("Creating guest VM template")
        list_volume = Volume.list(self.api_client,
                                  virtualmachineid=vm.id,
                                  type='ROOT',
                                  listall=True
                                  )
        if isinstance(list_volume, list):
            self.volume = list_volume[0]
        else:
            raise Exception("Exception: Unable to find root volume for VM "
                            "with ID - %s" % vm.id)
        self.pw_enabled_template = Template.create(
            self.api_client,
            self.test_data["template"],
            self.volume.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(self.pw_enabled_template.passwordenabled, True,
                         "Template is not password enabled"
                         )
        self.cleanup.append(self.pw_enabled_template)
        self.debug("Created guest VM template")

    # updateTemplate - Updates value of the guest VM template's password
    # enabled setting
    def updateTemplate(self, value):
        self.debug("Updating value of guest VM template's password enabled "
                   "setting")
        cmd = updateTemplate.updateTemplateCmd()
        cmd.id = self.template.id
        cmd.passwordenabled = value
        self.api_client.updateTemplate(cmd)
        list_template_response = list_templates(self.api_client,
                                                templatefilter="all",
                                                id=self.template.id
                                                )
        self.template = list_template_response[0]
        self.debug("Updated guest VM template")

    # get_userdata_url - Returns user data URL for the given VM object
    def get_userdata_url(self, vm):
        self.debug("Getting user data url")
        nic = vm.nic[0]
        gateway = str(nic.gateway)
        self.debug("Gateway: " + gateway)
        user_data_url = 'curl "http://' + gateway + ':80/latest/user-data"'
        return user_data_url

    # create_and_verify_fw - Creates and verifies (Ingress) firewall rule with
    # a Static NAT rule enabled public IP
    def create_and_verify_fw(self, vm, public_ip, network):
        self.debug("Creating and verifying firewall rule")
        self.create_StaticNatRule_For_VM(vm, public_ip, network)

        # VSD verification
        self.verify_vsd_floating_ip(network, vm, public_ip.ipaddress)

        fw_rule = self.create_FirewallRule(
            public_ip, self.test_data["ingress_rule"])

        # VSD verification
        self.verify_vsd_firewall_rule(fw_rule)
        self.debug("Successfully created and verified firewall rule")

    # stop_vm - Stops the given VM, and verifies its state
    def stop_vm(self, vm):
        self.debug("Stopping VM")
        vm.stop(self.api_client)
        list_vm_response = VirtualMachine.list(self.api_client,
                                               id=vm.id
                                               )
        if isinstance(list_vm_response, list):
            vm = list_vm_response[0]
            if vm.state != 'Stopped':
                raise Exception("Failed to stop VM (ID: %s) " % self.vm.id)
        else:
            raise Exception("Invalid response from list_virtual_machines VM "
                            "(ID: %s) " % self.vm.id)
        self.debug("Stopped VM")

    # install_cloud_set_guest_password_script - Installs the
    # cloud-set-guest-password script from people.apache.org in the given VM
    # (SSH client)
    def install_cloud_set_guest_password_script(self, ssh_client):
        if self.isSimulator:
            self.debug("Simulator Environment: Skipping installing"
                       " cloud-set-guest-password script")
            return
        self.debug("Installing cloud-set-guest-password script")
        cmd = "cd /etc/init.d;wget http://people.apache.org/~tsp/" \
              "cloud-set-guest-password"
        result = self.execute_cmd(ssh_client, cmd)
        self.debug("wget file cloud-set-guest-password: " + result)
        if "200 OK" not in result:
            self.fail("failed to wget file cloud-set-guest-password")
        cmds = ["chmod +x /etc/init.d/cloud-set-guest-password",
                "chkconfig --add cloud-set-guest-password"
                ]
        for c in cmds:
            result = self.execute_cmd(ssh_client, c)
            self.debug("get_set_password_file cmd " + c)
            self.debug("get_set_password_file result " + result)
        self.debug("Installed cloud-set-guest-password script")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_nuage_UserDataPasswordReset(self):
        """Test user data and password reset functionality with
        Nuage VSP SDN plugin
        """

        # 1. Create an Isolated Network with Nuage VSP Isolated Network
        #    offering, check if it is successfully created and is in the
        #    "Allocated" state.
        # 2. Set password enabled to false in the guest VM template.
        # 3. Deploy a VM in the created Isolated network with user data, check
        #    if the Isolated network state is changed to "Implemented", and
        #    both the VM & VR are successfully deployed and are in the
        #    "Running" state.
        # 4. Verify that the guest VM template is not password enabled by
        #    checking the deployed VM's password (password == "password").
        # 5. SSH into the deployed VM and verify its user data
        #    (expected user data == actual user data).
        # 6. Check for cloud-set-guest-password script in the deployed VM for
        #    testing password reset functionality.
        # 7. if cloud-set-guest-password script does not exist in the deployed
        #    VM:
        #       7.1  Install the cloud-set-guest-password script from
        #            people.apache.org in the deployed VM.
        #       7.2  Stop the deployed VM, and create a new password enabled
        #            guest VM template with it.
        #       7.3  Deploy a new VM in the created Isolated network with the
        #            newly created guest VM template, check if the VM is
        #            successfully deployed and is in the "Running" state.
        #       7.4  Verify that the new guest VM template is password enabled
        #            by checking the newly deployed VM's password
        #            (password != "password").
        #       7.5  SSH into the newly deployed VM for verifying its password.
        # 8. else cloud-set-guest-password script exists in the deployed VM:
        #       8.1  Change password enabled to true in the guest VM template.
        #       8.2  Verify that the guest VM template is password enabled.
        # 9. Reset VM password, and start the VM.
        # 10. Verify that the new guest VM template is password enabled by
        #     checking the VM's password (password != "password").
        # 11. SSH into the VM for verifying its new password after its password
        #     reset.
        # 12. Set password enabled to the default value in the guest VM
        #     template.
        # 13. Delete all the created objects (cleanup).

        for zone in self.zones:
            self.debug("Zone - %s" % zone.name)
            # Get Zone details
            self.getZoneDetails(zone=zone)
            # Configure VSD sessions
            self.configureVSDSessions()

            self.debug("Testing user data & password reset functionality in "
                       "an Isolated network...")

            self.debug("Creating an Isolated network...")
            net_off = self.create_NetworkOffering(
                self.test_data["nuagevsp"]["isolated_network_offering"])
            self.network = self.create_Network(net_off)
            self.validate_Network(self.network, state="Allocated")

            self.debug("Setting password enabled to false in the guest VM "
                       "template...")
            self.defaultTemplateVal = self.template.passwordenabled
            if self.template.passwordenabled:
                self.updateTemplate(False)

            self.debug("Deploying a VM in the created Isolated network with "
                       "user data...")
            expected_user_data = "hello world vm1"
            user_data = base64.b64encode(expected_user_data)
            self.test_data["virtual_machine_userdata"]["userdata"] = user_data
            self.vm_1 = self.create_VM(
                self.network,
                testdata=self.test_data["virtual_machine_userdata"])
            self.validate_Network(self.network, state="Implemented")
            vr = self.get_Router(self.network)
            self.check_Router_state(vr, state="Running")
            self.check_VM_state(self.vm_1, state="Running")

            # VSD verification
            self.verify_vsd_network(self.domain.id, self.network)
            self.verify_vsd_router(vr)
            self.verify_vsd_vm(self.vm_1)

            self.debug("verifying that the guest VM template is not password "
                       "enabled...")
            self.debug("VM - %s password - %s !" %
                       (self.vm_1.name, self.vm_1.password))
            self.assertEqual(
                self.vm_1.password,
                self.test_data["virtual_machine_userdata"]["password"],
                "Password is enabled for the VM (vm_1)"
            )

            self.debug("SSHing into the VM for verifying its user data...")
            public_ip_1 = self.acquire_PublicIPAddress(self.network)
            self.create_and_verify_fw(self.vm_1, public_ip_1, self.network)
            ssh = self.ssh_into_VM(self.vm_1, public_ip_1)
            user_data_cmd = self.get_userdata_url(self.vm_1)
            if self.isSimulator:
                self.debug("Simulator Environment: ending test early "
                           "because we don't have real vms")
                return
            self.debug("Getting user data with command: " + user_data_cmd)
            actual_user_data = base64.b64decode(self.execute_cmd
                                                (ssh, user_data_cmd))
            self.debug("Actual user data - " + actual_user_data +
                       ", Expected user data - " + expected_user_data)
            self.assertEqual(actual_user_data, expected_user_data,
                             "Un-expected VM (VM_1) user data")

            self.debug("Checking for cloud-set-guest-password script in the "
                       "VM for testing password reset functionality...")
            ls_cmd = "ls /etc/init.d/cloud-set-guest-password"
            ls_result = self.execute_cmd(ssh, ls_cmd)
            ls_result = ls_result.lower()
            self.debug("Response from ls_cmd: " + ls_result)

            if "no such file" in ls_result:
                self.debug("No cloud-set-guest-password script in the VM")
                self.debug("Installing the cloud-set-guest-password script "
                           "from people.apache.org in the VM...")
                self.install_cloud_set_guest_password_script(ssh)
                self.debug("Stopping the VM, and creating a new password "
                           "enabled guest VM template with it...")
                self.stop_vm(self.vm_1)
                self.create_template(self.vm_1)

                self.debug("Deploying a new VM in the created Isolated "
                           "network with the newly created guest VM "
                           "template...")
                self.vm_2 = self.create_VM(
                    self.network,
                    testdata=self.test_data["virtual_machine_userdata"])
                self.debug("Starting the VM...")
                vm_2a = self.vm_2.start(self.api_client)
                self.vm_2.password = vm_2a.password.strip()
                self.vm_2.nic = vm_2a.nic

                # VSD verification
                self.verify_vsd_vm(self.vm_2)

                self.debug("verifying that the guest VM template is password "
                           "enabled...")
                self.debug("VM - %s password - %s !" %
                           (self.vm_2.name, self.vm_2.password))
                self.assertNotEqual(
                    self.vm_2.password,
                    self.test_data["virtual_machine_userdata"]["password"],
                    "Password is not enabled for the VM"
                )

                self.debug("SSHing into the VM for verifying its password...")
                public_ip_2 = self.acquire_PublicIPAddress(self.network)
                self.create_and_verify_fw(self.vm_2, public_ip_2, self.network)
                self.ssh_into_VM(self.vm_2, public_ip_2)

                vm_test = self.vm_2
                vm_test_public_ip = public_ip_2
            else:
                self.debug("Updating the guest VM template to password "
                           "enabled")
                self.updateTemplate(True)
                self.assertEqual(self.template.passwordenabled, True,
                                 "Guest VM template is not password enabled"
                                 )
                vm_test = self.vm_1
                vm_test_public_ip = public_ip_1

            self.debug("Resetting password for VM - %s" % vm_test.name)
            self.stop_vm(vm_test)
            vm_test.password = vm_test.resetPassword(self.api_client)
            self.debug("Password reset to - %s" % vm_test.password)

            self.debug("Starting the VM")
            vm_test.start(self.api_client)

            self.debug("until CLOUDSTACK-10380 is fixed, redo resetPassword")
            self.stop_vm(vm_test)
            self.debug("Resetting password again for VM - %s" % vm_test.name)
            vm_test.password = vm_test.resetPassword(self.api_client)
            self.debug("VM - %s password - %s !" %
                       (vm_test.name, vm_test.password))
            self.debug("Starting the VM again")
            vm_test.start(self.api_client)

            self.debug("verifying that the guest VM template is password "
                       "enabled...")
            self.debug("VM - %s password - %s !" %
                       (vm_test.name, vm_test.password))
            self.assertNotEqual(
                vm_test.password,
                self.test_data["virtual_machine_userdata"]["password"],
                "Password is not enabled for the VM"
            )

            self.debug("SSHing into the VM for verifying its new password "
                       "after its password reset...")
            self.ssh_into_VM(vm_test, vm_test_public_ip)

            self.debug("Setting password enabled to the default value in the "
                       "guest VM template...")
            self.updateTemplate(self.defaultTemplateVal)
