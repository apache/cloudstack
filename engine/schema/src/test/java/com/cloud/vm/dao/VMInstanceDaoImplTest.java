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

package com.cloud.vm.dao;

import static com.cloud.vm.VirtualMachine.State.Running;
import static com.cloud.vm.VirtualMachine.State.Stopped;
import static com.cloud.vm.dao.VMInstanceDaoImpl.MAX_CONSECUTIVE_SAME_STATE_UPDATE_COUNT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;

/**
 * Created by sudharma_jain on 3/2/17.
 */

public class VMInstanceDaoImplTest {

    @Spy
    VMInstanceDaoImpl vmInstanceDao = new VMInstanceDaoImpl();

    @Mock
    VMInstanceVO vm;

    private AutoCloseable closeable;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        Long hostId = null;
        when(vm.getHostId()).thenReturn(hostId);
        when(vm.getUpdated()).thenReturn(5L);
        when(vm.getUpdateTime()).thenReturn(DateTime.now().toDate());
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testUpdateState() {
        Long destHostId = null;
        Pair<Long, Long> opaqueMock = new Pair<>(1L, destHostId);
        vmInstanceDao.updateState(Stopped, VirtualMachine.Event.FollowAgentPowerOffReport, Stopped, vm , opaqueMock);
    }

    @Test
    public void testIfStateAndHostUnchanged() {
        assertTrue(vmInstanceDao.ifStateUnchanged(Stopped, Stopped, null, null));
        assertFalse(vmInstanceDao.ifStateUnchanged(Stopped, Running, null, null));
    }

    @Test
    public void testUpdatePowerStateDifferentPowerState() {
        when(vm.getPowerStateUpdateTime()).thenReturn(null);
        when(vm.getPowerHostId()).thenReturn(1L);
        when(vm.getPowerState()).thenReturn(VirtualMachine.PowerState.PowerOn);
        doReturn(vm).when(vmInstanceDao).findById(anyLong());
        doReturn(true).when(vmInstanceDao).update(anyLong(), any());

        boolean result = vmInstanceDao.updatePowerState(1L, 1L, VirtualMachine.PowerState.PowerOff, new Date());

        verify(vm, times(1)).setPowerState(VirtualMachine.PowerState.PowerOff);
        verify(vm, times(1)).setPowerHostId(1L);
        verify(vm, times(1)).setPowerStateUpdateCount(1);
        verify(vm, times(1)).setPowerStateUpdateTime(any(Date.class));

        assertTrue(result);
    }

    @Test
    public void testUpdatePowerStateVmNotFound() {
        when(vm.getPowerStateUpdateTime()).thenReturn(null);
        when(vm.getPowerHostId()).thenReturn(1L);
        when(vm.getPowerState()).thenReturn(VirtualMachine.PowerState.PowerOn);
        doReturn(null).when(vmInstanceDao).findById(anyLong());

        boolean result = vmInstanceDao.updatePowerState(1L, 1L, VirtualMachine.PowerState.PowerOff, new Date());

        verify(vm, never()).setPowerState(any());
        verify(vm, never()).setPowerHostId(anyLong());
        verify(vm, never()).setPowerStateUpdateCount(any(Integer.class));
        verify(vm, never()).setPowerStateUpdateTime(any(Date.class));

        assertFalse(result);
    }

    @Test
    public void testUpdatePowerStateNoChangeFirstUpdate() {
        when(vm.getPowerStateUpdateTime()).thenReturn(null);
        when(vm.getPowerHostId()).thenReturn(1L);
        when(vm.getPowerState()).thenReturn(VirtualMachine.PowerState.PowerOn);
        when(vm.getState()).thenReturn(Running);
        when(vm.getPowerStateUpdateCount()).thenReturn(1);
        doReturn(vm).when(vmInstanceDao).findById(anyLong());
        doReturn(true).when(vmInstanceDao).update(anyLong(), any());

        boolean result = vmInstanceDao.updatePowerState(1L, 1L, VirtualMachine.PowerState.PowerOn, new Date());

        verify(vm, never()).setPowerState(any());
        verify(vm, never()).setPowerHostId(anyLong());
        verify(vm, times(1)).setPowerStateUpdateCount(2);
        verify(vm, times(1)).setPowerStateUpdateTime(any(Date.class));

        assertTrue(result);
    }

    @Test
    public void testUpdatePowerStateNoChangeMaxUpdatesValidState() {
        when(vm.getPowerStateUpdateTime()).thenReturn(null);
        when(vm.getPowerHostId()).thenReturn(1L);
        when(vm.getPowerState()).thenReturn(VirtualMachine.PowerState.PowerOn);
        when(vm.getPowerStateUpdateCount()).thenReturn(MAX_CONSECUTIVE_SAME_STATE_UPDATE_COUNT);
        when(vm.getState()).thenReturn(Running);
        doReturn(vm).when(vmInstanceDao).findById(anyLong());
        doReturn(true).when(vmInstanceDao).update(anyLong(), any());

        boolean result = vmInstanceDao.updatePowerState(1L, 1L, VirtualMachine.PowerState.PowerOn, new Date());

        verify(vm, never()).setPowerState(any());
        verify(vm, never()).setPowerHostId(anyLong());
        verify(vm, never()).setPowerStateUpdateCount(any(Integer.class));
        verify(vm, never()).setPowerStateUpdateTime(any(Date.class));

        assertFalse(result);
    }

    @Test
    public void testUpdatePowerStateNoChangeMaxUpdatesInvalidStateVmStopped() {
        when(vm.getPowerStateUpdateTime()).thenReturn(null);
        when(vm.getPowerHostId()).thenReturn(1L);
        when(vm.getPowerState()).thenReturn(VirtualMachine.PowerState.PowerOn);
        when(vm.getPowerStateUpdateCount()).thenReturn(MAX_CONSECUTIVE_SAME_STATE_UPDATE_COUNT);
        when(vm.getState()).thenReturn(Stopped);
        doReturn(vm).when(vmInstanceDao).findById(anyLong());
        doReturn(true).when(vmInstanceDao).update(anyLong(), any());

        boolean result = vmInstanceDao.updatePowerState(1L, 1L, VirtualMachine.PowerState.PowerOn, new Date());

        verify(vm, times(1)).setPowerState(any());
        verify(vm, times(1)).setPowerHostId(anyLong());
        verify(vm, times(1)).setPowerStateUpdateCount(1);
        verify(vm, times(1)).setPowerStateUpdateTime(any(Date.class));

        assertTrue(result);
    }

    @Test
    public void testUpdatePowerStateNoChangeMaxUpdatesInvalidStateVmRunning() {
        when(vm.getPowerStateUpdateTime()).thenReturn(null);
        when(vm.getPowerHostId()).thenReturn(1L);
        when(vm.getPowerState()).thenReturn(VirtualMachine.PowerState.PowerOff);
        when(vm.getPowerStateUpdateCount()).thenReturn(MAX_CONSECUTIVE_SAME_STATE_UPDATE_COUNT);
        when(vm.getState()).thenReturn(Running);
        doReturn(vm).when(vmInstanceDao).findById(anyLong());
        doReturn(true).when(vmInstanceDao).update(anyLong(), any());

        boolean result = vmInstanceDao.updatePowerState(1L, 1L, VirtualMachine.PowerState.PowerOff, new Date());

        verify(vm, times(1)).setPowerState(any());
        verify(vm, times(1)).setPowerHostId(anyLong());
        verify(vm, times(1)).setPowerStateUpdateCount(1);
        verify(vm, times(1)).setPowerStateUpdateTime(any(Date.class));

        assertTrue(result);
    }

    @Test
    public void testSearchRemovedByRemoveDate() {
        SearchBuilder<VMInstanceVO> sb = Mockito.mock(SearchBuilder.class);
        SearchCriteria<VMInstanceVO> sc = Mockito.mock(SearchCriteria.class);
        Mockito.when(sb.create()).thenReturn(sc);
        Mockito.when(vmInstanceDao.createSearchBuilder()).thenReturn(sb);
        final VMInstanceVO mockedVO = Mockito.mock(VMInstanceVO.class);
        Mockito.when(sb.entity()).thenReturn(mockedVO);
        Mockito.doReturn(new ArrayList<>()).when(vmInstanceDao).searchIncludingRemoved(
                Mockito.any(SearchCriteria.class), Mockito.any(Filter.class), Mockito.eq(null),
                Mockito.eq(false));
        Calendar cal = Calendar.getInstance();
        Date endDate = new Date();
        cal.setTime(endDate);
        cal.add(Calendar.DATE, -1 * 10);
        Date startDate = cal.getTime();
        vmInstanceDao.searchRemovedByRemoveDate(startDate, endDate, 50L, new ArrayList<>());
        Mockito.verify(sc).setParameters("startDate", startDate);
        Mockito.verify(sc).setParameters("endDate", endDate);
        Mockito.verify(sc, Mockito.never()).setParameters(Mockito.eq("skippedVmIds"), Mockito.any());
        Mockito.verify(vmInstanceDao, Mockito.times(1)).searchIncludingRemoved(
                Mockito.any(SearchCriteria.class), Mockito.any(Filter.class), Mockito.eq(null),
                Mockito.eq(false));
    }
}
