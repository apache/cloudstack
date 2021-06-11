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
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr

from http.server import BaseHTTPRequestHandler,HTTPServer

import socket
import sys
import _thread
import time


apiRequests = []
state = "Running"


class MockedCloudStackServer(BaseHTTPRequestHandler):
    """
        Mocked ACS Mgmt Server
    """
    def do_GET(self):
        global apiRequests, state
        command = self.path.split('command=')[1].split('&')[0]
        if command == 'startVirtualMachine':
            state = "Running"
        elif command == 'stopVirtualMachine':
            state = "Stopped"
        elif command == 'rebootVirtualMachine':
            state = "Running"

        apiRequests.append(command)

        self.send_response(200)
        self.send_header('Content-type','application/json')
        self.end_headers()

        json = "{\"listvirtualmachinesresponse\":{\"count\":1,\"virtualmachine\":[{\"id\":\"some-uuid\",\"name\":\"test-vm\",\"state\":\"%s\"}]}}" % state
        self.wfile.write(json.encode())

    def log_message(self, format, *args):
        return


class TestOutOfBandManagement(cloudstackTestCase):
    """ Test cases for out of band management
    """

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.dbclient = self.testClient.getDbConnection()
        self.services = self.testClient.getParsedTestDataConfig()
        self.mgtSvrDetails = self.config.__dict__["mgtSvr"][0].__dict__

        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.host = None
        self.server = None

        # use random port for mocked-mgmt server
        s = socket.socket()
        s.bind(('', 0))
        self.serverPort = s.getsockname()[1]
        s.close()

        self.cleanup = []
        global state, apiRequests
        state = "Running"
        apiRequests = []


    def tearDown(self):
        try:
            self.dbclient.execute("delete from oobm where driver='nestedcloudstack' and port='some-uuid'")
            cleanup_resources(self.apiclient, self.cleanup)
            if self.server:
                self.server.socket.close()
            global apiRequests
            apiRequests = []
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)


    def getHost(self, hostId=None):
        if self.host and hostId is None:
            return self.host

        response = list_hosts(
                        self.apiclient,
                        zoneid=self.zone.id,
                        type='Routing',
                        id=hostId)
        if len(response) > 0:
            self.host = response[0]
            return self.host
        raise self.skipTest("No hosts found, skipping out-of-band management test")


    def getServerIp(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.connect((self.mgtSvrDetails["mgtSvrIp"], self.mgtSvrDetails["port"]))
        return s.getsockname()[0]


    def getServerPort(self):
        return self.serverPort


    def getOobmConfigCmd(self):
        cmd = configureOutOfBandManagement.configureOutOfBandManagementCmd()
        cmd.driver = 'nestedcloudstack'
        cmd.address = 'http://%s:%s/client/api' % (self.getServerIp(), self.getServerPort())
        cmd.port = 'some-uuid'
        cmd.username = 'admin'
        cmd.password = 'password'
        cmd.hostid = self.getHost().id
        return cmd


    def getOobmEnableCmd(self):
        cmd = enableOutOfBandManagementForHost.enableOutOfBandManagementForHostCmd()
        cmd.hostid = self.getHost().id
        return cmd


    def getOobmIssueActionCmd(self):
        cmd = issueOutOfBandManagementPowerAction.issueOutOfBandManagementPowerActionCmd()
        cmd.hostid = self.getHost().id
        cmd.action = 'STATUS'
        return cmd


    def issuePowerActionCmd(self, action):
        cmd = self.getOobmIssueActionCmd()
        cmd.action = action
        return self.apiclient.issueOutOfBandManagementPowerAction(cmd)


    def configureAndEnableOobm(self):
        self.apiclient.configureOutOfBandManagement(self.getOobmConfigCmd())
        response = self.apiclient.enableOutOfBandManagementForHost(self.getOobmEnableCmd())
        self.assertEqual(response.enabled, True)


    def startMgmtServer(self):
        def startMgmtServer(tname, server):
            self.debug("Starting ACS mocked-mgmt server")
            try:
                server.serve_forever()
            except Exception: pass
        server = HTTPServer(('0.0.0.0', self.getServerPort()), MockedCloudStackServer)
        _thread.start_new_thread(startMgmtServer, ("mocked-mgmt-server", server,))
        self.server = server


    def configureAndStartMgmtServer(self):
        """
            Configure mocked-mgmt server and enable out-of-band management for host
        """
        self.configureAndEnableOobm()
        self.startMgmtServer()


    def assertIssueCommandState(self, command, expected):
        """
            Asserts power action result for a given power command
        """

        if command != 'STATUS':
            self.issuePowerActionCmd(command)
        response = self.issuePowerActionCmd('STATUS')
        self.assertEqual(response.powerstate, expected)


    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_oobm_issue_power_status(self):
        """
            Tests out-of-band management issue power action
        """
        self.configureAndStartMgmtServer()
        self.assertIssueCommandState('STATUS', 'On')


    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_oobm_issue_power_on(self):
        """
            Tests out-of-band management issue power on action
        """
        self.configureAndStartMgmtServer()
        self.assertIssueCommandState('ON', 'On')
        global apiRequests
        self.assertTrue('startVirtualMachine' in apiRequests)


    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_oobm_issue_power_off(self):
        """
            Tests out-of-band management issue power off action
        """
        self.configureAndStartMgmtServer()
        self.assertIssueCommandState('OFF', 'Off')
        global apiRequests
        self.assertTrue('stopVirtualMachine' in apiRequests)


    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_oobm_issue_power_cycle(self):
        """
            Tests out-of-band management issue power cycle action
        """
        self.configureAndStartMgmtServer()
        self.assertIssueCommandState('CYCLE', 'On')
        global apiRequests
        self.assertTrue('rebootVirtualMachine' in apiRequests)


    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_oobm_issue_power_reset(self):
        """
            Tests out-of-band management issue power reset action
        """
        self.configureAndStartMgmtServer()
        self.assertIssueCommandState('RESET', 'On')
        global apiRequests
        self.assertTrue('rebootVirtualMachine' in apiRequests)


    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_oobm_issue_power_soft(self):
        """
            Tests out-of-band management issue power soft action
        """
        self.configureAndStartMgmtServer()
        self.assertIssueCommandState('SOFT', 'Off')
        global apiRequests
        self.assertTrue('stopVirtualMachine' in apiRequests)
