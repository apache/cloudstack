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
package org.apache.cloudstack.framework.jobs.dao;

import java.util.Date;
import java.util.List;

import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;

import com.cloud.utils.db.GenericDao;

public interface AsyncJobDao extends GenericDao<AsyncJobVO, Long> {

    AsyncJobVO findInstancePendingAsyncJob(String instanceType, long instanceId);

    List<AsyncJobVO> findInstancePendingAsyncJobs(String instanceType, Long accountId);

    AsyncJobVO findPseudoJob(long threadId, long msid);

    void cleanupPseduoJobs(long msid);

    List<AsyncJobVO> getExpiredJobs(Date cutTime, int limit);

    List<AsyncJobVO> getExpiredUnfinishedJobs(Date cutTime, int limit);

    void resetJobProcess(long msid, int jobResultCode, String jobResultMessage);

    List<AsyncJobVO> getExpiredCompletedJobs(Date cutTime, int limit);

    List<AsyncJobVO> getResetJobs(long msid);

    List<AsyncJobVO> getFailureJobsSinceLastMsStart(long msId, String... cmds);

    long countPendingJobs(String havingInfo, String... cmds);

    // Returns the number of pending jobs for the given Management server msids.
    // NOTE: This is the msid and NOT the id
    long countPendingNonPseudoJobs(Long... msIds);
}
