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
package org.apache.cloudstack.dedicated.manager;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.UUID;

import javax.inject.Inject;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.apache.cloudstack.dedicated.DedicatedResourceManagerImpl;
import org.apache.cloudstack.test.utils.SpringUtils;

import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DedicatedResourceVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.dao.HostDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.dao.UserVmDao;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class DedicatedApiUnitTest {
    public static final Logger s_logger = Logger.getLogger(DedicatedApiUnitTest.class);
    @Inject
    DedicatedResourceManagerImpl _dedicatedService = new DedicatedResourceManagerImpl();

    @Inject
    AccountManager _acctMgr;

    @Inject
    AccountDao _accountDao;

    @Inject
    DomainDao _domainDao;

    @Inject
    UserVmDao _vmDao;

    @Inject
    DedicatedResourceDao _dedicatedDao;

    @Inject
    DataCenterDao _dcDao;

    @Inject
    HostPodDao _podDao;

    @Inject
    ClusterDao _clusterDao;

    @Inject
    HostDao _hostDao;

    @Inject
    ConfigurationDao _configDao;

    private static long domainId = 5L;
    private static long accountId = 5L;
    private static String accountName = "admin";

    @Before
    public void setUp() {
        ComponentContext.initComponentsLifeCycle();
        AccountVO account = new AccountVO(accountName, domainId, "networkDomain", Account.ACCOUNT_TYPE_NORMAL, "uuid");
        DomainVO domain = new DomainVO("rootDomain", 5L, 5L, "networkDomain");

        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString());

        CallContext.register(user, account);
        when(_acctMgr.finalizeOwner((Account) anyObject(), anyString(), anyLong(), anyLong())).thenReturn(account);
        when(_accountDao.findByIdIncludingRemoved(0L)).thenReturn(account);
        when(_accountDao.findById(anyLong())).thenReturn(account);
        when(_domainDao.findById(domainId)).thenReturn(domain);
    }

    @After
    public void tearDown() {
        CallContext.unregister();
    }

    @Test(expected = InvalidParameterValueException.class)
    public void InvalidDomainIDForAccountTest() {
        _dedicatedService.dedicateZone(10L, domainId, accountName);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void dedicateResourceInvalidAccountIDTest() {
        _dedicatedService.dedicateZone(10L, domainId, accountName);
    }

    @Test
    public void releaseDedicatedZoneInvalidIdTest() {
        when(_dedicatedDao.findByZoneId(10L)).thenReturn(null);
        try {
            _dedicatedService.releaseDedicatedResource(10L, null, null, null);
        } catch (InvalidParameterValueException e) {
            Assert.assertTrue(e.getMessage().contains(
                    "No Dedicated Resource available to release"));
        }
    }

/*    @Test
    public void runDedicateZoneTest() {
        DataCenterVO dc = new DataCenterVO(10L, "TestZone", "Dedicated",
                "8.8.8.8", null, "10.0.0.1", null, "10.0.0.1/24", null, null,
                NetworkType.Basic, null, null);
        when(_dcDao.findById(10L)).thenReturn(dc);
        try {
            List<DedicatedResourceVO> result = _dedicatedService.dedicateZone(10L, domainId, accountName);
            Assert.assertNotNull(result);
        } catch (Exception e) {
            s_logger.info("exception in testing dedication of zone "
                    + e.toString());
        }
    }

    @Test
    public void runDedicatePodTest() {
        HostPodVO pod = new HostPodVO("TestPod", 20L, "10.0.0.1", "10.0.0.0",
                22, null);
        when(_podDao.findById(10L)).thenReturn(pod);
        try {
            List<DedicatedResourceVO> result = _dedicatedService.dedicatePod(10L, domainId, accountName);
            Assert.assertNotNull(result);
        } catch (Exception e) {
            s_logger.info("exception in testing dedication of pod "
                    + e.toString());
        }
    }

    @Test
    public void runDedicateClusterTest() {
        ClusterVO cluster = new ClusterVO(10L, 10L, "TestCluster");
        when(_clusterDao.findById(10L)).thenReturn(cluster);
        try {
            List<DedicatedResourceVO> result = _dedicatedService.dedicateCluster(10L, domainId, accountName);
            Assert.assertNotNull(result);
        } catch (Exception e) {
            s_logger.info("exception in testing dedication of cluster "
                    + e.toString());
        }
    }

    @Test
    public void runDedicateHostTest() {
        HostVO host = new HostVO(10L, "Host-1", Host.Type.Routing, null,
                "10.0.0.0", null, null, null, null, null, null, null, null,
                Status.Up, null, null, null, 10L, 10L, 30L, 10233, null, null,
                null, 0, null);
        when(_hostDao.findById(10L)).thenReturn(host);
        try {
            List<DedicatedResourceVO> result = _dedicatedService.dedicateHost(10L, domainId, accountName);
            Assert.assertNotNull(result);
        } catch (Exception e) {
            s_logger.info("exception in testing dedication of host "
                    + e.toString());
        }
    }
*/

    @Test(expected = CloudRuntimeException.class)
    public void dedicateZoneExistTest() {
        DedicatedResourceVO dr = new DedicatedResourceVO(10L, null, null, null, domainId, accountId);
        when(_dedicatedDao.findByZoneId(10L)).thenReturn(dr);
        _dedicatedService.dedicateZone(10L, domainId, accountName);
    }

    @Test(expected = CloudRuntimeException.class)
    public void dedicatePodExistTest() {
        DedicatedResourceVO dr = new DedicatedResourceVO(null, 10L, null, null, domainId, accountId);
        when(_dedicatedDao.findByPodId(10L)).thenReturn(dr);
        _dedicatedService.dedicatePod(10L, domainId, accountName);
    }

    @Test(expected = CloudRuntimeException.class)
    public void dedicateClusterExistTest() {
        DedicatedResourceVO dr = new DedicatedResourceVO(null, null, 10L, null, domainId, accountId);
        when(_dedicatedDao.findByClusterId(10L)).thenReturn(dr);
        _dedicatedService.dedicateCluster(10L, domainId, accountName);
    }

    @Test(expected = CloudRuntimeException.class)
    public void dedicateHostExistTest() {
        DedicatedResourceVO dr = new DedicatedResourceVO(null, null, null, 10L, domainId, accountId);
        when(_dedicatedDao.findByHostId(10L)).thenReturn(dr);
        _dedicatedService.dedicateHost(10L, domainId, accountName);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void releaseDedicatedPodInvalidIdTest() {
        when(_dedicatedDao.findByPodId(10L)).thenReturn(null);
        _dedicatedService.releaseDedicatedResource(null, 10L, null, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void releaseDedicatedClusterInvalidIdTest() {
        when(_dedicatedDao.findByClusterId(10L)).thenReturn(null);
        _dedicatedService.releaseDedicatedResource(null, null, 10L, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void releaseDedicatedHostInvalidIdTest() {
        when(_dedicatedDao.findByHostId(10L)).thenReturn(null);
        _dedicatedService.releaseDedicatedResource(null, null, null, 10L);
    }

    @Configuration
    @ComponentScan(basePackageClasses = {DedicatedResourceManagerImpl.class},
    includeFilters = {@Filter(value = TestConfiguration.Library.class,
    type = FilterType.CUSTOM)}, useDefaultFilters = false)
    public static class TestConfiguration extends SpringUtils.CloudStackTestConfiguration {

        @Bean
        public AccountDao accountDao() {
            return Mockito.mock(AccountDao.class);
        }

        @Bean
        public DomainDao domainDao() {
            return Mockito.mock(DomainDao.class);
        }

        @Bean
        public DedicatedResourceDao dedicatedDao() {
            return Mockito.mock(DedicatedResourceDao.class);
        }

        @Bean
        public HostDao hostDao() {
            return Mockito.mock(HostDao.class);
        }

        @Bean
        public AccountManager acctManager() {
            return Mockito.mock(AccountManager.class);
        }

        @Bean
        public UserVmDao userVmDao() {
            return Mockito.mock(UserVmDao.class);
        }
        @Bean
        public DataCenterDao dataCenterDao() {
            return Mockito.mock(DataCenterDao.class);
        }
        @Bean
        public HostPodDao hostPodDao() {
            return Mockito.mock(HostPodDao.class);
        }

        @Bean
        public ClusterDao clusterDao() {
            return Mockito.mock(ClusterDao.class);
        }

        @Bean
        public ConfigurationDao configDao() {
            return Mockito.mock(ConfigurationDao.class);
        }

        public static class Library implements TypeFilter {
            @Override
            public boolean match(MetadataReader mdr, MetadataReaderFactory arg1) throws IOException {
                ComponentScan cs = TestConfiguration.class.getAnnotation(ComponentScan.class);
                return SpringUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
            }
        }
    }
}
