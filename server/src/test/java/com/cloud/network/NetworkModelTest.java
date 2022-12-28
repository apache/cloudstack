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
import java.util.Set;

import com.cloud.user.AccountManager;
import org.apache.cloudstack.network.NetworkPermissionVO;
import org.apache.cloudstack.network.dao.NetworkPermissionDao;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.Network.Provider;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDomainDao;
import com.cloud.network.dao.NetworkDomainVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;

import junit.framework.Assert;

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
    @Mock
    private AccountDao accountDao;
    @Mock
    private NetworkDao networkDao;
    @Mock
    private NetworkPermissionDao networkPermissionDao;
    @Mock
    private NetworkDomainDao networkDomainDao;
    @Mock
    private DomainManager domainManager;
    @Mock
    private DomainDao domainDao;
    @Mock
    private ProjectDao projectDao;
    @Mock
    private AccountManager _accountMgr;

    private static final long ZONE_1_ID = 1L;
    private static final long ZONE_2_ID = 2L;
    private static final long PHYSICAL_NETWORK_1_ID = 1L;
    private static final long PHYSICAL_NETWORK_2_ID = 2L;

    private static final String IPV6_CIDR = "fd59:16ba:559b:243d::/64";
    private static final String IPV6_GATEWAY = "fd59:16ba:559b:243d::1";
    private static final String START_IPV6 = "fd59:16ba:559b:243d:0:0:0:2";
    private static final String END_IPV6 = "fd59:16ba:559b:243d:ffff:ffff:ffff:ffff";

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

    @Test
    public void checkIp6ParametersTestAllGood() {
        networkModel.checkIp6Parameters(START_IPV6, END_IPV6, IPV6_GATEWAY,IPV6_CIDR);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void checkIp6ParametersTestCidr32() {
        String ipv6cidr = "fd59:16ba:559b:243d::/32";
        String endipv6 = "fd59:16ba:ffff:ffff:ffff:ffff:ffff:ffff";
        networkModel.checkIp6Parameters(START_IPV6, endipv6, IPV6_GATEWAY,ipv6cidr);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void checkIp6ParametersTestCidr63() {
        String ipv6cidr = "fd59:16ba:559b:243d::/63";
        String endipv6 = "fd59:16ba:559b:243d:ffff:ffff:ffff:ffff";
        networkModel.checkIp6Parameters(START_IPV6, endipv6, IPV6_GATEWAY,ipv6cidr);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void checkIp6ParametersTestCidr65() {
        String ipv6cidr = "fd59:16ba:559b:243d::/65";
        String endipv6 = "fd59:16ba:559b:243d:7fff:ffff:ffff:ffff";
        networkModel.checkIp6Parameters(START_IPV6, endipv6, IPV6_GATEWAY,ipv6cidr);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void checkIp6ParametersTestCidr120() {
        String ipv6cidr = "fd59:16ba:559b:243d::/120";
        String endipv6 = "fd59:16ba:559b:243d:0:0:0:ff";
        networkModel.checkIp6Parameters(START_IPV6, endipv6, IPV6_GATEWAY,ipv6cidr);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void checkIp6ParametersTestNullGateway() {
        networkModel.checkIp6Parameters(START_IPV6, END_IPV6, null,IPV6_CIDR);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void checkIp6ParametersTestNullCidr() {
        networkModel.checkIp6Parameters(START_IPV6, END_IPV6, IPV6_GATEWAY,null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void checkIp6ParametersTestNullCidrAndNulGateway() {
        networkModel.checkIp6Parameters(START_IPV6, END_IPV6, null,null);
    }

    @Test
    public void checkIp6ParametersTestNullStartIpv6() {
        networkModel.checkIp6Parameters(null, END_IPV6, IPV6_GATEWAY,IPV6_CIDR);
    }

    @Test
    public void checkIp6ParametersTestNullEndIpv6() {
        networkModel.checkIp6Parameters(START_IPV6, null, IPV6_GATEWAY,IPV6_CIDR);
    }

    @Test
    public void checkIp6ParametersTestNullStartAndEndIpv6() {
        networkModel.checkIp6Parameters(null, null, IPV6_GATEWAY,IPV6_CIDR);
    }

    @Test
    public void testCheckNetworkPermissions() {
        long accountId = 1L;
        AccountVO caller = mock(AccountVO.class);
        when(caller.getId()).thenReturn(accountId);
        when(caller.getType()).thenReturn(Account.Type.NORMAL);
        NetworkVO network = mock(NetworkVO.class);
        when(network.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(network.getAccountId()).thenReturn(accountId);
        when(accountDao.findById(accountId)).thenReturn(caller);
        when(networkDao.listBy(caller.getId(), network.getId())).thenReturn(List.of(network));
        when(networkPermissionDao.findByNetworkAndAccount(network.getId(), caller.getId())).thenReturn(mock(NetworkPermissionVO.class));
        networkModel.checkNetworkPermissions(caller, network);
    }

    @Test
    public void testCheckNetworkPermissionsForAdmin() {
        long accountId = 1L;
        AccountVO caller = mock(AccountVO.class);
        when(caller.getId()).thenReturn(accountId);
        when(caller.getType()).thenReturn(Account.Type.ADMIN);
        NetworkVO network = mock(NetworkVO.class);
        when(network.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(network.getAccountId()).thenReturn(accountId);
        when(accountDao.findById(accountId)).thenReturn(caller);
        when(networkDao.listBy(caller.getId(), network.getId())).thenReturn(List.of(network));
        when(networkPermissionDao.findByNetworkAndAccount(network.getId(), caller.getId())).thenReturn(mock(NetworkPermissionVO.class));
        networkModel.checkNetworkPermissions(caller, network);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCheckNetworkPermissionsNullNetwork() {
        AccountVO caller = mock(AccountVO.class);
        NetworkVO network = null;
        networkModel.checkNetworkPermissions(caller, network);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testCheckNetworkPermissionsNoOwner() {
        long accountId = 1L;
        AccountVO caller = mock(AccountVO.class);
        when(caller.getId()).thenReturn(accountId);
        when(caller.getType()).thenReturn(Account.Type.NORMAL);
        NetworkVO network = mock(NetworkVO.class);
        when(network.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(network.getAccountId()).thenReturn(accountId);
        when(accountDao.findById(accountId)).thenReturn(null);
        networkModel.checkNetworkPermissions(caller, network);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testCheckNetworkPermissionsNoPermission() {
        long accountId = 1L;
        AccountVO caller = mock(AccountVO.class);
        when(caller.getId()).thenReturn(accountId);
        when(caller.getType()).thenReturn(Account.Type.NORMAL);
        NetworkVO network = mock(NetworkVO.class);
        when(network.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(network.getAccountId()).thenReturn(accountId);
        when(accountDao.findById(accountId)).thenReturn(caller);
        when(networkDao.listBy(caller.getId(), network.getId())).thenReturn(null);
        when(networkPermissionDao.findByNetworkAndAccount(network.getId(), caller.getId())).thenReturn(null);
        networkModel.checkNetworkPermissions(caller, network);
    }

    @Test
    public void testCheckNetworkPermissionsSharedNetwork() {
        long id = 1L;
        long subDomainId = 2L;
        AccountVO caller = mock(AccountVO.class);
        when(caller.getId()).thenReturn(id);
        when(caller.getDomainId()).thenReturn(id);
        when(caller.getType()).thenReturn(Account.Type.NORMAL);
        NetworkVO network = mock(NetworkVO.class);
        when(network.getGuestType()).thenReturn(Network.GuestType.Shared);
        when(network.getId()).thenReturn(id);
        when(networkDao.findById(network.getId())).thenReturn(network);
        NetworkDomainVO networkDomainVO = mock(NetworkDomainVO.class);
        when(networkDomainVO.getDomainId()).thenReturn(id);
        when(networkDomainDao.getDomainNetworkMapByNetworkId(id)).thenReturn(networkDomainVO);
        networkModel.checkNetworkPermissions(caller, network);
        when(caller.getDomainId()).thenReturn(subDomainId);
        networkDomainVO.subdomainAccess = Boolean.TRUE;
        when(domainManager.getDomainParentIds(subDomainId)).thenReturn(Set.of(id));
        networkModel.checkNetworkPermissions(caller, network);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testCheckNetworkPermissionsSharedNetworkNoSubDomainAccess() {
        long id = 1L;
        long subDomainId = 2L;
        AccountVO caller = mock(AccountVO.class);
        when(caller.getId()).thenReturn(id);
        when(caller.getDomainId()).thenReturn(subDomainId);
        when(caller.getType()).thenReturn(Account.Type.NORMAL);
        NetworkVO network = mock(NetworkVO.class);
        when(network.getGuestType()).thenReturn(Network.GuestType.Shared);
        when(network.getId()).thenReturn(id);
        when(networkDao.findById(network.getId())).thenReturn(network);
        when(domainDao.findById(caller.getDomainId())).thenReturn(mock(DomainVO.class));
        NetworkDomainVO networkDomainVO = mock(NetworkDomainVO.class);
        when(networkDomainVO.getDomainId()).thenReturn(id);
        networkDomainVO.subdomainAccess = Boolean.FALSE;
        when(networkDomainDao.getDomainNetworkMapByNetworkId(id)).thenReturn(networkDomainVO);
        networkModel.checkNetworkPermissions(caller, network);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testCheckNetworkPermissionsSharedNetworkNotSubDomain() {
        long id = 1L;
        long subDomainId = 2L;
        AccountVO caller = mock(AccountVO.class);
        when(caller.getId()).thenReturn(id);
        when(caller.getDomainId()).thenReturn(subDomainId);
        when(caller.getType()).thenReturn(Account.Type.NORMAL);
        NetworkVO network = mock(NetworkVO.class);
        when(network.getGuestType()).thenReturn(Network.GuestType.Shared);
        when(network.getId()).thenReturn(id);
        when(networkDao.findById(network.getId())).thenReturn(network);
        when(domainDao.findById(caller.getDomainId())).thenReturn(mock(DomainVO.class));
        NetworkDomainVO networkDomainVO = mock(NetworkDomainVO.class);
        when(networkDomainVO.getDomainId()).thenReturn(id);
        networkDomainVO.subdomainAccess = Boolean.TRUE;
        when(networkDomainDao.getDomainNetworkMapByNetworkId(id)).thenReturn(networkDomainVO);
        when(domainManager.getDomainParentIds(subDomainId)).thenReturn(Set.of(0L));
        networkModel.checkNetworkPermissions(caller, network);
    }
}
