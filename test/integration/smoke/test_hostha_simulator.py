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

from ipmisim.ipmisim import IpmiServerContext, IpmiServer, ThreadedIpmiServer

import random
import socket
import sys
import _thread
import time


class TestHostHA(cloudstackTestCase):
    """ Test host-ha business logic using Simulator
    """

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.dbclient = self.testClient.getDbConnection()
        self.services = self.testClient.getParsedTestDataConfig()
        self.mgtSvrDetails = self.config.__dict__["mgtSvr"][0].__dict__
        self.fakeMsId = random.randint(10000, 99999) * random.randint(10, 20)
        self.host = None

        # Cleanup any existing configs
        self.dbclient.execute("delete from ha_config where resource_type='Host'")

        # use random port for ipmisim
        s = socket.socket()
        s.bind(('', 0))
        self.serverPort = s.getsockname()[1]
        s.close()

        # Get a host to run tests against
        self.host = self.getHost()

        self.cleanup = []


    def tearDown(self):
        try:
            host = self.getHost()
            self.configureAndDisableHostHa(host.id)
            self.host = None
            self.dbclient.execute("delete from mshost_peer where peer_runid=%s" % self.getFakeMsRunId())
            self.dbclient.execute("delete from mshost where runid=%s" % self.getFakeMsRunId())
            self.dbclient.execute("delete from cluster_details where name='resourceHAEnabled'")
            self.dbclient.execute("delete from data_center_details where name='resourceHAEnabled'")
            self.dbclient.execute("delete from ha_config where resource_type='Host'")
            self.dbclient.execute("update host set resource_state='Enabled' where type='Routing' and resource_state='Maintenance'")
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)


    def getFakeMsId(self):
        return self.fakeMsId


    def getFakeMsRunId(self):
        return self.fakeMsId * 1000


    def getHost(self, hostId=None):
        if self.host and hostId is None:
            return self.host

        response = list_hosts(
            self.apiclient,
            type='Routing',
            hypervisor='Simulator',
            resourcestate='Enabled',
            id=hostId
        )

        if response and len(response) > 0:
            random.shuffle(response)
            self.host = response[0]
            return self.host
        raise self.skipTest("No suitable hosts found, skipping host-ha test")


    def getHostHaConfigCmd(self, provider='simulatorhaprovider'):
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


    def getListHostHAResources(self):
        cmd = listHostHAResources.listHostHAResourcesCmd()
        cmd.hostid = self.getHost().id
        return cmd


    def configureAndEnableHostHa(self, initialize=True):
        self.apiclient.configureHAForHost(self.getHostHaConfigCmd())
        response = self.apiclient.enableHAForHost(self.getHostHaEnableCmd())
        self.assertEqual(response.haenable, True)
        if initialize:
            self.configureSimulatorHAProviderState(True, True, True, False)


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


    def configureSimulatorHAProviderState(self, health, activity, recover, fence):
        cmd = configureSimulatorHAProviderState.configureSimulatorHAProviderStateCmd()
        cmd.hostid = self.getHost().id
        cmd.health = health
        cmd.activity = activity
        cmd.recover = recover
        cmd.fence = fence
        response = self.apiclient.configureSimulatorHAProviderState(cmd)
        self.assertEqual(response.success, True)


    def getSimulatorHAStateTransitions(self, hostId):
        cmd = listSimulatorHAStateTransitions.listSimulatorHAStateTransitionsCmd()
        cmd.hostid = hostId
        return self.apiclient.listSimulatorHAStateTransitions(cmd)


    def checkSyncToState(self, state, interval=5000):
        def checkForStateSync(expectedState):
            response = self.getHost(hostId=self.getHost().id).hostha
            print(("checkForStateSync:: response=%s, expected=%s" % (response, expectedState)))
            return response.hastate == expectedState, None

        sync_interval = 1 + int(interval) / 1000
        res, _ = wait_until(sync_interval, 100, checkForStateSync, state)
        if not res:
            self.fail("Failed to get host.hastate synced to expected state:" + state)
        response = self.getHost(hostId=self.getHost().id).hostha
        self.assertEqual(response.hastate, state)


    def getNonConfiguredHaHost(self):
        response = list_hosts(
            self.apiclient,
            type='Routing'
        )
        for host in response:
            if host.haprovider is None:
                return host
            else:
                return None


    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_hostha_enable_feature_without_setting_provider(self):
        """
            Tests Enable HA without setting the provider, Exception is thrown
        """
        host = self.getNonConfiguredHaHost()

        if host is None:
            cloudstackTestCase.skipTest(self, "There is no non configured hosts. Skipping test.")

        cmd = self.getHostHaEnableCmd()
        cmd.hostid = host.id

        try:
            response = self.apiclient.enableHAForHost(cmd)
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")


    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_ha_list_providers(self):
        """
            Tests default ha providers list
        """
        cmd = listHostHAProviders.listHostHAProvidersCmd()

        cmd.hypervisor = 'Simulator'
        response = self.apiclient.listHostHAProviders(cmd)[0]
        self.assertEqual(response.haprovider, 'SimulatorHAProvider')

        cmd.hypervisor = 'KVM'
        response = self.apiclient.listHostHAProviders(cmd)[0]
        self.assertEqual(response.haprovider, 'KVMHAProvider')


    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
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


    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_hostha_configure_default_driver(self):
        """
            Tests host-ha configuration with valid data
        """
        cmd = self.getHostHaConfigCmd()
        response = self.apiclient.configureHAForHost(cmd)
        self.assertEqual(response.hostid, cmd.hostid)
        self.assertEqual(response.haprovider, cmd.provider.lower())


    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
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


    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
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


    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_hostha_enable_feature_valid(self):
        """
            Tests host-ha enable feature with valid options
        """
        self.apiclient.configureHAForHost(self.getHostHaConfigCmd())
        cmd = self.getHostHaEnableCmd()
        response = self.apiclient.enableHAForHost(cmd)
        self.assertEqual(response.hostid, cmd.hostid)
        self.assertEqual(response.haenable, True)


    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
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


    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_ha_configure_enabledisable_across_clusterzones(self):
        """
            Tests ha enable/disable feature at cluster and zone level
            Zone > Cluster > Host
        """
        host = self.getHost()
        self.configureAndDisableHostHa(host.id)
        self.configureAndEnableHostHa()

        self.checkSyncToState('Available')
        response = self.getHost(hostId=host.id).hostha
        self.assertTrue(response.hastate == 'Available')

        # Disable at host level
        cmd = disableHAForHost.disableHAForHostCmd()
        cmd.hostid = host.id
        response = self.apiclient.disableHAForHost(cmd)

        # Disable at cluster level
        cmd = disableHAForCluster.disableHAForClusterCmd()
        cmd.clusterid = host.clusterid
        response = self.apiclient.disableHAForCluster(cmd)

        # Disable at zone level
        cmd = disableHAForZone.disableHAForZoneCmd()
        cmd.zoneid = host.zoneid
        response = self.apiclient.disableHAForZone(cmd)

        # HA state check
        response = self.getHost(hostId=host.id).hostha
        self.assertTrue(response.hastate == 'Disabled')

        # Check ha-state check and sync
        self.dbclient.execute("update ha_config set ha_state='Available' where enabled='1' and resource_type='Host'")
        self.checkSyncToState('Disabled')

        # Enable at zone level
        cmd = enableHAForZone.enableHAForZoneCmd()
        cmd.zoneid = host.zoneid
        response = self.apiclient.enableHAForZone(cmd)

        # Enable at cluster level
        cmd = enableHAForCluster.enableHAForClusterCmd()
        cmd.clusterid = host.clusterid
        response = self.apiclient.enableHAForCluster(cmd)

        # Enable at host level
        cmd = enableHAForHost.enableHAForHostCmd()
        cmd.hostid = host.id
        response = self.apiclient.enableHAForHost(cmd)

        # Check state sync
        self.checkSyncToState('Available')


    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_ha_multiple_mgmt_server_ownership(self):
        """
            Tests ha resource ownership expiry across multi-mgmt server
        """
        host = self.getHost()
        self.configureAndDisableHostHa(host.id)
        self.configureSimulatorHAProviderState(True, True, True, False)
        self.configureAndEnableHostHa(False)

        cloudstackVersion = Configurations.listCapabilities(self.apiclient).cloudstackversion

        currentMsHosts = []
        mshosts = self.dbclient.execute(
            "select msid from mshost where version='%s' and removed is NULL and state='Up'" % (cloudstackVersion))
        if len(mshosts) > 0:
            currentMsHosts = [row[0] for row in mshosts]

        # Inject fake ms host
        self.dbclient.execute(
            "insert into mshost (msid,runid,name,state,version,service_ip,service_port,last_update) values (%s,%s,'ha-marvin-fakebox', 'Down', '%s', '127.0.0.1', '22', NOW())" % (
                self.getFakeMsId(), self.getFakeMsRunId(), cloudstackVersion))

        # Pass ownership to the fake ms id
        self.dbclient.execute(
            "update ha_config set mgmt_server_id=%d where resource_type='Host' and enabled=1 and provider='simulatorhaprovider'" % self.getFakeMsId())

        pingInterval = float(list_configurations(
            self.apiclient,
            name='ping.interval'
        )[0].value)

        pingTimeout = float(list_configurations(
            self.apiclient,
            name='ping.timeout'
        )[0].value)

        def removeFakeMgmtServer(fakeMsRunId):
            rows = self.dbclient.execute("select * from mshost_peer where peer_runid=%s" % fakeMsRunId)
            if len(rows) > 0:
                self.debug("Mgmt server is now trying to contact the fake mgmt server")
                self.dbclient.execute("update mshost set removed=now() where runid=%s" % fakeMsRunId)
                self.dbclient.execute("update mshost_peer set peer_state='Down' where peer_runid=%s" % fakeMsRunId)
                return True, None
            return False, None

        def checkHaOwnershipExpiry(fakeMsId):
            rows = self.dbclient.execute(
                "select mgmt_server_id from ha_config where resource_type='Host' and enabled=1 and provider='simulatorhaprovider'")
            if len(rows) > 0 and rows[0][0] != fakeMsId:
                self.debug("HA resource ownership expired as node was detected to be gone")
                return True, None
            return False, None

        retry_interval = 1 + (pingInterval * pingTimeout / 10)

        res, _ = wait_until(retry_interval, 20, removeFakeMgmtServer, self.getFakeMsRunId())
        if not res:
            self.fail("Management server failed to turn down or remove fake mgmt server")

        res, _ = wait_until(retry_interval, 100, checkHaOwnershipExpiry, self.getFakeMsId())
        if not res:
            self.fail("Management server failed to expire ownership of fenced peer")

        self.debug("Testing ha background sync should claim new ownership")
        self.checkSyncToState('Available')

        result = self.dbclient.execute(
            "select mgmt_server_id from ha_config where resource_type='Host' and enabled=1 and provider='simulatorhaprovider'")
        newOwnerId = result[0][0]
        self.assertTrue(newOwnerId in currentMsHosts)


    def checkFSMTransition(self, transition, event, haState, prevHaState, hasActiviyCounter, hasRecoveryCounter):
        print(("checkFSMTransition:: transition=%s, event=%s, state=%s" % (transition, event, haState)))
        self.assertEqual(transition.event, event)
        self.assertEqual(transition.hastate, haState)
        self.assertEqual(transition.prevhastate, prevHaState)

        if hasActiviyCounter is None:
            pass
        elif hasActiviyCounter:
            self.assertTrue(transition.activitycounter > 0)
        else:
            self.assertEqual(transition.activitycounter, 0)

        if hasRecoveryCounter is None:
            pass
        elif hasRecoveryCounter:
            self.assertTrue(transition.recoverycounter > 0)
        else:
            self.assertEqual(transition.recoverycounter, 0)


    def findFSMTransitionToState(self, state, host):
        transitions = self.getSimulatorHAStateTransitions(host.id)
        if not transitions:
            print("findFSMTransition:: no transitions returned")
            return False, (None, None, None)

        previousTransition = None
        stateTransition = None
        nextTransition = None
        for transition in transitions:
            if stateTransition:
                nextTransition = transition
                break
            if transition.hastate == state:
                stateTransition = transition
            if not stateTransition:
                previousTransition = transition

        print(("findFSMTransition:: prev=%s, cur=%s, next=%s, find state=%s" % (previousTransition, stateTransition, nextTransition, state)))
        if stateTransition:
            return True, (previousTransition, stateTransition, nextTransition,)
        return False, (previousTransition, stateTransition, nextTransition,)


    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_ha_verify_fsm_available(self):
        """
            Tests ha FSM transitions for valid healthy host
            Simulates health check passing
        """

        host = self.getHost()
        self.configureAndDisableHostHa(host.id)
        self.configureSimulatorHAProviderState(True, True, True, False)
        self.configureAndEnableHostHa(False)

        res, (_, T, _) = wait_until(3, 20, self.findFSMTransitionToState, 'available', host)
        if not res:
            self.fail("FSM did not transition to available state")

        self.checkFSMTransition(T, 'enabled', 'available', 'disabled', False, False)


    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_ha_verify_fsm_degraded(self):
        """
            Tests ha FSM transitions leading to degraded state
            Simulates health check failures with activity checks passing
            FSM transitions should happen indefinitely between:
            Available->Suspect<->Checking->Degraded->Available
        """
        host = self.getHost()
        self.configureAndDisableHostHa(host.id)
        self.configureSimulatorHAProviderState(False, True, True, False)
        self.configureAndEnableHostHa(False)

        # Initial health check failure
        res, (_, T, _) = wait_until(3, 50, self.findFSMTransitionToState, 'suspect', host)
        if not res:
            self.fail("FSM did not transition to suspect state")

        self.checkFSMTransition(T, 'healthcheckfailed', 'suspect', 'available', False, False)

        # Check transition to Degraded
        res, (prevT, T, _) = wait_until(3, 100, self.findFSMTransitionToState, 'degraded', host)
        if not res:
            self.fail("FSM did not transition to degraded state")

        if prevT:
            self.checkFSMTransition(prevT, 'performactivitycheck', 'checking', 'suspect', True, False)
        self.checkFSMTransition(T, 'activitycheckfailureunderthresholdratio', 'degraded', 'checking', True, False)


    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_ha_verify_fsm_recovering(self):
        """
            Tests ha FSM transitions leading to recovering
            Simulates both health and activity check failures
            FSM transitions should happen indefinitely between:
            Available->Suspect<->Checking->Recovering->Recovered<-retry-loop->->Fencing
        """
        host = self.getHost()
        self.configureAndDisableHostHa(host.id)
        self.configureSimulatorHAProviderState(False, False, True, False)
        self.configureAndEnableHostHa(False)

        # Initial health check failure
        res, (_, T, _) = wait_until(3, 50, self.findFSMTransitionToState, 'suspect', host)
        if not res:
            self.fail("FSM did not transition to suspect state")

        self.checkFSMTransition(T, 'healthcheckfailed', 'suspect', 'available', False, False)

        # Check transition to recovering
        res, (prevT, T, _) = wait_until(3, 100, self.findFSMTransitionToState, 'recovering', host)
        if not res:
            self.fail("FSM did not transition to recovering state")

        if prevT:
            self.checkFSMTransition(prevT, 'performactivitycheck', 'checking', 'suspect', True, False)
        self.checkFSMTransition(T, 'activitycheckfailureoverthresholdratio', 'recovering', 'checking', True, False)

        # Check transition to fencing due to recovery attempts exceeded
        res, (_, T, _) = wait_until(3, 100, self.findFSMTransitionToState, 'fencing', host)
        if not res:
            self.fail("FSM did not transition to fencing state")

        self.checkFSMTransition(T, 'recoveryoperationthresholdexceeded', 'fencing', 'recovering', None, True)


    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_ha_verify_fsm_fenced(self):
        """
            Tests ha FSM transitions for failures leading to fenced state
            FSM transitions should happen indefinitely between:
            Available->Suspect<->Checking->Recovering<-fail recovery->->Fencing->Fenced
        """
        host = self.getHost()
        self.configureAndDisableHostHa(host.id)
        self.configureSimulatorHAProviderState(False, False, False, True)
        self.configureAndEnableHostHa(False)

        # Check for transition to fenced
        res, (prevT, T, _) = wait_until(3, 100, self.findFSMTransitionToState, 'fenced', host)
        if not res:
            self.fail("FSM did not transition to fenced state")

        self.checkFSMTransition(prevT, 'recoveryoperationthresholdexceeded', 'fencing', 'recovering', False, True)
        self.checkFSMTransition(T, 'fenced', 'fenced', 'fencing', False, False)

        # Simulate manual recovery of host and cancel maintenance mode
        self.configureSimulatorHAProviderState(True, True, True, False)
        cancelCmd = cancelHostMaintenance.cancelHostMaintenanceCmd()
        cancelCmd.id = host.id
        self.apiclient.cancelHostMaintenance(cancelCmd)

        # Check for transition to available after manual recovery
        res, (prevT, T, _) = wait_until(3, 50, self.findFSMTransitionToState, 'available', host)
        if not res:
            self.fail("FSM did not transition to available state")

        self.checkFSMTransition(T, 'eligible', 'available', 'ineligible', False, False)


    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
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


    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
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


        # Setup wrong configuration for the host
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


    def getHaProvider(self, host):
        cmd = listHostHAProviders.listHostHAProvidersCmd()
        cmd.hypervisor = host.hypervisor
        response = self.apiclient.listHostHAProviders(cmd)
        return response[0].haprovider


    def configureHaProvider(self):
        cmd = self.getHostHaConfigCmd(self.getHaProvider(self.getHost()))
        return self.apiclient.configureHAForHost(cmd)


    @attr(tags=["advanced",
                "advancedns",
                "smoke",
                "basic",
                "sg"],
          required_hardware="false")
    def test_list_ha_for_host(self):
        """
            Test that verifies the listHAForHost API
        """
        self.configureHaProvider()
        db_count = self.dbclient.execute("SELECT count(*) FROM cloud.ha_config")

        cmd = self.getListHostHAResources()
        del cmd.hostid
        response = self.apiclient.listHostHAResources(cmd)

        self.assertEqual(db_count[0][0], len(response))


    @attr(tags=["advanced",
                "advancedns",
                "smoke",
                "basic",
                "sg"],
          required_hardware="false")
    def test_list_ha_for_host_valid(self):
        """
            Valid test for listing a specific host HA resources
        """

        self.configureHaProvider()
        cmd = self.getListHostHAResources()
        response = self.apiclient.listHostHAResources(cmd)
        self.assertEqual(response[0].hostid, cmd.hostid)


    @attr(tags=["advanced",
                "advancedns",
                "smoke",
                "basic",
                "sg"],
          required_hardware="false")
    def test_list_ha_for_host_invalid(self):
        """
            Test that listHostHAResources is returning exception when called with invalid data
        """

        self.configureHaProvider()
        cmd = self.getListHostHAResources()
        cmd.hostid = "someinvalidvalue"

        try:
            response = self.apiclient.listHostHAResources(cmd)
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")
