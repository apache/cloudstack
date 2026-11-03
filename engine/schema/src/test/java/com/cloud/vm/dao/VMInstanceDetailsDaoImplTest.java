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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.VMInstanceDetailVO;

@RunWith(MockitoJUnitRunner.class)
public class VMInstanceDetailsDaoImplTest {
    @Spy
    @InjectMocks
    private VMInstanceDetailsDaoImpl vmInstanceDetailsDaoImpl;

    @Test
    public void removeDetailsWithPrefixReturnsZeroWhenPrefixIsBlank() {
        Assert.assertEquals(0, vmInstanceDetailsDaoImpl.removeDetailsWithPrefix(1L, ""));
        Assert.assertEquals(0, vmInstanceDetailsDaoImpl.removeDetailsWithPrefix(1L, "   "));
        Assert.assertEquals(0, vmInstanceDetailsDaoImpl.removeDetailsWithPrefix(1L, null));
    }

    @Test
    public void removeDetailsWithPrefixRemovesMatchingDetails() {
        SearchBuilder<VMInstanceDetailVO> sb = mock(SearchBuilder.class);
        VMInstanceDetailVO entity = mock(VMInstanceDetailVO.class);
        when(sb.entity()).thenReturn(entity);
        when(sb.and(anyString(), any(), any(SearchCriteria.Op.class))).thenReturn(sb);
        SearchCriteria<VMInstanceDetailVO> sc = mock(SearchCriteria.class);
        when(sb.create()).thenReturn(sc);
        when(vmInstanceDetailsDaoImpl.createSearchBuilder()).thenReturn(sb);
        doReturn(3).when(vmInstanceDetailsDaoImpl).remove(sc);
        int removedCount = vmInstanceDetailsDaoImpl.removeDetailsWithPrefix(1L, "testPrefix");
        Assert.assertEquals(3, removedCount);
        Mockito.verify(sc).setParameters("vmId", 1L);
        Mockito.verify(sc).setParameters("prefix", "testPrefix%");
        Mockito.verify(vmInstanceDetailsDaoImpl, Mockito.times(1)).remove(sc);
    }

    @Test
    public void removeDetailsWithPrefixDoesNotRemoveWhenNoMatch() {
        SearchBuilder<VMInstanceDetailVO> sb = mock(SearchBuilder.class);
        VMInstanceDetailVO entity = mock(VMInstanceDetailVO.class);
        when(sb.entity()).thenReturn(entity);
        when(sb.and(anyString(), any(), any(SearchCriteria.Op.class))).thenReturn(sb);
        SearchCriteria<VMInstanceDetailVO> sc = mock(SearchCriteria.class);
        when(sb.create()).thenReturn(sc);
        when(vmInstanceDetailsDaoImpl.createSearchBuilder()).thenReturn(sb);
        doReturn(0).when(vmInstanceDetailsDaoImpl).remove(sc);

        int removedCount = vmInstanceDetailsDaoImpl.removeDetailsWithPrefix(1L, "nonExistentPrefix");

        Assert.assertEquals(0, removedCount);
        Mockito.verify(sc).setParameters("vmId", 1L);
        Mockito.verify(sc).setParameters("prefix", "nonExistentPrefix%");
        Mockito.verify(vmInstanceDetailsDaoImpl, Mockito.times(1)).remove(sc);
    }
}
