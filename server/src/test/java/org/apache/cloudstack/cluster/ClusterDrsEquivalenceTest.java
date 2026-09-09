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
package org.apache.cloudstack.cluster;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.affinity.AffinityGroupVMMapVO;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.vm.VMInstanceVO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Working out where a VM could go is the expensive part of DRS planning, so VMs that would get the
 * same answer share the work. These check what counts as the same answer.
 */
@RunWith(MockitoJUnitRunner.class)
public class ClusterDrsEquivalenceTest {

    @Mock
    private VolumeDao volumeDao;

    @Mock
    private AffinityGroupVMMapDao affinityGroupVMMapDao;

    @InjectMocks
    private ClusterDrsServiceImpl service = new ClusterDrsServiceImpl();

    private VMInstanceVO vm(long id, long offeringId, long templateId, Long hostId, Long... poolIds) {
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.lenient().when(vm.getId()).thenReturn(id);
        Mockito.lenient().when(vm.getServiceOfferingId()).thenReturn(offeringId);
        Mockito.lenient().when(vm.getTemplateId()).thenReturn(templateId);
        Mockito.lenient().when(vm.getHostId()).thenReturn(hostId);
        Mockito.lenient().when(vm.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);

        List<VolumeVO> volumes = Arrays.stream(poolIds).map(poolId -> {
            VolumeVO volume = Mockito.mock(VolumeVO.class);
            Mockito.lenient().when(volume.getPoolId()).thenReturn(poolId);
            return volume;
        }).collect(java.util.stream.Collectors.toList());
        Mockito.lenient().when(volumeDao.findCreatedByInstance(id)).thenReturn(volumes);
        Mockito.lenient().when(affinityGroupVMMapDao.listByInstanceId(id)).thenReturn(Collections.emptyList());
        return vm;
    }

    @Test
    public void testIdenticalVmsShareOnePass() {
        assertEquals(service.migrationEquivalenceKey(vm(1L, 10L, 20L, 30L, 40L)),
                service.migrationEquivalenceKey(vm(2L, 10L, 20L, 30L, 40L)));
    }

    @Test
    public void testVolumeOrderDoesNotMatter() {
        assertEquals(service.migrationEquivalenceKey(vm(1L, 10L, 20L, 30L, 40L, 41L)),
                service.migrationEquivalenceKey(vm(2L, 10L, 20L, 30L, 41L, 40L)));
    }

    @Test
    public void testDifferentOfferingIsADifferentAnswer() {
        assertNotEquals(service.migrationEquivalenceKey(vm(1L, 10L, 20L, 30L, 40L)),
                service.migrationEquivalenceKey(vm(2L, 11L, 20L, 30L, 40L)));
    }

    @Test
    public void testDifferentCurrentHostIsADifferentAnswer() {
        assertNotEquals(service.migrationEquivalenceKey(vm(1L, 10L, 20L, 30L, 40L)),
                service.migrationEquivalenceKey(vm(2L, 10L, 20L, 31L, 40L)));
    }

    @Test
    public void testDifferentStorageIsADifferentAnswer() {
        assertNotEquals(service.migrationEquivalenceKey(vm(1L, 10L, 20L, 30L, 40L)),
                service.migrationEquivalenceKey(vm(2L, 10L, 20L, 30L, 42L)));
    }

    @Test
    public void testDifferentTemplateIsADifferentAnswer() {
        assertNotEquals(service.migrationEquivalenceKey(vm(1L, 10L, 20L, 30L, 40L)),
                service.migrationEquivalenceKey(vm(2L, 10L, 21L, 30L, 40L)));
    }

    @Test
    public void testDifferentAffinityGroupsIsADifferentAnswer() {
        VMInstanceVO grouped = vm(1L, 10L, 20L, 30L, 40L);
        AffinityGroupVMMapVO mapping = Mockito.mock(AffinityGroupVMMapVO.class);
        Mockito.when(mapping.getAffinityGroupId()).thenReturn(99L);
        Mockito.when(affinityGroupVMMapDao.listByInstanceId(1L)).thenReturn(Collections.singletonList(mapping));

        assertNotEquals("a VM in an affinity group cannot reuse an ungrouped VM's candidate hosts",
                service.migrationEquivalenceKey(grouped),
                service.migrationEquivalenceKey(vm(2L, 10L, 20L, 30L, 40L)));
    }
}
