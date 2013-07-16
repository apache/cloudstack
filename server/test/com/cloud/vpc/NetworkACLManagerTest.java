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

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.test.utils.SpringUtils;

import com.cloud.configuration.ConfigurationManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkManager;
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
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.NetworkACLDao;
import com.cloud.network.vpc.dao.VpcGatewayDao;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class NetworkACLManagerTest extends TestCase{
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
    NetworkModel _networkModel;
    @Inject
    List<NetworkACLServiceProvider> _networkAclElements;

    private NetworkACLVO acl;
    private NetworkACLItemVO aclItem;

    private static final Logger s_logger = Logger.getLogger( NetworkACLManagerTest.class);

    @Override
    @Before
    public void setUp() {
        ComponentContext.initComponentsLifeCycle();
        Account account = new AccountVO("testaccount", 1, "testdomain", (short) 0, UUID.randomUUID().toString());
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
        Mockito.when(_networkACLDao.persist(Mockito.any(NetworkACLVO.class))).thenReturn(acl);
        assertNotNull(_aclMgr.createNetworkACL("acl_new", "acl desc", 1L));
    }

    @Test
    public void testApplyACL() throws Exception {
        NetworkVO network = Mockito.mock(NetworkVO.class);
        Mockito.when(_networkDao.findById(Mockito.anyLong())).thenReturn(network);
        Mockito.when(_networkModel.isProviderSupportServiceInNetwork(Mockito.anyLong(), Mockito.any(Network.Service.class), Mockito.any(Network.Provider.class))).thenReturn(true);
        Mockito.when(_networkAclElements.get(0).applyNetworkACLs(Mockito.any(Network.class), Mockito.anyList())).thenReturn(true);
        assertTrue(_aclMgr.applyACLToNetwork(1L));
    }

    @Test
    public void testRevokeACLItem() throws Exception {
        Mockito.when(_networkACLItemDao.findById(Mockito.anyLong())).thenReturn(aclItem);
        assertTrue(_aclMgr.revokeNetworkACLItem(1L));
    }

    @Test
    public void testUpdateACLItem() throws Exception {
        Mockito.when(_networkACLItemDao.findById(Mockito.anyLong())).thenReturn(aclItem);
        Mockito.when(_networkACLItemDao.update(Mockito.anyLong(), Mockito.any(NetworkACLItemVO.class))).thenReturn(true);
        assertNotNull(_aclMgr.updateNetworkACLItem(1L, "UDP", null, NetworkACLItem.TrafficType.Ingress, "Deny", 10, 22, 32, null, null));
    }

    @Test(expected = CloudRuntimeException.class)
    public void deleteNonEmptyACL() throws Exception {
        List<NetworkACLItemVO> aclItems = new ArrayList<NetworkACLItemVO>();
        aclItems.add(aclItem);
        Mockito.when(_networkACLItemDao.listByACL(Mockito.anyLong())).thenReturn(aclItems);
        _aclMgr.deleteNetworkACL(acl);
    }

    @Configuration
    @ComponentScan(basePackageClasses={NetworkACLManagerImpl.class},
            includeFilters={@ComponentScan.Filter(value=NetworkACLTestConfiguration.Library.class, type= FilterType.CUSTOM)},
            useDefaultFilters=false)
    public static class NetworkACLTestConfiguration extends SpringUtils.CloudStackTestConfiguration{

        @Bean
        public AccountManager accountManager() {
            return Mockito.mock(AccountManager.class);
        }

        @Bean
        public NetworkManager networkManager() {
            return Mockito.mock(NetworkManager.class);
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
        public VpcGatewayDao vpcGatewayDao () {
            return Mockito.mock(VpcGatewayDao.class);
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
