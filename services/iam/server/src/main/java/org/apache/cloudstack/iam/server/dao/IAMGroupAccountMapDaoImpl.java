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
package org.apache.cloudstack.iam.server.dao;

import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.iam.server.IAMGroupAccountMapVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;


import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class IAMGroupAccountMapDaoImpl extends GenericDaoBase<IAMGroupAccountMapVO, Long> implements IAMGroupAccountMapDao {
    private SearchBuilder<IAMGroupAccountMapVO> ListByGroupId;
    private SearchBuilder<IAMGroupAccountMapVO> ListByAccountId;
    private SearchBuilder<IAMGroupAccountMapVO> _findByAccountAndGroupId;

    public static final Logger s_logger = Logger.getLogger(IAMGroupAccountMapDaoImpl.class.getName());

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        ListByGroupId = createSearchBuilder();
        ListByGroupId.and("groupId", ListByGroupId.entity().getAclGroupId(), SearchCriteria.Op.EQ);
        ListByGroupId.done();

        ListByAccountId = createSearchBuilder();
        ListByAccountId.and("accountId", ListByAccountId.entity().getAccountId(), SearchCriteria.Op.EQ);
        ListByAccountId.done();

        _findByAccountAndGroupId = createSearchBuilder();
        _findByAccountAndGroupId
                .and("groupId", _findByAccountAndGroupId.entity().getAclGroupId(), SearchCriteria.Op.EQ);
        _findByAccountAndGroupId.and("accountId", _findByAccountAndGroupId.entity().getAccountId(),
                SearchCriteria.Op.EQ);
        _findByAccountAndGroupId.done();

        return true;
    }

    @Override
    public List<IAMGroupAccountMapVO> listByGroupId(long groupId) {
        SearchCriteria<IAMGroupAccountMapVO> sc = ListByGroupId.create();
        sc.setParameters("groupId", groupId);
        return listBy(sc);
    }

    @Override
    public List<IAMGroupAccountMapVO> listByAccountId(long accountId) {
        SearchCriteria<IAMGroupAccountMapVO> sc = ListByAccountId.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
    }

    @Override
    public IAMGroupAccountMapVO findAccountInAdminGroup(long accountId) {
        SearchCriteria<IAMGroupAccountMapVO> sc = _findByAccountAndGroupId.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("groupId", 2);
        return findOneBy(sc);
    }

    @Override
    public IAMGroupAccountMapVO findAccountInDomainAdminGroup(long accountId) {
        SearchCriteria<IAMGroupAccountMapVO> sc = _findByAccountAndGroupId.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("groupId", 3);
        return findOneBy(sc);
    }

    @Override
    public IAMGroupAccountMapVO findAccountInUserGroup(long accountId) {
        SearchCriteria<IAMGroupAccountMapVO> sc = _findByAccountAndGroupId.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("groupId", 1);
        return findOneBy(sc);
    }

    @Override
    public IAMGroupAccountMapVO findByGroupAndAccount(long groupId, long acctId) {
        SearchCriteria<IAMGroupAccountMapVO> sc = _findByAccountAndGroupId.create();
        sc.setParameters("accountId", acctId);
        sc.setParameters("groupId", groupId);
        return findOneBy(sc);
    }

    @Override
    public void removeAccountFromGroups(long accountId) {
        SearchCriteria<IAMGroupAccountMapVO> sc = ListByAccountId.create();
        sc.setParameters("accountId", accountId);

        int rowsRemoved = remove(sc);
        if (rowsRemoved > 0) {
            s_logger.debug("Removed account id=" + accountId + " from " + rowsRemoved + " groups");
        }
    }
}
