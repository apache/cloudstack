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
package org.apache.cloudstack.network.tungsten.service;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.TungstenProvider;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.resource.ResourceManager;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenProviderCmd;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

public class TungstenProviderServiceTest {

    @Mock
    DataCenterDao dcDao;
    @Mock
    DataCenterVO zone;
    @Mock
    Host host;
    @Mock
    ResourceManager resourceMgr;
    @Mock
    DomainDao domainDao;
    @Mock
    ProjectDao projectDao;
    @Mock
    TungstenProviderDao tungstenProviderDao;
    @Mock
    HostDetailsDao hostDetailsDao;

    TungstenProviderServiceImpl tungstenProviderService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        tungstenProviderService = new TungstenProviderServiceImpl();
        tungstenProviderService._zoneDao = dcDao;
        tungstenProviderService._resourceMgr = resourceMgr;
        tungstenProviderService._domainDao = domainDao;
        tungstenProviderService._projectDao = projectDao;
        tungstenProviderService._tungstenProviderDao = tungstenProviderDao;
        tungstenProviderService._hostDetailsDao = hostDetailsDao;

        when(dcDao.findById(anyLong())).thenReturn(zone);
        when(zone.getName()).thenReturn("ZoneName");
        when(resourceMgr.addHost(anyLong(), any(), any(), anyMap())).thenReturn(host);
        when(host.getId()).thenReturn(1l);
        when(domainDao.listAll()).thenReturn(null);
        when(projectDao.listAll()).thenReturn(null);
    }

    @Test
    public void addTungstenProviderTest() {
        CreateTungstenProviderCmd cmd = Mockito.mock(CreateTungstenProviderCmd.class);
        when(cmd.getZoneId()).thenReturn(1l);
        when(cmd.getName()).thenReturn("TungstenProviderName");
        when(cmd.getHostname()).thenReturn("TungstenProviderHostname");
        when(cmd.getPort()).thenReturn("8082");
        when(cmd.getVrouter()).thenReturn("TungstenProviderVrouter");
        when(cmd.getVrouterPort()).thenReturn("8091");

        try {
            TungstenProvider tungstenProvider = tungstenProviderService.addProvider(cmd);
            assertNotNull(tungstenProvider);
        } catch (CloudRuntimeException e) {
            e.printStackTrace();
            fail("Failed to add Tungsten provider due to internal error.");
        }
    }
}
