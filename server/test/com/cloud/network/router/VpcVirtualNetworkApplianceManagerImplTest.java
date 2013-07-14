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

package com.cloud.network.router;


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.cloud.configuration.ZoneConfig;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.vm.VirtualMachine;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cloud.agent.AgentManager.OnError;
import com.cloud.agent.api.routing.DnsMasqConfigCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.NetworkManager;
import com.cloud.network.router.VirtualRouter.RedundantState;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:/VpcVirtNetAppContext.xml")
public class VpcVirtualNetworkApplianceManagerImplTest {
    private static final Logger s_logger = Logger.getLogger(VpcVirtualNetworkApplianceManagerImplTest.class);

    @Mock DataCenterDao _dcDao;
    @Mock VpcDao _vpcDao;
    @Mock VirtualRouter router;
    @Mock NicDao _nicDao;
    @Mock DomainRouterDao _routerDao;
    @Mock NetworkModel _networkmodel;
    @Mock NicVO nicVO;
    @Mock DataCenterVO dcVO;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

    }

    @Test
    public void testConfigDnsMasq() {
        VpcVirtualNetworkApplianceManagerImpl vpcVirtNetAppMgr = new VpcVirtualNetworkApplianceManagerImpl();
        vpcVirtNetAppMgr._vpcDao = _vpcDao;
        vpcVirtNetAppMgr._dcDao = _dcDao;
        vpcVirtNetAppMgr._nicDao = _nicDao;
        vpcVirtNetAppMgr._routerDao = _routerDao;
        when(router.getId()).thenReturn(1L);
        when(router.getVpcId()).thenReturn(1L);
        when(router.getDataCenterId()).thenReturn(1L);
        VpcVO vpc = new VpcVO(1L,"bla","bla",1L,1L,1L,"10.0.0.0/8","blieb.net");
        when( _vpcDao.findById(1L)).thenReturn(vpc);
        DataCenterVO dcVo = new DataCenterVO(1L,"dc","dc","8.8.8.8",null,null,null,"10.0.0.0/8","bla.net",new Long(1L),NetworkType.Advanced,null,".net");
        Map<String, String> map = new HashMap<String, String>();
        dcVo.setDetails(map);
        dcVo.setDetail(ZoneConfig.DnsSearchOrder.getName(), "dummy");
        when(_dcDao.findById(1L)).thenReturn(dcVo);
        DomainRouterVO routerVo = new DomainRouterVO(1L,1L,1L,"brr",1L,HypervisorType.Any,1L,1L,1L,false,0,false,RedundantState.MASTER,false,false,1L);
        when( _routerDao.findById(1L)).thenReturn(routerVo);
//        when( vpcVirtNetAppMgr.getRouterControlIp(1L)).thenReturn("10.0.0.1");
        when( router.getInstanceName()).thenReturn("r-vm-1");
        when( router.getPublicIpAddress()).thenReturn("11.11.11.11");
        NicVO nicvo = new NicVO("server", 1l, 1l, VirtualMachine.Type.DomainRouter);
        nicvo.setNetworkId(1l);
        when(_nicDao.findByIp4AddressAndVmId(anyString(), anyLong())).thenReturn(nicvo);
        NetworkManager netMgr = mock(NetworkManager.class);
        vpcVirtNetAppMgr._networkMgr = netMgr;
        vpcVirtNetAppMgr._networkModel = _networkmodel;
        when(nicVO.getNetworkId()).thenReturn(1l);
        when(_networkmodel.isProviderSupportServiceInNetwork(1l, Network.Service.Dhcp, Network.Provider.VirtualRouter)).thenReturn(true);
        when(dcVO.getDetail(anyString())).thenReturn(null);
        Commands cmds = new Commands(OnError.Stop);

        vpcVirtNetAppMgr.configDnsMasq(router, cmds);
        Assert.assertEquals("expected one command",1, cmds.size());
        
        DnsMasqConfigCommand cmd = cmds.getCommand(DnsMasqConfigCommand.class);
    }

}
