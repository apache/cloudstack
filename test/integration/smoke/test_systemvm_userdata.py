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

# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (
    getDiagnosticsData,
    stopSystemVm,
    rebootSystemVm,
    destroySystemVm,
    updateConfiguration,
)
from marvin.lib.utils import (
    cleanup_resources,
    get_process_status,
    get_host_credentials,
    wait_until,
)
from marvin.lib.base import UserData, Network
from marvin.lib.common import (
    get_zone,
    list_hosts,
    list_routers,
    list_ssvms,
    list_zones,
    list_vlan_ipranges,
    createEnabledNetworkOffering,
)
from marvin.codes import PASS
from nose.plugins.attrib import attr
import telnetlib
import logging
import base64
import os
import urllib
import zipfile
import uuid
import shutil

# Import System modules
import time

_multiprocess_shared_ = True


class TestSystemVMUserData(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestSystemVMUserData, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill services from the external config file
        cls.testData = cls.testClient.getParsedTestDataConfig()

        # Enable user data and set the script to be run on SSVM
        cmd = updateConfiguration.updateConfigurationCmd()
        cmd.name = "systemvm.userdata.enabled"
        cmd.value = "true"
        cls.api_client.updateConfiguration(cmd)

    @classmethod
    def tearDownClass(cls):
        # Disable user data
        cmd = updateConfiguration.updateConfigurationCmd()
        cmd.name = "systemvm.userdata.enabled"
        cmd.value = "false"
        cls.api_client.updateConfiguration(cmd)

    def setUp(self):
        test_case = super(TestSystemVMUserData, self)
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.cleanup = []
        self.config = test_case.getClsConfig()
        self.services = self.testClient.getParsedTestDataConfig()
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())

        self.logger = logging.getLogger("TestSystemVMUserData")
        self.stream_handler = logging.StreamHandler()
        self.logger.setLevel(logging.DEBUG)
        self.logger.addHandler(self.stream_handler)

    def tearDown(self):
        if self.userdata_id:
            UserData.delete(self.apiclient, self.userdata_id)
            self.userdata_id = None

        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def waitForSystemVMAgent(self, vmname):
        def checkRunningAgent():
            list_host_response = list_hosts(self.apiclient, name=vmname)
            if isinstance(list_host_response, list):
                return list_host_response[0].state == "Up", None
            return False, None

        res, _ = wait_until(3, 300, checkRunningAgent)
        if not res:
            raise Exception("Failed to wait for SSVM agent to be Up")

    def checkForRunningSystemVM(self, ssvm, ssvm_type=None):
        if not ssvm:
            return None

        def checkRunningState():
            if not ssvm_type:
                response = list_ssvms(self.apiclient, id=ssvm.id)
            else:
                response = list_ssvms(
                    self.apiclient, zoneid=self.zone.id, systemvmtype=ssvm_type
                )

            if isinstance(response, list):
                ssvm_response = response[0]
                return ssvm_response.state == "Running", ssvm_response
            return False, None

        res, ssvm_response = wait_until(3, 300, checkRunningState)
        if not res:
            self.fail("Failed to reach systemvm state to Running")
        return ssvm_response

    def register_userdata(
        self, userdata_name, global_setting_name, vm_type_display_name
    ):
        """Helper method to register userdata and configure the global setting

        Args:
            userdata_name: Name for the userdata entry
            global_setting_name: Global setting name to update (e.g., 'secstorage.vm.userdata', 'console.proxy.vm.userdata', 'virtual.router.userdata')
            vm_type_display_name: Display name for the VM type (e.g., 'SSVM', 'CPVM', 'VR')

        Returns:
            UserData object
        """
        userdata_script = f"""#!/bin/bash
echo "User data script ran successfully on {vm_type_display_name}" > /tmp/userdata.txt
"""
        b64_encoded_userdata = base64.b64encode(userdata_script.encode("utf-8")).decode(
            "utf-8"
        )

        # Create a userdata entry
        try:
            userdata = UserData.register(
                self.apiclient, name=userdata_name, userdata=b64_encoded_userdata
            )
            userdata_id = userdata["userdata"]["id"]
        except Exception as e:
            if "already exists" in str(e):
                self.debug("Userdata already exists, getting it")
                userdata = UserData.list(
                    self.apiclient, name=userdata_name, listall=True
                )[0]
                userdata_id = userdata.id
            else:
                self.fail("Failed to register userdata: %s" % e)

        # Update global configuration to use this userdata
        cmd = updateConfiguration.updateConfigurationCmd()
        cmd.name = global_setting_name
        cmd.value = userdata_id
        self.apiclient.updateConfiguration(cmd)
        self.debug(
            "Updated global setting %s with userdata ID: %s"
            % (global_setting_name, userdata.id)
        )

        return userdata_id

    def download_and_verify_diagnostics_data(
        self, target_id, vm_type_display_name, expected_content, retries=4
    ):
        """Helper method to download and verify diagnostics data

        Args:
            target_id: ID of the target VM/router
            vm_type_display_name: Display name for log messages (e.g., 'SSVM', 'CPVM', 'VR')
            expected_content: Expected content to verify in the userdata file
            retries: Number of retries for getDiagnosticsData (default: 4)
        """
        # Create a random temporary directory for this test
        random_suffix = uuid.uuid4().hex[:8]
        vm_type_prefix = vm_type_display_name.lower()
        temp_dir = f"/tmp/{vm_type_prefix}-{random_suffix}"
        os.makedirs(temp_dir, exist_ok=True)

        # Download the file created by userdata script using the getDiagnosticsData command
        cmd = getDiagnosticsData.getDiagnosticsDataCmd()
        cmd.targetid = target_id
        cmd.files = "/tmp/userdata.txt"

        # getDiagnosticsData command takes some time to work after a VM is started
        response = None
        while retries > -1:
            try:
                response = self.apiclient.getDiagnosticsData(cmd)
                break  # Success, exit retry loop
            except Exception as e:
                if retries >= 0:
                    retries = retries - 1
                    self.debug(
                        "getDiagnosticsData failed (retries left: %d): %s"
                        % (retries + 1, e)
                    )
                    if retries > -1:
                        time.sleep(30)
                        continue
                # If all retries exhausted, re-raise the exception
                self.fail("Failed to get diagnostics data after retries: %s" % e)

        # Download response.url file to temporary directory and extract it
        zip_file_path = os.path.join(temp_dir, "userdata.zip")
        extracted_file_path = os.path.join(temp_dir, "userdata.txt")

        self.debug(
            "Downloading userdata file from %s to %s"
            % (vm_type_display_name, zip_file_path)
        )
        try:
            urllib.request.urlretrieve(response.url, zip_file_path)
        except Exception as e:
            self.fail(
                "Failed to download userdata file from %s: %s"
                % (vm_type_display_name, e)
            )
        self.debug(
            "Downloaded userdata file from %s: %s"
            % (vm_type_display_name, zip_file_path)
        )

        try:
            with zipfile.ZipFile(zip_file_path, "r") as zip_ref:
                zip_ref.extractall(temp_dir)
        except zipfile.BadZipFile as e:
            self.fail("Downloaded userdata file is not a zip file: %s" % e)
        self.debug("Extracted userdata file from zip: %s" % extracted_file_path)

        # Verify the file contains the expected content
        try:
            with open(extracted_file_path, "r") as f:
                content = f.read().strip()
            self.debug("Userdata file content: %s" % content)
            self.assertEqual(
                expected_content in content,
                True,
                f"Check that userdata file contains expected content: '{expected_content}'",
            )
        except FileNotFoundError:
            self.fail(
                "Userdata file not found in extracted zip at %s" % extracted_file_path
            )
        except Exception as e:
            self.fail("Failed to read userdata file: %s" % e)
        finally:
            # Clean up temporary directory
            try:
                if os.path.exists(temp_dir):
                    shutil.rmtree(temp_dir)
                    self.debug("Cleaned up temporary directory: %s" % temp_dir)
            except Exception as e:
                self.debug(
                    "Failed to clean up temporary directory %s: %s" % (temp_dir, e)
                )

    def test_userdata_on_systemvm(
        self, systemvm_type, userdata_name, vm_type_display_name, global_setting_name
    ):
        """Helper method to test user data functionality on system VMs

        Args:
            systemvm_type: Type of system VM ('secondarystoragevm' or 'consoleproxy')
            userdata_name: Name for the userdata entry
            vm_type_display_name: Display name for log messages (e.g., 'SSVM' or 'CPVM')
            global_setting_name: Global setting name for userdata (e.g., 'secstorage.vm.userdata' or 'console.proxy.vm.userdata')
        """
        # 1) Register userdata and configure global setting
        self.userdata_id = self.register_userdata(
            userdata_name, global_setting_name, vm_type_display_name
        )

        # 2) Get and destroy the system VM to trigger recreation with userdata
        list_ssvm_response = list_ssvms(
            self.apiclient,
            systemvmtype=systemvm_type,
            state="Running",
            zoneid=self.zone.id,
        )
        self.assertEqual(
            isinstance(list_ssvm_response, list),
            True,
            "Check list response returns a valid list",
        )

        ssvm = list_ssvm_response[0]
        self.debug("Destroying %s: %s" % (vm_type_display_name, ssvm.id))
        cmd = destroySystemVm.destroySystemVmCmd()
        cmd.id = ssvm.id
        self.apiclient.destroySystemVm(cmd)

        # 3) Wait for the system VM to be running again
        ssvm_response = self.checkForRunningSystemVM(ssvm, systemvm_type)
        self.debug(
            "%s state after restart: %s" % (vm_type_display_name, ssvm_response.state)
        )
        self.assertEqual(
            ssvm_response.state,
            "Running",
            "Check whether %s is running or not" % vm_type_display_name,
        )
        # Wait for the agent to be up
        self.waitForSystemVMAgent(ssvm_response.name)

        # 4) Download and verify the diagnostics data
        expected_content = (
            f"User data script ran successfully on {vm_type_display_name}"
        )
        self.download_and_verify_diagnostics_data(
            ssvm_response.id, vm_type_display_name, expected_content
        )

    @attr(
        tags=["advanced", "advancedns", "smoke", "basic", "sg"],
        required_hardware="true",
    )
    def test_1_userdata_on_ssvm(self):
        """Test user data functionality on SSVM"""
        self.test_userdata_on_systemvm(
            systemvm_type="secondarystoragevm",
            userdata_name="ssvm_userdata",
            vm_type_display_name="SSVM",
            global_setting_name="secstorage.vm.userdata",
        )

    @attr(
        tags=["advanced", "advancedns", "smoke", "basic", "sg"],
        required_hardware="true",
    )
    def test_2_userdata_on_cpvm(self):
        """Test user data functionality on CPVM"""
        self.test_userdata_on_systemvm(
            systemvm_type="consoleproxy",
            userdata_name="cpvm_userdata",
            vm_type_display_name="CPVM",
            global_setting_name="console.proxy.vm.userdata",
        )

    @attr(
        tags=["advanced", "advancedns", "smoke", "basic", "sg"],
        required_hardware="true",
    )
    def test_3_userdata_on_vr(self):
        """Test user data functionality on VR"""
        # 1) Register userdata and configure global setting
        self.userdata_id = self.register_userdata("vr_userdata", "virtual.router.userdata", "VR")

        # 2) Create an isolated network which will trigger VR creation with userdata
        result = createEnabledNetworkOffering(
            self.apiclient, self.testData["nw_off_isolated_persistent"]
        )
        assert result[0] == PASS, (
            "Network offering creation/enabling failed due to %s" % result[2]
        )
        isolated_persistent_network_offering = result[1]

        # Create an isolated network
        self.network = Network.create(
            self.apiclient,
            self.testData["isolated_network"],
            networkofferingid=isolated_persistent_network_offering.id,
            zoneid=self.zone.id,
        )
        self.assertIsNotNone(self.network, "Network creation failed")
        self.cleanup.append(self.network)
        self.cleanup.append(isolated_persistent_network_offering)

        # 3) Get the VR and verify it's running
        routers = list_routers(
            self.apiclient, networkid=self.network.id, state="Running"
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "Check list router response returns a valid list",
        )
        self.assertNotEqual(len(routers), 0, "Check list router response")
        router = routers[0]
        self.debug("Found VR: %s" % router.id)

        # 4) Download and verify the diagnostics data
        # VR doesn't need retries as it's freshly created
        expected_content = "User data script ran successfully on VR"
        self.download_and_verify_diagnostics_data(router.id, "VR", expected_content)
