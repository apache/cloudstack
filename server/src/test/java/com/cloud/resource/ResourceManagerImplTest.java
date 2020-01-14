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

import static com.cloud.resource.ResourceState.Event.ErrorsCorrected;
import static com.cloud.resource.ResourceState.Event.InternalEnterMaintenance;
import static com.cloud.resource.ResourceState.Event.UnableToMaintain;
import static com.cloud.resource.ResourceState.Event.UnableToMigrate;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.GetVncPortAnswer;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.event.ActionEventUtils;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.StorageManager;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.ssh.SSHCmdHelper;
import com.cloud.utils.ssh.SshException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.trilead.ssh2.Connection;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ActionEventUtils.class, ResourceManagerImpl.class, SSHCmdHelper.class})
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
    private VMInstanceDao vmInstanceDao;
    @Mock
    private ConfigurationDao configurationDao;

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
    private GetVncPortCommand getVncPortCommandVm1;
    @Mock
    private GetVncPortCommand getVncPortCommandVm2;

    @Mock
    private Connection sshConnection;

    private static long hostId = 1L;
    private static final String hostUsername = "user";
    private static final String hostPassword = "password";
    private static final String hostPrivateIp = "192.168.1.10";

    private static long vm1Id = 1L;
    private static String vm1InstanceName = "i-1-VM";
    private static long vm2Id = 2L;
    private static String vm2InstanceName = "i-2-VM";

    private static String vm1VncAddress = "10.2.2.2";
    private static int vm1VncPort = 5900;
    private static String vm2VncAddress = "10.2.2.2";
    private static int vm2VncPort = 5901;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(host.getType()).thenReturn(Host.Type.Routing);
        when(host.getId()).thenReturn(hostId);
        when(host.getResourceState()).thenReturn(ResourceState.Enabled);
        when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(host.getClusterId()).thenReturn(1L);
        when(hostDao.findById(hostId)).thenReturn(host);
        when(host.getDetail("username")).thenReturn(hostUsername);
        when(host.getDetail("password")).thenReturn(hostPassword);
        when(host.getStatus()).thenReturn(Status.Up);
        when(host.getPrivateIpAddress()).thenReturn(hostPrivateIp);
        when(vm1.getId()).thenReturn(vm1Id);
        when(vm2.getId()).thenReturn(vm2Id);
        when(vm1.getInstanceName()).thenReturn(vm1InstanceName);
        when(vm2.getInstanceName()).thenReturn(vm2InstanceName);
        when(vmInstanceDao.listByHostId(hostId)).thenReturn(new ArrayList<>());
        when(vmInstanceDao.listVmsMigratingFromHost(hostId)).thenReturn(new ArrayList<>());
        when(vmInstanceDao.listNonMigratingVmsByHostEqualsLastHost(hostId)).thenReturn(new ArrayList<>());
        PowerMockito.mockStatic(ActionEventUtils.class);
        BDDMockito.given(ActionEventUtils.onCompletedActionEvent(anyLong(), anyLong(), anyString(), anyString(), anyString(), anyLong()))
                .willReturn(1L);
        when(getVncPortAnswerVm1.getAddress()).thenReturn(vm1VncAddress);
        when(getVncPortAnswerVm1.getPort()).thenReturn(vm1VncPort);
        when(getVncPortAnswerVm2.getAddress()).thenReturn(vm2VncAddress);
        when(getVncPortAnswerVm2.getPort()).thenReturn(vm2VncPort);
        PowerMockito.whenNew(GetVncPortCommand.class).withArguments(vm1Id, vm1InstanceName).thenReturn(getVncPortCommandVm1);
        PowerMockito.whenNew(GetVncPortCommand.class).withArguments(vm2Id, vm2InstanceName).thenReturn(getVncPortCommandVm2);
        when(agentManager.easySend(eq(hostId), eq(getVncPortCommandVm1))).thenReturn(getVncPortAnswerVm1);
        when(agentManager.easySend(eq(hostId), eq(getVncPortCommandVm2))).thenReturn(getVncPortAnswerVm2);

        PowerMockito.mockStatic(SSHCmdHelper.class);
        BDDMockito.given(SSHCmdHelper.acquireAuthorizedConnection(eq(hostPrivateIp), eq(22),
                eq(hostUsername), eq(hostPassword))).willReturn(sshConnection);
        BDDMockito.given(SSHCmdHelper.sshExecuteCmdOneShot(eq(sshConnection),
                eq("service cloudstack-agent restart"))).
                willReturn(new SSHCmdHelper.SSHCmdResult(0,"",""));

        when(configurationDao.getValue(ResourceManager.KvmSshToAgentEnabled.key())).thenReturn("true");
    }

    @Test
    public void testCheckAndMaintainEnterMaintenanceModeNoVms() throws NoTransitionException {
        // Test entering into maintenance with no VMs running on host.
        boolean enterMaintenanceMode = resourceManager.checkAndMaintain(hostId);
        verify(resourceManager).attemptMaintain(host);
        verify(resourceManager).setHostIntoMaintenance(host);
        verify(resourceManager, never()).setHostIntoErrorInPrepareForMaintenance(anyObject(), anyObject());
        verify(resourceManager, never()).setHostIntoErrorInMaintenance(anyObject(), anyObject());
        verify(resourceManager, never()).setHostIntoPrepareForMaintenanceAfterErrorsFixed(anyObject());
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
        verify(userVmDetailsDao).addDetail(eq(vm1Id), eq("kvm.vnc.address"), eq(vm1VncAddress), anyBoolean());
        verify(userVmDetailsDao).addDetail(eq(vm1Id), eq("kvm.vnc.port"), eq(String.valueOf(vm1VncPort)), anyBoolean());
        verify(userVmDetailsDao).addDetail(eq(vm2Id), eq("kvm.vnc.address"), eq(vm2VncAddress), anyBoolean());
        verify(userVmDetailsDao).addDetail(eq(vm2Id), eq("kvm.vnc.port"), eq(String.valueOf(vm2VncPort)), anyBoolean());
        verify(agentManager).pullAgentToMaintenance(hostId);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetHostCredentialsMissingParameter() {
        when(host.getDetail("password")).thenReturn(null);
        resourceManager.getHostCredentials(host);
    }

    @Test
    public void testGetHostCredentials() {
        Pair<String, String> credentials = resourceManager.getHostCredentials(host);
        Assert.assertNotNull(credentials);
        Assert.assertEquals(hostUsername, credentials.first());
        Assert.assertEquals(hostPassword, credentials.second());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testConnectAndRestartAgentOnHostCannotConnect() {
        BDDMockito.given(SSHCmdHelper.acquireAuthorizedConnection(eq(hostPrivateIp), eq(22),
                eq(hostUsername), eq(hostPassword))).willReturn(null);
        resourceManager.connectAndRestartAgentOnHost(host, hostUsername, hostPassword);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testConnectAndRestartAgentOnHostCannotRestart() throws Exception {
        BDDMockito.given(SSHCmdHelper.sshExecuteCmdOneShot(eq(sshConnection),
                eq("service cloudstack-agent restart"))).willThrow(new SshException("exception"));
        resourceManager.connectAndRestartAgentOnHost(host, hostUsername, hostPassword);
    }

    @Test
    public void testConnectAndRestartAgentOnHost() {
        resourceManager.connectAndRestartAgentOnHost(host, hostUsername, hostPassword);
    }

    @Test
    public void testHandleAgentSSHEnabledNotConnectedAgent() {
        when(host.getStatus()).thenReturn(Status.Disconnected);
        resourceManager.handleAgentIfNotConnected(host, false);
        verify(resourceManager).getHostCredentials(eq(host));
        verify(resourceManager).connectAndRestartAgentOnHost(eq(host), eq(hostUsername), eq(hostPassword));
    }

    @Test
    public void testHandleAgentSSHEnabledConnectedAgent() {
        when(host.getStatus()).thenReturn(Status.Up);
        resourceManager.handleAgentIfNotConnected(host, false);
        verify(resourceManager, never()).getHostCredentials(eq(host));
        verify(resourceManager, never()).connectAndRestartAgentOnHost(eq(host), eq(hostUsername), eq(hostPassword));
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
        when(configurationDao.getValue(ResourceManager.KvmSshToAgentEnabled.key())).thenReturn("false");
        resourceManager.handleAgentIfNotConnected(host, false);
        verify(resourceManager, never()).getHostCredentials(eq(host));
        verify(resourceManager, never()).connectAndRestartAgentOnHost(eq(host), eq(hostUsername), eq(hostPassword));
    }

    @Test
    public void testHandleAgentVMsMigrating() {
        resourceManager.handleAgentIfNotConnected(host, true);
        verify(resourceManager, never()).getHostCredentials(eq(host));
        verify(resourceManager, never()).connectAndRestartAgentOnHost(eq(host), eq(hostUsername), eq(hostPassword));
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
        when(haManager.hasPendingMigrationsWork(vm2.getId())).thenReturn(false);
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
        verify(resourceManager).setHostIntoErrorInMaintenance(eq(host), anyObject());
        verify(resourceManager, never()).setHostIntoMaintenance(anyObject());
        verify(resourceManager, never()).setHostIntoErrorInPrepareForMaintenance(anyObject(), anyObject());
        verify(resourceManager, never()).setHostIntoPrepareForMaintenanceAfterErrorsFixed(anyObject());
        verify(resourceManager).resourceStateTransitTo(eq(host), eq(UnableToMaintain), anyLong());
        Assert.assertFalse(enterMaintenanceMode);
    }

    private void verifyErrorInPrepareForMaintenanceCalls() throws NoTransitionException {
        boolean enterMaintenanceMode = resourceManager.checkAndMaintain(hostId);
        verify(resourceManager).attemptMaintain(host);
        verify(resourceManager).setHostIntoErrorInPrepareForMaintenance(eq(host), anyObject());
        verify(resourceManager, never()).setHostIntoMaintenance(anyObject());
        verify(resourceManager, never()).setHostIntoErrorInMaintenance(anyObject(), anyObject());
        verify(resourceManager, never()).setHostIntoPrepareForMaintenanceAfterErrorsFixed(anyObject());
        verify(resourceManager).resourceStateTransitTo(eq(host), eq(UnableToMigrate), anyLong());
        Assert.assertFalse(enterMaintenanceMode);
    }

    private void verifyReturnToPrepareForMaintenanceCalls() throws NoTransitionException {
        boolean enterMaintenanceMode = resourceManager.checkAndMaintain(hostId);
        verify(resourceManager).attemptMaintain(host);
        verify(resourceManager).setHostIntoPrepareForMaintenanceAfterErrorsFixed(eq(host));
        verify(resourceManager).resourceStateTransitTo(eq(host), eq(ErrorsCorrected), anyLong());
        verify(resourceManager, never()).setHostIntoMaintenance(anyObject());
        verify(resourceManager, never()).setHostIntoErrorInPrepareForMaintenance(anyObject(), anyObject());
        verify(resourceManager, never()).setHostIntoErrorInMaintenance(anyObject(), anyObject());
        Assert.assertFalse(enterMaintenanceMode);
    }

    private void verifyNoChangeInMaintenance() throws NoTransitionException {
        boolean enterMaintenanceMode = resourceManager.checkAndMaintain(hostId);
        verify(resourceManager).attemptMaintain(host);
        verify(resourceManager, never()).setHostIntoMaintenance(anyObject());
        verify(resourceManager, never()).setHostIntoErrorInPrepareForMaintenance(anyObject(), anyObject());
        verify(resourceManager, never()).setHostIntoErrorInMaintenance(anyObject(), anyObject());
        verify(resourceManager, never()).setHostIntoPrepareForMaintenanceAfterErrorsFixed(anyObject());
        verify(resourceManager, never()).resourceStateTransitTo(anyObject(), any(), anyLong());
        Assert.assertFalse(enterMaintenanceMode);
    }
}
