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

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NsxProviderDao;
import com.cloud.network.vpc.NetworkACLItemDao;
import com.cloud.network.vpc.NetworkACLItemVO;
import com.cloud.network.vpc.NetworkACLManager;
import com.cloud.network.vpc.NetworkACLService;
import com.cloud.network.vpc.NetworkACLServiceImpl;
import com.cloud.network.vpc.NetworkACLVO;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcService;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.NetworkACLDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcGatewayDao;
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
import org.apache.cloudstack.test.utils.SpringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
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
import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class NetworkACLServiceTest extends TestCase {

    @Inject
    private NetworkACLService _aclService;
    @Inject
    private NetworkACLManager _networkAclMgr;
    @Inject
    private NetworkACLDao _networkACLDao;
    @Inject
    private NetworkACLItemDao _networkACLItemDao;
    @Inject
    private EntityManager _entityMgr;
    @Inject
    private NsxProviderDao nsxProviderDao;

    private NetworkACLVO acl;
    private NetworkACLItemVO aclItem;

    @Override
    @Before
    public void setUp() {
        ComponentContext.initComponentsLifeCycle();
        Account account = new AccountVO("testaccount", 1, "testdomain", Account.Type.NORMAL, UUID.randomUUID().toString());
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);

        CallContext.register(user, account);
    }

    @Override
    @After
    public void tearDown() {
        CallContext.unregister();
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteDefaultACL() throws Exception {
        Mockito.when(_networkACLDao.findById(ArgumentMatchers.anyLong())).thenReturn(acl);
        Mockito.when(_networkAclMgr.deleteNetworkACL(acl)).thenReturn(true);
        _aclService.deleteNetworkACL(1L);
    }

    @Test
    public void testDeleteACLItem() throws Exception {
        Mockito.when(_networkACLItemDao.findById(ArgumentMatchers.anyLong())).thenReturn(aclItem);
        Mockito.when(_networkAclMgr.getNetworkACL(ArgumentMatchers.anyLong())).thenReturn(acl);
        Mockito.when(_networkAclMgr.revokeNetworkACLItem(ArgumentMatchers.anyLong())).thenReturn(true);
        Mockito.when(_entityMgr.findById(Mockito.eq(Vpc.class), Mockito.anyLong())).thenReturn(new VpcVO());
        assertTrue(_aclService.revokeNetworkACLItem(1L));
    }

    @Configuration
    @ComponentScan(basePackageClasses = {NetworkACLServiceImpl.class}, includeFilters = {
            @ComponentScan.Filter(value = NetworkACLTestConfiguration.Library.class, type = FilterType.CUSTOM)}, useDefaultFilters = false)
    public static class NetworkACLTestConfiguration extends SpringUtils.CloudStackTestConfiguration {

        @Bean
        public EntityManager entityManager() {
            return Mockito.mock(EntityManager.class);
        }

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
        public NetworkACLManager networkACLManager() {
            return Mockito.mock(NetworkACLManager.class);
        }

        @Bean
        public VpcGatewayDao vpcGatewayDao() {
            return Mockito.mock(VpcGatewayDao.class);
        }

        @Bean
        public VpcDao vpcDao() {
            return Mockito.mock(VpcDao.class);
        }

        @Bean
        public VpcService vpcService() {
            return Mockito.mock(VpcService.class);
        }

        @Bean
        public NsxProviderDao nsxProviderDao() { return Mockito.mock(NsxProviderDao.class); }

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
