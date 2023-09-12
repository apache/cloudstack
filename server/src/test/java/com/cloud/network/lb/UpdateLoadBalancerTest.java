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

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.UUID;

import org.apache.cloudstack.api.command.user.loadbalancer.UpdateLoadBalancerRuleCmd;
import org.apache.cloudstack.context.CallContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.as.dao.AutoScaleVmGroupDao;
import com.cloud.network.dao.LBHealthCheckPolicyDao;
import com.cloud.network.dao.LBStickinessPolicyDao;
import com.cloud.network.dao.LoadBalancerCertMapDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.element.LoadBalancingServiceProvider;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.MockAccountManagerImpl;
import com.cloud.user.User;
import com.cloud.user.UserVO;

public class UpdateLoadBalancerTest {

    LoadBalancingRulesManagerImpl _lbMgr = new LoadBalancingRulesManagerImpl();

    private UpdateLoadBalancerRuleCmd updateLbRuleCmd;
    private LoadBalancerDao lbDao = Mockito.mock(LoadBalancerDao.class);
    private NetworkDao netDao = Mockito.mock(NetworkDao.class);
    private NetworkModel netModel = Mockito.mock(NetworkModel.class);
    private LoadBalancingServiceProvider lbServiceProvider= Mockito.mock(LoadBalancingServiceProvider.class);

    private static long domainId = 5L;
    private static String accountName = "admin";

    @Before
    public void setUp() {
        _lbMgr._accountMgr = new MockAccountManagerImpl();
        _lbMgr._autoScaleVmGroupDao = Mockito.mock(AutoScaleVmGroupDao.class);
        _lbMgr._networkDao = netDao;
        _lbMgr._networkModel = netModel;
        _lbMgr._lb2healthcheckDao = Mockito.mock(LBHealthCheckPolicyDao.class);
        _lbMgr._lb2stickinesspoliciesDao = Mockito.mock(LBStickinessPolicyDao.class);
        _lbMgr._lb2VmMapDao = Mockito.mock(LoadBalancerVMMapDao.class);
        _lbMgr._lbCertMapDao = Mockito.mock(LoadBalancerCertMapDao.class);
        _lbMgr._lbDao = lbDao;
        _lbMgr._lbProviders = new ArrayList<LoadBalancingServiceProvider>();
        _lbMgr._lbProviders.add(lbServiceProvider);

        updateLbRuleCmd = new UpdateLoadBalancerRuleCmd();

        AccountVO account = new AccountVO(accountName, domainId, "networkDomain", Account.Type.NORMAL, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
    }

    @Test
    public void testValidateRuleBeforeUpdateLB() throws ResourceAllocationException, ResourceUnavailableException, InsufficientCapacityException {

        LoadBalancerVO lb = new LoadBalancerVO(null, null, null, 0L, 0, 0, null, 0L, 0L, domainId, null, null);

        when(lbDao.findById(isNull())).thenReturn(lb);
        when(netModel.getPublicIpAddress(anyLong())).thenReturn(Mockito.mock(PublicIpAddress.class));
        when(netDao.findById(anyLong())).thenReturn(Mockito.mock(NetworkVO.class));
        when(lbServiceProvider.validateLBRule(any(Network.class), any(LoadBalancingRule.class))).thenReturn(true);
        when(lbDao.update(isNull(), eq(lb))).thenReturn(true);

        _lbMgr.updateLoadBalancerRule(updateLbRuleCmd);

        InOrder inOrder = Mockito.inOrder(lbServiceProvider, lbDao);
        inOrder.verify(lbServiceProvider).validateLBRule(any(Network.class), any(LoadBalancingRule.class));
        inOrder.verify(lbDao).update(isNull(),eq(lb));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRuleNotValidated() throws ResourceAllocationException, ResourceUnavailableException, InsufficientCapacityException {

        LoadBalancerVO lb = new LoadBalancerVO(null, null, null, 0L, 0, 0, null, 0L, 0L, domainId, null, null);

        when(lbDao.findById(anyLong())).thenReturn(lb);
        when(netModel.getPublicIpAddress(anyLong())).thenReturn(Mockito.mock(PublicIpAddress.class));
        when(netDao.findById(anyLong())).thenReturn(Mockito.mock(NetworkVO.class));
        when(lbServiceProvider.validateLBRule(any(Network.class), any(LoadBalancingRule.class))).thenReturn(false);

        _lbMgr.updateLoadBalancerRule(updateLbRuleCmd);
    }

    @After
    public void tearDown() {
        CallContext.unregister();
    }

}
