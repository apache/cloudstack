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

package com.cloud.vpc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

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

import com.cloud.configuration.ConfigurationManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.element.NetworkACLServiceProvider;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.NetworkACLItemDao;
import com.cloud.network.vpc.NetworkACLItemVO;
import com.cloud.network.vpc.NetworkACLManager;
import com.cloud.network.vpc.NetworkACLManagerImpl;
import com.cloud.network.vpc.NetworkACLVO;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.VpcGateway;
import com.cloud.network.vpc.VpcGatewayVO;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcService;
import com.cloud.network.vpc.dao.NetworkACLDao;
import com.cloud.network.vpc.dao.VpcGatewayDao;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class NetworkACLManagerTest extends TestCase {
    @Inject
    NetworkACLManager _aclMgr;

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
    ConfigurationManager _configMgr;
    @Inject
    EntityManager _entityMgr;
    @Inject
    NetworkModel _networkModel;
    @Inject
    List<NetworkACLServiceProvider> _networkAclElements;
    @Inject
    VpcService _vpcSvc;
    @Inject
    VpcGatewayDao _vpcGatewayDao;

    private NetworkACLVO acl;
    private NetworkACLItemVO aclItem;

    @Override
    @Before
    public void setUp() {
        ComponentContext.initComponentsLifeCycle();
        Account account = new AccountVO("testaccount", 1, "testdomain", (short)0, UUID.randomUUID().toString());
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString());

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
        NetworkVO network = Mockito.mock(NetworkVO.class);
        Mockito.when(_networkDao.findById(Matchers.anyLong())).thenReturn(network);
        Mockito.when(_networkModel.isProviderSupportServiceInNetwork(Matchers.anyLong(), Matchers.any(Network.Service.class), Matchers.any(Network.Provider.class)))
            .thenReturn(true);
        Mockito.when(_networkAclElements.get(0).applyNetworkACLs(Matchers.any(Network.class), Matchers.anyList())).thenReturn(true);
        assertTrue(_aclMgr.applyACLToNetwork(1L));
    }

    @Test
    public void testApplyNetworkACL() throws Exception {
        driveTestApplyNetworkACL(true, true, true);
        driveTestApplyNetworkACL(false, false, true);
        driveTestApplyNetworkACL(false, true, false);
    }

    @SuppressWarnings("unchecked")
    public void driveTestApplyNetworkACL(boolean result, boolean applyNetworkACLs, boolean applyACLToPrivateGw) throws Exception {
        // In order to test ONLY our scope method, we mock the others
        NetworkACLManager aclManager = Mockito.spy(_aclMgr);

        // Prepare
        // Reset mocked objects to reuse
        Mockito.reset(_networkACLItemDao);

        // Make sure it is handled
        long aclId = 1L;
        NetworkVO network = Mockito.mock(NetworkVO.class);
        List<NetworkVO> networks = new ArrayList<NetworkVO>();
        networks.add(network);
        Mockito.when(_networkDao.listByAclId(Matchers.anyLong()))
            .thenReturn(networks);
        Mockito.when(_networkDao.findById(Matchers.anyLong())).thenReturn(network);
        Mockito.when(_networkModel.isProviderSupportServiceInNetwork(Matchers.anyLong(),
                Matchers.any(Network.Service.class), Matchers.any(Network.Provider.class)))
                .thenReturn(true);
        Mockito.when(_networkAclElements.get(0).applyNetworkACLs(Matchers.any(Network.class),
                Matchers.anyList())).thenReturn(applyNetworkACLs);

        // Make sure it applies ACL to private gateway
        List<VpcGatewayVO> vpcGateways = new ArrayList<VpcGatewayVO>();
        VpcGatewayVO vpcGateway = Mockito.mock(VpcGatewayVO.class);
        PrivateGateway privateGateway = Mockito.mock(PrivateGateway.class);
        Mockito.when(_vpcSvc.getVpcPrivateGateway(Mockito.anyLong())).thenReturn(privateGateway);
        vpcGateways.add(vpcGateway);
        Mockito.when(_vpcGatewayDao.listByAclIdAndType(aclId, VpcGateway.Type.Private))
            .thenReturn(vpcGateways);

        // Create 4 rules to test all 4 scenarios: only revoke should
        // be deleted, only add should update
        List<NetworkACLItemVO> rules = new ArrayList<NetworkACLItemVO>();
        NetworkACLItemVO ruleActive = Mockito.mock(NetworkACLItemVO.class);
        NetworkACLItemVO ruleStaged = Mockito.mock(NetworkACLItemVO.class);
        NetworkACLItemVO rule2Revoke = Mockito.mock(NetworkACLItemVO.class);
        NetworkACLItemVO rule2Add = Mockito.mock(NetworkACLItemVO.class);
        Mockito.when(ruleActive.getState()).thenReturn(NetworkACLItem.State.Active);
        Mockito.when(ruleStaged.getState()).thenReturn(NetworkACLItem.State.Staged);
        Mockito.when(rule2Add.getState()).thenReturn(NetworkACLItem.State.Add);
        Mockito.when(rule2Revoke.getState()).thenReturn(NetworkACLItem.State.Revoke);
        rules.add(ruleActive);
        rules.add(ruleStaged);
        rules.add(rule2Add);
        rules.add(rule2Revoke);

        long revokeId = 8;
        Mockito.when(rule2Revoke.getId()).thenReturn(revokeId);

        long addId = 9;
        Mockito.when(rule2Add.getId()).thenReturn(addId);
        Mockito.when(_networkACLItemDao.findById(addId)).thenReturn(rule2Add);

        Mockito.when(_networkACLItemDao.listByACL(aclId))
            .thenReturn(rules);
        // Mock methods to avoid
        Mockito.doReturn(applyACLToPrivateGw).when(aclManager).applyACLToPrivateGw(privateGateway);

        // Execute
        assertEquals("Result was not congruent with applyNetworkACLs and applyACLToPrivateGw", result, aclManager.applyNetworkACL(aclId));

        // Assert if conditions met, network ACL was applied
        int timesProcessingDone = (applyNetworkACLs && applyACLToPrivateGw) ? 1 : 0;
        Mockito.verify(_networkACLItemDao, Mockito.times(timesProcessingDone)).remove(revokeId);
        Mockito.verify(rule2Add, Mockito.times(timesProcessingDone)).setState(NetworkACLItem.State.Active);
        Mockito.verify(_networkACLItemDao, Mockito.times(timesProcessingDone)).update(addId, rule2Add);
    }


    @Test
    public void testRevokeACLItem() throws Exception {
        Mockito.when(_networkACLItemDao.findById(Matchers.anyLong())).thenReturn(aclItem);
        assertTrue(_aclMgr.revokeNetworkACLItem(1L));
    }

    @Test
    public void testUpdateACLItem() throws Exception {
        Mockito.when(_networkACLItemDao.findById(Matchers.anyLong())).thenReturn(aclItem);
        Mockito.when(_networkACLItemDao.update(Matchers.anyLong(), Matchers.any(NetworkACLItemVO.class))).thenReturn(true);
        assertNotNull(_aclMgr.updateNetworkACLItem(1L, "UDP", null, NetworkACLItem.TrafficType.Ingress, "Deny", 10, 22, 32, null, null, null, true));
    }

    @Test(expected = CloudRuntimeException.class)
    public void deleteNonEmptyACL() throws Exception {
        List<NetworkACLItemVO> aclItems = new ArrayList<NetworkACLItemVO>();
        aclItems.add(aclItem);
        Mockito.when(_networkACLItemDao.listByACL(Matchers.anyLong())).thenReturn(aclItems);
        _aclMgr.deleteNetworkACL(acl);
    }

    @Configuration
    @ComponentScan(basePackageClasses = {NetworkACLManagerImpl.class}, includeFilters = {@ComponentScan.Filter(value = NetworkACLTestConfiguration.Library.class,
                                                                                                               type = FilterType.CUSTOM)}, useDefaultFilters = false)
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
            public boolean match(MetadataReader mdr, MetadataReaderFactory arg1) throws IOException {
                mdr.getClassMetadata().getClassName();
                ComponentScan cs = NetworkACLTestConfiguration.class.getAnnotation(ComponentScan.class);
                return SpringUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
            }
        }
    }

}
