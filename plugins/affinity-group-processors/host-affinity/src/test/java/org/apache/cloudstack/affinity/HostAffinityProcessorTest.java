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

import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.host.Host;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.junit.After;
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
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class HostAffinityProcessorTest {

    private static final long AFFINITY_GROUP_ID = 2L;
    private static final String AFFINITY_GROUP_NAME = "Host affinity group";
    private static final Long VM_ID = 3L;
    private static final Long GROUP_VM_1_ID = 1L;
    private static final Long GROUP_VM_2_ID = 2L;
    private static final Long HOST_ID = 1L;
    private static final Long HOST_2_ID = 2L;

    @Mock
    AffinityGroupDao affinityGroupDao;

    @Mock
    AffinityGroupVMMapDao affinityGroupVMMapDao;

    @Mock
    VMInstanceDao vmInstanceDao;

    @Spy
    @InjectMocks
    HostAffinityProcessor processor = new HostAffinityProcessor();

    @Mock
    DeploymentPlan plan;

    @Mock
    VirtualMachine vm;

    @Mock
    VMInstanceVO groupVM1;

    @Mock
    VMInstanceVO groupVM2;

    @Mock
    AffinityGroupVO affinityGroupVO;

    @Mock
    AffinityGroupVMMapVO mapVO;

    @Mock
    DeployDestination dest;

    @Mock
    Host host;

    @Mock
    VirtualMachineProfile profile;

    private AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);

        when(groupVM1.getHostId()).thenReturn(HOST_ID);
        when(groupVM2.getHostId()).thenReturn(HOST_ID);
        when(vmInstanceDao.findById(GROUP_VM_1_ID)).thenReturn(groupVM1);
        when(vmInstanceDao.findById(GROUP_VM_2_ID)).thenReturn(groupVM2);

        when(affinityGroupVMMapDao.listVmIdsByAffinityGroup(AFFINITY_GROUP_ID)).thenReturn(new ArrayList<>(Arrays.asList(GROUP_VM_1_ID, GROUP_VM_2_ID, VM_ID)));

        when(vm.getId()).thenReturn(VM_ID);

        when(affinityGroupVO.getId()).thenReturn(AFFINITY_GROUP_ID);
        when(affinityGroupVO.getName()).thenReturn(AFFINITY_GROUP_NAME);
        when(mapVO.getAffinityGroupId()).thenReturn(AFFINITY_GROUP_ID);

        when(affinityGroupDao.findById(AFFINITY_GROUP_ID)).thenReturn(affinityGroupVO);

        when(dest.getHost()).thenReturn(host);
        when(host.getId()).thenReturn(HOST_ID);
        when(profile.getVirtualMachine()).thenReturn(vm);
        when(affinityGroupVMMapDao.findByVmIdType(eq(VM_ID), any())).thenReturn(new ArrayList<>(Arrays.asList(mapVO)));
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testProcessAffinityGroupMultipleVMs() {
        processor.processAffinityGroup(mapVO, plan, vm);
        verify(plan).setPreferredHosts(Arrays.asList(HOST_ID));
    }

    @Test
    public void testProcessAffinityGroupEmptyGroup() {
        when(affinityGroupVMMapDao.listVmIdsByAffinityGroup(AFFINITY_GROUP_ID)).thenReturn(new ArrayList<>());
        processor.processAffinityGroup(mapVO, plan, vm);
        verify(plan).setPreferredHosts(new ArrayList<>());
    }

    @Test
    public void testGetPreferredHostsFromGroupVMIdsMultipleVMs() {
        List<Long> list = new ArrayList<>(Arrays.asList(GROUP_VM_1_ID, GROUP_VM_2_ID));
        List<Long> preferredHosts = processor.getPreferredHostsFromGroupVMIds(list, Collections.emptyList());
        assertNotNull(preferredHosts);
        assertEquals(1, preferredHosts.size());
        assertEquals(HOST_ID, preferredHosts.get(0));
    }

    @Test
    public void testGetPreferredHostsFromGroupVMIdsEmptyVMsList() {
        List<Long> list = new ArrayList<>();
        List<Long> preferredHosts = processor.getPreferredHostsFromGroupVMIds(list, Collections.emptyList());
        assertNotNull(preferredHosts);
        assertTrue(preferredHosts.isEmpty());
    }

    @Test
    public void testCheckAffinityGroup() {
        assertTrue(processor.checkAffinityGroup(mapVO, vm, HOST_ID));
    }

    @Test
    public void testCheckAffinityGroupWrongHostId() {
        assertFalse(processor.checkAffinityGroup(mapVO, vm, HOST_2_ID));
    }

    @Test
    public void testCheck() {
        assertTrue(processor.check(profile, dest));
    }

    @Test
    public void testCheckWrongHostId() {
        when(host.getId()).thenReturn(HOST_2_ID);
        assertFalse(processor.check(profile, dest));
    }
}
