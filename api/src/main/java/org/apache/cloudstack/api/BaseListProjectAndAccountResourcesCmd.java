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
package org.apache.cloudstack.api;

import org.apache.cloudstack.api.response.ProjectResponse;

public abstract class BaseListProjectAndAccountResourcesCmd extends BaseListAccountResourcesCmd implements IBaseListProjectAndAccountResourcesCmd {

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "list objects by project; if projectid=-1 lists All VMs")
    private Long projectId;

    @Override
    public Long getProjectId() {
        return projectId;
    }
}
