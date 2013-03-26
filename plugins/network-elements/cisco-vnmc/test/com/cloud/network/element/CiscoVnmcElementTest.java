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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.naming.ConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.matchers.Any;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AssociateAsaWithLogicalEdgeFirewallCommand;
import com.cloud.agent.api.ConfigureNexusVsmForAsaCommand;
import com.cloud.agent.api.CreateLogicalEdgeFirewallCommand;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.ClusterVSMMapVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.VlanVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.ClusterVSMMapDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.Domain;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.CiscoNexusVSMDeviceVO;
import com.cloud.network.IpAddress;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.cisco.CiscoAsa1000vDeviceVO;
import com.cloud.network.cisco.CiscoVnmcControllerVO;
import com.cloud.network.cisco.NetworkAsa1000vMapVO;
import com.cloud.network.dao.CiscoAsa1000vDao;
import com.cloud.network.dao.CiscoNexusVSMDeviceDao;
import com.cloud.network.dao.CiscoVnmcDao;
import com.cloud.network.dao.NetworkAsa1000vMapDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.user.Account;
import com.cloud.utils.net.Ip;
import com.cloud.vm.ReservationContext;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CiscoVnmcElementTest {

    CiscoVnmcElement _element = new CiscoVnmcElement();
    AgentManager _agentMgr = mock(AgentManager.class);
    NetworkManager _networkMgr = mock(NetworkManager.class);
    NetworkModel _networkModel = mock(NetworkModel.class);
    HostDao _hostDao = mock(HostDao.class);
    NetworkServiceMapDao _ntwkSrvcDao = mock(NetworkServiceMapDao.class);
    ConfigurationManager _configMgr = mock(ConfigurationManager.class);
    CiscoVnmcDao _ciscoVnmcDao = mock(CiscoVnmcDao.class);
    CiscoAsa1000vDao _ciscoAsa1000vDao = mock(CiscoAsa1000vDao.class);
    NetworkAsa1000vMapDao _networkAsa1000vMapDao = mock(NetworkAsa1000vMapDao.class);
    ClusterVSMMapDao _clusterVsmMapDao = mock(ClusterVSMMapDao.class);
    CiscoNexusVSMDeviceDao _vsmDeviceDao = mock(CiscoNexusVSMDeviceDao.class);
    VlanDao _vlanDao = mock(VlanDao.class);

    @Before
    public void setUp() throws ConfigurationException {
        _element._resourceMgr = mock(ResourceManager.class);
        _element._agentMgr = _agentMgr;
        _element._networkMgr = _networkMgr;
        _element._networkModel = _networkModel;
        _element._hostDao = _hostDao;
        _element._configMgr = _configMgr;
        _element._ciscoVnmcDao = _ciscoVnmcDao;
        _element._ciscoAsa1000vDao = _ciscoAsa1000vDao;
        _element._networkAsa1000vMapDao = _networkAsa1000vMapDao;
        _element._clusterVsmMapDao = _clusterVsmMapDao;
        _element._vsmDeviceDao = _vsmDeviceDao;

        // Standard responses
        when(_networkModel.isProviderForNetwork(Provider.CiscoVnmc, 1L)).thenReturn(true);

        _element.configure("CiscoVnmcTestElement", Collections.<String, Object> emptyMap());
    }

    @Test
    public void canHandleTest() {
        Network network = mock(Network.class);
        when(network.getId()).thenReturn(1L);
        when(network.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Vlan);
        assertTrue(_element.canHandle(network));

        when(network.getBroadcastDomainType()).thenReturn(BroadcastDomainType.UnDecided);
        assertFalse(_element.canHandle(network));
    }

    @Test
    public void implementTest() throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
    	URI uri = URI.create("vlan://123");

        Network network = mock(Network.class);
        when(network.getId()).thenReturn(1L);
        when(network.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Vlan);
        when(network.getDataCenterId()).thenReturn(1L);
        when(network.getGateway()).thenReturn("1.1.1.1");
        when(network.getBroadcastUri()).thenReturn(uri);
        when(network.getCidr()).thenReturn("1.1.1.0/24");

        NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(1L);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        DeployDestination dest = mock(DeployDestination.class);

        Domain dom = mock(Domain.class);
        when(dom.getName()).thenReturn("d1");
        Account acc = mock(Account.class);
        when(acc.getAccountName()).thenReturn("a1");
        ReservationContext context = mock(ReservationContext.class);
        when(context.getDomain()).thenReturn(dom);
        when(context.getAccount()).thenReturn(acc);

        DataCenter dc = mock(DataCenter.class);
        when(_configMgr.getZone(network.getDataCenterId())).thenReturn(dc);
        when(dc.getNetworkType()).thenReturn(NetworkType.Advanced);

        @SuppressWarnings("unchecked")
        List<CiscoVnmcControllerVO> devices = mock(List.class);
        when(devices.isEmpty()).thenReturn(false);
        when(devices.get(0)).thenReturn(mock(CiscoVnmcControllerVO.class));
        when(_ciscoVnmcDao.listByPhysicalNetwork(network.getPhysicalNetworkId())).thenReturn(devices);

        CiscoAsa1000vDeviceVO asaVO = mock(CiscoAsa1000vDeviceVO.class);
        when(asaVO.getInPortProfile()).thenReturn("foo");
        when(asaVO.getManagementIp()).thenReturn("1.2.3.4");

        List<CiscoAsa1000vDeviceVO> asaList = new ArrayList<CiscoAsa1000vDeviceVO>();
        asaList.add(asaVO);
        when(_ciscoAsa1000vDao.listByPhysicalNetwork(network.getPhysicalNetworkId())).thenReturn(asaList);

        when(_networkAsa1000vMapDao.findByNetworkId(network.getId())).thenReturn(mock(NetworkAsa1000vMapVO.class));
        when(_networkAsa1000vMapDao.findByAsa1000vId(anyLong())).thenReturn(null);
        when(_networkAsa1000vMapDao.persist(any(NetworkAsa1000vMapVO.class))).thenReturn(mock(NetworkAsa1000vMapVO.class));

        when(_networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.SourceNat, Provider.CiscoVnmc)).thenReturn(true);

        ClusterVSMMapVO clusterVsmMap = mock(ClusterVSMMapVO.class);
        when(_clusterVsmMapDao.findByClusterId(anyLong())).thenReturn(clusterVsmMap);

        CiscoNexusVSMDeviceVO vsmDevice = mock(CiscoNexusVSMDeviceVO.class);
        when(vsmDevice.getUserName()).thenReturn("foo");
        when(vsmDevice.getPassword()).thenReturn("bar");
        when(vsmDevice.getipaddr()).thenReturn("1.2.3.4");
        when(_vsmDeviceDao.findById(anyLong())).thenReturn(vsmDevice);

        HostVO hostVO = mock(HostVO.class);
        when(hostVO.getId()).thenReturn(1L);
        when(_hostDao.findById(anyLong())).thenReturn(hostVO);

        Ip ip = mock(Ip.class);
        when(ip.addr()).thenReturn("1.2.3.4");

        PublicIp publicIp = mock(PublicIp.class);
        when(publicIp.getAddress()).thenReturn(ip);
        when(publicIp.getState()).thenReturn(IpAddress.State.Releasing);
        when(publicIp.getAccountId()).thenReturn(1L);
        when(publicIp.isSourceNat()).thenReturn(true);
        when(publicIp.getVlanTag()).thenReturn("123");
        when(publicIp.getGateway()).thenReturn("1.1.1.1");
        when(publicIp.getNetmask()).thenReturn("1.1.1.1");
        when(publicIp.getMacAddress()).thenReturn(null);
        when(publicIp.isOneToOneNat()).thenReturn(true);
        when(_networkMgr.assignSourceNatIpAddressToGuestNetwork(acc, network)).thenReturn(publicIp);

        VlanVO vlanVO = mock(VlanVO.class);
        when(vlanVO.getVlanGateway()).thenReturn("1.1.1.1");
        List<VlanVO> vlanVOList = new ArrayList<VlanVO>();
        when(_vlanDao.listVlansByPhysicalNetworkId(network.getPhysicalNetworkId())).thenReturn(vlanVOList);

        Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(true);

        when(_agentMgr.easySend(anyLong(), any(CreateLogicalEdgeFirewallCommand.class))).thenReturn(answer);
        when(_agentMgr.easySend(anyLong(), any(ConfigureNexusVsmForAsaCommand.class))).thenReturn(answer);
        when(_agentMgr.easySend(anyLong(), any(SetSourceNatCommand.class))).thenReturn(answer);
        when(_agentMgr.easySend(anyLong(), any(AssociateAsaWithLogicalEdgeFirewallCommand.class))).thenReturn(answer);
        
        assertTrue(_element.implement(network, offering, dest, context));
    }
}
