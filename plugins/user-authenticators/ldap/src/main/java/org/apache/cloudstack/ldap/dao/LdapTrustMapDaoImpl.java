/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.ldap.dao;


import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.ldap.LdapTrustMapVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;

import java.util.List;

@Component
public class LdapTrustMapDaoImpl extends GenericDaoBase<LdapTrustMapVO, Long> implements LdapTrustMapDao  {
    private final SearchBuilder<LdapTrustMapVO> domainIdSearch;
    private final SearchBuilder<LdapTrustMapVO> groupSearch;

    public LdapTrustMapDaoImpl() {
        super();
        domainIdSearch = createSearchBuilder();
        domainIdSearch.and("domainId", domainIdSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        domainIdSearch.and("account_id", domainIdSearch.entity().getAccountId(),SearchCriteria.Op.EQ);
        domainIdSearch.done();
        groupSearch = createSearchBuilder();
        groupSearch.and("domainId", groupSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        groupSearch.and("name", groupSearch.entity().getName(),SearchCriteria.Op.EQ);
        groupSearch.done();
    }

    @Override
    public LdapTrustMapVO findByDomainId(long domainId) {
        final SearchCriteria<LdapTrustMapVO> sc = domainIdSearch.create();
        sc.setParameters("domainId", domainId);
        sc.setParameters("account_id", 0);
        return findOneBy(sc);
    }

    @Override
    public LdapTrustMapVO findByAccount(long domainId, Long accountId) {
        final SearchCriteria<LdapTrustMapVO> sc = domainIdSearch.create();
        sc.setParameters("domainId", domainId);
        sc.setParameters("account_id", accountId);
        return findOneBy(sc);
    }

    @Override
    public LdapTrustMapVO findGroupInDomain(long domainId, String group){
        final SearchCriteria<LdapTrustMapVO> sc = groupSearch.create();
        sc.setParameters("domainId", domainId);
        sc.setParameters("name", group);
        return findOneBy(sc);

    }

    @Override
    public List<LdapTrustMapVO> searchByDomainId(long domainId) {
        final SearchCriteria<LdapTrustMapVO> sc = domainIdSearch.create();
        sc.setParameters("domainId", domainId);
        return search(sc,null);
    }

}
