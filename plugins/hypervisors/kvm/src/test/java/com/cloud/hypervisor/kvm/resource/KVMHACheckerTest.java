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
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.to.HostTO;
import com.cloud.agent.api.to.NetworkTO;
import com.cloud.hypervisor.kvm.resource.KVMHABase.HAStoragePool;
import com.cloud.hypervisor.kvm.resource.KVMHABase.PoolType;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;

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
}
