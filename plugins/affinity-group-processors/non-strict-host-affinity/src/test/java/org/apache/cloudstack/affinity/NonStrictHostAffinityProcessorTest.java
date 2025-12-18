/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.affinity;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.class)
public class NonStrictHostAffinityProcessorTest {

    @Spy
    @InjectMocks
    NonStrictHostAffinityProcessor processor = new NonStrictHostAffinityProcessor();

    @Mock
    AffinityGroupVMMapDao _affinityGroupVMMapDao;
    @Mock
    AffinityGroupDao affinityGroupDao;
    @Mock
    VMInstanceDao vmInstanceDao;

    long vmId = 10L;
    long vm2Id = 11L;
    long vm3Id = 12L;
    long affinityGroupId = 20L;
    long zoneId = 2L;
    long host2Id = 3L;
    long host3Id = 4L;

    @Test
    public void testProcessWithEmptyPlan() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        when(vm.getId()).thenReturn(vmId);
        VirtualMachineProfile vmProfile = Mockito.mock(VirtualMachineProfile.class);
        when(vmProfile.getVirtualMachine()).thenReturn(vm);

        List<AffinityGroupVMMapVO> vmGroupMappings = new ArrayList<>();
        vmGroupMappings.add(new AffinityGroupVMMapVO(affinityGroupId, vmId));
        when(_affinityGroupVMMapDao.findByVmIdType(eq(vmId), nullable(String.class))).thenReturn(vmGroupMappings);

        DataCenterDeployment plan = new DataCenterDeployment(zoneId);
        ExcludeList avoid = new ExcludeList();

        AffinityGroupVO affinityGroupVO = Mockito.mock(AffinityGroupVO.class);
        when(affinityGroupDao.findById(affinityGroupId)).thenReturn(affinityGroupVO);
        when(affinityGroupVO.getId()).thenReturn(affinityGroupId);
        List<Long> groupVMIds = new ArrayList<>(Arrays.asList(vmId, vm2Id));
        when(_affinityGroupVMMapDao.listVmIdsByAffinityGroup(affinityGroupId)).thenReturn(groupVMIds);
        VMInstanceVO vm2 = new VMInstanceVO();
        when(vmInstanceDao.findById(vm2Id)).thenReturn(vm2);
        vm2.setHostId(host2Id);

        processor.process(vmProfile, plan, avoid);

        Assert.assertEquals(1, plan.getHostPriorities().size());
        Assert.assertNotNull(plan.getHostPriorities().get(host2Id));
        Assert.assertEquals(Integer.valueOf(1), plan.getHostPriorities().get(host2Id));
    }

    @Test
    public void testProcessWithPlan() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        when(vm.getId()).thenReturn(vmId);
        VirtualMachineProfile vmProfile = Mockito.mock(VirtualMachineProfile.class);
        when(vmProfile.getVirtualMachine()).thenReturn(vm);

        List<AffinityGroupVMMapVO> vmGroupMappings = new ArrayList<>();
        vmGroupMappings.add(new AffinityGroupVMMapVO(affinityGroupId, vmId));
        when(_affinityGroupVMMapDao.findByVmIdType(eq(vmId), nullable(String.class))).thenReturn(vmGroupMappings);

        DataCenterDeployment plan = new DataCenterDeployment(zoneId);
        plan.adjustHostPriority(host2Id, DeploymentPlan.HostPriorityAdjustment.DEFAULT);
        plan.adjustHostPriority(host3Id, DeploymentPlan.HostPriorityAdjustment.LOWER);
        ExcludeList avoid = new ExcludeList();

        AffinityGroupVO affinityGroupVO = Mockito.mock(AffinityGroupVO.class);
        when(affinityGroupDao.findById(affinityGroupId)).thenReturn(affinityGroupVO);
        when(affinityGroupVO.getId()).thenReturn(affinityGroupId);
        List<Long> groupVMIds = new ArrayList<>(Arrays.asList(vmId, vm2Id, vm3Id));
        when(_affinityGroupVMMapDao.listVmIdsByAffinityGroup(affinityGroupId)).thenReturn(groupVMIds);
        VMInstanceVO vm2 = new VMInstanceVO();
        when(vmInstanceDao.findById(vm2Id)).thenReturn(vm2);
        vm2.setHostId(host2Id);
        VMInstanceVO vm3 = new VMInstanceVO();
        when(vmInstanceDao.findById(vm3Id)).thenReturn(vm3);
        vm3.setHostId(host3Id);

        processor.process(vmProfile, plan, avoid);

        Assert.assertEquals(2, plan.getHostPriorities().size());
        Assert.assertNotNull(plan.getHostPriorities().get(host2Id));
        Assert.assertEquals(Integer.valueOf(1), plan.getHostPriorities().get(host2Id));
        Assert.assertNotNull(plan.getHostPriorities().get(host3Id));
        Assert.assertEquals(Integer.valueOf(0), plan.getHostPriorities().get(host3Id));
    }

    @Test
    public void testProcessWithNotRunningVM() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        when(vm.getId()).thenReturn(vmId);
        VirtualMachineProfile vmProfile = Mockito.mock(VirtualMachineProfile.class);
        when(vmProfile.getVirtualMachine()).thenReturn(vm);

        List<AffinityGroupVMMapVO> vmGroupMappings = new ArrayList<>();
        vmGroupMappings.add(new AffinityGroupVMMapVO(affinityGroupId, vmId));
        when(_affinityGroupVMMapDao.findByVmIdType(eq(vmId), nullable(String.class))).thenReturn(vmGroupMappings);

        DataCenterDeployment plan = new DataCenterDeployment(zoneId);
        ExcludeList avoid = new ExcludeList();

        AffinityGroupVO affinityGroupVO = Mockito.mock(AffinityGroupVO.class);
        when(affinityGroupDao.findById(affinityGroupId)).thenReturn(affinityGroupVO);
        when(affinityGroupVO.getId()).thenReturn(affinityGroupId);
        List<Long> groupVMIds = new ArrayList<>(Arrays.asList(vmId, vm2Id));
        when(_affinityGroupVMMapDao.listVmIdsByAffinityGroup(affinityGroupId)).thenReturn(groupVMIds);
        VMInstanceVO vm2 = Mockito.mock(VMInstanceVO.class);
        when(vmInstanceDao.findById(vm2Id)).thenReturn(vm2);
        when(vm2.getHostId()).thenReturn(null);
        when(vm2.getLastHostId()).thenReturn(host2Id);
        when(vm2.getState()).thenReturn(VirtualMachine.State.Starting);
        when(vm2.getUpdateTime()).thenReturn(new Date());

        ReflectionTestUtils.setField(processor, "vmCapacityReleaseInterval", 3600);
        processor.process(vmProfile, plan, avoid);

        Assert.assertEquals(1, plan.getHostPriorities().size());
        Assert.assertNotNull(plan.getHostPriorities().get(host2Id));
        Assert.assertEquals(Integer.valueOf(1), plan.getHostPriorities().get(host2Id));
    }
}
