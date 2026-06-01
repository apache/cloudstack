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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.apache.cloudstack.mom.webhook.vo.WebhookDeliveryJoinVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class WebhookDeliveryJoinDaoImplTest {
    @Spy
    @InjectMocks
    private WebhookDeliveryJoinDaoImpl webhookDeliveryJoinDao;

    @Mock
    private WebhookDeliveryJoinVO mockWebhookDeliveryJoinVO;
    @Mock
    private SearchBuilder<WebhookDeliveryJoinVO> mockSearchBuilder;
    @Mock
    private SearchCriteria<WebhookDeliveryJoinVO> mockSearchCriteria;

    @Before
    public void setUp() {
        when(mockSearchBuilder.entity()).thenReturn(mockWebhookDeliveryJoinVO);
        when(mockSearchBuilder.create()).thenReturn(mockSearchCriteria);
        doReturn(mockSearchBuilder).when(webhookDeliveryJoinDao).createSearchBuilder();
    }

    @Test
    public void searchAndCountByListApiParametersId() {
        long id = 1L;

        doReturn(new Pair(List.of(mockWebhookDeliveryJoinVO), 1)).when(webhookDeliveryJoinDao)
                .searchAndCount(any(), any());

        Pair<List<WebhookDeliveryJoinVO>, Integer> result =
                webhookDeliveryJoinDao.searchAndCountByListApiParameters(id, null, null,
                        null, null, null, null,null);

        assertNotNull(result);
        assertTrue(result.second() > 0);
        assertFalse(result.first().isEmpty());
        verify(mockSearchBuilder).and(eq("id"), any(), eq(SearchCriteria.Op.EQ));
        verify(mockSearchCriteria).setParameters("id", id);
    }

    @Test
    public void searchAndCountByListApiParametersWebhookId() {
        long webhookId = 1L;

        doReturn(new Pair(List.of(mockWebhookDeliveryJoinVO), 1)).when(webhookDeliveryJoinDao)
                .searchAndCount(any(), any());

        Pair<List<WebhookDeliveryJoinVO>, Integer> result =
                webhookDeliveryJoinDao.searchAndCountByListApiParameters(null, List.of(webhookId),
                        null, null, null, null, null, null);

        assertNotNull(result);
        assertTrue(result.second() > 0);
        assertFalse(result.first().isEmpty());
        verify(mockSearchBuilder).and(eq("webhookId"), any(), eq(SearchCriteria.Op.IN));
        verify(mockSearchCriteria).setParameters("webhookId", 1L);
    }

    @Test
    public void searchAndCountByListApiParametersMgmtKeywordStartEnd() {
        long managementServerId = 1L;
        String keyword = "error";
        Date start = new Date(System.currentTimeMillis() - 10000);
        Date end = new Date();
        Filter searchFilter = new Filter(WebhookDeliveryJoinVO.class, "id", false, 10L, 10L);

        doReturn(new Pair(List.of(mockWebhookDeliveryJoinVO), 1)).when(webhookDeliveryJoinDao)
                .searchAndCount(any(), eq(searchFilter));

        Pair<List<WebhookDeliveryJoinVO>, Integer> result =
                webhookDeliveryJoinDao.searchAndCountByListApiParameters(null, null,
                        managementServerId, keyword, start, end, null, searchFilter);

        assertNotNull(result);
        assertTrue(result.second() > 0);
        assertFalse(result.first().isEmpty());
        verify(mockSearchBuilder).and(eq("managementServerId"), any(), eq(SearchCriteria.Op.EQ));
        verify(mockSearchCriteria).setParameters("managementServerId", managementServerId);
        verify(mockSearchBuilder).and(eq("keyword"), any(), eq(SearchCriteria.Op.LIKE));
        verify(mockSearchCriteria).setParameters("keyword", "%" + keyword + "%");
        verify(mockSearchBuilder).and(eq("startDate"), any(), eq(SearchCriteria.Op.GTEQ));
        verify(mockSearchCriteria).setParameters("startDate", start);
        verify(mockSearchBuilder).and(eq("endDate"), any(), eq(SearchCriteria.Op.LTEQ));
        verify(mockSearchCriteria).setParameters("endDate", end);
    }
}
