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

@Component
public class KMSKekVersionDaoImpl extends GenericDaoBase<KMSKekVersionVO, Long> implements KMSKekVersionDao {

    private final SearchBuilder<KMSKekVersionVO> allFieldSearch;

    public KMSKekVersionDaoImpl() {
        allFieldSearch = createSearchBuilder();
        allFieldSearch.and("kmsKeyId", allFieldSearch.entity().getKmsKeyId(), SearchCriteria.Op.EQ);
        allFieldSearch.and("status", allFieldSearch.entity().getStatus(), SearchCriteria.Op.IN);
        allFieldSearch.and("versionNumber", allFieldSearch.entity().getVersionNumber(), SearchCriteria.Op.EQ);
        allFieldSearch.and("kekLabel", allFieldSearch.entity().getKekLabel(), SearchCriteria.Op.EQ);
        allFieldSearch.done();
    }

    @Override
    public KMSKekVersionVO getActiveVersion(Long kmsKeyId) {
        SearchCriteria<KMSKekVersionVO> sc = allFieldSearch.create();
        sc.setParameters("kmsKeyId", kmsKeyId);
        sc.setParameters("status", KMSKekVersionVO.Status.Active);
        return findOneBy(sc);
    }

    @Override
    public List<KMSKekVersionVO> getVersionsForDecryption(Long kmsKeyId) {
        SearchCriteria<KMSKekVersionVO> sc = allFieldSearch.create();
        sc.setParameters("kmsKeyId", kmsKeyId);
        sc.setParameters("status", KMSKekVersionVO.Status.Active, KMSKekVersionVO.Status.Previous);
        return listBy(sc);
    }

    @Override
    public List<KMSKekVersionVO> listByKmsKeyId(Long kmsKeyId) {
        SearchCriteria<KMSKekVersionVO> sc = allFieldSearch.create();
        sc.setParameters("kmsKeyId", kmsKeyId);
        return listBy(sc);
    }

    @Override
    public KMSKekVersionVO findByKmsKeyIdAndVersion(Long kmsKeyId, Integer versionNumber) {
        SearchCriteria<KMSKekVersionVO> sc = allFieldSearch.create();
        sc.setParameters("kmsKeyId", kmsKeyId);
        sc.setParameters("versionNumber", versionNumber);
        return findOneBy(sc);
    }

    @Override
    public KMSKekVersionVO findByKekLabel(String kekLabel) {
        SearchCriteria<KMSKekVersionVO> sc = allFieldSearch.create();
        sc.setParameters("kekLabel", kekLabel);
        return findOneBy(sc);
    }
}
