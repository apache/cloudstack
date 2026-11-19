// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the
// License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.ai.dao;

import java.util.List;

import com.cloud.utils.db.GenericDao;

/** Ownership rows for AI deployments (SPEC.md section 9). */
public interface AiDeploymentDao extends GenericDao<AiDeploymentVO, Long> {

    List<AiDeploymentVO> listByAccount(long accountId);

    List<AiDeploymentVO> listByDomain(long domainId);

    List<AiDeploymentVO> listByZone(long zoneId);

    AiDeploymentVO findByEngineId(String engineId);

    /**
     * Remove every deployment owned by an account.
     *
     * Called from the account deletion hook, so a deleted account leaves
     * nothing behind (SPEC.md section 9).
     *
     * @return how many rows were removed.
     */
    int removeByAccount(long accountId);
}
