//
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
//

package com.cloud.hypervisor.kvm.resource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.to.HostTO;
import com.cloud.agent.api.to.NetworkTO;
import com.cloud.hypervisor.kvm.resource.KVMHABase.HAStoragePool;
import com.cloud.hypervisor.kvm.resource.KVMHABase.PoolType;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;

@RunWith(MockitoJUnitRunner.class)
public class KVMHACheckerTest {

    private final HostTO host = mock(HostTO.class);

    @Before
    public void setUp() {
        NetworkTO network = mock(NetworkTO.class);
        when(network.getIp()).thenReturn("10.0.0.1");
        when(host.getPrivateNetwork()).thenReturn(network);
    }

    private KVMStoragePool mockPool(String uuid, boolean hasHeartBeat) {
        KVMStoragePool pool = mock(KVMStoragePool.class);
        when(pool.getUuid()).thenReturn(uuid);
        lenient().when(pool.hasHeartBeat(any(), any())).thenReturn(hasHeartBeat);
        return pool;
    }

    private List<HAStoragePool> haPools(KVMStoragePool... pools) {
        List<HAStoragePool> haPools = new ArrayList<>();
        for (KVMStoragePool pool : pools) {
            haPools.add(new HAStoragePool(pool, "10.0.0.2", "/export", PoolType.PrimaryStorage));
        }
        return haPools;
    }

    private boolean hasHeartBeat(boolean reportIfHeartBeatFailedForOneStoragePool, KVMStoragePool... pools) {
        return new KVMHAChecker(haPools(pools), host, reportIfHeartBeatFailedForOneStoragePool).hasHeartBeat();
    }

    @Test
    public void testHostWithoutStoragePoolsToCheckIsNotDead() {
        // e.g. a neighbour whose agent is not upgraded yet and monitors no pool: it can not tell, so it must not say that the host is dead
        assertTrue(hasHeartBeat(true));
        assertTrue(hasHeartBeat(false));
    }

    @Test
    public void testHostWithHeartBeatOnAllPoolsIsAlive() {
        assertTrue(hasHeartBeat(true, mockPool("a", true), mockPool("b", true)));
        assertTrue(hasHeartBeat(false, mockPool("a", true), mockPool("b", true)));
    }

    @Test
    public void testHostWithoutHeartBeatOnAllPoolsIsDead() {
        assertFalse(hasHeartBeat(true, mockPool("a", false), mockPool("b", false)));
        assertFalse(hasHeartBeat(false, mockPool("a", false), mockPool("b", false)));
    }

    @Test
    public void testHostIsDeadIfOnePoolHasNoHeartBeatAndThatIsReported() {
        assertFalse(hasHeartBeat(true, mockPool("a", true), mockPool("b", false)));
        assertFalse(hasHeartBeat(true, mockPool("a", false), mockPool("b", true)));
        // the order of the pools does not matter, it was the last pool which decided
        assertFalse(hasHeartBeat(true, mockPool("a", true), mockPool("b", false), mockPool("c", true)));
    }

    @Test
    public void testHostIsAliveIfOnePoolHasHeartBeatAndThatIsNotReported() {
        // a dead heartbeat on the last pool, here the pool of another type, does not outvote an alive one on an other pool
        assertTrue(hasHeartBeat(false, mockPool("a", true), mockPool("b", false)));
        assertTrue(hasHeartBeat(false, mockPool("a", false), mockPool("b", true)));
        assertTrue(hasHeartBeat(false, mockPool("a", false), mockPool("b", true), mockPool("c", false)));
    }

    @Test
    public void testPoolsAreNotCheckedAfterOneWithoutHeartBeatIsReported() {
        KVMStoragePool first = mockPool("a", false);
        KVMStoragePool second = mockPool("b", true);

        assertFalse(hasHeartBeat(true, first, second));

        verify(second, never()).hasHeartBeat(any(), any());
    }

    @Test
    public void testAllPoolsAreCheckedIfNotReported() {
        List<KVMStoragePool> pools = Arrays.asList(mockPool("a", true), mockPool("b", false));

        assertTrue(hasHeartBeat(false, pools.toArray(new KVMStoragePool[0])));

        for (KVMStoragePool pool : pools) {
            verify(pool).hasHeartBeat(any(), any());
        }
    }
}
