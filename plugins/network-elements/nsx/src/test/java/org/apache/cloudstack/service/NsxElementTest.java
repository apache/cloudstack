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
package org.apache.cloudstack.service;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.resource.ResourceManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.vm.ReservationContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NsxElementTest {

    @Mock
    DataCenterDao dataCenterDao;
    @Mock
    NsxServiceImpl nsxService;
    @Mock
    AccountManager accountManager;
    @Mock
    NetworkDao networkDao;
    @Mock
    ResourceManager resourceManager;
    @Mock
    PhysicalNetworkDao physicalNetworkDao;
    @Mock
    NetworkModel networkModel;
    @Mock
    Vpc vpc;
    @Mock
    DataCenterVO zone;
    @Mock
    DataCenterVO dataCenterVO;
    @Mock
    Account account;
    @Mock
    DomainVO domain;
    @Mock
    private VpcOfferingServiceMapDao vpcOfferingServiceMapDao;

    NsxElement nsxElement;
    ReservationContext reservationContext;
    DeployDestination deployDestination;
    @Mock
    DomainDao domainDao;

    @Before
    public void setup() {
        nsxElement = new NsxElement();

        nsxElement.dataCenterDao = dataCenterDao;
        nsxElement.nsxService = nsxService;
        nsxElement.accountMgr = accountManager;
        nsxElement.networkDao = networkDao;
        nsxElement.resourceManager = resourceManager;
        nsxElement.physicalNetworkDao = physicalNetworkDao;
        nsxElement.domainDao = domainDao;
        nsxElement.networkModel = networkModel;
        nsxElement.vpcOfferingServiceMapDao = vpcOfferingServiceMapDao;
        reservationContext = mock(ReservationContext.class);
        deployDestination = mock(DeployDestination.class);

        when(vpc.getZoneId()).thenReturn(1L);
        when(vpc.getAccountId()).thenReturn(2L);
        when(dataCenterVO.getId()).thenReturn(1L);
        when(vpc.getName()).thenReturn("VPC01");
        when(accountManager.getAccount(2L)).thenReturn(account);
        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(domainDao.findById(anyLong())).thenReturn(domain);
        when(vpc.getZoneId()).thenReturn(1L);
        when(vpc.getName()).thenReturn("testVPC");

        PhysicalNetworkVO physicalNetworkVO = new PhysicalNetworkVO();
        physicalNetworkVO.setIsolationMethods(List.of("NSX"));
        List<PhysicalNetworkVO> physicalNetworkVOList = List.of(physicalNetworkVO);

        when(physicalNetworkDao.listByZoneAndTrafficType(1L, Networks.TrafficType.Guest)).thenReturn(physicalNetworkVOList);
    }

    @Test
    public void testImplementVpc() throws ResourceUnavailableException, InsufficientCapacityException {
        // when(nsxService.createVpcNetwork(anyLong(), anyLong(), anyLong(), anyLong(), anyString(), anyBoolean())).thenReturn(true);
        assertTrue(nsxElement.implementVpc(vpc, deployDestination, reservationContext));
    }

    @Test
    public void testShutdownVpc() {
        when(nsxService.deleteVpcNetwork(anyLong(), anyLong(), anyLong(), anyLong(), anyString())).thenReturn(true);

        assertTrue(nsxElement.shutdownVpc(vpc, reservationContext));
    }



}
