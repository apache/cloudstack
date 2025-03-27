// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloud.resource;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.GetVncPortAnswer;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.event.ActionEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.ScopeType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.StoragePoolAndAccessGroupMapDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.ssh.SSHCmdHelper;
import com.cloud.utils.ssh.SshException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.trilead.ssh2.Connection;
import org.apache.cloudstack.api.command.admin.host.CancelHostAsDegradedCmd;
import org.apache.cloudstack.api.command.admin.host.DeclareHostAsDegradedCmd;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.cloud.resource.ResourceState.Event.ErrorsCorrected;
import static com.cloud.resource.ResourceState.Event.InternalEnterMaintenance;
import static com.cloud.resource.ResourceState.Event.UnableToMaintain;
import static com.cloud.resource.ResourceState.Event.UnableToMigrate;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResourceManagerImplTest {

    @Mock
    private CapacityDao capacityDao;
    @Mock
    private StorageManager storageManager;
    @Mock
    private HighAvailabilityManager haManager;
    @Mock
    private UserVmDetailsDao userVmDetailsDao;
    @Mock
    private AgentManager agentManager;
    @Mock
    private HostDao hostDao;
    @Mock
    private ClusterDao clusterDao;
    @Mock
    private HostPodDao podDao;
    @Mock
    private DataCenterDao dcDao;
    @Mock
    private VMInstanceDao vmInstanceDao;
    @Mock
    private ConfigurationDao configurationDao;
    @Mock
    private VolumeDao volumeDao;
    @Mock
    private PrimaryDataStoreDao storagePoolDao;
    @Mock
    private StoragePoolHostDao storagePoolHostDao;

    @Spy
    @InjectMocks
    private ResourceManagerImpl resourceManager = new ResourceManagerImpl();

    @Mock
    private HostVO host;
    @Mock
    private VMInstanceVO vm1;
    @Mock
    private VMInstanceVO vm2;

    @Mock
    private GetVncPortAnswer getVncPortAnswerVm1;
    @Mock
    private GetVncPortAnswer getVncPortAnswerVm2;
    @Mock
    private VolumeVO rootDisk1;
    @Mock
    private VolumeVO rootDisk2;
    @Mock
    private VolumeVO dataDisk;

    @Mock
    private Connection sshConnection;

    @Mock
    private StoragePoolAndAccessGroupMapDao storagePoolAccessGroupMapDao;

    private static long hostId = 1L;
    private static final String hostUsername = "user";
    private static final String hostPassword = "password";
    private static final String hostPrivateKey = "privatekey";
    private static final String hostPrivateIp = "192.168.1.10";

    private static long vm1Id = 1L;
    private static String vm1InstanceName = "i-1-VM";
    private static long vm2Id = 2L;
    private static String vm2InstanceName = "i-2-VM";

    private static String vm1VncAddress = "10.2.2.2";
    private static int vm1VncPort = 5900;
    private static String vm2VncAddress = "10.2.2.2";
    private static int vm2VncPort = 5901;

    private static long poolId = 1L;
    private List<VolumeVO> rootDisks;
    private List<VolumeVO> dataDisks;
    private MockedStatic<SSHCmdHelper> sshHelperMocked;
    private MockedStatic<ActionEventUtils> actionEventUtilsMocked;
    private MockedConstruction<GetVncPortCommand> getVncPortCommandMockedConstruction;
    private AutoCloseable closeable;

    @Before
    public void setup() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        when(host.getType()).thenReturn(Host.Type.Routing);
        when(host.getId()).thenReturn(hostId);
        when(host.getResourceState()).thenReturn(ResourceState.Enabled);
        when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(hostDao.findById(hostId)).thenReturn(host);
        when(host.getDetail("username")).thenReturn(hostUsername);
        when(host.getDetail("password")).thenReturn(hostPassword);
        when(configurationDao.getValue("ssh.privatekey")).thenReturn(hostPrivateKey);
        when(host.getStatus()).thenReturn(Status.Up);
        when(host.getPrivateIpAddress()).thenReturn(hostPrivateIp);
        when(vm1.getId()).thenReturn(vm1Id);
        when(vm2.getId()).thenReturn(vm2Id);
        when(vm1.getInstanceName()).thenReturn(vm1InstanceName);
        when(vm2.getInstanceName()).thenReturn(vm2InstanceName);
        when(vmInstanceDao.listByHostId(hostId)).thenReturn(new ArrayList<>());
        when(vmInstanceDao.listVmsMigratingFromHost(hostId)).thenReturn(new ArrayList<>());
        when(vmInstanceDao.listNonMigratingVmsByHostEqualsLastHost(hostId)).thenReturn(new ArrayList<>());
        actionEventUtilsMocked = Mockito.mockStatic(ActionEventUtils.class);
        BDDMockito.given(ActionEventUtils.onCompletedActionEvent(anyLong(), anyLong(), anyString(), anyString(), anyString(), anyLong(), anyString(), anyLong()))
                .willReturn(1L);
        when(getVncPortAnswerVm1.getAddress()).thenReturn(vm1VncAddress);
        when(getVncPortAnswerVm1.getPort()).thenReturn(vm1VncPort);
        when(getVncPortAnswerVm2.getAddress()).thenReturn(vm2VncAddress);
        when(getVncPortAnswerVm2.getPort()).thenReturn(vm2VncPort);
        getVncPortCommandMockedConstruction = Mockito.mockConstruction(GetVncPortCommand.class, (mock,context) -> {
            if (context.arguments().get(0).equals(vm1Id) && context.arguments().get(1) == vm1InstanceName) {
                when(agentManager.easySend(eq(hostId), eq(mock))).thenReturn(getVncPortAnswerVm1);
            } else if (context.arguments().get(0).equals(vm2Id) && context.arguments().get(1) == vm2InstanceName) {
                when(agentManager.easySend(eq(hostId), eq(mock))).thenReturn(getVncPortAnswerVm2);
            }
        });

        sshHelperMocked = Mockito.mockStatic(SSHCmdHelper.class);
        BDDMockito.given(SSHCmdHelper.acquireAuthorizedConnection(eq(hostPrivateIp), eq(22),
                eq(hostUsername), eq(hostPassword), eq(hostPrivateKey))).willReturn(sshConnection);
        BDDMockito.given(SSHCmdHelper.sshExecuteCmdOneShot(eq(sshConnection),
                eq("service cloudstack-agent restart"))).
                willReturn(new SSHCmdHelper.SSHCmdResult(0,"",""));

        when(configurationDao.getValue(ResourceManager.KvmSshToAgentEnabled.key())).thenReturn("true");

        rootDisks = Arrays.asList(rootDisk1, rootDisk2);
        dataDisks = Collections.singletonList(dataDisk);
        when(volumeDao.findByPoolId(poolId)).thenReturn(rootDisks);
        when(volumeDao.findByPoolId(poolId, Volume.Type.DATADISK)).thenReturn(dataDisks);
    }

    @After
    public void tearDown() throws Exception {
        sshHelperMocked.close();
        actionEventUtilsMocked.close();
        getVncPortCommandMockedConstruction.close();
        closeable.close();
    }

    @Test
    public void testCheckAndMaintainEnterMaintenanceModeNoVms() throws NoTransitionException {
        // Test entering into maintenance with no VMs running on host.
        boolean enterMaintenanceMode = resourceManager.checkAndMaintain(hostId);
        verify(resourceManager).attemptMaintain(host);
        verify(resourceManager).setHostIntoMaintenance(host);
        verify(resourceManager, never()).setHostIntoErrorInPrepareForMaintenance(any(), any());
        verify(resourceManager, never()).setHostIntoErrorInMaintenance(any(), any());
        verify(resourceManager, never()).setHostIntoPrepareForMaintenanceAfterErrorsFixed(any());
        verify(resourceManager).resourceStateTransitTo(eq(host), eq(InternalEnterMaintenance), anyLong());

        Assert.assertTrue(enterMaintenanceMode);
    }

    @Test
    public void testCheckAndMaintainProceedsWithPrepareForMaintenanceRunningVms() throws NoTransitionException {
        // Test proceeding through with no events if pending migrating works / retries left.
        setupRunningVMs();
        setupPendingMigrationRetries();
        verifyNoChangeInMaintenance();
    }

    @Test
    public void testCheckAndMaintainErrorInMaintenanceRunningVms() throws NoTransitionException {
        // Test entering into ErrorInMaintenance when no pending migrations etc, and due to - Running VMs
        setupRunningVMs();
        setupNoPendingMigrationRetries();
        verifyErrorInMaintenanceCalls();
    }

    @Test
    public void testCheckAndMaintainErrorInMaintenanceWithErrorVms() throws NoTransitionException {
        // Test entering into ErrorInMaintenance when no pending migrations etc, and due to - no migrating but error VMs
        setupErrorVms();
        setupNoPendingMigrationRetries();
        verifyErrorInMaintenanceCalls();
    }

    @Test
    public void testCheckAndMaintainErrorInPrepareForMaintenanceFailedMigrationsPendingRetries() throws NoTransitionException {
        // Test entering into ErrorInPrepareForMaintenance when pending migrations retries and due to - Failed Migrations
        setupFailedMigrations();
        setupPendingMigrationRetries();
        when(vmInstanceDao.findByHostInStates(hostId, VirtualMachine.State.Running)).thenReturn(Arrays.asList(vm2));
        verifyErrorInPrepareForMaintenanceCalls();
    }

    @Test
    public void testCheckAndMaintainErrorInPrepareForMaintenanceWithErrorVmsPendingRetries() throws NoTransitionException {
        // Test entering into ErrorInMaintenance when pending migrations retries due to - no migrating but error VMs
        setupErrorVms();
        setupPendingMigrationRetries();
        when(vmInstanceDao.listVmsMigratingFromHost(hostId)).thenReturn(Arrays.asList(vm2));
        verifyErrorInPrepareForMaintenanceCalls();
    }

    @Test
    public void testCheckAndMaintainErrorInPrepareForMaintenanceFailedMigrationsAndMigratingVms() throws NoTransitionException {
        // Test entering into ErrorInPrepareForMaintenance when no pending migrations retries
        // but executing migration and due to - Failed Migrations
        setupFailedMigrations();
        setupNoPendingMigrationRetries();
        when(vmInstanceDao.listVmsMigratingFromHost(hostId)).thenReturn(Arrays.asList(vm2));
        verifyErrorInPrepareForMaintenanceCalls();
    }

    @Test
    public void testCheckAndMaintainErrorInPrepareForMaintenanceWithErrorVmsAndMigratingVms() throws NoTransitionException {
        // Test entering into ErrorInPrepareForMaintenance when no pending migrations retries
        // but executing migration and due to - Error Vms
        setupErrorVms();
        setupNoPendingMigrationRetries();
        when(vmInstanceDao.listVmsMigratingFromHost(hostId)).thenReturn(Arrays.asList(vm2));
        verifyErrorInPrepareForMaintenanceCalls();
    }

    @Test
    public void testCheckAndMaintainErrorInPrepareForMaintenanceFailedMigrationsAndStoppingVms() throws NoTransitionException {
        // Test entering into ErrorInPrepareForMaintenance when no pending migrations retries
        // but stopping VMs and due to - Failed Migrations
        setupFailedMigrations();
        setupNoPendingMigrationRetries();
        when(vmInstanceDao.findByHostInStates(hostId, VirtualMachine.State.Stopping)).thenReturn(Arrays.asList(vm2));
        verifyErrorInPrepareForMaintenanceCalls();
    }

    @Test
    public void testCheckAndMaintainReturnsToPrepareForMaintenanceRunningVms() throws NoTransitionException {
        // Test switching back to PrepareForMaintenance
        when(host.getResourceState()).thenReturn(ResourceState.ErrorInPrepareForMaintenance);
        setupRunningVMs();
        setupPendingMigrationRetries();
        verifyReturnToPrepareForMaintenanceCalls();
    }

    @Test
    public void testConfigureVncAccessForKVMHostFailedMigrations() {
        when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        List<VMInstanceVO> vms = Arrays.asList(vm1, vm2);
        resourceManager.configureVncAccessForKVMHostFailedMigrations(host, vms);
        verify(agentManager).pullAgentOutMaintenance(hostId);
        verify(resourceManager).setKVMVncAccess(hostId, vms);
        verify(agentManager, times(vms.size())).easySend(eq(hostId), any(GetVncPortCommand.class));
        verify(agentManager).pullAgentToMaintenance(hostId);
        verify(userVmDetailsDao).addDetail(eq(vm1Id), eq("kvm.vnc.address"), eq(vm1VncAddress), anyBoolean());
        verify(userVmDetailsDao).addDetail(eq(vm1Id), eq("kvm.vnc.port"), eq(String.valueOf(vm1VncPort)), anyBoolean());
        verify(userVmDetailsDao).addDetail(eq(vm2Id), eq("kvm.vnc.address"), eq(vm2VncAddress), anyBoolean());
        verify(userVmDetailsDao).addDetail(eq(vm2Id), eq("kvm.vnc.port"), eq(String.valueOf(vm2VncPort)), anyBoolean());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetHostCredentialsMissingParameter() {
        when(host.getDetail("password")).thenReturn(null);
        when(configurationDao.getValue("ssh.privatekey")).thenReturn(null);
        resourceManager.getHostCredentials(host);
    }

    @Test
    public void testGetHostCredentials() {
        Ternary<String, String, String> credentials = resourceManager.getHostCredentials(host);
        Assert.assertNotNull(credentials);
        Assert.assertEquals(hostUsername, credentials.first());
        Assert.assertEquals(hostPassword, credentials.second());
        Assert.assertEquals(hostPrivateKey, credentials.third());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testConnectAndRestartAgentOnHostCannotConnect() {
        BDDMockito.given(SSHCmdHelper.acquireAuthorizedConnection(eq(hostPrivateIp), eq(22),
                eq(hostUsername), eq(hostPassword), eq(hostPrivateKey))).willReturn(null);
        resourceManager.connectAndRestartAgentOnHost(host, hostUsername, hostPassword, hostPrivateKey);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testConnectAndRestartAgentOnHostCannotRestart() throws Exception {
        BDDMockito.given(SSHCmdHelper.sshExecuteCmdOneShot(eq(sshConnection),
                eq("service cloudstack-agent restart"))).willThrow(new SshException("exception"));
        resourceManager.connectAndRestartAgentOnHost(host, hostUsername, hostPassword, hostPrivateKey);
    }

    @Test
    public void testConnectAndRestartAgentOnHost() {
        resourceManager.connectAndRestartAgentOnHost(host, hostUsername, hostPassword, hostPrivateKey);
    }

    @Test
    public void testHandleAgentSSHEnabledNotConnectedAgent() {
        when(host.getStatus()).thenReturn(Status.Disconnected);
        resourceManager.handleAgentIfNotConnected(host, false);
        verify(resourceManager).getHostCredentials(eq(host));
        verify(resourceManager).connectAndRestartAgentOnHost(eq(host), eq(hostUsername), eq(hostPassword), eq(hostPrivateKey));
    }

    @Test
    public void testHandleAgentSSHEnabledConnectedAgent() {
        when(host.getStatus()).thenReturn(Status.Up);
        resourceManager.handleAgentIfNotConnected(host, false);
        verify(resourceManager, never()).getHostCredentials(eq(host));
        verify(resourceManager, never()).connectAndRestartAgentOnHost(eq(host), eq(hostUsername), eq(hostPassword), eq(hostPrivateKey));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testHandleAgentSSHDisabledNotConnectedAgent() {
        when(host.getStatus()).thenReturn(Status.Disconnected);
        when(configurationDao.getValue(ResourceManager.KvmSshToAgentEnabled.key())).thenReturn("false");
        resourceManager.handleAgentIfNotConnected(host, false);
    }

    @Test
    public void testHandleAgentSSHDisabledConnectedAgent() {
        when(host.getStatus()).thenReturn(Status.Up);
        resourceManager.handleAgentIfNotConnected(host, false);
        verify(resourceManager, never()).getHostCredentials(eq(host));
        verify(resourceManager, never()).connectAndRestartAgentOnHost(eq(host), eq(hostUsername), eq(hostPassword), eq(hostPrivateKey));
    }

    @Test
    public void testHandleAgentVMsMigrating() {
        resourceManager.handleAgentIfNotConnected(host, true);
        verify(resourceManager, never()).getHostCredentials(eq(host));
        verify(resourceManager, never()).connectAndRestartAgentOnHost(eq(host), eq(hostUsername), eq(hostPassword), eq(hostPrivateKey));
    }

    private void setupNoPendingMigrationRetries() {
        when(haManager.hasPendingMigrationsWork(vm1.getId())).thenReturn(false);
        when(haManager.hasPendingMigrationsWork(vm2.getId())).thenReturn(false);
    }

    private void setupRunningVMs() {
        when(vmInstanceDao.listByHostId(hostId)).thenReturn(Arrays.asList(vm1, vm2));
        when(vmInstanceDao.findByHostInStates(hostId, VirtualMachine.State.Migrating, VirtualMachine.State.Running, VirtualMachine.State.Starting, VirtualMachine.State.Stopping, VirtualMachine.State.Error, VirtualMachine.State.Unknown)).thenReturn(Arrays.asList(vm1, vm2));
        when(vmInstanceDao.findByHostInStates(hostId, VirtualMachine.State.Running)).thenReturn(Arrays.asList(vm1, vm2));
    }

    private void setupPendingMigrationRetries() {
        when(haManager.hasPendingMigrationsWork(vm1.getId())).thenReturn(true);
    }

    private void setupFailedMigrations() {
        when(vmInstanceDao.listByHostId(hostId)).thenReturn(Arrays.asList(vm1, vm2));
        when(vmInstanceDao.findByHostInStates(hostId, VirtualMachine.State.Migrating, VirtualMachine.State.Running, VirtualMachine.State.Starting, VirtualMachine.State.Stopping, VirtualMachine.State.Error, VirtualMachine.State.Unknown)).thenReturn(Arrays.asList(vm1, vm2));
        when(vmInstanceDao.listNonMigratingVmsByHostEqualsLastHost(hostId)).thenReturn(Arrays.asList(vm1));
    }

    private void setupErrorVms() {
        when(vmInstanceDao.listByHostId(hostId)).thenReturn(Arrays.asList(vm1, vm2));
        when(vmInstanceDao.findByHostInStates(hostId, VirtualMachine.State.Migrating, VirtualMachine.State.Running, VirtualMachine.State.Starting, VirtualMachine.State.Stopping, VirtualMachine.State.Error, VirtualMachine.State.Unknown)).thenReturn(Arrays.asList(vm1, vm2));
        when(vmInstanceDao.findByHostInStates(hostId, VirtualMachine.State.Unknown, VirtualMachine.State.Error)).thenReturn(Arrays.asList(vm1));
    }

    private void verifyErrorInMaintenanceCalls() throws NoTransitionException {
        boolean enterMaintenanceMode = resourceManager.checkAndMaintain(hostId);
        verify(resourceManager).attemptMaintain(host);
        verify(resourceManager).setHostIntoErrorInMaintenance(eq(host), any());
        verify(resourceManager, never()).setHostIntoMaintenance(any());
        verify(resourceManager, never()).setHostIntoErrorInPrepareForMaintenance(any(), any());
        verify(resourceManager, never()).setHostIntoPrepareForMaintenanceAfterErrorsFixed(any());
        verify(resourceManager).resourceStateTransitTo(eq(host), eq(UnableToMaintain), anyLong());
        Assert.assertFalse(enterMaintenanceMode);
    }

    private void verifyErrorInPrepareForMaintenanceCalls() throws NoTransitionException {
        boolean enterMaintenanceMode = resourceManager.checkAndMaintain(hostId);
        verify(resourceManager).attemptMaintain(host);
        verify(resourceManager).setHostIntoErrorInPrepareForMaintenance(eq(host), any());
        verify(resourceManager, never()).setHostIntoMaintenance(any());
        verify(resourceManager, never()).setHostIntoErrorInMaintenance(any(), any());
        verify(resourceManager, never()).setHostIntoPrepareForMaintenanceAfterErrorsFixed(any());
        verify(resourceManager).resourceStateTransitTo(eq(host), eq(UnableToMigrate), anyLong());
        Assert.assertFalse(enterMaintenanceMode);
    }

    private void verifyReturnToPrepareForMaintenanceCalls() throws NoTransitionException {
        boolean enterMaintenanceMode = resourceManager.checkAndMaintain(hostId);
        verify(resourceManager).attemptMaintain(host);
        verify(resourceManager).setHostIntoPrepareForMaintenanceAfterErrorsFixed(eq(host));
        verify(resourceManager).resourceStateTransitTo(eq(host), eq(ErrorsCorrected), anyLong());
        verify(resourceManager, never()).setHostIntoMaintenance(any());
        verify(resourceManager, never()).setHostIntoErrorInPrepareForMaintenance(any(), any());
        verify(resourceManager, never()).setHostIntoErrorInMaintenance(any(), any());
        Assert.assertFalse(enterMaintenanceMode);
    }

    private void verifyNoChangeInMaintenance() throws NoTransitionException {
        boolean enterMaintenanceMode = resourceManager.checkAndMaintain(hostId);
        verify(resourceManager).attemptMaintain(host);
        verify(resourceManager, never()).setHostIntoMaintenance(any());
        verify(resourceManager, never()).setHostIntoErrorInPrepareForMaintenance(any(), any());
        verify(resourceManager, never()).setHostIntoErrorInMaintenance(any(), any());
        verify(resourceManager, never()).setHostIntoPrepareForMaintenanceAfterErrorsFixed(any());
        verify(resourceManager, never()).resourceStateTransitTo(any(), any(), anyLong());
        Assert.assertFalse(enterMaintenanceMode);
    }

    @Test
    public void declareHostAsDegradedTestDisconnected() throws NoTransitionException {
        prepareAndTestDeclareHostAsDegraded(Status.Disconnected, ResourceState.Enabled, ResourceState.Degraded);
    }

    @Test
    public void declareHostAsDegradedTestAlert() throws NoTransitionException {
        prepareAndTestDeclareHostAsDegraded(Status.Alert, ResourceState.Enabled, ResourceState.Degraded);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void declareHostAsDegradedExpectNoTransitionException() throws NoTransitionException {
        Status[] statusArray = Status.values();
        for (int i = 0; i < statusArray.length - 1; i++) {
            if (statusArray[i] != Status.Alert && statusArray[i] != Status.Disconnected) {
                prepareAndTestDeclareHostAsDegraded(statusArray[i], ResourceState.Enabled, ResourceState.Enabled);
            }
        }
    }

    @Test(expected = NoTransitionException.class)
    public void declareHostAsDegradedTestAlreadyDegraded() throws NoTransitionException {
        prepareAndTestDeclareHostAsDegraded(Status.Alert, ResourceState.Degraded, ResourceState.Degraded);
    }

    @Test(expected = NoTransitionException.class)
    public void declareHostAsDegradedTestOnError() throws NoTransitionException {
        prepareAndTestDeclareHostAsDegraded(Status.Alert, ResourceState.Error, ResourceState.Degraded);
    }

    @Test(expected = NoTransitionException.class)
    public void declareHostAsDegradedTestOnCreating() throws NoTransitionException {
        prepareAndTestDeclareHostAsDegraded(Status.Alert, ResourceState.Creating, ResourceState.Degraded);
    }

    @Test(expected = NoTransitionException.class)
    public void declareHostAsDegradedTestOnErrorInMaintenance() throws NoTransitionException {
        prepareAndTestDeclareHostAsDegraded(Status.Alert, ResourceState.ErrorInPrepareForMaintenance, ResourceState.Degraded);
    }

    @Test
    public void declareHostAsDegradedTestSupportedStates() throws NoTransitionException {
        ResourceState[] states = ResourceState.values();
        for (int i = 0; i < states.length - 1; i++) {
            if (states[i] == ResourceState.Enabled
                    || states[i] == ResourceState.Maintenance
                    || states[i] == ResourceState.Disabled) {
                prepareAndTestDeclareHostAsDegraded(Status.Alert, states[i], ResourceState.Degraded);
            }
        }
    }

    private void prepareAndTestDeclareHostAsDegraded(Status hostStatus, ResourceState originalState, ResourceState expectedResourceState) throws NoTransitionException {
        DeclareHostAsDegradedCmd declareHostAsDegradedCmd = Mockito.spy(new DeclareHostAsDegradedCmd());
        HostVO hostVo = createDummyHost(hostStatus);
        hostVo.setResourceState(originalState);
        when(declareHostAsDegradedCmd.getId()).thenReturn(0l);
        when(hostDao.findById(0l)).thenReturn(hostVo);

        Host result = resourceManager.declareHostAsDegraded(declareHostAsDegradedCmd);

        Assert.assertEquals(expectedResourceState, hostVo.getResourceState());
    }

    @Test
    public void cancelHostAsDegradedTest() throws NoTransitionException {
        prepareAndTestCancelHostAsDegraded(Status.Alert, ResourceState.Degraded, ResourceState.Enabled);
    }

    @Test(expected = NoTransitionException.class)
    public void cancelHostAsDegradedTestHostNotDegraded() throws NoTransitionException {
        prepareAndTestCancelHostAsDegraded(Status.Alert, ResourceState.Enabled, ResourceState.Enabled);
    }

    private void prepareAndTestCancelHostAsDegraded(Status hostStatus, ResourceState originalState, ResourceState expectedResourceState) throws NoTransitionException {
        CancelHostAsDegradedCmd cancelHostAsDegradedCmd = Mockito.spy(new CancelHostAsDegradedCmd());
        HostVO hostVo = createDummyHost(hostStatus);
        hostVo.setResourceState(originalState);
        when(cancelHostAsDegradedCmd.getId()).thenReturn(0l);
        when(hostDao.findById(0l)).thenReturn(hostVo);

        Host result = resourceManager.cancelHostAsDegraded(cancelHostAsDegradedCmd);

        Assert.assertEquals(expectedResourceState, hostVo.getResourceState());
    }

    private HostVO createDummyHost(Status hostStatus) {
        return new HostVO(1L, "host01", Host.Type.Routing, "192.168.1.1", "255.255.255.0", null, null, null, null, null, null, null, null, null, null, UUID.randomUUID().toString(),
                hostStatus, "1.0", null, null, 1L, null, 0, 0, null, 0, null);
    }

    @Test
    public void testDestroyLocalStoragePoolVolumesBothRootDisksAndDataDisks() {
        resourceManager.destroyLocalStoragePoolVolumes(poolId);
        verify(volumeDao, times(rootDisks.size() + dataDisks.size()))
                .updateAndRemoveVolume(any(VolumeVO.class));
    }

    @Test
    public void testDestroyLocalStoragePoolVolumesOnlyRootDisks() {
        when(volumeDao.findByPoolId(poolId, Volume.Type.DATADISK)).thenReturn(null);
        resourceManager.destroyLocalStoragePoolVolumes(poolId);
        verify(volumeDao, times(rootDisks.size())).updateAndRemoveVolume(any(VolumeVO.class));
    }

    @Test
    public void testDestroyLocalStoragePoolVolumesOnlyDataDisks() {
        when(volumeDao.findByPoolId(poolId)).thenReturn(null);
        resourceManager.destroyLocalStoragePoolVolumes(poolId);
        verify(volumeDao, times(dataDisks.size())).updateAndRemoveVolume(any(VolumeVO.class));
    }

    @Test
    public void testDestroyLocalStoragePoolVolumesNoDisks() {
        when(volumeDao.findByPoolId(poolId)).thenReturn(null);
        when(volumeDao.findByPoolId(poolId, Volume.Type.DATADISK)).thenReturn(null);
        resourceManager.destroyLocalStoragePoolVolumes(poolId);
        verify(volumeDao, never()).updateAndRemoveVolume(any(VolumeVO.class));
    }

    @Test
    public void testEmptyHostList() {
        List<HostVO> allHosts = new ArrayList<>();
        List<String> storageAccessGroups = Arrays.asList("group1", "group2");

        List<HostVO> hostsToConnect = resourceManager.filterHostsBasedOnStorageAccessGroups(allHosts, storageAccessGroups);

        Assert.assertTrue("No hosts should be returned when the host list is empty.", hostsToConnect.isEmpty());
    }

    @Test
    public void testEmptyStorageAccessGroups() {
        List<HostVO> allHosts = Arrays.asList(Mockito.mock(HostVO.class), Mockito.mock(HostVO.class));
        List<String> storageAccessGroups = new ArrayList<>();

        for (HostVO host : allHosts) {
            Mockito.when(host.getId()).thenReturn(1L);
            Mockito.doReturn(new String[]{"group1", "group2"})
                    .when(storageManager).getStorageAccessGroups(null, null, null, 1L);
        }

        List<HostVO> hostsToConnect = resourceManager.filterHostsBasedOnStorageAccessGroups(allHosts, storageAccessGroups);

        Assert.assertTrue("All hosts should be returned when storage access groups are empty.", hostsToConnect.containsAll(allHosts));
        Assert.assertEquals("The number of returned hosts should match the total number of hosts.", allHosts.size(), hostsToConnect.size());
    }

    @Test
    public void testHostWithMatchingStorageAccessGroups() {
        HostVO host1 = Mockito.mock(HostVO.class);
        HostVO host2 = Mockito.mock(HostVO.class);
        List<HostVO> allHosts = Arrays.asList(host1, host2);
        List<String> storageAccessGroups = Arrays.asList("group1", "group2");

        Mockito.when(host1.getId()).thenReturn(1L);
        Mockito.doReturn(new String[]{"group1"})
                .when(storageManager).getStorageAccessGroups(null, null, null, 1L);

        Mockito.when(host2.getId()).thenReturn(2L);
        Mockito.doReturn(new String[]{"group3"})
                .when(storageManager).getStorageAccessGroups(null, null, null, 2L);

        List<HostVO> hostsToConnect = resourceManager.filterHostsBasedOnStorageAccessGroups(allHosts, storageAccessGroups);

        Assert.assertTrue("Only hosts with matching storage access groups should be included.", hostsToConnect.contains(host1));
        Assert.assertFalse("Hosts without matching storage access groups should not be included.", hostsToConnect.contains(host2));
        Assert.assertEquals("Only one host should match the storage access groups.", 1, hostsToConnect.size());
    }

    @Test
    public void testHostWithoutMatchingStorageAccessGroups() {
        HostVO host1 = Mockito.mock(HostVO.class);
        HostVO host2 = Mockito.mock(HostVO.class);
        List<HostVO> allHosts = Arrays.asList(host1, host2);
        List<String> storageAccessGroups = Arrays.asList("group1", "group2");

        Mockito.when(host1.getId()).thenReturn(1L);
        Mockito.doReturn(new String[]{"group3"})
                .when(storageManager).getStorageAccessGroups(null, null, null, 1L);

        Mockito.when(host2.getId()).thenReturn(2L);
        Mockito.doReturn(new String[]{"group4"})
                .when(storageManager).getStorageAccessGroups(null, null, null, 2L);

        List<HostVO> hostsToConnect = resourceManager.filterHostsBasedOnStorageAccessGroups(allHosts, storageAccessGroups);

        Assert.assertTrue("No hosts should match the storage access groups.", hostsToConnect.isEmpty());
    }

    @Test
    public void testMixedMatchingAndNonMatchingHosts() {
        HostVO host1 = Mockito.mock(HostVO.class);
        HostVO host2 = Mockito.mock(HostVO.class);
        HostVO host3 = Mockito.mock(HostVO.class);
        List<HostVO> allHosts = Arrays.asList(host1, host2, host3);
        List<String> storageAccessGroups = Arrays.asList("group1", "group2");

        Mockito.when(host1.getId()).thenReturn(1L);
        Mockito.doReturn(new String[]{"group1"})
                .when(storageManager).getStorageAccessGroups(null, null, null, 1L);

        Mockito.when(host2.getId()).thenReturn(2L);
        Mockito.doReturn(new String[]{"group3"})
                .when(storageManager).getStorageAccessGroups(null, null, null, 2L);

        Mockito.when(host3.getId()).thenReturn(3L);
        Mockito.doReturn(new String[]{"group2"})
                .when(storageManager).getStorageAccessGroups(null, null, null, 3L);

        List<HostVO> hostsToConnect = resourceManager.filterHostsBasedOnStorageAccessGroups(allHosts, storageAccessGroups);

        Assert.assertTrue("Host1 should be included as it matches 'group1'.", hostsToConnect.contains(host1));
        Assert.assertFalse("Host2 should not be included as it doesn't match any group.", hostsToConnect.contains(host2));
        Assert.assertTrue("Host3 should be included as it matches 'group2'.", hostsToConnect.contains(host3));
    }

    @Test
    public void testHostsWithEmptyStorageAccessGroups() {
        HostVO host1 = Mockito.mock(HostVO.class);
        HostVO host2 = Mockito.mock(HostVO.class);
        List<HostVO> allHosts = Arrays.asList(host1, host2);
        List<String> storageAccessGroups = Arrays.asList("group1", "group2");

        Mockito.when(host1.getId()).thenReturn(1L);
        Mockito.doReturn(new String[0])
                .when(storageManager).getStorageAccessGroups(null, null, null, 1L);

        Mockito.when(host2.getId()).thenReturn(2L);
        Mockito.doReturn(new String[0])
                .when(storageManager).getStorageAccessGroups(null, null, null, 2L);

        List<HostVO> hostsToConnect = resourceManager.filterHostsBasedOnStorageAccessGroups(allHosts, storageAccessGroups);

        Assert.assertTrue("No hosts should be included if storage access groups are empty.", hostsToConnect.isEmpty());
    }

    @Test
    public void testZoneLevelWithAllHostsUsingTags() {
        List<String> sagsToDelete = Arrays.asList("tag1", "tag2");
        Long clusterId = null;
        Long podId = null;
        Long zoneId = 3L;

        List<Long> hostIdsUsingStorageTags = Arrays.asList(1L, 2L);
        Mockito.doReturn(hostIdsUsingStorageTags).when(resourceManager).listOfHostIdsUsingTheStorageAccessGroups(sagsToDelete, clusterId, podId, zoneId);

        List<HostVO> hostsInZone = Arrays.asList(Mockito.mock(HostVO.class), Mockito.mock(HostVO.class));
        Mockito.doReturn(hostsInZone).when(hostDao).findByDataCenterId(zoneId);

        Mockito.doReturn(1L).when(hostsInZone.get(0)).getId();
        Mockito.doReturn(2L).when(hostsInZone.get(1)).getId();

        try {
            resourceManager.checkIfAllHostsInUse(sagsToDelete, clusterId, podId, zoneId);
            Assert.fail("Exception should be thrown when all hosts in the zone are using the storage access groups.");
        } catch (CloudRuntimeException e) {
            Assert.assertEquals("All hosts in the zone are using the storage access groups", e.getMessage());
        }
    }

    @Test
    public void testClusterLevelWithAllHostsUsingTags() {
        List<String> sagsToDelete = Arrays.asList("tag1", "tag2");
        Long clusterId = 1L;
        Long podId = null;
        Long zoneId = null;

        List<Long> hostIdsUsingStorageTags = Arrays.asList(1L, 2L);
        Mockito.doReturn(hostIdsUsingStorageTags).when(resourceManager).listOfHostIdsUsingTheStorageAccessGroups(sagsToDelete, clusterId, podId, zoneId);

        List<HostVO> hostsInCluster = Arrays.asList(Mockito.mock(HostVO.class), Mockito.mock(HostVO.class));
        Mockito.doReturn(hostsInCluster).when(hostDao).findByClusterId(clusterId, Host.Type.Routing);

        Mockito.doReturn(1L).when(hostsInCluster.get(0)).getId();
        Mockito.doReturn(2L).when(hostsInCluster.get(1)).getId();

        try {
            resourceManager.checkIfAllHostsInUse(sagsToDelete, clusterId, podId, zoneId);
            Assert.fail("Exception should be thrown when all hosts in the cluster are using the storage access groups.");
        } catch (CloudRuntimeException e) {
            Assert.assertEquals("All hosts in the cluster are using the storage access groups", e.getMessage());
        }
    }

    @Test
    public void testPodLevelWithAllHostsUsingTags() {
        List<String> sagsToDelete = Arrays.asList("tag1", "tag2");
        Long clusterId = null;
        Long podId = 2L;
        Long zoneId = null;

        List<Long> hostIdsUsingStorageTags = Arrays.asList(1L, 2L);
        Mockito.doReturn(hostIdsUsingStorageTags).when(resourceManager).listOfHostIdsUsingTheStorageAccessGroups(sagsToDelete, clusterId, podId, zoneId);

        List<HostVO> hostsInPod = Arrays.asList(Mockito.mock(HostVO.class), Mockito.mock(HostVO.class));
        Mockito.doReturn(hostsInPod).when(hostDao).findByPodId(podId, Host.Type.Routing);

        Mockito.doReturn(1L).when(hostsInPod.get(0)).getId();
        Mockito.doReturn(2L).when(hostsInPod.get(1)).getId();

        try {
            resourceManager.checkIfAllHostsInUse(sagsToDelete, clusterId, podId, zoneId);
            Assert.fail("Exception should be thrown when all hosts in the pod are using the storage access groups.");
        } catch (CloudRuntimeException e) {
            Assert.assertEquals("All hosts in the pod are using the storage access groups", e.getMessage());
        }
    }

    @Test
    public void testCheckIfAnyVolumesInUseWithPoolsToAdd() {
        List<String> sagsToAdd = Arrays.asList("sag1", "sag2");
        List<String> sagsToDelete = Arrays.asList("sag3", "sag4");

        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getId()).thenReturn(1L);
        Mockito.when(host.getDataCenterId()).thenReturn(2L);
        Mockito.when(host.getPodId()).thenReturn(3L);
        Mockito.when(host.getClusterId()).thenReturn(4L);

        VolumeVO volume1 = Mockito.mock(VolumeVO.class);
        VolumeVO volume2 = Mockito.mock(VolumeVO.class);
        Mockito.when(volume1.getPoolId()).thenReturn(10L);
        Mockito.when(volume2.getPoolId()).thenReturn(11L);
        List<VolumeVO> volumesUsingTheStoragePoolAccessGroups = new ArrayList<>(Arrays.asList(volume1, volume2));
        Mockito.doReturn(volumesUsingTheStoragePoolAccessGroups).when(resourceManager).listOfVolumesUsingTheStorageAccessGroups(sagsToDelete, 1L, null, null, null);

        StoragePoolVO pool1 = Mockito.mock(StoragePoolVO.class);
        StoragePoolVO pool2 = Mockito.mock(StoragePoolVO.class);
        Mockito.when(pool1.getId()).thenReturn(10L);
        Mockito.when(pool2.getId()).thenReturn(12L);
        List<StoragePoolVO> poolsToAdd = Arrays.asList(pool1, pool2);

        Mockito.doReturn(poolsToAdd)
                .when(resourceManager).getStoragePoolsByAccessGroups(2L, 3L, 4L, sagsToAdd.toArray(new String[0]), true);

        try {
            resourceManager.checkIfAnyVolumesInUse(sagsToAdd, sagsToDelete, host);
            Assert.fail("Expected a CloudRuntimeException to be thrown.");
        } catch (CloudRuntimeException e) {
            Assert.assertTrue("Exception message should mention volumes in use.",
                    e.getMessage().contains("There are volumes in storage pools with the Storage Access Groups that need to be deleted"));
        }
    }

    @Test
    public void testUpdateStoragePoolConnectionsOnHostsConnect1AndDisconnect2() {
        Long poolId = 1L;
        List<String> storageAccessGroups = Arrays.asList("sag1", "sag2");

        StoragePoolVO storagePool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(storagePool.getId()).thenReturn(poolId);
        Mockito.when(storagePool.getScope()).thenReturn(ScopeType.CLUSTER);
        Mockito.when(storagePool.getClusterId()).thenReturn(1L);
        Mockito.when(storagePool.getPodId()).thenReturn(1L);
        Mockito.when(storagePool.getDataCenterId()).thenReturn(1L);

        Mockito.when(storagePoolDao.findById(poolId)).thenReturn(storagePool);

        HostVO host1 = Mockito.mock(HostVO.class);
        HostVO host2 = Mockito.mock(HostVO.class);
        Mockito.when(host1.getId()).thenReturn(2L);
        Mockito.when(host2.getId()).thenReturn(3L);

        List<HostVO> clusterHosts = Arrays.asList(host1, host2);
        Mockito.doReturn(clusterHosts).when(resourceManager).listAllUpHosts(Host.Type.Routing, 1L, 1L, 1L);

        StoragePoolHostVO hostPoolRecord = Mockito.mock(StoragePoolHostVO.class);
        Mockito.when(storagePoolHostDao.findByPoolHost(poolId, 2L)).thenReturn(null);
        Mockito.when(storagePoolHostDao.findByPoolHost(poolId, 3L)).thenReturn(hostPoolRecord);

        Mockito.doReturn(new String[]{"sag1", "sag2"}).when(storageManager).getStorageAccessGroups(null, null, null, 2L);
        Mockito.doReturn(new String[]{"sag3"}).when(storageManager).getStorageAccessGroups(null, null, null, 3L);

        Mockito.doReturn(new ArrayList<Long>()).when(resourceManager).listOfHostIdsUsingTheStoragePool(poolId);

        try {
            resourceManager.updateStoragePoolConnectionsOnHosts(poolId, storageAccessGroups);

            Mockito.verify(resourceManager, Mockito.times(1)).connectHostToStoragePool(host1, storagePool);
            Mockito.verify(resourceManager, Mockito.never()).connectHostToStoragePool(host2, storagePool);
            Mockito.verify(resourceManager, Mockito.times(1)).disconnectHostFromStoragePool(host2, storagePool);
            Mockito.verify(resourceManager, Mockito.never()).disconnectHostFromStoragePool(host1, storagePool);
        } catch (CloudRuntimeException e) {
            Assert.fail("No exception should be thrown.");
        }
    }

    @Test
    public void testUpdateStoragePoolConnectionsOnHosts_ZoneScope_NoAccessGroups() {
        Long poolId = 1L;
        List<String> storageAccessGroups = new ArrayList<>();

        StoragePoolVO storagePool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(storagePool.getId()).thenReturn(poolId);
        Mockito.when(storagePool.getScope()).thenReturn(ScopeType.ZONE);
        Mockito.when(storagePool.getDataCenterId()).thenReturn(1L);

        Mockito.when(storagePoolDao.findById(poolId)).thenReturn(storagePool);

        HostVO host1 = Mockito.mock(HostVO.class);
        HostVO host2 = Mockito.mock(HostVO.class);
        Mockito.when(host1.getId()).thenReturn(2L);
        Mockito.when(host2.getId()).thenReturn(3L);

        List<HostVO> zoneHosts = Arrays.asList(host1, host2);
        Mockito.doReturn(zoneHosts).when(resourceManager).listAllUpHosts(Host.Type.Routing, null, null, 1L);

        Mockito.doReturn(new String[]{"sag1", "sag2"}).when(storageManager).getStorageAccessGroups(null, null, null, 2L);
        Mockito.doReturn(new String[]{""}).when(storageManager).getStorageAccessGroups(null, null, null, 3L);

        Mockito.when(storagePoolHostDao.findByPoolHost(poolId, 2L)).thenReturn(null);
        Mockito.when(storagePoolHostDao.findByPoolHost(poolId, 3L)).thenReturn(null);

        try {
            resourceManager.updateStoragePoolConnectionsOnHosts(poolId, storageAccessGroups);

            Mockito.verify(resourceManager, Mockito.times(1)).connectHostToStoragePool(host1, storagePool);
            Mockito.verify(resourceManager, Mockito.times(1)).connectHostToStoragePool(host2, storagePool);
            Mockito.verify(resourceManager, Mockito.never()).disconnectHostFromStoragePool(Mockito.any(), Mockito.eq(storagePool));
        } catch (CloudRuntimeException e) {
            Assert.fail("No exception should be thrown.");
        }
    }

    @Test
    public void testUpdateStoragePoolConnectionsOnHosts_ConflictWithHostIdsAndVolumes() {
        Long poolId = 1L;
        List<String> storageAccessGroups = Arrays.asList("sag1", "sag2");

        StoragePoolVO storagePool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(storagePool.getId()).thenReturn(poolId);
        Mockito.when(storagePool.getScope()).thenReturn(ScopeType.CLUSTER);
        Mockito.when(storagePool.getClusterId()).thenReturn(1L);
        Mockito.when(storagePool.getPodId()).thenReturn(1L);
        Mockito.when(storagePool.getDataCenterId()).thenReturn(1L);

        Mockito.when(storagePoolDao.findById(poolId)).thenReturn(storagePool);

        HostVO host1 = Mockito.mock(HostVO.class);
        HostVO host2 = Mockito.mock(HostVO.class);
        Mockito.when(host1.getId()).thenReturn(2L);
        Mockito.when(host2.getId()).thenReturn(3L);

        List<HostVO> clusterHosts = Arrays.asList(host1, host2);
        Mockito.doReturn(clusterHosts).when(resourceManager).listAllUpHosts(Host.Type.Routing, 1L, 1L, 1L);

        VolumeVO volume1 = Mockito.mock(VolumeVO.class);
        VolumeVO volume2 = Mockito.mock(VolumeVO.class);

        Mockito.when(volume1.getInstanceId()).thenReturn(100L);
        Mockito.when(volume2.getInstanceId()).thenReturn(101L);

        List<VolumeVO> volumesInPool = Arrays.asList(volume1, volume2);
        Mockito.doReturn(volumesInPool).when(volumeDao).findByPoolId(poolId);

        VMInstanceVO vmInstance1 = Mockito.mock(VMInstanceVO.class);
        VMInstanceVO vmInstance2 = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vmInstance1.getHostId()).thenReturn(2L);
        Mockito.when(vmInstance2.getHostId()).thenReturn(3L);

        Mockito.doReturn(vmInstance1).when(vmInstanceDao).findById(100L);
        Mockito.doReturn(vmInstance2).when(vmInstanceDao).findById(101L);

        Mockito.when(storagePoolHostDao.findByPoolHost(poolId, 2L)).thenReturn(null);
        Mockito.when(storagePoolHostDao.findByPoolHost(poolId, 3L)).thenReturn(null);

        Mockito.doReturn(new String[]{"sag1"}).when(storageManager).getStorageAccessGroups(null, null, null, 2L);
        Mockito.doReturn(new String[]{"sag3"}).when(storageManager).getStorageAccessGroups(null, null, null, 3L);

        Mockito.doReturn(Arrays.asList(2L, 3L)).when(resourceManager).listOfHostIdsUsingTheStoragePool(poolId);

        try {
            resourceManager.updateStoragePoolConnectionsOnHosts(poolId, storageAccessGroups);
            Assert.fail("Expected a CloudRuntimeException to be thrown.");
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("Storage access groups cannot be updated as they are currently in use by some hosts."));
            Mockito.verify(resourceManager, Mockito.never()).connectHostToStoragePool(Mockito.any(), Mockito.eq(storagePool));
            Mockito.verify(resourceManager, Mockito.never()).disconnectHostFromStoragePool(Mockito.any(), Mockito.eq(storagePool));
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testNoUpHostsThrowsException() {
        PrimaryDataStoreInfo primaryStore = Mockito.mock(PrimaryDataStoreInfo.class);
        Mockito.when(primaryStore.getClusterId()).thenReturn(1L);
        Mockito.doReturn(Collections.emptyList()).when(resourceManager).listAllUpHosts(Mockito.any(), Mockito.anyLong(), Mockito.any(), Mockito.anyLong());
        resourceManager.getEligibleUpHostsInClusterForStorageConnection(primaryStore);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testNoUpAndEnabledHostsThrowsException() {
        PrimaryDataStoreInfo primaryStore = Mockito.mock(PrimaryDataStoreInfo.class);
        Mockito.when(primaryStore.getClusterId()).thenReturn(1L);
        Mockito.doReturn(Collections.emptyList()).when(resourceManager).listAllUpAndEnabledHosts(Mockito.any(), Mockito.anyLong(), Mockito.any(), Mockito.anyLong());
        resourceManager.getEligibleUpAndEnabledHostsInClusterForStorageConnection(primaryStore);
    }

    @Test
    public void testEligibleHostsMatchingStorageAccessGroups() {
        PrimaryDataStoreInfo primaryStore = Mockito.mock(PrimaryDataStoreInfo.class);
        DataStore dataStore = Mockito.mock(DataStore.class);
        Mockito.when(primaryStore.getId()).thenReturn(1L);
        Mockito.when(dataStore.getId()).thenReturn(1L);
        Mockito.when(primaryStore.getClusterId()).thenReturn(1L);

        HostVO host1 = Mockito.mock(HostVO.class);
        HostVO host2 = Mockito.mock(HostVO.class);
        List<HostVO> allHosts = Arrays.asList(host1, host2);

        Mockito.when(host1.getId()).thenReturn(1L);
        Mockito.when(host2.getId()).thenReturn(2L);

        Mockito.doReturn(allHosts).when(resourceManager).listAllUpHosts(Mockito.any(), Mockito.anyLong(), Mockito.any(), Mockito.anyLong());
        Mockito.doReturn(allHosts).when(resourceManager).listAllUpAndEnabledHosts(Mockito.any(), Mockito.anyLong(), Mockito.any(), Mockito.anyLong());
        Mockito.doReturn(allHosts).when(resourceManager).listAllUpAndEnabledHostsInOneZoneByHypervisor(Mockito.any(), Mockito.anyLong());
        Mockito.doReturn(Arrays.asList("group1", "group2")).when(storagePoolAccessGroupMapDao).getStorageAccessGroups(1L);

        Mockito.doReturn(new String[]{"group1"})
                .when(storageManager).getStorageAccessGroups(null, null, null, 1L);
        Mockito.doReturn(new String[]{"group3"})
                .when(storageManager).getStorageAccessGroups(null, null, null, 2L);

        List<HostVO> hostsToConnect = resourceManager.getEligibleUpHostsInClusterForStorageConnection(primaryStore);

        Assert.assertEquals("Only one host should match the storage access groups.", 1, hostsToConnect.size());
        Assert.assertTrue("Host1 should be included as it matches the storage access group.", hostsToConnect.contains(host1));
        Assert.assertFalse("Host2 should not be included as it does not match any storage access group.", hostsToConnect.contains(host2));

        hostsToConnect = resourceManager.getEligibleUpAndEnabledHostsInClusterForStorageConnection(primaryStore);

        Assert.assertEquals("Only one host should match the storage access groups.", 1, hostsToConnect.size());
        Assert.assertTrue("Host1 should be included as it matches the storage access group.", hostsToConnect.contains(host1));
        Assert.assertFalse("Host2 should not be included as it does not match any storage access group.", hostsToConnect.contains(host2));

        hostsToConnect = resourceManager.getEligibleUpAndEnabledHostsInZoneForStorageConnection(dataStore, 1L, Hypervisor.HypervisorType.KVM);

        Assert.assertEquals("Only one host should match the storage access groups.", 1, hostsToConnect.size());
        Assert.assertTrue("Host1 should be included as it matches the storage access group.", hostsToConnect.contains(host1));
        Assert.assertFalse("Host2 should not be included as it does not match any storage access group.", hostsToConnect.contains(host2));
    }

    @Test
    public void testUpdateZoneStorageAccessGroups() {
        long zoneId = 1L;
        long podId = 2L;
        long clusterId = 3L;
        long host1Id = 1L;
        long host2Id = 2L;
        List<String> newStorageAccessGroups = Arrays.asList("group1", "group2");

        DataCenterVO zoneVO = Mockito.mock(DataCenterVO.class);
        Mockito.when(dcDao.findById(zoneId)).thenReturn(zoneVO);
        Mockito.when(zoneVO.getId()).thenReturn(zoneId);
        Mockito.when(zoneVO.getStorageAccessGroups()).thenReturn("group1,group3");

        HostVO host1 = Mockito.mock(HostVO.class);
        HostVO host2 = Mockito.mock(HostVO.class);
        Mockito.when(host1.getId()).thenReturn(host1Id);
        Mockito.when(host2.getId()).thenReturn(host2Id);

        HostPodVO pod1 = Mockito.mock(HostPodVO.class);
        ClusterVO cluster1 = Mockito.mock(ClusterVO.class);
        Mockito.when(pod1.getId()).thenReturn(podId);
        Mockito.when(cluster1.getId()).thenReturn(clusterId);
        Mockito.when(podDao.findById(podId)).thenReturn(pod1);
        Mockito.when(clusterDao.findById(clusterId)).thenReturn(cluster1);
        Mockito.when(podDao.listByDataCenterId(zoneId)).thenReturn(Collections.singletonList(pod1));
        Mockito.when(clusterDao.listByPodId(podId)).thenReturn(Collections.singletonList(cluster1));
        Mockito.when(hostDao.findHypervisorHostInPod(podId)).thenReturn(Arrays.asList(host1, host2));
        Mockito.when(hostDao.findByDataCenterId(zoneId)).thenReturn(Arrays.asList(host1, host2));
        Mockito.when(hostDao.findByClusterId(clusterId)).thenReturn(Arrays.asList(host1, host2));

        List<Long> hostIdsUsingStorageTags = Arrays.asList(host1Id);
        Mockito.doReturn(hostIdsUsingStorageTags).when(resourceManager).listOfHostIdsUsingTheStorageAccessGroups(any(), any(), any(), any());

        Mockito.doReturn(new String[]{"group1", "group3"}).when(storageManager).getStorageAccessGroups(null, null, null, host1Id);
        Mockito.doReturn(new String[]{"group2", "group4"}).when(storageManager).getStorageAccessGroups(null, null, null, host2Id);

        resourceManager.updateZoneStorageAccessGroups(zoneId, newStorageAccessGroups);

        Mockito.verify(hostDao, Mockito.times(2)).update(host1Id, host1);
        Mockito.verify(hostDao, Mockito.times(1)).update(host2Id, host2);
    }

    @Test
    public void testUpdatePodStorageAccessGroups() {
        long podId = 2L;
        long clusterId = 3L;
        long host1Id = 1L;
        long host2Id = 2L;
        List<String> newStorageAccessGroups = Arrays.asList("group1", "group2");

        HostVO host1 = Mockito.mock(HostVO.class);
        HostVO host2 = Mockito.mock(HostVO.class);
        Mockito.when(host1.getId()).thenReturn(host1Id);
        Mockito.when(host2.getId()).thenReturn(host2Id);

        HostPodVO pod1 = Mockito.mock(HostPodVO.class);
        ClusterVO cluster1 = Mockito.mock(ClusterVO.class);
        Mockito.when(pod1.getStorageAccessGroups()).thenReturn("group1,group3");
        Mockito.when(cluster1.getId()).thenReturn(clusterId);
        Mockito.when(podDao.findById(podId)).thenReturn(pod1);
        Mockito.when(clusterDao.findById(clusterId)).thenReturn(cluster1);
        Mockito.when(clusterDao.listByPodId(podId)).thenReturn(Collections.singletonList(cluster1));
        Mockito.when(hostDao.findHypervisorHostInPod(podId)).thenReturn(Arrays.asList(host1, host2));
        Mockito.when(hostDao.findByPodId(podId, Host.Type.Routing)).thenReturn(Arrays.asList(host1, host2));
        Mockito.when(hostDao.findByClusterId(clusterId)).thenReturn(Arrays.asList(host1, host2));

        List<Long> hostIdsUsingStorageTags = Arrays.asList(host1Id);
        Mockito.doReturn(hostIdsUsingStorageTags).when(resourceManager).listOfHostIdsUsingTheStorageAccessGroups(any(), any(), any(), any());

        Mockito.doReturn(new String[]{"group1", "group3"}).when(storageManager).getStorageAccessGroups(null, null, null, host1Id);
        Mockito.doReturn(new String[]{"group2", "group4"}).when(storageManager).getStorageAccessGroups(null, null, null, host2Id);

        resourceManager.updatePodStorageAccessGroups(podId, newStorageAccessGroups);

        Mockito.verify(hostDao, Mockito.times(2)).update(host1Id, host1);
        Mockito.verify(hostDao, Mockito.times(1)).update(host2Id, host2);
    }

    @Test
    public void testUpdateClusterStorageAccessGroups() {
        long clusterId = 3L;
        long host1Id = 1L;
        long host2Id = 2L;
        List<String> newStorageAccessGroups = Arrays.asList("group1", "group2");

        HostVO host1 = Mockito.mock(HostVO.class);
        HostVO host2 = Mockito.mock(HostVO.class);
        Mockito.when(host1.getId()).thenReturn(host1Id);
        Mockito.when(host2.getId()).thenReturn(host2Id);

        ClusterVO cluster1 = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster1.getStorageAccessGroups()).thenReturn("group1,group3");
        Mockito.when(cluster1.getId()).thenReturn(clusterId);
        Mockito.when(clusterDao.findById(clusterId)).thenReturn(cluster1);
        Mockito.when(hostDao.findHypervisorHostInCluster(clusterId)).thenReturn(Arrays.asList(host1, host2));
        Mockito.when(hostDao.findByClusterId(clusterId)).thenReturn(Arrays.asList(host1, host2));
        Mockito.when(hostDao.findByClusterId(clusterId, Host.Type.Routing)).thenReturn(Arrays.asList(host1, host2));

        List<Long> hostIdsUsingStorageTags = Arrays.asList(host1Id);
        Mockito.doReturn(hostIdsUsingStorageTags).when(resourceManager).listOfHostIdsUsingTheStorageAccessGroups(any(), any(), any(), any());

        Mockito.doReturn(new String[]{"group1", "group3"}).when(storageManager).getStorageAccessGroups(null, null, null, host1Id);
        Mockito.doReturn(new String[]{"group2", "group4"}).when(storageManager).getStorageAccessGroups(null, null, null, host2Id);

        resourceManager.updateClusterStorageAccessGroups(clusterId, newStorageAccessGroups);

        Mockito.verify(hostDao, Mockito.times(2)).update(host1Id, host1);
        Mockito.verify(hostDao, Mockito.times(1)).update(host2Id, host2);
    }

    @Test
    public void testUpdateHostStorageAccessGroups() {
        long hostId = 1L;
        long clusterId = 2L;
        List<String> newStorageAccessGroups = Arrays.asList("group1", "group2");

        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getId()).thenReturn(hostId);
        Mockito.when(host.getClusterId()).thenReturn(clusterId);
        Mockito.when(host.getStorageAccessGroups()).thenReturn("group1,group3");

        Mockito.when(hostDao.findById(hostId)).thenReturn(host);
        Mockito.when(storageManager.getStorageAccessGroups(null, null, clusterId, null))
                .thenReturn(new String[]{"group3", "group4"});

        Mockito.doNothing().when(resourceManager).checkIfAnyVolumesInUse(any(), any(), any());
        Mockito.doNothing().when(resourceManager).updateConnectionsBetweenHostsAndStoragePools(any());

        resourceManager.updateHostStorageAccessGroups(hostId, newStorageAccessGroups);

        Mockito.verify(resourceManager).checkIfAnyVolumesInUse(eq(Arrays.asList("group1", "group2", "group3", "group4")),
                eq(Arrays.asList("group3")),
                eq(host));

        Mockito.verify(resourceManager).updateConnectionsBetweenHostsAndStoragePools(
                eq(Collections.singletonMap(host, Arrays.asList("group1", "group2", "group3", "group4")))
        );

        Mockito.verify(host).setStorageAccessGroups("group1,group2");
        Mockito.verify(hostDao).update(hostId, host);
    }
}
