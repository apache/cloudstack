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

import com.cloud.NuageTest;
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
import com.cloud.util.NuageVspEntityBuilder;
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

public class NuageVspManagerTest extends NuageTest {
    private static final long NETWORK_ID = 42L;

    private PhysicalNetworkDao _physicalNetworkDao = mock(PhysicalNetworkDao.class);
    private PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao = mock(PhysicalNetworkServiceProviderDao.class);
    private ResourceManager _resourceManager = mock(ResourceManager.class);
    private HostDetailsDao _hostDetailsDao = mock(HostDetailsDao.class);
    private NuageVspDao _nuageVspDao = mock(NuageVspDao.class);
    private NetworkDao _networkDao = mock(NetworkDao.class);
    private HostDao _hostDao = mock(HostDao.class);
    private AgentManager _agentManager = mock(AgentManager.class);
    private ConfigurationDao _configurationDao = mock(ConfigurationDao.class);
    private NuageVspEntityBuilder _nuageVspEntityBuilder = mock(NuageVspEntityBuilder.class);
    private NuageVspManagerImpl _nuageVspManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        _nuageVspManager = new NuageVspManagerImpl();

        _nuageVspManager._physicalNetworkServiceProviderDao = _physicalNetworkServiceProviderDao;
        _nuageVspManager._physicalNetworkDao = _physicalNetworkDao;
        _nuageVspManager._resourceMgr = _resourceManager;
        _nuageVspManager._hostDetailsDao = _hostDetailsDao;
        _nuageVspManager._nuageVspDao = _nuageVspDao;
        _nuageVspManager._networkDao = _networkDao;
        _nuageVspManager._hostDao = _hostDao;
        _nuageVspManager._agentMgr = _agentManager;
        _nuageVspManager._configDao = _configurationDao;
        _nuageVspManager._nuageVspEntityBuilder = _nuageVspEntityBuilder;
    }

    @Test
    public void testDeleteNuageVspDevice() throws ConfigurationException {

        final PhysicalNetworkVO physicalNetwork = mock(PhysicalNetworkVO.class);
        when(physicalNetwork.getDataCenterId()).thenReturn(NETWORK_ID);
        when(physicalNetwork.getId()).thenReturn(NETWORK_ID);
        when(_physicalNetworkDao.findById(NETWORK_ID)).thenReturn(physicalNetwork);

        final NuageVspDeviceVO nuageVspDevice = mock(NuageVspDeviceVO.class);
        when(nuageVspDevice.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(nuageVspDevice.getHostId()).thenReturn(NETWORK_ID);
        when(_nuageVspDao.findById(NETWORK_ID)).thenReturn(nuageVspDevice);

        when(_networkDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(new ArrayList<NetworkVO>());

        final HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(NETWORK_ID);
        when(_hostDao.findById(NETWORK_ID)).thenReturn(host);

        final DeleteNuageVspDeviceCmd cmd = mock(DeleteNuageVspDeviceCmd.class);
        when(cmd.getNuageVspDeviceId()).thenReturn(NETWORK_ID);

        ConfigurationVO cmsIdConfig = mock(ConfigurationVO.class);
        when(cmsIdConfig.getValue()).thenReturn("1:1");
        when(_configurationDao.findByName("nuagevsp.cms.id")).thenReturn(cmsIdConfig);

        final SyncNuageVspCmsIdAnswer answer = mock(SyncNuageVspCmsIdAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(_agentManager.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);

        _nuageVspManager.deleteNuageVspDevice(cmd);
    }

    @Test
    public void testListNuageVspDevices() {
        final NuageVspDeviceVO nuageVspDevice = mock(NuageVspDeviceVO.class);
        when(nuageVspDevice.getPhysicalNetworkId()).thenReturn(NETWORK_ID);

        final PhysicalNetworkVO phyNtwkVO = mock(PhysicalNetworkVO.class);
        when(_physicalNetworkDao.findById(NETWORK_ID)).thenReturn(phyNtwkVO);
        when(_nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(new ArrayList<NuageVspDeviceVO>());

        final ListNuageVspDevicesCmd cmd = mock(ListNuageVspDevicesCmd.class);
        when(cmd.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(cmd.getNuageVspDeviceId()).thenReturn(null);
        _nuageVspManager.listNuageVspDevices(cmd);
    }
}
