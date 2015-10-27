//
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

package com.cloud.network.manager;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.sync.SyncNuageVspCmsIdAnswer;
import com.cloud.api.commands.DeleteNuageVspDeviceCmd;
import com.cloud.api.commands.ListNuageVspDevicesCmd;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.NuageVspDeviceVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.NuageVspDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.resource.ResourceManager;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.junit.Before;
import org.junit.Test;

import javax.naming.ConfigurationException;
import java.util.ArrayList;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NuageVspManagerTest {
    private static final long NETWORK_ID = 42L;

    PhysicalNetworkDao physicalNetworkDao = mock(PhysicalNetworkDao.class);
    PhysicalNetworkServiceProviderDao physicalNetworkServiceProviderDao = mock(PhysicalNetworkServiceProviderDao.class);
    ResourceManager resourceMgr = mock(ResourceManager.class);
    HostDetailsDao hostDetailsDao = mock(HostDetailsDao.class);
    NuageVspDao nuageVspDao = mock(NuageVspDao.class);
    NetworkDao networkDao = mock(NetworkDao.class);
    HostDao hostDao = mock(HostDao.class);
    AgentManager agentManager = mock(AgentManager.class);
    ConfigurationDao configDao = mock(ConfigurationDao.class);

    NuageVspManagerImpl manager;

    @Before
    public void setUp() {
        manager = new NuageVspManagerImpl();

        manager._physicalNetworkServiceProviderDao = physicalNetworkServiceProviderDao;
        manager._physicalNetworkDao = physicalNetworkDao;
        manager._resourceMgr = resourceMgr;
        manager._hostDetailsDao = hostDetailsDao;
        manager._nuageVspDao = nuageVspDao;
        manager._networkDao = networkDao;
        manager._hostDao = hostDao;
        manager._agentMgr = agentManager;
        manager._configDao = configDao;
    }

    @Test
    public void testDeleteNuageVspDevice() throws ConfigurationException {

        final PhysicalNetworkVO physicalNetwork = mock(PhysicalNetworkVO.class);
        when(physicalNetwork.getDataCenterId()).thenReturn(NETWORK_ID);
        when(physicalNetwork.getId()).thenReturn(NETWORK_ID);
        when(physicalNetworkDao.findById(NETWORK_ID)).thenReturn(physicalNetwork);

        final NuageVspDeviceVO nuageVspDevice = mock(NuageVspDeviceVO.class);
        when(nuageVspDevice.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(nuageVspDevice.getHostId()).thenReturn(NETWORK_ID);
        when(nuageVspDao.findById(NETWORK_ID)).thenReturn(nuageVspDevice);

        when(networkDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(new ArrayList<NetworkVO>());

        final HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(NETWORK_ID);
        when(hostDao.findById(NETWORK_ID)).thenReturn(host);

        final DeleteNuageVspDeviceCmd cmd = mock(DeleteNuageVspDeviceCmd.class);
        when(cmd.getNuageVspDeviceId()).thenReturn(NETWORK_ID);

        ConfigurationVO cmsIdConfig = mock(ConfigurationVO.class);
        when(cmsIdConfig.getValue()).thenReturn("1:1");
        when(configDao.findByName("nuagevsp.cms.id")).thenReturn(cmsIdConfig);

        final SyncNuageVspCmsIdAnswer answer = mock(SyncNuageVspCmsIdAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentManager.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);

        manager.deleteNuageVspDevice(cmd);
    }

    @Test
    public void testListNuageVspDevices() {
        final NuageVspDeviceVO nuageVspDevice = mock(NuageVspDeviceVO.class);
        when(nuageVspDevice.getPhysicalNetworkId()).thenReturn(NETWORK_ID);

        final PhysicalNetworkVO phyNtwkVO = mock(PhysicalNetworkVO.class);
        when(physicalNetworkDao.findById(NETWORK_ID)).thenReturn(phyNtwkVO);
        when(nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(new ArrayList<NuageVspDeviceVO>());

        final ListNuageVspDevicesCmd cmd = mock(ListNuageVspDevicesCmd.class);
        when(cmd.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(cmd.getNuageVspDeviceId()).thenReturn(null);
        manager.listNuageVspDevices(cmd);
    }
}
