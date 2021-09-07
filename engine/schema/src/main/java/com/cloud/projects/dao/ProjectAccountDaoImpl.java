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

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.projects.ProjectAccount;
import com.cloud.projects.ProjectAccountVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
public class ProjectAccountDaoImpl extends GenericDaoBase<ProjectAccountVO, Long> implements ProjectAccountDao {
    protected final SearchBuilder<ProjectAccountVO> AllFieldsSearch;
    protected final SearchBuilder<ProjectAccountVO> ProjectAccountSearch;
    final GenericSearchBuilder<ProjectAccountVO, Long> AdminSearch;
    final GenericSearchBuilder<ProjectAccountVO, Long> ProjectAccountsSearch;
    final GenericSearchBuilder<ProjectAccountVO, Long> CountByRoleSearch;

    public static final Logger s_logger = Logger.getLogger(ProjectAccountDaoImpl.class.getName());

    protected ProjectAccountDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("role", AllFieldsSearch.entity().getAccountRole(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("projectId", AllFieldsSearch.entity().getProjectId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("accountId", AllFieldsSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("projectAccountId", AllFieldsSearch.entity().getProjectAccountId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("userId", AllFieldsSearch.entity().getUserId(), Op.EQ);
        AllFieldsSearch.and("projectRoleId", AllFieldsSearch.entity().getProjectRoleId(), Op.EQ);
        AllFieldsSearch.done();

        ProjectAccountSearch = createSearchBuilder();
        ProjectAccountSearch.and("projectId", ProjectAccountSearch.entity().getProjectId(), SearchCriteria.Op.EQ);
        ProjectAccountSearch.and("accountId", ProjectAccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        ProjectAccountSearch.and("userId", ProjectAccountSearch.entity().getUserId(), Op.NULL);
        ProjectAccountSearch.done();


        AdminSearch = createSearchBuilder(Long.class);
        AdminSearch.selectFields(AdminSearch.entity().getProjectId());
        AdminSearch.and("role", AdminSearch.entity().getAccountRole(), Op.EQ);
        AdminSearch.and("accountId", AdminSearch.entity().getAccountId(), Op.EQ);
        AdminSearch.done();

        ProjectAccountsSearch = createSearchBuilder(Long.class);
        ProjectAccountsSearch.selectFields(ProjectAccountsSearch.entity().getProjectAccountId());
        ProjectAccountsSearch.and("accountId", ProjectAccountsSearch.entity().getAccountId(), Op.EQ);
        ProjectAccountsSearch.done();

        CountByRoleSearch = createSearchBuilder(Long.class);
        CountByRoleSearch.select(null, Func.COUNT, CountByRoleSearch.entity().getId());
        CountByRoleSearch.and("accountId", CountByRoleSearch.entity().getAccountId(), Op.EQ);
        CountByRoleSearch.and("role", CountByRoleSearch.entity().getAccountRole(), Op.EQ);
        CountByRoleSearch.done();
    }

    @Override
    public ProjectAccountVO getProjectOwner(long projectId) {
        SearchCriteria<ProjectAccountVO> sc = AllFieldsSearch.create();
        sc.setParameters("role", ProjectAccount.Role.Admin);
        sc.setParameters("projectId", projectId);

        return findOneBy(sc);
    }

    public List<ProjectAccountVO> getProjectOwners(long projectId) {
        SearchCriteria<ProjectAccountVO> sc = AllFieldsSearch.create();
        sc.setParameters("role", ProjectAccount.Role.Admin);
        sc.setParameters("projectId", projectId);

        return listBy(sc);
    }

    @Override
    public List<ProjectAccountVO> listByProjectId(long projectId) {
        SearchCriteria<ProjectAccountVO> sc = AllFieldsSearch.create();
        sc.setParameters("projectId", projectId);
        Filter filter = new Filter(ProjectAccountVO.class, "id", Boolean.TRUE, null, null);
        return listBy(sc, filter);
    }

    @Override
    public ProjectAccountVO findByProjectIdAccountId(long projectId, long accountId) {
        SearchCriteria<ProjectAccountVO> sc = ProjectAccountSearch.create();
        sc.setParameters("projectId", projectId);
        sc.setParameters("accountId", accountId);
        return findOneBy(sc);
    }

    @Override
    public ProjectAccountVO findByProjectIdUserId(long projectId, long accountId, long userId) {
        SearchCriteria<ProjectAccountVO> sc = AllFieldsSearch.create();
        sc.setParameters("projectId", projectId);
        sc.setParameters("userId", userId);
        sc.setParameters("accountId", accountId);

        return findOneBy(sc);
    }

    @Override
    public boolean canUserAccessProjectAccount(long accountId, long userId, long projectAccountId) {
        SearchCriteria<ProjectAccountVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("userId", userId);
        sc.setParameters("projectAccountId", projectAccountId);

        if (findOneBy(sc) != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean canAccessProjectAccount(long accountId, long projectAccountId) {
        SearchCriteria<ProjectAccountVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("projectAccountId", projectAccountId);

        if (findOneBy(sc) != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean canModifyProjectAccount(long accountId, long projectAccountId) {
        SearchCriteria<ProjectAccountVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("projectAccountId", projectAccountId);
        sc.setParameters("role", ProjectAccount.Role.Admin);

        if (findOneBy(sc) != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public List<Long> listPermittedAccountIds(long accountId) {
        SearchCriteria<Long> sc = ProjectAccountsSearch.create();
        sc.setParameters("accountId", accountId);
        return customSearch(sc, null);
    }

    @Override
    public List<Long> listAdministratedProjectIds(long adminAccountId) {
        SearchCriteria<Long> sc = AdminSearch.create();
        sc.setParameters("role", ProjectAccount.Role.Admin);
        sc.setParameters("accountId", adminAccountId);
        return customSearch(sc, null);
    }

    @Override
    public Long countByAccountIdAndRole(long accountId, ProjectAccount.Role role) {
        SearchCriteria<Long> sc = CountByRoleSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("role", role);
        return customSearch(sc, null).get(0);
    }

    @Override
    public void removeAccountFromProjects(long accountId) {
        SearchCriteria<ProjectAccountVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);

        int rowsRemoved = remove(sc);
        if (rowsRemoved > 0) {
            s_logger.debug("Removed account id=" + accountId + " from " + rowsRemoved + " projects");
        }
    }

    @Override
    public boolean canUserModifyProject(long projectId, long accountId, long userId) {
        SearchCriteria<ProjectAccountVO> sc = AllFieldsSearch.create();
        sc.setParameters("role",  ProjectAccount.Role.Admin);
        sc.setParameters("projectId",projectId);
        sc.setParameters("accountId", accountId);
        sc.setParameters("userId", userId);
        if (findOneBy(sc) != null) {
            return true;
        }
        return false;
    }

    @Override
    public List<ProjectAccountVO> listUsersOrAccountsByRole(long id) {
        SearchCriteria<ProjectAccountVO> sc = AllFieldsSearch.create();
        sc.setParameters("projectRoleId", id);
        return listBy(sc);
    }
}
