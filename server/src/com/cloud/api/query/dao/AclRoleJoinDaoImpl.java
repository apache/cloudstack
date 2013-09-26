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

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.acl.AclRole;
import org.apache.cloudstack.api.response.AclRoleResponse;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.api.query.vo.AclRoleJoinVO;
import com.cloud.user.AccountManager;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value = {AclRoleJoinDao.class})
public class AclRoleJoinDaoImpl extends GenericDaoBase<AclRoleJoinVO, Long> implements AclRoleJoinDao {
    public static final Logger s_logger = Logger.getLogger(AclRoleJoinDaoImpl.class);


    private final SearchBuilder<AclRoleJoinVO> roleIdSearch;
    private final SearchBuilder<AclRoleJoinVO> roleSearch;
    @Inject
    public AccountManager _accountMgr;
    @Inject
    public ConfigurationDao _configDao;

    protected AclRoleJoinDaoImpl() {

        roleSearch = createSearchBuilder();
        roleSearch.and("idIN", roleSearch.entity().getId(), SearchCriteria.Op.IN);
        roleSearch.done();

        roleIdSearch = createSearchBuilder();
        roleIdSearch.and("id", roleIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        roleIdSearch.done();

        _count = "select count(distinct id) from acl_role_view WHERE ";
    }



    @Override
    public AclRoleResponse newAclRoleResponse(AclRoleJoinVO role) {

        AclRoleResponse response = new AclRoleResponse();
        response.setId(role.getUuid());
        response.setName(role.getName());
        response.setDescription(role.getDescription());
        response.setParentRoleId(role.getParentRoleUuid());
        response.setParentRoleName(role.getParentRoleName());
        response.setDomainId(role.getDomainUuid());
        response.setDomainName(role.getName());
        if (role.getApiName() != null) {
            response.addApi(role.getApiName());
        }

        response.setObjectName("aclrole");
        

        return response;
    }

    @Override
    public AclRoleResponse setAclRoleResponse(AclRoleResponse response, AclRoleJoinVO role) {
        if (role.getApiName() != null) {
            response.addApi(role.getApiName());
        }
        return response;
    }

    @Override
    public List<AclRoleJoinVO> newAclRoleView(AclRole role) {
        SearchCriteria<AclRoleJoinVO> sc = roleIdSearch.create();
        sc.setParameters("id", role.getId());
        return searchIncludingRemoved(sc, null, null, false);

    }

    @Override
    public List<AclRoleJoinVO> searchByIds(Long... roleIds) {
        // set detail batch query size
        int DETAILS_BATCH_SIZE = 2000;
        String batchCfg = _configDao.getValue("detail.batch.query.size");
        if (batchCfg != null) {
            DETAILS_BATCH_SIZE = Integer.parseInt(batchCfg);
        }
        // query details by batches
        List<AclRoleJoinVO> uvList = new ArrayList<AclRoleJoinVO>();
        // query details by batches
        int curr_index = 0;
        if (roleIds.length > DETAILS_BATCH_SIZE) {
            while ((curr_index + DETAILS_BATCH_SIZE) <= roleIds.length) {
                Long[] ids = new Long[DETAILS_BATCH_SIZE];
                for (int k = 0, j = curr_index; j < curr_index + DETAILS_BATCH_SIZE; j++, k++) {
                    ids[k] = roleIds[j];
                }
                SearchCriteria<AclRoleJoinVO> sc = roleSearch.create();
                sc.setParameters("idIN", ids);
                List<AclRoleJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
                if (vms != null) {
                    uvList.addAll(vms);
                }
                curr_index += DETAILS_BATCH_SIZE;
            }
        }
        if (curr_index < roleIds.length) {
            int batch_size = (roleIds.length - curr_index);
            // set the ids value
            Long[] ids = new Long[batch_size];
            for (int k = 0, j = curr_index; j < curr_index + batch_size; j++, k++) {
                ids[k] = roleIds[j];
            }
            SearchCriteria<AclRoleJoinVO> sc = roleSearch.create();
            sc.setParameters("idIN", ids);
            List<AclRoleJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
            if (vms != null) {
                uvList.addAll(vms);
            }
        }
        return uvList;
    }

}
