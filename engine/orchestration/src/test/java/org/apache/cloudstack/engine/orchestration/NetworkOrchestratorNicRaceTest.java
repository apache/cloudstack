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
package org.apache.cloudstack.engine.orchestration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.network.Network;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.VirtualMachineProfile;

@RunWith(MockitoJUnitRunner.Silent.class)
public class NetworkOrchestratorNicRaceTest {

    @Mock
    NetworkDao _networksDao;
    @Mock
    DataCenterDao _dcDao;

    @Spy
    @InjectMocks
    NetworkOrchestrator orchestrator = new NetworkOrchestrator();

    @Test
    public void checkForRaceAndAllocateNicRetriesInsteadOfNpeWhenNoIpRequested() throws Exception {
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getId()).thenReturn(1L);
        Mockito.when(network.getDataCenterId()).thenReturn(1L);
        Mockito.when(network.getTrafficType()).thenReturn(TrafficType.Guest);

        NetworkVO ntwkVO = Mockito.mock(NetworkVO.class);
        Mockito.when(ntwkVO.getGuruName()).thenReturn("TestGuru");
        Mockito.when(_networksDao.findById(1L)).thenReturn(ntwkVO);

        DataCenterVO dcVo = Mockito.mock(DataCenterVO.class);
        Mockito.when(dcVo.getNetworkType()).thenReturn(NetworkType.Advanced);
        Mockito.when(_dcDao.findById(1L)).thenReturn(dcVo);

        VirtualMachineProfile vm = Mockito.mock(VirtualMachineProfile.class);
        Mockito.when(vm.getId()).thenReturn(1L);
        Mockito.when(vm.getType()).thenReturn(Type.User);

        NicProfile profile = Mockito.mock(NicProfile.class);
        Mockito.when(profile.getIpv4AllocationRaceCheck()).thenReturn(true);

        NetworkGuru guru = Mockito.mock(NetworkGuru.class);
        Mockito.when(guru.getName()).thenReturn("TestGuru");
        Mockito.when(guru.allocate(eq(network), isNull(), eq(vm))).thenReturn(profile);
        orchestrator.setNetworkGurus(Collections.singletonList(guru));

        NicVO persisted = Mockito.mock(NicVO.class);
        // First attempt loses the IP-allocation race (persist returns null); the retry wins (non-null).
        Mockito.doReturn(null).doReturn(persisted)
                .when(orchestrator).persistNicAfterRaceCheck(any(NicVO.class), eq(1L), eq(profile), anyInt());

        // requested == null is the common "no explicit IP" deploy path. Before the fix, losing the race
        // dereferenced the null requested profile and threw NPE instead of retrying the allocation.
        NicVO result = orchestrator.checkForRaceAndAllocateNic(null, network, null, 0, vm);

        Assert.assertSame("losing the race should retry and return the persisted NIC, not NPE", persisted, result);
    }
}
