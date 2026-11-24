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

package com.cloud.hypervisor.kvm.resource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.to.HostTO;
import com.cloud.agent.api.to.NetworkTO;
import com.cloud.hypervisor.kvm.resource.KVMHABase.HAStoragePool;
import com.cloud.hypervisor.kvm.resource.KVMHABase.PoolType;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.storage.Storage.StoragePoolType;

@RunWith(MockitoJUnitRunner.Silent.class)
public class KVMHACheckerTest {

    @Mock
    private HostTO host;
    @Mock
    private NetworkTO privateNetwork;

    private List<HAStoragePool> poolsWithHeartBeats(boolean... heartBeats) {
        when(host.getPrivateNetwork()).thenReturn(privateNetwork);
        List<HAStoragePool> pools = new ArrayList<>();
        for (boolean heartBeat : heartBeats) {
            KVMStoragePool kvmPool = mock(KVMStoragePool.class);
            when(kvmPool.hasHeartBeat(any(HAStoragePool.class), any(HostTO.class))).thenReturn(heartBeat);
            pools.add(new HAStoragePool(kvmPool, "host", "/path", PoolType.PrimaryStorage));
        }
        return pools;
    }

    private boolean checkHeartBeat(boolean reportIfFailedForOnePool, boolean... heartBeats) {
        return new KVMHAChecker(poolsWithHeartBeats(heartBeats), host, reportIfFailedForOnePool).hasHeartBeat();
    }

    @Test
    public void testHostIsAliveWhenAnyPoolHasHeartBeatAndOnePoolFailureIsNotReported() {
        assertTrue(checkHeartBeat(false, true, false));
        assertTrue(checkHeartBeat(false, false, true));
    }

    @Test
    public void testHostIsDeadWhenNoPoolHasHeartBeat() {
        assertFalse(checkHeartBeat(false, false, false));
        assertFalse(checkHeartBeat(true, false, false));
    }

    @Test
    public void testHostIsDeadWhenOnePoolHasNoHeartBeatAndItIsReported() {
        assertFalse(checkHeartBeat(true, true, false));
        assertFalse(checkHeartBeat(true, false, true));
    }

    @Test
    public void testHostIsAliveWhenAllPoolsHaveHeartBeat() {
        assertTrue(checkHeartBeat(true, true, true));
        assertTrue(checkHeartBeat(false, true, true));
    }

    @Test
    public void testHostIsDeadWithoutPools() {
        assertFalse(checkHeartBeat(false));
        assertFalse(checkHeartBeat(true));
    }

    private HAStoragePool pool(String uuid, StoragePoolType type, boolean heartBeat) {
        KVMStoragePool kvmPool = mock(KVMStoragePool.class);
        when(kvmPool.getUuid()).thenReturn(uuid);
        when(kvmPool.getType()).thenReturn(type);
        when(kvmPool.hasHeartBeat(any(HAStoragePool.class), any(HostTO.class))).thenReturn(heartBeat);
        return new HAStoragePool(kvmPool, "host", "/path", PoolType.PrimaryStorage);
    }

    private boolean checkWithClvmVolumes(Map<String, List<String>> clvmVolumes, boolean reportIfFailedForOnePool, HAStoragePool... pools) {
        when(host.getPrivateNetwork()).thenReturn(privateNetwork);
        when(host.getClvmVolumesByPool()).thenReturn(clvmVolumes);
        return new KVMHAChecker(List.of(pools), host, reportIfFailedForOnePool).hasHeartBeat();
    }

    @Test
    public void testClvmPoolWithoutVolumesOfTheHostDoesNotKeepADeadNfsHostAlive() {
        Map<String, List<String>> volumes = new HashMap<>();
        assertFalse(checkWithClvmVolumes(volumes, false, pool("nfs", StoragePoolType.NetworkFilesystem, false), pool("clvm", StoragePoolType.CLVM_NG, true)));
        assertFalse(checkWithClvmVolumes(volumes, false, pool("clvm", StoragePoolType.CLVM, true), pool("nfs", StoragePoolType.NetworkFilesystem, false)));
    }

    @Test
    public void testClvmPoolWithVolumesOfTheHostKeepsItAlive() {
        Map<String, List<String>> volumes = Map.of("clvm", List.of("lv1"));
        assertTrue(checkWithClvmVolumes(volumes, false, pool("nfs", StoragePoolType.NetworkFilesystem, false), pool("clvm", StoragePoolType.CLVM_NG, true)));
    }

    @Test
    public void testClvmPoolWithoutVolumesDoesNotFailAliveHostWhenOnePoolFailureIsReported() {
        assertTrue(checkWithClvmVolumes(new HashMap<>(), true, pool("nfs", StoragePoolType.NetworkFilesystem, true), pool("clvm", StoragePoolType.CLVM_NG, false)));
    }

    @Test
    public void testEveryPoolVotesWhenTheSenderSentNoClvmVolumes() {
        assertTrue(checkWithClvmVolumes(null, false, pool("nfs", StoragePoolType.NetworkFilesystem, false), pool("clvm", StoragePoolType.CLVM_NG, true)));
    }

    @Test
    public void testNonClvmPoolsAlwaysVoteWhateverTheClvmVolumes() {
        assertTrue(checkWithClvmVolumes(new HashMap<>(), false, pool("nfs", StoragePoolType.NetworkFilesystem, true), pool("rbd", StoragePoolType.RBD, false)));
        assertFalse(checkWithClvmVolumes(new HashMap<>(), false, pool("nfs", StoragePoolType.NetworkFilesystem, false), pool("rbd", StoragePoolType.RBD, false)));
    }
}
