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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "project_account")
@SuppressWarnings("unused")
public class ProjectAccountVO implements ProjectAccount, InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "project_id")
    private long projectId;

    @Column(name = "account_id")
    private long accountId;

    @Column(name="user_id")
    private Long userId;

    @Column(name = "account_role")
    @Enumerated(value = EnumType.STRING)
    private Role accountRole = Role.Regular;

    @Column(name = "project_account_id")
    long projectAccountId;

    @Column(name = "project_role_id")
    private Long projectRoleId;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    protected ProjectAccountVO() {
    }

    public ProjectAccountVO(Project project, long accountId, Role accountRole, Long userId, Long projectRoleId) {
        this.accountId = accountId;
        if (accountRole != null) {
            this.accountRole = accountRole;
        } else {
            this.accountRole = Role.Regular;
        }
        this.projectId = project.getId();
        this.projectAccountId = project.getProjectAccountId();
        this.userId = userId;
        this.projectRoleId = projectRoleId;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getProjectId() {
        return projectId;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public Long getUserId() { return userId; }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public Role getAccountRole() {
        return accountRole;
    }

    @Override
    public long getProjectAccountId() {
        return projectAccountId;
    }

    public void setProjectRoleId(Long projectRoleId) {
        this.projectRoleId = projectRoleId;
    }

    @Override
    public Long getProjectRoleId() { return projectRoleId; }

    public void setAccountRole(Role accountRole) {
        this.accountRole = accountRole;
    }
}
