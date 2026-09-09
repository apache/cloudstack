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

package com.cloud.network.lb;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LoadBalancingRulesManagerImplTest{

    @Mock
    NetworkDao _networkDao;

    @Mock
    NetworkOrchestrationService _networkMgr;

    @Mock
    EntityManager _entityMgr;

    @Mock
    AccountManager _accountMgr;

    @Mock
    NetworkModel _networkModel;

    @Mock
    NetworkVO networkMock;

    private long accountId = 10L;
    private long networkId = 4L;

    @Spy
    @InjectMocks
    LoadBalancingRulesManagerImpl lbr = new LoadBalancingRulesManagerImpl();

    @Test
    public void generateCidrStringTestNullCidrList() {
        String result = lbr.generateCidrString(null);
        Assert.assertNull(result);
    }

    @Test
    public void generateCidrStringTestWithCidrList() {
        List<String> cidrList = new ArrayList<>();
        cidrList.add("1.1.1.1");
        cidrList.add("2.2.2.2/24");
        String result = lbr.generateCidrString(cidrList);
        Assert.assertEquals("1.1.1.1 2.2.2.2/24", result);
    }

    @Test (expected = ServerApiException.class)
    public void generateCidrStringTestWithInvalidCidrList() {
        List<String> cidrList = new ArrayList<>();
        cidrList.add("1.1");
        cidrList.add("2.2.2.2/24");
        String result = lbr.generateCidrString(cidrList);
        Assert.assertEquals("1.1.1.1 2.2.2.2/24", result);
    }

    @Test
    public void testGetLoadBalancerServiceProvider() {
        LoadBalancerVO loadBalancerMock = Mockito.mock(LoadBalancerVO.class);
        NetworkVO networkMock = Mockito.mock(NetworkVO.class);
        List<Network.Provider> providers = Arrays.asList(Network.Provider.VirtualRouter);

        when(loadBalancerMock.getNetworkId()).thenReturn(10L);
        when(_networkDao.findById(Mockito.anyLong())).thenReturn(networkMock);
        when(_networkMgr.getProvidersForServiceInNetwork(networkMock, Network.Service.Lb)).thenReturn(providers);

        Network.Provider provider = lbr.getLoadBalancerServiceProvider(loadBalancerMock);

        Assert.assertEquals(Network.Provider.VirtualRouter, provider);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetLoadBalancerServiceProviderFail() {
        LoadBalancerVO loadBalancerMock = Mockito.mock(LoadBalancerVO.class);
        NetworkVO networkMock = Mockito.mock(NetworkVO.class);

        when(_networkDao.findById(Mockito.any())).thenReturn(networkMock);
        when(_networkMgr.getProvidersForServiceInNetwork(networkMock, Network.Service.Lb)).thenReturn(new ArrayList<>());

        Network.Provider provider = lbr.getLoadBalancerServiceProvider(loadBalancerMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createPublicLoadBalancerRuleWithDnsPortAndNoIpDoesNotNpe() throws Exception {
        long lbOwnerId = accountId;
        long networkOfferingId = 7L;
        when(_accountMgr.getAccount(lbOwnerId)).thenReturn(Mockito.mock(Account.class));
        when(_networkModel.getNetwork(networkId)).thenReturn(networkMock);
        when(networkMock.getNetworkOfferingId()).thenReturn(networkOfferingId);
        NetworkOffering off = Mockito.mock(NetworkOffering.class);
        when(_entityMgr.findById(NetworkOffering.class, networkOfferingId)).thenReturn(off);
        when(off.isElasticLb()).thenReturn(false);

        lbr.createPublicLoadBalancerRule("xid", "name", "desc", 53, 53, 53, 53,
                null, "tcp", "roundrobin", networkId, lbOwnerId, false, "tcp", null, null);
    }
}
