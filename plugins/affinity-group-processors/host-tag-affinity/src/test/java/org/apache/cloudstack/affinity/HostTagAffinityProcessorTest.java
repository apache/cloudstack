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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@RunWith(MockitoJUnitRunner.class)
public class HostTagAffinityProcessorTest {

    @Spy
    @InjectMocks
    HostTagAffinityProcessor processor = new HostTagAffinityProcessor();

    @Mock
    AffinityGroupVMMapDao affinityGroupVMMapDao;
    @Mock
    AffinityGroupDao affinityGroupDao;
    @Mock
    HostDao hostDao;

    long vmId = 10L;
    long affinityGroupId = 20L;
    long zoneId = 2L;
    long host2Id = 3L;
    long host3Id = 4L;
    String groupName = "gold";

    private VirtualMachineProfile mockVmProfile() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        when(vm.getId()).thenReturn(vmId);
        when(vm.getDataCenterId()).thenReturn(zoneId);
        VirtualMachineProfile vmProfile = Mockito.mock(VirtualMachineProfile.class);
        when(vmProfile.getVirtualMachine()).thenReturn(vm);
        return vmProfile;
    }

    private void stubGroupMembership() {
        List<AffinityGroupVMMapVO> vmGroupMappings = new ArrayList<>();
        vmGroupMappings.add(new AffinityGroupVMMapVO(affinityGroupId, vmId));
        when(affinityGroupVMMapDao.findByVmIdType(eq(vmId), nullable(String.class))).thenReturn(vmGroupMappings);
        AffinityGroupVO group = Mockito.mock(AffinityGroupVO.class);
        when(affinityGroupDao.findById(affinityGroupId)).thenReturn(group);
        when(group.getName()).thenReturn(groupName);
    }

    @Test
    public void testProcessRaisesPriorityForTaggedHosts() {
        VirtualMachineProfile vmProfile = mockVmProfile();
        stubGroupMembership();

        HostVO host2 = Mockito.mock(HostVO.class);
        when(host2.getId()).thenReturn(host2Id);
        HostVO host3 = Mockito.mock(HostVO.class);
        when(host3.getId()).thenReturn(host3Id);
        when(hostDao.listByHostTag(eq(Host.Type.Routing), isNull(), isNull(), eq(zoneId), eq(groupName)))
                .thenReturn(Arrays.asList(host2, host3));

        DataCenterDeployment plan = new DataCenterDeployment(zoneId);
        ExcludeList avoid = new ExcludeList();

        processor.process(vmProfile, plan, avoid);

        // Both tagged hosts get raised to priority 1 (DEFAULT 0 -> HIGHER +1); nothing is excluded.
        Assert.assertEquals(2, plan.getHostPriorities().size());
        Assert.assertEquals(Integer.valueOf(1), plan.getHostPriorities().get(host2Id));
        Assert.assertEquals(Integer.valueOf(1), plan.getHostPriorities().get(host3Id));
        Assert.assertFalse("soft preference: no host may be excluded", avoid.shouldAvoid(host2));
    }

    @Test
    public void testProcessNoMatchingHostsIsNoOp() {
        VirtualMachineProfile vmProfile = mockVmProfile();
        stubGroupMembership();
        when(hostDao.listByHostTag(eq(Host.Type.Routing), isNull(), isNull(), eq(zoneId), eq(groupName)))
                .thenReturn(Collections.emptyList());

        DataCenterDeployment plan = new DataCenterDeployment(zoneId);
        ExcludeList avoid = new ExcludeList();

        processor.process(vmProfile, plan, avoid);

        Assert.assertTrue(plan.getHostPriorities().isEmpty());
    }

    @Test
    public void testCheckAlwaysTrue() throws Exception {
        // A soft preference must never fail a planned destination.
        Assert.assertTrue(processor.check(Mockito.mock(VirtualMachineProfile.class), Mockito.mock(DeployDestination.class)));
    }
}
