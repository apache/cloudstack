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
package com.cloud.network.vpn;

import java.util.ArrayList;

import org.apache.cloudstack.api.command.user.vpn.ListVpnConnectionsCmd;
import org.apache.cloudstack.api.command.user.vpn.ListVpnCustomerGatewaysCmd;
import org.apache.cloudstack.api.command.user.vpn.ListVpnGatewaysCmd;
import org.apache.cloudstack.context.CallContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.network.dao.Site2SiteCustomerGatewayDao;
import com.cloud.network.dao.Site2SiteCustomerGatewayVO;
import com.cloud.network.dao.Site2SiteVpnConnectionDao;
import com.cloud.network.dao.Site2SiteVpnConnectionVO;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.dao.Site2SiteVpnGatewayVO;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class Site2SiteVpnManagerImplTest {

    @Mock
    private Site2SiteCustomerGatewayDao customerGatewayDao;
    @Mock
    private Site2SiteVpnGatewayDao vpnGatewayDao;
    @Mock
    private Site2SiteVpnConnectionDao vpnConnectionDao;
    @Mock
    private AccountManager accountManager;

    @InjectMocks
    private Site2SiteVpnManagerImpl site2SiteVpnManager;

    private MockedStatic<CallContext> callContextMocked;

    @Before
    public void setUp() {
        callContextMocked = Mockito.mockStatic(CallContext.class);
        callContextMocked.when(CallContext::current).thenReturn(Mockito.mock(CallContext.class));
    }

    @After
    public void tearDown() {
        callContextMocked.close();
    }

    @SuppressWarnings("unchecked")
    private <T> SearchBuilder<T> mockSearchBuilder(T entity) {
        SearchBuilder<T> sb = Mockito.mock(SearchBuilder.class);
        Mockito.when(sb.entity()).thenReturn(entity);
        Mockito.when(sb.create()).thenReturn(Mockito.mock(SearchCriteria.class));
        return sb;
    }

    private void assertUnlimitedFilter(Filter filter) {
        Assert.assertNull(filter.getOffset());
        Assert.assertNull(filter.getLimit());
    }

    @Test
    public void searchForCustomerGatewaysWithUnlimitedPageSizeTest() {
        ListVpnCustomerGatewaysCmd cmd = Mockito.mock(ListVpnCustomerGatewaysCmd.class);
        Mockito.when(cmd.getStartIndex()).thenReturn(null);
        Mockito.when(cmd.getPageSizeVal()).thenReturn(null);
        SearchBuilder<Site2SiteCustomerGatewayVO> sb = mockSearchBuilder(Mockito.mock(Site2SiteCustomerGatewayVO.class));
        Mockito.when(customerGatewayDao.createSearchBuilder()).thenReturn(sb);
        ArgumentCaptor<Filter> filterCaptor = ArgumentCaptor.forClass(Filter.class);
        Mockito.when(customerGatewayDao.searchAndCount(Mockito.any(), filterCaptor.capture())).thenReturn(new Pair<>(new ArrayList<>(), 0));

        site2SiteVpnManager.searchForCustomerGateways(cmd);

        assertUnlimitedFilter(filterCaptor.getValue());
    }

    @Test
    public void searchForVpnGatewaysWithUnlimitedPageSizeTest() {
        ListVpnGatewaysCmd cmd = Mockito.mock(ListVpnGatewaysCmd.class);
        Mockito.when(cmd.getStartIndex()).thenReturn(null);
        Mockito.when(cmd.getPageSizeVal()).thenReturn(null);
        SearchBuilder<Site2SiteVpnGatewayVO> sb = mockSearchBuilder(Mockito.mock(Site2SiteVpnGatewayVO.class));
        Mockito.when(vpnGatewayDao.createSearchBuilder()).thenReturn(sb);
        ArgumentCaptor<Filter> filterCaptor = ArgumentCaptor.forClass(Filter.class);
        Mockito.when(vpnGatewayDao.searchAndCount(Mockito.any(), filterCaptor.capture())).thenReturn(new Pair<>(new ArrayList<>(), 0));

        site2SiteVpnManager.searchForVpnGateways(cmd);

        assertUnlimitedFilter(filterCaptor.getValue());
    }

    @Test
    public void searchForVpnConnectionsWithUnlimitedPageSizeTest() {
        ListVpnConnectionsCmd cmd = Mockito.mock(ListVpnConnectionsCmd.class);
        Mockito.when(cmd.getVpcId()).thenReturn(null);
        Mockito.when(cmd.getStartIndex()).thenReturn(null);
        Mockito.when(cmd.getPageSizeVal()).thenReturn(null);
        SearchBuilder<Site2SiteVpnConnectionVO> sb = mockSearchBuilder(Mockito.mock(Site2SiteVpnConnectionVO.class));
        Mockito.when(vpnConnectionDao.createSearchBuilder()).thenReturn(sb);
        ArgumentCaptor<Filter> filterCaptor = ArgumentCaptor.forClass(Filter.class);
        Mockito.when(vpnConnectionDao.searchAndCount(Mockito.any(), filterCaptor.capture())).thenReturn(new Pair<>(new ArrayList<>(), 0));

        site2SiteVpnManager.searchForVpnConnections(cmd);

        assertUnlimitedFilter(filterCaptor.getValue());
    }
}
