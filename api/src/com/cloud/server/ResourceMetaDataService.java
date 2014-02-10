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

import org.apache.cloudstack.api.ResourceDetail;

import com.cloud.server.ResourceTag.ResourceObjectType;

public interface ResourceMetaDataService {

    /**
     * @param resourceId TODO
     * @param resourceType
     * @param details
     * @return
     */
    boolean addResourceMetaData(String resourceId, ResourceObjectType resourceType, Map<String, String> details);

    /**
     *
     * @param resourceId
     * @param resourceType
     * @param key
     * @return
     */
    public boolean deleteResourceMetaData(String resourceId, ResourceObjectType resourceType, String key);

    ResourceDetail getDetail(long resourceId, ResourceObjectType resourceType, String key);

    Map<String, String> getDetailsMap(long resourceId, ResourceObjectType resourceType, Boolean forDisplay);

    List<? extends ResourceDetail> getDetailsList(long resourceId, ResourceObjectType resourceType, Boolean forDisplay);

}
