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
import org.apache.cloudstack.kms.KMSKekVersionVO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementation of KMSKekVersionDao
 */
@Component
public class KMSKekVersionDaoImpl extends GenericDaoBase<KMSKekVersionVO, Long> implements KMSKekVersionDao {

    private final SearchBuilder<KMSKekVersionVO> uuidSearch;
    private final SearchBuilder<KMSKekVersionVO> kmsKeyIdSearch;
    private final SearchBuilder<KMSKekVersionVO> activeVersionSearch;
    private final SearchBuilder<KMSKekVersionVO> decryptionVersionsSearch;
    private final SearchBuilder<KMSKekVersionVO> versionNumberSearch;
    private final SearchBuilder<KMSKekVersionVO> kekLabelSearch;

    public KMSKekVersionDaoImpl() {
        super();

        // Search by UUID
        uuidSearch = createSearchBuilder();
        uuidSearch.and("uuid", uuidSearch.entity().getUuid(), SearchCriteria.Op.EQ);
        uuidSearch.and("removed", uuidSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        uuidSearch.done();

        // Search by KMS key ID
        kmsKeyIdSearch = createSearchBuilder();
        kmsKeyIdSearch.and("kmsKeyId", kmsKeyIdSearch.entity().getKmsKeyId(), SearchCriteria.Op.EQ);
        kmsKeyIdSearch.and("removed", kmsKeyIdSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        kmsKeyIdSearch.done();

        // Search for active version by KMS key ID
        activeVersionSearch = createSearchBuilder();
        activeVersionSearch.and("kmsKeyId", activeVersionSearch.entity().getKmsKeyId(), SearchCriteria.Op.EQ);
        activeVersionSearch.and("status", activeVersionSearch.entity().getStatus(), SearchCriteria.Op.EQ);
        activeVersionSearch.and("removed", activeVersionSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeVersionSearch.done();

        // Search for versions usable for decryption (Active or Previous)
        decryptionVersionsSearch = createSearchBuilder();
        decryptionVersionsSearch.and("kmsKeyId", decryptionVersionsSearch.entity().getKmsKeyId(), SearchCriteria.Op.EQ);
        decryptionVersionsSearch.and("status", decryptionVersionsSearch.entity().getStatus(), SearchCriteria.Op.IN);
        decryptionVersionsSearch.and("removed", decryptionVersionsSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        decryptionVersionsSearch.done();

        // Search by KMS key ID and version number
        versionNumberSearch = createSearchBuilder();
        versionNumberSearch.and("kmsKeyId", versionNumberSearch.entity().getKmsKeyId(), SearchCriteria.Op.EQ);
        versionNumberSearch.and("versionNumber", versionNumberSearch.entity().getVersionNumber(), SearchCriteria.Op.EQ);
        versionNumberSearch.and("removed", versionNumberSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        versionNumberSearch.done();

        // Search by KEK label
        kekLabelSearch = createSearchBuilder();
        kekLabelSearch.and("kekLabel", kekLabelSearch.entity().getKekLabel(), SearchCriteria.Op.EQ);
        kekLabelSearch.and("removed", kekLabelSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        kekLabelSearch.done();
    }

    @Override
    public KMSKekVersionVO findByUuid(String uuid) {
        SearchCriteria<KMSKekVersionVO> sc = uuidSearch.create();
        sc.setParameters("uuid", uuid);
        return findOneBy(sc);
    }

    @Override
    public KMSKekVersionVO getActiveVersion(Long kmsKeyId) {
        SearchCriteria<KMSKekVersionVO> sc = activeVersionSearch.create();
        sc.setParameters("kmsKeyId", kmsKeyId);
        sc.setParameters("status", KMSKekVersionVO.Status.Active);
        return findOneBy(sc);
    }

    @Override
    public List<KMSKekVersionVO> getVersionsForDecryption(Long kmsKeyId) {
        SearchCriteria<KMSKekVersionVO> sc = decryptionVersionsSearch.create();
        sc.setParameters("kmsKeyId", kmsKeyId);
        sc.setParameters("status", KMSKekVersionVO.Status.Active, KMSKekVersionVO.Status.Previous);
        return listBy(sc);
    }

    @Override
    public List<KMSKekVersionVO> listByKmsKeyId(Long kmsKeyId) {
        SearchCriteria<KMSKekVersionVO> sc = kmsKeyIdSearch.create();
        sc.setParameters("kmsKeyId", kmsKeyId);
        return listBy(sc);
    }

    @Override
    public KMSKekVersionVO findByKmsKeyIdAndVersion(Long kmsKeyId, Integer versionNumber) {
        SearchCriteria<KMSKekVersionVO> sc = versionNumberSearch.create();
        sc.setParameters("kmsKeyId", kmsKeyId);
        sc.setParameters("versionNumber", versionNumber);
        return findOneBy(sc);
    }

    @Override
    public KMSKekVersionVO findByKekLabel(String kekLabel) {
        SearchCriteria<KMSKekVersionVO> sc = kekLabelSearch.create();
        sc.setParameters("kekLabel", kekLabel);
        return findOneBy(sc);
    }
}

