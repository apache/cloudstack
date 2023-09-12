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
package com.cloud.projects;

import java.util.List;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.projects.ProjectAccount.Role;
import com.cloud.user.Account;

public interface ProjectService {
    /**
     * Creates a new project
     *
     * @param name
     *            - project name
     * @param displayText
     *            - project display text
     * @param accountName
     *            - account name of the project owner
     * @param domainId
     *            - domainid of the project owner
     *
     * @param userId
     *            - id of the user to be made as project owner
     *
     * @param accountId
     *            - id of the account to which the user belongs
     *
     * @return the project if created successfully, null otherwise
     * @throws ResourceAllocationException
     */
    Project createProject(String name, String displayText, String accountName, Long domainId, Long userId, Long accountId) throws ResourceAllocationException;

    /**
     * Deletes a project
     *
     * @param id
     *            - project id
     * @return true if the project was deleted successfully, false otherwise
     */
    boolean deleteProject(long id, Boolean cleanup);

    /**
     * Gets a project by id
     *
     * @param id
     *            - project id
     * @return project object
     */
    Project getProject(long id);

    ProjectAccount assignAccountToProject(Project project, long accountId, Role accountRole, Long userId, Long projectRoleId);

    Account getProjectOwner(long projectId);

    List<Long> getProjectOwners(long projectId);

    boolean unassignAccountFromProject(long projectId, long accountId);

    Project findByProjectAccountId(long projectAccountId);

    Project findByNameAndDomainId(String name, long domainId);

    Project updateProject(long id, String name, String displayText, String newOwnerName) throws ResourceAllocationException;

    Project updateProject(long id, String name, String displayText, String newOwnerName, Long userId, Role newRole) throws ResourceAllocationException;

    boolean addAccountToProject(long projectId, String accountName, String email, Long projectRoleId, Role projectRoleType);

    boolean deleteAccountFromProject(long projectId, String accountName);

    boolean deleteUserFromProject(long projectId, long userId);

    boolean updateInvitation(long projectId, String accountName, Long userId, String token, boolean accept);

    Project activateProject(long projectId);

    Project suspendProject(long projectId) throws ConcurrentOperationException, ResourceUnavailableException;

    Project enableProject(long projectId);

    boolean deleteProjectInvitation(long invitationId);

    Project findByProjectAccountIdIncludingRemoved(long projectAccountId);

    boolean addUserToProject(Long projectId, String username, String email, Long projectRoleId, Role projectRole);

}
