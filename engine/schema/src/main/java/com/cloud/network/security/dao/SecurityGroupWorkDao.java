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
package com.cloud.network.security.dao;

import java.util.Date;
import java.util.List;

import com.cloud.network.security.SecurityGroupWork;
import com.cloud.network.security.SecurityGroupWork.Step;
import com.cloud.network.security.SecurityGroupWorkVO;
import com.cloud.utils.db.GenericDao;

public interface SecurityGroupWorkDao extends GenericDao<SecurityGroupWorkVO, Long> {
    SecurityGroupWork findByVmId(long vmId, boolean taken);

    SecurityGroupWorkVO findByVmIdStep(long vmId, Step step);

    SecurityGroupWorkVO take(long serverId);

    void updateStep(Long vmId, Long logSequenceNumber, Step done);

    void updateStep(Long workId, Step done);

    int deleteFinishedWork(Date timeBefore);

    List<SecurityGroupWorkVO> findUnfinishedWork(Date timeBefore);

    List<SecurityGroupWorkVO> findAndCleanupUnfinishedWork(Date timeBefore);

    List<SecurityGroupWorkVO> findScheduledWork();

}
