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

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.as.AutoScaleCounter;
import com.cloud.network.as.Counter;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.netris.NetrisService;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.vm.ReservationContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class NetrisElementTest {

    @Spy
    @InjectMocks
    private NetrisElement netrisElement = new NetrisElement();;

    @Mock
    private NetrisService netrisService;
    @Mock
    private AccountManager accountManager;
    @Mock
    private NetworkDao networkDao;
    @Mock
    private DataCenterDao dataCenterDao;
    @Mock
    private DomainDao domainDao;
    @Mock
    private VpcDao vpcDao;

    private AutoCloseable closeable;

    private static long accountId = 2L;
    private static long zoneId = 1L;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testAutoscalingCounterList() {
        List<AutoScaleCounter> counters = NetrisElement.getNetrisAutoScaleCounters();
        Assert.assertEquals(2, counters.size());
        Set<String> counterNamesSet = counters.stream().map(AutoScaleCounter::getName).collect(Collectors.toSet());
        Set<String> expected = Set.of(Counter.Source.CPU.name(), Counter.Source.MEMORY.name());
        Assert.assertEquals(expected, counterNamesSet);
    }

    @Test
    public void testInitCapabilities() {
        Map<Network.Service, Map<Network.Capability, String>> capabilities = NetrisElement.initCapabilities();
        Assert.assertTrue(capabilities.containsKey(Network.Service.Dns));
        Assert.assertTrue(capabilities.containsKey(Network.Service.Dhcp));
        Assert.assertTrue(capabilities.containsKey(Network.Service.SourceNat));
        Assert.assertTrue(capabilities.containsKey(Network.Service.StaticNat));
        Assert.assertTrue(capabilities.containsKey(Network.Service.Lb));
        Assert.assertTrue(capabilities.containsKey(Network.Service.PortForwarding));
        Assert.assertTrue(capabilities.containsKey(Network.Service.NetworkACL));
    }

    @Test
    public void testDeleteNetwork() throws ResourceUnavailableException {
        long networkId = 210L;
        long domainId = 2L;
        long vpcId = 8L;
        String vpcName = "testVpc";
        String networkName = "testVpcTier";
        String networkCidr = "10.10.30.0/24";
        VpcVO vpc = Mockito.mock(VpcVO.class);
        Mockito.when(vpc.getName()).thenReturn(vpcName);
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getAccountId()).thenReturn(accountId);
        Mockito.when(network.getId()).thenReturn(networkId);
        Mockito.when(network.getDataCenterId()).thenReturn(zoneId);
        Mockito.when(network.getName()).thenReturn(networkName);
        Mockito.when(network.getCidr()).thenReturn(networkCidr);
        Mockito.when(network.getVpcId()).thenReturn(vpcId);
        Account account = Mockito.mock(Account.class);
        Mockito.when(account.getId()).thenReturn(accountId);
        Mockito.when(account.getDomainId()).thenReturn(domainId);
        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        Mockito.when(networkVO.getName()).thenReturn(networkName);
        DataCenterVO dataCenterVO = Mockito.mock(DataCenterVO.class);
        Mockito.when(dataCenterVO.getId()).thenReturn(zoneId);
        DomainVO domain = Mockito.mock(DomainVO.class);
        Mockito.when(domain.getId()).thenReturn(domainId);
        Mockito.when(accountManager.getAccount(accountId)).thenReturn(account);
        Mockito.when(networkDao.findById(networkId)).thenReturn(networkVO);
        Mockito.when(dataCenterDao.findById(zoneId)).thenReturn(dataCenterVO);
        Mockito.when(domainDao.findById(domainId)).thenReturn(domain);
        Mockito.when(vpcDao.findById(vpcId)).thenReturn(vpc);
        ReservationContext context = Mockito.mock(ReservationContext.class);
        netrisElement.destroy(network, context);
        Mockito.verify(netrisService).deleteVnetResource(zoneId, accountId, domainId, vpcName, vpcId,
                networkName, networkId, networkCidr);
    }
}
