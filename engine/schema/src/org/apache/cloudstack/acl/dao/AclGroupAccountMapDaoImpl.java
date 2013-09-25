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
import java.util.Map;

import javax.naming.ConfigurationException;

import org.springframework.stereotype.Component;

import org.apache.cloudstack.acl.AclGroupAccountMapVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class AclGroupAccountMapDaoImpl extends GenericDaoBase<AclGroupAccountMapVO, Long> implements AclGroupAccountMapDao {
    private SearchBuilder<AclGroupAccountMapVO> ListByGroupId;
    private SearchBuilder<AclGroupAccountMapVO> ListByAccountId;
    private SearchBuilder<AclGroupAccountMapVO> _findByAccountAndGroupId;

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
    public List<AclGroupAccountMapVO> listByGroupId(long groupId) {
        SearchCriteria<AclGroupAccountMapVO> sc = ListByGroupId.create();
        sc.setParameters("groupId", groupId);
        return listBy(sc);
    }

    @Override
    public List<AclGroupAccountMapVO> listByAccountId(long accountId) {
        SearchCriteria<AclGroupAccountMapVO> sc = ListByAccountId.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
    }

    @Override
    public AclGroupAccountMapVO findAccountInAdminGroup(long accountId) {
        SearchCriteria<AclGroupAccountMapVO> sc = _findByAccountAndGroupId.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("groupId", 2);
        return findOneBy(sc);
    }

}
