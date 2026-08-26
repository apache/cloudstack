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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupDetailsVO;
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
public class InstanceBootGroupDetailsDaoImplTest {

    @Spy
    InstanceBootGroupDetailsDaoImpl instanceBootGroupDetailsDaoImplSpy;

    private static final long BOOT_GROUP_ID = 7L;
    private static final String NAME = "readiness.timeout";
    private static final String VALUE = "300";

    private Map<String, Object> paramMap(SearchCriteria<?> sc) {
        Map<String, Object> map = new HashMap<>();
        for (Pair<Attribute, Object> pair : sc.getValues()) {
            map.put(pair.first().getColumnName(), pair.second());
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetDetailWhenExists() {
        InstanceBootGroupDetailsVO detail = Mockito.mock(InstanceBootGroupDetailsVO.class);
        Mockito.when(detail.getValue()).thenReturn(VALUE);
        Mockito.doReturn(detail).when(instanceBootGroupDetailsDaoImplSpy).findOneBy(Mockito.any(SearchCriteria.class));

        String result = instanceBootGroupDetailsDaoImplSpy.getDetail(BOOT_GROUP_ID, NAME);

        Assert.assertEquals(VALUE, result);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        Mockito.verify(instanceBootGroupDetailsDaoImplSpy).findOneBy(scCaptor.capture());
        Map<String, Object> params = paramMap(scCaptor.getValue());
        Assert.assertEquals(BOOT_GROUP_ID, params.get("boot_group_id"));
        Assert.assertEquals(NAME, params.get("name"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetDetailWhenNotExists() {
        Mockito.doReturn(null).when(instanceBootGroupDetailsDaoImplSpy).findOneBy(Mockito.any(SearchCriteria.class));

        String result = instanceBootGroupDetailsDaoImplSpy.getDetail(BOOT_GROUP_ID, NAME);

        Assert.assertNull(result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetDetailInsertsWhenNotExisting() {
        Mockito.doReturn(null).when(instanceBootGroupDetailsDaoImplSpy).findOneBy(Mockito.any(SearchCriteria.class));
        Mockito.doReturn(null).when(instanceBootGroupDetailsDaoImplSpy).persist(Mockito.any(InstanceBootGroupDetailsVO.class));

        instanceBootGroupDetailsDaoImplSpy.setDetail(BOOT_GROUP_ID, NAME, VALUE);

        ArgumentCaptor<InstanceBootGroupDetailsVO> voCaptor = ArgumentCaptor.forClass(InstanceBootGroupDetailsVO.class);
        Mockito.verify(instanceBootGroupDetailsDaoImplSpy).persist(voCaptor.capture());
        Assert.assertEquals(BOOT_GROUP_ID, voCaptor.getValue().getBootGroupId());
        Assert.assertEquals(NAME, voCaptor.getValue().getName());
        Assert.assertEquals(VALUE, voCaptor.getValue().getValue());
        Mockito.verify(instanceBootGroupDetailsDaoImplSpy, Mockito.never()).update(Mockito.anyLong(), Mockito.any());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetDetailUpdatesWhenExisting() {
        InstanceBootGroupDetailsVO existing = new InstanceBootGroupDetailsVO(BOOT_GROUP_ID, NAME, "old-value");
        Mockito.doReturn(existing).when(instanceBootGroupDetailsDaoImplSpy).findOneBy(Mockito.any(SearchCriteria.class));
        Mockito.doReturn(true).when(instanceBootGroupDetailsDaoImplSpy).update(Mockito.anyLong(), Mockito.any());

        instanceBootGroupDetailsDaoImplSpy.setDetail(BOOT_GROUP_ID, NAME, VALUE);

        Assert.assertEquals(VALUE, existing.getValue());
        Mockito.verify(instanceBootGroupDetailsDaoImplSpy).update(existing.getId(), existing);
        Mockito.verify(instanceBootGroupDetailsDaoImplSpy, Mockito.never()).persist(Mockito.any(InstanceBootGroupDetailsVO.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testListDetails() {
        List<InstanceBootGroupDetailsVO> details = new ArrayList<>();
        details.add(new InstanceBootGroupDetailsVO(BOOT_GROUP_ID, "key1", "value1"));
        details.add(new InstanceBootGroupDetailsVO(BOOT_GROUP_ID, "key2", "value2"));
        Mockito.doReturn(details).when(instanceBootGroupDetailsDaoImplSpy).listBy(Mockito.any(SearchCriteria.class));

        Map<String, String> result = instanceBootGroupDetailsDaoImplSpy.listDetails(BOOT_GROUP_ID);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("value1", result.get("key1"));
        Assert.assertEquals("value2", result.get("key2"));

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        Mockito.verify(instanceBootGroupDetailsDaoImplSpy).listBy(scCaptor.capture());
        Map<String, Object> params = paramMap(scCaptor.getValue());
        Assert.assertEquals(BOOT_GROUP_ID, params.get("boot_group_id"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testListDetailsEmpty() {
        Mockito.doReturn(new ArrayList<>()).when(instanceBootGroupDetailsDaoImplSpy).listBy(Mockito.any(SearchCriteria.class));

        Map<String, String> result = instanceBootGroupDetailsDaoImplSpy.listDetails(BOOT_GROUP_ID);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }
}
