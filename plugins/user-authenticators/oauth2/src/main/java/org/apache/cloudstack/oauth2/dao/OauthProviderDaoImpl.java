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

package org.apache.cloudstack.oauth2.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;

import java.util.List;
import java.util.Objects;

public class OauthProviderDaoImpl extends GenericDaoBase<OauthProviderVO, Long> implements OauthProviderDao {

    private final SearchBuilder<OauthProviderVO> oauthProviderSearchByProviderAndDomain;

    public OauthProviderDaoImpl() {
        super();

        oauthProviderSearchByProviderAndDomain = createSearchBuilder();
        oauthProviderSearchByProviderAndDomain.and("provider", oauthProviderSearchByProviderAndDomain.entity().getProvider(), SearchCriteria.Op.EQ);
        oauthProviderSearchByProviderAndDomain.and("domainId", oauthProviderSearchByProviderAndDomain.entity().getDomainId(), SearchCriteria.Op.EQ);
        oauthProviderSearchByProviderAndDomain.done();
    }

    @Override
    public OauthProviderVO findByProviderAndDomain(String provider, Long domainId) {
        SearchCriteria<OauthProviderVO> sc = oauthProviderSearchByProviderAndDomain.create();
        sc.setParameters("provider", provider);
        sc.setParameters("domainId", domainId);
        return findOneBy(sc);
    }

    @Override
    public List<OauthProviderVO> listByDomainIncludingGlobal(Long domainId) {
        SearchCriteria<OauthProviderVO> sc = createSearchCriteria();
        sc.addOr("domainId", SearchCriteria.Op.EQ, domainId);
        sc.addOr("domainId", SearchCriteria.Op.NULL);
        return listBy(sc);
    }

    @Override
    public OauthProviderVO findByProviderAndDomainWithGlobalFallback(String provider, Long domainId) {
        OauthProviderVO providerVO = null;
        if (Objects.nonNull(domainId)) {
            providerVO = findByProviderAndDomain(provider, domainId);
        }
        if (Objects.isNull(providerVO)) {
            providerVO = findByProviderAndDomain(provider, null);
        }
        return providerVO;
    }
}
