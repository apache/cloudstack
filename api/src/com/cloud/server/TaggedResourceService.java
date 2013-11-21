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
// under the License.package com.cloud.server;

package com.cloud.server;

import java.util.List;
import java.util.Map;

import com.cloud.server.ResourceTag.ResourceObjectType;

public interface TaggedResourceService {

    /**
     * @param resourceIds TODO
     * @param resourceType
     * @param tags
     * @param customer TODO
     * @return
     */
    List<ResourceTag> createTags(List<String> resourceIds, ResourceObjectType resourceType, Map<String, String> tags, String customer);

    /**
     * @param resourceIds
     * @param resourceType
     * @param tags
     * @return
     */
    boolean deleteTags(List<String> resourceIds, ResourceObjectType resourceType, Map<String, String> tags);

    List<? extends ResourceTag> listByResourceTypeAndId(ResourceObjectType type, long resourceId);

    //FIXME - the methods below should be extracted to its separate manager/service responsible just for retrieving object details
    ResourceObjectType getResourceType(String resourceTypeStr);

    /**
     * @param resourceId
     * @param resourceType
     * @return
     */
    String getUuid(String resourceId, ResourceObjectType resourceType);

    public long getResourceId(String resourceId, ResourceObjectType resourceType);
}
