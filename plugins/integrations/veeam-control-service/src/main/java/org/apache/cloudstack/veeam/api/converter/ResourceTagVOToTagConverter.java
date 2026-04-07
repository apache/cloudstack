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

import java.util.List;
import java.util.stream.Collectors;

import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.api.TagsRouteHandler;
import org.apache.cloudstack.veeam.api.VmsRouteHandler;
import org.apache.cloudstack.veeam.api.dto.BaseDto;
import org.apache.cloudstack.veeam.api.dto.Ref;
import org.apache.cloudstack.veeam.api.dto.Tag;

import com.cloud.server.ResourceTag;
import com.cloud.tags.ResourceTagVO;

public class ResourceTagVOToTagConverter {

    public static Ref getRootTagRef() {
        String basePath = VeeamControlService.ContextPath.value();
        return Ref.of(basePath + TagsRouteHandler.BASE_ROUTE + "/" + BaseDto.ZERO_UUID, BaseDto.ZERO_UUID);
    }

    public static Tag getRootTag() {
        Tag tag = new Tag();
        tag.setId(BaseDto.ZERO_UUID);
        tag.setName("root");
        tag.setHref(getRootTagRef().getHref());
        return tag;
    }

    public static Tag toTag(ResourceTagVO vo) {
        String basePath = VeeamControlService.ContextPath.value();
        Tag tag = new Tag();
        tag.setId(vo.getUuid());
        tag.setName(String.format("%s-%s", vo.getKey(), vo.getValue()).replaceAll("\\s+", ""));
        tag.setDescription(String.format("Tag %s with value: %s", vo.getKey(), vo.getValue()));
        tag.setHref(basePath + TagsRouteHandler.BASE_ROUTE + "/" + vo.getUuid());
        if (ResourceTag.ResourceObjectType.UserVm.equals(vo.getResourceType())) {
            tag.setVm(Ref.of(basePath + VmsRouteHandler.BASE_ROUTE + "/" + vo.getResourceUuid(),
                    vo.getResourceUuid()));
        }
        tag.setParent(getRootTagRef());
        return tag;
    }

    public static List<Tag> toTags(List<ResourceTagVO> vos) {
        return vos.stream().map(ResourceTagVOToTagConverter::toTag).collect(Collectors.toList());
    }
}
