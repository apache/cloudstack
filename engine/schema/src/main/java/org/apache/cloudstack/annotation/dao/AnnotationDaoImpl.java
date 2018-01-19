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
package org.apache.cloudstack.annotation.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.annotation.AnnotationVO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @since 4.1
 */
@Component
public class AnnotationDaoImpl extends GenericDaoBase<AnnotationVO, Long> implements AnnotationDao {
    private final SearchBuilder<AnnotationVO> AnnotationSearchByType;
    private final SearchBuilder<AnnotationVO> AnnotationSearchByTypeAndUuid;

    public AnnotationDaoImpl() {
        super();
        AnnotationSearchByType = createSearchBuilder();
        AnnotationSearchByType.and("entityType", AnnotationSearchByType.entity().getEntityType(), SearchCriteria.Op.EQ);
        AnnotationSearchByType.done();
        AnnotationSearchByTypeAndUuid = createSearchBuilder();
        AnnotationSearchByTypeAndUuid.and("entityType", AnnotationSearchByTypeAndUuid.entity().getEntityType(), SearchCriteria.Op.EQ);
        AnnotationSearchByTypeAndUuid.and("entityUuid", AnnotationSearchByTypeAndUuid.entity().getEntityUuid(), SearchCriteria.Op.EQ);
        AnnotationSearchByTypeAndUuid.done();

    }

    @Override public List<AnnotationVO> findByEntityType(String entityType) {
        SearchCriteria<AnnotationVO> sc = createSearchCriteria();
        sc.addAnd("entityType", SearchCriteria.Op.EQ, entityType);
        return listBy(sc);
    }

    @Override public List<AnnotationVO> findByEntity(String entityType, String entityUuid) {
        SearchCriteria<AnnotationVO> sc = createSearchCriteria();
        sc.addAnd("entityType", SearchCriteria.Op.EQ, entityType);
        sc.addAnd("entityUuid", SearchCriteria.Op.EQ, entityUuid);
        return listBy(sc, null);
    }
}
