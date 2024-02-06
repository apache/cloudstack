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
package org.apache.cloudstack.internallbelement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.apache.cloudstack.lb.ApplicationLoadBalancerRuleVO;
import org.apache.cloudstack.network.element.InternalLoadBalancerElement;
import org.apache.cloudstack.network.lb.InternalLoadBalancerVMManager;

import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.VirtualRouterProvider.Type;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.element.VirtualRouterProviderVO;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.user.AccountManager;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.net.Ip;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/lb_element.xml")
public class InternalLbElementTest {
    //The class to test
    @Inject
    InternalLoadBalancerElement _lbEl;

    //Mocked interfaces
    @Inject
    AccountManager _accountMgr;
    @Inject
    VirtualRouterProviderDao _vrProviderDao;
    @Inject
    PhysicalNetworkServiceProviderDao _pNtwkProviderDao;
    @Inject
    InternalLoadBalancerVMManager _internalLbMgr;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    EntityManager _entityMgr;

    long validElId = 1L;
    long nonExistingElId = 2L;
    long invalidElId = 3L; //not of VirtualRouterProviderType
    long notEnabledElId = 4L;

    long validProviderId = 1L;
    long nonExistingProviderId = 2L;
    long invalidProviderId = 3L;

    @Before
    public void setUp() {

        ComponentContext.initComponentsLifeCycle();
        VirtualRouterProviderVO validElement = new VirtualRouterProviderVO(1, Type.InternalLbVm);
        validElement.setEnabled(true);
        VirtualRouterProviderVO invalidElement = new VirtualRouterProviderVO(1, Type.VirtualRouter);
        VirtualRouterProviderVO notEnabledElement = new VirtualRouterProviderVO(1, Type.InternalLbVm);

        Mockito.when(_vrProviderDao.findByNspIdAndType(validElId, Type.InternalLbVm)).thenReturn(validElement);
        Mockito.when(_vrProviderDao.findByNspIdAndType(invalidElId, Type.InternalLbVm)).thenReturn(invalidElement);
        Mockito.when(_vrProviderDao.findByNspIdAndType(notEnabledElId, Type.InternalLbVm)).thenReturn(notEnabledElement);

        Mockito.when(_vrProviderDao.persist(validElement)).thenReturn(validElement);

        Mockito.when(_vrProviderDao.findByNspIdAndType(validProviderId, Type.InternalLbVm)).thenReturn(validElement);

        PhysicalNetworkServiceProviderVO validProvider = new PhysicalNetworkServiceProviderVO(1, "InternalLoadBalancerElement");
        PhysicalNetworkServiceProviderVO invalidProvider = new PhysicalNetworkServiceProviderVO(1, "Invalid name!");

        Mockito.when(_pNtwkProviderDao.findById(validProviderId)).thenReturn(validProvider);
        Mockito.when(_pNtwkProviderDao.findById(invalidProviderId)).thenReturn(invalidProvider);

        Mockito.when(_vrProviderDao.persist(ArgumentMatchers.any(VirtualRouterProviderVO.class))).thenReturn(validElement);

        DataCenterVO dc = new DataCenterVO(1L, null, null, null, null, null, null, null, null, null, NetworkType.Advanced, null, null);
        Mockito.when(_entityMgr.findById(ArgumentMatchers.eq(DataCenter.class), ArgumentMatchers.anyLong())).thenReturn(dc);
    }

    //TEST FOR getProvider() method

    @Test
    public void verifyProviderName() {
        Provider pr = _lbEl.getProvider();
        assertEquals("Wrong provider is returned", pr.getName(), Provider.InternalLbVm.getName());
    }

    //TEST FOR isReady() METHOD

    @Test
    public void verifyValidProviderState() {
        PhysicalNetworkServiceProviderVO provider = new PhysicalNetworkServiceProviderVO();
        provider = setId(provider, validElId);
        boolean isReady = _lbEl.isReady(provider);
        assertTrue("Valid provider is returned as not ready", isReady);
    }

    @Test
    public void verifyNonExistingProviderState() {
        PhysicalNetworkServiceProviderVO provider = new PhysicalNetworkServiceProviderVO();
        provider = setId(provider, nonExistingElId);
        boolean isReady = _lbEl.isReady(provider);
        assertFalse("Non existing provider is returned as ready", isReady);
    }

    @Test
    public void verifyInvalidProviderState() {
        PhysicalNetworkServiceProviderVO provider = new PhysicalNetworkServiceProviderVO();
        provider = setId(provider, invalidElId);
        boolean isReady = _lbEl.isReady(provider);
        assertFalse("Not valid provider is returned as ready", isReady);
    }

    @Test
    public void verifyNotEnabledProviderState() {
        PhysicalNetworkServiceProviderVO provider = new PhysicalNetworkServiceProviderVO();
        provider = setId(provider, notEnabledElId);
        boolean isReady = _lbEl.isReady(provider);
        assertFalse("Not enabled provider is returned as ready", isReady);
    }

    //TEST FOR canEnableIndividualServices METHOD
    @Test
    public void verifyCanEnableIndividualSvc() {
        boolean result = _lbEl.canEnableIndividualServices();
        assertTrue("Wrong value is returned by canEnableIndividualSvc", result);
    }

    //TEST FOR verifyServicesCombination METHOD
    @Test
    public void verifyServicesCombination() {
        boolean result = _lbEl.verifyServicesCombination(new HashSet<Service>());
        assertTrue("Wrong value is returned by verifyServicesCombination", result);
    }

    //TEST FOR applyIps METHOD
    @Test
    public void verifyApplyIps() throws ResourceUnavailableException {
        List<PublicIp> ips = new ArrayList<PublicIp>();
        boolean result = _lbEl.applyIps(new NetworkVO(), ips, new HashSet<Service>());
        assertTrue("Wrong value is returned by applyIps method", result);
    }

    //TEST FOR updateHealthChecks METHOD
    @Test
    public void verifyUpdateHealthChecks() throws ResourceUnavailableException {
        List<LoadBalancerTO> check = _lbEl.updateHealthChecks(new NetworkVO(), new ArrayList<LoadBalancingRule>());
        assertNull("Wrong value is returned by updateHealthChecks method", check);
    }

    //TEST FOR validateLBRule METHOD
    @Test
    public void verifyValidateLBRule() throws ResourceUnavailableException {
        ApplicationLoadBalancerRuleVO lb = new ApplicationLoadBalancerRuleVO(null, null, 22, 22, "roundrobin", 1L, 1L, 1L, new Ip("10.10.10.1"), 1L, Scheme.Internal);
        lb.setState(FirewallRule.State.Add);

        LoadBalancingRule rule = new LoadBalancingRule(lb, null, null, null, new Ip("10.10.10.1"));

        boolean result = _lbEl.validateLBRule(new NetworkVO(), rule);
        assertTrue("Wrong value is returned by validateLBRule method", result);
    }

    private static PhysicalNetworkServiceProviderVO setId(PhysicalNetworkServiceProviderVO vo, long id) {
        PhysicalNetworkServiceProviderVO voToReturn = vo;
        Class<?> c = voToReturn.getClass();
        try {
            Field f = c.getDeclaredField("id");
            f.setAccessible(true);
            f.setLong(voToReturn, id);
        } catch (NoSuchFieldException ex) {
            return null;
        } catch (IllegalAccessException ex) {
            return null;
        }

        return voToReturn;
    }

}
