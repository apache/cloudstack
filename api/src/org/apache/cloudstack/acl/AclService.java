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
package org.apache.cloudstack.acl;

import java.util.List;

public interface AclService {

    /**
     * Creates an acl role for the given domain.
     *
     * @param domainId
     * @param name
     * @param description
     * @return AclRole
     */

    AclRole createAclRole(Long domainId, String aclRoleName, String description, Long parentRoleId);

    /**
     * Delete an acl role.
     *
     * @param aclRoleId
     */
    boolean deleteAclRole(long aclRoleId);

    AclRole grantPermissionToAclRole(long aclRoleId, List<String> apiNames);

    AclRole revokePermissionFromAclRole(long aclRoleId, List<String> apiNames);

    AclGroup addAclRolesToGroup(List<Long> roleIds, Long groupId);

    AclGroup removeAclRolesFromGroup(List<Long> roleIds, Long groupId);

    AclGroup addAccountsToGroup(List<Long> acctIds, Long groupId);

    AclGroup removeAccountsFromGroup(List<Long> acctIds, Long groupId);

    /**
     * Creates an acl group for the given domain.
     *
     * @param domainId
     * @param name
     * @param description
     * @return AclGroup
     */

    AclGroup createAclGroup(Long domainId, String aclGroupName, String description);

    /**
     * Delete an acl group.
     *
     * @param aclGroupId
     */
    boolean deleteAclGroup(Long aclGroupId);


}
