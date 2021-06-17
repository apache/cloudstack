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

package org.apache.cloudstack.api.response;

import org.apache.cloudstack.acl.ProjectRolePermission;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = ProjectRolePermission.class)
public class ProjectRolePermissionResponse extends BaseRolePermissionResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the project role permission")
    private String id;

    @SerializedName(ApiConstants.PROJECT_ROLE_ID)
    @Param(description = "the ID of the project role to which the role permission belongs")
    private String projectRoleId;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the ID of the project")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT_ROLE_NAME)
    @Param(description = "the name of the project role to which the role permission belongs")
    private String projectRoleName;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectRoleId() {
        return projectRoleId;
    }

    public void setProjectRoleId(String projectRoleId) {
        this.projectRoleId = projectRoleId;
    }

    public String getProjectRoleName() {
        return projectRoleName;
    }

    public void setProjectRoleName(String projectRoleName) {
        this.projectRoleName = projectRoleName;
    }
}
