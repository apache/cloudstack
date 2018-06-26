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

""" Component tests for user data, meta data, ssh keys
    and password reset functionality with
    ConfigDrive and Nuage VSP SDN plugin
"""
import base64
import copy
import os
import threading

from datetime import datetime
from marvin.lib.base import (Account,
                             NetworkServiceProvider,
                             PublicIpRange,
                             PublicIPAddress,
                             VirtualMachine)
# Import System Modules
from nose.plugins.attrib import attr
from nuage_lib import GherkinMetaClass

# Import Local Modules
from component.test_configdrive import ConfigDriveUtils
from nuageTestCase import nuageTestCase

NO_SUCH_FILE = "No such file or directory"


class TestNuageConfigDrive(nuageTestCase, ConfigDriveUtils):
    """Test user data and password reset functionality
    using configDrive with Nuage VSP SDN plugin
    """

    __metaclass__ = GherkinMetaClass

    class StartVM(threading.Thread):

        def __init__(self, nuagetestcase, network, index):
            threading.Thread.__init__(self)
            self.network = network
            self.nuagetestcase = nuagetestcase
            self.vm = None
            self.index = index

        def run(self):
            self.vm = self.nuagetestcase.create_VM(
                self.network,
                account=self.nuagetestcase.account,
                cleanup=False)
            self.nuagetestcase.check_VM_state(self.vm, state="Running")
            self.nuagetestcase.debug("[Concurrency]VM %d running, name = %s"
                                     % (self.index + 1, self.vm.name))

        def get_vm(self):
            return self.vm

        def stop(self):
            self.vm.delete(self.nuagetestcase.api_client)

        def update(self):
            expected_user_data = "hello world vm %s" % self.vm.name
            user_data = base64.b64encode(expected_user_data)
            self.vm.update(self.nuagetestcase.api_client, userdata=user_data)

    class StopVM(threading.Thread):

        def __init__(self, nuagetestcase, vm, **kwargs):
            threading.Thread.__init__(self)
            self.vm = vm
            self.nuagetestcase = nuagetestcase

        def run(self):
            self.vm.delete(self.nuagetestcase.api_client)
            if self.vm in self.nuagetestcase.cleanup:
                self.nuagetestcase.cleanup.remove(self.vm)

        def get_vm(self):
            return self.vm

        @staticmethod
        def get_name():
            return "delete"

    class UpdateVM(threading.Thread):

        def __init__(self, nuagetestcase, vm, **kwargs):
            threading.Thread.__init__(self)
            self.vm = vm
            self.nuagetestcase = nuagetestcase
            self.idx = kwargs["idx"]

        def run(self):
            self.expected_user_data = "hello world vm %s" % self.vm.name
            self.end = None
            self.start = datetime.now()
            self.nuagetestcase.when_I_update_userdata(self.vm,
                                                      self.expected_user_data)
            self.end = datetime.now()
            self.nuagetestcase.debug("[Concurrency]Update userdata idx=%d "
                                     "for vm: %s. Duration in seconds: %s " %
                                     (self.idx, self.vm.name,
                                      (self.end - self.start).total_seconds()))
            return self.expected_user_data

        def get_vm(self):
            return self.vm

        def get_timestamps(self):
            if not self.end:
                self.end = datetime.now()
            return [self.start, self.end]

        def get_userdata(self):
            return self.expected_user_data

        @staticmethod
        def get_name():
            return "userdata"

    class ResetPassword(threading.Thread):

        def __init__(self, nuagetestcase, vm, **kwargs):
            threading.Thread.__init__(self)
            self.vm = vm
            self.nuagetestcase = nuagetestcase

        def run(self):
            self.start = datetime.now()
            self.nuagetestcase.when_I_reset_the_password(self.vm)
            self.end = datetime.now()
            self.nuagetestcase.debug("[Concurrency]Reset password for vm: %s. "
                                     "Duration in seconds: %s "
                                     %
                                     (self.vm.name,
                                      (self.end - self.start).total_seconds()))

        def get_vm(self):
            return self.vm

        def get_timestamps(self):
            if not self.end:
                self.end = datetime.now()
            return [self.start, self.end]

        def get_password(self):
            return self.vm.password

        @staticmethod
        def get_name():
            return "reset password"

    def __init__(self, methodName='runTest'):
        super(TestNuageConfigDrive, self).__init__(methodName)
        ConfigDriveUtils.__init__(self)

    @classmethod
    def setUpClass(cls):
        super(TestNuageConfigDrive, cls).setUpClass()
        return

    def setUp(self):
        # Create an account
        self.account = Account.create(self.api_client,
                                      self.test_data["account"],
                                      admin=True,
                                      domainid=self.domain.id
                                      )
        self.tmp_files = []
        self.cleanup = [self.account]
        self.generate_ssh_keys()
        return

    def tearDown(self):
        super(TestNuageConfigDrive, self).tearDown()
        for tmp_file in self.tmp_files:
            os.remove(tmp_file)

        self.update_template(passwordenabled=False)
        return

    def validate_acl_rule(self, fw_rule):
        self.verify_vsd_firewall_rule(fw_rule)

    def validate_StaticNat_rule_For_VM(self, public_ip, network, vm):
        self.verify_vsd_floating_ip(network, vm, public_ip.ipaddress, self.vpc)

    def validate_vm_networking(self, vm):
        self.verify_vsd_vm(vm)

    def validate_network_networking(self, network, vpc):
        self.verify_vsd_network(self.domain.id, network, vpc=vpc)

    def _get_test_data(self, key):
        return self.test_data["nuagevsp"][key]

    def get_configdrive_provider(self):
        return NetworkServiceProvider.list(
            self.api_client,
            name="ConfigDrive",
            physicalnetworkid=self.vsp_physical_network.id)[0]

    def get_vpc_offering_name(self):
        return "vpc_offering_configdrive_withoutdns"

    def get_network_offering_name(self):
        return "isolated_configdrive_network_offering_withoutdns"

    def get_network_offering_name_for_vpc(self):
        return "vpc_network_offering_configdrive_withoutdns"

    # =========================================================================
    # ---                    Gherkin style helper methods                   ---
    # =========================================================================

    def then_vr_is_as_expected(self, network):
        """Then there is a VR or not in network {network.id}"""
        if "Dns" in [s.name for s in network.service]:
            vr = self.check_Router_state(network=network, state="Running")
            self.verify_vsd_router(vr)
        else:
            with self.assertRaises(Exception):
                self.get_Router(network)
            self.debug("+++Verified no VR is spawned for this network ")

    def when_I_change_the_network_offering_to(self, network, offering_name):
        updated_network =\
            self.upgrade_Network(self._get_test_data(offering_name), network)
        network.service = updated_network.service

    # =========================================================================
    # ---                            TEST CASES                             ---
    # =========================================================================

    @attr(tags=["advanced", "nuagevsp", "isonw"], required_hardware="true")
    def test_nuage_config_drive_isolated_network_with_vr(self):
        self.debug("+++Test user data & password reset functionality "
                   "using configdrive in an Isolated network with VR")

        self.given_config_drive_provider_is("Enabled")
        self.given_template_password_enabled_is(True)
        self.given_a_network_offering("isolated_configdrive_network_offering")
        create_vrnetwork =\
            self.when_I_create_a_network_with_that_offering(gateway='10.1.3.1')
        self.then_the_network_is_successfully_created(create_vrnetwork)
        self.then_the_network_has(create_vrnetwork, state="Allocated")

        vrnetwork = create_vrnetwork.network

        vm = self.when_I_deploy_a_vm(vrnetwork)
        self.then_vr_is_as_expected(network=vrnetwork)

        # We need to have the vm password
        self.when_I_reset_the_password(vm)
        public_ip = self.when_I_create_a_static_nat_ip_to(vm, vrnetwork)

        self.then_config_drive_is_as_expected(vm, public_ip, metadata=True)

        self.update_and_validate_userdata(vm, "helloworld vm", public_ip)
        self.then_config_drive_is_as_expected(vm, public_ip)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "verify config drive after restart Isolated network without"
                   " cleanup...")
        self.when_I_restart_the_network_with(vrnetwork, cleanup=False)
        self.then_config_drive_is_as_expected(vm, public_ip, metadata=True)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "verify config drive after restart Isolated network with"
                   " cleanup...")
        self.when_I_restart_the_network_with(vrnetwork, cleanup=True)
        self.then_config_drive_is_as_expected(vm, public_ip, metadata=True)

        self.debug("+++ Upgrade offering of created Isolated network with "
                   "an offering which removes the VR...")
        self.when_I_change_the_network_offering_to(
            vrnetwork, "isolated_configdrive_network_offering_withoutdns")
        self.then_vr_is_as_expected(network=vrnetwork)
        self.then_config_drive_is_as_expected(vm, public_ip, metadata=True)

        vm.delete(self.api_client, expunge=True)
        vrnetwork.delete(self.api_client)

    @attr(tags=["advanced", "nuagevsp", "isonw"], required_hardware="true")
    def test_nuage_configdrive_isolated_network(self):
        """Test Configdrive as provider for isolated Networks
           to provide userdata and password reset functionality
           with Nuage VSP SDN plugin
        """

        # 1. Given ConfigDrive provider is disabled in zone
        #    And a network offering which has
        #      user data provided by ConfigDrive
        #    Then creating an Isolated Network
        #    using that network offering fails

        # 2. Given ConfigDrive provider is enabled in zone
        #    And a network offering which has
        #    * user data provided by ConfigDrive
        #    * No DNS
        #    When I create an Isolated Network using that network offering
        #    Then the network is successfully created,
        #    And is in the "Allocated" state.

        # 3. When I deploy a VM in the created Isolated network with user data,
        #    Then the Isolated network state is changed to "Implemented"
        #    And the VM is successfully deployed and is in the "Running" state
        #    And there is no VR is deployed.
        # 4. And the user data in the ConfigDrive device is as expected
        # 5. And the the vm's password in the ConfigDrive device is as expected

        # 6. When I stop, reset the password, and start the VM
        # 7. Then I can login into the VM using the new password.
        # 8. SSH into the VM for verifying its new password
        #     after its password reset.

        # 9. Verify various scenarios and check the data in configdriveIso
        # 10. Delete all the created objects (cleanup).

        self.debug("+++ Scenario: creating an Isolated network with "
                   "config drive fails when config drive provider is "
                   "disabled.")
        self.given_config_drive_provider_is("Disabled")
        self.given_a_network_offering_with_configdrive()
        self.then_creating_a_network_with_that_offering_fails()

        self.debug("+++ Preparation Scenario: "
                   "creating an Isolated networks with "
                   "config drive when config drive provider is "
                   "enabled.")

        self.given_config_drive_provider_is("Enabled")

        create_network1 = self.when_I_create_a_network_with_that_offering()
        self.then_the_network_is_successfully_created(create_network1)
        self.then_the_network_has(create_network1, state="Allocated")

        create_network2 = self.when_I_create_a_network_with_that_offering()
        self.then_the_network_is_successfully_created(create_network2)
        self.then_the_network_has(create_network2, state="Allocated")

        network1 = create_network1.network
        network2 = create_network2.network

        self.given_template_password_enabled_is(True)

        self.debug("+++Deploy VM in the created Isolated network "
                   "with user data provider as configdrive")

        vm1 = self.when_I_deploy_a_vm_with_keypair_in(network1)
        public_ip_1 = \
            self.when_I_create_a_static_nat_ip_to(vm1, network1)

        self.then_vr_is_as_expected(network=network1)
        self.then_config_drive_is_as_expected(
            vm1, public_ip_1,
            metadata=True)

        self.update_and_validate_userdata(vm1, "helloworld vm1", public_ip_1)
        self.update_and_validate_sshkeypair(vm1, public_ip_1)

        # =====================================================================

        self.debug("Adding a non-default nic to the VM "
                   "making it a multi-nic VM...")
        self.plug_nic(vm1, network2)
        self.then_config_drive_is_as_expected(vm1, public_ip_1,
                                              metadata=True, reconnect=False)

        with self.stopped_vm(vm1):
            self.when_I_reset_the_password(vm1)
            self.when_I_update_userdata(vm1, "hellomultinicvm1")

        self.then_config_drive_is_as_expected(vm1, public_ip_1)

        # =====================================================================
        # Test using network2 as default network
        # =====================================================================

        self.debug("updating non-default nic as the default nic "
                   "of the multi-nic VM and enable staticnat...")
        self.update_default_nic(vm1, network2)

        public_ip_2 = \
            self.when_I_create_a_static_nat_ip_to(vm1, network2)
        self.stop_and_start_vm(vm1)
        self.then_config_drive_is_as_expected(vm1, public_ip_2, metadata=True)

        self.when_I_reset_the_password(vm1)
        self.then_config_drive_is_as_expected(vm1, public_ip_2)

        user_data = "hellomultinicvm1again"
        self.update_and_validate_userdata(vm1, user_data, public_ip_2)

        self.debug("Updating the default nic of the multi-nic VM, "
                   "deleting the non-default nic...")
        self.update_default_nic(vm1, network1)
        self.stop_and_start_vm(vm1)
        self.then_config_drive_is_as_expected(vm1, public_ip_1, metadata=True)

        self.delete(public_ip_2)
        self.unplug_nic(vm1, network2)

        # =====================================================================
        # Another Multinic VM
        # =====================================================================
        self.debug("+++ Scenario: "
                   "Reset password and update userdata on a multi nic vm")
        multinicvm1 = self.when_I_deploy_a_vm([network2, network1])
        self.when_I_reset_the_password(multinicvm1)
        public_ip_3 = self.when_I_create_a_static_nat_ip_to(multinicvm1,
                                                            network2)
        self.then_config_drive_is_as_expected(
            multinicvm1, public_ip_3,
            metadata=True)

        user_data2 = "hello multinicvm1"
        self.update_and_validate_userdata(multinicvm1, user_data2, public_ip_3)

        self.delete(multinicvm1, expunge=True)
        self.delete(public_ip_3)
        self.delete(network2)

        # =====================================================================
        # Network restart tests
        # =====================================================================

        self.debug("+++ Scenario: "
                   "verify config drive after restart Isolated network without"
                   " cleanup...")
        self.when_I_reset_the_password(vm1)
        self.when_I_restart_the_network_with(network1, cleanup=False)
        self.then_config_drive_is_as_expected(vm1, public_ip_1,
                                              metadata=True, reconnect=False)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "verify config drive after restart Isolated network with"
                   " cleanup...")
        self.when_I_restart_the_network_with(network1, cleanup=True)
        self.then_config_drive_is_as_expected(vm1, public_ip_1,
                                              metadata=True, reconnect=False)

        # =====================================================================
        # Nuage --
        #   Update offering to VR
        # =====================================================================
        self.debug("+++ Upgrade offering of created Isolated network with "
                   "a dns offering which spins a VR")
        self.upgrade_Network(self.test_data["nuagevsp"][
                                 "isolated_configdrive_network_offering"],
                             create_network1.network)
        vr = self.get_Router(create_network1.network)
        self.check_Router_state(vr, state="Running")
        # VSD verification
        self.verify_vsd_network(self.domain.id, create_network1.network)
        self.verify_vsd_router(vr)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "update userdata and reset password after reboot")
        vm1.reboot(self.api_client)
        self.then_config_drive_is_as_expected(vm1, public_ip_1, metadata=True)
        self.update_and_validate_userdata(vm1, "hello afterboot", public_ip_1)
        self.when_I_reset_the_password(vm1)
        self.then_config_drive_is_as_expected(vm1, public_ip_1)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "update userdata and reset password after migrate")
        self.migrate_VM(vm1)
        self.then_config_drive_is_as_expected(vm1, public_ip_1, metadata=True)
        self.debug("Updating userdata after migrating VM - %s" % vm1.name)
        self.update_and_validate_userdata(vm1, "hello after migrate",
                                          public_ip_1)
        self.when_I_reset_the_password(vm1)
        self.then_config_drive_is_as_expected(vm1, public_ip_1)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "update userdata and reset password after stop/start")
        self.stop_and_start_vm(vm1)
        self.then_config_drive_is_as_expected(vm1, public_ip_1, metadata=True)
        self.update_and_validate_userdata(vm1, "hello afterstopstart",
                                          public_ip_1)
        self.when_I_reset_the_password(vm1)
        self.then_config_drive_is_as_expected(vm1, public_ip_1)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "verify config drive after delete/recover")
        self.delete_and_recover_vm(vm1)
        self.then_config_drive_is_as_expected(vm1, public_ip_1,
                                              metadata=True)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "Start VM fails when ConfigDrive provider is disabled")
        self.given_config_drive_provider_is("Disabled")
        with self.assertRaises(Exception):
            self.when_I_update_userdata(vm1, "hi with provider state Disabled")
        self.given_config_drive_provider_is("Enabled")

        self.delete(vm1, expunge=True)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "Update Userdata on a VM that is not password enabled")
        self.update_template(passwordenabled=False)
        vm1 = self.when_I_deploy_a_vm_with_keypair_in(network1)

        public_ip_1 = \
            self.when_I_create_a_static_nat_ip_to(vm1, network1)

        self.update_and_validate_userdata(vm1,
                                          "This is sample data",
                                          public_ip_1,
                                          metadata=True)

    @attr(tags=["advanced", "nuagevsp", "vpc"], required_hardware="true")
    def test_nuage_configdrive_vpc_network_with_vr(self):
        self.debug("Testing user data & password reset functionality "
                   " using configdrive in a VPC network with VR...")

        self.given_config_drive_provider_is("Enabled")
        self.given_template_password_enabled_is(True)
        self.given_a_vpc_with_offering("vpc_offering_configdrive_withdns")
        self.given_a_network_offering(
                "vpc_network_offering_configdrive_withdns")
        create_tier = self.when_I_create_a_vpc_tier_with_that_offering(
            gateway='10.1.3.1')
        self.then_the_network_is_successfully_created(create_tier)
        self.then_the_network_has(create_tier, state="Implemented")

        tier = create_tier.network

        # =====================================================================
        self.debug("+++ Scenario: "
                   "Deploy VM in the Tier 1 with user data")
        vm = self.when_I_deploy_a_vm(tier)
        self.then_vr_is_as_expected(network=tier)

        public_ip = \
            self.when_I_create_a_static_nat_ip_to(vm, tier)
        self.then_config_drive_is_as_expected(vm, public_ip, metadata=True)

        self.when_I_reset_the_password(vm)
        self.then_config_drive_is_as_expected(vm, public_ip)

        self.update_and_validate_userdata(vm, "helloworld vm2", public_ip,
                                          metadata=True)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "Restarting the created vpc without cleanup...")
        self.when_I_restart_the_vpc_with(cleanup=False)
        self.then_config_drive_is_as_expected(vm, public_ip, metadata=True)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "verify config drive after Restart VPC with cleanup...")
        self.when_I_restart_the_vpc_with(cleanup=True)
        self.then_config_drive_is_as_expected(vm, public_ip,
                                              metadata=True, reconnect=False)
        # =====================================================================
        self.debug("+++ Scenario: "
                   "verify config drive after Restart tier without cleanup...")
        self.when_I_restart_the_network_with(tier, cleanup=False)
        self.then_config_drive_is_as_expected(vm, public_ip,
                                              metadata=True, reconnect=False)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "verify config drive after restart tier with cleanup...")
        self.when_I_restart_the_network_with(tier, cleanup=True)
        self.then_config_drive_is_as_expected(vm, public_ip,
                                              metadata=True, reconnect=False)

        self.debug("+++ Upgrade offering of created VPC network with "
                   "an offering which removes the VR...")
        self.when_I_change_the_network_offering_to(
            tier, "vpc_network_offering_configdrive_withoutdns")
        self.then_vr_is_as_expected(network=tier)
        self.then_config_drive_is_as_expected(vm, public_ip,
                                              metadata=True, reconnect=False)

        vm.delete(self.api_client, expunge=True)
        tier.delete(self.api_client)
        self.vpc.delete(self.api_client)

    @attr(tags=["advanced", "nuagevsp", "vpc"], required_hardware="true")
    def test_nuage_configdrive_vpc_network(self):
        """Test Configdrive for VPC Networks
           choose user data with configDrive as service provider
           and test password reset functionality using ConfigDrive
           with Nuage VSP SDN plugin
        """

        # 1. Given ConfigDrive provider is disabled in zone
        #    And a network offering for VPC which has
        #      user data provided by ConfigDrive
        #    And a VPC
        #    Then creating an VPC Tier in the VPC
        #    using that network offering fails

        # 2. Given ConfigDrive provider is enabled in zone
        #    And a network offering for VPC which has
        #      user data provided by ConfigDrive
        #    And a VPC
        #    When I create an VPC Tier in the VPC  using that network offering
        #    Then the network is successfully created,
        #    And is in the "Allocated" state.

        # 3. When I deploy a VM in the created VPC tier with user data,
        #    Then the network state is changed to "Implemented"
        #    And the VM is successfully deployed and is in the "Running" state

        # 4. And the user data in the ConfigDrive device is as expected
        # 5. And the the vm password in the ConfigDrive device is as expected

        # 6. When I stop, reset the password, and start the VM
        # 7. Then I can login into the VM using the new password.
        # 8. And the the vm password in the ConfigDrive device is the new one

        # 9. Verify various scenarios and check the data in configdriveIso
        # 10. Delete all the created objects (cleanup).

        self.debug("+++ Scenario: creating an VPC tier with "
                   "config drive fails when config drive provider is "
                   "disabled.")
        self.given_a_vpc()
        self.given_config_drive_provider_is("Disabled")
        self.given_a_network_offering_for_vpc_with_configdrive()
        self.then_creating_a_vpc_tier_with_that_offering_fails()

        self.debug("+++ Preparation Scenario: "
                   "Create 2 tier with config drive "
                   "when config drive provider is enabled.")

        self.given_config_drive_provider_is("Enabled")

        create_network1 = self.when_I_create_a_vpc_tier_with_that_offering(
            gateway='10.1.1.1')
        self.then_the_network_is_successfully_created(create_network1)
        self.then_the_network_has(create_network1, state="Implemented")

        create_network2 = self.when_I_create_a_vpc_tier_with_that_offering(
            gateway='10.1.2.1')
        self.then_the_network_is_successfully_created(create_network2)
        self.then_the_network_has(create_network2, state="Implemented")

        network1 = create_network1.network
        network2 = create_network2.network

        self.given_template_password_enabled_is(True)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "Deploy VM in the Tier 1 with user data")
        vm = self.when_I_deploy_a_vm(network1,
                                     keypair=self.keypair.name)
        public_ip_1 = \
            self.when_I_create_a_static_nat_ip_to(vm, network1)
        self.then_config_drive_is_as_expected(vm, public_ip_1, metadata=True)

        self.update_and_validate_userdata(vm, "helloworld vm1",
                                          public_ip_1,
                                          metadata=True)

        self.when_I_reset_the_password(vm)
        self.then_config_drive_is_as_expected(vm, public_ip_1)

        self.update_and_validate_sshkeypair(vm, public_ip_1)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "Restarting the created vpc without cleanup...")
        self.when_I_restart_the_vpc_with(cleanup=False)
        self.then_config_drive_is_as_expected(vm, public_ip_1, metadata=True)

        # =====================================================================
        self.debug("Adding a non-default nic to the VM "
                   "making it a multi-nic VM...")
        self.plug_nic(vm, network2)
        self.then_config_drive_is_as_expected(vm, public_ip_1,
                                              metadata=True, reconnect=False)
        with self.stopped_vm(vm):
            self.when_I_reset_the_password(vm)
            self.when_I_update_userdata(vm, "hellomultinicvm1")

        self.then_config_drive_is_as_expected(vm, public_ip_1, metadata=True)

        self.unplug_nic(vm, network2)
        self.delete(network2)

        self.debug("+++ Scenario: "
                   "verify config drive after Restart VPC with cleanup...")
        self.when_I_restart_the_vpc_with(cleanup=True)
        self.then_config_drive_is_as_expected(vm, public_ip_1,
                                              metadata=True, reconnect=False)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "verify config drive after Restart tier without cleanup...")
        self.when_I_restart_the_network_with(network1, cleanup=False)
        self.then_config_drive_is_as_expected(vm, public_ip_1,
                                              metadata=True, reconnect=False)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "verify config drive after restart tier with cleanup...")
        self.when_I_restart_the_network_with(network1, cleanup=True)
        self.then_config_drive_is_as_expected(vm, public_ip_1,
                                              metadata=True, reconnect=False)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "update userdata and reset password after reboot")
        vm.reboot(self.api_client)
        self.then_config_drive_is_as_expected(vm, public_ip_1, metadata=True)
        self.update_and_validate_userdata(vm, "hello reboot", public_ip_1)

        self.when_I_reset_the_password(vm)
        self.then_config_drive_is_as_expected(vm, public_ip_1)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "update userdata and reset password after migrate")
        self.migrate_VM(vm)
        self.then_config_drive_is_as_expected(vm, public_ip_1, metadata=True)
        self.update_and_validate_userdata(vm, "hello migrate", public_ip_1)

        self.when_I_reset_the_password(vm)
        self.then_config_drive_is_as_expected(vm, public_ip_1)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "update userdata and reset password after stop/start")
        self.stop_and_start_vm(vm)
        self.then_config_drive_is_as_expected(vm, public_ip_1, metadata=True)

        self.update_and_validate_userdata(vm, "hello stop/start", public_ip_1)

        self.when_I_reset_the_password(vm)
        self.then_config_drive_is_as_expected(vm, public_ip_1)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "verify config drive after delete/recover")
        self.delete_and_recover_vm(vm)
        self.then_config_drive_is_as_expected(vm, public_ip_1, metadata=True)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "Verify configdrive when template is not password enabled")
        self.given_config_drive_provider_is("Disabled")
        self.then_config_drive_is_as_expected(vm, public_ip_1,
                                              metadata=True, reconnect=False)
        self.given_config_drive_provider_is("Enabled")

        self.delete(vm, expunge=True)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "Update Userdata on a VM that is not password enabled")

        self.update_template(passwordenabled=False)

        vm = self.when_I_deploy_a_vm(network1,
                                     keypair=self.keypair.name)
        public_ip_1 = \
            self.when_I_create_a_static_nat_ip_to(vm, network1)
        self.update_and_validate_userdata(vm, "This is sample data",
                                          public_ip_1,
                                          metadata=True)

        self.delete(vm, expunge=True)
        self.delete(network1)

    def handle_threads(self, source_threads, thread_class, **kwargs):
        my_threads = []
        for aThread in source_threads:
            my_vm = aThread.get_vm()
            self.debug("[Concurrency]%s in vm: %s"
                       % (thread_class.get_name(), my_vm.name))
            new_thread = thread_class(self, my_vm, **kwargs)
            my_threads.append(new_thread)
            new_thread.start()
        #
        # Wait until all threads are finished
        self.wait_until_done(my_threads, thread_class.get_name())
        return my_threads

    @attr(
        tags=["advanced", "nuagevsp", "concurrency"], required_hardware="true")
    def test_nuage_configDrive_concurrency(self):
        """ Verify concurrency of ConfigDrive userdata update & password reset
        """

        # Validate the following
        # 1. When ConfigDrive is enabled as provider in zone
        #    Create an Isolated Network with Nuage VSP Isolated Network
        #    offering specifying ConfigDrive as serviceProvider for userdata,
        #    make sure no Dns is in the offering so no VR is spawned.
        #    check if it is successfully created and is in the "Allocated"
        #    state.
        # 2. Concurrently create a number of VM's in the above isolated network
        # 3. Wait until all VM's are running
        # 4. Concurrently update the userdata of all the VM's
        # 5. Wait util all updates are finished
        # 6. Repeat above (5-6) x times
        # 7. Check userdata in all VM's
        # 8. Concurrently reset password on all VM's
        # 9. Wait until all resets are finished
        # 10. Verify all passwords
        # 11. Concurrently delete all VM's.
        # 12. Restore ConfigDrive provider state
        # 13. Delete all the created objects (cleanup).

        #
        #  1. When ConfigDrive enabled create network
        default_state = self.given_config_drive_provider_is("Enabled")
        create_network = self.verify_network_creation(
            offering_name="isolated_configdrive_network_offering_withoutdns",
            gateway='10.1.1.1')
        #
        # 2. Concurrently create all VMs
        self.password_enabled = self.given_template_password_enabled_is(False)
        my_create_threads = []
        nbr_vms = 5
        for i in range(nbr_vms):
            # Add VM
            self.debug("+++ [Concurrency]Going to verify %d VM's, starting "
                       "the %d VM" % (nbr_vms, i + 1))
            vm_thread = self.StartVM(self, create_network.network, i)
            my_create_threads.append(vm_thread)
            vm_thread.start()
        #
        # 3. Wait until all VM's are running
        self.wait_until_done(my_create_threads, "creation")
        self.assertEqual(
            nbr_vms, len(my_create_threads), "Not all VM's are up")

        try:
            for i in range(2):
                self.debug("\n+++ [Concurrency]Start update on all VM's")
                #
                # 5. Concurrently update all VM's
                my_update_threads = self.handle_threads(my_create_threads,
                                                        self.UpdateVM, idx=i)

                first = my_update_threads[0].get_timestamps()
                last = my_update_threads[-1].get_timestamps()
                self.debug("[Concurrency] Update report: first start %s, "
                           "last start %s. Duration in seconds: %s" %
                           (first[0].strftime("%H:%M:%S-%f"),
                            last[0].strftime("%H:%M:%S-%f"),
                            (last[0] - first[0]).total_seconds()))
                self.debug("[Concurrency] Update report: first end %s, "
                           "last end %s. Duration in seconds: %s" %
                           (first[1].strftime("%H:%M:%S-%f"),
                            last[1].strftime("%H:%M:%S-%f"),
                            (last[0] - first[0]).total_seconds()))
            #
            # 7. Check userdata in all VM's
            self.debug("\n+++ [Concurrency]Check userdata")
            public_ip_1 = self.acquire_PublicIPAddress(create_network.network)
            for aThread in my_update_threads:
                #
                # create floating ip
                self.when_I_create_a_static_nat_ip_to(aThread.get_vm(),
                                                      create_network.network,
                                                      public_ip_1)
                #
                # verify userdata
                self.debug("[Concurrency]verify userdata for vm %s"
                           % aThread.get_vm().name)
                self.then_config_drive_is_as_expected(
                    aThread.get_vm(), public_ip_1)
                self.delete_StaticNatRule_For_VM(public_ip_1)
            #
            #  8. Concurrently reset password on all VM's
            self.given_template_password_enabled_is(True)
            my_reset_threads = self.handle_threads(my_create_threads,
                                                   self.ResetPassword)
            #
            # 10. Verify the passwords
            self.debug("\n+++ [Concurrency]Verify passwords on all VM's")
            for aThread in my_reset_threads:
                # create floating ip
                self.when_I_create_a_static_nat_ip_to(aThread.get_vm(),
                                                      create_network.network,
                                                      public_ip_1)

                # verify password
                self.debug("[Concurrency]verify password for vm %s"
                           % aThread.get_vm().name)
                self.then_config_drive_is_as_expected(
                    aThread.get_vm(), public_ip_1)
                self.delete_StaticNatRule_For_VM(public_ip_1)
            public_ip_1.delete(self.api_client)

            self.debug("\n+++ [Concurrency]Stop all VM's")

        finally:
            self.given_template_password_enabled_is(self.password_enabled)
            #
            # 11. Concurrently delete all VM's.
            self.handle_threads(my_create_threads, self.StopVM)
            #
            # 12. Restore ConfigDrive provider state
            self.given_config_drive_provider_is(default_state)
            #
            # 13. Delete all the created objects (cleanup).
            self.delete_Network(create_network.network)

    @attr(tags=["advanced", "nuagevsp", "shared"], required_hardware="true")
    def test_nuage_configdrive_shared_network(self):
        """Test Configdrive as provider for shared Networks
           to provide userdata and password reset functionality
           with Nuage VSP SDN plugin
        """

        # 1. When ConfigDrive is disabled as provider in zone
        #    Verify Shared Network creation with a network offering
        #    which has userdata provided by ConfigDrive fails
        # 2. When ConfigDrive is enabled as provider in zone
        #    Create a shared Network with Nuage VSP Isolated Network
        #    offering specifying ConfigDrive as serviceProvider
        #    for userdata,
        #    make sure no Dns is in the offering so no VR is spawned.
        #    check if it is successfully created and
        #    is in the "Allocated" state.
        # 3. Deploy a VM in the created Shared network with user data,
        #    check if the Shared network state is changed to
        #    "Implemented", and the VM is successfully deployed and
        #    is in the "Running" state.
        #    Check that no VR is deployed.
        # 4. SSH into the deployed VM and verify its user data in the iso
        #    (expected user data == actual user data).
        # 5. Verify that the guest VM's password in the iso.
        # 6. Reset VM password, and start the VM.
        # 7. Verify that the new guest VM template is password enabled by
        #     checking the VM's password (password != "password").
        # 8. SSH into the VM for verifying its new password
        #     after its password reset.
        # 9. Verify various scenarios and check the data in configdriveIso
        # 10. Delete all the created objects (cleanup).

        if not self.isNuageInfraUnderlay:
            self.skipTest(
                "Configured Nuage VSP SDN platform infrastructure "
                "does not support underlay networking: "
                "skipping test")

        self.debug("+++Testing configdrive in an shared network fails..."
                   "as provider configdrive is still disabled...")
        self.given_config_drive_provider_is("Disabled")
        shared_test_data = self.test_data["nuagevsp"]["network_all"]
        shared_network = self.verify_network_creation(
            offering_name="shared_nuage_network_config_drive_offering",
            testdata=shared_test_data)
        self.assertFalse(shared_network.success,
                         'Network found success = %s, expected success =%s'
                         % (str(shared_network.success), 'False'))

        self.given_config_drive_provider_is("Enabled")
        shared_network = self.verify_network_creation(
            offering=shared_network.offering, testdata=shared_test_data)
        self.assertTrue(shared_network.success,
                        'Network found success = %s, expected success = %s'
                        % (str(shared_network.success), 'True'))

        self.validate_Network(shared_network.network, state="Allocated")

        shared_test_data2 = self.test_data["nuagevsp"]["network_all2"]
        shared_network2 = self.verify_network_creation(
            offering=shared_network.offering,
            testdata=shared_test_data2)
        self.assertTrue(shared_network2.success,
                        'Network found success = %s, expected success = %s'
                        % (str(shared_network2.success), 'True'))

        self.validate_Network(shared_network2.network, state="Allocated")

        self.debug("+++Test user data & password reset functionality "
                   "using configdrive in an Isolated network without VR")

        self.given_template_password_enabled_is(True)
        public_ip_ranges = PublicIpRange.list(self.api_client)
        for ip_range in public_ip_ranges:
            if shared_network.network.id == ip_range.networkid \
                    or shared_network2.network.id == ip_range.networkid:
                self.enable_NuageUnderlayPublicIpRange(ip_range.id)

        self.generate_ssh_keys()
        self.debug("keypair name %s " % self.keypair.name)

        self.debug("+++Deploy of a VM on a shared network with multiple "
                   "ip ranges, all should have the same value for the "
                   "underlay flag.")
        # Add subnet of different gateway
        self.debug("+++ Adding subnet of different gateway")

        subnet = self.add_subnet_to_shared_network_and_verify(
            shared_network.network,
            self.test_data["nuagevsp"]["publiciprange2"])
        tmp_test_data = copy.deepcopy(
            self.test_data["virtual_machine"])

        tmp_test_data["ipaddress"] = \
            self.test_data["nuagevsp"]["network_all"]["endip"]

        with self.assertRaises(Exception):
            self.create_VM(
                [shared_network.network],
                testdata=tmp_test_data)

        self.debug("+++ In a shared network with multiple ip ranges, "
                   "userdata with config drive must be allowed.")

        self.enable_NuageUnderlayPublicIpRange(subnet.vlan.id)

        vm1 = self.create_VM(
            [shared_network.network],
            testdata=self.test_data["virtual_machine_userdata"],
            keypair=self.keypair.name)
        # Check VM
        self.check_VM_state(vm1, state="Running")
        # Verify shared Network and VM in VSD
        self.verify_vsd_shared_network(
            self.domain.id,
            shared_network.network,
            gateway=self.test_data["nuagevsp"]["network_all"]["gateway"])
        subnet_id = self.get_subnet_id(
            shared_network.network.id,
            self.test_data["nuagevsp"]["network_all"]["gateway"])
        self.verify_vsd_enterprise_vm(
            self.domain.id,
            shared_network.network, vm1,
            sharedsubnetid=subnet_id)

        with self.assertRaises(Exception):
            self.get_Router(shared_network)
        self.debug("+++ Verified no VR is spawned for this network ")
        # We need to have the vm password
        self.when_I_reset_the_password(vm1)
        self.debug("Password reset to - %s" % vm1.password)
        self.debug("VM - %s password - %s !" %
                   (vm1.name, vm1.password))
        public_ip = PublicIPAddress({"ipaddress": vm1})
        self.then_config_drive_is_as_expected(
            vm1, public_ip,
            metadata=True)
        expected_user_data = self.update_and_validate_userdata(vm1,
                                                               "helloworldvm1",
                                                               public_ip)
        self.then_config_drive_is_as_expected(
            vm1, public_ip)

        self.debug("+++ Adding a non-default nic to the VM "
                   "making it a multi-nic VM...")
        self.nic_operation_VM(vm1, shared_network2.network,
                              operation="add")
        self.then_config_drive_is_as_expected(vm1, public_ip,
                                              metadata=True)
        self.when_I_reset_the_password(vm1)
        self.debug("Password reset to - %s" % vm1.password)
        self.debug("VM - %s password - %s !" %
                   (vm1.name, vm1.password))

        expected_user_data1 = self.update_and_validate_userdata(vm1,
                                                                "himultinicvm",
                                                                public_ip)
        self.then_config_drive_is_as_expected(vm1, public_ip)

        self.debug("+++ Updating non-default nic as the default nic "
                   "of the multi-nic VM...")
        self.nic_operation_VM(vm1,
                              shared_network2.network, operation="update")
        vm1.stop(self.api_client)
        vm1.start(self.api_client)

        public_ip_2 = PublicIPAddress(
            {"ipaddress": VirtualMachine.list(self.api_client,
                                              id=vm1.id)[0].nic[1]})
        self.then_config_drive_is_as_expected(vm1, public_ip_2,
                                              metadata=True)
        self.when_I_reset_the_password(vm1)
        self.debug("Password reset to - %s" % vm1.password)
        self.debug("VM - %s password - %s !" %
                   (vm1.name, vm1.password))
        self.then_config_drive_is_as_expected(vm1, public_ip_2)
        expected_user_data1 = self.update_and_validate_userdata(vm1,
                                                                "himultinicvm",
                                                                public_ip)
        self.then_config_drive_is_as_expected(vm1, public_ip_2)

        self.debug("+++ Updating the default nic of the multi-nic VM, "
                   "deleting the non-default nic...")
        self.nic_operation_VM(vm1,
                              shared_network.network, operation="update")
        vm1.stop(self.api_client)
        vm1.start(self.api_client)
        public_ip = PublicIPAddress({"ipaddress": vm1})
        self.then_config_drive_is_as_expected(vm1, public_ip,
                                              metadata=True)

        self.nic_operation_VM(vm1,
                              shared_network2.network, operation="remove")

        multinicvm1 = self.create_VM([shared_network2.network,
                                      shared_network.network])
        multinicvm1.password = multinicvm1.resetPassword(self.api_client)
        self.debug("+++ MultiNICVM Password reset to - %s"
                   % multinicvm1.password)
        self.debug("MultiNICVM - %s password - %s !"
                   % (multinicvm1.name, multinicvm1.password))
        public_ip_3 = \
            PublicIPAddress(
                {"ipaddress": VirtualMachine.list(
                    self.api_client, id=multinicvm1.id)[0].nic[0]})
        self.then_config_drive_is_as_expected(
            multinicvm1, public_ip_3,
            metadata=True)
        expected_user_data2 = self.update_and_validate_userdata(
            multinicvm1, "hello multinicvm1", public_ip)
        self.then_config_drive_is_as_expected(multinicvm1, public_ip_3)
        multinicvm1.delete(self.api_client, expunge=True)

        shared_network2.network.delete(self.api_client)
        # We need to have the vm password
        self.when_I_reset_the_password(vm1)
        self.debug("Password reset to - %s" % vm1.password)
        self.debug("VM - %s password - %s !" %
                   (vm1.name, vm1.password))
        public_ip = PublicIPAddress({"ipaddress": vm1})

        self.debug("+++ Verifying userdata after rebootVM - %s" % vm1.name)
        vm1.reboot(self.api_client)
        self.then_config_drive_is_as_expected(vm1, public_ip,
                                              metadata=True)

        self.debug("Updating userdata for VM - %s" % vm1.name)
        expected_user_data1 = self.update_and_validate_userdata(vm1,
                                                                "hiafterboot",
                                                                public_ip)
        self.then_config_drive_is_as_expected(vm1, public_ip)
        self.debug("Resetting password for VM - %s" % vm1.name)
        self.when_I_reset_the_password(vm1)
        self.debug("SSHing into the VM for verifying its new password "
                   "after its password reset...")
        self.then_config_drive_is_as_expected(vm1, public_ip)

        self.debug("+++ Migrating one of the VMs in the created Isolated "
                   "network to another host, if available...")
        self.migrate_VM(vm1)
        self.then_config_drive_is_as_expected(vm1, public_ip,
                                              metadata=True)

        self.debug("Updating userdata after migrating VM - %s" % vm1.name)
        expected_user_data1 = self.update_and_validate_userdata(vm1,
                                                                "aftermigrate",
                                                                public_ip)
        self.then_config_drive_is_as_expected(vm1, public_ip)
        self.debug("Resetting password for VM - %s" % vm1.name)
        self.when_I_reset_the_password(vm1)
        self.debug("SSHing into the VM for verifying its new password "
                   "after its password reset...")
        self.then_config_drive_is_as_expected(vm1, public_ip)

        self.debug("+++ Verify userdata after stopstartVM - %s" % vm1.name)
        vm1.stop(self.api_client)
        vm1.start(self.api_client)
        self.then_config_drive_is_as_expected(vm1, public_ip,
                                              metadata=True)

        self.debug("Updating userdata for VM - %s" % vm1.name)
        expected_user_data1 = self.update_and_validate_userdata(vm1,
                                                                "stopstart",
                                                                public_ip)
        self.then_config_drive_is_as_expected(vm1, public_ip)
        self.debug("Resetting password for VM - %s" % vm1.name)
        self.when_I_reset_the_password(vm1)
        self.debug("SSHing into the VM for verifying its new password "
                   "after its password reset...")
        self.then_config_drive_is_as_expected(vm1, public_ip)

        self.debug("+++ Verify userdata after VM recover- %s" % vm1.name)
        vm1.delete(self.api_client, expunge=False)
        self.debug("Recover VM - %s" % vm1.name)
        vm1.recover(self.api_client)
        vm1.start(self.api_client)
        self.then_config_drive_is_as_expected(vm1, public_ip,
                                              metadata=True)
        self.given_config_drive_provider_is("Disabled")
        expected_user_data1 = self.update_and_validate_userdata(vm1,
                                                                "afterrecover",
                                                                public_ip)
        self.then_config_drive_is_as_expected(vm1, public_ip,
                                              metadata=True)

        self.debug("+++ When template is not password enabled, "
                   "verify configdrive of VM - %s" % vm1.name)
        vm1.delete(self.api_client, expunge=True)
        self.given_config_drive_provider_is("Enabled")
        self.update_template(passwordenabled=False)
        self.generate_ssh_keys()
        self.debug("keypair name %s " % self.keypair.name)
        vm1 = self.create_VM(
            [shared_network.network],
            testdata=self.test_data["virtual_machine_userdata"],
            keypair=self.keypair.name)
        expected_user_data1 = self.update_and_validate_userdata(vm1,
                                                                "sample data",
                                                                public_ip)
        public_ip = PublicIPAddress({"ipaddress": vm1})
        self.then_config_drive_is_as_expected(vm1, public_ip,
                                              metadata=True)
        vm1.delete(self.api_client, expunge=True)
        shared_network.network.delete(self.api_client)

    @attr(tags=["advanced", "nuagevsp", "endurance"], required_hardware="true")
    def test_nuage_configdrive_endurance(self):
        """ Verify endurance of ConfigDrive userdata update
        """
        # Validate the following
        # 1. When ConfigDrive is enabled as provider in zone
        #    Create an Isolated Network with Nuage VSP Isolated Network
        #    offering specifying ConfigDrive as serviceProvider for userdata,
        #    make sure no Dns is in the offering so no VR is spawned.
        # 2. create a VM in the above isolated network
        # 3. Wait until VM is running
        # 4. Concurrently update the userdata for the VM
        # 5. Wait util all updates are finished
        # 6. Check userdata in VM
        # 7. Delete all the created objects (cleanup).

        self.given_config_drive_provider_is("Enabled")
        self.given_template_password_enabled_is(True)
        self.given_a_network_offering_with_configdrive()
        create_network = self.when_I_create_a_network_with_that_offering(
            gateway='10.5.1.1'
        )
        self.then_the_network_is_successfully_created(create_network)
        self.then_the_network_has(create_network, state="Allocated")

        network = create_network.network

        vm = self.when_I_deploy_a_vm(network, keypair=self.keypair.name)
        self.then_vr_is_as_expected(network=network)

        public_ip = \
            self.when_I_create_a_static_nat_ip_to(vm, create_network.network)
        self.then_config_drive_is_as_expected(vm, public_ip, metadata=True)

        for i in range(0, 30):
            self.update_and_validate_userdata(vm, 'This is sample data %s' % i,
                                              public_ip)

if __name__ == "__main__" and __package__ is None:
    __package__ = "integration.plugins.nuage"
