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

package org.apache.cloudstack.mom.webhook.dao;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.mom.webhook.vo.WebhookJoinVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class WebhookJoinDaoImplTest {
    @Spy
    @InjectMocks
    private WebhookJoinDaoImpl webhookJoinDao;

    @Mock
    private WebhookJoinVO mockWebhookVO;
    @Mock
    private SearchBuilder<WebhookJoinVO> mockSearchBuilder;
    @Mock
    private SearchCriteria<WebhookJoinVO> mockSearchCriteria;

    @Before
    public void setUp() {
        when(mockSearchBuilder.entity()).thenReturn(mockWebhookVO);
        when(mockSearchBuilder.and()).thenReturn(mockSearchBuilder);
        when(mockSearchBuilder.create()).thenReturn(mockSearchCriteria);
        doReturn(mockSearchBuilder).when(webhookJoinDao).createSearchBuilder();
    }

    @Test
    public void listByAccountOrDomainReturnsResultsWhenAccountIdMatches() {
        long accountId = 1L;

        doReturn(List.of(mockWebhookVO)).when(webhookJoinDao).listBy(any(SearchCriteria.class));

        List<WebhookJoinVO> result = webhookJoinDao.listByAccountOrDomain(accountId, null);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(mockSearchBuilder).op(eq("accountId"), any(), eq(SearchCriteria.Op.EQ));
        verify(mockSearchCriteria).setParameters("accountId", accountId);
        verify(mockSearchBuilder, never()).or(eq("domainPath"), any(), eq(SearchCriteria.Op.LIKE));
        verify(mockSearchCriteria, never()).setParameters(eq("domainPath"), any());
    }

    @Test
    public void listByAccountOrDomainReturnsResultsWhenBothAccountIdAndDomainPathMatch() {
        long accountId = 10L;
        String domainPath = "domain/path";

        doReturn(List.of(mockWebhookVO)).when(webhookJoinDao).listBy(any(SearchCriteria.class));

        List<WebhookJoinVO> result = webhookJoinDao.listByAccountOrDomain(accountId, domainPath);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(mockSearchBuilder).op(eq("accountId"), any(), eq(SearchCriteria.Op.EQ));
        verify(mockSearchCriteria).setParameters("accountId", accountId);
        verify(mockSearchBuilder).or(eq("domainPath"), any(), eq(SearchCriteria.Op.LIKE));
        verify(mockSearchCriteria).setParameters("domainPath", domainPath);
    }

    @Test
    public void listByAccountOrDomainReturnsEmptyWhenNoMatchFound() {
        long accountId = 999L;
        String domainPath = "nonexistent/path";

        doReturn(Collections.emptyList()).when(webhookJoinDao).listBy(any(SearchCriteria.class));

        List<WebhookJoinVO> result = webhookJoinDao.listByAccountOrDomain(accountId, domainPath);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mockSearchBuilder).op(eq("accountId"), any(), eq(SearchCriteria.Op.EQ));
        verify(mockSearchCriteria).setParameters("accountId", accountId);
        verify(mockSearchBuilder).or(eq("domainPath"), any(), eq(SearchCriteria.Op.LIKE));
        verify(mockSearchCriteria).setParameters("domainPath", domainPath);
    }
}
