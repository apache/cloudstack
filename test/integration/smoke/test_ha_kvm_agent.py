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


from marvin.cloudstackTestCase import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr

from ipmisim.ipmisim import IpmiServerContext, IpmiServer, ThreadedIpmiServer

import random
import socket
import thread


class TestHaKVMAgent(cloudstackTestCase):
    """ Test cases for out of band management
    """

    def setUp(self):
        testClient = super(TestHaKVMAgent, self).getClsTestClient()

        self.apiClient = testClient.getApiClient()
        self.dbclient = testClient.getDbConnection()
        self.services = testClient.getParsedTestDataConfig()

        self.zone = get_zone(self.apiClient, testClient.getZoneForTests())
        self.host = self.getHost()
        self.cluster_id = self.host.clusterid
        self.server = None

        self.hypervisor = self.testClient.getHypervisorInfo()
        self.mgtSvrDetails = self.config.__dict__["mgtSvr"][0].__dict__
        self.hostConfig = self.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__
        self.fakeMsId = random.randint(10000, 99999) * random.randint(10, 20)

        # Cleanup any existing configs
        self.dbclient.execute("delete from ha_config where resource_type='Host'")

        # use random port for ipmisim
        s = socket.socket()
        s.bind(('', 0))
        self.serverPort = s.getsockname()[1]
        s.close()

        # Set Cluster-level setting in order to run tests faster
        self.update_configuration("kvm.ha.activity.check.failure.ratio", "0.7")
        self.update_configuration("kvm.ha.activity.check.interval", "10")
        self.update_configuration("kvm.ha.activity.check.max.attempts", "5")
        self.update_configuration("kvm.ha.activity.check.timeout", "60")
        self.update_configuration("kvm.ha.degraded.max.period", "30")
        self.update_configuration("kvm.ha.fence.timeout", "60")
        self.update_configuration("kvm.ha.health.check.timeout", "10")
        self.update_configuration("kvm.ha.recover.failure.threshold", "1")
        self.update_configuration("kvm.ha.recover.timeout", "120")
        self.update_configuration("kvm.ha.recover.wait.period", "60")

        self.service_offering = ServiceOffering.create(
            self.apiClient,
            self.services["service_offerings"]
        )

        self.template = get_template(
            self.apiClient,
            self.zone.id,
            self.services["ostype"]
        )

        self.cleanup = [self.service_offering]

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
            cleanup_resources(self.apiClient, self.cleanup)
            if self.server:
                self.server.shutdown()
                self.server.server_close()
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def getFakeMsId(self):
        return self.fakeMsId

    def getFakeMsRunId(self):
        return self.fakeMsId * 1000

    def getHostHaConfigCmd(self, provider='kvmhaprovider'):
        cmd = configureHAForHost.configureHAForHostCmd()
        cmd.provider = provider
        cmd.hostid = self.host.id
        return cmd

    def getHostHaEnableCmd(self):
        cmd = enableHAForHost.enableHAForHostCmd()
        cmd.hostid = self.host.id
        return cmd

    def getHost(self, hostId=None):
        response = list_hosts(
                        self.apiClient,
                        zoneid=self.zone.id,
                        type='Routing',
                        id=hostId
                        )
        if len(response) > 0:
            self.host = response[0]
            return self.host
        raise self.skipTest("No hosts found, skipping out-of-band management test")

    def getIpmiServerIp(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect((self.mgtSvrDetails["mgtSvrIp"], self.mgtSvrDetails["port"]))
        return s.getsockname()[0]

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

    def getOobmEnableCmd(self):
        cmd = enableOutOfBandManagementForHost.enableOutOfBandManagementForHostCmd()
        cmd.hostid = self.host.id
        return cmd

    def getOobmDisableCmd(self):
        cmd = disableOutOfBandManagementForHost.disableOutOfBandManagementForHostCmd()
        cmd.hostid = self.host.id
        return cmd

    def getOobmIssueActionCmd(self):
        cmd = issueOutOfBandManagementPowerAction.issueOutOfBandManagementPowerActionCmd()
        cmd.hostid = self.host.id
        cmd.action = 'STATUS'
        return cmd

    def issue_power_action_cmd(self, action, timeout=None):
        cmd = self.getOobmIssueActionCmd()
        cmd.action = action
        if timeout:
            cmd.timeout = timeout

        try:
            return self.apiClient.issueOutOfBandManagementPowerAction(cmd)
        except Exception as e:
            if "packet session id 0x0 does not match active session" in str(e):
                raise self.skipTest("Known ipmitool issue hit, skipping test")
            raise e

    def configure_and_enable_oobm(self):
        self.apiClient.configureOutOfBandManagement(self.getOobmConfigCmd())
        response = self.apiClient.enableOutOfBandManagementForHost(self.getOobmEnableCmd())
        self.assertEqual(response.enabled, True)

    def start_ipmi_server(self):
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

    def checkSyncToState(self, state, interval):
        def checkForStateSync(expectedState):
            response = self.getHost(hostId=self.host.id).outofbandmanagement
            return response.powerstate == expectedState, None

        sync_interval = 1 + int(interval)/1000
        res, _ = wait_until(sync_interval, 10, checkForStateSync, state)
        if not res:
            self.fail("Failed to get host.powerstate synced to expected state:" + state)
        response = self.getHost(hostId=self.host.id).outofbandmanagement
        self.assertEqual(response.powerstate, state)

    def get_host_in_available_state(self):

        self.configure_and_start_ipmi_server()
        self.assert_issue_command_state('ON', 'On')
        self.configureAndEnableHostHa()

        self.check_host_transition_to_available()

        response = self.getHost()
        if response.hostha.hastate is not "Available":
            print response

        self.assertEqual(response.hostha.hastate, "Available")

    def configureAndEnableHostHa(self):
        self.apiClient.configureHAForHost(self.getHostHaConfigCmd())

        response = self.apiClient.enableHAForHost(self.getHostHaEnableCmd())
        self.assertEqual(response.haenable, True)

    def configure_and_start_ipmi_server(self, power_state=None):
        """
            Setup ipmisim and enable out-of-band management for host
        """
        self.configure_and_enable_oobm()
        self.start_ipmi_server()
        if power_state:
            bmc = IpmiServerContext().bmc
            bmc.powerstate = power_state

    def assert_issue_command_state(self, command, expected):
        """
            Asserts power action result for a given power command
        """
        if command != 'STATUS':
            self.issue_power_action_cmd(command)
        response = self.issue_power_action_cmd('STATUS')
        self.assertEqual(response.powerstate, expected)

    def kill_agent(self):
        t_end = time.time() + 90
        while time.time() < t_end:
            try:
                SshClient(self.host.ipaddress, port=22, user=self.hostConfig["username"],
                          passwd=self.hostConfig["password"]).execute \
                    ("kill $(ps aux | grep 'cloudstack-agent' | awk '{print $2}')")
                return
            except Exception:
                print("Cannot ssh into: " + self.host.ipaddress)
        self.fail(self)

    def set_host_to_alert(self):
        self.dbclient.execute("update host set host.status = 'Alert' where host.uuid = '%s'" % self.host.id)

    def check_host_transitioned_to_degraded(self):
        t_end = time.time() + 120
        while time.time() < t_end:
            host = self.getHost()
            if host.hostha.hastate in "Degraded":
                return
            else:
                continue
        self.fail(self)

    def wait_util_host_is_fencing(self):
        t_end = time.time() + 120
        while time.time() < t_end:
            host = self.getHost()
            if host.hostha.hastate in "Fencing":
                return
            else:
                continue
        self.fail(self)

    def check_host_transitioned_to_suspect(self):
        t_end = time.time() + 120
        while time.time() < t_end:
            host = self.getHost()
            if host.hostha.hastate in "Suspect":
                return
            else:
                continue
        self.fail(self)

    def check_host_transitioned_to_checking(self):
        t_end = time.time() + 120
        while time.time() < t_end:
            host = self.getHost()
            if host.hostha.hastate in "Checking":
                return
            else:
                continue
        self.fail(self)

    def wait_util_host_is_fenced(self):
        t_end = time.time() + 120
        while time.time() < t_end:
            host = self.getHost()
            if host.hostha.hastate in "Fenced":
                return
            else:
                continue
        self.fail(self)

    def wait_util_host_is_up(self):
        t_end = time.time() + 120
        while time.time() < t_end:
            host = self.getHost()
            if host.state in "Up":
                return
            else:
                continue
        self.fail(self)

    def stop_agent(self):
        SshClient(self.host.ipaddress, port=22, user=self.hostConfig["username"], passwd=self.hostConfig["password"]).execute\
            ("service cloudstack-agent stop")

    def start_agent(self):
        self.ssh_and_restart_agent()
        self.check_host_transition_to_available()

    def ssh_and_restart_agent(self):
        t_end = time.time() + 90
        while time.time() < t_end:
            try:
                SshClient(self.host.ipaddress, port=22, user=self.hostConfig["username"],
                          passwd=self.hostConfig["password"]).execute \
                    ("service cloudstack-agent restart")
                return
            except Exception:
                print("Cannot ssh into: " + self.host.ipaddress)
        self.fail(self)

    def check_host_transition_to_available(self):
        t_end = time.time() + 90
        while time.time() < t_end:
            host = self.getHost()
            if host.hostha.hastate == "Available":
                return
            else:
                continue
        self.fail(self)

    def wait_util_host_is_recovered(self):
        t_end = time.time() + 180
        while time.time() < t_end:
            host = self.getHost()
            if host.hostha.hastate in "Recovered":
                return
            else:
                continue
        self.fail(self)

    def reset_host(self):
        SshClient(self.host.ipaddress, port=22, user=self.hostConfig["username"],
                  passwd=self.hostConfig["password"]).execute \
            ("reboot")

    def deploy_vm(self):
        vm = VirtualMachine.create(
            self.apiClient,
            services=self.services["virtual_machine"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            zoneid=self.zone.id,
            hostid = self.host.id,
            method="POST"
        )

        self.cleanup.append(vm)

    def update_configuration(self, name, value):
        update_configuration_cmd = updateConfiguration.updateConfigurationCmd()
        update_configuration_cmd.name = name
        update_configuration_cmd.value = value
        update_configuration_cmd.clusterid = self.cluster_id

        self.apiClient.updateConfiguration(update_configuration_cmd)


    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_ha_stop_agent_host_is_degraded(self):
        """
            Tests HA state turns Degraded when agent is stopped
        """
        self.deploy_vm()

        # Configure and Enable OOBM, Set HA Provider and Enable HA. At the end checks if HA State is Available
        self.get_host_in_available_state()

        # SSH into the KVM Host and executes kill -9 of the agent
        self.stop_agent()

        # Checks if the host would turn into Degraded in the next 120 seconds
        try:
            self.check_host_transitioned_to_degraded()
        except Exception as e:
            self.start_agent()
            raise Exception("Warning: Exception during test execution : %s" % e)

        # Enable Host
        self.start_agent()

    #@attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_ha_recovering_start_agent_host_is_available(self):
        """
            Tests HA state turns Recovered when agent is stopped and host is reset
        """
        # Configure and Enable OOBM, Set HA Provider and Enable HA. At the end checks if HA State is Available
        # Then kills the agent and wait untill the state is Degraded

        self.deploy_vm()
        # Configure and Enable OOBM, Set HA Provider and Enable HA. At the end checks if HA State is Available
        self.get_host_in_available_state()

        # SSH into the KVM Host and executes kill -9 of the agent
        self.kill_agent()

        # Checks if the host would turn into Degraded in the next 120 seconds
        try:
            self.check_host_transitioned_to_degraded()
        except Exception as e:
            self.start_agent()
            raise Exception("Warning: Exception during test execution : %s" % e)

        # Reset host so a shut down could be emulated. During the bootup host should transition into recovered state
        self.reset_host()

        # Waits until Degraded host turns into Recovered for 180 seconds,
        # if it fails it tries to revert host back to Available
        try:
            self.wait_util_host_is_recovered()
        except Exception as e:
            self.start_agent()
            raise Exception("Warning: Exception during test execution : %s" % e)

        # SSH into the KVM Host and executes service cloudstack-agent restart of the agent
        self.start_agent()

    #@attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_ha_fencing_host(self):
        """
            Tests HA state turns Recovered when agent is stopped and host is reset,
            then configure incorrect OOBM configuration, so that Recover command would fail
            and host would transition into Fenced state.
        """
        self.deploy_vm()

        # Configure and Enable OOBM, Set HA Provider and Enable HA. At the end checks if HA State is Available
        self.get_host_in_available_state()

        # SSH into the KVM Host and executes kill -9 of the agent
        self.kill_agent()

        # Checks if the host would turn into Degraded in the next 120 seconds
        try:
            self.check_host_transitioned_to_degraded()
        except Exception as e:
            self.start_agent()
            raise Exception("Warning: Exception during test execution : %s" % e)

        # Change OOBM Configuration to invalid so it would fail the recover operations.
        cmd = self.getOobmConfigCmd()
        cmd.address = "1.1.1.1"
        self.apiClient.configureOutOfBandManagement(cmd)

        # Reset host so a shut down could be emulated. During the bootup host should transition into recovered state
        self.reset_host()
        self.kill_agent()

        # Waits until Recovering host turns into Fencing for 180 seconds,
        # if it fails it tries to revert host back to Up
        try:
            self.wait_util_host_is_fencing()
        except Exception as e:
            self.ssh_and_restart_agent()
            raise Exception("Warning: Exception during test execution : %s" % e)

        # Configure correct OOBM configuration so that the Fencing operation would succeed
        self.apiClient.configureOutOfBandManagement(self.getOobmConfigCmd())

        # Waits until Fencing host turns into Fenced for 180 seconds,
        # if it fails it tries to revert host back to Up
        try:
            self.wait_util_host_is_fenced()
        except Exception as e:
            self.ssh_and_restart_agent()
            raise Exception("Warning: Exception during test execution : %s" % e)

        # SSH into the KVM Host and executes service cloudstack-agent restart of the agent
        self.ssh_and_restart_agent()

        # Waits until state is Up so that cleanup would be successful
        self.wait_util_host_is_up()

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_ha_kill_agent_host_is_degraded(self):
        """
            Tests HA state turns Suspect/Checking when some activity/health checks fail
            Configures HA, Logs into to a host and restarts the service
            Then it confirms the ha state jumps through Suspect -> Checking -> Available
        """
        # Configure and Enable OOBM, Set HA Provider and Enable HA. At the end checks if HA State is Available
        self.get_host_in_available_state()

        # SSH into the KVM Host and executes kill -9 of the agent
        self.ssh_and_restart_agent()

        # Checks if the host would turn into Suspect in the next 120 seconds
        try:
            self.check_host_transitioned_to_suspect()
        except Exception as e:
            self.start_agent()
            raise Exception("Warning: Exception during test execution : %s" % e)

        # Checks if the host would turn into Degraded in the next 120 seconds
        try:
            self.check_host_transitioned_to_checking()
        except Exception as e:
            self.start_agent()
            raise Exception("Warning: Exception during test execution : %s" % e)

        # Enable Host
        self.check_host_transition_to_available()
