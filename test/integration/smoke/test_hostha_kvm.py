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
import _thread
import time


class TestHAKVM(cloudstackTestCase):
    """ Test cases for host HA using KVM host(s)
    """

    def setUp(self):
        self.testClient = super(TestHAKVM, self).getClsTestClient()
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.services = self.testClient.getParsedTestDataConfig()
        self.logger = logging.getLogger('TestHAKVM')

        #Get Zone specifics
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.host = self.getHost()
        self.hostConfig = self.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__
        self.mgtSvrDetails = self.config.__dict__["mgtSvr"][0].__dict__
        self.cluster_id = self.host.clusterid

        # Cleanup any existing configs
        self.dbclient.execute("delete from ha_config where resource_type='Host'")

        # use random port for ipmisim
        self.fakeMsId = random.randint(10000, 99999) * random.randint(10, 20)
        s = socket.socket()
        s.bind(('', 0))
        self.serverPort = s.getsockname()[1]
        s.close()

        # Set Cluster-level setting in order to run tests faster
        self.updateConfiguration("kvm.ha.activity.check.failure.ratio", "0.6")
        self.updateConfiguration("kvm.ha.activity.check.interval", "8")
        self.updateConfiguration("kvm.ha.activity.check.max.attempts", "5")
        self.updateConfiguration("kvm.ha.activity.check.timeout", "30")
        self.updateConfiguration("kvm.ha.degraded.max.period", "30")
        self.updateConfiguration("kvm.ha.fence.timeout", "30")
        self.updateConfiguration("kvm.ha.health.check.timeout", "30")
        self.updateConfiguration("kvm.ha.recover.failure.threshold", "2")
        self.updateConfiguration("kvm.ha.recover.timeout", "30")
        self.updateConfiguration("kvm.ha.recover.wait.period", "30")

        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["hasmall"]
        )

        self.template = get_test_template(
            self.apiclient,
            self.zone.id,
            self.hypervisor
        )

        self.configureAndDisableHostHa()
        self.cleanup = [self.service_offering]

    def updateConfiguration(self, name, value):
        cmd = updateConfiguration.updateConfigurationCmd()
        cmd.name = name
        cmd.value = value
        cmd.clusterid = self.cluster_id
        self.apiclient.updateConfiguration(cmd)

    def getFakeMsId(self):
        return self.fakeMsId

    def getFakeMsRunId(self):
        return self.fakeMsId * 1000

    def tearDown(self):
        self.configureAndDisableHostHa()
        self.host = None
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
        cmd.hostid = self.host.id
        return cmd

    def getHost(self, hostId=None):
        response = list_hosts(
            self.apiclient,
            type='Routing',
            hypervisor='kvm',
            id=hostId
        )
        # Check if more than one kvm hosts are available in order to successfully configure host-ha
        if response and len(response) > 0:
            self.host = response[0]
            return self.host
        raise self.skipTest("Not enough KVM hosts found, skipping host-ha test")

    def getHostHaConfigCmd(self, provider='kvmhaprovider'):
        cmd = configureHAForHost.configureHAForHostCmd()
        cmd.provider = provider
        cmd.hostid = self.host.id
        return cmd

    def getHostHaDisableCmd(self):
        cmd = disableHAForHost.disableHAForHostCmd()
        cmd.hostid = self.host.id
        return cmd

    def configureAndEnableHostHa(self):
        #Adding sleep between configuring and enabling
        self.apiclient.configureHAForHost(self.getHostHaConfigCmd())
        response = self.apiclient.enableHAForHost(self.getHostHaEnableCmd())
        self.assertEqual(response.haenable, True)

    def configureAndDisableHostHa(self):
        self.apiclient.configureHAForHost(self.getHostHaConfigCmd())
        cmd = self.getHostHaDisableCmd()
        cmd.hostid = self.host.id
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

    def disableAgent(self):
        SshClient(self.host.ipaddress, port=22, user=self.hostConfig["username"], passwd=self.hostConfig["password"]).execute\
            ("systemctl disable cloudstack-agent || chkconfig cloudstack-agent off")

    def resetHost(self):
        SshClient(self.host.ipaddress, port=22, user=self.hostConfig["username"],
                  passwd=self.hostConfig["password"]).execute \
            ("reboot")

    def enableAgent(self):
        SshClient(self.host.ipaddress, port=22, user=self.hostConfig["username"], passwd=self.hostConfig["password"]).execute\
            ("systemctl enable cloudstack-agent || chkconfig cloudstack-agent on")

    def waitUntilHostInState(self, state="Available", interval=3):
        def checkForState(expectedState):
            response = self.getHost(self.host.id)
            print("checkForState:: expected=%s, actual=%s" % (state, response.hostha))
            return response.hostha.hastate == expectedState, None

        res, _ = wait_until(interval, 200, checkForState, state)
        if not res:
            self.fail("Failed to see host ha state in :" + state)

    def deployVM(self):
        try:
            vm = VirtualMachine.create(
                self.apiclient,
                services=self.services["virtual_machine"],
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                zoneid=self.zone.id,
                hostid = self.host.id,
                method="POST"
            )
            self.cleanup.append(vm)
        except Exception as e:
            raise self.skipTest("Failed to deploy VM, skipping kvm host-ha test case")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_disable_oobm_ha_state_ineligible(self):
        """
            Tests that when HA is enabled for a host, if oobm is disabled HA State should turn into Ineligible
        """
        self.logger.debug("Starting test_disable_oobm_ha_state_ineligible")

        # Enable ha for host
        self.configureAndEnableHostHa()

        # Disable OOBM
        self.apiclient.configureOutOfBandManagement(self.getOobmConfigCmd())
        oobm_cmd = self.getOobmDisableCmd()
        oobm_cmd.hostid = self.host.id
        response = self.apiclient.disableOutOfBandManagementForHost(oobm_cmd)
        self.assertEqual(response.hostid, oobm_cmd.hostid)
        self.assertEqual(response.enabled, False)

        response = self.getHost(hostId=self.host.id).outofbandmanagement
        self.assertEqual(response.powerstate, 'Disabled')

        # Verify HA State is Ineligeble
        self.waitUntilHostInState("Ineligible")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_hostha_configure_default_driver(self):
        """
            Tests host-ha configuration with valid data
        """
        self.logger.debug("Starting test_hostha_configure_default_driver")

        cmd = self.getHostHaConfigCmd()
        response = self.apiclient.configureHAForHost(cmd)
        self.assertEqual(response.hostid, cmd.hostid)
        self.assertEqual(response.haprovider, cmd.provider.lower())

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_hostha_enable_ha_when_host_disabled(self):
        """
            Tests Enable HA when host is disconnected, should be Ineligible
        """
        self.logger.debug("Starting test_hostha_enable_ha_when_host_disabled")

        # Enable HA
        self.configureAndEnableHostHa()

        # Disable Host
        self.disableHost(self.host.id)

        # Check HA State
        try:
            self.waitUntilHostInState("Ineligible")
        except Exception as e:
            self.enableHost(self.host.id)
            self.fail(e)

        # Enable Host
        self.enableHost(self.host.id)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_hostha_enable_ha_when_host_in_maintenance(self):
        """
            Tests Enable HA when host is in Maintenance mode, should be Ineligible
        """
        self.logger.debug("Starting test_hostha_enable_ha_when_host_in_maintenance")
        self.logger.debug("Pausing to wait for VMs to have finished starting")
        time.sleep(300)
        
        # Enable HA
        self.configureAndEnableHostHa()
        

        # Prepare for maintenance Host
        self.setHostToMaintanance(self.host.id)

        # Check HA State
        try:
            self.waitUntilHostInState("Ineligible")
        except Exception as e:
            self.cancelMaintenance()
            self.fail(e)

        # Enable Host
        self.cancelMaintenance()

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_hostha_enable_ha_when_host_disconected(self):
        """
            Tests Enable HA when host is disconnected, should be Ineligible
        """
        self.logger.debug("Starting test_hostha_enable_ha_when_host_disconected")

        # Enable HA
        self.apiclient.configureHAForHost(self.getHostHaConfigCmd())
        cmd = self.getHostHaEnableCmd()
        cmd.hostid = self.host.id
        enable = self.apiclient.enableHAForHost(cmd)
        self.assertEqual(enable.hostid, cmd.hostid)
        self.assertEqual(enable.haenable, True)

        # Make Host Disconnected
        self.killAgent()

        # Check HA State
        try:
            self.waitUntilHostInState("Ineligible")
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
        self.logger.debug("Starting test_remove_ha_provider_not_possible")


        # Enable HA
        self.apiclient.configureHAForHost(self.getHostHaConfigCmd())
        cmd = self.getHostHaEnableCmd()
        cmd.hostid = self.host.id
        enable = self.apiclient.enableHAForHost(cmd)
        self.assertEqual(enable.hostid, cmd.hostid)
        self.assertEqual(enable.haenable, True)

        try:
            self.apiclient.configureHAForHost(self.getHostHaConfigCmd(''))
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")

    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_hostha_kvm_host_degraded(self):
        """
            Tests degraded HA state when agent is stopped/killed
        """

        self.configureAndStartIpmiServer()
        self.assertIssueCommandState('ON', 'On')
        self.configureAndEnableHostHa()

        self.deployVM()

        # Start with the available state
        self.waitUntilHostInState("Available")

        # SSH into the KVM Host and executes kill -9 of the agent
        self.stopAgent()

        # Check if host would go into Suspect state
        try:
            self.waitUntilHostInState("Suspect")
        except Exception as e:
            self.startAgent()
            raise Exception("Warning: Exception during test execution : %s" % e)

        # Checks if the host would turn into Degraded
        try:
            self.waitUntilHostInState("Degraded")
        except Exception as e:
            self.startAgent()
            raise Exception("Warning: Exception during test execution : %s" % e)

        self.startAgent()
        self.waitUntilHostInState("Available")


    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_hostha_kvm_host_recovering(self):
        """
            Tests recovery and fencing HA state transitions
        """

        self.configureAndStartIpmiServer()
        self.assertIssueCommandState('ON', 'On')
        self.configureAndEnableHostHa()

        self.deployVM()

        # Start with the available state
        self.waitUntilHostInState("Available")

        # Kill host by triggering a fault
        self.killAgent()
        self.disableAgent()
        self.resetHost()

        # Check if host would go into Suspect state
        try:
            self.waitUntilHostInState("Suspect")
        except Exception as e:
            self.startAgent()
            raise Exception("Warning: Exception during test execution : %s" % e)

        # Checks if the host would turn into Recovered
        try:
            self.waitUntilHostInState("Recovered")
        except Exception as e:
            self.startAgent()
            raise Exception("Warning: Exception during test execution : %s" % e)

        self.enableAgent()
        self.startAgent()
        self.waitUntilHostInState("Available")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_hostha_kvm_host_fencing(self):
        """
            Tests fencing/fenced HA state when host crashes
        """
        self.logger.debug("Starting test_ha_kvm_host_fencing")


        self.configureAndStartIpmiServer()
        self.assertIssueCommandState('ON', 'On')
        self.configureAndEnableHostHa()

        self.deployVM()

        # Start with the available state
        self.waitUntilHostInState("Available")

        # Fail oobm commands
        cmd = self.getOobmConfigCmd()
        cmd.address = "1.1.1.1"
        self.apiclient.configureOutOfBandManagement(cmd)

        # Kill host by triggering a fault
        self.killAgent()
        self.disableAgent()
        self.resetHost()

        # Check if host would go into Suspect state
        try:
            self.waitUntilHostInState("Suspect")
        except Exception as e:
            self.startAgent()
            raise Exception("Warning: Exception during test execution : %s" % e)

        # Checks if the host would turn into Fencing
        try:
            self.waitUntilHostInState("Fencing")
        except Exception as e:
            self.startAgent()
            raise Exception("Warning: Exception during test execution : %s" % e)

        # Allow oobm commands to work now
        self.configureAndEnableOobm()

        # Checks if the host would turn into Fenced
        try:
            self.waitUntilHostInState("Fenced")
        except Exception as e:
            self.startAgent()
            raise Exception("Warning: Exception during test execution : %s" % e)

        self.enableAgent()
        self.startAgent()
        self.cancelMaintenance()
        self.waitUntilHostInState("Available")

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
        try:
            response = self.issuePowerActionCmd('STATUS')
            self.assertEqual(response.powerstate, expected)
        except:
            pass  # in case of ipmisim errors ignore

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
        _thread.start_new_thread(startIpmiServer, ("ipmi-server", server,))
        self.server = server

    def stopIpmiServer(self):
        if self.server:
            self.server.shutdown()
            self.server.server_close()

    def getOobmIssueActionCmd(self):
        cmd = issueOutOfBandManagementPowerAction.issueOutOfBandManagementPowerActionCmd()
        cmd.hostid = self.host.id
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
        cmd.hostid = self.host.id
        return cmd

    def getOobmDisableCmd(self):
        cmd = disableOutOfBandManagementForHost.disableOutOfBandManagementForHostCmd()
        cmd.hostid = self.host.id
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
        cmd.hostid = self.host.id
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
        SshClient(host=self.host.ipaddress, port=22, user=self.hostConfig["username"],
                  passwd=self.hostConfig["password"]).execute \
            ("systemctl start cloudstack-agent || service cloudstack-agent start")

    def stopAgent(self):
        SshClient(host=self.host.ipaddress, port=22, user=self.hostConfig["username"],
                  passwd=self.hostConfig["password"]).execute \
            ("systemctl stop cloudstack-agent || service cloudstack-agent stop")

    def killAgent(self):
        SshClient(host=self.host.ipaddress, port=22, user=self.hostConfig["username"], passwd=self.hostConfig["password"]).execute\
            ("kill -9 $(ps aux | grep 'cloudstack-agent' | awk '{print $2}')")

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

    def cancelMaintenance(self):
        cmd = cancelHostMaintenance.cancelHostMaintenanceCmd()
        cmd.id = self.host.id
        response = self.apiclient.cancelHostMaintenance(cmd)

        self.assertEqual(response.resourcestate, "Enabled")
