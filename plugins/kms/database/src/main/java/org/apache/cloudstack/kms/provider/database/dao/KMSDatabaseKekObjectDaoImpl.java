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

package org.apache.cloudstack.kms.provider.database.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.framework.kms.KeyPurpose;
import org.apache.cloudstack.kms.provider.database.KMSDatabaseKekObjectVO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KMSDatabaseKekObjectDaoImpl extends GenericDaoBase<KMSDatabaseKekObjectVO, Long> implements KMSDatabaseKekObjectDao {

    private final SearchBuilder<KMSDatabaseKekObjectVO> allFieldSearch;

    public KMSDatabaseKekObjectDaoImpl() {
        allFieldSearch = createSearchBuilder();
        allFieldSearch.and("uuid", allFieldSearch.entity().getUuid(), SearchCriteria.Op.EQ);
        allFieldSearch.and("label", allFieldSearch.entity().getLabel(), SearchCriteria.Op.EQ);
        allFieldSearch.and("objectId", allFieldSearch.entity().getObjectId(), SearchCriteria.Op.EQ);
        allFieldSearch.and("purpose", allFieldSearch.entity().getPurpose(), SearchCriteria.Op.EQ);
        allFieldSearch.and("keyType", allFieldSearch.entity().getKeyType(), SearchCriteria.Op.EQ);
        allFieldSearch.and("objectClass", allFieldSearch.entity().getObjectClass(), SearchCriteria.Op.EQ);
        allFieldSearch.done();
    }

    @Override
    public KMSDatabaseKekObjectVO findByLabel(String label) {
        SearchCriteria<KMSDatabaseKekObjectVO> sc = allFieldSearch.create();
        sc.setParameters("label", label);
        return findOneBy(sc);
    }

    @Override
    public KMSDatabaseKekObjectVO findByObjectId(byte[] objectId) {
        SearchCriteria<KMSDatabaseKekObjectVO> sc = allFieldSearch.create();
        sc.setParameters("objectId", objectId);
        return findOneBy(sc);
    }

    @Override
    public List<KMSDatabaseKekObjectVO> listByPurpose(KeyPurpose purpose) {
        SearchCriteria<KMSDatabaseKekObjectVO> sc = allFieldSearch.create();
        sc.setParameters("purpose", purpose);
        return listBy(sc);
    }

    @Override
    public List<KMSDatabaseKekObjectVO> listByKeyType(String keyType) {
        SearchCriteria<KMSDatabaseKekObjectVO> sc = allFieldSearch.create();
        sc.setParameters("keyType", keyType);
        return listBy(sc);
    }

    @Override
    public List<KMSDatabaseKekObjectVO> listByObjectClass(String objectClass) {
        SearchCriteria<KMSDatabaseKekObjectVO> sc = allFieldSearch.create();
        sc.setParameters("objectClass", objectClass);
        return listBy(sc);
    }

    @Override
    public boolean existsByLabel(String label) {
        return findByLabel(label) != null;
    }
}
