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

package com.cloud.network;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.VlanDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.user.Account;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.net.Ip;
import com.cloud.network.Network.Provider;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class NetworkModelTest {

    @Mock
    private DataCenterDao dataCenterDao;
    @Mock
    private PhysicalNetworkDao physicalNetworkDao;
    @Mock
    private PhysicalNetworkServiceProviderDao physicalNetworkServiceProviderDao;
    @Mock
    private NetworkService networkService;

    @InjectMocks
    @Spy
    private NetworkModelImpl networkModel = new NetworkModelImpl();

    @Mock
    private DataCenterVO zone1;
    @Mock
    private DataCenterVO zone2;
    @Mock
    private PhysicalNetworkVO physicalNetworkZone1;
    @Mock
    private PhysicalNetworkVO physicalNetworkZone2;
    @Mock
    private PhysicalNetworkServiceProviderVO providerVO;

    private static final long ZONE_1_ID = 1L;
    private static final long ZONE_2_ID = 2L;
    private static final long PHYSICAL_NETWORK_1_ID = 1L;
    private static final long PHYSICAL_NETWORK_2_ID = 2L;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(dataCenterDao.listEnabledZones()).thenReturn(Arrays.asList(zone1, zone2));
        when(physicalNetworkDao.listByZoneAndTrafficType(ZONE_1_ID, Networks.TrafficType.Guest)).
                thenReturn(Collections.singletonList(physicalNetworkZone1));
        when(physicalNetworkDao.listByZoneAndTrafficType(ZONE_2_ID, Networks.TrafficType.Guest)).
                thenReturn(Collections.singletonList(physicalNetworkZone2));
        when(physicalNetworkServiceProviderDao.findByServiceProvider(
                PHYSICAL_NETWORK_1_ID, Network.Provider.ConfigDrive.getName())).thenReturn(null);
        when(physicalNetworkServiceProviderDao.findByServiceProvider(
                PHYSICAL_NETWORK_2_ID, Network.Provider.ConfigDrive.getName())).thenReturn(null);

        when(zone1.getNetworkType()).thenReturn(DataCenter.NetworkType.Advanced);
        when(zone1.getId()).thenReturn(ZONE_1_ID);

        when(zone2.getNetworkType()).thenReturn(DataCenter.NetworkType.Advanced);
        when(zone2.getId()).thenReturn(ZONE_2_ID);

        when(physicalNetworkZone1.getId()).thenReturn(PHYSICAL_NETWORK_1_ID);
        when(physicalNetworkZone2.getId()).thenReturn(PHYSICAL_NETWORK_2_ID);
    }

    @Test
    public void testGetSourceNatIpAddressForGuestNetwork() {
        NetworkModelImpl modelImpl = new NetworkModelImpl();
        IPAddressDao ipAddressDao = mock(IPAddressDao.class);
        modelImpl._ipAddressDao = ipAddressDao;
        List<IPAddressVO> fakeList = new ArrayList<IPAddressVO>();
        IPAddressVO fakeIp = new IPAddressVO(new Ip("75.75.75.75"), 1, 0xaabbccddeeffL, 10, false);
        fakeList.add(fakeIp);
        SearchBuilder<IPAddressVO> fakeSearch = mock(SearchBuilder.class);
        modelImpl.IpAddressSearch = fakeSearch;
        VlanDao fakeVlanDao = mock(VlanDao.class);
        when(fakeVlanDao.findById(anyLong())).thenReturn(mock(VlanVO.class));
        modelImpl._vlanDao = fakeVlanDao;
        when(fakeSearch.create()).thenReturn(mock(SearchCriteria.class));
        when(ipAddressDao.search(any(SearchCriteria.class), (Filter) isNull())).thenReturn(fakeList);
        when(ipAddressDao.findById(anyLong())).thenReturn(fakeIp);
        Account fakeAccount = mock(Account.class);
        when(fakeAccount.getId()).thenReturn(1L);
        Network fakeNetwork = mock(Network.class);
        when(fakeNetwork.getId()).thenReturn(1L);
        PublicIpAddress answer = modelImpl.getSourceNatIpAddressForGuestNetwork(fakeAccount, fakeNetwork);
        Assert.assertNull(answer);
        IPAddressVO fakeIp2 = new IPAddressVO(new Ip("76.75.75.75"), 1, 0xaabb10ddeeffL, 10, true);
        fakeList.add(fakeIp2);
        when(ipAddressDao.findById(anyLong())).thenReturn(fakeIp2);
        answer = modelImpl.getSourceNatIpAddressForGuestNetwork(fakeAccount, fakeNetwork);
        Assert.assertNotNull(answer);
        Assert.assertEquals(answer.getAddress().addr(), "76.75.75.75");

    }

    @Test
    public void testVerifyDisabledConfigDriveEntriesOnZonesBothEnabledZones() {
        networkModel.verifyDisabledConfigDriveEntriesOnEnabledZones();
        verify(networkModel, times(2)).addDisabledConfigDriveEntriesOnZone(any(DataCenterVO.class));
    }

    @Test
    public void testVerifyDisabledConfigDriveEntriesOnZonesOneEnabledZone() {
        when(dataCenterDao.listEnabledZones()).thenReturn(Collections.singletonList(zone1));

        networkModel.verifyDisabledConfigDriveEntriesOnEnabledZones();
        verify(networkModel).addDisabledConfigDriveEntriesOnZone(any(DataCenterVO.class));
    }

    @Test
    public void testVerifyDisabledConfigDriveEntriesOnZonesNoEnabledZones() {
        when(dataCenterDao.listEnabledZones()).thenReturn(null);

        networkModel.verifyDisabledConfigDriveEntriesOnEnabledZones();
        verify(networkModel, never()).addDisabledConfigDriveEntriesOnZone(any(DataCenterVO.class));
    }

    @Test
    public void testAddDisabledConfigDriveEntriesOnZoneBasicZone() {
        when(zone1.getNetworkType()).thenReturn(DataCenter.NetworkType.Basic);

        networkModel.addDisabledConfigDriveEntriesOnZone(zone1);
        verify(physicalNetworkDao, never()).listByZoneAndTrafficType(ZONE_1_ID, Networks.TrafficType.Guest);
        verify(networkService, never()).
                addProviderToPhysicalNetwork(anyLong(), eq(Provider.ConfigDrive.getName()), isNull(Long.class), isNull(List.class));
    }

    @Test
    public void testAddDisabledConfigDriveEntriesOnZoneAdvancedZoneExistingConfigDrive() {
        when(physicalNetworkServiceProviderDao.findByServiceProvider(
                PHYSICAL_NETWORK_1_ID, Network.Provider.ConfigDrive.getName())).thenReturn(providerVO);

        networkModel.addDisabledConfigDriveEntriesOnZone(zone1);
        verify(networkService, never()).
                addProviderToPhysicalNetwork(anyLong(), eq(Provider.ConfigDrive.getName()), isNull(Long.class), isNull(List.class));
    }

    @Test
    public void testAddDisabledConfigDriveEntriesOnZoneAdvancedZoneNonExistingConfigDrive() {
        networkModel.addDisabledConfigDriveEntriesOnZone(zone1);
        verify(networkService).
                addProviderToPhysicalNetwork(anyLong(), eq(Provider.ConfigDrive.getName()), isNull(Long.class), isNull(List.class));
    }

}
