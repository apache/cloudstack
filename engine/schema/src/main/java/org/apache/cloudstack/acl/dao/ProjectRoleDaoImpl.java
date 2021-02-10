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

package org.apache.cloudstack.acl.dao;

import java.util.List;

import org.apache.cloudstack.acl.ProjectRoleVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.google.common.base.Strings;

public class ProjectRoleDaoImpl extends GenericDaoBase<ProjectRoleVO, Long>  implements ProjectRoleDao{
    private final SearchBuilder<ProjectRoleVO>  ProjectRoleSearch;

    public ProjectRoleDaoImpl() {
        super();

        ProjectRoleSearch = createSearchBuilder();
        ProjectRoleSearch.and("name", ProjectRoleSearch.entity().getName(), SearchCriteria.Op.LIKE);
        ProjectRoleSearch.and("project_id", ProjectRoleSearch.entity().getProjectId(), SearchCriteria.Op.EQ);
        ProjectRoleSearch.done();

    }
    @Override
    public List<ProjectRoleVO> findByName(String name, Long projectId) {
        SearchCriteria<ProjectRoleVO> sc = ProjectRoleSearch.create();
        if (!Strings.isNullOrEmpty(name)) {
            sc.setParameters("name", "%" + name + "%");
        }
        if (projectId != null) {
            sc.setParameters("project_id", projectId);
        }
        return listBy(sc);
    }

    @Override
    public List<ProjectRoleVO> findAllRoles(Long projectId) {
        SearchCriteria<ProjectRoleVO> sc = ProjectRoleSearch.create();
        if (projectId != null) {
            sc.setParameters("project_id", projectId);
        }
        return listBy(sc);
    }
}
