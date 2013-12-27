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

public interface IAMService {

    /* ACL group related interfaces */
    AclGroup createAclGroup(String aclGroupName, String description, String path);

    boolean deleteAclGroup(Long aclGroupId);

    List<AclGroup> listAclGroups(long accountId);

    AclGroup addAccountsToGroup(List<Long> acctIds, Long groupId);

    AclGroup removeAccountsFromGroup(List<Long> acctIds, Long groupId);

    /* ACL Policy related interfaces */
    AclPolicy createAclPolicy(String aclPolicyName, String description, Long parentPolicyId);

    boolean deleteAclPolicy(long aclPolicyId);

    List<AclPolicy> listAclPolicies(long accountId);

    AclGroup attachAclPoliciesToGroup(List<Long> policyIds, Long groupId);

    AclGroup removeAclPoliciesFromGroup(List<Long> policyIds, Long groupId);

    AclPolicy addAclPermissionToAclPolicy(long aclPolicyId, String entityType, String scope, Long scopeId,
            String action, String accessType, Permission perm);

    AclPolicy removeAclPermissionFromAclPolicy(long aclPolicyId, String entityType, String scope, Long scopeId,
            String action);

    List<AclPolicyPermission> listPolicyPermissionsByScope(long policyId, String action, String scope);

    boolean isAPIAccessibleForPolicies(String apiName, List<AclPolicy> policies);

    List<Long> getGrantedEntities(long accountId, String action, String scope);


}
