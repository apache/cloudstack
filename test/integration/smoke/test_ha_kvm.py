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

import random

from ipmisim.ipmisim import IpmiServerContext, IpmiServer, ThreadedIpmiServer

import random
import socket
import sys
import thread
import time


class TestHAKVM(cloudstackTestCase):
    """ Test cases for host HA using KVM host(s)
    """

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.dbclient = self.testClient.getDbConnection()
        self.services = self.testClient.getParsedTestDataConfig()
        self.hostConfig = self.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__
        self.mgtSvrDetails = self.config.__dict__["mgtSvr"][0].__dict__
        self.fakeMsId = random.randint(10000, 99999) * random.randint(10, 20)

        # Cleanup any existing configs
        self.dbclient.execute("delete from ha_config where resource_type='Host'")
        self.host = self.getHost()

        # use random port for ipmisim
        s = socket.socket()
        s.bind(('', 0))
        self.serverPort = s.getsockname()[1]
        s.close()

        self.cleanup = []

    def getFakeMsId(self):
        return self.fakeMsId

    def getFakeMsRunId(self):
        return self.fakeMsId * 1000

    def tearDown(self):
        try:
            self.dbclient.execute("delete from mshost_peer where peer_runid=%s" % self.getFakeMsRunId())
            self.dbclient.execute("delete from mshost where runid=%s" % self.getFakeMsRunId())
            self.dbclient.execute("delete from cluster_details where name='resourceHAEnabled'")
            self.dbclient.execute("delete from data_center_details where name='resourceHAEnabled'")
            self.dbclient.execute("delete from ha_config where resource_type='Host'")
            self.dbclient.execute("delete from oobm where port=%d" % self.getIpmiServerPort())
            self.dbclient.execute("delete from mshost_peer where peer_runid=%s" % self.getFakeMsRunId())
            self.dbclient.execute("delete from mshost where runid=%s" % self.getFakeMsRunId())
            self.dbclient.execute("delete from cluster_details where name='outOfBandManagementEnabled'")
            self.dbclient.execute("delete from data_center_details where name='outOfBandManagementEnabled'")
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def getHostHaEnableCmd(self):
        cmd = enableHAForHost.enableHAForHostCmd()
        cmd.hostid = self.getHost().id
        return cmd

    def check_host_transition_to_available(self):
        t_end = time.time() + 90
        while time.time() < t_end:
            host = self.getHost()
            if host.hostha.hastate == "Available":
                return
            else:
                continue
        self.fail(self)

    def getHost(self):
        response = list_hosts(
            self.apiclient,
            type='Routing',
            resourcestate='Enabled'
        )
        if response and len(response) > 0:
            self.host = response[0]
            return self.host
        raise self.skipTest("No KVM hosts found, skipping host-ha test")

    def getHost(self, hostId=None):

        response = list_hosts(
            self.apiclient,
            type='Routing',
            id=hostId
        )
        if response and len(response) > 0:
            self.host = response[0]
            return self.host
        raise self.skipTest("No KVM hosts found, skipping host-ha test")

    def getHostHaConfigCmd(self, provider='kvmhaprovider'):
        cmd = configureHAForHost.configureHAForHostCmd()
        cmd.provider = provider
        cmd.hostid = self.getHost().id
        return cmd

    def getHostHaEnableCmd(self):
        cmd = enableHAForHost.enableHAForHostCmd()
        cmd.hostid = self.getHost().id
        return cmd

    def getHostHaDisableCmd(self):
        cmd = disableHAForHost.disableHAForHostCmd()
        cmd.hostid = self.getHost().id
        return cmd

    def configureAndEnableHostHa(self, initialize=True):
        self.apiclient.configureHAForHost(self.getHostHaConfigCmd())
        response = self.apiclient.enableHAForHost(self.getHostHaEnableCmd())
        self.assertEqual(response.haenable, True)
        if initialize:
            self.configureKVMHAProviderState(True, True, True, False)

    def configureAndDisableHostHa(self, hostId):
        self.apiclient.configureHAForHost(self.getHostHaConfigCmd())
        cmd = self.getHostHaDisableCmd()
        cmd.hostid = hostId
        response = self.apiclient.disableHAForHost(cmd)
        self.assertEqual(response.hostid, cmd.hostid)
        self.assertEqual(response.haenable, False)

    def enableHostHa(self, hostId):
        cmd = self.getHostHaEnableCmd()
        cmd.hostid = hostId
        response = self.apiclient.enableHAForHost(cmd)
        self.assertEqual(response.hostid, cmd.hostid)
        self.assertEqual(response.haenable, True)
        return response

    def configureKVMHAProviderState(self, health, activity, recover, fence):
        cmd = configureHAForHost.configureHAForHostCmd()
        cmd.hostid = self.getHost().id
        cmd.health = health
        cmd.activity = activity
        cmd.recover = recover
        cmd.fence = fence
        response = self.apiclient.configureKVMHAProviderState(cmd)
        self.assertEqual(response.success, 'true')

    def checkSyncToState(self, state, interval=5000):
        def checkForStateSync(expectedState):
            response = self.getHost(hostId=self.getHost().id).hostha
            return response.hastate == expectedState, None

        sync_interval = 1 + int(interval) / 1000
        res, _ = wait_until(sync_interval, 10, checkForStateSync, state)
        if not res:
            self.fail("Failed to get host.hastate synced to expected state:" + state)
        response = self.getHost(hostId=self.getHost().id).hostha
        self.assertEqual(response.hastate, state)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_hostha_configure_invalid_provider(self):
            """
                Tests host-ha configuration with invalid driver
            """
            cmd = self.getHostHaConfigCmd()
            cmd.provider = 'randomDriverThatDoesNotExist'
            try:
                response = self.apiclient.configureHAForHost(cmd)
            except Exception:
                pass
            else:
                self.fail("Expected an exception to be thrown, failing")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_hostha_configure_default_driver(self):
        """
           Tests host-ha configuration with valid data
        """
        cmd = self.getHostHaConfigCmd()
        response = self.apiclient.configureHAForHost(cmd)
        self.assertEqual(response.hostid, cmd.hostid)
        self.assertEqual(response.haprovider, cmd.provider.lower())

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_ha_enable_feature_invalid(self):
            """
                Tests ha feature enable command with invalid options
            """
            cmd = self.getHostHaEnableCmd()
            cmd.hostid = -1
            try:
                response = self.apiclient.enableHAForHost(cmd)
            except Exception:
                pass
            else:
                self.fail("Expected an exception to be thrown, failing")

            try:
                cmd = enableHAForCluster.enableHAForClusterCmd()
                response = self.apiclient.enableHAForCluster(cmd)
            except Exception:
                pass
            else:
                self.fail("Expected an exception to be thrown, failing")

            try:
                cmd = enableHAForZone.enableHAForZoneCmd()
                response = self.apiclient.enableHAForZone(cmd)
            except Exception:
                pass
            else:
                self.fail("Expected an exception to be thrown, failing")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_ha_disable_feature_invalid(self):
        """
            Tests ha feature disable command with invalid options
        """
        cmd = self.getHostHaDisableCmd()
        cmd.hostid = -1
        try:
            response = self.apiclient.disableHAForHost(cmd)
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")

        try:
            cmd = disableHAForCluster.disableHAForClusterCmd()
            response = self.apiclient.disableHAForCluster(cmd)
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")

        try:
            cmd = disableHAForZone.disableHAForZoneCmd()
            response = self.apiclient.disableHAForZone(cmd)
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_hostha_enable_feature_valid(self):
        """
            Tests host-ha enable feature with valid options
        """
        self.apiclient.configureHAForHost(self.getHostHaConfigCmd())
        cmd = self.getHostHaEnableCmd()
        response = self.apiclient.enableHAForHost(cmd)
        self.assertEqual(response.hostid, cmd.hostid)
        self.assertEqual(response.haenable, True)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_hostha_disable_feature_valid(self):
        """
            Tests host-ha disable feature with valid options
        """
        self.apiclient.configureHAForHost(self.getHostHaConfigCmd())
        cmd = self.getHostHaDisableCmd()
        response = self.apiclient.disableHAForHost(cmd)
        self.assertEqual(response.hostid, cmd.hostid)
        self.assertEqual(response.haenable, False)

        response = self.getHost(hostId=cmd.hostid).hostha
        self.assertEqual(response.hastate, 'Disabled')

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_configure_ha_provider_invalid(self):
        """
            Tests configure HA Provider with invalid provider options
        """

        # Enable ha for host
        self.apiclient.configureHAForHost(self.getHostHaConfigCmd())
        cmd = self.getHostHaEnableCmd()
        response = self.apiclient.enableHAForHost(cmd)
        self.assertEqual(response.hostid, cmd.hostid)
        self.assertEqual(response.haenable, True)

        host = self.getHost(response.hostid)

        # Setup wrong configuration for the host
        conf_ha_cmd = configureHAForHost.configureHAForHostCmd()
        if host.hypervisor.lower() in "simulator":
            conf_ha_cmd.provider = "kvmhaprovider"
        if host.hypervisor.lower() in "kvm":
            conf_ha_cmd.provider = "simulatorhaprovider"

        conf_ha_cmd.hostid = cmd.hostid

        # Call the configure HA provider API with not supported provider for HA
        try:
            self.apiclient.configureHAForHost(conf_ha_cmd)
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_configure_ha_provider_valid(self):
        """
            Tests configure HA Provider with valid provider options
        """

        # Enable ha for host
        self.apiclient.configureHAForHost(self.getHostHaConfigCmd())
        cmd = self.getHostHaEnableCmd()
        response = self.apiclient.enableHAForHost(cmd)
        self.assertEqual(response.hostid, cmd.hostid)
        self.assertEqual(response.haenable, True)

        host = self.getHost(response.hostid)

        # Setup configuration for the host
        conf_ha_cmd = configureHAForHost.configureHAForHostCmd()
        if host.hypervisor.lower() in "kvm":
            conf_ha_cmd.provider = "kvmhaprovider"
        if host.hypervisor.lower() in "simulator":
            conf_ha_cmd.provider = "simulatorhaprovider"

        conf_ha_cmd.hostid = cmd.hostid

        # Call the configure HA provider API with not supported provider for HA
        response = self.apiclient.configureHAForHost(conf_ha_cmd)

        # Check the response contains the set provider and hostID
        self.assertEqual(response.haprovider, conf_ha_cmd.provider)
        self.assertEqual(response.hostid, conf_ha_cmd.hostid)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_disable_oobm_ha_state_ineligible(self):
        """
            Tests that when HA is enabled for a host, if oobm is disabled HA State should turn into Ineligible
        """

        # Enable ha for host
        self.apiclient.configureHAForHost(self.getHostHaConfigCmd())
        cmd = self.getHostHaEnableCmd()
        response = self.apiclient.enableHAForHost(cmd)
        self.assertEqual(response.hostid, cmd.hostid)
        self.assertEqual(response.haenable, True)

        # Disable OOBM
        self.apiclient.configureOutOfBandManagement(self.getOobmConfigCmd())
        oobm_cmd = self.getOobmDisableCmd()
        oobm_cmd.hostid = cmd.hostid
        response = self.apiclient.disableOutOfBandManagementForHost(oobm_cmd)
        self.assertEqual(response.hostid, oobm_cmd.hostid)
        self.assertEqual(response.enabled, False)

        response = self.getHost(hostId=cmd.hostid).outofbandmanagement
        self.assertEqual(response.powerstate, 'Disabled')

        # Verify HA State is Ineligeble
        response = self.getHost(hostId=cmd.hostid).hostha
        self.assertEqual(response.hastate, "Ineligible")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_hostha_configure_default_driver(self):
        """
            Tests host-ha configuration with valid data
        """
        cmd = self.getHostHaConfigCmd()
        response = self.apiclient.configureHAForHost(cmd)
        self.assertEqual(response.hostid, cmd.hostid)
        self.assertEqual(response.haprovider, cmd.provider.lower())

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_enable_ha_when_host_powerstate_on(self):
        """
            Tests that when HA is enabled for a host, if oobm state is on HA State should turn into Available
        """

        self.configureAndStartIpmiServer()

        self.assertIssueCommandState('ON', 'On')

        self.apiclient.configureHAForHost(self.getHostHaConfigCmd())
        cmd = self.getHostHaEnableCmd()
        response = self.apiclient.enableHAForHost(cmd)
        self.assertEqual(response.hostid, cmd.hostid)
        self.assertEqual(response.haenable, True)

        # Verify HA State is Available
        self.check_host_transition_to_available()

        response = self.getHost()
        if response.hostha.hastate is not "Available":
            print response

        self.assertEqual(response.hostha.hastate, "Available")

        self.stopIpmiServer()

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_hostha_enable_feature_without_setting_provider(self):
        """
            Tests Enable HA without setting the provider, Exception is thrown
        """
        host = self.get_non_configured_ha_host()
        cmd = self.getHostHaEnableCmd()
        cmd.hostid = host.id

        try:
            self.apiclient.enableHAForHost(cmd)
        except Exception as e:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="treu")
    def test_hostha_enable_ha_when_host_disabled(self):
        """
            Tests Enable HA when host is disconnected, should be Ineligible
        """
        # Enable HA
        self.apiclient.configureHAForHost(self.getHostHaConfigCmd())
        cmd = self.getHostHaEnableCmd()
        cmd.hostid = self.host.id
        enable = self.apiclient.enableHAForHost(cmd)
        self.assertEqual(enable.hostid, cmd.hostid)
        self.assertEqual(enable.haenable, True)

        # Disable Host
        self.disableHost(self.host.id)

        # Check HA State
        try:
            response = self.getHost(self.host.id)
            self.assertEqual(response.hostha.hastate, "Ineligible")
        except Exception as e:
            self.enableHost(self.host.id)
            self.fail(e)

        # Enable Host
        self.enableHost(self.host.id)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_hostha_enable_ha_when_host_inMaintenance(self):
        """
            Tests Enable HA when host is in Maintenance mode, should be Ineligible
        """

        host = self.getHost()

        # Enable HA
        self.apiclient.configureHAForHost(self.getHostHaConfigCmd())
        cmd = self.getHostHaEnableCmd()
        cmd.hostid = host.id
        enable = self.apiclient.enableHAForHost(cmd)
        self.assertEqual(enable.hostid, cmd.hostid)
        self.assertEqual(enable.haenable, True)

        # Prepare for maintenance Host
        self.setHostToMaintanance(host.id)

        # Check HA State
        try:
            response = self.getHost(host.id)
            self.assertEqual(response.hostha.hastate, "Ineligible")
        except Exception as e:
            self.cancelMaintenance(host.id)
            self.fail(e)

        # Enable Host
        self.cancelMaintenance(host.id)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_hostha_enable_ha_when_host_disconected(self):
        """
            Tests Enable HA when host is disconnected, should be Ineligible
        """
        host = self.getHost()

        # Enable HA
        self.apiclient.configureHAForHost(self.getHostHaConfigCmd())
        cmd = self.getHostHaEnableCmd()
        cmd.hostid = host.id
        enable = self.apiclient.enableHAForHost(cmd)
        self.assertEqual(enable.hostid, cmd.hostid)
        self.assertEqual(enable.haenable, True)

        # Make Host Disconnected
        self.killAgent()

        # Check HA State
        try:
            time.sleep(1)
            response = self.getHost(self.host.id)
            self.assertEqual(response.hostha.hastate, "Ineligible")
        except Exception as e:
            self.startAgent()
            self.fail(e)

        # Enable Host
        self.startAgent()

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_remove_ha_provider_not_possible(self):
        """
            Tests HA Provider should be possible to be removed when HA is enabled
        """

        host = self.getHost()

        # Enable HA
        self.apiclient.configureHAForHost(self.getHostHaConfigCmd())
        cmd = self.getHostHaEnableCmd()
        cmd.hostid = host.id
        enable = self.apiclient.enableHAForHost(cmd)
        self.assertEqual(enable.hostid, cmd.hostid)
        self.assertEqual(enable.haenable, True)

        try:
            self.apiclient.configureHAForHost(self.getHostHaConfigCmd(''))
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")

    def configureAndStartIpmiServer(self, power_state=None):
        """
            Setup ipmisim and enable out-of-band management for host
        """
        self.configureAndEnableOobm()
        self.startIpmiServer()
        if power_state:
            bmc = IpmiServerContext().bmc
            bmc.powerstate = power_state

    def assertIssueCommandState(self, command, expected):
        """
            Asserts power action result for a given power command
        """
        if command != 'STATUS':
            self.issuePowerActionCmd(command)
        response = self.issuePowerActionCmd('STATUS')
        self.assertEqual(response.powerstate, expected)

    def configureAndEnableOobm(self):
        self.apiclient.configureOutOfBandManagement(self.getOobmConfigCmd())
        response = self.apiclient.enableOutOfBandManagementForHost(self.getOobmEnableCmd())
        self.assertEqual(response.enabled, True)

    def startIpmiServer(self):
        def startIpmiServer(tname, server):
            self.debug("Starting ipmisim server")
            try:
                server.serve_forever()
            except Exception: pass
        IpmiServerContext('reset')
        ThreadedIpmiServer.allow_reuse_address = False
        server = ThreadedIpmiServer(('0.0.0.0', self.getIpmiServerPort()), IpmiServer)
        thread.start_new_thread(startIpmiServer, ("ipmi-server", server,))
        self.server = server

    def stopIpmiServer(self):
        if self.server:
            self.server.shutdown()
            self.server.server_close()

    def getOobmIssueActionCmd(self):
        cmd = issueOutOfBandManagementPowerAction.issueOutOfBandManagementPowerActionCmd()
        cmd.hostid = self.getHost().id
        cmd.action = 'STATUS'
        return cmd

    def issuePowerActionCmd(self, action, timeout=None):
        cmd = self.getOobmIssueActionCmd()
        cmd.action = action
        if timeout:
            cmd.timeout = timeout

        try:
            return self.apiclient.issueOutOfBandManagementPowerAction(cmd)
        except Exception as e:
            if "packet session id 0x0 does not match active session" in str(e):
                raise self.skipTest("Known ipmitool issue hit, skipping test")
            raise e

    def getOobmEnableCmd(self):
        cmd = enableOutOfBandManagementForHost.enableOutOfBandManagementForHostCmd()
        cmd.hostid = self.getHost().id
        return cmd

    def getOobmDisableCmd(self):
        cmd = disableOutOfBandManagementForHost.disableOutOfBandManagementForHostCmd()
        cmd.hostid = self.getHost().id
        return cmd

    def getIpmiServerPort(self):
        return self.serverPort

    def getOobmConfigCmd(self):
        cmd = configureOutOfBandManagement.configureOutOfBandManagementCmd()
        cmd.driver = 'ipmitool' # The default available driver
        cmd.address = self.getIpmiServerIp()
        cmd.port = self.getIpmiServerPort()
        cmd.username = 'admin'
        cmd.password = 'password'
        cmd.hostid = self.getHost().id
        return cmd

    def getIpmiServerIp(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect((self.mgtSvrDetails["mgtSvrIp"], self.mgtSvrDetails["port"]))
        return s.getsockname()[0]

    def get_non_configured_ha_host(self):

        response = list_hosts(
            self.apiclient,
            type='Routing'
        )

        for host in response:
            if host.haprovider is None:
                return host
            else:
                cloudstackTestCase.skipTest(self, "There is no non configured hosts. Skipping test.")

        raise self.skipTest("No KVM hosts found, skipping host-ha test")

    def getHAState(self, id):
        cmd = listHostHAResources.listHostHAResourcesCmd()
        cmd.hostid = id
        response = self.apiclient.listHostHAResources(cmd)

        return response[0]

    def startAgent(self):
        host = self.getHost()
        SshClient(host=host.ipaddress, port=22, user=self.hostConfig["username"],
                  passwd=self.hostConfig["password"]).execute \
            ("service cloudstack-agent start")

    def disableHost(self, id):

        cmd = updateHost.updateHostCmd()
        cmd.id = id
        cmd.allocationstate = "Disable"

        response = self.apiclient.updateHost(cmd)

        self.assertEqual(response.resourcestate, "Disabled")

    def enableHost(self, id):
        cmd = updateHost.updateHostCmd()
        cmd.id = id
        cmd.allocationstate = "Enable"

        response = self.apiclient.updateHost(cmd)

        self.assertEqual(response.resourcestate, "Enabled")

    def setHostToMaintanance(self, id):
        cmd = prepareHostForMaintenance.prepareHostForMaintenanceCmd()
        cmd.id = id

        response = self.apiclient.prepareHostForMaintenance(cmd)

        self.assertEqual(response.resourcestate, "PrepareForMaintenance")

    def cancelMaintenance(self, id):
        cmd = cancelHostMaintenance.cancelHostMaintenanceCmd()
        cmd.id = id

        response = self.apiclient.cancelHostMaintenance(cmd)

        self.assertEqual(response.resourcestate, "Enabled")

    def killAgent(self):
        host = self.getHost()
        SshClient(host=host.ipaddress, port=22, user=self.hostConfig["username"], passwd=self.hostConfig["password"]).execute\
            ("kill $(ps aux | grep 'cloudstack-agent' | awk '{print $2}')")
