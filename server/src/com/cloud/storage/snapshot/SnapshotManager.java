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
package com.cloud.storage.snapshot;

import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.host.HostVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.fsm.NoTransitionException;

/**
 *
 *
 */
public interface SnapshotManager {

    public static final int HOURLYMAX = 8;
    public static final int DAILYMAX = 8;
    public static final int WEEKLYMAX = 8;
    public static final int MONTHLYMAX = 12;
    public static final int DELTAMAX = 16;

    void deletePoliciesForVolume(Long volumeId);

    /**
     * For each of the volumes in the account, (which can span across multiple zones and multiple secondary storages), delete
     * the dir on the secondary storage which contains the backed up snapshots for that volume. This is called during
     * deleteAccount.
     *
     * @param accountId
     *            The account which is to be deleted.
     */
    boolean deleteSnapshotDirsForAccount(long accountId);

    String getSecondaryStorageURL(SnapshotVO snapshot);

   //void deleteSnapshotsDirForVolume(String secondaryStoragePoolUrl, Long dcId, Long accountId, Long volumeId);

	boolean canOperateOnVolume(Volume volume);

	Answer sendToPool(Volume vol, Command cmd);

	SnapshotVO getParentSnapshot(VolumeInfo volume);

	Snapshot backupSnapshot(Long snapshotId);

	SnapshotInfo takeSnapshot(VolumeInfo volume)
			throws ResourceAllocationException;
}
