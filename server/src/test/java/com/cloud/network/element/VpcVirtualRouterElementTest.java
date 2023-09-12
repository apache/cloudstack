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
package com.cloud.network.element;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.network.topology.AdvancedNetworkTopology;
import org.apache.cloudstack.network.topology.BasicNetworkTopology;
import org.apache.cloudstack.network.topology.NetworkTopologyContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VpnUser;
import com.cloud.network.router.VpcVirtualNetworkApplianceManagerImpl;
import com.cloud.network.vpc.Vpc;
import com.cloud.utils.db.EntityManager;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.dao.DomainRouterDao;

@RunWith(MockitoJUnitRunner.class)
public class VpcVirtualRouterElementTest {
    @Mock
    DataCenterDao _dcDao;
    @Mock private DomainRouterDao _routerDao;

    @Mock
    EntityManager _entityMgr;

    @Mock
    NetworkTopologyContext networkTopologyContext;

    @InjectMocks
    VpcVirtualNetworkApplianceManagerImpl _vpcRouterMgr;

    @InjectMocks
    VpcVirtualRouterElement vpcVirtualRouterElement;


    @Test
    public void testApplyVpnUsers() {
        vpcVirtualRouterElement._vpcRouterMgr = _vpcRouterMgr;

        final AdvancedNetworkTopology advancedNetworkTopology = Mockito.mock(AdvancedNetworkTopology.class);
        final BasicNetworkTopology basicNetworkTopology = Mockito.mock(BasicNetworkTopology.class);

        networkTopologyContext.setAdvancedNetworkTopology(advancedNetworkTopology);
        networkTopologyContext.setBasicNetworkTopology(basicNetworkTopology);
        networkTopologyContext.init();

        final Vpc vpc = Mockito.mock(Vpc.class);
        final DataCenterVO dataCenterVO = Mockito.mock(DataCenterVO.class);
        final RemoteAccessVpn remoteAccessVpn = Mockito.mock(RemoteAccessVpn.class);
        final DomainRouterVO domainRouterVO1 = Mockito.mock(DomainRouterVO.class);
        final DomainRouterVO domainRouterVO2 = Mockito.mock(DomainRouterVO.class);
        final VpnUser vpnUser1 = Mockito.mock(VpnUser.class);
        final VpnUser vpnUser2 = Mockito.mock(VpnUser.class);

        final List<VpnUser> users = new ArrayList<VpnUser>();
        users.add(vpnUser1);
        users.add(vpnUser2);

        final List<DomainRouterVO> routers = new ArrayList<DomainRouterVO>();
        routers.add(domainRouterVO1);
        routers.add(domainRouterVO2);

        final Long vpcId = new Long(1l);
        final Long zoneId = new Long(1l);

        when(remoteAccessVpn.getVpcId()).thenReturn(vpcId);
        when(_vpcRouterMgr.getVpcRouters(vpcId)).thenReturn(routers);
        when(_entityMgr.findById(Vpc.class, vpcId)).thenReturn(vpc);
        when(vpc.getZoneId()).thenReturn(zoneId);
        when(_dcDao.findById(zoneId)).thenReturn(dataCenterVO);
        when(networkTopologyContext.retrieveNetworkTopology(dataCenterVO)).thenReturn(advancedNetworkTopology);

        try {
            when(advancedNetworkTopology.applyVpnUsers(remoteAccessVpn, users, domainRouterVO1)).thenReturn(new String[]{"user1", "user2"});
            when(advancedNetworkTopology.applyVpnUsers(remoteAccessVpn, users, domainRouterVO2)).thenReturn(new String[]{"user3", "user4"});
        } catch (final ResourceUnavailableException e) {
            fail(e.getMessage());
        }

        try {
            final String [] results = vpcVirtualRouterElement.applyVpnUsers(remoteAccessVpn, users);

            assertNotNull(results);
            assertEquals(results[0], "user1");
            assertEquals(results[1], "user2");
            assertEquals(results[2], "user3");
            assertEquals(results[3], "user4");
        } catch (final ResourceUnavailableException e) {
            fail(e.getMessage());
        }

        verify(remoteAccessVpn, times(1)).getVpcId();
        verify(vpc, times(1)).getZoneId();
        verify(_dcDao, times(1)).findById(zoneId);
        verify(networkTopologyContext, times(1)).retrieveNetworkTopology(dataCenterVO);
    }

    @Test
    public void testApplyVpnUsersException1() {
        vpcVirtualRouterElement._vpcRouterMgr = _vpcRouterMgr;

        final AdvancedNetworkTopology advancedNetworkTopology = Mockito.mock(AdvancedNetworkTopology.class);
        final BasicNetworkTopology basicNetworkTopology = Mockito.mock(BasicNetworkTopology.class);

        networkTopologyContext.setAdvancedNetworkTopology(advancedNetworkTopology);
        networkTopologyContext.setBasicNetworkTopology(basicNetworkTopology);
        networkTopologyContext.init();

        final RemoteAccessVpn remoteAccessVpn = Mockito.mock(RemoteAccessVpn.class);
        final List<VpnUser> users = new ArrayList<VpnUser>();

        when(remoteAccessVpn.getVpcId()).thenReturn(null);

        try {
            final String [] results = vpcVirtualRouterElement.applyVpnUsers(remoteAccessVpn, users);
            assertNull(results);
        } catch (final ResourceUnavailableException e) {
            fail(e.getMessage());
        }

        verify(remoteAccessVpn, times(1)).getVpcId();
    }

    @Test
    public void testApplyVpnUsersException2() {
        vpcVirtualRouterElement._vpcRouterMgr = _vpcRouterMgr;

        final AdvancedNetworkTopology advancedNetworkTopology = Mockito.mock(AdvancedNetworkTopology.class);
        final BasicNetworkTopology basicNetworkTopology = Mockito.mock(BasicNetworkTopology.class);

        networkTopologyContext.setAdvancedNetworkTopology(advancedNetworkTopology);
        networkTopologyContext.setBasicNetworkTopology(basicNetworkTopology);
        networkTopologyContext.init();

        final RemoteAccessVpn remoteAccessVpn = Mockito.mock(RemoteAccessVpn.class);

        final List<VpnUser> users = new ArrayList<VpnUser>();

        final Long vpcId = new Long(1l);

        when(remoteAccessVpn.getVpcId()).thenReturn(vpcId);
        when(_vpcRouterMgr.getVpcRouters(vpcId)).thenReturn(null);

        try {
            final String [] results = vpcVirtualRouterElement.applyVpnUsers(remoteAccessVpn, users);

            assertNull(results);
        } catch (final ResourceUnavailableException e) {
            fail(e.getMessage());
        }

        verify(remoteAccessVpn, times(1)).getVpcId();
    }
}
