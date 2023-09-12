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
package com.cloud.api.query.dao;

import java.util.List;

import com.cloud.storage.ScopeType;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import org.apache.cloudstack.api.response.StoragePoolResponse;

import com.cloud.api.query.vo.StoragePoolJoinVO;
import com.cloud.storage.StoragePool;
import com.cloud.utils.db.GenericDao;

public interface StoragePoolJoinDao extends GenericDao<StoragePoolJoinVO, Long> {

    StoragePoolResponse newStoragePoolResponse(StoragePoolJoinVO host);

    StoragePoolResponse setStoragePoolResponse(StoragePoolResponse response, StoragePoolJoinVO host);

    StoragePoolResponse newStoragePoolForMigrationResponse(StoragePoolJoinVO host);

    StoragePoolResponse setStoragePoolForMigrationResponse(StoragePoolResponse response, StoragePoolJoinVO host);

    List<StoragePoolJoinVO> newStoragePoolView(StoragePool group);

    List<StoragePoolJoinVO> searchByIds(Long... spIds);

    Pair<List<StoragePoolJoinVO>, Integer> searchAndCount(Long storagePoolId, String storagePoolName, Long zoneId, String path, Long podId, Long clusterId, String address, ScopeType scopeType, StoragePoolStatus status, String keyword, Filter searchFilter);

}
