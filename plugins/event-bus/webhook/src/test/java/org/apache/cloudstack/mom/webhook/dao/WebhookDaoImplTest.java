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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.mom.webhook.Webhook;
import org.apache.cloudstack.mom.webhook.vo.WebhookVO;
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
public class WebhookDaoImplTest {
    @Spy
    @InjectMocks
    private WebhookDaoImpl webhookDao;

    @Mock
    private WebhookVO mockWebhookVO;
    @Mock
    private SearchBuilder<WebhookVO> mockSearchBuilder;
    @Mock
    private SearchCriteria<WebhookVO> mockSearchCriteria;

    @Before
    public void setUp() {
        when(mockSearchBuilder.entity()).thenReturn(mockWebhookVO);
        when(mockSearchBuilder.and()).thenReturn(mockSearchBuilder);
        when(mockSearchBuilder.or()).thenReturn(mockSearchBuilder);
        when(mockSearchBuilder.create()).thenReturn(mockSearchCriteria);
        doReturn(mockSearchBuilder).when(webhookDao).createSearchBuilder();
        webhookDao.accountIdSearch = mockSearchBuilder;
    }

    @Test
    public void listByEnabledForDeliveryReturnsWebhooksWhenAccountIdAndDomainIdsMatch() {
        Long accountId = 1L;
        List<Long> domainIds = List.of(2L, 3L);

        doReturn(List.of(mockWebhookVO)).when(webhookDao).listBy(any(SearchCriteria.class));

        List<WebhookVO> result = webhookDao.listByEnabledForDelivery(accountId, domainIds);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(mockSearchCriteria).setParameters("state", Webhook.State.Enabled.name());
        verify(mockSearchCriteria).setParameters("scopeGlobal", Webhook.Scope.Global.name());
        verify(mockSearchCriteria).setParameters("scopeLocal", Webhook.Scope.Local.name());
        verify(mockSearchCriteria).setParameters("scopeDomain", Webhook.Scope.Domain.name());
        verify(mockSearchCriteria).setParameters("domainId", 2L, 3L);
    }

    @Test
    public void listByEnabledForDeliveryReturnsEmptyWhenNoMatchFound() {
        Long accountId = 100L;
        List<Long> domainIds = Collections.emptyList();

        doReturn(Collections.emptyList()).when(webhookDao).listBy(any(SearchCriteria.class));

        List<WebhookVO> result = webhookDao.listByEnabledForDelivery(accountId, domainIds);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mockSearchCriteria, never()).setParameters("scopeDomain", Webhook.Scope.Domain.name());
        verify(mockSearchCriteria, never()).setParameters(eq("domainId"), any());
    }

    @Test
    public void deleteByAccountRemovesWebhooksForGivenAccountId() {
        long accountId = 1L;

        doReturn(1).when(webhookDao).remove(any(SearchCriteria.class));

        webhookDao.deleteByAccount(accountId);

        verify(webhookDao, times(1)).remove(any(SearchCriteria.class));
        verify(mockSearchCriteria).setParameters("accountId", accountId);
    }

    @Test
    public void listByAccountReturnsWebhooksForGivenAccountId() {
        long accountId = 1L;

        doReturn(List.of(mockWebhookVO)).when(webhookDao).listBy(any(SearchCriteria.class));

        List<WebhookVO> result = webhookDao.listByAccount(accountId);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(mockSearchCriteria).setParameters("accountId", accountId);
    }

    @Test
    public void listByAccountReturnsEmptyWhenNoWebhooksExistForAccountId() {
        long accountId = 1L;

        doReturn(Collections.emptyList()).when(webhookDao).listBy(any(SearchCriteria.class));

        List<WebhookVO> result = webhookDao.listByAccount(accountId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mockSearchCriteria).setParameters("accountId", accountId);
    }

    @Test
    public void findByAccountAndPayloadUrlReturnsWebhookWhenMatchFound() {
        long accountId = 1L;
        String payloadUrl = "http://example.com";

        doReturn(mockWebhookVO).when(webhookDao).findOneBy(any());

        WebhookVO result = webhookDao.findByAccountAndPayloadUrl(accountId, payloadUrl);

        assertNotNull(result);
        verify(mockSearchCriteria).setParameters("accountId", accountId);
        verify(mockSearchCriteria).setParameters("payloadUrl", payloadUrl);
    }

    @Test
    public void findByAccountAndPayloadUrlReturnsNullWhenNoMatchFound() {
        long accountId = 1L;
        String payloadUrl = "http://example.com";

        doReturn(null).when(webhookDao).findOneBy(any());

        WebhookVO result = webhookDao.findByAccountAndPayloadUrl(accountId, payloadUrl);

        assertNull(result);
        verify(mockSearchCriteria).setParameters("accountId", accountId);
        verify(mockSearchCriteria).setParameters("payloadUrl", payloadUrl);
    }
}
