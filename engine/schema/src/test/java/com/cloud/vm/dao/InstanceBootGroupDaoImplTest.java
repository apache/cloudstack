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
package com.cloud.vm.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class InstanceBootGroupDaoImplTest {

    @Spy
    InstanceBootGroupDaoImpl instanceBootGroupDaoImplSpy;

    private static final long ACCOUNT_ID = 10L;
    private static final String NAME = "boot-group-1";

    private Map<String, Object> paramMap(SearchCriteria<?> sc) {
        Map<String, Object> map = new HashMap<>();
        for (Pair<Attribute, Object> pair : sc.getValues()) {
            map.put(pair.first().getColumnName(), pair.second());
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testListByAccountId() {
        List<InstanceBootGroupVO> expected = new ArrayList<>();
        expected.add(Mockito.mock(InstanceBootGroupVO.class));

        Mockito.doReturn(expected).when(instanceBootGroupDaoImplSpy).listBy(Mockito.any(SearchCriteria.class));

        List<InstanceBootGroupVO> result = instanceBootGroupDaoImplSpy.listByAccountId(ACCOUNT_ID);

        Assert.assertEquals(expected, result);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        Mockito.verify(instanceBootGroupDaoImplSpy).listBy(scCaptor.capture());
        Map<String, Object> params = paramMap(scCaptor.getValue());
        Assert.assertEquals(ACCOUNT_ID, params.get("account_id"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIsNameInUseTrueWhenResultsFound() {
        List<InstanceBootGroupVO> found = Collections.singletonList(Mockito.mock(InstanceBootGroupVO.class));
        Mockito.doReturn(found).when(instanceBootGroupDaoImplSpy).listBy(Mockito.any(SearchCriteria.class));

        boolean result = instanceBootGroupDaoImplSpy.isNameInUse(ACCOUNT_ID, NAME);

        Assert.assertTrue(result);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        Mockito.verify(instanceBootGroupDaoImplSpy).listBy(scCaptor.capture());
        Map<String, Object> params = paramMap(scCaptor.getValue());
        Assert.assertEquals(ACCOUNT_ID, params.get("account_id"));
        Assert.assertEquals(NAME, params.get("name"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIsNameInUseFalseWhenNoResults() {
        Mockito.doReturn(new ArrayList<>()).when(instanceBootGroupDaoImplSpy).listBy(Mockito.any(SearchCriteria.class));

        boolean result = instanceBootGroupDaoImplSpy.isNameInUse(ACCOUNT_ID, NAME);

        Assert.assertFalse(result);
    }
}
