/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.storage.dao;

import java.util.List;

import com.cloud.storage.Snapshot;
import com.cloud.storage.Snapshot.Status;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Snapshot.Type;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDao;

public interface SnapshotDao extends GenericDao<SnapshotVO, Long> {
	List<SnapshotVO> listByVolumeId(long volumeId);
	List<SnapshotVO> listByVolumeId(Filter filter, long volumeId);
	SnapshotVO findNextSnapshot(long parentSnapId);
	long getLastSnapshot(long volumeId, long snapId);
    List<SnapshotVO> listByVolumeIdType(long volumeId, Type type);
    List<SnapshotVO> listByVolumeIdIncludingRemoved(long volumeId);
    List<SnapshotVO> listByBackupUuid(long volumeId, String backupUuid);
    long updateSnapshotVersion(long volumeId, String from, String to);
    List<SnapshotVO> listByVolumeIdVersion(long volumeId, String version);
    Long getSecHostId(long volumeId);
    long updateSnapshotSecHost(long dcId, long secHostId);
    List<SnapshotVO> listByHostId(Filter filter, long hostId);
    List<SnapshotVO> listByHostId(long hostId);
    public Long countSnapshotsForAccount(long accountId);
	List<SnapshotVO> listByInstanceId(long instanceId, Snapshot.Status... status);
	List<SnapshotVO> listByStatus(long volumeId, Snapshot.Status... status);
}
