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
package com.cloud.vm;

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.impl.ConfigDepotImpl;
import org.apache.cloudstack.resourcelimit.Reserver;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.api.ApiDBUtils;
import com.cloud.capacity.CapacityManager;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.VMInstanceDao;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserVmManagerLiveScaleTest {
    @Spy
    @InjectMocks
    private UserVmManagerImpl manager;
    @Mock
    private VirtualMachineManager vmManager;
    @Mock
    private VMInstanceDao vmDao;
    @Mock
    private ServiceOfferingDao offeringDao;
    @Mock
    private AccountDao accountDao;
    @Mock
    private AccountManager accountManager;
    @Mock
    private ResourceLimitService resourceLimits;
    @Mock
    private CapacityManager capacityManager;
    @Mock
    private HostDao hostDao;
    @Mock
    private VMTemplateDao templateDao;
    @Mock
    private DiskOfferingDao diskOfferingDao;
    @Mock
    private VMInstanceVO vm;
    @Mock
    private HostVO host;
    @Mock
    private Account caller;
    @Mock
    private AccountVO owner;
    @Mock
    private User user;
    @Mock
    private Runnable prepare;
    @Mock
    private Reserver reservation;
    @Mock
    private ConfigDepotImpl config;

    private final ServiceOfferingVO current = offering(2, 2048);
    private final ServiceOfferingVO target = offering(3, 3072);
    private MockedStatic<ApiDBUtils> db;

    @Before
    public void setUp() throws Exception {
        lenient().when(vmDao.findById(1L)).thenReturn(vm);
        lenient().when(vm.getId()).thenReturn(1L);
        lenient().when(vm.getUuid()).thenReturn("vm");
        lenient().when(vm.getAccountId()).thenReturn(8L);
        lenient().when(vm.getServiceOfferingId()).thenReturn(2L);
        lenient().when(vm.getHostId()).thenReturn(7L);
        lenient().when(vm.getState()).thenReturn(State.Running);
        lenient().when(vm.getHypervisorType()).thenReturn(HypervisorType.KVM);
        lenient().when(vm.isDynamicallyScalable()).thenReturn(true);
        lenient().when(caller.getAccountId()).thenReturn(17L);
        lenient().when(accountDao.findById(8L)).thenReturn(owner);
        lenient().when(offeringDao.findById(3L)).thenReturn(target);
        lenient().when(offeringDao.findByIdIncludingRemoved(1L, 2L)).thenReturn(current);
        lenient().doNothing().when(manager).validateDiskOfferingChecks(current, target);
        lenient().doReturn(true).when(manager).checkEnforceStrictHostTagCheck(vm, host);
        lenient().when(hostDao.findById(7L)).thenReturn(host);
        lenient().when(host.getId()).thenReturn(7L);
        lenient().when(host.getClusterId()).thenReturn(12L);
        lenient().when(host.checkHostServiceOfferingAndTemplateTags(eq(target), any(), any())).thenReturn(true);
        lenient().when(capacityManager.checkIfHostHasCpuCapability(host, 3, 1000)).thenReturn(true);
        lenient().when(capacityManager.checkIfHostHasCapacity(eq(host), anyInt(), anyLong(), anyBoolean(), anyFloat(), anyFloat(), anyBoolean()))
                .thenReturn(true);
        lenient().when(vmManager.reConfigureVm(eq("vm"), eq(current), eq(target), any(), anyBoolean())).thenReturn(vm);
        lenient().when(config.getConfigStringValue(eq(UserVmManager.EnableDynamicallyScaleVm.key()), any(), anyLong())).thenReturn("true");
        lenient().when(config.getConfigStringValue(eq(UserVmManager.AutoMigrateVmOnLiveScaleInsufficientCapacity.key()), any(), anyLong())).thenReturn("true");
        lenient().doAnswer(invocation -> {
            invocation.<List<Reserver>>getArgument(9).add(reservation);
            return null;
        }).when(resourceLimits).checkVmResourceLimitsForServiceOfferingChange(eq(owner), anyBoolean(), eq(2L), eq(3L),
                eq(2048L), eq(3072L), eq(current), eq(target), any(), anyList());
        ReflectionTestUtils.setField(manager, "_scaleRetry", 2);
        db = Mockito.mockStatic(ApiDBUtils.class);
        db.when(() -> ApiDBUtils.findHostById(7L)).thenReturn(host);
        ConfigKey.init(config);
        CallContext.register(user, caller);
    }

    @After
    public void tearDown() {
        CallContext.unregister();
        ConfigKey.init(null);
        if (db != null) {
            db.close();
        }
    }

    @Test
    public void testSuccessfulResizeKeepsResourceCountAndClosesReservations() throws Exception {
        Assert.assertTrue(resize());

        InOrder order = Mockito.inOrder(resourceLimits, prepare, vmManager, reservation);
        order.verify(resourceLimits).checkVmResourceLimitsForServiceOfferingChange(eq(owner), anyBoolean(), eq(2L), eq(3L),
                eq(2048L), eq(3072L), eq(current), eq(target), any(), anyList());
        order.verify(resourceLimits).updateVmResourceCountForServiceOfferingChange(eq(17L), anyBoolean(), eq(2L), eq(3L),
                eq(2048L), eq(3072L), eq(current), eq(target), any());
        order.verify(prepare).run();
        order.verify(vmManager).reConfigureVm("vm", current, target, Map.of(), true);
        order.verify(reservation).close();
        verifyRollback(0);
        verify(vmManager, never()).findHostAndMigrate(any(), anyLong(), any(), any());
    }

    @Test
    public void testResizeFailureRollsBackThenRetriesWithoutRepeatingPreparation() throws Exception {
        when(vmManager.reConfigureVm("vm", current, target, Map.of(), true))
                .thenThrow(new ResourceUnavailableException("host failed", HostVO.class, 7L));

        Assert.assertTrue(resize());

        InOrder order = Mockito.inOrder(prepare, vmManager);
        order.verify(prepare).run();
        order.verify(vmManager).reConfigureVm("vm", current, target, Map.of(), true);
        order.verify(vmManager).findHostAndMigrate(eq("vm"), eq(3L), any(), any());
        order.verify(vmManager).reConfigureVm("vm", current, target, Map.of(), false);
        verify(prepare, times(1)).run();
        verifyIncrement(2);
        verifyRollback(1);
        verify(reservation).close();
    }

    @Test
    public void testMigrationFailuresExhaustRetriesAndRollBackEveryAttempt() throws Exception {
        when(capacityManager.checkIfHostHasCpuCapability(host, 3, 1000)).thenReturn(false);
        doThrow(new InsufficientServerCapacityException("no host", 1L)).when(vmManager).findHostAndMigrate(eq("vm"), eq(3L), any(), any());

        Assert.assertFalse(resize());

        verify(vmManager, times(2)).findHostAndMigrate(eq("vm"), eq(3L), any(), any());
        verifyIncrement(2);
        verifyRollback(2);
        Mockito.verifyNoInteractions(prepare);
        verify(vmManager, never()).reConfigureVm(any(), any(), any(), any(), anyBoolean());
        verify(reservation).close();
    }

    @Test
    public void testNoCapacityWithMigrationDisabledRollsBackBeforeReturningFalse() throws Exception {
        when(capacityManager.checkIfHostHasCpuCapability(host, 3, 1000)).thenReturn(false);
        when(config.getConfigStringValue(eq(UserVmManager.AutoMigrateVmOnLiveScaleInsufficientCapacity.key()), any(), anyLong())).thenReturn("false");

        Assert.assertFalse(resize());

        verifyIncrement(1);
        verifyRollback(1);
        Mockito.verifyNoInteractions(prepare);
        verify(vmManager, never()).findHostAndMigrate(any(), anyLong(), any(), any());
        verify(reservation).close();
    }

    @Test
    public void testPreparationFailurePropagatesAndReleasesResourcesWithoutResize() throws Exception {
        CloudRuntimeException failure = new CloudRuntimeException("node preparation failed");
        doThrow(failure).when(prepare).run();

        Assert.assertSame(failure, Assert.assertThrows(CloudRuntimeException.class, this::resize));

        verifyIncrement(1);
        verifyRollback(1);
        verify(prepare).run();
        verify(vmManager, never()).reConfigureVm(any(), any(), any(), any(), anyBoolean());
        verify(reservation).close();
    }

    @Test
    public void testDisabledZoneClosesReservationsBeforeAnyAttempt() throws Exception {
        when(config.getConfigStringValue(eq(UserVmManager.EnableDynamicallyScaleVm.key()), any(), anyLong())).thenReturn("false");

        Assert.assertThrows(PermissionDeniedException.class, this::resize);

        verifyIncrement(0);
        verifyRollback(0);
        Mockito.verifyNoInteractions(prepare);
        verify(reservation).close();
    }

    @Test
    public void testThresholdRejectionClosesReservationsBeforeAnyAttempt() throws Exception {
        when(capacityManager.checkIfClusterCrossesThreshold(12L, 1000, 1024)).thenReturn(true);

        Assert.assertThrows(CloudRuntimeException.class, this::resize);

        verifyIncrement(0);
        Mockito.verifyNoInteractions(prepare);
        verify(reservation).close();
    }

    @Test
    public void testZeroRetriesClosesReservationsWithoutPreparationOrCountChanges() throws Exception {
        ReflectionTestUtils.setField(manager, "_scaleRetry", 0);

        Assert.assertFalse(resize());

        verifyIncrement(0);
        verifyRollback(0);
        Mockito.verifyNoInteractions(prepare);
        verify(reservation).close();
    }

    @Test
    public void testDownscaleIsRejectedBeforeResourceReservationOrPreparation() {
        target.setCpu(1);

        Assert.assertThrows(InvalidParameterValueException.class, this::resize);

        Mockito.verifyNoInteractions(resourceLimits, prepare, reservation);
    }

    private boolean resize() throws Exception {
        return manager.upgradeVirtualMachine(1L, 3L, Map.of(), prepare);
    }

    private void verifyIncrement(int count) {
        verify(resourceLimits, times(count)).updateVmResourceCountForServiceOfferingChange(eq(17L), anyBoolean(), eq(2L), eq(3L),
                eq(2048L), eq(3072L), eq(current), eq(target), any());
    }

    private void verifyRollback(int count) {
        verify(resourceLimits, times(count)).updateVmResourceCountForServiceOfferingChange(eq(17L), anyBoolean(), eq(3L), eq(2L),
                eq(3072L), eq(2048L), eq(target), eq(current), any());
    }

    private static ServiceOfferingVO offering(int cpu, int memory) {
        ServiceOfferingVO offering = new ServiceOfferingVO("test", cpu, memory, 1000, null, null, false, "test", false, null, false);
        offering.setDetails(Map.of());
        return offering;
    }
}
