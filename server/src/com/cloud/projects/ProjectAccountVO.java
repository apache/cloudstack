/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
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

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="project_account")
@SuppressWarnings("unused")
public class ProjectAccountVO implements ProjectAccount{
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
    @Column(name="project_id")
    private long projectId;

    @Column(name="account_id")
    private long accountId;
    
    @Column(name="account_role")
    @Enumerated(value=EnumType.STRING)
    private Role accountRole = Role.Regular;
    
    @Column(name="project_account_id")
    long projectAccountId;
    
    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;

    
    protected ProjectAccountVO(){
    }
    
    public ProjectAccountVO(Project project, long accountId, Role accountRole) {
       this.accountId = accountId;
       this.accountRole = accountRole;
       this.projectId = project.getId();
       this.projectAccountId = project.getProjectAccountId();
    }

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
    public Role getAccountRole() {
        return accountRole;
    }
    
    @Override
    public long getProjectAccountId() {
        return projectAccountId;
    }

    public void setAccountRole(Role accountRole) {
        this.accountRole = accountRole;
    }
}
