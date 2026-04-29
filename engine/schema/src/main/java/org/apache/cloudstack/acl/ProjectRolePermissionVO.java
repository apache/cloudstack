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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "project_role_permissions")
public class ProjectRolePermissionVO extends RolePermissionBaseVO implements ProjectRolePermission {

    @Column(name = "project_id")
    private long projectId;

    @Column(name = "project_role_id")
    private long projectRoleId;

    @Column(name = "sort_order")
    private long sortOrder = 0;

    public ProjectRolePermissionVO() {
        super();
    }

    public ProjectRolePermissionVO(final long projectId, final long projectRoleId, final String rule, final Permission permission, final String description) {
        super(rule, permission, description);
        this.projectId = projectId;
        this.projectRoleId = projectRoleId;
    }

    @Override
    public long getProjectRoleId() {
        return projectRoleId;
    }

    public void setProjectRoleId(long projectRoleId) {
        this.projectRoleId = projectRoleId;
    }

    @Override
    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public long getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(long sortOrder) {
        this.sortOrder = sortOrder;
    }
}
