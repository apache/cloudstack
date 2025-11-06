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
package org.apache.cloudstack.resourcedetail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.ResourceDetail;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class ResourceDetailsDaoBaseTest {
    @Spy
    @InjectMocks
    TestDetailsDao testDetailsDao = new TestDetailsDao();

    private SearchBuilder<TestDetailVO> searchBuilder;
    private SearchCriteria<TestDetailVO> searchCriteria;

    @Before
    public void setUp() {
        searchBuilder = mock(SearchBuilder.class);
        searchCriteria = mock(SearchCriteria.class);
        TestDetailVO entityVO = mock(TestDetailVO.class);
        when(searchBuilder.entity()).thenReturn(entityVO);
        searchCriteria = mock(SearchCriteria.class);
        doReturn(searchBuilder).when(testDetailsDao).createSearchBuilder();
        when(searchBuilder.create()).thenReturn(searchCriteria);
    }

    @Test
    public void testListDetailsKeyPairs() {
        long resourceId = 1L;
        List<String> keys = Arrays.asList("key1", "key2");
        TestDetailVO result1 = mock(TestDetailVO.class);
        when(result1.getName()).thenReturn("key1");
        when(result1.getValue()).thenReturn("value1");
        TestDetailVO result2 = mock(TestDetailVO.class);
        when(result2.getName()).thenReturn("key2");
        when(result2.getValue()).thenReturn("value2");
        List<TestDetailVO> mockResults = Arrays.asList(result1, result2);
        doReturn(mockResults).when(testDetailsDao).search(any(SearchCriteria.class), isNull());
        Map<String, String> result = testDetailsDao.listDetailsKeyPairs(resourceId, keys);
        verify(searchBuilder).and(eq("resourceId"), any(), eq(SearchCriteria.Op.EQ));
        verify(searchBuilder).and(eq("name"), any(), eq(SearchCriteria.Op.IN));
        verify(searchBuilder).done();
        verify(searchCriteria).setParameters("resourceId", resourceId);
        verify(searchCriteria).setParameters("name", keys.toArray());
        verify(testDetailsDao).search(searchCriteria, null);
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
    }

    @Test
    public void testListDetailsKeyPairsEmptyResult() {
        long resourceId = 1L;
        List<String> keys = Arrays.asList("key1", "key2");
        doReturn(Collections.emptyList()).when(testDetailsDao).search(any(SearchCriteria.class), isNull());
        Map<String, String> result = testDetailsDao.listDetailsKeyPairs(resourceId, keys);
        verify(searchBuilder).and(eq("resourceId"), any(), eq(SearchCriteria.Op.EQ));
        verify(searchBuilder).and(eq("name"), any(), eq(SearchCriteria.Op.IN));
        verify(searchBuilder).done();
        verify(searchCriteria).setParameters("resourceId", resourceId);
        verify(searchCriteria).setParameters("name", keys.toArray());
        verify(testDetailsDao).search(searchCriteria, null);
        assertTrue(result.isEmpty());
    }

    protected static class TestDetailsDao extends ResourceDetailsDaoBase<TestDetailVO> {
        @Override
        public void addDetail(long resourceId, String key, String value, boolean display) {
            super.addDetail(new TestDetailVO(resourceId, key, value, display));
        }
    }

    @Entity
    @Table(name = "test_details")
    protected static class TestDetailVO implements ResourceDetail {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id")
        private long id;

        @Column(name = "resource_id")
        private long resourceId;

        @Column(name = "name")
        private String name;

        @Column(name = "value")
        private String value;

        @Column(name = "display")
        private boolean display = true;

        public TestDetailVO() {
        }

        public TestDetailVO(long resourceId, String name, String value, boolean display) {
            this.resourceId = resourceId;
            this.name = name;
            this.value = value;
            this.display = display;
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public long getResourceId() {
            return resourceId;
        }

        @Override
        public boolean isDisplay() {
            return display;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
