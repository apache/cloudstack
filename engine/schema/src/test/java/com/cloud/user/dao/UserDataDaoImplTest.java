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
package com.cloud.user.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.user.UserDataVO;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class UserDataDaoImplTest {

    @Spy
    @InjectMocks
    UserDataDaoImpl userDataDaoImpl;

    @Test
    public void listIdsByAccountId_ReturnsEmptyListWhenNoIdsFound() {
        long accountId = 1L;

        GenericSearchBuilder<UserDataVO, Long> sb = mock(GenericSearchBuilder.class);
        when(sb.entity()).thenReturn(mock(UserDataVO.class));
        SearchCriteria<Long> sc = mock(SearchCriteria.class);
        doReturn(sb).when(userDataDaoImpl).createSearchBuilder(Long.class);
        doReturn(sc).when(sb).create();
        doReturn(Collections.emptyList()).when(userDataDaoImpl).customSearch(sc, null);

        List<Long> result = userDataDaoImpl.listIdsByAccountId(accountId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void listIdsByAccountId_ReturnsListOfIdsWhenFound() {
        long accountId = 1L;

        GenericSearchBuilder<UserDataVO, Long> sb = mock(GenericSearchBuilder.class);
        when(sb.entity()).thenReturn(mock(UserDataVO.class));
        SearchCriteria<Long> sc = mock(SearchCriteria.class);
        doReturn(sb).when(userDataDaoImpl).createSearchBuilder(Long.class);
        doReturn(sc).when(sb).create();
        doReturn(Arrays.asList(10L, 20L)).when(userDataDaoImpl).customSearch(sc, null);

        List<Long> result = userDataDaoImpl.listIdsByAccountId(accountId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(10L));
        assertTrue(result.contains(20L));
    }
}
