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
package com.cloud.storage.dao;

import java.util.List;

import org.apache.cloudstack.api.response.StorageTagResponse;

import com.cloud.storage.StoragePoolTagVO;
import com.cloud.utils.db.GenericDao;

public interface StoragePoolTagsDao extends GenericDao<StoragePoolTagVO, Long> {

    void persist(long poolId, List<String> storagePoolTags, Boolean isTagARule);

    void persist(List<StoragePoolTagVO> storagePoolTags);
    List<String> getStoragePoolTags(long poolId);
    void deleteTags(long poolId);
    List<StoragePoolTagVO> searchByIds(Long... stIds);
    StorageTagResponse newStorageTagResponse(StoragePoolTagVO tag);

    List<StoragePoolTagVO> findStoragePoolTags(long poolId);
    List<Long> listPoolIdsByTag(String tag);

}
