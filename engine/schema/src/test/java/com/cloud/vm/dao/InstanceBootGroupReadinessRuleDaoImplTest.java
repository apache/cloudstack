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

import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupMember;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupReadinessRuleVO;
import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRule;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class InstanceBootGroupReadinessRuleDaoImplTest {

    @Spy
    InstanceBootGroupReadinessRuleDaoImpl instanceBootGroupReadinessRuleDaoImplSpy;

    private static final long BOOT_GROUP_ID = 3L;
    private static final long ITEM_ID = 8L;

    private Map<String, Object> paramMap(SearchCriteria<?> sc) {
        Map<String, Object> map = new HashMap<>();
        for (Pair<Attribute, Object> pair : sc.getValues()) {
            map.put(pair.first().getColumnName(), pair.second());
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSearchAndCountByBootGroupIdWithAllFiltersSet() {
        Pair<List<InstanceBootGroupReadinessRuleVO>, Integer> expected = new Pair<>(new ArrayList<>(), 0);
        Mockito.doReturn(expected).when(instanceBootGroupReadinessRuleDaoImplSpy)
                .searchAndCount(Mockito.any(SearchCriteria.class), Mockito.any(Filter.class));

        Pair<List<InstanceBootGroupReadinessRuleVO>, Integer> result = instanceBootGroupReadinessRuleDaoImplSpy.searchAndCountByBootGroupId(
                BOOT_GROUP_ID, 1L, InstanceBootGroupMember.MemberType.VirtualMachine, ITEM_ID,
                InstanceBootGroupReadinessRule.RuleType.Ping, "healthcheck", 0L, 10L);

        Assert.assertEquals(expected, result);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        ArgumentCaptor<Filter> filterCaptor = ArgumentCaptor.forClass(Filter.class);
        Mockito.verify(instanceBootGroupReadinessRuleDaoImplSpy).searchAndCount(scCaptor.capture(), filterCaptor.capture());

        Map<String, Object> params = paramMap(scCaptor.getValue());
        Assert.assertEquals(BOOT_GROUP_ID, params.get("boot_group_id"));
        Assert.assertEquals(1L, params.get("id"));
        Assert.assertEquals(InstanceBootGroupMember.MemberType.VirtualMachine, params.get("item_type"));
        Assert.assertEquals(ITEM_ID, params.get("item_id"));
        Assert.assertEquals(InstanceBootGroupReadinessRule.RuleType.Ping, params.get("rule_type"));
        Assert.assertEquals("%healthcheck%", params.get("name"));
        Assert.assertNotNull(filterCaptor.getValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSearchAndCountByBootGroupIdWithOnlyMandatoryParams() {
        Pair<List<InstanceBootGroupReadinessRuleVO>, Integer> expected = new Pair<>(new ArrayList<>(), 0);
        Mockito.doReturn(expected).when(instanceBootGroupReadinessRuleDaoImplSpy)
                .searchAndCount(Mockito.any(SearchCriteria.class), Mockito.any(Filter.class));

        Pair<List<InstanceBootGroupReadinessRuleVO>, Integer> result = instanceBootGroupReadinessRuleDaoImplSpy.searchAndCountByBootGroupId(
                BOOT_GROUP_ID, null, null, null, null, null, null, null);

        Assert.assertEquals(expected, result);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        Mockito.verify(instanceBootGroupReadinessRuleDaoImplSpy).searchAndCount(scCaptor.capture(), Mockito.any(Filter.class));

        Map<String, Object> params = paramMap(scCaptor.getValue());
        Assert.assertEquals(BOOT_GROUP_ID, params.get("boot_group_id"));
        Assert.assertFalse(params.containsKey("id"));
        Assert.assertFalse(params.containsKey("item_type"));
        Assert.assertFalse(params.containsKey("item_id"));
        Assert.assertFalse(params.containsKey("rule_type"));
        Assert.assertFalse(params.containsKey("name"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testListEnabledByItem() {
        List<InstanceBootGroupReadinessRuleVO> expected = List.of(Mockito.mock(InstanceBootGroupReadinessRuleVO.class));
        Mockito.doReturn(expected).when(instanceBootGroupReadinessRuleDaoImplSpy).listBy(Mockito.any(SearchCriteria.class));

        List<InstanceBootGroupReadinessRuleVO> result = instanceBootGroupReadinessRuleDaoImplSpy.listEnabledByItem(
                BOOT_GROUP_ID, InstanceBootGroupMember.MemberType.InstanceGroup, ITEM_ID);

        Assert.assertEquals(expected, result);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        Mockito.verify(instanceBootGroupReadinessRuleDaoImplSpy).listBy(scCaptor.capture());
        Map<String, Object> params = paramMap(scCaptor.getValue());
        Assert.assertEquals(BOOT_GROUP_ID, params.get("boot_group_id"));
        Assert.assertEquals(InstanceBootGroupMember.MemberType.InstanceGroup, params.get("item_type"));
        Assert.assertEquals(ITEM_ID, params.get("item_id"));
        Assert.assertEquals(Boolean.TRUE, params.get("enabled"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testListByItem() {
        List<InstanceBootGroupReadinessRuleVO> expected = List.of(Mockito.mock(InstanceBootGroupReadinessRuleVO.class));
        Mockito.doReturn(expected).when(instanceBootGroupReadinessRuleDaoImplSpy).listBy(Mockito.any(SearchCriteria.class));

        List<InstanceBootGroupReadinessRuleVO> result = instanceBootGroupReadinessRuleDaoImplSpy.listByItem(
                BOOT_GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine, ITEM_ID);

        Assert.assertEquals(expected, result);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        Mockito.verify(instanceBootGroupReadinessRuleDaoImplSpy).listBy(scCaptor.capture());
        Map<String, Object> params = paramMap(scCaptor.getValue());
        Assert.assertEquals(BOOT_GROUP_ID, params.get("boot_group_id"));
        Assert.assertEquals(InstanceBootGroupMember.MemberType.VirtualMachine, params.get("item_type"));
        Assert.assertEquals(ITEM_ID, params.get("item_id"));
        Assert.assertFalse(params.containsKey("enabled"));
    }
}
