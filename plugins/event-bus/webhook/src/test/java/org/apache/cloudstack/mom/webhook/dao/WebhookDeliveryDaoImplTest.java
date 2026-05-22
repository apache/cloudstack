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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.cloudstack.mom.webhook.vo.WebhookDeliveryVO;
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
public class WebhookDeliveryDaoImplTest {
    @Spy
    @InjectMocks
    private WebhookDeliveryDaoImpl webhookDeliveryDao;

    @Mock
    private WebhookDeliveryVO mockWebhookDeliveryVO;
    @Mock
    private SearchBuilder<WebhookDeliveryVO> mockSearchBuilder;
    @Mock
    private SearchCriteria<WebhookDeliveryVO> mockSearchCriteria;

    @Before
    public void setUp() {
        when(mockSearchBuilder.entity()).thenReturn(mockWebhookDeliveryVO);
        when(mockSearchBuilder.create()).thenReturn(mockSearchCriteria);
        doReturn(mockSearchBuilder).when(webhookDeliveryDao).createSearchBuilder();
    }

    @Test
    public void deleteByDeleteApiParamsDeletesWhenParametersMatch() {
        Long webhookId = 2L;
        Date startDate = new Date(System.currentTimeMillis() - 10000);

        doReturn(1).when(webhookDeliveryDao).remove(any(SearchCriteria.class));

        int result = webhookDeliveryDao.deleteByDeleteApiParams(null, webhookId, null, startDate, null);

        assertEquals(1, result);
        verify(webhookDeliveryDao).remove(any(SearchCriteria.class));
        verify(mockSearchBuilder).and(eq("webhookId"), any(), eq(SearchCriteria.Op.EQ));
        verify(mockSearchCriteria).setParameters("webhookId", webhookId);
    }

    @Test
    public void deleteByDeleteApiParamsReturnsZeroWhenNoMatchFound() {
        Long id = 999L;
        Long webhookId = 999L;
        Long managementServerId = 999L;
        Date startDate = new Date(System.currentTimeMillis() - 10000);
        Date endDate = new Date();

        doReturn(0).when(webhookDeliveryDao).remove(any(SearchCriteria.class));

        int result = webhookDeliveryDao.deleteByDeleteApiParams(id, webhookId, managementServerId, startDate, endDate);

        assertEquals(0, result);
    }

    @Test
    public void removeOlderDeliveriesWhenParametersMatch() {
        long webhookId = 2L;

        WebhookDeliveryVO d1 = mock(WebhookDeliveryVO.class);
        when(d1.getId()).thenReturn(1L);
        WebhookDeliveryVO d2 = mock(WebhookDeliveryVO.class);
        when(d2.getId()).thenReturn(2L);
        List<WebhookDeliveryVO> list = List.of(d1, d2);
        doReturn(list).when(webhookDeliveryDao).listBy(any(SearchCriteria.class), any());
        doReturn(10).when(webhookDeliveryDao).remove(any(SearchCriteria.class));

        webhookDeliveryDao.removeOlderDeliveries(webhookId, 10);
        verify(webhookDeliveryDao).remove(any(SearchCriteria.class));
        verify(mockSearchBuilder).and(eq("webhookId"), any(), eq(SearchCriteria.Op.EQ));
        verify(mockSearchCriteria).setParameters("webhookId", webhookId);
        verify(mockSearchBuilder).and(eq("id"), any(), eq(SearchCriteria.Op.NOTIN));
        verify(mockSearchCriteria).setParameters("id", 1L, 2L);
    }

    @Test
    public void removeOlderDeliveriesWhenNoKeepDeliveries() {
        long webhookId = 2L;
        doReturn(Collections.emptyList()).when(webhookDeliveryDao).listBy(any(SearchCriteria.class), any());

        webhookDeliveryDao.removeOlderDeliveries(webhookId, 10);
        verify(webhookDeliveryDao, never()).remove(any(SearchCriteria.class));
        verify(mockSearchBuilder).and(eq("webhookId"), any(), eq(SearchCriteria.Op.EQ));
        verify(mockSearchCriteria).setParameters("webhookId", webhookId);
        verify(mockSearchBuilder, never()).and(eq("id"), any(), eq(SearchCriteria.Op.NOTIN));
    }
}
