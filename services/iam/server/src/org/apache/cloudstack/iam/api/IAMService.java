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

    /* IAM group related interfaces */
    IAMGroup createIAMGroup(String iamGroupName, String description, String path);

    boolean deleteIAMGroup(Long iamGroupId);

    List<IAMGroup> listIAMGroups(long accountId);

    IAMGroup addAccountsToGroup(List<Long> acctIds, Long groupId);

    IAMGroup removeAccountsFromGroup(List<Long> acctIds, Long groupId);

    List<Long> listAccountsByGroup(long groupId);

    Pair<List<IAMGroup>, Integer> listIAMGroups(Long iamGroupId, String iamGroupName, String path, Long startIndex, Long pageSize);

    /* IAM Policy related interfaces */
    IAMPolicy createIAMPolicy(String iamPolicyName, String description, Long parentPolicyId, String path);

    boolean deleteIAMPolicy(long iamPolicyId);

    List<IAMPolicy> listIAMPolicies(long accountId);

    List<IAMPolicy> listIAMPoliciesByGroup(long groupId);

    Pair<List<IAMPolicy>, Integer> listIAMPolicies(Long iamPolicyId, String iamPolicyName, String path, Long startIndex, Long pageSize);

    IAMGroup attachIAMPoliciesToGroup(List<Long> policyIds, Long groupId);

    IAMGroup removeIAMPoliciesFromGroup(List<Long> policyIds, Long groupId);

    void attachIAMPolicyToAccounts(Long policyId, List<Long> acctIds);

    void removeIAMPolicyFromAccounts(Long policyId, List<Long> acctIds);

    IAMPolicy addIAMPermissionToIAMPolicy(long iamPolicyId, String entityType, String scope, Long scopeId,
            String action, String accessType, Permission perm, Boolean recursive);

    IAMPolicy removeIAMPermissionFromIAMPolicy(long iamPolicyId, String entityType, String scope, Long scopeId,
            String action);

    void removeIAMPermissionForEntity(final String entityType, final Long entityId);

    IAMPolicy getResourceGrantPolicy(String entityType, Long entityId, String accessType, String action);

    IAMPolicy getResourceOwnerPolicy();

    List<IAMPolicyPermission> listPolicyPermissions(long policyId);

    List<IAMPolicyPermission> listPolicyPermissionsByScope(long policyId, String action, String scope, String accessType);

    List<IAMPolicyPermission> listPolicyPermissionByActionAndEntity(long policyId, String action, String entityType);

    boolean isActionAllowedForPolicies(String action, List<IAMPolicy> policies);

    List<Long> getGrantedEntities(long accountId, String action, String scope);

    IAMPolicy resetIAMPolicy(long iamPolicyId);

    List<IAMPolicyPermission> listPolicyPermissionByAccessAndEntity(long policyId, String accessType,
            String entityType);

    List<IAMGroup> listParentIAMGroups(long groupId);

    List<IAMPolicy> listRecursiveIAMPoliciesByGroup(long groupId);

    /* Interface used for cache IAM checkAccess result */
    void addToIAMCache(Object accessKey, Object allowDeny);

    Object getFromIAMCache(Object accessKey);

    void invalidateIAMCache();

}
