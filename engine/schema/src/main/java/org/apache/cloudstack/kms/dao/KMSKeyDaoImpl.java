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

@Component
public class KMSKeyDaoImpl extends GenericDaoBase<KMSKeyVO, Long> implements KMSKeyDao {

    private final SearchBuilder<KMSKeyVO> allFieldSearch;

    @Inject
    private KMSWrappedKeyDao kmsWrappedKeyDao;

    public KMSKeyDaoImpl() {
        allFieldSearch = createSearchBuilder();
        allFieldSearch.and("kekLabel", allFieldSearch.entity().getKekLabel(), SearchCriteria.Op.EQ);
        allFieldSearch.and("providerName", allFieldSearch.entity().getProviderName(), SearchCriteria.Op.EQ);
        allFieldSearch.and("domainId", allFieldSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        allFieldSearch.and("accountId", allFieldSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        allFieldSearch.and("purpose", allFieldSearch.entity().getPurpose(), SearchCriteria.Op.EQ);
        allFieldSearch.and("state", allFieldSearch.entity().getState(), SearchCriteria.Op.EQ);
        allFieldSearch.and("zoneId", allFieldSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        allFieldSearch.done();
    }

    @Override
    public KMSKeyVO findByKekLabel(String kekLabel, String providerName) {
        SearchCriteria<KMSKeyVO> sc = allFieldSearch.create();
        sc.setParameters("kekLabel", kekLabel);
        sc.setParameters("providerName", providerName);
        return findOneBy(sc);
    }

    @Override
    public List<KMSKeyVO> listByAccount(Long accountId, KeyPurpose purpose, KMSKey.State state) {
        SearchCriteria<KMSKeyVO> sc = allFieldSearch.create();
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
    public List<KMSKeyVO> listByZone(Long zoneId, KeyPurpose purpose, KMSKey.State state) {
        SearchCriteria<KMSKeyVO> sc = allFieldSearch.create();
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
        SearchCriteria<KMSKeyVO> sc = allFieldSearch.create();
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
        return kmsWrappedKeyDao.countByKmsKeyId(kmsKeyId);
    }

    @Override
    public long countByKekLabel(String kekLabel, String providerName) {
        SearchCriteria<KMSKeyVO> sc = allFieldSearch.create();
        sc.setParameters("kekLabel", kekLabel);
        sc.setParameters("providerName", providerName);
        Integer count = getCount(sc);
        return count != null ? count.longValue() : 0L;
    }
}
