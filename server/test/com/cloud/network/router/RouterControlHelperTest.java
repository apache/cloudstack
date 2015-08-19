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
package com.cloud.network.router;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;

@RunWith(MockitoJUnitRunner.class)
public class RouterControlHelperTest {

    private static final String DIDN_T_GET_THE_EXPECTED_IP4_ADDRESS = "Didn't get the expected IP4 address";
    private static final String IP4_ADDRES1 = "IP4Addres1";
    private static final String IP4_ADDRES2 = "IP4Addres2";
    protected static final long ROUTER_ID = 1L;
    protected static final long NW_ID_1 = 11L;
    protected static final long NW_ID_2 = 12L;
    protected static final long NW_ID_3 = 13L;

    @Mock
    protected NicDao nicDao;
    @Mock
    protected NetworkDao nwDao;
    @Mock
    protected DomainRouterDao routerDao;

    @InjectMocks
    protected RouterControlHelper routerControlHelper = new RouterControlHelper();

    @Test
    public void testGetRouterControlIp() {
        // Prepare
        List<NicVO> nics = new ArrayList<>();
        NicVO nic1 = mock(NicVO.class);
        NicVO nic2 = mock(NicVO.class);
        // Actually the third one will never be used, but we must assert that is not
        NicVO nic3 = mock(NicVO.class);
        when(nic1.getNetworkId()).thenReturn(NW_ID_1);
        when(nic2.getNetworkId()).thenReturn(NW_ID_2);
        when(nic2.getIPv4Address()).thenReturn(IP4_ADDRES1);
        when(nic3.getNetworkId()).thenReturn(NW_ID_3);
        when(nic3.getIPv4Address()).thenReturn(IP4_ADDRES2);
        nics.add(nic1);
        nics.add(nic2);
        nics.add(nic3);
        when(this.nicDao.listByVmId(ROUTER_ID)).thenReturn(nics);

        NetworkVO nw1 = mock(NetworkVO.class);
        when(nw1.getTrafficType()).thenReturn(TrafficType.Public);
        NetworkVO nw2 = mock(NetworkVO.class);
        when(nw2.getTrafficType()).thenReturn(TrafficType.Control);
        NetworkVO nw3 = mock(NetworkVO.class);
        when(nw3.getTrafficType()).thenReturn(TrafficType.Control);
        when(this.nwDao.findById(NW_ID_1)).thenReturn(nw1);
        when(this.nwDao.findById(NW_ID_2)).thenReturn(nw2);
        when(this.nwDao.findById(NW_ID_3)).thenReturn(nw3);

        // Execute
        final String ip4address = this.routerControlHelper.getRouterControlIp(ROUTER_ID);

        // Assert
        assertEquals(DIDN_T_GET_THE_EXPECTED_IP4_ADDRESS, IP4_ADDRES1, ip4address);
    }

    @Test
    public void testGetRouterControlIpWithRouterIp() {
        // Prepare
        List<NicVO> nics = new ArrayList<>();
        NicVO nic1 = mock(NicVO.class);
        when(nic1.getNetworkId()).thenReturn(NW_ID_1);
        when(nic1.getIPv4Address()).thenReturn(null);
        nics.add(nic1);
        when(this.nicDao.listByVmId(ROUTER_ID)).thenReturn(nics);

        NetworkVO nw1 = mock(NetworkVO.class);
        when(nw1.getTrafficType()).thenReturn(TrafficType.Public);
        when(this.nwDao.findById(NW_ID_1)).thenReturn(nw1);

        DomainRouterVO router = mock(DomainRouterVO.class);
        when(this.routerDao.findById(ROUTER_ID)).thenReturn(router);
        when(router.getPrivateIpAddress()).thenReturn(IP4_ADDRES1);

        // Execute
        final String ip4address = this.routerControlHelper.getRouterControlIp(ROUTER_ID);

        // Assert
        assertEquals(DIDN_T_GET_THE_EXPECTED_IP4_ADDRESS, IP4_ADDRES1, ip4address);
    }

}
