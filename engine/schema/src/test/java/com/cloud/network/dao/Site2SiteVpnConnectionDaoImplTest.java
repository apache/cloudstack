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
package com.cloud.network.dao;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Site2SiteVpnConnectionDaoImplTest {

    @Spy
    private Site2SiteVpnConnectionDaoImpl dao;
    @Mock
    private SearchBuilder<Site2SiteVpnConnectionVO> stateSearch;
    @Mock
    private SearchCriteria<Site2SiteVpnConnectionVO> searchCriteria;

    @Test
    public void testListByStatesUsesStateSearchCriteria() {
        ReflectionTestUtils.setField(dao, "StateSearch", stateSearch);
        when(stateSearch.create()).thenReturn(searchCriteria);
        doReturn(List.of()).when(dao).listBy(searchCriteria);
        Site2SiteVpnConnection.State[] states = {
                Site2SiteVpnConnection.State.Pending,
                Site2SiteVpnConnection.State.Connecting,
                Site2SiteVpnConnection.State.Connected,
                Site2SiteVpnConnection.State.Disconnected
        };

        dao.listByStates(states);

        verify(searchCriteria).setParameters("state", (Object[]) states);
        verify(dao).listBy(searchCriteria);
    }
}
