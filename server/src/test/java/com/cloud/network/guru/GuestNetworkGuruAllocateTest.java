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
package com.cloud.network.guru;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.utils.Pair;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class GuestNetworkGuruAllocateTest {

    // Minimal concrete guru so the abstract base's allocate() can be exercised directly.
    public static class TestGuestNetworkGuru extends GuestNetworkGuru {
        @Override
        protected boolean canHandle(NetworkOffering offering, DataCenter.NetworkType networkType, PhysicalNetwork physicalNetwork) {
            return false;
        }
    }

    @Mock
    private DataCenterDao dcDao;
    @Mock
    private IpAddressManager ipAddrMgr;
    @Mock
    private NetworkModel networkModel;
    @Mock
    private NetworkOfferingDao networkOfferingDao;

    @InjectMocks
    private TestGuestNetworkGuru guru = new TestGuestNetworkGuru();

    private static final long ZONE_ID = 1L;
    private static final long NETWORK_ID = 100L;
    private static final long OFFERING_ID = 11L;

    private VirtualMachineProfile userVmProfile() {
        VirtualMachine vmInstance = Mockito.mock(VirtualMachine.class);
        when(vmInstance.getType()).thenReturn(VirtualMachine.Type.User);
        VirtualMachineProfile vm = Mockito.mock(VirtualMachineProfile.class);
        when(vm.getVirtualMachine()).thenReturn(vmInstance);
        when(vm.getType()).thenReturn(VirtualMachine.Type.User);
        return vm;
    }

    private Network guestNetwork(GuestType guestType) {
        Network network = Mockito.mock(Network.class);
        when(network.getTrafficType()).thenReturn(TrafficType.Guest);
        when(network.getGuestType()).thenReturn(guestType);
        when(network.getSpecifyIpRanges()).thenReturn(true);
        when(network.getDataCenterId()).thenReturn(ZONE_ID);
        when(network.getId()).thenReturn(NETWORK_ID);
        when(network.getNetworkOfferingId()).thenReturn(OFFERING_ID);
        when(network.getGateway()).thenReturn("10.0.0.1");
        return network;
    }

    @Test
    public void testAllocateIsolatedWithSpecifyIpRangesUsesNetworkCidrNotDirectIp()
            throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        Network network = guestNetwork(GuestType.Isolated);
        when(network.getCidr()).thenReturn("10.0.0.0/8");
        VirtualMachineProfile vm = userVmProfile();

        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        when(dcDao.findById(ZONE_ID)).thenReturn(dc);
        when(ipAddrMgr.acquireGuestIpAddress(eq(network), isNull())).thenReturn("10.0.0.5");
        when(networkModel.getValidNetworkCidr(network)).thenReturn("10.0.0.0/8");
        when(networkModel.getNetworkIp4Dns(eq(network), eq(dc))).thenReturn(new Pair<>("8.8.8.8", null));
        when(networkModel.getNextAvailableMacAddressInNetwork(NETWORK_ID)).thenReturn("02:00:00:aa:bb:cc");
        when(networkOfferingDao.isIpv6Supported(OFFERING_ID)).thenReturn(false);

        NicProfile result = guru.allocate(network, null, vm);

        verify(ipAddrMgr, never()).allocateDirectIp(any(), any(), any(), any(), any(), any());
        verify(ipAddrMgr).acquireGuestIpAddress(eq(network), isNull());
        Assert.assertEquals("10.0.0.5", result.getIPv4Address());
    }

    @Test
    public void testAllocateSharedWithSpecifyIpRangesUsesDirectIp()
            throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        Network network = guestNetwork(GuestType.Shared);
        VirtualMachineProfile vm = userVmProfile();

        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        when(dcDao.findById(ZONE_ID)).thenReturn(dc);
        when(networkModel.getNextAvailableMacAddressInNetwork(NETWORK_ID)).thenReturn("02:00:00:aa:bb:cc");
        when(networkOfferingDao.isIpv6Supported(OFFERING_ID)).thenReturn(false);

        guru.allocate(network, null, vm);

        verify(ipAddrMgr, times(1)).allocateDirectIp(any(), eq(dc), eq(vm), eq(network), isNull(), isNull());
        verify(ipAddrMgr, never()).acquireGuestIpAddress(any(), any());
    }
}
