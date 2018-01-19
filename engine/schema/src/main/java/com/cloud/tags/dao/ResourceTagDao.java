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
package com.cloud.tags.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.tags.ResourceTagVO;
import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.api.response.ResourceTagResponse;

public interface ResourceTagDao extends GenericDao<ResourceTagVO, Long> {

    /**
     * Remove a resourceTag based on the resourceId and type
     * @param resourceId the id of the resource you want to remove
     * @param resourceType the resource type
     * @return true if successful
     */
    boolean removeByIdAndType(long resourceId, ResourceObjectType resourceType);

    List<? extends ResourceTag> listBy(long resourceId, ResourceObjectType resourceType);

    /**
     * Find a resource tag based on the resource id, resource type and key
     * @param resourceId the id of the resource you want to find
     * @param resourceType the resource type (e.g. VPC)
     * @param key the key value
     * @return the ResourceTag matching the search criteria
     */
    ResourceTag findByKey(long resourceId, ResourceObjectType resourceType, String key);

    void updateResourceId(long srcId, long destId, ResourceObjectType resourceType);

    Map<String, Set<ResourceTagResponse>> listTags();

    /**
     * remove a resource tag based on the resource id, resource type and key
     * @param resourceId the id of the resource you want to remove
     * @param resourceType the resource type (e.g. VPC)
     * @param key the key value
     */
    void removeByResourceIdAndKey(long resourceId, ResourceObjectType resourceType, String key);

    List<? extends ResourceTag> listByResourceUuid(String resourceUuid);
}
