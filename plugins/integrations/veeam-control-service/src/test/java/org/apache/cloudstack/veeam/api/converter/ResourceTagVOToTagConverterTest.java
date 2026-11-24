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

package org.apache.cloudstack.veeam.api.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.cloudstack.veeam.api.dto.BaseDto;
import org.apache.cloudstack.veeam.api.dto.Tag;
import org.junit.Test;

import com.cloud.server.ResourceTag;
import com.cloud.tags.ResourceTagVO;

public class ResourceTagVOToTagConverterTest {

    @Test
    public void testGetRootTagAndRootRef() {
        final Tag root = ResourceTagVOToTagConverter.getRootTag();

        assertEquals(BaseDto.ZERO_UUID, root.getId());
        assertEquals("root", root.getName());
        assertNotNull(ResourceTagVOToTagConverter.getRootTagRef().getHref());
    }

    @Test
    public void testToTag_FromResourceTagVoWithVmReference() {
        final ResourceTagVO vo = mock(ResourceTagVO.class);
        when(vo.getKey()).thenReturn("env");
        when(vo.getValue()).thenReturn("prod");
        when(vo.getResourceType()).thenReturn(ResourceTag.ResourceObjectType.UserVm);
        when(vo.getResourceUuid()).thenReturn("vm-1");

        final Tag tag = ResourceTagVOToTagConverter.toTag(vo);

        assertEquals("prod", tag.getId());
        assertEquals("prod", tag.getName());
        assertNotNull(tag.getParent());
        assertNotNull(tag.getVm());
        assertEquals("vm-1", tag.getVm().getId());
    }

    @Test
    public void testToTag_FromResourceTagVoWithoutVmReference() {
        final ResourceTagVO vo = mock(ResourceTagVO.class);
        when(vo.getKey()).thenReturn("scope");
        when(vo.getValue()).thenReturn("global");
        when(vo.getResourceType()).thenReturn(ResourceTag.ResourceObjectType.Volume);

        final Tag tag = ResourceTagVOToTagConverter.toTag(vo);

        assertEquals("global", tag.getId());
        assertNull(tag.getVm());
    }

    @Test
    public void testToTagsAndToTagsFromValues() {
        final ResourceTagVO vo = mock(ResourceTagVO.class);
        when(vo.getKey()).thenReturn("team");
        when(vo.getValue()).thenReturn("ops");
        when(vo.getResourceType()).thenReturn(ResourceTag.ResourceObjectType.UserVm);
        when(vo.getResourceUuid()).thenReturn("vm-2");

        assertEquals(1, ResourceTagVOToTagConverter.toTags(List.of(vo)).size());
        assertEquals("ops", ResourceTagVOToTagConverter.toTagsFromValues(List.of("ops")).get(0).getId());
    }
}
