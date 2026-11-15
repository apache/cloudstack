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
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupMemberVO;
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
public class InstanceBootGroupMemberDaoImplTest {

    @Spy
    InstanceBootGroupMemberDaoImpl instanceBootGroupMemberDaoImplSpy;

    private static final long BOOT_GROUP_ID = 5L;
    private static final long MEMBER_ID = 42L;

    private Map<String, Object> paramMap(SearchCriteria<?> sc) {
        Map<String, Object> map = new HashMap<>();
        for (Pair<Attribute, Object> pair : sc.getValues()) {
            map.put(pair.first().getColumnName(), pair.second());
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testListByBootGroupId() {
        List<InstanceBootGroupMemberVO> expected = List.of(Mockito.mock(InstanceBootGroupMemberVO.class));
        Mockito.doReturn(expected).when(instanceBootGroupMemberDaoImplSpy).listBy(Mockito.any(SearchCriteria.class), Mockito.isNull());

        List<InstanceBootGroupMemberVO> result = instanceBootGroupMemberDaoImplSpy.listByBootGroupId(BOOT_GROUP_ID);

        Assert.assertEquals(expected, result);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        Mockito.verify(instanceBootGroupMemberDaoImplSpy).listBy(scCaptor.capture(), Mockito.isNull());
        Map<String, Object> params = paramMap(scCaptor.getValue());
        Assert.assertEquals(BOOT_GROUP_ID, params.get("boot_group_id"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSearchAndCountByBootGroupId() {
        Pair<List<InstanceBootGroupMemberVO>, Integer> expected = new Pair<>(new ArrayList<>(), 0);
        Mockito.doReturn(expected).when(instanceBootGroupMemberDaoImplSpy).searchAndCount(Mockito.any(SearchCriteria.class), Mockito.isNull());

        Pair<List<InstanceBootGroupMemberVO>, Integer> result = instanceBootGroupMemberDaoImplSpy.searchAndCountByBootGroupId(BOOT_GROUP_ID);

        Assert.assertEquals(expected, result);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        Mockito.verify(instanceBootGroupMemberDaoImplSpy).searchAndCount(scCaptor.capture(), Mockito.isNull());
        Map<String, Object> params = paramMap(scCaptor.getValue());
        Assert.assertEquals(BOOT_GROUP_ID, params.get("boot_group_id"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSearchAndCountByBootGroupIdAndType() {
        Pair<List<InstanceBootGroupMemberVO>, Integer> expected = new Pair<>(new ArrayList<>(), 0);
        Mockito.doReturn(expected).when(instanceBootGroupMemberDaoImplSpy).searchAndCount(Mockito.any(SearchCriteria.class), Mockito.isNull());

        Pair<List<InstanceBootGroupMemberVO>, Integer> result = instanceBootGroupMemberDaoImplSpy
                .searchAndCountByBootGroupIdAndType(BOOT_GROUP_ID, InstanceBootGroupMember.MemberType.VirtualMachine);

        Assert.assertEquals(expected, result);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        Mockito.verify(instanceBootGroupMemberDaoImplSpy).searchAndCount(scCaptor.capture(), Mockito.isNull());
        Map<String, Object> params = paramMap(scCaptor.getValue());
        Assert.assertEquals(BOOT_GROUP_ID, params.get("boot_group_id"));
        Assert.assertEquals(InstanceBootGroupMember.MemberType.VirtualMachine, params.get("member_type"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFindByMember() {
        InstanceBootGroupMemberVO expected = Mockito.mock(InstanceBootGroupMemberVO.class);
        Mockito.doReturn(expected).when(instanceBootGroupMemberDaoImplSpy).findOneBy(Mockito.any(SearchCriteria.class));

        InstanceBootGroupMemberVO result = instanceBootGroupMemberDaoImplSpy.findByMember(InstanceBootGroupMember.MemberType.InstanceGroup, MEMBER_ID);

        Assert.assertEquals(expected, result);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        Mockito.verify(instanceBootGroupMemberDaoImplSpy).findOneBy(scCaptor.capture());
        Map<String, Object> params = paramMap(scCaptor.getValue());
        Assert.assertEquals(InstanceBootGroupMember.MemberType.InstanceGroup, params.get("member_type"));
        Assert.assertEquals(MEMBER_ID, params.get("member_id"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFindByMemberNotFound() {
        Mockito.doReturn(null).when(instanceBootGroupMemberDaoImplSpy).findOneBy(Mockito.any(SearchCriteria.class));

        InstanceBootGroupMemberVO result = instanceBootGroupMemberDaoImplSpy.findByMember(InstanceBootGroupMember.MemberType.VirtualMachine, MEMBER_ID);

        Assert.assertNull(result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteByBootGroupId() {
        Mockito.doReturn(1).when(instanceBootGroupMemberDaoImplSpy).expunge(Mockito.any(SearchCriteria.class));

        instanceBootGroupMemberDaoImplSpy.deleteByBootGroupId(BOOT_GROUP_ID);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        Mockito.verify(instanceBootGroupMemberDaoImplSpy).expunge(scCaptor.capture());
        Map<String, Object> params = paramMap(scCaptor.getValue());
        Assert.assertEquals(BOOT_GROUP_ID, params.get("boot_group_id"));
    }
}
