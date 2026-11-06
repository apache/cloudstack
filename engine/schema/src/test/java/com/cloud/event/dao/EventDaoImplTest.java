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
package com.cloud.event.dao;

import com.cloud.event.Event;
import com.cloud.event.EventVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchCriteria;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class EventDaoImplTest {

    @Spy
    @InjectMocks
    EventDaoImpl eventDaoImpl = new EventDaoImpl();

    @Test
    public void listLatestOutcomeEventsByResourceQueriesOutcomesByResourceAndTypeExcludingArchived() {
        List<EventVO> expected = Collections.singletonList(new EventVO());
        doReturn(expected).when(eventDaoImpl).listBy(any(SearchCriteria.class), any(Filter.class));

        List<EventVO> result = eventDaoImpl.listLatestOutcomeEventsByResource(1L, "Volume", "SNAPSHOT.CREATE", 3);

        Assert.assertSame(expected, result);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        ArgumentCaptor<Filter> filterCaptor = ArgumentCaptor.forClass(Filter.class);
        verify(eventDaoImpl).listBy(scCaptor.capture(), filterCaptor.capture());

        SearchCriteria<EventVO> sc = scCaptor.getValue();
        String whereClause = sc.getWhereClause();
        for (String predicate : List.of("event.resource_id = ?", "event.resource_type = ?", "event.type = ?", "event.state IN (?,?)", "event.archived = ?")) {
            Assert.assertTrue("missing predicate: " + predicate, whereClause.contains(predicate));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object[]> params = (Map<String, Object[]>) ReflectionTestUtils.getField(sc, "_params");
        Assert.assertArrayEquals(new Object[]{1L}, params.get("resourceId"));
        Assert.assertArrayEquals(new Object[]{"Volume"}, params.get("resourceType"));
        Assert.assertArrayEquals(new Object[]{"SNAPSHOT.CREATE"}, params.get("type"));
        Assert.assertArrayEquals(new Object[]{Event.State.Created, Event.State.Completed}, params.get("state"));
        Assert.assertArrayEquals(new Object[]{false}, params.get("archived"));

        Filter filter = filterCaptor.getValue();
        Assert.assertEquals(" ORDER BY event.created DESC , event.id DESC ", filter.getOrderBy());
        Assert.assertEquals(Long.valueOf(0L), filter.getOffset());
        Assert.assertEquals(Long.valueOf(3L), filter.getLimit());
    }

    @Test
    public void listLatestOutcomeEventsByResourceReturnsEmptyListWhenNoneFound() {
        doReturn(Collections.emptyList()).when(eventDaoImpl).listBy(any(SearchCriteria.class), any(Filter.class));

        List<EventVO> result = eventDaoImpl.listLatestOutcomeEventsByResource(1L, "Volume", "SNAPSHOT.CREATE", 3);

        Assert.assertTrue(result.isEmpty());
    }
}
