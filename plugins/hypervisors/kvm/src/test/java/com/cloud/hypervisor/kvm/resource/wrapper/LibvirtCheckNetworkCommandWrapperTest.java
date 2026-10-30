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

package com.cloud.hypervisor.kvm.resource.wrapper;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.network.PhysicalNetworkSetupInfo;

// Covers the guest traffic label tracking that getEffectiveGuestBridgeName() relies on
@RunWith(MockitoJUnitRunner.class)
public class LibvirtCheckNetworkCommandWrapperTest {

    @Mock
    private LibvirtComputingResource libvirtComputingResource;

    private final LibvirtCheckNetworkCommandWrapper wrapper = new LibvirtCheckNetworkCommandWrapper();

    private PhysicalNetworkSetupInfo infoWithGuestLabel(String guestLabel) {
        PhysicalNetworkSetupInfo info = new PhysicalNetworkSetupInfo();
        info.setGuestNetworkName(guestLabel);
        return info;
    }

    @Test
    public void setsGuestTrafficLabelWhenPhysicalNetworkCarriesOne() {
        when(libvirtComputingResource.checkNetwork(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
        List<PhysicalNetworkSetupInfo> phyNics = Collections.singletonList(infoWithGuestLabel("cloudbr1"));
        CheckNetworkCommand command = new CheckNetworkCommand(phyNics);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        assertTrue(answer.getResult());
        verify(libvirtComputingResource).setGuestTrafficLabel("cloudbr1");
    }

    @Test
    public void clearsGuestTrafficLabelWhenNoPhysicalNetworkCarriesOne() {
        when(libvirtComputingResource.checkNetwork(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
        List<PhysicalNetworkSetupInfo> phyNics = Collections.singletonList(infoWithGuestLabel(null));
        CheckNetworkCommand command = new CheckNetworkCommand(phyNics);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        assertTrue(answer.getResult());
        verify(libvirtComputingResource).setGuestTrafficLabel(null);
    }

    @Test
    public void revertsGuestTrafficLabelOnceOperatorRemovesIt() {
        when(libvirtComputingResource.checkNetwork(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(true);

        wrapper.execute(new CheckNetworkCommand(Collections.singletonList(infoWithGuestLabel("cloudbr1"))), libvirtComputingResource);
        verify(libvirtComputingResource).setGuestTrafficLabel("cloudbr1");

        // Operator removes the label on a later connect/reconnect - the resource must be told, not left stale.
        wrapper.execute(new CheckNetworkCommand(Collections.singletonList(infoWithGuestLabel(null))), libvirtComputingResource);
        verify(libvirtComputingResource).setGuestTrafficLabel(null);
    }

    @Test
    public void blankGuestNetworkNameDoesNotOverrideAnEarlierRealLabelInTheSameConnect() {
        when(libvirtComputingResource.checkNetwork(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
        List<PhysicalNetworkSetupInfo> phyNics = java.util.Arrays.asList(infoWithGuestLabel("cloudbr1"), infoWithGuestLabel(""));
        CheckNetworkCommand command = new CheckNetworkCommand(phyNics);

        wrapper.execute(command, libvirtComputingResource);

        verify(libvirtComputingResource).setGuestTrafficLabel("cloudbr1");
    }
}
