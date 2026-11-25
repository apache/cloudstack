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

package com.cloud.bgp;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.element.VirtualRouterElement;
import com.cloud.network.element.VpcVirtualRouterElement;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.dao.VpcServiceMapDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import org.apache.cloudstack.network.BgpPeerVO;
import org.apache.cloudstack.network.RoutedIpv4Manager;
import org.apache.cloudstack.network.dao.BgpPeerDao;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BGPServiceImplTest {

    @Spy
    @InjectMocks
    BGPServiceImpl bGPServiceImplSpy = new BGPServiceImpl();

    @Mock
    RoutedIpv4Manager routedIpv4Manager;

    @Mock
    NetworkServiceMapDao ntwkSrvcDao;

    @Mock
    NetworkModel networkModel;

    @Mock
    BgpPeerDao bgpPeerDao;

    @Mock
    AccountDao accountDao;

    @Mock
    VpcServiceMapDao vpcServiceMapDao;

    @Test
    public void testASNumbersOverlap() {
        Assert.assertEquals(bGPServiceImplSpy.isASNumbersOverlap(1,2,3,4), false);
        Assert.assertEquals(bGPServiceImplSpy.isASNumbersOverlap(1,2,2,4), true);
        Assert.assertEquals(bGPServiceImplSpy.isASNumbersOverlap(1,3,2,4), true);
        Assert.assertEquals(bGPServiceImplSpy.isASNumbersOverlap(2,4,1,3), true);
        Assert.assertEquals(bGPServiceImplSpy.isASNumbersOverlap(1,4,2,3), true);
        Assert.assertEquals(bGPServiceImplSpy.isASNumbersOverlap(3,4,1,2), false);
    }

    @Test
    public void testApplyBgpPeersForIsolatedNetwork() throws ResourceUnavailableException {
        Long networkId = 11L;
        Network network = Mockito.mock(Network.class);
        when(network.getId()).thenReturn(networkId);
        when(network.getVpcId()).thenReturn(null);

        when(routedIpv4Manager.isDynamicRoutedNetwork(network)).thenReturn(true);
        when(ntwkSrvcDao.getProviderForServiceInNetwork(networkId, Network.Service.Gateway)).thenReturn("VirtualRouter");
        VirtualRouterElement virtualRouterElement = Mockito.mock(VirtualRouterElement.class);
        when(networkModel.getElementImplementingProvider("VirtualRouter")).thenReturn(virtualRouterElement);

        BgpPeerVO bgpPeer1 = Mockito.mock(BgpPeerVO.class);
        List<BgpPeerVO> bgpPeers = Arrays.asList(bgpPeer1);
        when(bgpPeerDao.listNonRevokeByNetworkId(networkId)).thenReturn(bgpPeers);
        doReturn(true).when(virtualRouterElement).applyBgpPeers(null, network, bgpPeers);

        bGPServiceImplSpy.applyBgpPeers(network, true);

        verify(virtualRouterElement).applyBgpPeers(null, network, bgpPeers);
    }

    @Test
    public void testApplyBgpPeersForVpcTier() throws ResourceUnavailableException {
        Long networkId = 11L;
        Long accountId = 12L;
        Long vpcId = 13L;
        Long zoneId = 1L;
        Network network = Mockito.mock(Network.class);
        when(network.getId()).thenReturn(networkId);
        when(network.getVpcId()).thenReturn(vpcId);
        when(network.getAccountId()).thenReturn(accountId);
        when(network.getDataCenterId()).thenReturn(zoneId);

        when(routedIpv4Manager.isDynamicRoutedNetwork(network)).thenReturn(true);
        when(ntwkSrvcDao.getProviderForServiceInNetwork(networkId, Network.Service.Gateway)).thenReturn("VirtualRouter");
        VirtualRouterElement virtualRouterElement = Mockito.mock(VirtualRouterElement.class);
        when(networkModel.getElementImplementingProvider("VirtualRouter")).thenReturn(virtualRouterElement);

        when(bgpPeerDao.listNonRevokeByVpcId(vpcId)).thenReturn(new ArrayList<>());

        AccountVO owner = Mockito.mock(AccountVO.class);
        when(accountDao.findByIdIncludingRemoved(accountId)).thenReturn(owner);

        Long bgpPeerId1 = 14L;
        BgpPeerVO bgpPeer1 = Mockito.mock(BgpPeerVO.class);
        when(bgpPeerDao.findById(bgpPeerId1)).thenReturn(bgpPeer1);
        when(routedIpv4Manager.getBgpPeerIdsForAccount(owner, zoneId)).thenReturn(Arrays.asList(bgpPeerId1));

        doReturn(true).when(virtualRouterElement).applyBgpPeers(eq(null), eq(network), any());

        bGPServiceImplSpy.applyBgpPeers(network, true);

        verify(virtualRouterElement).applyBgpPeers(eq(null), eq(network), any());
    }

    @Test
    public void testApplyBgpPeersForVpcWithBgpPeers() throws ResourceUnavailableException {
        Long accountId = 12L;
        Long vpcId = 13L;
        Long zoneId = 1L;
        Vpc vpc = Mockito.mock(Vpc.class);
        when(vpc.getId()).thenReturn(vpcId);
        when(vpc.getAccountId()).thenReturn(accountId);
        when(vpc.getZoneId()).thenReturn(zoneId);

        when(routedIpv4Manager.isDynamicRoutedVpc(vpc)).thenReturn(true);
        when(vpcServiceMapDao.getProviderForServiceInVpc(vpcId, Network.Service.Gateway)).thenReturn("VPCVirtualRouter");
        VpcVirtualRouterElement vpcVirtualRouterElement = Mockito.mock(VpcVirtualRouterElement.class);
        when(networkModel.getElementImplementingProvider("VPCVirtualRouter")).thenReturn(vpcVirtualRouterElement);

        when(bgpPeerDao.listNonRevokeByVpcId(vpcId)).thenReturn(new ArrayList<>());

        AccountVO owner = Mockito.mock(AccountVO.class);
        when(accountDao.findByIdIncludingRemoved(accountId)).thenReturn(owner);

        Long bgpPeerId1 = 14L;
        BgpPeerVO bgpPeer1 = Mockito.mock(BgpPeerVO.class);
        when(bgpPeerDao.findById(bgpPeerId1)).thenReturn(bgpPeer1);
        when(routedIpv4Manager.getBgpPeerIdsForAccount(owner, zoneId)).thenReturn(Arrays.asList(bgpPeerId1));

        doReturn(true).when(vpcVirtualRouterElement).applyBgpPeers(eq(vpc), eq(null), any());

        bGPServiceImplSpy.applyBgpPeers(vpc, true);

        verify(vpcVirtualRouterElement).applyBgpPeers(eq(vpc), eq(null), any());
    }
}
