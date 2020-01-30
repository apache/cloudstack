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

package com.cloud.network.dao;

import java.util.List;

import com.cloud.utils.db.GenericDao;

public interface RouterHealthCheckResultDao extends GenericDao<RouterHealthCheckResultVO, Long> {
    /**
     * @param routerId
     * @return Returns all the health checks in the database for the given router id
     */
    List<RouterHealthCheckResultVO> getHealthCheckResults(long routerId);

    boolean expungeHealthChecks(long routerId);

    /**
     * @param routerId
     * @return true if there are checks that have been marked failed in the database
     */
    boolean hasFailingChecks(long routerId);

    /**
     * For a router, we have only one (check name, check type) possible as we keep the most
     * recent check result. This method finds that last check result.
     *
     * @param routerId
     * @param checkName
     * @param checkType
     * @return returns the check result for the routerId, check type and the check name.
     */
    RouterHealthCheckResultVO getRouterHealthCheckResult(long routerId, String checkName, String checkType);
}
