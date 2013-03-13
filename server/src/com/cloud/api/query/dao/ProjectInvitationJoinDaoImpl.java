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
package com.cloud.api.query.dao;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.response.ProjectInvitationResponse;
import org.springframework.stereotype.Component;

import com.cloud.api.query.vo.ProjectInvitationJoinVO;
import com.cloud.projects.ProjectInvitation;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value={ProjectInvitationJoinDao.class})
public class ProjectInvitationJoinDaoImpl extends GenericDaoBase<ProjectInvitationJoinVO, Long> implements ProjectInvitationJoinDao {
    public static final Logger s_logger = Logger.getLogger(ProjectInvitationJoinDaoImpl.class);


    private SearchBuilder<ProjectInvitationJoinVO> piIdSearch;

    protected ProjectInvitationJoinDaoImpl() {

        piIdSearch = createSearchBuilder();
        piIdSearch.and("id", piIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        piIdSearch.done();

        this._count = "select count(distinct id) from project_invitation_view WHERE ";
    }



    @Override
    public ProjectInvitationResponse newProjectInvitationResponse(ProjectInvitationJoinVO invite) {
        ProjectInvitationResponse response = new ProjectInvitationResponse();
        response.setId(invite.getUuid());
        response.setProjectId(invite.getProjectUuid());
        response.setProjectName(invite.getProjectName());
        if (invite.getState() != null) {
            response.setInvitationState(invite.getState().toString());
        }

        if (invite.getAccountName() != null) {
            response.setAccountName(invite.getAccountName());
        } else {
            response.setEmail(invite.getEmail());
        }

        response.setDomainId(invite.getDomainUuid());
        response.setDomainName(invite.getDomainName());

        response.setObjectName("projectinvitation");
        return response;
    }



    @Override
    public ProjectInvitationJoinVO newProjectInvitationView(ProjectInvitation proj) {
        SearchCriteria<ProjectInvitationJoinVO> sc = piIdSearch.create();
        sc.setParameters("id", proj.getId());
        List<ProjectInvitationJoinVO> grps = searchIncludingRemoved(sc, null, null, false);
        assert grps != null && grps.size() == 1 : "No project invitation found for id  " + proj.getId();
        return grps.get(0);
    }


}
