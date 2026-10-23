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
package com.cloud.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.configuration.Resource;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.ResourceLimitService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ApiDBUtilsTest {

    private static final long DOMAIN_ID = 2L;

    private DomainDao originalDomainDao;
    private ResourceLimitService originalResourceLimitMgr;
    private DomainVO domain;
    private ResourceLimitService resourceLimitMgr;

    @Before
    public void setUp() {
        originalDomainDao = ApiDBUtils.s_domainDao;
        originalResourceLimitMgr = ApiDBUtils.s_resourceLimitMgr;

        domain = mock(DomainVO.class);
        DomainDao domainDao = mock(DomainDao.class);
        when(domainDao.findById(DOMAIN_ID)).thenReturn(domain);
        resourceLimitMgr = mock(ResourceLimitService.class);
        ApiDBUtils.s_domainDao = domainDao;
        ApiDBUtils.s_resourceLimitMgr = resourceLimitMgr;
    }

    @After
    public void tearDown() {
        ApiDBUtils.s_domainDao = originalDomainDao;
        ApiDBUtils.s_resourceLimitMgr = originalResourceLimitMgr;
    }

    @Test
    public void testDomainWithoutLimitGetsTheEnforcedLimit() {
        // max.domain.buckets is enforced when the domain has no bucket limit of its own
        when(resourceLimitMgr.findCorrectResourceLimitForDomain(domain, ResourceType.bucket, null)).thenReturn(20L);
        when(resourceLimitMgr.findDefaultResourceLimitForDomain(ResourceType.bucket)).thenReturn((long) Resource.RESOURCE_UNLIMITED);

        Assert.assertEquals(20L, ApiDBUtils.findCorrectResourceLimitForDomain(null, ResourceType.bucket, DOMAIN_ID));
    }

    @Test
    public void testDomainLimitIsUsedWhenSet() {
        Assert.assertEquals(7L, ApiDBUtils.findCorrectResourceLimitForDomain(7L, ResourceType.bucket, DOMAIN_ID));
    }

    @Test
    public void testRootDomainIsUnlimited() {
        Assert.assertEquals(Resource.RESOURCE_UNLIMITED,
                ApiDBUtils.findCorrectResourceLimitForDomain(null, ResourceType.bucket, Domain.ROOT_DOMAIN));
    }
}
