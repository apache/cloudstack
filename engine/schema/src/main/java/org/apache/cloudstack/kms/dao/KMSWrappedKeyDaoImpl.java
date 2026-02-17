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

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.kms.KMSWrappedKeyVO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KMSWrappedKeyDaoImpl extends GenericDaoBase<KMSWrappedKeyVO, Long> implements KMSWrappedKeyDao {

    private final SearchBuilder<KMSWrappedKeyVO> allFieldSearch;

    public KMSWrappedKeyDaoImpl() {
        super();

        allFieldSearch = createSearchBuilder();
        allFieldSearch.and("kmsKeyId", allFieldSearch.entity().getKmsKeyId(), SearchCriteria.Op.EQ);
        allFieldSearch.and("kekVersionId", allFieldSearch.entity().getKekVersionId(), SearchCriteria.Op.EQ);
        allFieldSearch.and("zoneId", allFieldSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        allFieldSearch.and("kmsKeyId", allFieldSearch.entity().getKmsKeyId(), SearchCriteria.Op.EQ);
        allFieldSearch.done();
    }

    @Override
    public long countByKmsKeyId(Long kmsKeyId) {
        SearchCriteria<KMSWrappedKeyVO> sc = allFieldSearch.create();
        sc.setParameters("kmsKeyId", kmsKeyId);
        Integer count = getCount(sc);
        return count != null ? count.longValue() : 0L;
    }

    @Override
    public List<KMSWrappedKeyVO> listByKekVersionId(Long kekVersionId, int limit) {
        SearchCriteria<KMSWrappedKeyVO> sc = allFieldSearch.create();
        sc.setParameters("kekVersionId", kekVersionId);
        Filter filter = new Filter(limit);
        return listBy(sc, filter);
    }

    @Override
    public long countByKekVersionId(Long kekVersionId) {
        if (kekVersionId == null) {
            return 0;
        }
        SearchCriteria<KMSWrappedKeyVO> sc = allFieldSearch.create();
        sc.setParameters("kekVersionId", kekVersionId);
        Integer count = getCount(sc);
        return count != null ? count.longValue() : 0L;
    }
}
