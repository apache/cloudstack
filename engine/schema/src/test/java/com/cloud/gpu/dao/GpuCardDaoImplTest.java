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

import com.cloud.gpu.GpuCardVO;
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
public class GpuCardDaoImplTest {

    @Spy
    @InjectMocks
    GpuCardDaoImpl gpuCardDaoImpl = new GpuCardDaoImpl();

    @Test
    public void findByVendorIdAndDeviceId() {
        doReturn(mock(GpuCardVO.class)).when(gpuCardDaoImpl).findOneBy(any(SearchCriteria.class));

        GpuCardVO gpuCard = gpuCardDaoImpl.findByVendorIdAndDeviceId("0d1a", "1a3b");
        Assert.assertNotNull("Expected non-null gpu card", gpuCard);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(gpuCardDaoImpl).findOneBy(scCaptor.capture());
        Assert.assertEquals("Expected correct where clause",
                "gpu_card.vendor_id = ?  AND gpu_card.device_id = ?",
                scCaptor.getValue().getWhereClause().trim());
    }
}
