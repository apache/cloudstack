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
import org.apache.cloudstack.kms.KMSWrappedKeyVO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementation of KMSWrappedKeyDao
 */
@Component
public class KMSWrappedKeyDaoImpl extends GenericDaoBase<KMSWrappedKeyVO, Long> implements KMSWrappedKeyDao {

    private final SearchBuilder<KMSWrappedKeyVO> uuidSearch;
    private final SearchBuilder<KMSWrappedKeyVO> kmsKeyIdSearch;
    private final SearchBuilder<KMSWrappedKeyVO> kekVersionIdSearch;
    private final SearchBuilder<KMSWrappedKeyVO> zoneSearch;

    public KMSWrappedKeyDaoImpl() {
        super();

        // Search by UUID
        uuidSearch = createSearchBuilder();
        uuidSearch.and("uuid", uuidSearch.entity().getUuid(), SearchCriteria.Op.EQ);
        uuidSearch.and("removed", uuidSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        uuidSearch.done();

        // Search by KMS Key ID (FK to kms_keys)
        kmsKeyIdSearch = createSearchBuilder();
        kmsKeyIdSearch.and("kmsKeyId", kmsKeyIdSearch.entity().getKmsKeyId(), SearchCriteria.Op.EQ);
        kmsKeyIdSearch.and("removed", kmsKeyIdSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        kmsKeyIdSearch.done();

        // Search by KEK Version ID (FK to kms_kek_versions)
        kekVersionIdSearch = createSearchBuilder();
        kekVersionIdSearch.and("kekVersionId", kekVersionIdSearch.entity().getKekVersionId(), SearchCriteria.Op.EQ);
        kekVersionIdSearch.and("removed", kekVersionIdSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        kekVersionIdSearch.done();

        // Search by zone
        zoneSearch = createSearchBuilder();
        zoneSearch.and("zoneId", zoneSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        zoneSearch.and("removed", zoneSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        zoneSearch.done();
    }

    @Override
    public KMSWrappedKeyVO findByUuid(String uuid) {
        SearchCriteria<KMSWrappedKeyVO> sc = uuidSearch.create();
        sc.setParameters("uuid", uuid);
        return findOneBy(sc);
    }

    @Override
    public List<KMSWrappedKeyVO> listByKmsKeyId(Long kmsKeyId) {
        SearchCriteria<KMSWrappedKeyVO> sc = kmsKeyIdSearch.create();
        sc.setParameters("kmsKeyId", kmsKeyId);
        return listBy(sc);
    }

    @Override
    public List<KMSWrappedKeyVO> listByZone(Long zoneId) {
        SearchCriteria<KMSWrappedKeyVO> sc = zoneSearch.create();
        sc.setParameters("zoneId", zoneId);
        return listBy(sc);
    }

    @Override
    public long countByKmsKeyId(Long kmsKeyId) {
        SearchCriteria<KMSWrappedKeyVO> sc = kmsKeyIdSearch.create();
        sc.setParameters("kmsKeyId", kmsKeyId);
        Integer count = getCount(sc);
        return count != null ? count.longValue() : 0L;
    }

    @Override
    public List<KMSWrappedKeyVO> listByKekVersionId(Long kekVersionId) {
        SearchCriteria<KMSWrappedKeyVO> sc = kekVersionIdSearch.create();
        sc.setParameters("kekVersionId", kekVersionId);
        return listBy(sc);
    }
}

