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

import java.sql.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.projects.ProjectInvitation.State;
import com.cloud.projects.ProjectInvitationVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class ProjectInvitationDaoImpl extends GenericDaoBase<ProjectInvitationVO, Long> implements ProjectInvitationDao {
    private static final Logger s_logger = Logger.getLogger(ProjectInvitationDaoImpl.class);
    protected final SearchBuilder<ProjectInvitationVO> AllFieldsSearch;
    protected final SearchBuilder<ProjectInvitationVO> InactiveSearch;
    protected final SearchBuilder<ProjectInvitationVO> ProjectAccountInviteSearch;

    protected ProjectInvitationDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("accountId", AllFieldsSearch.entity().getForAccountId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("projectId", AllFieldsSearch.entity().getProjectId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("userId", AllFieldsSearch.entity().getForUserId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("created", AllFieldsSearch.entity().getCreated(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("projectAccountId", AllFieldsSearch.entity().getState(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), SearchCriteria.Op.IN);
        AllFieldsSearch.and("email", AllFieldsSearch.entity().getEmail(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("token", AllFieldsSearch.entity().getToken(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();

        ProjectAccountInviteSearch = createSearchBuilder();
        ProjectAccountInviteSearch.and("accountId", ProjectAccountInviteSearch.entity().getForAccountId(), SearchCriteria.Op.EQ);
        ProjectAccountInviteSearch.and("projectId", ProjectAccountInviteSearch.entity().getProjectId(), SearchCriteria.Op.EQ);
        ProjectAccountInviteSearch.and("userId", ProjectAccountInviteSearch.entity().getForUserId(), SearchCriteria.Op.NULL);
        ProjectAccountInviteSearch.done();

        InactiveSearch = createSearchBuilder();
        InactiveSearch.and("id", InactiveSearch.entity().getId(), SearchCriteria.Op.EQ);
        InactiveSearch.and("accountId", InactiveSearch.entity().getForAccountId(), SearchCriteria.Op.EQ);
        InactiveSearch.and("projectId", InactiveSearch.entity().getProjectId(), SearchCriteria.Op.EQ);
        InactiveSearch.and("created", InactiveSearch.entity().getCreated(), SearchCriteria.Op.LTEQ);
        InactiveSearch.and("state", InactiveSearch.entity().getState(), SearchCriteria.Op.EQ);
        InactiveSearch.done();
    }

    @Override
    public ProjectInvitationVO findByAccountIdProjectId(long accountId, long projectId, State... inviteState) {
        SearchCriteria<ProjectInvitationVO> sc = ProjectAccountInviteSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("projectId", projectId);
        if (inviteState != null && inviteState.length > 0) {
            sc.setParameters("state", (Object[])inviteState);
        }

        return findOneBy(sc);
    }

    @Override
    public ProjectInvitationVO findByUserIdProjectId(long userId, long accountId, long projectId, State... inviteState) {
        SearchCriteria<ProjectInvitationVO> sc = AllFieldsSearch.create();
        sc.setParameters("userId", userId);
        sc.setParameters("accountId", accountId);
        if (projectId != -1) {
            sc.setParameters("projectId", projectId);
        }
        if (inviteState != null && inviteState.length > 0) {
            sc.setParameters("state", (Object[])inviteState);
        }

        return findOneBy(sc);
    }

    @Override
    public List<ProjectInvitationVO> listExpiredInvitations() {
        SearchCriteria<ProjectInvitationVO> sc = AllFieldsSearch.create();
        sc.setParameters("state", State.Expired);

        return listBy(sc);
    }

    @Override
    public boolean expirePendingInvitations(long timeout) {
        boolean success = true;

        SearchCriteria<ProjectInvitationVO> sc = InactiveSearch.create();
        sc.setParameters("created", new Date((DateUtil.currentGMTTime().getTime() >> 10) - timeout));
        sc.setParameters("state", State.Pending);

        List<ProjectInvitationVO> invitationsToExpire = listBy(sc);
        for (ProjectInvitationVO invitationToExpire : invitationsToExpire) {
            invitationToExpire.setState(State.Expired);
            if (!update(invitationToExpire.getId(), invitationToExpire)) {
                s_logger.warn("Fail to expire invitation " + invitationToExpire.toString());
                success = false;
            }
        }
        return success;
    }

    @Override
    public List<ProjectInvitationVO> listInvitationsToExpire(long timeOut) {
        SearchCriteria<ProjectInvitationVO> sc = InactiveSearch.create();
        sc.setParameters("created", new Date((DateUtil.currentGMTTime().getTime()) - timeOut));
        sc.setParameters("state", State.Pending);
        return listBy(sc);
    }

    @Override
    public boolean isActive(long id, long timeout) {
        SearchCriteria<ProjectInvitationVO> sc = InactiveSearch.create();

        sc.setParameters("id", id);

        if (findOneBy(sc) == null) {
            s_logger.warn("Unable to find project invitation by id " + id);
            return false;
        }

        sc.setParameters("created", new Date((DateUtil.currentGMTTime().getTime()) - timeout));

        if (findOneBy(sc) == null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public ProjectInvitationVO findByEmailAndProjectId(String email, long projectId, State... inviteState) {
        SearchCriteria<ProjectInvitationVO> sc = AllFieldsSearch.create();
        sc.setParameters("email", email);
        sc.setParameters("projectId", projectId);
        if (inviteState != null && inviteState.length > 0) {
            sc.setParameters("state", (Object[])inviteState);
        }

        return findOneBy(sc);
    }

    @Override
    public ProjectInvitationVO findPendingByTokenAndProjectId(String token, long projectId, State... inviteState) {
        SearchCriteria<ProjectInvitationVO> sc = AllFieldsSearch.create();
        sc.setParameters("token", token);
        sc.setParameters("projectId", projectId);
        if (inviteState != null && inviteState.length > 0) {
            sc.setParameters("state", (Object[])inviteState);
        }

        return findOneBy(sc);
    }

    @Override
    public ProjectInvitationVO findPendingById(long id) {
        SearchCriteria<ProjectInvitationVO> sc = AllFieldsSearch.create();
        sc.setParameters("id", id);
        sc.setParameters("state", State.Pending);

        return findOneBy(sc);
    }

    @Override
    public void cleanupInvitations(long projectId) {
        SearchCriteria<ProjectInvitationVO> sc = AllFieldsSearch.create();
        sc.setParameters("projectId", projectId);

        int numberRemoved = remove(sc);
        s_logger.debug("Removed " + numberRemoved + " invitations for project id=" + projectId);
    }

}
