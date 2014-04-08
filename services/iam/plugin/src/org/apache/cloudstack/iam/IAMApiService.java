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
package org.apache.cloudstack.iam;

import java.util.List;

import org.apache.cloudstack.acl.PermissionScope;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.iam.IAMGroupResponse;
import org.apache.cloudstack.api.response.iam.IAMPolicyResponse;
import org.apache.cloudstack.iam.api.IAMGroup;
import org.apache.cloudstack.iam.api.IAMPolicy;
import org.apache.cloudstack.iam.api.IAMPolicyPermission;
import org.apache.cloudstack.iam.api.IAMPolicyPermission.Permission;

import com.cloud.user.Account;
import com.cloud.utils.component.PluggableService;

public interface IAMApiService extends PluggableService {

    /* ACL group related interfaces */
    IAMGroup createIAMGroup(Account caller, String iamGroupName, String description);

    boolean deleteIAMGroup(Long iamGroupId);

    List<IAMGroup> listIAMGroups(long accountId);

    IAMGroup addAccountsToGroup(List<Long> acctIds, Long groupId);

    IAMGroup removeAccountsFromGroup(List<Long> acctIds, Long groupId);

    /* IAM Policy related interfaces */
    IAMPolicy createIAMPolicy(Account caller, String iamPolicyName, String description, Long parentPolicyId);

    boolean deleteIAMPolicy(long iamPolicyId);

    List<IAMPolicy> listIAMPolicies(long accountId);

    IAMGroup attachIAMPoliciesToGroup(List<Long> policyIds, Long groupId);

    IAMGroup removeIAMPoliciesFromGroup(List<Long> policyIds, Long groupId);

    void attachIAMPolicyToAccounts(Long policyId, List<Long> accountIds);

    void removeIAMPolicyFromAccounts(Long policyId, List<Long> accountIds);

    IAMPolicy addIAMPermissionToIAMPolicy(long iamPolicyId, String entityType, PermissionScope scope, Long scopeId,
            String action, Permission perm, Boolean recursive, Boolean readOnly);

    IAMPolicy removeIAMPermissionFromIAMPolicy(long iamPolicyId, String entityType, PermissionScope scope, Long scopeId, String action);

    IAMPolicyPermission getIAMPolicyPermission(long accountId, String entityType, String action);

    /* Utility routine to grant/revoke invidivual resource to list of accounts */
    void grantEntityPermissioinToAccounts(String entityType, Long entityId, AccessType accessType, String action, List<Long> accountIds);

    void revokeEntityPermissioinFromAccounts(String entityType, Long entityId, AccessType accessType, String action, List<Long> accountIds);

    /* Response Generation */
    IAMPolicyResponse createIAMPolicyResponse(IAMPolicy policy);

    IAMGroupResponse createIAMGroupResponse(IAMGroup group);

    ListResponse<IAMGroupResponse> listIAMGroups(Long iamGroupId, String iamGroupName,
            Long domainId, Long startIndex, Long pageSize);

    ListResponse<IAMPolicyResponse> listIAMPolicies(Long iamPolicyId, String iamPolicyName,
            Long domainId, Long startIndex, Long pageSize);

    // Convert passed scope uuid to internal scope long id
    Long getPermissionScopeId(String scope, String entityType, String scopeId);
}
