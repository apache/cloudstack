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
import java.util.Map;

import javax.naming.ConfigurationException;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.projects.ProjectAccount.Role;
import com.cloud.user.Account;
import com.cloud.utils.component.ManagerBase;

public class MockProjectManagerImpl extends ManagerBase implements ProjectManager {

    @Override
    public Project createProject(String name, String displayText, String accountName, Long domainId) throws ResourceAllocationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean deleteProject(long id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Project getProject(long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProjectAccount assignAccountToProject(Project project, long accountId, Role accountRole) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account getProjectOwner(long projectId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean unassignAccountFromProject(long projectId, long accountId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Project findByProjectAccountId(long projectAccountId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Project findByNameAndDomainId(String name, long domainId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Project updateProject(long id, String displayText, String newOwnerName) throws ResourceAllocationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean addAccountToProject(long projectId, String accountName, String email) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteAccountFromProject(long projectId, String accountName) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean updateInvitation(long projectId, String accountName, String token, boolean accept) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Project activateProject(long projectId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Project suspendProject(long projectId) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Project enableProject(long projectId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean deleteProjectInvitation(long invitationId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean start() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean stop() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean canAccessProjectAccount(Account caller, long accountId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canModifyProjectAccount(Account caller, long accountId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteAccountFromProject(long projectId, long accountId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<Long> listPermittedProjectAccounts(long accountId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean projectInviteRequired() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean allowUserToCreateProject() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteProject(Account caller, long callerUserId, ProjectVO project) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long getInvitationTimeout() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Project findByProjectAccountIdIncludingRemoved(long projectAccountId) {
        return null;
    }

}
