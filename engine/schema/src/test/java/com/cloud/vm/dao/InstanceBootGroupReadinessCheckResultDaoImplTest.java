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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupReadinessCheckResultVO;
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
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class InstanceBootGroupReadinessCheckResultDaoImplTest {

    @Spy
    InstanceBootGroupReadinessCheckResultDaoImpl instanceBootGroupReadinessCheckResultDaoImplSpy;

    private static final long RULE_ID = 21L;
    private static final long VM_ID = 99L;

    private Map<String, Object> paramMap(SearchCriteria<?> sc) {
        Map<String, Object> map = new HashMap<>();
        for (Pair<Attribute, Object> pair : sc.getValues()) {
            map.put(pair.first().getColumnName(), pair.second());
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFindByRuleAndVm() {
        InstanceBootGroupReadinessCheckResultVO expected = Mockito.mock(InstanceBootGroupReadinessCheckResultVO.class);
        Mockito.doReturn(expected).when(instanceBootGroupReadinessCheckResultDaoImplSpy).findOneBy(Mockito.any(SearchCriteria.class));

        InstanceBootGroupReadinessCheckResultVO result = instanceBootGroupReadinessCheckResultDaoImplSpy.findByRuleAndVm(RULE_ID, VM_ID);

        Assert.assertEquals(expected, result);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        Mockito.verify(instanceBootGroupReadinessCheckResultDaoImplSpy).findOneBy(scCaptor.capture());
        Map<String, Object> params = paramMap(scCaptor.getValue());
        Assert.assertEquals(RULE_ID, params.get("rule_id"));
        Assert.assertEquals(VM_ID, params.get("vm_id"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpsertInsertsWhenNoExistingResult() {
        Mockito.doReturn(null).when(instanceBootGroupReadinessCheckResultDaoImplSpy).findOneBy(Mockito.any(SearchCriteria.class));
        Mockito.doReturn(null).when(instanceBootGroupReadinessCheckResultDaoImplSpy).persist(Mockito.any(InstanceBootGroupReadinessCheckResultVO.class));

        Date checkedOn = new Date();
        instanceBootGroupReadinessCheckResultDaoImplSpy.upsert(RULE_ID, VM_ID, InstanceBootGroupReadinessRule.Status.Ready, "all good", checkedOn);

        ArgumentCaptor<InstanceBootGroupReadinessCheckResultVO> voCaptor = ArgumentCaptor.forClass(InstanceBootGroupReadinessCheckResultVO.class);
        Mockito.verify(instanceBootGroupReadinessCheckResultDaoImplSpy).persist(voCaptor.capture());
        Assert.assertEquals(RULE_ID, voCaptor.getValue().getRuleId());
        Assert.assertEquals(VM_ID, voCaptor.getValue().getVmId());
        Assert.assertEquals(InstanceBootGroupReadinessRule.Status.Ready, voCaptor.getValue().getStatus());
        Assert.assertEquals("all good", voCaptor.getValue().getMessage());
        Assert.assertEquals(checkedOn, voCaptor.getValue().getCheckedOn());
        Mockito.verify(instanceBootGroupReadinessCheckResultDaoImplSpy, Mockito.never()).update(Mockito.anyLong(), Mockito.any());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpsertUpdatesWhenExistingResult() {
        InstanceBootGroupReadinessCheckResultVO existing = new InstanceBootGroupReadinessCheckResultVO(
                RULE_ID, VM_ID, InstanceBootGroupReadinessRule.Status.Unknown, "old", new Date(0));
        Mockito.doReturn(existing).when(instanceBootGroupReadinessCheckResultDaoImplSpy).findOneBy(Mockito.any(SearchCriteria.class));
        Mockito.doReturn(true).when(instanceBootGroupReadinessCheckResultDaoImplSpy).update(Mockito.anyLong(), Mockito.any());

        Date checkedOn = new Date();
        instanceBootGroupReadinessCheckResultDaoImplSpy.upsert(RULE_ID, VM_ID, InstanceBootGroupReadinessRule.Status.NotReady, "still booting", checkedOn);

        Assert.assertEquals(InstanceBootGroupReadinessRule.Status.NotReady, existing.getStatus());
        Assert.assertEquals("still booting", existing.getMessage());
        Assert.assertEquals(checkedOn, existing.getCheckedOn());
        Mockito.verify(instanceBootGroupReadinessCheckResultDaoImplSpy).update(existing.getId(), existing);
        Mockito.verify(instanceBootGroupReadinessCheckResultDaoImplSpy, Mockito.never()).persist(Mockito.any(InstanceBootGroupReadinessCheckResultVO.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteByRuleId() {
        Mockito.doReturn(1).when(instanceBootGroupReadinessCheckResultDaoImplSpy).expunge(Mockito.any(SearchCriteria.class));

        instanceBootGroupReadinessCheckResultDaoImplSpy.deleteByRuleId(RULE_ID);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        Mockito.verify(instanceBootGroupReadinessCheckResultDaoImplSpy).expunge(scCaptor.capture());
        Map<String, Object> params = paramMap(scCaptor.getValue());
        Assert.assertEquals(RULE_ID, params.get("rule_id"));
    }
}
