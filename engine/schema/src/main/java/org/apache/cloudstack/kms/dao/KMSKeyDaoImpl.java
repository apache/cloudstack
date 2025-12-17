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

package org.apache.cloudstack.kms.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.framework.kms.KeyPurpose;
import org.apache.cloudstack.kms.KMSKey;
import org.apache.cloudstack.kms.KMSKeyVO;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

/**
 * Implementation of KMSKeyDao
 */
@Component
public class KMSKeyDaoImpl extends GenericDaoBase<KMSKeyVO, Long> implements KMSKeyDao {

    private final SearchBuilder<KMSKeyVO> uuidSearch;
    private final SearchBuilder<KMSKeyVO> kekLabelSearch;
    private final SearchBuilder<KMSKeyVO> accountSearch;
    private final SearchBuilder<KMSKeyVO> domainSearch;
    private final SearchBuilder<KMSKeyVO> zoneSearch;
    private final SearchBuilder<KMSKeyVO> accessibleSearch;

    @Inject
    private KMSWrappedKeyDao kmsWrappedKeyDao;

    public KMSKeyDaoImpl() {
        super();

        // Search by UUID
        uuidSearch = createSearchBuilder();
        uuidSearch.and("uuid", uuidSearch.entity().getUuid(), SearchCriteria.Op.EQ);
        uuidSearch.and("removed", uuidSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        uuidSearch.done();

        // Search by KEK label and provider
        kekLabelSearch = createSearchBuilder();
        kekLabelSearch.and("kekLabel", kekLabelSearch.entity().getKekLabel(), SearchCriteria.Op.EQ);
        kekLabelSearch.and("providerName", kekLabelSearch.entity().getProviderName(), SearchCriteria.Op.EQ);
        kekLabelSearch.and("removed", kekLabelSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        kekLabelSearch.done();

        // Search by account
        accountSearch = createSearchBuilder();
        accountSearch.and("accountId", accountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        accountSearch.and("purpose", accountSearch.entity().getPurpose(), SearchCriteria.Op.EQ);
        accountSearch.and("state", accountSearch.entity().getState(), SearchCriteria.Op.EQ);
        accountSearch.and("removed", accountSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        accountSearch.done();

        // Search by domain
        domainSearch = createSearchBuilder();
        domainSearch.and("domainId", domainSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        domainSearch.and("purpose", domainSearch.entity().getPurpose(), SearchCriteria.Op.EQ);
        domainSearch.and("state", domainSearch.entity().getState(), SearchCriteria.Op.EQ);
        domainSearch.and("removed", domainSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        domainSearch.done();

        // Search by zone
        zoneSearch = createSearchBuilder();
        zoneSearch.and("zoneId", zoneSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        zoneSearch.and("purpose", zoneSearch.entity().getPurpose(), SearchCriteria.Op.EQ);
        zoneSearch.and("state", zoneSearch.entity().getState(), SearchCriteria.Op.EQ);
        zoneSearch.and("removed", zoneSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        zoneSearch.done();

        // Search for accessible keys (by account or domain)
        accessibleSearch = createSearchBuilder();
        accessibleSearch.and("accountId", accessibleSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        accessibleSearch.and("domainId", accessibleSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        accessibleSearch.and("zoneId", accessibleSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        accessibleSearch.and("purpose", accessibleSearch.entity().getPurpose(), SearchCriteria.Op.EQ);
        accessibleSearch.and("state", accessibleSearch.entity().getState(), SearchCriteria.Op.EQ);
        accessibleSearch.and("removed", accessibleSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        accessibleSearch.done();
    }

    @Override
    public KMSKeyVO findByUuid(String uuid) {
        SearchCriteria<KMSKeyVO> sc = uuidSearch.create();
        sc.setParameters("uuid", uuid);
        return findOneBy(sc);
    }

    @Override
    public KMSKeyVO findByKekLabel(String kekLabel, String providerName) {
        SearchCriteria<KMSKeyVO> sc = kekLabelSearch.create();
        sc.setParameters("kekLabel", kekLabel);
        sc.setParameters("providerName", providerName);
        return findOneBy(sc);
    }

    @Override
    public List<KMSKeyVO> listByAccount(Long accountId, KeyPurpose purpose, KMSKey.State state) {
        SearchCriteria<KMSKeyVO> sc = accountSearch.create();
        sc.setParameters("accountId", accountId);
        if (purpose != null) {
            sc.setParameters("purpose", purpose);
        }
        if (state != null) {
            sc.setParameters("state", state);
        }
        return listBy(sc);
    }

    @Override
    public List<KMSKeyVO> listByDomain(Long domainId, KeyPurpose purpose, KMSKey.State state, boolean includeSubdomains) {
        SearchCriteria<KMSKeyVO> sc = domainSearch.create();
        sc.setParameters("domainId", domainId);
        if (purpose != null) {
            sc.setParameters("purpose", purpose);
        }
        if (state != null) {
            sc.setParameters("state", state);
        }
        // TODO: Implement subdomain traversal if includeSubdomains is true
        // For now, just return keys in this domain
        return listBy(sc);
    }

    @Override
    public List<KMSKeyVO> listByZone(Long zoneId, KeyPurpose purpose, KMSKey.State state) {
        SearchCriteria<KMSKeyVO> sc = zoneSearch.create();
        sc.setParameters("zoneId", zoneId);
        if (purpose != null) {
            sc.setParameters("purpose", purpose);
        }
        if (state != null) {
            sc.setParameters("state", state);
        }
        return listBy(sc);
    }

    @Override
    public List<KMSKeyVO> listAccessibleKeys(Long accountId, Long domainId, Long zoneId, KeyPurpose purpose, KMSKey.State state) {
        SearchCriteria<KMSKeyVO> sc = accessibleSearch.create();
        // Keys owned by the account or in the domain
        sc.setParameters("accountId", accountId);
        if (zoneId != null) {
            sc.setParameters("zoneId", zoneId);
        }
        if (purpose != null) {
            sc.setParameters("purpose", purpose);
        }
        if (state != null) {
            sc.setParameters("state", state);
        }
        return listBy(sc);
    }

    @Override
    public long countWrappedKeysByKmsKey(Long kmsKeyId) {
        if (kmsKeyId == null) {
            return 0;
        }
        // Delegate to KMSWrappedKeyDao
        return kmsWrappedKeyDao.countByKmsKeyId(kmsKeyId);
    }

    @Override
    public long countByKekLabel(String kekLabel, String providerName) {
        SearchCriteria<KMSKeyVO> sc = kekLabelSearch.create();
        sc.setParameters("kekLabel", kekLabel);
        sc.setParameters("providerName", providerName);
        Integer count = getCount(sc);
        return count != null ? count.longValue() : 0L;
    }
}

