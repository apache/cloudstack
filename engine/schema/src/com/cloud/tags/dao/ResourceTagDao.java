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

import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.tags.ResourceTagVO;
import com.cloud.utils.db.GenericDao;

public interface ResourceTagDao extends GenericDao<ResourceTagVO, Long> {

    /**
     * @param resourceId
     * @param resourceType
     * @return
     */
    boolean removeByIdAndType(long resourceId, ResourceObjectType resourceType);

    List<? extends ResourceTag> listBy(long resourceId, ResourceObjectType resourceType);

    void updateResourceId(long srcId, long destId, ResourceObjectType resourceType);
}
