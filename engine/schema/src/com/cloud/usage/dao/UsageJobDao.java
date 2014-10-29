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
package com.cloud.usage.dao;

import java.util.Date;

import com.cloud.usage.UsageJobVO;
import com.cloud.utils.db.GenericDao;

public interface UsageJobDao extends GenericDao<UsageJobVO, Long> {
    Long checkHeartbeat(String hostname, int pid, int aggregationDuration);

    void createNewJob(String hostname, int pid, int jobType);

    UsageJobVO getLastJob();

    UsageJobVO getNextImmediateJob();

    long getLastJobSuccessDateMillis();

    Date getLastHeartbeat();

    UsageJobVO isOwner(String hostname, int pid);

    void updateJobSuccess(Long jobId, long startMillis, long endMillis, long execTime, boolean success);
}
