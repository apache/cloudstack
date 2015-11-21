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

@Component
public class LdapTrustMapDaoImpl extends GenericDaoBase<LdapTrustMapVO, Long> implements LdapTrustMapDao  {
    private final SearchBuilder<LdapTrustMapVO> domainIdSearch;

    public LdapTrustMapDaoImpl() {
        super();
        domainIdSearch = createSearchBuilder();
        domainIdSearch.and("domainId", domainIdSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        domainIdSearch.done();
    }

    @Override
    public LdapTrustMapVO findByDomainId(long domainId) {
        final SearchCriteria<LdapTrustMapVO> sc = domainIdSearch.create();
        sc.setParameters("domainId", domainId);
        return findOneBy(sc);
    }
}
