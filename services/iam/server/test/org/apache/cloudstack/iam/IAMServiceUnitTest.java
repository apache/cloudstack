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
package org.apache.cloudstack.iam;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.junit.After;
import org.apache.cloudstack.test.utils.SpringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
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

import com.cloud.exception.InvalidParameterValueException;

import org.apache.cloudstack.iam.api.AclGroup;
import org.apache.cloudstack.iam.api.AclPolicy;
import org.apache.cloudstack.iam.api.IAMService;
import org.apache.cloudstack.iam.server.AclGroupAccountMapVO;
import org.apache.cloudstack.iam.server.AclGroupVO;
import org.apache.cloudstack.iam.server.AclPolicyVO;
import org.apache.cloudstack.iam.server.IAMServiceImpl;
import org.apache.cloudstack.iam.server.dao.AclGroupAccountMapDao;
import org.apache.cloudstack.iam.server.dao.AclGroupDao;
import org.apache.cloudstack.iam.server.dao.AclGroupPolicyMapDao;
import org.apache.cloudstack.iam.server.dao.AclPolicyDao;
import org.apache.cloudstack.iam.server.dao.AclPolicyPermissionDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class IAMServiceUnitTest {

    @Inject
    IAMService _iamService;

    @Inject
    AclPolicyDao _aclPolicyDao;

    @Inject
    AclGroupDao _aclGroupDao;

    @Inject
    EntityManager _entityMgr;

    @Inject
    AclGroupPolicyMapDao _aclGroupPolicyMapDao;

    @Inject
    AclGroupAccountMapDao _aclGroupAccountMapDao;

    @Inject
    AclPolicyPermissionDao _policyPermissionDao;


    @BeforeClass
    public static void setUpClass() throws ConfigurationException {
    }

    @Before
    public void setUp() {
        ComponentContext.initComponentsLifeCycle();
        AclGroupVO group = new AclGroupVO("group1", "my first group");
        Mockito.when(_aclGroupDao.persist(Mockito.any(AclGroupVO.class))).thenReturn(group);
        List<AclGroupVO> groups = new ArrayList<AclGroupVO>();
        groups.add(group);
        when(_aclGroupDao.search(Mockito.any(SearchCriteria.class), Mockito.any(com.cloud.utils.db.Filter.class)))
                .thenReturn(groups);

        AclPolicyVO policy = new AclPolicyVO("policy1", "my first policy");
        Mockito.when(_aclPolicyDao.persist(Mockito.any(AclPolicyVO.class))).thenReturn(policy);

    }

    @After
    public void tearDown() {
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createAclGroupTest() {
        AclGroup group = _iamService.createAclGroup("group1", "my first group", "/root/mydomain");
        assertNotNull("Acl group 'group1' failed to create ", group);

        AclGroupVO group2 = new AclGroupVO("group1", "my second group");
        when(_aclGroupDao.findByName(eq("/root/mydomain"), eq("group1"))).thenReturn(group2);

        AclGroup group3 = _iamService.createAclGroup("group1", "my first group", "/root/mydomain");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void deleteAclGroupInvalidIdTest(){
        when(_aclGroupDao.findById(20L)).thenReturn(null);
        _iamService.deleteAclGroup(20L);
    }

    @Test
    public void accountGroupMaptest() {
        // create group
        AclGroupVO group = new AclGroupVO("group1", "my first group");

        // add account to group
        List<Long> accountIds = new ArrayList<Long>();
        accountIds.add(100L);
        when(_aclGroupDao.findById(20L)).thenReturn(group);
        _iamService.addAccountsToGroup(accountIds, 20L);

        _iamService.removeAccountsFromGroup(accountIds, 20L);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createAclPolicyTest() {
        AclPolicy policy = _iamService.createAclPolicy("policy1", "my first policy", null);
        assertNotNull("Acl policy 'policy1' failed to create ", policy);

        AclPolicyVO rvo = new AclPolicyVO("policy2", "second policy");
        when(_aclPolicyDao.findByName(eq("policy2"))).thenReturn(rvo);

        _iamService.createAclPolicy("policy2", "second policy", null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void deleteAclPolicyInvalidIdTest() {
        when(_aclPolicyDao.findById(34L)).thenReturn(null);
        _iamService.deleteAclPolicy(34L);
    }

    @Configuration
    @ComponentScan(basePackageClasses = { IAMServiceImpl.class }, includeFilters = { @Filter(value = TestConfiguration.Library.class, type = FilterType.CUSTOM) }, useDefaultFilters = false)
    public static class TestConfiguration extends SpringUtils.CloudStackTestConfiguration {

        @Bean
        public AclPolicyDao aclPolicyDao() {
            return Mockito.mock(AclPolicyDao.class);
        }

        @Bean
        public AclGroupDao aclGroupDao() {
            return Mockito.mock(AclGroupDao.class);
        }

        @Bean
        public EntityManager entityManager() {
            return Mockito.mock(EntityManager.class);
        }

        @Bean
        public AclGroupPolicyMapDao aclGroupPolicyMapDao() {
            return Mockito.mock(AclGroupPolicyMapDao.class);
        }

        @Bean
        public AclGroupAccountMapDao aclGroupAccountMapDao() {
            return Mockito.mock(AclGroupAccountMapDao.class);
        }

        @Bean
        public AclPolicyPermissionDao aclPolicyPermissionDao() {
            return Mockito.mock(AclPolicyPermissionDao.class);
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
