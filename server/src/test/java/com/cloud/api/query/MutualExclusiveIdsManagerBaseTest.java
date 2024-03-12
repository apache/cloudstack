//
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
//
package com.cloud.api.query;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.db.SearchCriteria;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.never;

@RunWith(MockitoJUnitRunner.class)
public class MutualExclusiveIdsManagerBaseTest {

    @Mock
    SearchCriteria<String> sc;

    private static Long id1 = 1L;
    private static Long id2 = 2L;

    private List<Long> idsList;
    private List<Long> idsEmptyList;
    private List<Long> expectedListId;
    private List<Long> expectedListIds;

    private MutualExclusiveIdsManagerBase mgr = new MutualExclusiveIdsManagerBase();

    @Before
    public void setup() {
        idsList = Arrays.asList(id1, id2);
        idsEmptyList = Arrays.asList();
        expectedListId = Arrays.asList(id1);
        expectedListIds = Arrays.asList(id1, id2);
    }

    @Test
    public void testSetIdsListToSearchCriteria(){
        mgr.setIdsListToSearchCriteria(sc, idsList);
        Mockito.verify(sc).setParameters(Mockito.same("idIN"), Mockito.same(id1), Mockito.same(id2));
    }

    @Test
    public void testSetIdsListToSearchCriteriaEmptyList(){
        mgr.setIdsListToSearchCriteria(sc, idsEmptyList);
        Mockito.verify(sc, never()).setParameters(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void testGetIdsListId(){
        List<Long> result = mgr.getIdsListFromCmd(id1, idsEmptyList);
        assertEquals(expectedListId, result);
    }

    @Test
    public void testGetIdsListProvideList(){
        List<Long> result = mgr.getIdsListFromCmd(null, idsList);
        assertEquals(expectedListIds, result);
    }

    @Test(expected=InvalidParameterValueException.class)
    public void testGetIdsListBothNotNull(){
        mgr.getIdsListFromCmd(id1, idsList);
    }

    @Test
    public void testGetIdsListBothNull(){
        List<Long> result = mgr.getIdsListFromCmd(null, null);
        assertNull(result);
    }

    @Test
    public void testGetIdsEmptyListIdNull(){
        List<Long> result = mgr.getIdsListFromCmd(null, idsEmptyList);
        assertEquals(idsEmptyList, result);
    }
}
