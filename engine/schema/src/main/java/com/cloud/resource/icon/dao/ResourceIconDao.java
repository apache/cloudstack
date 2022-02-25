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
package com.cloud.resource.icon.dao;

import com.cloud.resource.icon.ResourceIconVO;
import com.cloud.server.ResourceIcon;
import com.cloud.server.ResourceTag;
import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.api.response.ResourceIconResponse;

import java.util.List;

public interface ResourceIconDao extends GenericDao<ResourceIconVO, Long> {
    ResourceIconResponse newResourceIconResponse(ResourceIcon resourceIconVO);
    ResourceIconVO findByResourceUuid(String resourceUuid, ResourceTag.ResourceObjectType resourceType);
    List<ResourceIconResponse> listResourceIcons(List<String> resourceUuids, ResourceTag.ResourceObjectType resourceType);
}
