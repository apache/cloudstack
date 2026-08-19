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

package com.cloud.vm.dao;

import java.util.Date;

import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupReadinessCheckResultVO;
import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRule;

import com.cloud.utils.db.GenericDao;

public interface InstanceBootGroupReadinessCheckResultDao extends GenericDao<InstanceBootGroupReadinessCheckResultVO, Long> {

    /**
     * vmId 0 is the rule's own row (a VM-scoped rule's single target, or a group-scoped rule's
     * all-members aggregate); any other vmId is one inherited member's individual result.
     */
    InstanceBootGroupReadinessCheckResultVO findByRuleAndVm(long ruleId, long vmId);

    /**
     * Inserts or updates the single cached result row for (ruleId, vmId) (no history, by design).
     */
    void upsert(long ruleId, long vmId, InstanceBootGroupReadinessRule.Status status, String message, Date checkedOn);

    void deleteByRuleId(long ruleId);
}
