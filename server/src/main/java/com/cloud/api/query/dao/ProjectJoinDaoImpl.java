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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.vo.AccountJoinVO;
import com.cloud.api.query.vo.ProjectJoinVO;
import com.cloud.api.query.vo.ResourceTagJoinVO;
import com.cloud.projects.Project;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class ProjectJoinDaoImpl extends GenericDaoBase<ProjectJoinVO, Long> implements ProjectJoinDao {
    public static final Logger s_logger = Logger.getLogger(ProjectJoinDaoImpl.class);

    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private AccountJoinDao _accountJoinDao;
    @Inject
    private AccountDao _accountDao;

    private final SearchBuilder<ProjectJoinVO> prjSearch;

    private final SearchBuilder<ProjectJoinVO> prjIdSearch;

    protected ProjectJoinDaoImpl() {

        prjSearch = createSearchBuilder();
        prjSearch.and("idIN", prjSearch.entity().getId(), SearchCriteria.Op.IN);
        prjSearch.done();

        prjIdSearch = createSearchBuilder();
        prjIdSearch.and("id", prjIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        prjIdSearch.done();

        this._count = "select count(distinct id) from project_view WHERE ";
    }

    @Override
    public ProjectResponse newProjectResponse(ProjectJoinVO proj) {
        ProjectResponse response = new ProjectResponse();
        response.setId(proj.getUuid());
        response.setName(proj.getName());
        response.setDisplaytext(proj.getDisplayText());
        if (proj.getState() != null) {
            response.setState(proj.getState().toString());
        }
        response.setDomainId(proj.getDomainUuid());
        response.setDomain(proj.getDomainName());

        response.setOwner(proj.getOwner());

        // update tag information
        List<ResourceTagJoinVO> tags = ApiDBUtils.listResourceTagViewByResourceUUID(proj.getUuid(), ResourceObjectType.Project);
        for (ResourceTagJoinVO vtag : tags) {
            response.addTag(ApiDBUtils.newResourceTagResponse(vtag, false));
        }

        //set resource limit/count information for the project (by getting the info of the project's account)
        Account account = _accountDao.findByIdIncludingRemoved(proj.getProjectAccountId());
        AccountJoinVO accountJn = ApiDBUtils.newAccountView(account);
        _accountJoinDao.setResourceLimits(accountJn, false, response);
        response.setProjectAccountName(accountJn.getAccountName());

        response.setObjectName("project");
        return response;
    }

    @Override
    public ProjectResponse setProjectResponse(ProjectResponse rsp, ProjectJoinVO proj) {
        return rsp;
    }

    @Override
    public List<ProjectJoinVO> newProjectView(Project proj) {
        SearchCriteria<ProjectJoinVO> sc = prjIdSearch.create();
        sc.setParameters("id", proj.getId());
        return searchIncludingRemoved(sc, null, null, false);
    }

    @Override
    public List<ProjectJoinVO> searchByIds(Long... prjIds) {
        // set detail batch query size
        int DETAILS_BATCH_SIZE = 2000;
        String batchCfg = _configDao.getValue("detail.batch.query.size");
        if (batchCfg != null) {
            DETAILS_BATCH_SIZE = Integer.parseInt(batchCfg);
        }
        // query details by batches
        List<ProjectJoinVO> uvList = new ArrayList<ProjectJoinVO>();
        // query details by batches
        int curr_index = 0;
        if (prjIds.length > DETAILS_BATCH_SIZE) {
            while ((curr_index + DETAILS_BATCH_SIZE) <= prjIds.length) {
                Long[] ids = new Long[DETAILS_BATCH_SIZE];
                for (int k = 0, j = curr_index; j < curr_index + DETAILS_BATCH_SIZE; j++, k++) {
                    ids[k] = prjIds[j];
                }
                SearchCriteria<ProjectJoinVO> sc = prjSearch.create();
                sc.setParameters("idIN", ids);
                List<ProjectJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
                if (vms != null) {
                    uvList.addAll(vms);
                }
                curr_index += DETAILS_BATCH_SIZE;
            }
        }
        if (curr_index < prjIds.length) {
            int batch_size = (prjIds.length - curr_index);
            // set the ids value
            Long[] ids = new Long[batch_size];
            for (int k = 0, j = curr_index; j < curr_index + batch_size; j++, k++) {
                ids[k] = prjIds[j];
            }
            SearchCriteria<ProjectJoinVO> sc = prjSearch.create();
            sc.setParameters("idIN", ids);
            List<ProjectJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
            if (vms != null) {
                uvList.addAll(vms);
            }
        }
        return uvList;
    }

}
