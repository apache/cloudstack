// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.cloud.network.vpc;

import com.cloud.configuration.ConfigurationManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.element.NetworkACLServiceProvider;
import com.cloud.network.vpc.NetworkACLItem.State;
import com.cloud.network.vpc.dao.NetworkACLDao;
import com.cloud.network.vpc.dao.VpcGatewayDao;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.server.ResourceTag;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.EntityManager;
import junit.framework.TestCase;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.test.utils.SpringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class NetworkACLManagerTest extends TestCase {
    @Inject
    NetworkACLManagerImpl _aclMgr;

    @Inject
    AccountManager _accountMgr;
    @Inject
    VpcManager _vpcMgr;
    @Inject
    NetworkACLDao _networkACLDao;
    @Inject
    NetworkACLItemDao _networkACLItemDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    NetworkOfferingDao networkOfferingDao;
    @Inject
    NetworkModel _networkModel;
    @Inject
    List<NetworkACLServiceProvider> _networkAclElements;
    @Inject
    VpcService _vpcSvc;
    @Inject
    VpcGatewayDao _vpcGatewayDao;
    @Inject
    private ResourceTagDao resourceTagDao;

    private NetworkACLVO acl;
    private NetworkACLItemVO aclItem;

    @Override
    @Before
    public void setUp() {
        ComponentContext.initComponentsLifeCycle();
        final Account account = new AccountVO("testaccount", 1, "testdomain", Account.Type.NORMAL, UUID.randomUUID().toString());
        final UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);

        CallContext.register(user, account);
        acl = Mockito.mock(NetworkACLVO.class);
        aclItem = Mockito.mock(NetworkACLItemVO.class);
    }

    @Override
    @After
    public void tearDown() {
        CallContext.unregister();
    }

    @Test
    public void testCreateACL() throws Exception {
        Mockito.when(_networkACLDao.persist(Matchers.any(NetworkACLVO.class))).thenReturn(acl);
        assertNotNull(_aclMgr.createNetworkACL("acl_new", "acl desc", 1L, true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testApplyACL() throws Exception {
        final NetworkVO network = Mockito.mock(NetworkVO.class);
        Mockito.when(_networkDao.findById(anyLong())).thenReturn(network);
        Mockito.when(networkOfferingDao.isIpv6Supported(anyLong())).thenReturn(false);
        Mockito.when(_networkModel.isProviderSupportServiceInNetwork(anyLong(), Matchers.any(Network.Service.class), Matchers.any(Network.Provider.class))).thenReturn(true);
        Mockito.when(_networkAclElements.get(0).applyNetworkACLs(Matchers.any(Network.class), Matchers.anyList())).thenReturn(true);
        assertTrue(_aclMgr.applyACLToNetwork(1L));
    }

    @Test
    public void testApplyNetworkACLsOnGatewayAndInGeneral() throws Exception {
        driveTestApplyNetworkACL(true, true, true);
    }

    @Test
    public void testApplyNetworkACLsOnGatewayOnly() throws Exception {
        driveTestApplyNetworkACL(false, false, true);
    }

    @Test
    public void testApplyNetworkACLsButNotOnGateway() throws Exception {
        driveTestApplyNetworkACL(false, true, false);
    }

    @SuppressWarnings("unchecked")
    public void driveTestApplyNetworkACL(final boolean result, final boolean applyNetworkACLs, final boolean applyACLToPrivateGw) throws Exception {
        // In order to test ONLY our scope method, we mock the others
        final NetworkACLManager aclManager = Mockito.spy(_aclMgr);

        // Prepare
        // Reset mocked objects to reuse
        Mockito.reset(_networkACLItemDao);
        Mockito.reset(_networkDao);

        // Make sure it is handled
        final long aclId = 1L;
        final NetworkVO network = Mockito.mock(NetworkVO.class);
        final List<NetworkVO> networks = new ArrayList<>();
        networks.add(network);

        NetworkServiceMapDao ntwkSrvcDao = mock(NetworkServiceMapDao.class);
        when(ntwkSrvcDao.canProviderSupportServiceInNetwork(anyLong(), eq(Network.Service.NetworkACL), nullable(Network.Provider.class))).thenReturn(true);
        Mockito.when(_networkDao.listByAclId(anyLong())).thenReturn(networks);
        Mockito.when(_networkDao.findById(anyLong())).thenReturn(network);
        Mockito.when(networkOfferingDao.isIpv6Supported(anyLong())).thenReturn(false);
        Mockito.when(_networkModel.isProviderSupportServiceInNetwork(anyLong(), any(Network.Service.class), any(Network.Provider.class))).thenReturn(true);
        Mockito.when(_networkAclElements.get(0).getProvider()).thenReturn(Mockito.mock(Network.Provider.class));
        Mockito.when(_networkAclElements.get(0).applyNetworkACLs(any(Network.class), anyList())).thenReturn(applyNetworkACLs);

        // Make sure it applies ACL to private gateway
        final List<VpcGatewayVO> vpcGateways = new ArrayList<VpcGatewayVO>();
        final VpcGatewayVO vpcGateway = Mockito.mock(VpcGatewayVO.class);
        final PrivateGateway privateGateway = Mockito.mock(PrivateGateway.class);
        Mockito.when(_vpcSvc.getVpcPrivateGateway(anyLong())).thenReturn(privateGateway);
        vpcGateways.add(vpcGateway);
        Mockito.when(_vpcGatewayDao.listByAclIdAndType(aclId, VpcGateway.Type.Private)).thenReturn(vpcGateways);

        // Create 4 rules to test all 4 scenarios: only revoke should
        // be deleted, only add should update
        final List<NetworkACLItemVO> rules = new ArrayList<>();
        final NetworkACLItemVO ruleActive = Mockito.mock(NetworkACLItemVO.class);
        final NetworkACLItemVO ruleStaged = Mockito.mock(NetworkACLItemVO.class);
        final NetworkACLItemVO rule2Revoke = Mockito.mock(NetworkACLItemVO.class);
        final NetworkACLItemVO rule2Add = Mockito.mock(NetworkACLItemVO.class);
        Mockito.when(ruleActive.getState()).thenReturn(NetworkACLItem.State.Active);
        Mockito.when(ruleStaged.getState()).thenReturn(NetworkACLItem.State.Staged);
        Mockito.when(rule2Add.getState()).thenReturn(NetworkACLItem.State.Add);
        Mockito.when(rule2Revoke.getState()).thenReturn(NetworkACLItem.State.Revoke);
        rules.add(ruleActive);
        rules.add(ruleStaged);
        rules.add(rule2Add);
        rules.add(rule2Revoke);

        final long revokeId = 8;
        Mockito.when(rule2Revoke.getId()).thenReturn(revokeId);

        final long addId = 9;
        Mockito.when(rule2Add.getId()).thenReturn(addId);
        Mockito.when(_networkACLItemDao.findById(addId)).thenReturn(rule2Add);

        Mockito.when(_networkACLItemDao.listByACL(aclId)).thenReturn(rules);
        // Mock methods to avoid
        Mockito.doReturn(applyACLToPrivateGw).when(aclManager).applyACLToPrivateGw(privateGateway);

        // Execute
        assertEquals("Result was not congruent with applyNetworkACLs and applyACLToPrivateGw", result, aclManager.applyNetworkACL(aclId));

        // Assert if conditions met, network ACL was applied
        final int timesProcessingDone = applyNetworkACLs && applyACLToPrivateGw ? 1 : 0;
        Mockito.verify(rule2Add, Mockito.times(timesProcessingDone)).setState(NetworkACLItem.State.Active);
        Mockito.verify(_networkACLItemDao, Mockito.times(timesProcessingDone)).update(addId, rule2Add);
    }

    @Test
    public void testRevokeACLItem() throws Exception {
        Mockito.when(_networkACLItemDao.findById(anyLong())).thenReturn(aclItem);
        assertTrue(_aclMgr.revokeNetworkACLItem(1L));
    }

    @Test
    public void testRemoveRule() {
        NetworkACLItem aclItem = Mockito.mock(NetworkACLItemVO.class);
        when(aclItem.getId()).thenReturn(1l);
        Mockito.when(resourceTagDao.removeByIdAndType(1l, ResourceTag.ResourceObjectType.NetworkACL)).thenReturn(true);
        Mockito.when(_networkACLItemDao.remove(1l)).thenReturn(true);
        assertTrue(_aclMgr.removeRule(aclItem));

    }

    @Test
    public void deleteNonEmptyACL() throws Exception {
        Mockito.reset(_networkDao);
        final List<NetworkACLItemVO> aclItems = new ArrayList<>();
        aclItems.add(aclItem);
        Mockito.when(_networkACLItemDao.listByACL(anyLong())).thenReturn(aclItems);
        Mockito.when(acl.getId()).thenReturn(3l);
        Mockito.when(_networkACLItemDao.findById(anyLong())).thenReturn(aclItem);
        Mockito.when(aclItem.getState()).thenReturn(State.Add);
        Mockito.when(aclItem.getId()).thenReturn(3l);
        Mockito.when(_networkACLDao.remove(anyLong())).thenReturn(true);

        final boolean result = _aclMgr.deleteNetworkACL(acl);

        Mockito.verify(aclItem, Mockito.times(4)).getState();

        assertTrue("Operation should be successful!", result);
    }

    @Configuration
    @ComponentScan(basePackageClasses = {NetworkACLManagerImpl.class}, includeFilters = {
            @ComponentScan.Filter(value = NetworkACLTestConfiguration.Library.class, type = FilterType.CUSTOM)}, useDefaultFilters = false)
    public static class NetworkACLTestConfiguration extends SpringUtils.CloudStackTestConfiguration {

        @Bean
        public AccountManager accountManager() {
            return Mockito.mock(AccountManager.class);
        }

        @Bean
        public NetworkOrchestrationService networkManager() {
            return Mockito.mock(NetworkOrchestrationService.class);
        }

        @Bean
        public NetworkModel networkModel() {
            return Mockito.mock(NetworkModel.class);
        }

        @Bean
        public VpcManager vpcManager() {
            return Mockito.mock(VpcManager.class);
        }

        @Bean
        public EntityManager entityManager() {
            return Mockito.mock(EntityManager.class);
        }

        @Bean
        public ResourceTagDao resourceTagDao() {
            return Mockito.mock(ResourceTagDao.class);
        }

        @Bean
        public NetworkACLDao networkACLDao() {
            return Mockito.mock(NetworkACLDao.class);
        }

        @Bean
        public NetworkACLItemDao networkACLItemDao() {
            return Mockito.mock(NetworkACLItemDao.class);
        }

        @Bean
        public NetworkDao networkDao() {
            return Mockito.mock(NetworkDao.class);
        }

        @Bean
        public NetworkOfferingDao networkOfferingDao() {
            return Mockito.mock(NetworkOfferingDao.class);
        }

        @Bean
        public ConfigurationManager configMgr() {
            return Mockito.mock(ConfigurationManager.class);
        }

        @Bean
        public NetworkACLServiceProvider networkElements() {
            return Mockito.mock(NetworkACLServiceProvider.class);
        }

        @Bean
        public VpcGatewayDao vpcGatewayDao() {
            return Mockito.mock(VpcGatewayDao.class);
        }

        @Bean
        public VpcService vpcService() {
            return Mockito.mock(VpcService.class);
        }

        @Bean
        public MessageBus messageBus() {
            return Mockito.mock(MessageBus.class);
        }

        public static class Library implements TypeFilter {
            @Override
            public boolean match(final MetadataReader mdr, final MetadataReaderFactory arg1) throws IOException {
                mdr.getClassMetadata().getClassName();
                final ComponentScan cs = NetworkACLTestConfiguration.class.getAnnotation(ComponentScan.class);
                return SpringUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
            }
        }
    }
}
