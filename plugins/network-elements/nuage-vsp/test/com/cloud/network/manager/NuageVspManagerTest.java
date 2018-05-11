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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import javax.naming.ConfigurationException;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.cloudstack.resourcedetail.VpcDetailVO;
import org.apache.cloudstack.resourcedetail.dao.VpcDetailsDao;

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
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.NuageVspDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.resource.ResourceManager;
import com.cloud.util.NuageVspEntityBuilder;

public class NuageVspManagerTest extends NuageTest {
    private static final long NETWORK_ID = 42L;
    private static final long VPC_ID = 1L;
    private static final long VPC_ID2 = 2L;

    @Mock private PhysicalNetworkDao _physicalNetworkDao ;
    @Mock private PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;
    @Mock private ResourceManager _resourceManager;
    @Mock private HostDetailsDao _hostDetailsDao;
    @Mock private NuageVspDao _nuageVspDao;
    @Mock private NetworkDao _networkDao;
    @Mock private HostDao _hostDao;
    @Mock private AgentManager _agentManager;
    @Mock private NuageVspEntityBuilder _nuageVspEntityBuilder;
    @Mock private NetworkDetailsDao _networkDetailsDao;
    @Mock private VpcDetailsDao _vpcDetailsDao;

    @InjectMocks
    private NuageVspManagerImpl _nuageVspManager = new NuageVspManagerImpl();

    private NetworkVO setUpMockedNetwork(Long vpcId, String domainTemplateName) {
        NetworkVO networkToMock = mock(NetworkVO.class);
        when(networkToMock.getId()).thenReturn(NETWORK_ID);

        reset(_vpcDetailsDao, _networkDetailsDao);

        when(networkToMock.getVpcId()).thenReturn(vpcId);

        if (domainTemplateName != null) {
            if (vpcId != null) {
                VpcDetailVO detail = new VpcDetailVO(vpcId, NuageVspManager.nuageDomainTemplateDetailName, domainTemplateName, false);
                when(_vpcDetailsDao.findDetail(vpcId, NuageVspManager.nuageDomainTemplateDetailName)).thenReturn(detail);
            } else {
                NetworkDetailVO detail = new NetworkDetailVO(NETWORK_ID, NuageVspManager.nuageDomainTemplateDetailName, domainTemplateName, false);
                when(_networkDetailsDao.findDetail(NETWORK_ID, NuageVspManager.nuageDomainTemplateDetailName)).thenReturn(detail);
            }
        }

        return networkToMock;
    }

    @Test
    public void testNuagePreConfiguredDomainTemplates() {
        NetworkVO _mockedL2Network = setUpMockedNetwork(VPC_ID, "VpcDomainTemplate2");
        String checkDomainTemplate =_nuageVspManager.getPreConfiguredDomainTemplateName(_mockedL2Network);
        assertEquals("VpcDomainTemplate2", checkDomainTemplate);

        _mockedL2Network = setUpMockedNetwork(VPC_ID2, null);
        checkDomainTemplate =_nuageVspManager.getPreConfiguredDomainTemplateName(_mockedL2Network);
        assertEquals("VpcDomainTemplate", checkDomainTemplate);

        _mockedL2Network = setUpMockedNetwork(null, "IsolatedDomainTemplate2");
        checkDomainTemplate =_nuageVspManager.getPreConfiguredDomainTemplateName(_mockedL2Network);
        assertEquals("IsolatedDomainTemplate2", checkDomainTemplate);

        _mockedL2Network = setUpMockedNetwork(null, null);
        checkDomainTemplate =_nuageVspManager.getPreConfiguredDomainTemplateName(_mockedL2Network);
        assertEquals("IsolatedDomainTemplate", checkDomainTemplate);

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
        when(_configDao.findByName("nuagevsp.cms.id")).thenReturn(cmsIdConfig);

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
