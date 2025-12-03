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
package com.cloud.agent.manager.allocator.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;

@RunWith(MockitoJUnitRunner.class)
public class RandomAllocatorTest {

    @Mock
    HostDao hostDao;
    @InjectMocks
    RandomAllocator randomAllocator;

    @Test
    public void testListHostsByTags() {
        Host.Type type = Host.Type.Routing;
        Long id = 1L;
        String templateTag = "tag1";
        String offeringTag = "tag2";
        HostVO host1 = Mockito.mock(HostVO.class);
        HostVO host2 = Mockito.mock(HostVO.class);
        Mockito.when(hostDao.listByHostTag(type, id, id, id, offeringTag)).thenReturn(List.of(host1, host2));

        // No template tagged host
        Mockito.when(hostDao.listByHostTag(type, id, id, id, templateTag)).thenReturn(new ArrayList<>());
        List<HostVO> result = randomAllocator.listHostsByTags(type, id, id, id, offeringTag, templateTag);
        Assert.assertTrue(CollectionUtils.isEmpty(result));

        // Different template tagged host
        HostVO host3 = Mockito.mock(HostVO.class);
        Mockito.when(hostDao.listByHostTag(type, id, id, id, templateTag)).thenReturn(List.of(host3));
        result = randomAllocator.listHostsByTags(type, id, id, id, offeringTag, templateTag);
        Assert.assertTrue(CollectionUtils.isEmpty(result));

        // Matching template tagged host
        Mockito.when(hostDao.listByHostTag(type, id, id, id, templateTag)).thenReturn(List.of(host1));
        result = randomAllocator.listHostsByTags(type, id, id, id, offeringTag, templateTag);
        Assert.assertFalse(CollectionUtils.isEmpty(result));
        Assert.assertEquals(1, result.size());

        // No template tag
        result = randomAllocator.listHostsByTags(type, id, id, id, offeringTag, null);
        Assert.assertFalse(CollectionUtils.isEmpty(result));
        Assert.assertEquals(2, result.size());

        // No offering tag
        result = randomAllocator.listHostsByTags(type, id, id, id, null, templateTag);
        Assert.assertFalse(CollectionUtils.isEmpty(result));
        Assert.assertEquals(1, result.size());
    }
}
