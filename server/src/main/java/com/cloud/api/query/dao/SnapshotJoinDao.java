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

import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.SnapshotResponse;

import com.cloud.api.query.vo.SnapshotJoinVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.SearchCriteria;

public interface SnapshotJoinDao extends GenericDao<SnapshotJoinVO, Long> {

    SnapshotResponse newSnapshotResponse(ResponseObject.ResponseView view, boolean isShowUnique, SnapshotJoinVO snapshotJoinVO);

    SnapshotResponse setSnapshotResponse(SnapshotResponse snapshotResponse, SnapshotJoinVO snapshot);

    Pair<List<SnapshotJoinVO>, Integer> searchIncludingRemovedAndCount(final SearchCriteria<SnapshotJoinVO> sc, final Filter filter);

    List<SnapshotJoinVO> searchBySnapshotStorePair(String... pairs);
    List<SnapshotJoinVO> findByDistinctIds(Long zoneId, Long... ids);
}
