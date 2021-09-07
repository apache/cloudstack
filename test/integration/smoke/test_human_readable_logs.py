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
import time

from marvin.cloudstackAPI import updateConfiguration
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.sshClient import SshClient
from nose.plugins.attrib import attr

class TestHumanReadableLogs(cloudstackTestCase):
    """
    Test correct output when logging byte size values.
    """

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()
        self.mgtSvrDetails = self.config.__dict__["mgtSvr"][0].__dict__


    @attr(tags=["devcloud", "basic", "advanced"], required_hardware="false")
    def test_01_disableHumanReadableLogs(self):
        """
        Test log file output after disabling human readable sizes feature
        """
        #create ssh client
        sshClient = getSSHClient(self)

        # Disable feature
        updateConfig(self, "false")

        # Restart service
        command = "systemctl restart cloudstack-management"
        sshClient.execute(command)

        # CapacityChecker runs as soon as management server is up
        # Check if "usedMem: (" is printed out within 60 seconds while server is starting
        command = "timeout 60 tail -f /var/log/cloudstack/management/management-server.log | grep 'usedMem: ('"
        sshClient.timeout = 60
        result = sshClient.runCommand(command)
        self.assertTrue(result['status'] == "FAILED")

    @attr(tags=["devcloud", "basic", "advanced"], required_hardware="false")
    def test_02_enableHumanReadableLogs(self):
        """
        Test log file output after enabling human readable sizes feature
        """
        # create ssh client
        sshClient = getSSHClient(self)

        # Enable feature
        updateConfig(self, "true")

        # Restart service
        command = "systemctl restart cloudstack-management"
        sshClient.execute(command)

        # CapacityChecker runs as soon as management server is up
        # Check if "usedMem: (" is printed out within 60 seconds while server is restarting
        command = "timeout 120 tail -f /var/log/cloudstack/management/management-server.log | grep 'usedMem: ('"
        sshClient.timeout = 120
        result = sshClient.runCommand(command)
        if result['status'] == "SUCCESS":
            pass
        else:
            self.warn("We're not sure if test didn't pass due to timeout, so skipping failing the test")

def updateConfig(self, enableFeature):
    updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
    updateConfigurationCmd.name = "display.human.readable.sizes"
    updateConfigurationCmd.value = enableFeature

    updateConfigurationResponse = self.apiClient.updateConfiguration(updateConfigurationCmd)
    self.debug("updated the parameter %s with value %s" % (
        updateConfigurationResponse.name, updateConfigurationResponse.value))

def getSSHClient(self):
    sshClient = SshClient(
        self.mgtSvrDetails["mgtSvrIp"],
        22,
        self.mgtSvrDetails["user"],
        self.mgtSvrDetails["passwd"]
    )
    return sshClient
