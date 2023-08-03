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
import org.apache.log4j.Logger;

import com.cloud.api.ApiResponseHelper;
import com.cloud.api.query.vo.SnapshotJoinVO;
import com.cloud.storage.Snapshot;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchCriteria;

public class SnapshotJoinDaoImpl extends GenericDaoBaseWithTagInformation<SnapshotJoinVO, SnapshotResponse> implements SnapshotJoinDao {

    public static final Logger s_logger = Logger.getLogger(SnapshotJoinDaoImpl.class);

    @Override
    public SnapshotResponse newSnapshotResponse(ResponseObject.ResponseView view, SnapshotJoinVO snapshot) {
        SnapshotResponse snapshotResponse = new SnapshotResponse();
        snapshotResponse.setId(snapshot.getUuid());
        // populate owner.
        ApiResponseHelper.populateOwner(snapshotResponse, snapshot);
        if (snapshot.getVolumeId() != null) {
            snapshotResponse.setVolumeId(snapshot.getVolumeUuid());
            snapshotResponse.setVolumeName(snapshot.getVolumeName());
            snapshotResponse.setVolumeType(snapshot.getVolumeType().name());
            snapshotResponse.setVirtualSize(snapshot.getVolumeSize());
        }
        snapshotResponse.setZoneId(snapshot.getDataCenterUuid());
        snapshotResponse.setZoneName(snapshot.getDataCenterName());
        snapshotResponse.setCreated(snapshot.getCreated());
        snapshotResponse.setName(snapshot.getName());
        String intervalType = null;
        if (snapshot.getSnapshotType() >= 0 && snapshot.getSnapshotType() < Snapshot.Type.values().length) {
            intervalType = Snapshot.Type.values()[snapshot.getSnapshotType()].name();
        }
        snapshotResponse.setIntervalType(intervalType);
        snapshotResponse.setState(snapshot.getStatus());
        snapshotResponse.setLocationType(snapshot.getLocationType() != null ? snapshot.getLocationType().name() : null);

//        SnapshotInfo snapshotInfo = null;

        snapshotResponse.setPhysicaSize(snapshot.getStoreSize());

        snapshotResponse.setObjectName("snapshot");
        return snapshotResponse;
    }

    @Override
    public SnapshotResponse setSnapshotResponse(ResponseObject.ResponseView view, SnapshotResponse snapsData, SnapshotJoinVO snapshot) {
        return null;
    }

    @Override
    public Pair<List<SnapshotJoinVO>, Integer> searchIncludingRemovedAndCount(final SearchCriteria<SnapshotJoinVO> sc, final Filter filter) {
        List<SnapshotJoinVO> objects = searchIncludingRemoved(sc, filter, null, false);
        Integer count = getCountIncludingRemoved(sc);
        return new Pair<>(objects, count);
    }
}
