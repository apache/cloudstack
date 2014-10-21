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

import java.util.Date;

import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.utils.component.Manager;
import com.cloud.utils.concurrency.Scheduler;

/**
 */
public interface SnapshotScheduler extends Manager, Scheduler {

    /**
     * Schedule the next snapshot job for this policy instance.
     *
     * @return The timestamp at which the next snapshot is scheduled.
     */
    public Date scheduleNextSnapshotJob(SnapshotPolicyVO policyInstance);

    /**
     * Remove schedule for volumeId, policyId combination
     * @param volumeId
     * @param policyId
     * @return
     */
    boolean removeSchedule(Long volumeId, Long policyId);

    void scheduleOrCancelNextSnapshotJobOnDisplayChange(SnapshotPolicyVO policy, boolean previousDisplay);
}
