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

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.view.vo.ProjectJoinVO;
import org.apache.cloudstack.api.view.vo.ResourceTagJoinVO;
import com.cloud.projects.Project;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;


@Local(value={ProjectJoinDao.class})
public class ProjectJoinDaoImpl extends GenericDaoBase<ProjectJoinVO, Long> implements ProjectJoinDao {
    public static final Logger s_logger = Logger.getLogger(ProjectJoinDaoImpl.class);

    private SearchBuilder<ProjectJoinVO> vrSearch;

    private SearchBuilder<ProjectJoinVO> vrIdSearch;


    protected ProjectJoinDaoImpl() {

        vrSearch = createSearchBuilder();
        vrSearch.and("idIN", vrSearch.entity().getId(), SearchCriteria.Op.IN);
        vrSearch.done();

        vrIdSearch = createSearchBuilder();
        vrIdSearch.and("id", vrIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        vrIdSearch.done();

        this._count = "select count(distinct id) from project_view WHERE ";
    }






    @Override
    public ProjectResponse newProjectResponse(ProjectJoinVO proj) {
        ProjectResponse response = new ProjectResponse();
        response.setId(proj.getUuid());
        response.setName(proj.getName());
        response.setDisplaytext(proj.getDisplayText());
        response.setState(proj.getState().toString());

        response.setDomainId(proj.getDomainUuid());
        response.setDomain(proj.getDomainName());

        response.setOwner(proj.getOwner());

        // update tag information
        Long tag_id = proj.getTagId();
        if (tag_id != null && tag_id.longValue() > 0) {
            ResourceTagJoinVO vtag = ApiDBUtils.findResourceTagViewById(tag_id);
            if ( vtag != null ){
                response.addTag(ApiDBUtils.newResourceTagResponse(vtag, false));
            }
        }

        response.setObjectName("project");
        return response;
    }



    @Override
    public ProjectResponse setProjectResponse(ProjectResponse rsp, ProjectJoinVO proj) {
        // update tag information
        Long tag_id = proj.getTagId();
        if (tag_id != null && tag_id.longValue() > 0) {
            ResourceTagJoinVO vtag = ApiDBUtils.findResourceTagViewById(tag_id);
            if ( vtag != null ){
                rsp.addTag(ApiDBUtils.newResourceTagResponse(vtag, false));
            }
        }
        return rsp;
    }






    @Override
    public List<ProjectJoinVO> newProjectView(Project proj) {
        SearchCriteria<ProjectJoinVO> sc = vrIdSearch.create();
        sc.setParameters("id", proj.getId());
        return searchIncludingRemoved(sc, null, null, false);
    }






    @Override
    public List<ProjectJoinVO> searchByIds(Long... ids) {
        SearchCriteria<ProjectJoinVO> sc = vrSearch.create();
        sc.setParameters("idIN", ids);
        return searchIncludingRemoved(sc, null, null, false);
    }




}
