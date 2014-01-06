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

import org.apache.cloudstack.iam.api.AclPolicyPermission.Permission;

import com.cloud.utils.Pair;

public interface IAMService {

    /* ACL group related interfaces */
    AclGroup createAclGroup(String aclGroupName, String description, String path);

    boolean deleteAclGroup(Long aclGroupId);

    List<AclGroup> listAclGroups(long accountId);

    AclGroup addAccountsToGroup(List<Long> acctIds, Long groupId);

    AclGroup removeAccountsFromGroup(List<Long> acctIds, Long groupId);

    List<Long> listAccountsByGroup(long groupId);

    Pair<List<AclGroup>, Integer> listAclGroups(Long aclGroupId, String aclGroupName, String path, Long startIndex, Long pageSize);

    /* ACL Policy related interfaces */
    AclPolicy createAclPolicy(String aclPolicyName, String description, Long parentPolicyId);

    boolean deleteAclPolicy(long aclPolicyId);

    List<AclPolicy> listAclPolicies(long accountId);

    List<AclPolicy> listAclPoliciesByGroup(long groupId);

    Pair<List<AclPolicy>, Integer> listAclPolicies(Long aclPolicyId, String aclPolicyName, String path, Long startIndex, Long pageSize);

    AclGroup attachAclPoliciesToGroup(List<Long> policyIds, Long groupId);

    AclGroup removeAclPoliciesFromGroup(List<Long> policyIds, Long groupId);

    AclPolicy addAclPermissionToAclPolicy(long aclPolicyId, String entityType, String scope, Long scopeId,
            String action, String accessType, Permission perm);

    AclPolicy removeAclPermissionFromAclPolicy(long aclPolicyId, String entityType, String scope, Long scopeId,
            String action);

    AclPolicy getResourceOwnerPolicy();

    List<AclPolicyPermission> listPolicyPermissions(long policyId);

    List<AclPolicyPermission> listPolicyPermissionsByScope(long policyId, String action, String scope);

    List<AclPolicyPermission> listPolicyPermissionByEntityType(long policyId, String action, String entityType);

    boolean isActionAllowedForPolicies(String action, List<AclPolicy> policies);

    List<Long> getGrantedEntities(long accountId, String action, String scope);

    AclPolicy resetAclPolicy(long aclPolicyId);

    List<AclPolicyPermission> listPolicyPermissionByAccessType(long policyId, String accessType, String entityType, String action);

}
