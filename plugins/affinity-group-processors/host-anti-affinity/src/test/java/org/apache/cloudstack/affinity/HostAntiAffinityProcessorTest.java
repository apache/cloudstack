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
package org.apache.cloudstack.affinity;

import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.utils.DateUtil;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class HostAntiAffinityProcessorTest {

    private static final long AFFINITY_GROUP_ID = 2L;
    private static final long VM_ID = 3L;
    private static final long GROUP_VM_ID = 1L;
    private static final long HOST_ID = 10L;
    private static final long LAST_HOST_ID = 11L;
    private static final long PLANNED_HOST_ID = 12L;
    private static final int CAPACITY_RELEASE_INTERVAL = 3600;

    @Mock
    AffinityGroupDao _affinityGroupDao;

    @Mock
    AffinityGroupVMMapDao _affinityGroupVMMapDao;

    @Mock
    VMInstanceDao _vmInstanceDao;

    @Spy
    @InjectMocks
    HostAntiAffinityProcessor processor = new HostAntiAffinityProcessor();

    @Mock
    VirtualMachine vm;

    @Mock
    VMInstanceVO groupVM;

    @Mock
    AffinityGroupVO affinityGroupVO;

    @Mock
    AffinityGroupVMMapVO vmGroupMapping;

    private ExcludeList avoid;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        processor._vmCapacityReleaseInterval = CAPACITY_RELEASE_INTERVAL;
        avoid = new ExcludeList();

        when(vm.getId()).thenReturn(VM_ID);
        when(vmGroupMapping.getAffinityGroupId()).thenReturn(AFFINITY_GROUP_ID);
        when(_affinityGroupDao.findById(AFFINITY_GROUP_ID)).thenReturn(affinityGroupVO);
        when(affinityGroupVO.getId()).thenReturn(AFFINITY_GROUP_ID);
        when(_affinityGroupVMMapDao.listVmIdsByAffinityGroup(AFFINITY_GROUP_ID))
                .thenReturn(new ArrayList<>(Arrays.asList(GROUP_VM_ID, VM_ID)));
    }

    private boolean avoids(long hostId) {
        return avoid.getHostsToAvoid() != null && avoid.getHostsToAvoid().contains(hostId);
    }

    @Test
    public void testRunningGroupVmHostIsAvoided() {
        when(_vmInstanceDao.findById(GROUP_VM_ID)).thenReturn(groupVM);
        when(groupVM.isRemoved()).thenReturn(false);
        when(groupVM.getHostId()).thenReturn(HOST_ID);

        processor.processAffinityGroup(vmGroupMapping, avoid, vm);

        assertTrue(avoids(HOST_ID));
    }

    @Test
    public void testStoppedGroupVmWithReservedCapacityLastHostIsAvoided() {
        when(_vmInstanceDao.findById(GROUP_VM_ID)).thenReturn(groupVM);
        when(groupVM.isRemoved()).thenReturn(false);
        when(groupVM.getHostId()).thenReturn(null);
        when(groupVM.getState()).thenReturn(VirtualMachine.State.Stopped);
        when(groupVM.getLastHostId()).thenReturn(LAST_HOST_ID);
        when(groupVM.getUpdateTime()).thenReturn(DateUtil.currentGMTTime());

        processor.processAffinityGroup(vmGroupMapping, avoid, vm);

        assertTrue(avoids(LAST_HOST_ID));
    }

    @Test
    public void testStoppedGroupVmPastReleaseIntervalIsNotAvoided() {
        Date wellPast = new Date(DateUtil.currentGMTTime().getTime() - (CAPACITY_RELEASE_INTERVAL + 60) * 1000L);
        when(_vmInstanceDao.findById(GROUP_VM_ID)).thenReturn(groupVM);
        when(groupVM.isRemoved()).thenReturn(false);
        when(groupVM.getHostId()).thenReturn(null);
        when(groupVM.getState()).thenReturn(VirtualMachine.State.Stopped);
        when(groupVM.getLastHostId()).thenReturn(LAST_HOST_ID);
        when(groupVM.getUpdateTime()).thenReturn(wellPast);

        processor.processAffinityGroup(vmGroupMapping, avoid, vm);

        assertFalse(avoids(LAST_HOST_ID));
    }

    @Test
    public void testMissingGroupVmIsSkipped() {
        when(_vmInstanceDao.findById(GROUP_VM_ID)).thenReturn(null);

        processor.processAffinityGroup(vmGroupMapping, avoid, vm);

        assertTrue(avoid.getHostsToAvoid() == null || avoid.getHostsToAvoid().isEmpty());
    }

    @Test
    public void testRemovedGroupVmIsSkipped() {
        when(_vmInstanceDao.findById(GROUP_VM_ID)).thenReturn(groupVM);
        when(groupVM.isRemoved()).thenReturn(true);
        // a removed VM is not running anywhere, so neither of its hosts should be avoided
        lenient().when(groupVM.getHostId()).thenReturn(HOST_ID);
        lenient().when(groupVM.getState()).thenReturn(VirtualMachine.State.Stopped);
        lenient().when(groupVM.getLastHostId()).thenReturn(LAST_HOST_ID);
        lenient().when(groupVM.getUpdateTime()).thenReturn(DateUtil.currentGMTTime());

        processor.processAffinityGroup(vmGroupMapping, avoid, vm);

        assertFalse(avoids(HOST_ID));
        assertFalse(avoids(LAST_HOST_ID));
    }

    @Test
    public void testStartingGroupVmWithReservedCapacityLastHostIsAvoided() {
        when(_vmInstanceDao.findById(GROUP_VM_ID)).thenReturn(groupVM);
        when(groupVM.isRemoved()).thenReturn(false);
        when(groupVM.getHostId()).thenReturn(null);
        when(groupVM.getState()).thenReturn(VirtualMachine.State.Starting);
        when(groupVM.getLastHostId()).thenReturn(LAST_HOST_ID);
        when(groupVM.getUpdateTime()).thenReturn(DateUtil.currentGMTTime());

        processor.processAffinityGroup(vmGroupMapping, avoid, vm);

        assertTrue(avoids(LAST_HOST_ID));
    }

    @Test
    public void testPlannedHostWinsOverStaleDatabaseHost() {
        VMInstanceVO plannedVm = org.mockito.Mockito.mock(VMInstanceVO.class);
        when(plannedVm.getId()).thenReturn(GROUP_VM_ID);
        when(plannedVm.getHostId()).thenReturn(PLANNED_HOST_ID);

        // the database still shows the pre-migration host
        when(_vmInstanceDao.findById(GROUP_VM_ID)).thenReturn(groupVM);
        when(groupVM.isRemoved()).thenReturn(false);
        when(groupVM.getHostId()).thenReturn(HOST_ID);

        processor.processAffinityGroup(vmGroupMapping, avoid, vm, Arrays.asList(plannedVm));

        assertTrue(avoids(PLANNED_HOST_ID));
        assertFalse(avoids(HOST_ID));
    }

    @Test
    public void testPlannedVmWithoutHostFallsBackToDatabase() {
        VMInstanceVO plannedVm = org.mockito.Mockito.mock(VMInstanceVO.class);
        when(plannedVm.getId()).thenReturn(GROUP_VM_ID);
        when(plannedVm.getHostId()).thenReturn(null);

        when(_vmInstanceDao.findById(GROUP_VM_ID)).thenReturn(groupVM);
        when(groupVM.isRemoved()).thenReturn(false);
        when(groupVM.getHostId()).thenReturn(HOST_ID);

        processor.processAffinityGroup(vmGroupMapping, avoid, vm, Arrays.asList(plannedVm));

        assertTrue(avoids(HOST_ID));
    }

    @Test
    public void testEmptyVmListBehavesAsBefore() {
        when(_vmInstanceDao.findById(GROUP_VM_ID)).thenReturn(groupVM);
        when(groupVM.isRemoved()).thenReturn(false);
        when(groupVM.getHostId()).thenReturn(HOST_ID);

        processor.processAffinityGroup(vmGroupMapping, avoid, vm, Collections.emptyList());

        assertTrue(avoids(HOST_ID));
    }

    @Test
    public void testVmIsNotAvoidedAgainstItself() {
        List<Long> ids = new ArrayList<>(Arrays.asList(VM_ID));
        when(_affinityGroupVMMapDao.listVmIdsByAffinityGroup(AFFINITY_GROUP_ID)).thenReturn(ids);

        processor.processAffinityGroup(vmGroupMapping, avoid, vm);

        assertTrue(avoid.getHostsToAvoid() == null || avoid.getHostsToAvoid().isEmpty());
    }
}
