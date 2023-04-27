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

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *

class TestSafeShutdown(cloudstackTestCase):
    """
        Tests safely shutting down the Management Server
    """

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.mgtSvrDetails = self.config.__dict__["mgtSvr"][0].__dict__
        self.cleanup = []

    def tearDown(self):
        super(TestSafeShutdown, self).tearDown()

    def isServerShutdown(self):
        sshClient = SshClient(
            self.mgtSvrDetails["mgtSvrIp"],
            22,
            self.mgtSvrDetails["user"],
            self.mgtSvrDetails["passwd"]
        )

        timeout = time.time() + 300
        while time.time() < timeout:
            command = "service cloudstack-management status | grep dead"
            results = sshClient.execute(command)

            if len(results) > 0 and "(dead)" in results[0] :
                return
            time.sleep(30)
        return self.fail("Management server did shut down, failing")

    def isManagementUp(self):
        try:
            self.apiclient.listInfrastructure(listInfrastructure.listInfrastructureCmd())
            return True
        except Exception:
            return False

    def startServer(self):
        """Start management server"""

        sshClient = SshClient(
                    self.mgtSvrDetails["mgtSvrIp"],
            22,
            self.mgtSvrDetails["user"],
            self.mgtSvrDetails["passwd"]
        )

        command = "service cloudstack-management start"
        sshClient.execute(command)

        #Waits for management to come up in 5 mins, when it's up it will continue
        timeout = time.time() + 300
        while time.time() < timeout:
            if self.isManagementUp() is True: return
            time.sleep(5)
        return self.fail("Management server did not come up, failing")

    def run_async_cmd(self) :
        return Project.create(
            self.apiclient,
            {"name": "test", "displaytext": "test"}
        )

    @attr(tags=["advanced", "smoke"])
    def test_01_prepare_and_cancel_shutdown(self):
        try :
            prepare_for_shutdown_cmd = prepareForShutdown.prepareForShutdownCmd()
            prepare_for_shutdown_cmd.managementserverid = 1
            self.apiclient.prepareForShutdown(prepare_for_shutdown_cmd)
            try :
                self.run_async_cmd()
            except Exception as e:
                self.debug("Prepare for shutdown check successful, API failure: %s" % e)
        finally :
            cancel_shutdown_cmd = cancelShutdown.cancelShutdownCmd()
            cancel_shutdown_cmd.managementserverid = 1
            response = self.apiclient.cancelShutdown(cancel_shutdown_cmd)
            self.assertEqual(
                response.shutdowntriggered,
                False,
                "Failed to cancel shutdown"
            )
            ## Just to be sure, run another async command
            project = self.run_async_cmd()
            self.cleanup.append(project)

    @attr(tags=["advanced", "smoke"])
    def test_02_trigger_shutdown(self):
        try :
            cmd = triggerShutdown.triggerShutdownCmd()
            cmd.managementserverid = 1
            self.apiclient.triggerShutdown(cmd)
            self.isServerShutdown()
        finally :
            self.startServer()
