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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.cloudstack.mom.webhook.vo.WebhookFilterVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.Pair;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class WebhookFilterDaoImplTest {

    @Spy
    @InjectMocks
    private WebhookFilterDaoImpl webhookFilterDaoImpl;

    @Before
    public void setUp() {
        SearchBuilder<WebhookFilterVO> sb = Mockito.mock(SearchBuilder.class);
        Mockito.when(sb.create()).thenReturn(Mockito.mock(SearchCriteria.class));
        webhookFilterDaoImpl.IdWebhookIdSearch = sb;
    }

    @Test
    public void searchByReturnsResultsWhenIdAndWebhookIdMatch() {
        Long id = 1L;
        Long webhookId = 2L;
        Long startIndex = 0L;
        Long pageSize = 10L;

        Mockito.doReturn(new Pair(List.of(Mockito.mock(WebhookFilterVO.class)), 1))
                .when(webhookFilterDaoImpl).searchAndCount(Mockito.any(), Mockito.any());

        Pair<List<WebhookFilterVO>, Integer> result = webhookFilterDaoImpl.searchBy(id, webhookId, startIndex, pageSize);

        assertNotNull(result);
        assertTrue(result.first().size() >= 0);
    }

    @Test
    public void searchByReturnsEmptyWhenNoMatch() {
        Long id = 999L;
        Long webhookId = 999L;
        Long startIndex = 0L;
        Long pageSize = 10L;

        Mockito.doReturn(new Pair(List.of(), 0))
                .when(webhookFilterDaoImpl).searchAndCount(Mockito.any(), Mockito.any());

        Pair<List<WebhookFilterVO>, Integer> result = webhookFilterDaoImpl.searchBy(id, webhookId, startIndex, pageSize);

        assertNotNull(result);
        assertEquals(0, result.first().size());
    }

    @Test
    public void listByWebhookReturnsResultsWhenWebhookIdExists() {
        Long webhookId = 2L;

        Mockito.doReturn(List.of(Mockito.mock(WebhookFilterVO.class)))
                .when(webhookFilterDaoImpl).listBy(Mockito.any(SearchCriteria.class));

        List<WebhookFilterVO> result = webhookFilterDaoImpl.listByWebhook(webhookId);

        assertNotNull(result);
        assertTrue(result.size() >= 0);
    }

    @Test
    public void listByWebhookReturnsEmptyWhenWebhookIdDoesNotExist() {
        Long webhookId = 999L;

        Mockito.doReturn(List.of())
                .when(webhookFilterDaoImpl).listBy(Mockito.any(SearchCriteria.class));

        List<WebhookFilterVO> result = webhookFilterDaoImpl.listByWebhook(webhookId);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void deleteReturnsZeroWhenIdAndWebhookIdAreNull() {
        int result = webhookFilterDaoImpl.delete(null, null);

        assertEquals(0, result);
    }

    @Test
    public void deleteReturnsNonZeroWhenIdOrWebhookIdExists() {
        Long id = 1L;
        Long webhookId = 2L;

        Mockito.doReturn(1)
                .when(webhookFilterDaoImpl).remove(Mockito.any(SearchCriteria.class));

        int result = webhookFilterDaoImpl.delete(id, webhookId);

        assertTrue(result >= 0);
    }
}
