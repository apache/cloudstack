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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;

import org.apache.cloudstack.api.command.admin.vm.ScaleVMCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vm.UpgradeVMCmdByAdmin;
import org.apache.cloudstack.api.command.user.vm.ScaleVMCmd;
import org.apache.cloudstack.api.command.user.vm.UpgradeVMCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import com.cloud.api.ApiDBUtils;
import com.cloud.capacity.CapacityManager;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.user.dao.AccountDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.impl.ConfigDepotImpl;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.AccountVO;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.class)
public class UserVmOfferingChangeTest {
    @Spy @InjectMocks private UserVmManagerImpl manager = new UserVmManagerImpl();
    @Mock private EntityManager _entityMgr;
    @Mock private UserVmDao _vmDao;
    @Mock private VMInstanceDao _vmInstanceDao;
    @Mock private AccountManager _accountMgr;
    @Mock private ServiceOfferingDao serviceOfferingDao;
    @Mock private VirtualMachineManager _itMgr;
    @Mock private ResourceLimitService _resourceLimitMgr;
    @Mock private VMTemplateDao _templateDao;
    @Mock private DataCenterDao _dcDao;
    @Mock private DiskOfferingDao _diskOfferingDao;
    @Mock private VolumeDao _volsDao;
    @Mock private VolumeService _volService;
    @Mock private AccountDao _accountDao;
    @Mock private HostDao _hostDao;
    @Mock private CapacityManager _capacityMgr;
    @Mock private KubernetesServiceHelper helper;
    @Mock private UserVmVO vm;
    @Mock private AccountVO account;
    private MockedStatic<ComponentContext> components;

    @Before
    public void setup() {
        CallContext.register(mock(User.class), account);
        components = mockStatic(ComponentContext.class);
        components.when(() -> ComponentContext.getDelegateComponentOfType(KubernetesServiceHelper.class)).thenReturn(helper);
    }

    @After
    public void cleanup() {
        if (components != null) {
            components.close();
        }
        CallContext.unregister();
    }

    private void scaleApiBlocked(ScaleVMCmd cmd, VirtualMachine.State state) throws Exception {
        lenient().when(vm.getState()).thenReturn(state);
        when(cmd.getId()).thenReturn(1L);
        when(_entityMgr.findById(VirtualMachine.class, 1L)).thenReturn(vm);
        when(vm.getHypervisorType()).thenReturn(HypervisorType.KVM);
        when(_vmDao.findById(1L)).thenReturn(vm);
        doThrow(new CloudRuntimeException("Use cluster scaling")).when(helper).checkVmServiceOfferingCanBeChanged(vm);
        assertThrows(CloudRuntimeException.class, () -> manager.upgradeVirtualMachine(cmd));
        verify(_accountMgr).checkAccess(account, null, true, vm);
        verify(manager, never()).updateInstanceDetailsMapWithCurrentValuesForAbsentDetails(anyMap(), any(), any());
        verify(manager, never()).upgradeVirtualMachine(anyLong(), anyLong(), anyMap());
        verifyNoInteractions(_itMgr, serviceOfferingDao);
    }

    @Test public void blocksRunningScaleApi() throws Exception {
        scaleApiBlocked(mock(ScaleVMCmd.class), VirtualMachine.State.Running);
    }
    @Test public void blocksStoppedScaleApi() throws Exception {
        scaleApiBlocked(mock(ScaleVMCmd.class), VirtualMachine.State.Stopped);
    }
    @Test public void blocksAdminScaleApi() throws Exception {
        scaleApiBlocked(mock(ScaleVMCmdByAdmin.class), VirtualMachine.State.Running);
    }

    private void deprecatedApiBlocked(UpgradeVMCmd cmd) throws Exception {
        when(cmd.getId()).thenReturn(1L);
        when(_vmInstanceDao.findById(1L)).thenReturn(vm);
        when(vm.getState()).thenReturn(VirtualMachine.State.Stopped);
        when(_vmDao.findById(1L)).thenReturn(vm);
        doThrow(new CloudRuntimeException("Use cluster scaling")).when(helper).checkVmServiceOfferingCanBeChanged(vm);
        assertThrows(CloudRuntimeException.class, () -> manager.upgradeVirtualMachine(cmd));
        verify(_accountMgr).checkAccess(account, null, true, vm);
        verifyNoInteractions(_itMgr, serviceOfferingDao);
    }

    @Test public void blocksDeprecatedApi() throws Exception {
        deprecatedApiBlocked(mock(UpgradeVMCmd.class));
    }
    @Test public void blocksAdminDeprecatedApi() throws Exception {
        deprecatedApiBlocked(mock(UpgradeVMCmdByAdmin.class));
    }

    @Test public void allowsScaleApiWhenPluginPermits() throws Exception {
        ScaleVMCmd cmd = mock(ScaleVMCmd.class);
        HashMap<String, String> details = new HashMap<>();
        when(cmd.getId()).thenReturn(1L);
        when(cmd.getServiceOfferingId()).thenReturn(2L);
        when(cmd.getDetails()).thenReturn(details);
        when(_entityMgr.findById(VirtualMachine.class, 1L)).thenReturn(vm);
        when(vm.getHypervisorType()).thenReturn(HypervisorType.KVM);
        when(_vmDao.findById(1L)).thenReturn(vm);
        doNothing().when(manager).updateInstanceDetailsMapWithCurrentValuesForAbsentDetails(details, vm, 2L);
        doReturn(true).when(manager).upgradeVirtualMachine(1L, 2L, details);
        when(vm.getState()).thenReturn(VirtualMachine.State.Running);
        assertSame(vm, manager.upgradeVirtualMachine(cmd));
        verify(helper).checkVmServiceOfferingCanBeChanged(vm);
    }

    @Test public void allowsMissingKubernetesPlugin() {
        components.when(() -> ComponentContext.getDelegateComponentOfType(KubernetesServiceHelper.class))
                .thenThrow(new NoSuchBeanDefinitionException(KubernetesServiceHelper.class));
        manager.checkPluginsIfVmServiceOfferingCanBeChanged(vm);
    }

    @Test public void internalStoppedResizeDoesNotInvokeApiGuard() throws Exception {
        when(_vmInstanceDao.findById(1L)).thenReturn(vm);
        when(vm.getId()).thenReturn(1L);
        when(vm.getState()).thenReturn(VirtualMachine.State.Stopped);
        when(vm.getServiceOfferingId()).thenReturn(2L);
        ServiceOfferingVO oldOffering = mock(ServiceOfferingVO.class);
        ServiceOfferingVO newOffering = mock(ServiceOfferingVO.class);
        when(serviceOfferingDao.findById(3L)).thenReturn(newOffering);
        when(serviceOfferingDao.findByIdIncludingRemoved(1L, 2L)).thenReturn(oldOffering);
        when(newOffering.getCpu()).thenReturn(3);
        when(newOffering.getRamSize()).thenReturn(3072);
        when(oldOffering.getCpu()).thenReturn(2);
        when(oldOffering.getRamSize()).thenReturn(2048);
        when(_accountMgr.getActiveAccountById(anyLong())).thenReturn(account);
        doNothing().when(manager).validateDiskOfferingChecks(oldOffering, newOffering);
        lenient().when(_volsDao.findReadyAndAllocatedRootVolumesByInstance(1L)).thenReturn(Collections.emptyList());
        assertTrue(manager.upgradeVirtualMachine(1L, 3L, new HashMap<>()));
        verify(_itMgr).upgradeVmDb(1L, newOffering, oldOffering);
        verifyNoInteractions(helper);
        components.verifyNoInteractions();
    }

    @Test public void internalLiveResizeDoesNotInvokeApiGuard() throws Exception {
        Field depotField = ConfigKey.class.getDeclaredField("s_depot");
        depotField.setAccessible(true);
        ConfigDepotImpl originalDepot = (ConfigDepotImpl) depotField.get(null);
        ConfigDepotImpl depot = mock(ConfigDepotImpl.class);
        when(depot.getConfigStringValue(eq(UserVmManager.EnableDynamicallyScaleVm.key()), any(), any()))
                .thenReturn("true");
        Field retryField = UserVmManagerImpl.class.getDeclaredField("_scaleRetry");
        retryField.setAccessible(true);
        retryField.set(manager, 1);
        ConfigKey.init(depot);
        try (MockedStatic<ApiDBUtils> apiDb = mockStatic(ApiDBUtils.class)) {
            HostVO host = mock(HostVO.class);
            when(_vmInstanceDao.findById(1L)).thenReturn(vm);
            when(vm.getId()).thenReturn(1L);
            when(vm.getState()).thenReturn(VirtualMachine.State.Running);
            when(vm.getHypervisorType()).thenReturn(HypervisorType.KVM);
            when(vm.getServiceOfferingId()).thenReturn(2L);
            when(vm.isDynamicallyScalable()).thenReturn(true);
            when(_accountDao.findById(anyLong())).thenReturn(account);
            when(_hostDao.findById(anyLong())).thenReturn(host);
            apiDb.when(() -> ApiDBUtils.findHostById(vm.getHostId())).thenReturn(host);
            ServiceOfferingVO oldOffering = mock(ServiceOfferingVO.class);
            ServiceOfferingVO newOffering = mock(ServiceOfferingVO.class);
            when(serviceOfferingDao.findById(3L)).thenReturn(newOffering);
            when(serviceOfferingDao.findByIdIncludingRemoved(1L, 2L)).thenReturn(oldOffering);
            when(newOffering.getCpu()).thenReturn(3);
            when(newOffering.getRamSize()).thenReturn(3072);
            when(newOffering.getSpeed()).thenReturn(1000);
            when(newOffering.getDetails()).thenReturn(Collections.emptyMap());
            when(oldOffering.getCpu()).thenReturn(2);
            when(oldOffering.getRamSize()).thenReturn(2048);
            when(oldOffering.getSpeed()).thenReturn(1000);
            when(oldOffering.getDetails()).thenReturn(Collections.emptyMap());
            when(host.checkHostServiceOfferingAndTemplateTags(eq(newOffering), any(), any())).thenReturn(true);
            when(_capacityMgr.checkIfHostHasCpuCapability(host, 3, 1000)).thenReturn(true);
            when(_capacityMgr.checkIfHostHasCapacity(eq(host), any(), anyLong(), eq(false), anyFloat(), anyFloat(), eq(false)))
                    .thenReturn(true);
            doReturn(true).when(manager).checkEnforceStrictHostTagCheck(vm, host);
            doNothing().when(manager).validateDiskOfferingChecks(oldOffering, newOffering);
            lenient().when(_volsDao.findReadyAndAllocatedRootVolumesByInstance(1L)).thenReturn(Collections.emptyList());
            HashMap<String, String> details = new HashMap<>();
            assertTrue(manager.upgradeVirtualMachine(1L, 3L, details));
            verify(_itMgr).reConfigureVm(vm.getUuid(), oldOffering, newOffering, details, true);
            verifyNoInteractions(helper);
            components.verifyNoInteractions();
        } finally {
            ConfigKey.init(originalDepot);
        }
    }

}
