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
package org.apache.cloudstack.engine.orchestration;

import java.util.ArrayList;

import org.apache.commons.lang3.ObjectUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.cloud.configuration.Resource;
import com.cloud.storage.VolumeVO;
import com.cloud.user.ResourceLimitService;

@RunWith(MockitoJUnitRunner.class)
public class VolumeOrchestratorTest {

    @Mock
    protected ResourceLimitService resourceLimitMgr;

    @Spy
    @InjectMocks
    private VolumeOrchestrator volumeOrchestrator = new VolumeOrchestrator();

    private static final Long DEFAULT_ACCOUNT_PS_RESOURCE_COUNT = 100L;
    private Long accountPSResourceCount;

    @Before
    public void setUp() throws Exception {
        accountPSResourceCount = DEFAULT_ACCOUNT_PS_RESOURCE_COUNT;
        Mockito.when(resourceLimitMgr.recalculateResourceCount(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyInt())).thenReturn(new ArrayList<>());
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Resource.ResourceType type = (Resource.ResourceType)invocation.getArguments()[1];
            Long increment = (Long)invocation.getArguments()[3];
            if (Resource.ResourceType.primary_storage.equals(type)) {
                accountPSResourceCount += increment;
            }
            return null;
        }).when(resourceLimitMgr).incrementResourceCount(Mockito.anyLong(), Mockito.any(Resource.ResourceType.class), Mockito.anyBoolean(), Mockito.anyLong());
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Resource.ResourceType type = (Resource.ResourceType)invocation.getArguments()[1];
            Long decrement = (Long)invocation.getArguments()[3];
            if (Resource.ResourceType.primary_storage.equals(type)) {
                accountPSResourceCount -= decrement;
            }
            return null;
        }).when(resourceLimitMgr).decrementResourceCount(Mockito.anyLong(), Mockito.any(Resource.ResourceType.class), Mockito.anyBoolean(), Mockito.anyLong());
    }

    private void runCheckAndUpdateVolumeAccountResourceCountTest(Long originalSize, Long newSize) {
        VolumeVO v1 = Mockito.mock(VolumeVO.class);
        Mockito.when(v1.getSize()).thenReturn(originalSize);
        VolumeVO v2 = Mockito.mock(VolumeVO.class);
        Mockito.when(v2.getSize()).thenReturn(newSize);
        volumeOrchestrator.checkAndUpdateVolumeAccountResourceCount(v1, v2);
        Long expected = ObjectUtils.anyNull(originalSize, newSize) ?
                DEFAULT_ACCOUNT_PS_RESOURCE_COUNT : DEFAULT_ACCOUNT_PS_RESOURCE_COUNT + (newSize - originalSize);
        Assert.assertEquals(expected, accountPSResourceCount);
    }

    @Test
    public void testCheckAndUpdateVolumeAccountResourceCountSameSize() {
        runCheckAndUpdateVolumeAccountResourceCountTest(10L, 10L);
    }

    @Test
    public void testCheckAndUpdateVolumeAccountResourceCountEitherSizeNull() {
        runCheckAndUpdateVolumeAccountResourceCountTest(null, 10L);
        runCheckAndUpdateVolumeAccountResourceCountTest(10L, null);
    }

    @Test
    public void testCheckAndUpdateVolumeAccountResourceCountMoreSize() {
        runCheckAndUpdateVolumeAccountResourceCountTest(10L, 20L);
    }

    @Test
    public void testCheckAndUpdateVolumeAccountResourceCountLessSize() {
        runCheckAndUpdateVolumeAccountResourceCountTest(20L, 10L);
    }
}
