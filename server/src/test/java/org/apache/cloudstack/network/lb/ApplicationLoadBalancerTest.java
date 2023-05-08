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
package org.apache.cloudstack.network.lb;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.lb.ApplicationLoadBalancerRuleVO;
import org.apache.cloudstack.lb.dao.ApplicationLoadBalancerRuleDao;
import org.apache.cloudstack.test.utils.SpringUtils;

import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.UnsupportedServiceException;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.lb.LoadBalancingRulesService;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;

/**
 * This class is responsible for unittesting the methods defined in ApplicationLoadBalancerService
 *
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class ApplicationLoadBalancerTest extends TestCase {
    //The interface to test
    @Inject
    ApplicationLoadBalancerManagerImpl _appLbSvc;

    //The interfaces below are mocked
    @Inject
    ApplicationLoadBalancerRuleDao _lbDao;
    @Inject
    LoadBalancingRulesManager _lbMgr;
    @Inject
    NetworkModel _ntwkModel;
    @Inject
    AccountManager _accountMgr;
    @Inject
    FirewallRulesDao _firewallDao;
    @Inject
    UsageEventDao _usageEventDao;
    @Inject
    LoadBalancingRulesService _lbService;

    public static long existingLbId = 1L;
    public static long nonExistingLbId = 2L;

    public static long validGuestNetworkId = 1L;
    public static long invalidGuestNetworkId = 2L;
    public static long validPublicNetworkId = 3L;

    public static long validAccountId = 1L;
    public static long invalidAccountId = 2L;

    public String validRequestedIp = "10.1.1.1";

    @Override
    @Before
    public void setUp() {
        ComponentContext.initComponentsLifeCycle();
        //mockito for .getApplicationLoadBalancer tests
        Mockito.when(_lbDao.findById(1L)).thenReturn(new ApplicationLoadBalancerRuleVO());
        Mockito.when(_lbDao.findById(2L)).thenReturn(null);

        //mockito for .deleteApplicationLoadBalancer tests
        Mockito.when(_lbService.deleteLoadBalancerRule(existingLbId, true)).thenReturn(true);
        Mockito.when(_lbService.deleteLoadBalancerRule(nonExistingLbId, true)).thenReturn(false);

        //mockito for .createApplicationLoadBalancer tests
        NetworkVO guestNetwork = new NetworkVO(TrafficType.Guest, null, null, 1, null, 1, 1L, false);
        setId(guestNetwork, validGuestNetworkId);
        guestNetwork.setCidr("10.1.1.1/24");

        NetworkVO publicNetwork = new NetworkVO(TrafficType.Public, null, null, 1, null, 1, 1L, false);

        Mockito.when(_ntwkModel.getNetwork(validGuestNetworkId)).thenReturn(guestNetwork);
        Mockito.when(_ntwkModel.getNetwork(invalidGuestNetworkId)).thenReturn(null);
        Mockito.when(_ntwkModel.getNetwork(validPublicNetworkId)).thenReturn(publicNetwork);

        Mockito.when(_accountMgr.getAccount(validAccountId)).thenReturn(new AccountVO());
        Mockito.when(_accountMgr.getAccount(invalidAccountId)).thenReturn(null);
        Mockito.when(_ntwkModel.areServicesSupportedInNetwork(validGuestNetworkId, Service.Lb)).thenReturn(true);
        Mockito.when(_ntwkModel.areServicesSupportedInNetwork(invalidGuestNetworkId, Service.Lb)).thenReturn(false);

        ApplicationLoadBalancerRuleVO lbRule =
            new ApplicationLoadBalancerRuleVO("new", "new", 22, 22, "roundrobin", validGuestNetworkId, validAccountId, 1L, new Ip(validRequestedIp), validGuestNetworkId,
                Scheme.Internal);
        Mockito.when(_lbDao.persist(Matchers.any(ApplicationLoadBalancerRuleVO.class))).thenReturn(lbRule);

        Mockito.when(_lbMgr.validateLbRule(Matchers.any(LoadBalancingRule.class))).thenReturn(true);

        Mockito.when(_firewallDao.setStateToAdd(Matchers.any(FirewallRuleVO.class))).thenReturn(true);

        Mockito.when(_accountMgr.getSystemUser()).thenReturn(new UserVO(1));
        Mockito.when(_accountMgr.getSystemAccount()).thenReturn(new AccountVO(2));
        CallContext.register(_accountMgr.getSystemUser(), _accountMgr.getSystemAccount());

        Mockito.when(_ntwkModel.areServicesSupportedInNetwork(Matchers.anyLong(), Matchers.any(Network.Service.class))).thenReturn(true);

        Map<Network.Capability, String> caps = new HashMap<Network.Capability, String>();
        caps.put(Capability.SupportedProtocols, NetUtils.TCP_PROTO);
        Mockito.when(_ntwkModel.getNetworkServiceCapabilities(Matchers.anyLong(), Matchers.any(Network.Service.class))).thenReturn(caps);

        Mockito.when(_lbDao.countBySourceIp(new Ip(validRequestedIp), validGuestNetworkId)).thenReturn(1L);

    }

    @Override
    @After
    public void tearDown() {
        CallContext.unregister();
    }

    /**
     * TESTS FOR .getApplicationLoadBalancer
     */

    @Test
    //Positive test - retrieve existing lb
        public
        void searchForExistingLoadBalancer() {
        ApplicationLoadBalancerRule rule = _appLbSvc.getApplicationLoadBalancer(existingLbId);
        assertNotNull("Couldn't find existing application load balancer", rule);
    }

    @Test(expected = InvalidParameterValueException.class)
    //Negative test - try to retrieve non-existing lb
        public
        void searchForNonExistingLoadBalancer() throws InvalidParameterValueException {
        _appLbSvc.getApplicationLoadBalancer(nonExistingLbId);
    }

    /**
     * TESTS FOR .deleteApplicationLoadBalancer
     */

    @Test
    //Positive test - delete existing lb
        public
        void deleteExistingLoadBalancer() {
        boolean result = false;
        try {
            result = _appLbSvc.deleteApplicationLoadBalancer(existingLbId);
        } finally {
            assertTrue("Couldn't delete existing application load balancer", result);
        }
    }

    @Test
    //Negative test - try to delete non-existing lb
        public
        void deleteNonExistingLoadBalancer() {
        boolean result = true;
        try {
            result = _appLbSvc.deleteApplicationLoadBalancer(nonExistingLbId);
        } finally {
            assertFalse("Didn't fail when try to delete non-existing load balancer", result);
        }
    }

    /**
     * TESTS FOR .createApplicationLoadBalancer
     * @throws NetworkRuleConflictException
     * @throws InsufficientVirtualNetworkCapacityException
     * @throws InsufficientAddressCapacityException
     */

    @Test(expected = CloudRuntimeException.class)
    //Positive test
        public
        void createValidLoadBalancer() throws InsufficientAddressCapacityException, InsufficientVirtualNetworkCapacityException, NetworkRuleConflictException {
        _appLbSvc.createApplicationLoadBalancer("alena", "alena", Scheme.Internal, validGuestNetworkId, validRequestedIp, 22, 22, "roundrobin", validGuestNetworkId,
            validAccountId, true);
    }

    @Test(expected = UnsupportedServiceException.class)
    //Negative test - only internal scheme value is supported in the current release
        public
        void createPublicLoadBalancer() throws InsufficientAddressCapacityException, InsufficientVirtualNetworkCapacityException, NetworkRuleConflictException {
        _appLbSvc.createApplicationLoadBalancer("alena", "alena", Scheme.Public, validGuestNetworkId, validRequestedIp, 22, 22, "roundrobin", validGuestNetworkId,
            validAccountId, true);
    }

    @Test(expected = InvalidParameterValueException.class)
    //Negative test - invalid SourcePort
        public
        void createWithInvalidSourcePort() throws InsufficientAddressCapacityException, InsufficientVirtualNetworkCapacityException, NetworkRuleConflictException {
        _appLbSvc.createApplicationLoadBalancer("alena", "alena", Scheme.Internal, validGuestNetworkId, validRequestedIp, 65536, 22, "roundrobin", validGuestNetworkId,
            validAccountId, true);
    }

    @Test(expected = InvalidParameterValueException.class)
    //Negative test - invalid instancePort
        public
        void createWithInvalidInstandePort() throws InsufficientAddressCapacityException, InsufficientVirtualNetworkCapacityException, NetworkRuleConflictException {
        _appLbSvc.createApplicationLoadBalancer("alena", "alena", Scheme.Internal, validGuestNetworkId, validRequestedIp, 22, 65536, "roundrobin", validGuestNetworkId,
            validAccountId, true);

    }

    @Test(expected = InvalidParameterValueException.class)
    //Negative test - invalid algorithm
        public
        void createWithInvalidAlgorithm() throws InsufficientAddressCapacityException, InsufficientVirtualNetworkCapacityException, NetworkRuleConflictException {
        String expectedExcText = null;
        _appLbSvc.createApplicationLoadBalancer("alena", "alena", Scheme.Internal, validGuestNetworkId, validRequestedIp, 22, 22, "invalidalgorithm",
            validGuestNetworkId, validAccountId, true);

    }

    @Test(expected = InvalidParameterValueException.class)
    //Negative test - invalid sourceNetworkId (of Public type, which is not supported)
        public
        void createWithInvalidSourceIpNtwk() throws InsufficientAddressCapacityException, InsufficientVirtualNetworkCapacityException, NetworkRuleConflictException {
        _appLbSvc.createApplicationLoadBalancer("alena", "alena", Scheme.Internal, validPublicNetworkId, validRequestedIp, 22, 22, "roundrobin", validGuestNetworkId,
            validAccountId, true);

    }

    @Test(expected = InvalidParameterValueException.class)
    //Negative test - invalid requested IP (outside of guest network cidr range)
        public
        void createWithInvalidRequestedIp() throws InsufficientAddressCapacityException, InsufficientVirtualNetworkCapacityException, NetworkRuleConflictException {

        _appLbSvc.createApplicationLoadBalancer("alena", "alena", Scheme.Internal, validGuestNetworkId, "10.2.1.1", 22, 22, "roundrobin", validGuestNetworkId,
            validAccountId, true);
    }

    private static NetworkVO setId(NetworkVO vo, long id) {
        NetworkVO voToReturn = vo;
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

    @Configuration
    @ComponentScan(basePackageClasses = {NetUtils.class, ApplicationLoadBalancerManagerImpl.class},
                   includeFilters = {@Filter(value = TestConfiguration.Library.class, type = FilterType.CUSTOM)},
                   useDefaultFilters = false)
    public static class TestConfiguration extends SpringUtils.CloudStackTestConfiguration {

        @Bean
        public ApplicationLoadBalancerRuleDao applicationLoadBalancerDao() {
            return Mockito.mock(ApplicationLoadBalancerRuleDao.class);
        }

        @Bean
        IpAddressManager ipAddressManager() {
            return Mockito.mock(IpAddressManager.class);
        }

        @Bean
        public NetworkModel networkModel() {
            return Mockito.mock(NetworkModel.class);
        }

        @Bean
        public AccountManager accountManager() {
            return Mockito.mock(AccountManager.class);
        }

        @Bean
        public LoadBalancingRulesManager loadBalancingRulesManager() {
            return Mockito.mock(LoadBalancingRulesManager.class);
        }

        @Bean
        public LoadBalancingRulesService loadBalancingRulesService() {
            return Mockito.mock(LoadBalancingRulesService.class);
        }

        @Bean
        public FirewallRulesDao firewallRulesDao() {
            return Mockito.mock(FirewallRulesDao.class);
        }

        @Bean
        public ResourceTagDao resourceTagDao() {
            return Mockito.mock(ResourceTagDao.class);
        }

        @Bean
        public NetworkOrchestrationService networkManager() {
            return Mockito.mock(NetworkOrchestrationService.class);
        }

        @Bean
        public UsageEventDao UsageEventDao() {
            return Mockito.mock(UsageEventDao.class);
        }

        public static class Library implements TypeFilter {
            @Override
            public boolean match(MetadataReader mdr, MetadataReaderFactory arg1) throws IOException {
                mdr.getClassMetadata().getClassName();
                ComponentScan cs = TestConfiguration.class.getAnnotation(ComponentScan.class);
                return SpringUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
            }
        }
    }

}
