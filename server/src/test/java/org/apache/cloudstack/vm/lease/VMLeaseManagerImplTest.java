/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cloudstack.vm.lease;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.user.vm.DestroyVMCmd;
import org.apache.cloudstack.api.command.user.vm.StopVMCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.event.ActionEventUtils;
import com.cloud.user.User;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.VMInstanceDetailsDao;

@RunWith(MockitoJUnitRunner.class)
public class VMLeaseManagerImplTest {
    public static final String DESTROY = "DESTROY";
    public static final String VM_UUID = UUID.randomUUID().toString();
    public static final String VM_NAME = "vm-name";

    @Spy
    @InjectMocks
    private VMLeaseManagerImpl vmLeaseManager;

    @Mock
    private UserVmJoinDao userVmJoinDao;

    @Mock
    MessageBus messageBus;

    @Mock
    private VMInstanceDetailsDao vmInstanceDetailsDao;

    @Mock
    private AsyncJobManager asyncJobManager;

    @Mock
    private AsyncJobDispatcher asyncJobDispatcher;

    @Mock
    private GlobalLock globalLock;

    @Before
    public void setUp() {
        vmLeaseManager.setAsyncJobDispatcher(asyncJobDispatcher);
        when(asyncJobDispatcher.getName()).thenReturn("AsyncJobDispatcher");
        when(asyncJobManager.submitAsyncJob(any(AsyncJobVO.class))).thenReturn(1L);
        doNothing().when(vmInstanceDetailsDao).addDetail(
                anyLong(), anyString(), anyString(), anyBoolean()
        );
        try {
            vmLeaseManager.configure("VMLeaseManagerImpl", new HashMap<>());
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Test
    public void testReallyRunNoExpiredInstances() {
        when(userVmJoinDao.listEligibleInstancesWithExpiredLease()).thenReturn(new ArrayList<>());
        vmLeaseManager.reallyRun();
        verify(asyncJobManager, never()).submitAsyncJob(any(AsyncJobVO.class));
    }

    @Test
    public void testReallyRunWithDeleteProtection() {
        UserVmJoinVO vm = createMockVm(1L, VM_UUID, VM_NAME, VirtualMachine.State.Running, true);
        when(vm.getLeaseExpiryAction()).thenReturn("DESTROY");
        List<UserVmJoinVO> expiredVms = Arrays.asList(vm);
        when(userVmJoinDao.listEligibleInstancesWithExpiredLease()).thenReturn(expiredVms);
        vmLeaseManager.reallyRun();
        // Verify no jobs were submitted because of delete protection
        verify(asyncJobManager, never()).submitAsyncJob(any(AsyncJobVO.class));
    }

    @Test
    public void testReallyRunStopAction() {
        UserVmJoinVO vm = createMockVm(1L, VM_UUID, VM_NAME, VirtualMachine.State.Running, false);
        List<UserVmJoinVO> expiredVms = Arrays.asList(vm);
        when(userVmJoinDao.listEligibleInstancesWithExpiredLease()).thenReturn(expiredVms);
        when(userVmJoinDao.findById(1L)).thenReturn(vm);
        doReturn(1L).when(vmLeaseManager).executeStopInstanceJob(eq(vm), anyLong());
        try (MockedStatic<ActionEventUtils> utilities = Mockito.mockStatic(ActionEventUtils.class)) {
            utilities.when(() -> ActionEventUtils.onStartedActionEvent(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(),
                    Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyLong())).thenReturn(1L);

            vmLeaseManager.reallyRun();
        }
        verify(vmLeaseManager).executeStopInstanceJob(eq(vm), anyLong());
    }

    @Test
    public void testReallyRunDestroyAction() {
        UserVmJoinVO vm = createMockVm(1L, VM_UUID, VM_NAME, VirtualMachine.State.Running, false, DESTROY);
        List<UserVmJoinVO> expiredVms = Arrays.asList(vm);
        when(userVmJoinDao.listEligibleInstancesWithExpiredLease()).thenReturn(expiredVms);
        when(userVmJoinDao.findById(1L)).thenReturn(vm);
        doReturn(1L).when(vmLeaseManager).executeDestroyInstanceJob(eq(vm), anyLong());
        try (MockedStatic<ActionEventUtils> utilities = Mockito.mockStatic(ActionEventUtils.class)) {
            utilities.when(() -> ActionEventUtils.onStartedActionEvent(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(),
                    Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyLong())).thenReturn(1L);
            vmLeaseManager.reallyRun();
        }
        verify(vmLeaseManager).executeDestroyInstanceJob(eq(vm), anyLong());
    }

    @Test
    public void testExecuteExpiryActionStop() {
        UserVmJoinVO vm = createMockVm(1L, VM_UUID, VM_NAME, VirtualMachine.State.Running, false);
        doReturn(1L).when(vmLeaseManager).executeStopInstanceJob(eq(vm), eq(123L));
        Long jobId = vmLeaseManager.executeExpiryAction(vm, VMLeaseManager.ExpiryAction.STOP, 123L);
        assertNotNull(jobId);
        assertEquals(1L, jobId.longValue());
        verify(vmLeaseManager).executeStopInstanceJob(eq(vm), eq(123L));
    }

    @Test
    public void testExecuteExpiryActionDestroy() {
        UserVmJoinVO vm = createMockVm(1L, VM_UUID, VM_NAME, VirtualMachine.State.Running, false, DESTROY);
        doReturn(1L).when(vmLeaseManager).executeDestroyInstanceJob(eq(vm), eq(123L));
        Long jobId = vmLeaseManager.executeExpiryAction(vm, VMLeaseManager.ExpiryAction.DESTROY, 123L);
        assertNotNull(jobId);
        assertEquals(1L, jobId.longValue());
        verify(vmLeaseManager).executeDestroyInstanceJob(eq(vm), eq(123L));
    }

    @Test
        public void testExecuteStopInstanceJob() {
        UserVmJoinVO vm = createMockVm(1L, VM_UUID, VM_NAME, VirtualMachine.State.Running, false);
        // Mock the static ComponentContext
        try (MockedStatic<ComponentContext> mockedComponentContext = Mockito.mockStatic(ComponentContext.class)) {
            ApplicationContext mockAppContext = mock(ApplicationContext.class);
            mockedComponentContext.when(ComponentContext::getApplicationContext).thenReturn(mockAppContext);
            mockedComponentContext.when(() -> ComponentContext.inject(any())).thenReturn(true);
            long jobId = vmLeaseManager.executeStopInstanceJob(vm, 123L);
            assertEquals(1L, jobId);
            ArgumentCaptor<AsyncJobVO> jobCaptor = ArgumentCaptor.forClass(AsyncJobVO.class);
            verify(asyncJobManager).submitAsyncJob(jobCaptor.capture());
            AsyncJobVO capturedJob = jobCaptor.getValue();
            assertEquals(User.UID_SYSTEM, capturedJob.getUserId());
            assertEquals(vm.getAccountId(), capturedJob.getAccountId());
            assertEquals(StopVMCmd.class.getName(), capturedJob.getCmd());
            assertEquals(vm.getId(), capturedJob.getInstanceId().longValue());
            assertEquals("AsyncJobDispatcher", capturedJob.getDispatcher());
        }
    }

    @Test
    public void testExecuteDestroyInstanceJob() {
        UserVmJoinVO vm = createMockVm(1L, VM_UUID, VM_NAME, VirtualMachine.State.Running, false, DESTROY);
        try (MockedStatic<ComponentContext> mockedComponentContext = Mockito.mockStatic(ComponentContext.class)) {
            ApplicationContext mockAppContext = mock(ApplicationContext.class);
            mockedComponentContext.when(ComponentContext::getApplicationContext).thenReturn(mockAppContext);
            mockedComponentContext.when(() -> ComponentContext.inject(any())).thenReturn(true);
            long jobId = vmLeaseManager.executeDestroyInstanceJob(vm, 123L);
            assertEquals(1L, jobId);
            ArgumentCaptor<AsyncJobVO> jobCaptor = ArgumentCaptor.forClass(AsyncJobVO.class);
            verify(asyncJobManager).submitAsyncJob(jobCaptor.capture());
            AsyncJobVO capturedJob = jobCaptor.getValue();
            assertEquals(User.UID_SYSTEM, capturedJob.getUserId());
            assertEquals(vm.getAccountId(), capturedJob.getAccountId());
            assertEquals(DestroyVMCmd.class.getName(), capturedJob.getCmd());
            assertEquals(vm.getId(), capturedJob.getInstanceId().longValue());
            assertEquals("AsyncJobDispatcher", capturedJob.getDispatcher());
        }
    }

    @Test
    public void testGetLeaseExpiryAction() {
        UserVmJoinVO vm = createMockVm(1L, VM_UUID, VM_NAME, VirtualMachine.State.Running, false);
        VMLeaseManager.ExpiryAction action = vmLeaseManager.getLeaseExpiryAction(vm);
        assertEquals(VMLeaseManager.ExpiryAction.STOP, action);
    }

    @Test
    public void testGetLeaseExpiryActionNoAction() {
        UserVmJoinVO vm = createMockVm(1L, VM_UUID, VM_NAME, VirtualMachine.State.Running, false);
        when(vm.getLeaseExpiryAction()).thenReturn(null);
        vm.setLeaseExpiryAction(null);
        assertNull(vmLeaseManager.getLeaseExpiryAction(vm));
    }

    @Test
    public void testGetLeaseExpiryInvalidAction() {
        UserVmJoinVO vm = createMockVm(1L, VM_UUID, VM_NAME, VirtualMachine.State.Running, false);
        when(vm.getLeaseExpiryAction()).thenReturn("Unknown");
        assertNull(vmLeaseManager.getLeaseExpiryAction(vm));
    }

    @Test
    public void testGetComponentName() {
        assertEquals(vmLeaseManager.getConfigComponentName(), "VMLeaseManager");
    }

    @Test
    public void testConfigKeys() {
        assertEquals(vmLeaseManager.getConfigKeys().length, 4);
    }

    @Test
    public void testConfigure() throws Exception {
        overrideDefaultConfigValue(VMLeaseManager.InstanceLeaseEnabled, "true");
        vmLeaseManager.configure("VMLeaseManagerImpl", new HashMap<>());
    }

    @Test
    public void testStopShouldShutdownExecutors() {
        assertTrue(vmLeaseManager.stop());
    }

    @Test
    public void testCancelLeaseOnExistingInstances() {
        UserVmJoinVO vm = createMockVm(1L, VM_UUID, VM_NAME, VirtualMachine.State.Running, true);
        when(userVmJoinDao.listLeaseInstancesExpiringInDays(-1)).thenReturn(List.of(vm));
        try (MockedStatic<ActionEventUtils> utilities = Mockito.mockStatic(ActionEventUtils.class)) {
            utilities.when(() -> ActionEventUtils.onStartedActionEvent(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(),
                    Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyLong())).thenReturn(1L);
            vmLeaseManager.cancelLeaseOnExistingInstances();
            verify(vmInstanceDetailsDao).addDetail(1L, VmDetailConstants.INSTANCE_LEASE_EXECUTION, VMLeaseManager.LeaseActionExecution.CANCELLED.name(), false);
        }
    }

    @Test
    public void testOnLeaseFeatureToggleEnabled() throws Exception {
        overrideDefaultConfigValue(VMLeaseManager.InstanceLeaseEnabled, "true");
        vmLeaseManager.onLeaseFeatureToggle();
    }

    @Test
    public void testOnLeaseFeatureToggleDisabled() throws Exception {
        overrideDefaultConfigValue(VMLeaseManager.InstanceLeaseEnabled, "false");
        vmLeaseManager.onLeaseFeatureToggle();
    }

    private UserVmJoinVO createMockVm(Long id, String uuid, String name, VirtualMachine.State state, boolean deleteProtection) {
        return createMockVm(id, uuid, name, state, deleteProtection, "STOP");
    }

    // Helper method to create mock VMs
    private UserVmJoinVO createMockVm(Long id, String uuid, String name, VirtualMachine.State state, boolean deleteProtection, String expiryAction) {
        UserVmJoinVO vm = mock(UserVmJoinVO.class);
        when(vm.getId()).thenReturn(id);
        when(vm.getUuid()).thenReturn(uuid);
        when(vm.isDeleteProtection()).thenReturn(deleteProtection);
        when(vm.getAccountId()).thenReturn(1L);
        when(vm.getLeaseExpiryAction()).thenReturn(expiryAction);
        return vm;
    }

    private void overrideDefaultConfigValue(final ConfigKey configKey, final String value) throws IllegalAccessException, NoSuchFieldException {
        final Field f = ConfigKey.class.getDeclaredField("_defaultValue");
        f.setAccessible(true);
        f.set(configKey, value);
    }
}
