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
package com.cloud.api.query.dao;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.api.response.ResourceLimitAndCountResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.vo.DomainJoinVO;
import com.cloud.configuration.Resource;
import com.cloud.configuration.Resource.ResourceType;

@RunWith(MockitoJUnitRunner.Silent.class)
public class DomainJoinDaoImplTest {

    private static final long DOMAIN_ID = 2L;

    private DomainJoinDaoImpl domainJoinDao;
    private DomainJoinVO domain;
    private ResourceLimitAndCountResponse response;
    private MockedStatic<ApiDBUtils> apiDBUtils;

    @Before
    public void setUp() {
        // The constructor builds search builders against the database, which setResourceLimits does not need.
        domainJoinDao = mock(DomainJoinDaoImpl.class, Mockito.CALLS_REAL_METHODS);
        domain = mock(DomainJoinVO.class);
        response = mock(ResourceLimitAndCountResponse.class);
        when(domain.getId()).thenReturn(DOMAIN_ID);

        apiDBUtils = mockStatic(ApiDBUtils.class);
        // A domain lookup returns the domain's own limit, or unlimited when none is set.
        apiDBUtils.when(() -> ApiDBUtils.findCorrectResourceLimitForDomain(any(), any(ResourceType.class), anyLong()))
                .thenAnswer(invocation -> limitOrUnlimited(invocation.getArgument(0)));
        apiDBUtils.when(() -> ApiDBUtils.findCorrectResourceLimitForDomain(any(), anyBoolean(), any(ResourceType.class), anyLong()))
                .thenAnswer(invocation -> limitOrUnlimited(invocation.getArgument(0)));
        // The account lookup resolves its id against the account table. Given a domain id it finds the root
        // admin or no account at all, and in both cases answers unlimited.
        apiDBUtils.when(() -> ApiDBUtils.findCorrectResourceLimit(any(), anyLong(), any(ResourceType.class)))
                .thenReturn((long) Resource.RESOURCE_UNLIMITED);
    }

    @After
    public void tearDown() {
        apiDBUtils.close();
    }

    private static long limitOrUnlimited(Long limit) {
        return limit == null ? Resource.RESOURCE_UNLIMITED : limit;
    }

    @Test
    public void testBucketAndObjectStorageLimitsComeFromTheDomain() {
        when(domain.getBucketLimit()).thenReturn(147L);
        when(domain.getObjectStorageLimit()).thenReturn(157L * ResourceType.bytesToGiB);

        domainJoinDao.setResourceLimits(domain, false, response);

        verify(response).setBucketLimit("147");
        verify(response).setBucketAvailable("147");
        verify(response).setObjectStorageLimit("157");
        verify(response).setObjectStorageAvailable("157");
    }

    @Test
    public void testBackupLimitIsNotHiddenByAnUnlimitedSnapshotLimit() {
        when(domain.getSnapshotLimit()).thenReturn((long) Resource.RESOURCE_UNLIMITED);
        when(domain.getBackupLimit()).thenReturn(127L);

        domainJoinDao.setResourceLimits(domain, false, response);

        verify(response).setBackupLimit("127");
        verify(response).setBackupAvailable("127");
    }

    @Test
    public void testBackupStorageLimitIsNotHiddenByAnUnlimitedBackupLimit() {
        when(domain.getBackupLimit()).thenReturn((long) Resource.RESOURCE_UNLIMITED);
        when(domain.getBackupStorageLimit()).thenReturn(137L * ResourceType.bytesToGiB);

        domainJoinDao.setResourceLimits(domain, false, response);

        verify(response).setBackupStorageLimit("137");
        verify(response).setBackupStorageAvailable("137");
    }
}
