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
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkService;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.vpc.NetworkACLManager;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcOfferingVO;
import com.cloud.network.vpc.VpcService;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.PrivateIpDao;
import com.cloud.network.vpc.dao.StaticRouteDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcGatewayDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.network.vpc.dao.VpcServiceMapDao;
import com.cloud.network.vpn.Site2SiteVpnManager;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.server.ConfigurationServer;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.UserVO;
import com.cloud.utils.component.ComponentContext;
import com.cloud.vm.dao.DomainRouterDao;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class VpcTest extends TestCase {

    @Inject
    VpcService _vpcService;

    @Inject
    AccountManager _accountMgr;

    @Inject
    VpcManager _vpcMgr;

    @Inject
    VpcDao _vpcDao;

    @Inject
    VpcOfferingDao _vpcOfferinDao;

    private VpcVO vpc;
    private static final Logger s_logger = Logger.getLogger(VpcTest.class);

    @Override
    @Before
    public void setUp() {
        ComponentContext.initComponentsLifeCycle();
        Account account = new AccountVO("testaccount", 1, "testdomain", (short) 0, UUID.randomUUID().toString());
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString());

        CallContext.register(user, account);
        vpc = new VpcVO(1, "myvpc", "myvpc", 2, 1, 1, "10.0.1.0/16", "mydomain");
    }

    @Override
    @After
    public void tearDown() {
        CallContext.unregister();
    }

    @Test
    public void testCreateVpc() throws Exception {
        Mockito.when(
                _vpcMgr.createVpc(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(),
                        Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(vpc);
        Mockito.when(_vpcOfferinDao.persist(Mockito.any(VpcOfferingVO.class))).thenReturn(
                new VpcOfferingVO("test", "test", 1L));
        Vpc vpc1 = _vpcMgr.createVpc(1, 1, 1, "myVpc", "my Vpc", "10.0.0.0/16", "test");
        assertNotNull("Vpc is created", vpc1);
    }

    @Configuration
    @ComponentScan(basePackageClasses = { VpcManager.class }, includeFilters = { @ComponentScan.Filter(value = VpcTestConfiguration.Library.class, type = FilterType.CUSTOM) }, useDefaultFilters = false)
    public static class VpcTestConfiguration extends SpringUtils.CloudStackTestConfiguration {

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
        public VpcDao VpcDao() {
            return Mockito.mock(VpcDao.class);
        }

        @Bean
        public VpcOfferingDao vpcOfferingDao() {
            return Mockito.mock(VpcOfferingDao.class);
        }

        @Bean
        public VpcOfferingServiceMapDao vpcOfferingServiceMapDao() {
            return Mockito.mock(VpcOfferingServiceMapDao.class);
        }

        @Bean
        public ConfigurationDao configurationDao() {
            return Mockito.mock(ConfigurationDao.class);
        }

        @Bean
        public ConfigurationManager configurationManager() {
            return Mockito.mock(ConfigurationManager.class);
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
        public IPAddressDao ipAddressDao() {
            return Mockito.mock(IPAddressDao.class);
        }

        @Bean
        public DomainRouterDao domainRouterDao() {
            return Mockito.mock(DomainRouterDao.class);
        }

        @Bean
        public VpcGatewayDao vpcGatewayDao() {
            return Mockito.mock(VpcGatewayDao.class);
        }

        @Bean
        public PrivateIpDao privateIpDao() {
            return Mockito.mock(PrivateIpDao.class);
        }

        @Bean
        public StaticRouteDao staticRouteDao() {
            return Mockito.mock(StaticRouteDao.class);
        }

        @Bean
        public NetworkOfferingServiceMapDao networkOfferingServiceMapDao() {
            return Mockito.mock(NetworkOfferingServiceMapDao.class);
        }

        @Bean
        public PhysicalNetworkDao physicalNetworkDao() {
            return Mockito.mock(PhysicalNetworkDao.class);
        }

        @Bean
        public FirewallRulesDao firewallRulesDao() {
            return Mockito.mock(FirewallRulesDao.class);
        }

        @Bean
        public Site2SiteVpnGatewayDao site2SiteVpnGatewayDao() {
            return Mockito.mock(Site2SiteVpnGatewayDao.class);
        }

        @Bean
        public Site2SiteVpnManager site2SiteVpnManager() {
            return Mockito.mock(Site2SiteVpnManager.class);
        }

        @Bean
        public VlanDao vlanDao() {
            return Mockito.mock(VlanDao.class);
        }

        @Bean
        public ResourceLimitService resourceLimitService() {
            return Mockito.mock(ResourceLimitService.class);
        }

        @Bean
        public VpcServiceMapDao vpcServiceMapDao() {
            return Mockito.mock(VpcServiceMapDao.class);
        }

        @Bean
        public NetworkService networkService() {
            return Mockito.mock(NetworkService.class);
        }

        @Bean
        public DataCenterDao dataCenterDao() {
            return Mockito.mock(DataCenterDao.class);
        }

        @Bean
        public ConfigurationServer configurationServer() {
            return Mockito.mock(ConfigurationServer.class);
        }

        public static class Library implements TypeFilter {
            @Override
            public boolean match(MetadataReader mdr, MetadataReaderFactory arg1) throws IOException {
                mdr.getClassMetadata().getClassName();
                ComponentScan cs = VpcTestConfiguration.class.getAnnotation(ComponentScan.class);
                return SpringUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
            }
        }
    }

}
