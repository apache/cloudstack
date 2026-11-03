/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.gpu.dao;

import com.cloud.gpu.VgpuProfileVO;
import com.cloud.utils.db.SearchCriteria;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class VgpuProfileDaoImplTest {

    @Spy
    @InjectMocks
    VgpuProfileDaoImpl vgpuProfileDaoImpl = new VgpuProfileDaoImpl();

    @Test
    public void findByNameAndCardId() {
        doReturn(mock(VgpuProfileVO.class)).when(vgpuProfileDaoImpl).findOneBy(any(SearchCriteria.class));

        VgpuProfileVO vgpuProfile = vgpuProfileDaoImpl.findByNameAndCardId("test-profile", 1L);
        Assert.assertNotNull("Expected non-null vgpu profile", vgpuProfile);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(vgpuProfileDaoImpl).findOneBy(scCaptor.capture());
        Assert.assertEquals("Expected correct where clause",
                "vgpu_profile.name = ?  AND vgpu_profile.card_id=?",
                scCaptor.getValue().getWhereClause().trim());
    }

    @Test
    public void removeByCardId() {
        doReturn(1).when(vgpuProfileDaoImpl).remove(any(SearchCriteria.class));

        int removed = vgpuProfileDaoImpl.removeByCardId(123L);
        Assert.assertEquals("Expected one vgpu profile removed", 1, removed);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(vgpuProfileDaoImpl).remove(scCaptor.capture());
        Assert.assertEquals("Expected correct where clause", "vgpu_profile.card_id=?",
                scCaptor.getValue().getWhereClause().trim());
    }
}
