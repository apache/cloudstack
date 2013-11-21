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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.acl.AclGroup;
import org.apache.cloudstack.acl.AclGroupAccountMapVO;
import org.apache.cloudstack.acl.dao.AclGroupAccountMapDao;
import org.apache.cloudstack.api.response.AclGroupResponse;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.api.query.vo.AclGroupJoinVO;
import com.cloud.user.AccountManager;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value = {AclGroupJoinDao.class})
public class AclGroupJoinDaoImpl extends GenericDaoBase<AclGroupJoinVO, Long> implements AclGroupJoinDao {
    public static final Logger s_logger = Logger.getLogger(AclGroupJoinDaoImpl.class);


    private final SearchBuilder<AclGroupJoinVO> grpIdSearch;
    private final SearchBuilder<AclGroupJoinVO> grpSearch;
    @Inject
    public AccountManager _accountMgr;
    @Inject
    public ConfigurationDao _configDao;
    @Inject
    public AclGroupAccountMapDao _grpAccountDao;

    protected AclGroupJoinDaoImpl() {

        grpSearch = createSearchBuilder();
        grpSearch.and("idIN", grpSearch.entity().getId(), SearchCriteria.Op.IN);
        grpSearch.done();

        grpIdSearch = createSearchBuilder();
        grpIdSearch.and("id", grpIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        grpIdSearch.done();

        _count = "select count(distinct id) from acl_group_view WHERE ";
    }



    @Override
    public AclGroupResponse newAclGroupResponse(AclGroupJoinVO group) {

        AclGroupResponse response = new AclGroupResponse();
        response.setId(group.getUuid());
        response.setName(group.getName());
        response.setDescription(group.getDescription());
        response.setDomainId(group.getDomainUuid());
        response.setDomainName(group.getName());
        response.setAccountName(group.getAccountName());
        if (group.getMemberAccountId() > 0) {
            response.addMemberAccount(group.getMemberAccountName());
        }
        if (group.getPolicyId() > 0) {
            response.addPolicy(group.getPolicyName());
        }

        response.setObjectName("aclgroup");

        return response;
    }

    @Override
    public AclGroupResponse setAclGroupResponse(AclGroupResponse response, AclGroupJoinVO group) {
        if (group.getMemberAccountId() > 0) {
            response.addMemberAccount(group.getMemberAccountName());
        }
        if (group.getPolicyId() > 0) {
            response.addPolicy(group.getPolicyName());
        }

        return response;
    }

    @Override
    public List<AclGroupJoinVO> newAclGroupView(AclGroup group) {
        SearchCriteria<AclGroupJoinVO> sc = grpIdSearch.create();
        sc.setParameters("id", group.getId());
        return searchIncludingRemoved(sc, null, null, false);

    }

    @Override
    public List<AclGroupJoinVO> searchByIds(Long... grpIds) {
        // set detail batch query size
        int DETAILS_BATCH_SIZE = 2000;
        String batchCfg = _configDao.getValue("detail.batch.query.size");
        if (batchCfg != null) {
            DETAILS_BATCH_SIZE = Integer.parseInt(batchCfg);
        }
        // query details by batches
        List<AclGroupJoinVO> uvList = new ArrayList<AclGroupJoinVO>();
        // query details by batches
        int curr_index = 0;
        if (grpIds.length > DETAILS_BATCH_SIZE) {
            while ((curr_index + DETAILS_BATCH_SIZE) <= grpIds.length) {
                Long[] ids = new Long[DETAILS_BATCH_SIZE];
                for (int k = 0, j = curr_index; j < curr_index + DETAILS_BATCH_SIZE; j++, k++) {
                    ids[k] = grpIds[j];
                }
                SearchCriteria<AclGroupJoinVO> sc = grpSearch.create();
                sc.setParameters("idIN", ids);
                List<AclGroupJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
                if (vms != null) {
                    uvList.addAll(vms);
                }
                curr_index += DETAILS_BATCH_SIZE;
            }
        }
        if (curr_index < grpIds.length) {
            int batch_size = (grpIds.length - curr_index);
            // set the ids value
            Long[] ids = new Long[batch_size];
            for (int k = 0, j = curr_index; j < curr_index + batch_size; j++, k++) {
                ids[k] = grpIds[j];
            }
            SearchCriteria<AclGroupJoinVO> sc = grpSearch.create();
            sc.setParameters("idIN", ids);
            List<AclGroupJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
            if (vms != null) {
                uvList.addAll(vms);
            }
        }
        return uvList;
    }

    @Override
    public List<AclGroupJoinVO> findAclGroupsByAccount(long accountId) {
        List<AclGroupAccountMapVO> grpMap = _grpAccountDao.listByAccountId(accountId);
        if (grpMap != null && grpMap.size() > 0) {
            Set<Long> grpList = new HashSet<Long>();
            for (AclGroupAccountMapVO m : grpMap) {
                grpList.add(m.getAclGroupId());
            }
            return searchByIds(grpList.toArray(new Long[grpList.size()]));
        }
        else{
            return null;
        }
    }

}
