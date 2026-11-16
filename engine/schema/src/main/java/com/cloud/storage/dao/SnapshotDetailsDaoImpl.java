/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.storage.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.resourcedetail.ResourceDetailsDaoBase;

import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

public class SnapshotDetailsDaoImpl extends ResourceDetailsDaoBase<SnapshotDetailsVO> implements SnapshotDetailsDao {
    private static final String GET_SNAPSHOT_DETAILS_ON_ZONE = "SELECT s.* FROM snapshot_details s LEFT JOIN snapshots ss ON ss.id=s.snapshot_id WHERE ss.data_center_id = ? AND s.name = ?";

    @Override
    public void addDetail(long resourceId, String key, String value, boolean display) {
        super.addDetail(new SnapshotDetailsVO(resourceId, key, value, display));
    }

    public List<SnapshotDetailsVO> findDetailsByZoneAndKey(long dcId, String key) {
        StringBuilder sql = new StringBuilder(GET_SNAPSHOT_DETAILS_ON_ZONE);
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        List<SnapshotDetailsVO> snapshotDetailsOnZone = new ArrayList<SnapshotDetailsVO>();
        try (PreparedStatement pstmt = txn.prepareStatement(sql.toString());) {
            if (pstmt != null) {
                pstmt.setLong(1, dcId);
                pstmt.setString(2, key);
                try (ResultSet rs = pstmt.executeQuery();) {
                    while (rs.next()) {
                        snapshotDetailsOnZone.add(toEntityBean(rs, false));
                    }
                } catch (SQLException e) {
                    throw new CloudRuntimeException("Could not find details by given zone and key due to:" + e.getMessage(), e);
                }
            }
            return snapshotDetailsOnZone;
        } catch (SQLException e) {
            throw new CloudRuntimeException("Could not find details by given zone and key due to:" + e.getMessage(), e);
        }
    }
}
