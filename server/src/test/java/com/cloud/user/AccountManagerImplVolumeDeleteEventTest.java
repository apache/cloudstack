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
package com.cloud.user;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.engine.cloud.entity.api.VirtualMachineEntity;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.domain.DomainVO;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.event.UsageEventVO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.CloudException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.VolumeVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmManagerImpl;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VmStatsDao;

@RunWith(MockitoJUnitRunner.class)
public class AccountManagerImplVolumeDeleteEventTest extends AccountManagetImplTestBase {

    private static final Long ACCOUNT_ID = 1l;
    private static final String VOLUME_UUID = "vol-111111";

    @Spy
    @InjectMocks
    private UserVmManagerImpl _vmMgr;

    @Mock
    private UserVmManager userVmManager;

    @Mock
    private VmStatsDao vmStatsDaoMock;

    Map<String, Object> oldFields = new HashMap<>();
    UserVmVO vm = mock(UserVmVO.class);

    // Configures the static fields of the UsageEventUtils class, Storing the
    // previous values to be restored during the cleanup phase, to avoid
    // interference with other unit tests.
    protected UsageEventUtils setupUsageUtils() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        _usageEventDao = new MockUsageEventDao();
        UsageEventUtils utils = new UsageEventUtils();

        List<String> usageUtilsFields = new ArrayList<String>();
        usageUtilsFields.add("usageEventDao");
        usageUtilsFields.add("accountDao");
        usageUtilsFields.add("dcDao");
        usageUtilsFields.add("configDao");

        for (String fieldName : usageUtilsFields) {
            try {
                Field f = UsageEventUtils.class.getDeclaredField(fieldName);
                f.setAccessible(true);
                Field staticField = UsageEventUtils.class.getDeclaredField("s_" + fieldName);
                staticField.setAccessible(true);
                oldFields.put(f.getName(), staticField.get(null));
                f.set(utils, this.getClass().getSuperclass().getDeclaredField("_" + fieldName).get(this));
            } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
                e.printStackTrace();
            }
        }
        Method method;
        method = UsageEventUtils.class.getDeclaredMethod("init");
        method.setAccessible(true);
        method.invoke(utils);
        return utils;
    }

    protected void defineMocksBehavior() throws AgentUnavailableException, ConcurrentOperationException, CloudException {

        AccountVO account = new AccountVO();
        account.setId(ACCOUNT_ID);
        when(_accountDao.remove(ACCOUNT_ID)).thenReturn(true);
        when(_accountDao.findById(ACCOUNT_ID)).thenReturn(account);

        DomainVO domain = new DomainVO();
        VirtualMachineEntity vmEntity = mock(VirtualMachineEntity.class);

        when(_orchSrvc.getVirtualMachine(nullable(String.class))).thenReturn(vmEntity);
        when(vmEntity.destroy(nullable(Boolean.class))).thenReturn(true);

        Mockito.lenient().doReturn(vm).when(_vmDao).findById(nullable(Long.class));

        VolumeVO vol = new VolumeVO(VOLUME_UUID, 1l, 1l, 1l, 1l, 1l, "folder", "path", null, 50, Type.ROOT);
        vol.setDisplayVolume(true);
        List<VolumeVO> volumes = new ArrayList<>();
        volumes.add(vol);

        lenient().when(securityChecker.checkAccess(Mockito.eq(account), nullable(ControlledEntity.class), nullable(AccessType.class), nullable(String.class))).thenReturn(true);


        when(_userVmDao.findById(nullable(Long.class))).thenReturn(vm);
        lenient().when(_userVmDao.listByAccountId(ACCOUNT_ID)).thenReturn(Arrays.asList(vm));
        lenient().when(_userVmDao.findByUuid(nullable(String.class))).thenReturn(vm);

        when(_volumeDao.findByInstance(nullable(Long.class))).thenReturn(volumes);

        ServiceOfferingVO offering = mock(ServiceOfferingVO.class);
        lenient().when(offering.getCpu()).thenReturn(500);
        lenient().when(offering.getId()).thenReturn(1l);
        when(offering.getCpu()).thenReturn(500);
        when(offering.getRamSize()).thenReturn(500);
        when(serviceOfferingDao.findByIdIncludingRemoved(nullable(Long.class), nullable(Long.class))).thenReturn(offering);

        lenient().when(_domainMgr.getDomain(nullable(Long.class))).thenReturn(domain);

        Mockito.lenient().doReturn(true).when(_vmMgr).expunge(any(UserVmVO.class));

    }

    @Before
    public void init() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, AgentUnavailableException,
    ConcurrentOperationException, CloudException {

        setupUsageUtils();
        defineMocksBehavior();
    }

    @After
    // Restores static fields of the UsageEventUtil class to previous values.
    public void cleanupUsageUtils() throws ReflectiveOperationException, SecurityException {

        UsageEventUtils utils = new UsageEventUtils();
        for (String fieldName : oldFields.keySet()) {
            Field f = UsageEventUtils.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(utils, oldFields.get(fieldName));
        }
        Method method = UsageEventUtils.class.getDeclaredMethod("init");
        method.setAccessible(true);
        method.invoke(utils);
    }

    protected List<UsageEventVO> deleteUserAccountRootVolumeUsageEvents(boolean vmDestroyedPrior) throws AgentUnavailableException, ConcurrentOperationException, CloudException {

        when(vm.getState()).thenReturn(vmDestroyedPrior ? VirtualMachine.State.Destroyed : VirtualMachine.State.Running);
        lenient().when(vm.getRemoved()).thenReturn(vmDestroyedPrior ? new Date() : null);
        Mockito.doNothing().when(accountManagerImpl).checkAccess(nullable(Account.class), Mockito.isNull(), nullable(Boolean.class), nullable(Account.class));
        accountManagerImpl.deleteUserAccount(ACCOUNT_ID);

        return _usageEventDao.listAll();
    }

    @Test
    // If the VM is already destroyed, no events should get emitted
    public void destroyedVMRootVolumeUsageEvent()
            throws SecurityException, IllegalArgumentException, ReflectiveOperationException, AgentUnavailableException, ConcurrentOperationException, CloudException {
        Mockito.lenient().doReturn(vm).when(_vmMgr).destroyVm(nullable(Long.class), nullable(Boolean.class));
        List<UsageEventVO> emittedEvents = deleteUserAccountRootVolumeUsageEvents(true);
        Assert.assertEquals(0, emittedEvents.size());
    }

    @Test
    // If the VM is running, we should see one emitted event for the root
    // volume.
    public void runningVMRootVolumeUsageEvent()
            throws SecurityException, IllegalArgumentException, ReflectiveOperationException, AgentUnavailableException, ConcurrentOperationException, CloudException {
        Mockito.doNothing().when(vmStatsDaoMock).removeAllByVmId(Mockito.anyLong());
        Mockito.lenient().when(_vmMgr.destroyVm(nullable(Long.class), nullable(Boolean.class))).thenReturn(vm);
        List<UsageEventVO> emittedEvents = deleteUserAccountRootVolumeUsageEvents(false);
        UsageEventVO event = emittedEvents.get(0);
        Assert.assertEquals(EventTypes.EVENT_VOLUME_DELETE, event.getType());
        Assert.assertEquals(VOLUME_UUID, event.getResourceName());

    }
}
