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

import java.util.List;

import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupMember;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupReadinessRuleVO;

import com.cloud.utils.db.GenericDao;

public interface InstanceBootGroupReadinessRuleDao extends GenericDao<InstanceBootGroupReadinessRuleVO, Long> {

    List<InstanceBootGroupReadinessRuleVO> listByBootGroupId(long bootGroupId);

    List<InstanceBootGroupReadinessRuleVO> listEnabledByItem(long bootGroupId, InstanceBootGroupMember.MemberType itemType, long itemId);

    List<InstanceBootGroupReadinessRuleVO> listByItem(long bootGroupId, InstanceBootGroupMember.MemberType itemType, long itemId);

    /**
     * Cleanup for a member removed from its boot group, or a VM leaving its Instance Group — there's
     * no FK path for this (item_id doesn't reference instance_boot_group_member/instance_group_vm_map),
     * so it's enforced here in code instead of a DB cascade.
     */
    void deleteByItem(InstanceBootGroupMember.MemberType itemType, long itemId);
}
