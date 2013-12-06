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

import org.apache.cloudstack.acl.AclPolicy;
import org.apache.cloudstack.api.response.AclPermissionResponse;
import org.apache.cloudstack.api.response.AclPolicyResponse;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.api.query.vo.AclPolicyJoinVO;
import com.cloud.user.AccountManager;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value = {AclPolicyJoinDao.class})
public class AclPolicyJoinDaoImpl extends GenericDaoBase<AclPolicyJoinVO, Long> implements AclPolicyJoinDao {
    public static final Logger s_logger = Logger.getLogger(AclPolicyJoinDaoImpl.class);


    private final SearchBuilder<AclPolicyJoinVO> policyIdSearch;
    private final SearchBuilder<AclPolicyJoinVO> policySearch;
    @Inject
    public AccountManager _accountMgr;
    @Inject
    public ConfigurationDao _configDao;

    protected AclPolicyJoinDaoImpl() {

        policySearch = createSearchBuilder();
        policySearch.and("idIN", policySearch.entity().getId(), SearchCriteria.Op.IN);
        policySearch.done();

        policyIdSearch = createSearchBuilder();
        policyIdSearch.and("id", policyIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        policyIdSearch.done();

        _count = "select count(distinct id) from acl_policy_view WHERE ";
    }



    @Override
    public AclPolicyResponse newAclPolicyResponse(AclPolicyJoinVO policy) {

        AclPolicyResponse response = new AclPolicyResponse();
        response.setId(policy.getUuid());
        response.setName(policy.getName());
        response.setDescription(policy.getDescription());
        response.setDomainId(policy.getDomainUuid());
        response.setDomainName(policy.getName());
        response.setAccountName(policy.getAccountName());
        if (policy.getPermissionAction() != null) {
            AclPermissionResponse perm = new AclPermissionResponse();
            perm.setAction(policy.getPermissionAction());
            perm.setEntityType(policy.getPermissionEntityType());
            perm.setScope(policy.getPermissionScope());
            perm.setScopeId(policy.getPermissionScopeId());
            perm.setPermission(policy.getPermissionAllowDeny());
            response.addPermission(perm);
        }

        response.setObjectName("aclpolicy");
        return response;
    }

    @Override
    public AclPolicyResponse setAclPolicyResponse(AclPolicyResponse response, AclPolicyJoinVO policy) {
        if (policy.getPermissionAction() != null) {
            AclPermissionResponse perm = new AclPermissionResponse();
            perm.setAction(policy.getPermissionAction());
            perm.setEntityType(policy.getPermissionEntityType());
            perm.setScope(policy.getPermissionScope());
            perm.setScopeId(policy.getPermissionScopeId());
            perm.setPermission(policy.getPermissionAllowDeny());
            response.addPermission(perm);
        }
        return response;
    }

    @Override
    public List<AclPolicyJoinVO> newAclPolicyView(AclPolicy policy) {
        SearchCriteria<AclPolicyJoinVO> sc = policyIdSearch.create();
        sc.setParameters("id", policy.getId());
        return searchIncludingRemoved(sc, null, null, false);

    }

    @Override
    public List<AclPolicyJoinVO> searchByIds(Long... policyIds) {
        // set detail batch query size
        int DETAILS_BATCH_SIZE = 2000;
        String batchCfg = _configDao.getValue("detail.batch.query.size");
        if (batchCfg != null) {
            DETAILS_BATCH_SIZE = Integer.parseInt(batchCfg);
        }
        // query details by batches
        List<AclPolicyJoinVO> uvList = new ArrayList<AclPolicyJoinVO>();
        // query details by batches
        int curr_index = 0;
        if (policyIds.length > DETAILS_BATCH_SIZE) {
            while ((curr_index + DETAILS_BATCH_SIZE) <= policyIds.length) {
                Long[] ids = new Long[DETAILS_BATCH_SIZE];
                for (int k = 0, j = curr_index; j < curr_index + DETAILS_BATCH_SIZE; j++, k++) {
                    ids[k] = policyIds[j];
                }
                SearchCriteria<AclPolicyJoinVO> sc = policySearch.create();
                sc.setParameters("idIN", ids);
                List<AclPolicyJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
                if (vms != null) {
                    uvList.addAll(vms);
                }
                curr_index += DETAILS_BATCH_SIZE;
            }
        }
        if (curr_index < policyIds.length) {
            int batch_size = (policyIds.length - curr_index);
            // set the ids value
            Long[] ids = new Long[batch_size];
            for (int k = 0, j = curr_index; j < curr_index + batch_size; j++, k++) {
                ids[k] = policyIds[j];
            }
            SearchCriteria<AclPolicyJoinVO> sc = policySearch.create();
            sc.setParameters("idIN", ids);
            List<AclPolicyJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
            if (vms != null) {
                uvList.addAll(vms);
            }
        }
        return uvList;
    }

}
