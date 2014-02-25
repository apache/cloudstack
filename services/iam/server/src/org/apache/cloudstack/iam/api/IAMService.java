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
package org.apache.cloudstack.iam.api;

import java.util.List;

import org.apache.cloudstack.iam.api.IAMPolicyPermission.Permission;

import com.cloud.utils.Pair;

public interface IAMService {

    /* ACL group related interfaces */
    IAMGroup createAclGroup(String aclGroupName, String description, String path);

    boolean deleteAclGroup(Long aclGroupId);

    List<IAMGroup> listAclGroups(long accountId);

    IAMGroup addAccountsToGroup(List<Long> acctIds, Long groupId);

    IAMGroup removeAccountsFromGroup(List<Long> acctIds, Long groupId);

    List<Long> listAccountsByGroup(long groupId);

    Pair<List<IAMGroup>, Integer> listAclGroups(Long aclGroupId, String aclGroupName, String path, Long startIndex, Long pageSize);

    /* ACL Policy related interfaces */
    IAMPolicy createAclPolicy(String aclPolicyName, String description, Long parentPolicyId, String path);

    boolean deleteAclPolicy(long aclPolicyId);

    List<IAMPolicy> listAclPolicies(long accountId);

    List<IAMPolicy> listAclPoliciesByGroup(long groupId);

    Pair<List<IAMPolicy>, Integer> listAclPolicies(Long aclPolicyId, String aclPolicyName, String path, Long startIndex, Long pageSize);

    IAMGroup attachAclPoliciesToGroup(List<Long> policyIds, Long groupId);

    IAMGroup removeAclPoliciesFromGroup(List<Long> policyIds, Long groupId);

    void attachAclPolicyToAccounts(Long policyId, List<Long> acctIds);

    void removeAclPolicyFromAccounts(Long policyId, List<Long> acctIds);

    IAMPolicy addAclPermissionToAclPolicy(long aclPolicyId, String entityType, String scope, Long scopeId,
            String action, String accessType, Permission perm, Boolean recursive);

    IAMPolicy removeAclPermissionFromAclPolicy(long aclPolicyId, String entityType, String scope, Long scopeId,
            String action);

    void removeAclPermissionForEntity(final String entityType, final Long entityId);

    IAMPolicy getResourceGrantPolicy(String entityType, Long entityId, String accessType, String action);

    IAMPolicy getResourceOwnerPolicy();

    List<IAMPolicyPermission> listPolicyPermissions(long policyId);

    List<IAMPolicyPermission> listPolicyPermissionsByScope(long policyId, String action, String scope);

    List<IAMPolicyPermission> listPolicyPermissionByActionAndEntity(long policyId, String action, String entityType);

    boolean isActionAllowedForPolicies(String action, List<IAMPolicy> policies);

    List<Long> getGrantedEntities(long accountId, String action, String scope);

    IAMPolicy resetAclPolicy(long aclPolicyId);

    List<IAMPolicyPermission> listPolicyPermissionByAccessAndEntity(long policyId, String accessType,
            String entityType);

    List<IAMGroup> listParentAclGroups(long groupId);

    List<IAMPolicy> listRecursiveAclPoliciesByGroup(long groupId);

}
