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
package org.apache.cloudstack.service;

import com.cloud.network.IpAddress;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.utils.net.Ip;
import org.apache.cloudstack.NsxAnswer;
import org.apache.cloudstack.agent.api.CreateNsxStaticNatCommand;
import org.apache.cloudstack.agent.api.CreateNsxTier1GatewayCommand;
import org.apache.cloudstack.agent.api.CreateOrUpdateNsxTier1NatRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNsxNatRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNsxSegmentCommand;
import org.apache.cloudstack.agent.api.DeleteNsxTier1GatewayCommand;
import org.apache.cloudstack.utils.NsxControllerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NsxServiceImplTest {
    @Mock
    private NsxControllerUtils nsxControllerUtils;
    @Mock
    private VpcDao vpcDao;
    NsxServiceImpl nsxService;

    AutoCloseable closeable;

    private static final long domainId = 1L;
    private static final long accountId = 2L;
    private static final long zoneId = 1L;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        nsxService = new NsxServiceImpl();
        nsxService.nsxControllerUtils = nsxControllerUtils;
        nsxService.vpcDao = vpcDao;
    }

    @After
    public void teardown() throws Exception {
        closeable.close();
    }

    @Test
    public void testCreateVpcNetwork() {
        NsxAnswer createNsxTier1GatewayAnswer = mock(NsxAnswer.class);
        when(nsxControllerUtils.sendNsxCommand(any(CreateNsxTier1GatewayCommand.class), anyLong())).thenReturn(createNsxTier1GatewayAnswer);
        when(createNsxTier1GatewayAnswer.getResult()).thenReturn(true);

        assertTrue(nsxService.createVpcNetwork(1L, 3L, 2L, 5L, "VPC01", false));
    }

    @Test
    public void testDeleteVpcNetwork() {
        NsxAnswer deleteNsxTier1GatewayAnswer = mock(NsxAnswer.class);
        when(nsxControllerUtils.sendNsxCommand(any(DeleteNsxTier1GatewayCommand.class), anyLong())).thenReturn(deleteNsxTier1GatewayAnswer);
        when(deleteNsxTier1GatewayAnswer.getResult()).thenReturn(true);

        assertTrue(nsxService.deleteVpcNetwork(1L, 2L, 3L, 10L, "VPC01"));
    }

    @Test
    public void testDeleteNetworkOnVpc() {
        NetworkVO network = new NetworkVO();
        network.setVpcId(1L);
        when(vpcDao.findById(1L)).thenReturn(mock(VpcVO.class));
        NsxAnswer deleteNsxSegmentAnswer = mock(NsxAnswer.class);
        when(nsxControllerUtils.sendNsxCommand(any(DeleteNsxSegmentCommand.class), anyLong())).thenReturn(deleteNsxSegmentAnswer);
        when(deleteNsxSegmentAnswer.getResult()).thenReturn(true);

        assertTrue(nsxService.deleteNetwork(zoneId, accountId, domainId, network));
    }

    @Test
    public void testDeleteNetwork() {
        NetworkVO network = new NetworkVO();
        network.setVpcId(null);
        NsxAnswer deleteNsxSegmentAnswer = mock(NsxAnswer.class);
        when(deleteNsxSegmentAnswer.getResult()).thenReturn(true);
        when(nsxControllerUtils.sendNsxCommand(any(DeleteNsxSegmentCommand.class), anyLong())).thenReturn(deleteNsxSegmentAnswer);
        NsxAnswer deleteNsxTier1GatewayAnswer = mock(NsxAnswer.class);
        when(deleteNsxTier1GatewayAnswer.getResult()).thenReturn(true);
        when(nsxControllerUtils.sendNsxCommand(any(DeleteNsxTier1GatewayCommand.class), anyLong())).thenReturn(deleteNsxTier1GatewayAnswer);
        assertTrue(nsxService.deleteNetwork(zoneId, accountId, domainId, network));
    }

    @Test
    public void testUpdateVpcSourceNatIp() {
        VpcVO vpc = mock(VpcVO.class);
        IpAddress ipAddress = mock(IpAddress.class);
        Ip ip = Mockito.mock(Ip.class);
        when(ip.addr()).thenReturn("10.1.10.10");
        when(ipAddress.getAddress()).thenReturn(ip);
        long vpcId = 1L;
        when(vpc.getAccountId()).thenReturn(accountId);
        when(vpc.getDomainId()).thenReturn(domainId);
        when(vpc.getZoneId()).thenReturn(zoneId);
        when(vpc.getId()).thenReturn(vpcId);
        NsxAnswer answer = mock(NsxAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(nsxControllerUtils.sendNsxCommand(any(CreateOrUpdateNsxTier1NatRuleCommand.class), eq(zoneId))).thenReturn(answer);
        nsxService.updateVpcSourceNatIp(vpc, ipAddress);
        Mockito.verify(nsxControllerUtils).sendNsxCommand(any(CreateOrUpdateNsxTier1NatRuleCommand.class), eq(zoneId));
    }

    @Test
    public void testCreateStaticNatRule() {
        long networkId = 1L;
        String networkName = "Network-Test";
        long vmId = 1L;
        String publicIp = "10.10.1.10";
        String vmIp = "192.168.1.20";
        NsxAnswer answer = Mockito.mock(NsxAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(nsxControllerUtils.sendNsxCommand(any(CreateNsxStaticNatCommand.class), eq(zoneId))).thenReturn(answer);
        nsxService.createStaticNatRule(zoneId, domainId, accountId,
                networkId, networkName, true, vmId, publicIp, vmIp);
        Mockito.verify(nsxControllerUtils).sendNsxCommand(any(CreateNsxStaticNatCommand.class), eq(zoneId));
    }

    @Test
    public void testDeleteStaticNatRule() {
        long networkId = 1L;
        String networkName = "Network-Test";
        NsxAnswer answer = Mockito.mock(NsxAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(nsxControllerUtils.sendNsxCommand(any(DeleteNsxNatRuleCommand.class), eq(zoneId))).thenReturn(answer);
        nsxService.deleteStaticNatRule(zoneId, domainId, accountId, networkId, networkName, true);
        Mockito.verify(nsxControllerUtils).sendNsxCommand(any(DeleteNsxNatRuleCommand.class), eq(zoneId));
    }
}
