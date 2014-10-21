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
package com.cloud.projects.dao;

import java.util.List;

import com.cloud.projects.ProjectAccount;
import com.cloud.projects.ProjectAccountVO;
import com.cloud.utils.db.GenericDao;

public interface ProjectAccountDao extends GenericDao<ProjectAccountVO, Long> {
    ProjectAccountVO getProjectOwner(long projectId);

    List<ProjectAccountVO> listByProjectId(long projectId);

    ProjectAccountVO findByProjectIdAccountId(long projectId, long accountId);

    boolean canAccessProjectAccount(long accountId, long projectAccountId);

    boolean canModifyProjectAccount(long accountId, long projectAccountId);

    List<Long> listPermittedAccountIds(long accountId);

    List<Long> listAdministratedProjectIds(long adminAccountId);

    Long countByAccountIdAndRole(long accountId, ProjectAccount.Role role);

    void removeAccountFromProjects(long accountId);
}
